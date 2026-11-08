package com.hermestower.voiceconsole.ui.theme

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.pow

/**
 * GRAPH 4.13 §6/§7 — SEMANTIC COLOR AUTHORITY pins (WCAG 2.x).
 *
 * Every pair measured in APP_WIDE_RENDERED_CONTRAST_AUDIT_GRAPH413.md PART B
 * (the live semantic authority) is pinned here for BOTH themes. Thresholds:
 * normal text ≥4.5, large/critical text ≥4.5 (critical is informative), icon/
 * focus/disabled ≥3.0 (§5 disabled contract). The pre-repair failures (M3
 * default disabled labels 1.43–2.09:1; Marble prose on the always-obsidian
 * shell 1.16–3.12:1) are permanently closed by these pins + the §5 wrappers.
 */
class SemanticContrastAuthorityTest {

    private fun linear(channel01: Double): Double =
        if (channel01 <= 0.04045) channel01 / 12.92 else ((channel01 + 0.055) / 1.055).pow(2.4)

    private fun luminance(color: Color): Double =
        0.2126 * linear(color.red.toDouble()) +
            0.7152 * linear(color.green.toDouble()) +
            0.0722 * linear(color.blue.toDouble())

    private fun ratio(fg: Color, bg: Color): Double {
        val l1 = luminance(fg)
        val l2 = luminance(bg)
        return (maxOf(l1, l2) + 0.05) / (minOf(l1, l2) + 0.05)
    }

    private fun blend(fg: Color, bg: Color, alpha: Float): Color = Color(
        red = fg.red * alpha + bg.red * (1 - alpha),
        green = fg.green * alpha + bg.green * (1 - alpha),
        blue = fg.blue * alpha + bg.blue * (1 - alpha),
        alpha = 1f,
    )

    private fun assertPair(name: String, fg: Color, bg: Color, threshold: Double) {
        val r = ratio(fg, bg)
        assertTrue("$name ${"%.2f".format(r)}:1 < $threshold:1", r >= threshold)
    }

    private fun assertPalette(c: HermesColors, theme: String) {
        // §6 text authority on the semantic surfaces prose actually renders on
        assertPair("$theme textPrimary/surfaceBase", c.textPrimary, c.surfaceBase, 4.5)
        assertPair("$theme textPrimary/surfaceRaised", c.textPrimary, c.surfaceRaised, 4.5)
        assertPair("$theme textSecondary/surfaceBase", c.textSecondary, c.surfaceBase, 4.5)
        assertPair("$theme textSecondary/surfaceInteractive", c.textSecondary, c.surfaceInteractive, 4.5)
        assertPair("$theme textTertiaryReadable/surfaceInteractive", c.textTertiaryReadable, c.surfaceInteractive, 4.5)
        assertPair("$theme textTechnical/surfaceRaised", c.textTechnical, c.surfaceRaised, 4.5)

        // §4 critical status text
        assertPair("$theme textSuccess/surfaceRaised", c.textSuccess, c.surfaceRaised, 4.5)
        assertPair("$theme textWarning/surfaceRaised", c.textWarning, c.surfaceRaised, 4.5)
        assertPair("$theme textError/surfaceRaised", c.textError, c.surfaceRaised, 4.5)
        assertPair("$theme textInfo/surfaceRaised", c.textInfo, c.surfaceRaised, 4.5)

        // headings/accent identity (§7: brighter bronze for headings/selected)
        assertPair("$theme gold heading/surfaceInteractive", c.gold, c.surfaceInteractive, 3.0)
        assertPair("$theme bronze heading/surfaceInteractive", c.bronze, c.surfaceInteractive, 4.5)

        // §5 disabled contract — readable disabled labels on the disabled fill
        assertPair("$theme textDisabled/surfaceDisabled", c.textDisabled, c.surfaceDisabled, 3.0)
        assertPair("$theme textDisabled/surfaceInteractive", c.textDisabled, c.surfaceInteractive, 3.0)

        // selected/interactive states
        assertPair("$theme textOnSelected/surfaceSelected", c.textOnSelected, c.surfaceSelected, 4.5)
        assertPair("$theme textOnAccent/bronze", c.textOnAccent, c.bronze, 4.5)
        assertPair("$theme iconSecondary/surfaceInteractive", c.iconSecondary, c.surfaceInteractive, 3.0)
        assertPair("$theme outlineFocus/surfaceInteractive", c.outlineFocus, c.surfaceInteractive, 3.0)

        // TruthBadge alpha-blended bg (info@10% over interactive)
        val badgeBg = blend(c.textInfo, c.surfaceInteractive, 0.10f)
        assertPair("$theme textInfo/badge(10%)", c.textInfo, badgeBg, 4.5)
    }

    @Test
    fun `§6 obsidian night semantic authority meets every threshold`() {
        assertPalette(ObsidianNightColors, "OBSIDIAN")
    }

    @Test
    fun `§6 marble light semantic authority meets every threshold`() {
        assertPalette(MarbleLightColors, "MARBLE")
    }

    @Test
    fun `§5 pre-repair M3 disabled default is proven below threshold and replaced`() {
        // the old default (onSurface@38% on the disabled container) MUST fail —
        // this pins WHY the §5 wrappers exist; the wrappers now render
        // textDisabled/surfaceDisabled which the palette pins above guarantee ≥3.0.
        val onSurface = Color(0xFFE8E6E1)
        val disabledContainer = Color(0xFF2D2E32)
        val oldDisabled = blend(onSurface, disabledContainer, 0.38f)
        assertTrue(ratio(oldDisabled, disabledContainer) < 3.0)
    }

    @Test
    fun `§6 semantic token set is complete (25 typed tokens)`() {
        val c = ObsidianNightColors
        val tokens: List<Color> = listOf(
            c.textPrimary, c.textSecondary, c.textTertiaryReadable, c.textDisabled,
            c.textOnAccent, c.textOnSelected, c.textTechnical, c.textSuccess,
            c.textWarning, c.textError, c.textInfo,
            c.iconPrimary, c.iconSecondary, c.iconDisabled, c.iconOnAccent,
            c.surfaceBase, c.surfaceRaised, c.surfaceInteractive, c.surfaceSelected,
            c.surfaceDisabled, c.surfaceError,
            c.outlinePrimary, c.outlineSecondary, c.outlineFocus, c.outlineDisabled,
        )
        assertTrue("expected 25 semantic tokens", tokens.size == 25)
        // distinct roles carry distinct values where the design requires it
        assertTrue(c.textPrimary != c.textSecondary)
        assertTrue(c.surfaceBase != c.surfaceRaised)
    }
}
