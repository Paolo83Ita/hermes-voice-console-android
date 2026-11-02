package com.hermestower.voiceconsole.app

import com.hermestower.voiceconsole.bridge.DeterministicTowerSimulator
import com.hermestower.voiceconsole.bridge.MockTowerBridge
import com.hermestower.voiceconsole.core.routing.RuleBasedIntentRouter
import com.hermestower.voiceconsole.core.state.ConsoleState
import com.hermestower.voiceconsole.core.state.ConsoleStateStore
import com.hermestower.voiceconsole.core.state.ConversationTurn
import com.hermestower.voiceconsole.domain.TextToSpeechEngine
import com.hermestower.voiceconsole.local.MockLocalConversationEngine
import com.hermestower.voiceconsole.migration.InMemoryKeyValueStore
import com.hermestower.voiceconsole.simulator.ScriptedConsoleDriver
import com.hermestower.voiceconsole.store.DefaultSessionController
import com.hermestower.voiceconsole.store.InMemoryEventStore
import com.hermestower.voiceconsole.voice.ScriptedSystemSttEngine
import com.hermestower.voiceconsole.voice.SystemSttError
import com.hermestower.voiceconsole.voice.SystemSttErrorKind
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * GRAPH 4 G4-C03 tests: the real microphone path wired into the Main Console
 * through a single state authority and the SAME pipeline as typed text
 * (routing, attribution, history, receipts — Owner binding delta §1/§13).
 * Physics: LISTENING only while the recognizer captures; mute/permission/
 * unavailability are typed refusals; the draft persists across recreation.
 */
class ConsoleVoiceCoordinatorTest {

    private lateinit var scope: CoroutineScope
    private lateinit var store: ConsoleStateStore
    private lateinit var events: InMemoryEventStore
    private lateinit var stt: ScriptedSystemSttEngine
    private lateinit var tts: SpyTts
    private lateinit var driver: ScriptedConsoleDriver
    private lateinit var kv: InMemoryKeyValueStore
    private var micGranted = true

    private class SpyTts : TextToSpeechEngine {
        val spoken = mutableListOf<String>()
        var interrupts = 0
        override val state = MutableStateFlow(TextToSpeechEngine.State.READY)
        override fun speak(text: String, rate: Float): Flow<Float> = flow {
            spoken += text
            emit(0.6f)
        }
        override fun interrupt() { interrupts++ }
    }

    @Before
    fun setUp() {
        store = ConsoleStateStore()
        events = InMemoryEventStore()
        stt = ScriptedSystemSttEngine(available = true)
        tts = SpyTts()
        kv = InMemoryKeyValueStore()
    }

    @After
    fun tearDown() {
        if (this::scope.isInitialized) scope.cancel()
    }

    private fun build(testDispatcher: TestDispatcher): ConsoleVoiceCoordinator {
        scope = CoroutineScope(SupervisorJob() + testDispatcher)
        val bridge = MockTowerBridge(DeterministicTowerSimulator(), testDispatcher)
        driver = ScriptedConsoleDriver(
            scope = scope,
            store = store,
            router = RuleBasedIntentRouter(),
            localEngine = MockLocalConversationEngine(),
            bridge = bridge,
            tts = tts,
            events = events,
            session = DefaultSessionController(),
            statusProvider = { "12:00, test fixture" },
        )
        store.transition(ConsoleState.IDLE)
        driver.start()
        return ConsoleVoiceCoordinator(
            scope = scope,
            store = store,
            driver = driver,
            systemStt = stt,
            hasMicPermission = { micGranted },
            keyValueStore = kv,
            events = events,
        )
    }

    @Test
    fun `mute refuses push-to-talk — no capture attempt, typed refusal`() = runTest {
        val c = build(UnconfinedTestDispatcher(testScheduler))
        driver.setPrivacyMuted(true)
        c.onPushToTalkPress()
        assertEquals(0, stt.startCalls) // no hardware access while muted
        assertNotNull(c.lastRefusal.value)
        assertEquals(ConsoleState.PRIVACY_MUTED, store.currentState())
        assertNull(store.transcriptDraft.value)
    }

    @Test
    fun `missing permission routes to typed PERMISSION_REQUIRED, hardware untouched`() = runTest {
        micGranted = false
        val c = build(UnconfinedTestDispatcher(testScheduler))
        c.onPushToTalkPress()
        assertEquals(MicUiState.PERMISSION_REQUIRED, c.micState.value)
        assertEquals(0, stt.startCalls)
        assertTrue(store.currentState() != ConsoleState.LISTENING) // never fake listening
    }

    @Test
    fun `unavailable recognizer refuses with typed UNAVAILABLE and stays idle`() = runTest {
        stt = ScriptedSystemSttEngine(available = false)
        val c = build(UnconfinedTestDispatcher(testScheduler))
        c.onPushToTalkPress()
        assertEquals(MicUiState.UNAVAILABLE, c.micState.value)
        assertNotNull(c.lastRefusal.value)
        assertEquals(ConsoleState.IDLE, store.currentState())
        assertEquals(0, stt.startCalls)
    }

    @Test
    fun `happy path — partial updates live draft, final enters the SHARED pipeline with attribution`() = runTest {
        val c = build(UnconfinedTestDispatcher(testScheduler))
        c.onPushToTalkPress()
        assertEquals(MicUiState.LISTENING, c.micState.value)
        assertEquals(ConsoleState.LISTENING, store.currentState())
        stt.emitPartial("good day")
        assertEquals("good day", store.transcriptDraft.value)
        stt.emitFinal("good day hermes", 0.88f)
        advanceUntilIdle()
        val ownerTurn = store.turns.value.lastOrNull { it.role == ConversationTurn.Role.OWNER }
        assertNotNull(ownerTurn)
        assertEquals("good day hermes", ownerTurn!!.text)
        val reply = store.turns.value.lastOrNull { it.role == ConversationTurn.Role.HERMES }
        assertNotNull(reply)
        assertEquals(ConversationTurn.Delivery.DELIVERED, reply!!.delivery)
        assertEquals(MicUiState.IDLE, c.micState.value)
        assertEquals(ConsoleState.IDLE, store.currentState())
        // The last spoken reply came out of the console TTS (shared path).
        assertTrue(tts.spoken.isNotEmpty())
    }

