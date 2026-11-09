package com.hermestower.voiceconsole.ui.theme

import androidx.compose.ui.graphics.toArgb
import com.hermestower.voiceconsole.ui.monumental.MonumentalMarble
import com.hermestower.voiceconsole.ui.monumental.MonumentalObsidian
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.pow

/**
 * GRAPH 4.14 §15/§17/§19 — THEME AUTHORITY + Marble Light contrast pins.
 *
 * FINDING-04/05 repair proof: the monumental layer is THEME-AWARE and every
 * ordinary text/icon pair on the marble surfaces measures ≥4.5:1 (text) or
 * ≥3:1 (icons). The obsidian identity is pinned unchanged (regression).
 */
class ThemeAuthorityTest {

    private fun argb(c: androidx.compose.ui.graphics.Color): Long = c.toArgb().toLong() and 0xFFFFFFFFL

    private fun lum(argb: Long): Double {
        fun ch(v: Long): Double {
            val s = v / 255.0
            return if (s <= 0.03928) s / 12.92 else ((s + 0.055) / 1.055).pow(2.4)
        }
        val r = ch((argb shr 16) and 0xFF)
        val g = ch((argb shr 8) and 0xFF)
        val b = ch(argb and 0xFF)
        return 0.2126 * r + 0.7152 * g + 0.0722 * b
    }

    private fun ratio(fg: Long, bg: Long): Double {
        val l1 = lum(fg); val l2 = lum(bg)
        return (maxOf(l1, l2) + 0.05) / (minOf(l1, l2) + 0.05)
    }

    @Test
    fun `the monumental layer is theme-aware — marble is a distinct palette`() {
        // The FINDING-04 root (Theme.kt hard pin) is gone: two palettes exist.
        assertNotEquals(MonumentalMarble.obsidianDeep, MonumentalObsidian.obsidianDeep)
        assertNotEquals(MonumentalMarble.ink, MonumentalObsidian.ink)
        assertTrue("marble stage must be light", lum(argb(MonumentalMarble.obsidianDeep)) > 0.5)
        assertTrue("obsidian stage must be dark", lum(argb(MonumentalObsidian.obsidianDeep)) < 0.1)
    }

    @Test
    fun `marble ink family measures at least 4_5 on the marble surfaces`() {
        val surfaces = listOf(
            argb(MonumentalMarble.obsidianDeep),
            argb(MonumentalMarble.obsidian),
            argb(MonumentalMarble.obsidianPanel),
            argb(MonumentalMarble.obsidianRaised),
        )
        for (s in surfaces) {
            assertTrue(
                "ink ≥4.5 on 0x%06X".format(s and 0xFFFFFF),
                ratio(argb(MonumentalMarble.ink), s) >= 4.5,
            )
            assertTrue(
                "inkSecondary ≥4.5 on 0x%06X".format(s and 0xFFFFFF),
                ratio(argb(MonumentalMarble.inkSecondary), s) >= 4.5,
            )
            assertTrue(
                "inkDim ≥4.5 on 0x%06X".format(s and 0xFFFFFF),
                ratio(argb(MonumentalMarble.inkDim), s) >= 4.5,
            )
        }
    }

    @Test
    fun `marble energy accents measure at least 3_0 as icons and 4_5 as text`() {
        val stage = argb(MonumentalMarble.obsidianDeep)
        val panel = argb(MonumentalMarble.obsidianPanel)
        // Icons/dots ≥3:1 (§19 icons threshold):
        for (accent in listOf(
            argb(MonumentalMarble.cyanCore),
            argb(MonumentalMarble.amber),
            argb(MonumentalMarble.redBronze),
            argb(MonumentalMarble.bronzeLight),
        )) {
            assertTrue(
                "accent icon ≥3.0 on stage 0x%06X".format(stage and 0xFFFFFF),
                ratio(accent, stage) >= 3.0,
            )
            assertTrue(
                "accent icon ≥3.0 on panel 0x%06X".format(panel and 0xFFFFFF),
                ratio(accent, panel) >= 3.0,
            )
        }
        // Text-capable accents ≥4.5:1 (SIMULAZIONE label, error text):
        assertTrue("amber as text ≥4.5", ratio(argb(MonumentalMarble.amber), panel) >= 4.5)
        assertTrue("redBronzeText ≥4.5", ratio(argb(MonumentalMarble.redBronzeText), panel) >= 4.5)
        assertTrue("gold as text ≥4.5", ratio(argb(MonumentalMarble.gold), panel) >= 4.5)
    }

    @Test
    fun `marble semantic text tokens measure at least 4_5 on their surfaces`() {
        val bg = argb(MarbleBackground)
        val surface = argb(MarbleSurface)
        val variant = argb(MarbleSurfaceVariant)
        for (s in listOf(bg, surface, variant)) {
            assertTrue(ratio(argb(MarbleLightColors.textPrimary), s) >= 4.5)
            assertTrue(ratio(argb(MarbleLightColors.textSecondary), s) >= 4.5)
            assertTrue(ratio(argb(MarbleLightColors.textTertiaryReadable), s) >= 4.5)
        }
    }

    @Test
    fun `obsidian identity is pinned unchanged (regression guard)`() {
        // The 4.13 semantic authority (§7 anchors) must survive the marble work.
        assertTrue(ratio(argb(ObsidianNightColors.textPrimary), argb(ObsidianBackground)) >= 4.5)
        assertTrue(ratio(argb(ObsidianNightColors.textSecondary), argb(ObsidianSurface)) >= 4.5)
        assertTrue(ratio(argb(ObsidianNightColors.textTertiaryReadable), argb(ObsidianSurfaceVariant)) >= 4.5)
        assertTrue("obsidian ink stays light", lum(argb(MonumentalObsidian.ink)) > 0.5)
    }
}
