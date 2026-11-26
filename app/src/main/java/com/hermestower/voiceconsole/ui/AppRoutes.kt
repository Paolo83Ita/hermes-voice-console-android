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

    /** ANDROID REPAIR CP-005 (mission section 4): the TOWER primary surface and
     *  the PAIRING flow — pairing is a first-class destination, reachable within
     *  two taps from every primary screen (PAIRING_MAX_TWO_TAPS_PASS). */
    const val TOWER_HOME = "tower_home"
    const val PAIRING = "pairing"

    /** CP-G4-017 (directive §5): the Settings hub — the fifth and only new
     *  primary destination. General/voice/tower/models/display categories and
     *  the diagnostics group are reached FROM here; the drawer lists only the
     *  five primary destinations. */
    const val SETTINGS = "settings"

    val all: Set<String> = setOf(
        CONSOLE, HISTORY, CONNECTION, VOICE, INTELLIGENCE, APPEARANCE,
        DIAGNOSTICS, ABOUT, MODEL_MANAGER, TOWER_STUDIO, SELF_TEST, ARCHIVE,
        QR_SCANNER, SETTINGS, TOWER_HOME, PAIRING,
    )
}
