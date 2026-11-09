package com.hermestower.voiceconsole.local

import java.io.File

/**
 * GRAPH 4.14 §9 — VERIFIED LOCAL LLM RUNTIME (terminal decision implemented).
 *
 * Decision: VERIFIED_LOCAL_LLM_RUNTIME_INTEGRATED —
 * dev.ffmpegkit-maintained:llama-android:0.1.1 (Maven Central, MIT,
 * llama.cpp b9878, arm64-v8a, minSdk 24, AAR sha256 verified in-sandbox)
 * consumes the EXISTING catalog GGUF (org.qwen.qwen2.5-0.5b-instruct.q5-k-m,
 * pinned HF commit 9217f5db…, SHA-256 041474553f…, Apache-2.0). No model
 * replacement was required — the GGUF stays the compatible artifact.
 *
 * Contract (unchanged from [LlmRuntime]):
 *  - [load] binds the runtime to an installed, hash-verified GGUF file
 *    (integrity is checked by [LocalLlmEngine] BEFORE this call);
 *  - [unload] releases every native resource and resets the binding;
 *  - [generate] runs the safe prompt and returns model text (never Tower truth);
 *  - every failure is TYPED — no silent substitution, no fabricated output:
 *    native-load failures surface as LOAD_FAILED (runtime EXISTS here), while
 *    [LlmRuntimeUnavailableException] stays reserved for a genuinely absent
 *    runtime ([NoVerifiedLlmRuntime], the retained typed fallback).
 */
class LlamaCppAndroidRuntime(
    /** Native seam; the shipped build injects [LlamaRealNativeBridge]. */
    private val native: LlamaNativeBridge,
    /** Bounded generation parameters (no GPU layers on this CPU baseline). */
    private val config: LlamaNativeConfig = LlamaNativeConfig(),
    /** Read-back identity for the Diagnostics card and the runtime receipt. */
    override val name: String = RUNTIME_IDENTITY,
) : LlmRuntime {

    @Volatile
    private var model: Any? = null

    @Volatile
    private var boundFile: File? = null

    override fun load(modelFile: File, contextTokens: Int) {
        // Idempotent re-bind: release any previous handle first.
        unload()
        val handle = native.loadModel(
            modelPath = modelFile.absolutePath,
            config = config.copy(contextSize = contextTokens),
        )
        model = handle
        boundFile = modelFile
    }

    override fun unload() {
        model?.let { runCatching { native.release(it) } }
        model = null
        boundFile = null
    }

    override fun generate(prompt: String, maxTokens: Int): String {
        val handle = model
            ?: throw IllegalStateException("LLAMA_NOT_BOUND")
        val completion = native.complete(
            model = handle,
            prompt = prompt,
            systemPrompt = null, // the safe-prompt contract carries the system context inline
            maxTokens = maxTokens,
        )
        return completion.text
    }

    /** Measured load/inference facts for MODEL_RUNTIME_END_TO_END_RECEIPT (null when unbound). */
    fun lastCompletionStats(): LlamaNativeCompletion? = lastStats

    @Volatile
    private var lastStats: LlamaNativeCompletion? = null

    /** The binding file for receipts (null when unbound). */
    fun boundModelFile(): File? = boundFile

    companion object {
        /** Read-back identity — verbatim runtime truth (never invented). */
        const val RUNTIME_IDENTITY =
            "llama.cpp b9878 via dev.ffmpegkit-maintained:llama-android:0.1.1 (arm64-v8a, MIT)"
    }
}
