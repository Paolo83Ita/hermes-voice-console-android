package com.hermestower.voiceconsole.models

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.time.Instant

/**
 * GRAPH 4.12 §8 — MODEL RUNTIME RECEIPT pins (secret-clean + report).
 */
class ModelRuntimeReceiptTest {

    private fun receipt(fallback: String? = null) = ModelRuntimeReceipt(
        componentIdentity = "org.piper.voice.it-riccardo-x-low",
        componentVersion = "0.22",
        expectedSha256 = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
        actualSha256 = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
        storagePathClass = "filesDir/models/voices",
        runtimeIdentity = "SHERPA_ONNX",
        runtimeVersion = "sherpa-onnx v1.12.24",
        loadStartMillis = 1000L,
        loadResult = ModelRuntimeReceipt.LoadResult.LOADED,
        memoryBeforeBytes = 100_000_000L,
        memoryAfterBytes = 180_000_000L,
        functionalInputClass = "preview-phrase",
        functionalOutputSummary = "preview utterance completed",
        latencyMillis = 420L,
        unloadResult = ModelRuntimeReceipt.UnloadResult.RETAINED_ACTIVE,
        finalState = ModelCapabilityState.ACTIVE,
        fallback = fallback,
    )

    @Test
    fun `secret-clean line contains identities and states, never secret paths`() {
        val line = receipt().secretCleanLine()
        assertTrue(line.contains("org.piper.voice.it-riccardo-x-low"))
        assertTrue(line.contains("ACTIVE"))
        assertFalse(line.contains("/data/user/")) // no absolute secret paths
        assertFalse(line.contains("filesDir/models/voices/")) // path CLASS only, no concrete path
    }

    @Test
    fun `markdown report renders one row per receipt`() {
        val rec = ModelRuntimeReceiptRecorder()
        rec.record(receipt())
        rec.record(receipt(fallback = "ANDROID_SYSTEM_TTS"))
        val md = rec.renderMarkdownReport("MODEL RUNTIME TRUTH REPORT", Instant.parse("2026-11-08T10:00:00Z"))
        assertTrue(md.contains("MODEL RUNTIME TRUTH REPORT"))
        assertTrue(md.contains("org.piper.voice.it-riccardo-x-low"))
        assertEquals(2, Regex("^\\| org\\.piper", RegexOption.MULTILINE).findAll(md).count())
        assertTrue(md.contains("Secret-clean"))
    }

    @Test
    fun `persist round-trips through Properties`() {
        val rec = ModelRuntimeReceiptRecorder()
        rec.record(receipt())
        val f = File.createTempFile("mrt", ".properties")
        rec.persist(f)
        val rec2 = ModelRuntimeReceiptRecorder()
        assertEquals(0, rec2.all().size) // the recorder is session-scoped (not auto-loaded)
        val props = java.util.Properties()
        f.inputStream().use { props.load(it) }
        assertEquals("1", props.getProperty("receipt.count"))
        assertTrue(props.getProperty("receipt.0").contains("org.piper.voice.it-riccardo-x-low"))
        f.delete()
    }

    @Test
    fun `failure receipt carries the typed fallback`() {
        val r = receipt(fallback = "ANDROID_SYSTEM_TTS").copy(
            loadResult = ModelRuntimeReceipt.LoadResult.FAILED,
            finalState = ModelCapabilityState.FAILED,
        )
        assertEquals("ANDROID_SYSTEM_TTS", r.fallback)
        assertEquals(ModelCapabilityState.FAILED, r.finalState)
    }
}
