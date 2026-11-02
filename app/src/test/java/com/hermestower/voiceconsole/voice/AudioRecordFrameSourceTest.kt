package com.hermestower.voiceconsole.voice

import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/**
 * GRAPH 4 G4-C02 (§4): full lifecycle matrix of the real AudioRecord frame
 * source against deterministic JVM handles — permission missing, typed init /
 * busy / format / read failures, capture start, frame delivery geometry, stop,
 * privacy-mute physics (no retention), cancellation, double start/stop,
 * lifecycle stop from another thread, and one-shot native release.
 */
class AudioRecordFrameSourceTest {

    // ---- deterministic fake handle ------------------------------------------

    /** Fake hardware handle: scripted read behavior, records lifecycle facts. */
    private class FakeHandle(
        private val readBehavior: (ShortArray) -> Int = { it.size },
        private val throwOnRead: Boolean = false,
    ) : MicHandle {
        val startRecordingCalls = AtomicInteger(0)
        val stopCalls = AtomicInteger(0)
        val releaseCalls = AtomicInteger(0)
        override fun read(target: ShortArray): Int {
            if (throwOnRead) throw IllegalStateException("scripted driver crash")
            return readBehavior(target)
        }
        override fun startRecording() { startRecordingCalls.incrementAndGet() }
        override fun stopCapture() { stopCalls.incrementAndGet() }
        override fun release() { releaseCalls.incrementAndGet() }
    }

    private class FakeFactory(
        private val handle: FakeHandle? = FakeHandle(),
        private val throwKind: MicFailureKind? = null,
    ) : MicHandleFactory {
        var calls = 0
        override fun create(sampleRateHz: Int): MicHandle {
            calls++
            throwKind?.let { throw MicSourceException(it, "scripted $it") }
            return handle!!
        }
    }

    /** Sine-ish PCM generator that fills the whole buffer each call. */
    private fun pcmFiller(counter: AtomicInteger): (ShortArray) -> Int = { buf ->
        val phase = counter.getAndIncrement()
        for (i in buf.indices) {
            buf[i] = ((i + phase) % 100 * 120).toInt().toShort()
        }
        buf.size
    }

    private fun newSource(
        factory: MicHandleFactory,
        permission: Boolean = true,
    ): AudioRecordFrameSource = AudioRecordFrameSource(
        handleFactory = factory,
        sampleRateHz = 16_000,
        frameMs = 20,
        permissionGranted = { permission },
    )

    // ---- §4 test matrix ------------------------------------------------------

    @Test
    fun `permission missing - typed failure before any hardware access`() {
        val factory = FakeFactory()
        val source = newSource(factory, permission = false)
        source.start()
        assertFalse(source.isActive)
        assertNotNull(source.lastFailure)
        assertEquals(MicFailureKind.PERMISSION_DENIED, source.lastFailure!!.kind)
        assertEquals(0, factory.calls) // hardware NEVER touched without consent
        assertTrue(source.factoryCalls == 0)
        val frame = runBlocking { source.read() }
        assertNull(frame)
    }

    @Test
    fun `initialization failure - typed INIT_FAILED and no capture`() {
        val source = newSource(FakeFactory(handle = FakeHandle())) // handle created but init never checked here
        // INIT_FAILED path: factory throws the generic failure (hardware refused init).
        val failing = newSource(MicHandleFactory { throw IllegalStateException("native init failed") })
        failing.start()
        assertNotNull(failing.lastFailure)
        assertEquals(MicFailureKind.INIT_FAILED, failing.lastFailure!!.kind)
        assertFalse(failing.isActive)
        assertTrue(source.samplesPerFrame == 320)
    }

    @Test
    fun `device busy - typed DEVICE_BUSY failure`() {
        val source = newSource(FakeFactory(throwKind = MicFailureKind.DEVICE_BUSY))
        source.start()
        assertFalse(source.isActive)
        assertEquals(MicFailureKind.DEVICE_BUSY, source.lastFailure!!.kind)
    }

    @Test
    fun `unsupported format - typed UNSUPPORTED_FORMAT failure`() {
        val source = newSource(FakeFactory(throwKind = MicFailureKind.UNSUPPORTED_FORMAT))
        source.start()
        assertFalse(source.isActive)
        assertEquals(MicFailureKind.UNSUPPORTED_FORMAT, source.lastFailure!!.kind)
    }

    @Test
    fun `capture start - worker runs, handle recording, source active`() = runBlocking {
        val handle = FakeHandle(readBehavior = { 0 }) // silent device: no frames
        val source = newSource(FakeFactory(handle))
        source.start()
        assertTrue(source.isActive)
        assertEquals(1, handle.startRecordingCalls.get())
        // Give the worker a moment, then stop cleanly.
        Thread.sleep(80)
        assertTrue(source.isActive) // silent device does NOT stop the source
        source.stop()
        assertFalse(source.isActive)
    }

