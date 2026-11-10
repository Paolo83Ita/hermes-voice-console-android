# GRAPH 4.14 — PACKAGE 9 CONTINUATION READ-BACK (CP-G4.14-P9)

Mission mode: CONTINUE_FROM_LATEST_VERIFIED_REMOTE_AUTHORITY.
The fresh remote read-back prevails over every narrative value. No restart of
Graph 4.14; no repeated discovery; no re-created matrix/authority/theme; no
downloader rewrite (no new defect evidence); no Tower regeneration; main
untouched; no force-push.

## NODE

CP-G4.14-P9 — continuation after the Package 9 push (Owner round-2 repairs,
CP-G4.14-C12 at tip `7fc1c0b`). This session verifies the ENTIRE Package 9
gate ladder from the remote authority and closes the first incomplete gate
(the deployment + convergence receipts). All four Round-2 defects (G-A..G-D)
are code-verified as repaired at the tip; the physical verdict stays
Owner-only.

## READ-BACK (verified this session)

| Item | Verified value |
| --- | --- |
| Active branch | graph-4/real-voice-pairing-archive-e2e |
| Local HEAD at session start | afefd3d (recycled-sandbox stale snapshot) |
| Remote tip (fetch verified) | **7fc1c0bc1731e77e65908df061aa08a402ffde66** (CP-G4.14-C12, Package 9) |
| Delta | 0 ahead / 67 behind → ff-only afefd3d → 7fc1c0b (forward-only) |
| Working tree | clean at read-back and at close |
| main | untouched |
| Previous-execution processes | none active (no gradle/kotlin/java/adb) |
| Version truth (app/build.gradle.kts + all 3 cloud manifests) | 0.3.9-alpha-graph4.14-round2-repairs / versionCode 12 / OWNER RUNTIME THEME TOWER PACKAGE 9 |

## CLOUD QUALIFICATION (parsed from artifacts — icon not trusted)

`android-debug-build` run **#179** (id 42865207716) at 7fc1c0b — SUCCESS.

| Gate | Parsed evidence | Verdict |
| --- | --- | --- |
| JUnit XML | 68 suites, **501 tests, 0 failures, 0 errors, 0 skipped** | == local report |
| Lint XML | **0 errors**, 73 warnings; rule classes {GradleDependency 42, PluralsCandidate 11, ApplySharedPref 8, AndroidGradlePluginVersion 3, MissingQuantity 3, AppBundleLocaleChanges 1, DataExtractionRules 1, OldTargetApi 1, StaticFieldLeak 1, Typos 1, UnusedAttribute 1} | == classified 72-set + 1 environment-only `OldTargetApi` instance (the documented cloud delta); **0 new unclassified rules** |
| APK artifacts | 3 variants extracted; ZIP integrity OK; manifests attribute-verified | see GRAPH414_PACKAGE9_APK_RECEIPT.md |
| Secret scan (re-run this session) | working tree 0 / history 0 | clean |

## E2E TERMINAL ARBITRATION (§17)

`android-e2e` run **#196** (id 42865207852) at 7fc1c0b — dual-lane R-25
matrix, both lanes completed their walk with honest `E2E_VERDICT_FAILED`
verdicts; the arbitration job failed the run (missing evidence is not
success — semantics preserved).

Classification method: per-class verdict + first-failure-signature diff
against the **accepted C10 baseline** (tip 1d3adb9, run 42642866873, both
lane artifacts downloaded and parsed this session).

| Class (both lanes) | Baseline | Package 9 | Classification |
| --- | --- | --- | --- |
| gate_A (start markers only, no completion) | no-signature | identical | STANDING (TIMEOUT_WITH_PROGRESS family, R-16/R-24 lineage) |
| gate_B "inject menu item not found or without OnClick (it)" | identical | identical | STANDING_PREEXISTING_FAILURE (R-21 unresolved hub-row lookup lineage) |
| gate_E "settings-hub row 'Lingua' not found (it)" + same visible dump | identical | identical | STANDING_PREEXISTING_FAILURE |
| gate_C / gate_D / gate_F | OK | OK | GREEN (unchanged) |
| regression FreshInstall 4/5, ModelManager 2/5, VoiceFlow e2e_14 1/6, SelfTest 2/3 (ST-49 truth breach), residual 2/7, BridgeTruth 1/1, LifecycleProfile OK, Onboarding OK | identical signatures | identical signatures | STANDING_PREEXISTING_FAILURE (byte-identical line) |

