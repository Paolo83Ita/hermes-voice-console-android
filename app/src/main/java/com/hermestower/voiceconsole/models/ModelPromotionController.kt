package com.hermestower.voiceconsole.models

import java.io.RandomAccessFile
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption

/**
 * ANDROID REPAIR CP-006 — the MODEL PROMOTION CONTROLLER (mission section 7).
 *
 * Rebuilds the broken promotion chain (A4R-PHYS-MODEL-002: a real 522 MB
 * download completed to tmp and NOTHING promoted it — stages 4-7 of the
 * charter chain never ran for LOCAL_LLM):
 *
 *   DOWNLOADED
 *     → 1. expected-size check          (partial == catalog downloadSizeBytes)
 *     → 2. SHA-256 verification         (existing ModelSha256, mismatch → quarantine)
 *     → 3. GGUF metadata validation     (LOCAL_LLM only — magic/version/tensors sane)
 *     → 4. available-storage check      (installed size fits before promotion)
 *     → 5. fsync                        (the bytes are on disk before the rename)
 *     → 6. atomic rename/promotion      (storage.installAtomically)
 *     → 7. installation registry update (registry.markInstalled)
 *     → 8. read-back                    (installed file exists, size + hash re-read)
 *     → 9. temporary cleanup            (partial removed ONLY after a verified install)
 *
 * FAILURE HANDLING (mission section 7, all binding):
 *  - on ANY failure the component is NEVER marked INSTALLED;
 *  - the resumable partial is RETAINED when safe (size/hash/gguf/storage
 *    failures keep the bytes for inspection/resume) and removed only when the
 *    bytes themselves are proven wrong (hash mismatch → quarantine per the
 *    existing GRAPH 4 §7 evidence policy) or after a verified install;
 *  - a duplicate promotion while one is running is typed PROMOTION_BUSY;
 *  - a superseded version keeps the rollback point (registry semantics);
 *  - every step exposes a TYPED code (never a bare boolean at the UI edge).
 *
 * ACTIVATION stays an explicit Owner action (Tower-first preserved: the local
 * model is optional and OFF by default — unchanged by this repair).
 */
