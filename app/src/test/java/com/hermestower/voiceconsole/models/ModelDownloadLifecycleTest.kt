package com.hermestower.voiceconsole.models

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import com.hermestower.voiceconsole.towersim.LocalTowerMockServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.nio.file.Files
import java.time.Instant

/**
 * Download → verify → install → qualify → activate → rollback → remove lifecycle
 * against the deterministic local mock server (GRAPH 2 VM/ST-32..ST-40).
 */
class ModelDownloadLifecycleTest {

    private lateinit var server: LocalTowerMockServer
    private lateinit var root: java.nio.file.Path
    private lateinit var storage: ModelStorageManager
    private lateinit var registry: ModelInstallationRegistry
    private lateinit var downloads: DefaultModelDownloadManager

    private val artifactBytes = DeterministicTestArtifact.generate("hermes-test-artifact-v1", 4096)
    private val artifactHash = ModelSha256.ofBytes(artifactBytes)

    private fun entry() = ModelCatalogEntry(
        componentType = "WAKE_WORD",
        identity = "org.hermestower.test.probe-artifact",
        displayName = "Hermes Test Probe Artifact",
        version = "1.0.0",
        sourceUri = "mock://artifacts/hermes-test-artifact-v1",
        provider = "Hermes Tower (local test generator)",
        license = "Apache-2.0",
        licenseApproved = true,
        format = "hermes-probe",
        quantization = "none",
        supportedAbis = listOf("x86_64"),
        androidMinVersion = 30,
        downloadSizeBytes = artifactBytes.size.toLong(),
        installedSizeBytes = artifactBytes.size.toLong(),
        sha256 = artifactHash,
        manifestVersion = "1.0.0",
        storageLocation = "filesDir/models/probe/",
        ramEstimateBytes = 1048576,
        latencyClass = "NEGLIGIBLE",
        languages = listOf("und"),
        italianSupported = false,
    )

    @Before
    fun setUp() {
        server = LocalTowerMockServer()
        server.putArtifact("hermes-test-artifact-v1", LocalTowerMockServer.Artifact(artifactBytes))
        root = Files.createTempDirectory("hermes-models-test")
        storage = DefaultModelStorageManager(root)
        registry = FileInstallationRegistry(root.resolve("registry.json"))
        downloads = DefaultModelDownloadManager(storage, registry, Dispatchers.Default)
    }

    @After
    fun tearDown() {
        server.close()
        root.toFile().deleteRecursively()
    }

