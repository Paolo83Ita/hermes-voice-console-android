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
import com.hermestower.voiceconsole.R
import com.hermestower.voiceconsole.core.state.ConsoleState
import androidx.compose.ui.geometry.Offset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

/**
 * GRAPH 4.13 §29–§41 — MONUMENTAL ORACLE CITADEL (the Graph 4.12 Tower is
 * OWNER-REJECTED: CENTRAL_TOWER_GRAPH412_VISUAL_OWNER_REJECTED; the refounded
 * Tower is candidate A of docs/visual/tower413/ — selected for Owner-reference
 * fidelity first, then mobile readability).
 *
 * §36 LAYER CONTRACT (one normalized stage, 1000×1375; the mechanism layers
 * are cropped to the oracle box, stage bounds recorded in ASSET_MANIFEST.json):
 *
 *   z1  tower_architecture_rear   static   solid central body mass
 *   z2  tower_plinth              static   three-tier foundation (bottom-integrated)
 *   z3  tower_stairs              static   broad flaring flight
 *   z4  tower_lower_gate          static   arched gateway + its own order
 *   z5  tower_heavy_pylons        static   SOLID flanking masses, capitals+bases
 *   z6  tower_entablature         static   heavy stacked cornice + engraved frieze
 *   z7  tower_crown_sign          static   INTEGRATED aedicula sign (never floating)
 *   z8  tower_oracle_outer        rotate   outer bronze ring band (42% width)
 *   z9  tower_oracle_middle       rotate   middle engraved ring
 *   z10 tower_oracle_inner        rotate   inner bezel + gold marks
 *   z11 tower_portal              static   cyan glass + sigil (alpha breathing)
 *   z12 tower_energy              static   spine + gate glow (alpha per state)
 *   z13 tower_state_overlay       —        empty; state truth stays Canvas-side
 *
 * §24 STAGED DECODE: the first frame decodes ONLY the eight static layers;
 * the five mechanism layers decode asynchronously right after.
 *
 * §40 PERFORMANCE PROFILES:
 *   LOW      the `towerl_*` asset tier (≈10 MB decoded), rings STATIC,
 *            portal breathing + state alphas kept (cheap), no particles;
 *   BALANCED the full-resolution tier (≈22 MB decoded), full §39 animation;
 *   HIGH     optional (never default; thermal governor may demote).
 * Architecture and composition are IDENTICAL across tiers (§40: never degrade
 * the visual architecture to meet the budget — only resolution differs).
 *
 * §31 ANIMATION CONTRACT (per state, unchanged from the 4.12 truth):
 *   IDLE slow outer motion; LISTENING incoming energy; TRANSCRIBING sequential
 *   ring pulses; PROCESSING differentiated mechanisms; SPEAKING portal responds
 *   to audio amplitude; CONTINUOUS persistent low-energy state; PRIVACY MUTED
 *   closed input gate; CHAT MUTED dimmed output; DISCONNECTED conduit off;
 *   ERROR controlled amber fracture, never flashing.
 */
object TowerLayerContract {
    /** Static architecture layers in z-order (first-frame decode set). */
    val STATIC_BALANCED = intArrayOf(
        R.drawable.tower_architecture_rear,
        R.drawable.tower_plinth,
        R.drawable.tower_stairs,
        R.drawable.tower_lower_gate,
        R.drawable.tower_heavy_pylons,
        R.drawable.tower_entablature,
        R.drawable.tower_crown_sign,
        R.drawable.tower_state_overlay,
    )
    val STATIC_LOW = intArrayOf(
        R.drawable.towerl_tower_architecture_rear,
        R.drawable.towerl_tower_plinth,
        R.drawable.towerl_tower_stairs,
        R.drawable.towerl_tower_lower_gate,
        R.drawable.towerl_tower_heavy_pylons,
        R.drawable.towerl_tower_entablature,
        R.drawable.towerl_tower_crown_sign,
        R.drawable.towerl_tower_state_overlay,
    )

