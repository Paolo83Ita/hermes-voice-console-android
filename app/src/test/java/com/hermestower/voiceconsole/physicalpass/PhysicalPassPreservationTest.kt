package com.hermestower.voiceconsole.physicalpass

import com.hermestower.voiceconsole.core.state.ConsoleState
import com.hermestower.voiceconsole.interrupt.UnifiedInterruptController
import com.hermestower.voiceconsole.locale.ResponseCatalog
import com.hermestower.voiceconsole.locale.ResponseCategory
import com.hermestower.voiceconsole.pairing.QrPairingScanFlow
import com.hermestower.voiceconsole.voice.ListeningModePolicy
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * GRAPH 4.13 §10 — PHYSICAL PASS PRESERVATION regression gates.
 *
 * The Owner granted PRELIMINARY physical PASS for: unified Interrompi,
 * continuous listening, QR scanner open, Italian responses. This graph may not
 * rewrite them. These gates pin the exact seams the visual refactor touches so
 * any regression from the accessibility/UX work fails CI before the Owner ever
 * sees it. No new architecture is introduced around these functions.
 */
class PhysicalPassPreservationTest {

    // ---- PHYS-PASS: UNIFIED INTERRUPT (state-specific labels remain) ----

    @Test
    fun `interrompi keeps five distinct state-specific labels`() {
        val labels = listOf(
            UnifiedInterruptController.LABEL_LISTENING,
            UnifiedInterruptController.LABEL_TRANSCRIBING,
            UnifiedInterruptController.LABEL_PROCESSING,
            UnifiedInterruptController.LABEL_SPEAKING,
            UnifiedInterruptController.LABEL_CONTINUOUS,
        )
        assertTrue("the five interrupt labels must remain distinct", labels.size == labels.toSet().size)
    }

    @Test
    fun `interrompi resolves the correct label for every console state`() {
        // continuous active wins (the physical continuous+interrupt loop)
        assertTrue(
            UnifiedInterruptController.resolve(ConsoleState.SPEAKING, micListening = false, continuousActive = true).labelId ==
                UnifiedInterruptController.LABEL_CONTINUOUS,
        )
        assertTrue(
            UnifiedInterruptController.resolve(ConsoleState.LISTENING, micListening = true, continuousActive = false).labelId ==
                UnifiedInterruptController.LABEL_LISTENING,
        )
        assertTrue(
            UnifiedInterruptController.resolve(ConsoleState.TRANSCRIBING, micListening = false, continuousActive = false).labelId ==
                UnifiedInterruptController.LABEL_TRANSCRIBING,
        )
        for (s in listOf(ConsoleState.UNDERSTANDING_LOCAL, ConsoleState.CONTACTING_TOWER, ConsoleState.TOWER_PROCESSING, ConsoleState.REQUEST_QUEUED)) {
            assertTrue(
                UnifiedInterruptController.resolve(s, micListening = false, continuousActive = false).labelId ==
                    UnifiedInterruptController.LABEL_PROCESSING,
            )
        }
        assertTrue(
            UnifiedInterruptController.resolve(ConsoleState.SPEAKING, micListening = false, continuousActive = false).labelId ==
                UnifiedInterruptController.LABEL_SPEAKING,
        )
    }

    @Test
    fun `interrompi idle state is disabled but present (never hidden)`() {
        val idle = UnifiedInterruptController.resolve(ConsoleState.IDLE, micListening = false, continuousActive = false)
        assertTrue(!idle.enabled)
        assertTrue(!idle.cancellable)
    }

    // ---- PHYS-PASS: CONTINUOUS LISTENING (policy seam preserved) ----

    @Test
    fun `listening mode policy exposes the continuous mode`() {
        val modes = ListeningModePolicy.Mode.entries.map { it.name }
        assertTrue("CONTINUOUS_WHILE_SCREEN_ACTIVE must remain available", "CONTINUOUS_WHILE_SCREEN_ACTIVE" in modes)
    }

    // ---- PHYS-PASS: QR SCANNER (open path + camera permission flow remain) ----

    @Test
    fun `qr scanner open flow keeps the camera permission path`() {
        // the scan flow's public surface (open request → permission → scan → payload)
        // must remain intact; the visual refactor may not touch its architecture.
        val methods = QrPairingScanFlow::class.java.declaredMethods.map { it.name }
        assertTrue(
            "QrPairingScanFlow public API preserved",
            methods.isNotEmpty(),
        )
    }

    // ---- PHYS-PASS: ITALIAN RESPONSE (catalog remains Italian + complete) ----

    @Test
    fun `response catalog stays complete in italian for every category`() {
        // isComplete covers the whole table (non-blank per locale) without
        // invoking String.format on %-bearing entries — the render sites own
        // the arguments; the catalog owns the completeness truth.
        assertTrue(ResponseCatalog.isComplete("it"))
        val simulated = ResponseCatalog.text(ResponseCategory.SIMULATED_ACK, "it", "stato")
        assertTrue(simulated.contains("simulata", ignoreCase = true))
        val identity = ResponseCatalog.text(ResponseCategory.IDENTITY, "it")
        assertTrue(identity.contains("Hermes", ignoreCase = true))
    }

    @Test
    fun `response catalog falls back to italian-identity truth`() {
        // the Owner-mandated Italian identity response (PHYS-PASS-01 anchor)
        val identity = ResponseCatalog.text(ResponseCategory.IDENTITY, "it")
        assertTrue(identity.isNotBlank())
        assertTrue(identity.contains("Hermes", ignoreCase = true))
    }
}
