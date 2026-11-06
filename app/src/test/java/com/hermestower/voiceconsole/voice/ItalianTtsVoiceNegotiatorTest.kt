package com.hermestower.voiceconsole.voice

import com.hermestower.voiceconsole.voice.ItalianTtsVoiceNegotiator.ItalianTtsVoiceStatus
import com.hermestower.voiceconsole.voice.ItalianTtsVoiceNegotiator.TtsLanguageResultCode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

/**
 * OWNER PHYSICAL FEEDBACK 3 (2026-11-06, POCO X6 PRO): the Italian TTS voice
 * negotiation must be typed, read-back-verified and silent-fallback-free.
 * Pure JVM tests — the android engine maps platform objects into these types.
 */
class ItalianTtsVoiceNegotiatorTest {

    private val offlineItaly = ItalianTtsVoiceNegotiator.TtsVoiceCandidate(
        name = "it-it-x-ita-local", localeTag = "it-IT", requiresNetwork = false,
    )
    private val networkedItaly = ItalianTtsVoiceNegotiator.TtsVoiceCandidate(
        name = "it-it-x-ita-network", localeTag = "it-IT", requiresNetwork = true,
    )
    private val offlineOther = ItalianTtsVoiceNegotiator.TtsVoiceCandidate(
        name = "en-us-x-ene-local", localeTag = "en-US", requiresNetwork = false,
    )
    private val offlineItalianGeneric = ItalianTtsVoiceNegotiator.TtsVoiceCandidate(
        name = "it-x-ita-local", localeTag = "it", requiresNetwork = false,
    )

    // ---- request locale ----

    @Test
    fun `request locale maps app language to full country locale`() {
        assertEquals(Locale("it", "IT"), ItalianTtsVoiceNegotiator.requestLocaleFor("it"))
        assertEquals(Locale("en", "US"), ItalianTtsVoiceNegotiator.requestLocaleFor("en"))
        assertEquals(Locale("de", "DE"), ItalianTtsVoiceNegotiator.requestLocaleFor("de"))
    }

    @Test
    fun `request locale survives unknown language without country`() {
        assertEquals(Locale("sv"), ItalianTtsVoiceNegotiator.requestLocaleFor("sv"))
    }

    // ---- voice selection preference ----

    @Test
    fun `offline it-IT preferred over networked and other languages`() {
        val picked = ItalianTtsVoiceNegotiator.selectVoice(listOf(offlineOther, networkedItaly, offlineItaly))
        assertEquals("it-it-x-ita-local", picked?.name)
    }

    @Test
    fun `networked it-IT used when no offline Italian exists`() {
        val picked = ItalianTtsVoiceNegotiator.selectVoice(listOf(offlineOther, networkedItaly))
        assertEquals("it-it-x-ita-network", picked?.name)
    }

    @Test
    fun `bare it locale accepted as last Italian resort`() {
        val picked = ItalianTtsVoiceNegotiator.selectVoice(listOf(offlineOther, offlineItalianGeneric))
        assertEquals("it-x-ita-local", picked?.name)
    }

    @Test
    fun `no Italian voice exists yields null plan`() {
        assertNull(ItalianTtsVoiceNegotiator.selectVoice(listOf(offlineOther)))
        assertNull(ItalianTtsVoiceNegotiator.selectVoice(emptyList()))
    }

    // ---- classification: the five typed states ----

    @Test
    fun `italian active when active voice locale is italian after successful set`() {
        val rb = ItalianTtsVoiceNegotiator.classify(
            requestedLocaleTag = "it-IT",
            setLanguageResult = TtsLanguageResultCode.LANG_COUNTRY_AVAILABLE,
            selectedVoiceName = "it-it-x-ita-local",
            activeVoiceName = "it-it-x-ita-local",
            activeVoiceLocaleTag = "it-IT",
            activeLanguageTag = "it-IT",
            engineName = "com.google.android.tts",
            availableItalianVoiceNames = listOf("it-it-x-ita-local"),
        )
        assertEquals(ItalianTtsVoiceStatus.ITALIAN_VOICE_ACTIVE, rb.status)
        assertTrue(rb.isItalianAudible)
    }

    @Test
    fun `italian active can come from the language read-back when voice locale is absent`() {
        val rb = ItalianTtsVoiceNegotiator.classify(
            requestedLocaleTag = "it-IT",
            setLanguageResult = TtsLanguageResultCode.LANG_AVAILABLE,
            selectedVoiceName = null,
            activeVoiceName = null,
            activeVoiceLocaleTag = null,
            activeLanguageTag = "it-IT",
            engineName = null,
            availableItalianVoiceNames = emptyList(),
        )
        assertEquals(ItalianTtsVoiceStatus.ITALIAN_VOICE_ACTIVE, rb.status)
    }

