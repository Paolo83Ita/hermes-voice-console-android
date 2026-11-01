# WORK LOG — Graph 1 (graph era; Foundation-era log preserved at docs/WORK_LOG.md)

Format per node: NODE / FINDING / ACTION / TEST / FILES / COMMIT OR CHECKPOINT / NEXT.

---

NODE N0 — workspace reconciliation
FINDING: All major Foundation claims physically verified (19 docs, 50 Kotlin = 44+6, 7
Mermaid blocks, 3 mockup sets, ZIP present). Deltas: "26 test cases" is actually 33 @Test;
Gradle wrapper jar/scripts missing; 92 pending git changes were pure file-mode flips
(0 content delta); no git remote configured.
ACTION: normalized modes (git restore, no content change); typed every verdict in
CURRENT_PROJECT_TRUTH.md §1.
TEST: physical file enumeration, grep counts, sha256 of ZIP (8f5de42e…93ca80).
FILES: docs/governance/CURRENT_PROJECT_TRUTH.md
COMMIT: graph-1/governance
NEXT: governance install.

NODE N1 — governance install
FINDING: Foundation-era DECISION_LOG.md and WORK_LOG.md already exist under docs/ — authority
must not be duplicated.
ACTION: governance/ files extend them: Foundation logs stay in place; graph-era records live
here with explicit cross-references.
TEST: CHECKER read-back of all 10 files.
FILES: docs/governance/{PROJECT_OPERATING_CONTRACT,MISSION_GRAPH.yaml,CURRENT_PROJECT_TRUTH,
REQUIREMENT_TO_EVIDENCE_REGISTER,INVARIANT_REGISTRY,DECISION_LOG,BLOCKER_REGISTER,
CHECKPOINT_INDEX,WORK_LOG,REHYDRATION_PACK}.md
COMMIT: graph-1/governance
NEXT: repo convergence.

NODE N2 — git foundation
FINDING: working tree clean after mode normalization; branch main at ffe6749.
ACTION: created working branch `foundation/v0.1-cloud-build`; ran scripts/secret_scan.py over
working tree AND full git history.
TEST: 0 findings in tree, 0 in history (INV-03 verified before any push).
FILES: scripts/secret_scan.py (kept outside the repo, in the sandbox scripts dir)
COMMIT: (branch creation, no content change)
NEXT: build readiness.

NODE N4 — build readiness (before first push, per G-001)
FINDING (attempt 1, local build): 44 compile errors across 11 main-source files. Root causes:
missing frame imports in domain/TowerBridge.kt (20 cascades), nested Session import,
BatteryManager property API misuse, missing Compose/lifecycle imports, automirrored Menu
misuse, Int/Float mismatches, interface-vs-impl signature mismatch.
ACTION: minimal-diff repairs (G-003); regenerated canonical Gradle 8.9 wrapper (gradlew,
gradlew.bat, gradle-wrapper.jar committed); installed local Android SDK 35 + Temurin JDK 17
(G-002) after discovering sandbox Java 21 lacks jlink.
TEST: :app:compileDebugKotlin — 0 errors after repair.
FINDING (attempt 2, tests): 9 test-source compile errors (stale route() call signature,
UnconfinedTestDispatcher class not nameable in type positions in coroutines-test 1.9.0,
Job+Job error-level deprecation, suspend misuse).
ACTION: adapted tests to the production API; TestDispatcher parameter types; suspend helper.
TEST: tests compiled; 33 ran, 7 failed.
FINDING (attempt 3, semantics): state machine lacked documented-but-unenforced edges
(universal self-transition rule; IDLE→CONTACTING_TOWER queue-flush edge); LogRedactor lost
separator whitespace and misclassified mixed-case base64 as hex; MockTextToSpeechEngine
hard-coded real dispatcher broke virtual-time determinism.
ACTION: machine rule 4 + IDLE→CONTACTING_TOWER added to CODE AND DOC together (G-003);
redactor regexes fixed (G-004); TTS dispatcher seam (G-005); JWT-after-Bearer coverage.
TEST: final local run `:app:assembleDebug :app:testDebugUnitTest` → BUILD SUCCESSFUL,
33/33 pass; local APK sha256 163d700a…357557 (validation artifact only).
FILES: 15 source/doc files + 3 wrapper files (full list in the graph-1/repair commit).
COMMIT: graph-1/repair
NEXT: cloud build.

NODE N5 — cloud build
FINDING (iteration 1/3): run 41296487511 — CLOUD_BUILD_FAILED at the "Set up Android SDK"
step: android-actions/setup-android@v3's internal license acceptance ran `sdkmanager
--licenses` without stdin; the runner's cmdline-tools 16.0 prompts interactively, hits EOF,
exit 1. Not a project defect.
ACTION (material change, iteration 2/3): dropped the action; used the preinstalled runner
SDK with `yes`-piped license acceptance + pinned package install; kept JDK 17, wrapper,
gates, artifacts (G-007 amended).
TEST: run 41296592392 — CLOUD_BUILD_PASSED. CI tests 33/33 green; lintDebug executed
(non-blocking); three artifacts uploaded, names bound to the commit SHA.
FILES: .github/workflows/android-debug-build.yml (commit 505047e9)
COMMIT: graph-1/ci (8038bfd) + graph-1/ci-fix (505047e9)
NEXT: APK receipt.

NODE N6 — APK receipt
FINDING: artifact hermes-voice-console-debug-apk-505047e9… (id 13080210671) = app-debug.apk,
57,848,596 bytes, sha256 f7a767354e241961928f1b536ec71c23e005920f55b7e7a791b29cc126cd0be7.
Local-vs-cloud hash difference attributed to ephemeral per-runner debug keys + build
metadata (expected; both APKs identical in size and build inputs).
ACTION: receipt written; APK copied to the local deliverables folder as
hermes-voice-console-debug-v0.1-505047e.apk.
TEST: SHA-256 recomputed from the downloaded artifact bytes.
FILES: docs/governance/APK_RECEIPT_V0.1.md, docs/governance/CURRENT_PROJECT_TRUTH.md
COMMIT: graph-1/receipt
NEXT: Xiaomi readiness.

NODE N7 — Xiaomi qualification readiness
FINDING: Foundation checklist Q-01..Q-04 expectations superseded by Owner-confirmed device
identity (11 Lite 5G NE · 2109119DG · HyperOS 2.0.4.0 · Android 14); no V0.1 install section
existed.
ACTION: added docs/INSTALL_XIAOMI.md (Owner runbook, hash verification, no-root install,
11-step first-launch walk, read-back template); extended DEVICE_QUALIFICATION_CHECKLIST.md
with §7 Q-40..Q-48 and refreshed superseded expectations.
TEST: CHECKER read-back of both documents; every APK claim matches APK_RECEIPT_V0.1.md.
FILES: docs/INSTALL_XIAOMI.md, docs/DEVICE_QUALIFICATION_CHECKLIST.md
COMMIT: graph-1/qualification
NEXT: final report.

NODE N8 — final truth + packaging
FINDING: all Graph 1 nodes COMPLETE; no active blocker; device qualification remains
QUALIFICATION_PENDING_DEVICE by design (Owner-controlled).
ACTION: refreshed ZIP (source + docs + mockups + workflows; no build outputs, no
git metadata, no secrets); final scan re-run; report delivered in chat.
TEST: zip listing + secret scan 0 findings + git log verification.
FILES: download/hermes-voice-console-android-v0.1.1-graph1.zip (deliverables folder)
COMMIT: graph-1/final (SHA recorded at commit time; see git log)
NEXT: Owner — install per INSTALL_XIAOMI.md; report read-back; then Owner merges main.
