package com.hermestower.voiceconsole.e2e

import android.content.Intent
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
import androidx.test.platform.app.InstrumentationRegistry
import com.hermestower.voiceconsole.app.AppGraph
import com.hermestower.voiceconsole.app.AppGraphHolder
import com.hermestower.voiceconsole.ui.MainActivity
import com.hermestower.voiceconsole.ui.runtime.MotionProfile

typealias HermesComposeRule = AndroidComposeTestRule<ActivityScenarioRule<MainActivity>, MainActivity>

/**
 * CP-G4-016 — the two authorized E2E visual variants (OWNER_DECISION_E2E_RECOVERY).
 *
 *  A. NORMAL_VISUAL_PROFILE  — no `lifecycleProfile` instrumentation argument:
 *     the harness freezes the compose clock (the console ships an infinite Oracle
 *     Core animation, which would otherwise keep the rule from reaching idle).
 *
 *  B. CI_LIFECYCLE_PROFILE   — `lifecycleProfile true`: the activity is launched
 *     with [MainActivity.EXTRA_CI_LIFECYCLE_PROFILE] (debug builds only), which
 *     freezes every infinite ornamental frame clock through the app's own
 *     reduced-motion gate. NOTHING about navigation, state machine, controls,
 *     persistence, semantics or business logic changes. Because no infinite
 *     animation remains, the compose clock is left ADVANCING: NavHost and drawer
 *     transitions complete in real time, back-stack entries reach CREATED, and
 *     the harness navigates the real UI with REAL physical clicks — exactly the
 *     lifecycle/navigation qualification the Owner authorized.
 */
val ciLifecycleProfileRequested: Boolean =
    InstrumentationRegistry.getArguments()
        .getString("lifecycleProfile", "false") == "true"

/** Test rule factory honoring the authorized profile (see class docs above). */
fun hermesComposeRule(): HermesComposeRule {
    val context = InstrumentationRegistry.getInstrumentation().targetContext
    val intent = Intent(context, MainActivity::class.java)
    if (ciLifecycleProfileRequested) {
        intent.putExtra(MainActivity.EXTRA_CI_LIFECYCLE_PROFILE, true)
    }
    return AndroidComposeTestRule(ActivityScenarioRule<MainActivity>(intent)) { it }
}

/**
 * Variant A only: freeze the compose clock (the console ships infinite
 * animations, which would otherwise keep the test rule from ever reaching idle).
 * In variant B (CI_LIFECYCLE_PROFILE) the app's own profile freezes all infinite
 * ornaments, so the clock keeps advancing and transitions complete in real time.
 */
fun HermesComposeRule.freezeAnimationsForTest() {
    if (!ciLifecycleProfileRequested) {
        mainClock.autoAdvance = false
    }
}

/** Shared E2E helpers: graph access and drawer navigation. */
object E2E {
    fun graph(): AppGraph = AppGraphHolder.graph(InstrumentationRegistry.getInstrumentation().targetContext)
}

/**
 * CP-G4-007 E2E-ROBUSTNESS — the frozen-clock navigation contract (variant A).
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
 * Variant B (CI_LIFECYCLE_PROFILE, CP-G4-016): with the app's ornaments frozen
 * the clock advances, transitions complete, rows land on-screen, and the
 * PHYSICAL click is used (the truthful path); the accessible OnClick semantics
 * action remains the documented fallback in both variants.
 */
fun HermesComposeRule.openDrawerAndNavigate(destinationLabel: String, screenAnchor: String) {
    onNodeWithContentDescription("Open navigation drawer").performClick()
    if (ciLifecycleProfileRequested) {
        // ---- Variant B: REAL frames. Wait in real time, click physically. ----
        waitUntil(15_000) { drawerRowNode(destinationLabel) != null }
        val deadline = System.currentTimeMillis() + 5_000
        while (System.currentTimeMillis() < deadline) {
            val pos = drawerRowPosition(destinationLabel)
            if (pos != null && pos.x >= 0f) break
            Thread.sleep(50)
        }
        val rowNode = drawerRowNode(destinationLabel)
        if (rowNode == null) error("drawer row '$destinationLabel' disappeared before activation")
        val onScreen = drawerRowPosition(destinationLabel)?.let { it.x >= 0f } ?: false
        if (onScreen) {
            onAllNodes(hasText(destinationLabel, substring = true).and(hasClickAction()))[0]
                .performClick()
        } else {
            val accessibleClick = rowNode.config[SemanticsActions.OnClick]
            if (accessibleClick?.action != null) accessibleClick.action!!()
            else error("drawer row '$destinationLabel' has no accessible OnClick action")
        }
        waitUntil(20_000) {
            onAllNodesWithText(screenAnchor, substring = true).fetchSemanticsNodes().isNotEmpty()
        }
        waitForIdle()
        return
    }

    // ---- Variant A: frozen clock — bounded virtual-time pumping (unchanged). ----
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

    // (b) activate: the ACCESSIBLE OnClick semantics action is the primary
    // activation path here (Owner §3.3) — on this emulator the frozen drawer
    // animation leaves the sheet's visual position ambiguous (graphicsLayer
    // translation is not reflected in layout coordinates), so touch injection
    // cannot be trusted to land on the control. The physical clickability of
    // the row remains ASSERTED by the hasClickAction matcher (Owner §3.4);
    // physical touch qualification happens on the Xiaomi.
    val row = drawerRowNode(destinationLabel)
    if (row == null) error("drawer row '$destinationLabel' disappeared before activation")
    val accessibleClick = row.config[SemanticsActions.OnClick]
    if (accessibleClick?.action != null) {
        accessibleClick.action!!()
    } else {
        error("drawer row '$destinationLabel' has no accessible OnClick action")
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
 * the condition holds or the deadline expires. In variant B the virtual pump is
 * unnecessary (time flows; the bounded real-time poll remains).
 */
fun HermesComposeRule.pumpUntil(timeoutMs: Long = 20_000, condition: () -> Boolean) {
    val deadline = System.currentTimeMillis() + timeoutMs
    while (System.currentTimeMillis() < deadline) {
        if (condition()) return
        if (!ciLifecycleProfileRequested) mainClock.advanceTimeBy(100)
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
