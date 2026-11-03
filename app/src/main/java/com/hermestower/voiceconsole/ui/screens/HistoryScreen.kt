package com.hermestower.voiceconsole.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hermestower.voiceconsole.app.AppGraph
import com.hermestower.voiceconsole.core.state.ConversationTurn
import com.hermestower.voiceconsole.ui.components.SectionCard
import com.hermestower.voiceconsole.ui.components.MeanderBand
import com.hermestower.voiceconsole.ui.components.TruthBadge
import com.hermestower.voiceconsole.ui.theme.HermesType
import com.hermestower.voiceconsole.ui.theme.hermesColors

/**
 * S2 Conversation History (FR-16/17): LOCAL vs TOWER attribution, delivery state,
 * retry only when safe, delete and retention controls.
 */
@Composable
fun HistoryScreen(graph: AppGraph) {
    val c = hermesColors
    val turns by graph.store.turns.collectAsStateWithLifecycle()
    var confirmClear by remember { mutableStateOf(false) }
    var filter by remember { mutableStateOf(Filter.ALL) }

    Column(
        Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Conversation History", style = HermesType.screenTitle, color = c.ink)
        MeanderBand(colors = hermesColors, modifier = Modifier.padding(top = 6.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Filter.entries.forEach { f ->
                com.hermestower.voiceconsole.ui.components.ModeChip(
                    label = f.label,
                    selected = filter == f,
                    enabled = true,
                    onClick = { filter = f },
                )
            }
        }

        SectionCard(title = "Turns (${turns.size})") {
            if (turns.isEmpty()) {
                Text(
                    "No turns in this session. History is session-scoped in V0.1; persistence is evaluated at V0.3 (decision D-004).",
                    style = HermesType.bodySecondary,
                    color = c.inkSecondary,
                )
            }
        }

        LazyColumn(
            Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(turns.filter { filter.matches(it) }, key = { it.id }) { turn ->
                TurnRow(
                    turn = turn,
                    onRetry = { graph.driver.submitText(turn.text) },
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = { confirmClear = true }) { Text("Clear history") }
            TextButton(onClick = { graph.store.resetLocalData() }) { Text("Reset all local data") }
        }
        Text(
            "Retention: session-scoped, in-memory (V0.1). Speech audio is never recorded. " +
                "Transcript retention windows arrive with persistence (V0.3+).",
            style = HermesType.bodySecondary,
            color = c.inkSecondary,
        )
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("Clear history?") },
            text = { Text("This removes every turn from the current session. It cannot be undone.") },
            confirmButton = {
                Button(onClick = {
                    graph.store.resetLocalData()
                    confirmClear = false
                }) { Text("Clear") }
            },
            dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("Cancel") } },
        )
    }
}

private enum class Filter(val label: String) {
    ALL("ALL"), LOCAL("LOCAL"), TOWER("TOWER"), PENDING("QUEUED/FAILED");

    fun matches(turn: ConversationTurn): Boolean = when (this) {
        ALL -> true
        LOCAL -> turn.source == ConversationTurn.Source.LOCAL
        TOWER -> turn.source == ConversationTurn.Source.TOWER
        PENDING -> turn.delivery == ConversationTurn.Delivery.QUEUED || turn.delivery == ConversationTurn.Delivery.FAILED
    }
}

@Composable
private fun TurnRow(turn: ConversationTurn, onRetry: () -> Unit) {
    val c = hermesColors
    val who = when (turn.role) {
        ConversationTurn.Role.OWNER -> "OWNER"
        else -> if (turn.source == ConversationTurn.Source.TOWER) "HERMES TOWER" else "HERMES · LOCAL"
    }
    val whoColor = when {
        turn.role == ConversationTurn.Role.OWNER -> c.bronze
        turn.source == ConversationTurn.Source.TOWER -> c.energy
        else -> c.ok
    }
    SectionCard(title = "#${turn.id} · $who") {
        Text(turn.text, style = HermesType.body, color = c.ink)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            TruthBadge(
                text = turn.source.name,
                color = if (turn.source == ConversationTurn.Source.TOWER) c.energy else c.ok,
            )
            TruthBadge(
                text = turn.delivery.name,
                color = when (turn.delivery) {
                    ConversationTurn.Delivery.DELIVERED -> c.ok
                    ConversationTurn.Delivery.PENDING -> c.inkSecondary
                    ConversationTurn.Delivery.QUEUED -> c.warn
                    ConversationTurn.Delivery.FAILED -> c.danger
                },
            )
            if (turn.consequential) TruthBadge("CONSEQUENTIAL", c.warn)
            turn.receiptRef?.let { TruthBadge(it, c.gold) }
        }
        turn.note?.let { Text(it, style = HermesType.bodySecondary, color = c.inkSecondary) }
        // Retry only when safe (FR-16): failed or queued, non-consequential turns.
        val retryable = !turn.consequential &&
            (turn.delivery == ConversationTurn.Delivery.FAILED || turn.delivery == ConversationTurn.Delivery.QUEUED)
        if (retryable) {
            TextButton(onClick = onRetry) { Text("Retry (safe: non-consequential)") }
        }
    }
}
