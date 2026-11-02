package com.hermestower.voiceconsole.voice

/**
 * Microphone permission states (GRAPH 4 NODE 3, §8): the pure logic behind the
 * Android permission flow. The UI layer maps Android's grant results into these
 * states; the explanation strings are the rationale the UI shows BEFORE the
 * system dialog and after denials. Nothing here records audio — this is the
 * consent contract only.
 */
enum class MicPermissionState(val displayLabel: String) {
    NOT_DETERMINED("Permission not requested yet"),
    GRANTED("Microphone granted"),
    DENIED("Microphone denied"),
    PERMANENTLY_DENIED("Microphone permanently denied — enable it in Settings"),
}

object MicPermissionFlow {

    /** Why the microphone is needed (shown BEFORE the system dialog). */
    const val RATIONALE: String =
        "Hermes Voice Console needs the microphone only while you actively speak to it: " +
            "you press the talk control, your speech is transcribed on the device or sent to " +
            "your Hermes Tower, and the capture stops as soon as the turn ends. Raw audio is " +
            "never stored, and the privacy mute physically disconnects the microphone."

    /** Shown after a plain denial (can ask again). */
    const val DENIED_GUIDANCE: String =
        "Without the microphone, voice input stays unavailable. You can still type your " +
            "requests; the console works fully. Voice can be enabled again at any time."

    /** Shown after permanent denial (must go to Settings). */
    const val PERMANENT_GUIDANCE: String =
        "Voice input is permanently disabled for this app. Open Android Settings → Apps → " +
            "Hermes Voice Console → Permissions → Microphone to enable it."

    /** The state machine: system result + shouldShowRationale → typed state. */
    fun classify(granted: Boolean, shouldShowRationale: Boolean, previouslyRequested: Boolean): MicPermissionState = when {
        granted -> MicPermissionState.GRANTED
        !shouldShowRationale && previouslyRequested -> MicPermissionState.PERMANENTLY_DENIED
        else -> MicPermissionState.DENIED
    }
}
