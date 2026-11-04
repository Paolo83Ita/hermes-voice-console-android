package com.hermestower.voiceconsole.local

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files
import java.security.MessageDigest

/**
 * GRAPH 4.6 directive §6 — OPTIONAL LOCAL LLM engine tests.
 * Enforces the honest contract: DEFAULT DISABLED, typed refusals in every
 * non-runnable state, real integrity verification (size + SHA-256), a safe
 * prompt builder with bounded input, and the NEVER-INVENTS rule — with the
 * shipped NoVerifiedLlmRuntime the engine can only return typed diagnostics
 * with fromModel=false. No assertion weakened: each one pins the typed truth.
 */
class LocalLlmEngineTest {

    private val sha = "041474553fcabfc2a2d67903f9d2c2e50bd92528e670da4f33b5d0ce6e59fd55"

    private fun tempRoot(): File = Files.createTempDirectory("llm-engine-test").toFile()

    private fun engine(root: File, enabled: Boolean = false) = LocalLlmEngine(
        modelRoot = root,
        expectedSha256 = sha,
        expectedSizeBytes = 0L, // size gate exercised in the dedicated test below
        enabledProvider = { enabled },
    )

    @Test
    fun `default state is DISABLED and load without enablement refuses with the typed code`() {
        val e = engine(tempRoot())
        assertEquals(LocalLlmEngine.State.DISABLED, e.state.value)
        e.load()
        assertEquals(LocalLlmEngine.State.DISABLED, e.state.value)
        assertEquals(LocalLlmEngine.DiagnosticCode.DISABLED_BY_DEFAULT, e.diagnostics().lastDiagnosticCode)
        assertTrue(e.diagnostics().lastDiagnosticCode.asText.contains("LLM_DISABLED"))
    }

    @Test
    fun `prompt while disabled returns a typed diagnostic reply never fromModel`() {
        val e = engine(tempRoot())
        val reply = e.prompt("ciao", statusProvider = { "status" })
        assertFalse(reply.fromModel)
        assertEquals(LocalLlmEngine.DiagnosticCode.DISABLED_BY_DEFAULT, reply.diagnosticCode)
        assertEquals("LLM_DISABLED_BY_DEFAULT", reply.intent)
        assertTrue(reply.text.isNotBlank())
    }

    @Test
    fun `enabled without install reports NOT_INSTALLED and load keeps the honest state`() {
        val e = engine(tempRoot(), enabled = true)
        e.syncFromEnabledFlag()
        assertEquals(LocalLlmEngine.State.NOT_INSTALLED, e.state.value)
        assertEquals(LocalLlmEngine.DiagnosticCode.NOT_INSTALLED, e.verifyInstalledIntegrity())
        e.load()
        assertEquals(LocalLlmEngine.State.NOT_INSTALLED, e.state.value)
    }

    @Test
    fun `integrity check detects size mismatch and hash mismatch with typed codes`() {
        val root = tempRoot()
        val f = File(root, "qwen2.5-0.5b-instruct-q5_k_m.gguf").apply { writeText("payload") }

        val sizeGate = LocalLlmEngine(root, sha, expectedSizeBytes = 999L, enabledProvider = { true })
        assertEquals(LocalLlmEngine.DiagnosticCode.SIZE_MISMATCH, sizeGate.verifyInstalledIntegrity())

        val hashGate = LocalLlmEngine(root, sha, expectedSizeBytes = 0L, enabledProvider = { true })
        assertEquals(LocalLlmEngine.DiagnosticCode.HASH_MISMATCH, hashGate.verifyInstalledIntegrity())

        // Correct hash passes: recompute the digest of the actual bytes.
        val realSha = MessageDigest.getInstance("SHA-256").digest(f.readBytes())
            .joinToString("") { "%02x".format(it) }
        assertNotEquals(sha, realSha)
        val okGate = LocalLlmEngine(root, realSha, expectedSizeBytes = f.length(), enabledProvider = { true })
        assertEquals(LocalLlmEngine.DiagnosticCode.OK, okGate.verifyInstalledIntegrity())
    }

    @Test
    fun `load with the shipped NoVerifiedLlmRuntime reports the typed runtime gap`() {
        val root = tempRoot()
        val f = File(root, "qwen2.5-0.5b-instruct-q5_k_m.gguf").apply { writeText("payload") }
        val realSha = MessageDigest.getInstance("SHA-256").digest(f.readBytes())
            .joinToString("") { "%02x".format(it) }
        val e = LocalLlmEngine(root, realSha, expectedSizeBytes = f.length(), enabledProvider = { true })
        e.load()
        assertEquals(LocalLlmEngine.State.ERROR, e.state.value)
        assertEquals(LocalLlmEngine.DiagnosticCode.RUNTIME_UNAVAILABLE, e.diagnostics().lastDiagnosticCode)
        assertEquals("LLM_RUNTIME_PENDING_VERIFIED_SOURCE", e.diagnostics().lastDiagnosticCode.asText)
        assertEquals("no verified Android runtime linked", e.diagnostics().runtimeName)
    }

