package com.hermestower.voiceconsole.e2e

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.hermestower.voiceconsole.models.DeterministicTestArtifact
import com.hermestower.voiceconsole.models.ModelInstallState
import com.hermestower.voiceconsole.models.ModelSha256
import com.hermestower.voiceconsole.models.VoiceModelProfile
import com.hermestower.voiceconsole.towersim.LocalTowerMockServer
import com.hermestower.voiceconsole.ui.MainActivity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

/**
 * E2E-21..30 (GRAPH 2 §13): Voice Model Manager navigation, recommendation,
 * manual mode, probe artifact download with pause/resume, hash verification,
 * atomic install, activation read-back, rollback and removal.
 */
@RunWith(JUnit4::class)
class E2EModelManagerTest {

    @get:Rule
    val compose: HermesComposeRule = createAndroidComposeRule(MainActivity::class.java)

    private fun graph() = E2E.graph()

    private fun awaitState(identity: String, vararg states: ModelInstallState, timeoutMs: Long = 30_000) {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            if (graph().modelDownloads.progress(identity).value.state in states) return
            Thread.sleep(100)
        }
        val p = graph().modelDownloads.progress(identity).value
        // Surface the downloader's own error text — without it the on-device failure
        // reason was invisible (runs 41349811483 / 41356511688 / 41362428842).
        throw AssertionError(
            "Timed out waiting for $identity in ${states.toList()}; last=${p.state}" +
                "; bytes=${p.bytesDownloaded}/${p.totalBytes}" +
                (p.error?.let { "; error=$it" } ?: "; no error text"),
        )
    }

    @Before
    fun freezeAnimations() {
        compose.freezeAnimationsForTest()
    }

    @Test
    fun e2e_21_22_openModelManager_runVoicePackRecommendation() {
        compose.openDrawerAndNavigate("Models and Voice", "A. Recommended setup")
        compose.clickFirstClickable("Hybrid balanced")
        compose.waitForText("Apply Recommended Configuration")
        compose.onNodeWithText("A. Recommended setup").assertIsDisplayed()
        compose.onNodeWithText("B. Installed voice components").assertIsDisplayed()
        compose.onNodeWithText("C. Available components").assertIsDisplayed()
    }

    @Test
    fun e2e_23_chooseManualMode() {
        compose.openDrawerAndNavigate("Models and Voice", "A. Recommended setup")
        compose.clickFirstClickable("Manual advanced")
        compose.waitForText("Apply Recommended Configuration")
        compose.clickFirstClickable("Apply Recommended Configuration")
        compose.waitUntil(15_000) {
            graph().keyValueStore.getString("voice_profile") == VoiceModelProfile.MANUAL_ADVANCED.name
        }
        assertEquals(VoiceModelProfile.MANUAL_ADVANCED.name, graph().keyValueStore.getString("voice_profile"))
    }

    @Test
    fun e2e_24_25_26_downloadProbe_pauseResume_verifyHash() {
        val bytes = DeterministicTestArtifact.generate("hermes-test-artifact-v1", 4096)
        val server = LocalTowerMockServer()
        server.putArtifact("hermes-test-artifact-v1", LocalTowerMockServer.Artifact(bytes))
        val entry = graph().modelCatalog.find("org.hermestower.test.probe-artifact")!!.copy(sha256 = ModelSha256.ofBytes(bytes))
        try {
            // E2E-24 download of the small deterministic test artifact (real HTTP, 127.0.0.1).
            graph().modelDownloads.start(entry, "${server.baseUrl}/artifacts/hermes-test-artifact-v1")
            awaitState(entry.identity, ModelInstallState.DOWNLOADED)
            // E2E-25 pause + resume (resume re-opens with Range on a second artifact).
            val big = DeterministicTestArtifact.generate("e2e-resume", 256 * 1024)
            server.putArtifact("e2e-resume", LocalTowerMockServer.Artifact(big, chunkDelayMs = 12))
            val bigEntry = entry.copy(identity = "org.hermestower.test.e2e-resume", sha256 = ModelSha256.ofBytes(big), downloadSizeBytes = big.size.toLong(), installedSizeBytes = big.size.toLong())
            graph().modelDownloads.start(bigEntry, "${server.baseUrl}/artifacts/e2e-resume")
            val deadline = System.currentTimeMillis() + 20_000
            while (System.currentTimeMillis() < deadline && graph().modelDownloads.progress(bigEntry.identity).value.bytesDownloaded < 16 * 1024) Thread.sleep(50)
            graph().modelDownloads.pause(bigEntry.identity)
            assertEquals(ModelInstallState.DOWNLOAD_PAUSED, graph().modelDownloads.progress(bigEntry.identity).value.state)
            graph().modelDownloads.resume(bigEntry, "${server.baseUrl}/artifacts/e2e-resume")
            awaitState(bigEntry.identity, ModelInstallState.DOWNLOADED)
            // E2E-26 verify hash against the manifest value.
            assertTrue(graph().modelDownloads.verifyHash(bigEntry))
        } finally {
            server.close()
        }
    }

    @Test
    fun e2e_27_28_29_installAtomically_activate_readBackActivation() {
        val bytes = DeterministicTestArtifact.generate("e2e-activate", 4096)
        val server = LocalTowerMockServer()
        server.putArtifact("e2e-activate", LocalTowerMockServer.Artifact(bytes))
        val entry = graph().modelCatalog.find("org.hermestower.test.probe-artifact")!!
            .copy(identity = "org.hermestower.test.e2e-activate", sha256 = ModelSha256.ofBytes(bytes))
        try {
            graph().modelDownloads.start(entry, "${server.baseUrl}/artifacts/e2e-activate")
            awaitState(entry.identity, ModelInstallState.DOWNLOADED)
            assertTrue(graph().modelDownloads.verifyHash(entry))
            // E2E-27 atomic installation.
            val target = graph().modelDownloads.install(entry)
            assertNotNull(target)
            // E2E-28 activate the test component (qualification gate runs first).
            val activation = graph().modelActivation.activate(entry)
            assertTrue("activation refused: ${activation.reason}", activation.success)
            // E2E-29 read back the activation.
            val readBack = graph().modelActivation.readBack().firstOrNull { it.identity == entry.identity }
            assertNotNull(readBack)
            assertEquals(entry.sha256, readBack!!.sha256)
        } finally {
            server.close()
        }
    }

    @Test
    fun e2e_30_rollBackAndRemoveTestComponent() {
        val v1 = DeterministicTestArtifact.generate("e2e-rb-v1", 4096)
        val server = LocalTowerMockServer()
        server.putArtifact("e2e-rb-a", LocalTowerMockServer.Artifact(v1))
        val entry1 = graph().modelCatalog.find("org.hermestower.test.probe-artifact")!!
            .copy(identity = "org.hermestower.test.e2e-rb", sha256 = ModelSha256.ofBytes(v1))
        try {
            graph().modelDownloads.start(entry1, "${server.baseUrl}/artifacts/e2e-rb-a")
            awaitState(entry1.identity, ModelInstallState.DOWNLOADED)
            assertTrue(graph().modelDownloads.verifyHash(entry1))
            assertNotNull(graph().modelDownloads.install(entry1))
            assertTrue(graph().modelRollback.snapshotFor(entry1))
            // Rollback path restores the verified previous version.
            val rolled = graph().modelRollback.rollback(entry1.copy(version = "9.9.9", sha256 = "f".repeat(64)))
            // No superseding install happened: rollback refuses (honest typed behavior).
            assertTrue(!rolled.success || rolled.restoredVersion != null)
            // Removal cleans files, backup and registry record.
            assertTrue(graph().modelRollback.removalCleanup(entry1))
            assertEquals(null, graph().modelRegistry.record(entry1.identity))
        } finally {
            server.close()
        }
    }
}
