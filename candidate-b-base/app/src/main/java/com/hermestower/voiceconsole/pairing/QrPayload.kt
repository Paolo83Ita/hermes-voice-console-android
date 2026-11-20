package com.hermestower.voiceconsole.pairing

import org.json.JSONArray
import org.json.JSONObject

/**
 * GRAPH A4R C1-C4 — Tower QR v2 payload parser (B lineage, RECONSTRUCTED
 * SOURCE, NOT decompiled code).
 *
 * PROVENANCE (CANDIDATE_B_SOURCE_MANIFEST.json): the original
 * apps/android-voice-console/pairing/QrPayload.kt source tree is NOT reachable
 * in this execution environment (Tower GitHub repository ABSENT — fresh-read
 * 2026-11-20, GET /repos/Paolo83Ita/Hermes-Tower = 404; no local checkout; no
 * git bundle). This file is a FRESH canonical implementation bound to B's
 * observable contract surface (jadx forensic reading of the byte-exact B APK
 * `908b289f…`, dex string pool, and the previous session's contract delta
 * CD-A4R-001). It is used ONLY for: forensic confirmation, provenance
 * validation, and behavioral-spec binding — per mission section 1 decompiled
 * code is NEVER copied into the canonical application, and none is.
 *
 * BOUND BEHAVIOR (forensic truth from B's shipped binary):
 *  - QR text = `<base64url(payload JSON)>.<base64url(signature)>` (exactly
 *    two dot-separated parts);
 *  - FORBIDDEN fields (SecurityException class): password, api_key, apikey,
 *    secret, token_secret, private_key, credential, bearer, refresh_token,
 *    access_token — a QR carrying any of them is REJECTED (no secrets in QR,
 *    mission section 7);
 *  - integer field `v` must be exactly 2 (typed QR_SCHEMA_UNSUPPORTED
 *    otherwise, B's Italian message "Versione protocollo QR non supportata");
 *  - `protocol_version` must start with "voice-console/";
 *  - `expires_at` is epoch SECONDS and must be in the future (B's TTL note:
 *    2-5 minutes from Tower-side opening — the client enforces expiry only,
 *    the Tower owns issuance);
 *  - fields: tower_instance_id, endpoint, pairing_id, scope (string array),
 *    transport_hint (default "https"), nonce_ref;
 *  - endpoint resolution: QR endpoint if http(s), else the manual fallback,
 *    else typed failure (B's "Nessun endpoint disponibile");
 *  - the signature part is NOT verified client-side in B (the Tower verifies
 *    possession at claim time) — preserved verbatim; no client-side bypass is
 *    created, the claim secret is the pairing code the Owner types.
 *
 * DIFFERENCE FROM THE OLD CANONICAL v1 PARSER (PairingPayload.kt): the v1
 * hermes-pair:// URI scheme was CANDIDATE A's SIMULATED candidate scheme; v2
 * JSON is the LIVE Tower schema and is the ONLY production scanner input.
 */
object QrPayload {

    /** Typed v2 rejections — mapped 1:1 onto the mission section 8 states. */
    enum class RejectKind {
        MALFORMED,
        FORBIDDEN_FIELD,
        UNSUPPORTED_VERSION,
        UNSUPPORTED_PROTOCOL,
        EXPIRED,
        BAD_ENDPOINT,
        REPLAYED_NONCE_REF,
    }

    data class Reject(val kind: RejectKind, val detail: String) {
        /** Redaction-safe by construction: never carries the raw QR text. */
        override fun toString(): String = "QrReject(kind=$kind, detail=$detail)"
    }

    class QrRejectException(val reject: Reject) : Exception(reject.toString())

    data class Parsed(
        val protocolVersion: String,
        val towerInstanceId: String,
        val endpoint: String,
        val pairingId: String,
        val scope: List<String>,
        val expiresAtEpochSeconds: Long,
        val transportHint: String,
        val nonceRef: String,
    )

    /**
     * The fields that must NEVER appear in a QR payload (mission section 7:
     * no persistent secret travels in the QR). A payload carrying any of them
     * is rejected BEFORE any network use.
     */
    val FORBIDDEN_FIELDS: List<String> = listOf(
        "password", "api_key", "apikey", "secret", "token_secret",
        "private_key", "credential", "bearer", "refresh_token", "access_token",
    )

    const val SUPPORTED_VERSION = 2
    const val PROTOCOL_PREFIX = "voice-console/"

    /** Replay seam: the client remembers consumed nonce_ref values for the session. */
    interface NonceRefStore {
        fun seen(nonceRef: String): Boolean
        fun remember(nonceRef: String)
    }

