package com.hermestower.voiceconsole.privacy

/**
 * Log redaction applied to every exported line (SEC-06, unit-tested).
 * Patterns: credential-like assignments, long hex runs, base64-like runs.
 */
object LogRedactor {

    private val assignment = Regex(
        "(?i)\\b(token|key|secret|password|passwd|pwd|bearer|authorization|credential)\\b\\s*[:=]\\s*(bearer\\s+)?\\S+",
    )
    // Hex runs are case-uniform; mixed-case alphanumerics are classified as base64.
    private val hexRun = Regex("(?<![A-Za-z0-9])[0-9a-f]{24,}(?![A-Za-z0-9])|(?<![A-Za-z0-9])[0-9A-F]{24,}(?![A-Za-z0-9])")
    private val base64Run = Regex("(?<![A-Za-z0-9+/])[A-Za-z0-9+/]{32,}={0,2}(?![A-Za-z0-9+/])")

    fun redact(input: String): String {
        var out = assignment.replace(input) { m ->
            val v = m.value
            val sepIdx = v.indexOfFirst { it == ':' || it == '=' }
            var end = sepIdx + 1
            while (end < v.length && v[end].isWhitespace()) end++
            v.substring(0, end) + "[REDACTED]"
        }
        out = hexRun.replace(out, "[REDACTED_HEX]")
        out = base64Run.replace(out, "[REDACTED_B64]")
        return out
    }
}
