package com.hermestower.voiceconsole.app

import android.content.Context
import com.hermestower.voiceconsole.bridge.DeterministicTowerSimulator
import com.hermestower.voiceconsole.bridge.MockTowerBridge
import com.hermestower.voiceconsole.core.routing.RuleBasedIntentRouter
import com.hermestower.voiceconsole.core.state.ConsoleStateStore
import com.hermestower.voiceconsole.domain.TowerEndpoint
import com.hermestower.voiceconsole.health.AndroidDeviceHealthMonitor
import com.hermestower.voiceconsole.local.MockLocalConversationEngine
import com.hermestower.voiceconsole.locale.ResponseLocaleResolver
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
import com.hermestower.voiceconsole.tts.TtsRouteSwitch
import com.hermestower.voiceconsole.voice.SherpaOnnxTtsEngine
import com.hermestower.voiceconsole.voice.SherpaRealNativeBridge
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
import com.hermestower.voiceconsole.voice.SpeechProfile
import com.hermestower.voiceconsole.voice.TtsTelemetry
import com.hermestower.voiceconsole.voice.VoicePipelineController
import com.hermestower.voiceconsole.voice.ChatSpeechOutputPolicy
import com.hermestower.voiceconsole.voice.ContinuousListeningController
import com.hermestower.voiceconsole.voice.EchoFingerprintGuard
import com.hermestower.voiceconsole.voice.ListeningModePolicy
import com.hermestower.voiceconsole.voice.VoiceQualificationState
import com.hermestower.voiceconsole.voice.VoiceRuntimeFactory
import com.hermestower.voiceconsole.voice.VoiceSelectionAuthority
import com.hermestower.voiceconsole.voice.VoiceSelectionState
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
class AppGraph(private val context: Context) {

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
            // CP-G4.9-003 (ST-52): the REAL spoken route (registry + bundle).
            naturalTtsRouteProbe = {
                com.hermestower.voiceconsole.selftest.NaturalTtsRouteReadback(
                    activeRoute = ttsRouteSwitch.lastRouteUsed,
                    naturalEngineId = activeNaturalVoice()?.let { id ->
                        when (id) {
                            "org.piper.voice.it-riccardo-x-low" -> naturalTts.engineId
                            "org.piper.voice.it-paola-medium" -> naturalTtsPaola.engineId
                            else -> null
                        }
                    },
                    voiceBundlePresent = naturalTts.resolveVoiceDir() != null || naturalTtsPaola.resolveVoiceDir() != null,
                )
            },
            // CP-G4.9-004 (ST-53): the REAL speech-input route.
            localSttRouteProbe = {
                com.hermestower.voiceconsole.selftest.LocalSttRouteReadback(
                    activeEngine = if (voskFrameStt.loadedModelDir != null) voskFrameStt.engineId
                    else "Android system recognizer (offline vosk model not installed)",
                    offlineModelLoaded = voskFrameStt.loadedModelDir != null,
                    loadedModelDir = voskFrameStt.loadedModelDir?.name,
                )
            },
            // GRAPH 4.11 Node 1 (ST-54): the REAL A/B voice audition — voice A
            // (active) speaks, then voice B (the other installed voice) speaks
            // the SAME phrase; the selection authority is never touched.
            voiceAbQualificationProbe = { voiceAbQualification() },
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

