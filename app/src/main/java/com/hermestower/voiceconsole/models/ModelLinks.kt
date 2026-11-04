package com.hermestower.voiceconsole.models

import java.net.URI
import java.net.URL

/**
 * GRAPH 4 FINAL directive §12 — REAL SOURCE AND LICENSE LINKS.
 *
 * Validator for the physically clickable "Open source" / "Open license"
 * actions. A link is OPENABLE only when the directive's pre-conditions hold:
 *
 *  - URI parses and is absolute;
 *  - scheme is exactly HTTPS;
 *  - host is one of the reviewed authoritative hosts;
 *  - the URL is not a placeholder (no example.invalid, no "placeholder"/
 *    "unresolved" path segments, not the in-app mock/gradle schemes);
 *  - the catalog entry binds it to a revision where one exists.
 *
 * The openable decision is PURE JVM logic (java.net.URI) so the shipped
 * catalog can be verified by a unit test (CatalogLinkIntegrityTest) without
 * an Android device. The actual open is an ACTION_VIEW intent wrapped in
 * runCatching — when no browser activity resolves, the UI shows the
 * localized explanation and a "Retry source check" action re-runs this
 * validator. No source substitution is ever performed.
 */
object ModelLinks {

    /** Reviewed authoritative hosts (directive §12: "host is expected"). */
    val allowedHosts = setOf(
        "github.com",
        "raw.githubusercontent.com",
        "objects.githubusercontent.com",
        "huggingface.co",
        "www.apache.org",
        "alphacephei.com",
        "opensource.org",
        "creativecommons.org",
    )

    /** Schemes that are catalog-internal identities, never openable web links. */
    private val internalSchemes = setOf("mock", "unresolved", "gradle", "asset", "file", "data")

    data class LinkCheck(val ok: Boolean, val reason: String?) {
        companion object {
            val OK = LinkCheck(true, null)
            fun blocked(reason: String) = LinkCheck(false, reason)
        }
    }

    /** Validates a URL for the Open-source / Open-license actions. */
    fun validateForOpen(rawUri: String?): LinkCheck {
        if (rawUri.isNullOrBlank()) return LinkCheck.blocked("EMPTY")
        val trimmed = rawUri.trim()
        val parsed = runCatching { URI(trimmed) }.getOrElse {
            return LinkCheck.blocked("MALFORMED_URI")
        }
        val scheme = parsed.scheme?.lowercase() ?: return LinkCheck.blocked("NO_SCHEME")
        if (scheme in internalSchemes) return LinkCheck.blocked("INTERNAL_SCHEME")
        if (scheme != "https") return LinkCheck.blocked("NOT_HTTPS")
        val host = parsed.host?.lowercase() ?: return LinkCheck.blocked("NO_HOST")
        if (host == "example.invalid" || host.endsWith(".example.invalid")) {
            return LinkCheck.blocked("PLACEHOLDER_HOST")
        }
        if (host !in allowedHosts) return LinkCheck.blocked("UNEXPECTED_HOST")
        val path = parsed.rawPath ?: ""
        val lowered = path.lowercase()
        if (lowered.contains("placeholder") || lowered.contains("unresolved") ||
            lowered.contains("todo") || lowered.contains("example.")
        ) {
            return LinkCheck.blocked("PLACEHOLDER_PATH")
        }
        // Reachability of the exact URL is verified at the network layer when the
        // Owner taps it; the validator guarantees the identity and shape.
        return runCatching { URL(trimmed).toURI() }.fold(
            onSuccess = { LinkCheck.OK },
            onFailure = { LinkCheck.blocked("MALFORMED_URL") },
        )
    }

    /**
     * Whether a catalog entry may offer a web source link at all. Bundled
     * foundations (RUNTIME_BUNDLED, TEST_FIXTURE_ONLY, mock/unresolved/gradle
     * sources) are NOT web links: the UI shows "Included with the application"
     * (directive §13) instead of a download/source action.
     */
    fun webLinkAllowed(entry: ModelCatalogEntry): Boolean =
        entry.entryClassification == "VERIFIED_DOWNLOADABLE" &&
            validateForOpen(entry.sourceUri).ok

    /** Sanity for the license URL: same rules as the source link. */
    fun licenseCheck(entry: ModelCatalogEntry): LinkCheck =
        if (entry.licenseUrl.isNullOrBlank()) {
            LinkCheck.blocked("NO_LICENSE_URL")
        } else {
            validateForOpen(entry.licenseUrl)
        }
}
