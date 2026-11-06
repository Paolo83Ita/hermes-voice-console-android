package com.hermestower.voiceconsole.voice

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * OWNER PHYSICAL RETEST §5: the speech-profile authority — safe ranges,
 * recommended defaults, coercion, presets. Pure JVM.
 */
class SpeechProfileTest {

    @Test
    fun `recommended defaults are the owner-brief starting values`() {
        assertEquals(0.96f, SpeechProfile.RECOMMENDED.speechRate)
        assertEquals(0.95f, SpeechProfile.RECOMMENDED.pitch)
    }

    @Test
    fun `safe ranges match the owner brief`() {
        assertEquals(0.75f, SpeechProfile.RATE_MIN)
        assertEquals(1.30f, SpeechProfile.RATE_MAX)
        assertEquals(0.75f, SpeechProfile.PITCH_MIN)
        assertEquals(1.25f, SpeechProfile.PITCH_MAX)
    }

    @Test
    fun `coercion clamps out-of-range values into the safe ranges`() {
        val fast = SpeechProfile.safe(3.0f, 0.5f)
        assertTrue(fast.speechRate <= SpeechProfile.RATE_MAX)
        assertTrue(fast.pitch >= SpeechProfile.PITCH_MIN)
        val slow = SpeechProfile.safe(0.1f, 9.9f)
        assertTrue(slow.speechRate >= SpeechProfile.RATE_MIN)
        assertTrue(slow.pitch <= SpeechProfile.PITCH_MAX)
    }

    @Test
    fun `in-range values pass through unchanged`() {
        assertEquals(SpeechProfile(1.0f, 1.0f), SpeechProfile.safe(1.0f, 1.0f))
    }

    @Test
    fun `presets stay inside the safe ranges`() {
        listOf(
            SpeechProfile.RATE_SLOWER, SpeechProfile.RECOMMENDED_RATE, SpeechProfile.RATE_FASTER,
        ).forEach { assertTrue(it in SpeechProfile.RATE_MIN..SpeechProfile.RATE_MAX) }
        listOf(
            SpeechProfile.PITCH_LOWER, SpeechProfile.RECOMMENDED_PITCH, SpeechProfile.PITCH_HIGHER,
        ).forEach { assertTrue(it in SpeechProfile.PITCH_MIN..SpeechProfile.PITCH_MAX) }
    }
}