**Failure-set delta vs accepted baseline: EMPTY on both lanes → NO new
Graph 4.14 regression.** Per §17 the historical fleet campaign is not
restarted without new evidence; the standing families remain honestly
recorded and the Owner physical qualification path is preserved.

## PACKAGED RUNTIME RESOLVER TRUTH (§6)

Production construction path verified from tip source:
`HermesApp : Application()` → `AppGraph(applicationContext)` (application
Context only — no Activity/View/NavController retained) →
`AppGraph.localLlmEngine` (AppGraph.kt:461) injects explicitly
`LlamaCppAndroidRuntime(LlamaRealNativeBridge())` → native init → model path
(filesDir/models/llm/qwen25-05b, catalog SHA-256 + size gate) → generation.

Reference classification:

| Reference | Site | Class |
| --- | --- | --- |
| NoVerifiedLlmRuntime (class) | LlmRuntime.kt:46 | PRODUCTION_ACTIVE — typed fallback for genuine native-load failure (retained per mission) |
| NoVerifiedLlmRuntime (default param) | LocalLlmEngine.kt:40 | SUPPORTED_ABI_FALLBACK — overridden by the production injection |
| NoVerifiedLlmRuntime (hasVerifiedRuntime gate) | LocalLlmEngine.kt:88 | PRODUCTION_ACTIVE read-back (`runtime !is NoVerifiedLlmRuntime`) |
| LlamaCppAndroidRuntime injection | AppGraph.kt:467 | PRODUCTION_ACTIVE — the ordinary ARM64 path |
| Stale "runtime richiesto non disponibile" strings | values-it sweep | NONE FOUND (C11 F-C repair holds) |

With the native libraries physically packaged (receipt §runtime inventory),
the ordinary production path resolves to **LlamaCppAndroidRuntime**, not the
fallback. QWEN state stays
`LOCAL_LLM_IMPLEMENTED_PHYSICAL_QUALIFICATION_PENDING` — no physical claim.

## QWEN DOWNLOADER (§7) — verified live in the tip, NOT rewritten

Code-verified at 7fc1c0b (ModelDownloadManager.kt): explicit bounded redirect
walk (301/302/303/307/308 ≤5 hops, https never downgraded), UA
`HermesTowerConsole-ModelDownload/1.0` on every request (line 242), connect
20s / read 45s, true-offset resume via positioned channel (never blind
APPEND), Range-ignoring 200 restart from zero, HTTP 416 safe reset (line
263), one bounded hf-mirror.com attempt (line 201) recorded in the error
string, SHA-256 gate unchanged. Pins live in the cloud 501:
ModelDownloadLifecycleTest (redirect chain + UA, stale-partial resume
verifies by hash, bounded redirect-loop failure). The pinned source was
re-verified alive at C12 (HF resolve → 302 → CDN → 200, 522,186,592 B =
catalog q5_k_m). No new transport defect evidence → no rewrite (mission
ban respected).

## PIPER / TTS (§9–§11)

Stale-string sweep of production res: no "runtime required unavailable" /
"PENDING INTEGRATION" / "not integrated" survivors. sherpa-onnx native trio
+ onnxruntime physically packaged (receipt). Piper bundle consumption is
manifest-keyed (voice_pack_catalog.json packaged). Voice A/B stays
`VOICE_AB_PHYSICAL_QUALIFICATION_PENDING` (Owner-only).

## TOWER (§12–§13) and OBSIDIAN (§14–§15)

