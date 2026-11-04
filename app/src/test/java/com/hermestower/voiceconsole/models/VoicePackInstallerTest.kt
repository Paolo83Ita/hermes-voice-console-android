package com.hermestower.voiceconsole.models

import com.hermestower.voiceconsole.towersim.LocalTowerMockServer
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path

/**
 * GRAPH 4 NODE 2/3 (CP-G4-004): MODEL SANCTUM engine tests — pack planning honesty,
 * preflight summary math, gatekeeper classification, the receipted §7 pipeline,
 * quarantine on hash mismatch, previous-version preservation and receipt secrets.
 */
class VoicePackInstallerTest {

    private lateinit var server: LocalTowerMockServer
    private lateinit var root: Path
    private lateinit var storage: DefaultModelStorageManager
    private lateinit var registry: ModelInstallationRegistry
    private lateinit var downloads: DefaultModelDownloadManager
    private lateinit var qualification: DefaultModelQualificationRunner
    private lateinit var activation: DefaultModelActivationController
    private lateinit var installer: DefaultVoicePackInstaller

    private fun resources(
        online: Boolean = true,
        metered: Boolean = false,
        thermal: Int? = 1,
        powerSaving: Boolean? = false,
        freeStorage: Long = 100L * 1024 * 1024 * 1024,
    ) = DeviceResources(
        deviceModel = "Test Device",
        androidVersion = 35,
        supportedAbis = listOf("arm64-v8a"),
        totalRamBytes = 8L * 1024 * 1024 * 1024,
        availableRamBytes = 6L * 1024 * 1024 * 1024,
        freeStorageBytes = freeStorage,
        ttsEnginePackages = listOf("com.google.android.tts"),
        networkOnline = online,
        networkMetered = metered,
        thermalStatus = thermal,
        powerSaving = powerSaving,
    )

    private var seq = 0
    private fun entry(
        bytes: ByteArray,
        type: String = "VAD",
        identity: String = "test.comp.$seq",
        classification: String = "VERIFIED_DOWNLOADABLE",
        recommended: Boolean = true,
        optional: Boolean = false,
        version: String = "1.0.0",
    ): ModelCatalogEntry {
        seq++
        return ModelCatalogEntry(
            componentType = type,
            identity = identity,
            displayName = "Test Component $seq",
            version = version,
            sourceUri = "${server.baseUrl}/artifacts/comp-$seq",
            provider = "Test Provider",
            license = "MIT",
            licenseApproved = true,
            format = "onnx",
            quantization = "none",
            supportedAbis = listOf("arm64-v8a"),
            androidMinVersion = 30,
            downloadSizeBytes = bytes.size.toLong(),
            installedSizeBytes = bytes.size.toLong(),
            sha256 = ModelSha256.ofBytes(bytes),
            manifestVersion = "1.0.0",
            // Identity-based root so two versions of one component share a directory
            // (previous-version preservation is exercised against the same path).
            storageLocation = "filesDir/models/test/${identity}/",
            ramEstimateBytes = 1024 * 1024,
            latencyClass = "LOW",
            languages = listOf("it"),
            italianSupported = true,
            entryClassification = classification,
            recommended = recommended,
            optionalComponent = optional,
        )
    }

    @Before
    fun setUp() {
        server = LocalTowerMockServer()
        root = Files.createTempDirectory("sanctum-test")
        storage = DefaultModelStorageManager(root)
        registry = FileInstallationRegistry(root.resolve("registry.json"))
        downloads = DefaultModelDownloadManager(storage, registry)
        qualification = DefaultModelQualificationRunner(storage)
        activation = DefaultModelActivationController(registry, qualification)
        installer = DefaultVoicePackInstaller(
            downloads = downloads,
            qualification = qualification,
            activation = activation,
            registry = registry,
            storage = storage,
            awaitTimeoutMs = 15_000,
        )
    }

    @After
    fun tearDown() {
        root.toFile().deleteRecursively()
    }

    // ------------------------------------------------------------- planning

