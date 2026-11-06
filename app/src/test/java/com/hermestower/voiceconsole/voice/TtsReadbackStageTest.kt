package com.hermestower.voiceconsole.voice

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * GRAPH 4.10 CP-G4.10-004 (§12): the graded TTS read-back chain — never one
 * ambiguous ACTIVE. ST-51 requires the real engine to reach
 * ITALIAN_UTTERANCE_COMPLETED; ITALIAN_VOICE_OWNER_VERIFIED is NEVER
 * self-granted by the grader.
 */
class TtsReadbackStageTest {

    @Test
    fun `full chain grades to utterance completed`() {
        assertEquals(
            TtsReadbackStage.ITALIAN_UTTERANCE_COMPLETED,
            TtsReadbackGrader.grade(
                italianRequested = true,
                selectedVoiceName = "it-IT-Standard-A",
                selectedVoiceLocaleTag = "it-IT",
                runtimeLoaded = true,
                utteranceCompleted = true,
            ),
        )
    }

    @Test
    fun `no request stays at the first stage`() {
        assertEquals(
            TtsReadbackStage.ITALIAN_VOICE_REQUESTED,
            TtsReadbackGrader.grade(false, null, null, false, false),
        )
    }

    @Test
    fun `non-italian locale read-back is honest PARTIAL - never full active`() {
        assertEquals(
            TtsReadbackStage.ITALIAN_VOICE_SELECTED_READBACK_PARTIAL,
            TtsReadbackGrader.grade(true, "en-US-Standard-B", "en-US", true, true),
        )
        assertEquals(
            TtsReadbackStage.ITALIAN_VOICE_SELECTED_READBACK_PARTIAL,
            TtsReadbackGrader.grade(true, "some-voice", null, true, true),
        )
    }

    @Test
    fun `locale verified but not loaded stops at locale verified`() {
        assertEquals(
            TtsReadbackStage.ITALIAN_VOICE_LOCALE_VERIFIED,
            TtsReadbackGrader.grade(true, "v", "it-IT", false, false),
        )
    }

    @Test
    fun `loaded but no completed utterance stops at runtime loaded`() {
        assertEquals(
            TtsReadbackStage.ITALIAN_VOICE_RUNTIME_LOADED,
            TtsReadbackGrader.grade(true, "v", "it-IT", true, false),
        )
    }

    @Test
    fun `owner verification is never self-granted`() {
        val stages = TtsReadbackStage.entries
        // The grader's best case is utterance completed; OWNER_VERIFIED requires
        // Owner physical evidence by type system + this pin.
        assertTrue(stages.contains(TtsReadbackStage.ITALIAN_VOICE_OWNER_VERIFIED))
        val best = TtsReadbackGrader.grade(true, "v", "it-IT", true, true)
        assertTrue(best != TtsReadbackStage.ITALIAN_VOICE_OWNER_VERIFIED)
    }
}
