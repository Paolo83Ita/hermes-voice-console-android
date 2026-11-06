package com.hermestower.voiceconsole.voice

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * OWNER PHYSICAL RETEST §3: the structured TTS telemetry — typed events, a
 * bounded ring, and the SEC §4 contract: no spoken/user content, ever.
 */
class TtsTelemetryTest {

    @Test
    fun `events are recorded oldest-first with the event name`() {
        val t = TtsTelemetry(clockMillis = { 1000L }, sink = {})
        t.log(TtsTelemetry.Event.LOCALE_REQUESTED, "tag=it-IT")
        t.log(TtsTelemetry.Event.SET_LANGUAGE_RESULT, "code=LANG_COUNTRY_AVAILABLE")
        val snap = t.snapshot()
        assertEquals(2, snap.size)
        assertTrue(snap[0].endsWith("TTS_LOCALE_REQUESTED tag=it-IT"))
        assertTrue(snap[0].startsWith("1000 "))
        assertTrue(snap[1].contains("TTS_SET_LANGUAGE_RESULT"))
    }

    @Test
    fun `the ring is bounded to the capacity`() {
        val t = TtsTelemetry(capacity = 5, clockMillis = { 0L }, sink = {})
        repeat(12) { t.log("EVENT_$it") }
        val snap = t.snapshot()
        assertEquals(5, snap.size)
        assertEquals("0 EVENT_7", snap.first())
        assertEquals("0 EVENT_11", snap.last())
    }

    @Test
    fun `sink receives every event`() {
        val received = mutableListOf<String>()
        val t = TtsTelemetry(clockMillis = { 7L }, sink = { received.add(it) })
        t.log(TtsTelemetry.Event.UTTERANCE_START, "id=hermes-turn")
        assertEquals(1, received.size)
        assertTrue(received[0].endsWith("TTS_UTTERANCE_START id=hermes-turn"))
    }

    @Test
    fun `clear empties the buffer`() {
        val t = TtsTelemetry(clockMillis = { 0L }, sink = {})
        t.log(TtsTelemetry.Event.SPEAK_CALL, "len=42")
        t.clear()
        assertTrue(t.snapshot().isEmpty())
    }

    @Test
    fun `the event vocabulary covers the owner brief §3 list`() {
        val required = listOf(
            "TTS_ENGINE_INIT_OK", "TTS_LOCALE_REQUESTED", "TTS_LANGUAGE_AVAILABILITY",
            "TTS_VOICES_ENUMERATED", "TTS_VOICE_SELECTED", "TTS_NETWORK_REQUIRED",
            "TTS_SET_VOICE_RESULT", "TTS_SPEAK_CALL", "TTS_UTTERANCE_START",
            "TTS_UTTERANCE_DONE", "TTS_UTTERANCE_ERROR", "TTS_FALLBACK_REASON",
            "TTS_RATE_APPLIED", "TTS_PITCH_APPLIED", "TTS_SPEAK_BLOCKED",
            "TTS_READBACK_VOICE", "TTS_READBACK_LANGUAGE", "TTS_NEGOTIATION_STATUS",
        )
        val declared = listOf(
            TtsTelemetry.Event.ENGINE_INIT_OK, TtsTelemetry.Event.LOCALE_REQUESTED,
            TtsTelemetry.Event.LANGUAGE_AVAILABILITY, TtsTelemetry.Event.VOICES_ENUMERATED,
            TtsTelemetry.Event.VOICE_SELECTED, TtsTelemetry.Event.NETWORK_REQUIRED,
            TtsTelemetry.Event.SET_VOICE_RESULT, TtsTelemetry.Event.SPEAK_CALL,
            TtsTelemetry.Event.UTTERANCE_START, TtsTelemetry.Event.UTTERANCE_DONE,
            TtsTelemetry.Event.UTTERANCE_ERROR, TtsTelemetry.Event.FALLBACK_REASON,
            TtsTelemetry.Event.RATE_APPLIED, TtsTelemetry.Event.PITCH_APPLIED,
            TtsTelemetry.Event.SPEAK_BLOCKED, TtsTelemetry.Event.READBACK_VOICE,
            TtsTelemetry.Event.READBACK_LANGUAGE, TtsTelemetry.Event.NEGOTIATION_STATUS,
        )
        assertEquals(required, declared)
    }
}
