package com.hermestower.voiceconsole.locale

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.os.LocaleList
import java.util.Locale

/**
 * GRAPH 4 G4-C08 (Owner delta §3/§13): per-app language selection.
 *
 *  - Default FOLLOW_SYSTEM: the first launch uses the Android system language
 *    when it matches it/en/es/fr/de, English otherwise;
 *  - the Owner can override from Settings → Language; the override persists
 *    across restarts and process recreation and is reversible;
 *  - Android 13+ uses the OFFICIAL per-app locale mechanism
 *    (android.app.LocaleManager — no private APIs); Android 11/12 fall back
 *    to an attachBaseContext configuration override with the same store.
 *  - Untranslatable items (model identities, hashes, protocol identifiers,
 *    URLs, version numbers, file names, diagnostic codes) are NOT localized
 *    by this layer — they live outside string resources.
 */
object AppLanguage {

    const val FOLLOW_SYSTEM = "follow-system"
    val SUPPORTED = listOf("it", "en", "es", "fr", "de")

    private const val PREFS = "app_language"
    private const val KEY = "selected_language"

    fun stored(context: Context): String =
        prefs(context).getString(KEY, FOLLOW_SYSTEM) ?: FOLLOW_SYSTEM

    /** Applies and persists the selection; call recreate() on the activity after. */
    fun apply(context: Context, languageCode: String) {
        val code = if (languageCode in SUPPORTED || languageCode == FOLLOW_SYSTEM) languageCode else FOLLOW_SYSTEM
        prefs(context).edit().putString(KEY, code).apply()
        if (Build.VERSION.SDK_INT >= 33 && code != FOLLOW_SYSTEM) {
            runCatching {
                val lm = context.getSystemService(Context.LOCALE_SERVICE) as android.app.LocaleManager
                lm.applicationLocales = LocaleList.forLanguageTags(code)
            }
        }
        if (Build.VERSION.SDK_INT >= 33 && code == FOLLOW_SYSTEM) {
            // Reset to the system-driven per-app locale (empty = follow system).
            runCatching {
                val lm = context.getSystemService(Context.LOCALE_SERVICE) as android.app.LocaleManager
                lm.applicationLocales = LocaleList.getEmptyLocaleList()
            }
        }
    }

    /** The effective language code for the override ("it".."de"), or null when following system. */
    fun overrideTag(context: Context): String? {
        val stored = stored(context)
        if (stored == FOLLOW_SYSTEM) return null
        return stored
    }

    /** Context wrapper for the API 30–32 fallback path (MainActivity.attachBaseContext). */
    fun wrapContext(base: Context): Context {
        val tag = overrideTag(base) ?: return base
        val locale = Locale.forLanguageTag(tag)
        Locale.setDefault(locale)
        val config = base.resources.configuration
        val newConfig = android.content.res.Configuration(config)
        newConfig.setLocale(locale)
        newConfig.setLocales(LocaleList(locale))
        return base.createConfigurationContext(newConfig)
    }

    /** Resolves the FIRST launch language from the system: supported → system, else English. */
    fun resolveSystemLanguage(): String {
        val sys = LocaleList.getDefault().get(0)?.language?.lowercase(Locale.ROOT) ?: "en"
        return if (sys in SUPPORTED) sys else "en"
    }

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
