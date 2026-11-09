# REHYDRATION PACK — Hermes Voice Console Android

Purpose: any future executor (or the Owner) can resume exactly here. Contains NO secrets —
the GitHub token is Owner-supplied per session and is never stored in this repository or pack.

## Identity

- Project: Hermes Voice Console Android — dedicated voice/visual console for Hermes Tower.
- Private repo: `github.com/Paolo83Ita/hermes-voice-console-android` (visibility private).
- Default branch `main` (Owner merges only — INV-07). Working branches (Graph 3 era):
  `graph-3/monumental-oracle-tower-visual-refoundation` (active) ←
  `graph-2/model-manager-tower-connection-e2e` (terminal, preserved) ←
  `foundation/v0.1-cloud-build` (Graph 1, preserved).
- Graph 2 scope (preserved): Voice Model Manager, Tower Connection Studio, Self-Test
  Center, migration, 89 unit tests, 45-item E2E suite, workflows.
- Graph 3 scope (active): Monumental Oracle Tower visual refoundation — decision
  MONUMENTAL_ORACLE_TOWER_VISUAL_REFOUNDATION; V0.1 direction superseded for identity.
- V0.1 scope: 8-screen Compose console, 15-state enforced machine, MockTowerBridge +
  DeterministicTowerSimulator + ScriptedConsoleDriver, privacy mute, reduced motion,
  burn-in mitigation, mock TTS, inactive voice engines (intentional).

## Where everything is

- Governance authority: `docs/governance/` (10 files; start at PROJECT_OPERATING_CONTRACT.md).
- Foundation docs: `docs/*.md` (19 files; REQUIREMENTS, ARCHITECTURE, UI_STATE_MACHINE,
  TOWER_BRIDGE_PROPOSED_CONTRACT, …) — preserved, do not duplicate.
- App source: `app/src/main/java/com/hermestower/voiceconsole/**` (44 files),
  tests `app/src/test/**` (6 classes, 33 tests).
- Design mockups: `design/mockups/` (Marble Light, Obsidian Night, comparison).
- Build: `settings.gradle.kts`, `build.gradle.kts`, `app/build.gradle.kts`,
  `gradle/libs.versions.toml`, committed Gradle 8.9 wrapper.

## How to build (verified 2026-11-01)

Requirements: JDK 17 (Temurin), Android SDK with `platforms;android-35` +
`build-tools;35.0.0` + platform-tools.

```bash
./gradlew :app:assembleDebug :app:testDebugUnitTest   # gates used by CI
./gradlew :app:lintDebug                              # non-blocking report
# APK: app/build/outputs/apk/debug/app-debug.apk
```

CI: GitHub Actions workflow `android-debug-build` (`.github/workflows/android-debug-build.yml`),
runs on every push to the working branch and via workflow_dispatch; uploads the APK, test
results and lint report as artifacts named with the commit SHA.

## State at last update (2026-11-01, post-N8)

- Local build green: 33/33 unit tests pass; local APK hashed (validation only).
- Cloud build green: run 41296592392, commit 505047e9 — CI 33/33; APK artifact + SHA-256 in
  `docs/governance/APK_RECEIPT_V0.1.md` (APK_CREATED, APK_HASHED, APK_BOUND_TO_COMMIT).
- Secret scan: 0 findings (tree + history).
- Open: physical Xiaomi qualification (Owner-run; docs/INSTALL_XIAOMI.md),
  real Tower integration (V0.4), real voice front-end (V0.2+), Owner merge to main.

## Next exact actions (if resuming)

1. Read `docs/governance/CURRENT_PROJECT_TRUTH.md` and `BLOCKER_REGISTER.md`.
2. If Owner returned device read-back: update truth + registers, type DEVICE_READBACK_VERIFIED.
3. If new code work: create the next graph/branch per Owner instruction; never touch main.

## Authentication protocol (no secrets stored here)

The Owner supplies a fine-grained GitHub token at session start, authorized ONLY for
`Paolo83Ita/hermes-voice-console-android`. Store it transiently outside the repo, use via a
credential helper or bearer header, never in remote URLs, never in files, history, logs or
this pack. Repo auth budget: 2 attempts; push budget: 2 attempts.


---

# Graph 3 rehydration delta (2026-11-02)

## Where everything is (Graph 3 additions)

- Graph 3 design authority: `docs/graph3/` (DESIGN_GAP_ANALYSIS, VISUAL_SYSTEM,
  ORACLE_CORE_STATE_SPEC, MOBILE_COMPOSITION_SPEC, ASSET_PIPELINE_DECISION,
  PERFORMANCE_AND_THERMAL_BUDGET, AMOLED_AND_BURN_IN_SAFETY, ASSET_MANIFEST.json).
