package com.hermestower.voiceconsole.voice

import com.hermestower.voiceconsole.core.mode.OperatingMode
import com.hermestower.voiceconsole.core.routing.Connectivity
import com.hermestower.voiceconsole.core.routing.RuleBasedIntentRouter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * GRAPH 4 NODE 3 (CP-G4-005): voice pipeline controller tests on deterministic
 * scripted frames — routing attribution, privacy mute physics, barge-in,
 * no-speech truthfulness, typed failures and the TTS fallback chain.
 */
class VoicePipelineControllerTest {

    // ---- deterministic fakes -------------------------------------------------

    /** Scripted frame source: emits given frames, records lifecycle facts. */
    private class ScriptedSource(private val frames: List<AudioFrame>) : AudioFrameSource {
        var startedCount = 0
        var stoppedCount = 0
        private var idx = 0
        override val isActive: Boolean get() = startedCount > stoppedCount
        override fun start() { startedCount++ }
        override fun stop() { stoppedCount++ }
        override suspend fun read(): AudioFrame? {
            if (stoppedCount > startedCount) return null
            if (idx >= frames.size) return null
            return frames[idx++]
        }
    }

    /** Fake STT: builds a transcript from the number of accepted frames. */
    private class FakeStt(
        private val transcript: String,
        private val failOnFinalize: Boolean = false,
    ) : SttEngine {
        var acceptedFrames = 0
        var loads = 0
        var unloads = 0
        var resets = 0
        private val _state = MutableStateFlow(SttOutput(SttState.NOT_INSTALLED))
        override val state = _state
        override fun load() {
            loads++
            // Loading always reaches READY here; the scripted failure (if any)
            // fires at finalize time, exercising the finalize-failure path.
            _state.value = SttOutput(SttState.READY)
        }
        override fun unload() { unloads++; _state.value = SttOutput(SttState.UNLOADED) }
        override fun accept(frame: AudioFrame): SttOutput {
            acceptedFrames++
            _state.value = SttOutput(SttState.LISTENING, partialTranscript = if (acceptedFrames > 2) "par…" else null)
            return _state.value
        }
        override fun finalizeTranscript(): SttOutput {
            _state.value = if (failOnFinalize) {
                SttOutput(SttState.FAILED, error = SttError(SttError.Kind.MODEL_ERROR, "scripted failure"))
            } else {
                SttOutput(SttState.READY, finalTranscript = transcript, confidence = 0.93f)
            }
            return _state.value
        }
        override fun reset() { resets++; acceptedFrames = 0 }
    }

    /** Fake TTS: records speak/interrupt calls with honest states. */
    private class FakeTts(private val works: Boolean = true) : TtsEngine {
        val spokenTexts = mutableListOf<String>()
        var interrupts = 0
        private val _state = MutableStateFlow(TtsOutput(TtsState.NOT_INSTALLED))
        override val state = _state
        override fun load() { _state.value = if (works) TtsOutput(TtsState.READY) else TtsOutput(TtsState.FAILED, error = "no engine") }
        override fun unload() { _state.value = TtsOutput(TtsState.UNLOADED) }
        override fun speak(text: String) {
            spokenTexts += text
            _state.value = if (works) TtsOutput(TtsState.SPEAKING, spokenText = text) else TtsOutput(TtsState.FAILED, error = "speak failed")
        }
        override fun interrupt() { interrupts++; _state.value = TtsOutput(TtsState.READY) }
    }

    private fun speechFrame(ms: Int = 100) = AudioFrame(ShortArray(ms * 16) { (800 * kotlin.math.sin(it.toDouble() * 0.3)).toInt().toShort() })
    private fun silenceFrame(ms: Int = 100) = AudioFrame(ShortArray(ms * 16))

    private fun controller(
        frames: List<AudioFrame>,
        stt: SttEngine,
        tts: TtsEngine?,
        mode: OperatingMode = OperatingMode.HYBRID,
        connectivity: Connectivity = Connectivity.OK,
        localResponse: String? = "Fatto.",
        towerResponse: String? = "Ecco la risposta della Tower.",
        noSpeechTimeoutMs: Long = 400,
        history: (VoiceTurnRecord) -> Unit = {},
    ): VoicePipelineController = VoicePipelineController(
        sourceFactory = { ScriptedSource(frames) },
        vad = EnergyVadEngine(),
        stt = stt,
        tts = tts,
        router = RuleBasedIntentRouter(),
        modeProvider = { mode },
        connectivityProvider = { connectivity },
        localExecutor = { localResponse },
        towerExecutor = { towerResponse },
        historySink = history,
        noSpeechTimeoutMs = noSpeechTimeoutMs,
        framePollDelayMs = 1,
    )

    // ---- tests ----------------------------------------------------------------