    @Test
    fun `plan uses the shipped catalog — includes recommended, excludes optional and fixtures, states the wake-word gap`() {
        val text = javaClass.classLoader!!
            .getResourceAsStream("models/voice_pack_catalog.json")!!
            .bufferedReader().use { it.readText() }
        val catalog = DefaultVoiceModelCatalog.fromText(text)
        val plan = installer.plan(VoiceModelProfile.HYBRID_BALANCED, resources(), catalog)

        val included = plan.includedItems.mapNotNull { it.entry }.map { it.identity }
        assertTrue("VAD must be in the pack", included.contains("org.silero.vad.v5"))
        assertTrue("Italian STT must be in the pack", included.contains("org.vosk.model.small-it-0.22"))
        assertTrue("The default Italian voice must be in the pack", included.contains("org.piper.voice.it-riccardo-x-low"))
        // Never in the pack:
        assertFalse("probe fixture must never be packed", included.any { it == "org.hermestower.test.probe-artifact" })
        assertFalse("local LLM must never be packed", included.any { it.startsWith("org.qwen.") })
        assertFalse("alternative voice stays optional", included.contains("org.piper.voice.it-paola-medium"))
        assertFalse("denoiser stays optional", included.contains("org.gtcrn.speech-enhancement.simple"))
        // GRAPH 4 FINAL §13 HONESTY CORRECTION (2026-11-04): the sherpa-onnx TTS
        // runtime was previously mislabeled RUNTIME_BUNDLED ("pinned in gradle")
        // while NO such gradle dependency exists — the catalog now types it
        // CANDIDATE_PENDING_DISCOVERY and the plan must NOT present it as a
        // bundled component. The ACTIVE TTS path is the Android system engine.
        val bundled = plan.items.filter { it.role == "BUNDLED_RUNTIME" }
        assertTrue(
            "the sherpa-onnx runtime must NOT be presented as bundled (no gradle dep exists)",
            bundled.none { it.entry?.identity == "org.sherpa-onnx.tts.runtime" },
        )
        // The pending runtime still appears as an honest gap, never silently dropped:
        val pendingRuntime = plan.items.filter {
            it.entry?.identity == "org.sherpa-onnx.tts.runtime" &&
                it.role == "BLOCKED_NO_VERIFIED_SOURCE"
        }
        assertTrue(
            "the pending TTS runtime must appear as an honest gap (not bundled, not packed)",
            pendingRuntime.isNotEmpty(),
        )
        // Honest wake-word gap: required type with no verified production source.
        val wakeGap = plan.items.filter { it.role == "BLOCKED_NO_VERIFIED_SOURCE" }
        assertTrue("the custom Italian wake word must appear as an honest gap", wakeGap.isNotEmpty())
    }

    @Test
    fun `summary math — totals, conservative peak, reserve floor and labeled duration`() {
        val a = entry(ByteArray(1000), type = "VAD")
        val b = entry(ByteArray(500), type = "SPEECH_TO_TEXT")
        server.putArtifact("comp-1", LocalTowerMockServer.Artifact(ByteArray(1000)))
        server.putArtifact("comp-2", LocalTowerMockServer.Artifact(ByteArray(500)))
        val plan = PackPlan("HYBRID_BALANCED", listOf(
            PackItem(a, "VAD", "DOWNLOADABLE", true, "ok"),
            PackItem(b, "SPEECH_TO_TEXT", "DOWNLOADABLE", true, "ok"),
        ))
        val summary = installer.summarize(plan, resources(freeStorage = 10L * 1024 * 1024 * 1024))
        assertEquals(1500L, summary.totalDownloadBytes)
        assertEquals(1500L, summary.totalInstalledBytes)
        assertEquals(3000L, summary.peakTempBytes)
        // Reserve: greater of 2 GB floor and 15% of 10 GB (=1.5 GB) → 2 GB.
        assertEquals(DefaultVoicePackInstaller.PACK_RESERVE_FLOOR_BYTES, summary.reserveBytes)
        assertEquals(summary.peakTempBytes + summary.reserveBytes, summary.minimumFreeRequiredBytes)
        assertTrue("duration must be labeled as an unmeasured estimate",
            summary.durationRangeText.contains("TARGET_PENDING_DEVICE_QUALIFICATION"))
        assertTrue(summary.licenses.contains("MIT"))
    }

    // ------------------------------------------------------------- gatekeeper

