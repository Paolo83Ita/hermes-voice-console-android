package com.hermestower.voiceconsole.core.mode

/** The five operating modes of the console (PRODUCT_VISION.md §5). */
enum class OperatingMode(val label: String, val description: String) {
    OFF("OFF", "Voice processing disabled."),
    LOCAL("LOCAL", "Local-only conversational and device functions."),
    TOWER("TOWER", "Recognized speech is routed to Hermes Tower."),
    HYBRID("HYBRID", "Simple queries local; complex or consequential routed to the Tower."),
    DISCONNECTED_SAFE("SAFE", "Tower unreachable: safe local functions, optional queue.");

    companion object {
        val selectable: List<OperatingMode> = listOf(OFF, LOCAL, TOWER, HYBRID)
    }
}
