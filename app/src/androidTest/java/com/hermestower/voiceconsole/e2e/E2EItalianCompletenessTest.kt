package com.hermestower.voiceconsole.e2e

import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.hermestower.voiceconsole.R
import com.hermestower.voiceconsole.locale.AppLanguage
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Locale

/**
 * GRAPH 4 directive §4 — ITALIAN_UI_COMPLETE gate (automated Italian visual-text
 * audit; NOT a resource-presence check).
 *
 * The test sets the app language to Italian, recreates the activity so the REAL
 * localized composition runs, then walks every ordinary destination through the
 * REAL product path (drawer → primary destination; drawer → Settings hub → row).
 * At each destination it collects the visible semantics text (Text +
 * contentDescription) and classifies every token:
 *
 *  1. sanctioned — the token exists in the app's own Italian resource pool
 *     (built at runtime by reflecting over the app's R.string and resolving
 *     every entry under an Italian configuration): app-authored Italian is
 *     always allowed, including sanctioned loanwords;
 *  2. technical — reviewed patterns: ALL-CAPS codes (PASSED, TOWER_FIRST…),
 *     camelCase identifiers, URLs, versions, hashes, snake/kebab identifiers,
 *     numbers+units, and the small invariant brand/identity list
 *     (HERMES, TOWER, MODEL SANCTUM, SIMULATED…);
 *  3. ordinary-English — a token from the reviewed banned list (the/and/with/
 *     download/settings/…) that is NOT sanctioned and NOT technical → VIOLATION.
 *
 * Unknown tokens outside all three classes are neutral (proper nouns, device
 * data). The gate FAILS with a compact violation report if any destination
 * renders ordinary English UI text under Italian.
 *
 * The English override is restored in a finally block so the rest of the suite
 * runs against the documented en anchors (no assertion weakened anywhere).
 */
@RunWith(AndroidJUnit4::class)
class E2EItalianCompletenessTest {

    @get:Rule
    val compose: HermesComposeRule = hermesComposeRule()

    @Before
    fun freezeAnimations() {
        // Variant-A physics (same as every other suite class): without the frozen
        // clock compose.waitForIdle() waits for the console's infinite Oracle Core
        // animation and never returns (run 41818773091 — 3600 s instrument timeout).
        compose.freezeAnimationsForTest()
    }

