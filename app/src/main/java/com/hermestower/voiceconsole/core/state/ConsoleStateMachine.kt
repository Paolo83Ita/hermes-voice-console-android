package com.hermestower.voiceconsole.core.state

/**
 * Authoritative legal-transition table, mirrored exactly from docs/UI_STATE_MACHINE.md §3.
 * Three universal rules complete the table:
 *   1. any state -> DORMANT (power off is always legal)
 *   2. any state except DORMANT -> PRIVACY_MUTED (the Owner can always silence the console)
 *   3. any state except DORMANT -> IDLE (Owner override / barge-in / recovery to rest)
 */
object ConsoleStateMachine {

    private val allowed: Map<ConsoleState, Set<ConsoleState>> = mapOf(
        ConsoleState.DORMANT to setOf(ConsoleState.IDLE),
        ConsoleState.IDLE to setOf(
            ConsoleState.WAKE_DETECTED, ConsoleState.TRANSCRIBING, ConsoleState.LISTENING,
            ConsoleState.PRIVACY_MUTED, ConsoleState.DISCONNECTED, ConsoleState.DEGRADED,
            ConsoleState.ERROR, ConsoleState.UPDATE_AVAILABLE,
        ),
        ConsoleState.WAKE_DETECTED to setOf(
            ConsoleState.LISTENING, ConsoleState.IDLE, ConsoleState.PRIVACY_MUTED,
            ConsoleState.ERROR, ConsoleState.DISCONNECTED,
        ),
        ConsoleState.LISTENING to setOf(
            ConsoleState.TRANSCRIBING, ConsoleState.IDLE, ConsoleState.PRIVACY_MUTED,
            ConsoleState.ERROR, ConsoleState.DISCONNECTED,
        ),
        ConsoleState.TRANSCRIBING to setOf(
            ConsoleState.UNDERSTANDING_LOCAL, ConsoleState.CONTACTING_TOWER, ConsoleState.IDLE,
            ConsoleState.PRIVACY_MUTED, ConsoleState.ERROR, ConsoleState.DISCONNECTED,
        ),
        ConsoleState.UNDERSTANDING_LOCAL to setOf(
            ConsoleState.SPEAKING, ConsoleState.CONTACTING_TOWER, ConsoleState.IDLE,
            ConsoleState.PRIVACY_MUTED, ConsoleState.ERROR,
        ),
        ConsoleState.CONTACTING_TOWER to setOf(
            ConsoleState.TOWER_PROCESSING, ConsoleState.REQUEST_QUEUED, ConsoleState.SPEAKING,
            ConsoleState.DISCONNECTED, ConsoleState.DEGRADED, ConsoleState.ERROR,
            ConsoleState.PRIVACY_MUTED,
        ),
        ConsoleState.TOWER_PROCESSING to setOf(
            ConsoleState.SPEAKING, ConsoleState.REQUEST_QUEUED, ConsoleState.IDLE,
            ConsoleState.DISCONNECTED, ConsoleState.ERROR, ConsoleState.PRIVACY_MUTED,
        ),
        ConsoleState.SPEAKING to setOf(
            ConsoleState.IDLE, ConsoleState.LISTENING, ConsoleState.PRIVACY_MUTED,
            ConsoleState.ERROR, ConsoleState.DISCONNECTED,
        ),
        ConsoleState.REQUEST_QUEUED to setOf(
            ConsoleState.CONTACTING_TOWER, ConsoleState.IDLE, ConsoleState.DISCONNECTED,
            ConsoleState.ERROR, ConsoleState.PRIVACY_MUTED,
        ),
        ConsoleState.DISCONNECTED to setOf(
            ConsoleState.IDLE, ConsoleState.DEGRADED, ConsoleState.CONTACTING_TOWER,
            ConsoleState.REQUEST_QUEUED, ConsoleState.PRIVACY_MUTED,
        ),
        ConsoleState.DEGRADED to setOf(
            ConsoleState.IDLE, ConsoleState.DISCONNECTED, ConsoleState.CONTACTING_TOWER,
            ConsoleState.ERROR, ConsoleState.PRIVACY_MUTED,
        ),
        ConsoleState.ERROR to setOf(
            ConsoleState.IDLE, ConsoleState.DISCONNECTED, ConsoleState.PRIVACY_MUTED,
        ),
        ConsoleState.PRIVACY_MUTED to setOf(ConsoleState.IDLE),
        ConsoleState.UPDATE_AVAILABLE to setOf(ConsoleState.IDLE),
    )

    fun canTransition(from: ConsoleState, to: ConsoleState): Boolean {
        if (from == to) return false
        if (to == ConsoleState.DORMANT) return true
        if (from != ConsoleState.DORMANT && to == ConsoleState.PRIVACY_MUTED) return true
        if (from != ConsoleState.DORMANT && to == ConsoleState.IDLE) return true
        return allowed[from]?.contains(to) == true
    }

    /** Enforcing variant used by the store and driver; illegal moves throw. */
    fun ensure(from: ConsoleState, to: ConsoleState): ConsoleState {
        if (!canTransition(from, to)) throw IllegalConsoleTransitionException(from, to)
        return to
    }

    /** Non-throwing variant for UI affordances that probe legality. */
    fun tryTransition(from: ConsoleState, to: ConsoleState): ConsoleState? =
        if (canTransition(from, to)) to else null

    /** Every state must be able to leave itself somewhere (no dead ends except by design). */
    fun outgoing(state: ConsoleState): Set<ConsoleState> = allowed[state].orEmpty()
}

class IllegalConsoleTransitionException(from: ConsoleState, to: ConsoleState) :
    IllegalStateException("Illegal console state transition: $from -> $to")
