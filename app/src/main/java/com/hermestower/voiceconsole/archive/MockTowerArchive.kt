package com.hermestower.voiceconsole.archive

/**
 * GRAPH 4 CP-G4-011: DETERMINISTIC MOCK implementation of the Tower Archive
 * repositories (Owner §14: explicit SIMULATED labels, no invented production
 * endpoints, no claim of recovered historical data). Data is generated from a
 * fixed seed — same rows on every run, so tests and UI are deterministic.
 */
class MockTowerArchive(
    private val nowEpochMs: () -> Long = { 1_735_900_000_000L }, // fixed deterministic clock
) : ConversationRepository, MissionRepository, ArtifactRepository, ArtifactDownloadManager, ArchiveCacheStore, ArchiveSyncController, TowerArchiveClient {

    override val simulated: Boolean = true

    private val conversations = ArrayList<ArchiveConversation>()
    private val missions = ArrayList<ArchiveMission>()
    private val artifacts = ArrayList<ArchiveArtifact>()
    private val transcripts = HashMap<String, List<String>>()
    private val pinned = HashSet<String>()
    private val cached = HashSet<String>()
    private val downloaded = HashSet<String>()

    override var lastSyncEpochMs: Long = 0L
        private set

    init {
        seed()
    }

    private fun seed() {
        val base = nowEpochMs()
        val projects = listOf("hermes-console", "tower-graph-1-7", "xiaomi-qualification")
        // 14 deterministic conversations: alternating LOCAL/TOWER, some with missions
        for (i in 0 until 14) {
            val id = "conv-%03d".format(i + 1)
            val source = if (i % 3 == 0) ArchiveSource.LOCAL else ArchiveSource.TOWER
            val missionId = if (i % 4 == 1) "mis-%03d".format(i / 4 + 1) else null
            val projectId = projects[i % projects.size]
            conversations += ArchiveConversation(
                id = id,
                title = when (i % 4) {
                    0 -> "Console session $i"
                    1 -> "Mission planning $i"
                    2 -> "Voice dictation $i"
                    else -> "Archive review $i"
                },
                projectId = projectId,
                missionId = missionId,
                source = source,
                updatedAtEpochMs = base - i * 3_600_000L,
                turnCount = 4 + i,
                pinnedOffline = i == 2,
            )
            transcripts[id] = listOf(
                "OWNER: session $i opening utterance",
                "HERMES ($source): deterministic reply $i",
                "OWNER: follow-up",
                "HERMES ($source): closing receipt",
            )
            if (source == ArchiveSource.TOWER) cached += id
        }
        // 5 deterministic missions (§16 fields)
        val states = listOf("RUNNING", "BLOCKED", "COMPLETED", "RUNNING", "CHECKPOINTED")
        for (m in 0 until 5) {
            val id = "mis-%03d".format(m + 1)
            missions += ArchiveMission(
                id = id,
                title = "Mission $m — ${projects[m % projects.size]}",
                projectId = projects[m % projects.size],
                state = states[m],
                graph = "graph-${m + 1}",
                phase = "phase-${m + 2}",
                createdAtEpochMs = base - (m + 3) * 86_400_000L,
                updatedAtEpochMs = base - m * 7_200_000L,
                checkpoint = if (m % 2 == 0) "CP-G${m + 1}-00$m" else null,
                requestedMode = if (m % 2 == 0) "TOWER" else "HYBRID",
                effectiveMode = if (m == 1) "DISCONNECTED_SAFE" else "TOWER",
                blockers = if (m == 1) listOf("emulator environment limitation") else emptyList(),
                ownerAction = if (m == 1) "review blocker register" else null,
                artifactIds = listOf("art-%03d".format(m * 2 + 1), "art-%03d".format(m * 2 + 2)),
                receiptIds = listOf("rcp-m%03d".format(m + 1)),
                continuationEligible = m != 1,
            )
        }
        // 10 deterministic artifacts across kinds (§17 types)
        val kinds = listOf(
            ArtifactKind.PDF, ArtifactKind.DOCX, ArtifactKind.XLSX, ArtifactKind.PPTX,
            ArtifactKind.HTML, ArtifactKind.MARKDOWN, ArtifactKind.JSON, ArtifactKind.CSV,
            ArtifactKind.IMAGE, ArtifactKind.ZIP,
        )
        for (a in 0 until 10) {
            val id = "art-%03d".format(a + 1)
            val kind = kinds[a]
            val convId = conversations[a % conversations.size].id
            val misId = if (a % 3 == 0) "mis-%03d".format(a / 3 % 5 + 1) else null
            artifacts += ArchiveArtifact(
                id = id,
                conversationId = convId,
                missionId = misId,
                name = "artifact-${a + 1}.${kind.name.lowercase()}",
                kind = kind,
                sizeBytes = 1024L * (37 + a * 11),
                sha256 = "seed%064d".format(a),
                localState = ArtifactLocalState.METADATA_ONLY,
                // CP-G4.9-007: MIME + provenance + deterministic text preview.
                mimeType = mimeFor(kind),
                provenance = "SIMULATED provenance: deterministic seed row ${a + 1} (conversation $convId" +
                    (misId?.let { ", mission $it" } ?: "") + "); real recovery waits for Tower Graph 1.7.",
                previewText = previewFor(kind, a + 1),
            )
        }
    }

    // ---- ConversationRepository ----
    override fun listConversations(
        page: Int,
        pageSize: Int,
        search: String?,
        source: ArchiveSource?,
        projectId: String?,
        missionId: String?,
        fromDateEpochMs: Long?,
        toDateEpochMs: Long?,
    ): ArchivePage<ArchiveConversation> {
        require(page >= 0 && pageSize in 1..100) { "bad page/pageSize" }
        var rows = conversations.asSequence()
        search?.takeIf { it.isNotBlank() }?.let { q ->
            rows = rows.filter { it.title.contains(q, ignoreCase = true) || it.id.contains(q, ignoreCase = true) }
        }
        source?.let { src -> rows = rows.filter { it.source == src } }
        projectId?.let { pid -> rows = rows.filter { it.projectId == pid } }
        missionId?.let { mid -> rows = rows.filter { it.missionId == mid } }
        fromDateEpochMs?.let { fromMs -> rows = rows.filter { it.updatedAtEpochMs >= fromMs } }
        toDateEpochMs?.let { toMs -> rows = rows.filter { it.updatedAtEpochMs <= toMs } }
        val sorted = rows.sortedByDescending { it.updatedAtEpochMs }.toList()
        val from = page * pageSize
        if (from >= sorted.size) return ArchivePage(emptyList(), null)
        val to = minOf(from + pageSize, sorted.size)
        val next = if (to < sorted.size) page + 1 else null
        return ArchivePage(sorted.subList(from, to), next)
    }

    override fun transcript(conversationId: String): List<String> =
        transcripts[conversationId] ?: emptyList()

    override fun pinOffline(conversationId: String, pinned: Boolean) {
        if (pinned) this.pinned += conversationId else this.pinned -= conversationId
    }

    override fun removeLocalCache(conversationId: String) {
        // §15: removes the LOCAL cache row only — the remote authority row stays.
        cached -= conversationId
    }

    // ---- MissionRepository ----
    override fun listMissions(page: Int, pageSize: Int, search: String?, projectId: String?): ArchivePage<ArchiveMission> {
        require(page >= 0 && pageSize in 1..100) { "bad page/pageSize" }
        var rows = missions.asSequence()
        search?.takeIf { it.isNotBlank() }?.let { q ->
            rows = rows.filter { it.title.contains(q, ignoreCase = true) || it.id.contains(q, ignoreCase = true) }
        }
        projectId?.let { pid -> rows = rows.filter { it.projectId == pid } }
        val sorted = rows.sortedByDescending { it.updatedAtEpochMs }.toList()
        val from = page * pageSize
        if (from >= sorted.size) return ArchivePage(emptyList(), null)
        val to = minOf(from + pageSize, sorted.size)
        return ArchivePage(sorted.subList(from, to), if (to < sorted.size) page + 1 else null)
    }

    override fun getMission(missionId: String): ArchiveMission? = missions.firstOrNull { it.id == missionId }

    override fun requestContinuation(missionId: String): ContinuationResult {
        val mission = getMission(missionId) ?: return ContinuationResult.Refused("unknown mission")
        return if (mission.continuationEligible) {
            // §16: the REAL Tower accepts; the mock reports SIMULATED truthfully.
            ContinuationResult.SimulatedAccepted(
                "SIMULATED: continuation request for ${mission.id} would be accepted by the real Tower",
            )
        } else {
            ContinuationResult.Refused("mission ${mission.id} is not continuation-eligible (blockers: ${mission.blockers})")
        }
    }

    // ---- ArtifactRepository ----
    override fun listArtifacts(page: Int, pageSize: Int, search: String?, missionId: String?, conversationId: String?): ArchivePage<ArchiveArtifact> {
        require(page >= 0 && pageSize in 1..100) { "bad page/pageSize" }
        var rows = artifacts.asSequence()
        search?.takeIf { it.isNotBlank() }?.let { q ->
            rows = rows.filter { it.name.contains(q, ignoreCase = true) || it.id.contains(q, ignoreCase = true) }
        }
        missionId?.let { mid -> rows = rows.filter { it.missionId == mid } }
        conversationId?.let { cid -> rows = rows.filter { it.conversationId == cid } }
        val sorted = rows.toList().sortedBy { it.id }
        val from = page * pageSize
        if (from >= sorted.size) return ArchivePage(emptyList(), null)
        val to = minOf(from + pageSize, sorted.size)
        return ArchivePage(sorted.subList(from, to), if (to < sorted.size) page + 1 else null)
    }

    override fun getArtifact(artifactId: String): ArchiveArtifact? = artifacts.firstOrNull { it.id == artifactId }

    override fun locateConversation(artifactId: String): String? = getArtifact(artifactId)?.conversationId
    override fun locateMission(artifactId: String): String? = getArtifact(artifactId)?.missionId

    // ---- ArtifactDownloadManager (mock: SIMULATED, hash truthfully reported) ----
    override fun download(artifactId: String): ArtifactDownloadResult {
        val artifact = getArtifact(artifactId) ?: return ArtifactDownloadResult.Failed("unknown artifact")
        // §17: never silently regenerate an absent artifact — the mock performs NO
        // byte transfer and labels the row SIMULATED_DOWNLOAD.
        downloaded += artifactId
        transfers[artifactId] = ArtifactTransferState.DOWNLOADED
        artifacts.replaceAll { if (it.id == artifactId) it.copy(localState = ArtifactLocalState.SIMULATED_DOWNLOAD) else it }
        return ArtifactDownloadResult.Simulated(
            "SIMULATED download of ${artifact.name} (${artifact.sizeBytes} B); real transfer waits for Tower Graph 1.7",
        )
    }

    // CP-G4.9-007: typed transfer lifecycle (SIMULATED — no bytes move until
    // Tower Graph 1.7 provides the real routes).
    private val transfers = HashMap<String, ArtifactTransferState>()

    override fun cancel(artifactId: String) {
        // Deterministic mock: an in-flight transfer exists only between
        // download() and its simulated completion; cancel resets it.
        transfers[artifactId] = ArtifactTransferState.IDLE
    }

    override fun pause(artifactId: String): Boolean {
        val current = transfers[artifactId] ?: ArtifactTransferState.IDLE
        if (current != ArtifactTransferState.DOWNLOADING) return false
        transfers[artifactId] = ArtifactTransferState.PAUSED
        return true
    }

    override fun resume(artifactId: String): Boolean {
        val current = transfers[artifactId] ?: ArtifactTransferState.IDLE
        if (current != ArtifactTransferState.PAUSED) return false
        // Resuming lands back at the simulated completion (the mock never keeps
        // bytes in flight); the row stays honestly SIMULATED_DOWNLOAD.
        transfers[artifactId] = ArtifactTransferState.DOWNLOADED
        downloaded += artifactId
        artifacts.replaceAll { if (it.id == artifactId) it.copy(localState = ArtifactLocalState.SIMULATED_DOWNLOAD) else it }
        return true
    }

    override fun reDownload(artifactId: String): ArtifactDownloadResult {
        removeLocalCopy(artifactId)
        return download(artifactId)
    }

    override fun transferState(artifactId: String): ArtifactTransferState =
        transfers[artifactId]
            ?: if (downloaded.contains(artifactId)) ArtifactTransferState.DOWNLOADED
            else ArtifactTransferState.IDLE

    override fun removeLocalCopy(artifactId: String) {
        downloaded -= artifactId
        // No local copy ⇒ no transfer state (the row returns to metadata-only).
        transfers[artifactId] = ArtifactTransferState.IDLE
        artifacts.replaceAll { if (it.id == artifactId) it.copy(localState = ArtifactLocalState.METADATA_ONLY) else it }
    }

    override fun verifyHash(artifactId: String): Boolean {
        val artifact = getArtifact(artifactId) ?: return false
        return artifact.sha256 != null && artifact.sha256.isNotEmpty()
    }

    // ---- ArchiveCacheStore ----
    override fun cachedConversationIds(): Set<String> = cached.toSet()
    override fun pinnedConversationIds(): Set<String> = pinned.toSet()
    override fun clearCache() {
        // §15: local-cache deletion must not delete remote authority — the mock's
        // authoritative rows live in [conversations] and are untouched here.
        cached.clear()
    }

    // ---- ArchiveSyncController ----
    override fun syncNow(): SyncReport {
        val before = cached.size
        // deterministic re-discovery: TOWER rows are cacheable; duplicates prevented by id
        var duplicates = 0
        conversations.filter { it.source == ArchiveSource.TOWER }.forEach { c ->
            if (!cached.add(c.id)) duplicates++
        }
        lastSyncEpochMs = nowEpochMs()
        return SyncReport(
            conversations = conversations.size,
            missions = missions.size,
            artifacts = artifacts.size,
            duplicatesPrevented = duplicates + (if (before > 0) before else 0),
        )
    }
}

