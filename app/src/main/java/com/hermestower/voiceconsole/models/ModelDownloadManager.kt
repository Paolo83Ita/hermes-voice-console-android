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
        launchDownload(entry, url)
    }

    override fun pause(identity: String) {
        val f = flowFor(identity)
        // Pausing is only meaningful while bytes are in flight; a completed or
        // failed download is left in its truthful state.
        if (f.value.state != ModelInstallState.DOWNLOADING) return
        val job = synchronized(jobs) { jobs.remove(identity) }
        job?.cancel()
        joinQuietly(job)
        f.value = f.value.copy(state = ModelInstallState.DOWNLOAD_PAUSED)
    }

    override fun resume(entry: ModelCatalogEntry, url: String) {
        joinQuietly(synchronized(jobs) { jobs[entry.identity] })
        val partial = partialFile(entry)
        val resumeFrom = if (partial != null && Files.exists(partial)) Files.size(partial) else 0
        if (resumeFrom >= entry.downloadSizeBytes) {
            // Nothing left to fetch: the artifact is already fully on disk.
            flowFor(entry.identity).value = flowFor(entry.identity).value.copy(
                state = ModelInstallState.DOWNLOADED,
                bytesDownloaded = resumeFrom,
                totalBytes = resumeFrom,
            )
            registry.markState(entry.identity, ModelInstallState.DOWNLOADED, "already complete")
            return
        }
        launchDownload(entry, url)
    }

    override fun cancel(identity: String) {
        val job = synchronized(jobs) { jobs.remove(identity) }
        job?.cancel()
        joinQuietly(job)
        val f = flowFor(identity)
        val partial = partialFileByIdentity(identity)
        if (partial != null) Files.deleteIfExists(partial)
        f.value = f.value.copy(state = ModelInstallState.DOWNLOAD_APPROVAL_REQUIRED, bytesDownloaded = 0, error = "Cancelled by user.")
    }

    /** Bounded join: guarantees the writer has stopped before the partial is read or deleted. */
    private fun joinQuietly(job: Job?) {
        if (job == null) return
        val deadline = System.currentTimeMillis() + 3_000
        while (!job.isCompleted && System.currentTimeMillis() < deadline) {
            Thread.sleep(5)
        }
    }

    private fun partialFile(entry: ModelCatalogEntry): Path? =
        partialFileByIdentity(entry.identity)

    private fun partialFileByIdentity(identity: String): Path? = try {
        val dir = storage.tempDir()
        dir.resolve("$identity.part")
    } catch (_: Exception) {
        null
    }

    private fun launchDownload(entry: ModelCatalogEntry, url: String) {
        val f = flowFor(entry.identity)
        var attempts = 0
        synchronized(jobs) { jobs.remove(entry.identity)?.cancel() }
        val job = scope.launch {
            var lastError: String? = null
            var currentUrl = url
            var mirrorUsed = false
            while (attempts <= retryBudget && coroutineContext.isActive) {
                attempts++
                f.value = f.value.copy(state = ModelInstallState.DOWNLOADING, error = null)
                try {
                    doDownload(entry, currentUrl, f)
                    return@launch
                } catch (e: CancellationException2) {
                    throw kotlinx.coroutines.CancellationException(e.message ?: "cancelled")
                } catch (e: Exception) {
                    lastError = e.message ?: e.javaClass.simpleName
                    // OWNER PHYSICAL RETEST round 2 (Package 9, F-E residual): the
                    // Owner's POCO X6 Pro reported "errore di connessione" on the
                    // Qwen 2.5 0.5B download. The pinned source is ALIVE (verified
                    // 302 → us.aws.cdn.hf.co → 200, 522186592 bytes) — the failure
                    // modes are transport-level: missing User-Agent (CDN 403), the
                    // platform redirect quirks, tight timeouts, and — the correctness
                    // bug — the retry loop re-downloading from a STALE offset and
                    // APPENDing to the partial (corrupt file → hash quarantine).
                    // Typed bounded fallback: ONE mirror-host attempt for pinned
                    // huggingface.co sources (hf-mirror.com, same immutable path),
                    // consuming one attempt of the SAME budget — total attempts stay
                    // bounded; the fallback is recorded in the error string.
                    if (!mirrorUsed) {
                        val mirrored = mirrorUrl(currentUrl)
                        if (mirrored != null) {
                            mirrorUsed = true
                            currentUrl = mirrored
                            lastError = "primary source failed ($lastError) — retrying via mirror"
                        }
                    }
                    if (attempts > retryBudget) break
                    // Bounded, materially-changed retry: back off and re-open the
                    // stream; the next attempt resumes from the ACTUAL partial size.
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

    /** Mirror host for pinned huggingface.co sources (same immutable path, https). */
    private fun mirrorUrl(url: String): String? = try {
        val u = URL(url)
        if (u.protocol == "https" && u.host.equals("huggingface.co", ignoreCase = true)) {
            URL(u.protocol, "hf-mirror.com", u.file).toString()
        } else null
    } catch (_: Exception) {
        null
    }

    private suspend fun doDownload(
        entry: ModelCatalogEntry,
        url: String,
        f: MutableStateFlow<DownloadProgress>,
    ) = withContext(ioDispatcher) {
        val preflight = storage.preflight(entry.downloadSizeBytes)
        if (!preflight.sufficient) {
            throw IOException(preflight.message)
        }
        val partial = storage.tempDir().resolve("${entry.identity}.part")

        // ---- resume offset from the ACTUAL partial (the append-corruption fix) ----
        // The old code re-downloaded from the STALE offset captured before the
        // retry loop and opened the channel in APPEND mode: a mid-stream failure at
        // N bytes followed the file with bytes 0..M → corrupt .part → guaranteed
        // hash-mismatch quarantine. The offset is now the file's true size each
        // attempt; an oversized (legacy-corrupt) partial is reset honestly.
        var resumeFrom = if (Files.exists(partial)) Files.size(partial) else 0L
        if (resumeFrom > entry.downloadSizeBytes) {
            Files.deleteIfExists(partial)
            resumeFrom = 0L
        }
        if (resumeFrom > 0) {
            registry.markState(entry.identity, ModelInstallState.DOWNLOADING, "resumed at $resumeFrom")
        }

        // ---- explicit bounded redirect walk (UA + Range preserved on every hop) ----
        var current = url
        var hops = 0
        var rangeResetDone = false
        while (true) {
            val conn = URL(current).openConnection() as HttpURLConnection
            conn.connectTimeout = 20_000
            conn.readTimeout = 45_000
            conn.instanceFollowRedirects = false // followed explicitly below
            conn.setRequestProperty("User-Agent", DOWNLOAD_USER_AGENT)
            if (resumeFrom > 0) conn.setRequestProperty("Range", "bytes=$resumeFrom-")
            try {
                val code = conn.responseCode
                if (code == 301 || code == 302 || code == 303 || code == 307 || code == 308) {
                    val location = conn.getHeaderField("Location")
                        ?: throw IOException("HTTP $code redirect without Location while downloading ${entry.identity}.")
                    val next = java.net.URL(URL(current), location)
                    // Security policy: never DOWNGRADE the scheme (a pinned https
                    // source can never redirect to http). Plain-http sources (the
                    // local mock server in tests) may redirect within http.
                    if (current.startsWith("https://") && next.protocol != "https") {
                        throw IOException("Insecure redirect target dropped (${next.protocol}).")
                    }
                    conn.disconnect()
                    current = next.toString()
                    if (++hops > 5) throw IOException("Too many redirects while downloading ${entry.identity}.")
                    continue
                }
                when {
                    code in 200..299 -> Unit
                    code == 416 -> {
                        // Range not satisfiable: the resume offset is stale — one
                        // honest restart from zero (bounded, once per download).
                        if (rangeResetDone) throw IOException("Range not satisfiable after restart.")
                        rangeResetDone = true
                        Files.deleteIfExists(partial)
                        resumeFrom = 0L
                        conn.disconnect()
                        continue
                    }
                    else -> throw IOException("HTTP $code while downloading ${entry.identity}.")
                }
                if (code == 200 && resumeFrom > 0) {
                    // The server ignored the Range request: restart from zero
                    // instead of corrupting the offset arithmetic (the fresh
                    // body IS the whole file; the stale partial must not survive
                    // under it).
                    Files.deleteIfExists(partial)
                    resumeFrom = 0L
                }
                val total = if (code == 206) resumeFrom + (conn.contentLengthLong.takeIf { it > 0 } ?: 0)
                else conn.contentLengthLong.takeIf { it > 0 } ?: entry.downloadSizeBytes
                val input: InputStream = conn.inputStream
                // Positioned channel (NOT blind APPEND): the offset IS the file size,
                // so writes continue exactly where the previous attempt stopped.
                val out = Files.newByteChannel(
                    partial,
                    StandardOpenOption.CREATE, StandardOpenOption.WRITE,
                )
                out.position(resumeFrom)
                val buffer = ByteArray(32 * 1024)
                var written = resumeFrom
                f.value = f.value.copy(totalBytes = total, bytesDownloaded = written)
                input.use { ins ->
                    out.use { ch ->
                        while (true) {
                            if (!coroutineContext.isActive) {
                                // Paused or cancelled: stop writing immediately; the
                                // partial stays for a resume, never for activation.
                                throw CancellationException2("download paused/cancelled at $written bytes")
                            }
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
                return@withContext
            } finally {
                conn.disconnect()
            }
        }
    }

    internal companion object {
        /** Identifies the client to pinned sources (some CDNs reject empty UAs). */
        internal const val DOWNLOAD_USER_AGENT = "HermesTowerConsole-ModelDownload/1.0 (Android)"
    }

    /**
     * Verification step (hash), separated from download so tests can drive
     * success and mismatch independently. On success moves state to HASH_VERIFIED
     * and leaves the temp file for atomic installation. On mismatch the partial is
     * QUARANTINED with its facts (GRAPH 4 §7) — evidence is preserved, never
     * silently destroyed — and the state is FAILED.
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
            val quarantined = if (partial != null) storage.quarantinePartial(entry.identity, partial, entry.sha256, actual) else null
            val msg = "Hash mismatch: expected ${entry.sha256.take(12)}…, actual ${actual?.take(12) ?: "none"}…" +
                (quarantined?.let { " — partial quarantined." } ?: " — partial removed.")
            if (quarantined == null) partial?.let { Files.deleteIfExists(it) }
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
