package com.hermestower.voiceconsole.pairing

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.security.KeyStore
import java.security.Signature

/**
 * GRAPH A4R C1-C4 — REAL AndroidKeyStore instrumented tests for the EC-P256
 * device identity (mission section 5: instrumentation uses the REAL keystore;
 * the JVM suite never simulates it).
 *
 * These tests RUN ON THE EMULATOR in CI (SOFTWARE keystore) — the recorded
 * gate is SOFTWARE_ANDROID_KEYSTORE_TESTED; the HARDWARE_BACKED claim stays
 * reserved for the Xiaomi physical gate (mission section 5 wording).
 */
@RunWith(AndroidJUnit4::class)
class DeviceKeysInstrumentedTest {

    private lateinit var keys: DeviceKeys

    @Before
    fun isolate() {
        keys = DeviceKeys()
        val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        if (ks.containsAlias(DeviceKeys.ALIAS)) {
            ks.deleteEntry(DeviceKeys.ALIAS)
        }
    }

    @Test
    fun ensure_creates_a_repeatable_secp256r1_key() {
        val pem1 = keys.ensureDeviceKeyPair()
        val pem2 = keys.ensureDeviceKeyPair() // idempotent: the alias is reused
        assertEquals(pem1, pem2)
        assertTrue(pem1.startsWith("-----BEGIN PUBLIC KEY-----"))
        val pub = keys.decodePublicKeyPem(pem1)
        assertEquals("EC", pub.algorithm)
        assertEquals(256, pub.params.order.bitLength())
    }

    @Test
    fun signChallenge_verifies_with_the_public_key() {
        val pem = keys.ensureDeviceKeyPair()
        val pub = keys.decodePublicKeyPem(pem)
        val challenge = "challenge-under-test"
        val sig = keys.signChallenge(challenge)
        val verifier = Signature.getInstance("SHA256withECDSA")
        verifier.initVerify(pub)
        verifier.update(challenge.toByteArray(Charsets.UTF_8))
        assertTrue(verifier.verify(java.util.Base64.getDecoder().decode(sig)))
    }

    @Test
    fun attestationMetadata_carries_the_bound_contract_keys() {
        val meta = keys.attestationMetadata()
        assertEquals("AndroidKeyStore", meta["keystore"])
        assertEquals("secp256r1", meta["curve"])
        assertEquals("DER(SHA256withECDSA)", meta["signature_format"])
        assertNotNull(meta["hardware_backed_note"])
    }
}
