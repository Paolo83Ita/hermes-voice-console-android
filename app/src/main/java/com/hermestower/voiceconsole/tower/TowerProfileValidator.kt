package com.hermestower.voiceconsole.tower

/**
 * Deterministic pre-connection validation (GRAPH 2 §9). Pure functions over the
 * profile + presence flags of sensitive values — no network, no clock, fully
 * unit-testable. Mock mode bypasses production authentication requirements;
 * Tailscale mode accepts private Tailscale hostnames/addresses.
 */
object TowerProfileValidator {

    data class Issue(val field: String, val code: String, val message: String) {
        override fun toString(): String = "$field: $message ($code)"
    }

    data class ValidationReport(
        val profileId: String,
        val passed: Boolean,
        val issues: List<Issue>,
    )

    fun validate(
        profile: TowerConnectionProfile,
        secretPresent: Boolean,
        nowIso: String,
    ): ValidationReport {
        val issues = mutableListOf<Issue>()
        val mode = transportModeOf(profile.transportMode)

        if (profile.profileName.isBlank()) issues += Issue("profileName", "BLANK", "Connection name is required.")
        if (profile.profileId.isBlank()) issues += Issue("profileId", "BLANK", "Profile id is required.")

        when (mode) {
            null -> issues += Issue("transportMode", "UNKNOWN_MODE", "Unknown connection method '${profile.transportMode}'.")
            TransportMode.MOCK_SIMULATOR -> {
                // Mock mode requires no server fields (§7: do not require every field for Mock mode).
            }
            else -> {
                val scheme = profile.baseUrl.substringBefore("://", "")
                val expected = when (mode) {
                    TransportMode.HTTPS -> "https"
                    TransportMode.SECURE_WEBSOCKET -> "wss"
                    TransportMode.HTTPS_PLUS_SECURE_WEBSOCKET -> "https"
                    TransportMode.TAILSCALE_PRIVATE -> "http" // http(s) over the Tailscale tunnel; TLS is transport-internal
                    else -> ""
                }
                if (scheme.isEmpty()) {
                    issues += Issue("baseUrl", "SCHEME_MISSING", "Address must start with a scheme such as https:// or wss://.")
                } else if (scheme !in setOf("https", "wss", "http")) {
                    issues += Issue("baseUrl", "SCHEME_UNSUPPORTED", "Scheme '$scheme' is not supported (use https, wss or a Tailscale http address).")
                } else if (mode != TransportMode.TAILSCALE_PRIVATE && scheme != expected && !(mode == TransportMode.SECURE_WEBSOCKET && scheme == "wss")) {
                    issues += Issue("baseUrl", "SCHEME_MISMATCH", "Connection method '$mode' expects a $expected:// address.")
                }
                val afterScheme = profile.baseUrl.substringAfter("://", "")
                if (afterScheme.isBlank()) {
                    issues += Issue("baseUrl", "HOST_MISSING", "Host part is missing.")
                } else {
                    val hostPart = afterScheme.substringBefore('/')
                    val host = hostPart.substringBefore(':')
                    val port = hostPart.substringAfter(':', "")
                    if (host.isBlank()) issues += Issue("baseUrl", "HOST_MISSING", "Host part is missing.")
                    if (host.any { it.isWhitespace() }) issues += Issue("baseUrl", "WHITESPACE", "Address contains whitespace corruption.")
                    if (port.isNotEmpty() && (port.toIntOrNull() !in 1..65535)) {
                        issues += Issue("baseUrl", "PORT_INVALID", "Port must be a number between 1 and 65535.")
                    }
                    if (mode == TransportMode.TAILSCALE_PRIVATE && !isTailscaleHost(host)) {
                        issues += Issue(
                            "baseUrl",
                            "NOT_TAILSCALE",
                            "Tailscale mode expects a Tailscale hostname (…ts.net) or a 100.x.y.z address.",
                        )
                    }
                }
                if (!profile.restBasePath.startsWith("/")) {
                    issues += Issue("restBasePath", "PATH_INVALID", "REST base path must start with '/'.")
                }
                if (!profile.webSocketPath.startsWith("/")) {
                    issues += Issue("webSocketPath", "PATH_INVALID", "WebSocket path must start with '/'.")
                }
                if (!secretPresent) {
                    issues += Issue("secret", "SECRET_REQUIRED", "Pairing code or access token is required for this connection method.")
                }
            }
        }

        if (profile.protocolVersion.isBlank()) issues += Issue("protocolVersion", "PROTOCOL_MISSING", "Protocol version is required.")
        if (profile.requestTimeoutMs !in 1_000..120_000) {
            issues += Issue("requestTimeoutMs", "RANGE", "Request timeout must be between 1 and 120 seconds.")
        }
        if (profile.streamingTimeoutMs !in 5_000..600_000) {
            issues += Issue("streamingTimeoutMs", "RANGE", "Streaming timeout must be between 5 and 600 seconds.")
        }
        if (profile.maximumRetryCount !in 0..5) {
            issues += Issue("maximumRetryCount", "RANGE", "Retry count must be between 0 and 5.")
        }
        if (profile.certificatePolicy == "PINNED") {
            val fp = profile.certificateFingerprintReference
            if (!fp.matches(Regex("^[0-9a-f]{64}$"))) {
                issues += Issue("certificateFingerprintReference", "FINGERPRINT_INVALID", "Pinned fingerprint must be a 64-character lowercase hex SHA-256.")
            }
        }
        return ValidationReport(profile.profileId, issues.isEmpty(), issues)
    }

    private fun isTailscaleHost(host: String): Boolean =
        host.endsWith(".ts.net") ||
            host.startsWith("100.") ||
            Regex("^100\\.[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}$").matches(host)

    fun transportModeOf(raw: String): TransportMode? = TransportMode.entries.firstOrNull { it.name == raw }
}
