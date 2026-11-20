package com.hermestower.voiceconsole.towercontract

import com.hermestower.voiceconsole.pairing.QrPayload
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * GRAPH A4R C1-C4 (mission section 12) — QR v2 contract fixtures over the
 * B-bound parser (SIMULATED_CONTRACT_TEST: synthetic QR material generated
 * from the frozen contract; no real Tower QR is used or required).
 */
class TowerQrContractFixtureTest {

    /** Every fixture gets its OWN parser: the nonce_ref store is per-instance
     *  state, so sharing one parser across tests would couple them. */
    private fun freshParser(): QrPayload.Parser = QrPayload.Parser(clockEpochSeconds = { 1_000L })

    private fun qr(vararg overrides: Pair<String, Any>): String {
        val obj = JSONObject()
            .put("v", 2)
            .put("protocol_version", "voice-console/1.0.0")
            .put("tower_instance_id", "tower-fixture")
            .put("endpoint", "https://hermestower.aclclouds.online")
            .put("pairing_id", "pair-fixture-1")
            .put("scope", org.json.JSONArray(listOf("console")))
            .put("expires_at", 2_000L)
            .put("transport_hint", "https")
            .put("nonce_ref", "nonce-fixture-1")
        for ((k, v) in overrides) obj.put(k, v)
        val payload = java.util.Base64.getUrlEncoder().withoutPadding()
            .encodeToString(obj.toString().toByteArray(Charsets.UTF_8))
        return "$payload.c2ln"
    }

    private fun rejectOf(raw: String, anotherParser: QrPayload.Parser = QrPayload.Parser(clockEpochSeconds = { 1_000L })): QrPayload.RejectKind =
        try {
            anotherParser.parse(raw)
            throw AssertionError("expected rejection for: $raw")
        } catch (e: QrPayload.QrRejectException) {
            e.reject.kind
        }

    @Test
    fun `fixture 1 - valid QR v2 parses with all eight contract fields`() {
        val p = freshParser().parse(qr())
        assertEquals("voice-console/1.0.0", p.protocolVersion)
        assertEquals("tower-fixture", p.towerInstanceId)
        assertEquals("https://hermestower.aclclouds.online", p.endpoint)
        assertEquals("pair-fixture-1", p.pairingId)
        assertEquals(listOf("console"), p.scope)
        assertEquals(2_000L, p.expiresAtEpochSeconds)
        assertEquals("https", p.transportHint)
        assertEquals("nonce-fixture-1", p.nonceRef)
    }

    @Test
    fun `fixture 2 - expired QR is typed EXPIRED`() {
        val expired = qr("expires_at" to 500L) // before the 1000s clock
        assertEquals(QrPayload.RejectKind.EXPIRED, rejectOf(expired))
    }

    @Test
    fun `fixture 3 - malformed QR (no signature part) is typed MALFORMED`() {
        assertEquals(QrPayload.RejectKind.MALFORMED, rejectOf("not-a-qr"))
        assertEquals(QrPayload.RejectKind.MALFORMED, rejectOf("a.b.c"))
    }

    @Test
    fun `fixture 4 - forbidden secret field is typed FORBIDDEN_FIELD`() {
        for (banned in listOf("token_secret", "api_key", "password", "access_token")) {
            val malicious = qr(banned to "leak-attempt")
            assertEquals(QrPayload.RejectKind.FORBIDDEN_FIELD, rejectOf(malicious, QrPayload.Parser(clockEpochSeconds = { 1_000L })))
        }
    }

    @Test
    fun `fixture 5 - unsupported protocol version is typed UNSUPPORTED_VERSION`() {
        assertEquals(QrPayload.RejectKind.UNSUPPORTED_VERSION, rejectOf(qr("v" to 1)))
        assertEquals(QrPayload.RejectKind.UNSUPPORTED_VERSION, rejectOf(qr("v" to 3)))
    }

    @Test
    fun `fixture 6 - foreign protocol_version is typed UNSUPPORTED_PROTOCOL`() {
        assertEquals(QrPayload.RejectKind.UNSUPPORTED_PROTOCOL, rejectOf(qr("protocol_version" to "other-protocol/9")))
    }

    @Test
    fun `fixture 7 - QR nonce_ref replay is typed EXPIRED (single-use)`() {
        val fresh = QrPayload.Parser(clockEpochSeconds = { 1_000L })
        fresh.parse(qr())
        assertEquals(QrPayload.RejectKind.REPLAYED_NONCE_REF, rejectOf(qr(), fresh))
    }

    @Test
    fun `fixture 8 - endpoint resolution prefers the QR and requires the manual fallback otherwise`() {
        val p = freshParser().parse(qr())
        assertEquals("https://hermestower.aclclouds.online", freshParser().resolveEndpoint(p, null))
        val noEndpoint = freshParser().parse(qr("endpoint" to ""))
        assertEquals("https://manual.tower.lan", freshParser().resolveEndpoint(noEndpoint, "https://manual.tower.lan"))
        try {
            freshParser().resolveEndpoint(noEndpoint, null)
            throw AssertionError("expected BAD_ENDPOINT")
        } catch (e: QrPayload.QrRejectException) {
            assertEquals(QrPayload.RejectKind.BAD_ENDPOINT, e.reject.kind)
        }
        // LAN shape rule preserved verbatim from B ("http" prefix accepted for
        // the endpoint field; the mission's https-only rule is enforced by the
        // canonical ManualEndpoint validator on the manual lane): the QR-borne
        // LAN endpoint is accepted as B shipped it (ORIGINAL SOURCE FIRST),
        // and the claim body carries no secret either way.
        val lanQr = freshParser().parse(qr("endpoint" to "http://192.168.1.10:8080"))
        assertEquals("http://192.168.1.10:8080", freshParser().resolveEndpoint(lanQr, null))
    }

    @Test
    fun `fixture 9 - redaction describe never carries endpoint or pairing id`() {
        val p = freshParser().parse(qr())
        val described = freshParser().describe(p)
        assertTrue(!described.contains("hermestower.aclclouds.online"))
        assertTrue(!described.contains("pair-fixture-1"))
    }

    @Test
    fun `fixture 10 - garbage payloads are MALFORMED, never a crash`() {
        assertEquals(QrPayload.RejectKind.MALFORMED, rejectOf("%%'\\\".; DROP"))
    }
}
