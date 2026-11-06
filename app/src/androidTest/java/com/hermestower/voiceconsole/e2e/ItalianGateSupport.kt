package com.hermestower.voiceconsole.e2e

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
// R-27 (run #143 @ afefd3d, death #14): TouchInjectionScope.click is a
// TOP-LEVEL EXTENSION (`public fun TouchInjectionScope.click(position: Offset
// = center)`, TouchInjectionScope.kt line 392), not an interface member —
// members resolve inside the performTouchInput lambda but full-gesture
// extensions must be imported at the call site. Its absence was the single
// compiler error (`412:38 Unresolved reference 'click'`) that killed BOTH
// lanes before the emulator ever booted. No behavior change.
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onAllNodes
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.test.espresso.AppNotIdleException
import androidx.test.espresso.IdlingPolicies
import androidx.test.platform.app.InstrumentationRegistry
import com.hermestower.voiceconsole.R
import com.hermestower.voiceconsole.locale.AppLanguage
import com.hermestower.voiceconsole.ui.screens.Onboarding
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * GRAPH 4 directive §4 — shared harness for the SEGMENTED Italian completeness
 * gate (classes ItalianGateA..F). Replaces the monolithic audit
 * (superseded class E2EItalianCompletenessTest, failure ledger
 * ITALIAN_GATE_NAVIGATION_FAILURE_LEDGER.md R-1..R-7).
 *
 * The classification vocabulary is carried over VERBATIM from the monolithic
 * gate — the banned-English list, the invariant identities, the technical
 * shapes and the runtime Italian pool are UNCHANGED. Nothing is weakened: the
 * six segments together cover a SUPERSET of the monolithic flow (Onboarding
 * and the explicit scanner Back navigation were never covered before).
 *
 * Evidence-based physics (each item traces to a ledger run):
 *  - destination-composition floor 60 s: R-7 proved the destination composes
 *    past the 20 s wait (anchor present in the timeout dump); the floor is
 *    raised ONLY for destination composition and ONLY with that evidence;
 *  - every direct semantics activation dispatches on the main thread (R-3:
 *    NavController.navigate demands it);
 *  - the scanner is left via its own Back path before any drawer navigation
 *    (R-4: full-screen route, no drawer) and the Back return is ASSERTED;
 *  - locale switches use the system-driven relaunch only — no explicit
 *    recreate races (R-1); the activity baseline is re-established by the
 *    shared @Before helper (wizard-visible self-healing).
 */

object ItalianGate {

    /** Reviewed ordinary-English words that must never render under Italian. */
    val bannedEnglish = setOf(
        "the", "and", "with", "for", "you", "your", "yours", "from", "into", "will",
        "are", "was", "were", "been", "being", "have", "has", "had", "does", "did",
        "not", "all", "any", "can", "could", "should", "would", "shall", "may", "might",
        "now", "new", "more", "most", "when", "then", "than", "that", "this", "these",
        "those", "there", "here", "what", "which", "where", "who", "how", "why",
        "its", "also", "only", "just", "even", "still", "already", "after", "before",
        "between", "during", "through", "under", "over", "again", "once", "both",
        "each", "every", "some", "such", "own", "same", "too", "very", "well",
        "best", "better", "need", "needs", "needed", "get", "gets", "got", "put",
        "make", "makes", "made", "use", "used", "uses",
        "settings", "download", "downloads", "install", "installed", "uninstall",
        "search", "save", "saved", "cancel", "send", "delete", "remove", "removed",
        "open", "close", "start", "stop", "pause", "resume", "retry", "retries",
        "connect", "connected", "disconnect", "disconnected", "pair", "pairing",
        "scan", "share", "copy", "paste", "clear", "reset", "restore", "import",
        "export", "name", "address", "language", "voice", "model", "models",
        "history", "about", "advanced", "details", "status", "version", "update",
        "updates", "storage", "space", "network", "device", "screen", "theme",
        "dark", "light", "motion", "notifications", "notification", "permission",
        "permissions", "allow", "deny", "grant", "error", "errors", "warning",
        "warnings", "loading", "please", "wait", "try", "later", "yes", "none",
        "never", "always", "available", "unavailable", "unknown", "current",
        "recommended", "configuration", "profile", "profiles", "connection",
        "component", "components", "free", "reserve", "size", "duration", "expected",
        "minimum", "maximum", "verified", "qualified", "active", "inactive",
        "previous", "next", "state", "states", "compatibility", "licenses", "license",
        "optional", "required", "classification", "provider", "provenance",
    )

