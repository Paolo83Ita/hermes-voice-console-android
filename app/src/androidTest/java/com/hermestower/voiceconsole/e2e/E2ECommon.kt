package com.hermestower.voiceconsole.e2e

import android.content.Context
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.junit4.createAndroidComposeRule
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

typealias HermesComposeRule = AndroidComposeTestRule<ActivityScenarioRule<MainActivity>, MainActivity>

/**
 * CP-G4-016 — the two authorized E2E visual variants (OWNER_DECISION_E2E_RECOVERY).
 *
 *  A. NORMAL_VISUAL_PROFILE  — no `lifecycleProfile` instrumentation argument:
 *     the harness freezes the compose clock (the console ships an infinite Oracle
 *     Core animation, which would otherwise keep the rule from reaching idle).
 *
 *  B. CI_LIFECYCLE_PROFILE   — `lifecycleProfile true`: the harness sets the
 *     DEBUG-only system property [MainActivity.CI_LIFECYCLE_PROFILE_PROPERTY]
 *     BEFORE the activity launches (androidTest runs in the application process,
 *     so the property is visible to MainActivity, which freezes every infinite
 *     ornamental frame clock through the app's own reduced-motion gate). NOTHING
 *     about navigation, state machine, controls, persistence, semantics or
 *     business logic changes. Because no infinite animation remains, the compose
 *     clock is left ADVANCING: NavHost and drawer transitions complete in real
 *     time, back-stack entries reach CREATED, and the harness navigates the real
 *     UI with REAL physical clicks — exactly the lifecycle/navigation
 *     qualification the Owner authorized.
 */
val ciLifecycleProfileRequested: Boolean = run {
    val requested = InstrumentationRegistry.getArguments()
        .getString("lifecycleProfile", "false") == "true"
    if (requested) {
        System.setProperty(MainActivity.CI_LIFECYCLE_PROFILE_PROPERTY, "true")
    }
    requested
}

/** Test rule factory honoring the authorized profile (see class docs above).
 *  Referencing [ciLifecycleProfileRequested] here guarantees the DEBUG-only
 *  system property is set BEFORE the rule launches the activity. */
fun hermesComposeRule(): HermesComposeRule {
    val profileRequested = ciLifecycleProfileRequested
    check(!profileRequested || System.getProperty(MainActivity.CI_LIFECYCLE_PROFILE_PROPERTY) == "true")
    // GRAPH 4 directive §6: the suite tests the CONSOLE product path — preset the
    // onboarding-done flag so the first-launch wizard does not cover the
    // documented anchors. E2EOnboardingTest explicitly clears it to exercise the
    // wizard. (The androidTest runs in the application process, so the flag is
    // written in-process before the activity launches.)
    InstrumentationRegistry.getInstrumentation().targetContext
        .getSharedPreferences("onboarding", Context.MODE_PRIVATE)
        .edit().putBoolean("done", true).apply()
    return createAndroidComposeRule(MainActivity::class.java)
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
 * CP-G4-007 E2E-ROBUSTNESS — the frozen-clock navigation contract (variant A);
 * CP-G4-017 — the Settings-hub path. The drawer lists ONLY the five primary
 * destinations; when the requested label is not a drawer row, the harness walks
 * the REAL product path (drawer → Settings hub → row) — no destination logic is
 * ever called directly.
 *
 * Variant A physics (proven by runs 41459906172/41461749711/41470976708/
 * 41485512432/41503065076/41514356403 + the run-41514356403 semantics-tree dump):
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
    if (ciLifecycleProfileRequested) {
        // CP-G4-016: PROVE the profile engaged in the app process (probe + state).
        check(
            com.hermestower.voiceconsole.ui.runtime.MotionProfile.ciLifecycleProfile,
        ) { "CI_LIFECYCLE_PROFILE requested but NOT engaged in the app" }
    }
    onNodeWithContentDescription("Open navigation drawer").performClick()

    if (ciLifecycleProfileRequested) {
        // ---- Variant B: REAL frames — wait in real time, click physically. ----
        // CP-G4-017 wave 1 moved Appearance/Connection/Self-Test Center behind the
        // Settings hub: a hub-only label is NOT a drawer row. The activation
        // attempt therefore fails FAST for hub labels and the REAL product path
        // continues through the hub. (Cloud evidence for the previous
        // unconditional drawer-row wait: run 41657188171 — e2e_35_36 died in
        // ComposeTimeoutException inside this function, waiting 15 s for a drawer
        // row that no longer exists. The hub path was never reached.)
        if (!activateRowReal(destinationLabel)) {
            check(activateRowReal(SETTINGS_ROW)) { "Settings row missing from drawer" }
            waitUntil(15_000) { drawerRowNode(destinationLabel) != null }
            // Hub rows can sit below the fold: the accessible OnClick activation
            // (the screen-reader path, Owner §3.3) is used — truthful, and immune
            // to scroll state. Physical clicks stay reserved for drawer rows.
            val hubRow = drawerRowNode(destinationLabel)
                ?: error("settings-hub row '$destinationLabel' not found")
            activateAccessible(hubRow, destinationLabel)
        }
        waitUntil(20_000) {
            onAllNodesWithText(screenAnchor, substring = true).fetchSemanticsNodes().isNotEmpty()
        }
        waitForIdle()
        return
    }

    // ---- Variant A: frozen clock — bounded virtual-time pumping. ----
    // Same drawer-then-hub shape as variant B; the hub-row wait uses pumpUntil
    // because ONLY explicit virtual-time pumping composes new destinations on
    // the frozen clock (CP-G4-007 physics). activateRowFrozen already bounds its
    // own pump (5 s virtual), so no unconditional pre-wait is needed here.
    if (!activateRowFrozen(destinationLabel)) {
        check(activateRowFrozen(SETTINGS_ROW)) { "Settings row missing from drawer" }
        // pump until the Settings hub composes (its title anchor)
        pumpToAnchor("Settings")
        pumpUntil(15_000) { drawerRowNode(destinationLabel) != null }
        val hubRow = drawerRowNode(destinationLabel)
            ?: error("settings-hub row '$destinationLabel' not found")
        activateAccessible(hubRow, destinationLabel)
    }

    // destination screen: pump until the anchor composes, then settle
    pumpToAnchor(screenAnchor)
}

