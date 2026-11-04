package com.hermestower.voiceconsole.e2e

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * GRAPH 4 directive §4 — ITALIAN-GATE-E: Settings, Appearance and Language.
 *
 * Segment scope: the Settings hub with its six prescribed categories
 * (GENERAL / VOICE / TOWER CONNECTION / MODELS / DISPLAY / DIAGNOSTICS — all
 * six asserted PRESENT), the Appearance screen (theme chips are invariant
 * identities) and the language section (the five product-locale chips).
 */
@RunWith(AndroidJUnit4::class)
class ItalianGateE_SettingsAppearanceLanguageTest {

    @get:Rule
    val compose: HermesComposeRule = hermesComposeRule()

    @Before
    fun baseline() {
        compose.freezeAnimationsForTest()
        compose.gateEnsureBaseline()
    }

    @Test
    fun italianGateE_settingsHub_appearance_language_italian() {
        val pool = ItalianGate.italianPool()
        val violations = mutableListOf<String>()
        try {
        compose.gateSwitchLocaleAndAwait("it")

        // ---- Settings hub through the drawer row. ----
        compose.gateNavigate(R.string.dest_settings, R.string.settings_title)
        violations += compose.gateCollectViolations("Settings hub", pool)

        // All six prescribed categories must be visible (directive §5).
        val hubTexts = compose.gateVisibleTexts()
        val missingCats = listOf(
            R.string.settings_cat_general,
            R.string.settings_cat_voice,
            R.string.settings_cat_tower,
            R.string.settings_cat_models,
            R.string.settings_cat_display,
            R.string.settings_cat_diagnostics,
        ).map { catRes -> ItalianGate.localizedString(catRes, "it") }
            .filter { cat -> hubTexts.none { it.contains(cat) } }
        assertTrue(
            "Settings hub is missing prescribed categories (it): $missingCats; " +
                "visible: ${compose.gateVisibleTexts().take(20)}",
            missingCats.isEmpty(),
        )

        // ---- Appearance (DISPLAY category row). ----
        compose.gateNavigate(R.string.settings_display_visual, R.string.appearance_title)
        violations += compose.gateCollectViolations("Appearance", pool)

        // ---- Voice and audio (VOICE category row). ----
        compose.gateNavigate(R.string.settings_voice_controls, R.string.voice_title)
        violations += compose.gateCollectViolations("Voice and audio", pool)

        // ---- Language (GENERAL category row → the language section). ----
        compose.gateNavigate(R.string.settings_general_language, R.string.appearance_title)
        violations += compose.gateCollectViolations("Language section", pool)

        // ---- Return to the safe route (Console) through the REAL UI. ----
        compose.gateOpenDrawer()
        check(
            compose.gateActivateRow(ItalianGate.localizedString(R.string.dest_console, "it")),
        ) { "console drawer row not found (it)" }
        compose.gateAwaitAnchor("HERMES")

        assertTrue(
            "ITALIAN-GATE-E violated (${violations.distinct().size}; first 10):\n" +
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
