package com.hermestower.voiceconsole.pairing

import com.hermestower.voiceconsole.data.PairingStateStore
import com.hermestower.voiceconsole.net.TowerApi
import com.hermestower.voiceconsole.net.TowerHttp
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.net.UnknownHostException
import java.util.Base64

/**
 * ANDROID REPAIR CP-005 — the JVM-deterministic pairing envelope/resolver/
 * session-machine suite (mission section 14: pairing parser, endpoint
 * resolver, network classification, TTL, nonce, replay, state machine).
 *
 * A4R-P1-002 DISCIPLINE unchanged: no AndroidKeyStore simulation — the store
 * is [PairingStateStore.Fake]; the REAL keystore path is the instrumented
 * suite. The fixture Tower here speaks the v2/v3 contract truth INCLUDING the
 * in-band {ok:...} envelope (AMB-9) and the ack-route-missing case (AMB-8).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PairingEndpointResolutionJVMTest {

    // ------------------------------------------------------------ fixtures

    /** v2/v3 contract fixture with ok-envelopes, configurable route failures. */
    private class TowerV3HttpFixture : TowerHttp {
        var ackRouteMissing = false
        var capabilitiesRouteMissing = false
        var claimInBandError: String? = null
        var probeFailForHost: String? = null
        val calls = ArrayDeque<String>()

        override fun execute(request: TowerHttp.Request): TowerHttp.Response {
            calls += "${request.method} ${request.url}"
            val path = request.url.substringAfter("://").substringAfter('/').substringBefore('?')
            val host = request.url.substringAfter("://").substringBefore('/')
            if (request.method == "GET" && path.isEmpty()) {
                probeFailForHost?.let { if (host == it) throw UnknownHostException("$it: Name or service not known") }
                return TowerHttp.Response(200, "{\"ok\":true,\"root\":true}")
            }
            return when {
                path.endsWith("/claim") -> claimInBandError?.let { code ->
                    TowerHttp.Response(200, "{\"ok\":false,\"error\":\"$code\"}")
                } ?: TowerHttp.Response(
                    200, "{\"ok\":true,\"claim_key\":\"ck-1\",\"challenge\":\"ch-1\"}",
                )
                path.endsWith("/attest") -> TowerHttp.Response(
                    200, "{\"ok\":true,\"device_id\":\"dev-77\",\"phrase_hint\":\"ancora-luna-ancora\"}",
                )
                path.endsWith("/ack") -> if (ackRouteMissing) {
                    TowerHttp.Response(404, "{\"detail\":\"No such API endpoint: $path\"}")
                } else {
                    TowerHttp.Response(
                        200,
                        "{\"ok\":true,\"state\":\"ACKED\",\"device_id\":\"dev-77\",\"console_token\":\"tok-1\"}",
                    )
                }
                path.endsWith("/capabilities") -> if (capabilitiesRouteMissing) {
                    TowerHttp.Response(404, "{\"detail\":\"No such API endpoint\"}")
                } else {
                    TowerHttp.Response(200, "{\"ok\":true,\"capabilities\":[\"voice\",\"archive:read\"]}")
                }
                else -> TowerHttp.Response(404, "{\"detail\":\"No such API endpoint\"}")
            }
        }
    }

    private val fakeKeys = object : PairingFlow.DeviceKeysContract {
        override fun ensureDeviceKeyPair(): String = "-----BEGIN PUBLIC KEY-----FAKE-----END PUBLIC KEY-----"
        override fun signChallenge(challenge: String): String = "pop-of-$challenge"
        override fun attestationMetadata(): Map<String, String> = mapOf("keystore" to "FIXTURE")
    }

    private var nowSec = 1_000_000L

    private fun qr(payloadJson: String): String =
        Base64.getUrlEncoder().encodeToString(payloadJson.toByteArray()) + "." + Base64.getUrlEncoder().encodeToString("sig".toByteArray())

    private fun v3Qr(vararg candidates: JSONObject): String = qr(
        JSONObject()
            .put("v", 3)
            .put("protocol_version", "voice-console/2")
            .put("tower_public_id", "hermes-tower-1")
            .put("challenge_id", "pair-1")
            .put("nonce_hash_or_reference", "nref-1")
            .put("issued_at", nowSec)
            .put("expires_at", nowSec + 240)
            .put("verification_phrase_reference", JSONObject().put("kind", "REFERENCE_ONLY").put("word_count", 3).put("language", "it"))
            .put("endpoint_candidates", org.json.JSONArray().apply { candidates.forEach { put(it) } })
            .put("scope_request", org.json.JSONArray().put("voice"))
            .put("capability_negotiation_version", "capneg/1")
            .toString(),
    )

    private fun candidate(cls: String, url: String, priority: Int, netReq: String, tls: String = "CA_VALIDATED") = JSONObject()
        .put("transport_class", cls)
        .put("url_or_host", url)
        .put("priority", priority)
        .put("network_requirement", netReq)
        .put("tls_requirement", tls)
        .put("freshness", "AT_ISSUE")
        .put("expiry", nowSec + 240)
        .put("source", "TOWER_QR")
        .put("qualification_state", "QUALIFIED")

    private fun publicCandidate() = candidate("QUALIFIED_PUBLIC_ENDPOINT", "https://tower.fixture", 10, "INTERNET_ANY")
    private fun lanIpCandidate() = candidate("LAN_IP", "https://192.168.1.50:8443", 5, "LAN_SAME_SUBNET", "LAN_SELF_SIGNED_EXPECTED")
    private fun mdnsCandidate() = candidate("LAN_MDNS", "tower.local", 1, "LAN_SAME_SUBNET", "LAN_SELF_SIGNED_EXPECTED")

    private fun session(
        fixture: TowerV3HttpFixture = TowerV3HttpFixture(),
        network: NetworkContextType = NetworkContextType.WIFI,
        store: PairingStateStore = PairingStateStore.Fake(),
        sessionStore: PairingSessionStore = PairingSessionStore.Fake(),
    ): PairingSessionController {
        val api = TowerApi(baseUrl = "https://tower.fixture", http = fixture)
        return PairingSessionController(
            api = api,
            store = store,
            flow = PairingFlow(api, store, fakeKeys, UnconfinedTestDispatcher(), { 1_000L }),
            resolver = EndpointResolver(fixture, { network }, { nowSec }),
            sessionStore = sessionStore,
            deviceKeys = fakeKeys,
            ioDispatcher = UnconfinedTestDispatcher(),
            clockMs = { 1_000L },
            clockEpochSeconds = { nowSec },
        )
    }

    // --------------------------------------------------- envelope parser (v3/v2)

    @Test
    fun `v3 envelope parses multi-candidate with transport classes`() {
        val parsed = PairingEnvelope.Parser { nowSec }.parse(
            v3Qr(publicCandidate(), lanIpCandidate(), mdnsCandidate()),
        )
        assertEquals(3, parsed.candidates.size)
        assertEquals(setOf(TransportClass.QUALIFIED_PUBLIC_ENDPOINT, TransportClass.LAN_IP, TransportClass.LAN_MDNS), parsed.candidates.map { it.transportClass }.toSet())
        assertEquals("capneg/1", parsed.capabilityNegotiationVersion)
        assertEquals(3, parsed.phraseWordCount)
    }

    @Test
    fun `v2 envelope becomes one classified candidate - tower_dot_local is LAN_MDNS`() {
        val v2 = qr(
            JSONObject()
                .put("v", 2)
                .put("protocol_version", "voice-console/2")
                .put("tower_instance_id", "hermes-tower-1")
                .put("pairing_id", "pair-1")
                .put("nonce_ref", "nref-2")
                .put("endpoint", "https://tower.local")
                .put("expires_at", nowSec + 200)
                .toString(),
        )
        val parsed = PairingEnvelope.Parser { nowSec }.parse(v2)
        assertEquals(1, parsed.candidates.size)
        assertEquals(TransportClass.LAN_MDNS, parsed.candidates[0].transportClass)
        assertEquals(TlsRequirement.LAN_SELF_SIGNED_EXPECTED, parsed.candidates[0].tlsRequirement)
    }

    @Test
    fun `forbidden field fails closed before anything else`() {
        val bad = qr(JSONObject().put("v", 3).put("api_key", "x").toString())
        try {
            PairingEnvelope.Parser { nowSec }.parse(bad)
            throw AssertionError("must reject")
        } catch (e: PairingEnvelope.EnvelopeRejectException) {
            assertEquals(PairingEnvelope.RejectKind.FORBIDDEN_FIELD, e.reject.kind)
        }
    }

    @Test
    fun `expired envelope is rejected before any network use`() {
        val expired = qr(
            JSONObject().put("v", 3).put("protocol_version", "voice-console/2")
                .put("tower_public_id", "t").put("challenge_id", "c")
                .put("nonce_hash_or_reference", "n")
                .put("issued_at", nowSec - 400).put("expires_at", nowSec - 10)
                .put("endpoint_candidates", org.json.JSONArray().put(publicCandidate()))
                .toString(),
        )
        try {
            PairingEnvelope.Parser { nowSec }.parse(expired)
            throw AssertionError("must reject")
        } catch (e: PairingEnvelope.EnvelopeRejectException) {
            assertEquals(PairingEnvelope.RejectKind.EXPIRED, e.reject.kind)
        }
    }

    @Test
    fun `replayed nonce_ref is rejected - single use`() {
        val qrText = v3Qr(publicCandidate())
        PairingEnvelope.Parser { nowSec }.parse(qrText)
        try {
            PairingEnvelope.Parser { nowSec }.parse(qrText)
            throw AssertionError("must reject replay")
        } catch (e: PairingEnvelope.EnvelopeRejectException) {
            assertEquals(PairingEnvelope.RejectKind.REPLAYED_NONCE_REF, e.reject.kind)
        }
    }

    // ------------------------------------------------------- resolver rules

    @Test
    fun `THE OWNER PHYSICAL SCENARIO - cellular plus LAN-only envelope fails typed NO_ELIGIBLE_ENDPOINT`() = runTest {
        // The 2026-11-20 physical failure: QR with tower.local only, phone on 4G.
        val resolver = EndpointResolver(TowerV3HttpFixture(), { NetworkContextType.CELLULAR }, { nowSec })
        val parsed = PairingEnvelope.Parser { nowSec }.parse(v3Qr(mdnsCandidate()))
        val result = resolver.resolve(parsed.candidates)
        assertTrue(result is EndpointResolutionResult.Failed)
        assertEquals(TransportFailureCode.NO_ELIGIBLE_ENDPOINT, (result as EndpointResolutionResult.Failed).primaryCode)
        assertTrue(result.evidence.any { it.status == CandidateEvidence.CandidateStatus.SKIPPED_NOT_ELIGIBLE })
    }

    @Test
    fun `multi-candidate on cellular selects the public endpoint and skips LAN with evidence`() = runTest {
        val resolver = EndpointResolver(TowerV3HttpFixture(), { NetworkContextType.CELLULAR }, { nowSec })
        val parsed = PairingEnvelope.Parser { nowSec }.parse(v3Qr(publicCandidate(), lanIpCandidate(), mdnsCandidate()))
        val result = resolver.resolve(parsed.candidates)
        assertTrue(result is EndpointResolutionResult.Selected)
        val sel = result as EndpointResolutionResult.Selected
        assertEquals(TransportClass.QUALIFIED_PUBLIC_ENDPOINT, sel.candidate.transportClass)
        // LAN candidates skipped with the honest per-candidate reason
        assertTrue(sel.evidence.any { it.typedCode == TransportFailureCode.CELLULAR_CANNOT_REACH_LAN_ENDPOINT })
        assertTrue(sel.evidence.any { it.typedCode == TransportFailureCode.MDNS_UNAVAILABLE })
    }

    @Test
    fun `DNS failure on first candidate falls through to the next`() = runTest {
        val fixture = TowerV3HttpFixture().apply { probeFailForHost = "tower.fixture" }
        val resolver = EndpointResolver(fixture, { NetworkContextType.WIFI }, { nowSec })
        val manual = EndpointCandidate(
            transportClass = TransportClass.MANUAL_ADVANCED,
            urlOrHost = "https://manual.fixture",
            priority = 50,
            networkRequirement = NetworkRequirement.INTERNET_ANY,
            tlsRequirement = TlsRequirement.CA_VALIDATED,
            expiryEpochSeconds = nowSec + 240,
            source = EndpointCandidate.CandidateSource.MANUAL_ADVANCED,
            qualified = true,
        )
        val parsed = PairingEnvelope.Parser { nowSec }.parse(v3Qr(publicCandidate()))
        val result = resolver.resolve(parsed.candidates, listOf(manual))
        assertTrue(result is EndpointResolutionResult.Selected)
        assertEquals("https://manual.fixture", (result as EndpointResolutionResult.Selected).candidate.urlOrHost)
        assertTrue(result.evidence.any { it.typedCode == TransportFailureCode.DNS_RESOLUTION_FAILED })
    }

    @Test
    fun `expired and malformed candidates are skipped never attempted`() = runTest {
        val resolver = EndpointResolver(TowerV3HttpFixture(), { NetworkContextType.WIFI }, { nowSec })
        val expired = EndpointCandidate(
            TransportClass.QUALIFIED_PUBLIC_ENDPOINT, "https://expired.fixture", 1,
            NetworkRequirement.INTERNET_ANY, TlsRequirement.CA_VALIDATED,
            nowSec - 5, EndpointCandidate.CandidateSource.TOWER_QR, true,
        )
        val malformed = EndpointCandidate(
            TransportClass.LAN_IP, "not-a-url", 2,
            NetworkRequirement.LAN_SAME_SUBNET, TlsRequirement.LAN_SELF_SIGNED_EXPECTED,
            nowSec + 240, EndpointCandidate.CandidateSource.TOWER_QR, true,
        )
        val result = resolver.resolve(listOf(expired, malformed, publicCandidateFromModel()))
        assertTrue(result is EndpointResolutionResult.Selected)
        val sel = result as EndpointResolutionResult.Selected
        assertTrue(sel.evidence.any { it.status == CandidateEvidence.CandidateStatus.SKIPPED_EXPIRED })
        assertTrue(sel.evidence.any { it.status == CandidateEvidence.CandidateStatus.SKIPPED_MALFORMED })
    }

    private fun publicCandidateFromModel() = EndpointCandidate(
        TransportClass.QUALIFIED_PUBLIC_ENDPOINT, "https://tower.fixture", 10,
        NetworkRequirement.INTERNET_ANY, TlsRequirement.CA_VALIDATED,
        nowSec + 240, EndpointCandidate.CandidateSource.TOWER_QR, true,
    )

    @Test
    fun `offline network fails typed OFFLINE_NO_NETWORK before any candidate`() = runTest {
        val resolver = EndpointResolver(TowerV3HttpFixture(), { NetworkContextType.NONE }, { nowSec })
        val result = resolver.resolve(listOf(publicCandidateFromModel()))
        assertTrue(result is EndpointResolutionResult.Failed)
        assertEquals(TransportFailureCode.OFFLINE_NO_NETWORK, (result as EndpointResolutionResult.Failed).primaryCode)
    }

    // ------------------------------------------------- transport classifier

    @Test
    fun `classifier distinguishes DNS vs mDNS vs TCP vs TLS classes`() {
        assertEquals(TransportFailureCode.DNS_RESOLUTION_FAILED, TransportClassifier.classify(UnknownHostException("nope.example.com")))
        assertEquals(TransportFailureCode.MDNS_UNAVAILABLE, TransportClassifier.classify(UnknownHostException("tower.local"), "tower.local"))
        assertEquals(
            TransportFailureCode.TCP_CONNECTION_FAILED,
            TransportClassifier.classify(java.net.ConnectException("Connection refused")),
        )
        assertEquals(
            TransportFailureCode.TLS_HOSTNAME_MISMATCH,
            TransportClassifier.classify(javax.net.ssl.SSLPeerUnverifiedException("peer not authenticated")),
        )
        assertEquals(
            TransportFailureCode.CLEARTEXT_BLOCKED,
            TransportClassifier.classify(IOException("Cleartext HTTP traffic to tower.local not permitted")),
        )
    }

    // ------------------------------------------------ session state machine

    @Test
    fun `happy path walks the canonical states to PAIRED with real connection test and capabilities`() = runTest {
        val store = PairingStateStore.Fake()
        val sessionStore = PairingSessionStore.Fake()
        val controller = session(network = NetworkContextType.CELLULAR, store = store, sessionStore = sessionStore)
        val snap = controller.startFromEnvelope(v3Qr(publicCandidate(), lanIpCandidate()), "654321", "Casa")
        // stops at the phrase gate (bilateral confirmation, mission section 5)
        assertEquals(PairingSessionController.State.PHRASE_MATCH_REQUIRED, snap.state)
        assertEquals("ancora-luna-ancora", snap.phraseHint)
        val afterConfirm = controller.confirmPhraseMatch()
        assertEquals(PairingSessionController.State.PAIRED, afterConfirm.state)
        assertTrue(afterConfirm.capabilitiesNegotiated)
        assertTrue(afterConfirm.capabilities.contains("voice"))
        assertNotNull(afterConfirm.connectionTestMs)
        assertTrue(store.isPaired())
        assertEquals(null, sessionStore.load())
    }

    @Test
    fun `ack route missing ends REPAIR_AVAILABLE - never a fake PAIRED - AMB-8 honest state`() = runTest {
        val fixture = TowerV3HttpFixture().apply { ackRouteMissing = true }
        val controller = session(fixture = fixture, network = NetworkContextType.WIFI)
        controller.startFromEnvelope(v3Qr(publicCandidate()), "654321", "Casa")
        val snap = controller.confirmPhraseMatch()
        assertEquals(PairingSessionController.State.REPAIR_AVAILABLE, snap.state)
        assertEquals("PAIRING_ROUTE_MISSING", snap.typedCode)
        assertTrue((snap.humanDetail ?: "").contains("delta server"))
    }

    @Test
    fun `in-band error envelope surfaces the Tower typed reason - AMB-9 repair`() = runTest {
        val fixture = TowerV3HttpFixture().apply { claimInBandError = "PAIRING_NOT_FOUND_OR_EXPIRED" }
        val controller = session(fixture = fixture, network = NetworkContextType.WIFI)
        val snap = controller.startFromEnvelope(v3Qr(publicCandidate()), "000000", "Casa")
        assertEquals(PairingSessionController.State.EXPIRED, snap.state)
        assertEquals("PAIRING_CHALLENGE_EXPIRED", snap.typedCode)
        assertTrue((snap.humanDetail ?: "").contains("PAIRING_NOT_FOUND_OR_EXPIRED"))
    }

    @Test
    fun `capability route missing degrades honestly to DEGRADED not-negotiated`() = runTest {
        val fixture = TowerV3HttpFixture().apply { capabilitiesRouteMissing = true }
        val store = PairingStateStore.Fake()
        val controller = session(fixture = fixture, network = NetworkContextType.WIFI, store = store)
        controller.startFromEnvelope(v3Qr(publicCandidate()), "654321", "Casa")
        val snap = controller.confirmPhraseMatch()
        assertEquals(PairingSessionController.State.DEGRADED, snap.state)
        // DEGRADED still means REGISTERED durable truth (the encrypted store holds the pairing)
        assertTrue(store.isPaired())
        assertEquals(false, snap.capabilitiesNegotiated)
    }

    @Test
    fun `cellular with LAN-only envelope ends typed FAILED NO_ELIGIBLE_ENDPOINT at session level`() = runTest {
        val controller = session(network = NetworkContextType.CELLULAR)
        val snap = controller.startFromEnvelope(v3Qr(mdnsCandidate()), "654321", "Casa")
        assertEquals(PairingSessionController.State.FAILED, snap.state)
        assertEquals(TransportFailureCode.NO_ELIGIBLE_ENDPOINT.name, snap.typedCode)
    }

    @Test
    fun `readBack reconstructs PAIRED from durable truth after restart`() = runTest {
        val store = PairingStateStore.Fake()
        store.commitPairing("https://tower.fixture", "dev-77", "tok-1", "Casa")
        val controller = session(store = store)
        val snap = controller.readBack()
        assertEquals(PairingSessionController.State.PAIRED, snap.state)
        assertEquals("dev-77", snap.deviceId)
    }

    @Test
    fun `revoke wipes durable truth and returns REVOKED then re-pair starts UNPAIRED`() = runTest {
        val store = PairingStateStore.Fake()
        store.commitPairing("https://tower.fixture", "dev-77", "tok-1", "Casa")
        val controller = session(store = store)
        controller.readBack()
        val revoked = controller.revoke()
        assertEquals(PairingSessionController.State.REVOKED, revoked.state)
        val fresh = controller.cancel()
        assertTrue(fresh.state == PairingSessionController.State.UNPAIRED)
    }

    @Test
    fun `numeric code lane works without camera - startFromCode reaches the phrase gate`() = runTest {
        val controller = session(network = NetworkContextType.WIFI)
        val snap = controller.startFromCode(
            pairingId = "pair-9",
            claimCode = "654321",
            label = "Casa",
            candidates = listOf(publicCandidateFromModel()),
        )
        assertEquals(PairingSessionController.State.PHRASE_MATCH_REQUIRED, snap.state)
    }

    @Test
    fun `resume after interruption reloads the session within the TTL`() = runTest {
        val sessionStore = PairingSessionStore.Fake()
        sessionStore.save(
            PairingSessionStore.Session(
                stage = "AWAITING_OWNER_CONFIRMATION",
                pairingId = "pair-1",
                startedAtMs = 900L,
                updatedAtMs = 990L,
                expiresAtEpochSeconds = nowSec + 100,
                phraseConfirmed = false,
            ),
        )
        val controller = session(sessionStore = sessionStore, network = NetworkContextType.WIFI)
        val snap = controller.resume("654321", listOf(publicCandidateFromModel()))
        // resume re-runs claim+attest on the manual candidate and stops at the gate
        assertEquals(PairingSessionController.State.PHRASE_MATCH_REQUIRED, snap.state)
    }
}
