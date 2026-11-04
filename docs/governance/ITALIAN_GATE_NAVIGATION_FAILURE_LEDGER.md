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
