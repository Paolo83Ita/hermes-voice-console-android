package com.hermestower.voiceconsole.migration

import android.content.Context
import android.content.SharedPreferences

/**
 * SharedPreferences-backed KeyValueStore over the SAME file the V0.1
 * SettingsRepository used ("hermes_settings"), so the migration reads the real
 * V0.1 data. Non-sensitive settings only (SEC-01): no credential ever passes here.
 *
 * Typed writes: the V0.1 reader (`SettingsRepository`) expects native Boolean/Float
 * values for its keys. The migration speaks strings (platform-free), so this store
 * converts the known setting keys back to their native types on write. Regression
 * pinned by the emulator E2E: string-typed booleans crashed the app at startup
 * (SharedPreferencesImpl.getBoolean ClassCastException).
 */
class SharedPreferencesKeyValueStore(
    context: Context,
    name: String = "hermes_settings",
) : KeyValueStore {

    private val prefs: SharedPreferences = context.getSharedPreferences(name, Context.MODE_PRIVATE)

    override fun keys(): Set<String> = prefs.all.keys

    override fun getString(key: String): String? {
        val value = prefs.all[key] ?: return null
        return value.toString()
    }

    override fun putString(key: String, value: String) {
        val editor = prefs.edit()
        when (key) {
            in BOOLEAN_KEYS -> editor.putBoolean(key, value == "true")
            in FLOAT_KEYS -> {
                val f = value.toFloatOrNull()
                if (f != null) editor.putFloat(key, f) else editor.putString(key, value)
            }
            else -> editor.putString(key, value)
        }
        editor.apply()
    }

    override fun remove(key: String) {
        prefs.edit().remove(key).apply()
    }

    override fun clear() {
        prefs.edit().clear().apply()
    }

    companion object {
        /** V0.1 keys persisted as booleans by SettingsRepository. */
        val BOOLEAN_KEYS = setOf(
            "dark_theme", "follow_system_theme", "reduced_motion",
            "burn_in_guard", "keep_screen_on", "captions_enabled",
        )

        /** V0.1 keys persisted as floats by SettingsRepository. */
        val FLOAT_KEYS = setOf("animation_intensity", "brightness_scrim", "speaking_rate")
    }
}
