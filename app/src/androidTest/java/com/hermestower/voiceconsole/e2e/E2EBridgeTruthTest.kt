package com.hermestower.voiceconsole.e2e

import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performClick
import androidx.test.filters.LargeTest
import com.hermestower.voiceconsole.domain.BridgeModeAuthority
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * GRAPH 4.14 §24 — BRIDGE TRUTH E2E (real UI actions; no route assignment).
 *
 * The console boots on the mock endpoint (mock://tower.simulator). The
 * indicator MUST read SIMULAZIONE/SIMULATION with the amber/neutral treatment —
 * NEVER a real-connected label ("BRIDGE OK", "TOWER CONNESSA", "TOWER
 * OPERATIVA") — and the truth panel MUST state that no real Tower is
 * connected. Locale-tolerant: the fleet device may run en or it resources.
 */
@LargeTest
class E2EBridgeTruthTest {

    @get:Rule
    val compose: HermesComposeRule = hermesComposeRule()

    private fun graph() = E2E.graph()

    @Before
    fun freezeAnimations() {
        compose.freezeAnimationsForTest()
    }

    private fun mockLabelRes() =
        listOf("SIMULATION", "SIMULAZIONE")

    @Test
    fun e2e_bridgeTruth_mockIndicatorAndPanel() {
        // 1. The boot connection truth: mock endpoint → MOCK role (authority read-back).
        compose.waitUntil(15_000) {
            val conn = graph().store.connection.value
            conn.endpointLabel.startsWith("mock://")
        }
        val conn = graph().store.connection.value
        val truth = BridgeModeAuthority.resolve(conn)
        assertTrue("boot endpoint must be the mock simulator", conn.endpointLabel.startsWith("mock://"))
        assertTrue("authority must resolve the MOCK role", truth.isMock)

        // 2. The console pill shows the mock label (locale-tolerant).
        compose.waitUntil(15_000) {
            mockLabelRes().any { label ->
                compose.onAllNodesWithText(label, substring = false).fetchSemanticsNodes().isNotEmpty()
            }
        }

        // 3. The forbidden real-connection labels NEVER exist on the mock console.
        for (forbidden in listOf("BRIDGE OK", "TOWER CONNESSA", "TOWER OPERATIVA")) {
            compose.onAllNodesWithText(forbidden, substring = false)
                .fetchSemanticsNodes()
                .let { nodes -> assertTrue("forbidden label $forbidden must not render", nodes.isEmpty()) }
        }

        // 4. Tap the pill (REAL UI action) → the truth panel opens.
        val label = mockLabelRes().first { label ->
            compose.onAllNodesWithText(label, substring = false).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onFirstNodeWithText(label).performClick()
        compose.waitUntil(10_000) {
            val titles = listOf("Bridge truth", "Verità del bridge")
            titles.any { t ->
                compose.onAllNodesWithText(t, substring = false).fetchSemanticsNodes().isNotEmpty()
            }
        }

        // 5. The mock truth note is visible (no real Tower connected).
        val notes = listOf(
            "No real Hermes Tower is connected. Tower responses are simulated.",
            "Nessuna Hermes Tower reale è collegata. Le risposte Tower sono simulate.",
        )
        assertTrue(
            "the truth panel must state that no real Tower is connected",
            notes.any { n ->
                compose.onAllNodesWithText(n, substring = false).fetchSemanticsNodes().isNotEmpty()
            },
        )

        // 6. The endpoint class row reports the MOCK_SIMULATOR class (locale-free token).
        assertTrue(
            "endpoint class row must show MOCK_SIMULATOR",
            compose.onAllNodesWithText("MOCK_SIMULATOR", substring = false)
                .fetchSemanticsNodes().isNotEmpty(),
        )

        // 7. No real remote conduit: the authority reports capability negotiation false.
        assertFalse(truth.capabilityNegotiated)
        assertEquals("MOCK_SIMULATOR", truth.endpointClass)
    }
}
