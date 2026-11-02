package com.hermestower.voiceconsole.simulator

import com.hermestower.voiceconsole.bridge.BridgeFault
import com.hermestower.voiceconsole.bridge.LocalRoutingHint
import com.hermestower.voiceconsole.bridge.MockTowerBridge
import com.hermestower.voiceconsole.bridge.TowerEventFrame
import com.hermestower.voiceconsole.bridge.TowerProtocol
import com.hermestower.voiceconsole.bridge.TowerRequestFrame
import com.hermestower.voiceconsole.core.mode.OperatingMode
import com.hermestower.voiceconsole.core.routing.Connectivity
import com.hermestower.voiceconsole.core.routing.RoutingAction
import com.hermestower.voiceconsole.core.routing.RoutingRequest
import com.hermestower.voiceconsole.core.routing.RoutingDecision
import com.hermestower.voiceconsole.core.routing.IntentRouter
import com.hermestower.voiceconsole.core.state.ConsoleState
import com.hermestower.voiceconsole.core.state.ConsoleStateStore
import com.hermestower.voiceconsole.core.state.ConnectionStatus
import com.hermestower.voiceconsole.core.state.ConversationTurn
import com.hermestower.voiceconsole.domain.LocalConversationEngine
import com.hermestower.voiceconsole.domain.LocalEventStore
import com.hermestower.voiceconsole.domain.SessionController
import com.hermestower.voiceconsole.domain.TextToSpeechEngine
import com.hermestower.voiceconsole.domain.TowerBridge
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Instant
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

/**
 * V0.1 voice-loop orchestration (ARCHITECTURE.md §3): wake → listen → transcribe →
 * route → local/tower/queue/decline → speak → idle, using the REAL router, bridge
 * contract, TTS abstraction and state machine — only the acoustic front-end is
 * simulated by text injection (FR-10, D-006, D-014).
 *
 * PRIVACY RULE: transcripts are never written to the event log; only lengths,
 * decisions and reasons are (SEC section 4, "no secret speech in logs").
 */
