# GRAPH 4.13 FRESH READ-BACK (CP-G4.13-001)

Mission: OWNER VISUAL QUALITY, APP-WIDE ACCESSIBILITY, MODEL LIBRARY UX AND
MONUMENTAL TOWER REFOUNDATION — Block 1 of 3.
Mode: `CONTINUE_FROM_LATEST_VERIFIED_REMOTE_AUTHORITY`. Executed 2026-11-09.
Rule: the fresh remote tip is authoritative; the narrative APK SHA is NOT a
current-value authority (it identifies the Owner-tested Package 4 build, below).

## 1. Repository state (fresh, fetch-verified 2026-11-09)

| Item | Value |
| --- | --- |
| Branch | `graph-4/real-voice-pairing-archive-e2e` |
| Local HEAD at session start | `afefd3d` (STALE — behind 42) |
| Fresh remote tip (`ls-remote` + fetch) | `4b8bfb2d32e24240e5ef47260c589729955e4824` |
| Action taken | `git merge --ff-only` — local fast-forwarded `afefd3d → 4b8bfb2` |
| Tree after ff | CLEAN (0 uncommitted, 0 untracked) |
| main | untouched; no force-push; no history rewrite (forward-only) |
| Latest checkpoint | **CP-G4.12-016** — R-49 drawer-settle (harness-only) |
| Chain of last 4 commits | `dd5b3e2` (CP-G4.12-015 verdict), `4b8bfb2` (CP-G4.12-016 R-49) |

## 2. CI / E2E / APK identity at the fresh tip

| Item | Value |
| --- | --- |
| Latest CI build | run **42481808334** `android-debug-build` @ `4b8bfb2` — **completed SUCCESS** |
| Latest E2E | run **42481808264** `android-e2e` @ `4b8bfb2` — completed **FAILURE** (honest) |
| E2E verdict artifacts | `e2e-verdict-sample-1` and `e2e-verdict-sample-2` both print **`E2E_VERDICT_FAILED`** (typed honest verdict; arbitration job fails unless a lane passes) |
| E2E classification | the standing Graph 4.12 harness-anchor line (R-42..R-49): crash family already ELIMINATED (zero `Process crashed` for 4 consecutive samples); remaining typed classes are drawer/menu anchor-wait and fleet-infrastructure classes; no NEW app regression is evidenced at the tip |
| Narrative APK SHA | `cf630af8642d5a36f02ff56dd11d416334ccf5d8d5f96e922d6f173e9eda80d7` = the ARM64 APK of **POCO OWNER PACKAGE 4** (versionName `0.3.4-alpha-graph4.11`, versionCode `7`) that the Owner physically tested — a historical artifact identity, NOT the current tip |
| Current versionCode | **8** |
| Current versionName | **0.3.5-alpha-graph4.12-physical-repair** |
| Graph 4.13 planned identity | versionName `0.3.6-alpha-graph4.13-visual-accessibility`, versionCode **9** (next above tip; validated again at packaging time) |
| Unit baseline (re-verified locally in the recycled sandbox) | `./gradlew :app:assembleDebug :app:testDebugUnitTest` → BUILD SUCCESSFUL, **450/450 unit tests, 0 failures, 0 errors** at `4b8bfb2` |

Toolchain re-established in the recycled sandbox: Temurin JDK 17.0.21 (`/home/z/tools/jdk-17.0.21+9`), Android SDK 35 (`/home/z/android-sdk`), committed Gradle 8.9 wrapper, sandbox mirror-init retained out-of-repo.

## 3. Current implementation surface (read at the tip)

- **Central Tower**: `ui/monumental/` (`TowerArchitecture.kt` 243 L, `OracleCore.kt`
  641 L, `MonumentalColor.kt`, `MonumentalChrome.kt`, `MonumentalType.kt`,
  `OracleCoreModel.kt`, `VisualQuality.kt`, `BurnIn.kt`) + production WebP layers
  `res/drawable-nodpi/monumental_l{0,1,2,7}_*.webp`. Graph 4.12 replacement pack:
  `docs/visual/tower412/` (ASSET_MANIFEST.json, visual_validation_report.json,
  candidates a/b/c). State: **CENTRAL_TOWER_GRAPH412_VISUAL_OWNER_REJECTED** —
  the Owner verdict names it clean-but-flat, schematic, insufficient mass/depth,
  oracle core too small, detached sign, empty upper area (see
  OWNER_PHYSICAL_FINDINGS_GRAPH413.md). Not to be polished-as-only-solution: full
  refoundation per §29–§41.
