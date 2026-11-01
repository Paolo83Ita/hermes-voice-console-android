package com.hermestower.voiceconsole.models

import java.time.Instant

/**
 * ModelActivationController (GRAPH 2 §4): activation only after successful
 * qualification; read-back of the active configuration; rollback preserved.
 */
interface ModelActivationController {
    fun activate(entry: ModelCatalogEntry): ActivationResult
    fun deactivate(entry: ModelCatalogEntry): ActivationResult
    fun readBack(): List<ActiveComponent>
}

data class ActivationResult(
    val identity: String,
    val success: Boolean,
    val reason: String,
    val at: Instant,
)

data class ActiveComponent(
    val identity: String,
    val componentType: String,
    val version: String,
    val sha256: String,
    val activatedAt: String,
)

class DefaultModelActivationController(
    private val registry: ModelInstallationRegistry,
    private val qualificationRunner: ModelQualificationRunner,
) : ModelActivationController {

    override fun activate(entry: ModelCatalogEntry): ActivationResult {
        val record = registry.record(entry.identity)
        if (record == null || record.sha256 != entry.sha256) {
            return ActivationResult(entry.identity, false, "Component is not registered as installed.", Instant.now())
        }
        if (entry.componentType == ModelComponentType.LOCAL_LLM.name) {
            val profileGate = registry.record(entry.identity)?.state
            if (profileGate != null && profileGate == ModelInstallState.INCOMPATIBLE.name) {
                return ActivationResult(entry.identity, false, "Marked incompatible; activation refused.", Instant.now())
            }
        }
        val qualification = qualificationRunner.qualify(entry)
        registry.markVerified(entry.identity, qualification.at)
        if (!qualification.qualified) {
            return ActivationResult(
                entry.identity,
                false,
                "Qualification failed: " + qualification.checks.filter { !it.passed }.joinToString { it.name },
                qualification.at,
            )
        }
        registry.markQualified(entry.identity, qualification.at)
        registry.markActive(entry.identity, true)
        return ActivationResult(entry.identity, true, "Active (qualification ${qualification.checks.count { it.passed }}/${qualification.checks.size} passed).", qualification.at)
    }

    override fun deactivate(entry: ModelCatalogEntry): ActivationResult {
        val record = registry.record(entry.identity)
        return if (record != null && record.active) {
            registry.markActive(entry.identity, false)
            ActivationResult(entry.identity, true, "Deactivated.", Instant.now())
        } else {
            ActivationResult(entry.identity, false, "Not currently active.", Instant.now())
        }
    }

    override fun readBack(): List<ActiveComponent> = registry.snapshot()
        .filter { it.active }
        .map { ActiveComponent(it.identity, it.componentType, it.version, it.sha256, it.updatedAt) }
}
