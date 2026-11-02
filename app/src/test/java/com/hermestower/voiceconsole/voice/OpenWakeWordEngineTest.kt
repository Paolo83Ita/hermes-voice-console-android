package com.hermestower.voiceconsole.voice

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * GRAPH 4 CP-G4-008 — wake-word logic tests on deterministic fixtures
 * (Owner §9: threshold, cooldown, duplicate suppression, false-accept counter,
 * OFF default, FA/FR harness). The REAL three-model chain is exercised on
 * emulator/device; these tests prove the LOGIC around the chain.
 */
class OpenWakeWordEngineTest {

    /** TEST_FIXTURE frontend: every mel window → a scripted embedding. */
    private class FixtureFrontend : MelEmbedFrontend {
        var embedCount = 0
        override fun embed(melWindow: FloatArray): FloatArray {
            embedCount++
            require(melWindow.size == OpenWakeWordEngine.MEL_WINDOW * OpenWakeWordEngine.SAMPLES_PER_MEL_FRAME) {
                "bad mel window size ${melWindow.size}"
            }
            return FloatArray(OpenWakeWordEngine.EMBED_DIM) { 0.1f }
        }
    }

    /** TEST_FIXTURE head: scripted scores for the FA/FR harness. */
    private class FixtureScorer(vararg val scores: Float) : WakeScorer {
        private var i = 0
        val given = ArrayList<Float>()
        override fun score(embeddingWindow: FloatArray): Float {
            require(embeddingWindow.size == OpenWakeWordEngine.RING_EMBEDS * OpenWakeWordEngine.EMBED_DIM)
            val s = scores[minOf(i, scores.size - 1)]
            i++
            given.add(s)
            return s
        }
    }

    private fun melWindowSamples(): Int =
        OpenWakeWordEngine.MEL_WINDOW * OpenWakeWordEngine.SAMPLES_PER_MEL_FRAME // 76×160

    /** Feeds [count] full mel windows, one decision window each, [stepMs] apart. */
    private fun feedWindows(engine: OpenWakeWordEngine, count: Int, startMs: Long = 0, stepMs: Long = 10_000L): Boolean {
        var fired = false
        repeat(count) { idx ->
            val f = engine.feed(ShortArray(melWindowSamples()), 16000, startMs + idx * stepMs)
            fired = fired || f
        }
        return fired
    }

    @Test
    fun `OFF by default - feed never fires before enable`() {
        val engine = OpenWakeWordEngine(FixtureFrontend(), FixtureScorer(0.99f))
        assertFalse(engine.isReady)
        assertFalse(engine.feed(ShortArray(melWindowSamples()), 16000, nowMs = 0))
        assertEquals(OpenWakeWordEngine.EngineState.OFF, engine.state.value)
    }

    @Test
    fun `enable without chain is FAILED - honest state`() {
        val engine = OpenWakeWordEngine(null, null)
        engine.enable()
        assertEquals(OpenWakeWordEngine.EngineState.FAILED, engine.state.value)
    }

    @Test
    fun `fires only after full 16-embedding ring and above threshold`() {
        val fe = FixtureFrontend()
        val sc = FixtureScorer(0.1f, 0.99f)
        val engine = OpenWakeWordEngine(fe, sc)
        engine.enable()
        // windows 1..15 fill the ring (no decisions); window 16 scores 0.1 → no fire;
        // window 17 scores 0.99 → FIRE
        assertFalse(feedWindows(engine, 16))
        assertEquals(1, sc.given.size)
        assertTrue(feedWindows(engine, 1, startMs = 160_000))
        assertEquals(1, engine.acceptedCount)
        assertEquals(0, engine.falseAcceptCounter)
        assertEquals(17, fe.embedCount)
    }

    @Test
    fun `cooldown suppresses immediate re-trigger and counts it`() {
        val fe = FixtureFrontend()
        val sc = FixtureScorer(0.99f, 0.99f)
        val engine = OpenWakeWordEngine(fe, sc)
        engine.enable()
        // decision windows 100 ms apart (well inside the 2 s cooldown)
        assertTrue(feedWindows(engine, 16, stepMs = 100)) // ring fill + fire
        assertEquals(1, engine.acceptedCount)
        // next decision 100 ms later: cooldown (2 s) suppresses, counter increments
        val firedAgain = feedWindows(engine, 1, startMs = 1_600, stepMs = 100)
        assertFalse(firedAgain)
        assertEquals(1, engine.acceptedCount)
        assertEquals(1, engine.falseAcceptCounter)
    }

    @Test
    fun `fires again after cooldown elapses`() {
        val fe = FixtureFrontend()
        val sc = FixtureScorer(0.99f, 0.99f)
        val engine = OpenWakeWordEngine(fe, sc)
        engine.enable()
        assertTrue(feedWindows(engine, 16))
        assertTrue(feedWindows(engine, 1, startMs = 160_000 + 3_000))
        assertEquals(2, engine.acceptedCount)
        assertEquals(0, engine.falseAcceptCounter)
    }

    @Test
    fun `false reject - scores below threshold never fire`() {
        val fe = FixtureFrontend()
        val sc = FixtureScorer(0.49f, 0.0f, 0.2f, 0.3f, 0.49f, 0.1f)
        val engine = OpenWakeWordEngine(fe, sc)
        engine.enable()
        assertFalse(feedWindows(engine, 21)) // 16 fill + 6 decisions, all below
        assertEquals(0, engine.acceptedCount)
        assertEquals(0, engine.falseAcceptCounter)
        assertEquals(6, sc.given.size)
    }

    @Test
    fun `unload returns to OFF and feed is inert`() {
        val fe = FixtureFrontend()
        val sc = FixtureScorer(0.99f)
        val engine = OpenWakeWordEngine(fe, sc)
        engine.enable()
        engine.unload()
        assertEquals(OpenWakeWordEngine.EngineState.OFF, engine.state.value)
        assertFalse(engine.feed(ShortArray(melWindowSamples()), 16000, nowMs = 0))
        assertEquals(0, fe.embedCount)
    }

    @Test
    fun `wrong sample rate is a typed failure - no hidden resampling`() {
        val fe = FixtureFrontend()
        val engine = OpenWakeWordEngine(fe, FixtureScorer(0.99f))
        engine.enable()
        engine.feed(ShortArray(melWindowSamples()), 44100, nowMs = 0)
        assertEquals(OpenWakeWordEngine.EngineState.FAILED, engine.state.value)
    }

    @Test
    fun `resetForTest clears counters for FA-FR harness runs`() {
        val engine = OpenWakeWordEngine(FixtureFrontend(), FixtureScorer(0.99f))
        engine.enable()
        assertTrue(feedWindows(engine, 16))
        assertEquals(1, engine.acceptedCount)
        engine.resetForTest()
        assertEquals(0, engine.acceptedCount)
        assertEquals(0, engine.falseAcceptCounter)
    }
}
