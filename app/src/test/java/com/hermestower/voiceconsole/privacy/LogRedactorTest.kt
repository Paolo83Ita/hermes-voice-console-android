package com.hermestower.voiceconsole.privacy

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LogRedactorTest {

    @Test
    fun `credential assignments are masked`() {
        assertEquals(
            "token: [REDACTED]",
            LogRedactor.redact("token: sk-live-abc123"),
        )
        assertEquals(
            "AUTH Authorization: [REDACTED] header kept",
            LogRedactor.redact("AUTH Authorization: Bearer eyabc.def.ghi header kept"),
        )
        assertEquals(
            "password=[REDACTED]",
            LogRedactor.redact("password=hunter2"),
        )
    }

    @Test
    fun `long hex and base64 runs are masked`() {
        val redacted = LogRedactor.redact("device key aabbccddeeff00112233445566778899aabb end")
        assertTrue(redacted.contains("[REDACTED_HEX]"))
        val b64 = LogRedactor.redact("payload AbCdef123456AbCdef123456AbCdef1234567890 done")
        assertTrue(b64.contains("[REDACTED_B64]"))
    }

    @Test
    fun `normal diagnostics text survives untouched`() {
        val line = "2026-10-30T12:00:00Z [ROUTE] HANDLE_LOCAL intent=GREETING reason=whitelisted"
        assertEquals(line, LogRedactor.redact(line))
    }

    @Test
    fun `no transcript content policy is documented in event usage`() {
        // The driver logs shapes, not speech; this test pins the redaction layer it relies on.
        val leaked = LogRedactor.redact("turn text containing token: super-secret-value inside")
        assertFalse(leaked.contains("super-secret-value"))
        assertTrue(leaked.contains("inside"))
    }
}
