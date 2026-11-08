package com.hermestower.voiceconsole.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hermestower.voiceconsole.R as Res
import com.hermestower.voiceconsole.app.AppGraph
import com.hermestower.voiceconsole.ui.components.ResponsiveScreenHeader
import com.hermestower.voiceconsole.ui.components.SectionCard
import com.hermestower.voiceconsole.ui.components.MeanderBand
import com.hermestower.voiceconsole.ui.components.BadgeFlowRow
import com.hermestower.voiceconsole.ui.components.TruthBadge
import com.hermestower.voiceconsole.ui.theme.HermesType
import com.hermestower.voiceconsole.ui.theme.hermesColors
import com.hermestower.voiceconsole.voice.ItalianTtsVoiceNegotiator
import com.hermestower.voiceconsole.voice.ListeningModePolicy
import com.hermestower.voiceconsole.voice.SpeechProfile
import com.hermestower.voiceconsole.voice.TtsSpeakGate
import com.hermestower.voiceconsole.voice.TtsState
import com.hermestower.voiceconsole.ui.components.HermesFilterChip
import com.hermestower.voiceconsole.ui.components.HermesSwitch

/**
 * S4 Voice & Audio (FR-33): truthful voice-engine status, the OWNER PHYSICAL
 * RETEST §5 speech-profile authority (speed/pitch with the ordinary presets
 * and collapsed advanced sliders, preview phrase, reset), the §6 Android TTS
 * diagnostic card (engine identity, Italian availability, voice inventory,
 * last negotiation result + the device-side actions) and the §4 fail-closed
 * state surface (Italian message + open settings actions — the app NEVER
 * speaks a non-Italian voice while Italian is requested).
 */
