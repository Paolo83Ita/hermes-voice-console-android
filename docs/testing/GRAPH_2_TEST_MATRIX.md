# GRAPH 2 TEST MATRIX — requirements × evidence

Evidence classes are typed (no merged PASS). Cloud runs bind results to commit SHA.

| Area | JVM unit tests | In-app self-tests | Instrumented/E2E |
| --- | --- | --- | --- |
| Model catalog + manifest | VoiceModelManagerCoreTest (10) | ST-30, ST-31 | E2E-21, E2E-22 |
| Download/verify/install/rollback | ModelDownloadLifecycleTest (9) | ST-32..ST-40 | E2E-24..E2E-30 |
| Profile validation | TowerConnectionStudioTest (7 validator tests) | ST-02, ST-04 | E2E-05..E2E-07 |
| Connection tester outcomes | TowerConnectionStudioTest (6 tester tests) | ST-05..ST-12 | E2E-09, E2E-10 |
| Secret vault | — (interface JVM) | ST-03 | KeystoreVaultInstrumentedTest (4) |
| Profile store/export/import | TowerConnectionStudioTest (6 store tests) | ST-27..ST-29 | E2E-07, E2E-08, E2E-43, E2E-44 |
| RealTowerBridge adapter | TowerConnectionStudioTest (5 bridge tests) | ST-05..ST-10 | E2E-09, E2E-12, E2E-13 |
| Migration V0.1→V0.2 | SettingsMigrationTest (9) | ST-45 | E2E-41, E2E-42 |
| State machine + modes | Graph 1 suite (33) + ST-13..21 | ST-13..ST-21 | E2E-11..E2E-20, E2E-39 |
| UI semantics/persistence | — | ST-22..ST-26, ST-41..ST-44 | E2E-35..E2E-45 |
| Redaction | LogRedactorTest (4) + ST-28/41 | ST-28, ST-41 | secret-clean evidence scan (workflow) |

## Gate mapping

GATE-02 model-manager tests (lifecycle + core + ST-30..40) · GATE-03 tower studio tests ·
GATE-04 KeystoreVault instrumented · GATE-05 Self-Test Center suite · GATE-06 all JVM
unit tests · GATE-07 lintDebug (executed, non-blocking, report archived) · GATE-08
assembleDebug · GATE-09 connectedDebugAndroidTest (all instrumented) · GATE-10 Compose UI
tests (within the E2E classes) · GATE-11 complete E2E pass · GATE-12 APK + SHA-256 ·
GATE-13 this register current · GATE-14 secret-clean scan · GATE-15 Terminal Receipt.
