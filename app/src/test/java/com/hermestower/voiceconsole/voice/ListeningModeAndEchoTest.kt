package com.hermestower.voiceconsole.voice

import com.hermestower.voiceconsole.migration.InMemoryKeyValueStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * GRAPH 4.10 CP-G4.10-005/006: the listening-mode authority (§14), the echo
 * fingerprint guard (§16) and the continuous loop controller (§15).
 */
class ListeningModePolicyTest {

    @Test
    fun `default mode is PUSH_TO_TALK with the §15 defaults`() {
        val p = ListeningModePolicy(InMemoryKeyValueStore())
        assertEquals(ListeningModePolicy.Mode.PUSH_TO_TALK, p.state.value.mode)
        assertEquals(750L, p.state.value.resumeDelayMs)
        assertEquals(15, p.state.value.sessionLimitMinutes)
        assertEquals(ListeningModePolicy.BargeIn.BARGE_IN_OFF, p.state.value.bargeIn)
    }

    @Test
    fun `mode persists across restart`() {
        val store = InMemoryKeyValueStore()
        ListeningModePolicy(store).setMode(ListeningModePolicy.Mode.CONTINUOUS_WHILE_SCREEN_ACTIVE, "test")
        assertEquals(ListeningModePolicy.Mode.CONTINUOUS_WHILE_SCREEN_ACTIVE, ListeningModePolicy(store).state.value.mode)
    }

    @Test
    fun `wake word is honestly unqualified`() {
        val p = ListeningModePolicy(InMemoryKeyValueStore())
        p.setMode(ListeningModePolicy.Mode.WAKE_WORD, "test")
        assertFalse(p.isWakeWordQualified())
        assertTrue(p.state.value.wakeWordBlocked)
    }

    @Test
    fun `user stop ends the session and records the reason`() {
        val p = ListeningModePolicy(InMemoryKeyValueStore())
        p.setMode(ListeningModePolicy.Mode.CONTINUOUS_WHILE_SCREEN_ACTIVE, "test")
        p.sessionStarted(1000)
        assertTrue(p.state.value.continuousActive)
        p.sessionStopped(ListeningModePolicy.StopReason.USER_STOP, "test")
        assertFalse(p.state.value.continuousActive)
        assertNull(p.state.value.sessionStartElapsedMs)
        assertEquals(ListeningModePolicy.StopReason.USER_STOP, p.state.value.lastStopReason)
        assertEquals(ListeningModePolicy.Mode.PUSH_TO_TALK, p.state.value.mode)
    }

    @Test
    fun `privacy-mute stop does not change the mode away from continuous`() {
        val p = ListeningModePolicy(InMemoryKeyValueStore())
        p.setMode(ListeningModePolicy.Mode.CONTINUOUS_WHILE_SCREEN_ACTIVE, "test")
        p.sessionStopped(ListeningModePolicy.StopReason.PRIVACY_MUTE, "test")
        assertEquals(ListeningModePolicy.Mode.CONTINUOUS_WHILE_SCREEN_ACTIVE, p.state.value.mode)
        assertEquals(ListeningModePolicy.StopReason.PRIVACY_MUTE, p.state.value.lastStopReason)
    }
}

class EchoFingerprintGuardTest {

    private var now = 100_000L
    private fun guard() = EchoFingerprintGuard(clockMs = { now })

    @Test
    fun `verbatim spoken response is rejected as echo inside the window`() {
        val g = guard()
        g.rememberSpoken("Buon giorno. La console vocale di Hermes è pronta.")
        assertTrue(g.isLikelyEcho("Buon giorno. La console vocale di Hermes è pronta."))
        assertTrue(g.isLikelyEcho("buon giorno la console vocale di hermes e pronta")) // normalized
    }

    @Test
    fun `outside the time window the same text is NOT an echo`() {
        val g = guard()
        g.rememberSpoken("Buon giorno.")
        now += 20_000L // beyond DEFAULT_WINDOW_MS
        assertFalse(g.isLikelyEcho("Buon giorno."))
    }

    @Test
    fun `a genuine new user phrase is never an echo`() {
        val g = guard()
        g.rememberSpoken("Buon giorno. La console vocale di Hermes è pronta.")
        assertFalse(g.isLikelyEcho("che ore sono?"))
        assertFalse(g.isLikelyEcho("prepara il piano di missione per domani"))
    }

    @Test
    fun `partial overlap below the threshold is not an echo`() {
        val g = guard()
        g.rememberSpoken("il canale vocale è pronto")
        // One shared token out of many — low Jaccard.
        assertFalse(g.isLikelyEcho("pronto a partire per la missione"))
    }

    @Test
    fun `clear removes all fingerprints`() {
        val g = guard()
        g.rememberSpoken("Buon giorno.")
        g.clear()
        assertFalse(g.isLikelyEcho("Buon giorno."))
    }
}
