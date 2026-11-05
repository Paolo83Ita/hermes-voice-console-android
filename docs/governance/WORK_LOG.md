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

NODE G4-N04 — cloud evidence at CP-G4-004 + CP-G4-005 voice pipeline foundation (2026-11-02)
FINDING: android-debug-build run 41459906150 @ 213182a (CP-G4-004) SUCCESS — cloud
JUnit 126/126 (0 failures/errors/ignored), APK 58,950,857 B (size identical to the
local build; sha256 7ed066e0… — signing timestamps differ from local, expected),
lint report artifact archived. android-e2e @ 213182a and both workflows @ c2ebd88
(CP-G4-005) in progress at session close — evidence to be read back next session;
no terminality claimed on unfinished runs.
ACTION (CP-G4-005, NODE 3 §8–§10): typed voice seam layer — AudioFrameSource
(physically stoppable, INV-G4-10), VadEngine, SttEngine (§9 typed states, partial/
final transcript, confidence, typed SttError), TtsEngine (§10 states + fallback
tiers); VoicePipelineController (push-to-talk turn: capture→VAD→in-flight
STT→deterministic router→LOCAL/TOWER execution→TTS→typed history record;
privacy mute physically stops capture and refuses turns; mid-turn mute verified to
stop frames within one frame; barge-in/cancel typed; no-speech 3–10 s truthful;
frames consumed in-flight, never retained — INV-G4-09; "Handled locally"/"Sent to
Hermes Tower" attribution — no silent routing); EnergyVadEngine (real RMS VAD,
Silero remains primary pending runtime); AndroidSystemTtsEngine (REAL system TTS,
§10 tier 2); MicPermissionFlow (typed consent states + rationale strings).
TEST: 136/136 green (10 new voice tests); lint 0 errors / 31 warnings unchanged;
secret scan 0; commit c2ebd88 pushed + read back (exact).
COMMIT: c2ebd88 (CP-G4-005)
NEXT: android-e2e read-back for 213182a/c2ebd88 → Android AudioRecord frame source
+ console UI wiring (visible indicator, mute control) → vosk/silero/piper runtime
adapters → NODE 4 local LLM adapter → NODE 5 TOWER GATE pairing.

NODE G4-N05 — CP-G4-005R pending-E2E read-back (2026-11-02)
FINDING: remote graph-4 tip = afe3e08 = local HEAD (exact ls-remote match, clean
tree, nothing to recreate). Cloud android-debug-build SUCCESS at all three
reported commits; afe3e08 artifacts re-verified from download: 136/136 cloud
JUnit (16 XML files, 0 failures/errors/skipped), lint 0 errors / 32 warnings
(+1 warning vs CP-G4-004 — classification pending, gate is zero errors), APK
59,000,000-B class sha256 f2417045…c512ae. E2E: c2ebd88 run 41460745698 FAILED
at adb install — five attempts, all "cmd: Failure calling service package:
Broken pipe (32)", streamed-install last line, ZERO instrumentation tests
executed, boot polling normal (KVM PRESENT on this runner — Graph 2 no-KVM
limitation no longer applies) → classified E2E_BLOCKED_EMULATOR_ENVIRONMENT
(infrastructure, not source, not test). 213182a (41459906172) and afe3e08
(41461749711) E2E runs physically active at read-back → E2E_RESULT_UNAVAILABLE,
conservative polling only, no workflow change needed (fresh-runner retries).
ACTION: receipt docs/receipts/CP-G4-005-READBACK-RECEIPT.md + this convergence.
TEST: read-back only — no source changed in this step.
COMMIT: CP-G4-005R (recorded at commit time)
NEXT: G4-C02 Android AudioRecord frame source + mic lifecycle (16 kHz mono PCM16,
PUSH_TO_TALK default, typed init/busy/permission/read failures, full test
matrix) → G4-C03 Main Console voice wiring + first-class text chat (Owner
binding delta) → runtime adapters → monumental refinement → compact navigation
→ five-language localization → pairing/archive/self-test/packaging.

NODE G4-N06/07/08 — G4-C02/C03/C07/C08 wave (2026-11-02)
FINDING: sandbox recycled (Android SDK gone, Maven 429) — restored SDK+JDK17
into the PERSISTENT area (/home/z/my-project/android-sdk, jdk17) and rebuilt the
out-of-repo Aliyun-mirror init script; commits 090040d (G4-C02), adbc16c (G4-C03)
pushed and read back exact. Two E2E runs (213182a/afe3e08) were still active at
CP-G4-005R — re-read-back pending at next cloud evidence step.
ACTION: G4-C02 REAL AudioRecord source (AndroidMicHandleFactory VOICE_RECOGNITION
16k mono PCM16 with getMinBufferSize negotiation; bounded drop-oldest queue;
physically immediate stop with one-shot release + queue clear; typed
PERMISSION/BUSY/INIT/FORMAT/READ failures; idempotent double start/stop) wired
into VoicePipelineController (MIC_* typed errors never shown as NO_SPEECH).
G4-C03 ConsoleVoiceCoordinator: LISTENING only while the system recognizer
physically captures; system STT (EXTRA_PREFER_OFFLINE, partial → live draft,
final → driver.submitUtterance = SAME routing/attribution/history as text per
Owner delta §1/§13); real Android system TTS with UtteranceProgressListener
(SPEAKING ends when audio ends — delta §5); first-class chat composer (draft
persisted to KeyValueStore across restarts §15, send clears, counter, stop);
PTT/repeat-last/unified-stop medallions; foreground-only capture.
G4-C07 compact navigation: DrawerWidthPolicy pure (min(max(60%,248), min(2/3,360)))
+ rebuilt drawer HEADER/PRIMARY/SECONDARY/FOOTER, 48dp targets, two-line labels.
G4-C08 wave 1: 75-key five-language resources (Owner §14 anchors), AppLanguage
(LocaleManager API33+ official; attachBaseContext fallback 30–32), Settings→
Language persistent + reversible, LocalizationParityTest (GATE-05).
TEST: 178/178 unit green (41 new this wave); lint 0 errors (34 warn + 1 info —
warning classification doc update pending next pass); secret scan 0; local APK
59,927,205 B sha 931bc31c…
COMMIT: (this commit)
NEXT: E2E re-read-back for 213182a/afe3e08 + cloud build of this commit →
G4-C04 runtime adapters (vosk/piper delivery-dependent) → Tower Gate pairing
client + QR scanner (Owner delta) → Tower Archive → G4-C11/C12 packaging.

NODE G4-N09 — CP-G4-012a pairing client wave 1 + cloud evidence mid-cycle (2026-11-02)
FINDING: E2E 213182a (41459906172) ran the full suite this time (install OK —
the CP-G4-005 broken-pipe did not recur): 13 failures classified E2E_FAILED_TEST
with the Graph 3 known flake classes (touch-injection, wait-timeouts) plus the
known queue-reconnect question; receipt addendum written. Same commit unit-tested
126/126 green on cloud → flake-vs-regression separation needs the second/third
samples (afe3e08 run + adbc16c/749e746 runs). Cloud: adbc16c debug-build SUCCESS
(41470976809); 749e746 build+E2E in progress at node close (evidence read-back
next session, no terminality claimed on unfinished runs).
ACTION: G4-C09 wave 1 — PairingPayloadParser (strict versioned scheme, typed
rejections covering the whole Owner delta §11 matrix, replay nonce store,
redaction-safe describe) + MockTowerPairingClient (deterministic, zero network;
real pairing deferred to Tower Graph 1.7 — NO claim anywhere).
TEST: 192/192 unit green (14 new). Secret scan 0.
COMMIT: f7bde56 (CP-G4-012a)
NEXT (exact action sequence for the next session): (1) read back cloud runs
41472352299/41472352520 (749e746) + 41461749711 (afe3e08 E2E) and classify;
(2) G4-C09 wave 2 — CameraX+ML Kit in-app QR scanner (verify dependency
versions/licenses first) + scanner screen with camera permission flow and
§10 lifecycle rules; (3) Tower Archive (Conversations/Missions/Artifacts);
(4) G4-C04 runtime adapters when catalog artifacts are delivered on device;
(5) G4-C11/C12 self-test expansion, packaging, Xiaomi runbook.

