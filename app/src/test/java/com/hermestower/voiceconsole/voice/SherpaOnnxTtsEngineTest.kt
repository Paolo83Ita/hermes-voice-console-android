package com.hermestower.voiceconsole.voice

import kotlinx.coroutines.flow.StateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * CP-G4.9-003 unit proof for the REAL natural Italian TTS route.
 * The native bridge is a deterministic fake: the real com.k2fsa.sherpa.onnx
 * binding lives only in SherpaRealNativeBridge (device code).
 */
class SherpaOnnxTtsEngineTest {

    private class FakeNative(private val fail: Boolean = false) : SherpaOnnxTtsEngine.NativeBridge {
        var created = 0
        var released = 0
        var lastSpeed = Float.NaN
        override fun createVits(modelPath: String, tokensPath: String, dataDir: String): Any {
            created++
            if (fail) throw SherpaOnnxTtsEngine.TtsNativeException("synthetic native failure")
            return "handle"
        }

        override fun generate(handle: Any, text: String, speed: Float): FloatArray {
            lastSpeed = speed
            return FloatArray(1_600) { 0.25f } // 100 ms of a real-shaped signal
        }

        override fun sampleRate(handle: Any): Int = 16_000
        override fun release(handle: Any) {
            released++
        }
    }

    private fun installedVoiceDir(root: File): File {
        val dir = File(root, "vits-piper-it_IT-riccardo-x_low")
        dir.mkdirs()
        File(dir, "it_IT-riccardo-x_low.onnx").writeBytes(byteArrayOf(1))
        File(dir, "tokens.txt").writeText("a 0\nb 1\n")
        File(dir, "espeak-ng-data").mkdirs()
        return root
    }

    /** Deterministic playback fake: records the call, completes asynchronously. */
    private class FakePlayer : SherpaOnnxTtsEngine.PlayerBridge {
        var lastPlaybackRate = -1
        var playCalls = 0
        var stopCalls = 0
        private var onDone: Runnable? = null
        override fun play(pcm: ShortArray, sampleRate: Int, playbackRate: Int, onDone: Runnable): Boolean {
            playCalls++
            lastPlaybackRate = playbackRate
            this.onDone = onDone
            return true
        }
        fun complete() {
            onDone?.run()
        }
        override fun stop() {
            stopCalls++
            onDone = null
        }
    }

    private fun engine(root: File, native: SherpaOnnxTtsEngine.NativeBridge, player: FakePlayer = FakePlayer()) =
        SherpaOnnxTtsEngine(
            voiceRoot = root,
            voiceIdentity = "vits-piper-it_IT-riccardo-x_low",
            native = native,
            player = player,
            speechProfileProvider = { SpeechProfile(speechRate = 0.96f, pitch = 0.95f) },
            telemetry = TtsTelemetry(),
        )

    // ---- bundle resolution ----

    @Test
    fun `voice bundle resolves one level deep and requires model tokens espeak data`() {
        val root = installedVoiceDir(createTempDir())
        val e = engine(root, FakeNative())
        assertEquals("vits-piper-it_IT-riccardo-x_low", e.resolveVoiceDir()?.name)
        // An incomplete bundle (no espeak-ng-data) must NOT resolve.
        val broken = createTempDir()
        File(broken, "model.onnx").writeBytes(byteArrayOf(1))
        File(broken, "tokens.txt").writeText("a 0\n")
        assertNull(engine(broken, FakeNative()).resolveVoiceDir())
    }

    // ---- honest states ----

    @Test
    fun `not installed reports NOT_INSTALLED and never claims the natural voice`() {
        val e = engine(createTempDir(), FakeNative())
        e.load()
        assertEquals(TtsState.NOT_INSTALLED, e.state.value.state)
        assertTrue(e.engineId.contains("not installed"))
    }

    @Test
    fun `load binds the verified bundle and reports READY with the real identity`() {
        val root = installedVoiceDir(createTempDir())
        val e = engine(root, FakeNative())
        e.load()
        assertEquals(TtsState.READY, e.state.value.state)
        assertEquals("sherpa-onnx v1.12.24 + vits-piper-it_IT-riccardo-x_low", e.engineId)
    }

    @Test
    fun `native failure is typed FAILED with the reason`() {
        val root = installedVoiceDir(createTempDir())
        val e = engine(root, FakeNative(fail = true))
        e.load()
        assertEquals(TtsState.FAILED, e.state.value.state)
        assertTrue(e.state.value.error!!.contains("synthetic native failure"))
    }

    // ---- speak: synthesis runs, profile applies, gate uniformity holds ----

    @Test
    fun `speak moves through SYNTHESIZING to SPEAKING with the profile speed`() {
        val root = installedVoiceDir(createTempDir())
        val native = FakeNative()
        val player = FakePlayer()
        val e = engine(root, native, player)
        e.load()
        e.speak("Benvenuto nella console vocale di Hermes Tower.")
        // speak() dispatches synthesis to the worker thread; wait bounded for
        // the real playback start, then verify the profile application.
        val deadline = System.currentTimeMillis() + 2_000
        while (System.currentTimeMillis() < deadline && e.state.value.state != TtsState.SPEAKING) {
            Thread.sleep(10)
        }
        assertEquals(TtsState.SPEAKING, e.state.value.state)
        assertEquals(0.96f, native.lastSpeed)
        // Pitch 0.95 applies as a REAL resampling playback rate (16000 * 0.95).
        assertEquals((16_000 * 0.95f).toInt(), player.lastPlaybackRate)
        // The read-back is ITALIAN by construction (the voice IS Italian).
        assertTrue(e.state.value.voiceReadback?.isItalianAudible == true)
        // Completion marker → honest READY.
        player.complete()
        assertEquals(TtsState.READY, e.state.value.state)
    }

    @Test
    fun `interrupt during playback ends the utterance`() {
        val root = installedVoiceDir(createTempDir())
        val player = FakePlayer()
        val e = engine(root, FakeNative(), player)
        e.load()
        e.speak("Benvenuto.")
        val deadline = System.currentTimeMillis() + 2_000
        while (System.currentTimeMillis() < deadline && e.state.value.state != TtsState.SPEAKING) {
            Thread.sleep(10)
        }
        e.interrupt()
        assertEquals(1, player.stopCalls)
        assertEquals(TtsState.READY, e.state.value.state)
    }

    @Test
    fun `unload releases the native handle and reports UNLOADED`() {
        val root = installedVoiceDir(createTempDir())
        val native = FakeNative()
        val e = engine(root, native)
        e.load()
        e.unload()
        assertEquals(TtsState.UNLOADED, e.state.value.state)
        assertEquals(1, native.released)
        assertNull(e.loadedVoiceDir)
    }
}
