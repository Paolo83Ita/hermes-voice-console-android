# BLOCKER REGISTER — Hermes Voice Console Android

No active blocker to Graph 1 terminality as of 2026-11-01.

| ID | Item | Type | Impact | Resolution path |
| --- | --- | --- | --- | --- |
| B-0001 | Physical Xiaomi 11 Lite 5G NE not available to the executor during cloud nodes | EXPECTED_OWNER_DEFERRED | Device qualification (typed `QUALIFICATION_PENDING_DEVICE`) stays open | Owner runs `docs/INSTALL_XIAOMI.md`, then reports read-back for `docs/governance/CURRENT_PROJECT_TRUTH.md` |
| B-0002 | OEM unlocking enabled on device | OUT_OF_SCOPE | None — bootloader stays locked (INV-13) | No action; do not use |
| B-0003 | Real Tower API unknown | FOUNDATION_BASELINE | TowerBridge remains mock; real transport is V0.4 per roadmap | Owner provides real endpoint/credentials in a later graph; pairing designed first |

Resolved during Graph 1 (for the record):

| ID | Item | Resolution |
| --- | --- | --- |
| R-0001 | Gradle wrapper jar/scripts missing from Foundation scaffold | Wrapper generated with Gradle 8.9, committed (G-007) |
| R-0002 | 44 main-source + 9 test-source compile errors; 7 test failures | Repaired locally, tests 33/33 green (G-001..G-005) |
| R-0003 | Sandbox Java 21 was JRE-only (no jlink) | Standalone Temurin JDK 17 installed (G-002) |

No new blocker in Graph 4.13. Standing deferred items unchanged (B-0001
physical qualification runbook now = OWNER_POCO_XIAOMI_RETEST_PACKAGE_6_GRAPH413.md;
B-0003 real Tower API still V0.4). OWNER_VISUAL_RETEST_PENDING is a
pending OWNER ACTION, not an executor blocker.
