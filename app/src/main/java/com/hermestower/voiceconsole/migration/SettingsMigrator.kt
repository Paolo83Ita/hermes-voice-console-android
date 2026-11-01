package com.hermestower.voiceconsole.migration

/**
 * Platform-free key/value store so the V0.1→V0.2 settings migration is unit-testable
 * on the JVM; SharedPreferences-backed on Android.
 */
interface KeyValueStore {
    fun keys(): Set<String>
    fun getString(key: String): String?
    fun putString(key: String, value: String)
    fun remove(key: String)
    fun clear()
}

class InMemoryKeyValueStore(
    initial: Map<String, String> = emptyMap(),
) : KeyValueStore {
    private val map = LinkedHashMap(initial)
    override fun keys(): Set<String> = synchronized(map) { map.keys.toSet() }
    override fun getString(key: String): String? = synchronized(map) { map[key] }
    override fun putString(key: String, value: String) { synchronized(map) { map[key] = value } }
    override fun remove(key: String) { synchronized(map) { map.remove(key) } }
    override fun clear() = synchronized(map) { map.clear() }
}

/**
 * Versioned settings migration from the V0.1 schema (GRAPH 2 §18).
 *
 * Rules:
 *  - preserve theme, reduced-motion, animation intensity, burn-in, screen-on,
 *    captions, scrim, speaking rate, and conversation-history compatibility flag;
 *  - preserve Mock mode where configured;
 *  - create an empty secure Tower credential slot (no secret is ever migrated —
 *    V0.1 stored none, and nothing is imported from an unqualified source);
 *  - unknown fields from the past and from the future are preserved verbatim;
 *  - corrupt values fall back to defaults with an explicit recovery note;
 *  - the previous configuration is preserved for rollback.
 */
object SettingsMigrator {

    const val CURRENT_SCHEMA_VERSION = 2
    const val SCHEMA_KEY = "schema_version"

    // V0.1 keys (SettingsRepository.kt).
    val V01_KEYS = listOf(
        "dark_theme", "follow_system_theme", "reduced_motion", "animation_intensity",
        "burn_in_guard", "keep_screen_on", "captions_enabled", "brightness_scrim", "speaking_rate",
    )

    data class MigrationReport(
        val fromSchema: Int?,
        val toSchema: Int,
        val preservedKeys: List<String>,
        val recoveredKeys: List<String>,
        val preservedUnknownKeys: List<String>,
        val freshInstall: Boolean,
        val rollbackPointCreated: Boolean,
        val towerCredentialSlotCreated: Boolean,
    )

    fun migrate(store: KeyValueStore): MigrationReport {
        val rawSchema = store.getString(SCHEMA_KEY)?.toIntOrNull()
        val knownKeys = V01_KEYS.toSet() + V02_DEFAULT_KEYS
        val unknownKeys = store.keys().filter { it !in knownKeys && it != SCHEMA_KEY }
        val fresh = rawSchema == null && V01_KEYS.none { store.getString(it) != null }

        if (fresh) {
            writeDefaults(store)
            store.putString(SCHEMA_KEY, CURRENT_SCHEMA_VERSION.toString())
            ensureTowerCredentialSlot(store)
            return MigrationReport(
                fromSchema = null,
                toSchema = CURRENT_SCHEMA_VERSION,
                preservedKeys = emptyList(),
                recoveredKeys = emptyList(),
                preservedUnknownKeys = unknownKeys,
                freshInstall = true,
                rollbackPointCreated = false,
                towerCredentialSlotCreated = true,
            )
        }

        // Rollback point: copy the pre-migration state verbatim.
        val backup = BackupStore.read(store)
        store.putString(BACKUP_KEY, backup)

        val preserved = mutableListOf<String>()
        val recovered = mutableListOf<String>()
        for (key in V01_KEYS) {
            val value = store.getString(key)
            if (value == null) {
                store.putString(key, DEFAULTS[key]!!)
                recovered += key
                continue
            }
            val valid = when (key) {
                "animation_intensity", "brightness_scrim", "speaking_rate" -> value.toFloatOrNull()?.let { it in 0f..2f } == true
                else -> value == "true" || value == "false"
            }
            if (!valid) {
                store.putString(key, DEFAULTS[key]!!)
                recovered += key
            } else {
                preserved += key
            }
        }

        // Mock mode preservation: an existing V0.1 mock configuration stays mock (bridge_mode).
        if (store.getString("bridge_mode") == null) {
            store.putString("bridge_mode", "MOCK")
        }

        ensureTowerCredentialSlot(store)
        store.putString(SCHEMA_KEY, CURRENT_SCHEMA_VERSION.toString())

        return MigrationReport(
            fromSchema = rawSchema,
            toSchema = CURRENT_SCHEMA_VERSION,
            preservedKeys = preserved,
            recoveredKeys = recovered,
            preservedUnknownKeys = unknownKeys,
            freshInstall = false,
            rollbackPointCreated = true,
            towerCredentialSlotCreated = true,
        )
    }

    /** Rollback to the last compatible pre-migration snapshot (GRAPH 2 §18). */
    fun rollback(store: KeyValueStore): Boolean {
        val backup = store.getString(BACKUP_KEY) ?: return false
        val entries = backup.split("\u0001").filter { it.isNotBlank() }
        store.clear()
        entries.forEach { entry ->
            val idx = entry.indexOf('=')
            if (idx > 0) store.putString(entry.substring(0, idx), entry.substring(idx + 1))
        }
        return true
    }

    private fun ensureTowerCredentialSlot(store: KeyValueStore) {
        // The slot exists structurally; the secret itself lives only in the vault.
        if (store.getString("tower_profile_slot") == null) {
            store.putString("tower_profile_slot", "empty")
        }
    }

    private fun writeDefaults(store: KeyValueStore) {
        DEFAULTS.forEach { (k, v) -> store.putString(k, v) }
        store.putString("bridge_mode", "MOCK")
    }

    private val V02_DEFAULT_KEYS = listOf("bridge_mode", "tower_profile_slot")

    val DEFAULTS = mapOf(
        "dark_theme" to "true",
        "follow_system_theme" to "false",
        "reduced_motion" to "false",
        "animation_intensity" to "1.0",
        "burn_in_guard" to "true",
        "keep_screen_on" to "true",
        "captions_enabled" to "true",
        "brightness_scrim" to "0.0",
        "speaking_rate" to "1.0",
    )

    const val BACKUP_KEY = "migration_backup_v2"

    object BackupStore {
        fun read(store: KeyValueStore): String =
            (store.keys() - SCHEMA_KEY - BACKUP_KEY)
                .mapNotNull { key -> store.getString(key)?.let { "$key=$it" } }
                .joinToString("\u0001")
    }
}
