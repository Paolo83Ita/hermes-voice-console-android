package com.hermestower.voiceconsole.tts

import com.hermestower.voiceconsole.domain.TextToSpeechEngine
import com.hermestower.voiceconsole.voice.TtsEngine
import com.hermestower.voiceconsole.voice.TtsOutput
import com.hermestower.voiceconsole.voice.TtsState
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * GRAPH 4.9 REGRESSION REPAIR tests (e2e_14 / e2e_19_20 root cause):
 * interruption must be observable by an utterance flow that is still inside
 * the ready-wait or speak-start windows. The CP-G4-029 repair introduced the
 * honest windows but swallowed interrupt() inside them — the utterance was
 * spoken anyway and the console stayed SPEAKING past the Owner cancel.
 *
 * The fake engine mirrors AndroidSystemTtsEngine's state protocol:
 * speak() submits SYNTHESIZING synchronously; the injected ticker advances
 * the fake one step per poll AND yields, so every scenario is deterministic
 * on the single-threaded test scheduler (no real sleeping, no flakiness).
 */
class SystemTtsDomainAdapterTest {

    /** Manually driven fake: the ticker lambda advances it one step per poll. */
    private class FakeTtsEngine(
        private val step: (FakeTtsEngine) -> Unit,
        private val speakBlocked: Boolean = false,
    ) : TtsEngine {
        val _state = MutableStateFlow(TtsOutput(TtsState.LOADING))
        override val state: StateFlow<TtsOutput> = _state
        var loadCalls = 0
        var speakCalls = 0
        var interruptCalls = 0
        var steps = 0

        override fun load() {
            loadCalls++
        }

        override fun unload() {}

        override fun speak(text: String) {
            speakCalls++
            // Mirrors the real engine: a submitted utterance is SYNTHESIZING
            // until the async onStart flips it to SPEAKING; a fail-closed
            // refusal lands directly in TEXT_ONLY_SAFE_DEGRADED.
            _state.value = TtsOutput(
                if (speakBlocked) TtsState.TEXT_ONLY_SAFE_DEGRADED else TtsState.SYNTHESIZING,
            )
        }

        override fun interrupt() {
            interruptCalls++
            // Mirrors AndroidSystemTtsEngine.interrupt(): leaves the SPEAKING/
            // SYNTHESIZING path and returns to READY when initialised.
            if (_state.value.state != TtsState.LOADING) {
                _state.value = TtsOutput(TtsState.READY)
            }
        }

        fun advance() {
            steps++
            step(this)
        }
    }

    private fun adapter(
        engine: FakeTtsEngine,
        readyWaitMs: Long = 1_000L,
        speakStartWaitMs: Long = 1_000L,
        pollMs: Long = 10L,
    ) = SystemTtsDomainAdapter(
        engine = engine,
        readyWaitMs = readyWaitMs,
        speakStartWaitMs = speakStartWaitMs,
        pollMs = pollMs,
        // Each poll advances the fake AND yields to the test scheduler.
        ticker = {
            engine.advance()
            yield()
        },
    )

    // ---- 1. interrupt during the ready-wait window must prevent the utterance ----

    @Test
    fun `interrupt during ready wait prevents speak and completes not ready`() = runTest {
        val engine = FakeTtsEngine(step = { /* stuck in LOADING: TTS init never completes */ })
        val ad = adapter(engine)
        var amplitudes: List<Float> = emptyList()
        val collector = launch {
            amplitudes = ad.speak("benvenuto", 1.0f).toList()
        }
        // Dispatch rounds: yield 1 starts the collector (toList schedules the
        // channelFlow producer); yield 2 runs the producer INTO the ready-wait
        // loop where it suspends on the ticker. Cancel exactly there.
        yield()
        yield()
        ad.interrupt()
        collector.join()

        assertEquals("the cancelled utterance must NEVER be spoken", 0, engine.speakCalls)
        assertTrue("no amplitude may be emitted for a cancelled utterance", amplitudes.isEmpty())
        assertEquals(TextToSpeechEngine.State.NOT_READY, ad.state.value)
    }

