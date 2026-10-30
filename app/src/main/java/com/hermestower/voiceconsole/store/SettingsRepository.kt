package com.hermestower.voiceconsole.store

import android.content.Context
import android.content.SharedPreferences
import com.hermestower.voiceconsole.core.state.ConsoleSettings

/**
 * Non-sensitive UI settings persistence (decision D-004).
 * No credential ever passes through here (SEC-01); secrets have their own contract.
 */
class SettingsRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("hermes_settings", Context.MODE_PRIVATE)

    fun load(): ConsoleSettings {
        val d = ConsoleSettings()
        return ConsoleSettings(
            darkTheme = prefs.getBoolean(K_DARK, d.darkTheme),
            followSystemTheme = prefs.getBoolean(K_FOLLOW, d.followSystemTheme),
            reducedMotion = prefs.getBoolean(K_REDUCED, d.reducedMotion),
            animationIntensity = prefs.getFloat(K_INTENSITY, d.animationIntensity),
            burnInGuard = prefs.getBoolean(K_BURNIN, d.burnInGuard),
            keepScreenOn = prefs.getBoolean(K_SCREEN_ON, d.keepScreenOn),
            captionsEnabled = prefs.getBoolean(K_CAPTIONS, d.captionsEnabled),
            brightnessScrim = prefs.getFloat(K_SCRIM, d.brightnessScrim),
            speakingRate = prefs.getFloat(K_RATE, d.speakingRate),
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
            .apply()
    }

    fun clear() = prefs.edit().clear().apply()

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
    }
}
