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
}
