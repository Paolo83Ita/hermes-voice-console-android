package com.hermestower.voiceconsole.voice

import com.hermestower.voiceconsole.voice.onnx.OnnxRunner

/**
 * GRAPH 4.14 §6-§7 — REAL Silero VAD engine (decision EXTEND_EXISTING_RUNTIME).
 *
 * Closes the engine gap recorded in the runtime matrix (org.silero.vad.v5 was
 * VERIFIED_DOWNLOADABLE with NO engine): the model now runs on the SAME
 * onnxruntime-android session seam already integrated for GTCRN/openWakeWord.
 *
 * Verified model contract (silero-vad tag v5.1.2, sha256 2623a295…d788f,
 * ONNX graph inspected this mission):
 *   inputs : input FLOAT [batch, samples] · state FLOAT [2, batch, 128] · sr INT64 (scalar)
 *   outputs: output FLOAT [batch, 1] (speech probability) · stateN FLOAT (new state)
 *
 * 16 kHz contract: 512-sample windows (v5). The engine BUFFERS incoming
 * [AudioFrame]s (20 ms = 320 samples) into 512-sample windows, keeps the
 * recurrent state across calls, and applies hysteresis + hangover so callers
 * get the same stable boolean contract as [EnergyVadEngine]. Typed fallback:
 * any runner failure flips the engine into the passthrough-false state and
 * reports failure via [lastError] — never a crash, never fabricated speech.
 *
 * [OnnxRunner] is injected ALREADY BOUND to the Silero model path (on device:
 * OrtOnnxRunner(filesDir/models/vad/silero/silero_vad.onnx); JVM tests use a
 * fixture runner exercising the plumbing only). No audio is retained.
 */
class SileroVadEngine(
    private val runner: OnnxRunner,
    /** Speech probability threshold (Silero default 0.5). */
    private val threshold: Float = 0.5f,
    /** Frames the engine keeps speech=true after probability drops (hangover). */
    private val hangoverFrames: Int = 6,
) : VadEngine {

    /** Typed last failure (null when healthy). Surfaced in diagnostics. */
    @Volatile
    var lastError: String? = null
        private set

    private val window = FloatArray(SILERO_WINDOW_SAMPLES_16K)
    private var buffered = 0
    private var state = FloatArray(2 * 128) // flattened [2,1,128]
    private var hangover = 0
    @Volatile private var speech = false

    override fun isSpeech(frame: AudioFrame): Boolean {
        if (frame.sampleRate != 16000) {
            // Contract violation: Silero 16 kHz only — typed passthrough-false.
            lastError = "SILERO_SAMPLE_RATE_UNSUPPORTED:${frame.sampleRate}"
            return false
        }
        val samples = frame.samples
        var idx = 0
        var decided = speech
        while (idx < samples.size) {
            val take = minOf(window.size - buffered, samples.size - idx)
            for (i in 0 until take) {
                window[buffered + i] = samples[idx + i] / PCM_SCALE
            }
            buffered += take
            idx += take
            if (buffered == window.size) {
                decided = runWindow()
                buffered = 0
            }
        }
        speech = decided
        return speech
    }

    private fun runWindow(): Boolean {
        return try {
            val outs = runner.run(
                inputs = mapOf(
                    "input" to window,
                    "state" to state,
                ),
                inputShapes = mapOf(
                    "input" to longArrayOf(1, SILERO_WINDOW_SAMPLES_16K.toLong()),
                    "state" to longArrayOf(2, 1, 128),
                ),
                inputLongs = mapOf("sr" to longArrayOf(16000L)),
                longShapes = mapOf("sr" to longArrayOf(1)),
            )
            val prob = outs["output"]?.get(0) ?: 0f
            outs["stateN"]?.let { newState ->
                val n = minOf(state.size, newState.size)
                System.arraycopy(newState, 0, state, 0, n)
            }
            lastError = null
            if (prob >= threshold) {
                hangover = hangoverFrames
                true
            } else {
                if (hangover > 0) (hangover-- > 0) else false
            }
        } catch (t: Throwable) {
            // Typed honest failure: report, keep the window machinery consistent,
            // return false (no fabricated speech detection).
            lastError = "SILERO_RUN_FAILED:${t.javaClass.simpleName}"
            hangover = 0
            false
        }
    }

    override fun reset() {
        buffered = 0
        state = FloatArray(2 * 128)
        hangover = 0
        speech = false
        lastError = null
    }

    internal companion object {
        /** Silero v5 16 kHz window (512 samples = 32 ms). */
        const val SILERO_WINDOW_SAMPLES_16K = 512

        /** PCM16 → float normalisation scale. */
        const val PCM_SCALE = 32768f
    }
}
