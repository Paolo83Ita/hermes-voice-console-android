package com.hermestower.voiceconsole.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import com.hermestower.voiceconsole.pairing.CandidateEvidence
import com.hermestower.voiceconsole.pairing.PairingSessionController
import com.hermestower.voiceconsole.ui.theme.hermesColors
import kotlinx.coroutines.launch

/**
 * ANDROID REPAIR CP-005 — the shared pairing session state view (mission
 * sections 4/5). Renders ONE [PairingSessionController.Snapshot] with:
 *  - the live state (Italian, human);
 *  - the PHRASE_MATCH_REQUIRED bilateral confirmation gate;
 *  - per-candidate endpoint resolution evidence (class, host, status, reason);
 *  - typed failures with human explanation + failed stage + safe retry;
 *  - the AMB-8 REPAIR_AVAILABLE honest state (server delta pending);
 *  - cancel / retry / manual-fallback controls.
 *
 * Reused by the QR scanner, the pairing flow screen and the TOWER surface so
 * every pairing entry renders the SAME truth.
 */
@Composable
fun PairingSessionStateView(
    graph: AppGraph,
    snap: PairingSessionController.Snapshot,
    onPaired: () -> Unit,
    onRestartScan: () -> Unit = {},
) {
    val c = hermesColors
    val scope = rememberCoroutineScope()

    Text(stateTitle(snap), style = HermesType.body, color = c.textPrimary)
    snap.humanDetail?.let { Text(it, style = HermesType.body, color = c.textInfo) }
    snap.typedCode?.let { code ->
        Text(
            stringResource(R.string.pairing_typed_code_fmt, code),
            style = HermesType.bodySecondary, color = c.textSecondary,
        )
    }
    snap.failedStage?.let { stage ->
        Text(
            stringResource(R.string.pairing_failed_stage_fmt, stage),
            style = HermesType.bodySecondary, color = c.textSecondary,
        )
    }

    // ---- per-candidate endpoint resolution evidence (mission section 2, rule 7) ----
    if (snap.evidence.isNotEmpty()) {
        Text(
            stringResource(R.string.pairing_evidence_title),
            style = HermesType.bodySecondary, color = c.textSecondary,
        )
        snap.evidence.forEach { ev -> CandidateEvidenceRow(ev) }
    }

    // ---- the bilateral phrase gate ----
    if (snap.state == PairingSessionController.State.PHRASE_MATCH_REQUIRED) {
        Text(
            stringResource(R.string.pairing_phrase_gate_title),
            style = HermesType.body, color = c.textPrimary,
        )
        snap.phraseHint?.let { phrase ->
            Text(
                stringResource(R.string.pairing_phrase_fmt, phrase),
                style = HermesType.body, color = c.textInfo,
            )
        }
        Text(
            stringResource(R.string.pairing_phrase_gate_note),
            style = HermesType.bodySecondary, color = c.textSecondary,
        )
        HermesFilledButton(
            enabled = !snap.phraseHint.isNullOrEmpty(),
            onClick = {
                scope.launch {
                    val after = graph.pairingSessionController.confirmPhraseMatch()
                    if (after.state == PairingSessionController.State.PAIRED ||
                        after.state == PairingSessionController.State.DEGRADED
                    ) onPaired()
                }
            },
        ) { Text(stringResource(R.string.pairing_confirm_phrase)) }
    }

    // ---- terminal states ----
    when (snap.state) {
        PairingSessionController.State.PAIRED, PairingSessionController.State.DEGRADED -> {
            Text(
                stringResource(
                    R.string.pairing_paired_fmt,
                    snap.deviceId ?: "",
                    snap.connectionTestMs?.toString() ?: "-",
                ),
                style = HermesType.body, color = c.textPrimary,
            )
            if (!snap.capabilitiesNegotiated) {
                Text(
                    stringResource(R.string.pairing_caps_not_negotiated),
                    style = HermesType.bodySecondary, color = c.textSecondary,
                )
            } else {
                Text(
                    stringResource(R.string.pairing_caps_fmt, snap.capabilities.joinToString(", ")),
                    style = HermesType.bodySecondary, color = c.textSecondary,
                )
            }
            LaunchedEffect(snap.state) { onPaired() }
        }
        PairingSessionController.State.REPAIR_AVAILABLE -> {
            Text(
                stringResource(R.string.pairing_repair_available_note),
                style = HermesType.bodySecondary, color = c.textSecondary,
            )
        }
        else -> Unit
    }

    // ---- controls: cancel always; retry when a session envelope is live ----
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        TextButton(onClick = { graph.pairingSessionController.cancel() }) {
            Text(stringResource(R.string.pairing_cancel))
        }
        OutlinedButton(onClick = {
            scope.launch {
                val after = graph.pairingSessionController.retry()
                if (after.state == PairingSessionController.State.PAIRED ||
                    after.state == PairingSessionController.State.DEGRADED
                ) onPaired()
            }
        }) { Text(stringResource(R.string.pairing_retry)) }
        if (onRestartScan != null && snap.state in setOf(
                PairingSessionController.State.FAILED,
                PairingSessionController.State.EXPIRED,
                PairingSessionController.State.PAYLOAD_REJECTED,
                PairingSessionController.State.REPAIR_AVAILABLE,
            )
        ) {
            TextButton(onClick = onRestartScan) {
                Text(stringResource(R.string.pairing_new_qr))
            }
        }
    }
}

