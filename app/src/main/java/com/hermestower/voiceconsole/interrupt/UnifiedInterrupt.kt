package com.hermestower.voiceconsole.interrupt

import com.hermestower.voiceconsole.core.state.ConsoleState

/**
 * GRAPH 4.12 §13/§14 — UNIFIED INTERRUPT CONTRACT + INTERRUPT RECEIPT.
 *
 * The Owner reported the `Interrompi` control near `Parla` as apparently
 * non-functional (PHYS-FAIL-04). Root shape of the old path: each control
 * called ONE subsystem, so "stop" left capture running while TTS kept
 * speaking (or vice versa) — every partially-cancelled window looks
 * "non-functional" from the outside.
 *
 * The unified control cancels ALL applicable active operations:
 *   - AudioRecord capture;
 *   - STT partial recognition;
 *   - STT finalization;
 *   - continuous-listening current turn;
 *   - queued local response;
 *   - Tower request (when cancellable);
 *   - TTS utterance;
 *   - post-TTS resume timer;
 *   - echo-guard pending state.
 * Not every subsystem is active at once — the receipt shows the per-subsystem
 * outcome honestly (CANCELLED / WAS_INACTIVE / REFUSED).
 */
data class InterruptReceipt(
    val turnId: String,
    val requestedAtMillis: Long,
    val sourceControl: String,
    val microphoneStopResult: SubsystemOutcome,
    val sttCancelResult: SubsystemOutcome,
    val localTurnCancelResult: SubsystemOutcome,
    val bridgeCancelResult: SubsystemOutcome,
    val ttsStopResult: SubsystemOutcome,
    val listeningModeResult: SubsystemOutcome,
    val finalConsoleState: String,
    val durationMillis: Long,
    val typedResidual: String?,
) {
    enum class SubsystemOutcome { CANCELLED, WAS_INACTIVE, REFUSED }

    /**
     * §14 SUCCESS: final state IDLE, or CONTINUOUS_LISTENING_ACTIVE when the
     * policy intentionally resumes listening after the cancellation.
     */
    fun isSuccess(resumedContinuous: Boolean): Boolean =
        finalConsoleState == ConsoleState.IDLE.name ||
            (resumedContinuous && finalConsoleState == "CONTINUOUS_LISTENING_ACTIVE")

    /** Every subsystem that was active must show CANCELLED (never REFUSED). */
    fun hasRefusal(): Boolean = listOf(
        microphoneStopResult, sttCancelResult, localTurnCancelResult,
        bridgeCancelResult, ttsStopResult, listeningModeResult,
    ).any { it == SubsystemOutcome.REFUSED }
}

class UnifiedInterruptController(
    private val newTurnId: () -> String,
    private val clock: () -> Long = System::currentTimeMillis,
    private val stopMicrophone: () -> Boolean,
    private val cancelStt: () -> Boolean,
    private val cancelLocalTurn: () -> Boolean,
    private val cancelBridgeRequest: () -> Boolean,
    private val stopTts: () -> Boolean,
    private val stopListeningMode: () -> Boolean,
    private val clearEchoGuard: () -> Unit = {},
    private val clearPostTtsResumeTimer: () -> Unit = {},
    private val currentConsoleState: () -> ConsoleState,
    private val continuousResumesByPolicy: () -> Boolean = { false },
) {

    /** Idempotent: repeated taps produce honest WAS_INACTIVE outcomes. */
    @Volatile private var lastReceipt: InterruptReceipt? = null

    fun lastReceipt(): InterruptReceipt? = lastReceipt

    fun cancelAllActive(sourceControl: String): InterruptReceipt {
        val started = clock()
        val turnId = newTurnId()

        val mic = outcome(stopMicrophone())
        val stt = outcome(cancelStt())
        val local = outcome(cancelLocalTurn())
        val bridge = outcome(cancelBridgeRequest())
        val tts = outcome(stopTts())
        val listening = outcome(stopListeningMode())

        // post-TTS resume timer + echo-guard pending state: auxiliary clears.
        clearPostTtsResumeTimer()
        clearEchoGuard()

        val resumed = continuousResumesByPolicy()
        val finalState = when {
            resumed -> "CONTINUOUS_LISTENING_ACTIVE"
            else -> currentConsoleState().name
        }
        val receipt = InterruptReceipt(
            turnId = turnId,
            requestedAtMillis = started,
            sourceControl = sourceControl,
            microphoneStopResult = mic,
            sttCancelResult = stt,
            localTurnCancelResult = local,
            bridgeCancelResult = bridge,
            ttsStopResult = tts,
            listeningModeResult = listening,
            finalConsoleState = finalState,
            durationMillis = clock() - started,
            typedResidual = null,
        )
        lastReceipt = receipt
        return receipt
    }

    private fun outcome(cancelled: Boolean): InterruptReceipt.SubsystemOutcome =
        when (cancelled) {
            true -> InterruptReceipt.SubsystemOutcome.CANCELLED
            false -> InterruptReceipt.SubsystemOutcome.WAS_INACTIVE
        }

    companion object {
        /** §15 — state-specific interrupt label ids (never one ambiguous label). */
        const val LABEL_LISTENING = "interrupt_stop_listening"
        const val LABEL_TRANSCRIBING = "interrupt_cancel_transcription"
        const val LABEL_PROCESSING = "interrupt_cancel_request"
        const val LABEL_SPEAKING = "interrupt_stop_voice"
        const val LABEL_CONTINUOUS = "interrupt_stop_continuous"

        /**
         * §15 UI state resolution (pure, JVM-testable): which label applies,
         * and is there anything cancellable at all?
         */
        data class InterruptUi(
            val labelId: String,
            val enabled: Boolean,
            val cancellable: Boolean,
        )

        fun resolve(
            consoleState: ConsoleState,
            micListening: Boolean,
            continuousActive: Boolean,
        ): InterruptUi = when {
            continuousActive -> InterruptUi(LABEL_CONTINUOUS, enabled = true, cancellable = true)
            micListening || consoleState == ConsoleState.LISTENING ->
                InterruptUi(LABEL_LISTENING, enabled = true, cancellable = true)
            consoleState == ConsoleState.TRANSCRIBING ->
                InterruptUi(LABEL_TRANSCRIBING, enabled = true, cancellable = true)
            consoleState == ConsoleState.UNDERSTANDING_LOCAL ||
                consoleState == ConsoleState.CONTACTING_TOWER ||
                consoleState == ConsoleState.TOWER_PROCESSING ||
                consoleState == ConsoleState.REQUEST_QUEUED ->
                InterruptUi(LABEL_PROCESSING, enabled = true, cancellable = true)
            consoleState == ConsoleState.SPEAKING ->
                InterruptUi(LABEL_SPEAKING, enabled = true, cancellable = true)
            else -> InterruptUi(
                labelId = LABEL_LISTENING,
                enabled = false,
                cancellable = false,
            ) // idle: disabled state, clearly shown
        }
    }
}
