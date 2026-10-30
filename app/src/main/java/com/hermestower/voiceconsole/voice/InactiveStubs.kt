package com.hermestower.voiceconsole.voice

import com.hermestower.voiceconsole.domain.AudioCaptureEngine
import com.hermestower.voiceconsole.domain.SpeechToTextEngine
import com.hermestower.voiceconsole.domain.VoiceActivityDetector
import com.hermestower.voiceconsole.domain.WakeWordEngine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emptyFlow

/**
 * Inactive V0.1 stubs (decision D-006): the seams exist, no capture happens.
 * Each reports NOT_INSTALLED so every screen can state the truth.
 */

class InactiveWakeWordEngine(override val phrase: String) : WakeWordEngine {
    private val _state = MutableStateFlow(WakeWordEngine.State.NOT_INSTALLED)
    override val state: StateFlow<WakeWordEngine.State> = _state.asStateFlow()
    override fun start() { /* V0.2+: openWakeWord / Porcupine qualification */ }
    override fun stop() { /* V0.2+ */ }
}

class InactiveVoiceActivityDetector : VoiceActivityDetector {
    private val _state = MutableStateFlow(VoiceActivityDetector.State.NOT_INSTALLED)
    override val state: StateFlow<VoiceActivityDetector.State> = _state.asStateFlow()
    override fun start() { /* V0.3: Silero / WebRTC VAD */ }
    override fun stop() { /* V0.3 */ }
}

/** No capture engine is shipped in V0.1 at all (no adapter, no permission request). */
class NoAudioCaptureEngine : AudioCaptureEngine {
    private val _isCapturing = MutableStateFlow(false)
    override val isCapturing: StateFlow<Boolean> = _isCapturing.asStateFlow()
    override suspend fun start(): Result<Unit> =
        Result.failure(IllegalStateException("Audio capture is not implemented in V0.1 (mission stage B constraint)."))
    override suspend fun stop() { _isCapturing.value = false }
}

class InactiveSpeechToTextEngine : SpeechToTextEngine {
    private val _state = MutableStateFlow(SpeechToTextEngine.State.NOT_INSTALLED)
    override val state: StateFlow<SpeechToTextEngine.State> = _state.asStateFlow()
    override fun transcribe(chunks: Flow<AudioCaptureEngine.AudioChunk>): Flow<SpeechToTextEngine.Transcript> = emptyFlow()
}
