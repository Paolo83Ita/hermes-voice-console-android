package com.hermestower.voiceconsole.tower

import com.hermestower.voiceconsole.bridge.TowerEventFrame
import com.hermestower.voiceconsole.bridge.TowerRequestFrame
import com.hermestower.voiceconsole.core.state.ConnectionStatus
import com.hermestower.voiceconsole.domain.BridgeError
import com.hermestower.voiceconsole.domain.BridgeResult
import com.hermestower.voiceconsole.towersim.LocalTowerMockServer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path

/**
 * Tower Connection Studio JVM qualification (GRAPH 2 TC-01..TC-04, ST-04..ST-12,
 * ST-27..ST-29): validation rules, connection tester outcomes against the
 * deterministic local mock server, secret-clean storage, export/import conflicts.
 */
class TowerConnectionStudioTest {

    private lateinit var server: LocalTowerMockServer
    private lateinit var dir: Path

    @Before
    fun setUp() {
        server = LocalTowerMockServer()
        dir = Files.createTempDirectory("hermes-tower-test")
    }

    @After
    fun tearDown() {
        server.close()
        dir.toFile().deleteRecursively()
    }

    private fun profile(
        mode: TransportMode = TransportMode.MOCK_SIMULATOR,
        baseUrl: String = "mock://tower.simulator",
        id: String = "profile-1",
    ) = TowerConnectionProfile(
        profileId = id,
        profileName = "Studio test",
        transportMode = mode.name,
        baseUrl = baseUrl,
        createdAt = "2026-11-01T00:00:00Z",
        updatedAt = "2026-11-01T00:00:00Z",
    )

    private fun store(profileId: String = "profile-1"): FileTowerProfileStore =
        FileTowerProfileStore(dir.resolve("tower_profile.json"), InMemorySecretVault())

    // ---------- validator ----------

    @Test
    fun `mock mode bypasses production authentication requirements`() {
        val report = TowerProfileValidator.validate(profile(), secretPresent = false, nowIso = "now")
        assertTrue(report.issues.toString(), report.passed)
    }

    @Test
    fun `https requires a secret and a valid address`() {
        val report = TowerProfileValidator.validate(
            profile(mode = TransportMode.HTTPS, baseUrl = "https://tower.example.com"),
            secretPresent = false,
            nowIso = "now",
        )
        assertFalse(report.passed)
        assertTrue(report.issues.any { it.code == "SECRET_REQUIRED" })
        val ok = TowerProfileValidator.validate(
            profile(mode = TransportMode.HTTPS, baseUrl = "https://tower.example.com:8443/tower"),
            secretPresent = true,
            nowIso = "now",
        )
        assertTrue(ok.issues.toString(), ok.passed)
    }

    @Test
    fun `scheme host port and path corruption are typed`() {
        fun issues(url: String, mode: TransportMode = TransportMode.HTTPS) =
            TowerProfileValidator.validate(profile(mode = mode, baseUrl = url), true, "now").issues

        assertTrue(issues("ftp://tower.example.com").any { it.code == "SCHEME_UNSUPPORTED" })
        assertTrue(issues("tower.example.com").any { it.code == "SCHEME_MISSING" })
        assertTrue(issues("https://").any { it.code == "HOST_MISSING" })
        assertTrue(issues("https://tower.example.com:99999").any { it.code == "PORT_INVALID" })
        assertTrue(issues("https://tower .example.com").any { it.code == "WHITESPACE" })
        assertTrue(issues("wss://tower.example.com", TransportMode.HTTPS).any { it.code == "SCHEME_MISMATCH" })
    }

    @Test
    fun `timeout retry and fingerprint ranges are enforced`() {
        val p = profile().copy(requestTimeoutMs = 100, streamingTimeoutMs = 1, maximumRetryCount = 99)
        val report = TowerProfileValidator.validate(p, true, "now")
        assertTrue(report.issues.any { it.field == "requestTimeoutMs" })
        assertTrue(report.issues.any { it.field == "streamingTimeoutMs" })
        assertTrue(report.issues.any { it.field == "maximumRetryCount" })

        val pinned = profile().copy(certificatePolicy = "PINNED", certificateFingerprintReference = "abc")
        val pinnedReport = TowerProfileValidator.validate(pinned, true, "now")
        assertTrue(pinnedReport.issues.any { it.code == "FINGERPRINT_INVALID" })
    }

    @Test
    fun `tailscale mode accepts private tailscale hosts only`() {
        val ok = TowerProfileValidator.validate(
            profile(mode = TransportMode.TAILSCALE_PRIVATE, baseUrl = "http://hermes-tower.tail1234.ts.net:8080"),
            true, "now",
        )
        assertTrue(ok.issues.toString(), ok.passed)
        val okIp = TowerProfileValidator.validate(
            profile(mode = TransportMode.TAILSCALE_PRIVATE, baseUrl = "http://100.101.102.103:8080"),
            true, "now",
        )
        assertTrue(okIp.passed)
        val bad = TowerProfileValidator.validate(
            profile(mode = TransportMode.TAILSCALE_PRIVATE, baseUrl = "http://tower.example.com"),
            true, "now",
        )
        assertTrue(bad.issues.any { it.code == "NOT_TAILSCALE" })
    }

