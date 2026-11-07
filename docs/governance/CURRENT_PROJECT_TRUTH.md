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
