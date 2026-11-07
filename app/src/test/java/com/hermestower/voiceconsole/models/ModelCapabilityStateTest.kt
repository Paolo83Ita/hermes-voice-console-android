package com.hermestower.voiceconsole.models

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * GRAPH 4.12 §7 — MODEL CAPABILITY STATE MACHINE pins.
 * PHYS-FAIL-05: a downloaded component must never be treated as ACTIVE
 * without walking the runtime-truth ladder.
 */
class ModelCapabilityStateTest {

    @Test
    fun `the legal full ladder walks end to end`() = runBlocking {
        val ladder = CapabilityLadder()
        val id = "org.piper.voice.it-riccardo-x-low"
        val path = listOf(
            ModelCapabilityState.DOWNLOADING,
            ModelCapabilityState.DOWNLOADED,
            ModelCapabilityState.HASH_VERIFIED,
            ModelCapabilityState.INSTALLED,
            ModelCapabilityState.RUNTIME_AVAILABLE,
            ModelCapabilityState.LOADED,
            ModelCapabilityState.FUNCTIONAL_TEST_PASSED,
            ModelCapabilityState.ACTIVE,
            ModelCapabilityState.PHYSICAL_QUALIFICATION_PENDING,
            ModelCapabilityState.QUALIFIED,
        )
        for (target in path) {
            val r = ladder.transition(id, target)
            assertTrue("step $target refused: ${r.refusal}", r.accepted)
        }
        assertEquals(ModelCapabilityState.QUALIFIED, ladder.stateOf(id))
    }

    @Test
    fun `INSTALLED to ACTIVE jump is REFUSED (the PHYS-FAIL-05 class)`() = runBlocking {
        val ladder = CapabilityLadder()
        val id = "org.piper.voice.it-paola-medium"
        ladder.transition(id, ModelCapabilityState.DOWNLOADING)
        ladder.transition(id, ModelCapabilityState.DOWNLOADED)
        ladder.transition(id, ModelCapabilityState.HASH_VERIFIED)
        ladder.transition(id, ModelCapabilityState.INSTALLED)
        val jump = ladder.transition(id, ModelCapabilityState.ACTIVE)
        assertFalse("INSTALLED→ACTIVE must be refused", jump.accepted)
        assertEquals(
            ModelCapabilityTransitions.REFUSAL_NO_SKIP,
            jump.refusal?.substringBefore(":"),
        )
        assertEquals(ModelCapabilityState.INSTALLED, ladder.stateOf(id))
    }

    @Test
    fun `UI selection is NOT a state in this machine (no selection member exists)`() {
        // The machine's states contain no UI-selection concept; the selection
        // lives in VoiceSelectionAuthority and must walk THIS ladder to count.
        assertTrue(ModelCapabilityState.entries.none { it.name.contains("SELECT") })
    }

    @Test
    fun `restore rehydrates without inventing transitions`() = runBlocking {
        val ladder = CapabilityLadder()
        ladder.restore("x", ModelCapabilityState.LOADED)
        assertEquals(ModelCapabilityState.LOADED, ladder.stateOf("x"))
        val r = ladder.transition("x", ModelCapabilityState.ACTIVE)
        assertFalse(r.accepted) // LOADED → ACTIVE is illegal (test synthesis missing)
    }

    @Test
    fun `persist hook fires on every accepted transition`() = runBlocking {
        val persisted = mutableListOf<Pair<String, String>>()
        val ladder = CapabilityLadder(persist = { id, s -> persisted.add(id to s.name) })
        ladder.transition("comp", ModelCapabilityState.DOWNLOADING)
        ladder.transition("comp", ModelCapabilityState.DOWNLOADED)
        assertEquals(listOf("comp" to "DOWNLOADING", "comp" to "DOWNLOADED"), persisted)
    }
}
