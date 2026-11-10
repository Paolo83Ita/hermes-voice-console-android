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
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import com.hermestower.voiceconsole.ui.components.ResponsiveScreenHeader
import com.hermestower.voiceconsole.ui.components.SectionCard
import com.hermestower.voiceconsole.ui.components.AdaptiveAction
import com.hermestower.voiceconsole.ui.components.AdaptiveActionsRow
import com.hermestower.voiceconsole.ui.components.MeanderBand
import com.hermestower.voiceconsole.ui.theme.HermesType
import com.hermestower.voiceconsole.ui.theme.hermesColors
import kotlinx.coroutines.launch
import com.hermestower.voiceconsole.ui.components.HermesFilledButton
import com.hermestower.voiceconsole.ui.components.HermesFilterChip

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
    var registryTick by remember { mutableIntStateOf(0) }
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
        ResponsiveScreenHeader(title = "MODEL SANCTUM")
        MeanderBand(colors = hermesColors, modifier = Modifier.padding(top = 6.dp))
        Text(stringResource(R.string.mm_subtitle), style = HermesType.sectionLabel, color = c.bronze)
        Text(
            stringResource(R.string.mm_intro),
            style = HermesType.bodySecondary,
            color = c.textSecondary,
        )

        // ---------- A. Recommended configuration (GRAPH 4.13 §14: compact
        // two-column key-value rows; §25: device identifier stays in Advanced
        // Diagnostics, the ordinary card shows the device CLASS) ----------
        run {
            val resources = remember { graph.deviceResourceReader.read() }
            // GRAPH 4.14 §25: the Local-experimental profile is selectable when
            // the runtime is INTEGRATED (llama.cpp in-tree, CP-G4.14-005) —
            // model delivery/activation is the capability ladder's job, not the
            // selector's. Data-driven: if a future build ships without a verified
            // runtime, the selector disables itself again with the honest note.
            val localLlmAvailable = remember(registryTick) {
                graph.localLlmEngine.hasVerifiedRuntime
            }
            RecommendedConfigurationCard(
                deviceClass = com.hermestower.voiceconsole.models.DeviceClassLabel.resolve(resources.deviceModel),
                androidVersion = resources.androidVersion.toString(),
                architecture = resources.supportedAbis.joinToString(),
                availableRam = formatBytes(resources.availableRamBytes),
                freeStorage = formatBytes(resources.freeStorageBytes),
                speechEngine = if (resources.ttsEnginePackages.isEmpty()) context.getString(R.string.mm_tts_none) else resources.ttsEnginePackages.first(),
                network = if (resources.networkOnline) context.getString(R.string.mm_network_online) else context.getString(R.string.mm_network_offline),
                currentProfile = appliedProfile.displayLabel,
                recommendedProfile = selectedProfile.displayLabel,
            )
            // ---------- §15/§16 vertical segmented profile selector ----------
            ProfileSelector(
                cards = listOf(
                    ProfileCardData(
                        profile = VoiceModelProfile.TOWER_FIRST,
                        title = stringResource(R.string.mm_profile_tower_first_title),
                        body = stringResource(R.string.mm_profile_tower_first_body),
                        requirements = stringResource(R.string.mm_profile_req_tower),
                        selected = selectedProfile == VoiceModelProfile.TOWER_FIRST,
                        enabled = true,
                    ),
                    ProfileCardData(
                        profile = VoiceModelProfile.HYBRID_BALANCED,
                        title = stringResource(R.string.mm_profile_hybrid_title),
                        body = stringResource(R.string.mm_profile_hybrid_body),
                        requirements = stringResource(R.string.mm_profile_req_hybrid),
                        selected = selectedProfile == VoiceModelProfile.HYBRID_BALANCED,
                        enabled = true,
                    ),
                    ProfileCardData(
                        profile = VoiceModelProfile.LOCAL_EXPERIMENTAL,
                        title = stringResource(R.string.mm_profile_local_title),
                        body = stringResource(R.string.mm_profile_local_body),
                        requirements = stringResource(R.string.mm_profile_req_local),
                        selected = selectedProfile == VoiceModelProfile.LOCAL_EXPERIMENTAL,
                        enabled = localLlmAvailable,
                        unavailableExplanation = if (!localLlmAvailable) stringResource(R.string.mm_profile_local_unavailable) else null,
                    ),
                    ProfileCardData(
                        profile = VoiceModelProfile.MANUAL_ADVANCED,
                        title = stringResource(R.string.mm_profile_advanced_title),
                        body = stringResource(R.string.mm_profile_advanced_body),
                        requirements = stringResource(R.string.mm_profile_req_hybrid),
                        selected = selectedProfile == VoiceModelProfile.MANUAL_ADVANCED,
                        enabled = true,
                    ),
                ),
                onSelect = { profile ->
                    selectedProfile = profile
                    recommendation = graph.modelResolver.recommend(profile, resources, graph.modelCatalog)
                },
            )
            if (!localLlmAvailable && selectedProfile == VoiceModelProfile.LOCAL_EXPERIMENTAL) {
                // §16: "Why unavailable" — a readable explanation, never dim-only
                Text(stringResource(R.string.mm_profile_why_unavailable), style = HermesType.sectionLabel, color = c.textWarning)
                Text(stringResource(R.string.mm_profile_local_unavailable), style = HermesType.bodySecondary, color = c.textSecondary)
            }
            val rec = recommendation
            if (rec != null) {
                Text(context.getString(R.string.mm_compat_summary, rec.routeSummary), style = HermesType.body, color = c.textPrimary)
                Text(
                    context.getString(
                        R.string.mm_expected_footprint,
                        formatBytes(rec.expectedDownloadBytes),
                        formatBytes(rec.expectedInstalledBytes),
                        formatBytes(rec.ramEstimateLowBytes),
                        formatBytes(rec.ramEstimateHighBytes),
                    ),
                    style = HermesType.body, color = c.textPrimary,
                )
                rec.warnings.forEach { Text("⚠ $it", style = HermesType.bodySecondary, color = c.textWarning) }
                HermesFilledButton(onClick = {
                    graph.keyValueStore.putString("voice_profile", rec.profile)
                    appliedProfile = VoiceModelProfile.entries.first { it.name == rec.profile }
                    statusNote = context.getString(R.string.mm_applied_note, rec.profile)
                }) { Text(stringResource(R.string.mm_apply_recommended)) }
            } else {
                Text(stringResource(R.string.mm_select_mode_hint), style = HermesType.bodySecondary, color = c.textSecondary)
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
                    style = HermesType.bodySecondary, color = c.textSecondary,
                )
            } else {
                Text(context.getString(R.string.mm_will_install, included.size), style = HermesType.body, color = c.textPrimary)
                included.forEach { item ->
                    Text(
                        context.getString(
                            R.string.mm_pack_item,
                            item.entry?.displayName ?: "",
                            item.entry?.version ?: "",
                            formatBytes(item.entry?.downloadSizeBytes ?: 0),
                        ),
                        style = HermesType.body, color = c.textPrimary,
                    )
                }
            }
            plan.items.filter { it.role == "BUNDLED_RUNTIME" }.forEach {
                Text("• ${it.entry?.displayName} — ${it.reason}", style = HermesType.bodySecondary, color = c.textSecondary)
            }
            plan.items.filter { it.role == "ALREADY_INSTALLED" }.forEach {
                Text("• ${it.entry?.displayName} — ${it.reason}", style = HermesType.bodySecondary, color = c.textSecondary)
            }
            summary.blockedItems.forEach { (name, reason) ->
                Text("⚠ $name — $reason", style = HermesType.bodySecondary, color = c.textWarning)
            }
            Text(
                context.getString(
                    R.string.mm_download_total,
                    formatBytes(summary.totalDownloadBytes),
                    formatBytes(summary.totalInstalledBytes),
                    formatBytes(summary.peakTempBytes),
                ),
                style = HermesType.body, color = c.textPrimary,
            )
            Text(
                context.getString(
                    R.string.mm_reserve_line,
                    formatBytes(summary.reserveBytes),
                    formatBytes(summary.minimumFreeRequiredBytes),
                ),
                style = HermesType.body, color = c.textPrimary,
            )
            Text(context.getString(R.string.mm_expected_duration, summary.durationRangeText), style = HermesType.body, color = c.textPrimary)
            Text(summary.wifiAdvice, style = HermesType.bodySecondary, color = c.textSecondary)
            Text(summary.chargingAdvice, style = HermesType.bodySecondary, color = c.textSecondary)
            Text(summary.thermalAdvice, style = HermesType.bodySecondary, color = c.textSecondary)
            Text(context.getString(R.string.mm_licenses, summary.licenses.joinToString()), style = HermesType.bodySecondary, color = c.textSecondary)
            if (summary.optionalItems.isNotEmpty()) {
                Text(
                    context.getString(R.string.mm_optional_note, summary.optionalItems.joinToString()),
                    style = HermesType.bodySecondary, color = c.textSecondary,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 4.dp)) {
                HermesFilledButton(
                    enabled = !packRunning && included.isNotEmpty(),
                    onClick = { packConfirmOpen = true },
                ) { Text(stringResource(R.string.mm_install_pack)) }
            }
            if (packEvents.isNotEmpty()) {
                Column(Modifier.padding(top = 4.dp)) {
                    packEvents.takeLast(4).forEach {
                        Text(it, style = HermesType.bodySecondary, color = c.textInfo)
                    }
                }
            }
        }

        // ---------- F. Installed components (GRAPH 4.13 §13; localized states) ----------
        SectionCard(title = stringResource(R.string.mm_section_f2)) {
            val installed = graph.modelRegistry.snapshot().filter { it.state != "REMOVED" }
            if (installed.isEmpty()) {
                Text(
                    stringResource(R.string.mm_nothing_installed),
                    style = HermesType.bodySecondary, color = c.textSecondary,
                )
            }
            com.hermestower.voiceconsole.models.ModelComponentType.entries.forEach { type ->
                val records = installed.filter { it.componentType == type.name }
                if (records.isNotEmpty()) {
                    Text(type.displayLabel, style = HermesType.sectionLabel, color = c.bronze)
                    records.forEach { record ->
                        // §18: the state renders through StateLabels, never raw
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(record.displayName, style = HermesType.body, color = c.textPrimary, modifier = Modifier.weight(1f, fill = false))
                            ModelStatePill(record.state)
                        }
                        Text(
                            context.getString(
                                R.string.mm_verified_line,
                                record.lastVerificationAt ?: context.getString(R.string.mm_never),
                                record.lastQualificationAt ?: context.getString(R.string.mm_never),
                            ),
                            style = HermesType.bodySecondary, color = c.textSecondary,
                        )
                    }
                }
            }
        }

        // ---------- G. Updates or repairs (GRAPH 4.13 §13) ----------
        SectionCard(title = stringResource(R.string.mm_section_g2)) {
            val all = graph.modelCatalog.all()
            val pending = all.filter { entry ->
                val record = graph.modelRegistry.record(entry.identity)
                val progress = progressById[entry.identity]?.value
                (record != null && record.version != entry.version) ||
                    record?.state in listOf("FAILED", "QUARANTINED", "DOWNLOAD_PAUSED") ||
                    progress?.state in listOf(
                        com.hermestower.voiceconsole.models.ModelInstallState.DOWNLOAD_PAUSED,
                    )
            }
            if (pending.isEmpty()) {
                Text(
                    stringResource(R.string.mm_section_updates_none),
                    style = HermesType.bodySecondary, color = c.textSecondary,
                )
            }
            pending.forEach { entry ->
                val record = graph.modelRegistry.record(entry.identity)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(entry.displayName, style = HermesType.body, color = c.textPrimary, modifier = Modifier.weight(1f, fill = false))
                    ModelStatePill(record?.state ?: entry.state)
                }
                if (record != null && record.version != entry.version) {
                    Text(
                        context.getString(R.string.mm_update_available, record.version, entry.version),
                        style = HermesType.body, color = c.textInfo,
                    )
                }
            }
        }

        // ---------- B–E. Catalog by component family (GRAPH 4.13 §13):
        // Voices / Speech recognition / Wake word and noise reduction /
        // Optional local intelligence — no uninterrupted flat list ----------
        listOf(
            com.hermestower.voiceconsole.models.ModelComponentType.VOICE to R.string.mm_section_b2,
            com.hermestower.voiceconsole.models.ModelComponentType.SPEECH_TO_TEXT to R.string.mm_section_c2,
            com.hermestower.voiceconsole.models.ModelComponentType.WAKE_WORD to R.string.mm_section_d2,
            com.hermestower.voiceconsole.models.ModelComponentType.VAD to R.string.mm_section_d2,
            com.hermestower.voiceconsole.models.ModelComponentType.NOISE_SUPPRESSION to R.string.mm_section_d2,
            com.hermestower.voiceconsole.models.ModelComponentType.LOCAL_LLM to R.string.mm_section_e2,
            com.hermestower.voiceconsole.models.ModelComponentType.TTS_ENGINE to R.string.mm_section_c2,
        ).distinctBy { it.first }.groupBy({ it.second }, { it.first }).forEach { (titleRes, types) ->
            // GRAPH 4.14 §5 (REMOVE decision): the deterministic probe fixture is
            // pipeline-qualification evidence, never an ordinary product card —
            // it stays reachable through the Diagnostics probe flow by identity.
            val familyEntries = graph.modelCatalog.all()
                .filter { it.componentTypeEnum() in types }
                .filter { it.entryClassification != "TEST_FIXTURE_ONLY" }
            if (familyEntries.isEmpty()) return@forEach
            SectionCard(title = stringResource(titleRes)) {
            familyEntries.forEach { entry ->
                val progress = progressById[entry.identity]?.value
                val record = graph.modelRegistry.record(entry.identity)
                val isExpanded = expanded[entry.identity] == true
                val updateAvailable = record != null && record.version != entry.version

                // ---- §17 card header: name + localized state pill ----
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        entry.displayName,
                        style = HermesType.sectionLabel.copy(fontWeight = FontWeight.SemiBold, fontSize = 15.sp),
                        color = c.textPrimary,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    ModelStatePill(record?.state ?: progress?.state?.name ?: entry.state)
                }
                // ---- §13: plain-language purpose (never catalog raw data) ----
                val purposeRes = com.hermestower.voiceconsole.models.ComponentPurpose.stringRes(entry.componentTypeEnum())
                Text(stringResource(purposeRes), style = HermesType.bodySecondary, color = c.textSecondary)
                // ---- §20 collapsed card: size · RAM · language · runtime only ----
                Text(
                    context.getString(
                        R.string.mm_card_size,
                    ) + " ${formatBytes(entry.downloadSizeBytes)}  ·  " +
                        context.getString(R.string.mm_card_ram) + " ${formatBytes(entry.ramEstimateBytes)}  ·  " +
                        context.getString(R.string.mm_card_language) + " " +
                        (if (entry.italianSupported) "IT" else entry.languages.joinToString("/")),
                    style = HermesType.bodySecondary,
                    color = c.textTechnical,
                )
                Text(
                    context.getString(R.string.mm_card_runtime) + ": " + (entry.runtimeRequirement
                        ?: context.getString(R.string.mm_attr_install_status)),
                    style = HermesType.bodySecondary, color = c.textTechnical,
                )
                if (updateAvailable) {
                    Text(
                        context.getString(R.string.mm_update_available, record?.version ?: "", entry.version),
                        style = HermesType.body, color = c.textInfo,
                    )
                }
                // ---- §19 typed compatibility verdict (never "false") ----
                val rec = recommendation
                val resolved = rec?.components?.firstOrNull { it.identity == entry.identity }
                // OWNER PHYSICAL RETEST F-C REPAIR: the verdict consumes the REAL
                // runtime truth (linked native seam per component family), never
                // the label — an integrated runtime with no install record is a
                // NOT_DOWNLOADED component with a DOWNLOAD action, not a
                // "runtime unavailable" dead end.
                val runtimeReady = com.hermestower.voiceconsole.models.ComponentRuntimeAvailability.probe(
                    componentType = entry.componentTypeEnum(),
                    localLlmHasVerifiedRuntime = graph.localLlmEngine.hasVerifiedRuntime,
                )
                val verdict = com.hermestower.voiceconsole.models.CompatVerdictResolver.resolve(
                    entryClassification = entry.entryClassification,
                    recordState = record?.state,
                    active = record?.active,
                    runtimeRequirement = entry.runtimeRequirement,
                    compatible = resolved?.compatible,
                    incompatibilityReason = resolved?.incompatibilityReason,
                    progressState = progress?.state?.name,
                    runtimeAvailable = runtimeReady,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.mm_card_compatibility), style = com.hermestower.voiceconsole.ui.monumental.MonumentalType.sealSub, color = c.textTertiaryReadable)
                    Text(
                        compatVerdictLabel(verdict),
                        style = HermesType.bodySecondary,
                        color = compatVerdictColor(verdict, c),
                    )
                }
                progress?.let {
                    if (it.state == ModelInstallState.DOWNLOADING || it.state == ModelInstallState.DOWNLOAD_PAUSED) {
                        Text(
                            context.getString(
                                R.string.mm_download_progress,
                                it.state.displayLabel, it.bytesDownloaded, it.totalBytes, (it.fraction * 100).toInt(),
                            ),
                            style = HermesType.body, color = c.textPrimary,
                        )
                    }
                    it.error?.let { err -> Text(context.getString(R.string.mm_last_download_event, err), style = HermesType.bodySecondary, color = c.textWarning) }
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
                    entry.knownLimitations.forEach { Text("• $it", style = HermesType.bodySecondary, color = c.textSecondary) }

                    // ---- GRAPH 4 FINAL §12: REAL, physically clickable links. ----
                    // The open action validates first (directive pre-conditions);
                    // a blocked or un-openable link NEVER substitutes a source —
                    // it shows the localized explanation and offers the retry.
                    // OWNER PHYSICAL RETEST §8: the link row is ALSO adaptive —
                    // the four secondary actions live behind More actions.
                    AdaptiveActionsRow(
                        primary = emptyList(),
                        secondary = listOf(
                            AdaptiveAction(key = "open_source", label = context.getString(R.string.mm_open_source)) {
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
                            },
                            AdaptiveAction(key = "open_license", label = context.getString(R.string.mm_open_license)) {
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
                            },
                            AdaptiveAction(key = "copy_identity", label = context.getString(R.string.mm_copy_identity)) {
                                clipboard.setText(AnnotatedString(entry.identity))
                                statusNote = context.getString(R.string.mm_copied_identity, entry.identity)
                            },
                            AdaptiveAction(key = "retry_source", label = context.getString(R.string.mm_retry_source_check)) {
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
                            },
                        ),
                        moreActionsLabel = context.getString(R.string.mm_more_actions),
                    )
                }

                // ---------- D. Lifecycle actions (§21 action semantics + §22 layout):
                // ONE filled primary action that follows the real component
                // state (ModelActionResolver); Details stays direct (the E2E
                // Gate D walk expands the sheet through it); every other
                // action lives behind "More actions" (never clipped);
                // transfer actions are state-gated, never shown available. ----------
                val uxPlan = com.hermestower.voiceconsole.models.ModelActionResolver.resolve(
                    entryClassification = entry.entryClassification,
                    recordState = record?.state,
                    active = record?.active,
                    progressState = progress?.state?.name,
                    verdict = verdict,
                )
                AdaptiveActionsRow(
                    primary = listOf(
                        AdaptiveAction(
                            key = "primary_${uxPlan.primary.name.lowercase()}",
                            label = primaryActionLabel(uxPlan.primary),
                            emphasized = true,
                            enabled = when (uxPlan.primary) {
                                com.hermestower.voiceconsole.models.ModelPrimaryAction.DOWNLOAD ->
                                    resolved?.downloadAllowed != false &&
                                        entry.entryClassification == "VERIFIED_DOWNLOADABLE" &&
                                        progress?.state != ModelInstallState.DOWNLOADING && !packRunning
                                com.hermestower.voiceconsole.models.ModelPrimaryAction.INSTALL,
                                com.hermestower.voiceconsole.models.ModelPrimaryAction.ACTIVATE,
                                -> !packRunning
                                else -> true
                            },
                        ) {
                            when (uxPlan.primary) {
                                com.hermestower.voiceconsole.models.ModelPrimaryAction.DOWNLOAD -> {
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
                                }
                                com.hermestower.voiceconsole.models.ModelPrimaryAction.VERIFY -> {
                                    statusNote = if (graph.modelDownloads.verifiedExisting(entry)) context.getString(R.string.mm_hash_verified)
                                    else context.getString(R.string.mm_no_verified_file, entry.identity)
                                }
                                com.hermestower.voiceconsole.models.ModelPrimaryAction.INSTALL -> {
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
                                }
                                com.hermestower.voiceconsole.models.ModelPrimaryAction.CHECK_RUNTIME -> {
                                    val result = graph.modelQualification.qualify(entry)
                                    statusNote = context.getString(R.string.mm_qualify_result, entry.identity, if (result.qualified) "PASSED" else "FAILED", result.checks.joinToString { "${it.name}=${it.passed}" })
                                }
                                com.hermestower.voiceconsole.models.ModelPrimaryAction.ACTIVATE -> {
                                    val result = graph.modelActivation.activate(entry)
                                    if (result.success && entry.componentType == com.hermestower.voiceconsole.models.ModelComponentType.VOICE.name) {
                                        // GRAPH 4.10 §7/§8: registry activation alone is NOT
                                        // active — the six-condition runtime truth runs now.
                                        statusNote = context.getString(R.string.mm_voice_qualifying, entry.displayName)
                                        scope.launch {
                                            val outcome = graph.qualifyVoiceRuntime(entry)
                                            statusNote = when (outcome) {
                                                is com.hermestower.voiceconsole.voice.VoiceRuntimeFactory.ActivationOutcome.Active ->
                                                    context.getString(R.string.mm_voice_active, entry.displayName)
                                                is com.hermestower.voiceconsole.voice.VoiceRuntimeFactory.ActivationOutcome.Failed ->
                                                    context.getString(
                                                        R.string.mm_voice_runtime_failed,
                                                        entry.displayName,
                                                        outcome.typedReason.name,
                                                    )
                                            }
                                            registryTick++
                                        }
                                    } else {
                                        statusNote = context.getString(R.string.mm_activate_result, entry.identity, if (result.success) "ACTIVE" else "REFUSED", result.reason)
                                        registryTick++
                                    }
                                }
                                com.hermestower.voiceconsole.models.ModelPrimaryAction.RUN_FUNCTIONAL_TEST,
                                com.hermestower.voiceconsole.models.ModelPrimaryAction.TEST,
                                -> {
                                    // the REAL deterministic test path by component type
                                    when (entry.componentTypeEnum()) {
                                        com.hermestower.voiceconsole.models.ModelComponentType.VOICE -> {
                                            val phrase = "Benvenuto nella console vocale di Hermes Tower."
                                            val engine = when (entry.identity) {
                                                "org.piper.voice.it-riccardo-x-low" -> graph.naturalTts
                                                "org.piper.voice.it-paola-medium" -> graph.naturalTtsPaola
                                                else -> null
                                            }
                                            scope.launch {
                                                if (engine != null && engine.resolveVoiceDir() != null) {
                                                    engine.load()
                                                    engine.interrupt()
                                                    engine.speak(phrase)
                                                    statusNote = context.getString(R.string.mm_sample_requested)
                                                } else {
                                                    statusNote = context.getString(R.string.mm_fixture_error_fmt, "voice bundle not installed")
                                                }
                                            }
                                        }
                                        com.hermestower.voiceconsole.models.ModelComponentType.SPEECH_TO_TEXT -> {
                                            val runner = com.hermestower.voiceconsole.voice.SttFixtureTestRunner
                                            val expected = "Benvenuto alla torre di Hermes, il canale vocale è pronto."
                                            scope.launch {
                                                val bytes = runCatching {
                                                    context.assets.open("models/fixtures/fixture_benvenuto_torre_it.wav").use { it.readBytes() }
                                                }.getOrNull()
                                                if (bytes == null) {
                                                    statusNote = context.getString(R.string.mm_fixture_error_fmt, "fixture asset missing")
                                                } else {
                                                    val r = runner.run(graph.voskFrameStt, bytes)
                                                    statusNote = when {
                                                        !r.success -> context.getString(R.string.mm_fixture_error_fmt, r.error ?: "no transcript")
                                                        else -> context.getString(R.string.mm_fixture_transcript_fmt, r.transcript ?: "") +
                                                            "  " + context.getString(R.string.mm_fixture_expected_fmt, expected)
                                                    }
                                                }
                                            }
                                        }
                                        else -> expanded[entry.identity] = !isExpanded
                                    }
                                }
                                com.hermestower.voiceconsole.models.ModelPrimaryAction.REPAIR -> {
                                    if (graph.modelDownloads.verifiedExisting(entry)) {
                                        statusNote = context.getString(R.string.mm_repair_ok, entry.displayName)
                                    } else {
                                        graph.modelDownloads.cancel(entry.identity)
                                        graph.modelDownloads.start(entry, entry.sourceUri)
                                        statusNote = context.getString(R.string.mm_repair_needed, entry.displayName)
                                    }
                                    registryTick++
                                }
                                com.hermestower.voiceconsole.models.ModelPrimaryAction.REVIEW_ISSUE -> {
                                    expanded[entry.identity] = true
                                }
                                com.hermestower.voiceconsole.models.ModelPrimaryAction.DETAILS_ONLY -> {
                                    expanded[entry.identity] = !isExpanded
                                }
                            }
                        },
                        AdaptiveAction(
                            key = "details",
                            label = context.getString(if (isExpanded) R.string.mm_hide_details else R.string.mm_details),
                        ) {
                            expanded[entry.identity] = !isExpanded
                        },
                    ),
                    secondary = listOf(
                        AdaptiveAction(
                            key = "pause",
                            label = context.getString(R.string.mm_pause),
                            enabled = progress?.state == ModelInstallState.DOWNLOADING,
                        ) {
                            graph.modelDownloads.pause(entry.identity); statusNote = context.getString(R.string.mm_download_paused, entry.identity)
                        },
                        AdaptiveAction(
                            key = "resume",
                            label = context.getString(R.string.mm_resume),
                            enabled = progress?.state == ModelInstallState.DOWNLOAD_PAUSED,
                        ) {
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
                        AdaptiveAction(
                            key = "cancel",
                            label = context.getString(R.string.mm_cancel),
                            enabled = progress?.state == ModelInstallState.DOWNLOADING || progress?.state == ModelInstallState.DOWNLOAD_PAUSED,
                        ) {
                            graph.modelDownloads.cancel(entry.identity); statusNote = context.getString(R.string.mm_download_cancelled, entry.identity)
                        },
                        AdaptiveAction(key = "qualify", label = context.getString(R.string.mm_qualify)) {
                            val result = graph.modelQualification.qualify(entry)
                            statusNote = context.getString(R.string.mm_qualify_result, entry.identity, if (result.qualified) "PASSED" else "FAILED", result.checks.joinToString { "${it.name}=${it.passed}" })
                        },
                        AdaptiveAction(key = "deactivate", label = context.getString(R.string.mm_deactivate)) {
                            val result = graph.modelActivation.deactivate(entry)
                            statusNote = context.getString(R.string.mm_deactivate_result, entry.identity, result.reason)
                            registryTick++
                        },
                        AdaptiveAction(key = "rollback", label = context.getString(R.string.mm_rollback)) {
                            val result = graph.modelRollback.rollback(entry)
                            statusNote = context.getString(R.string.mm_rollback_result, entry.identity, if (result.success) context.getString(R.string.mm_rollback_restored, result.restoredVersion) else result.reason)
                            registryTick++
                        },
                        AdaptiveAction(key = "remove", label = context.getString(R.string.mm_remove)) {
                            val removed = graph.modelRollback.removalCleanup(entry)
                            statusNote = if (removed) context.getString(R.string.mm_removed, entry.identity) else context.getString(R.string.mm_nothing_to_remove, entry.identity)
                            registryTick++
                        },
                        AdaptiveAction(
                            key = "redownload",
                            label = context.getString(R.string.mm_redownload),
                            enabled = entry.entryClassification == "VERIFIED_DOWNLOADABLE" && entry.identity != PROBE_IDENTITY && !packRunning,
                        ) {
                            graph.modelDownloads.cancel(entry.identity)
                            val removed = graph.modelRollback.removalCleanup(entry)
                            graph.modelDownloads.start(entry, entry.sourceUri)
                            statusNote = context.getString(R.string.mm_redownload_note, entry.displayName, if (removed) context.getString(R.string.mm_removed_word) else context.getString(R.string.mm_absent_word))
                            registryTick++
                        },
                        AdaptiveAction(
                            key = "update",
                            label = context.getString(R.string.mm_update),
                            enabled = updateAvailable && !packRunning,
                        ) {
                            graph.modelDownloads.start(entry, entry.sourceUri)
                            statusNote = context.getString(R.string.mm_update_note, entry.displayName, record?.version ?: "", entry.version)
                            registryTick++
                        },
                        AdaptiveAction(key = "diagnostics", label = context.getString(R.string.mm_open_diagnostics)) {
                            diagnosticsOpen.value = !diagnosticsOpen.value
                        },
                    ),
                    moreActionsLabel = context.getString(R.string.mm_more_actions),
                )
            }
            }
        }

        // ---------- §20 technical details: storage roots + receipts ----------
        if (diagnosticsOpen.value) {
            SectionCard(title = stringResource(R.string.mm_tech_details)) {
                Text(stringResource(R.string.mm_diag_roots), style = HermesType.sectionLabel, color = c.bronze)
                graph.modelStorage.rootsUsage().forEach { (root, bytes) ->
                    Text(context.getString(R.string.mm_root_line, root, formatBytes(bytes)), style = HermesType.bodySecondary, color = c.textSecondary)
                }
                val receipts = com.hermestower.voiceconsole.models.ReceiptWriter.listReceipts(graph.modelStorage.receiptsDir())
                Text(
                    context.getString(
                        R.string.mm_receipts,
                        if (receipts.isEmpty()) context.getString(R.string.mm_receipts_none)
                        else context.getString(R.string.mm_receipts_some, receipts.size, receipts.last()),
                    ),
                    style = HermesType.bodySecondary, color = c.textSecondary,
                )
            }
        }

        // ---------- CP-G4-009: bundled verified delivery (no network) ----------
        SectionCard(title = stringResource(R.string.mm_bundled_title)) {
            var bundledReport by remember { mutableStateOf<List<String>>(emptyList()) }
            var bundledRunning by remember { mutableStateOf(false) }
            Text(
                stringResource(R.string.mm_bundled_explainer),
                style = HermesType.bodySecondary, color = c.textSecondary,
            )
            HermesFilledButton(
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
            bundledReport.forEach { Text(it, style = HermesType.bodySecondary, color = c.textSecondary) }
        }

        // ---------- E. Modes ----------
        SectionCard(title = stringResource(R.string.mm_section_e)) {
            Text(
                stringResource(R.string.mm_modes_text),
                style = HermesType.body, color = c.textPrimary,
            )
        }

        // ---------- F. Tower-only fallback ----------
        SectionCard(title = stringResource(R.string.mm_section_f)) {
            Text(
                stringResource(R.string.mm_fallback_text),
                style = HermesType.body, color = c.textPrimary,
            )
        }

        statusNote?.let {
            Text(context.getString(R.string.mm_status_prefix, it), style = HermesType.body, color = c.textInfo)
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
        color = c.textSecondary,
    )
}

internal const val PROBE_IDENTITY = "org.hermestower.test.probe-artifact"
