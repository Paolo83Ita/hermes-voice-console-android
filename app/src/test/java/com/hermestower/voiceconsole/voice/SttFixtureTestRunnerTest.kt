package com.hermestower.voiceconsole.voice

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * CP-G4.9-004 unit proof: the deterministic Italian fixture test feeds REAL
 * speech audio (the bundled WAV, spoken by the verified piper voice) through
 * the vosk frame engine with an injected scripted decoder — the full
 * load → accept → finalize → unload path, on the JVM.
 */
class SttFixtureTestRunnerTest {

    /** Scripted decoder: emits the transcript when the audio stream ends. */
    private class ScriptedDecoder(private val finalText: String) : VoskFrameSttEngine.FrameDecoder {
        var frames = 0
        var totalSamples = 0L
        override fun acceptWaveForm(recognizer: Any, samples: ShortArray): Boolean {
            frames++
            totalSamples += samples.size
            return false
        }

        fun flush(): Boolean {
            // The real bridge reads finalResultText at finalize; the runner
            // handles it via engine.finalizeTranscript(), so nothing here.
            return false
        }
    }

    /** Full-protocol fake SttEngine (no native, no subclassing). */
    private class ProtocolFake(
        private val finalText: String,
        private val loadOk: Boolean = true,
    ) : SttEngine {
        val _state = MutableStateFlow(SttOutput(SttState.NOT_INSTALLED))
        override val state: StateFlow<SttOutput> = _state
        var loadCalls = 0
        var unloadCalls = 0
        var acceptedSamples = 0L
        override fun load() {
            loadCalls++
            _state.value = SttOutput(if (loadOk) SttState.READY else SttState.FAILED, error = if (loadOk) null else SttError(SttError.Kind.MODEL_ERROR, "not installed"))
        }
        override fun unload() {
            unloadCalls++
            _state.value = SttOutput(SttState.UNLOADED)
        }
        override fun accept(frame: AudioFrame): SttOutput {
            acceptedSamples += frame.samples.size
            return SttOutput(SttState.TRANSCRIBING, partialTranscript = "")
        }
        override fun finalizeTranscript(): SttOutput =
            SttOutput(SttState.FINALIZING, finalTranscript = finalText)
        override fun reset() {}
    }

    private fun fakeModelDir(root: File): File {
        val dir = File(root, "vosk-model-small-it-0.22")
        dir.mkdirs()
        File(dir, "conf").mkdirs()
        File(dir, "graph").mkdirs()
        File(dir, "am").mkdirs()
        File(dir, "final.mdl").writeBytes(byteArrayOf(1))
        return root
    }

    @Test
    fun `parses the real bundled fixture as mono 16k pcm16`() {
        val bytes = javaClass.getResourceAsStream("/models/fixtures/fixture_benvenuto_torre_it.wav")!!.readBytes()
        val pcm = SttFixtureTestRunner.parsePcm16Mono16k(bytes)
        // 3.15 s at 16 kHz ≈ 50 400 samples (manifest says 100 908 bytes WAV).
        assertTrue("expected ~50k samples, got ${pcm.size}", pcm.size in 49_000..52_000)
        // Real speech: non-silent content.
        assertTrue(pcm.any { it > Short.MAX_VALUE / 4 })
    }

    @Test
    fun `rejects a wrong-format wav with a typed error`() {
        val bytes = ByteArray(64) // not a RIFF file
        val fake = ProtocolFake(finalText = "")
        val r = SttFixtureTestRunner.run(engine = fake, wavBytes = bytes)
        assertEquals(false, r.success)
        assertTrue(r.error!!.contains("RIFF"))
        assertEquals("a malformed fixture never reaches the engine", 0, fake.loadCalls)
    }

    @Test
    fun `runner drives load accept finalize unload and returns the engine transcript`() {
        val fake = ProtocolFake(finalText = "benvenuto alla torre di hermes il canale vocale è pronto")
        val bytes = javaClass.getResourceAsStream("/models/fixtures/fixture_benvenuto_torre_it.wav")!!.readBytes()
        val r = SttFixtureTestRunner.run(fake, bytes)
        assertTrue("fixture run must succeed: ${r.error}", r.success)
        assertEquals(
            "benvenuto alla torre di hermes il canale vocale è pronto",
            r.transcript,
        )
        // ~31 frames of 100 ms for 3.15 s of audio; the WHOLE utterance was fed.
        assertTrue("expected ~31 frames, got ${r.frameCount}", r.frameCount in 29..34)
        assertTrue("all samples must reach the engine", fake.acceptedSamples > 49_000)
        assertEquals("the model is released after the test", 1, fake.unloadCalls)
    }

    @Test
    fun `not-installed model yields typed failure without fabricating a transcript`() {
        val fake = ProtocolFake(finalText = "SHOULD NEVER APPEAR", loadOk = false)
        val bytes = javaClass.getResourceAsStream("/models/fixtures/fixture_benvenuto_torre_it.wav")!!.readBytes()
        val r = SttFixtureTestRunner.run(fake, bytes)
        assertEquals(false, r.success)
        assertNotNull(r.error)
        assertTrue(r.error!!.contains("not installed"))
    }
}
