package com.hermestower.voiceconsole.ui.screens

import com.hermestower.voiceconsole.R
import com.hermestower.voiceconsole.app.MicUiState

/**
 * ConsoleControlSemantics (OWNER PHYSICAL RETEST §9/§10): the REQUIRED visual
 * vocabulary that separates start-microphone, stop-microphone, privacy mute
 * and TTS-stop into four semantically distinct controls.
 *
 * The physical defect (PHYSICAL-FAIL-04): the privacy-mute medallion rendered
 * `Mic` when unmuted and the push-to-talk medallion rendered `Mic` when idle —
 * two adjacent controls with the SAME symbol (and the stop-speaking medallion
 * shared `Stop` with the listening push-to-talk). This resolver is the single
 * typed authority for icon + label + contentDescription + stateDescription per
 * control state; the composable only renders what it resolves.
 *
 * Pure Kotlin (resource IDs only — no Android framework): the uniqueness test
 * runs on the JVM.
 */
enum class ConsoleControlIcon { MIC, STOP, MIC_OFF, VOLUME_UP, REPLAY, SEND, HISTORY }

/** One resolved control identity (brief §10: unique icon + label + cd + state). */
data class ConsoleControlSemantics(
    /** The required semantic identity token (brief §10). */
    val semanticId: String,
    val icon: ConsoleControlIcon,
    val labelRes: Int,
    val contentDescriptionRes: Int,
    /** Optional stateDescription for TalkBack (null = the cd carries the state). */
    val stateDescriptionRes: Int?,
)

object ConsoleControlSemanticsResolver {

    // ---- Required semantic identities (brief §10) ----
    const val ID_VOICE_START_LISTENING = "voice_start_listening"
    const val ID_VOICE_STOP_LISTENING = "voice_stop_listening"
    const val ID_PRIVACY_MUTE_ENABLE = "privacy_mute_enable"
    const val ID_PRIVACY_MUTE_DISABLE = "privacy_mute_disable"
    const val ID_TTS_STOP_SPEAKING = "tts_stop_speaking"
    const val ID_TEXT_SEND = "text_send"

    /**
     * PRIVACY MUTE control. Its identity is ALWAYS `MicOff` (the privacy
     * control — never the start-microphone symbol). Muted adds the red-bronze
     * privacy-seal treatment rendered by the composable.
     */
    fun privacyMute(muted: Boolean): ConsoleControlSemantics =
        if (muted) {
            ConsoleControlSemantics(
                semanticId = ID_PRIVACY_MUTE_DISABLE,
                icon = ConsoleControlIcon.MIC_OFF,
                labelRes = R.string.mic_label_muted,
                contentDescriptionRes = R.string.mic_cd_mute_disable,
                stateDescriptionRes = R.string.mic_label_muted,
            )
        } else {
            ConsoleControlSemantics(
                semanticId = ID_PRIVACY_MUTE_ENABLE,
                icon = ConsoleControlIcon.MIC_OFF,
                labelRes = R.string.mic_label_mute_enable,
                contentDescriptionRes = R.string.mic_cd_mute_enable,
                stateDescriptionRes = null,
            )
        }

    /**
     * PUSH-TO-TALK control. Idle = `Mic` with the Parla label; listening =
     * `Stop` with the stop-listening label (never the mute symbol).
     * Permission-denied states keep the start identity (the control's action
     * IS the permission request).
     */
    fun pushToTalk(micState: MicUiState): ConsoleControlSemantics =
        if (micState == MicUiState.LISTENING) {
            ConsoleControlSemantics(
                semanticId = ID_VOICE_STOP_LISTENING,
                icon = ConsoleControlIcon.STOP,
                labelRes = R.string.mic_label_stop_listening,
                contentDescriptionRes = R.string.mic_cd_stop_listening,
                stateDescriptionRes = R.string.mic_state_listening,
            )
        } else {
            ConsoleControlSemantics(
                semanticId = ID_VOICE_START_LISTENING,
                icon = ConsoleControlIcon.MIC,
                labelRes = R.string.mic_label_talk,
                contentDescriptionRes = R.string.mic_cd_start_listening,
                stateDescriptionRes = null,
            )
        }

    /**
     * TTS STOP control — `VolumeUp` (voice OUTPUT), never a microphone icon
     * and never the same symbol as the listening push-to-talk's neighbor set
     * once the waveform variant is chosen for barge-in (brief §9: do not use
     * a microphone icon for TTS playback).
     */
    fun stopSpeaking(): ConsoleControlSemantics = ConsoleControlSemantics(
        semanticId = ID_TTS_STOP_SPEAKING,
        icon = ConsoleControlIcon.VOLUME_UP,
        labelRes = R.string.mic_label_stop_voice,
        contentDescriptionRes = R.string.mic_cd_stop_speaking,
        stateDescriptionRes = null,
    )

    /** TEXT SEND control (brief §10 semantic identity). */
    fun textSend(): ConsoleControlSemantics = ConsoleControlSemantics(
        semanticId = ID_TEXT_SEND,
        icon = ConsoleControlIcon.SEND,
        labelRes = R.string.composer_send,
        contentDescriptionRes = R.string.mic_cd_text_send,
        stateDescriptionRes = null,
    )

    /**
     * The on-screen control set for a given console state — the uniqueness
     * test resolves this for EVERY state combination and asserts that no two
     * controls share a content description.
     */
    fun activeControls(micState: MicUiState, muted: Boolean): List<ConsoleControlSemantics> = listOf(
        privacyMute(muted),
        pushToTalk(micState),
        stopSpeaking(),
    )

    /** All content descriptions for a state — used by the uniqueness contract. */
    fun contentDescriptionIds(micState: MicUiState, muted: Boolean): List<Int> =
        activeControls(micState, muted).map { it.contentDescriptionRes } + textSend().contentDescriptionRes
}
