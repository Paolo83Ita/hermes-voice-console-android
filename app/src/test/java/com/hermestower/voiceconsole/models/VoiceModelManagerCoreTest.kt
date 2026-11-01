package com.hermestower.voiceconsole.models

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * JVM tests for the Voice Model Manager core: catalog, manifest verifier,
 * voice-pack resolver, deterministic test artifact (GRAPH 2 VM-01..VM-11).
 */
class VoiceModelManagerCoreTest {

    private fun resources(
        androidVersion: Int = 34,
        abis: List<String> = listOf("arm64-v8a", "x86_64"),
        availRam: Long = 6L * 1024 * 1024 * 1024,
        freeStorage: Long = 40L * 1024 * 1024 * 1024,
        online: Boolean = true,
        metered: Boolean = false,
        thermal: Int? = 0,
    ) = DeviceResources(
        deviceModel = "Xiaomi 11 Lite 5G NE",
        androidVersion = androidVersion,
        supportedAbis = abis,
        totalRamBytes = 8L * 1024 * 1024 * 1024,
        availableRamBytes = availRam,
        freeStorageBytes = freeStorage,
        ttsEnginePackages = listOf("com.google.android.tts"),
        networkOnline = online,
        networkMetered = metered,
        thermalStatus = thermal,
        powerSaving = false,
    )

    private fun catalog(): VoiceModelCatalog = DefaultVoiceModelCatalog.fromText(CATALOG_JSON)

    @Test
    fun `catalog loads embedded manifest and resolves lookups`() {
        val catalog = catalog()
        assertEquals("1.0.0", catalog.manifestVersion())
        assertTrue(catalog.all().size >= 3)
        val probe = catalog.find("org.hermestower.test.probe-artifact")
        assertNotNull(probe)
        assertEquals("WAKE_WORD", probe!!.componentType)
        assertEquals(1, catalog.ofComponent(ModelComponentType.LOCAL_LLM).size)
        assertNull(catalog.find("does-not-exist"))
    }

    @Test
    fun `manifest verifier accepts valid entry and rejects broken ones`() {
        val verifier = DefaultModelManifestVerifier()
        val probe = catalog().find("org.hermestower.test.probe-artifact")!!
        assertTrue(verifier.verify(probe).valid)
        val broken = probe.copy(identity = "", sha256 = "nothex", downloadSizeBytes = 0)
        val result = verifier.verify(broken)
        assertFalse(result.valid)
        assertTrue(result.problems.any { it.contains("identity") })
        assertTrue(result.problems.any { it.contains("sha256") })
        assertTrue(result.problems.any { it.contains("downloadSizeBytes") })
    }

    @Test
    fun `probe artifact entry is downloadable while unqualified real models are not`() {
        val resolver = DefaultVoicePackResolver()
        val catalog = catalog()
        val recommendation = resolver.recommend(VoiceModelProfile.HYBRID_BALANCED, resources(), catalog)
        val probe = recommendation.components.first { it.identity == "org.hermestower.test.probe-artifact" }
        assertTrue(probe.downloadAllowed)
        val llm = recommendation.components.first { it.componentType == ModelComponentType.LOCAL_LLM.name }
        assertFalse(llm.downloadAllowed)
        assertEquals("License review pending (GEMMA_TERMS_REVIEW_REQUIRED).", llm.downloadBlockReason)
        val stt = recommendation.components.first { it.componentType == ModelComponentType.SPEECH_TO_TEXT.name }
        assertFalse(stt.downloadAllowed)
    }

    @Test
    fun `TOWER_FIRST never routes the local LLM locally`() {
        val resolver = DefaultVoicePackResolver()
        val recommendation = resolver.recommend(VoiceModelProfile.TOWER_FIRST, resources(), catalog())
        val llm = recommendation.components.first { it.componentType == ModelComponentType.LOCAL_LLM.name }
        assertEquals("TOWER", llm.route)
        assertTrue(recommendation.routeSummary.contains("Tower"))
    }

    @Test
    fun `HYBRID_BALANCED routes simple local and complex to Tower`() {
        val resolver = DefaultVoicePackResolver()
        val recommendation = resolver.recommend(VoiceModelProfile.HYBRID_BALANCED, resources(), catalog())
        assertTrue(recommendation.routeSummary.contains("Simple requests local"))
    }

    @Test
    fun `incompatible ABI and Android version are detected`() {
        val resolver = DefaultVoicePackResolver()
        val probe = catalog().find("org.hermestower.test.probe-artifact")!!
        val oldAndroid = resolver.compatibility(probe, resources(androidVersion = 29))
        assertFalse(oldAndroid.compatible)
        assertTrue(oldAndroid.reason!!.contains("Android"))
        val wrongAbi = probe.copy(supportedAbis = listOf("mips64"))
        val abi = resolver.compatibility(wrongAbi, resources())
        assertFalse(abi.compatible)
        assertTrue(abi.reason!!.contains("ABI"))
    }

    @Test
    fun `LOCAL_EXPERIMENTAL gates on RAM storage and thermal`() {
        val gates = VoicePackPolicyResolver.gatesFor(
            VoiceModelProfile.LOCAL_EXPERIMENTAL,
            resources(availRam = 2L * 1024 * 1024 * 1024, freeStorage = 3L * 1024 * 1024 * 1024, thermal = 5),
        )
        assertTrue(gates.any { it.contains("RAM gate") })
        assertTrue(gates.any { it.contains("Storage gate") })
        assertTrue(gates.any { it.contains("Thermal gate") })
        val noGates = VoicePackPolicyResolver.gatesFor(
            VoiceModelProfile.LOCAL_EXPERIMENTAL,
            resources(availRam = 4L * 1024 * 1024 * 1024, freeStorage = 20L * 1024 * 1024 * 1024, thermal = 0),
        )
        assertTrue(noGates.isEmpty())
    }

