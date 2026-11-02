package com.hermestower.voiceconsole.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.hermestower.voiceconsole.core.state.ConsoleState
import com.hermestower.voiceconsole.ui.runtime.rememberAnimationFrameTimeMs
import com.hermestower.voiceconsole.ui.theme.HermesColors
import com.hermestower.voiceconsole.ui.theme.HermesMotion
import kotlin.math.abs
import kotlin.math.sin

/**
 * The animated Hermes Tower — a truthful instrument (UI_STATE_MACHINE.md §2).
 * One Canvas, one continuous clock; every state derives its motion from real
 * console state, never from decoration. Reduced motion freezes to static
 * mid-states that keep every state legible (NFR-06).
 */
@Composable
fun TowerCore(
    state: ConsoleState,
    reducedMotion: Boolean,
    animationIntensity: Float,
    speakingAmplitude: Float,
    colors: HermesColors,
    modifier: Modifier = Modifier,
) {
    // GRAPH 4 NODE 0 — lifecycle-safe frame controller (CP-G4-002): replaces the
    // produceState+withFrameNanos loop lint could not verify
    // (ProduceStateDoesNotAssignValue). Reduced motion keeps the static frame.
    val timeMs = rememberAnimationFrameTimeMs(running = !reducedMotion)
    Canvas(
        modifier = modifier.semantics { contentDescription = state.accessibility },
    ) {
        drawTower(
            state = state,
            colors = colors,
            t = timeMs.toFloat(),
            intensity = animationIntensity.coerceIn(0.2f, 1.5f),
            amplitude = speakingAmplitude.coerceIn(0f, 1f),
            animated = !reducedMotion,
        )
    }
}

private fun phase(t: Float, periodMs: Float, offsetMs: Float = 0f): Float =
    ((t + offsetMs) % periodMs) / periodMs

