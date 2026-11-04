package com.hermestower.voiceconsole.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.hermestower.voiceconsole.R as Res
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hermestower.voiceconsole.app.AppGraph
import com.hermestower.voiceconsole.bridge.BridgeFault
import com.hermestower.voiceconsole.ui.components.SectionCard
import com.hermestower.voiceconsole.ui.components.MeanderBand
import com.hermestower.voiceconsole.ui.components.TruthBadge
import com.hermestower.voiceconsole.ui.theme.HermesType
import com.hermestower.voiceconsole.ui.theme.hermesColors
import kotlinx.coroutines.launch

/**
 * S7 Diagnostics (FR-36): real device facts, service truth, connection/audio tests,
 * simulation controls, redacted log export. No secret values anywhere (SEC-06).
 */
@Composable
fun DiagnosticsScreen(graph: AppGraph, onOpenStudio: () -> Unit = {}, onOpenSelfTest: () -> Unit = {}) {
    val c = hermesColors
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val healthState = remember { graph.health.snapshot() }
    val connection = graph.store.connection.collectAsStateWithLifecycle()
    var report by remember { mutableStateOf<String?>(null) }
    var testResult by remember { mutableStateOf<String?>(null) }
    val clipboard = LocalClipboardManager.current
    val context = androidx.compose.ui.platform.LocalContext.current

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(stringResource(Res.string.diag_title), style = HermesType.screenTitle, color = c.ink)
        MeanderBand(colors = hermesColors, modifier = Modifier.padding(top = 6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = onOpenStudio) { Text(stringResource(Res.string.diag_studio_link)) }
            TextButton(onClick = onOpenSelfTest) { Text(stringResource(Res.string.diag_selftest_link)) }
        }

        SectionCard(title = stringResource(Res.string.diag_device_card)) {
            // DeviceHealth fields are nullable — the platform may not expose them;
            // the localized 'unavailable' fallback keeps the honest-rendering rule.
            Text(stringResource(Res.string.diag_manufacturer_fmt, healthState.manufacturer ?: stringResource(Res.string.diag_unavailable)), style = HermesType.body, color = c.ink)
            Text(stringResource(Res.string.diag_model_fmt, healthState.model ?: stringResource(Res.string.diag_unavailable)), style = HermesType.body, color = c.ink)
            Text(stringResource(Res.string.diag_android_fmt, healthState.androidRelease ?: stringResource(Res.string.diag_unavailable), healthState.sdkInt ?: -1), style = HermesType.body, color = c.ink)
            Text(
                stringResource(
                    Res.string.diag_memory_fmt,
                    healthState.memoryTotalMb ?: stringResource(Res.string.diag_unavailable),
                    healthState.memoryAvailableMb ?: stringResource(Res.string.diag_unavailable),
                ),
                style = HermesType.body, color = c.ink,
            )
            Text(
                stringResource(
                    Res.string.diag_storage_fmt,
                    healthState.storageFreeGb?.let { "%.1f".format(it) } ?: stringResource(Res.string.diag_unavailable),
                ),
                style = HermesType.body, color = c.ink,
            )
            Text(
                stringResource(
                    Res.string.diag_battery_fmt,
                    healthState.batteryTemperatureC?.let { "%.1f".format(it) } ?: stringResource(Res.string.diag_unavailable),
                    healthState.isCharging ?: stringResource(Res.string.diag_unavailable),
                ),
                style = HermesType.body, color = c.ink,
            )
            Text(
                stringResource(
                    Res.string.diag_refresh_fmt,
                    healthState.refreshRateHz?.let { "%.0f".format(it) } ?: stringResource(Res.string.diag_unavailable),
                    healthState.thermalStatus ?: stringResource(Res.string.diag_unavailable),
                ),
                style = HermesType.body, color = c.ink,
            )
            Text(
                stringResource(Res.string.diag_unavailable_note),
                style = HermesType.bodySecondary,
                color = c.inkSecondary,
            )
        }

        // GRAPH 4 FINAL §13: ACTIVE-RUNTIME READ-BACK — safe view of the live
        // engines (identities and states only; no secrets, no raw paths).
        SectionCard(title = stringResource(Res.string.diag_runtime_card)) {
            Text(
                stringResource(Res.string.diag_stt_engine_fmt, graph.activeRuntimeReadback.sttActiveEngine),
                style = HermesType.body, color = c.ink,
            )
            Text(
                stringResource(Res.string.diag_stt_frame_fmt, graph.activeRuntimeReadback.sttFrameState),
                style = HermesType.body, color = c.ink,
            )
            Text(
                stringResource(Res.string.diag_tts_engine_fmt, graph.activeRuntimeReadback.ttsActiveEngine),
                style = HermesType.body, color = c.ink,
            )
            Text(
                stringResource(Res.string.diag_llm_state_fmt, graph.activeRuntimeReadback.llmState),
                style = HermesType.body, color = c.ink,
            )
            Text(
                stringResource(Res.string.diag_onnx_fmt, graph.activeRuntimeReadback.onnxRuntime),
                style = HermesType.body, color = c.ink,
            )
            Text(
                stringResource(Res.string.diag_runtime_note),
                style = HermesType.bodySecondary,
                color = c.inkSecondary,
            )
        }

        SectionCard(title = stringResource(Res.string.diag_service_card)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TruthBadge(stringResource(Res.string.diag_service_badge), c.warn)
            }
            Text(
                stringResource(Res.string.diag_service_note),
                style = HermesType.bodySecondary,
                color = c.inkSecondary,
            )
        }

        SectionCard(title = stringResource(Res.string.diag_tests_card)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = {
                    scope.launch {
                        testResult = when (val r = graph.bridge.handshake()) {
                            is com.hermestower.voiceconsole.domain.BridgeResult.Success ->
                                context.getString(Res.string.diag_bridge_ok)
                            is com.hermestower.voiceconsole.domain.BridgeResult.Failure ->
                                context.getString(Res.string.diag_bridge_fail, r.error)
                        }
                    }
                }) { Text(stringResource(Res.string.diag_run_bridge)) }
                TextButton(onClick = {
                    testResult = context.getString(Res.string.diag_audio_unavailable)
                }) { Text(stringResource(Res.string.diag_run_audio)) }
            }
            testResult?.let { Text(it, style = HermesType.body, color = c.ink) }
            Text(
                stringResource(Res.string.diag_bridge_endpoint_fmt, connection.value.endpointLabel, connection.value.status.name.lowercase()),
                style = HermesType.bodySecondary,
                color = c.inkSecondary,
            )
        }

        SectionCard(title = stringResource(Res.string.diag_sim_card)) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                TextButton(onClick = { graph.driver.setFault(BridgeFault.DISCONNECT_AT_ACK) }) { Text(stringResource(Res.string.diag_sim_disconnect)) }
                TextButton(onClick = { graph.driver.setFault(BridgeFault.ERROR_AFTER_PROGRESS) }) { Text(stringResource(Res.string.diag_sim_tower_error)) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                TextButton(onClick = { graph.driver.setFault(BridgeFault.SLOW_TOWER) }) { Text(stringResource(Res.string.diag_sim_slow)) }
                TextButton(onClick = { graph.driver.clearFault() }) { Text(stringResource(Res.string.diag_clear_fault)) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                TextButton(onClick = { graph.driver.simulateUpdateAvailable() }) { Text(stringResource(Res.string.diag_sim_update)) }
                TextButton(onClick = { graph.events.clear() }) { Text(stringResource(Res.string.diag_clear_events)) }
            }
            Text(
                stringResource(Res.string.diag_sim_note),
                style = HermesType.bodySecondary,
                color = c.inkSecondary,
            )
        }

        SectionCard(title = stringResource(Res.string.diag_export_card)) {
            Button(onClick = { report = graph.diagnostics.exportRedactedReport() }) { Text(stringResource(Res.string.diag_generate)) }
            report?.let {
                Text(
                    it.take(1200) + if (it.length > 1200) " …" else "",
                    style = HermesType.bodySecondary,
                    color = c.inkSecondary,
                )
                TextButton(onClick = { clipboard.setText(AnnotatedString(it)) }) { Text(stringResource(Res.string.diag_copy_full)) }
            }
            Text(
                stringResource(Res.string.diag_redaction_note),
                style = HermesType.bodySecondary,
                color = c.inkSecondary,
            )
        }
    }
}
