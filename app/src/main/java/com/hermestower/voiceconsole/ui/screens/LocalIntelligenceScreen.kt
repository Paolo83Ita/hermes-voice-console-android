package com.hermestower.voiceconsole.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hermestower.voiceconsole.app.AppGraph
import com.hermestower.voiceconsole.ui.components.SectionCard
import com.hermestower.voiceconsole.ui.components.TruthBadge
import com.hermestower.voiceconsole.ui.theme.HermesType
import com.hermestower.voiceconsole.ui.theme.hermesColors

/**
 * S5 Local Intelligence (FR-34): truthful model state, candidate placeholders,
 * routing policy, resource limits, load/unload controls (disabled until V0.6),
 * qualification status. Nothing here is claimed to work.
 */
@Composable
fun LocalIntelligenceScreen(graph: AppGraph) {
    val c = hermesColors
    val modelState = graph.localEngine.modelState.collectAsStateWithLifecycle()

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Local Intelligence", style = HermesType.screenTitle, color = c.ink)

        SectionCard(title = "Local conversation engine") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TruthBadge("STATE: ${modelState.value.name}", c.warn)
                TruthBadge("RUNTIME: NONE INSTALLED", c.inkSecondary)
            }
            Text(
                "V0.1 serves the seam with a deterministic mock. No model files exist on this device; no model is downloaded during the foundation phase (mission rule; LOCAL_MODEL_CANDIDATE_PLAN §2).",
                style = HermesType.bodySecondary,
                color = c.inkSecondary,
            )
        }

        SectionCard(title = "Candidates (not endorsements)") {
            val rows = listOf(
                Triple("Qwen2.5-0.5B-Instruct", "0.5B · Apache-2.0", "fastest; weakest reasoning"),
                Triple("Llama-3.2-1B-Instruct", "1B · Llama license — verify terms", "good Italian"),
                Triple("Qwen2.5-1.5B-Instruct", "1.5B · Apache-2.0", "best trade-off candidate"),
                Triple("SmolLM2-1.7B-Instruct", "1.7B · Apache-2.0", "best trade-off candidate"),
                Triple("Gemma-2-2b-it", "2B · Gemma terms — verify", "quality ceiling; RAM risk"),
            )
            rows.forEach { (name, meta, note) ->
                Column(Modifier.padding(vertical = 2.dp)) {
                    Text(name, style = HermesType.body, color = c.ink)
                    Text("$meta — $note", style = HermesType.bodySecondary, color = c.inkSecondary)
                    TruthBadge("QUALIFY_ON_DEVICE (V0.6)", c.gold)
                }
            }
        }

        SectionCard(title = "Routing policy (HYBRID)") {
            Text(
                "Simple, qualified queries (greetings, help, device status, time) resolve locally. " +
                    "Complex, operational, uncertain or consequential requests route to Hermes Tower. " +
                    "Low STT confidence (< 0.55) escalates to the Tower in HYBRID mode.",
                style = HermesType.body,
                color = c.ink,
            )
            Text(
                "The optional local model may only: converse, detect intent, clean commands, correct STT, assist routing, summarize a turn before forwarding, explain status, and answer offline. It never executes consequential actions (FR-19).",
                style = HermesType.bodySecondary,
                color = c.inkSecondary,
            )
        }

        SectionCard(title = "Resource limits (targets)") {
            Text(
                "With model resident: ≤ 1.6 GB RSS total; unload on idle timer; one resident model at a time. " +
                    "Cold load ≤ 4 s; warm first token ≤ 1.5 s; ≥ 8 tok/s — all pass-guidelines pending measurement (NFR-09).",
                style = HermesType.body,
                color = c.ink,
            )
        }

        SectionCard(title = "Load / unload controls") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TruthBadge("LOAD — ENABLES AT V0.6", c.inkSecondary)
                TruthBadge("UNLOAD — ENABLES AT V0.6", c.inkSecondary)
            }
            Text(
                "Controls stay visible but disabled until a qualified runtime exists; enabling them now would pretend a capability the build does not have.",
                style = HermesType.bodySecondary,
                color = c.inkSecondary,
            )
        }
    }
}
