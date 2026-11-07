package com.hermestower.voiceconsole.models

import java.time.Instant
import java.util.Properties

/**
 * GRAPH 4.12 §8 — MODEL RUNTIME RECEIPT (secret-clean).
 *
 * For every downloaded model or voice, the runtime must PROVE it consumed the
 * component: verified bytes, a real load, a real functional probe, a real
 * unload. This receipt is the typed evidence; it never contains user text,
 * audio bytes, or secret paths (only the STORAGE PATH CLASS).
 */
data class ModelRuntimeReceipt(
    val componentIdentity: String,
    val componentVersion: String,
    val expectedSha256: String,
    val actualSha256: String?,
    val storagePathClass: String,
    val runtimeIdentity: String,
    val runtimeVersion: String,
    val loadStartMillis: Long,
    val loadResult: LoadResult,
    val memoryBeforeBytes: Long,
    val memoryAfterBytes: Long,
    val functionalInputClass: String?,
    val functionalOutputSummary: String?,
    val latencyMillis: Long?,
    val unloadResult: UnloadResult,
    val finalState: ModelCapabilityState,
    val fallback: String?,
) {
    enum class LoadResult { LOADED, FAILED, SKIPPED }
    enum class UnloadResult { UNLOADED, RETAINED_ACTIVE, NOT_LOADED }

    fun secretCleanLine(): String =
        "$componentIdentity v$componentVersion runtime=$runtimeIdentity/$runtimeVersion " +
            "load=$loadResult memΔ=${memoryAfterBytes - memoryBeforeBytes}B " +
            "probe=${functionalInputClass ?: "-"}→${functionalOutputSummary ?: "-"} " +
            "latency=${latencyMillis ?: -1}ms unload=$unloadResult state=$finalState" +
            (fallback?.let { " fallback=$it" } ?: "")
}

/**
 * Recorder + renderer for runtime receipts (Graph 4.12 §8). The report is
 * persisted secret-clean (Properties) and rendered as the human-readable
 * MODEL_RUNTIME_TRUTH_REPORT.md body.
 */
class ModelRuntimeReceiptRecorder {

    private val receipts = mutableListOf<ModelRuntimeReceipt>()
    private val lock = Any()

    fun record(receipt: ModelRuntimeReceipt) {
        synchronized(lock) { receipts += receipt }
    }

    fun all(): List<ModelRuntimeReceipt> = synchronized(lock) { receipts.toList() }

    fun clear() = synchronized(lock) { receipts.clear() }

    /** Renders the MODEL_RUNTIME_TRUTH_REPORT.md body (§8). */
    fun renderMarkdownReport(title: String, generatedAt: Instant): String {
        val b = StringBuilder()
        b.append("# $title\n\nGenerated: $generatedAt\n\n")
        b.append("Runtime truth = bytes verified → load → functional probe → unload, per component.\n\n")
        if (receipts.isEmpty()) {
            b.append("_No runtime receipts recorded in this session._\n")
            return b.toString()
        }
        b.append("| Component | Version | Load | Probe | Latency | Unload | Final state | Fallback |\n")
        b.append("| --- | --- | --- | --- | --- | --- | --- | --- |\n")
        for (r in receipts) {
            b.append(
                "| ${r.componentIdentity} | ${r.componentVersion} | ${r.loadResult} | " +
                    "${r.functionalOutputSummary ?: "—"} | ${r.latencyMillis ?: "—"}ms | " +
                    "${r.unloadResult} | ${r.finalState} | ${r.fallback ?: "—"} |\n",
            )
        }
        b.append("\n## Hash binding\n\n")
        for (r in receipts) {
            b.append(
                "- ${r.componentIdentity}: expected `${r.expectedSha256.take(16)}…` " +
                    "actual ${r.actualSha256?.let { "`${it.take(16)}…`" } ?: "not computed"} " +
                    "(${r.storagePathClass})\n",
            )
        }
        b.append("\n## Secret-clean statement\n\n")
        b.append("No user text, no audio bytes, no secret paths. Only path classes, ")
        b.append("hashes, identities, timings and states are recorded.\n")
        return b.toString()
    }

    /** Persists the receipts (Properties = parser-free round trip). */
    fun persist(file: java.io.File) {
        val props = Properties()
        synchronized(lock) {
            props.setProperty("receipt.count", receipts.size.toString())
            receipts.forEachIndexed { i, r ->
                props.setProperty("receipt.$i", r.secretCleanLine())
            }
        }
        file.parentFile?.mkdirs()
        file.outputStream().use { props.store(it, "MODEL_RUNTIME_TRUTH (secret-clean)") }
    }
}
