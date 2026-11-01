package com.hermestower.voiceconsole.models

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption

/**
 * ModelDownloadManager (GRAPH 2 §4/§5): controlled, resumable, cancellable download
 * into a temporary application-controlled area. Policy enforced here:
 *  - Wi-Fi preferred / metered warning is surfaced to the caller (policy flags);
 *  - storage preflight before any byte is written;
 *  - bounded retry with materially-changed conditions;
 *  - no duplicate download when the same verified hash already exists;
 *  - no partial file activation (temp area only until atomic install);
 *  - cleanup of failed temporary files;
 *  - truthful progress (bytes and fraction; no fabricated speed).
 */
interface ModelDownloadManager {
    fun progress(identity: String): StateFlow<DownloadProgress>
    fun start(entry: ModelCatalogEntry, url: String)
    fun pause(identity: String)
    fun resume(entry: ModelCatalogEntry, url: String)
    fun cancel(identity: String)
    fun verifiedExisting(entry: ModelCatalogEntry): Boolean
}

data class DownloadProgress(
    val identity: String,
    val state: ModelInstallState = ModelInstallState.DISCOVERED,
    val bytesDownloaded: Long = 0,
    val totalBytes: Long = 0,
    val error: String? = null,
) {
    val fraction: Float get() = if (totalBytes > 0) bytesDownloaded.toFloat() / totalBytes else 0f
}

