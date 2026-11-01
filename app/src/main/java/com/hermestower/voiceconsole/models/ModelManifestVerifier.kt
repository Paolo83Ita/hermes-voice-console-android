package com.hermestower.voiceconsole.models

/**
 * ModelManifestVerifier (GRAPH 2 §4): structural verification of a manifest entry
 * before it may enter the download-approval flow. Zero-hash entries are structurally
 * valid but never downloadable (no verified binary published).
 */
interface ModelManifestVerifier {
    fun verify(entry: ModelCatalogEntry): ManifestVerification
}

data class ManifestVerification(
    val identity: String,
    val valid: Boolean,
    val problems: List<String>,
)

class DefaultModelManifestVerifier : ModelManifestVerifier {

    override fun verify(entry: ModelCatalogEntry): ManifestVerification {
        val problems = mutableListOf<String>()
        if (entry.identity.isBlank()) problems += "identity blank"
        if (entry.version.isBlank()) problems += "version blank"
        if (entry.sourceUri.isBlank()) problems += "sourceUri blank"
        if (entry.license.isBlank()) problems += "license blank"
        if (entry.manifestVersion.isBlank()) problems += "manifestVersion blank"
        if (entry.downloadSizeBytes <= 0) problems += "downloadSizeBytes not positive"
        if (entry.installedSizeBytes <= 0) problems += "installedSizeBytes not positive"
        if (!entry.sha256.matches(Regex("^[0-9a-f]{64}$"))) problems += "sha256 not a 64-char lowercase hex string"
        if (entry.supportedAbis.isEmpty()) problems += "supportedAbis empty"
        if (entry.androidMinVersion <= 0) problems += "androidMinVersion invalid"
        return ManifestVerification(entry.identity, problems.isEmpty(), problems)
    }
}
