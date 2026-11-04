# E2E LIFECYCLE ROOT CAUSE REPORT — the route=appearance INITIALIZED→DESTROYED residual

CP-G4-016 deliverable (directive §2, OWNER_DECISION_E2E_RECOVERY). Mechanism analysis is
final; the experiment matrix is filled with cloud-verdict evidence as runs complete.
No assertion was weakened; no application navigation/state/control/persistence/semantics
was changed in this work.

## 1. The failure, typed

`java.lang.IllegalStateException: State must be at least CREATED to move to DESTROYED,
but was INITIALIZED in component NavBackStackEntry(…) route=appearance` → process crash →
suite abort. 7 deterministic samples before this node (41551630218, 41615537760,
41621063936, 41621713747, 41621955951, 41632784491, 41638599014), position-independent
(aborted at suite #14 with 13 green, at #3 with 2 green — JUnit reflection ordering).

## 2. Mechanism (source-level analysis, this session)

Navigation-compose (2.8.5) gives each NavBackStackEntry a Lifecycle that is created in
INITIALIZED and advances to CREATED when the entry's content actually composes inside the
NavHost. Teardown (activity destroy / test-rule finish) moves EVERY entry to DESTROYED.

The E2E harness (variant A) MUST freeze the compose clock (`mainClock.autoAdvance=false`)
because the Oracle Core animates infinitely on the frame clock — without freezing, the
test rule never reaches idle. Under the frozen clock:

1. `navigate(appearance)` fires → the entry is created in INITIALIZED.
2. The NavHost AnimatedContent enter transition advances ONLY on the frozen clock; the
   harness pumps bounded virtual time until the destination ANCHOR composes (that is how
   navigation is asserted), but nothing guarantees the transition's terminal frame: the
   entry's lifecycle reaches CREATED only via the transition/state machinery that runs on
   the clock the harness partly controls and the framework partly schedules on its own
   choreographer frames — on this software-emulated configuration the entry can remain
   INITIALIZED after its content was observed.
3. At teardown, the framework moves the INITIALIZED entry toward DESTROYED;
   `LifecycleRegistry` forbids INITIALIZED→DESTROYED skipping CREATED →
   IllegalStateException → process death → suite abort.

Corroborating evidence: the two Appearance-class tests that exercise RECREATE/RESTART
(e2e_40, e2e_41_42) pass green in every run — recreate/restart drives the entry through
CREATED — while the plain navigation test (e2e_35_36) is the one that leaves an
INITIALIZED entry at teardown. The race is framework-internal (navigation-compose
lifecycle move), SURFACED by the frozen clock, not an application-logic defect: the
console, routing, queue, archive and voice paths all execute green in the same runs.

## 3. The authorized remedy — CI_LIFECYCLE_PROFILE (directive §2 variant B)

The app's ONLY infinite frame-clock consumer is the Oracle Core
(`rememberAnimationFrameTimeMs(running = …)`; MeanderBand is static geometry; TowerCore
is legacy and not instantiated). The profile reuses the app's own reduced-motion gate:

- `MotionProfile.ciLifecycleProfile` (TEST_ONLY) — set by MainActivity on DEBUG builds
  when launched with `hermes.ci_lifecycle_profile`; release builds can never set it.
- Effect: the Oracle Core frame clock stops (static frame — the exact state the existing
  reduced-motion tests already pin). Navigation, state machine, controls, persistence,
  semantics and business logic are untouched BY CONSTRUCTION (no code path differs).
- Harness: with no infinite animation, the compose clock is left ADVANCING; drawer and
  NavHost transitions complete in real time; back-stack entries reach CREATED; navigation
  uses REAL physical clicks through the real UI (no destination logic is ever called
  directly). Variant A behavior is byte-for-byte unchanged when the argument is absent.
- Workflow: android-e2e.yml gains workflow_dispatch inputs (`e2e_profile`,
  `e2e_class_filter`); push-triggered runs stay NORMAL-profile control evidence.

## 4. Experiment matrix (cloud, API 34 x86_64 emulator)

| ID | Scope | Profile | Expected | Cloud run | Verdict |
| --- | --- | --- | --- | --- | --- |
| TEST-A | e2e_35_36 alone | NORMAL_VISUAL_PROFILE | residual reproduces (control) | 41645556871 @ bd1606c = compile fail (harness defect, repaired); 41647987018 @ c6476a1 = install infra failure (`Can't find service: package`, zero tests); 41657209419 @ b0be658 = **args-transport defect + system collapse** (§4c, E2E_BLOCKED_EMULATOR_ENVIRONMENT) — no clean isolated control has executed yet | PENDING (re-dispatch on the repaired channel) |
| TEST-B | e2e_35_36 alone | CI_LIFECYCLE_PROFILE | passes with real navigation | **41718228168 @ 3893e88: EXECUTED CLEANLY — numtests=1 (filter AND `-e lifecycleProfile true` transmitted; probe `ciLifecycleProfile=true` at 21:48:02); RESIDUAL REPRODUCED WITH THE PROFILE ENGAGED** — `Unable to destroy activity … IllegalStateException … route=appearance` at 21:51:15 (§4d) | **REPRODUCED (profile-engaged, probe-verified)** |
| TEST-C | E2EAppearancePersistenceTest whole class | CI_LIFECYCLE_PROFILE | passes | 1st dispatch 41726653270 @ a0a5357 = **died pre-install** (`adb install … Broken pipe (32)` at 22:21:18 — E2E_BLOCKED_EMULATOR_ENVIRONMENT, instrumentation never started); re-dispatch **41802826358 @ c5b2cc9** in flight. Accidental NORMAL-profile breadth evidence in the meantime: push-run 41726726187 @ c5b2cc9 (default inputs) ran the full suite — numtests=31, crashed at the 3rd started test (`e2e_35_36`, 22:48:01) with the SAME signature on `route=settings` (THIRD distinct route; probe lines in logcat confirm `ciLifecycleProfile=false`, i.e. default profile) | REPRODUCED under NORMAL at class breadth; LIFECYCLE breadth pending re-dispatch |
| TEST-D | complete instrumentation suite | CI_LIFECYCLE_PROFILE | 0 failures | **41802836756 @ c5b2cc9 dispatched** (full suite + `-e lifecycleProfile true`), in flight at read-back time | PENDING |
| TEST-E | normal appearance navigation ×20, one session | CI_LIFECYCLE_PROFILE | no entry-lifecycle crash | **41802846917 @ c5b2cc9: EXECUTED on the repaired channel — numtests=1, filter `E2ELifecycleProfileTest` + `-e lifecycleProfile true` transmitted; the test FAILED BEFORE the soak could exercise navigation: `AppNotIdleException: Looped for 1 iterations over 60 SECONDS — ASYNC_TASKS_HAVE_IDLED` (Espresso idle sync during session startup), NOT the lifecycle residual** (no `Unable to destroy activity` / INITIALIZED→DESTROYED anywhere in the captured evidence) | **INCONCLUSIVE_EMULATOR_IDLE_SYNC** — environment/harness idle-sync limitation, distinct typed outcome; the soak never reached the first iteration on this emulator run |

## 4b. MAJOR FINDING (2026-11-03, run 41648000740 @ c6476a1) — the frozen clock is NOT the sole mechanism

> **SUPERSEDED IN PART by §4c (2026-11-04)**: run 41648000740 did NOT engage the profile
> (args-transport defect — the instrumentation arguments never reached the device; the
> probe would have read `ciLifecycleProfile=false`). This section's environment forensics
> (relaunch/pause/destroy timeouts, the exact destroy-path stack) remain VALID as
> NORMAL-profile evidence; its engagement inference is withdrawn. Retained for the
> forensic record as written:

TEST-B executed with the profile requested and the residual fired with the IDENTICAL
signature at the same test. Environment forensics from the run's logcat artifact:

- The test window (14:35:10 RESUMED → 14:35:59 pause request) contains a **33-second
  frame-skip-free gap** (14:35:10.8 → 14:35:43.8, then only transition-correlated skips):
  a continuously animating Oracle Core would produce frames (and skips) throughout —
  the static-frame profile behavior is consistent with engagement. The controlled re-run
  adds a hard probe (MainActivity logs `ciLifecycleProfile=` on debug builds; the harness
  `check`s `MotionProfile.ciLifecycleProfile` in the variant-B path) so the final matrix
  is judged on PROOF, not inference.
- **Environment pathology measured**: `finishDrawing of relaunch: 25084ms` (e2e_40's
  activity relaunch took 25 s to draw on the software emulator), `Activity pause timeout`
  after 49 s, `Activity destroy timeout`, 33 frame-skips in the tail window. The surface
  pipeline stalls for tens of seconds — NavHost enter transitions can fail to reach their
  terminal frame EVEN IN REAL TIME, leaving an entering entry un-promoted from INITIALIZED.
- **Destroy-path stack (exact)**: `ReportFragment.onActivityPreDestroyed` →
  `LifecycleRegistry.backwardPass` → `NavController.lifecycleObserver(ON_DESTROY)` →
  `NavBackStackEntry.handleLifecycleEvent → updateState → moveToState` — the framework
  moves EVERY back-stack entry straight to DESTROYED when the host dies; an entry still
  INITIALIZED makes `LifecycleRegistry` throw (INITIALIZED→DESTROYED is not a legal move).
  This is navigation-compose 2.8.5's non-defensive teardown meeting an un-promoted entry;
  the un-promotion is the emulator's transition stall, not application logic.
- Consequence for the disposition: the residual is NOT TEST_LIFECYCLE_DEFECT_REPAIRED by
  the profile alone. The candidate dispositions are now NORMAL_PROFILE_EMULATOR_LIMITATION
  (if the controlled re-runs confirm engagement and the crash persists — the emulator
  cannot host even the static-frame profile reliably) or PHYSICAL_XIAOMI_RECOVERY_REQUIRED
  (the physical device arbitrates, as the Owner's option (a) already provides). No
  navigation-compose upgrade has been performed (directive §2: no speculative upgrades).

A CI_LIFECYCLE_PROFILE PASS does NOT physically qualify the NORMAL animated profile —
that remains the physical Xiaomi gate (directive §2), which this report records.

## 4c. TRANSPORT-CHANNEL DEFECT (2026-11-04, read-back of runs 41657209419/41657223032 @ b0be658) — no dispatched experiment ever transmitted its arguments

The controlled re-runs were dispatched at b0be658 (TEST-A NORMAL 41657209419, TEST-B
LIFECYCLE 41657223032) and both died BEFORE any experiment verdict could form. Read-back
of the job logs + artifacts established a NEW infrastructure defect that retroactively
invalidates the experiment matrix as previously filled:

**Defect (same execution class as the verdict-channel bug, §N15)**: the emulator-runner
executes the `script:` input ONE LINE PER `sh -c`. The workflow built
`INSTRUMENT_ARGS=""` on one line, appended `-e lifecycleProfile true` / `-e class …`
in two separate `if` lines, and consumed `$INSTRUMENT_ARGS` on a FOURTH line. Each line
is a separate shell process, so the variable was ALWAYS unset at the consumption line:
`am instrument` ran with EMPTY arguments in EVERY dispatched run of this node.

**Proof (two independent signals)**:
1. `numtests=31` in both dispatch runs although a single-test `-e class …#e2e_35_36…`
   filter was requested (suite size is 31 — verified locally by @Test count) — the filter
   never reached the device; the FULL suite ran.
2. The engagement probe (b0be658) logged `HermesMotionProfile: ciLifecycleProfile=false`
   in the LIFECYCLE dispatch run's logcat artifact — the profile was requested at the
   workflow level but never requested at the instrumentation level. Definitive, no
   inference.

**Consequences for the record**:
- The "TEST-B first sample" (41648000740 @ c6476a1) is RECLASSIFIED: with empty args it
  was a NORMAL-profile FULL-SUITE run whose crash at e2e_35_36 / route=appearance is the
  **8th control sample** — NOT a TEST-B execution. §4b's "33 s frame-skip-free window"
  engagement inference is non-discriminative (variant A's frozen clock produces the same
  static frame) and is withdrawn as engagement evidence; the probe makes engagement
  observable, and it read FALSE.
- Both b0be658 dispatch runs died in a NEW environment failure mode before any test
  verdict: `com.android.phone` death loop → cascading system ANRs (phone/gms/as/search)
  → `ConnectivityModuleConnector.maybeCrashWithTerribleFailure` ("Lost network stack")
  FATAL in system_server → `INSTRUMENTATION_ABORTED: System has crashed` at suite
  position 1 (e2e_41_42 was merely the first-started test). Classified
  E2E_BLOCKED_EMULATOR_ENVIRONMENT (the same class as the 41647987018 install failure).
  The push run of the SAME commit (41657188171) survived to suite position 7 — the
  collapse is environmental, not deterministic.
- **Route variance confirmed (9th/10th/11th control samples)**: the push runs at
  b0be658/5319da3/543bc2d (41657188171/41657320566/41662377976) all crashed with the
  identical teardown class at `e2e_43_44_clearSensitiveValues_metadataRemains` with
  `route=tower_studio` (was: e2e_35_36 / route=appearance). The residual is therefore
  ROUTE-INDEPENDENT as well as position-independent: ANY frozen-clock navigation can
  leave its entry un-promoted. Additionally, in the b0be658 push run e2e_35_36 itself
  FAILED EARLY with `ComposeTimeoutException` (15 s) inside openDrawerAndNavigate — the
  frozen-clock stall surfacing as a typed test failure instead of a teardown crash.
- **Harness defect found by the same evidence (repaired)**: the wave-1 harness waited
  unconditionally for the destination label as a DRAWER row before the hub fallback —
  but Appearance/Connection/Self-Test Center are no longer drawer rows (CP-G4-017), so
  every hub-label navigation could only time out. Repaired: drawer-row attempt first,
  hub path on miss; variant A hub waits use pumpUntil (virtual time).

**Repairs (this commit)**: (1) android-e2e.yml — the instrument arguments are built AND
consumed on ONE POSIX-safe line (construction+consumption share the line; verified by
local simulation for NORMAL/LIFECYCLE × filter/no-filter). (2) E2ECommon.
openDrawerAndNavigate — drawer-then-hub in both variants, hub-row wait on the variant-
appropriate clock. (3) E2EFreshInstallTest e2e_03_04 — label "Connection" → the real
product row "Current Tower & reconnect" (the substring match would have landed on
"Connection Studio (advanced)", the wrong route for this test's anchor).

The matrix is RESET to PENDING on the repaired channel: no CI_LIFECYCLE_PROFILE-engaged
run exists yet; §4b's disposition narrowing stands as HYPOTHESIS until the controlled
re-runs execute with the probe reading TRUE.

## 4d. TEST-B DECISIVE VERDICT (2026-11-04, run 41718228168 @ 3893e88) — the residual fires with the profile ENGAGED: the frozen clock is excluded as the mechanism

The transport repair (§4c) held: the dispatch ran with `E2E_PROFILE: LIFECYCLE` and the
instrumentation output shows **numtests=1** — for the first time in this node the
class-filter AND the profile argument reached the device, and the experiment executed as
designed (single test, isolated session).

Timeline from the run's logcat artifact:
- 21:48:02 `HermesMotionProfile: ciLifecycleProfile=true` — the DEBUG-only probe proves
  the profile was engaged in the app process (MainActivity read the property through the
  app's own reduced-motion gate; the harness `check()` in the variant-B path passed,
  since navigation proceeded).
- The test ran to completion of its navigation work (real frames; physical drawer
  clicks; the anchor composed — otherwise the failure would have been a
  ComposeTimeoutException, not a destroy crash).
- 21:51:15 `FATAL EXCEPTION: main — RuntimeException: Unable to destroy activity
  {com.hermestower.voiceconsole/com.hermestower.voiceconsole.ui.MainActivity}:
  java.lang.IllegalStateException: State must be at least CREATED to move to DESTROYED,
  but was INITIALIZED in component NavBackStackEntry(…) route=appearance` — the SAME
  teardown race, at `ActivityThread.performDestroyActivity`, with the profile ENGAGED.

Consequences:
1. §2's frozen-clock mechanism analysis is REFUTED as the sole mechanism: with real
   frames and the ornamental clocks frozen in-app, the entering NavBackStackEntry still
   never reaches CREATED on this software-emulated environment. The un-promotion is an
   emulator environment property (frame/lifecycle pipeline stalls measured in §4b:
   finishDrawing of relaunch 25 s, pause timeouts), not an application defect and not a
   test-configuration defect.
2. Disposition per §5: **PHYSICAL_XIAOMI_RECOVERY_REQUIRED** — the directive's rule is
   explicit ("if any CI_LIFECYCLE_PROFILE experiment still fails, the residual is deeper
   than the animation clock and the physical device must arbitrate"). TEST-C/D/E remain
   in the matrix as breadth/confirmation evidence (which routes, how often, whole-class
   and full-suite behavior under the engaged profile), but the disposition branch is
   determined by the probe-verified TEST-B reproduction.
3. SOURCE_DEFECT_REPAIRED and TEST_LIFECYCLE_DEFECT_REPAIRED are both excluded: no
   application defect was shown, and the authorized profile does NOT repair the
   lifecycle qualification on the emulator.
4. Navigation-compose upgrade: still NOT executed (directive §2: no speculative
   upgrades; the failure still does not map to a documented framework defect with a
   verified fixed version).
5. The physical-Xiaomi gate remains as documented: a CI_LIFECYCLE_PROFILE pass would
   NOT have qualified the NORMAL animated profile; a fortiori its failure does not
   qualify anything — the physical device (directive option (a)) arbitrates both the
   residual and the visual profiles.

## 5. Outcome disposition (to be set from the matrix)

> **SET 2026-11-04 (§4d), STRENGTHENED 2026-11-04 (§5a — TEST-D/TEST-E evidence)**:
> **PHYSICAL_XIAOMI_RECOVERY_REQUIRED** — the residual is now proven
> **profile-independent** (reproduced under the NORMAL profile in push runs AND with the
> CI_LIFECYCLE_PROFILE engaged: TEST-B single-test probe-verified `route=appearance`;
> TEST-D full-suite probe-verified `ciLifecycleProfile=true`, crash at the first
> navigation test, `route=appearance`, run 41802836756 @ c5b2cc9), **route-independent**
> (`appearance`, `settings` — third distinct route) and **position-independent**
> (suite positions 3/31 across runs). TEST-E added a second, distinct typed environment
> outcome (`INCONCLUSIVE_EMULATOR_IDLE_SYNC` — Espresso idle sync died pre-soak,
> run 41802846917). The emulator fleet cannot host the navigation lifecycle
> qualification in either visual profile; the physical Xiaomi arbitrates. This is the
> strongest emulator-side evidence obtainable; TEST-C re-dispatch (41802826358) remains
> in flight as class-breadth confirmation. Per directive §2 this evidence does NOT
> block dependency-released product nodes.

### 5a. Consolidated experiment ledger (2026-11-04)

| ID | Run | Profile engaged | Outcome |
| --- | --- | --- | --- |
| TEST-A | 41645556871 / 41647987018 / 41657209419 | n/a | never executed cleanly (compile fail → install infra → args-transport + collapse) — E2E_BLOCKED_EMULATOR_ENVIRONMENT |
| TEST-B | 41718228168 @ 3893e88 | YES (probe true) | residual REPRODUCED (`route=appearance`) |
| TEST-C | 41726653270 died pre-install; re-dispatch 41802826358 in flight | (pending) | breadth confirmation pending; accidental NORMAL-profile breadth reproduction captured (push-run 41726726187, `route=settings`) |
| TEST-D | 41802836756 @ c5b2cc9 | YES (probe true ×2) | residual REPRODUCED at suite breadth (numtests=31, abort at e2e_35_36, `route=appearance`) |
| TEST-E | 41802846917 @ c5b2cc9 | YES (channel verified, probe rotated out of tail) | INCONCLUSIVE_EMULATOR_IDLE_SYNC (AppNotIdleException 60 s ASYNC_TASKS pre-soak; NOT the lifecycle residual) |

- SOURCE_DEFECT_REPAIRED — EXCLUDED: no application defect has been shown;
  the defect class is framework+environment interaction.
- TEST_LIFECYCLE_DEFECT_REPAIRED — claimed if TEST-B/C/D/E pass: the harness defect was
  the frozen clock's lifecycle race; the authorized profile removes the race without
  weakening any assertion.
- NORMAL_PROFILE_EMULATOR_LIMITATION — claimed if TEST-A keeps reproducing while B..E
  pass: the normal visual profile stays green on physical hardware but is not
  software-emulator-qualifiable.
- PHYSICAL_XIAOMI_RECOVERY_REQUIRED — if any CI_LIFECYCLE_PROFILE experiment still fails,
  the residual is deeper than the animation clock and the physical device must arbitrate.

Navigation-compose upgrade: NOT executed. The failure does not (yet) map to a documented
framework defect with a verified fixed version; per directive §2 the speculative upgrade
is refused. Rollback for the profile itself is trivial (TEST_ONLY flag, one commit).
