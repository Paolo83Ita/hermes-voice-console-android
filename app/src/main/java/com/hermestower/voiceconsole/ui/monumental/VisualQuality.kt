package com.hermestower.voiceconsole.ui.monumental

/**
 * GRAPH 3 WAVE 1 — visual quality profiles, thermal policy and motion cadence.
 * Mission §14: three profiles (LOW / BALANCED default / HIGH post-qualification).
 * No measured numbers are claimed: device-dependent values stay
 * TARGET_PENDING_DEVICE_QUALIFICATION and real thermal read-back arrives through
 * the existing DeviceHealthMonitor.
 */
enum class VisualQuality(val id: String, val label: String) {
    LOW("LOW", "Low — cool and still"),
    BALANCED("BALANCED", "Balanced — recommended"),
    HIGH("HIGH", "High — after device qualification");

    companion object {
        fun from(raw: String?): VisualQuality =
            entries.firstOrNull { it.id == raw?.uppercase() } ?: BALANCED
    }
}

/** Per-profile effect budgets (pure data — unit-testable). */
data class QualityBudget(
    val particleCount: Int,
    val glowEnabled: Boolean,
    val glyphRing: Boolean,
    val ringLayers: Int,
    val effectFpsCap: Int,
    val bitmapLayers: Boolean,
)

object QualityBudgets {
    fun of(q: VisualQuality): QualityBudget = when (q) {
        VisualQuality.LOW -> QualityBudget(
            particleCount = 0, glowEnabled = false, glyphRing = false,
            ringLayers = 1, effectFpsCap = 15, bitmapLayers = false,
        )
        VisualQuality.BALANCED -> QualityBudget(
            particleCount = 6, glowEnabled = true, glyphRing = true,
            ringLayers = 3, effectFpsCap = 30, bitmapLayers = true,
        )
        VisualQuality.HIGH -> QualityBudget(
            particleCount = 12, glowEnabled = true, glyphRing = true,
            ringLayers = 4, effectFpsCap = 60, bitmapLayers = true,
        )
    }
}

/**
 * Thermal ladder fed by REAL device health reads (AndroidDeviceHealthMonitor).
 * Never fabricates a temperature; an unavailable sensor keeps NOMINAL and the
 * diagnostics layer reports SENSOR_UNAVAILABLE.
 */
enum class ThermalStage { NOMINAL, ELEVATED, CRITICAL }

object ThermalVisualPolicy {
    fun apply(stage: ThermalStage, requested: VisualQuality): VisualQuality = when (stage) {
        ThermalStage.NOMINAL -> requested
        ThermalStage.ELEVATED -> if (requested == VisualQuality.HIGH) VisualQuality.BALANCED else requested
        ThermalStage.CRITICAL -> VisualQuality.LOW
    }
}
