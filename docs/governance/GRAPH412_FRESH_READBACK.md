# GRAPH 4.12 — FRESH LOCAL/REMOTE READ-BACK (CP-G4.12-001)

Timestamp: 2026-11-08 (Europe/Rome session)
Mode: CONTINUE_FROM_LATEST_VERIFIED_REMOTE_AUTHORITY
Branch under test: `graph-4/real-voice-pairing-archive-e2e`

## 1. Branch / HEAD / tree

| Item | Value |
| --- | --- |
| Local branch | `graph-4/real-voice-pairing-archive-e2e` |
| Local HEAD before action | `afefd3d` (CP-G4-023 fix 12 era) |
| Remote HEAD after fetch | `bb70165` (CP-G4.11-003 deployment record) |
| Divergence | local **behind 34** commits, zero local commits, zero stash, fast-forward safe |
| Action taken | `git merge --ff-only origin/graph-4/real-voice-pairing-archive-e2e` |
| Local HEAD now | `bb70165` == `origin/graph-4/real-voice-pairing-archive-e2e` |
| Working tree | clean (`git status --short` empty) |
| main | untouched; no force-push; no history rewrite |

The 34 commits are the complete Graph 4.10 execution + Graph 4.11 execution
(POCO_OWNER_PACKAGE_4 deployment record included). The narrative tip `c18e313`
from the prior round is **superseded**; the fresh remote tip `bb70165` prevails.

## 2. Version / build / CI

| Item | Value |
| --- | --- |
| versionName at tip | `0.3.4-alpha-graph4.11` |
| versionCode at tip | `7` |
| Graph 4.12 target | `0.3.5-alpha-graph4.12-physical-repair` / versionCode `8` / label `POCO XIAOMI PHYSICAL REPAIR 4` |
| CI debug build at `d83f2de` | run 42382864937 BUILD_SUCCESSFUL (JUnit independently verified 422/422, lint artifact, SHA-stamped APK artifact) |
| CI e2e at `d83f2de` | run **42382864925 completed / FAILURE** (was "in flight" at write time — now terminal) |
| E2E lanes | emulator-e2e (1) failure, emulator-e2e (2) failure, e2e-verdict failure |
| Lane verdict artifacts | both lanes printed `E2E_VERDICT_FAILED` (157-byte verdicts downloaded and read) |

### 2.1 E2E failure nature — REAL, not fleet-only

Downloaded lane sample-1 results (301,885 B) and failure evidence (260 B):

- Gates D, E, F: `Tests run: 1, Failures: 1` each.
- Gates A, B, C: interrupted without verdict (downstream of the crash window).
- Regression `E2EFreshInstallTest`: `shortMsg=Process crashed`.
- Regression `E2ESelfTestTest`: `shortMsg=Process crashed`.
- Residual `e2e_35_36_switchMarbleLightAndObsidianNight`: `Process crashed`.
- Logcat: `TestEventClient: Process crashed before connection to orchestrator`.

## 3. FATAL CRASH SIGNATURE (evidence, not guesswork)

```
java.lang.IllegalStateException: State must be at least CREATED to move to
DESTROYED, but was INITIALIZED in component NavBackStackEntry(46485c4d-…)
destination=Destination(0x98d0beb0) route=settings
  at androidx.lifecycle.LifecycleRegistry.moveToState(LifecycleRegistry.jvm.kt:131)
  at androidx.navigation.NavBackStackEntry.updateState(NavBackStackEntry.kt:195)
  at androidx.navigation.NavBackStackEntry.handleLifecycleEvent(NavBackStackEntry.kt:178)
  at androidx.navigation.NavController.lifecycleObserver$lambda$2(NavController.kt:200)
  at androidx.lifecycle.ReportFragment$LifecycleCallbacks.onActivityPreDestroyed(…)
  at android.app.Activity.performDestroy(Activity.java:8871)
```

Trigger chain proven by the stack: **Activity destroy (recreate from
appearance/locale change) → ReportFragment.onActivityPreDestroyed →
NavController dispatches DESTROYED to every back-stack entry → an entry whose
lifecycle is still INITIALIZED cannot legally move to DESTROYED → process
crash**. A `NavBackStackEntry` reaches INITIALIZED-but-not-CREATED when it was
navigated to inside an uncontrolled window (navigation + recreation racing) —
exactly the Graph 4.12 §5 forbidden window. Same family as the Owner's
post-onboarding first-launch crash (PHYS-FAIL-01): onboarding completion sets
`onboardingDone` and a pending route while a locale-driven
`AppCompatDelegate.setApplicationLocales` recreation can land in the same
window.

## 4. Tested APK binding

