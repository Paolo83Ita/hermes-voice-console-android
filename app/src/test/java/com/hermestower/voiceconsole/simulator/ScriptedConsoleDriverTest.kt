package com.hermestower.voiceconsole.simulator

import com.hermestower.voiceconsole.bridge.BridgeFault
import com.hermestower.voiceconsole.bridge.DeterministicTowerSimulator
import com.hermestower.voiceconsole.bridge.MockTowerBridge
import com.hermestower.voiceconsole.core.routing.RuleBasedIntentRouter
import com.hermestower.voiceconsole.core.state.ConsoleState
import com.hermestower.voiceconsole.core.state.ConsoleStateStore
import com.hermestower.voiceconsole.core.state.ConversationTurn
import com.hermestower.voiceconsole.local.MockLocalConversationEngine
import com.hermestower.voiceconsole.store.DefaultSessionController
import com.hermestower.voiceconsole.store.InMemoryEventStore
import com.hermestower.voiceconsole.tts.MockTextToSpeechEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Full V0.1 pipeline integration tests: local path, tower path, queue path,
 * mute gating and the no-transcript-in-logs privacy rule — real router, state
 * machine, mock bridge and mock TTS on virtual time.
 */
class ScriptedConsoleDriverTest {

    private val fixedStatus = "12:00, Android 13 (test fixture)"
    private lateinit var scope: CoroutineScope
    private lateinit var store: ConsoleStateStore
    private lateinit var events: InMemoryEventStore
    private lateinit var bridge: MockTowerBridge
    private lateinit var driver: ScriptedConsoleDriver

    @Before
    fun setUp() {
        store = ConsoleStateStore()
        events = InMemoryEventStore()
    }

    @After
    fun tearDown() {
        if (this::scope.isInitialized) scope.cancel()
    }

    private fun buildDriver(testDispatcher: TestDispatcher): ScriptedConsoleDriver {
        scope = CoroutineScope(SupervisorJob() + testDispatcher)
        bridge = MockTowerBridge(DeterministicTowerSimulator(), testDispatcher)
        return ScriptedConsoleDriver(
            scope = scope,
            store = store,
            router = RuleBasedIntentRouter(),
            localEngine = MockLocalConversationEngine(),
            bridge = bridge,
            tts = MockTextToSpeechEngine(testDispatcher),
            events = events,
            session = DefaultSessionController(),
            statusProvider = { fixedStatus },
        )
    }

    private suspend fun kotlinx.coroutines.test.TestScope.bootDriver(): ScriptedConsoleDriver {
        val d = buildDriver(UnconfinedTestDispatcher(testScheduler))
        store.transition(ConsoleState.IDLE)
        bridge.handshake() // deterministic handshake on virtual time
        d.start()
        return d
    }

    @Test
    fun `local turn resolves locally and returns to idle`() = runTest {
        val driver = bootDriver()
        driver.submitText("good day hermes")
        advanceUntilIdle()

        assertEquals(ConsoleState.IDLE, store.currentState())
        val turns = store.turns.value
        assertEquals(2, turns.size)
        assertEquals(ConversationTurn.Role.OWNER, turns[0].role)
        assertEquals(ConversationTurn.Source.LOCAL, turns[1].source)
        assertEquals(ConversationTurn.Delivery.DELIVERED, turns[1].delivery)
        assertTrue(turns[1].text.contains("standing by"))
    }

    @Test
    fun `consequential turn routes to the tower with receipt`() = runTest {
        val driver = bootDriver()
        driver.submitText("prepare tomorrow mission plan")
        advanceUntilIdle()

        assertEquals(ConsoleState.IDLE, store.currentState())
        val turns = store.turns.value
        assertEquals(ConversationTurn.Source.TOWER, turns[0].source)
        assertEquals(ConversationTurn.Delivery.DELIVERED, turns[0].delivery)
        assertTrue(turns[0].receiptRef!!.startsWith("rcp-mock-"))
    }

    @Test
    fun `transport failure queues non-consequential turn`() = runTest {
        val driver = bootDriver()
        bridge.setFault(BridgeFault.DISCONNECT_AT_ACK)
        driver.submitText("what is the weather")
        advanceUntilIdle()

        val ownerTurn = store.turns.value.first { it.role == ConversationTurn.Role.OWNER }
        assertEquals(ConversationTurn.Delivery.QUEUED, ownerTurn.delivery)
        assertEquals(1, store.queueCount.value)
        bridge.setFault(BridgeFault.NONE)
    }

