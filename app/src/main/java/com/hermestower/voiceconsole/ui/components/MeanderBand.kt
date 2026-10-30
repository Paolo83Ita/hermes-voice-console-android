package com.hermestower.voiceconsole.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import com.hermestower.voiceconsole.ui.theme.HermesColors

/**
 * The Greek key (meander) band — the single signature motif allowed per screen
 * zone (VISUAL_DIRECTION.md §1). Drawn, not an image asset: theme-aware, crisp
 * at any density, no copyrighted artwork.
 */
@Composable
fun MeanderBand(colors: HermesColors, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.fillMaxWidth().height(14.dp)) {
        val cell = size.height * 1.6f
        val stroke = size.height * 0.11f
        val ink = colors.gold.copy(alpha = 0.75f)
        val bottom = size.height - stroke
        var x = 0f
        val path = androidx.compose.ui.graphics.Path()
        while (x < size.width + cell) {
            val u = cell / 10f
            // base line
            path.moveTo(x, bottom)
            path.lineTo(x + cell, bottom)
            // key spiral (unit cell)
            path.moveTo(x + u, bottom)
            path.lineTo(x + u, u * 1.2f)
            path.lineTo(x + u * 9, u * 1.2f)
            path.lineTo(x + u * 9, u * 6.4f)
            path.lineTo(x + u * 4.4f, u * 6.4f)
            path.lineTo(x + u * 4.4f, u * 3.6f)
            path.lineTo(x + u * 7, u * 3.6f)
            x += cell
        }
        drawPath(
            path,
            color = ink,
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = stroke, cap = StrokeCap.Butt),
        )
    }
}

/** Pediment line: a single low triangle stroke crowning the console. */
@Composable
fun PedimentLine(colors: HermesColors, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.fillMaxWidth().height(18.dp)) {
        val w = size.width
        val h = size.height
        val stroke = h * 0.10f
        drawLine(
            color = colors.gold.copy(alpha = 0.8f),
            start = Offset(w * 0.08f, h * 0.86f),
            end = Offset(w * 0.5f, h * 0.1f),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
        drawLine(
            color = colors.gold.copy(alpha = 0.8f),
            start = Offset(w * 0.5f, h * 0.1f),
            end = Offset(w * 0.92f, h * 0.86f),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
    }
}
