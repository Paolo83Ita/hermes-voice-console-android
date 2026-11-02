package com.hermestower.voiceconsole.ui.nav

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * GRAPH 4 G4-C07 (§11): required drawer-width matrix. Portrait Xiaomi 11 Lite
 * 5G NE is ~411dp wide; the hard maximum is 2/3 of the available width at
 * every size; the preferred target is 60% with 248dp practical minimum.
 */
class DrawerWidthPolicyTest {

    private fun w(v: Float) = DrawerWidthPolicy.drawerWidthDp(v)

    @Test
    fun `drawer width at 411dp screen (Xiaomi portrait)`() {
        val width = w(411f)
        assertEquals(248f, width, 0.01f) // 60% = 246.6 → practical minimum 248
        assertTrue(width <= 411f * 0.6667f)
    }

    @Test
    fun `drawer width at 393dp screen`() {
        val width = w(393f)
        assertEquals(248f, width, 0.01f)
        assertTrue(width <= 393f * 0.6667f + 0.01f)
    }

    @Test
    fun `drawer width at 360dp screen`() {
        val width = w(360f)
        // 60% = 216 < min 248; hard cap 2/3 = 240 wins over the minimum.
        assertEquals(240f, width, 0.01f)
        assertTrue(width <= 360f * 0.6667f + 0.01f)
    }

    @Test
    fun `drawer width in landscape leaves two thirds for content`() {
        val width = w(800f)
        assertEquals(360f, width, 0.01f) // compact fixed cap, target 280–360
        assertTrue("content area preserved", width <= 800f * 0.6667f)
    }

    @Test
    fun `very narrow screen still respects the two-thirds hard maximum`() {
        val width = w(300f)
        assertTrue(width <= 300f * 0.6667f + 0.01f)
    }

    @Test
    fun `drawer never exceeds 360dp practical maximum`() {
        for (screen in listOf(411f, 480f, 600f, 800f, 1280f)) {
            assertTrue(w(screen) <= DrawerWidthPolicy.MAX_PRACTICAL_DP)
        }
    }

    @Test
    fun `drawer never appears as a full-screen replacement`() {
        for (screen in listOf(240f, 300f, 360f, 393f, 411f, 800f)) {
            val ratio = w(screen) / screen
            assertTrue("width $screen: ratio $ratio > 2/3", ratio <= 0.6667f + 0.001f)
        }
    }
}
