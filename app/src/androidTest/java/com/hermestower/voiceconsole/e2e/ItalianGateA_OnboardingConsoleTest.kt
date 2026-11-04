package com.hermestower.voiceconsole.e2e

import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.hermestower.voiceconsole.R
import com.hermestower.voiceconsole.ui.screens.Onboarding
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * GRAPH 4 directive §4 — ITALIAN-GATE-A: Onboarding and Console.
 *
 * Segment scope: the six-step wizard renders fully in Italian through the REAL
 * UI walk (language → Tower simulation → voice pack → LLM "Not now" → voice
 * test offer → finish) and the Console (top bar, truth row, composer affordances,
 * text-injection dialog) carries no ordinary English visible text.
 *
 * The technical-internals prohibition (directive §5) is asserted here too: no
 * SHA-256, ABI, quantization, protocol-path, TLS-fingerprint, raw-endpoint or
 * debug-internal token may appear in the wizard (the classification vocabulary
 * covers them: hashes/identifiers are technical-shaped, ordinary English is
 * banned).
 */
@RunWith(AndroidJUnit4::class)
class ItalianGateA_OnboardingConsoleTest {

    @get:Rule
    val compose: HermesComposeRule = hermesComposeRule()

    @Before
    fun baseline() {
        compose.freezeAnimationsForTest()
        compose.gateEnsureBaseline()
    }

    @Test
    fun italianGateA1_onboarding_sixSteps_italian() {
        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        val pool = ItalianGate.italianPool()
        val violations = mutableListOf<String>()
        try {
            // Fresh wizard, English baseline, then the system-driven relaunch to Italian.
            Onboarding.resetForTest(ctx)
            compose.activityRule.scenario.recreate()
            // Wait for the wizard to compose BEFORE the locale switch (the switch's
            // wizard-detection samples the tree once at entry — ledger R-1 discipline).
            compose.gateAwaitAnchor(
                ItalianGate.localizedString(R.string.ob_title, "en"),
                ItalianGate.localizedString(R.string.ob_title, "it"),
            )
            compose.gateSwitchLocaleAndAwait("it")

            // STEP 1 — language.
            compose.gateAwaitAnchor(ItalianGate.localizedString(R.string.ob_s1_title, "it"))
            violations += compose.gateCollectViolations("Onboarding step 1 (language)", pool)
            compose.onNodeWithText(ItalianGate.localizedString(R.string.ob_continue, "it"))
                .performClick()

            // STEP 2 — Tower connect (simulation, the authorized no-credential path).
            compose.gateAwaitAnchor(ItalianGate.localizedString(R.string.ob_s2_title, "it"))
            violations += compose.gateCollectViolations("Onboarding step 2 (tower)", pool)
            compose.onNodeWithText(ItalianGate.localizedString(R.string.ob_s2_simulation, "it"))
                .performClick()

            // STEP 3 — recommended voice pack (summary, install offered, skipped here).
            compose.gateAwaitAnchor(ItalianGate.localizedString(R.string.ob_s3_title, "it"))
            violations += compose.gateCollectViolations("Onboarding step 3 (voice pack)", pool)
            compose.onNodeWithText(ItalianGate.localizedString(R.string.ob_continue, "it"))
                .performClick()

            // STEP 4 — optional local LLM: default "Not now" (directive §6).
            compose.gateAwaitAnchor(ItalianGate.localizedString(R.string.ob_s4_title, "it"))
            violations += compose.gateCollectViolations("Onboarding step 4 (local LLM)", pool)
            compose.onNodeWithText(ItalianGate.localizedString(R.string.ob_s4_not_now, "it"))
                .performClick()

            // STEP 5 — microphone/STT/voice test offer (the run stays on the device).
            compose.gateAwaitAnchor(ItalianGate.localizedString(R.string.ob_s5_title, "it"))
            violations += compose.gateCollectViolations("Onboarding step 5 (voice test)", pool)
            compose.onNodeWithText(ItalianGate.localizedString(R.string.ob_continue, "it"))
                .performClick()

            // STEP 6 — finish → the REAL Console.
            compose.gateAwaitAnchor(ItalianGate.localizedString(R.string.ob_s6_title, "it"))
            violations += compose.gateCollectViolations("Onboarding step 6 (finish)", pool)
            compose.onNodeWithText(ItalianGate.localizedString(R.string.ob_s6_finish, "it"))
                .performClick()

            compose.gateAwaitAnchor("HERMES")
            violations += compose.gateCollectViolations("Console (post-onboarding)", pool)
            assertTrue("onboarding flag must persist after finish", Onboarding.isDone(ctx))
            assertTrue(
                "ITALIAN-GATE-A onboarding violated (${violations.distinct().size}; first 10):\n" +
                    violations.distinct().take(10).joinToString("\n"),
                violations.distinct().isEmpty(),
            )
        } finally {
            // Restore the documented English baseline for the NEXT instrumentation
            // invocation (the regression suite asserts the en anchors).
            runCatching { compose.gateSwitchLocaleAndAwait("en") }
        }
    }

    @Test
    fun italianGateA2_console_and_inject_dialog_italian() {
        val pool = ItalianGate.italianPool()
        val violations = mutableListOf<String>()
        try {
            compose.gateSwitchLocaleAndAwait("it")

            // Console body (top bar, oracle truth row, composer, engine-truth line).
            compose.gateAwaitAnchor("HERMES")
            violations += compose.gateCollectViolations("Console", pool)

            // R-9 completion: prove the LIVE console composition before the dialog
            // interaction (the composer hint only exists on the interactive console).
            compose.gateAwaitAnchor(ItalianGate.localizedString(R.string.composer_hint, "it"))

            // Text-injection dialog: localized title/placeholder/buttons.
            val injectDesc = ItalianGate.localizedString(R.string.inject_cd, "it")
            compose.onNodeWithContentDescription(injectDesc).performClick()
            compose.gateAwaitAnchor(ItalianGate.localizedString(R.string.cd_inject_title, "it"))
            violations += compose.gateCollectViolations("Console inject dialog", pool)
            compose.onNodeWithText(ItalianGate.localizedString(R.string.cd_cancel, "it"))
                .performClick()

            assertTrue(
                "ITALIAN-GATE-A console violated (${violations.distinct().size}; first 10):\n" +
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
