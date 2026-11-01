package com.hermestower.voiceconsole.ui.monumental

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.hermestower.voiceconsole.core.state.ConsoleState
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/**
 * GRAPH 3 WAVE 2 — the monumental Oracle Core (L0..L9), the voice identity.
 * One Canvas, one frame clock (same proven pattern as the V0.1 TowerCore, which
 * the frozen-clock E2E harness already handles). All parameters come from
 * [OracleCoreModel]; nothing decorative is independent of console state.
 * L0/L1/L2/L7 static depth may be backed by bitmap layers when the quality
 * budget allows; this Canvas composes the dynamic layers L3..L9 on top and can
 * also draw flat placeholders for any missing bitmap (replaceable-asset contract).
 */
@Composable
fun OracleCore(
    state: ConsoleState,
    reducedMotion: Boolean,
    quality: VisualQuality,
    speakingAmplitude: Float,
    queueCount: Int,
    colors: MonumentalColors,
    modifier: Modifier = Modifier,
    stateLabelOverride: String? = null,
) {
    val budget = QualityBudgets.of(quality)
    val timeMs by produceState(0L, state, reducedMotion, quality) {
        if (reducedMotion || quality == VisualQuality.LOW) {
            value = 0L
        } else {
            val startNanos = withFrameNanos { it }
            while (true) {
                withFrameNanos { now -> value = (now - startNanos) / 1_000_000L }
            }
        }
    }
    // GRAPH 3 hybrid pipeline (ASSET_PIPELINE_DECISION.md): static architectural
    // depth comes from generated WebP layers; all state-driven energy is drawn by
    // one Canvas on top. LOW profile draws the flat Canvas fallback instead.
    androidx.compose.foundation.layout.Box(modifier.semantics {
        contentDescription = stateLabelOverride
            ?: "Oracle Core. ${state.accessibility}"
    }) {
        if (budget.bitmapLayers) {
            androidx.compose.foundation.Image(
                painter = androidx.compose.ui.res.painterResource(com.hermestower.voiceconsole.R.drawable.monumental_l0_sky),
                contentDescription = null,
                contentScale = androidx.compose.ui.layout.ContentScale.FillBounds,
                modifier = Modifier.matchParentSize(),
            )
            androidx.compose.foundation.Image(
                painter = androidx.compose.ui.res.painterResource(com.hermestower.voiceconsole.R.drawable.monumental_l1_temple),
                contentDescription = null,
                contentScale = androidx.compose.ui.layout.ContentScale.FillBounds,
                modifier = Modifier.matchParentSize(),
            )
            androidx.compose.foundation.Image(
                painter = androidx.compose.ui.res.painterResource(com.hermestower.voiceconsole.R.drawable.monumental_l2_colonnade),
                contentDescription = null,
                contentScale = androidx.compose.ui.layout.ContentScale.FillBounds,
                modifier = Modifier.matchParentSize(),
            )
            androidx.compose.foundation.Image(
                painter = androidx.compose.ui.res.painterResource(com.hermestower.voiceconsole.R.drawable.monumental_l7_stairs),
                contentDescription = null,
                contentScale = androidx.compose.ui.layout.ContentScale.FillBounds,
                modifier = Modifier.matchParentSize(),
            )
        }
        Canvas(modifier = Modifier.matchParentSize()) {
            drawOracleCore(
                state = state,
                t = timeMs.toFloat(),
                params = OracleCoreModel.params(state),
                budget = budget,
                amplitude = speakingAmplitude.coerceIn(0f, 1f),
                queueCount = queueCount,
                colors = colors,
                staticLayersFromBitmaps = budget.bitmapLayers,
            )
        }
    }
}

