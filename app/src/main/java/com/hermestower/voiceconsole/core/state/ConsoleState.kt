package com.hermestower.voiceconsole.core.state

/**
 * The fifteen truthful visual states of the console.
 * Authoritative specification: docs/UI_STATE_MACHINE.md (§2 catalog, §3 transitions).
 */
enum class ConsoleState(
    val label: String,
    val accessibility: String,
    val isResting: Boolean,
) {
    DORMANT("DORMANT", "Console dormant. Voice processing disabled.", isResting = true),
    IDLE("IDLE", "Idle. Listening for wake word.", isResting = true),
    WAKE_DETECTED("WAKE DETECTED", "Wake word detected.", isResting = false),
    LISTENING("LISTENING", "Listening.", isResting = false),
    TRANSCRIBING("TRANSCRIBING", "Transcribing.", isResting = false),
    UNDERSTANDING_LOCAL("THINKING LOCALLY", "Thinking locally.", isResting = false),
    CONTACTING_TOWER("CONTACTING TOWER", "Contacting Hermes Tower.", isResting = false),
    TOWER_PROCESSING("TOWER PROCESSING", "Hermes Tower is processing.", isResting = false),
    SPEAKING("SPEAKING", "Speaking.", isResting = false),
    REQUEST_QUEUED("QUEUED", "Request queued until the Tower is reachable.", isResting = false),
    DISCONNECTED("DISCONNECTED", "Disconnected. Safe local functions only.", isResting = true),
    DEGRADED("DEGRADED", "Connection degraded.", isResting = true),
    ERROR("ERROR", "Error. Details in diagnostics.", isResting = true),
    PRIVACY_MUTED("PRIVACY MUTED", "Privacy muted. Microphone disabled.", isResting = true),
    UPDATE_AVAILABLE("UPDATE AVAILABLE", "Update available.", isResting = true);

    val isPipelineActive: Boolean
        get() = this in setOf(
            WAKE_DETECTED, LISTENING, TRANSCRIBING, UNDERSTANDING_LOCAL,
            CONTACTING_TOWER, TOWER_PROCESSING, SPEAKING,
        )
}
