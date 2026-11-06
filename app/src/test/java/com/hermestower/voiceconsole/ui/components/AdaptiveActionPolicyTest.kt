package com.hermestower.voiceconsole.ui.components

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * OWNER PHYSICAL RETEST §8/§11: the adaptive action policy — the POCO's
 * ≈372 dp class is CONSTRAINED (wrap), wide surfaces share one row. Pure JVM.
 */
class AdaptiveActionPolicyTest {

    @Test
    fun `the poco width class is constrained (wrap)`() {
        assertEquals(AdaptiveActionPolicy.Shape.WRAPPED_PRIMARY, AdaptiveActionPolicy.shape(360f))
        assertEquals(AdaptiveActionPolicy.Shape.WRAPPED_PRIMARY, AdaptiveActionPolicy.shape(393f))
        assertEquals(AdaptiveActionPolicy.Shape.WRAPPED_PRIMARY, AdaptiveActionPolicy.shape(411f))
    }

    @Test
    fun `wide surfaces share a row`() {
        assertEquals(AdaptiveActionPolicy.Shape.SINGLE_ROW, AdaptiveActionPolicy.shape(480f))
        assertEquals(AdaptiveActionPolicy.Shape.SINGLE_ROW, AdaptiveActionPolicy.shape(600f))
    }
}
