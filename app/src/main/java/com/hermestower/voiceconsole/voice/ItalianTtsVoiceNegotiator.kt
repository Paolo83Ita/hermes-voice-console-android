package com.hermestower.voiceconsole.voice

import java.util.Locale

/**
 * ItalianTtsVoiceNegotiator (OWNER PHYSICAL FEEDBACK, Finding 3): the PURE
 * language→voice negotiation logic for the system TTS route.
 *
 * The pre-repair engine set `tts.language = Locale.ITALY` and DISCARDED the typed
 * result — when the device lacked Italian voice data the engine silently kept its
 * default (usually en-US) voice and the console audibly answered in English
 * (OWNER PHYSICAL FINDING 3). The negotiation is now explicit, read-back-verified
 * and typed:
 *
 *  1. the engine enumerates its voices into [TtsVoiceCandidate]s;
 *  2. [selectVoice] picks the best Italian candidate (offline preferred over
 *     networked, exact country match preferred over bare language);
 *  3. after the engine applied setLanguage + the explicit voice, [classify]
 *     combines the setLanguage result with the POST-APPLY read-back
 *     (getVoice()/getLanguage()) into a typed [ItalianTtsVoiceStatus] that the
 *     UI, Diagnostics and Self-Test ST-51 surface verbatim — no invented state.
 *
 * Pure JVM logic: no android imports — the engine maps platform objects into the
 * candidate/read-back data types, so the negotiation is unit-testable.
 */
object ItalianTtsVoiceNegotiator {

    /** Typed interpretation of TextToSpeech.setLanguage's return code. */
    enum class TtsLanguageResultCode(val rawValue: Int) {
        LANG_COUNTRY_VAR_AVAILABLE(2),
        LANG_COUNTRY_AVAILABLE(1),
        LANG_AVAILABLE(0),
        LANG_MISSING_DATA(-1),
        LANG_NOT_SUPPORTED(-2),
        UNKNOWN(Int.MIN_VALUE),
        ;

        companion object {
            fun fromRaw(raw: Int): TtsLanguageResultCode =
                entries.firstOrNull { it.rawValue == raw } ?: UNKNOWN
        }
    }

    /**
     * The typed classification the Owner briefing demands. The pairing of
     * (requested language) → (audible voice) is verified, never assumed.
     */
    enum class ItalianTtsVoiceStatus(val isItalianAudible: Boolean, val requiresUserAction: Boolean = false) {
        /** setLanguage succeeded AND the post-apply read-back is an Italian voice. */
        ITALIAN_VOICE_ACTIVE(true),

        /** Italian data missing on the device (LANG_MISSING_DATA) — user action can fix it. */
        ITALIAN_VOICE_MISSING_DATA(false, true),

        /** The engine does not support Italian at all. */
        ITALIAN_VOICE_NOT_SUPPORTED(false),

        /** Language set accepted but the ACTIVE voice is not Italian (silent fallback / drift). */
        FALLBACK_VOICE_TRIGGERED(false, true),

        /** No TTS engine available on the device. */
        ENGINE_UNAVAILABLE(false),
    }

    /** One engine voice, platform-agnostic. */
    data class TtsVoiceCandidate(
        val name: String,
        val localeTag: String,
        val requiresNetwork: Boolean,
    ) {
        val language: String get() = localeTag.substringBefore('-').lowercase(Locale.ROOT)
        val isItalian: Boolean get() = language == "it"
        val isItalianItaly: Boolean get() = localeTag.lowercase(Locale.ROOT).startsWith("it-it")
    }

    /** The full read-back record surfaced in Diagnostics / Voice & Audio / ST-51. */
    data class TtsVoiceReadback(
        val requestedLocaleTag: String,
        val setLanguageResult: TtsLanguageResultCode,
        val selectedVoiceName: String?,
        val activeVoiceName: String?,
        val activeVoiceLocaleTag: String?,
        val activeLanguageTag: String?,
        val engineName: String?,
        val status: ItalianTtsVoiceStatus,
        val availableItalianVoiceNames: List<String>,
        val detail: String? = null,
    ) {
        val isItalianAudible: Boolean get() = status.isItalianAudible
    }

