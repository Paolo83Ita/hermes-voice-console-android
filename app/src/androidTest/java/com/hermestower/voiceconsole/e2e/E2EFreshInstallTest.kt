package com.hermestower.voiceconsole.e2e

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.hermestower.voiceconsole.core.state.ConnectionStatus
import com.hermestower.voiceconsole.tower.ConnectionConfigState
import com.hermestower.voiceconsole.tower.TowerConnectionTester
import com.hermestower.voiceconsole.ui.MainActivity
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

/**
 * E2E-01..10 (GRAPH 2 §13): fresh install, first launch, credential-free
 * onboarding, Mock mode, Tower Connection Studio, save/read-back, mock
 * connection test, CONNECTED only after a real handshake.
 */
@RunWith(JUnit4::class)
class E2EFreshInstallTest {

    @get:Rule
    val compose: HermesComposeRule = createAndroidComposeRule(MainActivity::class.java)

    @Test
    fun e2e_01_02_freshInstallAndFirstLaunch_showWorkingConsole() {
        compose.onNodeWithText("HERMES").assertIsDisplayed()
        compose.waitForText("wake · inject text · stop voice")
    }

    @Test
    fun e2e_03_04_onboardingWithoutCredentials_mockModeSelected() {
        compose.openDrawerAndNavigate("Connection")
        compose.waitForText("Open Hermes Tower Connection Studio")
        compose.onNodeWithText("Open Hermes Tower Connection Studio").performClick()
        compose.waitForText("Reset to Mock mode")
        compose.onNodeWithText("Reset to Mock mode").performClick()
        compose.waitUntil(15_000) {
            E2E.graph().profileStore.load()?.transportMode == "MOCK_SIMULATOR" &&
                E2E.graph().profileStore.state() != ConnectionConfigState.EMPTY
        }
        assertEquals("MOCK_SIMULATOR", E2E.graph().profileStore.load()!!.transportMode)
    }

    @Test
    fun e2e_05_06_07_openStudio_validateAndSaveMockProfile() {
        compose.openDrawerAndNavigate("Hermes Tower Connection")
        compose.waitForText("Connect Hermes Tower")
        compose.onNodeWithText("Save and connect").performClick()
        compose.waitUntil(15_000) { E2E.graph().profileStore.state() == ConnectionConfigState.VALIDATED }
        assertEquals(ConnectionConfigState.VALIDATED, E2E.graph().profileStore.state())
    }

    @Test
    fun e2e_08_readBackSavedProfile() {
        compose.openDrawerAndNavigate("Hermes Tower Connection")
        compose.waitForText("Connect Hermes Tower")
        compose.onNodeWithText("Save and connect").performClick()
        compose.waitUntil(15_000) { E2E.graph().profileStore.load() != null }
        val profile = E2E.graph().profileStore.load()
        assertNotNull(profile)
        assertEquals("MOCK_SIMULATOR", profile!!.transportMode)
        assertTrue(profile.profileName.isNotBlank())
    }

    @Test
    fun e2e_09_10_mockConnectionTest_connectedOnlyAfterHandshake() {
        compose.openDrawerAndNavigate("Hermes Tower Connection")
        compose.waitForText("Connect Hermes Tower")
        compose.onNodeWithText("Test connection").performClick()
        compose.waitForText("Connection test: CONNECTION_PASSED")
        // CONNECTED appears only after the accepted handshake — drive the real bridge now.
        E2E.graph().realBridge.configure(E2E.graph().profileStore.load()!!)
        val result = runBlocking { E2E.graph().realBridge.handshake() }
        assertTrue(result is com.hermestower.voiceconsole.domain.BridgeResult.Success)
        assertEquals(ConnectionStatus.BridgeStatus.CONNECTED, E2E.graph().realBridge.connection.value.status)
        assertEquals(TowerConnectionTester.Outcome.CONNECTION_PASSED, E2E.graph().realBridge.lastTestReport()!!.outcome)
    }
}
