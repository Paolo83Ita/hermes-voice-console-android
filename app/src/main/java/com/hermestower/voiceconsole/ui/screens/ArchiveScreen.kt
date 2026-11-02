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
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hermestower.voiceconsole.archive.ArchiveConversation
import com.hermestower.voiceconsole.archive.ArchiveMission
import com.hermestower.voiceconsole.archive.ArchiveSource
import com.hermestower.voiceconsole.archive.MockTowerArchive
import com.hermestower.voiceconsole.ui.components.SectionCard
import com.hermestower.voiceconsole.ui.components.TruthBadge
import com.hermestower.voiceconsole.ui.monumental.MonumentalType
import com.hermestower.voiceconsole.ui.monumental.monumentalColors
import com.hermestower.voiceconsole.ui.theme.HermesType
import com.hermestower.voiceconsole.ui.theme.hermesColors

/**
 * GRAPH 4 CP-G4-011 (Owner §14): TOWER ARCHIVE — "Archive Chamber".
 * Tabs: Conversations / Missions / Artifacts. Every row is deterministically
 * mock data and is labeled SIMULATED — the real Tower stays the authority
 * (Tower Graph 1.7 contract pending). Pinned strings are kept E2E-stable.
 */
@Composable
fun ArchiveScreen(archive: MockTowerArchive) {
    val c = hermesColors
    val m = monumentalColors
    var tab by remember { mutableIntStateOf(0) }
    var search by remember { mutableStateOf("") }
    var sourceFilter by remember { mutableStateOf<ArchiveSource?>(null) }
    var transcriptFor by remember { mutableStateOf<String?>(null) }

    Column(
        Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Tower Archive", style = HermesType.screenTitle, color = c.ink)
        Text("ARCHIVE CHAMBER", style = MonumentalType.sealSub, color = m.gold)
        TruthBadge("SIMULATED DATA — the real Tower stays the authority (Tower Graph 1.7 pending)", c.inkSecondary)

        TabRow(selectedTabIndex = tab) {
            listOf("Conversations", "Missions", "Artifacts").forEachIndexed { index, label ->
                Tab(
                    selected = tab == index,
                    onClick = { tab = index },
                    text = { Text(label) },
                )
            }
        }

        androidx.compose.material3.OutlinedTextField(
            value = search,
            onValueChange = { search = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            placeholder = { Text("Search the archive…") },
        )

        if (tab == 0) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(null to "All", ArchiveSource.LOCAL to "LOCAL", ArchiveSource.TOWER to "TOWER").forEach { (src, label) ->
                    com.hermestower.voiceconsole.ui.components.ModeChip(
                        label = label,
                        selected = sourceFilter == src,
                        enabled = true,
                        onClick = { sourceFilter = src },
                    )
                }
            }
        }

        val pageSize = 8
        when (tab) {
            0 -> {
                var page by remember(search, sourceFilter) { mutableIntStateOf(0) }
                val result = archive.listConversations(page, pageSize, search = search, source = sourceFilter)
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(result.items, key = { it.id }) { conv -> ConversationRow(conv) { transcriptFor = conv.id } }
                    if (result.nextCursor != null) {
                        item {
                            androidx.compose.material3.TextButton(onClick = { page = result.nextCursor }) {
                                Text("Load more")
                            }
                        }
                    }
                }
                transcriptFor?.let { id ->
                    AlertDialog(
                        onDismissRequest = { transcriptFor = null },
                        confirmButton = {
                            androidx.compose.material3.TextButton(onClick = { transcriptFor = null }) { Text("Close") }
                        },
                        title = { Text(id) },
                        text = {
                            Column {
                                archive.transcript(id).forEach { Text(it, style = HermesType.bodySecondary) }
                            }
                        },
                    )
                }
            }
            1 -> {
                var page by remember(search) { mutableIntStateOf(0) }
                val result = archive.listMissions(page, pageSize, search = search)
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(result.items, key = { it.id }) { mission -> MissionRow(mission, archive) }
                    if (result.nextCursor != null) {
                        item {
                            androidx.compose.material3.TextButton(onClick = { page = result.nextCursor }) {
                                Text("Load more")
                            }
                        }
                    }
                }
            }
            else -> {
                var page by remember(search) { mutableIntStateOf(0) }
                val result = archive.listArtifacts(page, pageSize, search = search, missionId = null, conversationId = null)
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(result.items, key = { it.id }) { artifact -> ArtifactRow(artifact, archive) }
                    if (result.nextCursor != null) {
                        item {
                            androidx.compose.material3.TextButton(onClick = { page = result.nextCursor }) {
                                Text("Load more")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ConversationRow(conv: ArchiveConversation, onOpen: () -> Unit) {
    SectionCard(title = conv.title) {
        Text(
            "${conv.id} · ${conv.source.name} · ${conv.turnCount} turns · ${conv.projectId ?: "no project"}" +
                (conv.missionId?.let { " · mission $it" } ?: ""),
            style = HermesType.bodySecondary,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            androidx.compose.material3.TextButton(onClick = onOpen) { Text("Open transcript") }
            if (conv.missionId != null) {
                Text("mission-linked", style = HermesType.bodySecondary)
            }
        }
    }
}

@Composable
private fun MissionRow(mission: ArchiveMission, archive: MockTowerArchive) {
    SectionCard(title = mission.title) {
        Text(
            "${mission.id} · ${mission.state} · graph ${mission.graph} · phase ${mission.phase}",
            style = HermesType.bodySecondary,
        )
        Text(
            "requested ${mission.requestedMode} · effective ${mission.effectiveMode}" +
                (mission.checkpoint?.let { " · checkpoint $it" } ?: ""),
            style = HermesType.bodySecondary,
        )
        if (mission.blockers.isNotEmpty()) {
            Text("Blockers: ${mission.blockers.joinToString()}", style = HermesType.bodySecondary)
        }
        Text(
            "artifacts ${mission.artifactIds.size} · receipts ${mission.receiptIds.size}" +
                if (mission.continuationEligible) " · continuation eligible" else " · continuation NOT eligible",
            style = HermesType.bodySecondary,
        )
        androidx.compose.material3.TextButton(onClick = { archive.requestContinuation(mission.id) }) {
            Text("Request continuation")
        }
    }
}

@Composable
private fun ArtifactRow(artifact: com.hermestower.voiceconsole.archive.ArchiveArtifact, archive: MockTowerArchive) {
    SectionCard(title = artifact.name) {
        Text(
            "${artifact.id} · ${artifact.kind.name} · ${artifact.sizeBytes} B · ${artifact.localState.name}",
            style = HermesType.bodySecondary,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            androidx.compose.material3.TextButton(onClick = { archive.download(artifact.id) }) { Text("Download") }
            androidx.compose.material3.TextButton(onClick = { archive.removeLocalCopy(artifact.id) }) { Text("Remove local") }
            androidx.compose.material3.TextButton(onClick = { archive.verifyHash(artifact.id) }) { Text("Verify hash") }
        }
    }
}
