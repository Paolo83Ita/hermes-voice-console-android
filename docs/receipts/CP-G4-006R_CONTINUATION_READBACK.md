# CP-G4-006R — GRAPH 4 CONTINUATION READ-BACK RECEIPT

Date: 2026-11-03 (session open) · Executor: autonomous continuation under Owner P. Lombisani
Mode: `CONTINUE_ACTIVE_GRAPH_FROM_LATEST_VERIFIED_REMOTE_CHECKPOINT`
Branch: `graph-4/real-voice-pairing-archive-e2e` · All facts below physically verified this session.

## 1. Repository truth (fresh local + remote read-back)

| Item | Verified value | Method |
| --- | --- | --- |
| Local HEAD | `fe53a64` ("second E2E sample… KNOWN Graph 3 residual…") | `git log` |
| Remote `graph-4/real-voice-pairing-archive-e2e` | `fe53a64` — EXACT match | `git ls-remote` |
| Remote `main` | `e7ab101` — untouched (Graph 1 authority preserved) | `git ls-remote` |
| Remote `graph-3/…` | `ca8980b` — untouched | `git ls-remote` |
| Remote `graph-2/…` | `bc186db` — untouched | `git ls-remote` |
| Working tree | CLEAN (0 modified files, no mode-only residue) | `git status --short` |
| Governance files | All present under `docs/governance/`, receipts intact | listing |

Conclusion: local and remote are CONVERGED at `fe53a64`; the reported governance commit is the
latest verified remote commit and prevails over every SHA named in the mission prompt. Nothing
was restarted or recreated.

## 2. Reported completed checkpoints — verification status

| Checkpoint | Reported SHA | Verified |
| --- | --- | --- |
| CP-G4-005R | `262a79e` | YES — in local+remote history |
| CP-G4-006 (AudioRecord) | `090040d` | YES |
| CP-G4-006 part 2 (voice/text wiring) | `adbc16c` | YES |
| CP-G4-010/011 wave 1 (nav+localization) | `749e746` | YES |
| CP-G4-012a pairing wave 1 | `f7bde56` | YES |
| Governance convergence N09/N10 | `aa02d2a`, `fe53a64` | YES |

Owner-requirement inventory from CP-G4-003..006 confirmed in source (spot-verified): AudioRecord
frame source (`voice/`), ConsoleVoiceCoordinator, system STT/TTS engines, chat composer with
persisted draft, `DrawerWidthPolicy` (2/3 hard max), 75-key five-language resources, QR parser +
nonce replay store (`pairing/`), MockTowerPairingClient. No production pairing endpoint invented.

## 3. Cloud build truth (GitHub API read-back, this session)

| Commit | `android-debug-build` | Run | Note |
| --- | --- | --- | --- |
| `090040d` | SUCCESS | 41469245134 | |
| `adbc16c` | SUCCESS | 41470976809 | |
| `749e746` | SUCCESS | 41472352299 | last fully green pair (build+E2E started) |
| `f7bde56` | **FAILURE** | 41473125545 | **LINT GATE** (new regression) |
| `aa02d2a` | **FAILURE** | 41473174634 | same |
| `fe53a64` | **FAILURE** | 41473331185 | same |

**NEW REGRESSION FOUND (not present in any prior report):** the pairing wave 1 commit `f7bde56`
broke the blocking lint gate. Exact error (job log 125372321561):

```
Lint found 2 errors, 35 warnings. First failure:
app/src/main/java/com/hermestower/voiceconsole/pairing/PairingPayload.kt:114: Error:
Call requires API level 33, or core library desugaring (current min is 30):
java.net.URLDecoder#decode [NewApi]
```

Both error instances are the two `URLDecoder.decode(String, Charset)` calls on line 114 (the
`Charset` overload exists only from API 33; minSdk is 30). Classification: APPLICATION_DEFECT
(gate-blocking). Repair scheduled as the immediate next source action in this same session.

## 4. Cloud E2E truth (runs + artifacts physically parsed this session)

### 4.1 Runs

| Commit | `android-e2e` run | State at read-back |
| --- | --- | --- |
| `adbc16c` | 41470976708 | completed, failure — **14/30 failed**, artifacts parsed |
| `f7bde56` | 41473125636 | **ZOMBIE** — `in_progress` >12 h despite 120-min job timeout; job started 15:37Z, zero updates since |
| `aa02d2a` | 41473174514 | ZOMBIE — same signature |
| `fe53a64` | 41473331031 | ZOMBIE — same signature |

Zombie classification: WORKFLOW_INFRASTRUCTURE_DEFECT (runner/queue anomaly, zero evidence
produced — no artifacts exist for any of the three). They are queued for cancellation with this
receipt as justification; cancellation deletes nothing (no artifacts).

### 4.2 Determinism across runners (re-verified)

- `213182a` (run 41459906172) and `afe3e08` (run 41461749711): **IDENTICAL 13-failure sets** —
  the `Error in …` lines are byte-identical; the only whole-file diff is the `Time:` line.
  Two identical failures across different runners = determinism, and determinism is NOT a
  license to ignore: each failure is individually classified in the CP-G4-007 ledger.
- `adbc16c` (run 41470976708): the same 13 **plus one new failure** `e2e_01_02_…` — analyzed
  below as selector rot caused by the CP-G4-006 console redesign (caption removed), NOT an
  emulator flake.

