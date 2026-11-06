package com.hermestower.voiceconsole.voice

import com.hermestower.voiceconsole.core.state.ConsoleState
import com.hermestower.voiceconsole.domain.LocalEventStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * GRAPH 4.10 CP-G4.10-006 (§15 CONTINUOUS LISTENING FLOW).
 *
 * Drives the turn-after-turn loop while CONTINUOUS_WHILE_SCREEN_ACTIVE is on:
 *   capture → (system recognizer VAD + end-of-speech) → STT final → submit turn
 *   → capture stays OFF during TTS (single mic owner) → response spoken/visible
 *   → utterance completion (state back to IDLE) → post-speech guard interval
 *   → stale-transcript clearance → resume capture → repeat, until a stop
 *   condition fires.
 *
 * Stop conditions (§15, all typed): user stop, privacy mute, app background,
 * permission revoked, audio failure (start refused), session timeout, thermal
 * policy, unrecoverable engine failure.
 *
 * Echo prevention (§16) is enforced at the turn boundary: the coordinator
 * rejects likely-echo transcripts (EchoFingerprintGuard) and capture only ever
 * resumes after the post-speech guard delay (default 750 ms) — the console
 * cannot listen while it is speaking (BARGE_IN_OFF default).
 */
class ContinuousListeningController(
    private val scope: CoroutineScope,
    private val storeProvider: () -> ConsoleState,
    val policy: ListeningModePolicy,
    private val events: LocalEventStore,
    private val startCapture: () -> Boolean,
    private val stopCapture: (reason: String) -> Unit,
    private val hasMicPermission: () -> Boolean,
    private val isPrivacyMuted: () -> Boolean,
    private val isThermalConstrained: () -> Boolean = { false },
    private val clockMs: () -> Long = { System.currentTimeMillis() },
) {

    private val _active = MutableStateFlow(false)
    val active: StateFlow<Boolean> = _active.asStateFlow()

    private var supervisor: Job? = null

    /** §15: explicitly enabled by the user; starts the loop. */
    fun start(source: String): Boolean {
        if (_active.value) return true
        if (policy.isWakeWordQualified() == false && policy.state.value.mode == ListeningModePolicy.Mode.WAKE_WORD) {
            events.log("CONTINUOUS", "wake word refused: not physically qualified")
            return false
        }
        if (isPrivacyMuted()) {
            events.log("CONTINUOUS", "refused: privacy mute")
            return false
        }
        if (!hasMicPermission()) {
            events.log("CONTINUOUS", "refused: permission missing")
            return false
        }
        policy.setMode(ListeningModePolicy.Mode.CONTINUOUS_WHILE_SCREEN_ACTIVE, source)
        policy.sessionStarted(clockMs())
        _active.value = true
        events.log("CONTINUOUS", "session start source=$source")
        supervisor = scope.launch { supervise() }
        // Kick the first capture immediately (state must be at rest).
        if (storeProvider() == ConsoleState.IDLE) startCapture()
        return true
    }

    /** Typed stop (§15): pauses capture, records the reason, ends the session. */
    fun stop(reason: ListeningModePolicy.StopReason, source: String) {
        if (!_active.value) return
        _active.value = false
        supervisor?.cancel()
        supervisor = null
        stopCapture("continuous listening stopped: ${reason.name}")
        policy.sessionStopped(reason, source)
        events.log("CONTINUOUS", "session stop reason=${reason.name} source=$source")
    }

    /** App leaves the foreground → capture must stop (foreground-only contract). */
    fun onConsolePaused() {
        if (_active.value) stop(ListeningModePolicy.StopReason.BACKGROUND, "lifecycle")
    }

    fun onPrivacyMuteEngaged() {
        if (_active.value) stop(ListeningModePolicy.StopReason.PRIVACY_MUTE, "privacy mute")
    }

    fun onPermissionRevoked() {
        if (_active.value) stop(ListeningModePolicy.StopReason.PERMISSION_REVOKED, "permission")
    }

    /**
     * The supervision loop: while active, watch the console come back to rest
     * after each turn, wait the post-speech guard, then resume capture —
     * bounded by the session limit, privacy mute, thermal policy and permission.
     */
    private suspend fun supervise() {
        var sawActivity = false
        while (_active.value) {
            val state = storeProvider()
            if (state != ConsoleState.IDLE) sawActivity = true

            if (sawActivity && state == ConsoleState.IDLE) {
                // §15 steps 8–11: utterance completion → guard → clear stale → resume.
                delay(policy.state.value.resumeDelayMs)
                if (!_active.value) return
                val elapsedMin = sessionElapsedMinutes()
                val limit = policy.state.value.sessionLimitMinutes
                if (limit > 0 && elapsedMin >= limit) {
                    stop(ListeningModePolicy.StopReason.SESSION_TIMEOUT, "session limit")
                    return
                }
                if (isPrivacyMuted()) {
                    stop(ListeningModePolicy.StopReason.PRIVACY_MUTE, "privacy mute")
                    return
                }
                if (!hasMicPermission()) {
                    stop(ListeningModePolicy.StopReason.PERMISSION_REVOKED, "permission")
                    return
                }
                if (isThermalConstrained()) {
                    stop(ListeningModePolicy.StopReason.THERMAL_POLICY, "thermal")
                    return
                }
                if (storeProvider() != ConsoleState.IDLE) continue
                sawActivity = false
                // §16: stale-transcript clearance is handled by the coordinator
                // on start; capture resumes only now (post-TTS guard elapsed).
                val started = runCatching { startCapture() }.getOrDefault(false)
                if (!started) {
                    stop(ListeningModePolicy.StopReason.AUDIO_FAILURE, "start refused")
                    return
                }
            } else if (sawActivity && (state == ConsoleState.DORMANT)) {
                stop(ListeningModePolicy.StopReason.ENGINE_FAILURE, "dormant mid-session")
                return
            }
            delay(SUPERVISE_TICK_MS)
        }
    }

    private fun sessionElapsedMinutes(): Long =
        policy.state.value.sessionStartElapsedMs?.let { (clockMs() - it) / 60_000L } ?: 0L

    companion object {
        const val SUPERVISE_TICK_MS = 200L
    }
}
