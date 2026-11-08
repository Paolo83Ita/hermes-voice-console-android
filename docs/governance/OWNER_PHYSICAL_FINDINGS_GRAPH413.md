# OWNER PHYSICAL FINDINGS — GRAPH 4.13 (§0 + §3 receipt)

CP-G4.13-001 component. This file records the Owner's physical verdict that
opens Graph 4.13 and binds it to the repair contract. Every verdict below is
Owner-granted evidence; no self-grading is performed here. Canonical states are
quoted verbatim from the Owner instruction.

## 1. Devices

| Device | RAM | Storage | Role |
| --- | --- | --- | --- |
| POCO X6 Pro | 8 GB | 256 GB | primary qualification device (product label `2311DRK48G` — advanced diagnostics only per §25) |
| Xiaomi 11 Lite 5G NE | 8 GB | 128 GB | compact-width qualification device |

Tested build: POCO OWNER PACKAGE 4 ARM64 APK SHA-256 `cf630af8…eda80d7`
(versionName 0.3.4-alpha-graph4.11, versionCode 7). Package 5 (0.3.5 / code 8)
repair-verified in CI; visual states below carry over as open defects.

## 2. Preliminary physical passes (Owner evidence — PRESERVE, §0/§10)

| Canonical state | Function |
| --- | --- |
| `ITALIAN_RESPONSE_PHYSICAL_PASS_PRELIMINARY` | local responses render in Italian |
| `CONTINUOUS_LISTENING_PHYSICAL_PASS_PRELIMINARY` | continuous listening functions |
| `UNIFIED_INTERRUPT_PHYSICAL_PASS_PRELIMINARY` | unified Interrompi control works |
| `QR_SCANNER_PHYSICAL_OPEN_PASS` | QR scanner opens physically |

Repair contract: preserve; regression-test; modify only if directly required by
the visual or accessibility repair; do not introduce new architecture around
them. §10 adds the regression gates (Interrompi label states, listening
lifecycle/echo, QR open path + camera permission + scanner layout, Italian
response catalog).

## 3. Open physical failures (Owner evidence — REPAIR, this graph)

| Canonical state | Repair surface |
| --- | --- |
| `CENTRAL_TOWER_VISUAL_OWNER_REJECTED` | §29–§41 Tower refoundation (Block 3) |
| `APP_WIDE_TEXT_CONTRAST_FAIL` | §4–§7 audit + semantic color authority (CP-G4.13-002/003) |
| `MODEL_LIBRARY_READABILITY_FAIL` | §12–§27 Model Library rebuild (Block 2) |
| `MODEL_PROFILE_LAYOUT_FAIL` | §14–§16 config card + profile selector (Block 2) |
| `COMPACT_WIDTH_ACTION_LAYOUT_FAIL` | §21–§23 action semantics + layout (Block 2) |
| `OWNER_VISUAL_AND_ACCESSIBILITY_RETEST_REQUIRED` | terminality — Owner-only grant after Package 6 (§42, §46) |

## 4. Screen findings recorded verbatim-class (Owner screenshots, §3)

### SCREEN — Models and Voice, configuration card
- device information almost black on a dark panel (detected device, ABI, RAM,
  storage, TTS engine, network hard to read);
- profile buttons forced into one row; "Local experimental" wraps vertically;
- descriptive text at the bottom low contrast;
- excessive empty space in the card.

### SCREEN — Models and Voice, catalog cards
- model metadata low-contrast gray;
- classification/state rendered as raw enum values
  (VERIFIED_DOWNLOADABLE / DISCOVERED / ACTIVE / FUNCTIONAL_TEST_PASSED /
  RUNTIME_AVAILABLE / QUALIFY_ON_DEVICE / NOT_INSTALLED);
- "Compatibilità: false" without a plain-language reason;
- action buttons visually ambiguous; Verify prominent even when another action
  should be primary; weak title/metadata hierarchy;
- repeated "Altre azioni" labels add vertical noise; long rows create scanning
  fatigue.

### SCREEN — Console
- current Central Tower clean but aesthetically rejected: flat technical
  scaffold, no monumental architectural mass, no depth/rich material/visual
  identity, too geometric and diagrammatic; central oracle mechanism too small
  relative to the structure; large upper void; thin disconnected columns; sign
  visually detached; significantly inferior to the Owner reference
  (`upload/Immagine_Hermes_Tower.png`).

## 5. Binding consequence

- Graph 4.12 Tower: `CENTRAL_TOWER_GRAPH412_VISUAL_OWNER_REJECTED` — minor
  polish as the only solution is forbidden; three distinct candidates required
  (§34), Owner-reference fidelity first.
- All contrast/UX repairs must preserve the §2 preliminary physical passes and
  the 450-test unit authority (no test deleted, skipped or weakened).
- Terminality stays `OWNER_VISUAL_AND_ACCESSIBILITY_RETEST_REQUIRED` until the
  Owner physically retests Package 6 and grants
  `OWNER_VISUAL_ACCESSIBILITY_AND_MODEL_LIBRARY_RETEST_PASSED`. State at
  implementation boundary: `GRAPH413_OWNER_VISUAL_ACCESSIBILITY_PACKAGE_READY`
  with `OWNER_VISUAL_RETEST_PENDING` — no invented PASS.
