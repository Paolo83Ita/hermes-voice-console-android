package com.hermestower.voiceconsole.voice

import com.hermestower.voiceconsole.locale.ResponseCategory
import com.hermestower.voiceconsole.locale.ResponseCatalog
import com.hermestower.voiceconsole.locale.ResponseLocaleResolver
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * GRAPH 4.10 CP-G4.10-003 (§7 VOICE SELECTION AUTHORITY) — the ONE canonical
 * authority for which voice the console actually speaks with.
 *
 * Owner physical truth P0-03: a downloaded voice was SHOWN as selected while
 * the chat kept speaking with the previous voice. From this authority on:
 *
 *  - UI selection is NOT activation: a downloaded voice becomes ACTIVE only
 *    after files verified + runtime available + voice loaded + test synthesis
 *    completed + playback completed + runtime read-back matches the selected
 *    voice (the six conditions recorded in [VoiceRuntimeReadBack]);
 *  - [VoiceRuntimeFactory] performs that qualification and reports here;
 *  - `activeNaturalVoiceId()` returns a voice ONLY in the fully qualified
 *    state — the route (TtsRouteSwitch) consumes exactly this, so a failed
 *    activation can never leave a stale voice audible;
 *  - runtime types: ANDROID_SYSTEM_TTS (mandatory fallback), SHERPA_PIPER_LOCAL
 *    (downloaded natural voice), TEXT_ONLY (last resort, fail-closed).
 *
 * Pure JVM + injectable providers; the Model Installation Registry stays the
 * persistence authority for installed/active records — this authority adds the
 * RUNTIME truth on top (never a second persistence of install state).
 */
enum class VoiceRuntimeType { ANDROID_SYSTEM_TTS, SHERPA_PIPER_LOCAL, TEXT_ONLY }

enum class VoiceQualificationState {
    /** Downloaded and selected in the UI only — NOT usable as route truth. */
    SELECTION_PENDING_RUNTIME,
    /** The six-condition runtime qualification is in progress. */
    QUALIFYING,
    /** Fully qualified: files + runtime + load + synthesis + playback + read-back all passed. */
    ACTIVE,
    /** Runtime qualification failed after installation; files preserved. */
    INSTALLED_RUNTIME_FAILED,
    /** No voice selected (system route / text-only route applies). */
    NONE,
}

/** §7: the six-condition runtime read-back (all must be true for ACTIVE). */
data class VoiceRuntimeReadBack(
    val voiceId: String?,
    val runtimeId: String,
    val filesVerified: Boolean,
    val runtimeAvailable: Boolean,
    val voiceLoaded: Boolean,
    val synthesisCompleted: Boolean,
    val playbackCompleted: Boolean,
    val readBackMatchesSelected: Boolean,
    val completedAt: String,
) {
    fun allConditionsMet(): Boolean =
        filesVerified && runtimeAvailable && voiceLoaded &&
            synthesisCompleted && playbackCompleted && readBackMatchesSelected
}

/** §7 required fields — one immutable snapshot published as StateFlow. */
data class VoiceSelectionState(
    val selectedVoiceId: String? = null,
    val runtimeType: VoiceRuntimeType = VoiceRuntimeType.ANDROID_SYSTEM_TTS,
    val modelFile: String? = null,
    val configFile: String? = null,
    val language: String = "it",
    val version: String? = null,
    val sha256: String? = null,
    val qualificationState: VoiceQualificationState = VoiceQualificationState.NONE,
    val activeRuntimeState: String = "SYSTEM_READY",
    val fallbackState: String? = null,
    val lastActivationResult: String? = null,
    val lastRuntimeReadBack: VoiceRuntimeReadBack? = null,
)

