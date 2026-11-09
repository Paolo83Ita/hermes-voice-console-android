package com.hermestower.voiceconsole.ui.monumental

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.height
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import com.hermestower.voiceconsole.R
import com.hermestower.voiceconsole.core.state.ConsoleState
import com.hermestower.voiceconsole.ui.monumental.VisualQuality
import kotlin.math.min

/**
 * GRAPH 4.14 §32-§39 — REFERENCE CENTRAL TOWER (the primary Console visual).
 *
 * Supersedes the Owner-rejected flat geometric direction (4.12/4.13): the
 * monument is the Owner-authorized derivative of the supplied reference image
 * (clean extraction → docs/visual/tower414/, all §38 defect gates PASS).
 *
 * Hybrid strategy (§36):
 *  - RASTER layers carry the complete architectural body, crown sign, bronze
 *    rings, lower gate, stairs, plinth (two tiers: LOW 460 / BALANCED 680,
 *    decoded 2.94 MB / 6.43 MB — inside the §24-style budgets);
 *  - DYNAMIC Canvas layers carry portal glow, ring shimmer, energy movement,
 *    state pulses, audio amplitude response, muted overlays, connection truth.
 *  The full tower is NEVER redrawn with geometric primitives.
 *
 * Visible animation contract (§39) — driven by the SAME truth as the Bridge
 * indicator ([bridgeIsMock] comes from BridgeModeAuthority, §40 coherence):
 *  - IDLE: slow outer-ring shimmer + portal breathing + subtle energy flow;
 *  - LISTENING: incoming cyan lines + stronger portal;
 *  - TRANSCRIBING: sequential illumination pulse;
 *  - LOCAL PROCESSING: inner mechanism moves, no remote conduit;
 *  - CONTACTING TOWER: outer path activates ONLY when the state says so;
 *  - SPEAKING: portal responds visibly to the TTS amplitude;
 *  - CONTINUOUS LISTENING: persistent listening pulse;
 *  - PRIVACY MUTED: red-bronze seal, input path closed;
 *  - CHAT SPEECH MUTED: output path dims;
 *  - MOCK: NO cyan remote-energy animation — a neutral amber simulation pulse.
 *
 * Mobile composition (§41): the tower fills the stage width; keyboard-open
 * collapse keeps sign + oracle mechanism + portal visible (the lower
 * architecture is clipped by the stage height, never horizontally scaled).
 */