    @Test
    fun `missing data classified when setLanguage reports LANG_MISSING_DATA`() {
        val rb = ItalianTtsVoiceNegotiator.classify(
            requestedLocaleTag = "it-IT",
            setLanguageResult = TtsLanguageResultCode.LANG_MISSING_DATA,
            selectedVoiceName = null,
            activeVoiceName = "en-us-x-ene-local",
            activeVoiceLocaleTag = "en-US",
            activeLanguageTag = "en-US",
            engineName = "com.google.android.tts",
            availableItalianVoiceNames = emptyList(),
        )
        assertEquals(ItalianTtsVoiceStatus.ITALIAN_VOICE_MISSING_DATA, rb.status)
        assertTrue(rb.detail!!.isNotEmpty())
    }

    @Test
    fun `not supported classified when setLanguage reports LANG_NOT_SUPPORTED`() {
        val rb = ItalianTtsVoiceNegotiator.classify(
            requestedLocaleTag = "it-IT",
            setLanguageResult = TtsLanguageResultCode.LANG_NOT_SUPPORTED,
            selectedVoiceName = null,
            activeVoiceName = "en-us-x-ene-local",
            activeVoiceLocaleTag = "en-US",
            activeLanguageTag = "en-US",
            engineName = null,
            availableItalianVoiceNames = emptyList(),
        )
        assertEquals(ItalianTtsVoiceStatus.ITALIAN_VOICE_NOT_SUPPORTED, rb.status)
    }

    @Test
    fun `silent fallback classified when language accepted but active voice is English`() {
        // THE OWNER-OBSERVED DEFECT CLASS: the request "succeeds" (or is even
        // ignored) yet the audible voice stays the engine default (en-US).
        val rb = ItalianTtsVoiceNegotiator.classify(
            requestedLocaleTag = "it-IT",
            setLanguageResult = TtsLanguageResultCode.LANG_AVAILABLE,
            selectedVoiceName = null,
            activeVoiceName = "en-us-x-ene-local",
            activeVoiceLocaleTag = "en-US",
            activeLanguageTag = "en-US",
            engineName = "com.google.android.tts",
            availableItalianVoiceNames = emptyList(),
        )
        assertEquals(ItalianTtsVoiceStatus.FALLBACK_VOICE_TRIGGERED, rb.status)
    }

    @Test
    fun `describe carries status and active voice truth`() {
        val rb = ItalianTtsVoiceNegotiator.classify(
            requestedLocaleTag = "it-IT",
            setLanguageResult = TtsLanguageResultCode.LANG_COUNTRY_AVAILABLE,
            selectedVoiceName = "v1",
            activeVoiceName = "v1",
            activeVoiceLocaleTag = "it-IT",
            activeLanguageTag = "it-IT",
            engineName = "com.google.android.tts",
            availableItalianVoiceNames = listOf("v1"),
        )
        val d = ItalianTtsVoiceNegotiator.describe(rb)
        assertTrue(d.contains("ITALIAN_VOICE_ACTIVE"))
        assertTrue(d.contains("it-IT"))
        assertTrue(d.contains("com.google.android.tts"))
    }

    // ---- result code mapping ----

    @Test
    fun `setLanguage raw codes map to typed results`() {
        assertEquals(TtsLanguageResultCode.LANG_AVAILABLE, TtsLanguageResultCode.fromRaw(0))
        assertEquals(TtsLanguageResultCode.LANG_COUNTRY_AVAILABLE, TtsLanguageResultCode.fromRaw(1))
        assertEquals(TtsLanguageResultCode.LANG_COUNTRY_VAR_AVAILABLE, TtsLanguageResultCode.fromRaw(2))
        assertEquals(TtsLanguageResultCode.LANG_MISSING_DATA, TtsLanguageResultCode.fromRaw(-1))
        assertEquals(TtsLanguageResultCode.LANG_NOT_SUPPORTED, TtsLanguageResultCode.fromRaw(-2))
        assertEquals(TtsLanguageResultCode.UNKNOWN, TtsLanguageResultCode.fromRaw(99))
    }

    // ---- TtsOutput carries the readback (seam surface) ----

    @Test
    fun `TtsOutput default readback is null and defaulted usages stay compatible`() {
        val out = com.hermestower.voiceconsole.voice.TtsOutput(com.hermestower.voiceconsole.voice.TtsState.READY)
        assertNull(out.voiceReadback)
        assertNull(out.spokenText)
        assertNull(out.error)
        // OWNER PHYSICAL RETEST §4: the fail-closed block surface is typed and
        // null in the normal path.
        assertNull(out.speakBlocked)
    }

