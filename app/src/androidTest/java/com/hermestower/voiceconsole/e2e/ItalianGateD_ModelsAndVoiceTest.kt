package com.hermestower.voiceconsole.e2e

import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.hermestower.voiceconsole.R
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * GRAPH 4 directive §4 — ITALIAN-GATE-D: Models and Voice (Model Sanctum).
 *
 * Segment scope: the primary Models and Voice destination (bundled delivery
 * card, catalog rows, download states) and the expanded attribute sheet —
 * every visible token classified. The invariant identity "MODEL SANCTUM" is
 * part of the approved allowlist (audit §2).
 */
@RunWith(AndroidJUnit4::class)
class ItalianGateD_ModelsAndVoiceTest {

    @get:Rule
    val compose: HermesComposeRule = hermesComposeRule()

    @Before
    fun baseline() {
        compose.freezeAnimationsForTest()
        compose.gateEnsureBaseline()
    }

    @Test
    fun italianGateD_modelSanctum_italian() {
        val pool = ItalianGate.italianPool()
        val violations = mutableListOf<String>()
        try {
        compose.gateSwitchLocaleAndAwait("it")

        // ---- Model Sanctum through the drawer row. ----
        compose.gateNavigateTo(R.string.dest_models, "MODEL SANCTUM")
        violations += compose.gateCollectViolations("Models and Voice", pool)

        // ---- Expanded attribute sheet through the REAL details control. ----
        check(
            compose.gateActivateRow(ItalianGate.localizedString(R.string.mm_details, "it")),
        ) { "details control not found (it)" }
        compose.gateAwaitAnchor(ItalianGate.localizedString(R.string.mm_attr_type, "it"))
        violations += compose.gateCollectViolations("Models attribute sheet", pool)

        // ---- Return to the safe route (Console) through the REAL UI. ----
        compose.gateOpenDrawer()
        check(
            compose.gateActivateRow(ItalianGate.localizedString(R.string.dest_console, "it")),
        ) { "console drawer row not found (it)" }
        compose.gateAwaitAnchor("HERMES")

        assertTrue(
            "ITALIAN-GATE-D violated (${violations.distinct().size}; first 10):\n" +
                violations.distinct().take(10).joinToString("\n"),
            violations.distinct().isEmpty(),
        )
        } finally {
            // Restore the documented English baseline for the NEXT instrumentation
            // invocation (the regression suite asserts the en anchors).
            runCatching { compose.gateSwitchLocaleAndAwait("en") }
        }
    }
}
