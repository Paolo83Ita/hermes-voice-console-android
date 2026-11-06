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
import com.hermestower.voiceconsole.tts.SystemTtsDomainAdapter
import com.hermestower.voiceconsole.voice.AndroidMicHandleFactory
import com.hermestower.voiceconsole.voice.AndroidSystemSttEngine
import com.hermestower.voiceconsole.voice.AndroidSystemTtsEngine
import com.hermestower.voiceconsole.voice.AudioRecordFrameSource
import com.hermestower.voiceconsole.voice.EnergyVadEngine
import com.hermestower.voiceconsole.voice.InactiveFrameSttEngine
import com.hermestower.voiceconsole.voice.VoskFrameSttEngine
import com.hermestower.voiceconsole.voice.InactiveSpeechToTextEngine
import com.hermestower.voiceconsole.voice.InactiveVoiceActivityDetector
import com.hermestower.voiceconsole.voice.InactiveWakeWordEngine
import com.hermestower.voiceconsole.voice.NoAudioCaptureEngine
import com.hermestower.voiceconsole.voice.VoicePipelineController
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

    // ---- Tower Archive (GRAPH 4 CP-G4-011, Owner §14): deterministic mock —
    // the real Tower stays the authority; SIMULATED labels are mandatory. ----
    val towerArchive = com.hermestower.voiceconsole.archive.MockTowerArchive()

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

    // GRAPH 4 CP-G4-010 wave 2 (G4-C09 wave 2): QR pairing scan flow. The strict
    // hermes-pair://v1 parser (CP-G4-012a) with a session-lifetime nonce store and
    // the deterministic mock executor. Real Tower pairing stays deferred to Tower
    // Graph 1.7: nothing here contacts an endpoint — the mock client is the only
    // executor and every result it produces is labeled SIMULATED in the UI.
    val pairingNonceStore = com.hermestower.voiceconsole.pairing.InMemoryPairingNonceStore()
    val pairingPayloadParser = com.hermestower.voiceconsole.pairing.PairingPayloadParser(nonceStore = pairingNonceStore)
    val mockTowerPairingClient = com.hermestower.voiceconsole.pairing.MockTowerPairingClient()

    // GRAPH 4 CP-G4-009 (Owner §6 real model delivery): the four physically
    // verified ONNX artifacts ride INSIDE the APK (assets/models/bundled/) and
    // are delivered into the §7 roots through the same hash-gated, receipted
    // pipeline as network downloads. Delivery ≠ enablement: wake stays OFF
    // until the FA/FR harness, the denoiser stays bypassed until device
    // qualification — the runtimes below are constructed ONLY on demand.
    val bundledModelInstaller = com.hermestower.voiceconsole.models.BundledModelInstaller(
        assetSource = com.hermestower.voiceconsole.models.AndroidModelProviders.bundledAssetSource(context),
        catalog = modelCatalog,
        storage = modelStorage,
        registry = modelRegistry,
        activation = modelActivation,
    )

    /** Denoiser runtime from the REAL installed gtcrn artifact (null until delivered). */
    fun gtcrnDenoiserIfDelivered(): com.hermestower.voiceconsole.voice.GtcrnDenoiserEngine? {
        val entry = modelCatalog.find("org.gtcrn.speech-enhancement.simple") ?: return null
        val file = modelStorage.installedFile(entry) ?: return null
        val record = modelRegistry.record(entry.identity)
        if (record == null || record.state != com.hermestower.voiceconsole.models.ModelInstallState.ACTIVE.name) return null
        return com.hermestower.voiceconsole.voice.GtcrnDenoiserEngine(
            com.hermestower.voiceconsole.voice.onnx.OrtOnnxRunner(file.toString()),
        )
    }

    /** Wake chain from the REAL installed trio (null until all three delivered).
     *  Construction never enables: OpenWakeWordEngine stays OFF until enable(). */
    fun openWakeWordIfDelivered(): com.hermestower.voiceconsole.voice.OpenWakeWordEngine? {
        val ids = listOf(
            "org.openwakeword.model.melspectrogram",
            "org.openwakeword.model.embedding",
            "org.openwakeword.wakeword.hey-jarvis",
        )
        val files = ids.map { id ->
            val entry = modelCatalog.find(id) ?: return null
            val file = modelStorage.installedFile(entry) ?: return null
            val record = modelRegistry.record(id)
            if (record == null || record.state != com.hermestower.voiceconsole.models.ModelInstallState.ACTIVE.name) return null
            file
        }
        return com.hermestower.voiceconsole.voice.OpenWakeWordEngine(
            frontend = com.hermestower.voiceconsole.voice.RealMelEmbedFrontend(
                com.hermestower.voiceconsole.voice.onnx.OrtOnnxRunner(files[0].toString()),
                com.hermestower.voiceconsole.voice.onnx.OrtOnnxRunner(files[1].toString()),
            ),
            scorer = com.hermestower.voiceconsole.voice.RealHeadScorer(
                com.hermestower.voiceconsole.voice.onnx.OrtOnnxRunner(files[2].toString()),
            ),
        )
    }

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
            bundledManifestText = {
                runCatching {
                    context.assets.open("models/bundled/bundled_manifest.json").bufferedReader().use { it.readText() }
                }.getOrNull()
            },
            bundledInstaller = bundledModelInstaller,
            pairingParserFactory = {
                com.hermestower.voiceconsole.pairing.PairingPayloadParser(
                    nonceStore = com.hermestower.voiceconsole.pairing.InMemoryPairingNonceStore(),
                )
            },
            towerArchive = towerArchive,
            // OWNER PHYSICAL FEEDBACK 3 (ST-51): the live voice read-back, read
            // lazily when the probe runs (after the engine negotiated).
            italianTtsVoiceProbe = { voiceTts.voiceReadback },
        ),
        dispatcher = Dispatchers.Default,
    )

    // Wake word remains INACTIVE by default (Owner §8/§9): the bundled trio is
    // delivered by CP-G4-009 but the FA/FR harness on the Xiaomi is the gate.
    val wakeWordEngine = InactiveWakeWordEngine(phrase = "hermes")
    val voiceActivityDetector = InactiveVoiceActivityDetector()
    val audioCaptureEngine = NoAudioCaptureEngine()
    val speechToTextEngine = InactiveSpeechToTextEngine()

    val localEngine = MockLocalConversationEngine()

    // GRAPH 4.6 §6: OPTIONAL LOCAL LLM — one verified candidate (catalog identity
    // org.qwen.qwen2.5-0.5b-instruct.q5-k-m: Qwen2.5-0.5B-Instruct GGUF q5_k_m,
    // Apache-2.0, revision-bound HF source, known SHA-256 + size), DEFAULT DISABLED
    // via ConsoleSettings.localLlmEnabled. The runtime seam is NoVerifiedLlmRuntime:
    // generation stays typed-blocked (LLM_RUNTIME_PENDING_VERIFIED_SOURCE) until a
    // verified Android llama.cpp runtime is adopted — the engine never invents text.
    val localLlmEngine = run {
        val qwen = modelCatalog.all().first { it.identity == "org.qwen.qwen2.5-0.5b-instruct.q5-k-m" }
        com.hermestower.voiceconsole.local.LocalLlmEngine(
            modelRoot = java.io.File(context.filesDir, "models/llm/qwen25-05b"),
            expectedSha256 = qwen.sha256,
            expectedSizeBytes = qwen.downloadSizeBytes,
            enabledProvider = { store.settings.value.localLlmEnabled },
        )
    }

    // ---- GRAPH 4 G4-C02/C03: REAL voice path (Owner delta §5) ----
    // Microphone permission is checked physically before any hardware access.
    val micPermissionGranted: () -> Boolean = {
        androidx.core.content.ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.RECORD_AUDIO,
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
    }
    val micHandleFactory = AndroidMicHandleFactory(hasPermission = micPermissionGranted)

    // Real Android system TTS: the console SPEAKS with real audio (tier 2 of
    // the §10 fallback chain; the natural Piper voice is G4-C07 after
    // qualification). No TTS engine on device → honest text-only degradation.
    // OWNER PHYSICAL FEEDBACK 3: the spoken locale follows the app language
    // (AppLanguage), the negotiation is read-back-verified and typed — the
    // English-voice silent fallback class is dead (ItalianTtsVoiceNegotiator).
    val voiceTts = AndroidSystemTtsEngine(context) {
        com.hermestower.voiceconsole.locale.AppLanguage.effectiveTag(context)
    }
    val consoleTts: com.hermestower.voiceconsole.domain.TextToSpeechEngine =
        SystemTtsDomainAdapter(voiceTts)

    // Real system speech recognizer (STT while the offline frame path is
    // pending G4-C07 runtime delivery). One owner of the microphone at a time:
    // the system recognizer captures by itself, AudioRecord stays closed.
    val systemStt = AndroidSystemSttEngine(context)

    // GRAPH 4 FINAL §14: the REAL offline frame STT — loads the verified
    // vosk-model-small-it-0.22 when it is installed (§12/§13 delivery chain);
    // until then it reports NOT_INSTALLED and the ACTIVE engine identity stays
    // the Android system recognizer (never a false local-STT claim).
    val voskFrameStt = VoskFrameSttEngine(
        modelRoot = java.io.File(context.filesDir, "models/stt/vosk-small-it"),
    )

    // Frame-based pipeline (G4-C02) kept wired for the offline STT path and
    // the Self-Test Center; its Tower executor is honestly unavailable until
    // the TowerGraph 1.7 contract exists (no invented endpoints).
    val router = RuleBasedIntentRouter()
    val voiceController = VoicePipelineController(
        sourceFactory = { AudioRecordFrameSource(micHandleFactory, permissionGranted = micPermissionGranted) },
        vad = EnergyVadEngine(),
        stt = voskFrameStt,
        tts = voiceTts,
        router = router,
        modeProvider = { store.requestedMode.value },
        connectivityProvider = {
            when (store.connection.value.status) {
                com.hermestower.voiceconsole.core.state.ConnectionStatus.BridgeStatus.CONNECTED ->
                    com.hermestower.voiceconsole.core.routing.Connectivity.OK
                com.hermestower.voiceconsole.core.state.ConnectionStatus.BridgeStatus.CONNECTING ->
                    com.hermestower.voiceconsole.core.routing.Connectivity.DEGRADED
                else -> com.hermestower.voiceconsole.core.routing.Connectivity.DISCONNECTED
            }
        },
        localExecutor = { text -> localEngine.respond(text) { "" }.text },
        towerExecutor = { _ -> null },
    )

    val simulator = DeterministicTowerSimulator()
    val bridge = MockTowerBridge(simulator)

    // GRAPH 4 FINAL §13: ACTIVE-RUNTIME READ-BACK — the safe Diagnostics view
    // consumes this snapshot; every value is read from the LIVE engine objects
    // (no invented identities, no aspiration shown as fact).
    val activeRuntimeReadback: ActiveRuntimeReadback
        get() {
            val readback = voiceTts.voiceReadback
            return ActiveRuntimeReadback(
                sttActiveEngine = if (voskFrameStt.loadedModelDir != null) voskFrameStt.engineId
                else "Android system recognizer (offline vosk model not installed)",
                sttFrameState = voskFrameStt.state.value.state.name +
                    (voskFrameStt.loadedModelDir?.let { " (${it.name})" } ?: ""),
                ttsActiveEngine = voiceTts.activeEngineName?.let { "Android system TTS ($it)" }
                    ?: "Android system TTS",
                ttsVoiceReadback = readback?.let {
                    com.hermestower.voiceconsole.voice.ItalianTtsVoiceNegotiator.describe(it)
                },
                ttsVoiceLocale = readback?.activeVoiceLocaleTag
                    ?: readback?.activeLanguageTag,
                llmState = "No local model (default) — Qwen2.5-0.5B candidate present, llama.cpp runtime pending",
                onnxRuntime = "onnxruntime-android 1.20.0 (Gradle)",
            )
        }

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
        tts = consoleTts,
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

    // G4-C03: real microphone path coordinator (single state authority, single
    // routing/history path — voice and text share everything).
    val voiceCoordinator = ConsoleVoiceCoordinator(
        scope = scope,
        store = store,
        driver = driver,
        systemStt = systemStt,
        hasMicPermission = micPermissionGranted,
        keyValueStore = keyValueStore,
        events = events,
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
        // Préchauffe: system TTS init is async — start it at boot so the first
        // answer can really speak (no fake SPEAKING while not ready).
        voiceTts.load()
        // INV-G4-10: privacy mute is a PHYSICAL gate on every capture path.
        scope.launch {
            store.privacyMuted.collect { voiceController.setMuted(it) }
        }
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

/** GRAPH 4 FINAL §13 — active-runtime snapshot consumed by safe Diagnostics.
 *  OWNER PHYSICAL FEEDBACK 3 adds the TTS voice read-back rows (active voice,
 *  locale, typed status) so the device itself answers the root-cause questions. */
data class ActiveRuntimeReadback(
    val sttActiveEngine: String,
    val sttFrameState: String,
    val ttsActiveEngine: String,
    val ttsVoiceReadback: String? = null,
    val ttsVoiceLocale: String? = null,
    val llmState: String,
    val onnxRuntime: String,
)
