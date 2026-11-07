package com.hermestower.voiceconsole.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import android.app.Activity
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hermestower.voiceconsole.app.AppGraph
import com.hermestower.voiceconsole.locale.AppLanguage
import com.hermestower.voiceconsole.R as Res
import com.hermestower.voiceconsole.ui.components.ModeChip
import com.hermestower.voiceconsole.ui.components.MeanderBand
import com.hermestower.voiceconsole.ui.components.ResponsiveScreenHeader
import com.hermestower.voiceconsole.ui.components.SectionCard
import com.hermestower.voiceconsole.ui.components.TruthBadge
import com.hermestower.voiceconsole.ui.monumental.MonumentalType
import com.hermestower.voiceconsole.ui.monumental.VisualQuality
import com.hermestower.voiceconsole.ui.monumental.monumentalColors
import com.hermestower.voiceconsole.ui.theme.HermesType
import com.hermestower.voiceconsole.ui.theme.MarbleLightColors
import com.hermestower.voiceconsole.ui.theme.ObsidianNightColors
import com.hermestower.voiceconsole.ui.theme.hermesColors

/**
 * S6 Appearance (FR-35) — GRAPH 3 architectural forge.
 * Adds the Monumental quality profile selector (LOW / BALANCED / HIGH, mission §14)
 * on top of the preserved theme, brightness, motion and display-safety controls.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AppearanceScreen(graph: AppGraph) {
    val c = hermesColors
    val m = monumentalColors
    val settings by graph.store.settings.collectAsStateWithLifecycle()

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ResponsiveScreenHeader(title = stringResource(Res.string.appearance_title))
        MeanderBand(colors = hermesColors, modifier = Modifier.padding(top = 6.dp))
        Text(stringResource(Res.string.appearance_subtitle), style = MonumentalType.sealSub, color = m.gold)

        SectionCard(title = stringResource(Res.string.appearance_quality_card)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                VisualQuality.entries.forEach { q ->
                    ModeChip(
                        label = q.id,
                        selected = VisualQuality.from(settings.visualQuality) == q,
                        enabled = true,
                        onClick = { graph.store.updateSettings { s -> s.copy(visualQuality = q.id) } },
                    )
                }
            }
            Text(
                when (VisualQuality.from(settings.visualQuality)) {
                    VisualQuality.LOW -> stringResource(Res.string.appearance_quality_low)
                    VisualQuality.BALANCED -> stringResource(Res.string.appearance_quality_balanced)
                    VisualQuality.HIGH -> stringResource(Res.string.appearance_quality_high)
                },
                style = HermesType.bodySecondary,
                color = c.inkSecondary,
                modifier = Modifier.padding(top = 6.dp),
            )
            Text(
                stringResource(Res.string.appearance_thermal_policy),
                style = HermesType.bodySecondary,
                color = m.inkDim,
            )
        }

        SectionCard(title = stringResource(Res.string.appearance_theme)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ThemeSwatch(
                    label = "MARBLE LIGHT",
                    selected = !settings.darkTheme,
                    colors = MarbleLightColors,
                    onClick = { graph.store.updateSettings { s -> s.copy(darkTheme = false) } },
                )
                ThemeSwatch(
                    label = "OBSIDIAN NIGHT",
                    selected = settings.darkTheme,
                    colors = ObsidianNightColors,
                    onClick = { graph.store.updateSettings { s -> s.copy(darkTheme = true) } },
                )
            }
            Text(
                stringResource(Res.string.appearance_theme_note),
                style = HermesType.bodySecondary,
                color = c.inkSecondary,
            )
        }

        SectionCard(title = stringResource(Res.string.appearance_brightness)) {
            Text(
                stringResource(Res.string.appearance_idle_dimming, "%.0f".format(settings.brightnessScrim * 100)),
                style = HermesType.body,
                color = c.ink,
            )
            Slider(
                value = settings.brightnessScrim,
                onValueChange = { graph.store.updateSettings { s -> s.copy(brightnessScrim = it) } },
                valueRange = 0f..0.6f,
            )
            Text(
                stringResource(Res.string.appearance_dimming_note),
                style = HermesType.bodySecondary,
                color = c.inkSecondary,
            )
        }

        SectionCard(title = stringResource(Res.string.appearance_motion)) {
            Text(
                stringResource(Res.string.appearance_animation_intensity, "%.0f".format(settings.animationIntensity * 100)),
                style = HermesType.body,
                color = c.ink,
            )
            Slider(
                value = settings.animationIntensity,
                onValueChange = { graph.store.updateSettings { s -> s.copy(animationIntensity = it) } },
                valueRange = 0.2f..1.5f,
            )
            SettingRow(
                label = stringResource(Res.string.appearance_reduced_motion),
                checked = settings.reducedMotion,
                enabled = true,
                onToggle = { graph.store.updateSettings { s -> s.copy(reducedMotion = it) } },
            )
            Text(
                stringResource(Res.string.appearance_reduced_motion_note),
                style = HermesType.bodySecondary,
                color = c.inkSecondary,
            )
        }

        SectionCard(title = stringResource(Res.string.appearance_display_safety)) {
            SettingRow(
                label = stringResource(Res.string.appearance_burn_in),
                checked = settings.burnInGuard,
                enabled = true,
                onToggle = { graph.store.updateSettings { s -> s.copy(burnInGuard = it) } },
            )
            Text(
                stringResource(Res.string.appearance_burn_in_note),
                style = HermesType.bodySecondary,
                color = c.inkSecondary,
            )
            SettingRow(
                label = stringResource(Res.string.appearance_keep_screen_on),
                checked = settings.keepScreenOn,
                enabled = true,
                onToggle = { graph.store.updateSettings { s -> s.copy(keepScreenOn = it) } },
            )
        }

        SectionCard(title = stringResource(Res.string.appearance_kiosk)) {
            Text(
                stringResource(Res.string.language_note),
                style = HermesType.bodySecondary,
                color = c.inkSecondary,
            )
            val context = LocalContext.current
            var currentLang by remember { mutableStateOf(AppLanguage.stored(context)) }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                LanguageChip(
                    code = AppLanguage.FOLLOW_SYSTEM,
                    label = stringResource(Res.string.lang_follow_system),
                    selected = currentLang == AppLanguage.FOLLOW_SYSTEM,
                    onSelect = {
                        AppLanguage.apply(context, AppLanguage.FOLLOW_SYSTEM)
                        currentLang = AppLanguage.FOLLOW_SYSTEM
                        // OWNER PHYSICAL FEEDBACK 3: the spoken voice follows the
                        // app language immediately (no recreate dependency).
                        runCatching { graph.voiceTts.applySpokenLocale() }
                        // GRAPH 4.12 §5: recreation posted to the NEXT frame —
                        // never inside the click handler (the proven crash window).
                        (context as? Activity)?.let {
                            com.hermestower.voiceconsole.ui.runtime.ActivityRecreation.recreateNextFrame(it)
                        }
                    },
                )
                listOf("it" to Res.string.lang_it, "en" to Res.string.lang_en, "es" to Res.string.lang_es, "fr" to Res.string.lang_fr, "de" to Res.string.lang_de)
                    .forEach { (code, res) ->
                        LanguageChip(
                            code = code,
                            label = stringResource(res),
                            selected = currentLang == code,
                            onSelect = {
                                AppLanguage.apply(context, code)
                                currentLang = code
                                // OWNER PHYSICAL FEEDBACK 3: the spoken voice
                                // follows the app language immediately.
                                runCatching { graph.voiceTts.applySpokenLocale() }
                                // GRAPH 4.12 §5: recreation posted to the NEXT frame.
                                (context as? Activity)?.let {
                                    com.hermestower.voiceconsole.ui.runtime.ActivityRecreation.recreateNextFrame(it)
                                }
                            },
                        )
                    }
            }
        }

        SectionCard(title = stringResource(Res.string.appearance_kiosk)) {
            TruthBadge(stringResource(Res.string.truth_controlled_capability), c.inkSecondary)
            Text(
                stringResource(Res.string.appearance_kiosk_note),
                style = HermesType.bodySecondary,
                color = c.inkSecondary,
            )
        }
    }
}

@Composable
private fun ThemeSwatch(
    label: String,
    selected: Boolean,
    colors: com.hermestower.voiceconsole.ui.theme.HermesColors,
    onClick: () -> Unit,
) {
    // CP-G4-007 accessibility-semantics repair (Owner §3): the whole swatch card
    // is the control — the label text merges into a clickable node (onClickLabel
    // names the action for TalkBack; E2E may click the label). The USE/ACTIVE
    // affordance below stays as the visible selected-state badge.
    Column(
        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
        modifier = Modifier
            .padding(4.dp)
            .clickable(onClickLabel = label, onClick = onClick),
    ) {
        Box(
            Modifier
                .fillMaxWidth(0.42f)
                .height(52.dp)
                .border(
                    width = if (selected) 2.dp else 1.dp,
                    color = if (selected) colors.bronze else colors.outline,
                    shape = MaterialTheme.shapes.medium,
                )
                .background(colors.background, MaterialTheme.shapes.medium),
        ) {
            Row(Modifier.padding(8.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Box(Modifier.height(36.dp).weight(1f).background(colors.stone, MaterialTheme.shapes.small))
                Box(Modifier.height(36.dp).weight(1f).background(colors.bronze, MaterialTheme.shapes.small))
                Box(Modifier.height(36.dp).weight(1f).background(colors.energyBright, MaterialTheme.shapes.small))
            }
        }
        Text(
            label,
            style = HermesType.pill,
            color = if (selected) colors.bronze else colors.inkSecondary,
            modifier = Modifier.padding(top = 4.dp),
        )
        androidx.compose.material3.TextButton(onClick = onClick) {
            Text(
                if (selected) stringResource(Res.string.appearance_chip_active)
                else stringResource(Res.string.appearance_chip_use),
                style = HermesType.pill,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LanguageChip(
    code: String,
    label: String,
    selected: Boolean,
    onSelect: () -> Unit,
) {
    ModeChip(label = label, selected = selected, enabled = true, onClick = onSelect)
}
