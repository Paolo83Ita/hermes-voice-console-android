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
| TEST-A | e2e_35_36 alone | NORMAL_VISUAL_PROFILE | residual reproduces (control) | 41645556871 @ bd1606c = compile fail (harness defect, repaired); 41647987018 @ c6476a1 = install infra failure (`Can't find service: package`, zero tests — E2E_BLOCKED_EMULATOR_ENVIRONMENT class); re-dispatched @ b0be658 | PENDING |
| TEST-B | e2e_35_36 alone | CI_LIFECYCLE_PROFILE | passes with real navigation | **41648000740 @ c6476a1: executed — RESIDUAL REPRODUCED** (identical signature); controlled re-run @ b0be658 with engagement probe | REPRODUCED (first sample) |
| TEST-C | E2EAppearancePersistenceTest whole class | CI_LIFECYCLE_PROFILE | passes | PENDING | PENDING |
| TEST-D | complete instrumentation suite | CI_LIFECYCLE_PROFILE | 0 failures | PENDING | PENDING |
| TEST-E | normal appearance navigation ×20, one session | CI_LIFECYCLE_PROFILE | no entry-lifecycle crash | PENDING | PENDING |

## 4b. MAJOR FINDING (2026-11-03, run 41648000740 @ c6476a1) — the frozen clock is NOT the sole mechanism

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

## 5. Outcome disposition (to be set from the matrix)

- SOURCE_DEFECT_REPAIRED — not applicable so far: no application defect has been shown;
  the defect class is framework+test-harness interaction.
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
