package com.hermestower.voiceconsole.pairing

import com.hermestower.voiceconsole.data.KeystoreFailure
import com.hermestower.voiceconsole.data.PairingStateStore
import com.hermestower.voiceconsole.net.TowerApi
import com.hermestower.voiceconsole.net.TowerApiException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.security.NoSuchAlgorithmException
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * GRAPH A4R C1-C4 — Tower pairing state machine (B lineage, RECONSTRUCTED
 * SOURCE, NOT decompiled code).
 *
 * PROVENANCE: see QrPayload.kt. Bound behavior from B's shipped binary
 * (byte-exact APK 908b289f…, forensic reading of PairingFlow.java):
 *
 *  constructor(api: TowerApi, store: TowerStore, deviceKeys: DeviceKeysContract)
 *  pairFromQr(endpoint, pairingId, numericCode, label): Step
 *      -> api.setBaseUrl(endpoint); store persistence of the endpoint;
 *         claimAndAttest(pairingId, numericCode, label)
 *  pairFromCode(endpoint, pairingId, code, label): Step  == pairFromQr
 *  claimAndAttest -> claim (single suspension) -> attest (second suspension)
 *  attest(pairingId?, claimKey, challenge) -> PendingOwnerAck(pairingId, deviceId, phraseHint)
 *  awaitOwnerAcknowledgement(): Step   (no args — pairing id held by the flow)
 *  freshPop(): String  (pop-challenge -> signChallenge -> pop_signature)
 *  Step: Claimed(pairingId, claimKey, challenge)
 *      | PendingOwnerAck(pairingId, deviceId, phraseHint)
 *      | Registered(deviceId)
 *      | Failed(reason, detail?)
 *  B typed reasons: CLAIM_FAILED, CLAIM_REJECTED, CLAIM_RESPONSE_INCOMPLETE,
 *      ATTEST_FAILED, ATTEST_REJECTED, ATTEST_RESPONSE_INCOMPLETE.
 *
 * A4R C1-C4 REPAIRS APPLIED (the whole point of the merge):
 *  - the OLD pairFromQr crashed the Owner's Xiaomi INSIDE store.setEndpoint
 *    (A4R-P1-001: KeyGenerator.init(256) without a spec). The repaired
 *    persistence returns a typed Boolean; a failure maps to
 *    Step.Failed("PAIRING_PERSISTENCE_FAILED", …) and NOTHING is committed —
 *    the pairing aborts BEFORE any server call (PATCH_NOTES_PairingFlow.md §2);
 *  - the console token released at Owner-acknowledgement is committed with the
 *    ATOMIC commitPairing (all-or-nothing), never per-field writes;
 *  - single-use challenge with a CLIENT-ENFORCED 120 s TTL (mission section 7;
 *    consistent with B's "TTL 2-5 minuti" QR note — the Tower owns issuance,
 *    the client refuses stale challenges);
 *  - replay/revocation/lockout are typed (never swallowed);
 *  - the JVM suite runs this machine against PairingStateStore.Fake,
 *    TowerApi via the TowerHttp seam, and a deterministic DeviceKeysContract
 *    fake — no Android dependency (A4R-P1-002 discipline).
 */
