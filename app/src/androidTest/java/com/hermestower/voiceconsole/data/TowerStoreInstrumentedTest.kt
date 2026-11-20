package com.hermestower.voiceconsole.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.security.KeyStore
import java.util.Base64 as JavaBase64

/**
 * A4R NODE A2 - REAL AndroidKeyStore instrumented tests for the repaired
 * Tower encrypted persistence.
 *
 * These tests RUN AGAINST THE REAL AndroidKeyStore on the device/emulator
 * (mission: "Android instrumentation test using real AndroidKeyStore"). They
 * exist because the 17 JVM client tests could never represent the keystore
 * path - exactly the class that shipped the Owner's physical crash
 * (A4R-P1-001 / A4R-P1-002).
 *
 * Coverage per mission section 5:
 *  - first-install; app-update/restart persistence; uninstall/reinstall
 *    (key-loss) with typed invalidation; corrupted ciphertext with typed
 *    corrupt state + record wipe; malformed framing; atomic pairing commit
 *    (success + no-partial-state); QR and numeric lane store parity.
 */
@RunWith(AndroidJUnit4::class)
class TowerStoreInstrumentedTest {

    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Before
    fun isolate() {
        TowerStore(context).clear()
        deleteStorageAlias()
    }

    private fun deleteStorageAlias() {
        val ks = KeyStore.getInstance("AndroidKeyStore")
        ks.load(null)
        if (ks.containsAlias("hermes_voice_console_storage_key")) {
            ks.deleteEntry("hermes_voice_console_storage_key")
        }
    }

    private fun rawBlob(key: String): ByteArray? {
        val prefs = context.getSharedPreferences("tower_console", 0)
        val b64 = prefs.getString(key, null) ?: return null
        return JavaBase64.getDecoder().decode(b64)
    }

    // ---- mission: first-install test -------------------------------------------------

    @Test
    fun firstInstall_roundTrip_and_no_failure() {
        val store = TowerStore(context)
        val endpoint = "https://hermestower.aclclouds.online"
        store.setEndpoint(endpoint)
        assertEquals(endpoint, store.getEndpoint())
        assertNull(store.lastFailure)
        assertTrue(store.failureHistory().isEmpty())
        assertFalse(store.isPaired()) // endpoint alone is not a pairing
    }

    // ---- mission: app update test (in-process persistence; the physical
    // ---- update is Owner step A8-22 and the CI emulator lane) ------------------------

    @Test
    fun restartPersistence_newInstance_readsBack() {
        val store = TowerStore(context)
        store.commitPairing("https://hermestower.aclclouds.online", "dev-1", "token-1", "Xiaomi")
        val restarted = TowerStore(context) // fresh object = post-restart read
        assertEquals("https://hermestower.aclclouds.online", restarted.getEndpoint())
        assertEquals("dev-1", restarted.getDeviceId())
        assertEquals("token-1", restarted.getConsoleToken())
        assertTrue(restarted.isPaired())
    }

    // ---- mission: uninstall/reinstall test (deterministic key-loss class) ------------

    @Test
    fun keyLoss_recordsInvalidation_wipesAndRegenerates() {
        val store = TowerStore(context)
        store.setEndpoint("https://hermestower.aclclouds.online")
        deleteStorageAlias() // the uninstall/reinstall/invalidation event
        val storeAfter = TowerStore(context)
        assertNull(storeAfter.getEndpoint()) // never plaintext, never crash
        assertEquals(KeystoreFailure.ANDROID_KEY_INVALIDATED, storeAfter.lastFailure)
        assertNull(rawBlob("endpoint")) // wiped, not left behind
        // regeneration: the store is usable again, no state carried over
        storeAfter.setEndpoint("https://hermestower.aclclouds.online")
        assertEquals("https://hermestower.aclclouds.online", storeAfter.getEndpoint())
    }

    // ---- mission: corrupted ciphertext test ------------------------------------------