    /** Oracle mechanism box, in stage units of 1000 width. */
    const val MECH_BOX_STAGE = 476
    /** Oracle core center in stage coordinates. */
    const val CORE_CX_STAGE = 500
    const val CORE_CY_STAGE = 640
    const val STAGE_W = 1000
    const val STAGE_H = 1375
}

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

    // ---- §24 stage 1: static architecture (first frame, tier-aware) ----
    val staticIds = if (low) TowerLayerContract.STATIC_LOW else TowerLayerContract.STATIC_BALANCED
    val staticPainters = staticIds.map { painterResource(it) }

    // ---- §24 stage 2: animated mechanisms decode AFTER the first frame ----
    val context = LocalContext.current
    var mech by remember { mutableStateOf<TowerMechanismBitmaps?>(null) }
    LaunchedEffect(quality) {
        mech = withContext(Dispatchers.IO) { TowerMechanismBitmaps.decode(context, low) }
    }

    BoxWithConstraints(modifier) {
        val density = LocalDensity.current
        val stageW = maxWidth
        val stageH = stageW * (TowerLayerContract.STAGE_H.toFloat() / TowerLayerContract.STAGE_W)
        val stageHp = with(density) { stageH.toPx() }
        val stageWp = with(density) { stageW.toPx() }

        staticPainters.forEach { painter ->
            Image(painter, contentDescription = null, contentScale = ContentScale.FillWidth,
                modifier = Modifier.align(Alignment.TopCenter).fillMaxWidth())
        }

        // §36 oracle box: cropped mech layers centered on the stage core center
        val mechSize = stageW * (TowerLayerContract.MECH_BOX_STAGE.toFloat() / TowerLayerContract.STAGE_W)
        val mechSizePx = with(density) { mechSize.toPx() }
        val mechX = ((stageWp - mechSizePx) / 2f).roundToInt()
        val mechY = (stageHp * (TowerLayerContract.CORE_CY_STAGE.toFloat() / TowerLayerContract.STAGE_H) - mechSizePx / 2f).roundToInt()

        // §31 rotation rates (differentiated per state). LOW profile: rings
        // STATIC (§40 reduced ring animation) — architecture never degrades.
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
        // LOW keeps ONE slow ring alive (reduced — not zero — ring animation)
        val innerRate = if (low) 110_000 else when (state) {
            ConsoleState.UNDERSTANDING_LOCAL -> 20_000
            ConsoleState.TOWER_PROCESSING -> 34_000
            else -> 48_000
        }
        val innerAngle = if (reducedMotion) 0f else infiniteAngle(innerRate)
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
            Image(m.energy.asImageBitmap(), contentDescription = null, contentScale = ContentScale.FillBounds,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .offset { IntOffset(mechX, mechY) }
                    .size(mechSize)
                    .alpha(energyAlpha))
            Image(m.outer.asImageBitmap(), contentDescription = null, contentScale = ContentScale.FillBounds,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .offset { IntOffset(mechX, mechY) }
                    .size(mechSize)
                    .graphicsLayer { rotationZ = outerAngle })
            Image(m.middle.asImageBitmap(), contentDescription = null, contentScale = ContentScale.FillBounds,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .offset { IntOffset(mechX, mechY) }
                    .size(mechSize)
                    .graphicsLayer { rotationZ = middleAngle })
            Image(m.inner.asImageBitmap(), contentDescription = null, contentScale = ContentScale.FillBounds,
                colorFilter = if (innerGold) ColorFilter.tint(Color(0x66FFC866)) else null,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .offset { IntOffset(mechX, mechY) }
                    .size(mechSize)
                    .graphicsLayer { rotationZ = innerAngle })
            Image(m.portal.asImageBitmap(), contentDescription = null, contentScale = ContentScale.FillBounds,
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
        fun decode(context: android.content.Context, low: Boolean): TowerMechanismBitmaps {
            fun d(id: Int): android.graphics.Bitmap =
                android.graphics.BitmapFactory.decodeResource(context.resources, id)
            return if (low) {
                TowerMechanismBitmaps(
                    outer = d(R.drawable.towerl_tower_oracle_outer),
                    middle = d(R.drawable.towerl_tower_oracle_middle),
                    inner = d(R.drawable.towerl_tower_oracle_inner),
                    portal = d(R.drawable.towerl_tower_portal),
                    energy = d(R.drawable.towerl_tower_energy),
                )
            } else {
                TowerMechanismBitmaps(
                    outer = d(R.drawable.tower_oracle_outer),
                    middle = d(R.drawable.tower_oracle_middle),
                    inner = d(R.drawable.tower_oracle_inner),
                    portal = d(R.drawable.tower_portal),
                    energy = d(R.drawable.tower_energy),
                )
            }
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
        val h = size.width * (TowerLayerContract.STAGE_H.toFloat() / TowerLayerContract.STAGE_W)
        val core = Offset(w * 0.5f, h * (TowerLayerContract.CORE_CY_STAGE.toFloat() / TowerLayerContract.STAGE_H))

        // PRIVACY MUTED: input gate closed + red-bronze seal at the gateway.
        if (privacyMuted) {
            val gate = Offset(w * 0.5f, h * (1150f / TowerLayerContract.STAGE_H))
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
