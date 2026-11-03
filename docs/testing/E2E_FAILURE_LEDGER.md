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

---

## ADDENDUM — CP-G4-007 attempts 2–3 (runs 41485512432 / 41503065076 / 41514356403 / 41527072357)

Per-failure verdicts after the instrumented reruns (Owner §2: no residual without evidence):

| Failure class | Evidence chain | Final classification | Disposition |
| --- | --- | --- | --- |
| e2e_19_20 queue-reconnect (F-06) | Run 41527072357 FLUSH diagnostics: `FLUSH: trigger state=TRANSCRIBING active=true queued=1` → `skip: pipeline active`. The pipeline's offline-queue branch called `transition(REQUEST_QUEUED)` while TRANSCRIBING — an ILLEGAL edge silently ignored since Graph 1 — so the state stayed pipeline-active and EVERY reconnect flush skipped (turns id=3 and id=6 both stuck QUEUED). The JVM repro passed only because virtual time had already drained the display delay. | **APPLICATION_DEFECT (STATE_SYNCHRONIZATION_DEFECT)** — latent since Graph 1; exposed only by the instrumented E2E | **REPAIRED**: TRANSCRIBING→REQUEST_QUEUED and IDLE→REQUEST_QUEUED edges added (ConsoleStateMachine), pinned by QueueStateMachineTest (4 tests); flush trigger/begin/skip/error events are now typed and redaction-safe |
| e2e_01_02 selector rot (F-01) | Run 41503065076: test no longer fails | TEST_IMPLEMENTATION_DEFECT | **CLOSED — repaired (selector re-pinned to current anchors)** |
| 11 navigation failures (F-02..05,07..14) | Attempt 1 (virtual-time advance) removed the never-composes class; run 41514356403's anchor-timeout semantics dump showed console-only texts after a "successful" row click → the drawer-open transition frozen at its closed offset leaves rows at OFF-SCREEN window coordinates where touch injection is silently dropped; clicks before layout placement land on unplaced nodes. Attempt 3 (78b92c2) pumps the drawer-open animation until the row is physically on-screen (physical click preserved per §3.4), falls back to the accessible OnClick semantics action (§3.3), adds a post-anchor settle window (layout before clicks) and dual-clock pumping waits. | TEST_IMPLEMENTATION_DEFECT (frozen-clock harness physics) + candidate EMULATOR_INPUT_INJECTION_LIMITATION if any residual remains after 78b92c2's cloud run | Attempt 3 of 3 executed; verdict = cloud E2E at 78b92c2+state-fix commit |
| e2e_31/32/34 zero-results | Run 41514356403: awaitCompletion polled before the run coroutine flipped `running` | TEST_IMPLEMENTATION_DEFECT (startup race) | **REPAIRED** (wait-for-start then wait-for-end) |

Rule 3 compliance: three materially different repair attempts per class were used only where
stated; no assertion was weakened at any point (predicates were tightened, anchors made
compile-time-required, diagnostics added). The "Process system isn't responding" dialog on the
software emulator remains an environment observation, not a verdict.

---

## CP-G4-007 VERDICT (run 41551630218 @ 26e2e8d — full cloud E2E)

**Trajectory across CP-G4-007:** 14 deterministic assertion failures → 8 → **0 assertion
failures** (all repaired tests green through the run), with the suite aborting at test #14
(e2e_35_36) on a process crash — NOT an assertion failure.

**The residual, typed with complete evidence (Owner §2/§3):**

- **Class: EMULATOR_INPUT_INJECTION_LIMITATION** (3 materially different repair attempts
  consumed: ① virtual-time advancement, ② semantic hasClickAction selectors + bounded
  advanceTimeUntil/advanceTimeBy pumping, ③ coordinate-free accessible OnClick activation).
- Mechanism: `freezeAnimationsForTest()` (required — the Oracle Core animates infinitely and
  Espresso-idle would hang) freezes the compose animation clock; the M3 drawer + navigation-
  compose AnimatedContent transitions advance only on that clock. The semantics activation
  provably fires `navigate()` (run 41551630218 logcat: the NavBackStackEntry for route
  `appearance` was created), but the entering content never composes on this emulator
  configuration, so the anchor cannot appear and the entry stays INITIALIZED.
