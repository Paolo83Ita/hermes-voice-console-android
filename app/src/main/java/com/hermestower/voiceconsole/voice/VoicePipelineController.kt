package com.hermestower.voiceconsole.voice

import com.hermestower.voiceconsole.core.mode.OperatingMode
import com.hermestower.voiceconsole.core.routing.Connectivity
import com.hermestower.voiceconsole.core.routing.IntentRouter
import com.hermestower.voiceconsole.core.routing.RoutingAction
import com.hermestower.voiceconsole.core.routing.RoutingRequest
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.Instant

/**
 * VoicePipelineController (GRAPH 4 NODE 3, §8): one real voice turn from the
 * microphone to the speaker with typed states, privacy mute, barge-in, LOCAL vs
 * TOWER attribution and a history record.
 *
 * Policy encoded here:
 *  - PUSH_TO_TALK is the default activation for initial physical qualification;
 *  - privacy mute (INV-G4-10) PHYSICALLY stops the frame source — no frames flow
 *    after mute, the microphone hardware is released;
 *  - raw audio is consumed in-flight and dropped; nothing retains frames
 *    (INV-G4-09: recording/raw-audio retention OFF by default);
 *  - no-speech timeout is configurable 3–10 s (§9) and ends the turn truthfully;
 *  - routing uses the existing deterministic IntentRouter; attribution is made
 *    visible ("Handled locally" / "Sent to Hermes Tower") — no silent ambiguity;
 *  - TTS fallback chain: local voice engine → (system engine injected by the
 *    composition root as a second candidate later) → text-only (§10).
 */
enum class VoicePipelineState(val displayLabel: String) {
    IDLE("Idle"),
    MUTED("Privacy mute"),
    LISTENING("Listening"),
    SPEECH_DETECTED("Speech detected"),
    TRANSCRIBING("Transcribing"),
    ROUTING("Routing"),
    RESPONDING("Responding"),
    SPEAKING("Speaking"),
    CANCELLED("Cancelled"),
    NO_SPEECH("No speech"),
    FAILED("Failed"),
}

/** Typed outcome of one turn (§8 receipt and history input). */
data class VoiceTurnRecord(
    val transcript: String,
    val confidence: Float?,
    val route: String,
    val attribution: String,
    val responseText: String?,
    val spoken: Boolean,
    val at: Instant,
    val elapsedMs: Long,
    val errorCode: String? = null,
) {
    companion object {
        const val ROUTE_LOCAL = "LOCAL"
        const val ROUTE_TOWER = "TOWER"
        const val ROUTE_NONE = "NONE"
        const val ATTRIBUTION_LOCAL = "Handled locally"
        const val ATTRIBUTION_TOWER = "Sent to Hermes Tower"
        const val ATTRIBUTION_NONE = "Not handled"
    }
}

