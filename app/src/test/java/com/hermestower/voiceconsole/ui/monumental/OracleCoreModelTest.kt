package com.hermestower.voiceconsole.ui.monumental

import com.hermestower.voiceconsole.core.state.ConsoleState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * GRAPH 3 — OracleCoreModel truthfulness tests: every console state maps to a
 * complete visual parameter set; resting states respect the AMOLED budgets;
 * DISCONNECTED/DEGRADED/ERROR/PRIVACY_MUTED can never fake stability.
 */
class OracleCoreModelTest {

    @Test
    fun `all fifteen states map to params`() {
        for (state in ConsoleState.entries) {
            val p = OracleCoreModel.params(state)
            assertTrue("portal budget out of range for $state", p.portalAlpha in 0f..1f)
            assertTrue("conduit budget out of range for $state", p.conduitAlpha in 0f..1f)
            assertTrue("bronze budget out of range for $state", p.bronzeBudget in 0f..1f)
            assertTrue("dim out of range for $state", p.dim in 0f..1f)
        }
        assertEquals(ConsoleState.entries.size, 15)
    }

    @Test
    fun `resting states stay inside OLED idle budgets`() {
        val resting = setOf(
            ConsoleState.DORMANT, ConsoleState.IDLE, ConsoleState.DISCONNECTED,
            ConsoleState.PRIVACY_MUTED, ConsoleState.UPDATE_AVAILABLE,
        )
        for (state in resting) {
            val p = OracleCoreModel.params(state)
            assertTrue("$state portal must stay dim at rest", p.portalAlpha <= 0.32f)
            assertTrue("$state bronze must stay dim at rest", p.bronzeBudget <= 0.62f)
        }
        val dormant = OracleCoreModel.params(ConsoleState.DORMANT)
        assertTrue(dormant.portalAlpha <= 0.15f && dormant.conduitAlpha <= 0.1f)
    }

    @Test
    fun `disconnected shows a broken path and partial seal`() {
        val p = OracleCoreModel.params(ConsoleState.DISCONNECTED)
        assertEquals(MessengerMode.BROKEN, p.messenger)
        assertTrue(p.brokenPath)
        assertTrue(p.halfShutters)
        assertFalse(p.towerLink)
    }

    @Test
    fun `degraded keeps a visible but dashed link`() {
        val p = OracleCoreModel.params(ConsoleState.DEGRADED)
        assertEquals(MessengerMode.DEGRADED, p.messenger)
        assertTrue(p.dashedConduits)
    }

    @Test
    fun `error shows the fracture without fake stability`() {
        val p = OracleCoreModel.params(ConsoleState.ERROR)
        assertTrue(p.fracture)
        assertEquals(MessengerMode.NONE, p.messenger)
    }

    @Test
    fun `privacy muted closes every input gate`() {
        val p = OracleCoreModel.params(ConsoleState.PRIVACY_MUTED)
        assertTrue(p.shutters)
        assertTrue(p.privacyRing)
        assertTrue(p.conduitAlpha <= 0.1f)
        assertEquals(MessengerMode.NONE, p.messenger)
    }

    @Test
    fun `local understanding never fakes a tower link`() {
        val p = OracleCoreModel.params(ConsoleState.UNDERSTANDING_LOCAL)
        assertTrue(p.localCorePlate)
        assertFalse(p.towerLink)
        assertEquals(MessengerMode.LOCAL_ORBIT, p.messenger)
    }

    @Test
    fun `tower processing keeps transport visible with differentiated rings`() {
        val p = OracleCoreModel.params(ConsoleState.TOWER_PROCESSING)
        assertTrue(p.towerLink)
        assertTrue(p.secondTrail)
        assertEquals(MessengerMode.DEPARTURE, p.messenger)
        val a = p.ringADegrees
        val b = p.ringBDegrees
        assertTrue("rings must rotate in opposite senses", a * b < 0f)
    }

    @Test
    fun `queued state carries the amber seal flag`() {
        val p = OracleCoreModel.params(ConsoleState.REQUEST_QUEUED)
        assertTrue(p.queueSeal)
    }

    @Test
    fun `reduced motion freezes to sample frames`() {
        val animated = OracleCoreModel.ringAPhase(ConsoleState.TOWER_PROCESSING, 12_345L, false, VisualQuality.BALANCED)
        val frozen = OracleCoreModel.ringAPhase(ConsoleState.TOWER_PROCESSING, 12_345L, true, VisualQuality.BALANCED)
        val low = OracleCoreModel.ringAPhase(ConsoleState.TOWER_PROCESSING, 12_345L, false, VisualQuality.LOW)
        assertEquals(OracleCoreModel.params(ConsoleState.TOWER_PROCESSING).ringADegrees, frozen, 0.001f)
        assertEquals(frozen, low, 0.001f)
        assertTrue(animated != frozen)
    }

    @Test
    fun `quality budgets respect the profile contract`() {
        val low = QualityBudgets.of(VisualQuality.LOW)
        val balanced = QualityBudgets.of(VisualQuality.BALANCED)
        val high = QualityBudgets.of(VisualQuality.HIGH)
        assertEquals(0, low.particleCount)
        assertFalse(low.glowEnabled)
        assertTrue(balanced.glowEnabled && balanced.particleCount in 1..11)
        assertTrue(high.particleCount > balanced.particleCount)
        assertTrue(high.ringLayers >= balanced.ringLayers)
    }

    @Test
    fun `thermal ladder downgrades only`() {
        assertEquals(VisualQuality.HIGH, ThermalVisualPolicy.apply(ThermalStage.NOMINAL, VisualQuality.HIGH))
        assertEquals(VisualQuality.BALANCED, ThermalVisualPolicy.apply(ThermalStage.ELEVATED, VisualQuality.HIGH))
        assertEquals(VisualQuality.LOW, ThermalVisualPolicy.apply(ThermalStage.CRITICAL, VisualQuality.HIGH))
        assertEquals(VisualQuality.LOW, ThermalVisualPolicy.apply(ThermalStage.CRITICAL, VisualQuality.BALANCED))
        assertEquals(VisualQuality.LOW, ThermalVisualPolicy.apply(ThermalStage.ELEVATED, VisualQuality.LOW))
    }

    @Test
    fun `visual quality parses tolerantly`() {
        assertEquals(VisualQuality.BALANCED, VisualQuality.from(null))
        assertEquals(VisualQuality.BALANCED, VisualQuality.from("garbage"))
        assertEquals(VisualQuality.LOW, VisualQuality.from("low"))
        assertEquals(VisualQuality.HIGH, VisualQuality.from("HIGH"))
    }
}