    /** Invariant identities (audit §2) allowed verbatim. */
    val invariantTokens = setOf(
        "hermes", "tower", "model", "sanctum", "simulated", "stt", "tts", "llm",
        "vad", "qr", "abi", "abis", "ram", "dns", "tls", "rest", "websocket",
        "sha", "onnx", "openwakeword", "gtcrn", "keystore", "tailscale", "android",
        "google", "xiaomi", "apk", "fa", "fr", "v0", "hermes-pair", "push-to-talk",
    )

    val technicalShape = listOf(
        Regex("^[A-Z0-9_]+$"),                          // ALL-CAPS codes: PASSED, TOWER_FIRST
        Regex(".*[a-z][A-Z].*"),                        // camelCase identifiers
        Regex("^(https?|wss?|mock)://.*"),              // URLs
        Regex(".*://.*"),
        Regex("v?\\d.*"),                               // versions, sizes, times
        Regex(".*\\d.*"),
        Regex("^[a-z0-9]+([-_.][a-z0-9_.-]+)*$"),       // snake/kebab/dotted identifiers
        Regex(".*(/|\\\\|\\||·|—|–|%%|%|•|⚠|✓|✗|：).*"),
    )

    fun tokenize(text: String): Set<String> =
        text.split(Regex("[^\\p{L}\\p{Nd}]+"))
            .filter { it.length >= 2 }
            .map { it.lowercase(Locale.ROOT) }
            .toSet()

    /** Every app string resolved under an Italian configuration (runtime pool). */
    fun italianPool(): Set<String> {
        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        val itCfg = android.content.res.Configuration(ctx.resources.configuration)
        val locale = Locale.forLanguageTag("it")
        itCfg.setLocale(locale)
        itCfg.setLocales(android.os.LocaleList(locale))
        val itCtx = ctx.createConfigurationContext(itCfg)
        val pool = mutableSetOf<String>()
        for (field in R.string::class.java.fields) {
            runCatching { pool += tokenize(itCtx.getString(field.getInt(null))) }
                .onFailure { /* non-string resource skipped */ }
        }
        return pool
    }

    /** Any app string resolved under an explicit locale configuration. */
    fun localizedString(resId: Int, code: String): String {
        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        val cfg = android.content.res.Configuration(ctx.resources.configuration)
        val locale = Locale.forLanguageTag(code)
        cfg.setLocale(locale)
        cfg.setLocales(android.os.LocaleList(locale))
        return ctx.createConfigurationContext(cfg).getString(resId)
    }

    fun violationsFor(text: String, pool: Set<String>): List<String> =
        tokenize(text).filter { token ->
            when {
                token in pool -> false
                token in invariantTokens -> false
                technicalShape.any { it.matches(token) } -> false
                token in bannedEnglish -> true
                else -> false
            }
        }
}

/**
 * Baseline for every gate class: onboarding done, Console front, documented
 * English locale. Self-healing — a previous class that died inside the wizard
 * (no drawer) is recovered here by re-marking the flag and recreating once.
 */