    // ---------- connection tester against the deterministic mock server ----------

    @Test
    fun `mock simulator reports passed without any network`() {
        val report = TowerConnectionTester.test(profile(), null, "now")
        assertEquals(TowerConnectionTester.Outcome.CONNECTION_PASSED, report.outcome)
        assertEquals("MOCK_NOT_REQUIRED", report.authenticationStatus)
    }

    private fun httpProfile(path: String = "/tower/api", timeoutMs: Int = 8000) = profile(
        mode = TransportMode.HTTPS,
        baseUrl = server.baseUrl,
        id = "profile-http",
    ).copy(restBasePath = path, requestTimeoutMs = timeoutMs)

    @Test
    fun `happy path passes transport protocol auth acceptance and streaming`() {
        val secret = "correct-horse".toCharArray()
        val report = TowerConnectionTester.test(httpProfile(), secret, "now")
        assertEquals(report.detail, TowerConnectionTester.Outcome.CONNECTION_PASSED, report.outcome)
        assertTrue(report.dnsResolved)
        assertTrue(report.transportConnected)
        assertEquals("NOT_ENCRYPTED", report.tlsStatus)
        assertNotNull(report.latencyMs)
        assertEquals("AVAILABLE", report.streamingCapability)
        assertEquals("Bearer correct-horse", server.lastAuthHeader[httpProfile().restBasePath])
        assertNull(report.typedFailure)
    }

    @Test
    fun `authentication failure is typed`() {
        server.setBehavior("/tower/api", LocalTowerMockServer.Behavior.AUTH_FAILURE)
        val report = TowerConnectionTester.test(httpProfile(), "wrong".toCharArray(), "now")
        assertEquals(TowerConnectionTester.Outcome.AUTHENTICATION_FAILED, report.outcome)
    }

    @Test
    fun `protocol mismatch is typed`() {
        server.setBehavior("/tower/api", LocalTowerMockServer.Behavior.PROTOCOL_MISMATCH)
        val report = TowerConnectionTester.test(httpProfile(), "x".toCharArray(), "now")
        assertEquals(TowerConnectionTester.Outcome.PROTOCOL_MISMATCH, report.outcome)
    }

    @Test
    fun `malformed response is tower rejection`() {
        server.setBehavior("/tower/api", LocalTowerMockServer.Behavior.MALFORMED_RESPONSE)
        val report = TowerConnectionTester.test(httpProfile(), "x".toCharArray(), "now")
        assertEquals(TowerConnectionTester.Outcome.TOWER_REJECTED, report.outcome)
    }

    @Test
    fun `timeout is typed with bounded wait`() {
        server.setBehavior("/tower/api", LocalTowerMockServer.Behavior.TIMEOUT)
        val report = TowerConnectionTester.test(httpProfile(timeoutMs = 1200), "x".toCharArray(), "now")
        assertEquals(TowerConnectionTester.Outcome.CONNECTION_TIMEOUT, report.outcome)
    }

    // ---------- profile store: secrets, export/import, restore, reset ----------

    @Test
    fun `metadata file never contains the secret`() {
        val store = store()
        val secretValue = "SUPER-SECRET-VALUE-42"
        store.setSecret("profile-1", secretValue.toCharArray())
        store.saveValidated(profile().copy())
        val persisted = Files.readString(dir.resolve("tower_profile.json"))
        assertFalse(persisted.contains(secretValue))
        assertTrue(persisted.contains("profileName"))
        assertEquals(secretValue, String(store.secretFor("profile-1")!!))
    }

    @Test
    fun `export is secret clean with placeholders`() {
        val store = store()
        store.setSecret("profile-1", "token-abc".toCharArray())
        store.saveValidated(profile())
        val exported = store.exportSecretClean("profile-1")
        assertEquals("ACCESS_TOKEN_REQUIRED", exported.accessToken)
        assertEquals("PAIRING_SECRET_REQUIRED", exported.pairingSecret)
        val serialized = kotlinx.serialization.json.Json.encodeToString(ExportedProfile.serializer(), exported)
        assertFalse(serialized.contains("token-abc"))
        assertTrue(serialized.contains("ACCESS_TOKEN_REQUIRED"))
    }

    @Test
    fun `import never silently overwrites an active configuration`() {
        val store = store()
        val original = profile().copy(profileName = "Original")
        store.saveValidated(original)
        val exported = store.exportSecretClean("profile-1")
        val modified = exported.copy(profile = exported.profile.copy(profileName = "Renamed"))

        val conflict = store.importExported(modified, confirmOverwrite = false)
        assertTrue(conflict is ImportResult.Conflict)
        assertEquals("Original", store.load()!!.profileName)

        val imported = store.importExported(modified, confirmOverwrite = true)
        assertTrue(imported is ImportResult.Imported)
        assertEquals("Renamed", store.load()!!.profileName)

        // previous configuration is preserved and restorable
        val restored = store.restorePrevious()
        assertTrue(restored is TowerProfileWriteResult.Written)
        assertEquals("Original", store.load()!!.profileName)
    }

