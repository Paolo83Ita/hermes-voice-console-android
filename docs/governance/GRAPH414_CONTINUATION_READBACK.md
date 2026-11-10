# GRAPH 4.14 — CONTINUATION READ-BACK (CP-G4.14-C11) — OWNER PHYSICAL RETEST ROUND

Mission mode: CONTINUE_THE_EXISTING_GRAPH_FROM_LATEST_VERIFIED_REMOTE_AUTHORITY.
Fresh remote tip prevails. No restart of Graph 4.14; no repeated discovery; no
recreated matrix/authority/theme; main untouched; no force-push.

## NODE
CP-G4.14-C11 — continuation after the Owner's physical retest of
**OWNER RUNTIME THEME TOWER PACKAGE 7** (0.3.7-alpha-graph4.14-runtime-theme-tower,
versionCode 10) on the **POCO X6 Pro**. The Owner granted the retest verdict and
it OPENS the `OWNER_VISUAL_RETEST_PENDING` gate with 4 defects + 1 requirement
(F-A..F-D). The gate is NOT closed; the repairs below are the response package
(Package 8) and the physical re-grant stays Owner-only.

## READ-BACK (verified this session)

| Item | Verified value |
| --- | --- |
| Active branch | graph-4/real-voice-pairing-archive-e2e |
| Local HEAD at session start | afefd3d (recycled sandbox snapshot — stale) |
| Remote tip (fetch + ls-remote verified) | 89e8da0 (CP-G4.14-C10 terminality convergence) |
| Delta | 0 ahead / 65 behind → ff-only afefd3d → 89e8da0 (forward-only) |
| Working tree | clean at read-back |
| main | untouched |
| CI at tip | build 42642866825 SUCCESS (496/496, lint 0/73), e2e dual full samples arbitrated at C10 |
| Open gates at read-back | OWNER_VISUAL_RETEST_PENDING + LLM_PHYSICAL_QUALIFICATION_PENDING |

## OWNER RETEST VERDICT (Package 7 on POCO X6 Pro) — registered verbatim-class

| ID | Finding (Owner, it) | Repair surface |
| --- | --- | --- |
| F-A | "l'elemento centrale dell'immagine della torre risulta duplicato e sfasato in basso rispetto all'immagine retrostante" (Console vocale) | ReferenceTowerVisual geometry |
| F-B | "alcune scritte nel tema scuro sono scritte in nero e risultano quasi invisibili" | fixed-palette text sweep |
| F-C | "alcune voci rimangono non utilizzabili perché il runtime Android richiesto non è disponibile e lo stesso il modello di intelligenza locale" | §19 verdict truth + runtime probe |
| F-D | "ricontrollare l'impaginazione dei testi e la disposizione dei testi, che si adatti ad ogni tipo di schermo" | ledger-class layout sweep |

## ROOT CAUSES (code-proven) + REPAIRS (all in this package)

### F-A — Tower duplication: crop coordinates vs image rect (mechanism, numeric)
The extraction layer crops (rings/portal/crown sign) are measured as fractions
of the asset's normalized stage (680×963 BALANCED / 460×652 LOW, aspect ≈1.416),
but `LayerAt` placed them at fractions of the CONSOLE STAGE BOX
(`min(screen·0.50, width·1.375)` — aspect ≤1.375) while the architecture is
drawn `ContentScale.FillWidth + BottomCenter` (top-clipped when the box is
shorter). Result on the POCO X6 Pro (412×457.5dp stage, 412×583.5dp drawn
image, 126dp top clip) — MEASURED drift of the OLD placement:

| Layer | OLD y drift vs baked | OLD size error |
| --- | --- | --- |
| ring_outer | +88.2 dp | −39.0 dp |
| portal | +37.2 dp | −27.7 dp |
| crown_sign | +122.2 dp | −24.6 dp |

Every crop rendered a SECOND copy of its baked content, offset DOWNWARD and
smaller — the Owner's duplicated, misaligned central element. The Canvas
dynamics (portal glow 0.815h, energy 0.455h) shared the same broken base.

REPAIR: `ReferenceTowerVisual` now computes ONE IMAGE RECT (the exact rectangle
FillWidth+BottomCenter produces from the architecture painter's INTRINSIC
aspect — no hardcoded constant; manifest fallback 963/680) and draws ALL
layers — halo, architecture, 5 crops, Canvas dynamics, energy mask — inside
that rect (bottom-anchored; stage `clipToBounds` preserves the §41 clipping
semantics). Post-repair drift = **0.0 dp on every layer** (numeric check in
the session log; asset-level coherence additionally covered by the CI
screenshot channel).

### F-B — black text in dark theme: swatch labels colored by the SWATCH's palette
`AppearanceScreen.ThemeSwatch` colored its label with the swatch's FIXED
palette (`colors.inkSecondary` / `colors.bronze` where `colors` =
MarbleLightColors or ObsidianNightColors). In the dark theme the UNSELECTED
Marble swatch label rendered MarbleLight `inkSecondary` 0xFF5A554A (near-black)
on the obsidian shell ≈ **2.6:1** — the Owner's "scritte in nero quasi
invisibili". This is the same defect class as the 4.13 "device information
almost black on a dark panel" finding, in the one component that still used a
fixed palette for AMBIENT text. Full-tree sweep: AppearanceScreen is the ONLY
file importing the fixed palettes (grep-verified); all other text is semantic
(`c.textPrimary/...`) and the live two-theme audit re-ran at **50 rows / 0 FAIL**.

REPAIR: swatch labels now use ambient semantic tokens
(`c.textPrimary` selected / `c.textSecondary` unselected — ≥4.5:1 in BOTH
themes; the selected cue stays on the swatch border). The swatch preview boxes
keep the fixed palette intentionally (they PREVIEW the theme's own colors on
their own surface).

