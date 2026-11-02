package com.hermestower.voiceconsole.voice

import com.hermestower.voiceconsole.voice.dsp.Dsp
import com.hermestower.voiceconsole.voice.onnx.OnnxRunner
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * GRAPH 4 CP-G4-008 (Owner §9 NOISE REDUCTION): REAL streaming inference path
 * for the physically verified GTCRN model (gtcrn_simple.onnx, SHA-256
 * e77603ac…, audit §4).
 *
 * Owner §9 contract, each item implemented:
 *  - real inference path: PCM → WOLA STFT(512, hop 256) → ORT([1,257,1,2] + 3
 *    streaming caches) → iSTFT → PCM. With a spectral-identity model the WOLA
 *    chain reconstructs the input within float error (unit-tested);
 *  - bypass: [bypass] short-circuits to raw passthrough (Owner-visible flag);
 *  - frame-size adaptation: any chunk length is buffered and streamed;
 *  - latency measurement: [lastChunkLatencyNanos] is truthful;
 *  - typed failure: the pipeline falls back to raw audio; state FAILED is visible;
 *  - feature flag: wired by the composition root only when enabled;
 *  - unload: [unload] closes the ORT session.
 */
class GtcrnDenoiserEngine(
    private val runner: OnnxRunner,
) : NoiseSuppressionEngine {

    data class NoiseSuppressionError(val kind: Kind, val detail: String) {
        enum class Kind { RUNTIME_UNAVAILABLE, INFERENCE_FAILED, INVALID_FRAME }
    }

    enum class EngineState { UNLOADED, READY, FAILED, BYPASSED }

    private val _state = MutableStateFlow(EngineState.UNLOADED)
    val state: StateFlow<EngineState> = _state.asStateFlow()

    @Volatile
    var bypass: Boolean = false

    /** Truthful latency of the LAST processed chunk (nanos); 0 before first use. */
    @Volatile
    var lastChunkLatencyNanos: Long = 0L
        private set

    // WOLA streaming state
    private var inBuf = FloatArray(0)      // pending input samples
    private var outAcc = FloatArray(Dsp.FFT_SIZE) // overlap-add accumulator
    private var winSum = FloatArray(Dsp.FFT_SIZE) // per-sample synthesis window energy
    // Index-aligned reconstruction stream: recon[j] = final WOLA value of input sample j.
    // Sample j is FINAL once window floor(j/256) has been processed (its two contributing
    // windows are floor(j/256) and floor(j/256)-1) — i.e. when ≥ floor(j/256)·256+512
    // input samples have been received. Output = input delayed by exactly LATENCY samples.
    private val recon = ArrayDeque<Float>()
    private var streamInPos = 0L   // total input samples consumed
    private var emittedPos = 0L    // total output samples produced
    private var reconStart = 0L    // recon[0] corresponds to this input index

    private val convCache = FloatArray(2 * 1 * 16 * 16 * 33)
    private val traCache = FloatArray(2 * 3 * 1 * 1 * 16)
    private val interCache = FloatArray(2 * 1 * 33 * 16)

    init {
        _state.value = if (runner.available) EngineState.READY else EngineState.FAILED
    }

    override val isBypassing: Boolean get() = bypass || _state.value != EngineState.READY

    /**
     * Processes one PCM16 capture chunk (any length) and returns EXACTLY
     * [samples].size samples under a 1:1 streaming contract: output[j] of a
     * call corresponds to the same stream position as input[j] of that call,
     * delayed by the fixed algorithmic latency [LATENCY] (512 samples = 32 ms
     * @16 kHz; zero-padded during the first latency window). Raw passthrough
     * while bypassing or after a typed failure.
     */
    override fun enhance(samples: ShortArray, sampleRate: Int): ShortArray {
        if (isBypassing) return samples
        if (sampleRate != 16000) {
            _state.value = EngineState.FAILED // honest: no hidden resampling
            return samples
        }
        val t0 = System.nanoTime()
        try {
            inBuf = concatenate(inBuf, FloatArray(samples.size) { samples[it] / 32768.0f })
            streamInPos += samples.size
            while (inBuf.size >= Dsp.FFT_SIZE) {
                processOneWindow()
                for (i in 0 until Dsp.HOP) {
                    val w = winSum[i]
                    recon.addLast(if (w > 1e-6f) outAcc[i] / w else 0f)
                }
                shiftLeft(outAcc, Dsp.HOP)
                shiftLeft(winSum, Dsp.HOP)
                inBuf = inBuf.copyOfRange(Dsp.HOP, inBuf.size)
            }
            val out = ShortArray(samples.size)
            for (i in 0 until samples.size) {
                val reconIndex = emittedPos + i - LATENCY
                out[i] = when {
                    reconIndex < reconStart -> 0f // before stream start (only if D>0 prefix)
                    reconIndex < reconStart + recon.size -> {
                        recon[ (reconIndex - reconStart).toInt() ]
                    }
                    else -> 0f // not final yet (cannot happen with LATENCY=512; defensive)
                }.coerceIn(-1f, 1f).let { v -> (v * 32768.0f).toInt().coerceIn(-32768, 32767).toShort() }
            }
            emittedPos += samples.size
            // trim recon entries the output has passed
            val keepFrom = emittedPos - LATENCY
            while (reconStart < keepFrom && recon.isNotEmpty()) {
                recon.removeFirst()
                reconStart++
            }
            lastChunkLatencyNanos = System.nanoTime() - t0
            return out
        } catch (e: com.hermestower.voiceconsole.voice.onnx.OnnxRuntimeException) {
            _state.value = EngineState.FAILED
            resetStream()
            return samples // Owner §9 fallback: raw audio
        } catch (e: IllegalStateException) {
            _state.value = EngineState.FAILED
            resetStream()
            return samples
        }
    }

    /** One 512-sample window: STFT → model (caches fed back) → iSTFT → OLA. */
    private fun processOneWindow() {
        val window = inBuf.copyOfRange(0, Dsp.FFT_SIZE)
        val spec = Dsp.stft(window)
        val mix = FloatArray(Dsp.BINS * 2)
        for (b in 0 until Dsp.BINS) {
            mix[b * 2] = spec[b][0]
            mix[b * 2 + 1] = spec[b][1]
        }
        val out = runner.run(
            inputs = mapOf(
                "mix" to mix,
                "conv_cache" to convCache.copyOf(),
                "tra_cache" to traCache.copyOf(),
                "inter_cache" to interCache.copyOf(),
            ),
            inputShapes = mapOf(
                "mix" to longArrayOf(1, 257, 1, 2),
                "conv_cache" to longArrayOf(2, 1, 16, 16, 33),
                "tra_cache" to longArrayOf(2, 3, 1, 1, 16),
                "inter_cache" to longArrayOf(2, 1, 33, 16),
            ),
        )
        val enh = out.getValue("enh")
        out.getValue("conv_cache_out").copyInto(convCache)
        out.getValue("tra_cache_out").copyInto(traCache)
        out.getValue("inter_cache_out").copyInto(interCache)
        val enhSpec = Array(Dsp.BINS) { b -> floatArrayOf(enh[b * 2], enh[b * 2 + 1]) }
        val frame = Dsp.istft(enhSpec) // synthesis-windowed
        for (i in 0 until Dsp.FFT_SIZE) {
            outAcc[i] += frame[i]
            winSum[i] += Dsp.HANN[i] * Dsp.HANN[i]
        }
    }

    companion object {
        /** Fixed algorithmic latency: sample j is final after window floor(j/256)
         *  is processed, which requires floor(j/256)·256+512 input samples —
         *  worst case j+512. 512 samples = 32 ms @ 16 kHz. */
        const val LATENCY = 512
    }

    /** §9 unload: session freed via runner; WOLA state reset. */
    override fun unload() {
        runner.close()
        resetStream()
        _state.value = EngineState.UNLOADED
    }

    /** Test/utterance-boundary hook: drop partial buffers, zero model caches. */
    fun resetStream() {
        inBuf = FloatArray(0)
        outAcc = FloatArray(Dsp.FFT_SIZE)
        winSum = FloatArray(Dsp.FFT_SIZE)
        recon.clear()
        reconStart = 0L
        streamInPos = 0L
        emittedPos = 0L
        convCache.fill(0f)
        traCache.fill(0f)
        interCache.fill(0f)
    }

    private fun concatenate(a: FloatArray, b: FloatArray): FloatArray {
        val r = FloatArray(a.size + b.size)
        System.arraycopy(a, 0, r, 0, a.size)
        System.arraycopy(b, 0, r, a.size, b.size)
        return r
    }

    private fun shiftLeft(a: FloatArray, by: Int) {
        System.arraycopy(a, by, a, 0, a.size - by)
        a.fill(0f, a.size - by, a.size)
    }
}

/** §9 seam: real noise suppression with raw-audio fallback, no silent failure. */
interface NoiseSuppressionEngine {
    val isBypassing: Boolean
    fun enhance(samples: ShortArray, sampleRate: Int): ShortArray
    fun unload()
}