private fun DrawScope.drawTower(
    state: ConsoleState,
    colors: HermesColors,
    t: Float,
    intensity: Float,
    amplitude: Float,
    animated: Boolean,
) {
    val w = size.width
    val h = size.height
    val cx = w / 2f

    // Muted/disconnected/dormant desaturate the stone.
    val stone = when (state) {
        ConsoleState.DISCONNECTED, ConsoleState.DORMANT -> lerp(colors.stone, Color(0xFF6E6E6E), 0.55f)
        ConsoleState.PRIVACY_MUTED -> lerp(colors.stone, Color(0xFF55504A), 0.4f)
        else -> colors.stone
    }
    val stoneAlt = when (state) {
        ConsoleState.DISCONNECTED, ConsoleState.DORMANT -> lerp(colors.stoneAlt, Color(0xFF565656), 0.55f)
        ConsoleState.PRIVACY_MUTED -> lerp(colors.stoneAlt, Color(0xFF45403B), 0.4f)
        else -> colors.stoneAlt
    }
    val energy = when (state) {
        ConsoleState.DISCONNECTED, ConsoleState.DORMANT -> colors.inkSecondary.copy(alpha = 0.5f)
        ConsoleState.DEGRADED -> colors.energy.copy(alpha = 0.45f * flicker(t))
        else -> colors.energy
    }
    val energyBright = when (state) {
        ConsoleState.DISCONNECTED, ConsoleState.DORMANT -> colors.inkSecondary.copy(alpha = 0.6f)
        ConsoleState.DEGRADED -> colors.energyBright.copy(alpha = 0.45f * flicker(t))
        else -> colors.energyBright
    }

    // ---- geometry ----
    val baseY = h * 0.94f
    val shaftW = w * 0.17f
    val shaftH = h * 0.42f
    val shaftTop = baseY - shaftH
    val plinthWs = listOf(w * 0.40f, w * 0.34f, w * 0.28f)
    val plinthH = h * 0.018f
    val capW = shaftW * 1.34f
    val capH = h * 0.024f
    val abacusW = shaftW * 1.5f
    val abacusH = h * 0.016f
    val beaconR = w * 0.052f
    val beaconCy = shaftTop - abacusH - capH - beaconR * 1.15f

    // ---- plinth steps ----
    var stepBottom = baseY
    for ((i, pw) in plinthWs.withIndex()) {
        val sh = plinthH * (3 - i)
        drawRoundRect(
            color = if (i % 2 == 0) stoneAlt else stone,
            topLeft = Offset(cx - pw / 2, stepBottom - sh),
            size = Size(pw, sh),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.01f),
        )
        stepBottom -= sh + plinthH * 0.35f
    }

    // ---- fluted shaft ----
    drawRoundRect(
        color = stone,
        topLeft = Offset(cx - shaftW / 2, shaftTop),
        size = Size(shaftW, shaftH),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.012f, w * 0.012f),
    )
    val flutes = 7
    val fluteW = shaftW / (flutes * 2 - 1)
    for (i in 0 until flutes) {
        val fx = cx - shaftW / 2 + i * 2 * fluteW
        drawRect(
            color = stoneAlt,
            topLeft = Offset(fx, shaftTop + shaftH * 0.02f),
            size = Size(fluteW, shaftH * 0.96f),
        )
    }

    // ---- capital + abacus ----
    drawRect(
        color = stoneAlt,
        topLeft = Offset(cx - capW / 2, shaftTop - capH),
        size = Size(capW, capH),
    )
    drawRect(
        color = stone,
        topLeft = Offset(cx - abacusW / 2, shaftTop - capH - abacusH),
        size = Size(abacusW, abacusH),
    )

    // ---- gold base line (restrained bronze detail) ----
    drawLine(
        color = colors.gold.copy(alpha = if (state == ConsoleState.DISCONNECTED) 0.3f else 0.75f),
        start = Offset(cx - w * 0.30f, baseY + plinthH),
        end = Offset(cx + w * 0.30f, baseY + plinthH),
        strokeWidth = h * 0.006f,
        cap = StrokeCap.Round,
    )

    // ---- beacon ----
    val beaconColor: Color = when (state) {
        ConsoleState.DISCONNECTED, ConsoleState.DORMANT, ConsoleState.PRIVACY_MUTED -> Color.Transparent
        ConsoleState.REQUEST_QUEUED -> colors.warn
        ConsoleState.ERROR -> colors.warn
        else -> energyBright
    }
    val pulse = idlePulseScale(t, state, intensity)
    val glowR = beaconR * 2.6f * pulse * (1f + amplitude * 0.9f)
    if (beaconColor != Color.Transparent) {
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(beaconColor.copy(alpha = 0.38f * intensity * glowAlpha(state, amplitude)), Color.Transparent),
                center = Offset(cx, beaconCy),
                radius = glowR,
            ),
            radius = glowR,
            center = Offset(cx, beaconCy),
        )
    }
    drawDiamond(cx, beaconCy, beaconR * pulse, beaconColor, stoneAlt, state)

    // ---- state energy overlays ----
    when (state) {
        ConsoleState.IDLE -> updraft(t, cx, shaftTop, shaftH, energyBright, intensity, animated)
        ConsoleState.UPDATE_AVAILABLE -> updraft(t, cx, shaftTop, shaftH, energyBright, intensity * 0.7f, animated)
        ConsoleState.WAKE_DETECTED -> rings(t, cx, beaconCy, w, energyBright, intensity, rings = 1, periodMs = 700f, animated = animated)
        ConsoleState.LISTENING -> rings(t, cx, beaconCy, w, energyBright, intensity, rings = 3, periodMs = HermesMotion.RING_MS.toFloat(), animated = animated)
        ConsoleState.TRANSCRIBING -> sweep(t, cx, shaftTop, shaftW, shaftH, energyBright, intensity, animated)
        ConsoleState.UNDERSTANDING_LOCAL -> updraft(t, cx, shaftTop, shaftH, lerp(colors.gold, energyBright, 0.4f), intensity * 0.8f, animated)
        ConsoleState.CONTACTING_TOWER -> stream(t, cx, shaftTop, shaftW, energyBright, intensity, animated)
        ConsoleState.TOWER_PROCESSING -> orbit(t, cx, shaftTop, shaftH, shaftW, energyBright, intensity, animated)
        ConsoleState.SPEAKING -> speakGlow(t, cx, shaftTop, shaftW, shaftH, beaconR, beaconCy, energyBright, amplitude, intensity)
        ConsoleState.REQUEST_QUEUED -> queuedPulse(t, cx, baseY, plinthWs.first(), colors.warn, intensity, animated)
        ConsoleState.ERROR -> errorCapital(t, cx, shaftTop, capW, capH, abacusW, colors.warn, animated)
        ConsoleState.DISCONNECTED -> brokenLink(cx, shaftTop + shaftH * 0.42f, shaftW * 1.7f, colors.danger)
        else -> Unit
    }
    if (state == ConsoleState.UPDATE_AVAILABLE) {
        val a = 0.18f + 0.14f * sin(phase(t, HermesMotion.IDLE_PULSE_MS.toFloat()) * 2f * Math.PI).toFloat()
        drawCircle(
            color = colors.gold.copy(alpha = a + 0.15f),
            radius = beaconR * 2.1f,
            center = Offset(cx, beaconCy),
            style = Stroke(width = h * 0.006f),
        )
    }

    // ---- privacy mute emblem: unmistakable mic-slash on the base ----
    if (state == ConsoleState.PRIVACY_MUTED) {
        micSlash(cx, baseY - h * 0.10f, w * 0.075f, colors.danger)
        drawRect(
            color = Color.Black.copy(alpha = 0.18f),
            topLeft = Offset.Zero,
            size = size,
        )
    }
}