fun HermesComposeRule.gateEnsureBaseline() {
    val ctx = InstrumentationRegistry.getInstrumentation().targetContext
    // R-16 (runs 41938185541 #132 + #135 @ 9508ac8): ActivityScenario.recreate()
    // ends in Espresso.onIdle() whose MASTER idling policy defaults to 60 s; under
    // the starved software fleet the app's cold-start AsyncTask drain needs
    // 70–180 s (it FINISHES — run #135's A1 class time of 604 s covers 4×60 s
    // failed syncs + backoffs and still returned; A1 PASSED at bba1ebc when the
    // fleet was healthier). The harness's own R-7 precedent: raise the tolerance
    // with evidence — 240 s master + idling-resource policy, 4× the default.
    // The recreate must still SUCCEED for the walk to continue; only the idle
    // tolerance changes. Fresh process per `am instrument -e class` invocation,
    // so this applies to exactly the one gate class running.
    IdlingPolicies.setMasterPolicyTimeout(240, TimeUnit.SECONDS)
    IdlingPolicies.setIdlingResourceTimeout(240, TimeUnit.SECONDS)
    Onboarding.markDone(ctx)
    val enDesc = ItalianGate.localizedString(R.string.nav_open_drawer, "en")
    val itDesc = ItalianGate.localizedString(R.string.nav_open_drawer, "it")
    val drawerVisible = runCatching {
        onAllNodesWithContentDescription(enDesc, substring = true).fetchSemanticsNodes().isNotEmpty() ||
            onAllNodesWithContentDescription(itDesc, substring = true).fetchSemanticsNodes().isNotEmpty()
    }.getOrDefault(false)
    if (!drawerVisible) {
        // R-13 (run 41910303552 @ e084be8): ActivityScenario.recreate() synchronizes
        // with the espresso idle controller; under the starved software fleet the
        // main looper can stay busy >60 s (AsyncTask drain) → AppNotIdleException —
        // the SAME flake class TEST-E typed as INCONCLUSIVE_EMULATOR_IDLE_SYNC
        // (A1 passed in run bba1ebc and failed here on nothing but that flake).
        // R-14 (run 41938185541 @ a2d0b27): the ONE-retry settlement was not enough
        // — the retry recreate threw the SAME AppNotIdleException seconds later and
        // honestly propagated. Superseded by gateRecreateSteadily() below (typed,
        // bounded, three-retry); nothing is skipped, the recreate must SUCCEED.
        gateRecreateSteadily()
        pumpUntil(30_000) {
            runCatching {
                onAllNodesWithContentDescription(enDesc, substring = true).fetchSemanticsNodes().isNotEmpty() ||
                    onAllNodesWithContentDescription(itDesc, substring = true).fetchSemanticsNodes().isNotEmpty()
            }.getOrDefault(false)
        }
    }
    // Restore the documented English baseline (no-op relaunch when already en).
    runCatching { gateSwitchLocaleAndAwait("en") }
}

/**
 * R-14 (run 41938185541 @ a2d0b27): the AppNotIdleException class around
 * ActivityScenario.recreate() is NOT a one-shot flake. At that tip A1's own
 * wizard recreate (then unprotected) threw it, and gateEnsureBaseline's
 * R-13 single retry ALSO threw the same exception seconds later — the
 * starved software fleet can keep AsyncTask drains alive past the 60 s
 * idle policy repeatedly (the flake class TEST-E typed
 * INCONCLUSIVE_EMULATOR_IDLE_SYNC; A1 passed bba1ebc, failed e084be8,
 * failed a2d0b27 — nothing but fleet physics changed between them).
 *
 * Repair: ONE typed helper used at EVERY gate recreate site. It catches
 * ONLY AppNotIdleException, settles with backoff and re-issues the recreate
 * (two retries on top of the first attempt — with the R-16 240 s idling
 * policy the worst case 3×240 s + backoffs ≈ 760 s stays inside the 900 s
 * per-class invocation budget). Any other failure — and a THIRD consecutive
 * AppNotIdle — propagates honestly. Nothing is skipped or weakened: the
 * recreate must SUCCEED for the segment to continue its REAL product walk.
 */
fun HermesComposeRule.gateRecreateSteadily() {
    val backoffsMs = longArrayOf(10_000, 30_000)
    for (attempt in backoffsMs.indices) {
        try {
            activityRule.scenario.recreate()
            return
        } catch (e: AppNotIdleException) {
            Thread.sleep(backoffsMs[attempt])
        }
    }
    // Third and last attempt: its failure is the honest typed outcome.
    activityRule.scenario.recreate()
}

