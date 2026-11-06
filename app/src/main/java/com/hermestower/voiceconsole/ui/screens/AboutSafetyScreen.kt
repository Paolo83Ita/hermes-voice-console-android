package com.hermestower.voiceconsole.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.hermestower.voiceconsole.R as Res
import com.hermestower.voiceconsole.app.AppGraph
import com.hermestower.voiceconsole.app.BuildIdentity
import com.hermestower.voiceconsole.ui.components.SectionCard
import com.hermestower.voiceconsole.ui.components.MeanderBand
import com.hermestower.voiceconsole.ui.components.TruthBadge
import com.hermestower.voiceconsole.ui.theme.HermesType
import com.hermestower.voiceconsole.ui.theme.hermesColors

/**
 * S8 About & Safety (FR-37): component versions, licenses, privacy explanation,
 * known limitations, reset local data. Honest limitation list included.
 */
@Composable
fun AboutSafetyScreen(graph: AppGraph) {
    val c = hermesColors
    var confirmReset by remember { mutableStateOf(false) }
    val clipboard = LocalClipboardManager.current
    // OWNER PHYSICAL RETEST §1: the identity is measured ONCE per composition
    // (the streamed SHA-256 over the real installed APK is not free).
    val injected = remember { BuildIdentity.injected() }
    val appContext = androidx.compose.ui.platform.LocalContext.current.applicationContext
    val measured = remember { BuildIdentity.measure(appContext) }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(stringResource(Res.string.about_title), style = HermesType.screenTitle, color = c.ink)
        MeanderBand(colors = hermesColors, modifier = Modifier.padding(top = 6.dp))

        // ---- OWNER PHYSICAL RETEST §1: BUILD INFORMATION (the in-app proof) ----
        SectionCard(title = stringResource(Res.string.about_build_card)) {
            BuildIdentityRow(stringResource(Res.string.about_build_label), injected.buildLabel)
            BuildIdentityRow(stringResource(Res.string.about_build_version), "${injected.versionName} (${injected.versionCode})")
            BuildIdentityRow(stringResource(Res.string.about_build_package), injected.packageName)
            BuildIdentityRow(
                stringResource(Res.string.about_build_git),
                injected.gitShaFull.ifBlank { stringResource(Res.string.about_build_local) },
            )
            BuildIdentityRow(stringResource(Res.string.about_build_timestamp), injected.buildTimestampIso8601)
            BuildIdentityRow(
                stringResource(Res.string.about_build_run),
                injected.workflowRunId.ifBlank { stringResource(Res.string.about_build_local) },
            )
            BuildIdentityRow(stringResource(Res.string.about_build_variant), measured.apkVariant)
            measured.supportedAbisInApk.takeIf { it.isNotEmpty() }?.let {
                BuildIdentityRow(stringResource(Res.string.about_build_abis), it.joinToString())
            }
            BuildIdentityRow(
                stringResource(Res.string.about_build_apk_sha256),
                measured.apkSha256 ?: stringResource(Res.string.diag_unavailable),
            )
            measured.apkSizeBytes?.let {
                BuildIdentityRow(stringResource(Res.string.about_build_apk_size), "$it B")
            }
            BuildIdentityRow(
                stringResource(Res.string.about_build_installed_at),
                measured.installationTimestampMs?.let { java.time.Instant.ofEpochMilli(it).toString() }
                    ?: stringResource(Res.string.diag_unavailable),
            )
            BuildIdentityRow(
                stringResource(Res.string.about_build_updated_at),
                measured.lastUpdateTimestampMs?.let { java.time.Instant.ofEpochMilli(it).toString() }
                    ?: stringResource(Res.string.diag_unavailable),
            )
            if (measured.processStartElapsedMs >= 0) {
                BuildIdentityRow(
                    stringResource(Res.string.about_build_process_start),
                    measured.processStartElapsedMs.toString(),
                )
            }
            Text(
                stringResource(Res.string.about_build_note),
                style = HermesType.bodySecondary,
                color = c.inkSecondary,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = {
                    clipboard.setText(
                        AnnotatedString(
                            BuildIdentity.format(injected, measured) { ms ->
                                ms?.let { java.time.Instant.ofEpochMilli(it).toString() }
                            },
                        ),
                    )
                }) { Text(stringResource(Res.string.about_build_copy)) }
                TextButton(onClick = {
                    clipboard.setText(AnnotatedString(graph.exportDiagnosticReceipt()))
                }) { Text(stringResource(Res.string.about_build_export_receipt)) }
            }
        }

        SectionCard(title = stringResource(Res.string.about_version_card)) {
            Text(
                stringResource(
                    Res.string.about_version_fmt,
                    com.hermestower.voiceconsole.BuildConfig.VERSION_NAME,
                    com.hermestower.voiceconsole.BuildConfig.VERSION_CODE,
                ),
                style = HermesType.body,
                color = c.ink,
            )
            TruthBadge(stringResource(Res.string.about_badge_foundation), c.gold)
        }

        SectionCard(title = stringResource(Res.string.about_licenses_card)) {
            val components = listOf(
                "Kotlin 2.0.21 — Apache-2.0",
                "Jetpack Compose BOM 2024.12.01 (ui, material3, icons) — Apache-2.0",
                "AndroidX Activity/Navigation/Lifecycle — Apache-2.0",
                "Kotlinx Coroutines 1.9.0 / Serialization 1.7.3 — Apache-2.0",
                "JUnit 4.13.2 (tests) — EPL-1.0",
            )
            components.forEach { Text(it, style = HermesType.body, color = c.ink) }
            Text(
                stringResource(Res.string.about_licenses_note),
                style = HermesType.bodySecondary,
                color = c.inkSecondary,
            )
        }

        SectionCard(title = stringResource(Res.string.about_privacy_card)) {
            Text(
                stringResource(Res.string.about_privacy_note),
                style = HermesType.body,
                color = c.ink,
            )
            Text(
                stringResource(Res.string.about_privacy_note_v03),
                style = HermesType.bodySecondary,
                color = c.inkSecondary,
            )
        }

        SectionCard(title = stringResource(Res.string.about_limits_card)) {
            val limits = listOf(
                Res.string.about_limit_mic,
                Res.string.about_limit_bridge,
                Res.string.about_limit_model,
                Res.string.about_limit_tts,
                Res.string.about_limit_service,
                Res.string.about_limit_history,
                Res.string.about_limit_language,
                Res.string.about_limit_build,
            )
            limits.forEach { Text("• " + stringResource(it), style = HermesType.body, color = c.ink) }
        }

        SectionCard(title = stringResource(Res.string.about_danger_card)) {
            Button(onClick = { confirmReset = true }) { Text(stringResource(Res.string.about_reset_button)) }
            Text(
                stringResource(Res.string.about_reset_note),
                style = HermesType.bodySecondary,
                color = c.inkSecondary,
            )
        }
    }

    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            title = { Text(stringResource(Res.string.about_reset_dialog_title)) },
            text = { Text(stringResource(Res.string.about_reset_dialog_text)) },
            confirmButton = {
                Button(onClick = {
                    graph.store.resetLocalData()
                    graph.settingsRepository.clear()
                    graph.store.updateSettings { graph.settingsRepository.load() }
                    confirmReset = false
                }) { Text(stringResource(Res.string.common_reset)) }
            },
            dismissButton = { TextButton(onClick = { confirmReset = false }) { Text(stringResource(Res.string.common_cancel)) } },
        )
    }
}

/** One label/value row of the build-identity panel (value column weighted — no overflow at 360 dp). */
@Composable
private fun BuildIdentityRow(label: String, value: String) {
    androidx.compose.foundation.layout.Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 1.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            label,
            style = HermesType.bodySecondary,
            color = hermesColors.inkSecondary,
            modifier = Modifier.weight(1.1f),
        )
        SelectionContainer {
            Text(
                value,
                style = HermesType.body,
                color = hermesColors.ink,
                modifier = Modifier.weight(1.9f),
            )
        }
    }
}