private fun idlePulseScale(t: Float, state: ConsoleState, intensity: Float): Float = when (state) {
    ConsoleState.DORMANT, ConsoleState.DISCONNECTED -> 0.85f
    ConsoleState.SPEAKING -> 1f
    else -> 1f + 0.08f * intensity * sin(phase(t, HermesMotion.IDLE_PULSE_MS.toFloat()) * 2f * Math.PI).toFloat()
}

private fun glowAlpha(state: ConsoleState, amplitude: Float): Float = when (state) {
    ConsoleState.SPEAKING -> 0.55f + amplitude * 0.45f
    ConsoleState.DORMANT, ConsoleState.DISCONNECTED, ConsoleState.PRIVACY_MUTED -> 0.15f
    else -> 0.5f
}

private fun flicker(t: Float): Float =
    if (phase(t, 1000f) < 0.08f) 0.55f else 1f

private fun DrawScope.drawDiamond(cx: Float, cy: Float, r: Float, fill: Color, stroke: Color, state: ConsoleState) {
    val path = Path().apply {
        moveTo(cx, cy - r)
        lineTo(cx + r, cy)
        lineTo(cx, cy + r)
        lineTo(cx - r, cy)
        close()
    }
    if (fill != Color.Transparent) {
        drawPath(path, fill)
    } else {
        drawPath(path, stroke.copy(alpha = 0.6f), style = Stroke(width = r * 0.22f))
    }
}

/** LISTENING / WAKE_DETECTED: expanding rings from the beacon (UI_STATE_MACHINE §2). */
private fun DrawScope.rings(
    t: Float, cx: Float, cy: Float, w: Float, color: Color, intensity: Float,
    rings: Int, periodMs: Float, animated: Boolean,
) {
    val maxR = w * 0.38f
    val baseR = w * 0.10f
    if (!animated) {
        // Reduced motion: two static concentric rings.
        drawCircle(color.copy(alpha = 0.35f), radius = baseR + (maxR - baseR) * 0.35f, center = Offset(cx, cy), style = Stroke(w * 0.008f))
        drawCircle(color.copy(alpha = 0.18f), radius = baseR + (maxR - baseR) * 0.7f, center = Offset(cx, cy), style = Stroke(w * 0.008f))
        return
    }
    for (i in 0 until rings) {
        val p = phase(t, periodMs, offsetMs = i * periodMs / rings)
        val r = baseR + (maxR - baseR) * p
        val alpha = (1f - p) * 0.7f * intensity
        drawCircle(color.copy(alpha = alpha), radius = r, center = Offset(cx, cy), style = Stroke(w * 0.01f))
    }
}

