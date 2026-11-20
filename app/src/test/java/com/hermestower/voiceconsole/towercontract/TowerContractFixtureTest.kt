package com.hermestower.voiceconsole.towercontract

import com.hermestower.voiceconsole.net.TowerApi
import com.hermestower.voiceconsole.net.TowerApiException
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * GRAPH A4R C1-C4 (mission section 12) — VERSIONED TOWER CONTRACT FIXTURES.
 *
 * LABEL: every test here runs against [TowerContractSimulator] — all outputs
 * are SIMULATED_CONTRACT_TEST material generated from the frozen
 * voice-console/v2 contract. No real Tower is contacted. The simulator is
 * NOT a parallel server authority (test source set only).
 *
 * Fixtures (the mission §12 list, pairing lanes first):
 *  1. valid QR v2 / expired QR / malformed QR / forbidden secret field
 *  2. valid numeric code / FIFTH invalid attempt lockout
 *  3. challenge replay / revoked device / unsupported protocol version
 *  4. capture: valid chunk sequence, missing/out-of-order chunk, stream resume
 *  5. archive: listing, valid artifact hash, corrupted artifact hash
 *  6. dispatch unavailable (voice submit 503)
 *
 * QR-side fixtures live with the parser (TowerQrContractFixtureTest) — this
 * file pins the NETWORK contract semantics (claim/attest/ack + lanes).
 */
class TowerContractFixtureTest {

    private fun api(): Pair<TowerApi, TowerContractSimulator> {
        val sim = TowerContractSimulator()
        return TowerApi(baseUrl = "https://tower.fixture", http = sim) to sim
    }

    // ---- pairing lane -------------------------------------------------------------

    @Test
    fun `fixture - valid numeric code claims and returns claim_key plus challenge`() {
        val (api, _) = api()
        val res = api.claim("pair-fixture-1", "654321", "Casa")
        assertEquals("claim-key-fixture", res.claimKey)
        assertEquals("challenge-claim-fixture", res.challenge)
    }

    @Test
    fun `fixture - a consumed claim is single-use (replay rejected 410)`() {
        val (api, _) = api()
        api.claim("pair-fixture-1", "654321", "Casa")
        val e = runCatching { api.claim("pair-fixture-1", "654321", "Casa") }.exceptionOrNull()
        assertTrue(e is TowerApiException && e.httpStatus == 410)
    }

    @Test
    fun `fixture - fifth invalid attempt lockout is typed RATE_LIMITED`() {
        val (api, _) = api()
        var last: TowerApiException? = null
        repeat(5) {
            last = runCatching { api.claim("pair-fixture-1", "000000", "Casa") }
                .exceptionOrNull() as? TowerApiException
        }
        assertEquals(TowerApiException.Kind.RATE_LIMITED, last?.kind)
        assertEquals(429, last?.httpStatus)
    }

    @Test
    fun `fixture - challenge replay is fail-closed 403`() {
        val (api, _) = api()
        api.popChallenge() // first challenge consumed
        val e = runCatching { api.popChallenge() }.exceptionOrNull()
        assertTrue(e is TowerApiException && e.httpStatus == 403)
    }

    @Test
    fun `fixture - attest returns device id and the Italian phrase hint`() {
        val (api, _) = api()
        val res = api.attest(
            pairingId = "pair-fixture-1",
            claimKey = "claim-key-fixture",
            challenge = "challenge-claim-fixture",
            popSignature = "pop-signature-fixture",
            publicPem = "-----BEGIN PUBLIC KEY-----",
            metadata = mapOf("curve" to "secp256r1"),
        )
        assertEquals("device-fixture-77", res.deviceId)
        assertEquals("ancora-luna-ancora", res.phraseHint)
    }

    @Test
    fun `fixture - ack releases the console token ONLY at ACKED`() {
        val (api, sim) = api()
        // PENDING: no token
        api.attest("pair-fixture-1", "claim-key-fixture", "challenge-claim-fixture", "pop", "pem", emptyMap())
        sim.ackState = "PENDING"
        val pending = api.ownerAck("pair-fixture-1")
        assertEquals(TowerApi.OwnerAckStateValue.PENDING, pending.state)
        assertEquals(null, pending.consoleToken)
        sim.ackState = "ACKED"
        val acked = api.ownerAck("pair-fixture-1")
        assertEquals(TowerApi.OwnerAckStateValue.ACKED, acked.state)
        assertEquals("console-token-fixture", acked.consoleToken)
    }

    @Test
    fun `fixture - revoked device maps to the typed REVOKED state`() {
        val (api, sim) = api()
        sim.ackState = "REVOKED"
        val ack = api.ownerAck("pair-fixture-1")
        assertEquals(TowerApi.OwnerAckStateValue.REVOKED, ack.state)
    }

    @Test
    fun `fixture - attest on a revoked device is 403 (fail closed)`() {
        val (api, sim) = api()
        sim.revokedAfterAttest = true
        val e = runCatching {
            api.attest("pair-fixture-1", "k", "c", "pop", "pem", emptyMap())
        }.exceptionOrNull()
        assertTrue(e is TowerApiException && e.kind == TowerApiException.Kind.REVOKED)
    }

    // ---- capture lane ---------------------------------------------------------------

    @Test
    fun `fixture - valid capture chunk sequence opens appends and commits`() {
        val (api, _) = api()
        assertTrue(api.captureOpen("d", "t", JSONObject().put("seq", 0)).has("capture_id"))
        assertTrue(api.captureAppend("d", "t", JSONObject().put("seq", 1).put("payload_b64", "AA")).has("ack_seq"))
        assertTrue(api.captureCommit("d", "t", JSONObject()).getBoolean("committed"))
    }

    @Test
    fun `fixture - missing or out-of-order chunk is a typed 409`() {
        val (api, sim) = api()
        sim.captureGap = true
        val e = runCatching { api.captureAppend("d", "t", JSONObject().put("seq", 5)) }.exceptionOrNull()
        assertTrue(e is TowerApiException && e.httpStatus == 409)
    }

    @Test
    fun `fixture - stream resume returns the resume sequence`() {
        val (api, _) = api()
        val res = api.captureResume("d", "t", JSONObject())
        assertEquals(2, res.getInt("resume_from_seq"))
    }

    // ---- voice / dispatch ------------------------------------------------------------

    @Test
    fun `fixture - dispatch unavailable is a typed 503`() {
        val (api, sim) = api()
        sim.dispatchAvailable = false
        val e = runCatching { api.voiceSubmit("d", "t", JSONObject().put("seq", 1)) }.exceptionOrNull()
        assertTrue(e is TowerApiException && e.httpStatus == 503)
    }

    // ---- archive lane ------------------------------------------------------------

    @Test
    fun `fixture - archive listing returns typed artifacts`() {
        val (api, _) = api()
        val list = api.archiveList("d", "t", JSONObject())
        assertEquals(2, list.length())
        assertEquals("art-1", list.getJSONObject(0).getString("artifact_id"))
        assertEquals("feedface", list.getJSONObject(1).getString("sha256"))
    }

    @Test
    fun `fixture - valid artifact hash fetch succeeds, corrupted hash is typed 422`() {
        val (api, sim) = api()
        assertTrue(api.archiveFetch("d", "t", JSONObject().put("artifact_id", "art-1")).has("sha256"))
        sim.artifactHashMatches = false
        val e = runCatching { api.archiveFetch("d", "t", JSONObject().put("artifact_id", "art-1")) }.exceptionOrNull()
        assertTrue(e is TowerApiException && e.httpStatus == 422)
    }
}
