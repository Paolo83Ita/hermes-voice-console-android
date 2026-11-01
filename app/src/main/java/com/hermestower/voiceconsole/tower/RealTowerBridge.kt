package com.hermestower.voiceconsole.tower

import com.hermestower.voiceconsole.bridge.TowerErrorBody
import com.hermestower.voiceconsole.bridge.TowerEventFrame
import com.hermestower.voiceconsole.bridge.TowerRequestFrame
import com.hermestower.voiceconsole.core.state.ConnectionStatus
import com.hermestower.voiceconsole.domain.BridgeError
import com.hermestower.voiceconsole.domain.BridgeResult
import com.hermestower.voiceconsole.domain.TowerBridge
import com.hermestower.voiceconsole.domain.TowerEndpoint
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import java.time.Instant

/**
 * RealTowerBridge adapter (GRAPH 2 §10): configurable, honest. The connection test
 * performs a real HTTP handshake against the configured profile; request exchange
 * POSTs the contract frame and parses tower event frames from the response.
 * No real production Tower endpoint exists yet — every failure is typed, nothing
 * is fabricated, CONNECTED appears only after an accepted handshake.
 */
class RealTowerBridge(
    private val profileStore: TowerProfileStore,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : TowerBridge {

    private val json = Json { ignoreUnknownKeys = true }
    private val _connection = MutableStateFlow(
        ConnectionStatus(
            status = ConnectionStatus.BridgeStatus.UNCONFIGURED,
            endpointLabel = "unconfigured",
            tls = ConnectionStatus.TlsStatus.MOCK_NOT_APPLICABLE,
        ),
    )
    override val connection: StateFlow<ConnectionStatus> = _connection.asStateFlow()

    @Volatile private var activeProfile: TowerConnectionProfile? = null
    @Volatile private var lastReport: TowerConnectionTester.Report? = null
    private val seenIdempotencyKeys = HashSet<String>()

    fun configure(profile: TowerConnectionProfile) {
        activeProfile = profile
        _connection.update {
            it.copy(
                endpointLabel = profile.baseUrl,
                status = ConnectionStatus.BridgeStatus.UNCONFIGURED,
            )
        }
    }

    fun lastTestReport(): TowerConnectionTester.Report? = lastReport

    override fun configure(endpoint: TowerEndpoint) {
        _connection.update { it.copy(endpointLabel = endpoint.displayLabel) }
    }

    override suspend fun handshake(): BridgeResult<Unit> = withContext(ioDispatcher) {
        val profile = activeProfile ?: return@withContext BridgeResult.Failure(
            BridgeError.INVALID_REQUEST, "No connection profile configured.", retryable = false,
        )
        _connection.update { it.copy(status = ConnectionStatus.BridgeStatus.CONNECTING) }
        val secret = profileStore.secretFor(profile.profileId)
        val report = TowerConnectionTester.test(profile, secret, Instant.now().toString())
        lastReport = report
        profileStore.saveValidated(
            profile.copy(
                lastConnectionAt = report.testedAt,
                lastConnectionResult = report.outcome.name,
            ),
        )
        when (report.outcome) {
            TowerConnectionTester.Outcome.CONNECTION_PASSED -> {
                _connection.update {
                    it.copy(
                        status = ConnectionStatus.BridgeStatus.CONNECTED,
                        endpointLabel = profile.baseUrl,
                        tls = if (report.tlsStatus == "TLS_OK") ConnectionStatus.TlsStatus.ACTIVE
                        else ConnectionStatus.TlsStatus.MOCK_NOT_APPLICABLE,
                        lastHandshakeAt = Instant.now(),
                    )
                }
                BridgeResult.Success(Unit)
            }
            TowerConnectionTester.Outcome.AUTHENTICATION_FAILED ->
                failure(BridgeError.AUTH, report.detail, retryable = false, tls = report.tlsStatus)
            TowerConnectionTester.Outcome.PROTOCOL_MISMATCH ->
                failure(BridgeError.PROTOCOL, report.detail, retryable = false, tls = report.tlsStatus)
            TowerConnectionTester.Outcome.CONNECTION_TIMEOUT ->
                failure(BridgeError.TRANSPORT, report.detail, retryable = true, tls = report.tlsStatus)
            TowerConnectionTester.Outcome.STREAMING_UNAVAILABLE ->
                failure(BridgeError.CAPACITY, report.detail, retryable = true, tls = report.tlsStatus)
            TowerConnectionTester.Outcome.TLS_FAILED -> {
                _connection.update { it.copy(status = ConnectionStatus.BridgeStatus.ERROR, tls = ConnectionStatus.TlsStatus.FAILED) }
                BridgeResult.Failure(BridgeError.TRANSPORT, report.detail, retryable = false)
            }
            else -> failure(BridgeError.TRANSPORT, report.detail, retryable = true, tls = report.tlsStatus)
        }
    }

    override fun sendRequest(frame: TowerRequestFrame): Flow<TowerEventFrame> = flow {
        val profile = activeProfile
        if (profile == null || _connection.value.status != ConnectionStatus.BridgeStatus.CONNECTED) {
            emit(errorEvent(frame.requestId, BridgeError.TRANSPORT, "Not connected; request refused (no silent queueing of Tower requests).", retryable = true))
            return@flow
        }
        // Duplicate protection: same requestId refused locally, server-side 409 also mapped.
        synchronized(seenIdempotencyKeys) {
            if (!seenIdempotencyKeys.add(frame.requestId)) {
                emit(errorEvent(frame.requestId, BridgeError.INVALID_REQUEST, "Duplicate request id refused.", retryable = false))
                return@flow
            }
        }
        emit(
            TowerEventFrame(type = TowerEventFrame.ACK, requestId = frame.requestId, stage = "SENT", timestamp = Instant.now().toString()),
        )
        val secret = profileStore.secretFor(profile.profileId)
        try {
            val response = withContext(ioDispatcher) { postRequest(profile, frame, secret) }
            val code = response.first
            val body = response.second
            when {
                code == 409 -> emit(errorEvent(frame.requestId, BridgeError.INVALID_REQUEST, "Tower reports duplicate idempotency key.", retryable = false))
                code == 401 || code == 403 -> emit(errorEvent(frame.requestId, BridgeError.AUTH, "Authentication rejected by Tower.", retryable = false))
                code !in 200..299 -> emit(errorEvent(frame.requestId, BridgeError.TRANSPORT, "Tower answered HTTP $code.", retryable = true))
                else -> emitAllParsed(frame.requestId, body)
            }
        } catch (e: SocketTimeoutException) {
            emit(errorEvent(frame.requestId, BridgeError.TRANSPORT, "Request timed out after ${profile.requestTimeoutMs} ms.", retryable = true))
        } catch (e: Exception) {
            emit(errorEvent(frame.requestId, BridgeError.TRANSPORT, "Transport failure: ${e.message}", retryable = true))
        }
    }.flowOn(ioDispatcher)

    private suspend fun kotlinx.coroutines.flow.FlowCollector<TowerEventFrame>.emitAllParsed(
        requestId: String,
        body: String,
    ) {
        val lines = body.lines().filter { it.isNotBlank() }
        var emitted = false
        for (line in lines) {
            val event = try {
                json.decodeFromString(TowerEventFrame.serializer(), line)
            } catch (_: Exception) {
                try {
                    json.decodeFromString(com.hermestower.voiceconsole.bridge.TowerErrorBody.serializer(), line).let {
                        TowerEventFrame(type = TowerEventFrame.ERROR, requestId = requestId, error = it, timestamp = Instant.now().toString())
                    }
                } catch (_: Exception) {
                    null
                }
            }
            if (event != null) {
                emit(event.copy(requestId = event.requestId ?: requestId))
                emitted = true
            }
        }
        if (!emitted) {
            emit(errorEvent(requestId, BridgeError.PROTOCOL, "Response was not a valid tower event stream.", retryable = false))
        }
    }

    private fun postRequest(
        profile: TowerConnectionProfile,
        frame: TowerRequestFrame,
        secret: CharArray?,
    ): Pair<Int, String> {
        val base = profile.baseUrl.trimEnd('/')
        val url = URL("$base${profile.restBasePath}/request")
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.connectTimeout = profile.requestTimeoutMs
        conn.readTimeout = profile.requestTimeoutMs
        conn.doOutput = true
        conn.setRequestProperty("Content-Type", "application/json")
        conn.setRequestProperty("Idempotency-Key", frame.requestId)
        if (!secret.isNullOrEmpty()) conn.setRequestProperty("Authorization", "Bearer " + String(secret))
        try {
            conn.outputStream.use { it.write(json.encodeToString(TowerRequestFrame.serializer(), frame).toByteArray()) }
            val code = conn.responseCode
            val body = if (code in 200..299) conn.inputStream.bufferedReader().use { it.readText() }
            else conn.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
            return code to body
        } finally {
            conn.disconnect()
        }
    }

    override suspend fun cancel(requestId: String) {
        // Cancellation is collection cancellation plus local bookkeeping in V0.2;
        // CANCEL frames travel tower-side when the real Tower contract is live.
        _connection.update { it.copy(status = ConnectionStatus.BridgeStatus.CONNECTED) }
    }

    private fun failure(error: BridgeError, message: String, retryable: Boolean, tls: String = "NOT_REACHED"): BridgeResult<Unit> {
        _connection.update {
            it.copy(
                status = ConnectionStatus.BridgeStatus.ERROR,
                tls = when (tls) {
                    "TLS_OK" -> ConnectionStatus.TlsStatus.ACTIVE
                    "NOT_ENCRYPTED" -> ConnectionStatus.TlsStatus.UNKNOWN
                    else -> ConnectionStatus.TlsStatus.FAILED
                },
            )
        }
        return BridgeResult.Failure(error, message, retryable)
    }

    private fun errorEvent(requestId: String, error: BridgeError, message: String, retryable: Boolean) = TowerEventFrame(
        type = TowerEventFrame.ERROR,
        requestId = requestId,
        error = TowerErrorBody(classification = error.name, retryable = retryable, message = message),
        timestamp = Instant.now().toString(),
    )
}
