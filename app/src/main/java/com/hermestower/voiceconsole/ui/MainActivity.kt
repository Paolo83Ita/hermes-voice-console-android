package com.hermestower.voiceconsole.ui

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hermestower.voiceconsole.app.AppGraphHolder
import com.hermestower.voiceconsole.ui.runtime.MotionProfile
import com.hermestower.voiceconsole.ui.theme.HermesTheme

/**
 * Single-activity Compose app (ARCHITECTURE.md §8).
 * FLAG_KEEP_SCREEN_ON follows the Appearance toggle (FR-35) — the device is a
 * powered desk console; the Owner can still let it sleep.
 */
class MainActivity : ComponentActivity() {

    // G4-C08: per-app language override (Android 11/12 fallback path; 13+ uses
    // the official LocaleManager inside AppLanguage.apply).
    override fun attachBaseContext(newBase: android.content.Context) {
        super.attachBaseContext(com.hermestower.voiceconsole.locale.AppLanguage.wrapContext(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // CP-G4-016 — CI_LIFECYCLE_PROFILE (OWNER_DECISION_E2E_RECOVERY): the test
        // harness may launch this activity with the TEST_ONLY extra to freeze every
        // infinite ornamental frame clock while navigation, state machine,
        // controls, persistence, semantics and business logic stay identical.
        // DEBUG builds only: a release build can never enter this branch, so the
        // shipped application is unaffected.
        if (com.hermestower.voiceconsole.BuildConfig.DEBUG &&
            intent.getBooleanExtra(EXTRA_CI_LIFECYCLE_PROFILE, false)
        ) {
            MotionProfile.ciLifecycleProfile = true
        }
        val graph = AppGraphHolder.graph(this)
        setContent {
            val settings by graph.store.settings.collectAsStateWithLifecycle()
            DisposableEffect(settings.keepScreenOn) {
                if (settings.keepScreenOn) {
                    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                } else {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                }
                onDispose { }
            }
            HermesTheme(
                darkTheme = settings.darkTheme,
                reducedMotion = settings.reducedMotion || MotionProfile.ornamentsFrozen,
                animationIntensity = settings.animationIntensity,
            ) {
                AppNav(graph)
            }
        }
    }

    companion object {
        /** TEST_ONLY (debug builds): freezes infinite ornamental motion for the
         *  CI lifecycle/navigation qualification variant (CP-G4-016). */
        const val EXTRA_CI_LIFECYCLE_PROFILE = "hermes.ci_lifecycle_profile"
    }
}