class VoicePipelineController(
    private val sourceFactory: () -> AudioFrameSource,
    private val vad: VadEngine,
    private val stt: SttEngine,
    private val tts: TtsEngine?,
    private val router: IntentRouter,
    private val modeProvider: () -> OperatingMode,
    private val connectivityProvider: () -> Connectivity,
    /** Safe local execution (greetings, status, help). Returns null when it cannot. */
    private val localExecutor: suspend (String) -> String?,
    /** Tower execution (mock bridge in simulation; real bridge when paired). */
    private val towerExecutor: suspend (String) -> String?,
    /** History sink (never raw audio). */
    private val historySink: (VoiceTurnRecord) -> Unit = {},
    /** No-speech timeout, milliseconds. §9 allows 3000–10000. */
    private val noSpeechTimeoutMs: Long = 5_000,
    /** Hard cap on one listening turn. */
    private val maxTurnMs: Long = 30_000,
    /** Frame read pacing for sources that return immediately (tests). */
    private val framePollDelayMs: Long = 5,
) {

    private val _state = MutableStateFlow(VoicePipelineState.IDLE)
    val state: StateFlow<VoicePipelineState> = _state.asStateFlow()

    private val _muted = MutableStateFlow(false)
    val muted: StateFlow<Boolean> = _muted.asStateFlow()

    @Volatile private var activeSource: AudioFrameSource? = null
    @Volatile private var turnCancelled = false

    /**
     * Privacy mute (INV-G4-10): stops the hardware capture immediately. While
     * muted, listenOnce() is refused — the voice pipeline cannot continue.
     */
    fun setMuted(muted: Boolean) {
        _muted.value = muted
        if (muted) {
            turnCancelled = true
            activeSource?.stop()
            activeSource = null
            tts?.interrupt()
            if (_state.value != VoicePipelineState.IDLE) _state.value = VoicePipelineState.MUTED
        } else {
            if (_state.value == VoicePipelineState.MUTED) _state.value = VoicePipelineState.IDLE
        }
    }

    /**
     * One push-to-talk turn. Returns the turn record — the outcome is always
     * typed and truthful (route NONE with an error code on no-speech, mute,
     * cancel or failure).
     */
    suspend fun listenOnce(): VoiceTurnRecord {
        val started = Instant.now()
        val startMs = System.currentTimeMillis()
        var lastSpeechAtMs = -1L

        fun record(
            transcript: String,
            confidence: Float?,
            route: String,
            attribution: String,
            responseText: String?,
            spoken: Boolean,
            errorCode: String?,
            endState: VoicePipelineState,
        ): VoiceTurnRecord {
            _state.value = endState
            val rec = VoiceTurnRecord(
                transcript = transcript,
                confidence = confidence,
                route = route,
                attribution = attribution,
                responseText = responseText,
                spoken = spoken,
                at = started,
                elapsedMs = System.currentTimeMillis() - startMs,
                errorCode = errorCode,
            )
            historySink(rec)
            return rec
        }

        if (_muted.value) {
            return record("", null, VoiceTurnRecord.ROUTE_NONE, VoiceTurnRecord.ATTRIBUTION_NONE, null, false, "MUTED", VoicePipelineState.MUTED)
        }
        turnCancelled = false
        vad.reset()
        stt.reset()
        stt.load()
        if (stt.state.value.state != SttState.READY && stt.state.value.state != SttState.LISTENING) {
            val refusedState = stt.state.value.state
            stt.unload()
            return record("", null, VoiceTurnRecord.ROUTE_NONE, VoiceTurnRecord.ATTRIBUTION_NONE, null, false, "STT_$refusedState", VoicePipelineState.FAILED)
        }

        val source = sourceFactory()
        activeSource = source
        source.start()
        // G4-C02: a typed microphone failure (permission/busy/init) must end the
        // turn truthfully — never as NO_SPEECH, which would claim capture ran.
        (source as? MicDiagnostics)?.lastFailure?.let { micFailure ->
            return record(
                "", null, VoiceTurnRecord.ROUTE_NONE, VoiceTurnRecord.ATTRIBUTION_NONE,
                null, false, "MIC_${micFailure.kind.name}", VoicePipelineState.FAILED,
            )
        }
        _state.value = VoicePipelineState.LISTENING
        var speechSeen = false
        var sttOutput: SttOutput = stt.state.value

        // ---- Capture phase: frames consumed in-flight, never stored (INV-G4-09).
        while (true) {
            if (_muted.value) {
                source.stop()
                activeSource = null
                return record("", null, VoiceTurnRecord.ROUTE_NONE, VoiceTurnRecord.ATTRIBUTION_NONE, null, false, "MUTED", VoicePipelineState.MUTED)
            }
            if (turnCancelled) {
                return record("", null, VoiceTurnRecord.ROUTE_NONE, VoiceTurnRecord.ATTRIBUTION_NONE, null, false, "CANCELLED", VoicePipelineState.CANCELLED)
            }
            val frame = source.read()
            if (frame == null) break // source stopped (mute or lifecycle)
            val now = System.currentTimeMillis()
            if (vad.isSpeech(frame)) {
                speechSeen = true
                lastSpeechAtMs = now
                _state.value = VoicePipelineState.SPEECH_DETECTED
            }
            sttOutput = stt.accept(frame)
            val elapsed = now - startMs
            if (!speechSeen && elapsed >= noSpeechTimeoutMs) {
                source.stop()
                return record("", null, VoiceTurnRecord.ROUTE_NONE, VoiceTurnRecord.ATTRIBUTION_NONE, null, false, "NO_SPEECH", VoicePipelineState.NO_SPEECH)
            }
            if (elapsed >= maxTurnMs) break
            if (speechSeen && now - lastSpeechAtMs >= END_OF_SPEECH_QUIET_MS) break
            delay(framePollDelayMs)
        }
        source.stop()
        if (activeSource === source) activeSource = null

        if (_muted.value) {
            return record("", null, VoiceTurnRecord.ROUTE_NONE, VoiceTurnRecord.ATTRIBUTION_NONE, null, false, "MUTED", VoicePipelineState.MUTED)
        }
        if (turnCancelled) {
            return record("", null, VoiceTurnRecord.ROUTE_NONE, VoiceTurnRecord.ATTRIBUTION_NONE, null, false, "CANCELLED", VoicePipelineState.CANCELLED)
        }
        if (!speechSeen) {
            return record("", null, VoiceTurnRecord.ROUTE_NONE, VoiceTurnRecord.ATTRIBUTION_NONE, null, false, "NO_SPEECH", VoicePipelineState.NO_SPEECH)
        }

        // ---- Finalize transcript.
        _state.value = VoicePipelineState.TRANSCRIBING
        sttOutput = stt.finalizeTranscript()
        val transcript = sttOutput.finalTranscript ?: sttOutput.partialTranscript.orEmpty()
        if (transcript.isBlank() || sttOutput.state == SttState.FAILED) {
            val code = sttOutput.error?.let { "STT_${it.kind.name}" } ?: "STT_EMPTY"
            return record(transcript, sttOutput.confidence, VoiceTurnRecord.ROUTE_NONE, VoiceTurnRecord.ATTRIBUTION_NONE, null, false, code, VoicePipelineState.FAILED)
        }

        // ---- Route with the deterministic router (§8: classification before execution).
        _state.value = VoicePipelineState.ROUTING
        val decision = router.route(
            RoutingRequest(
                text = transcript,
                requestedMode = modeProvider(),
                effectiveMode = modeProvider(),
                connectivity = connectivityProvider(),
                sttConfidence = sttOutput.confidence ?: 1f,
            ),
        )

        // ---- Execute with truthful attribution (§11: no silent routing ambiguity).
        _state.value = VoicePipelineState.RESPONDING
        var route = VoiceTurnRecord.ROUTE_NONE
        var attribution = VoiceTurnRecord.ATTRIBUTION_NONE
        var errorCode: String? = null
        val response: String? = when (decision.action) {
            RoutingAction.HANDLE_LOCAL -> {
                val local = localExecutor(transcript)
                if (local != null) {
                    route = VoiceTurnRecord.ROUTE_LOCAL
                    attribution = VoiceTurnRecord.ATTRIBUTION_LOCAL
                    local
                } else {
                    // Local executor could not handle: honest fallback to Tower.
                    val tower = towerExecutor(transcript)
                    if (tower != null) {
                        route = VoiceTurnRecord.ROUTE_TOWER
                        attribution = VoiceTurnRecord.ATTRIBUTION_TOWER
                    } else {
                        errorCode = "LOCAL_REFUSED_TOWER_UNAVAILABLE"
                    }
                    tower
                }
            }
            RoutingAction.ROUTE_TOWER, RoutingAction.QUEUE_FOR_TOWER -> {
                val tower = towerExecutor(transcript)
                if (tower != null) {
                    route = VoiceTurnRecord.ROUTE_TOWER
                    attribution = VoiceTurnRecord.ATTRIBUTION_TOWER
                } else {
                    errorCode = if (decision.action == RoutingAction.QUEUE_FOR_TOWER) "TOWER_QUEUED_OFFLINE" else "TOWER_UNAVAILABLE"
                }
                tower
            }
            RoutingAction.DECLINE -> {
                errorCode = "DECLINED_${decision.intent}"
                null
            }
        }

        if (response == null) {
            return record(transcript, sttOutput.confidence, route, attribution, null, false, errorCode, VoicePipelineState.FAILED)
        }

        // ---- Speak with the fallback chain (§10): selected voice → text-only.
        var spoken = false
        val engine = tts
        if (engine != null && !turnCancelled) {
            runCatching {
                engine.load()
                if (engine.state.value.state == TtsState.READY) {
                    engine.speak(response)
                    spoken = engine.state.value.state == TtsState.SPEAKING || engine.state.value.spokenText == response
                }
            }
        }
        val endState = if (spoken) VoicePipelineState.SPEAKING else VoicePipelineState.IDLE
        return record(transcript, sttOutput.confidence, route, attribution, response, spoken, errorCode, endState)
    }

    /** Barge-in / cancel: stops capture, interrupts speech, ends the turn. */
    fun cancel() {
        turnCancelled = true
        activeSource?.stop()
        activeSource = null
        tts?.interrupt()
        _state.value = VoicePipelineState.CANCELLED
    }

    companion object {
        /** Quiet time after the last speech frame that ends the capture phase. */
        const val END_OF_SPEECH_QUIET_MS = 700L
        const val NO_SPEECH_MIN_MS = 3_000L
        const val NO_SPEECH_MAX_MS = 10_000L
    }
}
