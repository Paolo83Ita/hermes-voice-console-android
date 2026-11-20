package com.hermestower.voiceconsole.towercontract

import com.hermestower.voiceconsole.net.TowerApi
import com.hermestower.voiceconsole.net.TowerHttp
import org.json.JSONArray
import org.json.JSONObject

/**
 * GRAPH A4R C1-C4 (mission section 12) — Tower contract SIMULATOR generated
 * from the frozen voice-console/v2 contract truth (dex surface + CD-A4R-001).
 *
 * LABEL: SIMULATED_CONTRACT_TEST — every response this class produces is
 * simulated contract material for the JVM fixture suite. It is NOT a parallel
 * server authority (it lives in the test source set, is never wired into the
 * production graph, and cannot issue real pairing state).
 *
 * Semantics implemented from the frozen contract:
 *  - pairing claim: SINGLE-USE numeric claim code, EXACTLY 6 digits, with a
 *    FIFTH-INVALID-ATTEMPT LOCKOUT (HTTP 429) — mission fixture list;
 *  - pop-challenge: single-use challenge; a replayed challenge is rejected
 *    with 403 (fail-closed revocation-class);
 *  - attest: verifies the PoP material is present and returns device_id +
 *    phrase_hint (Italian three-word phrase);
 *  - ack poll: PENDING -> ACKED (releasing console_token ONLY at ACKED)
 *    / REVOKED / EXPIRED;
 *  - capture/voice/archive lanes respond with typed status codes (200 OK,
 *    409 gap, 422 hash mismatch, 503 dispatch unavailable) so the client's
 *    typed mapping is pinned without a network.
 */
class TowerContractSimulator : TowerHttp {

    /** Counters for fixture assertions (which routes the client actually hit). */
    val calls = ArrayDeque<Pair<String, String>>()

    // ---- configurable policy knobs (each fixture sets what it needs) ----
    var pairingId = "pair-fixture-1"
    var validClaimCode = "654321"
    var invalidAttempts = 0
    var ackState = "PENDING"
    var revokedAfterAttest = false
    var replayedChallenge = false
    var dispatchAvailable = true
    var artifactHashMatches = true
    var captureGap = false
    var challengeTtlRemainingSeconds = 100L

    private var claimConsumed = false
    private var challengeUsed = false
    private var deviceRegistered = false

    fun reset() {
        calls.clear()
        claimConsumed = false
        challengeUsed = false
        deviceRegistered = false
        invalidAttempts = 0
        ackState = "PENDING"
        revokedAfterAttest = false
        replayedChallenge = false
        dispatchAvailable = true
        artifactHashMatches = true
        captureGap = false
    }

    /** The Italian three-word phrase the fixtures pin (bilateral confirmation). */
    val phraseHint = "ancora-luna-ancora"

