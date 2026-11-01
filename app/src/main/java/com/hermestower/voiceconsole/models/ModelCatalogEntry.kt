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
