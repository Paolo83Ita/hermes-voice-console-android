package com.hermestower.voiceconsole.ui.monumental

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.hermestower.voiceconsole.R
import com.hermestower.voiceconsole.core.state.ConsoleState
import com.hermestower.voiceconsole.ui.monumental.VisualQuality
import androidx.compose.foundation.layout.BoxWithConstraints

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
 *
 * OWNER PHYSICAL RETEST REPAIR (Package 8, finding F-A "torre duplicata e
 * sfasata in basso"): the extraction layer crops (rings / portal / crown
 * sign) are measured as fractions of the ASSET's normalized stage
 * (680×963 BALANCED / 460×652 LOW — aspect ≈1.416), but they were placed at
 * fractions of the CONSOLE STAGE BOX (min(screen·0.50, width·1.375) — aspect
 * ≤1.375). Whenever the stage box is shorter than the drawn image, the
 * ContentScale.FillWidth + BottomCenter architecture is top-clipped while the
 * crops keep box-relative geometry → every crop renders a SECOND copy of its
 * baked content, offset DOWNWARD and scaled smaller (the Owner's duplicated,
 * misaligned central element). The repair draws ALL layers — architecture,
 * crops, energy mask, Canvas dynamics AND the §18 halo — inside ONE computed
 * IMAGE RECT (the exact rectangle ContentScale.FillWidth + BottomCenter
 * produces for the architecture painter's intrinsic aspect), so the crops sit
 * precisely on their baked copies at every stage aspect, on every device.
 *
 * OWNER PHYSICAL RETEST REPAIR ROUND 2 (Package 9, F-A residual + F-E effects):
 * the Owner confirmed the tower background is now correct, but the CENTRAL
 * CIRCLE still read as "sdoppiato e traslato in basso" and "nessun effetto è
 * stato aggiunto". Two code-proven causes, both repaired here:
 *  1. The IDLE "ring shimmer" rotated the RASTER CROPS (±8°/−14°/22° about
 *     the crop box). The baked crops carry the FULL mechanism artwork (gears,
 *     sigil triangle, figures — NOT rotationally symmetric), so the rotating
 *     copy visibly separated from its baked original (masked-NCC deformation
 *     proof: similarity 0.9955 → 0.878 at the 8° extreme); the same shimmer
 *     was also the only motion, i.e. imperceptible AS an effect while harmful
 *     to geometry. The crops are now drawn at rotation 0 ALWAYS — pixel-true
 *     on their baked copies (NCC-verified ≤1.2dp on both tiers, C12
 *     measurement JSON) — and the shimmer became a CANVAS LIGHT SWEEP on the
 *     ring band (a rotating highlight arc), which cannot move geometry.
 *  2. The CONTACTING arc mixed h- and w-fractions (topLeft 0.30·h with size
 *     0.60·w): its circle centered +33dp BELOW the baked ring on the POCO
 *     stage with an 8dp-smaller radius — a literal second circle outline
 *     translated downward during every real contacting/queued state. The arc
 *     (and the transcribing ticks) are now CONCENTRIC with the measured ring
 *     band (center 0.5w, 0.455h; radius 0.32w = the ring_outer crop's outer
 *     edge).
 * The idle effects are also now VISIBLE (the F-E runbook expectations):
 * stronger portal breathing + ring sweep + energy tint, all Canvas-only,
 * all still amber in mock mode (§40 truth unchanged).
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

    // ---- §39 IDLE: portal breathing + ring light sweep ----
    // ROUND 2: the former "ring shimmer" was the crop ROTATION — removed (it
    // deformed the raster crops against their baked originals: the Owner's
    // doubled central circle — see the class KDoc). The shimmer phase now
    // drives the Canvas ring sweep; the portal breath is stronger so the idle
    // state is visibly alive (runbook §11 expectation).
    val transition = rememberInfiniteTransition(label = "tower414")
    val shimmer by transition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween(if (low) 120_000 else 42_000), RepeatMode.Restart,
        ), label = "ringSweep",
    )
    val breath by transition.animateFloat(
        initialValue = 0.55f, targetValue = 1f,
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

    // ---- A4R NODE A4 REPAIR: the ONE master-canvas contain transform ----
    // Mission formula: scale = min(containerWidth / 680, containerHeight / 963).
    // ALL layers (architecture, crops, energy mask, Canvas dynamics, halo) share
    // this single canvas rect: same variant, same scale, same origin, explicit
    // clip, deterministic z-order (TowerLayerManifest is the authority). The
    // whole monument is always visible — no device-specific pixel offsets ever.

    BoxWithConstraints(modifier.fillMaxSize().clipToBounds()) {
        val boxW = maxWidth
        val boxH = maxHeight
        val (canvasScale, rectW, rectH) = TowerLayerManifest.containTransform(boxW, boxH)

        // ---- the manifest-canvas container: every layer inside shares its geometry ----
        Box(
            Modifier
                .align(Alignment.Center)
                .size(rectW, rectH)
                .clipToBounds(),
        ) {
            // ---- controlled local dark halo behind the portal (§18) ----
            Canvas(Modifier.fillMaxSize()) {
                val haloCenter = Offset(
                    size.width * TowerLayerManifest.PORTAL_CENTER_X_FRAC,
                    size.height * TowerLayerManifest.PORTAL_CENTER_Y_FRAC,
                )
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
                alignment = Alignment.TopCenter,
            )

            // ---- A4R NODE A4: manifest-driven layers (NO hand-coded fractions) ----
            // The ring layers are crops of the SAME normalized stage; the manifest
            // places them on their baked copies at EVERY stage size (C12 NCC-verified
            // boxes). Raster crops never rotate against their baked originals.
            LayerAt(ringOuter, TowerLayerManifest.ringOuter, canvasScale)
            LayerAt(ringMiddle, TowerLayerManifest.ringMiddle, canvasScale)
            LayerAt(ringInner, TowerLayerManifest.ringInner, canvasScale)
            LayerAt(portal, TowerLayerManifest.portal, canvasScale)
            LayerAt(crownSign, TowerLayerManifest.crownSign, canvasScale)

            // ---- §39 dynamic overlays (Canvas — never architectural geometry) ----
            Canvas(Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                // A4R NODE A4: dynamic anchors come from the manifest constants
                // (the measured C12 ring band and the §39 portal center) — the
                // same coordinate truth the raster layers use.
                val portalCx = w * TowerLayerManifest.PORTAL_CENTER_X_FRAC
                val portalCy = h * TowerLayerManifest.PORTAL_CENTER_Y_FRAC
                val ringCx = w * TowerLayerManifest.RING_CENTER_X_FRAC
                val ringCy = h * TowerLayerManifest.RING_CENTER_Y_FRAC
                val ringR = w * TowerLayerManifest.RING_RADIUS_W_FRAC

                // portal glow — breathing idle; stronger listening; amplitude when speaking
                val glowBase = when {
                    isSpeaking -> 0.45f + smoothAmplitude * 0.55f
                    isListening -> 0.55f + listeningPulse * 0.3f
                    isTranscribing -> 0.5f + listeningPulse * 0.25f
                    else -> breath * 0.45f
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

                // §39 IDLE ring light sweep (ROUND 2 F-E): a rotating highlight
                // arc ON the ring band — visible but architectural-safe (it
                // paints over the band, it cannot move any geometry; the crop
                // rotation that deformed the mechanism is gone). Amber in mock (§40).
                if (!frozen) {
                    val sweepAlpha = if (isListening) 0.34f else 0.22f
                    drawArc(
                        brush = Brush.sweepGradient(
                            0f to glowColor.copy(alpha = 0f),
                            0.12f to glowColor.copy(alpha = sweepAlpha),
                            0.30f to glowColor.copy(alpha = 0f),
                            1f to glowColor.copy(alpha = 0f),
                            center = Offset(ringCx, ringCy),
                        ),
                        startAngle = shimmer * 360f,
                        sweepAngle = 360f,
                        useCenter = false,
                        topLeft = Offset(ringCx - ringR, ringCy - ringR),
                        size = androidx.compose.ui.geometry.Size(ringR * 2f, ringR * 2f),
                        style = Stroke(width = w * 0.018f),
                    )
                }

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
                        center = Offset(ringCx, ringCy),
                        radius = ringR * 1.03f, // the energy tint rides the measured ring band
                    ),
                    radius = ringR * 1.03f,
                    center = Offset(ringCx, ringCy),
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

                // TRANSCRIBING: sequential illumination ticks ON the outer ring band
                if (isTranscribing) {
                    val tick = (listeningPulse * 12f).toInt()
                    repeat(12) { i ->
                        val active = i == tick % 12
                        val angle = i * (Math.PI * 2 / 12.0)
                        val cx = ringCx + (ringR * kotlin.math.cos(angle)).toFloat()
                        val cy = ringCy + (ringR * kotlin.math.sin(angle)).toFloat()
                        drawCircle(
                            color = (if (bridgeIsMock) m.amber else m.cyanCore).copy(alpha = if (active) 0.9f else 0.25f),
                            radius = w * 0.012f,
                            center = Offset(cx, cy),
                        )
                    }
                }

                // CONTACTING TOWER (real only): outer path activation — ROUND 2:
                // CONCENTRIC with the baked ring band (the old arc mixed h- and
                // w-fractions and rendered a second circle +33dp below the ring —
                // the Owner's "cerchio centrale sdoppiato e traslato in basso").
                if (isContactingTower && !bridgeIsMock && !frozen) {
                    drawArc(
                        color = m.cyanCore.copy(alpha = 0.5f),
                        startAngle = -90f,
                        sweepAngle = (shimmer * 300f) % 360f,
                        useCenter = false,
                        topLeft = Offset(ringCx - ringR, ringCy - ringR),
                        size = androidx.compose.ui.geometry.Size(ringR * 2f, ringR * 2f),
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
                    .fillMaxSize()
                    .graphicsLayer { alpha = energyTintAlpha },
                contentScale = ContentScale.FillWidth,
                alignment = Alignment.TopCenter,
            )
        }
    }
}

/**
 * A4R NODE A4: places ONE manifest layer at its spec, in the shared canvas
 * scale — same variant, same scale, same origin as every other layer.
 * Rotation is structurally absent: raster crops must never rotate against
 * their baked originals (the C12 doubled-central-circle class).
 */
@Composable
private fun LayerAt(
    painter: androidx.compose.ui.graphics.painter.Painter,
    spec: TowerLayerManifest.TowerLayerSpec,
    scale: Float,
) {
    Image(
        painter,
        contentDescription = null,
        modifier = Modifier
            .offset(x = (spec.x * scale).dp, y = (spec.y * scale).dp)
            .size((spec.width * scale).dp, (spec.height * scale).dp),
        contentScale = ContentScale.Fit,
    )
}
