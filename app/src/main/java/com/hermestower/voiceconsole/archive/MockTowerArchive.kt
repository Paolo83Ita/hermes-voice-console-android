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
            artifacts += ArchiveArtifact(
                id = id,
                conversationId = conversations[a % conversations.size].id,
                missionId = if (a % 3 == 0) "mis-%03d".format(a / 3 % 5 + 1) else null,
                name = "artifact-${a + 1}.${kinds[a].name.lowercase()}",
                kind = kinds[a],
                sizeBytes = 1024L * (37 + a * 11),
                sha256 = "seed%064d".format(a),
                localState = ArtifactLocalState.METADATA_ONLY,
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
        artifacts.replaceAll { if (it.id == artifactId) it.copy(localState = ArtifactLocalState.SIMULATED_DOWNLOAD) else it }
        return ArtifactDownloadResult.Simulated(
            "SIMULATED download of ${artifact.name} (${artifact.sizeBytes} B); real transfer waits for Tower Graph 1.7",
        )
    }

    override fun cancel(artifactId: String) { /* deterministic mock: no in-flight transfer */ }

    override fun removeLocalCopy(artifactId: String) {
        downloaded -= artifactId
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
