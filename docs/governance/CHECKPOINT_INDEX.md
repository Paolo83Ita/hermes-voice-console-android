# CHECKPOINT INDEX — Hermes Voice Console Android

Foundation-era commits (preserved, local history before Graph 1):

| Commit | Checkpoint |
| --- | --- |
| 7a86640 | stage-a-docs: Stage A documentation set complete, Mermaid diagrams validated |
| 3aa69ad | stage-a-visuals: Marble Light / Obsidian Night mockups, comparison, contrast verification |
| ffe6749 | stage-c-package: README, BUILDING, FINAL_REPORT, worklog checkpoints; V0.1 checkpoint |

Graph 1 checkpoints (branch `foundation/v0.1-cloud-build`, pushed to private origin):

| Checkpoint | Content | Evidence added |
| --- | --- | --- |
| graph-1/repair | Local build brought to green: Gradle wrapper committed, 44+9 compile errors and 7 test failures repaired, UI_STATE_MACHINE doc co-updated | UNIT_TEST_EXECUTED (33/33 local) |
| graph-1/governance | 10 authority files installed under docs/governance/ | DOCUMENT_CREATED |
| graph-1/ci | GitHub Actions debug-build workflow (JDK 17, SDK 35, wrapper, tests, lint, artifacts) | CLOUD_WORKFLOW_CREATED |
| graph-1/receipt | Cloud-built APK retrieved + SHA-256 + commit binding | APK_CREATED, APK_HASHED, APK_BOUND_TO_COMMIT |
| graph-1/qualification | Owner install runbook + device checklist refresh | DOCUMENT_CREATED |
| graph-1/final | Truth refresh, blockers closed, ZIP refresh | DOCUMENT_CREATED |

Exact SHAs: repair=dfabeeb · governance=6b4cef8 · ci=8038bfd · ci-fix=505047e9 ·
receipt/qualification/final = see the git log of `foundation/v0.1-cloud-build` (recorded
progressively in WORK_LOG.md; a file cannot contain its own commit SHA).

Graph 2 checkpoint ledger (continued):

| Checkpoint | Commit | Content |
| --- | --- | --- |
| CP-G2-017..020 | 5b54b48 → fb610ce | e2e workflow rewrite, disk/userdata/budget, migration typed-crash repair, explicit adb install + heredoc fix |
| CP-G2-021 | a9657f0 | POSIX-sh-safe instrumentation runner (dash killed `set -o pipefail` before am instrument) |
| CP-G2-022 | (this commit) | E2E root-cause repairs (drawer HERMES dedup, display UOE guard) + Graph 2 terminal receipt + convergence |

Graph 3 checkpoints (branch graph-3/monumental-oracle-tower-visual-refoundation, from bc186db):

| Checkpoint | Commit | Content |
| --- | --- | --- |
| CP-G3-001 | (this commit) | bootstrap: workflow triggers for graph-3, MISSION_GRAPH/DECISION_LOG/WORK_LOG Graph 3 sections |

| CP-G3-002 | 6959c07 | Waves 1–4: foundations, Oracle Core, console, chrome; docs+preview pack; local 102/102 tests |

| CP-G3-003 | de4e54e | cloud build evidence + APK receipt + doc convergence |
| CP-G3-004 | (this commit) | loopback-only cleartext policy (download-FAILED root cause) + E2E ledger |

| CP-G3-005 | fcea6bc | in-script logcat/screenshot evidence capture (runner teardown was deleting the device first) |
| CP-G3-006 | 6a92942 | awaitState surfaces downloader error text + byte counters |
| CP-G3-007 | (this commit) | preflight getFileStore → File.usableSpace (Android-supported); REAL download root cause fixed |

| CP-G3-008 | (this commit) | terminality + terminal receipt + truth convergence + final package |

Graph 4 checkpoints (branch graph-4/real-voice-pairing-archive-e2e, from ca8980b):

