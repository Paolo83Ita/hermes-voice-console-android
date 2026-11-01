# PROJECT OPERATING CONTRACT — Hermes Voice Console Android

Graph: `HVC_GRAPH_1_PRIVATE_REPOSITORY_CLOUD_BUILD` v1.0 · Owner: Paolo Lombisani · Executor: GLM 5.3 Flash
Status: ACTIVE (2026-11-01). This file is the permanent operating authority for graph work on this project.

## 1. Mission invariants (summary)

Full register: `INVARIANT_REGISTRY.md`. Non-negotiables: private repo (INV-01), only the
authorized repository is touched (INV-02), no token ever enters files/history/URLs (INV-03),
no production signing key (INV-04), no public releases/artifacts (INV-05/06), no automatic
merge into `main` (INV-07), no force-push / history rewrite (INV-08/09), no remote branch
deletion without Owner approval (INV-10), no unrelated repositories (INV-11).

## 2. Authority precedence (when information conflicts)

1. Most recent explicit Owner decision
2. Verified private remote repository truth
3. Physical local workspace truth
4. Executed cloud-build truth
5. Executed test truth
6. Physical target-device read-back
7. Latest verified commit / checkpoint
8. Mission Graph (`MISSION_GRAPH.yaml`)
9. Requirement-to-Evidence Register
10. Invariant Registry
11. Decision Log
12. Architecture and requirements docs
13. Narrative summaries
14. Unverified assumptions

Narrative claims never override physical evidence.

## 3. Typed status vocabulary

UNKNOWN, ASSUMED_PENDING_VERIFICATION, NOT_FOUND, TEST_WRITTEN_NOT_EXECUTED,
IMPLEMENTED_NOT_TESTED, BUILD_NOT_AVAILABLE, BUILD_FAILED, QUALIFICATION_PENDING_DEVICE,
BLOCKED_ENVIRONMENT, BLOCKED_OWNER_INPUT, BLOCKED_DEPENDENCY.

Evidence classes (never collapse into a generic PASS): SOURCE_CREATED, SOURCE_STATICALLY_REVIEWED,
DOCUMENT_CREATED, DIAGRAM_SOURCE_CREATED, DIAGRAM_RENDERED, VISUAL_PREVIEW_CREATED/RENDERED/INSPECTED,
ACCESSIBILITY_CHECK_EXECUTED, MOCK_SIMULATION_PASSED, UNIT_TEST_WRITTEN, UNIT_TEST_EXECUTED,
ANDROID_LINT_EXECUTED, CLOUD_WORKFLOW_CREATED, CLOUD_WORKFLOW_EXECUTED, CLOUD_BUILD_PASSED,
CLOUD_BUILD_FAILED, APK_CREATED (only for cloud-built APKs), APK_HASHED, APK_BOUND_TO_COMMIT,
APK_INSTALLED, DEVICE_READBACK_VERIFIED, DEVICE_FUNCTION_TEST_PASSED, TOWER_MOCK_INTEGRATION_PASSED,
TOWER_REAL_INTEGRATION_PASSED, OWNER_ACCEPTED.

A node is not COMPLETE when: required compilation did not occur; mandatory tests were not
executed; only mock behavior exists where real behavior is required; implementation exists
only in documentation; an APK was claimed but not physically produced; device qualification
remains outstanding; documentation materially disagrees with implementation.

## 4. Node execution loop

READ → PLAN DELTA → PRECHECK → IMPLEMENT → STATIC VERIFY → TEST → REVIEW → REPAIR →
PERSIST → CHECKPOINT → RELEASE DEPENDENCIES → CONTINUE.

One active Node Context Bundle at a time (node ID, objective, dependencies, requirements,
decisions, files, invariants, expected output, tests, evidence, attempt budget, stop
condition, previous checkpoint, Next Exact Action) — recorded in `WORK_LOG.md`.

## 5. Attempt budgets (defaults)

- Local source repair: max 3 materially different attempts.
- Repository authentication: max 2 attempts.
- Remote push: max 2 attempts.
- Cloud-build repair: max 3 materially different build iterations.
- Dependency resolution: one standard attempt + one bounded retry.
- Workflow polling: conservative intervals, no polling storm.

Never repeat an identical failed action; every retry changes at least one material factor.

## 6. Logical roles

MAKER creates/modifies implementation; CHECKER verifies syntax/paths/imports/resources/
consistency; VALIDATOR verifies requirements/architecture/security/evidence; JUDGE assigns
typed verdicts; OWNER (Paolo Lombisani) retains final authority for material UX, repository
merge to main, physical installation, real Tower integration, and consequential decisions.
A Maker claim is not sufficient evidence of completion.

## 7. Project-specific prohibitions (standing)

- No bootloader unlock, no root, no custom recovery, no custom ROM. OEM unlocking is enabled
  on the device but is out of scope.
- No ADB exposure over Tailscale; no remote shell; no unrestricted ADB.
- No device identifiers (IP, MAC, IMEI, serial, Android ID) committed anywhere.
- No secrets, no Tower tokens, no production endpoints in the repository.
- Hermes Tower remains the sole authority; the local model must never become a second one.
- No fabricated runtime data in the UI; the 15-state machine renders only real console state.
- Foundation-era records (docs/*.md) are preserved; governance files extend, never duplicate
  or overwrite them.

## 8. Token hygiene (GRAPH 1)

The GitHub token is Owner-supplied at runtime, used transiently for authorized operations on
`Paolo83Ita/hermes-voice-console-android` only. It must never appear in chat prose, source,
docs, Git history, workflows, reports, ZIPs, receipts, or the Rehydration Pack, and never in
the persistent Git remote URL.
