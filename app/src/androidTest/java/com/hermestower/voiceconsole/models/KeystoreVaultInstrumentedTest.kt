package com.hermestower.voiceconsole.models

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.hermestower.voiceconsole.tower.KeystoreVault
import com.hermestower.voiceconsole.tower.PrivateFileVaultStore
import com.hermestower.voiceconsole.tower.VaultAvailability
import com.hermestower.voiceconsole.tower.VaultKeys
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * GATE-04: secure-storage tests on emulator-supported APIs. The vault must
 * round-trip secrets through AndroidKeyStore-backed AES-256-GCM, keep plaintext
 * out of the backing file, and survive vault reopen.
 */
@RunWith(AndroidJUnit4::class)
class KeystoreVaultInstrumentedTest {

    private fun vaultFile(): File {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        return File(context.filesDir, "test_tower_vault.bin")
    }

    @Test
    fun vaultRoundtripsSecretsThroughKeystore() {
        vaultFile().delete()
        val vault = KeystoreVault(PrivateFileVaultStore(vaultFile()))
        val key = VaultKeys.secretKeyFor("keystore-test")
        vault.put(key, "round-trip-secret-42".toCharArray())
        assertEquals("round-trip-secret-42", String(vault.get(key)!!))
        assertTrue(vault.contains(key))
        assertTrue(vault.remove(key))
        assertNull(vault.get(key))
    }

    @Test
    fun backingFileNeverContainsPlaintext() {
        vaultFile().delete()
        val vault = KeystoreVault(PrivateFileVaultStore(vaultFile()))
        val secretValue = "PLAINTEXT-MUST-NOT-LEAK"
        vault.put(VaultKeys.secretKeyFor("leak-test"), secretValue.toCharArray())
        val raw = vaultFile().readText()
        assertFalse("plaintext found in vault backing file", raw.contains(secretValue))
        assertTrue("ciphertext record missing", raw.contains("leak-test"))
    }

    @Test
    fun vaultSurvivesReopen() {
        vaultFile().delete()
        val key = VaultKeys.secretKeyFor("reopen-test")
        KeystoreVault(PrivateFileVaultStore(vaultFile())).put(key, "persist-me".toCharArray())
        val reopened = KeystoreVault(PrivateFileVaultStore(vaultFile()))
        assertEquals("persist-me", String(reopened.get(key)!!))
    }

    @Test
    fun availabilityIsReportedTruthfully() {
        val vault = KeystoreVault(PrivateFileVaultStore(vaultFile()))
        val availability = vault.available()
        assertTrue(
            "unexpected availability: $availability",
            availability == VaultAvailability.KEYSTORE_HARDWARE_BACKED || availability == VaultAvailability.KEYSTORE_SOFTWARE,
        )
        assertNotNull(vault.available().displayLabel)
    }
}
