package com.hermestower.voiceconsole.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hermestower.voiceconsole.app.AppGraph
import com.hermestower.voiceconsole.models.DeterministicTestArtifact
import com.hermestower.voiceconsole.models.ModelComponentType
import com.hermestower.voiceconsole.models.ModelSha256
import com.hermestower.voiceconsole.models.VoiceModelProfile
import com.hermestower.voiceconsole.towersim.LocalTowerMockServer
import com.hermestower.voiceconsole.ui.components.SectionCard
import com.hermestower.voiceconsole.ui.theme.HermesType
import com.hermestower.voiceconsole.ui.theme.hermesColors

private fun formatBytes(bytes: Long): String = when {
    bytes >= 1L shl 30 -> "%.1f GB".format(bytes.toDouble() / (1L shl 30))
    bytes >= 1L shl 20 -> "%.1f MB".format(bytes.toDouble() / (1L shl 20))
    bytes >= 1L shl 10 -> "%.1f KB".format(bytes.toDouble() / (1L shl 10))
    else -> "$bytes B"
}

/**
 * Settings → Voice Models and Local Intelligence (GRAPH 2 §6).
 * Sections: A recommended setup · B installed components · C available components
 * · D advanced actions · E modes · F Tower-only fallback. Nothing downloads or
 * activates silently: every action is an explicit button with a visible result.
 */
