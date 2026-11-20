package com.hermestower.voiceconsole.pairing

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyFactory
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.Signature
import java.security.interfaces.ECPublicKey
import java.security.spec.ECGenParameterSpec
import java.security.spec.X509EncodedKeySpec

/**
 * GRAPH A4R C1-C4 — EC-P256 AndroidKeyStore device identity (B lineage,
 * RECONSTRUCTED SOURCE, NOT decompiled code).
 *
 * PROVENANCE: see QrPayload.kt. Bound behavior from B's shipped binary (dex
 * + forensic reading of DeviceKeys.java, byte-exact APK 908b289f…):
 *  - alias "hermes_voice_console_device_key" on the "AndroidKeyStore"
 *    provider, curve secp256r1 (EC-P256, mission section 7);
 *  - SHA256withECDSA proof-of-possession signatures, DER-encoded output
 *    ("DER(SHA256withECDSA)" in B's attestation metadata);
 *  - attestation metadata keys: keystore, algorithm, curve, digest,
 *    signature_format, hardware_backed_note
 *    ("Keystore-bound; private key non-exportable").
 *
 * The private key is NEVER exportable (no import path, no plaintext key
 * material anywhere in this class). The A2 receipt recorded that B's
 * DeviceKeys already used the correct spec API — the AES storage key was the
 * only keystore defect (A4R-P1-001).
 */
class DeviceKeys : PairingFlow.DeviceKeysContract {

    override fun ensureDeviceKeyPair(): String {
        val ks = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        val existing = ks.getCertificate(ALIAS)
        if (existing == null) {
            val generator = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_EC, ANDROID_KEYSTORE)
            generator.initialize(
                KeyGenParameterSpec.Builder(
                    ALIAS,
                    KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY,
                )
                    .setDigests(KeyProperties.DIGEST_SHA256)
                    .setAlgorithmParameterSpec(ECGenParameterSpec("secp256r1"))
                    .build(),
            )
            generator.generateKeyPair()
        }
        return publicPem()
    }

    /** SubjectPublicKeyInfo PEM (B bound shape: "-----BEGIN PUBLIC KEY-----"). */
    fun publicPem(): String {
        val ks = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        val entry = ks.getEntry(ALIAS, null) as? KeyStore.PrivateKeyEntry
            ?: throw IllegalStateException("device key alias missing")
        val cert = entry.certificate
        val b64 = Base64.encodeToString(cert.encoded, Base64.NO_WRAP)
        return buildString {
            append("-----BEGIN PUBLIC KEY-----\n")
            b64.chunked(64).forEach { append(it).append('\n') }
            append("-----END PUBLIC KEY-----")
        }
    }

    /** SHA256withECDSA signature over the single-use challenge (DER, Base64). */
    override fun signChallenge(challenge: String): String {
        val ks = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        val entry = ks.getEntry(ALIAS, null) as? KeyStore.PrivateKeyEntry
            ?: throw IllegalStateException("device key alias missing")
        val signature = Signature.getInstance("SHA256withECDSA")
        signature.initSign(entry.privateKey)
        signature.update(challenge.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(signature.sign(), Base64.NO_WRAP)
    }

    /** Rotation statement signature (B bound API; used by future key rotation). */
    fun signRotationStatement(statement: String): String = signChallenge(statement)

    /**
     * Typed attestation metadata (B bound keys and values). The
     * hardware_backed_note is an HONEST static descriptor of the mechanism
     * (keystore-bound, non-exportable) — NOT a claim of hardware strength,
     * which is measured only on the Xiaomi physical gate.
     */
    override fun attestationMetadata(): Map<String, String> = mapOf(
        "keystore" to "AndroidKeyStore",
        "algorithm" to "EC",
        "curve" to "secp256r1",
        "digest" to "SHA-256",
        "signature_format" to "DER(SHA256withECDSA)",
        "hardware_backed_note" to "Keystore-bound; private key non-exportable",
    )

    /** JVM-side curve verification helper used by the contract fixtures. */
    fun decodePublicKeyPem(pem: String): ECPublicKey {
        val body = pem
            .removePrefix("-----BEGIN PUBLIC KEY-----")
            .removeSuffix("-----END PUBLIC KEY-----")
            .replace("\n", "")
            .trim()
        val der = Base64.decode(body, Base64.NO_WRAP)
        val spec = X509EncodedKeySpec(der)
        return KeyFactory.getInstance("EC").generatePublic(spec) as ECPublicKey
    }

    companion object {
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val ALIAS = "hermes_voice_console_device_key"
    }
}
