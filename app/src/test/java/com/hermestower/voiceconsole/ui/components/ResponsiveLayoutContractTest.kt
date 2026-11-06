package com.hermestower.voiceconsole.ui.components

import com.hermestower.voiceconsole.locale.StateLabels
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * GRAPH 4.10 CP-G4.10-008/009: the §19 width classes and the §23 raw-enum
 * localization table (Owner finding P0-09 — "Invalid" visible — is dead).
 */
class ResponsiveLayoutContractTest {

    @Test
    fun `width classes follow the §19 dp boundaries`() {
        assertEquals(ConsoleWindowWidthClass.COMPACT, classifyWidth(360f))
        assertEquals(ConsoleWindowWidthClass.COMPACT, classifyWidth(379.9f))
        assertEquals(ConsoleWindowWidthClass.MEDIUM, classifyWidth(380f))
        assertEquals(ConsoleWindowWidthClass.MEDIUM, classifyWidth(411f))
        assertEquals(ConsoleWindowWidthClass.MEDIUM, classifyWidth(420f))
        assertEquals(ConsoleWindowWidthClass.LARGE, classifyWidth(420.5f))
        assertEquals(ConsoleWindowWidthClass.LARGE, classifyWidth(480f))
    }

    @Test
    fun `the Xiaomi 11 Lite 5G NE classifies as COMPACT or MEDIUM by measured width`() {
        // Measured available width after insets + padding is ~360-393 dp on the
        // 11 Lite 5G NE — COMPACT or MEDIUM, NEVER by physical pixels.
        val c = classifyWidth(360f)
        assertTrue(c == ConsoleWindowWidthClass.COMPACT || c == ConsoleWindowWidthClass.MEDIUM)
    }

    @Test
    fun `actions per row follow the §22 rules`() {
        assertEquals(1, ConsoleWindowWidthClass.COMPACT.maxActionsPerRow())
        assertEquals(2, ConsoleWindowWidthClass.MEDIUM.maxActionsPerRow())
        assertEquals(3, ConsoleWindowWidthClass.LARGE.maxActionsPerRow())
    }

    // ---- §23 RAW ENUM LOCALIZATION ----

    @Test
    fun `the Owner-mandated §23 Italian table is verbatim`() {
        assertEquals("Non valida", StateLabels.label("INVALID", "it"))
        assertEquals("Validata", StateLabels.label("VALIDATED", "it"))
        assertEquals("Connessa", StateLabels.label("CONNECTED", "it"))
        assertEquals("Disconnessa", StateLabels.label("DISCONNECTED", "it"))
        assertEquals("Degradata", StateLabels.label("DEGRADED", "it"))
        assertEquals("Autenticazione non riuscita", StateLabels.label("AUTHENTICATION_FAILED", "it"))
        assertEquals("Protocollo incompatibile", StateLabels.label("PROTOCOL_MISMATCH", "it"))
        assertEquals("Tempo scaduto", StateLabels.label("TIMEOUT", "it"))
        assertEquals("Associazione richiesta", StateLabels.label("PAIRING_REQUIRED", "it"))
        assertEquals("Accesso revocato", StateLabels.label("REVOKED", "it"))
    }

    @Test
    fun `all five locales carry every label (parity)`() {
        for (name in listOf(
            "INVALID", "VALIDATED", "CONNECTED", "DISCONNECTED", "DEGRADED",
            "AUTHENTICATION_FAILED", "PROTOCOL_MISMATCH", "TIMEOUT",
            "PAIRING_REQUIRED", "REVOKED", "VALIDATION_PASSED", "CONNECTION_PASSED",
            "MOCK_SIMULATOR", "TAILSCALE_PRIVATE",
        )) {
            for (locale in listOf("it", "en", "es", "fr", "de")) {
                val label = StateLabels.label(name, locale)
                assertTrue("empty label $name/$locale", label.isNotBlank())
                assertTrue(
                    "raw enum leaked in ordinary UI: $name/$locale",
                    label != name,
                )
            }
        }
    }

    @Test
    fun `unknown identifiers fall back to the raw name (diagnostics only)`() {
        assertEquals("SOME_NEW_STATE", StateLabels.label("SOME_NEW_STATE", "it"))
    }
}
