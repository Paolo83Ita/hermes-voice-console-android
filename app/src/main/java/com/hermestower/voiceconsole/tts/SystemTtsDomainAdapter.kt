package com.hermestower.voiceconsole.tts

import com.hermestower.voiceconsole.domain.TextToSpeechEngine
import com.hermestower.voiceconsole.voice.TtsEngine
import com.hermestower.voiceconsole.voice.TtsState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow

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
 *
 * OWNER PHYSICAL RETEST §3/§4: (1) the post-speak state check no longer races
 * the utterance listener — the adapter polls briefly for SPEAKING (an utterance
 * start is asynchronous; the pre-repair single check ran before onStart could
 * ever fire, so the console never showed SPEAKING for real audio);
 * (2) the fail-closed refusal ([TtsState.TEXT_ONLY_SAFE_DEGRADED] — Italian
 * requested, no Italian voice read-back) completes the flow honestly: the
 * response stays visible as text, no amplitude, no SPEAKING lie. The `rate`
 * parameter of the domain seam is NOT the audio authority anymore — the
 * persisted [com.hermestower.voiceconsole.voice.SpeechProfile] is (the engine
 * applies it before every utterance); the parameter only drives the mock's
 * timing and is kept for the domain-seam contract.
 */
class SystemTtsDomainAdapter(
    private val engine: TtsEngine,
    private val readyWaitMs: Long = 2_000L,
    private val speakStartWaitMs: Long = 2_000L,
    private val pollMs: Long = 80L,
    private val ticker: suspend (Long) -> Unit = { delay(it) },
) : TextToSpeechEngine {

    override val state: kotlinx.coroutines.flow.StateFlow<TextToSpeechEngine.State>
        get() = _state
    private val _state = kotlinx.coroutines.flow.MutableStateFlow(TextToSpeechEngine.State.NOT_READY)

    override fun interrupt() {
        engine.interrupt()
        _state.value = TextToSpeechEngine.State.NOT_READY
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
        // The gate may have REFUSED the utterance (fail-closed, Italian missing):
        // complete honestly — text stays visible, no SPEAKING lie.
        val blocked = engine.state.value.state == TtsState.TEXT_ONLY_SAFE_DEGRADED ||
            engine.state.value.state == TtsState.FAILED
        if (blocked) {
            _state.value = TextToSpeechEngine.State.NOT_READY
            close()
            return@channelFlow
        }
        // Utterance start is asynchronous: poll briefly for SPEAKING (pre-repair
        // this was a single immediate check that always saw SYNTHESIZING).
        val startDeadline = System.currentTimeMillis() + speakStartWaitMs
        while (engine.state.value.state == TtsState.SYNTHESIZING && System.currentTimeMillis() < startDeadline) {
            ticker(pollMs)
        }
        when (engine.state.value.state) {
            TtsState.SPEAKING -> _state.value = TextToSpeechEngine.State.SPEAKING
            else -> {
                // speak refused or never started (engine error): text-only, no lie.
                _state.value = TextToSpeechEngine.State.NOT_READY
                close()
                return@channelFlow
            }
        }
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
