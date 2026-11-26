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
 *  - ANDROID_KEY_UNAVAILABLE: the keystore could not be reached AT ALL at this
 *    moment (keyguard-not-unlocked device profile, transient system service
 *    outage) - distinct from generation failure, retryable by the caller;
 *  - ENCRYPTED_STORE_CORRUPT: the stored blob failed its framing checks
 *    (tamper/truncation/garbage) - the record is wiped, the failure is
 *    recorded, no fallback;
 *  - ENCRYPTED_STORE_AUTHENTICATION_FAILED: the GCM authentication tag did not
 *    verify (wrong key / tampered ciphertext) - the record is wiped, the
 *    failure is recorded, no fallback (C1-C4 mission section 4);
 *  - PAIRING_PERSISTENCE_FAILED: an encryption failed while committing pairing
 *    state - NOTHING is written (no endpoint persistence after encryption
 *    failure);
 *  - PAIRING_ABORTED_NO_STATE_COMMITTED: the atomic multi-value pairing commit
 *    aborted before a single byte reached SharedPreferences.
 */
enum class KeystoreFailure {
    ANDROID_KEYSTORE_INITIALIZATION_FAILED,
    ANDROID_KEY_INVALIDATED,
    ANDROID_KEY_UNAVAILABLE,
    ENCRYPTED_STORE_CORRUPT,
    ENCRYPTED_STORE_AUTHENTICATION_FAILED,
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
 * C1-C4 EXTENSION (mission section 4): the pairing state store seam lives here
 * so the pairing flow (B lineage) can be deterministic under the JVM suite
 * (A4R-P1-002 discipline): the REAL store implements it against the
 * AndroidKeyStore; JVM tests use a typed fake; the production pairing flow
 * NEVER talks to plaintext storage.
 *
 * SIMULATED_CONTRACT_TEST note: [PairingStateStore.Fake] is a TEST fixture —
 * it is compiled in main source only because TowerStore implements the seam;
 * it is unreachable from production navigation.
 *
 * The framing is versioned: byte 0 is the IV length; a future envelope
 * version would extend the framing with a version byte and a migration path
 * (no silent format change).
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

/**
 * The store seam the pairing flow (B lineage, PairingFlow.kt) depends on.
 *
 * B's original PairingFlow depended on the concrete TowerStore class, which
 * made the flow JVM-untestable (A4R-P1-002 class). The merge keeps B's
 * semantics EXACTLY (REUSE/EXTEND/ADAPTER, mission section 1) but narrows the
 * dependency to this typed interface: [TowerStore] implements it against the
 * real AndroidKeyStore; the JVM suite uses [PairingStateStore.Fake]; the
 * production flow can never reach a plaintext store because the interface has
 * no plaintext path.
 */
interface PairingStateStore {
    /** Most recent typed failure, or null when every operation succeeded. */
    val lastFailure: KeystoreFailure?

    /**
     * Encrypt-and-persist the endpoint ALONE. Returns false on ANY typed
     * failure and writes NOTHING (mission section 4: no endpoint committed if
     * encryption fails).
     */
    fun persistEndpoint(endpoint: String): Boolean

    /**
     * ATOMIC all-or-nothing pairing commit (mission section 4: no partial
     * pairing state). Returns false and writes NOTHING on failure.
     */
    fun commitPairing(endpoint: String, deviceId: String, consoleToken: String, deviceLabel: String?): Boolean

    /** True when endpoint+deviceId+consoleToken are all present and decryptable. */
    fun isPaired(): Boolean

    /** Wipes every pairing record (revocation, re-pairing, invalidation). */
    fun clear()

    /** Typed failure observability for diagnostics (enum names only, no secrets). */
    fun failureHistory(): List<KeystoreFailure>

    /** Deterministic per-store turn sequence (B public API, unchanged). */
    fun nextTurnSeq(): Int

    /**
     * ANDROID REPAIR CP-005 — the paired device id for the restart read-back
     * (mission section 5), or null when not paired. Redaction note: the
     * device id is a Tower-assigned public identifier, not a secret.
     */
    fun pairedDeviceId(): String? = null

    /**
     * TEST fixture ONLY (SIMULATED_CONTRACT_TEST): an in-memory implementation
     * with the SAME typed contract. It is compiled beside the interface so the
     * JVM suite can exercise the pairing state machine deterministically; it
     * is NOT referenced from any production navigation path (the production
     * graph wires [TowerStore] only).
     */
    class Fake : PairingStateStore {
        override var lastFailure: KeystoreFailure? = null
            private set
        var failEncryptionOnce = false
            private set
        private val data = LinkedHashMap<String, String>()
        private val history = ArrayDeque<KeystoreFailure>()

        fun failNextEncryption() { failEncryptionOnce = true }
        private fun record(failure: KeystoreFailure) {
            lastFailure = failure
            history.addLast(failure)
        }
        override fun persistEndpoint(endpoint: String): Boolean {
            if (failEncryptionOnce) {
                failEncryptionOnce = false
                record(KeystoreFailure.PAIRING_PERSISTENCE_FAILED)
                return false
            }
            data["endpoint"] = endpoint
            return true
        }
        override fun commitPairing(endpoint: String, deviceId: String, consoleToken: String, deviceLabel: String?): Boolean {
            if (failEncryptionOnce) {
                failEncryptionOnce = false
                record(KeystoreFailure.PAIRING_PERSISTENCE_FAILED)
                record(KeystoreFailure.PAIRING_ABORTED_NO_STATE_COMMITTED)
                return false
            }
            data["endpoint"] = endpoint
            data["device_id"] = deviceId
            data["console_token"] = consoleToken
            data["device_label"] = deviceLabel ?: ""
            return true
        }
        override fun isPaired(): Boolean =
            data.containsKey("endpoint") && data.containsKey("device_id") && data.containsKey("console_token")
        override fun clear() {
            data.clear()
            lastFailure = null
        }
        override fun failureHistory(): List<KeystoreFailure> = history.toList()
        private var turnSeq = 0
        override fun nextTurnSeq(): Int = ++turnSeq
        override fun pairedDeviceId(): String? = data["device_id"]
    }
}
