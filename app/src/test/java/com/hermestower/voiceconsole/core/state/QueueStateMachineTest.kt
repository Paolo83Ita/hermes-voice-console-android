package com.hermestower.voiceconsole.core.state

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * GRAPH 4 CP-G4-007 — state-machine edges for the offline queue lifecycle.
 * Run 41527072357 diagnostics proved the flush-starvation root cause: the
 * queue branch entered REQUEST_QUEUED while TRANSCRIBING (illegal → silently
 * ignored → state stayed pipeline-active → the reconnect flush skipped every
 * delivery). These tests pin the repaired contract.
 */
class QueueStateMachineTest {

    @Test
    fun `offline queue entry is legal from TRANSCRIBING`() {
        val store = ConsoleStateStore()
        store.transition(ConsoleState.IDLE)
        store.transition(ConsoleState.TRANSCRIBING)
        assertEquals(ConsoleState.REQUEST_QUEUED, store.transition(ConsoleState.REQUEST_QUEUED))
        assertEquals(ConsoleState.REQUEST_QUEUED, store.currentState())
        // the flush path continues from the queue state
        assertEquals(ConsoleState.CONTACTING_TOWER, store.transition(ConsoleState.CONTACTING_TOWER))
    }

    @Test
    fun `reconnect flush entry is legal from IDLE`() {
        val store = ConsoleStateStore()
        store.transition(ConsoleState.IDLE)
        assertEquals(ConsoleState.REQUEST_QUEUED, store.transition(ConsoleState.REQUEST_QUEUED))
        assertEquals(ConsoleState.CONTACTING_TOWER, store.transition(ConsoleState.CONTACTING_TOWER))
        assertEquals(ConsoleState.TOWER_PROCESSING, store.transition(ConsoleState.TOWER_PROCESSING))
        assertEquals(ConsoleState.IDLE, store.transition(ConsoleState.IDLE))
    }

    @Test
    fun `illegal queue transitions are still rejected`() {
        val store = ConsoleStateStore()
        store.transition(ConsoleState.IDLE)
        store.transition(ConsoleState.LISTENING)
        assertNull("LISTENING must not enter the queue state directly", store.transition(ConsoleState.REQUEST_QUEUED))
        assertEquals(ConsoleState.LISTENING, store.currentState())
    }

    @Test
    fun `full offline queue lifecycle stays truthful`() {
        val store = ConsoleStateStore()
        store.transition(ConsoleState.IDLE)
        store.transition(ConsoleState.DISCONNECTED)
        store.transition(ConsoleState.IDLE) // submitText returns to rest first
        store.transition(ConsoleState.TRANSCRIBING)
        store.transition(ConsoleState.REQUEST_QUEUED) // queue branch (now legal)
        store.transition(ConsoleState.IDLE) // display delay ends
        // reconnect flush
        store.transition(ConsoleState.REQUEST_QUEUED)
        store.transition(ConsoleState.CONTACTING_TOWER)
        store.transition(ConsoleState.TOWER_PROCESSING)
        assertNotNull(store.transition(ConsoleState.IDLE))
        assertEquals(ConsoleState.IDLE, store.currentState())
    }
}