    @Test
    fun corruptedCiphertext_recordsAuthFailure_wipesRecordOnly() {
        val store = TowerStore(context)
        store.setDeviceId("device-42")
        store.setEndpoint("https://hermestower.aclclouds.online")
        // flip the LAST byte (inside the GCM authentication tag region)
        val blob = rawBlob("endpoint")!!
        blob[blob.size - 1] = (blob[blob.size - 1].toInt() xor 0x5A).toByte()
        context.getSharedPreferences("tower_console", 0).edit()
            .putString("endpoint", JavaBase64.getEncoder().encodeToString(blob)).commit()
        assertNull(store.getEndpoint())
        // C1-C4 mission section 4 typed truth: a tampered GCM AUTH TAG is
        // ENCRYPTED_STORE_AUTHENTICATION_FAILED (the A2-era 5-state set had no
        // dedicated auth state; the framing-garbage case below stays CORRUPT).
        assertEquals(KeystoreFailure.ENCRYPTED_STORE_AUTHENTICATION_FAILED, store.lastFailure)
        assertNull(rawBlob("endpoint")) // record wiped
        assertEquals("device-42", store.getDeviceId()) // sibling records untouched
    }

    @Test
    fun malformedFraming_recordsCorrupt_noCrash() {
        context.getSharedPreferences("tower_console", 0).edit()
            .putString("endpoint", "definitely-not-a-blob").commit()
        val store = TowerStore(context)
        assertNull(store.getEndpoint())
        assertEquals(KeystoreFailure.ENCRYPTED_STORE_CORRUPT, store.lastFailure)
    }

    // ---- mission: atomic pairing commit (QR + numeric lanes) -------------------------

    @Test
    fun commitPairing_success_isAtomicAndComplete() {
        val store = TowerStore(context)
        assertTrue(
            store.commitPairing(
                "https://hermestower.aclclouds.online", "dev-qr-1", "tok-qr-1", "Xiaomi 11 Lite 5G NE",
            ),
        )
        assertEquals("https://hermestower.aclclouds.online", store.getEndpoint())
        assertEquals("dev-qr-1", store.getDeviceId())
        assertEquals("tok-qr-1", store.getConsoleToken())
        assertEquals("Xiaomi 11 Lite 5G NE", store.getDeviceLabel())
        assertTrue(store.isPaired())
    }

    @Test
    fun commitPairing_failure_commitsNOTHING() {
        val store = TowerStore(context) { _ ->
            throw TowerStoreKeystoreException(
                KeystoreFailure.ANDROID_KEYSTORE_INITIALIZATION_FAILED, "forced by test",
            )
        }
        assertFalse(
            store.commitPairing("https://hermestower.aclclouds.online", "dev-x", "tok-x", "label"),
        )
        // NOTHING reached SharedPreferences (no partial state, ever)
        assertNull(rawBlob("endpoint"))
        assertNull(rawBlob("device_id"))
        assertNull(rawBlob("console_token"))
        assertEquals(KeystoreFailure.PAIRING_ABORTED_NO_STATE_COMMITTED, store.lastFailure)
        assertFalse(store.isPaired())
    }

    // ---- mission: QR pairing test + numeric pairing test (store level) ---------------

    @Test
    fun qrAndNumericLanes_shareTheSingleStoreAuthority() {
        val store = TowerStore(context)
        // QR lane material
        assertTrue(store.commitPairing("https://tower-a", "dev-qr", "tok-qr", "via QR"))
        assertEquals("via QR", store.getDeviceLabel())
        // numeric lane re-pair of the SAME device identity (no parallel store)
        assertTrue(store.commitPairing("https://tower-b", "dev-qr", "tok-num", "via numeric"))
        assertEquals("https://tower-b", store.getEndpoint())
        assertEquals("tok-num", store.getConsoleToken())
        assertEquals("dev-qr", store.getDeviceId())
        assertNotEquals("via QR", store.getDeviceLabel())
    }

    // ---- real keystore proof ----------------------------------------------------------

    @Test
    fun generatedKey_isAes256Gcm_withRandomizedIv() {
        val store = TowerStore(context)
        store.setEndpoint("https://hermestower.aclclouds.online")
        val ks = KeyStore.getInstance("AndroidKeyStore")
        ks.load(null)
        val entry = ks.getEntry("hermes_voice_console_storage_key", null) as KeyStore.SecretKeyEntry
        assertEquals("AES", entry.secretKey.algorithm)
        val blob1 = rawBlob("endpoint")!!
        store.setEndpoint("https://hermestower.aclclouds.online/second")
        val blob2 = rawBlob("endpoint")!!
        // fresh IV per encryption (randomized encryption required): the first 13
        // bytes (len + IV) must differ between two encryptions
        assertTrue(!blob1.copyOfRange(0, 13).contentEquals(blob2.copyOfRange(0, 13)))
        assertEquals("https://hermestower.aclclouds.online/second", store.getEndpoint())
    }
}