    @Test
    fun `offline network produces a network gate warning`() {
        val gates = VoicePackPolicyResolver.gatesFor(VoiceModelProfile.TOWER_FIRST, resources(online = false))
        assertTrue(gates.any { it.contains("Network gate") })
    }

    @Test
    fun `recommendation totals are the sum of downloadable components and confirmation is required`() {
        val resolver = DefaultVoicePackResolver()
        val recommendation = resolver.recommend(VoiceModelProfile.HYBRID_BALANCED, resources(), catalog())
        val downloadable = recommendation.components.filter { it.downloadAllowed }
        assertEquals(downloadable.sumOf { it.downloadSizeBytes }, recommendation.expectedDownloadBytes)
        assertEquals(downloadable.sumOf { it.installedSizeBytes }, recommendation.expectedInstalledBytes)
        assertTrue(recommendation.requiresExplicitConfirmation)
    }

    @Test
    fun `deterministic test artifact is stable and marked`() {
        val a = DeterministicTestArtifact.generate("hermes-test-artifact-v1", 4096)
        val b = DeterministicTestArtifact.generate("hermes-test-artifact-v1", 4096)
        assertTrue(a.contentEquals(b))
        assertEquals(4096, a.size)
        assertEquals('H'.code.toByte(), a[0])
        assertEquals('V'.code.toByte(), a[1])
        assertEquals('C'.code.toByte(), a[2])
        assertEquals('2'.code.toByte(), a[3])
        assertEquals("2d717056fd1399aae4a63c4fdf20bc6197dbb7d67290b2f410ec3f205df6f527", ModelSha256.ofBytes(a))
        val other = DeterministicTestArtifact.generate("other-seed", 4096)
        assertFalse(a.contentEquals(other))
    }

    private companion object {
        val CATALOG_JSON = """
        {
          "manifestVersion": "1.0.0",
          "generatedFor": "hermes-voice-console-android-0.2.0-debug",
          "entries": [
            {
              "componentType": "WAKE_WORD",
              "identity": "org.hermestower.test.probe-artifact",
              "displayName": "Hermes Test Probe Artifact",
              "version": "1.0.0",
              "sourceUri": "mock://artifacts/hermes-test-artifact-v1",
              "provider": "Hermes Tower (local test generator)",
              "license": "Apache-2.0",
              "licenseApproved": true,
              "format": "hermes-probe",
              "quantization": "none",
              "supportedAbis": ["arm64-v8a", "x86_64"],
              "androidMinVersion": 30,
              "downloadSizeBytes": 4096,
              "installedSizeBytes": 4096,
              "sha256": "2d717056fd1399aae4a63c4fdf20bc6197dbb7d67290b2f410ec3f205df6f527",
              "manifestVersion": "1.0.0",
              "storageLocation": "filesDir/models/probe/",
              "ramEstimateBytes": 1048576,
              "latencyClass": "NEGLIGIBLE",
              "languages": ["und"],
              "italianSupported": false,
              "knownLimitations": ["probe fixture"]
            },
            {
              "componentType": "SPEECH_TO_TEXT",
              "identity": "org.whisper.cpp.tiny.it",
              "displayName": "whisper.cpp tiny (it, int8)",
              "version": "1.7.4",
              "sourceUri": "https://example.invalid/whisper/ggml-tiny-int8.bin",
              "provider": "OpenAI whisper.cpp community",
              "license": "MIT",
              "licenseApproved": true,
              "format": "ggml",
              "quantization": "int8",
              "supportedAbis": ["arm64-v8a", "x86_64"],
              "androidMinVersion": 30,
              "downloadSizeBytes": 83886080,
              "installedSizeBytes": 88080384,
              "sha256": "0000000000000000000000000000000000000000000000000000000000000000",
              "manifestVersion": "1.0.0",
              "storageLocation": "filesDir/models/stt/",
              "ramEstimateBytes": 188743680,
              "latencyClass": "SLOW",
              "languages": ["it-IT"],
              "italianSupported": true
            },
            {
              "componentType": "LOCAL_LLM",
              "identity": "org.hermestower.llm.gemma-2b-it-q4",
              "displayName": "Gemma 2B IT (Q4_0)",
              "version": "2.0.0",
              "sourceUri": "https://example.invalid/llm/gemma-2b-it-q4_0.gguf",
              "provider": "Google (Gemma Terms of Use)",
              "license": "GEMMA_TERMS_REVIEW_REQUIRED",
              "licenseApproved": false,
              "format": "gguf",
              "quantization": "q4_0",
              "supportedAbis": ["arm64-v8a"],
              "androidMinVersion": 30,
              "downloadSizeBytes": 1610612736,
              "installedSizeBytes": 1717986918,
              "sha256": "0000000000000000000000000000000000000000000000000000000000000000",
              "manifestVersion": "1.0.0",
              "storageLocation": "filesDir/models/llm/",
              "ramEstimateBytes": 2147483648,
              "latencyClass": "SLOW",
              "languages": ["it-IT"],
              "italianSupported": true
            }
          ]
        }
        """.trimIndent()
    }
}
