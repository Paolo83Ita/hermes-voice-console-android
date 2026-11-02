# E2E FAILURE LEDGER — GRAPH 4 CP-G4-007 (E2E-ROBUSTNESS)

Mandate: Owner §2 — every deterministic failure individually classified and repaired or typed.
Evidence base (artifacts physically parsed this session):
- Run 41459906172 @ `213182a` — 13/30 failures
- Run 41461749711 @ `afe3e08` — 13/30 failures, `Error in` set byte-identical to the above
- Run 41470976708 @ `adbc16c` — 14/30 failures (the 13 + e2e_01_02)

Suite: 30 instrumented tests (E2E-01..45 + KeystoreVault ×4), API 34 x86_64 emulator, KVM runner.
Screen evidence: in-script captures (`e2e-final-screen.png`, `e2e-logcat-tail.txt`); the separate
failure-evidence step produced empty files in every run (workflow defect recorded in
CP-G4-006R_CONTINUATION_READBACK.md §4.4 — repair tracked separately, not a test classification).

## Root causes (evidence-established)

**RC-1 — Frozen navigation clock (12 failures).** `freezeAnimationsForTest()` sets
`mainClock.autoAdvance=false`; `navController.navigate()` enters the navigation-compose
`AnimatedContent` transition, which advances only on animation-clock frames. Proof: e2e_35_36
reached `MARBLE LIGHT` (hard-coded, fully-composed scroll Column) with ZERO semantics nodes —
the destination screen never composed; every non-navigation test passed on the same runner.
Classification class: TEST_IMPLEMENTATION_DEFECT.

**RC-2 — Selector rot after Owner-authorized UI changes (3 failures).**
(a) console caption `wake · inject text · stop voice` removed at `adbc16c` (replaced by PTT
medallion + composer + engine truth row); (b) drawer labels localized+renamed at `749e746`
(`Hermes Tower Connection`→`Connect to Hermes Tower`, `Voice Models`→`Models and Voice`).
Classification class: TEST_IMPLEMENTATION_DEFECT.

**RC-3 — Shared-store test pollution + handshake race (1 failure).** One `AppGraph` singleton
per instrumented process: e2e_17_18's flushed OWNER turn (`DELIVERED`, `note==null`) satisfies
e2e_19_20's unscoped `firstOrNull` predicate instantly; the immediately-following CONNECTED
assertion races the real (async) handshake. Classification: TEST_IMPLEMENTATION_DEFECT.
The application flush path itself is PROVEN correct on virtual time by the new
`QueueReconnectFlushReproTest` (real driver + bridge + router + state machine, 193/193 green).

**RC-4 — Accessibility semantics defect (underlies RC-1's MARBLE LIGHT / Reduced-motion
clicks).** `ThemeSwatch` label text carried no click action (only the nested USE/ACTIVE
TextButton did); `SettingRow` exposed the toggle only on the bare Switch. Owner §3 directs:
fix control semantics in the application, never bypass in the test. Classification:
ACCESSIBILITY_SEMANTICS_DEFECT (application-side, repaired app-side).

## Per-failure ledger

