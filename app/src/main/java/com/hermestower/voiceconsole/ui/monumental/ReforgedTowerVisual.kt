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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.hermestower.voiceconsole.R
import com.hermestower.voiceconsole.core.state.ConsoleState
import androidx.compose.ui.geometry.Offset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

/**
 * GRAPH 4.12 §26–§34 — NEW ORIGINAL CENTRAL TOWER (Owner image rejected the
 * previous cutout-derived set; PHYS-FAIL-03; CENTRAL_TOWER_ASSET_RECONSTRUCTION_FAIL).
 *
 * The shipped layers are ORIGINAL vector-drawn compositions (candidate A,
 * docs/visual/tower412/ASSET_MANIFEST.json) on ONE normalized stage
 * (1000×1375), designed from the start for transparency, symmetry, animation,
 * compact scaling, OLED safety and the LOW/BALANCED/HIGH budgets (§32).
 *
 * §24 STAGED DECODE: the first Console frame decodes ONLY the five static
 * architecture layers; the five animated mechanism layers (outer/middle/
 * inner/portal/energy) decode asynchronously right after — the first frame
 * never pays the full twelve-layer decode.
 *
 * §31 ANIMATION CONTRACT (per state):
 *   IDLE            slow outer motion, subtle portal breathing;
 *   LISTENING       incoming energy, open input gate;
 *   TRANSCRIBING    sequential ring pulses (phase-offset breathing);
 *   PROCESSING      differentiated mechanisms (three distinct rates/directions);
 *   SPEAKING        audio-amplitude response on the portal;
 *   CONTINUOUS      stable low-energy listening state;
 *   PRIVACY MUTED   input gate closed, red-bronze privacy seal;
 *   CHAT MUTED      output channel dimmed, microphone unaffected;
 *   DISCONNECTED    remote conduit off, local portal available;
 *   ERROR           controlled amber state, never flashing.
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

    // ---- §24 stage 1: static architecture (first frame) ----
    val rear = painterResource(R.drawable.tower_structure_rear)
    val sign = painterResource(R.drawable.tower_sign)
    val columns = painterResource(R.drawable.tower_columns)
    val gate = painterResource(R.drawable.tower_lower_gateway)
    val stairs = painterResource(R.drawable.tower_stairs_plinth)
    val foreground = painterResource(R.drawable.tower_foreground)

    // ---- §24 stage 2: animated mechanisms decode AFTER the first frame ----
    val context = LocalContext.current
    var mech by remember { mutableStateOf<TowerMechanismBitmaps?>(null) }
    LaunchedEffect(Unit) {
        mech = withContext(Dispatchers.IO) { TowerMechanismBitmaps.decode(context) }
    }

    BoxWithConstraints(modifier) {
        val density = LocalDensity.current
        // stage proportions: 1000 x 1375
        val stageW = maxWidth
        val stageH = stageW * (1375f / 1000f)
        val stageHp = with(density) { stageH.toPx() }
        val stageWp = with(density) { stageW.toPx() }

        // static stack (stage-wide, exact proportions)
        Image(rear, contentDescription = null, contentScale = ContentScale.FillWidth,
            modifier = Modifier.align(Alignment.TopCenter).fillMaxWidth())
        Image(sign, contentDescription = null, contentScale = ContentScale.FillWidth,
            modifier = Modifier.align(Alignment.TopCenter).fillMaxWidth())
        Image(columns, contentDescription = null, contentScale = ContentScale.FillWidth,
            modifier = Modifier.align(Alignment.TopCenter).fillMaxWidth())
        Image(gate, contentDescription = null, contentScale = ContentScale.FillWidth,
            modifier = Modifier.align(Alignment.TopCenter).fillMaxWidth())
        Image(stairs, contentDescription = null, contentScale = ContentScale.FillWidth,
            modifier = Modifier.align(Alignment.TopCenter).fillMaxWidth())
        Image(foreground, contentDescription = null, contentScale = ContentScale.FillWidth,
            modifier = Modifier.align(Alignment.TopCenter).fillMaxWidth())

        // oracle center: (0.5, 700/1375) of the stage; mechanism box 544/1000 wide
        val mechSize = stageW * (544f / 1000f)
        val mechSizePx = with(density) { mechSize.toPx() }
        val mechX = ((stageWp - mechSizePx) / 2f).roundToInt()
        val mechY = (stageHp * (700f / 1375f) - mechSizePx / 2f).roundToInt()

        // §31 rotation rates (differentiated per state)
        val outerAngle = if (frozen) 0f else infiniteAngle(
            when (state) {
                ConsoleState.TOWER_PROCESSING -> 22_000
                ConsoleState.CONTACTING_TOWER -> 28_000
                ConsoleState.UNDERSTANDING_LOCAL -> 40_000
                else -> 90_000
            },
        )
        val middleAngle = if (frozen) 0f else infiniteAngle(
            when (state) {
                ConsoleState.TOWER_PROCESSING -> -14_000
                ConsoleState.CONTACTING_TOWER -> -18_000
                else -> -64_000
            },
        )
        val innerAngle = if (frozen) 0f else infiniteAngle(
            when (state) {
                ConsoleState.UNDERSTANDING_LOCAL -> 20_000
                ConsoleState.TOWER_PROCESSING -> 34_000
                else -> 48_000
            },
        )
        val breathing = if (frozen) 0f else infiniteBreath()

        // §31 portal behavior
        val portalPulse = when {
            state == ConsoleState.SPEAKING -> 0.55f + 0.45f * speakingAmplitude.coerceIn(0f, 1f)
            state == ConsoleState.LISTENING -> 0.85f + 0.15f * breathing
            continuousActive -> 0.78f + 0.08f * breathing
            state == ConsoleState.TRANSCRIBING -> 0.55f + 0.35f * breathing
            state == ConsoleState.DISCONNECTED -> 0.45f + 0.08f * breathing
            else -> 0.62f + 0.10f * breathing
        }
        val portalAlpha by animateFloatAsState(portalPulse, label = "portalPulse")

        // §31 energy channel behavior (remote conduit vs local channel)
        val energyAlpha = when {
            privacyMuted -> 0f
            state == ConsoleState.CONTACTING_TOWER || state == ConsoleState.TOWER_PROCESSING -> 0.95f
            state == ConsoleState.LISTENING || continuousActive -> 0.8f
            state == ConsoleState.TRANSCRIBING -> 0.7f
            state == ConsoleState.DISCONNECTED -> 0.12f
            state == ConsoleState.SPEAKING -> 0.5f + 0.4f * speakingAmplitude.coerceIn(0f, 1f)
            else -> 0.4f
        }
        val innerGold = state == ConsoleState.UNDERSTANDING_LOCAL

        val m = mech
        if (m != null) {
            Image(m.energy.asImageBitmap(), contentDescription = null, contentScale = ContentScale.FillWidth,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .offset { IntOffset(mechX, mechY) }
                    .size(mechSize)
                    .alpha(energyAlpha))
            Image(m.outer.asImageBitmap(), contentDescription = null, contentScale = ContentScale.FillWidth,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .offset { IntOffset(mechX, mechY) }
                    .size(mechSize)
                    .graphicsLayer { rotationZ = outerAngle })
            Image(m.middle.asImageBitmap(), contentDescription = null, contentScale = ContentScale.FillWidth,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .offset { IntOffset(mechX, mechY) }
                    .size(mechSize)
                    .graphicsLayer { rotationZ = middleAngle })
            Image(m.inner.asImageBitmap(), contentDescription = null, contentScale = ContentScale.FillWidth,
                colorFilter = if (innerGold) ColorFilter.tint(Color(0x66FFC866)) else null,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .offset { IntOffset(mechX, mechY) }
                    .size(mechSize)
                    .graphicsLayer { rotationZ = innerAngle })
            Image(m.portal.asImageBitmap(), contentDescription = null, contentScale = ContentScale.FillWidth,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .offset { IntOffset(mechX, mechY) }
                    .size(mechSize)
                    .alpha(portalAlpha))
        }

        // §31 state overlays (Canvas, zero-bitmap memory): seals, dim, fracture
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

/** The five animated mechanism layers, decoded OFF the first frame (§24). */
class TowerMechanismBitmaps private constructor(
    val outer: android.graphics.Bitmap,
    val middle: android.graphics.Bitmap,
    val inner: android.graphics.Bitmap,
    val portal: android.graphics.Bitmap,
    val energy: android.graphics.Bitmap,
) {
    companion object {
        fun decode(context: android.content.Context): TowerMechanismBitmaps {
            fun d(id: Int): android.graphics.Bitmap =
                android.graphics.BitmapFactory.decodeResource(context.resources, id)
            return TowerMechanismBitmaps(
                outer = d(R.drawable.tower_outer_mechanism),
                middle = d(R.drawable.tower_middle_mechanism),
                inner = d(R.drawable.tower_inner_mechanism),
                portal = d(R.drawable.tower_portal),
                energy = d(R.drawable.tower_energy_paths),
            )
        }
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

/** §31 Canvas overlays — seals, dim, fracture, particles, speaking light. */
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
        val h = size.width * (1375f / 1000f) // stage-proportional overlay space
        val core = Offset(w * 0.5f, h * (700f / 1375f))

        // PRIVACY MUTED: input gate closed + red-bronze seal at the gateway.
        if (privacyMuted) {
            val gate = Offset(w * 0.5f, h * (1120f / 1375f))
            drawCircle(Color(0xCCB3452B), radius = w * 0.055f, center = gate)
            drawCircle(Color(0xFFFFD9C2).copy(alpha = 0.9f), radius = w * 0.02f, center = gate)
        }
        // CHAT SPEECH MUTED: the audio-OUTPUT channel dimmed (mic unaffected).
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