    @Test
    fun `gatekeeper blocks offline and severe thermal, warns on moderate and metered`() {
        val blocked = DownloadGatekeeper.check(resources(online = false), 1000L)
        assertFalse(blocked.allowed)
        assertTrue(blocked.blockReason!!.contains("offline"))

        val severe = DownloadGatekeeper.check(resources(thermal = 4), 1000L)
        assertFalse(severe.allowed)
        assertTrue(severe.blockReason!!.contains("Thermal gate"))

        val moderate = DownloadGatekeeper.check(resources(thermal = 2), 1000L)
        assertTrue(moderate.allowed)
        assertTrue(moderate.warnings.any { it.contains("Thermal status") })

        val metered = DownloadGatekeeper.check(resources(metered = true), 1000L)
        assertTrue(metered.allowed)
        assertTrue(metered.warnings.any { it.contains("Metered") })
    }

    // ------------------------------------------------------------- pipeline

    @Test
    fun `pack happy path — sequential receipted install to ACTIVE with registry read-back`() = runBlocking {
        val a = entry(ByteArray(2048) { (it % 251).toByte() }, type = "VAD")
        val b = entry(ByteArray(1024) { (it % 241).toByte() }, type = "SPEECH_TO_TEXT")
        server.putArtifact("comp-1", LocalTowerMockServer.Artifact(ByteArray(2048) { (it % 251).toByte() }))
        server.putArtifact("comp-2", LocalTowerMockServer.Artifact(ByteArray(1024) { (it % 241).toByte() }))
        val plan = PackPlan("HYBRID_BALANCED", listOf(
            PackItem(a, "VAD", "DOWNLOADABLE", true, "ok"),
            PackItem(b, "SPEECH_TO_TEXT", "DOWNLOADABLE", true, "ok"),
        ))
        val result = withTimeout(30_000) {
            installer.installPack(plan, resources())
        }
        assertTrue("pack should succeed: ${result.outcomes}", result.success)
        assertEquals(2, result.outcomes.size)
        assertTrue(result.outcomes.all { it.success })
        // Registry read-back: both ACTIVE.
        result.outcomes.forEach { outcome ->
            val rec = registry.record(outcome.identity)
            assertNotNull(rec)
            assertEquals(ModelInstallState.ACTIVE.name, rec!!.state)
            assertTrue(rec.active)
        }
        // Receipts: 2 component receipts + 1 pack receipt.
        val receiptFiles = ReceiptWriter.listReceipts(storage.receiptsDir())
        assertEquals(3, receiptFiles.size)
        assertTrue(receiptFiles.any { it.startsWith("pack-") })
        assertTrue(receiptFiles.count { it.startsWith("receipt-") } == 2)
        assertTrue(result.outcomes.all { it.receiptId != null })
    }

    @Test
    fun `pack stops truthfully on hash mismatch — earlier success kept, partial quarantined`() = runBlocking {
        val good = ByteArray(2048) { (it % 251).toByte() }
        val a = entry(good, type = "VAD")
        val corruptEntry = entry(ByteArray(1024), type = "SPEECH_TO_TEXT")
        server.putArtifact("comp-1", LocalTowerMockServer.Artifact(good))
        // Serve WRONG bytes for comp-2: the hash will not match.
        server.putArtifact("comp-2", LocalTowerMockServer.Artifact(ByteArray(1024) { 7 }))
        val plan = PackPlan("HYBRID_BALANCED", listOf(
            PackItem(a, "VAD", "DOWNLOADABLE", true, "ok"),
            PackItem(corruptEntry, "SPEECH_TO_TEXT", "DOWNLOADABLE", true, "ok"),
        ))
        val result = withTimeout(30_000) {
            installer.installPack(plan, resources())
        }
        assertFalse("pack must not report success", result.success)
        assertEquals("first component succeeded", true, result.outcomes.first().success)
        assertEquals("second component failed on hash", false, result.outcomes.last().success)
        assertTrue(result.outcomes.last().reason.contains("SHA-256"))
        // First stays ACTIVE (truthful partial state, not rolled back silently).
        assertEquals(ModelInstallState.ACTIVE.name, registry.record(a.identity)!!.state)
        // Quarantine root holds the evidence.
        val quarantined = storage.quarantineDir().toFile().listFiles().orEmpty()
        assertTrue("partial must be quarantined", quarantined.any { it.name.endsWith(".part") })
        assertTrue("facts sidecar must exist", quarantined.any { it.name.endsWith(".facts.txt") })
        // Pack receipt says PARTIAL_OR_FAILED.
        val packReceipt = ReceiptWriter.listReceipts(storage.receiptsDir()).first { it.startsWith("pack-") }
        val text = String(Files.readAllBytes(storage.receiptsDir().resolve(packReceipt)))
        assertTrue(text.contains("PARTIAL_OR_FAILED"))
    }