    @Test
    fun `frame delivery - 20ms frames at 16kHz, geometry correct`() = runBlocking {
        val counter = AtomicInteger(0)
        val handle = FakeHandle(readBehavior = pcmFiller(counter))
        val source = newSource(FakeFactory(handle))
        source.start()
        val f1 = withTimeout(2_000) { source.read() }
        val f2 = withTimeout(2_000) { source.read() }
        assertNotNull(f1)
        assertNotNull(f2)
        assertEquals(320, f1!!.samples.size)
        assertEquals(16_000, f1.sampleRate)
        assertEquals(20L, f1.durationMs)
        assertEquals(320, f2!!.samples.size)
        assertTrue(source.framesDelivered >= 2)
        source.stop()
        assertFalse(source.isActive)
    }

    @Test
    fun `capture stop - no frame readable after stop, queue dropped (privacy physics)`() = runBlocking {
        val counter = AtomicInteger(0)
        val handle = FakeHandle(readBehavior = pcmFiller(counter))
        val source = newSource(FakeFactory(handle), )
        source.start()
        // Fill the pipeline a bit.
        withTimeout(2_000) { source.read() }
        source.stop()
        val after = source.read()
        assertNull(after) // buffered audio is DROPPED, never readable post-stop
        assertFalse(source.isActive)
    }

    @Test
    fun `privacy mute path - stop releases hardware exactly once`() = runBlocking {
        val handle = FakeHandle(readBehavior = pcmFiller(AtomicInteger(0)))
        val source = newSource(FakeFactory(handle))
        source.start()
        withTimeout(2_000) { source.read() }
        source.stop()
        assertEquals(1, handle.stopCalls.get())
        assertEquals(1, handle.releaseCalls.get())
    }

    @Test
    fun `cancellation - read returns promptly after stop from another thread`() = runBlocking {
        val handle = FakeHandle(readBehavior = { 0 }) // never produces data
        val source = newSource(FakeFactory(handle))
        source.start()
        val done = CountDownLatch(1)
        // IO dispatcher: a REAL parallel thread parks inside the bounded poll.
        launch(kotlinx.coroutines.Dispatchers.IO) {
            val frame = source.read()
            assertNull(frame) // stopped source yields null, not a hang
            done.countDown()
        }
        Thread.sleep(120) // reader now parked in the bounded poll
        source.stop()
        assertTrue("read() must unblock after stop", done.await(2, TimeUnit.SECONDS))
    }

    @Test
    fun `double start - idempotent, single worker, capture continues`() = runBlocking {
        val counter = AtomicInteger(0)
        val handle = FakeHandle(readBehavior = pcmFiller(counter))
        val source = newSource(FakeFactory(handle))
        source.start()
        source.start()
        source.start()
        assertEquals(2, source.doubleStarts)
        assertEquals(1, handle.startRecordingCalls.get())
        withTimeout(2_000) { assertNotNull(source.read()) }
        source.stop()
        assertFalse(source.isActive)
    }

    @Test
    fun `double stop - idempotent, single release`() {
        val handle = FakeHandle()
        val source = newSource(FakeFactory(handle))
        source.start()
        source.stop()
        source.stop()
        source.stop()
        assertEquals(2, source.doubleStops)
        assertEquals("release exactly once", 1, handle.releaseCalls.get())
    }

    @Test
    fun `lifecycle stop - worker thread terminated within join timeout`() = runBlocking {
        val counter = AtomicInteger(0)
        val handle = FakeHandle(readBehavior = pcmFiller(counter))
        val source = newSource(FakeFactory(handle))
        source.start()
        withTimeout(2_000) { source.read() }
        source.stop()
        assertFalse("worker must be dead after stop", source.workerAlive)
        assertFalse(source.isActive)
    }

    @Test
    fun `read failure - typed READ_FAILURE, source goes inactive, no hang`() = runBlocking {
        val handle = FakeHandle(throwOnRead = true)
        val source = newSource(FakeFactory(handle))
        source.start()
        val frame = withTimeout(3_000) { source.read() }
        assertNull(frame)
        assertEquals(MicFailureKind.READ_FAILURE, source.lastFailure!!.kind)
        assertFalse(source.isActive) // failed source is truthfully inactive
        assertEquals(1, handle.releaseCalls.get()) // hardware still released
    }

    @Test
    fun `frame geometry - configured sample rate flows into AudioFrame`() {
        val source = AudioRecordFrameSource(
            handleFactory = FakeFactory(),
            sampleRateHz = 48_000,
            frameMs = 10,
            permissionGranted = { true },
        )
        assertEquals(480, source.samplesPerFrame)
    }

    @Test
    fun `bounded queue - no unbounded retention under stalled consumer`() = runBlocking {
        val counter = AtomicInteger(0)
        val handle = FakeHandle(readBehavior = pcmFiller(counter))
        // Capacity 2: consumer stalls; producer must drop-oldest, not grow.
        val source = AudioRecordFrameSource(FakeFactory(handle), 16_000, 20, { true }, 2)
        source.start()
        Thread.sleep(150) // producer fills ~7 frames against a 2-slot queue
        assertTrue("drop-oldest must cap retention", source.framesDropped > 0)
        source.stop()
        val post = source.read()
        assertNull(post) // nothing stale retained
    }
}
