# GRAPH 4.14 — FINAL RUNTIME AUDIT (CP-G4.14-C02)

Source-and-dependency audit of every integrated runtime component performed on
the fresh tip (d66a523 + this continuation's repairs) BEFORE packaging.
Evidence class: code + dependency coordinates + measured APK contents —
never narrative.

## NODE
CP-G4.14-C02 — Runtime integration final audit (§3).

## FINDING — per-component audit table

| # | Component | Catalog identity | Runtime identity (code) | Dependency coordinates (immutable) | License | arm64-v8a | x86_64 | minSdk | Model format | Load seam | Unload seam | Functional test | Fallback | UI status truth | No false ACTIVE |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | Vosk Italian STT | org.vosk.model.small-it-0.22 | VoskFrameSttEngine → real `Recognizer` | com.alphacephei:vosk-android:0.3.75 (Maven Central) | Apache-2.0 | libvosk.so 10.04 MB ✓ | libvosk.so ✓ | 24 (dep) < 30 (app) | vosk-model-small-it (zip, SHA-256 chain) | `VoskFrameSttEngine.load()` (filesDir/models/stt/vosk-small-it) | engine release | ST-53 + ST-55 (fixture leg; mic leg = Owner) | Android system recognizer (typed) | runtime resolved → CHECK_RUNTIME | ✓ ACTIVE only via ladder |
| 2 | sherpa-onnx TTS engine | org.sherpa-onnx.tts.runtime | SherpaRealNativeBridge (OfflineTts) | com.github.k2-fsa:sherpa-onnx:v1.12.24 (JitPack tag, AAR verified) | Apache-2.0 | jni+c-api+cxx-api ≈ 9.8 MB ✓ | ✓ | 21 (dep) < 30 | piper bundles (espeak-ng-data) | AppGraph wiring (2 per-voice engines) | engine release | ST-56/ST-57 probes; JVM typed PENDING | SYSTEM TTS route (TtsRouteSwitch) | routedNaturalVoiceId only when qualified | ✓ |
| 3 | Piper Riccardo | org.piper.voice.it-riccardo-x-low | sherpa voice A (engine wired L386-403) | (delivery via catalog, sha-pinned) | see bundle | via sherpa ✓ | via sherpa ✓ | — | .onnx voice bundle | sherpa load | sherpa release | ST-56 (synthesis + playback + read-back) | SYSTEM TTS | ACTIVE only after ladder | ✓ |
| 4 | Piper Paola | org.piper.voice.it-paola-medium | sherpa voice B | (same) | same | via sherpa ✓ | via sherpa ✓ | — | .onnx voice bundle | sherpa load | sherpa release | ST-57 | SYSTEM TTS | same | ✓ |
| 5 | GTCRN denoiser | org.gtcrn.speech-enhancement.simple | GtcrnDenoiserEngine (real streaming ORT) | com.microsoft.onnxruntime:onnxruntime-android:1.20.0 | MIT | libonnxruntime.so 17.57 MB ✓ | ✓ | 21 (dep) < 30 | gtcrn_simple.onnx (bundled 0.54 MB; downloadable verified) | OrtOnnxRunner(file) | engine UNLOADED/BYPASSED | ST-59 (REAL streaming frames; fixture) | typed-OFF (never fake denoise) | installed-vs-active distinction | ✓ |
| 6 | Silero VAD | org.silero.vad.v5 | SileroVadEngine (real ORT graph; IO verified) | onnxruntime-android 1.20.0 (seam) | MIT | ✓ | ✓ | — | silero_vad.onnx (downloadable, SHA-pinned) | OrtOnnxRunner(filesDir/models/vad/silero) | engine release | ST-58 (REAL inference on Italian fixture) | EnergyVadEngine (typed fallback) | ENERGY fallback never reported as Silero | ✓ |
| 7 | mel frontend | org.openwakeword.model.melspectrogram | RealMelEmbedFrontend (ORT chain) | onnxruntime-android 1.20.0 | MIT | ✓ | ✓ | — | melspec.onnx (bundled 1.09 MB) | OrtOnnxRunner | — | OpenWakeWordEngineTest | typed-OFF | engine dependency only | ✓ |
| 8 | embedding | org.openwakeword.model.embedding | RealMelEmbedFrontend (ORT chain) | same | MIT | ✓ | ✓ | — | embed.onnx (bundled 1.33 MB) | same | — | same | typed-OFF | engine dependency only | ✓ |
| 9 | wake word (hey-jarvis) | org.openwakeword.wakeword.hey-jarvis | OpenWakeWordEngine (real 3-graph chain) — OFF by default | same | MIT | ✓ | ✓ | — | hey_jarvis.onnx (bundled 1.27 MB) | enable() only | disable | OpenWakeWordEngineTest | InactiveWakeWordEngine (default) | ST-60 typed OFF_BY_DEFAULT_QUALIFICATION_PENDING_FA_FR_HARNESS | ✓ |
| 10 | Italian wake word | org.hermestower.wakeword.hermes-it | none — no verified source exists | — | — | — | — | — | — | — | — | — | — | CANDIDATE_PENDING_DISCOVERY; download blocked (SOURCE_UNVERIFIED_DOWNLOAD_BLOCKED) | ✓ |
| 11 | Qwen 0.5B GGUF (local LLM) | org.qwen.qwen2.5-0.5b-instruct.q5-k-m | LlamaCppAndroidRuntime + LlamaRealNativeBridge | dev.ffmpegkit-maintained:llama-android:0.1.1 (Maven Central, AAR sha256 verified in-sandbox) | MIT (runtime) / Apache-2.0 (model) | libllama+ggml ≈ 4.25 MB ✓ | native llama ABSENT (AAR arm64-only) — typed RUNTIME_ABI_LIMITATION, emulator lane keeps deterministic seam tests | 24 (dep) < 30 | GGUF q5_k_m (pinned HF commit 9217f5db…, SHA-256 041474553f…) | LlamaNativeBridge.loadModel (2048-ctx, bounded) | LlamaNativeBridge.release (idempotent) | LlamaCppAndroidRuntimeTest (seam, 6 tests) + ST-61 chain truth | NoVerifiedLlmRuntime (typed refusal) | selectability keyed to runtime INTEGRATION truth | ✓ |
| 12 | probe artifact | org.hermestower.test.probe-artifact | none (deterministic fixture) | — | — | — | — | — | — | — | — | — | — | hidden from ordinary UI (§25 of the 4.14 charter; probe filter in Model Library) | ✓ n/a |

APK contents re-measured from the SHIPPED arm64 APK (this build):
17 native libs, 49.1 MB total (onnxruntime 17.57, vosk 10.04, barhopper 4.95,
sherpa jni+c+cxx 9.84, llama+ggml 4.25, c++_shared 1.29, ort4j_jni 0.92,
jnidispatch 0.18). Bundled model assets 4.23 MB (embed 1.33 / hey_jarvis 1.27 /
melspec 1.09 / gtcrn_simple 0.54) + ML Kit barcode tflite 0.88 MB + fixtures.

## ACTION
- All 11 auditable components verified against catalog identity, dependency
  pins (`gradle/libs.versions.toml` + `docs/DEPENDENCY_PINNING.md`), ABI truth
  (measured from the APK ZIP), minSdk compatibility (app minSdk 30 > every
  dep minSdk), model formats, load/unload seams, functional tests, typed
  fallbacks and no-false-ACTIVE guarantees.
- x86_64: llama native absent → typed limitation recorded (§4 rule); the
  emulator lane keeps the deterministic fixture/seam tests; the arm64 product
  capability is NOT downgraded by the emulator fact.

## TEST
- 496/496 unit tests (incl. the 6 llama seam tests, Silero/GTCRN engine
  tests, OpenWakeWordEngineTest, ST-58..61 registration).
- `:app:assembleDebug` SUCCESS (all three variants; ZIP integrity verified).

## FILES
- docs/governance/ANDROID_RUNTIME_REQUIREMENT_MATRIX.{json,md} (unchanged —
  the matrix was NOT recreated, verified accurate at the tip).
- app/build.gradle.kts + gradle/libs.versions.toml (pins read, unchanged).

## EVIDENCE
- APK ZIP listing (lib/ ABI dirs, per-lib sizes) — measured this session.
- aapt2 badging: versionCode 10, versionName
  0.3.7-alpha-graph4.14-runtime-theme-tower, minSdk 30, targetSdk 35.

## CHECKPOINT
CP-G4.14-C02 — Final runtime audit: DONE (all feasible integrations complete;
terminal decisions hold).

## NEXT
CP-G4.14-C03 — LLM functional qualification (honesty gate states).