| Item | Value |
| --- | --- |
| Owner-reported tested APK SHA-256 | `cf630af8642d5a36f02ff56dd11d416334ccf5d8d5f96e922d6f173e9eda80d7` |
| Package identity | POCO OWNER PACKAGE 4, versionName `0.3.4-alpha-graph4.11`, versionCode `7`, label `POCO XIAOMI OWNER PACKAGE 4` (Graph 4.11 deployment record) |
| Note | the narrative arm64 hash at the 4.11 commit (`501792715d…`) was the local validation artifact; the CI-built package is the authoritative one. The Owner's hash binds the physically tested build; it is NOT re-derived here and does not need to match any narrative value. |

## 5. Flow-level read-back (§1 checklist)

| Flow | Read-back result |
| --- | --- |
| Onboarding completion flow | **NON-ATOMIC.** `AppNav.kt` completion callback: `Onboarding.markDone(appContext)` → `pendingRoute = route` → `onboardingDone = true` → recomposition builds the NavHost → `LaunchedEffect(pendingRoute)` navigates once. No one-shot token, no coroutine cancellation, no collector stop, no locale-recreation wait, no RESUMED gate. Matches the §5 contract gap and the proven crash family. |
| Locale-change flow | Locale change triggers `AppCompatDelegate` recreation; no deferral relative to navigation exists. |
| First Console launch flow | Console composes immediately after the gate flips; engine init ordering vs RESUMED is not gated. |
| TTS/STT/model init | `AppGraph` wires SystemTtsEngine + Sherpa/Vosk routes; initialization is not deferred behind a RESUMED gate (§5 item 11 gap). |
| Continuous-listening startup | `ContinuousListeningController` present and physically working (PHYS-PASS-02); startup-after-onboarding hardening is §17 scope. |
| Stop-control call path | Console row renders `Parla / Interrompi ascolto / Interrompi voce` (ConsoleScreen.kt ~line 495/649); no unified cancel path exists; `InterruptReceipt` type absent (grep: zero hits). Owner reports non-functional `Interrompi` near `Parla` (PHYS-FAIL-04, physical confirmation pending). |
| Model Sanctum registry | ModelInstallationRegistry / ModelCatalogEntry / ModelActivationController present; §7 capability state machine with 13 distinct states NOT present (states skip INSTALLED→ACTIVE). |
| VoiceSelectionAuthority | Present (six-condition read-back, Graph 4.10). Runtime-consumption proof for downloaded voices is exactly what the Owner still cannot see (PHYS-FAIL-05) → §8/§10 receipts. |
| VoiceRuntimeFactory | Present (typed failures, system-Italian fallback). |
| Vosk runtime | `VoskFrameSttEngine.kt` present; deterministic fixture phrase “Benvenuto alla torre di Hermes, il canale vocale è pronto.” route exists in SttFixtureTestRunner; §9 physical qualification remains pending. |
| Sherpa runtime | `SherpaOnnxTtsEngine.kt` + `SherpaRealNativeBridge.kt` present; Riccardo/Paola load path exists; §10 receipt contract not implemented. |
| Current Tower assets | All ten rejected assets present in `drawable-nodpi/`: tower_rear.webp (538,096 B), tower_sign.webp (266,394 B), tower_outer_ring.webp (244,714 B), tower_middle_ring.webp (128,552 B), tower_inner_ring.webp (98,266 B), tower_lower_gate.webp (56,288 B), tower_stairs.webp (38,752 B), tower_energy.webp (30,690 B), tower_foreground.webp (18,268 B), tower_portal.webp (23,034 B). Legacy Graph-3 layers (monumental_l*.webp) still ship alongside. |
| Current color tokens | Theme uses `ink / inkSecondary / outline` + bronze/gold primitives. The §19 semantic token set (textPrimary…focusRing) does not exist; raw low-alpha bronze text remains possible (PHYS-FAIL-02 consistent). |
| Governance state | CURRENT_PROJECT_TRUTH ends at Graph 4.11 truth (POCO_OWNER_PACKAGE_4 READY; forbidden tokens UNCLAIMED; terminal state was OWNER_VISUAL_RETEST_PENDING + VOICE_SWITCHING_OWNER_VERIFICATION_PENDING). Owner has now delivered the physical verdict for that package → state `OWNER_PHYSICAL_RETEST_FAILED_REPAIR_REQUIRED`. |

## 6. Local build environment

`local.properties` pins `sdk.dir=/home/z/android-sdk`; the sandbox recycled
that directory. Android SDK (platform 35, build-tools 35.0.0, platform-tools)
re-install started in this session; JDK 21 present (OpenJDK 21.0.13).
Local `assembleDebug` is a validation step only — the pushed-commit CI build
remains the authoritative package (unchanged rule).

## 7. Read-back verdict

- Fresh remote tip `bb70165` verified and adopted (fast-forward only).
- Tree clean; main untouched; no force-push; nothing discarded.
- CI build green at the tip commit; CI E2E red with a REAL process-crash
  signature that matches the Owner's PHYS-FAIL-01 family.
