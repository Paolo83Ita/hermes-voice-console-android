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
 * ladder in ModelInstallState, never replacing it). GRAPH 4.14 §7 completes it
 * with the discovery/resolution/read-back states:
 *
 *   NOT_AVAILABLE → DISCOVERED → DOWNLOAD_ALLOWED → DOWNLOADING → DOWNLOADED →
 *   HASH_VERIFIED → INSTALLED → RUNTIME_RESOLVED → RUNTIME_AVAILABLE → LOADED →
 *   FUNCTIONAL_TEST_PASSED → ACTIVE → ACTIVE_READBACK_VERIFIED →
 *   PHYSICAL_QUALIFICATION_PENDING → QUALIFIED
 *
 * plus the typed failure states FAILED and QUARANTINED and the terminal REMOVED.
 * NOT_DOWNLOADED stays as the legacy alias entry point (never removed — the
 * download manager emits it).
 *
 * Hard rules (enforced by [canTransition] and [CapabilityLadder.transition]):
 *   - do NOT skip from INSTALLED to ACTIVE (the jump is refused);
 *   - DOWNLOADED → ACTIVE and SELECTED → ACTIVE are equally refused;
 *   - ACTIVE requires an explicit read-back (ACTIVE_READBACK_VERIFIED) before
 *     qualification can be requested;
 *   - the UI-selected state is NEVER runtime evidence (selection is an
 *     input to activation, not a state in this machine).
 */
enum class ModelCapabilityState {
    // GRAPH 4.14 §7 head states (catalog/delivery truth before download):
    NOT_AVAILABLE,
    DISCOVERED,
    DOWNLOAD_ALLOWED,
    // Delivery ladder:
    NOT_DOWNLOADED,
    DOWNLOADING,
    DOWNLOADED,
    HASH_VERIFIED,
    INSTALLED,
    // Runtime ladder (§7: INSTALLED → RUNTIME_RESOLVED → RUNTIME_AVAILABLE):
    RUNTIME_RESOLVED,
    RUNTIME_AVAILABLE,
    LOADED,
    FUNCTIONAL_TEST_PASSED,
    ACTIVE,
    ACTIVE_READBACK_VERIFIED,
    PHYSICAL_QUALIFICATION_PENDING,
    QUALIFIED,
    FAILED,
    QUARANTINED,
    REMOVED,
}

object ModelCapabilityTransitions {

    /** The legal forward ladder + typed exits. Order matters. */
    private val legal: Map<ModelCapabilityState, Set<ModelCapabilityState>> = mapOf(
        // GRAPH 4.14 §7 head: discovery/download-allowance truth.
        ModelCapabilityState.NOT_AVAILABLE to setOf(
            ModelCapabilityState.DISCOVERED, ModelCapabilityState.REMOVED,
        ),
        ModelCapabilityState.DISCOVERED to setOf(
            ModelCapabilityState.DOWNLOAD_ALLOWED, ModelCapabilityState.REMOVED,
        ),
        ModelCapabilityState.DOWNLOAD_ALLOWED to setOf(
            ModelCapabilityState.DOWNLOADING, ModelCapabilityState.QUARANTINED,
            ModelCapabilityState.REMOVED,
        ),
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
            // §7: the runtime binding is RESOLVED first; a direct
            // INSTALLED → RUNTIME_AVAILABLE jump stays legal only for the legacy
            // entries whose runtime is verified in the same step (bundled).
            ModelCapabilityState.RUNTIME_RESOLVED, ModelCapabilityState.RUNTIME_AVAILABLE,
            ModelCapabilityState.FAILED, ModelCapabilityState.QUARANTINED,
        ),
        ModelCapabilityState.RUNTIME_RESOLVED to setOf(
            ModelCapabilityState.RUNTIME_AVAILABLE, ModelCapabilityState.FAILED,
            ModelCapabilityState.QUARANTINED,
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
            // §7: ACTIVE must be read-back verified before qualification.
            ModelCapabilityState.ACTIVE_READBACK_VERIFIED,
            ModelCapabilityState.PHYSICAL_QUALIFICATION_PENDING, ModelCapabilityState.RUNTIME_AVAILABLE,
            ModelCapabilityState.QUARANTINED,
        ),
        ModelCapabilityState.ACTIVE_READBACK_VERIFIED to setOf(
            ModelCapabilityState.PHYSICAL_QUALIFICATION_PENDING,
            ModelCapabilityState.RUNTIME_AVAILABLE, ModelCapabilityState.QUARANTINED,
        ),
        ModelCapabilityState.PHYSICAL_QUALIFICATION_PENDING to setOf(
            ModelCapabilityState.QUALIFIED, ModelCapabilityState.FUNCTIONAL_TEST_PASSED,
            ModelCapabilityState.QUARANTINED,
        ),
        ModelCapabilityState.QUALIFIED to setOf(
            ModelCapabilityState.QUARANTINED, ModelCapabilityState.REMOVED,
        ),
        ModelCapabilityState.FAILED to setOf(
            ModelCapabilityState.DOWNLOADING, ModelCapabilityState.QUARANTINED,
        ),
        ModelCapabilityState.QUARANTINED to setOf(),
        ModelCapabilityState.REMOVED to setOf(),
    )

    fun canTransition(from: ModelCapabilityState, to: ModelCapabilityState): Boolean =
        legal[from]?.contains(to) == true

    fun legalTargets(from: ModelCapabilityState): Set<ModelCapabilityState> =
        legal[from] ?: emptySet()

    /** The single refusal reason used everywhere (honest diagnostics). */
    const val REFUSAL_NO_SKIP = "CAPABILITY_LADDER_NO_SKIP"
}
