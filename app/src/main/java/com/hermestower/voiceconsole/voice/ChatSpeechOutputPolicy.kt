package com.hermestower.voiceconsole.voice

import com.hermestower.voiceconsole.migration.KeyValueStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * GRAPH 4.10 CP-G4.10-004 (§10 CHAT SPEECH OUTPUT POLICY) — the ONE persistent
 * authority for whether/how the console SPEAKS its chat responses.
 *
 * Owner physical findings P0-04/P0-05: the speaker control did nothing useful
 * and there was no dedicated mute for spoken responses. This policy fixes both:
 *
 *  - AUTO_SPEAK (default): every assistant response is spoken once;
 *  - MUTED: responses stay fully VISIBLE as text; engaging MUTED also STOPS the
 *    utterance in flight; the microphone is NEVER muted by this policy;
 *  - MANUAL: no automatic speech; every assistant message offers a per-message
 *    "Ascolta" playback control.
 *
 * Binding invariants (unit-pinned):
 *  - MUTED keeps text visible (the timeline never reads this policy);
 *  - engaging MUTED stops the CURRENT utterance only;
 *  - stopping the current speech (TTS stop control) NEVER changes the policy —
 *    future responses keep following the mode;
 *  - the microphone privacy mute is INDEPENDENT of this policy and vice versa;
 *  - the mode persists across restarts (KeyValueStore);
 *  - the global Android media volume is never touched by this policy.
 */
class ChatSpeechOutputPolicy(private val keyValueStore: KeyValueStore) {

    enum class Mode { AUTO_SPEAK, MUTED, MANUAL }

    private val _mode = MutableStateFlow(load())
    val mode: StateFlow<Mode> = _mode.asStateFlow()

    /** Set + persist. [reason] is diagnostic (event log only, never transcript). */
    fun setMode(newMode: Mode, reason: String) {
        _mode.value = newMode
        keyValueStore.putString(MODE_KEY, newMode.name)
    }

    /** Chat-speech mute toggle (the VolumeOff/VolumeUp control). */
    fun toggleMuted() {
        setMode(if (_mode.value == Mode.MUTED) Mode.AUTO_SPEAK else Mode.MUTED, "chat-speech toggle")
    }

    fun isMuted(): Boolean = _mode.value == Mode.MUTED

    /**
     * §10: should the driver auto-speak a response right now?
     * MANUAL responses are spoken ONLY through the explicit per-message
     * playback action (repeatLastSpoken), never automatically.
     */
    fun shouldAutoSpeak(): Boolean = _mode.value == Mode.AUTO_SPEAK

    private fun load(): Mode = when (keyValueStore.getString(MODE_KEY)) {
        Mode.MUTED.name -> Mode.MUTED
        Mode.MANUAL.name -> Mode.MANUAL
        else -> Mode.AUTO_SPEAK
    }

    companion object {
        const val MODE_KEY = "chat_speech_output_mode"
    }
}
