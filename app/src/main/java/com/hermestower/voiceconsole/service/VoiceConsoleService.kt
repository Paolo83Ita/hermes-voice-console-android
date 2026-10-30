package com.hermestower.voiceconsole.service

import android.app.Service
import android.content.Intent
import android.os.IBinder

/**
 * V0.1 skeleton only (decision D-011): registered, never started.
 * The real foreground service (microphone type, notification, MIUI survival policy)
 * is specified in DEVELOPMENT_ROADMAP.md V0.2 and FR-45.
 * Starting this service in V0.1 would imply a voice pipeline that does not exist yet.
 */
class VoiceConsoleService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        // V0.2+: startForeground with a quiet notification; wire to ScriptedConsoleDriver successor.
    }
}
