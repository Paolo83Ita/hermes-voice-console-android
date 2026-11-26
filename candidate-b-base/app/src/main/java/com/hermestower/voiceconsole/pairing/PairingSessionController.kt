package com.hermestower.voiceconsole.pairing

import com.hermestower.voiceconsole.data.PairingStateStore
import com.hermestower.voiceconsole.net.TowerApi
import com.hermestower.voiceconsole.net.TowerApiException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * ANDROID REPAIR CP-005 — the CANONICAL pairing session state machine
 * (mission section 5, binding).
 *
 * The 21 charter states, driven over the real B-lane primitives:
 *
 *   UNPAIRED → DISCOVERING → SCANNING → PAYLOAD_PARSED | PAYLOAD_REJECTED
 *   → ENDPOINTS_RESOLVING → ENDPOINT_SELECTED → CONNECTING
 *   → AWAITING_OWNER_CONFIRMATION (PHRASE_MATCH_REQUIRED gate)
 *   → GENERATING_DEVICE_KEY → CREDENTIAL_REQUESTED → CREDENTIAL_ISSUED
 *   → CONNECTION_VERIFYING → CAPABILITY_NEGOTIATING
 *   → PAIRED | DEGRADED | EXPIRED | REVOKED | FAILED | REPAIR_AVAILABLE
 *
 * Properties (mission section 5, all binding):
 *  - durable state ONLY via the encrypted [PairingStateStore] (endpoint, device
 *    id, console token); the session store persists ONLY non-secret resume
 *    hints (stage, challenge id, expiry, phrase-confirmed flag);
 *  - device-bound key material ONLY via the Android Keystore seam
 *    ([PairingFlow.DeviceKeysContract] — DeviceKeys in production);
 *  - single-use nonce + TTL + bounded attempts + rate limit + bilateral
 *    confirmation + phrase matching + minimal scopes + revocation — all
 *    inherited from the B-lane primitives and the envelope parser;
 *  - cancel / retry / resume-after-interruption / restart read-back /
 *    Tower-restart recovery (ack polling tolerates transient route/transport
 *    failures and keeps polling within the window) / revoke / re-pair;
 *  - every failure state exposes: human explanation (it), technical code,
 *    failed stage, candidate endpoint (class+host only), safe retry hint —
 *    [PairingSessionSnapshot] is redaction-safe by construction;
 *  - AMB-8 honest terminal state: when the ack route is missing on the server
 *    the machine ends REPAIR_AVAILABLE (never a bypass, never a fake PAIRED).
 *
 * JVM-deterministic: all I/O goes through injected seams.
 */
