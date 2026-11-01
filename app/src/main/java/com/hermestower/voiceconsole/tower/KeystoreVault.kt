package com.hermestower.voiceconsole.tower

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.io.File
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Android Keystore-backed AES-256-GCM vault (GRAPH 2 GATE-04). Each record is
 * encrypted with a fresh IV; ciphertext+IV are persisted base64 in a private file.
 * The key never leaves AndroidKeyStore; no plaintext secret ever touches disk.
 */
interface VaultFileStore {
    fun read(): Map<String, String>
    fun write(records: Map<String, String>)
}

class PrivateFileVaultStore(private val file: File) : VaultFileStore {
    override fun read(): Map<String, String> {
        if (!file.exists()) return emptyMap()
        val map = LinkedHashMap<String, String>()
        file.readLines().forEach { line ->
            val idx = line.indexOf('=')
            if (idx > 0) map[line.substring(0, idx)] = line.substring(idx + 1)
        }
        return map
    }

    override fun write(records: Map<String, String>) {
        file.parentFile?.mkdirs()
        val tmp = File(file.parentFile, file.name + ".tmp")
        tmp.writeText(records.entries.joinToString("\n") { "${it.key}=${it.value}" })
        check(tmp.renameTo(file) || (file.delete() && tmp.renameTo(file))) { "vault write failed" }
    }
}

class KeystoreVault(private val store: VaultFileStore) : SecretVault {

    override fun available(): VaultAvailability = try {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        if (keyStore.containsAlias(KEY_ALIAS)) VaultAvailability.KEYSTORE_HARDWARE_BACKED
        else VaultAvailability.KEYSTORE_SOFTWARE
    } catch (_: Exception) {
        VaultAvailability.KEYSTORE_SOFTWARE
    }

    override fun put(key: String, value: CharArray) {
        val cipher = Cipher.getInstance(TRANSFORMATION).apply { init(Cipher.ENCRYPT_MODE, getOrCreateKey()) }
        val iv = cipher.iv
        val encrypted = cipher.doFinal(String(value).toByteArray(Charsets.UTF_8))
        synchronized(store) {
            val records = store.read().toMutableMap()
            records[key] = android.util.Base64.encodeToString(iv, android.util.Base64.NO_WRAP) +
                ":" + android.util.Base64.encodeToString(encrypted, android.util.Base64.NO_WRAP)
            store.write(records)
        }
    }

    override fun get(key: String): CharArray? {
        val record = synchronized(store) { store.read()[key] } ?: return null
        val parts = record.split(':', limit = 2)
        if (parts.size != 2) return null
        return try {
            val cipher = Cipher.getInstance(TRANSFORMATION).apply {
                init(
                    Cipher.DECRYPT_MODE,
                    getOrCreateKey(),
                    GCMParameterSpec(128, android.util.Base64.decode(parts[0], android.util.Base64.NO_WRAP)),
                )
            }
            String(cipher.doFinal(android.util.Base64.decode(parts[1], android.util.Base64.NO_WRAP))).toCharArray()
        } catch (_: Exception) {
            null
        }
    }

    override fun remove(key: String): Boolean = synchronized(store) {
        val records = store.read().toMutableMap()
        val removed = records.remove(key) != null
        if (removed) store.write(records)
        removed
    }

    override fun contains(key: String): Boolean = synchronized(store) { store.read().containsKey(key) }

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry)?.let { return it.secretKey }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(KEY_ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .setRandomizedEncryptionRequired(true)
                .build(),
        )
        return generator.generateKey()
    }

    companion object {
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val KEY_ALIAS = "hermes_tower_vault_key"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
    }
}