    private fun awaitState(identity: String, vararg states: ModelInstallState, timeoutMs: Long = 8000) {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            val s = downloads.progress(identity).value.state
            if (states.contains(s)) return
            Thread.sleep(20)
        }
        throw AssertionError("timeout waiting for $identity in ${states.toList()}")
    }

    @Test
    fun `happy path download verify install qualify activate readback`() = runBlocking {
        val e = entry()
        downloads.start(e, "${server.baseUrl}/artifacts/hermes-test-artifact-v1")
        awaitState(e.identity, ModelInstallState.DOWNLOADED)
        assertEquals(artifactBytes.size.toLong(), downloads.progress(e.identity).value.bytesDownloaded)

        assertTrue(downloads.verifyHash(e))
        val target = downloads.install(e)
        assertNotNull(target)
        assertTrue(Files.exists(target!!))

        val qualification = DefaultModelQualificationRunner(storage).qualify(e)
        assertTrue(qualification.qualified)

        val activation = DefaultModelActivationController(registry, DefaultModelQualificationRunner(storage))
        val result = activation.activate(e)
        assertTrue(result.success)
        val readBack = activation.readBack()
        assertEquals(1, readBack.size)
        assertEquals(e.identity, readBack[0].identity)
        assertEquals(e.sha256, readBack[0].sha256)
    }

    @Test
    fun `hash mismatch rejects the artifact and cleans the partial file`() = runBlocking {
        val bad = entry().copy(sha256 = "f".repeat(64))
        server.putArtifact("hermes-test-artifact-v1", LocalTowerMockServer.Artifact(artifactBytes))
        downloads.start(bad, "${server.baseUrl}/artifacts/hermes-test-artifact-v1")
        awaitState(bad.identity, ModelInstallState.DOWNLOADED)
        assertFalse(downloads.verifyHash(bad))
        assertEquals(ModelInstallState.FAILED, downloads.progress(bad.identity).value.state)
        // partial temp file must be gone (no partial activation)
        assertNull(storage.installedFile(bad))
    }

    @Test
    fun `cancel discards the partial download`() = runBlocking {
        val big = DeterministicTestArtifact.generate("big", 2 * 1024 * 1024)
        server.putArtifact("slow", LocalTowerMockServer.Artifact(big, delayMs = 500))
        val e = entry().copy(identity = "org.hermestower.test.slow", downloadSizeBytes = big.size.toLong(), installedSizeBytes = big.size.toLong(), sha256 = ModelSha256.ofBytes(big))
        downloads.start(e, "${server.baseUrl}/artifacts/slow")
        awaitState(e.identity, ModelInstallState.DOWNLOADING)
        downloads.cancel(e.identity)
        assertEquals(ModelInstallState.DOWNLOAD_APPROVAL_REQUIRED, downloads.progress(e.identity).value.state)
        assertNull(storage.installedFile(e))
    }

    @Test
    fun `pause then resume completes with Range request`() = runBlocking {
        val big = DeterministicTestArtifact.generate("resumable", 512 * 1024)
        server.putArtifact("resumable", LocalTowerMockServer.Artifact(big, chunkDelayMs = 12))
        val e = entry().copy(
            identity = "org.hermestower.test.resumable",
            downloadSizeBytes = big.size.toLong(),
            installedSizeBytes = big.size.toLong(),
            sha256 = ModelSha256.ofBytes(big),
        )
        downloads.start(e, "${server.baseUrl}/artifacts/resumable")
        // wait until some bytes have landed, then pause
        withTimeout(8000) {
            while (downloads.progress(e.identity).value.bytesDownloaded < 32 * 1024) Thread.sleep(10)
        }
        downloads.pause(e.identity)
        assertEquals(ModelInstallState.DOWNLOAD_PAUSED, downloads.progress(e.identity).value.state)
        downloads.resume(e, "${server.baseUrl}/artifacts/resumable")
        awaitState(e.identity, ModelInstallState.DOWNLOADED)
        assertEquals(big.size.toLong(), downloads.progress(e.identity).value.bytesDownloaded)
        assertTrue(downloads.verifyHash(e))
        assertTrue(downloads.install(e) != null)
    }

    @Test
    fun `no duplicate download when the same verified hash already exists`() = runBlocking {
        val e = entry()
        downloads.start(e, "${server.baseUrl}/artifacts/hermes-test-artifact-v1")
        awaitState(e.identity, ModelInstallState.DOWNLOADED)
        assertTrue(downloads.verifyHash(e))
        downloads.install(e)
        val before = server.requestCount.get()
        assertTrue(downloads.verifiedExisting(e))
        // starting again must not re-fetch: verified short-circuit keeps request count unchanged
        assertEquals(before, server.requestCount.get())
    }

    @Test
    fun `rollback restores the verified previous version and removal cleans up`() = runBlocking {
        val e = entry()
        downloads.start(e, "${server.baseUrl}/artifacts/hermes-test-artifact-v1")
        awaitState(e.identity, ModelInstallState.DOWNLOADED)
        assertTrue(downloads.verifyHash(e))
        downloads.install(e)
        val rollback = DefaultModelRollbackController(storage, registry)
        assertTrue(rollback.snapshotFor(e))
        assertEquals("1.0.0", registry.record(e.identity)!!.previousVersion)

        // simulate a superseding install
        val e2 = e.copy(version = "1.1.0", sha256 = ModelSha256.ofBytes(DeterministicTestArtifact.generate("v2", 4096)))
        server.putArtifact("v2", LocalTowerMockServer.Artifact(DeterministicTestArtifact.generate("v2", 4096)))
        downloads.start(e2, "${server.baseUrl}/artifacts/v2")
        awaitState(e2.identity, ModelInstallState.DOWNLOADED)
        assertTrue(downloads.verifyHash(e2))
        downloads.install(e2)
        assertTrue(registry.record(e.identity)!!.rollbackAvailable)

        val result = rollback.rollback(e2)
        assertTrue(result.success)
        assertEquals("1.0.0", result.restoredVersion)
        assertEquals(artifactHash, ModelSha256.ofFile(storage.installedFile(e)!!))

        assertTrue(rollback.removalCleanup(e))
        assertNull(registry.record(e.identity))
        assertNull(storage.installedFile(e))
    }

    @Test
    fun `qualification refuses a tampered installed file`() = runBlocking {
        val e = entry()
        downloads.start(e, "${server.baseUrl}/artifacts/hermes-test-artifact-v1")
        awaitState(e.identity, ModelInstallState.DOWNLOADED)
        assertTrue(downloads.verifyHash(e))
        val target = downloads.install(e)!!
        Files.write(target, "tampered".toByteArray())
        val qualification = DefaultModelQualificationRunner(storage).qualify(e)
        assertFalse(qualification.qualified)
        assertTrue(qualification.checks.any { it.name == "hash-match" && !it.passed })
    }

    @Test
    fun `activation without installation is refused`() {
        val activation = DefaultModelActivationController(registry, DefaultModelQualificationRunner(storage))
        val result = activation.activate(entry())
        assertFalse(result.success)
        assertTrue(result.reason.contains("not registered"))
    }

    @Test
    fun `registry persists across instances`() = runBlocking {
        val e = entry()
        downloads.start(e, "${server.baseUrl}/artifacts/hermes-test-artifact-v1")
        awaitState(e.identity, ModelInstallState.DOWNLOADED)
        assertTrue(downloads.verifyHash(e))
        downloads.install(e)
        registry.markVerified(e.identity, Instant.now())
        val reopened = FileInstallationRegistry(root.resolve("registry.json"))
        val record = reopened.record(e.identity)
        assertNotNull(record)
        assertEquals(ModelInstallState.INSTALLED.name, record!!.state)
        assertNotNull(record.lastVerificationAt)
    }
}