| Checkpoint | Commit | Content |
| --- | --- | --- |
| CP-G4-001 | 33e7bb6 | bootstrap: verified read-back, branch creation, CURRENT_PROJECT_TRUTH_GRAPH4_PRE |
| CP-G4-002 | 9b4e17a | NODE 0: lint truth repaired (0 errors), AnimationFrameClock, §3 ancillary repairs |
| CP-G4-003 | 525c71f | NODE 1: verified production catalog, 12 entries, real hashes/licenses/provenance |
| CP-G4-004 | (this commit) | NODE 2/3: MODEL SANCTUM — pack installer, receipts, §7 roots, gatekeeper, real-source wiring |
| CP-G4-005 | c2ebd88 | NODE 3: real voice pipeline foundation — seams, controller, energy VAD, system TTS, permission flow; 136/136 |
| CP-G4-005R | 262a79e | pending-E2E read-back: remote tip afe3e08 verified = local; cloud builds 213182a/c2ebd88/afe3e08 SUCCESS (136/136 cloud JUnit, APK f241704…, lint 0 err/32 warn); c2ebd88 E2E = E2E_BLOCKED_EMULATOR_ENVIRONMENT (adb install broken pipe ×5, zero tests ran, KVM present); two E2E runs still active → E2E_RESULT_UNAVAILABLE; receipt docs/receipts/CP-G4-005-READBACK-RECEIPT.md |
| CP-G4-006 | 090040d + adbc16c | G4-C02 AudioRecord frame source (typed failures, lifecycle matrix, 17 new tests) + G4-C03 Main Console real voice wiring (ConsoleVoiceCoordinator, system SpeechRecognizer STT, real system TTS with honest SPEAKING end, first-class text chat composer, draft persistence, PTT medallion, repeat-last, unified stop) — 167/167 |
| CP-G4-010 | (this commit) | G4-C07 compact responsive navigation — DrawerWidthPolicy (hard 2/3 maximum, 60% preferred, 248–360dp bounds; pure + full width-matrix test), drawer rebuilt with HEADER (sigil/bridge/close) + PRIMARY/SECONDARY (Owner §11 destinations) + FOOTER (version/mode/lang), 48dp rows, two-line labels |
| CP-G4-011a | (this commit) | G4-C08 wave 1 — five-language localization: 75-key strings in values + it/es/fr/de (Owner §14 terminology anchors), AppLanguage per-app locale (LocaleManager 33+ official, attachBaseContext fallback 30–32), localeConfig, Settings→Language (Follow system + 5 languages, persistent, reversible), Console+Nav+Appearance localized, LocalizationParityTest (GATE-05) — 178/178 |
| CP-G4-012a | (this commit) | G4-C09 wave 1 — versioned Tower pairing client: strict hermes-pair://v1 QR parser with full typed-rejection matrix (Owner delta §11, 14 deterministic tests incl. replay/expiry/environment/endpoint), session nonce store, redaction-safe describe, MockTowerPairingClient (deterministic, ZERO network, real pairing explicitly deferred to Tower Graph 1.7). Tests 192/192 |
| CP-G4-009 | ed50b21 | G4-C06 real model delivery — the 4 physically verified ONNX artifacts ride in the APK (assets/models/bundled/ + manifest); BundledModelInstaller double-gates every byte against BOTH the bundled manifest AND the shipped catalog (typed refusals on conflict), quarantine facts sidecar, atomic install, registry read-back, activation never bypassed, 'app-bundle' provenance, idempotent; AppGraph on-demand constructors (wake OFF, denoiser bypassed); ModelManager 'Bundled verified delivery' section. Tests 234/234 |
| G4-C11 wave | 9543007 | Self-test expansion ST-46..ST-50 — bundled delivery (double-gate agreement + §6 end-to-end ×2 idempotent), QR strict-matrix probe, archive truth §15/§16/§17, Graph 4 route registry; SelfTestSubjects extended with asset-backed members degrading to typed skips on JVM; QR_SCANNER registry omission found+fixed. Tests 242/242 |
| infra | e9abee9 | Dependency-resolution resilience: Aliyun mirrors appended after primaries in settings.gradle.kts after Maven Central 429/5xx transient broke run 41618048112 (docs-only commit) — VERIFIED: e9abee9 build 41621063944 SUCCESS; E2E ed50b21 failure = documented typed residual (third sample) |
| G4-C10 wave | 32e35e0 | Monumental visual convergence — MeanderBand motif extended from Console-only to all 12 secondary screens; strictly additive Canvas, no semantics/text/selectors; E2E strings untouched. Tests 242/242 |
| CP-G4-010 wave 2 | 5c7993b | G4-C09 wave 2 — camera QR pairing scanner: CameraX 1.4.2 + ML Kit barcode-scanning 17.3.0 (both verified on Google Maven + licensed Apache-2.0 before adoption), pure QrPairingScanFlow (debounce, terminal detection, replay honesty, typed rejections, truthful frame counter), QrScannerScreen (in-context permission, typed CAMERA_UNAVAILABLE, lifecycle-bound release, SIMULATED-only pairing), Tower Studio entry, route qr_scanner, Manifest CAMERA+optional feature. Tests 242/242 |
| G4-N15 + read-back | 1fa7829 + 15b1f05 | E2E read-back e9abee9/32e35e0/b1df58a (4th/5th/6th residual samples, position-independent — class ordering varies, abort at #3 of 30 vs #14) + VERDICT-CHANNEL INFRA REPAIR (emulator-runner runs script line-by-line; multi-line if/fi always exit 2 — collapsed to POSIX one-liner, semantics identical) VALIDATED on cloud at 1fa7829 (41632784491 prints truthful INSTRUMENTATION_FAILED_OR_INCOMPLETE, zero execution syntax errors; 7th sample identical) + G4-C12 packaging: GRAPH4_DEBUG_APK_RECEIPT.md (APK @ b1df58a 165,029,486 B sha256 9275aa22…, growth decomposed honestly, 4/4 bundled assets re-hashed MATCH inside the CI APK, app_name invariant ×5 locales, ABI-splits Owner-optional) |

## GRAPH 4 — CP-G4-007 (E2E-ROBUSTNESS) — CLOSED WITH TYPED RESIDUAL (2026-11-03)
- NODE/FINDING/ACTION/TEST/FILES/EVIDENCE/COMMIT/READ-BACK: WORK_LOG G4-N11 + G4-N12;
  docs/testing/E2E_FAILURE_LEDGER.md (full per-failure ledger + verdict addenda).
- E2E trajectory: 14 deterministic failures (runs 41459906172/41461749711/41470976708) → 8
  (run 41539016998, state-machine repair verified) → 0 assertion failures
  (run 41551630218) with a typed EMULATOR_INPUT_INJECTION_LIMITATION residual aborting the
  suite at #14 (frozen-clock transition stall + navigation-compose teardown race).
- Application defect found + fixed: queue-state machine starvation (latent since Graph 1).
- Owner field report fixed: status-bar overlap (WindowInsets.safeDrawing at AppNav root).
- Governance: no main changes; no force-push; all checkpoints pushed and read back.
