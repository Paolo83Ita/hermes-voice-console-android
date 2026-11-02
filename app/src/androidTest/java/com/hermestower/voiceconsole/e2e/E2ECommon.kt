package com.hermestower.voiceconsole.e2e

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
 */
fun HermesComposeRule.openDrawerAndNavigate(destinationLabel: String, screenAnchor: String) {
    onNodeWithContentDescription("Open navigation drawer").performClick()
    waitUntil(15_000) { onAllNodesWithText(destinationLabel, substring = true).fetchSemanticsNodes().isNotEmpty() }
    onAllNodesWithText(destinationLabel, substring = true)[0].performClick()
    mainClock.advanceTimeUntil(20_000) {
        onAllNodesWithText(screenAnchor, substring = true).fetchSemanticsNodes().isNotEmpty()
    }
}

fun HermesComposeRule.waitForText(text: String, timeoutMs: Long = 20_000) {
    waitUntil(timeoutMs) { onAllNodesWithText(text, substring = true).fetchSemanticsNodes().isNotEmpty() }
}

/** Click/inspect the first node matching a text (used for toggle rows and cards). */
fun HermesComposeRule.onFirstNodeWithText(text: String) =
    onAllNodesWithText(text, substring = true)[0]
