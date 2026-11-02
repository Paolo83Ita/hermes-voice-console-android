package com.hermestower.voiceconsole.voice

/**
 * Energy-based VAD (GRAPH 4 NODE 3): a real, dependency-free voice activity
 * detector on RMS energy with hysteresis. Honest scope: it is the baseline VAD
 * that always works; the Silero model (org.silero.vad.v5) becomes the primary
 * engine after its runtime is integrated and qualified on the physical device.
 */
class EnergyVadEngine(
    private val rmsThreshold: Double = 450.0,
    private val hangoverFrames: Int = 6,
) : VadEngine {

    private var hangover = 0

    override fun isSpeech(frame: AudioFrame): Boolean {
        var sum = 0.0
        for (s in frame.samples) sum += s.toDouble() * s
        val rms = kotlin.math.sqrt(sum / frame.samples.size.coerceAtLeast(1))
        val speech = rms >= rmsThreshold
        if (speech) hangover = hangoverFrames else if (hangover > 0) hangover--
        return speech || hangover > 0
    }

    override fun reset() {
        hangover = 0
    }
}
