package com.hermestower.voiceconsole.voice

import android.content.Context
import android.speech.tts.TextToSpeech
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

/**
 * AndroidSystemTtsEngine (GRAPH 4 NODE 3, §10 fallback tier 2): the REAL Android
 * system offline TTS as the functional fallback while the natural local voice
 * (Piper IT) is pending runtime integration and device qualification. If the
 * system engine is unavailable, the pipeline degrades to text-only — the
 * truthful terminal fallback.
 *
 * Not loadable in JVM tests; the pipeline controller consumes it through the
 * TtsEngine seam, so tests inject deterministic fakes.
 */
class AndroidSystemTtsEngine(private val context: Context) : TtsEngine {

    private val _state = MutableStateFlow(TtsOutput(TtsState.NOT_INSTALLED))
    override val state: StateFlow<TtsOutput> = _state.asStateFlow()

    private var tts: TextToSpeech? = null
    private var ready = false

    private val initListener = TextToSpeech.OnInitListener { status ->
        ready = status == TextToSpeech.SUCCESS
        if (ready) {
            // Italian is the Owner's language; a failed language set is honest
            // (state stays READY but the pipeline reports the engine as usable
            // only when speak() succeeds — no invented qualification).
            runCatching { tts?.language = Locale.ITALY }
            _state.value = TtsOutput(TtsState.READY)
        } else {
            _state.value = TtsOutput(TtsState.FAILED, error = "Android system TTS init failed (status $status)")
        }
    }

    override fun load() {
        if (tts != null) return
        _state.value = TtsOutput(TtsState.LOADING)
        runCatching {
            tts = TextToSpeech(context, initListener)
        }.onFailure {
            _state.value = TtsOutput(TtsState.FAILED, error = "Android system TTS unavailable: ${it.message}")
        }
    }

    override fun unload() {
        runCatching { tts?.shutdown() }
        tts = null
        ready = false
        _state.value = TtsOutput(TtsState.UNLOADED)
    }

    override fun speak(text: String) {
        val engine = tts
        if (engine == null || !ready) {
            _state.value = TtsOutput(_state.value.state, error = "speak called before READY")
            return
        }
        _state.value = TtsOutput(TtsState.SYNTHESIZING, spokenText = text)
        val result = engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, "hermes-turn")
        _state.value = if (result == TextToSpeech.SUCCESS) {
            TtsOutput(TtsState.SPEAKING, spokenText = text)
        } else {
            TtsOutput(TtsState.FAILED, error = "system TTS speak returned $result")
        }
    }

    override fun interrupt() {
        runCatching { tts?.stop() }
        if (ready) _state.value = TtsOutput(TtsState.READY) else _state.value = TtsOutput(TtsState.NOT_INSTALLED)
    }
}
