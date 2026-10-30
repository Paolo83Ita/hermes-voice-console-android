package com.hermestower.voiceconsole.tts

import com.hermestower.voiceconsole.domain.TextToSpeechEngine
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive

/**
 * Mock TTS for V0.1: synthesizes plausible speaking timing and a smoothed amplitude
 * so SPEAKING visuals and captions behave truthfully relative to this engine's output.
 * No audio is produced; the real system-TTS adapter is V0.3 (VOICE_PIPELINE_OPTIONS §5).
 */
class MockTextToSpeechEngine : TextToSpeechEngine {

    private val _state = MutableStateFlow(TextToSpeechEngine.State.READY)
    override val state: StateFlow<TextToSpeechEngine.State> = _state.asStateFlow()

    @Volatile
    private var generation = 0L

    override fun speak(text: String, rate: Float): Flow<Float> = flow {
        val myGen = ++generation
        _state.value = TextToSpeechEngine.State.SPEAKING
        try {
            val durationMs = speakingDurationMs(text, rate)
            var elapsed = 0L
            while (elapsed < durationMs) {
                if (generation != myGen || !currentCoroutineContext().isActive) return@flow
                val phase = (elapsed % 320L) / 320.0
                val amplitude = (0.35 + 0.55 * kotlin.math.abs(kotlin.math.sin(phase * Math.PI))).toFloat()
                emit(amplitude)
                delay(TICK_MS)
                elapsed += TICK_MS
            }
            emit(0f)
        } finally {
            if (generation == myGen) {
                _state.value = TextToSpeechEngine.State.READY
            }
        }
    }.flowOn(Dispatchers.Default)

    override fun interrupt() {
        generation++
        _state.value = TextToSpeechEngine.State.READY
    }

    companion object {
        const val TICK_MS = 120L
        const val MAX_MS = 6000L

        fun speakingDurationMs(text: String, rate: Float): Long {
            val base = (text.length * 55L) / rate.coerceIn(0.5f, 2.0f)
            return base.coerceIn(600L, MAX_MS)
        }
    }
}
