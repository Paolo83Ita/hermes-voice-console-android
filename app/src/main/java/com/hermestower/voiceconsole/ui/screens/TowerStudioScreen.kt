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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.hermestower.voiceconsole.app.AppGraph
import com.hermestower.voiceconsole.domain.BridgeResult
import com.hermestower.voiceconsole.tower.ImportResult
import com.hermestower.voiceconsole.tower.TransportMode
import com.hermestower.voiceconsole.tower.TowerConnectionProfile
import com.hermestower.voiceconsole.tower.TowerConnectionTester
import com.hermestower.voiceconsole.tower.TowerProfileValidator
import com.hermestower.voiceconsole.tower.TowerProfileWriteResult
import com.hermestower.voiceconsole.ui.components.SectionCard
import com.hermestower.voiceconsole.ui.theme.HermesType
import com.hermestower.voiceconsole.ui.theme.hermesColors
import kotlinx.coroutines.launch

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

    var message by remember { mutableStateOf("Configuration state: ${graph.profileStore.state().displayLabel}") }
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

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Connect Hermes Tower", style = HermesType.screenTitle, color = c.ink)
        Text(
            "Name this console, enter the Tower address, choose how to connect, then test. " +
                "Pairing codes and tokens live in the Android Keystore — never in settings, logs or exports.",
            style = HermesType.bodySecondary, color = c.inkSecondary,
        )

        SectionCard(title = "Basic") {
            OutlinedTextField(name, onValueChange = { name = it }, label = { Text("Connection name") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(
                baseUrl, onValueChange = { baseUrl = it },
                label = { Text("Tower address or base URL") },
                modifier = Modifier.fillMaxWidth(), enabled = mode != TransportMode.MOCK_SIMULATOR,
            )
            Text("Connection method", style = HermesType.sectionLabel, color = c.bronze)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                TransportMode.entries.take(3).forEach { m ->
                    FilterChip(selected = mode == m, onClick = { mode = m }, label = { Text(m.displayLabel) })
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                TransportMode.entries.drop(3).forEach { m ->
                    FilterChip(selected = mode == m, onClick = { mode = m }, label = { Text(m.displayLabel) })
                }
            }
            Text(
                when (mode) {
                    TransportMode.MOCK_SIMULATOR -> "Mock mode needs no address and no secret."
                    TransportMode.TAILSCALE_PRIVATE -> "Use the Tailscale hostname (…ts.net) or 100.x.y.z address of the Tower."
                    else -> "Use an https:// or wss:// address."
                },
                style = HermesType.bodySecondary, color = c.inkSecondary,
            )
            OutlinedTextField(
                secret, onValueChange = { secret = it },
                label = { Text("Pairing code or access token") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(), enabled = mode != TransportMode.MOCK_SIMULATOR,
            )
            OutlinedTextField(userRef, onValueChange = { userRef = it }, label = { Text("User or Owner reference") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(deviceAlias, onValueChange = { deviceAlias = it }, label = { Text("Device display name") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(defaultMode, onValueChange = { defaultMode = it }, label = { Text("Default operating mode") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(language, onValueChange = { language = it }, label = { Text("Preferred language") }, modifier = Modifier.fillMaxWidth())
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Switch(checked = autoConnect, onCheckedChange = { autoConnect = it })
                Text("Connect automatically", style = HermesType.body, color = c.ink)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Switch(checked = useTailscale, onCheckedChange = { useTailscale = it })
                Text("Use Tailscale address", style = HermesType.body, color = c.ink)
            }
        }

        SectionCard(title = "Advanced") {
            TextButton(onClick = { advanced = !advanced }) {
                Text(if (advanced) "Hide advanced controls" else "Show advanced controls")
            }
            if (advanced) {
                OutlinedTextField(restBasePath, onValueChange = { restBasePath = it }, label = { Text("REST base path") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(webSocketPath, onValueChange = { webSocketPath = it }, label = { Text("WebSocket path") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(protocolVersion, onValueChange = { protocolVersion = it }, label = { Text("Protocol version") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(requestTimeout, onValueChange = { requestTimeout = it }, label = { Text("Request timeout (ms)") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(streamingTimeout, onValueChange = { streamingTimeout = it }, label = { Text("Streaming timeout (ms)") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(retryCount, onValueChange = { retryCount = it }, label = { Text("Bounded retry count") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(queuedPolicy, onValueChange = { queuedPolicy = it }, label = { Text("Queued-request policy") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(routingThreshold, onValueChange = { routingThreshold = it }, label = { Text("Local/Tower routing threshold (0–1)") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(verbosity, onValueChange = { verbosity = it }, label = { Text("Diagnostics verbosity") }, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Switch(checked = consequentialConfirm, onCheckedChange = { consequentialConfirm = it })
                    Text("Require confirmation for consequential requests", style = HermesType.body, color = c.ink)
                }
                Text("Certificate behavior", style = HermesType.sectionLabel, color = c.bronze)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(selected = certPolicy == "SYSTEM", onClick = { certPolicy = "SYSTEM" }, label = { Text("System trust") })
                    FilterChip(selected = certPolicy == "PINNED", onClick = { certPolicy = "PINNED" }, label = { Text("Pinned fingerprint") })
                }
                if (certPolicy == "PINNED") {
                    OutlinedTextField(pinnedFingerprint, onValueChange = { pinnedFingerprint = it }, label = { Text("Pinned SHA-256 fingerprint") }, modifier = Modifier.fillMaxWidth())
                }
            }
        }

        SectionCard(title = "Actions") {
            if (onOpenScanner != null) {
                Text(
                    "Pairing via QR: scan the Tower's hermes-pair code with the camera — the " +
                        "strict versioned parser checks it before anything is offered for pairing.",
                    style = HermesType.bodySecondary, color = c.inkSecondary,
                )
                Button(onClick = { onOpenScanner() }) { Text("Scan pairing QR") }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                TextButton(onClick = {
                    when (val r = graph.profileStore.saveDraft(buildProfile())) {
                        is TowerProfileWriteResult.Written -> message = "Draft saved. State: ${graph.profileStore.state().displayLabel}"
                        is TowerProfileWriteResult.Rejected -> message = "Draft rejected: ${r.reason}"
                    }
                }) { Text("Save draft") }
                TextButton(onClick = {
                    val profile = buildProfile()
                    val report = TowerProfileValidator.validate(
                        profile,
                        secretPresent = graph.profileStore.secretFor(profile.profileId) != null || secret.isNotBlank(),
                        nowIso = java.time.Instant.now().toString(),
                    )
                    message = if (report.passed) "Validation passed." else "Validation failed: " + report.issues.joinToString("; ") { it.toString() }
                }) { Text("Validate fields") }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                TextButton(onClick = {
                    val profile = buildProfile()
                    if (secret.isNotBlank()) graph.profileStore.setSecret(profile.profileId, secret.toCharArray())
                    val report = TowerProfileValidator.validate(
                        profile,
                        secretPresent = graph.profileStore.secretFor(profile.profileId) != null,
                        nowIso = java.time.Instant.now().toString(),
                    )
                    if (!report.passed) {
                        message = "Cannot test: " + report.issues.joinToString("; ") { it.toString() }
                    } else {
                        graph.realBridge.configure(profile)
                        val r = TowerConnectionTester.test(
                            profile,
                            graph.profileStore.secretFor(profile.profileId),
                            java.time.Instant.now().toString(),
                        )
                        testOutcome = r
                        message = "Connection test: ${r.outcome.name} — ${r.detail}"
                    }
                }) { Text("Test connection") }
                TextButton(onClick = {
                    val profile = buildProfile()
                    if (secret.isBlank() && graph.profileStore.secretFor(profile.profileId) == null && mode != TransportMode.MOCK_SIMULATOR) {
                        message = "Pairing requires a pairing code or token."
                    } else {
                        if (secret.isNotBlank()) graph.profileStore.setPairingCode(profile.profileId, secret.toCharArray())
                        message = "Pairing recorded for ${profile.profileId}. Verify with Test connection."
                    }
                }) { Text("Pair device") }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Button(onClick = {
                    val profile = buildProfile()
                    if (secret.isNotBlank()) graph.profileStore.setSecret(profile.profileId, secret.toCharArray())
                    when (val r = graph.profileStore.saveValidated(profile)) {
                        is TowerProfileWriteResult.Written -> {
                            graph.realBridge.configure(r.profile)
                            message = "Saved (${mode.displayLabel}). Use Test connection to verify; CONNECTED is shown only after an accepted handshake."
                        }
                        is TowerProfileWriteResult.Rejected -> message = "Save rejected: ${r.reason}"
                    }
                }) { Text("Save and connect") }
                TextButton(onClick = {
                    graph.realBridge.configure(buildProfile())
                    graph.scope.launch {
                        val result = graph.realBridge.handshake()
                        message = when (result) {
                            is BridgeResult.Success -> "Connected: handshake accepted by ${graph.realBridge.connection.value.endpointLabel}."
                            is BridgeResult.Failure -> "Not connected: ${result.error} — ${result.message}"
                        }
                    }
                }) { Text("Connect") }
                TextButton(onClick = {
                    message = "Disconnected (local mock pipeline remains available)."
                }) { Text("Disconnect") }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                TextButton(onClick = {
                    val removed = graph.profileStore.clearSensitiveValues(existing?.profileId ?: "profile-owner-1")
                    secret = ""
                    message = if (removed) "Sensitive values cleared; profile metadata preserved." else "No sensitive values stored."
                }) { Text("Clear sensitive values") }
                TextButton(onClick = {
                    runCatching { graph.profileStore.exportSecretClean(existing?.profileId ?: "profile-owner-1") }
                        .onSuccess { exported ->
                            val text = kotlinx.serialization.json.Json.encodeToString(
                                com.hermestower.voiceconsole.tower.ExportedProfile.serializer(),
                                exported,
                            )
                            graph.keyValueStore.putString("last_secret_clean_export", text.take(200_000))
                            message = "Secret-clean export stored (placeholders: ${exported.accessToken}, ${exported.pairingSecret}). " +
                                "Copy it from Diagnostics."
                        }
                        .onFailure { message = "Export failed: ${it.message}" }
                }) { Text("Export secret-clean profile") }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                TextButton(onClick = {
                    val stored = graph.keyValueStore.getString("last_secret_clean_export")
                    if (stored == null) {
                        message = "No stored export to import in this build."
                    } else {
                        runCatching {
                            kotlinx.serialization.json.Json.decodeFromString(
                                com.hermestower.voiceconsole.tower.ExportedProfile.serializer(),
                                stored,
                            )
                        }.onSuccess { extracted ->
                            when (val r = graph.profileStore.importExported(extracted, confirmOverwrite = false)) {
                                is ImportResult.Imported -> message = "Imported profile '${r.profile.profileName}'."
                                is ImportResult.Conflict -> message = "Conflict: ${r.message} Confirm to replace (previous preserved)."
                                is ImportResult.Rejected -> message = "Import rejected: ${r.reason}"
                            }
                        }.onFailure { message = "Import failed: ${it.message}" }
                    }
                }) { Text("Import profile") }
                TextButton(onClick = {
                    when (val r = graph.profileStore.restorePrevious()) {
                        is TowerProfileWriteResult.Written -> message = "Previous configuration restored ('${r.profile.profileName}')."
                        is TowerProfileWriteResult.Rejected -> message = r.reason
                    }
                }) { Text("Restore previous") }
                TextButton(onClick = {
                    when (val r = graph.profileStore.resetToMock()) {
                        is TowerProfileWriteResult.Written -> {
                            mode = TransportMode.MOCK_SIMULATOR
                            baseUrl = ""
                            secret = ""
                            message = "Reset to Mock mode ('${r.profile.profileName}')."
                        }
                        is TowerProfileWriteResult.Rejected -> message = r.reason
                    }
                }) { Text("Reset to Mock mode") }
            }
        }

        testOutcome?.let { r ->
            SectionCard(title = "Last connection test") {
                Text("Outcome: ${r.outcome.name}", style = HermesType.body, color = c.ink)
                Text(
                    "DNS: ${r.dnsResolved} · transport: ${r.transportConnected} · TLS: ${r.tlsStatus} · handshake: ${r.handshakeStatus}",
                    style = HermesType.bodySecondary, color = c.inkSecondary,
                )
                Text(
                    "Protocol compatible: ${r.protocolCompatible} · auth: ${r.authenticationStatus} · Tower accepted: ${r.towerAccepted}",
                    style = HermesType.bodySecondary, color = c.inkSecondary,
                )
                Text(
                    "Latency: ${r.latencyMs ?: "n/a"} ms · streaming: ${r.streamingCapability} · at ${r.testedAt}",
                    style = HermesType.bodySecondary, color = c.inkSecondary,
                )
            }
        }

        Text(message, style = HermesType.body, color = c.energy)
    }
}
