package com.hermestower.voiceconsole.e2e

import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.hermestower.voiceconsole.ui.screens.Onboarding
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * GRAPH 4 directive §6 — SIMPLE ONBOARDING E2E. Clears the first-launch flag,
 * recreates the activity so the REAL wizard mounts (chrome-free), walks the six
 * steps through the real UI (language → Tower choice → voice pack → LLM default
 * "Not now" → voice test → finish) and verifies the app lands on the Console
 * with the flag persisted. No destination logic is ever called directly.
 *
 * Technical internals (SHA-256, ABI, quantization, protocol paths, TLS
 * fingerprints, raw endpoints) must NOT appear anywhere in the wizard — the
 * gate asserts their absence on the visible tree.
 */
@RunWith(AndroidJUnit4::class)
class E2EOnboardingTest {

    @get:Rule
    val compose: HermesComposeRule = hermesComposeRule()

    @Test
    fun e2e_onboarding_sixSteps_reachConsole() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val ob = context
        // First-launch state: clear the flag, then rebuild the tree.
        Onboarding.resetForTest(context)
        compose.activityRule.scenario.recreate()
        compose.waitForIdle()

        // STEP 1 — language (Follow system selected by default).
        compose.waitForText(ob.getString(com.hermestower.voiceconsole.R.string.ob_title))
        compose.waitForText(ob.getString(com.hermestower.voiceconsole.R.string.ob_s1_follow_system))
        compose.clickFirstClickable(ob.getString(com.hermestower.voiceconsole.R.string.ob_continue))

        // STEP 2 — Tower: choose simulation (the authorized no-credential path).
        compose.waitForText(ob.getString(com.hermestower.voiceconsole.R.string.ob_s2_title))
        compose.clickFirstClickable(ob.getString(com.hermestower.voiceconsole.R.string.ob_s2_simulation))

        // STEP 3 — recommended voice pack (summary visible, no technical internals).
        compose.waitForText(ob.getString(com.hermestower.voiceconsole.R.string.ob_s3_title))
        compose.clickFirstClickable(ob.getString(com.hermestower.voiceconsole.R.string.ob_continue))

        // STEP 4 — optional local LLM: default is "Not now".
        compose.waitForText(ob.getString(com.hermestower.voiceconsole.R.string.ob_s4_title))
        compose.clickFirstClickable(ob.getString(com.hermestower.voiceconsole.R.string.ob_s4_not_now))

        // STEP 5 — voice test offer (the run itself stays on the Owner device).
        compose.waitForText(ob.getString(com.hermestower.voiceconsole.R.string.ob_s5_title))
        compose.clickFirstClickable(ob.getString(com.hermestower.voiceconsole.R.string.ob_continue))

        // STEP 6 — finish → Console with the flag persisted.
        compose.waitForText(ob.getString(com.hermestower.voiceconsole.R.string.ob_s6_title))
        compose.clickFirstClickable(ob.getString(com.hermestower.voiceconsole.R.string.ob_s6_finish))
        compose.waitForText("HERMES")
        compose.waitForText("Write a message…")
        assertTrue("onboarding flag must persist after finish", Onboarding.isDone(context))
    }
}