NODE G4-N10 — second E2E sample: deterministic residual confirmed, no new regression (2026-11-02)
FINDING: afe3e08 E2E (41461749711) = identical 13-failure set as 213182a, first
failure byte-identical; zero AppNav/Appearance changes since the Graph 3 green
run; count matches the Graph 3 terminal residual ledger exactly (13 = emulator
input/lifecycle flakes + queue-reconnect question). CONCLUSION: E2E_FAILED_TEST
= KNOWN Graph 3 residual, deterministic on this emulator environment; NOT a new
Graph 4 regression. Recovery = typed E2E-ROBUSTNESS repair node (selector
hardening + scroll-before-click + drawer-awareness), criterion <13 on two fresh
runners. In-flight: adbc16c E2E, 749e746 build+E2E — read-back next session.
ACTION: receipt §8 second-sample confirmation + this node.
TEST: read-back only.
COMMIT: (this commit)
NEXT: E2E-ROBUSTNESS repair node → camera QR scanner (G4-C09 wave 2) →
Tower Archive → cloud evidence read-back for adbc16c/749e746/f7bde56/aa02d2a.

NODE G4-N11 — CP-G4-007b/012a: repairs, sandbox-rollback recovery, status-bar fix (2026-11-03)
FINDING: (1) E2E run 41485512432 @ e10c490 changed the failure mode of all 12
navigation tests from ComposeTimeoutException to IllegalStateException
("Functions that involve synchronization … cannot be run from the main
thread") — the CP-G4-007 repair's semantics query inside
mainClock.advanceTimeUntil executes on the MAIN thread where Espresso-linked
sync is forbidden. (2) e2e_19_20 STILL failed with id-scoped predicates → the
flush does not deliver on the emulator despite the JVM virtual-time repro
passing; attempt-3 must be evidence-driven (the test now dumps the driver
event log + connection/state/queue/turns on failure). (3) SANDBOX ROLLBACK:
the local filesystem restored itself to an afe3e08-era snapshot mid-session;
remote tip 09963ce re-verified via ls-remote and the local repo hard-reset to
it (fast-forward restore — no history rewrite, no force-push); SDK+JDK17
rebuilt in the persistent area; gradle mirror init recreated. (4) OWNER FIELD
REPORT: the app content drew UNDER the system status bar (clock/signal) —
enableEdgeToEdge with zero insets handling anywhere in the UI.
ACTION: aeb69a3 — (a) AppNav root windowInsetsPadding(WindowInsets.safeDrawing):
full-bleed obsidian background preserved, content clears status bar, cutout,
nav bar and IME (keyboard-open chat-focused layout per §18 included);
(b) openDrawerAndNavigate advanced the frozen clock via bounded
advanceTimeBy(100ms) loop with the anchor query on the test thread BETWEEN
steps, explicit named-anchor error on timeout; (c) e2e_19_20 failure now
carries driver-event diagnostics (attempt-2 evidence step; no logic bypass).
TEST: 221/221 unit green; lint 0 errors / 34 warnings; androidTest compiles.
COMMIT: aeb69a3 (pushed, read back exact)
NEXT: cloud E2E verdict for aeb69a3 → update E2E_FAILURE_LEDGER verdicts;
19_20 root cause from the diagnostics dump (attempt 3 of 3 for RC-3);
then CP-G4-009 real model delivery + QR scanner wave 2; governance
convergence at each checkpoint.

NODE G4-N12 — CP-G4-007 terminal evidence: 14→8→0 assertion failures + typed residual (2026-11-03)
FINDING: three more instrumented E2E cycles produced decisive evidence: (a) run 41527072357
FLUSH diagnostics exposed the REAL queue root cause — the offline-queue branch entered
REQUEST_QUEUED while TRANSCRIBING (illegal edge, silently ignored since Graph 1) so the state
stayed pipeline-active and every reconnect flush skipped (turns id=3/id=6 stuck QUEUED);
(b) run 41539016998 confirmed the state-matrix repair: e2e_19_20 + SelfTest trio + e2e_23 all
GREEN (13→8 failures); (c) run 41551630218 (semantics-first drawer activation) reached ZERO
assertion failures — the suite aborted at test #14 on the frozen-clock residual: navigate()
provably fires (NavBackStackEntry created) but the entering content never composes under the
frozen animation clock, and teardown then trips the navigation-compose 2.8.5
INITIALIZED→DESTROYED lifecycle race (process death — library-internal).
ACTION: ConsoleStateMachine +TRANSCRIBING→REQUEST_QUEUED / +IDLE→REQUEST_QUEUED
(pinned by QueueStateMachineTest ×4); ledger verdict recorded (EMULATOR_INPUT_INJECTION_
LIMITATION, 3 attempts consumed, recovery = Xiaomi / animation-free CI variant / navigation
upgrade node); owner field report fixed earlier in this node chain (WindowInsets.safeDrawing).
TEST: 225/225 unit green; lint 0 errors / 34 warnings; cloud debug-build SUCCESS at
3deb28c and 26e2e8d; secret scan 0.
COMMIT: 3deb28c → 26e2e8d → 7014d3e (all pushed, read back exact)
NEXT: CP-G4-009 real model delivery + §6 control audit → CP-G4-010 wave 2 CameraX QR
scanner → visual refinement → packaging; Owner decision requested on the residual recovery
path (physical Xiaomi / animation-free CI variant / navigation upgrade).

NODE G4-N13 — CP-G4-009 real model delivery + CP-G4-010 wave 2 camera QR scanner (2026-11-03)
FINDING: (1) CP-G4-009 — the four physically verified ONNX artifacts (openWakeWord trio +
GTCRN, hashes re-measured this session and equal to bundled_manifest.json AND
voice_pack_catalog.json: melspec ba2b0e0f…, embed 70d16429…, hey_jarvis 94a13cfe…,
gtcrn_simple e77603ac…) now ride INSIDE the APK (assets/models/bundled/) and are delivered
through the SAME hash-gated, receipted pipeline as network downloads: double gate (manifest
AND catalog must agree on identity/hash/version/size — conflicts are typed refusals, no
silent trust), quarantine facts sidecar on hash mismatch (evidence preserved, §7), atomic
install with previous-version preservation, registry read-back, qualification/activation
ALWAYS via ModelActivationController, sequential truthful stop, idempotent re-delivery.
Provenance literal 'app-bundle' (no network). Delivery ≠ enablement: wake OFF until the
FA/FR harness, denoiser bypassed until device qualification; gtcrnDenoiserIfDelivered()/
openWakeWordIfDelivered() construct ONLY on demand. paola.tar.bz2 (81 MB VITS) intentionally
NOT bundled — APK size guard, receipted network installer stays its delivery path.
(2) CP-G4-010 wave 2 (G4-C09 wave 2) — camera QR pairing scanner: dependencies verified
BEFORE adoption (Google Maven HTTP 200 on both POMs, measured this session) and pinned with
licenses (CameraX 1.4.2 Apache-2.0, mlkit barcode-scanning 17.3.0 Apache-2.0 bundled
on-device, no Play Services). QrPairingScanFlow (pure, deterministic): duplicate-frame
suppression 1.5s debounce, full typed rejection matrix inherited, Detected terminal until
reset (nonce consumed — rescanning honestly yields REPLAYED_NONCE), rejections non-terminal,
truthful framesAnalyzed counting EVERY surfaced frame, redaction-safe summaries.
QrScannerScreen: CAMERA permission only in-context with honest rationale + manual-pairing
fallback text, typed CAMERA_UNAVAILABLE (no silent fallback), bindToLifecycle (hardware
released when screen leaves STARTED), scanner+use cases released onDispose, imageProxy
always closed. Pairing executes on the deterministic MOCK client only, labeled SIMULATED —
Tower Graph 1.7 authority untouched. Route qr_scanner reached from Tower Studio Actions
(drawer untouched — no E2E selector churn). Manifest: CAMERA + uses-feature required=false.
Review fix before commit: '$report.stoppedAt' template bug corrected (would have printed the
whole report object + literal '.stoppedAt').
TEST: 242/242 unit green (CP-G4-009: +9 bundled-delivery; CP-G4-010w2: +8 QR flow);
lint 0 errors / 34 warnings unchanged; androidTest compiles; local verify with JDK17.
COMMIT: ed50b21 (CP-G4-009) → 5c7993b (CP-G4-010 wave 2) — both pushed, ls-remote read back
exact (5c7993b). Governance rows added to CHECKPOINT_INDEX.
NEXT: visual refinement (Monumental convergence pass) → G4-C11/C12 self-test expansion and
packaging (cloud build evidence at these commits) → Xiaomi qualification package; cloud
debug-build + E2E evidence read-back for ed50b21/5c7993b next session (E2E residual recovery
path still awaiting Owner decision: physical Xiaomi / animation-free CI variant / navigation
upgrade).

NODE G4-N14 — G4-C11 self-test expansion + cloud evidence read-back + infra resilience + G4-C10 visual convergence (2026-11-03)
FINDING: (1) Cloud read-back: ed50b21 debug-build 41615537732 SUCCESS; its android-e2e
41615537760 FAILED at e2e_35_36 with the DOCUMENTED CP-G4-007 typed residual
(EMULATOR_INPUT_INJECTION_LIMITATION: frozen-clock transition stall → navigation-compose
2.8.5 INITIALIZED→DESTROYED teardown race, process death; tests 1–13 executed with ZERO
assertion failures) — third deterministic sample, no new regression, recovery paths still
awaiting the Owner decision (physical Xiaomi / animation-free CI variant / navigation
upgrade). 5c7993b debug-build 41617910340 SUCCESS. 35380b7 (docs-only commit) debug-build
41618048112 FAILED: Maven Central 429/5xx transient outage on the runner could not resolve
onnxruntime-android 1.20.0 + auto-value-annotations 1.6.3 (CameraX transitive) while the
ADJACENT 5c7993b run resolved the same artifacts minutes earlier — infrastructure failure,
NOT a test verdict (mission rule §CI).
ACTION: (1) G4-C11 SELF-TEST EXPANSION ST-46..ST-50 (bundled double-gate agreement, bundled
delivery §6 end-to-end ×2 idempotent, QR strict-matrix probe, archive truth §15/§16/§17,
Graph 4 route registry) — the Self-Test Center is the on-device qualification instrument:
the new Graph 4 subsystems now self-qualify on the Xiaomi with no host tooling; meta-tests
updated truthfully (JVM = 50 definitions, typed skips {ST-46,ST-47,ST-49} for asset-backed
subjects). Found+fixed during the wave: QR_SCANNER missing from AppRoutes.all. (2) INFRA
resilience: Aliyun central/google mirrors appended AFTER the primary authorities in
settings.gradle.kts (fall-through only when primaries cannot serve; byte-identical,
checksum-verified; same remedy as the sandbox init script) — VERIFIED ON THE FIELD:
e9abee9 debug-build 41621063944 SUCCESS with the same dependency tree that failed at
35380b7. (3) G4-C10 MONUMENTAL VISUAL CONVERGENCE: the MeanderBand signature motif
(VISUAL_DIRECTION §1) was Console-only; all 12 secondary screens now carry it under the
title — strictly additive pure Canvas, no semantics/text/selectors, E2E strings untouched
by construction.
TEST: 242/242 unit green; lint 0 errors / 43 warnings (classification doc updated: 39
accepted-pinned); androidTest compiles; secret scan 0.
COMMIT: 9543007 (G4-C11) → e9abee9 (infra) → 32e35e0 (G4-C10) — all pushed, ls-remote read
back exact at each step; governance rows in CHECKPOINT_INDEX.
NEXT: E2E read-back for e9abee9/32e35e0 + packaging (APK receipt at the converged tip,
5-language packaging strings if any) → Xiaomi runbook refresh (INSTALL_XIAOMI.md) → Owner
decision on the E2E residual recovery path. No terminal verdict: nodes remain.

NODE G4-N15 — E2E read-back e9abee9/32e35e0/b1df58a + verdict-channel infra repair + packaging (G4-C12) (2026-11-03)
FINDING: (1) android-debug-build SUCCESS at all three converged-tip commits (e9abee9 →
41621063944, 32e35e0 → 41621713767, b1df58a → 41621955833). (2) android-e2e at each
(41621063936 / 41621713747 / 41621955951) aborted with the SAME typed CP-G4-007 residual —
signature identical (INITIALIZED→DESTROYED NavBackStackEntry route=appearance, process
crash, suite abort) — 4th/5th/6th deterministic samples; NEW FACT: position independence
(class ordering varies per run; E2EAppearancePersistenceTest ran first here, abort at #3 of
numtests=30 vs #14 in 41551630218; e2e_40/e2e_41_42 recreate/restart paths green in the
same class). (3) INFRASTRUCTURE FINDING: the emulator-runner executes the script: input
ONE LINE PER sh -c — the multi-line if/fi verdict block ALWAYS exited 2
("Syntax error: end of file unexpected (expecting fi)"), so the job-level E2E verdict was
corrupted by construction (even a green suite would fail the job); same execution class as
the historical dash/pipefail bug.
ACTION: (1) android-e2e.yml verdict collapsed to a single POSIX-safe line, identical
semantics, evidence recorded in a script comment — no test, assertion or app source
touched. (2) G4-C12 PACKAGING: GRAPH4_DEBUG_APK_RECEIPT.md — CI APK @ b1df58a downloaded
and measured: 165,029,486 B, sha256 9275aa222d75411587eb9058bca370e6a0f4dfdcc32c2e21815d8
e3f30ffdaa0; growth vs Graph 3 decomposed honestly (ORT ×4 ABI ≈73.7 MB, ML Kit barhopper
×4 ABI ≈20.8 MB, dex ≈65 MB, bundled ONNX ≈4.3 MB); all 4 bundled artifacts re-hashed
MATCH inside the CI-built APK against the shipped manifest; app_name invariant across the
5 locales (brand anchor §14, no packaging-string work pending); ABI-splits recorded as an
Owner-optional refinement, runbook unchanged and valid. (3) E2E ledger addendum with the
three runs, the position-independence fact and the verdict-channel repair record.
TEST: read-back + workflow/packaging docs only — no source changed; no assertion touched.
COMMIT: (this commit) — pushed, ls-remote read-back exact.
NEXT: Owner decision gates remain: (a) E2E residual recovery path (physical Xiaomi /
authorized animation-free CI variant / navigation upgrade — now with a clean typed verdict
channel for any future run); (b) physical Xiaomi qualification via the b1df58a runbook;
(c) real Tower pairing (Tower Graph 1.7 authority). G4-C04 runtime adapters (vosk/piper/
silero STT·TTS·VAD, optional local LLM qwen) stay delivery-dependent per catalog contract.
No terminal verdict: nodes remain.

G4-N15 READ-BACK (2026-11-03) — cloud validation at 1fa7829: debug-build 41632784469
SUCCESS; android-e2e 41632784491 completed and the REPAIRED verdict channel printed the
truthful INSTRUMENTATION_FAILED_OR_INCOMPLETE marker as a single sh -c line (zero syntax
errors in execution); the typed residual fired again with identical signature (7th sample:
e2e_35_36, route=appearance INITIALIZED→DESTROYED, crash at #3 of 30, two green before).
Commit 1fa7829 pushed and read back exact (1fa7829b8b37decebc3acf6c92cb8bfa018de95a).

NODE G4-N16 — OWNER CONTINUATION DIRECTIVE intake + CP-G4-016 E2E decision work + CP-G4-017 wave 1 (2026-11-03)
DIRECTIVE: Owner Graph 4 continuation (simplified UX, complete localization, monumental
animated Tower, real clickable model installation, E2E recovery, Xiaomi readiness) —
10 checkpoints CP-G4-016..025, mission mode CONTINUE_FROM_LATEST_VERIFIED_REMOTE_TIP.
OWNER_DECISION_E2E_RECOVERY: CI_LIFECYCLE_PROFILE AUTHORIZED.
FINDING: (1) fresh read-back at 8013853 local=remote (OWNER_CONTINUATION_READBACK.md,
commit 1c8af71) — all five OWNER_DECISION_* intake-recorded; literal inventory MEASURED
(235 occurrences / 16 files). (2) E2E experiments: TEST-A control attempts blocked twice
by infrastructure (compile defect repaired from cloud evidence 41645556871/41645569087 —
getArguments static accessor + explicit ActivityScenarioRule type + missing import;
androidTest constructor inference defect repaired via library factory + DEBUG system
property 41646716053/41646727671; one install infra failure 41647987018 — Can't find
service: package, zero tests, E2E_BLOCKED_EMULATOR_ENVIRONMENT class). TEST-B executed
(41648000740 @ c6476a1): RESIDUAL REPRODUCED WITH THE PROFILE REQUESTED — identical
signature; forensics: 33 s frame-skip-free window (profile engaged by inference),
relaunch finishDrawing 25084 ms, Activity pause timeout 49 s + destroy timeout, destroy
stack = onActivityPreDestroyed → NavController propagates DESTROYED → INITIALIZED entry
throws (navigation-compose 2.8.5 non-defensive teardown meeting an un-promoted entry).
THE FROZEN CLOCK IS NOT THE SOLE MECHANISM. Engagement probe added (MainActivity debug
log + harness hard check) for proof on the controlled re-runs @ b0be658 (in flight).
ACTION: (1) CI_LIFECYCLE_PROFILE implemented: MotionProfile TEST_ONLY seam through the
app's own reduced-motion gate (release can never engage); harness variant-aware (variant
A byte-identical; variant B real frames, physical drawer clicks, accessible hub rows);
android-e2e.yml workflow_dispatch inputs (profile/class filter) with single-line POSIX
verdict discipline. (2) CP-G4-017 wave 1: drawer = ONLY the 5 primary destinations
(Console, Tower Archive, Connect to Hermes Tower, Models and Voice, Settings);
NEW SettingsHubScreen with the 6 prescribed categories reaching the EXISTING screens
(zero screen-logic moves); drawer closes on selection; 34 string keys ×5 locales
(parity 110×5); E2E walks the real product path via the hub; TEST-E onto the shared path;
LOCALIZATION_LITERAL_AUDIT.md delivered (235 literals classified per directive §3).
TEST: unit 242/242 at the wave-1 parent commit; compile/lint gates via cloud debug-build
(one Settings-icon import repaired from run 41651965634 evidence).
COMMIT: 1c8af71 (readback) → bd1606c (profile) → 6da1b58 → c6476a1 (propagation) →
39cc422 (nav wave 1) → 353fc22 (import fix) → b0be658 (probe) → 5319da3 (report) — all
pushed, ls-remote read back at each step.
CHECKPOINT: CP-G4-016 IN PROGRESS (controlled TEST-A/B re-runs with probe in flight;
TEST-C/D/E next; verdict + disposition in the next read-back).
NEXT: read controlled-run verdicts → disposition per directive §2 → TEST-C/D/E →
CP-G4-017 localization waves 2-3 + onboarding (§6) → CP-G4-018 Tower refoundation →
CP-G4-019..025. No terminal verdict: nodes remain.

G4-N16 CONTINUATION — controlled-run read-back: instrument-args TRANSPORT DEFECT + system collapse + route variance; channel repaired; matrix reset (2026-11-04)
FINDING: (1) Read-back of the two controlled re-runs dispatched @ b0be658 (TEST-A
NORMAL 41657209419, TEST-B LIFECYCLE 41657223032) and of the three push runs
b0be658/5319da3/543bc2d. NEW INFRA DEFECT (same per-line sh -c execution class as the
N15 verdict-channel bug): the emulator-runner executes the script ONE LINE PER sh -c, so
the workflow's INSTRUMENT_ARGS (built on line N, consumed on line N+3) was ALWAYS EMPTY
at `am instrument` — neither `-e class …#e2e_35_36…` nor `-e lifecycleProfile true` ever
reached the device in ANY dispatched CP-G4-016 run. Proof ×2: numtests=31 (full suite)
despite the single-test filter request in both dispatch runs; the engagement probe
logged `HermesMotionProfile: ciLifecycleProfile=false` in the LIFECYCLE run's logcat
artifact. (2) RECLASSIFICATION: the "TEST-B first sample" (41648000740 @ c6476a1) ran
with empty args → it was the 8th NORMAL-profile control sample, NOT a TEST-B execution;
§4b's frame-skip engagement inference is non-discriminative and withdrawn (probe read
FALSE). No CI_LIFECYCLE_PROFILE-engaged run exists yet. (3) Both b0be658 dispatch runs
died of a system collapse (com.android.phone death loop → cascading ANRs → system_server
FATAL "Lost network stack" → INSTRUMENTATION_ABORTED: System has crashed at position 1)
— E2E_BLOCKED_EMULATOR_ENVIRONMENT; the push run of the same commit survived to
position 7, so the collapse is environmental. (4) ROUTE VARIANCE of the residual: the
three push runs all show the identical teardown class at e2e_43_44 with
route=tower_studio (was e2e_35_36/route=appearance) — 9th/10th/11th samples; the
residual is position- AND route-independent. (5) HARNESS DEFECT surfaced by the same
evidence: the wave-1 openDrawerAndNavigate waited unconditionally for the destination
label as a DRAWER row, but Appearance/Connection/Self-Test Center are hub-only rows
since CP-G4-017 wave 1 — every hub-label navigation could only time out
(41657188171: e2e_35_36 ComposeTimeoutException 15 s at E2ECommon.kt:130); the hub
fallback was unreachable.
ACTION: (1) android-e2e.yml — instrument arguments built AND consumed on ONE POSIX-safe
line (IA=…; if…fi; if…fi; timeout 3600 adb shell am instrument -w -r $IA …), evidence
comment with the run ids; locally simulated for NORMAL/LIFECYCLE × filter/no-filter
(sh + dash). (2) E2ECommon.openDrawerAndNavigate — drawer-then-hub in BOTH variants:
activate the drawer row first (fails fast for hub labels), Settings-hub path on miss;
variant A hub-row wait moved to pumpUntil (virtual-time pumping composes destinations
on the frozen clock — CP-G4-007 physics); variant B unchanged semantics plus the hub
path actually reachable. (3) E2EFreshInstallTest e2e_03_04 label "Connection" → the
real product row "Current Tower & reconnect" (substring would land on "Connection
Studio (advanced)" = TOWER_STUDIO, whose screen lacks the test's anchor). (4) Root-
cause report §4c + matrix reset to PENDING on the repaired channel; §4b superseded in
part (banner); ledger addendum; CHECKPOINT_INDEX rows.
TEST: YAML structure validated (script = 14 non-comment lines; the one-liner is the
only cross-line stateful construct); one-liner simulation printed the expected args in
all four cases; Kotlin changes are harness-plumbing only (no assertion touched; no app
source changed); cloud compile gate via the dispatched runs themselves (no local SDK).
COMMIT: (this commit) — pushed, ls-remote read-back.
CHECKPOINT: CP-G4-016 IN PROGRESS — matrix reset; next = re-dispatch TEST-A/TEST-B on
the repaired channel (probe must read TRUE in B), then TEST-C/TEST-E/TEST-D; verdict +
disposition per directive §2 in the next read-back.
NEXT: TEST-A/B re-runs → C/E/D → disposition → CP-G4-017 localization waves 2-3 +
onboarding (§6) → CP-G4-018 Tower refoundation → CP-G4-019..025. No terminal verdict:
nodes remain.

G4-N17 — CP-G4-017 wave 2 execution (localization): 11 UI files migrated, 362-string parity ×5 locales (2026-11-04)
FINDING: (1) CP-G4-016 channel repair validated in structure; the dispatched TEST-A run
(41718222586 @ 3893e88) died BEFORE the script in the runner's own setup phase (emulator
boot 915,901 ms = 15.3 min, then `settings put system screen_off_timeout` Broken pipe (32)
→ runner abort exit 224, zero instrumentation) — E2E_BLOCKED_EMULATOR_ENVIRONMENT, third
environment-class failure of the day; the LIFECYCLE dispatch (41718228168) and the push
run (41718165578) were still in flight at session close (read-back next session).
(2) Wave-2 execution measured against LOCALIZATION_LITERAL_AUDIT.md: Appearance (24 keys),
Tower Archive (26), Connection (23), Console frieze rest (4), AppNav subtitles (13 + chip),
MonumentalChrome (7 + plurals), About & Safety (24 — limitation list updated truthfully to
five-language delivery), History (11), QR scanner (21 — SIMULATED tokens kept inside
localized frames), Voice & Audio (19), Self-Test Center (21 — E2E anchors preserved),
Diagnostics (27). Parity: 110 → 362 strings + 1 plurals, all five locales in lockstep.
(3) Cloud compile gate caught FOUR distinct defect classes in the mechanical migration,
each repaired from run evidence with a targeted fix: stringResource inside
semantics{} / onClick / coroutine lambdas (non-composable contexts — use hoisted vals or
context.getString: runs 41720542709, 41721015289); delegated-property smart-cast to
non-null Any (draft, cameraError — captured to local vals: runs 41721015289,
41721804513); missing stringResource imports (History/QrScanner/VoiceAudio/
LocalIntelligence — added).
ACTION: (1) LocalizationParityTest extended with plurals five-locale parity (one+other
mandatory) and positional-format-argument validation (bare %s/%d forbidden — %1$s style
enforced). (2) All 362 keys translated with quality review (it/es/fr/de); technical
allowlist respected (HERMES/TOWER/ORACLE CORE/MARBLE LIGHT/OBSIDIAN NIGHT/MODEL SANCTUM
invariant; TLS/diagnostic codes; LOCAL/TOWER source tokens; LOCAL/TOWER source tokens;
SIMULATED/QUALIFY_ON_DEVICE truthfulness tokens preserved in-frame). (3) New reusable
scanner scripts/extract_literals.py (UI-scope). (4) Remains for wave completion:
TowerStudioScreen (43), ModelManagerScreen (50), MockTowerArchive title frame (wave 3),
then the Italian completeness instrumentation test (directive §4) on the migrated UI.
TEST: parity hold at every batch (verified by key count ×5); compile gates via cloud
pushes (f757f3e/c2cb6b1 builds in flight at session close); no navigation, state,
control or persistence logic touched; E2E anchors preserved by construction (en defaults
byte-identical for every pinned string).
COMMIT: 1ac6f81 (batch 1) → 5f3fad9 (fix) → 84a2a64 (batch 2) → 34f92c2 (fix 2) →
f757f3e (batch 3) → c2cb6b1 (fix 3) — all pushed, ls-remote read back at each step.
CHECKPOINT: CP-G4-017 IN PROGRESS (wave 2 ≈ 75% by literal count; wave 3 pending; gate
test pending). CP-G4-016 IN PROGRESS (TEST-B/TEST-D pushes in flight; TEST-A re-dispatch
pending after the environment stabilizes).
NEXT: read 3893f88-era run verdicts → finish wave 2 (TowerStudio, ModelManager) + wave 3
(MockTowerArchive title) → Italian completeness gate test → CP-G4-018 Tower refoundation
→ CP-G4-019..025. No terminal verdict: nodes remain.

