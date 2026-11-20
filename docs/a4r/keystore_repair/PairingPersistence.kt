package com.hermestower.voiceconsole.data

/**
 * A4R NODE A2 - typed failure states for the Tower encrypted persistence layer.
 *
 * Mission section 5 requires these states to EXIST as typed values, never as
 * string-sniffed diagnostics and never as swallowed exceptions:
 *
 *  - ANDROID_KEYSTORE_INITIALIZATION_FAILED: the AndroidKeyStore provider or
 *    the key alias could not be opened/generated (keystore service failure,
 *    hardware error) - the operation NEVER proceeds unencrypted;
 *  - ANDROID_KEY_INVALIDATED: the alias is gone or unusable (uninstall/
 *    reinstall, credential reset invalidation, OS key rotation) - the stored
 *    ciphertext is unrecoverable BY DESIGN and is wiped, no plaintext fallback;
 *  - ENCRYPTED_STORE_CORRUPT: the stored blob failed its framing checks or its
 *    GCM authentication tag (tamper/truncation/garbage) - the record is wiped,
 *    the failure is recorded, no fallback;
 *  - PAIRING_PERSISTENCE_FAILED: an encryption failed while committing pairing
 *    state - NOTHING is written (no endpoint persistence after encryption
 *    failure);
 *  - PAIRING_ABORTED_NO_STATE_COMMITTED: the atomic multi-value pairing commit
 *    aborted before a single byte reached SharedPreferences.
 */
enum class KeystoreFailure {
    ANDROID_KEYSTORE_INITIALIZATION_FAILED,
    ANDROID_KEY_INVALIDATED,
    ENCRYPTED_STORE_CORRUPT,
    PAIRING_PERSISTENCE_FAILED,
    PAIRING_ABORTED_NO_STATE_COMMITTED,
}

/** Typed exception carrying a [KeystoreFailure]; NEVER a bare RuntimeException. */
class TowerStoreKeystoreException(
    val failure: KeystoreFailure,
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause)

/**
 * Pure IV/ciphertext framing codec for the encrypted persistence records.
 *
 * Blob layout: [1 byte ivLen][iv (ivLen bytes)][GCM ciphertext+tag].
 *
 * Extracted as a standalone object so the JVM unit suite can represent every
 * byte-level truth (A4R-P1-002: the JVM suite must ONLY test what the JVM can
 * represent; the KeyGenerator/Cipher path is instrumentation-tested against the
 * real AndroidKeyStore and is never simulated here).
 */
object BlobFraming {
    const val MAX_IV_LEN = 255
    const val GCM_TAG_BITS = 128
    const val GCM_IV_BYTES = 12

    /** Encodes [iv] + [ct] into the framing blob; fails fast instead of corrupting. */
    fun encode(iv: ByteArray, ct: ByteArray): ByteArray {
        require(iv.isNotEmpty() && iv.size <= MAX_IV_LEN) { "invalid IV length ${iv.size}" }
        require(ct.isNotEmpty()) { "ciphertext must not be empty" }
        val out = ByteArray(1 + iv.size + ct.size)
        out[0] = iv.size.toByte()
        iv.copyInto(out, 1)
        ct.copyInto(out, 1 + iv.size)
        return out
    }

    /**
     * Decodes the framing. Returns null on malformed input (the caller records
     * ENCRYPTED_STORE_CORRUPT and wipes the record instead of crashing).
     */
    fun decode(blob: ByteArray?): Pair<ByteArray, ByteArray>? {
        if (blob == null || blob.size < 1 + GCM_IV_BYTES + 16) return null // len + IV(12) + tag(16) minimum
        val ivLen = blob[0].toInt() and 0xFF
        if (ivLen != GCM_IV_BYTES) return null // AES-GCM keystore IVs are always 12 bytes
        if (blob.size < 1 + ivLen + 16) return null // no room for a full authentication tag
        val iv = blob.copyOfRange(1, 1 + ivLen)
        val ct = blob.copyOfRange(1 + ivLen, blob.size)
        return iv to ct
    }
}
