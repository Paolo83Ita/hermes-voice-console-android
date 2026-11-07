package com.hermestower.voiceconsole.app

import android.app.Application

/** Application entry: constructs the AppGraph once and boots the V0.1 simulation. */
class HermesApp : Application() {

    lateinit var graph: AppGraph
        private set

    override fun onCreate() {
        super.onCreate()
        // GRAPH 4.12 §3: the crash-evidence hook is installed BEFORE any graph
        // work — the receipt captures the freshest possible process state.
        FirstRunCrashEvidence.install(this) {
            "v" + com.hermestower.voiceconsole.BuildConfig.VERSION_NAME +
                " (" + com.hermestower.voiceconsole.BuildConfig.VERSION_CODE + ")" +
                " commit=" + (BuildIdentity.injected().gitShaShort.ifBlank { "unrecorded" })
        }
        // GRAPH 4.12 §4: FIRST_RUN_SAFE_MODE survives process death (committed prefs).
        graph = AppGraph(this, firstRunSafeMode = FirstRunSafeMode.isEnabled(this))
        graph.start()
    }
}