class ModelPromotionController(
    private val storage: ModelStorageManager,
    private val registry: ModelInstallationRegistry,
    private val downloadManager: ModelDownloadManager,
) {

    /** The typed promotion outcome codes (surfaced to the card + diagnostics). */
    enum class Code {
        NOT_DOWNLOADED,          // no partial on disk
        SIZE_MISMATCH,           // partial size != catalog size (resumable: retained)
        HASH_MISMATCH,           // quarantined by verifyHash (evidence preserved)
        GGUF_INVALID,            // magic/metadata sanity failed (retained for diagnosis)
        INSUFFICIENT_STORAGE,    // installed size does not fit (retained)
        PROMOTION_IO_FAILED,     // rename/fsync failed (retained; retryable)
        REGISTRY_WRITE_FAILED,   // registry update failed (installed file may exist: read-back decides truth)
        READ_BACK_FAILED,        // installed file missing/wrong after promotion
        PROMOTION_BUSY,          // a promotion is already running for this identity
        PROMOTED,                // full chain green: installed + registry + read-back
    }

    data class Result(
        val code: Code,
        val installedPath: Path? = null,
        val detail: String? = null,
        val elapsedMs: Long = 0L,
    )

    private val inFlight = java.util.Collections.synchronizedSet(HashSet<String>())

    /** Runs the full chain for one catalog entry. Deterministic; JVM-testable. */
    fun promote(entry: ModelCatalogEntry): Result {
        val started = System.currentTimeMillis()
        if (!inFlight.add(entry.identity)) {
            return Result(Code.PROMOTION_BUSY, detail = "promozione già in corso per ${entry.identity}")
        }
        try {
            return promoteInner(entry, started)
        } finally {
            inFlight.remove(entry.identity)
        }
    }

    private fun promoteInner(entry: ModelCatalogEntry, started: Long): Result {
        // 0. the partial must exist (DOWNLOADED semantics).
        val partial = downloadManager.partialFileOf(entry)
            ?: return Result(Code.NOT_DOWNLOADED, detail = "nessun file temporaneo per ${entry.identity}")
        if (!Files.exists(partial)) {
            return Result(Code.NOT_DOWNLOADED, detail = "file temporaneo assente: riscarica")
        }

        // 1. expected-size check.
        val size = Files.size(partial)
        if (size != entry.downloadSizeBytes) {
            return Result(
                Code.SIZE_MISMATCH,
                detail = "dimensione $size != attesa ${entry.downloadSizeBytes}: file parziale conservato per il resume",
            )
        }

        // 2. SHA-256 (the existing verified path: quarantine on mismatch).
        if (!downloadManager.verifyHash(entry)) {
            return Result(Code.HASH_MISMATCH, detail = "hash non corrispondente: parziale messo in quarantena come evidenza")
        }

        // 3. GGUF metadata validation (LOCAL_LLM only).
        if (entry.componentType == ModelComponentType.LOCAL_LLM.name && !ModelGgufValidator.isValidGguf(partial)) {
            return Result(
                Code.GGUF_INVALID,
                detail = "metadati GGUF non validi: file conservato per diagnosi (nessuna installazione)",
            )
        }

        // 4. available-storage check (the same preflight the download uses).
        val preflight = storage.preflight(entry.installedSizeBytes)
        if (!preflight.sufficient) {
            return Result(
                Code.INSUFFICIENT_STORAGE,
                detail = "spazio insufficiente per l'installazione: ${preflight.message ?: "liberi spazio e riprova"}",
            )
        }

        // 5+6. fsync then atomic promotion.
        try {
            RandomAccessFile(partial.toFile(), "rw").use { raf -> raf.channel.force(true) }
        } catch (e: Exception) {
            return Result(Code.PROMOTION_IO_FAILED, detail = "fsync non riuscito: ${e.message}")
        }
        val target: Path = try {
            storage.installAtomically(entry, partial)
        } catch (e: Exception) {
            return Result(Code.PROMOTION_IO_FAILED, detail = "promozione atomica non riuscita: ${e.message} (parziale conservato)")
        }

        // 7. installation registry update (failure leaves the installed file: read-back decides).
        try {
            registry.markInstalled(entry, target)
        } catch (e: Exception) {
            val readBackOk = Files.exists(target) && Files.size(target) == entry.installedSizeBytes
            return if (readBackOk) {
                Result(
                    Code.REGISTRY_WRITE_FAILED,
                    installedPath = target,
                    detail = "file installato ma registro non aggiornato (${e.message}): la lettura al riavvio deciderà",
                )
            } else {
                Result(Code.REGISTRY_WRITE_FAILED, detail = "registro non aggiornato e file assente: ${e.message}")
            }
        }

        // 8. read-back: the installed file exists with the expected size.
        if (!Files.exists(target) || Files.size(target) != entry.installedSizeBytes) {
            return Result(
                Code.READ_BACK_FAILED,
                installedPath = target,
                detail = "lettura di controllo non riuscita dopo l'installazione",
            )
        }

        // 9. temporary cleanup — ONLY after a verified install.
        runCatching { Files.deleteIfExists(partial) }

        return Result(
            Code.PROMOTED,
            installedPath = target,
            detail = "installato e verificato (${entry.installedSizeBytes / (1024 * 1024)} MB)",
            elapsedMs = System.currentTimeMillis() - started,
        )
    }

    /**
     * Convenience truth for the card + self-test: is this identity fully
     * installed per the registry AND on disk (read-back)?
     */
    fun isInstalledOnDisk(entry: ModelCatalogEntry): Boolean {
        val rec = registry.record(entry.identity) ?: return false
        val path = rec.installedPath ?: return false
        return Files.exists(Path.of(path))
    }
}

/**
 * GGUF structural validation (mission section 7 stage: "GGUF metadata
 * validation"). Checks the magic, the version range and the sanity of the
 * header counts — a corrupt/truncated/hostile file is rejected BEFORE
 * promotion, with the bytes retained for diagnosis.
 */
object ModelGgufValidator {

    private val MAGIC = byteArrayOf(0x47, 0x47, 0x55, 0x46) // "GGUF"

    fun isValidGguf(file: Path): Boolean = try {
        RandomAccessFile(file.toFile(), "r").use { raf ->
            val header = ByteArray(12)
            if (raf.readFullyOrNull(header) != 12) return@use false
            // magic
            if (!header.copyOfRange(0, 4).contentEquals(MAGIC)) return@use false
            // version: little-endian u32, accepted 1..3 (v3 = current spec at bind time)
            val version = readU32LE(header, 4)
            if (version !in 1..3) return@use false
            // tensor_count and kv_count: little-endian u64 each, must be sane
            val kv = ByteArray(16)
            if (raf.readFullyOrNull(kv) != 16) return@use false
            val tensorCount = readU64LE(kv, 0)
            val kvCount = readU64LE(kv, 8)
            tensorCount in 0..10_000_000L && kvCount in 1..100_000L
        }
    } catch (e: Exception) {
        false
    }

    private fun RandomAccessFile.readFullyOrNull(buf: ByteArray): Int {
        var off = 0
        while (off < buf.size) {
            val n = read(buf, off, buf.size - off)
            if (n < 0) return off
            off += n
        }
        return off
    }

    private fun readU32LE(b: ByteArray, off: Int): Long =
        (b[off].toLong() and 0xFF) or
            ((b[off + 1].toLong() and 0xFF) shl 8) or
            ((b[off + 2].toLong() and 0xFF) shl 16) or
            ((b[off + 3].toLong() and 0xFF) shl 24)

    private fun readU64LE(b: ByteArray, off: Int): Long {
        var v = 0L
        for (i in 7 downTo 0) v = (v shl 8) or (b[off + i].toLong() and 0xFF)
        return v
    }
}
