package com.hermestower.voiceconsole.ui.monumental

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.hermestower.voiceconsole.R
import com.hermestower.voiceconsole.core.state.ConsoleState
import com.hermestower.voiceconsole.ui.monumental.VisualQuality
import androidx.compose.ui.geometry.Offset
import kotlin.math.roundToInt

/**
 * GRAPH 4.10 CP-G4.10-010/011 (§29-§35) — CENTRAL TOWER VISUAL REFOUNDATION.
 *
 * The Owner REJECTED the procedural Tower. This composable renders the
 * Owner-reference-derived LAYERED central Tower (ten transparent WebP layers,
 * geometry from docs/visual/ASSET_MANIFEST.json) and drives the §34 states:
 *
 *  - IDLE: slow outer ring, low portal breathing;
 *  - LISTENING: incoming cyan energy, input gate open;
 *  - TRANSCRIBING: sequential ring pulses;
 *  - UNDERSTANDING_LOCAL: cyan-gold inner mechanism (no remote path);
 *  - CONTACTING_TOWER / TOWER_PROCESSING: upward energy / differentiated ring
 *    rotation;
 *  - SPEAKING: portal and architectural light respond to the audio amplitude;
 *  - CONTINUOUS LISTENING: persistent moderate listening seal;
 *  - PRIVACY MUTED: input gate closed, red-bronze seal;
 *  - CHAT SPEECH MUTED: audio-output glyph muted, microphone state unchanged;
 *  - DISCONNECTED: remote conduit off, local core remains available;
 *  - ERROR: controlled amber fracture — never an aggressive flash.
 *
 * Performance profiles (§35): LOW renders the static rear + static portal (no
 * rotation, no particles); BALANCED (default) adds smooth ring movement and
 * state-driven energy; HIGH adds Canvas particles and is NOT the default.
 * Reduced motion freezes every infinite animation honestly.
 *
 * Stage geometry (uniform layer scale): rear 720x1653 defines the stage;
 * core center at (0.500, 0.403) of the stage; ring box = 645/720 of width.
 */
