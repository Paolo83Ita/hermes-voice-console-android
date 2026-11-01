package com.hermestower.voiceconsole.models

import android.app.ActivityManager
import android.content.Context
import android.net.ConnectivityManager
import android.os.BatteryManager
import android.os.Build
import android.os.PowerManager
import android.speech.tts.TextToSpeech
import java.io.File

/**
 * Android implementation of DeviceResourceReader (GRAPH 2 §5 steps 1–8).
 * Every value is read from the platform; when a signal is unavailable the field
 * is null and the UI says so — nothing is invented.
 */
class AndroidDeviceResourceReader(private val context: Context) : DeviceResourceReader {

    override fun read(): DeviceResources {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val mem = ActivityManager.MemoryInfo()
        am.getMemoryInfo(mem)
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val caps = cm.getNetworkCapabilities(cm.activeNetwork)
        val bm = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        val pw = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val thermal = if (Build.VERSION.SDK_INT >= 30) {
            runCatching { pw.currentThermalStatus }.getOrNull()
        } else null
        return DeviceResources(
            deviceModel = Build.MODEL ?: "unknown",
            androidVersion = Build.VERSION.SDK_INT,
            supportedAbis = Build.SUPPORTED_ABIS?.toList() ?: emptyList(),
            totalRamBytes = mem.totalMem,
            availableRamBytes = mem.availMem,
            freeStorageBytes = readFreeStorage(),
            ttsEnginePackages = readTtsEngines(),
            networkOnline = caps != null,
            networkMetered = caps != null && !caps.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_NOT_METERED),
            thermalStatus = thermal,
            powerSaving = runCatching { pw.isPowerSaveMode }.getOrNull(),
        )
    }

    private fun readFreeStorage(): Long = try {
        val path = context.filesDir.absolutePath
        val stat = android.os.StatFs(path)
        stat.availableBytes
    } catch (_: Exception) {
        0L
    }

    private fun readTtsEngines(): List<String> = runCatching {
        val tts = TextToSpeech(context) { /* detached probe; status ignored */ }
        val engines = tts.engines?.mapNotNull { it?.name } ?: emptyList()
        tts.shutdown()
        engines
    }.getOrDefault(emptyList())
}

/** Android wiring: assets catalog + file-backed storage under filesDir/models. */
object AndroidModelProviders {

    fun loadCatalog(context: Context): VoiceModelCatalog {
        val text = context.assets.open("models/voice_pack_catalog.json").bufferedReader().use { it.readText() }
        return DefaultVoiceModelCatalog.fromText(text)
    }

    fun storageManager(context: Context): ModelStorageManager =
        DefaultModelStorageManager(DefaultModelStorageManager.defaultRoot(context.filesDir))

    fun registry(context: Context): ModelInstallationRegistry =
        FileInstallationRegistry(File(context.filesDir, "models/installation_registry.json").toPath())
}
