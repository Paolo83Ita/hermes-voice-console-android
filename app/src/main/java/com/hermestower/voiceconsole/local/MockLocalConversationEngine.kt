package com.hermestower.voiceconsole.local

import com.hermestower.voiceconsole.domain.LocalConversationEngine
import com.hermestower.voiceconsole.locale.ResponseCategory
import com.hermestower.voiceconsole.locale.ResponseCatalog
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Deterministic local conversation front-end for V0.1 (FR-18 roles, no model loaded).
 * Answers are canned and labeled LOCAL by the caller; nothing here is Tower truth.
 *
 * GRAPH 4.10 CP-G4.10-002: every user-visible reply comes from the localized
 * ResponseCatalog (5 locales) — never hard-coded English. The locale tag is
 * RECEIVED from the ResponseLocaleResolver authority, never resolved here.
 * Intent matching already accepts Italian/English keywords; intent NAMES stay
 * diagnostic (English constants, event log only).
 */
class MockLocalConversationEngine : LocalConversationEngine {

    private val _modelState = MutableStateFlow(LocalConversationEngine.ModelState.NOT_LOADED)
    override val modelState: StateFlow<LocalConversationEngine.ModelState> = _modelState.asStateFlow()

    override fun respond(text: String, statusProvider: () -> String): LocalConversationEngine.LocalReply =
        respond(text, statusProvider, "en")

    override fun respond(text: String, statusProvider: () -> String, localeTag: String): LocalConversationEngine.LocalReply {
        val t = text.trim().lowercase()
        val (reply, intent) = when {
            Regex("(?i)\\b(ciao|hello|hi|buongiorno|buonasera|good (morning|evening|day)|salve)\\b").containsMatchIn(t) ->
                ResponseCatalog.text(ResponseCategory.GREETING, localeTag) to "GREETING"
            Regex("(?i)\\b(status|stato|battery|batteria|memory|memoria|storage|temperature|temperatura)\\b").containsMatchIn(t) ->
                ResponseCatalog.text(ResponseCategory.CONSOLE_STATUS, localeTag, statusProvider()) to "DEVICE_STATUS"
            Regex("(?i)\\b(help|aiuto|cosa sai fare|what can you do|come funzioni)\\b").containsMatchIn(t) ->
                ResponseCatalog.text(ResponseCategory.HELP, localeTag) to "HELP"
            Regex("(?i)\\b(who are you|chi sei|your name|come ti chiami)\\b").containsMatchIn(t) ->
                ResponseCatalog.text(ResponseCategory.IDENTITY, localeTag) to "IDENTITY"
            Regex("(?i)\\b(che ora|what time|che giorno|the date|oggi)\\b").containsMatchIn(t) ->
                ResponseCatalog.text(ResponseCategory.TIME_DATE, localeTag, statusProvider()) to "TIME_DATE"
            Regex("(?i)\\b(grazie|thanks|thank you)\\b").containsMatchIn(t) ->
                ResponseCatalog.text(ResponseCategory.THANKS, localeTag) to "THANKS"
            else ->
                ResponseCatalog.text(ResponseCategory.UNKNOWN_SAFE, localeTag) to "NOT_LOCAL"
        }
        return LocalConversationEngine.LocalReply(text = reply, intent = intent, fromModel = false)
    }
}
