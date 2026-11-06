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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hermestower.voiceconsole.R as Res
import com.hermestower.voiceconsole.app.AppGraph
import com.hermestower.voiceconsole.core.state.ConnectionStatus
import com.hermestower.voiceconsole.ui.components.ResponsiveScreenHeader
import com.hermestower.voiceconsole.ui.components.SectionCard
import com.hermestower.voiceconsole.ui.components.MeanderBand
import com.hermestower.voiceconsole.ui.components.TruthBadge
import com.hermestower.voiceconsole.ui.components.BadgeFlowRow
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
    // Non-composable contexts (the handshake coroutine) resolve strings through
    // the context — stringResource is @Composable-only.
    val context = androidx.compose.ui.platform.LocalContext.current
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
        ResponsiveScreenHeader(title = stringResource(Res.string.connection_title))
        MeanderBand(colors = hermesColors, modifier = Modifier.padding(top = 6.dp))
        TextButton(onClick = onOpenStudio) {
            Text(stringResource(Res.string.connection_open_studio))
        }

        SectionCard(title = stringResource(Res.string.connection_endpoint_card)) {
            OutlinedTextField(
                value = endpointDraft,
                onValueChange = { endpointDraft = it },
                label = { Text(stringResource(Res.string.connection_endpoint_field)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            Text(
                stringResource(Res.string.connection_endpoint_note),
                style = HermesType.bodySecondary,
                color = c.inkSecondary,
            )
        }

        SectionCard(title = stringResource(Res.string.connection_transport_card)) {
            BadgeFlowRow(horizontalSpacing = 8.dp) {
                TruthBadge(stringResource(Res.string.connection_badge_mock), c.ok)
                TruthBadge(stringResource(Res.string.connection_badge_adapter), c.inkSecondary)
            }
            BadgeFlowRow(horizontalSpacing = 8.dp) {
                TruthBadge(
                    stringResource(Res.string.connection_tls_fmt) + " " + when (connection.tls) {
                        ConnectionStatus.TlsStatus.MOCK_NOT_APPLICABLE -> "N/A (MOCK)"
                        ConnectionStatus.TlsStatus.ACTIVE -> "ACTIVE"
                        ConnectionStatus.TlsStatus.FAILED -> "FAILED"
                        ConnectionStatus.TlsStatus.UNKNOWN -> "UNKNOWN"
                    },
                    c.warn,
                )
            }
            Text(
                stringResource(Res.string.connection_endpoint_label_fmt, connection.endpointLabel),
                style = HermesType.bodySecondary,
                color = c.inkSecondary,
            )
        }

        SectionCard(title = stringResource(Res.string.connection_pairing_card)) {
            TruthBadge(stringResource(Res.string.connection_badge_not_paired), c.warn)
            Text(
                stringResource(Res.string.connection_pairing_note),
                style = HermesType.bodySecondary,
                color = c.inkSecondary,
            )
        }

        SectionCard(title = stringResource(Res.string.connection_tests_card)) {
            BadgeFlowRow(horizontalSpacing = 8.dp) {
                Button(
                    onClick = {
                        testing = true
                        testResult = null
                        scope.launch {
                            val result = graph.bridge.handshake()
                            testResult = when (result) {
                                is com.hermestower.voiceconsole.domain.BridgeResult.Success ->
                                    context.getString(
                                        Res.string.connection_handshake_ok_fmt,
                                        java.time.LocalTime.now().withSecond(0),
                                    )
                                is com.hermestower.voiceconsole.domain.BridgeResult.Failure ->
                                    context.getString(
                                        Res.string.connection_handshake_fail_fmt,
                                        result.error,
                                        result.message,
                                    )
                            }
                            testing = false
                        }
                    },
                    enabled = !testing,
                ) {
                    Text(
                        if (testing) stringResource(Res.string.connection_testing)
                        else stringResource(Res.string.connection_test_button),
                    )
                }
            }
            testResult?.let { Text(it, style = HermesType.body, color = c.ink) }
            Text(
                stringResource(
                    Res.string.connection_last_handshake_fmt,
                    connection.lastHandshakeAt ?: stringResource(Res.string.connection_handshake_none),
                ),
                style = HermesType.bodySecondary,
                color = c.inkSecondary,
            )
            Text(
                stringResource(Res.string.connection_status_fmt, connection.status.name.lowercase()),
                style = HermesType.bodySecondary,
                color = c.inkSecondary,
            )
        }

        SectionCard(title = stringResource(Res.string.connection_nosecrets_card)) {
            Text(
                stringResource(Res.string.connection_nosecrets_note),
                style = HermesType.bodySecondary,
                color = c.inkSecondary,
            )
        }
    }
}
