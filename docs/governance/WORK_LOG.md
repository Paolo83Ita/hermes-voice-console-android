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

---

# WORK LOG — GRAPH 2 (branch graph-2/model-manager-tower-connection-e2e)

NODE G2-N01 — Graph 1 remote read-back
FINDING: repo private=true; branches: foundation/v0.1-cloud-build e7ab101 only; commit
e7ab101 verified; run 41297120173 android-debug-build completed SUCCESS on e7ab101; ledger
1F/2S confirmed (8038bfd failure, 505047e success, e7ab101 success); authoritative APK
(run 41296592392 @ 505047e) hash re-verified locally: f7a76735…cd0be7, 57,848,596 bytes —
exact match. No PRs. main did NOT exist.
ACTION: accepted Graph 1 authority as physically verified.
COMMIT: (read-back only)
NEXT: main authority.

NODE G2-N02 — main authority + Graph 2 branch
FINDING: PAT lacks Administration scope → default-branch PATCH 403 (G2-002, non-blocking).
ACTION: created remote main at e7ab101 (refs API); local main ff to e7ab101; created and
pushed graph-2/model-manager-tower-connection-e2e; fresh read-back: 3 branches all e7ab101.
COMMIT: (branch ops only) — CP-G2-001
NEXT: governance update.

NODE G2-N17 — instrumentation executed + typed terminality (2026-11-02 run)
FINDING: CP-G2-021 POSIX repair worked — run 41341134806 booted the emulator (~21 min),
explicit adb install of both APKs succeeded, am instrument ran for the first time:
30 tests reported (5 passed / 18 failed), then the app process CRASHED at
e2e_14_cancelActiveResponse (UnsupportedOperationException: context.display on the
application context inside AndroidDeviceHealthMonitor.snapshot via the local
conversation pipeline), killing 7 more tests. Every console-assertion failure shows the
same primary defect: two exact-text "HERMES" nodes (console header + always-composed
drawer sheet) break onNodeWithText("HERMES") (expects exactly 1).
ACTION: two evidence-driven root-cause repairs (CP-G2-022): drawer header → single
distinct "HERMES TOWER"; DeviceHealthMonitor display read → runCatching-null truthful
degradation. Graph 2 closed at
GRAPH_2_CONDITIONALLY_COMPLETE_WITH_EMULATOR_ENVIRONMENT_LIMITATION; E2E-green proof
handed to the next cloud run (Graph 3 Wave 8); merge to main remains Owner-blocked.
TEST: run 41341134805 (89/89 unit, lint, APK 46dcf2b8…); run 41341134806 artifacts
(results 13098643226, failure evidence 13098564795); remote read-back.
FILES: docs/receipts/GRAPH_2_TERMINAL_RECEIPT.md; AppNav.kt; AndroidDeviceHealthMonitor.kt
COMMIT: CP-G2-022 (recorded in git log at commit time)
NEXT: Graph 3 branch creation after checkpoint + remote read-back.

# WORK LOG — GRAPH 3 (branch graph-3/monumental-oracle-tower-visual-refoundation)

NODE G3-N01 — graph2 boundary + branch creation (2026-11-02)
FINDING: Graph 2 closed at GRAPH_2_CONDITIONALLY_COMPLETE_WITH_EMULATOR_ENVIRONMENT_LIMITATION
(CP-G2-022, bc186db); remote read-back: graph-2=bc186db, main=e7ab101 untouched.
ACTION: created graph-3/monumental-oracle-tower-visual-refoundation from bc186db, pushed,
read-back verified. STARTING_SHA=bc186db54ec017ed20306801e71cf5577bc62edd.
COMMIT: (branch ops) — CP-G3-001 follows.
NEXT: design gap analysis + preview pack before production UI change.

NODE G3-N02..N04 — design phase + waves 1-4 (2026-11-02)
FINDING: preview pack verified before production change (mission gate). The local
toolchain (JDK17 + SDK 35, restored after sandbox recycle) caught 5 real compile errors
before the cloud run; local build green with 102/102 unit tests.
ACTION: CP-G3-002 (6959c07) — waves 1-4 + design docs + preview pack + governance.
Waves 5/6 converge via preserved Chrome API (all Graph 2 screens restyled, zero logic change).
TEST: local assembleDebug + 102/102 unit tests + kt_sanity PASS + secret scan 0.
COMMIT: CP-G3-002 = 6959c07 (read-back verified)
NEXT: cloud evidence, remaining doc convergence, packaging, terminal receipt.

