package com.hermestower.voiceconsole.app

import com.hermestower.voiceconsole.core.state.ConsoleState
import com.hermestower.voiceconsole.core.state.ConsoleStateStore
import com.hermestower.voiceconsole.local.MockLocalConversationEngine
import com.hermestower.voiceconsole.locale.ResponseCategory
import com.hermestower.voiceconsole.locale.ResponseCatalog
import com.hermestower.voiceconsole.migration.KeyValueStore
import com.hermestower.voiceconsole.privacy.LogRedactor
import com.hermestower.voiceconsole.simulator.ScriptedConsoleDriver
import com.hermestower.voiceconsole.voice.EchoFingerprintGuard
import com.hermestower.voiceconsole.voice.MicPermissionFlow
import com.hermestower.voiceconsole.voice.SystemSttEngine
import com.hermestower.voiceconsole.voice.SystemSttError
import com.hermestower.voiceconsole.voice.SystemSttEvents
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * GRAPH 4 G4-C03 (§4/§5): wires the REAL microphone path into the Main Console
 * with a single state authority (ConsoleStateStore) and a single routing/
 * history/receipt path (ScriptedConsoleDriver) — voice and text share the same
 * conversation identity per the Owner binding delta.
 *
 * PHYSICS (never decorative):
 *  - LISTENING appears only while the system recognizer physically owns the
 *    microphone (onCaptureStarted/Ended drive the truth);
 *  - privacy mute refuses turns and physically stops capture (INV-G4-10);
 *  - partial transcript goes to the live draft; final transcript goes through
 *    the SAME pipeline as typed text (routing, LOCAL/TOWER attribution,
 *    history, receipts, TTS);
 *  - no raw audio, no transcript content in logs (SEC §4: length-only).
 */
enum class MicUiState(val label: String) {
    IDLE("Microphone idle"),
    PERMISSION_REQUIRED("Microphone permission required"),
    LISTENING("Listening"),
    DENIED("Microphone denied — text input still works"),
    PERMANENTLY_DENIED("Microphone permanently denied — enable in Settings"),
    UNAVAILABLE("No speech recognizer available — use text input"),
}

