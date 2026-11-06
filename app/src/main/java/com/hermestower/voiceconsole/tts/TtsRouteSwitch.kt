package com.hermestower.voiceconsole.tts

import com.hermestower.voiceconsole.domain.TextToSpeechEngine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * CP-G4.9-003 — the console TTS ROUTE between the REAL engines:
 *
 *  - SYSTEM: the Android system TTS (mandatory fallback, fail-closed Italian);
 *  - NATURAL: a sherpa-onnx piper Italian voice delivered through the Model
 *    Sanctum (real downloadable natural Italian voice, one engine per voice).
 *
 * Routing truth (never invented): a natural route is ACTIVE only while its
 * voice bundle is installed, integrity-verified AND activated in the Model
 * Installation Registry (routeProvider returns it). Removing or deactivating
 * the voice returns the console to the system route on the next utterance —
 * never a silent swap mid-utterance (an utterance in flight on the natural
 * route is interrupted on route change).
 *
 * The amplitude flow of the ACTIVE route is forwarded verbatim; interrupt()
 * stops ALL routes (so a route change can never leave one engine speaking
 * behind the console's back).
 */
class TtsRouteSwitch(
    private val systemRoute: TextToSpeechEngine,
    /** The natural routes by identity (voice id → its domain adapter). */
    private val naturalRoutes: Map<String, TextToSpeechEngine>,
    /** The active natural voice id, or null when the system route applies. */
    private val routeProvider: () -> String?,
) : TextToSpeechEngine {

    /** Last route actually used (Diagnostics read-back; starts SYSTEM). */
    @Volatile
    var lastRouteUsed: String = "SYSTEM"
        private set

    override val state: StateFlow<TextToSpeechEngine.State>
        get() = activeRoute().state

    private fun activeRoute(): TextToSpeechEngine {
        val id = routeProvider()
        return if (id != null) naturalRoutes[id] ?: systemRoute else systemRoute
    }

    override fun speak(text: String, rate: Float): Flow<Float> {
        val id = routeProvider()
        val route = if (id != null) naturalRoutes[id] ?: systemRoute else systemRoute
        lastRouteUsed = if (id != null && naturalRoutes.containsKey(id)) "NATURAL:$id" else "SYSTEM"
        // Route change mid-utterance: no OTHER engine may keep speaking
        // behind the console's back.
        naturalRoutes.values.asSequence().plus(systemRoute)
            .filter { it !== route }
            .forEach { it.interrupt() }
        return route.speak(text, rate)
    }

    override fun interrupt() {
        naturalRoutes.values.forEach { it.interrupt() }
        systemRoute.interrupt()
    }
}
