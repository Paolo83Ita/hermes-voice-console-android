package com.hermestower.voiceconsole.bridge

import com.hermestower.voiceconsole.core.state.ConnectionStatus
import com.hermestower.voiceconsole.domain.BridgeResult
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MockTowerBridgeTest {

    private fun newBridge(testDispatcher: TestDispatcher) =
        MockTowerBridge(DeterministicTowerSimulator(), testDispatcher)

    private fun frame(transcript: String = "prepare tomorrow mission plan") = TowerRequestFrame(
        deviceId = "console-test",
        userIdRef = "owner-test",
        sessionId = "sess-test",
        conversationId = "conv-test",
        requestId = "req-000001",
        transcript = transcript,
        confidence = 0.9f,
        requestedMode = "HYBRID",
        effectiveMode = "HYBRID",
        timestamp = "2026-10-30T12:00:00Z",
    )

    @Test
    fun `handshake succeeds and marks bridge connected`() = runTest {
        val bridge = newBridge(UnconfinedTestDispatcher(testScheduler))
        bridge.configure(com.hermestower.voiceconsole.domain.TowerEndpoint.MOCK)
        val result = bridge.handshake()
        assertTrue(result is BridgeResult.Success)
        assertEquals(ConnectionStatus.BridgeStatus.CONNECTED, bridge.connection.value.status)
        assertTrue(bridge.connection.value.lastHandshakeAt != null)
    }

    @Test
    fun `request emits ack, progress, deltas and a final receipt`() = runTest {
        val bridge = newBridge(UnconfinedTestDispatcher(testScheduler))
        val events = bridge.sendRequest(frame()).toList()
        assertEquals("ACK", events.first().type)
        assertEquals("FINAL", events.last().type)
        assertTrue(events.any { it.type == "PROGRESS" })
        assertTrue(events.count { it.type == "DELTA" } >= 2)
        val final = events.last()
        assertTrue(final.receiptRef!!.startsWith("rcp-mock-"))
        assertTrue(final.finalText!!.contains("mission"))
    }

    @Test
    fun `disconnect fault produces a retryable transport error after ack`() = runTest {
        val bridge = newBridge(UnconfinedTestDispatcher(testScheduler))
        bridge.setFault(BridgeFault.DISCONNECT_AT_ACK)
        val events = bridge.sendRequest(frame()).toList()
        assertEquals(ConnectionStatus.BridgeStatus.DISCONNECTED, bridge.connection.value.status)
        val error = events.last()
        assertEquals("ERROR", error.type)
        assertEquals("TRANSPORT", error.error?.classification)
        assertTrue(error.error?.retryable == true)
        bridge.setFault(BridgeFault.NONE)
    }

    @Test
    fun `tower error fault produces a non-retryable error after progress`() = runTest {
        val bridge = newBridge(UnconfinedTestDispatcher(testScheduler))
        bridge.setFault(BridgeFault.ERROR_AFTER_PROGRESS)
        val events = bridge.sendRequest(frame()).toList()
        assertEquals("ERROR", events.last().type)
        assertEquals("TOWER_INTERNAL", events.last().error?.classification)
        assertTrue(events.last().error?.retryable == false)
        bridge.setFault(BridgeFault.NONE)
    }

    @Test
    fun `simulator responses are deterministic`() {
        val simulator = DeterministicTowerSimulator()
        val a = simulator.respond("mission status")
        val b = simulator.respond("mission status")
        assertEquals(a, b)
        assertTrue(simulator.respond("hello").contains("simulated"))
    }
}
