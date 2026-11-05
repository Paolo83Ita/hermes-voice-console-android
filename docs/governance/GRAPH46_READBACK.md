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
