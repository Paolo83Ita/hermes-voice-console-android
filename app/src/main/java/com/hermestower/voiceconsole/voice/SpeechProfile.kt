package com.hermestower.voiceconsole.voice

/**
 * SpeechProfile (OWNER PHYSICAL RETEST §5): the explicit speech-profile
 * authority for the real TTS output.
 *
 * The physically heard output on the POCO X6 Pro was the ENGINE DEFAULT voice
 * character (`HIGH_PITCH_ENGLISH_TTS_OUTPUT`) because no `setSpeechRate` /
 * `setPitch` call existed anywhere in the pre-repair pipeline. This profile is
 * now the single authority: the engine applies it before every utterance and
 * the Voice & Audio screen edits it through the bounded controls.
 *
 * Safe ranges (Owner brief §5): speed 0.75–1.30, pitch 0.75–1.25.
 * Recommended initial defaults (starting values, NOT a physical
 * qualification): speechRate = 0.96f, pitch = 0.95f. No forced high pitch, no
 * random voice selection.
 *
 * Pure Kotlin: unit-testable, Compose-agnostic, no Android imports.
 */
data class SpeechProfile(
    val speechRate: Float,
    val pitch: Float,
) {
    companion object {
        /** Safe speed range (Owner brief §5). */
        const val RATE_MIN = 0.75f
        const val RATE_MAX = 1.30f

        /** Safe pitch range (Owner brief §5). */
        const val PITCH_MIN = 0.75f
        const val PITCH_MAX = 1.25f

        /** Recommended initial defaults (starting values, not qualification). */
        const val RECOMMENDED_RATE = 0.96f
        const val RECOMMENDED_PITCH = 0.95f

        val RECOMMENDED = SpeechProfile(RECOMMENDED_RATE, RECOMMENDED_PITCH)

        /** Coerces any incoming value into the safe ranges (persistence guard). */
        fun safe(rate: Float, pitch: Float): SpeechProfile = SpeechProfile(
            speechRate = rate.coerceIn(RATE_MIN, RATE_MAX),
            pitch = pitch.coerceIn(PITCH_MIN, PITCH_MAX),
        )

        /** The three ordinary speed presets (Owner brief §5: Slower / Recommended / Faster). */
        val RATE_SLOWER = 0.88f
        val RATE_FASTER = 1.06f

        /** The three ordinary pitch presets (Owner brief §5: Lower / Recommended / Higher). */
        val PITCH_LOWER = 0.85f
        val PITCH_HIGHER = 1.08f
    }
}
