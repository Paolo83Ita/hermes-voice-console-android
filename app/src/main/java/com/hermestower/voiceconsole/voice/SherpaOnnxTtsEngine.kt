package com.hermestower.voiceconsole.voice

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicLong

/**
 * CP-G4.9-003 — REAL downloadable natural Italian TTS delivery.
 *
 * The Android system engine remains the MANDATORY fallback; this engine is the
 * natural local voice path that rides on the VERIFIED piper vits bundle
 * (`vits-piper-it_IT-riccardo-x_low`, sha256 8ad07915…7021, verified by direct
 * download this session; `vits-piper-it_IT-paola-medium`, sha256 7541f757…16b4)
 * synthesised by the VERIFIED sherpa-onnx Android runtime (JitPack tag
 * v1.12.24, AAR sha256 ad052d51…6c7e, minSdk 21, Apache-2.0, arm64-v8a JNI).
 *
 * Honesty contract (mirrors VoskFrameSttEngine):
 *  - when the voice bundle is NOT installed/verified, the engine reports
 *    [TtsState.NOT_INSTALLED] and the console route stays on the Android
 *    system TTS (the app NEVER claims the natural voice while speaking with
 *    the system engine);
 *  - the sherpa-onnx classes are referenced ONLY through the injected
 *    [NativeBridge], so JVM unit tests construct and drive this engine
 *    without the native library (the real bridge binds com.k2fsa.sherpa.onnx);
 *  - load() verifies the bundle files exist before binding (no aspirational
 *    load), synthesis runs on a single background thread, playback is REAL
 *    AudioTrack PCM output, interrupt() stops generation AND playback;
 *  - SpeechProfile is the rate/pitch authority: rate drives the vits
 *    generation speed, pitch drives the AudioTrack playback rate (a real
 *    resampling pitch shift; documented as slightly affecting duration);
 *  - every failure is typed, spoken content never lands in the telemetry
 *    (SEC §4: length-only).
 */
