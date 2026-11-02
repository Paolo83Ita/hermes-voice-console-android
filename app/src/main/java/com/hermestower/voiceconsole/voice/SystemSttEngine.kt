package com.hermestower.voiceconsole.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer

/**
 * GRAPH 4 G4-C03/C04 (§6 STT): system speech recognizer seam.
 *
 * While the offline frame-based STT (vosk) is pending model delivery and
 * runtime qualification (G4-C04), the Android system recognizer is the REAL,
 * physically working STT path: it captures the microphone itself (the app MUST
 * NOT open AudioRecord at the same time — one owner of the microphone),
 * emits partial and final transcripts for Italian, and is stopped physically
 * on mute/stop/lifecycle (INV-G4-10: no capture after stop).
 *
 * Privacy contract: offline recognition is preferred (EXTRA_PREFER_OFFLINE);
 * no audio, no transcript is retained by this adapter — events go to the
 * caller and are dropped; nothing is written to logs.
 */
enum class SystemSttErrorKind {
    NO_SPEECH,
    NETWORK_REQUIRED,
    BUSY,
    PERMISSION,
    UNAVAILABLE,
    FAILED,
}

data class SystemSttError(val kind: SystemSttErrorKind, val systemCode: Int) {
    val isNoSpeech: Boolean get() = kind == SystemSttErrorKind.NO_SPEECH
}

interface SystemSttEvents {
    /** Live partial transcript (store draft, never logged). */
    fun onPartial(text: String)
    /** Final transcript for one utterance; confidence when the engine provides it. */
    fun onFinal(text: String, confidence: Float?)
    /** Typed failure; capture has already stopped when this fires. */
    fun onError(error: SystemSttError)
    /** Capture physically began (microphone owned by the recognizer). */
    fun onCaptureStarted()
    /** Capture physically ended (microphone released by the recognizer). */
    fun onCaptureEnded()
}

interface SystemSttEngine {
    /** True when the Android system exposes a usable recognizer. */
    val isAvailable: Boolean
    /** Starts one recognition session. Returns false when unavailable/busy. */
    fun start(languageTag: String, events: SystemSttEvents): Boolean
    /** Physically stops capture for this session (Owner stop / mute / lifecycle). */
    fun stop()
    /** Releases every recognizer resource; engine must be re-created after this. */
    fun destroy()
}

/** JVM fake for coordinator tests: scripted partial/final/error emission. */
class ScriptedSystemSttEngine(
    private val available: Boolean = true,
) : SystemSttEngine {
    var events: SystemSttEvents? = null
    var startCalls = 0
    var stopCalls = 0
    var destroyCalls = 0
    var started = false
    var startAccepted = true

    override val isAvailable: Boolean get() = available
    override fun start(languageTag: String, events: SystemSttEvents): Boolean {
        startCalls++
        if (!startAccepted) return false
        this.events = events
        started = true
        events.onCaptureStarted()
        return true
    }
    override fun stop() {
        stopCalls++
        if (started) {
            started = false
            events?.onCaptureEnded()
        }
    }
    override fun destroy() {
        destroyCalls++
        stop()
    }

    // ---- scripted driving helpers (tests) ----
    fun emitPartial(text: String) { events?.onPartial(text) }
    fun emitFinal(text: String, confidence: Float? = 0.9f) {
        events?.onFinal(text, confidence)
        started = false
        events?.onCaptureEnded()
    }
    fun emitNoSpeech() {
        events?.onError(SystemSttError(SystemSttErrorKind.NO_SPEECH, 7))
        started = false
        events?.onCaptureEnded()
    }
}

/**
 * Real Android recognizer. One instance per app; createSpeechRecognizer is
 * lazy on first start; destroy() releases it (§10 lifecycle: camera/mic only
 * while the surface is active — same rule applied to the microphone).
 */
class AndroidSystemSttEngine(private val context: Context) : SystemSttEngine {

    private var recognizer: SpeechRecognizer? = null
    private var activeEvents: SystemSttEvents? = null
    private var capturing = false

    override val isAvailable: Boolean
        get() = runCatching { SpeechRecognizer.isRecognitionAvailable(context) }.getOrDefault(false)

    private val listener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            capturing = true
            activeEvents?.onCaptureStarted()
        }
        override fun onBeginningOfSpeech() {}
        override fun onRmsChanged(rmsdB: Float) {}
        override fun onBufferReceived(buffer: ByteArray?) {} // never retained
        override fun onEndOfSpeech() {}
        override fun onError(error: Int) {
            capturing = false
            val events = activeEvents
            activeEvents = null
            events?.onError(mapError(error))
            events?.onCaptureEnded()
        }
        override fun onResults(results: Bundle?) {
            capturing = false
            val events = activeEvents
            activeEvents = null
            val text = results
                ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                ?.firstOrNull()
                .orEmpty()
            val conf = results?.getFloatArray(SpeechRecognizer.CONFIDENCE_SCORES)?.firstOrNull()
            events?.onFinal(text, conf)
            events?.onCaptureEnded()
        }
        override fun onPartialResults(partialResults: Bundle?) {
            val text = partialResults
                ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                ?.firstOrNull()
                ?: return
            if (text.isNotBlank()) activeEvents?.onPartial(text)
        }
        override fun onEvent(eventType: Int, params: Bundle?) {}
    }

    private fun mapError(error: Int): SystemSttError = when (error) {
        SpeechRecognizer.ERROR_NO_MATCH, SpeechRecognizer.ERROR_SPEECH_TIMEOUT ->
            SystemSttError(SystemSttErrorKind.NO_SPEECH, error)
        SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT ->
            SystemSttError(SystemSttErrorKind.NETWORK_REQUIRED, error)
        SpeechRecognizer.ERROR_RECOGNIZER_BUSY ->
            SystemSttError(SystemSttErrorKind.BUSY, error)
        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS ->
            SystemSttError(SystemSttErrorKind.PERMISSION, error)
        else -> SystemSttError(SystemSttErrorKind.FAILED, error)
    }

    override fun start(languageTag: String, events: SystemSttEvents): Boolean {
        if (!isAvailable) return false
        if (capturing || activeEvents != null) return false
        val rec = recognizer ?: runCatching {
            SpeechRecognizer.createSpeechRecognizer(context).also { recognizer = it }
        }.getOrNull() ?: return false
        activeEvents = events
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageTag)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
        }
        rec.setRecognitionListener(listener)
        runCatching { rec.startListening(intent) }
            .onFailure {
                activeEvents = null
                events.onError(SystemSttError(SystemSttErrorKind.FAILED, -1))
                return false
            }
        return true
    }

    override fun stop() {
        if (!capturing && activeEvents == null) return
        runCatching { recognizer?.stopListening() }
        capturing = false
        val events = activeEvents
        activeEvents = null
        events?.onCaptureEnded()
    }

    override fun destroy() {
        stop()
        runCatching { recognizer?.destroy() }
        recognizer = null
    }
}
