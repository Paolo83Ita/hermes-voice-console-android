package com.hermestower.voiceconsole.voice

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * GRAPH 4.10 CP-G4.10-003: the voice-selection authority (§7) and the runtime
 * factory (§8). A downloaded voice is ACTIVE only after the six-condition
 * read-back; every failure falls back honestly (system Italian TTS, files
 * preserved) — the P0-03 "selected but not consumed" defect class is dead.
 */
class VoiceSelectionAuthorityTest {

    private fun authority(filesPresent: Boolean = true) =
        VoiceSelectionAuthority(voiceFilesPresent = { filesPresent })

    @Test
    fun `selection alone never makes the voice active`() {
        val a = authority()
        a.selectDownloaded("org.piper.voice.it-riccardo-x-low", "m.onnx", "tokens.txt", "1.0", "abc")
        assertEquals(VoiceQualificationState.SELECTION_PENDING_RUNTIME, a.state.value.qualificationState)
        assertNull(a.activeNaturalVoiceId())
    }

    @Test
    fun `complete six-condition read-back activates the voice`() {
        val a = authority()
        a.selectDownloaded("org.piper.voice.it-riccardo-x-low", "m.onnx", "tokens.txt", "1.0", "abc")
        a.beginQualification()
        a.recordRuntimeReadBack(
            VoiceRuntimeReadBack(
                voiceId = "org.piper.voice.it-riccardo-x-low",
                runtimeId = "SHERPA_ONNX",
                filesVerified = true, runtimeAvailable = true, voiceLoaded = true,
                synthesisCompleted = true, playbackCompleted = true,
                readBackMatchesSelected = true, completedAt = "2026-11-07T00:00:00Z",
            ),
        )
        assertEquals(VoiceQualificationState.ACTIVE, a.state.value.qualificationState)
        assertEquals("org.piper.voice.it-riccardo-x-low", a.activeNaturalVoiceId())
    }

    @Test
    fun `read-back from a different voice never activates`() {
        val a = authority()
        a.selectDownloaded("org.piper.voice.it-paola-medium", "p.onnx", "tokens.txt", "1.0", "abc")
        a.beginQualification()
        a.recordRuntimeReadBack(
            VoiceRuntimeReadBack(
                voiceId = "org.piper.voice.it-riccardo-x-low",
                runtimeId = "SHERPA_ONNX",
                filesVerified = true, runtimeAvailable = true, voiceLoaded = true,
                synthesisCompleted = true, playbackCompleted = true,
                readBackMatchesSelected = true, completedAt = "2026-11-07T00:00:00Z",
            ),
        )
        assertEquals(VoiceQualificationState.INSTALLED_RUNTIME_FAILED, a.state.value.qualificationState)
        assertNull(a.activeNaturalVoiceId())
    }

    @Test
    fun `runtime failure drops the route to the system voice with files preserved`() {
        val a = authority()
        a.selectDownloaded("org.piper.voice.it-riccardo-x-low", "m.onnx", "tokens.txt", "1.0", "abc")
        a.beginQualification()
        a.markRuntimeFailed("RUNTIME_INIT_FAILED")
        assertEquals(VoiceQualificationState.INSTALLED_RUNTIME_FAILED, a.state.value.qualificationState)
        assertEquals(VoiceRuntimeType.ANDROID_SYSTEM_TTS, a.state.value.runtimeType)
        assertNull(a.activeNaturalVoiceId())
        assertTrue(a.state.value.lastActivationResult!!.contains("INSTALLED_RUNTIME_FAILED"))
        assertTrue(a.state.value.fallbackState!!.contains("preserved"))
    }

    @Test
    fun `missing files on selection are reported honestly`() {
        val a = authority(filesPresent = false)
        a.selectDownloaded("org.piper.voice.it-riccardo-x-low", "m.onnx", "tokens.txt", "1.0", "abc")
        assertEquals(VoiceQualificationState.INSTALLED_RUNTIME_FAILED, a.state.value.qualificationState)
        assertEquals("FILES_MISSING", a.state.value.activeRuntimeState)
    }

    @Test
    fun `voice removed from disk is no longer route truth`() {
        val mutable = mutableListOf("org.piper.voice.it-riccardo-x-low")
        val a = VoiceSelectionAuthority(voiceFilesPresent = { it in mutable })
        a.selectDownloaded("org.piper.voice.it-riccardo-x-low", "m.onnx", "tokens.txt", "1.0", "abc")
        a.beginQualification()
        a.recordRuntimeReadBack(
            VoiceRuntimeReadBack(
                voiceId = "org.piper.voice.it-riccardo-x-low", runtimeId = "SHERPA_ONNX",
                filesVerified = true, runtimeAvailable = true, voiceLoaded = true,
                synthesisCompleted = true, playbackCompleted = true,
                readBackMatchesSelected = true, completedAt = "t",
            ),
        )
        assertEquals("org.piper.voice.it-riccardo-x-low", a.activeNaturalVoiceId())
        mutable.clear() // the user removed the active voice
        assertNull(a.activeNaturalVoiceId())
    }

    @Test
    fun `clear selection returns to the system route`() {
        val a = authority()
        a.selectDownloaded("org.piper.voice.it-paola-medium", "p.onnx", "tokens.txt", "1.0", "abc")
        a.clearSelection()
        assertEquals(VoiceQualificationState.NONE, a.state.value.qualificationState)
        assertEquals(VoiceRuntimeType.ANDROID_SYSTEM_TTS, a.state.value.runtimeType)
        assertNull(a.state.value.selectedVoiceId)
    }

