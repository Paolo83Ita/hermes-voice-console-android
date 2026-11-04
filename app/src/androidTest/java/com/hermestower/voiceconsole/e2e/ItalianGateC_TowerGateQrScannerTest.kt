package com.hermestower.voiceconsole.e2e

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.hermestower.voiceconsole.R
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * GRAPH 4 directive §4 — ITALIAN-GATE-C: Tower Gate and QR scanner.
 *
 * Segment scope: the Tower Gate (Connection screen), the Tower Connection
 * Studio and the QR scanner. The scanner is a FULL-SCREEN route without the
 * drawer (ledger R-4): the segment enters it through the REAL studio control,
 * collects its localized semantics, leaves it through its OWN Back path and
 * ASSERTS the return to the Tower Gate before any further navigation.
 */
@RunWith(AndroidJUnit4::class)
class ItalianGateC_TowerGateQrScannerTest {

    @get:Rule
    val compose: HermesComposeRule = hermesComposeRule()

    @Before
    fun baseline() {
        compose.freezeAnimationsForTest()
        compose.gateEnsureBaseline()
    }

    @Test
    fun italianGateC_towerGate_scanner_back_italian() {
        val pool = ItalianGate.italianPool()
        val violations = mutableListOf<String>()
        try {
        compose.gateSwitchLocaleAndAwait("it")

        // ---- Tower Gate (Connection) through the drawer row. ----
        compose.gateNavigate(R.string.dest_connect, R.string.connection_title)
        violations += compose.gateCollectViolations("Tower Gate (connection)", pool)

        // ---- Tower Connection Studio through the REAL control. ----
        check(
            compose.gateActivateRow(ItalianGate.localizedString(R.string.connection_open_studio, "it")),
        ) { "open-studio control not found (it)" }
        compose.gateAwaitAnchor(ItalianGate.localizedString(R.string.ts_title, "it"))
        violations += compose.gateCollectViolations("Tower Connection Studio", pool)

        // ---- QR scanner (full-screen route). ----
        check(
            compose.gateActivateRow(ItalianGate.localizedString(R.string.ts_action_scan_qr, "it")),
        ) { "scan-QR control not found (it)" }
        compose.gateAwaitAnchor(ItalianGate.localizedString(R.string.qr_title, "it"))
        violations += compose.gateCollectViolations("QR scanner", pool)

        // ---- EXPLICIT Back navigation test (ledger R-4): the scanner has no
        //      drawer — the only exit is its own back path, and the segment
        //      PROVES the return route before continuing. ----
        compose.gateLeaveScannerBack(ItalianGate.localizedString(R.string.ts_title, "it"))
        violations += compose.gateCollectViolations("Tower Connection Studio (after scanner back)", pool)

        // ---- Return to the safe route (Console) through the REAL UI. ----
        compose.gateOpenDrawer()
        check(
            compose.gateActivateRow(ItalianGate.localizedString(R.string.dest_console, "it")),
        ) { "console drawer row not found (it)" }
        compose.gateAwaitAnchor("HERMES")

        assertTrue(
            "ITALIAN-GATE-C violated (${violations.distinct().size}; first 10):\n" +
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
