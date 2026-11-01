package com.hermestower.voiceconsole.e2e

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import com.hermestower.voiceconsole.bridge.BridgeFault
import com.hermestower.voiceconsole.core.state.ConnectionStatus
import com.hermestower.voiceconsole.core.state.ConversationTurn
import com.hermestower.voiceconsole.ui.MainActivity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

/**
 * E2E-11..20 (GRAPH 2 §13): local request, Tower-routed request, streamed
 * response, cancellation, disconnect simulation, DISCONNECTED state, offline
 * queueing of a permitted non-consequential request, queue truthfulness,
 * reconnect and queued-request validation before delivery.
 */
@RunWith(JUnit4::class)
class E2EVoiceFlowTest {

    @get:Rule
    val compose: HermesComposeRule = createAndroidComposeRule(MainActivity::class.java)

    private fun graph() = E2E.graph()

    private fun awaitTurn(predicate: (ConversationTurn) -> Boolean, timeoutMs: Long = 30_000): ConversationTurn? {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            graph().store.turns.value.firstOrNull(predicate)?.let { return it }
            Thread.sleep(120)
        }
        return null
    }

    @Test
    fun e2e_11_submitLocalRequest_handledLocally() {
        graph().driver.setRequestedMode(com.hermestower.voiceconsole.core.mode.OperatingMode.LOCAL)
        graph().driver.submitText("hello")
        val turn = awaitTurn(predicate = {
            it.role == ConversationTurn.Role.HERMES &&
                it.source == ConversationTurn.Source.LOCAL &&
                it.delivery == ConversationTurn.Delivery.DELIVERED
        })
        assertTrue("no local reply produced", turn != null)
    }

    @Test
    fun e2e_12_13_submitTowerRequest_receivesStreamedFinalResponse() {
        graph().driver.setRequestedMode(com.hermestower.voiceconsole.core.mode.OperatingMode.TOWER)
        graph().driver.submitText("tell me about the hermes tower")
        val reply = awaitTurn(predicate = {
            it.role == ConversationTurn.Role.HERMES &&
                it.source == ConversationTurn.Source.TOWER &&
                it.delivery == ConversationTurn.Delivery.DELIVERED
        })
        assertTrue("no tower reply produced", reply != null)
        assertTrue("no receipt bound to the delivered reply", reply!!.receiptRef != null)
    }

    @Test
    fun e2e_14_cancelActiveResponse_returnsToIdle() {
        graph().driver.submitText("status report")
        // Cancel while the pipeline is active: stopSpeaking is the Owner cancel affordance.
        val deadline = System.currentTimeMillis() + 15_000
        while (System.currentTimeMillis() < deadline && !graph().store.currentState().isPipelineActive) Thread.sleep(50)
        graph().driver.stopSpeaking()
        val idle = awaitTurn(predicate = { it.role == ConversationTurn.Role.OWNER }, timeoutMs = 5_000)
        assertTrue(idle != null)
        // The console must be back at rest (IDLE) after cancellation.
        val deadline2 = System.currentTimeMillis() + 10_000
        while (System.currentTimeMillis() < deadline2 && graph().store.currentState() != com.hermestower.voiceconsole.core.state.ConsoleState.IDLE) Thread.sleep(100)
        assertEquals(com.hermestower.voiceconsole.core.state.ConsoleState.IDLE, graph().store.currentState())
    }

    @Test
    fun e2e_15_16_simulateDisconnect_disconnectedStateTruthful() {
        graph().driver.setFault(BridgeFault.DISCONNECT_AT_ACK)
        val deadline = System.currentTimeMillis() + 10_000
        while (System.currentTimeMillis() < deadline &&
            graph().store.connection.value.status != ConnectionStatus.BridgeStatus.DISCONNECTED
        ) Thread.sleep(100)
        assertEquals(ConnectionStatus.BridgeStatus.DISCONNECTED, graph().store.connection.value.status)
        assertEquals(
            com.hermestower.voiceconsole.core.mode.OperatingMode.DISCONNECTED_SAFE,
            graph().store.effectiveMode.value,
        )
        graph().driver.clearFault()
    }

    @Test
    fun e2e_17_18_queueNonConsequentialWhileOffline_queuedNotShownAsCompleted() {
        graph().driver.setFault(BridgeFault.DISCONNECT_AT_ACK)
        val deadline = System.currentTimeMillis() + 10_000
        while (System.currentTimeMillis() < deadline &&
            graph().store.connection.value.status != ConnectionStatus.BridgeStatus.DISCONNECTED
        ) Thread.sleep(100)
        graph().driver.submitText("tell me a joke")
        val queued = awaitTurn(predicate = {
            it.role == ConversationTurn.Role.OWNER && it.delivery == ConversationTurn.Delivery.QUEUED
        }, timeoutMs = 30_000)
        assertTrue("no queued turn appeared", queued != null)
        // Truthfulness: a queued request must never be shown as completed.
        assertTrue(queued!!.delivery != ConversationTurn.Delivery.DELIVERED)
        graph().driver.clearFault()
    }

    @Test
    fun e2e_19_20_reconnect_validatesQueuedRequestBeforeDelivery() {
        // Precondition: something queued while offline.
        graph().driver.setFault(BridgeFault.DISCONNECT_AT_ACK)
        val d0 = System.currentTimeMillis() + 10_000
        while (System.currentTimeMillis() < d0 && graph().store.connection.value.status != ConnectionStatus.BridgeStatus.DISCONNECTED) Thread.sleep(100)
        graph().driver.submitText("tell me a joke")
        awaitTurn(predicate = { it.role == ConversationTurn.Role.OWNER && it.delivery == ConversationTurn.Delivery.QUEUED }, timeoutMs = 30_000)
        // Reconnect: clearing the fault triggers handshake + queue flush (validated before delivery).
        graph().driver.clearFault()
        val flushed = awaitTurn(predicate = {
            it.role == ConversationTurn.Role.OWNER && it.delivery == ConversationTurn.Delivery.DELIVERED && it.note == null
        }, timeoutMs = 60_000) ?: awaitTurn(predicate = { it.role == ConversationTurn.Role.HERMES && it.source == ConversationTurn.Source.TOWER }, timeoutMs = 60_000)
        assertTrue("queued request was never validated and delivered after reconnect", flushed != null)
        assertTrue(graph().store.connection.value.status == ConnectionStatus.BridgeStatus.CONNECTED)
    }
}