    @Test
    fun `prompt after a failed load returns the runtime-gap diagnostic with fromModel false`() {
        val root = tempRoot()
        val f = File(root, "qwen2.5-0.5b-instruct-q5_k_m.gguf").apply { writeText("payload") }
        val realSha = MessageDigest.getInstance("SHA-256").digest(f.readBytes())
            .joinToString("") { "%02x".format(it) }
        val e = LocalLlmEngine(root, realSha, expectedSizeBytes = f.length(), enabledProvider = { true })
        e.load()
        val reply = e.prompt("chi sei?", statusProvider = { "status" })
        assertFalse(reply.fromModel)
        assertEquals(LocalLlmEngine.DiagnosticCode.RUNTIME_UNAVAILABLE, reply.diagnosticCode)
        assertTrue(reply.text.contains("LLM_RUNTIME_PENDING_VERIFIED_SOURCE"))
    }

    @Test
    fun `unload returns to the honest pre-load state`() {
        val root = tempRoot()
        val f = File(root, "qwen2.5-0.5b-instruct-q5_k_m.gguf").apply { writeText("payload") }
        val realSha = MessageDigest.getInstance("SHA-256").digest(f.readBytes())
            .joinToString("") { "%02x".format(it) }
        val e = LocalLlmEngine(root, realSha, expectedSizeBytes = f.length(), enabledProvider = { true })
        e.load()
        e.unload()
        assertEquals(LocalLlmEngine.State.NOT_INSTALLED, e.state.value)
    }

    @Test
    fun `safe prompt builder caps input strips control characters and states the bounded scope`() {
        val built = SafePromptBuilder.build("ciao\x00\t civilization".repeat(120), "STATUS 8/10")
        assertTrue(built.startsWith(SafePromptBuilder.SYSTEM_SCOPE))
        assertTrue(built.contains("NOT Hermes Tower"))
        assertTrue(built.contains("Truthful device status: STATUS 8/10"))
        assertTrue(built.length <= SafePromptBuilder.SYSTEM_SCOPE.length + SafePromptBuilder.MAX_INPUT_CHARS + 32)
        assertFalse(built.contains("\u0000"))
        assertEquals(512, SafePromptBuilder.MAX_INPUT_CHARS)
    }

    @Test
    fun `a real bound runtime is used verbatim - seam supports the future verified adapter`() {
        val root = tempRoot()
        val f = File(root, "qwen2.5-0.5b-instruct-q5_k_m.gguf").apply { writeText("payload") }
        val realSha = MessageDigest.getInstance("SHA-256").digest(f.readBytes())
            .joinToString("") { "%02x".format(it) }
        val scripted = object : LlmRuntime {
            override val name = "scripted-test-runtime"
            override fun load(modelFile: File, contextTokens: Int) { /* binds */ }
            override fun unload() { /* releases */ }
            override fun generate(prompt: String, maxTokens: Int): String =
                "SCRIPTED:" + prompt.take(12) + ":" + maxTokens
        }
        val e = LocalLlmEngine(root, realSha, expectedSizeBytes = f.length(), enabledProvider = { true }, runtime = scripted)
        e.load()
        assertEquals(LocalLlmEngine.State.LOADED, e.state.value)
        val reply = e.prompt("status?", statusProvider = { "STATUS 8/10" })
        assertTrue(reply.fromModel)
        assertEquals("LOCAL_LLM", reply.intent)
        assertTrue(reply.text.startsWith("SCRIPTED:"))
        assertTrue(reply.text.length <= LocalLlmEngine.MAX_REPLY_CHARS)
    }

    @Test
    fun `syncFromEnabledFlag never tears down a loaded engine`() {
        val root = tempRoot()
        val f = File(root, "qwen2.5-0.5b-instruct-q5_k_m.gguf").apply { writeText("payload") }
        val realSha = MessageDigest.getInstance("SHA-256").digest(f.readBytes())
            .joinToString("") { "%02x".format(it) }
        var enabled = true
        val scripted = object : LlmRuntime {
            override val name = "scripted-test-runtime"
            override fun load(modelFile: File, contextTokens: Int) {}
            override fun unload() {}
            override fun generate(prompt: String, maxTokens: Int) = "x"
        }
        val e = LocalLlmEngine(root, realSha, expectedSizeBytes = f.length(), enabledProvider = { enabled }, runtime = scripted)
        e.load()
        assertEquals(LocalLlmEngine.State.LOADED, e.state.value)
        enabled = false
        e.syncFromEnabledFlag()
        assertEquals(LocalLlmEngine.State.LOADED, e.state.value)
        e.unload()
        assertEquals(LocalLlmEngine.State.DISABLED, e.state.value)
    }
}
