package com.hermestower.voiceconsole.voice

import com.hermestower.voiceconsole.migration.KeyValueStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * GRAPH 4.10 CP-G4.10-005 (§14 LISTENING MODE AUTHORITY) — the ONE canonical
 * authority for how the console listens.
 *
 * Modes:
 *  - OFF                  — listening fully disabled;
 *  - PUSH_TO_TALK         — the default: one capture per press;
 *  - CONTINUOUS_WHILE_SCREEN_ACTIVE — the console keeps listening turn after
 *    turn until stopped (explicitly enabled by the user, foreground only);
 *  - WAKE_WORD            — UNAVAILABLE until physically qualified (the policy
 *    records the request but the coordinator refuses to arm it: WAKE_WORD
 *    stays honest until a physical qualification pass exists).
 *
 * All persisted fields of §14 live here: mode, session start, session limit,
 * resume delay, barge-in state, last stop reason, change source. The microphone
 * privacy mute is a SEPARATE physical authority and is never written by this one.
 */
class ListeningModePolicy(private val keyValueStore: KeyValueStore) {

    enum class Mode { OFF, PUSH_TO_TALK, CONTINUOUS_WHILE_SCREEN_ACTIVE, WAKE_WORD }

    /** §15 session-limit options (minutes; 0 = while the Console remains open). */
    enum class SessionLimitMinutes(val minutes: Int, val labelRes: Int) {
        FIVE(5, 0), FIFTEEN(15, 0), THIRTY(30, 0), WHILE_OPEN(0, 0)
    }

    enum class BargeIn { BARGE_IN_OFF, BARGE_IN_ON }

    enum class StopReason { USER_STOP, PRIVACY_MUTE, BACKGROUND, PERMISSION_REVOKED, AUDIO_FAILURE, SESSION_TIMEOUT, THERMAL_POLICY, ENGINE_FAILURE, NONE }

    data class ListeningState(
        val mode: Mode = Mode.PUSH_TO_TALK,
        val sessionLimitMinutes: Int = DEFAULT_SESSION_LIMIT_MINUTES,
        val resumeDelayMs: Long = DEFAULT_RESUME_DELAY_MS,
        val bargeIn: BargeIn = BargeIn.BARGE_IN_OFF,
        val sessionStartElapsedMs: Long? = null,
        val lastStopReason: StopReason = StopReason.NONE,
        val lastChangeSource: String = "default",
        /** WAKE_WORD requested but not physically qualified — honest refusal. */
        val wakeWordBlocked: Boolean = false,
    ) {
        val continuousActive: Boolean get() = mode == Mode.CONTINUOUS_WHILE_SCREEN_ACTIVE && sessionStartElapsedMs != null
    }

    private val _state = MutableStateFlow(load())
    val state: StateFlow<ListeningState> = _state.asStateFlow()

    /** Set + persist. [source] records who changed it (settings UI, voice stop, timeout…). */
    fun setMode(mode: Mode, source: String) {
        val s = _state.value
        _state.value = s.copy(
            mode = mode,
            lastChangeSource = source,
            wakeWordBlocked = mode == Mode.WAKE_WORD,
            sessionStartElapsedMs = if (mode == Mode.CONTINUOUS_WHILE_SCREEN_ACTIVE) s.sessionStartElapsedMs else null,
        )
        keyValueStore.putString(MODE_KEY, mode.name)
    }

    fun setSessionLimit(minutes: Int, source: String) {
        _state.value = _state.value.copy(sessionLimitMinutes = minutes, lastChangeSource = source)
        keyValueStore.putString(LIMIT_KEY, minutes.toString())
    }

    fun setBargeIn(bargeIn: BargeIn, source: String) {
        _state.value = _state.value.copy(bargeIn = bargeIn, lastChangeSource = source)
        keyValueStore.putString(BARGE_KEY, bargeIn.name)
    }

    /** The controller records when a continuous session physically starts. */
    fun sessionStarted(startElapsedMs: Long) {
        _state.value = _state.value.copy(sessionStartElapsedMs = startElapsedMs)
    }

    fun sessionStopped(reason: StopReason, source: String) {
        _state.value = _state.value.copy(
            sessionStartElapsedMs = null,
            lastStopReason = reason,
            lastChangeSource = source,
            mode = if (reason == StopReason.USER_STOP && _state.value.mode == Mode.CONTINUOUS_WHILE_SCREEN_ACTIVE) {
                Mode.PUSH_TO_TALK // user stop returns to the default mode honestly
            } else {
                _state.value.mode
            },
        )
        if (_state.value.mode != Mode.CONTINUOUS_WHILE_SCREEN_ACTIVE) {
            keyValueStore.putString(MODE_KEY, _state.value.mode.name)
        }
        keyValueStore.putString(STOP_REASON_KEY, reason.name)
    }

    /** §14: WAKE_WORD stays unavailable until physically qualified. */
    fun isWakeWordQualified(): Boolean = false

    private fun load(): ListeningState {
        val mode = when (keyValueStore.getString(MODE_KEY)) {
            Mode.OFF.name -> Mode.OFF
            Mode.CONTINUOUS_WHILE_SCREEN_ACTIVE.name -> Mode.CONTINUOUS_WHILE_SCREEN_ACTIVE
            Mode.WAKE_WORD.name -> Mode.WAKE_WORD
            else -> Mode.PUSH_TO_TALK
        }
        val limit = keyValueStore.getString(LIMIT_KEY)?.toIntOrNull() ?: DEFAULT_SESSION_LIMIT_MINUTES
        val barge = when (keyValueStore.getString(BARGE_KEY)) {
            BargeIn.BARGE_IN_ON.name -> BargeIn.BARGE_IN_ON
            else -> BargeIn.BARGE_IN_OFF
        }
        return ListeningState(
            mode = mode,
            sessionLimitMinutes = limit,
            bargeIn = barge,
            wakeWordBlocked = mode == Mode.WAKE_WORD,
        )
    }

    companion object {
        const val MODE_KEY = "listening_mode"
        const val LIMIT_KEY = "listening_session_limit_minutes"
        const val BARGE_KEY = "listening_barge_in"
        const val STOP_REASON_KEY = "listening_last_stop_reason"

        /** §15 initial defaults. */
        const val DEFAULT_RESUME_DELAY_MS = 750L
        const val DEFAULT_SESSION_LIMIT_MINUTES = 15

        val SESSION_LIMIT_OPTIONS = listOf(5, 15, 30, 0)
    }
}
