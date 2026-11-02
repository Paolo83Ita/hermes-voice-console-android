package com.hermestower.voiceconsole.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hermestower.voiceconsole.app.AppGraph
import com.hermestower.voiceconsole.models.ComponentFallback
import com.hermestower.voiceconsole.models.DeterministicTestArtifact
import com.hermestower.voiceconsole.models.ModelInstallState
import com.hermestower.voiceconsole.models.ModelSha256
import com.hermestower.voiceconsole.models.VoiceModelProfile
import com.hermestower.voiceconsole.towersim.LocalTowerMockServer
import com.hermestower.voiceconsole.ui.components.SectionCard
import com.hermestower.voiceconsole.ui.theme.HermesType
import com.hermestower.voiceconsole.ui.theme.hermesColors
import kotlinx.coroutines.launch

private fun formatBytes(bytes: Long): String = when {
    bytes >= 1L shl 30 -> "%.1f GB".format(bytes.toDouble() / (1L shl 30))
    bytes >= 1L shl 20 -> "%.1f MB".format(bytes.toDouble() / (1L shl 20))
    bytes >= 1L shl 10 -> "%.1f KB".format(bytes.toDouble() / (1L shl 10))
    else -> "$bytes B"
}

/**
 * MODEL SANCTUM — Models and Voice (GRAPH 4 §6/§7, refounding the Graph 2 Voice
 * Model Manager). The screen is a live window into the real delivery engine:
 * recommended minimal pack with a full preflight summary and explicit confirmation,
 * per-component attribute sheets with provenance and fallback contracts, and every
 * lifecycle action wired to the verified catalog sources. Nothing downloads or
 * activates silently; the optional local LLM is never part of the pack.
 */
