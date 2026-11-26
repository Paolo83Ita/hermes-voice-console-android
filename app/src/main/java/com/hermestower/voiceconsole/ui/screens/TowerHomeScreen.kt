package com.hermestower.voiceconsole.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.hermestower.voiceconsole.R
import com.hermestower.voiceconsole.app.AppGraph
import com.hermestower.voiceconsole.pairing.PairingSessionController
import com.hermestower.voiceconsole.ui.components.HermesFilledButton
import com.hermestower.voiceconsole.ui.components.MeanderBand
import com.hermestower.voiceconsole.ui.components.ResponsiveScreenHeader
import com.hermestower.voiceconsole.ui.components.SectionCard
import com.hermestower.voiceconsole.ui.theme.HermesType
import com.hermestower.voiceconsole.ui.theme.hermesColors
import kotlinx.coroutines.launch

/**
 * ANDROID REPAIR CP-005 — the TOWER primary surface (mission section 4).
 *
 * Primary navigation destination (HOME / VOICE / TOWER / ARCHIVE / SETTINGS)
 * exposing ALL the charter duties: connection status, pairing, endpoint and
 * transport, test connection (REAL, never 'SIMULATED' — the APP-P1-07 repair),
 * diagnostics, retry, revoke, pair again, advanced manual configuration.
 * Pairing is NEVER buried here: the primary card opens the pairing flow when
 * unpaired (one tap from a primary screen).
 */
@Composable
fun TowerHomeScreen(
    graph: AppGraph,
    onOpenPairing: () -> Unit,
    onOpenDiagnostics: () -> Unit,
) {
    val c = hermesColors
    val scope = rememberCoroutineScope()
    var snap by remember { mutableStateOf<PairingSessionController.Snapshot?>(null) }
    var testLine by remember { mutableStateOf<String?>(null) }
    var testing by remember { mutableStateOf(false) }
    var revokeConfirm by remember { mutableStateOf(false) }

    // Restart read-back: the durable truth decides what this screen shows.
    val readBack = remember { graph.pairingSessionController.readBack() }
    val paired = readBack.state == PairingSessionController.State.PAIRED ||
        readBack.state == PairingSessionController.State.DEGRADED

    Column(
        Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ResponsiveScreenHeader(title = stringResource(R.string.tower_home_title))
        MeanderBand(colors = hermesColors, modifier = Modifier.padding(top = 6.dp))

        // ---- connection status ----
        SectionCard(title = stringResource(if (paired) R.string.tower_home_status_paired else R.string.tower_home_status_unpaired)) {
            if (paired) {
                readBack.deviceId?.let {
                    Text(
                        stringResource(R.string.pairing_paired_fmt, it, readBack.connectionTestMs?.toString() ?: "-"),
                        style = HermesType.body, color = c.textPrimary,
                    )
                }
                if (readBack.state == PairingSessionController.State.DEGRADED) {
                    Text(
                        stringResource(R.string.pairing_caps_not_negotiated),
                        style = HermesType.bodySecondary, color = c.textSecondary,
                    )
                }
                // ---- REAL connection test (APP-P1-07: never a simulated value) ----
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(enabled = !testing, onClick = {
                        testing = true
                        testLine = null
                        scope.launch {
                            try {
                                val result = graph.pairingSessionController.testConnection()
                                testLine = if (result == null) {
                                    "nessun endpoint noto: rifai l'abbinamento"
                                } else {
                                    val (ok, ms) = result
                                    if (ok) "connessione OK — $ms ms" else "connessione NON riuscita ($ms ms): vedi Diagnostica"
                                }
                            } finally {
                                testing = false
                            }
                        }
                    }) { Text(stringResource(R.string.tower_home_test_connection)) }
                    TextButton(onClick = onOpenDiagnostics) { Text(stringResource(R.string.tower_home_diagnostics)) }
                }
                if (testing) Text(stringResource(R.string.tower_home_test_running), style = HermesType.bodySecondary, color = c.textSecondary)
                testLine?.let { Text(it, style = HermesType.bodySecondary, color = c.textSecondary) }
                TextButton(onClick = { revokeConfirm = true }) { Text(stringResource(R.string.tower_home_revoke)) }
                TextButton(onClick = onOpenPairing) { Text(stringResource(R.string.tower_home_pair_again)) }
            } else {
                // ---- the primary pairing card: ONE tap from a primary screen ----
                Text(
                    stringResource(R.string.tower_home_connect_card_body),
                    style = HermesType.bodySecondary, color = c.textSecondary,
                )
                HermesFilledButton(onClick = onOpenPairing) {
                    Text(stringResource(R.string.tower_home_connect_now))
                }
                TextButton(onClick = onOpenPairing) { Text(stringResource(R.string.tower_home_advanced)) }
            }
        }

        // ---- live session (when a pairing is in flight from any entry point) ----
        val session = snap
        val live = session ?: if (readBack.state == PairingSessionController.State.AWAITING_OWNER_CONFIRMATION) readBack else null
        if (live != null) {
            SectionCard(title = stringResource(R.string.pairing_session_card)) {
                PairingSessionStateView(
                    graph = graph,
                    snap = live,
                    onPaired = { snap = graph.pairingSessionController.snapshot },
                    onRestartScan = onOpenPairing,
                )
            }
        }

        // ---- the advanced entry stays reachable (never buried in Tower Studio) ----
        SectionCard(title = stringResource(R.string.pairing_method_manual_title)) {
            Text(
                stringResource(R.string.pairing_method_manual_note),
                style = HermesType.bodySecondary, color = c.textSecondary,
            )
            TextButton(onClick = onOpenPairing) { Text(stringResource(R.string.pairing_method_manual_button)) }
        }
    }

    if (revokeConfirm) {
        AlertDialog(
            onDismissRequest = { revokeConfirm = false },
            title = { Text(stringResource(R.string.tower_home_revoke)) },
            text = { Text(stringResource(R.string.tower_home_revoke_confirm)) },
            confirmButton = {
                TextButton(onClick = {
                    revokeConfirm = false
                    graph.pairingSessionController.revoke()
                    snap = null
                    testLine = null
                }) { Text(stringResource(R.string.tower_home_revoke)) }
            },
            dismissButton = {
                TextButton(onClick = { revokeConfirm = false }) { Text(stringResource(R.string.pairing_cancel)) }
            },
        )
    }
}
