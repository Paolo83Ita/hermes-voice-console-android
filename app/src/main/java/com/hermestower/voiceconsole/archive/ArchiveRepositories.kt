package com.hermestower.voiceconsole.archive

/**
 * GRAPH 4 CP-G4-011 (Owner §14–17): TOWER ARCHIVE — versioned repository
 * layer. The REAL Tower remains the authority; until real Tower routes exist
 * (Tower Graph 1.7) everything here runs on a DETERMINISTIC MOCK that is
 * labeled SIMULATED in the UI and never claims recovered historical data.
 *
 * Hard rules encoded in this layer:
 *  - NO invented production endpoints (mock is in-process, zero network);
 *  - local-cache deletion NEVER deletes the remote authority (§15);
 *  - mission continuation authority is the real Tower — cached phone state is
 *    NOT sufficient (§16); the mock returns typed SIMULATED responses;
 *  - an absent artifact is never silently regenerated (§17).
 */

enum class ArchiveSource { LOCAL, TOWER }

data class ArchiveConversation(
    val id: String,
    val title: String,
    val projectId: String?,
    val missionId: String?,
    val source: ArchiveSource,
    val updatedAtEpochMs: Long,
    val turnCount: Int,
    val pinnedOffline: Boolean = false,
)

data class ArchiveMission(
    val id: String,
    val title: String,
    val projectId: String,
    val state: String,
    val graph: String,
    val phase: String,
    val createdAtEpochMs: Long,
    val updatedAtEpochMs: Long,
    val checkpoint: String?,
    val requestedMode: String,
    val effectiveMode: String,
    val blockers: List<String>,
    val ownerAction: String?,
    val artifactIds: List<String>,
    val receiptIds: List<String>,
    val continuationEligible: Boolean,
)

enum class ArtifactKind { PDF, DOCX, XLSX, PPTX, HTML, TXT, MARKDOWN, JSON, CSV, IMAGE, AUDIO, ZIP, EVIDENCE_REGISTER, MISSION_RECEIPT }

data class ArchiveArtifact(
    val id: String,
    val conversationId: String?,
    val missionId: String?,
    val name: String,
    val kind: ArtifactKind,
    val sizeBytes: Long,
    val sha256: String?,
    /** METADATA_ONLY = header known, bytes not on this device. */
    val localState: ArtifactLocalState,
    /** CP-G4.9-007: RFC 4064-style MIME type (honest mapping of the kind). */
    val mimeType: String,
    /** CP-G4.9-007: deterministic provenance line (producer, mission context). */
    val provenance: String,
    /** CP-G4.9-007: deterministic text preview (text-bearing kinds only, else null). */
    val previewText: String? = null,
)

/** CP-G4.9-007: the transfer lifecycle of one artifact (typed, observable). */
enum class ArtifactTransferState { IDLE, DOWNLOADING, PAUSED, DOWNLOADED, FAILED }

enum class ArtifactLocalState { METADATA_ONLY, SIMULATED_DOWNLOAD, DOWNLOADED }

/** One page of results + the cursor for the next page (null = end). */
data class ArchivePage<T>(val items: List<T>, val nextCursor: Int?)

interface ConversationRepository {
    fun listConversations(
        page: Int,
        pageSize: Int,
        search: String? = null,
        source: ArchiveSource? = null,
        projectId: String? = null,
        missionId: String? = null,
        fromDateEpochMs: Long? = null,
        toDateEpochMs: Long? = null,
    ): ArchivePage<ArchiveConversation>

    fun transcript(conversationId: String): List<String>
    fun pinOffline(conversationId: String, pinned: Boolean)
    fun removeLocalCache(conversationId: String)
}

interface MissionRepository {
    fun listMissions(page: Int, pageSize: Int, search: String? = null, projectId: String? = null): ArchivePage<ArchiveMission>
    fun getMission(missionId: String): ArchiveMission?
    /** §16: continuation must be accepted by the REAL Tower; mock = typed SIMULATED. */
    fun requestContinuation(missionId: String): ContinuationResult
}

sealed class ContinuationResult {
    data class SimulatedAccepted(val note: String) : ContinuationResult()
    data class Refused(val reason: String) : ContinuationResult()
}

interface ArtifactRepository {
    fun listArtifacts(page: Int, pageSize: Int, search: String? = null, missionId: String? = null, conversationId: String? = null): ArchivePage<ArchiveArtifact>
    fun getArtifact(artifactId: String): ArchiveArtifact?
    fun locateConversation(artifactId: String): String?
    fun locateMission(artifactId: String): String?
}

interface ArtifactDownloadManager {
    /** §17: controlled download with verify-hash semantics; mock = SIMULATED_DOWNLOAD. */
    fun download(artifactId: String): ArtifactDownloadResult
    fun cancel(artifactId: String)
    fun removeLocalCopy(artifactId: String)
    fun verifyHash(artifactId: String): Boolean

    /** CP-G4.9-007: typed pause/resume/re-download of the transfer lifecycle. */
    fun pause(artifactId: String): Boolean
    fun resume(artifactId: String): Boolean
    fun reDownload(artifactId: String): ArtifactDownloadResult
    fun transferState(artifactId: String): ArtifactTransferState
}

sealed class ArtifactDownloadResult {
    data class Simulated(val note: String) : ArtifactDownloadResult()
    data class Completed(val localPath: String, val sha256: String) : ArtifactDownloadResult()
    data class Failed(val reason: String) : ArtifactDownloadResult()
}

interface ArchiveCacheStore {
    fun cachedConversationIds(): Set<String>
    fun pinnedConversationIds(): Set<String>
    fun clearCache() // clears LOCAL cache rows only — remote authority untouched
}

interface ArchiveSyncController {
    /** §14: explicit simulated sync; duplicate prevention by stable id. */
    fun syncNow(): SyncReport
    val lastSyncEpochMs: Long
}

data class SyncReport(val conversations: Int, val missions: Int, val artifacts: Int, val duplicatesPrevented: Int)

/**
 * Versioned client seam for the REAL Tower (Tower Graph 1.7 contract boundary).
 * No production route is named here — the implementation is injected when the
 * real contract exists (ANDROID_GRAPH4_TOWER_GRAPH1_7_COMPATIBILITY_MATRIX.md).
 */
interface TowerArchiveClient {
    val simulated: Boolean
}
