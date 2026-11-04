# GRAPH4 CONTINUATION READBACK — LATEST (2026-11-04)

Directive: GRAPH 4 NEXT CONTINUATION (§0 FIRST EXACT ACTION).
Mission mode: `CONTINUE_FROM_LATEST_VERIFIED_REMOTE_TIP`.
Method: fresh local + remote + cloud read-back performed at 2026-11-04 08:47–08:56 UTC.
No SHA from narrative history was assumed; every tip below was re-read live.

## 1. Branch / HEAD / working tree

| Item | Value |
| --- | --- |
| Active local branch | `graph-4/real-voice-pairing-archive-e2e` |
| Local HEAD | `c5b2cc947fd25513cc0ec193cbae3412fc747325` |
| Remote branch | `refs/heads/graph-4/real-voice-pairing-archive-e2e` |
| Remote HEAD (ls-remote) | `c5b2cc947fd25513cc0ec193cbae3412fc747325` — IDENTICAL to local |
| Ahead / behind after fetch | 0 / 0 — no remote work to advance to; workspace NOT behind |
| Working tree | clean (no uncommitted changes) |
| `main` | `e7ab101ecde0dd093edd27b94bbb969822063d26` — untouched (not modified, not merged) |

## 2. Latest cloud build

| Item | Value |
| --- | --- |
| Latest `android-debug-build` | run **41726726210**, SUCCESS, @ `c5b2cc9` (2026-11-03T21:59:22Z) |
| Artifacts | debug APK `13274248376` (56.9 MB zipped universal debug), lint report `13275861099`, unit test results `13275557355` |
| Build health at tip | compile GREEN, unit gate GREEN, lint produced |

## 3. E2E runs — actual results (directive §2)

### 3.1 Completed experiments

| Exp | Config | Run | Result |
| --- | --- | --- | --- |
| TEST-B | `e2e_35_36` alone, CI_LIFECYCLE_PROFILE | 41718228168 @ 3893e88 | EXECUTED CLEANLY — numtests=1, probe `ciLifecycleProfile=true` (21:48:02), residual REPRODUCED with profile engaged: `Unable to destroy activity … route=appearance` at 21:51:15 |
| TEST-A | single test, NORMAL profile | dispatch died pre-script | runner `Broken pipe` after 15.3-min emulator boot — `E2E_BLOCKED_EMULATOR_ENVIRONMENT` |
| TEST-C (1st dispatch) | whole class + LIFECYCLE | 41726653270 @ a0a5357 | **DIED PRE-INSTALL** — `adb: failed to install app-debug.apk: Broken pipe (32)` at 22:21:18; instrumentation never started |

**TEST-C actual result (as directed, "read the actual result of TEST-C"):** the first
dispatch never executed (`E2E_BLOCKED_EMULATOR_ENVIRONMENT`). A re-dispatch has been
issued (§3.2). Accidental breadth evidence exists from the push-triggered suite run:

| Push-run 41726726187 @ c5b2cc9 (default inputs: NORMAL profile, full suite) |
| --- |
| numtests=31; suite crashed at the 3rd started test (`e2e_35_36`, 22:48:01) |
| Failing entry: `NavBackStackEntry(…) route=settings` — THIRD distinct route after `appearance` (TEST-B, ledger samples) — residual is route-independent |
| Same signature: `IllegalStateException: State must be at least CREATED to move to DESTROYED, but was INITIALIZED` → process crash → suite abort |
| Probe lines in logcat: `HermesMotionProfile: ciLifecycleProfile=false` (default profile, as expected for a push run) |

### 3.2 Experiments dispatched in this continuation (all @ `c5b2cc9`, all LIFECYCLE)

| Exp | Config | New run | Status at read-back |
| --- | --- | --- | --- |
| TEST-C (re) | whole `E2EAppearancePersistenceTest` (31 tests) | **41802826358** | in_progress |
| TEST-D | complete instrumentation suite | **41802836756** | in_progress |
| TEST-E | `E2ELifecycleProfileTest` — normal appearance navigation ×20, one session, REAL UI path (drawer→hub→row, physical clicks), Assume-gated skip under NORMAL | **41802846917** | in_progress |

Results will be recorded in `E2E_LIFECYCLE_ROOT_CAUSE_REPORT.md` §4 (each row: run,
commit, profile, class filter, numtests, probe, test result, crash, lifecycle state,
environment state, typed classification) when the runs complete.

### 3.3 Standing disposition (unchanged, directive-conformant)

- `CLOCK_FREEZE_NOT_SUFFICIENT_CAUSE` — TEST-B reproduced the residual WITH the
  profile engaged (probe-verified). The frozen clock is excluded as the mechanism.
