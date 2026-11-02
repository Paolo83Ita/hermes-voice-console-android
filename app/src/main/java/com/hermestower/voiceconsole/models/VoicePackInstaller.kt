package com.hermestower.voiceconsole.models

import kotlinx.coroutines.CancellationException
import java.time.Instant

/**
 * VoicePackInstaller (GRAPH 4 §6 RECOMMENDED SETUP + §7 delivery pipeline): plans the
 * minimal recommended voice pack for THIS device, states its full preflight summary,
 * and executes it only after an explicit Owner confirmation.
 *
 * HONESTY RULES enforced here:
 *  - the pack NEVER contains optional components (local LLM, alternative voices,
 *    denoiser, fixtures) — they stay visible as "not included, optional";
 *  - a required type with no verified production source (e.g. the custom Italian
 *    wake word) appears as an honest BLOCKED gap with its reason — it is never
 *    substituted silently by an unrelated model;
 *  - a bundled runtime (sherpa-onnx TTS via Gradle) appears as BUNDLED, not as a
 *    download;
 *  - execution is sequential download → hash → atomic install → registry read-back
 *    → qualification → activation; it STOPS truthfully at the first failure and
 *    writes per-component MODEL_INSTALLATION_RECEIPTs plus a pack receipt (§7);
 *  - every duration figure is a labeled transport estimate, never a measurement
 *    (unmeasured values remain TARGET_PENDING_DEVICE_QUALIFICATION).
 */
data class PackItem(
    val entry: ModelCatalogEntry?,
    val componentType: String,
    /** BUNDLED_RUNTIME | DOWNLOADABLE | ALREADY_INSTALLED | BLOCKED_NO_VERIFIED_SOURCE | INCOMPATIBLE */
    val role: String,
    val included: Boolean,
    val reason: String,
)

data class PackPlan(
    val profile: String,
    val items: List<PackItem>,
) {
    val includedItems: List<PackItem> get() = items.filter { it.included && it.entry != null }
}

data class PackPreflightSummary(
    val totalDownloadBytes: Long,
    val totalInstalledBytes: Long,
    val peakTempBytes: Long,
    val reserveBytes: Long,
    val minimumFreeRequiredBytes: Long,
    val durationRangeText: String,
    val wifiAdvice: String,
    val chargingAdvice: String,
    val thermalAdvice: String,
    val licenses: List<String>,
    val optionalItems: List<String>,
    val blockedItems: List<Pair<String, String>>,
)

data class ComponentOutcome(
    val identity: String,
    val version: String,
    val success: Boolean,
    val receiptId: String? = null,
    val reason: String,
)

data class PackResult(
    val success: Boolean,
    val outcomes: List<ComponentOutcome>,
    val interrupted: Boolean,
    val packReceiptId: String?,
)

