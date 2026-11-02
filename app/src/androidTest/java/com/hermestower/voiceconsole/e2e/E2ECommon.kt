package com.hermestower.voiceconsole.e2e

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
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
 * CP-G4-007 E2E-ROBUSTNESS — the frozen-clock navigation contract.
 *
 * Physics of the failure class (proven by runs 41459906172/41461749711/
 * 41470976708/41485512432/41503065076/41514356403 + the run-41514356403
 * semantics-tree dump):
 *  1. `freezeAnimationsForTest()` (needed: infinite Oracle Core animation)
 *     freezes the compose animation clock;
 *  2. the drawer-open and NavHost enter transitions advance ONLY on that clock
 *     — without frames the drawer sheet stays at its CLOSED offset (off-screen)
 *     and the destination screen never composes;
 *  3. touch injection at OFF-SCREEN window coordinates is silently dropped —
 *     the semantics exist but the physics does not (the visible-texts dump of
 *     run 41514356403 shows console-only content after a "successful" row click).
 *
 * The repair (Owner §3: virtual time, semantic selectors, accessible click
 * action, performSemanticsAction where appropriate, physical-click assertion
 * preserved, no logic bypass):
 *  - pump BOUNDED VIRTUAL TIME so the drawer-open animation actually places the
 *    row on-screen, then use the PHYSICAL click (rule 4);
 *  - if the row is still off-screen after the bound, activate it via
 *    performSemanticsAction(SemanticsActions.OnClick) — the accessible
 *    activation path a screen reader uses (rule 3), never a logic bypass;
 *  - pump until the destination anchor composes, then advance a settle window
 *    so layout/placement is complete before any further physical click;
 *  - all post-click state waits pump both clocks (waitForText/pumpUntil).
 */
fun HermesComposeRule.openDrawerAndNavigate(destinationLabel: String, screenAnchor: String) {
    onNodeWithContentDescription("Open navigation drawer").performClick()
    waitUntil(15_000) { drawerRowNode(destinationLabel) != null }

    // (a) drawer-open animation: pump until the row is physically on-screen
    var advanced = 0L
    var onScreen = false
    while (advanced < 5_000L) {
        mainClock.advanceTimeBy(100)
        advanced += 100
        onScreen = drawerRowPosition(destinationLabel)?.let { it.x >= 0f } ?: false
        if (onScreen) break
    }

    // (b) activate: physical click when the control is physically clickable,
    //     accessible semantics activation otherwise (Owner §3.3)
    val row = drawerRowNode(destinationLabel)
    if (row == null) error("drawer row '$destinationLabel' disappeared before activation")
    if (onScreen) {
        onAllNodes(hasText(destinationLabel, substring = true).and(hasClickAction()))[0].performClick()
    } else {
        // accessible activation path (screen-reader equivalent), never a logic bypass
        val clickAction = row.config[SemanticsActions.OnClick]
        if (clickAction?.action != null) clickAction.action!!.invoke()
    }

    // (c) destination screen: pump until the anchor composes
    var settled = 0L
    while (settled < 20_000L) {
        mainClock.advanceTimeBy(100)
        settled += 100
        if (onAllNodesWithText(screenAnchor, substring = true).fetchSemanticsNodes().isNotEmpty()) {
            // (d) settle window: transition + layout/placement complete before
            // any further PHYSICAL click on the new screen
            mainClock.advanceTimeBy(600)
            return
        }
    }
    error(
        "destination screen anchor '$screenAnchor' did not compose within 20 s of virtual time; " +
            "visible texts: " + visibleTextsSample(),
    )
}

private fun HermesComposeRule.drawerRowNode(label: String) =
    onAllNodes(hasText(label, substring = true).and(hasClickAction()))
        .fetchSemanticsNodes()
        .firstOrNull()

private fun HermesComposeRule.drawerRowPosition(label: String): Offset? =
    try {
        drawerRowNode(label)?.positionInRoot
    } catch (t: Throwable) {
        null
    }

/**
 * Bounded dual-clock wait: checks the condition, then advances 100 ms of
 * VIRTUAL time and yields 20 ms of real time (coroutine-driven state), until
 * the condition holds or the deadline expires. The controlled replacement for
 * frozen-clock waitUntil — transitions, layout and effects progress while
 * state flows are polled (Owner §3: bounded waitUntil + virtual time).
 */
fun HermesComposeRule.pumpUntil(timeoutMs: Long = 20_000, condition: () -> Boolean) {
    val deadline = System.currentTimeMillis() + timeoutMs
    while (System.currentTimeMillis() < deadline) {
        if (condition()) return
        mainClock.advanceTimeBy(100)
        Thread.sleep(20)
    }
    error("condition not met within ${timeoutMs} ms; visible texts: ${visibleTextsSample()}")
}

fun HermesComposeRule.waitForText(text: String, timeoutMs: Long = 20_000) {
    pumpUntil(timeoutMs) { onAllNodesWithText(text, substring = true).fetchSemanticsNodes().isNotEmpty() }
}

/** Bounded diagnostics for anchor-timeout triage (labels only, no secrets). */
private fun HermesComposeRule.visibleTextsSample(): String =
    try {
        onAllNodesWithText("", substring = true)
            .fetchSemanticsNodes()
            .asSequence()
            .map { node ->
                node.config.get(SemanticsProperties.Text).joinToString(" ") { t -> t.text }
            }
            .filter { it.isNotBlank() }
            .take(25)
            .toList()
            .toString()
    } catch (t: Throwable) {
        "<tree unavailable: ${t.javaClass.simpleName}>"
    }

/** Click the first CLICKABLE node matching a text (Owner §3 semantic selector). */
fun HermesComposeRule.clickFirstClickable(text: String) {
    onAllNodes(hasText(text, substring = true).and(hasClickAction()))[0].performClick()
}

/** Click/inspect the first node matching a text (used for toggle rows and cards). */
fun HermesComposeRule.onFirstNodeWithText(text: String) =
    onAllNodesWithText(text, substring = true)[0]