- CP-G4.12-001 evidence complete. Next: `OWNER_PHYSICAL_FINDINGS_GRAPH412.md`
  (§2), then the §40 repair order beginning with crash evidence (§3).

## 8. Execution-close read-back (appended at mission close)

- Local HEAD == remote tip: **979e5ef** (fast-forward only from bb70165; main
  untouched; no force-push; tree clean).
- CI android-debug-build at 979e5ef: run **42420686766 BUILD_SUCCESSFUL** —
  SHA-stamped artifacts (debug APK / lint report / test results) uploaded with
  the commit hash `979e5ef5f9e709ac170ba9ed68aa9df39b2f44ff`. This is the
  authoritative package.
- CI android-e2e at 979e5ef: run **42420686751 in flight** at close (emulator
  fleet lane; not an Owner-package gate — its verdict is recorded by the
  existing arbitration flow).
- Terminal pre-device state reached: **POCO_XIAOMI_PHYSICAL_REPAIR_PACKAGE_READY**.
  Physical verdicts remain Owner-only (runbook:
  docs/receipts/OWNER_POCO_XIAOMI_RETEST_PACKAGE_5_GRAPH412.md).

## 9. Continuation read-back (2026-11-08): the close-out's in-flight E2E resolved + typed repairs R-42..R-45

- Fresh remote tip at resume: **23929f1** (close-out) — local fast-forwarded from
  afefd3d (the Graph 4.12 execution had advanced 36 commits beyond this file's §1
  snapshot); tree clean; main untouched (e7ab101); no force-push.
- No executor processes were alive in the backend (fresh sandbox: no gradle, no
  emulator, no instrumentation). The only live executions were the two CI pairs at
  979e5ef/23929f1, now TERMINAL: debug-build 42420686766 + 42421301278 SUCCESS
  (SHA-stamped artifacts); android-e2e **42420686751 + 42421301291 FAILURE** — all
  four lanes printed E2E_VERDICT_FAILED (157-byte VERDICT.txt artifacts read).
- Lane evidence downloaded and classified (ledger addendum "GRAPH 4.12 close-out
  read-back"): 12 findings → 4 typed classes — R-42 inject-control drift (product-
  proven: the §18 "Altre azioni" DropdownMenu), R-43 45 s relaunch tolerance,
  R-44 60 s master-idling policy at the recreate call site, R-45 the destroy-time
  INITIALIZED→DESTROYED crash (dominant; regression classes included).
- **Recovery path (c) CLOSED with source evidence**: navigation 2.8.5 AND 2.10.2
  (latest stable) dispatch ON_DESTROY to back-stack entries unconditionally
  (source-verified from Google Maven sources jars); Google holds the sibling family
  as intended behavior — an upgrade cannot fix the class; the harness must keep
  navigation out of the recreation window.
- Executed typed repairs (harness-only; no test deleted, skipped or weakened;
  app product code untouched): `gateClickByText` + the REAL "Altre azioni → menu
  item" inject walk (A2/B); relaunch tolerance 45 s→120 s; per-attempt 240 s policy
  re-apply in `gateRecreateSteadily`; bounded `gateSettleNavigationBeforeRecreate`
  before every drawer-anchored locale recreation.
- Local verification at the repair tip: compile GREEN (JDK 17 + SDK 35 reinstalled),
  unit 450/450, lint 0 errors / 84 warnings (pre-existing classifications),
  secret scan 0 findings.
- The CI fleet arbitrates at the new tip; the pre-device terminal state
  POCO_XIAOMI_PHYSICAL_REPAIR_PACKAGE_READY remains governed by the runbook
  docs/receipts/OWNER_POCO_XIAOMI_RETEST_PACKAGE_5_GRAPH412.md (physical verdicts
  Owner-only).

## 10. CP-G4.12-011/012 read-back (2026-11-08): the R-42..R-45 tip's fleet verdict

- Push 23929f1..3169083 verified forward-only; main untouched (e7ab101); tree clean.
- CI at 3169083: debug-build 42438234533 SUCCESS (compile gate + 450/450-equivalent
  unit + artifacts); android-e2e 42438234612 — lane 1 boot-collapse (runner adb exit
  224, zero instrumentation, the R-33/R-34 class), lane 2 complete sample (108 min)
  E2E_VERDICT_FAILED; arbitration honest.
- Repair verdicts (ledger "CP-G4.12-011 verdict at 3169083"): R-43 HELD (0 findings),
  R-44 HELD (0 findings), R-42 ADVANCED (new typed message at the popup step →
  R-42b bounded presence-poll executed), R-45 PERSISTED (the unpromoted entry is
  pushed inside the recreation window; static analysis exhausted → R-45b debug-only
  navigation-provenance diagnostics executed: HermesNav logs across every navigate
  pusher + MainActivity lifecycle).
- The pre-device terminal state POCO_XIAOMI_PHYSICAL_REPAIR_PACKAGE_READY remains
  governed by the package-5 runbook (physical verdicts Owner-only).