private fun DrawScope.drawOracleCore(
    state: ConsoleState,
    t: Float,
    params: CoreVisualParams,
    budget: QualityBudget,
    amplitude: Float,
    queueCount: Int,
    colors: MonumentalColors,
    staticLayersFromBitmaps: Boolean,
) {
    val w = size.width
    val h = size.height
    val cx = w / 2f
    // design space is 1080 x 1420 with the core center at y=620 — keep the exact
    // normalized mapping so the Canvas dynamics align with the WebP depth layers.
    val cy = h * (620f / 1420f)
    val scale = w / 1080f

    // ---- L0 deep atmosphere (skipped when the WebP layer provides it) ----
    if (!staticLayersFromBitmaps) {
        drawRect(
            Brush.radialGradient(
                listOf(colors.obsidianSheen, colors.obsidian, colors.obsidianDeep),
                center = Offset(cx, cy),
                radius = h * 0.75f,
            ),
        )
    }
    if (budget.glowEnabled && params.portalAlpha > 0.03f) {
        drawCircle(
            Brush.radialGradient(
                listOf(colors.cyanAbyss.copy(alpha = 0.5f * params.portalAlpha), Color.Transparent),
                center = Offset(cx, cy),
                radius = 560f * scale,
            ),
            radius = 560f * scale,
            center = Offset(cx, cy),
        )
    }

    val dim = params.dim
    if (!staticLayersFromBitmaps) {
        // ---- L1 rear temple + crown (Canvas fallback) ----
        drawCrown(colors, cx, cy, w, h, scale, dim)
        // ---- L2 colonnade (Canvas fallback) ----
        drawColonnade(colors, cx, cy, w, h, scale, params.conduitAlpha, dim)
    } else {
        // state-driven lighting overlays on top of the bitmap architecture
        drawColonnadeRimLight(colors, cx, w, scale, params.conduitAlpha, dim)
    }

    // ---- L3 outer bronze ring ----
    val r3 = 440f * scale
    if (params.bronzeBudget > 0.02f) {
        drawCircle(Brush.verticalGradient(
            listOf(colors.bronzeLight, colors.bronzeEdge, colors.bronzeDeep, colors.bronze),
            startY = cy - r3, endY = cy + r3,
        ), r3, Offset(cx, cy), style = Stroke(22f * scale))
        drawCircle(colors.bronzeDeep.copy(alpha = 0.8f), r3 - 18f * scale, Offset(cx, cy), style = Stroke(3f * scale))
        // engraved ticks
        for (i in 0 until 72) {
            val a = Math.toRadians((i * 5).toDouble())
            val ln = if (i % 6 == 0) 24f else 13f
            val tickColor = if (i % 18 == 0) colors.gold else colors.bronzeEdge
            drawLine(
                tickColor.copy(alpha = 0.85f * dim),
                Offset(cx + (r3 + 9f * scale) * cos(a).toFloat(), cy + (r3 + 9f * scale) * sin(a).toFloat()),
                Offset(cx + (r3 + (9f + ln) * scale) * cos(a).toFloat(), cy + (r3 + (9f + ln) * scale) * sin(a).toFloat()),
                strokeWidth = (if (i % 6 == 0) 3f else 2f) * scale,
            )
        }
        // cardinal bosses
        for (i in 0 until 4) {
            val (bx, by) = OracleCoreModel.polar(cx, cy, r3 + 26f * scale, (i * 90 - 90).toFloat())
            drawDiamond(bx, by, 11f * scale, colors.bronze, colors.gold)
        }
    }

    // ---- L4 mechanical rings ----
    val aPhase = OracleCoreModel.ringAPhase(state, t.toLong(), false, VisualQuality.BALANCED)
    val bPhase = OracleCoreModel.ringBPhase(state, t.toLong(), false, VisualQuality.BALANCED)
    val r4a = 352f * scale
    val r4b = 270f * scale
    if (params.bronzeBudget > 0.02f) {
        rotate(aPhase, pivot = Offset(cx, cy)) {
            drawCircle(colors.bronzeDeep, r4a, Offset(cx, cy), style = Stroke(44f * scale))
            drawCircle(colors.bronze.copy(alpha = 0.55f), r4a, Offset(cx, cy), style = Stroke(30f * scale))
            if (budget.particleCount >= 0) {
                val teeth = if (budget.glyphRing) 24 else 12
                for (i in 0 until teeth) {
                    val (tx, ty) = OracleCoreModel.polar(cx, cy, r4a + 24f * scale, (i * 360f / teeth))
                    drawDiamond(tx, ty, 8f * scale, colors.bronze.copy(alpha = 0.8f), colors.bronze)
                }
                for (k in 0 until 8) {
                    val lit = params.conduitAlpha > 0.3f && k % 2 == 0
                    val (wx, wy) = OracleCoreModel.polar(cx, cy, r4a, (k * 45).toFloat())
                    drawRoundRect(
                        (if (lit) colors.cyanCore else colors.gold).copy(alpha = 0.9f * dim),
                        topLeft = Offset(wx - 14f * scale, wy - 9f * scale),
                        size = Size(28f * scale, 18f * scale),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(8f * scale),
                        style = Stroke(1.4f * scale),
                    )
                }
            }
        }
        rotate(bPhase, pivot = Offset(cx, cy)) {
            drawCircle(colors.bronze.copy(alpha = 0.9f), r4b, Offset(cx, cy), style = Stroke(16f * scale))
            for (k in 0 until 12) {
                val lit = params.glyphSequenceUpTo > k / 2 || params.secondTrail && k % 3 == 0
                val (sx2, sy2) = OracleCoreModel.polar(cx, cy, r4b, (k * 30).toFloat())
                drawCircle(
                    (if (lit) colors.cyanCore else colors.bronzeEdge).copy(alpha = (if (lit) 0.9f else 0.4f) * dim + 0.08f),
                    7f * scale,
                    Offset(sx2, sy2),
                )
            }
        }
        drawCircle(colors.gold.copy(alpha = 0.6f * dim + 0.1f), 208f * scale, Offset(cx, cy), style = Stroke(3f * scale))
    }

    // ---- L5 conduits ----
    if (params.conduitAlpha > 0.03f) {
        for (i in 0 until 8) {
            val (x1, y1) = OracleCoreModel.polar(cx, cy, (166f + 26f) * scale, (i * 45 - 90).toFloat())
            val (x2, y2) = OracleCoreModel.polar(cx, cy, r3 - 18f * scale, (i * 45 - 90).toFloat())
            drawLine(
                colors.cyan.copy(alpha = params.conduitAlpha * 0.8f),
                Offset(x1, y1), Offset(x2, y2),
                strokeWidth = 5f * scale,
                cap = StrokeCap.Round,
                pathEffect = if (params.dashedConduits) androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(26f * scale, 14f * scale)) else null,
            )
        }
    }

    // ---- L6 portal + sigil ----
    val r6 = 166f * scale
    val breath = OracleCoreModel.portalBreath(state, t.toLong(), false)
    val amp = if (params.amplitudeGlow) amplitude else 0f
    val glowR = r6 * (1.05f + amp * 0.55f) * breath
    drawCircle(
        Brush.radialGradient(
            listOf(Color(0xFF0C1520), Color(0xFF070B12), Color(0xFF05080D)),
            center = Offset(cx, cy),
            radius = r6,
        ),
        r6, Offset(cx, cy),
    )
    drawCircle(colors.bronze, r6, Offset(cx, cy), style = Stroke(9f * scale))
    if (params.portalAlpha > 0.02f) {
        drawCircle(
            Brush.radialGradient(
                listOf(colors.cyanCore.copy(alpha = (params.portalAlpha * breath).coerceAtMost(1f)), colors.cyanDeep.copy(alpha = params.portalAlpha * 0.5f), Color.Transparent),
                center = Offset(cx, cy),
                radius = glowR,
            ),
            glowR, Offset(cx, cy),
        )
    }
    if (params.shutters) {
        for (i in 0 until 5) {
            drawRect(
                colors.stoneDeep.copy(alpha = 0.92f),
                topLeft = Offset(cx - r6 + (18f + i * 30f) * scale, cy - r6 + 26f * scale),
                size = Size(14f * scale, 2 * r6 - 52f * scale),
            )
        }
        drawCircle(colors.redBronze.copy(alpha = 0.9f), r6 + 16f * scale, Offset(cx, cy), style = Stroke(7f * scale, pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(40f * scale, 12f * scale))))
    } else if (params.halfShutters) {
        for (i in 0 until 3) {
            drawRect(
                colors.stoneDeep.copy(alpha = 0.85f),
                topLeft = Offset(cx - r6 + (30f + i * 38f) * scale, cy - r6 + 26f * scale),
                size = Size(12f * scale, 2 * r6 - 52f * scale),
            )
        }
    }
    val sigAlpha = (0.25f + params.portalAlpha + amp * 0.4f).coerceAtMost(1f) * dim + 0.12f
    drawSigil(colors, cx, cy, scale, sigAlpha)
    if (params.expansionWave) {
        drawCircle(colors.cyanCore.copy(alpha = 0.55f), r6 + 70f * scale, Offset(cx, cy), style = Stroke(6f * scale))
    }
    if (params.convergeWaves) {
        for (i in 0 until 3) {
            val frac = OracleCoreModel.convergeFraction(t.toLong(), i, false)
            val rr = r3 + (1f - frac) * (200f * scale) // converging inward
            drawCircle(
                colors.cyan.copy(alpha = (0.15f + 0.35f * frac)),
                rr, Offset(cx, cy),
                style = Stroke(4f * scale, pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(60f * scale, 90f * scale))),
            )
        }
    }
    if (params.fracture) {
        val fr = colors.redBronze.copy(alpha = 0.9f)
        drawLine(fr, Offset(cx - r3 + 30f * scale, cy - 140f * scale), Offset(cx - 190f * scale, cy - 40f * scale), strokeWidth = 7f * scale, cap = StrokeCap.Round)
        drawLine(fr, Offset(cx - 190f * scale, cy - 40f * scale), Offset(cx - 250f * scale, cy - 90f * scale), strokeWidth = 7f * scale, cap = StrokeCap.Round)
        drawLine(fr, Offset(cx + r3 - 30f * scale, cy + 120f * scale), Offset(cx + 200f * scale, cy + 30f * scale), strokeWidth = 7f * scale, cap = StrokeCap.Round)
        drawCircle(colors.redBronze.copy(alpha = 0.75f), 208f * scale, Offset(cx, cy), style = Stroke(5f * scale, pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(120f * scale, 40f * scale, 60f * scale, 30f * scale))))
    }

    // ---- L7 gateway glow (light layer; the stairs mass comes from the WebP layer
    //      when bitmaps are active, otherwise the full Canvas fallback) ----
    if (!staticLayersFromBitmaps) {
        drawStairs(colors, cx, cy, w, h, scale, params.portalAlpha, dim)
    } else {
        drawGatewayGlow(colors, cx, cy, scale, params.portalAlpha, dim)
    }

    // ---- L8 state seals ----
    if (params.queueSeal && queueCount > 0) {
        val sy = cy - r4b - 96f * scale
        drawRoundRect(colors.stoneDeep, Offset(cx - 46f * scale, sy), Size(92f * scale, 44f * scale), androidx.compose.ui.geometry.CornerRadius(8f * scale))
        drawRoundRect(colors.amber, Offset(cx - 46f * scale, sy), Size(92f * scale, 44f * scale), androidx.compose.ui.geometry.CornerRadius(8f * scale), style = Stroke(2.5f * scale))
    }
    if (params.maintenanceSeal) {
        val (bx, by) = OracleCoreModel.polar(cx, cy, r3 - 30f * scale, -45f)
        drawCircle(colors.stoneDeep, 26f * scale, Offset(bx, by))
        drawCircle(colors.gold, 26f * scale, Offset(bx, by), style = Stroke(2.5f * scale))
    }
    if (params.localCorePlate) {
        val px = cx + 208f * scale + 30f * scale
        drawRoundRect(colors.stoneDeep, Offset(px, cy - 70f * scale), Size(150f * scale, 40f * scale), androidx.compose.ui.geometry.CornerRadius(8f * scale))
        drawRoundRect(colors.localGold, Offset(px, cy - 70f * scale), Size(150f * scale, 40f * scale), androidx.compose.ui.geometry.CornerRadius(8f * scale), style = Stroke(2f * scale))
    }

    // ---- L9 messenger paths ----
    drawMessengers(colors, params, cx, cy, w, scale, dim, t)
}