@Composable
fun VoiceAudioScreen(graph: AppGraph) {
    val c = hermesColors
    val settings by graph.store.settings.collectAsStateWithLifecycle()
    val wakeState = graph.wakeWordEngine.state.collectAsStateWithLifecycle()
    val vadState = graph.voiceActivityDetector.state.collectAsStateWithLifecycle()
    val sttState = graph.speechToTextEngine.state.collectAsStateWithLifecycle()
    val ttsState = graph.voiceTts.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ResponsiveScreenHeader(title = stringResource(Res.string.voice_title))
        MeanderBand(colors = hermesColors, modifier = Modifier.padding(top = 6.dp))

        // ---- GRAPH 4.10 §17: Listening mode (the §14 authority is the only writer) ----
        SectionCard(title = stringResource(Res.string.listening_mode_title)) {
            val listening by graph.listeningModePolicy.state.collectAsStateWithLifecycle()
            val modeOptions = listOf(
                ListeningModePolicy.Mode.PUSH_TO_TALK to Res.string.listening_mode_push_to_talk,
                ListeningModePolicy.Mode.CONTINUOUS_WHILE_SCREEN_ACTIVE to Res.string.listening_mode_continuous,
                ListeningModePolicy.Mode.WAKE_WORD to Res.string.listening_mode_wake_word,
                ListeningModePolicy.Mode.OFF to Res.string.listening_mode_off,
            )
            modeOptions.forEach { (mode, labelRes) ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .toggleable(
                            value = listening.mode == mode,
                            role = Role.RadioButton,
                            onValueChange = {
                                if (it) {
                                    if (mode == ListeningModePolicy.Mode.WAKE_WORD) {
                                        // §14: honest refusal until physically qualified.
                                        graph.listeningModePolicy.setMode(mode, "voice-audio settings")
                                    } else {
                                        graph.listeningModePolicy.setMode(mode, "voice-audio settings")
                                    }
                                }
                            },
                        ),
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                ) {
                    androidx.compose.material3.RadioButton(
                        selected = listening.mode == mode,
                        onClick = null,
                    )
                    Text(
                        stringResource(labelRes),
                        style = HermesType.body,
                        color = c.textPrimary,
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
            }
            if (listening.mode == ListeningModePolicy.Mode.WAKE_WORD) {
                Text(
                    stringResource(Res.string.wake_word_unavailable),
                    style = HermesType.bodySecondary,
                    color = c.textInfo,
                )
            }
            if (listening.mode == ListeningModePolicy.Mode.CONTINUOUS_WHILE_SCREEN_ACTIVE) {
                Text(
                    stringResource(Res.string.listening_mode_title) + ": " + stringResource(Res.string.session_limit_15),
                    style = HermesType.bodySecondary,
                    color = c.textSecondary,
                    modifier = Modifier.padding(top = 4.dp),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    ListeningModePolicy.SESSION_LIMIT_OPTIONS.forEach { minutes ->
                        val label = when (minutes) {
                            5 -> Res.string.session_limit_5
                            15 -> Res.string.session_limit_15
                            30 -> Res.string.session_limit_30
                            else -> Res.string.session_limit_open
                        }
                        HermesFilterChip(
                            selected = listening.sessionLimitMinutes == minutes,
                            onClick = { graph.listeningModePolicy.setSessionLimit(minutes, "voice-audio settings") },
                            label = { Text(stringResource(label), style = HermesType.bodySecondary) },
                        )
                    }
                }
            }
        }

        SectionCard(title = stringResource(Res.string.voice_wake_card)) {
            BadgeFlowRow(horizontalSpacing = 8.dp) {
                TruthBadge(stringResource(Res.string.voice_provider_fmt, graph.wakeWordEngine.phrase.uppercase()), c.gold)
                TruthBadge(stringResource(Res.string.voice_engine_fmt, wakeState.value.name), c.textSecondary)
            }
            Text(
                stringResource(Res.string.voice_wake_note),
                style = HermesType.bodySecondary,
                color = c.textSecondary,
            )
        }

        SectionCard(title = stringResource(Res.string.voice_vad_card)) {
            TruthBadge(stringResource(Res.string.voice_engine_fmt, vadState.value.name), c.textSecondary)
            Text(
                stringResource(Res.string.voice_vad_note),
                style = HermesType.bodySecondary,
                color = c.textSecondary,
            )
        }

        SectionCard(title = stringResource(Res.string.voice_stt_card)) {
            TruthBadge(stringResource(Res.string.voice_engine_fmt, sttState.value.name), c.textSecondary)
            TruthBadge(stringResource(Res.string.voice_v01_path), c.textWarning)
            Text(
                stringResource(Res.string.voice_stt_note),
                style = HermesType.bodySecondary,
                color = c.textSecondary,
            )
        }

        SectionCard(title = stringResource(Res.string.voice_tts_card)) {
            BadgeFlowRow(horizontalSpacing = 8.dp) {
                when (ttsState.value.state) {
                    TtsState.READY,
                    TtsState.SPEAKING,
                    TtsState.SYNTHESIZING,
                    -> TruthBadge(stringResource(Res.string.voice_tts_active), c.textSuccess)
                    TtsState.TEXT_ONLY_SAFE_DEGRADED ->
                        TruthBadge(stringResource(Res.string.voice_tts_text_only), c.textWarning)
                    else -> TruthBadge(stringResource(Res.string.voice_tts_unavailable), c.textWarning)
                }
                TruthBadge(stringResource(Res.string.voice_engine_fmt, ttsState.value.state.name), c.textSecondary)
            }
            Text(
                stringResource(Res.string.voice_speed_fmt, "%.2f".format(settings.speakingRate)),
                style = HermesType.body,
                color = c.textPrimary,
            )
            Slider(
                value = settings.speakingRate,
                onValueChange = {
                    graph.store.updateSettings { s ->
                        s.copy(speakingRate = SpeechProfile.safe(it, s.speechPitch).speechRate)
                    }
                },
                valueRange = SpeechProfile.RATE_MIN..SpeechProfile.RATE_MAX,
                steps = 10,
            )
            Text(
                stringResource(Res.string.voice_tts_note),
                style = HermesType.bodySecondary,
                color = c.textSecondary,
            )
        }

        // ---- OWNER PHYSICAL RETEST §4: the fail-closed surface. When Italian
        // is the effective language and the active voice is not Italian, the
        // app REFUSES audio: the response stays visible as text, the reason is
        // typed and the device-side repairs are one tap away.
        ttsState.value.speakBlocked?.let { block ->
            SectionCard(title = stringResource(Res.string.voice_blocked_card)) {
                TruthBadge(block.token, c.textWarning)
                Text(
                    stringResource(Res.string.voice_blocked_message),
                    style = HermesType.body,
                    color = c.textPrimary,
                )
                Text(
                    stringResource(Res.string.voice_blocked_text_visible),
                    style = HermesType.bodySecondary,
                    color = c.textSecondary,
                )
                BadgeFlowRow(horizontalSpacing = 8.dp) {
                    TextButton(onClick = { openSystemTtsSettings(context) }) {
                        Text(stringResource(Res.string.voice_blocked_open_settings))
                    }
                    TextButton(onClick = { openVoiceDataInstaller(context) }) {
                        Text(stringResource(Res.string.voice_blocked_install_data))
                    }
                }
            }
        }

        // ---- OWNER PHYSICAL RETEST §5: Speech output (the profile authority). ----
        SectionCard(title = stringResource(Res.string.voice_speech_output_card)) {
            var advancedOpen by remember { mutableStateOf(false) }
            Text(
                stringResource(Res.string.voice_profile_speed_row, "%.2f".format(settings.speakingRate)),
                style = HermesType.body,
                color = c.textPrimary,
            )
            BadgeFlowRow(horizontalSpacing = 6.dp) {
                PresetButton(stringResource(Res.string.voice_preset_slower)) {
                    graph.store.updateSettings { s -> s.copy(speakingRate = SpeechProfile.RATE_SLOWER) }
                }
                PresetButton(stringResource(Res.string.voice_preset_recommended)) {
                    graph.store.updateSettings { s ->
                        s.copy(speakingRate = SpeechProfile.RECOMMENDED_RATE, speechPitch = SpeechProfile.RECOMMENDED_PITCH)
                    }
                }
                PresetButton(stringResource(Res.string.voice_preset_faster)) {
                    graph.store.updateSettings { s -> s.copy(speakingRate = SpeechProfile.RATE_FASTER) }
                }
            }
            Text(
                stringResource(Res.string.voice_profile_pitch_row, "%.2f".format(settings.speechPitch)),
                style = HermesType.body,
                color = c.textPrimary,
            )
            BadgeFlowRow(horizontalSpacing = 6.dp) {
                PresetButton(stringResource(Res.string.voice_preset_lower)) {
                    graph.store.updateSettings { s -> s.copy(speechPitch = SpeechProfile.PITCH_LOWER) }
                }
                PresetButton(stringResource(Res.string.voice_preset_pitch_recommended)) {
                    graph.store.updateSettings { s -> s.copy(speechPitch = SpeechProfile.RECOMMENDED_PITCH) }
                }
                PresetButton(stringResource(Res.string.voice_preset_higher)) {
                    graph.store.updateSettings { s -> s.copy(speechPitch = SpeechProfile.PITCH_HIGHER) }
                }
            }
            TextButton(onClick = { advancedOpen = !advancedOpen }) {
                Text(
                    if (advancedOpen) stringResource(Res.string.voice_profile_advanced_hide)
                    else stringResource(Res.string.voice_profile_advanced_show),
                )
            }
            if (advancedOpen) {
                Text(
                    stringResource(Res.string.voice_profile_advanced_speed),
                    style = HermesType.bodySecondary,
                    color = c.textSecondary,
                )
                Slider(
                    value = settings.speakingRate,
                    onValueChange = {
                        graph.store.updateSettings { s ->
                            s.copy(speakingRate = SpeechProfile.safe(it, s.speechPitch).speechRate)
                        }
                    },
                    valueRange = SpeechProfile.RATE_MIN..SpeechProfile.RATE_MAX,
                    steps = 10,
                )
                Text(
                    stringResource(Res.string.voice_profile_advanced_pitch),
                    style = HermesType.bodySecondary,
                    color = c.textSecondary,
                )
                Slider(
                    value = settings.speechPitch,
                    onValueChange = {
                        graph.store.updateSettings { s ->
                            s.copy(speechPitch = SpeechProfile.safe(s.speakingRate, it).pitch)
                        }
                    },
                    valueRange = SpeechProfile.PITCH_MIN..SpeechProfile.PITCH_MAX,
                    steps = 9,
                )
            }
            TextButton(onClick = {
                graph.store.updateSettings { s ->
                    s.copy(speakingRate = SpeechProfile.RECOMMENDED_RATE, speechPitch = SpeechProfile.RECOMMENDED_PITCH)
                }
            }) {
                Text(stringResource(Res.string.voice_profile_reset))
            }
            Text(
                stringResource(Res.string.voice_profile_preview_phrase),
                style = HermesType.bodySecondary,
                color = c.textSecondary,
            )
            BadgeFlowRow(horizontalSpacing = 8.dp) {
                TextButton(onClick = {
                    graph.voiceTts.testItalianVoice(context.getString(Res.string.voice_preview_phrase))
                }) { Text(stringResource(Res.string.voice_profile_preview_button)) }
            }
            Text(
                stringResource(Res.string.voice_profile_persist_note),
                style = HermesType.bodySecondary,
                color = c.textSecondary,
            )
        }

        // ---- OWNER PHYSICAL RETEST §6: the Android TTS diagnostic card. ----
        SectionCard(title = stringResource(Res.string.voice_diag_card)) {
            val rr = graph.activeRuntimeReadback
            val rb = ttsState.value.voiceReadback
            DiagRow(stringResource(Res.string.voice_diag_engine), rr.ttsActiveEngine)
            DiagRow(stringResource(Res.string.voice_diag_engine_label), rr.ttsEngineLabel)
            DiagRow(stringResource(Res.string.voice_diag_availability), rr.ttsItalianAvailability)
            DiagRow(stringResource(Res.string.voice_diag_inventory), rr.ttsVoiceInventory)
            DiagRow(stringResource(Res.string.voice_diag_selected_voice), rr.ttsSelectedVoice)
            DiagRow(stringResource(Res.string.voice_diag_selected_locale), rr.ttsSelectedVoiceLocale)
            DiagRow(stringResource(Res.string.voice_diag_rate), "%.2f".format(rr.ttsSpeechRate))
            DiagRow(stringResource(Res.string.voice_diag_pitch), "%.2f".format(rr.ttsSpeechPitch))
            DiagRow(stringResource(Res.string.voice_diag_last_negotiation), rr.ttsLastNegotiationStatus)
            rb?.detail?.let {
                Text(it, style = HermesType.bodySecondary, color = c.textSecondary)
            }
            BadgeFlowRow(horizontalSpacing = 6.dp) {
                TextButton(onClick = {
                    graph.voiceTts.testItalianVoice(context.getString(Res.string.voice_preview_phrase))
                }) { Text(stringResource(Res.string.voice_diag_test)) }
                TextButton(onClick = { openSystemTtsSettings(context) }) {
                    Text(stringResource(Res.string.voice_diag_open_settings))
                }
                TextButton(onClick = { openVoiceDataInstaller(context) }) {
                    Text(stringResource(Res.string.voice_diag_install_data))
                }
                TextButton(onClick = { graph.voiceTts.refreshVoices() }) {
                    Text(stringResource(Res.string.voice_diag_refresh))
                }
                TextButton(onClick = { graph.voiceTts.resetVoiceSelection() }) {
                    Text(stringResource(Res.string.voice_diag_reset))
                }
            }
            // The fail-closed expectation in one honest line.
            when (val verdict = TtsSpeakGate.decide(
                effectiveLanguageTag = com.hermestower.voiceconsole.locale.AppLanguage.effectiveTag(context),
                readback = rb,
            )) {
                is TtsSpeakGate.Verdict.Allow -> Text(
                    stringResource(Res.string.voice_diag_gate_allow),
                    style = HermesType.bodySecondary,
                    color = c.textSuccess,
                )
                is TtsSpeakGate.Verdict.Block -> Text(
                    stringResource(Res.string.voice_diag_gate_block, verdict.reason.name),
                    style = HermesType.bodySecondary,
                    color = c.textWarning,
                )
            }
        }

        // OWNER PHYSICAL FEEDBACK 3: the Italian voice card — the post-apply
        // read-back surfaced verbatim, with the honest device-side action when
        // the Italian voice data is missing on the POCO/Xiaomi.
        SectionCard(title = stringResource(Res.string.voice_italian_card)) {
            val rb = ttsState.value.voiceReadback
            if (rb == null) {
                Text(
                    stringResource(Res.string.diag_unavailable),
                    style = HermesType.bodySecondary,
                    color = c.textSecondary,
                )
            } else {
                BadgeFlowRow(horizontalSpacing = 8.dp) {
                    TruthBadge(rb.status.name, if (rb.isItalianAudible) c.textSuccess else c.textWarning)
                    rb.activeVoiceName?.let {
                        TruthBadge(
                            stringResource(Res.string.voice_italian_voice_fmt, it),
                            c.textSecondary,
                        )
                    }
                    rb.inventory?.let { inv ->
                        TruthBadge(
                            stringResource(
                                Res.string.voice_italian_inventory_fmt,
                                inv.italianVoices,
                                inv.offlineItalianVoices,
                            ),
                            c.textSecondary,
                        )
                    }
                }
                Text(
                    stringResource(
                        Res.string.voice_italian_lang_fmt,
                        rb.activeVoiceLocaleTag ?: rb.activeLanguageTag ?: "?",
                    ),
                    style = HermesType.body,
                    color = c.textPrimary,
                )
                rb.detail?.let {
                    Text(
                        it,
                        style = HermesType.bodySecondary,
                        color = c.textSecondary,
                    )
                }
                if (rb.status.requiresUserAction) {
                    TextButton(onClick = { openSystemTtsSettings(context) }) {
                        Text(stringResource(Res.string.voice_italian_open_settings))
                    }
                }
            }
            Text(
                stringResource(Res.string.voice_italian_note),
                style = HermesType.bodySecondary,
                color = c.textSecondary,
            )
        }

        SectionCard(title = stringResource(Res.string.voice_interruption_card)) {
            SettingRow(
                label = stringResource(Res.string.voice_barge_in),
                checked = true,
                enabled = false,
                onToggle = { /* V0.1: always on by design in the driver */ },
            )
            SettingRow(
                label = stringResource(Res.string.voice_captions),
                checked = settings.captionsEnabled,
                enabled = true,
                onToggle = { graph.store.updateSettings { s -> s.copy(captionsEnabled = it) } },
            )
            Text(
                stringResource(Res.string.voice_capture_note),
                style = HermesType.bodySecondary,
                color = c.textSecondary,
            )
        }
    }
}