@Composable
fun ModelManagerScreen(graph: AppGraph) {
    val c = hermesColors
    var selectedProfile by remember { mutableStateOf(VoiceModelProfile.TOWER_FIRST) }
    var recommendation by remember { mutableStateOf<com.hermestower.voiceconsole.models.VoicePackRecommendation?>(null) }
    var statusNote by remember { mutableStateOf<String?>(null) }
    var appliedProfile by remember {
        mutableStateOf(
            graph.keyValueStore.getString("voice_profile")?.let { name ->
                VoiceModelProfile.entries.firstOrNull { it.name == name }
            } ?: VoiceModelProfile.TOWER_FIRST,
        )
    }
    var registryTick by remember { mutableStateOf(0) }
    val progressById = if (registryTick >= 0) {
        graph.modelCatalog.all().associate { it.identity to graph.modelDownloads.progress(it.identity).collectAsStateWithLifecycle() }
    } else emptyMap()

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Voice Models & Local Intelligence", style = HermesType.screenTitle, color = c.ink)
        Text(
            "Local intelligence is optional and independent per component; complex, uncertain and " +
                "consequential reasoning always belongs to Hermes Tower. No model is downloaded without " +
                "your explicit confirmation.",
            style = HermesType.bodySecondary,
            color = c.inkSecondary,
        )

        // ---------- A. Recommended setup ----------
        SectionCard(title = "A. Recommended setup") {
            val resources = remember { graph.deviceResourceReader.read() }
            Text("Detected device: ${resources.deviceModel} (Android ${resources.androidVersion})", style = HermesType.body, color = c.ink)
            Text(
                "ABIs: ${resources.supportedAbis.joinToString()} · RAM available: ${formatBytes(resources.availableRamBytes)} · Storage free: ${formatBytes(resources.freeStorageBytes)}",
                style = HermesType.body, color = c.ink,
            )
            Text(
                "TTS engines on device: ${if (resources.ttsEnginePackages.isEmpty()) "none reported" else resources.ttsEnginePackages.joinToString()}",
                style = HermesType.body, color = c.ink,
            )
            Text(
                "Network: ${if (resources.networkOnline) "online" else "offline"}${if (resources.networkMetered) " (metered — downloads will warn)" else ""}",
                style = HermesType.body, color = c.ink,
            )
            Text("Current profile: ${appliedProfile.displayLabel}", style = HermesType.body, color = c.ink)
            Text("Recommended profile: ${selectedProfile.displayLabel}", style = HermesType.body, color = c.ink)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 4.dp)) {
                VoiceModelProfile.entries.forEach { profile ->
                    FilterChip(
                        selected = selectedProfile == profile,
                        onClick = {
                            selectedProfile = profile
                            recommendation = graph.modelResolver.recommend(profile, resources, graph.modelCatalog)
                        },
                        label = { Text(profile.displayLabel) },
                    )
                }
            }
            val rec = recommendation
            if (rec != null) {
                Text("Compatibility summary: ${rec.routeSummary}", style = HermesType.body, color = c.ink)
                Text(
                    "Expected download: ${formatBytes(rec.expectedDownloadBytes)} · installed: ${formatBytes(rec.expectedInstalledBytes)} · " +
                        "RAM range: ${formatBytes(rec.ramEstimateLowBytes)}–${formatBytes(rec.ramEstimateHighBytes)}",
                    style = HermesType.body, color = c.ink,
                )
                rec.warnings.forEach { Text("⚠ $it", style = HermesType.bodySecondary, color = c.warn) }
                Button(onClick = {
                    graph.keyValueStore.putString("voice_profile", rec.profile)
                    appliedProfile = VoiceModelProfile.entries.first { it.name == rec.profile }
                    statusNote = "Recommended configuration applied: ${rec.profile}. Components below still require explicit download + activation."
                }) { Text("Apply Recommended Configuration") }
            } else {
                Text("Select a mode to compute the deterministic recommendation.", style = HermesType.bodySecondary, color = c.inkSecondary)
            }
        }

        // ---------- B. Installed voice components ----------
        SectionCard(title = "B. Installed voice components") {
            val installed = graph.modelRegistry.snapshot().filter { it.state != "REMOVED" }
            if (installed.isEmpty()) {
                Text(
                    "Nothing installed yet — the console runs fully in Tower-only fallback (usable without any local model).",
                    style = HermesType.bodySecondary, color = c.inkSecondary,
                )
            }
            ModelComponentType.entries.forEach { type ->
                val records = installed.filter { it.componentType == type.name }
                if (records.isNotEmpty()) {
                    Text(type.displayLabel, style = HermesType.sectionLabel, color = c.bronze)
                    records.forEach { record ->
                        Text(
                            "${record.displayName} v${record.version} — ${record.state}${if (record.active) " · ACTIVE" else ""}",
                            style = HermesType.body, color = c.ink,
                        )
                        Text(
                            "verified: ${record.lastVerificationAt ?: "never"} · qualified: ${record.lastQualificationAt ?: "never"}",
                            style = HermesType.bodySecondary, color = c.inkSecondary,
                        )
                    }
                }
            }
        }

        // ---------- C + D. Available components with advanced actions ----------
        SectionCard(title = "C. Available components") {
            graph.modelCatalog.all().forEach { entry ->
                val progress = progressById[entry.identity]?.value
                Text(entry.displayName, style = HermesType.sectionLabel, color = c.bronze)
                Text(
                    "${entry.identity} · v${entry.version} · ${entry.provider} · ${entry.license}" +
                        " · download ${formatBytes(entry.downloadSizeBytes)} · installed ${formatBytes(entry.installedSizeBytes)}" +
                        " · RAM est. ${formatBytes(entry.ramEstimateBytes)} · ${entry.latencyClass}" +
                        " · languages ${entry.languages.joinToString()}" +
                        if (entry.italianSupported) " · Italian: yes" else " · Italian: no",
                    style = HermesType.bodySecondary, color = c.inkSecondary,
                )
                val rec = recommendation
                val resolved = rec?.components?.firstOrNull { it.identity == entry.identity }
                Text(
                    "Compatibility: ${resolved?.compatible ?: false}" +
                        (resolved?.incompatibilityReason?.let { " ($it)" } ?: "") +
                        " · qualification state: ${entry.state}",
                    style = HermesType.bodySecondary, color = c.inkSecondary,
                )
                progress?.let {
                    if (it.state == com.hermestower.voiceconsole.models.ModelInstallState.DOWNLOADING ||
                        it.state == com.hermestower.voiceconsole.models.ModelInstallState.DOWNLOAD_PAUSED
                    ) {
                        Text(
                            "Download: ${it.state.displayLabel} ${it.bytesDownloaded}/${it.totalBytes} bytes (${(it.fraction * 100).toInt()}%)",
                            style = HermesType.body, color = c.ink,
                        )
                    }
                    it.error?.let { err -> Text("Last download event: $err", style = HermesType.bodySecondary, color = c.warn) }
                }

                // ---------- D. Advanced actions ----------
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(
                        enabled = resolved?.downloadAllowed == true &&
                            progress?.state != com.hermestower.voiceconsole.models.ModelInstallState.DOWNLOADING,
                        onClick = {
                            if (entry.identity == PROBE_IDENTITY) {
                                // The only downloadable artifact in V0.2: the deterministic test probe,
                                // generated locally and served by the in-process mock server (127.0.0.1).
                                val server = LocalTowerMockServer()
                                val bytes = DeterministicTestArtifact.generate("hermes-test-artifact-v1", 4096)
                                server.putArtifact("hermes-test-artifact-v1", LocalTowerMockServer.Artifact(bytes))
                                graph.keyValueStore.putString("probe_server_port", server.port.toString())
                                graph.modelDownloads.start(
                                    entry.copy(sha256 = ModelSha256.ofBytes(bytes)),
                                    "${server.baseUrl}/artifacts/hermes-test-artifact-v1",
                                )
                                statusNote = "Probe download started from the local test server (port ${server.port})."
                            } else {
                                statusNote = "Download not available for ${entry.identity}: ${resolved?.downloadBlockReason ?: "no verified binary published yet"}."
                            }
                            registryTick++
                        },
                    ) { Text("Download") }
                    TextButton(onClick = { graph.modelDownloads.pause(entry.identity); statusNote = "Download paused (${entry.identity})." }) { Text("Pause") }
                    TextButton(
                        enabled = entry.identity == PROBE_IDENTITY,
                        onClick = {
                            val port = graph.keyValueStore.getString("probe_server_port")?.toIntOrNull()
                            if (port != null) {
                                val bytes = DeterministicTestArtifact.generate("hermes-test-artifact-v1", 4096)
                                graph.modelDownloads.resume(entry.copy(sha256 = ModelSha256.ofBytes(bytes)), "http://127.0.0.1:$port/artifacts/hermes-test-artifact-v1")
                                statusNote = "Download resumed (${entry.identity})."
                            } else {
                                statusNote = "No resumable local download server recorded."
                            }
                        },
                    ) { Text("Resume") }
                    TextButton(onClick = { graph.modelDownloads.cancel(entry.identity); statusNote = "Download cancelled (${entry.identity})." }) { Text("Cancel") }
                    TextButton(onClick = {
                        statusNote = if (graph.modelDownloads.verifiedExisting(entry)) "Installed file hash verified."
                        else "No verified installed file for ${entry.identity}."
                    }) { Text("Verify") }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(onClick = {
                        val result = graph.modelQualification.qualify(entry)
                        statusNote = "Qualification ${entry.identity}: ${if (result.qualified) "PASSED" else "FAILED"} — " + result.checks.joinToString { "${it.name}=${it.passed}" }
                    }) { Text("Qualify") }
                    TextButton(onClick = {
                        val result = graph.modelActivation.activate(entry)
                        statusNote = "Activate ${entry.identity}: ${if (result.success) "ACTIVE" else "REFUSED"} — ${result.reason}"
                        registryTick++
                    }) { Text("Activate") }
                    TextButton(onClick = {
                        val result = graph.modelActivation.deactivate(entry)
                        statusNote = "Deactivate ${entry.identity}: ${result.reason}"
                        registryTick++
                    }) { Text("Deactivate") }
                    TextButton(onClick = {
                        val result = graph.modelRollback.rollback(entry)
                        statusNote = "Rollback ${entry.identity}: ${if (result.success) "restored v${result.restoredVersion}" else result.reason}"
                        registryTick++
                    }) { Text("Roll back") }
                    TextButton(onClick = {
                        val removed = graph.modelRollback.removalCleanup(entry)
                        statusNote = if (removed) "Removed ${entry.identity} (files + registry)." else "Nothing to remove for ${entry.identity}."
                        registryTick++
                    }) { Text("Remove") }
                }
            }
        }

        // ---------- E. Modes ----------
        SectionCard(title = "E. Modes") {
            Text(
                "TOWER_FIRST — wake word + VAD local, everything else to the Tower.\n" +
                    "HYBRID_BALANCED — simple requests local; complex/uncertain/consequential to the Tower.\n" +
                    "LOCAL_EXPERIMENTAL — full local pipeline, disabled by default, explicit activation, RAM/storage/thermal gates.\n" +
                    "MANUAL_ADVANCED — independent per-component selection, full visibility, no silent activation.",
                style = HermesType.body, color = c.ink,
            )
        }

        // ---------- F. Tower-only fallback ----------
        SectionCard(title = "F. Tower-only fallback") {
            Text(
                "The console remains fully usable with zero downloaded models: wake word simulation, captions, " +
                    "history, diagnostics and the Tower conversation run without any local LLM.",
                style = HermesType.body, color = c.ink,
            )
        }

        statusNote?.let {
            Text("Status: $it", style = HermesType.body, color = c.energy)
        }
    }
}

internal const val PROBE_IDENTITY = "org.hermestower.test.probe-artifact"
