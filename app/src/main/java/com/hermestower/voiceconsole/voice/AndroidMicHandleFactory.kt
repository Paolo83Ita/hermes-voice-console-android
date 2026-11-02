package com.hermestower.voiceconsole.voice

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder

/**
 * GRAPH 4 G4-C02 (§4): production [MicHandleFactory] on top of the real
 * android.media.AudioRecord. Format negotiation: 16 kHz mono PCM 16-bit with
 * read-back of the actually-created format; minSdk 30 has full support.
 *
 * The factory is the ONLY class in the app that constructs an AudioRecord, so
 * every JVM lifecycle test can run against fakes while the physical behavior
 * stays concentrated here.
 */
class AndroidMicHandleFactory(
    /** RECORD_AUDIO check supplied by the caller (MicPermissionFlow-driven UI). */
    private val hasPermission: () -> Boolean,
    private val audioSource: Int = MediaRecorder.AudioSource.VOICE_RECOGNITION,
) : MicHandleFactory {

    @SuppressLint("MissingPermission") // permission is enforced by hasPermission() in AudioRecordFrameSource.start()
    override fun create(sampleRateHz: Int): MicHandle {
        val minBufferBytes = AudioRecord.getMinBufferSize(
            sampleRateHz,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
        )
        if (minBufferBytes <= 0) {
            throw MicSourceException(
                MicFailureKind.UNSUPPORTED_FORMAT,
                "getMinBufferSize($sampleRateHz, MONO, PCM16) = $minBufferBytes",
            )
        }
        // 4x min buffer: enough slack that a briefly stalled consumer never
        // overruns the driver queue on the target device.
        val bufferBytes = maxOf(minBufferBytes * 4, sampleRateHz / 5 * 2) // >= 200 ms of PCM16 mono
        val record = AudioRecord(
            audioSource,
            sampleRateHz,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            bufferBytes,
        )
        val initialized = try {
            record.state == AudioRecord.STATE_INITIALIZED
        } catch (e: Exception) {
            runCatching { record.release() }
            throw MicSourceException(MicFailureKind.INIT_FAILED, "AudioRecord state probe failed")
        }
        if (!initialized) {
            val detail = "AudioRecord not initialized (state=${record.state})"
            runCatching { record.release() }
            throw MicSourceException(MicFailureKind.INIT_FAILED, detail)
        }
        return AndroidMicHandle(record, sampleRateHz, initialized = true)
    }

    fun isPermissionGranted(): Boolean = hasPermission()
}

/**
 * Real handle wrapper. Read contract: [MicHandle.read] returns the number of
 * samples read (0 = no data yet) or a negative error code, mirroring
 * AudioRecord.read(short[], int).
 */
private class AndroidMicHandle(
    private val record: AudioRecord,
    val sampleRateHz: Int,
    val initialized: Boolean,
) : MicHandle {

    override fun read(target: ShortArray): Int =
        try {
            record.read(target, 0, target.size) // blocking read; 0 or negative per AudioRecord contract
        } catch (e: IllegalStateException) {
            ERROR_READ_FAILED
        }

    override fun startRecording() {
        runCatching { record.startRecording() }
    }

    override fun stopCapture() {
        runCatching { record.stop() }
    }

    override fun release() {
        runCatching { record.release() }
    }

    companion object {
        /** Sentinel for read failure inside [MicHandle] (AudioRecord returns negative codes too). */
        const val ERROR_READ_FAILED = -1000
    }
}
