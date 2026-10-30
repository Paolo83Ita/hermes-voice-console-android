# VOICE PIPELINE OPTIONS — HERMES VOICE CONSOLE ANDROID

| | |
|---|---|
| Owner | Paolo Lombisani |
| Version | v0.1.0-alpha |
| Date | 2026-10-30 |
| Status | For Owner review |
| Decision codes | USE_FOR_FOUNDATION · PROTOTYPE · QUALIFY_ON_DEVICE · DEFER · REJECT |

Rule from the mission: **no candidate is claimed to work until built and tested on the physical Xiaomi 11 Lite 5G.** Every slot below therefore has a foundation choice (what V0.1/V0.3 code is written against) and a qualification queue (what must pass on-device before it becomes default).

## 1. Pipeline stages and latency budget (local path, targets)

| Stage | Target (local path) | Notes |
|---|---|---|
| Wake-word detection | ≤ 300 ms after speech onset | Streaming, low-power |
| VAD end-of-speech | ≤ 500 ms trailing silence | Balances cutoff vs latency |
| STT to final transcript | ≤ 1.2 s after end-of-speech (short utterances) | Partial streaming preferred |
| Router decision | ≤ 50 ms (rule-based) / ≤ 1.5 s (LLM-assisted, optional) | LLM path optional |
| TTS first audio | ≤ 400 ms after response ready | Sentence streaming |
| **Total wake → first audio** | **≤ 2.5 s** | NFR-01 |

## 2. Wake-word engine

| Candidate | License / model | Android story | Decision | Qualification criteria |
|---|---|---|---|---|
| openWakeWord | Apache-2.0, ONNX/tflite | Community Android ports; self-trained custom "Hermes" model feasible | **PROTOTYPE → QUALIFY_ON_DEVICE** | False-wake ≤ 1/12 h idle console; detection ≤ 300 ms; CPU ≤ 10 % one core; Italian custom phrase |
| Porcupine (Picovoice) | Proprietary SDK, free tier limits | First-class Android support, many platforms | **QUALIFY_ON_DEVICE** (license review for always-on dedicated device) | Same metrics + license acceptance by Owner; compare against openWakeWord |
| Android SoundTrigger DSP | Vendor/HAL dependent | Only if OEM exposes; MIUI unknown | **DEFER** | Only if HAL service present on device |
| Snowboy | Dead project | — | **REJECT** | Unmaintained |

## 3. Voice activity detector

| Candidate | License | Decision | Qualification criteria |
|---|---|---|---|
| Silero VAD (ONNX) | MIT | **QUALIFY_ON_DEVICE** | Frame accuracy on Italian utterances; ≤ 2 % CPU; ONNX Runtime footprint |
| WebRTC VAD | BSD | **PROTOTYPE** (lightweight fallback) | Aggressiveness tuning; error rate vs Silero |
| Android built-in speech silence | API-dependent | **DEFER** | Only if SpeechRecognizer path chosen |

## 4. Speech-to-text

| Candidate | License | Decision | Qualification criteria |
|---|---|---|---|
| whisper.cpp (ggml, tiny/base/small int8) | MIT; models OpenAI Whisper (MIT) | **QUALIFY_ON_DEVICE** — primary candidate | WER on Italian test set ≤ baseline; small-model first-token ≤ 1.2 s; RAM ≤ 500 MB (small int8); thermals within NFR-07 |
| Android SpeechRecognizer (on-device service) | System component | **PROTOTYPE** as functional fallback; **REJECT for LOCAL mode default** unless verified fully on-device (privacy: may route via Google) | Verify `isOnDeviceSpeech` availability/behavior on MIUI |
| Vosk small-it | Apache-2.0 | **PROTOTYPE** alternative (streaming, low RAM) | WER vs whisper; streaming UX |
| Cloud STT | — | **REJECT** for V-line (privacy + offline requirement) | — |

## 5. Text-to-speech

| Candidate | License | Decision | Qualification criteria |
|---|---|---|---|
| Android system TTS (MIUI/TTS engine) | System | **USE_FOR_FOUNDATION** (V0.3 fallback; abstraction live in V0.1) | Italian voice quality audible test; latency ≤ 400 ms first audio; interruptibility |
| Piper | MIT, onnxruntime | **QUALIFY_ON_DEVICE** — primary quality candidate | Italian voice naturalness (Owner judgment), RAM ≤ 300 MB, first audio ≤ 400 ms, barge-in clean |
| Kokoro | Apache-2.0 | **DEFER** | Size/ONNX runtime behavior on mobile unproven; revisit after Piper results |
| eSpeak NG | GPL | **REJECT** for identity voice (robotic; license friction) | — |

Voice identity note: the target voice is calm, authoritative, warm, Italian-first. Selection is an Owner listening test with ≥ 3 finalists; no vendor is presumed.

## 6. Local LLM runtime

| Candidate | License | Decision | Qualification criteria |
|---|---|---|---|
| llama.cpp-based Android runtime (JNI binding) | MIT | **PROTOTYPE → QUALIFY_ON_DEVICE** | See LOCAL_MODEL_CANDIDATE_PLAN.md matrix |
| MLC LLM Android | Apache-2.0 | **DEFER** | Evaluate GPU path later; thermal unknown |
| ONNX Runtime Generative AI | MIT | **DEFER** | Model-format ecosystem check first |
| Google AICore / Play services | Proprietary | **REJECT** | Dedicated-device control + privacy; unavailable guarantee on MIUI |

## 7. Transport and platform libraries (foundation decisions)

| Concern | Choice | Decision | Rationale |
|---|---|---|---|
| Language / UI | Kotlin 2.0.x + Jetpack Compose (Material 3 only where compatible with the custom identity) | **USE_FOR_FOUNDATION** | Declarative UI fits the animated Tower; M3 components adopted selectively under the Hermes theme |
| HTTP client (V0.4) | OkHttp | **USE_FOR_FOUNDATION** | Maintained, TLS hygiene, WebSocket support |
| Serialization | kotlinx-serialization | **USE_FOR_FOUNDATION** | Contract models shared with tests |
| Local state (V0.3+, if justified) | Room | **USE_FOR_FOUNDATION (conditional)** | Only when turn/event persistence leaves the session scope |
| Foreground service | Android foreground service, microphone type | **USE_FOR_FOUNDATION** (V0.2) | MIUI survival strategy in checklist |
| DI | Manual service locator (`AppGraph`) | **USE_FOR_FOUNDATION (V0.1)** → Hilt **PROTOTYPE** at V0.4 | Keep un-compiled surface minimal now |
| Background scheduling | WorkManager | **USE_FOR_FOUNDATION** (queue flush V0.4) | Doze-aware retries |
| Keystore | Android Keystore | **USE_FOR_FOUNDATION** (V0.5) | Device binding requirement |

## 8. Noise suppression

| Candidate | Decision | Criteria |
|---|---|---|
| WebRTC audio processing (AEC/NS) | **PROTOTYPE** | Improvement on Italian STT WER in enclosure noise |
| Simple high-pass + AGC | **USE_FOR_FOUNDATION (V0.3 stopgap)** | Cheap, predictable |

## 9. Sequencing

1. V0.1: abstractions + mock/simulated pipeline (this delivery).
2. V0.2: device qualification program runs the checklist; mic capture spike behind permission-safe isolation.
3. V0.3: system TTS + first STT candidate integrated behind seams; wake/VAD prototypes.
4. V0.4: real transport adapter + WorkManager queue flush.
5. V0.6: STT/TTS/LLM finalists measured with the benchmark matrix; winners become defaults; losers documented with numbers.
