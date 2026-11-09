package com.hermestower.voiceconsole.ui.monumental

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import com.hermestower.voiceconsole.core.state.ConsoleState

/**
 * GRAPH 4 directive §7 — MONUMENTAL TOWER ARCHITECTURE (original work, no
 * reference bitmap is shipped). The frame wraps the existing Oracle Core stage:
 *
 *   BACKGROUND — obsidian/stone depth with a controlled cyan atmosphere
 *   (never an empty flat void);
 *   UPPER TOWER — pediment crown, flanking columns, bronze inscription band;
 *   LOWER TOWER — gateway arch, stairs, plinth;
 *   MESSENGER PATH — a cyan energy path flowing from the gateway toward the
 *   viewer, driven by the APP STATE (active only in CONTACTING_TOWER,
 *   TOWER_PROCESSING and QUEUED; a static carved conduit in IDLE; fully
 *   static when motion is frozen by reduced-motion or the CI lifecycle
 *   profile — the same TEST_ONLY gate the Oracle Core honors).
 *
 * Everything is deterministic geometry: no randomness, no decorative timers
 * when the state is quiet, no bitmap memory (pure Canvas), quality-profile
 * aware via the existing [QualityBudget].
 */
object TowerArchitecture {

    /** States in which the messenger path flows (directive §7/§8 truthfulness). */
    private val flowStates = setOf(
        ConsoleState.CONTACTING_TOWER,
        ConsoleState.TOWER_PROCESSING,
        ConsoleState.REQUEST_QUEUED,
    )

    fun stateDrivesFlow(state: ConsoleState): Boolean = state in flowStates
}

/**
 * The architectural frame: upper order, lower structure and the state-driven
 * messenger path — drawn as the FOREGROUND architecture INSIDE the existing
 * Oracle Core stage (the obsidian/stone depth background is already provided
 * by the Oracle Core depth layers, so the frame adds the built order around
 * the rings without ever double-painting the background).
 */
fun DrawScope.drawTowerFrame(
    phase: Float,
    quality: QualityBudget,
    m: MonumentalColors,
) {
    val w = size.width
    val h = size.height
    drawUpperOrder(w, h, m, quality)
    drawLowerStructure(w, h, m, quality)
    drawMessengerPath(w, h, phase, m, quality)
}

/** UPPER TOWER — pediment crown, columns, bronze inscription band. */
private fun DrawScope.drawUpperOrder(w: Width, h: Height, m: MonumentalColors, quality: QualityBudget) {
    val bandH = h * 0.115f
    // Pediment (crown): low triangle in bronze over obsidian.
    val pediment = Path().apply {
        moveTo(w * 0.30f, bandH * 0.92f)
        lineTo(w * 0.50f, bandH * 0.16f)
        lineTo(w * 0.70f, bandH * 0.92f)
        close()
    }
    drawPath(pediment, Brush.verticalGradient(listOf(m.bronzeDeep, m.bronze)))
    drawPath(pediment, m.bronzeEdge, style = Stroke(width = 2f))
    // entablature band
    drawRect(m.obsidianPanel, Offset(w * 0.24f, bandH * 0.92f), Size(w * 0.52f, bandH * 0.34f))
    drawRect(m.bronze.copy(alpha = 0.85f), Offset(w * 0.24f, bandH * 1.26f), Size(w * 0.52f, 2.2f))
    // Bronze inscription band with engraved glyph ticks (original, non-lexical).
    val ticks = 9
    for (i in 0 until ticks) {
        val tx = w * (0.30f + 0.05f * i)
        val tickW = if (i % 3 == 0) bandH * 0.16f else bandH * 0.10f
        drawRect(
            m.gold.copy(alpha = 0.55f),
            Offset(tx, bandH * 1.02f),
            Size(if (i % 3 == 0) 2.6f else 1.6f, tickW),
        )
    }
    // Flanking columns (stylized, bronze caps + stone shafts).
    val colTop = bandH * 1.30f
    val colBottom = h * 0.62f
    for (cx in listOf(w * 0.085f, w * 0.915f)) {
        val halfW = w * 0.032f
        drawRect(
            Brush.verticalGradient(listOf(m.stone, m.stoneDeep)),
            Offset(cx - halfW, colTop),
            Size(halfW * 2, colBottom - colTop),
        )
        // flutes (three vertical grooves)
        for (f in -1..1) {
            drawLine(
                m.stoneEdge.copy(alpha = 0.5f),
                Offset(cx + f * halfW * 0.5f, colTop + 6f),
                Offset(cx + f * halfW * 0.5f, colBottom - 6f),
                strokeWidth = 1.4f,
            )
        }
        // capital + base (bronze)
        drawRect(m.bronze, Offset(cx - halfW * 1.35f, colTop), Size(halfW * 2.7f, bandH * 0.22f))
        drawRect(m.bronzeDeep, Offset(cx - halfW * 1.35f, colBottom), Size(halfW * 2.7f, bandH * 0.20f))
    }
}

