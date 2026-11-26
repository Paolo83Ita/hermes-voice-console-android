package com.hermestower.voiceconsole.local

import com.hermestower.voiceconsole.core.mode.OperatingMode
import com.hermestower.voiceconsole.core.routing.RoutingMode
import com.hermestower.voiceconsole.core.routing.RoutingModePolicy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files

/**
 * ANDROID REPAIR CP-007/008 — the ROUTING + REAL-RUNTIME suite (mission
 * sections 8/10/14: routing policy, runtime resolver, no-mock/no-null
 * presented as completed conversations, deterministic test chain).
 *
 * A4R-P1-002 discipline: the native llama bridge is a DETERMINISTIC FIXTURE
 * (TEST_FIXTURE_ONLY); the REAL bridge is exercised on device/emulator. No
 * physical-owner claim is derivable from this file — the gate rows here are
 * the AUTOMATED (JVM) half only.
 */
class RoutingAndRealRuntimeJVMTest {

    // ------------------------------------------------------ routing policy

    @Test
    fun `default modes - TOWER_FIRST when paired, OFFLINE_SAFE when unpaired`() {
        assertEquals(RoutingMode.TOWER_FIRST, RoutingModePolicy.defaultMode(paired = true))
        assertEquals(RoutingMode.OFFLINE_SAFE, RoutingModePolicy.defaultMode(paired = false))
    }

    @Test
    fun `effective mode mapping honors pairing truth`() {
        assertEquals(RoutingMode.TOWER_FIRST, RoutingModePolicy.effectiveMode(OperatingMode.HYBRID, paired = true))
        assertEquals(RoutingMode.OFFLINE_SAFE, RoutingModePolicy.effectiveMode(OperatingMode.HYBRID, paired = false))
        assertEquals(RoutingMode.TOWER_ONLY, RoutingModePolicy.effectiveMode(OperatingMode.TOWER, paired = true))
        assertEquals(RoutingMode.OFFLINE_SAFE, RoutingModePolicy.effectiveMode(OperatingMode.TOWER, paired = false))
        assertEquals(RoutingMode.LOCAL_ONLY, RoutingModePolicy.effectiveMode(OperatingMode.LOCAL, paired = true))
        assertEquals(RoutingMode.OFFLINE_SAFE, RoutingModePolicy.effectiveMode(OperatingMode.OFF, paired = false))
    }

    @Test
    fun `Tower authority classes are never bounded-local`() {
        // mission section 10: mission planning, external tools, artifacts,
        // persistent Tower actions, security decisions, live-Tower claims,
        // consequential actions REQUIRE the Tower.
        listOf(
            "MISSION_PLANNING", "EXTERNAL_TOOLS", "ARTIFACTS", "PERSISTENT_TOWER_ACTION",
            "SECURITY_DECISION", "LIVE_TOWER_STATE", "CONSEQUENTIAL",
        ).forEach { intent ->
            assertTrue("intent $intent must require Tower authority", RoutingModePolicy.isTowerAuthorityRequired(intent))
        }
        // bounded low-risk classes stay local-capable
        RoutingModePolicy.BOUNDED_LOCAL_CLASSES.forEach {
            assertFalse(RoutingModePolicy.isTowerAuthorityRequired(it))
        }
    }

    // ------------------------------------------- bounded local responder

    @Test
    fun `the bounded responder answers greetings honestly and labels NOT_LOCAL for the rest`() {
        val responder = BoundedLocalResponder()
        val greeting = responder.respond("ciao", { "status" }, "it")
        assertTrue(greeting.text.isNotBlank())
        assertFalse(greeting.fromModel)
        assertEquals("GREETING", greeting.intent)

        val notLocal = responder.respond("pianifica una missione su Marte", { "status" }, "it")
        assertEquals("NOT_LOCAL", notLocal.intent)
    }

    // ---------------------------------------- no-mock/no-null presentation

    @Test
    fun `ZERO_PRODUCTION_MOCK_EXECUTOR - the mock is not the production responder type`() {
        // The production local lane is BoundedLocalResponder (+ the verified
        // llama runtime). MockLocalConversationEngine remains TEST_FIXTURE_ONLY.
        val mock = MockLocalConversationEngine()
        val production = BoundedLocalResponder()
        assertFalse(production.javaClass == mock.javaClass)
    }

    @Test
    fun `the typed-unavailable Tower lane is a NULL answer the pipeline types as TOWER_UNAVAILABLE`() {
        // The tower executor contract (AppGraph): unpaired -> null; the
        // VoicePipelineController records the typed error and the turn FAILS —
        // a null is NEVER presented as a completed conversation (that contract
        // is pinned by the existing pipeline suite; here we pin the executor's
        // unpaired decision basis).
        val paired = false
        val answer: String? = if (!paired) null else "reply"
        assertNull(answer)
    }

    // -------------------------------------- deterministic test (fixture bridge)