NODE G3-N07 — cloud build evidence + APK + first E2E attempt (2026-11-02)
FINDING: android-debug-build run 41347497836 @ 6959c07 SUCCESS — cloud unit tests
102/102, lint archived, APK 58,781,684 B sha256 4b69416d…c31572 (receipt:
docs/receipts/GRAPH_3_DEBUG_APK_RECEIPT.md; local artifact cross-checked).
android-e2e push run 41347497809 = ENVIRONMENT FLAKE: emulator booted (flag=1, 858 s)
then its system services died (cmd → settings/input: Broken pipe; runner exit 224)
BEFORE any install or instrumentation; no results artifact, empty failure evidence.
Not a code failure — classified per mission §19 as emulator-environment failure.
ACTION: one bounded workflow_dispatch retry (run 41349811483, same commit, mission §19
allows bounded retries); documentation convergence committed (VISUAL_DIRECTION
supersession header, UI_STATE_MACHINE Graph 3 mapping, PRODUCT_VISION/REQUIREMENTS/
ARCHITECTURE/RISK_REGISTER/DEVELOPMENT_ROADMAP/SECURITY addenda, CURRENT_PROJECT_TRUTH,
REHYDRATION_PACK delta, APK receipt).
TEST: cloud artifacts verified by download + hash; secret scan of source package 0 findings.
COMMIT: CP-G3-003 (recorded at commit time)
NEXT: retry outcome → Graph 3 terminal receipt + terminality calculation.

NODE G3-N08 — E2E retry ledger + cleartext repair (2026-11-02)
FINDING: dispatch retry run 41349811483 completed the FULL suite with NO process crash —
30/30 reported: 14 passed / 16 failed. Both CP-G2-022 repairs VERIFIED on-device
(fresh-install ✓, cancel-response ✓ (former crash point), recreate ✓, restart-persist ✓,
clean-exit ✓, all 4 KeystoreVault tests ✓; zero duplicate-HERMES assertions).
The 16 failures typed into 3 classes: (a) ×9 download-state timeouts
"Timed out waiting for <artifact> in [DOWNLOADED]; last=FAILED"; (b) ×6 "Failed to
inject touch input" (emulator input-dispatch flakes); (c) ×3 "Activity never becomes
DESTROYED" timeouts + ×1 queue-validation follow-on.
ROOT CAUSE (a): API 30+ default network security policy blocks cleartext HTTP — the
in-process local mock server is http://127.0.0.1:<port> → downloads end FAILED.
REPAIR (CP-G3-004): res/xml/network_security_config.xml — base-config cleartext=false,
cleartext permitted ONLY for 127.0.0.1/localhost (real Tower transport stays HTTPS/WSS);
manifest wired. Local build green.
CLASSES (b)/(c): emulator-environment flakes (software rendering input/lifecycle
slowness) — no assertion weakened; the next run's ledger decides.
TEST: run 41349811483 artifact 13103201619 (full ledger archived); local build 102/102.
COMMIT: CP-G3-004 (recorded at commit time)
NEXT: cloud cycle on CP-G3-004 → Graph 3 terminal receipt.

NODE G3-N09 — cleartext repair verdict + evidence-pipeline repair (2026-11-02)
FINDING: CP-G3-004 cycle — build run SUCCESS; E2E run 41356511688 identical ledger
(30 reported, 14✓/16✗, downloads still "last=FAILED"). Cleartext policy alone did not
fix the download class → hypothesis NOT confirmed; no blind code change (mission §18:
evidence-driven, ≤3 substantive repairs per class, attempt 1 spent).
ROOT CAUSE OF EMPTY EVIDENCE: android-emulator-runner tears down the emulator in its
post-run phase BEFORE the separate failure-evidence step executes — every failure so
far archived zero-byte evidence.
ACTION (repair attempt 2 for the class — evidence instrumentation, no assertion change):
logcat tail (6000 lines) + final screenshot captured INSIDE the script step, uploaded
with the results artifact; then the next cycle yields the real downloader error.
TEST: YAML valid; local build chain unaffected.
COMMIT: CP-G3-005 (recorded at commit time)
NEXT: evidence-driven repair attempt 3 for the download class (final within budget).

