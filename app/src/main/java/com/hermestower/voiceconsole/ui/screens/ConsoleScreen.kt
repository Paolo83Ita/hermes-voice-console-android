package com.hermestower.voiceconsole.ui.screens

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Send
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.hermestower.voiceconsole.R
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hermestower.voiceconsole.app.AppGraph
import com.hermestower.voiceconsole.app.MicUiState
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
import com.hermestower.voiceconsole.ui.monumental.TowerEnergyPhase
import com.hermestower.voiceconsole.ui.monumental.TowerArchitecture
import com.hermestower.voiceconsole.ui.monumental.drawTowerFrame
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

    // ---- GRAPH 4 G4-C03: real microphone path (Owner delta §5) ----
    val micState by graph.voiceCoordinator.micState.collectAsStateWithLifecycle()
    val lastRefusal by graph.voiceCoordinator.lastRefusal.collectAsStateWithLifecycle()

    // Foreground-only capture (§4): leaving the screen physically stops the mic.
    DisposableEffect(Unit) {
        onDispose { graph.voiceCoordinator.onConsolePaused() }
    }

    val context = LocalContext.current
    val micPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        val rationale = ActivityCompat.shouldShowRequestPermissionRationale(
            context as Activity,
            android.Manifest.permission.RECORD_AUDIO,
        )
        graph.voiceCoordinator.onMicPermissionResult(granted, rationale)
        if (granted) graph.voiceCoordinator.onPushToTalkPress()
    }

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
                        contentDescription = stringResource(com.hermestower.voiceconsole.R.string.nav_open_drawer),
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
                    stringResource(com.hermestower.voiceconsole.R.string.console_subtitle),
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
                            ConnectionStatus.BridgeStatus.CONNECTED -> stringResource(com.hermestower.voiceconsole.R.string.pill_connected)
                            ConnectionStatus.BridgeStatus.CONNECTING -> stringResource(com.hermestower.voiceconsole.R.string.pill_connecting)
                            ConnectionStatus.BridgeStatus.DISCONNECTED -> stringResource(com.hermestower.voiceconsole.R.string.pill_offline)
                            ConnectionStatus.BridgeStatus.UNCONFIGURED -> stringResource(com.hermestower.voiceconsole.R.string.pill_mock)
                        },
                        dotColor = when (connection.status) {
                            ConnectionStatus.BridgeStatus.CONNECTED -> m.cyanCore
                            ConnectionStatus.BridgeStatus.CONNECTING -> m.amber
                            else -> m.redBronze
                        },
                        description = stringResource(com.hermestower.voiceconsole.R.string.desc_bridge_fmt, connection.status.name.lowercase(), connection.endpointLabel),
                    )
                    if (queueCount > 0) {
                        StatusPill(
                            label = stringResource(com.hermestower.voiceconsole.R.string.pill_queued_fmt, queueCount),
                            dotColor = m.amber,
                            description = stringResource(com.hermestower.voiceconsole.R.string.desc_queued_fmt, queueCount),
                        )
                    }
                    StatusPill(
                        label = if (muted) stringResource(com.hermestower.voiceconsole.R.string.pill_muted) else if (micState == MicUiState.LISTENING) stringResource(com.hermestower.voiceconsole.R.string.pill_mic_live) else stringResource(com.hermestower.voiceconsole.R.string.pill_mic),
                        dotColor = when {
                            muted -> m.redBronze
                            micState == MicUiState.LISTENING -> m.cyanCore
                            else -> m.bronzeLight
                        },
                        description = when {
                            muted -> stringResource(com.hermestower.voiceconsole.R.string.desc_muted)
                            micState == MicUiState.LISTENING -> stringResource(com.hermestower.voiceconsole.R.string.desc_mic_live)
                            else -> stringResource(com.hermestower.voiceconsole.R.string.desc_mic_ready)
                        },
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
                // GRAPH 4 directive §7: when the composer is focused (keyboard
                // visible) the Tower smoothly shrinks to a compact portal and the
                // timeline/input area gains the space — chat-focused layout.
                val imeOpen = WindowInsets.ime.getBottom(LocalDensity.current) > 0
                val coreScale by animateFloatAsState(
                    targetValue = if (imeOpen) 0.52f else 1f,
                    label = "towerChatFocus",
                )
                val coreH = maxWidth * (1420f / 1080f) * coreScale
                OracleCore(
                    state = state,
                    // CP-G4-016: CI_LIFECYCLE_PROFILE flows through the same
                    // static-frame gate as reduced motion (TEST_ONLY debug seam).
                    reducedMotion = settings.reducedMotion ||
                        com.hermestower.voiceconsole.ui.runtime.MotionProfile.ornamentsFrozen,
                    quality = quality,
                    speakingAmplitude = amplitude,
                    queueCount = queueCount,
                    colors = m,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(coreH),
                )
                // GRAPH 4 directive §7: the original architectural frame (upper
                // order, lower structure, state-driven messenger path) drawn over
                // the core stage — foreground built order, no bitmap memory.
                val towerPhase = TowerEnergyPhase(
                    flowing = TowerArchitecture.stateDrivesFlow(state),
                    frozen = settings.reducedMotion ||
                        com.hermestower.voiceconsole.ui.runtime.MotionProfile.ornamentsFrozen,
                )
                Canvas(Modifier.matchParentSize()) {
                    drawTowerFrame(towerPhase, com.hermestower.voiceconsole.ui.monumental.QualityBudgets.of(quality), m)
                }
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
            // Typed refusal from the voice coordinator (mute/permission/unavailable).
            // G4-VISUAL D8 repair: this Text was rendered TWICE (two identical
            // blocks — MONUMENTAL_TOWER_VISUAL_GAP_GRAPH4_FINAL.md defect D8); a
            // typed refusal appeared doubled on screen and in the semantics tree.
            if (!lastRefusal.isNullOrEmpty()) {
                Text(
                    lastRefusal!!,
                    style = MonumentalType.bodySecondary,
                    color = m.redBronze,
                    modifier = Modifier
                        .padding(horizontal = 18.dp, vertical = 2.dp)
                        .semantics { liveRegion = LiveRegionMode.Assertive },
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
                    Text(stringResource(com.hermestower.voiceconsole.R.string.local_core_label), style = MonumentalType.sealSub, color = m.localGold)
                    Text(stringResource(com.hermestower.voiceconsole.R.string.tower_label), style = MonumentalType.sealSub, color = m.towerBlue)
                    Text(
                        stringResource(com.hermestower.voiceconsole.R.string.attribution_note),
                        style = MonumentalType.sealSub,
                        color = m.inkDim,
                    )
                }
                turns.takeLast(3).forEach { turn ->
                    val who = when (turn.role) {
                        com.hermestower.voiceconsole.core.state.ConversationTurn.Role.OWNER ->
                            stringResource(com.hermestower.voiceconsole.R.string.turn_role_owner)
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
                // capture the delegated value once: smart cast is impossible on a
                // delegated property, and the format arg must be non-null Any.
                val draftedText = draft
                if (draftedText != null) {
                    Text(
                        stringResource(com.hermestower.voiceconsole.R.string.console_you_draft_fmt, draftedText),
                        style = MonumentalType.bodySecondary,
                        color = m.inkSecondary,
                    )
                }
                if (micState == MicUiState.LISTENING) {
                    Text(
                        stringResource(com.hermestower.voiceconsole.R.string.listening_hint),
                        style = MonumentalType.bodySecondary,
                        color = m.cyanCore,
                        modifier = Modifier
                            .padding(top = 4.dp)
                            .semantics { liveRegion = LiveRegionMode.Polite },
                    )
                }
                if (turns.isEmpty() && draft == null) {
                    Text(
                        stringResource(com.hermestower.voiceconsole.R.string.no_conversation),
                        style = MonumentalType.bodySecondary,
                        color = m.inkSecondary,
                    )
                }
                TextButton(onClick = onOpenHistory) {
                    Text(stringResource(com.hermestower.voiceconsole.R.string.archive_link), style = MonumentalType.bodySecondary, color = m.inkSecondary)
                }
            }

            // ---- first-class text chat composer (GRAPH 4 Owner delta §12) ----
            val composerCd = stringResource(com.hermestower.voiceconsole.R.string.composer_cd)
            Column(
                Modifier
                    .padding(horizontal = 14.dp, vertical = 4.dp)
                    .fillMaxWidth()
                    .background(Brush.linearGradient(listOf(m.obsidianPanel, m.obsidianRaised)), RoundedCornerShape(10.dp))
                    .border(1.5.dp, m.bronzeEdge, RoundedCornerShape(10.dp))
                    .padding(8.dp),
            ) {
                OutlinedTextField(
                    value = draft.orEmpty(),
                    onValueChange = { graph.store.setTranscriptDraft(it.take(MAX_CHAT_CHARS)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics { contentDescription = composerCd },
                    placeholder = { Text(stringResource(com.hermestower.voiceconsole.R.string.composer_hint), style = MonumentalType.bodySecondary, color = m.inkDim) },
                    minLines = 1,
                    maxLines = 4,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    textStyle = MonumentalType.body.copy(color = m.ink),
                )
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (draft.orEmpty().length > CHAT_COUNTER_THRESHOLD) {
                        Text(
                            "${draft.orEmpty().length}/${MAX_CHAT_CHARS}",
                            style = MonumentalType.sealSub,
                            color = m.inkDim,
                        )
                    }
                    TextButton(onClick = {
                        graph.voiceCoordinator.stopCapture("composer mic")
                        graph.voiceCoordinator.onPushToTalkPressWithPermission { micPermissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO) }
                    }) {
                        Text(stringResource(com.hermestower.voiceconsole.R.string.composer_speak_instead), style = MonumentalType.bodySecondary, color = m.inkSecondary)
                    }
                    TextButton(onClick = {
                        graph.driver.cancelCurrentTurn()
                    }) {
                        Text(stringResource(com.hermestower.voiceconsole.R.string.composer_stop), style = MonumentalType.bodySecondary, color = m.inkSecondary)
                    }
                    TextButton(
                        onClick = {
                            val text = draft.orEmpty().trim()
                            if (text.isNotEmpty()) {
                                graph.driver.submitText(text) // sending clears the draft (§15)
                            }
                        },
                        enabled = draft.orEmpty().isNotBlank(),
                    ) {
                        Text(stringResource(com.hermestower.voiceconsole.R.string.composer_send), color = m.cyanCore)
                    }
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

            // ---- control medallions (OWNER PHYSICAL RETEST §9: distinct semantics) ----
            // Four semantically distinct controls: privacy mute = MicOff (+ red-bronze
            // seal + persistent “Microfono disattivato” text when engaged); push-to-talk
            // = Mic (Parla) / Stop (Interrompi ascolto); TTS stop = VolumeUp (Interrompi
            // voce — never a microphone icon). Every cd is unique per state (§10).
            val muteSemantics = ConsoleControlSemanticsResolver.privacyMute(muted)
            val pttSemantics = ConsoleControlSemanticsResolver.pushToTalk(micState)
            val stopVoiceSemantics = ConsoleControlSemanticsResolver.stopSpeaking()
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
                        cd = stringResource(muteSemantics.contentDescriptionRes),
                        icon = medallionIcon(muteSemantics.icon),
                        tint = if (muted) m.redBronze else m.bronzeLight,
                        m = m,
                        sealed = muted,
                        sealColor = m.redBronze,
                    )
                }
                Spacer(Modifier.size(8.dp))
                // ---- central push-to-talk medallion (G4-C03 §5) ----
                IconButton(onClick = {
                    when (micState) {
                        MicUiState.PERMISSION_REQUIRED, MicUiState.DENIED ->
                            micPermissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
                        MicUiState.PERMANENTLY_DENIED -> graph.driver.setPrivacyMuted(false)
                        else -> {
                            if (micState == MicUiState.LISTENING) graph.voiceCoordinator.stopCapture("push-to-talk released")
                            else graph.voiceCoordinator.onPushToTalkPress()
                        }
                    }
                }) {
                    MedallionDisc(
                        cd = stringResource(pttSemantics.contentDescriptionRes),
                        icon = medallionIcon(pttSemantics.icon),
                        tint = if (micState == MicUiState.LISTENING) m.cyanCore else m.gold,
                        m = m,
                        big = true,
                    )
                }
                Spacer(Modifier.size(8.dp))
                IconButton(onClick = { graph.driver.repeatLastSpoken() }) {
                    MedallionDisc(
                        cd = stringResource(com.hermestower.voiceconsole.R.string.repeat_cd),
                        icon = Icons.Filled.Replay,
                        tint = m.bronzeLight,
                        m = m,
                    )
                }
                Spacer(Modifier.size(8.dp))
                IconButton(onClick = {
                    graph.voiceCoordinator.stopCapture("stop pressed")
                    graph.driver.stopSpeaking()
                }) {
                    MedallionDisc(
                        cd = stringResource(stopVoiceSemantics.contentDescriptionRes),
                        icon = medallionIcon(stopVoiceSemantics.icon),
                        tint = m.cyanCore,
                        m = m,
                    )
                }
                Spacer(Modifier.size(8.dp))
                IconButton(onClick = { graph.driver.injectWake() }) {
                    MedallionDisc(
                        cd = stringResource(com.hermestower.voiceconsole.R.string.sim_wake_cd),
                        icon = Icons.Outlined.Send,
                        tint = m.gold,
                        m = m,
                    )
                }
                Spacer(Modifier.size(8.dp))
                IconButton(onClick = { showDialog = true }) {
                    MedallionDisc(
                        cd = stringResource(com.hermestower.voiceconsole.R.string.inject_cd),
                        icon = Icons.Outlined.History,
                        tint = m.bronzeLight,
                        m = m,
                    )
                }
            }
            // ---- OWNER PHYSICAL RETEST §9: the persistent privacy-mute text + the
            // live control label row (Parla / Interrompi ascolto / Interrompi voce). ----
            if (muted) {
                Text(
                    stringResource(com.hermestower.voiceconsole.R.string.mic_label_muted),
                    style = MonumentalType.sealSub,
                    color = m.redBronze,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 2.dp)
                        .semantics { liveRegion = LiveRegionMode.Polite },
                )
            } else {
                Text(
                    stringResource(pttSemantics.labelRes),
                    style = MonumentalType.sealSub,
                    color = if (micState == MicUiState.LISTENING) m.cyanCore else m.inkDim,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 2.dp),
                )
            }
            // ---- engine truth row (§5: engines are named, simulation labeled) ----
            Text(
                stringResource(com.hermestower.voiceconsole.R.string.engine_row),
                style = MonumentalType.sealSub,
                color = m.inkDim,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
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
            title = { Text(stringResource(R.string.cd_inject_title)) },
            text = {
                OutlinedTextField(
                    value = injectText,
                    onValueChange = { injectText = it },
                    placeholder = { Text(stringResource(R.string.cd_inject_placeholder)) },
                    singleLine = true,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    graph.driver.submitText(injectText)
                    injectText = ""
                    showDialog = false
                }) { Text(stringResource(R.string.cd_send)) }
            },
            dismissButton = { TextButton(onClick = { showDialog = false }) { Text(stringResource(R.string.cd_cancel)) } },
        )
    }
}

