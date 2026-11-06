package com.hermestower.voiceconsole.voice

import com.k2fsa.sherpa.onnx.GeneratedAudio
import com.k2fsa.sherpa.onnx.OfflineTts
import com.k2fsa.sherpa.onnx.OfflineTtsConfig
import com.k2fsa.sherpa.onnx.OfflineTtsModelConfig
import com.k2fsa.sherpa.onnx.OfflineTtsVitsModelConfig

/**
 * CP-G4.9-003 — the REAL sherpa-onnx bridge (the ONLY class touching
 * com.k2fsa.sherpa.onnx). Kept apart from [SherpaOnnxTtsEngine] so JVM unit
 * tests drive the engine through the injected [SherpaOnnxTtsEngine.NativeBridge]
 * fake without the native library; on device this bridge binds the verified
 * v1.12.24 AAR (JitPack tag build, arm64-v8a JNI, Apache-2.0).
 */
class SherpaRealNativeBridge : SherpaOnnxTtsEngine.NativeBridge {

    override fun createVits(modelPath: String, tokensPath: String, dataDir: String): Any {
        val vits = OfflineTtsVitsModelConfig(
            model = modelPath,
            lexicon = "",
            tokens = tokensPath,
            dataDir = dataDir,
            dictDir = "",
            noiseScale = 0.667f,
            noiseScaleW = 0.8f,
            lengthScale = 1.0f,
        )
        val config = OfflineTtsConfig(
            model = OfflineTtsModelConfig(vits = vits, numThreads = 2, debug = false),
            ruleFsts = "",
            ruleFars = "",
            maxNumSentences = 1,
            silenceScale = 0.2f,
        )
        return OfflineTts(assetManager = null, config = config)
    }

    override fun generate(handle: Any, text: String, speed: Float): FloatArray {
        val tts = handle as OfflineTts
        val audio: GeneratedAudio = tts.generate(text, sid = 0, speed = speed)
        return audio.samples
    }

    override fun sampleRate(handle: Any): Int = (handle as OfflineTts).sampleRate()

    override fun release(handle: Any) {
        (handle as OfflineTts).release()
    }
}
