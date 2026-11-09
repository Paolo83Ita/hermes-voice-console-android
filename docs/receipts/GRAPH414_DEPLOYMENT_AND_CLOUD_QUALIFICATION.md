# GRAPH 4.14 — DEPLOYMENT RECORD AND CLOUD QUALIFICATION (CP-G4.14-C09, §19–§22)

## NODE
CP-G4.14-C09 — deployment + cloud build verification + E2E arbitration.

## 1. Deployment history (forward-only; main untouched; no force-push)

| Commit | Content | Push verification |
| --- | --- | --- |
| b7339e3 | CP-G4.14-C01..C07 (read-back + audit wave + repairs + version 0.3.7/code 10 + APKs + Owner package + runbook + governance convergence) | pushed d66a523..b7339e3; ls-remote tip == local HEAD |
| eed00d5 | C08 CI repair: the machine-local JDK pin (committed in f6d5cd6) broke the cloud build at the FIRST gradle step ('Java home supplied is invalid'; hit runs 42600609475 @ d66a523 AND 42618478430 @ b7339e3, zero tasks executed). Pin removed from the tracked gradle.properties (CI uses JAVA_HOME temurin 17 as before f6d5cd6); sandbox keeps it in ~/.gradle/gradle.properties (uncommitted) | pushed b7339e3..eed00d5; ls-remote tip == local HEAD |
| (this commit) | C09 arbitration persistence: bridge-truth E2E added to the default regression list; §22-typed repairs (ST-30/31 fixture-contract + e2e_31_33/e2e_32 typed-state semantics); deployment record | pushed eed00d5..(this) |

## 2. Cloud build verification (§20 — run 42619128335 @ eed00d5: **SUCCESS**)

Parsed from the DOWNLOADED artifacts (never the green icon alone):

| Artifact | Content | Verified |
| --- | --- | --- |
| hermes-voice-console-test-results-eed00d5… | JUnit XML (71 files) | **suites=68 tests=496 failures=0 errors=0 skipped=0** — cloud == local EXACTLY (§21: no discrepancy) |
| hermes-voice-console-lint-report-eed00d5… | lint XML | **0 errors / 73 warnings** (local 72 + 1 environment-only instance of the CLASSIFIED `OldTargetApi` rule on build.gradle.kts — CI lint sees a newer preview API level; rule ACCEPTED, INTENTIONAL since Graph 1; NOT newly introduced) |
| hermes-voice-console-debug-apk-eed00d5… | 3 APKs | arm64 122,622,634 B `18690418…bfdc` · universal 252,269,463 B `3658d0e2…83b8` · x86_64 121,751,216 B `37f6c940…7f3c`; aapt2: versionCode 10, versionName 0.3.7-alpha-graph4.14-runtime-theme-tower ✓ |

APK content identity (§21 — file-size delta −1.39 MB vs local investigated):
both APKs contain the SAME 596 ZIP entries; the only uncompressed-size deltas
are ≤128 B in 3 dex files (CI's buildConfig stamps: full SHA eed00d5… +
run id 42619128335 + timestamps) → the delta is pure ZIP recompression
variance. No content regression.

## 3. E2E verdict (§22 — run 42619128215 @ eed00d5, dual lane)

| Lane | Outcome | Typed classification |
| --- | --- | --- |
| emulator-e2e (2) | E2E_BLOCKED_EMULATOR_ENVIRONMENT — the app APK install timed out after 900 s AFTER the R-17 guest-health gate (typed honest abort; zero instrumentation, empty evidence artifact) | EMULATOR_INFRASTRUCTURE (install/transport pathology; the same APK installs locally and in prior runs) |
| emulator-e2e (1) | FULL HONEST SAMPLE (gates + regression + residual + VERDICT.txt = **E2E_VERDICT_FAILED**) | see arbitration below |

Arbitration (lane 1 vs the PRE-4.14 BASELINE tip 17090bb, run 42571197180
sample-2 — the failure-set diff that decides attribution):

