package com.hermestower.voiceconsole.app

import android.content.Context
import com.hermestower.voiceconsole.bridge.DeterministicTowerSimulator
import com.hermestower.voiceconsole.bridge.MockTowerBridge
import com.hermestower.voiceconsole.core.routing.RuleBasedIntentRouter
import com.hermestower.voiceconsole.core.state.ConsoleStateStore
import com.hermestower.voiceconsole.domain.TowerEndpoint
import com.hermestower.voiceconsole.health.AndroidDeviceHealthMonitor
import com.hermestower.voiceconsole.local.MockLocalConversationEngine
import com.hermestower.voiceconsole.migration.SettingsMigrator
import com.hermestower.voiceconsole.migration.SharedPreferencesKeyValueStore
import com.hermestower.voiceconsole.models.AndroidDeviceResourceReader
import com.hermestower.voiceconsole.models.AndroidModelProviders
import com.hermestower.voiceconsole.models.DefaultModelActivationController
import com.hermestower.voiceconsole.models.DefaultModelDownloadManager
import com.hermestower.voiceconsole.models.DefaultModelQualificationRunner
import com.hermestower.voiceconsole.models.DefaultModelRollbackController
import com.hermestower.voiceconsole.models.DefaultVoicePackInstaller
import com.hermestower.voiceconsole.models.DefaultVoicePackResolver
import com.hermestower.voiceconsole.privacy.LogRedactor
import com.hermestower.voiceconsole.selftest.SelfTestCenter
import com.hermestower.voiceconsole.selftest.SelfTestSubjects
import com.hermestower.voiceconsole.simulator.ScriptedConsoleDriver
import com.hermestower.voiceconsole.store.DefaultSessionController
import com.hermestower.voiceconsole.store.InMemoryEventStore
import com.hermestower.voiceconsole.store.InMemorySecureConfigurationStore
import com.hermestower.voiceconsole.store.RedactedDiagnosticsExporter
import com.hermestower.voiceconsole.store.SettingsRepository
import com.hermestower.voiceconsole.tower.FileTowerProfileStore
import com.hermestower.voiceconsole.tower.KeystoreVault
import com.hermestower.voiceconsole.tower.PrivateFileVaultStore
import com.hermestower.voiceconsole.tower.RealTowerBridge
import com.hermestower.voiceconsole.towersim.LocalTowerMockServer
import com.hermestower.voiceconsole.tts.MockTextToSpeechEngine
import com.hermestower.voiceconsole.voice.InactiveSpeechToTextEngine
import com.hermestower.voiceconsole.voice.InactiveVoiceActivityDetector
import com.hermestower.voiceconsole.voice.InactiveWakeWordEngine
import com.hermestower.voiceconsole.voice.NoAudioCaptureEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.File
import java.time.Clock

/**
 * Service locator (decision D-003). One place wires every seam; swapping a
 * provider later means changing a line here, not the UI or the pipeline.
 * GRAPH 2 adds: Voice Model Manager, Tower Connection Studio stores, RealTowerBridge
 * adapter, Keystore vault, V0.2 migration, Self-Test Center. The console pipeline
 * remains mock-first until a real Tower endpoint is configured by the Owner.
 */
class AppGraph(context: Context) {

    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    val clock: Clock = Clock.systemDefaultZone()

    // ---- migration (V0.1 → V0.2, idempotent, runs before settings restore) ----
    val keyValueStore = SharedPreferencesKeyValueStore(context)
    val migrationReport = SettingsMigrator.migrate(keyValueStore)

    val settingsRepository = SettingsRepository(context)
    val store = ConsoleStateStore()
    val health = AndroidDeviceHealthMonitor(context)
    val events = InMemoryEventStore()
    val session = DefaultSessionController()
    val secureStore = InMemorySecureConfigurationStore()

    // ---- Voice Model Manager (GRAPH 2 §4–6) ----
    val deviceResourceReader = AndroidDeviceResourceReader(context)
    val modelCatalog = AndroidModelProviders.loadCatalog(context)
    val modelStorage = AndroidModelProviders.storageManager(context)
    val modelRegistry = AndroidModelProviders.registry(context)
    val modelDownloads = DefaultModelDownloadManager(modelStorage, modelRegistry)
    val modelQualification = DefaultModelQualificationRunner(modelStorage)
    val modelActivation = DefaultModelActivationController(modelRegistry, modelQualification)
    val modelRollback = DefaultModelRollbackController(modelStorage, modelRegistry)
    val modelResolver = DefaultVoicePackResolver()