### 4.3 The 14-failure set at `adbc16c` (exact, from instrumentation output)

| # | Test | Error signature |
| --- | --- | --- |
| 1 | e2e_01_02 freshInstallAndFirstLaunch | timeout `waitForText("wake · inject text · stop voice")` — string NO LONGER EXISTS in source (removed at `adbc16c`) |
| 2 | e2e_03_04 onboardingWithoutCredentials | timeout at `waitForText` after drawer navigation |
| 3 | e2e_05_06_07 openStudio | same |
| 4 | e2e_08 readBackSavedProfile | same |
| 5 | e2e_09_10 mockConnectionTest | same |
| 6 | e2e_19_20 reconnect_validatesQueuedRequest | `AssertionError: queued request was never validated and delivered after reconnect` |
| 7 | e2e_21_22 openModelManager | timeout at `waitForText` after drawer navigation |
| 8 | e2e_23 chooseManualMode | same |
| 9 | e2e_31_33 runQuickSelfTest | timeout `waitForText("Run Quick")` after drawer navigation |
| 10 | e2e_32 runFullSelfTest | same |
| 11 | e2e_34 exportSecretCleanReport | same |
| 12 | e2e_35_36 switchMarbleLight… | `Failed to inject touch input` — `MARBLE LIGHT` has ZERO nodes |
| 13 | e2e_37 enableReducedMotion | same for `Reduced motion` |
| 14 | e2e_43_44 clearSensitiveValues | timeout at `waitForText` after drawer navigation |

### 4.4 Root-cause analysis (evidence-based, this session)

**Pattern A — 12 failures, ONE root cause (frozen navigation clock):**
Every failing test starts with `openDrawerAndNavigate(...)` and then times out waiting for the
destination screen content. Every passing test uses direct `graph()` calls without UI
navigation. Evidence chain:
1. `E2ECommon.freezeAnimationsForTest()` sets `mainClock.autoAdvance = false` (needed because the
   Oracle Core animates forever).
2. The drawer-row click itself succeeds (`openDrawerAndNavigate`'s own `waitUntil` finds the
   drawer-item text, `performClick` fires no error).
3. `navController.navigate()` starts a navigation-compose `AnimatedContent` transition which is
   driven by the compose animation clock — frozen, it never advances; the destination screen
   semantics never appear. `waitForText` then fails in real time (deterministic on every runner).
4. e2e_35_36 proves it: `waitForText("Appearance")` passes (drawer item matches) yet
   `MARBLE LIGHT` — hard-coded in `AppearanceScreen.kt:99`, in a fully-composed `Column` — has
   **zero semantics nodes**: the Appearance screen was never composed.
Classification: **TEST_IMPLEMENTATION_DEFECT** (the test harness froze the clock globally and
then waited on a clock-driven transition). NOT an application defect: production devices never
run with a frozen clock. Repair = bounded virtual-time advancement (`mainClock.advanceTimeUntil`)
after navigation + per-destination screen anchors — no sleeps, no logic bypass.

**Pattern B — 1 failure (selector rot):** `wake · inject text · stop voice` no longer exists in
source; the CP-G4-006 console hero redesign replaced it with the PTT medallion/composer/engine
truth row. Classification: **TEST_IMPLEMENTATION_DEFECT** (stale selector after an
Owner-authorized UI change). Repair = pin to the current console anchors.

**Pattern C — 1 failure (real behavioral question):** e2e_19_20 queue-reconnect: after
`clearFault()`, neither a DELIVERED OWNER turn nor a TOWER reply appeared within 60 s. This is
the pre-existing "queue-reconnect question" from Graph 3 — now it must be resolved by inspecting
the real state authority (queued delivery state, reconnect event, idempotency, freshness,
final UI status). Classification pending physical inspection in CP-G4-007.

**Environment anomalies (recorded, not failure classifications):** final screenshot shows
"Process system isn't responding" dialog on the launcher (software-rendered emulator under
suite load); the separate `Capture failure evidence` step again produced empty artifacts
(emulator already torn down — known workflow defect, in-script capture compensates).

## 5. Artifacts verified this session

- `hermes-voice-console-e2e-results-adbc16c…` (325,950 B): instrumentation-output.txt
  (30 tests, 14 failures), e2e-logcat-tail.txt (1.6 MB), e2e-final-screen.png (320×640).
- Same artifact family for `afe3e08` (332,917 B) and `213182a` — all parsed.
- Failure-evidence artifacts (260 B) = empty files (workflow defect documented above).

## 6. Current Next Exact Action (authoritative for this session)

1. Commit this read-back receipt (evidence-only, no source change) → push → remote read-back.
2. Repair the lint-gate regression (`PairingPayload.kt` NewApi) → local unit tests + lint →
   commit → push → verify cloud build green again.
3. Cancel the three zombie E2E runs (zero artifacts; justification = §4.1).
4. **E2E-ROBUSTNESS (CP-G4-007)**: create the E2E Failure Ledger with per-failure
   classification (this receipt §4.4 supplies the evidence base), implement repairs per Owner
   §3 rules (virtual time, semantic anchors, no sleeps), narrow rerun, full cloud E2E rerun,
   final verdicts. No additional product scope until the ledger is closed.