    @Test
    fun `status line is localized and truthful`() {
        val a = authority()
        assertTrue(a.statusLine("it").startsWith("Voce attiva:"))
        a.selectDownloaded("org.piper.voice.it-paola-medium", "p.onnx", null, "1", "s")
        assertTrue(a.statusLine("it").contains("runtime pending"))
    }
}

class VoiceRuntimeFactoryTest {

    private class ScriptedSeam(
        private val files: VoiceRuntimeFactory.VoiceFiles? = VoiceRuntimeFactory.VoiceFiles("/v/m.onnx", "/v/tokens.txt"),
        private val loadError: String? = null,
        private val utterance: VoiceRuntimeFactory.UtteranceResult =
            VoiceRuntimeFactory.UtteranceResult.COMPLETED,
        private val readBackId: String? = "org.piper.voice.it-riccardo-x-low",
    ) : VoiceRuntimeFactory.VoiceRuntimeSeam {
        var interrupted = false
        var spokePhrase: String? = null
        override fun resolveFiles() = files
        override fun load(): String? = loadError
        override fun speakTest(phrase: String) { spokePhrase = phrase }
        override fun awaitUtteranceCompletion(timeoutMs: Long) = utterance
        override fun readBackVoiceId() = readBackId
        override fun interrupt() { interrupted = true }
    }

    private val voiceId = "org.piper.voice.it-riccardo-x-low"

    @Test
    fun `full pipeline activates with the six-condition read-back`() {
        val f = VoiceRuntimeFactory()
        val seam = ScriptedSeam()
        val outcome = f.activate(seam, voiceId)
        assertTrue(outcome is VoiceRuntimeFactory.ActivationOutcome.Active)
        outcome as VoiceRuntimeFactory.ActivationOutcome.Active
        assertEquals(voiceId, outcome.voiceId)
        assertTrue(outcome.readBack.allConditionsMet())
        assertTrue(outcome.readBack.readBackMatchesSelected)
        assertEquals(VoiceRuntimeFactory.PREVIEW_PHRASE, seam.spokePhrase)
    }

    @Test
    fun `missing files fail before the runtime with FILES_MISSING`() {
        val outcome = VoiceRuntimeFactory().activate(ScriptedSeam(files = null), voiceId)
        assertTrue(outcome is VoiceRuntimeFactory.ActivationOutcome.Failed)
        outcome as VoiceRuntimeFactory.ActivationOutcome.Failed
        assertEquals(VoiceRuntimeFactory.TypedReason.FILES_MISSING, outcome.typedReason)
        assertEquals(VoiceRuntimeType.ANDROID_SYSTEM_TTS, outcome.fallback)
    }

    @Test
    fun `runtime init failure preserves installed files and falls back to system`() {
        val seam = ScriptedSeam(loadError = "native createVits threw")
        val outcome = VoiceRuntimeFactory().activate(seam, voiceId)
        assertTrue(outcome is VoiceRuntimeFactory.ActivationOutcome.Failed)
        outcome as VoiceRuntimeFactory.ActivationOutcome.Failed
        assertEquals(VoiceRuntimeFactory.TypedReason.RUNTIME_INIT_FAILED, outcome.typedReason)
        assertTrue(seam.interrupted)
        assertEquals(VoiceRuntimeType.ANDROID_SYSTEM_TTS, outcome.fallback)
    }

    @Test
    fun `synthesis failure is typed separately from playback failure`() {
        val synth = VoiceRuntimeFactory().activate(
            ScriptedSeam(utterance = VoiceRuntimeFactory.UtteranceResult.SYNTHESIS_FAILED), voiceId,
        )
        val playback = VoiceRuntimeFactory().activate(
            ScriptedSeam(utterance = VoiceRuntimeFactory.UtteranceResult.PLAYBACK_FAILED), voiceId,
        )
        assertEquals(VoiceRuntimeFactory.TypedReason.SYNTHESIS_FAILED, (synth as VoiceRuntimeFactory.ActivationOutcome.Failed).typedReason)
        assertEquals(VoiceRuntimeFactory.TypedReason.PLAYBACK_FAILED, (playback as VoiceRuntimeFactory.ActivationOutcome.Failed).typedReason)
    }

    @Test
    fun `read-back mismatch refuses activation`() {
        val outcome = VoiceRuntimeFactory().activate(ScriptedSeam(readBackId = "org.piper.voice.it-paola-medium"), voiceId)
        assertTrue(outcome is VoiceRuntimeFactory.ActivationOutcome.Failed)
        assertEquals(VoiceRuntimeFactory.TypedReason.READBACK_MISMATCH, (outcome as VoiceRuntimeFactory.ActivationOutcome.Failed).typedReason)
    }

    @Test
    fun `thrown load is treated as a runtime init failure`() {
        val throwing = object : VoiceRuntimeFactory.VoiceRuntimeSeam {
            override fun resolveFiles() = VoiceRuntimeFactory.VoiceFiles("/v/m.onnx", null)
            override fun load(): String? = throw IllegalStateException("linker error")
            override fun speakTest(phrase: String) {}
            override fun awaitUtteranceCompletion(timeoutMs: Long) =
                VoiceRuntimeFactory.UtteranceResult.COMPLETED
            override fun readBackVoiceId(): String? = voiceId
            override fun interrupt() {}
        }
        val outcome = VoiceRuntimeFactory().activate(throwing, voiceId)
        assertEquals(VoiceRuntimeFactory.TypedReason.RUNTIME_INIT_FAILED, (outcome as VoiceRuntimeFactory.ActivationOutcome.Failed).typedReason)
    }
}
