package com.hermestower.voiceconsole.ui.screens

import com.hermestower.voiceconsole.R
import com.hermestower.voiceconsole.app.MicUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * OWNER PHYSICAL RETEST §9/§10: the control-semantics contract — unique icon,
 * label and contentDescription per control state; no two on-screen controls
 * share a content description in ANY state combination; a microphone icon is
 * never used for TTS playback. Pure JVM.
 */
class ConsoleControlSemanticsTest {

    private val micStates = MicUiState.entries
    private val mutedStates = listOf(false, true)

    @Test
    fun `privacy mute is ALWAYS the mic-off identity (never the start-microphone symbol)`() {
        mutedStates.forEach { muted ->
            assertEquals(ConsoleControlIcon.MIC_OFF, ConsoleControlSemanticsResolver.privacyMute(muted).icon)
        }
    }

    @Test
    fun `push-to-talk idle is the MIC identity and listening is STOP`() {
        assertEquals(ConsoleControlIcon.MIC, ConsoleControlSemanticsResolver.pushToTalk(MicUiState.IDLE).icon)
        assertEquals(ConsoleControlIcon.STOP, ConsoleControlSemanticsResolver.pushToTalk(MicUiState.LISTENING).icon)
    }

    @Test
    fun `tts stop is VOLUME_UP — never a microphone icon`() {
        val s = ConsoleControlSemanticsResolver.stopSpeaking()
        assertEquals(ConsoleControlIcon.VOLUME_UP, s.icon)
        assertTrue(s.icon != ConsoleControlIcon.MIC && s.icon != ConsoleControlIcon.MIC_OFF)
    }

    @Test
    fun `the four required semantic identity tokens are present`() {
        val ids = mutableSetOf<String>()
        mutedStates.forEach { muted ->
            micStates.forEach { mic ->
                ConsoleControlSemanticsResolver.activeControls(mic, muted).forEach { ids.add(it.semanticId) }
            }
        }
        ids.add(ConsoleControlSemanticsResolver.textSend().semanticId)
        assertTrue(
            "missing identities: $ids",
            ids.containsAll(
                listOf(
                    "voice_start_listening", "voice_stop_listening",
                    "privacy_mute_enable", "privacy_mute_disable",
                    "tts_stop_speaking", "text_send",
                ),
            ),
        )
    }

    @Test
    fun `no two on-screen controls share a content description in any state`() {
        micStates.forEach { mic ->
            mutedStates.forEach { muted ->
                val cds = ConsoleControlSemanticsResolver.contentDescriptionIds(mic, muted)
                assertEquals(
                    "duplicate content descriptions at mic=$mic muted=$muted: $cds",
                    cds.size,
                    cds.distinct().size,
                )
            }
        }
    }

    @Test
    fun `the muted state carries the persistent label and the listening state the stop label`() {
        assertEquals(
            R.string.mic_label_muted,
            ConsoleControlSemanticsResolver.privacyMute(true).stateDescriptionRes,
        )
        assertEquals(
            R.string.mic_label_stop_listening,
            ConsoleControlSemanticsResolver.pushToTalk(MicUiState.LISTENING).labelRes,
        )
        assertEquals(
            R.string.mic_label_talk,
            ConsoleControlSemanticsResolver.pushToTalk(MicUiState.IDLE).labelRes,
        )
        assertEquals(
            R.string.mic_label_stop_voice,
            ConsoleControlSemanticsResolver.stopSpeaking().labelRes,
        )
    }
}
