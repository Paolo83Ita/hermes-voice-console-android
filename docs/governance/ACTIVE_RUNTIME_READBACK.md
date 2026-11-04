# ACTIVE RUNTIME READBACK — Graph 4 FINAL (§13)

Directive: GRAPH 4 FINAL IMPLEMENTATION CONTINUATION §13 ("Installation files are
not proof"). This document is the runtime-evidence ledger: what is ACTIVELY
running inside the application, read from the live engines — never from
catalog aspiration. Updated at every evidence change.

## 1. In-app safe Diagnostics view (implemented 2026-11-04)

`DiagnosticsScreen` now carries the **Active runtime** card (strings ×5 locales)
bound to `AppGraph.activeRuntimeReadback`, a live snapshot read from the engine
objects at access time:

| Row | Source | Values |
| --- | --- | --- |
| Speech recognition in use | `VoskFrameSttEngine.engineId` / system fallback | `vosk-android 0.3.75 + vosk-model-small-it-0.22` when the model is installed and loaded; `Android system recognizer (offline vosk model not installed)` otherwise |
| Offline model state | `VoskFrameSttEngine.state` | NOT_INSTALLED / LOADING / READY / TRANSCRIBING / FINALIZING / FAILED / UNLOADED (+ loaded dir name) |
| Voice output in use | `AndroidSystemTtsEngine.activeEngineName` (captured at init from `TextToSpeech.getDefaultEngine()`) | `Android system TTS (<engine package>)` |
| Local intelligence | truthful constant | `No local model (default) — Qwen2.5-0.5B candidate present, llama.cpp runtime pending` |
| Inference runtime | build fact | `onnxruntime-android 1.20.0 (Gradle)` |

## 2. STT — REAL offline path (§14)

| Contract item | State |
| --- | --- |
| Authoritative source | alphacephei.com (frozen archive URL in the shipped catalog, `org.vosk.model.small-it-0.22`) |
| Runtime | `com.alphacephei:vosk-android:0.3.75` — **ADDED to gradle this wave** (Maven Central, Apache-2.0, AAR with arm64-v8a/armv7/x86/x86_64) |
| Consumer | `VoskFrameSttEngine` implements the real `SttEngine` frame seam (AudioFrame 16 kHz PCM → LE bytes → `Recognizer.acceptWaveForm` → typed partial/final JSON) wired into `VoicePipelineController` |
| Model presence gate | NOT_INSTALLED until the download→verify→install chain places the model under `filesDir/models/stt/vosk-small-it` (dir resolver tolerates the zip's single-level unzip) |
| Active-engine identity | NEVER claims local STT while the system recognizer is the active engine (directive §14 prohibition) |
| Fallback | Android system `SpeechRecognizer` (unchanged, active by default) |
| Runtime proof (real transcription) | **PENDING DEVICE/EMULATOR EXECUTION** — requires the installed model + real microphone input; recorded here when it lands |
| Unload | `unload()` closes recognizer+model → UNLOADED (implemented) |

## 3. TTS — honest delivery state (§15)

| Contract item | State |
| --- | --- |
| Downloadable Italian voices (REAL files, verified SHA-256, revision-bound) | `vits-piper-it_IT-riccardo-x_low` (26,496,614 B) · `vits-piper-it_IT-paola-medium` (67,221,173 B) — download/verify/install/remove chain functional |
| Consuming runtime | **sherpa-onnx Android runtime — PENDING INTEGRATION** (no authoritative Maven artifact; pre-built AAR ships via k2-fsa GitHub releases). The catalog previously claimed `RUNTIME_BUNDLED / pinned in gradle` — that claim was **FALSE and has been corrected** to CANDIDATE_PENDING_DISCOVERY (§13 honesty correction, catalog diff of this wave) |
| ACTIVE TTS path | Android system TTS engine (fallback chain tier 2) — real synthesis, real interruption, honest SPEAKING state; engine package visible in Diagnostics |
| Fallback chain | 1. qualified downloaded voice (pending runtime) → 2. Android offline system TTS (**active**) → 3. text only |
| Runtime proof (selected downloaded voice actually used) | **PENDING** the sherpa-onnx integration + physical audio qualification |

## 4. Optional local LLM (§16)

| Contract item | State |
| --- | --- |
| Candidate | `Qwen2.5-0.5B-Instruct-GGUF` q5-k… (522,186,590 B, commit-pinned revision, Apache-2.0) — real, immutable, arm64-v8a-compatible file |
| Runtime | llama.cpp Android — **CANDIDATE_PENDING_DISCOVERY** (no runtime integrated; no invented claims) |
| Default | **No local model** (default state, truthful in Diagnostics) |
| State name | `LOCAL_LLM_IMPLEMENTED_PHYSICAL_QUALIFICATION_PENDING` once the download/install path is exercised; NEVER `LOCAL_LLM_QUALIFIED` before physical inference |

## 5. Bundled foundations (§13)

| Component | Runtime | Truthful label |
| --- | --- | --- |
| openWakeWord trio (melspec/embed/hey_jarvis) | onnxruntime-android 1.20.0 (Gradle) | Included with the application |
| GTCRN denoiser | onnxruntime-android 1.20.0 via the app's own `GtcrnDenoiserEngine` (real ORT inference — the catalog's earlier "sherpa-onnx denoiser API" claim was false and is corrected) | Included with the application |
| sherpa-onnx TTS runtime | **NOT BUNDLED** (corrected this wave) | Pending integration |

The Model Sanctum attribute sheet now renders "Included with the application"
(mm_included_label, ×5 locales) for RUNTIME_BUNDLED-classified entries instead of
a download-style state.

## 6. Evidence log (append-only)

- 2026-11-04 — runtime seams implemented (vosk-android 0.3.75 gradle pin,
  VoskFrameSttEngine wired, activeEngineName read-back, Diagnostics card ×5,
  catalog honesty corrections). Commit: (this wave). No physical inference
  evidence yet — nothing is claimed qualified.
- 2026-11-04 (Graph 4.6, CP-G4-022) — OPTIONAL LOCAL LLM implemented at the seam:
  `LocalLlmEngine` (states DISABLED→NOT_INSTALLED→LOADING→LOADED/ERROR, typed
  diagnostics, safe-prompt builder, hash-gated delivery reuse) +
  `NoVerifiedLlmRuntime` (the ONLY shipped runtime — every call fails typed
  LLM_RUNTIME_PENDING_VERIFIED_SOURCE; the engine NEVER invents a completion) +
  `ConsoleSettings.localLlmEnabled` DEFAULT FALSE persisted through
  SettingsRepository + Local Intelligence card (state/runtime/diagnostics rows,
  enable switch, Load/Unload buttons) ×5 locales (652 keys). Generation stays
  typed-blocked until a verified Android llama.cpp runtime is adopted.
  STT/TTS receipts delivered (docs/receipts/): vosk-android AAR re-verified LIVE
  on Maven Central (sha256 ab2f8b91…, ABI-complete) and the Italian model URL
  re-verified LIVE (HTTP 200, 49,665,141 B = catalog-exact).