class VoiceSelectionAuthority(
    /** Disk truth for a downloaded voice bundle (AppGraph: resolveVoiceDir() != null). */
    private val voiceFilesPresent: (voiceId: String) -> Boolean = { false },
    /** System-route truth (AppGraph: Italian system TTS negotiated available). */
    private val systemItalianAvailable: () -> Boolean = { true },
) {

    private val _state = MutableStateFlow(VoiceSelectionState())
    val state: StateFlow<VoiceSelectionState> = _state.asStateFlow()

    /** §7: a downloaded voice is SELECTED, never yet ACTIVE. */
    fun selectDownloaded(voiceId: String, modelFile: String?, configFile: String?, version: String?, sha256: String?) {
        val filesPresent = voiceFilesPresent(voiceId)
        _state.value = VoiceSelectionState(
            selectedVoiceId = voiceId,
            runtimeType = VoiceRuntimeType.SHERPA_PIPER_LOCAL,
            modelFile = modelFile,
            configFile = configFile,
            language = "it",
            version = version,
            sha256 = sha256,
            qualificationState = if (filesPresent) VoiceQualificationState.SELECTION_PENDING_RUNTIME
            else VoiceQualificationState.INSTALLED_RUNTIME_FAILED,
            activeRuntimeState = if (filesPresent) "NOT_LOADED" else "FILES_MISSING",
            fallbackState = if (filesPresent) null else "voice files missing on disk",
            lastActivationResult = "SELECTED (runtime qualification pending)",
            lastRuntimeReadBack = null,
        )
    }

    fun beginQualification() {
        val s = _state.value
        if (s.selectedVoiceId == null) return
        _state.value = s.copy(
            qualificationState = VoiceQualificationState.QUALIFYING,
            activeRuntimeState = "LOADING",
            lastActivationResult = "RUNTIME QUALIFICATION IN PROGRESS",
        )
    }

    /** §7/§8: the ONLY path to ACTIVE — a complete six-condition read-back. */
    fun recordRuntimeReadBack(readBack: VoiceRuntimeReadBack) {
        val s = _state.value
        val matchedVoice = readBack.voiceId == s.selectedVoiceId
        val complete = readBack.allConditionsMet() && matchedVoice
        _state.value = s.copy(
            runtimeType = if (complete) VoiceRuntimeType.SHERPA_PIPER_LOCAL else s.runtimeType,
            qualificationState = if (complete) VoiceQualificationState.ACTIVE
            else VoiceQualificationState.INSTALLED_RUNTIME_FAILED,
            activeRuntimeState = if (complete) "ACTIVE" else "RUNTIME_FAILED",
            lastRuntimeReadBack = readBack,
            lastActivationResult = if (complete) "ACTIVE (runtime read-back complete)"
            else "RUNTIME QUALIFICATION FAILED",
            fallbackState = if (complete) null else "runtime qualification failed; system Italian TTS applies",
        )
        if (!complete) {
            // §8: never silently keep the OLD voice while showing a new one —
            // the route drops to the system Italian TTS (mandatory fallback).
            _state.value = _state.value.copy(runtimeType = VoiceRuntimeType.ANDROID_SYSTEM_TTS)
        }
    }

    /** §8: activation failure with installed files preserved. */
    fun markRuntimeFailed(typedReason: String) {
        val s = _state.value
        _state.value = s.copy(
            qualificationState = VoiceQualificationState.INSTALLED_RUNTIME_FAILED,
            activeRuntimeState = "RUNTIME_FAILED",
            runtimeType = VoiceRuntimeType.ANDROID_SYSTEM_TTS,
            lastActivationResult = "INSTALLED_RUNTIME_FAILED: $typedReason",
            fallbackState = "installed files preserved; system Italian TTS applies",
        )
    }

    fun fallbackToTextOnly(reason: String) {
        val s = _state.value
        _state.value = s.copy(
            runtimeType = VoiceRuntimeType.TEXT_ONLY,
            activeRuntimeState = "TEXT_ONLY",
            fallbackState = reason,
            lastActivationResult = "TEXT_ONLY_SAFE_DEGRADED: $reason",
        )
    }

    /** Clear the downloaded selection (Remove action): back to the system route. */
    fun clearSelection() {
        _state.value = VoiceSelectionState(
            runtimeType = VoiceRuntimeType.ANDROID_SYSTEM_TTS,
            fallbackState = null,
            lastActivationResult = "SELECTION CLEARED (system route)",
        )
    }

    /**
     * §7: route truth. A downloaded voice is consumed by the runtime ONLY in
     * the ACTIVE state with files still present on disk — the registry/disk
     * read-back of Graph 4.9 is preserved and tightened by the qualification.
     */
    fun activeNaturalVoiceId(): String? {
        val s = _state.value
        if (s.qualificationState != VoiceQualificationState.ACTIVE) return null
        if (s.runtimeType != VoiceRuntimeType.SHERPA_PIPER_LOCAL) return null
        val id = s.selectedVoiceId ?: return null
        return if (voiceFilesPresent(id)) id else null
    }

    /** Localized one-line status for the Voice & Audio / Sanctum cards (§5 producer). */
    fun statusLine(localeTag: String): String {
        val s = _state.value
        val detail = when (s.qualificationState) {
            VoiceQualificationState.ACTIVE -> s.selectedVoiceId ?: "system"
            VoiceQualificationState.QUALIFYING -> "qualifying"
            VoiceQualificationState.SELECTION_PENDING_RUNTIME -> "selected (runtime pending)"
            VoiceQualificationState.INSTALLED_RUNTIME_FAILED -> "runtime failed (fallback active)"
            VoiceQualificationState.NONE -> if (systemItalianAvailable()) "system Italian TTS" else "text only"
        }
        return ResponseCatalog.text(ResponseCategory.VOICE_STATUS, ResponseLocaleResolver.normalize(localeTag) ?: "en", detail)
    }
}
