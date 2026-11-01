package com.hermestower.voiceconsole.models

/**
 * VoicePackPolicyResolver (GRAPH 2 §4). Encodes per-profile component policy:
 * which component classes a profile requires, which are optional, and whether the
 * profile may route complex reasoning locally (it never does — Tower is the only
 * authority for complex reasoning in every profile).
 */
object VoicePackPolicyResolver {

    data class Policy(
        val required: Set<ModelComponentType>,
        val optional: Set<ModelComponentType>,
        val allowsLocalLlm: Boolean,
        val allowsExperimental: Boolean,
        val defaultRoute: String,
    )

    fun policyFor(profile: VoiceModelProfile): Policy = when (profile) {
        VoiceModelProfile.TOWER_FIRST -> Policy(
            required = setOf(ModelComponentType.WAKE_WORD, ModelComponentType.VAD),
            optional = setOf(ModelComponentType.SPEECH_TO_TEXT, ModelComponentType.TTS_ENGINE, ModelComponentType.VOICE),
            allowsLocalLlm = false,
            allowsExperimental = false,
            defaultRoute = "TOWER",
        )
        VoiceModelProfile.HYBRID_BALANCED -> Policy(
            required = setOf(ModelComponentType.WAKE_WORD, ModelComponentType.VAD, ModelComponentType.SPEECH_TO_TEXT, ModelComponentType.TTS_ENGINE),
            optional = setOf(ModelComponentType.LOCAL_LLM, ModelComponentType.NOISE_SUPPRESSION, ModelComponentType.VOICE),
            allowsLocalLlm = true,
            allowsExperimental = false,
            defaultRoute = "LOCAL_SIMPLE_TOWER_COMPLEX",
        )
        VoiceModelProfile.LOCAL_EXPERIMENTAL -> Policy(
            required = setOf(ModelComponentType.WAKE_WORD, ModelComponentType.VAD, ModelComponentType.SPEECH_TO_TEXT, ModelComponentType.LOCAL_LLM, ModelComponentType.TTS_ENGINE),
            optional = setOf(ModelComponentType.NOISE_SUPPRESSION, ModelComponentType.VOICE),
            allowsLocalLlm = true,
            allowsExperimental = true,
            defaultRoute = "LOCAL_FIRST_TOWER_FALLBACK",
        )
        VoiceModelProfile.MANUAL_ADVANCED -> Policy(
            required = emptySet(),
            optional = ModelComponentType.entries.toSet(),
            allowsLocalLlm = true,
            allowsExperimental = true,
            defaultRoute = "MANUAL_PER_COMPONENT",
        )
    }

    /** RAM/storage/thermal gates for the experimental profile (GRAPH 2 §4). */
    fun gatesFor(profile: VoiceModelProfile, resources: DeviceResources): List<String> {
        val gates = mutableListOf<String>()
        if (profile == VoiceModelProfile.LOCAL_EXPERIMENTAL) {
            if (resources.availableRamBytes < EXPERIMENTAL_MIN_AVAILABLE_RAM) {
                gates += "RAM gate: available RAM below 3 GB — LOCAL_EXPERIMENTAL stays disabled."
            }
            if (resources.freeStorageBytes < EXPERIMENTAL_MIN_FREE_STORAGE) {
                gates += "Storage gate: free storage below 6 GB — LOCAL_EXPERIMENTAL stays disabled."
            }
            if (resources.thermalStatus != null && resources.thermalStatus >= 4) {
                gates += "Thermal gate: device thermally constrained — LOCAL_EXPERIMENTAL stays disabled."
            }
        }
        if (!resources.networkOnline) {
            gates += "Network gate: offline — downloads unavailable until a network returns."
        }
        return gates
    }

    const val EXPERIMENTAL_MIN_AVAILABLE_RAM: Long = 3L * 1024 * 1024 * 1024
    const val EXPERIMENTAL_MIN_FREE_STORAGE: Long = 6L * 1024 * 1024 * 1024
}
