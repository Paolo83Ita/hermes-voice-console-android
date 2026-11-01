package com.hermestower.voiceconsole.models

import kotlinx.serialization.json.Json

/**
 * VoiceModelCatalog (GRAPH 2 §4 required contract). Immutable view over the shipped
 * manifest; mutation of lifecycle fields happens through the installation registry.
 */
interface VoiceModelCatalog {
    fun all(): List<ModelCatalogEntry>
    fun find(identity: String, version: String? = null): ModelCatalogEntry?
    fun ofComponent(type: ModelComponentType): List<ModelCatalogEntry>
    fun manifestVersion(): String
}

class DefaultVoiceModelCatalog(
    private val manifest: VoicePackManifest,
) : VoiceModelCatalog {

    private val entries = manifest.entries.toList()

    override fun all(): List<ModelCatalogEntry> = entries

    override fun find(identity: String, version: String?): ModelCatalogEntry? =
        entries.filter { it.identity == identity }
            .let { list -> if (version == null) list.lastOrNull() else list.firstOrNull { it.version == version } }

    override fun ofComponent(type: ModelComponentType): List<ModelCatalogEntry> =
        entries.filter { it.componentType == type.name }

    override fun manifestVersion(): String = manifest.manifestVersion

    companion object {
        val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

        fun fromText(text: String): DefaultVoiceModelCatalog =
            DefaultVoiceModelCatalog(json.decodeFromString(VoicePackManifest.serializer(), text))
    }
}
