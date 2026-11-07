# OWNER PHYSICAL FINDINGS — GRAPH 4.12 (§2 receipt)

CP-G4.12-001 component. This file records the Owner's physical verdict on
**POCO OWNER PACKAGE 4** (versionName `0.3.4-alpha-graph4.11`, versionCode `7`,
tested APK SHA-256 `cf630af8…eda80d7`) and binds it to the repair contract.
Every verdict below is Owner-granted; no self-grading is performed here.

## 1. Devices

| Device | RAM | Storage | Role |
| --- | --- | --- | --- |
| POCO X6 Pro | 8 GB | 256 GB | primary qualification device |
| Xiaomi 11 Lite 5G NE | 8 GB | 128 GB | compact-device qualification device |

## 2. Physical passes (Owner evidence)

### PHYS-PASS-01 — Local responses are finally Italian

The Graph 4.10 ResponseLocaleResolver + ResponseCatalog repair chain is
physically confirmed: under the Italian UI the assistant's local responses no
longer come back in English. This closes the Graph 4.10 P0 language node as
**OWNER-VERIFIED PASS** on both devices.

### PHYS-PASS-02 — Continuous-listening mode functions

The Graph 4.10 ListeningModePolicy + ContinuousListeningController chain is
physically confirmed working on the device. Preliminary pass: Graph 4.12 §17
still hardens startup-after-onboarding, TTS pause/resume, echo rejection and
session/thermal stops, but the core loop is physically alive.

## 3. Physical defects (Owner evidence)

### PHYS-FAIL-01 — Crash on first launch after onboarding completes

- Severity: **P0** (blocks every other flow on a fresh install).
- Simulator corroboration (this read-back): E2E run 42382864925 lanes both
  `E2E_VERDICT_FAILED`; regression FreshInstall / SelfTest / residual
  appearance-switch all report `Process crashed`;
  `IllegalStateException: State must be at least CREATED to move to DESTROYED,
  but was INITIALIZED … NavBackStackEntry … route=settings` — the
  navigation-vs-recreation window that §5 forbids.
- Repair owner: §3 crash-evidence system + §4 FIRST_RUN_SAFE_MODE + §5 atomic
  onboarding completion contract + §6 fresh-install matrix.

### PHYS-FAIL-02 — Insufficient foreground/background text contrast

- Severity: P0 for accessibility.
- Repair owner: §18 token-level contrast audit + §19 semantic color tokens +
  §20 screen/theme/font-scale matrix.

### PHYS-FAIL-03 — Tower derived from the Owner image visually corrupted

- Severity: P1 (highest visual priority; the package is physically rejected).
- Read-back confirms all ten rejected layers still ship in the default path
  (tower_rear/sign/outer_ring/middle_ring/inner_ring/portal/lower_gate/
  stairs/foreground/energy).
- Repair owner: §26 retirement + §27 original reconstruction + §28 candidate
  gate + §29 layer contract + §30 validation + §34 visual retest package.
- State after repair remains `OWNER_VISUAL_RETEST_PENDING` (Owner-only accept).

### PHYS-FAIL-04 — `Interrompi` near `Parla` appears non-functional

- Physical confirmation still required by the Owner; treated as defect until
  proven otherwise.
- Read-back: no unified cancel path exists in code (no InterruptReceipt type,
  no per-subsystem cancel aggregation).
- Repair owner: §13 unified interrupt contract + §14 InterruptReceipt +
  §15 state-specific labels + §16 physical tests (final-state read-back
  provided to the Owner so the next verdict is decidable).

### PHYS-FAIL-05 — Downloaded models/voices not proven consumed by the runtime

- Severity: P0 (truth gap between UI selection and runtime identity).
- Read-back: VoiceSelectionAuthority exists, but the capability ladder skips
  INSTALLED→ACTIVE and no per-component runtime receipt exists.
- Repair owner: §7 capability state machine + §8 runtime receipts +
  §9 Vosk fixture/mic test + §10 Sherpa/Piper function test + §11 LLM truth.

## 4. Current terminal state

```
OWNER_PHYSICAL_RETEST_FAILED_REPAIR_REQUIRED
```

Next terminal boundary targeted by this mission:
`POCO_XIAOMI_PHYSICAL_REPAIR_PACKAGE_READY` → then Owner-only
`POCO_XIAOMI_STABILITY_MODELS_CONTROLS_CONTRAST_AND_VISUAL_RETEST_PASSED`.

## 5. Governance ledger updates required by this receipt

| Ledger | Update |
| --- | --- |
| CURRENT_PROJECT_TRUTH | Graph 4.12 truth section appended after the repair lands (additive, no history rewrite). |
| WORK_LOG | Graph 4.12 physical findings + repair chain entries. |
| CHECKPOINT_INDEX | CP-G4.12-001..010 rows. |
| REQUIREMENT_TO_EVIDENCE_REGISTER | R-entries for §3..§34 requirements bound to receipts/tests. |
| BLOCKER_REGISTER | PHYS-FAIL-01..05 as tracked blockers with repair owners. |
| OWNER_UI_LAYOUT_DEFECT_LEDGER | contrast (PHYS-FAIL-02) + Tower corruption (PHYS-FAIL-03) rows appended. |

Forbidden tokens remain UNCLAIMED (grader never self-grants):
`POCO_XIAOMI_…_RETEST_PASSED`, `RESPONSIVE_UI_QUALIFIED`,
`REAL_TOWER_PAIRING_PASSED`, and all physical PASS tokens.

Typed carry-forward (unchanged, not expanded this mission):
`LOCAL_LLM_RUNTIME_UNAVAILABLE`, `REAL_TOWER_PENDING_GRAPH_1_7`,
`REAL_ARCHIVE_PENDING_TOWER_API`.
