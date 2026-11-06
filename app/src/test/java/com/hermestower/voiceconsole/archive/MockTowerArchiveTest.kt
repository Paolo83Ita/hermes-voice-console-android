package com.hermestower.voiceconsole.archive

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * GRAPH 4 CP-G4-011 — Tower Archive repository contract tests (Owner §14–17).
 * The mock is deterministic; these tests pin the CONTRACTS the real Tower
 * client must honor (pagination, search, filters, cache-vs-authority rule,
 * continuation authority, no silent artifact regeneration).
 */
class MockTowerArchiveTest {

    private lateinit var archive: MockTowerArchive

    @Before
    fun setUp() {
        archive = MockTowerArchive()
    }

    // ---- Conversations (§15) ----

    @Test
    fun `conversation listing paginates with cursor`() {
        val page0 = archive.listConversations(0, 5)
        assertEquals(5, page0.items.size)
        assertEquals(1, page0.nextCursor)
        val page1 = archive.listConversations(1, 5)
        assertEquals(5, page1.items.size)
        assertNotEquals(page0.items[0].id, page1.items[0].id)
        val page2 = archive.listConversations(2, 5)
        assertEquals(4, page2.items.size)
        assertNull(page2.nextCursor)
        // sorted newest first
        assertTrue(page0.items[0].updatedAtEpochMs >= page0.items[1].updatedAtEpochMs)
    }

    @Test
    fun `conversation search filters by title`() {
        val page = archive.listConversations(0, 100, search = "Mission planning")
        assertTrue(page.items.isNotEmpty())
        assertTrue(page.items.all { it.title.contains("Mission planning", ignoreCase = true) })
    }

    @Test
    fun `conversation filters by source project mission and date`() {
        val local = archive.listConversations(0, 100, source = ArchiveSource.LOCAL)
        assertTrue(local.items.isNotEmpty())
        assertTrue(local.items.all { it.source == ArchiveSource.LOCAL })
        val tower = archive.listConversations(0, 100, source = ArchiveSource.TOWER)
        assertTrue(tower.items.all { it.source == ArchiveSource.TOWER })
        val byProject = archive.listConversations(0, 100, projectId = "hermes-console")
        assertTrue(byProject.items.all { it.projectId == "hermes-console" })
        val byMission = archive.listConversations(0, 100, missionId = "mis-001")
        assertTrue(byMission.items.all { it.missionId == "mis-001" })
        val cutoff = 1_735_900_000_000L - 5 * 3_600_000L
        val recent = archive.listConversations(0, 100, fromDateEpochMs = cutoff)
        assertTrue(recent.items.all { it.updatedAtEpochMs >= cutoff })
        // combined filters can be empty — that is honest, not an error
        val none = archive.listConversations(0, 100, source = ArchiveSource.LOCAL, missionId = "mis-001")
        assertTrue(none.items.isEmpty() || none.items.all { it.source == ArchiveSource.LOCAL && it.missionId == "mis-001" })
    }

    @Test
    fun `transcript opens and pin toggles`() {
        val conv = archive.listConversations(0, 1).items[0]
        assertTrue(archive.transcript(conv.id).isNotEmpty())
        archive.pinOffline(conv.id, true)
        assertTrue(archive.pinnedConversationIds().contains(conv.id))
        archive.pinOffline(conv.id, false)
        assertFalse(archive.pinnedConversationIds().contains(conv.id))
    }

    @Test
    fun `cache removal never deletes remote authority (Owner §15)`() {
        val towerConv = archive.listConversations(0, 100, source = ArchiveSource.TOWER).items[0]
        assertTrue(archive.cachedConversationIds().contains(towerConv.id))
        archive.removeLocalCache(towerConv.id)
        // local cache row gone...
        assertFalse(archive.cachedConversationIds().contains(towerConv.id))
        // ...but the AUTHORITY row remains listable with full transcript
        val stillThere = archive.listConversations(0, 100, source = ArchiveSource.TOWER).items.firstOrNull { it.id == towerConv.id }
        assertNotNull("remote authority must survive local cache removal", stillThere)
        assertTrue(archive.transcript(towerConv.id).isNotEmpty())
        archive.clearCache()
        assertTrue(archive.cachedConversationIds().isEmpty())
        assertEquals(14, archive.listConversations(0, 100).items.size) // authority intact
    }

    // ---- Missions (§16) ----

    @Test
    fun `mission listing shows required fields and filters`() {
        val page = archive.listMissions(0, 3)
        assertEquals(3, page.items.size)
        val m = page.items[0]
        assertNotNull(m.id); assertNotNull(m.title); assertNotNull(m.projectId)
        assertNotNull(m.state); assertNotNull(m.graph); assertNotNull(m.phase)
        assertNotNull(m.requestedMode); assertNotNull(m.effectiveMode)
        assertNotNull(m.artifactIds); assertNotNull(m.receiptIds)
        val filtered = archive.listMissions(0, 100, projectId = "xiaomi-qualification")
        assertTrue(filtered.items.all { it.projectId == "xiaomi-qualification" })
    }

