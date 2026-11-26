package com.hermestower.voiceconsole.models

import com.hermestower.voiceconsole.R

/**
 * GRAPH 4.13 §17-§21 — MODEL UX SEMANTICS (pure, JVM-testable).
 *
 * The presentation layer of Models and Voice renders ONLY what these typed
 * resolvers decide. Raw enum values never reach the ordinary UI (§18 — the
 * §26 pin asserts StateLabels coverage); compatibility is a typed, plain-
 * language verdict (§19), never "false"; the primary action follows the real
 * component state (§21) and only ONE action may be filled (§17).
 *
 * This file is deliberately compose-free: the §26 matrix pins it on the JVM.
 */

// ---------------- §19 typed compatibility ----------------

enum class CompatVerdictKind {
    COMPATIBLE,
    IN_USE,
    INACTIVE_READY,
    NOT_DOWNLOADED,
    DOWNLOAD_BLOCKED,
    RUNTIME_UNAVAILABLE,
    VERIFICATION_REQUIRED,
    INSTALL_READY,
    QUALIFICATION_PENDING,
    QUARANTINED,
    FAILED,
    INCOMPATIBLE,
    INSUFFICIENT_MEMORY,
}

data class CompatVerdict(
    val kind: CompatVerdictKind,
    /** The typed reason detail (e.g. the incompatibility reason text). */
    val detail: String? = null,
)

object CompatVerdictResolver {

    private val BLOCKED = setOf(
        "SOURCE_UNVERIFIED_DOWNLOAD_BLOCKED",
        "LICENSE_BLOCKED",
        "COMPATIBILITY_BLOCKED",
        "TEST_FIXTURE_ONLY",
        "SUPERSEDED",
        "REJECTED",
    )

    /**
     * §19: compatibility must not be false merely because installation has not
     * occurred. OWNER PHYSICAL RETEST F-C REPAIR (Package 8): a never-downloaded
     * entry with a declared runtime requirement used to resolve to
     * RUNTIME_UNAVAILABLE unconditionally — on the physical POCO X6 Pro the
     * Owner saw "runtime Android richiesto non disponibile" on NINE cards whose
     * runtimes (vosk-android / sherpa-onnx / onnxruntime / llama.cpp) are REAL,
     * BUNDLED and INTEGRATED, and with no actionable primary action the voices
     * and the local model became literally unusable. The verdict is now keyed to
     * the ACTUAL runtime truth ([runtimeAvailable], probed from the linked
     * native seams — not from labels): only a genuinely absent runtime says
     * RUNTIME_UNAVAILABLE; a present runtime with no install record says
     * NOT_DOWNLOADED (→ the DOWNLOAD primary action, §21).
     */
    fun resolve(
        entryClassification: String,
        recordState: String?,
        active: Boolean?,
        runtimeRequirement: String?,
        compatible: Boolean?,
        incompatibilityReason: String?,
        progressState: String?,
        runtimeAvailable: Boolean = true,
    ): CompatVerdict {
        if (progressState == "QUARANTINED" || recordState == "QUARANTINED") {
            return CompatVerdict(CompatVerdictKind.QUARANTINED)
        }
        if (recordState == "FAILED" || progressState == "FAILED") return CompatVerdict(CompatVerdictKind.FAILED)
        if (compatible == false) {
            val reason = incompatibilityReason
            val memory = reason != null && listOf("memory", "ram", "memoria").any { reason.contains(it, ignoreCase = true) }
            return if (memory) CompatVerdict(CompatVerdictKind.INSUFFICIENT_MEMORY, reason)
            else CompatVerdict(CompatVerdictKind.INCOMPATIBLE, reason)
        }
        if (entryClassification in BLOCKED) return CompatVerdict(CompatVerdictKind.DOWNLOAD_BLOCKED, entryClassification)
        if (entryClassification == "RUNTIME_BUNDLED") return CompatVerdict(CompatVerdictKind.COMPATIBLE)
        // ANDROID REPAIR CP-006 (R3, A4R-PHYS-MODEL-002): the PROGRESS state is
        // the on-disk truth when no installation record exists yet (the record
        // could not exist before the ensureRecord repair). A DOWNLOADED partial
        // is VERIFICATION_REQUIRED (the promote path), a HASH_VERIFIED partial
        // is INSTALL_READY — the card no longer lies 'non installato' beside a
        // completed download.
        val effectiveState = recordState ?: progressState
        if (runtimeRequirement != null && effectiveState == null) {
            return if (runtimeAvailable) CompatVerdict(CompatVerdictKind.NOT_DOWNLOADED)
            else CompatVerdict(CompatVerdictKind.RUNTIME_UNAVAILABLE)
        }
        return when (effectiveState) {
            null -> CompatVerdict(CompatVerdictKind.NOT_DOWNLOADED)
            "DOWNLOADING", "DOWNLOAD_PAUSED" -> CompatVerdict(CompatVerdictKind.NOT_DOWNLOADED)
            "DOWNLOADED", "HASH_VERIFICATION_PENDING", "INSTALLING" -> CompatVerdict(CompatVerdictKind.VERIFICATION_REQUIRED)
            "HASH_VERIFIED" -> CompatVerdict(CompatVerdictKind.INSTALL_READY)
            "INSTALLED" -> if (active == true) CompatVerdict(CompatVerdictKind.IN_USE)
            else CompatVerdict(CompatVerdictKind.VERIFICATION_REQUIRED)
            "RUNTIME_AVAILABLE" -> CompatVerdict(CompatVerdictKind.VERIFICATION_REQUIRED)
            "LOADED", "FUNCTIONAL_TEST_PASSED", "QUALIFIED", "ACTIVE" ->
                if (active == true) CompatVerdict(CompatVerdictKind.IN_USE)
                else CompatVerdict(CompatVerdictKind.INACTIVE_READY)
            "PHYSICAL_QUALIFICATION_PENDING" -> CompatVerdict(CompatVerdictKind.QUALIFICATION_PENDING)
            "REMOVED" -> CompatVerdict(CompatVerdictKind.NOT_DOWNLOADED)
            "DISCOVERED" -> CompatVerdict(CompatVerdictKind.NOT_DOWNLOADED)
            else -> CompatVerdict(CompatVerdictKind.COMPATIBLE)
        }
    }
}