    @Test
    fun `import refuses files that claim to carry real secrets`() {
        val store = store()
        store.saveValidated(profile())
        val exported = store.exportSecretClean("profile-1")
        val dirty = exported.copy(accessToken = "real-token-value")
        val result = store.importExported(dirty, confirmOverwrite = true)
        assertTrue(result is ImportResult.Rejected)
    }

    @Test
    fun `clear sensitive values keeps metadata`() {
        val store = store()
        store.setSecret("profile-1", "sekrit".toCharArray())
        store.saveValidated(profile())
        assertTrue(store.clearSensitiveValues("profile-1"))
        assertNull(store.secretFor("profile-1"))
        assertNotNull(store.load())
    }

    @Test
    fun `reset to mock returns a usable mock profile`() {
        val store = store()
        store.saveValidated(profile(mode = TransportMode.HTTPS, baseUrl = "https://tower.example.com"))
        val result = store.resetToMock()
        assertTrue(result is TowerProfileWriteResult.Written)
        assertEquals(TransportMode.MOCK_SIMULATOR.name, store.load()!!.transportMode)
    }

    // ---------- RealTowerBridge adapter against the mock server ----------

    @Test
    fun `real bridge handshake connects only after an accepted exchange`() = runBlocking {
        val store = store("profile-http")
        val bridge = RealTowerBridge(store, Dispatchers.Default)
        val p = httpProfile()
        store.setSecret(p.profileId, "tok".toCharArray())
        bridge.configure(p)
        val result = bridge.handshake()
        assertTrue((result as BridgeResult.Success).toString(), true)
        assertEquals(ConnectionStatus.BridgeStatus.CONNECTED, bridge.connection.value.status)
    }

    @Test
    fun `real bridge maps auth and protocol failures to typed errors`() = runBlocking {
        server.setBehavior("/tower/api", LocalTowerMockServer.Behavior.AUTH_FAILURE)
        val store = store("profile-http")
        val bridge = RealTowerBridge(store, Dispatchers.Default)
        bridge.configure(httpProfile())
        val result = bridge.handshake()
        assertTrue(result is BridgeResult.Failure)
        assertEquals(BridgeError.AUTH, (result as BridgeResult.Failure).error)
    }

    @Test
    fun `real bridge request exchange emits contract events with receipt`() = runBlocking {
        val store = store("profile-http")
        val bridge = RealTowerBridge(store, Dispatchers.Default)
        val p = httpProfile()
        store.setSecret(p.profileId, "tok".toCharArray())
        bridge.configure(p)
        bridge.handshake()
        val frame = TowerRequestFrame(
            deviceId = "dev", userIdRef = "owner", sessionId = "s", conversationId = "c",
            requestId = "req-1", transcript = "hello", confidence = 1f,
            requestedMode = "TOWER", effectiveMode = "TOWER", timestamp = "now",
        )
        val events = withTimeout(10_000) { bridge.sendRequest(frame).toList() }
        assertEquals(listOf("ACK", "PROGRESS", "DELTA", "FINAL"), events.map { it.type })
        assertEquals("rcp-sim-0001", events.last().receiptRef)
        assertEquals("Hello from the mock Tower.", events.last().finalText)
    }

    @Test
    fun `real bridge refuses duplicate request ids`() = runBlocking {
        val store = store("profile-http")
        val bridge = RealTowerBridge(store, Dispatchers.Default)
        bridge.configure(httpProfile())
        bridge.handshake()
        val frame = TowerRequestFrame(
            deviceId = "dev", userIdRef = "owner", sessionId = "s", conversationId = "c",
            requestId = "req-dup", transcript = "hello", confidence = 1f,
            requestedMode = "TOWER", effectiveMode = "TOWER", timestamp = "now",
        )
        bridge.sendRequest(frame).toList()
        val second = bridge.sendRequest(frame).toList()
        assertEquals(TowerEventFrame.ERROR, second.single().type)
        assertEquals(BridgeError.INVALID_REQUEST.name, second.single().error!!.classification)
    }

    @Test
    fun `real bridge refuses requests while not connected`() = runBlocking {
        val store = store("profile-http")
        val bridge = RealTowerBridge(store, Dispatchers.Default)
        bridge.configure(httpProfile()) // no handshake
        val frame = TowerRequestFrame(
            deviceId = "dev", userIdRef = "owner", sessionId = "s", conversationId = "c",
            requestId = "req-x", transcript = "hello", confidence = 1f,
            requestedMode = "TOWER", effectiveMode = "TOWER", timestamp = "now",
        )
        val events = bridge.sendRequest(frame).toList()
        assertEquals(TowerEventFrame.ERROR, events.single().type)
        assertEquals(BridgeError.TRANSPORT.name, events.single().error!!.classification)
    }
}