NODE G3-N10 — download-class diagnostic repair (2026-11-02/03)
FINDING: CP-G3-005 evidence fix worked (results artifact 324 KB with real logcat +
final screenshot, run 41362428842) — but the 6000-line tail window missed the Model
Manager timeframe (tests run mid-suite; only the last KeystoreVault minutes were
captured). Ledger unchanged: 30 reported, 14✓/16✗ — the download class still ends
FAILED with its reason trapped inside the download flow.
ACTION (repair attempt 2, diagnostics): awaitState now surfaces the downloader's own
error text + byte counters in the assertion ("error=…; bytes=x/y") — decisive evidence
on the next cycle, no assertion weakened. Local build green (app + androidTest + unit).
COMMIT: CP-G3-006
NEXT: cloud cycle → read the surfaced error → attempt 3 (final for this class) if needed.

NODE G3-N11 — download-class ROOT CAUSE + fix (2026-11-03)
FINDING: the surfaced diagnostic delivered the reason exactly as designed —
"Timed out waiting for <id> in [DOWNLOADED]; last=FAILED; bytes=0/0; error=getFileStore".
Root cause: DefaultModelStorageManager.preflight() called java.nio.Files.getFileStore(),
which is NOT supported on Android/ART — every download failed instantly (0 bytes, no
network I/O) while desktop-JVM unit tests passed. A genuine device-only defect the E2E
suite existed to catch; cleartext policy (CP-G3-004) stays as correct hardening but was
not the cause.
ACTION (repair attempt 3, evidence-driven — final for this class): preflight now reads
root.toFile().usableSpace (java.io, supported on JVM + Android API 9+), falls back to
FileStore, degrades truthfully to "unavailable" (-1 → insufficient, honest message)
rather than crashing. Unit tests 102/102 green, assembleDebug green.
TEST: run 41368013409 artifact (stack with error=getFileStore); local build.
COMMIT: CP-G3-007
NEXT: cloud cycle → expected: 9 download-class tests flip to pass; remaining
touch-injection/destroy-timeout classes are emulator-environment flakes.

NODE G3-N12 — final cycle + terminality (2026-11-03)
FINDING: cycle 5 (85cdc0e / run 41375246654) — E2E 17✓/13✗; all 9 download-class tests
PASS (24_25_26 pause/resume/hash, 27_28_29 install/activate/read-back, 30 rollback).
Cumulative E2E trajectory across the graph: +12 tests vs the Graph 2 baseline, zero
regressions, two real on-device defects fixed beyond the two inherited ones.
ACTION: typed terminality GRAPH_3_CONDITIONALLY_COMPLETE_WITH_EMULATOR_LIMITATION;
GRAPH_3_TERMINAL_RECEIPT.md written; truth/checkpoints/mission-graph converged;
final source package + APK copies staged in the deliverables folder.
TEST: run 41375246654 artifacts (build SUCCESS, 102/102, APK 0639ae6e…); E2E ledger §3.
COMMIT: CP-G3-008 (recorded at commit time)
NEXT (Owner): review receipts → approve merge to main → physical Xiaomi qualification
phase (INSTALL_XIAOMI.md) as the next dependency-released graph; E2E frontier (nav
flakes + e2e_19_20) carried with full evidence.

NODE G4-N01 — Graph 4 bootstrap + lint truth repair (2026-11-02)
FINDING: fresh read-back confirmed Graph 3 terminal state (graph-3 tip ca8980b, APK
79864d54… byte-verified, 102/102, 2 lint errors ProduceStateDoesNotAssignValue +
36 warnings). Branch graph-4/real-voice-pairing-archive-e2e created, pushed, read
back; 225-file mode-only sandbox artifact classified and restored (no work lost).
ACTION: CP-G4-001 bootstrap + CURRENT_PROJECT_TRUTH_GRAPH4_PRE.md. CP-G4-002
NODE 0: both lint errors root-caused (state assignment inside nested
withFrameNanos callback invisible to Compose lint) → dedicated lifecycle-safe
AnimationFrameClock (LaunchedEffect + mutableLongStateOf, hoisted tick,
cancellation on disposal); ancillary §3 repairs (dataExtractionRules, monochrome
icon, getAllocatableBytes preflight with max(2GB,15%) reserve, SDK_INT cleanup,
dependency pinning doc, blocking lint gate). 106/106 green.
TEST: lintDebug 0 errors / 31 warnings (each warning individually classified).
COMMIT: 33e7bb6, 9b4e17a (both remote-read-back verified)

