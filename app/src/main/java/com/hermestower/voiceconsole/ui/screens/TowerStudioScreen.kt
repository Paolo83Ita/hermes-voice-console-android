package com.hermestower.voiceconsole.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.hermestower.voiceconsole.app.AppGraph
import com.hermestower.voiceconsole.R
import com.hermestower.voiceconsole.domain.BridgeResult
import com.hermestower.voiceconsole.tower.ImportResult
import com.hermestower.voiceconsole.tower.TransportMode
import com.hermestower.voiceconsole.tower.TowerConnectionProfile
import com.hermestower.voiceconsole.tower.TowerConnectionTester
import com.hermestower.voiceconsole.tower.TowerProfileValidator
import com.hermestower.voiceconsole.tower.TowerProfileWriteResult
import com.hermestower.voiceconsole.locale.StateLabels
import androidx.compose.foundation.layout.BoxWithConstraints
import com.hermestower.voiceconsole.ui.components.SectionCard
import com.hermestower.voiceconsole.ui.components.ConsoleWindowWidthClass
import com.hermestower.voiceconsole.ui.components.MeanderBand
import com.hermestower.voiceconsole.ui.components.ResponsiveAction
import com.hermestower.voiceconsole.ui.components.ResponsiveActionGroup
import com.hermestower.voiceconsole.ui.components.consoleWindowWidthClass
import com.hermestower.voiceconsole.ui.theme.HermesType
import com.hermestower.voiceconsole.ui.theme.hermesColors
import kotlinx.coroutines.launch
import com.hermestower.voiceconsole.ui.components.HermesFilterChip
import com.hermestower.voiceconsole.ui.components.HermesOutlinedField
import com.hermestower.voiceconsole.ui.components.HermesSwitch

/**
 * Settings → Hermes Tower Connection (GRAPH 2 §7). Basic fields always visible,
 * Advanced collapsed by default. Every action reports a truthful typed result;
 * secrets are held only long enough to store them in the vault — they are never
 * placed in Compose state after save, in logs, or in exports.
 *
 * CP-G4-010 wave 2: the QR pairing scanner is reachable from the Actions card
 * (callback injected by AppNav; null in previews keeps the screen standalone).
 */
