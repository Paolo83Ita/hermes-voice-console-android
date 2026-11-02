package com.hermestower.voiceconsole.models

import android.content.Context
import android.os.storage.StorageManager
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption

/**
 * ModelStorageManager (GRAPH 2 §4): application-controlled storage with storage
 * preflight, atomic installation (temp file + move) and cleanup of failed temps.
 * Storage root is `filesDir/models` on Android; a plain directory in JVM tests.
 */
interface ModelStorageManager {
    fun preflight(neededBytes: Long): StoragePreflight
    fun componentDir(entry: ModelCatalogEntry): Path
    fun tempDir(): Path
    fun installAtomically(entry: ModelCatalogEntry, tempFile: Path): Path
    fun installedFile(entry: ModelCatalogEntry): Path?
    fun remove(entry: ModelCatalogEntry): Boolean
    fun cleanupFailedTempFiles()
}

data class StoragePreflight(
    val sufficient: Boolean,
    val freeBytes: Long,
    val requiredBytes: Long,
    val message: String,
)

class DefaultModelStorageManager(
    private val root: Path,
    private val reserveBytes: Long = DefaultVoicePackResolver.INSTALL_RESERVE_BYTES,
    private val context: Context? = null,
) : ModelStorageManager {

    override fun componentDir(entry: ModelCatalogEntry): Path =
        root.resolve(entry.storageLocation.removePrefix("filesDir/").trim('/'))

    override fun preflight(neededBytes: Long): StoragePreflight {
        runCatching { Files.createDirectories(root) }
        // Graph 3 E2E evidence (run 41368013409): java.nio.Files.getFileStore() is NOT
        // supported on Android/ART — every download instantly failed with error
        // "getFileStore" (bytes=0/0) while the desktop-JVM unit tests passed. The
        // java.io.File space query is supported on both JVM and Android (API 9+).
        val file = root.toFile()
        var free = runCatching {
            val viaFile = file.usableSpace
            if (viaFile > 0) viaFile else Files.getFileStore(root).usableSpace
        }.getOrElse {
            // Last resort on exotic filesystems: report unavailable so the preflight
            // degrades truthfully instead of crashing the download pipeline.
            -1L
        }
        // GRAPH 4 NODE 0 (UsableSpace): when a Context is available, the preflight
        // also consults StorageManager.getAllocatableBytes, which counts clearable
        // cached data the system could evict — the space that is truly allocatable
        // to this download. The larger of the two views wins; failures degrade
        // truthfully back to the File view. (API 26+ APIs; minSdk is 30.)
        context?.let { appContext ->
            if (free >= 0) {
                runCatching {
                    val sm = appContext.getSystemService(Context.STORAGE_SERVICE) as StorageManager
                    // filesDir lives on primary internal storage → UUID_DEFAULT.
                    val allocatable = sm.getAllocatableBytes(StorageManager.UUID_DEFAULT)
                    if (allocatable > free) free = allocatable
                }
            }
        }
        // GRAPH 4 NODE 0 (mission §7): the safe reserve is the GREATER of the fixed
        // floor and 15 percent of the storage currently available, so a download can
        // never push the device into its low-storage regime. The floor keeps the
        // historical 256 MB behavior for JVM tests and emulator fixtures; device
        // builds constructed with a Context get the full policy.
        val reserve = if (context != null && free > 0) {
            maxOf(reserveBytes, free / 100L * 15L)
        } else {
            reserveBytes
        }
        val required = neededBytes + reserve
        return StoragePreflight(
            sufficient = free >= required,
            freeBytes = free,
            requiredBytes = required,
            message = when {
                free < 0 -> "Storage preflight unavailable on this filesystem (free space unknown)."
                free >= required -> "Storage preflight passed ($free bytes free, $reserve reserved)."
                else -> "Storage preflight failed: $free bytes free, $required required (download + safe reserve of $reserve)."
            },
        )
    }

    override fun tempDir(): Path {
        val dir = root.resolve("tmp")
        Files.createDirectories(dir)
        return dir
    }

    override fun installAtomically(entry: ModelCatalogEntry, tempFile: Path): Path {
        val targetDir = root.resolve(entry.storageLocation.removePrefix("filesDir/").trim('/'))
        Files.createDirectories(targetDir)
        val target = targetDir.resolve("${entry.identity}-${entry.version}.pkg")
        // Atomic within the same filesystem: move, then fsync the directory entry.
        Files.move(tempFile, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
        return target
    }

    override fun installedFile(entry: ModelCatalogEntry): Path? {
        val target = root.resolve(entry.storageLocation.removePrefix("filesDir/").trim('/'))
            .resolve("${entry.identity}-${entry.version}.pkg")
        return if (Files.exists(target)) target else null
    }

    override fun remove(entry: ModelCatalogEntry): Boolean {
        val file = installedFile(entry) ?: return false
        return try {
            Files.deleteIfExists(file)
        } catch (_: IOException) {
            false
        }
    }

    override fun cleanupFailedTempFiles() {
        val dir = root.resolve("tmp")
        if (!Files.isDirectory(dir)) return
        dir.toFile().listFiles()?.forEach { it.delete() }
    }

    companion object {
        fun defaultRoot(filesDir: File): Path = filesDir.toPath().resolve("models")
    }
}
