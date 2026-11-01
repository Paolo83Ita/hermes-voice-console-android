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
import com.hermestower.voiceconsole.selftest.SelfTestMode
import com.hermestower.voiceconsole.selftest.SelfTestState
import com.hermestower.voiceconsole.ui.components.SectionCard
import com.hermestower.voiceconsole.ui.theme.HermesType
import com.hermestower.voiceconsole.ui.theme.hermesColors

/**
 * Diagnostics → Self-Test Center (GRAPH 2 §11): five modes, typed results with
 * evidence, stop/retry/export/reset. Every result is measured, never invented;
 * physical-device-only checks are reported QUALIFICATION_PENDING_DEVICE.
 */
@Composable
fun SelfTestScreen(graph: AppGraph) {
    val c = hermesColors
    val center = graph.selfTestCenter
    val results by center.results.collectAsStateWithLifecycle()
    val running by center.running.collectAsStateWithLifecycle()
    val current by center.current.collectAsStateWithLifecycle()
    var note by remember { mutableStateOf<String?>(null) }
    val clipboard = LocalClipboardManager.current

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Self-Test Center", style = HermesType.screenTitle, color = c.ink)
        Text(
            "Deterministic checks for every V0.2 subsystem. They run on this device, need no Tower, " +
                "and never send data anywhere. Results are honest: FAIL is a finding, not an error to hide.",
            style = HermesType.bodySecondary, color = c.inkSecondary,
        )

        SectionCard(title = "Run") {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Button(enabled = !running, onClick = { note = null; center.run(SelfTestMode.QUICK) }) { Text("Run Quick") }
                TextButton(enabled = !running, onClick = { note = null; center.run(SelfTestMode.STANDARD) }) { Text("Run Standard") }
                TextButton(enabled = !running, onClick = { note = null; center.run(SelfTestMode.FULL) }) { Text("Run Full") }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                TextButton(enabled = !running, onClick = { note = null; center.run(SelfTestMode.CONNECTIVITY_ONLY) }) { Text("Connectivity only") }
                TextButton(enabled = !running, onClick = { note = null; center.run(SelfTestMode.VOICE_FOUNDATION_ONLY) }) { Text("Voice foundation only") }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                TextButton(enabled = running, onClick = { center.stop(); note = "Stopped." }) { Text("Stop") }
                TextButton(enabled = !running, onClick = { center.retryFailed(); note = "Retrying failed tests." }) { Text("Retry Failed Tests") }
                TextButton(enabled = !running, onClick = { center.resetHistory(); note = "Test history reset." }) { Text("Reset Test History") }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                TextButton(onClick = {
                    val report = center.exportSecretCleanReport()
                    clipboard.setText(AnnotatedString(report))
                    note = "Secret-clean report copied to clipboard (${report.lines().size} lines)."
                }) { Text("Export Secret-Clean Report") }
                TextButton(onClick = {
                    clipboard.setText(AnnotatedString(center.summary()))
                    note = "Summary copied."
                }) { Text("Copy Summary") }
            }
            if (running) Text("Running: ${current ?: "…"}", style = HermesType.body, color = c.energy)
            note?.let { Text(it, style = HermesType.bodySecondary, color = c.inkSecondary) }
        }

        SectionCard(title = "Results") {
            if (results.isEmpty()) {
                Text("No tests run yet in this session.", style = HermesType.bodySecondary, color = c.inkSecondary)
            }
            results.toSortedMap().forEach { (id, result) ->
                val stateLabel = when (result.state) {
                    SelfTestState.PASS -> "PASS"
                    SelfTestState.FAIL -> "FAIL"
                    SelfTestState.RUNNING -> "RUNNING"
                    SelfTestState.NOT_RUN -> "NOT RUN"
                    SelfTestState.SKIPPED_NOT_APPLICABLE -> "SKIPPED"
                    SelfTestState.BLOCKED_DEPENDENCY -> "BLOCKED"
                    SelfTestState.QUALIFICATION_PENDING_DEVICE -> "PENDING DEVICE"
                }
                Text("$id — ${result.testName}", style = HermesType.body, color = c.ink)
                Text(
                    "$stateLabel · ${result.durationMs} ms · ${result.evidence}",
                    style = HermesType.bodySecondary,
                    color = when (result.state) {
                        SelfTestState.PASS -> c.inkSecondary
                        SelfTestState.FAIL -> c.warn
                        else -> c.inkSecondary
                    },
                )
                result.repairSuggestion?.let { Text("Repair: $it", style = HermesType.bodySecondary, color = c.inkSecondary) }
            }
            if (results.isNotEmpty()) {
                Text(center.summary(), style = HermesType.body, color = c.ink)
            }
        }
    }
}
