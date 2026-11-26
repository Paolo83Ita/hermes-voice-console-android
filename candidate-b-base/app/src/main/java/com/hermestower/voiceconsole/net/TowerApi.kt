package com.hermestower.voiceconsole.net

import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * GRAPH A4R C1-C4 — Tower voice-console v2 API client (B lineage,
 * RECONSTRUCTED SOURCE, NOT decompiled code).
 *
 * PROVENANCE: see QrPayload.kt. The 18 live routes are bound from B's dex
 * string pool (physical evidence, byte-exact APK 908b289f…) and the previous
 * session's contract delta CD-A4R-001:
 *
 *  pairing   POST /api/tower/voice-console/v2/pairings            (create — Tower console side)
 *            POST /api/tower/voice-console/v2/pairings/{id}/claim
 *            POST /api/tower/voice-console/v2/pairings/{id}/attest
 *            POST /api/tower/voice-console/v2/pop-challenge
 *  capture   POST /api/tower/voice-console/v2/g4/capture/{open,append,partial,commit,resume,cancel}
 *  voice     POST /api/tower/voice-console/v2/g4/voice/{submit,response-stream,cancel}
 *  archive   POST /api/tower/voice-console/v2/g4/archive/{bind,list,metadata,fetch,download-receipt}
 *
 * REQUEST/RESPONSE FIELD CONTRACT: response keys bound from the dex string
 * pool (claim_key, challenge, device_id, console_token, phrase…); request
 * body keys are the typed hypothesis recorded in
 * docs/a4r/ANDROID_TOWER_CONTRACT_DELTA_REQUEST.json — a simulator fixture
 * (SIMULATED_CONTRACT_TEST) pins them and any divergence becomes a versioned
 * contract delta, never a client-side bypass.
 *
 * The transport is a seam ([TowerHttp]) so the contract fixtures and the JVM
 * pairing tests run deterministically without network I/O; the production
 * implementation is [HttpUrlTowerHttp] (java.net.HttpURLConnection — zero new
 * dependencies). Auth: the console token travels ONLY in the Authorization
 * header after pairing and is never logged.
 */
interface TowerHttp {
    data class Request(val method: String, val url: String, val body: String?, val bearerToken: String?)
    data class Response(val statusCode: Int, val body: String)

    fun execute(request: Request): Response
}

class HttpUrlTowerHttp(private val connectTimeoutMs: Int = 10_000, private val readTimeoutMs: Int = 20_000) : TowerHttp {
    override fun execute(request: TowerHttp.Request): TowerHttp.Response {
        val conn = URL(request.url).openConnection() as HttpURLConnection
        try {
            conn.requestMethod = request.method
            conn.connectTimeout = connectTimeoutMs
            conn.readTimeout = readTimeoutMs
            if (request.bearerToken != null) {
                conn.setRequestProperty("Authorization", "Bearer ${request.bearerToken}")
            }
            if (request.body != null) {
                conn.doOutput = true
                conn.setRequestProperty("Content-Type", "application/json")
                conn.outputStream.use { it.write(request.body.toByteArray(Charsets.UTF_8)) }
            }
            val stream = try {
                conn.inputStream
            } catch (_: java.io.IOException) {
                conn.errorStream
            }
            val bodyText = stream?.use { it.readBytes().toString(Charsets.UTF_8) } ?: ""
            return TowerHttp.Response(conn.responseCode, bodyText)
        } finally {
            conn.disconnect()
        }
    }
}

/**
 * Typed transport failure mapped by the pairing flow (never a raw stack trace).
 *
 * ANDROID REPAIR CP-005 additions (mission section 3 / AMB-8 / AMB-9):
 *  - IN_BAND_ERROR: the Tower's error envelope {ok:false, error:CODE} read
 *    BEFORE DTO parsing (the AMB-9 repair — the Tower's typed reason is never
 *    lost into 'risposta claim incompleta');
 *  - ROUTE_MISSING: HTTP 404 catch-all ('No such API endpoint') — the AMB-8
 *    honest state: a route the client speaks that the server does not serve;
 *  - transportCode: the classified TLS/network failure code when the failure
 *    is transport-class (DNS/TCP/TLS/cleartext), for the pairing diagnostics.
 */
