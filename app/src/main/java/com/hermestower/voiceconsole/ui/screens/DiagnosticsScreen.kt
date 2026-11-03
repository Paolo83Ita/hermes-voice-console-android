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
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
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

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Diagnostics", style = HermesType.screenTitle, color = c.ink)
        MeanderBand(colors = hermesColors, modifier = Modifier.padding(top = 6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = onOpenStudio) { Text("Open Tower Connection Studio →") }
            TextButton(onClick = onOpenSelfTest) { Text("Run Self-Test →") }
        }

        SectionCard(title = "Device (real values)") {
            Text("Manufacturer: ${healthState.manufacturer}", style = HermesType.body, color = c.ink)
            Text("Model: ${healthState.model}", style = HermesType.body, color = c.ink)
            Text("Android: ${healthState.androidRelease} (SDK ${healthState.sdkInt})", style = HermesType.body, color = c.ink)
            Text(
                "Memory: ${healthState.memoryTotalMb ?: "unavailable"} MB total, ${healthState.memoryAvailableMb ?: "unavailable"} MB available",
                style = HermesType.body, color = c.ink,
            )
            Text(
                "Storage free: ${healthState.storageFreeGb?.let { "%.1f".format(it) } ?: "unavailable"} GB",
                style = HermesType.body, color = c.ink,
            )
            Text(
                "Battery temperature: ${healthState.batteryTemperatureC?.let { "%.1f".format(it) } ?: "unavailable"} °C (charging: ${healthState.isCharging ?: "unavailable"})",
                style = HermesType.body, color = c.ink,
            )
            Text(
                "Refresh rate: ${healthState.refreshRateHz?.let { "%.0f".format(it) } ?: "unavailable"} Hz — thermal: ${healthState.thermalStatus ?: "unavailable"}",
                style = HermesType.body, color = c.ink,
            )
            Text(
                "A dash or 'unavailable' means the platform does not expose the datum; nothing here is fabricated.",
                style = HermesType.bodySecondary,
                color = c.inkSecondary,
            )
        }

        SectionCard(title = "Service state") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TruthBadge("VoiceConsoleService: REGISTERED, NOT STARTED", c.warn)
            }
            Text(
                "Truthful per decision D-011: the foreground voice service is V0.2 work; starting a service now would imply a pipeline that does not exist.",
                style = HermesType.bodySecondary,
                color = c.inkSecondary,
            )
        }

        SectionCard(title = "Tests") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = {
                    scope.launch {
                        testResult = when (val r = graph.bridge.handshake()) {
                            is com.hermestower.voiceconsole.domain.BridgeResult.Success -> "Bridge handshake: OK (mock)"
                            is com.hermestower.voiceconsole.domain.BridgeResult.Failure -> "Bridge handshake: FAILED (${r.error})"
                        }
                    }
                }) { Text("Run bridge test") }
                TextButton(onClick = {
                    testResult = "Audio loopback test: NOT AVAILABLE (V0.2 — no capture engine exists in V0.1)."
                }) { Text("Run audio test") }
            }
            testResult?.let { Text(it, style = HermesType.body, color = c.ink) }
            Text(
                "Bridge endpoint: ${connection.value.endpointLabel} · status ${connection.value.status.name.lowercase()}",
                style = HermesType.bodySecondary,
                color = c.inkSecondary,
            )
        }

        SectionCard(title = "Simulation (prototype controls)") {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                TextButton(onClick = { graph.driver.setFault(BridgeFault.DISCONNECT_AT_ACK) }) { Text("Simulate disconnect") }
                TextButton(onClick = { graph.driver.setFault(BridgeFault.ERROR_AFTER_PROGRESS) }) { Text("Simulate tower error") }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                TextButton(onClick = { graph.driver.setFault(BridgeFault.SLOW_TOWER) }) { Text("Simulate slow tower") }
                TextButton(onClick = { graph.driver.clearFault() }) { Text("Clear fault") }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                TextButton(onClick = { graph.driver.simulateUpdateAvailable() }) { Text("Simulate update") }
                TextButton(onClick = { graph.events.clear() }) { Text("Clear event log") }
            }
            Text(
                "These buttons drive the real code paths (bridge faults, states) — they do not fake UI states.",
                style = HermesType.bodySecondary,
                color = c.inkSecondary,
            )
        }

        SectionCard(title = "Redacted log export") {
            Button(onClick = { report = graph.diagnostics.exportRedactedReport() }) { Text("Generate redacted report") }
            report?.let {
                Text(
                    it.take(1200) + if (it.length > 1200) " …" else "",
                    style = HermesType.bodySecondary,
                    color = c.inkSecondary,
                )
                TextButton(onClick = { clipboard.setText(AnnotatedString(it)) }) { Text("Copy full report") }
            }
            Text(
                "Every exported line passes LogRedactor (token/key/secret assignments, long hex and base64 runs are masked) — unit-tested (SEC-06).",
                style = HermesType.bodySecondary,
                color = c.inkSecondary,
            )
        }
    }
}
