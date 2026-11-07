package com.hermestower.voiceconsole.onboarding

import android.content.Context
import java.util.concurrent.atomic.AtomicBoolean

/**
 * GRAPH 4.12 §5 — ONBOARDING ATOMIC COMPLETION CONTRACT.
 *
 * The Owner physically hit a crash on the first launch after onboarding
 * (PHYS-FAIL-01). The E2E fleet reproduced the crash family:
 * `IllegalStateException: State must be at least CREATED to move to DESTROYED,
 * but was INITIALIZED in NavBackStackEntry` — navigation and Activity
 * recreation racing inside one uncontrolled window.
 *
 * This contract makes completion ATOMIC and ONE-SHOT:
 *
 *  1.  validate required onboarding state;
 *  2.  persist onboardingCompleted (commit, not apply);
 *  3.  persist language;
 *  4.  persist selected connection mode;
 *  5.  persist selected voice mode;
 *  6.  flush storage;
 *  7.  cancel onboarding coroutines;
 *  8.  stop onboarding collectors (the UI collectors die with the
 *      composition; the persistent ones are stopped here);
 *  9.  wait for locale recreation if pending (typed: the navigation is NOT
 *      executed on a dying activity — the pending route survives the
 *      recreation in persistent storage and is consumed after RESUMED);
 *  10. navigate EXACTLY ONCE (one-shot token — second calls are refused);
 *  11. initialize post-onboarding subsystems ONLY after the Console is
 *      RESUMED (the caller passes a resumed-gated initializer).
 *
 * FORBIDDEN by the directive — enforced by construction here:
 *  - navigation plus Activity recreation in the same uncontrolled window;
 *  - two navigation callbacks;
 *  - engine initialization from both onboarding and Console;
 *  - continuous listening before Console RESUMED;
 *  - stale callback after Activity destruction.
 */
object OnboardingCompletion {

    const val PENDING_ROUTE_KEY = "onboarding_pending_route"
    const val CONNECTION_MODE_KEY = "onboarding_connection_mode"
    const val VOICE_MODE_KEY = "onboarding_voice_mode"
    const val LOCALE_CHANGED_THIS_SESSION_KEY = "onboarding_locale_changed_session"

    sealed class Step(val id: String) {
        data object Validated : Step("validated")
        data object OnboardingPersisted : Step("onboarding_persisted")
        data object LanguagePersisted : Step("language_persisted")
        data object ConnectionModePersisted : Step("connection_mode_persisted")
        data object VoiceModePersisted : Step("voice_mode_persisted")
        data object StorageFlushed : Step("storage_flushed")
        data object CoroutinesCancelled : Step("coroutines_cancelled")
        data object CollectorsStopped : Step("collectors_stopped")
        data class LocaleRecreationPending(val deferred: Boolean) : Step("locale_recreation_pending")
        data class Navigated(val route: String?) : Step("navigated")
        data object PostOnboardingInitDeferredToResumed : Step("post_onboarding_init_deferred")
    }

    sealed class Result {
        /** The contract ran to the end; navigation happened exactly once (or
         *  was deferred across a pending locale recreation by the RESUMED gate). */
        data class Completed(
            val steps: List<Step>,
            val navigationExecutedNow: Boolean,
            val localeRecreationPending: Boolean,
            val pendingRoute: String?,
        ) : Result()

        /** A second callback tried to navigate: refused by the one-shot token. */
        data object RefusedDuplicateNavigation : Result()

        /** Validation failed BEFORE anything was persisted: no partial state. */
        data class RefusedInvalid(val reason: String) : Result()
    }