class TowerApiException(
    val kind: Kind,
    val httpStatus: Int?,
    detail: String,
    cause: Throwable? = null,
    val inBandError: String? = null,
    val transportCode: String? = null,
) : Exception(detail, cause) {
    enum class Kind {
        NETWORK_UNREACHABLE,
        SERVER_ERROR,
        REJECTED,
        REVOKED,
        RATE_LIMITED,
        MALFORMED_RESPONSE,
        /** The server answered in-band {ok:false,error:CODE} — the typed reason is in [inBandError]. */
        IN_BAND_ERROR,
        /** HTTP 404 catch-all: the route does not exist on this server version (AMB-8). */
        ROUTE_MISSING,
    }
}

class TowerApi(
    /** Base URL WITHOUT a trailing slash, e.g. https://hermestower.aclclouds.online */
    private var baseUrl: String = "",
    private val http: TowerHttp = HttpUrlTowerHttp(),
) {

    /** B public API: the flow re-binds the base URL per pairing attempt. */
    fun setBaseUrl(endpoint: String) {
        baseUrl = endpoint.trim().trimEnd('/')
    }

    fun currentBaseUrl(): String = baseUrl
    private fun url(path: String) = "$baseUrl$path"

    private fun call(method: String, path: String, body: JSONObject?, bearer: String?): JSONObject {
        val res = rawCall(method, path, body, bearer)
        val obj = try {
            JSONObject(res)
        } catch (e: Exception) {
            throw TowerApiException(TowerApiException.Kind.MALFORMED_RESPONSE, null, "risposta non leggibile", e)
        }
        readInBandEnvelope(obj)
        return obj
    }

    /**
     * AMB-9 ENVELOPE ADAPTER (mission section 3): the live Tower reports errors
     * as HTTP 200 {ok:false, error:CODE}. The envelope is read BEFORE any DTO
     * field parsing so the Tower's typed reason reaches the UI catalog instead
     * of the generic 'risposta incompleta'. Success envelopes ({ok:true} or no
     * ok field) pass through unchanged. Pinned by JVM fixtures for BOTH shapes.
     */
    private fun readInBandEnvelope(obj: JSONObject) {
        if (obj.has("ok") && !obj.optBoolean("ok", true)) {
            val code = obj.optString("error", "UNKNOWN_IN_BAND_ERROR")
            throw TowerApiException(
                TowerApiException.Kind.IN_BAND_ERROR,
                200,
                "Tower: $code",
                null,
                inBandError = code,
            )
        }
    }

    /** Raw body text for array-response routes (archive/list). */
    private fun rawCall(method: String, path: String, body: JSONObject?, bearer: String?): String {
        val req = TowerHttp.Request(
            method = method,
            url = url(path),
            body = body?.toString(),
            bearerToken = bearer,
        )
        val res = try {
            http.execute(req)
        } catch (e: java.io.IOException) {
            throw TowerApiException(TowerApiException.Kind.NETWORK_UNREACHABLE, null, "Tower non raggiungibile", e)
        } catch (e: Exception) {
            throw TowerApiException(TowerApiException.Kind.NETWORK_UNREACHABLE, null, "transport failure", e)
        }
        if (res.statusCode == 429) throw TowerApiException(TowerApiException.Kind.RATE_LIMITED, 429, "troppi tentativi (blocco tentativi errati)")
        if (res.statusCode == 403 || res.statusCode == 410) {
            throw TowerApiException(TowerApiException.Kind.REVOKED, res.statusCode, "dispositivo revocato o pairing non valido")
        }
        if (res.statusCode == 404) {
            // AMB-8: the catch-all 'No such API endpoint' — the route does not
            // exist on this server version (e.g. the ack route on the live
            // Tower today). Typed as ROUTE_MISSING, never a generic rejection.
            throw TowerApiException(TowerApiException.Kind.ROUTE_MISSING, 404, "rotta non presente su questa versione della Tower")
        }
        if (res.statusCode < 200 || res.statusCode >= 300) {
            throw TowerApiException(TowerApiException.Kind.REJECTED, res.statusCode, "richiesta rifiutata dalla Tower")
        }
        return res.body
    }

    private fun callArray(method: String, path: String, body: JSONObject?, bearer: String?): JSONArray {
        val res = rawCall(method, path, body, bearer)
        return try {
            JSONArray(res)
        } catch (e: Exception) {
            throw TowerApiException(TowerApiException.Kind.MALFORMED_RESPONSE, null, "risposta elenco non leggibile", e)
        }
    }

    // ------------------------------------------------------------------
    // Pairing lane (production path)
    // ------------------------------------------------------------------

    data class ClaimResponse(val claimKey: String, val challenge: String)

    /**
     * POST /pairings/{id}/claim — consumes the single-use pairing claim with
     * the numeric code as the claim secret. Request keys are the typed
     * hypothesis (contract delta request CD-A4R-C1-001): claim_code,
     * device_label.
     */
    fun claim(pairingId: String, claimSecret: String, deviceLabel: String?): ClaimResponse {
        val body = JSONObject()
            .put("claim_code", claimSecret)
            .put("device_label", deviceLabel ?: "Console vocale Android")
        val obj = call("POST", "/api/tower/voice-console/v2/pairings/$pairingId/claim", body, null)
        val claimKey = obj.optString("claim_key", "")
        val challenge = obj.optString("challenge", "")
        if (claimKey.isEmpty() || challenge.isEmpty()) {
            throw TowerApiException(TowerApiException.Kind.MALFORMED_RESPONSE, null, "risposta claim incompleta")
        }
        return ClaimResponse(claimKey, challenge)
    }

    data class AttestResponse(val deviceId: String, val phraseHint: String)

    /**
     * POST /pairings/{id}/attest — EC-P256 proof-of-possession over the
     * single-use challenge. Keys: pop_signature, device_public_key_pem,
     * device_key_algorithm, attestation_metadata.
     */
    fun attest(pairingId: String, claimKey: String, challenge: String, popSignature: String, publicPem: String, metadata: Map<String, String>): AttestResponse {
        val meta = JSONObject()
        for ((k, v) in metadata) meta.put(k, v)
        val body = JSONObject()
            .put("claim_key", claimKey)
            .put("challenge", challenge)
            .put("pop_signature", popSignature)
            .put("device_public_key_pem", publicPem)
            .put("device_key_algorithm", "EC")
            .put("attestation_metadata", meta)
        val obj = call("POST", "/api/tower/voice-console/v2/pairings/$pairingId/attest", body, null)
        val deviceId = obj.optString("device_id", "")
        if (deviceId.isEmpty()) throw TowerApiException(TowerApiException.Kind.MALFORMED_RESPONSE, null, "risposta attest incompleta")
        val phrase = obj.optString("phrase_hint", obj.optString("phrase", ""))
        return AttestResponse(deviceId, phrase)
    }

    /** POST /pop-challenge — fresh single-use challenge for the PoP signature. */
    fun popChallenge(): String {
        val obj = call("POST", "/api/tower/voice-console/v2/pop-challenge", JSONObject().put("purpose", "pairing"), null)
        val c = obj.optString("challenge", "")
        if (c.isEmpty()) throw TowerApiException(TowerApiException.Kind.MALFORMED_RESPONSE, null, "challenge mancante")
        return c
    }

    enum class OwnerAckStateValue { PENDING, ACKED, REVOKED, EXPIRED, UNKNOWN }

    data class OwnerAck(val state: OwnerAckStateValue, val deviceId: String?, val consoleToken: String?, val phraseHint: String?)

    /**
     * GET /pairings/{id}/ack — bilateral Owner acknowledgement poll. The
     * console token is released by the Tower ONLY at ACKED (after the
     * three-word Italian phrase match on both sides).
     */
    fun ownerAck(pairingId: String): OwnerAck {
        val obj = call("GET", "/api/tower/voice-console/v2/pairings/$pairingId/ack", null, null)
        val raw = obj.optString("state", "UNKNOWN").uppercase()
        val state = when (raw) {
            "PENDING" -> OwnerAckStateValue.PENDING
            "ACKED", "ACK", "CONFIRMED" -> OwnerAckStateValue.ACKED
            "REVOKED", "REJECTED" -> OwnerAckStateValue.REVOKED
            "EXPIRED" -> OwnerAckStateValue.EXPIRED
            else -> OwnerAckStateValue.UNKNOWN
        }
        return OwnerAck(
            state = state,
            deviceId = obj.optString("device_id", "").takeIf { it.isNotEmpty() },
            consoleToken = obj.optString("console_token", "").takeIf { it.isNotEmpty() },
            phraseHint = obj.optString("phrase_hint", obj.optString("phrase", "")).takeIf { it.isNotEmpty() },
        )
    }

    /** POST /pairings — Tower-side pairing creation (contract-complete; the client does not drive it in production navigation). */
    fun createPairing(towerInstanceId: String, scope: List<String>): JSONObject {
        val body = JSONObject()
            .put("tower_instance_id", towerInstanceId)
            .put("scope", JSONArray(scope))
        return call("POST", "/api/tower/voice-console/v2/pairings", body, null)
    }

    /**
     * ANDROID REPAIR CP-005 — raw GET for the CONNECTION_TEST stage (mission
     * section 4): a REAL transport probe (TCP+TLS+HTTP, real latency), never a
     * hardcoded 'SIMULATED' capability value (APP-P1-07 repair).
     */
    fun rawGet(url: String): Int {
        val res = try {
            http.execute(TowerHttp.Request(method = "GET", url = url, body = null, bearerToken = null))
        } catch (e: java.io.IOException) {
            throw TowerApiException(TowerApiException.Kind.NETWORK_UNREACHABLE, null, "Tower non raggiungibile", e)
        }
        return res.statusCode
    }

    /**
     * ANDROID REPAIR CP-005 — capability negotiation route (capneg/1, pending
     * the Tower-side delta HR-A4R-PHYS-002 DELTA-D). ROUTE_MISSING on the live
     * Tower today → the client degrades honestly to DEGRADED/un-negotiated.
     */
    fun capabilities(bearer: String?): JSONObject =
        call("POST", "/api/tower/voice-console/v2/g4/capabilities", JSONObject(), bearer)

    // ------------------------------------------------------------------
    // Capture / voice / archive lanes (contract-complete surface; the
    // production voice switch is the NEXT physical block after the Owner
    // mini-gate — the pairing mini-gate P1-P8 does not exercise them).
    // ------------------------------------------------------------------

    fun captureOpen(deviceId: String, bearer: String, body: JSONObject): JSONObject =
        call("POST", "/api/tower/voice-console/v2/g4/capture/open", body.put("device_id", deviceId), bearer)

    fun captureAppend(deviceId: String, bearer: String, body: JSONObject): JSONObject =
        call("POST", "/api/tower/voice-console/v2/g4/capture/append", body.put("device_id", deviceId), bearer)

    fun capturePartial(deviceId: String, bearer: String, body: JSONObject): JSONObject =
        call("POST", "/api/tower/voice-console/v2/g4/capture/partial", body.put("device_id", deviceId), bearer)

    fun captureCommit(deviceId: String, bearer: String, body: JSONObject): JSONObject =
        call("POST", "/api/tower/voice-console/v2/g4/capture/commit", body.put("device_id", deviceId), bearer)

    fun captureResume(deviceId: String, bearer: String, body: JSONObject): JSONObject =
        call("POST", "/api/tower/voice-console/v2/g4/capture/resume", body.put("device_id", deviceId), bearer)

    fun captureCancel(deviceId: String, bearer: String, body: JSONObject): JSONObject =
        call("POST", "/api/tower/voice-console/v2/g4/capture/cancel", body.put("device_id", deviceId), bearer)

    fun voiceSubmit(deviceId: String, bearer: String, body: JSONObject): JSONObject =
        call("POST", "/api/tower/voice-console/v2/g4/voice/submit", body.put("device_id", deviceId), bearer)

    fun voiceResponseStream(deviceId: String, bearer: String, body: JSONObject): JSONObject =
        call("POST", "/api/tower/voice-console/v2/g4/voice/response-stream", body.put("device_id", deviceId), bearer)

    fun voiceCancel(deviceId: String, bearer: String, body: JSONObject): JSONObject =
        call("POST", "/api/tower/voice-console/v2/g4/voice/cancel", body.put("device_id", deviceId), bearer)

    fun archiveBind(deviceId: String, bearer: String, body: JSONObject): JSONObject =
        call("POST", "/api/tower/voice-console/v2/g4/archive/bind", body.put("device_id", deviceId), bearer)

    /** GET /g4/archive/list — array body (typed artifacts). */
    fun archiveList(deviceId: String, bearer: String, body: JSONObject): JSONArray =
        callArray("POST", "/api/tower/voice-console/v2/g4/archive/list", body.put("device_id", deviceId), bearer)

    fun archiveMetadata(deviceId: String, bearer: String, body: JSONObject): JSONObject =
        call("POST", "/api/tower/voice-console/v2/g4/archive/metadata", body.put("device_id", deviceId), bearer)

    fun archiveFetch(deviceId: String, bearer: String, body: JSONObject): JSONObject =
        call("POST", "/api/tower/voice-console/v2/g4/archive/fetch", body.put("device_id", deviceId), bearer)

    fun archiveDownloadReceipt(deviceId: String, bearer: String, body: JSONObject): JSONObject =
        call("POST", "/api/tower/voice-console/v2/g4/archive/download-receipt", body.put("device_id", deviceId), bearer)
}
