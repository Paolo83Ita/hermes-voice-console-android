# DEVELOPMENT ROADMAP — HERMES VOICE CONSOLE ANDROID

| | |
|---|---|
| Owner | Paolo Lombisani |
| Version | v0.1.0-alpha |
| Date | 2026-10-30 |
| Discipline | One stage at a time; clean, buildable checkpoint at each stage boundary; checkpoint report (NODE/FINDING/ACTION/TEST/FILES/COMMIT/NEXT) in WORK_LOG.md |

## V0.1 — Foundation and Interactive Visual Prototype ✅ (this delivery)

Scope: Stage A documentation set; techno-Greek visual system; animated Tower with truthful 15-state machine; 8 UI surfaces; Marble Light + Obsidian Night; reduced motion; burn-in guard prototype; mock conversation/local/Tower/simulated disconnect/error; text-injection harness instead of microphone.

Exit criteria: quality gates of REQUIREMENTS §9; source package complete; build instructions exact; no fabricated success.

## V0.2 — Device Qualification and Service PoC

- Owner runs DEVICE_QUALIFICATION_CHECKLIST after factory reset; registers updated (assumptions CONFIRMED/REFUTED).
- Foreground service proof-of-concept (microphone type declared; notification; survival across 24 h plugged — Q-20/Q-21).
- Permission-safe microphone capture spike, isolated behind AudioCaptureEngine; privacy mute gates it end-to-end.
- Real DeviceHealthMonitor warnings: charging temperature, thermal status → UI.
- Boot recovery (FR-44): restart to truthful state.

Exit: checklist fully recorded; service survives; capture spike opens/closes the mic honestly.

## V0.3 — Voice On-Device

- System TTS adapter (TextToSpeechEngine) with interruption, captions, rate/volume; MockTTS retired to test harness.
- First STT candidate behind SpeechToTextEngine (whisper.cpp prototype priority; verify, don't assume).
- Wake-word + VAD prototypes (openWakeWord, Silero) behind seams; wake→listen→transcribe real loop on-device.
- Latency instrumentation vs NFR-01 budget; results recorded.

Exit: full voice round trip works on the physical device with at least the fallback chain; numbers recorded.

## V0.4 — Tower Bridge Alpha

- OkHttp/WSS transport adapter implementing TowerBridge; contract tests against a mock Tower server implementing hermes.console.v0.1.
- WorkManager queue flush for REQUEST_QUEUED turns; DISCONNECTED_SAFE full behavior.
- Optional Hilt adoption (prototype decision D-003 review).

Exit: console talks to a mock Tower over WSS with the proposed contract; queue flush proven.

## V0.5 — Pairing and Security Hardening

- Pairing flow per SECURITY_AND_PRIVACY_MODEL §3 (out-of-band code, Keystore device binding).
- SecureConfigurationStore (Keystore-backed); token never displayed; redaction audit.
- TLS verification policy; certificate pinning evaluation.

Exit: a real (Owner-controlled) Tower instance can pair with the console without any secret entering source or logs.

## V0.6 — Local Model Qualification

- Benchmark harness per LOCAL_MODEL_CANDIDATE_PLAN §6; candidates measured on-device; Owner Italian-quality listening test.
- Decision: default model + fallback, or "no local model ships".
- If shipped: load/unload policy, coexistence proof, thermal proof.

Exit: signed qualification report; Local Intelligence screen reflects truth.

## V0.7 — Console Hardening

- Screen rest schedule, black idle mode, real brightness control (FR-41/42).
- Kiosk mode evaluation as controlled capability (FR-46) — explicit Owner decision required.
- Stress: 72 h always-on soak (burn-in guard, thermals, memory).
- Enclosure engineering: vents, speaker port, fit (with thermal deltas measured).

Exit: soak report clean; enclosure design frozen.

## V1.0 — Dedicated Console Release

- Italian localization of UI (A-12); About & Safety finalization; licenses page complete.
- Signed release build strategy (Owner-owned keystore); update procedure documented.
- Final acceptance against PRODUCT_VISION success criteria.

## Standing rules across all stages

- No root, no Tower repository changes, no secrets in source.
- A candidate becomes "qualified" only via recorded on-device evidence.
- Every stage ends with: ZIP archive + checkpoint report + updated registers.
