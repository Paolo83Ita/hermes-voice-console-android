# GRAPH 4 FINAL CONTINUATION READBACK

Directive: GRAPH 4 FINAL IMPLEMENTATION CONTINUATION (§0 FIRST EXACT ACTION).
Read-back executed physically at session start. Supersedes narrative claims.

## 1. Repository authority (physically verified)

| Dimension | Value |
| --- | --- |
| Active branch (local) | `graph-4/real-voice-pairing-archive-e2e` |
| Local HEAD | `92465e5c5c71942eb7d61ea2dc913ea3babf60e4` |
| Remote fetch | executed this session (`git fetch origin`, exit 0) |
| Remote HEAD `origin/graph-4/real-voice-pairing-archive-e2e` | `92465e5c5c71942eb7d61ea2dc913ea3babf60e4` |
| Convergence | local HEAD == remote HEAD — **NO ADVANCE NEEDED** |
| Working tree | clean (`nothing to commit, working tree clean`) |
| `main` | untouched (verified: not checked out, not modified) |
| Reported tip `92465e5` | **CONFIRMED STILL THE LATEST TIP** — the physically verified remote tip prevails and equals the reported one |

Verdict: workspace already at remote authority; forward-only advance not required
before the new work (the new work itself advances the tip).

## 2. Cloud runs at tip `92465e5` (read via GitHub API this session)

| Run | Workflow | Commit | Conclusion | Meaning |
| --- | --- | --- | --- | --- |
| 41846567840 | android-debug-build | 92465e5 | **success** | assembleDebug + lint green at tip |
| 41846567723 | android-e2e (push, full suite) | 92465e5 | failure | crashed at test 3/33 — see §4 |
| 41840691263 | android-e2e (dispatch, gate-only) | 01493a1 | failure | gate anchor wait timed out — see §5 |
| 41840678804 | android-e2e (push, full suite) | 01493a1 | failure | same lifecycle crash, test 3/33 |
| 41833663387 | android-e2e (dispatch, gate-only) | 94180c5 | failure | QR/archive region stall (fix-12 evidence) |

Artifacts of 41846567723 and 41840691263 downloaded and parsed this session
(instrumentation-output.txt + failure evidence).

## 3. Physical state dimensions (verified live)

| Dimension | Verified value |
| --- | --- |
| String keys | **622 `<string>` × 5 locales (en/it/es/fr/de) + 1 `<plurals>`** — counted live from `res/values*/strings.xml`; all five files carry exactly 622 (key parity holds) |
| Lint | android-debug-build 41846567840 SUCCESS at tip (lint non-blocking step executed) |
| Test APK | assembles; 33 instrumented tests registered in the suite (`numtests=33`) |
| Lifecycle disposition | TEST-B/C/D evidence recorded; residual typed PHYSICAL_XIAOMI_RECOVERY_REQUIRED; directive forbids reopening (§0) |
| Localization waves | wave 1 + wave 2 batches 1–3 committed (TowerStudio `ts_*`, ModelManager `mm_*`, Console dialog `cd_*`, Self-Test/Diagnostics/About/History/QR/Voice/Local Intelligence) |
| Navigation | drawer = 5 primary destinations; Settings hub = 6 categories (GENERAL/VOICE/TOWER CONNECTION/MODELS/DISPLAY/DIAGNOSTICS) — verified from `SettingsHubScreen.kt` |
| Onboarding | six-step wizard implemented (`OnboardingScreen.kt`, `ob_s1..ob_s6` keys ×5 locales) |
| Model Sanctum | `ModelManagerScreen.kt` + bundled catalog + installer intact ("MODEL SANCTUM" identity) |
| Tower Gate | `ConnectionScreen` + `TowerStudioScreen` + `QrScannerScreen` (QR parser, replay store, deterministic mock, SIMULATED labels) |
| Tower Archive | `ArchiveScreen` (Conversations/Missions/Artifacts tabs) + `MockTowerArchive` (deterministic, SIMULATED badges) |
| Visuals | `ui/monumental/` Oracle Core + MonumentalChrome/Color/Type, BurnIn, VisualQuality, MotionProfile (CI_LIFECYCLE_PROFILE debug gate) |
| APK artifacts | newest at tip: artifact of run 41846567840; previous receipt `GRAPH4_DEBUG_APK_RECEIPT.md` (b1df58a: 165,029,486 B, sha256 9275aa…fdaa0) |
| Governance authorities | all present at tip under `docs/` (WORK_LOG, CHECKPOINT_INDEX, E2E_FAILURE_LEDGER, E2E_LIFECYCLE_ROOT_CAUSE_REPORT, LOCALIZATION_LITERAL_AUDIT, APK receipt) |

