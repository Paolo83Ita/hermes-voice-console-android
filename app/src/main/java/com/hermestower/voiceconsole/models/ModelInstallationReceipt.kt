package com.hermestower.voiceconsole.models

import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.time.Instant

/**
 * ModelInstallationReceipt (GRAPH 4 §7): a per-component, secret-free record of one
 * complete installation attempt. Written to the receipts root after every pack or
 * single-component install; the receipt is the honest trace the Owner and the
 * Self-Test Center can read back.
 *
 * SECRET POLICY (INV-G4-03): a receipt contains artifact facts (identity, version,
 * public SHA-256, sizes), step outcomes and timings only. No URLs with credentials,
 * no tokens, no pairing secrets, no private endpoints. The source host is recorded
 * as a plain host name for provenance; the download pipeline never embeds
 * credentials in URLs (INV-G4-03 holds by construction).
 */
@Serializable
data class ModelInstallationReceipt(
    val receiptId: String,
    val identity: String,
    val displayName: String,
    val version: String,
    val componentType: String,
    val sha256: String,
    val downloadSizeBytes: Long,
    val installedSizeBytes: Long,
    val sourceHost: String,
    val startedAt: String,
    val finishedAt: String,
    val outcome: String,
    val resumedFromBytes: Long = 0,
    val retryCount: Int = 0,
    val steps: List<ReceiptStep>,
) {
    companion object {
        const val OUTCOME_INSTALLED = "INSTALLED"
        const val OUTCOME_FAILED = "FAILED"
    }
}

@Serializable
data class ReceiptStep(
    val name: String,
    val outcome: String,
    val detail: String = "",
)

/** Pack-level receipt: the ordered outcomes of one bulk voice-pack install. */
@Serializable
data class PackInstallationReceipt(
    val receiptId: String,
    val profile: String,
    val startedAt: String,
    val finishedAt: String,
    val outcome: String,
    val components: List<PackComponentOutcome>,
    val interruptedByUser: Boolean = false,
)

@Serializable
data class PackComponentOutcome(
    val identity: String,
    val version: String,
    val success: Boolean,
    val receiptId: String?,
    val reason: String,
)

object ReceiptWriter {
    private val json = Json { prettyPrint = true; encodeDefaults = true }

    /** Atomic write into the receipts root; returns the written path. */
    fun writeReceipts(dir: Path, receipt: ModelInstallationReceipt): Path {
        Files.createDirectories(dir)
        val target = dir.resolve("receipt-${receipt.receiptId}.json")
        val tmp = dir.resolve("receipt-${receipt.receiptId}.json.tmp")
        Files.write(tmp, json.encodeToString(ModelInstallationReceipt.serializer(), receipt).toByteArray(Charsets.UTF_8))
        Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
        return target
    }

    fun writePack(dir: Path, receipt: PackInstallationReceipt): Path {
        Files.createDirectories(dir)
        val target = dir.resolve("pack-${receipt.receiptId}.json")
        val tmp = dir.resolve("pack-${receipt.receiptId}.json.tmp")
        Files.write(tmp, json.encodeToString(PackInstallationReceipt.serializer(), receipt).toByteArray(Charsets.UTF_8))
        Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
        return target
    }

    /** List receipt file names (read-back), newest last. */
    fun listReceipts(dir: Path): List<String> {
        if (!Files.isDirectory(dir)) return emptyList()
        return dir.toFile().listFiles()?.filter { it.isFile }?.map { it.name }?.sorted() ?: emptyList()
    }
}

/** Uniform step-name vocabulary so receipts are machine-comparable. */
object ReceiptSteps {
    const val PREFLIGHT = "preflight"
    const val DOWNLOAD = "download"
    const val HASH = "hash-verification"
    const val INSTALL = "atomic-install"
    const val REGISTRY_READBACK = "registry-read-back"
    const val QUALIFICATION = "qualification"
    const val ACTIVATION = "activation"

    val ALL = listOf(PREFLIGHT, DOWNLOAD, HASH, INSTALL, REGISTRY_READBACK, QUALIFICATION, ACTIVATION)
}