private fun DrawScope.drawCrown(colors: MonumentalColors, cx: Float, cy: Float, w: Float, h: Float, scale: Float, dim: Float) {
    val topY = cy - 470f * scale
    val crownColor = lerp(colors.obsidian, Color(0xFF0B121C), 0.6f)
    // stepped crown silhouette
    val path = Path().apply {
        moveTo(0f, topY + 118f * scale)
        lineTo(96f * scale, topY + 118f * scale)
        lineTo(96f * scale, topY + 76f * scale)
        lineTo(150f * scale, topY + 76f * scale)
        lineTo(150f * scale, topY + 30f * scale)
        lineTo(w - 150f * scale, topY + 30f * scale)
        lineTo(w - 150f * scale, topY + 76f * scale)
        lineTo(w - 96f * scale, topY + 76f * scale)
        lineTo(w - 96f * scale, topY + 118f * scale)
        lineTo(w, topY + 118f * scale)
        lineTo(w, topY + 180f * scale)
        lineTo(0f, topY + 180f * scale)
        close()
    }
    drawPath(path, crownColor)
    // crenellation teeth
    var tx = 108f * scale
    while (tx < w - 60f * scale) {
        drawRect(
            colors.bronzeDeep.copy(alpha = 0.9f * dim),
            topLeft = Offset(tx, topY + 30f * scale),
            size = Size(34f * scale, 22f * scale),
        )
        tx += 72f * scale
    }
    // turrets with warm windows
    for (turretX in listOf(150f * scale, w - 208f * scale)) {
        drawRect(Color(0xFF0A0F18), Offset(turretX, topY - 24f * scale), Size(58f * scale, 204f * scale))
        drawRect(colors.bronzeDeep, Offset(turretX - 8f * scale, topY - 32f * scale), Size(74f * scale, 12f * scale))
        val lit = if (dim > 0.4f) 0.5f else 0.22f
        for (wy in listOf(140f, 190f, 240f)) {
            drawRoundRect(
                colors.localGold.copy(alpha = lit),
                topLeft = Offset(turretX + 19f * scale, topY - 24f * scale + wy * scale),
                size = Size(20f * scale, 26f * scale),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(9f * scale),
            )
        }
    }
    // pediment sliver
    val ped = Path().apply {
        moveTo(cx - 250f * scale, topY + 180f * scale)
        lineTo(cx, topY + 88f * scale)
        lineTo(cx + 250f * scale, topY + 180f * scale)
        close()
    }
    drawPath(ped, Color(0xFF0C131E))
}

