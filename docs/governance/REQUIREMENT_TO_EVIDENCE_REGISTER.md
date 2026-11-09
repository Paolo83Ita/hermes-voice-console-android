# REQUIREMENT-TO-EVIDENCE REGISTER — Graph-era snapshot

Full requirement set: `docs/REQUIREMENTS.md` (Foundation). This register pins the
requirements for which GRAPH 1 holds physical evidence, with typed evidence classes.

| Req (Foundation ID) | Requirement (short) | Evidence | Status |
| --- | --- | --- | --- |
| FR-10 | Text-injection simulation drives the real pipeline (no acoustic front-end in V0.1) | ScriptedConsoleDriver + ScriptedConsoleDriverTest (5 pipeline tests) | UNIT_TEST_EXECUTED — MOCK_SIMULATION_PASSED |
| FR-11..FR-15 | Deterministic routing per mode/connectivity/consequential rules | RuleBasedIntentRouterTest (9 tests) | UNIT_TEST_EXECUTED |
| FR-19 | Local model never a second authority (bounded roles) | IntentRouter seam + LOCAL_MODEL_CANDIDATE_PLAN.md §1 | IMPLEMENTED_NOT_TESTED (design constraint, compile-verified) |
| FR-20/21 | TowerBridge seam; mock bridge with fault injection | MockTowerBridgeTest (5 tests), BridgeFault enum | UNIT_TEST_EXECUTED — TOWER_MOCK_INTEGRATION_PASSED (mock only) |
| Tower contract | Versioned request/event frames, receipt refs, error taxonomy | TowerContract.kt + TowerContractJsonTest (4 tests) | UNIT_TEST_EXECUTED |
| State machine | 15 states, legal-transition table, enforced | ConsoleStateMachineTest (6 tests) + docs/UI_STATE_MACHINE.md §3 (co-updated) | UNIT_TEST_EXECUTED |
| Privacy mute | Mute reachable everywhere except DORMANT; idempotent | ConsoleStateMachineTest + driver mute test | UNIT_TEST_EXECUTED |
| SEC-06 | Log redaction: credentials, hex, base64 masked | LogRedactorTest (4 tests) | UNIT_TEST_EXECUTED |
| SEC-04 | No transcript content in event logs | driver test `event log never contains transcript content` | UNIT_TEST_EXECUTED |
| FR-36 | Diagnostics show real device facts only | AndroidDeviceHealthMonitor (platform-sourced, null when unavailable) | IMPLEMENTED_NOT_TESTED (needs device) |
| FR-34 | Local Intelligence screen truthful (nothing claimed to work) | LocalIntelligenceScreen + TruthBadge | SOURCE_CREATED |
| Build gate | Project compiles and unit tests pass | Local Gradle build green; cloud build pending | UNIT_TEST_EXECUTED (local) / CLOUD_BUILD_PASSED pending |
| Device qualification | Install + read-back on Xiaomi 11 Lite 5G NE | — | QUALIFICATION_PENDING_DEVICE |

Test totals (physical, 2026-11-01): 33 `@Test` in 6 classes; 33 executed, 0 failed (local JVM).

---

# GRAPH 2 REQUIREMENTS (VM-*, TC-*, SC-*, MG-*) — evidence appended as nodes complete

| Req | Requirement (short) | Evidence | Status |
| --- | --- | --- | --- |
| VM-01..11 | Voice Model Manager contracts (catalog, resolver, download, verify, storage, qualification, activation, rollback, registry, resource reader, policy) | — | PENDING |
| VM-STATE | 18 model states; no silent download; explicit confirmation gate | — | PENDING |
| VM-UI | Settings→Voice Models and Local Intelligence (A–F sections) | — | PENDING |
| TC-01 | TowerConnectionProfile typed schema (26 fields) + config states | — | PENDING |
| TC-02 | Deterministic validation + typed test outcomes; never CONNECTED on validation-only | — | PENDING |
| TC-03 | Secrets in Keystore-backed vault; secret-clean export/import | — | PENDING |
| TC-04 | TowerBridge boundary: versioned contract, mock + real adapter + local deterministic server | — | PENDING |
| TC-UI | Settings→Hermes Tower Connection; Basic/Advanced split; 11 actions | — | PENDING |
| SC-01 | Self-Test Center ST-01..ST-45 with typed results and modes | — | PENDING |
| SC-02 | In-app automated test architecture (JVM + instrumentation + Compose) | — | PENDING |
| MG-01 | V0.1→V0.2 versioned settings migration (fresh/partial/corrupt/unknown/rollback) | — | PENDING |
| E2E-01..45 | Android emulator end-to-end flow, terminal state ANDROID_EMULATOR_E2E_PASSED | — | PENDING |
| APK-G2 | 0.2.0-debug APK created, hashed, bound to commit | — | PENDING |
| GATE-01..15 | Graph 2 mandatory gates | — | PENDING |

