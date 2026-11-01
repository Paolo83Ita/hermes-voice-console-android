package com.hermestower.voiceconsole.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import com.hermestower.voiceconsole.ui.monumental.LocalMonumentalColors
import com.hermestower.voiceconsole.ui.monumental.MonumentalObsidian

/** Accessor for identity colors beyond the Material scheme. */
val LocalHermesColors = staticCompositionLocalOf { ObsidianNightColors }

val hermesColors: HermesColors
    @Composable get() = LocalHermesColors.current

/**
 * GRAPH 3: the theme now provides BOTH color systems — the legacy [HermesColors]
 * (used by components not yet migrated) and the authoritative Monumental tokens.
 * The monumental identity is always dark obsidian; [darkTheme]=false keeps the
 * legacy Marble scheme available but is no longer the console identity.
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
    CompositionLocalProvider(
        LocalHermesColors provides colors,
        LocalMonumentalColors provides MonumentalObsidian,
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
