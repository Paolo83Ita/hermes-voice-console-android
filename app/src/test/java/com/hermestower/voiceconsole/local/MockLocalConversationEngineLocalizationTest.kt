package com.hermestower.voiceconsole.local

import com.hermestower.voiceconsole.locale.ResponseCategory
import com.hermestower.voiceconsole.locale.ResponseCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * GRAPH 4.10 CP-G4.10-002: the local conversation engine produces the SAME
 * intent in the REQUESTED locale — the P0-01 English-while-Italian defect class.
 */
class MockLocalConversationEngineLocalizationTest {

    private val engine = MockLocalConversationEngine()
    private val status = { "12:00, battery 80% (test fixture)" }

    @Test
    fun `italian greeting produces the localized greeting`() {
        val reply = engine.respond("ciao", status, "it")
        assertEquals("GREETING", reply.intent)
        assertEquals(
            ResponseCatalog.text(ResponseCategory.GREETING, "it"),
            reply.text,
        )
        assertTrue(reply.text.contains("console"))
        assertTrue(!reply.text.contains("Good day"))
    }

    @Test
    fun `italian identity produces the Owner-mandated Italian string`() {
        val reply = engine.respond("chi sei", status, "it")
        assertEquals("IDENTITY", reply.intent)
        assertEquals(
            "Sono la console vocale di Hermes Tower. Gestisco localmente le funzioni della console, mentre Hermes Tower rimane l’autorità per le missioni e le operazioni complesse.",
            reply.text,
        )
    }

    @Test
    fun `english input under italian locale still answers italian`() {
        val reply = engine.respond("who are you", status, "it")
        assertEquals("IDENTITY", reply.intent)
        assertTrue(reply.text.startsWith("Sono la console vocale"))
    }

    @Test
    fun `italian status embeds the truthful device status`() {
        val reply = engine.respond("qual è lo stato", status, "it")
        assertEquals("DEVICE_STATUS", reply.intent)
        assertEquals(
            "Stato console: ${status()}",
            reply.text,
        )
    }

    @Test
    fun `unknown input under italian locale answers italian and safe`() {
        val reply = engine.respond("compriamo azioni in borsa", status, "it")
        assertEquals("NOT_LOCAL", reply.intent)
        assertEquals(
            ResponseCatalog.text(ResponseCategory.UNKNOWN_SAFE, "it"),
            reply.text,
        )
    }

    @Test
    fun `legacy two-arg respond stays english for JVM tests`() {
        val reply = engine.respond("ciao", status)
        assertEquals(
            ResponseCatalog.text(ResponseCategory.GREETING, "en"),
            reply.text,
        )
    }

    @Test
    fun `all five locales produce distinct localized help`() {
        val answers = setOf("it", "en", "es", "fr", "de").map {
            engine.respond("help", status, it).text
        }.toSet()
        assertEquals(5, answers.size)
    }
}