/** IDLE faint updraft; also UNDERSTANDING_LOCAL with a warmer tone. */
private fun DrawScope.updraft(
    t: Float, cx: Float, shaftTop: Float, shaftH: Float, color: Color,
    intensity: Float, animated: Boolean,
) {
    val particles = 4
    for (i in 0 until particles) {
        val p = if (animated) phase(t, 3000f, offsetMs = i * 3000f / particles) else i.toFloat() / particles
        val y = shaftTop - shaftH * 0.25f * p
        val x = cx + sin(p * 2f * Math.PI + i).toFloat() * shaftH * 0.05f
        val alpha = (if (animated) sin(p * Math.PI).toFloat() else 0.45f) * 0.4f * intensity
        drawCircle(color.copy(alpha = alpha), radius = shaftH * 0.012f, center = Offset(x, y))
    }
}

/** TRANSCRIBING: controlled vertical light sweep along the shaft. */
private fun DrawScope.sweep(
    t: Float, cx: Float, shaftTop: Float, shaftW: Float, shaftH: Float,
    color: Color, intensity: Float, animated: Boolean,
) {
    val p = if (animated) phase(t, HermesMotion.SWEEP_MS.toFloat()) else 0.6f
    val bandH = shaftH * 0.14f
    val y = shaftTop + (shaftH - bandH) * p
    drawRoundRect(
        color = color.copy(alpha = (if (animated) (1f - abs(2f * p - 1f)) else 0.5f) * 0.55f * intensity),
        topLeft = Offset(cx - shaftW * 0.62f, y),
        size = Size(shaftW * 1.24f, bandH),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(bandH / 2),
    )
}

/** CONTACTING_TOWER: outward/upward data stream from the beacon. */
private fun DrawScope.stream(
    t: Float, cx: Float, shaftTop: Float, shaftW: Float, color: Color,
    intensity: Float, animated: Boolean,
) {
    val particles = 8
    val travel = shaftTop * 0.62f // from the shaft top toward the screen top
    for (i in 0 until particles) {
        val p = if (animated) phase(t, HermesMotion.STREAM_MS.toFloat(), offsetMs = (i * HermesMotion.STREAM_MS / particles).toFloat())
        else i.toFloat() / particles
        val yPos = shaftTop - travel * p
        val x = cx + sin(p * 4f * Math.PI + i).toFloat() * shaftW * 0.35f
        val alpha = (if (animated) sin(p * Math.PI).toFloat() else 0.5f) * 0.75f * intensity
        drawCircle(color.copy(alpha = alpha), radius = shaftW * 0.035f, center = Offset(x, yPos))
    }
}

/** TOWER_PROCESSING: energy circulating around the core. */
private fun DrawScope.orbit(
    t: Float, cx: Float, shaftTop: Float, shaftH: Float, shaftW: Float,
    color: Color, intensity: Float, animated: Boolean,
) {
    val rotation = if (animated) t * 360f / HermesMotion.ORBIT_MS else 30f
    val rx = shaftW * 1.15f
    for ((i, fracY) in listOf(0.30f, 0.62f).withIndex()) {
        val cy = shaftTop + shaftH * fracY
        val left = cx - rx
        val top = cy - rx * 0.30f
        drawArc(
            color = color.copy(alpha = if (i == 0) 0.8f * intensity else 0.55f * intensity),
            startAngle = rotation * (if (i == 0) 1f else -1f) + (if (i == 0) 0f else 140f),
            sweepAngle = 80f,
            useCenter = false,
            topLeft = Offset(left, top),
            size = Size(rx * 2f, rx * 0.6f),
            style = Stroke(width = shaftW * 0.06f, cap = StrokeCap.Round),
        )
    }
}

