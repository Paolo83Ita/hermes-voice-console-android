package com.hermestower.voiceconsole.models

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * GRAPH 4 NODE 1 (CP-G4-003) — required automated tests for the verified catalog:
 * placeholder URL blocked, zero hash blocked, mutable unbound source blocked,
 * unapproved license blocked, ABI mismatch, Android mismatch — plus the guarantee
 * that the SHIPPED production catalog itself audits clean.
 */
class CatalogIntegrityTest {

    private fun entry(
        identity: String = "org.test.entry",
        classification: String = "VERIFIED_DOWNLOADABLE",
        sourceUri: String = "https://example.org/model.onnx",
        sha256: String = "a".repeat(64),
        licenseApproved: Boolean = true,
        abis: List<String> = listOf("arm64-v8a"),
        minApi: Int = 30,
        revision: String? = "v1.0.0",
        recommended: Boolean = false,
        optional: Boolean = false,
    ) = ModelCatalogEntry(
        componentType = "VAD",
        identity = identity,
        displayName = identity,
        version = "1.0.0",
        sourceUri = sourceUri,
        provider = "test",
        license = "Apache-2.0",
        licenseApproved = licenseApproved,
        format = "onnx",
        quantization = "fp32",
        supportedAbis = abis,
        androidMinVersion = minApi,
        downloadSizeBytes = 1,
        installedSizeBytes = 1,
        sha256 = sha256,
        manifestVersion = "1.0.0",
        storageLocation = "filesDir/models/test/",
        ramEstimateBytes = 1,
        latencyClass = "LOW",
        languages = listOf("und"),
        italianSupported = false,
        entryClassification = classification,
        revisionBinding = revision,
        recommended = recommended,
        optionalComponent = optional,
    )

    private fun codes(findings: List<CatalogIntegrity.Finding>) =
        findings.map { it.code }

    @Test
    fun `placeholder URL is blocked for verified entries`() {
        val findings = CatalogIntegrity.audit(
            listOf(
                entry(sourceUri = "https://example.invalid/model.onnx"),
                entry(identity = "org.test.pending", classification = "CANDIDATE_PENDING_DISCOVERY",
                    sourceUri = "unresolved://wake-word/hermes-it", sha256 = ""),
            ),
        )
        assertEquals(listOf("PLACEHOLDER_SOURCE"), codes(findings))
    }

    @Test
    fun `zero hash is blocked for verified entries`() {
        val findings = CatalogIntegrity.audit(
            listOf(entry(sha256 = "0".repeat(64))),
        )
        assertTrue(codes(findings).contains("ZERO_HASH"))
    }

    @Test
    fun `mutable unbound source is blocked`() {
        val findings = CatalogIntegrity.audit(
            listOf(
                entry(identity = "a", sourceUri = "https://hf.co/org/model/resolve/main/m.gguf", revision = null),
                entry(identity = "b", sourceUri = "https://hf.co/org/model/resolve/9217f5db/m.gguf", revision = "9217f5db"),
            ),
        )
        assertEquals(listOf("MUTABLE_SOURCE"), codes(findings))
    }

    @Test
    fun `unapproved license is blocked`() {
        val findings = CatalogIntegrity.audit(
            listOf(entry(licenseApproved = false)),
        )
        assertTrue(codes(findings).contains("LICENSE_NOT_APPROVED"))
    }

    @Test
    fun `abi mismatch is blocked`() {
        val findings = CatalogIntegrity.audit(
            listOf(entry(abis = listOf("x86_64"))),
        )
        assertTrue(codes(findings).contains("ABI_MISMATCH"))
    }

    @Test
    fun `android version mismatch is blocked`() {
        val findings = CatalogIntegrity.audit(
            listOf(entry(minApi = 31)),
        )
        assertTrue(codes(findings).contains("ANDROID_MISMATCH"))
    }

    @Test
    fun `insecure http source is blocked`() {
        val findings = CatalogIntegrity.audit(
            listOf(entry(sourceUri = "http://example.org/model.onnx")),
        )
        assertTrue(codes(findings).contains("INSECURE_SOURCE"))
    }

    @Test
    fun `only verified entries are downloadable`() {
        val entries = listOf(
            entry(identity = "ok"),
            entry(identity = "fixture", classification = "TEST_FIXTURE_ONLY", sourceUri = "mock://x", sha256 = "0".repeat(64)),
            entry(identity = "pending", classification = "CANDIDATE_PENDING_DISCOVERY", sourceUri = "unresolved://x", sha256 = ""),
        )
        assertEquals(listOf("ok"), CatalogIntegrity.downloadable(entries).map { it.identity })
    }

    @Test
    fun `recommended pack excludes optional components`() {
        val entries = listOf(
            entry(identity = "vad", recommended = true),
            entry(identity = "llm", recommended = true, optional = true),
        )
        assertEquals(listOf("vad"), CatalogIntegrity.recommendedPack(entries).map { it.identity })
    }

    /** The shipped asset catalog must audit clean on the physical device profile. */
    @Test
    fun `shipped production catalog audits clean`() {
        val text = javaClass.classLoader!!
            .getResourceAsStream("models/voice_pack_catalog.json")!!
            .bufferedReader().use { it.readText() }
        val catalog = DefaultVoiceModelCatalog.fromText(text)
        val findings = CatalogIntegrity.audit(catalog.all())
        assertTrue(
            "shipped catalog must audit clean, got: $findings",
            findings.none { it.severity == CatalogIntegrity.Severity.ERROR },
        )
        // Core NODE 1 invariants on the shipped catalog:
        val all = catalog.all()
        assertTrue(all.any { it.entryClassification == "TEST_FIXTURE_ONLY" })           // probe kept, separated
        assertTrue(all.none { it.sourceUri.contains("example.invalid") })               // no placeholders
        assertTrue(all.none { it.identity.contains("gemma") })                          // Gemma placeholder superseded
        val minimal = CatalogIntegrity.recommendedPack(all).map { it.identity }
        assertTrue(minimal.contains("org.silero.vad.v5"))
        assertTrue(minimal.contains("org.vosk.model.small-it-0.22"))
        assertTrue(minimal.contains("org.piper.voice.it-riccardo-x-low"))
        assertTrue(minimal.none { it.startsWith("org.qwen.") })                          // LLM stays optional
    }
}
