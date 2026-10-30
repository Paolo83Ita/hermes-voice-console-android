package com.hermestower.voiceconsole.app

import android.app.Application

/** Application entry: constructs the AppGraph once and boots the V0.1 simulation. */
class HermesApp : Application() {

    lateinit var graph: AppGraph
        private set

    override fun onCreate() {
        super.onCreate()
        graph = AppGraph(this)
        graph.start()
    }
}
