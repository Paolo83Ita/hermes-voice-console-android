package com.hermestower.voiceconsole.pairing

/**
 * GRAPH A4R C1-C4 — QR v2 scan flow (the production scanner state machine).
 *
 * Same semantics the canonical v1 machine (QrPairingScanFlow) proved under
 * the Graph 4 suite, bound to the Tower v2 parser ([QrPayload.Parser]):
 *  - duplicate-frame suppression inside the debounce window;
 *  - every NEW raw text goes through the full v2 parse — the typed rejection
 *    matrix (MALFORMED…REPLAYED_NONCE_REF) is inherited unchanged;
 *  - a successful parse is TERMINAL (Detected until [reset]) because the
 *    nonce_ref was consumed — rescanning the same QR would honestly yield a
 *    replay rejection;
 *  - rejections do NOT terminate the flow: the camera keeps scanning;
 *  - the summary is redaction-safe by construction.
 *
 * No clock, camera, or I/O dependency: fully deterministic under test.
 */
class TowerQrV2ScanFlow(
    private val parser: QrPayload.Parser,
    private val clockMs: () -> Long = System::currentTimeMillis,
) {

    sealed class ScanState {
        object Idle : ScanState()
        data class Scanning(val framesAnalyzed: Long) : ScanState()

        /** Strict v2 parse succeeded; NOTHING is persisted yet (UI confirms first). */
        data class Detected(val parsed: QrPayload.Parsed, val summary: String) : ScanState()

        /** Strict parse refused; [reject] is redacted by construction. */
        data class Rejected(val reject: QrPayload.Reject, val framesAnalyzed: Long) : ScanState()
    }

    var state: ScanState = ScanState.Idle
        private set

    var framesAnalyzed: Long = 0
        private set

    private var lastRaw: String? = null
    private var lastProcessedAtMs: Long = Long.MIN_VALUE

    fun onRawScan(raw: String?): ScanState {
        framesAnalyzed++
        if (state is ScanState.Detected) return state
        val now = clockMs()
        val text = raw?.trim().orEmpty()
        if (text.isEmpty()) {
            state = ScanState.Scanning(framesAnalyzed)
            return state
        }
        if (text == lastRaw && now - lastProcessedAtMs < DEBOUNCE_MS) {
            return state
        }
        lastRaw = text
        lastProcessedAtMs = now
        state = try {
            val parsed = parser.parse(text)
            ScanState.Detected(parsed, parser.describe(parsed))
        } catch (e: QrPayload.QrRejectException) {
            ScanState.Rejected(e.reject, framesAnalyzed)
        }
        return state
    }

    /** Back to Idle. The nonce_ref store is NOT cleared: replay stays rejected. */
    fun reset() {
        state = ScanState.Idle
        lastRaw = null
        lastProcessedAtMs = Long.MIN_VALUE
    }

    companion object {
        const val DEBOUNCE_MS = 1_500L
    }
}
