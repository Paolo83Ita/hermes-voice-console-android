package com.hermestower.voiceconsole.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * GRAPH 4.12 §3/§4 — FIRST-RUN CRASH EVIDENCE + SAFE MODE (pure-logic pins).
 * The receipt round-trip itself is device-side; the JVM pins guard the typed
 * verdicts and the safe-mode decision core.
 */
class FirstRunCrashEvidenceAndSafeModeTest {

    @Test
    fun `isFirstRunCrash requires onboarding context in the receipt`() {
        val base = FirstRunCrashEvidence.CrashReceipt(
            context = FirstRunCrashEvidence.ContextSnapshot(sessionId = "s1"),
            exceptionClass = "java.lang.IllegalStateException",
            stackTrace = "at androidx.navigation.NavBackStackEntry",
            thread = "main",
            buildIdentity = "v0.3.5 (8)",
            deviceModel = "POCO X6 Pro",
            androidVersion = "35",
            crashAt = java.time.Instant.EPOCH,
        )
        // A crash with NO onboarding context is not a first-run crash.
        assertFalse(FirstRunCrashEvidence.isFirstRunCrash(base))
        // Onboarding step >= 0 → first-run window.
        val withStep = base.copy(context = base.context.copy(onboardingStep = 5))
        assertTrue(FirstRunCrashEvidence.isFirstRunCrash(withStep))
        // Completion timestamp → first-run window (the Owner's exact case).
        val withCompletion = base.copy(
            context = base.context.copy(onboardingCompletedAt = "2026-11-08T09:00:00Z"),
        )
        assertTrue(FirstRunCrashEvidence.isFirstRunCrash(withCompletion))
    }

    @Test
    fun `session snapshot updates are thread-safe value swaps`() {
        val before = FirstRunCrashEvidence.current()
        val s1 = before.sessionId
        FirstRunCrashEvidence.update { it.copy(onboardingStep = 3, destinationRoute = "CONSOLE") }
        val after = FirstRunCrashEvidence.current()
        assertEquals(3, after.onboardingStep)
        assertEquals("CONSOLE", after.destinationRoute)
        assertEquals(s1, after.sessionId) // session identity survives updates
        // restore the neutral snapshot for other tests / future installs
        FirstRunCrashEvidence.update { it.copy(onboardingStep = -1, destinationRoute = null) }
        assertNull(FirstRunCrashEvidence.current().destinationRoute)
    }

    @Test
    fun `§4 safe-mode decision core`() {
        // Safe mode OFF → nothing refused.
        assertFalse(
            FirstRunSafeMode.mustRefuse(
                FirstRunSafeMode.Subsystem.TTS,
                safeModeEnabled = false,
                manuallyInitialized = false,
            ),
        )
        // Safe mode ON + not manually initialized → REFUSE automatic start.
        assertTrue(
            FirstRunSafeMode.mustRefuse(
                FirstRunSafeMode.Subsystem.TTS,
                safeModeEnabled = true,
                manuallyInitialized = false,
            ),
        )
        // Safe mode ON + the Owner initialized THIS subsystem manually → allow.
        assertFalse(
            FirstRunSafeMode.mustRefuse(
                FirstRunSafeMode.Subsystem.TTS,
                safeModeEnabled = true,
                manuallyInitialized = true,
            ),
        )
        // The other subsystems stay refused until individually initialized.
        assertTrue(
            FirstRunSafeMode.mustRefuse(
                FirstRunSafeMode.Subsystem.SHERPA_TTS,
                safeModeEnabled = true,
                manuallyInitialized = false,
            ),
        )
    }

    @Test
    fun `§4 the safe-mode subsystem list covers every automatic engine`() {
        val ids = FirstRunSafeMode.Subsystem.entries.map { it.id }.toSet()
        assertEquals(
            setOf(
                "tts", "vosk_stt", "sherpa_tts", "continuous_listening",
                "wake_word", "model_download", "qr_camera",
            ),
            ids,
        )
    }

    @Test
    fun `§3 snapshot carries every §3 contract field`() {
        val s = FirstRunCrashEvidence.ContextSnapshot(
            sessionId = "s-x",
            onboardingStep = 2,
            onboardingCompletedAt = "t",
            destinationRoute = "CONSOLE",
            backStackSummary = "CONSOLE",
            effectiveLocale = "it",
            pendingLocaleRecreation = true,
            ttsState = "INITIALIZING",
            sttState = "NOT_STARTED",
            continuousListeningState = "OFF",
            modelInstallState = "NONE",
            lastLifecycleEvent = "ON_RESUME",
            lastSafeCheckpoint = "onboarding-step-2",
        )
        assertEquals("it", s.effectiveLocale)
        assertTrue(s.pendingLocaleRecreation)
        assertEquals("ON_RESUME", s.lastLifecycleEvent)
    }
}
