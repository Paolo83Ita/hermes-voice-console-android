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

    private fun buildDriver(testDispatcher: UnconfinedTestDispatcher): ScriptedConsoleDriver {
        scope = CoroutineScope(SupervisorJob() + testDispatcher)
        bridge = MockTowerBridge(DeterministicTowerSimulator(), testDispatcher)
        return ScriptedConsoleDriver(
            scope = scope,
            store = store,
            router = RuleBasedIntentRouter(),
            localEngine = MockLocalConversationEngine(),
            bridge = bridge,
            tts = MockTextToSpeechEngine(),
            events = events,
            session = DefaultSessionController(),
            statusProvider = { fixedStatus },
        )
    }

    private fun kotlinx.coroutines.test.TestScope.bootDriver(): ScriptedConsoleDriver {
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
}
