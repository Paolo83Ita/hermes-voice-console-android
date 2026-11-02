package com.hermestower.voiceconsole.ui.runtime

import androidx.compose.runtime.MonotonicFrameClock
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * GRAPH 4 NODE 0 (CP-G4-002) — required test set for the frame-timing controller
 * that replaced the lint-unverifiable produceState loop in OracleCore/TowerCore.
 *
 * `rememberAnimationFrameTimeMs` wraps `pumpAnimationFrameTime` in exactly one
 * Compose primitive: `LaunchedEffect(running) { pumpAnimationFrameTime(running) }`.
 * Compose guarantees that a LaunchedEffect coroutine is cancelled when the
 * composable leaves composition and re-launched when the key changes — so the
 * behavioral surface of the composable reduces to the pump contract tested here:
 *
 *  1. animation state updates when enabled        -> `pump advances the emitted time…`
 *  2. static in reduced-motion mode               -> `pump emits exactly one static zero…`
 *     (reduced-motion/LOW simply re-launch the pump with running=false)
 *  3. animation work stops when the Composable
 *     leaves composition                          -> `cancelled pump stops consuming frames`
 *     (disposal cancels the LaunchedEffect coroutine — the only cancellation path)
 *  4. state changes restart/update correctly      -> `pump restarts from a fresh start frame…`
 *  5. no orphan coroutine remains                 -> same cancellation test: frames sent
 *     after cancellation are consumed by nobody
 *  6. OLED idle behavior inside declared policy   -> static frame is exactly 0 ms and
 *     emission STOPS (no per-frame redraw churn while idle)
 *
 * Frame model note (matches the device Choreographer contract): the FIRST
 * `withFrameNanos { it }` call inside the pump consumes the current frame time as the
 * start reference and emits nothing; every subsequent frame emits `(now - start)` ms.
 */
class AnimationFrameClockTest {

    /**
     * Minimal test MonotonicFrameClock: each frame time is handed to exactly one
     * `withFrameNanos` waiter, in send order. Cancelling the waiter (compose
     * effect disposal in production) unblocks the receive and stops frame work.
     */
    private class ManualFrameClock : MonotonicFrameClock {

        override val key: CoroutineContext.Key<*> get() = MonotonicFrameClock

        private val frames = Channel<Long>(Channel.UNLIMITED)

        override suspend fun <R> withFrameNanos(onFrame: (Long) -> R): R =
            onFrame(frames.receive())

        /** Dispatches one frame time to the next waiting frame callback. */
        suspend fun advanceFrame(timeNanos: Long) {
            frames.send(timeNanos)
        }
    }

    // ---- 1. animation state updates when enabled -------------------------------

    @Test(timeout = 30_000)
    fun `pump advances the emitted time when running`() = runBlocking {
        val emissions = mutableListOf<Long>()
        val clock = ManualFrameClock()
        val job = launch(clock) {
            pumpAnimationFrameTime(running = true) { emissions += it }
        }
        yield() // pump is parked in its start-frame receive
        clock.advanceFrame(1_000_000_000L); yield() // start reference consumed, no emit yet
        assertEquals(0, emissions.size)
        clock.advanceFrame(1_500_000_000L); yield() // +0.5 s -> 500 ms
        assertEquals(listOf(500L), emissions)
        clock.advanceFrame(2_750_000_000L); yield() // +1.75 s -> 1750 ms
        assertEquals(listOf(500L, 1750L), emissions)
        assertTrue(emissions.zipWithNext().all { (a, b) -> b >= a })
        job.cancel()
    }

    // ---- 2 & 6. static in reduced-motion / OLED idle policy --------------------

    @Test(timeout = 30_000)
    fun `pump emits exactly one static zero frame when not running`() = runBlocking {
        val emissions = mutableListOf<Long>()
        // In the composable this is the LaunchedEffect body when running=false
        // (reduced motion or the LOW quality profile).
        pumpAnimationFrameTime(running = false) { emissions += it }
        // Exactly ONE emission, then completion: no per-frame churn while the
        // console is idle (OLED burn-in protection stays inside its policy).
        assertEquals(listOf(0L), emissions)
    }

    // ---- 3 & 5. stops on cancellation; no orphan coroutine ---------------------

    @Test(timeout = 30_000)
    fun `cancelled pump stops consuming frames`() = runBlocking {
        val emissions = mutableListOf<Long>()
        val clock = ManualFrameClock()
        val job = launch(clock) {
            pumpAnimationFrameTime(running = true) { emissions += it }
        }
        yield() // parked in start-frame receive
        clock.advanceFrame(1_000_000_000L); yield() // start consumed
        clock.advanceFrame(2_000_000_000L); yield() // one tick emitted
        assertEquals(1, emissions.size)
        // Cancellation = the exact mechanism compose disposal triggers.
        job.cancel()
        yield()
        // Frames dispatched after cancellation must find NO waiter — the mechanism
        // that guarantees "animation work stops when the Composable leaves
        // composition" and "no orphan coroutine remains".
        clock.advanceFrame(3_000_000_000L); yield()
        clock.advanceFrame(4_000_000_000L); yield()
        assertEquals(1, emissions.size)
    }

    // ---- 4. state changes restart/update correctly -----------------------------

    @Test(timeout = 30_000)
    fun `pump restarts from a fresh start frame after re-enable`() = runBlocking {
        val emissions = mutableListOf<Long>()
        val clock = ManualFrameClock()
        val job = launch(clock) {
            pumpAnimationFrameTime(running = true) { emissions += it }
        }
        yield()
        clock.advanceFrame(1_000_000_000L); yield() // start = 1e9
        clock.advanceFrame(2_000_000_000L); yield() // tick  = 1000 ms
        assertEquals(1000L, emissions.last())
        job.cancel()

        // Second lifecycle: re-enabled — the effect relaunches with a fresh start.
        val second = mutableListOf<Long>()
        val job2 = launch(clock) {
            pumpAnimationFrameTime(running = true) { second += it }
        }
        yield()
        clock.advanceFrame(4_000_000_000L); yield() // fresh start = 4e9
        clock.advanceFrame(5_000_000_000L); yield() // tick
        // (5e9 - 4e9) = 1000 ms — NOT (5e9 - 1e9) = 4000 ms, proving the start
        // reference is fresh rather than stale from the cancelled lifecycle.
        assertEquals(1000L, second.last())
        job2.cancel()
    }
}
