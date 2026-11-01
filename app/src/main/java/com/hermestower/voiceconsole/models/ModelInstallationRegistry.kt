package com.hermestower.voiceconsole.models

import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.time.Instant

/**
 * ModelInstallationRegistry (GRAPH 2 §4): persistent lifecycle ledger for every
 * installed component — state, previous version, rollback availability, last
 * verification/qualification. Backed by a JSON file with atomic replacement
 * (write temp + move). JVM tests inject a plain directory.
 */
interface ModelInstallationRegistry {
    fun snapshot(): List<InstallationRecord>
    fun record(identity: String): InstallationRecord?
    fun markState(identity: String, state: ModelInstallState, note: String?)
    fun markInstalled(entry: ModelCatalogEntry, installedPath: Path)
    fun markVerified(identity: String, at: Instant)
    fun markQualified(identity: String, at: Instant)
    fun markActive(identity: String, active: Boolean)
    fun markRollbackPoint(identity: String, previous: ModelCatalogEntry)
    fun clearRollback(identity: String)
    fun removeRecord(identity: String)
}

@Serializable
data class InstallationRecord(
    val identity: String,
    val componentType: String,
    val displayName: String,
    val version: String,
    val state: String,
    val active: Boolean = false,
    val installedPath: String? = null,
    val sha256: String,
    val previousVersion: String? = null,
    val previousSha256: String? = null,
    val rollbackAvailable: Boolean = false,
    val lastVerificationAt: String? = null,
    val lastQualificationAt: String? = null,
    val lastNote: String? = null,
    val updatedAt: String,
)

class FileInstallationRegistry(
    private val file: Path,
) : ModelInstallationRegistry {

    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true; encodeDefaults = true }
    private val lock = Any()

    override fun snapshot(): List<InstallationRecord> = synchronized(lock) { readAll() }

    override fun record(identity: String): InstallationRecord? =
        synchronized(lock) { readAll().firstOrNull { it.identity == identity } }

    override fun markState(identity: String, state: ModelInstallState, note: String?) {
        synchronized(lock) { mutate(identity) { it.copy(state = state.name, lastNote = note, updatedAt = now()) } }
    }

    override fun markInstalled(entry: ModelCatalogEntry, installedPath: Path) {
        synchronized(lock) {
            val records = readAll().toMutableList()
            val existing = records.firstOrNull { it.identity == entry.identity }
            val rec = InstallationRecord(
                identity = entry.identity,
                componentType = entry.componentType,
                displayName = entry.displayName,
                version = entry.version,
                state = ModelInstallState.INSTALLED.name,
                active = false,
                installedPath = installedPath.toString(),
                sha256 = entry.sha256,
                previousVersion = existing?.takeIf { it.version != entry.version }?.version,
                previousSha256 = existing?.takeIf { it.version != entry.version }?.sha256,
                rollbackAvailable = existing != null && existing.version != entry.version && existing.previousSha256 != null,
                updatedAt = now(),
            )
            records.removeAll { it.identity == entry.identity }
            records += rec
            writeAll(records)
        }
    }

    override fun markVerified(identity: String, at: Instant) {
        synchronized(lock) { mutate(identity) { it.copy(lastVerificationAt = at.toString(), updatedAt = now()) } }
    }

    override fun markQualified(identity: String, at: Instant) {
        synchronized(lock) { mutate(identity) { it.copy(lastQualificationAt = at.toString(), state = ModelInstallState.QUALIFIED.name, updatedAt = now()) } }
    }

    override fun markActive(identity: String, active: Boolean) {
        synchronized(lock) { mutate(identity) { it.copy(active = active, state = if (active) ModelInstallState.ACTIVE.name else ModelInstallState.QUALIFIED.name, updatedAt = now()) } }
    }

    override fun markRollbackPoint(identity: String, previous: ModelCatalogEntry) {
        synchronized(lock) {
            mutate(identity) {
                it.copy(
                    previousVersion = previous.version,
                    previousSha256 = previous.sha256,
                    rollbackAvailable = true,
                    state = ModelInstallState.ROLLBACK_AVAILABLE.name,
                    updatedAt = now(),
                )
            }
        }
    }

    override fun clearRollback(identity: String) {
        synchronized(lock) { mutate(identity) { it.copy(rollbackAvailable = false, updatedAt = now()) } }
    }

    override fun removeRecord(identity: String) {
        synchronized(lock) {
            val records = readAll().toMutableList()
            records.removeAll { it.identity == identity }
            writeAll(records)
        }
    }

    private fun mutate(identity: String, transform: (InstallationRecord) -> InstallationRecord) {
        val records = readAll().toMutableList()
        val idx = records.indexOfFirst { it.identity == identity }
        if (idx >= 0) {
            records[idx] = transform(records[idx])
            writeAll(records)
        }
    }

    private fun readAll(): List<InstallationRecord> {
        if (!Files.exists(file)) return emptyList()
        val text = String(Files.readAllBytes(file), Charsets.UTF_8)
        if (text.isBlank()) return emptyList()
        return runCatching {
            json.decodeFromString(ListSerializer(InstallationRecord.serializer()), text)
        }.getOrDefault(emptyList())
    }

    private fun writeAll(records: List<InstallationRecord>) {
        Files.createDirectories(file.parent)
        val tmp = file.resolveSibling(file.fileName.toString() + ".tmp")
        Files.write(tmp, json.encodeToString(ListSerializer(InstallationRecord.serializer()), records).toByteArray(Charsets.UTF_8))
        Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
    }

    private fun now(): String = Instant.now().toString()
}