private fun DrawScope.drawColonnade(colors: MonumentalColors, cx: Float, cy: Float, w: Float, h: Float, scale: Float, conduitAlpha: Float, dim: Float) {
    val colTop = cy - 290f * scale
    val colH = 560f * scale
    val xs = listOf(118f, 218f, 318f, w - 174f, w - 274f, w - 374f)
    for (x in xs) {
        drawRect(colors.stoneDeep, Offset(x * scale - 8f * scale, colTop + colH), Size(72f * scale, 16f * scale))
        drawRect(colors.stoneEdge, Offset(x * scale + 4f * scale, colTop + colH - 14f * scale), Size(48f * scale, 12f * scale))
        drawRect(
            Brush.horizontalGradient(
                listOf(colors.stoneDeep, colors.stoneMid, colors.stoneLit.copy(alpha = 0.85f), colors.stoneDeep),
                startX = x * scale, endX = x * scale + 56f * scale,
            ),
            Offset(x * scale, colTop),
            Size(56f * scale, colH),
        )
        for (fi in 1..4) {
            drawRect(
                colors.stoneDeep.copy(alpha = 0.8f),
                Offset(x * scale + fi * 11f * scale, colTop + 6f * scale),
                Size(4f * scale, colH - 24f * scale),
            )
        }
        drawRect(colors.stoneMid, Offset(x * scale - 8f * scale, colTop), Size(72f * scale, 14f * scale))
        drawRect(colors.stoneEdge, Offset(x * scale - 12f * scale, colTop - 14f * scale), Size(80f * scale, 14f * scale))
        val rim = conduitAlpha * 0.55f * dim
        if (rim > 0.02f) {
            val rimX = if (x * scale < cx) x * scale + 52f * scale else x * scale - 6f * scale
            drawRect(colors.cyan.copy(alpha = rim), Offset(rimX, colTop), Size(4f * scale, colH))
        }
    }
}