### F-C — "runtime Android richiesto non disponibile" on nine usable components
Two stacked defects in `CompatVerdictResolver` + `ModelActionResolver`:
1. `runtimeRequirement != null && recordState == null` resolved
   RUNTIME_UNAVAILABLE unconditionally — on a FRESH INSTALL every
   VERIFIED_DOWNLOADABLE entry with a runtimeRequirement string (vosk model,
   Riccardo, Paola, Qwen GGUF, wake trio, Silero, GTCRN = 9 cards) showed
   "Non utilizzabile: il runtime Android richiesto non è disponibile" although
   the runtimes are REAL Gradle-bundled AARs (vosk-android 0.3.75, sherpa-onnx
   v1.12.24, onnxruntime 1.20.0, llama-android 0.1.1 — all in
   app/build.gradle.kts).
2. `ModelActionResolver` has no branch for RUNTIME_UNAVAILABLE →
   DETAILS_ONLY → **no download button at all**: the voices and the local
   model were literally unusable, exactly as the Owner reported.

REPAIR (truth-keyed, not label-keyed):
- `CompatVerdictResolver.resolve` gains a typed `runtimeAvailable: Boolean`
  (default true); with a real runtime present the verdict is NOT_DOWNLOADED
  (→ §21 DOWNLOAD primary action); RUNTIME_UNAVAILABLE now fires ONLY when the
  runtime seam is genuinely absent (stays honest).
- NEW `models/ComponentRuntimeAvailability.kt`: per-family probes over the
  LINKED native seams (`com.k2fsa.sherpa.onnx.OfflineTts`,
  `org.vosk.Model`, `ai.onnxruntime.OrtEnvironment`) + the injected llama.cpp
  seam (`LocalLlmEngine.hasVerifiedRuntime`) — a build that drops a runtime
  loses the class and the card honestly flips back to RUNTIME_UNAVAILABLE.
- `ModelManagerScreen` call site feeds the probe result into the verdict.
- Local-experimental profile selectability was already keyed to
  `hasVerifiedRuntime` (CP-G4.14-009) — unchanged.
- The Qwen GGUF (522 MB) and the voices remain download-first-run by design
  (§3 external access policy: verified sources, published checksums); after
  the repair the cards say NOT_DOWNLOADED and offer DOWNLOAD — the physical
  download leg stays part of the Owner retest (Package 7 runbook steps 6–10).

PINS: `ModelUxSemanticsTest` — the old unconditional-RUNTIME_UNAVAILABLE pin
updated to the honest `runtimeAvailable=false` case + 2 new pins:
integrated-runtime-with-no-record → NOT_DOWNLOADED + DOWNLOAD action;
probe family mapping (LOCAL_LLM injected seam, missing class → absent).

### F-D — adaptive text layout (ledger-class sweep)
- Console status-pill row: fixed non-wrapping Row → `BadgeFlowRow`
  (BADGE_ROW_OVERFLOW class; bridge + queued + mic labels wrap at 360dp).
- TowerStudioScreen: 3 switch rows (autoconnect / tailscale / consequential)
  had unweighted label columns → `weight(1f)` (UNWEIGHTED_TEXT_COLUMN class;
  the long "azioni consequenziali" label no longer overflows).
- Re-verified repaired classes: SettingsRow (weight+56dp), VoiceAudio/
  LocalIntelligence/Connection/History/Diagnostics/Archive badge rows already
  on BadgeFlowRow; config card/profile selector governed by the §14/§15/§16
  width contract (stacked <600dp).
- The CI visual channel (`ScreenshotMatrixTest`, 360/393/411/600dp ×
  fontScale 1.0/1.3/1.5) continues to arbitrate the physical re-check; no
  harness change was needed.

## TEST

- Unit suite (local, post-repair): **498/498 GREEN** (68 suites; 0 f/e/s;
  JUnit XML independently re-parsed — 496 baseline + 2 net new pins).
- Lint: **0 errors / 72 warnings** (exactly the previously classified set;
  0 newly introduced).
- F-A numeric drift check: 0.0 dp on all layers post-repair (see table above).
- Live two-theme rendered contrast audit re-run: 50 rows / 0 FAIL.

## FILES
- Modified: ReferenceTowerVisual.kt (image-rect coordinate system),
  AppearanceScreen.kt (swatch labels), ModelUxSemantics.kt (typed
  runtimeAvailable), ModelManagerScreen.kt (probe wiring), TowerStudioScreen.kt
  (weighted switch labels), ConsoleScreen.kt (BadgeFlowRow pills),
  app/build.gradle.kts (0.3.8 / versionCode 11 / PACKAGE 8).
- Added: models/ComponentRuntimeAvailability.kt.
- Tests: ModelUxSemanticsTest.kt (updated pin + 2 new).

## CHECKPOINT
CP-G4.14-C11 — Owner retest intake + F-A..F-D repairs: DONE (this package).
State after this package: **GRAPH414_OWNER_RETEST_REPAIRS_PACKAGE_READY** with
`OWNER_VISUAL_RETEST_PENDING` (round 2) + `LLM_PHYSICAL_QUALIFICATION_PENDING`
unchanged — no physical verdict is self-granted.

## NEXT
1. Package 8 APKs (arm64-v8a / universal / x86_64) + SHA-256 receipt.
2. Push to graph-4/real-voice-pairing-archive-e2e (never main); CI arbitrates.
3. Owner physical retest round 2 on POCO X6 Pro (+ Xiaomi 11 Lite 5G NE),
   Package 7 runbook steps re-run with the F-A..F-D checklist.