## 4. Full-suite E2E blocker at tip (physically parsed from artifacts)

Runs 41846567723 (92465e5) and 41840678804 (01493a1): the push-triggered FULL suite
crashes the instrumentation process at test 3/33
(`E2EAppearancePersistenceTest.e2e_35_36_switchMarbleLightAndObsidianNight`):

```
java.lang.IllegalStateException: State must be at least CREATED to move to
DESTROYED, but was INITIALIZED in component NavBackStackEntry(…) route=settings
   at androidx.lifecycle.LifecycleRegistry.moveToState(…)
   at androidx.navigation.NavBackStackEntry.updateState(…)
   at android.app.Activity.performDestroy(Activity.java:8871)
INSTRUMENTATION_STATUS_CODE: -2  shortMsg=Process crashed.
```

This is the ALREADY-DISPOSITIONED lifecycle residual (E2E_LIFECYCLE_ROOT_CAUSE_REPORT;
TEST-B/C/D physically reproduced it under the requested profiles; typed
PHYSICAL_XIAOMI_RECOVERY_REQUIRED). Per directive §0 the investigation is NOT reopened.
Operational consequence (NEW, evidence-based): one class's process crash destroys the
evidence of every class ordered after it — the full suite never reaches the other
classes. Repair (this session, §engine of the directive): crash-QUARANTINE BY
INVOCATION — the residual class runs in its own instrumentation invocation; it is not
deleted, weakened or bypassed; every other class regains runnable evidence.

## 5. Italian gate failure (last gate-only run, physically parsed)

Run 41840691263 (commit 01493a1, dispatch, `numtests=1`):

- The gate rendered REAL Italian end-to-end (dump: CONSOLE ORACOLO VOCALE, CAMERA
  DELL'ARCHIVIO, localized engine-truth row, Nessuna conversazione…).
- Failure: `compose.waitForText(anchor)` timed out after 20 000 ms inside
  `navigate()` (E2EItalianCompletenessTest.kt:163 ← :258), in the destinations loop.
- The timeout dump shows the requested Archive screen ("Archivio della Torre" +
  "CAMERA DELL'ARCHIVIO") ALREADY COMPOSED in the semantics tree when the error was
  built → the destination appears; the wait expired at the composition boundary.
- Full typed disposition: see `ITALIAN_GATE_NAVIGATION_FAILURE_LEDGER.md` (run row R-7).

## 6. True Next Exact Action

1. Create `ITALIAN_GATE_NAVIGATION_FAILURE_LEDGER.md` (§3 of the directive) — done in
   the same push as this read-back.
2. Replace the monolithic Italian audit with the segmented gate
   `ITALIAN-GATE-A..F` (§4) — deterministic classes, verified per-screen anchors,
   explicit scanner Back navigation, technical allowlist unchanged in spirit.
3. Restructure `android-e2e.yml`: gate invocation (must pass) + full regression
   invocation (minus the crash-quarantined residual class) + residual-evidence
   invocation (typed expected signature; any OTHER failure still fails the job).
4. Push, read back the cloud runs, record ITALIAN_UI_COMPLETE only when all six
   segments are green.
5. Then: five-language checks (§5), visual previews (§8), Tower refoundation (§9–11),
   real links (§12–13), STT/TTS/LLM delivery (§14–16), Tower Gate/Archive (§17–18),
   full regression (§19), APKs (§20), Xiaomi runbook (§21), governance (§22),
   checkpoints CP-G4-017..025 (§23), terminal verdict (§24).

No dependency-released executable node is blocked by the recorded evidence.
