package com.hermestower.voiceconsole.voice

import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/**
 * GRAPH 4 G4-C02 (§4): real Android microphone frame source.
 *
 * Physical contract:
 *  - 16 kHz mono PCM 16-bit by default (§4 preferred format), 20 ms frames;
 *  - capture runs on a dedicated daemon thread; frames move to a BOUNDED
 *    in-memory queue (drop-oldest, ~500 ms) and are consumed in-flight —
 *    nothing retains raw audio (INV-G4-09);
 *  - stop() is physically immediate: the worker exits, the hardware handle is
 *    stopped AND released, the pending queue is cleared (privacy mute physics
 *    INV-G4-10 — no frame captured after stop can ever be read);
 *  - every failure is typed: PERMISSION_DENIED, DEVICE_BUSY, INIT_FAILED,
 *    UNSUPPORTED_FORMAT, READ_FAILURE;
 *  - double start and double stop are idempotent and counted (truthful
 *    diagnostics, no hidden exceptions);
 *  - no raw audio, no PCM bytes, in logs or exceptions (privacy contract).
 *
 * The hardware handle is abstracted behind [MicHandle] so the full lifecycle
 * matrix (start/stop/double/lifecycle/release/read-failure) is testable on the
 * JVM with deterministic fakes; [AndroidMicHandleFactory] is the only class
 * that touches android.media.
 */
interface MicDiagnostics {
    /** Last typed failure, null when the source never failed. */
    val lastFailure: MicSourceException?
}

enum class MicFailureKind {
    PERMISSION_DENIED,
    DEVICE_BUSY,
    INIT_FAILED,
    UNSUPPORTED_FORMAT,
    READ_FAILURE,
}

/** Typed microphone failure. Detail NEVER contains audio bytes. */
class MicSourceException(val kind: MicFailureKind, message: String) : Exception(message)

/**
 * Abstraction over one hardware capture handle (android.media.AudioRecord in
 * production). Constants mirror AudioRecord states without importing it so
 * JVM tests can implement the interface without the Android runtime.
 */
interface MicHandle {
    fun read(target: ShortArray): Int
    fun startRecording()
    fun stopCapture()
    fun release()
}

/** Creates one hardware handle for the requested sample rate. */
fun interface MicHandleFactory {
    /** @throws MicSourceException on typed failure (e.g. DEVICE_BUSY, UNSUPPORTED_FORMAT). */
    fun create(sampleRateHz: Int): MicHandle
}

class AudioRecordFrameSource(
    private val handleFactory: MicHandleFactory,
    private val sampleRateHz: Int = 16_000,
    private val frameMs: Int = 20,
    /** Physical gate checked BEFORE any hardware access. */
    private val permissionGranted: () -> Boolean = { true },
    /** Bounded buffer: ~500 ms at the default frame size. */
    private val queueCapacityFrames: Int = 25,
) : AudioFrameSource, MicDiagnostics {

    @Volatile override var lastFailure: MicSourceException? = null
        private set

    // Truthful diagnostics counters.
    @Volatile var framesDelivered = 0
        private set
    @Volatile var framesDropped = 0
        private set
    @Volatile var doubleStarts = 0
        private set
    @Volatile var doubleStops = 0
        private set
    @Volatile var factoryCalls = 0
        private set
    @Volatile var workerAlive: Boolean = false
        private set

    @Volatile private var active = false
    private val released = AtomicBoolean(false)
    private val queue = ArrayBlockingQueue<AudioFrame>(queueCapacityFrames)
    @Volatile private var currentHandle: MicHandle? = null
    private var worker: Thread? = null

    val samplesPerFrame: Int get() = sampleRateHz * frameMs / 1000

    override val isActive: Boolean get() = active

    override fun start() {
        if (active) {
            doubleStarts++
            return
        }
        lastFailure = null
        if (!permissionGranted()) {
            fail(MicFailureKind.PERMISSION_DENIED, "microphone permission not granted; no hardware access attempted")
            return
        }
        factoryCalls++
        val handle: MicHandle = try {
            handleFactory.create(sampleRateHz)
        } catch (e: MicSourceException) {
            fail(e.kind, e.message ?: e.kind.name)
            return
        } catch (e: Exception) {
            fail(MicFailureKind.INIT_FAILED, "handle creation failed")
            return
        }
        handle.startRecording()
        active = true
        currentHandle = handle
        queue.clear()
        released.set(false)
        worker = Thread { captureLoop(handle) }.apply {
            name = "hermes-mic-capture"
            isDaemon = true
            start()
        }
    }

    private fun captureLoop(handle: MicHandle) {
        workerAlive = true
        val buffer = ShortArray(samplesPerFrame)
        while (active) {
            val n = try {
                handle.read(buffer)
            } catch (e: Exception) {
                fail(MicFailureKind.READ_FAILURE, "capture read threw")
                active = false // a failed source is NOT active: truthful diagnostics
                break
            }
            if (!active) break // stopped while reading: drop everything, no retention
            if (n < 0) {
                fail(MicFailureKind.READ_FAILURE, "capture read returned error code")
                active = false // a failed source is NOT active: truthful diagnostics
                break
            }
            if (n == 0) continue
            val frame = AudioFrame(buffer.copyOf(n), sampleRateHz)
            if (!queue.offer(frame)) {
                queue.poll()
                framesDropped++
                queue.offer(frame)
            }
        }
        // Either stop() was called or the capture failed: in both cases the
        // hardware is released exactly once and pending audio is dropped (a
        // failed or stopped source must not leak buffered audio).
        releaseHardware(handle)
        queue.clear()
        workerAlive = false
    }

    /** Stops and releases the hardware handle exactly once per session. */
    private fun releaseHardware(handle: MicHandle) {
        if (released.compareAndSet(false, true)) {
            runCatching { handle.stopCapture() }
            runCatching { handle.release() }
        }
    }

    override fun stop() {
        if (!active && worker == null) {
            doubleStops++
            return
        }
        active = false
        val w = worker
        worker = null
        if (w != null) {
            try {
                w.join(JOIN_TIMEOUT_MS)
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
            }
        }
        currentHandle?.let { releaseHardware(it) }
        currentHandle = null
        // No retention: pending frames are dropped, never readable after stop.
        queue.clear()
        workerAlive = false
    }

    override suspend fun read(): AudioFrame? {
        while (true) {
            if (!active) return null
            val frame = try {
                queue.poll(READ_POLL_TIMEOUT_MS, TimeUnit.MILLISECONDS)
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
                return null
            }
            if (frame != null) {
                framesDelivered++
                return frame
            }
            // Loop re-checks active so a stop during the poll returns promptly.
        }
    }

    private fun fail(kind: MicFailureKind, detail: String) {
        lastFailure = MicSourceException(kind, detail)
    }

    companion object {
        private const val READ_POLL_TIMEOUT_MS = 50L
        private const val JOIN_TIMEOUT_MS = 2_000L
    }
}
