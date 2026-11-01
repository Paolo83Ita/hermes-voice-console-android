package com.hermestower.voiceconsole.models

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.time.Instant

/**
 * ModelRollbackController (GRAPH 2 §4): preserves the previous verified version and
 * restores it on demand. Rollback restores the exact previous bytes (verified by
 * hash) — never a partial or unknown artifact.
 */
interface ModelRollbackController {
    fun snapshotFor(entry: ModelCatalogEntry): Boolean
    fun rollback(entry: ModelCatalogEntry): RollbackResult
    fun removalCleanup(entry: ModelCatalogEntry): Boolean
}

data class RollbackResult(
    val identity: String,
    val success: Boolean,
    val restoredVersion: String?,
    val reason: String,
    val at: Instant,
)

class DefaultModelRollbackController(
    private val storage: ModelStorageManager,
    private val registry: ModelInstallationRegistry,
) : ModelRollbackController {

    private fun backupPath(entry: ModelCatalogEntry): Path? =
        // Identity-based: the backup must survive version changes (the previous
        // version's filename differs from the current one's by design).
        runCatching { storage.componentDir(entry).resolve("${entry.identity}.pkg.prev") }.getOrNull()

    override fun snapshotFor(entry: ModelCatalogEntry): Boolean {
        val current = storage.installedFile(entry) ?: return false
        val backup = backupPath(entry) ?: return false
        return try {
            Files.copy(current, backup, StandardCopyOption.REPLACE_EXISTING)
            registry.markRollbackPoint(entry.identity, entry)
            true
        } catch (_: Exception) {
            false
        }
    }

    override fun rollback(entry: ModelCatalogEntry): RollbackResult {
        val record = registry.record(entry.identity)
        val backup = backupPath(entry)
        if (record == null || !record.rollbackAvailable || backup == null || !Files.exists(backup)) {
            return RollbackResult(entry.identity, false, null, "No rollback point preserved.", Instant.now())
        }
        val restoredHash = try {
            ModelSha256.ofFile(backup)
        } catch (_: Exception) {
            null
        }
        if (restoredHash != record.previousSha256) {
            return RollbackResult(entry.identity, false, null, "Backup hash mismatch; rollback refused.", Instant.now())
        }
        return try {
            val current = storage.installedFile(entry)
            if (current != null) Files.move(current, current.resolveSibling(current.fileName.toString() + ".superseded"), StandardCopyOption.REPLACE_EXISTING)
            Files.move(backup, current!!, StandardCopyOption.REPLACE_EXISTING)
            registry.markState(entry.identity, ModelInstallState.QUALIFIED, "rolled back to ${record.previousVersion}")
            registry.clearRollback(entry.identity)
            current.resolveSibling(current.fileName.toString() + ".superseded").toFile().delete()
            RollbackResult(entry.identity, true, record.previousVersion, "Restored verified previous version.", Instant.now())
        } catch (e: Exception) {
            RollbackResult(entry.identity, false, null, "Rollback failed: ${e.message}", Instant.now())
        }
    }

    override fun removalCleanup(entry: ModelCatalogEntry): Boolean {
        val removed = storage.remove(entry)
        if (removed) {
            backupPath(entry)?.let { Files.deleteIfExists(it) }
            registry.removeRecord(entry.identity)
        }
        return removed
    }
}