G4-N16 READ-BACK 2 — TEST-B decisive verdict: residual reproduces with the CI_LIFECYCLE_PROFILE ENGAGED → PHYSICAL_XIAOMI_RECOVERY_REQUIRED (2026-11-04)
FINDING: (1) The transport repair (3893e88) is cloud-VALIDATED: dispatch 41718228168
(LIFECYCLE) executed with numtests=1 — the single-test class filter AND
`-e lifecycleProfile true` reached the device for the first time in this node.
(2) Engagement PROVEN by the probe: `HermesMotionProfile: ciLifecycleProfile=true`
(21:48:02, logcat artifact); the harness variant-B hard-check passed (navigation
proceeded — anchor composed, no ComposeTimeoutException). (3) The residual REPRODUCED
with the profile engaged: `FATAL EXCEPTION: main — RuntimeException: Unable to destroy
activity … IllegalStateException: State must be at least CREATED to move to DESTROYED,
but was INITIALIZED … route=appearance` at 21:51:15 in
ActivityThread.performDestroyActivity. The frozen clock is EXCLUDED as the mechanism
(§2's analysis refuted as sole cause): with real frames and ornaments frozen in-app the
entering entry still never reaches CREATED on this software emulator — the un-promotion
is an environment property of the emulator's frame/lifecycle pipeline (§4b forensics:
finishDrawing of relaunch 25 s, pause timeouts, 15.3-min boots). (4) TEST-A dispatch
@ 3893e88 died BEFORE the script in the runner's own setup (boot 915,901 ms → Broken
pipe (32) on settings → runner abort exit 224, zero instrumentation) —
E2E_BLOCKED_EMULATOR_ENVIRONMENT. (5) DISPOSITION SET per directive §2 rule:
PHYSICAL_XIAOMI_RECOVERY_REQUIRED — the physical Xiaomi arbitrates the E2E residual and
the visual profiles; SOURCE_DEFECT_REPAIRED and TEST_LIFECYCLE_DEFECT_REPAIRED are both
excluded; no navigation-compose upgrade (still no documented framework defect with a
verified fixed version; directive §2). TEST-C dispatched (whole class, LIFECYCLE) as
breadth/confirmation evidence; TEST-D/E remain in the matrix.
ACTION: root-cause report §4d + §5 disposition set + matrix row updated; ledger
addendum 2; CHECKPOINT_INDEX row updated.
TEST: verdict read from the run's job log + logcat artifact (probe line, numtests=1,
crash stack); no source changed in this entry (docs only).
COMMIT: (this commit) — pushed, ls-remote read-back.
CHECKPOINT: CP-G4-016 CONVERGED TO DISPOSITION (breadth evidence pending: TEST-C in
flight, TEST-D/E to dispatch). CP-G4-017 IN PROGRESS (wave 2 done at 362×5; build GREEN
@ a0a5357; wave 3 + Italian gate test pending).
NEXT: read TEST-C verdict → dispatch TEST-D (full suite, LIFECYCLE — includes the TEST-E
soak class) → wave 3 (MockTowerArchive title frame) → TowerStudio + ModelManager wave-2
completion → Italian completeness gate test (directive §4) → CP-G4-018 Tower
refoundation → CP-G4-019..025. No terminal verdict: nodes remain.