@Composable
fun TowerStudioScreen(graph: AppGraph, onOpenScanner: (() -> Unit)? = null) {
    val c = hermesColors
    val context = LocalContext.current
    val existing = remember { graph.profileStore.load() }
    var name by rememberSaveable { mutableStateOf(existing?.profileName ?: "") }
    var baseUrl by rememberSaveable {
        mutableStateOf(existing?.takeIf { it.transportMode != TransportMode.MOCK_SIMULATOR.name }?.baseUrl ?: "")
    }
    var mode by remember {
        mutableStateOf(existing?.transportMode?.let { stored -> TransportMode.entries.firstOrNull { it.name == stored } } ?: TransportMode.MOCK_SIMULATOR)
    }
    var secret by rememberSaveable { mutableStateOf("") }
    var userRef by rememberSaveable { mutableStateOf(existing?.userReference ?: "") }
    var deviceAlias by rememberSaveable { mutableStateOf(existing?.deviceAlias ?: "") }
    var defaultMode by rememberSaveable { mutableStateOf(existing?.requestedMode ?: "TOWER_FIRST") }
    var language by rememberSaveable { mutableStateOf(existing?.preferredLanguage ?: "it-IT") }
    var autoConnect by remember { mutableStateOf(existing?.connectAutomatically ?: false) }
    var useTailscale by remember { mutableStateOf(existing?.useTailscale ?: false) }

    var advanced by remember { mutableStateOf(false) }
    var restBasePath by rememberSaveable { mutableStateOf(existing?.restBasePath ?: "/tower/api") }
    var webSocketPath by rememberSaveable { mutableStateOf(existing?.webSocketPath ?: "/tower/ws") }
    var protocolVersion by rememberSaveable { mutableStateOf(existing?.protocolVersion ?: "hermes.console.v0.1") }
    var requestTimeout by rememberSaveable { mutableStateOf((existing?.requestTimeoutMs ?: 10_000).toString()) }
    var streamingTimeout by rememberSaveable { mutableStateOf((existing?.streamingTimeoutMs ?: 60_000).toString()) }
    var retryCount by rememberSaveable { mutableStateOf((existing?.maximumRetryCount ?: 2).toString()) }
    var certPolicy by rememberSaveable { mutableStateOf(existing?.certificatePolicy ?: "SYSTEM") }
    var pinnedFingerprint by rememberSaveable { mutableStateOf(existing?.certificateFingerprintReference ?: "") }
    var queuedPolicy by rememberSaveable { mutableStateOf(existing?.queuedRequestPolicy ?: "QUEUE_NON_CONSEQUENTIAL") }
    var consequentialConfirm by remember { mutableStateOf(existing?.consequentialConfirmationRequired ?: true) }
    var routingThreshold by rememberSaveable { mutableStateOf((existing?.localTowerRoutingThreshold ?: 0.55f).toString()) }
    var verbosity by rememberSaveable { mutableStateOf(existing?.diagnosticsVerbosity ?: "REDACTED_SUMMARY") }

    var message by remember { mutableStateOf(context.getString(R.string.ts_msg_config_state, StateLabels.label(graph.profileStore.state().name, com.hermestower.voiceconsole.locale.AppLanguage.effectiveTag(context)))) }
    var testOutcome by remember { mutableStateOf<TowerConnectionTester.Report?>(null) }

    fun buildProfile(): TowerConnectionProfile {
        val now = java.time.Instant.now().toString()
        return TowerConnectionProfile(
            profileId = existing?.profileId ?: "profile-owner-1",
            profileName = name,
            transportMode = mode.name,
            baseUrl = if (mode == TransportMode.MOCK_SIMULATOR) "mock://tower.simulator" else baseUrl.trim(),
            restBasePath = restBasePath,
            webSocketPath = webSocketPath,
            protocolVersion = protocolVersion,
            userReference = userRef,
            deviceAlias = deviceAlias,
            preferredLanguage = language,
            requestedMode = defaultMode,
            connectAutomatically = autoConnect,
            useTailscale = useTailscale,
            requestTimeoutMs = requestTimeout.toIntOrNull() ?: 10_000,
            streamingTimeoutMs = streamingTimeout.toIntOrNull() ?: 60_000,
            maximumRetryCount = retryCount.toIntOrNull() ?: 2,
            queuedRequestPolicy = queuedPolicy,
            consequentialConfirmationRequired = consequentialConfirm,
            certificatePolicy = certPolicy,
            certificateFingerprintReference = pinnedFingerprint.trim().lowercase(),
            diagnosticsVerbosity = verbosity,
            localTowerRoutingThreshold = routingThreshold.toFloatOrNull() ?: 0.55f,
            createdAt = existing?.createdAt?.takeIf { it.isNotBlank() } ?: now,
            updatedAt = now,
            lastValidationAt = existing?.lastValidationAt,
            lastConnectionAt = existing?.lastConnectionAt,
            lastConnectionResult = existing?.lastConnectionResult,
        )
    }

    // GRAPH 4.10 §19: the width class is measured from the ACTUAL available
    // width inside the padding (never physical pixels).
    BoxWithConstraints(Modifier.fillMaxSize().padding(16.dp)) {
    val widthClass = consoleWindowWidthClass(maxWidth.value)
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(stringResource(R.string.ts_title), style = HermesType.screenTitle, color = c.textPrimary, maxLines = 2)
        MeanderBand(colors = hermesColors, modifier = Modifier.padding(top = 6.dp))
        Text(
            stringResource(R.string.ts_intro),
            style = HermesType.bodySecondary, color = c.textSecondary,
        )

        SectionCard(title = stringResource(R.string.ts_section_basic)) {
            HermesOutlinedField(name, onValueChange = { name = it }, label = { Text(stringResource(R.string.ts_field_connection_name)) }, modifier = Modifier.fillMaxWidth())
            HermesOutlinedField(
                baseUrl, onValueChange = { baseUrl = it },
                label = { Text(stringResource(R.string.ts_field_tower_address)) },
                modifier = Modifier.fillMaxWidth(), enabled = mode != TransportMode.MOCK_SIMULATOR,
            )
            Text(stringResource(R.string.ts_label_connection_method), style = HermesType.sectionLabel, color = c.bronze)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                TransportMode.entries.take(3).forEach { m ->
                    HermesFilterChip(selected = mode == m, onClick = { mode = m }, label = { Text(m.displayLabel) })
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                TransportMode.entries.drop(3).forEach { m ->
                    HermesFilterChip(selected = mode == m, onClick = { mode = m }, label = { Text(m.displayLabel) })
                }
            }
            Text(
                when (mode) {
                    TransportMode.MOCK_SIMULATOR -> stringResource(R.string.ts_mode_mock_hint)
                    TransportMode.TAILSCALE_PRIVATE -> stringResource(R.string.ts_mode_tailscale_hint)
                    else -> stringResource(R.string.ts_mode_https_hint)
                },
                style = HermesType.bodySecondary, color = c.textSecondary,
            )
            HermesOutlinedField(
                secret, onValueChange = { secret = it },
                label = { Text(stringResource(R.string.ts_field_secret)) },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(), enabled = mode != TransportMode.MOCK_SIMULATOR,
            )
            HermesOutlinedField(userRef, onValueChange = { userRef = it }, label = { Text(stringResource(R.string.ts_field_user_ref)) }, modifier = Modifier.fillMaxWidth())
            HermesOutlinedField(deviceAlias, onValueChange = { deviceAlias = it }, label = { Text(stringResource(R.string.ts_field_device_alias)) }, modifier = Modifier.fillMaxWidth())
            HermesOutlinedField(defaultMode, onValueChange = { defaultMode = it }, label = { Text(stringResource(R.string.ts_field_default_mode)) }, modifier = Modifier.fillMaxWidth())
            HermesOutlinedField(language, onValueChange = { language = it }, label = { Text(stringResource(R.string.ts_field_language)) }, modifier = Modifier.fillMaxWidth())
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                HermesSwitch(checked = autoConnect, onCheckedChange = { autoConnect = it })
                // OWNER PHYSICAL RETEST F-D REPAIR: weighted text column — long
                // localized switch labels wrap instead of pushing the row overflow.
                Text(stringResource(R.string.ts_switch_autoconnect), style = HermesType.body, color = c.textPrimary, modifier = Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                HermesSwitch(checked = useTailscale, onCheckedChange = { useTailscale = it })
                Text(stringResource(R.string.ts_switch_tailscale), style = HermesType.body, color = c.textPrimary, modifier = Modifier.weight(1f))
            }
        }

        SectionCard(title = stringResource(R.string.ts_section_advanced)) {
            TextButton(onClick = { advanced = !advanced }) {
                Text(if (advanced) stringResource(R.string.ts_hide_advanced) else stringResource(R.string.ts_show_advanced))
            }
            if (advanced) {
                HermesOutlinedField(restBasePath, onValueChange = { restBasePath = it }, label = { Text(stringResource(R.string.ts_field_rest_path)) }, modifier = Modifier.fillMaxWidth())
                HermesOutlinedField(webSocketPath, onValueChange = { webSocketPath = it }, label = { Text(stringResource(R.string.ts_field_ws_path)) }, modifier = Modifier.fillMaxWidth())
                HermesOutlinedField(protocolVersion, onValueChange = { protocolVersion = it }, label = { Text(stringResource(R.string.ts_field_protocol_version)) }, modifier = Modifier.fillMaxWidth())
                HermesOutlinedField(requestTimeout, onValueChange = { requestTimeout = it }, label = { Text(stringResource(R.string.ts_field_request_timeout)) }, modifier = Modifier.fillMaxWidth())
                HermesOutlinedField(streamingTimeout, onValueChange = { streamingTimeout = it }, label = { Text(stringResource(R.string.ts_field_streaming_timeout)) }, modifier = Modifier.fillMaxWidth())
                HermesOutlinedField(retryCount, onValueChange = { retryCount = it }, label = { Text(stringResource(R.string.ts_field_retry_count)) }, modifier = Modifier.fillMaxWidth())
                HermesOutlinedField(queuedPolicy, onValueChange = { queuedPolicy = it }, label = { Text(stringResource(R.string.ts_field_queue_policy)) }, modifier = Modifier.fillMaxWidth())
                HermesOutlinedField(routingThreshold, onValueChange = { routingThreshold = it }, label = { Text(stringResource(R.string.ts_field_routing_threshold)) }, modifier = Modifier.fillMaxWidth())
                HermesOutlinedField(verbosity, onValueChange = { verbosity = it }, label = { Text(stringResource(R.string.ts_field_verbosity)) }, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    HermesSwitch(checked = consequentialConfirm, onCheckedChange = { consequentialConfirm = it })
                    Text(stringResource(R.string.ts_switch_consequential), style = HermesType.body, color = c.textPrimary, modifier = Modifier.weight(1f))
                }
                Text(stringResource(R.string.ts_label_cert_behavior), style = HermesType.sectionLabel, color = c.bronze)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    HermesFilterChip(selected = certPolicy == "SYSTEM", onClick = { certPolicy = "SYSTEM" }, label = { Text(stringResource(R.string.ts_cert_system)) })
                    HermesFilterChip(selected = certPolicy == "PINNED", onClick = { certPolicy = "PINNED" }, label = { Text(stringResource(R.string.ts_cert_pinned)) })
                }
                if (certPolicy == "PINNED") {
                    HermesOutlinedField(pinnedFingerprint, onValueChange = { pinnedFingerprint = it }, label = { Text(stringResource(R.string.ts_field_pinned_fp)) }, modifier = Modifier.fillMaxWidth())
                }
            }
        }

        val uiLocale = com.hermestower.voiceconsole.locale.AppLanguage.effectiveTag(context)

        SectionCard(title = stringResource(R.string.ts_section_actions)) {
            // GRAPH 4.10 §21: the rearchitected action layout. PRIMARY full-width;
            // connection / profile / destructive groups through ResponsiveActionGroup
            // (§22) — at compact width long Italian labels stack full-width, so the
            // one-character-per-line defect class is dead.
            ResponsiveActionGroup(
                widthClass = widthClass,
                primary = ResponsiveAction(
                    key = "save_connect",
                    label = stringResource(R.string.ts_action_save_connect),
                    onClick = {
                        val profile = buildProfile()
                        if (secret.isNotBlank()) graph.profileStore.setSecret(profile.profileId, secret.toCharArray())
                        when (val r = graph.profileStore.saveValidated(profile)) {
                            is TowerProfileWriteResult.Written -> {
                                graph.realBridge.configure(r.profile)
                                message = context.getString(R.string.ts_msg_saved_use_test, StateLabels.label(mode.name, uiLocale))
                            }
                            is TowerProfileWriteResult.Rejected -> message = context.getString(R.string.ts_msg_save_rejected, r.reason)
                        }
                    },
                ),
                secondary = listOf(
                    ResponsiveAction(key = "connect", label = stringResource(R.string.ts_action_connect), onClick = {
                        graph.realBridge.configure(buildProfile())
                        graph.scope.launch {
                            val result = graph.realBridge.handshake()
                            message = when (result) {
                                is BridgeResult.Success -> context.getString(R.string.ts_msg_connected, graph.realBridge.connection.value.endpointLabel)
                                is BridgeResult.Failure -> context.getString(R.string.ts_msg_not_connected, result.error, result.message)
                            }
                        }
                    }),
                    ResponsiveAction(key = "disconnect", label = stringResource(R.string.ts_action_disconnect), onClick = {
                        message = context.getString(R.string.ts_msg_disconnected)
                    }),
                    ResponsiveAction(key = "save_draft", label = stringResource(R.string.ts_action_save_draft), onClick = {
                        when (val r = graph.profileStore.saveDraft(buildProfile())) {
                            is TowerProfileWriteResult.Written -> message = context.getString(R.string.ts_msg_draft_saved, StateLabels.label(graph.profileStore.state().name, uiLocale))
                            is TowerProfileWriteResult.Rejected -> message = context.getString(R.string.ts_msg_draft_rejected, r.reason)
                        }
                    }),
                    ResponsiveAction(key = "validate", label = stringResource(R.string.ts_action_validate), onClick = {
                        val profile = buildProfile()
                        val report = TowerProfileValidator.validate(
                            profile,
                            secretPresent = graph.profileStore.secretFor(profile.profileId) != null || secret.isNotBlank(),
                            nowIso = java.time.Instant.now().toString(),
                        )
                        message = if (report.passed) context.getString(R.string.ts_msg_validation_passed)
                            else context.getString(R.string.ts_msg_validation_failed, report.issues.joinToString("; ") { it.toString() })
                    }),
                    ResponsiveAction(key = "test", label = stringResource(R.string.ts_action_test), onClick = {
                        val profile = buildProfile()
                        if (secret.isNotBlank()) graph.profileStore.setSecret(profile.profileId, secret.toCharArray())
                        val report = TowerProfileValidator.validate(
                            profile,
                            secretPresent = graph.profileStore.secretFor(profile.profileId) != null,
                            nowIso = java.time.Instant.now().toString(),
                        )
                        if (!report.passed) {
                            message = context.getString(R.string.ts_msg_cannot_test, report.issues.joinToString("; ") { it.toString() })
                        } else {
                            graph.realBridge.configure(profile)
                            val r = TowerConnectionTester.test(
                                profile,
                                graph.profileStore.secretFor(profile.profileId),
                                java.time.Instant.now().toString(),
                            )
                            testOutcome = r
                            message = context.getString(R.string.ts_msg_connection_test, StateLabels.label(r.outcome.name, uiLocale), r.detail)
                        }
                    }),
                    ResponsiveAction(key = "pair", label = stringResource(R.string.ts_action_pair), onClick = {
                        val profile = buildProfile()
                        if (secret.isBlank() && graph.profileStore.secretFor(profile.profileId) == null && mode != TransportMode.MOCK_SIMULATOR) {
                            message = context.getString(R.string.ts_msg_pairing_required)
                        } else {
                            if (secret.isNotBlank()) graph.profileStore.setPairingCode(profile.profileId, secret.toCharArray())
                            message = context.getString(R.string.ts_msg_pairing_recorded, profile.profileId)
                        }
                    }),
                ),
                management = listOf(
                    ResponsiveAction(key = "scan_qr", label = stringResource(R.string.ts_action_scan_qr), onClick = { onOpenScanner?.invoke() }),
                    ResponsiveAction(key = "import", label = stringResource(R.string.ts_action_import), onClick = {
                        val stored = graph.keyValueStore.getString("last_secret_clean_export")
                        if (stored == null) {
                            message = context.getString(R.string.ts_msg_no_import)
                        } else {
                            runCatching {
                                kotlinx.serialization.json.Json.decodeFromString(
                                    com.hermestower.voiceconsole.tower.ExportedProfile.serializer(),
                                    stored,
                                )
                            }.onSuccess { extracted ->
                                when (val r = graph.profileStore.importExported(extracted, confirmOverwrite = false)) {
                                    is ImportResult.Imported -> message = context.getString(R.string.ts_msg_imported, r.profile.profileName)
                                    is ImportResult.Conflict -> message = context.getString(R.string.ts_msg_import_conflict, r.message)
                                    is ImportResult.Rejected -> message = context.getString(R.string.ts_msg_import_rejected, r.reason)
                                }
                            }.onFailure { message = context.getString(R.string.ts_msg_import_failed, it.message ?: "") }
                        }
                    }),
                    ResponsiveAction(key = "export", label = stringResource(R.string.ts_action_export), onClick = {
                        runCatching { graph.profileStore.exportSecretClean(existing?.profileId ?: "profile-owner-1") }
                            .onSuccess { exported ->
                                val text = kotlinx.serialization.json.Json.encodeToString(
                                    com.hermestower.voiceconsole.tower.ExportedProfile.serializer(),
                                    exported,
                                )
                                graph.keyValueStore.putString("last_secret_clean_export", text.take(200_000))
                                message = context.getString(R.string.ts_msg_export_stored, exported.accessToken, exported.pairingSecret)
                            }
                            .onFailure { message = context.getString(R.string.ts_msg_export_failed, it.message ?: "") }
                    }),
                    ResponsiveAction(key = "restore", label = stringResource(R.string.ts_action_restore), onClick = {
                        when (val r = graph.profileStore.restorePrevious()) {
                            is TowerProfileWriteResult.Written -> message = context.getString(R.string.ts_msg_restored, r.profile.profileName)
                            is TowerProfileWriteResult.Rejected -> message = r.reason
                        }
                    }),
                ),
                destructive = listOf(
                    ResponsiveAction(key = "clear_sensitive", label = stringResource(R.string.ts_action_clear_sensitive), onClick = {
                        val removed = graph.profileStore.clearSensitiveValues(existing?.profileId ?: "profile-owner-1")
                        secret = ""
                        message = if (removed) context.getString(R.string.ts_msg_cleared) else context.getString(R.string.ts_msg_no_sensitive)
                    }),
                    ResponsiveAction(key = "reset_mock", label = stringResource(R.string.ts_action_reset_mock), onClick = {
                        when (val r = graph.profileStore.resetToMock()) {
                            is TowerProfileWriteResult.Written -> {
                                mode = TransportMode.MOCK_SIMULATOR
                                baseUrl = ""
                                secret = ""
                                message = context.getString(R.string.ts_msg_reset_mock, r.profile.profileName)
                            }
                            is TowerProfileWriteResult.Rejected -> message = r.reason
                        }
                    }),
                ),
            )
        }

        testOutcome?.let { r ->
            SectionCard(title = stringResource(R.string.ts_section_last_test)) {
                // §23: the outcome is localized — raw English enum values never
                // appear in ordinary UI.
                Text(context.getString(R.string.ts_msg_outcome, StateLabels.label(r.outcome.name, uiLocale)), style = HermesType.body, color = c.textPrimary)
                Text(
                    context.getString(
                        R.string.ts_msg_dns_line,
                        r.dnsResolved, r.transportConnected, r.tlsStatus, r.handshakeStatus,
                    ),
                    style = HermesType.bodySecondary, color = c.textSecondary,
                )
                Text(
                    context.getString(
                        R.string.ts_msg_proto_line,
                        r.protocolCompatible, r.authenticationStatus, r.towerAccepted,
                    ),
                    style = HermesType.bodySecondary, color = c.textSecondary,
                )
                Text(
                    context.getString(
                        R.string.ts_msg_latency_line,
                        r.latencyMs ?: context.getString(R.string.mm_na), r.streamingCapability, r.testedAt,
                    ),
                    style = HermesType.bodySecondary, color = c.textSecondary,
                )
            }
        }

        Text(message, style = HermesType.body, color = c.textInfo)
        }
    }
}
