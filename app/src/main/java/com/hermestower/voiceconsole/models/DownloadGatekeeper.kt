package com.hermestower.voiceconsole.models

/**
 * DownloadGatekeeper (GRAPH 4 §7 thermal/connectivity prechecks): a pure decision
 * function evaluated BEFORE any transfer starts and re-evaluated between pack items.
 * It never invents device facts — it classifies the facts read by DeviceResourceReader.
 *
 * PowerManager thermal statuses (Android): 0 NONE, 1 LIGHT, 2 MODERATE,
 * 3 SEVERE, 4 CRITICAL, 5 EMERGENCY, 6 SHUTDOWN.
 */
object DownloadGatekeeper {

    /** PowerManager.THERMAL_STATUS_* mirrors (constants kept literal for JVM tests). */
    const val THERMAL_MODERATE = 2
    const val THERMAL_SEVERE = 3
    const val THERMAL_CRITICAL = 4

    data class GateDecision(
        val allowed: Boolean,
        val warnings: List<String>,
        val blockReason: String?,
    )

    fun check(resources: DeviceResources, neededBytes: Long): GateDecision {
        val warnings = mutableListOf<String>()
        if (!resources.networkOnline) {
            return GateDecision(false, warnings, "Network gate: offline — no download can start until connectivity returns.")
        }
        val thermal = resources.thermalStatus
        if (thermal != null && thermal >= THERMAL_CRITICAL) {
            return GateDecision(false, warnings, "Thermal gate: device thermally critical (status $thermal) — downloads blocked.")
        }
        if (thermal != null && thermal >= THERMAL_SEVERE) {
            return GateDecision(false, warnings, "Thermal gate: device thermally severe (status $thermal) — downloads blocked until it cools.")
        }
        if (thermal != null && thermal >= THERMAL_MODERATE) {
            warnings += "Thermal status ${thermal}: downloads may be slower; consider waiting for the device to cool."
        }
        if (resources.networkMetered) {
            warnings += "Metered network: this download consumes mobile data — Wi-Fi recommended."
        }
        if (resources.powerSaving == true) {
            warnings += "Power saving is active: downloads and model loading may be throttled."
        }
        if (resources.freeStorageBytes in 0 until neededBytes) {
            return GateDecision(false, warnings, "Storage gate: ${resources.freeStorageBytes} bytes free, $neededBytes needed (reserve excluded — final check at preflight).")
        }
        return GateDecision(true, warnings, null)
    }
}