- Consequence recorded: at test teardown the stuck INITIALIZED entry makes activity destroy
  throw `IllegalStateException: State must be at least CREATED to move to DESTROYED`
  (navigation-compose 2.8.5 internal lifecycle move) → process death → suite abort at #14.
  This is a library-internal lifecycle race surfaced by the frozen clock, not application
  logic (the console, routing, queue, archive and voice paths are all exercised green).
- Physical-click truthfulness: row clickability remains asserted (hasClickAction matcher);
  the accessible activation is the screen-reader path (Owner §3.3), never a logic bypass.
- **Recovery paths (Owner decision):** (a) physical Xiaomi qualification — no frozen clock,
  real touch; (b) a CI emulator variant with animation-free console build (dedicated
  resource flag) — a separate, named node if authorized; (c) navigation-compose upgrade node
  (pinned 2.8.5) re-evaluating the INITIALIZED-entry teardown race.

**Closed failures (repaired + verified green in run 41551630218):** e2e_01_02 (selector rot),
e2e_19_20 (APPLICATION_DEFECT — state machine queue edges, verified delivering), e2e_23,
e2e_31_33/32/34 (SelfTest startup race), and all anchor/wait mechanics for the classes they
cover. 225/225 JVM unit tests green; lint 0 errors / 34 warnings; cloud debug-build SUCCESS
at every repair commit.

---

## ADDENDUM — post-verdict read-back (G4-N15, 2026-11-03): runs at e9abee9 / 32e35e0 / b1df58a

Cloud android-debug-build SUCCESS at all three commits (41621063944 / 41621713767 /
41621955833). Cloud android-e2e executed at each (41621063936 / 41621713747 / 41621955951)
and aborted with the SAME typed residual, signature identical to the CP-G4-007 verdict:

`java.lang.IllegalStateException: State must be at least CREATED to move to DESTROYED,
but was INITIALIZED in component NavBackStackEntry(…) route=appearance` → process crash
(`INSTRUMENTATION_RESULT: shortMsg=Process crashed.`) → suite abort.

New deterministic facts (4th/5th/6th samples of the residual after 41551630218 @26e2e8d
and 41615537760 @ed50b21):

1. **Position independence.** JUnit class/method ordering is reflection-dependent and
   varied between runs: in these three runs `E2EAppearancePersistenceTest` ran FIRST and
   the abort landed at suite test #3 of `numtests=30` (e2e_35_36 was its 3rd method,
   after e2e_41_42 and e2e_40 passed green). In 41551630218 the same crash landed at #14
   after 13 green tests. The MECHANISM is deterministic (any run that leaves the
   route=appearance entry stuck INITIALIZED at teardown crashes the process); only the
   crash POSITION varies with ordering. No new test, selector, screen or assertion is
   implicated: e2e_40 and e2e_41_42 (recreate/restart paths, which drive the entry
   lifecycle forward before teardown) pass green in the same class.
2. **INFRASTRUCTURE FINDING — the E2E verdict channel was corrupted by construction.**
   The emulator-runner executes the `script:` input ONE LINE PER `sh -c` invocation.
   The multi-line `if grep -q 'OK (…)' … fi` verdict block (android-e2e.yml) therefore
   always died with `/usr/bin/sh: 1: Syntax error: end of file unexpected (expecting
   "fi")`, exit 2 — every job failed at the verdict line REGARDLESS of suite outcome,
   and even a fully green suite would have failed the job without a truthful marker.
   This is the same line-by-line execution class as the historical dash/`set -o pipefail`
   bug documented in the script comment. Repair (same commit as this addendum): verdict
   collapsed to a single POSIX-safe line with identical semantics, with the evidence
   recorded in a comment. The three runs above remain valid RESIDUAL evidence (the
   process crash is visible in the instrumentation output itself, before the verdict
   line), but their job-level "failure" conclusion was not yet a clean typed verdict;
   future runs will print INSTRUMENTATION_PASSED / INSTRUMENTATION_FAILED_OR_INCOMPLETE
   truthfully.
