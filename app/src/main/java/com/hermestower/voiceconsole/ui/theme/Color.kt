package com.hermestower.voiceconsole.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

// ==== MARBLE LIGHT (docs/VISUAL_DIRECTION.md §2) ====
val MarbleBackground = Color(0xFFF2EFE7)
val MarbleSurface = Color(0xFFFAF7F0)
val MarbleSurfaceVariant = Color(0xFFE9E4D8)
val MarbleOutline = Color(0xFFC9BFA9)
val MarbleInk = Color(0xFF201E19)
val MarbleInkSecondary = Color(0xFF5A554A)
val MarbleBronze = Color(0xFF7A5C2E)
// GRAPH 4.12 §19: deepened for ≥4.5:1 on every marble surface (was 0xFFB08D4C at 2.45–2.90:1).
val MarbleGold = Color(0xFF7E612F)
// GRAPH 4.12 §19: deepened for ≥4.5:1 (was 0xFF0E7490 at 4.22:1 on surfaceVariant).
val MarbleEnergy = Color(0xFF0C6478)
val MarbleEnergyBright = Color(0xFF06B6D4)
val MarbleEnergyGlow = Color(0xFF7DD3E8)
val MarbleStone = Color(0xFFD9D4C8)
val MarbleStoneShade = Color(0xFFC2BBAB)
// GRAPH 4.12 §19: deepened for ≥4.5:1 (was 0xFF2A7654 at 4.34:1).
val MarbleOk = Color(0xFF276B4D)
// GRAPH 4.12 §19: deepened for ≥4.5:1 (was 0xFF9A5B12 at 4.27:1).
val MarbleWarn = Color(0xFF8A5210)
val MarbleError = Color(0xFFB3261E)

// ==== OBSIDIAN NIGHT (default identity, D-009) ====
val ObsidianBackground = Color(0xFF0B0D10)
val ObsidianSurface = Color(0xFF12151A)
val ObsidianSurfaceVariant = Color(0xFF1B2027)
val ObsidianOutline = Color(0xFF2E3640)
val ObsidianInk = Color(0xFFE8E6E1)
val ObsidianInkSecondary = Color(0xFFA7A39A)
val ObsidianBronze = Color(0xFFC08A3E)
val ObsidianGold = Color(0xFFD4A24E)
val ObsidianEnergy = Color(0xFF22D3EE)
val ObsidianEnergyBright = Color(0xFF35D6FF)
val ObsidianEnergyGlow = Color(0xFF164E5F)
val ObsidianStone = Color(0xFF222933)
val ObsidianStoneLight = Color(0xFF333D4A)
val ObsidianOk = Color(0xFF34D399)
val ObsidianWarn = Color(0xFFF59E0B)
val ObsidianError = Color(0xFFF87171)

/**
 * Identity colors beyond the Material scheme: the Tower's materials and energy.
 * UI reads these through hermesColors() so the theme swap never forks components.
 *
 * GRAPH 4.13 §6: the SEMANTIC COLOR AUTHORITY lives here too — 25 typed tokens
 * (text/icon/surface/outline) resolved per theme. All ordinary prose, icons,
 * interactive surfaces and outlines MUST consume these; raw Color literals in
 * ordinary screen components are banned (decoration-only, documented).
 * §5 disabled contract: disabled label ≥3.0 with ≥2 cues (color+border+fill);
 * enabled informative text never renders in disabled colors.
 */
@Immutable
data class HermesColors(
    val background: Color,
    val surface: Color,
    val surfaceVariant: Color,
    val outline: Color,
    val ink: Color,
    val inkSecondary: Color,
    val bronze: Color,
    val gold: Color,
    val energy: Color,
    val energyBright: Color,
    val energyGlow: Color,
    val stone: Color,
    val stoneAlt: Color,
    val ok: Color,
    val warn: Color,
    val danger: Color,
    val onAccent: Color,
    // ---- GRAPH 4.13 §6 semantic authority ----
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiaryReadable: Color,
    val textDisabled: Color,
    val textOnAccent: Color,
    val textOnSelected: Color,
    val textTechnical: Color,
    val textSuccess: Color,
    val textWarning: Color,
    val textError: Color,
    val textInfo: Color,
    val iconPrimary: Color,
    val iconSecondary: Color,
    val iconDisabled: Color,
    val iconOnAccent: Color,
    val surfaceBase: Color,
    val surfaceRaised: Color,
    val surfaceInteractive: Color,
    val surfaceSelected: Color,
    val surfaceDisabled: Color,
    val surfaceError: Color,
    val outlinePrimary: Color,
    val outlineSecondary: Color,
    val outlineFocus: Color,
    val outlineDisabled: Color,
)