- Verified preview pack + V0.1 comparison: `design/monumental/preview/` (11 renders +
  verification_report.json + HTML/font sources; regenerator:
  scripts/monumental_lib.py + gen_monumental_previews.py + verify_graph3_previews.py).
- Monumental implementation: `ui/monumental/` (7 files), Chrome re-expression in
  `ui/components/Chrome.kt`, console in `ui/screens/ConsoleScreen.kt`, shell in
  `ui/AppNav.kt`, theme in `ui/theme/Theme.kt`, settings keys `visual_quality`.
- Production layer assets: `res/drawable-nodpi/monumental_l{0,1,2,7}_*.webp`
  (generator: scripts/gen_monumental_assets.py).
- New tests: `ui/monumental/OracleCoreModelTest` (13 tests).

## How to build (verified 2026-11-02)

Same toolchain as Graph 1, re-verified in a fresh sandbox:
- Temurin JDK 17 at /home/z/tools/jdk-17.0.21+9 (Java 21 JRE alone lacks jlink —
  JdkImageTransform fails), ANDROID_HOME=/home/z/android-sdk with
  platforms;android-35 + build-tools;35.0.0 + platform-tools.
- `./gradlew --no-daemon :app:assembleDebug :app:testDebugUnitTest` → BUILD SUCCESSFUL,
  102/102 tests.

## Active frontier

1. Cloud evidence for CP-G3-002 (build+tests+lint+APK) — runs triggered.
2. E2E suite on the emulator: verify CP-G2-022 repairs (drawer HERMES dedup,
   DeviceHealthMonitor display guard); drive to green without weakening assertions.
3. Packaging: cloud APK + SHA-256 + source package + terminal receipt.
4. Owner decisions reserved: merge to main; physical Xiaomi qualification phase.

## GRAPH 4.11 rehydration pointer (2026-11-07)
- Read first: docs/governance/GRAPH411_FRESH_READBACK.md (authority at 5a567a3 + node mapping).
- Tower truth: docs/receipts/CENTRAL_TOWER_VISUAL_REFOUNDATION_REPORT.md (Graph 4.11 update section) + docs/visual/ASSET_MANIFEST.json (manifestVersion graph-4.11-central-tower-refoundation-2) + scripts/tower_refoundation_g411.py (reproducible measure/build/preview).
- Package 4: docs/receipts/OWNER_POCO_PACKAGE_4_GRAPH411.md (runbook; version 7 / 0.3.4-alpha-graph4.11).
- Standing pending tokens: OWNER_VISUAL_RETEST_PENDING, VOICE_SWITCHING_OWNER_VERIFICATION_PENDING, REAL_TOWER_PENDING_GRAPH_1_7.

## GRAPH 4.13 rehydration pointer (2026-11-09)
- Working branch (authoritative, fresh remote tip always wins): `graph-4/real-voice-pairing-archive-e2e` — CP-G4.13-015 tip `afbe48d`; ff-only re-attach, never trust an old SHA.
- Read first: docs/governance/GRAPH413_FRESH_READBACK.md → CURRENT_PROJECT_TRUTH.md "GRAPH 4.13 TRUTH" block → WORK_LOG from CP-G4.13-001 → CHECKPOINT_INDEX G4.13 rows.
- Implementation boundary REACHED: **GRAPH413_OWNER_VISUAL_ACCESSIBILITY_PACKAGE_READY** — Block 1 semantic color authority (25 tokens, live audit 50 rows / 0 FAIL both themes), Block 2 Model Library rebuild (§13–§27, presentation-only over the UNCHANGED model authority), Block 3 tower refoundation (candidate A "Monumental Oracle Citadel", 13-layer §36 contract, docs/visual/tower413/, defect gates ALL PASS, 4.12 assets retired).
- Version: 0.3.6-alpha-graph4.13-visual-accessibility / versionCode 9 / label OWNER VISUAL ACCESSIBILITY PACKAGE 6; Owner runbook = OWNER_POCO_XIAOMI_RETEST_PACKAGE_6_GRAPH413.md (POCO 15 + Xiaomi 10 items); tower package = OWNER_TOWER_VISUAL_PACKAGE_GRAPH413.md.
- Tests truth at the tip: unit 472/472 GREEN (independently re-parsed from CI), lint 0 errors / 82 classified warnings (below the 84 baseline), secret scan 0.
- Standing e2e line: both lanes E2E_VERDICT_FAILED typed — gate signatures IDENTICAL to the pre-4.13 baseline 4b8bfb2 (R-42..R-49 harness-anchor family; residual 7/2 byte-identical; zero Process-crashed → crash family stays eliminated) → NO 4.13-attributed e2e regression (CP-G4.13-015 arbitration). The e2e arbitrates autonomously at every new tip; record its verdict in the next read-back, never as an Owner-gate claim.
- ONLY open gate: **OWNER_VISUAL_RETEST_PENDING** — the Owner physically runs Package 6 on POCO X6 Pro (2311DRK48G) + Xiaomi 11 Lite 5G NE. OWNER_VISUAL_ACCESSIBILITY_AND_MODEL_LIBRARY_RETEST_PASSED is Owner-only and is NEVER self-granted.
- Next session resume action: fetch; ff-only to the remote tip; read CURRENT_PROJECT_TRUTH; if the Owner granted the physical PASS, converge governance to the terminal state; otherwise report pending.

