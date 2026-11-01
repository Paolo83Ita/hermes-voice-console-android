package com.hermestower.voiceconsole.ui.monumental

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/**
 * GRAPH 3 WAVE 1 — per-region burn-in safeguard (AMOLED_AND_BURN_IN_SAFETY.md §2).
 * Slow bounded Lissajous-style drift applied per region; disabled when reduced
 * motion is on or the burn-in guard is off (Owner setting, default ON).
 * Amplitude ≤4 dp — usable and imperceptible in practice.
 */
enum class BurnInRegion(val maxOffsetDp: Int, val phase: Int) {
    FRIEZE(3, 0),
    CORE(4, 1),
    SEALS(3, 2),
    TRANSCRIPT(2, 3),
    MEDALLIONS(3, 4),
}

data class BurnInOffset(val xDp: Float, val yDp: Float)

object BurnInOffsets {
    /** Deterministic offset for a step index (4-step cycle). */
    fun of(region: BurnInRegion, step: Int): BurnInOffset {
        val s = ((step + region.phase) % 4 + 4) % 4
        val m = region.maxOffsetDp.toFloat()
        return when (s) {
            1 -> BurnInOffset(m, 0f)
            2 -> BurnInOffset(-m, -m * 0.5f)
            3 -> BurnInOffset(-m, m * 0.5f)
            else -> BurnInOffset(0f, 0f)
        }
    }
}

/** Remembers and advances the shared 25 s burn-in cycle; returns the current step. */
@Composable
fun rememberBurnInStep(enabled: Boolean, reducedMotion: Boolean): Int {
    var step by remember { mutableIntStateOf(0) }
    LaunchedEffect(enabled, reducedMotion) {
        while (true) {
            delay(25_000L)
            if (enabled && !reducedMotion) step = (step + 1) % 4
        }
    }
    return if (enabled && !reducedMotion) step else 0
}

/** Applies the region offset to any composable. */
fun Modifier.burnInOffset(step: Int, region: BurnInRegion): Modifier {
    val off = BurnInOffsets.of(region, step)
    return graphicsLayer {
        translationX = off.xDp.dp.toPx()
        translationY = off.yDp.dp.toPx()
    }
}
