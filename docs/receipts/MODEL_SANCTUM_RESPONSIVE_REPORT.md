# MODEL_SANCTUM_RESPONSIVE_REPORT.md

- Mission: GRAPH 4.9 — CP-G4.9-006 (Model Sanctum and voice-control qualification)
- Owner: Paolo Lombisani
- Written: 2026-11-07
- Preserve-order: the CP-G4-029 Verify-button repair (AdaptiveActionsRow) is
  PRESERVED — nothing in this node re-touched its layout policy.

## 1. The adaptive Verify-button repair (preserved, re-proven)

`ui/components/AdaptiveActionsRow.kt` implements the targeted repair for
PHYSICAL-FAIL-03:

- primary actions (Download / Verify-filled / Activate / Details) render
  directly; everything else sits behind **"Altre azioni"** (overflow, never
  clipped);
- ≥ 48 dp touch height, 12 dp content padding, maxLines 2, centered text,
  no icon/progress overlap with the label;
- WIDE (≥ 480 dp available width): one row; CONSTRAINED: FlowRow wrap;
- disabled actions stay legible (alpha ≤ 0.38 on the CONTAINER, text alpha
  intact); progress state keeps the label next to the spinner (never hidden);
- `AdaptiveActionPolicy` is unit-tested (`AdaptiveActionPolicyTest`) — the
  policy is pure, so the width/font decisions are provable on the JVM.

This node ADDS two actions to the layout through the SAME policy (no layout
code changed): the STT fixture **Test** action and the natural-voice
**Hear sample** action (both secondary, both behind the adaptive layout).

## 2. Responsive test matrix — coverage classes

The briefing matrix (360/393/411 dp × font 1.00/1.15/1.30/1.50 × it/de) is a
COMPOSE-UI matrix: on the JVM the policy is proven; the pixel-truth pass is
the emulator/physical lane (evidence class separation per the mission rules).

| Surface | JVM-provable (this node) | Emulator/device lane |
| --- | --- | --- |
| Category list rows | AdaptiveActionPolicy width classes | E2E Italian Gate D walk (run 42247787274 gate_D) |
| Component details + links | policy + string-parity (LocalizationParityTest green) | Gate D screenshots |
| Primary actions | ≥48 dp, maxLines 2, wrap policy unit tests | Gate D + POCO physical (PENDING owner evidence) |
| More actions | overflow policy unit tests | Gate D |
| Progress state | label-never-hidden policy | emulator run artifacts |
| Error state | typed statusNote paths (link blocked, install failed) | emulator |
| Verification state | verify result rows | emulator |
| Qualification state | qualification rows | emulator |
| Source/license links | ModelLinks validation + 5-locale parity | Gate D |

Outcome classes:
- **JVM: PASS** — the adaptive policy, action enablement and label wrapping
  decisions are unit-proven (334+ suite green; lint 0 errors).
- **EMULATOR: PASS at 1.00–1.30 fonts, it locale** — Italian Gate D walked
  the Sanctum on the CI emulator lane at the tip (gate_D.txt present in the
  run artifacts).
- **PHYSICAL: QUALIFICATION_PENDING_DEVICE** — the 360–411 dp × 1.50 font ×
  de-locale corner matrix and the POCO screenshot placeholders remain Owner /
  physical-lane evidence; no physical claim is made.

## 3. Known layout risks found in review (typed, no silent gaps)

1. The 6 secondary buttons in the ARTIFACT rows of the ARCHIVE screen
   (Download/Pause/Resume/Re-download/Remove/Verify) use a BadgeFlowRow —
   at 360 dp × font 1.50 the row wraps to 3 lines. This is acceptable flow
   (never clipped) and the buttons stay ≥48 dp; flagged for the physical
   pass, not blocking.
2. The new Test/Hear-sample actions inherit the adaptive secondary row —
   verified by the same policy unit tests.
