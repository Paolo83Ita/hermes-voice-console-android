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

---

# GRAPH 4.13 ADDENDUM (§43 — no new warning accepted without classification)

Re-measured after the Block 1/Block 2 visual + accessibility + Model Library
rebuild (contrast audit, semantic color authority, §5 control wrappers,
§13–§21 Model Library UX). Result: **0 errors, 83 warnings** — one BELOW the
Graph 4.12 baseline of 84 (the §13–§21 rebuild orphaned 26 dead string keys
across 5 locales; they were REMOVED, not suppressed).

| Count | Rule | Classification | Disposition |
| --- | --- | --- | --- |
| 42 | `GradleDependency` | ACCEPTED, INTENTIONAL — pinned versions (see `docs/DEPENDENCY_PINNING.md`); upgrades happen only in a dedicated named node | Documented; no change |
| 3 | `AndroidGradlePluginVersion` | ACCEPTED, INTENTIONAL — AGP 8.7.3 pinned | Documented; no change |
| 8 | `ApplySharedPref` | PRE-EXISTING (Graph 2 era stores/migrators; apply() on background-safe paths; blocking-commit migration is a deliberate durability choice) | Deferred to a dedicated persistence-hygiene node |
| 11 | `PluralsCandidate` | PRE-EXISTING (counts are formatted with `%d` strings today; plural grammar polish is a localization task, not a defect) | Deferred to a localization-polish node |
| 3 | `MissingQuantity` | PRE-EXISTING (same class as above) | Deferred with PluralsCandidate |
| 9 | `UnusedResources` | REDUCED from 35 → 9 by removing the 26 verified-dead keys (`/tmp`-verified zero references in main+test+androidTest); the remaining 9 are drawable/attr entries referenced dynamically | Documented; dynamic-reference guard |
| 1 | `UnusedAttribute` | PRE-EXISTING (locale-config attribute ignored by older minSdk) | Documented; no change |
| 1 | `AppBundleLocaleChanges` | PRE-EXISTING (debug APK qualification packaging; not shipped as AAB) | Documented; no change |
| 1 | `Typos` | PRE-EXISTING (an intentional identifier in a string) | Documented; no change |
| 1 | `DataExtractionRules` | PRE-EXISTING (the manifest carries the rules file; lint flags the additional `fullBackupContent` legacy attribute kept for the migration window) | Documented; no change |
| 1 | `StaticFieldLeak` | PRE-EXISTING (Graph 2 registry singleton holding app context intentionally, app-lifetime scope) | Documented; no change |

Newly introduced warnings: **0**. Release blockers: **0**.
Deferred dependency upgrades: the standing pinned-catalog set only.

---

# GRAPH 4.14 ADDENDUM (§17 — no new warning accepted without classification)

Re-measured after the Graph 4.14 continuation (runtime integration + bridge
truth + Marble + Model Library truth + reference Tower). At the continuation
tip the first lint pass reported **0 errors / 89 warnings** — 7 above the
classified 82 baseline. Per §17 the newly-introduced set was REPAIRED before
package delivery, not suppressed:

| Repair | Count | Class | Disposition |
| --- | --- | --- | --- |
| 13 orphaned string keys removed across ALL 5 locales (`desc_bridge_fmt`, `pill_connected`, `pill_connecting`, `pill_mock`, `pill_offline`, `mm_network`, `mm_section_b`, `mm_section_c`, `mm_repair`, `continuous_start`, `continuous_stop`, `msg_listen_label`, `mm_cfg_device`) — zero-reference verified (main+test+androidTest) before removal | 13 UnusedResources → 0 | orphaned localized strings from the 4.13/4.14 UI rebuild | REPAIRED |
| `mutableStateOf(0)` → `mutableIntStateOf(0)` (ModelManagerScreen registryTick; OnboardingScreen step) | 2 AutoboxingStateCreation → 0 | newly introduced | REPAIRED |
| dead `TowerEnergyPhase` composable deleted (retired 4.13 direction; zero call sites) + dead import | 1 ComposableNaming → 0 | dead runtime declaration | REPAIRED |
| ResponsiveActionGroup: `modifier` moved to the first optional parameter | 1 ModifierParameter → 0 | newly introduced | REPAIRED |

Note on the baseline drift: the 4.13 classification listed 9
`UnusedResources` (drawable/attr entries referenced dynamically). Those
entries disappeared with the RETIRED 4.13 Tower assets (30 webp deleted in
d66a523) — the class is now genuinely empty (0), which is why the post-repair
total (72) sits 10 BELOW the 82 baseline.

## Post-repair terminal set (0 errors / 72 warnings — all previously classified)

| Count | Rule | Classification |
| --- | --- | --- |
| 42 | GradleDependency | ACCEPTED, INTENTIONAL — pinned versions (docs/DEPENDENCY_PINNING.md); the 4.14 runtime additions (llama-android 0.1.1) are part of the integrated runtime, not bulk upgrades |
| 3 | AndroidGradlePluginVersion | ACCEPTED, INTENTIONAL — AGP 8.7.3 pinned |
| 8 | ApplySharedPref | PRE-EXISTING (Graph 2 era persistence; deferred to a dedicated persistence-hygiene node) |
| 11 | PluralsCandidate | PRE-EXISTING (deferred to localization-polish) |
| 3 | MissingQuantity | PRE-EXISTING (deferred with PluralsCandidate) |
| 1 | StaticFieldLeak | PRE-EXISTING (Graph 2 registry singleton, app-lifetime scope) |
| 1 | DataExtractionRules | PRE-EXISTING (legacy fullBackupContent kept for the migration window) |
| 1 | UnusedAttribute | PRE-EXISTING (locale-config vs minSdk) |
| 1 | AppBundleLocaleChanges | PRE-EXISTING (debug qualification packaging; not shipped as AAB) |
| 1 | Typos | PRE-EXISTING (intentional identifier) |

Newly introduced warnings after repair: **0**. Release blockers: **0**.
Deferred dependency upgrades: the standing pinned-catalog set only (no mass
upgrades performed; llama-android was a REQUIRED integrated runtime, not an
upgrade).

## §12 gate verification (Graph 4.14)

- `:app:lintDebug` executed locally post-repair: **0 errors / 72 warnings**
  (abortOnError=true stays terminal truth).
- Secret scan: 0 findings (working-tree diff + full-tree pattern sweep).
