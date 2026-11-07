package com.hermestower.voiceconsole.selftest

import com.hermestower.voiceconsole.models.DefaultModelActivationController
import com.hermestower.voiceconsole.models.DefaultModelDownloadManager
import com.hermestower.voiceconsole.models.DefaultModelQualificationRunner
import com.hermestower.voiceconsole.models.DefaultModelRollbackController
import com.hermestower.voiceconsole.models.DefaultModelStorageManager
import com.hermestower.voiceconsole.models.DefaultVoiceModelCatalog
import com.hermestower.voiceconsole.models.DefaultVoicePackResolver
import com.hermestower.voiceconsole.models.DeviceResources
import com.hermestower.voiceconsole.models.FileInstallationRegistry
import com.hermestower.voiceconsole.migration.InMemoryKeyValueStore
import com.hermestower.voiceconsole.tower.FileTowerProfileStore
import com.hermestower.voiceconsole.tower.InMemorySecretVault
import com.hermestower.voiceconsole.tower.RealTowerBridge
import com.hermestower.voiceconsole.towersim.LocalTowerMockServer
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files

/**
 * GRAPH 4.11 Node 1 (ST-54 "Voice A/B comparison"): the decision logic of the
 * Owner's qualification shape "Voice A → play → Voice B → play → prove
 * difference", proven on the JVM against scripted audition read-backs. The
 * AUDIBLE difference itself stays Owner-verified on the physical device —
 * this test pins the typed verdicts (pass / fail / blocked / pending-device)
 * and the route-truth invariants (identity distinctness, authority untouched).
 */
class VoiceAbQualificationSelfTestTest {

    private val dir: java.nio.file.Path by lazy { Files.createTempDirectory("hermes-st54") }

    private fun subjectsWith(
        probe: (() -> VoiceAbQualificationReadback?)?,
    ): SelfTestSubjects {
        val storage = DefaultModelStorageManager(
            java.nio.file.Files.createDirectories(dir.resolve("models-st54")),
        )
        val registry = FileInstallationRegistry(dir.resolve("registry-st54.json"))
        val profileStore = FileTowerProfileStore(dir.resolve("tower-st54.json"), InMemorySecretVault())
        // The established deterministic catalog fixture (SelfTestCenterTest precedent).
        val catalog = DefaultVoiceModelCatalog.fromText(
            javaClass.classLoader!!
                .getResourceAsStream("voice_pack_catalog_test.json")!!
                .bufferedReader()
                .readText(),
        )
        return SelfTestSubjects(
            catalog = catalog,
            resolver = DefaultVoicePackResolver(),
            storage = storage,
            registry = registry,
            downloads = DefaultModelDownloadManager(storage, registry, Dispatchers.Default),
            qualificationRunner = DefaultModelQualificationRunner(storage),
            activation = DefaultModelActivationController(registry, DefaultModelQualificationRunner(storage)),
            rollback = DefaultModelRollbackController(storage, registry),
            profileStore = profileStore,
            vault = InMemorySecretVault(),
            realBridge = RealTowerBridge(profileStore, Dispatchers.Default),
            deviceResources = {
                DeviceResources(
                    deviceModel = "ST-54 JVM",
                    androidVersion = 34,
                    supportedAbis = listOf("x86_64"),
                    totalRamBytes = 8L * 1024 * 1024 * 1024,
                    availableRamBytes = 6L * 1024 * 1024 * 1024,
                    freeStorageBytes = 40L * 1024 * 1024 * 1024,
                    ttsEnginePackages = listOf("com.google.android.tts"),
                    networkOnline = true,
                    networkMetered = false,
                    thermalStatus = 0,
                    powerSaving = false,
                )
            },
            keyValueStore = InMemoryKeyValueStore(),
            reopenKeyValueStore = { InMemoryKeyValueStore() },
            reopenRegistry = { registry },
            serverFactory = { LocalTowerMockServer() },
            appVersion = "st54-test",
            commitReference = null,
            isAndroidRuntime = false,
            voiceAbQualificationProbe = probe,
        )
    }

    private fun st54(subjects: SelfTestSubjects): SelfTestResult =
        SelfTestRegistry.build(subjects)
            .first { it.id == "ST-54" }
            .check.run(
                SelfTestContext(
                    environment = "test",
                    appVersion = "st54-test",
                    commitReference = null,
                    isAndroidRuntime = false,
                ),
            )

    @Test
    fun `no probe on the JVM is an honest pending-device result`() {
        val result = st54(subjectsWith(probe = null))
        assertEquals(SelfTestState.QUALIFICATION_PENDING_DEVICE, result.state)
        assertTrue(result.evidence.contains("physical device"))
    }