---

# OWNER PHYSICAL FEEDBACK (2026-11-06) — evidence appended

| Req | Requirement (short) | Evidence | Status |
| --- | --- | --- | --- |
| PHYS-01 | Physical install + launch on the Owner device | POCO X6 PRO real installation; package ca8980b (Graph 3 terminal, 0.2.0-debug) binary-inspected | DEVICE_INSTALLATION_PASSED + APPLICATION_LAUNCH_PASSED (PARTIAL_OWNER_EVIDENCE — package predates Graph 4) |
| PHYS-02 | Secondary screens layout/spacing | OWNER_UI_LAYOUT_DEFECT_LEDGER.md + UI_POLISH_PASS commit (BadgeFlowRow, SectionCard weight, SettingsHub rows) | REPAIRED_IN_CODE — verification pending second Owner cycle |
| PHYS-03 | Italian TTS answers in Italian | Code-proven root cause (silent setLanguage failure, no voice read-back) + typed negotiation repair + runtime read-back (Diagnostics rows, ST-51) | REPAIRED_IN_CODE — ITALIAN_VOICE_ACTIVE / ITALIAN_VOICE_READY_AND_BLOCKED_BY_KNOWN_RUNTIME_LIMITATION gated on device read-back |
| PHYS-04 | Current APK + arm64 APK + install/upgrade guide | 0.3.0-alpha-graph4-owner-feedback universal + arm64-v8a APKs; docs/INSTALL_POCO_X6_PRO.md | PACKAGE_PRODUCED — second Owner validation cycle pending |

---

# OWNER PHYSICAL RETEST FAILURE (2026-11-06) — evidence appended

| Req | Requirement (short) | Evidence | Status |
| --- | --- | --- | --- |
| RETEST-01 | Spoken response in Italian on device | Call-path trace: speak() fail-open (negotiation verdict never gates audio); repair = fail-closed gate + ITALIAN_SYSTEM_TTS_DATA_MISSING / ITALIAN_SYSTEM_TTS_READBACK_MISMATCH tokens + TEXT_ONLY_SAFE_DEGRADED | REPAIRED_IN_CODE — unit-tested; physical re-check = next Owner cycle |
| RETEST-02 | Comfortable voice (no HIGH_PITCH_ENGLISH_TTS_OUTPUT) | No setSpeechRate/setPitch anywhere pre-repair; speech-profile authority added (rate 0.96f, pitch 0.95f defaults, safe ranges, persisted) | REPAIRED_IN_CODE — unit-tested; physical re-check = next Owner cycle |
| RETEST-03 | Verify button readable in Models and Voice | Fixed horizontal TextButton rows overflow ≈372 dp; adaptive action layout + More actions overflow + 48 dp touch height | REPAIRED_IN_CODE — unit-tested; physical re-check = next Owner cycle |
| RETEST-04 | Microphone/mute/TTS-stop controls semantically distinct | Idle+unmuted privacy-mute and push-to-talk BOTH render Icons.Filled.Mic pre-repair; distinct visual vocabulary + unique contentDescriptions + tests | REPAIRED_IN_CODE — unit-tested; physical re-check = next Owner cycle |
| RETEST-05 | In-app proof of the installed build | Build identity panel (About & Safety → Build information; Diagnostics → Runtime truth; Diagnostics footer) with build-injected version/Git SHA/timestamp/run ID/label + on-device measured APK SHA-256 + variant + install timestamps; Copy + Export secret-clean receipt actions | REPAIRED_IN_CODE — build identity panel live; on-device verification next cycle |
| RETEST-06 | New repair package for the Owner | versionName 0.3.1-alpha-graph4-owner-repair / versionCode 4 / label POCO OWNER REPAIR 1; universal + arm64-v8a APKs + hashes + install guide + retest package | POCO_RETEST_PACKAGE_2_READY (unit tests green, lint 0 errors, universal + arm64 APKs hashed) |