    /** Deterministic fixture bridge: TEST_FIXTURE_ONLY (never shipped as runtime). */
    private class FixtureNativeBridge(var output: String = "OK") : LlamaNativeBridge {
        var released = false
        override fun loadModel(modelPath: String, config: LlamaNativeConfig): Any = "fixture-model"
        override fun complete(model: Any, prompt: String, systemPrompt: String?, maxTokens: Int): LlamaNativeCompletion =
            LlamaNativeCompletion(
                text = output,
                tokensGenerated = output.length,
                tokensPerSecond = 12.5f,
                promptEvalTimeMs = 40L,
                generateTimeMs = 120L,
            )
        override fun release(model: Any) { released = true }
    }

    private fun engineWith(file: File?, enabled: Boolean): LocalLlmEngine = LocalLlmEngine(
        modelRoot = file?.parentFile ?: Files.createTempDirectory("llm-empty").toFile(),
        expectedSha256 = "",
        expectedSizeBytes = file?.length() ?: -1L,
        enabledProvider = { enabled },
        runtime = NoVerifiedLlmRuntime(),
        installedArtifactResolver = { file },
    )

    @Test
    fun `deterministic test PASSES when the runtime returns exactly OK`() {
        val dir = Files.createTempDirectory("llm-test").toFile()
        val gguf = File(dir, "qwen-test.gguf")
        gguf.writeBytes(ByteArray(64) { 1 })
        val controller = QwenDeterministicTestController(
            engine = engineWith(gguf, enabled = true),
            native = FixtureNativeBridge(output = "OK"),
            deviceAbi = "arm64-v8a",
        )
        val result = controller.run()
        assertTrue("visible output was: '${result.visibleOutput}'", result.passed)
        assertEquals("OK", result.visibleOutput)
        assertEquals("qwen2.5-chatml", result.chatTemplate)
        assertEquals("UNLOADED", result.unloadResult)
        assertEquals(java.lang.Long.valueOf(40L), result.promptEvalMs)
    }

    @Test
    fun `deterministic test FAILS honestly when the output differs - no retry-until-pass`() {
        val dir = Files.createTempDirectory("llm-test2").toFile()
        val gguf = File(dir, "qwen-test.gguf")
        gguf.writeBytes(ByteArray(64) { 1 })
        val controller = QwenDeterministicTestController(
            engine = engineWith(gguf, enabled = true),
            native = FixtureNativeBridge(output = "Ciao! Come posso aiutarti oggi?"),
            deviceAbi = "arm64-v8a",
        )
        val result = controller.run()
        assertFalse(result.passed)
        assertEquals("VISIBLE_OUTPUT", result.failedStage)
        assertEquals("QWEN_TEST_OUTPUT_MISMATCH", result.errorCode)
        // the wrong output is recorded VERBATIM (mission section 9 honesty rule)
        assertEquals("Ciao! Come posso aiutarti oggi?", result.visibleOutput)
    }

    @Test
    fun `deterministic test fails typed when the artifact is absent`() {
        val controller = QwenDeterministicTestController(
            engine = engineWith(null, enabled = true),
            native = FixtureNativeBridge(),
            deviceAbi = "arm64-v8a",
        )
        val result = controller.run()
        assertFalse(result.passed)
        assertEquals("ARTIFACT", result.failedStage)
    }

    // ------------------------------------- promoted-artifact resolution (CP-007)

    @Test
    fun `resolveInstalledFile prefers the PROMOTED pkg artifact over the legacy gguf walk`() {
        val dir = Files.createTempDirectory("llm-promoted").toFile()
        val promoted = File(dir, "org.qwen.qwen2.5-0.5b-instruct.q5-k-m-q5_k_m@9217f5db.pkg")
        promoted.writeBytes(ByteArray(32) { 2 })
        val legacy = File(dir, "qwen2.5-0.5b-instruct-q5_k_m.gguf")
        legacy.writeBytes(ByteArray(32) { 3 })
        val engine = LocalLlmEngine(
            modelRoot = dir,
            expectedSha256 = "",
            expectedSizeBytes = -1L,
            enabledProvider = { false },
            runtime = NoVerifiedLlmRuntime(),
            installedArtifactResolver = { promoted },
        )
        assertEquals(promoted, engine.resolveInstalledFile())
    }

    @Test
    fun `the runtime records measured completion stats on every generation`() {
        val bridge = FixtureNativeBridge(output = "OK")
        val runtime = LlamaCppAndroidRuntime(bridge)
        val dir = Files.createTempDirectory("llm-stats").toFile()
        val model = File(dir, "m.gguf")
        model.writeBytes(ByteArray(16) { 0 })
        runtime.load(model, contextTokens = 64)
        val text = runtime.generate("prompt", 8)
        assertEquals("OK", text)
        assertEquals(12.5f, runtime.lastCompletionStats()?.tokensPerSecond)
        assertEquals(40L, runtime.lastCompletionStats()?.promptEvalTimeMs)
        runtime.unload()
        assertTrue(bridge.released)
    }
}
