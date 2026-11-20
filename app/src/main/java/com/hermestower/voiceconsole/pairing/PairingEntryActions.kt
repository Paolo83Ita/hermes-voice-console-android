package com.hermestower.voiceconsole.pairing

/**
 * GRAPH A4R NODE A3 — pairing ENTRY actions and typed states.
 *
 * Mission section 6 requires one clear user path for pairing on Candidate A:
 *
 *   primary   SCANSIONA IL QR DELLA TOWER   (QR_SCANNER route — existed, now
 *                                             also reachable as the onboarding
 *                                             QR choice's demonstrated outcome)
 *   secondary INSERISCI CODICE NUMERICO     (numeric lane — NEW)
 *   advanced  INSERISCI ENDPOINT MANUALMENTE (manual endpoint lane — NEW)
 *
 * and the typed states (exactly these six, surfaced in the UI):
 *   CAMERA_PERMISSION_DENIED, CAMERA_UNAVAILABLE, QR_NOT_RECOGNIZED, QR_EXPIRED,
 *   QR_SCHEMA_UNSUPPORTED, PAIRING_ENDPOINT_UNREACHABLE.
 *
 * AUTHORITY CONTRACT (unchanged, PairingPayload.kt): the QR parser stays the
 * STRICT versioned candidate-scheme authority; endpoints are validated by
 * SHAPE, never contacted from here; the only wired executor is the
 * deterministic [TowerPairingClient] (mock) and every result is labeled
 * SIMULATED in the UI. Real Tower pairing is the live Tower contract (NODE A6
 * hands the delta to the Tower account) — no client-side bypass is created.
 *
 * This file is pure Kotlin (no Android imports): fully deterministic under
 * the JVM suite, honoring A4R-P1-002's discipline.
 */

/** The six mission typed states, exactly. */
enum class PairingEntryState {
    CAMERA_PERMISSION_DENIED,
    CAMERA_UNAVAILABLE,
    QR_NOT_RECOGNIZED,
    QR_EXPIRED,
    QR_SCHEMA_UNSUPPORTED,
    PAIRING_ENDPOINT_UNREACHABLE,
}

/**
 * Maps the strict parser's typed rejections onto the six mission states.
 * Rejection detail text stays available for diagnostics — the mapping is on
 * the TYPED kind only (never string sniffing).
 */
fun PairingRejectKind.toPairingEntryState(): PairingEntryState = when (this) {
    PairingRejectKind.MALFORMED,
    PairingRejectKind.MISSING_NONCE,
    PairingRejectKind.MISSING_CHALLENGE,
    PairingRejectKind.ALTERED_PAYLOAD,
    PairingRejectKind.FUTURE_ISSUED,
    -> PairingEntryState.QR_NOT_RECOGNIZED

    PairingRejectKind.EXPIRED,
    PairingRejectKind.REPLAYED_NONCE,
    -> PairingEntryState.QR_EXPIRED

    PairingRejectKind.UNSUPPORTED_SCHEME,
    PairingRejectKind.UNSUPPORTED_PROTOCOL,
    PairingRejectKind.WRONG_ENVIRONMENT,
    -> PairingEntryState.QR_SCHEMA_UNSUPPORTED

    PairingRejectKind.BAD_ENDPOINT,
    -> PairingEntryState.PAIRING_ENDPOINT_UNREACHABLE
}

/** Numeric pairing code truth: exactly 8 digits (Tower numeric-code shape). */
object NumericPairingCode {
    const val REQUIRED_LENGTH = 8

    /** Strips grouping separators and whitespace; null when anything else remains. */
    fun normalize(raw: String): String? {
        val digits = raw.filter { it.isDigit() }
        val stripped = raw.replace(" ", "").replace("-", "").replace("\u00a0", "")
        if (stripped != digits) return null
        if (digits.length != REQUIRED_LENGTH) return null
        return digits
    }

    fun isValid(raw: String): Boolean = normalize(raw) != null
}

