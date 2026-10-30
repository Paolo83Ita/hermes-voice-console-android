package com.hermestower.voiceconsole.bridge

import kotlinx.serialization.Serializable

/**
 * Proposed versioned contract (docs/TOWER_BRIDGE_PROPOSED_CONTRACT.md).
 * These models implement the frame schemas; the mock bridge emits them.
 * Payloads carry no secrets and no personal data beyond the transcript (SEC section 3).
 */
object TowerProtocol {
    const val VERSION = "hermes.console.v0.1"
}

@Serializable
data class LocalRoutingHint(
    val decision: String,
    val reason: String,
    val candidateIntent: String? = null,
    val consequential: Boolean = false,
)

@Serializable
data class TowerRequestFrame(
    val protocol: String = TowerProtocol.VERSION,
    val type: String = "REQUEST",
    val deviceId: String,
    val userIdRef: String,
    val sessionId: String,
    val conversationId: String,
    val requestId: String,
    val transcript: String,
    val language: String = "it-IT",
    val confidence: Float,
    val requestedMode: String,
    val effectiveMode: String,
    val localRouting: LocalRoutingHint? = null,
    val timestamp: String,
)

@Serializable
data class TowerErrorBody(
    val classification: String,
    val retryable: Boolean,
    val message: String,
)

@Serializable
data class TowerEventFrame(
    val protocol: String = TowerProtocol.VERSION,
    val type: String,
    val requestId: String? = null,
    val stage: String? = null,
    val deltaText: String? = null,
    val finalText: String? = null,
    val state: String? = null,
    val receiptRef: String? = null,
    val error: TowerErrorBody? = null,
    val timestamp: String,
) {
    companion object {
        const val HELLO = "HELLO"
        const val SESSION_OPEN = "SESSION_OPEN"
        const val REQUEST = "REQUEST"
        const val ACK = "ACK"
        const val PROGRESS = "PROGRESS"
        const val DELTA = "DELTA"
        const val FINAL = "FINAL"
        const val CANCEL = "CANCEL"
        const val ERROR = "ERROR"
        const val RECEIPT_REF = "RECEIPT_REF"
    }
}

@Serializable
data class TowerHelloFrame(
    val protocol: String = TowerProtocol.VERSION,
    val type: String = "HELLO",
    val deviceId: String,
    val deviceModel: String,
    val appVersion: String,
    val capabilities: List<String>,
    val protocolVersion: String = TowerProtocol.VERSION,
    val timestamp: String,
)
