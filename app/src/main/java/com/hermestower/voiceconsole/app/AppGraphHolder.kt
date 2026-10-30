package com.hermestower.voiceconsole.app

import android.content.Context

/** process-wide access to the AppGraph created by HermesApp (kept out of Compose code). */
object AppGraphHolder {

    @Volatile
    private var cached: AppGraph? = null

    fun graph(context: Context): AppGraph =
        cached ?: (context.applicationContext as HermesApp).graph.also { cached = it }
}