    @Test
    fun `no active natural voice blocks with the Sanctum repair path`() {
        val result = st54(
            subjectsWith(
                probe = {
                    VoiceAbQualificationReadback(
                        voiceAId = null, voiceBId = null, utteranceA = null, utteranceB = null,
                        readBackAId = null, readBackBId = null,
                        identitiesDistinct = false, authorityUnchanged = true,
                        detail = "no natural voice active",
                    )
                },
            ),
        )
        assertEquals(SelfTestState.BLOCKED_DEPENDENCY, result.state)
        assertTrue(result.evidence.contains("Model Sanctum"))
    }

    @Test
    fun `a single installed voice blocks with the second-voice repair path`() {
        val result = st54(
            subjectsWith(
                probe = {
                    VoiceAbQualificationReadback(
                        voiceAId = "org.piper.voice.it-riccardo-x-low", voiceBId = null,
                        utteranceA = null, utteranceB = null,
                        readBackAId = null, readBackBId = null,
                        identitiesDistinct = false, authorityUnchanged = true,
                        detail = "no second installed voice for the comparison",
                    )
                },
            ),
        )
        assertEquals(SelfTestState.BLOCKED_DEPENDENCY, result.state)
        assertTrue(result.evidence.contains("SECOND installed voice"))
    }

    @Test
    fun `two completed distinct legs with the authority untouched pass honestly`() {
        val result = st54(
            subjectsWith(
                probe = {
                    VoiceAbQualificationReadback(
                        voiceAId = "org.piper.voice.it-riccardo-x-low",
                        voiceBId = "org.piper.voice.it-paola-medium",
                        utteranceA = "COMPLETED", utteranceB = "COMPLETED",
                        readBackAId = "vits-piper-it_IT-riccardo-x_low",
                        readBackBId = "vits-piper-it_IT-paola-medium",
                        identitiesDistinct = true, authorityUnchanged = true,
                    )
                },
            ),
        )
        assertEquals(SelfTestState.PASS, result.state)
        // the physical verdict stays unclaimed: the evidence must carry the
        // Owner-only pending token verbatim (no simulated audible pass).
        assertTrue(result.evidence.contains("VOICE_SWITCHING_OWNER_VERIFICATION_PENDING"))
    }

    @Test
    fun `a failed leg fails with the typed reason`() {
        val result = st54(
            subjectsWith(
                probe = {
                    VoiceAbQualificationReadback(
                        voiceAId = "org.piper.voice.it-riccardo-x-low",
                        voiceBId = "org.piper.voice.it-paola-medium",
                        utteranceA = "COMPLETED", utteranceB = "PLAYBACK_FAILED",
                        readBackAId = "vits-piper-it_IT-riccardo-x_low",
                        readBackBId = "vits-piper-it_IT-paola-medium",
                        identitiesDistinct = true, authorityUnchanged = true,
                        detail = "voice B leg did not complete",
                    )
                },
            ),
        )
        assertEquals(SelfTestState.FAIL, result.state)
        assertTrue(result.evidence.contains("PLAYBACK_FAILED"))
    }

    @Test
    fun `identical runtime identities on both legs is a route-truth violation`() {
        val result = st54(
            subjectsWith(
                probe = {
                    VoiceAbQualificationReadback(
                        voiceAId = "org.piper.voice.it-riccardo-x-low",
                        voiceBId = "org.piper.voice.it-paola-medium",
                        utteranceA = "COMPLETED", utteranceB = "COMPLETED",
                        readBackAId = "vits-piper-it_IT-riccardo-x_low",
                        readBackBId = "vits-piper-it_IT-riccardo-x_low",
                        identitiesDistinct = false, authorityUnchanged = true,
                    )
                },
            ),
        )
        assertEquals(SelfTestState.FAIL, result.state)
        assertTrue(result.evidence.contains("route-truth violation"))
    }

    @Test
    fun `a changed selection authority after the audition is a route-truth violation`() {
        val result = st54(
            subjectsWith(
                probe = {
                    VoiceAbQualificationReadback(
                        voiceAId = "org.piper.voice.it-riccardo-x-low",
                        voiceBId = "org.piper.voice.it-paola-medium",
                        utteranceA = "COMPLETED", utteranceB = "COMPLETED",
                        readBackAId = "vits-piper-it_IT-riccardo-x_low",
                        readBackBId = "vits-piper-it_IT-paola-medium",
                        identitiesDistinct = true, authorityUnchanged = false,
                    )
                },
            ),
        )
        assertEquals(SelfTestState.FAIL, result.state)
        assertTrue(result.evidence.contains("selection authority"))
    }
}