@Composable
private fun CandidateEvidenceRow(ev: CandidateEvidence) {
    val c = hermesColors
    val status = when (ev.status) {
        CandidateEvidence.CandidateStatus.SELECTED -> stringResource(R.string.pairing_ev_selected)
        CandidateEvidence.CandidateStatus.ATTEMPTED_FAILED -> stringResource(R.string.pairing_ev_failed)
        CandidateEvidence.CandidateStatus.SKIPPED_NOT_ELIGIBLE -> stringResource(R.string.pairing_ev_skipped)
        CandidateEvidence.CandidateStatus.SKIPPED_EXPIRED -> stringResource(R.string.pairing_ev_expired)
        CandidateEvidence.CandidateStatus.SKIPPED_MALFORMED -> stringResource(R.string.pairing_ev_malformed)
        CandidateEvidence.CandidateStatus.SKIPPED_UNQUALIFIED -> stringResource(R.string.pairing_ev_unqualified)
        CandidateEvidence.CandidateStatus.PENDING -> stringResource(R.string.pairing_ev_pending)
    }
    Text(
        stringResource(R.string.pairing_evidence_row_fmt, ev.transportClass.name, ev.hostView, status) +
            (ev.typedCode?.let { " — $it" } ?: ""),
        style = HermesType.bodySecondary, color = c.textSecondary,
        modifier = Modifier.padding(start = 8.dp),
    )
}

private fun stateTitle(snap: PairingSessionController.Snapshot): String = when (snap.state) {
    PairingSessionController.State.UNPAIRED -> "Console non abbinata"
    PairingSessionController.State.DISCOVERING -> "Ricerca della Tower…"
    PairingSessionController.State.SCANNING -> "Lettura del QR…"
    PairingSessionController.State.PAYLOAD_PARSED -> "QR letto, verifica in corso…"
    PairingSessionController.State.PAYLOAD_REJECTED -> "QR rifiutato"
    PairingSessionController.State.ENDPOINTS_RESOLVING -> "Selezione dell'endpoint…"
    PairingSessionController.State.ENDPOINT_SELECTED -> "Endpoint selezionato"
    PairingSessionController.State.CONNECTING -> "Connessione alla Tower…"
    PairingSessionController.State.AWAITING_OWNER_CONFIRMATION -> "In attesa della conferma del proprietario…"
    PairingSessionController.State.PHRASE_MATCH_REQUIRED -> "Verifica la frase"
    PairingSessionController.State.GENERATING_DEVICE_KEY -> "Generazione della chiave del dispositivo…"
    PairingSessionController.State.CREDENTIAL_REQUESTED -> "Richiesta credenziale…"
    PairingSessionController.State.CREDENTIAL_ISSUED -> "Credenziale ricevuta"
    PairingSessionController.State.CONNECTION_VERIFYING -> "Test di connessione…"
    PairingSessionController.State.CAPABILITY_NEGOTIATING -> "Negoziazione capacità…"
    PairingSessionController.State.PAIRED -> "Console abbinata alla Tower"
    PairingSessionController.State.DEGRADED -> "Abbinata in modalità ridotta"
    PairingSessionController.State.EXPIRED -> "Sessione scaduta"
    PairingSessionController.State.REVOKED -> "Dispositivo revocato"
    PairingSessionController.State.FAILED -> "Abbinamento non riuscito"
    PairingSessionController.State.REPAIR_AVAILABLE -> "Aggiornamento Tower richiesto"
}

/**
 * ANDROID REPAIR CP-005 — the pairing METHOD chooser (mission section 4):
 * SCAN_QR (consigliata) / NUMERIC_CODE / LAN_DISCOVERY / TAILSCALE_MAGICDNS /
 * ADVANCED_MANUAL. Pairing is never buried: this screen IS the reachable
 * surface behind the Home card, the TOWER tab, onboarding and Diagnostics.
 */
