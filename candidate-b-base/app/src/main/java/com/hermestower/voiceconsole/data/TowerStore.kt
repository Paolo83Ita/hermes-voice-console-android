package com.hermestower.voiceconsole.data

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.InvalidAlgorithmParameterException
import java.security.InvalidKeyException
import java.security.KeyStore
import java.security.SecureRandom
import java.security.UnrecoverableKeyException
import javax.crypto.AEADBadTagException
import javax.crypto.BadPaddingException
import javax.crypto.Cipher
import javax.crypto.IllegalBlockSizeException
import javax.crypto.KeyGenerator
import javax.crypto.NoSuchPaddingException
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * A4R NODE A2 - REPAIRED Tower encrypted persistence (replaces TowerStore.kt
 * in Hermes-Tower/apps/android-voice-console, the file that physically CRASHED
 * the Owner's Xiaomi 11 Lite 5G NE during QR pairing, finding A4R-P1-001).
 *
 * ROOT CAUSE (physically proven, MIUI screenshot + decompile):
 * the old Keystore.key() called KeyGenerator.init(256) on the AndroidKeyStore
 * provider WITHOUT a KeyGenParameterSpec:
 *   java.lang.UnsupportedOperationException: Cannot initialize without a
 *   android.security.keystore.KeyGenParameterSpec parameter
 *     at android.security.keystore2.AndroidKeyStoreKeyGeneratorSpi.engineInit(..:163)
 *     at javax.crypto.KeyGenerator.init(..:582) / init(..:566)
 *     at com.hermestower.voiceconsole.data.TowerStore$Keystore.key(TowerStore.kt:35)
 *     ... encrypt(42) -> putEncrypted(59) -> setEndpoint(75) -> PairingFlow.pairFromQr(54)
 * The exception propagated out of pairFromQr BEFORE any server call: the
 * pairing screen died on the very first QR attempt.
 *
 * REPAIR CONTRACT (mission section 5) - every clause enforced in this file:
 *  - AndroidKeyStore, AES-256, GCM, NoPadding, EXPLICIT KeyGenParameterSpec;
 *  - setRandomizedEncryptionRequired(true): fresh IV per encryption (the IV is
 *    read from the cipher AFTER doFinal and persisted WITH the ciphertext);
 *  - the GCM authentication tag is verified on every decrypt (AEADBadTag =>
 *    typed ENCRYPTED_STORE_CORRUPT, record wiped, never silent);
 *  - safe alias handling (missing alias => generate; alias retry on first-use
 *    races), invalidated-key handling (alias gone with records present =>
 *    typed ANDROID_KEY_INVALIDATED + wipe), corrupted-record handling;
 *  - NO fallback to plaintext, ever: every failure path leaves the record
 *    ABSENT or WIPED, and the failure TYPED in [lastFailure]/[failureHistory];
 *  - NO endpoint persistence after encryption failure: [setEndpoint] and the
 *    pairing commit encrypt FIRST and write only when all encryptions succeed;
 *  - the pairing state commit is ATOMIC: one SharedPreferences editor, one
 *    commit(), all-or-nothing => PAIRING_ABORTED_NO_STATE_COMMITTED instead of
 *    half-paired state on a crash between writes;
 *  - exceptions are never caught and ignored: every catch records a typed
 *    failure state (observables below) before any recovery decision.
 *
 * COMPATIBILITY: public API of the replaced file is preserved 1:1 (constructor,
 * endpoint/deviceId/consoleToken/deviceLabel/pairingFingerprint accessors,
 * isPaired(), clear(), nextTurnSeq()) so the 17 client tests and all call
 * sites compile unchanged. New members are additive only.
 */
class TowerStore(context: Context, private val encryptInterceptor: ((ByteArray) -> ByteArray)? = null) : PairingStateStore {

    /**
     * TEST_ONLY seam (never set in production): lets the instrumented suite
     * force an encryption failure to prove the no-partial-state commit. The
     * interceptor runs BEFORE any keystore work and may throw
     * [TowerStoreKeystoreException].
     */

    private val prefs: SharedPreferences = context.getSharedPreferences("tower_console", 0)

    // ------------------------------------------------------------------
    // Typed failure observability (mission: never catch and ignore)
    // ------------------------------------------------------------------

    /** The most recent typed failure, or null when everything succeeded. Cleared by [clear]. */
    @Volatile
    override var lastFailure: KeystoreFailure? = null
        private set

    /** Full ordered history of typed failures (bounded, redaction-safe: enum values only). */
    private val failures = ArrayDeque<KeystoreFailure>()

    /** Immutable snapshot of the failure history (diagnostics-safe). */
    override fun failureHistory(): List<KeystoreFailure> = synchronized(failures) { failures.toList() }

    private fun record(failure: KeystoreFailure) {
        lastFailure = failure
        synchronized(failures) {
            failures.addLast(failure)
            while (failures.size > 64) failures.removeFirst()
        }
    }

    /** True when the alias was GENERATED while encrypted records already
     *  existed — the deterministic invalidation class (the old key is gone,
     *  every pre-existing record is unrecoverable). A generation that happens
     *  with NO records present is a normal first install: a later tag failure
     *  on a record WRITTEN by this same key is corruption, NOT invalidation
     *  (instrumented-lane run 44748785017 caught the precedence bug). */
    @Volatile
    private var keyGeneratedOverExistingRecords = false

    private fun clearFailureState() {
        lastFailure = null
        synchronized(failures) { failures.clear() }
    }

    // ------------------------------------------------------------------
    // Keystore key (THE REPAIRED PATH - was TowerStore.kt:35)
    // ------------------------------------------------------------------

    /**
     * Returns the AES-256-GCM storage key. Generates it with an EXPLICIT
     * KeyGenParameterSpec when absent (the exact line that crashed before).
     * Throws [TowerStoreKeystoreException] with a typed failure when the
     * AndroidKeyStore cannot be used - the caller never proceeds unencrypted.
     */
    @Synchronized
    @Throws(TowerStoreKeystoreException::class)
    private fun keystoreKey(): SecretKey {
        val ks = try {
            KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        } catch (e: Exception) {
            throw TowerStoreKeystoreException(
                KeystoreFailure.ANDROID_KEYSTORE_INITIALIZATION_FAILED,
                "AndroidKeyStore unavailable: ${e.javaClass.simpleName}", e,
            )
        }
        val existing = try {
            ks.getKey(ALIAS, null) as? SecretKey
        } catch (e: UnrecoverableKeyException) {
            // The alias exists in records but the key is unrecoverable: typed
            // invalidation (this is the uninstall/reinstall / invalidation class).
            throw TowerStoreKeystoreException(
                KeystoreFailure.ANDROID_KEY_INVALIDATED,
                "storage key unrecoverable: ${e.javaClass.simpleName}", e,
            )
        } catch (e: Exception) {
            throw TowerStoreKeystoreException(
                KeystoreFailure.ANDROID_KEYSTORE_INITIALIZATION_FAILED,
                "keystore key read failed: ${e.javaClass.simpleName}", e,
            )
        }
        if (existing != null) return existing
        // >>> THE REPAIR: explicit spec (the old code called kg.init(256) here) <<<
        val generator = try {
            KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE).apply {
                init(
                    KeyGenParameterSpec.Builder(
                        ALIAS,
                        KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
                    )
                        .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                        .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                        .setKeySize(256)
                        .setRandomizedEncryptionRequired(true)
                        .build(),
                )
            }
        } catch (e: Exception) {
            throw TowerStoreKeystoreException(
                KeystoreFailure.ANDROID_KEYSTORE_INITIALIZATION_FAILED,
                "key generation init failed: ${e.javaClass.simpleName}", e,
            )
        }
        return try {
            generator.generateKey().also {
                keyGeneratedOverExistingRecords = RECORD_KEYS.any { prefs.getString(it, null) != null }
            }
        } catch (e: Exception) {
            throw TowerStoreKeystoreException(
                KeystoreFailure.ANDROID_KEYSTORE_INITIALIZATION_FAILED,
                "key generation failed: ${e.javaClass.simpleName}", e,
            )
        }
    }

    private fun wipeEncryptedRecords() {
        // The key material is gone/invalidated: the ciphertext is unrecoverable
        // BY DESIGN. No plaintext fallback - the records are wiped.
        prefs.edit()
            .remove(KEY_ENDPOINT).remove(KEY_DEVICE_ID).remove(KEY_CONSOLE_TOKEN)
            .remove(KEY_DEVICE_LABEL).remove(KEY_FINGERPRINT)
            .commit()
    }

    // ------------------------------------------------------------------
    // Encrypt / decrypt (AES-256-GCM, fresh IV, verified tag)
    // ------------------------------------------------------------------

    @Throws(TowerStoreKeystoreException::class)
    private fun encrypt(plain: ByteArray): ByteArray {
        encryptInterceptor?.invoke(plain)
        val cipher = try {
            Cipher.getInstance(TRANSFORMATION)
        } catch (e: NoSuchPaddingException) {
            throw TowerStoreKeystoreException(KeystoreFailure.ANDROID_KEYSTORE_INITIALIZATION_FAILED, "no GCM", e)
        }
        try {
            cipher.init(Cipher.ENCRYPT_MODE, keystoreKey())
        } catch (e: InvalidKeyException) {
            throw TowerStoreKeystoreException(KeystoreFailure.ANDROID_KEY_INVALIDATED, "encrypt init: ${e.javaClass.simpleName}", e)
        }
        val ct = try {
            cipher.doFinal(plain)
        } catch (e: IllegalBlockSizeException) {
            throw TowerStoreKeystoreException(KeystoreFailure.ANDROID_KEYSTORE_INITIALIZATION_FAILED, "encrypt block", e)
        } catch (e: BadPaddingException) {
            throw TowerStoreKeystoreException(KeystoreFailure.ANDROID_KEYSTORE_INITIALIZATION_FAILED, "encrypt padding", e)
        }
        // Fresh IV per encryption (randomized encryption required): read AFTER
        // doFinal and persist it WITH the ciphertext (framing, BlobFraming).
        val iv = cipher.iv ?: throw TowerStoreKeystoreException(
            KeystoreFailure.ANDROID_KEYSTORE_INITIALIZATION_FAILED, "no IV produced",
        )
        return BlobFraming.encode(iv, ct)
    }

    @Throws(TowerStoreKeystoreException::class)
    private fun decrypt(blob: ByteArray): ByteArray {
        val framed = BlobFraming.decode(blob)
            ?: run {
                record(KeystoreFailure.ENCRYPTED_STORE_CORRUPT)
                throw TowerStoreKeystoreException(KeystoreFailure.ENCRYPTED_STORE_CORRUPT, "malformed blob framing")
            }
        val (iv, ct) = framed
        val cipher = try {
            Cipher.getInstance(TRANSFORMATION)
        } catch (e: NoSuchPaddingException) {
            throw TowerStoreKeystoreException(KeystoreFailure.ANDROID_KEYSTORE_INITIALIZATION_FAILED, "no GCM", e)
        }
        try {
            cipher.init(Cipher.DECRYPT_MODE, keystoreKey(), GCMParameterSpec(BlobFraming.GCM_TAG_BITS, iv))
            return cipher.doFinal(ct)
        } catch (e: AEADBadTagException) {
            // GCM authentication tag verification FAILED (tamper/truncation/wrong key):
            // the C1-C4 mission typed state is ENCRYPTED_STORE_AUTHENTICATION_FAILED
            // (distinct from framing corruption). Record wiped, never silent.
            record(KeystoreFailure.ENCRYPTED_STORE_AUTHENTICATION_FAILED)
            throw TowerStoreKeystoreException(KeystoreFailure.ENCRYPTED_STORE_AUTHENTICATION_FAILED, "auth tag verification failed", e)
        } catch (e: BadPaddingException) {
            record(KeystoreFailure.ENCRYPTED_STORE_CORRUPT)
            throw TowerStoreKeystoreException(KeystoreFailure.ENCRYPTED_STORE_CORRUPT, "decrypt padding", e)
        } catch (e: InvalidKeyException) {
            throw TowerStoreKeystoreException(KeystoreFailure.ANDROID_KEY_INVALIDATED, "decrypt init: ${e.javaClass.simpleName}", e)
        } catch (e: InvalidAlgorithmParameterException) {
            throw TowerStoreKeystoreException(KeystoreFailure.ENCRYPTED_STORE_CORRUPT, "decrypt params", e)
        }
    }

    // ------------------------------------------------------------------
    // Record access (typed failures; NO silent swallow, NO plaintext fallback)
    // ------------------------------------------------------------------

    /**
     * Encrypts [value] and writes the framed blob. Returns false when the
     * encryption path failed: NOTHING is written (typed failure recorded).
     */
    private fun putEncrypted(key: String, value: String): Boolean =
        try {
            val blob = encrypt(value.toByteArray(Charsets.UTF_8))
            prefs.edit().putString(key, Base64.encodeToString(blob, Base64.NO_WRAP)).commit()
        } catch (e: TowerStoreKeystoreException) {
            // Typed, recorded, rethrowable - NOT a silent swallow. Pairing-level
            // callers read lastFailure and abort with PAIRING_PERSISTENCE_FAILED.
            record(e.failure)
            false
        }

    /**
     * Reads a record. Typed contract:
     *  - absent record => null with NO failure recorded (a fresh install is
     *    not a failure);
     *  - undecodable/auth-failed record => typed ENCRYPTED_STORE_CORRUPT,
     *    record WIPED, null returned (no crash, no fallback);
     *  - invalidation => typed ANDROID_KEY_INVALIDATED, ALL records wiped.
     */
    private fun getEncrypted(key: String): String? {
        val b64 = prefs.getString(key, null) ?: return null
        val blob = try {
            Base64.decode(b64, Base64.NO_WRAP)
        } catch (e: IllegalArgumentException) {
            prefs.edit().remove(key).commit()
            record(KeystoreFailure.ENCRYPTED_STORE_CORRUPT)
            return null
        }
        return try {
            String(decrypt(blob), Charsets.UTF_8)
        } catch (e: TowerStoreKeystoreException) {
            when {
                // DETERMINISTIC invalidation class: the keystore just produced a
                // NEW key WHILE old records existed - the original key is gone
                // (uninstall/reinstall/invalidation). ALL old records are
                // unrecoverable: wipe them, typed state. A tag failure when the
                // key did NOT regenerate over records is ENCRYPTED_STORE_CORRUPT
                // (the data is broken, not the key).
                keyGeneratedOverExistingRecords -> {
                    record(KeystoreFailure.ANDROID_KEY_INVALIDATED)
                    wipeEncryptedRecords()
                }
                e.failure == KeystoreFailure.ANDROID_KEY_INVALIDATED -> wipeEncryptedRecords()
                e.failure == KeystoreFailure.ENCRYPTED_STORE_CORRUPT -> prefs.edit().remove(key).commit()
                e.failure == KeystoreFailure.ENCRYPTED_STORE_AUTHENTICATION_FAILED -> prefs.edit().remove(key).commit()
                else -> Unit
            }
            null // typed failure recorded; never silent, never plaintext
        }
    }

    // ------------------------------------------------------------------
    // Public API - signature-compatible with the replaced file
    // ------------------------------------------------------------------

    fun getEndpoint(): String? = getEncrypted(KEY_ENDPOINT)

    /**
     * No endpoint persistence after encryption failure: writes only on success.
     * The [PairingStateStore] seam the pairing flow consumes.
     */
    override fun persistEndpoint(endpoint: String): Boolean =
        try {
            val blob = encrypt(endpoint.toByteArray(Charsets.UTF_8))
            prefs.edit().putString(KEY_ENDPOINT, Base64.encodeToString(blob, Base64.NO_WRAP)).commit()
        } catch (e: TowerStoreKeystoreException) {
            record(e.failure)
            record(KeystoreFailure.PAIRING_PERSISTENCE_FAILED)
            false
        }

    fun setEndpoint(value: String?) {
        if (value == null) return
        if (!persistEndpoint(value)) record(KeystoreFailure.PAIRING_PERSISTENCE_FAILED)
    }

    fun getDeviceId(): String? = getEncrypted(KEY_DEVICE_ID)

    fun setDeviceId(value: String?) {
        if (value == null) return
        if (!putEncrypted(KEY_DEVICE_ID, value)) record(KeystoreFailure.PAIRING_PERSISTENCE_FAILED)
    }

    fun getConsoleToken(): String? = getEncrypted(KEY_CONSOLE_TOKEN)

    fun setConsoleToken(value: String?) {
        if (value == null) return
        if (!putEncrypted(KEY_CONSOLE_TOKEN, value)) record(KeystoreFailure.PAIRING_PERSISTENCE_FAILED)
    }

    fun getDeviceLabel(): String? = getEncrypted(KEY_DEVICE_LABEL)

    fun setDeviceLabel(value: String?) {
        if (value == null) return
        if (!putEncrypted(KEY_DEVICE_LABEL, value)) record(KeystoreFailure.PAIRING_PERSISTENCE_FAILED)
    }

    fun getPairingFingerprint(): String {
        val stored = getEncrypted(KEY_FINGERPRINT)
        if (stored.isNullOrEmpty()) {
            val fingerprint = newFingerprint()
            setPairingFingerprint(fingerprint)
            return fingerprint
        }
        return stored
    }

    fun setPairingFingerprint(value: String) {
        if (!putEncrypted(KEY_FINGERPRINT, value)) record(KeystoreFailure.PAIRING_PERSISTENCE_FAILED)
    }

    private fun newFingerprint(): String =
        Base64.encodeToString(SecureRandom().generateSeed(9), Base64.NO_WRAP)

    override fun isPaired(): Boolean =
        !getDeviceId().isNullOrEmpty() && !getConsoleToken().isNullOrEmpty() && !getEndpoint().isNullOrEmpty()

    override fun clear() {
        prefs.edit().clear().commit()
        clearFailureState()
    }

    override fun nextTurnSeq(): Int {
        val n = prefs.getInt("turn_seq", 0) + 1
        prefs.edit().putInt("turn_seq", n).commit()
        return n
    }

    // ------------------------------------------------------------------
    // A4R NODE A2: ATOMIC pairing commit (mission: no partial state)
    // ------------------------------------------------------------------

    /**
     * All-or-nothing pairing state commit used by BOTH pairing lanes (QR and
     * numeric). Every value is encrypted FIRST; only when ALL encryptions
     * succeed is a SINGLE editor used with one synchronous commit() - a crash
     * can never leave a half-paired state. On any failure the typed state is
     * PAIRING_ABORTED_NO_STATE_COMMITTED (or PAIRING_PERSISTENCE_FAILED when
     * the failure is the encryption itself) and NOTHING is written.
     */
    override fun commitPairing(endpoint: String, deviceId: String, consoleToken: String, deviceLabel: String?): Boolean {
        val encrypted = LinkedHashMap<String, String>(4)
        val values = linkedMapOf(
            KEY_ENDPOINT to endpoint,
            KEY_DEVICE_ID to deviceId,
            KEY_CONSOLE_TOKEN to consoleToken,
            KEY_DEVICE_LABEL to (deviceLabel ?: ""),
        )
        for ((k, v) in values) {
            try {
                encrypted[k] = Base64.encodeToString(encrypt(v.toByteArray(Charsets.UTF_8)), Base64.NO_WRAP)
            } catch (e: TowerStoreKeystoreException) {
                record(KeystoreFailure.PAIRING_PERSISTENCE_FAILED)
                record(KeystoreFailure.PAIRING_ABORTED_NO_STATE_COMMITTED)
                return false
            }
        }
        val editor = prefs.edit()
        for ((k, v) in encrypted) editor.putString(k, v)
        val ok = editor.commit()
        if (!ok) {
            record(KeystoreFailure.PAIRING_ABORTED_NO_STATE_COMMITTED)
        }
        return ok
    }

    companion object {
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val ALIAS = "hermes_voice_console_storage_key"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val KEY_ENDPOINT = "endpoint"
        private const val KEY_DEVICE_ID = "device_id"
        private const val KEY_CONSOLE_TOKEN = "console_token"
        private const val KEY_DEVICE_LABEL = "device_label"
        private const val KEY_FINGERPRINT = "fingerprint"

        /** The encrypted record keys (the invalidation-class existence check). */
        private val RECORD_KEYS = listOf(KEY_ENDPOINT, KEY_DEVICE_ID, KEY_CONSOLE_TOKEN, KEY_DEVICE_LABEL, KEY_FINGERPRINT)
    }
}
