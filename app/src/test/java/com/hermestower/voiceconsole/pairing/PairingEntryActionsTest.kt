package com.hermestower.voiceconsole.pairing

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * A4R NODE A3 — pairing entry lanes and typed states (pure JVM: the executor
 * runs against the deterministic [MockTowerPairingClient]; no Android classes,
 * no camera, no network — the same A4R-P1-002 discipline).
 */
class PairingEntryActionsTest {

    // ---- the six mission typed states exist, exactly ---------------------------------

    @Test
    fun `the six mission typed states are exactly the declared set`() {
        val expected = setOf(
            "CAMERA_PERMISSION_DENIED", "CAMERA_UNAVAILABLE", "QR_NOT_RECOGNIZED",
            "QR_EXPIRED", "QR_SCHEMA_UNSUPPORTED", "PAIRING_ENDPOINT_UNREACHABLE",
        )
        assertEquals(expected, PairingEntryState.entries.map { it.name }.toSet())
    }

    // ---- rejection mapping matrix (11 parser kinds -> 6 mission states) --------------

    @Test
    fun `malformed families map to QR_NOT_RECOGNIZED`() {
        assertEquals(PairingEntryState.QR_NOT_RECOGNIZED, PairingRejectKind.MALFORMED.toPairingEntryState())
        assertEquals(PairingEntryState.QR_NOT_RECOGNIZED, PairingRejectKind.MISSING_NONCE.toPairingEntryState())
        assertEquals(PairingEntryState.QR_NOT_RECOGNIZED, PairingRejectKind.MISSING_CHALLENGE.toPairingEntryState())
        assertEquals(PairingEntryState.QR_NOT_RECOGNIZED, PairingRejectKind.ALTERED_PAYLOAD.toPairingEntryState())
        assertEquals(PairingEntryState.QR_NOT_RECOGNIZED, PairingRejectKind.FUTURE_ISSUED.toPairingEntryState())
    }

    @Test
    fun `expiry families map to QR_EXPIRED`() {
        assertEquals(PairingEntryState.QR_EXPIRED, PairingRejectKind.EXPIRED.toPairingEntryState())
        assertEquals(PairingEntryState.QR_EXPIRED, PairingRejectKind.REPLAYED_NONCE.toPairingEntryState())
    }

    @Test
    fun `schema families map to QR_SCHEMA_UNSUPPORTED`() {
        assertEquals(PairingEntryState.QR_SCHEMA_UNSUPPORTED, PairingRejectKind.UNSUPPORTED_SCHEME.toPairingEntryState())
        assertEquals(PairingEntryState.QR_SCHEMA_UNSUPPORTED, PairingRejectKind.UNSUPPORTED_PROTOCOL.toPairingEntryState())
        assertEquals(PairingEntryState.QR_SCHEMA_UNSUPPORTED, PairingRejectKind.WRONG_ENVIRONMENT.toPairingEntryState())
    }

    @Test
    fun `bad endpoint maps to PAIRING_ENDPOINT_UNREACHABLE`() {
        assertEquals(PairingEntryState.PAIRING_ENDPOINT_UNREACHABLE, PairingRejectKind.BAD_ENDPOINT.toPairingEntryState())
    }

    // ---- numeric code truth ----------------------------------------------------------

    @Test
    fun `numeric code accepts exactly 8 digits with grouping separators`() {
        assertEquals("12345678", NumericPairingCode.normalize("12345678"))
        assertEquals("12345678", NumericPairingCode.normalize("1234-5678"))
        assertEquals("12345678", NumericPairingCode.normalize("1234 5678"))
        assertTrue(NumericPairingCode.isValid("1234 5678"))
    }

    @Test
    fun `numeric code rejects letters short codes and stray separators`() {
        assertNull(NumericPairingCode.normalize("1234A678"))
        assertNull(NumericPairingCode.normalize("1234567"))
        assertNull(NumericPairingCode.normalize("123456789"))
        assertNull(NumericPairingCode.normalize("1234.5678"))
        assertFalse(NumericPairingCode.isValid(""))
    }

    // ---- manual endpoint truth -------------------------------------------------------

    @Test
    fun `manual endpoint accepts https hosts only`() {
        assertTrue(ManualEndpoint.validate("https://hermestower.aclclouds.online").isSuccess)
        assertTrue(ManualEndpoint.validate("  https://tower.lan:8443/api  ").isSuccess)
        assertTrue(ManualEndpoint.validate("http://tower.lan").isFailure) // no plaintext endpoints
        assertTrue(ManualEndpoint.validate("https://nohost").isFailure)
        assertTrue(ManualEndpoint.validate("https://tower host").isFailure)
        assertTrue(ManualEndpoint.validate("").isFailure)
    }

    // ---- executor lanes (deterministic SIMULATED client) ------------------------------

    @Test
    fun `numeric lane accepts a valid code through the deterministic client`() = runTest {
        val executor = PairingEntryExecutor(MockTowerPairingClient(clockMs = { 1_000L }))
        val outcome = executor.pairNumeric("1234-5678", "https://hermestower.aclclouds.online")
        assertTrue(outcome is PairingEntryOutcome.Accepted)
        val accepted = outcome as PairingEntryOutcome.Accepted
        assertEquals(PairingEntryOutcome.Lane.NUMERIC, accepted.lane)
        assertTrue(accepted.sessionId.startsWith("pair-mock-"))
        assertTrue(accepted.matchPhrase.isNotEmpty())
    }

    @Test
    fun `numeric lane rejects a bad code with a typed state`() = runTest {
        val executor = PairingEntryExecutor(MockTowerPairingClient())
        val outcome = executor.pairNumeric("12A45678", "https://hermestower.aclclouds.online")
        assertEquals(PairingEntryOutcome.Rejected(PairingEntryState.QR_NOT_RECOGNIZED, "numeric code must be exactly 8 digits"), outcome)
    }

    @Test
    fun `manual lane rejects a bad endpoint with a typed state`() = runTest {
        val executor = PairingEntryExecutor(MockTowerPairingClient())
        val outcome = executor.pairManualEndpoint("http://insecure.example")
        assertEquals(PairingEntryOutcome.Rejected(PairingEntryState.PAIRING_ENDPOINT_UNREACHABLE, "endpoint shape rejected (https://host required)"), outcome)
    }

    @Test
    fun `both lanes share one client implementation - no duplicated pairing`() = runTest {
        // The mission merge rule: one executor, one client interface, two lanes.
        val client: TowerPairingClient = MockTowerPairingClient()
        val executor = PairingEntryExecutor(client)
        val numeric = executor.pairNumeric("87654321", "https://tower.example")
        val manual = executor.pairManualEndpoint("https://tower.example")
        assertTrue(numeric is PairingEntryOutcome.Accepted)
        assertTrue(manual is PairingEntryOutcome.Accepted)
        assertTrue((numeric as PairingEntryOutcome.Accepted).sessionId != (manual as PairingEntryOutcome.Accepted).sessionId)
    }
}
