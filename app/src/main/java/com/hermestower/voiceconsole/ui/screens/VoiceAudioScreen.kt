package com.hermestower.voiceconsole.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hermestower.voiceconsole.app.AppGraph
import com.hermestower.voiceconsole.ui.components.SectionCard
import com.hermestower.voiceconsole.ui.components.TruthBadge
import com.hermestower.voiceconsole.ui.theme.HermesType
import com.hermestower.voiceconsole.ui.theme.hermesColors

/**
 * S4 Voice & Audio (FR-33): provider placeholders with truthful status,
 * speaking speed, interruption behavior, captions. No engine is active in V0.1.
 */
@Composable
fun VoiceAudioScreen(graph: AppGraph) {
    val c = hermesColors
    val settings by graph.store.settings.collectAsStateWithLifecycle()
    val wakeState = graph.wakeWordEngine.state.collectAsStateWithLifecycle()
    val vadState = graph.voiceActivityDetector.state.collectAsStateWithLifecycle()
    val sttState = graph.speechToTextEngine.state.collectAsStateWithLifecycle()
    val ttsState = graph.voiceTts.state.collectAsStateWithLifecycle()

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Voice & Audio", style = HermesType.screenTitle, color = c.ink)

        SectionCard(title = "Wake word") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TruthBadge("PROVIDER: ${graph.wakeWordEngine.phrase.uppercase()} CANDIDATES", c.gold)
                TruthBadge("ENGINE: ${wakeState.value.name}", c.inkSecondary)
            }
            Text(
                "Configuration placeholder. Candidates (openWakeWord, Porcupine) are QUALIFY_ON_DEVICE — nothing is active until physical tests pass (VOICE_PIPELINE_OPTIONS §2).",
                style = HermesType.bodySecondary,
                color = c.inkSecondary,
            )
        }

        SectionCard(title = "Voice activity detection") {
            TruthBadge("ENGINE: ${vadState.value.name}", c.inkSecondary)
            Text(
                "Silero VAD (primary candidate) and WebRTC VAD (prototype) behind one seam; qualification criteria fixed in VOICE_PIPELINE_OPTIONS §3.",
                style = HermesType.bodySecondary,
                color = c.inkSecondary,
            )
        }

        SectionCard(title = "Speech-to-text") {
            TruthBadge("ENGINE: ${sttState.value.name}", c.inkSecondary)
            TruthBadge("V0.1 PATH: TEXT INJECTION", c.warn)
            Text(
                "whisper.cpp is the primary QUALIFY_ON_DEVICE candidate; the Android system recognizer is a prototype fallback that must prove fully on-device behavior before LOCAL-mode use.",
                style = HermesType.bodySecondary,
                color = c.inkSecondary,
            )
        }

        SectionCard(title = "Text-to-speech") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                when (ttsState.value.state) {
                    com.hermestower.voiceconsole.voice.TtsState.READY,
                    com.hermestower.voiceconsole.voice.TtsState.SPEAKING,
                    com.hermestower.voiceconsole.voice.TtsState.SYNTHESIZING -> TruthBadge("SYSTEM TTS — ACTIVE (REAL AUDIO)", c.ok)
                    else -> TruthBadge("SYSTEM TTS — UNAVAILABLE (text-only)", c.warn)
                }
                TruthBadge("ENGINE: ${ttsState.value.state.name}", c.inkSecondary)
            }
            Text("Speaking speed: ${"%.2f".format(settings.speakingRate)}×", style = HermesType.body, color = c.ink)
            Slider(
                value = settings.speakingRate,
                onValueChange = { graph.store.updateSettings { s -> s.copy(speakingRate = it) } },
                valueRange = 0.8f..1.2f,
                steps = 7,
            )
            Text(
                "Voice selection and higher-quality local voices (Piper candidate, Kokoro deferred) follow the physical qualification sessions; the target voice is calm, authoritative, Italian-first.",
                style = HermesType.bodySecondary,
                color = c.inkSecondary,
            )
        }

        SectionCard(title = "Interruption & captions") {
            SettingRow(
                label = "Barge-in: new input interrupts playback",
                checked = true,
                enabled = false,
                onToggle = { /* V0.1: always on by design in the driver */ },
            )
            SettingRow(
                label = "Captions",
                checked = settings.captionsEnabled,
                enabled = true,
                onToggle = { graph.store.updateSettings { s -> s.copy(captionsEnabled = it) } },
            )
            Text(
                "Stop-speaking and privacy mute are always available on the console; speech is never captured into persistent logs (SEC §4).",
                style = HermesType.bodySecondary,
                color = c.inkSecondary,
            )
        }
    }
}

@Composable
internal fun SettingRow(
    label: String,
    checked: Boolean,
    enabled: Boolean,
    onToggle: (Boolean) -> Unit,
) {
    val c = hermesColors
    // CP-G4-007 accessibility-semantics repair (Owner §3): the WHOLE row is the
    // control — toggleable with Role.Switch so the label text carries the toggle
    // action (merged semantics). TalkBack announces label + state together and
    // E2E may click the label. The Switch is state-display only (onCheckedChange
    // null = no separate click target, Material accessibility guidance).
    Row(
        Modifier
            .fillMaxWidth()
            .toggleable(
                value = checked,
                enabled = enabled,
                role = Role.Switch,
                onValueChange = onToggle,
            ),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
    ) {
        Text(label, style = HermesType.body, color = c.ink, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = null, enabled = enabled)
    }
}