    @Test
    fun `no speech ends the turn truthfully — no fake LISTENING residue`() = runTest {
        val c = build(UnconfinedTestDispatcher(testScheduler))
        c.onPushToTalkPress()
        stt.emitNoSpeech()
        assertEquals(MicUiState.IDLE, c.micState.value)
        assertEquals(ConsoleState.IDLE, store.currentState())
        assertTrue(store.turns.value.isEmpty())
    }

    @Test
    fun `owner stop physically stops capture and returns to idle`() = runTest {
        val c = build(UnconfinedTestDispatcher(testScheduler))
        c.onPushToTalkPress()
        c.stopCapture("owner stop")
        assertEquals(1, stt.stopCalls)
        assertEquals(MicUiState.IDLE, c.micState.value)
        assertEquals(ConsoleState.IDLE, store.currentState())
    }

    @Test
    fun `lifecycle pause stops capture (foreground-only contract)`() = runTest {
        val c = build(UnconfinedTestDispatcher(testScheduler))
        c.onPushToTalkPress()
        c.onConsolePaused()
        assertEquals(1, stt.stopCalls)
        assertEquals(ConsoleState.IDLE, store.currentState())
    }

    @Test
    fun `draft persists across coordinator recreation and restart`() = runTest {
        build(UnconfinedTestDispatcher(testScheduler))
        store.setTranscriptDraft("bozza non ancora inviata")
        advanceUntilIdle() // persist collector writes through
        // Recreate the coordinator with the SAME store (app process recreation).
        ConsoleVoiceCoordinator(
            scope = scope, store = store, driver = driver, systemStt = stt,
            hasMicPermission = { micGranted }, keyValueStore = kv, events = events,
        )
        assertEquals("bozza non ancora inviata", store.transcriptDraft.value)
        assertEquals("bozza non ancora inviata", kv.getString(ConsoleVoiceCoordinator.DRAFT_KEY))
    }

    @Test
    fun `sending clears the persisted draft`() = runTest {
        val c = build(UnconfinedTestDispatcher(testScheduler))
        store.setTranscriptDraft("good day hermes")
        advanceUntilIdle()
        driver.submitText("good day hermes")
        advanceUntilIdle()
        assertNull(store.transcriptDraft.value) // §15: sending clears the draft
        assertNull(kv.getString(ConsoleVoiceCoordinator.DRAFT_KEY))
    }

    @Test
    fun `typed permission classification — denial then permanent denial`() = runTest {
        val c = build(UnconfinedTestDispatcher(testScheduler))
        c.onMicPermissionResult(granted = false, shouldShowRationale = true)
        assertEquals(MicUiState.DENIED, c.micState.value)
        c.onMicPermissionResult(granted = false, shouldShowRationale = false)
        assertEquals(MicUiState.PERMANENTLY_DENIED, c.micState.value)
        assertTrue(c.micPermissionPreviouslyRequested())
    }

    @Test
    fun `granted permission from the launcher resumes push-to-talk`() = runTest {
        micGranted = false
        val c = build(UnconfinedTestDispatcher(testScheduler))
        c.onPushToTalkPress()
        assertEquals(MicUiState.PERMISSION_REQUIRED, c.micState.value)
        micGranted = true
        c.onMicPermissionResult(granted = true, shouldShowRationale = false)
        assertEquals(MicUiState.IDLE, c.micState.value)
        c.onPushToTalkPress()
        assertEquals(MicUiState.LISTENING, c.micState.value)
        assertEquals(1, stt.startCalls)
    }

    @Test
    fun `repeat last spoken response re-speaks the last HERMES turn`() = runTest {
        val c = build(UnconfinedTestDispatcher(testScheduler))
        c.onPushToTalkPress()
        stt.emitFinal("good day hermes", 0.9f)
        advanceUntilIdle()
        val spokenCount = tts.spoken.size
        assertTrue(spokenCount >= 1)
        driver.repeatLastSpoken()
        advanceUntilIdle()
        assertEquals(spokenCount + 1, tts.spoken.size)
        assertEquals(tts.spoken[spokenCount - 1], tts.spoken.last())
    }

    @Test
    fun `cancel current turn stops everything and returns to idle`() = runTest {
        val c = build(UnconfinedTestDispatcher(testScheduler))
        c.onPushToTalkPress()
        stt.emitFinal("prepare tomorrow mission plan", 0.9f)
        driver.cancelCurrentTurn()
        advanceUntilIdle()
        assertEquals(ConsoleState.IDLE, store.currentState())
    }

    @Test
    fun `recognizer network error is typed and never blocks text input`() = runTest {
        val c = build(UnconfinedTestDispatcher(testScheduler))
        c.onPushToTalkPress()
        stt.events!!.onError(SystemSttError(SystemSttErrorKind.NETWORK_REQUIRED, 2))
        assertEquals(MicUiState.IDLE, c.micState.value)
        assertEquals(ConsoleState.IDLE, store.currentState())
        // Text input still works offline.
        driver.submitText("good day hermes")
        advanceUntilIdle()
        assertTrue(store.turns.value.any { it.role == ConversationTurn.Role.HERMES })
    }
}