/** NAVIGATION DEFECT R-1: never recreate() while a locale relaunch is in flight. */
fun HermesComposeRule.gateSwitchLocaleAndAwait(code: String, timeoutMs: Long = 45_000) {
    val ctx = InstrumentationRegistry.getInstrumentation().targetContext
    val wizardVisible = runCatching {
        onAllNodesWithText(
            ItalianGate.localizedString(com.hermestower.voiceconsole.R.string.ob_title, "en"),
            substring = true,
        ).fetchSemanticsNodes().isNotEmpty() ||
            onAllNodesWithText(
                ItalianGate.localizedString(com.hermestower.voiceconsole.R.string.ob_title, "it"),
                substring = true,
            ).fetchSemanticsNodes().isNotEmpty()
    }.getOrDefault(false)
    val anchor = if (wizardVisible) {
        ItalianGate.localizedString(R.string.ob_title, code)
    } else {
        ItalianGate.localizedString(R.string.nav_open_drawer, code)
    }
    AppLanguage.apply(ctx, code)
    val deadline = System.currentTimeMillis() + timeoutMs
    while (System.currentTimeMillis() < deadline) {
        val found = runCatching {
            if (wizardVisible) {
                onAllNodesWithText(anchor, substring = true, ignoreCase = true).fetchSemanticsNodes().isNotEmpty()
            } else {
                onAllNodesWithContentDescription(anchor, substring = true).fetchSemanticsNodes().isNotEmpty()
            }
        }.getOrDefault(false)
        if (found) {
            // R-9 (run 41868118783, gates A2+B): the FIRST interaction after the
            // system relaunch raced the recreation — the inject-dialog state is
            // remember{}-held and the recreation silently dropped it, so the
            // dialog anchor never composed. Repair: settle the relaunch, then
            // REQUIRE the anchor to persist across a second poll before the
            // caller may interact with the new composition.
            repeat(12) {
                if (!ciLifecycleProfileRequested) mainClock.advanceTimeBy(100)
                Thread.sleep(50)
            }
            val persisted = runCatching {
                if (wizardVisible) {
                    onAllNodesWithText(anchor, substring = true, ignoreCase = true).fetchSemanticsNodes().isNotEmpty()
                } else {
                    onAllNodesWithContentDescription(anchor, substring = true).fetchSemanticsNodes().isNotEmpty()
                }
            }.getOrDefault(false)
            if (persisted) return
            // Not persisted: the composition is still being replaced — keep polling.
        }
        if (!ciLifecycleProfileRequested) mainClock.advanceTimeBy(100)
        Thread.sleep(50)
    }
    error("locale '$code' relaunch not observed within $timeoutMs ms (anchor: '$anchor')")
}

/** Drawer open through the REAL affordance. */
fun HermesComposeRule.gateOpenDrawer() {
    val openDesc = ItalianGate.localizedString(R.string.nav_open_drawer, "it")
    onNodeWithContentDescription(openDesc).performClick()
}

/**
 * R-36 (run #149 lane 2, job 127595389539 — "console drawer row not found"
 * right after the scanner back path, n=1): the screen behind the drawer can
 * still be settling when the single drawer-open click lands, leaving the row
 * lookup running against a not-yet-open drawer. The REAL user path — open
 * the drawer, tap the row — is repeated up to [attempts] times with the same
 * bounded settle the anchors use. The destination anchor after the call
 * still decides success — nothing weakened.
 */
fun HermesComposeRule.gateOpenDrawerAndActivateRow(label: String, attempts: Int = 3): Boolean {
    repeat(attempts) { index ->
        gateOpenDrawer()
        if (index < attempts - 1 && !ciLifecycleProfileRequested) {
            repeat(6) { mainClock.advanceTimeBy(100); Thread.sleep(25) }
        }
        if (gateActivateRow(label)) return true
    }
    return false
}

/**
 * R-37 (runs #145/#149/#150 — the MODEL SANCTUM anchor timeout n=3): port the
 * CLOUD-PROVEN activation from the regression suite (E2ECommon
 * openDrawerAndNavigate → activateRowFrozen → activateAccessible) — match
 * `hasText + hasClickAction` and invoke the OnClick action DIRECTLY. The
 * regression mechanism navigates to the SAME destination on the SAME fleet in
 * every cloud sample (E2EModelManagerTest green in #149 lane 2), while the
 * gate's runOnMainSync dispatch and the R-26/R-35 physical tap have failed n=3
 * on exactly this row. The destination anchor after the call STILL decides
 * success — nothing weakened.
 */
