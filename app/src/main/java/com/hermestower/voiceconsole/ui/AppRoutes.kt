package com.hermestower.voiceconsole.ui

/**
 * Single registry of navigation routes (ST-25 navigation coverage; used by AppNav,
 * Compose UI tests and the Self-Test Center).
 */
object AppRoutes {
    const val CONSOLE = "console"
    const val HISTORY = "history"
    const val CONNECTION = "connection"
    const val VOICE = "voice"
    const val INTELLIGENCE = "intelligence"
    const val APPEARANCE = "appearance"
    const val DIAGNOSTICS = "diagnostics"
    const val ABOUT = "about"
    const val MODEL_MANAGER = "model_manager"
    const val TOWER_STUDIO = "tower_studio"
    const val SELF_TEST = "self_test"
    const val ARCHIVE = "archive"
    const val QR_SCANNER = "qr_scanner"

    val all: Set<String> = setOf(
        CONSOLE, HISTORY, CONNECTION, VOICE, INTELLIGENCE, APPEARANCE,
        DIAGNOSTICS, ABOUT, MODEL_MANAGER, TOWER_STUDIO, SELF_TEST, ARCHIVE,
        QR_SCANNER,
    )
}
