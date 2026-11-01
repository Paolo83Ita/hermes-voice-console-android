package com.hermestower.voiceconsole.models

/**
 * Independent component classes managed by the Voice Model Manager (GRAPH 2 §4).
 * Each class is managed and qualified independently; a component class never
 * silently activates another.
 */
enum class ModelComponentType(val displayLabel: String) {
    WAKE_WORD("Wake word engine"),
    VAD("Voice activity detector"),
    NOISE_SUPPRESSION("Noise suppression"),
    SPEECH_TO_TEXT("Speech-to-text model"),
    LOCAL_LLM("Local conversational LLM"),
    TTS_ENGINE("Text-to-speech engine"),
    VOICE("Voice model"),
}

/**
 * Typed model lifecycle states (GRAPH 2 §4). Transitions are enforced by the
 * installation registry; no state is skipped silently.
 */
enum class ModelInstallState(val displayLabel: String, val terminal: Boolean = false) {
    DISCOVERED("Discovered"),
    COMPATIBILITY_PENDING("Compatibility pending"),
    INCOMPATIBLE("Incompatible", terminal = true),
    LICENSE_PENDING("License review pending"),
    DOWNLOAD_APPROVAL_REQUIRED("Download approval required"),
    DOWNLOADING("Downloading"),
    DOWNLOAD_PAUSED("Download paused"),
    DOWNLOADED("Downloaded"),
    HASH_VERIFICATION_PENDING("Hash verification pending"),
    HASH_VERIFIED("Hash verified"),
    INSTALLING("Installing"),
    INSTALLED("Installed"),
    QUALIFICATION_PENDING("Qualification pending"),
    QUALIFIED("Qualified"),
    ACTIVE("Active"),
    FAILED("Failed", terminal = true),
    ROLLBACK_AVAILABLE("Rollback available"),
    REMOVED("Removed", terminal = true),
}

/** Download/activation decision gates the resolver and controllers must honor. */
enum class VoiceModelProfile(val displayLabel: String, val experimental: Boolean = false) {
    TOWER_FIRST("Tower-first"),
    HYBRID_BALANCED("Hybrid balanced"),
    LOCAL_EXPERIMENTAL("Local experimental", experimental = true),
    MANUAL_ADVANCED("Manual advanced"),
}