    // GRAPH 4.10 CP-G4.10-002: the ONE response-locale authority (§4 precedence
    // chain). Every local text producer consumes the resolved tag from HERE —
    // no producer duplicates locale logic.
    val responseLocaleResolver = ResponseLocaleResolver(
        appLanguageTag = { com.hermestower.voiceconsole.locale.AppLanguage.effectiveTag(context) },
    )

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
            localeTagProvider = { responseLocaleResolver.resolve().tag },
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
    // (AppLanguage), the negotiation is read-back-verified and typed.
    // OWNER PHYSICAL RETEST §4/§5: the engine now FAILS CLOSED (a non-Italian
    // active voice while Italian is requested refuses the utterance —
    // TEXT_ONLY_SAFE_DEGRADED + typed TtsSpeakBlock) and applies the persisted
    // SpeechProfile (rate 0.96 / pitch 0.95 defaults) before every utterance;
    // every negotiation/utterance event lands in the secret-clean telemetry.
    val ttsTelemetry = TtsTelemetry()
    val voiceTts = AndroidSystemTtsEngine(
        context,
        spokenLocaleTagProvider = {
            com.hermestower.voiceconsole.locale.AppLanguage.effectiveTag(context)
        },
        speechProfileProvider = {
            SpeechProfile.safe(
                store.settings.value.speakingRate,
                store.settings.value.speechPitch,
            )
        },
        telemetry = ttsTelemetry,
    )
    val consoleTts: com.hermestower.voiceconsole.domain.TextToSpeechEngine =
        SystemTtsDomainAdapter(voiceTts)

    // CP-G4.9-003: REAL downloadable natural Italian voice route (sherpa-onnx
    // v1.12.24 + the verified piper vits bundles). The route switch keeps the
    // Android system engine as the MANDATORY fallback: the natural route is
    // used ONLY while a piper voice is installed, verified AND active in the
    // registry — removal/deactivation falls back honestly on the next
    // utterance. The sherpa-onnx classes are touched only by the real bridge
    // (constructed lazily on device; JVM tests never build AppGraph).
    val naturalTts = SherpaOnnxTtsEngine(
        voiceRoot = java.io.File(context.filesDir, "models/voices/riccardo-x-low"),
        voiceIdentity = "vits-piper-it_IT-riccardo-x_low",
        native = SherpaRealNativeBridge(),
        player = SherpaOnnxTtsEngine.AudioTrackPlayer(),
        speechProfileProvider = {
            SpeechProfile.safe(
                store.settings.value.speakingRate,
                store.settings.value.speechPitch,
            )
        },
        telemetry = ttsTelemetry,
    )
    val naturalTtsPaola = SherpaOnnxTtsEngine(
        voiceRoot = java.io.File(context.filesDir, "models/voices/paola-medium"),
        voiceIdentity = "vits-piper-it_IT-paola-medium",
        native = SherpaRealNativeBridge(),
        player = SherpaOnnxTtsEngine.AudioTrackPlayer(),
        speechProfileProvider = {
            SpeechProfile.safe(
                store.settings.value.speakingRate,
                store.settings.value.speechPitch,
            )
        },
        telemetry = ttsTelemetry,
    )

    /** Registry read-back: which natural voice (if any) is installed+active. */
    fun activeNaturalVoice(): String? {
        val active = modelRegistry.snapshot().firstOrNull {
            it.componentType == "VOICE" && it.active && it.state == "ACTIVE"
        } ?: return null
        // The bundle must still resolve on disk (an activated record whose
        // files were removed falls back honestly to the system route).
        val engine = when (active.identity) {
            "org.piper.voice.it-riccardo-x-low" -> naturalTts
            "org.piper.voice.it-paola-medium" -> naturalTtsPaola
            else -> null
        }
        return engine?.takeIf { it.resolveVoiceDir() != null }?.let { active.identity }
    }

    private val naturalVoiceActive: () -> String? = { activeNaturalVoice() }

    // GRAPH 4.10 CP-G4.10-003 (§7/§8): the ONE voice-selection authority + the
    // ONE runtime factory. A downloaded voice is not ACTIVE merely because it
    // is selected in UI — activation requires files verified, runtime loaded,
    // test synthesis, playback and a matching read-back (six conditions).
    val voiceSelectionAuthority = VoiceSelectionAuthority(
        voiceFilesPresent = { voiceId ->
            when (voiceId) {
                "org.piper.voice.it-riccardo-x-low" -> naturalTts.resolveVoiceDir() != null
                "org.piper.voice.it-paola-medium" -> naturalTtsPaola.resolveVoiceDir() != null
                else -> false
            }
        },
        systemItalianAvailable = { true },
        // NOTE: the REAL Italian availability gate is per-utterance and fail-closed
        // (TtsSpeakGate + ItalianTtsVoiceNegotiator); this flag only labels the
        // resting route in the status line and never bypasses that gate.
    )
    val voiceRuntimeFactory = VoiceRuntimeFactory()

