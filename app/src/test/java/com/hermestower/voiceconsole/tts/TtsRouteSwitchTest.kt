package com.hermestower.voiceconsole.tts

import com.hermestower.voiceconsole.domain.TextToSpeechEngine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * CP-G4.9-003 route-truth tests: the console speaks through the natural voice
 * ONLY while the registry reports it installed+verified+active; removal or
 * deactivation returns the console to the Android system route on the next
 * utterance; a route change never leaves another engine speaking.
 */
class TtsRouteSwitchTest {

    /** Minimal fake domain engine recording speak/interrupt calls. */
    private class FakeDomainEngine : TextToSpeechEngine {
        val _state = MutableStateFlow(TextToSpeechEngine.State.NOT_READY)
        override val state: StateFlow<TextToSpeechEngine.State> = _state
        var speakCalls = 0
        var interrupts = 0
        override fun speak(text: String, rate: Float): Flow<Float> {
            speakCalls++
            return kotlinx.coroutines.flow.flowOf(0.5f)
        }

        override fun interrupt() {
            interrupts++
        }
    }

    @Test
    fun `routes to natural only while the registry reports it active`() = runTest {
        val system = FakeDomainEngine()
        val natural = FakeDomainEngine()
        var naturalActive: String? = "org.piper.voice.it-riccardo-x-low"
        val switch = TtsRouteSwitch(
            systemRoute = system,
            naturalRoutes = mapOf("org.piper.voice.it-riccardo-x-low" to natural),
            routeProvider = { naturalActive },
        )

        val a = switch.speak("ciao", 1f).toList()
        assertEquals(1, natural.speakCalls)
        assertEquals(0, system.speakCalls)
        assertEquals(listOf(0.5f), a)

        // Deactivation: the SAME switch falls back honestly to the system route.
        naturalActive = null
        switch.speak("ciao", 1f).toList()
        assertEquals(1, natural.speakCalls)
        assertEquals(1, system.speakCalls)
        assertEquals("SYSTEM", switch.lastRouteUsed)
    }

    @Test
    fun `unknown active identity falls back to system`() = runTest {
        val system = FakeDomainEngine()
        val natural = FakeDomainEngine()
        val switch = TtsRouteSwitch(
            systemRoute = system,
            naturalRoutes = mapOf("org.piper.voice.it-riccardo-x-low" to natural),
            routeProvider = { "org.piper.voice.unknown" },
        )
        switch.speak("ciao", 1f).toList()
        assertEquals(0, natural.speakCalls)
        assertEquals(1, system.speakCalls)
    }

    @Test
    fun `interrupt stops every route and route change interrupts the idle engine`() = runTest {
        val system = FakeDomainEngine()
        val natural = FakeDomainEngine()
        var naturalActive: String? = "org.piper.voice.it-riccardo-x-low"
        val switch = TtsRouteSwitch(
            systemRoute = system,
            naturalRoutes = mapOf("org.piper.voice.it-riccardo-x-low" to natural),
            routeProvider = { naturalActive },
        )
        switch.speak("ciao", 1f).toList()
        // While the natural route spoke, the system engine was interrupted once
        // (never speaking behind the console's back).
        assertTrue(system.interrupts >= 1)
        switch.interrupt()
        assertEquals(1, natural.interrupts)
        assertEquals(2, system.interrupts)
    }
}
