package com.hermestower.voiceconsole.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Send
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
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
import com.hermestower.voiceconsole.ui.components.PedimentLine
import com.hermestower.voiceconsole.ui.components.StatusPill
import com.hermestower.voiceconsole.ui.components.TowerCore
import com.hermestower.voiceconsole.ui.theme.HermesType
import com.hermestower.voiceconsole.ui.theme.hermesColors
import kotlinx.coroutines.delay

/**
 * S1 Main Voice Console (FR-30): central animated Tower, truthful state,
 * transcript area, captions, mic control, privacy mute, connection indicator,
 * mode, compact status. Burn-in guard drifts content subtly (FR-40).
 */
@Composable
fun ConsoleScreen(
    graph: AppGraph,
    onOpenHistory: () -> Unit,
) {
    val c = hermesColors
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

    // Burn-in guard: bounded slow drift of the whole console content (FR-40, D-015).
    val drift = remember { androidx.compose.runtime.mutableIntStateOf(0) }
    LaunchedEffect(settings.burnInGuard, settings.reducedMotion) {
        var step = 0
        while (true) {
            delay(20_000L)
            if (settings.burnInGuard && !settings.reducedMotion) {
                step = (step + 1) % 4
                drift.intValue = step
            }
        }
    }
    val driftDp = when (drift.intValue) {
        1 -> 4.dp; 2 -> (-4).dp; 3 -> (-4).dp; else -> 0.dp
    }
    val driftY = when (drift.intValue) { 3 -> 4.dp; else -> 0.dp }

    Box(
        Modifier
            .fillMaxSize()
            .background(c.background),
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .graphicsLayer {
                    translationX = driftDp.toPx()
                    translationY = driftY.toPx()
                }
                .verticalScroll(rememberScrollState())
                .padding(bottom = 16.dp),
        ) {
            // ---- header ----
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                PedimentLine(colors = c, modifier = Modifier.padding(top = 8.dp, start = 48.dp, end = 48.dp))
                Text(
                    "HERMES",
                    style = HermesType.brand,
                    color = c.ink,
                    modifier = Modifier.padding(top = 4.dp),
                )
                MeanderBand(colors = c, modifier = Modifier.padding(top = 6.dp))
            }

            // ---- status strip ----
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OperatingMode.selectable.forEach { mode ->
                    ModeChip(
                        label = mode.label,
                        selected = requestedMode == mode,
                        enabled = true,
                        onClick = { graph.driver.setRequestedMode(mode) },
                    )
                }
                Spacer(Modifier.weight(1f))
                StatusPill(
                    label = when (connection.status) {
                        ConnectionStatus.BridgeStatus.CONNECTED -> "BRIDGE OK"
                        ConnectionStatus.BridgeStatus.CONNECTING -> "BRIDGE…"
                        ConnectionStatus.BridgeStatus.DISCONNECTED -> "OFFLINE"
                        ConnectionStatus.BridgeStatus.UNCONFIGURED -> "MOCK"
                    },
                    dotColor = when (connection.status) {
                        ConnectionStatus.BridgeStatus.CONNECTED -> c.ok
                        ConnectionStatus.BridgeStatus.CONNECTING -> c.warn
                        else -> c.danger
                    },
                    description = "Tower bridge: ${connection.status.name.lowercase()}, endpoint ${connection.endpointLabel}",
                )
                if (queueCount > 0) {
                    StatusPill(label = "QUEUED ×$queueCount", dotColor = c.warn, description = "$queueCount requests queued for the Tower")
                }
                StatusPill(
                    label = if (muted) "MUTED" else "MIC",
                    dotColor = if (muted) c.danger else c.ok,
                    description = if (muted) "Privacy mute engaged, microphone disabled" else "Microphone path simulated in V0.1",
                )
            }

            // ---- tower stage ----
            Box(
                Modifier
                    .padding(horizontal = 14.dp)
                    .fillMaxWidth()
                    .height(310.dp)
                    .border(1.dp, c.outline, MaterialTheme.shapes.large)
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                        MaterialTheme.shapes.large,
                    ),
            ) {
                TowerCore(
                    state = state,
                    reducedMotion = settings.reducedMotion,
                    animationIntensity = settings.animationIntensity,
                    speakingAmplitude = amplitude,
                    colors = c,
                    modifier = Modifier.fillMaxSize(),
                )
                StateOverlayChip(state, effectiveMode, requestedMode, Modifier.align(Alignment.BottomCenter))
            }

            // ---- status note ----
            if (!statusNote.isNullOrEmpty()) {
                Text(
                    statusNote!!,
                    style = HermesType.bodySecondary,
                    color = c.inkSecondary,
                    modifier = Modifier
                        .padding(horizontal = 18.dp, vertical = 4.dp)
                        .semantics { liveRegion = LiveRegionMode.Polite },
                )
            }

            // ---- transcript area ----
            Column(
                Modifier
                    .padding(horizontal = 14.dp)
                    .fillMaxWidth()
                    .border(1.dp, c.outline, MaterialTheme.shapes.large)
                    .background(MaterialTheme.colorScheme.surface, MaterialTheme.shapes.large)
                    .padding(12.dp),
            ) {
                turns.takeLast(3).forEach { turn ->
                    val who = when (turn.role) {
                        com.hermestower.voiceconsole.core.state.ConversationTurn.Role.OWNER -> "OWNER"
                        else -> if (turn.source == com.hermestower.voiceconsole.core.state.ConversationTurn.Source.TOWER) "HERMES TOWER" else "HERMES · LOCAL"
                    }
                    val whoColor = when {
                        turn.role == com.hermestower.voiceconsole.core.state.ConversationTurn.Role.OWNER -> c.bronze
                        turn.source == com.hermestower.voiceconsole.core.state.ConversationTurn.Source.TOWER -> c.energy
                        else -> c.ok
                    }
                    Text(who, style = HermesType.pill, color = whoColor)
                    Text(turn.text, style = HermesType.body, color = c.ink)
                    Spacer(Modifier.height(6.dp))
                }
                if (draft != null) {
                    Text("you: “${draft}”", style = HermesType.bodySecondary, color = c.inkSecondary)
                }
                if (turns.isEmpty() && draft == null) {
                    Text(
                        "No conversation yet. Wake the console or inject a line of text — everything shown is the simulator's real output.",
                        style = HermesType.bodySecondary,
                        color = c.inkSecondary,
                    )
                }
                TextButton(onClick = onOpenHistory) { Text("Conversation history →", style = HermesType.bodySecondary) }
            }

            // ---- caption (live region) ----
            if (settings.captionsEnabled && caption != null) {
                Text(
                    caption!!,
                    style = HermesType.caption,
                    color = c.energy,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                        .semantics { liveRegion = LiveRegionMode.Polite },
                )
            }

            // ---- controls ----
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(
                    onClick = { graph.driver.setPrivacyMuted(!muted) },
                    modifier = Modifier.size(58.dp),
                ) {
                    Box(
                        Modifier
                            .size(50.dp)
                            .border(
                                width = if (muted) 2.dp else 1.dp,
                                color = if (muted) c.danger else c.outline,
                                shape = CircleShape,
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = if (muted) Icons.Filled.MicOff else Icons.Filled.Mic,
                            contentDescription = if (muted) "Disable privacy mute" else "Engage privacy mute",
                            tint = if (muted) c.danger else c.bronze,
                        )
                    }
                }
                Spacer(Modifier.size(14.dp))
                IconButton(
                    onClick = { graph.driver.injectWake() },
                    modifier = Modifier.size(66.dp),
                ) {
                    Box(
                        Modifier
                            .size(60.dp)
                            .border(2.dp, c.bronze, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Outlined.Send,
                            contentDescription = "Simulate wake word and canned utterance",
                            tint = c.gold,
                        )
                    }
                }
                Spacer(Modifier.size(14.dp))
                IconButton(
                    onClick = { showDialog = true },
                    modifier = Modifier.size(58.dp),
                ) {
                    Box(
                        Modifier
                            .size(50.dp)
                            .border(1.dp, c.outline, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Outlined.History,
                            contentDescription = "Inject a line of text instead of speaking",
                            tint = c.bronze,
                        )
                    }
                }
                Spacer(Modifier.size(14.dp))
                IconButton(
                    onClick = { graph.driver.stopSpeaking() },
                    modifier = Modifier.size(58.dp),
                ) {
                    Box(
                        Modifier
                            .size(50.dp)
                            .border(1.dp, c.outline, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Filled.Stop, contentDescription = "Stop speaking", tint = c.bronze)
                    }
                }
            }
            Text(
                "wake · inject text · stop voice — no microphone is used in V0.1",
                style = HermesType.pill,
                color = c.inkSecondary,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp),
                textAlign = TextAlign.Center,
            )
        }

        // brightness scrim (FR-41 prototype)
        if (settings.brightnessScrim > 0f) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color_Black.copy(alpha = settings.brightnessScrim)),
            )
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Inject text (V0.1 STT stand-in)") },
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
private fun StateOverlayChip(
    state: ConsoleState,
    effectiveMode: OperatingMode,
    requestedMode: OperatingMode,
    modifier: Modifier = Modifier,
) {
    val c = hermesColors
    val chipColor = when (state) {
        ConsoleState.REQUEST_QUEUED -> c.warn
        ConsoleState.ERROR -> c.danger
        ConsoleState.DISCONNECTED, ConsoleState.PRIVACY_MUTED -> c.inkSecondary
        else -> c.energy
    }
    val modeNote = if (effectiveMode != requestedMode) " · effective ${effectiveMode.label}" else ""
    Box(
        modifier
            .background(MaterialTheme.colorScheme.surface, MaterialTheme.shapes.extraLarge)
            .border(1.dp, c.outline, MaterialTheme.shapes.extraLarge)
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .semantics { liveRegion = LiveRegionMode.Polite },
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(state.label, style = HermesType.stateChip, color = chipColor)
            Text(
                "mode ${effectiveMode.label}$modeNote",
                style = HermesType.pill,
                color = c.inkSecondary,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}

private val Color_Black = androidx.compose.ui.graphics.Color(0xFF000000)