class PairingFlow(
    private val api: TowerApi,
    private val store: PairingStateStore,
    private val deviceKeys: DeviceKeysContract,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val clockMs: () -> Long = System::currentTimeMillis,
) {

    /** B bound contract (forensic: ensureDeviceKeyPair / signChallenge / attestationMetadata). */
    interface DeviceKeysContract {
        fun ensureDeviceKeyPair(): String
        fun signChallenge(challenge: String): String
        fun attestationMetadata(): Map<String, String>
    }

    sealed class Step {
        data class Claimed(val pairingId: String, val claimKey: String, val challenge: String) : Step()
        data class PendingOwnerAck(val pairingId: String, val deviceId: String, val phraseHint: String) : Step()
        data class Registered(val deviceId: String) : Step()
        data class Failed(val reason: String, val detail: String? = null) : Step()
    }

    /** Single-use challenge TTL (mission section 7): 120 seconds. */
    companion object {
        const val CHALLENGE_TTL_MS = 120_000L
        const val ACK_POLL_INTERVAL_MS = 2_000L
        const val ACK_WINDOW_MS = 300_000L
    }

    /** B held the pairing id across awaitOwnerAcknowledgement(); preserved. */
    @Volatile
    private var activePairingId: String? = null

    @Volatile
    private var activeChallengeAtMs: Long = 0

    /** Deterministic HMAC helper retained from B's public API (tested vector). */
    fun hmacSha256Hex(key: String, message: String): String {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(key.toByteArray(Charsets.UTF_8), "HmacSHA256"))
        return mac.doFinal(message.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }
    }

    private fun fail(reason: String, detail: String? = null): Step = Step.Failed(reason, detail)

    private fun mapApiException(e: TowerApiException, claimPhase: Boolean): Step = when (e.kind) {
        TowerApiException.Kind.NETWORK_UNREACHABLE ->
            fail("PAIRING_ENDPOINT_UNREACHABLE", e.transportCode ?: "Tower non raggiungibile")
        TowerApiException.Kind.RATE_LIMITED ->
            fail("PAIRING_CHALLENGE_REJECTED", "troppi tentativi errati: richiesta bloccata dalla Tower")
        TowerApiException.Kind.REVOKED ->
            fail("DEVICE_REVOKED", "dispositivo revocato o pairing non valido")
        TowerApiException.Kind.REJECTED ->
            fail(
                if (claimPhase) "CLAIM_REJECTED" else "ATTEST_REJECTED",
                "richiesta rifiutata dalla Tower",
            )
        TowerApiException.Kind.ROUTE_MISSING ->
            // AMB-8 honest state: the route does not exist on this server
            // version (e.g. the ack route on the live Tower). NEVER a bypass.
            fail("PAIRING_ROUTE_MISSING", "rotta non presente su questa versione della Tower: aggiornamento server richiesto")
        TowerApiException.Kind.IN_BAND_ERROR ->
            // AMB-9 repair: the Tower's typed reason (PAIRING_NOT_FOUND_OR_EXPIRED,
            // CLAIM_CODE_INVALID, …) reaches the user instead of the generic
            // 'risposta incompleta'.
            fail(inBandReason(e, claimPhase), "Tower: ${e.inBandError}")
        TowerApiException.Kind.MALFORMED_RESPONSE ->
            fail(
                if (claimPhase) "CLAIM_RESPONSE_INCOMPLETE" else "ATTEST_RESPONSE_INCOMPLETE",
                "risposta Tower incompleta",
            )
        TowerApiException.Kind.SERVER_ERROR ->
            fail(if (claimPhase) "CLAIM_FAILED" else "ATTEST_FAILED", "errore della Tower")
    }

    /** In-band Tower reason -> pairing reason (the AMB-9 catalog mapping). */
    private fun inBandReason(e: TowerApiException, claimPhase: Boolean): String = when (e.inBandError) {
        "PAIRING_NOT_FOUND_OR_EXPIRED", "PAIRING_EXPIRED" -> "PAIRING_CHALLENGE_EXPIRED"
        "PAIRING_REVOKED", "DEVICE_REVOKED" -> "DEVICE_REVOKED"
        "PAIRING_ALREADY_CLAIMED" -> "CLAIM_REJECTED"
        "CLAIM_CODE_INVALID", "CLAIM_EXHAUSTED" -> "CLAIM_REJECTED"
        "ATTEST_INVALID" -> "ATTEST_REJECTED"
        "RATE_LIMITED" -> "PAIRING_CHALLENGE_REJECTED"
        else -> if (claimPhase) "CLAIM_REJECTED" else "ATTEST_REJECTED"
    }

    /**
     * B pairFromQr with the A4R REPAIR: endpoint persistence is typed and
     * ABORTS the pairing BEFORE any server call when it fails. No partial
     * pairing state can exist.
     */
    suspend fun pairFromQr(endpoint: String, pairingId: String, numericCode: String, label: String): Step =
        withContext(ioDispatcher) {
            api.setBaseUrl(endpoint)
            if (!store.persistEndpoint(endpoint)) {
                return@withContext fail(
                    "PAIRING_PERSISTENCE_FAILED",
                    "persistenza endpoint fallita: ${store.lastFailure?.name ?: "UNKNOWN"}; nessuno stato scritto",
                )
            }
            claimAndAttest(pairingId, numericCode, label)
        }

    suspend fun pairFromCode(endpoint: String, pairingId: String, code: String, label: String): Step =
        pairFromQr(endpoint, pairingId, code, label)

    /** claim -> attest chain (B: two suspension points, lines 64/76 of the original). */
    suspend fun claimAndAttest(pairingId: String, claimSecret: String, label: String): Step {
        activePairingId = pairingId
        val claimed = try {
            val res = api.claim(pairingId, claimSecret, label)
            activeChallengeAtMs = clockMs()
            Step.Claimed(pairingId, res.claimKey, res.challenge)
        } catch (e: TowerApiException) {
            return mapApiException(e, claimPhase = true)
        }
        return attest(pairingId, claimed.claimKey, claimed.challenge)
    }

    /** attest: fresh PoP over the single-use challenge + device registration. */
    suspend fun attest(pairingId: String, claimKey: String, challenge: String): Step {
        if (clockMs() - activeChallengeAtMs > CHALLENGE_TTL_MS) {
            return fail("PAIRING_CHALLENGE_EXPIRED", "challenge scaduta (TTL ${CHALLENGE_TTL_MS / 1000} s)")
        }
        val pop = try {
            freshPop(challenge)
        } catch (e: TowerApiException) {
            return mapApiException(e, claimPhase = false)
        } catch (e: Exception) {
            return fail("ATTEST_FAILED", "firma del challenge non riuscita: ${e.javaClass.simpleName}")
        }
        val response = try {
            api.attest(
                pairingId = pairingId,
                claimKey = claimKey,
                challenge = challenge,
                popSignature = pop,
                publicPem = deviceKeys.ensureDeviceKeyPair(),
                metadata = deviceKeys.attestationMetadata(),
            )
        } catch (e: TowerApiException) {
            return mapApiException(e, claimPhase = false)
        }
        return Step.PendingOwnerAck(pairingId, response.deviceId, response.phraseHint)
    }

    /**
     * Polls the bilateral Owner acknowledgement. The console token is
     * released by the Tower ONLY at ACKED (after the Italian three-word
     * phrase match on both sides) and is committed ATOMICALLY with the
     * endpoint/deviceId — the flow NEVER commits a half-paired state.
     */
    suspend fun awaitOwnerAcknowledgement(): Step = withContext(ioDispatcher) {
        val pairingId = activePairingId
            ?: return@withContext fail("PAIRING_STATE_LOST", "nessun pairing attivo")
        val deadline = clockMs() + ACK_WINDOW_MS
        while (clockMs() < deadline) {
            val ack = try {
                api.ownerAck(pairingId)
            } catch (e: TowerApiException) {
                return@withContext mapApiException(e, claimPhase = false)
            }
            when (ack.state) {
                TowerApi.OwnerAckStateValue.ACKED -> {
                    val deviceId = ack.deviceId
                        ?: return@withContext fail("ATTEST_RESPONSE_INCOMPLETE", "ack senza device_id")
                    val consoleToken = ack.consoleToken
                        ?: return@withContext fail("ATTEST_RESPONSE_INCOMPLETE", "ack senza console_token")
                    val endpoint = api.currentBaseUrl()
                    if (!store.commitPairing(endpoint, deviceId, consoleToken, null)) {
                        return@withContext fail(
                            "PAIRING_PERSISTENCE_FAILED",
                            "commit pairing fallito: ${store.lastFailure?.name ?: "UNKNOWN"} (stato precedente conservato)",
                        )
                    }
                    return@withContext Step.Registered(deviceId)
                }
                TowerApi.OwnerAckStateValue.REVOKED ->
                    return@withContext fail("DEVICE_REVOKED", "pairing revocato dal Tower")
                TowerApi.OwnerAckStateValue.EXPIRED ->
                    return@withContext fail("PAIRING_CHALLENGE_REJECTED", "pairing scaduto: ripeti dal QR")
                TowerApi.OwnerAckStateValue.PENDING, TowerApi.OwnerAckStateValue.UNKNOWN -> delay(ACK_POLL_INTERVAL_MS)
            }
        }
        fail("OWNER_ACK_TIMEOUT", "conferma del Tower non arrivata nella finestra di attesa")
    }

    /**
     * B freshPop (line 139): fresh single-use challenge from the Tower,
     * signed with the device key. [precomputed] carries the challenge to sign
     * when the caller already holds it (attest signs THE claim challenge;
     * a standalone pop signs a fresh one).
     */
    suspend fun freshPop(precomputed: String? = null): String = withContext(ioDispatcher) {
        val challenge = precomputed ?: api.popChallenge()
        deviceKeys.signChallenge(challenge)
    }

    /** Typed failure history exposure for diagnostics (enum names only). */
    fun failureHistory(): List<KeystoreFailure> = store.failureHistory()
}