    @Test
    fun `happy path — local greeting is transcribed, handled locally, attributed and spoken`() = runBlocking {
        val frames = List(6) { speechFrame() } + List(10) { silenceFrame() }
        val stt = FakeStt("Ciao Hermes, qual è lo stato della console?")
        val tts = FakeTts()
        lateinit var record: VoiceTurnRecord
        val c = controller(frames, stt, tts) { record = it }
        val result = c.listenOnce()
        assertEquals("Ciao Hermes, qual è lo stato della console?", result.transcript)
        assertEquals(VoiceTurnRecord.ROUTE_LOCAL, result.route)
        assertEquals(VoiceTurnRecord.ATTRIBUTION_LOCAL, result.attribution)
        assertEquals("Fatto.", result.responseText)
        assertTrue(result.spoken)
        assertEquals(listOf("Fatto."), tts.spokenTexts)
        assertNotNull(record.transcript)
        assertEquals(0.93f, result.confidence!!)
    }

    @Test
    fun `tower-required request is attributed Sent to Hermes Tower`() = runBlocking {
        val frames = List(6) { speechFrame() } + List(10) { silenceFrame() }
        val stt = FakeStt("Hermes, mostrami l'ultima missione della Tower e poi modifica il documento finale")
        val tts = FakeTts()
        val c = controller(frames, stt, tts)
        val result = c.listenOnce()
        assertEquals(VoiceTurnRecord.ROUTE_TOWER, result.route)
        assertEquals(VoiceTurnRecord.ATTRIBUTION_TOWER, result.attribution)
        assertEquals("Ecco la risposta della Tower.", result.responseText)
        assertTrue(result.spoken)
    }

    @Test
    fun `privacy mute physically stops the source and refuses turns`() = runBlocking {
        val frames = List(30) { speechFrame() }
        val stt = FakeStt("non dovrebbe mai arrivare qui")
        val c = controller(frames, stt, FakeTts())
        c.setMuted(true)
        assertTrue(c.muted.value)
        val result = c.listenOnce()
        assertEquals("MUTED", result.errorCode)
        assertEquals(0, stt.acceptedFrames) // no frame ever reached the STT
        c.setMuted(false)
        assertFalse(c.muted.value)
    }

    @Test
    fun `mute mid-turn stops capture within one frame`() = runBlocking {
        val stt = FakeStt("frase a metà")
        lateinit var source: ScriptedSource
        val longFrames = List(200) { speechFrame() }
        val c = VoicePipelineController(
            sourceFactory = { ScriptedSource(longFrames).also { source = it } },
            vad = EnergyVadEngine(),
            stt = stt,
            tts = FakeTts(),
            router = RuleBasedIntentRouter(),
            modeProvider = { OperatingMode.HYBRID },
            connectivityProvider = { Connectivity.OK },
            localExecutor = { "ok" },
            towerExecutor = { "tower" },
            framePollDelayMs = 1,
        )
        var acceptedAtMute = -1
        val job = launch {
            kotlinx.coroutines.delay(30)
            acceptedAtMute = stt.acceptedFrames
            c.setMuted(true)
            kotlinx.coroutines.delay(60)
            assertEquals("capture stopped after mute", acceptedAtMute, stt.acceptedFrames)
        }
        val result = c.listenOnce()
        job.join()
        assertEquals("MUTED", result.errorCode)
        assertTrue(source.stoppedCount >= 1)
    }

    @Test
    fun `no speech ends the turn truthfully with no Tower contact`() = runBlocking {
        val frames = List(20) { silenceFrame() }
        val stt = FakeStt("nothing")
        var towerCalled = false
        val c = VoicePipelineController(
            sourceFactory = { ScriptedSource(frames) },
            vad = EnergyVadEngine(),
            stt = stt,
            tts = FakeTts(),
            router = RuleBasedIntentRouter(),
            modeProvider = { OperatingMode.HYBRID },
            connectivityProvider = { Connectivity.OK },
            localExecutor = { "local" },
            towerExecutor = { towerCalled = true; "tower" },
            noSpeechTimeoutMs = 300,
            framePollDelayMs = 1,
        )
        val result = c.listenOnce()
        assertEquals("NO_SPEECH", result.errorCode)
        assertEquals(VoiceTurnRecord.ROUTE_NONE, result.route)
        assertFalse(towerCalled)
        assertNull(result.responseText)
    }

    @Test
    fun `stt failure is typed and never crashes the pipeline`() = runBlocking {
        val frames = List(6) { speechFrame() } + List(10) { silenceFrame() }
        val stt = FakeStt("whatever", failOnFinalize = true)
        val c = controller(frames, stt, FakeTts())
        val result = c.listenOnce()
        assertEquals("STT_MODEL_ERROR", result.errorCode)
        assertEquals(VoiceTurnRecord.ROUTE_NONE, result.route)
    }