| ID | Test (class#method) | Exact error (run 41470976708) | Screen / last good action | Root cause | Classification | Repair | Verdict |
| --- | --- | --- | --- | --- | --- | --- | --- |
| F-01 | E2EFreshInstallTest#e2e_01_02_freshInstallAndFirstLaunch_showWorkingConsole | `ComposeTimeoutException` at `waitForText("wake · inject text · stop voice")` | Console visible ("HERMES" asserted OK); caption no longer exists in source | RC-2a | TEST_IMPLEMENTATION_DEFECT | Re-pinned to current anchors: engine truth row `push-to-talk (system recognizer)` + composer hint `Write a message…` | REPAIRED — awaiting cloud rerun |
| F-02 | E2EFreshInstallTest#e2e_03_04_onboardingWithoutCredentials_mockModeSelected | `ComposeTimeoutException` at `waitForText("Open Hermes Tower Connection Studio")` after drawer nav | Drawer row clicked OK; Connection screen never composed | RC-1 | TEST_IMPLEMENTATION_DEFECT | `openDrawerAndNavigate(dest, anchor)` + bounded `mainClock.advanceTimeUntil(20s)` virtual-time advance | REPAIRED — awaiting cloud rerun |
| F-03 | E2EFreshInstallTest#e2e_05_06_07_openStudio_validateAndSaveMockProfile | same, after drawer nav | drawer item `Hermes Tower Connection` no longer exists post-749e746 | RC-1+RC-2b | TEST_IMPLEMENTATION_DEFECT | anchor advance + label updated to `Connect to Hermes Tower` | REPAIRED — awaiting cloud rerun |
| F-04 | E2EFreshInstallTest#e2e_08_readBackSavedProfile | same | same | RC-1+RC-2b | TEST_IMPLEMENTATION_DEFECT | same | REPAIRED — awaiting cloud rerun |
| F-05 | E2EFreshInstallTest#e2e_09_10_mockConnectionTest_connectedOnlyAfterHandshake | same | same | RC-1+RC-2b | TEST_IMPLEMENTATION_DEFECT | same | REPAIRED — awaiting cloud rerun |
| F-06 | E2EVoiceFlowTest#e2e_19_20_reconnect_validatesQueuedRequestBeforeDelivery | `AssertionError: queued request was never validated and delivered after reconnect` (131.5 s ≈ 60+60 s full predicate waits) | shared store polluted by earlier tests' OWNER DELIVERED turns; CONNECTED raced the handshake | RC-3 | TEST_IMPLEMENTATION_DEFECT | predicates scoped `it.id > baselineId` (STRONGER: must be this test's turn); bounded observable-state wait for CONNECTED before asserting; flush logic proven by new JVM test | REPAIRED — awaiting cloud rerun |
| F-07 | E2EModelManagerTest#e2e_21_22_openModelManager_runVoicePackRecommendation | `ComposeTimeoutException` at `waitForText("A. Recommended setup")` | drawer item `Voice Models` renamed `Models and Voice` at 749e746 | RC-1+RC-2b | TEST_IMPLEMENTATION_DEFECT | anchor advance + label updated | REPAIRED — awaiting cloud rerun |
| F-08 | E2EModelManagerTest#e2e_23_chooseManualMode | same | same | RC-1+RC-2b | TEST_IMPLEMENTATION_DEFECT | same | REPAIRED — awaiting cloud rerun |
| F-09 | E2ESelfTestTest#e2e_31_33_runQuickSelfTest_verifyTypedResults | `ComposeTimeoutException` at `waitForText("Run Quick")` | screen never composed | RC-1 | TEST_IMPLEMENTATION_DEFECT | anchor advance | REPAIRED — awaiting cloud rerun |
| F-10 | E2ESelfTestTest#e2e_32_runFullSelfTest_allPassOnEmulator | same | same | RC-1 | TEST_IMPLEMENTATION_DEFECT | anchor advance | REPAIRED — awaiting cloud rerun |
| F-11 | E2ESelfTestTest#e2e_34_exportSecretCleanReport | same | same | RC-1 | TEST_IMPLEMENTATION_DEFECT | anchor advance | REPAIRED — awaiting cloud rerun |
| F-12 | E2EAppearancePersistenceTest#e2e_35_36_switchMarbleLightAndObsidianNight | `Failed to inject touch input` — `MARBLE LIGHT` zero nodes | navigation frozen (RC-1); label node had no click action (RC-4) | RC-1+RC-4 | TEST_IMPLEMENTATION_DEFECT + ACCESSIBILITY_SEMANTICS_DEFECT | anchor advance; ThemeSwatch whole-card `clickable(onClickLabel=label)` app-side | REPAIRED — awaiting cloud rerun |
| F-13 | E2EAppearancePersistenceTest#e2e_37_enableReducedMotion | same for `Reduced motion` | navigation frozen; SettingRow toggle only on bare Switch | RC-1+RC-4 | TEST_IMPLEMENTATION_DEFECT + ACCESSIBILITY_SEMANTICS_DEFECT | anchor advance; SettingRow now row-level `toggleable(Role.Switch)`, Switch display-only (Material accessibility guidance) | REPAIRED — awaiting cloud rerun |
| F-14 | E2EAppearancePersistenceTest#e2e_43_44_clearSensitiveValues_metadataRemains | `ComposeTimeoutException` at `waitForText("Connect Hermes Tower")` | drawer item renamed | RC-1+RC-2b | TEST_IMPLEMENTATION_DEFECT | anchor advance + label updated | REPAIRED — awaiting cloud rerun |

## Repair-integrity statement (Owner §3 compliance)

- No uncontrolled sleep delays introduced; waits are bounded (`advanceTimeUntil(20s)`,
  `waitUntil(15–20s)`, deadline-polling on StateFlows as the suite already did).
- No weakened assertion: drawer navigation now REQUIRES the destination screen to compose
  (compile-time-required `screenAnchor` parameter); queue predicates are STRICTLY scoped to
  each test's own turns; CONNECTED is asserted after an observable-state bounded wait.
- Application repairs are semantics-only (toggleable row, clickable swatch) — no behavior,
  routing, state-machine or storage logic touched; all 28 prior behavioral contracts and the
  193 unit tests remain green.
- Reproducibility evidence: two byte-identical 13-sets across different runners + the 14th
  explained by a source-level string removal = deterministic, environment-independent.

## Explicit non-classifications

No failure was typed `KNOWN RESIDUAL` or `FLAKE`. The pre-CP-G4-007 usage of those words in
commit messages is superseded by this ledger. No EMULATOR_INPUT_INJECTION_LIMITATION /
EMULATOR_PERFORMANCE_TIMEOUT classification was needed: every failure traced to a specific,
repaired defect. The "Process system isn't responding" dialog visible on the run's final
screenshot is recorded as an environment observation (software-rendered emulator under suite
load), not as a test verdict.

## Narrow + complete rerun plan

1. Cloud `android-debug-build` @ repair commit must be SUCCESS (build+lint+unit gate).
2. Cloud `android-e2e` @ same commit = the complete-suite rerun. Success criterion
   (typed in the CP-G4-007 checkpoint): 30/30 PASS, or any residual individually re-classified
   in this ledger with a new materially-different repair attempt (max 3 per root-cause class).
