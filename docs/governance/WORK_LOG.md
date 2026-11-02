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