    @Test
    fun `tts unavailable degrades to text-only response`() = runBlocking {
        val frames = List(6) { speechFrame() } + List(10) { silenceFrame() }
        val stt = FakeStt("Ciao Hermes")
        val c = controller(frames, stt, tts = null)
        val result = c.listenOnce()
        assertEquals(VoiceTurnRecord.ROUTE_LOCAL, result.route)
        assertEquals("Fatto.", result.responseText)
        assertFalse(result.spoken) // text-only fallback (§10 tier 3)
    }

    @Test
    fun `cancel mid-listen is typed and stops the source`() = runBlocking {
        val longFrames = List(200) { speechFrame() }
        val stt = FakeStt("frase lunga")
        lateinit var source: ScriptedSource
        val c = VoicePipelineController(
            sourceFactory = { ScriptedSource(longFrames).also { source = it } },
            vad = EnergyVadEngine(),
            stt = stt,
            tts = FakeTts(),
            router = RuleBasedIntentRouter(),
            modeProvider = { OperatingMode.HYBRID },
            connectivityProvider = { Connectivity.OK },
            localExecutor = { "ok" },
            towerExecutor = { "tower" },
            framePollDelayMs = 1,
        )
        val job = launch {
            kotlinx.coroutines.delay(40)
            c.cancel()
        }
        val result = c.listenOnce()
        job.join()
        assertEquals("CANCELLED", result.errorCode)
        assertTrue(source.stoppedCount >= 1)
    }

    @Test
    fun `no raw audio retention — frames consumed in-flight, no buffer API`() = runBlocking {
        val frames = List(6) { speechFrame() } + List(10) { silenceFrame() }
        val stt = FakeStt("Ciao")
        val c = controller(frames, stt, FakeTts())
        c.listenOnce()
        assertTrue(stt.acceptedFrames >= 6)
        val hasBufferApi = c.javaClass.methods.any {
            it.name.contains("frameBuffer", ignoreCase = true) || it.name.contains("retainedAudio", ignoreCase = true)
        }
        assertFalse(hasBufferApi)
    }

    @Test
    fun `mic permission flow classification is deterministic`() {
        assertEquals(MicPermissionState.GRANTED, MicPermissionFlow.classify(granted = true, shouldShowRationale = false, previouslyRequested = false))
        assertEquals(MicPermissionState.DENIED, MicPermissionFlow.classify(granted = false, shouldShowRationale = true, previouslyRequested = true))
        assertEquals(MicPermissionState.PERMANENTLY_DENIED, MicPermissionFlow.classify(granted = false, shouldShowRationale = false, previouslyRequested = true))
        assertEquals(MicPermissionState.DENIED, MicPermissionFlow.classify(granted = false, shouldShowRationale = false, previouslyRequested = false))
        assertTrue(MicPermissionFlow.RATIONALE.contains("never stored"))
        assertTrue(MicPermissionFlow.RATIONALE.contains("privacy mute"))
    }

    /** Source that fails typed at start (G4-C02 contract), like a real AudioRecord. */
    private class FailedMicSource(kind: MicFailureKind) : AudioFrameSource, MicDiagnostics {
        override val isActive = false
        override var lastFailure: MicSourceException? = MicSourceException(kind, "scripted $kind")
        override fun start() {}
        override fun stop() {}
        override suspend fun read(): AudioFrame? = null
    }

    @Test
    fun `typed mic permission failure ends the turn as MIC_PERMISSION_DENIED, never NO_SPEECH`() = runBlocking {
        val stt = FakeStt("mai trascritto")
        var towerCalled = false
        val c = VoicePipelineController(
            sourceFactory = { FailedMicSource(MicFailureKind.PERMISSION_DENIED) },
            vad = EnergyVadEngine(),
            stt = stt,
            tts = FakeTts(),
            router = RuleBasedIntentRouter(),
            modeProvider = { OperatingMode.HYBRID },
            connectivityProvider = { Connectivity.OK },
            localExecutor = { "ok" },
            towerExecutor = { towerCalled = true; "tower" },
            framePollDelayMs = 1,
        )
        val result = c.listenOnce()
        assertEquals("MIC_PERMISSION_DENIED", result.errorCode)
        assertEquals(VoicePipelineState.FAILED, c.state.value)
        assertEquals(0, stt.acceptedFrames)
        assertFalse(towerCalled) // no Tower contact on a local hardware failure
    }

    @Test
    fun `typed mic busy failure is surfaced verbatim`() = runBlocking {
        val c = VoicePipelineController(
            sourceFactory = { FailedMicSource(MicFailureKind.DEVICE_BUSY) },
            vad = EnergyVadEngine(),
            stt = FakeStt("x"),
            tts = null,
            router = RuleBasedIntentRouter(),
            modeProvider = { OperatingMode.HYBRID },
            connectivityProvider = { Connectivity.OK },
            localExecutor = { "ok" },
            towerExecutor = { "tower" },
            framePollDelayMs = 1,
        )
        val result = c.listenOnce()
        assertEquals("MIC_DEVICE_BUSY", result.errorCode)
        assertEquals(VoiceTurnRecord.ROUTE_NONE, result.route)
    }
}
