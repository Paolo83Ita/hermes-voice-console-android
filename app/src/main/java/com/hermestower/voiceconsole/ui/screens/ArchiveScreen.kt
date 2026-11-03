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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.hermestower.voiceconsole.R as Res
import com.hermestower.voiceconsole.archive.ArchiveConversation
import com.hermestower.voiceconsole.archive.ArchiveMission
import com.hermestower.voiceconsole.archive.ArchiveSource
import com.hermestower.voiceconsole.archive.MockTowerArchive
import com.hermestower.voiceconsole.ui.components.SectionCard
import com.hermestower.voiceconsole.ui.components.MeanderBand
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
        Text(stringResource(Res.string.archive_title), style = HermesType.screenTitle, color = c.ink)
        MeanderBand(colors = hermesColors, modifier = Modifier.padding(top = 6.dp))
        Text(stringResource(Res.string.archive_subtitle), style = MonumentalType.sealSub, color = m.gold)
        TruthBadge(stringResource(Res.string.archive_simulated_badge), c.inkSecondary)

        TabRow(selectedTabIndex = tab) {
            listOf(
                stringResource(Res.string.archive_tab_conversations),
                stringResource(Res.string.archive_tab_missions),
                stringResource(Res.string.archive_tab_artifacts),
            ).forEachIndexed { index, label ->
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
            placeholder = { Text(stringResource(Res.string.archive_search_hint)) },
        )

        if (tab == 0) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(null to stringResource(Res.string.archive_filter_all), ArchiveSource.LOCAL to "LOCAL", ArchiveSource.TOWER to "TOWER").forEach { (src, label) ->
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
                                Text(stringResource(Res.string.archive_load_more))
                            }
                        }
                    }
                }
                transcriptFor?.let { id ->
                    AlertDialog(
                        onDismissRequest = { transcriptFor = null },
                        confirmButton = {
                            androidx.compose.material3.TextButton(onClick = { transcriptFor = null }) { Text(stringResource(Res.string.common_close)) }
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
                                Text(stringResource(Res.string.archive_load_more))
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
                                Text(stringResource(Res.string.archive_load_more))
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
            "${conv.id} · ${conv.source.name} · " + stringResource(Res.string.archive_meta_turns, conv.turnCount) +
                " · " + (conv.projectId ?: stringResource(Res.string.archive_meta_no_project)) +
                (conv.missionId?.let { " · " + stringResource(Res.string.archive_meta_mission, it) } ?: ""),
            style = HermesType.bodySecondary,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            androidx.compose.material3.TextButton(onClick = onOpen) { Text(stringResource(Res.string.archive_open_transcript)) }
            if (conv.missionId != null) {
                Text(stringResource(Res.string.archive_mission_linked), style = HermesType.bodySecondary)
            }
        }
    }
}

@Composable
private fun MissionRow(mission: ArchiveMission, archive: MockTowerArchive) {
    SectionCard(title = mission.title) {
        Text(
            "${mission.id} · ${mission.state} · " +
                stringResource(Res.string.archive_meta_graph, mission.graph) + " · " +
                stringResource(Res.string.archive_meta_phase, mission.phase),
            style = HermesType.bodySecondary,
        )
        Text(
            stringResource(Res.string.archive_meta_requested, mission.requestedMode, mission.effectiveMode) +
                (mission.checkpoint?.let { " · " + stringResource(Res.string.archive_meta_checkpoint, it) } ?: ""),
            style = HermesType.bodySecondary,
        )
        if (mission.blockers.isNotEmpty()) {
            Text(stringResource(Res.string.archive_meta_blockers, mission.blockers.joinToString()), style = HermesType.bodySecondary)
        }
        Text(
            stringResource(Res.string.archive_meta_artifacts, mission.artifactIds.size, mission.receiptIds.size) +
                if (mission.continuationEligible) " " + stringResource(Res.string.archive_meta_continuation_ok)
                else " " + stringResource(Res.string.archive_meta_continuation_no),
            style = HermesType.bodySecondary,
        )
        androidx.compose.material3.TextButton(onClick = { archive.requestContinuation(mission.id) }) {
            Text(stringResource(Res.string.archive_request_continuation))
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
            androidx.compose.material3.TextButton(onClick = { archive.download(artifact.id) }) { Text(stringResource(Res.string.common_download)) }
            androidx.compose.material3.TextButton(onClick = { archive.removeLocalCopy(artifact.id) }) { Text(stringResource(Res.string.archive_remove_local)) }
            androidx.compose.material3.TextButton(onClick = { archive.verifyHash(artifact.id) }) { Text(stringResource(Res.string.archive_verify_hash)) }
        }
    }
}