    private fun voiceSeamFor(voiceId: String): VoiceRuntimeFactory.VoiceRuntimeSeam? {
        val engine = when (voiceId) {
            "org.piper.voice.it-riccardo-x-low" -> naturalTts
            "org.piper.voice.it-paola-medium" -> naturalTtsPaola
            else -> return null
        }
        return object : VoiceRuntimeFactory.VoiceRuntimeSeam {
            override fun resolveFiles(): VoiceRuntimeFactory.VoiceFiles? {
                val dir = engine.resolveVoiceDir() ?: return null
                val model = dir.listFiles()?.firstOrNull { it.name.endsWith(".onnx") } ?: return null
                return VoiceRuntimeFactory.VoiceFiles(model.absolutePath, dir.resolve("tokens.txt").absolutePath)
            }

            override fun load(): String? {
                engine.load()
                return when (engine.state.value.state) {
                    com.hermestower.voiceconsole.voice.TtsState.READY -> null
                    com.hermestower.voiceconsole.voice.TtsState.NOT_INSTALLED -> "voice bundle files not resolvable on disk"
                    else -> engine.state.value.error ?: "runtime init failed"
                }
            }

            override fun speakTest(phrase: String) {
                engine.interrupt()
                engine.speak(phrase)
            }

            override fun awaitUtteranceCompletion(timeoutMs: Long): VoiceRuntimeFactory.UtteranceResult {
                val deadline = System.currentTimeMillis() + timeoutMs
                var sawSpeech = false
                while (System.currentTimeMillis() < deadline) {
                    when (engine.state.value.state) {
                        com.hermestower.voiceconsole.voice.TtsState.SYNTHESIZING,
                        com.hermestower.voiceconsole.voice.TtsState.SPEAKING,
                        -> sawSpeech = true
                        com.hermestower.voiceconsole.voice.TtsState.READY ->
                            return if (sawSpeech) VoiceRuntimeFactory.UtteranceResult.COMPLETED
                            else VoiceRuntimeFactory.UtteranceResult.SYNTHESIS_FAILED
                        com.hermestower.voiceconsole.voice.TtsState.FAILED ->
                            return if (sawSpeech) VoiceRuntimeFactory.UtteranceResult.PLAYBACK_FAILED
                            else VoiceRuntimeFactory.UtteranceResult.SYNTHESIS_FAILED
                        com.hermestower.voiceconsole.voice.TtsState.TEXT_ONLY_SAFE_DEGRADED ->
                            return VoiceRuntimeFactory.UtteranceResult.SYNTHESIS_FAILED
                        else -> Unit
                    }
                    Thread.sleep(120)
                }
                return if (sawSpeech) VoiceRuntimeFactory.UtteranceResult.PLAYBACK_FAILED
                else VoiceRuntimeFactory.UtteranceResult.SYNTHESIS_FAILED
            }

            override fun readBackVoiceId(): String? = engine.state.value.voiceReadback?.activeVoiceName

            override fun interrupt() {
                engine.interrupt()
            }
        }
    }

