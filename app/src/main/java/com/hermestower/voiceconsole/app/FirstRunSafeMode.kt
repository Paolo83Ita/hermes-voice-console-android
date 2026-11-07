package com.hermestower.voiceconsole.app

import android.content.Context
import java.util.Properties

/**
 * GRAPH 4.12 §4 — FIRST_RUN_SAFE_MODE.
 *
 * After a first-run crash the recovery dialog can start the app in safe mode:
 * the Console text path and diagnostics stay available, but NO subsystem that
 * could participate in the crash starts automatically:
 *   - no automatic TTS initialization;
 *   - no automatic Vosk load;
 *   - no automatic Sherpa load;
 *   - no continuous listening;
 *   - no wake word;
 *   - no model download;
 *   - no QR camera.
 * Each subsystem can then be initialized SEPARATELY from the recovery panel —
 * the first subsystem that crashes again is the proven culprit (evidence, not
 * guessing).
 */
object FirstRunSafeMode {

    private const val PREFS = "first_run_safe_mode"
    private const val KEY_ENABLED = "enabled"

    /** §4 subsystem list — each can be initialized individually in safe mode. */
    enum class Subsystem(val id: String) {
        TTS("tts"),
        VOSK_STT("vosk_stt"),
        SHERPA_TTS("sherpa_tts"),
        CONTINUOUS_LISTENING("continuous_listening"),
        WAKE_WORD("wake_word"),
        MODEL_DOWNLOAD("model_download"),
        QR_CAMERA("qr_camera"),
    }

    private val manuallyInitialized = java.util.concurrent.ConcurrentHashMap.newKeySet<String>()

    fun enable(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_ENABLED, true).commit()
    }

    fun disable(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_ENABLED, false).commit()
        manuallyInitialized.clear()
    }

    fun isEnabled(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY_ENABLED, false)

    /**
     * The core safe-mode gate. TRUE means: in safe mode AND the subsystem was
     * NOT yet manually initialized — automatic start must be REFUSED.
     */
    fun mustRefuseAutomaticStart(context: Context, subsystem: Subsystem): Boolean =
        mustRefuse(subsystem, isEnabled(context), manuallyInitialized.contains(subsystem.id))

    /** Pure decision core (JVM-testable, no Context). */
    fun mustRefuse(subsystem: Subsystem, safeModeEnabled: Boolean, manuallyInitialized: Boolean): Boolean =
        safeModeEnabled && !manuallyInitialized

    /** Owner-driven single-subsystem initialization from the recovery panel. */
    fun markManuallyInitialized(subsystem: Subsystem) {
        manuallyInitialized.add(subsystem.id)
    }

    fun manuallyInitializedIds(): Set<String> = manuallyInitialized.toSet()

    /** Typed per-subsystem init attempt result (diagnostics-grade honesty). */
    data class InitAttempt(
        val subsystem: Subsystem,
        val startedAtMillis: Long,
        val success: Boolean,
        val errorClass: String?,
        val durationMillis: Long,
    )

    /**
     * Runs ONE subsystem initializer with typed capture. The initializer runs
     * inside [block]; any throwable is captured into the attempt (a crash in
     * one subsystem must never take the safe-mode console down).
     */
    fun initializeSubsystem(
        subsystem: Subsystem,
        block: () -> Unit,
    ): InitAttempt {
        val start = System.currentTimeMillis()
        return try {
            block()
            markManuallyInitialized(subsystem)
            InitAttempt(subsystem, start, true, null, System.currentTimeMillis() - start)
        } catch (t: Throwable) {
            InitAttempt(subsystem, start, false, t.javaClass.name, System.currentTimeMillis() - start)
        }
    }

    /** Persist the attempt log (secret-clean: subsystem ids + outcomes only). */
    fun persistAttempts(context: Context, attempts: List<InitAttempt>) {
        val props = Properties()
        attempts.forEachIndexed { i, a ->
            props.setProperty("attempt.$i.subsystem", a.subsystem.id)
            props.setProperty("attempt.$i.success", a.success.toString())
            props.setProperty("attempt.$i.error_class", a.errorClass ?: "")
            props.setProperty("attempt.$i.duration_ms", a.durationMillis.toString())
        }
        val f = java.io.File(context.filesDir, "crash_receipts/safe_mode_attempts.properties")
        f.parentFile?.mkdirs()
        f.outputStream().use { props.store(it, "safe-mode subsystem init attempts (secret-clean)") }
    }
}