/** The ordinary preset chip (§5: Slower/Recommended/Faster, Lower/Recommended/Higher). */
@Composable
private fun PresetButton(label: String, onClick: () -> Unit) {
    TextButton(
        onClick = onClick,
        modifier = Modifier.heightIn(min = 48.dp),
    ) { Text(label, textAlign = TextAlign.Center) }
}

/** One diagnostic label/value row (value weighted — no overflow at 360 dp). */
@Composable
private fun DiagRow(label: String, value: String?) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 1.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            label,
            style = HermesType.bodySecondary,
            color = hermesColors.textSecondary,
            modifier = Modifier.weight(1.1f),
        )
        Text(
            value ?: stringResource(Res.string.diag_unavailable),
            style = HermesType.body,
            color = hermesColors.textPrimary,
            modifier = Modifier.weight(1.9f),
        )
    }
}

/** Opens the system Text-to-speech settings (the action Android supports). */
internal fun openSystemTtsSettings(context: android.content.Context) {
    runCatching {
        // "android.settings.TTS_SETTINGS" — the system Text-to-speech screen
        // (the public constant value; API 14+, no resolution risk in this scope).
        context.startActivity(
            android.content.Intent("android.settings.TTS_SETTINGS")
                .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }
}

/**
 * Opens the engine's voice-data installation screen where Android supports it
 * (TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA). Some engines/settings builds
 * do not resolve it — the failure stays silent-honest (the TTS settings action
 * above is always available and the diagnostic card names the missing data).
 */
internal fun openVoiceDataInstaller(context: android.content.Context) {
    runCatching {
        context.startActivity(
            android.content.Intent(android.speech.tts.TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA)
                .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }.onFailure {
        openSystemTtsSettings(context)
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
        Text(label, style = HermesType.body, color = c.textPrimary, modifier = Modifier.weight(1f))
        HermesSwitch(checked = checked, onCheckedChange = null, enabled = enabled)
    }
}
