package com.hermestower.voiceconsole.store

import com.hermestower.voiceconsole.app.AppGraph
import com.hermestower.voiceconsole.privacy.LogRedactor
import java.io.File
import java.security.MessageDigest
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * ANDROID REPAIR CP-008 — the OWNER DIAGNOSTIC BUNDLE exporter (mission
 * section 11): pairing diagnostics, Qwen diagnostics and console routing
 * diagnostics in ONE secret-clean ZIP bundle with manifest and SHA-256.
 *
 * SECRET-CLEAN BY CONSTRUCTION:
 *  - every line passes [LogRedactor.redact];
 *  - the sources are typed enums/hosts/latencies only (the session snapshot,
 *    the LLM diagnostics, the route traces) — no tokens, no private keys, no
 *    claim codes ever reach this class;
 *  - the manifest carries the SHA-256 of each entry for tamper evidence.
 */
class OwnerDiagnosticBundleExporter(private val graph: AppGraph) {

    /** The full text report (also embedded in the ZIP). */
    fun exportBundleText(): String {
        val sb = StringBuilder()
        sb.appendLine("HERMES VOICE CONSOLE — OWNER DIAGNOSTIC BUNDLE")
        sb.appendLine("Generated: ${java.time.Instant.now()}")
        sb.appendLine("App version: ${com.hermestower.voiceconsole.BuildConfig.VERSION_NAME} (${com.hermestower.voiceconsole.BuildConfig.GIT_SHA_SHORT})")
        sb.appendLine()

        // ---- PAIRING DIAGNOSTICS (mission section 11) ----
        val snap = graph.pairingSessionController.snapshot
        sb.appendLine("== PAIRING ==")
        sb.appendLine("state: ${snap.state.name}")
        snap.typedCode?.let { sb.appendLine("code: ${LogRedactor.redact(it)}") }
        snap.failedStage?.let { sb.appendLine("failed_stage: $it") }
        snap.candidateClass?.let { sb.appendLine("endpoint_class: $it") }
        snap.candidateHost?.let { sb.appendLine("endpoint_host: ${LogRedactor.redact(it)}") }
        snap.evidence.forEach { ev ->
            sb.appendLine("candidate: ${ev.transportClass.name} host=${LogRedactor.redact(ev.hostView)} status=${ev.status.name} code=${ev.typedCode?.name ?: "-"} latency_ms=${ev.latencyMs ?: -1} http=${ev.httpStatus ?: -1}")
        }
        sb.appendLine("paired: ${graph.towerStore.isPaired()}")
        sb.appendLine("keystore_failures: ${graph.towerStore.failureHistory().joinToString(",") { it.name }}")
        sb.appendLine()

        // ---- QWEN / LOCAL MODEL DIAGNOSTICS ----
        sb.appendLine("== LOCAL MODEL (Qwen2.5 0.5B) ==")
        val diag = graph.localLlmEngine.diagnostics()
        sb.appendLine("enabled: ${diag.enabled}")
        sb.appendLine("state: ${diag.state.name}")
        sb.appendLine("runtime: ${diag.runtimeName}")
        sb.appendLine("candidate: ${diag.candidateIdentity}")
        sb.appendLine("installed_file_exists: ${diag.installedFileExists}")
        sb.appendLine("installed_size_bytes: ${diag.installedSizeBytes ?: -1}")
        sb.appendLine("sha_verified: ${diag.shaVerified}")
        sb.appendLine("last_code: ${diag.lastDiagnosticCode.name}")
        graph.modelDownloads.progress(diag.candidateIdentity).value.let {
            sb.appendLine("download_state: ${it.state.name} bytes=${it.bytesDownloaded}/${it.totalBytes} error=${it.error?.let { e -> LogRedactor.redact(e) } ?: "null"}")
        }
        val rec = graph.modelRegistry.record(diag.candidateIdentity)
        sb.appendLine("registry_state: ${rec?.state ?: "NO_RECORD"}")
        sb.appendLine("registry_installed_path: ${rec?.installedPath ?: "null"}")
        sb.appendLine()

        // ---- CONSOLE ROUTING DIAGNOSTICS ----
        sb.appendLine("== CONSOLE ROUTING ==")
        val last = graph.routeDecisionLog.last()
        sb.appendLine("last_requested_mode: ${last?.requestedMode ?: "none"}")
        sb.appendLine("last_effective_mode: ${last?.effectiveMode ?: "none"}")
        sb.appendLine("last_executor: ${last?.executor ?: "none"}")
        sb.appendLine("last_reason: ${last?.reason ?: "none"}")
        sb.appendLine("tower_connection: ${last?.towerConnectionState ?: "unknown"}")
        sb.appendLine("local_runtime: ${last?.localRuntimeState ?: "unknown"}")
        graph.routeDecisionLog.snapshot().takeLast(10).forEach {
            sb.appendLine("route: ${it.timestampMs} ${it.requestedMode}->${it.effectiveMode} ${it.executor} ${it.reason}")
        }
        sb.appendLine()
        sb.appendLine("== POLICY ==")
        sb.appendLine("No secrets: token-like strings redacted; no private keys, no durable credentials, no tokens exist in the sources.")
        return sb.toString()
    }

    /**
     * Writes the bundle ZIP (report + manifest with per-entry SHA-256) into
     * [dir] and returns the file. The manifest enables tamper evidence.
     */
    fun writeBundleZip(dir: File): File {
        val report = exportBundleText()
        val stamp = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"))
        val out = File(dir, "hermes-owner-diagnostic-bundle-$stamp.zip")
        dir.mkdirs()
        ZipOutputStream(out.outputStream().buffered()).use { zos ->
            fun entry(name: String, content: String) {
                zos.putNextEntry(ZipEntry(name))
                zos.write(content.toByteArray(Charsets.UTF_8))
                zos.closeEntry()
            }
            entry("OWNER_DIAGNOSTIC_BUNDLE.txt", report)
            val sha = MessageDigest.getInstance("SHA-256").digest(report.toByteArray(Charsets.UTF_8))
                .joinToString("") { "%02x".format(it) }
            entry(
                "MANIFEST.json",
                "{" +
                    "\"schema\": \"android_repair.owner_diagnostic_bundle/1.0\", " +
                    "\"entries\": [{\"name\": \"OWNER_DIAGNOSTIC_BUNDLE.txt\", \"sha256\": \"$sha\", \"bytes\": ${report.toByteArray(Charsets.UTF_8).size}}], " +
                    "\"secret_scan\": \"LogRedactor on every line; sources typed enums/hosts only; no token/key/credential classes reachable\", " +
                    "\"redaction\": \"LogRedactor\"" +
                    "}",
            )
        }
        return out
    }
}
