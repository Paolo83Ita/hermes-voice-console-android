package com.hermestower.voiceconsole.onboarding

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.atomic.AtomicBoolean

/**
 * GRAPH 4.12 §5 — ONBOARDING ATOMIC COMPLETION CONTRACT (JVM pins).
 *
 * The proven crash family (NavBackStackEntry INITIALIZED→DESTROYED under a
 * racing navigation+recreation window) is fenced by these pins: one-shot
 * navigation, recreation deferral, validation-before-persist.
 */
class OnboardingCompletionTest {

    private class FakeStore {
        val flags = LinkedHashSet<String>()
    }

    private fun execute(
        store: FakeStore,
        token: AtomicBoolean,
        validateError: String? = null,
        recreationPending: Boolean = false,
        pendingRoute: String? = "MODEL_MANAGER",
        navigated: MutableList<String?> = mutableListOf(),
        storedRoutes: MutableList<String?> = mutableListOf(),
    ): OnboardingCompletion.Result = OnboardingCompletion.execute(
        oneShotNavigationToken = token,
        validate = { validateError },
        persistOnboardingCompleted = { store.flags.add("onboarding") },
        persistLanguage = { store.flags.add("language") },
        persistConnectionMode = { store.flags.add("connection") },
        persistVoiceMode = { store.flags.add("voice") },
        cancelOnboardingCoroutines = { store.flags.add("coroutines") },
        stopOnboardingCollectors = { store.flags.add("collectors") },
        localeRecreationPending = { recreationPending },
        navigation = { route -> navigated.add(route) },
        pendingRoute = pendingRoute,
        storePendingRoute = { r -> storedRoutes.add(r) },
        clearPendingRoute = { storedRoutes.add(null) },
    )

    @Test
    fun `happy path navigates exactly once in contract order`() {
        val store = FakeStore()
        val token = AtomicBoolean(false)
        val navigated = mutableListOf<String?>()
        val result = execute(store, token, navigated = navigated)
        result as OnboardingCompletion.Result.Completed
        assertTrue(result.navigationExecutedNow)
        val ids = result.steps.map { it.id }
        assertEquals(
            listOf(
                "validated", "onboarding_persisted", "language_persisted",
                "connection_mode_persisted", "voice_mode_persisted", "storage_flushed",
                "coroutines_cancelled", "collectors_stopped", "locale_recreation_pending",
                "navigated", "post_onboarding_init_deferred",
            ),
            ids,
        )
        assertEquals(setOf("onboarding", "language", "connection", "voice", "coroutines", "collectors"), store.flags)
    }

    @Test
    fun `duplicate callback cannot navigate again (one-shot token)`() {
        val store = FakeStore()
        val token = AtomicBoolean(false)
        val navigated = mutableListOf<String?>()
        execute(store, token, navigated = navigated)
        val second = execute(store, token, navigated = navigated)
        second as OnboardingCompletion.Result.Completed
        assertFalse(second.navigationExecutedNow)
        assertEquals(1, navigated.size)
    }

    @Test
    fun `pending locale recreation defers navigation to the RESUMED consumer`() {
        val store = FakeStore()
        val token = AtomicBoolean(false)
        val navigated = mutableListOf<String?>()
        val storedRoutes = mutableListOf<String?>()
        val result = execute(store, token, recreationPending = true, navigated = navigated, storedRoutes = storedRoutes)
        result as OnboardingCompletion.Result.Completed
        assertTrue(result.localeRecreationPending)
        assertEquals(0, navigated.size) // no navigate on a (possibly dying) activity
        assertEquals(listOf("MODEL_MANAGER"), storedRoutes) // route survives in committed storage
    }

    @Test
    fun `validation failure persists NOTHING and navigates NEVER`() {
        val store = FakeStore()
        val token = AtomicBoolean(false)
        val navigated = mutableListOf<String?>()
        val result = execute(store, token, validateError = "wizard not complete", navigated = navigated)
        assertTrue(result is OnboardingCompletion.Result.RefusedInvalid)
        assertEquals(0, store.flags.size)
        assertEquals(0, navigated.size)
        assertFalse(token.get())
    }
}
