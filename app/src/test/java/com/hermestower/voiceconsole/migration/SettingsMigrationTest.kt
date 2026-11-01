package com.hermestower.voiceconsole.migration

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Migration cases required by GRAPH 2 §18: fresh install, upgrade from V0.1,
 * partial V0.1 settings, corrupt settings, old unknown field, future unknown
 * field, rollback.
 */
class SettingsMigrationTest {

    private fun v01Store(vararg overrides: Pair<String, String>): InMemoryKeyValueStore {
        val store = InMemoryKeyValueStore(
            mapOf(
                "dark_theme" to "true",
                "follow_system_theme" to "false",
                "reduced_motion" to "false",
                "animation_intensity" to "1.0",
                "burn_in_guard" to "true",
                "keep_screen_on" to "true",
                "captions_enabled" to "true",
                "brightness_scrim" to "0.0",
                "speaking_rate" to "1.0",
            ),
        )
        overrides.forEach { (k, v) -> store.putString(k, v) }
        return store
    }

    @Test
    fun `fresh install writes defaults and creates the credential slot`() {
        val store = InMemoryKeyValueStore()
        val report = SettingsMigrator.migrate(store)
        assertTrue(report.freshInstall)
        assertEquals(2, report.toSchema)
        assertTrue(report.towerCredentialSlotCreated)
        assertEquals("MOCK", store.getString("bridge_mode"))
        assertEquals("empty", store.getString("tower_profile_slot"))
        assertEquals("true", store.getString("burn_in_guard"))
        assertFalse(report.rollbackPointCreated)
    }

    @Test
    fun `upgrade from V0_1 preserves every preference`() {
        val store = v01Store(
            "dark_theme" to "false",
            "reduced_motion" to "true",
            "animation_intensity" to "0.5",
        )
        val report = SettingsMigrator.migrate(store)
        assertEquals(1, report.fromSchema ?: 1) // unversioned V0.1 store migrates from null schema
        assertTrue(report.freshInstall.not())
        assertEquals("false", store.getString("dark_theme"))
        assertEquals("true", store.getString("reduced_motion"))
        assertEquals("0.5", store.getString("animation_intensity"))
        assertEquals("true", store.getString("burn_in_guard"))
        assertTrue(report.rollbackPointCreated)
        assertTrue(report.towerCredentialSlotCreated)
        assertEquals("empty", store.getString("tower_profile_slot"))
        // No secret is ever migrated from an unqualified source.
        assertTrue(store.keys().none { it.contains("secret") || it.contains("token") })
    }

    @Test
    fun `partial V0_1 settings are completed with defaults`() {
        val store = InMemoryKeyValueStore(mapOf("dark_theme" to "false"))
        val report = SettingsMigrator.migrate(store)
        assertTrue(report.recoveredKeys.containsAll(setOf("reduced_motion", "speaking_rate", "burn_in_guard")))
        assertEquals("false", store.getString("dark_theme"))
        assertEquals("false", store.getString("reduced_motion"))
        assertEquals("1.0", store.getString("speaking_rate"))
    }

    @Test
    fun `corrupt settings are recovered`() {
        val store = v01Store("animation_intensity" to "not-a-number", "dark_theme" to "perhaps")
        val report = SettingsMigrator.migrate(store)
        assertTrue(report.recoveredKeys.contains("animation_intensity"))
        assertTrue(report.recoveredKeys.contains("dark_theme"))
        assertEquals("1.0", store.getString("animation_intensity"))
        assertEquals("true", store.getString("dark_theme"))
        assertTrue(report.preservedKeys.contains("reduced_motion"))
    }

    @Test
    fun `old and future unknown fields are preserved`() {
        val store = v01Store("legacy_v0_field" to "old", "future_unknown_field" to "new")
        val report = SettingsMigrator.migrate(store)
        assertTrue(report.preservedUnknownKeys.containsAll(setOf("legacy_v0_field", "future_unknown_field")))
        assertEquals("old", store.getString("legacy_v0_field"))
        assertEquals("new", store.getString("future_unknown_field"))
    }

    @Test
    fun `mock mode is preserved where configured`() {
        val store = v01Store("bridge_mode" to "MOCK")
        SettingsMigrator.migrate(store)
        assertEquals("MOCK", store.getString("bridge_mode"))
    }

    @Test
    fun `rollback restores the exact pre-migration state`() {
        val store = v01Store("dark_theme" to "false", "future_unknown_field" to "keep")
        val migrated = SettingsMigrator.migrate(store)
        assertTrue(migrated.rollbackPointCreated)
        assertTrue(SettingsMigrator.rollback(store))
        assertEquals("false", store.getString("dark_theme"))
        assertEquals("keep", store.getString("future_unknown_field"))
        assertEquals(null, store.getString("tower_profile_slot"))
        assertEquals(null, store.getString(SettingsMigrator.SCHEMA_KEY))
    }

    @Test
    fun `rollback without a migration is refused`() {
        val store = InMemoryKeyValueStore(mapOf("dark_theme" to "true"))
        assertFalse(SettingsMigrator.rollback(store))
    }

    @Test
    fun `idempotent re-migration is stable`() {
        val store = v01Store()
        SettingsMigrator.migrate(store)
        val second = SettingsMigrator.migrate(store)
        assertEquals(2, second.fromSchema)
        assertEquals(2, second.toSchema)
        assertEquals("2", store.getString(SettingsMigrator.SCHEMA_KEY))
    }
}
