package com.hermestower.voiceconsole.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hermestower.voiceconsole.app.AppGraph
import com.hermestower.voiceconsole.ui.components.ResponsiveScreenHeader
import com.hermestower.voiceconsole.ui.components.SectionCard
import com.hermestower.voiceconsole.ui.components.MeanderBand
import com.hermestower.voiceconsole.ui.components.TruthBadge
import com.hermestower.voiceconsole.ui.components.BadgeFlowRow
import com.hermestower.voiceconsole.ui.theme.HermesType
import com.hermestower.voiceconsole.ui.theme.hermesColors

/**
 * S5 Local Intelligence (FR-34): truthful model state, candidate placeholders,
 * routing policy, resource limits, load/unload controls, qualification status.
 * GRAPH 4.6 §6: the OPTIONAL local LLM is implemented end-to-end at the seam
 * (verified candidate, hash-gated delivery, safe prompt, typed diagnostics) and
 * ships DEFAULT DISABLED; generation stays typed-blocked until a verified
 * Android runtime is adopted — every state shown here is the real state.
 */
@Composable
fun LocalIntelligenceScreen(graph: AppGraph) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val c = hermesColors
    val modelState = graph.localEngine.modelState.collectAsStateWithLifecycle()

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ResponsiveScreenHeader(title = stringResource(com.hermestower.voiceconsole.R.string.li_title))
        MeanderBand(colors = hermesColors, modifier = Modifier.padding(top = 6.dp))

        SectionCard(title = stringResource(com.hermestower.voiceconsole.R.string.li_engine_card)) {
            BadgeFlowRow(horizontalSpacing = 8.dp) {
                TruthBadge(stringResource(com.hermestower.voiceconsole.R.string.li_state_fmt, com.hermestower.voiceconsole.locale.StateLabels.label(modelState.value.name, com.hermestower.voiceconsole.locale.AppLanguage.effectiveTag(context))), c.warn)
                TruthBadge(stringResource(com.hermestower.voiceconsole.R.string.li_runtime_none), c.inkSecondary)
            }
            Text(
                stringResource(com.hermestower.voiceconsole.R.string.li_mock_note),
                style = HermesType.bodySecondary,
                color = c.inkSecondary,
            )
        }

        SectionCard(title = stringResource(com.hermestower.voiceconsole.R.string.li_candidates_card)) {
            val rows = listOf(
                Triple("Qwen2.5-0.5B-Instruct", "0.5B · Apache-2.0", stringResource(com.hermestower.voiceconsole.R.string.li_note_fastest)),
                Triple("Llama-3.2-1B-Instruct", "1B · Llama license — verify terms", stringResource(com.hermestower.voiceconsole.R.string.li_note_italian)),
                Triple("Qwen2.5-1.5B-Instruct", "1.5B · Apache-2.0", stringResource(com.hermestower.voiceconsole.R.string.li_note_tradeoff)),
                Triple("SmolLM2-1.7B-Instruct", "1.7B · Apache-2.0", stringResource(com.hermestower.voiceconsole.R.string.li_note_tradeoff)),
                Triple("Gemma-2-2b-it", "2B · Gemma terms — verify", stringResource(com.hermestower.voiceconsole.R.string.li_note_ceiling)),
            )
            rows.forEach { (name, meta, note) ->
                Column(Modifier.padding(vertical = 2.dp)) {
                    Text(name, style = HermesType.body, color = c.ink)
                    Text(stringResource(com.hermestower.voiceconsole.R.string.li_meta_fmt, meta, note), style = HermesType.bodySecondary, color = c.inkSecondary)
                    TruthBadge(stringResource(com.hermestower.voiceconsole.R.string.li_qualify_badge), c.gold)
                }
            }
        }

        SectionCard(title = stringResource(com.hermestower.voiceconsole.R.string.li_routing_card)) {
            Text(
                stringResource(com.hermestower.voiceconsole.R.string.li_routing_note),
                style = HermesType.body,
                color = c.ink,
            )
            Text(
                stringResource(com.hermestower.voiceconsole.R.string.li_scope_note),
                style = HermesType.bodySecondary,
                color = c.inkSecondary,
            )
        }

        SectionCard(title = stringResource(com.hermestower.voiceconsole.R.string.li_limits_card)) {
            Text(
                stringResource(com.hermestower.voiceconsole.R.string.li_limits_note),
                style = HermesType.body,
                color = c.ink,
            )
        }

        // ---- GRAPH 4.6 §6: OPTIONAL LOCAL LLM — DEFAULT DISABLED, honest states ----
        val llmState by graph.localLlmEngine.state.collectAsStateWithLifecycle()
        val llmEnabled = graph.store.settings.collectAsStateWithLifecycle().value.localLlmEnabled
        val llmDiag = graph.localLlmEngine.diagnostics()
        SectionCard(title = stringResource(com.hermestower.voiceconsole.R.string.li_llm_card)) {
            BadgeFlowRow(horizontalSpacing = 8.dp) {
                TruthBadge(stringResource(com.hermestower.voiceconsole.R.string.li_state_fmt, com.hermestower.voiceconsole.locale.StateLabels.label(llmState.name, com.hermestower.voiceconsole.locale.AppLanguage.effectiveTag(context))), c.warn)
                TruthBadge(stringResource(com.hermestower.voiceconsole.R.string.li_llm_runtime_fmt, llmDiag.runtimeName), c.inkSecondary)
            }
            Text(
                stringResource(com.hermestower.voiceconsole.R.string.li_llm_identity_fmt, llmDiag.candidateIdentity, llmDiag.installedFileExists.toString()),
                style = HermesType.bodySecondary,
                color = c.inkSecondary,
            )
            Text(
                stringResource(com.hermestower.voiceconsole.R.string.li_llm_diag_fmt, llmDiag.lastDiagnosticCode.asText),
                style = HermesType.bodySecondary,
                color = c.inkSecondary,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Text(
                    stringResource(com.hermestower.voiceconsole.R.string.li_llm_enable),
                    style = HermesType.body,
                    color = c.ink,
                    modifier = Modifier.weight(1f),
                )
                Switch(
                    checked = llmEnabled,
                    onCheckedChange = { checked ->
                        graph.store.updateSettings { it.copy(localLlmEnabled = checked) }
                        graph.localLlmEngine.syncFromEnabledFlag()
                    },
                )
            }
            BadgeFlowRow(horizontalSpacing = 8.dp) {
                TextButton(onClick = { graph.localLlmEngine.load() }) {
                    Text(stringResource(com.hermestower.voiceconsole.R.string.li_llm_load), color = c.ink)
                }
                TextButton(onClick = { graph.localLlmEngine.unload() }) {
                    Text(stringResource(com.hermestower.voiceconsole.R.string.li_llm_unload), color = c.ink)
                }
            }
            Text(
                stringResource(com.hermestower.voiceconsole.R.string.li_llm_note),
                style = HermesType.bodySecondary,
                color = c.inkSecondary,
            )
        }

        SectionCard(title = stringResource(com.hermestower.voiceconsole.R.string.li_controls_card)) {
            BadgeFlowRow(horizontalSpacing = 8.dp) {
                TruthBadge(stringResource(com.hermestower.voiceconsole.R.string.li_load_badge), c.inkSecondary)
                TruthBadge(stringResource(com.hermestower.voiceconsole.R.string.li_unload_badge), c.inkSecondary)
            }
            Text(
                stringResource(com.hermestower.voiceconsole.R.string.li_controls_note),
                style = HermesType.bodySecondary,
                color = c.inkSecondary,
            )
        }
    }
}
