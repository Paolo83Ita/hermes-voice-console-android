package com.hermestower.voiceconsole.voice

import com.hermestower.voiceconsole.voice.onnx.OnnxRunner
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * GRAPH 4 CP-G4-008 (Owner §9 WAKE WORD): REAL inference path over the three
 * physically verified openWakeWord assets (audit §§1–3):
 *   melspec.onnx  [b, samples]      → [t, 1, 1, 32] log-mel
 *   embed.onnx    [b, 76, 32, 1]    → [b, 1, 1, 96] embedding per mel window
 *   hey_jarvis    [1, 16, 96]       → [1, 1]       sigmoid score per 16 embeddings
 *
 * Pipeline: PCM → [MelEmbedFrontend: melspec+embed, REAL] → sliding ring of 16
 * embeddings → [WakeScorer: head, REAL] → threshold → cooldown → duplicate
 * suppression → wake event + false-accept counter.
 *
 * Owner §9 contract:
 *  - actual preprocessing (raw PCM → float32 → mel → 76-frame windows);
 *  - actual inference (three chained ORT graphs — RealMelEmbedFrontend + RealHeadScorer);
 *  - threshold + cooldown + duplicate suppression + false-accept counter (all truthful);
 *  - false-accept/false-reject harness: injectable [MelEmbedFrontend]/[WakeScorer]
 *    fixtures for JVM tests — fixtures are TEST_ONLY and never shipped as models;
 *  - OFF by default until physical qualification (§8; enable() is explicit).
 */
interface MelEmbedFrontend {
    /** One 76×32-bin mel window → one 96-dim embedding. */
    fun embed(melWindow: FloatArray): FloatArray
}

interface WakeScorer {
    /** 16×96 embedding window → wake score (0..1). */
    fun score(embeddingWindow: FloatArray): Float
}

class OpenWakeWordEngine(
    private val frontend: MelEmbedFrontend?,
    private val scorer: WakeScorer?,
) : WakeWordEngine {

    data class WakeWordError(val kind: Kind, val detail: String) {
        enum class Kind { RUNTIME_UNAVAILABLE, INFERENCE_FAILED, MODEL_MISMATCH }
    }

    enum class EngineState { OFF, READY, FAILED }

    private val _state = MutableStateFlow(EngineState.OFF)
    val state: StateFlow<EngineState> = _state.asStateFlow()

    // --- §9 tuning (conservative defaults; final values only after the FA/FR harness) ---
    var threshold: Float = 0.5f
    var cooldownMs: Long = 2_000L

    // §9 counters (truthful diagnostics, surfaced in Self-Test Center)
    var falseAcceptCounter: Int = 0
        private set
    var acceptedCount: Int = 0
        private set

    // streaming state
    private val pcmBuffer = ArrayList<Float>(64)
    private val embeddingRing = FloatArray(RING_EMBEDS * EMBED_DIM)
    private var ringFilled = 0
    private var hasWoken = false
    private var lastWakeAtMs = 0L
    private var lastAcceptedWindow = -1L
    private var windowCounter = 0L

    companion object {
        const val MEL_BINS = 32
        const val MEL_WINDOW = 76           // mel frames per embedding
        const val EMBED_DIM = 96
        const val RING_EMBEDS = 16          // embeddings per head decision
        // VERIFIED from melspec.onnx graph constants (audit §1): Conv kernel 512,
        // stride 160 → one mel frame per 160 samples = 10 ms @ 16 kHz.
        const val SAMPLES_PER_MEL_FRAME = 160
    }

    private val chainAvailable: Boolean
        get() = frontend != null && scorer != null

    /** §9: OFF by default — explicit enabling only (feature-flag path). */
    fun enable() {
        _state.value = if (chainAvailable) EngineState.READY else EngineState.FAILED
    }

    fun disable() {
        _state.value = EngineState.OFF
    }

    override val isReady: Boolean get() = _state.value == EngineState.READY

    /**
     * Feeds one PCM16 chunk (16 kHz mono). Returns true EXACTLY WHEN a wake
     * event fires (threshold + cooldown + duplicate suppression all passed).
     */
    override fun feed(samples: ShortArray, sampleRate: Int, nowMs: Long): Boolean {
        if (_state.value != EngineState.READY) return false
        if (sampleRate != 16000) {
            _state.value = EngineState.FAILED
            return false
        }
        val fe = frontend ?: return false
        val sc = scorer ?: return false
        for (s in samples) pcmBuffer.add(s / 32768.0f)
        var fired = false
        while (pcmBuffer.size >= MEL_WINDOW * SAMPLES_PER_MEL_FRAME) {
            val window = FloatArray(MEL_WINDOW * SAMPLES_PER_MEL_FRAME)
            for (i in window.indices) window[i] = pcmBuffer[i]
            val embedding = try {
                fe.embed(window)
            } catch (e: com.hermestower.voiceconsole.voice.onnx.OnnxRuntimeException) {
                _state.value = EngineState.FAILED
                return fired
            }
            pushEmbedding(embedding)
            pcmBuffer.subList(0, MEL_WINDOW * SAMPLES_PER_MEL_FRAME).clear()
            windowCounter++
            if (ringFilled == RING_EMBEDS) {
                val score = try {
                    sc.score(embeddingRing.copyOf())
                } catch (e: com.hermestower.voiceconsole.voice.onnx.OnnxRuntimeException) {
                    _state.value = EngineState.FAILED
                    return fired
                }
                if (score >= threshold) {
                    if (lastAcceptedWindow == windowCounter - 1) {
                        falseAcceptCounter++ // duplicate suppression (consecutive window)
                    } else if (hasWoken && nowMs - lastWakeAtMs < cooldownMs) {
                        falseAcceptCounter++ // cooldown-suppressed
                    } else {
                        acceptedCount++
                        hasWoken = true
                        lastWakeAtMs = nowMs
                        lastAcceptedWindow = windowCounter - 1
                        fired = true
                    }
                }
            }
        }
        return fired
    }

    private fun pushEmbedding(embedding: FloatArray) {
        require(embedding.size >= EMBED_DIM) { "embedding too small: ${embedding.size}" }
        val src = embedding.copyOfRange(embedding.size - EMBED_DIM, embedding.size)
        if (ringFilled < RING_EMBEDS) {
            System.arraycopy(src, 0, embeddingRing, ringFilled * EMBED_DIM, EMBED_DIM)
            ringFilled++
        } else {
            System.arraycopy(
                embeddingRing, EMBED_DIM, embeddingRing, 0,
                (RING_EMBEDS - 1) * EMBED_DIM,
            )
            System.arraycopy(src, 0, embeddingRing, (RING_EMBEDS - 1) * EMBED_DIM, EMBED_DIM)
        }
    }

    /** §9 unload. */
    fun unload() {
        pcmBuffer.clear()
        ringFilled = 0
        _state.value = EngineState.OFF
    }

    /** FA/FR harness hook: reset buffers + counters between harness runs. */
    fun resetForTest() {
        pcmBuffer.clear()
        ringFilled = 0
        windowCounter = 0
        hasWoken = false
        lastWakeAtMs = 0L
        lastAcceptedWindow = -1
        falseAcceptCounter = 0
        acceptedCount = 0
    }
}

