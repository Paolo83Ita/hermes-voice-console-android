package com.hermestower.voiceconsole.app

import android.content.Context
import android.content.pm.PackageInfo
import android.os.Build
import android.os.Process
import com.hermestower.voiceconsole.BuildConfig
import java.io.File
import java.util.Locale
import java.util.zip.ZipFile

/**
 * BuildIdentity (OWNER PHYSICAL RETEST §1): the in-app proof of the installed
 * build — the Owner must be able to prove the exact installed build from
 * inside the application WITHOUT ADB.
 *
 * Authority split (no invented value anywhere):
 *  - BUILD-INJECTED (authoritative, baked into the APK by Gradle from CI env):
 *    versionName, versionCode, Git SHA (full/short), build timestamp (ISO 8601),
 *    workflow run ID, build label, package name;
 *  - ON-DEVICE MEASURED (read from the REAL installed file at runtime — these
 *    are measurements of the installed artifact, not generated identities):
 *    installed-APK SHA-256 (streamed hash of the actual APK file), APK variant
 *    (UNIVERSAL / ARM64_V8A / X86_64 from the lib/ entries inside the installed
 *    APK), installation timestamp, last-update timestamp (PackageInfo);
 *  - PROCESS-MEASURED: current process start time.
 *
 * A build cannot hash its own APK at build time (chicken-and-egg) and no
 * runtime value is FABRICATED — the on-device rows are labeled `measured` and
 * match the CI-published sha256sum byte for byte.
 */
object BuildIdentity {

    const val VARIANT_UNIVERSAL = "UNIVERSAL"
    const val VARIANT_ARM64_V8A = "ARM64_V8A"
    const val VARIANT_X86_64 = "X86_64"
    const val VARIANT_UNKNOWN = "UNKNOWN"

    /** Build-injected identity (values baked at build time; empty when built outside CI). */
    data class Injected(
        val versionName: String,
        val versionCode: Int,
        val gitShaFull: String,
        val gitShaShort: String,
        val buildTimestampIso8601: String,
        val workflowRunId: String,
        val workflowRef: String,
        val buildLabel: String,
        val packageName: String,
    ) {
        val isCiStamped: Boolean get() = gitShaFull.isNotBlank()
    }

    /** On-device measured identity of the INSTALLED APK file. */
    data class Measured(
        val apkSha256: String?,
        val apkVariant: String,
        val supportedAbisInApk: List<String>,
        val installationTimestampMs: Long?,
        val lastUpdateTimestampMs: Long?,
        val processStartElapsedMs: Long,
        val apkSizeBytes: Long?,
    )

    fun injected(): Injected = Injected(
        versionName = BuildConfig.VERSION_NAME,
        versionCode = BuildConfig.VERSION_CODE,
        gitShaFull = BuildConfig.GIT_SHA_FULL,
        gitShaShort = BuildConfig.GIT_SHA_SHORT,
        buildTimestampIso8601 = BuildConfig.BUILD_TIMESTAMP_ISO8601,
        workflowRunId = BuildConfig.BUILD_WORKFLOW_RUN_ID,
        workflowRef = BuildConfig.BUILD_WORKFLOW_REF,
        buildLabel = BuildConfig.BUILD_LABEL,
        packageName = BuildConfig.APPLICATION_ID,
    )

