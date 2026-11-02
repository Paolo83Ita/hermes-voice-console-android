package com.hermestower.voiceconsole.tts

import com.hermestower.voiceconsole.domain.TextToSpeechEngine
import com.hermestower.voiceconsole.voice.TtsEngine
import com.hermestower.voiceconsole.voice.TtsState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.first

/**
 * GRAPH 4 G4-C03 (§5/§10): bridges the voice-seam [TtsEngine] (real Android
 * system TTS) into the console-domain [TextToSpeechEngine].
 *
 * Truthfulness contract (Owner delta §5): the console may show SPEAKING only
 * while the engine is producing audio. The adapter therefore polls the engine
 * state — SPEAKING emits amplitude, anything else completes the flow. If the
 * engine is not READY yet, it is loaded and given a short window; if it never
 * becomes ready the flow completes EMPTY (text-only degradation, honest).
 * The amplitude curve is a display approximation only (the seam does not
 * expose real loudness); the AUDIO itself is real system TTS output.
 */
class SystemTtsDomainAdapter(
    private val engine: TtsEngine,
    private val readyWaitMs: Long = 2_000L,
    private val pollMs: Long = 80L,
    private val ticker: suspend (Long) -> Unit = { delay(it) },
) : TextToSpeechEngine {

    override val state: kotlinx.coroutines.flow.StateFlow<TextToSpeechEngine.State>
        get() = _state
    private val _state = kotlinx.coroutines.flow.MutableStateFlow(TextToSpeechEngine.State.NOT_READY)

    init {
        // Note: engine state mapping happens per speak() call; NOT_READY is the
        // honest resting label until the first successful speak.
    }

    override fun interrupt() {
        engine.interrupt()
    }

    override fun speak(text: String, rate: Float): Flow<Float> = channelFlow {
        engine.load()
        // Short honest wait for READY (system TTS init is async). Never fake SPEAKING.
        val deadline = System.currentTimeMillis() + readyWaitMs
        while (engine.state.value.state != TtsState.READY && System.currentTimeMillis() < deadline) {
            ticker(pollMs)
        }
        if (engine.state.value.state != TtsState.READY) {
            engine.interrupt()
            close()
            return@channelFlow
        }
        _state.value = TextToSpeechEngine.State.READY
        engine.speak(text)
        if (engine.state.value.state != TtsState.SPEAKING) {
            // speak refused (engine error): text-only, no SPEAKING lie.
            close()
            return@channelFlow
        }
        _state.value = TextToSpeechEngine.State.SPEAKING
        var t = 0.0
        while (engine.state.value.state == TtsState.SPEAKING) {
            // Decorative amplitude shaped like a calm syllable train.
            t += 0.35
            val amp = (0.55 + 0.45 * kotlin.math.sin(t)).toFloat().coerceIn(0f, 1f)
            trySend(amp)
            ticker(pollMs)
        }
        _state.value = TextToSpeechEngine.State.READY
        close()
    }
}