/** Manual endpoint validator: https-only shape check, NEVER contacted here. */
object ManualEndpoint {
    fun validate(url: String): Result<String> {
        val trimmed = url.trim()
        if (trimmed.isEmpty()) return Result.failure(IllegalArgumentException("empty endpoint"))
        if (!trimmed.startsWith("https://")) {
            return Result.failure(IllegalArgumentException("endpoint must be https://"))
        }
        val host = trimmed.removePrefix("https://").substringBefore('/').substringBefore(':')
        if (host.isBlank() || host.contains(' ') || !host.contains('.')) {
            return Result.failure(IllegalArgumentException("endpoint host invalid"))
        }
        if (trimmed.length > 256) return Result.failure(IllegalArgumentException("endpoint too long"))
        return Result.success(trimmed)
    }
}

/** Typed outcome of the numeric / manual entry lanes. */
sealed class PairingEntryOutcome {
    /** Pairing handshake accepted (SIMULATED executor) — redaction-safe fields only. */
    data class Accepted(val sessionId: String, val matchPhrase: String, val lane: Lane) : PairingEntryOutcome()

    /** Typed rejection — one of the six mission states, no raw payload. */
    data class Rejected(val state: PairingEntryState, val detail: String) : PairingEntryOutcome()

    enum class Lane { NUMERIC, MANUAL_ENDPOINT, QR }
}

/**
 * Runs the numeric and manual-entry pairing lanes through the SAME versioned
 * client interface the QR lane uses ([TowerPairingClient]) — one executor, one
 * truth, no duplicated implementation (mission merge rule).
 *
 * The synthesized [PairingPayload] is SIMULATED material for the deterministic
 * client; it never touches the network and is labeled accordingly by the UI.
 */
class PairingEntryExecutor(
    private val client: TowerPairingClient,
    private val clockMs: () -> Long = System::currentTimeMillis,
) {

    suspend fun pairNumeric(code: String, endpoint: String): PairingEntryOutcome {
        val normalized = NumericPairingCode.normalize(code)
            ?: return PairingEntryOutcome.Rejected(
                PairingEntryState.QR_NOT_RECOGNIZED,
                "numeric code must be exactly ${NumericPairingCode.REQUIRED_LENGTH} digits",
            )
        val validated = ManualEndpoint.validate(endpoint)
            ?: return PairingEntryOutcome.Rejected(
                PairingEntryState.PAIRING_ENDPOINT_UNREACHABLE,
                "endpoint shape rejected (https://host required)",
            )
        return runLane(PairingEntryOutcome.Lane.NUMERIC, normalized, validated)
    }

    suspend fun pairManualEndpoint(endpoint: String): PairingEntryOutcome {
        val validated = ManualEndpoint.validate(endpoint)
            ?: return PairingEntryOutcome.Rejected(
                PairingEntryState.PAIRING_ENDPOINT_UNREACHABLE,
                "endpoint shape rejected (https://host required)",
            )
        return runLane(PairingEntryOutcome.Lane.MANUAL_ENDPOINT, null, validated)
    }

    private suspend fun runLane(
        lane: PairingEntryOutcome.Lane,
        numericCode: String?,
        endpoint: String,
    ): PairingEntryOutcome {
        val now = clockMs()
        // SIMULATED payload (labeled by the UI): synthetic tower id + nonce,
        // 10-minute validity like the candidate scheme's own cap.
        val synthetic = PairingPayload(
            schemeVersion = PairingPayload.EXPECTED_VERSION,
            towerId = "manual-entry",
            environment = PairingPayload.EXPECTED_ENVIRONMENT,
            issuedEpochMs = now,
            expiresEpochMs = now + PairingPayload.MAX_VALIDITY_MS,
            nonce = numericCode?.let { "num-$it" } ?: "man-${now / 1000}",
            challenge = "sim-challenge",
            pairingEndpoint = endpoint,
            requestedScopes = listOf("console"),
            trustNote = "SIMULATED (deterministic client — no network)",
        )
        return when (val result = client.pair(synthetic, confirmPhrase = "OWNER-CONSOLE")) {
            is PairingResult.Success -> PairingEntryOutcome.Accepted(result.sessionId, result.matchPhrase, lane)
            is PairingResult.Failure ->
                PairingEntryOutcome.Rejected(result.kind.toPairingEntryState(), result.detail)
        }
    }
}
