package com.hermestower.voiceconsole.e2e

import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * GRAPH 4 directive §4 — ITALIAN-GATE-F: Diagnostics, Self-Test and About.
 *
 * Segment scope: the Diagnostics screen, the Self-Test Center, the Local
 * Intelligence surface and About and Safety — reached ONLY through the REAL
 * product path (drawer → Settings hub → DIAGNOSTICS rows). Typed diagnostic
 * codes stay on the approved allowlist.
 */
@RunWith(AndroidJUnit4::class)
class ItalianGateF_DiagnosticsSelfTestAboutTest {

    @get:Rule
    val compose: HermesComposeRule = hermesComposeRule()

    @Before
    fun baseline() {
        compose.freezeAnimationsForTest()
        compose.gateEnsureBaseline()
    }

    @Test
    fun italianGateF_diagnostics_selftest_about_italian() {
        val pool = ItalianGate.italianPool()
        val violations = mutableListOf<String>()
        try {
        compose.gateSwitchLocaleAndAwait("it")

        // ---- Diagnostics (engine health). ----
        compose.gateNavigate(R.string.settings_diag_engine, R.string.diag_title)
        violations += compose.gateCollectViolations("Diagnostics", pool)

        // ---- Self-Test Center. ----
        compose.gateNavigate(R.string.settings_diag_selftest, R.string.st_title)
        violations += compose.gateCollectViolations("Self-Test Center", pool)

        // ---- Local Intelligence. ----
        compose.gateNavigate(R.string.settings_diag_intelligence, R.string.li_title)
        violations += compose.gateCollectViolations("Local Intelligence", pool)

        // ---- About and Safety. ----
        compose.gateNavigate(R.string.settings_diag_about, R.string.about_title)
        violations += compose.gateCollectViolations("About and Safety", pool)

        // ---- Return to the safe route (Console) through the REAL UI. ----
        compose.gateOpenDrawer()
        check(
            compose.gateActivateRow(ItalianGate.localizedString(R.string.dest_console, "it")),
        ) { "console drawer row not found (it)" }
        compose.gateAwaitAnchor("HERMES")

        assertTrue(
            "ITALIAN-GATE-F violated (${violations.distinct().size}; first 10):\n" +
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
