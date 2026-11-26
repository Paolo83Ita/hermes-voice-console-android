package com.hermestower.voiceconsole.core.routing

import com.hermestower.voiceconsole.core.mode.OperatingMode

/**
 * ANDROID REPAIR CP-008 — the ROUTING MODE POLICY (mission section 10,
 * binding). The five charter routing modes with their defaults and the
 * per-decision exposure contract.
 *
 * Modes:
 *  - TOWER_FIRST (default when PAIRED): every request goes to the paired
 *    Tower; the local lane answers ONLY the bounded low-risk classes when
 *    the Tower is unreachable (honest fallback, labeled);
 *  - HYBRID: the router decides per intent (whitelisted local intents local,
 *    everything else Tower) — the Graph-2 HYBRID OperatingMode semantics;
 *  - LOCAL_ONLY: only the local lane; Tower authority requests are DECLINED
 *    typed (never silently local-answered);
 *  - TOWER_ONLY: everything to the Tower, no local answering;
 *  - OFFLINE_SAFE (default when UNPAIRED): bounded local answers only;
 *    consequential requests are declined typed with prerequisites; nothing
 *    is presented as Tower work.
 *
 * Tower authority is REQUIRED for: mission planning, external tools,
 * artifacts, persistent Tower actions, security decisions, claims about live
 * Tower state, consequential actions. The RuleBasedIntentRouter already
 * enforces the consequential whitelist — this policy maps the mission modes
 * onto the effective operating mode and exposes the trace.
 */
enum class RoutingMode {
    TOWER_FIRST, HYBRID, LOCAL_ONLY, TOWER_ONLY, OFFLINE_SAFE,
}

object RoutingModePolicy {

    /** Default mode per pairing state (mission section 10). */
    fun defaultMode(paired: Boolean): RoutingMode = if (paired) RoutingMode.TOWER_FIRST else RoutingMode.OFFLINE_SAFE

    /** Resolves the EFFECTIVE routing mode from the Owner-requested operating mode. */
    fun effectiveMode(requested: OperatingMode, paired: Boolean): RoutingMode = when (requested) {
        OperatingMode.OFF -> RoutingMode.OFFLINE_SAFE // OFF never routes anywhere: the pipeline declines
        OperatingMode.LOCAL -> RoutingMode.LOCAL_ONLY
        OperatingMode.TOWER -> if (paired) RoutingMode.TOWER_ONLY else RoutingMode.OFFLINE_SAFE
        OperatingMode.HYBRID -> if (paired) RoutingMode.TOWER_FIRST else RoutingMode.OFFLINE_SAFE
        OperatingMode.DISCONNECTED_SAFE -> RoutingMode.OFFLINE_SAFE
    }

    /** The bounded local-risk classes Qwen/the rule lane may answer (mission section 10). */
    val BOUNDED_LOCAL_CLASSES = setOf(
        "GREETING", "HELP", "DEVICE_STATUS", "IDENTITY", "TIME_DATE", "THANKS",
    )

    /** Intent classes that REQUIRE Tower authority — never answered locally. */
    val TOWER_AUTHORITY_CLASSES = setOf(
        "MISSION_PLANNING", "EXTERNAL_TOOLS", "ARTIFACTS", "PERSISTENT_TOWER_ACTION",
        "SECURITY_DECISION", "LIVE_TOWER_STATE", "CONSEQUENTIAL",
    )

    fun isTowerAuthorityRequired(intent: String): Boolean = intent in TOWER_AUTHORITY_CLASSES || intent !in BOUNDED_LOCAL_CLASSES
}

/**
 * The per-decision trace every route exposes (mission section 10): requested
 * mode, effective mode, reason, executor, fallback, confidence, Tower
 * connection state, local runtime state. Secret-clean by construction.
 */
data class RouteDecisionTrace(
    val requestedMode: String,
    val effectiveMode: String,
    val reason: String,
    val executor: String,
    val fallback: String?,
    val confidence: Float?,
    val towerConnectionState: String,
    val localRuntimeState: String,
    val timestampMs: Long = System.currentTimeMillis(),
)

/** Bounded in-memory trace log for the console diagnostics surface (last N decisions). */
class RouteDecisionLog(private val capacity: Int = 32) {
    private val entries = ArrayDeque<RouteDecisionTrace>()

    @Synchronized
    fun record(trace: RouteDecisionTrace) {
        entries.addLast(trace)
        while (entries.size > capacity) entries.removeFirst()
    }

    @Synchronized
    fun snapshot(): List<RouteDecisionTrace> = entries.toList()

    @Synchronized
    fun last(): RouteDecisionTrace? = entries.lastOrNull()
}
