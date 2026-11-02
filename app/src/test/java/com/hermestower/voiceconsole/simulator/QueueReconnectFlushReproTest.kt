package com.hermestower.voiceconsole.simulator

import com.hermestower.voiceconsole.bridge.BridgeFault
import com.hermestower.voiceconsole.bridge.DeterministicTowerSimulator
import com.hermestower.voiceconsole.bridge.MockTowerBridge
import com.hermestower.voiceconsole.core.routing.RuleBasedIntentRouter
import com.hermestower.voiceconsole.core.state.ConnectionStatus
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
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * CP-G4-007 E2E-ROBUSTNESS — reproduction of the E2E e2e_19_20 failure
 * ("queued request was never validated and delivered after reconnect",
 * deterministic on runs 41459906172 / 41461749711 / 41470976708) on virtual
 * time, against the REAL driver + bridge + router + state machine.
 */
class QueueReconnectFlushReproTest {

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

    private fun dumpEvents(): String =
        events.recent(200).joinToString("\n") { "${it.type}: ${it.detail}" }

    @Test
    fun `queued turn is delivered after reconnect flush (e2e_19_20 reproduction)`() = runTest {
        val d = buildDriver(UnconfinedTestDispatcher(testScheduler))
        store.transition(ConsoleState.IDLE)
        d.start()
        bridge.handshake()
        advanceUntilIdle()
        assertEquals(ConnectionStatus.BridgeStatus.CONNECTED, store.connection.value.status)

        // --- e2e_19_20 sequence ---
        d.setFault(BridgeFault.DISCONNECT_AT_ACK)
        advanceUntilIdle()
        assertEquals(ConnectionStatus.BridgeStatus.DISCONNECTED, store.connection.value.status)

        d.submitText("tell me a joke")
        advanceUntilIdle()

        val queued = store.turns.value.lastOrNull { it.role == ConversationTurn.Role.OWNER }
        assertNotNull("no owner turn created", queued)
        assertEquals(
            "turn should be QUEUED while offline (got note=${queued!!.note})",
            ConversationTurn.Delivery.QUEUED, queued.delivery,
        )

        d.clearFault()
        advanceUntilIdle()

        assertEquals(
            "bridge should be CONNECTED after clearFault handshake",
            ConnectionStatus.BridgeStatus.CONNECTED, store.connection.value.status,
        )
        val ownerAfter = store.turns.value.last { it.role == ConversationTurn.Role.OWNER }
        assertEquals(
            "OWNER turn should be DELIVERED after reconnect flush (note=${ownerAfter.note})",
            ConversationTurn.Delivery.DELIVERED, ownerAfter.delivery,
        )
        val hermesFlushed = store.turns.value.lastOrNull {
            it.role == ConversationTurn.Role.HERMES && it.source == ConversationTurn.Source.TOWER
        }
        assertNotNull("no HERMES/TOWER turn was appended by the flush", hermesFlushed)
        println("---- driver events ----\n${dumpEvents()}")
    }
}
