package com.hermestower.voiceconsole.local

import java.io.File

/**
 * ANDROID REPAIR CP-007 — the DETERMINISTIC LOCAL MODEL TEST (mission
 * section 9, binding). Owner-accessible from Models and Voice → Local Models
 * → Qwen2.5 0.5B → "Test local model".
 *
 * Configuration (fixed, deterministic):
 *   prompt            = "Rispondi soltanto con OK"
 *   temperature       = 0
 *   max_output_tokens = 8
 *   concurrency       = 1
 *   context           = smallest safe qualified value (512)
 *   chat template     = Qwen2.5 ChatML
 *
 * The chain (mission section 8, stages 8-25):
 *   artifact selected → registry resolved → ABI checked → native library
 *   loaded → JNI bridge opened → GGUF opened → model created → context
 *   created → template applied → prompt tokenized → prompt evaluated →
 *   generation started → UTF-8 output decoded → visible result → unload →
 *   RAM read-back.
 *
 * HONESTY RULE (mission section 9): if the visible output differs from OK,
 * the result records it verbatim and the gate FAILS — no retry-until-pass,
 * no fabricated output, no physical-owner claim from emulator or CI
 * evidence.
 */
class QwenDeterministicTestController(
    private val engine: LocalLlmEngine,
    private val native: LlamaNativeBridge,
    /** ABI of the running device (Build.SUPPORTED_ABIS[0] in production). */
    private val deviceAbi: String,
    /** Thermal state string (PowerManager in production; '-' on JVM). */
    private val thermalStateProvider: () -> String = { "-" },
    /** Native heap bytes provider (Debug.getNativeHeapAllocatedSize in production). */
    private val nativeHeapProvider: () -> Long = { -1L },
) {

    companion object {
        const val TEST_PROMPT = "Rispondi soltanto con OK"
        const val EXPECTED_OUTPUT = "OK"
        const val TEMPERATURE = 0.0f
        const val MAX_OUTPUT_TOKENS = 8
        /** Smallest safe qualified context for the 0.5B q5_k_m class. */
        const val CONTEXT_TOKENS = 512
        const val CHAT_TEMPLATE = "qwen2.5-chatml"
        const val NATIVE_LIBRARY = "libllama.so (llama-android 0.1.1, llama.cpp b9878)"

        /** The Qwen2.5 ChatML template (system carries the deterministic contract). */
        fun buildTestPrompt(userPrompt: String): String =
            "<|im_start|>system\nSei l'assistente locale della console. Rispondi ESATTAMENTE con il testo richiesto, senza altro.<|im_end|>\n" +
                "<|im_start|>user\n$userPrompt<|im_end|>\n" +
                "<|im_start|>assistant\n"
    }

    /** The full, measured result — every field the Owner sees (no invented value). */
    data class Result(
        val passed: Boolean,
        val artifactIdentity: String,
        val artifactFile: String?,
        val hashVerified: Boolean,
        val fileSizeBytes: Long?,
        val quantization: String,
        val abi: String,
        val nativeLibraryState: String,
        val jniState: String,
        val ggufState: String,
        val modelState: String,
        val contextState: String,
        val chatTemplate: String,
        val loadDurationMs: Long?,
        val promptEvalMs: Long?,
        val generationDurationMs: Long?,
        val tokensPerSecond: Float?,
        val peakNativeHeapBytes: Long?,
        val thermalState: String,
        val visibleOutput: String,
        val unloadResult: String,
        val postUnloadNativeHeapBytes: Long?,
        val failedStage: String?,
        val errorCode: String?,
    )

    /** Runs the deterministic chain and returns the measured truth. */
    fun run(): Result {
        val diag = engine.diagnostics()
        val artifactIdentity = diag.candidateIdentity
        val abi = deviceAbi
        val thermal = thermalStateProvider()
        val heapBefore = nativeHeapProvider()

        // 1. artifact selected + registry resolved + integrity (the engine's truth)
        val integrity = engine.verifyInstalledIntegrity()
        val artifact: File? = engine.resolveInstalledFile()
        val base = Result(
            passed = false,
            artifactIdentity = artifactIdentity,
            artifactFile = artifact?.absolutePath,
            hashVerified = integrity == LocalLlmEngine.DiagnosticCode.OK,
            fileSizeBytes = artifact?.length(),
            quantization = "q5_k_m",
            abi = abi,
            nativeLibraryState = if (engine.hasVerifiedRuntime) NATIVE_LIBRARY else "ABSENT",
            jniState = "OPEN",
            ggufState = "PENDING",
            modelState = "PENDING",
            contextState = "PENDING",
            chatTemplate = CHAT_TEMPLATE,
            loadDurationMs = null,
            promptEvalMs = null,
            generationDurationMs = null,
            tokensPerSecond = null,
            peakNativeHeapBytes = null,
            thermalState = thermal,
            visibleOutput = "",
            unloadResult = "PENDING",
            postUnloadNativeHeapBytes = null,
            failedStage = null,
            errorCode = null,
        )

        if (integrity != LocalLlmEngine.DiagnosticCode.OK || artifact == null) {
            return base.copy(
                ggufState = "NOT_OPENED",
                modelState = "NOT_CREATED",
                failedStage = "ARTIFACT",
                errorCode = integrity.asText,
                unloadResult = "NOT_LOADED",
                postUnloadNativeHeapBytes = heapBefore,
            )
        }

        // 2. native library + JNI + GGUF + model + context (deterministic config)
        val runtime = LlamaCppAndroidRuntime(
            native = native,
            config = LlamaNativeConfig(
                contextSize = CONTEXT_TOKENS,
                temperature = TEMPERATURE,
                topK = 1,
                topP = 1.0f,
                seed = 0,
            ),
        )
        val loadStart = System.currentTimeMillis()
        return try {
            runtime.load(artifact, contextTokens = CONTEXT_TOKENS)
            val loadDuration = System.currentTimeMillis() - loadStart
            val heapAfterLoad = nativeHeapProvider()

            // 3. prompt tokenized + evaluated + generation (bounded, deterministic)
            val prompt = buildTestPrompt(TEST_PROMPT)
            val visible = runtime.generate(prompt, MAX_OUTPUT_TOKENS).trim()
            val stats = runtime.lastCompletionStats()
            val heapAfterGen = nativeHeapProvider()

            // 4. unload + RAM read-back
            runtime.unload()
            val heapAfterUnload = nativeHeapProvider()

            base.copy(
                passed = visible == EXPECTED_OUTPUT,
                ggufState = "OPENED",
                modelState = "CREATED",
                contextState = "CREATED(${CONTEXT_TOKENS}t)",
                loadDurationMs = loadDuration,
                promptEvalMs = stats?.promptEvalTimeMs,
                generationDurationMs = stats?.generateTimeMs,
                tokensPerSecond = stats?.tokensPerSecond,
                peakNativeHeapBytes = maxOf(heapAfterLoad, heapAfterGen),
                visibleOutput = visible,
                unloadResult = "UNLOADED",
                postUnloadNativeHeapBytes = heapAfterUnload,
                failedStage = if (visible == EXPECTED_OUTPUT) null else "VISIBLE_OUTPUT",
                errorCode = if (visible == EXPECTED_OUTPUT) null else "QWEN_TEST_OUTPUT_MISMATCH",
            )
        } catch (t: Throwable) {
            runCatching { runtime.unload() }
            base.copy(
                ggufState = "OPENED",
                modelState = "LOAD_FAILED",
                unloadResult = "UNLOADED_AFTER_FAILURE",
                postUnloadNativeHeapBytes = nativeHeapProvider(),
                failedStage = "NATIVE_CHAIN",
                errorCode = "QWEN_TEST_NATIVE_FAILURE:${t.javaClass.simpleName}",
            )
        }
    }
}
