package com.hermestower.voiceconsole.voice

/**
 * TtsTelemetry (OWNER PHYSICAL RETEST §3): safe, structured, secret-clean TTS
 * runtime telemetry.
 *
 * Every event records the TTS negotiation / utterance lifecycle WITHOUT ever
 * logging spoken user content — text is recorded as LENGTH ONLY (SEC §4: no
 * secret speech in logs). The ring buffer is surfaced verbatim in the TTS
 * diagnostic card and in the exported secret-clean diagnostic receipt, so the
 * device itself answers the call-path questions (which engine, which voice,
 * which locale, whether fallback occurred) without ADB.
 *
 * The sink is injectable: the Android engine mirrors events to logcat, JVM
 * tests assert the buffer directly.
 */
class TtsTelemetry(
    private val capacity: Int = 48,
    private val clockMillis: () -> Long = System::currentTimeMillis,
    private val sink: (String) -> Unit = {},
) {

    /** Typed event names (Owner brief §3 list) — never free-form user text. */
    object Event {
        const val ENGINE_INIT_START = "TTS_ENGINE_INIT_START"
        const val ENGINE_INIT_OK = "TTS_ENGINE_INIT_OK"
        const val ENGINE_INIT_FAILED = "TTS_ENGINE_INIT_FAILED"
        const val LOCALE_REQUESTED = "TTS_LOCALE_REQUESTED"
        const val SET_LANGUAGE_RESULT = "TTS_SET_LANGUAGE_RESULT"
        const val LANGUAGE_AVAILABILITY = "TTS_LANGUAGE_AVAILABILITY"
        const val VOICES_ENUMERATED = "TTS_VOICES_ENUMERATED"
        const val VOICE_SELECTED = "TTS_VOICE_SELECTED"
        const val SET_VOICE_RESULT = "TTS_SET_VOICE_RESULT"
        const val READBACK_VOICE = "TTS_READBACK_VOICE"
        const val READBACK_LANGUAGE = "TTS_READBACK_LANGUAGE"
        const val NEGOTIATION_STATUS = "TTS_NEGOTIATION_STATUS"
        const val NETWORK_REQUIRED = "TTS_NETWORK_REQUIRED"
        const val SPEAK_CALL = "TTS_SPEAK_CALL"
        const val SPEAK_BLOCKED = "TTS_SPEAK_BLOCKED"
        const val UTTERANCE_START = "TTS_UTTERANCE_START"
        const val UTTERANCE_DONE = "TTS_UTTERANCE_DONE"
        const val UTTERANCE_ERROR = "TTS_UTTERANCE_ERROR"
        const val FALLBACK_REASON = "TTS_FALLBACK_REASON"
        const val RATE_APPLIED = "TTS_RATE_APPLIED"
        const val PITCH_APPLIED = "TTS_PITCH_APPLIED"
        const val ENGINE_SHUTDOWN = "TTS_ENGINE_SHUTDOWN"
    }

    private val lock = Any()
    private val buffer = ArrayDeque<String>(capacity)

    /**
     * Records one typed event. `detail` carries technical identity only
     * (codes, package names, voice names, locales, counts, lengths) — the
     * caller MUST NOT pass spoken or user content.
     */
    fun log(event: String, detail: String = "") {
        val ts = clockMillis()
        val line = buildString {
            append(ts)
            append(' ')
            append(event)
            if (detail.isNotEmpty()) {
                append(' ')
                append(detail)
            }
        }
        synchronized(lock) {
            buffer.addLast(line)
            while (buffer.size > capacity) buffer.removeFirst()
        }
        runCatching { sink(line) }
    }

    /** Snapshot of the recorded events, oldest first (secret-clean by contract). */
    fun snapshot(): List<String> = synchronized(lock) { buffer.toList() }

    fun clear() = synchronized(lock) { buffer.clear() }
}