@Composable
fun ReforgedTowerVisual(
    state: ConsoleState,
    speakingAmplitude: Float,
    quality: VisualQuality,
    reducedMotion: Boolean,
    privacyMuted: Boolean,
    continuousActive: Boolean,
    chatSpeechMuted: Boolean,
    modifier: Modifier = Modifier,
) {
    val low = quality == VisualQuality.LOW
    val frozen = reducedMotion || low

    val rear = painterResource(R.drawable.tower_rear)
    val sign = painterResource(R.drawable.tower_sign)
    val outer = painterResource(R.drawable.tower_outer_ring)
    val middle = painterResource(R.drawable.tower_middle_ring)
    val inner = painterResource(R.drawable.tower_inner_ring)
    val portal = painterResource(R.drawable.tower_portal)
    val gate = painterResource(R.drawable.tower_lower_gate)
    val stairs = painterResource(R.drawable.tower_stairs)
    val foreground = painterResource(R.drawable.tower_foreground)
    val energy = painterResource(R.drawable.tower_energy)

    BoxWithConstraints(modifier) {
        val density = LocalDensity.current
        val stageW = maxWidth
        val stageH = stageW * (1653f / 720f)
        val stageHp = with(density) { stageH.toPx() }
        val stageWp = with(density) { stageW.toPx() }

        // static architecture (fills the stage width)
        Image(rear, contentDescription = null, contentScale = ContentScale.Fit,
            modifier = Modifier.align(Alignment.TopCenter).fillMaxWidth())
        Image(sign, contentDescription = null, contentScale = ContentScale.Fit,
            modifier = Modifier.align(Alignment.TopCenter).fillMaxWidth())
        Image(stairs, contentDescription = null, contentScale = ContentScale.Fit,
            modifier = Modifier.align(Alignment.TopCenter).fillMaxWidth())
        Image(gate, contentDescription = null, contentScale = ContentScale.Fit,
            modifier = Modifier.align(Alignment.TopCenter).fillMaxWidth())
        Image(foreground, contentDescription = null, contentScale = ContentScale.Fit,
            modifier = Modifier.align(Alignment.TopCenter).fillMaxWidth())

        // energy column: width 420/720 of the stage, top at source y = CY+40.
        val energyW = stageW * (420f / 720f)

        val ringSize = stageW * (645f / 720f)
        val ringSizePx = with(density) { ringSize.toPx() }
        val energyWPx = with(density) { energyW.toPx() }
        val energyX = ((stageWp - energyWPx) / 2f).roundToInt()
        val energyY = (stageHp * 0.4425f).roundToInt()
        val ringX = ((stageWp - ringSizePx) / 2f).roundToInt()
        val ringY = (stageHp * 0.403f - ringSizePx / 2f).roundToInt()

        // §34 rotation + breathing
        val outerAngle = if (frozen) 0f else infiniteAngle(
            when (state) {
                ConsoleState.TOWER_PROCESSING -> 24_000
                ConsoleState.CONTACTING_TOWER -> 30_000
                else -> 90_000
            },
        )
        val middleAngle = if (frozen) 0f else infiniteAngle(
            if (state == ConsoleState.TOWER_PROCESSING) 16_000 else -64_000,
        )
        val innerAngle = if (frozen) 0f else infiniteAngle(
            if (state == ConsoleState.UNDERSTANDING_LOCAL) 22_000 else 48_000,
        )
        val breathing = if (frozen) 0f else infiniteBreath()
        val portalPulse = when {
            state == ConsoleState.SPEAKING -> 0.55f + 0.45f * speakingAmplitude.coerceIn(0f, 1f)
            state == ConsoleState.LISTENING -> 0.85f + 0.15f * breathing
            else -> 0.62f + 0.10f * breathing
        }
        val portalAlpha by animateFloatAsState(portalPulse, label = "portalPulse")

        val energyAlpha = when {
            privacyMuted -> 0f
            state == ConsoleState.CONTACTING_TOWER || state == ConsoleState.TOWER_PROCESSING -> 0.95f
            state == ConsoleState.LISTENING || continuousActive -> 0.8f
            state == ConsoleState.DISCONNECTED -> 0.12f
            else -> 0.4f
        }
        val innerGold = state == ConsoleState.UNDERSTANDING_LOCAL

        Image(energy, contentDescription = null, contentScale = ContentScale.Fit,
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset { IntOffset(energyX, energyY) }
                .size(energyW)
                .alpha(energyAlpha))

        Image(outer, contentDescription = null, contentScale = ContentScale.Fit,
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset { IntOffset(ringX, ringY) }
                .size(ringSize)
                .graphicsLayer { rotationZ = outerAngle })
        Image(middle, contentDescription = null, contentScale = ContentScale.Fit,
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset { IntOffset(ringX, ringY) }
                .size(ringSize)
                .graphicsLayer { rotationZ = middleAngle })
        Image(inner, contentDescription = null, contentScale = ContentScale.Fit,
            colorFilter = if (innerGold) ColorFilter.tint(Color(0x66FFC866)) else null,
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset { IntOffset(ringX, ringY) }
                .size(ringSize)
                .graphicsLayer { rotationZ = innerAngle })
        Image(portal, contentDescription = null, contentScale = ContentScale.Fit,
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset { IntOffset(ringX, ringY) }
                .size(ringSize)
                .alpha(portalAlpha))

        // §34 overlays (Canvas): seals, fracture, particles, speaking light
        TowerStateOverlays(
            privacyMuted = privacyMuted,
            chatSpeechMuted = chatSpeechMuted,
            listening = state == ConsoleState.LISTENING || continuousActive,
            error = state == ConsoleState.ERROR,
            breathing = breathing,
            particles = quality == VisualQuality.HIGH && !frozen,
            speaking = state == ConsoleState.SPEAKING,
            amplitude = speakingAmplitude,
            modifier = Modifier.align(Alignment.TopCenter).fillMaxWidth(),
        )
    }
}