3. Recovery paths unchanged and still awaiting the Owner decision: (a) physical Xiaomi
   qualification; (b) authorized animation-free CI console variant (separate named node);
   (c) navigation-compose upgrade node re-evaluating the INITIALIZED-entry teardown race.
   No assertion was weakened; no app source changed in this read-back node.

### Verdict-channel repair VALIDATED on cloud (run 41632784491 @ 1fa7829, 2026-11-03)

- The single-line verdict executed as ONE `sh -c` invocation and printed the truthful
  marker `INSTRUMENTATION_FAILED_OR_INCOMPLETE` + exit 1 — no `Syntax error` anywhere in
  execution (the only textual match is the evidence comment inside the script dump).
- The residual fired again with the identical signature (7th deterministic sample):
  e2e_35_36, `IllegalStateException … route=appearance`, process crash, abort at #3 of
  numtests=30; e2e_41_42 and e2e_40 green before it. Debug-build @ 1fa7829 SUCCESS
  (run 41632784469).
- Net effect of the repair: from this run forward every android-e2e job conclusion carries
  a truthful typed marker, so the Owner's recovery-path decision (a/b/c) can rest on clean
  evidence. The residual itself remains open and decision-gated.

### CP-G4-016 addendum — instrument-args transport defect + system collapse + route variance (2026-11-04)

Read-back of the CP-G4-016 controlled re-runs (all @ b0be658) and of the three push
runs b0be658/5319da3/543bc2d:

1. **NEW INFRA DEFECT — instrument arguments never transmitted** (same per-line `sh -c`
   execution class as the verdict-channel bug above): the workflow built
   `INSTRUMENT_ARGS` across three lines and consumed it on a fourth; the emulator-runner
   runs each line as a SEPARATE `sh -c`, so the variable was always unset at consumption.
   Evidence: dispatch runs 41657209419 (NORMAL) and 41657223032 (LIFECYCLE) both ran the
   FULL suite (`numtests=31`) despite a single-test `-e class …#e2e_35_36…` request, and
   the b0be658 engagement probe logged `HermesMotionProfile: ciLifecycleProfile=false` in
   the LIFECYCLE run's logcat artifact. Consequence: NO dispatched CP-G4-016 experiment
   ever executed with its requested profile/filter; the "TEST-B first sample"
   (41648000740 @ c6476a1) is reclassified as the 8th NORMAL-profile control sample.
   Repair: android-e2e.yml builds AND consumes the args on ONE POSIX-safe line (locally
   simulated for NORMAL/LIFECYCLE × filter/no-filter).
2. **System collapse in both dispatch runs** (`INSTRUMENTATION_ABORTED: System has
   crashed` at suite position 1, first-started test e2e_41_42): `com.android.phone`
   death loop → cascading system ANRs (com.google.android.as, gms.persistent,
   googlequicksearchbox, RoleControllerService) → system_server FATAL
   `ConnectivityModuleConnector.maybeCrashWithTerribleFailure` ("Lost network stack" —
   the message itself declares it a side effect of an earlier crash). The push run of
   the SAME commit survived to position 7 → environmental, classified
   E2E_BLOCKED_EMULATOR_ENVIRONMENT. Not a residual regression; not counted as a test
   verdict.
3. **Route variance of the residual (9th/10th/11th control samples)**: push runs
   41657188171 (b0be658) / 41657320566 (5319da3) / 41662377976 (543bc2d) all show the
   IDENTICAL teardown class at `e2e_43_44_clearSensitiveValues_metadataRemains` with
   `route=tower_studio` (previously route=appearance @ e2e_35_36). The residual is
   position- AND route-independent: ANY frozen-clock navigation can leave its entry
   un-promoted. In 41657188171, e2e_35_36 additionally failed EARLY with
   `ComposeTimeoutException` (15 s) in openDrawerAndNavigate — the wave-1 harness
   demanded hub-only labels as drawer rows (repaired in the same commit as this
   addendum: drawer-then-hub in both variants; variant A hub waits on pumpUntil;
   e2e_03_04 label "Connection" → "Current Tower & reconnect").
4. Disposition state: the experiment matrix (root-cause report §4) is RESET to PENDING
   on the repaired channel. Recovery paths unchanged; no assertion weakened by the
   repairs (harness plumbing + workflow plumbing only; one test label updated to the
   real product row that carries its anchor).
