package com.hermestower.voiceconsole.models

/**
 * GRAPH 4.12 §7 — MODEL CAPABILITY STATE MACHINE.
 *
 * PHYS-FAIL-05: downloaded models and voices were not PROVEN to be consumed
 * by the actual runtime. The legacy install ladder stops at INSTALLED/ACTIVE
 * and allows INSTALLED → ACTIVE directly — a UI selection state was being
 * read as runtime evidence.
 *
 * This machine is the RUNTIME-TRUTH ladder (parallel to the download/install
 * ladder in ModelInstallState, never replacing it). Distinct states:
 *
 *   NOT_DOWNLOADED → DOWNLOADING → DOWNLOADED → HASH_VERIFIED → INSTALLED →
 *   RUNTIME_AVAILABLE → LOADED → FUNCTIONAL_TEST_PASSED → ACTIVE →
 *   PHYSICAL_QUALIFICATION_PENDING → QUALIFIED
 *
 * plus the two typed failure states FAILED and QUARANTINED.
 *
 * Hard rules (enforced by [canTransition] and [CapabilityLadder.transition]):
 *   - do NOT skip from INSTALLED to ACTIVE (the jump is refused);
 *   - the UI-selected state is NEVER runtime evidence (selection is an
 *     input to activation, not a state in this machine).
 */
enum class ModelCapabilityState {
    NOT_DOWNLOADED,
    DOWNLOADING,
    DOWNLOADED,
    HASH_VERIFIED,
    INSTALLED,
    RUNTIME_AVAILABLE,
    LOADED,
    FUNCTIONAL_TEST_PASSED,
    ACTIVE,
    PHYSICAL_QUALIFICATION_PENDING,
    QUALIFIED,
    FAILED,
    QUARANTINED,
}

object ModelCapabilityTransitions {

    /** The legal forward ladder + typed exits. Order matters. */
    private val legal: Map<ModelCapabilityState, Set<ModelCapabilityState>> = mapOf(
        ModelCapabilityState.NOT_DOWNLOADED to setOf(
            ModelCapabilityState.DOWNLOADING, ModelCapabilityState.QUARANTINED,
        ),
        ModelCapabilityState.DOWNLOADING to setOf(
            ModelCapabilityState.DOWNLOADED, ModelCapabilityState.FAILED,
        ),
        ModelCapabilityState.DOWNLOADED to setOf(
            ModelCapabilityState.HASH_VERIFIED, ModelCapabilityState.FAILED, ModelCapabilityState.QUARANTINED,
        ),
        ModelCapabilityState.HASH_VERIFIED to setOf(
            ModelCapabilityState.INSTALLED, ModelCapabilityState.FAILED, ModelCapabilityState.QUARANTINED,
        ),
        ModelCapabilityState.INSTALLED to setOf(
            ModelCapabilityState.RUNTIME_AVAILABLE, ModelCapabilityState.FAILED, ModelCapabilityState.QUARANTINED,
        ),
        ModelCapabilityState.RUNTIME_AVAILABLE to setOf(
            ModelCapabilityState.LOADED, ModelCapabilityState.FAILED, ModelCapabilityState.QUARANTINED,
        ),
        ModelCapabilityState.LOADED to setOf(
            ModelCapabilityState.FUNCTIONAL_TEST_PASSED, ModelCapabilityState.RUNTIME_AVAILABLE,
            ModelCapabilityState.FAILED, ModelCapabilityState.QUARANTINED,
        ),
        ModelCapabilityState.FUNCTIONAL_TEST_PASSED to setOf(
            ModelCapabilityState.ACTIVE, ModelCapabilityState.PHYSICAL_QUALIFICATION_PENDING,
            ModelCapabilityState.RUNTIME_AVAILABLE, ModelCapabilityState.QUARANTINED,
        ),
        ModelCapabilityState.ACTIVE to setOf(
            ModelCapabilityState.PHYSICAL_QUALIFICATION_PENDING, ModelCapabilityState.RUNTIME_AVAILABLE,
            ModelCapabilityState.QUARANTINED,
        ),
        ModelCapabilityState.PHYSICAL_QUALIFICATION_PENDING to setOf(
            ModelCapabilityState.QUALIFIED, ModelCapabilityState.FUNCTIONAL_TEST_PASSED,
            ModelCapabilityState.QUARANTINED,
        ),
        ModelCapabilityState.QUALIFIED to setOf(ModelCapabilityState.QUARANTINED),
        ModelCapabilityState.FAILED to setOf(
            ModelCapabilityState.DOWNLOADING, ModelCapabilityState.QUARANTINED,
        ),
        ModelCapabilityState.QUARANTINED to setOf(),
    )

    fun canTransition(from: ModelCapabilityState, to: ModelCapabilityState): Boolean =
        legal[from]?.contains(to) == true

    fun legalTargets(from: ModelCapabilityState): Set<ModelCapabilityState> =
        legal[from] ?: emptySet()

    /** The single refusal reason used everywhere (honest diagnostics). */
    const val REFUSAL_NO_SKIP = "CAPABILITY_LADDER_NO_SKIP"
}
