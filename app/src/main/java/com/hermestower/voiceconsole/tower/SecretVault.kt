package com.hermestower.voiceconsole.tower

/**
 * SecretVault (GRAPH 2 §8): sensitive connection values are stored separately from
 * profile metadata, encrypted at rest by the Android implementation. The interface
 * is intentionally minimal and platform-free so JVM tests can use the in-memory
 * implementation while the emulator/device uses the Keystore-backed one.
 */
interface SecretVault {
    fun put(key: String, value: CharArray)
    fun get(key: String): CharArray?
    fun remove(key: String): Boolean
    fun contains(key: String): Boolean
    fun available(): VaultAvailability
}

enum class VaultAvailability(val displayLabel: String) {
    KEYSTORE_HARDWARE_BACKED("Android Keystore (hardware-backed where available)"),
    KEYSTORE_SOFTWARE("Android Keystore (software)"),
    IN_MEMORY_ONLY("In-memory only (tests / unavailable Keystore)"),
}

class InMemorySecretVault : SecretVault {

    private val map = HashMap<String, CharArray>()

    override fun put(key: String, value: CharArray) {
        synchronized(map) { map[key] = value.copyOf() }
    }

    override fun get(key: String): CharArray? = synchronized(map) { map[key]?.copyOf() }

    override fun remove(key: String): Boolean = synchronized(map) {
        val removed = map.remove(key)
        removed?.fill(' ')
        removed != null
    }

    override fun contains(key: String): Boolean = synchronized(map) { map.containsKey(key) }

    override fun available(): VaultAvailability = VaultAvailability.IN_MEMORY_ONLY
}

/** Single source of truth for vault key layout (metadata and secrets never mix). */
object VaultKeys {
    fun secretKeyFor(profileId: String) = "tower/$profileId/secret"
    fun pairingKeyFor(profileId: String) = "tower/$profileId/pairing"
}
