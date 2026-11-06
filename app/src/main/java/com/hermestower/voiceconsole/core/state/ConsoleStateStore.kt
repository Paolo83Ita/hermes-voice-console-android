package com.hermestower.voiceconsole.core.state

import com.hermestower.voiceconsole.core.mode.OperatingMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.time.Instant

/** Attributed conversation turn shown in History (FR-16). */
data class ConversationTurn(
    val id: Long,
    val role: Role,
    val text: String,
    val at: Instant,
    val source: Source,
    val delivery: Delivery,
    val consequential: Boolean = false,
    val receiptRef: String? = null,
    val note: String? = null,
) {
    enum class Role { OWNER, HERMES }
    enum class Source { LOCAL, TOWER }
    enum class Delivery { DELIVERED, PENDING, QUEUED, FAILED }
}

/** Bridge connection truth, mirrored from TowerBridge.connection (FR-23). */
data class ConnectionStatus(
    val status: BridgeStatus = BridgeStatus.UNCONFIGURED,
    val endpointLabel: String = "mock://tower.simulator",
    val tls: TlsStatus = TlsStatus.MOCK_NOT_APPLICABLE,
    val lastHandshakeAt: Instant? = null,
) {
    enum class BridgeStatus { UNCONFIGURED, CONNECTING, CONNECTED, DISCONNECTED }
    enum class TlsStatus { MOCK_NOT_APPLICABLE, UNKNOWN, ACTIVE, FAILED }
}

/** UI preferences held in the store so Compose can collect them; persisted by SettingsRepository. */
data class ConsoleSettings(
    val darkTheme: Boolean = true,
    val followSystemTheme: Boolean = false,
    val reducedMotion: Boolean = false,
    val animationIntensity: Float = 1.0f,
    val burnInGuard: Boolean = true,
    val keepScreenOn: Boolean = true,
    val captionsEnabled: Boolean = true,
    val brightnessScrim: Float = 0f,
    // OWNER PHYSICAL RETEST §5: the speech-profile authority. Defaults are the
    // recommended starting values (0.96 / 0.95 — comfortable, no forced high
    // pitch); safe ranges are enforced at the SpeechProfile boundary.
    val speakingRate: Float = 0.96f,
    val speechPitch: Float = 0.95f,
    /** GRAPH 3: LOW / BALANCED / HIGH visual quality profile (mission §14). */
    val visualQuality: String = "BALANCED",
    /** GRAPH 4.6 §6: optional local LLM — DEFAULT DISABLED (never self-enables). */
    val localLlmEnabled: Boolean = false,
)

/**
 * Single source of truth for the console UI (ARCHITECTURE.md §3).
 * Pure Kotlin: no Android imports, unit-testable, Compose-agnostic.
 * All state changes pass through ConsoleStateMachine.ensure().
 */
class ConsoleStateStore {

    private var turnId = 0L

    private val _state = MutableStateFlow(ConsoleState.DORMANT)
    val state: StateFlow<ConsoleState> = _state.asStateFlow()

    private val _requestedMode = MutableStateFlow(OperatingMode.HYBRID)
    val requestedMode: StateFlow<OperatingMode> = _requestedMode.asStateFlow()

    private val _effectiveMode = MutableStateFlow(OperatingMode.DISCONNECTED_SAFE)
    val effectiveMode: StateFlow<OperatingMode> = _effectiveMode.asStateFlow()

    private val _connection = MutableStateFlow(ConnectionStatus())
    val connection: StateFlow<ConnectionStatus> = _connection.asStateFlow()

    private val _privacyMuted = MutableStateFlow(false)
    val privacyMuted: StateFlow<Boolean> = _privacyMuted.asStateFlow()

    private val _turns = MutableStateFlow<List<ConversationTurn>>(emptyList())
    val turns: StateFlow<List<ConversationTurn>> = _turns.asStateFlow()

    private val _transcriptDraft = MutableStateFlow<String?>(null)
    val transcriptDraft: StateFlow<String?> = _transcriptDraft.asStateFlow()

