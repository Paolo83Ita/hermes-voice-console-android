package com.hermestower.voiceconsole.voice

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.Voice
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.hermestower.voiceconsole.voice.ItalianTtsVoiceNegotiator.ItalianTtsVoiceStatus
import com.hermestower.voiceconsole.voice.ItalianTtsVoiceNegotiator.TtsLanguageResultCode
import com.hermestower.voiceconsole.voice.ItalianTtsVoiceNegotiator.TtsVoiceCandidate
import com.hermestower.voiceconsole.voice.ItalianTtsVoiceNegotiator.TtsVoiceReadback
import java.util.Locale

/**
 * AndroidSystemTtsEngine (GRAPH 4 NODE 3, §10 fallback tier 2): the REAL Android
 * system offline TTS as the functional fallback while the natural local voice
 * (Piper IT) is pending runtime integration and device qualification. If the
 * system engine is unavailable, the pipeline degrades to text-only — the
 * truthful terminal fallback.
 *
 * OWNER PHYSICAL FEEDBACK 3 (2026-11-06, POCO X6 PRO): the pre-repair engine set
 * `language = Locale.ITALY` and DISCARDED setLanguage's typed result — when the
 * device lacked Italian voice data it silently kept its default (English) voice.
 * The engine now (1) reads the spoken locale from the app language (not a
 * hardcoded constant), (2) selects an explicit Italian voice when the engine
 * exposes one (offline preferred), (3) verifies the POST-APPLY truth
 * (getVoice/getLanguage) through the pure [ItalianTtsVoiceNegotiator], and
 * (4) publishes a typed [TtsVoiceReadback] on every state emission — the
 * Diagnostics screen, the Voice & Audio card and Self-Test ST-51 surface it
 * verbatim. The negotiation re-runs before EVERY utterance, so an app-language
 * change is honored on the next answer without any extra wiring.
 *
 * Not loadable in JVM tests; the pipeline controller consumes it through the
 * TtsEngine seam, so tests inject deterministic fakes.
 */
