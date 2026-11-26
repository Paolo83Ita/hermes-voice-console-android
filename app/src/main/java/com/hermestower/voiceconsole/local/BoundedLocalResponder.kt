package com.hermestower.voiceconsole.local

import com.hermestower.voiceconsole.domain.LocalConversationEngine
import com.hermestower.voiceconsole.locale.ResponseCategory
import com.hermestower.voiceconsole.locale.ResponseCatalog
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * ANDROID REPAIR CP-007/008 — the PRODUCTION bounded local responder
 * (mission section 10: Qwen may handle only bounded low-risk tasks; the
 * rule-based lane covers greetings, short help, intent normalization and
 * status explanation when the model is not enabled/loaded).
 *
 * This is the honestly-named successor of MockLocalConversationEngine on the
 * production route: same deterministic localized ResponseCatalog answers
 * (nothing invented, nothing labeled as model output), but named for what it
 * IS (the bounded rule-based lane) and carrying the normalized intent for the
 * route-decision exposure. MockLocalConversationEngine is demoted to
 * TEST_FIXTURE_ONLY (mission section 8: the mock may remain only as a test
 * fixture; ZERO_PRODUCTION_MOCK_EXECUTOR).
 */
class BoundedLocalResponder : LocalConversationEngine {

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
