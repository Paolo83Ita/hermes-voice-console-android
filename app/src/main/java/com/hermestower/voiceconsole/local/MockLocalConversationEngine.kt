package com.hermestower.voiceconsole.local

import com.hermestower.voiceconsole.domain.LocalConversationEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Deterministic local conversation front-end for V0.1 (FR-18 roles, no model loaded).
 * Answers are canned and labeled LOCAL by the caller; nothing here is Tower truth.
 */
class MockLocalConversationEngine : LocalConversationEngine {

    private val _modelState = MutableStateFlow(LocalConversationEngine.ModelState.NOT_LOADED)
    override val modelState: StateFlow<LocalConversationEngine.ModelState> = _modelState.asStateFlow()

    override fun respond(text: String, statusProvider: () -> String): LocalConversationEngine.LocalReply {
        val t = text.trim().lowercase()
        val (reply, intent) = when {
            Regex("(?i)\\b(ciao|hello|hi|buongiorno|buonasera|good (morning|evening|day)|salve)\\b").containsMatchIn(t) ->
                "Good day. Hermes Console standing by." to "GREETING"
            Regex("(?i)\\b(status|stato|battery|batteria|memory|memoria|storage|temperature|temperatura)\\b").containsMatchIn(t) ->
                "Console status: ${statusProvider()}" to "DEVICE_STATUS"
            Regex("(?i)\\b(help|aiuto|cosa sai fare|what can you do|come funzioni)\\b").containsMatchIn(t) ->
                "I can answer locally for greetings, time, identity and truthful device status. Everything else reaches Hermes Tower in HYBRID mode, or queues safely when offline." to "HELP"
            Regex("(?i)\\b(who are you|chi sei|your name|come ti chiami)\\b").containsMatchIn(t) ->
                "I am the Hermes Console, the voice front of Hermes Tower. Locally I speak only for myself; the Tower remains the authority." to "IDENTITY"
            Regex("(?i)\\b(che ora|what time|che giorno|the date|oggi)\\b").containsMatchIn(t) ->
                "Time and date are read from this device: ${statusProvider()}" to "TIME_DATE"
            Regex("(?i)\\b(grazie|thanks|thank you)\\b").containsMatchIn(t) ->
                "At your service." to "THANKS"
            else ->
                "This console answers only whitelisted local intents for now; your words would be safer with the Tower in HYBRID mode." to "NOT_LOCAL"
        }
        return LocalConversationEngine.LocalReply(text = reply, intent = intent, fromModel = false)
    }
}