private fun DrawScope.drawStairs(colors: MonumentalColors, cx: Float, cy: Float, w: Float, h: Float, scale: Float, portalAlpha: Float, dim: Float) {
    for (i in 0 until 7) {
        val y = cy + 422f * scale + i * 52f * scale
        val halfw = (150f + i * 62f) * scale
        val fill = if (i % 2 == 0) colors.stoneDeep else colors.stone
        val path = Path().apply {
            moveTo(cx - halfw, y)
            lineTo(cx + halfw, y)
            lineTo(cx + halfw - 14f * scale, y + 52f * scale)
            lineTo(cx - halfw + 14f * scale, y + 52f * scale)
            close()
        }
        drawPath(path, fill)
        if (i == 1 || i == 3 || i == 5) {
            drawLine(
                colors.bronzeEdge.copy(alpha = 0.8f * dim),
                Offset(cx - halfw + 8f * scale, y + 3f * scale),
                Offset(cx + halfw - 8f * scale, y + 3f * scale),
                strokeWidth = 2.5f * scale,
            )
        }
    }
}

/** Gateway glow only — drawn above the stairs WebP layer as state-driven light. */
private fun DrawScope.drawGatewayGlow(colors: MonumentalColors, cx: Float, cy: Float, scale: Float, portalAlpha: Float, dim: Float) {
    val gy = cy + 422f * scale + 7 * 52f * scale - 12f * scale
    val gw = 84f * scale
    val glowA = (0.35f + 0.45f * portalAlpha) * dim
    val inner = Path().apply {
        moveTo(cx - gw + 20f * scale, gy)
        lineTo(cx - gw + 20f * scale, gy - 76f * scale)
        quadraticBezierTo(cx, gy - 116f * scale, cx + gw - 20f * scale, gy - 76f * scale)
        lineTo(cx + gw - 20f * scale, gy)
        close()
    }
    drawPath(inner, colors.cyanDeep.copy(alpha = glowA.coerceAtMost(0.9f)))
}

