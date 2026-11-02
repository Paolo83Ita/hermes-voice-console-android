package com.hermestower.voiceconsole.voice.onnx

/**
 * GRAPH 4 CP-G4-008 (§9): minimal ONNX execution seam.
 *
 * The REAL production implementation ([OrtOnnxRunner]) binds the physically
 * verified models (docs/models/VOICE_ARTIFACT_PHYSICAL_AUDIT.md) to
 * onnxruntime-android. Tests use deterministic fixture runners that exercise
 * the PLUMBING only — a fixture is never labeled or shipped as a production
 * model (Owner §6 discipline).
 *
 * Everything here is synchronous float32 in / float32 out: the two verified
 * workloads (GTCRN streaming frame, openWakeWord chain) are small and the
 * capture loop already runs on a dedicated thread.
 */
interface OnnxRunner {
    /** True when the runtime is physically loadable on this device. */
    val available: Boolean

    /**
     * Runs one graph invocation.
     * [inputs]: tensor name → rank-shaped float payload (row-major).
     * [inputShapes]: tensor name → dimensions (product must equal payload size).
     * [inputTypes]: tensor name → element type (FLOAT or INT64 — int64 payloads
     * are passed as LongArray via [inputLongs]).
     * Returns output tensor name → flattened float payload.
     */
    fun run(
        inputs: Map<String, FloatArray>,
        inputShapes: Map<String, LongArray>,
        inputLongs: Map<String, LongArray> = emptyMap(),
        longShapes: Map<String, LongArray> = emptyMap(),
    ): Map<String, FloatArray>

    fun close()
}

class OnnxRuntimeException(message: String, cause: Throwable? = null) : Exception(message, cause)
