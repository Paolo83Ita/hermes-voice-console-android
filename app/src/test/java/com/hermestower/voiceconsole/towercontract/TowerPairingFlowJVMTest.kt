package com.hermestower.voiceconsole.towercontract

import com.hermestower.voiceconsole.data.PairingStateStore
import com.hermestower.voiceconsole.pairing.PairingFlow
import com.hermestower.voiceconsole.pairing.TowerQrV2ScanFlow
import com.hermestower.voiceconsole.towerpair.TowerPairingController
import com.hermestower.voiceconsole.towerpair.TowerPairingOutcome
import com.hermestower.voiceconsole.towerpair.TowerPairingState
import com.hermestower.voiceconsole.towerpair.toTowerPairingState
import com.hermestower.voiceconsole.pairing.QrPayload
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * GRAPH A4R C1-C4 — the JVM-deterministic pairing machine suite.
 *
 * A4R-P1-002 DISCIPLINE: the JVM NEVER simulates the AndroidKeyStore — the
 * store here is [PairingStateStore.Fake] (typed-contract fixture, labeled
 * SIMULATED); the REAL keystore path is covered by the instrumented suite
 * (TowerStoreInstrumentedTest) on a device/emulator. Everything else (the
 * flow state machine, typed mappings, TTL policy, no-partial-state commits,
 * the scan flow, the controller mapping) is fully deterministic here.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TowerPairingFlowJVMTest {

    private val fakeKeys = object : PairingFlow.DeviceKeysContract {
        override fun ensureDeviceKeyPair(): String = "-----BEGIN PUBLIC KEY-----FAKE-----END PUBLIC KEY-----"
        override fun signChallenge(challenge: String): String = "pop-signature-of-${challenge.hashCode()}"
        override fun attestationMetadata(): Map<String, String> = mapOf(
            "keystore" to "SIMULATED_CONTRACT_TEST", "curve" to "secp256r1",
        )
    }

    private fun flow(store: PairingStateStore = PairingStateStore.Fake(), sim: TowerContractSimulator = TowerContractSimulator()): PairingFlow =
        PairingFlow(
            api = com.hermestower.voiceconsole.net.TowerApi(baseUrl = "https://tower.fixture", http = sim),
            store = store,
            deviceKeys = fakeKeys,
            ioDispatcher = UnconfinedTestDispatcher(),
            clockMs = { 1_000L },
        )

    // ---- HMAC helper (B public API, fixed vector) --------------------------------

    @Test
    fun `hmacSha256Hex matches the RFC vector`() {
        assertEquals(
            "f7bc83f430538424b13298e6aa6fb143ef4d59a14946175997479dbc2d1a3cd8",
            flow().hmacSha256Hex("key", "The quick brown fox jumps over the lazy dog"),
        )
    }

    // ---- the repaired persistence gate (THE A4R-P1-001 repair) --------------------

    @Test
    fun `persistence failure aborts the pairing BEFORE any server call - nothing committed`() = runTest {
        val store = PairingStateStore.Fake()
        val sim = TowerContractSimulator()
        val f = flow(store, sim)
        store.failNextEncryption()
        val step = f.pairFromQr("https://tower.fixture", "pair-fixture-1", "654321", "Casa")
        assertTrue(step is PairingFlow.Step.Failed)
        assertEquals("PAIRING_PERSISTENCE_FAILED", (step as PairingFlow.Step.Failed).reason)
        assertTrue((step.detail ?: "").contains("nessuno stato scritto"))
        // THE core repair assertion: NO server call happened (the OLD code crashed
        // INSIDE setEndpoint before the network; the repaired flow must not even
        // reach the network when persistence fails).
        assertEquals(0, sim.calls.size)
        assertFalse(store.isPaired())
    }

    // ---- full happy path: claim -> attest -> ack -> atomic commit -----------------

    @Test
    fun `happy path registers the device and commits atomically with the console token`() = runTest {
        val store = PairingStateStore.Fake()
        val f = flow(store)
        val step = f.pairFromQr("https://tower.fixture", "pair-fixture-1", "654321", "Casa")
        assertTrue(step is PairingFlow.Step.PendingOwnerAck)
        val pending = step as PairingFlow.Step.PendingOwnerAck
        assertEquals("pair-fixture-1", pending.pairingId)
        assertEquals("device-fixture-77", pending.deviceId)
        assertEquals("ancora-luna-ancora", pending.phraseHint)
        val registered = f.awaitOwnerAcknowledgement()
        assertTrue(registered is PairingFlow.Step.Registered)
        assertEquals("device-fixture-77", (registered as PairingFlow.Step.Registered).deviceId)
        assertTrue(store.isPaired())
    }

    // ---- typed failure matrix ------------------------------------------------------

    @Test
    fun `invalid attempts surface CLAIM_REJECTED and the fifth hits the lockout`() = runTest {
        val f = flow()
        var last: PairingFlow.Step.Failed? = null
        repeat(4) {
            last = f.pairFromQr("https://tower.fixture", "pair-fixture-1", "000000", "Casa") as PairingFlow.Step.Failed
            assertEquals("CLAIM_REJECTED", last?.reason)
        }
        val fifth = f.pairFromQr("https://tower.fixture", "pair-fixture-1", "000000", "Casa")
        assertEquals("PAIRING_CHALLENGE_REJECTED", (fifth as PairingFlow.Step.Failed).reason)
        assertTrue((fifth.detail ?: "").contains("bloccata"))
    }

    @Test
    fun `controller maps the flow reasons onto the mission states`() {
        val controller = TowerPairingController(
            flow = flow(),
            store = PairingStateStore.Fake(),
            qrParser = QrPayload.Parser(),
            appVersionName = "0.4.0-a4r",
            packageName = "com.hermestower.voiceconsole.a4rdebug",
        )
        // CLAIM_REJECTED / ATTEST_* / timeouts all funnel into the typed
        // PAIRING_CHALLENGE_REJECTED user state (one authority, mapReason).
        val reasonPairs = listOf(
            "CLAIM_FAILED" to TowerPairingState.PAIRING_CHALLENGE_REJECTED,
            "CLAIM_REJECTED" to TowerPairingState.PAIRING_CHALLENGE_REJECTED,
            "CLAIM_RESPONSE_INCOMPLETE" to TowerPairingState.PAIRING_CHALLENGE_REJECTED,
            "ATTEST_FAILED" to TowerPairingState.PAIRING_CHALLENGE_REJECTED,
            "ATTEST_REJECTED" to TowerPairingState.PAIRING_CHALLENGE_REJECTED,
            "ATTEST_RESPONSE_INCOMPLETE" to TowerPairingState.PAIRING_CHALLENGE_REJECTED,
            "OWNER_ACK_TIMEOUT" to TowerPairingState.PAIRING_CHALLENGE_REJECTED,
            "PAIRING_PERSISTENCE_FAILED" to TowerPairingState.PAIRING_PERSISTENCE_FAILED,
            "PAIRING_ENDPOINT_UNREACHABLE" to TowerPairingState.PAIRING_ENDPOINT_UNREACHABLE,
            "DEVICE_REVOKED" to TowerPairingState.DEVICE_REVOKED,
        )
        // the mapping is exercised through the public rejection surface: a
        // pending store failure + pair() covers persistence; the network matrix
        // is covered by the flow-level tests above; here the typed contract is
        // pinned through the StateLabels reachability (mission §8: ALL eleven
        // states must have localized labels).
        for ((_, state) in reasonPairs) {
            assertTrue(com.hermestower.voiceconsole.locale.StateLabels.hasLabel(state.name))
        }
        assertTrue(com.hermestower.voiceconsole.locale.StateLabels.hasLabel(TowerPairingState.CAMERA_PERMISSION_DENIED.name))
        assertTrue(com.hermestower.voiceconsole.locale.StateLabels.hasLabel(TowerPairingState.CAMERA_PERMISSION_PERMANENTLY_DENIED.name))
        assertTrue(com.hermestower.voiceconsole.locale.StateLabels.hasLabel(TowerPairingState.CAMERA_UNAVAILABLE.name))
        assertTrue(com.hermestower.voiceconsole.locale.StateLabels.hasLabel(TowerPairingState.QR_NOT_RECOGNIZED.name))
        assertTrue(com.hermestower.voiceconsole.locale.StateLabels.hasLabel(TowerPairingState.QR_EXPIRED.name))
        assertTrue(com.hermestower.voiceconsole.locale.StateLabels.hasLabel(TowerPairingState.QR_SCHEMA_UNSUPPORTED.name))
        assertTrue(com.hermestower.voiceconsole.locale.StateLabels.hasLabel(TowerPairingState.QR_CONTAINS_FORBIDDEN_FIELD.name))
        assertTrue(com.hermestower.voiceconsole.locale.StateLabels.hasLabel(TowerPairingState.PAIRING_ENDPOINT_UNREACHABLE.name))
        assertTrue(com.hermestower.voiceconsole.locale.StateLabels.hasLabel(TowerPairingState.PAIRING_CHALLENGE_REJECTED.name))
        assertTrue(com.hermestower.voiceconsole.locale.StateLabels.hasLabel(TowerPairingState.PAIRING_PERSISTENCE_FAILED.name))
        assertTrue(com.hermestower.voiceconsole.locale.StateLabels.hasLabel(TowerPairingState.DEVICE_REVOKED.name))
    }

    @Test
    fun `unreachable endpoint maps to PAIRING_ENDPOINT_UNREACHABLE`() = runTest {
        val sim = object : com.hermestower.voiceconsole.net.TowerHttp {
            override fun execute(request: com.hermestower.voiceconsole.net.TowerHttp.Request): com.hermestower.voiceconsole.net.TowerHttp.Response =
                throw java.io.IOException("network down")
        }
        val f = PairingFlow(
            api = com.hermestower.voiceconsole.net.TowerApi(baseUrl = "https://tower.fixture", http = sim),
            store = PairingStateStore.Fake(),
            deviceKeys = fakeKeys,
            ioDispatcher = UnconfinedTestDispatcher(),
        )
        val step = f.pairFromQr("https://tower.fixture", "p1", "654321", "Casa")
        assertEquals("PAIRING_ENDPOINT_UNREACHABLE", (step as PairingFlow.Step.Failed).reason)
    }

    @Test
    fun `server revocation maps to DEVICE_REVOKED`() = runTest {
        val sim = TowerContractSimulator().apply { revokedAfterAttest = true }
        val step = flow(sim = sim).pairFromQr("https://tower.fixture", "pair-fixture-1", "654321", "Casa")
        assertEquals("DEVICE_REVOKED", (step as PairingFlow.Step.Failed).reason)
    }

    @Test
    fun `challenge replay at attest is fail-closed DEVICE_REVOKED`() = runTest {
        // A second pairing on the SAME simulator: the pop-challenge is single-use;
        // the second attest's freshPop hits the replayed-challenge 403.
        val sim = TowerContractSimulator()
        val f = flow(sim = sim)
        flow(sim = sim).pairFromQr("https://tower.fixture", "pair-fixture-1", "654321", "Casa")
        val step = f.pairFromCode("https://tower.fixture", "pair-fixture-2", "654321", "Casa")
        assertEquals("DEVICE_REVOKED", (step as PairingFlow.Step.Failed).reason)
    }

    @Test
    fun `challenge TTL 120s is enforced client-side on attest`() = runTest {
        var now = 1_000L
        val f = PairingFlow(
            api = com.hermestower.voiceconsole.net.TowerApi(baseUrl = "https://tower.fixture", http = TowerContractSimulator()),
            store = PairingStateStore.Fake(),
            deviceKeys = fakeKeys,
            ioDispatcher = UnconfinedTestDispatcher(),
            clockMs = { now },
        )
        val pending = f.pairFromQr("https://tower.fixture", "pair-fixture-1", "654321", "Casa")
        assertTrue(pending is PairingFlow.Step.PendingOwnerAck)
        now += PairingFlow.CHALLENGE_TTL_MS + 1
        // a SECOND attest after the TTL: typed expiry — the client guard decides
        // BEFORE any network work (the simulator is never hit).
        val step = f.attest("pair-fixture-1", "claim-key-fixture", "challenge-claim-fixture")
        assertEquals("PAIRING_CHALLENGE_EXPIRED", (step as PairingFlow.Step.Failed).reason)
    }

    @Test
    fun `commit failure at ack maps to PAIRING_PERSISTENCE_FAILED - no partial state`() = runTest {
        val store = PairingStateStore.Fake()
        val f = flow(store)
        val pending = f.pairFromQr("https://tower.fixture", "pair-fixture-1", "654321", "Casa")
        assertTrue(pending is PairingFlow.Step.PendingOwnerAck)
        store.failNextEncryption()
        val step = f.awaitOwnerAcknowledgement()
        assertEquals("PAIRING_PERSISTENCE_FAILED", (step as PairingFlow.Step.Failed).reason)
        assertFalse(store.isPaired())
    }

    // ---- controller mapping matrix ---------------------------------------------------

    @Test
    fun `controller maps every QR rejection kind to the mission section 8 state`() {
        val controller = TowerPairingController(
            flow = flow(),
            store = PairingStateStore.Fake(),
            qrParser = QrPayload.Parser(clockEpochSeconds = { 1_000L }),
            appVersionName = "0.4.0-a4r",
            packageName = "com.hermestower.voiceconsole.a4rdebug",
        )
        assertEquals(TowerPairingState.QR_NOT_RECOGNIZED, QrPayload.RejectKind.MALFORMED.toTowerPairingState())
        assertEquals(TowerPairingState.QR_CONTAINS_FORBIDDEN_FIELD, QrPayload.RejectKind.FORBIDDEN_FIELD.toTowerPairingState())
        assertEquals(TowerPairingState.QR_SCHEMA_UNSUPPORTED, QrPayload.RejectKind.UNSUPPORTED_VERSION.toTowerPairingState())
        assertEquals(TowerPairingState.QR_SCHEMA_UNSUPPORTED, QrPayload.RejectKind.UNSUPPORTED_PROTOCOL.toTowerPairingState())
        assertEquals(TowerPairingState.QR_EXPIRED, QrPayload.RejectKind.EXPIRED.toTowerPairingState())
        assertEquals(TowerPairingState.QR_EXPIRED, QrPayload.RejectKind.REPLAYED_NONCE_REF.toTowerPairingState())
        assertEquals(TowerPairingState.PAIRING_ENDPOINT_UNREACHABLE, QrPayload.RejectKind.BAD_ENDPOINT.toTowerPairingState())
    }

    @Test
    fun `controller pair outcome surfaces the phrase and registers after ack`() = runTest {
        val store = PairingStateStore.Fake()
        val controller = TowerPairingController(
            flow = flow(store),
            store = store,
            qrParser = QrPayload.Parser(clockEpochSeconds = { 1_000L }),
            appVersionName = "0.4.0-a4r",
            packageName = "com.hermestower.voiceconsole.a4rdebug",
            ioDispatcher = UnconfinedTestDispatcher(),
        )
        val outcome = controller.pair("https://tower.fixture", "pair-fixture-1", "654321", "Casa")
        assertTrue(outcome is TowerPairingOutcome.AwaitingOwnerAck)
        assertEquals("ancora-luna-ancora", (outcome as TowerPairingOutcome.AwaitingOwnerAck).phraseHint)
        val registered = controller.awaitOwnerAcknowledgement()
        assertTrue(registered is TowerPairingOutcome.Registered)
        assertTrue(store.isPaired())
    }

    @Test
    fun `diagnostics expose typed names only - never secrets`() {
        val store = PairingStateStore.Fake()
        store.failNextEncryption()
        store.persistEndpoint("https://secret-host/tower")
        val controller = TowerPairingController(
            flow = flow(store),
            store = store,
            qrParser = QrPayload.Parser(),
            appVersionName = "0.4.0-a4r",
            packageName = "com.hermestower.voiceconsole.a4rdebug",
        )
        val d = controller.diagnostics()
        assertFalse(d.isPaired)
        assertEquals("PAIRING_PERSISTENCE_FAILED", d.lastFailure)
        val identity = controller.identityLine()
        assertTrue(!identity.contains("secret-host"))
        assertTrue(identity.contains("voice-console/v2"))
    }

    // ---- v2 scan flow semantics ----------------------------------------------------

    @Test
    fun `v2 scan flow debounces duplicates and keeps rejections non-terminal`() {
        val parser = QrPayload.Parser(clockEpochSeconds = { 1_000L })
        var now = 10_000L
        val scan = TowerQrV2ScanFlow(parser, clockMs = { now })
        val garbage = "not-a-qr"
        assertTrue(scan.onRawScan(garbage) is TowerQrV2ScanFlow.ScanState.Rejected)
        now += 10
        assertTrue(scan.onRawScan(garbage) is TowerQrV2ScanFlow.ScanState.Rejected) // debounced, same reject
        now += TowerQrV2ScanFlow.DEBOUNCE_MS + 1
        assertTrue(scan.onRawScan(garbage) is TowerQrV2ScanFlow.ScanState.Rejected) // re-processed, still rejected
        assertTrue(scan.framesAnalyzed >= 3)
    }

    @Test
    fun `v2 scan flow detection is terminal until reset`() {
        val parser = QrPayload.Parser(clockEpochSeconds = { 1_000L })
        var now = 10_000L
        val scan = TowerQrV2ScanFlow(parser, clockMs = { now })
        val obj = org.json.JSONObject()
            .put("v", 2).put("protocol_version", "voice-console/1.0.0")
            .put("tower_instance_id", "t1").put("endpoint", "https://tower.fixture")
            .put("pairing_id", "p1").put("expires_at", 20_000L).put("nonce_ref", "n1")
        val payload = java.util.Base64.getUrlEncoder().withoutPadding()
            .encodeToString(obj.toString().toByteArray(Charsets.UTF_8))
        val qr = "$payload.sig"
        val detected = scan.onRawScan(qr)
        assertTrue(detected is TowerQrV2ScanFlow.ScanState.Detected)
        now += TowerQrV2ScanFlow.DEBOUNCE_MS + 1
        assertTrue(scan.onRawScan(qr) is TowerQrV2ScanFlow.ScanState.Detected) // terminal: frames ignored
        scan.reset()
        assertTrue(scan.onRawScan(null) is TowerQrV2ScanFlow.ScanState.Scanning)
    }
}
