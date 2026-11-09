package com.hermestower.voiceconsole.local

/**
 * GRAPH 4.14 §9 — NATIVE SEAM for the verified Android llama.cpp runtime.
 *
 * The ONLY class allowed to touch `dev.ffmpegkit.llama` is
 * [LlamaRealNativeBridge] (same discipline as SherpaRealNativeBridge). The
 * JVM tests exercise [LlamaCppAndroidRuntime] against a deterministic fixture
 * bridge — a fixture is never shipped or labeled as the production runtime.
 *
 * Verified upstream (CP-G4.14-005 evidence):
 *   dev.ffmpegkit-maintained:llama-android:0.1.1 (Maven Central, MIT)
 *   AAR sha256 fdccf04c77de470004d7e54679bb918074ee9dbf31ea75af8c014d19c31e7501
 *   llama.cpp b9878 (submodule 2da6686); jni/arm64-v8a; minSdk 24 ≤ 30.
 *   API: Llama.loadModel(modelPath, LlamaConfig) → LlamaModel (suspend);
 *        Llama.complete(model, prompt, systemPrompt, maxTokens) → LlamaResult (suspend);
 *        Llama.releaseModel(model).
 */
interface LlamaNativeBridge {
    /** Binds the runtime to an existing GGUF file; throws [LlamaNativeException] on failure. */
    fun loadModel(modelPath: String, config: LlamaNativeConfig): Any

    /** Blocking bounded completion. Throws [LlamaNativeException] on failure. */
    fun complete(model: Any, prompt: String, systemPrompt: String?, maxTokens: Int): LlamaNativeCompletion

    /** Releases every native resource for the model (idempotent). */
    fun release(model: Any)
}

/** Runtime construction parameters (bounded defaults; no GPU layers on this baseline). */
data class LlamaNativeConfig(
    val contextSize: Int = DEFAULT_CONTEXT_SIZE,
    val threads: Int = DEFAULT_THREADS,
    val gpuLayers: Int = 0,
    val temperature: Float = 0.7f,
    val topP: Float = 0.9f,
    val topK: Int = 40,
    val seed: Int = -1,
) {
    companion object {
        const val DEFAULT_CONTEXT_SIZE = 2048
        const val DEFAULT_THREADS = 4
    }
}

/** Measured completion facts (never invented; surfaced in diagnostics/receipts). */
data class LlamaNativeCompletion(
    val text: String,
    val tokensGenerated: Int,
    val tokensPerSecond: Float,
    val promptEvalTimeMs: Long,
    val generateTimeMs: Long,
)

/** Typed native failure — always wrapped into the engine's LOAD_FAILED/GENERATION path. */
class LlamaNativeException(message: String, cause: Throwable? = null) : IllegalStateException(message, cause)
