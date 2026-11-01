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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import com.hermestower.voiceconsole.core.state.ConnectionStatus
import com.hermestower.voiceconsole.ui.components.SectionCard
import com.hermestower.voiceconsole.ui.components.TruthBadge
import com.hermestower.voiceconsole.ui.theme.HermesType
import com.hermestower.voiceconsole.ui.theme.hermesColors
import kotlinx.coroutines.launch

/**
 * S3 Connection (FR-23): endpoint placeholder, pairing state, transport mode,
 * TLS status, last successful connection, test action. No real endpoint is
 * contacted in V0.1 and no credential exists (D-005, SEC-01).
 */
@Composable
fun ConnectionScreen(graph: AppGraph, onOpenStudio: () -> Unit = {}) {
    val c = hermesColors
    val connection by graph.store.connection.collectAsStateWithLifecycle()
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    var endpointDraft by remember { mutableStateOf("https://tower.example/ws") }
    var testResult by remember { mutableStateOf<String?>(null) }
    var testing by remember { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Connection", style = HermesType.screenTitle, color = c.ink)
        TextButton(onClick = onOpenStudio) {
            Text("Open Hermes Tower Connection Studio →")
        }

        SectionCard(title = "Tower endpoint") {
            OutlinedTextField(
                value = endpointDraft,
                onValueChange = { endpointDraft = it },
                label = { Text("Endpoint (placeholder — unused in V0.1)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            Text(
                "The draft above is never saved, never used, never sent anywhere. The real endpoint is configured at V0.4 after the Tower contract is attached (assumption A-20).",
                style = HermesType.bodySecondary,
                color = c.inkSecondary,
            )
        }

        SectionCard(title = "Transport") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TruthBadge("MOCK SIMULATOR — ACTIVE", c.ok)
                TruthBadge("HTTPS/WSS ADAPTER — V0.4", c.inkSecondary)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TruthBadge(
                    "TLS: " + when (connection.tls) {
                        ConnectionStatus.TlsStatus.MOCK_NOT_APPLICABLE -> "N/A (MOCK)"
                        ConnectionStatus.TlsStatus.ACTIVE -> "ACTIVE"
                        ConnectionStatus.TlsStatus.FAILED -> "FAILED"
                        ConnectionStatus.TlsStatus.UNKNOWN -> "UNKNOWN"
                    },
                    c.warn,
                )
            }
            Text(
                "Endpoint label: ${connection.endpointLabel}",
                style = HermesType.bodySecondary,
                color = c.inkSecondary,
            )
        }

        SectionCard(title = "Pairing") {
            TruthBadge("NOT PAIRED — BY DESIGN IN V0.1", c.warn)
            Text(
                "Pairing is designed but not implemented: an out-of-band code is shown here, entered in the Tower admin surface; the Tower issues a device token bound to Android Keystore (V0.5). No credential exists in this build.",
                style = HermesType.bodySecondary,
                color = c.inkSecondary,
            )
        }

        SectionCard(title = "Tests") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        testing = true
                        testResult = null
                        scope.launch {
                            val result = graph.bridge.handshake()
                            testResult = when (result) {
                                is com.hermestower.voiceconsole.domain.BridgeResult.Success ->
                                    "Handshake OK (mock) at ${java.time.LocalTime.now().withSecond(0)}"
                                is com.hermestower.voiceconsole.domain.BridgeResult.Failure ->
                                    "Handshake failed: ${result.error} — ${result.message}"
                            }
                            testing = false
                        }
                    },
                    enabled = !testing,
                ) { Text(if (testing) "Testing…" else "Test connection (mock)") }
            }
            testResult?.let { Text(it, style = HermesType.body, color = c.ink) }
            Text(
                "Last successful handshake: ${connection.lastHandshakeAt ?: "none in this session"}",
                style = HermesType.bodySecondary,
                color = c.inkSecondary,
            )
            Text(
                "Status: ${connection.status.name.lowercase()}",
                style = HermesType.bodySecondary,
                color = c.inkSecondary,
            )
        }

        SectionCard(title = "No secrets policy") {
            Text(
                "No tokens, passwords or production endpoints exist in this repository (SEC-01). The Diagnostics export redacts token-like strings, and this screen never displays a credential after entry (SEC-02).",
                style = HermesType.bodySecondary,
                color = c.inkSecondary,
            )
        }
    }
}
