package com.hermestower.voiceconsole.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import com.hermestower.voiceconsole.ui.monumental.LocalMonumentalColors
import com.hermestower.voiceconsole.ui.monumental.MonumentalMarble
import com.hermestower.voiceconsole.ui.monumental.MonumentalObsidian

/** Accessor for identity colors beyond the Material scheme. */
val LocalHermesColors = staticCompositionLocalOf { ObsidianNightColors }

val hermesColors: HermesColors
    @Composable get() = LocalHermesColors.current

/**
 * GRAPH 4.14 §15 — ONE THEME AUTHORITY for the whole app.
 *
 * The theme now provides BOTH color systems per theme: the legacy [HermesColors]
 * AND the authoritative Monumental tokens are THEME-AWARE (MonumentalMarble in
 * light, MonumentalObsidian in dark). The old hard pin
 * (`LocalMonumentalColors provides MonumentalObsidian` unconditionally) was the
 * root cause of FINDING-04/05 — Marble Light could not reach the Console or the
 * shell. No separate Console theme authority exists; every consumer reads the
 * same composition locals.
 */
@Composable
fun HermesTheme(
    darkTheme: Boolean,
    reducedMotion: Boolean = false,
    animationIntensity: Float = 1f,
    content: @Composable () -> Unit,
) {
    val colors = if (darkTheme) ObsidianNightColors else MarbleLightColors
    val scheme = if (darkTheme) obsidianMaterialScheme() else marbleMaterialScheme()
    val monumental = if (darkTheme) MonumentalObsidian else MonumentalMarble
    CompositionLocalProvider(
        LocalHermesColors provides colors,
        LocalMonumentalColors provides monumental,
    ) {
        MaterialTheme(
            colorScheme = scheme,
            typography = HermesTypography,
            shapes = HermesShapes,
            content = content,
        )
    }
}

/** Convenience for previews/tests. */
object HermesThemeDefaults {
    const val DEFAULT_DARK = true
}