class ConsoleVoiceCoordinator(
    private val scope: CoroutineScope,
    private val store: ConsoleStateStore,
    private val driver: ScriptedConsoleDriver,
    private val systemStt: SystemSttEngine,
    private val hasMicPermission: () -> Boolean,
    private val keyValueStore: com.hermestower.voiceconsole.migration.KeyValueStore,
    private val events: com.hermestower.voiceconsole.domain.LocalEventStore,
    private val languageTag: String = "it-IT",
    /** GRAPH 4.10 §4: application-language tag for user-visible status notes. */
    private val statusLocale: () -> String = { "en" },
    /** §16: likely-echo transcripts (the console's own spoken text) are rejected. */
    private val echoGuard: EchoFingerprintGuard? = null,
) {

    private val _micState = MutableStateFlow(MicUiState.IDLE)
    val micState: StateFlow<MicUiState> = _micState.asStateFlow()

    /** Why the last push-to-talk was refused (shown once, typed). */
    private val _lastRefusal = MutableStateFlow<String?>(null)
    val lastRefusal: StateFlow<String?> = _lastRefusal.asStateFlow()

    init {
        // Delta §15: chat draft persists across process recreation and restart.
        keyValueStore.getString(DRAFT_KEY)?.takeIf { it.isNotEmpty() }?.let { saved ->
            store.setTranscriptDraft(saved)
        }
        scope.launch {
            store.transcriptDraft.collect { draft ->
                if (draft.isNullOrEmpty()) keyValueStore.remove(DRAFT_KEY)
                else keyValueStore.putString(DRAFT_KEY, draft)
            }
        }
    }

    /**
     * Composer "speak instead": same gate as push-to-talk, but a missing
     * permission is routed to the typed UI launcher instead of failing.
     */
    fun onPushToTalkPressWithPermission(requestPermission: () -> Unit) {
        if (!hasMicPermission()) {
            _micState.value = MicUiState.PERMISSION_REQUIRED
            requestPermission()
            return
        }
        onPushToTalkPress()
    }

    /**
     * Push-to-talk: one real capture turn. Returns TRUE only when the
     * recognizer physically started (§15 continuous loop consumes this).
     */
    fun onPushToTalkPress(): Boolean {
        _lastRefusal.value = null
        if (store.privacyMuted.value) {
            _lastRefusal.value = ResponseCatalog.text(ResponseCategory.MIC_PRIVACY_REFUSAL, statusLocale())
            events.log("MIC", "push-to-talk refused: privacy mute")
            return false
        }
        if (store.currentState() == ConsoleState.DORMANT || store.currentState().isPipelineActive) return false
        if (!hasMicPermission()) {
            _micState.value = MicUiState.PERMISSION_REQUIRED
            events.log("MIC", "push-to-talk refused: permission missing")
            return false
        }
        if (!systemStt.isAvailable) {
            _micState.value = MicUiState.UNAVAILABLE
            _lastRefusal.value = ResponseCatalog.text(ResponseCategory.MIC_UNAVAILABLE, statusLocale())
            events.log("MIC", "push-to-talk refused: recognizer unavailable")
            return false
        }
        // Owner override to rest, then enter LISTENING legally (IDLE→LISTENING).
        store.transition(ConsoleState.IDLE)
        if (store.transition(ConsoleState.LISTENING) == null) {
            events.log("MIC", "LISTENING transition rejected from ${store.currentState()}")
            return false
        }
        val started = systemStt.start(languageTag, listener)
        if (!started) {
            _micState.value = MicUiState.UNAVAILABLE
            store.transition(ConsoleState.IDLE)
            _lastRefusal.value = ResponseCatalog.text(ResponseCategory.MIC_REFUSED, statusLocale())
            events.log("MIC", "recognizer start refused")
            return false
        }
        _micState.value = MicUiState.LISTENING
        events.log("MIC", "capture started (system recognizer, len-guard on)")
        return true
    }

    /** Owner stop / lifecycle / mute: capture physically stops, no capture after. */
    fun stopCapture(reason: String) {
        systemStt.stop()
        if (_micState.value == MicUiState.LISTENING) _micState.value = MicUiState.IDLE
        if (store.currentState() == ConsoleState.LISTENING) {
            store.transition(ConsoleState.IDLE)
            store.setStatusNote(ResponseCatalog.text(ResponseCategory.MIC_CAPTURE_STOPPED, statusLocale(), reason))
        }
        events.log("MIC", "capture stopped: $reason")
    }

    /** Foreground-only capture by default (§4): app loses the screen → stop. */
    fun onConsolePaused() = stopCapture("console paused")

    /** Typed permission result from the UI launcher. */
    fun onMicPermissionResult(granted: Boolean, shouldShowRationale: Boolean) {
        val previouslyRequested = keyValueStore.getString(PERM_KEY) == "true"
        keyValueStore.putString(PERM_KEY, "true")
        val classified = MicPermissionFlow.classify(granted, shouldShowRationale, previouslyRequested)
        _micState.value = when (classified) {
            com.hermestower.voiceconsole.voice.MicPermissionState.GRANTED -> MicUiState.IDLE
            com.hermestower.voiceconsole.voice.MicPermissionState.DENIED -> MicUiState.DENIED
            com.hermestower.voiceconsole.voice.MicPermissionState.PERMANENTLY_DENIED -> MicUiState.PERMANENTLY_DENIED
            else -> MicUiState.PERMISSION_REQUIRED
        }
        if (!granted) store.setStatusNote(classified.displayLabel)
        events.log("MIC", "permission result classified=${classified.name}")
    }

    fun micPermissionPreviouslyRequested(): Boolean = keyValueStore.getString(PERM_KEY) == "true"

    private val listener = object : SystemSttEvents {
        override fun onCaptureStarted() {
            // Physical truth: mic owned by the recognizer from this moment.
        }

        override fun onPartial(text: String) {
            store.setTranscriptDraft(text) // live partial transcript (§5)
        }

        override fun onFinal(text: String, confidence: Float?) {
            _micState.value = MicUiState.IDLE
            val final = text.trim()
            if (final.isEmpty()) {
                store.setStatusNote(ResponseCatalog.text(ResponseCategory.MIC_EMPTY, statusLocale()))
                if (store.currentState() == ConsoleState.LISTENING) store.transition(ConsoleState.IDLE)
                events.log("MIC", "final transcript empty")
                return
            }
            // Privacy: length only (SEC §4). The transcript flows through the
            // SAME pipeline as typed text — same routing, attribution, history.
            // §16 ECHO PREVENTION: a transcript that (near-)verbatim matches the
            // assistant's recent spoken response inside the time window is
            // REJECTED — the console never turns its own voice into a user turn.
            if (echoGuard?.isLikelyEcho(final) == true) {
                events.log("MIC", "echo rejected len=${final.length}")
                store.setStatusNote("echo rejected")
                if (store.currentState() == ConsoleState.LISTENING) store.transition(ConsoleState.IDLE)
                return
            }
            events.log("MIC", "final transcript len=${final.length} conf=${confidence ?: -1f}")
            store.setTranscriptDraft(final)
            driver.submitUtterance(final, confidence ?: 0.9f, languageHint = languageTag)
        }

        override fun onError(error: SystemSttError) {
            _micState.value = when (error.kind) {
                com.hermestower.voiceconsole.voice.SystemSttErrorKind.PERMISSION -> MicUiState.DENIED
                else -> MicUiState.IDLE
            }
            store.setStatusNote(
                when (error.kind) {
                    com.hermestower.voiceconsole.voice.SystemSttErrorKind.NO_SPEECH ->
                        ResponseCatalog.text(ResponseCategory.MIC_NO_SPEECH, statusLocale())
                    com.hermestower.voiceconsole.voice.SystemSttErrorKind.NETWORK_REQUIRED ->
                        ResponseCatalog.text(ResponseCategory.MIC_NETWORK, statusLocale())
                    com.hermestower.voiceconsole.voice.SystemSttErrorKind.BUSY ->
                        ResponseCatalog.text(ResponseCategory.MIC_BUSY, statusLocale())
                    com.hermestower.voiceconsole.voice.SystemSttErrorKind.PERMISSION ->
                        ResponseCatalog.text(ResponseCategory.MIC_PERMISSION_REVOKED, statusLocale())
                    else -> ResponseCatalog.text(ResponseCategory.MIC_FAILED, statusLocale(), error.systemCode)
                },
            )
            events.log("MIC", "recognizer error kind=${error.kind.name} code=${error.systemCode}")
            if (store.currentState() == ConsoleState.LISTENING) store.transition(ConsoleState.IDLE)
        }

        override fun onCaptureEnded() {
            // If no final transcript handed the turn to the pipeline, come back
            // to rest truthfully: never linger in LISTENING without the mic.
            if (_micState.value == MicUiState.LISTENING) _micState.value = MicUiState.IDLE
            if (store.currentState() == ConsoleState.LISTENING) store.transition(ConsoleState.IDLE)
        }
    }

    companion object {
        const val DRAFT_KEY = "chat_draft_console"
        const val PERM_KEY = "mic_permission_requested_before"
    }
}
