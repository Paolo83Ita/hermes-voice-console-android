package com.hermestower.voiceconsole.routing

import com.hermestower.voiceconsole.core.mode.OperatingMode
import com.hermestower.voiceconsole.core.routing.Connectivity
import com.hermestower.voiceconsole.core.routing.RoutingAction
import com.hermestower.voiceconsole.core.routing.RuleBasedIntentRouter
import com.hermestower.voiceconsole.core.routing.RoutingRequest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RuleBasedIntentRouterTest {

    private val router = RuleBasedIntentRouter()

    private fun req(
        text: String,
        mode: OperatingMode = OperatingMode.HYBRID,
        connectivity: Connectivity = Connectivity.OK,
        confidence: Float = 0.9f,
        effective: OperatingMode? = null,
    ) = RoutingRequest(text, mode, effective ?: mode, connectivity, confidence)

    @Test
    fun `OFF declines everything`() {
        val d = router.route(req("status report", OperatingMode.OFF))
        assertEquals(RoutingAction.DECLINE, d.action)
        assertFalse(d.consequential)
    }

    @Test
    fun `LOCAL handles whitelist and declines the rest`() {
        assertEquals(RoutingAction.HANDLE_LOCAL, router.route(req("good morning")).action)
        assertEquals(RoutingAction.HANDLE_LOCAL, router.route(req("what is the battery status")).action)
        assertEquals(RoutingAction.HANDLE_LOCAL, router.route(req("chi sei")).action)
        assertEquals(RoutingAction.DECLINE, router.route(req("prepare tomorrow mission plan", OperatingMode.LOCAL)).action)
        assertEquals(RoutingAction.DECLINE, router.route(req("what is the weather", OperatingMode.LOCAL)).action)
    }

    @Test
    fun `TOWER routes everything except offline queueing rules`() {
        assertEquals(RoutingAction.ROUTE_TOWER, router.route(req("hello there", OperatingMode.TOWER)).action)
        assertEquals(RoutingAction.ROUTE_TOWER, router.route(req("status", OperatingMode.TOWER)).action)
    }

    @Test
    fun `HYBRID resolves whitelisted locally and forwards the rest`() {
        val local = router.route(req("ciao hermes"))
        assertEquals(RoutingAction.HANDLE_LOCAL, local.action)
        val tower = router.route(req("what is the weather tomorrow"))
        assertEquals(RoutingAction.ROUTE_TOWER, tower.action)
        assertFalse(tower.consequential)
    }

    @Test
    fun `consequential requests never resolve locally and never queue offline`() {
        val hybrid = router.route(req("execute the deployment plan"))
        assertEquals(RoutingAction.ROUTE_TOWER, hybrid.action)
        assertTrue(hybrid.consequential)

        val offline = router.route(
            req("execute the deployment plan", connectivity = Connectivity.DISCONNECTED, effective = OperatingMode.DISCONNECTED_SAFE),
        )
        assertEquals(RoutingAction.DECLINE, offline.action)
        assertTrue(offline.consequential)

        val local = router.route(req("delete the archive", OperatingMode.LOCAL))
        assertEquals(RoutingAction.DECLINE, local.action)
        assertTrue(local.consequential)
    }

    @Test
    fun `DISCONNECTED_SAFE queues non-consequential and serves whitelist locally`() {
        val queued = router.route(
            req("what is the weather", OperatingMode.TOWER, Connectivity.DISCONNECTED, effective = OperatingMode.DISCONNECTED_SAFE),
        )
        assertEquals(RoutingAction.QUEUE_FOR_TOWER, queued.action)
        assertFalse(queued.consequential)

        val local = router.route(
            req("battery status", OperatingMode.TOWER, Connectivity.DISCONNECTED, effective = OperatingMode.DISCONNECTED_SAFE),
        )
        assertEquals(RoutingAction.HANDLE_LOCAL, local.action)
    }

    @Test
    fun `low confidence escalates whitelisted intents to the Tower in HYBRID`() {
        val d = router.route(req("gibberish status", confidence = 0.3f))
        assertEquals(RoutingAction.ROUTE_TOWER, d.action)
        assertTrue(d.reason.contains("confidence"))
    }

    @Test
    fun `italian whitelist phrases resolve locally`() {
        assertEquals(RoutingAction.HANDLE_LOCAL, router.route(req("buongiorno")).action)
        assertEquals(RoutingAction.HANDLE_LOCAL, router.route(req("che ora è?")).action)
        assertEquals(RoutingAction.HANDLE_LOCAL, router.route(req("grazie mille")).action)
    }

    @Test
    fun `empty text declines`() {
        assertEquals(RoutingAction.DECLINE, router.route(req("   ")).action)
    }
}
