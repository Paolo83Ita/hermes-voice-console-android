# LOCAL LLM RUNTIME DECISION (Graph 4.14, CP-G4.14-005)

Terminal decision required by charter §9. The prior state
(CANDIDATE_PENDING_DISCOVERY without a terminal investigation) is closed.

## VERDICT: VERIFIED_LOCAL_LLM_RUNTIME_INTEGRATED

**Runtime adopted**: `dev.ffmpegkit-maintained:llama-android:0.1.1` (Maven
Central) — a maintained prebuilt llama.cpp JNI/AAR integration.

**Model**: the EXISTING catalog GGUF
`org.qwen.qwen2.5-0.5b-instruct.q5-k-m` (pinned HF commit 9217f5db…, SHA-256
041474553f…, Apache-2.0) — **GGUF stays the compatible artifact; no model
replacement required** (llama.cpp consumes GGUF natively). Italian capability
preserved (Qwen2.5 Instruct is multilingual; the safe-prompt contract carries
the resolved locale tag).

## Fresh runtime comparison (charter §9 dimensions)

| Option | Format | arm64-v8a | minSdk | CPU | GPU/NPU | RAM (0.5B) | License | Maintenance | Integration quality | Verdict |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| **llama.cpp via llama-android 0.1.1 (Maven Central AAR)** | **GGUF (native)** | yes (jni/arm64-v8a, 16KB pages ready) | 24 | yes (libggml-cpu) | gpuLayers param (0 on baseline) | ≈1 GB incl. context | MIT (+MIT llama.cpp) | actively maintained (release 2026-07-06, commits through 2026-09-15) | facade API loadModel/complete/releaseModel; typed exceptions; real libllama/libggml .so verified in AAR | **ADOPTED** |
| LiteRT-LM (Google AI Edge) | .litertlm (LiteRT format) | yes | 24+ | yes | NPU path | low | Apache-2.0 | production-ready 2026 | GGUF NOT compatible → model replacement + conversion pipeline; heavier | rejected for this graph (format incompatible; conversion not verifiable end-to-end in-sandbox) |
| MediaPipe GenAI tasks (com.google.mediapipe:tasks-genai) | .task/.bin | yes | 24 | yes | yes | medium | Apache-2.0 | maintained | supported model set (Gemma/Phi/…) does NOT include Qwen2 → replacement required | rejected (same reason) |
| MLC LLM Android | MLC-compiled weights | yes | 24+ | yes | WebGPU/Vulkan | medium | Apache-2.0 | maintained | requires per-device model compilation + runtime weight download from HF; no simple immutable Maven AAR; heaviest integration | rejected (integration risk + unverifiable artifact chain) |
| ONNX Runtime GenAI | ONNX | yes | 24+ | yes | ORT EPs | medium | MIT | maintained | Qwen2 supported, but the Android package is distributed as GitHub-release AARs (not Maven Central) and the model must be converted GGUF→ONNX shards (multi-artifact provenance chain) | rejected for this graph (more moving parts than the verified GGUF path) |
| VERIFIED_NO_SAFE_LOCAL_LLM_RUNTIME_AVAILABLE | — | — | — | — | — | — | — | — | — | **NOT the case**: a safe, verified, maintained runtime exists |

## Verification evidence (charter §3 sources, this mission)

- Maven Central POM read: `dev.ffmpegkit-maintained:llama-android:0.1.1`,
  MIT, upstream `github.com/ffmpegkit-maintained/llama-android`.
- AAR downloaded from repo1.maven.org and inspected in-sandbox:
  - SHA-256 `fdccf04c77de470004d7e54679bb918074ee9dbf31ea75af8c014d19c31e7501`;
  - 2,062,204 bytes; `jni/arm64-v8a` with real native libraries
    (libllama.so 2.76 MB, libggml*.so, libllama_jni.so, libc++_shared.so);
  - `classes.jar` exposes `dev.ffmpegkit.llama.{Llama, LlamaModel,
    LlamaConfig, LlamaResult, LlamaException, LlamaJNI}`;
  - AndroidManifest minSdk 24 ≤ project minSdk 30.
- Upstream README/source: llama.cpp build **b9878** (submodule 2da6686);
  API `Llama.loadModel(modelPath, LlamaConfig)` / `Llama.complete(model,
  prompt, systemPrompt, maxTokens)` → `LlamaResult(text, tokensGenerated,
  tokensPerSecond, promptEvalTimeMs, generateTimeMs)` /
  `Llama.releaseModel(model)`; suspend API on the library's own dispatcher.
- Model: HF LFS metadata SHA-256 for the pinned commit (already in the
  catalog since 2026-11-02).

## Integration (in-tree at CP-G4.14-002/003/005 integration commit f6d5cd6)

- Gradle: `libs.versions.toml` llamaAndroid = "0.1.1"; `app/build.gradle.kts`
  `implementation(libs.llama.android)` (pinned; documented in
  DEPENDENCY_PINNING.md discipline).
- `local/LlamaNativeBridge.kt` — typed seam (config/handle/completion/
  LlamaNativeException).
- `local/LlamaRealNativeBridge.kt` — the ONLY class touching
  `dev.ffmpegkit.llama` (same discipline as SherpaRealNativeBridge).
- `local/LlamaCppAndroidRuntime.kt` — `LlmRuntime` implementation: idempotent
  load/unload, typed failures (native-load → LOAD_FAILED, never a fake
  RUNTIME_UNAVAILABLE), verbatim runtime identity for diagnostics/receipts.
- `AppGraph` injects the real runtime into `LocalLlmEngine` (default OFF;
  enable is an explicit Owner action; integrity chain unchanged; never
  auto-loaded; never Tower authority).
- `NoVerifiedLlmRuntime` retained as the typed fallback for genuinely
  unavailable-runtime paths.
- Catalog updated: `runtimeRequirement` → llama-android 0.1.1 INTEGRATED;
  limitations updated (integration truth + pending on-device qualification).
- Tests: `LlamaCppAndroidRuntimeTest` (6) — fixture bridge proves bind,
  bounded generation, idempotent unload (no double-release), typed load
  failure ≠ runtime-unavailable, LLAMA_NOT_BOUND, identity truth, plus the
  engine end-to-end (enabled → integrity OK → LOADED → fromModel reply →
  unload). Unit suite 491/491 GREEN at the checkpoint.

## What remains honest

- On-device load latency / token rate / thermal / persistence:
  **PHYSICAL_QUALIFICATION_PENDING** (Owner device) — recorded typed
  PENDING_DEVICE in MODEL_RUNTIME_END_TO_END_RECEIPT.json. The executor does
  not fabricate device measurements.
- The stale incompatible-entry concern: none — the GGUF is compatible; the
  catalog entry stays with updated provenance (no removal needed).
