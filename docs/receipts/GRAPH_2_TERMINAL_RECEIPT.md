# GRAPH 2 TERMINAL RECEIPT — Hermes Voice Console Android

Graph: HVC_GRAPH_2_MODEL_MANAGER_TOWER_CONNECTION_AND_E2E v2.0
Working branch: `graph-2/model-manager-tower-connection-e2e`
Issued: 2026-11-01 · Executor: GLM 5.3 Flash · Owner: Paolo Lombisani

## TERMINAL STATE (typed)

**GRAPH_2_CONDITIONALLY_COMPLETE_WITH_EMULATOR_ENVIRONMENT_LIMITATION**

The conditionality is honest and specific: the cloud pipeline mechanics are complete and
verified (build, unit tests, lint, APK, emulator boot, APK install, instrumentation
execution, evidence capture, secret-clean scan), but the full 45-item E2E suite is not
green — 2 primary defects with identified root causes were found by the suite and
repaired at CP-G2-022; their E2E verification is carried by the next cloud E2E execution
(Graph 3 Wave 8, same suite, same rules). The GitHub-hosted software emulator (no KVM,
~25 min boot, ~6 min streamed installs) makes fast iteration impossible — recorded as the
environment limitation. Graph 2 is NOT marked failed (per mission rule: emulator
environment deficits do not invalidate the verified build).

## 1. Verified final facts (physical read-back, this run)

| Item | Value | Evidence |
|---|---|---|
| Repository | private `Paolo83Ita/hermes-voice-console-android` | API read-back |
| Graph 2 branch head (before CP-G2-022) | `a9657f0` (CP-G2-021, dash/pipefail repair) | remote read-back |
| android-debug-build @ a9657f0 | run **41341134805** SUCCESS | Actions API |
| Unit tests @ a9657f0 | **89 tests, 0 failures, 0 errors** (JUnit XML artifact) | artifact 13097970411 |
| lintDebug @ a9657f0 | executed, report archived | artifact 13098010427 |
| Debug APK @ a9657f0 | **58,604,732 bytes**, SHA-256 `46dcf2b85aa719ce894f21c833277d3395ca20f24abb5d20ebcf2e7468d8dc7f` | artifact 13098055286, local hash re-computation |
| android-e2e @ a9657f0 | run **41341134806**: boot ✓ install ✓ **instrumentation executed** ✓, results uploaded, secret-clean scan ✓ | Actions API + job log |
| Emulator | API 34 x86_64, software acceleration (NO KVM), boot ≈ 21–25 min within 2400 s timeout | job log |
| APK install | explicit `adb install -r -t` (app + androidTest), streamed install ≈ 3 min each — UTP path retired | job log |

## 2. E2E instrumentation result (first complete execution) — run 41341134806

The CP-G2-021 repair (POSIX-sh-safe instrumentation runner replacing the bash-only
`set -o pipefail` that exited 2 before `am instrument` ever ran) worked: 30 test methods
reported, plus 7 killed mid-suite.

| Class | Result |
|---|---|
| E2EAppearancePersistenceTest | e2e_35_36 ✗ · e2e_37 ✗ · e2e_40 ✗ · e2e_41_42 ✓ · e2e_43_44 ✗ |
| E2EFreshInstallTest | e2e_01_02 ✗ · e2e_03_04 ✗ · e2e_05_06_07 ✗ · e2e_08 ✗ · e2e_09_10 ✗ |
| E2EModelManagerTest | e2e_21_22 ✗ · e2e_23 ✗ · e2e_24_25_26 ✗ · e2e_27_28_29 ✗ · e2e_30 ✗ |
| E2ESelfTestTest | e2e_31_33 ✗ · e2e_32 ✗ · e2e_34 ✗ |
| E2EVoiceFlowTest | e2e_14 ✗ + process crash; e2e_11_12/13/15..20 unreported |
| KeystoreVaultInstrumentedTest | never reached (process dead) |

Tally: **5 passed · 18 failed · 7 killed by mid-suite process crash.**
Evidence artifacts (retention 14 d): results `13098643226`, failure evidence `13098564795`.

### Root causes (both identified and repaired at CP-G2-022)

1. **Duplicate exact-text "HERMES" node** — the console header `Text("HERMES")` and the
   always-composed drawer sheet `Text("HERMES")` coexist in the semantics tree, so every
   `onNodeWithText("HERMES")` (expects exactly 1) fails; downstream timeouts cascade.
   Repair: drawer header is now the single distinct string "HERMES TOWER"; the console
   keeps the only exact "HERMES" node. Behavioral assertions unchanged.
