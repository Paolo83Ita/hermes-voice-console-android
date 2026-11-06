package com.hermestower.voiceconsole.selftest

import com.hermestower.voiceconsole.models.ModelCatalogEntry
import com.hermestower.voiceconsole.models.ModelComponentType
import com.hermestower.voiceconsole.models.DefaultModelDownloadManager
import com.hermestower.voiceconsole.models.ModelInstallationRegistry
import com.hermestower.voiceconsole.models.ModelQualificationRunner
import com.hermestower.voiceconsole.models.ModelRollbackController
import com.hermestower.voiceconsole.models.ModelActivationController
import com.hermestower.voiceconsole.models.ModelStorageManager
import com.hermestower.voiceconsole.models.VoiceModelCatalog
import com.hermestower.voiceconsole.models.VoiceModelProfile
import com.hermestower.voiceconsole.models.VoicePackResolver
import com.hermestower.voiceconsole.models.DeviceResources
import com.hermestower.voiceconsole.privacy.LogRedactor
import com.hermestower.voiceconsole.core.mode.OperatingMode
import com.hermestower.voiceconsole.core.routing.Connectivity
import com.hermestower.voiceconsole.core.routing.RuleBasedIntentRouter
import com.hermestower.voiceconsole.core.routing.RoutingRequest
import com.hermestower.voiceconsole.core.routing.RoutingAction
import com.hermestower.voiceconsole.core.state.ConsoleState
import com.hermestower.voiceconsole.core.state.ConsoleSettings
import com.hermestower.voiceconsole.core.state.ConsoleStateMachine
import com.hermestower.voiceconsole.core.state.ConnectionStatus
import com.hermestower.voiceconsole.tower.ConnectionConfigState
import com.hermestower.voiceconsole.tower.ImportResult
import com.hermestower.voiceconsole.tower.RealTowerBridge
import com.hermestower.voiceconsole.tower.SecretVault
import com.hermestower.voiceconsole.tower.TowerConnectionProfile
import com.hermestower.voiceconsole.tower.TowerConnectionTester
import com.hermestower.voiceconsole.tower.TowerProfileStore
import com.hermestower.voiceconsole.tower.TowerProfileValidator
import com.hermestower.voiceconsole.tower.VaultKeys
import com.hermestower.voiceconsole.towersim.LocalTowerMockServer
import com.hermestower.voiceconsole.migration.SettingsMigrator
import com.hermestower.voiceconsole.migration.KeyValueStore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import java.time.Duration
import java.time.Instant
import com.hermestower.voiceconsole.archive.MockTowerArchive
import com.hermestower.voiceconsole.models.BundledModelInstaller
import com.hermestower.voiceconsole.pairing.PairingPayloadParser

/**
 * Runtime dependencies the Self-Test Center exercises. Everything here is already
 * part of the app graph; the center owns no business logic of its own.
 */
data class SelfTestSubjects(
    val catalog: VoiceModelCatalog,
    val resolver: VoicePackResolver,
    val storage: ModelStorageManager,
    val registry: ModelInstallationRegistry,
    val downloads: DefaultModelDownloadManager,
    val qualificationRunner: ModelQualificationRunner,
    val activation: ModelActivationController,
    val rollback: ModelRollbackController,
    val profileStore: TowerProfileStore,
    val vault: SecretVault,
    val realBridge: RealTowerBridge,
    val deviceResources: () -> DeviceResources,
    val keyValueStore: KeyValueStore,
    val reopenKeyValueStore: () -> KeyValueStore,
    val reopenRegistry: () -> ModelInstallationRegistry,
    val serverFactory: () -> LocalTowerMockServer,
    val appVersion: String,
    val commitReference: String?,
    val isAndroidRuntime: Boolean,
    // GRAPH 4 self-test expansion (G4-C11 wave): bundled delivery, QR pairing
    // parser and Tower Archive subjects. Nullable members degrade honestly to
    // SKIPPED_NOT_APPLICABLE on runtimes that cannot provide them (JVM checks).
    val bundledManifestText: () -> String? = { null },
    val bundledInstaller: BundledModelInstaller? = null,
    val pairingParserFactory: () -> PairingPayloadParser = { PairingPayloadParser() },
    val towerArchive: MockTowerArchive? = null,
    // OWNER PHYSICAL FEEDBACK 3 (ST-51): the live TTS voice read-back — null
    // provider or null read-back degrades to the honest typed skip/blocked.
    val italianTtsVoiceProbe: (() -> com.hermestower.voiceconsole.voice.ItalianTtsVoiceNegotiator.TtsVoiceReadback?)? = null,
    // CP-G4.9-003 (ST-52): the natural local TTS route read-back — which REAL
    // engine the console speaks through, from the registry + on-disk truth.
    val naturalTtsRouteProbe: (() -> NaturalTtsRouteReadback?)? = null,
    // CP-G4.9-004 (ST-53): the local offline STT route read-back (Vosk frame
    // path vs the Android system recognizer).
    val localSttRouteProbe: (() -> LocalSttRouteReadback?)? = null,
)

