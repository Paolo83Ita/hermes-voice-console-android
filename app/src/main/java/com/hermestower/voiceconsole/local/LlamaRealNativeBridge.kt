package com.hermestower.voiceconsole.local

import kotlinx.coroutines.runBlocking

/**
 * GRAPH 4.14 §9 — the ONLY class touching `dev.ffmpegkit.llama`.
 *
 * Wraps the verified AAR (dev.ffmpegkit-maintained:llama-android:0.1.1,
 * llama.cpp b9878, MIT, arm64-v8a, minSdk 24) behind [LlamaNativeBridge]:
 *  - the library API is suspend — the bridge pins it to runBlocking with the
 *    library's own dispatcher (the callers of [LlmRuntime] are already
 *    background-scoped; load of a 0.5B GGUF is a bounded multi-second op);
 *  - every library failure is translated to [LlamaNativeException] (typed);
 *  - no secrets, no audio retention, no Tower claims — text in, text out.
 */
class LlamaRealNativeBridge : LlamaNativeBridge {

    override fun loadModel(modelPath: String, config: LlamaNativeConfig): Any = runBlocking {
        try {
            dev.ffmpegkit.llama.Llama.loadModel(
                modelPath = modelPath,
                config = dev.ffmpegkit.llama.LlamaConfig(
                    contextSize = config.contextSize,
                    threads = config.threads,
                    gpuLayers = config.gpuLayers,
                    temperature = config.temperature,
                    topP = config.topP,
                    topK = config.topK,
                    seed = config.seed,
                ),
            )
        } catch (t: Throwable) {
            throw LlamaNativeException("LLAMA_MODEL_LOAD_FAILED:${t.javaClass.simpleName}", t)
        } ?: throw LlamaNativeException("LLAMA_MODEL_LOAD_FAILED:null")
    }

    override fun complete(
        model: Any,
        prompt: String,
        systemPrompt: String?,
        maxTokens: Int,
    ): LlamaNativeCompletion = runBlocking {
        val llamaModel = model as? dev.ffmpegkit.llama.LlamaModel
            ?: throw LlamaNativeException("LLAMA_HANDLE_TYPE_MISMATCH")
        try {
            val result = dev.ffmpegkit.llama.Llama.complete(
                llamaModel,
                prompt = prompt,
                systemPrompt = systemPrompt ?: "",
                maxTokens = maxTokens,
            )
            LlamaNativeCompletion(
                text = result.text,
                tokensGenerated = result.tokensGenerated,
                tokensPerSecond = result.tokensPerSecond,
                promptEvalTimeMs = result.promptEvalTimeMs,
                generateTimeMs = result.generateTimeMs,
            )
        } catch (t: Throwable) {
            throw LlamaNativeException("LLAMA_INFERENCE_FAILED:${t.javaClass.simpleName}", t)
        }
    }

    override fun release(model: Any) {
        val llamaModel = model as? dev.ffmpegkit.llama.LlamaModel ?: return
        try {
            dev.ffmpegkit.llama.Llama.releaseModel(llamaModel)
        } catch (_: Throwable) {
            // Release is best-effort by contract; the handle is dropped either way.
        }
    }
}
