package com.hermestower.voiceconsole.selftest

import com.hermestower.voiceconsole.models.DefaultModelDownloadManager
import com.hermestower.voiceconsole.models.DefaultModelQualificationRunner
import com.hermestower.voiceconsole.models.DefaultModelStorageManager
import com.hermestower.voiceconsole.models.DefaultVoiceModelCatalog
import com.hermestower.voiceconsole.models.DefaultVoicePackResolver
import com.hermestower.voiceconsole.models.DeviceResources
import com.hermestower.voiceconsole.models.FileInstallationRegistry
import com.hermestower.voiceconsole.models.ModelActivationController
import com.hermestower.voiceconsole.models.ModelRollbackController
import com.hermestower.voiceconsole.migration.InMemoryKeyValueStore
import com.hermestower.voiceconsole.tower.FileTowerProfileStore
import com.hermestower.voiceconsole.tower.InMemorySecretVault
import com.hermestower.voiceconsole.tower.RealTowerBridge
import com.hermestower.voiceconsole.towersim.LocalTowerMockServer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.nio.file.Files
import java.util.concurrent.Executors

/**
 * Executes the actual in-app Self-Test battery on the JVM (ST-01..ST-45) against
 * the deterministic subjects. This is the executable proof that the Self-Test
 * Center itself works before the emulator run (GRAPH 2 GATE-05).
 */
class SelfTestCenterTest {

    private lateinit var dir: java.nio.file.Path
    private lateinit var center: SelfTestCenter
    private lateinit var dispatcher: kotlinx.coroutines.CoroutineDispatcher
    private val executor = Executors.newSingleThreadExecutor()

    @Before
    fun setUp() {
        dir = Files.createTempDirectory("hermes-selftest")
        val registryFile = dir.resolve("registry.json")
        val storage = DefaultModelStorageManager(dir.resolve("models"))
        val registry = FileInstallationRegistry(registryFile)
        val profileStore = FileTowerProfileStore(dir.resolve("tower_profile.json"), InMemorySecretVault())
        val vault = InMemorySecretVault()
        val keyValue = InMemoryKeyValueStore()
        val catalog = DefaultVoiceModelCatalog.fromText(catalogJson())
        dispatcher = executor.asCoroutineDispatcher()
        center = SelfTestCenter(
            subjects = SelfTestSubjects(
                catalog = catalog,
                resolver = DefaultVoicePackResolver(),
                storage = storage,
                registry = registry,
                downloads = DefaultModelDownloadManager(storage, registry, Dispatchers.Default),
                qualificationRunner = DefaultModelQualificationRunner(storage),
                activation = ModelActivationController(registry, DefaultModelQualificationRunner(storage)),
                rollback = ModelRollbackController(storage, registry),
                profileStore = profileStore,
                vault = vault,
                realBridge = RealTowerBridge(profileStore, Dispatchers.Default),
                deviceResources = {
                    DeviceResources(
                        deviceModel = "SelfTest JVM",
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
                keyValueStore = keyValue,
                reopenKeyValueStore = { keyValue },
                reopenRegistry = { FileInstallationRegistry(registryFile) },
                serverFactory = { LocalTowerMockServer() },
                appVersion = "0.2.0-debug",
                commitReference = "self-test",
                isAndroidRuntime = false,
            ),
            dispatcher = dispatcher,
        )
    }

    @After
    fun tearDown() {
        center.stop()
        executor.shutdownNow()
        dir.toFile().deleteRecursively()
    }

    private fun awaitCompletion(timeoutMs: Long = 180_000) {
        runBlocking {
            withTimeout(timeoutMs) {
                while (center.running.value) kotlinx.coroutines.delay(50)
            }
        }
    }

    @Test
    fun `quick mode runs its subset and every result is pass`() {
        center.run(SelfTestMode.QUICK)
        awaitCompletion()
        val results = center.results.value
        assertEquals(45, results.size)
        val quick = results.values.filter { it.state != SelfTestState.NOT_RUN }
        assertTrue("quick subset empty", quick.isNotEmpty())
        val failures = quick.filter { it.state != SelfTestState.PASS }
        assertTrue("non-pass quick results: ${failures.map { it.testId to it.evidence }}", failures.isEmpty())
        assertTrue(center.summary().contains("20 pass"))
    }

    @Test
    fun `full mode runs all 45 definitions and every one passes on the JVM`() {
        center.run(SelfTestMode.FULL)
        awaitCompletion()
        val results = center.results.value
        assertEquals(45, results.size)
        assertTrue(results.keys.containsAll((1..45).map { "ST-%02d".format(it) }))
        val failures = results.values.filter { it.state != SelfTestState.PASS }
        assertTrue("non-pass results: ${failures.map { it.testId to it.evidence }}", failures.isEmpty())
    }

    @Test
    fun `results carry the required typed fields`() {
        center.run(SelfTestMode.QUICK)
        awaitCompletion()
        val st01 = center.results.value["ST-01"]!!
        assertTrue(st01.testName.isNotBlank())
        assertTrue(st01.startTime != null && st01.finishTime != null)
        assertTrue(st01.durationMs >= 0)
        assertTrue(st01.evidence.isNotBlank())
        assertEquals("0.2.0-debug", st01.appVersion)
        assertEquals("self-test", st01.commitReference)
    }

    @Test
    fun `stop resets running entries and never leaves RUNNING state`() {
        Thread { center.run(SelfTestMode.FULL) }.start()
        awaitFirstRunning()
        center.stop()
        assertFalse(center.running.value)
        assertTrue(center.results.value.values.none { it.state == SelfTestState.RUNNING })
        center.stop() // idempotent
        assertFalse(center.running.value)
    }

    @Test
    fun `export is secret clean and contains typed ids`() {
        center.run(SelfTestMode.QUICK)
        awaitCompletion()
        val report = center.exportSecretCleanReport()
        assertTrue(report.contains("ST-01"))
        assertTrue(report.contains("secret-clean"))
        assertFalse(report.contains("st-secret"))
    }

    @Test
    fun `reset history clears results and retry of empty history is a no-op`() {
        center.run(SelfTestMode.QUICK)
        awaitCompletion()
        center.resetHistory()
        assertTrue(center.results.value.isEmpty())
        center.retryFailed()
        assertFalse(center.running.value)
    }

    private fun awaitFirstRunning() {
        runBlocking {
            withTimeout(30_000) {
                while (center.results.value.values.none { it.state == SelfTestState.RUNNING }) kotlinx.coroutines.delay(20)
            }
        }
    }

    private fun catalogJson(): String = javaClass.classLoader!!
        .getResourceAsStream("voice_pack_catalog_test.json")!!
        .bufferedReader().readText()
}
