# RISK REGISTER — HERMES VOICE CONSOLE ANDROID

| | |
|---|---|
| Owner | Paolo Lombisani |
| Version | v0.1.0-alpha |
| Date | 2026-10-30 |
| Review cadence | At every stage checkpoint and after every on-device qualification session |

Likelihood / Impact: L (low), M (medium), H (high).

| ID | Risk | L | I | Mitigation | Early warning sign | Owner |
|---|---|---|---|---|---|---|
| R-01 | MIUI kills or throttles the foreground voice service despite charging | H | H | Foreground service (V0.2) + MIUI autostart allowance + battery-saver exception + lock-in-recents; checklist Q-10…Q-12 | Service onDestory logs; wake failures after idle | PL |
| R-02 | AMOLED burn-in from static console layout over months | H | M | Burn-in guard drift, dark default theme, periodic pixel-shift, screen rest schedule (V0.2), brightness ceiling | Visible ghosting of status strip | PL |
| R-03 | Thermal: charging + sustained audio pipeline in sealed 3D-printed enclosure | M | H | Thermal monitor + charging-temperature warning (FR-43); animation intensity reduction on MODERATE thermal status; enclosure vents in design | Battery temp > 40 °C at idle-charging | PL |
| R-04 | whisper.cpp too slow on Snapdragon 780G for acceptable latency | M | H | Candidate ladder tiny→base→small int8; Vosk fallback prototype; budget gates in VOICE_PIPELINE_OPTIONS §1 | Small-model first-token > 1.2 s | PL |
| R-05 | RAM pressure: LLM + STT + TTS coexistence on 8 GB shared with MIUI | M | H | One-resident-model policy; unload timer; NFR-09 caps; coexistence test in model matrix | Low-memory kill events in logcat | PL |
| R-06 | MIUI Italian system TTS quality/latency inadequate | M | M | Piper as qualified candidate; voice selection stays configurable | Owner listening test fails fallback voice | PL |
| R-07 | No local LLM passes the qualification matrix | M | L | Console designed fully functional without local model; router falls back to rules; decision gate allows "no model ships" | Benchmarks below pass guideline | PL |
| R-08 | Tower API contract churn after V0.4 | M | M | Versioned envelope; adapter seam; contract tests; assumption A-20 tracked | Contract diff on real spec arrival | PL |
| R-09 | Keystore/pairing friction after factory resets or MIUI specifics | M | M | Pairing designed as repeatable, documented, fast flow; reset playbook in SECURITY_AND_PRIVACY_MODEL §8 | Pairing failures in V0.5 tests | PL |
| R-10 | Compose animation jank at 90 Hz on mid-SoC | L | M | Animation intensity setting; reduced-motion mode; profile in V0.2; Canvas-only drawing (no bitmap churn) | Frame timing warnings in perf trace | PL |
| R-11 | Network flakiness causes queue overflow / stale queue | M | M | Backlog cap 20 with visible overflow disclosure; WorkManager-style flush (V0.4); turns show QUEUED/FAILED truthfully | Queue count > 5 during outages | PL |
| R-12 | Sensitive content leak via logs/exports | L | H | LogRedactor on all exports (unit-tested); no audio persistence; redaction verified in Diagnostics export | Token-like strings in export | PL |
| R-13 | Scope creep into "second Tower" behavior | M | H | FR-19 permanent rule; architecture review at each checkpoint; UI labels LOCAL vs TOWER | Any PR adding local consequential actions | PL |
| R-14 | Enclosure heat trap / acoustic muffle | M | M | Vents in enclosure design; loudspeaker port; thermal checklist Q-09/Q-14 before final print | Temp delta enclosure vs open air > 5 °C | PL |
| R-15 | Owner trust damaged by dishonest states (fake "done") | L | H | Truthful-UI quality gates; REQUEST_QUEUED/FAILED states; receipt refs surfaced | Any state not matching reality in tests | PL |
| R-16 | Development environment cannot build Android (sandbox constraint) | H (V0.1) | M | Complete source package + exact build instructions + preserved checkpoints (this delivery's Stage C path) | — | PL |

## Register hygiene

- A risk becomes a work item when Likelihood AND Impact are both ≥ M and the stage it blocks has started.
- Closed risks are archived with the evidence, never deleted.


| ID | Risk (Graph 3) | Type | Mitigation |
|---|---|---|---|
| R-G3-01 | Redesign regresses Graph 2 E2E | REGRESSION | Chrome API signatures preserved; pinned strings preserved; E2E suite is the Wave 8 gate |
| R-G3-02 | Bitmap layers inflate memory | PERFORMANCE | 4 layers ≈ 24.5 MB decoded, within the 48 MB budget; LOW profile skips bitmaps entirely |
| R-G3-03 | OLED burn-in from monumental brightness | HARDWARE | state-budgeted luminance + per-region drift + scheduled dimming (AMOLED_AND_BURN_IN_SAFETY.md) |
| R-G3-04 | Emulator cycle too slow for iteration | ENVIRONMENT | local JDK17+SDK build chain verifies compile/tests before every push |
| R-G3-05 | E2E repair frontier from Graph 2 not green yet | QUALITY | CP-G2-022 root causes fixed; next cloud E2E run verifies; suite rules unchanged |
