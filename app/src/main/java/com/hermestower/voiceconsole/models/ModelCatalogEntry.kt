package com.hermestower.voiceconsole.models

import kotlinx.serialization.Serializable

/**
 * One catalog/model registry entry — every field required by GRAPH 2 §4.
 * Serialized as the voice-pack manifest (docs/models/VOICE_PACK_MANIFEST.md).
 */
@Serializable
data class ModelCatalogEntry(
    val componentType: String,
    val identity: String,
    val displayName: String,
    val version: String,
    val sourceUri: String,
    val provider: String,
    val license: String,
    val licenseApproved: Boolean = false,
    val format: String,
    val quantization: String,
    val supportedAbis: List<String>,
    val androidMinVersion: Int,
    val downloadSizeBytes: Long,
    val installedSizeBytes: Long,
    val sha256: String,
    val manifestVersion: String,
    val storageLocation: String,
    val ramEstimateBytes: Long,
    val latencyClass: String,
    val languages: List<String>,
    val italianSupported: Boolean,
    val knownLimitations: List<String> = emptyList(),
    // ---- GRAPH 4 NODE 1 (CP-G4-003): verification & provenance fields ----
    /**
     * NODE 1 classification of this entry:
     *  TEST_FIXTURE_ONLY / VERIFIED_DOWNLOADABLE / CANDIDATE_PENDING_DISCOVERY /
     *  SOURCE_UNVERIFIED_DOWNLOAD_BLOCKED / LICENSE_BLOCKED / COMPATIBILITY_BLOCKED /
     *  SUPERSEDED / REJECTED / RUNTIME_BUNDLED (extension: code runtime shipped via Gradle).
     * Only VERIFIED_DOWNLOADABLE entries are downloadable in production.
     */
    val entryClassification: String = "VERIFIED_DOWNLOADABLE",
    /** Authoritative upstream repository (verified), if one exists. */
    val upstreamRepo: String? = null,
    /** Immutable revision binding: release tag, git tag or commit SHA. */
    val revisionBinding: String? = null,
    /** Verified license text URL. */
    val licenseUrl: String? = null,
    /** Runtime engine required to use this component (Gradle-bundled or pending). */
    val runtimeRequirement: String? = null,
    /** How this entry was verified, and when. No invention allowed. */
    val provenanceNote: String? = null,
    /** Minimal-pack membership: shown in RECOMMENDED SETUP (NODE 2). */
    val recommended: Boolean = false,
    /** True for components the minimal pack must NOT auto-download (LLM, alternatives). */
    val optionalComponent: Boolean = false,
    // Lifecycle fields maintained by the installation registry (not part of the static manifest).
    val state: String = ModelInstallState.DISCOVERED.name,
    val active: Boolean = false,
    val previousVersion: String? = null,
    val rollbackAvailable: Boolean = false,
    val lastVerificationAt: String? = null,
    val lastQualificationAt: String? = null,
) {
    fun componentTypeEnum(): ModelComponentType =
        ModelComponentType.entries.first { it.name == componentType }

    fun installState(): ModelInstallState =
        ModelInstallState.entries.first { it.name == state }
}

/** Root of the static catalog manifest shipped in assets/models/. */
@Serializable
data class VoicePackManifest(
    val manifestVersion: String,
    val generatedFor: String,
    val entries: List<ModelCatalogEntry>,
)
