package com.hermestower.voiceconsole.selftest

import java.time.Instant

/**
 * Self-Test Center contracts (GRAPH 2 §11). Every test produces a fully typed
 * result; allowed states are closed; no fabricated metrics are ever emitted.
 */
enum class SelfTestMode(val displayLabel: String, val targetSeconds: String) {
    QUICK("Quick", "under 30 s"),
    STANDARD("Standard", "1–3 min"),
    FULL("Full", "up to 10 min (excludes soak tests)"),
    CONNECTIVITY_ONLY("Connectivity only", "under 1 min"),
    VOICE_FOUNDATION_ONLY("Voice foundation only", "under 2 min"),
}

enum class SelfTestState(val displayLabel: String) {
    NOT_RUN("Not run"),
    RUNNING("Running"),
    PASS("Passed"),
    FAIL("Failed"),
    SKIPPED_NOT_APPLICABLE("Skipped — not applicable"),
    BLOCKED_DEPENDENCY("Blocked — dependency"),
    QUALIFICATION_PENDING_DEVICE("Qualification pending device"),
}

enum class SelfTestCategory {
    IDENTITY, CONFIGURATION, SECURITY, TOWER, OPERATING_MODE, PRIVACY,
    STATE_MACHINE, UI_SEMANTICS, PERSISTENCE, MODELS, DIAGNOSTICS, RECOVERY,
}

data class SelfTestResult(
    val testId: String,
    val testName: String,
    val startTime: Instant?,
    val finishTime: Instant?,
    val durationMs: Long,
    val state: SelfTestState,
    val evidence: String,
    val failureType: String? = null,
    val repairSuggestion: String? = null,
    val logsReference: String? = null,
    val environment: String,
    val appVersion: String,
    val commitReference: String? = null,
)

/** A deterministic check executed by the center. Must not touch the network except 127.0.0.1. */
fun interface SelfTestCheck {
    fun run(context: SelfTestContext): SelfTestResult
}

/** Inputs the center provides to every check; tests never reach around it. */
data class SelfTestContext(
    val environment: String,
    val appVersion: String,
    val commitReference: String?,
    val isAndroidRuntime: Boolean,
)

/**
 * Definition of one self-test (ST-01..ST-45). The center runs checks in ID order;
 * each check returns PASS/FAIL/SKIPPED_NOT_APPLICABLE/BLOCKED_DEPENDENCY/
 * QUALIFICATION_PENDING_DEVICE — the center never invents outcomes.
 */
data class SelfTestDefinition(
    val id: String,
    val name: String,
    val category: SelfTestCategory,
    val modes: Set<SelfTestMode>,
    val check: SelfTestCheck,
)
