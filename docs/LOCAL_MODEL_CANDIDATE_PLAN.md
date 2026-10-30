# LOCAL MODEL CANDIDATE PLAN — HERMES VOICE CONSOLE ANDROID

| | |
|---|---|
| Owner | Paolo Lombisani |
| Version | v0.1.0-alpha |
| Date | 2026-10-30 |
| Status | For Owner review |

## 1. Role of the local model (bounded, non-authoritative)

The local model is a **conversational front-end**, never a second Tower authority. Permitted roles:

1. Conversational front-end and short small-talk responses
2. Intent detection support
3. Command cleanup (normalizing spoken phrasing)
4. STT correction when confidence is sufficient
5. Lightweight semantic routing (assisting the IntentRouter)
6. Summarization of a voice turn before forwarding to the Tower
7. Local help and device-status explanations
8. Graceful offline responses in DISCONNECTED_SAFE

It is forbidden from: consequential actions, mission reasoning, receipts, approvals, or any output presented as Tower truth. The UI always labels local-model answers as LOCAL.

## 2. Current status (honest)

**V0.1: no model is downloaded, loaded, or qualified.** The Local Conversation engine seam is served by `MockLocalConversationEngine` (deterministic canned responses). The Local Intelligence screen shows state NOT_LOADED and candidate placeholders with qualification status "not qualified". Load/unload controls are present but disabled until V0.6.

## 3. Candidate classes (evaluation set — not endorsements)

Candidate classes: compact quantized instruct models, approximately **0.5B–3B parameters**, GGUF (llama.cpp runtime) or equivalent.

| Class | Example candidates | License (verify text at adoption time) | Notes for Italian |
|---|---|---|---|
| ~0.5B instruct | Qwen2.5-0.5B-Instruct | Apache-2.0 | Fastest; weakest reasoning; Italian fair |
| ~1B instruct | Llama-3.2-1B-Instruct | Llama Community License (terms/footnote restrictions to verify) | Good multilingual incl. Italian |
| ~1.5–1.7B instruct | Qwen2.5-1.5B-Instruct; SmolLM2-1.7B-Instruct | Apache-2.0 | Best speed/quality trade-off candidates |
| ~2–3B instruct | Qwen2.5-3B-Instruct; Gemma-2-2b-it | Apache-2.0 / Gemma Terms | Quality ceiling; RAM/thermal risk highest |
| ~3.8B | Phi-3.5-mini | MIT | Likely above RAM/thermal comfort on 780G; include only as stretch |

Model identity recording requirement: every candidate entry records exact repo id, revision hash, license text date, GGUF quantization variant(s), and file size — before first on-device run.

## 4. Runtime candidates

| Runtime | Format | Decision |
|---|---|---|
| llama.cpp-based Android runtime (JNI) | GGUF | **PROTOTYPE → QUALIFY_ON_DEVICE** (primary) |
| MLC LLM (Android) | MLC weights | **DEFER** |
| ONNX Runtime GenAI | ONNX | **DEFER** |

## 5. Qualification matrix (every candidate must be measured, not assumed)

| Dimension | Metric / method | Pass guideline (initial; tunable after V0.2 baseline) |
|---|---|---|
| Exact identity | Repo id + revision + quant recorded | Mandatory |
| License | License text captured; commercial compatibility confirmed | Mandatory, Owner sign-off |
| Format / quantization | GGUF Q4_K_M / Q8_0 variants recorded | Q4 default, Q8 for quality A/B |
| RAM usage | Peak RSS with model resident (`dumpsys meminfo`) | 0.5B ≤ 600 MB; 1–1.7B ≤ 1.4 GB; 2–3B ≤ 2.6 GB |
| Application size delta | APK/AAB size before/after | ≤ 2.5 GB storage used total with one model |
| Cold-load time | Page-cache-cold load to first token | ≤ 4 s target |
| Warm latency | Time to first token, warm | ≤ 1.5 s target |
| Throughput | Tokens/s steady | ≥ 8 tok/s for conversational use |
| Italian comprehension | Fixed 20-utterance Italian set, Owner-rated ≥ 4/5 | Mandatory |
| Instruction following | Fixed 15-instruction set success ≥ 80 % | Mandatory |
| Thermal behavior | 10-min loop, sustained temperature | Surface ≤ 42 °C in enclosure |
| Battery/charging temp | Same loop while charging | ≤ 40 °C battery temp, throttling documented |
| Coexistence with STT+TTS | Wake→STT→LLM→TTS pipeline run | No OOM; round trip within NFR-01 + LLM budget |
| Unload and recovery | Unload → reload ×10 | No leak (RSS returns to baseline ±5 %) |
| Commercial compatibility | License audit vs intended use | Owner sign-off |

## 6. Benchmark protocol (V0.6)

1. Freeze the harness: scripted prompt sets (Italian), timing harness, `dumpsys` capture.
2. Measure each candidate three times (cold/warm/hot device state).
3. Log raw numbers into a qualification report committed next to this plan; no verbal claims.
4. Owner listening/rating session for Italian quality.
5. Decision meeting: pick one default + one fallback, or decide "no local model ships" if no candidate passes — the console remains fully functional without it (router falls back to rule-based decisions only).

## 7. Storage and download policy

- No model artifacts are downloaded during the architecture phase (V0.1–V0.4). This is a mission rule.
- Models are side-loaded onto the device via the Owner's workstation over USB into app-private storage; no broad storage permission is requested.
- Exactly one resident model at a time; unload timer returns the console to the no-model baseline on idle.
