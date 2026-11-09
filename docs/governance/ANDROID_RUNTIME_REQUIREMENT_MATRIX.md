# ANDROID RUNTIME REQUIREMENT MATRIX (Graph 4.14, CP-G4.14-002/003)

Machine-readable authority: ANDROID_RUNTIME_REQUIREMENT_MATRIX.json (same directory).
Decision vocabulary = charter §5. Runtime integration order (§6) satisfied:
Vosk IT → sherpa-onnx → Piper Riccardo → Piper Paola → GTCRN → wake-word → optional LLM.

| Component | Capability | Required Android runtime | Runtime version | Decision | Runtime state (post-4.14) |
| --- | --- | --- | --- | --- | --- |
| org.vosk.model.small-it-0.22 | SPEECH_TO_TEXT | com.alphacephei:vosk-android:0.3.75 | 0.3.75 | **REUSE EXISTING RUNTIME** | INTEGRATED — VoskFrameSttEngine loads org.vosk.Model from filesDir/models/stt/vosk-small-it; real Recognizer 16 kHz |
| org.sherpa-onnx.tts.runtime | TTS_ENGINE | com.github.k2-fsa:sherpa-onnx:v1.12.24 | v1.12.24 | **REUSE EXISTING RUNTIME** | INTEGRATED — SherpaOnnxTtsEngine + SherpaRealNativeBridge (OfflineTts VITS); AudioTrackPlayer playback; TtsRouteSwitch authority |
| org.piper.voice.it-riccardo-x-low | VOICE | sherpa-onnx v1.12.24 | 1.12.24 | **REUSE EXISTING RUNTIME** | RUNTIME AVAILABLE IN CODE — engine wired; capability ladder must advance INSTALLED→RUNTIME_RESOLVED→…→ACTIVE; activation gated by physical qualificati |
| org.piper.voice.it-paola-medium | VOICE | sherpa-onnx v1.12.24 | 1.12.24 | **REUSE EXISTING RUNTIME** | RUNTIME AVAILABLE IN CODE — same ladder as Riccardo; alternative voice (larger, higher first-audio latency) |
| org.gtcrn.speech-enhancement.simple | NOISE_SUPPRESSION | com.microsoft.onnxruntime:onnxruntime-android:1.20.0 | 1.20.0 | **REUSE EXISTING RUNTIME** | INTEGRATED — GtcrnDenoiserEngine real streaming WOLA STFT→ORT→iSTFT; optional, off by default |
| org.silero.vad.v5 | VAD | com.microsoft.onnxruntime:onnxruntime-android:1.20.0 | 1.20.0 | **EXTEND EXISTING RUNTIME** | ENGINE GAP REPAIRED IN 4.14 — SileroVadEngine added on the existing ORT runner (state/stateN streaming); EnergyVadEngine stays fallback |
| org.openwakeword.model.melspectrogram | WAKE_WORD | com.microsoft.onnxruntime:onnxruntime-android:1.20.0 | 1.20.0 | **REUSE EXISTING RUNTIME** | INTEGRATED as pipeline dependency (RealMelEmbedFrontend); not user-activatable standalone |
| org.openwakeword.model.embedding | WAKE_WORD | com.microsoft.onnxruntime:onnxruntime-android:1.20.0 | 1.20.0 | **REUSE EXISTING RUNTIME** | INTEGRATED as pipeline dependency (RealEmbedHead); not user-activatable standalone |
| org.openwakeword.wakeword.hey-jarvis | WAKE_WORD | com.microsoft.onnxruntime:onnxruntime-android:1.20.0 | 1.20.0 | **KEEP DOWNLOAD ONLY IN ADVANCED LAB** | ENGINE REAL, OFF BY DEFAULT (SEC-04 false-accept policy) — pipeline qualification fixture (EN) |
| org.hermestower.wakeword.hermes-it | WAKE_WORD | openWakeWord ORT chain | pending | **BLOCK UNTIL RUNTIME AVAILABLE** | SOURCE NEVER FOUND — download refused (SOURCE_UNVERIFIED_DOWNLOAD_BLOCKED); honest pending entry |
| org.qwen.qwen2.5-0.5b-instruct.q5-k-m | LOCAL_LLM | dev.ffmpegkit-maintained:llama-android:0.1.1 | 0.1.1 | **ADD VERIFIED ANDROID RUNTIME** | INTEGRATION COMPLETED IN 4.14 — LlamaCppAndroidRuntime binds Llama.loadModel/complete/releaseModel behind LlamaNativeBridge; NoVerifiedLlmRuntime reta |
| org.hermestower.test.probe-artifact | WAKE_WORD | none | 1.0.0 | **REMOVE MISLEADING ENTRY** | TEST_FIXTURE_ONLY — never offered as a production component |