/** SPEAKING: beacon + shaft illumination follows voice amplitude (non-lip). */
private fun DrawScope.speakGlow(
    t: Float, cx: Float, shaftTop: Float, shaftW: Float, shaftH: Float,
    beaconR: Float, beaconCy: Float, color: Color, amplitude: Float, intensity: Float,
) {
    val a = (0.25f + amplitude * 0.55f) * intensity
    drawRect(
        color = color.copy(alpha = a * 0.5f),
        topLeft = Offset(cx - shaftW * 0.62f, shaftTop),
        size = Size(shaftW * 1.24f, shaftH),
    )
    drawCircle(
        brush = Brush.radialGradient(
            listOf(color.copy(alpha = a), Color.Transparent),
            center = Offset(cx, beaconCy),
            radius = beaconR * (2.2f + amplitude * 1.6f),
        ),
        radius = beaconR * (2.2f + amplitude * 1.6f),
        center = Offset(cx, beaconCy),
    )
}

/** REQUEST_QUEUED: slow amber pulse at the base. */
private fun DrawScope.queuedPulse(t: Float, cx: Float, baseY: Float, baseW: Float, warn: Color, intensity: Float, animated: Boolean) {
    val p = if (animated) phase(t, HermesMotion.QUEUED_PULSE_MS.toFloat()) else 0.5f
    val alpha = (0.25f + 0.45f * (1f - abs(2f * p - 1f))) * intensity
    drawRoundRect(
        color = warn.copy(alpha = alpha),
        topLeft = Offset(cx - baseW * 0.62f, baseY - baseW * 0.06f),
        size = Size(baseW * 1.24f, baseW * 0.12f),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(baseW * 0.05f),
    )
}

/** ERROR: amber-red pulses on the capital, three blinks then steady. */
private fun DrawScope.errorCapital(
    t: Float, cx: Float, shaftTop: Float, capW: Float, capH: Float, abacusW: Float, warn: Color,
    animated: Boolean,
) {
    val blinkPhase = (t % (HermesMotion.ERROR_BLINK_MS * 6)) / HermesMotion.ERROR_BLINK_MS
    val alpha = if (!animated) 0.55f else if (blinkPhase < 6f) (if (blinkPhase % 2f < 1f) 0.85f else 0.25f) else 0.55f
    drawRect(
        color = warn.copy(alpha = alpha),
        topLeft = Offset(cx - abacusW / 2, shaftTop - capH * 2.2f),
        size = Size(abacusW, capH * 2.2f),
    )
    drawRect(
        color = warn.copy(alpha = alpha * 0.8f),
        topLeft = Offset(cx - capW / 2, shaftTop - capH),
        size = Size(capW, capH),
    )
}

/** DISCONNECTED: broken link across the shaft. */
private fun DrawScope.brokenLink(cx: Float, y: Float, half: Float, danger: Color) {
    drawLine(danger.copy(alpha = 0.85f), Offset(cx - half, y), Offset(cx - half * 0.2f, y), strokeWidth = half * 0.06f, cap = StrokeCap.Round)
    drawLine(danger.copy(alpha = 0.85f), Offset(cx + half * 0.2f, y), Offset(cx + half, y), strokeWidth = half * 0.06f, cap = StrokeCap.Round)
}

/** PRIVACY_MUTED: static mic-slash emblem. */
private fun DrawScope.micSlash(cx: Float, cy: Float, r: Float, danger: Color) {
    drawCircle(danger.copy(alpha = 0.9f), radius = r, center = Offset(cx, cy), style = Stroke(r * 0.16f, cap = StrokeCap.Round))
    // mic body
    drawRoundRect(
        color = danger,
        topLeft = Offset(cx - r * 0.22f, cy - r * 0.55f),
        size = Size(r * 0.44f, r * 0.75f),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(r * 0.2f),
    )
    // mic arc (holder)
    drawArc(
        color = danger,
        startAngle = -20f, sweepAngle = 220f, useCenter = false,
        topLeft = Offset(cx - r * 0.5f, cy - r * 0.25f),
        size = Size(r, r * 0.85f),
        style = Stroke(r * 0.10f),
    )
    // slash
    drawLine(
        danger,
        Offset(cx - r * 0.8f, cy - r * 0.8f),
        Offset(cx + r * 0.8f, cy + r * 0.8f),
        strokeWidth = r * 0.18f,
        cap = StrokeCap.Round,
    )
}
