package com.hermestower.voiceconsole.voice

import com.hermestower.voiceconsole.voice.ItalianTtsVoiceNegotiator.ItalianTtsVoiceStatus
import com.hermestower.voiceconsole.voice.ItalianTtsVoiceNegotiator.TtsLanguageResultCode
import com.hermestower.voiceconsole.voice.ItalianTtsVoiceNegotiator.TtsVoiceReadback
import com.hermestower.voiceconsole.voice.TtsSpeakGate.BlockReason
import com.hermestower.voiceconsole.voice.TtsSpeakGate.Verdict
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * OWNER PHYSICAL RETEST §4: the FAIL-CLOSED gate. The pre-repair pipeline
 * negotiated the voice but never BLOCKED audio — the engine's default English
 * voice stayed audible while Italian was requested (the exact PHYSICAL-FAIL-01
 * the Owner heard on the POCO X6 Pro). Pure JVM.
 */
class TtsSpeakGateTest {

    private fun readback(status: ItalianTtsVoiceStatus, activeLocale: String? = "en-US") = TtsVoiceReadback(
        requestedLocaleTag = "it-IT",
        setLanguageResult = TtsLanguageResultCode.LANG_COUNTRY_AVAILABLE,
        selectedVoiceName = null,
        activeVoiceName = "en-us-x-ene-local",
        activeVoiceLocaleTag = activeLocale,
        activeLanguageTag = activeLocale,
        engineName = "com.google.android.tts",
        status = status,
        availableItalianVoiceNames = emptyList(),
    )

    @Test
    fun `italian active allows spoken output`() {
        val verdict = TtsSpeakGate.decide("it", readback(ItalianTtsVoiceStatus.ITALIAN_VOICE_ACTIVE, "it-IT"))
        assertTrue(verdict is Verdict.Allow)
    }

    @Test
    fun `the owner-observed fallback class is BLOCKED as readback mismatch`() {
        // setLanguage accepted but the ACTIVE voice is English — the audible
        // English answer class. The gate refuses it.
        val verdict = TtsSpeakGate.decide("it", readback(ItalianTtsVoiceStatus.FALLBACK_VOICE_TRIGGERED))
        assertTrue(verdict is Verdict.Block)
        assertEquals(BlockReason.ITALIAN_SYSTEM_TTS_READBACK_MISMATCH, (verdict as Verdict.Block).reason)
    }

    @Test
    fun `missing italian data is BLOCKED as data missing`() {
        val verdict = TtsSpeakGate.decide("it", readback(ItalianTtsVoiceStatus.ITALIAN_VOICE_MISSING_DATA))
        assertTrue(verdict is Verdict.Block)
        assertEquals(BlockReason.ITALIAN_SYSTEM_TTS_DATA_MISSING, (verdict as Verdict.Block).reason)
    }

    @Test
    fun `unsupported italian is BLOCKED`() {
        val verdict = TtsSpeakGate.decide("it", readback(ItalianTtsVoiceStatus.ITALIAN_VOICE_NOT_SUPPORTED))
        assertTrue(verdict is Verdict.Block)
        assertEquals(BlockReason.ITALIAN_SYSTEM_TTS_NOT_SUPPORTED, (verdict as Verdict.Block).reason)
    }

    @Test
    fun `no readback at all is BLOCKED (engine unavailable)`() {
        val verdict = TtsSpeakGate.decide("it", null)
        assertTrue(verdict is Verdict.Block)
        assertEquals(BlockReason.ITALIAN_SYSTEM_TTS_ENGINE_UNAVAILABLE, (verdict as Verdict.Block).reason)
    }

    @Test
    fun `non-italian effective language allows the negotiated path`() {
        // English app language + English voice: the gate is Italian-specific.
        assertTrue(TtsSpeakGate.decide("en", readback(ItalianTtsVoiceStatus.FALLBACK_VOICE_TRIGGERED)) is Verdict.Allow)
        assertTrue(TtsSpeakGate.decide("de", null) is Verdict.Allow)
        assertTrue(TtsSpeakGate.decide("it-IT", readback(ItalianTtsVoiceStatus.ITALIAN_VOICE_ACTIVE, "it-IT")) is Verdict.Allow)
    }

    @Test
    fun `block verdict carries the readback status verbatim`() {
        val verdict = TtsSpeakGate.decide("it", readback(ItalianTtsVoiceStatus.FALLBACK_VOICE_TRIGGERED)) as Verdict.Block
        assertEquals("FALLBACK_VOICE_TRIGGERED", verdict.readbackStatus)
    }
}