    @Test
    fun `privacy mute ignores wake and requests`() = runTest {
        val driver = bootDriver()
        driver.setPrivacyMuted(true)
        assertEquals(ConsoleState.PRIVACY_MUTED, store.currentState())

        val turnsBefore = store.turns.value.size
        driver.injectWake()
        driver.submitText("hello")
        advanceUntilIdle()

        assertEquals(turnsBefore, store.turns.value.size)
        driver.setPrivacyMuted(false)
        assertEquals(ConsoleState.IDLE, store.currentState())
    }

    @Test
    fun `event log never contains transcript content`() = runTest {
        val driver = bootDriver()
        val secretPhrase = "the vault passphrase is sunrise-seven"
        driver.submitText(secretPhrase)
        advanceUntilIdle()

        val allEvents = events.recent(500).joinToString("\n") { "${it.type} ${it.detail}" }
        assertTrue(!allEvents.contains(secretPhrase))
        assertTrue(allEvents.contains("ROUTE"))
    }

    // ---- GRAPH 4.10 CP-G4.10-002: response-language authority (P0-01) ----

    @Test
    fun `italian app language produces italian visible and spoken text end to end`() = runTest {
        val italianResolver = com.hermestower.voiceconsole.locale.ResponseLocaleResolver(
            appLanguageTag = { "it" },
            systemLanguageTag = { "it" },
        )
        scope = CoroutineScope(SupervisorJob() + UnconfinedTestDispatcher(testScheduler))
        val italianBridge = MockTowerBridge(DeterministicTowerSimulator(), UnconfinedTestDispatcher(testScheduler), localeTagProvider = { "it" })
        val mockTts = MockTextToSpeechEngine(UnconfinedTestDispatcher(testScheduler))
        val italianDriver = ScriptedConsoleDriver(
            scope = scope,
            store = store,
            router = RuleBasedIntentRouter(),
            localEngine = MockLocalConversationEngine(),
            bridge = italianBridge,
            tts = mockTts,
            events = events,
            session = DefaultSessionController(),
            statusProvider = { fixedStatus },
            localeResolver = italianResolver,
        )
        store.transition(ConsoleState.IDLE)
        italianBridge.handshake()
        italianDriver.start()

        italianDriver.submitText("good day hermes")
        advanceUntilIdle()

        val turns = store.turns.value
        assertEquals(2, turns.size)
        // Visible assistant text is ITALIAN — the exact P0-01 defect is dead.
        assertEquals(
            com.hermestower.voiceconsole.locale.ResponseCatalog.text(
                com.hermestower.voiceconsole.locale.ResponseCategory.GREETING, "it",
            ),
            turns[1].text,
        )
        // TTS input equals the visible text (§6 coherence) — mock TTS records it.
        assertEquals(turns[1].text, mockTts.lastSpokenText)
    }

    @Test
    fun `italian tower path yields italian simulated response and localized notes`() = runTest {
        val italianResolver = com.hermestower.voiceconsole.locale.ResponseLocaleResolver(
            appLanguageTag = { "it" },
            systemLanguageTag = { "it" },
        )
        scope = CoroutineScope(SupervisorJob() + UnconfinedTestDispatcher(testScheduler))
        val italianBridge = MockTowerBridge(DeterministicTowerSimulator(), UnconfinedTestDispatcher(testScheduler), localeTagProvider = { "it" })
        val italianDriver = ScriptedConsoleDriver(
            scope = scope,
            store = store,
            router = RuleBasedIntentRouter(),
            localEngine = MockLocalConversationEngine(),
            bridge = italianBridge,
            tts = MockTextToSpeechEngine(UnconfinedTestDispatcher(testScheduler)),
            events = events,
            session = DefaultSessionController(),
            statusProvider = { fixedStatus },
            localeResolver = italianResolver,
        )
        store.transition(ConsoleState.IDLE)
        italianBridge.handshake()
        italianDriver.start()

        italianDriver.submitText("prepare tomorrow mission plan")
        advanceUntilIdle()

        val turns = store.turns.value
        val hermesTurn = turns.last { it.role == ConversationTurn.Role.HERMES }
        assertTrue(hermesTurn.text.contains("predisporre un piano di missione"))
        assertTrue(hermesTurn.text.contains("simulazione"))
    }

    // ---- GRAPH 4.10 CP-G4.10-004: chat speech output policy (P0-04/P0-05) ----

