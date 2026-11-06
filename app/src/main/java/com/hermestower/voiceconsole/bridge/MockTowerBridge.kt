package com.hermestower.voiceconsole.bridge

import com.hermestower.voiceconsole.core.state.ConnectionStatus
import com.hermestower.voiceconsole.domain.BridgeError
import com.hermestower.voiceconsole.domain.BridgeResult
import com.hermestower.voiceconsole.domain.TowerBridge
import com.hermestower.voiceconsole.domain.TowerEndpoint
import com.hermestower.voiceconsole.locale.ResponseCategory
import com.hermestower.voiceconsole.locale.ResponseCatalog
import com.hermestower.voiceconsole.locale.ResponseLocaleResolver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import java.time.Instant
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

/** Injectable faults for honest UI testing of DISCONNECTED / ERROR / SLOW (FR-21). */
enum class BridgeFault { NONE, DISCONNECT_AT_ACK, ERROR_AFTER_PROGRESS, SLOW_TOWER }

/**
 * Mock TowerBridge: deterministic, fault-injectable, zero network (D-005).
 * Emits the proposed contract frames: ACK -> PROGRESS* -> DELTA* -> FINAL(receiptRef).
 *
 * GRAPH 4.10 CP-G4.10-002: simulated fault MESSAGES are localized via the
 * ResponseCatalog; classifications stay protocol identifiers (diagnostics).
 * Request-path errors follow the frame's explicit conversation language
 * (precedence 1 of the ResponseLocaleResolver chain); handshake errors use
 * the injected application-language provider.
 */
class MockTowerBridge(
    private val simulator: DeterministicTowerSimulator,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
    private val localeTagProvider: () -> String = { "en" },
) : TowerBridge {

    private val fault = AtomicReference(BridgeFault.NONE)
    private val receiptCounter = AtomicInteger(0)

    private val _connection = MutableStateFlow(
        ConnectionStatus(
            status = ConnectionStatus.BridgeStatus.UNCONFIGURED,
            endpointLabel = TowerEndpoint.MOCK.displayLabel,
            tls = ConnectionStatus.TlsStatus.MOCK_NOT_APPLICABLE,
        ),
    )
    override val connection: StateFlow<ConnectionStatus> = _connection.asStateFlow()

    fun setFault(value: BridgeFault) {
        fault.set(value)
        if (value == BridgeFault.DISCONNECT_AT_ACK) {
            _connection.update {
                it.copy(status = ConnectionStatus.BridgeStatus.DISCONNECTED)
            }
        }
    }

    fun currentFault(): BridgeFault = fault.get()

    override fun configure(endpoint: TowerEndpoint) {
        _connection.update { it.copy(endpointLabel = endpoint.displayLabel) }
    }

    override suspend fun handshake(): BridgeResult<Unit> = withContext(dispatcher) {
        val locale = ResponseLocaleResolver.normalize(localeTagProvider()) ?: "en"
        _connection.update { it.copy(status = ConnectionStatus.BridgeStatus.CONNECTING) }
        delay(HANDSHAKE_MS)
        when (fault.get()) {
            BridgeFault.DISCONNECT_AT_ACK -> {
                _connection.update { it.copy(status = ConnectionStatus.BridgeStatus.DISCONNECTED) }
                BridgeResult.Failure(BridgeError.TRANSPORT, ResponseCatalog.text(ResponseCategory.BRIDGE_TRANSPORT_FAILURE, locale), retryable = true)
            }
            BridgeFault.ERROR_AFTER_PROGRESS -> {
                _connection.update { it.copy(status = ConnectionStatus.BridgeStatus.DISCONNECTED) }
                BridgeResult.Failure(BridgeError.TOWER_INTERNAL, ResponseCatalog.text(ResponseCategory.BRIDGE_TOWER_FAILURE, locale), retryable = false)
            }
            else -> {
                _connection.update {
                    it.copy(
                        status = ConnectionStatus.BridgeStatus.CONNECTED,
                        lastHandshakeAt = Instant.now(),
                    )
                }
                BridgeResult.Success(Unit)
            }
        }
    }

    override fun sendRequest(frame: TowerRequestFrame): Flow<TowerEventFrame> = flow {
        val locale = ResponseLocaleResolver.normalize(frame.language) ?: localeTagProvider.let { ResponseLocaleResolver.normalize(it()) ?: "en" }
        emit(ack(frame.requestId))
        when (fault.get()) {
            BridgeFault.DISCONNECT_AT_ACK -> {
                emit(errorFrame(frame.requestId, BridgeError.TRANSPORT, ResponseCatalog.text(ResponseCategory.BRIDGE_TRANSPORT_FAILURE, locale), retryable = true))
                return@flow
            }
            BridgeFault.ERROR_AFTER_PROGRESS -> {
                emit(progress(frame.requestId, "ROUTED"))
                delay(PROGRESS_MS)
                emit(errorFrame(frame.requestId, BridgeError.TOWER_INTERNAL, ResponseCatalog.text(ResponseCategory.BRIDGE_TOWER_FAILURE, locale), retryable = false))
                return@flow
            }
            else -> Unit
        }
        emit(progress(frame.requestId, "ROUTED"))
        delay(PROGRESS_MS)
        emit(progress(frame.requestId, "PLANNING"))
        if (fault.get() == BridgeFault.SLOW_TOWER) delay(SLOW_EXTRA_MS)

        val response = simulator.respond(frame.transcript, locale)
        simulator.streamChunks(response).forEach { chunk ->
            emit(
                TowerEventFrame(
                    type = TowerEventFrame.DELTA,
                    requestId = frame.requestId,
                    deltaText = chunk,
                    timestamp = Instant.now().toString(),
                ),
            )
            delay(DELTA_MS)
        }
        emit(
            TowerEventFrame(
                type = TowerEventFrame.FINAL,
                requestId = frame.requestId,
                finalText = response,
                state = "COMPLETED",
                receiptRef = "rcp-mock-${"%04d".format(receiptCounter.incrementAndGet())}",
                timestamp = Instant.now().toString(),
            ),
        )
    }.flowOn(dispatcher)

    override suspend fun cancel(requestId: String) {
        // Cancellation is flow-collection cancellation in V0.1; recorded for the contract
        // where CANCEL frames travel tower-side in V0.4.
        receiptCounter.incrementAndGet()
    }

    private fun ack(requestId: String) = TowerEventFrame(
        type = TowerEventFrame.ACK,
        requestId = requestId,
        stage = "ACCEPTED",
        timestamp = Instant.now().toString(),
    )

    private fun progress(requestId: String, stage: String) = TowerEventFrame(
        type = TowerEventFrame.PROGRESS,
        requestId = requestId,
        stage = stage,
        timestamp = Instant.now().toString(),
    )

    private fun errorFrame(requestId: String, error: BridgeError, message: String, retryable: Boolean) =
        TowerEventFrame(
            type = TowerEventFrame.ERROR,
            requestId = requestId,
            error = TowerErrorBody(
                classification = error.name,
                retryable = retryable,
                message = message,
            ),
            timestamp = Instant.now().toString(),
        )

    companion object {
        const val HANDSHAKE_MS = 350L
        const val PROGRESS_MS = 450L
        const val DELTA_MS = 90L
        const val SLOW_EXTRA_MS = 2000L
    }
}