@Composable
fun ModelManagerScreen(graph: AppGraph) {
    val c = hermesColors
    val scope = rememberCoroutineScope()
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
    var packRunning by remember { mutableStateOf(false) }
    var packEvents by remember { mutableStateOf<List<String>>(emptyList()) }
    var packConfirmOpen by remember { mutableStateOf(false) }
    val expanded = remember { mutableStateMapOf<String, Boolean>() }
    val diagnosticsOpen = remember { mutableStateOf(false) }
    // GRAPH 4 §6: the pack plan and its preflight summary are computed once per
    // registry change and shared by the pack card and the confirmation dialog.
    val packResources = remember { graph.deviceResourceReader.read() }
    val computedPlan = remember(registryTick) {
        graph.modelPackInstaller.plan(VoiceModelProfile.HYBRID_BALANCED, packResources, graph.modelCatalog)
    }
    val computedSummary = remember(registryTick) { graph.modelPackInstaller.summarize(computedPlan, packResources) }

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
        Text("MODEL SANCTUM", style = HermesType.screenTitle, color = c.ink)
        Text("Models and Voice", style = HermesType.sectionLabel, color = c.bronze)
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

        // ---------- RECOMMENDED VOICE PACK (GRAPH 4 §6 bulk action) ----------
        SectionCard(title = "INSTALL RECOMMENDED VOICE PACK") {
            val plan = computedPlan
            val summary = computedSummary
            val included = plan.includedItems

            Text("RECOMMENDED SETUP — minimal voice pack for this device (profile: Hybrid balanced)", style = HermesType.sectionLabel, color = c.bronze)
            if (included.isEmpty()) {
                Text(
                    "No additional components are required right now: everything the minimal pack needs is already installed or bundled.",
                    style = HermesType.bodySecondary, color = c.inkSecondary,
                )
            } else {
                Text("Will be installed (${included.size}):", style = HermesType.body, color = c.ink)
                included.forEach { item ->
                    Text(
                        "• ${item.entry?.displayName} v${item.entry?.version} — download ${formatBytes(item.entry?.downloadSizeBytes ?: 0)}",
                        style = HermesType.body, color = c.ink,
                    )
                }
            }
            plan.items.filter { it.role == "BUNDLED_RUNTIME" }.forEach {
                Text("• ${it.entry?.displayName} — ${it.reason}", style = HermesType.bodySecondary, color = c.inkSecondary)
            }
            plan.items.filter { it.role == "ALREADY_INSTALLED" }.forEach {
                Text("• ${it.entry?.displayName} — ${it.reason}", style = HermesType.bodySecondary, color = c.inkSecondary)
            }
            summary.blockedItems.forEach { (name, reason) ->
                Text("⚠ $name — $reason", style = HermesType.bodySecondary, color = c.warn)
            }
            Text(
                "Download total: ${formatBytes(summary.totalDownloadBytes)} · installed total: ${formatBytes(summary.totalInstalledBytes)} · " +
                    "peak temporary storage: ${formatBytes(summary.peakTempBytes)}",
                style = HermesType.body, color = c.ink,
            )
            Text(
                "Free-space reserve kept: ${formatBytes(summary.reserveBytes)} (greater of 2 GB and 15% of available) — " +
                    "minimum free required: ${formatBytes(summary.minimumFreeRequiredBytes)}",
                style = HermesType.body, color = c.ink,
            )
            Text("Expected duration: ${summary.durationRangeText}", style = HermesType.body, color = c.ink)
            Text(summary.wifiAdvice, style = HermesType.bodySecondary, color = c.inkSecondary)
            Text(summary.chargingAdvice, style = HermesType.bodySecondary, color = c.inkSecondary)
            Text(summary.thermalAdvice, style = HermesType.bodySecondary, color = c.inkSecondary)
            Text("Licenses: ${summary.licenses.joinToString()}", style = HermesType.bodySecondary, color = c.inkSecondary)
            if (summary.optionalItems.isNotEmpty()) {
                Text(
                    "Optional and NOT included (never auto-downloaded): ${summary.optionalItems.joinToString()} — including the local LLM; complex reasoning stays on Hermes Tower.",
                    style = HermesType.bodySecondary, color = c.inkSecondary,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 4.dp)) {
                Button(
                    enabled = !packRunning && included.isNotEmpty(),
                    onClick = { packConfirmOpen = true },
                ) { Text("Install Recommended Voice Pack") }
            }
            if (packEvents.isNotEmpty()) {
                Column(Modifier.padding(top = 4.dp)) {
                    packEvents.takeLast(4).forEach {
                        Text(it, style = HermesType.bodySecondary, color = c.energy)
                    }
                }
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
            com.hermestower.voiceconsole.models.ModelComponentType.entries.forEach { type ->
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

        // ---------- C. Available components ----------
        SectionCard(title = "C. Available components") {
            graph.modelCatalog.all().forEach { entry ->
                val progress = progressById[entry.identity]?.value
                val record = graph.modelRegistry.record(entry.identity)
                val isExpanded = expanded[entry.identity] == true
                val updateAvailable = record != null && record.version != entry.version

                Text(entry.displayName, style = HermesType.sectionLabel, color = c.bronze)
                Text(
                    "${entry.identity} · v${entry.version} · ${entry.provider} · ${entry.license}" +
                        " · download ${formatBytes(entry.downloadSizeBytes)} · installed ${formatBytes(entry.installedSizeBytes)}" +
                        " · RAM est. ${formatBytes(entry.ramEstimateBytes)} · ${entry.latencyClass}" +
                        " · languages ${entry.languages.joinToString()}" +
                        if (entry.italianSupported) " · Italian: yes" else " · Italian: no",
                    style = HermesType.bodySecondary, color = c.inkSecondary,
                )
                Text(
                    "Classification: ${entry.entryClassification}" +
                        (if (entry.optionalComponent) " · OPTIONAL (never auto-downloaded)" else " · required-class") +
                        " · state: ${record?.state ?: entry.state}" +
                        (if (record?.active == true) " · ACTIVE" else ""),
                    style = HermesType.bodySecondary, color = c.inkSecondary,
                )
                if (updateAvailable) {
                    Text(
                        "Update available: installed v${record?.version} → catalog v${entry.version}.",
                        style = HermesType.body, color = c.energy,
                    )
                }
                val rec = recommendation
                val resolved = rec?.components?.firstOrNull { it.identity == entry.identity }
                Text(
                    "Compatibility: ${resolved?.compatible ?: false}" +
                        (resolved?.incompatibilityReason?.let { " ($it)" } ?: ""),
                    style = HermesType.bodySecondary, color = c.inkSecondary,
                )
                progress?.let {
                    if (it.state == ModelInstallState.DOWNLOADING || it.state == ModelInstallState.DOWNLOAD_PAUSED) {
                        Text(
                            "Download: ${it.state.displayLabel} ${it.bytesDownloaded}/${it.totalBytes} bytes (${(it.fraction * 100).toInt()}%)",
                            style = HermesType.body, color = c.ink,
                        )
                    }
                    it.error?.let { err -> Text("Last download event: $err", style = HermesType.bodySecondary, color = c.warn) }
                }

                if (isExpanded) {
                    // ---- Full attribute sheet (GRAPH 4 §6, all fields) ----
                    AttributeLine("Component type", entry.componentTypeEnum().displayLabel)
                    AttributeLine("Version", entry.version)
                    AttributeLine("Provider", entry.provider)
                    AttributeLine("License", entry.license + (entry.licenseUrl?.let { " ($it)" } ?: ""))
                    AttributeLine("Installed status", record?.let { "${it.state} (v${it.version})" } ?: "not installed")
                    AttributeLine("Qualification status", record?.lastQualificationAt?.let { "qualified at $it" } ?: entry.state)
                    AttributeLine("Download size", formatBytes(entry.downloadSizeBytes))
                    AttributeLine("Installed size", formatBytes(entry.installedSizeBytes))
                    AttributeLine("RAM estimate", formatBytes(entry.ramEstimateBytes))
                    AttributeLine("Expected usage mode", "${entry.latencyClass} latency · runs locally in its storage root")
                    AttributeLine("Italian support", if (entry.italianSupported) "yes" else "no (${entry.languages.joinToString()})")
                    AttributeLine("Runtime format", entry.format)
                    AttributeLine("Quantization", entry.quantization)
                    AttributeLine("Storage destination", entry.storageLocation)
                    AttributeLine("Update availability", if (updateAvailable) "v${record?.version} installed, v${entry.version} available" else "current")
                    AttributeLine("Active state", if (record?.active == true) "ACTIVE" else "inactive")
                    AttributeLine("Fallback", ComponentFallback.forType(entry.componentType))
                    AttributeLine("Upstream", entry.upstreamRepo ?: "n/a")
                    AttributeLine("Revision binding", entry.revisionBinding ?: "n/a")
                    AttributeLine("Runtime requirement", entry.runtimeRequirement ?: "none")
                    AttributeLine("Provenance", entry.provenanceNote ?: "not yet verified")
                    entry.knownLimitations.forEach { Text("• $it", style = HermesType.bodySecondary, color = c.inkSecondary) }
                }

                // ---------- D. Lifecycle actions (all wired to the real engine) ----------
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(
                        enabled = resolved?.downloadAllowed != false &&
                            entry.entryClassification == "VERIFIED_DOWNLOADABLE" &&
                            progress?.state != ModelInstallState.DOWNLOADING && !packRunning,
                        onClick = {
                            if (entry.identity == PROBE_IDENTITY) {
                                // The only mock-served artifact: the deterministic test probe.
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
                                graph.modelDownloads.start(entry, entry.sourceUri)
                                statusNote = "Download started from the verified catalog source: ${entry.sourceUri}."
                            }
                            registryTick++
                        },
                    ) { Text("Download") }
                    TextButton(onClick = { graph.modelDownloads.pause(entry.identity); statusNote = "Download paused (${entry.identity})." }) { Text("Pause") }
                    TextButton(
                        onClick = {
                            if (entry.identity == PROBE_IDENTITY) {
                                val port = graph.keyValueStore.getString("probe_server_port")?.toIntOrNull()
                                if (port != null) {
                                    val bytes = DeterministicTestArtifact.generate("hermes-test-artifact-v1", 4096)
                                    graph.modelDownloads.resume(entry.copy(sha256 = ModelSha256.ofBytes(bytes)), "http://127.0.0.1:$port/artifacts/hermes-test-artifact-v1")
                                    statusNote = "Download resumed (${entry.identity})."
                                } else {
                                    statusNote = "No resumable local download server recorded."
                                }
                            } else {
                                graph.modelDownloads.resume(entry, entry.sourceUri)
                                statusNote = "Download resumed from the verified source (${entry.identity})."
                            }
                        },
                    ) { Text("Resume") }
                    TextButton(onClick = { graph.modelDownloads.cancel(entry.identity); statusNote = "Download cancelled (${entry.identity})." }) { Text("Cancel") }
                    TextButton(onClick = {
                        statusNote = if (graph.modelDownloads.verifiedExisting(entry)) "Installed file hash verified."
                        else "No verified installed file for ${entry.identity}."
                    }) { Text("Verify") }
                    TextButton(onClick = { expanded[entry.identity] = !isExpanded }) { Text(if (isExpanded) "Hide details" else "Details") }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(
                        enabled = !packRunning,
                        onClick = {
                            val resources = graph.deviceResourceReader.read()
                            scope.launch {
                                if (graph.modelDownloads.verifiedExisting(entry)) {
                                    val q = graph.modelQualification.qualify(entry)
                                    val a = if (q.qualified) graph.modelActivation.activate(entry) else null
                                    statusNote = "Already verified — qualification ${if (q.qualified) "PASSED" else "FAILED"}" +
                                        (a?.let { " · activation ${if (it.success) "ACTIVE" else "REFUSED: ${it.reason}"}" } ?: "")
                                    registryTick++
                                } else {
                                    val outcome = graph.modelPackInstaller.installComponent(entry, resources) { statusNote = it }
                                    statusNote = "Install ${entry.displayName}: ${if (outcome.success) "COMPLETED" else "FAILED"} — ${outcome.reason}"
                                    registryTick++
                                }
                            }
                        },
                    ) { Text("Install") }
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
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(
                        enabled = entry.entryClassification == "VERIFIED_DOWNLOADABLE" && entry.identity != PROBE_IDENTITY && !packRunning,
                        onClick = {
                            graph.modelDownloads.cancel(entry.identity)
                            val removed = graph.modelRollback.removalCleanup(entry)
                            graph.modelDownloads.start(entry, entry.sourceUri)
                            statusNote = "Re-download ${entry.displayName}: previous copy ${if (removed) "removed" else "was absent"}, fetching from the verified source."
                            registryTick++
                        },
                    ) { Text("Re-download") }
                    TextButton(
                        enabled = entry.identity != PROBE_IDENTITY && !packRunning,
                        onClick = {
                            if (graph.modelDownloads.verifiedExisting(entry)) {
                                statusNote = "Repair ${entry.displayName}: installed file hash intact — no repair needed."
                            } else {
                                graph.modelDownloads.cancel(entry.identity)
                                graph.modelDownloads.start(entry, entry.sourceUri)
                                statusNote = "Repair ${entry.displayName}: hash invalid or missing — fetching from the verified source."
                            }
                            registryTick++
                        },
                    ) { Text("Repair") }
                    TextButton(
                        enabled = updateAvailable && !packRunning,
                        onClick = {
                            graph.modelDownloads.start(entry, entry.sourceUri)
                            statusNote = "Update ${entry.displayName}: v${record?.version} → v${entry.version} (previous version is preserved automatically)."
                            registryTick++
                        },
                    ) { Text("Update") }
                    TextButton(onClick = { diagnosticsOpen.value = !diagnosticsOpen.value }) { Text("Open diagnostics") }
                }
            }

            if (diagnosticsOpen.value) {
                Text("Diagnostics — storage roots (bytes used):", style = HermesType.sectionLabel, color = c.bronze)
                graph.modelStorage.rootsUsage().forEach { (root, bytes) ->
                    Text("• $root: ${formatBytes(bytes)}", style = HermesType.bodySecondary, color = c.inkSecondary)
                }
                val receipts = com.hermestower.voiceconsole.models.ReceiptWriter.listReceipts(graph.modelStorage.receiptsDir())
                Text(
                    "Installation receipts: ${if (receipts.isEmpty()) "none yet" else "${receipts.size} written (latest: ${receipts.last()})"}",
                    style = HermesType.bodySecondary, color = c.inkSecondary,
                )
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

    // ---------- Pack confirmation (explicit, with the full summary) ----------
    if (packConfirmOpen) {
        val summary = computedSummary
        val plan = computedPlan
        AlertDialog(
            onDismissRequest = { packConfirmOpen = false },
            title = { Text("Install recommended voice pack?") },
            text = {
                Column {
                    Text(
                        "${plan?.includedItems?.size ?: 0} components · ${formatBytes(summary?.totalDownloadBytes ?: 0)} download · " +
                            "${formatBytes(summary?.totalInstalledBytes ?: 0)} installed · reserve ${formatBytes(summary?.reserveBytes ?: 0)} kept free.",
                    )
                    Text("Licenses: ${summary?.licenses?.joinToString()}")
                    Text("Optional components (including the local LLM) are NOT part of this pack.")
                    Text("You can pause, cancel or remove each component afterwards.")
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    packConfirmOpen = false
                    val p = computedPlan
                    val resources = graph.deviceResourceReader.read()
                    packRunning = true
                    packEvents = listOf("Pack install started (${p.includedItems.size} components).")
                    scope.launch {
                        val result = graph.modelPackInstaller.installPack(p, resources) { event ->
                            packEvents = packEvents + event
                        }
                        statusNote = if (result.success) {
                            "Recommended voice pack INSTALLED — pack receipt ${result.packReceiptId}."
                        } else {
                            "Voice pack stopped truthfully: " + result.outcomes.lastOrNull()?.reason.orEmpty()
                        }
                        packRunning = false
                        registryTick++
                    }
                }) { Text("Install") }
            },
            dismissButton = {
                TextButton(onClick = { packConfirmOpen = false }) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun AttributeLine(label: String, value: String) {
    val c = hermesColors
    Text(
        "$label: $value",
        style = HermesType.bodySecondary,
        color = c.inkSecondary,
    )
}

internal const val PROBE_IDENTITY = "org.hermestower.test.probe-artifact"
