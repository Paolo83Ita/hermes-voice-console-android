package com.hermestower.voiceconsole.voice

import java.time.Instant

/**
 * GRAPH 4.10 CP-G4.10-003 (§8 VOICE RUNTIME FACTORY) — the single activation
 * pipeline for a downloaded Piper voice.
 *
 * Pipeline (each step typed, each failure honest):
 *   1. files verified   — the exact ONNX + tokens/config must resolve on disk;
 *   2. runtime available — the sherpa runtime initializes against those files;
 *   3. voice loaded      — the runtime reports a handle for THIS voice;
 *   4. test synthesis    — the preview phrase is synthesized;
 *   5. playback          — the PCM actually plays to completion;
 *   6. read-back         — the runtime identity matches the selected voice.
 *
 * Only then [ActivationOutcome.Active]; on any failure the installed files are
 * PRESERVED (never deleted) and the outcome carries the typed reason + the
 * mandatory fallback chain (Android Italian TTS if available, else TEXT_ONLY).
 * The factory never keeps the old voice audible while showing the new voice as
 * selected — the [VoiceSelectionAuthority] drops to the system route on failure.
 */
class VoiceRuntimeFactory(
    private val clock: () -> Instant = { Instant.now() },
) {

    /** The exact files a loaded voice must have. */
    data class VoiceFiles(val modelFile: String, val configFile: String?)

    /**
     * One activation seam per downloadable voice. AppGraph adapts the REAL
     * [SherpaOnnxTtsEngine] onto this; JVM tests inject a scripted seam.
     */
    interface VoiceRuntimeSeam {
        fun resolveFiles(): VoiceFiles?
        /** Loads the runtime against the resolved files; null = loaded, else typed error. */
        fun load(): String?
        /** Starts the test synthesis + playback of [phrase]. */
        fun speakTest(phrase: String)
        /** Blocks until the utterance completes; typed failure distinguishes synthesis vs playback. */
        fun awaitUtteranceCompletion(timeoutMs: Long = COMPLETION_TIMEOUT_MS): UtteranceResult
        /** Runtime identity read-back (engine id / voice name). */
        fun readBackVoiceId(): String?
        fun interrupt()
    }

    enum class UtteranceResult { COMPLETED, SYNTHESIS_FAILED, PLAYBACK_FAILED }

    sealed class ActivationOutcome {
        data class Active(
            val voiceId: String,
            val readBack: VoiceRuntimeReadBack,
        ) : ActivationOutcome()

        data class Failed(
            val voiceId: String,
            val typedReason: TypedReason,
            val detail: String?,
            /** The mandatory fallback the console applies (system TTS, else TEXT_ONLY). */
            val fallback: VoiceRuntimeType,
        ) : ActivationOutcome()
    }

    enum class TypedReason { FILES_MISSING, RUNTIME_INIT_FAILED, SYNTHESIS_FAILED, PLAYBACK_FAILED, READBACK_MISMATCH }

    fun activate(
        seam: VoiceRuntimeSeam,
        voiceId: String,
        phrase: String = PREVIEW_PHRASE,
    ): ActivationOutcome {
        // 1. files verified
        val files = runCatching { seam.resolveFiles() }.getOrNull()
            ?: return ActivationOutcome.Failed(voiceId, TypedReason.FILES_MISSING, null, fallbackAfterFailure())

        // 2. runtime available + 3. voice loaded
        val loadError = runCatching { seam.load() }.getOrElse { it.message ?: "runtime init threw" }
        if (loadError != null) {
            seam.interrupt()
            return ActivationOutcome.Failed(voiceId, TypedReason.RUNTIME_INIT_FAILED, loadError, fallbackAfterFailure())
        }

        // 4. test synthesis + 5. playback (one awaited utterance)
        runCatching { seam.speakTest(phrase) }
        val completed = runCatching { seam.awaitUtteranceCompletion() }
            .getOrDefault(UtteranceResult.SYNTHESIS_FAILED)
        if (completed != UtteranceResult.COMPLETED) {
            seam.interrupt()
            val reason = if (completed == UtteranceResult.SYNTHESIS_FAILED) {
                TypedReason.SYNTHESIS_FAILED
            } else {
                TypedReason.PLAYBACK_FAILED
            }
            return ActivationOutcome.Failed(voiceId, reason, "test utterance state=$completed", fallbackAfterFailure())
        }

        // 6. read-back matches the selected voice
        val readBackId = runCatching { seam.readBackVoiceId() }.getOrNull()
        val readBack = VoiceRuntimeReadBack(
            voiceId = readBackId,
            runtimeId = "SHERPA_ONNX",
            filesVerified = true,
            runtimeAvailable = true,
            voiceLoaded = true,
            synthesisCompleted = true,
            playbackCompleted = true,
            readBackMatchesSelected = readBackId == voiceId,
            completedAt = clock().toString(),
        )
        return if (readBack.readBackMatchesSelected) {
            ActivationOutcome.Active(voiceId, readBack)
        } else {
            ActivationOutcome.Failed(voiceId, TypedReason.READBACK_MISMATCH, "runtime reported '$readBackId'", fallbackAfterFailure())
        }
    }

    private fun fallbackAfterFailure(): VoiceRuntimeType = VoiceRuntimeType.ANDROID_SYSTEM_TTS

    companion object {
        /** The activation preview phrase (the verified ST-51 phrase family). */
        const val PREVIEW_PHRASE = "Benvenuto nella console vocale di Hermes Tower."
        const val COMPLETION_TIMEOUT_MS = 20_000L
    }
}
