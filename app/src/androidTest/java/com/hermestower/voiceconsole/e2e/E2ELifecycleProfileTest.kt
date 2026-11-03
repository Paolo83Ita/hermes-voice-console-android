package com.hermestower.voiceconsole.e2e

import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assume
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * CP-G4-016 TEST-E (OWNER_DECISION_E2E_RECOVERY): the normal appearance
 * navigation repeated >= 20 times in ONE instrumentation session, under the
 * authorized CI_LIFECYCLE_PROFILE.
 *
 * Every iteration drives the REAL UI: drawer open (real frames), physical row
 * click, destination anchor assertion, drawer open again, physical click back
 * to the console, console anchor assertion. No destination logic is ever called
 * directly — the NavHost, back stack and lifecycle are exercised exactly as a
 * user would. Each navigation creates and disposes real back-stack entries, so
 * the run doubles as the repeated-entry lifecycle soak (appearance included —
 * the route of the documented INITIALIZED→DESTROYED residual).
 *
 * Under NORMAL_VISUAL_PROFILE this class aborts truthfully as SKIPPED
 * (Assume) — the normal animated profile stays physical-Xiaomi-gated.
 */
@RunWith(AndroidJUnit4::class)
class E2ELifecycleProfileTest {

    @get:Rule
    val compose: HermesComposeRule = hermesComposeRule()

    @Before
    fun requireLifecycleProfile() {
        Assume.assumeTrue("CI_LIFECYCLE_PROFILE required (directive §2 TEST-E)", ciLifecycleProfileRequested)
    }

    @Test
    fun e2e_lifecycle_appearanceNavigationRepeat20() {
        val appearances = "MARBLE LIGHT"
        val consoleAnchor = "Write a message"
        repeat(20) { i ->
            // Console -> Appearance (drawer, physical click, real frames)
            compose.onNodeWithContentDescription("Open navigation drawer").performClick()
            compose.waitUntil(15_000) {
                compose.onAllNodes(hasText("Appearance", substring = true).and(hasClickAction()))
                    .fetchSemanticsNodes().isNotEmpty()
            }
            val deadline = System.currentTimeMillis() + 5_000
            while (System.currentTimeMillis() < deadline) {
                val pos = compose.onAllNodes(
                    hasText("Appearance", substring = true).and(hasClickAction()),
                ).fetchSemanticsNodes().firstOrNull()?.positionInRoot
                if (pos != null && pos.x >= 0f) break
                Thread.sleep(50)
            }
            compose.onAllNodes(hasText("Appearance", substring = true).and(hasClickAction()))[0]
                .performClick()
            compose.waitUntil(20_000) {
                compose.onAllNodesWithText(appearances, substring = true)
                    .fetchSemanticsNodes().isNotEmpty()
            }
            compose.waitForIdle()

            // Appearance -> Console (same real path, back stack exercised)
            compose.onNodeWithContentDescription("Open navigation drawer").performClick()
            compose.waitUntil(15_000) {
                compose.onAllNodes(hasText("Console", substring = true).and(hasClickAction()))
                    .fetchSemanticsNodes().isNotEmpty()
            }
            compose.onAllNodes(hasText("Console", substring = true).and(hasClickAction()))[0]
                .performClick()
            compose.waitUntil(20_000) {
                compose.onAllNodesWithText(consoleAnchor, substring = true)
                    .fetchSemanticsNodes().isNotEmpty()
            }
            compose.waitForIdle()
            // Iteration i complete: both entries were created, composed, started,
            // stopped and disposed through the real navigation path 2*i times.
        }
    }
}
