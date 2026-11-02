package com.hermestower.voiceconsole.voice

import com.hermestower.voiceconsole.voice.dsp.Dsp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * GRAPH 4 CP-G4-008 — DSP numerical contract tests (pure JVM, no fixtures).
 */
class DspTest {

    @Test
    fun `fft of pure cosine peaks in bin 1 and N-1`() {
        val n = 64
        val re = FloatArray(n) { cos(2.0 * PI * it / n).toFloat() }
        val im = FloatArray(n)
        Dsp.fft(re, im)
        val mag = FloatArray(n) { hypot(re[it], im[it]) }
        val maxBin = mag.indices.maxByOrNull { mag[it] }!!
        assertTrue("peak bin was $maxBin", maxBin == 1 || maxBin == n - 1)
        // bin 1 and bin n-1 carry ~n/2 amplitude each
        assertTrue(mag[1] > n / 2 - 1f)
        assertTrue(mag[n - 1] > n / 2 - 1f)
    }

    @Test
    fun `stft istft round trip reconstructs sine within 1e-3 at frame center`() {
        val frame = FloatArray(Dsp.FFT_SIZE) { 0.1f * sin(2.0 * PI * 3.0 * it / Dsp.FFT_SIZE).toFloat() }
        val spec = Dsp.stft(frame)
        assertEquals(Dsp.BINS, spec.size)
        val rec = Dsp.istft(spec)
        // istft(stft(x)) = x·HANN² (analysis×synthesis windowing) — the WOLA
        // caller normalizes by the accumulated window energy. Verify the exact
        // contract at interior points where the window is well above zero.
        var maxErr = 0f
        for (i in 64 until Dsp.FFT_SIZE - 64) {
            val expected = frame[i] * Dsp.HANN[i] * Dsp.HANN[i]
            maxErr = maxOf(maxErr, kotlin.math.abs(rec[i] - expected))
        }
        assertTrue("max reconstruction error $maxErr", maxErr < 1e-3f)
    }

    @Test
    fun `parseval energy is conserved`() {
        val frame = FloatArray(Dsp.FFT_SIZE) { sin(2.0 * PI * 5.0 * it / Dsp.FFT_SIZE).toFloat() }
        val timeEnergy = frame.sumOf { (it * it).toDouble() }
        val spec = Dsp.stft(frame)
        // stft applies the window first — compare against the WINDOWED energy
        val windowedEnergy = frame.indices.sumOf { (frame[it] * Dsp.HANN[it]).let { v -> (v * v).toDouble() } }
        var specEnergy = 0.0
        for (b in 0 until Dsp.BINS) specEnergy += (spec[b][0] * spec[b][0] + spec[b][1] * spec[b][1]).toDouble()
        // rFFT stores half the spectrum; total energy = 2*sum(half) - dc - nyquist.
        // FFT forward convention carries NO 1/N scaling: Parseval is
        // Σ|x|² = (1/N)·Σ|X|² → Σ|X|² = N·Σ|x_windowed|².
        val total = 2 * specEnergy - (spec[0][0] * spec[0][0] + spec[0][1] * spec[0][1]) -
            (spec[Dsp.BINS - 1][0] * spec[Dsp.BINS - 1][0] + spec[Dsp.BINS - 1][1] * spec[Dsp.BINS - 1][1])
        assertEquals(windowedEnergy * Dsp.FFT_SIZE, total, windowedEnergy * Dsp.FFT_SIZE * 1e-4)
    }

    private fun hypot(a: Float, b: Float): Float = kotlin.math.sqrt(a * a + b * b)
}
