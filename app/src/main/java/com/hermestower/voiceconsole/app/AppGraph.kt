package com.hermestower.voiceconsole.app

import android.content.Context
import com.hermestower.voiceconsole.bridge.DeterministicTowerSimulator
import com.hermestower.voiceconsole.bridge.MockTowerBridge
import com.hermestower.voiceconsole.core.routing.RuleBasedIntentRouter
import com.hermestower.voiceconsole.core.state.ConsoleStateStore
import com.hermestower.voiceconsole.domain.TowerEndpoint
import com.hermestower.voiceconsole.health.AndroidDeviceHealthMonitor
import com.hermestower.voiceconsole.local.MockLocalConversationEngine
import com.hermestower.voiceconsole.privacy.LogRedactor
import com.hermestower.voiceconsole.simulator.ScriptedConsoleDriver
import com.hermestower.voiceconsole.store.DefaultSessionController
import com.hermestower.voiceconsole.store.InMemoryEventStore
import com.hermestower.voiceconsole.store.InMemorySecureConfigurationStore
import com.hermestower.voiceconsole.store.RedactedDiagnosticsExporter
import com.hermestower.voiceconsole.store.SettingsRepository
import com.hermestower.voiceconsole.tts.MockTextToSpeechEngine
import com.hermestower.voiceconsole.voice.InactiveSpeechToTextEngine
import com.hermestower.voiceconsole.voice.InactiveVoiceActivityDetector
import com.hermestower.voiceconsole.voice.InactiveWakeWordEngine
import com.hermestower.voiceconsole.voice.NoAudioCaptureEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.Clock

/**
 * V0.1 service locator (decision D-003). One place wires every seam; swapping a
 * provider later means changing a line here, not the UI or the pipeline.
 */
class AppGraph(context: Context) {

    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    val clock: Clock = Clock.systemDefaultZone()

    val settingsRepository = SettingsRepository(context)
    val store = ConsoleStateStore()
    val health = AndroidDeviceHealthMonitor(context)
    val events = InMemoryEventStore()
    val session = DefaultSessionController()
    val secureStore = InMemorySecureConfigurationStore()

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
        appVersion = "0.1.0-alpha",
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
        events.log("APP", "graph started (mock bridge; redaction ${LogRedactor.redact("token: sample-value")})")
    }

    private fun Int.format2(): String = toString().padStart(2, '0')
}