private const val MAX_CHAT_CHARS = 2000
private const val CHAT_COUNTER_THRESHOLD = 1600

@Composable
private fun MedallionDisc(
    cd: String,
    icon: ImageVector,
    tint: Color,
    m: MonumentalColors,
    big: Boolean = false,
    /** OWNER PHYSICAL RETEST §9: the red-bronze PRIVACY SEAL (muted state). */
    sealed: Boolean = false,
    sealColor: Color = Color.Red,
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
                width = if (sealed) 3.dp else if (big) 2.5.dp else 1.5.dp,
                color = if (sealed) sealColor else tint,
                shape = CircleShape,
            )
            .semantics { contentDescription = cd },
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = tint)
    }
}

/**
 * OWNER PHYSICAL RETEST §9: the typed icon vocabulary → the material vector.
 * The privacy-mute control is ALWAYS MicOff (its identity), push-to-talk is
 * Mic/Stop, the TTS stop is VolumeUp — the collision class is dead.
 */
private fun medallionIcon(icon: ConsoleControlIcon): ImageVector = when (icon) {
    ConsoleControlIcon.MIC -> Icons.Filled.Mic
    ConsoleControlIcon.STOP -> Icons.Filled.Stop
    ConsoleControlIcon.MIC_OFF -> Icons.Filled.MicOff
    ConsoleControlIcon.VOLUME_UP -> Icons.Filled.VolumeUp
    ConsoleControlIcon.REPLAY -> Icons.Filled.Replay
    ConsoleControlIcon.SEND -> Icons.Outlined.Send
    ConsoleControlIcon.HISTORY -> Icons.Outlined.History
}