    override fun execute(request: TowerHttp.Request): TowerHttp.Response {
        val path = request.url.substringAfter("://").substringAfter('/')
        calls += request.method to path
        val body = request.body?.let { runCatching { JSONObject(it) }.getOrNull() }
        return when {
            path.endsWith("/pop-challenge") -> {
                if (challengeUsed) {
                    // REPLAYED CHALLENGE: fail closed (403 revocation class).
                    TowerHttp.Response(403, """{"error":"challenge_replay"}""")
                } else {
                    challengeUsed = true
                    TowerHttp.Response(
                        200,
                        JSONObject()
                            .put("challenge", "challenge-fixture-$challengeTtlRemainingSeconds")
                            .put("expires_in", challengeTtlRemainingSeconds)
                            .toString(),
                    )
                }
            }
            path.contains("/pairings/") && path.endsWith("/claim") -> {
                if (claimConsumed) return TowerHttp.Response(410, """{"error":"pairing_already_claimed"}""")
                val code = body?.optString("claim_code") ?: ""
                if (code == validClaimCode) {
                    claimConsumed = true
                    TowerHttp.Response(
                        200,
                        JSONObject().put("claim_key", "claim-key-fixture").put("challenge", "challenge-claim-fixture").toString(),
                    )
                } else {
                    invalidAttempts++
                    when {
                        invalidAttempts >= 5 -> TowerHttp.Response(429, """{"error":"too_many_attempts","detail":"fifth invalid attempt lockout"}""")
                        else -> TowerHttp.Response(401, """{"error":"invalid_claim_code","attempts_left":${5 - invalidAttempts}}""")
                    }
                }
            }
            path.contains("/pairings/") && path.endsWith("/attest") -> {
                val pop = body?.optString("pop_signature") ?: ""
                if (pop.isEmpty() || body?.optString("challenge").isNullOrEmpty()) {
                    TowerHttp.Response(422, """{"error":"attest_incomplete"}""")
                } else if (revokedAfterAttest) {
                    TowerHttp.Response(403, """{"error":"device_revoked"}""")
                } else {
                    deviceRegistered = true
                    if (ackState == "PENDING") ackState = "ACKED"
                    TowerHttp.Response(
                        200,
                        JSONObject()
                            .put("device_id", "device-fixture-77")
                            .put("phrase_hint", phraseHint)
                            .toString(),
                    )
                }
            }
            path.contains("/pairings/") && path.endsWith("/ack") -> {
                if (!deviceRegistered && ackState == "ACKED") ackState = "PENDING"
                val payload = JSONObject().put("state", ackState)
                when (ackState) {
                    "ACKED" -> payload
                        .put("device_id", "device-fixture-77")
                        .put("console_token", "console-token-fixture")
                        .put("phrase_hint", phraseHint)
                    "REVOKED" -> payload.put("detail", "revoked by owner")
                    "EXPIRED" -> payload.put("detail", "pairing window expired")
                }
                TowerHttp.Response(200, payload.toString())
            }
            path.endsWith("/g4/capture/open") -> TowerHttp.Response(200, JSONObject().put("capture_id", "cap-1").put("last_acked_seq", 0).toString())
            path.endsWith("/g4/capture/append") ->
                if (captureGap) TowerHttp.Response(409, """{"error":"chunk_gap","expected_seq":3}""")
                else TowerHttp.Response(200, JSONObject().put("ack_seq", body?.optInt("seq") ?: 1).toString())
            path.endsWith("/g4/capture/commit") -> TowerHttp.Response(200, JSONObject().put("committed", true).put("client_sha", "abc").toString())
            path.endsWith("/g4/capture/resume") -> TowerHttp.Response(200, JSONObject().put("resume_from_seq", 2).toString())
            path.endsWith("/g4/capture/cancel") -> TowerHttp.Response(200, JSONObject().put("cancelled", true).toString())
            path.endsWith("/g4/voice/submit") ->
                if (dispatchAvailable) TowerHttp.Response(200, JSONObject().put("turn_seq", body?.optInt("seq") ?: 1).put("state", "DISPATCHED").toString())
                else TowerHttp.Response(503, """{"error":"dispatch_unavailable"}""")
            path.endsWith("/g4/voice/response-stream") -> TowerHttp.Response(200, JSONArray().put(JSONObject().put("event", "token").put("v", "ciao")).toString())
            path.endsWith("/g4/voice/cancel") -> TowerHttp.Response(200, JSONObject().put("cancelled", true).toString())
            path.endsWith("/g4/archive/list") -> TowerHttp.Response(
                200,
                JSONArray()
                    .put(JSONObject().put("artifact_id", "art-1").put("sha256", "deadbeef"))
                    .put(JSONObject().put("artifact_id", "art-2").put("sha256", "feedface"))
                    .toString(),
            )
            path.endsWith("/g4/archive/metadata") -> TowerHttp.Response(200, JSONObject().put("artifact_id", "art-1").put("bytes", 1024).toString())
            path.endsWith("/g4/archive/fetch") ->
                if (artifactHashMatches) TowerHttp.Response(200, JSONObject().put("artifact_id", "art-1").put("sha256", "deadbeef").put("payload_b64", "AAEC").toString())
                else TowerHttp.Response(422, """{"error":"artifact_hash_mismatch"}""")
            path.endsWith("/g4/archive/bind") -> TowerHttp.Response(200, JSONObject().put("bound", true).toString())
            path.endsWith("/g4/archive/download-receipt") -> TowerHttp.Response(200, JSONObject().put("receipt_id", "r-1").put("verified", true).toString())
            path.endsWith("/pairings") && request.method == "POST" ->
                TowerHttp.Response(200, JSONObject().put("pairing_id", pairingId).put("expires_at", 180).toString())
            else -> TowerHttp.Response(404, """{"error":"no_such_route"}""")
        }
    }
}
