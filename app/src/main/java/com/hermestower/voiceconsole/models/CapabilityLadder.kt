package com.hermestower.voiceconsole.models

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * In-memory capability ladder per component identity (Graph 4.12 §7).
 * Persistence of the ladder rides the existing InstallationRegistry note
 * channel (`capability=<STATE>`); this class is the ENFORCER, not the store.
 */
class CapabilityLadder(
    private val persist: (identity: String, state: ModelCapabilityState) -> Unit = { _, _ -> },
) {

    data class TransitionResult(
        val accepted: Boolean,
        val from: ModelCapabilityState,
        val to: ModelCapabilityState,
        val refusal: String? = null,
    )

    private val mutex = Mutex()
    private val states = HashMap<String, ModelCapabilityState>()

    fun stateOf(identity: String): ModelCapabilityState =
        states[identity] ?: ModelCapabilityState.NOT_DOWNLOADED

    suspend fun transition(identity: String, to: ModelCapabilityState): TransitionResult =
        mutex.withLock {
            val from = stateOf(identity)
            if (from == to) return TransitionResult(true, from, to, null)
            if (!ModelCapabilityTransitions.canTransition(from, to)) {
                return TransitionResult(
                    false, from, to,
                    ModelCapabilityTransitions.REFUSAL_NO_SKIP + ": " + from.name + " → " + to.name,
                )
            }
            states[identity] = to
            persist(identity, to)
            TransitionResult(true, from, to, null)
        }

    /** Restores a persisted ladder (startup read-back — never invents states). */
    fun restore(identity: String, state: ModelCapabilityState) {
        states[identity] = state
    }
}