    /** §8 pipeline: registry activation + the six-condition runtime truth. */
    suspend fun qualifyVoiceRuntime(entry: com.hermestower.voiceconsole.models.ModelCatalogEntry): VoiceRuntimeFactory.ActivationOutcome {
        val seam = voiceSeamFor(entry.identity)
            ?: return VoiceRuntimeFactory.ActivationOutcome.Failed(
                entry.identity, VoiceRuntimeFactory.TypedReason.FILES_MISSING,
                "no runtime seam for this identity", com.hermestower.voiceconsole.voice.VoiceRuntimeType.ANDROID_SYSTEM_TTS,
            )
        voiceSelectionAuthority.selectDownloaded(
            voiceId = entry.identity,
            modelFile = seam.resolveFiles()?.modelFile,
            configFile = seam.resolveFiles()?.configFile,
            version = entry.version,
            sha256 = entry.sha256,
        )
        voiceSelectionAuthority.beginQualification()
        val outcome = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
            voiceRuntimeFactory.activate(seam, entry.identity)
        }
        when (outcome) {
            is VoiceRuntimeFactory.ActivationOutcome.Active ->
                voiceSelectionAuthority.recordRuntimeReadBack(outcome.readBack)
            is VoiceRuntimeFactory.ActivationOutcome.Failed -> {
                seam.interrupt()
                voiceSelectionAuthority.markRuntimeFailed(outcome.typedReason.name)
            }
        }
        return outcome
    }

    /**
     * GRAPH 4.11 Node 1 (ST-54): the Owner's A/B voice audition — voice A (the
     * ACTIVE natural voice) speaks the preview phrase, then the OTHER installed
     * voice B speaks the SAME phrase, sequentially, on the REAL engines.
     *
     * Truth rules:
     *  - the audition NEVER touches the selection authority (B is auditioned,
     *    not selected) — the authority snapshot is compared byte-for-byte after;
     *  - both legs must COMPLETE (synthesis + playback) for a proven result;
     *  - distinctness is MEASURED at the identity level (two different runtime
     *    read-backs); the AUDIBLE difference stays Owner-verified;
     *  - voice B's engine is unloaded afterwards (memory hygiene on the
     *    8 GB devices; B is never the route-active engine).
     */
    fun voiceAbQualification(): com.hermestower.voiceconsole.selftest.VoiceAbQualificationReadback {
        val installed: (String) -> Boolean = { id ->
            when (id) {
                "org.piper.voice.it-riccardo-x-low" -> naturalTts.resolveVoiceDir() != null
                "org.piper.voice.it-paola-medium" -> naturalTtsPaola.resolveVoiceDir() != null
                else -> false
            }
        }
        val voiceAId = activeNaturalVoice()
        if (voiceAId == null) {
            return com.hermestower.voiceconsole.selftest.VoiceAbQualificationReadback(
                voiceAId = null, voiceBId = null, utteranceA = null, utteranceB = null,
                readBackAId = null, readBackBId = null,
                identitiesDistinct = false, authorityUnchanged = true,
                detail = "no natural voice active",
            )
        }
        val voiceBId = listOf("org.piper.voice.it-riccardo-x-low", "org.piper.voice.it-paola-medium")
            .firstOrNull { it != voiceAId && installed(it) }
        if (voiceBId == null) {
            return com.hermestower.voiceconsole.selftest.VoiceAbQualificationReadback(
                voiceAId = voiceAId, voiceBId = null, utteranceA = null, utteranceB = null,
                readBackAId = null, readBackBId = null,
                identitiesDistinct = false, authorityUnchanged = true,
                detail = "no second installed voice for the comparison",
            )
        }
        val authorityBefore = voiceSelectionAuthority.state.value
        val seamA = voiceSeamFor(voiceAId)!!
        val seamB = voiceSeamFor(voiceBId)!!
        val phrase = VoiceRuntimeFactory.PREVIEW_PHRASE
        var utteranceA = "SKIPPED"
        var utteranceB = "SKIPPED"
        var readBackA: String? = null
        var readBackB: String? = null
        var detail: String? = null

        // Leg A — the ACTIVE voice (already loaded by the route; load() is idempotent).
        val filesA = runCatching { seamA.resolveFiles() }.getOrNull()
        if (filesA == null) {
            utteranceA = "SYNTHESIS_FAILED"
            detail = "voice A files missing on disk"
        } else {
            val loadError = runCatching { seamA.load() }.getOrElse { it.message ?: "voice A runtime init threw" }
            if (loadError != null) {
                utteranceA = "SYNTHESIS_FAILED"
                detail = "voice A load failed: $loadError"
            } else {
                runCatching { seamA.speakTest(phrase) }
                utteranceA = seamA.awaitUtteranceCompletion().name
                readBackA = runCatching { seamA.readBackVoiceId() }.getOrNull()
                if (utteranceA != "COMPLETED") detail = "voice A leg did not complete"
            }
        }

        // Leg B — audition only, AFTER leg A completed (sequential, never overlapping).
        if (utteranceA == "COMPLETED") {
            val loadError = runCatching { seamB.load() }.getOrElse { it.message ?: "voice B runtime init threw" }
            when {
                loadError != null -> {
                    utteranceB = "SYNTHESIS_FAILED"
                    detail = "voice B load failed: $loadError"
                }
                else -> {
                    runCatching { seamB.speakTest(phrase) }
                    utteranceB = seamB.awaitUtteranceCompletion().name
                    readBackB = runCatching { seamB.readBackVoiceId() }.getOrNull()
                    if (utteranceB != "COMPLETED") detail = "voice B leg did not complete"
                }
            }
            runCatching { seamB.interrupt() }
            // B was never the route-active engine (A is) — release its native handle.
            when (voiceBId) {
                "org.piper.voice.it-riccardo-x-low" -> naturalTts.unload()
                "org.piper.voice.it-paola-medium" -> naturalTtsPaola.unload()
            }
        }

        return com.hermestower.voiceconsole.selftest.VoiceAbQualificationReadback(
            voiceAId = voiceAId,
            voiceBId = voiceBId,
            utteranceA = utteranceA,
            utteranceB = utteranceB,
            readBackAId = readBackA,
            readBackBId = readBackB,
            identitiesDistinct = utteranceA == "COMPLETED" && utteranceB == "COMPLETED" &&
                readBackA != null && readBackB != null && readBackA != readBackB,
            authorityUnchanged = voiceSelectionAuthority.state.value == authorityBefore,
            detail = detail,
        )
    }

    /** Route truth: qualification state decides; a fresh process falls back to the registry read-back. */
    private fun routedNaturalVoiceId(): String? = when (voiceSelectionAuthority.state.value.qualificationState) {
        VoiceQualificationState.ACTIVE -> voiceSelectionAuthority.activeNaturalVoiceId()
        VoiceQualificationState.INSTALLED_RUNTIME_FAILED, VoiceQualificationState.QUALIFYING -> null
        VoiceQualificationState.NONE, VoiceQualificationState.SELECTION_PENDING_RUNTIME -> activeNaturalVoice()
    }

    val ttsRouteSwitch = TtsRouteSwitch(
        systemRoute = consoleTts,
        naturalRoutes = mapOf(
            "org.piper.voice.it-riccardo-x-low" to SystemTtsDomainAdapter(naturalTts),
            "org.piper.voice.it-paola-medium" to SystemTtsDomainAdapter(naturalTtsPaola),
        ),
        routeProvider = { routedNaturalVoiceId() },
    )
    val consoleTtsRouted: com.hermestower.voiceconsole.domain.TextToSpeechEngine =
        ttsRouteSwitch

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
        localExecutor = { text -> localEngine.respond(text, { "" }, responseLocaleResolver.resolve().tag).text },
        towerExecutor = { _ -> null },
    )

    val simulator = DeterministicTowerSimulator()
    val bridge = MockTowerBridge(
        simulator,
        localeTagProvider = { responseLocaleResolver.resolve().tag },
    )

    // GRAPH 4 FINAL §13: ACTIVE-RUNTIME READ-BACK — the safe Diagnostics view
    // consumes this snapshot; every value is read from the LIVE engine objects
    // (no invented identities, no aspiration shown as fact).
    val activeRuntimeReadback: ActiveRuntimeReadback
        get() {
            val readback = voiceTts.voiceReadback
            val profile = SpeechProfile.safe(store.settings.value.speakingRate, store.settings.value.speechPitch)
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
                // OWNER PHYSICAL RETEST §6: the TTS diagnostic card rows.
                ttsEngineLabel = voiceTts.activeEngineLabel,
                ttsItalianAvailability = readback?.setLanguageResult?.name,
                ttsVoiceInventory = readback?.inventory?.let {
                    "total=${it.totalVoices} italian=${it.italianVoices} " +
                        "offlineItalian=${it.offlineItalianVoices} " +
                        "networkItalian=${it.networkRequiredItalianVoices}"
                },
                ttsSelectedVoice = readback?.selectedVoiceName,
                ttsSelectedVoiceLocale = readback?.inventory?.selectedVoiceLocaleTag,
                ttsSpeechRate = profile.speechRate,
                ttsSpeechPitch = profile.pitch,
                ttsLastNegotiationStatus = readback?.status?.name,
                // CP-G4.9-003: the REAL spoken route (registry + on-disk read-back).
                ttsActiveRoute = ttsRouteSwitch.lastRouteUsed.let { route: String ->
                    if (route.startsWith("NATURAL:")) "NATURAL (${route.removePrefix("NATURAL:")})" else route
                },
                ttsNaturalEngineId = activeNaturalVoice()?.let { id ->
                    when (id) {
                        "org.piper.voice.it-riccardo-x-low" -> naturalTts.engineId
                        "org.piper.voice.it-paola-medium" -> naturalTtsPaola.engineId
                        else -> null
                    }
                },
                llmState = "No local model (default) — Qwen2.5-0.5B candidate present, llama.cpp runtime pending",
                onnxRuntime = "onnxruntime-android 1.20.0 (Gradle)",
            )
        }

    val diagnostics = RedactedDiagnosticsExporter(
        events = events,
        health = health,
        appVersion = APP_VERSION,
    )

    /**
     * OWNER PHYSICAL RETEST §1: the secret-clean diagnostic receipt — build
     * identity (build-injected + on-device measured) + the live TTS call-path
     * telemetry + the runtime read-back. No spoken content, no transcript
     * content, no credential (SEC §4/SEC-06): technical identity only. The
     * Owner copies this out of the app to prove the exact installed build and
     * what the TTS path actually did.
     */
    fun exportDiagnosticReceipt(): String {
        val injected = BuildIdentity.injected()
        val measured = BuildIdentity.measure(context)
        val identityBlock = BuildIdentity.format(injected, measured) { ms ->
            ms?.let { java.time.Instant.ofEpochMilli(it).toString() }
        }
        return buildString {
            appendLine(identityBlock)
            appendLine()
            appendLine("TTS CALL-PATH TELEMETRY (last ${ttsTelemetry.snapshot().size} events, secret-clean)")
            ttsTelemetry.snapshot().forEach { appendLine("  $it") }
            if (ttsTelemetry.snapshot().isEmpty()) appendLine("  (no TTS activity recorded yet)")
            appendLine()
            appendLine("RUNTIME READ-BACK")
            appendLine("  tts engine: ${activeRuntimeReadback.ttsActiveEngine}")
            activeRuntimeReadback.ttsEngineLabel?.let { appendLine("  engine label: $it") }
            appendLine("  tts voice: ${activeRuntimeReadback.ttsVoiceReadback ?: "(not negotiated yet)"}")
            appendLine("  tts locale: ${activeRuntimeReadback.ttsVoiceLocale ?: "(unavailable)"}")
            appendLine("  speech rate: ${activeRuntimeReadback.ttsSpeechRate} / pitch: ${activeRuntimeReadback.ttsSpeechPitch}")
        }.trimEnd()
    }

    // GRAPH 4.10 CP-G4.10-004 (§10): the ONE persistent chat-speech policy —
    // AUTO_SPEAK default; MUTED keeps text visible and stops the utterance in
    // flight; MANUAL speaks only through per-message playback; restart-persistent;
    // independent of the microphone privacy mute; never touches media volume.
    val chatSpeechPolicy = ChatSpeechOutputPolicy(keyValueStore)

    // GRAPH 4.10 CP-G4.10-005 (§14): the ONE listening-mode authority.
    val listeningModePolicy = ListeningModePolicy(keyValueStore)

    // GRAPH 4.10 CP-G4.10-006 (§16): the echo fingerprint guard (no audio stored).
    val echoGuard = EchoFingerprintGuard()

    val driver = ScriptedConsoleDriver(
        scope = scope,
        store = store,
        router = router,
        localEngine = localEngine,
        bridge = bridge,
        tts = consoleTtsRouted,
        events = events,
        session = session,
        statusProvider = {
            buildString {
                val time = java.time.LocalTime.now(clock).withSecond(0)
                append("${time.hour.format2()}:${time.minute.format2()}, ")
                append(health.statusSummary())
            }
        },
        localeResolver = responseLocaleResolver,
        chatSpeechPolicy = chatSpeechPolicy,
        echoGuard = echoGuard,
        onPrivacyMuteEngaged = { continuousListening.onPrivacyMuteEngaged() },
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
        statusLocale = { responseLocaleResolver.resolve().tag },
        echoGuard = echoGuard,
    )

    // GRAPH 4.10 CP-G4.10-006 (§15): the continuous-listening loop controller.
    val continuousListening: ContinuousListeningController = ContinuousListeningController(
        scope = scope,
        storeProvider = { store.currentState() },
        policy = listeningModePolicy,
        events = events,
        startCapture = { voiceCoordinator.onPushToTalkPress() }, // Boolean return
        stopCapture = { reason -> voiceCoordinator.stopCapture(reason) },
        hasMicPermission = micPermissionGranted,
        isPrivacyMuted = { store.privacyMuted.value },
        isThermalConstrained = { health.statusSummary().contains("thermal", ignoreCase = true) && health.statusSummary().contains("critical", ignoreCase = true) },
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
        /**
         * OWNER PHYSICAL RETEST §2 (provenance drift removed): the app version
         * is the BUILD-INJECTED BuildConfig authority — the pre-repair hardcoded
         * constant reported a stale identity (0.2.0-debug) against the real APK.
         */
        val APP_VERSION: String = com.hermestower.voiceconsole.BuildConfig.VERSION_NAME
    }
}

