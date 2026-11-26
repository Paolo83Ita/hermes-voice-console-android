package com.hermestower.voiceconsole.pairing

import org.json.JSONArray
import org.json.JSONObject
import java.util.Base64

/**
 * ANDROID REPAIR CP-005 — pairing envelope v3 parser (mission section 2).
 *
 * Parses BOTH live schemas with one authority:
 *  - v2 (today's live Tower QR): single `endpoint` field -> ONE candidate
 *    (transport class inferred by [classifyLegacyEndpoint]); every v2 semantic
 *    (forbidden-field gate, version/protocol/expiry/nonce rules) is the
 *    existing QrPayload behavior, re-implemented 1:1 here so the envelope
 *    layer is self-contained;
 *  - v3 (the requested Tower delta, HR-A4R-PHYS-002 DELTA-A): multi-candidate
 *    envelope per PAIRING_ENVELOPE_V3_SCHEMA.json.
 *
 * SECURITY INVARIANTS (unchanged from v2, enforced BEFORE anything else):
 *  - forbidden-secret gate fails CLOSED (no secrets in the QR, ever);
 *  - expiry checked against the injected clock BEFORE any network use;
 *  - nonce reference single-use per [QrPayload.NonceRefStore];
 *  - the signature part is carried, not verified client-side (the Tower proves
 *    possession at claim time) — no client-side bypass is created.
 *
 * Pure Kotlin + org.json: deterministic under the JVM suite.
 */
object PairingEnvelope {

    const val SUPPORTED_VERSION = 3
    const val SUPPORTED_LEGACY_VERSION = 2
    const val PROTOCOL_PREFIX = "voice-console/"
    const val CLOCK_SKEW_TOLERANCE_S = 300L

    /** Typed rejections — same vocabulary as QrPayload (UI mapping preserved). */
    enum class RejectKind {
        MALFORMED,
        FORBIDDEN_FIELD,
        UNSUPPORTED_VERSION,
        UNSUPPORTED_PROTOCOL,
        EXPIRED,
        ISSUED_IN_FUTURE,
        BAD_ENDPOINT,
        REPLAYED_NONCE_REF,
    }

    data class Reject(val kind: RejectKind, val detail: String) {
        override fun toString(): String = "EnvelopeReject(kind=$kind, detail=$detail)"
    }

    class EnvelopeRejectException(val reject: Reject) : Exception(reject.toString())

    /** The unified parsed envelope (v2 and v3 both land here). */
    data class Parsed(
        val schemaVersion: Int,
        val protocolVersion: String,
        val towerPublicId: String,
        val challengeId: String,
        val nonceRef: String,
        val issuedAtEpochSeconds: Long,
        val expiresAtEpochSeconds: Long,
        val scope: List<String>,
        val candidates: List<EndpointCandidate>,
        val transportCandidates: List<String>,
        val certSha256Reference: String?,
        val certSansReference: List<String>,
        val capabilityNegotiationVersion: String?,
        val signaturePart: String,
        val phraseWordCount: Int,
        val phraseLanguage: String,
    )

    val FORBIDDEN_FIELDS: List<String> = listOf(
        "password", "api_key", "apikey", "secret", "token_secret",
        "private_key", "credential", "bearer", "refresh_token", "access_token",
        "console_token", "claim_key",
    )

