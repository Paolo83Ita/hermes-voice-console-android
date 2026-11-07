package com.hermestower.voiceconsole.app

import android.content.Context
import java.io.File
import java.time.Instant
import java.util.Properties

/**
 * GRAPH 4.12 §3 — FIRST-RUN CRASH EVIDENCE SYSTEM.
 *
 * Purpose: the Owner reported a crash on first launch after onboarding
 * (PHYS-FAIL-01). This subsystem NEVER guesses the root cause: it records a
 * typed, secret-clean receipt of the process state AROUND the crash, then
 * persists it before the process terminates wherever possible.
 *
 * Secret-clean contract (§3): NO user text, NO audio, NO secrets, NO full
 * shared-preferences dumps. Only classes, states, counts, timestamps,
 * locale codes, build identity and the JVM stack trace (class/method lines —
 * the VM never puts user content in stack frames).
 *
 * The receipt is a Java Properties file (not JSON): it round-trips on both
 * Android and the JVM unit-test host without any parser dependency.
 */
object FirstRunCrashEvidence {

    private const val DIR = "crash_receipts"
    private const val FILE = "last_crash.properties"

    // ---- keys (stable contract; E2E and diagnostics read these) ----
    const val K_SESSION_ID = "session.id"
    const val K_ONBOARDING_STEP = "onboarding.step"
    const val K_ONBOARDING_COMPLETED_AT = "onboarding.completed_at"
    const val K_DESTINATION_ROUTE = "navigation.destination_route"
    const val K_BACKSTACK_SUMMARY = "navigation.backstack_summary"
    const val K_EFFECTIVE_LOCALE = "locale.effective"
    const val K_PENDING_LOCALE_RECREATION = "locale.pending_recreation"
    const val K_TTS_STATE = "engines.tts_state"
    const val K_STT_STATE = "engines.stt_state"
    const val K_CONTINUOUS_LISTENING = "engines.continuous_listening_state"
    const val K_MODEL_INSTALL_STATE = "models.install_state"
    const val K_LAST_LIFECYCLE_EVENT = "lifecycle.last_event"
    const val K_LAST_SAFE_CHECKPOINT = "lifecycle.last_safe_checkpoint"
    const val K_FATAL_EXCEPTION_CLASS = "fatal.exception_class"
    const val K_FATAL_STACK = "fatal.stack"
    const val K_FATAL_THREAD = "fatal.thread"
    const val K_BUILD_IDENTITY = "build.identity"
    const val K_DEVICE_MODEL = "device.model"
    const val K_ANDROID_VERSION = "device.android_version"
    const val K_CRASH_AT = "fatal.crash_at"

    /** Live process context captured by the app shell, refreshed at boundaries. */
    data class ContextSnapshot(
        val sessionId: String,
        val onboardingStep: Int = -1,
        val onboardingCompletedAt: String? = null,
        val destinationRoute: String? = null,
        val backStackSummary: String? = null,
        val effectiveLocale: String? = null,
        val pendingLocaleRecreation: Boolean = false,
        val ttsState: String? = null,
        val sttState: String? = null,
        val continuousListeningState: String? = null,
        val modelInstallState: String? = null,
        val lastLifecycleEvent: String? = null,
        val lastSafeCheckpoint: String? = null,
    )

    data class CrashReceipt(
        val context: ContextSnapshot,
        val exceptionClass: String,
        val stackTrace: String,
        val thread: String,
        val buildIdentity: String,
        val deviceModel: String,
        val androidVersion: String,
        val crashAt: Instant,
    )

    @Volatile private var snapshot: ContextSnapshot =
        ContextSnapshot(sessionId = newSessionId())

    @Volatile private var installed: Boolean = false
    private var priorHandler: Thread.UncaughtExceptionHandler? = null

    fun newSessionId(): String =
        "s-" + java.util.UUID.randomUUID().toString().replace("-", "").take(16)

    /** The CURRENT live context (call before crash-free navigation too). */
    fun current(): ContextSnapshot = snapshot

    fun update(transform: (ContextSnapshot) -> ContextSnapshot) {
        snapshot = transform(snapshot)
    }