    @Test
    fun `MUTED keeps the assistant text visible and speaks nothing`() = runTest {
        scope = CoroutineScope(SupervisorJob() + UnconfinedTestDispatcher(testScheduler))
        val mockTts = MockTextToSpeechEngine(UnconfinedTestDispatcher(testScheduler))
        val policy = com.hermestower.voiceconsole.voice.ChatSpeechOutputPolicy(
            com.hermestower.voiceconsole.migration.InMemoryKeyValueStore(),
        )
        policy.setMode(com.hermestower.voiceconsole.voice.ChatSpeechOutputPolicy.Mode.MUTED, "test")
        val localBridge = MockTowerBridge(DeterministicTowerSimulator(), UnconfinedTestDispatcher(testScheduler))
        val d = ScriptedConsoleDriver(
            scope = scope, store = store, router = RuleBasedIntentRouter(),
            localEngine = MockLocalConversationEngine(), bridge = localBridge, tts = mockTts,
            events = events, session = DefaultSessionController(), statusProvider = { fixedStatus },
            chatSpeechPolicy = policy,
        )
        store.transition(ConsoleState.IDLE)
        localBridge.handshake()
        d.start()

        d.submitText("good day hermes")
        advanceUntilIdle()

        // The Italian/English text is VISIBLE (the turn was appended)...
        val turns = store.turns.value
        assertEquals(2, turns.size)
        assertEquals(ConsoleState.IDLE, store.currentState())
        // ...but NO audio was produced and SPEAKING was never claimed.
        assertEquals(null, mockTts.lastSpokenText)
        assertTrue(events.recent(50).none { it.detail.contains("SPEAKING claimed") })
    }

    @Test
    fun `stopping speech never mutes future responses`() = runTest {
        scope = CoroutineScope(SupervisorJob() + UnconfinedTestDispatcher(testScheduler))
        val mockTts = MockTextToSpeechEngine(UnconfinedTestDispatcher(testScheduler))
        val d = buildDriver(UnconfinedTestDispatcher(testScheduler)).let {
            ScriptedConsoleDriver(
                scope = scope, store = store, router = RuleBasedIntentRouter(),
                localEngine = MockLocalConversationEngine(), bridge = bridge, tts = mockTts,
                events = events, session = DefaultSessionController(), statusProvider = { fixedStatus },
            )
        }
        store.transition(ConsoleState.IDLE)
        bridge.handshake()
        d.start()

        d.submitText("good day hermes")
        advanceUntilIdle()
        val firstSpoken = mockTts.lastSpokenText
        d.stopSpeaking() // user pressed the stop control
        assertEquals(ConsoleState.IDLE, store.currentState())

        d.submitText("status report")
        advanceUntilIdle()
        // The next response IS spoken again (stop != mute, §10) and it is the
        // SECOND response (not the stale first one).
        val secondText = store.turns.value.last { it.role == ConversationTurn.Role.HERMES }.text
        org.junit.Assert.assertNotEquals(firstSpoken, mockTts.lastSpokenText)
        assertEquals(secondText, mockTts.lastSpokenText)
    }

    @Test
    fun `per-message playback speaks the exact visible text`() = runTest {
        scope = CoroutineScope(SupervisorJob() + UnconfinedTestDispatcher(testScheduler))
        val mockTts = MockTextToSpeechEngine(UnconfinedTestDispatcher(testScheduler))
        val policy = com.hermestower.voiceconsole.voice.ChatSpeechOutputPolicy(
            com.hermestower.voiceconsole.migration.InMemoryKeyValueStore(),
        )
        policy.setMode(com.hermestower.voiceconsole.voice.ChatSpeechOutputPolicy.Mode.MANUAL, "test")
        val manualBridge = MockTowerBridge(DeterministicTowerSimulator(), UnconfinedTestDispatcher(testScheduler))
        val d = ScriptedConsoleDriver(
            scope = scope, store = store, router = RuleBasedIntentRouter(),
            localEngine = MockLocalConversationEngine(), bridge = manualBridge, tts = mockTts,
            events = events, session = DefaultSessionController(), statusProvider = { fixedStatus },
            chatSpeechPolicy = policy,
        )
        store.transition(ConsoleState.IDLE)
        manualBridge.handshake()
        d.start()

        d.submitText("good day hermes")
        advanceUntilIdle()
        assertEquals(null, mockTts.lastSpokenText) // MANUAL: no auto speech

        val assistantText = store.turns.value.last { it.role == ConversationTurn.Role.HERMES }.text
        d.repeatMessage(assistantText)
        advanceUntilIdle()
        // Explicit playback: TTS input == visible text (§6), even in MANUAL.
        assertEquals(assistantText, mockTts.lastSpokenText)
    }
}
