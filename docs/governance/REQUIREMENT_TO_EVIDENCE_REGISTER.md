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
