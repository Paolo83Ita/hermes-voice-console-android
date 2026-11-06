package com.hermestower.voiceconsole.voice

/**
 * GRAPH 4.10 CP-G4.10-006 (§16 ECHO PREVENTION) — the fingerprint guard that
 * keeps the console from transcribing its own spoken response.
 *
 * Default BARGE_IN_OFF: the microphone is paused during TTS, a post-speech
 * guard interval elapses before capture resumes, and ANY candidate transcript
 * that resembles recently spoken assistant text inside the time window is
 * REJECTED as a user turn (plus a verbatim-match rule for the exact response
 * text). Normalized comparison: lowercase, punctuation stripped, token-set
 * Jaccard similarity. No audio is ever stored — only the spoken TEXT length
 * and normalized token fingerprint in memory.
 */
class EchoFingerprintGuard(
    private val clockMs: () -> Long = { System.currentTimeMillis() },
) {

    private data class Entry(val normalizedTokens: Set<String>, val raw: String, val atMs: Long)

    @Volatile
    private var lastSpoken: Entry? = null
    private val recentSpoken = ArrayDeque<Entry>()

    /** The driver records every utterance the console actually speaks. */
    fun rememberSpoken(text: String) {
        val entry = Entry(normalize(text), text, clockMs())
        lastSpoken = entry
        synchronized(recentSpoken) {
            recentSpoken.addLast(entry)
            while (recentSpoken.size > MAX_RECENT) recentSpoken.removeFirst()
        }
    }

    /**
     * True when [candidate] should be REJECTED as a user turn because it is
     * (near-)verbatim the assistant's recent spoken response.
     */
    fun isLikelyEcho(candidate: String, windowMs: Long = DEFAULT_WINDOW_MS): Boolean {
        val now = clockMs()
        val tokens = normalize(candidate)
        if (tokens.isEmpty()) return false
        synchronized(recentSpoken) {
            for (entry in recentSpoken) {
                if (now - entry.atMs > windowMs) continue
                // Verbatim (normalized) echo of the spoken response.
                if (entry.normalizedTokens == tokens) return true
                // Near-duplicate: high token overlap with a recent spoken text.
                if (similarity(tokens, entry.normalizedTokens) >= ECHO_SIMILARITY_THRESHOLD) return true
            }
        }
        return false
    }

    fun clear() {
        lastSpoken = null
        synchronized(recentSpoken) { recentSpoken.clear() }
    }

    private fun similarity(a: Set<String>, b: Set<String>): Double {
        if (a.isEmpty() || b.isEmpty()) return 0.0
        val intersection = a.intersect(b).size.toDouble()
        val union = a.union(b).size.toDouble()
        return intersection / union
    }

    companion object {
        const val DEFAULT_WINDOW_MS = 8_000L
        const val ECHO_SIMILARITY_THRESHOLD = 0.80
        const val MAX_RECENT = 4

        /** Normalized fingerprint: lowercase, punctuation stripped, tokenized. */
        fun normalize(text: String): Set<String> =
            text.lowercase()
                .replace(Regex("[\\p{Punct}\\p{IsPunctuation}]"), " ")
                .split(Regex("\\s+"))
                .filter { it.isNotBlank() }
                .toSet()
    }
}
