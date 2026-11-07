package com.hermestower.voiceconsole.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.hermestower.voiceconsole.R
import com.hermestower.voiceconsole.app.AppGraph
import com.hermestower.voiceconsole.app.FirstRunSafeMode

/**
 * GRAPH 4.12 §3/§4 — FIRST-RUN CRASH RECOVERY DIALOG.
 *
 * Shown on the launch AFTER a first-run crash receipt was persisted:
 *   "L'app si è chiusa durante il primo avvio."
 * with the four typed recovery actions:
 *   - Esporta dati diagnostici (secret-clean receipt + diagnostics receipt);
 *   - Continua in modalità sicura (FIRST_RUN_SAFE_MODE: engines stay cold);
 *   - Riprova configurazione (onboarding restarts from step 1);
 *   - Apri Console senza attivare i motori (safe mode + straight to Console).
 *
 * In safe mode the panel offers the SINGLE-SUBSYSTEM initializers so the
 * crashing subsystem can be isolated by evidence.
 */
@Composable
fun FirstRunRecoveryDialog(
    graph: AppGraph,
    onDismissToConsole: () -> Unit,
    onRetryOnboarding: () -> Unit,
    onExported: (String) -> Unit,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    AlertDialog(
        onDismissRequest = onDismissToConsole,
        title = { Text(stringResource(R.string.recovery_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(stringResource(R.string.recovery_message))
                Text(
                    stringResource(R.string.recovery_safe_mode_note),
                    style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                )
            }
        },
        confirmButton = {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                TextButton(
                    onClick = {
                        FirstRunSafeMode.enable(context)
                        com.hermestower.voiceconsole.app.FirstRunCrashEvidence.clear(context)
                        onDismissToConsole()
                    },
                ) { Text(stringResource(R.string.recovery_action_safe_mode)) }
                TextButton(
                    onClick = {
                        // Console without engines = safe mode, straight to Console.
                        FirstRunSafeMode.enable(context)
                        com.hermestower.voiceconsole.app.FirstRunCrashEvidence.clear(context)
                        onDismissToConsole()
                    },
                ) { Text(stringResource(R.string.recovery_action_console_no_engines)) }
                TextButton(
                    onClick = {
                        com.hermestower.voiceconsole.app.FirstRunCrashEvidence.clear(context)
                        onRetryOnboarding()
                    },
                ) { Text(stringResource(R.string.recovery_action_retry_onboarding)) }
                TextButton(
                    onClick = {
                        val receipt = runCatching { graph.exportDiagnosticReceipt() }
                            .getOrDefault("diagnostics unavailable")
                        onExported(receipt)
                    },
                ) { Text(stringResource(R.string.recovery_action_export)) }
            }
        },
    )
}
