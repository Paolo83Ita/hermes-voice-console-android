package com.hermestower.voiceconsole.models

/**
 * OWNER PHYSICAL RETEST F-C REPAIR (Package 8) — REAL per-component runtime
 * availability for the §19 typed verdict.
 *
 * The linked native seams (Gradle AARs, one per component family) are the
 * truth source. On any build that actually ships them the probe succeeds; on
 * a build where a runtime was removed the class disappears and the probe
 * fails — so RUNTIME_UNAVAILABLE stays an HONEST verdict (never a label-keyed
 * guess). Probes are cheap class-presence checks (no JNI load, no I/O) and
 * are safe on the JVM test classpath as long as the dependency is on it.
 */
object ComponentRuntimeAvailability {

    /** sherpa-onnx AAR (Paola / Riccardo voices + TTS engine family). */
    const val SHERPA_CLASS = "com.k2fsa.sherpa.onnx.OfflineTts"

    /** vosk-android AAR (offline Italian STT model family). */
    const val VOSK_CLASS = "org.vosk.Model"

    /** onnxruntime-android AAR (wake word trio + Silero VAD + GTCRN family). */
    const val ORT_CLASS = "ai.onnxruntime.OrtEnvironment"

    fun classPresent(name: String): Boolean =
        runCatching { Class.forName(name) }.isSuccess

    fun sherpaAvailable(): Boolean = classPresent(SHERPA_CLASS)

    fun voskAvailable(): Boolean = classPresent(VOSK_CLASS)

    fun ortAvailable(): Boolean = classPresent(ORT_CLASS)

    /**
     * Family-mapped probe for one catalog entry. [localLlmHasVerifiedRuntime]
     * comes from LocalLlmEngine.hasVerifiedRuntime (the injected llama.cpp
     * seam — CP-G4.14-005); the other families read the linked AAR seams.
     */
    fun probe(componentType: ModelComponentType, localLlmHasVerifiedRuntime: Boolean): Boolean =
        when (componentType) {
            ModelComponentType.VOICE, ModelComponentType.TTS_ENGINE -> sherpaAvailable()
            ModelComponentType.SPEECH_TO_TEXT -> voskAvailable()
            ModelComponentType.WAKE_WORD, ModelComponentType.VAD, ModelComponentType.NOISE_SUPPRESSION -> ortAvailable()
            ModelComponentType.LOCAL_LLM -> localLlmHasVerifiedRuntime
        }
}