## GRAPH 4.14 CONTINUATION rehydration pointer (2026-11-09)
- Working branch (authoritative, fresh remote tip always wins): `graph-4/real-voice-pairing-archive-e2e` — CP-G4.14-C08 tip = THIS deployment commit; ff-only re-attach, never trust an old SHA.
- Read first: docs/governance/GRAPH414_CONTINUATION_READBACK.md (C01) → docs/governance/GRAPH414_FRESH_READBACK.md (mission origin) → CURRENT_PROJECT_TRUTH.md "GRAPH 4.14 ADDITIVE BLOCK" → CHECKPOINT_INDEX G4.14 rows → WORK_LOG from CP-G4.14-001.
- Implementation boundary REACHED: **GRAPH414_RUNTIME_THEME_TOWER_PACKAGE_READY** — runtime completion (llama.cpp Android + Qwen GGUF + Silero ORT seam; matrix verified NOT recreated), bridge truth authority (SIMULAZIONE amber always), Marble Light app-wide (42-row contrast, 0 FAIL), Model Library product truth, reference central Tower (7 layers ×2 tiers, §38 gates on shipped pixels after the calibrated residue repair, 12-state animation evidence), self-test battery repaired + extended to 61 definitions (ST-55..57 registration bug fixed; ST-58..61 added).
- Version: **0.3.7-alpha-graph4.14-runtime-theme-tower / versionCode 10 / label OWNER RUNTIME THEME TOWER PACKAGE 7**; three validation APKs (arm64 1c3697a5…c593 · universal 216e95a6…d784 · x86_64 ce628573…693e); size growth vs Package 6 = EXPECTED_RUNTIME_GROWTH (llama.cpp).
- Tests truth: unit 496/496 GREEN (68 suites); lint 0 errors / 72 classified warnings (7 newly-introduced repaired in C01; 0 suppressed); secret scan 0.
- Standing e2e line: the R-42..R-49 typed harness family CONTINUES to arbitrate autonomously at every new tip — record its verdict in the next read-back; never falsify a green E2E; never restart the historical campaign without new evidence.
- OPEN gates (Owner-only): **OWNER_VISUAL_RETEST_PENDING** (Package 7 runbook: OWNER_POCO_XIAOMI_RETEST_PACKAGE_7_GRAPH414.md — POCO 17 + Xiaomi 15 items) and **LLM_PHYSICAL_QUALIFICATION_PENDING** (legs B/C/D/F/K/L of GRAPH414_LLM_HONESTY_GATE.md = runbook items 10/15). NOT granted and NEVER self-grantable: OWNER_RUNTIME_THEME_TOWER_RETEST_PASSED, OWNER_VISUAL_APPROVED, LOCAL_LLM_QUALIFIED, REAL_TOWER_PAIRED, REAL_ARCHIVE_ACTIVE.
- Key artifacts: GRAPH414_FINAL_RUNTIME_AUDIT.md · GRAPH414_LLM_HONESTY_GATE.md · GRAPH414_BRIDGE_TRUTH_FINAL_CHECK.md · GRAPH414_MARBLE_ROUTE_MATRIX.md · GRAPH414_MODEL_LIBRARY_FINAL_REPORT.md · docs/visual/tower414/ (ASSET_MANIFEST.json re-pinned, TOWER_FINAL_ASSET_AUDIT.{md,json}, GRAPH414_TOWER_LAYER_CONTACT_SHEET.png, animation/*, renders/*) · GRAPH414_OWNER_REFERENCE_COMPARISON.md · docs/receipts/GRAPH414_{APK_SIZE_AND_RUNTIME_REPORT,LOCAL_BUILD_RECEIPT}.md · docs/lint/LINT_WARNING_CLASSIFICATION.md (G4.14 addendum) · OWNER_TOWER_VISUAL_PACKAGE_GRAPH414_FINAL.md.
- Next session resume action: fetch; ff-only to the remote tip; read CURRENT_PROJECT_TRUTH + this pointer; if the Owner recorded Package 7 results, converge governance to the Owner verdict; otherwise arbitrate the CI/E2E wave at the tip and report pending.