private fun DrawScope.drawSigil(colors: MonumentalColors, cx: Float, cy: Float, scale: Float, alpha: Float) {
    val gold = colors.gold.copy(alpha = alpha)
    val cyan = colors.cyanCore.copy(alpha = alpha)
    // staff
    drawLine(gold, Offset(cx, cy - 92f * scale), Offset(cx, cy + 92f * scale), strokeWidth = 7f * scale, cap = StrokeCap.Round)
    // twin S-waves
    val s1 = Path().apply {
        moveTo(cx, cy + 62f * scale)
        cubicTo(cx + 56f * scale, cy + 42f * scale, cx + 56f * scale, cy + 8f * scale, cx, cy - 10f * scale)
        cubicTo(cx - 56f * scale, cy - 28f * scale, cx - 56f * scale, cy - 62f * scale, cx, cy - 78f * scale)
    }
    drawPath(s1, cyan, style = Stroke(5f * scale, cap = StrokeCap.Round))
    val s2 = Path().apply {
        moveTo(cx, cy + 62f * scale)
        cubicTo(cx - 56f * scale, cy + 42f * scale, cx - 56f * scale, cy + 8f * scale, cx, cy - 10f * scale)
        cubicTo(cx + 56f * scale, cy - 28f * scale, cx + 56f * scale, cy - 62f * scale, cx, cy - 78f * scale)
    }
    drawPath(s2, cyan, style = Stroke(5f * scale, cap = StrokeCap.Round))
    // wings
    val w1 = Path().apply {
        moveTo(cx - 4f * scale, cy - 88f * scale)
        lineTo(cx - 64f * scale, cy - 112f * scale)
        lineTo(cx - 30f * scale, cy - 78f * scale)
        lineTo(cx - 72f * scale, cy - 92f * scale)
        lineTo(cx - 34f * scale, cy - 64f * scale)
    }
    drawPath(w1, gold, style = Stroke(6f * scale, cap = StrokeCap.Round))
    val w2 = Path().apply {
        moveTo(cx + 4f * scale, cy - 88f * scale)
        lineTo(cx + 64f * scale, cy - 112f * scale)
        lineTo(cx + 30f * scale, cy - 78f * scale)
        lineTo(cx + 72f * scale, cy - 92f * scale)
        lineTo(cx + 34f * scale, cy - 64f * scale)
    }
    drawPath(w2, gold, style = Stroke(6f * scale, cap = StrokeCap.Round))
    // diamond eye
    drawDiamond(cx, cy, 13f * scale, colors.cyanCore.copy(alpha = alpha * 0.9f), colors.cyanCore)
}

