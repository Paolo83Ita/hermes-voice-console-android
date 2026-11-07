# OWNER_POCO_XIAOMI_RETEST_PACKAGE_5 — GRAPH 4.12 (§37)

Build label: **POCO XIAOMI PHYSICAL REPAIR 4**
Version: `0.3.5-alpha-graph4.12-physical-repair` / versionCode 8
(precedes `0.3.4-alpha-graph4.11` / versionCode 7 — the physically tested POCO OWNER PACKAGE 4)

## Devices

| Device | RAM | Storage | Extra focus |
| --- | --- | --- | --- |
| POCO X6 Pro | 8 GB | 256 GB | primary: Vosk mic, Riccardo/Paola, A/B switching, continuous listening |
| Xiaomi 11 Lite 5G NE | 8 GB | 128 GB | compact layout, LOW/BALANCED profiles, memory, temperature |

## POCO checklist (§37)

1. **Fresh install → onboarding → NO CRASH.** Complete the six steps (change the language mid-onboarding at least once in a separate attempt — that was the crash window). If a crash ever happens, the app now shows: “L’app si è chiusa durante il primo avvio.” with Esporta dati diagnostici / Continua in modalità sicura / Riprova configurazione / Apri Console senza attivare i motori.
2. **Italian response** (regression guard — must stay Italian; PHYS-PASS-01 holds).
3. **Continuous listening** (regression guard — PHYS-PASS-02 holds; now with hardened TTS pause/resume + echo rejection).
4. **Interrompi — the unified stop.** During LISTENING the primary control label reads *Interrompi ascolto*; during transcription *Annulla trascrizione*; during processing *Annulla richiesta*; while speaking *Interrompi voce*; in continuous mode *Ferma ascolto continuo*. After the tap, a typed final-state read-back line appears under the label (mic/turn/bridge/tts/mode → final state). Idle shows the clearly disabled state.
5. **Vosk microphone** — Model Sanctum → install/verify the offline Italian model → Self-Test ST-55 (FIXTURE_PASS on device) → then a REAL microphone turn: spoken Italian appears as the transcript.
6. **Riccardo** — install/activate → ST-56 must show the voice consumed by the REAL runtime (system TTS route → honest INSTALLED_RUNTIME_NOT_AVAILABLE, never ACTIVE).
7. **Paola** — same via ST-57.
8. **A/B switching** — ST-54: voice A speaks the preview phrase, then voice B speaks the SAME phrase; the identities are distinct; the audible difference is your verdict (VOICE_SWITCHING_OWNER_VERIFICATION_PENDING stays until you say PASS).
9. **Contrast** — every label readable (the old gray-on-navy class is repaired; tokens now pinned by tests).
10. **New Tower** — the ORIGINAL central tower (candidate A): complete sign, three rings, cyan portal, real gateway, widening stairs. §31 states animate; privacy/chat-mute/disconnected/error seals are Canvas overlays. Only you grant the visual PASS (OWNER_VISUAL_RETEST_PENDING).
11. **Restart** — kill + reopen: selected voice, language, listening mode and policy persist; no crash; capability ladder restores from storage.

## Xiaomi checklist (§37)

1. Fresh install → onboarding → NO CRASH.
2. **Compact layout** — 360 dp class: Connection Studio rows render one-line full-width (Graph 4.10 fixes hold); no one-character-per-line anywhere.
3. **Contrast** — same as POCO (both themes).
4. **Interrompi** — same state-specific labels as POCO.
5. **Continuous listening** — 5 turns minimum, no self-echo, no duplicated user turn.
6. **LOW profile** — Appearance → Visual quality → LOW: static architecture only, no rotation, no particles; Tower decoded memory ≈ 11 MB.
7. **BALANCED profile** — default: state-driven animation; decoded ≈ 21 MB.
8. **Memory** — first Console interactive ≤ ~3 s after onboarding navigation (mechanism layers decode after the first frame; if slower, a typed slower-device note is acceptable evidence).
9. **Temperature** — 10 minutes of continuous listening: no thermal stop unless the system itself is throttled; thermal stop is an honest typed stop, not a crash.

## Evidence the app itself provides (secret-clean)

- First-run crash receipt (`crash_receipts/last_crash.properties`) — only after a real crash.
- Model runtime receipts (`model_runtime_truth.properties`) — per activation attempt.
- Diagnostic export (clipboard) — build identity + TTS call-path telemetry + runtime read-back.

## Verdict rules (unchanged)

Only the Owner grants physical PASS. The required physical state remains
`POCO_XIAOMI_STABILITY_MODELS_CONTROLS_CONTRAST_AND_VISUAL_RETEST_PASSED`.
The pre-device state after this package is
`POCO_XIAOMI_PHYSICAL_REPAIR_PACKAGE_READY`.
