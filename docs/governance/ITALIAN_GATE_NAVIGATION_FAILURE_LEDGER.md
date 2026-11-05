# ITALIAN GATE NAVIGATION FAILURE LEDGER

Directive §3 — every failed Italian-gate run recorded with the required fields and a
typed classification. The monolithic audit `e2e_italian_ui_completeness_gate` is
REPLACED (not retried) by the segmented classes `ITALIAN-GATE-A..F` (directive §4);
this ledger is the evidence chain that justifies the segmentation and fixes each
segment's anchors.

Source of truth for each row: the run's own artifacts
(`hermes-voice-console-e2e-results-<sha>` / `…-failure-evidence-<sha>`, GitHub
artifacts, 14-day retention) and the instrumentation output parsed this session.

## R-1 — explicit recreate raced the system relaunch

| Field | Value |
| --- | --- |
| Run ID / commit | 41814872419 @ a9e5db5 (dispatch, gate-only) |
| Last screen reached | Console under the NEW locale (system relaunch in progress) |
| Next destination requested | Console (first destination of the loop) |
| Expected anchor | `HERMES` (top bar) |
| Actual semantic tree | partially composed / no activity window during relaunch |
| Screenshot / Logcat | run artifacts (`…-failure-evidence-a9e5db5…`) |
| Navigation action executed | yes (AppLanguage.apply per-app locale) |
| Route changed | no (relaunch resets to Console start destination) |
| Destination composed | not yet — relaunch in flight |
| Main thread stalled | no — activity was mid-recreate |
| Process responsive | yes until the test asserted |
| ANR evidence | none for our process |
| Typed classification | **TEST_NAVIGATION_DEFECT** — explicit `scenario.recreate()` raced the API-33+ automatic relaunch ("stage must be … but was DESTROYED") |
| Narrow repair | remove the explicit recreate; poll for the relaunch (adopted in `switchLocaleAndAwait`) |
| Narrow rerun / complete rerun | repair validated in later gate runs |

## R-2 — compose idle never reachable (infinite Oracle Core animation)

| Field | Value |
| --- | --- |
| Run ID / commit | 41818773091 @ 5e9f0d2 (dispatch, gate-only) |
| Last screen reached | gate class STARTED, never progressed |
| Expected anchor | n/a — `compose.waitForIdle()` never returned |
| Actual semantic tree | n/a (rule stuck pre-first-statement) |
| Navigation action executed | no |
| Main thread stalled | no — the TEST rule waited for idle; the console's infinite animation kept the idle condition false |
| Typed classification | **TEST_TIMEOUT_TOO_LOW_WITH_EVIDENCE** (idle provably never reachable: the gate class missed the variant-A `freezeAnimationsForTest` that every other suite class has; instrument hit the 3600 s wall) |
| Narrow repair | fix 10 — the gate class freezes the clock in `@Before` (identical physics to the other classes) |
| Rerun | next dispatch executed normally (R-3) |

## R-3 — NavController.navigate invoked off-main

| Field | Value |
| --- | --- |
| Run ID / commit | 8727210 @ 94180c5 fix-11 (dispatch, gate-only) |
| Last screen reached | Console; drawer row activation fired a navigation |
| Expected anchor | next destination's anchor |
| Actual failure | `IllegalStateException: Method addObserver must be called on the main thread` at `NavController.navigate` — the gate's semantics `OnClick` invocation ran off-main |
| Navigation action executed | attempted — framework refused (lifecycle observer add on non-main) |
| Route changed | no |
| Typed classification | **TEST_NAVIGATION_DEFECT** (test-side threading; NavController requires main) |
| Narrow repair | fix 11 — the gate's three direct action sites dispatch via `instrumentation.runOnMainSync`; shared harness untouched |
| Rerun | next dispatch navigated (R-4) |

## R-4 — QR scanner region stall + system ANR

