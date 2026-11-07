package com.hermestower.voiceconsole.locale

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * GRAPH 4.10 CP-G4.10-002: the localized response catalog (§5).
 * Five-locale parity, the Owner-mandated Italian identity and simulation
 * strings verbatim, and no blank entry anywhere.
 */
class ResponseCatalogTest {

    @Test
    fun `every category exists in all five locales`() {
        for (locale in ResponseCatalog.LOCALES) {
            for (category in ResponseCatalog.allCategories()) {
                // Extra args are ignored by the Formatter; %s categories consume "x".
                val text = ResponseCatalog.text(category, locale, "x", "y")
                assertTrue(
                    "blank entry: ${category.name} in $locale",
                    text.isNotBlank(),
                )
                assertTrue(
                    "placeholder leaked: ${category.name} in $locale",
                    !text.contains("%s"),
                )
            }
        }
    }

    @Test
    fun `italian identity response is the Owner-mandated string`() {
        assertEquals(
            "Sono la console vocale di Hermes Tower. Gestisco localmente le funzioni della console, mentre Hermes Tower rimane l’autorità per le missioni e le operazioni complesse.",
            ResponseCatalog.text(ResponseCategory.IDENTITY, "it"),
        )
    }

    @Test
    fun `italian simulation explanation preserves SIMULATED truth verbatim`() {
        val text = ResponseCatalog.text(ResponseCategory.SIMULATED_ACK, "it", "prova")
        assertTrue(text.contains("Questa è una risposta simulata e deterministica."))
        assertTrue(text.contains("Non è stata contattata alcuna Tower reale."))
    }

    @Test
    fun `every locale states the simulated truth`() {
        val markers = mapOf(
            "it" to "simulata e deterministica",
            "en" to "simulated, deterministic",
            "es" to "simulada y determinista",
            "fr" to "simulée et déterministe",
            "de" to "simulierte, deterministische",
        )
        for ((locale, marker) in markers) {
            assertTrue(
                "SIMULATED truth missing in $locale",
                ResponseCatalog.text(ResponseCategory.SIMULATED_ACK, locale, "x").contains(marker),
            )
        }
    }

    @Test
    fun `format arguments substitute into catalog entries`() {
        assertEquals(
            "Stato console: 12:00, test",
            ResponseCatalog.text(ResponseCategory.CONSOLE_STATUS, "it", "12:00, test"),
        )
        assertEquals(
            "Ricevuta rcp-mock-0001",
            ResponseCatalog.text(ResponseCategory.TOWER_RECEIPT, "it", "rcp-mock-0001"),
        )
    }

    @Test
    fun `unsupported locale falls back to English honestly`() {
        assertEquals(
            ResponseCatalog.text(ResponseCategory.GREETING, "en"),
            ResponseCatalog.text(ResponseCategory.GREETING, "ja"),
        )
    }

    @Test
    fun `catalog is complete for every locale`() {
        for (locale in ResponseCatalog.LOCALES) {
            assertTrue("catalog incomplete for $locale", ResponseCatalog.isComplete(locale))
        }
    }

    @Test
    fun `decline label is localized`() {
        assertEquals("Non posso farlo.", ResponseCatalog.text(ResponseCategory.DECLINE, "it"))
        assertEquals("I cannot do that.", ResponseCatalog.text(ResponseCategory.DECLINE, "en"))
    }

    /**
     * GRAPH 4.11 Node 3 (Owner: "Nessuna risposta locale inglese") — the
     * catalog-level pin of the P0-01 repair: with the Italian locale active,
     * EVERY category must produce the Italian text, never the English one.
     * The single locale-neutral entry (TOWER_PROGRESS renders "Tower: %s" —
     * the substituted progress text is produced localized upstream) is typed
     * explicitly instead of silently skipped.
     */
    @Test
    fun `no category answers English under the Italian locale`() {
        val localeNeutral = setOf(ResponseCategory.TOWER_PROGRESS)
        for (category in ResponseCategory.entries) {
            // placeholder args: format entries (%s) format honestly; extra
            // arguments are ignored by String.format
            val italian = ResponseCatalog.text(category, "it", "x", "x", "x")
            val english = ResponseCatalog.text(category, "en", "x", "x", "x")
            assertTrue("category $category: the Italian text must not be blank", italian.isNotBlank())
            if (category in localeNeutral) continue
            assertTrue(
                "category $category: Italian must differ from English (P0-01 defect class)",
                italian != english,
            )
        }
        // And the Italian text really is the Italian text: the identity
        // response stays the Owner-mandated string under the resolver path.
        assertEquals(
            "Sono la console vocale di Hermes Tower. Gestisco localmente le funzioni della console, mentre Hermes Tower rimane l’autorità per le missioni e le operazioni complesse.",
            ResponseCatalog.text(ResponseCategory.IDENTITY, "it"),
        )
    }
}
