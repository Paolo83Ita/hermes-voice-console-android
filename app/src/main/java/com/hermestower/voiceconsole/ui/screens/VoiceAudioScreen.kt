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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hermestower.voiceconsole.app.AppGraph
import com.hermestower.voiceconsole.ui.components.SectionCard
import com.hermestower.voiceconsole.ui.components.MeanderBand
import com.hermestower.voiceconsole.ui.components.BadgeFlowRow
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
        Text(stringResource(com.hermestower.voiceconsole.R.string.voice_title), style = HermesType.screenTitle, color = c.ink)
        MeanderBand(colors = hermesColors, modifier = Modifier.padding(top = 6.dp))

        SectionCard(title = stringResource(com.hermestower.voiceconsole.R.string.voice_wake_card)) {
            BadgeFlowRow(horizontalSpacing = 8.dp) {
                TruthBadge(stringResource(com.hermestower.voiceconsole.R.string.voice_provider_fmt, graph.wakeWordEngine.phrase.uppercase()), c.gold)
                TruthBadge(stringResource(com.hermestower.voiceconsole.R.string.voice_engine_fmt, wakeState.value.name), c.inkSecondary)
            }
            Text(
                stringResource(com.hermestower.voiceconsole.R.string.voice_wake_note),
                style = HermesType.bodySecondary,
                color = c.inkSecondary,
            )
        }

        SectionCard(title = stringResource(com.hermestower.voiceconsole.R.string.voice_vad_card)) {
            TruthBadge(stringResource(com.hermestower.voiceconsole.R.string.voice_engine_fmt, vadState.value.name), c.inkSecondary)
            Text(
                stringResource(com.hermestower.voiceconsole.R.string.voice_vad_note),
                style = HermesType.bodySecondary,
                color = c.inkSecondary,
            )
        }

        SectionCard(title = stringResource(com.hermestower.voiceconsole.R.string.voice_stt_card)) {
            TruthBadge(stringResource(com.hermestower.voiceconsole.R.string.voice_engine_fmt, sttState.value.name), c.inkSecondary)
            TruthBadge(stringResource(com.hermestower.voiceconsole.R.string.voice_v01_path), c.warn)
            Text(
                stringResource(com.hermestower.voiceconsole.R.string.voice_stt_note),
                style = HermesType.bodySecondary,
                color = c.inkSecondary,
            )
        }

        SectionCard(title = stringResource(com.hermestower.voiceconsole.R.string.voice_tts_card)) {
            BadgeFlowRow(horizontalSpacing = 8.dp) {
                when (ttsState.value.state) {
                    com.hermestower.voiceconsole.voice.TtsState.READY,
                    com.hermestower.voiceconsole.voice.TtsState.SPEAKING,
                    com.hermestower.voiceconsole.voice.TtsState.SYNTHESIZING -> TruthBadge(stringResource(com.hermestower.voiceconsole.R.string.voice_tts_active), c.ok)
                    else -> TruthBadge(stringResource(com.hermestower.voiceconsole.R.string.voice_tts_unavailable), c.warn)
                }
                TruthBadge(stringResource(com.hermestower.voiceconsole.R.string.voice_engine_fmt, ttsState.value.state.name), c.inkSecondary)
            }
            Text(
                stringResource(com.hermestower.voiceconsole.R.string.voice_speed_fmt, "%.2f".format(settings.speakingRate)),
                style = HermesType.body,
                color = c.ink,
            )
            Slider(
                value = settings.speakingRate,
                onValueChange = { graph.store.updateSettings { s -> s.copy(speakingRate = it) } },
                valueRange = 0.8f..1.2f,
                steps = 7,
            )
            Text(
                stringResource(com.hermestower.voiceconsole.R.string.voice_tts_note),
                style = HermesType.bodySecondary,
                color = c.inkSecondary,
            )
        }

        // OWNER PHYSICAL FEEDBACK 3: the Italian voice card — the post-apply
        // read-back surfaced verbatim, with the honest device-side action when
        // the Italian voice data is missing on the POCO/Xiaomi.
        SectionCard(title = stringResource(com.hermestower.voiceconsole.R.string.voice_italian_card)) {
            val rb = ttsState.value.voiceReadback
            if (rb == null) {
                Text(
                    stringResource(com.hermestower.voiceconsole.R.string.diag_unavailable),
                    style = HermesType.bodySecondary,
                    color = c.inkSecondary,
                )
            } else {
                BadgeFlowRow(horizontalSpacing = 8.dp) {
                    TruthBadge(rb.status.name, if (rb.isItalianAudible) c.ok else c.warn)
                    rb.activeVoiceName?.let {
                        TruthBadge(
                            stringResource(com.hermestower.voiceconsole.R.string.voice_italian_voice_fmt, it),
                            c.inkSecondary,
                        )
                    }
                }
                Text(
                    stringResource(
                        com.hermestower.voiceconsole.R.string.voice_italian_lang_fmt,
                        rb.activeVoiceLocaleTag ?: rb.activeLanguageTag ?: "?",
                    ),
                    style = HermesType.body,
                    color = c.ink,
                )
                rb.detail?.let {
                    Text(
                        it,
                        style = HermesType.bodySecondary,
                        color = c.inkSecondary,
                    )
                }
                if (rb.status.requiresUserAction) {
                    val ctx = androidx.compose.ui.platform.LocalContext.current
                    TextButton(onClick = {
                        runCatching {
                            ctx.startActivity(android.content.Intent(android.provider.Settings.ACTION_TTS_SETTINGS)
                                .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK))
                        }
                    }) {
                        Text(stringResource(com.hermestower.voiceconsole.R.string.voice_italian_open_settings))
                    }
                }
            }
            Text(
                stringResource(com.hermestower.voiceconsole.R.string.voice_italian_note),
                style = HermesType.bodySecondary,
                color = c.inkSecondary,
            )
        }

        SectionCard(title = stringResource(com.hermestower.voiceconsole.R.string.voice_interruption_card)) {
            SettingRow(
                label = stringResource(com.hermestower.voiceconsole.R.string.voice_barge_in),
                checked = true,
                enabled = false,
                onToggle = { /* V0.1: always on by design in the driver */ },
            )
            SettingRow(
                label = stringResource(com.hermestower.voiceconsole.R.string.voice_captions),
                checked = settings.captionsEnabled,
                enabled = true,
                onToggle = { graph.store.updateSettings { s -> s.copy(captionsEnabled = it) } },
            )
            Text(
                stringResource(com.hermestower.voiceconsole.R.string.voice_capture_note),
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