class AndroidSystemTtsEngine(
    private val context: Context,
    /** App-language provider re-read on every negotiation (default: the app language store). */
    private val spokenLocaleTagProvider: () -> String = { "it" },
) : TtsEngine {

    private val _state = MutableStateFlow(TtsOutput(TtsState.NOT_INSTALLED))
    override val state: StateFlow<TtsOutput> = _state.asStateFlow()

    private var tts: TextToSpeech? = null
    private var ready = false

    /** GRAPH 4 FINAL §13: the ACTUAL system engine identity (read-back). */
    @Volatile
    var activeEngineName: String? = null
        private set

    /** OWNER FEEDBACK 3: the post-apply voice truth (null until the first negotiation). */
    @Volatile
    var voiceReadback: TtsVoiceReadback? = null
        private set

    private val initListener = TextToSpeech.OnInitListener { status ->
        ready = status == TextToSpeech.SUCCESS
        if (ready) {
            val engine = tts
            if (engine != null) {
                // §13 read-back: capture the ACTUAL engine package (honest identity).
                activeEngineName = runCatching { engine.defaultEngine }.getOrNull()
                negotiateAndApply(engine)
            } else {
                voiceReadback = engineUnavailableReadback()
                _state.value = TtsOutput(TtsState.READY)
            }
        } else {
            voiceReadback = engineUnavailableReadback()
            _state.value = TtsOutput(TtsState.FAILED, error = "Android system TTS init failed (status $status)")
        }
    }

    override fun load() {
        if (tts != null) return
        _state.value = TtsOutput(TtsState.LOADING)
        runCatching {
            tts = TextToSpeech(context, initListener)
        }.onFailure {
            voiceReadback = engineUnavailableReadback()
            _state.value = TtsOutput(TtsState.FAILED, error = "Android system TTS unavailable: ${it.message}")
        }
    }

    override fun unload() {
        runCatching { tts?.shutdown() }
        tts = null
        ready = false
        _state.value = TtsOutput(TtsState.UNLOADED)
    }

    /**
     * Public re-negotiation (app-language change or boot): re-reads the spoken
     * locale from the provider and re-applies language+voice with full read-back.
     */
    fun applySpokenLocale() {
        val engine = tts ?: return
        if (!ready) return
        negotiateAndApply(engine)
    }

    override fun speak(text: String) {
        val engine = tts
        if (engine == null || !ready) {
            _state.value = TtsOutput(_state.value.state, error = "speak called before READY")
            return
        }
        // OWNER FEEDBACK 3: re-negotiate before EVERY utterance — an app-language
        // change between utterances is honored on the very next answer, and the
        // read-back always reflects the voice that is about to be heard.
        negotiateAndApply(engine)
        _state.value = TtsOutput(TtsState.SYNTHESIZING, spokenText = text, voiceReadback = voiceReadback)
        // G4-C03 truthfulness: SPEAKING must END when the audio ends. The
        // utterance listener closes the state honestly (OWNER delta §5: never
        // show SPEAKING when no TTS engine is producing audio).
        engine.setOnUtteranceProgressListener(object : android.speech.tts.UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                _state.value = TtsOutput(TtsState.SPEAKING, spokenText = text, voiceReadback = voiceReadback)
            }
            override fun onDone(utteranceId: String?) {
                if (ready) _state.value = TtsOutput(TtsState.READY, voiceReadback = voiceReadback)
            }
            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                _state.value = TtsOutput(TtsState.FAILED, error = "system TTS utterance failed", voiceReadback = voiceReadback)
            }
        })
        val result = engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, "hermes-turn")
        _state.value = if (result == TextToSpeech.SUCCESS) {
            TtsOutput(TtsState.SYNTHESIZING, spokenText = text, voiceReadback = voiceReadback) // onStart flips to SPEAKING
        } else {
            TtsOutput(TtsState.FAILED, error = "system TTS speak returned $result", voiceReadback = voiceReadback)
        }
    }

    override fun interrupt() {
        runCatching { tts?.stop() }
        if (ready) _state.value = TtsOutput(TtsState.READY, voiceReadback = voiceReadback)
        else _state.value = TtsOutput(TtsState.NOT_INSTALLED)
    }

    /**
     * The OWNER FEEDBACK 3 core: explicit language request + explicit voice
     * selection + post-apply read-back, all typed, all published.
     */
    private fun negotiateAndApply(engine: TextToSpeech) {
        val requestedTag = runCatching { spokenLocaleTagProvider() }.getOrNull() ?: "it"
        val requestLocale = ItalianTtsVoiceNegotiator.requestLocaleFor(requestedTag)

        val setLanguageRaw = runCatching { engine.setLanguage(requestLocale) }
            .getOrDefault(TextToSpeech.LANG_NOT_SUPPORTED)
        val setLanguageResult = TtsLanguageResultCode.fromRaw(setLanguageRaw)

        // Enumerate the engine's voices (may be null/empty on some engines — honest).
        val candidates: List<TtsVoiceCandidate> = runCatching {
            engine.voices?.map { v ->
                TtsVoiceCandidate(
                    name = v.name,
                    localeTag = v.locale.toLanguageTag(),
                    requiresNetwork = v.isNetworkConnectionRequired,
                )
            } ?: emptyList()
        }.getOrDefault(emptyList())
        val italianVoiceNames = candidates.filter { it.isItalian }.map { it.name }

        // Explicit voice selection (language alone is NOT the voice).
        var selectedName: String? = null
        val plan = ItalianTtsVoiceNegotiator.selectVoice(candidates)
        if (plan != null) {
            val platformVoice = runCatching {
                engine.voices?.firstOrNull { it.name == plan.name }
            }.getOrNull()
            if (platformVoice != null) {
                val applied = runCatching { engine.voice = platformVoice }.isSuccess
                if (applied) selectedName = plan.name
            }
        }

        // POST-APPLY TRUTH (never trust the request — read what is active now).
        val activeVoice: Voice? = runCatching { engine.voice }.getOrNull()
        val activeVoiceName = activeVoice?.name
        val activeVoiceLocaleTag = activeVoice?.locale?.toLanguageTag()
        // getLanguage() returns a Locale (deprecated but still the engine's truth) —
        // read it back as a language tag; null when the engine reports nothing.
        val activeLanguageTag = runCatching { engine.language?.toLanguageTag() }.getOrNull()

        val readback = ItalianTtsVoiceNegotiator.classify(
            requestedLocaleTag = requestLocale.toLanguageTag(),
            setLanguageResult = setLanguageResult,
            selectedVoiceName = selectedName,
            activeVoiceName = activeVoiceName,
            activeVoiceLocaleTag = activeVoiceLocaleTag,
            activeLanguageTag = activeLanguageTag,
            engineName = activeEngineName,
            availableItalianVoiceNames = italianVoiceNames,
        )
        voiceReadback = readback
        if (_state.value.state == TtsState.READY || _state.value.state == TtsState.NOT_INSTALLED) {
            _state.value = TtsOutput(TtsState.READY, voiceReadback = readback)
        }
    }

    private fun engineUnavailableReadback(): TtsVoiceReadback =
        ItalianTtsVoiceNegotiator.classify(
            requestedLocaleTag = ItalianTtsVoiceNegotiator.requestLocaleFor(
                runCatching { spokenLocaleTagProvider() }.getOrDefault("it"),
            ).toLanguageTag(),
            setLanguageResult = TtsLanguageResultCode.UNKNOWN,
            selectedVoiceName = null,
            activeVoiceName = null,
            activeVoiceLocaleTag = null,
            activeLanguageTag = null,
            engineName = activeEngineName,
            availableItalianVoiceNames = emptyList(),
        ).copy(status = ItalianTtsVoiceStatus.ENGINE_UNAVAILABLE)
}
