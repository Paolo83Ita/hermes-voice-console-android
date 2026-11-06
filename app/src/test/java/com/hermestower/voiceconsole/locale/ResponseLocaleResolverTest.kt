package com.hermestower.voiceconsole.locale

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * GRAPH 4.10 CP-G4.10-002: the ONE response-locale authority (§4).
 * The five-level precedence chain, normalization and honest fallback reasons.
 */
class ResponseLocaleResolverTest {

    @Test
    fun `explicit conversation language wins over everything`() {
        val r = ResponseLocaleResolver(appLanguageTag = { "it" }, systemLanguageTag = { "de" })
        val res = r.resolve(explicitConversationLanguage = "fr-CA")
        assertEquals("fr", res.tag)
        assertEquals(ResponseLocaleResolver.Source.EXPLICIT_CONVERSATION, res.source)
        assertNull(res.fallbackReason)
        assertNull(res.confidence)
    }

    @Test
    fun `application language is level two`() {
        val r = ResponseLocaleResolver(appLanguageTag = { "it" }, systemLanguageTag = { "de" })
        val res = r.resolve()
        assertEquals("it", res.tag)
        assertEquals(ResponseLocaleResolver.Source.APP_LANGUAGE, res.source)
        assertNull(res.fallbackReason)
    }

    @Test
    fun `unsupported application language falls to input hint with confidence`() {
        val r = ResponseLocaleResolver(appLanguageTag = { "ja" }, systemLanguageTag = { "ja" })
        val res = r.resolve(inputLanguageHint = "it-IT", hintConfidence = 0.87f)
        assertEquals("it", res.tag)
        assertEquals(ResponseLocaleResolver.Source.INPUT_HINT, res.source)
        assertNotNull(res.fallbackReason)
        assertEquals(0.87f, res.confidence)
    }

    @Test
    fun `system locale is level four`() {
        val r = ResponseLocaleResolver(appLanguageTag = { "ja" }, systemLanguageTag = { "de-DE" })
        val res = r.resolve()
        assertEquals("de", res.tag)
        assertEquals(ResponseLocaleResolver.Source.SYSTEM_LOCALE, res.source)
    }

    @Test
    fun `english fallback records the full skip chain`() {
        val r = ResponseLocaleResolver(appLanguageTag = { "ja" }, systemLanguageTag = { "ko" })
        val res = r.resolve()
        assertEquals("en", res.tag)
        assertEquals(ResponseLocaleResolver.Source.FALLBACK, res.source)
        assertTrue(res.fallbackReason!!.contains("ja"))
        assertTrue(res.fallbackReason!!.contains("ko"))
    }

    @Test
    fun `unsupported explicit language is skipped honestly with its reason`() {
        val r = ResponseLocaleResolver(appLanguageTag = { "it" }, systemLanguageTag = { "it" })
        val res = r.resolve(explicitConversationLanguage = "ru")
        assertEquals("it", res.tag)
        assertEquals(ResponseLocaleResolver.Source.APP_LANGUAGE, res.source)
        assertTrue(res.fallbackReason!!.contains("explicit conversation language 'ru'"))
    }

    @Test
    fun `normalize reduces BCP-47 tags to base language`() {
        assertEquals("it", ResponseLocaleResolver.normalize("it-IT"))
        assertEquals("de", ResponseLocaleResolver.normalize("DE"))
        assertEquals("en", ResponseLocaleResolver.normalize("en-US"))
        assertNull(ResponseLocaleResolver.normalize(null))
        assertNull(ResponseLocaleResolver.normalize("  "))
    }

    @Test
    fun `null hint falls through to application language`() {
        val r = ResponseLocaleResolver(appLanguageTag = { "es" }, systemLanguageTag = { "es" })
        val res = r.resolve(inputLanguageHint = null)
        assertEquals("es", res.tag)
        assertEquals(ResponseLocaleResolver.Source.APP_LANGUAGE, res.source)
    }
}
