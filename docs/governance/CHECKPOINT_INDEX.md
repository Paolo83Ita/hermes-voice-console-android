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