    // ---- 2. interrupt during the speak-start window must not claim SPEAKING ----

    @Test
    fun `interrupt during speak start window stays not ready and emits nothing`() = runTest {
        val engine = FakeTtsEngine(step = { fake ->
            // After the submission the engine sits in SYNTHESIZING forever
            // (onStart never fires — the exact "start window" scenario).
            if (fake._state.value.state == TtsState.LOADING) {
                fake._state.value = TtsOutput(TtsState.READY)
            }
        })
        val ad = adapter(engine)
        var amplitudes: List<Float> = emptyList()
        val collector = launch {
            amplitudes = ad.speak("benvenuto", 1.0f).toList()
        }
        // Dispatch rounds: yield 1 starts the collector, yield 2 runs the
        // producer into the ready-wait ticker, yield 3 exits the loop, fires
        // speak() (SYNTHESIZING) and suspends inside the speak-start window.
        // Cancel exactly there, while the utterance is queued but not audible.
        yield()
        yield()
        yield()
        ad.interrupt()
        collector.join()

        assertEquals(1, engine.speakCalls) // speak was already submitted before the cancel
        assertTrue("no SPEAKING amplitude may be emitted after the cancel", amplitudes.isEmpty())
        assertEquals(TextToSpeechEngine.State.NOT_READY, ad.state.value)
        assertTrue("the engine must have been told to stop the queued utterance", engine.interruptCalls >= 1)
    }

    // ---- 3. the honest happy path still works (regression guard) ----

    @Test
    fun `happy path emits amplitude then completes ready`() = runTest {
        val engine = FakeTtsEngine(step = { fake ->
            fake._state.value = when {
                fake._state.value.state == TtsState.LOADING -> TtsOutput(TtsState.READY)
                fake._state.value.state == TtsState.SYNTHESIZING -> TtsOutput(TtsState.SPEAKING) // onStart
                fake._state.value.state == TtsState.SPEAKING && fake.steps >= 8 ->
                    TtsOutput(TtsState.READY) // utterance done
                else -> fake._state.value
            }
        })
        val ad = adapter(engine)
        val result = async {
            ad.speak("benvenuto", 1.0f).toList()
        }.await()

        assertEquals(1, engine.speakCalls)
        assertTrue("real audio must be shown as amplitude", result.isNotEmpty())
        assertEquals(TextToSpeechEngine.State.READY, ad.state.value)
    }

    // ---- 4. the fail-closed refusal still completes honestly (no SPEAKING lie) ----

    @Test
    fun `fail closed refusal completes without amplitude`() = runTest {
        val engine = FakeTtsEngine(
            step = { fake ->
                if (fake._state.value.state == TtsState.LOADING) {
                    fake._state.value = TtsOutput(TtsState.READY)
                }
            },
            speakBlocked = true, // Italian requested, non-Italian read-back: REFUSED
        )
        val ad = adapter(engine)
        val amplitudes: List<Float> = async {
            ad.speak("risposta", 1.0f).toList()
        }.await()

        assertEquals("the refused utterance must stay text-only", emptyList<Float>(), amplitudes)
        assertEquals(TextToSpeechEngine.State.NOT_READY, ad.state.value)
    }

    // ---- 5. interrupt after natural completion leaves the adapter at rest ----

    @Test
    fun `interrupt while speaking ends not ready without overriding the cancel`() = runTest {
        val engine = FakeTtsEngine(step = { fake ->
            fake._state.value = when {
                fake._state.value.state == TtsState.LOADING -> TtsOutput(TtsState.READY)
                fake._state.value.state == TtsState.SYNTHESIZING -> TtsOutput(TtsState.SPEAKING)
                else -> fake._state.value // speaks forever until interrupted
            }
        })
        val ad = adapter(engine)
        val job = launch { ad.speak("benvenuto", 1.0f).toList() }
        yield() // wait-loop ticker: LOADING→READY
        yield() // speak() fired (SYNTHESIZING→SPEAKING), suspended in the start window
        ad.interrupt()
        job.join()

        assertEquals(TextToSpeechEngine.State.NOT_READY, ad.state.value)
    }
}