fun HermesComposeRule.gateActivateRowAccessible(label: String): Boolean {
    var advanced = 0L
    while (advanced < 5_000L) {
        mainClock.advanceTimeBy(100)
        advanced += 100
        val rows = runCatching {
            onAllNodes(hasText(label, substring = true, ignoreCase = true).and(hasClickAction()))
                .fetchSemanticsNodes()
        }.getOrDefault(emptyList())
        val row = rows.firstOrNull() ?: continue
        val click = row.config[SemanticsActions.OnClick]?.action ?: continue
        click()
        return true
    }
    return false
}

/**
 * Activates the first clickable node labeled [label] (drawer rows, hub rows,
 * screen buttons) — the accessible OnClick dispatched ON MAIN (ledger R-3).
 * Bounded virtual pump first (frozen-clock physics, CP-G4-007).
 */
fun HermesComposeRule.gateActivateRow(label: String): Boolean {
    var advanced = 0L
    while (advanced < 5_000L) {
        mainClock.advanceTimeBy(100)
        advanced += 100
        val found = runCatching {
            onAllNodesWithText(label, substring = true, ignoreCase = true).fetchSemanticsNodes()
                .firstOrNull { it.config[SemanticsActions.OnClick]?.action != null }
        }.getOrNull()
        if (found != null) break
    }
    val row = runCatching {
        onAllNodesWithText(label, substring = true, ignoreCase = true).fetchSemanticsNodes()
            .firstOrNull { it.config[SemanticsActions.OnClick]?.action != null }
    }.getOrNull() ?: return false
    val click = row.config[SemanticsActions.OnClick]?.action ?: return false
    androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().runOnMainSync { click() }
    return true
}

/**
 * Destination-composition wait: ANY of [anchors] visible. Floor 60 s with the
 * R-7 evidence (destination composes past 20 s at the Console→Archive
 * boundary); the error carries the visible tree for the ledger.
 * R-8 (run 41868118783): matching is CASE-INSENSITIVE — the product renders
 * some section titles case-transformed (wizard step 1 shows "SCEGLI LA TUA
 * LINGUA" in semantics while the resource is "Scegli la tua lingua"); the
 * anchor was composed and correct, only its case differed (dump evidence).
 */
fun HermesComposeRule.gateAwaitAnchor(vararg anchors: String, timeoutMs: Long = 60_000) {
    val deadline = System.currentTimeMillis() + timeoutMs
    while (System.currentTimeMillis() < deadline) {
        val found = runCatching {
            anchors.any { anchor ->
                onAllNodesWithText(anchor, substring = true, ignoreCase = true).fetchSemanticsNodes().isNotEmpty()
            }
        }.getOrDefault(false)
        if (found) {
            // settle window: transition + layout complete before further activation
            if (!ciLifecycleProfileRequested) mainClock.advanceTimeBy(600)
            return
        }
        if (!ciLifecycleProfileRequested) mainClock.advanceTimeBy(100)
        Thread.sleep(20)
    }
    error(
        "no anchor of [${anchors.joinToString("|")}] composed within $timeoutMs ms; " +
            "visible texts: " + gateVisibleTexts(),
    )
}

/** Visible semantics text (Text + contentDescription) of the current screen. */
fun HermesComposeRule.gateVisibleTexts(): List<String> {
    val texts = runCatching {
        onAllNodesWithText("", substring = true)
            .fetchSemanticsNodes()
            .mapNotNull { n -> n.config.get(SemanticsProperties.Text)?.joinToString(" ") { it.text } }
    }.getOrDefault(emptyList())
    val descs = runCatching {
        onAllNodesWithContentDescription("", substring = true)
            .fetchSemanticsNodes()
            .mapNotNull { n -> n.config.get(SemanticsProperties.ContentDescription)?.joinToString(" ") { it } }
    }.getOrDefault(emptyList())
    return (texts + descs).filter { it.isNotBlank() }
}

/**
 * Drawer → (optionally the Settings hub) → row, then the destination anchor.
 * Identical product path to the monolithic gate (drawer row first; hub-row
 * fallback through Settings — the five-destination navigation contract).
 */
fun HermesComposeRule.gateNavigate(labelRes: Int, anchorRes: Int, timeoutMs: Long = 60_000) =
    gateNavigateTo(labelRes, ItalianGate.localizedString(anchorRes, "it"), timeoutMs)

