package com.hermestower.voiceconsole.pairing

/**
 * GRAPH 4 G4-C09 (§16 + Owner delta §9): versioned Tower pairing QR payload
 * client — STRICT parser, typed rejections, redaction-safe diagnostics.
 *
 * AUTHORITY CONTRACT (hard rule): the authoritative final Tower routes and QR
 * payload come from Hermes Tower Graph 1.7. Until that contract exists:
 *  - the parser validates a VERSIONED candidate scheme (hermes-pair/v1);
 *  - NO production endpoint is invented — endpoints are validated by SHAPE,
 *    never called from here;
 *  - pairing with a real Tower is NOT claimed anywhere; the deterministic
 *    mock client is the only executor wired in this graph.
 *
 * Expiry/replay/environment rules are enforced here so the scanner UI and the
 * wizard cannot accept an invalid or expired payload by accident.
 */
data class PairingPayload(
    val schemeVersion: Int,
    val towerId: String,
    val environment: String,
    val issuedEpochMs: Long,
    val expiresEpochMs: Long,
    val nonce: String,
    val challenge: String,
    val pairingEndpoint: String,
    val requestedScopes: List<String>,
    val trustNote: String?,
) {
    companion object {
        const val EXPECTED_SCHEME = "hermes-pair"
        const val EXPECTED_VERSION = 1
        const val EXPECTED_ENVIRONMENT = "local"
        /** A payload is never valid longer than this, even if it claims so. */
        const val MAX_VALIDITY_MS = 10L * 60_000L
    }
}

/** Typed parser rejections (Owner delta §11 test matrix). */
enum class PairingRejectKind {
    MALFORMED,
    UNSUPPORTED_SCHEME,
    UNSUPPORTED_PROTOCOL,
    EXPIRED,
    FUTURE_ISSUED,
    MISSING_NONCE,
    MISSING_CHALLENGE,
    WRONG_ENVIRONMENT,
    REPLAYED_NONCE,
    ALTERED_PAYLOAD,
    BAD_ENDPOINT,
}

data class PairingReject(val kind: PairingRejectKind, val detail: String) {
    /** NEVER includes the raw payload or secrets — redacted by construction. */
    override fun toString(): String = "PairingReject(kind=$kind, detail=$detail)"
}

/** Replay store seam: the app keeps the nonce set for the session lifetime only. */
interface PairingNonceStore {
    fun seen(nonce: String): Boolean
    fun remember(nonce: String)
}

class InMemoryPairingNonceStore : PairingNonceStore {
    private val seen = LinkedHashSet<String>()
    override fun seen(nonce: String): Boolean = synchronized(seen) { nonce in seen }
    override fun remember(nonce: String) { synchronized(seen) { seen += nonce } }
    fun clear() = synchronized(seen) { seen.clear() }
}

/**
 * Strict QR payload parser. Deterministic; clock is injectable for tests.
 * The payload is a compact URI: hermes-pair://v1/<towerId>?<query>
 */
