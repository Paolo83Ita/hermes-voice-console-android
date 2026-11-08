# CURRENT PROJECT TRUTH — Hermes Voice Console Android

Updated: 2026-11-01 (GRAPH 1, Node 0–5). Supersedes narrative claims elsewhere.

## 1. Foundation Run reconciliation (physical read-back, 2026-11-01)

| Foundation claim | Physical read-back | Verdict (typed) |
| --- | --- | --- |
| Stage A documentation completed | 19 files under `docs/` (+ README, BUILDING, FINAL_REPORT) | VERIFIED — DOCUMENT_CREATED |
| 7 Mermaid diagrams rendered | 7 ` ```mermaid ` blocks: 2 in ARCHITECTURE_DIAGRAM.md, 5 in SEQUENCE_DIAGRAMS.md | VERIFIED — DIAGRAM_SOURCE_CREATED (blocks present; source-of-truth = markdown) |
| 3 visual proposals + comparison | `design/mockups/`: marble_light, obsidian_night, comparison (HTML+PNG each) | VERIFIED — VISUAL_PREVIEW files present |
| Marble Light success green replaced with #2A7654 (~4.79:1) | `2A7654` present in Color.kt, both mockup HTMLs, VISUAL_DIRECTION.md; 4.79:1 value not re-computed in Graph 1 | VERIFIED (string) / NOT_RE_VERIFIED (ratio) |
| Obsidian Night recommended default | Recommendation recorded in Foundation docs (VISUAL_DIRECTION.md) | CARRIED (Stage A decision stands) |
| 50 Kotlin files (44 main + 6 tests) | 44 + 6 = 50 `.kt` files on disk | VERIFIED |
| 26 JVM test cases | 33 `@Test` annotations across 6 test classes | CLAIM_SUPERSEDED — physical count is 33 |
| 8-screen Compose UI | 8 screen files under `ui/screens/` + AppNav | VERIFIED |
| 15-state truthful UI machine | `ConsoleState` 15 entries + `ConsoleStateMachine` table | VERIFIED |
| Gradle scaffold created | settings/build/libs.versions.toml/gradle.properties present; wrapper jar+scripts MISSING | VERIFIED + GAP_FIXED (wrapper generated, G-007) |
| 92 working-tree modifications at handover | all 92 were file-mode flips (0 content delta) | NORMALIZED (git restore, no content change) |
| No Android build executed (Foundation env) | true at Foundation time | SUPERSEDED — see §2 |
| Final ZIP `download/hermes-voice-console-android-v0.1.zip` | exists, 4.2 MB, sha256 `8f5de42e6b065a9e1cde8df12a305aadf64e4940f6df5d8501669834b893ca80` | VERIFIED |
| Git checkpoints created | 3 commits: 7a86640 (stage-a-docs), 3aa69ad (stage-a-visuals), ffe6749 (stage-c-package); no remote | VERIFIED + CONVERGENCE_DONE_BY_GRAPH_1 |

Secret/device scan: 0 findings in working tree and full git history (scripts/secret_scan.py).

## 2. Build truth (as of 2026-11-01)

- Toolchain: Temurin JDK 17.0.21 · Gradle 8.9 (wrapper, committed) · AGP 8.7.3 · Kotlin 2.0.21
  · compileSdk 35 · build-tools 35.0.0 · minSdk 30 · targetSdk 35.
- LOCAL verification build: `./gradlew :app:assembleDebug :app:testDebugUnitTest` →
  **BUILD SUCCESSFUL**, **33 tests completed, 0 failed** — UNIT_TEST_EXECUTED (local JVM).
- Repair history: initial local build failed with 44 main-source errors (11 files), then
  9 test-source errors, then 7 test failures; all repaired (see `DECISION_LOG.md` G-001..G-005,
  `WORK_LOG.md` N4).
- Local APK (validation artifact, NOT the deliverable): `app-debug.apk`, 56 MB,
  sha256 `163d700a29161d78714dccbd13c22d3710ec1aaa6401854de082a2f913357557`.
- CLOUD build (authoritative): GitHub Actions run 41296592392 (workflow `android-debug-build`),
  commit `505047e9e05351a74b2b400ccca6442c364118ff` → **CLOUD_BUILD_PASSED**, iteration 2/3
  (run 41296487511 CLOUD_BUILD_FAILED: setup-android action's internal license prompt hit EOF;
  repair = preinstalled runner SDK + yes-piped license acceptance). CI tests: **33 tests,
  0 failures** (test-results artifact) — UNIT_TEST_EXECUTED (CI). `lintDebug` executed,
  non-blocking — ANDROID_LINT_EXECUTED.
- Cloud APK (the Owner-installable deliverable): 57,848,596 bytes,
  sha256 `f7a767354e241961928f1b536ec71c23e005920f55b7e7a791b29cc126cd0be7` — APK_CREATED,
  APK_HASHED, APK_BOUND_TO_COMMIT (full receipt: `APK_RECEIPT_V0.1.md`). Also copied to the
  local deliverables folder as `hermes-voice-console-debug-v0.1-505047e.apk`.

## 3. Repository truth

- Private repo `Paolo83Ita/hermes-voice-console-android` exists (API-verified: private=true,
  token has admin+push). Working branch `foundation/v0.1-cloud-build` pushed and read-back
  verified (remote head = local HEAD). `main` does not exist yet: the repo default-branch
  setting currently resolves to the working branch; the Owner creates/merges `main` after
  device qualification (G-006, INV-07).

## 4. Explicitly NOT true yet

- No device installation or device read-back yet → QUALIFICATION_PENDING_DEVICE
  (Owner runbook: `docs/INSTALL_XIAOMI.md`; checklist §7 Q-40..Q-48).
- Tower integration is mock-only (MockTowerBridge + DeterministicTowerSimulator) —
  TOWER_REAL_INTEGRATION never claimed.
- Microphone capture, real STT, real TTS, wake word: intentionally INACTIVE in V0.1
  (Inactive* stubs; manifest permissions declared but not requested at runtime).

---

# GRAPH 2 TRUTH (appended 2026-11-01; supersedes §3 for main/branch facts)

## 5. Graph 2 baseline (physical read-back)

- Graph 1 authority accepted after physical verification: private repo, commit e7ab101,
  run 41297120173 SUCCESS, authoritative APK hash f7a76735… re-verified locally.
- `main` established at e7ab101 (refs API, new ref); local main ff'd; read-back: 3 branches
  all e7ab101. Default-branch switch to main blocked by PAT scope (403) — G2-002.
- Working branch: graph-2/model-manager-tower-connection-e2e (pushed, e7ab101).

## 6. Graph 2 explicit NOT-true-yet (will be updated per node)

- Voice Model Manager, Tower Connection Studio, Self-Test Center: PENDING (this graph).
- Real Tower connectivity: still never claimed; mock + configurable real adapter only.
- Emulator E2E: PENDING. V0.2 APK: PENDING. Physical Xiaomi: QUALIFICATION_PENDING_DEVICE.

---

# GRAPH 2 TERMINAL TRUTH (appended 2026-11-02 run; supersedes §6 above)

- Terminal state: **GRAPH_2_CONDITIONALLY_COMPLETE_WITH_EMULATOR_ENVIRONMENT_LIMITATION**
  (receipt: `docs/receipts/GRAPH_2_TERMINAL_RECEIPT.md`).
- Branch head: a9657f0 (CP-G2-021) + CP-G2-022 (repairs + this convergence).
- Build authority: run 41341134805 @ a9657f0 — unit tests **89/89**, lint executed,
  APK 58,604,732 B sha256 46dcf2b8…d8dc7f. Earlier preserved authority: 9ea0c6c.
- Emulator E2E: EXECUTED for the first time (run 41341134806): boot ✓, explicit adb
  install ✓, am instrument ✓; 30 reported: 5✓ 18✗, 7 killed by process crash.
- Root causes found + repaired at CP-G2-022: (1) duplicate exact-text "HERMES" node
  (console header + always-composed drawer) → drawer now "HERMES TOWER"; (2)
  AndroidDeviceHealthMonitor context.display UnsupportedOperationException on
  application context → runCatching-null (truthful unavailable). E2E-green proof is
  carried by the next cloud E2E run (Graph 3 Wave 8 gate).
- Real Tower integration: STILL never claimed (mock + deterministic simulator only).
- Physical Xiaomi: QUALIFICATION_PENDING_DEVICE. Merge to main: OWNER_APPROVAL_PENDING.

---

# GRAPH 3 TRUTH (appended 2026-11-02; supersedes nothing above — additive)

- Branch: graph-3/monumental-oracle-tower-visual-refoundation (STARTING_SHA bc186db).
- Decision: MONUMENTAL_ORACLE_TOWER_VISUAL_REFOUNDATION — OWNER_AUTHORIZED_FOR_IMPLEMENTATION;
  V0.1 direction superseded for identity (evidence preserved).
- Design phase: DESIGN_GAP_ANALYSIS + 11-preview pack VERIFIED (nonblank 11/11, dimensions
  exact, 0 overflow, contrast 8/8 PASS, state-truthfulness + AMOLED review recorded),
  V0.1-vs-Graph3 comparison rendered. Preview pack: design/monumental/preview/.
- Waves 1–4 implemented (CP-G3-002, 6959c07): monumental token system, pure OracleCoreModel
  (13 unit tests), layered Oracle Core renderer (WebP L0/L1/L2/L7 + Canvas dynamics),
  monumental console (frieze/core/lower), chrome re-expression; all screens converge via
  preserved Chrome API. Pinned E2E strings preserved; single exact-text HERMES invariant kept.
- LOCAL verification: JDK17+SDK toolchain — assembleDebug OK, unit tests 102/102
  (89 G2 + 13 G3), secret scan 0 findings.
- Cloud verification on 6959c07: android-debug-build + android-e2e triggered
  (runs recorded in the git-era ledger at final receipt time).
- Explicitly NOT true yet: cloud build results for CP-G3-002 (pending), E2E green
  (repair frontier from CP-G2-022 pending verification), V0.3 cloud APK (pending),
  physical Xiaomi qualification (QUALIFICATION_PENDING_DEVICE), merge to main
  (OWNER_APPROVAL_PENDING).

---

# GRAPH 3 TERMINAL TRUTH (appended 2026-11-03; additive)

- Terminal state: **GRAPH_3_CONDITIONALLY_COMPLETE_WITH_EMULATOR_LIMITATION**
  (receipt: `docs/receipts/GRAPH_3_TERMINAL_RECEIPT.md`).
- Branch head: 85cdc0e (CP-G3-007) + CP-G3-008 (this convergence). Chain:
  bc186db → 84c3114 → 6959c07 → de4e54e → 21a57c6 → fcea6bc → 6a92942 → 85cdc0e.
- Visual identity: MONUMENTAL_ORACLE_TOWER (OWNER_AUTHORIZED) implemented — layered
  Oracle Core, command frieze, architectural chrome on every screen, messenger language,
  quality profiles, per-region AMOLED safeguard, hybrid WebP+Canvas asset pipeline.
  V0.1 minimal direction superseded (evidence preserved).
- Cloud authority: run 41375246590 @ 85cdc0e — unit tests 102/102, lint archived,
  APK 58,782,349 B sha256 0639ae6e…b155cbc3a (receipt: GRAPH_3_DEBUG_APK_RECEIPT.md).
- Emulator E2E: 5 full-content cycles; trajectory 5✓/18✗/7-crashed → 17✓/13✗. Genuine
  on-device root causes found AND fixed: duplicate exact-text HERMES node,
  context.display UOE process crash, Files.getFileStore unsupported on ART (downloads).
  Residual 13: emulator input/lifecycle environment flakes + 1 queue-reconnect question.
  NOT claimed green.
- Real Tower: STILL mock/simulator only (TOWER_REAL_INTEGRATION never claimed).
- Physical Xiaomi: QUALIFICATION_PENDING_DEVICE. Merge to main: OWNER_APPROVAL_PENDING.

---

# OWNER PHYSICAL FEEDBACK TRUTH (appended 2026-11-06; first PHYSICAL DEVICE evidence)

- FIRST physical-device evidence of the project: POCO X6 PRO (256 GB / 8 GB) real
  installation — `DEVICE_INSTALLATION_PASSED` + `APPLICATION_LAUNCH_PASSED` typed
  (receipt: `docs/receipts/OWNER_PHYSICAL_FEEDBACK_RECEIPT.md`).
- Tested package provenance (binary-inspected): commit `ca8980b` = Graph 3 terminal,
  versionName `0.2.0-debug` versionCode 2 — contains NO real TTS/voice pipeline
  (`MockTextToSpeechEngine` + `Inactive*` only). The tested APK predates ALL of
  Graph 4's real voice work → the Owner's Finding 3 (voice answers in English)
  CANNOT be attributed to the app's voice pipeline ON THAT PACKAGE; on any Graph 4
  build the code-proven TTS defect applies (see the root-cause report).
- Latest physical qualification state: **PARTIAL_OWNER_EVIDENCE** (Owner-typed).
- Findings 2 (submenu layout) and 3 (Italian TTS) drive the Owner priority override:
  Italian TTS → submenu layout → current APKs → install/upgrade package → SECOND
  Owner validation cycle (remaining Graph 4 nodes resume after that).
- Explicitly NOT true yet: Italian voice terminality on device
  (`ITALIAN_VOICE_ACTIVE` / `ITALIAN_VOICE_READY_AND_BLOCKED_BY_KNOWN_RUNTIME_LIMITATION`
  still gated on the next physical cycle's runtime read-back); owner screenshots for
  the layout ledger; second-cycle installation evidence.

---

# OWNER PHYSICAL RETEST FAILURE TRUTH (appended 2026-11-06; second physical cycle)

- The Owner re-tested on the physical POCO X6 Pro and REJECTED the checkpoint:
  state **OWNER_RETEST_FAILED_REPAIR_REQUIRED** /
  `CHECKPOINT_REJECTED_PENDING_REPAIR` (receipt:
  `docs/receipts/OWNER_PHYSICAL_RETEST_FAILURE_RECEIPT.md`).
- Physical failures recorded: PHYSICAL-FAIL-01 English spoken response;
  PHYSICAL-FAIL-02 `HIGH_PITCH_ENGLISH_TTS_OUTPUT` (no speaker identity/gender
  inferred anywhere); PHYSICAL-FAIL-03 Verify-button text layout in Models and
  Voice; PHYSICAL-FAIL-04 microphone-activation icon indistinguishable from the
  mute icon; PHYSICAL-FAIL-05 no material improvement perceived.
- Explicitly NOT true (prohibited tokens): `ITALIAN_TTS_OWNER_VERIFIED`,
  `RESPONSIVE_UI_QUALIFIED`, `POCO_RETEST_PASSED`. The CP-G4-026/027/028
  implementation checkpoint is NOT Owner-accepted.
- Installed-build provenance of the retested APK: UNPROVEN — the supplied
  build exposed no in-app build identity. The retest observations cannot be
  bound to an exact binary; the repair build closes this forward (build
  identity panel, build-injected values, on-device measured APK SHA-256).
- Pre-repair code truth (read-back at `53841a5`): the spoken path is
  FAIL-OPEN — `AndroidSystemTtsEngine.speak()` never blocks on the Italian
  negotiation verdict, and no `setSpeechRate`/`setPitch` exists in the entire
  pipeline; the in-app `APP_VERSION` constant was stale (`0.2.0-debug`)
  against BuildConfig `0.3.0-alpha-graph4` (drift removed this cycle).
- Repair cycle in flight (Owner priority override unchanged): Italian
  fail-closed TTS → speech profile → Verify button → control semantics →
  build identity → `POCO_RETEST_PACKAGE_2_READY`.

---

# GRAPH 4.9 TRUTH (appended 2026-11-07; CP-G4.9-001…010 session)

- Fresh read-back (docs/governance/GRAPH49_FRESH_READBACK.md): remote tip
  advanced 53841a5 → eb660d5 (20 commits: R-27…R-41 E2E chain, CP-G4-026…029);
  local fast-forwarded forward-only; tree clean; the CP-G4-029 repair and
  POCO_X6_PRO_OWNER_RETEST_PACKAGE_2 are AT THE TIP (CI run 42247787324 BUILD
  SUCCESS, 310/310 unit from the CI JUnit artifact, lint 0 errors).
- E2E regression FOUND at the tip and REPAIRED this cycle:
  `e2e_14_cancelActiveResponse_returnsToIdle` (expected IDLE, was SPEAKING)
  was a repair-introduced interrupt-swallow in `SystemTtsDomainAdapter` —
  interrupt() inside the honest ready-wait/speak-start windows was lost and
  the utterance was spoken anyway. Fix: interruption TOKEN checked in every
  wait loop and before the SPEAKING claim + 5 deterministic unit tests
  (e2e_19_20 was the cascade). The 3 crash classes (FreshInstall/ModelManager/
  SelfTest NavBackStackEntry INITIALIZED→DESTROYED) remain the LEDGERED
  pre-existing emulator class — NOT a repair regression (proven by the
  bf34014 lane artifacts).
- CP-G4.9-003 natural local TTS: DELIVERED — sherpa-onnx v1.12.24 AAR
  verified on JitPack (AAR sha256 ad052d51…6c7e, arm64-v8a + 3 more ABIs,
  minSdk 21 ≤ 30, Apache-2.0) and linked via Gradle; piper voices
  it-riccardo-x_low (sha256 8ad07915…7021) and it-paola-medium (7541f757…16b4)
  download-verified; `SherpaOnnxTtsEngine` (native+player seams, JVM-testable)
  + `TtsRouteSwitch` (system engine stays the MANDATORY fallback; route =
  registry ACTIVE + on-disk bundle) + Hear-sample action + ST-52 probe +
  catalog runtime entry resolved (unresolved:// → RUNTIME_BUNDLED with the
  AAR hash). Pre-device state:
  LOCAL_NATURAL_TTS_IMPLEMENTED_PHYSICAL_QUALIFICATION_PENDING.
- CP-G4.9-004 local STT: DELIVERED — vosk-android 0.3.75 was already linked;
  the user-operable TEST path added: real Italian fixture WAV (the verified
  mission phrase, synthesised with the SAME verified piper voice, sha256
  7b5483a2…f1a2f bundled with a provenance manifest) driven through
  `SttFixtureTestRunner` (SttEngine interface; load→accept→finalize→unload);
  Test action in the Sanctum; ST-53 route probe; Diagnostics active-route
  read-back already present. Pre-device state:
  LOCAL_STT_IMPLEMENTED_PHYSICAL_QUALIFICATION_PENDING.
- CP-G4.9-005 optional local LLM: ARTIFACT VERIFIED — Qwen2.5-0.5B-Instruct
  q5_k_m GGUF downloaded IN FULL at the pinned revision 9217f5db: SHA-256
  041474553fcabfc2a2d67903f9d2c2e50bd92528e670da4f33b5d0ce6e59fd55 (exact
  catalog match, 522,186,592 B). Runtime remains TYPED-BLOCKED
  (`NoVerifiedLlmRuntime`, LLM_RUNTIME_PENDING_VERIFIED_SOURCE): no verified
  llama.cpp Android AAR exists to link; the engine never invents completions.
  LOCAL_LLM_CANDIDATE_VERIFIED_RUNTIME_TYPED_BLOCKED.
- CP-G4.9-007 Tower Archive: product-complete (mock) — artifacts gained MIME
  type, provenance, deterministic SIMULATED text previews, and a typed
  transfer lifecycle (download/pause/resume/re-download/remove/verify/
  cancel/transferState) + UI actions + tests; report
  docs/receipts/TOWER_ARCHIVE_PRODUCT_COMPLETION_REPORT.md.
- CP-G4.9-008 Tower Gate matrix: docs/tower/
  ANDROID_GRAPH4_TOWER_GRAPH1_7_COMPATIBILITY_MATRIX.md (15 CLIENT_IMPLEMENTED,
  11 MOCK_PASSED, 5 GRAPH_1_7_SERVER_REQUIRED, 2 REAL_TEST_PENDING,
  2 BLOCKED — no invented routes).
- Version: 0.3.2-alpha-graph4.9-local-models / versionCode 5 (strictly greater
  than the Owner's in-flight 0.3.1/versionCode 4). Build label
  "GRAPH 4.9 LOCAL MODELS 1". Unit suite 335/335 GREEN, lint 0 errors.
- Owner gate UNCHANGED: `OWNER_POCO_RETEST_PENDING` on package 2
  (arm64, dbbdc429…b60cc). No physical claim. Prohibited tokens remain
  unclaimed.


---

## GRAPH 4.10 TRUTH (2026-11-07, supersedes the Graph 4.9 sections above semantically — history preserved)

Mission: physical-product repair for the Owner findings P0-01..P0-11
(POCO X6 Pro + Xiaomi 11 Lite 5G NE — `docs/receipts/OWNER_PHYSICAL_FINDINGS_GRAPH410.md`).
Fresh read-back at mission start: local == remote tip `c18e313` (reported tip verified current).

| Node | State |
| --- | --- |
| Response-language authority + localized catalog (§4/§5) | IMPLEMENTED — ResponseLocaleResolver + ResponseCatalog (43 cat × 5 locales, parity-tested) |
| Visible text == TTS input (§6) | IMPLEMENTED + unit-pinned (MockTextToSpeechEngine.lastSpokenText) |
| Voice selection/runtime truth (§7/§8) | IMPLEMENTED — VoiceSelectionAuthority (six-condition read-back) + VoiceRuntimeFactory (typed failures, files preserved, system-Italian fallback) |
| Chat speech policy + speaker control (§10/§11/§12) | IMPLEMENTED — ChatSpeechOutputPolicy (persistent) + §11 state machine + TtsReadbackStage grader (Owner-verified never self-granted) |
| Listening mode + continuous + echo (§14/§15/§16) | IMPLEMENTED — ListeningModePolicy + ContinuousListeningController + EchoFingerprintGuard (WAKE_WORD honestly UNQUALIFIED) |
| Console hierarchy / Xiaomi layout / raw enums (§17–§26) | IMPLEMENTED — §18 1-primary+4-secondary hierarchy, §19 width classes, §20 responsive headers (11 screens), §21 Connection Studio rebuild, §22 ResponsiveActionGroup, §23 StateLabels (P0-09 table verbatim) |
| Central Tower refoundation (§29–§35) | IMPLEMENTED — 10 layered WebP assets (ASSET_MANIFEST.json), ReforgedTowerVisual with 11 §34 states + §35 profiles, OracleCore retired from the Console |
| Regression (§37) | unit 412/412 GREEN, lint 0 errors (abortOnError) |
| APKs (§38) | versionName 0.3.3-alpha-graph4.10-owner-repair / versionCode 6 / label POCO XIAOMI OWNER REPAIR 3; arm64-v8a 116,561,845 B sha256 4ffefb80d4a51992… (preferred) / universal 246,208,674 B 97e6f8124d59bcbf… / x86_64 121,277,812 B 0312db163e39477e…; dex stamps verified (classes11+13) |
| Owner retest package (§39/§40) | POCO_XIAOMI_RETEST_PACKAGE_3_READY — `docs/receipts/OWNER_POCO_XIAOMI_RETEST_PACKAGE_3_GRAPH410.md` |

Forbidden tokens remain UNCLAIMED: POCO_RETEST_PASSED, ITALIAN_TTS_OWNER_VERIFIED,
VOICE_SWITCHING_OWNER_VERIFIED, RESPONSIVE_UI_QUALIFIED, REAL_TOWER_PAIRING_PASSED,
ITALIAN_VOICE_OWNER_VERIFIED (grader never self-grants), POCO_XIAOMI_…_RETEST_PASSED.
Current terminal state: POCO_XIAOMI_RETEST_PACKAGE_3_READY (physical verdict = Owner-only).

---

# GRAPH 4.11 TRUTH (appended 2026-11-07; additive — supersedes the Package-3 "next action" only)

- Directive: PHYSICAL EXPERIENCE COMPLETION — no new architecture, no new governance systems, no new abstractions, no Graph 5.
- Branch: graph-4/real-voice-pairing-archive-e2e; tip at execution start 5a567a3 (local was 32 commits behind → ff-only, nothing discarded).
- Nodes executed: 1 voice A/B qualification (ST-54 + AppGraph audition, authority untouched); 2 continuous listening 10-turn pins (loop + anti-echo); 3 no-English-under-Italian catalog pin; 4 Xiaomi responsive coverage VERIFIED (4.10 authorities stand); 5 central Tower MEASURED repairs (sign clip, arrow intrusions, static-band stage alignment — manifest graph-4.11-central-tower-refoundation-2, portal/energy byte-identical); 6 version 0.3.4-alpha-graph4.11 / code 7 / label POCO XIAOMI OWNER PACKAGE 4.
- Verification at the commit: 422/422 unit tests GREEN (was 412; +10 new), lint 0 errors, secret scan 0 findings, local assembleDebug SUCCESS (universal 248,446,578 B / arm64 118,799,749 B sha256 501792715d… / x86_64 123,515,716 B — validation artifacts; the CI build of the pushed commit is the authoritative package).
- Honest states: OWNER_VISUAL_RETEST_PENDING, VOICE_SWITCHING_OWNER_VERIFICATION_PENDING, physical passes Owner-only, REAL_TOWER_PENDING_GRAPH_1_7 (P2 next: Tower visual → Archive → Graph 1.7 pairing, per the Owner's own note).

---

# GRAPH 4.12 TRUTH (appended 2026-11-08; additive — never a history rewrite)

- Directive: PHYSICAL STABILITY, MODEL RUNTIME TRUTH, UNIFIED STOP CONTROL,
  ACCESSIBILITY CONTRAST, CENTRAL TOWER REPLACEMENT, OWNER RETEST PACKAGE.
  No Archive expansion, no LLM expansion, no Graph 1.7 work in this run.
- Branch: graph-4/real-voice-pairing-archive-e2e; execution start tip bb70165
  (fresh fetch-verified; the narrative c18e313 superseded).
- Owner physical verdict on POCO OWNER PACKAGE 4 recorded in
  OWNER_PHYSICAL_FINDINGS_GRAPH412.md: PASS = Italian local responses +
  continuous listening; FAIL = first-run crash (PHYS-FAIL-01), low contrast
  (02), Tower corrupted (03), Interrompi uncertainty (04), model-consumption
  uncertainty (05). State OWNER_PHYSICAL_RETEST_FAILED_REPAIR_REQUIRED.
- Crash ROOT CAUSE (evidence, not guesswork): E2E run 42382864925 both lanes
  E2E_VERDICT_FAILED with `IllegalStateException: State must be at least
  CREATED to move to DESTROYED, but was INITIALIZED in NavBackStackEntry` —
  navigation + Activity recreation racing (and on API 33+ a DOUBLE recreate:
  LocaleManager automatic + manual recreate()). Repairs: §5 atomic completion
  contract (one-shot navigation token, committed pending-route that survives
  recreation, RESUMED-gated navigation via lifecycle.withResumed), single
  sanctioned recreation scheduler (next-frame, finishing-guarded, no manual
  recreate on 33+), §3 secret-clean crash receipt system, §4
  FIRST_RUN_SAFE_MODE with typed per-subsystem gates.
- Model runtime truth (§7/§8/§9/§10/§11): CapabilityLadder (13 distinct
  states; INSTALLED→ACTIVE jump REFUSED) + ModelRuntimeReceipt (secret-clean,
  recorded per activation, report renderer) + ST-55 Vosk fixture truth +
  ST-56/ST-57 Riccardo/Paola consumption truth (system-TTS route can never be
  ACTIVE) + LOCAL_LLM_RUNTIME_UNAVAILABLE preserved.
- Unified interrupt (§13–§16): UnifiedInterruptController cancels every
  applicable subsystem and returns an InterruptReceipt; §15 state-specific
  labels (Interrompi ascolto / Annulla trascrizione / Annulla richiesta /
  Interrompi voce / Ferma ascolto continuo) + visible final-state read-back.
- Contrast (§18–§20): inkDim repaired 0xFF6E675C→0xFF8A8375 (3.17–3.63:1 →
  ≥4.72:1); redBronzeText 0xFFDE7A5E for small text; Marble Gold/Energy/Warn/Ok
  deepened to ≥4.5:1; all pinned by ContrastRatioTest;
  ACCESSIBILITY_COLOR_CONTRAST_REPORT.md is the measured record.
- Raw enums (§21) moved out of ordinary UI via StateLabels expansion (54 keys);
  Advanced Diagnostics keeps raw values.
- Central Tower (§26–§34): the ten rejected layers RETIRED to
  docs/evidence/quarantined-tower-assets-graph412/ (CENTRAL_TOWER_ASSET_RECONSTRUCTION_FAIL
  declared); NEW ORIGINAL candidate set (A/B/C) vector-drawn on a 1000×1375
  normalized stage; candidate A selected (Owner fidelity decision);
  automated §30 checks PASS (3 candidates, 0 defects); BALANCED decoded 20.9MB
  < 24MB budget, LOW 11.1MB < 12MB; ReforgedTowerVisual rewritten (12-layer
  contract, §31 animation, §32 profiles, §24 staged decode — first frame pays
  only the 6 static layers); §33 height contract (50%/24% usable area).
- Regression: unit 450/450 GREEN (+28 new), lint 0 errors, secret scan 0.
- APKs: versionName 0.3.5-alpha-graph4.12-physical-repair / versionCode 8 /
  label POCO XIAOMI PHYSICAL REPAIR 4; local validation arm64
  ec3a0217d687f80d… (universal 09f935dc…, x86_64 4409c3dd…); the pushed-commit
  CI build is the authoritative package (see
  docs/receipts/GRAPH412_LOCAL_APK_RECEIPT.md).
- Owner retest package: OWNER_POCO_XIAOMI_RETEST_PACKAGE_5_READY —
  docs/receipts/OWNER_POCO_XIAOMI_RETEST_PACKAGE_5_GRAPH412.md.
- Honest states: POCO_XIAOMI_PHYSICAL_REPAIR_PACKAGE_READY (pre-device);
  OWNER_VISUAL_RETEST_PENDING; VOICE_SWITCHING_OWNER_VERIFICATION_PENDING;
  physical passes Owner-only. Typed carry-forward unchanged:
  LOCAL_LLM_RUNTIME_UNAVAILABLE, REAL_TOWER_PENDING_GRAPH_1_7,
  REAL_ARCHIVE_PENDING_TOWER_API.
- Forbidden tokens remain UNCLAIMED (grader never self-grants):
  POCO_XIAOMI_…_RETEST_PASSED, ITALIAN_TTS_OWNER_VERIFIED,
  VOICE_SWITCHING_OWNER_VERIFIED, RESPONSIVE_UI_QUALIFIED, REAL_TOWER_PAIRING_PASSED.

---

# GRAPH 4.13 TRUTH (appended 2026-11-09; additive)

- Branch: graph-4/real-voice-pairing-archive-e2e. Fresh read-back at
  CP-G4.13-001: local was behind 42 → ff-only to 4b8bfb2 (remote tip,
  CP-G4.12-016 R-49). main untouched; no force-push; tree clean.
- Implementation boundary reached: **GRAPH413_OWNER_VISUAL_ACCESSIBILITY_PACKAGE_READY**
  (this commit chain: 19eb7e2 read-back+findings → 62767b3 audit+semantic
  authority → 9ab2fa3 screenshot+preservation gates → 7e0abc7 Model Library →
  bd0f5af before/after pack → d104f53 Tower refoundation → 9d4f3d0 version/APKs).
- Physical PASS preservation (preliminary, Owner-granted): Italian response,
  continuous listening, unified Interrompi, QR scanner open — preserved and
  regression-gated (PhysicalPassPreservationTest); no architecture added
  around them.
- Contrast: APP_WIDE_RENDERED_CONTRAST_AUDIT_GRAPH413.{json,md} — PRE-REPAIR
  evidence 82 rows / 11 FAIL (M3 default disabled labels 1.43–2.09:1;
  Marble-mode prose on the always-obsidian shell 1.16–3.12:1); LIVE semantic
  authority 50 rows / 0 FAIL in BOTH themes; 25-token §6 authority in
  HermesColors (two measured palettes, §7 anchors); §5 disabled contract via
  Hermes control wrappers; pinned by SemanticContrastAuthorityTest.
- Model Library: §13–§27 presentation rebuild over the UNCHANGED model
  authority (no second registry); sections A–G; vertical profile cards with
  honest Local-experimental state; typed compatibility verdicts; ONE filled
  primary action per card (§21); raw enums eliminated (StateLabels +16 keys
  ×5 locales, parity-tested); device CLASS label in ordinary UI (§25).
- Tower: CENTRAL_TOWER_GRAPH412_VISUAL_OWNER_REJECTED → retired from res;
  MONUMENTAL ORACLE CITADEL (candidate A of A/B/C, §36 13-layer contract,
  ONE 1000×1375 stage) integrated with LOW (≈9.8MB)/BALANCED (≈21.4MB)
  asset tiers, §37 defect gates ALL PASS, §39 state truth preserved,
  §24 staged decode retained, §38 keyboard collapse preserved.
- Tests: unit **472/472** GREEN (450 baseline + 4 semantic-contrast pins +
  7 physical-pass gates + 11 model-UX pins, minus… net as measured);
  lint **0 errors / 82 warnings** (below the 84 baseline; every warning
  classified; 0 newly introduced); secret scan 0 (tree+history);
  compileDebugAndroidTestKotlin GREEN (screenshot matrix + visual gates).
- Version: **0.3.6-alpha-graph4.13-visual-accessibility / versionCode 9**,
  label OWNER VISUAL ACCESSIBILITY PACKAGE 6; three validation APKs hashed
  (arm64 3b79b7c6…bcaa, universal da8a0ee1…1662, x86_64 a2e3cea8…d21c).
- Explicitly NOT true yet: E2E green (the R-42..R-49 harness-anchor line
  continues; crash family stays eliminated); physical visual retest —
  **OWNER_VISUAL_RETEST_PENDING**; the terminal physical state
  OWNER_VISUAL_ACCESSIBILITY_AND_MODEL_LIBRARY_RETEST_PASSED is
  OWNER-ONLY and is NOT granted.