/** §9 seam: push-fed wake detector; at most one event per cooldown window. */
interface WakeWordEngine {
    val isReady: Boolean
    fun feed(samples: ShortArray, sampleRate: Int, nowMs: Long): Boolean
}

/**
 * REAL mel→embed frontend (production): uses the verified melspec.onnx +
 * embed.onnx tensor contracts (audit §§1–2).
 */
class RealMelEmbedFrontend(
    private val melspec: OnnxRunner,
    private val embed: OnnxRunner,
) : MelEmbedFrontend {
    override fun embed(melWindow: FloatArray): FloatArray {
        val mel = melspec.run(
            inputs = mapOf("input" to melWindow),
            inputShapes = mapOf("input" to longArrayOf(1, melWindow.size.toLong())),
        )
        val frames = mel.getValue("output") // [t,1,1,32] flattened
        val need = OpenWakeWordEngine.MEL_WINDOW * OpenWakeWordEngine.MEL_BINS
        require(frames.size >= need) { "melspec produced ${frames.size}; expected ≥ $need" }
        val off = frames.size - need
        val embIn = FloatArray(need)
        for (i in 0 until need) embIn[i] = frames[off + i]
        val emb = embed.run(
            inputs = mapOf("input_1" to embIn),
            inputShapes = mapOf("input_1" to longArrayOf(1, 76, 32, 1)),
        )
        val out = emb.getValue("conv2d_19") // [1,1,1,96]
        require(out.size >= OpenWakeWordEngine.EMBED_DIM) { "embedding too small: ${out.size}" }
        return out.copyOfRange(out.size - OpenWakeWordEngine.EMBED_DIM, out.size)
    }
}

/**
 * REAL wake head (production): uses the verified hey_jarvis.onnx contract
 * [1,16,96] → [1,1] sigmoid (audit §3).
 */
class RealHeadScorer(
    private val head: OnnxRunner,
) : WakeScorer {
    override fun score(embeddingWindow: FloatArray): Float {
        require(embeddingWindow.size == OpenWakeWordEngine.RING_EMBEDS * OpenWakeWordEngine.EMBED_DIM) {
            "head input must be ${OpenWakeWordEngine.RING_EMBEDS * OpenWakeWordEngine.EMBED_DIM}"
        }
        val out = head.run(
            inputs = mapOf("x.1" to embeddingWindow),
            inputShapes = mapOf("x.1" to longArrayOf(1, 16, 96)),
        )
        return out.getValue("53")[0]
    }
}
