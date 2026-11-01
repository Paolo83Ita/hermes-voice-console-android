package com.hermestower.voiceconsole.migration

import android.content.Context
import android.content.SharedPreferences

/**
 * SharedPreferences-backed KeyValueStore over the SAME file the V0.1
 * SettingsRepository used ("hermes_settings"), so the migration reads the real
 * V0.1 data. Non-sensitive settings only (SEC-01): no credential ever passes here.
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
        prefs.edit().putString(key, value).apply()
    }

    override fun remove(key: String) {
        prefs.edit().remove(key).apply()
    }

    override fun clear() {
        prefs.edit().clear().apply()
    }
}
