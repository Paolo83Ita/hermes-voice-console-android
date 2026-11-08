package com.hermestower.voiceconsole.e2e

import androidx.compose.ui.test.onNodeWithContentDescription
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
 * GRAPH 4 directive §4 — ITALIAN-GATE-B: Chat, History and Archive.
 *
 * Segment scope: the chat surface (composer affordances, localized injection
 * dialog, honest timeline with the engine-truth row), the conversation History
 * screen and the Tower Archive with all three tabs (Conversations / Missions /
 * Artifacts) — every visible token classified against the allowlist.
 */
@RunWith(AndroidJUnit4::class)
class ItalianGateB_ChatHistoryArchiveTest {

    @get:Rule
    val compose: HermesComposeRule = hermesComposeRule()

    @Before
    fun baseline() {
        compose.freezeAnimationsForTest()
        compose.gateEnsureBaseline()
    }

    @Test
    fun italianGateB_chat_history_archive_italian() {
        val pool = ItalianGate.italianPool()
        val violations = mutableListOf<String>()
        try {
        compose.gateSwitchLocaleAndAwait("it")

        // ---- CHAT: localized injection dialog + honest timeline. ----
        compose.gateAwaitAnchor("HERMES")
        violations += compose.gateCollectViolations("Chat composer", pool)
        // R-9 completion: prove the LIVE console composition before the dialog
        // interaction (the composer hint only exists on the interactive console).
        compose.gateAwaitAnchor(ItalianGate.localizedString(R.string.composer_hint, "it"))
        // R-12: direct semantics-action dispatch (R-3 physics) — the gesture
        // click on the medallion row did not compose the dialog on the
        // software emulator (run 41890040629).
        // R-42 (runs 42420686751 + 42421301291 @ 979e5ef/23929f1 — dump +
        // product-code-proven): the Graph 4.10 §18 console hierarchy moved the
        // inject action INSIDE the "Altre azioni" DropdownMenu (ConsoleScreen.kt)
        // — inject_cd is the menu item's VISIBLE TEXT, not a contentDescription,
        // and the item composes only when the menu is expanded. The REAL
        // product path: "Altre azioni" → menu item (text) → the dialog; the
        // dialog anchor (cd_inject_title) still decides success honestly.
        check(
            compose.gateClickByText(ItalianGate.localizedString(R.string.mm_more_actions, "it")),
        ) { "more-actions control not found or without OnClick (it)" }
        check(
            compose.gateClickByText(ItalianGate.localizedString(R.string.inject_cd, "it")),
        ) { "inject menu item not found or without OnClick (it)" }
        compose.gateAwaitAnchor(ItalianGate.localizedString(R.string.cd_inject_title, "it"))
        violations += compose.gateCollectViolations("Chat inject dialog", pool)
        check(compose.gateActivateRow(ItalianGate.localizedString(R.string.cd_cancel, "it"))) {
            "dialog cancel button not found (it)"
        }
        compose.gateAwaitAnchor(ItalianGate.localizedString(R.string.composer_hint, "it"))
        violations += compose.gateCollectViolations("Chat timeline", pool)

        // ---- HISTORY via the REAL product path (drawer → Settings hub → row). ----
        compose.gateNavigate(R.string.settings_diag_history, R.string.history_title)
        violations += compose.gateCollectViolations("History", pool)

        // ---- ARCHIVE: all three tabs through the drawer row. ----
        compose.gateNavigate(R.string.dest_archive, R.string.archive_title)
        violations += compose.gateCollectViolations("Archive conversations tab", pool)
        check(
            compose.gateActivateRow(ItalianGate.localizedString(R.string.archive_tab_missions, "it")),
        ) { "missions tab row not found (it)" }
        compose.gateAwaitAnchor(ItalianGate.localizedString(R.string.archive_subtitle, "it"))
        violations += compose.gateCollectViolations("Archive missions tab", pool)
        check(
            compose.gateActivateRow(ItalianGate.localizedString(R.string.archive_tab_artifacts, "it")),
        ) { "artifacts tab row not found (it)" }
        compose.gateAwaitAnchor(ItalianGate.localizedString(R.string.archive_subtitle, "it"))
        violations += compose.gateCollectViolations("Archive artifacts tab", pool)

        // ---- Return to the safe route (Console) through the REAL UI. ----
        compose.gateOpenDrawer()
        check(
            compose.gateActivateRow(ItalianGate.localizedString(R.string.dest_console, "it")),
        ) { "console drawer row not found (it)" }
        compose.gateAwaitAnchor("HERMES")

        assertTrue(
            "ITALIAN-GATE-B violated (${violations.distinct().size}; first 10):\n" +
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
