package com.hermestower.voiceconsole.ui

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.pow

/**
 * GRAPH 4.12 §18/§19 — CONTRAST pins (WCAG 2.x relative-luminance ratios).
 * The Owner physically reported unreadable low-contrast text (PHYS-FAIL-02);
 * these pins make the repaired tokens PERMANENT.
 */
class ContrastRatioTest {

    private fun linear(channel01: Float): Double {
        val v = channel01.toDouble()
        return if (v <= 0.04045) v / 12.92 else ((v + 0.055) / 1.055).pow(2.4)
    }

    private fun luminance(color: Color): Double =
        0.2126 * linear(color.red) +
            0.7152 * linear(color.green) +
            0.0722 * linear(color.blue)

    private fun ratio(fg: Color, bg: Color): Double {
        val l1 = luminance(fg)
        val l2 = luminance(bg)
        val hi = maxOf(l1, l2)
        val lo = minOf(l1, l2)
        return (hi + 0.05) / (lo + 0.05)
    }

    // the surfaces ordinary text lives on (MonumentalColors obsidian identity)
    private val obsidianDeep = Color(0xFF04060A)
    private val obsidianPanel = Color(0xFF0D1119)
    private val obsidianRaised = Color(0xFF131824)

    @Test
    fun `§19 textMuted (inkDim) meets 4_5 on every obsidian surface`() {
        val inkDim = Color(0xFF8A8375) // repaired value (was 0xFF6E675C at 3.17–3.63)
        for (bg in listOf(obsidianDeep, obsidianPanel, obsidianRaised)) {
            assertTrue(
                "inkDim ${ratio(inkDim, bg)} on $bg < 4.5",
                ratio(inkDim, bg) >= 4.5,
            )
        }
    }

    @Test
    fun `§19 textPrimary and textSecondary meet 4_5 everywhere`() {
        val ink = Color(0xFFEDE6D8)
        val inkSecondary = Color(0xFFA89F8D)
        for (bg in listOf(obsidianDeep, obsidianPanel, obsidianRaised)) {
            assertTrue(ratio(ink, bg) >= 4.5)
            assertTrue(ratio(inkSecondary, bg) >= 4.5)
        }
    }

    @Test
    fun `§19 redBronzeText meets 4_5 on every obsidian surface`() {
        val redBronzeText = Color(0xFFDE7A5E)
        for (bg in listOf(obsidianDeep, obsidianPanel, obsidianRaised)) {
            assertTrue(
                "redBronzeText ${ratio(redBronzeText, bg)} on $bg < 4.5",
                ratio(redBronzeText, bg) >= 4.5,
            )
        }
    }

    @Test
    fun `§18 essential icon and control colors meet 3_0`() {
        val cyanCore = Color(0xFF35E0FF)
        val gold = Color(0xFFD9B36A)
        val bronzeLight = Color(0xFFB98C4A)
        for (bg in listOf(obsidianDeep, obsidianPanel)) {
            assertTrue(ratio(cyanCore, bg) >= 3.0)
            assertTrue(ratio(gold, bg) >= 3.0)
            assertTrue(ratio(bronzeLight, bg) >= 3.0)
        }
    }

    @Test
    fun `the OLD inkDim value would FAIL (regression guard on the repair itself)`() {
        val old = Color(0xFF6E675C)
        assertTrue(ratio(old, obsidianPanel) < 4.5) // the measured PHYS-FAIL-02 value
    }
}
