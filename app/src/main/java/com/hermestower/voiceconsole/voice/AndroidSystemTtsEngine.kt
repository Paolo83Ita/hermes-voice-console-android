package com.hermestower.voiceconsole.voice

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
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
 * system offline TTS. Not loadable in JVM tests; the pipeline controller
 * consumes it through the TtsEngine seam, so tests inject deterministic fakes.
 *
 * OWNER PHYSICAL RETEST (2026-11-06, POCO X6 Pro — second physical cycle):
 * the CP-G4-026 build negotiated the voice but its `speak()` was FAIL-OPEN —
 * the engine's default English voice stayed audible while Italian was
 * requested (PHYSICAL-FAIL-01) and no rate/pitch was ever applied
 * (PHYSICAL-FAIL-02, `HIGH_PITCH_ENGLISH_TTS_OUTPUT`). This rewrite:
 *
 *  1. FAIL-CLOSED speak gate ([TtsSpeakGate]): when the effective app language
 *     is Italian, audio is produced ONLY after the post-apply read-back
 *     reports an Italian voice. Otherwise the utterance is REFUSED, the state
 *     becomes [TtsState.TEXT_ONLY_SAFE_DEGRADED] with a typed
 *     [TtsSpeakBlock] (`ITALIAN_SYSTEM_TTS_DATA_MISSING` /
 *     `ITALIAN_SYSTEM_TTS_READBACK_MISMATCH`), the response stays visible as
 *     text and no English word is ever spoken.
 *  2. SPEECH-PROFILE AUTHORITY ([SpeechProfile]): `setSpeechRate` /
 *     `setPitch` are applied from the persisted profile before every
 *     utterance (defaults 0.96 / 0.95 — comfortable, no forced high pitch).
 *  3. STRUCTURED TELEMETRY ([TtsTelemetry]): engine init, locale requested,
 *     language availability, voices enumerated, selected voice, setVoice
 *     result, utterance start/done/error, fallback reason, rate/pitch
 *     applied — spoken content is NEVER logged (length only, SEC §4).
 *  4. NEGOTIATION re-runs before EVERY utterance: an app-language change is
 *     honored on the next answer without extra wiring.
 *  5. DIAGNOSTICS: the full inventory (engine package + label, availability,
 *     Italian/offline/networked voice counts, selected voice + locale, last
 *     negotiation result) is exposed for the Voice & Audio diagnostic card.
 */