private const val SETTINGS_ROW = "Settings"

/** Variant B activation: physical click when truly on-screen, accessible fallback. */
private fun HermesComposeRule.activateRowReal(label: String): Boolean {
    val deadline = System.currentTimeMillis() + 5_000
    while (System.currentTimeMillis() < deadline) {
        val pos = drawerRowPosition(label)
        if (pos != null && pos.x >= 0f) break
        Thread.sleep(50)
    }
    val rowNode = drawerRowNode(label) ?: return false
    val onScreen = drawerRowPosition(label)?.let { it.x >= 0f } ?: false
    return if (onScreen) {
        onAllNodes(hasText(label, substring = true).and(hasClickAction()))[0].performClick()
        true
    } else {
        activateAccessible(rowNode, label)
    }
}

/** Variant A activation: accessible OnClick is primary (documented CP-G4-007
 *  physics — frozen drawer position ambiguity), bounded virtual pump first. */
private fun HermesComposeRule.activateRowFrozen(label: String): Boolean {
    var advanced = 0L
    while (advanced < 5_000L) {
        mainClock.advanceTimeBy(100)
        advanced += 100
        val pos = drawerRowPosition(label)
        if (pos != null && pos.x >= 0f) break
    }
    val row = drawerRowNode(label) ?: return false
    return activateAccessible(row, label)
}

private fun activateAccessible(row: androidx.compose.ui.semantics.SemanticsNode, label: String): Boolean {
    val accessibleClick = row.config[SemanticsActions.OnClick]
    if (accessibleClick?.action != null) {
        accessibleClick.action!!()
        return true
    }
    error("row '$label' has no accessible OnClick action")
}

/** Frozen-clock bounded pump until [anchor] composes, then a settle window. */
private fun HermesComposeRule.pumpToAnchor(anchor: String) {
    var settled = 0L
    while (settled < 20_000L) {
        mainClock.advanceTimeBy(100)
        settled += 100
        if (onAllNodesWithText(anchor, substring = true).fetchSemanticsNodes().isNotEmpty()) {
            // settle window: transition + layout/placement complete before any
            // further activation on the new screen
            mainClock.advanceTimeBy(600)
            return
        }
    }
    error(
        "screen anchor '$anchor' did not compose within 20 s of virtual time; " +
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