    class Parser(
        private val nonceRefs: QrPayload.NonceRefStore = QrPayload.InMemoryNonceRefStore(),
        private val clockEpochSeconds: () -> Long = { System.currentTimeMillis() / 1000L },
    ) {

        fun parse(qrText: String): Parsed {
            val text = qrText.trim()
            if (text.isEmpty()) throw EnvelopeRejectException(Reject(RejectKind.MALFORMED, "QR vuoto"))
            val parts = text.split(".")
            if (parts.size != 2 || parts[0].isEmpty() || parts[1].isEmpty()) {
                throw EnvelopeRejectException(Reject(RejectKind.MALFORMED, "Formato QR non valido (atteso payload.firma)"))
            }
            val payloadJson = try {
                String(Base64.getUrlDecoder().decode(parts[0]), Charsets.UTF_8)
            } catch (_: IllegalArgumentException) {
                throw EnvelopeRejectException(Reject(RejectKind.MALFORMED, "Payload QR non decodificabile"))
            }
            val obj = try {
                JSONObject(payloadJson)
            } catch (_: Exception) {
                throw EnvelopeRejectException(Reject(RejectKind.MALFORMED, "Payload QR non decodificabile"))
            }
            // Forbidden-secret gate FIRST (fail closed).
            for (banned in FORBIDDEN_FIELDS) {
                if (obj.has(banned)) {
                    throw EnvelopeRejectException(Reject(RejectKind.FORBIDDEN_FIELD, "QR contiene un campo vietato"))
                }
            }
            val v = obj.optInt("v", 0)
            if (v != SUPPORTED_VERSION && v != SUPPORTED_LEGACY_VERSION) {
                throw EnvelopeRejectException(Reject(RejectKind.UNSUPPORTED_VERSION, "Versione protocollo QR non supportata: $v"))
            }
            val proto = obj.optString("protocol_version", "")
            if (!proto.startsWith(PROTOCOL_PREFIX)) {
                throw EnvelopeRejectException(Reject(RejectKind.UNSUPPORTED_PROTOCOL, "Protocollo non riconosciuto"))
            }
            val now = clockEpochSeconds()
            val issuedAt = if (v == SUPPORTED_VERSION) obj.optLong("issued_at", 0L) else 0L
            val expiresAt = obj.optLong("expires_at", 0L)
            if (expiresAt <= now) {
                throw EnvelopeRejectException(Reject(RejectKind.EXPIRED, "QR scaduto (TTL 2-5 minuti dall'apertura sul Tower)"))
            }
            if (v == SUPPORTED_VERSION && issuedAt > 0 && issuedAt - now > CLOCK_SKEW_TOLERANCE_S) {
                throw EnvelopeRejectException(Reject(RejectKind.ISSUED_IN_FUTURE, "QR emesso con orario futuro oltre la tolleranza di clock"))
            }
            val towerId = obj.optString("tower_public_id", obj.optString("tower_instance_id", ""))
            val challengeId = obj.optString("challenge_id", obj.optString("pairing_id", ""))
            if (towerId.isEmpty() || challengeId.isEmpty()) {
                throw EnvelopeRejectException(Reject(RejectKind.MALFORMED, "Payload QR incompleto"))
            }
            val nonceRef = obj.optString("nonce_hash_or_reference", obj.optString("nonce_ref", ""))
            if (nonceRef.isEmpty()) {
                throw EnvelopeRejectException(Reject(RejectKind.MALFORMED, "nonce_ref mancante"))
            }
            if (nonceRefs.seen(nonceRef)) {
                throw EnvelopeRejectException(Reject(RejectKind.REPLAYED_NONCE_REF, "QR già consumato (nonce_ref)"))
            }
            nonceRefs.remember(nonceRef)

            val scope: List<String> = obj.optJSONArray("scope")?.let(::stringArray) ?: obj.optJSONArray("scope_request")?.let(::stringArray) ?: emptyList()
            val candidates = when (v) {
                SUPPORTED_VERSION -> parseV3Candidates(obj, expiresAt)
                else -> parseLegacyCandidate(obj, expiresAt)
            }
            if (candidates.isEmpty()) {
                throw EnvelopeRejectException(Reject(RejectKind.BAD_ENDPOINT, "Nessun endpoint nel QR (usa l'inserimento manuale)"))
            }
            val transportCandidates = obj.optJSONArray("transport_candidates")?.let(::stringArray) ?: listOf("HTTPS_POST", "HTTPS_GET_POLL")
            val trustMeta = obj.optJSONObject("certificate_trust_metadata")
            val phraseRef = obj.optJSONObject("verification_phrase_reference")
            return Parsed(
                schemaVersion = v,
                protocolVersion = proto,
                towerPublicId = towerId,
                challengeId = challengeId,
                nonceRef = nonceRef,
                issuedAtEpochSeconds = issuedAt,
                expiresAtEpochSeconds = expiresAt,
                scope = scope,
                candidates = candidates,
                transportCandidates = transportCandidates,
                certSha256Reference = trustMeta?.optString("public_cert_sha256", null),
                certSansReference = trustMeta?.optJSONArray("public_cert_sans")?.let(::stringArray) ?: emptyList(),
                capabilityNegotiationVersion = obj.optString("capability_negotiation_version", "").takeIf { it.isNotEmpty() },
                signaturePart = parts[1],
                phraseWordCount = phraseRef?.optInt("word_count", 3) ?: 3,
                phraseLanguage = phraseRef?.optString("language", "it") ?: "it",
            )
        }

        private fun stringArray(arr: JSONArray): List<String> =
            (0 until arr.length()).map { arr.optString(it) }.filter { it.isNotEmpty() }

        private fun parseV3Candidates(obj: JSONObject, envelopeExpiry: Long): List<EndpointCandidate> {
            val arr = obj.optJSONArray("endpoint_candidates") ?: return emptyList()
            val out = ArrayList<EndpointCandidate>(arr.length())
            for (i in 0 until arr.length()) {
                val c = arr.optJSONObject(i) ?: continue
                val cls = when (c.optString("transport_class", "")) {
                    "LAN_MDNS" -> TransportClass.LAN_MDNS
                    "LAN_IP" -> TransportClass.LAN_IP
                    "TAILSCALE_MAGICDNS" -> TransportClass.TAILSCALE_MAGICDNS
                    "QUALIFIED_PUBLIC_ENDPOINT" -> TransportClass.QUALIFIED_PUBLIC_ENDPOINT
                    else -> null
                } ?: continue
                val url = c.optString("url_or_host", "")
                if (url.isEmpty()) continue
                val netReq = when (c.optString("network_requirement", "")) {
                    "LAN_SAME_SUBNET" -> NetworkRequirement.LAN_SAME_SUBNET
                    "TAILNET_MEMBERSHIP" -> NetworkRequirement.TAILNET_MEMBERSHIP
                    "INTERNET_ANY" -> NetworkRequirement.INTERNET_ANY
                    else -> if (cls == TransportClass.QUALIFIED_PUBLIC_ENDPOINT) NetworkRequirement.INTERNET_ANY else NetworkRequirement.LAN_SAME_SUBNET
                }
                val tlsReq = when (c.optString("tls_requirement", "")) {
                    "CA_VALIDATED" -> TlsRequirement.CA_VALIDATED
                    "TAILSCALE_LE" -> TlsRequirement.TAILSCALE_LE
                    "LAN_SELF_SIGNED_EXPECTED" -> TlsRequirement.LAN_SELF_SIGNED_EXPECTED
                    else -> inferTlsRequirement(cls)
                }
                out += EndpointCandidate(
                    transportClass = cls,
                    urlOrHost = url,
                    priority = c.optInt("priority", 100),
                    networkRequirement = netReq,
                    tlsRequirement = tlsReq,
                    expiryEpochSeconds = c.optLong("expiry", envelopeExpiry).coerceAtMost(envelopeExpiry),
                    source = EndpointCandidate.CandidateSource.TOWER_QR,
                    qualified = c.optString("qualification_state", "QUALIFIED") == "QUALIFIED",
                )
            }
            return out
        }

        /** v2: the single endpoint becomes one candidate with inferred class. */
        private fun parseLegacyCandidate(obj: JSONObject, envelopeExpiry: Long): List<EndpointCandidate> {
            val endpoint = obj.optString("endpoint", "")
            if (endpoint.isEmpty()) return emptyList()
            val cls = classifyLegacyEndpoint(endpoint)
            return listOf(
                EndpointCandidate(
                    transportClass = cls,
                    urlOrHost = endpoint,
                    priority = 10,
                    networkRequirement = when (cls) {
                        TransportClass.QUALIFIED_PUBLIC_ENDPOINT -> NetworkRequirement.INTERNET_ANY
                        TransportClass.TAILSCALE_MAGICDNS -> NetworkRequirement.TAILNET_MEMBERSHIP
                        else -> NetworkRequirement.LAN_SAME_SUBNET
                    },
                    tlsRequirement = inferTlsRequirement(cls),
                    expiryEpochSeconds = envelopeExpiry,
                    source = EndpointCandidate.CandidateSource.TOWER_QR,
                    qualified = true,
                ),
            )
        }

        /** Redaction-safe summary (no endpoint, no ids). */
        fun describe(p: Parsed): String =
            "tower=${p.towerPublicId.take(8)}… proto=${p.protocolVersion} v${p.schemaVersion} " +
                "candidates=${p.candidates.joinToString(",") { it.transportClass.name }} " +
                "expiresIn=${p.expiresAtEpochSeconds - clockEpochSeconds()}s"
    }
}
