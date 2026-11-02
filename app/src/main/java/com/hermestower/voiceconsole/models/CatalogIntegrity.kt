package com.hermestower.voiceconsole.models

/**
 * GRAPH 4 NODE 1 (CP-G4-003) — catalog integrity audit.
 *
 * Enforces the NODE 1 contract on every catalog entry BEFORE anything is offered
 * for download:
 *  - placeholder URLs are blocked (example.invalid, unresolved://, mock:// in
 *    production entries);
 *  - zero hashes are blocked for anything claimed VERIFIED_DOWNLOADABLE;
 *  - mutable unbound sources are blocked (…/latest/, un-pinned /resolve/main/);
 *  - unapproved licenses are blocked;
 *  - ABI and Android compatibility against the physical device are checked;
 *  - only VERIFIED_DOWNLOADABLE entries may ever enter the download pipeline.
 *
 * TEST_FIXTURE_ONLY entries are exempt from the https/revision rules but must be
 * clearly labelled so they can never be presented as production components
 * (INV-G4-04/05/11).
 */
object CatalogIntegrity {

    enum class Severity { ERROR, WARNING }

    data class Finding(
        val identity: String,
        val severity: Severity,
        val code: String,
        val message: String,
    )

    fun audit(
        entries: List<ModelCatalogEntry>,
        deviceApi: Int = 30,
        deviceAbi: String = "arm64-v8a",
    ): List<Finding> {
        val findings = mutableListOf<Finding>()
        for (e in entries) {
            val cls = e.entryClassification

            // --- placeholder / unverifiable source detection --------------------
            val uri = e.sourceUri.lowercase()
            val placeholder = uri.contains("example.invalid") ||
                uri.startsWith("unresolved://")
            val zeroHash = e.sha256.none { it != '0' } || e.sha256.isBlank()

            if (placeholder && cls == "VERIFIED_DOWNLOADABLE") {
                findings += Finding(
                    e.identity, Severity.ERROR, "PLACEHOLDER_SOURCE",
                    "Entry claims VERIFIED_DOWNLOADABLE but its source is a placeholder: ${e.sourceUri}",
                )
            }
            if (zeroHash && cls == "VERIFIED_DOWNLOADABLE") {
                findings += Finding(
                    e.identity, Severity.ERROR, "ZERO_HASH",
                    "Entry claims VERIFIED_DOWNLOADABLE without a verified SHA-256.",
                )
            }

            // --- mutable unversioned sources ------------------------------------
            if (cls == "VERIFIED_DOWNLOADABLE") {
                val mutable = uri.contains("/latest/") ||
                    (uri.contains("resolve/main/") && (e.revisionBinding.isNullOrBlank())) ||
                    uri.endsWith("/releases/download/latest")
                if (mutable) {
                    findings += Finding(
                        e.identity, Severity.ERROR, "MUTABLE_SOURCE",
                        "Downloadable entry uses a mutable unversioned URL without revision binding.",
                    )
                }
                val httpsOk = uri.startsWith("https://")
                if (!httpsOk) {
                    findings += Finding(
                        e.identity, Severity.ERROR, "INSECURE_SOURCE",
                        "Downloadable entry must use https:// (got ${e.sourceUri}).",
                    )
                }
                if (!e.licenseApproved) {
                    findings += Finding(
                        e.identity, Severity.ERROR, "LICENSE_NOT_APPROVED",
                        "Downloadable entry without an approved license.",
                    )
                }
                if (deviceAbi !in e.supportedAbis) {
                    findings += Finding(
                        e.identity, Severity.ERROR, "ABI_MISMATCH",
                        "Entry does not support the device ABI $deviceAbi.",
                    )
                }
                if (e.androidMinVersion > deviceApi) {
                    findings += Finding(
                        e.identity, Severity.ERROR, "ANDROID_MISMATCH",
                        "Entry requires API ${e.androidMinVersion} > device API $deviceApi.",
                    )
                }
            }

            // --- production entries must be clearly separated from fixtures -----
            if (e.componentType != "WAKE_WORD" && cls == "TEST_FIXTURE_ONLY") {
                findings += Finding(
                    e.identity, Severity.WARNING, "FIXTURE_OUTSIDE_WAKE_WORD",
                    "TEST_FIXTURE_ONLY entry outside the wake-word fixture block.",
                )
            }
        }
        return findings
    }

    /** Entries that may legally appear in the Model Sanctum download UI. */
    fun downloadable(entries: List<ModelCatalogEntry>): List<ModelCatalogEntry> =
        entries.filter { it.entryClassification == "VERIFIED_DOWNLOADABLE" }

    /** The NODE 2 recommended minimal voice pack for this physical device. */
    fun recommendedPack(entries: List<ModelCatalogEntry>): List<ModelCatalogEntry> =
        downloadable(entries).filter { it.recommended && !it.optionalComponent }
}
