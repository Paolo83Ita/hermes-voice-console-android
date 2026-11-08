package com.hermestower.voiceconsole.models

import com.hermestower.voiceconsole.locale.StateLabels
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * GRAPH 4.13 §18/§19/§21/§26 — MODEL UX SEMANTICS pins.
 *
 * The rebuilt Models and Voice surface renders ONLY through these resolvers:
 *  - §18: raw enum values never render in ordinary UI — StateLabels must
 *    carry a localized label for every state/classification a card can show;
 *  - §19: compatibility is a typed verdict (never a bare "false"), and must
 *    not be negative merely because installation has not occurred;
 *  - §21: the primary action follows the actual state; transfer actions are
 *    state-gated;
 *  - §25: the raw device identifier never shows in the ordinary config card.
 */
class ModelUxSemanticsTest {

    // ---- §18 enum elimination ----

    @Test
    fun `every card-visible state has a localized label in all five locales`() {
        val cardVisibleStates = listOf(
            "NOT_DOWNLOADED", "DISCOVERED", "DOWNLOADING", "DOWNLOAD_PAUSED", "DOWNLOADED",
            "HASH_VERIFICATION_PENDING", "HASH_VERIFIED", "INSTALLING", "INSTALLED",
            "RUNTIME_AVAILABLE", "LOADED", "FUNCTIONAL_TEST_PASSED", "ACTIVE",
            "PHYSICAL_QUALIFICATION_PENDING", "QUALIFIED", "FAILED", "QUARANTINED", "REMOVED",
            "VERIFIED_DOWNLOADABLE", "RUNTIME_BUNDLED", "TEST_FIXTURE_ONLY",
            "CANDIDATE_PENDING_DISCOVERY", "SOURCE_UNVERIFIED_DOWNLOAD_BLOCKED",
            "LICENSE_BLOCKED", "COMPATIBILITY_BLOCKED", "SUPERSEDED", "REJECTED",
        )
        for (state in cardVisibleStates) {
            assertTrue("no StateLabels entry for $state", StateLabels.hasLabel(state))
            for (locale in listOf("it", "en", "es", "fr", "de")) {
                val label = StateLabels.label(state, locale)
                assertFalse("raw enum $state leaked in $locale", label == state)
            }
        }
    }

    // ---- §19 typed compatibility ----

    @Test
    fun `compatibility is not false merely because installation has not occurred`() {
        val v = CompatVerdictResolver.resolve(
            entryClassification = "VERIFIED_DOWNLOADABLE",
            recordState = null,
            active = null,
            runtimeRequirement = null,
            compatible = true,
            incompatibilityReason = null,
            progressState = null,
        )
        assertEquals(CompatVerdictKind.NOT_DOWNLOADED, v.kind)
        // NOT_DOWNLOADED is a readiness verdict, not an incompatibility
        assertTrue(v.kind != CompatVerdictKind.INCOMPATIBLE)
    }

    @Test
    fun `incompatible with reason renders the typed incompatibility verdict`() {
        val v = CompatVerdictResolver.resolve(
            entryClassification = "VERIFIED_DOWNLOADABLE",
            recordState = null,
            active = null,
            runtimeRequirement = null,
            compatible = false,
            incompatibilityReason = "Not compatible with arm64-v8a",
            progressState = null,
        )
        assertEquals(CompatVerdictKind.INCOMPATIBLE, v.kind)
        assertEquals("Not compatible with arm64-v8a", v.detail)
    }

    @Test
    fun `memory reason classifies as insufficient memory`() {
        val v = CompatVerdictResolver.resolve(
            entryClassification = "VERIFIED_DOWNLOADABLE",
            recordState = null,
            active = null,
            runtimeRequirement = null,
            compatible = false,
            incompatibilityReason = "Insufficient free memory for this component",
            progressState = null,
        )
        assertEquals(CompatVerdictKind.INSUFFICIENT_MEMORY, v.kind)
    }

