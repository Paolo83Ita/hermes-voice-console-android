package com.hermestower.voiceconsole.voice.onnx

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import java.nio.FloatBuffer
import java.nio.LongBuffer

/**
 * GRAPH 4 CP-G4-008: REAL onnxruntime-android binding for the verified ONNX
 * assets. Loaded lazily; a missing/mismatched native runtime is a TYPED
 * failure (`OnnxRuntimeException`), never a silent degradation — callers own
 * the fallback (raw-audio passthrough, wake OFF).
 */
class OrtOnnxRunner(private val modelPath: String) : OnnxRunner {

    private val env: OrtEnvironment by lazy { OrtEnvironment.getEnvironment() }
    private val session: OrtSession by lazy {
        try {
            env.createSession(modelPath, OrtSession.SessionOptions())
        } catch (t: Throwable) {
            throw OnnxRuntimeException("ORT session creation failed for $modelPath", t)
        }
    }

    override val available: Boolean
        get() = try { session.inputNames.isNotEmpty() } catch (t: Throwable) { false }

    override fun run(
        inputs: Map<String, FloatArray>,
        inputShapes: Map<String, LongArray>,
        inputLongs: Map<String, LongArray>,
        longShapes: Map<String, LongArray>,
    ): Map<String, FloatArray> {
        val tensors = LinkedHashMap<String, OnnxTensor>()
        try {
            for ((name, payload) in inputs) {
                val shape = inputShapes.getValue(name)
                tensors[name] = OnnxTensor.createTensor(env, FloatBuffer.wrap(payload), shape)
            }
            for ((name, payload) in inputLongs) {
                val shape = longShapes.getValue(name)
                tensors[name] = OnnxTensor.createTensor(env, LongBuffer.wrap(payload), shape)
            }
            val out = session.run(tensors)
            try {
                val result = LinkedHashMap<String, FloatArray>()
                for ((name, value) in out) {
                    val t = value as? OnnxTensor ?: continue
                    val flat = FloatArray(t.floatBuffer?.let { it.remaining() } ?: 0)
                    t.floatBuffer?.get(flat)
                    result[name] = flat
                }
                return result
            } finally {
                out.close()
            }
        } catch (t: Throwable) {
            throw OnnxRuntimeException("ORT inference failed for $modelPath", t)
        } finally {
            tensors.values.forEach { it.close() }
        }
    }

    override fun close() {
        try {
            session.close()
        } catch (_: Throwable) {
            // unload is best-effort; the runner is unusable after close either way
        }
    }
}
