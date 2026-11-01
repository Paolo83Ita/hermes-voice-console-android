package com.hermestower.voiceconsole.models

/**
 * VoicePackResolver (GRAPH 2 §4): deterministic compatibility + recommendation
 * engine. Same inputs always produce the same recommendation; no silent downloads,
 * every component is displayed with size/license/qualification before confirmation.
 */
interface VoicePackResolver {
    fun recommend(
        profile: VoiceModelProfile,
        resources: DeviceResources,
        catalog: VoiceModelCatalog,
    ): VoicePackRecommendation

    fun compatibility(entry: ModelCatalogEntry, resources: DeviceResources): Compatibility
}

data class Compatibility(
    val compatible: Boolean,
    val reason: String? = null,
)

class DefaultVoicePackResolver : VoicePackResolver {

    override fun compatibility(entry: ModelCatalogEntry, resources: DeviceResources): Compatibility {
        if (resources.androidVersion < entry.androidMinVersion) {
            return Compatibility(false, "Requires Android ${entry.androidMinVersion}+ (device: ${resources.androidVersion}).")
        }
        if (entry.supportedAbis.isNotEmpty() && entry.supportedAbis.intersect(resources.supportedAbis.toSet()).isEmpty()) {
            return Compatibility(false, "No supported ABI in common (needs ${entry.supportedAbis.joinToString()}, device: ${resources.supportedAbis.joinToString()}).")
        }
        if (resources.freeStorageBytes < entry.installedSizeBytes + INSTALL_RESERVE_BYTES) {
            return Compatibility(false, "Free storage below installed size + safe reserve.")
        }
        if (resources.availableRamBytes < entry.ramEstimateBytes) {
            return Compatibility(false, "Available RAM below the component RAM estimate.")
        }
        return Compatibility(true)
    }

    override fun recommend(
        profile: VoiceModelProfile,
        resources: DeviceResources,
        catalog: VoiceModelCatalog,
    ): VoicePackRecommendation {
        val policy = VoicePackPolicyResolver.policyFor(profile)
        val warnings = VoicePackPolicyResolver.gatesFor(profile, resources).toMutableList()
        val resolved = mutableListOf<ResolvedComponent>()

        val candidates = (policy.required + policy.optional)
            .flatMap { type -> catalog.ofComponent(type).ifEmpty { emptyList() } }
        for (entry in candidates) {
            val compat = compatibility(entry, resources)
            var downloadAllowed = compat.compatible && entry.licenseApproved && entry.sha256 != ZERO_HASH
            var blockReason: String? = null
            when {
                !compat.compatible -> { downloadAllowed = false; blockReason = compat.reason }
                !entry.licenseApproved -> {
                    downloadAllowed = false
                    blockReason = "License review pending (${entry.license})."
                    if (!warnings.any { it.contains(entry.identity) }) {
                        warnings += "License review pending for ${entry.displayName} (${entry.license})."
                    }
                }
                entry.sha256 == ZERO_HASH -> {
                    downloadAllowed = false
                    blockReason = "Awaiting qualified artifact: no verified binary is published for this candidate yet."
                    if (!warnings.any { it.contains(entry.identity) }) {
                        warnings += "${entry.displayName}: no verified binary published — real-model qualification is a physical-device graph."
                    }
                }
            }
            if (entry.componentType == ModelComponentType.LOCAL_LLM.name && !policy.allowsLocalLlm) {
                downloadAllowed = false
                blockReason = "Profile ${policy::class.simpleName} does not include a local LLM; complex reasoning stays on Hermes Tower."
            }
            resolved += ResolvedComponent(
                identity = entry.identity,
                componentType = entry.componentType,
                displayName = entry.displayName,
                version = entry.version,
                sourceUri = entry.sourceUri,
                provider = entry.provider,
                license = entry.license,
                licenseApproved = entry.licenseApproved,
                downloadSizeBytes = entry.downloadSizeBytes,
                installedSizeBytes = entry.installedSizeBytes,
                ramEstimateBytes = entry.ramEstimateBytes,
                compatible = compat.compatible,
                incompatibilityReason = compat.reason,
                downloadAllowed = downloadAllowed,
                downloadBlockReason = blockReason,
                qualificationState = entry.state,
                route = if (entry.componentType == ModelComponentType.LOCAL_LLM.name && !policy.allowsLocalLlm) "TOWER" else "LOCAL",
            )
        }

        val downloadable = resolved.filter { it.downloadAllowed }
        val expectedDownload = downloadable.sumOf { it.downloadSizeBytes }
        val expectedInstalled = downloadable.sumOf { it.installedSizeBytes }
        val ramLow = downloadable.minOfOrNull { it.ramEstimateBytes } ?: 0L
        val ramHigh = downloadable.sumOf { it.ramEstimateBytes }
        val localShare = resolved.count { it.route == "LOCAL" }
        val routeSummary = when {
            resolved.isEmpty() -> "No components required; console runs in TOWER_ONLY mode."
            localShare == 0 -> "All wake/VAD locally; speech understanding on Hermes Tower."
            resolved.any { it.componentType == ModelComponentType.LOCAL_LLM.name && it.route == "LOCAL" } ->
                "Simple requests local, complex/uncertain/consequential on Hermes Tower."
            else -> "Voice front-end local; understanding on Hermes Tower."
        }
        return VoicePackRecommendation(
            profile = profile.name,
            recommendedProfile = profile.name,
            deviceModel = resources.deviceModel,
            components = resolved.sortedBy { it.componentType },
            expectedDownloadBytes = expectedDownload,
            expectedInstalledBytes = expectedInstalled,
            ramEstimateLowBytes = ramLow,
            ramEstimateHighBytes = ramHigh,
            routeSummary = routeSummary,
            warnings = warnings.distinct(),
        )
    }

    companion object {
        const val ZERO_HASH = "0000000000000000000000000000000000000000000000000000000000000000"
        const val INSTALL_RESERVE_BYTES: Long = 256L * 1024 * 1024
    }
}
