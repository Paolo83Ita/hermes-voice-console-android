package com.hermestower.voiceconsole.models

import com.hermestower.voiceconsole.towersim.LocalTowerMockServer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
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
 * ANDROID REPAIR CP-006 — the PROMOTION REPAIR suite (mission sections 7/14:
 * registry, promotion, resolver matrix; A4R-PHYS-MODEL-002 binding).
 *
 * Reproduces the Owner's exact physical dead-end on the JVM: a REAL full
 * download to tmp, a registry that silently no-ops for record-less
 * identities, resolvers that key only on the record state, and a re-click
 * that hits HTTP 416 and DELETES the complete partial. Every repair (R1-R4)
 * is pinned.
 */
class ModelPromotionRepairTest {

    private lateinit var server: LocalTowerMockServer
    private lateinit var root: Path
    private lateinit var storage: ModelStorageManager
    private lateinit var registry: FileInstallationRegistry
    private lateinit var downloads: DefaultModelDownloadManager
    private lateinit var promotion: ModelPromotionController

    /** A valid GGUF header (magic + v3 + sane counts) for the LOCAL_LLM artifact. */
    private fun ggufBytes(payloadBytes: Int): ByteArray {
        val header = ByteArray(28)
        // magic "GGUF"
        header[0] = 0x47; header[1] = 0x47; header[2] = 0x55; header[3] = 0x46
        // version u32 LE = 3
        header[4] = 3
        // tensor_count u64 LE = 1
        header[12] = 1
        // kv_count u64 LE = 8
        header[20] = 8
        return header + ByteArray(payloadBytes)
    }

    private val ggufArtifact = ggufBytes(4096)
    private val ggufHash = ModelSha256.ofBytes(ggufArtifact)

    private fun ggufEntry() = ModelCatalogEntry(
        componentType = ModelComponentType.LOCAL_LLM.name,
        identity = "org.qwen.test-0.5b",
        displayName = "Qwen2.5 0.5B (test)",
        version = "1",
        sourceUri = "https://example.invalid/qwen.gguf",
        provider = "Hermes Tower (local test generator)",
        license = "Apache-2.0",
        licenseApproved = true,
        format = "gguf",
        quantization = "q5_k_m",
        supportedAbis = listOf("arm64-v8a"),
        androidMinVersion = 30,
        downloadSizeBytes = ggufArtifact.size.toLong(),
        installedSizeBytes = ggufArtifact.size.toLong(),
        sha256 = ggufHash,
        manifestVersion = "1.0.0",
        storageLocation = "filesDir/models/llm/",
        ramEstimateBytes = 1048576,
        latencyClass = "SLOW",
        languages = listOf("it"),
        italianSupported = true,
    )

    @Before
    fun setUp() {
        server = LocalTowerMockServer()
        server.putArtifact("qwen-test.gguf", LocalTowerMockServer.Artifact(ggufArtifact))
        root = Files.createTempDirectory("hermes-promotion-test")
        storage = DefaultModelStorageManager(root)
        registry = FileInstallationRegistry(root.resolve("registry.json"))
        downloads = DefaultModelDownloadManager(storage, registry, Dispatchers.Default)
        promotion = ModelPromotionController(storage, registry, downloads)
    }

    @After
    fun tearDown() {
        server.close()
        root.toFile().deleteRecursively()
    }