    /** Default country used to turn an app language code into a full request locale. */
    fun defaultCountry(languageCode: String): String = when (languageCode.lowercase(Locale.ROOT)) {
        "it" -> "IT"
        "en" -> "US"
        "es" -> "ES"
        "fr" -> "FR"
        "de" -> "DE"
        else -> ""
    }

    /** Builds the FULL request locale (language + default country) from an app tag like "it". */
    fun requestLocaleFor(tag: String): Locale {
        val locale = Locale.forLanguageTag(tag)
        val country = defaultCountry(locale.language)
        return if (country.isEmpty()) Locale(locale.language) else Locale(locale.language, country)
    }

    /**
     * Voice selection preference: exact it-IT offline → any it offline →
     * it-IT networked → any it networked → null (no Italian voice exists).
     */
    fun selectVoice(candidates: List<TtsVoiceCandidate>): TtsVoiceCandidate? {
        val italian = candidates.filter { it.isItalian }
        return italian.firstOrNull { it.isItalianItaly && !it.requiresNetwork }
            ?: italian.firstOrNull { !it.requiresNetwork }
            ?: italian.firstOrNull { it.isItalianItaly }
            ?: italian.firstOrNull()
    }

    /**
     * Classifies the POST-APPLY truth. Everything the device reports is taken
     * verbatim; nothing is inferred beyond the typed rules below.
     */
    fun classify(
        requestedLocaleTag: String,
        setLanguageResult: TtsLanguageResultCode,
        selectedVoiceName: String?,
        activeVoiceName: String?,
        activeVoiceLocaleTag: String?,
        activeLanguageTag: String?,
        engineName: String?,
        availableItalianVoiceNames: List<String>,
    ): TtsVoiceReadback {
        val activeLocale = activeVoiceLocaleTag?.lowercase(Locale.ROOT)
        val activeLanguage = activeLocale?.substringBefore('-')
        val status = when {
            setLanguageResult == TtsLanguageResultCode.LANG_MISSING_DATA ->
                ItalianTtsVoiceStatus.ITALIAN_VOICE_MISSING_DATA
            setLanguageResult == TtsLanguageResultCode.LANG_NOT_SUPPORTED ->
                ItalianTtsVoiceStatus.ITALIAN_VOICE_NOT_SUPPORTED
            activeLanguage == "it" || activeLocale?.startsWith("it-") == true ->
                ItalianTtsVoiceStatus.ITALIAN_VOICE_ACTIVE
            // setLanguage accepted (or even succeeded) yet the audible voice is
            // not Italian → the engine silently fell back to its default voice.
            else -> ItalianTtsVoiceStatus.FALLBACK_VOICE_TRIGGERED
        }
        val detail = when (status) {
            ItalianTtsVoiceStatus.ITALIAN_VOICE_MISSING_DATA ->
                "Italian voice data is not installed on this device; the engine kept another voice."
            ItalianTtsVoiceStatus.ITALIAN_VOICE_NOT_SUPPORTED ->
                "The active TTS engine does not support the requested Italian locale."
            ItalianTtsVoiceStatus.FALLBACK_VOICE_TRIGGERED ->
                "The language request was accepted but the engine's ACTIVE voice is not Italian."
            else -> null
        }
        return TtsVoiceReadback(
            requestedLocaleTag = requestedLocaleTag,
            setLanguageResult = setLanguageResult,
            selectedVoiceName = selectedVoiceName,
            activeVoiceName = activeVoiceName,
            activeVoiceLocaleTag = activeVoiceLocaleTag,
            activeLanguageTag = activeLanguageTag,
            engineName = engineName,
            status = status,
            availableItalianVoiceNames = availableItalianVoiceNames,
            detail = detail,
        )
    }

    /** One-line honest description for diagnostics rows. */
    fun describe(readback: TtsVoiceReadback): String = buildString {
        append(readback.status.name)
        append(" (requested ")
        append(readback.requestedLocaleTag)
        append(", active ")
        append(readback.activeVoiceLocaleTag ?: readback.activeLanguageTag ?: "unknown")
        readback.activeVoiceName?.let { append(", voice ").append(it) }
        readback.engineName?.let { append(", engine ").append(it) }
        append(")")
    }
}
