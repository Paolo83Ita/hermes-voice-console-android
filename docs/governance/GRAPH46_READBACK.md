# GRAPH 4.6 READBACK

Directive: GRAPH 4.6 — BETA READINESS / ITALIAN_UI_COMPLETE / MONUMENTAL TOWER
FINALIZATION / REAL STT-TTS DELIVERY / OPTIONAL LOCAL LLM / TOWER ARCHIVE / ARM64 APK /
XIAOMI QUALIFICATION PACKAGE.
§0 FIRST EXACT ACTION executed physically at session start. This file supersedes
narrative claims; every dimension below was re-verified live this session.

## 1. Repository authority (physically verified)

| Dimension | Value |
| --- | --- |
| Reported tip in directive | `b3b78ed` (owner explicitly says: do NOT assume it is authoritative) |
| Active branch (local) | `graph-4/real-voice-pairing-archive-e2e` |
| Local HEAD (before this session's work) | `b3b78ed` |
| `git fetch origin` | executed this session (exit 0) |
| Remote HEAD `origin/graph-4/real-voice-pairing-archive-e2e` | `b3b78ed` |
| Convergence | local HEAD == remote HEAD (rev-list left/right = 0/0) — **the reported tip IS the verified tip this time** |
| `main` | untouched (not checked out, not modified; verified via `git status`) |
| Working tree at read-back | NOT clean — two files modified (see §2) |
| History guard | no force-push, no rebase, no checkpoint deletion; forward-only |

## 2. Uncommitted work found in the tree (rescued, not discarded)

The previous session was interrupted between edit and commit. The working tree carried
the §8 (APK OPTIMIZATION) implementation UNCOMMITTED:

- `app/build.gradle.kts` — `splits.abi` block: `isEnable=true`, `isUniversalApk=true`,
  `include("arm64-v8a", "x86_64")` (universal debug APK retained for CI/x86_64 emulator;
  arm64-v8a split APK serves the physical Xiaomi; debug-only qualification, no
  versionCode offsets).
- `.github/workflows/android-debug-build.yml` — SHA-256 step widened to ALL debug APKs
  (`sha256sum` + `du -b` of every output), artifact upload widened to the three APK
  outputs.

**Defect found in the rescued diff**: the upload step carried TWO `if-no-files-found`
keys (`warn` then `error`) — duplicate YAML key, invalid input shape. Repaired in the
CP-G4-021 commit (single `error`: at least one APK always exists).
**Also hardened this session**: the E2E workflow's two `adb install` calls are now
bounded (`timeout 900`) — an unbounded install of a ~165 MB APK on a software-emulated
device was the one unbounded statement in the emulator script, and it is inside the
emulator step that current runs occupy for 70–100 min.

## 3. Cloud runs (GitHub API, read this session with the stored credential)

| Run | Workflow | Commit | Conclusion | Meaning |
| --- | --- | --- | --- | --- |
| 41868119129 | android-debug-build | b3b78ed | **success** | assembleDebug + 250 unit tests + lint green at the verified tip |
| 41868118783 | android-e2e (push, segmented model) | b3b78ed | in_progress → E2E_RESULT_UNAVAILABLE at read-back | FIRST fully compile-green execution of the segmented gate model |
| 41867050032 | android-e2e (push) | 0731d07 | in_progress → E2E_RESULT_UNAVAILABLE | same |
| 41863355495 | android-e2e (push) | 3eac29a | in_progress → E2E_RESULT_UNAVAILABLE | same |
| 41867050334 | android-debug-build | 0731d07 | failure | testDebugUnitTest 249/250 → fixed by CP-G4-020 fix 14 (b3b78ed) |
| 41863355721 | android-debug-build | 3eac29a | success | build+tests+lint green |
| 41859464564/… | android-e2e | 145ee08 | failure | compileDebugAndroidTestKotlin (fixed by 3eac29a) |

**Read on the three in-flight E2E runs**: job `emulator-e2e`, step 9 ("Run emulator
end-to-end suite") started 16:05:53 / 16:29:11 / 16:37:33 UTC; all three were still
inside step 9 at read-back (≈ 17:50 UTC), with a job timeout of 120 min. The previous
emulator-executing run (41846567723, monolithic model, crash at test 3/33) consumed
~40 min in the same step; the segmented model's own bounding timeouts sum to 75 min of
instrumentation + boot — the three runs are therefore either legitimately slow (first
execution of gate+regression+residual invocations) or hung on a step the live-log
credential cannot read. They will terminate by job timeout and upload their evidence
(`if: always()`), and the CP-G4-021 push triggers a FRESH run on the new tip.
Nothing in the read-back justifies re-opening the dispositioned lifecycle residual
(PHYSICAL_XIAOMI_RECOVERY_REQUIRED stands; directive forbids reopening).

## 4. Physical state dimensions (verified live this session)

| Dimension | Verified value |
| --- | --- |
| String keys | **639 `<string>` × 5 locales (en/it/es/fr/de)** — counted live from `res/values*/strings.xml`; parity holds (LocalizationParityTest enforces at unit level) |
| Unit tests | 250 in `testDebugUnitTest` (cloud run 41868119129: green at b3b78ed) |
| Lint | android-debug-build SUCCESS at b3b78ed (abortOnError gate active) |
| Local build capability | sandbox has NO Gradle/Android SDK — all builds are cloud-executed (Stage A contingency, unchanged) |
| Italian gate code | ITALIAN-GATE-A..F segmented classes present in `app/src/androidTest/.../e2e/`; `ItalianGateSupport.kt` harness with 60 s destination floor, main-thread activation, scanner explicit Back, en-restore finally |
| Segmented instrumentation | `android-e2e.yml`: GATE invocation (must pass) → REGRESSION invocation (full suite minus quarantined residual class, must pass) → RESIDUAL invocation (typed expected signature only) → single-line POSIX verdict |
| STT | `VoskFrameSttEngine.kt` on the frame seam; vosk-android 0.3.75 pinned in `gradle/libs.versions.toml` (Maven Central, Apache-2.0); NOT_INSTALLED honesty + model-dir resolver + typed errors; AppGraph wired; active-engine read-back never claims local STT while the system recognizer runs |
| TTS | Android System TTS engine (`AndroidSystemTtsEngine.kt`) active fallback; `AndroidSystemTtsEngine.activeEngineName` + AppGraph.activeRuntimeReadback diagnostics card ×5 locales |
| LLM | foundation only (no engine, no candidate delivery) → implemented this session (§6 of the directive) |
| Archive | `ArchiveScreen` (Conversations/Missions/Artifacts) + `MockTowerArchive` (deterministic, SIMULATED labels) — finished this session per §7 (search/pagination/filters/pin/provenance/hash/re-download) |
| Source links | `ModelLinks.kt` validator (reviewed-HTTPS-host allowlist; Open source / Open license / Copy identity / View SHA-256 / Retry source check physically clickable; blocked links show localized explanation, NEVER a substitute source) + `CatalogLinkIntegrityTest` |
| Monumental Tower | Oracle Core + original architecture frame (CP-G4-019) + visual repairs D1/D2/D4/D6/D8 (CP-G4-018, verified by post-repair re-render); preview pack with all seven required states exists in the delivery folder |
| Onboarding | six-step wizard (ob_* keys), non-blocking post-finish, E2E-walked |
| Navigation | drawer = 5 primary destinations; Settings hub = 6 categories |
| APK artifacts | newest SUCCESS artifact at b3b78ed (universal only, 4-ABI); previous receipt: 165,029,486 B, sha256 9275aa…fdaa0 (b1df58a) |
| Governance | all authorities present under `docs/governance/` (WORK_LOG, CHECKPOINT_INDEX, ledger, readbacks, MISSION_GRAPH) |

## 5. Directive-item gap map (what Graph 4.6 must add)

| Directive item | State at read-back | Action this session |
| --- | --- | --- |
| §1 ITALIAN_UI_COMPLETE | gate code complete; cloud verdict pending (runs in flight) | fresh run on CP-G4-021 tip → ITALIAN_UI_REPORT.md from its evidence |
| §2 MONUMENTAL TOWER | implementation + repairs + previews in place | MONUMENTAL_TOWER_FINAL_REPORT.md |
| §3 REAL SOURCE LINKS | implemented + integrity-tested (CP-G4-019/020) | verify + fold into receipts |
| §4 REAL STT | vosk-android pinned, engine implemented, NOT_INSTALLED honesty | STT_RUNTIME_RECEIPT.md (LOCAL_STT_READY_FOR_XIAOMI_QUALIFICATION) |
| §5 REAL TTS | system TTS active fallback | TTS_RUNTIME_RECEIPT.md + downloadable-voice candidate wiring |
| §6 OPTIONAL LOCAL LLM | foundation only | implement engine + one verified candidate, DEFAULT DISABLED → LOCAL_LLM_IMPLEMENTED |
| §7 TOWER ARCHIVE | tabs + mock + SIMULATED present | finish search/pagination/filters/open/preview/cache/pin/provenance/hash/re-download |
| §8 APK OPTIMIZATION | splits implemented but UNCOMMITTED (with YAML defect) | repair + commit CP-G4-021 → dual APKs → APK_SIZE_AND_ABI_REPORT.md + SHA-256 both |
| §9 XIAOMI PACKAGE | INSTALL_XIAOMI_GRAPH4_FINAL.md exists | XIAOMI_GRAPH4_BETA_PACKAGE runbook |
| §10 TERMINALITY | pending | terminal receipt after all above |

## 6. Forward-only plan from the true Next Exact Action

> **SUPERSEDED 2026-11-05**: items 1–4 of this plan are DONE (CP-G4-021/022/023).
> The current truth is §7 below — the fresh read-back at tip `a2d0b27` and the
> fix-5 repairs. §10 terminality stays gated on the cloud gate verdict.


1. CP-G4-021: repair the rescued §8 diff (duplicate YAML key), harden the E2E install
   statements, commit + push → fresh cloud build/E2E on the new tip.
2. While cloud runs: §6 LLM implementation, §7 Archive completion, §4/§5 receipts,
   §2 Tower final report, §9 Xiaomi package.
3. Read back the fresh cloud runs; on green gate+regression+typed residual →
   ITALIAN_UI_COMPLETE in ITALIAN_UI_REPORT.md; on any failure → ledger row R-n + typed
   classification + narrow repair + full rerun (no assertion weakened).
4. Download the dual APK artifacts, hash + size-breakdown → APK_SIZE_AND_ABI_REPORT.md.
5. Governance convergence + terminal receipt
   `GRAPH_4_IMPLEMENTATION_COMPLETE_XIAOMI_QUALIFICATION_PENDING`.


## 7. FRESH READ-BACK — 2026-11-05, NEW session (directive §0 re-executed)

This section supersedes §§1–6 as the current authority map. Every dimension was
re-verified live THIS session; no claim is carried over from the b3b78ed-era
read-back.

### 7.1 Repository authority

| Dimension | Value (verified this session) |
| --- | --- |
| `git fetch origin` | exit 0 |
| Remote tip `origin/graph-4/real-voice-pairing-archive-e2e` | **`a2d0b27`** (CP-G4-023 fix 4) |
| Local HEAD | `a2d0b27` — equal to remote (rev-list 0/0) |
| `main` | untouched, not checked out |
| Working tree at read-back | clean (before this session's fix-5 edits) |
| History guard | forward-only; no force-push; no checkpoint deleted |

### 7.2 The staged terminality is NOT a verdict (critical intake)

`docs/receipts/GRAPH_46_TERMINAL_RECEIPT.md` (committed at e084be8) contains an
intact `<!-- TERMINAL_VERDICT_PLACEHOLDER -->` in its condition-1 section, and
`docs/receipts/ITALIAN_UI_REPORT.md` §6 contains an intact
`<!-- VERDICT_PLACEHOLDER -->`. Both documents were pre-staged BEFORE the gate
decided. **ITALIAN_UI_COMPLETE is not claimed anywhere yet** — the placeholders
may only be filled from a real cloud run that shows every gate class
`OK (… tests)`.

### 7.3 Cloud evidence at the verified tip

| Run | Workflow | Commit | Outcome (parsed from the job log THIS session) |
| --- | --- | --- | --- |
| 41938185541 (#132) | android-e2e | a2d0b27 | **CANCELLED at the 180-min job cap** (duration 3 h 0 m 24 s). Gate A1 FAILED: `AppNotIdleException` 60 s ASYNC_TASKS around `ActivityScenario.recreate()` — R-13's settle+ONE-retry was consumed, the retry threw the SAME exception (stack: `Espresso.onIdle` → `loopMainThreadUntilIdle`). Gate A2 **PASSED** (STATUS_CODE 0) — the R-12 direct-dispatch physics holds at this tip. Gate B: the emulator DIED (~00:39 UTC, 4th fleet death) — the offline-guard typed `DEVICE_OFFLINE_ABORT` and stopped the loop without burning timeouts (C–F and regression never ran; tally honestly 0/6). The UNBOUNDED `adb logcat -d` capture line then consumed **2 h 22 min** against the dead fleet until the cap cancelled the job BEFORE the verdict printed. → ledger **R-14** |
| 41938185538 (#114) | android-debug-build | a2d0b27 | **BUILD SUCCESSFUL**: assembleDebug + testDebugUnitTest + lintDebug; lint 0 errors (badges: android-e2e failing, android-debug-build passing) |
| #115/#116 | android-debug-build | 8bffd60 / 7c57025 | **BOTH SUCCESS** — fix-5 code compiles; unit tests 260/260 COUNTED from the 7c57025 artifact (0 failures/errors/skipped); lint 0 errors; APK sizes byte-identical (arm64 105,247,044 / universal 206,679,899 / x86_64 109,176,588) |
| #133 + #134 | android-e2e | 8bffd60 / 7c57025 | **BOTH INFRASTRUCTURE FAILURE (R-15)** — #134 boot-level (guest never online, zero instrumentation); #133 died at gate A start after compiling+installing the gate; all fix-5 mechanics verified under collapse (typed guards, bounded captures, honest verdict, ~1 h 05 m); NOT a code property |
| #135 | android-e2e | 9508ac8 | **R-14 EXERCISED, POLICY BOUND PROVEN (R-16)** — A1 failed after 604 s: AppNotIdleException through ALL FOUR recreate attempts (the 60 s master idling policy is the constraint; the drain needs 70–180 s and finishes); A2 PASSED (3rd consecutive); fleet death #7 mid-gate B → typed aborts everywhere; repair: IdlingPolicies 240 s + retry resize |

### 7.4 Physical state dimensions (re-verified live)

| Dimension | Value |
| --- | --- |
| Localization | **652 keys ×5 locales** counted live (en/it/es/fr/de `strings.xml` each 652) — parity holds at the tip |
| APKs (run #114 at the tip) | arm64-v8a **105,247,044 B** `59c3c26e…`; universal **206,679,899 B** `ebb9d7b4…`; x86_64 **109,176,588 B** `761f1693…` — sizes byte-identical to the c97a02f receipt; the briefing's "~165 MB" figure was the pre-split APK at b1df58a and is obsolete |
| Unit tests | **260/260 COUNTED** from the a2d0b27 artifact 13375237125 (tests=260, failures=0, errors=0, skipped=0) — task green at the tip (BUILD SUCCESSFUL) |
| STT | vosk-android 0.3.75 pinned + frame engine; `LOCAL_STT_READY_FOR_XIAOMI_QUALIFICATION` (STT_RUNTIME_RECEIPT.md) |
| TTS | system TTS ACTIVE with real playback/interruption/engine read-back (TTS_RUNTIME_RECEIPT.md); Piper candidates verified-downloadable, runtime row honestly pending |
| LLM | LocalLlmEngine DEFAULT DISABLED, typed-blocked generation, 10 unit tests → `LOCAL_LLM_IMPLEMENTED` (never QUALIFIED before Xiaomi) |
| Archive | deterministic mock complete (§7 rows: search/pagination/filters/open/preview/cache/pin/provenance/hash/re-download), every record SIMULATED |
| Tower | MONUMENTAL_TOWER_FINAL_REPORT.md — 7 previews with measured evidence; painterly delta honestly dispositioned |
| Xiaomi package | docs/XIAOMI_GRAPH4_BETA_PACKAGE.md — dual APK, install/upgrade, 13-item matrix, 4 h soak |
| GitHub API access | unauthenticated api.github.com rate-limited from this sandbox IP; authenticated reads executed via the stored git credential (never printed) |

### 7.5 The true Next Exact Action (executed this session → CP-G4-023 fix 5)

1. R-14 typed repair: `gateRecreateSteadily()` (AppNotIdle-only catch, backoff
   3 s→10 s→30 s, three retries, fourth propagates honestly) wired at BOTH gate
   recreate sites — `gateEnsureBaseline` (supersedes the R-13 single retry) and
   ItalianGateA's raw `scenario.recreate()`. The REAL walk is unchanged; nothing
   skipped or weakened.
2. Workflow: `adb logcat -d`/`screencap` bounded `timeout 60`; residual
   invocation offline-guard writing the typed `DEVICE_OFFLINE_ABORT` INTO
   residual-output.txt (verdict channel stays honest); job timeout 180→300 min
   (per-class arithmetic worst case ≈263 min; hosted ceiling 6 h).
3. Governance: ledger R-14, WORK_LOG G4-N25, CHECKPOINT_INDEX fix-5 row, this §7.
4. Push → the push-triggered run at the fix-5 tip is the deciding evidence:
   gate 6/6 `OK (… tests)` + regression green-or-residual-typed + residual
   typed → fill ITALIAN_UI_REPORT §6 and GRAPH_46_TERMINAL_RECEIPT §2 ONLY from
   that run → then, and only then, §10 terminality. Any other outcome → next
   typed ledger row + narrow repair + full rerun.

## 8. §0 fresh read-back — session of 2026-11-05 (fix 7 / R-17)

### 8.1 Authority verification (fetch-first, memory rejected)

| Check | Result |
| --- | --- |
| `git fetch origin` | exit 0 |
| Remote tip `origin/graph-4/real-voice-pairing-archive-e2e` | **`982877c`** (CP-G4-023 fix 6 / R-16) — the briefing's `92465e5` was NOT the tip and was NOT trusted |
| Local HEAD | `982877c1c0fa478ed1cac1f5cab6d4f9a5956238` — equal to remote (rev-parse identical) |
| `main` | untouched, not checked out; working tree clean at read-back |

### 8.2 Cloud evidence at the verified tip (parsed from the job log THIS session)

| Run | Workflow | Commit | Outcome |
| --- | --- | --- | --- |
| 41983695691 | android-debug-build | 982877c | **SUCCESS** — unit + lint green at the R-16 tip |
| 41983695658 (#136) | android-e2e | 982877c | **INFRASTRUCTURE FAILURE — fleet death #8, NEW mode**: boot COMPLETED in 968 s (07:16:41), then system_server lost activity/package services within ~1 min (`Can't find service: activity` at 07:17:27; install failed `cmd: Can't find service: package` at 07:18:15); ZERO instrumentation; job failed honestly in 23 min → ledger **R-17** |

### 8.3 Untouched terminality (intake discipline re-verified)

`docs/receipts/GRAPH_46_TERMINAL_RECEIPT.md` §2 `<!-- TERMINAL_VERDICT_PLACEHOLDER -->`
and `docs/receipts/ITALIAN_UI_REPORT.md` §6 `<!-- VERDICT_PLACEHOLDER -->` are both
INTACT — ITALIAN_UI_COMPLETE is not claimed anywhere; CP-G4-024 remains STAGED.

### 8.4 Next Exact Action (executed this session → CP-G4-023 fix 7 / R-17)

The unguarded boot→install window received the R-17 typed recovery (guest health
gate: `pm list packages` probe → `adb reboot` + re-wait 1200 s + one re-probe;
typed honest aborts on both install lines; ONE recovery per run; 300-min cap
arithmetic ≈289 min worst case; the REAL walk unchanged, nothing deleted, skipped
or weakened). This governance commit's push run is the deciding evidence: gate
6/6 `OK (… tests)` + per-class regression green-or-typed + residual typed → fill
ITALIAN_UI_REPORT §6 and GRAPH_46_TERMINAL_RECEIPT §2 ONLY from that run → then,
and only then, §10 terminality. Any other outcome → next typed ledger row +
narrow repair + full rerun.

## 9. §0 fresh read-back — session of 2026-11-05, part 2 (fix 8 / R-18..R-22)

### 9.1 Authority verification

| Check | Result |
| --- | --- |
| Remote tip after the fix-7 push | `e89d014` (CP-G4-023 fix 7 / R-17) — pushed from this session, fetch-echo confirmed (`982877c..e89d014`) |
| `main` | untouched; forward-only history preserved |

### 9.2 Cloud evidence at e89d014 (parsed from the FULL job log THIS session)

| Run | Workflow | Outcome |
| --- | --- | --- |
| 41990077160 | android-debug-build | **SUCCESS** at e89d014 |
| 41990077345 (#137) | android-e2e | **FIRST FULL-INSTRUMENTATION EVIDENCE in 10 E2E executions** (1 h 27 m): R-17 GUEST_HEALTH_OK → both APKs installed → gates A–F all invoked (A OK 2 tests 588.6 s; B timeout-killed at ~899 s with only the start header — fleet-speed baseline stall with ColorBuffer errors; C FAIL open-studio-not-found; D FAIL hub-row-not-found; E FAIL categories-reported-missing; F **OK (1 test)** 239.0 s but counted FAILED) → regression per-class (FreshInstall residual-typed; **LifecycleProfile OK (1 test) but counted FAILED**; ModelManager residual-typed; Onboarding FAIL 20 s case-sensitive wait vs "CONNECT TO HERMES TOWER"; SelfTest residual-typed; VoiceFlow OK (6 tests)) → residual typed (the dispositioned signature — expected) → E2E_VERDICT_FAILED printed honestly |
| TRUE tally after re-classification | — | gates **A+F = 2/6 truly green**; regression **2 OK + 3 residual-typed + 1 real failure (onboarding, R-22)**; residual typed ✓ |

### 9.3 The typed findings (ledger R-18..R-22) and the repairs (fix 8)

- **R-18** harness false-negative: the classification pattern was plural-only —
  `OK (1 test)` (the instrument's SINGULAR verdict) counted as failure (gate F,
  regression LifecycleProfile). Repaired at ALL five sites with
  `grep -qE 'OK \([0-9][0-9]* tests?\)'` — a FALSE-NEGATIVE repair (the opposite
  of flake-hiding), nothing weakened.
- **R-19** gate C navigation defect (route-table-proven): dest_connect routes
  DIRECTLY to AppRoutes.TOWER_STUDIO; the open-studio control lives on the
  CONNECTION screen (dest_connection). The walk now follows the real product
  path: Connection → open-studio → Tower Studio → scanner → back.
- **R-20** gate E: the failure dump PROVED all six categories visible
  ("GENERALE", "CONNESSIONE ALLA TORRE", "DISPLAY" — SectionCard uppercases
  titles); the case-sensitive contains() could never match → ignoreCase (R-8
  lineage, matcher-only).
- **R-21** gateNavigateTo: the fallback awaits the Settings hub TITLE (the row
  label can false-match body text — the VOICE engine_row contains "Modelli e
  voce"), the hub-row lookup retries on the unmerged tree, and the not-found
  error now carries gateVisibleTexts() (observability contract = gateAwaitAnchor).
  Gate D's drawer-row lookup mechanism stays UNRESOLVED-by-evidence — the next
  occurrence decides from the dump.
- **R-22** waitForText ignoreCase (the onboarding regression failure class).
- **Gate B**: no code change — fleet-speed infrastructure failure (baseline
  consumed 899/900 s; C/D/E/F baselines normal minutes later) → the R-15 rerun
  channel.

### 9.4 Untouched terminality

The two placeholders (GRAPH_46_TERMINAL_RECEIPT §2, ITALIAN_UI_REPORT §6) remain
INTACT — ITALIAN_UI_COMPLETE is still not claimed anywhere; CP-G4-024 STAGED.

### 9.5 Next Exact Action (executed this session → CP-G4-023 fix 8)

This governance commit's push run is the deciding evidence: gate 6/6
`OK (… test[s])` + per-class regression green-or-typed + residual typed → fill
ITALIAN_UI_REPORT §6 and GRAPH_46_TERMINAL_RECEIPT §2 ONLY from that run → then,
and only then, §10 terminality. Any other outcome → next typed ledger row +
narrow repair + full rerun.

## 10. §0 fresh read-back — session of 2026-11-05, part 3 (fix 9 / R-23)

### 10.1 Authority + cloud evidence

| Item | Result |
| --- | --- |
| Push-verified tip | `29447ae` (fix 8) — pushed and run-confirmed this session |
| Build at 29447ae | **SUCCESS** (42009479286 — the R-19/20/21/22 Kotlin edits compile; unit + lint green) |
| E2E #138 (42009479297) | fleet death #10 DURING gate A2 — A1 PASSED (3rd consecutive cloud sample); fix-8 repairs LIVE (the R-18 pattern log-proven); containment typed everywhere; honest verdict |
| Statistics | 7 infra failures in 11 E2E executions; #137 PROVES a healthy sample completes the whole model |

### 10.2 Repair executed (fix 9 / R-23)

ONE bounded ≈45 s transport recovery (`adb reconnect offline` + adb-server
restart) at EVERY class boundary BEFORE the typed abort — recovered → remaining
classes continue; not recovered → unchanged typed abort. Cap 310 (≈273 min
worst case). No test deleted, skipped or weakened.

### 10.3 Untouched terminality + Next Exact Action

Both placeholders remain INTACT; ITALIAN_UI_COMPLETE still not claimed; CP-G4-024
STAGED. This commit's push run is the deciding evidence: gate 6/6 green +
regression green-or-typed + residual typed → fill the two placeholders ONLY from
that run → §10 terminality. Any other outcome → next typed row + narrow repair +
full rerun.

## 11. §0 fresh read-back — session of 2026-11-05, part 4 (fix 10 / R-24)

### 11.1 Cloud evidence at 3eecf76

Run #139 (42017388466): boot 13.6 min, R-17 gate OK, both installs OK, gate A
consumed its whole 900 s budget on the cold-start baseline (only its start
marker printed), the guest was dead at the B boundary — death #11 (8 infra
failures in 12 executions). The R-23 recovery executed to spec: bounded
attempts, typed aborts, honest verdict — it correctly cannot revive a dead
guest.

### 11.2 Repair executed (fix 10 / R-24)

Typed bounded warm-up AFTER the installs and BEFORE the gate loop:
`am start -W` (120 s cap) + 60 s settle + `am force-stop` (data preserved) —
the evidenced one-time cold-start costs (#137: gate A 588 s; #138: A1 ~747 s;
#139: 900 s → marker only) are paid OUTSIDE every class budget. A warm-up
failure is a typed honest abort. Worst case ≈276 min < the 310 cap.

### 11.3 Untouched terminality + Next Exact Action

Both placeholders remain INTACT; ITALIAN_UI_COMPLETE still not claimed;
CP-G4-024 STAGED. This commit's push run is the deciding evidence: gate 6/6
green + regression green-or-typed + residual typed → fill the two placeholders
ONLY from that run → §10 terminality. Any other outcome → next typed row +
narrow repair + full rerun.

## 12. §0 fresh read-back — session of 2026-11-05, part 5 (fix 11 / R-25)

### 12.1 Cloud evidence at 91da8f8

Run #140 (42024623890): R-17 gate OK, **R-24 warm-up complete (12:40:10) — the
mechanism worked**, gate A PASSED (4th consecutive gate-A green), the guest died
during gate B (death #12 — the recurring B window: #132/#135/#140); R-23
recovery attempted and correctly failed on a dead guest; typed aborts; honest
verdict. 10 fleet deaths in 13 E2E executions; the single complete survival
(#137) produced the real test-outcome evidence that became R-18..R-22.

### 12.2 Repair executed (fix 11 / R-25)

The residual failure mode is pure infrastructure sampling — no further
app-side/harness repair is evidence-justified. Structural: TWO independent fleet
samples per push (matrix sample [1,2], fail-fast: false); each lane writes
VERDICT.txt from the ESTABLISHED verdict logic and uploads it; a new
`e2e-verdict` arbitration job fails the run UNLESS at least one lane printed
E2E_VERDICT_PASSED. The lanes carry continue-on-error so one infrastructure
death cannot mask the other lane's complete green sample. "Missing evidence is
NOT success" is preserved: no VERDICT.txt = no credit; both lanes failed = the
run fails honestly.

### 12.3 Untouched terminality + Next Exact Action

Both placeholders remain INTACT; ITALIAN_UI_COMPLETE still not claimed;
CP-G4-024 STAGED. This commit's dual-sample push runs are the deciding
evidence: ANY lane showing gate 6/6 green + regression green-or-typed + residual
typed → fill the two placeholders ONLY from that lane's artifacts → §10
terminality. Any other outcome → next typed row + narrow repair + full rerun.

## 13. §0 fresh read-back — session of 2026-11-05, part 6 (fix 12 / R-26)

### 13.1 Cloud evidence at 06eaa62 (two-sample run #142)

Lane 2 died early (fleet). **Lane 1 SURVIVED 104 MINUTES — the best sample since
#137**: R-17 OK, R-24 warm-up complete, **A1 green (5th consecutive cloud
sample)**, A2 budget-stalled mid-class (fleet stall), **GATE E PASSED — R-20
CLOUD-VERIFIED**, gates C+D failed at the OnClick lookup WITH FULL DUMPS (the
R-21 observability contract paid off), the guest died during gate F (death #13);
regression/residual typed aborts; honest verdict. The dumps DECIDE: the hub
rows' text is present ("Modelli e Voce Componenti installati…", "Torre attuale e
riconnessione…"), no OnClick is findable in either tree, and dest_connection is
not a drawer row (primaryDestinations: CONSOLE/ARCHIVE/TOWER_STUDIO/
MODEL_MANAGER/SETTINGS).

### 13.2 Repair executed (fix 12 / R-26)

Gate C navigates via the hub row's REAL label (settings_tower_status → the
CONNECTION route). gateNavigateTo's fallback: when both OnClick-filtered lookups
fail BUT a node whose text STARTS WITH the row label exists, the harness
performs ONE physical tap at that node's center (the real user path) — the
destination anchor still decides success; the no-node error path is unchanged.
Nothing deleted, skipped or weakened.

### 13.3 Untouched terminality + Next Exact Action

Both placeholders remain INTACT; ITALIAN_UI_COMPLETE still not claimed;
CP-G4-024 STAGED. This commit's dual-sample push runs are the deciding evidence:
ANY lane showing gate 6/6 green + regression green-or-typed + residual typed →
fill the two placeholders ONLY from that lane's artifacts → §10 terminality.

## 14. §0 fresh read-back — session of 2026-11-05, part 7 (fix 13 / R-27)

### 14.1 Authority verification

Fresh fetch read-back (never trusting memory): remote tip
origin/graph-4/real-voice-pairing-archive-e2e = local HEAD = `afefd3d`
(fix 12 / R-26); `main` untouched (`e7ab101`). The decisive run #137
(41990077345 @ `e89d014`) turned out to be the FIRST full-instrumentation
evidence run and its honest verdict produced R-18..R-22; the chain then ran
fix 9 (R-23, death #10), fix 10 (R-24, death #11), fix 11 (R-25, death #12),
fix 11 deployment repair, fix 12 (R-26, death #13) — every step typed from
parsed cloud logs, every tip push-triggered.

### 14.2 Run #143 (42050155253 @ `afefd3d`) — arbitrated from full lane logs

BOTH lanes (jobs 127323782870 / 127323782414) died at ~4 min — NOT in the
fleet: `> Task :app:compileDebugAndroidTestKotlin FAILED` with exactly ONE
compiler error on each lane:
`e: ItalianGateSupport.kt:412:38 Unresolved reference 'click'`.
The build run 42050155287 at the same tip was SUCCESS — the androidTest
source set is only compiled inside the E2E workflow, so the R-26 harness
change shipped without any androidTest compilation gate.

### 14.3 Root cause (artifact-proven, zero speculation)

`TouchInjectionScope.click` is a TOP-LEVEL EXTENSION
(`public fun TouchInjectionScope.click(position: Offset = center)`,
androidx.compose.ui.test.TouchInjectionScope.kt line 392), not an interface
member: interface members (down/moveTo/up) resolve inside the
`performTouchInput { }` lambda, full-gesture extensions must be imported at
the call site. ItalianGateSupport.kt imported `performTouchInput` but not
`click` — exactly one unresolved reference at the call site, identical on both
lanes (BUILD_CODE_ERROR, deterministic; death #14).

### 14.4 Repair executed (fix 13 / R-27) + Next Exact Action

(1) `import androidx.compose.ui.test.click` added to ItalianGateSupport.kt —
the single unresolved reference, no behavior change, the R-26 tap fallback
semantics intact. (2) Structural leg: `:app:compileDebugAndroidTestKotlin`
added to the android-debug-build build step so any future BUILD_CODE_ERROR in
the harness fails the cheap build job and never reaches the fleet. No test
deleted, skipped or weakened. Both placeholders remain INTACT; CP-G4-024
STAGED. THIS commit's dual-sample push runs are the deciding evidence: ANY
lane showing gate 6/6 green + regression green-or-typed + residual typed →
fill the two placeholders ONLY from that lane's artifacts → §10 terminality.

## 15. §0 fresh read-back — session of 2026-11-05, part 8 (fix 14 / R-28)

### 15.1 Authority verification

Fresh fetch read-back: remote tip = local HEAD = `7c17745` (fix 13 / R-27);
`main` untouched (`e7ab101`). The fix-13 build run 42059335648 = SUCCESS —
`:app:compileDebugAndroidTestKotlin` passed with the R-27 import (root-cause
repair compile-verified) and the build-stage androidTest gate is LIVE.

### 15.2 Run #144 (42059335798 @ `7c17745`) — arbitrated from full lane logs

BOTH lanes died ~40 min in, gate A `OK (2 tests)` on both (6th consecutive
cloud sample), ColorBuffer errors precede the death, and the guest becomes
FULLY unreachable (`could not connect to TCP port 5554: Connection refused`) —
the EMULATOR PROCESS dies. The B-window is the dominant kill zone
(#132/#135/#140/#144). R-23 correctly refused (a dead process is not a
transport wedge). Death #15, dual-lane correlated.

### 15.3 Repair executed (fix 14 / R-28) + Next Exact Action

ONE typed bounded recovery for the evidenced collapse class: relaunch the SAME
AVD once per lane (file-flag survives the one-line-per-sh -c execution),
bounded warm boot + settle + R-17-style health probe; the class that died WITH
the device is re-invoked ONCE, its file rewritten; verdict logic untouched.
Cap 310→340 (worst ≈337). Both placeholders remain INTACT; CP-G4-024 STAGED.
THIS commit's dual-sample push runs are the deciding evidence: ANY lane showing
gate 6/6 green + regression green-or-typed + residual typed → fill the two
placeholders ONLY from that lane's artifacts → §10 terminality.

## 16. §0 fresh read-back — session of 2026-11-05, part 9 (fix 15 / R-29+R-30)

### 16.1 Authority verification

Fresh fetch read-back: remote tip = local HEAD = `b69a5e6` (fix 14 / R-28);
`main` untouched (`e7ab101`). The fix-14 build run 42067864288 = SUCCESS.

### 16.2 Run #145 (42067858465 @ `b69a5e6`) — arbitrated from full lane logs

Lane 2 (job 127386946544): R-28's pkill matched the script's own sh -c command
line → the recovery killed its own process tree (exit code null) → lane dead
at ~43 min (SCRIPT_DEFECT). Lane 1 (job 127386947318): the FIRST fully-alive
116-min fleet sample — gate A OK (7th consecutive), E OK, F OK, regression
2 OK + 3 dispositioned-typed; gate B = instrument stream broke silently at
~813 s (ColorBuffer in-window); gate C = 753 s walk SUCCEEDED then
`Espresso.pressBack` threw `RootViewWithoutFocusException` (has-window-focus
=false on the CameraX scanner screen); gate D = MODEL SANCTUM anchor timeout
with the drawer visible (mechanism unproven on a ~2.5×-degraded guest — no
blind repair). Verdict FAILED honestly.

### 16.3 Repair executed (fix 15 / R-29+R-30) + Next Exact Action

R-29: variable-split pkill pattern at all 6 R-28 sites (the joined string
never appears in the script's own cmdline). R-30: gateLeaveScannerBack uses
the device-level SYSTEM back (UiAutomator, uiautomator 2.3.0 Apache-2.0) —
no window focus required; the return anchor unchanged. B and D untouched —
their next healthy-guest sample decides. Both placeholders remain INTACT;
CP-G4-024 STAGED. THIS commit's dual-sample push runs are the deciding
evidence: ANY lane showing gate 6/6 green + regression green-or-typed +
residual typed → fill the two placeholders ONLY from that lane's artifacts →
§10 terminality.

## 17. §0 fresh read-back — session of 2026-11-05, part 10 (fix 16 / R-31+R-32)

### 17.1 Authority verification

Fresh fetch read-back: remote tip = local HEAD = `bbf5cc1` (fix 15 /
R-29+R-30); `main` untouched (`e7ab101`). The fix-15 build run (42088354391)
= SUCCESS — uiautomator 2.3.0 resolved, the R-30 back path compiles.

### 17.2 Run #146 (42088354468 @ `bbf5cc1`) — arbitrated from full lane logs

Lane 2 (job 127459452797): `INSTRUMENTATION_ABORTED: System has crashed.`
during gate A — system_server died with adb ALIVE (collapse mode #16); the
device-loss guards correctly stayed silent; 12 classes burned in ~43 s with
no evidence. Lane 1 (job 127386947318): R-28 fired EXACTLY as designed (the
R-29 fix verified in production — the relaunch ran cleanly, no self-match)
but the relaunched guest missed the 1200 s boot bound (R-28 UNBOOTED, typed
honest abort). Verdict FAILED honestly on both lanes.

### 17.3 Repair executed (fix 16 / R-31+R-32) + Next Exact Action

R-31: unified guest health (get-state AND pm-list probe) at every guard; the
in-class retries also fire on the typed `INSTRUMENTATION_ABORTED: System has
crashed` marker — a crashed system now gets the same single R-28 relaunch +
one re-invocation instead of 12 evidence-free classes. R-32: relaunch boot
bound 1800 s; cap 350 (worst ≈348). Both placeholders remain INTACT;
CP-G4-024 STAGED. THIS commit's dual-sample push runs are the deciding
evidence: ANY lane showing gate 6/6 green + regression green-or-typed +
residual typed → fill the two placeholders ONLY from that lane's artifacts →
§10 terminality.

## 18. §0 fresh read-back — session of 2026-11-05, part 11 (fix 17 / R-33)

### 18.1 Authority verification

Fresh fetch read-back: remote tip = local HEAD = `5bcad1a` (fix 16 /
R-31+R-32); `main` untouched (`e7ab101`).

### 18.2 Run #147 (42099734635 @ `5bcad1a`) — arbitrated from full lane logs

Lane 1 (job 127499073230): R-17 OK 20:18:22; gate A `OK (2 tests)` 20:40:37
(8th consecutive cloud sample); gate B died with the device ~20:49 → R-28
relaunch → UNBOOTED at the 1800 s bound (typed honest abort). Lane 2 (job
127499072938): a class died with the device → R-28 → UNBOOTED at 1800 s
(20:33:22→21:03:42). Combined with #146 lane 1: 3/3 relaunch boots failed
while every fresh-job boot succeeded → stale-AVD-lock mechanism.

### 18.3 Repair executed (fix 17 / R-33) + Next Exact Action

rm -f the stale AVD locks before every relaunch (6 sites) + the UNBOOTED
typed aborts now carry the qemu relaunch-log tail. Both placeholders remain
INTACT; CP-G4-024 STAGED. THIS commit's dual-sample push runs are the
deciding evidence: ANY lane showing gate 6/6 green + regression green-or-typed
+ residual typed → fill the two placeholders ONLY from that lane's artifacts
→ §10 terminality.

## 19. §0 fresh read-back — session of 2026-11-05, part 12 (fix 18 / R-34)

### 19.1 Authority verification

Fresh fetch read-back: remote tip = local HEAD = `9bbb8af` (fix 17 / R-33);
`main` untouched (`e7ab101`).

### 19.2 Run #148 (42113516002 @ `9bbb8af`) — arbitrated from full lane logs

Lane 1 (job 127546980862): gates A, B, E `OK` — B GREEN on the R-30-era
healthy sample (the B window is not inherently deadly) — gate C died with the
device at 22:31 → R-28 → UNBOOTED at 1800 s; D/F lost. Lane 2 (job
127546981241): R-28 → UNBOOTED, and the NEW R-33 log tail captured the
mechanism: `ProbeKVM: This user doesn't have permissions to use KVM
(/dev/kvm)` + `x86_64 emulation currently requires hardware acceleration!` —
the relaunched qemu runs software acceleration (5/5 relaunch failures now
explained).

### 19.3 Repair executed (fix 18 / R-34) + Next Exact Action

kvm4all udev rule + udevadm reload/trigger + chmod 666 /dev/kvm before every
relaunch (6 sites, || true-guarded). Both placeholders remain INTACT;
CP-G4-024 STAGED. THIS commit's dual-sample push runs are the deciding
evidence: ANY lane showing gate 6/6 green + regression green-or-typed +
residual typed → fill the two placeholders ONLY from that lane's artifacts →
§10 terminality.
