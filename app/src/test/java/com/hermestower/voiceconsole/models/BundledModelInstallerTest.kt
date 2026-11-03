package com.hermestower.voiceconsole.models

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.ByteArrayInputStream
import java.io.File
import java.io.InputStream

/**
 * GRAPH 4 CP-G4-009: behavior of the bundled-delivery pipeline over the REAL
 * shipped assets (no model fixtures). Every hard rule is exercised:
 * hash double-gate, atomic install, registry read-back, qualification+activation,
 * receipts with honest provenance, idempotence, typed refusals, truthful stop.
 */
class BundledModelInstallerTest {

    private val temp = TemporaryFolder()
    @get:org.junit.Rule
    val rule: TemporaryFolder get() = temp

    private fun bundledDir(): File =
        listOf(File("src/main/assets/models/bundled"), File("app/src/main/assets/models/bundled"))
            .first { it.isDirectory }

    private fun catalog(): DefaultVoiceModelCatalog =
        DefaultVoiceModelCatalog.fromText(
            File(listOf(File("src/main/assets/models"), File("app/main/assets/models"),
                File("app/src/main/assets/models")).first { it.isDirectory },
                "voice_pack_catalog.json").inputStream().bufferedReader().use { it.readText() },
        )

    /** Directory-backed asset source over the real shipped bundle. */
    private class DirectoryAssetSource(private val dir: File) :
        BundledModelInstaller.BundledAssetSource {
        override fun open(assetName: String): InputStream = File(dir, assetName).inputStream()
        override fun exists(assetName: String): Boolean = File(dir, assetName).exists()
    }

    /** Corrupts exactly one named asset (first byte flipped) — same real source otherwise. */
    private class CorruptingAssetSource(
        private val delegate: BundledModelInstaller.BundledAssetSource,
        private val victim: String,
    ) : BundledModelInstaller.BundledAssetSource {
        override fun open(assetName: String): InputStream {
            if (assetName != victim) return delegate.open(assetName)
            val bytes = delegate.open(assetName).readBytes()
            bytes[0] = (bytes[0].toInt() xor 0x41).toByte()
            return ByteArrayInputStream(bytes)
        }
        override fun exists(assetName: String): Boolean = delegate.exists(assetName)
    }

    private var registry: ModelInstallationRegistry? = null

    private fun installer(assetSource: BundledModelInstaller.BundledAssetSource, storage: ModelStorageManager): BundledModelInstaller {
        val reg = FileInstallationRegistry(temp.newFile("registry.json").toPath())
        registry = reg
        return BundledModelInstaller(
            assetSource = assetSource,
            catalog = catalog(),
            storage = storage,
            registry = reg,
            activation = DefaultModelActivationController(reg, DefaultModelQualificationRunner(storage)),
        )
    }

    private fun newStorage(): ModelStorageManager =
        DefaultModelStorageManager(temp.newFolder("models-${System.nanoTime()}").toPath())

    private val bundleIdentities = listOf(
        "org.openwakeword.model.melspectrogram",
        "org.openwakeword.model.embedding",
        "org.openwakeword.wakeword.hey-jarvis",
        "org.gtcrn.speech-enhancement.simple",
    )

    @Test
    fun `full delivery installs all four artifacts active with receipts`() {
        val storage = newStorage()
        val installer = installer(DirectoryAssetSource(bundledDir()), storage)
        val report = installer.deliverAll()

        assertTrue("delivery stopped at ${report.stoppedAt}", report.allDelivered)
        assertEquals(4, report.installedCount)
        assertEquals(4, report.results.size)
        report.results.forEach { assertTrue("unexpected result: $it", it is BundledModelInstaller.BundledInstallResult.Installed) }

        // Registry read-back: every identity ACTIVE with the catalog sha256.
        for (identity in bundleIdentities) {
            val record = registry!!.record(identity)
            assertNotNull(record)
            record ?: continue
            assertEquals("$identity not ACTIVE", ModelInstallState.ACTIVE.name, record.state)
            assertTrue(record.active)
            assertEquals("$identity sha mismatch in registry", catalog().find(identity)!!.sha256, record.sha256)
        }

        // Installed bytes exist at the exact expected sizes (catalog == physical).
        for (artifact in installer.loadManifest().artifacts) {
            val catalogEntry = catalog().find(artifact.identity)!!
            val file = storage.installedFile(catalogEntry)
            assertNotNull("no installed file for ${artifact.identity}", file)
            file ?: continue
            assertEquals("size drift after install for ${artifact.asset}", artifact.sizeBytes, file.toFile().length())
            assertEquals("hash drift after install for ${artifact.asset}", artifact.sha256, ModelSha256.ofFile(file))
        }

        // Receipts written with the honest app-bundle provenance.
        val receipts = ReceiptWriter.listReceipts(storage.receiptsDir())
        assertEquals(4, receipts.size)
        val receiptText = File(storage.receiptsDir().toFile(), receipts.last()).readText()
        assertTrue("receipt provenance must be app-bundle", receiptText.contains("app-bundle"))
        assertFalse("bundled receipts must not claim a network host", receiptText.contains("https://"))
    }

