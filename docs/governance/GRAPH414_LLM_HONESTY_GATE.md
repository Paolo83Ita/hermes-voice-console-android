# GRAPH 4.14 — LLM HONESTY GATE (CP-G4.14-C03)

§4 rule: the llama.cpp Android integration receives NO PASS from AAR presence
alone. Every claim below is test-anchored; physical layers stay typed-PENDING.

## NODE
CP-G4.14-C03 — Local-LLM functional qualification.

## FINDING — required test layers

| Layer | Requirement | State | Evidence |
| --- | --- | --- | --- |
| A | JVM seam test | **VERIFIED** | `LlamaCppAndroidRuntimeTest` (6 tests): bind/generate/unload/idempotent-rebind/typed-load-failure/identity + engine end-to-end; `LocalLlmEngineTest` (10 tests incl. integrity + safe-prompt builder) |
| B | Android native library load test | **LLM_PHYSICAL_QUALIFICATION_PENDING** | no androidTest for llama; `libllama.so`+`libggml*.so` verified PRESENT in the shipped arm64 APK (ZIP listing) — load executes only on the Owner device |
| C | GGUF header + metadata test | **LLM_MODEL_FORMAT_VERIFIED (catalog)** / physical read-back PENDING | format pinned in catalog (GGUF q5_k_m, HF commit 9217f5db…, SHA-256 041474553f…, size pinned); integrity chain (SHA-256 + size) unit-tested; 522 MB artifact not bundled → header read-back happens at Owner-device load |
| D | model load test | **VERIFIED (seam) / PENDING (native)** | seam: `load binds the runtime` test; native: ST-61 chain probe on device |
| E | one safe local prompt | **VERIFIED (seam)** | `engine end-to-end — enabled, integrity OK, load, prompt from model` + SafePromptBuilder test (scope verbatim, control-chars stripped, 512-char cap, 256-token cap) |
| F | generated token or response | **VERIFIED (fixture) / PENDING (native)** | fixture completion text flows with `fromModel=true`; physical generation = Owner device |
| G | timeout handling | **VERIFIED (bounded contract)** | generation bounded by MAX_GENERATION_TOKENS=256 + MAX_INPUT_CHARS=512; caller-side coroutine scope bounds the call; the engine never blocks the UI thread (background-scoped). A dedicated native-call watchdog remains an Owner-device observation item (typed, not assumed) |
| H | cancellation | **VERIFIED (typed)** | `unload()` is safe from any state; syncFromEnabledFlag never tears down a loaded engine (test); generate failure path returns a typed diagnostic (never fabricated text) |
| I | unload | **VERIFIED** | `unload releases the native handle and is idempotent` (no double-release, re-bind verified) |
| J | restart and reload | **VERIFIED (seam)** | re-bind after unload test (`load binds…` + idempotent unload sequence); device restart persistence = Owner retest item 10 |
| K | memory read-back | **LLM_PHYSICAL_QUALIFICATION_PENDING** | requires the device RSS working set of the 0.5B q5_k_m at 2048 ctx |
| L | thermal state | **LLM_PHYSICAL_QUALIFICATION_PENDING** | requires a physical sustained-prompt run |

## Required states (typed)

- LLM_RUNTIME_DEPENDENCY_VERIFIED — llama-android 0.1.1 pinned, AAR sha256
  verified in-sandbox, MIT, arm64-v8a, minSdk 24 < 30.
- LLM_MODEL_FORMAT_VERIFIED — GGUF q5_k_m pinned in the verified catalog
  (no model replacement needed; ADAPT/REPLACE not required).
- LLM_NATIVE_LOAD_VERIFIED — PENDING (APK contains the .so set; on-device
  load evidence belongs to the Owner retest).
- LLM_MODEL_LOAD_VERIFIED — seam yes; native PENDING.
- LLM_FUNCTIONAL_OUTPUT_VERIFIED — seam (fixture) yes; native output PENDING.
- LLM_UNLOAD_VERIFIED — yes (idempotent, tested).
- **LLM_PHYSICAL_QUALIFICATION_PENDING — the terminal state of this gate.**

NOT claimed: LOCAL_LLM_QUALIFIED (needs B/C/D/F/K/L device evidence).

x86_64 handling: the AAR is arm64-only → the x86_64 emulator lane keeps
DETERMINISTIC SEAM TESTS ONLY (typed RUNTIME_ABI_LIMITATION); the arm64
product capability is unaffected and never falsely failed.

UI truth: the Local-experimental profile selectability is keyed to
`LocalLlmEngine.hasVerifiedRuntime` (data-driven — it disables itself
honestly if the runtime disappears); ST-61 reports the chain truth
(dependency/runtime identity/state/integrity/file) with no PASS from catalog
state alone; Diagnostics panel reads the ENGINE (stale hardcoded "runtime
pending" string REMOVED this session).

## ACTION
Verified all layers; recorded the typed states; repaired the stale
diagnostic string; wired ST-61 (AppGraph probe → registry).

## TEST
- 496/496 unit suite incl. LocalLlmEngineTest + LlamaCppAndroidRuntimeTest +
  SelfTestCenterTest (61-definition battery).

## FILES
- app/src/main/java/com/hermestower/voiceconsole/local/{LocalLlmEngine,
  LlamaCppAndroidRuntime,LlamaRealNativeBridge,LlamaNativeBridge}.kt (read;
  LocalLlmEngine selectability comment §25).
- app/src/main/java/com/hermestower/voiceconsole/app/AppGraph.kt (probe +
  diagnostic repair).
- docs/governance/LOCAL_LLM_RUNTIME_DECISION.md (unchanged decision).

## EVIDENCE
- Test class list + APK native-lib listing (measured).
- ST-61 evidence string template (registry).

## CHECKPOINT
CP-G4.14-C03 — LLM functional qualification: DONE at
**LLM_PHYSICAL_QUALIFICATION_PENDING** (honest terminal boundary).

## NEXT
CP-G4.14-C04 — Bridge truth final check + Marble route matrix + contrast.
