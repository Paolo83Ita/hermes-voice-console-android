package com.hermestower.voiceconsole.pairing

/**
 * ANDROID REPAIR CP-005 — endpoint candidate model (mission section 2).
 *
 * The typed vocabulary of the PAIRING_ENDPOINT_RESOLUTION_CONTRACT: transport
 * classes, network requirements, TLS requirements, per-candidate evidence and
 * the typed transport failure catalog. Pure Kotlin (org.json-free), fully
 * deterministic under the JVM suite.
 *
 * BINDING: PAIRING_ENVELOPE_V3_SCHEMA.json (endpoint_candidates item fields),
 * PAIRING_TYPED_FAILURE_CATALOG.json (failure codes), PAIRING_TRUST_POLICY.json
 * (TLS rules TRUST-1..TRUST-7).
 */

/** Transport class of an endpoint candidate (MANUAL_ADVANCED never travels in a QR). */
enum class TransportClass {
    LAN_MDNS,
    LAN_IP,
    TAILSCALE_MAGICDNS,
    QUALIFIED_PUBLIC_ENDPOINT,
    MANUAL_ADVANCED,
}

/** What network context a candidate needs to be reachable at all. */
enum class NetworkRequirement {
    LAN_SAME_SUBNET,
    TAILNET_MEMBERSHIP,
    INTERNET_ANY,
}

/** TLS posture of a candidate (see PAIRING_TRUST_POLICY rules TRUST-1..4). */
enum class TlsRequirement {
    /** Platform default trust store + hostname verification, always on. */
    CA_VALIDATED,
    /** Tailscale-issued Let's Encrypt cert (*.ts.net) — standard CA validation. */
    TAILSCALE_LE,
    /** Self-signed LAN cert — FAILS CLOSED unless the Owner enabled the explicit dev-trust path. */
    LAN_SELF_SIGNED_EXPECTED,
}

/** Device network context (ConnectivityManager classification, seam-injected). */
enum class NetworkContextType { WIFI, CELLULAR, VPN, ETHERNET, NONE }

/**
 * A single endpoint candidate. Constructed from envelope v3 entries, from a v2
 * single endpoint (class inferred), or from Owner manual entry (MANUAL_ADVANCED).
 */
data class EndpointCandidate(
    val transportClass: TransportClass,
    /** Full https URL (or bare host for LAN_MDNS, which is NOT socket-resolvable). */
    val urlOrHost: String,
    val priority: Int,
    val networkRequirement: NetworkRequirement,
    val tlsRequirement: TlsRequirement,
    /** Candidate-level expiry, epoch seconds (<= envelope expires_at). */
    val expiryEpochSeconds: Long,
    val source: CandidateSource,
    val qualified: Boolean,
) {
    enum class CandidateSource { TOWER_QR, MANUAL_ADVANCED, LAN_DISCOVERY, TAILSCALE_GUIDED }

    /** True when the URL is a usable https URL (LAN_MDNS .local hosts are not). */
    val isHttpsUrl: Boolean
        get() = urlOrHost.startsWith("https://", ignoreCase = true)

    /** Host-only view for logs/exports (never the full URL with path). */
    fun hostView(): String = runCatching {
        val noScheme = urlOrHost.substringAfter("://")
        noScheme.substringBefore('/').substringBefore(':')
    }.getOrDefault(urlOrHost.take(24))

    /** Rule 2: malformed candidates are recorded, never attempted. */
    val isMalformed: Boolean
        get() = when (transportClass) {
            TransportClass.LAN_MDNS -> urlOrHost.isBlank() || !urlOrHost.endsWith(".local")
            else -> !isHttpsUrl || hostView().isBlank()
        }
}

/** Rule 4 eligibility decision: can this candidate be ATTEMPTED on this network? */
fun EndpointCandidate.isEligibleOnNetwork(network: NetworkContextType): Boolean = when (networkRequirement) {
    NetworkRequirement.INTERNET_ANY -> network != NetworkContextType.NONE
    NetworkRequirement.LAN_SAME_SUBNET -> network == NetworkContextType.WIFI || network == NetworkContextType.ETHERNET
    NetworkRequirement.TAILNET_MEMBERSHIP -> network == NetworkContextType.VPN || network == NetworkContextType.WIFI || network == NetworkContextType.ETHERNET
} && network != NetworkContextType.NONE