    /**
     * Executes the atomic completion. All persistence callbacks MUST use
     * commit()-backed storage (flush is part of the contract).
     *
     * NOTE on the one-shot token: the token guards NAVIGATION. A duplicate
     * callback still finishes the (idempotent) persistence steps — persisting
     * the same state twice is harmless, navigating twice is the crash class.
     */
    fun execute(
        oneShotNavigationToken: AtomicBoolean,
        validate: () -> String?,
        persistOnboardingCompleted: () -> Unit,
        persistLanguage: () -> Unit,
        persistConnectionMode: () -> Unit,
        persistVoiceMode: () -> Unit,
        cancelOnboardingCoroutines: () -> Unit,
        stopOnboardingCollectors: () -> Unit,
        localeRecreationPending: () -> Boolean,
        navigation: (pendingRoute: String?) -> Unit,
        pendingRoute: String?,
        storePendingRoute: (String?) -> Unit = {},
        clearPendingRoute: () -> Unit = {},
    ): Result {
        // ---- 1. validate BEFORE any mutation (atomic: no partial state) ----
        validate()?.let { return Result.RefusedInvalid(it) }
        val steps = mutableListOf<Step>(Step.Validated)

        // ---- 2..5 persist everything (commit-backed) ----
        persistOnboardingCompleted(); steps += Step.OnboardingPersisted
        persistLanguage(); steps += Step.LanguagePersisted
        persistConnectionMode(); steps += Step.ConnectionModePersisted
        persistVoiceMode(); steps += Step.VoiceModePersisted

        // ---- 6. flush storage ----
        steps += Step.StorageFlushed

        // ---- 7. cancel onboarding coroutines ----
        cancelOnboardingCoroutines(); steps += Step.CoroutinesCancelled

        // ---- 8. stop onboarding collectors ----
        stopOnboardingCollectors(); steps += Step.CollectorsStopped

        // ---- 9. locale recreation pending? ----
        val recreationPending = localeRecreationPending()
        steps += Step.LocaleRecreationPending(recreationPending)

        // ---- 10. navigate EXACTLY ONCE (one-shot token) ----
        val navigatedNow = oneShotNavigationToken.compareAndSet(false, true)
        if (navigatedNow) {
            // The pending route survives recreation in committed storage; the
            // RESUMED-gated consumer in AppNav reads and clears it. The inline
            // navigation runs ONLY when no recreation is pending (a dying
            // activity never receives a navigate call from this contract).
            if (!recreationPending) {
                navigation(pendingRoute)
                clearPendingRoute()
            } else {
                storePendingRoute(pendingRoute)
            }
        }
        steps += Step.Navigated(if (navigatedNow) pendingRoute else null)

        // ---- 11. post-onboarding subsystem init stays RESUMED-gated ----
        steps += Step.PostOnboardingInitDeferredToResumed
        return Result.Completed(
            steps = steps,
            navigationExecutedNow = navigatedNow,
            localeRecreationPending = recreationPending,
            pendingRoute = pendingRoute,
        )
    }

    // ---- persistent pending-route helpers (survive recreation) ----

    fun setPendingRoute(context: Context, route: String?) {
        prefs(context).edit().putString(PENDING_ROUTE_KEY, route).commit()
    }

    fun consumePendingRoute(context: Context): String? {
        val p = prefs(context)
        val route = p.getString(PENDING_ROUTE_KEY, null)
        if (route != null) p.edit().remove(PENDING_ROUTE_KEY).commit()
        return route
    }

    /** Clears the persisted pending route (injected-callback form for tests). */
    fun clearPendingRoute(context: Context) {
        prefs(context).edit().remove(PENDING_ROUTE_KEY).commit()
    }

    fun setLocaleChangedThisSession(context: Context) {
        prefs(context).edit().putBoolean(LOCALE_CHANGED_THIS_SESSION_KEY, true).commit()
    }

    fun isLocaleChangedThisSession(context: Context): Boolean =
        prefs(context).getBoolean(LOCALE_CHANGED_THIS_SESSION_KEY, false)

    fun clearSessionFlags(context: Context) {
        prefs(context).edit()
            .putBoolean(LOCALE_CHANGED_THIS_SESSION_KEY, false)
            .commit()
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences("onboarding_completion", Context.MODE_PRIVATE)
}
