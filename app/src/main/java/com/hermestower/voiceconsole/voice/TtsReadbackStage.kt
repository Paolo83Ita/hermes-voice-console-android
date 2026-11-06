package com.hermestower.voiceconsole.voice

/**
 * GRAPH 4.10 CP-G4.10-004 (§12 TTS READBACK TRUTH) — graded read-back stages.
 *
 * Never one ambiguous "ACTIVE": the Italian voice chain is reported step by
 * step, so the Owner can see exactly how far the runtime got. ST-51 PASS
 * requires reaching [ITALIAN_UTTERANCE_COMPLETED] on a real engine;
 * [ITALIAN_VOICE_OWNER_VERIFIED] can be granted ONLY by Owner physical
 * evidence (never by this code).
 *
 * When a selected voice looks Italian but the locale/inventory read-back is
 * incomplete, the honest stage is [ITALIAN_VOICE_SELECTED_READBACK_PARTIAL] —
 * never a full ACTIVE claim.
 */
enum class TtsReadbackStage {
    ITALIAN_VOICE_REQUESTED,
    ITALIAN_VOICE_IDENTIFIER_SELECTED,
    ITALIAN_VOICE_LOCALE_VERIFIED,
    ITALIAN_VOICE_RUNTIME_LOADED,
    ITALIAN_UTTERANCE_COMPLETED,
    ITALIAN_VOICE_OWNER_VERIFIED,
    ITALIAN_VOICE_SELECTED_READBACK_PARTIAL,
}

object TtsReadbackGrader {

    /**
     * Grades the chain from the observable facts of the last utterance.
     * @param italianRequested the console requested an Italian voice.
     * @param selectedVoiceName the selected voice identity (may be null pre-selection).
     * @param selectedVoiceLocaleTag the locale of the SELECTED voice read-back.
     * @param runtimeLoaded the engine reports a loaded runtime/handle.
     * @param utteranceCompleted the utterance physically started and completed.
     */
    fun grade(
        italianRequested: Boolean,
        selectedVoiceName: String?,
        selectedVoiceLocaleTag: String?,
        runtimeLoaded: Boolean,
        utteranceCompleted: Boolean,
    ): TtsReadbackStage {
        if (!italianRequested) return TtsReadbackStage.ITALIAN_VOICE_REQUESTED
        if (selectedVoiceName.isNullOrBlank()) return TtsReadbackStage.ITALIAN_VOICE_REQUESTED
        val locale = selectedVoiceLocaleTag?.lowercase()
        val localeItalian = locale != null && locale.startsWith("it")
        if (!localeItalian) return TtsReadbackStage.ITALIAN_VOICE_SELECTED_READBACK_PARTIAL
        if (!runtimeLoaded) return TtsReadbackStage.ITALIAN_VOICE_LOCALE_VERIFIED
        if (!utteranceCompleted) return TtsReadbackStage.ITALIAN_VOICE_RUNTIME_LOADED
        // Owner verification is NEVER self-granted by the grader.
        return TtsReadbackStage.ITALIAN_UTTERANCE_COMPLETED
    }
}