- Crop rotation REMOVED (ReferenceTowerVisual.kt: rounds-2 comments at lines
  85–91, 232–237, 408 — crops always render at rotation 0; the shimmer is a
  Canvas ring-light sweep that cannot move geometry).
- CONTACTING arc + transcribing ticks CONCENTRIC with the measured ring band
  (center 0.5w/0.455h, radius 0.32w).
- Effects: portal breathing strengthened, ring sweep 42 s (alpha 0.22 idle /
  0.34 listening), mock stays amber never cyan (§40) — code-verified.
- Theme root: `LocalContentColor provides colors.textPrimary`
  (Theme.kt:56) + explicit token at the Owner-named hub row
  (SettingsHubScreen.kt:207).
- C12 measurement evidence in-repo: docs/visual/tower414/
  C12_CROP_ALIGNMENT_MEASUREMENT.json (static drift ≤1.2 dp both tiers,
  masked NCC ≥0.96; rotation deformation documented as the removal reason).
- Dark-screen evidence remains **OBSIDIAN_OWNER_RETEST_PENDING** — the
  physical device is the only contrast authority for the rendered screen
  (no self-granted PASS).

## CHECKPOINTS BLOCK 1+2 (report format per §16)

| Checkpoint | NODE | FINDING | ACTION | TEST | EVIDENCE |
| --- | --- | --- | --- | --- | --- |
| CP-G4.14-P9-001 | continuation read-back | stale local afefd3d / remote 7fc1c0b | ff-only + full read-back | git fetch + rev-parse | this doc READ-BACK table |
| CP-G4.14-P9-002 | cloud qualification | run #179 SUCCESS unverified | artifacts downloaded + parsed | JUnit 501/0, lint 0e/73w classified | /tmp-parsed XMLs → receipt |
| CP-G4.14-P9-003 | resolver truth | §6 classification required | production path inspected | code read (AppGraph 461–472) | §resolver table |
| CP-G4.14-P9-004 | Qwen readiness | downloader must not be rewritten | live-code verification only | 3 cloud pins in 501 | ModelDownloadManager.kt |
| CP-G4.14-P9-005 | Piper/bundles | manifest-keyed consumption | catalog packaged + probed | arsc/assets inventory | receipt §assets |
| CP-G4.14-P9-006 | tower alignment + effects | rotation/arc fixes must hold | code + C12 JSON verified | drift ≤1.2dp / NCC ≥0.96 | C12_CROP_ALIGNMENT_MEASUREMENT.json |
| CP-G4.14-P9-007 | Obsidian preparation | physical verdict pending | root fix verified, runbook issued | theme-root + hub token | OWNER_POCO_XIAOMI_RETEST_PACKAGE_9_GRAPH414.md |

## COMMIT / REMOTE READ-BACK / TERMINALITY

- Continuation commit: docs-only (receipts + governance convergence) on
  graph-4/real-voice-pairing-archive-e2e, ff-only push, main untouched.
- Implementation terminality: **GRAPH414_PACKAGE9_CONTINUATION_COMPLETE**
  with the Owner-only gates OPEN:
  `QWEN_RUNTIME_IMPLEMENTED_PHYSICAL_RETEST_PENDING`,
  `RICCARDO_RUNTIME_IMPLEMENTED_PHYSICAL_RETEST_PENDING`,
  `PAOLA_RUNTIME_IMPLEMENTED_PHYSICAL_RETEST_PENDING`,
  `OBSIDIAN_CONTRAST_REPAIR_OWNER_RETEST_PENDING`,
  `TOWER_EFFECT_REPAIR_OWNER_RETEST_PENDING`.

## NEXT

1. Owner physical retest ROUND 3 on POCO X6 Pro with the cloud ARM64 APK
   (run #179, SHA-256 bc79ced6…90e8) — runbook:
   OWNER_POCO_XIAOMI_RETEST_PACKAGE_9_GRAPH414.md (items A..W).
2. Only the Owner may set the *_OWNER_VERIFIED states after physical
   evidence (§28).
3. Standing E2E families stay recorded; no fleet restart without new
   evidence (§17).