class PairingPayloadParser(
    private val nonceStore: PairingNonceStore = InMemoryPairingNonceStore(),
    private val clockMs: () -> Long = System::currentTimeMillis,
) {

    /** Strict runtime environment this client accepts (Owner device is local-first). */
    var acceptedEnvironment: String = PairingPayload.EXPECTED_ENVIRONMENT

    fun parse(raw: String): Result<PairingPayload> {
        val text = raw.trim()
        if (!text.startsWith(PairingPayload.EXPECTED_SCHEME + "://")) {
            val hasOtherScheme = text.contains("://") && !text.startsWith(PairingPayload.EXPECTED_SCHEME)
            return reject(
                if (hasOtherScheme) PairingRejectKind.UNSUPPORTED_SCHEME else PairingRejectKind.MALFORMED,
                if (hasOtherScheme) "unknown scheme" else "not a pairing URI",
            )
        }
        val rest = text.removePrefix(PairingPayload.EXPECTED_SCHEME + "://")
        val parts = rest.split("?")
        if (parts.size != 2 || parts[0].isBlank()) return reject(PairingRejectKind.MALFORMED, "missing query")
        // Host section is "v<version>/<towerId>" — version and identity travel together.
        val hostParts = parts[0].split("/", limit = 2)
        if (hostParts.size != 2 || hostParts[1].isBlank()) {
            return reject(PairingRejectKind.MALFORMED, "missing tower id in host")
        }
        if (!hostParts[0].startsWith("v")) return reject(PairingRejectKind.MALFORMED, "no protocol version")
        val version = hostParts[0].removePrefix("v").toIntOrNull()
            ?: return reject(PairingRejectKind.UNSUPPORTED_PROTOCOL, "non-numeric version")
        if (version != PairingPayload.EXPECTED_VERSION) {
            return reject(PairingRejectKind.UNSUPPORTED_PROTOCOL, "protocol v$version not supported")
        }
        val towerId = hostParts[1]
        val query = parts[1]
        if (query.length > MAX_QUERY_LENGTH) return reject(PairingRejectKind.ALTERED_PAYLOAD, "oversized query")
        val params: Map<String, String> = query.split("&")
            .filter { it.contains('=') }
            .associate { p ->
                val kv = p.split("=", limit = 2)
                // URLDecoder.decode(String, Charset) exists only from API 33
                // (lint NewApi on minSdk 30); the String-charset overload exists
                // since API 1 and UTF-8 is always supported (never throws here).
                urlDecodeUtf8(kv[0]) to urlDecodeUtf8(kv[1])
            }

        val environment = params["env"]?.takeIf { it.isNotBlank() }
            ?: return reject(PairingRejectKind.WRONG_ENVIRONMENT, "missing environment")
        if (environment != acceptedEnvironment) {
            return reject(PairingRejectKind.WRONG_ENVIRONMENT, "environment mismatch")
        }
        val issued = params["iat"]?.toLongOrNull()
            ?: return reject(PairingRejectKind.MALFORMED, "missing issued time")
        val expires = params["exp"]?.toLongOrNull()
            ?: return reject(PairingRejectKind.MALFORMED, "missing expiry")
        val now = clockMs()
        if (issued > now + FUTURE_TOLERANCE_MS) {
            return reject(PairingRejectKind.FUTURE_ISSUED, "issued in the future")
        }
        if (expires <= now) {
            return reject(PairingRejectKind.EXPIRED, "expired ${now - expires} ms ago")
        }
        if (expires - issued > PairingPayload.MAX_VALIDITY_MS) {
            return reject(PairingRejectKind.ALTERED_PAYLOAD, "validity window exceeds policy")
        }
        val nonce = params["n"]?.takeIf { it.isNotBlank() }
            ?: return reject(PairingRejectKind.MISSING_NONCE, "missing nonce")
        if (nonce.length < MIN_NONCE_LENGTH) {
            return reject(PairingRejectKind.ALTERED_PAYLOAD, "nonce too short")
        }
        if (nonceStore.seen(nonce)) {
            return reject(PairingRejectKind.REPLAYED_NONCE, "nonce already consumed")
        }
        val challenge = params["c"]?.takeIf { it.isNotBlank() }
            ?: return reject(PairingRejectKind.MISSING_CHALLENGE, "missing challenge")
        val endpoint = params["ep"]?.takeIf { it.isNotBlank() }
            ?: return reject(PairingRejectKind.BAD_ENDPOINT, "missing pairing endpoint")
        if (!endpoint.startsWith("https://") && !endpoint.startsWith("http://")) {
            return reject(PairingRejectKind.BAD_ENDPOINT, "endpoint scheme not http(s)")
        }
        // Scope list is optional and free-form until Tower Graph 1.7 fixes the set.
        val scopes = params["s"]?.split(',')?.filter { it.isNotBlank() }.orEmpty()
        val payload = PairingPayload(
            schemeVersion = version,
            towerId = towerId,
            environment = environment,
            issuedEpochMs = issued,
            expiresEpochMs = expires,
            nonce = nonce,
            challenge = challenge,
            pairingEndpoint = endpoint,
            requestedScopes = scopes,
            trustNote = params["tn"],
        )
        nonceStore.remember(nonce)
        return Result.success(payload)
    }

    /** Redaction-safe preview for the confirmation screen (no endpoint, no challenge). */
    fun describe(p: PairingPayload): String =
        "tower=${p.towerId} env=${p.environment} scopes=${p.requestedScopes.joinToString(",")}" +
            " expiresIn=${p.expiresEpochMs - clockMs()}ms"

    private fun reject(kind: PairingRejectKind, detail: String): Result<PairingPayload> =
        Result.failure(PairingRejectException(PairingReject(kind, detail)))

    companion object {
        const val FUTURE_TOLERANCE_MS = 30_000L
        const val MIN_NONCE_LENGTH = 8
        const val MAX_QUERY_LENGTH = 512
    }
}

class PairingRejectException(val reject: PairingReject) : Exception(reject.toString())

/** API-30-compatible UTF-8 percent-decoding (the Charset overload is API 33+).
 *  UTF-8 is guaranteed present on every Android runtime: decoding never throws. */
private fun urlDecodeUtf8(value: String): String =
    try {
        java.net.URLDecoder.decode(value, "UTF-8")
    } catch (_: java.io.UnsupportedEncodingException) {
        value // unreachable on Android; truthful passthrough keeps the parser total
    }

/**
 * Deterministic mock pairing client (§16): versioned client interface ONLY.
 * NO network call, NO invented production endpoint. Qualification with a real
 * Tower belongs to Tower Graph 1.7.
 */
interface TowerPairingClient {
    /** Runs the pairing handshake against the payload; typed result. */
    suspend fun pair(payload: PairingPayload, confirmPhrase: String): PairingResult
}

sealed class PairingResult {
    data class Success(val sessionId: String, val matchPhrase: String) : PairingResult()
    data class Failure(val kind: PairingRejectKind, val detail: String) : PairingResult()
}

class MockTowerPairingClient(
    private val clockMs: () -> Long = System::currentTimeMillis,
) : TowerPairingClient {
    var deterministicSessionBase: Long = 7000L

    override suspend fun pair(payload: PairingPayload, confirmPhrase: String): PairingResult {
        // Deterministic derivation, no randomness, no I/O.
        val session = "pair-mock-${deterministicSessionBase + (payload.nonce.hashCode() and 0xff)}"
        val phrase = matchPhraseFor(payload, confirmPhrase)
        return PairingResult.Success(sessionId = session, matchPhrase = phrase)
    }

    /** Four-character deterministic confirmation phrase (shown on BOTH sides in real flow). */
    private fun matchPhraseFor(payload: PairingPayload, confirm: String): String {
        val h = (payload.towerId + confirm + payload.nonce).hashCode()
        val alphabet = "ABCDEFGHJKMNPQRSTUVWXYZ"
        return (0 until 4).map { alphabet[Math.abs((h shr (it * 4)) and 0xf) % alphabet.length] }
            .joinToString("-")
    }
}
