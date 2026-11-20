package com.hermestower.voiceconsole.towerpair

import com.hermestower.voiceconsole.data.PairingStateStore
import com.hermestower.voiceconsole.pairing.PairingFlow
import com.hermestower.voiceconsole.pairing.QrPayload
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * GRAPH A4R C1-C4 — PRODUCTION Tower pairing orchestrator (merge integration
 * layer: B authority lane + canonical UI contract).
 *
 * This is the ONLY pairing executor wired in production navigation
 * (NO_MOCK_PAIRING_IN_PRODUCTION_PATH): the QR lane and the numeric lane run
 * the real frozen-contract flow (PairingFlow -> TowerApi), the SIMULATED
 * candidate-scheme executor is retained in tests and diagnostics only.
 *
 * Typed user states = EXACTLY the mission section 8 list (11 states). The UI
 * never sees a stack trace, a raw payload, or a secret: every rejection
 * carries a typed state plus a redaction-safe Italian detail string.
 */
enum class TowerPairingState {
    CAMERA_PERMISSION_DENIED,
    CAMERA_PERMISSION_PERMANENTLY_DENIED,
    CAMERA_UNAVAILABLE,
    QR_NOT_RECOGNIZED,
    QR_EXPIRED,
    QR_SCHEMA_UNSUPPORTED,
    QR_CONTAINS_FORBIDDEN_FIELD,
    PAIRING_ENDPOINT_UNREACHABLE,
    PAIRING_CHALLENGE_REJECTED,
    PAIRING_PERSISTENCE_FAILED,
    DEVICE_REVOKED,
}

sealed class TowerQrResolution {
    /** Parse-only success: NOTHING was persisted, NO network call happened. */
    data class Resolved(
        val endpoint: String,
        val pairingId: String,
        val summary: String,
        val expiresAtEpochSeconds: Long,
        val towerInstanceId: String,
    ) : TowerQrResolution()

    data class Rejected(val state: TowerPairingState, val detail: String) : TowerQrResolution()
}

sealed class TowerPairingOutcome {
    /** Pairing registered: the store holds endpoint+deviceId+consoleToken (all encrypted). */
    data class Registered(
        val deviceId: String,
        val phraseHint: String?,
        val pairedState: TowerPairingDiagnostics,
    ) : TowerPairingOutcome()

    /** Bilateral confirmation pending: the Owner matches the phrase on the Tower console. */
    data class AwaitingOwnerAck(
        val pairingId: String,
        val deviceId: String,
        val phraseHint: String?,
    ) : TowerPairingOutcome()

    /** Typed rejection — one of the eleven mission states; detail is redaction-safe. */
    data class Rejected(val state: TowerPairingState, val detail: String) : TowerPairingOutcome()
}

/**
 * Diagnostics snapshot (mission section 6): app identity + typed pairing
 * truth, NO secrets (no endpoint path, no tokens, no pairing ids).
 */
data class TowerPairingDiagnostics(
    val isPaired: Boolean,
    val towerContractVersion: String,
    val lastFailure: String?,
    val failureHistory: List<String>,
)

/** Public typed mapping (UI + tests consume the same truth, one authority). */
fun QrPayload.RejectKind.toTowerPairingState(): TowerPairingState = when (this) {
    QrPayload.RejectKind.MALFORMED -> TowerPairingState.QR_NOT_RECOGNIZED
    QrPayload.RejectKind.FORBIDDEN_FIELD -> TowerPairingState.QR_CONTAINS_FORBIDDEN_FIELD
    QrPayload.RejectKind.UNSUPPORTED_VERSION, QrPayload.RejectKind.UNSUPPORTED_PROTOCOL ->
        TowerPairingState.QR_SCHEMA_UNSUPPORTED
    QrPayload.RejectKind.EXPIRED, QrPayload.RejectKind.REPLAYED_NONCE_REF -> TowerPairingState.QR_EXPIRED
    QrPayload.RejectKind.BAD_ENDPOINT -> TowerPairingState.PAIRING_ENDPOINT_UNREACHABLE
}