## G4-N19 (2026-11-04) — CONTINUATION read-back + TEST-D/E + localization wave 2-tail + Italian gate + onboarding + Tower architecture

NODE: GRAPH 4 NEXT CONTINUATION (directive §0-§8), mission CONTINUE_FROM_LATEST_VERIFIED_REMOTE_TIP.
FINDING: (1) FRESH READ-BACK (GRAPH4_CONTINUATION_READBACK_LATEST.md, §0): local=remote=c5b2cc9, worktree clean, main untouched. (2) TEST-C actual: 1st dispatch 41726653270 died pre-install (adb Broken pipe — E2E_BLOCKED_EMULATOR_ENVIRONMENT); accidental NORMAL-profile breadth reproduction captured from push-run 41726726187 (crash at 3rd test, route=settings — THIRD distinct route). (3) TEST-D 41802836756 @ c5b2cc9 EXECUTED with probe ciLifecycleProfile=true ×2: residual REPRODUCED AT SUITE BREADTH (numtests=31, abort at e2e_35_36, route=appearance). (4) TEST-E 41802846917 typed INCONCLUSIVE_EMULATOR_IDLE_SYNC (AppNotIdleException 60s ASYNC_TASKS pre-soak — NOT the lifecycle residual; channel verified numtests=1). Disposition STRENGTHENED: PHYSICAL_XIAOMI_RECOVERY_REQUIRED now proven profile-, route- and position-independent (report §5a ledger).
ACTION: localization wave 2-tail EXECUTED (TowerStudio 63 ts_*, ModelManager 150 mm_*, Console dialog 4 cd_* — 591×5 then 622×5 with onboarding; parity/positional/anchors green; E2E-pinned en defaults byte-preserved; wave 3 dispositioned SIMULATED_SAMPLE_DATA, audit §5); directive §4 Italian completeness gate implemented (E2EItalianCompletenessTest: sets Italian via AppLanguage+recreate, walks every ordinary destination + dialogs + QR + archive tabs through the REAL product path, classifies visible semantics against runtime-built Italian resource pool + reviewed technical shapes + banned-English list, restores en in finally); directive §6 SIMPLE ONBOARDING (OnboardingScreen 6 chrome-free steps born-localized, first-launch flag, AppNav early-return gate + pendingRoute, REAL pack installer + REAL Self-Test voice foundation, LLM default "Not now", E2E harness presets the flag, E2EOnboardingTest walks all six REAL steps); directive §7/§8 TowerArchitecture (original Canvas-only upper order/lower structure/messenger path, app-state-driven flow, IME chat-focus 52% animated shrink; QualityBudgets.of fix).
TEST: cloud runs — 9af5d8a build SUCCESS (onboarding green); 41804541425 (syntax), 41805307105 (lint ×35), 41806550389 (androidTest compile), 41809752801 (lint StateFlow), 41811951325 (budget type) all diagnosed from evidence and repaired (fixes 6/7/8/2/2); LocalizationParityTest + lint green at 349aba1; gate re-dispatched at 349aba1 (2 earlier attempts died to adb device offline / fleet flakiness); TEST-C re-dispatch 41802826358 remained in flight.
FILES: docs/governance/GRAPH4_CONTINUATION_READBACK_LATEST.md; LOCALIZATION_LITERAL_AUDIT.md §5; E2E_LIFECYCLE_ROOT_CAUSE_REPORT.md §4 matrix + §5/§5a; app strings ×5 (+231 keys); TowerStudioScreen/ModelManagerScreen/ConsoleScreen migration; E2EItalianCompletenessTest.kt; OnboardingScreen.kt + E2EOnboardingTest.kt + E2ECommon harness preset; AppNav gate; TowerArchitecture.kt + ConsoleScreen wiring; a2ed0db→56b181c→1b3b65c→44f0cf8→0b55615→32079a1→9af5d8a→41b4c35→349aba1.
COMMIT: (this commit) — pushed with ls-remote read-back.
CHECKPOINT: CP-G4-016 CLOSED at disposition (§5a); CP-G4-017 code-complete (gate evidence pending — runs in flight); CP-G4-018 part 1 (onboarding) DONE; CP-G4-019 part 1 (Tower architecture) DONE pending visual gate.
NEXT: gate + TEST-C read-back; CP-G4-019 visual preview gate (§9); real source links/downloads (§10-11); runtime consumption proof (§13); full regression + APKs (§16-17). No terminal verdict: nodes remain.