class DefaultVoicePackInstaller(
    private val downloads: DefaultModelDownloadManager,
    private val qualification: ModelQualificationRunner,
    private val activation: ModelActivationController,
    private val registry: ModelInstallationRegistry,
    private val storage: ModelStorageManager,
    /** Await timeout per component download; tests shrink it. */
    private val awaitTimeoutMs: Long = 10L * 60 * 1000,
) {

    // ------------------------------------------------------------------ planning

    /**
     * Plan the minimal pack for the given profile: every policy-required component
     * type plus recommended entries. Honest gaps are rendered as blocked items.
     */
    fun plan(profile: VoiceModelProfile, resources: DeviceResources, catalog: VoiceModelCatalog): PackPlan {
        val policy = VoicePackPolicyResolver.policyFor(profile)
        val items = mutableListOf<PackItem>()
        val coveredTypes = mutableSetOf<String>()

        // 1. Recommended entries (the minimal-pack membership marked in the catalog).
        catalog.all().filter { it.recommended }.forEach { entry ->
            coveredTypes += entry.componentType
            items += classify(entry, resources)
        }

        // 2. Required types not covered by a recommended entry: honest gap item.
        policy.required.filter { it.name !in coveredTypes }.forEach { type ->
            val candidates = catalog.ofComponent(type)
            val entry = candidates.firstOrNull { it.entryClassification != "TEST_FIXTURE_ONLY" }
            items += PackItem(
                entry = entry,
                componentType = type.name,
                role = if (entry == null) "BLOCKED_NO_VERIFIED_SOURCE" else "BLOCKED_NO_VERIFIED_SOURCE",
                included = false,
                reason = if (entry == null) {
                    "No ${type.displayLabel} entry exists in the catalog yet — awaiting a verified production source."
                } else {
                    "Required for the minimal pack, but classification is ${entry.entryClassification}: " +
                        (entry.knownLimitations.firstOrNull() ?: "no verified production source yet.")
                },
            )
        }
        return PackPlan(profile.name, items)
    }

    internal fun classify(entry: ModelCatalogEntry, resources: DeviceResources): PackItem {
        val record = registry.record(entry.identity)
        val installedCurrent = record != null && record.version == entry.version &&
            (record.state == ModelInstallState.INSTALLED.name || record.state == ModelInstallState.QUALIFIED.name ||
                record.state == ModelInstallState.ACTIVE.name)
        if (entry.entryClassification == "RUNTIME_BUNDLED") {
            return PackItem(entry, entry.componentType, "BUNDLED_RUNTIME", false,
                "Engine is bundled with the application build (Gradle) — no download required.")
        }
        if (installedCurrent) {
            return PackItem(entry, entry.componentType, "ALREADY_INSTALLED", false,
                "Already installed (v${entry.version}) and registered.")
        }
        if (entry.entryClassification != "VERIFIED_DOWNLOADABLE") {
            return PackItem(entry, entry.componentType, "BLOCKED_NO_VERIFIED_SOURCE", false,
                "Catalog classification is ${entry.entryClassification} — no verified production source to download.")
        }
        val compat = DefaultVoicePackResolver().compatibility(entry, resources)
        if (!compat.compatible) {
            return PackItem(entry, entry.componentType, "INCOMPATIBLE", false, compat.reason ?: "Incompatible with this device.")
        }
        return PackItem(entry, entry.componentType, "DOWNLOADABLE", true, "Will be downloaded, verified, installed, qualified and activated.")
    }

    // ------------------------------------------------------------------ summary

    fun summarize(plan: PackPlan, resources: DeviceResources): PackPreflightSummary {
        val included = plan.includedItems.mapNotNull { it.entry }
        val totalDownload = included.sumOf { it.downloadSizeBytes }
        val totalInstalled = included.sumOf { it.installedSizeBytes }
        // Conservative peak: at the last atomic move every partial AND every installed
        // file coexist. Never presented as a measurement.
        val peakTemp = totalDownload + totalInstalled
        // Mission §7 reserve: the greater of the 2 GB floor and 15 percent of the
        // storage currently available.
        val reserve = maxOf(PACK_RESERVE_FLOOR_BYTES, resources.freeStorageBytes / 100L * 15L)
        val licenses = included.map { it.license }.distinct()
        val optional = plan.items.mapNotNull { it.entry }.filter { it.optionalComponent }.map { it.displayName }
        val blocked = plan.items.filter { it.role == "BLOCKED_NO_VERIFIED_SOURCE" || it.role == "INCOMPATIBLE" }
            .map { (it.entry?.displayName ?: it.componentType) to it.reason }
        return PackPreflightSummary(
            totalDownloadBytes = totalDownload,
            totalInstalledBytes = totalInstalled,
            peakTempBytes = peakTemp,
            reserveBytes = reserve,
            minimumFreeRequiredBytes = peakTemp + reserve,
            durationRangeText = durationEstimateText(totalDownload),
            wifiAdvice = "Wi-Fi recommended: ${formatBytes(totalDownload)} will be transferred.",
            chargingAdvice = "Charging recommended: download + qualification + first model load are battery-intensive.",
            thermalAdvice = "Downloads block automatically at severe thermal status; moderate thermal status only warns.",
            licenses = licenses,
            optionalItems = optional,
            blockedItems = blocked,
        )
    }

    /**
     * Transport lower/upper bound from labeled assumption constants — a planning
     * estimate, explicitly NOT a measured duration (INV: unmeasured values stay
     * TARGET_PENDING_DEVICE_QUALIFICATION).
     */
    fun durationEstimateText(bytes: Long): String {
        if (bytes <= 0) return "no download required"
        val secondsMin = bytes * 8 / (ESTIMATE_MAX_MBPS * 1_000_000L)
        val secondsMax = bytes * 8 / (ESTIMATE_MIN_MBPS * 1_000_000L)
        return "≈ ${humanDuration(secondsMin)}–${humanDuration(secondsMax)} at ${ESTIMATE_MIN_MBPS}–${ESTIMATE_MAX_MBPS} Mbps " +
            "(transport estimate, not measured — TARGET_PENDING_DEVICE_QUALIFICATION)"
    }

    private fun humanDuration(seconds: Long): String {
        val s = seconds.coerceAtLeast(1)
        return if (s < 90) "${s}s" else "${s / 60}m${if (s % 60 > 0) " ${s % 60}s" else ""}"
    }

    // ---------------------------------------------------------------- execution

    /**
     * Execute the pack sequentially. STOPS at the first failed component (truthful
     * partial state: earlier successes stay installed and active). Writes one receipt
     * per component and one pack receipt. Optional components are NEVER included.
     */
    suspend fun installPack(
        plan: PackPlan,
        resources: DeviceResources,
        onEvent: (String) -> Unit = {},
    ): PackResult {
        val started = Instant.now()
        val outcomes = mutableListOf<ComponentOutcome>()
        var interrupted = false
        try {
            for (item in plan.includedItems) {
                val entry = item.entry ?: continue
                onEvent("Pack: processing ${entry.displayName}…")
                val outcome = installComponent(entry, resources, onEvent)
                outcomes += outcome
                if (!outcome.success) {
                    onEvent("Pack stopped: ${entry.displayName} — ${outcome.reason}")
                    break
                }
            }
        } catch (e: CancellationException) {
            interrupted = true
            writePackReceipt(plan, started, outcomes, interrupted)
            throw e
        }
        val packId = writePackReceipt(plan, started, outcomes, interrupted)
        val success = outcomes.isNotEmpty() && outcomes.all { it.success }
        onEvent(if (success) "Recommended voice pack installed (${outcomes.size} components)." else "Voice pack did not complete — see receipts.")
        return PackResult(success, outcomes, interrupted, packId)
    }

    private fun writePackReceipt(
        plan: PackPlan,
        started: Instant,
        outcomes: List<ComponentOutcome>,
        interrupted: Boolean,
    ): String {
        val packId = "voice-pack-${started.toEpochMilli()}"
        val receipt = PackInstallationReceipt(
            receiptId = packId,
            profile = plan.profile,
            startedAt = started.toString(),
            finishedAt = Instant.now().toString(),
            outcome = when {
                interrupted -> "INTERRUPTED"
                outcomes.isNotEmpty() && outcomes.all { it.success } -> "INSTALLED"
                else -> "PARTIAL_OR_FAILED"
            },
            components = outcomes.map { PackComponentOutcome(it.identity, it.version, it.success, it.receiptId, it.reason) },
            interruptedByUser = interrupted,
        )
        runCatching { ReceiptWriter.writePack(storage.receiptsDir(), receipt) }
        return packId
    }

    /**
     * One component through the full §7 pipeline with receipts. Public so the UI can
     * later reuse the same receipted path for single-component installs.
     */
    suspend fun installComponent(
        entry: ModelCatalogEntry,
        resources: DeviceResources,
        onEvent: (String) -> Unit = {},
    ): ComponentOutcome {
        val started = Instant.now()
        val steps = mutableListOf<ReceiptStep>()
        var resumedFrom = 0L
        var retries = 0

        fun finish(success: Boolean, reason: String, extraSteps: List<ReceiptStep> = emptyList()): ComponentOutcome {
            steps += extraSteps
            val receipt = ModelInstallationReceipt(
                receiptId = "${entry.identity}-${started.toEpochMilli()}",
                identity = entry.identity,
                displayName = entry.displayName,
                version = entry.version,
                componentType = entry.componentType,
                sha256 = entry.sha256,
                downloadSizeBytes = entry.downloadSizeBytes,
                installedSizeBytes = entry.installedSizeBytes,
                sourceHost = hostOf(entry.sourceUri),
                startedAt = started.toString(),
                finishedAt = Instant.now().toString(),
                outcome = if (success) ModelInstallationReceipt.OUTCOME_INSTALLED else ModelInstallationReceipt.OUTCOME_FAILED,
                resumedFromBytes = resumedFrom,
                retryCount = retries,
                steps = steps.toList(),
            )
            val written = runCatching { ReceiptWriter.writeReceipts(storage.receiptsDir(), receipt) }.getOrNull()
            return ComponentOutcome(entry.identity, entry.version, success, written?.fileName?.toString(), reason)
        }

        // 1. Gate (connectivity, thermal, metered, power saving).
        val gate = DownloadGatekeeper.check(resources, entry.downloadSizeBytes)
        steps += ReceiptStep(ReceiptSteps.PREFLIGHT, if (gate.allowed) "PASS" else "BLOCKED", gate.blockReason ?: "allowed; warnings: ${gate.warnings.size}")
        if (!gate.allowed) return finish(false, gate.blockReason ?: "blocked by preflight gate")

        // 2. Download from the VERIFIED catalog source.
        val before = downloads.progress(entry.identity).value
        resumedFrom = if (before.state == ModelInstallState.DOWNLOAD_PAUSED) before.bytesDownloaded else 0L
        downloads.start(entry, entry.sourceUri)
        val awaited = awaitTerminal(entry.identity, onEvent)
        steps += ReceiptStep(ReceiptSteps.DOWNLOAD, if (awaited.state == ModelInstallState.DOWNLOADED) "PASS" else "FAILED",
            "${awaited.bytesDownloaded}/${awaited.totalBytes} bytes${awaited.error?.let { "; $it" } ?: ""}")
        if (awaited.state != ModelInstallState.DOWNLOADED) {
            return finish(false, awaited.error ?: "download ended in ${awaited.state}")
        }

        // 3. Hash verification (mismatch quarantines the partial — §7).
        val hashOk = downloads.verifyHash(entry)
        steps += ReceiptStep(ReceiptSteps.HASH, if (hashOk) "PASS" else "FAILED", if (hashOk) entry.sha256 else "mismatch — quarantined")
        if (!hashOk) return finish(false, "SHA-256 mismatch — partial quarantined")

        // 4. Atomic install (previous version preserved — §7).
        val installed = downloads.install(entry)
        steps += ReceiptStep(ReceiptSteps.INSTALL, if (installed != null) "PASS" else "FAILED", installed?.toString() ?: "atomic move failed")
        if (installed == null) return finish(false, "atomic install failed")

        // 5. Fresh registry read-back (§7 step 19) — the registry file is re-read.
        val record = registry.record(entry.identity)
        val readBackOk = record != null && record.state == ModelInstallState.INSTALLED.name &&
            record.sha256 == entry.sha256 && installed.toString() == record.installedPath
        steps += ReceiptStep(ReceiptSteps.REGISTRY_READBACK, if (readBackOk) "PASS" else "FAILED",
            record?.let { "state=${it.state} sha=${it.sha256.take(12)}…" } ?: "no record")
        if (!readBackOk) return finish(false, "registry read-back failed after install")

        // 6. Qualification — activation only after qualification (§7 step 21).
        val qual = qualification.qualify(entry)
        steps += ReceiptStep(ReceiptSteps.QUALIFICATION, if (qual.qualified) "PASS" else "FAILED",
            qual.checks.joinToString { "${it.name}=${if (it.passed) "ok" else "FAIL"}" })
        if (!qual.qualified) return finish(false, "qualification failed")

        // 7. Activation.
        val act = activation.activate(entry)
        steps += ReceiptStep(ReceiptSteps.ACTIVATION, if (act.success) "PASS" else "REFUSED", act.reason)
        return finish(act.success, if (act.success) "installed, qualified and active" else "activation refused: ${act.reason}")
    }

    /** Poll the download flow until a terminal/paused state or the timeout. */
    private suspend fun awaitTerminal(identity: String, onEvent: (String) -> Unit): DownloadProgress {
        val deadline = System.currentTimeMillis() + awaitTimeoutMs
        var lastBytes = -1L
        while (true) {
            val p = downloads.progress(identity).value
            if (p.state.terminal || p.state == ModelInstallState.DOWNLOAD_PAUSED ||
                p.state == ModelInstallState.DOWNLOADED
            ) return p
            if (p.bytesDownloaded != lastBytes) {
                lastBytes = p.bytesDownloaded
                onEvent("Pack download: ${p.identity} ${p.bytesDownloaded}/${p.totalBytes} bytes")
            }
            if (System.currentTimeMillis() > deadline) {
                downloads.cancel(identity)
                return downloads.progress(identity).value.copy(error = "await timeout after ${awaitTimeoutMs} ms")
            }
            kotlinx.coroutines.delay(200)
        }
    }

    private fun hostOf(uri: String): String = runCatching {
        java.net.URI(uri).host ?: uri.substringBefore("://").take(24)
    }.getOrDefault("local")

    companion object {
        /** Mission §7: reserve is the greater of 2 GB and 15 percent of available storage. */
        const val PACK_RESERVE_FLOOR_BYTES: Long = 2L * 1024 * 1024 * 1024
        /** Labeled transport-assumption bounds for duration estimates (NOT measurements). */
        const val ESTIMATE_MIN_MBPS: Long = 10
        const val ESTIMATE_MAX_MBPS: Long = 80

        fun formatBytes(bytes: Long): String = when {
            bytes >= 1L shl 30 -> "%.1f GB".format(bytes.toDouble() / (1L shl 30))
            bytes >= 1L shl 20 -> "%.1f MB".format(bytes.toDouble() / (1L shl 20))
            bytes >= 1L shl 10 -> "%.1f KB".format(bytes.toDouble() / (1L shl 10))
            else -> "$bytes B"
        }
    }
}

/** Deterministic fallback contract per component type (§6 per-item "fallback"). */
object ComponentFallback {
    fun forType(componentType: String): String = when (componentType) {
        "WAKE_WORD" -> "Push-to-talk button (default for initial physical qualification)."
        "VAD" -> "Manual listening start/stop."
        "SPEECH_TO_TEXT" -> "Typed text input."
        "TTS_ENGINE" -> "Android system offline TTS; text-only response as last resort."
        "VOICE" -> "Android system TTS default voice."
        "NOISE_SUPPRESSION" -> "Raw microphone audio (denoising off)."
        "LOCAL_LLM" -> "Hermes Tower (complex reasoning is never local)."
        else -> "Hermes Tower."
    }
}