## Per-component detail

### org.vosk.model.small-it-0.22 — Vosk Italian (small)

- Decision: **REUSE_EXISTING_RUNTIME** — Runtime integrated and real since Graph 4 FINAL; this graph produces the end-to-end runtime receipt and keeps the ladder authoritative.
- Model: kaldi-zip (int8) / int8 ; artifact: vosk-model-small-it-0.22.zip (verified 2026-11-02, 49,665,141 B, sha256 9ec65e75…f3267)
- Runtime: com.alphacephei:vosk-android:0.3.75 (Maven Central) @ 0.3.75 (immutable revision: vosk-android 0.3.75 + model small-it-0.22 frozen alphacephei.com URL)
- Coordinates: com.alphacephei:vosk-android:0.3.75 ; ABIs: arm64-v8a ; minSdk 21
- License: Apache-2.0 (runtime) / Apache-2.0 (model) (no license assumption; sources read this mission)
- Native deps: libvosk (JNI, bundled by AAR)
- Pre/post: 16 kHz mono PCM16 20 ms frames (AudioRecordFrameSource) → Kaldi JSON partial/final → Transcript(text, confidence, isFinal, it)
- Memory ≈256 MB ; storage ≈91 MB
- Load: VoskNativeBridge.loadModel(dir) ; Inference: Recognizer.acceptWaveForm / finalResult ; Unload: Model finalization (bridge release)
- Fallback: Android system SpeechRecognizer (EXTRA_PREFER_OFFLINE) ; Rollback: Keep system STT route; model removal → NOT_INSTALLED
- Physical qualification: PHYSICAL_QUALIFICATION_PENDING (Owner device)

### org.sherpa-onnx.tts.runtime — sherpa-onnx TTS runtime

- Decision: **REUSE_EXISTING_RUNTIME** — JitPack tag build verified CP-G4.9-003; catalog stale CANDIDATE_PENDING_DISCOVERY label corrected to RUNTIME_BUNDLED/INTEGRATED truth.
- Model: native-runtime (code) / n/a ; artifact: AAR v1.12.24 (sha256 ad052d51…6c7e, JitPack tag build, inspected CP-G4.9-003)
- Runtime: com.github.k2-fsa:sherpa-onnx:v1.12.24 (JitPack, immutable tag) @ v1.12.24 (immutable revision: JitPack tag v1.12.24 (v2.x line exists, un-adopted per pinning policy))
- Coordinates: com.github.k2-fsa:sherpa-onnx:v1.12.24 ; ABIs: arm64-v8a, armeabi-v7a, x86, x86_64 ; minSdk 21
- License: Apache-2.0 (no license assumption; sources read this mission)
- Native deps: libsherpa-onnx-jni + libonnxruntime (pickFirsts)
- Pre/post: text → VITS phonemizer (espeak-ng-data inside voice bundle) → PCM → AudioTrack MODE_STATIC playback; latency read-back
- Memory ≈128 MB ; storage ≈0 MB
- Load: SherpaRealNativeBridge.createVits(model, tokens, dataDir) ; Inference: OfflineTts.generate(text, sid, speed) ; Unload: OfflineTts release via bridge
- Fallback: Android system TTS (TtsRouteSwitch SYSTEM route) ; Rollback: Route switch back to SYSTEM; engines remain installed
- Physical qualification: PHYSICAL_QUALIFICATION_PENDING (Owner device)

### org.piper.voice.it-riccardo-x-low — Piper Italian voice — Riccardo (x-low)

