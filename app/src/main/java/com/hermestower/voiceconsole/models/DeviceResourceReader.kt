package com.hermestower.voiceconsole.models

import kotlinx.serialization.Serializable

/**
 * Physical device facts read by DeviceResourceReader (GRAPH 2 §5 first-launch flow
 * steps 1–8). Every value is platform-sourced; nothing is estimated silently.
 */
@Serializable
data class DeviceResources(
    val deviceModel: String,
    val androidVersion: Int,
    val supportedAbis: List<String>,
    val totalRamBytes: Long,
    val availableRamBytes: Long,
    val freeStorageBytes: Long,
    val ttsEnginePackages: List<String>,
    val networkOnline: Boolean,
    val networkMetered: Boolean,
    val thermalStatus: Int?,
    val powerSaving: Boolean?,
)

/** Read-back view of what the resolver recommends (first-launch flow steps 9–16). */
@Serializable
data class VoicePackRecommendation(
    val profile: String,
    val recommendedProfile: String,
    val deviceModel: String,
    val components: List<ResolvedComponent>,
    val expectedDownloadBytes: Long,
    val expectedInstalledBytes: Long,
    val ramEstimateLowBytes: Long,
    val ramEstimateHighBytes: Long,
    val routeSummary: String,
    val warnings: List<String>,
    val requiresExplicitConfirmation: Boolean = true,
)

@Serializable
data class ResolvedComponent(
    val identity: String,
    val componentType: String,
    val displayName: String,
    val version: String,
    val sourceUri: String,
    val provider: String,
    val license: String,
    val licenseApproved: Boolean,
    val downloadSizeBytes: Long,
    val installedSizeBytes: Long,
    val ramEstimateBytes: Long,
    val compatible: Boolean,
    val incompatibilityReason: String? = null,
    val downloadAllowed: Boolean,
    val downloadBlockReason: String? = null,
    val qualificationState: String,
    val route: String,
)

/**
 * Platform seam for device facts. Android impl reads Build/ActivityManager/StatFs/
 * ConnectivityManager/BatteryManager; JVM tests inject deterministic values.
 */
interface DeviceResourceReader {
    fun read(): DeviceResources
}