private fun DrawScope.drawMessengers(
    colors: MonumentalColors,
    params: CoreVisualParams,
    cx: Float, cy: Float, w: Float, scale: Float, dim: Float, t: Float,
) {
    fun trail(path: Path, color: Color, alpha: Float, dash: Boolean) {
        drawPath(
            path, color.copy(alpha = alpha * dim),
            style = Stroke(
                3.5f * scale,
                cap = StrokeCap.Round,
                pathEffect = if (dash) androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(3f * scale, 12f * scale)) else null,
            ),
        )
    }
    when (params.messenger) {
        MessengerMode.NONE -> Unit
        MessengerMode.LOCAL_ORBIT -> {
            drawCircle(
                colors.localGold.copy(alpha = 0.5f * dim), 248f * scale, Offset(cx, cy),
                style = Stroke(3f * scale, pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(6f * scale, 14f * scale))),
            )
            drawMessengerSigil(colors, cx + 248f * scale, cy, colors.localGold, 0.9f * scale, 90f)
        }
        MessengerMode.DEPARTURE -> {
            val p = Path().apply {
                moveTo(cx - 150f * scale, cy - 160f * scale)
                cubicTo(cx - 320f * scale, cy - 260f * scale, cx - 430f * scale, cy - 340f * scale, 70f * scale, cy - 420f * scale)
            }
            trail(p, colors.towerBlue, 0.6f, true)
            drawMessengerSigil(colors, cx - 310f * scale, cy - 272f * scale, colors.towerBlue, 1.15f * scale, -18f)
            if (params.secondTrail) {
                val p2 = Path().apply {
                    moveTo(cx + 150f * scale, cy - 160f * scale)
                    cubicTo(cx + 320f * scale, cy - 250f * scale, w - 160f * scale, cy - 330f * scale, w - 90f * scale, cy - 390f * scale)
                }
                trail(p2, colors.towerBlue, 0.35f, true)
                drawMessengerSigil(colors, cx + 260f * scale, cy - 232f * scale, colors.towerBlue, 0.85f * scale, 16f)
            }
        }
        MessengerMode.ARRIVAL -> {
            val p = Path().apply {
                moveTo(w - 16f * scale, cy - 420f * scale)
                cubicTo(cx + 430f * scale, cy - 330f * scale, cx + 300f * scale, cy - 240f * scale, cx + 150f * scale, cy - 150f * scale)
            }
            trail(p, colors.localCyan, 0.6f, true)
            drawMessengerSigil(colors, cx + 290f * scale, cy - 252f * scale, colors.localCyan, 1.05f * scale, 14f)
        }
        MessengerMode.BROKEN -> {
            val y = cy - 150f * scale
            drawLine(colors.towerBlue.copy(alpha = 0.7f * dim), Offset(cx - r3px(scale) - 70f * scale, y), Offset(cx - r3px(scale) + 10f * scale, y), strokeWidth = 5f * scale)
            drawLine(colors.towerBlue.copy(alpha = 0.35f * dim), Offset(cx - r3px(scale) + 66f * scale, y), Offset(cx - r3px(scale) + 110f * scale, y), strokeWidth = 5f * scale)
            drawLine(colors.redBronze.copy(alpha = 0.9f * dim), Offset(cx - r3px(scale) + 14f * scale, y - 8f * scale), Offset(cx - r3px(scale) + 34f * scale, y), strokeWidth = 4f * scale)
            drawLine(colors.redBronze.copy(alpha = 0.9f * dim), Offset(cx - r3px(scale) + 14f * scale, y + 8f * scale), Offset(cx - r3px(scale) + 34f * scale, y), strokeWidth = 4f * scale)
            drawLine(colors.redBronze.copy(alpha = 0.9f * dim), Offset(cx - r3px(scale) + 62f * scale, y - 8f * scale), Offset(cx - r3px(scale) + 42f * scale, y), strokeWidth = 4f * scale)
            drawLine(colors.redBronze.copy(alpha = 0.9f * dim), Offset(cx - r3px(scale) + 62f * scale, y + 8f * scale), Offset(cx - r3px(scale) + 42f * scale, y), strokeWidth = 4f * scale)
        }
        MessengerMode.DEGRADED -> {
            val p = Path().apply {
                moveTo(cx - 150f * scale, cy - 160f * scale)
                cubicTo(cx - 320f * scale, cy - 260f * scale, cx - 430f * scale, cy - 340f * scale, 70f * scale, cy - 420f * scale)
            }
            trail(p, colors.towerBlue, 0.30f, true)
            drawMessengerSigil(colors, cx - 370f * scale, cy - 314f * scale, colors.towerBlue, 0.7f * scale, -18f)
        }
    }
}