    class InMemoryNonceRefStore : NonceRefStore {
        private val seen = LinkedHashSet<String>()
        override fun seen(nonceRef: String): Boolean = synchronized(seen) { nonceRef in seen }
        override fun remember(nonceRef: String) { synchronized(seen) { seen += nonceRef } }
        fun clear() = synchronized(seen) { seen.clear() }
    }

    class Parser(
        private val nonceRefs: NonceRefStore = InMemoryNonceRefStore(),
        private val clockEpochSeconds: () -> Long = { System.currentTimeMillis() / 1000L },
    ) {
        fun parse(qrText: String): Parsed {
            val text = qrText.trim()
            if (text.isEmpty()) throw QrRejectException(Reject(RejectKind.MALFORMED, "QR vuoto"))
            val parts = text.split(".")
            if (parts.size != 2 || parts[0].isEmpty() || parts[1].isEmpty()) {
                throw QrRejectException(Reject(RejectKind.MALFORMED, "Formato QR non valido (atteso payload.firma)"))
            }
            val payloadJson = try {
                String(java.util.Base64.getUrlDecoder().decode(parts[0]), Charsets.UTF_8)
            } catch (_: IllegalArgumentException) {
                throw QrRejectException(Reject(RejectKind.MALFORMED, "Payload QR non decodificabile"))
            }
            val obj = try {
                JSONObject(payloadJson)
            } catch (_: Exception) {
                throw QrRejectException(Reject(RejectKind.MALFORMED, "Payload QR non decodificabile"))
            }
            // Forbidden-secret gate BEFORE anything else (FAIL CLOSED).
            for (banned in FORBIDDEN_FIELDS) {
                if (obj.has(banned)) {
                    throw QrRejectException(Reject(RejectKind.FORBIDDEN_FIELD, "QR contiene un campo vietato"))
                }
            }
            val v = obj.optInt("v", 0)
            if (v != SUPPORTED_VERSION) {
                throw QrRejectException(Reject(RejectKind.UNSUPPORTED_VERSION, "Versione protocollo QR non supportata: $v"))
            }
            val proto = obj.optString("protocol_version", "")
            if (!proto.startsWith(PROTOCOL_PREFIX)) {
                throw QrRejectException(Reject(RejectKind.UNSUPPORTED_PROTOCOL, "Protocollo non riconosciuto"))
            }
            val expiresAt = obj.optLong("expires_at", 0L)
            val now = clockEpochSeconds()
            if (expiresAt <= now) {
                throw QrRejectException(Reject(RejectKind.EXPIRED, "QR scaduto (TTL 2-5 minuti dal momento dell'apertura sul Tower)"))
            }
            val towerInstanceId = obj.optString("tower_instance_id", "")
            val endpoint = obj.optString("endpoint", "")
            val pairingId = obj.optString("pairing_id", "")
            if (towerInstanceId.isEmpty() || pairingId.isEmpty()) {
                throw QrRejectException(Reject(RejectKind.MALFORMED, "Payload QR incompleto"))
            }
            val scope: List<String> = obj.optJSONArray("scope")?.let { arr ->
                (0 until arr.length()).map { arr.optString(it) }.filter { it.isNotEmpty() }
            } ?: emptyList()
            val transportHint = obj.optString("transport_hint", "https")
            val nonceRef = obj.optString("nonce_ref", "")
            if (nonceRef.isEmpty()) {
                throw QrRejectException(Reject(RejectKind.MALFORMED, "nonce_ref mancante"))
            }
            if (nonceRefs.seen(nonceRef)) {
                // Single-use replay rejection (mission section 7): the SAME QR
                // frame cannot open a second pairing session.
                throw QrRejectException(Reject(RejectKind.REPLAYED_NONCE_REF, "QR già consumato (nonce_ref)"))
            }
            nonceRefs.remember(nonceRef)
            return Parsed(proto, towerInstanceId, endpoint, pairingId, scope, expiresAt, transportHint, nonceRef)
        }

        /**
         * B's resolveEndpoint: QR endpoint wins when http(s); the manual
         * endpoint is the ONLY fallback; there is NO invented default.
         */
        fun resolveEndpoint(parsed: Parsed, manualEndpoint: String?): String {
            val fromQr = parsed.endpoint.takeIf { it.startsWith("http") }
            if (fromQr != null) return fromQr
            val manual = manualEndpoint?.trim()?.takeIf { it.startsWith("http") }
            if (manual != null) return manual
            throw QrRejectException(Reject(RejectKind.BAD_ENDPOINT, "Nessun endpoint disponibile (usa l'inserimento manuale)"))
        }

        /** Redaction-safe summary for the confirmation UI (no endpoint, no ids). */
        fun describe(p: Parsed): String =
            "tower=${p.towerInstanceId.take(8)}… proto=${p.protocolVersion} scopes=${p.scope.joinToString(",")} " +
                " expiresIn=${p.expiresAtEpochSeconds - clockEpochSeconds()}s"
    }
}
