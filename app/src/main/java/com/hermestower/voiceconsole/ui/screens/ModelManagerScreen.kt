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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hermestower.voiceconsole.app.AppGraph
import com.hermestower.voiceconsole.R
import com.hermestower.voiceconsole.models.ComponentFallback
import com.hermestower.voiceconsole.models.DeterministicTestArtifact
import com.hermestower.voiceconsole.models.ModelInstallState
import com.hermestower.voiceconsole.models.ModelLinks
import com.hermestower.voiceconsole.models.ModelSha256
import com.hermestower.voiceconsole.models.VoiceModelProfile
import android.content.Intent
import android.net.Uri
import com.hermestower.voiceconsole.towersim.LocalTowerMockServer
import com.hermestower.voiceconsole.ui.components.SectionCard
import com.hermestower.voiceconsole.ui.components.MeanderBand
import com.hermestower.voiceconsole.ui.theme.HermesType
import com.hermestower.voiceconsole.ui.theme.hermesColors
import kotlinx.coroutines.launch

internal fun formatBytes(bytes: Long): String = when {
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
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
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
        MeanderBand(colors = hermesColors, modifier = Modifier.padding(top = 6.dp))
        Text(stringResource(R.string.mm_subtitle), style = HermesType.sectionLabel, color = c.bronze)
        Text(
            stringResource(R.string.mm_intro),
            style = HermesType.bodySecondary,
            color = c.inkSecondary,
        )

        // ---------- A. Recommended setup ----------
        SectionCard(title = stringResource(R.string.mm_section_a)) {
            val resources = remember { graph.deviceResourceReader.read() }
            Text(context.getString(R.string.mm_detected_device, resources.deviceModel, resources.androidVersion), style = HermesType.body, color = c.ink)
            Text(
                context.getString(
                    R.string.mm_resources_line,
                    resources.supportedAbis.joinToString(),
                    formatBytes(resources.availableRamBytes),
                    formatBytes(resources.freeStorageBytes),
                ),
                style = HermesType.body, color = c.ink,
            )
            Text(
                context.getString(
                    R.string.mm_tts_engines,
                    if (resources.ttsEnginePackages.isEmpty()) context.getString(R.string.mm_tts_none) else resources.ttsEnginePackages.joinToString(),
                ),
                style = HermesType.body, color = c.ink,
            )
            Text(
                context.getString(
                    R.string.mm_network,
                    if (resources.networkOnline) context.getString(R.string.mm_network_online) else context.getString(R.string.mm_network_offline),
                    if (resources.networkMetered) context.getString(R.string.mm_network_metered) else "",
                ),
                style = HermesType.body, color = c.ink,
            )
            Text(context.getString(R.string.mm_current_profile, appliedProfile.displayLabel), style = HermesType.body, color = c.ink)
            Text(context.getString(R.string.mm_recommended_profile, selectedProfile.displayLabel), style = HermesType.body, color = c.ink)
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
                Text(context.getString(R.string.mm_compat_summary, rec.routeSummary), style = HermesType.body, color = c.ink)
                Text(
                    context.getString(
                        R.string.mm_expected_footprint,
                        formatBytes(rec.expectedDownloadBytes),
                        formatBytes(rec.expectedInstalledBytes),
                        formatBytes(rec.ramEstimateLowBytes),
                        formatBytes(rec.ramEstimateHighBytes),
                    ),
                    style = HermesType.body, color = c.ink,
                )
                rec.warnings.forEach { Text("⚠ $it", style = HermesType.bodySecondary, color = c.warn) }
                Button(onClick = {
                    graph.keyValueStore.putString("voice_profile", rec.profile)
                    appliedProfile = VoiceModelProfile.entries.first { it.name == rec.profile }
                    statusNote = context.getString(R.string.mm_applied_note, rec.profile)
                }) { Text(stringResource(R.string.mm_apply_recommended)) }
            } else {
                Text(stringResource(R.string.mm_select_mode_hint), style = HermesType.bodySecondary, color = c.inkSecondary)
            }
        }

        // ---------- RECOMMENDED VOICE PACK (GRAPH 4 §6 bulk action) ----------
        SectionCard(title = stringResource(R.string.mm_section_pack)) {
            val plan = computedPlan
            val summary = computedSummary
            val included = plan.includedItems

            Text(context.getString(R.string.mm_pack_header, VoiceModelProfile.HYBRID_BALANCED.displayLabel), style = HermesType.sectionLabel, color = c.bronze)
            if (included.isEmpty()) {
                Text(
                    stringResource(R.string.mm_pack_nothing_needed),
                    style = HermesType.bodySecondary, color = c.inkSecondary,
                )
            } else {
                Text(context.getString(R.string.mm_will_install, included.size), style = HermesType.body, color = c.ink)
                included.forEach { item ->
                    Text(
                        context.getString(
                            R.string.mm_pack_item,
                            item.entry?.displayName ?: "",
                            item.entry?.version ?: "",
                            formatBytes(item.entry?.downloadSizeBytes ?: 0),
                        ),
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
                context.getString(
                    R.string.mm_download_total,
                    formatBytes(summary.totalDownloadBytes),
                    formatBytes(summary.totalInstalledBytes),
                    formatBytes(summary.peakTempBytes),
                ),
                style = HermesType.body, color = c.ink,
            )
            Text(
                context.getString(
                    R.string.mm_reserve_line,
                    formatBytes(summary.reserveBytes),
                    formatBytes(summary.minimumFreeRequiredBytes),
                ),
                style = HermesType.body, color = c.ink,
            )
            Text(context.getString(R.string.mm_expected_duration, summary.durationRangeText), style = HermesType.body, color = c.ink)
            Text(summary.wifiAdvice, style = HermesType.bodySecondary, color = c.inkSecondary)
            Text(summary.chargingAdvice, style = HermesType.bodySecondary, color = c.inkSecondary)
            Text(summary.thermalAdvice, style = HermesType.bodySecondary, color = c.inkSecondary)
            Text(context.getString(R.string.mm_licenses, summary.licenses.joinToString()), style = HermesType.bodySecondary, color = c.inkSecondary)
            if (summary.optionalItems.isNotEmpty()) {
                Text(
                    context.getString(R.string.mm_optional_note, summary.optionalItems.joinToString()),
                    style = HermesType.bodySecondary, color = c.inkSecondary,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 4.dp)) {
                Button(
                    enabled = !packRunning && included.isNotEmpty(),
                    onClick = { packConfirmOpen = true },
                ) { Text(stringResource(R.string.mm_install_pack)) }
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
        SectionCard(title = stringResource(R.string.mm_section_b)) {
            val installed = graph.modelRegistry.snapshot().filter { it.state != "REMOVED" }
            if (installed.isEmpty()) {
                Text(
                    stringResource(R.string.mm_nothing_installed),
                    style = HermesType.bodySecondary, color = c.inkSecondary,
                )
            }
            com.hermestower.voiceconsole.models.ModelComponentType.entries.forEach { type ->
                val records = installed.filter { it.componentType == type.name }
                if (records.isNotEmpty()) {
                    Text(type.displayLabel, style = HermesType.sectionLabel, color = c.bronze)
                    records.forEach { record ->
                        Text(
                            context.getString(
                                R.string.mm_component_line,
                                record.displayName, record.version, record.state,
                                if (record.active) context.getString(R.string.mm_active_suffix) else "",
                            ),
                            style = HermesType.body, color = c.ink,
                        )
                        Text(
                            context.getString(
                                R.string.mm_verified_line,
                                record.lastVerificationAt ?: context.getString(R.string.mm_never),
                                record.lastQualificationAt ?: context.getString(R.string.mm_never),
                            ),
                            style = HermesType.bodySecondary, color = c.inkSecondary,
                        )
                    }
                }
            }
        }

        // ---------- C. Available components ----------
        SectionCard(title = stringResource(R.string.mm_section_c)) {
            graph.modelCatalog.all().forEach { entry ->
                val progress = progressById[entry.identity]?.value
                val record = graph.modelRegistry.record(entry.identity)
                val isExpanded = expanded[entry.identity] == true
                val updateAvailable = record != null && record.version != entry.version

                Text(entry.displayName, style = HermesType.sectionLabel, color = c.bronze)
                Text(
                    context.getString(
                        R.string.mm_spec_line,
                        entry.identity, entry.version, entry.provider, entry.license,
                        formatBytes(entry.downloadSizeBytes), formatBytes(entry.installedSizeBytes),
                        formatBytes(entry.ramEstimateBytes), entry.latencyClass,
                        entry.languages.joinToString(),
                        if (entry.italianSupported) context.getString(R.string.mm_it_yes) else context.getString(R.string.mm_it_no),
                    ),
                    style = HermesType.bodySecondary, color = c.inkSecondary,
                )
                Text(
                    context.getString(
                        R.string.mm_classification,
                        entry.entryClassification,
                        if (entry.optionalComponent) context.getString(R.string.mm_optional_suffix) else context.getString(R.string.mm_required_suffix),
                        record?.state ?: entry.state,
                        if (record?.active == true) context.getString(R.string.mm_active_suffix) else "",
                    ),
                    style = HermesType.bodySecondary, color = c.inkSecondary,
                )
                if (updateAvailable) {
                    Text(
                        context.getString(R.string.mm_update_available, record?.version ?: "", entry.version),
                        style = HermesType.body, color = c.energy,
                    )
                }
                val rec = recommendation
                val resolved = rec?.components?.firstOrNull { it.identity == entry.identity }
                Text(
                    context.getString(
                        R.string.mm_compat_line,
                        (resolved?.compatible ?: false).toString(),
                        resolved?.incompatibilityReason?.let { context.getString(R.string.mm_incompat_paren, it) } ?: "",
                    ),
                    style = HermesType.bodySecondary, color = c.inkSecondary,
                )
                progress?.let {
                    if (it.state == ModelInstallState.DOWNLOADING || it.state == ModelInstallState.DOWNLOAD_PAUSED) {
                        Text(
                            context.getString(
                                R.string.mm_download_progress,
                                it.state.displayLabel, it.bytesDownloaded, it.totalBytes, (it.fraction * 100).toInt(),
                            ),
                            style = HermesType.body, color = c.ink,
                        )
                    }
                    it.error?.let { err -> Text(context.getString(R.string.mm_last_download_event, err), style = HermesType.bodySecondary, color = c.warn) }
                }

                if (isExpanded) {
                    // ---- Full attribute sheet (GRAPH 4 §6, all fields) ----
                    AttributeLine(stringResource(R.string.mm_attr_type), entry.componentTypeEnum().displayLabel)
                    AttributeLine(stringResource(R.string.mm_attr_version), entry.version)
                    AttributeLine(stringResource(R.string.mm_attr_provider), entry.provider)
                    AttributeLine(stringResource(R.string.mm_attr_license), entry.license + (entry.licenseUrl?.let { " ($it)" } ?: ""))
                    AttributeLine(stringResource(R.string.mm_attr_install_status), entry.takeIf { it.entryClassification == "RUNTIME_BUNDLED" }?.let { context.getString(R.string.mm_included_label) } ?: (record?.let { "${it.state} (v${it.version})" } ?: context.getString(R.string.mm_not_installed)))
                    AttributeLine(stringResource(R.string.mm_attr_qualification), record?.lastQualificationAt?.let { context.getString(R.string.mm_qualified_at, it) } ?: entry.state)
                    AttributeLine(stringResource(R.string.mm_attr_download_size), formatBytes(entry.downloadSizeBytes))
                    AttributeLine(stringResource(R.string.mm_attr_installed_size), formatBytes(entry.installedSizeBytes))
                    AttributeLine(stringResource(R.string.mm_attr_ram), formatBytes(entry.ramEstimateBytes))
                    AttributeLine(stringResource(R.string.mm_attr_usage), context.getString(R.string.mm_usage_value, entry.latencyClass))
                    AttributeLine(stringResource(R.string.mm_attr_italian), if (entry.italianSupported) context.getString(R.string.mm_italian_yes) else context.getString(R.string.mm_italian_no, entry.languages.joinToString()))
                    AttributeLine(stringResource(R.string.mm_attr_format), entry.format)
                    AttributeLine(stringResource(R.string.mm_attr_quant), entry.quantization)
                    AttributeLine(stringResource(R.string.mm_attr_storage), entry.storageLocation)
                    AttributeLine(stringResource(R.string.mm_attr_updates), if (updateAvailable) context.getString(R.string.mm_update_both, record?.version ?: "", entry.version) else context.getString(R.string.mm_update_current))
                    AttributeLine(stringResource(R.string.mm_attr_active_state), if (record?.active == true) "ACTIVE" else context.getString(R.string.mm_inactive))
                    AttributeLine(stringResource(R.string.mm_attr_fallback), ComponentFallback.forType(entry.componentType))
                    AttributeLine(stringResource(R.string.mm_attr_upstream), entry.upstreamRepo ?: context.getString(R.string.mm_na))
                    AttributeLine(stringResource(R.string.mm_attr_revision), entry.revisionBinding ?: context.getString(R.string.mm_na))
                    AttributeLine(stringResource(R.string.mm_attr_runtime_req), entry.runtimeRequirement ?: context.getString(R.string.mm_na))
                    AttributeLine(stringResource(R.string.mm_attr_provenance), entry.provenanceNote ?: context.getString(R.string.mm_provenance_unverified))
                    AttributeLine(stringResource(R.string.mm_attr_sha256), entry.sha256)
                    entry.knownLimitations.forEach { Text("• $it", style = HermesType.bodySecondary, color = c.inkSecondary) }

                    // ---- GRAPH 4 FINAL §12: REAL, physically clickable links. ----
                    // The open action validates first (directive pre-conditions);
                    // a blocked or un-openable link NEVER substitutes a source —
                    // it shows the localized explanation and offers the retry.
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        TextButton(onClick = {
                            val check = ModelLinks.validateForOpen(entry.sourceUri)
                            if (!check.ok) {
                                statusNote = context.getString(
                                    R.string.mm_link_blocked_fmt,
                                    context.getString(R.string.mm_open_source),
                                    check.reason ?: "?",
                                )
                            } else {
                                runCatching {
                                    context.startActivity(
                                        Intent(Intent.ACTION_VIEW, Uri.parse(entry.sourceUri)),
                                    )
                                    statusNote = context.getString(R.string.mm_link_opened, entry.sourceUri)
                                }.onFailure {
                                    statusNote = context.getString(
                                        R.string.mm_link_blocked_fmt,
                                        context.getString(R.string.mm_open_source),
                                        context.getString(R.string.mm_link_no_browser),
                                    )
                                }
                            }
                        }) { Text(stringResource(R.string.mm_open_source)) }
                        TextButton(onClick = {
                            val check = ModelLinks.licenseCheck(entry)
                            if (!check.ok) {
                                statusNote = context.getString(
                                    R.string.mm_link_blocked_fmt,
                                    context.getString(R.string.mm_open_license),
                                    check.reason ?: "?",
                                )
                            } else {
                                val url = entry.licenseUrl!!
                                runCatching {
                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                                    statusNote = context.getString(R.string.mm_link_opened, url)
                                }.onFailure {
                                    statusNote = context.getString(
                                        R.string.mm_link_blocked_fmt,
                                        context.getString(R.string.mm_open_license),
                                        context.getString(R.string.mm_link_no_browser),
                                    )
                                }
                            }
                        }) { Text(stringResource(R.string.mm_open_license)) }
                        TextButton(onClick = {
                            clipboard.setText(AnnotatedString(entry.identity))
                            statusNote = context.getString(R.string.mm_copied_identity, entry.identity)
                        }) { Text(stringResource(R.string.mm_copy_identity)) }
                        TextButton(onClick = {
                            // Retry source check — re-runs the directive §12
                            // pre-conditions and reports the result honestly.
                            val check = ModelLinks.validateForOpen(entry.sourceUri)
                            statusNote = if (check.ok) {
                                context.getString(R.string.mm_link_opened, entry.sourceUri)
                            } else {
                                context.getString(
                                    R.string.mm_link_blocked_fmt,
                                    context.getString(R.string.mm_retry_source_check),
                                    check.reason ?: "?",
                                )
                            }
                        }) { Text(stringResource(R.string.mm_retry_source_check)) }
                    }
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
                                statusNote = context.getString(R.string.mm_probe_started, server.port)
                            } else {
                                graph.modelDownloads.start(entry, entry.sourceUri)
                                statusNote = context.getString(R.string.mm_download_started, entry.sourceUri)
                            }
                            registryTick++
                        },
                    ) { Text(stringResource(R.string.mm_download)) }
                    TextButton(onClick = { graph.modelDownloads.pause(entry.identity); statusNote = context.getString(R.string.mm_download_paused, entry.identity) }) { Text(stringResource(R.string.mm_pause)) }
                    TextButton(
                        onClick = {
                            if (entry.identity == PROBE_IDENTITY) {
                                val port = graph.keyValueStore.getString("probe_server_port")?.toIntOrNull()
                                if (port != null) {
                                    val bytes = DeterministicTestArtifact.generate("hermes-test-artifact-v1", 4096)
                                    graph.modelDownloads.resume(entry.copy(sha256 = ModelSha256.ofBytes(bytes)), "http://127.0.0.1:$port/artifacts/hermes-test-artifact-v1")
                                    statusNote = context.getString(R.string.mm_download_resumed, entry.identity)
                                } else {
                                    statusNote = context.getString(R.string.mm_no_resume_server)
                                }
                            } else {
                                graph.modelDownloads.resume(entry, entry.sourceUri)
                                statusNote = context.getString(R.string.mm_download_resumed_source, entry.identity)
                            }
                        },
                    ) { Text(stringResource(R.string.mm_resume)) }
                    TextButton(onClick = { graph.modelDownloads.cancel(entry.identity); statusNote = context.getString(R.string.mm_download_cancelled, entry.identity) }) { Text(stringResource(R.string.mm_cancel)) }
                    TextButton(onClick = {
                        statusNote = if (graph.modelDownloads.verifiedExisting(entry)) context.getString(R.string.mm_hash_verified)
                        else context.getString(R.string.mm_no_verified_file, entry.identity)
                    }) { Text(stringResource(R.string.mm_verify)) }
                    TextButton(onClick = { expanded[entry.identity] = !isExpanded }) { Text(if (isExpanded) stringResource(R.string.mm_hide_details) else stringResource(R.string.mm_details)) }
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
                                    statusNote = context.getString(R.string.mm_already_verified, if (q.qualified) "PASSED" else "FAILED") +
                                        (a?.let { context.getString(R.string.mm_activation_suffix, if (it.success) "ACTIVE" else "REFUSED: ${it.reason}") } ?: "")
                                    registryTick++
                                } else {
                                    val outcome = graph.modelPackInstaller.installComponent(entry, resources) { statusNote = it }
                                    statusNote = context.getString(R.string.mm_install_result, entry.displayName, if (outcome.success) "COMPLETED" else "FAILED", outcome.reason)
                                    registryTick++
                                }
                            }
                        },
                    ) { Text(stringResource(R.string.mm_install)) }
                    TextButton(onClick = {
                        val result = graph.modelQualification.qualify(entry)
                        statusNote = context.getString(R.string.mm_qualify_result, entry.identity, if (result.qualified) "PASSED" else "FAILED", result.checks.joinToString { "${it.name}=${it.passed}" })
                    }) { Text(stringResource(R.string.mm_qualify)) }
                    TextButton(onClick = {
                        val result = graph.modelActivation.activate(entry)
                        statusNote = context.getString(R.string.mm_activate_result, entry.identity, if (result.success) "ACTIVE" else "REFUSED", result.reason)
                        registryTick++
                    }) { Text(stringResource(R.string.mm_activate)) }
                    TextButton(onClick = {
                        val result = graph.modelActivation.deactivate(entry)
                        statusNote = context.getString(R.string.mm_deactivate_result, entry.identity, result.reason)
                        registryTick++
                    }) { Text(stringResource(R.string.mm_deactivate)) }
                    TextButton(onClick = {
                        val result = graph.modelRollback.rollback(entry)
                        statusNote = context.getString(R.string.mm_rollback_result, entry.identity, if (result.success) context.getString(R.string.mm_rollback_restored, result.restoredVersion) else result.reason)
                        registryTick++
                    }) { Text(stringResource(R.string.mm_rollback)) }
                    TextButton(onClick = {
                        val removed = graph.modelRollback.removalCleanup(entry)
                        statusNote = if (removed) context.getString(R.string.mm_removed, entry.identity) else context.getString(R.string.mm_nothing_to_remove, entry.identity)
                        registryTick++
                    }) { Text(stringResource(R.string.mm_remove)) }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(
                        enabled = entry.entryClassification == "VERIFIED_DOWNLOADABLE" && entry.identity != PROBE_IDENTITY && !packRunning,
                        onClick = {
                            graph.modelDownloads.cancel(entry.identity)
                            val removed = graph.modelRollback.removalCleanup(entry)
                            graph.modelDownloads.start(entry, entry.sourceUri)
                            statusNote = context.getString(R.string.mm_redownload_note, entry.displayName, if (removed) context.getString(R.string.mm_removed_word) else context.getString(R.string.mm_absent_word))
                            registryTick++
                        },
                    ) { Text(stringResource(R.string.mm_redownload)) }
                    TextButton(
                        enabled = entry.identity != PROBE_IDENTITY && !packRunning,
                        onClick = {
                            if (graph.modelDownloads.verifiedExisting(entry)) {
                                statusNote = context.getString(R.string.mm_repair_ok, entry.displayName)
                            } else {
                                graph.modelDownloads.cancel(entry.identity)
                                graph.modelDownloads.start(entry, entry.sourceUri)
                                statusNote = context.getString(R.string.mm_repair_needed, entry.displayName)
                            }
                            registryTick++
                        },
                    ) { Text(stringResource(R.string.mm_repair)) }
                    TextButton(
                        enabled = updateAvailable && !packRunning,
                        onClick = {
                            graph.modelDownloads.start(entry, entry.sourceUri)
                            statusNote = context.getString(R.string.mm_update_note, entry.displayName, record?.version ?: "", entry.version)
                            registryTick++
                        },
                    ) { Text(stringResource(R.string.mm_update)) }
                    TextButton(onClick = { diagnosticsOpen.value = !diagnosticsOpen.value }) { Text(stringResource(R.string.mm_open_diagnostics)) }
                }
            }

            if (diagnosticsOpen.value) {
                Text(stringResource(R.string.mm_diag_roots), style = HermesType.sectionLabel, color = c.bronze)
                graph.modelStorage.rootsUsage().forEach { (root, bytes) ->
                    Text(context.getString(R.string.mm_root_line, root, formatBytes(bytes)), style = HermesType.bodySecondary, color = c.inkSecondary)
                }
                val receipts = com.hermestower.voiceconsole.models.ReceiptWriter.listReceipts(graph.modelStorage.receiptsDir())
                Text(
                    context.getString(
                        R.string.mm_receipts,
                        if (receipts.isEmpty()) context.getString(R.string.mm_receipts_none)
                        else context.getString(R.string.mm_receipts_some, receipts.size, receipts.last()),
                    ),
                    style = HermesType.bodySecondary, color = c.inkSecondary,
                )
            }
        }

        // ---------- CP-G4-009: bundled verified delivery (no network) ----------
        SectionCard(title = stringResource(R.string.mm_bundled_title)) {
            var bundledReport by remember { mutableStateOf<List<String>>(emptyList()) }
            var bundledRunning by remember { mutableStateOf(false) }
            Text(
                stringResource(R.string.mm_bundled_explainer),
                style = HermesType.bodySecondary, color = c.inkSecondary,
            )
            Button(
                enabled = !bundledRunning,
                onClick = {
                    bundledRunning = true
                    bundledReport = listOf(context.getString(R.string.mm_bundled_started))
                    scope.launch {
                        val report = graph.bundledModelInstaller.deliverAll()
                        bundledReport = report.results.map { result ->
                            when (result) {
                                is com.hermestower.voiceconsole.models.BundledModelInstaller.BundledInstallResult.Installed ->
                                    context.getString(R.string.mm_bundled_installed, result.artifact.asset, result.qualificationSummary, result.receiptId)
                                is com.hermestower.voiceconsole.models.BundledModelInstaller.BundledInstallResult.AlreadyInstalled ->
                                    context.getString(R.string.mm_bundled_already, result.artifact.asset)
                                is com.hermestower.voiceconsole.models.BundledModelInstaller.BundledInstallResult.Refused ->
                                    context.getString(R.string.mm_bundled_refused, result.artifact?.asset ?: "manifest", result.kind, result.detail)
                            }
                        } + if (report.stoppedAt != null) context.getString(R.string.mm_bundled_stopped, report.stoppedAt) else context.getString(R.string.mm_bundled_done, report.installedCount)
                        registryTick++
                        bundledRunning = false
                    }
                },
            ) { Text(if (bundledRunning) stringResource(R.string.mm_delivering) else stringResource(R.string.mm_install_bundled)) }
            bundledReport.forEach { Text(it, style = HermesType.bodySecondary, color = c.inkSecondary) }
        }

        // ---------- E. Modes ----------
        SectionCard(title = stringResource(R.string.mm_section_e)) {
            Text(
                stringResource(R.string.mm_modes_text),
                style = HermesType.body, color = c.ink,
            )
        }

        // ---------- F. Tower-only fallback ----------
        SectionCard(title = stringResource(R.string.mm_section_f)) {
            Text(
                stringResource(R.string.mm_fallback_text),
                style = HermesType.body, color = c.ink,
            )
        }

        statusNote?.let {
            Text(context.getString(R.string.mm_status_prefix, it), style = HermesType.body, color = c.energy)
        }
    }

    // ---------- Pack confirmation (explicit, with the full summary) ----------
    if (packConfirmOpen) {
        val summary = computedSummary
        val plan = computedPlan
        AlertDialog(
            onDismissRequest = { packConfirmOpen = false },
            title = { Text(stringResource(R.string.mm_pack_confirm_title)) },
            text = {
                Column {
                    Text(
                        context.getString(
                            R.string.mm_pack_confirm_summary,
                            plan?.includedItems?.size ?: 0,
                            formatBytes(summary?.totalDownloadBytes ?: 0),
                            formatBytes(summary?.totalInstalledBytes ?: 0),
                            formatBytes(summary?.reserveBytes ?: 0),
                        ),
                    )
                    Text(context.getString(R.string.mm_licenses, summary?.licenses?.joinToString() ?: ""))
                    Text(stringResource(R.string.mm_pack_confirm_optional))
                    Text(stringResource(R.string.mm_pack_confirm_pause))
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    packConfirmOpen = false
                    val p = computedPlan
                    val resources = graph.deviceResourceReader.read()
                    packRunning = true
                    packEvents = listOf(context.getString(R.string.mm_pack_started, p.includedItems.size))
                    scope.launch {
                        val result = graph.modelPackInstaller.installPack(p, resources) { event ->
                            packEvents = packEvents + event
                        }
                        statusNote = if (result.success) {
                            context.getString(R.string.mm_pack_installed, result.packReceiptId)
                        } else {
                            context.getString(R.string.mm_pack_stopped, result.outcomes.lastOrNull()?.reason.orEmpty())
                        }
                        packRunning = false
                        registryTick++
                    }
                }) { Text(stringResource(R.string.mm_install)) }
            },
            dismissButton = {
                TextButton(onClick = { packConfirmOpen = false }) { Text(stringResource(R.string.mm_cancel)) }
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
