package com.hermestower.voiceconsole.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Send
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hermestower.voiceconsole.app.AppGraph
import com.hermestower.voiceconsole.core.mode.OperatingMode
import com.hermestower.voiceconsole.core.state.ConsoleState
import com.hermestower.voiceconsole.core.state.ConnectionStatus
import com.hermestower.voiceconsole.ui.components.MeanderBand
import com.hermestower.voiceconsole.ui.components.ModeChip
import com.hermestower.voiceconsole.ui.components.StatusPill
import com.hermestower.voiceconsole.ui.monumental.BurnInRegion
import com.hermestower.voiceconsole.ui.monumental.MonumentalColors
import com.hermestower.voiceconsole.ui.monumental.MonumentalType
import com.hermestower.voiceconsole.ui.monumental.OracleCore
import com.hermestower.voiceconsole.ui.monumental.VisualQuality
import com.hermestower.voiceconsole.ui.monumental.burnInOffset
import com.hermestower.voiceconsole.ui.monumental.monumentalColors
import com.hermestower.voiceconsole.ui.monumental.rememberBurnInStep
import com.hermestower.voiceconsole.ui.theme.hermesColors

/**
 * S1 Main Voice Console — GRAPH 3 monumental refoundation.
 * The screen is a live window into the Oracle Core; every state, control,
 * description and live region of the Graph 2 console is preserved (mission §4
 * preservation contract). Visual identity per MONUMENTAL_ORACLE_TOWER_VISUAL_SYSTEM.
 * Pinned E2E strings ("HERMES", control content descriptions) are preserved verbatim.
 */
