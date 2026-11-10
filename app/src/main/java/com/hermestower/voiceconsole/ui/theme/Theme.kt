package com.hermestower.voiceconsole.ui.theme

import androidx.compose.material3.LocalContentColor
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
        // OWNER PHYSICAL RETEST round 2 (Package 9, F-B residual): the app root is
        // a plain Column with a manual background — NOT a Material Surface/Scaffold —
        // so LocalContentColor kept the Compose default Color.Black. Every Text/Icon
        // without an explicit color= rendered BLACK on the obsidian shell (the
        // Owner's "scritte nere su sfondo scuro poco visibili" in the settings
        // menu; 152 uncolored Text sites across 18 files). The theme authority now
        // provides the theme's own readable ink as the ambient content color;
        // Material components that set their own contentColor (buttons, chips,
        // surfaces) still override it deeper in the tree, so no existing pairing
        // changes — only the UNANCHORED sites flip from invisible black to the
        // theme ink (light theme: 0xFF201E19 marble ink — same look as black on
        // light; dark theme: 0xFFE8E1D5 ivory — the repair).
        LocalContentColor provides colors.textPrimary,
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