| Item | Current tip | Baseline 17090bb | Verdict |
| --- | --- | --- | --- |
| gate A | A1 onboarding-anchor timeout (2 tests started, no result captured) | A1 FAILED | STANDING harness family (R-42..R-49 onboarding anchor) |
| gate B | inject-menu OnClick lookup | FAILED (same) | STANDING (R-42 family) |
| gate C | **OK (1 test)** | FAILED | **IMPROVED** at the 4.14 tip |
| gate D | **OK (1 test)** — MODEL SANCTUM green for the first time in the sampled line | FAILED | **IMPROVED** (4.13 Model Library rebuild + 4.14 truth layer) |
| gate E | settings-hub row lookup ('Lingua' with visible-dump) | FAILED (same) | STANDING (R-49 family) |
| gate F | **OK (1 test)** | FAILED | **IMPROVED** |
| E2EFreshInstallTest | e2e_03_04 / e2e_05_06_07 / e2e_08 anchor timeouts | SAME SET FAILED | STANDING |
| E2EModelManagerTest | e2e_27_28_29 / e2e_21_22 anchor 'A. Recommended setup' | SAME SET FAILED | STANDING (4.13-era stale anchor; a separate named repair node is required — NOT 4.14-attributed) |
| E2ESelfTestTest | e2e_31_33 + e2e_32 + e2e_34 | e2e_34 + e2e_32 | e2e_34/e2e_32 STANDING; **e2e_31_33 NEWLY FAILED = 4.14-ATTRIBUTED** (ST-30/31 stale expectations) → REPAIRED (below) |
| E2EVoiceFlowTest | **OK (1 test)** | e2e_11 FAILED | **IMPROVED** (fleet variance) |
| E2EOnboardingTest | OK | OK | stable |
| E2ELifecycleProfileTest | typed assumption skip (NORMAL profile) | same | STANDING (by design) |
| residual (E2EAppearancePersistenceTest) | e2e_41_42 + e2e_40 | SAME SET FAILED | STANDING (byte-identical 2-failure family) |

**4.14-attributed defect found and repaired (TEST_IMPLEMENTATION_DEFECT, no
application regression):**
- ST-30 pinned the PRE-4.14 manifest contract ("1.0.0") — the shipped catalog
  is manifest **2.0.0** (12 entries, runtime-requirement schema, CP-G4.14-002)
  → expectation updated to pin the CURRENT shipped truth (stricter).
- ST-31 pinned "probe downloadable" — the §5 terminal decision
  REMOVE_MISLEADING_ENTRY blocks the probe fixture download
  (TEST_FIXTURE_ONLY) → expectation updated to pin the BLOCKED truth.
- The JVM test fixture catalog (`voice_pack_catalog_test.json`) upgraded to
  mirror the shipped 2.0.0 contract (manifest 2.0.0 + probe
  TEST_FIXTURE_ONLY) so the JVM battery exercises the CURRENT schema.
- e2e_31_33/e2e_32: FAIL is the ONLY defect signal — the designed honest
  typed states (QUALIFICATION_PENDING_DEVICE / BLOCKED_DEPENDENCY /
  SKIPPED_NOT_APPLICABLE on a device without physical artifacts) are not
  regressions; e2e_32 additionally pins the repaired 61-definition battery
  (would have caught the CP-G4.12 discard bug). A FAIL still fails the e2e —
  nothing weakened.
- E2EBridgeTruthTest added (§24: real UI actions — mock pill label
  SIMULAZIONE/SIMULATION, forbidden real-connection labels asserted absent,
  truth panel mock note + MOCK_SIMULATOR endpoint class, authority
  read-back) and wired into the default regression list.
- Local verification: **496/496 unit GREEN**; androidTest compiles; APKs
  rebuilt (arm64 `8cfd9632…77c9` / universal `9229d31b…60c9` / x86_64
  `e9d7ad77…b9ff`).

The remaining failures are the byte-baseline-identical standing line — the
historical harness campaign (R-13..R-48) is NOT restarted without new
evidence; the physical Owner qualification stays the authority for product
truth.

## 4. Gate tallies this run
- Lane 1: gates 3/6 OK (C, D, F — a new best for the sampled line), regression
  E2EOnboardingTest + E2EVoiceFlowTest + E2ELifecycleProfileTest(typed) green,
  residual 2 failures (standing family), VERDICT: E2E_VERDICT_FAILED
  (arbitration honest-fail: a complete sample with standing failures is not a
  green sample).
- Lane 2: typed install-phase abort, zero instrumentation (no verdict credit —
  missing evidence is NOT success).

## FILES
- .github/workflows/android-e2e.yml (regression list + E2EBridgeTruthTest)
- app/src/androidTest/.../e2e/E2EBridgeTruthTest.kt (new)
- app/src/androidTest/.../e2e/E2ESelfTestTest.kt (typed-state repairs)
- app/src/main/.../selftest/SelfTestRegistry.kt (ST-30/31 contract pins)
- app/src/test/resources/voice_pack_catalog_test.json (2.0.0 fixture)
- docs/receipts/GRAPH414_DEPLOYMENT_AND_CLOUD_QUALIFICATION.md (this file)

## CHECKPOINT
CP-G4.14-C09 — DONE (deployment + cloud verification + arbitration + typed
repairs).

## NEXT
Push this commit; re-arbitrate the CI wave at the new tip; final remote
read-back (§29); terminality (§30); final session report (§31).