/** CP-G4.9-003 (ST-52): the honest snapshot of the spoken-output route. */
data class NaturalTtsRouteReadback(
    /** "SYSTEM" or "NATURAL:<voice identity>" (registry + bundle read-back). */
    val activeRoute: String,
    /** The natural engine identity when that route is bound, else null. */
    val naturalEngineId: String?,
    /** The verified piper bundle resolves on disk. */
    val voiceBundlePresent: Boolean,
    /** Last utterance telemetry line (route=natural), or null. */
    val lastUtteranceEvent: String? = null,
)

/** CP-G4.9-004 (ST-53): the honest snapshot of the speech-input route. */
data class LocalSttRouteReadback(
    /** The ACTIVE STT engine identity (never an aspiration). */
    val activeEngine: String,
    /** Whether the offline vosk model is installed and loaded. */
    val offlineModelLoaded: Boolean,
    /** The loaded model directory name, when loaded. */
    val loadedModelDir: String? = null,
)

/**
 * SelfTestCenter (GRAPH 2 §11): runs the deterministic test battery in the five
 * modes, keeps typed results, and exposes stop/retry/export/reset.
 * Runs on a single-threaded dispatcher per run; every check is bounded.
 */
class SelfTestCenter(
    private val subjects: SelfTestSubjects,
    private val dispatcher: CoroutineDispatcher,
) {
    private val scope = CoroutineScope(SupervisorJob() + dispatcher)
    private val definitions: List<SelfTestDefinition> by lazy { SelfTestRegistry.build(subjects) }

    private val _results = MutableStateFlow<Map<String, SelfTestResult>>(emptyMap())
    val results: StateFlow<Map<String, SelfTestResult>> = _results.asStateFlow()

    private val _running = MutableStateFlow(false)
    val running: StateFlow<Boolean> = _running.asStateFlow()

    private val _current = MutableStateFlow<String?>(null)
    val current: StateFlow<String?> = _current.asStateFlow()

    private var runJob: Job? = null

    fun definitionsFor(mode: SelfTestMode): List<SelfTestDefinition> =
        definitions.filter { mode in it.modes }

    fun run(mode: SelfTestMode) {
        if (_running.value) return
        val selected = definitionsFor(mode)
        _results.value = definitions.associate { it.id to notRun(it) }
        _running.value = true
        runJob = scope.launch {
            for (definition in selected) {
                _current.value = definition.id
                _results.value = _results.value + (definition.id to running(definition))
                val result = try {
                    withTimeout(TIMEOUT_MS) {
                        kotlinx.coroutines.withContext(dispatcher) { definition.check.run(context()) }
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    fail(definition, e)
                }
                _results.value = _results.value + (definition.id to result)
            }
            _current.value = null
            _running.value = false
        }
    }

    fun stop() {
        runJob?.cancel()
        runJob = null
        _current.value = null
        _running.value = false
        // Tests still marked RUNNING are reset to NOT_RUN: honest bookkeeping.
        _results.value = _results.value.mapValues { (_, v) -> if (v.state == SelfTestState.RUNNING) v.copy(state = SelfTestState.NOT_RUN, evidence = "Stopped by user before completion.") else v }
    }

    fun retryFailed() {
        if (_running.value) return
        val failed = _results.value.filterValues { it.state == SelfTestState.FAIL || it.state == SelfTestState.NOT_RUN }.keys
        if (failed.isEmpty()) return
        val selected = definitions.filter { it.id in failed }
        _running.value = true
        runJob = scope.launch {
            for (definition in selected) {
                _current.value = definition.id
                _results.value = _results.value + (definition.id to running(definition))
                val result = try {
                    withTimeout(TIMEOUT_MS) { definition.check.run(context()) }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    fail(definition, e)
                }
                _results.value = _results.value + (definition.id to result)
            }
            _current.value = null
            _running.value = false
        }
    }

    fun resetHistory() {
        if (_running.value) return
        _results.value = emptyMap()
    }

    fun summary(): String {
        val values = _results.value
        val pass = values.values.count { it.state == SelfTestState.PASS }
        val fail = values.values.count { it.state == SelfTestState.FAIL }
        val skipped = values.values.count { it.state == SelfTestState.SKIPPED_NOT_APPLICABLE }
        val blocked = values.values.count { it.state == SelfTestState.BLOCKED_DEPENDENCY }
        val pending = values.values.count { it.state == SelfTestState.QUALIFICATION_PENDING_DEVICE }
        return "Self-Test: $pass pass, $fail fail, $skipped skipped, $blocked blocked, $pending pending-device" +
            " (of ${values.size} executed definitions; app ${subjects.appVersion})"
    }

    /** Secret-clean export: every result field passes the redactor before leaving the app. */
    fun exportSecretCleanReport(): String = buildString {
        appendLine("Hermes Voice Console — Self-Test Report (secret-clean)")
        appendLine("app: ${subjects.appVersion}  commit: ${subjects.commitReference ?: "unrecorded"}")
        appendLine("environment: ${if (subjects.isAndroidRuntime) "android-runtime" else "in-app"}  vault: ${subjects.vault.available().displayLabel}")
        appendLine()
        _results.value.toSortedMap().forEach { (id, r) ->
            appendLine(
                "$id | ${LogRedactor.redact(r.testName)} | ${r.state.name}" +
                    " | ${r.durationMs} ms | ${LogRedactor.redact(r.evidence)}",
            )
            r.failureType?.let { appendLine("     failure: ${LogRedactor.redact(it)}") }
            r.repairSuggestion?.let { appendLine("     repair:  ${LogRedactor.redact(it)}") }
        }
        appendLine()
        appendLine(summary())
    }

    private fun context() = SelfTestContext(
        environment = if (subjects.isAndroidRuntime) "android-runtime" else "in-app",
        appVersion = subjects.appVersion,
        commitReference = subjects.commitReference,
        isAndroidRuntime = subjects.isAndroidRuntime,
    )

    private fun notRun(definition: SelfTestDefinition) = SelfTestResult(
        testId = definition.id,
        testName = definition.name,
        startTime = null,
        finishTime = null,
        durationMs = 0,
        state = SelfTestState.NOT_RUN,
        evidence = "Not run yet.",
        environment = "in-app",
        appVersion = subjects.appVersion,
        commitReference = subjects.commitReference,
    )

    private fun running(definition: SelfTestDefinition) = SelfTestResult(
        testId = definition.id,
        testName = definition.name,
        startTime = Instant.now(),
        finishTime = null,
        durationMs = 0,
        state = SelfTestState.RUNNING,
        evidence = "Running.",
        environment = context().environment,
        appVersion = subjects.appVersion,
        commitReference = subjects.commitReference,
    )

    private fun fail(definition: SelfTestDefinition, e: Exception) = SelfTestResult(
        testId = definition.id,
        testName = definition.name,
        startTime = Instant.now(),
        finishTime = Instant.now(),
        durationMs = 0,
        state = SelfTestState.FAIL,
        evidence = "Unhandled exception: ${e.javaClass.simpleName}: ${LogRedactor.redact(e.message ?: "")}",
        failureType = "UNHANDLED_EXCEPTION",
        repairSuggestion = "Inspect diagnostics; rerun the failing test.",
        environment = context().environment,
        appVersion = subjects.appVersion,
        commitReference = subjects.commitReference,
    )

    companion object {
        const val TIMEOUT_MS = 120_000L
    }
}