    // ---- OWNER PHYSICAL RETEST: quality-aware selection + inventory ----

    @Test
    fun `selection prefers the highest quality within a tier`() {
        val lowQuality = ItalianTtsVoiceNegotiator.TtsVoiceCandidate(
            name = "it-a-low", localeTag = "it-IT", requiresNetwork = false, quality = 300,
        )
        val highQuality = ItalianTtsVoiceNegotiator.TtsVoiceCandidate(
            name = "it-b-high", localeTag = "it-IT", requiresNetwork = false, quality = 500,
        )
        assertEquals("it-b-high", ItalianTtsVoiceNegotiator.selectVoice(listOf(lowQuality, highQuality))?.name)
    }

    @Test
    fun `quality never overrides the offline preference`() {
        val networkedHigh = ItalianTtsVoiceNegotiator.TtsVoiceCandidate(
            name = "it-net-high", localeTag = "it-IT", requiresNetwork = true, quality = 500,
        )
        val offlineLow = ItalianTtsVoiceNegotiator.TtsVoiceCandidate(
            name = "it-off-low", localeTag = "it-IT", requiresNetwork = false, quality = 300,
        )
        assertEquals("it-off-low", ItalianTtsVoiceNegotiator.selectVoice(listOf(networkedHigh, offlineLow))?.name)
    }

    @Test
    fun `inventory counts the italian offline and networked voices`() {
        val inv = ItalianTtsVoiceNegotiator.inventory(
            engineName = "com.google.android.tts",
            candidates = listOf(
                ItalianTtsVoiceNegotiator.TtsVoiceCandidate("en-a", "en-US", false),
                offlineItaly,
                networkedItaly,
            ),
            selectedVoiceName = offlineItaly.name,
            selectedVoiceLocaleTag = "it-IT",
        )
        assertEquals("com.google.android.tts", inv.engineName)
        assertEquals(3, inv.totalVoices)
        assertEquals(2, inv.italianVoices)
        assertEquals(1, inv.offlineItalianVoices)
        assertEquals(1, inv.networkRequiredItalianVoices)
        assertEquals("it-it-x-ita-local", inv.selectedVoiceName)
    }

    @Test
    fun `the owner-observed english-fallback classification stays typed`() {
        // setLanguage accepted, active voice English → FALLBACK_VOICE_TRIGGERED.
        val rb = ItalianTtsVoiceNegotiator.classify(
            requestedLocaleTag = "it-IT",
            setLanguageResult = TtsLanguageResultCode.LANG_COUNTRY_AVAILABLE,
            selectedVoiceName = null,
            activeVoiceName = "en-us-x-ene-local",
            activeVoiceLocaleTag = "en-US",
            activeLanguageTag = "en-US",
            engineName = "com.google.android.tts",
            availableItalianVoiceNames = emptyList(),
        )
        assertEquals(ItalianTtsVoiceStatus.FALLBACK_VOICE_TRIGGERED, rb.status)
        assertTrue(!rb.isItalianAudible)
        assertTrue(rb.status.requiresUserAction)
    }
    // ---- GRAPH 4.9 CP-G4.9-002 hardening: the remaining briefing scenarios ----

    @Test
    fun `selected voice disappearing after selection classifies fallback and blocks`() {
        // The engine applied setVoice(offline it-IT) successfully, but by the
        // post-apply read-back the voice is GONE (voice pack uninstalled,
        // engine restarted) and the ACTIVE voice drifted back to English.
        val rb = ItalianTtsVoiceNegotiator.classify(
            requestedLocaleTag = "it-IT",
            setLanguageResult = ItalianTtsVoiceNegotiator.TtsLanguageResultCode.LANG_COUNTRY_VAR_AVAILABLE,
            selectedVoiceName = "it-it-x-ital-local", // was selected…
            activeVoiceName = "en-us-x-ene-local",    // …but is no longer the active voice
            activeVoiceLocaleTag = "en-US",
            activeLanguageTag = "en-US",
            engineName = "com.google.android.tts",
            availableItalianVoiceNames = emptyList(), // the Italian voice disappeared entirely
        )
        assertEquals(ItalianTtsVoiceStatus.FALLBACK_VOICE_TRIGGERED, rb.status)
        assertEquals(false, rb.isItalianAudible)
        // TtsSpeakGate: the drifted read-back must be REFUSED (fail-closed).
        val verdict = TtsSpeakGate.decide(
            effectiveLanguageTag = "it",
            readback = rb,
        )
        assertTrue(verdict is TtsSpeakGate.Verdict.Block)
        assertEquals(
            TtsSpeakGate.BlockReason.ITALIAN_SYSTEM_TTS_READBACK_MISMATCH,
            (verdict as TtsSpeakGate.Verdict.Block).reason,
        )
    }

