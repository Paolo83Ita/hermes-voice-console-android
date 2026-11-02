package com.hermestower.voiceconsole.e2e

import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.rules.ActivityScenarioRule
import com.hermestower.voiceconsole.app.AppGraph
import com.hermestower.voiceconsole.app.AppGraphHolder
import com.hermestower.voiceconsole.ui.MainActivity
import androidx.test.platform.app.InstrumentationRegistry

typealias HermesComposeRule = AndroidComposeTestRule<ActivityScenarioRule<MainActivity>, MainActivity>

/** Freeze the compose clock: the console ships infinite animations, which would
 *  otherwise keep the test rule from ever reaching idle. State-driven waitUntil
 *  conditions still work (they read StateFlows, not the clock). */
fun HermesComposeRule.freezeAnimationsForTest() {
    mainClock.autoAdvance = false
}

/** Shared E2E helpers: graph access and drawer navigation. */
object E2E {
    fun graph(): AppGraph = AppGraphHolder.graph(InstrumentationRegistry.getInstrumentation().targetContext)
}

/**
 * Open the drawer and navigate to [destinationLabel], then REQUIRE the
 * destination screen to actually compose by waiting for [screenAnchor] —
 * a string that exists only on the destination screen (never on the drawer).
 *
 * CP-G4-007 E2E-ROBUSTNESS root-cause repair (runs 41459906172 / 41461749711 /
 * 41470976708): the suite freezes the animation clock (infinite Oracle Core
 * animation), and the NavHost enter transition is animation-clock-driven —
 * with a frozen clock the destination screen never composed, so every
 * post-navigation assertion timed out deterministically (12 of the 14 E2E
 * failures). The repair advances BOUNDED VIRTUAL TIME until the anchor exists
 * (Owner §3: virtual time + UI-hierarchy assertion; no sleeps, no logic
 * bypass). [screenAnchor] is a required parameter so a caller cannot
 * accidentally assert on the drawer item itself.
 *
 * RUN 41503065076 repairs (attempt 3, evidence-driven):
 *  1. the drawer-row click now uses the SEMANTIC selector
 *     `hasText(label) AND hasClickAction()` — a bare text node elsewhere on
 *     screen (e.g. the console's engine-truth row) can precede the drawer row
 *     in tree order and had no click action (Owner §3: stable semantic
 *     selectors + accessible click action, no bypass);
 *  2. the anchor query must not run inside mainClock.advanceTimeUntil (its
 *     condition executes on the MAIN thread where Espresso-linked sync throws
 *     — run 41485512432): bounded advanceTimeBy steps with the query BETWEEN
 *     steps (200 × 100 ms = 20 s virtual bound).
 */
fun HermesComposeRule.openDrawerAndNavigate(destinationLabel: String, screenAnchor: String) {
    onNodeWithContentDescription("Open navigation drawer").performClick()
    waitUntil(15_000) { onAllNodesWithText(destinationLabel, substring = true).fetchSemanticsNodes().isNotEmpty() }
    onAllNodes(hasText(destinationLabel, substring = true).and(hasClickAction()))[0].performClick()
    var advanced = 0L
    while (advanced < 20_000L) {
        mainClock.advanceTimeBy(100)
        advanced += 100
        if (onAllNodesWithText(screenAnchor, substring = true).fetchSemanticsNodes().isNotEmpty()) return
    }
    error(
        "destination screen anchor '$screenAnchor' did not compose within 20 s of virtual time; " +
            "visible texts: " + visibleTextsSample(),
    )
}

/** Bounded diagnostics for anchor-timeout triage (labels only, no secrets). */
private fun HermesComposeRule.visibleTextsSample(): String =
    try {
        onAllNodesWithText("", substring = true)
            .fetchSemanticsNodes()
            .asSequence()
            .map { node ->
                node.config.get(androidx.compose.ui.semantics.SemanticsProperties.Text)
                    .joinToString(" ") { t -> t.text }
            }
            .filter { it.isNotBlank() }
            .take(25)
            .toList()
            .toString()
    } catch (t: Throwable) {
        "<tree unavailable: ${t.javaClass.simpleName}>"
    }

fun HermesComposeRule.waitForText(text: String, timeoutMs: Long = 20_000) {
    waitUntil(timeoutMs) { onAllNodesWithText(text, substring = true).fetchSemanticsNodes().isNotEmpty() }
}

/** Click the first CLICKABLE node matching a text (Owner §3 semantic selector;
 *  exact-match + decorated source strings like "…Studio →" stay out of scope). */
fun HermesComposeRule.clickFirstClickable(text: String) {
    onAllNodes(hasText(text, substring = true).and(hasClickAction()))[0].performClick()
}

/** Click/inspect the first node matching a text (used for toggle rows and cards). */
fun HermesComposeRule.onFirstNodeWithText(text: String) =
    onAllNodesWithText(text, substring = true)[0]