/** Same product path with a LITERAL anchor (invariant identities: HERMES, MODEL SANCTUM). */
fun HermesComposeRule.gateNavigateTo(labelRes: Int, anchor: String, timeoutMs: Long = 60_000) {
    val label = ItalianGate.localizedString(labelRes, "it")
    gateOpenDrawer()
    // R-37: the gate's own activation first, then the CLOUD-PROVEN accessible
    // dispatch from the regression suite (same matcher + direct OnClick call —
    // E2EModelManagerTest green on this fleet in every sample). The hub
    // fallback below is unchanged; the destination anchor still decides.
    if (!gateActivateRow(label) && !gateActivateRowAccessible(label)) {
        val settingsLabel = ItalianGate.localizedString(R.string.dest_settings, "it")
        check(gateActivateRow(settingsLabel)) { "Settings row missing from drawer (it)" }
        // R-21 (run 41990077345 @ e89d014): the fallback used to await the ROW
        // label itself — but hub body text can contain the same words (the VOICE
        // section's engine_row contains "Modelli e voce"), so the await could
        // pass while the hub was never proven. The fallback now awaits the
        // Settings hub TITLE (the hub identity), then looks the row up.
        gateAwaitAnchor(ItalianGate.localizedString(R.string.settings_title, "it"), timeoutMs = 30_000)
        // R-21: the lookup tries the merged tree first, then the UNMERGED tree
        // (a clickable whose label lives in a non-merged child is still the real
        // control); both lookups are matcher-only — nothing weakened.
        val hubRow = runCatching {
            onAllNodesWithText(label, substring = true, ignoreCase = true).fetchSemanticsNodes()
                .firstOrNull { it.config[SemanticsActions.OnClick]?.action != null }
        }.getOrNull() ?: runCatching {
            onAllNodesWithText(label, substring = true, ignoreCase = true, useUnmergedTree = true)
                .fetchSemanticsNodes()
                .firstOrNull { it.config[SemanticsActions.OnClick]?.action != null }
        }.getOrNull()
        if (hubRow != null) {
            val click = hubRow.config[SemanticsActions.OnClick]?.action
                ?: error("settings-hub row '$label' has no OnClick (it)")
            // R-37: the direct invocation FIRST (the cloud-proven regression
            // mechanism); the runOnMainSync dispatch stays as the second arm.
            val dispatched = runCatching { click() }.isSuccess || runCatching {
                androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().runOnMainSync { click() }
            }.isSuccess
            check(dispatched) { "settings-hub row '$label' OnClick dispatch failed (it)" }
        } else if (gateActivateRowAccessible(label)) {
            // R-37: the proven accessible dispatch found a clickable node the
            // OnClick-filtered onAllNodesWithText lookup missed (the matcher
            // difference: hasText matches ContentDescription-carrying rows too).
        } else {
            // R-26 (runs #137 + #142 lane 1 — the SAME failure signature twice,
            // now with full dumps): the row text is dump-proven present ("Modelli
            // e Voce Componenti installati…", "Torre attuale e riconnessione…")
            // while NO text-matching node exposes OnClick in EITHER tree on this
            // fleet. The last-mile fallback is the REAL user path: one physical
            // tap at the center of the node whose text STARTS WITH the row label
            // (the hub row renders label+sub merged, so the row's text begins
            // with its label; body text like the console engine_row can CONTAIN
            // "Modelli e voce" without starting with it — the prefix
            // discriminates). The destination anchor after this block still
            // decides success — a tap that does not navigate fails the anchor
            // honestly with the visible dump. Nothing weakened.
            val nodes = runCatching {
                onAllNodesWithText(label, substring = true, ignoreCase = true).fetchSemanticsNodes()
            }.getOrDefault(emptyList())
            // R-35 (runs #145 lane 1 + #149 lane 2 — the SAME MODEL SANCTUM
            // anchor timeout TWICE, identical dump signature): the drawer's
            // hidden-but-composed rows are in the semantics tree even when the
            // drawer is closed, so the prefix match can pick the DRAWER's row
            // ("Modelli e voce") instead of the ON-SCREEN hub row ("Modelli e
            // Voce Componenti installati…") — the physical tap then lands on a
            // translated-off-screen node and nothing navigates. The hub row is
            // the on-screen one: require the node's center to lie inside the
            // root bounds (a closed drawer's rows sit outside them). If bounds
            // are unavailable the old semantics hold (fail-open); the
            // destination anchor after the tap STILL decides success — a tap
            // that does not navigate fails the anchor honestly. Nothing
            // weakened; the OnClick direct-dispatch path above is unchanged.
            val rootBounds = runCatching {
                onRoot().fetchSemanticsNode().boundsInRoot
            }.getOrDefault(androidx.compose.ui.geometry.Rect.Zero)
            val tapIdx = nodes.indexOfFirst { n ->
                n.config.get(SemanticsProperties.Text)?.any { it.text.startsWith(label, ignoreCase = true) } == true &&
                    runCatching { rootBounds.contains(n.boundsInRoot.center) }.getOrDefault(true)
            }
            check(tapIdx >= 0) {
                "settings-hub row '$label' not found (it); visible texts: " +
                    gateVisibleTexts().take(40)
            }
            onAllNodesWithText(label, substring = true, ignoreCase = true)[tapIdx]
                .performTouchInput { click() }
        }
    }
    gateAwaitAnchor(anchor, timeoutMs = timeoutMs)
    // R-10 (run 41868118783, gate C hang): the unconditional waitForIdle here
    // waited on the infinite Oracle Core animation and consumed the whole gate
    // invocation budget (runCatching catches exceptions, NOT hangs). Replaced
    // with the same bounded settle the anchors use — no unbounded wait remains
    // anywhere in the harness.
    if (!ciLifecycleProfileRequested) {
        repeat(6) { mainClock.advanceTimeBy(100); Thread.sleep(25) }
    }
}

