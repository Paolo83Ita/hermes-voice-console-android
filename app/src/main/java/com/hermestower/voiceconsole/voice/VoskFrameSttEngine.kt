package com.hermestower.voiceconsole.voice

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject
import java.io.File

/**
 * GRAPH 4 FINAL directive §14 — REAL offline STT delivery and use.
 *
 * Frame-based seam implementation over the VERIFIED downloadable Italian model
 * `vosk-model-small-it-0.22` consumed by the runtime
 * `com.alphacephei:vosk-android:0.3.75` (Maven Central, Apache-2.0, ships
 * arm64-v8a/armv7/x86/x86_64 — ABI-complete for the Xiaomi 11 Lite 5G NE and
 * the x86_64 emulator). The engine identity is exposed for the safe
 * Diagnostics view (§13 ACTIVE_RUNTIME_READBACK).
 *
 * Honesty contract (directive §14):
 *  - when the model is NOT installed/verified, the engine reports
 *    [SttState.NOT_INSTALLED] and the ACTIVE STT engine remains the Android
 *    system recognizer (the app NEVER claims local STT while running the
 *    network/system recognition);
 *  - when installed, load() constructs the vosk Model/Recognizer and the
 *    engine identity becomes "vosk-android 0.3.75 + vosk-model-small-it-0.22";
 *  - frames are consumed in-flight and never retained (INV-G4-09);
 *  - unload() closes the recognizer and the model and reports UNLOADED;
 *  - every failure is typed (MODEL_ERROR / NOT_READY / AUDIO_LOST).
 *
 * The vosk classes are referenced ONLY inside [load]/[accept]/[finalizeTranscript]
 * so JVM unit tests can construct and inspect this class without loading the
 * native runtime.
 */
class VoskFrameSttEngine(
    /** Root directory of the installed model (filesDir/models/stt/vosk-small-it). */
    private val modelRoot: File,
    /** Injected decoder so the byte-path stays unit-testable without the native lib. */
    private val decoder: FrameDecoder = object : FrameDecoder {
        override fun acceptWaveForm(recognizer: Any, samples: ShortArray): Boolean =
            VoskNativeBridge.acceptWaveForm(recognizer, samples)
    },
) : SttEngine {

    /** Byte-path abstraction (unit tests inject a scripted decoder). */
    fun interface FrameDecoder {
        fun acceptWaveForm(recognizer: Any, samples: ShortArray): Boolean
    }

    private val _state = MutableStateFlow(SttOutput(SttState.NOT_INSTALLED))
    override val state: StateFlow<SttOutput> = _state.asStateFlow()

    /** Model directory actually loaded (null until load() succeeds). */
    @Volatile
    var loadedModelDir: File? = null
        private set

    /** The identity surfaced in Diagnostics (§13). */
    val engineId: String
        get() = if (loadedModelDir != null) {
            "vosk-android 0.3.75 + vosk-model-small-it-0.22"
        } else {
            "Android system recognizer (vosk model not installed)"
        }

    private var recognizer: Any? = null
    private var model: Any? = null

    /** Resolves the actual vosk model directory (the install may unzip one level deep). */
    fun resolveModelDir(): File? {
        val candidates = listOf(
            modelRoot,
            // the vosk zip typically unzips to vosk-model-small-it-0.22/
            File(modelRoot, "vosk-model-small-it-0.22"),
        )
        for (dir in candidates) {
            if (!dir.isDirectory) continue
            val hasModel = dir.listFiles()?.any {
                it.name in setOf("am", "final.mdl", "conf", "graph")
            } == true
            if (hasModel) return dir
        }
        return null
    }

    override fun load() {
        val dir = resolveModelDir()
        if (dir == null) {
            _state.value = SttOutput(SttState.NOT_INSTALLED)
            return
        }
        runCatching {
            val m = VoskNativeBridge.loadModel(dir.absolutePath)
            val r = VoskNativeBridge.createRecognizer(m, 16000f)
            model = m
            recognizer = r
            loadedModelDir = dir
            _state.value = SttOutput(SttState.READY)
        }.onFailure {
            _state.value = SttOutput(
                SttState.FAILED,
                error = SttError(SttError.Kind.MODEL_ERROR, it.javaClass.simpleName),
            )
        }
    }

    override fun unload() {
        runCatching {
            recognizer?.let { VoskNativeBridge.closeRecognizer(it) }
            model?.let { VoskNativeBridge.closeModel(it) }
        }
        recognizer = null
        model = null
        loadedModelDir = null
        _state.value = SttOutput(SttState.UNLOADED)
    }

    override fun accept(frame: AudioFrame): SttOutput {
        val r = recognizer ?: return _state.value
        return runCatching {
            val hasResult = decoder.acceptWaveForm(r, frame.samples)
            if (hasResult) {
                val text = VoskNativeBridge.resultText(r)
                if (text.isNotBlank()) {
                    SttOutput(SttState.FINALIZING, partialTranscript = null, finalTranscript = text)
                } else {
                    SttOutput(SttState.TRANSCRIBING, partialTranscript = "")
                }
            } else {
                val partial = VoskNativeBridge.partialText(r)
                SttOutput(SttState.TRANSCRIBING, partialTranscript = partial.ifBlank { null })
            }
        }.getOrElse {
            SttOutput(
                SttState.FAILED,
                error = SttError(SttError.Kind.AUDIO_LOST, it.javaClass.simpleName),
            )
        }
    }

    override fun finalizeTranscript(): SttOutput {
        val r = recognizer ?: return _state.value
        return runCatching {
            val text = VoskNativeBridge.finalResultText(r)
            SttOutput(SttState.FINALIZING, finalTranscript = text.ifBlank { null })
        }.getOrElse {
            SttOutput(SttState.FAILED, error = SttError(SttError.Kind.UNKNOWN, it.javaClass.simpleName))
        }
    }

    override fun reset() {
        recognizer?.let { runCatching { VoskNativeBridge.resetRecognizer(it) } }
    }
}

/**
 * The ONLY class that touches the vosk types — isolated so the engine above
 * stays constructible on the JVM (unit tests) and the native runtime is
 * touched exclusively on-device.
 */
private object VoskNativeBridge {
    fun loadModel(path: String): Any = org.vosk.Model(path)
    fun createRecognizer(model: Any, sampleRate: Float): Any =
        org.vosk.Recognizer(model as org.vosk.Model, sampleRate)

    fun acceptWaveForm(recognizer: Any, samples: ShortArray): Boolean {
        val bytes = ByteArray(samples.size * 2)
        for (i in samples.indices) {
            bytes[2 * i] = (samples[i].toInt() and 0xFF).toByte()
            bytes[2 * i + 1] = ((samples[i].toInt() shr 8) and 0xFF).toByte()
        }
        return (recognizer as org.vosk.Recognizer).acceptWaveForm(bytes, bytes.size)
    }

    fun partialText(recognizer: Any): String =
        JSONObject((recognizer as org.vosk.Recognizer).partialResult).optString("partial").orEmpty()

    fun resultText(recognizer: Any): String =
        JSONObject((recognizer as org.vosk.Recognizer).result).optString("text").orEmpty()

    fun finalResultText(recognizer: Any): String =
        JSONObject((recognizer as org.vosk.Recognizer).finalResult).optString("text").orEmpty()

    fun resetRecognizer(recognizer: Any) = (recognizer as org.vosk.Recognizer).reset()

    fun closeRecognizer(recognizer: Any) = (recognizer as org.vosk.Recognizer).close()

    fun closeModel(model: Any) = (model as org.vosk.Model).close()
}