    @Test
    fun `component receipt contains no secrets and records the pipeline steps`() = runBlocking {
        val bytes = ByteArray(4096) { (it % 251).toByte() }
        val e = entry(bytes, type = "VAD")
        server.putArtifact("comp-1", LocalTowerMockServer.Artifact(bytes))
        val outcome = withTimeout(30_000) { installer.installComponent(e, resources()) }
        assertTrue(outcome.success)
        val receiptFile = storage.receiptsDir().resolve(outcome.receiptId!!)
        val text = String(Files.readAllBytes(receiptFile))
        // Secret policy (INV-G4-03): no credential-looking material.
        assertFalse(text.contains("Bearer "))
        assertFalse(text.contains("Authorization"))
        assertFalse(text.contains("token"))
        // Structured pipeline facts present.
        assertTrue(text.contains(e.identity))
        assertTrue(text.contains(e.sha256))
        ReceiptSteps.ALL.forEach { assertTrue("step ${it} missing", text.contains(it)) }
        assertTrue(text.contains(ModelInstallationReceipt.OUTCOME_INSTALLED))
    }

    @Test
    fun `update install preserves the previous version into the previous root`() {
        val oldBytes = ByteArray(512) { 1 }
        val newBytes = ByteArray(512) { 2 }
        val e1 = entry(oldBytes, type = "VAD", version = "1.0.0")
        val e2 = entry(newBytes, type = "VAD", identity = e1.identity, version = "1.1.0")
        // Install v1 manually through the atomic path.
        val partial = storage.tempDir().resolve("${e1.identity}.part")
        Files.write(partial, oldBytes)
        storage.installAtomically(e1, partial)
        assertNotNull(storage.installedFile(e1))
        // Install v2 over it: the v1 bytes must land in previous/.
        val partial2 = storage.tempDir().resolve("${e2.identity}.part")
        Files.write(partial2, newBytes)
        storage.installAtomically(e2, partial2)
        assertNotNull(storage.installedFile(e2))
        val previous = storage.previousDir().toFile().listFiles().orEmpty()
        assertTrue("previous version must be preserved", previous.any { it.name.contains("1.0.0") })
        assertEquals(oldBytes.size.toLong(), previous.first { it.name.contains("1.0.0") }.length())
    }

    @Test
    fun `quarantine writes an evidence sidecar with expected and actual hashes`() {
        val partial = storage.tempDir().resolve("x.part")
        Files.write(partial, ByteArray(128) { 3 })
        val path = storage.quarantinePartial("test.identity", partial, "expected", "actual")
        assertNotNull(path)
        assertFalse(Files.exists(partial))
        val facts = String(Files.readAllBytes(path!!.resolveSibling(path.fileName.toString().replace(".part", ".facts.txt"))))
        assertTrue(facts.contains("expectedSha256=expected"))
        assertTrue(facts.contains("actualSha256=actual"))
    }

    @Test
    fun `plan classifies an installed current version as already installed`() {
        val bytes = ByteArray(2048) { 5 }
        val e = entry(bytes, type = "VAD")
        // Simulate an installed+active v1.0.0.
        val partial = storage.tempDir().resolve("${e.identity}.part")
        Files.write(partial, bytes)
        val installed = storage.installAtomically(e, partial)
        registry.markInstalled(e, installed)
        registry.markQualified(e.identity, java.time.Instant.now())
        registry.markActive(e.identity, true)
        val plan = PackPlan("HYBRID_BALANCED", listOf(installer.classify(e, resources())))
        assertTrue(plan.includedItems.isEmpty())
        assertEquals("ALREADY_INSTALLED", plan.items.single().role)
    }

    @Test
    fun `fallback contract is deterministic per component type`() {
        assertEquals("Typed text input.", ComponentFallback.forType("SPEECH_TO_TEXT"))
        assertEquals("Android system offline TTS; text-only response as last resort.", ComponentFallback.forType("TTS_ENGINE"))
        assertEquals("Hermes Tower (complex reasoning is never local).", ComponentFallback.forType("LOCAL_LLM"))
        assertTrue(ComponentFallback.forType("UNKNOWN_TYPE").contains("Hermes Tower"))
    }
}
