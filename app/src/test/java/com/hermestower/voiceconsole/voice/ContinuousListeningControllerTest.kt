package com.hermestower.voiceconsole.voice

import com.hermestower.voiceconsole.core.state.ConsoleState
import com.hermestower.voiceconsole.domain.LocalEventStore
import com.hermestower.voiceconsole.migration.InMemoryKeyValueStore
import com.hermestower.voiceconsole.store.InMemoryEventStore
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * GRAPH 4.10 CP-G4.10-006 (§15): the continuous-listening loop — start, the
 * 12-step resume cycle, the typed stop conditions and the foreground contract.
 * Deterministic virtual time; the capture seam is scripted.
 */
class ContinuousListeningControllerTest {

    private val events = InMemoryEventStore()
    private var consoleState = ConsoleState.IDLE
    private var muted = false
    private var permission = true
    private var captureStarted = 0
    private var captureStopped = 0
    private var startResult = true

    private fun buildController(scope: kotlinx.coroutines.CoroutineScope): ContinuousListeningController {
        val policy = ListeningModePolicy(InMemoryKeyValueStore())
        return ContinuousListeningController(
            scope = scope,
            storeProvider = { consoleState },
            policy = policy,
            events = events,
            startCapture = {
                captureStarted++
                startResult
            },
            stopCapture = { captureStopped++ },
            hasMicPermission = { permission },
            isPrivacyMuted = { muted },
        )
    }

    @Test
    fun `start begins a session and kicks the first capture`() = runTest {
        val c = buildController(backgroundScope)
        assertTrue(c.start("test"))
        assertTrue(c.active.value)
        assertEquals(1, captureStarted)
        c.stop(ListeningModePolicy.StopReason.USER_STOP, "test")
        assertFalse(c.active.value)
        assertEquals(1, captureStopped)
    }

    @Test
    fun `resume happens only after the post-speech guard delay`() = runTest {
        val c = buildController(backgroundScope)
        assertTrue(c.start("test"))
        assertEquals(1, captureStarted)
        // A turn runs: state leaves IDLE and comes back after the response.
        consoleState = ConsoleState.SPEAKING
        advanceTimeBy(100)
        consoleState = ConsoleState.IDLE
        // §15 step 9: the 750 ms guard must elapse BEFORE capture resumes.
        advanceTimeBy(200)
        assertEquals("capture resumed before the guard elapsed", 1, captureStarted)
        advanceTimeBy(1000)
        assertEquals("capture did not resume after the guard", 2, captureStarted)
    }

    @Test
    fun `privacy mute stop condition is typed`() = runTest {
        val c = buildController(backgroundScope)
        assertTrue(c.start("test"))
        muted = true
        c.onPrivacyMuteEngaged()
        assertFalse(c.active.value)
        assertTrue(events.recent(20).any { it.detail.contains("PRIVACY_MUTE") })
    }

    @Test
    fun `background stop is the foreground-only contract`() = runTest {
        val c = buildController(backgroundScope)
        assertTrue(c.start("test"))
        c.onConsolePaused()
        assertFalse(c.active.value)
        assertTrue(events.recent(20).any { it.detail.contains("BACKGROUND") })
    }

    @Test
    fun `session timeout stops within the configured limit`() = runTest {
        val c = buildController(backgroundScope)
        c.policy.setSessionLimit(5, "test") // 5 minutes
        assertTrue(c.start("test"))
        consoleState = ConsoleState.UNDERSTANDING_LOCAL
        advanceTimeBy(300) // the loop observes the activity
        // Simulate 6 minutes of session time.
        c.policy.sessionStarted(System.currentTimeMillis() - 6 * 60_000L)
        consoleState = ConsoleState.IDLE
        advanceTimeBy(2000)
        assertFalse(c.active.value)
        assertTrue(events.recent(20).any { it.detail.contains("SESSION_TIMEOUT") })
    }

    @Test
    fun `audio failure on resume stops the session`() = runTest {
        val c = buildController(backgroundScope)
        assertTrue(c.start("test"))
        consoleState = ConsoleState.SPEAKING
        advanceTimeBy(100)
        consoleState = ConsoleState.IDLE
        startResult = false // the recognizer refuses the next start
        advanceTimeBy(1200)
        assertFalse(c.active.value)
        assertTrue(events.recent(20).any { it.detail.contains("AUDIO_FAILURE") })
    }

    /**
     * GRAPH 4.11 Node 2 (Owner: "10 turni continui"): ten full turn cycles —
     * capture → activity → response → IDLE → 750 ms post-speech guard →
     * resume — keep the session ALIVE with exactly one capture start per turn
     * and NO stop condition firing (no timeout, no mute, no audio failure).
     */
    @Test
    fun `ten consecutive turns keep the session alive without any stop condition`() = runTest {
        val c = buildController(backgroundScope)
        assertTrue(c.start("graph411-node2"))
        assertEquals(1, captureStarted)
        repeat(10) { turn ->
            consoleState = ConsoleState.UNDERSTANDING_LOCAL
            advanceTimeBy(250) // the supervision loop observes the activity
            consoleState = ConsoleState.SPEAKING
            advanceTimeBy(250) // the response is being spoken
            assertTrue("turn ${turn + 1}: the session must stay active while speaking", c.active.value)
            consoleState = ConsoleState.IDLE
            advanceTimeBy(300) // inside the 750 ms post-speech guard
            assertEquals("turn ${turn + 1}: capture must NOT resume before the guard elapses", 1 + turn, captureStarted)
            advanceTimeBy(1000) // the guard elapses → capture resumes
            assertEquals("turn ${turn + 1}: capture must resume after the guard", 2 + turn, captureStarted)
        }
        assertTrue("ten turns must complete without the session ending", c.active.value)
        assertFalse(
            "no stop condition may fire across the ten turns",
            events.recent(50).any { it.detail.contains("session stop reason=") },
        )
        c.stop(ListeningModePolicy.StopReason.USER_STOP, "graph411-node2-complete")
        assertFalse(c.active.value)
    }
}
