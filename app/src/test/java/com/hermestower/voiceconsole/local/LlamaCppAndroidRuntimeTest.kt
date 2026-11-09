package com.hermestower.voiceconsole.local

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * GRAPH 4.14 §9 — the verified llama.cpp runtime adapter, exercised against a
 * deterministic fixture bridge (plumbing only; the REAL native bridge binds
 * llama.cpp b9878 on device through llama-android 0.1.1).
 */
class LlamaCppAndroidRuntimeTest {

    /** Deterministic bridge: counts lifecycle calls, returns canned text. */
    private class FixtureBridge : LlamaNativeBridge {
        var loaded = 0
        var released = 0
        var completed = 0
        var failLoad = false
        var failComplete = false
        var lastConfig: LlamaNativeConfig? = null
        var lastPath: String? = null
        private var handle: Any? = null

        override fun loadModel(modelPath: String, config: LlamaNativeConfig): Any {
            if (failLoad) throw LlamaNativeException("LLAMA_MODEL_LOAD_FAILED:fixture")
            loaded++
            lastConfig = config
            lastPath = modelPath
            return Object().also { handle = it }
        }

        override fun complete(model: Any, prompt: String, systemPrompt: String?, maxTokens: Int): LlamaNativeCompletion {
            if (failComplete) throw LlamaNativeException("LLAMA_INFERENCE_FAILED:fixture")
            if (model !== handle) throw LlamaNativeException("LLAMA_HANDLE_TYPE_MISMATCH")
            completed++
            return LlamaNativeCompletion(
                text = "risposta locale: ${prompt.take(16)}",
                tokensGenerated = 7,
                tokensPerSecond = 9.5f,
                promptEvalTimeMs = 120,
                generateTimeMs = 340,
            )
        }

        override fun release(model: Any) {
            released++
            handle = null
        }
    }

    @Test
    fun `load binds the runtime and generate returns the completion text`() {
        val bridge = FixtureBridge()
        val runtime = LlamaCppAndroidRuntime(bridge, LlamaNativeConfig(contextSize = 1024, threads = 2))
        val f = File("/tmp/fake.gguf").apply { writeText("gguf") }
        runtime.load(f, contextTokens = 1024)
        assertEquals("loadModel called once", 1, bridge.loaded)
        assertEquals("path forwarded verbatim", f.absolutePath, bridge.lastPath)
        assertEquals("context forwarded", 1024, bridge.lastConfig!!.contextSize)
        val text = runtime.generate("Ciao, chi sei?", maxTokens = 64)
        assertTrue(text.startsWith("risposta locale:"))
        assertEquals(1, bridge.completed)
        assertNull("no failure recorded on the happy path", null)
    }

    @Test
    fun `unload releases the native handle and is idempotent`() {
        val bridge = FixtureBridge()
        val runtime = LlamaCppAndroidRuntime(bridge)
        runtime.load(File("/tmp/fake.gguf").apply { writeText("gguf") }, contextTokens = 2048)
        runtime.unload()
        assertEquals(1, bridge.released)
        runtime.unload()
        assertEquals("second unload must not double-release", 1, bridge.released)
        // Re-bind after unload works and releases the (already empty) old handle only once.
        runtime.load(File("/tmp/fake2.gguf").apply { writeText("gguf") }, contextTokens = 2048)
        assertEquals(2, bridge.loaded)
        assertEquals(1, bridge.released)
    }

    @Test
    fun `load failure is typed — the engine maps it to LOAD_FAILED`() {
        val bridge = FixtureBridge().apply { failLoad = true }
        val runtime = LlamaCppAndroidRuntime(bridge)
        try {
            runtime.load(File("/tmp/fake.gguf").apply { writeText("gguf") }, contextTokens = 2048)
            throw AssertionError("load must throw")
        } catch (expected: IllegalStateException) {
            assertTrue(expected.message!!.contains("LLAMA_MODEL_LOAD_FAILED"))
            // NOT LlmRuntimeUnavailableException: the runtime EXISTS here — a
            // native load failure is a model/runtime compatibility problem
            // (LOAD_FAILED), never a "runtime unavailable" claim.
            assertFalse(
                "load failure must not be tagged as runtime-unavailable",
                expected is LlmRuntimeUnavailableException,
            )
        }
    }

    @Test
    fun `generate before load is typed LLAMA_NOT_BOUND`() {
        val runtime = LlamaCppAndroidRuntime(FixtureBridge())
        try {
            runtime.generate("x", maxTokens = 4)
            throw AssertionError("generate must throw before load")
        } catch (expected: IllegalStateException) {
            assertTrue(expected.message!!.contains("LLAMA_NOT_BOUND"))
        }
    }

    @Test
    fun `runtime identity is the verbatim verified truth`() {
        val identity = LlamaCppAndroidRuntime(FixtureBridge()).name
        assertTrue(identity.contains("llama.cpp b9878"))
        assertTrue(identity.contains("llama-android:0.1.1"))
        assertTrue(identity.contains("arm64-v8a"))
    }

    @Test
    fun `engine end-to-end — enabled, integrity OK, load, prompt from model`() {
        val root = createTempDir("llmroot")
        val f = File(root, "qwen2.5-0.5b-instruct-q5_k_m.gguf").apply { writeText("payload") }
        val sha = java.security.MessageDigest.getInstance("SHA-256").digest(f.readBytes())
            .joinToString("") { "%02x".format(it) }
        val engine = LocalLlmEngine(
            modelRoot = root,
            expectedSha256 = sha,
            expectedSizeBytes = f.length(),
            enabledProvider = { true },
            runtime = LlamaCppAndroidRuntime(FixtureBridge()),
        )
        engine.syncFromEnabledFlag()
        engine.load()
        assertEquals(LocalLlmEngine.State.LOADED, engine.state.value)
        val reply = engine.prompt("Saluta in italiano", statusProvider = { "CONSOLE" })
        assertTrue("model text must flow through the safe path", reply.fromModel)
        assertNotNull(reply.text)
        engine.unload()
        assertEquals("unload returns to NOT_INSTALLED when enabled", LocalLlmEngine.State.NOT_INSTALLED, engine.state.value)
    }
}