    @Test
    fun `second delivery is idempotent - already installed with re-verified bytes`() {
        val storage = newStorage()
        val installer = installer(DirectoryAssetSource(bundledDir()), storage)
        assertTrue(installer.deliverAll().allDelivered)
        val report = installer.deliverAll()
        assertTrue(report.allDelivered)
        assertEquals(4, report.results.count { it is BundledModelInstaller.BundledInstallResult.AlreadyInstalled })
    }

    @Test
    fun `corrupted asset is refused with quarantine evidence and nothing installed`() {
        val storage = newStorage()
        val installer = installer(CorruptingAssetSource(DirectoryAssetSource(bundledDir()), "embed.onnx"), storage)
        val report = installer.deliverAll()

        // Truthful stop at the first failure (embed is the 2nd artifact).
        assertEquals("embed.onnx", report.stoppedAt)
        assertEquals(2, report.results.size)
        assertTrue(report.results[0] is BundledModelInstaller.BundledInstallResult.Installed)
        val refusal = report.results[1] as BundledModelInstaller.BundledInstallResult.Refused
        assertEquals(BundledModelInstaller.RefusalKind.HASH_MISMATCH, refusal.kind)

        // Quarantine facts sidecar written; the corrupted bytes are NOT installed.
        val quarantine = storage.quarantineDir().toFile().listFiles().orEmpty()
        assertTrue("quarantine facts sidecar missing", quarantine.any { it.name.startsWith("bundled-embed.onnx") && it.name.endsWith(".facts.txt") })
        assertEquals("corrupted bytes must never be installed", null, storage.installedFile(catalog().find("org.openwakeword.model.embedding")!!))
        // The registry must not carry a record for the refused identity.
        assertEquals("registry must stay empty for the refused identity", null, registry!!.record("org.openwakeword.model.embedding"))
    }

    @Test
    fun `manifest-catalog conflict is refused before any byte is written`() {
        val storage = newStorage()
        val installer = installer(DirectoryAssetSource(bundledDir()), storage)
        val artifact = installer.loadManifest().artifacts.first().copy(
            sha256 = "1111111111111111111111111111111111111111111111111111111111111111",
        )
        val result = installer.deliver(artifact)
        val refusal = result as BundledModelInstaller.BundledInstallResult.Refused
        assertEquals(BundledModelInstaller.RefusalKind.CATALOG_CONFLICT, refusal.kind)
        assertEquals(0, storage.rootsUsage()["tmp"] ?: 0)
        assertEquals(0, ReceiptWriter.listReceipts(storage.receiptsDir()).size)
    }

    @Test
    fun `missing asset is a typed refusal - no silent placeholder`() {
        val storage = newStorage()
        // The manifest is still served (delivery starts), but every artifact is absent.
        val installer = installer(object : BundledModelInstaller.BundledAssetSource {
            override fun open(assetName: String): InputStream =
                if (assetName == "bundled_manifest.json") File(bundledDir(), assetName).inputStream()
                else throw java.io.FileNotFoundException("no asset $assetName")
            override fun exists(assetName: String): Boolean = assetName == "bundled_manifest.json"
        }, storage)
        val result = installer.deliver(installer.loadManifest().artifacts.first())
        val refusal = result as BundledModelInstaller.BundledInstallResult.Refused
        assertEquals(BundledModelInstaller.RefusalKind.ASSET_MISSING, refusal.kind)
        assertTrue("refusal must name the missing asset", refusal.detail.contains("missing from the APK"))
        assertTrue(refusal.detail.contains("no silent placeholder"))
    }
}