- `PHYSICAL_XIAOMI_RECOVERY_REQUIRED` — set 2026-11-04 §4d; the physical Xiaomi
  arbitrates the residual and the visual profiles.
- TEST-C/D/E remain in the matrix as breadth/confirmation evidence. They do NOT
  block dependency-released product nodes (directive §2).

## 4. String count and untranslated-literal inventory

| Item | Value |
| --- | --- |
| Locale string parity | **362 `<string>` + 1 `<plurals>` × 5 locales** (en/it/es/fr/de — counted live from `res/values*/strings.xml`) |
| Audit source of truth | `docs/governance/LOCALIZATION_LITERAL_AUDIT.md` (235 literals measured & classified at wave 1; re-scanned after every UI wave) |
| Migrated so far | Wave 1 (navigation + Settings hub, commit 39cc422), Wave 2 batches 1–3 (Self-Test, Diagnostics, About & Safety, History, QR, Voice & Audio, Local Intelligence; commits 34f92c2→a0a5357) |
| Remaining inventory (live grep) | `TowerStudioScreen.kt` ≈ 39 `Text("…")` literals; `ModelManagerScreen.kt` ≈ 43; wave 3 = `archival/MockTowerArchive.kt` generated titles; then the Italian completeness gate (directive §4) |

## 5. Current navigation (directive §5 state)

- Drawer = five PRIMARY destinations only: Console, Tower Archive, Connect to Hermes
  Tower, Models and Voice, Settings (secondary section removed, wave 1).
- `SettingsHubScreen` with the six prescribed categories (GENERAL/VOICE/TOWER
  CONNECTION/MODELS/DISPLAY/DIAGNOSTICS); every category opens an EXISTING screen and
  route (zero screen-logic moves); route `settings` registered in `AppRoutes.all`.
- Drawer closes on destination selection; compact-panel policy unchanged
  (60% preferred, 2/3 max, 248–360dp).

## 6. Model Sanctum / Tower Archive / visual implementation

- **Model Sanctum**: `ModelManagerScreen.kt` present (`MODEL SANCTUM` identity kept
  invariant per audit §2); bundled catalog + installer intact; localization of the
  screen body is the open item (§4).
- **Tower Archive**: `ArchiveScreen.kt` + `archival/MockTowerArchive.kt` (deterministic
  mock, SIMULATED labels; wave-3 title localization pending).
- **Visual implementation**: `ui/monumental/` — OracleCore (the only infinite
  frame-clock consumer), OracleCoreModel, MonumentalChrome/Color/Type, BurnIn,
  VisualQuality. CI_LIFECYCLE_PROFILE freezes it via the reduced-motion gate
  (MotionProfile, debug-only, test-harness-set).

## 7. APK artifacts and governance authorities

- APK receipt: `docs/receipts/GRAPH4_DEBUG_APK_RECEIPT.md` (APK @ b1df58a =
  165,029,486 B, sha256 `9275aa222d75411587eb9058bca370e6a0f4dfdcc32c2e21815d8e3f30ffdaa0`,
  run 41621955833; universal debug, minSdk 30 / targetSdk 35).
- Newest APK artifact at tip `c5b2cc9`: artifact 13274248376 (run 41726726210).
- Governance authorities live at tip: `WORK_LOG.md`, `CHECKPOINT_INDEX.md`,
  `E2E_FAILURE_LEDGER.md`, `E2E_LIFECYCLE_ROOT_CAUSE_REPORT.md`,
  `LOCALIZATION_LITERAL_AUDIT.md`, `GRAPH4_DEBUG_APK_RECEIPT.md` (all under `docs/`).

## 8. True Next Exact Action

1. Collect TEST-C/D/E run results (§3.2) → record rows in the root-cause report.
2. ~~Localization wave 2-tail~~ **EXECUTED same day** (TowerStudio 63 `ts_*` keys,
   ModelManager 150 `mm_*` keys, Console dialog 4 `cd_*` keys; 591 keys ×5 locales,
   parity/positional/anchor gates green; zero ordinary-English residuals — audit §5).
3. ~~Wave 3~~ dispositioned: MockTowerArchive titles = SIMULATED_SAMPLE_DATA
   (dataset content under the localized SIMULATED badge), documented in audit §5.
4. Italian completeness gate (directive §4: automated visual-text audit →
   `ITALIAN_UI_COMPLETE`).
5. Then: onboarding (§6), Monumental Tower refoundation (§7–9), real links/downloads
   (§10–13), Tower Gate & Archive (§14–15), full regression (§16), APK/ABI report (§17).

No dependency-released executable node is blocked; the E2E experiments run in the
cloud while product waves proceed (directive §20).
