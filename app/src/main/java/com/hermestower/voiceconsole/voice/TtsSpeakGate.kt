package com.hermestower.voiceconsole.voice

/**
 * TtsSpeakGate (OWNER PHYSICAL RETEST §4): the FAIL-CLOSED decision that the
 * pre-repair pipeline lacked.
 *
 * The CP-G4-026 repair produced a typed Italian negotiation verdict but
 * `speak()` never consumed it — the engine's default (English) voice stayed
 * audible while Italian was requested (`FALLBACK_VOICE_TRIGGERED` audible on
 * the POCO X6 Pro, PHYSICAL-FAIL-01). The gate closes that class:
 *
 *  - when the effective app language is Italian, spoken output is allowed
 *    ONLY after the post-apply read-back reports an Italian voice;
 *  - an English (or any non-Italian) read-back while Italian is requested
 *    produces `ITALIAN_SYSTEM_TTS_READBACK_MISMATCH` and speech is BLOCKED;
 *  - missing Italian voice data produces `ITALIAN_SYSTEM_TTS_DATA_MISSING`
 *    and speech is BLOCKED;
 *  - when the effective language is NOT Italian, the gate allows the
 *    negotiated path for that language (the read-back is still published).
 *
 * Pure Kotlin: unit-testable, no Android imports.
 */
object TtsSpeakGate {

    /** Typed reasons speech is blocked (surfaced verbatim in UI + telemetry). */
    enum class BlockReason {
        /** No Italian voice data installed on the device. */
        ITALIAN_SYSTEM_TTS_DATA_MISSING,

        /** The engine's ACTIVE voice is not Italian while Italian is requested. */
        ITALIAN_SYSTEM_TTS_READBACK_MISMATCH,

        /** The engine does not support Italian at all. */
        ITALIAN_SYSTEM_TTS_NOT_SUPPORTED,

        /** No engine exists to speak with. */
        ITALIAN_SYSTEM_TTS_ENGINE_UNAVAILABLE,
    }

    /** The typed gate verdict. */
    sealed class Verdict {
        /** Spoken output may proceed (the read-back matches the requested language). */
        object Allow : Verdict()

        /** Spoken output is REFUSED — the response stays visible as text. */
        data class Block(val reason: BlockReason, val readbackStatus: String) : Verdict()
    }

    /**
     * @param effectiveLanguageTag the app's effective language tag (e.g. "it", "en").
     * @param readback the post-apply negotiation truth (null = the engine never negotiated).
     */
    fun decide(
        effectiveLanguageTag: String,
        readback: ItalianTtsVoiceNegotiator.TtsVoiceReadback?,
    ): Verdict {
        val language = effectiveLanguageTag.substringBefore('-').lowercase(java.util.Locale.ROOT)
        if (language != "it") return Verdict.Allow
        val rb = readback ?: return Verdict.Block(
            BlockReason.ITALIAN_SYSTEM_TTS_ENGINE_UNAVAILABLE,
            "NO_READBACK",
        )
        return when (rb.status) {
            ItalianTtsVoiceNegotiator.ItalianTtsVoiceStatus.ITALIAN_VOICE_ACTIVE -> Verdict.Allow
            ItalianTtsVoiceNegotiator.ItalianTtsVoiceStatus.ITALIAN_VOICE_MISSING_DATA ->
                Verdict.Block(BlockReason.ITALIAN_SYSTEM_TTS_DATA_MISSING, rb.status.name)
            ItalianTtsVoiceNegotiator.ItalianTtsVoiceStatus.ITALIAN_VOICE_NOT_SUPPORTED ->
                Verdict.Block(BlockReason.ITALIAN_SYSTEM_TTS_NOT_SUPPORTED, rb.status.name)
            ItalianTtsVoiceNegotiator.ItalianTtsVoiceStatus.FALLBACK_VOICE_TRIGGERED ->
                Verdict.Block(BlockReason.ITALIAN_SYSTEM_TTS_READBACK_MISMATCH, rb.status.name)
            ItalianTtsVoiceNegotiator.ItalianTtsVoiceStatus.ENGINE_UNAVAILABLE ->
                Verdict.Block(BlockReason.ITALIAN_SYSTEM_TTS_ENGINE_UNAVAILABLE, rb.status.name)
        }
    }
}
