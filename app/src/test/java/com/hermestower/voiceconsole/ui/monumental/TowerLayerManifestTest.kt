package com.hermestower.voiceconsole.ui.monumental

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * A4R NODE A4 — the master-canvas manifest golden test (JVM-runnable in CI).
 *
 * The ON-DEVICE golden screenshot + pixel-diff for the Xiaomi 11 Lite 5G NE
 * stays PHYSICAL_TEST_REQUIRED (Owner visual acceptance, A4R ledger). This
 * suite pins the manifest truth itself: the mission formula, the deterministic
 * z-order, the single-variant coherence, and the box provenance against the
 * §37 extraction fractions.
 */
class TowerLayerManifestTest {

    @Test
    fun `master canvas is 680x963`() {
        assertEquals(680f, TowerLayerManifest.CANVAS_WIDTH)
        assertEquals(963f, TowerLayerManifest.CANVAS_HEIGHT)
    }

    @Test
    fun `contain transform is exactly the mission formula`() {
        // scale = min(cw/680, ch/963)
        val (s, w, h) = TowerLayerManifest.containTransform(1080.dp, 2400.dp)
        assertEquals(minOf(1080f / 680f, 2400f / 963f), s, 1e-6f)
        assertEquals(680f * s, w.value, 1e-3f)
        assertEquals(963f * s, h.value, 1e-3f)
        // width-limited stage: scale rides on width, height fits
        val (s2, w2, h2) = TowerLayerManifest.containTransform(400.dp, 300.dp)
        assertEquals(300f / 963f, s2, 1e-6f)
        assertTrue(w2.value <= 400.dp.value && h2.value <= 300.dp.value)
    }

    @Test
    fun `every layer carries all mission spec fields`() {
        val layers = TowerLayerManifest.layersInDrawOrder
        assertEquals(7, layers.size)
        for (l in layers) {
            assertTrue(l.layer.startsWith("tower414"))
            assertTrue(l.width > 0 && l.height > 0)
            assertTrue(l.x >= 0 && l.y >= 0)
            assertEquals(l.x + l.width / 2, l.pivotX) // nominal center pivot
            assertEquals(l.y + l.height / 2, l.pivotY)
            assertTrue(l.zIndex >= 0)
            assertTrue(l.variant.isNotBlank())
            assertEquals(TowerLayerManifest.Anchor.TOP_START, l.anchor)
        }
    }

    @Test
    fun `z-order is deterministic and architecture sits under every crop`() {
        val order = TowerLayerManifest.layersInDrawOrder
        assertEquals(0, order[0].zIndex) // architecture first
        assertTrue(order.zipWithNext().all { (a, b) -> a.zIndex <= b.zIndex })
        assertEquals(TowerLayerManifest.Z_ENERGY_MASK, order.last().zIndex) // energy tint on top
    }

    @Test
    fun `all crop boxes match the section 37 extraction provenance`() {
        // provenance: docs/visual/tower414/ASSET_MANIFEST.json balanced px sizes
        val expectedSizes = mapOf(
            "tower414_crown_sign" to (476 to 187),
            "tower414_ring_outer" to (435 to 298),
            "tower414_ring_middle" to (320 to 226),
            "tower414_ring_inner" to (177 to 149),
            "tower414_portal" to (272 to 212),
        )
        for (l in TowerLayerManifest.layersInDrawOrder) {
            val expected = expectedSizes[l.layer] ?: continue
            assertEquals(expected.first, l.width)
            assertEquals(expected.second, l.height)
        }
        // origins: the §37 fractions expressed on the 680x963 canvas
        assertEquals(102, TowerLayerManifest.crownSign.x)
        assertEquals(29, TowerLayerManifest.crownSign.y)
        assertEquals(122, TowerLayerManifest.ringOuter.x)
        assertEquals(289, TowerLayerManifest.ringOuter.y)
        assertEquals(180, TowerLayerManifest.ringMiddle.x)
        assertEquals(327, TowerLayerManifest.ringMiddle.y)
        assertEquals(252, TowerLayerManifest.ringInner.x)
        assertEquals(371, TowerLayerManifest.ringInner.y)
        assertEquals(204, TowerLayerManifest.portal.x)
        assertEquals(679, TowerLayerManifest.portal.y)
    }

    @Test
    fun `dynamic anchors come from the same canvas truth`() {
        // ring band center == ring_outer box center (C12 measurement)
        assertEquals(0.5f, TowerLayerManifest.RING_CENTER_X_FRAC)
        assertEquals(0.455f, TowerLayerManifest.RING_CENTER_Y_FRAC)
        assertEquals(0.32f, TowerLayerManifest.RING_RADIUS_W_FRAC)
        assertEquals(0.815f, TowerLayerManifest.PORTAL_CENTER_Y_FRAC)
        // cross-consistency: ring_outer center on the canvas ≈ the band anchor
        val cx = (TowerLayerManifest.ringOuter.x + TowerLayerManifest.ringOuter.width / 2f) / TowerLayerManifest.CANVAS_WIDTH
        assertEquals(TowerLayerManifest.RING_CENTER_X_FRAC, cx, 0.01f)
        val cy = (TowerLayerManifest.ringOuter.y + TowerLayerManifest.ringOuter.height / 2f) / TowerLayerManifest.CANVAS_HEIGHT
        assertEquals(TowerLayerManifest.RING_CENTER_Y_FRAC, cy, 0.01f)
    }

    @Test
    fun `portal is horizontally centered on the canvas (no device offsets)`() {
        val portal = TowerLayerManifest.portal
        val centerX = portal.x + portal.width / 2f
        assertEquals(TowerLayerManifest.CANVAS_WIDTH / 2f, centerX, 1f) // <=1px canvas tolerance
    }
}
