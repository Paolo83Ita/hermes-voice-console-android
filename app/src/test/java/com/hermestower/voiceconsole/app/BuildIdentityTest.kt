package com.hermestower.voiceconsole.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * OWNER PHYSICAL RETEST §1: build-identity purity — the variant derivation
 * from the real lib/ entries and the receipt format. Pure JVM (the on-device
 * measurement path needs Android and is exercised on device).
 */
class BuildIdentityTest {

    @Test
    fun `arm64-only lib entries derive the ARM64_V8A variant`() {
        assertEquals(
            BuildIdentity.VARIANT_ARM64_V8A,
            BuildIdentity.variantFromLibEntries(listOf("lib/arm64-v8a/libc.so", "lib/arm64-v8a/libonnx.so")),
        )
    }

    @Test
    fun `multi-abi lib entries derive the UNIVERSAL variant`() {
        assertEquals(
            BuildIdentity.VARIANT_UNIVERSAL,
            BuildIdentity.variantFromLibEntries(
                listOf("lib/arm64-v8a/libc.so", "lib/x86_64/libc.so", "lib/x86_64/libonnx.so"),
            ),
        )
    }

    @Test
    fun `x86_64-only lib entries derive the X86_64 variant`() {
        assertEquals(
            BuildIdentity.VARIANT_X86_64,
            BuildIdentity.variantFromLibEntries(listOf("lib/x86_64/libc.so")),
        )
    }

    @Test
    fun `empty or foreign entries derive UNKNOWN honestly`() {
        assertEquals(BuildIdentity.VARIANT_UNKNOWN, BuildIdentity.variantFromLibEntries(emptyList()))
        assertEquals(BuildIdentity.VARIANT_UNKNOWN, BuildIdentity.variantFromLibEntries(listOf("res/layout.xml")))
    }

    @Test
    fun `the identity block carries every owner-required row`() {
        val injected = BuildIdentity.Injected(
            versionName = "0.3.1-alpha-graph4-owner-repair",
            versionCode = 4,
            gitShaFull = "53841a5154c10aa7177c95fa9c9517b5476e436e",
            gitShaShort = "53841a5",
            buildTimestampIso8601 = "2026-11-06T12:00:00Z",
            workflowRunId = "42226155144",
            workflowRef = "graph-4/real-voice-pairing-archive-e2e",
            buildLabel = "POCO OWNER REPAIR 1",
            packageName = "com.hermestower.voiceconsole",
        )
        val measured = BuildIdentity.Measured(
            apkSha256 = "a".repeat(64),
            apkVariant = BuildIdentity.VARIANT_ARM64_V8A,
            supportedAbisInApk = listOf("arm64-v8a"),
            installationTimestampMs = 1_700_000_000_000,
            lastUpdateTimestampMs = 1_770_000_000_000,
            processStartElapsedMs = 12_345,
            apkSizeBytes = 105_268_304,
        )
        val block = BuildIdentity.format(injected, measured) { ms -> java.time.Instant.ofEpochMilli(ms!!).toString() }
        listOf(
            "versionName: 0.3.1-alpha-graph4-owner-repair",
            "versionCode: 4",
            "git sha (full): 53841a5154c10aa7177c95fa9c9517b5476e436e",
            "git sha (short): 53841a5",
            "build timestamp: 2026-11-06T12:00:00Z",
            "workflow run id: 42226155144",
            "APK variant: ARM64_V8A".lowercase(),
            "apk sha-256 (measured on device): ${"a".repeat(64)}",
            "installed at:",
            "last updated at:",
            "process start (elapsed ms): 12345",
            "build label: POCO OWNER REPAIR 1",
            "package: com.hermestower.voiceconsole",
        ).forEach { assertTrue("missing row: $it\n$block", block.contains(it, ignoreCase = true)) }
    }

    @Test
    fun `footer is compact and carries version sha variant label`() {
        val injected = BuildIdentity.Injected(
            versionName = "0.3.1-alpha-graph4-owner-repair",
            versionCode = 4,
            gitShaFull = "abc",
            gitShaShort = "53841a5",
            buildTimestampIso8601 = "t",
            workflowRunId = "r",
            workflowRef = "b",
            buildLabel = "POCO OWNER REPAIR 1",
            packageName = "p",
        )
        val footer = BuildIdentity.footerLine(injected, null)
        assertTrue(footer.contains("v0.3.1-alpha-graph4-owner-repair (4)"))
        assertTrue(footer.contains("53841a5"))
        assertTrue(footer.contains("UNKNOWN")) // measured == null → honest UNKNOWN
        assertTrue(footer.contains("POCO OWNER REPAIR 1"))
    }

    @Test
    fun `unversioned builds declare themselves honestly`() {
        val injected = BuildIdentity.Injected(
            versionName = "0.3.1-alpha-graph4-owner-repair",
            versionCode = 4,
            gitShaFull = "",
            gitShaShort = "",
            buildTimestampIso8601 = "t",
            workflowRunId = "",
            workflowRef = "",
            buildLabel = "POCO OWNER REPAIR 1",
            packageName = "p",
        )
        val block = BuildIdentity.format(injected, null) { null }
        assertTrue(block.contains("(local build)"))
        assertTrue(!injected.isCiStamped)
    }
}
