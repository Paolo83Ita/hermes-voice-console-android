package com.hermestower.voiceconsole.core.routing

import com.hermestower.voiceconsole.core.mode.OperatingMode

/** Routing decisions (FR-11..FR-15). Deterministic, auditable, unit-pinned. */
enum class RoutingAction {
    HANDLE_LOCAL, ROUTE_TOWER, QUEUE_FOR_TOWER, DECLINE
}

enum class Connectivity { OK, DEGRADED, DISCONNECTED }

data class RoutingRequest(
    val text: String,
    val requestedMode: OperatingMode,
    val effectiveMode: OperatingMode,
    val connectivity: Connectivity,
    val sttConfidence: Float,
)

data class RoutingDecision(
    val action: RoutingAction,
    val intent: String,
    val confidence: Float,
    val consequential: Boolean,
    val reason: String,
)

/**
 * Router seam. The V0.1 implementation is rule-based; a qualified local model may
 * later assist it behind this same interface (LOCAL_MODEL_CANDIDATE_PLAN.md §1).
 */
interface IntentRouter {
    fun route(request: RoutingRequest): RoutingDecision
}
