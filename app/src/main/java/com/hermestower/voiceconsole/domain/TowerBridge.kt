package com.hermestower.voiceconsole.domain

import com.hermestower.voiceconsole.core.state.ConnectionStatus
import kotlinx.coroutines.flow.StateFlow

/**
 * TowerBridge seam (FR-20/21). UI and pipeline code never touch a transport.
 * V0.1 ships MockTowerBridge only; the real OkHttp/WSS adapter is V0.4 (D-005).
 */
interface TowerBridge {
    val connection: StateFlow<ConnectionStatus>

    fun configure(endpoint: TowerEndpoint)

    /** Connection test (Connection screen). Never throws; returns a typed result. */
    suspend fun handshake(): BridgeResult<Unit>

    /**
     * Sends one REQUEST frame and emits tower events until FINAL/ERROR.
     * Cold flow: collection drives the exchange; cancelling collection cancels the request.
     */
    fun sendRequest(frame: TowerRequestFrame): kotlinx.coroutines.flow.Flow<TowerEventFrame>

    suspend fun cancel(requestId: String)
}

data class TowerEndpoint(
    val displayLabel: String,
    val url: String? = null,
) {
    companion object {
        val MOCK = TowerEndpoint(displayLabel = "mock://tower.simulator")
    }
}

sealed class BridgeResult<out T> {
    data class Success<T>(val value: T) : BridgeResult<T>()
    data class Failure(val error: BridgeError, val message: String, val retryable: Boolean) :
        BridgeResult<Nothing>()
}

/** Closed error taxonomy (contract §3.6). */
enum class BridgeError {
    AUTH, TRANSPORT, PROTOCOL, CAPACITY, TOWER_INTERNAL, CANCELLED, INVALID_REQUEST
}
