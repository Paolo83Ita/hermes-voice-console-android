# XIAOMI MODEL QUALIFICATION PLAN — physical-device graph (QUALIFICATION_PENDING_DEVICE)

Target: Xiaomi 11 Lite 5G NE (2109119DG), Android 14, HyperOS 2.0.4.0 UKOEUXM, 8 GB RAM,
128 GB storage. Status: **QUALIFICATION_PENDING_DEVICE** — none of this is claimed as done.

This plan qualifies REAL models on REAL hardware. Graph 2 only built the manager;
nothing here is asserted by emulator evidence.

## Candidates (from the shipped catalog)

| Component | Candidate | Gate to pass |
| --- | --- | --- |
| Wake word | openWakeWord Noah (it) | false-accept < 1/day ambient, react < 1.5 s |
| VAD | Silero VAD tiny | speech/non-speech agreement ≥ 95% on 10-min sample |
| STT | whisper.cpp tiny int8 | Italian WER on 20 known phrases ≤ 15%; RTF ≤ 1.0 |
| Local LLM (HYBRID) | compact it-capable Q4 | no second-authority behavior; latency ≤ 6 s simple |
| Local LLM (LOCAL_EXPERIMENTAL) | Gemma-2B IT Q4 | RAM gate (≥3 GB free), thermal budget, Owner activation |
| TTS | Piper it_IT riccardo | intelligibility; speaking-rate control honored |

## Procedure per component

1. Read device facts (Model Manager "Recommended setup" card): model, Android, ABI,
   RAM available, storage free, TTS engines, network, thermal, power-save.
2. Resolve the profile (TOWER_FIRST / HYBRID_BALANCED first; LOCAL_EXPERIMENTAL only
   with explicit Owner activation and passing gates).
3. Confirm download (Wi-Fi, size, license), then observe: preflight → download →
   hash verify → atomic install → qualification → activation → read-back.
4. Record: download time, installed size, RAM in use (before/during/after),
   storage delta, thermal status, latency class experienced.
5. Exercise rollback to the previous version and removal cleanup; re-install if kept.
6. Repeat after restart and process recreation (registry persistence).

## Thermal and soak gates

- 1-hour voice-session soak at 25 °C ambient: no thermal status ≥ 4 sustained > 5 min.
- Battery temperature stays ≤ 42 °C during LOCAL_EXPERIMENTAL LLM use.
- Four-hour desk-console soak (TOWER_FIRST): drift, burn-in guard, no leak-driven death
  (see `docs/device/XIAOMI_GRAPH_2_PHYSICAL_TEST.md`).

## Decision rule

A component becomes ACTIVE on the Xiaomi only if its gates pass AND the Owner accepts
the quality. Otherwise it stays QUALIFICATION_PENDING / QUALIFIED-not-active, and the
console keeps the Tower-only fallback. The local model is never a second authority.