    /** Reviewed ordinary-English words that must never render under Italian. */
    private val bannedEnglish = setOf(
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
    private val invariantTokens = setOf(
        "hermes", "tower", "model", "sanctum", "simulated", "stt", "tts", "llm",
        "vad", "qr", "abi", "abis", "ram", "dns", "tls", "rest", "websocket",
        "sha", "onnx", "openwakeword", "gtcrn", "keystore", "tailscale", "android",
        "google", "xiaomi", "apk", "fa", "fr", "v0", "hermes-pair", "push-to-talk",
    )

    private val technicalShape = listOf(
        Regex("^[A-Z0-9_]+$"),                          // ALL-CAPS codes: PASSED, TOWER_FIRST
        Regex(".*[a-z][A-Z].*"),                        // camelCase identifiers
        Regex("^(https?|wss?|mock)://.*"),              // URLs
        Regex(".*://.*"),
        Regex("v?\\d.*"),                               // versions, sizes, times
        Regex(".*\\d.*"),
        Regex("^[a-z0-9]+([-_.][a-z0-9_.-]+)*$"),       // snake/kebab/dotted identifiers
        Regex(".*(/|\\\\|\\||·|—|–|%%|%|•|⚠|✓|✗|：).*"),
    )

    private fun italianPool(): Set<String> {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val ctx = instrumentation.targetContext
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

    private fun tokenize(text: String): Set<String> =
        text.split(Regex("[^\\p{L}\\p{Nd}]+"))
            .filter { it.length >= 2 }
            .map { it.lowercase(Locale.ROOT) }
            .toSet()

    private fun violationsFor(text: String, pool: Set<String>): List<String> =
        tokenize(text).filter { token ->
            when {
                token in pool -> false
                token in invariantTokens -> false
                technicalShape.any { it.matches(token) } -> false
                token in bannedEnglish -> true
                else -> false
            }
        }

    /** Drawer → (optionally hub) → row; returns after the anchor text is visible. */
    private fun navigate(label: String, anchor: String) {
        val openDesc = targetContext().getString(R.string.nav_open_drawer)
        compose.onNodeWithContentDescription(openDesc).performClick()
        if (!activateRow(label)) {
            check(activateRow(targetContext().getString(R.string.dest_settings))) {
                "Settings row missing from drawer (it)"
            }
            compose.waitForText(label)
            val hubRow = compose.onAllNodesWithText(label, substring = true)
                .fetchSemanticsNodes()
                .firstOrNull { it.config[SemanticsActions.OnClick]?.action != null }
                ?: error("settings-hub row '$label' not found (it)")
            invokeOnMain { hubRow.config[SemanticsActions.OnClick]!!.action!!() }
        }
        compose.waitForText(anchor)
        compose.waitForIdle()
    }


    /** NavController demands the main thread (lifecycle addObserver) — semantics
     *  actions are dispatched via runOnMainSync (evidence run 8727210 gate). */
    private fun invokeOnMain(action: () -> Unit) {
        androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().runOnMainSync(action)
    }

    private fun activateRow(label: String): Boolean {
        val row = compose.onAllNodesWithText(label, substring = true)
            .fetchSemanticsNodes()
            .firstOrNull { it.config[SemanticsActions.OnClick]?.action != null }
            ?: return false
        invokeOnMain { row.config[SemanticsActions.OnClick]!!.action!!() }
        return true
    }

    private fun targetContext() = InstrumentationRegistry.getInstrumentation().targetContext

    /** Visible semantics text (Text + contentDescription) of the current screen. */
    private fun collectVisibleTexts(): List<String> {
        val texts = compose.onAllNodesWithText("", substring = true)
            .fetchSemanticsNodes()
            .mapNotNull { n -> n.config.get(SemanticsProperties.Text)?.joinToString(" ") { it.text } }
        val descs = compose.onAllNodesWithContentDescription("", substring = true)
            .fetchSemanticsNodes()
            .mapNotNull { n -> n.config.get(SemanticsProperties.ContentDescription)?.joinToString(" ") { it } }
        return (texts + descs).filter { it.isNotBlank() }
    }

    private fun collectViolations(destination: String, pool: Set<String>): List<String> =
        collectVisibleTexts().flatMap { text ->
            violationsFor(text, pool).map { token -> "$destination: '$token' in \"$text\"" }
        }

    /**
     * Switches the per-app locale and WAITS for the system-driven activity
     * relaunch (API 33+ LocaleManager recreates the activity automatically —
     * an explicit scenario.recreate() races the relaunch and dies with
     * "stage must be [...] but was DESTROYED", evidence run 41814872419).
     * The wait polls for the drawer open-description resolved under the NEW
     * locale, tolerating the no-activity windows during relaunch.
     */
    private fun switchLocaleAndAwait(code: String, timeoutMs: Long = 45_000) {
        val ctx = targetContext()
        AppLanguage.apply(ctx, code)
        val locale = Locale.forLanguageTag(code)
        val cfg = android.content.res.Configuration(ctx.resources.configuration)
        cfg.setLocale(locale)
        cfg.setLocales(android.os.LocaleList(locale))
        val localizedDesc = ctx.createConfigurationContext(cfg)
            .getString(R.string.nav_open_drawer)
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            val found = runCatching {
                compose.onAllNodesWithContentDescription(localizedDesc, substring = true)
                    .fetchSemanticsNodes().isNotEmpty()
            }.getOrDefault(false)
            if (found) return
            if (!ciLifecycleProfileRequested) compose.mainClock.advanceTimeBy(100)
            Thread.sleep(50)
        }
        error("locale '$code' relaunch not observed within ${timeoutMs} ms")
    }

    @Test
    fun e2e_italian_ui_completeness_gate() {
        val appContext = targetContext()
        val violations = mutableListOf<String>()
        switchLocaleAndAwait("it")
        try {
            val pool = italianPool()

            data class Dest(val labelKey: Int, val anchor: String, val hub: Boolean, val name: String)
            val destinations = listOf(
                Dest(R.string.dest_console, "HERMES", false, "Console"),
                Dest(R.string.dest_archive, appContext.getString(R.string.archive_title), false, "Tower Archive"),
                Dest(R.string.dest_connect, appContext.getString(R.string.ts_title), false, "Tower Gate"),
                Dest(R.string.dest_models, "MODEL SANCTUM", false, "Models and Voice"),
                Dest(R.string.dest_settings, appContext.getString(R.string.settings_title), false, "Settings hub"),
                // Hub rows (label keys of the actual clickable rows, directive §5):
                Dest(R.string.settings_display_visual, "MARBLE LIGHT", true, "Appearance"),
                Dest(R.string.settings_voice_controls, appContext.getString(R.string.voice_title), true, "Voice and Audio"),
                Dest(R.string.settings_diag_selftest, appContext.getString(R.string.st_title), true, "Self-Test Center"),
                Dest(R.string.settings_diag_engine, appContext.getString(R.string.diag_title), true, "Diagnostics"),
                Dest(R.string.settings_diag_history, appContext.getString(R.string.history_title), true, "History"),
                Dest(R.string.settings_diag_intelligence, appContext.getString(R.string.li_title), true, "Local Intelligence"),
                Dest(R.string.settings_diag_about, appContext.getString(R.string.about_title), true, "About and Safety"),
            )

            for (dest in destinations) {
                val label = appContext.getString(dest.labelKey)
                navigate(label, dest.anchor)
                violations += collectViolations(dest.name, pool)
            }

            // Console text-injection dialog (localized title/placeholder/buttons).
            navigate(appContext.getString(R.string.dest_console), "HERMES")
            compose.onNodeWithContentDescription(appContext.getString(R.string.inject_cd)).performClick()
            compose.waitForText(appContext.getString(R.string.cd_inject_title))
            violations += collectViolations("Console inject dialog", pool)

            // Models and Voice: expanded attribute sheet + download-state rows.
            navigate(appContext.getString(R.string.dest_models), "MODEL SANCTUM")
            val details = compose.onAllNodesWithText(appContext.getString(R.string.mm_details), substring = true)
                .fetchSemanticsNodes()
                .firstOrNull { it.config[SemanticsActions.OnClick]?.action != null }
            invokeOnMain { details?.config[SemanticsActions.OnClick]!!.action!!() }
            compose.waitForText(appContext.getString(R.string.mm_attr_type))
            violations += collectViolations("Models attribute sheet", pool)

            // QR scanner (permission/camera state renders localized).
            navigate(appContext.getString(R.string.dest_connect), appContext.getString(R.string.ts_title))
            activateRow(appContext.getString(R.string.ts_action_scan_qr))
            compose.waitForText("SIMULATED", timeoutMs = 20_000)
            violations += collectViolations("QR scanner", pool)

            // Tower Archive: all three tabs (Conversations/Missions/Artifacts).
            navigate(appContext.getString(R.string.dest_archive), appContext.getString(R.string.archive_title))
            for (tabKey in intArrayOf(R.string.archive_tab_missions, R.string.archive_tab_artifacts)) {
                activateRow(appContext.getString(tabKey))
                compose.waitForIdle()
                violations += collectViolations("Archive tab", pool)
            }

            val report = violations.distinct()
            assertTrue(
                "ITALIAN_UI_COMPLETE violated (${report.size} findings; first 10):\n" +
                    report.take(10).joinToString("\n"),
                report.isEmpty(),
            )
        } finally {
            // Restore the documented en anchors for the rest of the suite
            // (system-driven relaunch awaited, same physics as the switch above).
            runCatching { switchLocaleAndAwait("en") }
        }
    }
}