@Composable
fun ReferenceTowerVisual(
    state: ConsoleState,
    speakingAmplitude: Float,
    quality: VisualQuality,
    reducedMotion: Boolean,
    privacyMuted: Boolean,
    continuousActive: Boolean,
    chatSpeechMuted: Boolean,
    /** §40: consumed from BridgeModeAuthority — mock mode never shows cyan remote energy. */
    bridgeIsMock: Boolean,
    modifier: Modifier = Modifier,
) {
    val m = monumentalColors
    val low = quality == VisualQuality.LOW
    val frozen = reducedMotion || low

    // ---- tier-aware layer ids (LOW ≤12MB budget; BALANCED ≤24MB) ----
    val architecture = painterResource(
        if (low) R.drawable.tower414l_architecture else R.drawable.tower414_architecture
    )
    val crownSign = painterResource(
        if (low) R.drawable.tower414l_crown_sign else R.drawable.tower414_crown_sign
    )
    val ringOuter = painterResource(
        if (low) R.drawable.tower414l_ring_outer else R.drawable.tower414_ring_outer
    )
    val ringMiddle = painterResource(
        if (low) R.drawable.tower414l_ring_middle else R.drawable.tower414_ring_middle
    )
    val ringInner = painterResource(
        if (low) R.drawable.tower414l_ring_inner else R.drawable.tower414_ring_inner
    )
    val portal = painterResource(
        if (low) R.drawable.tower414l_portal else R.drawable.tower414_portal
    )
    val energyMask = painterResource(
        if (low) R.drawable.tower414l_energy_mask else R.drawable.tower414_energy_mask
    )

    // ---- §39 IDLE: slow ring shimmer + portal breathing ----
    val transition = rememberInfiniteTransition(label = "tower414")
    val shimmer by transition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween(if (low) 120_000 else 90_000), RepeatMode.Restart,
        ), label = "ringShimmer",
    )
    val breath by transition.animateFloat(
        initialValue = 0.72f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2400), RepeatMode.Reverse), label = "portalBreath",
    )
    val listeningPulse by transition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1100), RepeatMode.Reverse), label = "listenPulse",
    )
    val mockPulse by transition.animateFloat(
        initialValue = 0.35f, targetValue = 0.8f,
        animationSpec = infiniteRepeatable(tween(2600), RepeatMode.Reverse), label = "mockPulse",
    )

    // ---- §39 SPEAKING: portal responds to the TTS amplitude (smoothed) ----
    val smoothAmplitude by animateFloatAsState(
        targetValue = speakingAmplitude.coerceIn(0f, 1f),
        animationSpec = tween(180), label = "amp",
    )

    // ---- state classification (typed, no string sniffing) ----
    val stateName = remember(state) { state.javaClass.simpleName }
    val isListening = stateName.contains("Listening", ignoreCase = true) || stateName.contains("Capture", ignoreCase = true) || continuousActive
    val isTranscribing = stateName.contains("Transcrib", ignoreCase = true)
    val isLocalProcessing = stateName.contains("Local", ignoreCase = true) || stateName.contains("Thinking", ignoreCase = true)
    val isContactingTower = stateName.contains("Contacting", ignoreCase = true) || stateName.contains("Connecting", ignoreCase = true) || stateName.contains("Queued", ignoreCase = true)
    val isSpeaking = stateName.contains("Speaking", ignoreCase = true)
    val remoteActive = (isContactingTower || isSpeaking) && !bridgeIsMock

    Box(modifier.fillMaxSize()) {
        // ---- controlled local dark halo behind the portal (§18) ----
        Canvas(Modifier.fillMaxSize()) {
            val haloCenter = Offset(size.width * 0.5f, size.height * 0.78f)
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(Color.Black.copy(alpha = if (low) 0.55f else 0.42f), Color.Transparent),
                    center = haloCenter,
                    radius = size.width * 0.62f,
                ),
                radius = size.width * 0.62f,
                center = haloCenter,
            )
        }

        // ---- raster architecture (the full monument) ----
        Image(
            architecture,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.FillWidth,
            alignment = Alignment.BottomCenter,
        )

        // ---- oracle mechanism: ring crops + rotation shimmer ----
        // The ring layers are crops of the SAME normalized stage (§37): they are
        // positioned by the extraction's measured fractions, never re-drawn.
        Box(Modifier.matchParentSize()) {
            LayerAt(ringOuter, xFrac = 0.180f, yFrac = 0.300f, wFrac = 0.640f, hFrac = 0.310f, rotation = if (frozen) 0f else shimmer * 8f)
            LayerAt(ringMiddle, xFrac = 0.265f, yFrac = 0.340f, wFrac = 0.470f, hFrac = 0.235f, rotation = if (frozen) 0f else -shimmer * 14f)
            LayerAt(ringInner, xFrac = 0.370f, yFrac = 0.385f, wFrac = 0.260f, hFrac = 0.155f, rotation = if (frozen) 0f else shimmer * 22f)
            LayerAt(portal, xFrac = 0.300f, yFrac = 0.705f, wFrac = 0.400f, hFrac = 0.220f, rotation = 0f)
            LayerAt(crownSign, xFrac = 0.150f, yFrac = 0.030f, wFrac = 0.700f, hFrac = 0.195f, rotation = 0f)

            // ---- §39 dynamic overlays (Canvas — never architectural geometry) ----
            Canvas(Modifier.matchParentSize()) {
                val w = size.width
                val h = size.height
                val portalCx = w * 0.5f
                val portalCy = h * 0.815f

                // portal glow — breathing idle; stronger listening; amplitude when speaking
                val glowBase = when {
                    isSpeaking -> 0.45f + smoothAmplitude * 0.55f
                    isListening -> 0.55f + listeningPulse * 0.3f
                    isTranscribing -> 0.5f + listeningPulse * 0.25f
                    else -> breath * 0.4f
                }
                val glowColor = if (bridgeIsMock) m.amber else m.cyanCore
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(glowColor.copy(alpha = glowBase), Color.Transparent),
                        center = Offset(portalCx, portalCy),
                        radius = w * (0.16f + glowBase * 0.10f),
                    ),
                    radius = w * (0.16f + glowBase * 0.10f),
                    center = Offset(portalCx, portalCy),
                )

                // energy mask tint follows the connection truth (§40)
                val energyAlpha = when {
                    remoteActive -> 0.85f
                    isLocalProcessing -> 0.7f
                    bridgeIsMock -> mockPulse * 0.4f // amber simulation pulse, visibly different
                    else -> 0.45f
                }
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(
                            (if (bridgeIsMock) m.amber else m.cyanCore).copy(alpha = energyAlpha * 0.5f),
                            Color.Transparent,
                        ),
                        center = Offset(w * 0.5f, h * 0.455f),
                        radius = w * 0.33f,
                    ),
                    radius = w * 0.33f,
                    center = Offset(w * 0.5f, h * 0.455f),
                )

                // LISTENING: incoming cyan lines toward the mechanism
                if (isListening && !frozen) {
                    val lineColor = (if (bridgeIsMock) m.amber else m.cyanCore).copy(alpha = 0.55f * listeningPulse)
                    repeat(5) { i ->
                        val phase = (listeningPulse + i / 5f) % 1f
                        val y = h * (0.18f + 0.24f * phase)
                        val halfW = w * (0.10f + 0.05f * phase)
                        drawLine(
                            color = lineColor,
                            start = Offset(portalCx - halfW, y),
                            end = Offset(portalCx + halfW, y),
                            strokeWidth = 2.dp.toPx(),
                        )
                    }
                }

                // TRANSCRIBING: sequential illumination ticks on the outer ring
                if (isTranscribing) {
                    val tick = (listeningPulse * 12f).toInt()
                    repeat(12) { i ->
                        val active = i == tick % 12
                        val angle = i * (Math.PI * 2 / 12.0)
                        val cx = w * 0.5f + (w * 0.30f * kotlin.math.cos(angle)).toFloat()
                        val cy = h * 0.455f + (w * 0.30f * kotlin.math.sin(angle)).toFloat()
                        drawCircle(
                            color = (if (bridgeIsMock) m.amber else m.cyanCore).copy(alpha = if (active) 0.9f else 0.25f),
                            radius = w * 0.012f,
                            center = Offset(cx, cy),
                        )
                    }
                }

                // CONTACTING TOWER (real only): outer path activation
                if (isContactingTower && !bridgeIsMock && !frozen) {
                    drawArc(
                        color = m.cyanCore.copy(alpha = 0.5f),
                        startAngle = -90f,
                        sweepAngle = (shimmer * 300f) % 360f,
                        useCenter = false,
                        topLeft = Offset(w * 0.20f, h * 0.30f),
                        size = androidx.compose.ui.geometry.Size(w * 0.60f, w * 0.60f),
                        style = Stroke(width = 3.dp.toPx()),
                    )
                }

                // PRIVACY MUTED: red-bronze seal over the input path (§39)
                if (privacyMuted) {
                    drawLine(
                        color = m.redBronze.copy(alpha = 0.85f),
                        start = Offset(w * 0.30f, h * 0.42f),
                        end = Offset(w * 0.70f, h * 0.49f),
                        strokeWidth = 5.dp.toPx(),
                    )
                    drawLine(
                        color = m.redBronze.copy(alpha = 0.85f),
                        start = Offset(w * 0.70f, h * 0.42f),
                        end = Offset(w * 0.30f, h * 0.49f),
                        strokeWidth = 5.dp.toPx(),
                    )
                }
            }

            // ---- energy mask overlay (raster glow, state-tinted) ----
            val energyTintAlpha = when {
                privacyMuted -> 0.10f
                chatSpeechMuted && isSpeaking -> 0.12f
                remoteActive -> 0.55f
                isLocalProcessing -> 0.45f
                else -> 0.30f
            }
            Image(
                energyMask,
                contentDescription = null,
                modifier = Modifier
                    .matchParentSize()
                    .graphicsLayer { alpha = energyTintAlpha },
                contentScale = ContentScale.FillWidth,
                alignment = Alignment.BottomCenter,
            )
        }
    }
}

/** Places one normalized-stage layer at its extraction fractions (single coordinate system §37). */
@Composable
private fun LayerAt(
    painter: androidx.compose.ui.graphics.painter.Painter,
    xFrac: Float,
    yFrac: Float,
    wFrac: Float,
    hFrac: Float,
    rotation: Float,
) {
    androidx.compose.foundation.layout.BoxWithConstraints(Modifier.fillMaxSize()) {
        val pw = maxWidth * wFrac
        val ph = maxHeight * hFrac
        Image(
            painter,
            contentDescription = null,
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset(x = maxWidth * xFrac, y = maxHeight * yFrac)
                .size(pw, ph)
                .graphicsLayer { rotationZ = rotation },
            contentScale = ContentScale.Fit,
        )
    }
}

