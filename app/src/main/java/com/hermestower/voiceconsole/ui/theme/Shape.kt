package com.hermestower.voiceconsole.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/** Geometry: measured, architectural corners — never playful. */
val HermesShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(18.dp),
    extraLarge = RoundedCornerShape(26.dp),
)

/**
 * Motion tokens (UI_STATE_MACHINE.md §5). Reduced motion replaces loops with
 * static mid-states; intensity scales glow amplitudes, never information.
 */
object HermesMotion {
    const val FAST_MS = 150
    const val NORMAL_MS = 300
    const val IDLE_PULSE_MS = 4000
    const val RING_MS = 1600
    const val SWEEP_MS = 1200
    const val STREAM_MS = 1200
    const val ORBIT_MS = 2000
    const val QUEUED_PULSE_MS = 2400
    const val ERROR_BLINK_MS = 350
    const val BURNIN_DRIFT_PERIOD_S = 60
    const val BURNIN_DRIFT_DP = 8f
}
