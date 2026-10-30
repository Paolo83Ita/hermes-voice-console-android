package com.hermestower.voiceconsole.store

import com.hermestower.voiceconsole.domain.DiagnosticsExporter
import com.hermestower.voiceconsole.domain.DeviceHealthMonitor
import com.hermestower.voiceconsole.domain.LocalEventStore
import com.hermestower.voiceconsole.privacy.LogRedactor

/**
 * Builds the redacted diagnostics report (FR-36). Every line passes LogRedactor;
 * no secret values exist to begin with, and the report never contains one (SEC-06).
 */
class RedactedDiagnosticsExporter(
    private val events: LocalEventStore,
    private val health: DeviceHealthMonitor,
    private val appVersion: String,
) : DiagnosticsExporter {

    override fun exportRedactedReport(): String {
        val sb = StringBuilder()
        sb.appendLine("HERMES VOICE CONSOLE — REDACTED DIAGNOSTICS REPORT")
        sb.appendLine("Generated: ${java.time.Instant.now()}")
        sb.appendLine("App version: $appVersion")
        sb.appendLine()
        sb.appendLine("== DEVICE (truthful values; 'unavailable' means the datum is not exposed) ==")
        val s = health.snapshot()
        sb.appendLine("Manufacturer: ${s.manufacturer ?: "unavailable"}")
        sb.appendLine("Model: ${s.model ?: "unavailable"}")
        sb.appendLine("Android: ${s.androidRelease ?: "unavailable"} (SDK ${s.sdkInt ?: "unavailable"})")
        sb.appendLine("Memory total/available MB: ${s.memoryTotalMb ?: "unavailable"} / ${s.memoryAvailableMb ?: "unavailable"}")
        sb.appendLine("Storage free GB: ${s.storageFreeGb ?: "unavailable"}")
        sb.appendLine("Battery temperature C: ${s.batteryTemperatureC ?: "unavailable"} (charging: ${s.isCharging ?: "unavailable"})")
        sb.appendLine("Refresh rate Hz: ${s.refreshRateHz ?: "unavailable"}")
        sb.appendLine("Thermal status: ${s.thermalStatus ?: "unavailable"}")
        sb.appendLine()
        sb.appendLine("== RECENT EVENTS (redacted, newest last) ==")
        events.recent(100).forEach { e ->
            sb.appendLine("${e.at} [${LogRedactor.redact(e.type)}] ${LogRedactor.redact(e.detail)}")
        }
        sb.appendLine()
        sb.appendLine("== POLICY ==")
        sb.appendLine("Audio retention: OFF by default. Speech audio is never persisted in V0.1.")
        sb.appendLine("This report contains no secrets; token-like strings are redacted by LogRedactor.")
        return sb.toString()
    }
}