/** Collects classified violations of the current tree for one destination. */
fun HermesComposeRule.gateCollectViolations(destination: String, pool: Set<String>): List<String> =
    gateVisibleTexts().flatMap { text ->
        ItalianGate.violationsFor(text, pool).map { token -> "$destination: '$token' in \"$text\"" }
    }

/** Leave the QR scanner through its OWN back path and PROVE the return route. */
fun HermesComposeRule.gateLeaveScannerBack(expectedReturnAnchor: String) {
    // R-30 (run #145 lane 1 job 127386947318 @ b69a5e6 — the FIRST fully-alive
    // 116-min fleet sample; the walk itself SUCCEEDED for 753 s and then died
    // at the back press): Espresso.pressBack requires the root window to have
    // focus ("RootViewWithoutFocusException ... has-window-focus=false"), and
    // on the CameraX scanner screen the window can legitimately be unfocused
    // on the software fleet (ColorBuffer degradation in the same window). The
    // REAL user path is the SYSTEM back button — injected at the DEVICE level
    // (UiAutomator), which needs no window focus. The return anchor still
    // decides success — nothing weakened.
    androidx.test.uiautomator.UiDevice.getInstance(
        androidx.test.platform.app.InstrumentationRegistry.getInstrumentation(),
    ).pressBack()
    gateAwaitAnchor(expectedReturnAnchor, timeoutMs = 60_000)
}

/**
 * R-12 (run 41890040629 @ bba1ebc — gate A2 identical anchor failure with the
 * relaunch race ALREADY repaired): the medallion-row control was clicked via
 * the gesture path (performClick = touch injection at the node center) and the
 * dialog never composed, while the SAME screen's drawer/hub/button rows pass
 * everywhere via gateActivateRow's DIRECT semantics-action dispatch on main
 * (ledger R-3 physics). Unify: find the node carrying BOTH the description and
 * an OnClick and invoke the action on the main thread. No assertion weakened —
 * the dialog anchor assert stays.
 */
fun HermesComposeRule.gateClickByContentDescription(desc: String): Boolean {
    val node = runCatching {
        onAllNodesWithContentDescription(desc, substring = true).fetchSemanticsNodes()
            .firstOrNull { it.config[SemanticsActions.OnClick]?.action != null }
    }.getOrNull() ?: return false
    val click = node.config[SemanticsActions.OnClick]?.action ?: return false
    androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().runOnMainSync { click() }
    return true
}
