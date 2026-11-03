package com.hermestower.voiceconsole.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.FactCheck
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.hermestower.voiceconsole.R
import com.hermestower.voiceconsole.ui.AppRoutes
import com.hermestower.voiceconsole.ui.components.MeanderBand
import com.hermestower.voiceconsole.ui.components.SectionCard
import com.hermestower.voiceconsole.ui.monumental.MonumentalType
import com.hermestower.voiceconsole.ui.monumental.monumentalColors
import com.hermestower.voiceconsole.ui.theme.HermesType
import com.hermestower.voiceconsole.ui.theme.hermesColors

/**
 * CP-G4-017 (directive §5) — SETTINGS HUB, the fifth primary destination.
 *
 * The ordinary product surfaces ONLY five destinations (Console, Tower Archive,
 * Connect to Hermes Tower, Models and Voice, Settings). Everything technical
 * lives here, organized in the six prescribed categories. Each entry navigates
 * to the EXISTING screen — no screen logic moves; only the doorway changes.
 */
@Composable
fun SettingsHubScreen(
    onOpen: (String) -> Unit,
) {
    val c = hermesColors
    val m = monumentalColors

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(stringResource(R.string.settings_title), style = HermesType.screenTitle, color = c.ink)
        MeanderBand(colors = hermesColors, modifier = Modifier.padding(top = 6.dp))
        Text(
            stringResource(R.string.settings_subtitle),
            style = HermesType.bodySecondary,
            color = c.inkSecondary,
        )

        // ---- GENERAL ----
        SectionCard(title = stringResource(R.string.settings_cat_general)) {
            SettingsRow(
                label = stringResource(R.string.settings_general_language),
                sub = stringResource(R.string.settings_general_language_sub),
                icon = Icons.Outlined.Tune,
                onClick = { onOpen(AppRoutes.APPEARANCE) },
            )
            SettingsRow(
                label = stringResource(R.string.settings_general_behavior),
                sub = stringResource(R.string.settings_general_behavior_sub),
                icon = Icons.Outlined.Tune,
                onClick = { onOpen(AppRoutes.APPEARANCE) },
            )
        }

        // ---- VOICE ----
        SectionCard(title = stringResource(R.string.settings_cat_voice)) {
            SettingsRow(
                label = stringResource(R.string.settings_voice_controls),
                sub = stringResource(R.string.settings_voice_controls_sub),
                icon = Icons.Outlined.Mic,
                onClick = { onOpen(AppRoutes.VOICE) },
            )
        }

        // ---- TOWER CONNECTION ----
        SectionCard(title = stringResource(R.string.settings_cat_tower)) {
            SettingsRow(
                label = stringResource(R.string.settings_tower_status),
                sub = stringResource(R.string.settings_tower_status_sub),
                icon = Icons.Outlined.Link,
                onClick = { onOpen(AppRoutes.CONNECTION) },
            )
            SettingsRow(
                label = stringResource(R.string.settings_tower_studio),
                sub = stringResource(R.string.settings_tower_studio_sub),
                icon = Icons.Outlined.Settings,
                onClick = { onOpen(AppRoutes.TOWER_STUDIO) },
            )
        }

        // ---- MODELS ----
        SectionCard(title = stringResource(R.string.settings_cat_models)) {
            SettingsRow(
                label = stringResource(R.string.settings_models_manager),
                sub = stringResource(R.string.settings_models_manager_sub),
                icon = Icons.Outlined.Memory,
                onClick = { onOpen(AppRoutes.MODEL_MANAGER) },
            )
        }

        // ---- DISPLAY ----
        SectionCard(title = stringResource(R.string.settings_cat_display)) {
            SettingsRow(
                label = stringResource(R.string.settings_display_visual),
                sub = stringResource(R.string.settings_display_visual_sub),
                icon = Icons.Outlined.Palette,
                onClick = { onOpen(AppRoutes.APPEARANCE) },
            )
        }

        // ---- DIAGNOSTICS ----
        SectionCard(title = stringResource(R.string.settings_cat_diagnostics)) {
            SettingsRow(
                label = stringResource(R.string.settings_diag_selftest),
                sub = stringResource(R.string.settings_diag_selftest_sub),
                icon = Icons.Outlined.FactCheck,
                onClick = { onOpen(AppRoutes.SELF_TEST) },
            )
            SettingsRow(
                label = stringResource(R.string.settings_diag_engine),
                sub = stringResource(R.string.settings_diag_engine_sub),
                icon = Icons.Outlined.BugReport,
                onClick = { onOpen(AppRoutes.DIAGNOSTICS) },
            )
            SettingsRow(
                label = stringResource(R.string.settings_diag_intelligence),
                sub = stringResource(R.string.settings_diag_intelligence_sub),
                icon = Icons.Outlined.Psychology,
                onClick = { onOpen(AppRoutes.INTELLIGENCE) },
            )
            SettingsRow(
                label = stringResource(R.string.settings_diag_history),
                sub = stringResource(R.string.settings_diag_history_sub),
                icon = Icons.Outlined.History,
                onClick = { onOpen(AppRoutes.HISTORY) },
            )
            SettingsRow(
                label = stringResource(R.string.settings_diag_about),
                sub = stringResource(R.string.settings_diag_about_sub),
                icon = Icons.Outlined.Info,
                onClick = { onOpen(AppRoutes.ABOUT) },
            )
        }

        Text(
            stringResource(R.string.settings_note_advanced),
            style = MonumentalType.sealSub,
            color = m.inkDim,
            modifier = Modifier.padding(bottom = 16.dp),
        )
    }
}

@Composable
private fun SettingsRow(
    label: String,
    sub: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClickLabel = sub) { onClick() },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Column(Modifier.padding(start = 12.dp)) {
            Text(label, style = HermesType.body)
            Text(sub, style = HermesType.bodySecondary, color = hermesColors.inkSecondary)
        }
    }
}
