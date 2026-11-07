package com.hermestower.voiceconsole.interrupt

import com.hermestower.voiceconsole.core.state.ConsoleState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * GRAPH 4.12 §13/§14/§15 — UNIFIED INTERRUPT pins.
 *
 * The Owner's "Interrompi appears non-functional" report (PHYS-FAIL-04) is
 * answered by: every applicable subsystem is cancelled, the receipt shows the
 * per-subsystem outcome, the final state is IDLE (or the policy-resumed
 * continuous state), and repeated taps are idempotent.
 */
class UnifiedInterruptTest {

    private class World {
        var micActive = false
        var sttActive = false
        var turnActive = false
        var bridgeActive = false
        var ttsActive = false
        var listeningActive = false
        var echoCleared = 0
        var resumeCleared = 0
        var state = ConsoleState.IDLE
        var resumePolicy = false
    }

    private fun controller(w: World, clock: () -> Long = { 0L }) = UnifiedInterruptController(
        newTurnId = { "t-fixed" },
        clock = clock,
        stopMicrophone = { val was = w.micActive; w.micActive = false; was },
        cancelStt = { val was = w.sttActive; w.sttActive = false; was },
        cancelLocalTurn = { val was = w.turnActive; w.turnActive = false; was },
        cancelBridgeRequest = { val was = w.bridgeActive; w.bridgeActive = false; was },
        stopTts = {
            val was = w.ttsActive
            w.ttsActive = false
            if (was) w.state = ConsoleState.IDLE // mirrors driver.stopSpeaking()'s transition
            was
        },
        stopListeningMode = { val was = w.listeningActive; w.listeningActive = false; was },
        clearEchoGuard = { w.echoCleared++ },
        clearPostTtsResumeTimer = { w.resumeCleared++ },
        currentConsoleState = { w.state },
        continuousResumesByPolicy = { w.resumePolicy },
    )

    @Test
    fun `all active subsystems cancel and receipt reports each outcome`() {
        val w = World().apply {
            micActive = true; sttActive = true; turnActive = true
            bridgeActive = true; ttsActive = true; listeningActive = true
            state = ConsoleState.SPEAKING
        }
        val r = controller(w).cancelAllActive("test")
        assertEquals(InterruptReceipt.SubsystemOutcome.CANCELLED, r.microphoneStopResult)
        assertEquals(InterruptReceipt.SubsystemOutcome.CANCELLED, r.sttCancelResult)
        assertEquals(InterruptReceipt.SubsystemOutcome.CANCELLED, r.localTurnCancelResult)
        assertEquals(InterruptReceipt.SubsystemOutcome.CANCELLED, r.bridgeCancelResult)
        assertEquals(InterruptReceipt.SubsystemOutcome.CANCELLED, r.ttsStopResult)
        assertEquals(InterruptReceipt.SubsystemOutcome.CANCELLED, r.listeningModeResult)
        assertTrue(r.isSuccess(resumedContinuous = false))
        assertFalse(r.hasRefusal())
        assertEquals(1, w.echoCleared)
        assertEquals(1, w.resumeCleared)
    }

    @Test
    fun `idle world reports WAS_INACTIVE everywhere and stays IDLE`() {
        val w = World()
        val r = controller(w).cancelAllActive("idle tap")
        assertEquals(InterruptReceipt.SubsystemOutcome.WAS_INACTIVE, r.microphoneStopResult)
        assertEquals(InterruptReceipt.SubsystemOutcome.WAS_INACTIVE, r.ttsStopResult)
        assertTrue(r.isSuccess(resumedContinuous = false))
    }

    @Test
    fun `repeated taps are idempotent (no REFUSED, no stuck state)`() {
        val w = World().apply { ttsActive = true; state = ConsoleState.SPEAKING }
        val c = controller(w)
        val r1 = c.cancelAllActive("tap1")
        val r2 = c.cancelAllActive("tap2")
        assertEquals(InterruptReceipt.SubsystemOutcome.CANCELLED, r1.ttsStopResult)
        assertEquals(InterruptReceipt.SubsystemOutcome.WAS_INACTIVE, r2.ttsStopResult)
        assertFalse(r2.hasRefusal())
        assertEquals(c.lastReceipt(), r2)
    }

    @Test
    fun `policy-resumed continuous listening yields CONTINUOUS_LISTENING_ACTIVE`() {
        val w = World().apply { resumePolicy = true }
        val r = controller(w).cancelAllActive("continuous stop")
        assertTrue(r.isSuccess(resumedContinuous = true))
        assertEquals("CONTINUOUS_LISTENING_ACTIVE", r.finalConsoleState)
    }

    @Test
    fun `§15 resolver matrix — one state-specific label, idle disabled`() {
        // continuous wins
        assertEquals(
            UnifiedInterruptController.LABEL_CONTINUOUS,
            UnifiedInterruptController.resolve(ConsoleState.IDLE, micListening = false, continuousActive = true).labelId,
        )
        assertEquals(
            UnifiedInterruptController.LABEL_LISTENING,
            UnifiedInterruptController.resolve(ConsoleState.LISTENING, micListening = true, continuousActive = false).labelId,
        )
        assertEquals(
            UnifiedInterruptController.LABEL_TRANSCRIBING,
            UnifiedInterruptController.resolve(ConsoleState.TRANSCRIBING, micListening = false, continuousActive = false).labelId,
        )
        assertEquals(
            UnifiedInterruptController.LABEL_PROCESSING,
            UnifiedInterruptController.resolve(ConsoleState.CONTACTING_TOWER, micListening = false, continuousActive = false).labelId,
        )
        assertEquals(
            UnifiedInterruptController.LABEL_PROCESSING,
            UnifiedInterruptController.resolve(ConsoleState.TOWER_PROCESSING, micListening = false, continuousActive = false).labelId,
        )
        assertEquals(
            UnifiedInterruptController.LABEL_SPEAKING,
            UnifiedInterruptController.resolve(ConsoleState.SPEAKING, micListening = false, continuousActive = false).labelId,
        )
        // idle: nothing cancellable, clearly disabled
        val idle = UnifiedInterruptController.resolve(ConsoleState.IDLE, micListening = false, continuousActive = false)
        assertFalse(idle.enabled)
        assertFalse(idle.cancellable)
    }
}
