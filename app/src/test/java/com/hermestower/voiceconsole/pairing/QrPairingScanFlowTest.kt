package com.hermestower.voiceconsole.pairing

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * GRAPH 4 CP-G4-010 wave 2: QR scan-flow policy tests — the pure state machine
 * between a camera analyzer and the strict parser. Deterministic clock; no
 * camera, no Robolectric. Parser acceptance/rejection matrix itself is already
 * covered exhaustively by PairingPayloadParserTest; these tests pin the FLOW
 * policies: duplicate-frame suppression, terminal detection, non-terminal
 * rejections, truthful frame counter, reset semantics and the replay honesty.
 */
class QrPairingScanFlowTest {

    private val now = 1_700_000_000_000L

    /** Mutable virtual clock for debounce-window traversal (nested: no outer capture). */
    private class Clock(var t: Long) {
        fun ms(): Long = t
    }

    private val validPayload =
        "hermes-pair://v1/tower-alpha" +
            "?env=local&iat=${now - 60_000}&exp=${now + 300_000}" +
            "&n=NONCE12345678&c=CHALLENGE9&ep=https%3A%2F%2Ftower.local%3A8443%2Fpair&s=console,voice"

    private fun expiredPayload(nonce: String = "NONCE12345678") =
        "hermes-pair://v1/tower-alpha" +
            "?env=local&iat=${now - 700_000}&exp=${now - 100_000}" +
            "&n=$nonce&c=CHALLENGE9&ep=https%3A%2F%2Ftower.local%3A8443%2Fpair"

    @Test
    fun `first valid scan reaches Detected with redaction-safe summary`() {
        val clock = Clock(now)
        val flow = QrPairingScanFlow(PairingPayloadParser(InMemoryPairingNonceStore()) { clock.ms() }) { clock.ms() }
        val state = flow.onRawScan(validPayload)
        assertTrue("expected Detected, got $state", state is QrPairingScanFlow.ScanState.Detected)
        val d = state as QrPairingScanFlow.ScanState.Detected
        assertEquals("tower-alpha", d.payload.towerId)
        // Summary is redaction-safe: endpoint and challenge never appear.
        assertFalse(d.summary.contains("tower.local"))
        assertFalse(d.summary.contains("CHALLENGE9"))
        assertTrue(d.summary.contains("tower=tower-alpha"))
    }

    @Test
    fun `duplicate frames inside the debounce window are ignored without state churn`() {
        val clock = Clock(now)
        val flow = QrPairingScanFlow(PairingPayloadParser(InMemoryPairingNonceStore()) { clock.ms() }) { clock.ms() }
        val first = flow.onRawScan(validPayload)
        val duplicate = flow.onRawScan(validPayload)
        assertEquals(first, duplicate)
        assertEquals(2, flow.framesAnalyzed) // both frames counted truthfully
    }

    @Test
    fun `same raw after the debounce window is re-processed and honestly refused as replay`() {
        val clock = Clock(now)
        val flow = QrPairingScanFlow(PairingPayloadParser(InMemoryPairingNonceStore()) { clock.ms() }) { clock.ms() }
        val first = flow.onRawScan(validPayload)
        assertTrue(first is QrPairingScanFlow.ScanState.Detected)
        flow.reset()
        clock.t += QrPairingScanFlow.DEBOUNCE_MS + 1
        val second = flow.onRawScan(validPayload)
        assertTrue("expected Rejected, got $second", second is QrPairingScanFlow.ScanState.Rejected)
        assertEquals(PairingRejectKind.REPLAYED_NONCE, (second as QrPairingScanFlow.ScanState.Rejected).reject.kind)
    }

    @Test
    fun `rejection does not terminate the flow - a valid QR after a bad one is accepted`() {
        val clock = Clock(now)
        val flow = QrPairingScanFlow(PairingPayloadParser(InMemoryPairingNonceStore()) { clock.ms() }) { clock.ms() }
        val bad = flow.onRawScan("https://some-other-scheme.example/qr")
        assertTrue(bad is QrPairingScanFlow.ScanState.Rejected)
        assertEquals(PairingRejectKind.UNSUPPORTED_SCHEME, (bad as QrPairingScanFlow.ScanState.Rejected).reject.kind)
        clock.t += QrPairingScanFlow.DEBOUNCE_MS + 1
        val good = flow.onRawScan(validPayload)
        assertTrue(good is QrPairingScanFlow.ScanState.Detected)
    }

    @Test
    fun `Detected is terminal until reset - later frames do not change the state`() {
        val clock = Clock(now)
        val flow = QrPairingScanFlow(PairingPayloadParser(InMemoryPairingNonceStore()) { clock.ms() }) { clock.ms() }
        flow.onRawScan(validPayload)
        clock.t += QrPairingScanFlow.DEBOUNCE_MS + 1
        val after = flow.onRawScan(expiredPayload(nonce = "OTHERNONCE1234"))
        assertTrue(after is QrPairingScanFlow.ScanState.Detected) // terminal hold
        // BOTH frames count truthfully (terminal-held frames are still surfaced).
        assertEquals(2, flow.framesAnalyzed)
    }

    @Test
    fun `expired QR is typed-rejected with the frame counter surfaced`() {
        val clock = Clock(now)
        val flow = QrPairingScanFlow(PairingPayloadParser(InMemoryPairingNonceStore()) { clock.ms() }) { clock.ms() }
        flow.onRawScan(null) // frame without QR
        val rejected = flow.onRawScan(expiredPayload())
        assertTrue(rejected is QrPairingScanFlow.ScanState.Rejected)
        val r = rejected as QrPairingScanFlow.ScanState.Rejected
        assertEquals(PairingRejectKind.EXPIRED, r.reject.kind)
        assertEquals(2, r.framesAnalyzed)
    }

    @Test
    fun `blank frames move the state to Scanning and count truthfully`() {
        val clock = Clock(now)
        val flow = QrPairingScanFlow(PairingPayloadParser(InMemoryPairingNonceStore()) { clock.ms() }) { clock.ms() }
        assertEquals(QrPairingScanFlow.ScanState.Idle, flow.state)
        val s1 = flow.onRawScan(null)
        assertTrue(s1 is QrPairingScanFlow.ScanState.Scanning)
        val s2 = flow.onRawScan("   ")
        assertTrue(s2 is QrPairingScanFlow.ScanState.Scanning)
        assertEquals(2, flow.framesAnalyzed)
    }

    @Test
    fun `reset returns to Idle and clears the debounce memory but not the nonce store`() {
        val clock = Clock(now)
        val store = InMemoryPairingNonceStore()
        val flow = QrPairingScanFlow(PairingPayloadParser(store) { clock.ms() }) { clock.ms() }
        flow.onRawScan(validPayload)
        flow.reset()
        assertEquals(QrPairingScanFlow.ScanState.Idle, flow.state)
        assertTrue("nonce stays consumed after reset", store.seen("NONCE12345678"))
        // Debounce memory cleared: the identical raw is re-processed (→ typed replay).
        val second = flow.onRawScan(validPayload)
        assertTrue(second is QrPairingScanFlow.ScanState.Rejected)
        assertEquals(PairingRejectKind.REPLAYED_NONCE, (second as QrPairingScanFlow.ScanState.Rejected).reject.kind)
    }
}