// ---------------- §21 action semantics ----------------

enum class ModelPrimaryAction {
    DOWNLOAD,
    VERIFY,
    INSTALL,
    CHECK_RUNTIME,
    RUN_FUNCTIONAL_TEST,
    ACTIVATE,
    TEST,
    REPAIR,
    REVIEW_ISSUE,
    DETAILS_ONLY,
}

data class ModelActionPlan(
    val primary: ModelPrimaryAction,
    /** Whether the download flow's pause/resume/cancel row applies right now. */
    val transferActions: Boolean,
)

object ModelActionResolver {

    /** §21: actions must follow the actual state; one filled action per card. */
    fun resolve(
        entryClassification: String,
        recordState: String?,
        active: Boolean?,
        progressState: String?,
        verdict: CompatVerdict,
    ): ModelActionPlan {
        if (progressState == "DOWNLOADING" || progressState == "DOWNLOAD_PAUSED") {
            // transfer in flight — the primary action is the transfer control
            return ModelActionPlan(ModelPrimaryAction.DETAILS_ONLY, transferActions = true)
        }
        // CP-006 R3: the record-less progress truth participates in the action
        // decision too (a DOWNLOADED partial promotes — never re-downloads).
        val effectiveState = recordState ?: progressState
        val primary = when {
            verdict.kind == CompatVerdictKind.QUARANTINED ||
                progressState == "QUARANTINED" || recordState == "QUARANTINED" -> ModelPrimaryAction.REVIEW_ISSUE
            verdict.kind == CompatVerdictKind.FAILED || recordState == "FAILED" || progressState == "FAILED" -> ModelPrimaryAction.REPAIR
            effectiveState == "ACTIVE" || active == true -> ModelPrimaryAction.TEST
            effectiveState == "FUNCTIONAL_TEST_PASSED" -> ModelPrimaryAction.ACTIVATE
            effectiveState == "RUNTIME_AVAILABLE" -> ModelPrimaryAction.RUN_FUNCTIONAL_TEST
            effectiveState == "RUNTIME_RESOLVED" -> ModelPrimaryAction.CHECK_RUNTIME
            effectiveState == "INSTALLED" -> ModelPrimaryAction.CHECK_RUNTIME
            effectiveState == "HASH_VERIFIED" -> ModelPrimaryAction.INSTALL
            effectiveState == "DOWNLOADED" || effectiveState == "HASH_VERIFICATION_PENDING" || effectiveState == "INSTALLING" -> ModelPrimaryAction.VERIFY
            entryClassification == "RUNTIME_BUNDLED" -> ModelPrimaryAction.TEST
            entryClassification == "VERIFIED_DOWNLOADABLE" -> ModelPrimaryAction.DOWNLOAD
            else -> ModelPrimaryAction.DETAILS_ONLY
        }
        return ModelActionPlan(primary, transferActions = false)
    }
}

// ---------------- §25 device identity privacy ----------------

/**
 * Ordinary UI shows the DEVICE CLASS label; the raw product identifier stays
 * in Advanced Diagnostics only. Mapping covers the Owner's qualification
 * devices; unknown products fall back to the detected product label verbatim.
 */
object DeviceClassLabel {
    private val known = mapOf(
        "2311DRK48G" to "POCO X6 Pro",
        "2109119DG" to "Xiaomi 11 Lite 5G NE",
    )

    fun resolve(model: String): String = known[model] ?: model
}

/** §13: the plain-language purpose line per component type. */
object ComponentPurpose {
    fun stringRes(type: ModelComponentType): Int = when (type) {
        ModelComponentType.VOICE -> R.string.mm_purpose_voice
        ModelComponentType.SPEECH_TO_TEXT -> R.string.mm_purpose_stt
        ModelComponentType.WAKE_WORD -> R.string.mm_purpose_wake_word
        ModelComponentType.VAD -> R.string.mm_purpose_vad
        ModelComponentType.NOISE_SUPPRESSION -> R.string.mm_purpose_noise
        ModelComponentType.LOCAL_LLM -> R.string.mm_purpose_llm
        ModelComponentType.TTS_ENGINE -> R.string.mm_purpose_tts_engine
    }
}
