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
