package com.hermestower.voiceconsole.domain

import kotlinx.coroutines.flow.StateFlow

/**
 * System-side contracts: motion, health, secrets, events, diagnostics.
 */

/** Animation policy source for the Tower core (reduced motion, intensity). */
interface AnimationStateController {
    val reducedMotion: StateFlow<Boolean>
    val intensity: StateFlow<Float>
}

/** Truthful device facts only; null means "unavailable", never a fabricated number (NFR truth rule). */
interface DeviceHealthMonitor {
    data class DeviceHealthSnapshot(
        val manufacturer: String?,
        val model: String?,
        val androidRelease: String?,
        val sdkInt: Int?,
        val memoryTotalMb: Long?,
        val memoryAvailableMb: Long?,
        val storageFreeGb: Double?,
        val batteryTemperatureC: Double?,
        val isCharging: Boolean?,
        val refreshRateHz: Float?,
        val thermalStatus: String?,
    )

    fun snapshot(): DeviceHealthSnapshot
    fun statusSummary(): String
}

/** Device-bound secret storage. V0.1 holds NO secrets (SEC-01); Keystore wiring is V0.5. */
interface SecureConfigurationStore {
    enum class Capability { IN_MEMORY_ONLY, KEYSTORE_BOUND }
    val capability: Capability
    fun put(key: String, value: CharArray)
    fun get(key: String): CharArray?
    fun clear(key: String)
}

/** Append-only local event ring powering redacted diagnostics export. */
interface LocalEventStore {
    data class Event(val at: java.time.Instant, val type: String, val detail: String)

    fun log(type: String, detail: String)
    fun recent(limit: Int): List<Event>
    fun clear()
}

/** Builds the redacted diagnostics report (FR-36, SEC-06). */
interface DiagnosticsExporter {
    fun exportRedactedReport(): String
}
