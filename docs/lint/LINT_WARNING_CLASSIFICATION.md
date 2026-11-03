# LINT WARNING CLASSIFICATION — GRAPH 4 NODE 0 (CP-G4-002)

Baseline at `ca8980b` (verified from the Owner-uploaded `lint-results-debug.txt`, run `41380930670`):
**2 errors, 36 warnings.** Terminal condition after NODE 0: **0 errors**; every warning individually
classified below. No blanket suppression is used anywhere (`abortOnError=true`, `warningsAsErrors=false`).

## Errors (repaired)

| # | Rule | Location | Diagnosis | Repair |
| --- | --- | --- | --- | --- |
| 1 | `ProduceStateDoesNotAssignValue` | `ui/monumental/OracleCore.kt:47` | Compose lint analysis limitation + suboptimal lifecycle coupling: the state assignment happened inside a nested `withFrameNanos` callback, statically invisible to lint; additionally `state` was a `produceState` key, resetting the animation phase on every console-state change | Replaced by `ui/runtime/AnimationFrameClock.kt` — `LaunchedEffect(running)` + `mutableLongStateOf`, hoisted tick callback, cancellation on disposal guaranteed by Compose; clock deliberately not keyed on console state (monotonic time, phase preserved) |
| 2 | `ProduceStateDoesNotAssignValue` | `ui/components/TowerCore.kt:41` | Same class | Same controller |

## Required NODE 0 tests (`ui/runtime/AnimationFrameClockTest.kt`)

1. animation state updates when enabled (`pump advances…`, composition test)
2. static in reduced-motion mode (`composable freezes at zero…`, `pump emits exactly one static zero…`)
3. animation work stops when the Composable leaves composition (`composable stops frame work on disposal…`)
4. state changes restart/update correctly (`re-enabling after reduced motion restarts from zero`, `console state change keeps time monotonic…`)
5. no orphan coroutine remains (`cancelled pump stops consuming frames`, disposal test)
6. OLED idle policy preserved (static 0 ms frame asserted; LOW profile shares the same `running=false` gate)

## Warnings (43 at CP-G4-010 wave 2, individually classified)

| Count | Rule | Location | Classification | Disposition |
| --- | --- | --- | --- | --- |
| 36 | `GradleDependency` | `gradle/libs.versions.toml` (pinned lines incl. CP-G4-008 onnxruntime 1.20.0 and CP-G4-010 wave 2 CameraX 1.4.2 / ML Kit barcode-scanning 17.3.0) | ACCEPTED, INTENTIONAL — pinned versions (see `docs/DEPENDENCY_PINNING.md`); upgrades happen only in a dedicated named node | Documented; no change |
| 3 | `AndroidGradlePluginVersion` | `gradle/libs.versions.toml:2` | ACCEPTED, INTENTIONAL — AGP 8.7.3 pinned (mission: "do not bulk-upgrade AGP, Kotlin or Compose in this node") | Documented; no change |
| 1 | `OldTargetApi` | `app/build.gradle.kts:15` | ACCEPTED, INTENTIONAL — targetSdk 35 is current for this project; lint's "newer exists" noise refers to a preview API level | Documented; no change |
| 2 | `ObsoleteSdkInt` | `models/AndroidModelProviders.kt:27`; `res/mipmap-anydpi-v26` | REPAIRED — dead `SDK_INT >= 30` guard removed (minSdk 30 guarantees it); `mipmap-anydpi-v26` merged into `mipmap-anydpi` | Fixed in CP-G4-002 |
| 1 | `UsableSpace` | `models/ModelStorageManager.kt:47` | REPAIRED — preflight now consults `StorageManager.getAllocatableBytes` when a Context is available (larger of the two views wins) and enforces the max(2 GB, 15%) safe-reserve policy | Fixed in CP-G4-002 |
| 1 | `MonochromeLauncherIcon` | `res/mipmap-anydpi-v26/ic_launcher.xml:2` | REPAIRED — `<monochrome>` layer added (`drawable/ic_launcher_monochrome.xml`) | Fixed in CP-G4-002 |
| 1 | `DataExtractionRules` | `AndroidManifest.xml:22` | REPAIRED — `android:dataExtractionRules="@xml/data_extraction_rules"` added; cloud backup AND device transfer exclude every storage domain (INV-G4-03) | Fixed in CP-G4-002 |

## Verification

- `:app:lintDebug` executed locally and in CI (blocking step, report uploaded);
- terminal: **0 errors**; remaining warnings = the 39 accepted/pinned-version ones above (36 `GradleDependency` + 3 `AndroidGradlePluginVersion` + 1 `OldTargetApi`).
- mutableIntStateOf audit: no integer-valued `mutableStateOf` exists in `app/src/main` (both long state
  usages — the frame clocks — now use `mutableLongStateOf` inside `AnimationFrameClock`); nothing to convert.
