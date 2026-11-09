package com.hermestower.voiceconsole.domain

import com.hermestower.voiceconsole.core.state.ConnectionStatus

/**
 * GRAPH 4.14 §10-§11 — BRIDGE STATE AUTHORITY.
 *
 * FINDING-03 repair: the console used to render "BRIDGE OK" with a cyan dot
 * while the application was in Mock mode — a misleading impression that a real
 * Hermes Tower was connected. The authority derives the bridge TRUTH MODE from
 * the existing connection state (endpoint class + transport status + TLS
 * evidence) and maps it to exactly one indicator role. The UI consumes the
 * role (label string + color token) — it never re-derives truth from raw
 * status.name.
 *
 * Truth modes (§10, never collapsed into "BRIDGE OK"):
 *  - MOCK_BRIDGE_HEALTH        → indicator MOCK (amber/bronze, never cyan)
 *  - LOCAL_BRIDGE_HEALTH       → LOCAL_READY (neutral cyan-gray)
 *  - REAL_TOWER_TRANSPORT      → REACHABLE (amber) — reachable, NOT authenticated
 *  - REAL_TOWER_AUTHENTICATION → CONNECTED (cyan)
 *  - REAL_TOWER_SESSION        → CONNECTED (cyan)
 *  - REAL_TOWER_CAPABILITY_NEGOTIATION → OPERATIVE (cyan + verified mark)
 *  - DISCONNECTED → DISCONNECTED (gray)   FAILED → ERROR (red)
 *
 * Scope freeze honored: this authority READS existing truth sources (the
 * console bridge flow and the real-Tower TLS evidence already surfaced by the
 * Connection contract); it opens no pairing path and no new transport.
 */
enum class BridgeMode {
    MOCK_BRIDGE_HEALTH,
    LOCAL_BRIDGE_HEALTH,
    REAL_TOWER_TRANSPORT,
    REAL_TOWER_AUTHENTICATION,
    REAL_TOWER_SESSION,
    REAL_TOWER_CAPABILITY_NEGOTIATION,
    DISCONNECTED,
    FAILED,
}

/** One indicator role per §11 row; the UI maps it to a localized label + color token. */
enum class BridgeIndicatorRole {
    /** Mock mode — amber/bronze, visually distinct from any real connection. */
    MOCK,
    /** Local bridge process healthy, no real Tower — neutral cyan-gray. */
    LOCAL_READY,
    /** Real transport reachable, not authenticated — amber. */
    REACHABLE,
    /** Authenticated real Tower — cyan. */
    CONNECTED,
    /** Capability negotiation passed — cyan + verified mark. */
    OPERATIVE,
    DISCONNECTED,
    ERROR,
}

/** The compact truth behind the indicator (§12 truth panel rows). */
data class BridgeTruth(
    val mode: BridgeMode,
    val role: BridgeIndicatorRole,
    val isMock: Boolean,
    /** Endpoint class: MOCK_SIMULATOR | LOCAL_BRIDGE | REAL_TOWER | UNKNOWN. */
    val endpointClass: String,
    val transportState: ConnectionStatus.BridgeStatus,
    /** null = authentication not applicable (mock). */
    val authenticated: Boolean?,
    /** null = session not applicable (mock / not authenticated). */
    val sessionEstablished: Boolean?,
    /** Capability negotiation result; false until a real negotiation passes. */
    val capabilityNegotiated: Boolean,
    /** Last successful handshake/check instant (null when never checked). */
    val lastSuccessfulCheck: java.time.Instant?,
    /** Typed limitation shown in the truth panel (never a secret). */
    val typedLimitation: String?,
)

object BridgeModeAuthority {

    const val LIMITATION_MOCK = "BRIDGE_MOCK_SIMULATOR"
    const val LIMITATION_NO_REAL_TOWER = "BRIDGE_NO_REAL_TOWER_CONNECTED"
    const val LIMITATION_UNAUTHENTICATED = "BRIDGE_TRANSPORT_UNAUTHENTICATED"
    const val LIMITATION_NEGOTIATION_PENDING = "BRIDGE_CAPABILITY_NEGOTIATION_PENDING"

    fun resolve(connection: ConnectionStatus): BridgeTruth {
        val isMockEndpoint = connection.endpointLabel.startsWith("mock://") ||
            connection.endpointLabel == TowerEndpoint.MOCK.displayLabel
        return if (isMockEndpoint) {
            // Mock mode: ALWAYS the amber/bronze simulation indicator — never cyan,
            // never "BRIDGE OK", regardless of the mock handshake outcome.
            BridgeTruth(
                mode = BridgeMode.MOCK_BRIDGE_HEALTH,
                role = BridgeIndicatorRole.MOCK,
                isMock = true,
                endpointClass = "MOCK_SIMULATOR",
                transportState = connection.status,
                authenticated = null,
                sessionEstablished = null,
                capabilityNegotiated = false,
                lastSuccessfulCheck = connection.lastHandshakeAt,
                typedLimitation = LIMITATION_MOCK,
            )
        } else {
            when (connection.status) {
                ConnectionStatus.BridgeStatus.CONNECTED -> {
                    val authenticated = connection.tls == ConnectionStatus.TlsStatus.ACTIVE
                    BridgeTruth(
                        mode = if (authenticated) BridgeMode.REAL_TOWER_SESSION
                        else BridgeMode.REAL_TOWER_TRANSPORT,
                        role = if (authenticated) BridgeIndicatorRole.CONNECTED
                        else BridgeIndicatorRole.REACHABLE,
                        isMock = false,
                        endpointClass = "REAL_TOWER",
                        transportState = connection.status,
                        authenticated = authenticated,
                        sessionEstablished = authenticated,
                        capabilityNegotiated = false,
                        lastSuccessfulCheck = connection.lastHandshakeAt,
                        typedLimitation = if (authenticated) LIMITATION_NEGOTIATION_PENDING
                        else LIMITATION_UNAUTHENTICATED,
                    )
                }
                ConnectionStatus.BridgeStatus.CONNECTING -> BridgeTruth(
                    mode = BridgeMode.REAL_TOWER_TRANSPORT,
                    role = BridgeIndicatorRole.REACHABLE,
                    isMock = false,
                    endpointClass = "REAL_TOWER",
                    transportState = connection.status,
                    authenticated = false,
                    sessionEstablished = null,
                    capabilityNegotiated = false,
                    lastSuccessfulCheck = connection.lastHandshakeAt,
                    typedLimitation = LIMITATION_UNAUTHENTICATED,
                )
                ConnectionStatus.BridgeStatus.UNCONFIGURED -> BridgeTruth(
                    mode = BridgeMode.LOCAL_BRIDGE_HEALTH,
                    role = BridgeIndicatorRole.LOCAL_READY,
                    isMock = false,
                    endpointClass = "LOCAL_BRIDGE",
                    transportState = connection.status,
                    authenticated = null,
                    sessionEstablished = null,
                    capabilityNegotiated = false,
                    lastSuccessfulCheck = connection.lastHandshakeAt,
                    typedLimitation = LIMITATION_NO_REAL_TOWER,
                )
                ConnectionStatus.BridgeStatus.DISCONNECTED -> BridgeTruth(
                    mode = BridgeMode.DISCONNECTED,
                    role = BridgeIndicatorRole.DISCONNECTED,
                    isMock = false,
                    endpointClass = "REAL_TOWER",
                    transportState = connection.status,
                    authenticated = null,
                    sessionEstablished = null,
                    capabilityNegotiated = false,
                    lastSuccessfulCheck = null,
                    typedLimitation = LIMITATION_NO_REAL_TOWER,
                )
            }
        }
    }
}
