package com.hermestower.voiceconsole.health

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.os.PowerManager
import android.os.StatFs
import android.view.Display
import com.hermestower.voiceconsole.domain.DeviceHealthMonitor

/**
 * Real device facts only (FR-36). Every field is sourced from the platform;
 * null means the datum is unavailable on this device/API — never fabricated.
 */
class AndroidDeviceHealthMonitor(private val context: Context) : DeviceHealthMonitor {

    override fun snapshot(): DeviceHealthMonitor.DeviceHealthSnapshot {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager

        val memInfo = ActivityManager.MemoryInfo().also { activityManager?.getMemoryInfo(it) }
        val stat = runCatching { StatFs(android.os.Environment.getDataDirectory().path) }.getOrNull()
        // Battery temperature is not a BatteryManager property on this API level;
        // it is exposed via the sticky ACTION_BATTERY_CHANGED broadcast (tenths of °C).
        val temperature = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            ?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, Int.MIN_VALUE)
        // Application contexts are not associated with a display on API 30+; asking
        // for one throws UnsupportedOperationException and killed the E2E process
        // (emulator run 41341134806, e2e_14). Degrade truthfully instead: null means
        // "unavailable on this context" — the same contract as every other field.
        val display: Display? = runCatching { context.display }.getOrNull()

        return DeviceHealthMonitor.DeviceHealthSnapshot(
            manufacturer = Build.MANUFACTURER,
            model = Build.MODEL,
            androidRelease = Build.VERSION.RELEASE,
            sdkInt = Build.VERSION.SDK_INT,
            memoryTotalMb = memInfo.totalMem.takeIf { it > 0 }?.let { it / (1024 * 1024) },
            memoryAvailableMb = memInfo.availMem.takeIf { it > 0 }?.let { it / (1024 * 1024) },
            storageFreeGb = stat?.availableBytes?.let { it / (1024.0 * 1024 * 1024) },
            // BatteryManager returns tenths of a degree Celsius.
            batteryTemperatureC = temperature?.takeIf { it != Int.MIN_VALUE }?.let { it / 10.0 },
            isCharging = batteryManager?.isCharging,
            refreshRateHz = display?.refreshRate,
            thermalStatus = powerManager?.let { thermalName(it.currentThermalStatus) },
        )
    }

    override fun statusSummary(): String {
        val s = snapshot()
        return listOfNotNull(
            s.androidRelease?.let { "Android $it" },
            s.memoryAvailableMb?.let { "${it / 1024} GB RAM free" },
            s.batteryTemperatureC?.let { "battery ${"%.1f".format(it)} °C" },
        ).joinToString(", ").ifEmpty { "device details unavailable" }
    }

    private fun thermalName(status: Int): String = when (status) {
        PowerManager.THERMAL_STATUS_NONE -> "NONE"
        PowerManager.THERMAL_STATUS_LIGHT -> "LIGHT"
        PowerManager.THERMAL_STATUS_MODERATE -> "MODERATE"
        PowerManager.THERMAL_STATUS_SEVERE -> "SEVERE"
        PowerManager.THERMAL_STATUS_CRITICAL -> "CRITICAL"
        PowerManager.THERMAL_STATUS_EMERGENCY -> "EMERGENCY"
        PowerManager.THERMAL_STATUS_SHUTDOWN -> "SHUTDOWN"
        else -> "UNKNOWN"
    }
}
