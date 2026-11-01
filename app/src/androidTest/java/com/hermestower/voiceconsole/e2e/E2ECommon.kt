package com.hermestower.voiceconsole.e2e

import androidx.compose.ui.test.AndroidComposeTestRule
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

/** Shared E2E helpers: graph access and drawer navigation. */
object E2E {
    fun graph(): AppGraph = AppGraphHolder.graph(InstrumentationRegistry.getInstrumentation().targetContext)
}

fun HermesComposeRule.openDrawerAndNavigate(destination: String) {
    onNodeWithContentDescription("Open navigation drawer").performClick()
    waitUntil(15_000) { onAllNodesWithText(destination, substring = true).fetchSemanticsNodes().isNotEmpty() }
    onAllNodesWithText(destination, substring = true)[0].performClick()
}

fun HermesComposeRule.waitForText(text: String, timeoutMs: Long = 20_000) {
    waitUntil(timeoutMs) { onAllNodesWithText(text, substring = true).fetchSemanticsNodes().isNotEmpty() }
}

/** Click/inspect the first node matching a text (used for toggle rows and cards). */
fun HermesComposeRule.onFirstNodeWithText(text: String) =
    onAllNodesWithText(text, substring = true)[0]