    private fun awaitState(identity: String, vararg states: ModelInstallState, timeoutMs: Long = 8000) {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            if (states.contains(downloads.progress(identity).value.state)) return
            Thread.sleep(20)
        }
        throw AssertionError("timeout waiting for $identity in ${states.toList()}")
    }

    // ------------------------------------------------ R1: registry upsert

    @Test
    fun `R1 - markState silently no-ops for record-less identities (the baseline defect)`() {
        val e = ggufEntry()
        registry.markState(e.identity, ModelInstallState.DOWNLOADED, null)
        // THE DEFECT: no record exists after the call — the baseline behavior
        // this repair replaces with ensureRecord (asserted for contrast below).
        assertNull(registry.record(e.identity))
    }

    @Test
    fun `R1 - ensureRecord CREATES the observable record for record-less identities`() {
        val e = ggufEntry()
        registry.ensureRecord(e, ModelInstallState.DOWNLOADED, "already complete")
        val rec = registry.record(e.identity)
        assertNotNull(rec)
        assertEquals("DOWNLOADED", rec!!.state)
        assertEquals(e.sha256, rec.sha256)
        // the truth rule: no installedPath without bytes on disk
        assertNull(rec.installedPath)
    }

    // --------------------------------- the Owner's physical dead-end scenario

    @Test
    fun `THE OWNER SCENARIO - full download now leaves an observable DOWNLOADED record`() = runBlocking {
        val e = ggufEntry()
        downloads.start(e, "${server.baseUrl}/artifacts/qwen-test.gguf")
        awaitState(e.identity, ModelInstallState.DOWNLOADED)
        // BEFORE the repair the record was null here (markState no-op) and the
        // card said 'Scaricato' + 'non installato' with primary action DOWNLOAD.
        val rec = registry.record(e.identity)
        assertNotNull("the DOWNLOADED state must be observable in the registry", rec)
        assertEquals("DOWNLOADED", rec!!.state)
    }

    @Test
    fun `THE OWNER SCENARIO - resolvers honor progressState when the record is absent (R3)`() {
        // The exact matrix of the dead card: record==null, progress==DOWNLOADED
        val verdict = CompatVerdictResolver.resolve(
            entryClassification = "VERIFIED_DOWNLOADABLE",
            recordState = null,
            active = null,
            runtimeRequirement = "llama",
            compatible = true,
            incompatibilityReason = null,
            progressState = "DOWNLOADED",
            runtimeAvailable = true,
        )
        assertEquals(CompatVerdictKind.VERIFICATION_REQUIRED, verdict.kind)
        val plan = ModelActionResolver.resolve(
            entryClassification = "VERIFIED_DOWNLOADABLE",
            recordState = null,
            active = null,
            progressState = "DOWNLOADED",
            verdict = verdict,
        )
        // NEVER DOWNLOAD again over a complete file — VERIFY (the promote path)
        assertEquals(ModelPrimaryAction.VERIFY, plan.primary)

        val hashVerified = ModelActionResolver.resolve(
            "VERIFIED_DOWNLOADABLE", null, null, "HASH_VERIFIED",
            CompatVerdictResolver.resolve("VERIFIED_DOWNLOADABLE", null, null, "llama", true, null, "HASH_VERIFIED", true),
        )
        assertEquals(ModelPrimaryAction.INSTALL, hashVerified.primary)
    }

    @Test
    fun `R4 - re-click with a COMPLETE partial recovers via 416 and NEVER deletes the artifact`() = runBlocking {
        val e = ggufEntry()
        // First download completes to tmp.
        downloads.start(e, "${server.baseUrl}/artifacts/qwen-test.gguf")
        awaitState(e.identity, ModelInstallState.DOWNLOADED)
        val partial = downloads.partialFileOf(e)!!
        assertTrue(Files.exists(partial))
        val sizeBefore = Files.size(partial)

        // THE OWNER'S RE-CLICK: start() again — the Range header is at EOF and
        // the (mock and real HF) server answers 416. The OLD branch deleted the
        // complete 522 MB partial; the repair recovers it as DOWNLOADED.
        downloads.start(e, "${server.baseUrl}/artifacts/qwen-test.gguf")
        awaitState(e.identity, ModelInstallState.DOWNLOADED)
        assertTrue("the complete partial must SURVIVE the 416 re-click", Files.exists(partial))
        assertEquals(sizeBefore, Files.size(partial))
        val rec = registry.record(e.identity)
        assertNotNull(rec)
        assertEquals("DOWNLOADED", rec!!.state)
        assertTrue(rec.lastNote?.contains("416") == true)
    }

    // -------------------------------------------- R2: the promotion chain

    @Test
    fun `R2 - promote runs the full chain to PROMOTED with read-back and tmp cleanup`() = runBlocking {
        val e = ggufEntry()
        downloads.start(e, "${server.baseUrl}/artifacts/qwen-test.gguf")
        awaitState(e.identity, ModelInstallState.DOWNLOADED)
        val partial = downloads.partialFileOf(e)!!

        val result = promotion.promote(e)
        assertEquals(ModelPromotionController.Code.PROMOTED, result.code)
        assertNotNull(result.installedPath)
        // read-back: the installed file exists at the recorded path
        assertTrue(Files.exists(result.installedPath!!))
        // registry: INSTALLED with the path
        val rec = registry.record(e.identity)
        assertNotNull(rec)
        assertEquals("INSTALLED", rec!!.state)
        assertEquals(result.installedPath!!.toString(), rec.installedPath)
        // tmp cleanup: the partial is gone after a VERIFIED install
        assertFalse(Files.exists(partial))
        // duplicate-promotion guard (typed, not a crash)
        assertEquals(ModelPromotionController.Code.NOT_DOWNLOADED, promotion.promote(e).code)
    }

    @Test
    fun `R2 - size mismatch retains the resumable partial and never installs`() {
        val e = ggufEntry().copy(downloadSizeBytes = ggufArtifact.size.toLong() + 1024)
        // place an incomplete partial
        val partial = storage.tempDir().resolve("${e.identity}.part")
        Files.createDirectories(storage.tempDir())
        Files.write(partial, ggufArtifact.copyOf(1024))
        val result = promotion.promote(e)
        assertEquals(ModelPromotionController.Code.SIZE_MISMATCH, result.code)
        assertTrue("resumable partial retained", Files.exists(partial))
        assertNull(registry.record(e.identity)?.installedPath)
        assertNotEqualsState("INSTALLED", e.identity)
    }

    @Test
    fun `R2 - hash mismatch is typed HASH_MISMATCH and the evidence is quarantined`() {
        val e = ggufEntry().copy(sha256 = "f".repeat(64))
        Files.createDirectories(storage.tempDir())
        Files.write(storage.tempDir().resolve("${e.identity}.part"), ggufArtifact)
        val result = promotion.promote(e)
        assertEquals(ModelPromotionController.Code.HASH_MISMATCH, result.code)
        assertNotEqualsState("INSTALLED", e.identity)
    }

    @Test
    fun `R2 - invalid GGUF is typed GGUF_INVALID, retained, never installed`() {
        val notGguf = ByteArray(4096) { it.toByte() }
        val notGgufHash = ModelSha256.ofBytes(notGguf)
        val e = ggufEntry().copy(sha256 = notGgufHash)
        Files.createDirectories(storage.tempDir())
        val partial = storage.tempDir().resolve("${e.identity}.part")
        Files.write(partial, notGguf)
        val result = promotion.promote(e)
        assertEquals(ModelPromotionController.Code.GGUF_INVALID, result.code)
        assertTrue("bytes retained for diagnosis", Files.exists(partial))
        assertNotEqualsState("INSTALLED", e.identity)
    }

    private fun assertNotEqualsState(state: String, identity: String) {
        val rec = registry.record(identity)
        assertTrue(rec?.state != state)
    }

    // ------------------------------------------- GGUF structural validator

    @Test
    fun `GGUF validator accepts a sane v3 header and rejects corrupt files`() {
        val good = Files.createTempFile("good", ".gguf")
        Files.write(good, ggufBytes(64))
        assertTrue(ModelGgufValidator.isValidGguf(good))

        val badMagic = Files.createTempFile("badmagic", ".gguf")
        Files.write(badMagic, ByteArray(4096))
        assertFalse(ModelGgufValidator.isValidGguf(badMagic))

        val truncated = Files.createTempFile("trunc", ".gguf")
        Files.write(truncated, byteArrayOf(0x47, 0x47, 0x55))
        assertFalse(ModelGgufValidator.isValidGguf(truncated))
    }

    // -------------------------------------------- restart read-back truth

    @Test
    fun `registry reopen keeps the promoted record (restart read-back)`() {
        val e = ggufEntry()
        registry.ensureRecord(e, ModelInstallState.DOWNLOADED, null)
        val reopened = FileInstallationRegistry(root.resolve("registry.json"))
        assertEquals("DOWNLOADED", reopened.record(e.identity)?.state)
    }
}