val MarbleLightColors = HermesColors(
    background = MarbleBackground, surface = MarbleSurface, surfaceVariant = MarbleSurfaceVariant,
    outline = MarbleOutline, ink = MarbleInk, inkSecondary = MarbleInkSecondary,
    bronze = MarbleBronze, gold = MarbleGold,
    energy = MarbleEnergy, energyBright = MarbleEnergyBright, energyGlow = MarbleEnergyGlow,
    stone = MarbleStone, stoneAlt = MarbleStoneShade,
    ok = MarbleOk, warn = MarbleWarn, danger = MarbleError,
    onAccent = Color(0xFFFFF8EC),
    // §6 semantics — measured on the marble surfaces that host the text
    textPrimary = Color(0xFF201E19),
    textSecondary = Color(0xFF5A554A),
    textTertiaryReadable = Color(0xFF6B655A),
    textDisabled = Color(0xFF857E70),
    textOnAccent = Color(0xFFFFF8EC),
    textOnSelected = Color(0xFF3F2F14),
    textTechnical = Color(0xFF6B655A),
    textSuccess = Color(0xFF276B4D),
    textWarning = Color(0xFF8A5210),
    textError = Color(0xFFB3261E),
    textInfo = Color(0xFF0C6478),
    iconPrimary = Color(0xFF7A5C2E),
    iconSecondary = Color(0xFF7A5C2E),
    iconDisabled = Color(0xFF857E70),
    iconOnAccent = Color(0xFFFFF8EC),
    surfaceBase = MarbleBackground,
    surfaceRaised = MarbleSurface,
    surfaceInteractive = MarbleSurfaceVariant,
    surfaceSelected = Color(0xFFE8D9BC),
    surfaceDisabled = MarbleSurfaceVariant,
    surfaceError = Color(0xFFF6E3E1),
    outlinePrimary = MarbleOutline,
    outlineSecondary = MarbleStoneShade,
    outlineFocus = MarbleBronze,
    outlineDisabled = MarbleStone,
)

val ObsidianNightColors = HermesColors(
    background = ObsidianBackground, surface = ObsidianSurface, surfaceVariant = ObsidianSurfaceVariant,
    outline = ObsidianOutline, ink = ObsidianInk, inkSecondary = ObsidianInkSecondary,
    bronze = ObsidianBronze, gold = ObsidianGold,
    energy = ObsidianEnergy, energyBright = ObsidianEnergyBright, energyGlow = ObsidianEnergyGlow,
    stone = ObsidianStone, stoneAlt = ObsidianStoneLight,
    ok = ObsidianOk, warn = ObsidianWarn, danger = ObsidianError,
    onAccent = Color(0xFF120C04),
    // §6 semantics — §7 anchors (warm ivory / light taupe / readable technical gray),
    // measured ≥4.5 (text), ≥3.0 (icons/focus/disabled) on every obsidian surface
    textPrimary = Color(0xFFE8E1D5),
    textSecondary = Color(0xFFBEB5A6),
    textTertiaryReadable = Color(0xFFAAA69F),
    textDisabled = Color(0xFF9A9488),
    textOnAccent = Color(0xFF120C04),
    textOnSelected = Color(0xFFF0D9A0),
    textTechnical = Color(0xFFAAA69F),
    textSuccess = Color(0xFF34D399),
    textWarning = Color(0xFFF59E0B),
    textError = Color(0xFFF87171),
    textInfo = Color(0xFF22D3EE),
    iconPrimary = Color(0xFFD4A24E),
    iconSecondary = Color(0xFFB98C4A),
    iconDisabled = Color(0xFF9A9488),
    iconOnAccent = Color(0xFF120C04),
    surfaceBase = ObsidianBackground,
    surfaceRaised = Color(0xFF131824),
    surfaceInteractive = ObsidianSurfaceVariant,
    surfaceSelected = Color(0xFF2C261B),
    surfaceDisabled = Color(0xFF2D2E32),
    surfaceError = Color(0xFF2A0B09),
    outlinePrimary = Color(0xFF8A6A38),
    outlineSecondary = Color(0xFF3E2F18),
    outlineFocus = Color(0xFFD9B36A),
    outlineDisabled = Color(0xFF4A453C),
)

fun marbleMaterialScheme(): ColorScheme = lightColorScheme(
    background = MarbleBackground,
    onBackground = MarbleInk,
    surface = MarbleSurface,
    onSurface = MarbleInk,
    surfaceVariant = MarbleSurfaceVariant,
    onSurfaceVariant = MarbleInkSecondary,
    outline = MarbleOutline,
    primary = MarbleBronze,
    onPrimary = Color(0xFFFFF8EC),
    primaryContainer = Color(0xFFE8D9BC),
    onPrimaryContainer = Color(0xFF3F2F14),
    secondary = MarbleEnergy,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD3EBF2),
    onSecondaryContainer = Color(0xFF0B3D4C),
    error = MarbleError,
    onError = Color(0xFFFFF6F5),
)

fun obsidianMaterialScheme(): ColorScheme = darkColorScheme(
    background = ObsidianBackground,
    onBackground = ObsidianInk,
    surface = ObsidianSurface,
    onSurface = ObsidianInk,
    surfaceVariant = ObsidianSurfaceVariant,
    onSurfaceVariant = ObsidianInkSecondary,
    outline = ObsidianOutline,
    primary = ObsidianBronze,
    onPrimary = Color(0xFF120C04),
    primaryContainer = Color(0xFF3A2C14),
    onPrimaryContainer = Color(0xFFEED9AE),
    secondary = ObsidianEnergy,
    onSecondary = Color(0xFF06222A),
    secondaryContainer = Color(0xFF0E3A46),
    onSecondaryContainer = Color(0xFFB8F0FA),
    error = ObsidianError,
    onError = Color(0xFF2A0B09),
)
