package com.hermestower.voiceconsole.domain

import com.hermestower.voiceconsole.core.state.ConnectionStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * GRAPH 4.14 §10/§11 — the bridge truth table. FINDING-03 repair proof:
 * a mock-mode handshake can NEVER resolve to the cyan connected treatment.
 */
class BridgeModeAuthorityTest {

    private fun conn(
        status: ConnectionStatus.BridgeStatus,
        endpoint: String = "mock://tower.simulator",
        tls: ConnectionStatus.TlsStatus = ConnectionStatus.TlsStatus.MOCK_NOT_APPLICABLE,
    ) = ConnectionStatus(status = status, endpointLabel = endpoint, tls = tls)

    @Test
    fun `mock connected — SIMULAZIONE amber, never BRIDGE OK cyan`() {
        val t = BridgeModeAuthority.resolve(conn(ConnectionStatus.BridgeStatus.CONNECTED))
        assertEquals(BridgeMode.MOCK_BRIDGE_HEALTH, t.mode)
        assertEquals(BridgeIndicatorRole.MOCK, t.role)
        assertTrue(t.isMock)
        assertEquals("MOCK_SIMULATOR", t.endpointClass)
        assertNull("mock has no auth truth", t.authenticated)
        assertNull(t.sessionEstablished)
        assertFalse("mock is never capability-negotiated", t.capabilityNegotiated)
        assertEquals(BridgeModeAuthority.LIMITATION_MOCK, t.typedLimitation)
    }

    @Test
    fun `mock unconfigured and disconnected stay the amber simulation role`() {
        for (s in listOf(
            ConnectionStatus.BridgeStatus.UNCONFIGURED,
            ConnectionStatus.BridgeStatus.DISCONNECTED,
            ConnectionStatus.BridgeStatus.CONNECTING,
        )) {
            val t = BridgeModeAuthority.resolve(conn(s))
            assertEquals("mock mode must stay MOCK for status $s", BridgeIndicatorRole.MOCK, t.role)
            assertTrue(t.isMock)
        }
    }

    @Test
    fun `real tower authenticated — cyan TOWER CONNESSA with session`() {
        val t = BridgeModeAuthority.resolve(
            conn(ConnectionStatus.BridgeStatus.CONNECTED, endpoint = "https://tower.example", tls = ConnectionStatus.TlsStatus.ACTIVE),
        )
        assertEquals(BridgeMode.REAL_TOWER_SESSION, t.mode)
        assertEquals(BridgeIndicatorRole.CONNECTED, t.role)
        assertFalse(t.isMock)
        assertEquals(true, t.authenticated)
        assertEquals(true, t.sessionEstablished)
    }

    @Test
    fun `real tower reachable but unauthenticated — amber, not cyan`() {
        val t = BridgeModeAuthority.resolve(
            conn(ConnectionStatus.BridgeStatus.CONNECTED, endpoint = "https://tower.example", tls = ConnectionStatus.TlsStatus.UNKNOWN),
        )
        assertEquals(BridgeMode.REAL_TOWER_TRANSPORT, t.mode)
        assertEquals(BridgeIndicatorRole.REACHABLE, t.role)
        assertEquals(false, t.authenticated)
        assertEquals(BridgeModeAuthority.LIMITATION_UNAUTHENTICATED, t.typedLimitation)
    }

    @Test
    fun `real tower connecting — reachable amber`() {
        val t = BridgeModeAuthority.resolve(conn(ConnectionStatus.BridgeStatus.CONNECTING, endpoint = "https://tower.example"))
        assertEquals(BridgeIndicatorRole.REACHABLE, t.role)
        assertEquals(BridgeMode.REAL_TOWER_TRANSPORT, t.mode)
    }

    @Test
    fun `local bridge healthy, no real tower — LOCAL_READY`() {
        val t = BridgeModeAuthority.resolve(conn(ConnectionStatus.BridgeStatus.UNCONFIGURED, endpoint = "local://bridge"))
        assertEquals(BridgeMode.LOCAL_BRIDGE_HEALTH, t.mode)
        assertEquals(BridgeIndicatorRole.LOCAL_READY, t.role)
        assertFalse(t.isMock)
        assertEquals("LOCAL_BRIDGE", t.endpointClass)
        assertEquals(BridgeModeAuthority.LIMITATION_NO_REAL_TOWER, t.typedLimitation)
    }

    @Test
    fun `capability negotiation stays pending until a real negotiation passes`() {
        val t = BridgeModeAuthority.resolve(
            conn(ConnectionStatus.BridgeStatus.CONNECTED, endpoint = "https://tower.example", tls = ConnectionStatus.TlsStatus.ACTIVE),
        )
        assertFalse(t.capabilityNegotiated)
        assertEquals(BridgeModeAuthority.LIMITATION_NEGOTIATION_PENDING, t.typedLimitation)
    }

    @Test
    fun `last successful check flows through from the connection state`() {
        val now = java.time.Instant.parse("2026-11-09T10:15:30Z")
        val t = BridgeModeAuthority.resolve(
            ConnectionStatus(
                status = ConnectionStatus.BridgeStatus.CONNECTED,
                endpointLabel = "mock://tower.simulator",
                tls = ConnectionStatus.TlsStatus.MOCK_NOT_APPLICABLE,
                lastHandshakeAt = now,
            ),
        )
        assertEquals(now, t.lastSuccessfulCheck)
    }
}