class AndroidSystemTtsEngine(
    private val context: Context,
    /** App-language provider re-read on every negotiation (default: the app language store). */
    private val spokenLocaleTagProvider: () -> String = { "it" },
    /** Speech-profile provider re-read before every utterance (the persisted settings authority). */
    private val speechProfileProvider: () -> SpeechProfile = { SpeechProfile.RECOMMENDED },
    /** Secret-clean structured telemetry (Owner brief §3). */
    val telemetry: TtsTelemetry = TtsTelemetry(),
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

    /** OWNER PHYSICAL RETEST §6: the last enumeration snapshot (diagnostic card). */
    @Volatile
    var lastInventory: ItalianTtsVoiceNegotiator.TtsVoiceInventory? = null
        private set

    /** OWNER PHYSICAL RETEST §6: the human label of the active engine package. */
    @Volatile
    var activeEngineLabel: String? = null
        private set

    private val initListener = TextToSpeech.OnInitListener { status ->
        ready = status == TextToSpeech.SUCCESS
        if (ready) {
            val engine = tts
            if (engine != null) {
                // §13 read-back: capture the ACTUAL engine package (honest identity).
                activeEngineName = runCatching { engine.defaultEngine }.getOrNull()
                activeEngineLabel = engineLabelFor(activeEngineName)
                telemetry.log(TtsTelemetry.Event.ENGINE_INIT_OK, "engine=${activeEngineName ?: "default"}")
                negotiateAndApply(engine)
            } else {
                voiceReadback = engineUnavailableReadback()
                _state.value = TtsOutput(TtsState.READY)
            }
        } else {
            telemetry.log(TtsTelemetry.Event.ENGINE_INIT_FAILED, "status=$status")
            voiceReadback = engineUnavailableReadback()
            _state.value = TtsOutput(TtsState.FAILED, error = "Android system TTS init failed (status $status)")
        }
    }

    override fun load() {
        if (tts != null) return
        telemetry.log(TtsTelemetry.Event.ENGINE_INIT_START)
        _state.value = TtsOutput(TtsState.LOADING)
        runCatching {
            tts = TextToSpeech(context, initListener)
        }.onFailure {
            telemetry.log(TtsTelemetry.Event.ENGINE_INIT_FAILED, "error=${it.javaClass.simpleName}")
            voiceReadback = engineUnavailableReadback()
            _state.value = TtsOutput(TtsState.FAILED, error = "Android system TTS unavailable: ${it.message}")
        }
    }

    override fun unload() {
        runCatching { tts?.shutdown() }
        tts = null
        ready = false
        telemetry.log(TtsTelemetry.Event.ENGINE_SHUTDOWN)
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
        // OWNER PHYSICAL RETEST §4: THE FAIL-CLOSED GATE. A non-Italian active
        // voice while Italian is requested REFUSES the utterance — the
        // pre-repair fail-open call spoke the engine's default English voice
        // (the exact PHYSICAL-FAIL-01 the Owner heard).
        val verdict = TtsSpeakGate.decide(
            effectiveLanguageTag = runCatching { spokenLocaleTagProvider() }.getOrDefault("it"),
            readback = voiceReadback,
        )
        if (verdict is TtsSpeakGate.Verdict.Block) {
            val block = TtsSpeakBlock(reason = verdict.reason, readbackStatus = verdict.readbackStatus)
            telemetry.log(TtsTelemetry.Event.SPEAK_BLOCKED, "token=${block.token} status=${block.readbackStatus}")
            telemetry.log(TtsTelemetry.Event.FALLBACK_REASON, "speech refused: ${block.token} (text-only safe degraded)")
            _state.value = TtsOutput(
                TtsState.TEXT_ONLY_SAFE_DEGRADED,
                voiceReadback = voiceReadback,
                speakBlocked = block,
            )
            return
        }

        // OWNER PHYSICAL RETEST §5: the speech profile is the rate/pitch
        // authority (the pre-repair pipeline never called setSpeechRate /
        // setPitch — the engine default voice character was audible).
        val profile = runCatching { speechProfileProvider() }.getOrDefault(SpeechProfile.RECOMMENDED)
        runCatching { engine.setSpeechRate(profile.speechRate) }
        telemetry.log(TtsTelemetry.Event.RATE_APPLIED, "rate=${profile.speechRate}")
        runCatching { engine.setPitch(profile.pitch) }
        telemetry.log(TtsTelemetry.Event.PITCH_APPLIED, "pitch=${profile.pitch}")

        _state.value = TtsOutput(TtsState.SYNTHESIZING, spokenText = text, voiceReadback = voiceReadback)
        // G4-C03 truthfulness: SPEAKING must END when the audio ends. The
        // utterance listener closes the state honestly (OWNER delta §5: never
        // show SPEAKING when no TTS engine is producing audio).
        engine.setOnUtteranceProgressListener(utteranceListener(text))
        telemetry.log(TtsTelemetry.Event.SPEAK_CALL, "len=${text.length}")
        val result = engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, "hermes-turn")
        _state.value = if (result == TextToSpeech.SUCCESS) {
            TtsOutput(TtsState.SYNTHESIZING, spokenText = text, voiceReadback = voiceReadback) // onStart flips to SPEAKING
        } else {
            telemetry.log(TtsTelemetry.Event.FALLBACK_REASON, "speak returned $result")
            TtsOutput(TtsState.FAILED, error = "system TTS speak returned $result", voiceReadback = voiceReadback)
        }
    }

    private fun utteranceListener(text: String): UtteranceProgressListener =
        object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                telemetry.log(TtsTelemetry.Event.UTTERANCE_START, "id=$utteranceId")
                _state.value = TtsOutput(TtsState.SPEAKING, spokenText = text, voiceReadback = voiceReadback)
            }
            override fun onDone(utteranceId: String?) {
                telemetry.log(TtsTelemetry.Event.UTTERANCE_DONE, "id=$utteranceId")
                if (ready) _state.value = TtsOutput(TtsState.READY, voiceReadback = voiceReadback)
            }
            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                telemetry.log(TtsTelemetry.Event.UTTERANCE_ERROR, "id=$utteranceId")
                _state.value = TtsOutput(TtsState.FAILED, error = "system TTS utterance failed", voiceReadback = voiceReadback)
            }
        }

    override fun interrupt() {
        runCatching { tts?.stop() }
        if (ready) _state.value = TtsOutput(TtsState.READY, voiceReadback = voiceReadback)
        else _state.value = TtsOutput(TtsState.NOT_INSTALLED)
    }

    // ---- OWNER PHYSICAL RETEST §6: diagnostic-card actions ----

    /**
     * Test Italian voice (diagnostic card): speaks the PREVIEW PHRASE through
     * the real engine path — the same negotiation, profile and fail-closed
     * gate as a console answer. Never speaks a non-Italian voice.
     */
    fun testItalianVoice(previewPhrase: String) {
        load()
        speak(previewPhrase)
    }

    /** Refresh available voices: re-enumerates and re-negotiates with full read-back. */
    fun refreshVoices() {
        val engine = tts ?: return
        if (!ready) return
        negotiateAndApply(engine)
    }

    /**
     * Reset voice selection: the engine negotiates deterministically from the
     * full enumeration every time (no persisted app-side voice override
     * exists), so the reset action re-runs the negotiation from scratch.
     */
    fun resetVoiceSelection() {
        telemetry.log(TtsTelemetry.Event.NEGOTIATION_STATUS, "reset requested — deterministic re-negotiation")
        refreshVoices()
    }

    /**
     * The OWNER FEEDBACK 3 core, now with telemetry + inventory: explicit
     * language request + explicit voice selection + post-apply read-back,
     * all typed, all published.
     */
    private fun negotiateAndApply(engine: TextToSpeech) {
        val requestedTag = runCatching { spokenLocaleTagProvider() }.getOrNull() ?: "it"
        val requestLocale = ItalianTtsVoiceNegotiator.requestLocaleFor(requestedTag)
        telemetry.log(TtsTelemetry.Event.LOCALE_REQUESTED, "tag=${requestLocale.toLanguageTag()}")

        val setLanguageRaw = runCatching { engine.setLanguage(requestLocale) }
            .getOrDefault(TextToSpeech.LANG_NOT_SUPPORTED)
        val setLanguageResult = TtsLanguageResultCode.fromRaw(setLanguageRaw)
        telemetry.log(TtsTelemetry.Event.SET_LANGUAGE_RESULT, "code=${setLanguageResult.name}")
        telemetry.log(TtsTelemetry.Event.LANGUAGE_AVAILABILITY, "result=${setLanguageResult.name}")

        // Enumerate the engine's voices (may be null/empty on some engines — honest).
        val candidates: List<TtsVoiceCandidate> = runCatching {
            engine.voices?.map { v ->
                TtsVoiceCandidate(
                    name = v.name,
                    localeTag = v.locale.toLanguageTag(),
                    requiresNetwork = v.isNetworkConnectionRequired,
                    quality = runCatching { v.quality }.getOrDefault(0),
                )
            } ?: emptyList()
        }.getOrDefault(emptyList())
        val italianVoiceNames = candidates.filter { it.isItalian }.map { it.name }
        telemetry.log(
            TtsTelemetry.Event.VOICES_ENUMERATED,
            "total=${candidates.size} italian=${italianVoiceNames.size} " +
                "offlineItalian=${candidates.count { it.isItalian && !it.requiresNetwork }} " +
                "networkItalian=${candidates.count { it.isItalian && it.requiresNetwork }}",
        )

        // Explicit voice selection (language alone is NOT the voice).
        var selectedName: String? = null
        var selectedLocaleTag: String? = null
        val plan = ItalianTtsVoiceNegotiator.selectVoice(candidates)
        if (plan != null) {
            val platformVoice = runCatching {
                engine.voices?.firstOrNull { it.name == plan.name }
            }.getOrNull()
            if (platformVoice != null) {
                val applied = runCatching { engine.voice = platformVoice }.isSuccess
                if (applied) {
                    selectedName = plan.name
                    selectedLocaleTag = plan.localeTag
                }
            }
            telemetry.log(
                TtsTelemetry.Event.SET_VOICE_RESULT,
                if (selectedName != null) "voice=${plan.name} locale=${plan.localeTag}" else "result=REFUSED",
            )
        } else {
            telemetry.log(TtsTelemetry.Event.SET_VOICE_RESULT, "result=NO_ITALIAN_VOICE")
        }

        // POST-APPLY TRUTH (never trust the request — read what is active now).
        val activeVoice: Voice? = runCatching { engine.voice }.getOrNull()
        val activeVoiceName = activeVoice?.name
        val activeVoiceLocaleTag = activeVoice?.locale?.toLanguageTag()
        // getLanguage() returns a Locale (deprecated but still the engine's truth) —
        // read it back as a language tag; null when the engine reports nothing.
        val activeLanguageTag = runCatching { engine.language?.toLanguageTag() }.getOrNull()
        telemetry.log(
            TtsTelemetry.Event.READBACK_VOICE,
            "voice=${activeVoiceName ?: "null"} locale=${activeVoiceLocaleTag ?: "null"}",
        )
        telemetry.log(TtsTelemetry.Event.READBACK_LANGUAGE, "lang=${activeLanguageTag ?: "null"}")
        activeVoice?.let {
            telemetry.log(TtsTelemetry.Event.NETWORK_REQUIRED, "network=${it.isNetworkConnectionRequired}")
        }

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
        lastInventory = ItalianTtsVoiceNegotiator.inventory(
            engineName = activeEngineName,
            candidates = candidates,
            selectedVoiceName = selectedName,
            selectedVoiceLocaleTag = selectedLocaleTag,
        )
        voiceReadback = readback
        telemetry.log(TtsTelemetry.Event.NEGOTIATION_STATUS, "status=${readback.status.name}")
        // Recovery: a fresh negotiation returns the engine from the fail-closed
        // resting state to READY (a later utterance re-blocks if still warranted).
        val recoverable = _state.value.state == TtsState.READY ||
            _state.value.state == TtsState.NOT_INSTALLED ||
            _state.value.state == TtsState.TEXT_ONLY_SAFE_DEGRADED
        if (recoverable) {
            _state.value = TtsOutput(TtsState.READY, voiceReadback = readback)
        }
    }

    /** Human label of the active engine package (PackageManager truth; null when unknown). */
    private fun engineLabelFor(enginePackage: String?): String? {
        if (enginePackage.isNullOrBlank()) return null
        return runCatching {
            context.packageManager.getApplicationLabel(
                context.packageManager.getApplicationInfo(enginePackage, 0),
            ).toString()
        }.getOrNull()
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