- Decision: **REUSE_EXISTING_RUNTIME** — Engine + native bridge real; ladder activation path documented; ONE primary action per state; qualification pending on device.
- Model: vits-onnx-bundle / x-low fp32 ; artifact: vits-piper-it_IT-riccardo-x_low.tar.bz2 (verified 2026-11-02, sha256 8ad07915…7021)
- Runtime: sherpa-onnx v1.12.24 (INTEGRATED, see runtime row) @ 1.12.24 (immutable revision: sherpa-onnx release tag tts-models)
- Coordinates: n/a (model artifact; runtime com.github.k2-fsa:sherpa-onnx) ; ABIs: arm64-v8a ; minSdk 30
- License: MIT (no license assumption; sources read this mission)
- Native deps: sherpa-onnx jni
- Pre/post: espeak-ng phonemization (bundle data) → AudioTrack playback
- Memory ≈128 MB ; storage ≈39 MB
- Load: createVits(modelPath, tokensPath, espeak-ng-data) ; Inference: generate(text, sid=0, speed) ; Unload: release
- Fallback: Android system TTS Italian voice ; Rollback: ModelRollbackController; deactivate → ladder down
- Physical qualification: PHYSICAL_QUALIFICATION_PENDING

### org.piper.voice.it-paola-medium — Piper Italian voice — Paola (medium)

- Decision: **REUSE_EXISTING_RUNTIME** — Same as Riccardo; alternative voice.
- Model: vits-onnx-bundle / medium fp32 ; artifact: vits-piper-it_IT-paola-medium.tar.bz2 (verified 2026-11-02, sha256 7541f757…16b4)
- Runtime: sherpa-onnx v1.12.24 (INTEGRATED) @ 1.12.24 (immutable revision: sherpa-onnx release tag tts-models)
- Coordinates: n/a (model artifact) ; ABIs: arm64-v8a ; minSdk 30
- License: MIT (no license assumption; sources read this mission)
- Native deps: sherpa-onnx jni
- Pre/post: espeak-ng phonemization → AudioTrack playback
- Memory ≈256 MB ; storage ≈81 MB
- Load: createVits(modelPath, tokensPath, espeak-ng-data) ; Inference: generate(text, sid=0, speed) ; Unload: release
- Fallback: Android system TTS Italian voice ; Rollback: ModelRollbackController
- Physical qualification: PHYSICAL_QUALIFICATION_PENDING

### org.gtcrn.speech-enhancement.simple — GTCRN speech enhancement (denoiser)

- Decision: **REUSE_EXISTING_RUNTIME** — Real streaming ORT inference; optional by default.
- Model: onnx fp32 / fp32 ; artifact: gtcrn_simple.onnx (verified, 535,638 B, sha256 e77603ac…1b534; bundled asset)
- Runtime: com.microsoft.onnxruntime:onnxruntime-android:1.20.0 (Maven Central, INTEGRATED) @ 1.20.0 (immutable revision: sherpa-onnx speech-enhancement-models asset (sha-pinned))
- Coordinates: com.microsoft.onnxruntime:onnxruntime-android:1.20.0 (Maven Central, INTEGRATED) ; ABIs: arm64-v8a ; minSdk 30
- License: MIT (no license assumption; sources read this mission)
- Native deps: libonnxruntime.so
- Pre/post: STFT 512/256 WOLA + 3 streaming caches → iSTFT → overlap-add → denoised PCM
- Memory ≈32 MB ; storage ≈1 MB
- Load: OrtOnnxRunner.createSession(path) ; Inference: runner.run([1,257,1,2] mix + caches) ; Unload: session close
- Fallback: raw audio passthrough (typed) ; Rollback: disable denoising setting
- Physical qualification: PHYSICAL_QUALIFICATION_PENDING (A/B on device)

### org.silero.vad.v5 — Silero VAD

- Decision: **EXTEND_EXISTING_RUNTIME** — NEW SileroVadEngine on the existing ORT runner closes the engine gap (download-exposed-but-unusable FINDING-01 case); EnergyVadEngine stays fallback.
- Model: onnx fp32 / fp32 ; artifact: silero_vad.onnx (verified 2026-11-02, 2,327,524 B, sha256 2623a295…d788f)
- Runtime: com.microsoft.onnxruntime:onnxruntime-android:1.20.0 (Maven Central, INTEGRATED) @ 1.20.0 (immutable revision: tag v5.1.2 (raw.githubusercontent, sha-pinned))
- Coordinates: com.microsoft.onnxruntime:onnxruntime-android:1.20.0 (Maven Central, INTEGRATED) ; ABIs: arm64-v8a ; minSdk 30
- License: MIT (no license assumption; sources read this mission)
- Native deps: libonnxruntime.so
- Pre/post: 16 kHz mono PCM frames (576-sample windows, v5.1.2 contract) → speech probability → hysteresis + hangover → isSpeech
- Memory ≈16 MB ; storage ≈3 MB
- Load: OrtOnnxRunner.createSession(path) ; Inference: run(input, state, sr) → (prob, stateN) ; Unload: session close
- Fallback: EnergyVadEngine (RMS, dependency-free) ; Rollback: VAD selection setting reverts to energy baseline
- Physical qualification: PHYSICAL_QUALIFICATION_PENDING