/** Infinite rotation angle in degrees over [durationMs]. */
@Composable
private fun infiniteAngle(durationMs: Int): Float {
    val t = rememberInfiniteTransition(label = "ring")
    val a by t.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(durationMs, easing = LinearEasing)),
        label = "angle",
    )
    return a
}

/** 0..1 breathing wave for the portal (2.4 s cycle). */
@Composable
private fun infiniteBreath(): Float {
    val t = rememberInfiniteTransition(label = "breath")
    val b by t.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2400, easing = LinearEasing), RepeatMode.Reverse),
        label = "breath",
    )
    return b
}

/** §34 Canvas overlays — seals, fracture, particles, speaking light. */
@Composable
private fun TowerStateOverlays(
    privacyMuted: Boolean,
    chatSpeechMuted: Boolean,
    listening: Boolean,
    error: Boolean,
    breathing: Float,
    particles: Boolean,
    speaking: Boolean,
    amplitude: Float,
    modifier: Modifier,
) {
    val fractureAlpha = if (error) 0.55f + 0.2f * breathing else 0f
    Canvas(modifier) {
        val w = size.width
        val h = size.height * (1653f / 720f) // the canvas is stage-wide; overlays use stage fractions
        val core = Offset(w * 0.5f, w * 0.403f * (1653f / 720f))

        // PRIVACY MUTED: input gate closed + red-bronze seal (gate ≈ y 0.78 of stage).
        if (privacyMuted) {
            val gate = Offset(w * 0.5f, w * 0.403f * (1653f / 720f) + w * 0.36f)
            drawCircle(Color(0xCCB3452B), radius = w * 0.055f, center = gate)
            drawCircle(Color(0xFFFFD9C2).copy(alpha = 0.9f), radius = w * 0.02f, center = gate)
        }
        // CHAT SPEECH MUTED: the audio-OUTPUT glyph only — microphone unchanged.
        if (chatSpeechMuted) {
            drawCircle(
                Color(0x882B2F3A),
                radius = w * 0.03f,
                center = Offset(core.x, core.y - w * 0.10f),
            )
        }
        // LISTENING / CONTINUOUS: moderate persistent cyan seal on the portal rim.
        if (listening) {
            drawCircle(Color(0x5527C4D8), radius = w * 0.115f, center = core)
        }
        // ERROR: controlled amber fracture (two calm arcs), never a flash.
        if (error) {
            drawArc(
                Color(0xB3E0A63C).copy(alpha = fractureAlpha),
                startAngle = -30f, sweepAngle = 22f, useCenter = false,
                style = Stroke(width = w * 0.012f),
                topLeft = Offset(core.x - w * 0.16f, core.y - w * 0.16f),
                size = androidx.compose.ui.geometry.Size(w * 0.32f, w * 0.32f),
            )
            drawArc(
                Color(0xB3E0A63C).copy(alpha = fractureAlpha * 0.8f),
                startAngle = 150f, sweepAngle = 16f, useCenter = false,
                style = Stroke(width = w * 0.010f),
                topLeft = Offset(core.x - w * 0.16f, core.y - w * 0.16f),
                size = androidx.compose.ui.geometry.Size(w * 0.32f, w * 0.32f),
            )
        }
        // SPEAKING: architectural light responds to the audio amplitude.
        if (speaking && amplitude > 0.02f) {
            drawCircle(
                Color(0x3327C4D8).copy(alpha = amplitude * 0.5f),
                radius = w * (0.14f + amplitude * 0.05f),
                center = core,
            )
        }
        // HIGH profile: calm drifting particles around the core (never default).
        if (particles) {
            repeat(14) { i ->
                val ang = (i * 25.7f + breathing * 36f) * (Math.PI / 180).toFloat()
                val rad = w * (0.20f + 0.03f * ((i % 4) + breathing))
                drawCircle(
                    Color(0x6627C4D8),
                    radius = w * 0.006f,
                    center = Offset(core.x + rad * kotlin.math.cos(ang), core.y + rad * kotlin.math.sin(ang) * 0.6f),
                )
            }
        }
    }
}
