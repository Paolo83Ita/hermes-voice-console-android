# OWNER PHYSICAL FEEDBACK RECEIPT — Hermes Voice Console Android

Received: 2026-11-06 (Europe/Rome) · Channel: Owner briefing (physical device delta)
Authority: this receipt records the Owner's findings VERBATIM-TYPED. It is the intake
document for the Owner priority override. Repo authority at intake: branch
`graph-4/real-voice-pairing-archive-e2e`, tip `411681941146f8fe6d627c81b97331f8d379aa9a`
(fresh fetch + read-back at receipt time; local tree fast-forwarded 13 commits, no
content divergence).

## 1. Device and provenance (Owner-declared)

| Field | Value |
| --- | --- |
| Device | POCO X6 PRO |
| Storage / RAM | 256 GB / 8 GB |
| Observation class | REAL INSTALLATION ON A PHYSICAL DEVICE |
| Priority rule (Owner) | physical findings have HIGHER PRIORITY than emulator assumptions |

Supersession note: earlier briefings named Xiaomi 11 Lite 5G NE as the target device;
this briefing names POCO X6 PRO as the device the feedback comes from. The upgrade
package (Finding 4 deliverable) is produced for Xiaomi/POCO HyperOS-family devices.

## 2. Tested package provenance (physical evidence, this session)