@Composable
fun ConsoleScreen(
    graph: AppGraph,
    onOpenHistory: () -> Unit,
    onOpenNavigation: () -> Unit = {},
) {
    val m = monumentalColors
    val state by graph.store.state.collectAsStateWithLifecycle()
    val requestedMode by graph.store.requestedMode.collectAsStateWithLifecycle()
    val effectiveMode by graph.store.effectiveMode.collectAsStateWithLifecycle()
    val connection by graph.store.connection.collectAsStateWithLifecycle()
    val muted by graph.store.privacyMuted.collectAsStateWithLifecycle()
    val turns by graph.store.turns.collectAsStateWithLifecycle()
    val draft by graph.store.transcriptDraft.collectAsStateWithLifecycle()
    val caption by graph.store.caption.collectAsStateWithLifecycle()
    val amplitude by graph.store.speakingAmplitude.collectAsStateWithLifecycle()
    val queueCount by graph.store.queueCount.collectAsStateWithLifecycle()
    val statusNote by graph.store.statusNote.collectAsStateWithLifecycle()
    val settings by graph.store.settings.collectAsStateWithLifecycle()

    var showDialog by remember { mutableStateOf(false) }
    var injectText by remember { mutableStateOf("") }

    // GRAPH 3 per-region burn-in safeguard (mission §15; replaces whole-console drift).
    val burnStep = rememberBurnInStep(settings.burnInGuard, settings.reducedMotion)
    val quality = VisualQuality.from(settings.visualQuality)

    Box(
        Modifier
            .fillMaxSize()
            .background(m.obsidianDeep),
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 16.dp),
        ) {
            // ---- command frieze (top area ~13%) ----
            Box(
                Modifier
                    .fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(Color(0xFF0A0E16), Color(0xFF070A11)))),
            ) {
                // navigation chamber access — same content description as Graph 2 E2E pins
                IconButton(
                    onClick = onOpenNavigation,
                    modifier = Modifier.align(Alignment.TopStart).padding(start = 2.dp, top = 2.dp),
                ) {
                    Icon(
                        Icons.Filled.Menu,
                        contentDescription = "Open navigation drawer",
                        tint = m.bronzeLight,
                    )
                }
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .burnInOffset(burnStep, BurnInRegion.FRIEZE)
                        .padding(top = 18.dp, bottom = 10.dp),
                ) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text("HERMES", style = MonumentalType.friezeTitle, color = m.gold)
                    Spacer(Modifier.padding(2.dp))
                    Text("TOWER", style = MonumentalType.friezeTitle, color = m.bronzeLight)
                }
                Text(
                    "VOICE ORACLE CONSOLE",
                    style = MonumentalType.friezeSubtitle,
                    color = m.inkSecondary,
                    modifier = Modifier.padding(top = 3.dp),
                )
                MeanderBand(colors = hermesColors, modifier = Modifier.padding(top = 6.dp))
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                ) {
                    OperatingMode.selectable.forEach { mode ->
                        ModeChip(
                            label = mode.label,
                            selected = requestedMode == mode,
                            enabled = true,
                            onClick = { graph.driver.setRequestedMode(mode) },
                        )
                    }
                }
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    StatusPill(
                        label = when (connection.status) {
                            ConnectionStatus.BridgeStatus.CONNECTED -> "BRIDGE OK"
                            ConnectionStatus.BridgeStatus.CONNECTING -> "BRIDGE…"
                            ConnectionStatus.BridgeStatus.DISCONNECTED -> "OFFLINE"
                            ConnectionStatus.BridgeStatus.UNCONFIGURED -> "MOCK"
                        },
                        dotColor = when (connection.status) {
                            ConnectionStatus.BridgeStatus.CONNECTED -> m.cyanCore
                            ConnectionStatus.BridgeStatus.CONNECTING -> m.amber
                            else -> m.redBronze
                        },
                        description = "Tower bridge: ${connection.status.name.lowercase()}, endpoint ${connection.endpointLabel}",
                    )
                    if (queueCount > 0) {
                        StatusPill(
                            label = "QUEUED ×$queueCount",
                            dotColor = m.amber,
                            description = "$queueCount requests queued for the Tower",
                        )
                    }
                    StatusPill(
                        label = if (muted) "MUTED" else "MIC",
                        dotColor = if (muted) m.redBronze else m.cyanCore,
                        description = if (muted) "Privacy mute engaged, microphone disabled" else "Microphone path simulated until voice pack activation",
                    )
                }
                }
            }

            // ---- monumental oracle core (central area, state-driven) ----
            BoxWithConstraints(
                Modifier
                    .fillMaxWidth()
                    .burnInOffset(burnStep, BurnInRegion.CORE),
            ) {
                // The core stage keeps the exact design aspect (1080 x 1420) so the
                // WebP depth layers and the Canvas dynamics align pixel-perfectly.
                val coreH = maxWidth * (1420f / 1080f)
                OracleCore(
                    state = state,
                    reducedMotion = settings.reducedMotion,
                    quality = quality,
                    speakingAmplitude = amplitude,
                    queueCount = queueCount,
                    colors = m,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(coreH),
                )
                StateOverlayChip(
                    state,
                    effectiveMode,
                    requestedMode,
                    Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 6.dp),
                )
            }

            // ---- status note ----
            if (!statusNote.isNullOrEmpty()) {
                Text(
                    statusNote!!,
                    style = MonumentalType.bodySecondary,
                    color = m.inkSecondary,
                    modifier = Modifier
                        .padding(horizontal = 18.dp, vertical = 4.dp)
                        .semantics { liveRegion = LiveRegionMode.Polite },
                )
            }

            // ---- lower area: transcript panel (architectural panel) ----
            Column(
                Modifier
                    .padding(horizontal = 14.dp)
                    .fillMaxWidth()
                    .background(Brush.linearGradient(listOf(m.obsidianRaised, m.obsidianPanel)), RoundedCornerShape(10.dp))
                    .border(1.5.dp, m.bronzeEdge, RoundedCornerShape(10.dp))
                    .burnInOffset(burnStep, BurnInRegion.TRANSCRIPT)
                    .padding(12.dp),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("LOCAL CORE", style = MonumentalType.sealSub, color = m.localGold)
                    Text("TOWER", style = MonumentalType.sealSub, color = m.towerBlue)
                    Text(
                        "attribution is real — local answers never claim the Tower",
                        style = MonumentalType.sealSub,
                        color = m.inkDim,
                    )
                }
                turns.takeLast(3).forEach { turn ->
                    val who = when (turn.role) {
                        com.hermestower.voiceconsole.core.state.ConversationTurn.Role.OWNER -> "OWNER"
                        else -> if (turn.source == com.hermestower.voiceconsole.core.state.ConversationTurn.Source.TOWER) "HERMES TOWER" else "HERMES · LOCAL"
                    }
                    val whoColor = when {
                        turn.role == com.hermestower.voiceconsole.core.state.ConversationTurn.Role.OWNER -> m.gold
                        turn.source == com.hermestower.voiceconsole.core.state.ConversationTurn.Source.TOWER -> m.cyanCore
                        else -> m.localGold
                    }
                    Text(who, style = MonumentalType.sealLabel, color = whoColor)
                    Text(turn.text, style = MonumentalType.body, color = m.ink)
                    Spacer(Modifier.height(6.dp))
                }
                if (draft != null) {
                    Text("you: “${draft}”", style = MonumentalType.bodySecondary, color = m.inkSecondary)
                }
                if (turns.isEmpty() && draft == null) {
                    Text(
                        "No conversation yet. Wake the console or inject a line of text — everything shown is the simulator's real output.",
                        style = MonumentalType.bodySecondary,
                        color = m.inkSecondary,
                    )
                }
                TextButton(onClick = onOpenHistory) {
                    Text("Conversation archive →", style = MonumentalType.bodySecondary, color = m.inkSecondary)
                }
            }

            // ---- caption (live region) ----
            if (settings.captionsEnabled && caption != null) {
                Text(
                    caption!!,
                    style = MonumentalType.caption,
                    color = m.cyanCore,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                        .semantics { liveRegion = LiveRegionMode.Polite },
                )
            }

            // ---- control medallions ----
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp)
                    .burnInOffset(burnStep, BurnInRegion.MEDALLIONS),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = { graph.driver.setPrivacyMuted(!muted) }) {
                    MedallionDisc(
                        cd = if (muted) "Disable privacy mute" else "Engage privacy mute",
                        icon = if (muted) Icons.Filled.MicOff else Icons.Filled.Mic,
                        tint = if (muted) m.redBronze else m.bronzeLight,
                        m = m,
                    )
                }
                Spacer(Modifier.size(10.dp))
                IconButton(onClick = { graph.driver.injectWake() }) {
                    MedallionDisc(
                        cd = "Simulate wake word and canned utterance",
                        icon = Icons.Outlined.Send,
                        tint = m.gold,
                        m = m,
                        big = true,
                    )
                }
                Spacer(Modifier.size(10.dp))
                IconButton(onClick = { showDialog = true }) {
                    MedallionDisc(
                        cd = "Inject a line of text instead of speaking",
                        icon = Icons.Outlined.History,
                        tint = m.bronzeLight,
                        m = m,
                    )
                }
                Spacer(Modifier.size(10.dp))
                IconButton(onClick = { graph.driver.stopSpeaking() }) {
                    MedallionDisc(
                        cd = "Stop speaking",
                        icon = Icons.Filled.Stop,
                        tint = m.bronzeLight,
                        m = m,
                    )
                }
            }
            Text(
                "wake · inject text · stop voice — no microphone capture until a voice pack is activated",
                style = MonumentalType.sealSub,
                color = m.inkDim,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp),
                textAlign = TextAlign.Center,
            )
        }

        // brightness scrim (FR-41 preserved)
        if (settings.brightnessScrim > 0f) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = settings.brightnessScrim)),
            )
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Inject text (STT stand-in)") },
            text = {
                OutlinedTextField(
                    value = injectText,
                    onValueChange = { injectText = it },
                    placeholder = { Text("Type what you would say…") },
                    singleLine = true,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    graph.driver.submitText(injectText)
                    injectText = ""
                    showDialog = false
                }) { Text("Send") }
            },
            dismissButton = { TextButton(onClick = { showDialog = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun MedallionDisc(
    cd: String,
    icon: ImageVector,
    tint: Color,
    m: MonumentalColors,
    big: Boolean = false,
) {
    val size = if (big) 62.dp else 50.dp
    Box(
        Modifier
            .size(size)
            .background(
                Brush.radialGradient(listOf(m.obsidianRaised, m.obsidianPanel, Color(0xFF070A10))),
                CircleShape,
            )
            .border(
                width = if (big) 2.5.dp else 1.5.dp,
                color = tint,
                shape = CircleShape,
            )
            .semantics { contentDescription = cd },
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = tint)
    }
}

@Composable
private fun StateOverlayChip(
    state: ConsoleState,
    effectiveMode: OperatingMode,
    requestedMode: OperatingMode,
    modifier: Modifier = Modifier,
) {
    val m = monumentalColors
    val chipColor = when (state) {
        ConsoleState.REQUEST_QUEUED -> m.amber
        ConsoleState.ERROR -> m.redBronze
        ConsoleState.DISCONNECTED, ConsoleState.PRIVACY_MUTED -> m.inkSecondary
        else -> m.cyanCore
    }
    val modeNote = if (effectiveMode != requestedMode) " · effective ${effectiveMode.label}" else ""
    Column(
        modifier
            .background(m.obsidianPanel.copy(alpha = 0.92f), RoundedCornerShape(8.dp))
            .border(1.dp, m.bronzeEdge, RoundedCornerShape(8.dp))
            .padding(horizontal = 14.dp, vertical = 7.dp)
            .semantics { liveRegion = LiveRegionMode.Polite },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(state.label, style = MonumentalType.stateLabel, color = chipColor)
        Text(
            "mode ${effectiveMode.label}$modeNote",
            style = MonumentalType.sealSub,
            color = m.inkSecondary,
            modifier = Modifier.padding(top = 2.dp),
        )
    }
}