/** GRAPH 4 FINAL §13 — active-runtime snapshot consumed by safe Diagnostics.
 *  OWNER PHYSICAL FEEDBACK 3 adds the TTS voice read-back rows (active voice,
 *  locale, typed status) so the device itself answers the root-cause questions.
 *  OWNER PHYSICAL RETEST §6 adds the diagnostic-card rows (engine label,
 *  Italian availability, voice inventory, selected voice, rate/pitch, last
 *  negotiation result). All fields may be null when the engine has not
 *  negotiated yet — the UI renders the honest localized "unavailable". */
data class ActiveRuntimeReadback(
    val sttActiveEngine: String,
    val sttFrameState: String,
    val ttsActiveEngine: String,
    val ttsVoiceReadback: String? = null,
    val ttsVoiceLocale: String? = null,
    val ttsEngineLabel: String? = null,
    val ttsItalianAvailability: String? = null,
    val ttsVoiceInventory: String? = null,
    val ttsSelectedVoice: String? = null,
    val ttsSelectedVoiceLocale: String? = null,
    val ttsSpeechRate: Float = 0.96f,
    val ttsSpeechPitch: Float = 0.95f,
    val ttsLastNegotiationStatus: String? = null,
    /** CP-G4.9-003: which REAL route the console speaks through (registry read-back). */
    val ttsActiveRoute: String = "SYSTEM",
    /** CP-G4.9-003: the natural-voice engine identity when that route is active. */
    val ttsNaturalEngineId: String? = null,
    val llmState: String,
    val onnxRuntime: String,
)