## GRAPH 4.9 register (2026-11-07)

| Requirement | Evidence | Class |
| --- | --- | --- |
| Fresh read-back before any work | docs/governance/GRAPH49_FRESH_READBACK.md; fetch + API (tip eb660d5, runs 42247787324/42247787274, JUnit 310/310) | VERIFIED |
| e2e_14 regression repaired | SystemTtsDomainAdapter interrupt token + SystemTtsDomainAdapterTest (5); pre-repair lane artifacts bf34014 (VoiceFlow OK(6) both lanes) | JVM-VERIFIED |
| Natural local TTS (verified runtime + voice) | JitPack v1.12.24 AAR sha256 ad052d51…6c7e; piper 8ad07915…7021 + 7541f757…16b4; SherpaOnnxTtsEngineTest/TtsRouteSwitchTest; ST-52 | JVM-VERIFIED / PHYSICAL-QUALIFICATION-PENDING |
| Local STT user-operable test | fixture sha256 7b5483a2…f1a2f + manifest; SttFixtureTestRunnerTest; ST-53; Sanctum Test action | JVM-VERIFIED / PHYSICAL-QUALIFICATION-PENDING |
| Optional local LLM verified-or-blocked | qwen2.5-0.5b q5_k_m @ 9217f5db sha256 04147455…9fd55 (downloaded in full, exact match); NoVerifiedLlmRuntime typed block | ARTIFACT-VERIFIED / RUNTIME-TYPED-BLOCKED |
| Model Sanctum responsive qualification | MODEL_SANCTUM_RESPONSIVE_REPORT.md; AdaptiveActionPolicyTest; Gate D walk at tip | JVM+EMULATOR-VERIFIED / PHYSICAL-PENDING |
| Distinct microphone controls | VOICE_CONTROL_SEMANTICS_QUALIFICATION.md; ConsoleControlSemanticsTest | JVM-VERIFIED / TALKBACK-DEVICE-PENDING |
| Tower Archive product completion | TOWER_ARCHIVE_PRODUCT_COMPLETION_REPORT.md; MockTowerArchiveTest +2 | JVM-VERIFIED |
| Tower Gate / Graph 1.7 matrix | ANDROID_GRAPH4_TOWER_GRAPH1_7_COMPATIBILITY_MATRIX.md | DOCUMENTED (no invented routes) |
| Self-Test expansion | ST-52/ST-53; SelfTestCenterTest (53 definitions) | JVM-VERIFIED |
| Regression + APKs | 335/335 unit, lint 0 errors; POCO_X6_PRO_OWNER_RETEST_PACKAGE_3 hashes | BUILD-VERIFIED |
| Owner physical verdict | OWNER_POCO_RETEST_PENDING — no claim | PENDING (only the Owner may set POCO_RETEST_PASSED) |