- **Semantic color tokens**: `ui/theme/Color.kt` (132 L, 51 vals) — Graph 4.12
  added `inkDim`/`redBronzeText`/marble repairs + `ContrastRatioTest`. NOT yet the
  full §6 semantic authority (no `textPrimary/textSecondary/textTertiaryReadable/
  textOnSelected/surfaceSelected/outlineFocus` complete set). Repair in CP-G4.13-003.
- **Typography tokens**: `ui/theme/Type.kt` — `brand` 22 sp SemiBold, `screenTitle`
  17 sp SemiBold, body styles; §24 hierarchy audit folds into the token repair.
- **ModelManagerScreen**: `ui/screens/ModelManagerScreen.kt` (822 L) — single flat
  list presentation over the model authority (catalog/registry/receipts/download/
  capability state machine/profiles/rollback — all REUSED, never duplicated).
  Owner defects: readability, profile layout, enum rendering, compatibility
  rendering, action hierarchy. Repair in CP-G4.13-005..008.
- **Responsive policies**: `DrawerWidthPolicy` (+ test), WindowWidth/height
  contracts in `ui/AppNav.kt`, §33 height contract in Tower code. Graph 4.10/4.9
  responsive authorities stand; compact-width action layout defect is open.
- **Owner physical finding register**: `OWNER_PHYSICAL_FINDINGS_GRAPH412.md`
  (PHYS-PASS-01 Italian responses, PHYS-PASS-02 continuous listening,
  PHYS-FAIL-01 first-launch crash → repaired in 4.12, PHYS-FAIL-02 contrast →
  this graph's surface). This graph's register:
  `OWNER_PHYSICAL_FINDINGS_GRAPH413.md` (new physical evidence block).
- **Visual reports**: `ACCESSIBILITY_COLOR_CONTRAST_REPORT.md`,
  `CENTRAL_TOWER_REPLACEMENT_REPORT.md`,
  `docs/receipts/CENTRAL_TOWER_VISUAL_REFOUNDATION_REPORT.md`,
  `docs/visual/tower412/visual_validation_report.json`.
- **Screenshots / reference**: `download/graph4-monumental-preview-pack/*.png`
  (Graph 4 render pack), `design/monumental/preview/` (Graph 3 verified pack),
  Owner reference image `upload/Immagine_Hermes_Tower.png` (authority for
  monumentality per §33).

## 4. Physical PASS preservation register (preliminary, Owner-granted)

| State | Meaning |
| --- | --- |
| `ITALIAN_RESPONSE_PHYSICAL_PASS_PRELIMINARY` | local responses in Italian (ResponseLocaleResolver/Catalog chain) |
| `CONTINUOUS_LISTENING_PHYSICAL_PASS_PRELIMINARY` | continuous-listening loop alive |
| `UNIFIED_INTERRUPT_PHYSICAL_PASS_PRELIMINARY` | unified Interrompi control functional |
| `QR_SCANNER_PHYSICAL_OPEN_PASS` | QR scanner opens physically |

Contract: preserve; regression-test; modify only if directly required by the
visual or accessibility repair; no new architecture around them (§0, §10).

## 5. Scope freeze (§2)

Implementation surfaces: (A) app-wide visual accessibility + readable semantic
colors; (B) Model Library + Model Profile responsive UX reconstruction;
(C) Central Hermes Tower visual refoundation. NOT in scope: pairing protocol,
Archive, local LLM runtime, wake-word qualification, Tower API, Graph 1.7, QR
protocol, model catalog scope, continuous-listening architecture, interrupt
architecture.

## 6. Next exact action

CP-G4.13-002 — app-wide rendered contrast audit (`APP_WIDE_RENDERED_CONTRAST_AUDIT_GRAPH413.{json,md}`),
then CP-G4.13-003 semantic color authority repair, CP-G4.13-004 screenshot test
foundation, then Block 2 / Block 3 in the §47 execution order.
