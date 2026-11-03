package com.hermestower.voiceconsole.models

import kotlinx.serialization.Serializable
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.time.Instant

/**
 * GRAPH 4 CP-G4-009 (Owner §6 "real model delivery"): delivery of the four
 * physically verified ONNX artifacts bundled inside the APK
 * (`assets/models/bundled/`) into the §7 storage roots through the SAME
 * verified pipeline the network downloads use — hash gate, atomic install,
 * registry read-back, qualification, activation, receipt.
 *
 * Hard rules (each one typed and tested):
 *  - Every byte is SHA-256 verified against BOTH the bundled manifest AND the
 *    shipped voice_pack_catalog.json before anything is written. A conflict
 *    between the two shipped manifests is a typed refusal — no silent trust.
 *  - No network, no invented endpoints: provenance in every receipt is the
 *    honest literal "app-bundle".
 *  - A hash mismatch refuses the install and writes a quarantine facts sidecar
 *    (evidence preserved, never silently discarded — §7 semantics).
 *  - Sequential with a truthful stop at the first failure (same contract as
 *    the recommended voice pack): earlier successes stay installed.
 *  - Activation goes through the ordinary ModelActivationController — the
 *    installer never bypasses the qualification gate.
 *  - This DELIVERS artifacts; it does not enable behaviors. The wake word
 *    stays OFF until the FA/FR harness passes on the Xiaomi (Owner §8/§9) and
 *    the denoiser stays bypassed until device qualification.
 */