| GRAPH 4.10 §4/§5 (P0-01) | ResponseLocaleResolver + ResponseCatalog; driver/engine/bridge/coordinator/LLM consume the authority; Italian E2E driver tests | JVM tests (8+8+7+2) | IMPLEMENTED — PHYSICAL PENDING |
| §6 coherence | visibleAssistantText == ttsInputText pinned via MockTextToSpeechEngine.lastSpokenText (Italian E2E + MANUAL playback tests) | JVM | IMPLEMENTED |
| §7/§8 (P0-03) | VoiceSelectionAuthority six-condition read-back + VoiceRuntimeFactory typed pipeline | 8+6 JVM tests + Sanctum wiring | IMPLEMENTED — VOICE_SWITCHING_OWNER_VERIFICATION_PENDING |
| §10/§11/§12 (P0-04/05) | ChatSpeechOutputPolicy persistence + speaker-control state machine + TtsReadbackStage (owner-verified never self-granted) | 6+6+2 JVM tests | IMPLEMENTED |
| §14/§15/§16 (P0-06) | ListeningModePolicy + ContinuousListeningController (typed stop conditions, guard interval) + EchoFingerprintGuard (0.80 Jaccard / 8 s window) | 5+5+6 JVM tests | IMPLEMENTED — WAKE_WORD UNQUALIFIED |
| §17-§26 (P0-07/08/09) | Parla labels; §19 width classes; §20 headers; §21 Studio rebuild; §22 action groups; §23 StateLabels table verbatim | 6 JVM contract tests + OWNER_XIAOMI_LAYOUT_REFERENCE.md | IMPLEMENTED — RESPONSIVE_UI_QUALIFIED UNCLAIMED |
| §29-§35 (P0-10/11) | 10 layered assets + ASSET_MANIFEST.json + ReforgedTowerVisual (11 states, 3 profiles) | previews + manifest hashes | IMPLEMENTED — OWNER_VISUAL_RETEST_PENDING |
| §37/§38 | 412/412 unit; lint 0 errors; v0.3.3-alpha-graph4.10-owner-repair / code 6 / label POCO XIAOMI OWNER REPAIR 3; 3 APKs hashed; dex stamps verified | this build | SHIPPED |
| §39/§40 | POCO_XIAOMI_RETEST_PACKAGE_3_GRAPH410.md runbook (both devices) | receipt | POCO_XIAOMI_RETEST_PACKAGE_3_READY |

| GRAPH 4.11 Node 1 — Voice A → play → Voice B → play → prove difference | ST-54 definition + AppGraph voiceAbQualification() (sequential real-engine legs, authority untouched, B unloaded) + 7 JVM decision tests | VOICE_SWITCHING_OWNER_VERIFICATION_PENDING (audible verdict Owner-only) |
| GRAPH 4.11 Node 2 — 10 turni continui | ContinuousListeningControllerTest ten-turn loop test + ListeningModeAndEchoTest ten-turn echo-rejection test | physical 10-turn soak stays Owner-runbook (Package 4) |
| GRAPH 4.11 Node 3 — nessuna risposta locale inglese | ResponseCatalogTest no-English-under-Italian sweep (46 categories) over the 4.10 producer tests | 422/422 unit GREEN |
| GRAPH 4.11 Node 4 — Xiaomi responsive pass | ResponsiveLayoutContractTest (§19/§22/§23 + Xiaomi classification) VERIFIED, no regression | real screenshots remain Owner-capture (ledger) |
| GRAPH 4.11 Node 5 — central Tower reconstruction from the Owner image | tower_refoundation_g411.py + ASSET_MANIFEST v2 (10/10 sha256) + re-rendered previews + receipts report update | OWNER_VISUAL_RETEST_PENDING |
| GRAPH 4.11 Node 6 — Package 4, 0.3.4-alpha-graph4.11 o superiore | versionCode 7 / versionName 0.3.4-alpha-graph4.11 / label POCO XIAOMI OWNER PACKAGE 4; aapt2-verified; APKs hashed | OWNER_POCO_PACKAGE_4_GRAPH411.md runbook |