    /**
     * Measures the REAL installed APK: streamed SHA-256 of the actual file,
     * the lib/<abi> entries inside it (variant truth), PackageInfo install /
     * update timestamps and the process start time. Every failure yields null
     * honestly — nothing is substituted or invented.
     */
    fun measure(context: Context): Measured {
        val appInfo = runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0)
        }.getOrNull()
        val sourceDir = appInfo?.applicationInfo?.sourceDir
        val sha = sourceDir?.let { sha256Of(File(it)) }
        val (variant, abis) = sourceDir?.let { inspectVariant(File(it)) }
            ?: (VARIANT_UNKNOWN to emptyList())
        return Measured(
            apkSha256 = sha,
            apkVariant = variant,
            supportedAbisInApk = abis,
            installationTimestampMs = appInfo?.firstInstallTime,
            lastUpdateTimestampMs = appInfo?.lastUpdateTime,
            processStartElapsedMs = runCatching { Process.getStartElapsedRealtime() }.getOrDefault(-1L),
            apkSizeBytes = sourceDir?.let { File(it).length() },
        )
    }

    /**
     * Derives the APK variant from the lib/<abi>/ entries inside the installed
     * APK: x86_64 present → UNIVERSAL (the debug universal APK carries every
     * ABI); only arm64-v8a → ARM64_V8A; only x86_64 → X86_64. Pure function.
     */
    fun variantFromLibEntries(libDirs: List<String>): String {
        val abis = libDirs
            .map { it.removePrefix("lib/").substringBefore('/').lowercase(Locale.ROOT) }
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()
        return when {
            abis.size > 1 -> VARIANT_UNIVERSAL
            abis == listOf("arm64-v8a") -> VARIANT_ARM64_V8A
            abis == listOf("x86_64") -> VARIANT_X86_64
            else -> VARIANT_UNKNOWN
        }
    }

    /** Pure helper: the multi-line human-readable identity block (receipt + panel share it). */
    fun format(
        injected: Injected,
        measured: Measured?,
        isoTimestamp: (Long?) -> String?,
    ): String = buildString {
        appendLine("HERMES VOICE CONSOLE — BUILD IDENTITY")
        appendLine("build label: ${injected.buildLabel}")
        appendLine("versionName: ${injected.versionName}")
        appendLine("versionCode: ${injected.versionCode}")
        appendLine("package: ${injected.packageName}")
        appendLine("git sha (full): ${injected.gitShaFull.ifBlank { "(not CI-stamped — local build)" }}")
        appendLine("git sha (short): ${injected.gitShaShort.ifBlank { "(local build)" }}")
        appendLine("build timestamp: ${injected.buildTimestampIso8601}")
        appendLine("workflow run id: ${injected.workflowRunId.ifBlank { "(local build)" }}")
        appendLine("workflow ref: ${injected.workflowRef.ifBlank { "(local build)" }}")
        if (measured == null) {
            appendLine("installed apk: (measurement unavailable)")
        } else {
            appendLine("apk variant: ${measured.apkVariant}")
            if (measured.supportedAbisInApk.isNotEmpty()) {
                appendLine("apk abis: ${measured.supportedAbisInApk.joinToString()}")
            }
            appendLine("apk sha-256 (measured on device): ${measured.apkSha256 ?: "(unavailable)"}")
            measured.apkSizeBytes?.let { appendLine("apk size (bytes): $it") }
            appendLine("installed at: ${isoTimestamp(measured.installationTimestampMs) ?: "(unavailable)"}")
            appendLine("last updated at: ${isoTimestamp(measured.lastUpdateTimestampMs) ?: "(unavailable)"}")
            if (measured.processStartElapsedMs >= 0) {
                appendLine("process start (elapsed ms): ${measured.processStartElapsedMs}")
            }
        }
        appendLine("android: ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT}), ${Build.MANUFACTURER} ${Build.MODEL}")
    }.trimEnd()

    /** The compact one-line footer for the Diagnostics screen. */
    fun footerLine(injected: Injected, measured: Measured?): String {
        val sha = injected.gitShaShort.ifBlank { "local" }
        val variant = measured?.apkVariant ?: VARIANT_UNKNOWN
        return "v${injected.versionName} (${injected.versionCode}) · $sha · $variant · ${injected.buildLabel}"
    }

    private fun sha256Of(file: File): String? = runCatching {
        val digest = java.security.MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buf = ByteArray(64 * 1024)
            while (true) {
                val read = input.read(buf)
                if (read < 0) break
                digest.update(buf, 0, read)
            }
        }
        digest.digest().joinToString("") { "%02x".format(it) }
    }.getOrNull()

    private fun inspectVariant(apk: File): Pair<String, List<String>> = runCatching {
        ZipFile(apk).use { zip ->
            val abis = zip.entries().asSequence()
                .map { it.name }
                .filter { it.startsWith("lib/") && it.count { ch -> ch == '/' } >= 2 }
                .map { it.split('/')[1].lowercase(Locale.ROOT) }
                .distinct()
                .sorted()
                .toList()
            val libDirs = abis.map { "lib/$it" }
            variantFromLibEntries(libDirs) to abis
        }
    }.getOrDefault(VARIANT_UNKNOWN to emptyList())
}
