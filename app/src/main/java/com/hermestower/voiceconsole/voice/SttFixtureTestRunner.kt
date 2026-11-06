package com.hermestower.voiceconsole.voice

import java.io.ByteArrayInputStream
import java.io.File

/**
 * CP-G4.9-004 — the deterministic Italian STT fixture test.
 *
 * The bundled fixture `models/fixtures/fixture_benvenuto_torre_it.wav` is REAL
 * Italian speech (the physically verified phrase “Benvenuto alla torre di
 * Hermes, il canale vocale è pronto.”), synthesised with the SAME verified
 * piper voice (vits-piper-it_IT-riccardo-x_low) the natural TTS route speaks
 * with: the Sanctum Test action proves the WHOLE local loop — real speech
 * audio in, vosk transcript out — without touching the microphone or the
 * network.
 *
 * Honesty contract:
 *  - the runner feeds REAL 16 kHz mono PCM frames to the [VoskFrameSttEngine]
 *    (load → accept → finalizeTranscript → unload);
 *  - the transcript returned is what the engine produced — never the expected
 *    phrase (the caller may compare and report the similarity itself);
 *  - any failure is typed.
 */
object SttFixtureTestRunner {

    data class Result(
        val success: Boolean,
        val transcript: String?,
        val frameCount: Int,
        val durationSeconds: Float,
        val error: String? = null,
    )

    /** Minimal PCM16-mono WAV parser (RIFF walk; validates the format honestly). */
    fun parsePcm16Mono16k(bytes: ByteArray): ShortArray {
        val stream = ByteArrayInputStream(bytes)
        val riff = ByteArray(12)
        if (stream.read(riff) != 12) throw IllegalArgumentException("not a RIFF file")
        if (String(riff, 0, 4) != "RIFF") throw IllegalArgumentException("not a RIFF file")
        var channels = -1
        var sampleRate = -1
        var bitsPerSample = -1
        val pcm = mutableListOf<ShortArray>()
        while (true) {
            val header = ByteArray(8)
            val read = stream.read(header)
            if (read < 8) break
            val chunkId = String(header, 0, 4)
            val chunkSize = (header[4].toInt() and 0xFF) or
                ((header[5].toInt() and 0xFF) shl 8) or
                ((header[6].toInt() and 0xFF) shl 16) or
                ((header[7].toInt() and 0xFF) shl 24)
            if (chunkId == "fmt ") {
                val fmt = ByteArray(chunkSize)
                if (stream.read(fmt) != chunkSize) throw IllegalArgumentException("truncated fmt")
                channels = (fmt[2].toInt() and 0xFF) or ((fmt[3].toInt() and 0xFF) shl 8)
                sampleRate = (fmt[4].toInt() and 0xFF) or ((fmt[5].toInt() and 0xFF) shl 8) or
                    ((fmt[6].toInt() and 0xFF) shl 16) or ((fmt[7].toInt() and 0xFF) shl 24)
                bitsPerSample = (fmt[14].toInt() and 0xFF) or ((fmt[15].toInt() and 0xFF) shl 8)
            } else if (chunkId == "data") {
                val data = ByteArray(chunkSize)
                if (stream.read(data) != chunkSize) throw IllegalArgumentException("truncated data")
                val samples = ShortArray(chunkSize / 2)
                for (i in samples.indices) {
                    samples[i] = ((data[2 * i].toInt() and 0xFF) or ((data[2 * i + 1].toInt() and 0xFF) shl 8)).toShort()
                }
                pcm += samples
            } else {
                // Skip unknown chunks (LIST, fact, …).
                var skipped = 0
                val buf = ByteArray(4096)
                while (skipped < chunkSize) {
                    val n = stream.read(buf, 0, minOf(buf.size, chunkSize - skipped))
                    if (n <= 0) break
                    skipped += n
                }
            }
        }
        if (channels != 1 || sampleRate != 16_000 || bitsPerSample != 16) {
            throw IllegalArgumentException("fixture must be mono 16 kHz PCM16 (got ch=$channels rate=$sampleRate bits=$bitsPerSample)")
        }
        return pcm.reduce { a, b -> a + b }
    }

    /** Runs the fixture through the engine; load/unload around the utterance. */
    fun run(engine: SttEngine, wavBytes: ByteArray, frameMs: Int = 100): Result {
        val pcm = try {
            parsePcm16Mono16k(wavBytes)
        } catch (e: IllegalArgumentException) {
            return Result(false, null, 0, 0f, e.message)
        }
        val samplesPerFrame = 16_000 * frameMs / 1000
        engine.load()
        if (engine.state.value.state != SttState.READY) {
            return Result(false, null, 0, 0f, "offline STT model not installed or failed to load (${engine.state.value.state})")
        }
        var finals = StringBuilder()
        var frames = 0
        var i = 0
        while (i < pcm.size) {
            val chunk = pcm.copyOfRange(i, minOf(i + samplesPerFrame, pcm.size))
            val out = engine.accept(AudioFrame(samples = chunk))
            if (out.state == SttState.FAILED) {
                engine.unload()
                return Result(false, null, frames, 0f, out.error?.detail ?: "frame decode failed")
            }
            out.finalTranscript?.let { finals.append(it).append(' ') }
            i += samplesPerFrame
            frames++
        }
        val final = engine.finalizeTranscript()
        engine.unload()
        final.finalTranscript?.let { finals.append(it) }
        val transcript = finals.toString().trim()
        return Result(
            success = transcript.isNotBlank(),
            transcript = transcript.ifBlank { null },
            frameCount = frames,
            durationSeconds = pcm.size / 16_000f,
        )
    }

    /** Convenience: reads the fixture from the installed-bundle assets dir. */
    fun fixtureFile(modelsDir: File): File = File(modelsDir, "fixtures/fixture_benvenuto_torre_it.wav")
}