/** LOWER TOWER — gateway, stairs, plinth (foreground structure). */
private fun DrawScope.drawLowerStructure(w: Width, h: Height, m: MonumentalColors, quality: QualityBudget) {
    val top = h * 0.70f
    // Plinth: stepped base widening toward the viewer.
    for (i in 0 until 3) {
        val inset = w * (0.06f + 0.045f * (2 - i))
        val y0 = top + (h - top) * (i / 3f)
        val y1 = top + (h - top) * ((i + 1) / 3f)
        drawRect(
            Brush.verticalGradient(listOf(m.obsidianRaised, m.obsidian)),
            Offset(inset, y0),
            Size(w - inset * 2, y1 - y0),
        )
        drawRect(m.bronze.copy(alpha = 0.35f), Offset(inset, y0), Size(w - inset * 2, 1.6f))
    }
    // Gateway arch (dark opening, bronze rim) centered on the plinth's top step.
    val archCenter = Offset(w / 2f, top + (h - top) * 0.16f)
    val archW = w * 0.22f
    val archH = (h - top) * 0.52f
    val opening = Path().apply {
        moveTo(archCenter.x - archW / 2, archCenter.y + archH)
        lineTo(archCenter.x - archW / 2, archCenter.y)
        cubicTo(
            archCenter.x - archW / 2, archCenter.y - archH * 0.9f,
            archCenter.x + archW / 2, archCenter.y - archH * 0.9f,
            archCenter.x + archW / 2, archCenter.y,
        )
        lineTo(archCenter.x + archW / 2, archCenter.y + archH)
        close()
    }
    drawPath(opening, Color(0xFF05070C))
    drawPath(opening, m.bronzeEdge, style = Stroke(width = 2.4f))
    // G4-VISUAL D6 repair (MONUMENTAL_TOWER_VISUAL_GAP_GRAPH4_FINAL.md): the
    // opening read at ≈1.9:1 against the plinth (threshold invisible without the
    // rim). A controlled warm interior glow gives the gateway presence while
    // staying inside the OLED budget (mean-L impact < 1%). Clipped to the
    // opening so the glow never bleeds onto the plinth or stairs.
    clipPath(opening) {
        drawCircle(
            Brush.radialGradient(
                listOf(m.gold.copy(alpha = 0.14f), m.bronzeDeep.copy(alpha = 0.05f), Color.Transparent),
            ),
            radius = archW * 0.78f,
            center = Offset(archCenter.x, archCenter.y + archH * 0.42f),
        )
    }
    // Stairs descending from the arch toward the viewer (five steps, deterministic).
    for (i in 0 until 5) {
        val sy = archCenter.y + archH + (h - archCenter.y - archH) * (i / 5f)
        val sw = archW * (0.55f + 0.16f * i)
        drawRect(
            m.stoneDeep.copy(alpha = 0.85f),
            Offset(archCenter.x - sw / 2, sy),
            Size(sw, (h - archCenter.y - archH) / 5f - 1.5f),
        )
    }
}

/** MESSENGER PATH — energy conduit from the gateway toward the viewer. */
private fun DrawScope.drawMessengerPath(
    w: Width,
    h: Height,
    phase: Float,
    m: MonumentalColors,
    quality: QualityBudget,
) {
    val flowing = phase > 0f
    // G4-VISUAL D1 repair (MONUMENTAL_TOWER_VISUAL_GAP_GRAPH4_FINAL.md): the band
    // previously ran h*0.86→h*0.985 and the bottom-center StateOverlayChip (top
    // edge ≈ h*0.8985) occluded ≈71% of it — including the pulse centers. The
    // messenger path now runs from below the Oracle Core, THROUGH the gateway,
    // and ends CLEAR of the chip (h*0.875): the §7 "energy path toward the user"
    // is fully visible in exactly the states that advertise it.
    val start = Offset(w / 2f, h * 0.62f)
    val end = Offset(w / 2f, h * 0.875f)
    // Carved conduit (always visible, quiet).
    drawLine(m.cyanDeep.copy(alpha = 0.55f), start, end, strokeWidth = w * 0.012f)
    drawLine(m.cyanAbyss.copy(alpha = 0.8f), start, end, strokeWidth = w * 0.006f)
    // Flowing energy only when the app state drives it (directive §8).
    if (flowing) {
        // G4-VISUAL D2 repair: the former `particleCount >= 40` branch was dead
        // code (the ladder tops out at 12) so the HIGH profile rendered
        // byte-identically to BALANCED. Real budgets: LOW=0, BALANCED=6, HIGH=12.
        val pulses = when {
            quality.particleCount >= 12 -> 4
            quality.particleCount > 0 -> 2
            else -> 0
        }
        for (i in 0 until pulses) {
            val t = ((phase + i.toFloat() / pulses) % 1f)
            val pos = Offset(start.x, start.y + (end.y - start.y) * t)
            val alpha = (0.85f * (1f - kotlin.math.abs(t - 0.5f) * 1.2f)).coerceIn(0.15f, 0.85f)
            drawCircle(
                Brush.radialGradient(
                    0f to m.cyan.copy(alpha = alpha),
                    1f to Color.Transparent,
                ),
                radius = w * 0.030f,
                center = pos,
            )
        }
    }
}

private typealias Width = Float
private typealias Height = Float