package com.hermestower.voiceconsole.e2e

import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.runner.RunWith
import androidx.test.platform.app.InstrumentationRegistry
import com.hermestower.voiceconsole.app.AppGraphHolder
import com.hermestower.voiceconsole.core.state.ConsoleState
import com.hermestower.voiceconsole.tower.VaultKeys
import com.hermestower.voiceconsole.ui.MainActivity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * E2E-35..45 (GRAPH 2 §13): theme switching, reduced motion, privacy mute,
 * listening blocked while muted, activity recreation, application restart,
 * configuration persistence, sensitive-value clearing, final clean exit.
 */
@RunWith(AndroidJUnit4::class)
class E2EAppearancePersistenceTest {

    @get:Rule
    val compose: HermesComposeRule = hermesComposeRule()

    private fun graph() = E2E.graph()

    @Before
    fun freezeAnimations() {
        compose.freezeAnimationsForTest()
    }

    @Test
    fun e2e_35_36_switchMarbleLightAndObsidianNight() {
        compose.openDrawerAndNavigate("Appearance", "MARBLE LIGHT")
        // MARBLE LIGHT is hard-coded in AppearanceScreen and the screen is a
        // fully-composed scroll Column — zero nodes here means the navigation
        // never happened (the CP-G4-007 frozen-clock evidence).
        compose.onFirstNodeWithText("MARBLE LIGHT").performClick()
        compose.waitUntil(10_000) { !graph().store.settings.value.darkTheme }
        assertFalse(graph().store.settings.value.darkTheme)
        compose.onFirstNodeWithText("OBSIDIAN NIGHT").performClick()
        compose.waitUntil(10_000) { graph().store.settings.value.darkTheme }
        assertTrue(graph().store.settings.value.darkTheme)
    }

    @Test
    fun e2e_37_enableReducedMotion() {
        compose.openDrawerAndNavigate("Appearance", "MARBLE LIGHT")
        compose.onFirstNodeWithText("Reduced motion").performClick()
        compose.waitUntil(10_000) { graph().store.settings.value.reducedMotion }
        assertTrue(graph().store.settings.value.reducedMotion)
        graph().store.updateSettings { it.copy(reducedMotion = false) }
    }

    @Test
    fun e2e_38_39_privacyMute_blocksListening() {
        // E2E-38 enable privacy mute via the console control.
        graph().driver.setPrivacyMuted(true)
        compose.waitUntil(10_000) { graph().store.privacyMuted.value }
        assertTrue(graph().store.privacyMuted.value)
        // E2E-39 listening cannot start while muted: wake is ignored, state stays PRIVACY_MUTED.
        graph().driver.injectWake()
        Thread.sleep(1_500)
        assertEquals(ConsoleState.PRIVACY_MUTED, graph().store.currentState())
        graph().driver.setPrivacyMuted(false)
    }

    @Test
    fun e2e_40_recreateActivity_stateRestored() {
        compose.activityRule.scenario.recreate()
        compose.waitForIdle()
        compose.onNodeWithText("HERMES").assertExists()
        // Settings survive recreation (in-memory store re-applied from persistence).
        assertTrue(graph().store.settings.value.burnInGuard)
    }

    @Test
    fun e2e_41_42_restartApplication_configurationPersists() {
        // Persist a known setting, then restart the activity (application restart semantics).
        graph().store.updateSettings { it.copy(captionsEnabled = false) }
        compose.activityRule.scenario.close()
        androidx.test.core.app.ActivityScenario.launch(MainActivity::class.java)
        compose.waitForIdle()
        compose.waitUntil(15_000) { !graph().store.settings.value.captionsEnabled }
        assertFalse(graph().store.settings.value.captionsEnabled)
        graph().store.updateSettings { it.copy(captionsEnabled = true) }
    }

    @Test
    fun e2e_43_44_clearSensitiveValues_metadataRemains() {
        val profileId = "profile-e2e-secret"
        graph().profileStore.setSecret(profileId, "e2e-secret-value".toCharArray())
        assertNotNull(graph().profileStore.secretFor(profileId))
        compose.openDrawerAndNavigate("Connect to Hermes Tower", "Connect Hermes Tower")
        compose.clickFirstClickable("Clear sensitive values")
        compose.waitForText("Sensitive values cleared")
        assertNull(graph().vault.get(VaultKeys.secretKeyFor(profileId)))
        assertNull(graph().vault.get(VaultKeys.pairingKeyFor(profileId)))
        // Profile metadata remains readable and intact.
        assertNotNull(graph().profileStore.load())
    }

    @Test
    fun e2e_45_finalCleanExit_consoleAtRest() {
        graph().driver.setPrivacyMuted(false)
        graph().driver.stopSpeaking()
        val state = graph().store.currentState()
        assertTrue(
            "console did not reach a resting state (was $state)",
            state.isResting || state == ConsoleState.IDLE,
        )
        AppGraphHolder.graph(InstrumentationRegistry.getInstrumentation().targetContext)
    }
}