    // GRAPH 4 NODE 2 (§6/§7): MODEL SANCTUM — recommended-pack planner, preflight
    // summary and receipted sequential installer. Optional components (LLM,
    // alternatives, denoiser) are never part of the pack plan.
    val modelPackInstaller = DefaultVoicePackInstaller(
        downloads = modelDownloads,
        qualification = modelQualification,
        activation = modelActivation,
        registry = modelRegistry,
        storage = modelStorage,
    )

    // ---- Tower Connection Studio (GRAPH 2 §7–10) ----
    val vault = KeystoreVault(PrivateFileVaultStore(File(context.filesDir, "tower_vault.bin")))
    val profileStore = FileTowerProfileStore(
        File(context.filesDir, "tower_profile.json").toPath(),
        vault,
    )
    val realBridge = RealTowerBridge(profileStore)

    // ---- Self-Test Center (GRAPH 2 §11) ----
    val selfTestCenter = SelfTestCenter(
        subjects = SelfTestSubjects(
            catalog = modelCatalog,
            resolver = modelResolver,
            storage = modelStorage,
            registry = modelRegistry,
            downloads = modelDownloads,
            qualificationRunner = modelQualification,
            activation = modelActivation,
            rollback = modelRollback,
            profileStore = profileStore,
            vault = vault,
            realBridge = realBridge,
            deviceResources = { deviceResourceReader.read() },
            keyValueStore = keyValueStore,
            reopenKeyValueStore = { SharedPreferencesKeyValueStore(context) },
            reopenRegistry = { AndroidModelProviders.registry(context) },
            serverFactory = { LocalTowerMockServer() },
            appVersion = APP_VERSION,
            commitReference = null,
            isAndroidRuntime = true,
        ),
        dispatcher = Dispatchers.Default,
    )

    val wakeWordEngine = InactiveWakeWordEngine(phrase = "hermes")
    val voiceActivityDetector = InactiveVoiceActivityDetector()
    val audioCaptureEngine = NoAudioCaptureEngine()
    val speechToTextEngine = InactiveSpeechToTextEngine()

    val localEngine = MockLocalConversationEngine()
    val tts = MockTextToSpeechEngine()
    val simulator = DeterministicTowerSimulator()
    val bridge = MockTowerBridge(simulator)
    val router = RuleBasedIntentRouter()

    val diagnostics = RedactedDiagnosticsExporter(
        events = events,
        health = health,
        appVersion = APP_VERSION,
    )

    val driver = ScriptedConsoleDriver(
        scope = scope,
        store = store,
        router = router,
        localEngine = localEngine,
        bridge = bridge,
        tts = tts,
        events = events,
        session = session,
        statusProvider = {
            buildString {
                val time = java.time.LocalTime.now(clock).withSecond(0)
                append("${time.hour.format2()}:${time.minute.format2()}, ")
                append(health.statusSummary())
            }
        },
    )

    fun start() {
        // Restore persisted settings into the store, then keep persisting on change.
        store.applySettings(settingsRepository.load())
        scope.launch {
            store.settings.collect { settingsRepository.persist(it) }
        }
        store.setConnection(
            com.hermestower.voiceconsole.core.state.ConnectionStatus(
                status = com.hermestower.voiceconsole.core.state.ConnectionStatus.BridgeStatus.UNCONFIGURED,
                endpointLabel = TowerEndpoint.MOCK.displayLabel,
                tls = com.hermestower.voiceconsole.core.state.ConnectionStatus.TlsStatus.MOCK_NOT_APPLICABLE,
            ),
        )
        bridge.configure(TowerEndpoint.MOCK)
        driver.start()
        driver.boot()
        events.log(
            "APP",
            "graph started (mock bridge; v$APP_VERSION; migration→v${migrationReport.toSchema} fresh=${migrationReport.freshInstall}; " +
                "redaction ${LogRedactor.redact("token: sample-value")})",
        )
    }

    private fun Int.format2(): String = toString().padStart(2, '0')

    companion object {
        const val APP_VERSION = "0.2.0-debug"
    }
}