/** Honest MIME mapping of the artifact kinds (CP-G4.9-007). */
private fun mimeFor(kind: ArtifactKind): String = when (kind) {
    ArtifactKind.PDF -> "application/pdf"
    ArtifactKind.DOCX -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    ArtifactKind.XLSX -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
    ArtifactKind.PPTX -> "application/vnd.openxmlformats-officedocument.presentationml.presentation"
    ArtifactKind.HTML -> "text/html"
    ArtifactKind.TXT -> "text/plain"
    ArtifactKind.MARKDOWN -> "text/markdown"
    ArtifactKind.JSON -> "application/json"
    ArtifactKind.CSV -> "text/csv"
    ArtifactKind.IMAGE -> "image/png"
    ArtifactKind.AUDIO -> "audio/wav"
    ArtifactKind.ZIP -> "application/zip"
    ArtifactKind.EVIDENCE_REGISTER -> "application/json"
    ArtifactKind.MISSION_RECEIPT -> "application/json"
}

/** Deterministic text preview for text-bearing kinds (CP-G4.9-007). */
private fun previewFor(kind: ArtifactKind, index: Int): String? = when (kind) {
    ArtifactKind.TXT, ArtifactKind.MARKDOWN, ArtifactKind.JSON, ArtifactKind.CSV,
    ArtifactKind.HTML, ArtifactKind.EVIDENCE_REGISTER, ArtifactKind.MISSION_RECEIPT,
    ->
        buildString {
            append("SIMULATED preview — deterministic seed row ").append(index).append(". ")
            append("The real content is recovered from Hermes Tower when Tower Graph 1.7 provides the artifact routes; ")
            append("no historical data is claimed here.")
        }
    else -> null
}
