package com.hermestower.voiceconsole.locale

/**
 * GRAPH 4.10 CP-G4.10-002 (§4 RESPONSE LOCALE AUTHORITY).
 *
 * The ONE canonical authority for the language of every locally produced
 * user-visible response (local conversation answers, driver status notes,
 * MockTowerBridge / simulator strings, local LLM system prompt).
 *
 * Required precedence:
 *   1. explicit conversation language;
 *   2. effective application language (AppLanguage override → system display);
 *   3. recognized input-language hint (with confidence where detection is used);
 *   4. system locale;
 *   5. English fallback.
 *
 * Producers must NOT duplicate this logic: they receive the resolved tag.
 * Pure JVM (no Android Context) so the whole chain is unit-testable.
 */
class ResponseLocaleResolver(
    /** Effective application language ("it".."de" — AppLanguage.effectiveTag in prod). */
    private val appLanguageTag: () -> String,
    /** Device system language (Locale.getDefault().language in prod). */
    private val systemLanguageTag: () -> String = { java.util.Locale.getDefault().language },
) {

    enum class Source { EXPLICIT_CONVERSATION, APP_LANGUAGE, INPUT_HINT, SYSTEM_LOCALE, FALLBACK }

    /**
     * The resolution result.
     * @param tag BCP-47 language tag normalized to a supported language code.
     * @param source which precedence level produced the tag.
     * @param fallbackReason why a higher-precedence level was skipped (diagnostic, English).
     * @param confidence detection confidence when [Source.INPUT_HINT] is used, else null.
     */
    data class Resolution(
        val tag: String,
        val source: Source,
        val fallbackReason: String?,
        val confidence: Float?,
    )

    fun resolve(
        explicitConversationLanguage: String? = null,
        inputLanguageHint: String? = null,
        hintConfidence: Float? = null,
    ): Resolution {
        val skip: MutableList<String> = mutableListOf()

        // 1. explicit conversation language
        if (explicitConversationLanguage != null) {
            normalize(explicitConversationLanguage)?.let { tag ->
                if (ResponseCatalog.isSupported(tag)) {
                    return Resolution(tag, Source.EXPLICIT_CONVERSATION, null, null)
                }
                skip.add("explicit conversation language '$explicitConversationLanguage' is not supported")
            } ?: skip.add("explicit conversation language '$explicitConversationLanguage' is not parseable")
        }

        // 2. effective application language
        val app = normalize(appLanguageTag())
        if (app != null && ResponseCatalog.isSupported(app)) {
            val reason = if (skip.isEmpty()) null else skip.joinToString("; ")
            return Resolution(app, Source.APP_LANGUAGE, reason, null)
        }
        skip.add("application language '${appLanguageTag()}' is not supported")

        // 3. recognized input-language hint (detection carries confidence)
        if (inputLanguageHint != null) {
            normalize(inputLanguageHint)?.let { tag ->
                if (ResponseCatalog.isSupported(tag)) {
                    val reason = if (skip.isEmpty()) null else skip.joinToString("; ")
                    return Resolution(tag, Source.INPUT_HINT, reason, hintConfidence)
                }
                skip.add("input-language hint '$inputLanguageHint' is not supported")
            } ?: skip.add("input-language hint '$inputLanguageHint' is not parseable")
        }

        // 4. system locale
        val system = normalize(systemLanguageTag())
        if (system != null && ResponseCatalog.isSupported(system)) {
            val reason = if (skip.isEmpty()) null else skip.joinToString("; ")
            return Resolution(system, Source.SYSTEM_LOCALE, reason, null)
        }

        // 5. English fallback
        val reason = (skip + "system locale '${systemLanguageTag()}' is not supported")
            .joinToString("; ")
        return Resolution(
            tag = "en",
            source = Source.FALLBACK,
            fallbackReason = reason,
            confidence = null,
        )
    }

    companion object {
        /** Normalizes a BCP-47 tag to its base language code ("it-IT" → "it"), or null. */
        fun normalize(tag: String?): String? {
            if (tag.isNullOrBlank()) return null
            val base = tag.trim().lowercase(java.util.Locale.ROOT).substringBefore('-')
            return base.ifBlank { null }
        }

        /** JVM-default resolver (English) for pure-JVM tests that never resolve locale. */
        fun englishDefaults(): ResponseLocaleResolver =
            ResponseLocaleResolver(appLanguageTag = { "en" }, systemLanguageTag = { "en" })
    }
}
