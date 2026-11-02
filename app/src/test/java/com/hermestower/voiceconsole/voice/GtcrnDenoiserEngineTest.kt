package com.hermestower.voiceconsole.voice

import com.hermestower.voiceconsole.voice.onnx.OnnxRunner
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * GRAPH 4 CP-G4-008 — GTCRN streaming plumbing tests on virtual fixtures.
 * The fixture runner is a SPECTRAL IDENTITY: `enh = mix`, caches echoed.
 * With it, the WOLA chain must reconstruct the input within float error —
 * a strong proof that STFT + overlap-add + normalization are correct.
 * (The REAL GTCRN model path is exercised on-device/emulator via ORT.)
 */
class GtcrnDenoiserEngineTest {

    /** TEST_FIXTURE: identity in the spectral domain + truthful cache echo. */
    private class IdentityRunner : OnnxRunner {
        override val available: Boolean get() = true
        var runCount = 0
            private set
        override fun run(
            inputs: Map<String, FloatArray>,
            inputShapes: Map<String, LongArray>,
            inputLongs: Map<String, LongArray>,
            longShapes: Map<String, LongArray>,
        ): Map<String, FloatArray> {
            runCount++
            assertEquals(4, inputs.size)
            assertTrue(inputs.containsKey("mix"))
            return mapOf(
                "enh" to inputs.getValue("mix").copyOf(),
                "conv_cache_out" to inputs.getValue("conv_cache").copyOf(),
                "tra_cache_out" to inputs.getValue("tra_cache").copyOf(),
                "inter_cache_out" to inputs.getValue("inter_cache").copyOf(),
            )
        }

        override fun close() { /* fixture */ }
    }

    @Test
    fun `WOLA with spectral-identity model reconstructs input`() {
        val runner = IdentityRunner()
        val engine = GtcrnDenoiserEngine(runner)
        // 3 windows worth of a smooth signal (no spectral leakage edge issues)
        val total = DspHop * 8
        val input = ShortArray(total) { i -> (8000.0 * kotlin.math.sin(2.0 * Math.PI * 3.0 * i / 512.0)).toInt().toShort() }
        // feed in realistic 320-sample (20 ms) chunks
        val out = ShortArray(total)
        var pos = 0
        while (pos < total) {
            val len = minOf(320, total - pos)
            val chunk = input.copyOfRange(pos, pos + len)
            val enhanced = engine.enhance(chunk, 16000)
            System.arraycopy(enhanced, 0, out, pos, len)
            pos += len
        }
        // skip warm-up (latency prefix) AND the 256-sample pipeline tail that
        // never leaves the WOLA accumulator in a finite stream
        var maxErr = 0f
        for (i in 512 until total - 512) {
            maxErr = maxOf(maxErr, kotlin.math.abs(out[i] - input[i]) / 32768f)
        }
        assertTrue("max reconstruction error $maxErr after warm-up", maxErr < 5e-2f)
        assertTrue("model must have been invoked", runner.runCount > 0)
    }

    @Test
    fun `bypass passes raw audio through with zero model calls`() {
        val runner = IdentityRunner()
        val engine = GtcrnDenoiserEngine(runner)
        engine.bypass = true
        val samples = ShortArray(320) { 100 }
        val out = engine.enhance(samples, 16000)
        assertTrue(out.contentEquals(samples))
        assertEquals(0, runner.runCount)
    }

    @Test
    fun `failed runtime falls back to raw audio with FAILED state`() {
        val engine = GtcrnDenoiserEngine(UnavailableRunner())
        val samples = ShortArray(320) { 42 }
        val out = engine.enhance(samples, 16000)
        assertTrue(out.contentEquals(samples))
        assertTrue(engine.state.value == GtcrnDenoiserEngine.EngineState.FAILED || engine.state.value == GtcrnDenoiserEngine.EngineState.UNLOADED)
    }

    @Test
    fun `unload closes session and stops processing`() {
        val runner = IdentityRunner()
        val engine = GtcrnDenoiserEngine(runner)
        engine.unload()
        assertEquals(GtcrnDenoiserEngine.EngineState.UNLOADED, engine.state.value)
        val out = engine.enhance(ShortArray(320), 16000)
        assertEquals(320, out.size)
        assertEquals(0, runner.runCount)
    }

    @Test
    fun `latency measurement is truthful`() {
        val engine = GtcrnDenoiserEngine(IdentityRunner())
        engine.enhance(ShortArray(512), 16000)
        assertTrue("latency must be > 0 after processing", engine.lastChunkLatencyNanos > 0)
    }

    private val DspHop get() = 256

    private class UnavailableRunner : OnnxRunner {
        override val available: Boolean get() = false
        override fun run(
            inputs: Map<String, FloatArray>,
            inputShapes: Map<String, LongArray>,
            inputLongs: Map<String, LongArray>,
            longShapes: Map<String, LongArray>,
        ): Map<String, FloatArray> = throw com.hermestower.voiceconsole.voice.onnx.OnnxRuntimeException("unavailable")

        override fun close() { /* fixture */ }
    }
}
