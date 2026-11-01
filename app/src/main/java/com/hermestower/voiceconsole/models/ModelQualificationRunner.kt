package com.hermestower.voiceconsole.models

import java.io.IOException
import java.nio.file.Files
import java.time.Instant

/**
 * ModelQualificationRunner (GRAPH 2 §4): deterministic, bounded qualification of an
 * installed component. V0.2 checks are honest and local: file presence, hash,
 * size sanity, manifest completeness. Real acoustic/LLM qualification is a
 * physical-device graph and stays QUALIFICATION_PENDING_DEVICE.
 */
interface ModelQualificationRunner {
    fun qualify(entry: ModelCatalogEntry): QualificationResult
}

data class QualificationResult(
    val identity: String,
    val qualified: Boolean,
    val checks: List<QualificationCheck>,
    val at: Instant,
    val pendingDevice: Boolean = false,
)

data class QualificationCheck(
    val name: String,
    val passed: Boolean,
    val detail: String,
)

class DefaultModelQualificationRunner(
    private val storage: ModelStorageManager,
) : ModelQualificationRunner {

    override fun qualify(entry: ModelCatalogEntry): QualificationResult {
        val checks = mutableListOf<QualificationCheck>()
        val file = storage.installedFile(entry)
        checks += QualificationCheck(
            "installed-file-present",
            file != null,
            if (file != null) "Found at $file" else "No installed file for ${entry.identity}.",
        )
        if (file != null) {
            val size = try { Files.size(file) } catch (_: IOException) { -1L }
            checks += QualificationCheck(
                "size-sane",
                size == entry.installedSizeBytes || size == entry.downloadSizeBytes,
                "Installed size $size vs manifest ${entry.installedSizeBytes}/${entry.downloadSizeBytes}.",
            )
            val hashOk = try {
                ModelSha256.ofFile(file) == entry.sha256
            } catch (_: IOException) {
                false
            }
            checks += QualificationCheck("hash-match", hashOk, "SHA-256 re-computed against manifest.")
        }
        checks += QualificationCheck(
            "manifest-complete",
            entry.sha256.length == 64 && entry.manifestVersion.isNotBlank() && entry.license.isNotBlank(),
            "Manifest fields complete for ${entry.identity}.",
        )
        val allPassed = checks.all { it.passed }
        val result = QualificationResult(
            identity = entry.identity,
            qualified = allPassed,
            checks = checks,
            at = Instant.now(),
        )
        return result
    }
}
