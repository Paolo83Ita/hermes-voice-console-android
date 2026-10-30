package com.hermestower.voiceconsole.core.routing

import com.hermestower.voiceconsole.core.mode.OperatingMode

/**
 * Deterministic rule table for V0.1 (ARCHITECTURE.md §4).
 *
 * Order of evaluation:
 *   1. mode OFF            -> DECLINE
 *   2. local whitelist     -> HANDLE_LOCAL where the mode permits (safe local functions
 *                             are allowed even in DISCONNECTED_SAFE)
 *   3. consequential       -> ROUTE_TOWER, or DECLINE when the Tower is unreachable or
 *                             authority is unavailable (a consequential request is NEVER queued)
 *   4. anything else       -> ROUTE_TOWER / QUEUE_FOR_TOWER / DECLINE per mode and connectivity
 *
 * Whitelist beats consequential on compound phrases; the reason string records what
 * matched so the History screen can explain itself.
 */
class RuleBasedIntentRouter : IntentRouter {

    private data class Rule(val intent: String, val regex: Regex)

    private val localWhitelist = listOf(
        Rule("GREETING", Regex("(?i)\\b(hello|hi|good (morning|evening|day)|ciao|buongiorno|buonasera|salve)\\b")),
        Rule("HELP", Regex("(?i)\\b(help|what can you do|aiuto|cosa sai fare|come funzioni)\\b")),
        Rule("DEVICE_STATUS", Regex("(?i)\\b(status|battery|memory|storage|temperature|stato|batteria|memoria|temperatura)\\b")),
        Rule("TIME_DATE", Regex("(?i)\\b(what time|the date|che ora|che giorno|oggi)\\b")),
        Rule("IDENTITY", Regex("(?i)\\b(who are you|chi sei|your name|come ti chiami)\\b")),
        Rule("THANKS", Regex("(?i)\\b(thanks|thank you|grazie)\\b")),
    )

    private val consequentialPatterns = listOf(
        Regex("(?i)\\b(mission|plan|execute|esegui|run|send|invia|delete|elimina|open|apri)\\b"),
        Regex("(?i)\\b(control|controlla|approve|approva|deploy|schedule|programma)\\b"),
        Regex("(?i)\\b(purchase|compra|confirm|conferma|shutdown|arresta)\\b"),
        Regex("(?i)\\btower\\b"),
    )

    override fun route(request: RoutingRequest): RoutingDecision {
        val text = request.text.trim()
        if (text.isEmpty()) {
            return RoutingDecision(RoutingAction.DECLINE, "EMPTY", request.sttConfidence, false, "Empty request.")
        }

        if (request.effectiveMode == OperatingMode.OFF || request.requestedMode == OperatingMode.OFF) {
            return RoutingDecision(RoutingAction.DECLINE, "OFF", request.sttConfidence, false, "Console is OFF; voice processing is disabled.")
        }

        // 2. Local whitelist (safe functions).
        val whitelisted = localWhitelist.firstOrNull { it.regex.containsMatchIn(text) }
        if (whitelisted != null) {
            return when (request.effectiveMode) {
                OperatingMode.LOCAL, OperatingMode.HYBRID, OperatingMode.DISCONNECTED_SAFE ->
                    if (request.effectiveMode == OperatingMode.HYBRID && request.sttConfidence < LOW_CONFIDENCE) {
                        RoutingDecision(
                            RoutingAction.ROUTE_TOWER, whitelisted.intent, request.sttConfidence, false,
                            "Whitelisted intent '${whitelisted.intent}' but STT confidence ${request.sttConfidence} is low; Tower disambiguates.",
                        )
                    } else {
                        RoutingDecision(
                            RoutingAction.HANDLE_LOCAL, whitelisted.intent, request.sttConfidence, false,
                            "Whitelisted local intent '${whitelisted.intent}'.",
                        )
                    }
                OperatingMode.TOWER ->
                    RoutingDecision(RoutingAction.ROUTE_TOWER, whitelisted.intent, request.sttConfidence, false, "Mode TOWER routes every request to the Tower.")
                OperatingMode.OFF -> RoutingDecision(RoutingAction.DECLINE, "OFF", request.sttConfidence, false, "Console is OFF.")
            }
        }

        // 3. Consequential requests never resolve locally and never queue silently.
        val consequential = consequentialPatterns.any { it.containsMatchIn(text) }
        if (consequential) {
            return when {
                request.effectiveMode == OperatingMode.LOCAL ->
                    RoutingDecision(RoutingAction.DECLINE, "CONSEQUENTIAL_LOCAL", request.sttConfidence, true, "Consequential requests require Hermes Tower authority, unavailable in LOCAL mode.")
                request.effectiveMode == OperatingMode.DISCONNECTED_SAFE || request.connectivity == Connectivity.DISCONNECTED ->
                    RoutingDecision(RoutingAction.DECLINE, "CONSEQUENTIAL_OFFLINE", request.sttConfidence, true, "Consequential requests cannot be queued offline; they require the Tower and, where needed, Owner approval.")
                else ->
                    RoutingDecision(RoutingAction.ROUTE_TOWER, "CONSEQUENTIAL", request.sttConfidence, true, "Consequential content detected; routed to Hermes Tower authority.")
            }
        }

        // 4. Everything else.
        return when {
            request.effectiveMode == OperatingMode.LOCAL ->
                RoutingDecision(RoutingAction.DECLINE, "NOT_LOCAL", request.sttConfidence, false, "Not a whitelisted local intent; LOCAL mode declines it.")
            request.effectiveMode == OperatingMode.DISCONNECTED_SAFE || request.connectivity == Connectivity.DISCONNECTED ->
                RoutingDecision(RoutingAction.QUEUE_FOR_TOWER, "QUEUED_REQUEST", request.sttConfidence, false, "Non-consequential request queued for delivery when the Tower is reachable.")
            else ->
                RoutingDecision(
                    RoutingAction.ROUTE_TOWER, "TOWER_REQUEST", request.sttConfidence, false,
                    if (request.sttConfidence < LOW_CONFIDENCE) "Uncertain phrasing (confidence ${request.sttConfidence}); Tower decides."
                    else "No local qualification; forwarded to the Tower.",
                )
        }
    }

    companion object {
        const val LOW_CONFIDENCE = 0.55f
    }
}
