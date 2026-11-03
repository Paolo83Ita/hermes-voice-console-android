package com.hermestower.voiceconsole.ui.runtime

/**
 * CP-G4-016 — CI_LIFECYCLE_PROFILE (OWNER_DECISION_E2E_RECOVERY, directive §2/§8).
 *
 * The Owner authorized an animation-disabled CI test variant SOLELY for lifecycle
 * and navigation qualification. Variant B (CI_LIFECYCLE_PROFILE) disables/freezes
 * every infinite ornamental animation, particle field and continuous frame clock
 * while preserving — by construction, because nothing below touches them:
 *
 *  - the same navigation (NavHost routes unchanged);
 *  - the same state machine (ConsoleStateMachine untouched);
 *  - the same controls (every composable and callback unchanged);
 *  - the same persistence (SettingsRepository untouched);
 *  - the same semantics (labels, contentDescriptions, roles unchanged);
 *  - the same lifecycle (activity/NavHost lifecycle untouched — this is the point:
 *    with no infinite clock the E2E harness can leave the compose clock advancing,
 *    so NavHost transitions complete and back-stack entries reach CREATED);
 *  - the same business logic.
 *
 * The app's ONLY infinite frame-clock consumer is the Oracle Core via
 * [rememberAnimationFrameTimeMs(running = …)] (MeanderBand is static geometry;
 * TowerCore is legacy and not instantiated). The profile therefore flows through
 * the SAME reduced-motion gate the app already supports — the static-frame state
 * the existing model tests pin — and is set ONLY from MainActivity on a debug
 * build when the test harness launches it with EXTRA_CI_LIFECYCLE_PROFILE.
 * Release builds can never set it; ordinary users are unaffected; the normal
 * animated profile remains the physical-Xiaomi qualification target.
 */
object MotionProfile {

    /** TEST_ONLY flag (directive §2 variant B). Never set outside debug builds. */
    @Volatile
    var ciLifecycleProfile: Boolean = false

    /** True when infinite ornamental motion must be frozen (accessibility or CI). */
    val ornamentsFrozen: Boolean
        get() = ciLifecycleProfile
}
