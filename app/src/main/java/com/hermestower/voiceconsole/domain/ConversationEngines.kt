package com.hermestower.voiceconsole.domain

import kotlinx.coroutines.flow.StateFlow

/**
 * Conversation-side engine contracts.
 * The local model is never a second authority (FR-19); its permitted roles are
 * bounded in LOCAL_MODEL_CANDIDATE_PLAN.md §1.
 */

/** Local conversational front-end (mock in V0.1; qualified LLM candidate only after V0.6). */
interface LocalConversationEngine {
    enum class ModelState { NOT_LOADED, LOADING, LOADED, ERROR }
    data class LocalReply(val text: String, val intent: String, val fromModel: Boolean)

    val modelState: StateFlow<ModelState>
    /** Provider of truthful device status text (real values only — no fake telemetry). */
    fun respond(text: String, statusProvider: () -> String): LocalReply
}

/**
 * Text-to-speech abstraction. V0.1: mock with synthesized timing/amplitude.
 * V0.3: Android system TTS fallback; higher voices only after qualification.
 */
interface TextToSpeechEngine {
    enum class State { NOT_READY, READY, SPEAKING }
    val state: StateFlow<State>
    /** Emits a smoothed 0..1 amplitude while speaking; completes when playback ends. */
    fun speak(text: String, rate: Float): kotlinx.coroutines.flow.Flow<Float>
    fun interrupt()
}

/** Session and identity bookkeeping (contract TOWER_BRIDGE_PROPOSED_CONTRACT.md §3). */
interface SessionController {
    data class Session(val sessionId: String, val conversationId: String, val openedAt: java.time.Instant)
    fun current(): Session
    fun renew(): Session
}
