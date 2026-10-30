package com.hermestower.voiceconsole.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext

/** Accessor for identity colors beyond the Material scheme. */
val LocalHermesColors = staticCompositionLocalOf { ObsidianNightColors }

val hermesColors: HermesColors
    @Composable get() = LocalHermesColors.current

@Composable
fun HermesTheme(
    darkTheme: Boolean,
    reducedMotion: Boolean = false,
    animationIntensity: Float = 1f,
    content: @Composable () -> Unit,
) {
    val colors = if (darkTheme) ObsidianNightColors else MarbleLightColors
    val scheme = if (darkTheme) obsidianMaterialScheme() else marbleMaterialScheme()
    CompositionLocalProvider(LocalHermesColors provides colors) {
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
