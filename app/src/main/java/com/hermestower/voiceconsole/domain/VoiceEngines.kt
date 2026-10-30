package com.hermestower.voiceconsole.domain

/**
 * Voice pipeline contracts (VOICE_PIPELINE_OPTIONS.md).
 * V0.1 ships no real audio: every seam is prepared, nothing is claimed (D-006).
 */

/** Always-on wake detection. The only permitted always-listening component (SEC-04). */
interface WakeWordEngine {
    enum class State { NOT_INSTALLED, READY, LISTENING, ERROR }
    val state: kotlinx.coroutines.flow.StateFlow<State>
    /** Keyword phrase the engine listens for; configurable per qualification. */
    val phrase: String
    fun start()
    fun stop()
}

/** Gates capture windows on speech presence. */
interface VoiceActivityDetector {
    enum class State { NOT_INSTALLED, READY, ACTIVE }
    val state: kotlinx.coroutines.flow.StateFlow<State>
    fun start()
    fun stop()
}

/** Microphone capture. Must never start while privacy mute is engaged (FR-03). */
interface AudioCaptureEngine {
    data class AudioChunk(val pcm: ShortArray, val sampleRate: Int, val atMs: Long)
    val isCapturing: kotlinx.coroutines.flow.StateFlow<Boolean>
    suspend fun start(): Result<Unit>
    suspend fun stop()
}

/** Speech-to-text with confidence, feeding the router (FR-05). */
interface SpeechToTextEngine {
    enum class State { NOT_INSTALLED, READY, TRANSCRIBING }
    data class Transcript(val text: String, val confidence: Float, val isFinal: Boolean, val language: String)
    val state: kotlinx.coroutines.flow.StateFlow<State>
    fun transcribe(chunks: kotlinx.coroutines.flow.Flow<AudioCaptureEngine.AudioChunk>): kotlinx.coroutines.flow.Flow<Transcript>
}
