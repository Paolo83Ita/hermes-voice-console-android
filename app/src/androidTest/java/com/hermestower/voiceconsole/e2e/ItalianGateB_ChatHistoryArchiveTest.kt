package com.hermestower.voiceconsole.e2e

import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
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
        compose.onNodeWithContentDescription(ItalianGate.localizedString(R.string.inject_cd, "it"))
            .performClick()
        compose.gateAwaitAnchor(ItalianGate.localizedString(R.string.cd_inject_title, "it"))
        violations += compose.gateCollectViolations("Chat inject dialog", pool)
        compose.onNodeWithText(ItalianGate.localizedString(R.string.cd_cancel, "it"))
            .performClick()
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