@Composable
fun PairingFlowScreen(
    graph: AppGraph,
    onBack: () -> Unit,
    onOpenQrScanner: () -> Unit,
    onPaired: () -> Unit,
) {
    val c = hermesColors
    val scope = rememberCoroutineScope()
    var snap by remember { mutableStateOf<PairingSessionController.Snapshot?>(null) }
    var mode by remember { mutableStateOf<String?>(null) }
    var pairingIdInput by remember { mutableStateOf("") }
    var codeInput by remember { mutableStateOf("") }
    var endpointInput by remember { mutableStateOf("") }
    var manualError by remember { mutableStateOf<String?>(null) }
    val discovered by graph.nsdTowerDiscovery.towers.collectAsState()

    Column(
        Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ResponsiveScreenHeader(title = stringResource(R.string.pairing_title))
        MeanderBand(colors = hermesColors, modifier = Modifier.padding(top = 6.dp))
        Text(
            stringResource(R.string.pairing_choose_method),
            style = HermesType.bodySecondary, color = c.textSecondary,
        )

        val session = snap
        if (session != null) {
            SectionCard(title = stringResource(R.string.pairing_session_card)) {
                PairingSessionStateView(
                    graph = graph,
                    snap = session,
                    onPaired = onPaired,
                    onRestartScan = { snap = null; mode = null },
                )
            }
        }

        when (mode) {
            null -> {
                SectionCard(title = stringResource(R.string.pairing_method_qr_title)) {
                    Text(stringResource(R.string.pairing_method_qr_note), style = HermesType.bodySecondary, color = c.textSecondary)
                    HermesFilledButton(onClick = onOpenQrScanner) {
                        Text(stringResource(R.string.pairing_method_qr_button))
                    }
                }
                SectionCard(title = stringResource(R.string.pairing_method_numeric_title)) {
                    Text(stringResource(R.string.pairing_method_numeric_note), style = HermesType.bodySecondary, color = c.textSecondary)
                    TextButton(onClick = { mode = "numeric" }) { Text(stringResource(R.string.pairing_method_numeric_button)) }
                }
                SectionCard(title = stringResource(R.string.pairing_method_lan_title)) {
                    Text(stringResource(R.string.pairing_method_lan_note), style = HermesType.bodySecondary, color = c.textSecondary)
                    TextButton(onClick = { mode = "lan"; graph.nsdTowerDiscovery.start() }) {
                        Text(stringResource(R.string.pairing_method_lan_button))
                    }
                }
                SectionCard(title = stringResource(R.string.pairing_method_tailscale_title)) {
                    Text(stringResource(R.string.pairing_method_tailscale_note), style = HermesType.bodySecondary, color = c.textSecondary)
                    TextButton(onClick = { mode = "tailscale" }) { Text(stringResource(R.string.pairing_method_tailscale_button)) }
                }
                SectionCard(title = stringResource(R.string.pairing_method_manual_title)) {
                    Text(stringResource(R.string.pairing_method_manual_note), style = HermesType.bodySecondary, color = c.textSecondary)
                    TextButton(onClick = { mode = "manual" }) { Text(stringResource(R.string.pairing_method_manual_button)) }
                }
            }
            "numeric", "tailscale" -> {
                SectionCard(title = stringResource(R.string.pairing_code_card_title)) {
                    OutlinedTextField(
                        value = pairingIdInput,
                        onValueChange = { pairingIdInput = it },
                        label = { Text(stringResource(R.string.pairing_id_label)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = codeInput,
                        onValueChange = { if (it.length <= 6) codeInput = it.filter { ch -> ch.isDigit() } },
                        label = { Text(stringResource(R.string.pairing_code_label)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = endpointInput,
                        onValueChange = { endpointInput = it },
                        label = { Text(stringResource(if (mode == "tailscale") R.string.pairing_tailscale_label else R.string.pairing_manual_label)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    manualError?.let { Text(it, style = HermesType.body, color = c.textInfo) }
                    HermesFilledButton(
                        enabled = pairingIdInput.isNotBlank() && codeInput.length == 6 && endpointInput.isNotBlank(),
                        onClick = {
                            val parsed = com.hermestower.voiceconsole.pairing.ManualEndpointParser.parse(endpointInput)
                            if (parsed.candidate == null) {
                                manualError = parsed.detail ?: "indirizzo non valido"
                                return@HermesFilledButton
                            }
                            manualError = null
                            scope.launch {
                                snap = graph.pairingSessionController.startFromCode(
                                    pairingId = pairingIdInput.trim(),
                                    claimCode = codeInput,
                                    label = "Console vocale Android",
                                    candidates = listOf(parsed.candidate),
                                )
                            }
                        },
                    ) { Text(stringResource(R.string.pairing_connect_button)) }
                    Text(
                        stringResource(R.string.pairing_numeric_endpoint_note),
                        style = HermesType.bodySecondary, color = c.textSecondary,
                    )
                }
            }
            "lan" -> {
                SectionCard(title = stringResource(R.string.pairing_lan_card_title)) {
                    var selectedHost by remember { mutableStateOf<String?>(null) }
                    if (!graph.nsdTowerDiscovery.isAvailable) {
                        Text(stringResource(R.string.pairing_lan_unavailable), style = HermesType.body, color = c.textInfo)
                    } else {
                        if (discovered.isEmpty()) {
                            Text(stringResource(R.string.pairing_lan_searching), style = HermesType.bodySecondary, color = c.textSecondary)
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        } else {
                            Text(stringResource(R.string.pairing_lan_found), style = HermesType.body, color = c.textPrimary)
                            discovered.forEach { tower ->
                                val selected = selectedHost == "${tower.host}:${tower.port}"
                                OutlinedButton(onClick = { selectedHost = "${tower.host}:${tower.port}" }) {
                                    Text(
                                        (if (selected) "● " else "○ ") + "${tower.serviceName} — ${tower.host}:${tower.port}",
                                        style = HermesType.body, color = c.textPrimary,
                                    )
                                }
                            }
                        }
                        Text(stringResource(R.string.pairing_lan_code_note), style = HermesType.bodySecondary, color = c.textSecondary)
                        OutlinedTextField(
                            value = pairingIdInput,
                            onValueChange = { pairingIdInput = it },
                            label = { Text(stringResource(R.string.pairing_id_label)) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        OutlinedTextField(
                            value = codeInput,
                            onValueChange = { if (it.length <= 6) codeInput = it.filter { ch -> ch.isDigit() } },
                            label = { Text(stringResource(R.string.pairing_code_label)) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        manualError?.let { Text(it, style = HermesType.body, color = c.textInfo) }
                        HermesFilledButton(
                            enabled = pairingIdInput.isNotBlank() && codeInput.length == 6 && selectedHost != null,
                            onClick = {
                                val tower = discovered.firstOrNull { "${it.host}:${it.port}" == selectedHost }
                                if (tower == null) {
                                    manualError = "nessuna Tower selezionata"
                                    return@HermesFilledButton
                                }
                                manualError = null
                                scope.launch {
                                    snap = graph.pairingSessionController.startFromCode(
                                        pairingId = pairingIdInput.trim(),
                                        claimCode = codeInput,
                                        label = "Console vocale Android",
                                        candidates = listOf(tower.toCandidate()),
                                    )
                                }
                            },
                        ) { Text(stringResource(R.string.pairing_connect_button)) }
                    }
                }
            }
            "manual" -> {
                SectionCard(title = stringResource(R.string.pairing_manual_card_title)) {
                    OutlinedTextField(
                        value = endpointInput,
                        onValueChange = { endpointInput = it },
                        label = { Text(stringResource(R.string.pairing_manual_label)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = codeInput,
                        onValueChange = { if (it.length <= 6) codeInput = it.filter { ch -> ch.isDigit() } },
                        label = { Text(stringResource(R.string.pairing_code_label)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    manualError?.let { Text(it, style = HermesType.body, color = c.textInfo) }
                    HermesFilledButton(
                        enabled = endpointInput.isNotBlank() && codeInput.length == 6,
                        onClick = {
                            val parsed = com.hermestower.voiceconsole.pairing.ManualEndpointParser.parse(endpointInput)
                            if (parsed.candidate == null) {
                                manualError = parsed.detail ?: "indirizzo non valido"
                                return@HermesFilledButton
                            }
                            manualError = null
                            scope.launch {
                                snap = graph.pairingSessionController.startFromCode(
                                    pairingId = pairingIdInput.ifBlank { endpointInput },
                                    claimCode = codeInput,
                                    label = "Console vocale Android",
                                    candidates = listOf(parsed.candidate),
                                )
                            }
                        },
                    ) { Text(stringResource(R.string.pairing_connect_button)) }
                    Text(
                        stringResource(R.string.pairing_manual_trust_note),
                        style = HermesType.bodySecondary, color = c.textSecondary,
                    )
                }
            }
        }

        TextButton(onClick = onBack) { Text(stringResource(R.string.qr_back)) }
    }
}