    /**
     * Installs the crash hook ONCE (idempotent). The prior handler is ALWAYS
     * chained — the default framework death handling is preserved.
     */
    fun install(
        appContext: Context,
        buildIdentity: () -> String,
    ) {
        if (installed) return
        installed = true
        priorHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            runCatching {
                persist(
                    appContext = appContext,
                    receipt = buildReceipt(throwable, thread, buildIdentity()),
                )
            }
            // Chain: the platform handler performs the real process death.
            priorHandler?.uncaughtException(thread, throwable)
        }
    }

    fun buildReceipt(
        throwable: Throwable,
        thread: Thread,
        buildIdentity: String,
    ): CrashReceipt = CrashReceipt(
        context = snapshot,
        exceptionClass = throwable.javaClass.name,
        stackTrace = throwable.stackTraceToString().take(24_000),
        thread = thread.name,
        buildIdentity = buildIdentity,
        deviceModel = android.os.Build.MODEL ?: "unknown",
        androidVersion = android.os.Build.VERSION.SDK_INT.toString(),
        crashAt = Instant.now(),
    )

    /** Persists the receipt synchronously (best-effort BEFORE process death). */
    fun persist(appContext: Context, receipt: CrashReceipt) {
        val props = Properties()
        fun put(k: String, v: String?) { if (v != null) props.setProperty(k, v.take(400)) }
        put(K_SESSION_ID, receipt.context.sessionId)
        put(K_ONBOARDING_STEP, receipt.context.onboardingStep.toString())
        put(K_ONBOARDING_COMPLETED_AT, receipt.context.onboardingCompletedAt)
        put(K_DESTINATION_ROUTE, receipt.context.destinationRoute)
        put(K_BACKSTACK_SUMMARY, receipt.context.backStackSummary)
        put(K_EFFECTIVE_LOCALE, receipt.context.effectiveLocale)
        put(K_PENDING_LOCALE_RECREATION, receipt.context.pendingLocaleRecreation.toString())
        put(K_TTS_STATE, receipt.context.ttsState)
        put(K_STT_STATE, receipt.context.sttState)
        put(K_CONTINUOUS_LISTENING, receipt.context.continuousListeningState)
        put(K_MODEL_INSTALL_STATE, receipt.context.modelInstallState)
        put(K_LAST_LIFECYCLE_EVENT, receipt.context.lastLifecycleEvent)
        put(K_LAST_SAFE_CHECKPOINT, receipt.context.lastSafeCheckpoint)
        put(K_FATAL_EXCEPTION_CLASS, receipt.exceptionClass)
        put(K_FATAL_STACK, receipt.stackTrace) // full stack preserved here (24k cap above)
        put(K_FATAL_THREAD, receipt.thread)
        put(K_BUILD_IDENTITY, receipt.buildIdentity)
        put(K_DEVICE_MODEL, receipt.deviceModel)
        put(K_ANDROID_VERSION, receipt.androidVersion)
        put(K_CRASH_AT, receipt.crashAt.toString())
        val dir = File(appContext.filesDir, DIR)
        if (!dir.exists()) dir.mkdirs()
        File(dir, FILE).outputStream().use { props.store(it, "hermes first-run crash receipt (secret-clean)") }
    }

    /** Reads the persisted receipt on the NEXT launch, or null when clean. */
    fun consume(appContext: Context): CrashReceipt? {
        val f = File(File(appContext.filesDir, DIR), FILE)
        if (!f.exists()) return null
        val props = Properties()
        runCatching { f.inputStream().use { props.load(it) } }
        if (props.isEmpty) return null
        val ctx = ContextSnapshot(
            sessionId = props.getProperty(K_SESSION_ID) ?: "unknown",
            onboardingStep = props.getProperty(K_ONBOARDING_STEP)?.toIntOrNull() ?: -1,
            onboardingCompletedAt = props.getProperty(K_ONBOARDING_COMPLETED_AT),
            destinationRoute = props.getProperty(K_DESTINATION_ROUTE),
            backStackSummary = props.getProperty(K_BACKSTACK_SUMMARY),
            effectiveLocale = props.getProperty(K_EFFECTIVE_LOCALE),
            pendingLocaleRecreation = props.getProperty(K_PENDING_LOCALE_RECREATION) == "true",
            ttsState = props.getProperty(K_TTS_STATE),
            sttState = props.getProperty(K_STT_STATE),
            continuousListeningState = props.getProperty(K_CONTINUOUS_LISTENING),
            modelInstallState = props.getProperty(K_MODEL_INSTALL_STATE),
            lastLifecycleEvent = props.getProperty(K_LAST_LIFECYCLE_EVENT),
            lastSafeCheckpoint = props.getProperty(K_LAST_SAFE_CHECKPOINT),
        )
        return CrashReceipt(
            context = ctx,
            exceptionClass = props.getProperty(K_FATAL_EXCEPTION_CLASS) ?: "unknown",
            stackTrace = props.getProperty(K_FATAL_STACK) ?: "",
            thread = props.getProperty(K_FATAL_THREAD) ?: "unknown",
            buildIdentity = props.getProperty(K_BUILD_IDENTITY) ?: "unknown",
            deviceModel = props.getProperty(K_DEVICE_MODEL) ?: "unknown",
            androidVersion = props.getProperty(K_ANDROID_VERSION) ?: "unknown",
            crashAt = runCatching { Instant.parse(props.getProperty(K_CRASH_AT)) }.getOrDefault(Instant.EPOCH),
        )
    }

    /** Clears the receipt after the recovery flow handled it. */
    fun clear(appContext: Context) {
        File(File(appContext.filesDir, DIR), FILE).delete()
    }

    /**
     * §3 typed verdict: does the receipt describe the FIRST-RUN window?
     * (onboarding not yet completed OR completion within this process session).
     */
    fun isFirstRunCrash(receipt: CrashReceipt): Boolean =
        receipt.context.onboardingStep >= 0 || receipt.context.onboardingCompletedAt != null
}
