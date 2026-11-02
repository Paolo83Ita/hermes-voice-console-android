package com.hermestower.voiceconsole.voice.dsp

/**
 * GRAPH 4 CP-G4-008 (§9 NOISE REDUCTION): pure-Kotlin DSP for the GTCRN
 * streaming denoiser — 512-point radix-2 FFT, Hann STFT/iSTFT.
 *
 * Numerical contract (verified by unit tests):
 *  - FFT of a pure cosine peaks in bin 1 and N-1;
 *  - STFT→iSTFT round-trip reconstruction error < 1e-3 for real signals.
 *
 * All sizes are fixed to the physically verified GTCRN contract
 * (docs/models/VOICE_ARTIFACT_PHYSICAL_AUDIT.md §4): fftSize=512, bins=257,
 * complex layout [bin][re, im].
 */
object Dsp {
    const val FFT_SIZE = 512
    const val HOP = 256          // 50 % overlap (WOLA)
    const val BINS = FFT_SIZE / 2 + 1 // 257

    /** Hann window, periodic. */
    val HANN: FloatArray = FloatArray(FFT_SIZE) { i ->
        0.5f * (1.0f - kotlin.math.cos((2.0 * Math.PI * i / FFT_SIZE).toFloat()))
    }

    /** In-place iterative radix-2 complex FFT; [re]/[im] length must be a power of two. */
    fun fft(re: FloatArray, im: FloatArray, inverse: Boolean = false) {
        val n = re.size
        require(n and (n - 1) == 0) { "FFT size must be a power of two, got $n" }
        // bit-reversal permutation (full copy approach: correct by construction)
        val rePerm = FloatArray(n)
        val imPerm = FloatArray(n)
        var j = 0
        for (i in 0 until n) {
            rePerm[i] = re[j]
            imPerm[i] = im[j]
            var bit = n shr 1
            while (j and bit != 0) {
                j = j xor bit
                bit = bit shr 1
            }
            j = j xor bit
        }
        System.arraycopy(rePerm, 0, re, 0, n)
        System.arraycopy(imPerm, 0, im, 0, n)

        val sign = if (inverse) 1.0f else -1.0f
        var len = 2
        while (len <= n) {
            val ang = sign * 2.0f * Math.PI.toFloat() / len
            val wr = kotlin.math.cos(ang)
            val wi = kotlin.math.sin(ang)
            var start = 0
            while (start < n) {
                var cRe = 1.0f
                var cIm = 0.0f
                for (k in 0 until len / 2) {
                    val a = start + k
                    val b = start + k + len / 2
                    val vRe = re[b] * cRe - im[b] * cIm
                    val vIm = re[b] * cIm + im[b] * cRe
                    val uRe = re[a]
                    val uIm = im[a]
                    re[a] = uRe + vRe
                    im[a] = uIm + vIm
                    re[b] = uRe - vRe
                    im[b] = uIm - vIm
                    val nRe = cRe * wr - cIm * wi
                    cIm = cRe * wi + cIm * wr
                    cRe = nRe
                }
                start += len
            }
            len = len shl 1
        }
        if (inverse) {
            for (i in 0 until n) {
                re[i] /= n
                im[i] /= n
            }
        }
    }

    /**
     * Frame → 257-bin complex spectrum [bin][re, im] (GTCRN `mix` layout:
     * [1, 257, 1, 2] flattened). The caller supplies exactly FFT_SIZE samples.
     */
    fun stft(frame: FloatArray): Array<FloatArray> {
        require(frame.size == FFT_SIZE) { "frame must be $FFT_SIZE samples" }
        val re = FloatArray(FFT_SIZE) { frame[it] * HANN[it] }
        val im = FloatArray(FFT_SIZE)
        fft(re, im, inverse = false)
        return Array(BINS) { b -> floatArrayOf(re[b], im[b]) }
    }

    /** 257-bin spectrum → FFT_SIZE time samples (windowed; overlap-add by caller). */
    fun istft(spec: Array<FloatArray>): FloatArray {
        require(spec.size == BINS) { "spectrum must have $BINS bins" }
        val re = FloatArray(FFT_SIZE)
        val im = FloatArray(FFT_SIZE)
        for (b in 0 until BINS) {
            re[b] = spec[b][0]
            im[b] = spec[b][1]
        }
        for (b in 1 until FFT_SIZE / 2) { // conjugate-symmetric fill
            re[FFT_SIZE - b] = re[b]
            im[FFT_SIZE - b] = -im[b]
        }
        fft(re, im, inverse = true)
        return FloatArray(FFT_SIZE) { i -> re[i] * HANN[i] }
    }
}