| Field | Value |
| --- | --- |
| Run ID / commit | 41833663387 @ 94180c5 (dispatch, gate-only) |
| Last screen reached | QR scanner (full-screen route) after REAL Italian rendering through Console/Archive/Tower Gate/Models (dump proves: CAMERA DELL'ARCHIVIO, localized engine-truth row, Nessuna conversazione…) |
| Next destination requested | Tower Archive via drawer |
| Expected anchor | archive title |
| Actual semantic tree | scanner still front; drawer ABSENT inside the scanner |
| Navigation action executed | the drawer-based activation found no drawer |
| Route changed | no — dead-ended in the scanner |
| Main thread stalled | the process froze when a system ANR dialog appeared in the QR/archive region (camera-less emulator) |
| ANR evidence | run logcat + fix-12 commit evidence |
| Typed classification | **CAMERA_SCREEN_BACK_NAVIGATION_DEFECT** + **EMULATOR_SYSTEM_ANR** (QR_ARCHIVE_REGION_ANR_EMULATOR) |
| Narrow repair | fix 12 — the gate leaves the scanner via `pressBack` BEFORE any drawer navigation; the ANR stays typed for the physical device |
| Rerun | R-7 |

## R-5 / R-6 — full-suite pushes: the dispositioned lifecycle residual kills the suite before the gate runs

| Field | R-5: 41840678804 @ 01493a1 | R-6: 41846567723 @ 92465e5 |
| --- | --- | --- |
| Trigger | push (full suite, numtests=33) | push (full suite, numtests=33) |
| Last screen reached | test 3/33 `e2e_35_36_switchMarbleLightAndObsidianNight` | same |
| Failure | `IllegalStateException: State must be at least CREATED to move to DESTROYED, but was INITIALIZED in component NavBackStackEntry(…) route=settings` at `Activity.performDestroy` → `shortMsg=Process crashed` → instrumentation aborted | identical |
| Gate executed | NO — the crash destroyed all later classes' evidence | NO — identical |
| Typed classification | **INFRASTRUCTURE_FAILURE** (the ALREADY-DISPOSITIONED lifecycle residual, PHYSICAL_XIAOMI_RECOVERY_REQUIRED, blocks other classes' evidence by process crash — investigation NOT reopened per directive §0) | same |
| Narrow repair | crash-quarantine BY INVOCATION (this session): the residual class runs in its own `am instrument` invocation; no test deleted, weakened or bypassed; all other classes regain runnable evidence; the residual invocation is expected to show exactly the known signature — any OTHER signature still fails the job | same repair |
| Rerun | next E2E workflow execution | — |

## R-7 — anchor wait expired at the composition boundary (last monolithic failure)

| Field | Value |
| --- | --- |
| Run ID / commit | 41840691263 @ 01493a1 (dispatch, gate-only, numtests=1) |
| Last screen reached | Console fully rendered in Italian; Archive navigation IN FLIGHT |
| Next destination requested | Tower Archive (dest 2 of the loop) |
| Expected semantic anchor | `archive_title` = "Archivio della Torre" |
| Actual semantic tree (timeout dump, verbatim) | `[Archivio Tower, CAMERA DELL’ARCHIVIO, HERMES, TOWER, CONSOLE ORACOLO VOCALE, OFF, LOCAL, TOWER, HYBRID, BRIDGE OK, MIC, FERMA, modalità HYBRID, CORE LOCALE, TOWER, l’attribuzione è reale — le risposte locali non si attribuiscono mai alla Tower, Nessuna conversazione. Parla con il microfono o scrivi un messaggio — tutto ciò che è mostrato è l’output reale della console., Archivio conversazioni →, Scrivi un messaggio…, parla invece, stop, invia, mic: push-to-talk (riconoscitore di sistema) · testo: di prima classe · voce uscita: TTS di sistema Android · STT offline + voce naturale: in attesa in Modelli e voce · tower: SIMULATA fino a Tower Graph 1.7, Archivio della Torre, CAMERA DELL'ARCHIVIO]` |
| Key observation | the anchor "Archivio della Torre" AND the archive subtitle ARE PRESENT in the dump built at timeout — the destination composed at the wait boundary; the timeout dump was taken after the deadline while the composition landed |
| Screenshot / Logcat | `hermes-voice-console-e2e-results-01493a1…` (e2e-final-screen.png, e2e-logcat-tail.txt); logcat shows NO ANR of our process (only an unrelated background `com.google.android.apps.messaging` ANR) |
| Navigation action executed | YES — drawer row "Archivio Tower" activated |
| Route changed | YES — Archive route composed |
| Destination composed | YES (title + subtitle in tree at dump time) |
| Main thread stalled | NO — `pumpUntil` polled for the full 20 s window |
| Process responsive | YES |
| ANR evidence | none for our process |
| Typed classification | **TEST_TIMEOUT_TOO_LOW_WITH_EVIDENCE** — the destination eventually appears (dump proves it); 20 s is below the Console→Archive composition boundary on the software emulator (full-tree semantics fetches + first composition of the Archive + monumental canvas). Timeout raised ONLY for destination composition, and ONLY because appearance is here proven |
| Narrow repair | segmented classes (smaller per-class scope), verified per-screen anchors, settle pump after activation, bounded 60 s destination-composition floor with the same in-dump evidence requirement |
| Complete gate rerun | the six segmented classes (ITALIAN-GATE-A..F) run in their own invocation from the next workflow execution |

## Classification summary (allowed vocabulary, directive §3)

| Run | Classification |
| --- | --- |
| R-1 | TEST_NAVIGATION_DEFECT |
| R-2 | TEST_TIMEOUT_TOO_LOW_WITH_EVIDENCE |
| R-3 | TEST_NAVIGATION_DEFECT |
| R-4 | CAMERA_SCREEN_BACK_NAVIGATION_DEFECT + EMULATOR_SYSTEM_ANR |
| R-5 | INFRASTRUCTURE_FAILURE (dispositioned lifecycle residual) |
| R-6 | INFRASTRUCTURE_FAILURE (dispositioned lifecycle residual) |
| R-7 | TEST_TIMEOUT_TOO_LOW_WITH_EVIDENCE |

NO run to date shows an APPLICATION_NAVIGATION_DEFECT, a ROUTE_NOT_REACHABLE, an
ARCHIVE_TO_TOWER_GATE_DEFECT or a MAIN_THREAD_BLOCK: in every gate-specific failure the
navigation executed, the route changed and the destination composed — the defects were
test-side (threading, anchors, idle physics, timeouts) or environmental (scanner region
ANR on a camera-less emulator; the dispositioned lifecycle residual).

## R-8 — wizard step-1 anchor composed but CASE-TRANSFORMED (first segmented run)

| Field | Value |
| --- | --- |
| Run ID / commit | 41868118783 @ b3b78ed (push, segmented model, gate invocation) |
| Last screen reached | Onboarding wizard step 1 FULLY rendered in Italian |
| Expected anchor | `ob_s1_title` = "Scegli la tua lingua" |
| Actual semantic tree (timeout dump, verbatim) | `[Benvenuto in Hermes Voice Console, Passo 1 di 6, SCEGLI LA TUA LINGUA, Puoi cambiarla in qualsiasi momento in Impostazioni → Lingua., Segui il sistema, Italiano, English, Español, Français, Deutsch, Continua]` |
| Key observation | the anchor text IS composed — the wizard's section-card title renders the string case-transformed (semantics text "SCEGLI LA TUA LINGUA" vs resource "Scegli la tua lingua"); `onAllNodesWithText` is case-sensitive by default |
| Screenshot / Logcat | artifact `hermes-voice-console-e2e-results-b3b78ed…` (gate-output.txt) |
| Navigation action executed | YES — gate A reached step 1 through the real relaunch |
| Route changed | YES (wizard step 1) |
| Destination composed | YES (dump) |
| Main thread stalled | NO |
| ANR evidence | none |
| Typed classification | **MISSING_SEMANTIC_ANCHOR** (anchor present, matcher case-exact) — test-side |
| Narrow repair | `gateAwaitAnchor` / `gateActivateRow` / hub-row lookup now match with `ignoreCase = true` (R-8 comment; matching only — the banned-English classifier is unchanged) |
| Complete gate rerun | next workflow execution |

## R-9 — first post-relaunch interaction raced the recreation (gates A2 + B)

| Field | Value |
| --- | --- |
| Run ID / commit | 41868118783 @ b3b78ed (gate invocation, tests 2 and 3) |
| Last screen reached | Console fully rendered in Italian (dump: composer, engine-truth row, drawer affordances) |
| Next destination requested | text-injection DIALOG via `inject_cd` click |
| Expected anchor | `cd_inject_title` = "Inserisci testo (sostituto STT)" |
| Actual semantic tree | console composed, dialog ABSENT (visible texts in the dump) |
| Key observation | `showInjectDialog` is `remember{}`-held (not saveable): the click landed on a composition that the API-33+ system relaunch then replaced, silently dropping the dialog state; gate A1 (its own test) had just failed mid-wizard leaving a relaunch in flight |
| Screenshot / Logcat | artifact `hermes-voice-console-e2e-results-b3b78ed…` |
| Navigation action executed | YES (click dispatched) |
| Route changed | no (dialog state lost at recreation) |
| Destination composed | NO |
| Main thread stalled | NO |
| ANR evidence | none |
| Typed classification | **TEST_NAVIGATION_DEFECT** (interaction raced the system-driven relaunch) |
| Narrow repair | `gateSwitchLocaleAndAwait` now settles the relaunch (~0.6 s) and REQUIRES the anchor to persist across a second poll before returning; gates A2 and B additionally await the composer hint (live-composition proof) before the inject click |
| Complete gate rerun | next workflow execution |

## R-10 — unbounded waitForIdle consumed the gate invocation budget (gate C)

| Field | Value |
| --- | --- |
| Run ID / commit | 41868118783 @ b3b78ed (gate invocation) |
| Last screen reached | gate C started (INSTRUMENTATION_STATUS: test=…C…, STATUS_CODE: 1) and produced NO further output for the remainder of the 25-min `timeout 1500` |
| Expected anchor | Tower Gate progression |
| Actual semantic tree | n/a (no output — the wait never returned) |
| Key observation | `gateNavigateTo` ended with `runCatching { waitForIdle() }`: runCatching catches exceptions, NOT hangs — with the console's infinite Oracle Core animation on screen the idle condition never arrives (same physics fix 10 already proved for a missing @Before freeze) |
| Screenshot / Logcat | artifact `hermes-voice-console-e2e-results-b3b78ed…` |
| Navigation action executed | YES (C started) |
| Route changed | unknown (no output) |
| Destination composed | unknown |
| Main thread stalled | the TEST thread hung inside waitForIdle |
| ANR evidence | n/a |
| Typed classification | **MAIN_THREAD_BLOCK** (test-side unbounded wait; product navigation unproven either way) |
| Narrow repair | `gateNavigateTo`'s waitForIdle replaced by the bounded settle pump (6× advance 100 ms + 25 ms sleep) — the harness now contains NO unbounded wait |
| Complete gate rerun | next workflow execution |

## R-11 — regression blackout generalizes: the residual is CLASS-INDEPENDENT

| Field | Value |
| --- | --- |
| Run ID / commit | 41868118783 @ b3b78ed (regression invocation) |
| Last screen reached | regression ran until `E2EFreshInstallTest.e2e_03_04_onboardingWithoutCredentials_mockModeSelected` |
| Expected anchor | n/a (suite health leg) |
| Actual result | `INSTRUMENTATION_RESULT: shortMsg=Process crashed.` — the SAME INITIALIZED→DESTROYED lifecycle residual (activity-destroy path: ReportFragment → performDestroy), in a DIFFERENT class than the dispositioned `E2EAppearancePersistenceTest` |
| Key observation | the dispositioned residual is not bound to one class or route (now 4 distinct routes/classes: appearance, settings, tower_studio, fresh-install onboarding); quarantining ONE class cannot protect the rest — every class ordered after a crash loses its evidence |
| Screenshot / Logcat | artifact `hermes-voice-console-e2e-results-b3b78ed…` (regression-output.txt) |
| Navigation action executed | n/a |
| Route changed | n/a |
| Destination composed | n/a |
| Main thread stalled | no — process crash |
| ANR evidence | n/a |
| Typed classification | **INFRASTRUCTURE_FAILURE** (the dispositioned lifecycle residual, PHYSICAL_XIAOMI_RECOVERY_REQUIRED stands — this row does NOT reopen the root-cause investigation; it upgrades the CONTAINMENT) |
| Narrow repair | regression leg upgraded to ONE INVOCATION PER CLASS with per-class typed classification: OK → pass; dispositioned residual signature → typed INFRASTRUCTURE_FAILURE (recorded, job-neutral); ANY other outcome fails the job. No test deleted, skipped or weakened; residual-class timeout 600→900 s (evidence: e2e_41_42 passed and e2e_40 was mid-recreate at the 600 s cut) |
| Complete gate rerun | next workflow execution |

## Classification summary (allowed vocabulary, directive §3)

| Run | Classification |
| --- | --- |
| R-1 | TEST_NAVIGATION_DEFECT |
| R-2 | TEST_TIMEOUT_TOO_LOW_WITH_EVIDENCE |
| R-3 | TEST_NAVIGATION_DEFECT |
| R-4 | CAMERA_SCREEN_BACK_NAVIGATION_DEFECT + EMULATOR_SYSTEM_ANR |
| R-5 | INFRASTRUCTURE_FAILURE (dispositioned lifecycle residual) |
| R-6 | INFRASTRUCTURE_FAILURE (dispositioned lifecycle residual) |
| R-7 | TEST_TIMEOUT_TOO_LOW_WITH_EVIDENCE |
| R-8 | MISSING_SEMANTIC_ANCHOR |
| R-9 | TEST_NAVIGATION_DEFECT |
| R-10 | MAIN_THREAD_BLOCK (test-side) |
| R-11 | INFRASTRUCTURE_FAILURE (dispositioned lifecycle residual — containment upgrade) |

NO run to date shows an APPLICATION_NAVIGATION_DEFECT, a ROUTE_NOT_REACHABLE or an
ARCHIVE_TO_TOWER_GATE_DEFECT. The dispositioned residual stays
PHYSICAL_XIAOMI_RECOVERY_REQUIRED; R-11 changes only its containment (per-class
isolation), not the disposition.

## R-12 — gate A1 GREEN; A2 dialog gesture-click never composes the dialog (run bba1ebc)

| Field | Value |
| --- | --- |
| Run ID / commit | 41890040629 @ bba1ebc (push, segmented model) |
| Gate result | **A1 PASSED** (STATUS_CODE 0 — the six-step wizard REAL walk + onboarding flag: the R-8 case repair and R-9 settle+persist VERIFIED in the cloud); A2 FAILED with the SAME anchor signature as b3b78ed |
| Expected anchor | `cd_inject_title` = "Inserisci testo (sostituto STT)" |
| Actual semantic tree | console fully composed (composer hint awaited OK first — the R-9 live-proof), dialog ABSENT after 60 s |
| Key observation | the R-9 relaunch-race repair did NOT change the failure: the medallion-row control was clicked via the GESTURE path (performClick = touch injection at the node center), while every drawer/hub/screen-row click that PASSES in the cloud uses gateActivateRow's DIRECT semantics-action dispatch on main (R-3 physics). The emulator also DIED mid-run (adb: device offline from ~19:41; boot 18.5 min, universal-APK install 12.7 min — the regression loop then burned 15-min timeouts per class against the dead device, and the artifact upload glob `regression-*.txt` missed the `regression_*.txt` files) |
| Typed classification | **TEST_NAVIGATION_DEFECT** (click physics) + **INFRASTRUCTURE_FAILURE** (emulator death, E2E_BLOCKED_EMULATOR_ENVIRONMENT class) + workflow evidence-gap (upload glob) |
| Narrow repair | A2/B inject clicks + dialog cancel clicks now use the DIRECT semantics-action dispatch (gateClickByContentDescription / gateActivateRow — same proven physics); artifact glob corrected to `regression_*.txt`; the regression loop fast-fails on `adb get-state` failure (typed DEVICE_OFFLINE_ABORT); per-class timeout 900→600 s; gate invocation timeout 1500→3000 s (measured: A1 alone ≈ 12 min on the software fleet — six classes cannot fit 25 min); job timeout 120→180 min (boot+install+gate+6 classes+residual measured budget) |
| Complete gate rerun | next workflow execution |

## R-14 — baseline recreate idle-sync: the R-13 single retry was consumed and insufficient (run 41938185541 @ a2d0b27)

| Field | Value |
| --- | --- |
| Run ID / commit | 41938185541 @ a2d0b27 (push, one-invocation-per-class gate model, fix-4 harness active) |
| Run outcome | **CANCELLED at the 180-min job cap** — never reached a verdict |
| Gate A1 | FAILED — `AppNotIdleException: Looped for 1 iterations over 60 SECONDS (ASYNC_TASKS_HAVE_IDLED)` around `ActivityScenario.recreate()`; the R-13 repair (fix-4 commit message designation; settle+ONE-retry) was consumed and the RETRY recreate threw the SAME exception seconds later (stack: `Espresso.onIdle` → `loopMainThreadUntilIdle` → `IdlingPolicy.handleTimeout`) |
| Gate A2 | **PASSED** (STATUS_CODE 0) — the R-12 direct-dispatch physics holds at this tip; A1 passed bba1ebc, failed e084be8, failed a2d0b27 — the SAME code three different idle-sync outcomes: pure fleet physics |
| Gate B | the emulator **DIED mid-invocation** (4th fleet death: `adb: device offline` at 00:39 UTC, ~38 min into the step) — the offline-guard aborted C–F with typed `DEVICE_OFFLINE_ABORT`, no timeout burned |
| Regression / residual | offline-abort typed immediately (tally 0/0/[]); the residual's `adb logcat -d` capture line ran against the dead fleet UNBOUNDED and consumed 2 h 22 min until the 3 h cap cancelled the job BEFORE the verdict printed (the upload step only ran at 03:01:40 after the cancel signal) |
| Typed classification | **INFRASTRUCTURE_FAILURE** (fleet death #4 + unbounded adb capture hang — E2E_BLOCKED_EMULATOR_ENVIRONMENT class) + **TEST_NAVIGATION_DEFECT-CLASS FLAKE** (AppNotIdleException at BOTH gate recreate sites — the TEST-E INCONCLUSIVE_EMULATOR_IDLE_SYNC lineage) |
| Narrow repair (fix 5) | (1) `gateRecreateSteadily()` — ONE typed helper at EVERY gate recreate site: catches ONLY AppNotIdleException, backoff 3 s→10 s→30 s, three retries on top of the first attempt, fourth failure propagates honestly; replaces R-13's single retry in `gateEnsureBaseline` AND replaces A1's raw `scenario.recreate()`; nothing skipped, nothing weakened — the recreate must SUCCEED for the REAL walk to continue. (2) Workflow: `adb logcat -d` + `adb exec-out screencap` bounded `timeout 60`; residual invocation gets the same offline-guard (typed DEVICE_OFFLINE_ABORT written INTO residual-output.txt so the verdict channel stays honest); job timeout 180→300 min (arithmetic worst case of the per-class model ≈263 min) |
| Complete gate rerun | next workflow execution (fix-5 tip) |

## Classification summary update

| Run | Classification |
| --- | --- |
| R-12 | TEST_NAVIGATION_DEFECT + INFRASTRUCTURE_FAILURE (emulator death) |
| R-13 | (fix-4 repair designation, commit a2d0b27 — baseline recreate settle+retry; no separate run row) |
| R-14 | INFRASTRUCTURE_FAILURE (fleet death #4 + unbounded adb capture hang) + recreate idle-sync flake (AppNotIdleException at both gate recreate sites) |

## R-15 — two fleet collapses in ONE push wave; fix-5 mechanics VERIFIED under collapse (runs 41972674909 #133 + #134 @ 8bffd60/7c57025)

| Field | Value |
| --- | --- |
| Runs | #133 (41972674909 @ 8bffd60, fix-5 tip) and #134 (@ 7c57025, docs follow-up) — both push-triggered, ~concurrent |
| #134 | **BOOT-LEVEL collapse** (NEW mode, 5th infra failure): emulator process launched 05:19:42 UTC but `adb -s emulator-5554` reported `device offline` CONTINUOUSLY 05:19:53→05:37 — the guest never became reachable; ZERO instrumentation ran; job failed honestly at ~35 min |
| #133 | the fleet died **at the very START of gate A** (6th infra failure): gate_A.txt holds only A1's stream header (STATUS_CODE 1 = test started 05:46:37) then the device went offline mid-invocation; B–F and regression typed `DEVICE_OFFLINE_ABORT`; tally honestly 0/6 |
| fix-5 mechanics VERIFIED in the collapse | (1) residual offline-guard wrote the typed `DEVICE_OFFLINE_ABORT` INTO residual-output.txt (verdict channel intact); (2) logcat/screencap bounded `timeout 60` — the residual-abort→verdict gap is exactly 60 s (NO 2 h hang); (3) `E2E_VERDICT_FAILED` printed honestly; (4) total job ~1 h 05 m instead of the previous 3 h+; (5) `assembleDebugAndroidTest` SUCCESS at 8bffd60 — the R-14 `gateRecreateSteadily()` code COMPILES and the gate instrument started |
| Typed classification | **INFRASTRUCTURE_FAILURE ×2 (E2E_BLOCKED_EMULATOR_ENVIRONMENT)** — six deaths across the last six E2E executions (4 mid-gate, 1 boot-level, 1 gate-A-start); NOT a code property: the same gate harness PASSED A1 (bba1ebc) and A2 (e084be8 + a2d0b27) on healthier fleet samples |
| Narrow repair | none required in code — the established "complete rerun on infrastructure failure" contract; this governance commit re-enters the queue at the current tip |
| Complete gate rerun | THIS commit's push run |

## R-16 — the 60 s master idling policy is the binding constraint, not the retry count (run #135 @ 9508ac8)

| Field | Value |
| --- | --- |
| Run ID / commit | #135 (job 127078393485) @ 9508ac8 — R-14 helper ACTIVE and fully exercised |
| Gate A1 | FAILED after `Time: 604.008` — `AppNotIdleException: Looped for 2 iterations over 60 SECONDS (ASYNC_TASKS_HAVE_IDLED)` propagated through ALL FOUR gateRecreateSteadily attempts (4 × 60 s idle sync + 3/10/30 s backoffs ≈ 283 s inside the 604 s class time). The helper worked as designed; the POLICY bound (60 s) failed four times — the drain needs MORE THAN 60 s, not more attempts |
| Physics evidence | ActivityScenario.recreate() ends in Espresso.onIdle() (stack-proven in #132) bounded by IdlingPolicies' 60 s MASTER default; the drain FINISHES (A1 passed at bba1ebc with the same code; #135's own run continued past A1 and A2 PASSED again — the fleet was merely slow, not hung). The app-side relaunch physics (AppLanguage.apply + semantics polling) has NEVER hit this exception in any run — it never waits on the idle controller |
| Gate A2 | PASSED again (3rd consecutive sample) |
| Fleet death #7 | mid-gate B (06:47:44 UTC, ~7 min into B) → typed DEVICE_OFFLINE_ABORT for C–F + regression instant abort + residual guard typed — the containment worked as designed |
| Typed classification | **FLEET-SPEED IDLE-SYNC TOLERANCE** (the TEST-E INCONCLUSIVE_EMULATOR_IDLE_SYNC lineage — the drain exceeds the 60 s policy under starvation) + **INFRASTRUCTURE_FAILURE** (death #7) |
| Narrow repair | **R-16**: raise the espresso idling tolerance in the gate harness — `IdlingPolicies.setMasterPolicyTimeout(240, SECONDS)` + `setIdlingResourceTimeout(240, SECONDS)` at gateEnsureBaseline entry (fresh process per gate-class invocation, so exactly one class is affected); same precedent as R-7's evidence-backed destination floor (60 s), 4× the default, the recreate must STILL SUCCEED; gateRecreateSteadily resized to 2 retries (worst case 3×240 s + backoffs ≈ 760 s < the 900 s per-class budget) |
| Complete gate rerun | next workflow execution (R-16 tip) |

## R-17 — post-boot service collapse: the boot→install window was the last unguarded segment (run #136 41983695658 @ 982877c)

| Field | Value |
| --- | --- |
| Run ID / commit | #136 (job 127099393926, 2026-11-05 06:55:51→07:18:55 UTC) @ 982877c (fix 6 / R-16 tip, fetch-verified) |
| Failure | **NEW collapse mode, fleet death #8 (9th infra failure in 10 E2E executions)**: emulator BOOT COMPLETED in 968177 ms (07:16:41 UTC) — the runner accepted boot — then system_server lost activity/package services within ~1 min: the runner's multidisplay broadcast hit `cmd: Can't find service: activity` at 07:17:27 (46 s after boot), and the app install failed at 07:18:15 with `adb: failed to install app-universal-debug.apk: cmd: Can't find service: package` → step exit 1. **ZERO instrumentation ran** — every gate/regression/residual channel had no evidence by design (missing evidence is NOT success) |
| Why the guard missed it | the post-boot settle loop waits ONLY for the `sys.boot_completed` FLAG — it never proves the package service is usable; the R-14/R-16 repairs guard the in-gate idle sync, the R-11/offline-guards guard mid-suite deaths, but boot→install was the last unguarded segment |
| Not a code property | the same APKs install successfully in every build run (41983695691 SUCCESS at 982877c); installs succeeded at bba1ebc/e084be8 on healthier fleet samples; the collapse struck AFTER boot acceptance — pure guest physics |
| Typed classification | **INFRASTRUCTURE_FAILURE (E2E_BLOCKED_EMULATOR_ENVIRONMENT — post-boot service collapse)** |
| Narrow repair | **R-17** (R-14/R-16 precedent: ONE typed bounded recovery for the ONE evidence-proven collapse mode): a guest health gate before the installs — `pm list packages` probe; dead → `adb reboot` + bounded re-wait 1200 s (observed cold boots 968–1110 s; warm reboots faster) + sleep-60 settle + ONE re-probe; still dead → typed `E2E_BLOCKED_EMULATOR_ENVIRONMENT` honest abort; healthy → the real walk proceeds UNCHANGED. Both install lines additionally typed (`… || { echo 'E2E_BLOCKED_EMULATOR_ENVIRONMENT — …'; exit 1; }`) so non-collapse failures abort honestly WITHOUT a blind retry (a flake-hiding retry would be dishonest). Exactly ONE recovery per run keeps the 300-min cap arithmetic safe (worst case ≈289 min: build 8 + boot 40 + settle 6 + recovery 23 + installs 30 + gates 90 + regression 60 + residual 15 + evidence 15) |
| Complete gate rerun | THIS commit's push run (the deciding evidence channel, unchanged) |

## R-18 — the FIRST full-instrumentation run: the harness miscounted REAL passes (run #137 41990077345 @ e89d014, 2026-11-05 07:50→09:18 UTC)

The R-17 health gate WORKED (GUEST_HEALTH_OK at T+60s, both APKs installed, ZERO
fleet deaths) and the per-class model ran to completion for the first time in 10
E2E executions. Honest verdict E2E_VERDICT_FAILED. The evidence re-classified:

| Segment | Printed result | TRUE result (evidence) |
| --- | --- | --- |
| Gate A (588.6 s) | OK (2 tests) → counted | **PASSED** (A1+A2 — the R-16 240 s policy held) |
| Gate B | timeout 900 killed at ~899 s, only the stream header | **FLEET-SPEED INFRASTRUCTURE FAILURE**: the baseline consumed the whole class budget (test STARTED at 08:50:04 = second 899); ColorBuffer errors 08:47–08:50 in the window; C/D/E/F baselines normal minutes later — not a code property |
| Gate C (156.9 s) | FAIL: open-studio control not found (it) | **TEST_NAVIGATION_DEFECT (R-19)**: dest_connect routes DIRECTLY to the Tower Studio (AppNav: Destination(TOWER_STUDIO, dest_connect)) — the open-studio control lives on the CONNECTION screen (dest_connection); the step could never succeed |
| Gate D (146.2 s) | FAIL: settings-hub row 'Modelli e voce' not found | **HARNESS ROBUSTNESS (R-21)**: drawer-row lookup failed (mechanism unresolved — no dump existed) AND the fallback awaited the ROW label, which the VOICE section's engine_row text ("…in attesa in Modelli e voce…") can false-match; the hub-row OnClick lookup then failed without evidence |
| Gate E (112.8 s) | FAIL: missing [Generale, Connessione alla Torre, Display] | **TEST_ANCHOR_DEFECT (R-20)**: the failure dump PROVES all six categories visible — "GENERALE", "CONNESSIONE ALLA TORRE", "DISPLAY" (SectionCard uppercases titles, Chrome.kt) — the case-sensitive contains() could never match |
| Gate F (239.0 s) | counted FAILED | **PASSED — FALSE NEGATIVE (R-18)**: the instrument prints `OK (1 test)` (SINGULAR) and the classification pattern `OK ([0-9][0-9]* tests)` (plural-only) missed it |
| Regression | ok=1 residual=3 failed=[Lifecycle, Onboarding] | **TRUE: ok=2** (LifecycleProfile `OK (1 test)` miscounted by R-18; VoiceFlow `OK (6 tests)`) **+ residual-typed 3** (FreshInstall/ModelManager/SelfTest — the dispositioned signature, job-neutral by design) **+ 1 real failure**: e2e_onboarding_sixSteps_reachConsole waited 20 s for "Connect to Hermes Tower" while the screen shows "CONNECT TO HERMES TOWER" → **R-22 (R-8 lineage)** |
| Residual | dispositioned signature | typed ✓ (expected, PHYSICAL_XIAOMI_RECOVERY_REQUIRED) |

**R-18 (harness false-negative)**: classification accepts `OK (N test)` AND
`OK (N tests)` — `grep -qE 'OK \([0-9][0-9]* tests?\)'` at ALL sites (gate loop,
regression loop, experiment check, verdict loop, residual check). This is a
FALSE-NEGATIVE repair (a real pass was counted as a failure — the OPPOSITE of
flake-hiding); nothing weakened.

**R-19 (test navigation)**: gate C walks `dest_connection` → Connection screen →
open-studio → Tower Studio → scanner → back (the REAL product path).

**R-20/R-22 (R-8 lineage, matcher-only)**: gate E's category assertion and the
harness waitForText match ignoreCase — the rendered typography (uppercased
SectionCard titles) is authoritative; the banned-English classifier is unchanged.

**R-21 (gateNavigateTo robustness + observability)**: the fallback awaits the
Settings hub TITLE (proves the hub, not a body-text coincidence); the hub-row
lookup retries on the UNMERGED tree; the not-found error now carries
gateVisibleTexts() (the same contract as gateAwaitAnchor) so a repeat is
diagnosable from the log alone. Gate B: no code change — the infrastructure
rerun channel (R-15 precedent).

No test deleted, skipped or weakened; no assertion touched.
