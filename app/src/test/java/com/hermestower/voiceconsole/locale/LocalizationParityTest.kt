package com.hermestower.voiceconsole.locale

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * GRAPH 4 G4-C08 (GATE-05): five-language resource parity. Every supported
 * locale (it/en/es/fr/de) must declare EXACTLY the same string keys with
 * non-empty values; unlocalized technical identifiers are simply not keys.
 * Runs against the REAL production resources (test sourceSet sees res? no —
 * this test reads the file tree directly so the audit is on the shipped XML).
 */
class LocalizationParityTest {

    private fun resDir(): File {
        // app/src/main/res relative to the module the JVM test runs in
        var dir = File(System.getProperty("user.dir") ?: ".")
        val candidates = listOf(
            File(dir, "src/main/res"),
            File(dir, "app/src/main/res"),
        )
        return candidates.first { it.exists() }
    }

    private fun parseKeys(lang: String): Map<String, String> {
        val f = File(resDir(), if (lang == "en") "values/strings.xml" else "values-$lang/strings.xml")
        assertTrue("missing strings.xml for $lang at ${f.path}", f.exists())
        val text = f.readText()
        val regex = Regex("""<string name="([^"]+)">(.*?)</string>""", RegexOption.DOT_MATCHES_ALL)
        return regex.findAll(text).associate { it.groupValues[1] to it.groupValues[2].trim() }
    }

    @Test
    fun `all five locales declare the same key set`() {
        val en = parseKeys("en").keys
        assertTrue("base locale too small", en.size >= 70)
        for (lang in listOf("it", "es", "fr", "de")) {
            val keys = parseKeys(lang).keys
            assertEquals("key set mismatch for $lang", en, keys)
        }
    }

    @Test
    fun `no locale has empty values or leftover placeholders`() {
        for (lang in listOf("en", "it", "es", "fr", "de")) {
            for ((k, v) in parseKeys(lang)) {
                assertTrue("empty value $lang:$k", v.isNotEmpty())
                assertTrue("unfilled placeholder $lang:$k -> $v", !v.contains("XXX") && !v.contains("TODO"))
            }
        }
    }

    private fun parsePluralNames(lang: String): Set<String> {
        val f = File(resDir(), if (lang == "en") "values/strings.xml" else "values-$lang/strings.xml")
        if (!f.exists()) return emptySet()
        val text = f.readText()
        val regex = Regex("""<plurals name="([^"]+)">.*?</plurals>""", RegexOption.DOT_MATCHES_ALL)
        // every plurals entry must carry at least one/other in EVERY locale
        val ok = regex.findAll(text).all { block ->
            listOf("one", "other").all { q -> block.value.contains("quantity=\"$q\"") }
        }
        assertTrue("plurals entries must define one+other in $lang", ok)
        return regex.findAll(text).map { it.groupValues[1] }.toSet()
    }

    @Test
    fun `plurals resources have five-locale parity with one and other quantities`() {
        val en = parsePluralNames("en")
        for (lang in listOf("it", "es", "fr", "de")) {
            assertEquals("plurals mismatch for $lang", en, parsePluralNames(lang))
        }
    }

    @Test
    fun `format-argument placeholders are positional in every locale`() {
        for (lang in listOf("en", "it", "es", "fr", "de")) {
            for ((k, v) in parseKeys(lang)) {
                val bare = Regex("%[sd]").findAll(v).toList()
                assertTrue(
                    "non-positional format arg in $lang:$k -> $v (use %1\$s style)",
                    bare.isEmpty(),
                )
            }
        }
    }

    @Test
    fun `owner terminology anchors are respected in every locale`() {
        // §14 anchors — product name never translated as a generic tower.
        val anchors = mapOf(
            "it" to "Hermes Tower",
            "es" to "Hermes Tower",
            "fr" to "Hermes Tower",
            "de" to "Hermes Tower",
        )
        for ((lang, anchor) in anchors) {
            val all = parseKeys(lang).values.joinToString(" ")
            assertTrue("missing anchor in $lang", all.contains(anchor))
        }
        // Self-test center anchor per locale.
        val selfTest = mapOf(
            "it" to "Centro di autotest",
            "en" to "Self-Test Center",
            "es" to "Centro de autoprueba",
            "fr" to "autotest",
            "de" to "Selbsttest-Center",
        )
        for ((lang, anchor) in selfTest) {
            val all = parseKeys(lang).values.joinToString(" ")
            assertTrue("missing self-test anchor in $lang", all.contains(anchor))
        }
    }

    @Test
    fun `italian uses voice console anchor`() {
        val it = parseKeys("it")
        assertEquals("Console vocale", it["dest_console"])
        assertEquals("Modelli e voce", it["dest_models"])
        assertEquals("Archivio Tower", it["dest_archive"])
    }

    /**
     * GRAPH 4 FINAL directive §5 — required COVERAGE areas: every ordinary
     * product surface must carry translated keys in ALL FIVE locales. A key
     * present in one locale but missing in another is impossible (key-set
     * parity above); this test pins the AREAS themselves so a future key
     * removal cannot silently de-localize a surface (no accidental fallback).
     */
    @Test
    fun `required coverage areas are translated in all five locales`() {
        val areas = mapOf(
            "screen titles" to listOf(
                "connection_title", "archive_title", "ts_title", "qr_title",
                "settings_title", "appearance_title", "voice_title",
                "st_title", "diag_title", "history_title", "li_title", "about_title",
            ),
            "dialogs" to listOf(
                "cd_inject_title", "cd_inject_placeholder", "cd_send", "cd_cancel",
                "history_clear_dialog_title",
            ),
            "accessibility descriptions" to listOf(
                "nav_open_drawer", "inject_cd", "composer_cd",
            ),
            "onboarding" to listOf(
                "ob_title", "ob_s1_title", "ob_s2_title", "ob_s3_title",
                "ob_s4_title", "ob_s5_title", "ob_s6_title", "ob_continue", "ob_s6_finish",
            ),
            "pairing" to listOf(
                "qr_intro", "qr_pair_button", "qr_mock_note", "qr_permission_card",
            ),
            "download states" to listOf(
                "common_download", "mm_download_total", "mm_download_progress",
                "mm_attr_download_size", "mm_last_download_event",
            ),
            "archive" to listOf(
                "archive_tab_conversations", "archive_tab_missions",
                "archive_tab_artifacts", "archive_simulated_badge",
            ),
            "self-test" to listOf(
                "st_title", "st_intro", "st_none_yet",
            ),
        )
        val langs = listOf("en", "it", "es", "fr", "de")
        for ((area, keys) in areas) {
            for (lang in langs) {
                val keysOfLang = parseKeys(lang)
                for (key in keys) {
                    assertTrue(
                        "coverage area '$area' key '$key' missing in $lang",
                        keysOfLang.containsKey(key),
                    )
                    assertTrue(
                        "coverage area '$area' key '$key' empty in $lang",
                        keysOfLang[key]!!.isNotBlank(),
                    )
                }
            }
        }
    }
}
