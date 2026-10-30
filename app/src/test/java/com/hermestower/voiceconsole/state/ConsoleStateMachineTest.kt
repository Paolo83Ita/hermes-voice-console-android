package com.hermestower.voiceconsole.state

import com.hermestower.voiceconsole.core.state.ConsoleState
import com.hermestower.voiceconsole.core.state.ConsoleStateMachine
import com.hermestower.voiceconsole.core.state.ConsoleStateStore
import com.hermestower.voiceconsole.core.state.IllegalConsoleTransitionException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ConsoleStateMachineTest {

    @Test
    fun `every state has outgoing edges and can reach IDLE or DORMANT`() {
        ConsoleState.entries.forEach { state ->
            assertTrue(
                "state $state has no outgoing transitions",
                ConsoleStateMachine.outgoing(state).isNotEmpty() ||
                    ConsoleStateMachine.canTransition(state, ConsoleState.IDLE),
            )
            assertTrue(
                "state $state cannot reach IDLE (universal rule 3)",
                ConsoleStateMachine.canTransition(state, ConsoleState.IDLE) || state == ConsoleState.DORMANT,
            )
            assertTrue("power off must always be legal from $state", ConsoleStateMachine.canTransition(state, ConsoleState.DORMANT))
        }
    }

    @Test
    fun `privacy mute is reachable from every state except DORMANT`() {
        ConsoleState.entries.forEach { state ->
            assertEquals(
                "mute legality from $state",
                state != ConsoleState.DORMANT,
                ConsoleStateMachine.canTransition(state, ConsoleState.PRIVACY_MUTED),
            )
        }
    }

    @Test
    fun `canonical voice loop paths are legal`() {
        val loop = listOf(
            ConsoleState.IDLE, ConsoleState.WAKE_DETECTED, ConsoleState.LISTENING,
            ConsoleState.TRANSCRIBING, ConsoleState.UNDERSTANDING_LOCAL, ConsoleState.SPEAKING,
            ConsoleState.IDLE,
        )
        assertPathLegal(loop)

        val towerLoop = listOf(
            ConsoleState.IDLE, ConsoleState.WAKE_DETECTED, ConsoleState.LISTENING,
            ConsoleState.TRANSCRIBING, ConsoleState.CONTACTING_TOWER, ConsoleState.TOWER_PROCESSING,
            ConsoleState.SPEAKING, ConsoleState.IDLE,
        )
        assertPathLegal(towerLoop)

        val queueLoop = listOf(
            ConsoleState.IDLE, ConsoleState.CONTACTING_TOWER, ConsoleState.REQUEST_QUEUED,
            ConsoleState.IDLE, ConsoleState.CONTACTING_TOWER,
            ConsoleState.TOWER_PROCESSING, ConsoleState.SPEAKING, ConsoleState.IDLE,
        )
        assertPathLegal(queueLoop)
    }

    @Test
    fun `disconnect and recovery paths are legal`() {
        assertPathLegal(listOf(ConsoleState.IDLE, ConsoleState.DISCONNECTED, ConsoleState.IDLE))
        assertPathLegal(listOf(ConsoleState.IDLE, ConsoleState.DISCONNECTED, ConsoleState.DEGRADED, ConsoleState.IDLE))
        assertPathLegal(listOf(ConsoleState.IDLE, ConsoleState.ERROR, ConsoleState.IDLE))
        assertPathLegal(listOf(ConsoleState.IDLE, ConsoleState.UPDATE_AVAILABLE, ConsoleState.IDLE))
        assertPathLegal(listOf(ConsoleState.PRIVACY_MUTED, ConsoleState.IDLE, ConsoleState.DORMANT, ConsoleState.IDLE))
    }

    @Test
    fun `illegal transitions are rejected with exception`() {
        assertFalse(ConsoleStateMachine.canTransition(ConsoleState.IDLE, ConsoleState.SPEAKING))
        assertFalse(ConsoleStateMachine.canTransition(ConsoleState.LISTENING, ConsoleState.TOWER_PROCESSING))
        assertFalse(ConsoleStateMachine.canTransition(ConsoleState.DORMANT, ConsoleState.SPEAKING))
        assertFalse(ConsoleStateMachine.canTransition(ConsoleState.PRIVACY_MUTED, ConsoleState.LISTENING))
        try {
            ConsoleStateMachine.ensure(ConsoleState.IDLE, ConsoleState.SPEAKING)
            throw AssertionError("expected IllegalConsoleTransitionException")
        } catch (e: IllegalConsoleTransitionException) {
            assertNotNull(e.message)
        }
    }

    @Test
    fun `store enforces transitions and reports rejection`() {
        val store = ConsoleStateStore()
        assertEquals(ConsoleState.DORMANT, store.currentState())
        assertNull(store.transition(ConsoleState.SPEAKING))
        assertEquals(ConsoleState.DORMANT, store.currentState())
        assertNotNull(store.transition(ConsoleState.IDLE))
        assertEquals(ConsoleState.IDLE, store.currentState())
        assertNull(store.transition(ConsoleState.SPEAKING))
        assertNotNull(store.transition(ConsoleState.TRANSCRIBING))
        assertEquals(ConsoleState.TRANSCRIBING, store.currentState())
    }

    private fun assertPathLegal(path: List<ConsoleState>) {
        path.zipWithNext().forEach { (from, to) ->
            assertTrue("illegal edge in path: $from -> $to", ConsoleStateMachine.canTransition(from, to))
        }
    }
}