class SherpaOnnxTtsEngine(
    /** Root directory of the installed voice bundle (filesDir/models/voices/…). */
    private val voiceRoot: File,
    /** Human-visible identity surfaced in Diagnostics/Self-Test when loaded. */
    private val voiceIdentity: String,
    /** Injected native bridge so the byte-path stays unit-testable. */
    private val native: NativeBridge,
    /** Injected PCM playback (AudioTrack on device, fake in tests). */
    private val player: PlayerBridge,
    /** Persisted speech profile (rate + pitch authority). */
    private val speechProfileProvider: () -> SpeechProfile,
    private val telemetry: TtsTelemetry,
) : TtsEngine {

    /**
     * The ONLY seam to the native runtime. The production bridge (wired in
     * AppGraph) constructs com.k2fsa.sherpa.onnx.OfflineTts and calls
     * generate(); unit tests inject a deterministic fake.
     */
    interface NativeBridge {
        /** Returns an opaque native handle or throws [TtsNativeException]. */
        fun createVits(modelPath: String, tokensPath: String, dataDir: String): Any

        /** Synchronous synthesis (runs on the engine's worker thread). */
        fun generate(handle: Any, text: String, speed: Float): FloatArray

        fun sampleRate(handle: Any): Int

        fun release(handle: Any)
    }

    /** Typed native failure (the UI shows the reason, never a silent swap). */
    class TtsNativeException(message: String, cause: Throwable? = null) : IllegalStateException(message, cause)

    /**
     * PCM playback seam. The production bridge drives android.media.AudioTrack
     * (REAL device output); JVM tests inject a deterministic fake — the engine
     * logic (state truth, interruption, pitch application) stays unit-provable.
     */
    interface PlayerBridge {
        /** Starts playing [pcm]; returns false when playback cannot start. */
        fun play(pcm: ShortArray, sampleRate: Int, playbackRate: Int, onDone: Runnable): Boolean

        /** Stops any current playback (idempotent). */
        fun stop()
    }

    /** Production playback: AudioTrack MODE_STATIC with a completion marker. */
    class AudioTrackPlayer : PlayerBridge {
        @Volatile
        private var track: AudioTrack? = null

        override fun play(pcm: ShortArray, sampleRate: Int, playbackRate: Int, onDone: Runnable): Boolean {
            val t = runCatching {
                AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                            .build(),
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build(),
                    )
                    .setBufferSizeInBytes(maxOf(pcm.size * 2, sampleRate * 2))
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()
            }.getOrNull() ?: return false
            track = t
            t.write(pcm, 0, pcm.size)
            // Pitch: a real resampling shift (playback rate). 1.0 = untouched.
            runCatching { t.playbackRate = playbackRate }
            t.setNotificationMarkerPosition(pcm.size - 1)
            t.setPlaybackPositionUpdateListener(object : AudioTrack.OnPlaybackPositionUpdateListener {
                override fun onMarkerReached(t: AudioTrack?) {
                    runCatching { t?.pause(); t?.release() }
                    if (track === t) track = null
                    onDone.run()
                }

                override fun onPeriodicNotification(t: AudioTrack?) {}
            })
            return runCatching { t.play() }.isSuccess
        }

        override fun stop() {
            runCatching { track?.pause(); track?.release() }
            track = null
        }
    }

    private val _state = MutableStateFlow(TtsOutput(TtsState.NOT_INSTALLED))
    override val state: StateFlow<TtsOutput> = _state.asStateFlow()

    /** The bundle directory actually loaded (null until load() succeeds). */
    @Volatile
    var loadedVoiceDir: File? = null
        private set

    val engineId: String
        get() = if (loadedVoiceDir != null) {
            "sherpa-onnx v1.12.24 + $voiceIdentity"
        } else {
            "Android system TTS (natural voice not installed)"
        }

    private val worker = Executors.newSingleThreadExecutor { r -> Thread(r, "sherpa-tts") }
    private var handle: Any? = null
    private val generation = AtomicLong(0)

    /** Resolves the actual bundle directory (the install may unzip one level deep). */
    fun resolveVoiceDir(): File? {
        val children: Array<File> = voiceRoot.listFiles() ?: emptyArray()
        val candidates = listOf(voiceRoot, *children)
        for (dir in candidates) {
            if (!dir.isDirectory) continue
            val model = dir.listFiles()?.firstOrNull { it.name.endsWith(".onnx") }
            val hasTokens = dir.resolve("tokens.txt").isFile
            val hasEspeak = dir.resolve("espeak-ng-data").isDirectory
            if (model != null && hasTokens && hasEspeak) return dir
        }
        return null
    }

    override fun load() {
        if (handle != null) return
        val dir = resolveVoiceDir()
        if (dir == null) {
            _state.value = TtsOutput(TtsState.NOT_INSTALLED)
            return
        }
        telemetry.log(TtsTelemetry.Event.ENGINE_INIT_START, "route=natural voice=$voiceIdentity")
        _state.value = TtsOutput(TtsState.LOADING)
        runCatching {
            val model = dir.listFiles()!!.first { it.name.endsWith(".onnx") }
            native.createVits(
                modelPath = model.absolutePath,
                tokensPath = dir.resolve("tokens.txt").absolutePath,
                dataDir = dir.resolve("espeak-ng-data").absolutePath,
            )
        }.onSuccess { bound ->
            handle = bound
            loadedVoiceDir = dir
            telemetry.log(TtsTelemetry.Event.ENGINE_INIT_OK, "route=natural")
            _state.value = TtsOutput(TtsState.READY)
        }.onFailure { e ->
            telemetry.log(TtsTelemetry.Event.ENGINE_INIT_FAILED, "route=natural error=${e.javaClass.simpleName}")
            _state.value = TtsOutput(TtsState.FAILED, error = "natural TTS load failed: ${e.message}")
        }
    }

    override fun unload() {
        generation.incrementAndGet() // invalidate any in-flight utterance
        player.stop()
        handle?.let { runCatching { native.release(it) } }
        handle = null
        loadedVoiceDir = null
        telemetry.log(TtsTelemetry.Event.ENGINE_SHUTDOWN, "route=natural")
        _state.value = TtsOutput(TtsState.UNLOADED)
    }

    override fun speak(text: String) {
        val bound = handle
        if (bound == null) {
            _state.value = TtsOutput(_state.value.state, error = "natural speak called before load")
            return
        }
        // The natural voice IS Italian: the read-back is published verbatim so
        // the same fail-closed gate and diagnostics apply to both routes.
        val readback = ItalianTtsVoiceNegotiator.TtsVoiceReadback(
            requestedLocaleTag = "it-IT",
            setLanguageResult = ItalianTtsVoiceNegotiator.TtsLanguageResultCode.LANG_COUNTRY_VAR_AVAILABLE,
            selectedVoiceName = voiceIdentity,
            activeVoiceName = voiceIdentity,
            activeVoiceLocaleTag = "it-IT",
            activeLanguageTag = "it-IT",
            engineName = engineId,
            status = ItalianTtsVoiceNegotiator.ItalianTtsVoiceStatus.ITALIAN_VOICE_ACTIVE,
            availableItalianVoiceNames = listOf(voiceIdentity),
        )
        val verdict = TtsSpeakGate.decide(
            effectiveLanguageTag = "it",
            readback = readback,
        )
        if (verdict is TtsSpeakGate.Verdict.Block) {
            telemetry.log(TtsTelemetry.Event.SPEAK_BLOCKED, "token=${verdict.reason.name}")
            _state.value = TtsOutput(
                TtsState.TEXT_ONLY_SAFE_DEGRADED,
                voiceReadback = readback,
                speakBlocked = TtsSpeakBlock(reason = verdict.reason, readbackStatus = verdict.readbackStatus),
            )
            return
        }
        val profile = runCatching { speechProfileProvider() }.getOrDefault(SpeechProfile.RECOMMENDED)
        val myGeneration = generation.incrementAndGet()
        _state.value = TtsOutput(TtsState.SYNTHESIZING, spokenText = null, voiceReadback = readback)
        telemetry.log(TtsTelemetry.Event.SPEAK_CALL, "route=natural len=${text.length} rate=${profile.speechRate} pitch=${profile.pitch}")
        worker.execute {
            // A newer speak()/interrupt()/unload() invalidates this utterance.
            if (generation.get() != myGeneration) return@execute
            val audio = runCatching { native.generate(bound, text, profile.speechRate) }
                .onFailure {
                    if (generation.get() == myGeneration) {
                        telemetry.log(TtsTelemetry.Event.UTTERANCE_ERROR, "route=natural ${it.javaClass.simpleName}")
                        _state.value = TtsOutput(TtsState.FAILED, error = "natural synthesis failed: ${it.message}")
                    }
                }
                .getOrNull()
            if (generation.get() != myGeneration) return@execute
            if (audio == null || audio.isEmpty()) {
                _state.value = TtsOutput(TtsState.FAILED, error = "natural synthesis produced no audio")
                return@execute
            }
            playAndReport(audio, bound, myGeneration, readback)
        }
    }

    /** REAL playback through the [PlayerBridge] (AudioTrack on device). */
    private fun playAndReport(samples: FloatArray, bound: Any, myGeneration: Long, readback: ItalianTtsVoiceNegotiator.TtsVoiceReadback) {
        val sampleRate = runCatching { native.sampleRate(bound) }.getOrDefault(16_000)
        val profile = runCatching { speechProfileProvider() }.getOrDefault(SpeechProfile.RECOMMENDED)
        val pcm = ShortArray(samples.size) { (samples[it].coerceIn(-1f, 1f) * Short.MAX_VALUE).toInt().toShort() }
        val pitchRate = (sampleRate * profile.pitch.coerceIn(0.5f, 2.0f)).toInt()
        if (generation.get() != myGeneration) return
        telemetry.log(TtsTelemetry.Event.UTTERANCE_START, "route=natural samples=${samples.size} rate=$pitchRate")
        val started = runCatching {
            player.play(
                pcm = pcm,
                sampleRate = sampleRate,
                playbackRate = pitchRate,
                onDone = {
                    if (generation.get() == myGeneration) {
                        telemetry.log(TtsTelemetry.Event.UTTERANCE_DONE, "route=natural")
                        _state.value = TtsOutput(TtsState.READY, voiceReadback = readback)
                    }
                },
            )
        }.getOrDefault(false)
        if (!started) {
            telemetry.log(TtsTelemetry.Event.UTTERANCE_ERROR, "route=natural playback unavailable")
            _state.value = TtsOutput(TtsState.FAILED, error = "natural playback unavailable", voiceReadback = readback)
            return
        }
        _state.value = TtsOutput(TtsState.SPEAKING, voiceReadback = readback)
    }

    override fun interrupt() {
        generation.incrementAndGet() // kills generation AND playback of the current utterance
        player.stop()
        if (handle != null) _state.value = TtsOutput(TtsState.READY)
    }
}
