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
import androidx.compose.ui.unit.dp
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
        Text("About & Safety", style = HermesType.screenTitle, color = c.ink)
        MeanderBand(colors = hermesColors, modifier = Modifier.padding(top = 6.dp))

        SectionCard(title = "Version") {
            Text("Hermes Voice Console ${com.hermestower.voiceconsole.BuildConfig.VERSION_NAME} (build ${com.hermestower.voiceconsole.BuildConfig.VERSION_CODE})", style = HermesType.body, color = c.ink)
            TruthBadge("V0.1 — FOUNDATION AND VISUAL PROTOTYPE", c.gold)
        }

        SectionCard(title = "Components and licenses") {
            val components = listOf(
                "Kotlin 2.0.21 — Apache-2.0",
                "Jetpack Compose BOM 2024.12.01 (ui, material3, icons) — Apache-2.0",
                "AndroidX Activity/Navigation/Lifecycle — Apache-2.0",
                "Kotlinx Coroutines 1.9.0 / Serialization 1.7.3 — Apache-2.0",
                "JUnit 4.13.2 (tests) — EPL-1.0",
            )
            components.forEach { Text(it, style = HermesType.body, color = c.ink) }
            Text(
                "The application code is a private project of Paolo Lombisani. No third-party interface artwork is used anywhere; every visual element is drawn in code (mission rule).",
                style = HermesType.bodySecondary,
                color = c.inkSecondary,
            )
        }

        SectionCard(title = "Privacy explanation") {
            Text(
                "The console captures no audio in V0.1; wake and speech are simulated by text injection. Audio retention is OFF by design and nothing is recorded. " +
                    "Conversation turns live in memory for the session. Diagnostics export is redacted. The privacy mute stops the whole pipeline and is unmistakable on screen.",
                style = HermesType.body,
                color = c.ink,
            )
            Text(
                "From V0.3 (real microphone): capture only during visible LISTENING; wake-word engine is the only always-on consumer and is disabled by the mute; retention stays opt-in.",
                style = HermesType.bodySecondary,
                color = c.inkSecondary,
            )
        }

        SectionCard(title = "Known limitations (truthful)") {
            val limits = listOf(
                "No microphone input — wake, listening and STT are simulated (V0.2/V0.3).",
                "Tower bridge is a deterministic mock; no real endpoint is contacted (V0.4).",
                "No local model is installed or qualified (V0.6).",
                "TTS produces timing/amplitude but no audible voice (system TTS at V0.3).",
                "Foreground service is a registered skeleton, not started (V0.2).",
                "History is session-scoped; persistence is a later, justified addition.",
                "UI language is English; Italian localization is scheduled for V1.0.",
                "Build cannot be verified inside this sandbox — see FINAL_REPORT.md and BUILDING.md.",
            )
            limits.forEach { Text("• $it", style = HermesType.body, color = c.ink) }
        }

        SectionCard(title = "Danger zone") {
            Button(onClick = { confirmReset = true }) { Text("Reset local data") }
            Text(
                "Clears conversation turns, captions, queue counters and persisted settings. Pairing does not exist yet, so there is nothing to unpair (V0.5 note).",
                style = HermesType.bodySecondary,
                color = c.inkSecondary,
            )
        }
    }

    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            title = { Text("Reset local data?") },
            text = { Text("Conversation history, captions, queue state and settings return to their defaults. This cannot be undone.") },
            confirmButton = {
                Button(onClick = {
                    graph.store.resetLocalData()
                    graph.settingsRepository.clear()
                    graph.store.updateSettings { graph.settingsRepository.load() }
                    confirmReset = false
                }) { Text("Reset") }
            },
            dismissButton = { TextButton(onClick = { confirmReset = false }) { Text("Cancel") } },
        )
    }
}
