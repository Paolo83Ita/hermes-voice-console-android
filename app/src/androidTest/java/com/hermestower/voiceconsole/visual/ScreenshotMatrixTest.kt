package com.hermestower.voiceconsole.visual

import android.graphics.Bitmap
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.hermestower.voiceconsole.app.AppGraph
import com.hermestower.voiceconsole.app.AppGraphHolder
import com.hermestower.voiceconsole.e2e.hermesComposeRule
import com.hermestower.voiceconsole.ui.screens.AppearanceScreen
import com.hermestower.voiceconsole.ui.screens.ConsoleScreen
import com.hermestower.voiceconsole.ui.screens.ModelManagerScreen
import com.hermestower.voiceconsole.ui.screens.SelfTestScreen
import com.hermestower.voiceconsole.ui.screens.VoiceAudioScreen
import com.hermestower.voiceconsole.ui.theme.HermesTheme
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * GRAPH 4.13 §9 — DETERMINISTIC SCREENSHOT + RESPONSIVE VISUAL MATRIX
 * (foundation, CP-G4.13-004).
 *
 * Renders the REAL screen composables from the REAL AppGraph in isolation —
 * no navigation walk — at the mandated widths (360/393/411/600 dp) and font
 * scales (1.00/1.30/1.50), captures deterministic PNGs and runs the §9 defect
 * gates: horizontal clipping, blank renders, unreachable interactive nodes.
 * PNGs are written under the app files dir as screenshot evidence.
 *
 * Uses the authorized [hermesComposeRule] factory (the console ships infinite
 * ornamental animations; the factory freezes them through the app's own
 * reduced-motion gate — nothing about navigation/state/semantics changes).
 *
 * CI channel: the android-e2e workflow's E2E_CLASS_FILTER experiment input
 * (class com.hermestower.voiceconsole.visual.ScreenshotMatrixTest) — the
 * standing gate/regression fleet arithmetic is untouched. The unit gates
 * (SemanticContrastAuthorityTest + the audit) run on EVERY push.
 *
 * §9 gates that join with the Block 2 rebuilds (CP-G4.13-006/007): touch
 * targets ≥48 dp on the profile cards and model action rows, vertical-letter-
 * wrap detection on the profile selector, primary-action uniqueness.
 */
@RunWith(AndroidJUnit4::class)
class ScreenshotMatrixTest {

    @get:Rule
    val compose = hermesComposeRule()

    private val graph: AppGraph by lazy {
        AppGraphHolder.graph(InstrumentationRegistry.getInstrumentation().targetContext)
    }

    private fun screenshotDir(): File =
        File(InstrumentationRegistry.getInstrumentation().targetContext.filesDir, "screenshots/g413")
            .apply { mkdirs() }

