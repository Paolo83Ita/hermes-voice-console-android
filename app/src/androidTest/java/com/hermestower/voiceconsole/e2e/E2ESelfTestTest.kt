package com.hermestower.voiceconsole.e2e

import android.content.Context
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.hermestower.voiceconsole.selftest.SelfTestMode
import com.hermestower.voiceconsole.selftest.SelfTestState
import com.hermestower.voiceconsole.ui.MainActivity
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

/**
 * E2E-31..34 (GRAPH 2 §13): Quick and Full Self-Test runs from the Self-Test
 * Center surface, typed result verification, secret-clean report export.
 */
@RunWith(JUnit4::class)
class E2ESelfTestTest {

    @get:Rule
    val compose: HermesComposeRule = createAndroidComposeRule(MainActivity::class.java)

    private fun graph() = E2E.graph()

    private fun awaitCompletion(timeoutMs: Long) {
        // CP-G4-007 attempt-3 race repair: `run()` flips `running` inside a
        // launched coroutine — a poll that arrives before the coroutine starts
        // saw `running == false` and returned with zero results (run 41514356403,
        // e2e_31_33 "results empty"). First wait (bounded) for the run to START,
        // then wait for completion.
        val startedDeadline = System.currentTimeMillis() + 10_000
        while (System.currentTimeMillis() < startedDeadline && !graph().selfTestCenter.running.value) {
            Thread.sleep(50)
        }
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            if (!graph().selfTestCenter.running.value) return
            Thread.sleep(250)
        }
        throw AssertionError("Self-Test did not finish within ${timeoutMs} ms")
    }

    @Before
    fun freezeAnimations() {
        compose.freezeAnimationsForTest()
    }

    @Test
    fun e2e_31_33_runQuickSelfTest_verifyTypedResults() {
        compose.openDrawerAndNavigate("Self-Test Center", "Run Quick")
        compose.clickFirstClickable("Run Quick")
        awaitCompletion(180_000)
        val results = graph().selfTestCenter.results.value
        assertTrue(results.isNotEmpty())
        val executed = results.values.filter { it.state != SelfTestState.NOT_RUN }
        assertTrue("quick subset empty", executed.isNotEmpty())
        val failures = executed.filter { it.state != SelfTestState.PASS }
        assertTrue("non-pass results: ${failures.map { it.testId to it.evidence }}", failures.isEmpty())
        // Every executed result carries the required typed fields.
        executed.forEach { r ->
            assertTrue(r.testName.isNotBlank())
            assertTrue(r.evidence.isNotBlank())
        }
    }

    @Test
    fun e2e_32_runFullSelfTest_allPassOnEmulator() {
        compose.openDrawerAndNavigate("Self-Test Center", "Run Full")
        compose.clickFirstClickable("Run Full")
        awaitCompletion(480_000)
        val results = graph().selfTestCenter.results.value
        assertTrue(results.size >= 45)
        val failures = results.values.filter { it.state != SelfTestState.PASS }
        assertTrue("non-pass results: ${failures.map { it.testId to it.evidence }}", failures.isEmpty())
    }

    @Test
    fun e2e_34_exportSecretCleanReport() {
        compose.openDrawerAndNavigate("Self-Test Center", "Run Quick")
        compose.clickFirstClickable("Run Quick")
        awaitCompletion(180_000)
        compose.clickFirstClickable("Export Secret-Clean Report")
        val clipboard = graph().let {
            val cm = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation()
                .targetContext.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
            cm.primaryClip?.getItemAt(0)?.text?.toString() ?: ""
        }
        assertTrue(clipboard.contains("secret-clean"))
        assertTrue(clipboard.contains("ST-01"))
        assertFalse(clipboard.contains("st-secret"))
    }
}