    private val _caption = MutableStateFlow<String?>(null)
    val caption: StateFlow<String?> = _caption.asStateFlow()

    private val _speakingAmplitude = MutableStateFlow(0f)
    val speakingAmplitude: StateFlow<Float> = _speakingAmplitude.asStateFlow()

    private val _queueCount = MutableStateFlow(0)
    val queueCount: StateFlow<Int> = _queueCount.asStateFlow()

    private val _statusNote = MutableStateFlow<String?>(null)
    val statusNote: StateFlow<String?> = _statusNote.asStateFlow()

    private val _settings = MutableStateFlow(ConsoleSettings())
    val settings: StateFlow<ConsoleSettings> = _settings.asStateFlow()

    /** Enforced transition. Returns null if the move is illegal (UI never lies by accident). */
    fun transition(to: ConsoleState): ConsoleState? {
        val current = _state.value
        val target = ConsoleStateMachine.tryTransition(current, to) ?: return null
        _state.value = target
        return target
    }

    fun currentState(): ConsoleState = _state.value

    fun setRequestedMode(mode: OperatingMode) {
        _requestedMode.value = mode
        recomputeEffectiveMode()
    }

    /** DISCONNECTED_SAFE overrides the requested mode while the bridge is down (FR-15). */
    fun recomputeEffectiveMode() {
        _effectiveMode.value =
            if (_connection.value.status == ConnectionStatus.BridgeStatus.DISCONNECTED ||
                _state.value == ConsoleState.DISCONNECTED
            ) OperatingMode.DISCONNECTED_SAFE
            else _requestedMode.value
    }

    fun setConnection(status: ConnectionStatus) {
        _connection.value = status
        recomputeEffectiveMode()
    }

    fun setPrivacyMuted(muted: Boolean) {
        _privacyMuted.value = muted
        if (muted) {
            _transcriptDraft.value = null
        }
    }

    fun appendTurn(
        role: ConversationTurn.Role,
        text: String,
        source: ConversationTurn.Source,
        delivery: ConversationTurn.Delivery,
        consequential: Boolean = false,
        receiptRef: String? = null,
        note: String? = null,
    ): ConversationTurn {
        val turn = ConversationTurn(
            id = ++turnId,
            role = role,
            text = text,
            at = Instant.now(),
            source = source,
            delivery = delivery,
            consequential = consequential,
            receiptRef = receiptRef,
            note = note,
        )
        _turns.update { it + turn }
        return turn
    }

    fun updateTurn(turnId: Long, transform: (ConversationTurn) -> ConversationTurn) {
        _turns.update { list -> list.map { if (it.id == turnId) transform(it) else it } }
    }

    fun findTurn(turnId: Long): ConversationTurn? = _turns.value.firstOrNull { it.id == turnId }

    fun queuedTurns(): List<ConversationTurn> =
        _turns.value.filter { it.delivery == ConversationTurn.Delivery.QUEUED }

    fun setTranscriptDraft(text: String?) { _transcriptDraft.value = text }
    fun setCaption(text: String?) { _caption.value = text }
    fun setSpeakingAmplitude(value: Float) { _speakingAmplitude.value = value.coIn(0f, 1f) }
    fun setStatusNote(note: String?) { _statusNote.value = note }

    fun setQueueCount(count: Int) { _queueCount.value = count.coerceAtLeast(0) }

    fun updateSettings(transform: (ConsoleSettings) -> ConsoleSettings) {
        _settings.update(transform)
    }

    fun applySettings(settings: ConsoleSettings) { _settings.value = settings }

    /** Owner-initiated reset (About & Safety). Clears session-visible state, keeps settings identity. */
    fun resetLocalData() {
        _turns.value = emptyList()
        _caption.value = null
        _transcriptDraft.value = null
        _queueCount.value = 0
        turnId = 0
    }

    private fun Float.coIn(min: Float, max: Float) = coerceIn(min, max)
}
