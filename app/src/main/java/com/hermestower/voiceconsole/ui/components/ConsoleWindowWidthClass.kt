package com.hermestower.voiceconsole.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

/**
 * GRAPH 4.10 CP-G4.10-008 (§19 COMPACT PHONE WINDOW CLASSES) — the ONE
 * available-width class authority.
 *
 * Classes are computed from the ACTUAL available width after status bars,
 * navigation bars, display cutouts and application padding — NEVER from the
 * physical pixel resolution.
 *
 *   COMPACT: under 380 dp   (the Xiaomi 11 Lite 5G NE qualifies here)
 *   MEDIUM:  380–420 dp
 *   LARGE:   above 420 dp
 */
enum class ConsoleWindowWidthClass {
    COMPACT,
    MEDIUM,
    LARGE,
}

/**
 * Measures the available width inside the current layout constraints
 * (already reduced by insets/padding at the call site) and classifies it.
 */
@Composable
fun consoleWindowWidthClass(availableWidthDp: Float): ConsoleWindowWidthClass = classifyWidth(availableWidthDp)

/** Pure classifier (JVM-testable): §19 class boundaries in dp. */
fun classifyWidth(availableWidthDp: Float): ConsoleWindowWidthClass = when {
    availableWidthDp < 380f -> ConsoleWindowWidthClass.COMPACT
    availableWidthDp <= 420f -> ConsoleWindowWidthClass.MEDIUM
    else -> ConsoleWindowWidthClass.LARGE
}

/** Convenience for screens that only know the pixel width (still density-correct). */
@Composable
fun consoleWindowWidthClassPx(availableWidthPx: Int): ConsoleWindowWidthClass {
    val density = LocalDensity.current
    return consoleWindowWidthClass(availableWidthPx / density.density)
}

/** True when the measured text of an action cannot fit one row at this width. */
fun ConsoleWindowWidthClass.maxActionsPerRow(): Int = when (this) {
    ConsoleWindowWidthClass.COMPACT -> 1
    ConsoleWindowWidthClass.MEDIUM -> 2
    ConsoleWindowWidthClass.LARGE -> 3
}