class BundledModelInstaller(
    private val assetSource: BundledAssetSource,
    private val catalog: VoiceModelCatalog,
    private val storage: ModelStorageManager,
    private val registry: ModelInstallationRegistry,
    private val activation: ModelActivationController,
) {

    @Serializable
    data class BundledArtifact(
        val asset: String,
        val identity: String,
        val version: String,
        val componentType: String,
        val sha256: String,
        val sizeBytes: Long,
        val license: String,
        val licenseUrl: String? = null,
        val upstreamRepo: String? = null,
        val revisionBinding: String? = null,
        val provenanceNote: String? = null,
    )

    @Serializable
    data class BundledManifest(
        val manifestVersion: String,
        val generatedFor: String,
        val verificationEvidence: String,
        val deliveryContract: String? = null,
        val artifacts: List<BundledArtifact>,
    )

    /** Seam: Android opens APK assets; JVM tests open a real directory. */
    interface BundledAssetSource {
        fun open(assetName: String): InputStream
        fun exists(assetName: String): Boolean
    }

    enum class RefusalKind {
        ASSET_MISSING, CATALOG_CONFLICT, HASH_MISMATCH, SIZE_MISMATCH, ACTIVATION_REFUSED, IO_FAILURE
    }

    sealed class BundledInstallResult {
        data class Installed(
            val artifact: BundledArtifact,
            val installedPath: String,
            val receiptId: String,
            val qualificationSummary: String,
        ) : BundledInstallResult()

        data class AlreadyInstalled(val artifact: BundledArtifact, val detail: String) : BundledInstallResult()

        data class Refused(val artifact: BundledArtifact?, val kind: RefusalKind, val detail: String) : BundledInstallResult()
    }

    data class BundledInstallReport(
        val results: List<BundledInstallResult>,
        val stoppedAt: String?,
        val allDelivered: Boolean,
    ) {
        val installedCount: Int get() = results.count { it is BundledInstallResult.Installed || it is BundledInstallResult.AlreadyInstalled }
    }

    fun loadManifest(): BundledManifest =
        DefaultVoiceModelCatalog.json.decodeFromString(
            BundledManifest.serializer(),
            assetSource.open("bundled_manifest.json").bufferedReader().use { it.readText() },
        )

    /** Sequential delivery with truthful stop at the first failure. */
    fun deliverAll(): BundledInstallReport {
        val manifest = loadManifest()
        val results = mutableListOf<BundledInstallResult>()
        var stoppedAt: String? = null
        for (artifact in manifest.artifacts) {
            val result = deliver(artifact)
            results += result
            if (result is BundledInstallResult.Refused) {
                stoppedAt = artifact.asset
                break
            }
        }
        return BundledInstallReport(
            results = results,
            stoppedAt = stoppedAt,
            allDelivered = stoppedAt == null,
        )
    }

    fun deliver(artifact: BundledArtifact): BundledInstallResult {
        val startedAt = Instant.now().toString()
        // ---- 1. CATALOG CROSSCHECK: the two shipped manifests must agree ----
        val catalogEntry = catalog.find(artifact.identity)
            ?: return BundledInstallResult.Refused(artifact, RefusalKind.CATALOG_CONFLICT,
                "Identity ${artifact.identity} is not in the shipped voice_pack_catalog.json — bundled delivery refused.")
        if (catalogEntry.sha256 != artifact.sha256) {
            return BundledInstallResult.Refused(artifact, RefusalKind.CATALOG_CONFLICT,
                "Manifest/catalog hash conflict for ${artifact.identity}: bundled=${artifact.sha256.take(12)}… catalog=${catalogEntry.sha256.take(12)}… — refusing both readings.")
        }
        if (catalogEntry.version != artifact.version) {
            return BundledInstallResult.Refused(artifact, RefusalKind.CATALOG_CONFLICT,
                "Manifest/catalog version conflict for ${artifact.identity}: bundled=${artifact.version} catalog=${catalogEntry.version}.")
        }
        if (artifact.sizeBytes != catalogEntry.downloadSizeBytes) {
            return BundledInstallResult.Refused(artifact, RefusalKind.SIZE_MISMATCH,
                "Manifest/catalog size conflict for ${artifact.identity}: bundled=${artifact.sizeBytes} catalog=${catalogEntry.downloadSizeBytes}.")
        }
        // ---- 2. Idempotence: an existing install is re-verified, not blindly repeated ----
        val existing = storage.installedFile(catalogEntry)
        if (existing != null) {
            val existingHash = runCatching { ModelSha256.ofFile(existing) }.getOrNull()
            if (existingHash == catalogEntry.sha256) {
                // Keep the registry state coherent (an interrupted earlier run may
                // have installed bytes without completing activation).
                val record = registry.record(artifact.identity)
                if (record == null || record.sha256 != catalogEntry.sha256) {
                    registry.markInstalled(catalogEntry, existing)
                }
                if (registry.record(artifact.identity)?.state != ModelInstallState.ACTIVE.name) {
                    val activation = activation.activate(catalogEntry)
                    if (!activation.success) {
                        return BundledInstallResult.Refused(artifact, RefusalKind.ACTIVATION_REFUSED,
                            "Bytes verified but activation refused: ${activation.reason}")
                    }
                }
                return BundledInstallResult.AlreadyInstalled(artifact,
                    "Verified bytes already installed at $existing (hash re-checked, activation coherent).")
            }
            // Hash-invalid local copy: fall through and re-deliver through the full
            // pipeline (installAtomically preserves the previous copy into §7).
        }
        // ---- 3. ASSET PRESENCE + HASH GATE (streamed, bounded memory) ----
        if (!assetSource.exists(artifact.asset)) {
            return BundledInstallResult.Refused(artifact, RefusalKind.ASSET_MISSING,
                "Bundled asset ${artifact.asset} is missing from the APK — delivery refused (no silent placeholder).")
        }
        val actualHash = try {
            assetSource.open(artifact.asset).use { ModelSha256.ofStream(it) }
        } catch (t: Throwable) {
            return BundledInstallResult.Refused(artifact, RefusalKind.IO_FAILURE,
                "Failed reading bundled asset ${artifact.asset}: ${t.javaClass.simpleName}.")
        }
        if (actualHash != artifact.sha256) {
            writeQuarantineFacts(artifact, actualHash)
            return BundledInstallResult.Refused(artifact, RefusalKind.HASH_MISMATCH,
                "Bundled asset ${artifact.asset} hash mismatch: expected ${artifact.sha256.take(12)}…, read ${actualHash.take(12)}… — quarantine facts written, nothing installed.")
        }
        // ---- 4. TEMP WRITE → ATOMIC INSTALL (§7 previous-version preservation) ----
        val temp = try {
            val t = Files.createTempFile(storage.tempDir(), "bundled-", ".part")
            assetSource.open(artifact.asset).use { input ->
                Files.copy(input, t, StandardCopyOption.REPLACE_EXISTING)
            }
            t
        } catch (t: Throwable) {
            return BundledInstallResult.Refused(artifact, RefusalKind.IO_FAILURE,
                "Failed staging bundled asset ${artifact.asset}: ${t.javaClass.simpleName}.")
        }
        val installed = try {
            storage.installAtomically(catalogEntry, temp)
        } catch (t: Throwable) {
            runCatching { Files.deleteIfExists(temp) }
            return BundledInstallResult.Refused(artifact, RefusalKind.IO_FAILURE,
                "Atomic install failed for ${artifact.identity}: ${t.javaClass.simpleName}.")
        }
        // ---- 5. REGISTRY + READ-BACK ----
        registry.markInstalled(catalogEntry, installed)
        val record = registry.record(artifact.identity)
        if (record == null || record.sha256 != catalogEntry.sha256) {
            return BundledInstallResult.Refused(artifact, RefusalKind.IO_FAILURE,
                "Registry read-back failed for ${artifact.identity} — bytes installed but registration not verifiable.")
        }
        // ---- 6. QUALIFICATION + ACTIVATION (never bypassed) ----
        val activationResult = activation.activate(catalogEntry)
        if (!activationResult.success) {
            return BundledInstallResult.Refused(artifact, RefusalKind.ACTIVATION_REFUSED,
                "Installed but activation refused: ${activationResult.reason}")
        }
        // ---- 7. RECEIPT (honest provenance: app-bundle, no network) ----
        val receiptId = "bundled-${artifact.identity}-${startedAt.substring(0, 19).replace(":", "")}"
        runCatching {
            ReceiptWriter.writeReceipts(storage.receiptsDir(), ModelInstallationReceipt(
                receiptId = receiptId,
                identity = artifact.identity,
                displayName = catalogEntry.displayName,
                version = artifact.version,
                componentType = artifact.componentType,
                sha256 = artifact.sha256,
                downloadSizeBytes = artifact.sizeBytes,
                installedSizeBytes = artifact.sizeBytes,
                sourceHost = SOURCE_HOST_APP_BUNDLE,
                startedAt = startedAt,
                finishedAt = Instant.now().toString(),
                outcome = ModelInstallationReceipt.OUTCOME_INSTALLED,
                steps = listOf(
                    ReceiptStep(ReceiptSteps.HASH, "PASS", "SHA-256 verified against bundled manifest AND catalog (double gate)."),
                    ReceiptStep("atomic-install", "PASS", "Installed at $installed (previous version preserved if any)."),
                    ReceiptStep(ReceiptSteps.REGISTRY_READBACK, "PASS", "Registry record sha256 matches."),
                    ReceiptStep("qualification-activation", "PASS", activationResult.reason),
                ),
            ))
        }
        return BundledInstallResult.Installed(
            artifact = artifact,
            installedPath = installed.toString(),
            receiptId = receiptId,
            qualificationSummary = activationResult.reason,
        )
    }

    /** Evidence sidecar for a hash mismatch: facts only, nothing deleted. */
    private fun writeQuarantineFacts(artifact: BundledArtifact, actualHash: String) {
        runCatching {
            val dir = storage.quarantineDir()
            val stamp = System.currentTimeMillis()
            Files.write(dir.resolve("bundled-${artifact.asset}-$stamp.facts.txt"),
                ("identity=${artifact.identity}\nasset=${artifact.asset}\nexpectedSha256=${artifact.sha256}\n" +
                    "actualSha256=$actualHash\nsource=app-bundle\nquarantinedAt=$stamp\n").toByteArray(Charsets.UTF_8))
        }
    }

    companion object {
        /** Honest provenance literal: these bytes came from inside the APK, no network host. */
        const val SOURCE_HOST_APP_BUNDLE = "app-bundle"
    }
}