| G4.13 §4–§7 app-wide accessibility audit + semantic authority | APP_WIDE_RENDERED_CONTRAST_AUDIT_GRAPH413.{json,md}; SemanticContrastAuthorityTest; LIVE 50 rows / 0 FAIL both themes | 62767b3 |
| G4.13 §5 disabled-state contract | Hermes control wrappers (Chrome.kt) + textDisabled/surfaceDisabled pins ≥3.0 + multi-cue rendering | 62767b3 |
| G4.13 §9 automated screenshot testing | ScreenshotMatrixTest (16 captures, gates) via E2E_CLASS_FILTER channel | 9ab2fa3 |
| G4.13 §10 physical PASS preservation | PhysicalPassPreservationTest (Interrompi/continuous/QR/Italian) | 9ab2fa3 |
| G4.13 §12–§27 Model Library rebuild | ModelManagerScreen + ModelUxComponents + ModelUxSemantics + ModelUxSemanticsTest; MODEL_LIBRARY_BEFORE_AFTER_GRAPH413.md | 7e0abc7, bd0f5af |
| G4.13 §29–§41 Tower refoundation | docs/visual/tower413/ (candidates, gates ALL PASS, INTEGRATION_MANIFEST, renders); ReforgedTowerVisual; OWNER_TOWER_VISUAL_PACKAGE_GRAPH413.md | d104f53 |
| G4.13 §42 physical retest package | OWNER_POCO_XIAOMI_RETEST_PACKAGE_6_GRAPH413.md + hashed APKs | 9d4f3d0 |
| G4.13 §44 version + APKs | 0.3.6-alpha-graph4.13-visual-accessibility / code 9; arm64 3b79b7c6…, universal da8a0ee1…, x86_64 a2e3cea8… | 9d4f3d0 |
| G4.13 §43 warning classification | LINT_WARNING_CLASSIFICATION.md addendum (0 errors / 82 warnings; 0 newly introduced) | 9d4f3d0 |
| G4.13 §46 terminality | GRAPH413_OWNER_VISUAL_ACCESSIBILITY_PACKAGE_READY reached; OWNER_VISUAL_RETEST_PENDING (physical grant Owner-only) | this convergence |

---

# GRAPH 4.14 ADDITIVE BLOCK — requirement evidence pins (this session)

| Req | Requirement (short) | Evidence | Status |
| --- | --- | --- | --- |
| G4.14-runtime | Every integrated runtime verified (identity/coords/ABI/minSdk/format/seams/fallback/no-false-ACTIVE) | GRAPH414_FINAL_RUNTIME_AUDIT.md + measured APK ZIP contents | VERIFIED_LOCAL |
| G4.14-llm | Local LLM honesty gate: no PASS from AAR presence; physical layers typed | GRAPH414_LLM_HONESTY_GATE.md; LlamaCppAndroidRuntimeTest + LocalLlmEngineTest | UNIT_TEST_EXECUTED — LLM_PHYSICAL_QUALIFICATION_PENDING |
| G4.14-bridge | Mock NEVER appears as real Tower (SIMULAZIONE amber always) | BridgeModeAuthorityTest + deleted legacy cyan pills + renders/* | UNIT_TEST_EXECUTED — device leg OWNER PENDING |
| G4.14-theme | Marble Light on EVERY route; contrast ≥4.5 text / ≥3.0 icons; no mixed chrome | GRAPH414_MARBLE_ROUTE_MATRIX.md + MARBLE_LIGHT_RENDERED_CONTRAST_REPORT (42 rows/0 FAIL) + ThemeAuthorityTest | UNIT_TEST_EXECUTED — rendered device leg OWNER PENDING |
| G4.14-library | Truthful product states (typed verdicts, one primary action, installed≠active, probe hidden) | GRAPH414_MODEL_LIBRARY_FINAL_REPORT.md + ModelUxSemanticsTest | UNIT_TEST_EXECUTED |
| G4.14-tower | Reference Tower integrated; §38 gates on SHIPPED pixels; 10-state visible animation; §40 mock-amber coherence | GRAPH414_TOWER_FINAL_ASSET_AUDIT.md (allPass) + contact sheet + animation strips | AUDIT_EXECUTED — ordinary-speed visibility OWNER PENDING |
| G4.14-selftests | §25: real runtime self-tests (no catalog-state PASS) | ST-58..61 + repaired ST-55..57 registration; AppGraph probes | UNIT_TEST_EXECUTED — device probes QUALIFICATION_PENDING_DEVICE |
| G4.14-package | 3 APK variants, version above the fresh tip, hashes + size classification | GRAPH414_APK_SIZE_AND_RUNTIME_REPORT.md + aapt2 badging | MEASURED |