private fun r3px(scale: Float) = 440f * scale

private fun DrawScope.drawMessengerSigil(colors: MonumentalColors, x: Float, y: Float, color: Color, scale: Float, rot: Float) {
    rotate(rot, pivot = Offset(x, y)) {
        val body = Path().apply {
            moveTo(x - 9f * scale, y - 2f * scale)
            lineTo(x, y - 9f * scale)
            lineTo(x + 9f * scale, y - 2f * scale)
            lineTo(x, y + 5f * scale)
            close()
        }
        drawPath(body, color)
        val wl = Path().apply {
            moveTo(x - 6f * scale, y - 6f * scale)
            lineTo(x - 22f * scale, y - 14f * scale)
            lineTo(x - 10f * scale, y - 4f * scale)
            close()
        }
        drawPath(wl, color.copy(alpha = 0.85f))
        val wr = Path().apply {
            moveTo(x + 6f * scale, y - 6f * scale)
            lineTo(x + 22f * scale, y - 14f * scale)
            lineTo(x + 10f * scale, y - 4f * scale)
            close()
        }
        drawPath(wr, color.copy(alpha = 0.85f))
        drawCircle(colors.obsidianDeep, 2.6f * scale, Offset(x, y - 1f * scale))
    }
}

private fun DrawScope.drawDiamond(cx: Float, cy: Float, r: Float, fill: Color, stroke: Color) {
    val path = Path().apply {
        moveTo(cx, cy - r)
        lineTo(cx + r, cy)
        lineTo(cx, cy + r)
        lineTo(cx - r, cy)
        close()
    }
    if (fill != Color.Transparent) drawPath(path, fill) else drawPath(path, stroke, style = Stroke(r * 0.22f))
}

/** State-driven rim light drawn over the colonnade WebP layer (BALANCED/HIGH). */
private fun DrawScope.drawColonnadeRimLight(
    colors: MonumentalColors,
    cx: Float,
    w: Float,
    scale: Float,
    conduitAlpha: Float,
    dim: Float,
) {
    val cy = size.height * (620f / 1420f)
    val colTop = cy - 290f * scale
    val colH = 560f * scale
    val rim = conduitAlpha * 0.55f * dim
    if (rim <= 0.02f) return
    val xs = listOf(118f, 218f, 318f, w - 174f, w - 274f, w - 374f)
    for (x in xs) {
        val rimX = if (x * scale < cx) x * scale + 52f * scale else x * scale - 6f * scale
        drawRect(colors.cyan.copy(alpha = rim), Offset(rimX, colTop), Size(4f * scale, colH))
    }
    // turret window dimming follows the state luminance budget
    val winDim = 1f - dim
    if (winDim > 0.05f) {
        for (turretX in listOf(150f * scale, w - 208f * scale)) {
            for (wy in listOf(140f, 190f, 240f)) {
                drawRect(
                    colors.obsidianDeep.copy(alpha = winDim * 0.6f),
                    topLeft = Offset(turretX + 19f * scale, cy - 470f * scale - 24f * scale + wy * scale),
                    size = Size(20f * scale, 26f * scale),
                )
            }
        }
    }
}