class TowerPairingController(
    private val flow: PairingFlow,
    private val store: PairingStateStore,
    private val qrParser: QrPayload.Parser,
    private val appVersionName: String,
    private val packageName: String,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {

    companion object {
        /** The frozen integration contract this controller speaks (CD-A4R-001 + v2 dex truth). */
        const val TOWER_CONTRACT_VERSION = "voice-console/v2"
    }

    /**
     * QR v2 parse + endpoint resolution ONLY (no persistence, no network):
     * the UI confirms BEFORE the claim. Malformed/forbidden/expired/replayed
     * payloads are typed-rejected exactly per mission section 8.
     */
    fun resolveQr(rawQr: String, manualEndpoint: String?): TowerQrResolution {
        val parsed = try {
            qrParser.parse(rawQr)
        } catch (e: QrPayload.QrRejectException) {
            return TowerQrResolution.Rejected(e.reject.kind.toTowerPairingState(), e.reject.detail)
        }
        return resolveParsed(parsed, manualEndpoint)
    }

    /** Resolution for an ALREADY-parsed payload (the scanner flow parses first). */
    fun resolveParsed(parsed: QrPayload.Parsed, manualEndpoint: String?): TowerQrResolution {
        val endpoint = try {
            qrParser.resolveEndpoint(parsed, manualEndpoint)
        } catch (e: QrPayload.QrRejectException) {
            return TowerQrResolution.Rejected(e.reject.kind.toTowerPairingState(), e.reject.detail)
        }
        return TowerQrResolution.Resolved(
            endpoint = endpoint,
            pairingId = parsed.pairingId,
            summary = qrParser.describe(parsed),
            expiresAtEpochSeconds = parsed.expiresAtEpochSeconds,
            towerInstanceId = parsed.towerInstanceId,
        )
    }

    /**
     * Numeric claim lane: the Owner types the Tower's six-digit code for the
     * resolved pairing. Runs claim -> attest on the REAL contract.
     */
    suspend fun pair(endpoint: String, pairingId: String, numericCode: String, label: String?): TowerPairingOutcome =
        withContext(ioDispatcher) {
            when (val step = flow.pairFromQr(endpoint, pairingId, numericCode, label ?: "Console vocale Android")) {
                is PairingFlow.Step.Claimed ->
                    // Claimed without progressing to attest happens only when
                    // the challenge TTL expired in between; treat as rejected.
                    TowerPairingOutcome.Rejected(
                        TowerPairingState.PAIRING_CHALLENGE_REJECTED,
                        "challenge non confermata: ripeti il pairing",
                    )
                is PairingFlow.Step.PendingOwnerAck ->
                    TowerPairingOutcome.AwaitingOwnerAck(step.pairingId, step.deviceId, step.phraseHint)
                is PairingFlow.Step.Registered -> TowerPairingOutcome.Registered(step.deviceId, null, diagnostics())
                is PairingFlow.Step.Failed -> step.toOutcome()
            }
        }

    /**
     * Continues the pending bilateral acknowledgement (the Owner confirmed
     * the Italian phrase on the Tower console).
     */
    suspend fun awaitOwnerAcknowledgement(): TowerPairingOutcome = withContext(ioDispatcher) {
        when (val step = flow.awaitOwnerAcknowledgement()) {
            is PairingFlow.Step.PendingOwnerAck ->
                TowerPairingOutcome.AwaitingOwnerAck(step.pairingId, step.deviceId, step.phraseHint)
            is PairingFlow.Step.Registered -> TowerPairingOutcome.Registered(step.deviceId, null, diagnostics())
            is PairingFlow.Step.Claimed -> TowerPairingOutcome.Rejected(
                TowerPairingState.PAIRING_CHALLENGE_REJECTED, "stato pairing inatteso",
            )
            is PairingFlow.Step.Failed -> step.toOutcome()
        }
    }

    /** Diagnostics snapshot — typed truth only, never secrets. */
    fun diagnostics(): TowerPairingDiagnostics = TowerPairingDiagnostics(
        isPaired = store.isPaired(),
        towerContractVersion = TOWER_CONTRACT_VERSION,
        lastFailure = store.lastFailure?.name,
        failureHistory = store.failureHistory().map { it.name },
    )

    /** Identity line for the diagnostics screen (mission section 6, no secrets). */
    fun identityLine(): String = "app=$packageName/$appVersionName contract=$TOWER_CONTRACT_VERSION"

    /** Unpair: wipes every encrypted record (revocation/re-pairing). */
    fun unpair() = store.clear()

    private fun PairingFlow.Step.Failed.toOutcome(): TowerPairingOutcome.Rejected =
        TowerPairingOutcome.Rejected(mapReason(reason), detail ?: "errore di pairing")

    private fun mapReason(reason: String): TowerPairingState = when (reason) {
        "PAIRING_PERSISTENCE_FAILED" -> TowerPairingState.PAIRING_PERSISTENCE_FAILED
        "PAIRING_ENDPOINT_UNREACHABLE" -> TowerPairingState.PAIRING_ENDPOINT_UNREACHABLE
        "DEVICE_REVOKED" -> TowerPairingState.DEVICE_REVOKED
        else -> TowerPairingState.PAIRING_CHALLENGE_REJECTED
    }
}