The Owner's install artifact available for inspection is
`hermes-voice-console-debug-apk-ca8980b8f157f25e9bede9158d46835f8b5079df.zip`
(uploaded with the briefing's earlier session; inspected 2026-11-06).

| Inspection item | Result (typed) |
| --- | --- |
| Archive content | `app-debug.apk`, 58,782,349 B — byte-identical size to the Graph 3 terminal cloud authority |
| `aapt`/dex inspection | versionName `0.2.0-debug`, versionCode 2 |
| Commit binding | `ca8980b` = Graph 3 terminal (CP-G3-008, cloud authority run 41375246590) |
| Voice pipeline inside the tested APK | `InactiveSpeechToTextEngine`, `InactiveVoiceActivityDetector`, `InactiveWakeWordEngine`, `NoAudioCaptureEngine`, `MockTextToSpeechEngine` — NO `AndroidSystemTtsEngine`, NO `ConsoleVoiceCoordinator`, NO real voice pipeline |
| Verdict | **TESTED_PACKAGE_IS_GRAPH_3_TERMINAL** — the tested package was produced ~30 commits BEFORE the current Graph 4 tip; the Owner's own statement ("produced before the latest Graph 4 commits") is CONFIRMED by binary inspection |

## 3. Physical findings (Owner verbatim, typed)

### FINDING 1 — Install and launch

| Field | Value |
| --- | --- |
| Observation | The application installs successfully; the application launches successfully; basic interaction is possible |
| Typed evidence (Owner-declared) | `DEVICE_INSTALLATION_PASSED`, `APPLICATION_LAUNCH_PASSED` |
| Qualifier (Owner) | The tested package was produced before the latest Graph 4 commits; observations do NOT automatically apply to the latest APK |

### FINDING 2 — Secondary screens layout/spacing

| Field | Value |
| --- | --- |
| Observation | Several secondary screens and settings submenus show layout and spacing problems: incorrect text layout; imperfect spacing; visual polish issues; some screens feel unfinished compared with the main experience |
| Typed action | `OWNER_UI_LAYOUT_DEFECT_LEDGER.md` (per-screen ledger) + dedicated `UI_POLISH_PASS` before terminality |
| Status | LEDGER_CREATED this cycle (see the ledger file); repairs in `UI_POLISH_PASS` commit |

### FINDING 3 — Local voice answers in English

| Field | Value |
| --- | --- |
| Observation (verbatim) | The local voice answers in English. The TTS route is operational. The selected language route is incorrect. |
| Owner constraint | Classify the defect precisely, do not guess, determine the real root cause |
| Status | Root-cause investigation completed: `ITALIAN_TTS_ROOT_CAUSE_REPORT.md`; repair implemented (typed voice negotiation + runtime read-back); terminal state gated on the NEXT physical cycle |

**Provenance contradiction (recorded honestly, not resolved by guessing):** the
inspected tested package (`ca8980b`) contains no real TTS engine — its TTS seam is
`MockTextToSpeechEngine` (silent) and the voice classes are `Inactive*`. On THAT
package the app's own voice pipeline could not produce any spoken answer. Therefore
either (a) the installed build was a later Graph 4 package (in which case Finding 3
applies to `AndroidSystemTtsEngine` and the code-proven defect below is the direct
cause), or (b) the spoken answer came from a source outside the tested app. The new
build's runtime read-back (Diagnostics voice rows + Self-Test ST-51) produces the
discriminating evidence ON DEVICE in the next cycle — no further speculation is
recorded here.

### FINDING 4 — Tested APK predates Graph 4

| Field | Value |
| --- | --- |
| Observation | The tested APK was generated before the latest Graph 4 commits; the latest physical qualification therefore remains `PARTIAL_OWNER_EVIDENCE` |
| Owner-required deliverables | current APK; arm64 APK; installation guide; upgrade guide; Owner installs the newest package and repeats the test |
| Status | delivered this cycle (see §6) |

## 4. Owner priority override (recorded as binding)

Before Archive, before optional LLM optimization, before visual micro-polish:

1. complete Italian TTS;
2. improve submenu layout;
3. generate current APK;
4. produce physical-install package;
5. collect a second Owner validation cycle.

After that, continue with the remaining Graph 4 nodes.

## 5. Dispositions at intake

| Finding | Disposition | Deliverable |
| --- | --- | --- |
| 1 | RECORDED — `DEVICE_INSTALLATION_PASSED`, `APPLICATION_LAUNCH_PASSED` (package-bound to `ca8980b`) | this receipt |
| 2 | LEDGER + REPAIR | `OWNER_UI_LAYOUT_DEFECT_LEDGER.md`, `UI_POLISH_PASS` commit |
| 3 | ROOT-CAUSE REPORT + REPAIR + READ-BACK | `ITALIAN_TTS_ROOT_CAUSE_REPORT.md`, typed negotiation code, ST-51, Diagnostics rows |
| 4 | PACKAGE | universal + arm64-v8a debug APKs, `docs/INSTALL_POCO_X6_PRO.md` upgrade guide, second-cycle runbook |

## 6. Second Owner validation cycle — required evidence (planned)

With the NEW package (versionName `0.3.0-alpha-graph4-owner-feedback`, see the build
receipt) the Owner runbook collects, ON DEVICE:

1. `DEVICE_INSTALLATION_PASSED` + `APPLICATION_LAUNCH_PASSED` re-typed for the new package;
2. Diagnostics → Active runtime: active TTS engine package, active voice name, active voice locale, Italian voice classification (the runtime read-back the root-cause report requires);
3. Self-Test Center → ST-51 "Italian TTS voice negotiation probe" result;
4. Settings → Voice & Audio → Italian voice status card (typed state + direct action when voice data is missing);
5. submenu layout re-check of the six Settings categories and the secondary screens listed in the defect ledger;
6. spoken answer language check: console answer in Italian (or the honest typed blocked state).

## 7. Honesty boundaries

- No emulator assumption is recorded as physical evidence; emulator-derived facts are labeled.
- Finding 3's on-device root cause is NOT declared resolved — it is declared
  code-proven with a typed repair, and terminality requires the next cycle's read-back
  (`ITALIAN_VOICE_ACTIVE` or `ITALIAN_VOICE_READY_AND_BLOCKED_BY_KNOWN_RUNTIME_LIMITATION`).
- No screenshot exists for Finding 2 (the ledger's screenshot references are typed
  `PENDING_OWNER_CAPTURE` for the next cycle).
