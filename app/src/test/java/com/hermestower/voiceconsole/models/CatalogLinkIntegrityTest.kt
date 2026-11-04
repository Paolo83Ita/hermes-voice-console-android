package com.hermestower.voiceconsole.models

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * GRAPH 4 FINAL directive §12 — the SHIPPED catalog must satisfy the
 * real-source contract: every production downloadable entry carries a valid
 * HTTPS source on a reviewed authoritative host bound to a revision, a
 * license URL, a real SHA-256 and plausible sizes. Test fixtures and bundled
 * runtimes are excluded from the web-link contract and must be classified so
 * the UI can label them honestly ("Included with the application" / fixture).
 *
 * This is the §12 pre-enable gate for Download as well: exact file identity
 * (SHA-256), expected size, license and revision binding must be KNOWN before
 * the download action can be enabled.
 */
class CatalogLinkIntegrityTest {

    private fun catalogFile(): File {
        var dir = File(System.getProperty("user.dir") ?: ".")
        val candidates = listOf(
            File(dir, "src/main/assets/models/voice_pack_catalog.json"),
            File(dir, "app/src/main/assets/models/voice_pack_catalog.json"),
        )
        return candidates.first { it.exists() }
    }

    private fun entries(): List<ModelCatalogEntry> {
        val text = catalogFile().readText()
        val json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }
        return json.decodeFromString(
            VoicePackManifest.serializer(),
            text,
        ).entries
    }

    @Test
    fun `every verified downloadable entry has a valid openable source`() {
        val violations = mutableListOf<String>()
        for (e in entries()) {
            if (e.entryClassification != "VERIFIED_DOWNLOADABLE") continue
            val check = ModelLinks.validateForOpen(e.sourceUri)
            if (!check.ok) violations += "${e.identity}: source ${check.reason}"
            val lic = ModelLinks.licenseCheck(e)
            if (!lic.ok) violations += "${e.identity}: license ${lic.reason}"
            if (e.sha256.length != 64 || !e.sha256.all { it.isDigit() || it in 'a'..'f' || it in 'A'..'F' }) {
                violations += "${e.identity}: sha256 not a 64-hex digest"
            }
            if (e.downloadSizeBytes <= 0) violations += "${e.identity}: download size unknown"
            if (e.revisionBinding.isNullOrBlank()) violations += "${e.identity}: no revision binding"
        }
        assertTrue(
            "catalog §12 violations:\n" + violations.joinToString("\n"),
            violations.isEmpty(),
        )
    }

    @Test
    fun `bundled foundations are labeled bundled and never web-openable`() {
        for (e in entries()) {
            if (e.entryClassification == "RUNTIME_BUNDLED") {
                assertFalse(
                    "${e.identity} must not advertise a web source link",
                    ModelLinks.webLinkAllowed(e),
                )
            }
        }
    }

    @Test
    fun `test fixtures never satisfy the production download contract`() {
        for (e in entries()) {
            if (e.entryClassification == "TEST_FIXTURE_ONLY") {
                assertFalse(
                    "${e.identity} is a fixture and must not be web-downloadable",
                    ModelLinks.webLinkAllowed(e),
                )
            }
        }
    }

    @Test
    fun `validator rejects the directive §12 forbidden shapes`() {
        assertFalse(ModelLinks.validateForOpen("http://github.com/x").ok)                 // not https
        assertFalse(ModelLinks.validateForOpen("https://example.invalid/x").ok)          // placeholder host
        assertFalse(ModelLinks.validateForOpen("mock://artifacts/x").ok)                 // internal scheme
        assertFalse(ModelLinks.validateForOpen("unresolved://wake-word/hermes-it").ok)   // unresolved
        assertFalse(ModelLinks.validateForOpen("gradle://sherpa-onnx-android").ok)       // gradle identity
        assertFalse(ModelLinks.validateForOpen(null).ok)
        assertFalse(ModelLinks.validateForOpen("https://some-cdn.example.com/f.bin").ok) // unexpected host
        assertTrue(ModelLinks.validateForOpen("https://github.com/dscripka/openWakeWord/releases/download/v0.5.1/melspectrogram.onnx").ok)
        assertTrue(ModelLinks.validateForOpen("https://alphacephei.com/vosk/models/vosk-model-small-it-0.22.zip").ok)
        assertTrue(ModelLinks.validateForOpen("https://huggingface.co/Qwen/Qwen2.5-0.5B-Instruct-GGUF/resolve/main/x.gguf").ok)
    }

    @Test
    fun `catalog carries at least one real italian STT, TTS voice and LLM candidate`() {
        val es = entries()
        val stt = es.filter { it.componentType == "SPEECH_TO_TEXT" && it.italianSupported && it.entryClassification == "VERIFIED_DOWNLOADABLE" }
        val voice = es.filter { it.componentType == "VOICE" && it.italianSupported && it.entryClassification == "VERIFIED_DOWNLOADABLE" }
        val llm = es.filter { it.componentType == "LOCAL_LLM" && it.entryClassification == "VERIFIED_DOWNLOADABLE" }
        assertTrue("no verified Italian STT candidate", stt.isNotEmpty())
        assertTrue("no verified Italian TTS voice candidate", voice.isNotEmpty())
        assertTrue("no verified optional LLM candidate", llm.isNotEmpty())
        // §16 target class: 0.5B–1.5B quantized.
        llm.forEach {
            assertNotNull("LLM quantization must be declared", it.quantization)
        }
    }
}