private fun stateLabelRes(state: ConsoleState): Int = when (state) {
    ConsoleState.DORMANT -> com.hermestower.voiceconsole.R.string.state_DORMANT
    ConsoleState.IDLE -> com.hermestower.voiceconsole.R.string.state_IDLE
    ConsoleState.WAKE_DETECTED -> com.hermestower.voiceconsole.R.string.state_WAKE_DETECTED
    ConsoleState.LISTENING -> com.hermestower.voiceconsole.R.string.state_LISTENING
    ConsoleState.TRANSCRIBING -> com.hermestower.voiceconsole.R.string.state_TRANSCRIBING
    ConsoleState.UNDERSTANDING_LOCAL -> com.hermestower.voiceconsole.R.string.state_UNDERSTANDING_LOCAL
    ConsoleState.CONTACTING_TOWER -> com.hermestower.voiceconsole.R.string.state_CONTACTING_TOWER
    ConsoleState.TOWER_PROCESSING -> com.hermestower.voiceconsole.R.string.state_TOWER_PROCESSING
    ConsoleState.SPEAKING -> com.hermestower.voiceconsole.R.string.state_SPEAKING
    ConsoleState.REQUEST_QUEUED -> com.hermestower.voiceconsole.R.string.state_REQUEST_QUEUED
    ConsoleState.DISCONNECTED -> com.hermestower.voiceconsole.R.string.state_DISCONNECTED
    ConsoleState.DEGRADED -> com.hermestower.voiceconsole.R.string.state_DEGRADED
    ConsoleState.ERROR -> com.hermestower.voiceconsole.R.string.state_ERROR
    ConsoleState.PRIVACY_MUTED -> com.hermestower.voiceconsole.R.string.state_PRIVACY_MUTED
    ConsoleState.UPDATE_AVAILABLE -> com.hermestower.voiceconsole.R.string.state_UPDATE_AVAILABLE
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
    val modeNote = if (effectiveMode != requestedMode) stringResource(com.hermestower.voiceconsole.R.string.effective_note_fmt, effectiveMode.label) else ""
    Column(
        modifier
            .background(m.obsidianPanel.copy(alpha = 0.92f), RoundedCornerShape(8.dp))
            .border(1.dp, m.bronzeEdge, RoundedCornerShape(8.dp))
            .padding(horizontal = 14.dp, vertical = 7.dp)
            .semantics { liveRegion = LiveRegionMode.Polite },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(stringResource(stateLabelRes(state)), style = MonumentalType.stateLabel, color = chipColor)
        Text(
            stringResource(com.hermestower.voiceconsole.R.string.mode_note_fmt, effectiveMode.label, modeNote),
            style = MonumentalType.sealSub,
            color = m.inkSecondary,
            modifier = Modifier.padding(top = 2.dp),
        )
    }
}
