package com.hermestower.voiceconsole.bridge

import com.hermestower.voiceconsole.locale.ResponseCategory
import com.hermestower.voiceconsole.locale.ResponseCatalog
import com.hermestower.voiceconsole.locale.ResponseLocaleResolver

/**
 * Deterministic Tower simulator (mission: "create a deterministic simulator").
 * No network, no randomness: keyword-driven canned responses with a documented fallback.
 *
 * GRAPH 4.10 CP-G4.10-002: all deterministic Tower responses are localized via
 * the ResponseCatalog (5 locales). The SIMULATED truth is preserved verbatim in
 * every locale. The legacy one-arg form defaults to English (JVM tests).
 */
class DeterministicTowerSimulator {

    fun respond(transcript: String): String = respond(transcript, "en")

    fun respond(transcript: String, localeTag: String): String {
        val locale = ResponseLocaleResolver.normalize(localeTag) ?: "en"
        val t = transcript.trim().lowercase()
        return when {
            ("status" in t || "stato" in t) && ("mission" in t || "missione" in t) ->
                ResponseCatalog.text(ResponseCategory.SIM_TOWER_STATUS, locale)
            "mission" in t || "piano" in t || "plan" in t || "pianifica" in t ->
                ResponseCatalog.text(ResponseCategory.SIM_TOWER_PLAN, locale)
            "report" in t || "rapporto" in t ->
                ResponseCatalog.text(ResponseCategory.SIM_TOWER_REPORT, locale)
            "approva" in t || "approv" in t ->
                ResponseCatalog.text(ResponseCategory.SIM_TOWER_APPROVAL, locale)
            "chi sei" in t || "who are you" in t ->
                ResponseCatalog.text(ResponseCategory.SIM_TOWER_IDENTITY, locale)
            else ->
                ResponseCatalog.text(ResponseCategory.SIMULATED_ACK, locale, transcript.take(80))
        }
    }

    /** Streaming cadence: chunk count is deterministic given the text (unit-pinned). */
    fun streamChunks(text: String): List<String> =
        text.split(" ").chunked(4).map { it.joinToString(" ") + " " }
}