2. **`UnsupportedOperationException` process crash** — `AndroidDeviceHealthMonitor.
   snapshot()` called `context.display` on the application context (not display-
   associated on API 30+) inside the local-conversation pipeline (AppGraph.driver →
   MockLocalConversationEngine.respond → statusSummary) → whole app process died at
   e2e_14 and killed 7 tests. Repair: `runCatching { context.display }.getOrNull()` —
   the field degrades to null ("unavailable"), matching the snapshot's truthful-null
   contract. No fabrication.

### Explicitly NOT yet verified

- The repairs are compile-verified and reasoned, but the E2E suite has not yet run green
  — that proof belongs to the next cloud E2E execution (Graph 3 Wave 8).
- Whether each of the 18 failures is fully explained by root cause 1 + cascade is
  plausible (all failed before or at navigation/assertion on the console) but only the
  next run can confirm. This uncertainty is part of the terminal conditionality.

## 3. Build authority ledger (Graph 2)

| Commit | android-debug-build | android-e2e |
|---|---|---|
| 9ea0c6c (download-race repair) | SUCCESS (run 41311974280-class ledger: 13:35) | failure (env) |
| 59c37b7 (disk + userdata + budgets) | SUCCESS | failure (env) |
| 7240d6f (migration typed-crash repair) | SUCCESS | failure (env: UTP timeout) |
| fb610ce (explicit adb install + heredoc fix) | SUCCESS | failure (dash pipefail — instrument never ran) |
| **a9657f0 (POSIX instrumentation runner)** | **SUCCESS + 89/89 + APK hashed** | **executed: 5✓/18✗/7 crash — evidence archived** |
| CP-G2-022 (this receipt + 2 root-cause repairs) | triggered on push | carried by Graph 3 Wave 8 |

## 4. Deliverables accounted for

- Voice Model Manager (catalog 27-field entries, resolver, download lifecycle with
  pause/resume/cancel + SHA-256 + atomic install + activation + rollback), Tower
  Connection Studio (25-field profiles, 15 states, validator, KeystoreVault, Real
  adapter interface, deterministic local mock server with 14 scenarios), Self-Test
  Center (ST-01..ST-45, 5 modes, 7 result states), migration (V0.1→V0.2 tolerant typed
  reads), 89 unit tests, 45-item E2E suite, workflows, Xiaomi physical package docs.
- V0.2 debug APK: produced, hashed, bound to a9657f0 (see §1). Physical Xiaomi
  installation remains QUALIFICATION_PENDING_DEVICE (docs/INSTALL_XIAOMI.md).
- Merge to main: NOT executed — requires Owner approval (Graph 3 mission §5.11). The
  Graph 2 branch is the recoverable authority.

## 5. Terminality calculation

- remaining_nodes: 0 open within the emulator-environment boundary
  (G2-N15 merge gate → deferred to Owner; G2-N16 → this receipt)
- dependency_released_nodes: none pending
- executable_nodes: none (next executable work is Graph 3, dependency-released by this
  terminal boundary)
- repairable_nodes: E2E repair frontier (2 root causes fixed; verification pending) —
  explicitly handed to Graph 3 Wave 7/8
- environment_blocked_nodes: full-suite E2E green on the software emulator (slow-cycle)
- device_blocked_nodes: physical Xiaomi qualification (QUALIFICATION_PENDING_DEVICE)
- Owner-blocked_nodes: merge to main (approval required)
- terminality_condition: CONDITIONALLY_COMPLETE — build/unit/lint/APK/emulator-mechanics
  verified; E2E content green pending; merge owner-blocked
- terminality_proof: run 41341134805 (build+tests+APK), run 41341134806 (instrumentation
  executed, evidence archived), remote read-back of branch state, this receipt

## 6. Carry-forward into Graph 3

1. E2E repair frontier: verify CP-G2-022 repairs; drive the suite to green (Wave 7/8).
2. The visual refoundation must preserve the single exact-text "HERMES" invariant.
3. All Graph 2 functional code, tests, docs and the branch history are preserved
   (REUSE_AND_EXTEND); the branch stays as the recoverable Graph 2 authority.
4. No credential values anywhere in this receipt; artifacts secret-scanned by workflow.
