package com.hermestower.voiceconsole.bridge

/**
 * Deterministic Tower simulator (mission: "create a deterministic simulator").
 * No network, no randomness: keyword-driven canned responses with a documented fallback.
 */
class DeterministicTowerSimulator {

    fun respond(transcript: String): String {
        val t = transcript.trim().lowercase()
        return when {
            "status" in t && "mission" in t ->
                "Tower reports: mission queue is empty in this simulation. No real Tower data exists yet; this is a deterministic mock answer."
            "mission" in t || "piano" in t || "plan" in t ->
                "Understood. I can draft a mission plan in this simulation. In the real system I would plan carefully and ask for your approval before any consequential step."
            "report" in t ->
                "Report received and acknowledged by the simulated Tower. A receipt reference would be issued here."
            "approva" in t || "approv" in t ->
                "Approval flows are Tower authority. In production, nothing consequential proceeds without your explicit approval."
            "chi sei" in t || "who are you" in t ->
                "I am Hermes Tower, simulated edition: the authority side of this console, reduced to deterministic answers for V0.1 review."
            else ->
                "Tower acknowledged: \u00AB${transcript.take(80)}\u00BB. This is a simulated, deterministic response; no real Tower was contacted."
        }
    }

    /** Streaming cadence: chunk count is deterministic given the text (unit-pinned). */
    fun streamChunks(text: String): List<String> =
        text.split(" ").chunked(4).map { it.joinToString(" ") + " " }
}
