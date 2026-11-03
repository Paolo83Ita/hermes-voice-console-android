package com.hermestower.voiceconsole.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.hermestower.voiceconsole.R as Res
import com.hermestower.voiceconsole.app.AppGraph
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

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(stringResource(Res.string.about_title), style = HermesType.screenTitle, color = c.ink)
        MeanderBand(colors = hermesColors, modifier = Modifier.padding(top = 6.dp))

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