    /** Renders [content] at [widthDp]/[fontScale], captures a PNG and returns it. */
    private fun renderAndCapture(
        name: String,
        widthDp: Int,
        fontScale: Float,
        content: @Composable AppGraph.() -> Unit,
    ): Bitmap {
        val density = compose.activity.resources.displayMetrics.density
        compose.setContent {
            CompositionLocalProvider(
                LocalDensity provides Density(density, fontScale = fontScale),
            ) {
                HermesTheme(darkTheme = true) {
                    Box(Modifier.requiredWidth(widthDp.dp)) {
                        graph.content()
                    }
                }
            }
        }
        compose.waitForIdle()
        val bmp = compose.onRoot().captureToImage().asAndroidBitmap()
        val out = File(screenshotDir(), "${name}_${widthDp}dp_fs${fontScale}.png")
        out.outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 90, it) }
        android.util.Log.i("ScreenshotMatrix", "captured ${out.absolutePath} ${bmp.width}x${bmp.height}")
        return bmp
    }

    /** §9 blank/unreadable gate: the capture must carry real pixel variance. */
    private fun assertNotBlank(name: String, bmp: Bitmap) {
        val w = bmp.width
        val h = bmp.height
        val pixels = IntArray(w * h)
        bmp.getPixels(pixels, 0, w, 0, 0, w, h)
        var minL = 255.0
        var maxL = 0.0
        var n = 0
        var i = 0
        while (i < pixels.size) {
            val p = pixels[i]
            val l = 0.2126 * (p shr 16 and 0xFF) + 0.7152 * (p shr 8 and 0xFF) + 0.0722 * (p and 0xFF)
            if (l < minL) minL = l
            if (l > maxL) maxL = l
            n++
            i += 4096 // deterministic sparse sweep keeps the gate fast
        }
        assertTrue("$name render is blank (range $minL..$maxL over $n samples)", maxL - minL > 24.0)
    }

    /** §9 clipping gate: every interactive node sits inside the render width. */
    private fun assertNoHorizontalClipping(name: String, widthDp: Int) {
        val density = compose.activity.resources.displayMetrics.density
        val maxPx = widthDp * density + 1f
        val rootWidth = compose.onRoot().fetchSemanticsNode().boundsInRoot.width
        assertTrue(
            "$name: render root narrower than the required width ($rootWidth < ${widthDp * density})",
            rootWidth >= widthDp * density - 1f,
        )
        for (node in compose.onAllNodes(hasClickAction()).fetchSemanticsNodes()) {
            val b = node.boundsInRoot
            assertTrue(
                "$name: clickable node clipped horizontally at ${widthDp}dp (${b.left}..${b.right} vs $maxPx)",
                b.left >= -1f && b.right <= maxPx + 1f,
            )
        }
    }

    private fun renderGate(name: String, widthDp: Int, fontScale: Float, content: @Composable AppGraph.() -> Unit) {
        val bmp = renderAndCapture(name, widthDp, fontScale, content)
        assertNotBlank(name, bmp)
        assertNoHorizontalClipping(name, widthDp)
    }

    /** Widths wider than the guest window are honestly skipped (assumption). */
    private fun assumeFitsWindow(widthDp: Int) {
        val w = compose.activity.resources.configuration.screenWidthDp
        assumeTrue("guest window ${w}dp narrower than requested ${widthDp}dp", w >= widthDp)
    }

    // ---- §9 required screenshot states: Model Library ----

    @Test fun g413_01_modelLibrary_initial_360dp() = renderGate("model_library", 360, 1.00f) { ModelManagerScreen(this) }
    @Test fun g413_02_modelLibrary_initial_393dp() = renderGate("model_library", 393, 1.00f) { ModelManagerScreen(this) }
    @Test fun g413_03_modelLibrary_initial_411dp() = renderGate("model_library", 411, 1.00f) { ModelManagerScreen(this) }

    @Test
    fun g413_04_modelLibrary_initial_600dp() {
        assumeFitsWindow(600)
        renderGate("model_library", 600, 1.00f) { ModelManagerScreen(this) }
    }

    @Test fun g413_05_modelLibrary_fontScale130_360dp() = renderGate("model_library", 360, 1.30f) { ModelManagerScreen(this) }
    @Test fun g413_06_modelLibrary_fontScale150_360dp() = renderGate("model_library", 360, 1.50f) { ModelManagerScreen(this) }

    // ---- §9: configuration card at the three compact widths ----

    @Test fun g413_07_configurationCard_360dp() = renderGate("configuration_card", 360, 1.00f) { ModelManagerScreen(this) }
    @Test fun g413_08_configurationCard_393dp() = renderGate("configuration_card", 393, 1.00f) { ModelManagerScreen(this) }
    @Test fun g413_09_configurationCard_411dp() = renderGate("configuration_card", 411, 1.00f) { ModelManagerScreen(this) }

    // ---- §9: Voice and Audio ----

    @Test fun g413_10_voiceAndAudio_360dp() = renderGate("voice_audio", 360, 1.00f) { VoiceAudioScreen(this) }

    @Test
    fun g413_11_voiceAndAudio_600dp() {
        assumeFitsWindow(600)
        renderGate("voice_audio", 600, 1.00f) { VoiceAudioScreen(this) }
    }

    // ---- §9: Console idle (LISTENING/ERROR/Tower-LOW/BALANCED renders join
    // with the Block 3 integration — the console renders the real bridge
    // idle state here) ----

    @Test fun g413_12_console_idle_360dp() = renderGate("console_idle", 360, 1.00f) {
        ConsoleScreen(graph = this, onOpenHistory = {}, onOpenNavigation = {})
    }
    @Test fun g413_13_console_idle_411dp() = renderGate("console_idle", 411, 1.00f) {
        ConsoleScreen(graph = this, onOpenHistory = {}, onOpenNavigation = {})
    }

    // ---- §9: Appearance + Self-Test (settings surfaces on the semantic shell) ----

    @Test fun g413_14_appearance_360dp() = renderGate("appearance", 360, 1.00f) { AppearanceScreen(this) }
    @Test fun g413_15_selfTest_393dp() = renderGate("self_test", 393, 1.00f) { SelfTestScreen(this) }

    @Test
    fun g413_16_screenshot_evidence_index() {
        // writes the evidence index so the CI artifact lists every capture
        val dir = screenshotDir()
        val files = dir.listFiles()?.map { it.name }?.sorted() ?: emptyList()
        File(dir, "INDEX.txt").writeText(files.joinToString("\n"))
    }
}
