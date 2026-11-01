package com.hermestower.voiceconsole.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hermestower.voiceconsole.app.AppGraph
import com.hermestower.voiceconsole.ui.components.ModeChip
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
        Text("Appearance", style = HermesType.screenTitle, color = c.ink)
        Text("ARCHITECTURAL FORGE · VISUAL CALIBRATION CHAMBER", style = MonumentalType.sealSub, color = m.gold)

        SectionCard(title = "Visual quality (Monumental core)") {
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
                    VisualQuality.LOW -> "LOW: static or nearly static core, no glow, effect cadence 5–15 fps. Intended for thermal pressure and reduced motion."
                    VisualQuality.BALANCED -> "BALANCED (default): 30 fps complex animation target, moderate particles, layered assets, limited glow."
                    VisualQuality.HIGH -> "HIGH: richer particles and more ring layers. Only after physical-device qualification; falls back automatically under pressure."
                },
                style = HermesType.bodySecondary,
                color = c.inkSecondary,
                modifier = Modifier.padding(top = 6.dp),
            )
            Text(
                "Thermal policy uses real device temperature reads; TARGET_PENDING_DEVICE_QUALIFICATION until Xiaomi measurements exist.",
                style = HermesType.bodySecondary,
                color = m.inkDim,
            )
        }

        SectionCard(title = "Theme") {
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
                "Obsidian Night is the default identity (decision D-009) and the base of the Monumental Oracle Tower identity: lowest power draw, lowest burn-in stress and correct night ambience for an always-on AMOLED console. Marble Light is the daylight / inspection theme.",
                style = HermesType.bodySecondary,
                color = c.inkSecondary,
            )
        }

        SectionCard(title = "Brightness") {
            Text("Idle dimming scrim: ${"%.0f".format(settings.brightnessScrim * 100)} %", style = HermesType.body, color = c.ink)
            Slider(
                value = settings.brightnessScrim,
                onValueChange = { graph.store.updateSettings { s -> s.copy(brightnessScrim = it) } },
                valueRange = 0f..0.6f,
            )
            Text(
                "A truthful dimming overlay (FR-41). Scheduled screen rest and the black idle option are specified in FR-42 (AMOLED_AND_BURN_IN_SAFETY.md).",
                style = HermesType.bodySecondary,
                color = c.inkSecondary,
            )
        }

        SectionCard(title = "Motion") {
            Text("Animation intensity: ${"%.0f".format(settings.animationIntensity * 100)} %", style = HermesType.body, color = c.ink)
            Slider(
                value = settings.animationIntensity,
                onValueChange = { graph.store.updateSettings { s -> s.copy(animationIntensity = it) } },
                valueRange = 0.2f..1.5f,
            )
            SettingRow(
                label = "Reduced motion (static mid-state rendering of every state)",
                checked = settings.reducedMotion,
                enabled = true,
                onToggle = { graph.store.updateSettings { s -> s.copy(reducedMotion = it) } },
            )
        }

        SectionCard(title = "Display safety") {
            SettingRow(
                label = "Burn-in protection (per-region bounded drift, 25 s cadence)",
                checked = settings.burnInGuard,
                enabled = true,
                onToggle = { graph.store.updateSettings { s -> s.copy(burnInGuard = it) } },
            )
            SettingRow(
                label = "Keep screen on while console is open",
                checked = settings.keepScreenOn,
                enabled = true,
                onToggle = { graph.store.updateSettings { s -> s.copy(keepScreenOn = it) } },
            )
            Text(
                "The Graph 3 safeguard drifts title, core, seals, transcript and medallions independently (AMOLED_AND_BURN_IN_SAFETY.md §2).",
                style = HermesType.bodySecondary,
                color = c.inkSecondary,
            )
        }

        SectionCard(title = "Kiosk behavior") {
            TruthBadge("CONTROLLED CAPABILITY — V0.7", c.inkSecondary)
            Text(
                "Kiosk lockdown (single-app, owner-controlled exit) is deliberately deferred: it requires an explicit Owner decision and enclosure-finalized testing (FR-46).",
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
    Column(
        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
        modifier = Modifier.padding(4.dp),
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
            Text(if (selected) "ACTIVE" else "USE", style = HermesType.pill)
        }
    }
}