### org.openwakeword.model.melspectrogram — openWakeWord melspectrogram (engine dependency)

- Decision: **REUSE_EXISTING_RUNTIME** — Engine dependency of the openWakeWord chain; hidden from ordinary download offers as standalone.
- Model: onnx fp32 / fp32 ; artifact: melspectrogram.onnx (verified, sha256 ba2b0e0f…76f)
- Runtime: com.microsoft.onnxruntime:onnxruntime-android:1.20.0 (Maven Central, INTEGRATED) @ 1.20.0 (immutable revision: release v0.5.1)
- Coordinates: com.microsoft.onnxruntime:onnxruntime-android:1.20.0 (Maven Central, INTEGRATED) ; ABIs: arm64-v8a ; minSdk 30
- License: Apache-2.0 (no license assumption; sources read this mission)
- Native deps: libonnxruntime.so
- Pre/post: 16 kHz melspec frames → 96×96 features → embedding model
- Memory ≈40 MB ; storage ≈2 MB
- Load: OrtOnnxRunner ; Inference: run ; Unload: session close
- Fallback: n/a (dependency) ; Rollback: wake-word off by default
- Physical qualification: covered by pipeline qualification

### org.openwakeword.model.embedding — openWakeWord embedding model (engine dependency)

- Decision: **REUSE_EXISTING_RUNTIME** — Engine dependency; same handling.
- Model: onnx fp32 / fp32 ; artifact: embedding_model.onnx (verified, sha256 70d16429…c1f)
- Runtime: com.microsoft.onnxruntime:onnxruntime-android:1.20.0 (Maven Central, INTEGRATED) @ 1.20.0 (immutable revision: release v0.5.1)
- Coordinates: com.microsoft.onnxruntime:onnxruntime-android:1.20.0 (Maven Central, INTEGRATED) ; ABIs: arm64-v8a ; minSdk 30
- License: Apache-2.0 (no license assumption; sources read this mission)
- Native deps: libonnxruntime.so
- Pre/post: melspec features → embedding → head scorer
- Memory ≈40 MB ; storage ≈2 MB
- Load: OrtOnnxRunner ; Inference: run ; Unload: session close
- Fallback: n/a ; Rollback: wake-word off by default
- Physical qualification: covered by pipeline qualification

### org.openwakeword.wakeword.hey-jarvis — openWakeWord 'Hey Jarvis'

- Decision: **KEEP_DOWNLOAD_ONLY_IN_ADVANCED_LAB** — Real engine exists but stays OFF by default (SEC-04); download offered only in the Advanced Laboratory as the pipeline qualification fixture.
- Model: onnx fp32 / fp32 ; artifact: hey_jarvis_v0.1.onnx (verified, sha256 94a13cfe…2cb; bundled)
- Runtime: com.microsoft.onnxruntime:onnxruntime-android:1.20.0 (Maven Central, INTEGRATED) @ 1.20.0 (immutable revision: release v0.5.1)
- Coordinates: com.microsoft.onnxruntime:onnxruntime-android:1.20.0 (Maven Central, INTEGRATED) ; ABIs: arm64-v8a ; minSdk 30
- License: Apache-2.0 (no license assumption; sources read this mission)
- Native deps: libonnxruntime.so
- Pre/post: melspec→embed chain → threshold+cooldown+duplicate suppression+false-accept counter
- Memory ≈40 MB ; storage ≈2 MB
- Load: OrtOnnxRunner ; Inference: run ; Unload: session close
- Fallback: manual push-to-talk (Interrompi path) ; Rollback: keep OFF
- Physical qualification: BLOCKED until false-accept/false-reject testing passes on Owner device