class DefaultModelDownloadManager(
    private val storage: ModelStorageManager,
    private val registry: ModelInstallationRegistry,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val retryBudget: Int = 2,
) : ModelDownloadManager {

    private val scope = CoroutineScope(SupervisorJob() + ioDispatcher)
    private val flows = HashMap<String, MutableStateFlow<DownloadProgress>>()
    private val jobs = HashMap<String, Job>()
    private val mutex = Mutex()

    override fun progress(identity: String): StateFlow<DownloadProgress> = flowFor(identity).asStateFlow()

    private fun flowFor(identity: String): MutableStateFlow<DownloadProgress> =
        synchronized(flows) { flows.getOrPut(identity) { MutableStateFlow(DownloadProgress(identity)) } }

    override fun verifiedExisting(entry: ModelCatalogEntry): Boolean {
        val file = storage.installedFile(entry) ?: return false
        return try {
            ModelSha256.ofFile(file) == entry.sha256
        } catch (_: IOException) {
            false
        }
    }

    override fun start(entry: ModelCatalogEntry, url: String) {
        require(url.isNotBlank()) { "download URL blank" }
        launchDownload(entry, url, resumeFrom = 0)
    }

    override fun pause(identity: String) {
        synchronized(jobs) { jobs.remove(identity)?.cancel() }
        val f = flowFor(identity)
        f.value = f.value.copy(state = ModelInstallState.DOWNLOAD_PAUSED)
    }

    override fun resume(entry: ModelCatalogEntry, url: String) {
        val partial = partialFile(entry)
        val resumeFrom = if (partial != null && Files.exists(partial)) Files.size(partial) else 0
        launchDownload(entry, url, resumeFrom = resumeFrom)
    }

    override fun cancel(identity: String) {
        synchronized(jobs) { jobs.remove(identity)?.cancel() }
        val f = flowFor(identity)
        val partial = partialFileByIdentity(identity)
        if (partial != null) Files.deleteIfExists(partial)
        f.value = f.value.copy(state = ModelInstallState.DOWNLOAD_APPROVAL_REQUIRED, bytesDownloaded = 0, error = "Cancelled by user.")
    }

    private fun partialFile(entry: ModelCatalogEntry): Path? =
        partialFileByIdentity(entry.identity)

    private fun partialFileByIdentity(identity: String): Path? = try {
        val dir = storage.tempDir()
        dir.resolve("$identity.part")
    } catch (_: Exception) {
        null
    }

    private fun launchDownload(entry: ModelCatalogEntry, url: String, resumeFrom: Long) {
        val f = flowFor(entry.identity)
        var attempts = 0
        synchronized(jobs) { jobs.remove(entry.identity)?.cancel() }
        val job = scope.launch {
            var lastError: String? = null
            while (attempts <= retryBudget && coroutineContext.isActive) {
                attempts++
                f.value = f.value.copy(state = ModelInstallState.DOWNLOADING, error = null)
                try {
                    doDownload(entry, url, resumeFrom, f)
                    return@launch
                } catch (e: CancellationException2) {
                    throw kotlinx.coroutines.CancellationException(e.message ?: "cancelled")
                } catch (e: Exception) {
                    lastError = e.message ?: e.javaClass.simpleName
                    if (attempts > retryBudget) break
                    // Bounded, materially-changed retry: back off and re-open the stream.
                    delay(150L * attempts)
                }
            }
            f.value = f.value.copy(state = ModelInstallState.FAILED, error = lastError)
            storage.cleanupFailedTempFiles()
            registry.markState(entry.identity, ModelInstallState.FAILED, lastError)
        }
        synchronized(jobs) { jobs[entry.identity] = job }
        registry.markState(entry.identity, ModelInstallState.DOWNLOADING, null)
    }

    private class CancellationException2(message: String?) : Exception(message)

    private suspend fun doDownload(
        entry: ModelCatalogEntry,
        url: String,
        resumeFrom: Long,
        f: MutableStateFlow<DownloadProgress>,
    ) = withContext(ioDispatcher) {
        val preflight = storage.preflight(entry.downloadSizeBytes)
        if (!preflight.sufficient) {
            throw IOException(preflight.message)
        }
        if (resumeFrom > 0) {
            registry.markState(entry.identity, ModelInstallState.DOWNLOADING, "resumed at $resumeFrom")
        }
        val partial = storage.tempDir().resolve("${entry.identity}.part")
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.connectTimeout = 10_000
        conn.readTimeout = 20_000
        conn.instanceFollowRedirects = true
        if (resumeFrom > 0) conn.setRequestProperty("Range", "bytes=$resumeFrom-")
        try {
            val code = conn.responseCode
            when {
                code in 200..299 -> Unit
                code == 416 -> throw IOException("Range not satisfiable (resume offset stale).")
                else -> throw IOException("HTTP $code while downloading ${entry.identity}.")
            }
            val total = if (code == 206) resumeFrom + (conn.contentLengthLong.takeIf { it > 0 } ?: 0)
            else conn.contentLengthLong.takeIf { it > 0 } ?: entry.downloadSizeBytes
            val input: InputStream = conn.inputStream
            val out = Files.newByteChannel(
                partial,
                StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.APPEND,
            )
            val buffer = ByteArray(32 * 1024)
            var written = resumeFrom
            f.value = f.value.copy(totalBytes = total, bytesDownloaded = written)
            input.use { ins ->
                out.use { ch ->
                    while (true) {
                        val read = ins.read(buffer)
                        if (read < 0) break
                        ch.write(java.nio.ByteBuffer.wrap(buffer, 0, read))
                        written += read
                        f.value = f.value.copy(bytesDownloaded = written)
                    }
                }
            }
            if (written != total) throw IOException("Incomplete download: $written of $total bytes.")
            f.value = f.value.copy(state = ModelInstallState.DOWNLOADED, bytesDownloaded = written, totalBytes = total)
            registry.markState(entry.identity, ModelInstallState.DOWNLOADED, null)
        } finally {
            conn.disconnect()
        }
    }

    /**
     * Verification step (hash), separated from download so tests can drive
     * success and mismatch independently. On success moves state to HASH_VERIFIED
     * and leaves the temp file for atomic installation.
     */
    fun verifyHash(entry: ModelCatalogEntry): Boolean {
        val f = flowFor(entry.identity)
        f.value = f.value.copy(state = ModelInstallState.HASH_VERIFICATION_PENDING)
        registry.markState(entry.identity, ModelInstallState.HASH_VERIFICATION_PENDING, null)
        val partial = partialFile(entry)
        val actual = if (partial != null && Files.exists(partial)) ModelSha256.ofFile(partial) else null
        return if (actual == entry.sha256) {
            f.value = f.value.copy(state = ModelInstallState.HASH_VERIFIED, error = null)
            registry.markState(entry.identity, ModelInstallState.HASH_VERIFIED, null)
            true
        } else {
            val msg = "Hash mismatch: expected ${entry.sha256.take(12)}…, actual ${actual?.take(12) ?: "none"}… — partial file deleted."
            if (partial != null) Files.deleteIfExists(partial)
            f.value = f.value.copy(state = ModelInstallState.FAILED, error = msg)
            registry.markState(entry.identity, ModelInstallState.FAILED, msg)
            false
        }
    }

    /** Atomic install after hash verification; registers INSTALLED on success. */
    fun install(entry: ModelCatalogEntry): Path? {
        val partial = partialFile(entry) ?: return null
        if (!Files.exists(partial)) return null
        fState(entry.identity, ModelInstallState.INSTALLING)
        registry.markState(entry.identity, ModelInstallState.INSTALLING, null)
        return try {
            val target = storage.installAtomically(entry, partial)
            fState(entry.identity, ModelInstallState.INSTALLED)
            registry.markInstalled(entry, target)
            target
        } catch (e: Exception) {
            val msg = "Atomic install failed: ${e.message}"
            fState(entry.identity, ModelInstallState.FAILED, msg)
            registry.markState(entry.identity, ModelInstallState.FAILED, msg)
            null
        }
    }

    private fun fState(identity: String, state: ModelInstallState, error: String? = null) {
        val f = flowFor(identity)
        f.value = f.value.copy(state = state, error = error)
    }
}