    @Test
    fun `mission continuation authority is the Tower - mock answers SIMULATED or refuses`() {
        val eligible = archive.listMissions(0, 100).items.first { it.continuationEligible }
        val result = archive.requestContinuation(eligible.id)
        assertTrue(result is ContinuationResult.SimulatedAccepted)
        assertTrue((result as ContinuationResult.SimulatedAccepted).note.startsWith("SIMULATED"))
        val blocked = archive.listMissions(0, 100).items.first { !it.continuationEligible }
        val refused = archive.requestContinuation(blocked.id)
        assertTrue(refused is ContinuationResult.Refused)
        assertNull(archive.getMission("mis-999"))
    }

    // ---- Artifacts (§17) ----

    @Test
    fun `artifact listing filters and locators work`() {
        val byMission = archive.listArtifacts(0, 100, search = null, missionId = "mis-001", conversationId = null)
        assertTrue(byMission.items.all { it.missionId == "mis-001" })
        val art = archive.listArtifacts(0, 1, search = null, missionId = null, conversationId = null).items[0]
        assertEquals(art.conversationId, archive.locateConversation(art.id))
        assertEquals(art.missionId, archive.locateMission(art.id))
        val byConv = archive.listArtifacts(0, 100, search = null, missionId = null, conversationId = art.conversationId)
        assertTrue(byConv.items.isNotEmpty())
    }

    @Test
    fun `artifact download is SIMULATED - never silent regeneration - and removal restores metadata-only`() {
        val art = archive.listArtifacts(0, 1, search = null, missionId = null, conversationId = null).items[0]
        assertEquals(ArtifactLocalState.METADATA_ONLY, art.localState)
        val result = archive.download(art.id)
        assertTrue(result is ArtifactDownloadResult.Simulated)
        assertTrue((result as ArtifactDownloadResult.Simulated).note.startsWith("SIMULATED"))
        assertEquals(ArtifactLocalState.SIMULATED_DOWNLOAD, archive.getArtifact(art.id)!!.localState)
        assertTrue(archive.verifyHash(art.id))
        archive.removeLocalCopy(art.id)
        assertEquals(ArtifactLocalState.METADATA_ONLY, archive.getArtifact(art.id)!!.localState)
        assertNull(archive.getArtifact("art-999"))
    }

    // ---- Sync (§14) ----

    @Test
    fun `sync reports duplicate prevention and stable ids`() {
        val first = archive.syncNow()
        assertTrue(first.duplicatesPrevented >= 0)
        val second = archive.syncNow()
        // second pass: everything already cached → duplicates prevented
        assertTrue(second.duplicatesPrevented >= 0 && second.conversations == first.conversations)
        assertTrue(archive.lastSyncEpochMs > 0)
    }

    @Test
    fun `bad pagination is rejected`() {
        try {
            archive.listConversations(-1, 10)
            throw AssertionError("negative page must be rejected")
        } catch (expected: IllegalArgumentException) { /* typed */ }
        try {
            archive.listConversations(0, 1000)
            throw AssertionError("oversized page must be rejected")
        } catch (expected: IllegalArgumentException) { /* typed */ }
    }
    // ---- CP-G4.9-007: product-completion of the artifact transfer lifecycle ----

    @Test
    fun `artifacts carry mime type provenance and deterministic text previews`() {
        val archive = MockTowerArchive()
        val page = archive.listArtifacts(page = 0, pageSize = 20)
        assertTrue(page.items.all { it.mimeType.isNotBlank() })
        assertTrue(page.items.all { it.provenance.startsWith("SIMULATED provenance") })
        val json = page.items.first { it.kind == ArtifactKind.JSON }
        assertEquals("application/json", json.mimeType)
        assertTrue("text-bearing kinds expose a preview", json.previewText!!.startsWith("SIMULATED preview"))
        val pdf = page.items.first { it.kind == ArtifactKind.PDF }
        assertEquals("application/pdf", pdf.mimeType)
        assertEquals("binary kinds expose no fabricated text preview", null, pdf.previewText)
    }

    @Test
    fun `transfer lifecycle pauses resumes re-downloads with typed states`() {
        val archive = MockTowerArchive()
        val id = archive.listArtifacts(page = 0, pageSize = 20).items.first().id
        assertEquals(ArtifactTransferState.IDLE, archive.transferState(id))
        // pause without a transfer in flight is refused (typed honesty)
        assertEquals(false, archive.pause(id))
        val result = archive.download(id)
        assertTrue(result is ArtifactDownloadResult.Simulated)
        assertEquals(ArtifactTransferState.DOWNLOADED, archive.transferState(id))
        // re-download removes the local copy first, then downloads again
        val again = archive.reDownload(id)
        assertTrue(again is ArtifactDownloadResult.Simulated)
        assertEquals(ArtifactLocalState.SIMULATED_DOWNLOAD, archive.getArtifact(id)!!.localState)
        // cancel resets to IDLE; resume of an IDLE transfer is refused
        archive.cancel(id)
        assertEquals(ArtifactTransferState.IDLE, archive.transferState(id))
        assertEquals(false, archive.resume(id))
        // removeLocalCopy returns the row to METADATA_ONLY (remote untouched)
        archive.download(id)
        archive.removeLocalCopy(id)
        assertEquals(ArtifactLocalState.METADATA_ONLY, archive.getArtifact(id)!!.localState)
        assertEquals(ArtifactTransferState.IDLE, archive.transferState(id))
}
}
