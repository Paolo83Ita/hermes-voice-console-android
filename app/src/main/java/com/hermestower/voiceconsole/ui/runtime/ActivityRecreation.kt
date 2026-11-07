package com.hermestower.voiceconsole.ui.runtime

import android.app.Activity
import android.os.Handler
import android.os.Looper

/**
 * GRAPH 4.12 §5 — SINGLE recreation scheduler.
 *
 * The proven first-run crash family is navigation + Activity recreation
 * racing in one uncontrolled window (and, on Android 13+, the DOUBLE
 * recreate: LocaleManager's automatic one plus the manual `recreate()` call).
 *
 * Every manual recreation in the product now goes through this ONE scheduler:
 *  - posted to the NEXT frame (never inside the click/navigation handler);
 *  - skipped when the activity is already finishing/destroyed;
 *  - never called when the official per-app locale mechanism (API 33+) will
 *    recreate on its own.
 */
object ActivityRecreation {

    fun recreateNextFrame(activity: Activity) {
        Handler(Looper.getMainLooper()).post {
            if (!activity.isFinishing && !activity.isDestroyed) {
                activity.recreate()
            }
        }
    }
}
