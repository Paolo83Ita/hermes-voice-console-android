package com.hermestower.voiceconsole.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.assertThrows
import org.junit.Test

/**
 * A4R NODE A2 - JVM suite for the byte-level framing truth ONLY.
 *
 * A4R-P1-002 discipline: the JVM suite never simulates the AndroidKeyStore
 * (that was the defect class that let the physical crash ship) - the keystore
 * path is covered by [TowerStoreInstrumentedTest] on a real device/emulator.
 * Here only pure byte logic is represented: framing, bounds, failure shapes.
 */
class BlobFramingTest {

    private val iv = ByteArray(12) { it.toByte() }
    private val ct = ByteArray(32) { (it * 7).toByte() }

    @Test
    fun `round trip preserves iv and ciphertext`() {
        val blob = BlobFraming.encode(iv, ct)
        assertEquals(1 + 12 + 32, blob.size)
        assertEquals(12, blob[0].toInt())
        val decoded = BlobFraming.decode(blob)
        assertEquals(iv.toList(), decoded?.first?.toList())
        assertEquals(ct.toList(), decoded?.second?.toList())
    }

    @Test
    fun `decode rejects non 12-byte iv length`() {
        val blob = BlobFraming.encode(iv, ct)
        blob[0] = 16
        assertNull(BlobFraming.decode(blob))
    }

    @Test
    fun `decode rejects blobs without a full authentication tag`() {
        val blob = BlobFraming.encode(iv, ct).copyOfRange(0, 1 + 12 + 10)
        assertNull(BlobFraming.decode(blob))
    }

    @Test
    fun `decode rejects null and tiny blobs`() {
        assertNull(BlobFraming.decode(null))
        assertNull(BlobFraming.decode(ByteArray(0)))
        assertNull(BlobFraming.decode(ByteArray(15)))
    }

    @Test
    fun `encode rejects empty ciphertext`() {
        assertThrows(IllegalArgumentException::class.java) { BlobFraming.encode(iv, ByteArray(0)) }
    }

    @Test
    fun `encode rejects oversized iv`() {
        assertThrows(IllegalArgumentException::class.java) {
            BlobFraming.encode(ByteArray(256), ct)
        }
    }

    @Test
    fun `gcm constants are the mission truth`() {
        assertEquals(128, BlobFraming.GCM_TAG_BITS)
        assertEquals(12, BlobFraming.GCM_IV_BYTES)
        assertTrue(BlobFraming.MAX_IV_LEN >= BlobFraming.GCM_IV_BYTES)
    }
}
