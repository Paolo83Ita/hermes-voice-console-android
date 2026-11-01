package com.hermestower.voiceconsole.tower

import kotlinx.serialization.Serializable

/**
 * Typed connection profile (GRAPH 2 §8). Sensitive values (pairing code / access
 * token) are NEVER fields of this class: they live in the SecretVault, keyed by
 * profileId. This file/class is safe to log, export and persist.
 */
@Serializable
data class TowerConnectionProfile(
    val profileId: String,
    val profileName: String,
    val transportMode: String,
    val baseUrl: String,
    val restBasePath: String = "/tower/api",
    val webSocketPath: String = "/tower/ws",
    val protocolVersion: String = "hermes.console.v0.1",
    val userReference: String = "",
    val deviceAlias: String = "",
    val preferredLanguage: String = "it-IT",
    val requestedMode: String = "TOWER_FIRST",
    val connectAutomatically: Boolean = false,
    val useTailscale: Boolean = false,
    val requestTimeoutMs: Int = 10_000,
    val streamingTimeoutMs: Int = 60_000,
    val maximumRetryCount: Int = 2,
    val queuedRequestPolicy: String = "QUEUE_NON_CONSEQUENTIAL",
    val consequentialConfirmationRequired: Boolean = true,
    val certificatePolicy: String = "SYSTEM",
    val certificateFingerprintReference: String = "",
    val diagnosticsVerbosity: String = "REDACTED_SUMMARY",
    val localTowerRoutingThreshold: Float = 0.55f,
    val createdAt: String,
    val updatedAt: String,
    val lastValidationAt: String? = null,
    val lastConnectionAt: String? = null,
    val lastConnectionResult: String? = null,
) {
    companion object {
        fun emptyMock(profileId: String = "profile-mock-default") = TowerConnectionProfile(
            profileId = profileId,
            profileName = "Hermes Tower (simulator)",
            transportMode = TransportMode.MOCK_SIMULATOR.name,
            baseUrl = "mock://tower.simulator",
            createdAt = "",
            updatedAt = "",
        )
    }
}

/** Connection methods (GRAPH 2 §7). */
enum class TransportMode(val displayLabel: String, val requiresServer: Boolean) {
    HTTPS("HTTPS", true),
    SECURE_WEBSOCKET("Secure WebSocket", true),
    HTTPS_PLUS_SECURE_WEBSOCKET("HTTPS + secure WebSocket", true),
    TAILSCALE_PRIVATE("Tailscale private address", true),
    MOCK_SIMULATOR("Mock simulator", false),
}

/** Configuration states (GRAPH 2 §8). */
enum class ConnectionConfigState(val displayLabel: String) {
    EMPTY("No configuration"),
    DRAFT("Draft"),
    INVALID("Invalid"),
    VALIDATED("Validated"),
    PAIRING_REQUIRED("Pairing required"),
    PAIRED("Paired"),
    CONNECTION_TEST_PENDING("Connection test pending"),
    CONNECTED("Connected"),
    DISCONNECTED("Disconnected"),
    DEGRADED("Degraded"),
    AUTHENTICATION_FAILED("Authentication failed"),
    PROTOCOL_MISMATCH("Protocol mismatch"),
    CERTIFICATE_ERROR("Certificate error"),
    TIMEOUT("Timed out"),
    ROLLBACK_AVAILABLE("Previous configuration available"),
}