class ScriptedConsoleDriver(
    private val scope: kotlinx.coroutines.CoroutineScope,
    private val store: ConsoleStateStore,
    private val router: IntentRouter,
    private val localEngine: LocalConversationEngine,
    private val bridge: TowerBridge,
    private val tts: TextToSpeechEngine,
    private val events: LocalEventStore,
    private val session: SessionController,
    private val statusProvider: () -> String,
    private val deviceId: String = "console-hermes-01",
    private val userIdRef: String = "owner-paolo",
) {

    private var pipelineJob: Job? = null
    private val requestCounter = AtomicInteger(0)
    private val wakeIndex = AtomicLong(0)

    /** Fixed demo utterances (deterministic, D-014) cycling on each simulated wake. */
    val wakeUtterances = listOf(
        "good day hermes",
        "prepare tomorrow mission plan",
        "status report",
        "who are you",
        "send the weekly report to the archive",
    )

    fun start() {
        scope.launch {
            bridge.connection.collect { connection ->
                val previous = store.connection.value.status
                store.setConnection(connection)
                if (connection.status != previous) {
                    events.log("BRIDGE", "connection $previous -> ${connection.status}")
                    if (connection.status == ConnectionStatus.BridgeStatus.CONNECTED) {
                        // CP-G4-007 attempt-3 diagnostics: record the exact guard
                        // inputs at the flush trigger (counts/states only — no
                        // transcript content ever reaches the event log).
                        events.log(
                            "FLUSH", "trigger state=${store.currentState()} " +
                                "active=${store.currentState().isPipelineActive} " +
                                "queued=${store.queuedTurns().size}",
                        )
                        try {
                            flushQueuedIfAny()
                        } catch (e: kotlinx.coroutines.CancellationException) {
                            throw e
                        } catch (t: Throwable) {
                            // A failed flush must NEVER kill the connection collector:
                            // a dead collector silently drops every later flush trigger
                            // (the run-41503065076 evidence class). Typed log, truthful.
                            events.log("FLUSH", "error ${t.javaClass.simpleName}")
                        }
                    }
                }
            }
        }
        scope.launch {
            store.state.collect { state ->
                if (state == ConsoleState.DISCONNECTED) store.recomputeEffectiveMode()
            }
        }
        events.log("DRIVER", "started (V0.1 simulation; no microphone)")
    }

    /** Power-on: DORMANT -> IDLE, then a bridge handshake against the mock. */
    fun boot() {
        store.transition(ConsoleState.IDLE) ?: events.log("DRIVER", "boot transition rejected from ${store.currentState()}")
        scope.launch {
            when (val r = bridge.handshake()) {
                is com.hermestower.voiceconsole.domain.BridgeResult.Success ->
                    events.log("BRIDGE", "handshake ok")
                is com.hermestower.voiceconsole.domain.BridgeResult.Failure ->
                    events.log("BRIDGE", "handshake failed: ${r.error} retryable=${r.retryable}")
            }
        }
    }

    /** Simulated wake word. Ignored honestly while privacy muted (FR-03). */
    fun injectWake() {
        if (store.privacyMuted.value) {
            store.setStatusNote("Wake ignored: privacy mute is engaged.")
            events.log("WAKE", "ignored while privacy muted")
            return
        }
        if (store.currentState().isPipelineActive) return
        pipelineJob?.cancel()
        pipelineJob = scope.launch {
            if (store.transition(ConsoleState.WAKE_DETECTED) == null) return@launch
            events.log("WAKE", "detected (simulated)")
            delay(700)
            if (store.transition(ConsoleState.LISTENING) == null) return@launch
            delay(1600)
            val utterance = wakeUtterances[(wakeIndex.getAndIncrement() % wakeUtterances.size).toInt()]
            runPipeline(utterance, sttConfidence = 0.92f)
        }
    }

    /** Text injection test harness standing in for STT (V0.1 only). */
    fun submitText(text: String) {
        if (text.isBlank()) return
        if (store.privacyMuted.value) {
            store.setStatusNote("Request ignored: privacy mute is engaged.")
            events.log("TURN", "ignored while privacy muted")
            return
        }
        if (store.currentState() == ConsoleState.DORMANT) return
        pipelineJob?.cancel()
        tts.interrupt()
        // Owner override: return to rest first (universal rule 3), then enter the pipeline.
        store.transition(ConsoleState.IDLE)
        pipelineJob = scope.launch { runPipeline(text.trim(), sttConfidence = 0.9f) }
    }

    /**
     * GRAPH 4 G4-C03: a REAL user utterance (typed or recognized from speech)
     * enters the pipeline here — one single routing/attribution/history path
     * for voice and text (Owner binding delta §1/§13).
     */
    fun submitUtterance(text: String, sttConfidence: Float) {
        if (text.isBlank()) return
        if (store.privacyMuted.value) {
            store.setStatusNote("Request ignored: privacy mute is engaged.")
            events.log("TURN", "ignored while privacy muted")
            return
        }
        if (store.currentState() == ConsoleState.DORMANT) return
        pipelineJob?.cancel()
        tts.interrupt()
        store.transition(ConsoleState.IDLE)
        pipelineJob = scope.launch { runPipeline(text.trim(), sttConfidence = sttConfidence) }
    }

    /** GRAPH 4 G4-C03 (§5): repeat the last spoken response on demand. */
    fun repeatLastSpoken() {
        if (store.currentState() == ConsoleState.DORMANT) return
        val last = store.turns.value.lastOrNull {
            it.role == ConversationTurn.Role.HERMES && it.delivery == ConversationTurn.Delivery.DELIVERED
        }
        if (last == null) {
            store.setStatusNote("Nothing has been spoken yet.")
            return
        }
        pipelineJob?.cancel()
        tts.interrupt()
        store.transition(ConsoleState.IDLE)
        pipelineJob = scope.launch {
            // Legal path to SPEAKING: IDLE→TRANSCRIBING→UNDERSTANDING_LOCAL→SPEAKING
            // (the state machine has no IDLE→SPEAKING shortcut by design).
            if (store.transition(ConsoleState.TRANSCRIBING) == null) return@launch
            if (store.transition(ConsoleState.UNDERSTANDING_LOCAL) == null) {
                store.transition(ConsoleState.IDLE)
                return@launch
            }
            speakAndFinish(last.text, store.settings.value.captionsEnabled)
        }
    }

    /** GRAPH 4 G4-C03 (§5): stop listening, cancel the current turn, interrupt speech. */
    fun cancelCurrentTurn() {
        pipelineJob?.cancel()
        pipelineJob = null
        tts.interrupt()
        if (store.currentState() != ConsoleState.IDLE && store.currentState() != ConsoleState.DORMANT) {
            store.transition(ConsoleState.IDLE)
        }
        store.setSpeakingAmplitude(0f)
        events.log("TURN", "cancelled by Owner")
    }

    fun setPrivacyMuted(muted: Boolean) {
        store.setPrivacyMuted(muted)
        if (muted) {
            pipelineJob?.cancel()
            tts.interrupt()
            val reached = store.transition(ConsoleState.PRIVACY_MUTED)
            events.log("MUTE", "engaged (transition to ${reached ?: "REJECTED"})")
        } else {
            val reached = store.transition(ConsoleState.IDLE)
            events.log("MUTE", "released (transition to ${reached ?: "REJECTED"})")
        }
    }

    fun stopSpeaking() {
        tts.interrupt()
        if (store.currentState() == ConsoleState.SPEAKING) {
            store.transition(ConsoleState.IDLE)
            store.setSpeakingAmplitude(0f)
            events.log("TTS", "stopped by Owner")
        }
    }

    fun setRequestedMode(mode: OperatingMode) {
        store.setRequestedMode(mode)
        events.log("MODE", "requested ${mode.name}")
    }

    fun setFault(fault: BridgeFault) {
        (bridge as? MockTowerBridge)?.setFault(fault)
        events.log("SIM", "fault ${fault}")
        if (fault == BridgeFault.DISCONNECT_AT_ACK) {
            store.transition(ConsoleState.DISCONNECTED)
            store.recomputeEffectiveMode()
        }
    }

    fun clearFault() {
        (bridge as? MockTowerBridge)?.setFault(BridgeFault.NONE)
        events.log("SIM", "fault cleared")
        if (store.currentState() == ConsoleState.DISCONNECTED ||
            store.connection.value.status == ConnectionStatus.BridgeStatus.DISCONNECTED
        ) {
            scope.launch { bridge.handshake() }
        }
    }

    fun simulateUpdateAvailable() {
        val reached = store.transition(ConsoleState.UPDATE_AVAILABLE)
        events.log("SIM", "update available shown: $reached")
    }

    private suspend fun runPipeline(text: String, sttConfidence: Float) {
        val ownerTurn = store.appendTurn(
            role = ConversationTurn.Role.OWNER,
            text = text,
            source = ConversationTurn.Source.LOCAL,
            delivery = ConversationTurn.Delivery.PENDING,
        )
        // Privacy: never log transcript content, only shape (SEC §4).
        events.log("TURN", "submit len=${text.length} conf=$sttConfidence")
        store.setTranscriptDraft(text)
        store.setStatusNote(null)

        if (store.transition(ConsoleState.TRANSCRIBING) == null) {
            store.updateTurn(ownerTurn.id) { it.copy(delivery = ConversationTurn.Delivery.FAILED, note = "console busy") }
            return
        }
        delay(STT_SIMULATION_MS)

        val connectivity = when (store.connection.value.status) {
            ConnectionStatus.BridgeStatus.CONNECTED -> Connectivity.OK
            ConnectionStatus.BridgeStatus.CONNECTING -> Connectivity.DEGRADED
            else -> Connectivity.DISCONNECTED
        }
        val decision: RoutingDecision = router.route(
            RoutingRequest(
                text = text,
                requestedMode = store.requestedMode.value,
                effectiveMode = store.effectiveMode.value,
                connectivity = connectivity,
                sttConfidence = sttConfidence,
            ),
        )
        events.log("ROUTE", "${decision.action} intent=${decision.intent} consequential=${decision.consequential} reason=${decision.reason}")

        when (decision.action) {
            RoutingAction.HANDLE_LOCAL -> {
                store.updateTurn(ownerTurn.id) { it.copy(source = ConversationTurn.Source.LOCAL) }
                store.transition(ConsoleState.UNDERSTANDING_LOCAL)
                delay(LOCAL_THINK_MS)
                val reply = localEngine.respond(text, statusProvider)
                store.appendTurn(ConversationTurn.Role.HERMES, reply.text, ConversationTurn.Source.LOCAL, ConversationTurn.Delivery.DELIVERED, note = reply.intent)
                speakAndFinish(reply.text, captions = store.settings.value.captionsEnabled)
            }
            RoutingAction.ROUTE_TOWER -> {
                store.updateTurn(ownerTurn.id) { it.copy(source = ConversationTurn.Source.TOWER) }
                towerExchange(text, decision, ownerTurn.id)
            }
            RoutingAction.QUEUE_FOR_TOWER -> {
                store.updateTurn(ownerTurn.id) {
                    it.copy(source = ConversationTurn.Source.TOWER, delivery = ConversationTurn.Delivery.QUEUED)
                }
                store.setQueueCount(store.queuedTurns().size)
                store.transition(ConsoleState.REQUEST_QUEUED)
                events.log("QUEUE", "queued total=${store.queuedTurns().size}")
                delay(QUEUED_DISPLAY_MS)
                store.transition(ConsoleState.IDLE)
            }
            RoutingAction.DECLINE -> {
                val message = "I cannot do that. ${decision.reason}"
                store.updateTurn(ownerTurn.id) {
                    it.copy(
                        delivery = ConversationTurn.Delivery.DELIVERED,
                        note = "declined: ${decision.intent}",
                    )
                }
                store.transition(ConsoleState.UNDERSTANDING_LOCAL)
                delay(LOCAL_THINK_MS)
                store.appendTurn(ConversationTurn.Role.HERMES, message, ConversationTurn.Source.LOCAL, ConversationTurn.Delivery.DELIVERED, note = decision.intent)
                speakAndFinish(message, captions = store.settings.value.captionsEnabled)
            }
        }
    }

    private suspend fun towerExchange(text: String, decision: RoutingDecision, ownerTurnId: Long) {
        if (store.transition(ConsoleState.CONTACTING_TOWER) == null) {
            store.updateTurn(ownerTurnId) { it.copy(delivery = ConversationTurn.Delivery.FAILED, note = "console busy") }
            return
        }
        val frame = TowerRequestFrame(
            deviceId = deviceId,
            userIdRef = userIdRef,
            sessionId = session.current().sessionId,
            conversationId = session.current().conversationId,
            requestId = "req-${"%06d".format(requestCounter.incrementAndGet())}",
            transcript = text,
            language = "it-IT",
            confidence = decision.confidence,
            requestedMode = store.requestedMode.value.name,
            effectiveMode = store.effectiveMode.value.name,
            localRouting = LocalRoutingHint(
                decision = decision.action.name,
                reason = decision.reason,
                candidateIntent = decision.intent,
                consequential = decision.consequential,
            ),
            timestamp = Instant.now().toString(),
        )
        val buffer = StringBuilder()
        try {
            bridge.sendRequest(frame).collect { event ->
                when (event.type) {
                    TowerEventFrame.ACK -> {
                        store.transition(ConsoleState.TOWER_PROCESSING)
                        store.setStatusNote("Tower accepted the request.")
                    }
                    TowerEventFrame.PROGRESS -> store.setStatusNote("Tower: ${event.stage?.lowercase() ?: "working"}")
                    TowerEventFrame.DELTA -> {
                        if (event.deltaText != null) buffer.append(event.deltaText)
                        store.setCaption(buffer.toString())
                    }
                    TowerEventFrame.FINAL -> {
                        val finalText = event.finalText ?: buffer.toString()
                        store.updateTurn(ownerTurnId) {
                            it.copy(delivery = ConversationTurn.Delivery.DELIVERED, receiptRef = event.receiptRef)
                        }
                        store.appendTurn(
                            ConversationTurn.Role.HERMES, finalText, ConversationTurn.Source.TOWER,
                            ConversationTurn.Delivery.DELIVERED, receiptRef = event.receiptRef,
                        )
                        store.setStatusNote("Receipt ${event.receiptRef ?: "pending"}")
                        speakAndFinish(finalText, captions = store.settings.value.captionsEnabled)
                    }
                    TowerEventFrame.ERROR -> {
                        val err = event.error
                        val classification = err?.classification ?: "UNKNOWN"
                        val retryable = err?.retryable ?: false
                        events.log("TOWER_ERROR", "classification=$classification retryable=$retryable")
                        if (retryable && !decision.consequential) {
                            store.updateTurn(ownerTurnId) {
                                it.copy(delivery = ConversationTurn.Delivery.QUEUED, note = "transport failure; queued for retry")
                            }
                            store.setQueueCount(store.queuedTurns().size)
                            store.transition(ConsoleState.REQUEST_QUEUED)
                            delay(QUEUED_DISPLAY_MS)
                            store.transition(ConsoleState.IDLE)
                        } else {
                            store.updateTurn(ownerTurnId) {
                                it.copy(delivery = ConversationTurn.Delivery.FAILED, note = "$classification: ${err?.message ?: "error"}")
                            }
                            store.appendTurn(
                                ConversationTurn.Role.HERMES,
                                "The request failed at the Tower ($classification). ${err?.message ?: ""}".trim(),
                                ConversationTurn.Source.TOWER, ConversationTurn.Delivery.FAILED, note = classification,
                            )
                            store.transition(ConsoleState.ERROR)
                            store.setStatusNote(if (retryable) "Retry available for non-consequential requests." else "Non-retryable Tower error.")
                            delay(ERROR_DISPLAY_MS)
                            store.transition(ConsoleState.IDLE)
                        }
                    }
                }
            }
        } catch (e: CancellationException) {
            store.updateTurn(ownerTurnId) { it.copy(note = it.note ?: "cancelled by Owner") }
            throw e
        }
    }

    private suspend fun flushQueuedIfAny() {
        val queued = store.queuedTurns()
        if (queued.isEmpty()) {
            events.log("FLUSH", "skip: queue empty")
            return
        }
        if (store.currentState().isPipelineActive) {
            events.log("FLUSH", "skip: pipeline active state=${store.currentState()}")
            return
        }
        events.log("FLUSH", "begin n=${queued.size}")
        store.transition(ConsoleState.REQUEST_QUEUED)
        for (turn in queued) {
            if (store.transition(ConsoleState.CONTACTING_TOWER) == null) break
            events.log("QUEUE", "flush turn=${turn.id}")
            store.updateTurn(turn.id) { it.copy(delivery = ConversationTurn.Delivery.PENDING) }
            store.setQueueCount(store.queuedTurns().size)
            val frame = TowerRequestFrame(
                deviceId = deviceId,
                userIdRef = userIdRef,
                sessionId = session.current().sessionId,
                conversationId = session.current().conversationId,
                requestId = "req-${"%06d".format(requestCounter.incrementAndGet())}",
                transcript = turn.text,
                language = "it-IT",
                confidence = 0.9f,
                requestedMode = store.requestedMode.value.name,
                effectiveMode = store.effectiveMode.value.name,
                localRouting = LocalRoutingHint("FLUSH", "queued while offline", null, false),
                timestamp = Instant.now().toString(),
            )
            var delivered = false
            bridge.sendRequest(frame).collect { event ->
                when (event.type) {
                    TowerEventFrame.ACK -> store.transition(ConsoleState.TOWER_PROCESSING)
                    TowerEventFrame.FINAL -> {
                        store.updateTurn(turn.id) {
                            it.copy(delivery = ConversationTurn.Delivery.DELIVERED, receiptRef = event.receiptRef)
                        }
                        store.appendTurn(
                            ConversationTurn.Role.HERMES, event.finalText ?: "", ConversationTurn.Source.TOWER,
                            ConversationTurn.Delivery.DELIVERED, receiptRef = event.receiptRef, note = "flushed from queue",
                        )
                        delivered = true
                    }
                    TowerEventFrame.ERROR -> {
                        store.updateTurn(turn.id) {
                            it.copy(delivery = ConversationTurn.Delivery.FAILED, note = event.error?.message ?: "flush failed")
                        }
                        store.transition(ConsoleState.ERROR)
                    }
                }
            }
            if (delivered) store.transition(ConsoleState.IDLE)
            store.setQueueCount(store.queuedTurns().size)
        }
        store.transition(ConsoleState.IDLE)
    }

    private suspend fun speakAndFinish(text: String, captions: Boolean) {
        if (store.transition(ConsoleState.SPEAKING) == null) {
            // Console was overridden mid-flight (mute/stop); do not speak over the Owner.
            return
        }
        if (captions) store.setCaption(text)
        tts.speak(text, store.settings.value.speakingRate).collect { amplitude ->
            store.setSpeakingAmplitude(amplitude)
        }
        store.setSpeakingAmplitude(0f)
        store.transition(ConsoleState.IDLE)
        store.setTranscriptDraft(null)
    }

    companion object {
        const val STT_SIMULATION_MS = 450L
        const val LOCAL_THINK_MS = 350L
        const val QUEUED_DISPLAY_MS = 1800L
        const val ERROR_DISPLAY_MS = 2200L
    }
}
