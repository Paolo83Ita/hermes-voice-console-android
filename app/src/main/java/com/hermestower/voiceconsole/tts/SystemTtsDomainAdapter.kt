package com.hermestower.voiceconsole.tts

import com.hermestower.voiceconsole.domain.TextToSpeechEngine
import com.hermestower.voiceconsole.voice.TtsEngine
import com.hermestower.voiceconsole.voice.TtsState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import java.util.concurrent.atomic.AtomicLong

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
 *
 * GRAPH 4.9 REGRESSION REPAIR (e2e_14 / e2e_19_20): interruption is a TOKEN,
 * not a best-effort state write. The CP-G4-029 repair introduced the honest
 * ready-wait window, but an [interrupt] arriving INSIDE the window was
 * swallowed: the engine flipped LOADING→READY, the wait loop exited on READY
 * and [TtsEngine.speak] was called unconditionally — the Owner cancel was
 * lost and the utterance was spoken anyway (the console then showed SPEAKING
 * past the cancel; E2EVoiceFlowTest e2e_14 failed at the repaired tip and
 * e2e_19_20 failed as its cascade). The adapter now samples the token when
 * the utterance flow starts and re-checks it in EVERY wait loop and before
 * the SPEAKING state is claimed; a token change closes the flow without
 * speaking and without overriding the NOT_READY state that interrupt() set.
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

    /** Bumped on every interrupt: any in-flight speak() with an older token must stop. */
    private val interruptToken = AtomicLong(0L)

    override fun interrupt() {
        interruptToken.incrementAndGet()
        engine.interrupt()
        _state.value = TextToSpeechEngine.State.NOT_READY
    }

    override fun speak(text: String, rate: Float): Flow<Float> = channelFlow {
        val myToken = interruptToken.get()
        fun interrupted() = interruptToken.get() != myToken

        engine.load()
        // Short honest wait for READY (system TTS init is async). Never fake SPEAKING.
        val deadline = System.currentTimeMillis() + readyWaitMs
        while (engine.state.value.state != TtsState.READY && System.currentTimeMillis() < deadline) {
            if (interrupted()) { // cancel wins: the utterance must never start
                close()
                return@channelFlow
            }
            ticker(pollMs)
        }
        if (interrupted() || engine.state.value.state != TtsState.READY) {
            if (!interrupted()) engine.interrupt() // engine never became ready
            _state.value = TextToSpeechEngine.State.NOT_READY
            close()
            return@channelFlow
        }
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
            if (interrupted()) {
                _state.value = TextToSpeechEngine.State.NOT_READY
                close()
                return@channelFlow
            }
            ticker(pollMs)
        }
        when (engine.state.value.state) {
            TtsState.SPEAKING -> {
                if (interrupted()) {
                    _state.value = TextToSpeechEngine.State.NOT_READY
                    close()
                    return@channelFlow
                }
                _state.value = TextToSpeechEngine.State.SPEAKING
            }
            else -> {
                // speak refused or never started (engine error): text-only, no lie.
                _state.value = TextToSpeechEngine.State.NOT_READY
                close()
                return@channelFlow
            }
        }
        var t = 0.0
        while (engine.state.value.state == TtsState.SPEAKING) {
            if (interrupted()) break // interrupt() owns the state now (NOT_READY)
            // Decorative amplitude shaped like a calm syllable train.
            t += 0.35
            val amp = (0.55 + 0.45 * kotlin.math.sin(t)).toFloat().coerceIn(0f, 1f)
            trySend(amp)
            ticker(pollMs)
        }
        if (!interrupted()) _state.value = TextToSpeechEngine.State.READY
        close()
    }
}