## G4-N22 (2026-11-04) — Italian gate evidence chain (4 dispatches) + fixes 9-12

NODE: CP-G4-017 directive §4 gate — evidence collection on the emulator fleet.
FINDING: the gate EXECUTED the real Italian UI twice end-to-end. Run 41814872419: explicit scenario.recreate() raced the LocaleManager system relaunch → "stage was DESTROYED" (fix 9: switchLocaleAndAwait polls the relaunch). Run 8727210: gate ran with the real clock → compose.waitForIdle() never returned on the console's infinite Oracle Core animation → 3600s instrument timeout (fix 10: @Before freezeAnimationsForTest). Run 41818773091 (frozen): semantics OnClick from the test thread hit NavController.navigate → "addObserver must be called on the main thread" (fix 11: runOnMainSync on the gate's three direct-action sites). Run 41833663387: the gate WALKED the real Italian UI — the visible-semantics dump PROVES the Italian rendering (CAMERA DELL'ARCHIVIO, Archivio conversazioni →, Nessuna conversazione…, localized engine-truth row 'mic: push-to-talk (riconoscitore di sistema) · testo: di prima classe · …tower: SIMULATA fino a Tower Graph 1.7') — then stalled navigating archive→connect and a system ANR dialog froze the process (fix 12: pressBack leaves the full-screen scanner; ANR typed QR_ARCHIVE_REGION_ANR_EMULATOR). Run 41840691263 (01493a1): SAME stall at the archive→TowerStudio transition (anchor 'Connessione a Hermes Tower' not composed within 20s; tree shows console+archive) — typed GATE_NAVIGATION_STALL_ARCHIVE_TO_TOWERSTUDIO_EMULATOR.
ACTION: four gate dispatches + four fixes, all cloud-verified; the stall is a frozen-clock navigation-physics issue of the GATE harness on the emulator, distinct from the lifecycle residual (no destroy-path crash anywhere in the gate runs) and from the Italian UI content (proven correct twice).
TEST: artifacts 13315138252/13320206927/13323955209/13328900109/13330164361 + screenshots.
FILES: E2EItalianCompletenessTest.kt (fixes 9-12); WORK_LOG/audit unchanged otherwise.
COMMIT: (this commit) — pushed with ls-remote read-back.
CHECKPOINT: CP-G4-017 = localization code-complete (622×5) + ITALIAN_UI_GATE_EVIDENCE_COLLECTED; the ITALIAN_UI_COMPLETE state is NOT claimed — the gate must go green first (next session: root-cause the archive→connect stall, likely the hub-path fallback after activateRow returns true-but-stale; then §9 visual preview gate, §10-13 links/downloads/runtime, §14-15, §16-17).
NEXT: continue the same mission — no terminal verdict while dependency-released nodes remain.

## G4-N23 (2026-11-04) — FINAL CONTINUATION intake + segmented Italian gate (directive §3/§4/§25)

NODE: GRAPH 4 FINAL IMPLEMENTATION CONTINUATION (directive §0-§25), mission
CONTINUE_FROM_LATEST_VERIFIED_REMOTE_TIP.
FINDING: (1) FRESH READ-BACK (GRAPH4_FINAL_CONTINUATION_READBACK.md): local HEAD ==
remote HEAD == 92465e5c5c71942eb7d61ea2dc913ea3babf60e4 (fetch exit 0), worktree clean,
main untouched; the reported tip is CONFIRMED still the latest. 622 string keys ×5
locales counted live. (2) Cloud at tip: android-debug-build 41846567840 SUCCESS;
android-e2e 41846567723 (push, full suite) crashed at test 3/33 with the DISPOSITIONED
lifecycle residual (NavBackStackEntry route=settings INITIALIZED→DESTROYED at
Activity.performDestroy → Process crashed) — the crash destroys every later class's
evidence (same shape at 01493a1: run 41840678804). (3) The last gate-only run
41840691263 (01493a1) timed out at waitForText 20 s while its OWN timeout dump shows the
requested Archive anchor ("Archivio della Torre" + CAMERA DELL'ARCHIVIO) ALREADY
COMPOSED — the destination appears; the wait expired at the composition boundary
(R-7, TEST_TIMEOUT_TOO_LOW_WITH_EVIDENCE); logcat shows NO ANR of our process.
ACTION: (1) ITALIAN_GATE_NAVIGATION_FAILURE_LEDGER.md created — seven gate-related runs
R-1..R-7, each with the directive §3 fields and a typed classification from the allowed
vocabulary; NO APPLICATION_NAVIGATION_DEFECT / ROUTE_NOT_REACHABLE / MAIN_THREAD_BLOCK
anywhere. (2) The monolithic audit is REPLACED (superseded, logic carried verbatim) by
the SEGMENTED GATE: ItalianGateSupport.kt (shared harness: verbatim banned-English list,
invariants, technical shapes, runtime Italian pool; evidence-based physics — 60 s
destination-composition floor backed by R-7, runOnMainSync activations per R-3,
scanner Back exit asserted per R-4, baseline self-healing, en-restore in every
finally so the regression invocation keeps its en anchors) + six classes:
ItalianGateA_OnboardingConsoleTest (6-step wizard REAL walk + Console + inject dialog),
ItalianGateB_ChatHistoryArchiveTest (chat surface, History, Archive 3 tabs),
ItalianGateC_TowerGateQrScannerTest (Tower Gate → Studio → scanner → EXPLICIT Back
return), ItalianGateD_ModelsAndVoiceTest (Model Sanctum + attribute sheet),
ItalianGateE_SettingsAppearanceLanguageTest (hub + SIX categories asserted +
Appearance + Voice + Language), ItalianGateF_DiagnosticsSelfTestAboutTest
(Diagnostics/Self-Test/Local Intelligence/About). (3) android-e2e.yml restructured to
the SEGMENTED INSTRUMENTATION MODEL: gate invocation (six classes, MUST pass) +
regression invocation (full suite minus the quarantined class via -e notClass, MUST
pass) + residual invocation (quarantined class alone, EXPECTED exactly the dispositioned
signature — any OTHER signature fails the job); the workflow_dispatch experiment channel
keeps the EXACT old single-invocation contract (TEST-A..E preserved); artifacts +
secret-scan extended to all four output files.
TEST: structural sanity 162 Kotlin files (the two LocalizationParityTest hits are the
test's own placeholder-assertion string literals, pre-existing); brace/paren balance
verified on all seven new files; anchors verified against screen sources (every segment
anchor is a string the destination actually renders — the R-7 class of defect excluded
by construction).
FILES: docs/governance/GRAPH4_FINAL_CONTINUATION_READBACK.md;
docs/governance/ITALIAN_GATE_NAVIGATION_FAILURE_LEDGER.md;
app/src/androidTest/.../ItalianGateSupport.kt + ItalianGateA..F (7 new);
E2EItalianCompletenessTest.kt superseded (deleted; classification carried verbatim);
.github/workflows/android-e2e.yml; WORK_LOG.md; CHECKPOINT_INDEX.md.
COMMIT: (this commit) — pushed with ls-remote read-back.
CHECKPOINT: CP-G4-017 = segmented gate CODE-COMPLETE; ITALIAN_UI_COMPLETE still NOT
claimed — the six segments must go green in the cloud first (next exact action).
NEXT: read the gate/regression/residual cloud runs → record ITALIAN_UI_COMPLETE or
typed evidence → §5 five-language completion → §8 visual preview gate → §9-11 Tower
refoundation → §12-13 links → §14-16 STT/TTS/LLM → §17-18 → §19-20 regression+APKs →
§21 runbook → §22 governance → §23 CP-G4-018..025 → §24 terminal verdict only when
no dependency-released executable node remains.

## G4-N24 (2026-11-04) — GRAPH 4.6: dual APK + LLM + Archive + receipts + gate repairs (directive §0-§9)

NODE: GRAPH 4.6 BETA READINESS (mission CONTINUE_FROM_LATEST_VERIFIED_REMOTE_AUTHORITY).
FINDING: (1) FRESH READ-BACK (GRAPH46_READBACK.md): remote tip VERIFIED b3b78ed
(local==remote, fetch exit 0, main untouched, 639 keys ×5 counted live, unit 250 green
at tip); the working tree carried the UNCOMMITTED §8 dual-APK split (rescued, with a
duplicate-if-no-files-found YAML defect repaired). (2) Three segmented-model E2E runs
were in flight at read-back (E2E_RESULT_UNAVAILABLE); the first to complete
(41868118783 @ b3b78ed) delivered the FIRST segmented-gate evidence and FOUR typed
rows: R-8 anchor case-transform (wizard 'SCEGLI LA TUA LINGUA' in semantics vs 'Scegli
la tua lingua' resource — composed but case-exact-matched), R-9 the first
post-relaunch interaction raced the recreation and the remember{}-held inject-dialog
state was dropped (gates A2+B), R-10 gateNavigateTo's unconditional waitForIdle hung
on the infinite Oracle Core animation for the whole gate budget (runCatching does not
catch hangs), R-11 the dispositioned lifecycle residual crashed a SECOND class
(E2EFreshInstallTest e2e_03_04) in the regression invocation — it is CLASS-INDEPENDENT
(4th distinct class/route), so single-class quarantine can never protect the rest.
(3) CP-G4-021 splits RENAMED the debug APK outputs — runs 41883447613/41884838553 died
at install ('adb: failed to stat app-debug.apk') BEFORE any instrumentation; the E2E
workflow was still installing the dead name. (4) 6545969 build failed:
compileDebugUnitTestKotlin — \x00 is not a Kotlin escape (LocalLlmEngineTest:124).
ACTION: CP-G4-021 (7d1f8cf) read-back + splits committed (universal + arm64-v8a +
x86_64, universalApk=true, no versionCode offsets) + bounded installs; CP-G4-022
(d37d922) OPTIONAL LOCAL LLM (LocalLlmEngine: DEFAULT DISABLED via
ConsoleSettings.localLlmEnabled + SettingsRepository persistence; real size+SHA-256
integrity gate against the verified catalog candidate; safe-prompt builder with
bounded scope/length/control-char strip; typed diagnostics) + NoVerifiedLlmRuntime as
the ONLY shipped runtime (generation typed-blocked LLM_RUNTIME_PENDING_VERIFIED_SOURCE
— the engine never invents a completion) + 10 unit tests + Local Intelligence card
(652 keys ×5) + TOWER ARCHIVE §7 completion (offline pin toggle + PINNED OFFLINE
badge, provenance + SHA-256 rows on artifacts) + STT_RUNTIME_RECEIPT.md (vosk-android
0.3.75 AAR re-verified LIVE on Maven Central: sha256 ab2f8b91…, 13,472,638 B,
ABI-complete; model URL re-verified LIVE: HTTP 200, 49,665,141 B = catalog-exact;
LOCAL_STT_READY_FOR_XIAOMI_QUALIFICATION) + TTS_RUNTIME_RECEIPT.md (system TTS ACTIVE
with playback/interruption/engine read-back; Piper candidates verified-downloadable,
runtime row honestly pending; LOCAL_TTS_ACTIVE) + MONUMENTAL_TOWER_FINAL_REPORT.md
(7 required previews with measured post-repair evidence) + XIAOMI_GRAPH4_BETA_PACKAGE.md;
CP-G4-023 (6545969→c97a02f→bba1ebc) gate repairs R-8 (ignoreCase matching in the
harness — matcher only, banned-English classifier unchanged) + R-9 (switchLocaleAndAwait
settles the relaunch and requires the anchor to PERSIST across a second poll; gates
A2/B await the composer hint before the dialog interaction) + R-10 (bounded settle
pump — NO unbounded wait remains in the harness) + R-11 (regression leg = ONE
INVOCATION PER CLASS with per-class typed classification: OK → pass; dispositioned
signature → typed job-neutral INFRASTRUCTURE_FAILURE; anything else FAILS; no test
deleted/skipped/weakened; residual timeout 600→900 s) + fix 1 (escape) + fix 2 (E2E
installs app-universal-debug.apk + single-line androidTest glob).
TEST: build SUCCESS + 260/260 unit tests + lint 0 errors/60 warnings at c97a02f AND
bba1ebc (runs 41884838651/41890040725); APKs hashed from the artifact bytes
(APK_SIZE_AND_ABI_REPORT.md: arm64 105,247,044 B f2f38b61…, universal 206,679,899 B
97998108…, x86_64 109,176,588 B 7c4de490…); vosk AAR + model URL re-verified live.
FILES: docs/governance/{GRAPH46_READBACK,ITALIAN_GATE_NAVIGATION_FAILURE_LEDGER,
ACTIVE_RUNTIME_READBACK,WORK_LOG,CHECKPOINT_INDEX}.md;
docs/receipts/{STT_RUNTIME_RECEIPT,TTS_RUNTIME_RECEIPT,APK_SIZE_AND_ABI_REPORT,
ITALIAN_UI_REPORT}.md; docs/tower/MONUMENTAL_TOWER_FINAL_REPORT.md;
docs/XIAOMI_GRAPH4_BETA_PACKAGE.md; app/local/{LocalLlmEngine,LlmRuntime}.kt +
ConsoleStateStore/SettingsRepository/AppGraph/LocalIntelligenceScreen/ArchiveScreen +
res ×5 + LocalLlmEngineTest + ItalianGateSupport/A/B + android-e2e.yml +
android-debug-build.yml.
COMMIT: (this commit).
CHECKPOINT: CP-G4-021/022/023 CLOSED; CP-G4-024 (terminality) = the E2E verdict at
bba1ebc (run 41890040629) arbitrates ITALIAN_UI_COMPLETE and the terminal receipt.
NEXT: read the bba1ebc E2E artifacts → ITALIAN_UI_REPORT verdict → terminal receipt
GRAPH_4_IMPLEMENTATION_COMPLETE_XIAOMI_QUALIFICATION_PENDING (or the next typed fix).

## G4-N25 (2026-11-05) — GRAPH 4.6: fresh read-back at a2d0b27 + R-14 gate/workflow repairs (directive §0/§11)

NODE: GRAPH 4.6 §0 FIRST EXACT ACTION (fresh read-back, NEW session) + §11 Next Exact
Action from the TRUE remote tip.
FINDING: (1) FRESH READ-BACK: `git fetch origin` exit 0; remote tip VERIFIED
**a2d0b27** (local HEAD == origin/graph-4/real-voice-pairing-archive-e2e, main
untouched, tree clean) — the CP-G4-023 fix-4 commit is the true authority; every
prior tip claim (b3b78ed et al.) superseded. (2) The staged terminality is NOT a
verdict: GRAPH_46_TERMINAL_RECEIPT.md §2 and ITALIAN_UI_REPORT.md §6 both carry
intact placeholders committed at e084be8 — ITALIAN_UI_COMPLETE remains UNCLAIMED
until a real cloud run shows gate 6/6 OK. (3) Cloud evidence at the tip: E2E run
41938185541 (#132) **CANCELLED at the 180-min cap** — gate A1 failed on the
AppNotIdleException flake (R-13's single retry consumed; stack Espresso.onIdle →
loopMainThreadUntilIdle), gate A2 **PASSED** (R-12 direct-dispatch physics holds),
the emulator DIED a FOURTH time during gate B (typed DEVICE_OFFLINE_ABORT — the
offline-guard worked), and the UNBOUNDED `adb logcat -d` capture line ran against
the dead fleet for 2 h 22 min until the cap cancelled the job BEFORE the verdict.
Build run 41938185538 (#114) at the same tip: BUILD SUCCESSFUL (assembleDebug +
testDebugUnitTest + lintDebug), lint 0 errors, APKs byte-identical in size to the
c97a02f receipt (arm64-v8a 105,247,044 B; universal 206,679,899 B; x86_64
109,176,588 B — the "~165 MB" figure in the old briefing was the pre-split APK at
b1df58a), strings counted live 652 ×5 locales (en/it/es/fr/de).
ACTION: CP-G4-023 fix 5 — (a) R-14 typed repair: `gateRecreateSteadily()` in
ItalianGateSupport (catch ONLY AppNotIdleException, backoff 3 s→10 s→30 s, three
retries, fourth failure propagates honestly) wired at BOTH gate recreate sites
(gateEnsureBaseline supersedes the R-13 single retry; ItalianGateA's raw
`scenario.recreate()` replaced) — nothing skipped, nothing weakened, the REAL walk
unchanged; (b) android-e2e.yml: logcat/screencap bounded `timeout 60`, residual
invocation offline-guard writes the typed DEVICE_OFFLINE_ABORT into
residual-output.txt (verdict channel stays honest), job timeout 180→300 min
(per-class arithmetic worst case ≈263 min; hosted ceiling 6 h); (c) governance:
ledger R-14 + this file + CHECKPOINT_INDEX + GRAPH46_READBACK §7 fresh read-back.
FILES: app/src/androidTest/java/com/hermestower/voiceconsole/e2e/
{ItalianGateSupport,ItalianGateA_OnboardingConsoleTest}.kt;
.github/workflows/android-e2e.yml;
docs/governance/{GRAPH46_READBACK,ITALIAN_GATE_NAVIGATION_FAILURE_LEDGER,WORK_LOG,
CHECKPOINT_INDEX}.md.
COMMIT: (this commit).
CHECKPOINT: CP-G4-023 fix 5; CP-G4-024 (terminality) stays STAGED — both verdict
placeholders intact until a cloud run at THIS tip shows gate 6/6 'OK (… tests)' +
regression green-or-residual-typed + residual typed.
NEXT: run #133 verdict → ITALIAN_UI_REPORT §6 + GRAPH_46_TERMINAL_RECEIPT §2 filled
ONLY from that run → GRAPH_4_IMPLEMENTATION_COMPLETE_XIAOMI_QUALIFICATION_PENDING
(or the next typed fix row).

## G4-N26 (2026-11-05) — R-15: two fleet collapses in one wave; fix-5 mechanics verified; rerun re-entered

NODE: GRAPH 4.6 §11 — monitor the fix-5 tip runs and classify.
FINDING: both push-triggered E2E samples of the fix-5 wave died on INFRASTRUCTURE,
not code — #134 never brought the guest online (boot-level, `adb: device offline`
05:19:53→05:37, zero instrumentation); #133 booted, installed, compiled the gate
(assembleDebugAndroidTest SUCCESS — R-14 code compiles) and died at gate A's first
invocation (gate_A.txt holds only A1's stream header). Every fix-5 mechanism worked
under collapse: typed offline-guard into residual-output.txt, bounded 60 s
logcat/screencap (residual→verdict gap exactly 60 s), honest E2E_VERDICT_FAILED,
~1 h 05 m total instead of 3 h+. Builds #115/#116 SUCCESS at both tips (unit 260/260
COUNTED from the 7c57025 artifact; lint 0 errors; APK sizes byte-identical:
105,247,044 / 206,679,899 / 109,176,588).
ACTION: ledger R-15 + this entry + read-back addendum; governance-only commit
re-enters the E2E queue at the current tip (the established infrastructure rerun
channel — no test touched).
FILES: docs/governance/{ITALIAN_GATE_NAVIGATION_FAILURE_LEDGER,WORK_LOG,
GRAPH46_READBACK,CHECKPOINT_INDEX}.md.
COMMIT: (this commit).
CHECKPOINT: CP-G4-023 fix 5 (R-15 addendum); CP-G4-024 STAGED.
NEXT: the rerun verdict at this tip arbitrates ITALIAN_UI_COMPLETE (or the next
typed row).

## G4-N27 (2026-11-05) — R-16: idling tolerance 60s→240s (the binding constraint), rerun re-entered

NODE: GRAPH 4.6 §11 — monitor #135, classify, narrow repair.
FINDING: run #135 @ 9508ac8 proved the R-14 helper mechanically correct and
insufficient: A1's recreate hit AppNotIdleException through ALL FOUR attempts
(class time 604 s = 4×60 s idle syncs + backoffs + setup), because the binding
constraint is Espresso's 60 s MASTER idling policy against a drain that needs
70–180 s under fleet starvation (it finishes — A1 passed at bba1ebc, A2 passed
again here). Fleet death #7 hit mid-gate B; typed containment held everywhere.
ACTION: R-16 — IdlingPolicies master+resource 240 s at gateEnsureBaseline (the
R-7 precedent: evidence-backed tolerance raise; fresh process per gate class so
exactly one class is affected; the recreate must still SUCCEED);
gateRecreateSteadily resized to 2 retries (worst 3×240 s ≈ 760 s < 900 s
per-class budget); ledger R-16 + this entry; governance commit re-enters the
queue at the R-16 tip. No test deleted, skipped or weakened; no assertion touched.
FILES: app/src/androidTest/java/com/hermestower/voiceconsole/e2e/
ItalianGateSupport.kt; docs/governance/{ITALIAN_GATE_NAVIGATION_FAILURE_LEDGER,
WORK_LOG,GRAPH46_READBACK,CHECKPOINT_INDEX}.md.
COMMIT: (this commit).
CHECKPOINT: CP-G4-023 fix 6 (R-16); CP-G4-024 STAGED.
NEXT: run #136 verdict at this tip arbitrates ITALIAN_UI_COMPLETE (or the next
typed row).

## G4-N28 — CP-G4-023 fix 7 / R-17 (post-boot service collapse repair)

Run #136 (41983695658 @ 982877c, job 127099393926) parsed from the full job log:
boot COMPLETED in 968 s, then system_server lost activity/package services within
~1 min (multidisplay broadcast "Can't find service: activity" at 07:17:27; app
install "cmd: Can't find service: package" at 07:18:15) — ZERO instrumentation,
death #8, the boot→install window was the last unguarded collapse segment.
R-17 (R-14/R-16 precedent, evidence-backed single recovery): guest health gate
BEFORE the installs — `pm list packages` probe; dead → `adb reboot` + bounded
re-wait 1200 s + sleep-60 settle + ONE re-probe; still dead → typed
E2E_BLOCKED_EMULATOR_ENVIRONMENT honest abort; healthy → the REAL walk proceeds
unchanged; both install lines typed `|| { …; exit 1; }` so non-collapse failures
abort honestly with NO blind retry. ONE recovery per run keeps the 300-min cap
safe (worst ≈289 min). No test deleted, skipped or weakened; no assertion touched.
FILES: .github/workflows/android-e2e.yml; docs/governance/
{ITALIAN_GATE_NAVIGATION_FAILURE_LEDGER,WORK_LOG,GRAPH46_READBACK,CHECKPOINT_INDEX}.md.
COMMIT: (this commit).
CHECKPOINT: CP-G4-023 fix 7 (R-17); CP-G4-024 STAGED.
NEXT: the push run at the R-17 tip arbitrates ITALIAN_UI_COMPLETE (or the next
typed row).
