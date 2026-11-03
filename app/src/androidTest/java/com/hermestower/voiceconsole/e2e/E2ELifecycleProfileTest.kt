package com.hermestower.voiceconsole.e2e

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
 * Every iteration drives the REAL UI through the shared harness path
 * (drawer open, real frames, physical drawer clicks, Settings-hub rows where
 * the product moved them — CP-G4-017): Console → Appearance → Console. No
 * destination logic is ever called directly — the NavHost, back stack and
 * lifecycle are exercised exactly as a user would. Each navigation creates and
 * disposes real back-stack entries, so the run doubles as the repeated-entry
 * lifecycle soak (appearance included — the route of the documented
 * INITIALIZED→DESTROYED residual).
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
        repeat(20) { i ->
            // Console -> Appearance (real product path, physical clicks)
            compose.openDrawerAndNavigate("Appearance", "MARBLE LIGHT")
            compose.waitForIdle()

            // Appearance -> Console (back stack exercised both ways)
            compose.openDrawerAndNavigate("Voice Console", "Write a message")
            compose.waitForIdle()
            // Iteration i complete: both entries were created, composed, started,
            // stopped and disposed through the real navigation path.
        }
    }
}