    @Test
    fun `runtime requirement without installation is runtime unavailable`() {
        val v = CompatVerdictResolver.resolve(
            entryClassification = "VERIFIED_DOWNLOADABLE",
            recordState = null,
            active = null,
            runtimeRequirement = "vosk-android",
            compatible = true,
            incompatibilityReason = null,
            progressState = null,
        )
        assertEquals(CompatVerdictKind.RUNTIME_UNAVAILABLE, v.kind)
    }

    @Test
    fun `installed without qualification requires verification`() {
        val v = CompatVerdictResolver.resolve(
            entryClassification = "VERIFIED_DOWNLOADABLE",
            recordState = "INSTALLED",
            active = false,
            runtimeRequirement = null,
            compatible = true,
            incompatibilityReason = null,
            progressState = null,
        )
        assertEquals(CompatVerdictKind.VERIFICATION_REQUIRED, v.kind)
    }

    @Test
    fun `quarantined and failed are typed terminal verdicts`() {
        val q = CompatVerdictResolver.resolve("VERIFIED_DOWNLOADABLE", "ACTIVE", true, null, true, null, "QUARANTINED")
        assertEquals(CompatVerdictKind.QUARANTINED, q.kind)
        val f = CompatVerdictResolver.resolve("VERIFIED_DOWNLOADABLE", "FAILED", false, null, true, null, null)
        assertEquals(CompatVerdictKind.FAILED, f.kind)
    }

    // ---- §21 action semantics ----

    @Test
    fun `primary action follows the actual state`() {
        fun plan(state: String?, active: Boolean? = false, progress: String? = null) =
            ModelActionResolver.resolve(
                "VERIFIED_DOWNLOADABLE", state, active, progress,
                CompatVerdict(CompatVerdictKind.NOT_DOWNLOADED),
            ).primary

        assertEquals(ModelPrimaryAction.DOWNLOAD, plan(null))
        assertEquals(ModelPrimaryAction.VERIFY, plan("DOWNLOADED"))
        assertEquals(ModelPrimaryAction.INSTALL, plan("HASH_VERIFIED"))
        assertEquals(ModelPrimaryAction.CHECK_RUNTIME, plan("INSTALLED"))
        assertEquals(ModelPrimaryAction.RUN_FUNCTIONAL_TEST, plan("RUNTIME_AVAILABLE"))
        assertEquals(ModelPrimaryAction.ACTIVATE, plan("FUNCTIONAL_TEST_PASSED"))
        assertEquals(ModelPrimaryAction.TEST, plan("ACTIVE", active = true))
        assertEquals(ModelPrimaryAction.REPAIR, plan("FAILED"))
        assertEquals(ModelPrimaryAction.REVIEW_ISSUE, plan("ACTIVE", active = true, progress = "QUARANTINED"))
    }

    @Test
    fun `transfer in flight gates the transfer actions`() {
        val plan = ModelActionResolver.resolve(
            "VERIFIED_DOWNLOADABLE", null, null, "DOWNLOADING",
            CompatVerdict(CompatVerdictKind.NOT_DOWNLOADED),
        )
        assertTrue(plan.transferActions)
        // the card must not offer Download while a transfer is in flight
        assertFalse(plan.primary == ModelPrimaryAction.DOWNLOAD)
    }

    // ---- §25 device identity privacy ----

    @Test
    fun `ordinary config card shows the device class, never the raw identifier`() {
        assertEquals("POCO X6 Pro", DeviceClassLabel.resolve("2311DRK48G"))
        assertEquals("Xiaomi 11 Lite 5G NE", DeviceClassLabel.resolve("2109119DG"))
        // unknown products fall back to the detected label verbatim
        assertEquals("Unknown-Product", DeviceClassLabel.resolve("Unknown-Product"))
    }

    // ---- §15 profile surface ----

    @Test
    fun `the three mandated profiles exist with their display labels`() {
        assertEquals("Tower-first", VoiceModelProfile.TOWER_FIRST.displayLabel)
        assertEquals("Hybrid balanced", VoiceModelProfile.HYBRID_BALANCED.displayLabel)
        assertEquals("Local experimental", VoiceModelProfile.LOCAL_EXPERIMENTAL.displayLabel)
        assertTrue(VoiceModelProfile.LOCAL_EXPERIMENTAL.experimental)
    }
}
