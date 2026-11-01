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
