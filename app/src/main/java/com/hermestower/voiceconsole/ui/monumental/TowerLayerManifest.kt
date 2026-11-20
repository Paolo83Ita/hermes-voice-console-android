package com.hermestower.voiceconsole.ui.monumental

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.min

/**
 * GRAPH A4R NODE A4 — the ONE master-canvas layer manifest for the reference
 * Tower visual (Owner finding A4R-P2-004: the central circular portal must be
 * aligned to the architecture, NEVER with a device-specific pixel offset).
 *
 * Master canvas: 680 x 963 (the §37 BALANCED normalized stage; the LOW tier is
 * its proportional 460 x 652 derivative — every spec below is expressed on the
 * master canvas and scales uniformly).
 *
 * For EVERY layer the manifest defines: x, y, width, height, pivotX, pivotY,
 * zIndex, variant and anchor — one table, one truth. The shared transformation
 * is exactly the mission formula:
 *
 *     scale = min(containerWidth / 680, containerHeight / 963)
 *
 * All layers therefore share: the SAME source variant, the SAME scale, the
 * SAME origin (the canvas rect), the SAME inset policy (no per-layer padding),
 * explicit clipping (the canvas rect clips) and a deterministic z-order.
 *
 * Provenance: the boxes are the §37 extraction fractions of
 * docs/visual/tower414/ASSET_MANIFEST.json expressed in master-canvas pixels
 * (e.g. ring_outer 0.180/0.300/0.640/0.310 -> 122/289/435/298). The C12
 * masked-NCC measurement proved these boxes pixel-true on their baked copies
 * (drift <= 1.2dp, NCC >= 0.96) — the manifest keeps that truth and adds the
 * ONE shared contain transform the mission prescribes.
 */
object TowerLayerManifest {

    /** The master canvas every layer is specified on. */
    const val CANVAS_WIDTH = 680f
    const val CANVAS_HEIGHT = 963f

    /** Deterministic draw order (bottom -> top). Canvas dynamics draw above 40. */
    const val Z_ARCHITECTURE = 0
    const val Z_CROWN_SIGN = 10
    const val Z_RING_OUTER = 20
    const val Z_RING_MIDDLE = 21
    const val Z_RING_INNER = 22
    const val Z_PORTAL = 30
    const val Z_ENERGY_MASK = 40

    /** One layer spec: everything the mission requires, per layer. */
    data class TowerLayerSpec(
        val layer: String,
        val x: Int,
        val y: Int,
        val width: Int,
        val height: Int,
        /** Rotation pivot in canvas px — nominal center; raster layers NEVER rotate. */
        val pivotX: Int,
        val pivotY: Int,
        val zIndex: Int,
        /** Resource stem: the tier qualifier is appended by the resolver. */
        val variant: String,
        /** How the layer box anchors inside the canvas rect. */
        val anchor: Anchor,
    )

    enum class Anchor { TOP_START, CENTER }

    val architecture = TowerLayerSpec("tower414_architecture", 0, 0, 680, 963, 340, 481, Z_ARCHITECTURE, "tower414_architecture", Anchor.TOP_START)
    val crownSign = TowerLayerSpec("tower414_crown_sign", 102, 29, 476, 187, 340, 122, Z_CROWN_SIGN, "tower414_crown_sign", Anchor.TOP_START)
    val ringOuter = TowerLayerSpec("tower414_ring_outer", 122, 289, 435, 298, 339, 438, Z_RING_OUTER, "tower414_ring_outer", Anchor.TOP_START)
    val ringMiddle = TowerLayerSpec("tower414_ring_middle", 180, 327, 320, 226, 340, 440, Z_RING_MIDDLE, "tower414_ring_middle", Anchor.TOP_START)
    val ringInner = TowerLayerSpec("tower414_ring_inner", 252, 371, 177, 149, 340, 445, Z_RING_INNER, "tower414_ring_inner", Anchor.TOP_START)
    val portal = TowerLayerSpec("tower414_portal", 204, 679, 272, 212, 340, 785, Z_PORTAL, "tower414_portal", Anchor.TOP_START)
    val energyMask = TowerLayerSpec("tower414_energy_mask", 0, 0, 680, 963, 340, 481, Z_ENERGY_MASK, "tower414_energy_mask", Anchor.TOP_START)

    /** Deterministic bottom-to-top order (the manifest is the z-authority). */
    val layersInDrawOrder: List<TowerLayerSpec> = listOf(
        architecture, crownSign, ringOuter, ringMiddle, ringInner, portal, energyMask,
    )

    // ---- the ONE shared transformation (mission formula) ------------------------------

    /**
     * Contain scale: the whole monument is always visible, no top-clip, no
     * per-device offsets. Returns (scale, rectWidthDp, rectHeightDp).
     */
    fun containTransform(containerWidth: Dp, containerHeight: Dp): Triple<Float, Dp, Dp> {
        val s = min(
            containerWidth.value / CANVAS_WIDTH,
            containerHeight.value / CANVAS_HEIGHT,
        )
        return Triple(s, (CANVAS_WIDTH * s).dp, (CANVAS_HEIGHT * s).dp)
    }

    // ---- dynamic anchors (Canvas layers) in canvas-normalized units -------------------

    /** Measured ring band center (ring_outer box center, C12): (0.5, 0.455) of canvas. */
    const val RING_CENTER_X_FRAC = 0.5f
    const val RING_CENTER_Y_FRAC = 0.455f
    /** ring_outer outer edge radius on the canvas: 0.32 of canvas width. */
    const val RING_RADIUS_W_FRAC = 0.32f
    /** Portal glow center (§39): (0.5, 0.815) of canvas. */
    const val PORTAL_CENTER_X_FRAC = 0.5f
    const val PORTAL_CENTER_Y_FRAC = 0.815f
}
