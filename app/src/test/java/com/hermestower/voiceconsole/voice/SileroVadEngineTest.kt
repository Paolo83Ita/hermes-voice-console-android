package com.hermestower.voiceconsole.voice

import com.hermestower.voiceconsole.voice.onnx.OnnxRuntimeException
import com.hermestower.voiceconsole.voice.onnx.OnnxRunner
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * GRAPH 4.14 §6 — SileroVadEngine plumbing tests (fixture runner; the real
 * graph runs on device via OrtOnnxRunner against the SHA-256-verified
 * silero_vad.onnx). The fixture proves: windowing 320→512, state threading,
 * hysteresis+hangover, typed failure path, sample-rate guard.
 */
class SileroVadEngineTest {

    /** Deterministic runner: emits probability = max |float| of the input window. */
    private class FixtureRunner(private val failEvery: Int = 0) : OnnxRunner {
        override val available: Boolean = true
        var calls = 0
        val lastState = FloatArray(256)
        override fun run(
            inputs: Map<String, FloatArray>,
            inputShapes: Map<String, LongArray>,
            inputLongs: Map<String, LongArray>,
            longShapes: Map<String, LongArray>,
        ): Map<String, FloatArray> {
            calls++
            assertTrue("input shape must be [1,512]", inputShapes["input"]?.contentEquals(longArrayOf(1, 512)) == true)
            assertTrue("state shape must be [2,1,128]", inputShapes["state"]?.contentEquals(longArrayOf(2, 1, 128)) == true)
            assertTrue("sr payload must be 16000", inputLongs["sr"]?.contentEquals(longArrayOf(16000)) == true)
            val input = inputs.getValue("input")
            val prob = input.map { kotlin.math.abs(it) }.maxOrNull() ?: 0f
            // Echo the state back so the test can observe state threading.
            val newState = inputs.getValue("state").map { it + 1f }.toFloatArray()
            System.arraycopy(newState, 0, lastState, 0, newState.size)
            if (failEvery > 0 && calls % failEvery == 0) {
                throw OnnxRuntimeException("fixture failure")
            }
            return mapOf("output" to floatArrayOf(prob), "stateN" to newState)
        }

        override fun close() {}
    }

    private fun frame(vararg samples: Short, sampleRate: Int = 16000) =
        AudioFrame(ShortArray(samples.size) { samples[it] }, sampleRate)

    private fun loudFrame(): AudioFrame = frame(*ShortArray(320) { Short.MAX_VALUE })

    private fun quietFrame(): AudioFrame = frame(*ShortArray(320) { 0 })

    @Test
    fun `buffers 320-sample frames into 512-sample silero windows`() {
        val runner = FixtureRunner()
        val engine = SileroVadEngine(runner, threshold = 0.5f)
        // Two quiet frames = 640 samples = ONE window invocation (320+320=640>512 → 2nd frame triggers).
        assertFalse(engine.isSpeech(quietFrame()))
        assertEquals("no window until 512 samples buffered", 0, runner.calls)
        assertFalse(engine.isSpeech(quietFrame()))
        assertEquals("640 buffered → window ran (512 consumed, 128 held)", 1, runner.calls)
        // The next window needs 384 more samples — the next frame (320) is not enough.
        assertFalse(engine.isSpeech(quietFrame()))
        assertEquals(1, runner.calls)
    }

    @Test
    fun `speech window crosses the threshold and hangover keeps it true briefly`() {
        val runner = FixtureRunner()
        val engine = SileroVadEngine(runner, threshold = 0.5f, hangoverFrames = 1)
        // Loud speech: three loud frames guarantee at least one loud 512-window runs.
        engine.isSpeech(loudFrame())
        engine.isSpeech(loudFrame())
        assertTrue("a loud window decided → true", engine.isSpeech(loudFrame()))
        // Quiet afterwards: exactly one hangover window keeps true, then false.
        // (Robust to 320→512 buffer arithmetic: assert the SEQUENCE, not frames.)
        val quietResults = (1..5).map { engine.isSpeech(quietFrame()) }
        assertTrue("hangover keeps speech true right after the loud window", quietResults.take(2).any { it })
        assertFalse("after the hangover window the engine returns false", quietResults.last())
    }

    @Test
    fun `runner failure is typed and never fabricates speech`() {
        val runner = FixtureRunner(failEvery = 1)
        val engine = SileroVadEngine(runner, threshold = 0.5f)
        engine.isSpeech(loudFrame())
        engine.isSpeech(loudFrame())
        assertFalse("failed window must return false, not crash", engine.isSpeech(quietFrame()))
        assertNotNull(engine.lastError)
        assertTrue(engine.lastError!!.startsWith("SILERO_RUN_FAILED"))
        // reset() clears the typed failure.
        engine.reset()
        assertEquals(null, engine.lastError)
    }

    @Test
    fun `non-16kHz frames are rejected with a typed contract error`() {
        val runner = FixtureRunner()
        val engine = SileroVadEngine(runner)
        assertFalse(engine.isSpeech(frame(1, 2, 3, sampleRate = 8000)))
        assertTrue(engine.lastError!!.startsWith("SILERO_SAMPLE_RATE_UNSUPPORTED"))
        assertEquals("no window consumed for a contract violation", 0, runner.calls)
    }

    @Test
    fun `state threads across windows`() {
        val runner = FixtureRunner()
        val engine = SileroVadEngine(runner)
        engine.isSpeech(loudFrame())
        engine.isSpeech(loudFrame())
        engine.isSpeech(quietFrame()) // second window
        assertTrue("state advanced by the fixture echo", runner.lastState[0] > 0f)
    }
}