class PairingSessionController(
    private val api: TowerApi,
    private val store: PairingStateStore,
    private val flow: PairingFlow,
    private val resolver: EndpointResolver,
    private val sessionStore: PairingSessionStore,
    private val deviceKeys: PairingFlow.DeviceKeysContract,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val clockMs: () -> Long = System::currentTimeMillis,
    private val clockEpochSeconds: () -> Long = { System.currentTimeMillis() / 1000L },
) {

    /** The canonical charter state list (mission section 5). */
    enum class State {
        UNPAIRED,
        DISCOVERING,
        SCANNING,
        PAYLOAD_PARSED,
        PAYLOAD_REJECTED,
        ENDPOINTS_RESOLVING,
        ENDPOINT_SELECTED,
        CONNECTING,
        AWAITING_OWNER_CONFIRMATION,
        PHRASE_MATCH_REQUIRED,
        GENERATING_DEVICE_KEY,
        CREDENTIAL_REQUESTED,
        CREDENTIAL_ISSUED,
        CONNECTION_VERIFYING,
        CAPABILITY_NEGOTIATING,
        PAIRED,
        DEGRADED,
        EXPIRED,
        REVOKED,
        FAILED,
        REPAIR_AVAILABLE,
    }

    /** Redaction-safe live snapshot for the UI (never a secret). */
    data class Snapshot(
        val state: State,
        val typedCode: String? = null,
        val humanDetail: String? = null,
        val failedStage: String? = null,
        val candidateClass: String? = null,
        val candidateHost: String? = null,
        val evidence: List<CandidateEvidence> = emptyList(),
        val phraseHint: String? = null,
        val deviceId: String? = null,
        val expiresAtEpochSeconds: Long? = null,
        val connectionTestMs: Long? = null,
        val capabilities: List<String> = emptyList(),
        val capabilitiesNegotiated: Boolean = false,
        val startedAtMs: Long = 0L,
        val updatedAtMs: Long = 0L,
    )

    // ------------------------------------------------------------------ state

    @Volatile
    var snapshot: Snapshot = Snapshot(State.UNPAIRED, startedAtMs = clockMs(), updatedAtMs = clockMs())
        private set

    /** One nonce-ref store per controller lifetime (replay protection across retries). */
    private val defaultParser: PairingEnvelope.Parser = PairingEnvelope.Parser(clockEpochSeconds = clockEpochSeconds)

    private var envelope: PairingEnvelope.Parsed? = null
    private var remainingCandidates: List<EndpointCandidate> = emptyList()
    private var selectedCandidate: EndpointCandidate? = null
    private var claimSecret: String? = null
    private var deviceLabel: String = "Console vocale Android"
    private var phraseConfirmedByOwner = false

    private fun at(state: State, block: (Snapshot) -> Snapshot = { it }): Snapshot {
        snapshot = block(snapshot.copy(state = state, updatedAtMs = clockMs()))
        return snapshot
    }

    private fun failed(state: State, code: String, detail: String, stage: String): Snapshot {
        val s = snapshot.copy(
            state = state, typedCode = code, humanDetail = detail, failedStage = stage,
            candidateClass = selectedCandidate?.transportClass?.name,
            candidateHost = selectedCandidate?.hostView(),
            updatedAtMs = clockMs(),
        )
        snapshot = s
        return s
    }

    // ------------------------------------------------------------- lifecycle

    /** Restart read-back: reconstructs the live state from durable truth. */
    fun readBack(): Snapshot {
        val paired = store.isPaired()
        val persisted = sessionStore.load()
        snapshot = if (paired) {
            Snapshot(State.PAIRED, deviceId = store.pairedDeviceId(), updatedAtMs = clockMs())
        } else if (persisted != null && persisted.expiresAtEpochSeconds > clockEpochSeconds()) {
            Snapshot(
                State.AWAITING_OWNER_CONFIRMATION,
                typedCode = "SESSION_RESUMABLE",
                humanDetail = "sessione di abbinamento interrotta: riprendi o riparti dal QR",
                expiresAtEpochSeconds = persisted.expiresAtEpochSeconds,
                startedAtMs = persisted.startedAtMs,
                updatedAtMs = clockMs(),
            )
        } else {
            if (persisted != null) sessionStore.save(null)
            Snapshot(State.UNPAIRED, startedAtMs = clockMs(), updatedAtMs = clockMs())
        }
        return snapshot
    }

    /**
     * Entry point A — the QR/envelope lane. Parses (SCANNING → PAYLOAD_*),
     * resolves endpoints, runs claim+attest on the selected candidate, and
     * stops at the PHRASE gate (bilateral confirmation) or a typed failure.
     */
    suspend fun startFromEnvelope(
        qrText: String,
        claimCode: String,
        label: String,
        manualCandidates: List<EndpointCandidate> = emptyList(),
        parser: PairingEnvelope.Parser = defaultParser,
    ): Snapshot = withContext(ioDispatcher) {
        this@PairingSessionController.claimSecret = claimCode.trim()
        this@PairingSessionController.deviceLabel = label.ifBlank { "Console vocale Android" }
        phraseConfirmedByOwner = false
        at(State.SCANNING)
        val parsed = try {
            parser.parse(qrText)
        } catch (e: PairingEnvelope.EnvelopeRejectException) {
            sessionStore.save(null)
            return@withContext failed(
                State.PAYLOAD_REJECTED, "QR_${e.reject.kind.name}", e.reject.detail, "PAYLOAD_PARSE",
            )
        }
        envelope = parsed
        at(State.PAYLOAD_PARSED) { it.copy(expiresAtEpochSeconds = parsed.expiresAtEpochSeconds) }
        sessionStore.save(
            PairingSessionStore.Session(
                stage = State.PAYLOAD_PARSED.name,
                pairingId = parsed.challengeId,
                startedAtMs = clockMs(),
                updatedAtMs = clockMs(),
                expiresAtEpochSeconds = parsed.expiresAtEpochSeconds,
                phraseConfirmed = false,
            ),
        )
        resolveAndConnect(parsed, manualCandidates)
    }

    /**
     * Entry point B — the NUMERIC_CODE lane (no camera): the Owner types the
     * challenge id + numeric code, with manual/envelope endpoint candidates.
     */
    suspend fun startFromCode(
        pairingId: String,
        claimCode: String,
        label: String,
        candidates: List<EndpointCandidate>,
    ): Snapshot = withContext(ioDispatcher) {
        claimSecret = claimCode.trim()
        deviceLabel = label.ifBlank { "Console vocale Android" }
        phraseConfirmedByOwner = false
        at(State.DISCOVERING)
        val synthetic = PairingEnvelope.Parsed(
            schemaVersion = 0,
            protocolVersion = "voice-console/2",
            towerPublicId = "manual",
            challengeId = pairingId.trim(),
            nonceRef = "manual-${clockMs()}",
            issuedAtEpochSeconds = clockEpochSeconds(),
            expiresAtEpochSeconds = clockEpochSeconds() + 300L,
            scope = emptyList(),
            candidates = candidates,
            transportCandidates = listOf("HTTPS_POST"),
            certSha256Reference = null,
            certSansReference = emptyList(),
            capabilityNegotiationVersion = null,
            signaturePart = "",
            phraseWordCount = 3,
            phraseLanguage = "it",
        )
        envelope = synthetic
        at(State.PAYLOAD_PARSED) { it.copy(expiresAtEpochSeconds = synthetic.expiresAtEpochSeconds) }
        sessionStore.save(
            PairingSessionStore.Session(
                stage = State.PAYLOAD_PARSED.name,
                pairingId = synthetic.challengeId,
                startedAtMs = clockMs(),
                updatedAtMs = clockMs(),
                expiresAtEpochSeconds = synthetic.expiresAtEpochSeconds,
                phraseConfirmed = false,
            ),
        )
        resolveAndConnect(synthetic, emptyList())
    }

    /** Entry point C — resume an interrupted session (same challenge id, still unexpired). */
    suspend fun resume(claimCode: String, manualCandidates: List<EndpointCandidate> = emptyList()): Snapshot =
        withContext(ioDispatcher) {
            val persisted = sessionStore.load()
                ?: return@withContext failed(State.FAILED, "NO_SESSION", "nessuna sessione da riprendere", "RESUME")
            if (persisted.expiresAtEpochSeconds <= clockEpochSeconds()) {
                sessionStore.save(null)
                return@withContext failed(State.EXPIRED, "QR_EXPIRED", "sessione scaduta: riparti da un nuovo QR", "RESUME")
            }
            claimSecret = claimCode.trim()
            at(State.CONNECTING)
            connectOn(persisted.pairingId, manualCandidates)
        }

    /** Resolution + per-candidate connect (rules R1-R10 of the resolver contract). */
    private suspend fun resolveAndConnect(
        parsed: PairingEnvelope.Parsed,
        manualCandidates: List<EndpointCandidate>,
    ): Snapshot {
        at(State.ENDPOINTS_RESOLVING)
        val resolution = resolver.resolve(parsed.candidates, manualCandidates)
        return when (resolution) {
            is EndpointResolutionResult.Selected -> {
                selectedCandidate = resolution.candidate
                remainingCandidates = (parsed.candidates + manualCandidates)
                    .sortedBy { it.priority }
                    .filter { it != resolution.candidate }
                at(State.ENDPOINT_SELECTED) { it.copy(evidence = resolution.evidence) }
                connectOn(parsed.challengeId, listOf(resolution.candidate) + remainingCandidates)
            }
            is EndpointResolutionResult.Failed -> {
                failed(
                    State.FAILED,
                    resolution.primaryCode.name,
                    TransportClassifier.explain(resolution.primaryCode),
                    "ENDPOINT_RESOLUTION",
                ).copy(evidence = resolution.evidence).also { snapshot = it }
            }
        }
    }

    /** CONNECTING: claim → attest on the candidate; transport failure falls through. */
    private suspend fun connectOn(pairingId: String, candidates: List<EndpointCandidate>): Snapshot {
        val secret = claimSecret ?: return failed(State.FAILED, "NO_CLAIM_SECRET", "codice non impostato", "CONNECTING")
        for (candidate in candidates) {
            if (candidate.expiryEpochSeconds <= clockEpochSeconds()) continue
            selectedCandidate = candidate
            at(State.CONNECTING) { it.copy(candidateClass = candidate.transportClass.name, candidateHost = candidate.hostView()) }
            at(State.GENERATING_DEVICE_KEY) // keystore-bound EC-P256 via DeviceKeys (inside attest)
            val step = flow.pairFromQr(candidate.urlOrHost.trim().trimEnd('/'), pairingId, secret, deviceLabel)
            when (step) {
                is PairingFlow.Step.PendingOwnerAck -> {
                    at(State.AWAITING_OWNER_CONFIRMATION) {
                        it.copy(phraseHint = step.phraseHint, deviceId = step.deviceId, expiresAtEpochSeconds = envelope?.expiresAtEpochSeconds)
                    }
                    sessionStore.save(
                        PairingSessionStore.Session(
                            stage = State.AWAITING_OWNER_CONFIRMATION.name,
                            pairingId = pairingId,
                            startedAtMs = snapshot.startedAtMs,
                            updatedAtMs = clockMs(),
                            expiresAtEpochSeconds = envelope?.expiresAtEpochSeconds ?: (clockEpochSeconds() + 300),
                            phraseConfirmed = phraseConfirmedByOwner,
                        ),
                    )
                    return at(if (phraseConfirmedByOwner) State.CREDENTIAL_REQUESTED else State.PHRASE_MATCH_REQUIRED)
                }
                is PairingFlow.Step.Registered -> return finishPairing(step.deviceId, null)
                is PairingFlow.Step.Claimed ->
                    return failed(State.FAILED, "CLAIM_RESPONSE_INCOMPLETE", "claim senza attest: ripeti", "CONNECTING")
                is PairingFlow.Step.Failed -> {
                    val transportish = step.reason == "PAIRING_ENDPOINT_UNREACHABLE"
                    if (!transportish) {
                        return mapFailedStep(step)
                    }
                    // transport-class failure: fall through to the next
                    // candidate (bounded by the candidate list itself)
                }
            }
        }
        return failed(
            State.FAILED,
            "PAIRING_ENDPOINT_UNREACHABLE",
            "nessun endpoint utilizzabile dopo tutti i candidati",
            "CONNECTING",
        )
    }

    private fun mapFailedStep(step: PairingFlow.Step.Failed): Snapshot = when (step.reason) {
        "PAIRING_PERSISTENCE_FAILED" -> failed(State.FAILED, step.reason, step.detail ?: "persistenza fallita", "CREDENTIAL_COMMIT")
        "DEVICE_REVOKED" -> failed(State.REVOKED, step.reason, step.detail ?: "revocato", "OWNER_ACK")
        "PAIRING_CHALLENGE_EXPIRED", "OWNER_ACK_TIMEOUT" -> failed(State.EXPIRED, step.reason, step.detail ?: "scaduto", "OWNER_ACK")
        "PAIRING_ROUTE_MISSING" -> failed(
            State.REPAIR_AVAILABLE, step.reason,
            "il client ha completato il suo lavoro ma questa versione della Tower non espone ancora la rotta di conferma: serve il delta server (HR-A4R-PHYS-002)",
            "OWNER_ACK",
        )
        "PAIRING_ENDPOINT_UNREACHABLE" -> failed(
            State.FAILED, step.reason, step.detail ?: TransportClassifier.explain(TransportFailureCode.TCP_CONNECTION_FAILED), "CONNECTING",
        )
        else -> failed(State.FAILED, step.reason, step.detail ?: "errore di pairing", "CONNECTING")
    }

    // ------------------------------------------------------- bilateral phrase

    /**
     * The PHRASE_MATCH_REQUIRED gate: the UI calls this AFTER the Owner
     * confirms the phone phrase hint matches the Tower wizard phrase. Only
     * then does the ack polling start (bilateral confirmation).
     */
    suspend fun confirmPhraseMatch(): Snapshot = withContext(ioDispatcher) {
        phraseConfirmedByOwner = true
        sessionStore.save(sessionStore.load()?.copy(phraseConfirmed = true))
        if (snapshot.state == State.PHRASE_MATCH_REQUIRED) awaitOwnerAcknowledgement() else snapshot
    }

    /** OWNER_ACK polling → CREDENTIAL_ISSUED → commit → verify → negotiate. */
    suspend fun awaitOwnerAcknowledgement(): Snapshot = withContext(ioDispatcher) {
        at(State.CREDENTIAL_REQUESTED)
        val step = flow.awaitOwnerAcknowledgement()
        when (step) {
            is PairingFlow.Step.Registered -> finishPairing(step.deviceId, null)
            is PairingFlow.Step.PendingOwnerAck -> at(State.AWAITING_OWNER_CONFIRMATION)
            is PairingFlow.Step.Claimed -> failed(State.FAILED, "UNEXPECTED_STATE", "stato inatteso", "OWNER_ACK")
            is PairingFlow.Step.Failed -> mapFailedStep(step)
        }
    }

    /** CONNECTION_VERIFYING + CAPABILITY_NEGOTIATING → PAIRED / DEGRADED. */
    private suspend fun finishPairing(deviceId: String, phraseHint: String?): Snapshot {
        at(State.CREDENTIAL_ISSUED) { it.copy(deviceId = deviceId) }
        sessionStore.save(null) // the durable encrypted store IS the state now
        at(State.CONNECTION_VERIFYING)
        val started = clockMs()
        var connectionOk = false
        var latencyMs = 0L
        try {
            val res = api.rawGet("$selectedBaseUrl/")
            latencyMs = clockMs() - started
            connectionOk = res in 200..599
        } catch (e: TowerApiException) {
            latencyMs = clockMs() - started
            connectionOk = false
        }
        at(State.CONNECTION_VERIFYING) { it.copy(connectionTestMs = latencyMs) }
        at(State.CAPABILITY_NEGOTIATING)
        val caps = negotiateCapabilities()
        return if (!connectionOk) {
            failed(
                State.DEGRADED, "CONNECTION_TEST_FAILED",
                "abbinamento registrato ma il test di connessione non è passato: verifica la diagnostica",
                "CONNECTION_TEST",
            ).copy(connectionTestMs = latencyMs).also { snapshot = it }
        } else if (caps == null) {
            at(State.DEGRADED) {
                it.copy(
                    capabilities = listOf("voice", "archive:read"),
                    capabilitiesNegotiated = false,
                    connectionTestMs = latencyMs,
                )
            }
        } else {
            at(State.PAIRED) {
                it.copy(
                    capabilities = caps,
                    capabilitiesNegotiated = true,
                    connectionTestMs = latencyMs,
                    deviceId = deviceId,
                    phraseHint = phraseHint,
                )
            }
        }
    }

    private val selectedBaseUrl: String
        get() = selectedCandidate?.urlOrHost?.trim()?.trimEnd('/') ?: ""

    /** Optional capability route; 404/ROUTE_MISSING → not negotiated (DEGRADED, honest). */
    private fun negotiateCapabilities(): List<String>? = try {
        val obj = api.capabilities(null)
        obj.optJSONArray("capabilities")?.let { arr ->
            (0 until arr.length()).map { arr.optString(it) }.filter { it.isNotEmpty() }
        } ?: emptyList()
    } catch (e: TowerApiException) {
        // Every failure class (route missing on the live Tower today included)
        // means NOT NEGOTIATED — the pairing stays valid in DEGRADED state.
        null
    }

    // ------------------------------------------------------------- controls

    /** Cancel: stops the session; durable truth untouched (nothing paired). */
    fun cancel(): Snapshot {
        sessionStore.save(null)
        envelope = null
        remainingCandidates = emptyList()
        selectedCandidate = null
        return at(if (store.isPaired()) State.PAIRED else State.UNPAIRED) { it.copy(typedCode = "CANCELLED", humanDetail = "annullato dall'utente") }
    }

    /** Retry: re-runs endpoint resolution with the SAME envelope while unexpired. */
    suspend fun retry(manualCandidates: List<EndpointCandidate> = emptyList()): Snapshot = withContext(ioDispatcher) {
        val parsed = envelope
            ?: return@withContext failed(State.FAILED, "NO_SESSION", "nessuna sessione da riprovare: scansiona un nuovo QR", "RETRY")
        if (parsed.expiresAtEpochSeconds <= clockEpochSeconds()) {
            sessionStore.save(null)
            return@withContext failed(State.EXPIRED, "QR_EXPIRED", "QR scaduto: genera un nuovo QR sul wizard della Tower", "RETRY")
        }
        resolveAndConnect(parsed, manualCandidates)
    }

    /** Revoke: wipes every encrypted pairing record (re-pair from UNPAIRED). */
    fun revoke(): Snapshot {
        store.clear()
        sessionStore.save(null)
        envelope = null
        selectedCandidate = null
        remainingCandidates = emptyList()
        return at(State.REVOKED) { it.copy(typedCode = "REVOKED", humanDetail = "console scollegata e credenziali revocate") }
    }

    /**
     * ANDROID REPAIR CP-005 — the REAL connection test for the TOWER surface
     * (mission section 4; the APP-P1-07 repair: never a hardcoded 'SIMULATED'
     * value). Falls back to the STORED endpoint after a restart. Returns
     * (ok, latencyMs) or null when no endpoint is known at all.
     */
    fun testConnection(): Pair<Boolean, Long>? {
        val base = selectedBaseUrl.ifEmpty { store.storedEndpoint()?.trimEnd('/') ?: return null }
        val started = clockMs()
        return try {
            val status = api.rawGet("$base/")
            (status in 200..599) to (clockMs() - started)
        } catch (e: TowerApiException) {
            false to (clockMs() - started)
        }
    }

    /** Re-pair: revoke + back to UNPAIRED (the UI chains revoke + a new scan). */
    fun repairAvailable(): Snapshot = at(State.REPAIR_AVAILABLE)
}

/**
 * Non-secret resume store (mission section 5: persist only appropriate durable
 * state). Holds stage/challenge-id/expiry/phrase-confirmed — NO endpoints (the
 * encrypted TowerStore owns those), NO tokens, NO codes.
 */
interface PairingSessionStore {
    data class Session(
        val stage: String,
        val pairingId: String,
        val startedAtMs: Long,
        val updatedAtMs: Long,
        val expiresAtEpochSeconds: Long,
        val phraseConfirmed: Boolean,
    )

    fun save(session: Session?)
    fun load(): Session?

    /** TEST fixture only (never wired in production). */
    class Fake : PairingSessionStore {
        private var s: Session? = null
        override fun save(session: Session?) { s = session }
        override fun load(): Session? = s
    }
}
