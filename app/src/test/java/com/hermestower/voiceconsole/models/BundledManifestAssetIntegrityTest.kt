package com.hermestower.voiceconsole.models

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * GRAPH 4 CP-G4-009: shipped-bundle integrity audit.
 *
 * The four ONNX artifacts shipped in `assets/models/bundled/` are read AS REAL
 * BYTES (no fixtures) and verified against BOTH shipped manifests. If an asset
 * is corrupted, replaced or diverges from the catalog, the unit-test gate fails
 * before any APK ships. Same discipline as the CP-G4-003 shipped-manifest
 * audit, extended to the artifact bytes themselves.
 */
class BundledManifestAssetIntegrityTest {

    /** Assets are read straight from the source tree (real bytes on disk). */
    private fun bundledDir(): File =
        listOf(File("src/main/assets/models/bundled"), File("app/src/main/assets/models/bundled"))
            .firstOrNull { it.isDirectory }
            ?: error("bundled assets directory not found from ${System.getProperty("user.dir")}")

    private fun catalogDir(): File =
        listOf(File("src/main/assets/models"), File("app/src/main/assets/models"))
            .firstOrNull { it.isDirectory }
            ?: error("assets/models directory not found")

    private fun readText(file: File): String =
        file.inputStream().bufferedReader().use { it.readText() }

    private fun manifest(): BundledModelInstaller.BundledManifest =
        DefaultVoiceModelCatalog.json.decodeFromString(
            BundledModelInstaller.BundledManifest.serializer(),
            readText(File(bundledDir(), "bundled_manifest.json")),
        )

    private fun catalog(): DefaultVoiceModelCatalog =
        DefaultVoiceModelCatalog.fromText(readText(File(catalogDir(), "voice_pack_catalog.json")))

    @Test
    fun `bundled manifest parses and lists exactly the four verified artifacts`() {
        val m = manifest()
        assertEquals("1.0", m.manifestVersion)
        assertEquals(4, m.artifacts.size)
        assertEquals(
            setOf(
                "org.openwakeword.model.melspectrogram",
                "org.openwakeword.model.embedding",
                "org.openwakeword.wakeword.hey-jarvis",
                "org.gtcrn.speech-enhancement.simple",
            ),
            m.artifacts.map { it.identity }.toSet(),
        )
    }

    @Test
    fun `every bundled asset on disk matches its manifest sha256 and size`() {
        val dir = bundledDir()
        for (artifact in manifest().artifacts) {
            val file = File(dir, artifact.asset)
            assertTrue("missing bundled asset ${artifact.asset}", file.exists())
            assertEquals("size drift on ${artifact.asset}", artifact.sizeBytes, file.length())
            assertEquals(
                "hash drift on ${artifact.asset} — the shipped bytes no longer match the manifest",
                artifact.sha256,
                ModelSha256.ofStream(file.inputStream()),
            )
        }
    }

    @Test
    fun `bundled manifest and voice pack catalog agree on every artifact (double gate)`() {
        val catalog = catalog()
        for (artifact in manifest().artifacts) {
            val entry = catalog.find(artifact.identity)
            assertTrue("bundled identity ${artifact.identity} absent from the catalog", entry != null)
            entry ?: continue
            assertEquals("hash conflict for ${artifact.identity}", entry.sha256, artifact.sha256)
            assertEquals("version conflict for ${artifact.identity}", entry.version, artifact.version)
            assertEquals("size conflict for ${artifact.identity}", entry.downloadSizeBytes, artifact.sizeBytes)
            assertEquals("license conflict for ${artifact.identity}", entry.license, artifact.license)
            assertTrue(
                "${artifact.identity} must be VERIFIED_DOWNLOADABLE, not ${entry.entryClassification}",
                entry.entryClassification == "VERIFIED_DOWNLOADABLE",
            )
        }
    }

    @Test
    fun `licenses permit redistribution with attribution`() {
        for (artifact in manifest().artifacts) {
            assertTrue("unexpected license ${artifact.license} for ${artifact.asset}",
                artifact.license in setOf("Apache-2.0", "MIT"))
            assertTrue("license URL required for attribution", !artifact.licenseUrl.isNullOrBlank())
            assertTrue("upstream repo required for provenance", !artifact.upstreamRepo.isNullOrBlank())
        }
    }
}
