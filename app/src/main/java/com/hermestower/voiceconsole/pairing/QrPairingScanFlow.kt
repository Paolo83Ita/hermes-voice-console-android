package com.hermestower.voiceconsole.pairing

/**
 * GRAPH 4 CP-G4-010 wave 2 (G4-C09 wave 2): QR pairing scan flow — the pure
 * state machine between a camera barcode analyzer and the strict
 * [PairingPayloadParser]. The analyzer only delivers raw decoded strings; this
 * class owns every policy decision:
 *
 *  - duplicate-frame suppression: ML Kit fires per video frame, so the SAME
 *    raw text inside the debounce window is ignored (no state churn, no
 *    double-pair prompts);
 *  - every NEW raw text goes through the full strict parser — the typed
 *    rejection matrix (MALFORMED…REPLAYED_NONCE) is inherited unchanged;
 *  - a successful parse is TERMINAL for the flow (Detected stays until
 *    [reset]) because the parser consumed the nonce — rescanning the same
 *    QR would honestly yield REPLAYED_NONCE;
 *  - rejections do NOT terminate the flow: the camera keeps scanning and a
 *    corrected/different QR can be accepted without leaving the screen;
 *  - the summary shown by the UI comes from [PairingPayloadParser.describe],
 *    which is redaction-safe by construction (no endpoint, no challenge).
 *
 * No clock, camera, or I/O dependency: fully deterministic under test.
 */
class QrPairingScanFlow(
    private val parser: PairingPayloadParser,
    private val clockMs: () -> Long = System::currentTimeMillis,
) {

    sealed class ScanState {
        /** No scan accepted yet. */
        object Idle : ScanState()

        /** Frames are being analyzed; no decodable QR content since last reset. */
        data class Scanning(val framesAnalyzed: Long) : ScanState()

        /** Strict parse succeeded; [summary] is redaction-safe, payload carries the pair() material. */
        data class Detected(val payload: PairingPayload, val summary: String) : ScanState()

        /** Strict parse refused; [reject] is redacted by construction. */
        data class Rejected(val reject: PairingReject, val framesAnalyzed: Long) : ScanState()
    }

    var state: ScanState = ScanState.Idle
        private set

    /** Total frames surfaced by the analyzer (truthful counter for diagnostics). */
    var framesAnalyzed: Long = 0
        private set

    private var lastRaw: String? = null
    private var lastProcessedAtMs: Long = Long.MIN_VALUE

    /** Entry point for every analyzer callback. Null/blank = frame without QR. */
    fun onRawScan(raw: String?): ScanState {
        // Truthful counter: EVERY surfaced frame counts, including duplicates
        // and frames held off by a terminal Detected state.
        framesAnalyzed++
        if (state is ScanState.Detected) return state // terminal until reset()
        val now = clockMs()
        val text = raw?.trim().orEmpty()
        if (text.isEmpty()) {
            state = ScanState.Scanning(framesAnalyzed)
            return state
        }
        if (text == lastRaw && now - lastProcessedAtMs < DEBOUNCE_MS) {
            return state // duplicate frame inside the debounce window: ignored
        }
        lastRaw = text
        lastProcessedAtMs = now
        state = parser.parse(text).fold(
            onSuccess = { p -> ScanState.Detected(p, parser.describe(p)) },
            onFailure = { t ->
                val reject = (t as? PairingRejectException)?.reject
                    ?: PairingReject(PairingRejectKind.MALFORMED, "scan decode failure")
                ScanState.Rejected(reject, framesAnalyzed)
            },
        )
        return state
    }

    /** Back to Idle (new scan session). Nonce store is NOT cleared: replay stays rejected. */
    fun reset() {
        state = ScanState.Idle
        lastRaw = null
        lastProcessedAtMs = Long.MIN_VALUE
    }

    companion object {
        /** Same-raw suppression window (ms): QR dwell on camera ≈ many frames/sec. */
        const val DEBOUNCE_MS = 1_500L
    }
}