/**
 * The typed transport failure catalog (mission section 3 / PAIRING_TYPED_FAILURE_CATALOG).
 * Every failure the resolver and the HTTP layer can produce. NEVER a raw
 * IOException on a user-facing path.
 */
enum class TransportFailureCode {
    OFFLINE_NO_NETWORK,
    NO_ELIGIBLE_ENDPOINT,
    DNS_RESOLUTION_FAILED,
    MDNS_UNAVAILABLE,
    LAN_ROUTE_UNAVAILABLE,
    CELLULAR_CANNOT_REACH_LAN_ENDPOINT,
    TCP_CONNECTION_FAILED,
    TLS_CA_UNTRUSTED,
    TLS_HOSTNAME_MISMATCH,
    TLS_CERTIFICATE_EXPIRED,
    TLS_HANDSHAKE_FAILED,
    CLEARTEXT_BLOCKED,
    HTTP_PROTOCOL_ERROR,
    PAIRING_ROUTE_MISSING,
    PAIRING_ROUTE_REJECTED,
    BUDGET_EXHAUSTED,
}

/** Typed transport exception (the ONLY transport failure type crossing layers). */
class TypedTransportException(
    val code: TransportFailureCode,
    val httpStatus: Int? = null,
    val inBandError: String? = null,
    detail: String,
    cause: Throwable? = null,
) : Exception(detail, cause) {
    override fun toString(): String =
        "TypedTransport(code=$code, http=$httpStatus, inBand=$inBandError)"
}

/** Per-candidate resolution evidence (rule 7; feeds Diagnostics + export). No secrets. */
data class CandidateEvidence(
    val transportClass: TransportClass,
    val hostView: String,
    val priority: Int,
    val status: CandidateStatus,
    val typedCode: TransportFailureCode? = null,
    val detail: String? = null,
    val latencyMs: Long? = null,
    val tlsVerified: Boolean? = null,
    val httpStatus: Int? = null,
) {
    enum class CandidateStatus {
        SELECTED,
        ATTEMPTED_FAILED,
        SKIPPED_NOT_ELIGIBLE,
        SKIPPED_EXPIRED,
        SKIPPED_MALFORMED,
        SKIPPED_UNQUALIFIED,
        PENDING,
    }
}

/** Result of one resolution round (rule 10: typed terminal failure, never null). */
sealed class EndpointResolutionResult {
    /** One verified candidate proceeds to claim. */
    data class Selected(
        val candidate: EndpointCandidate,
        val evidence: List<CandidateEvidence>,
    ) : EndpointResolutionResult()

    /** Every eligible candidate exhausted / all skipped. */
    data class Failed(
        val primaryCode: TransportFailureCode,
        val evidence: List<CandidateEvidence>,
    ) : EndpointResolutionResult()
}

/**
 * Classifies a v2 single endpoint into the v3 transport class vocabulary
 * (compatibility: v2 payloads produce one candidate).
 */
fun classifyLegacyEndpoint(endpoint: String): TransportClass = when {
    endpoint.endsWith(".local", ignoreCase = true) -> TransportClass.LAN_MDNS
    endpoint.contains(".ts.net", ignoreCase = true) -> TransportClass.TAILSCALE_MAGICDNS
    isPrivateLanHost(endpoint) -> TransportClass.LAN_IP
    else -> TransportClass.QUALIFIED_PUBLIC_ENDPOINT
}

/** RFC1918 / link-local host check (best-effort, for CLASSIFICATION only). */
fun isPrivateLanHost(urlOrHost: String): Boolean {
    val host = urlOrHost.substringAfter("://").substringBefore('/').substringBefore(':')
    val ipv4 = Regex("^(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})$").find(host) ?: return false
    val (a, b) = ipv4.destructured
    val ai = a.toIntOrNull() ?: return false
    val bi = b.toIntOrNull() ?: return false
    return ai == 10 || (ai == 172 && bi in 16..31) || (ai == 192 && bi == 168) || (ai == 169 && bi == 254)
}

/** TLS requirement inference for legacy endpoints (v2 has no TLS metadata). */
fun inferTlsRequirement(transportClass: TransportClass): TlsRequirement = when (transportClass) {
    TransportClass.LAN_MDNS, TransportClass.LAN_IP -> TlsRequirement.LAN_SELF_SIGNED_EXPECTED
    else -> TlsRequirement.CA_VALIDATED
}
