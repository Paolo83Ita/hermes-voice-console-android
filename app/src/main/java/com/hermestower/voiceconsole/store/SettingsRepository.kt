package com.hermestower.voiceconsole.store

import android.content.Context
import android.content.SharedPreferences
import com.hermestower.voiceconsole.core.state.ConsoleSettings

/**
 * Non-sensitive UI settings persistence (decision D-004).
 * No credential ever passes through here (SEC-01); secrets have their own contract.
 *
 * Reads are type-tolerant: after a versioned migration the same file may hold a
 * value written as string (migration) or native Boolean/Float (this repository).
 * A wrong-typed value falls back to the default instead of crashing the app
 * (emulator E2E finding, fixed in Graph 2).
 */
class SettingsRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("hermes_settings", Context.MODE_PRIVATE)

    fun load(): ConsoleSettings {
        val d = ConsoleSettings()
        return ConsoleSettings(
            darkTheme = prefs.tolerantBool(K_DARK, d.darkTheme),
            followSystemTheme = prefs.tolerantBool(K_FOLLOW, d.followSystemTheme),
            reducedMotion = prefs.tolerantBool(K_REDUCED, d.reducedMotion),
            animationIntensity = prefs.tolerantFloat(K_INTENSITY, d.animationIntensity),
            burnInGuard = prefs.tolerantBool(K_BURNIN, d.burnInGuard),
            keepScreenOn = prefs.tolerantBool(K_SCREEN_ON, d.keepScreenOn),
            captionsEnabled = prefs.tolerantBool(K_CAPTIONS, d.captionsEnabled),
            brightnessScrim = prefs.tolerantFloat(K_SCRIM, d.brightnessScrim),
            speakingRate = prefs.tolerantFloat(K_RATE, d.speakingRate),
            // OWNER PHYSICAL RETEST §5: pitch persists and reads back after restart.
            speechPitch = prefs.tolerantFloat(K_PITCH, d.speechPitch),
            visualQuality = prefs.tolerantString(K_VISUAL_QUALITY, d.visualQuality),
            localLlmEnabled = prefs.tolerantBool(K_LLM_ENABLED, d.localLlmEnabled),
        )
    }

    fun persist(settings: ConsoleSettings) {
        prefs.edit()
            .putBoolean(K_DARK, settings.darkTheme)
            .putBoolean(K_FOLLOW, settings.followSystemTheme)
            .putBoolean(K_REDUCED, settings.reducedMotion)
            .putFloat(K_INTENSITY, settings.animationIntensity)
            .putBoolean(K_BURNIN, settings.burnInGuard)
            .putBoolean(K_SCREEN_ON, settings.keepScreenOn)
            .putBoolean(K_CAPTIONS, settings.captionsEnabled)
            .putFloat(K_SCRIM, settings.brightnessScrim)
            .putFloat(K_RATE, settings.speakingRate)
            .putFloat(K_PITCH, settings.speechPitch)
            .putString(K_VISUAL_QUALITY, settings.visualQuality)
            .putBoolean(K_LLM_ENABLED, settings.localLlmEnabled)
            .apply()
    }

    fun clear() = prefs.edit().clear().apply()

    private fun SharedPreferences.tolerantBool(key: String, default: Boolean): Boolean =
        when (val raw = all[key]) {
            is Boolean -> raw
            is String -> raw.toBooleanStrictOrNull() ?: default
            else -> default
        }

    private fun SharedPreferences.tolerantString(key: String, default: String): String =
        when (val raw = all[key]) {
            is String -> raw
            else -> default
        }

    private fun SharedPreferences.tolerantFloat(key: String, default: Float): Float =
        when (val raw = all[key]) {
            is Float -> raw
            is String -> raw.toFloatOrNull() ?: default
            else -> default
        }

    private companion object {
        const val K_DARK = "dark_theme"
        const val K_FOLLOW = "follow_system_theme"
        const val K_REDUCED = "reduced_motion"
        const val K_INTENSITY = "animation_intensity"
        const val K_BURNIN = "burn_in_guard"
        const val K_SCREEN_ON = "keep_screen_on"
        const val K_CAPTIONS = "captions_enabled"
        const val K_SCRIM = "brightness_scrim"
        const val K_RATE = "speaking_rate"
        const val K_PITCH = "speech_pitch"
        const val K_VISUAL_QUALITY = "visual_quality"
        const val K_LLM_ENABLED = "local_llm_enabled"
    }
}
