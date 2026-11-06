package com.hermestower.voiceconsole.voice

import kotlinx.coroutines.flow.StateFlow

/**
 * GRAPH 4 NODE 3 (§8/§9/§10): typed seams of the real voice pipeline.
 *
 * The pipeline is: AudioFrameSource → VadEngine → SttEngine → IntentRouter →
 * Local/Tower execution → TtsEngine → speaker, with a transcript/attribution
 * record at the end. Every seam is replaceable; every engine state is typed; no
 * component here can retain raw audio (INV-G4-09: retention stays OFF by
 * default, frames are consumed in-flight and dropped).
 */

/** One 16 kHz mono 16-bit PCM frame consumed in-flight and never stored. */
class AudioFrame(val samples: ShortArray, val sampleRate: Int = 16000) {
    val durationMs: Long get() = samples.size * 1000L / sampleRate
}

/**
 * Microphone source. The implementation MUST stop the hardware capture when
 * [stop] is called — privacy mute (INV-G4-10) physically prevents further
 * capture by closing the source, not by dropping frames downstream.
 */
interface AudioFrameSource {
    val isActive: Boolean
    /** Starts capture; emits frames until stopped. Suspending read. */
    fun start()
    /** Stops capture and releases the microphone. Idempotent. */
    fun stop()
    /** Blocking/suspending read of the next frame; null when the source is stopped. */
    suspend fun read(): AudioFrame?
}

/** Voice activity detection. Real-time, low-memory, no retention. */
interface VadEngine {
    /** True when the frame contains speech energy above the engine's threshold. */
    fun isSpeech(frame: AudioFrame): Boolean
    fun reset()
}

/** Speech-to-text adapter states (§9, typed). */
enum class SttState(val displayLabel: String) {
    NOT_INSTALLED("Not installed"),
    LOADING("Loading"),
    READY("Ready"),
    LISTENING("Listening"),
    TRANSCRIBING("Transcribing"),
    FINALIZING("Finalizing"),
    FAILED("Failed"),
    UNLOADED("Unloaded"),
}

data class SttOutput(
    val state: SttState,
    val partialTranscript: String? = null,
    val finalTranscript: String? = null,
    val language: String = "it-IT",
    val confidence: Float? = null,
    val elapsedMs: Long = 0,
    val error: SttError? = null,
)

/** Typed STT errors (§9) — never a bare string leak into state. */
data class SttError(val kind: Kind, val detail: String) {
    enum class Kind { NOT_READY, AUDIO_LOST, MODEL_ERROR, TIMEOUT, UNKNOWN }
}

/** Replaceable STT adapter (§9). */
interface SttEngine {
    val state: StateFlow<SttOutput>
    fun load()
    fun unload()
    /** Feeds a frame; returns the current output (may carry a partial transcript). */
    fun accept(frame: AudioFrame): SttOutput
    /** End-of-speech finalization; produces the final transcript. */
    fun finalizeTranscript(): SttOutput
    fun reset()
}

/** Text-to-speech adapter states (§10, typed). */
enum class TtsState(val displayLabel: String) {
    NOT_INSTALLED("Not installed"),
    LOADING("Loading"),
    READY("Ready"),
    SYNTHESIZING("Synthesizing"),
    SPEAKING("Speaking"),
    INTERRUPTED("Interrupted"),
    FAILED("Failed"),
    UNLOADED("Unloaded"),
    // OWNER PHYSICAL RETEST §4: the honest fail-closed resting state — Italian
    // was requested, the read-back is not an Italian voice, spoken output is
    // REFUSED and the response stays visible as text. Never a silent English voice.
    TEXT_ONLY_SAFE_DEGRADED("Text-only (safe degraded)"),
}

/**
 * OWNER PHYSICAL RETEST §4: the typed reason speech was REFUSED by the
 * fail-closed gate (TtsSpeakGate). Surfaced verbatim in the TTS diagnostic
 * card, Voice & Audio and the exported receipt.
 */
data class TtsSpeakBlock(
    val reason: TtsSpeakGate.BlockReason,
    val readbackStatus: String,
) {
    /** The typed token the Owner brief mandates for the missing-data class. */
    val token: String
        get() = when (reason) {
            TtsSpeakGate.BlockReason.ITALIAN_SYSTEM_TTS_DATA_MISSING -> "ITALIAN_SYSTEM_TTS_DATA_MISSING"
            TtsSpeakGate.BlockReason.ITALIAN_SYSTEM_TTS_READBACK_MISMATCH -> "ITALIAN_SYSTEM_TTS_READBACK_MISMATCH"
            TtsSpeakGate.BlockReason.ITALIAN_SYSTEM_TTS_NOT_SUPPORTED -> "ITALIAN_SYSTEM_TTS_NOT_SUPPORTED"
            TtsSpeakGate.BlockReason.ITALIAN_SYSTEM_TTS_ENGINE_UNAVAILABLE -> "ITALIAN_SYSTEM_TTS_ENGINE_UNAVAILABLE"
        }
}

data class TtsOutput(
    val state: TtsState,
    val spokenText: String? = null,
    val error: String? = null,
    /**
     * OWNER PHYSICAL FEEDBACK 3: the post-apply voice truth (null when the
     * engine has not negotiated yet or the implementation has no read-back).
     * Carries the typed ItalianTtsVoiceStatus the Diagnostics screen, the
     * Voice & Audio card and Self-Test ST-51 surface verbatim.
     */
    val voiceReadback: ItalianTtsVoiceNegotiator.TtsVoiceReadback? = null,
    /**
     * OWNER PHYSICAL RETEST §4: non-null when the fail-closed gate REFUSED
     * the last speak request (text-only safe degraded). The response text
     * remains visible in the console; no non-Italian audio is ever produced.
     */
    val speakBlocked: TtsSpeakBlock? = null,
)

/**
 * Replaceable TTS adapter (§10) with the three-tier fallback contract:
 * selected qualified local voice → Android system offline TTS → text-only.
 * Fallback selection is the pipeline controller's job; adapters only report
 * their honest state.
 */
interface TtsEngine {
    val state: StateFlow<TtsOutput>
    fun load()
    fun unload()
    /**
     * Starts speaking; returns immediately (state moves SYNTHESIZING → SPEAKING).
     * OWNER PHYSICAL RETEST §4: the implementation MUST fail closed — when the
     * negotiated voice does not match the requested language the utterance is
     * refused (TEXT_ONLY_SAFE_DEGRADED + TtsSpeakBlock), never spoken with a
     * fallback voice.
     */
    fun speak(text: String)
    /** Interrupts speech (barge-in / cancel). */
    fun interrupt()
}