### org.hermestower.wakeword.hermes-it — Hermes Italian wake phrase (custom model)

- Decision: **BLOCK_UNTIL_RUNTIME_AVAILABLE** — No verified source exists upstream; BLOCK_UNTIL_RUNTIME_AVAILABLE — honest 'Non ancora usabile' in ordinary UI, download refused; never guessed.
- Model: onnx (openWakeWord-compatible) / pending ; artifact: NONE — no verified source exists upstream
- Runtime: openWakeWord ORT chain (exists) + a TRAINED verified model (does not exist) @ pending (immutable revision: none)
- Coordinates: n/a ; ABIs: arm64-v8a ; minSdk 30
- License: pending (cannot be assumed) (no license assumption; sources read this mission)
- Native deps: n/a
- Pre/post: n/a → n/a
- Memory ≈40 MB ; storage ≈0 MB
- Load: n/a ; Inference: n/a ; Unload: n/a
- Fallback: push-to-talk + continuous listening (physical PASS, preserved) ; Rollback: n/a
- Physical qualification: NOT_APPLICABLE (no artifact)

### org.qwen.qwen2.5-0.5b-instruct.q5-k-m — Qwen2.5 0.5B Instruct (GGUF q5_k_m)

- Decision: **ADD_VERIFIED_ANDROID_RUNTIME** — §9 terminal decision VERIFIED_LOCAL_LLM_RUNTIME_INTEGRATED: llama-android 0.1.1 (Maven Central, MIT, llama.cpp b9878, arm64-v8a, minSdk 24) consumes the EXISTING pinned GGUF — no model replacement required; GGUF stays the compatible artifact.
- Model: gguf / q5_k_m ; artifact: qwen2.5-0.5b-instruct-q5_k_m.gguf (pinned HF commit 9217f5db…, sha256 041474553f…, 522,186,592 B)
- Runtime: dev.ffmpegkit-maintained:llama-android:0.1.1 (Maven Central) — llama.cpp b9878 (submodule 2da6686) prebuilt AAR @ 0.1.1 (immutable revision: llama.cpp b9878 / submodule 2da6686 / Maven 0.1.1)
- Coordinates: dev.ffmpegkit-maintained:llama-android:0.1.1 ; ABIs: arm64-v8a ; minSdk 24
- License: MIT (AAR + llama.cpp) / Apache-2.0 (Qwen model) (no license assumption; sources read this mission)
- Native deps: libllama.so + libggml*.so + libc++_shared.so (bundled by AAR)
- Pre/post: SafePromptBuilder (bounded Italian system context; never Tower truth) → bounded completion text; typed diagnostics on failure
- Memory ≈1024 MB ; storage ≈522 MB
- Load: Llama.loadModel(modelPath, LlamaConfig(contextSize, threads)) ; Inference: Llama.complete(model, prompt, systemPrompt, maxTokens) → LlamaResult(text, tokensPerSecond) ; Unload: Llama.releaseModel(model)
- Fallback: NoVerifiedLlmRuntime typed refusal → route to Tower (TOWER_FIRST/HYBRID) ; Rollback: disable local intelligence; catalog entry removable; model deletion via Remove
- Physical qualification: PHYSICAL_QUALIFICATION_PENDING (Owner device: load latency, token rate, thermal)

### org.hermestower.test.probe-artifact — Hermes Test Probe Artifact

- Decision: **REMOVE_MISLEADING_ENTRY** — Removed from the ordinary catalog presentation (test fixture must not appear as a downloadable product component); retained in the registry for pipeline qualification evidence.
- Model: hermes-probe / none ; artifact: deterministic 4,096 B fixture (sha256 2d717056…527)
- Runtime: none (download-pipeline qualification fixture) @ 1.0.0 (immutable revision: fixture-v1)
- Coordinates: n/a ; ABIs: arm64-v8a ; minSdk 30
- License: Apache-2.0 (no license assumption; sources read this mission)
- Native deps: none
- Pre/post: n/a → n/a
- Memory ≈1 MB ; storage ≈0 MB
- Load: n/a ; Inference: n/a ; Unload: n/a
- Fallback: n/a ; Rollback: n/a
- Physical qualification: NOT_APPLICABLE (test fixture)