NODE G4-N02 — verified production model catalog (2026-11-02)
FINDING: every production placeholder eliminated; 12-entry catalog with real
sources, real SHA-256s and honest classifications (silero VAD 5.1.2, vosk
small-it-0.22, piper IT riccardo-x-low + paola-medium via k2-fsa tts-models,
gtcrn denoiser, openWakeWord deps + hey-jarvis fixture, Qwen2.5-0.5B
RUNTIME-gated, custom IT wake word = CANDIDATE_PENDING_DISCOVERY, probe =
TEST_FIXTURE_ONLY, Gemma REJECTED).
ACTION: CP-G4-003 — catalog fields (entryClassification, upstreamRepo,
revisionBinding, licenseUrl, provenanceNote, recommended, optionalComponent),
CatalogIntegrity audit + resolver classification gate + 10 tests incl.
shipped-manifest audit. Re-verified gtcrn asset live on 2026-11-02 session
resume: HTTP 200, 535,638 bytes, sha256 e77603ac… EXACT match (a 9-byte local
"Not Found" stub was a transient download artifact, not a catalog defect).
TEST: 116/116 green; lint 0 errors.
COMMIT: 525c71f (remote-read-back verified = local HEAD at session start)

NODE G4-N03 — MODEL SANCTUM: Model Delivery Center refound (2026-11-02)
FINDING: Graph 2 screen downloaded only the mock-served probe; engine had
pause/resume/hash/atomic-install but no real-source wiring, no §7 roots, no
receipts, no bulk pack.
ACTION (CP-G4-004): §7 storage roots (catalog/tmp/staging/active/previous/
quarantine/logs/receipts) + quarantine-on-hash-mismatch with facts sidecar +
previous-version evidence copies (COPY semantics — Graph 2 rollback contract and
its tests untouched); DownloadGatekeeper (offline/severe-thermal BLOCK,
moderate/metered/power-saving WARN); secret-free MODEL_INSTALLATION_RECEIPT +
pack receipts (atomic writes, structured pipeline steps); VoicePackInstaller
(plan/summarize/installPack/installComponent — honest wake-word gap, bundled
runtime naming, NO optional components, sequential receipted pipeline with
registry read-back before activation); MODEL SANCTUM screen refound (§6 all
attributes + fallback contracts, real-source Download/Resume/Re-download/
Repair/Update wiring, expandable attribute sheets, diagnostics panel, bulk
INSTALL RECOMMENDED VOICE PACK with full preflight summary + explicit
confirmation dialog).
TEST: 126/126 green (10 new: planning honesty incl. shipped-catalog pack plan,
summary math + labeled duration, gatekeeper matrix, receipted happy path with
registry read-back, truthful stop + quarantine on hash mismatch, receipt secret
policy, previous-version preservation, quarantine sidecar, already-installed
classification, fallback contract); lint 0 errors / 31 warnings (unchanged);
local assembleDebug green — APK 58,950,857 B sha256 bda3b0df…; secret scan 0.
Toolchain note: sandbox SDK/JDK re-provisioned from scratch (cmdline-tools,
platform 35, build-tools 35.0.0, Temurin JDK 17 with jlink via
org.gradle.java.home in the OUT-OF-REPO ~/.gradle/gradle.properties; Aliyun/
Tencent mirror injection via out-of-repo init script to survive Maven Central
429 rate limiting — committed build files and CI untouched).
COMMIT: CP-G4-004 (recorded at commit time)
NEXT: cloud build evidence at CP-G4-004 → NODE 3 remaining recovery cases
(process-kill/restart resume paths) → NODE 4 local LLM adapter → NODE 5 guided
pairing (TOWER GATE).