    @Test
    fun `engine package change between utterances is decided by voice language not engine identity`() {
        // The user switched the system TTS engine (Settings → Google → another
        // engine). The gate must decide on the ACTIVE VOICE's language; a new
        // engine that genuinely activates an Italian voice stays allowed.
        val rb = ItalianTtsVoiceNegotiator.classify(
            requestedLocaleTag = "it-IT",
            setLanguageResult = ItalianTtsVoiceNegotiator.TtsLanguageResultCode.LANG_COUNTRY_VAR_AVAILABLE,
            selectedVoiceName = "pico-it-it",
            activeVoiceName = "pico-it-it",
            activeVoiceLocaleTag = "it-IT",
            activeLanguageTag = "it-IT",
            engineName = "com.svox.pico", // DIFFERENT engine package than before
            availableItalianVoiceNames = listOf("pico-it-it"),
        )
        assertEquals(ItalianTtsVoiceStatus.ITALIAN_VOICE_ACTIVE, rb.status)
        val verdict = TtsSpeakGate.decide(effectiveLanguageTag = "it", readback = rb)
        assertTrue(verdict is TtsSpeakGate.Verdict.Allow)
        // And the read-back record itself carries the new engine name verbatim.
        assertEquals("com.svox.pico", rb.engineName)
    }

    @Test
    fun `app language switches without process restart re-decide the gate per utterance`() {
        // The engine re-negotiates before EVERY utterance (OWNER FEEDBACK 3),
        // so a language switch (it → de → it) must flip the verdict WITHOUT a
        // process restart: the effective-language tag drives the decision.
        val italianReadback = ItalianTtsVoiceNegotiator.classify(
            requestedLocaleTag = "it-IT",
            setLanguageResult = ItalianTtsVoiceNegotiator.TtsLanguageResultCode.LANG_COUNTRY_VAR_AVAILABLE,
            selectedVoiceName = "it-it-x-ital-local",
            activeVoiceName = "it-it-x-ital-local",
            activeVoiceLocaleTag = "it-IT",
            activeLanguageTag = "it-IT",
            engineName = "com.google.android.tts",
            availableItalianVoiceNames = listOf("it-it-x-ital-local"),
        )
        val englishReadback = ItalianTtsVoiceNegotiator.classify(
            requestedLocaleTag = "en-US",
            setLanguageResult = ItalianTtsVoiceNegotiator.TtsLanguageResultCode.LANG_COUNTRY_VAR_AVAILABLE,
            selectedVoiceName = "en-us-x-ene-local",
            activeVoiceName = "en-us-x-ene-local",
            activeVoiceLocaleTag = "en-US",
            activeLanguageTag = "en-US",
            engineName = "com.google.android.tts",
            availableItalianVoiceNames = listOf("it-it-x-ital-local"),
        )
        // Italian app language + Italian active voice → audible.
        assertTrue(TtsSpeakGate.decide("it", italianReadback) is TtsSpeakGate.Verdict.Allow)
        // Switch to German WITHOUT restart: the German request follows the
        // negotiated path (no Italian fail-closed constraint applies).
        assertTrue(TtsSpeakGate.decide("de", englishReadback) is TtsSpeakGate.Verdict.Allow)
        // Switch back to Italian while the voice drifted to English (the
        // engine kept the en voice after the round-trip) → BLOCKED, fail-closed.
        val verdict = TtsSpeakGate.decide("it", englishReadback)
        assertTrue(verdict is TtsSpeakGate.Verdict.Block)
    }

    @Test
    fun `inventory snapshot counts offline and networked italian voices verbatim`() {
        val inventory = ItalianTtsVoiceNegotiator.inventory(
            engineName = "com.google.android.tts",
            candidates = listOf(
                networkedItaly,
                offlineItaly,
                offlineItalianGeneric,
                offlineOther,
            ),
            selectedVoiceName = "it-it-x-ital-local",
            selectedVoiceLocaleTag = "it-IT",
        )
        assertEquals(4, inventory.totalVoices)
        assertEquals(3, inventory.italianVoices)
        assertEquals(2, inventory.offlineItalianVoices)
        assertEquals(1, inventory.networkRequiredItalianVoices)
        assertEquals("it-it-x-ital-local", inventory.selectedVoiceName)
        assertEquals("it-IT", inventory.selectedVoiceLocaleTag)
}
}
