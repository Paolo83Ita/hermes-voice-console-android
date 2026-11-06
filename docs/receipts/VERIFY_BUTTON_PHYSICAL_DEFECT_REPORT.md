# VERIFY BUTTON PHYSICAL DEFECT REPORT

- Mission: OWNER PHYSICAL RETEST FAILURE (CP-G4-029) — §8 VERIFY BUTTON LAYOUT
  REPAIR
- Owner: Paolo Lombisani · Device: POCO X6 Pro (≈372 dp width class)
- Screen: **Models and Voice** (`model_manager`, ModelManagerScreen) —
  lifecycle action rows of every voice-component card.
- Physical observation (PHYSICAL-FAIL-03): the text inside the **Verify**
  button remains incorrectly laid out.

## 1. The exact Composable that rendered the defect

Pre-repair, the Verify button was:

```kotlin
Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {   // ← fixed row, no wrap
    TextButton(onClick = { ... }) { Text(stringResource(R.string.mm_verify)) }  // ← in row 2 of 3
    ...
}
```

Three stacked FIXED horizontal `Row`s held up to 6 `TextButton`s each
(Download, Pause, Resume, Cancel, **Verify**, Details / Install, Qualify,
Activate, Deactivate, Rollback, Remove / Re-download, Repair, Update, Open
diagnostics). The screen Column scrolls VERTICALLY only — a horizontal
overflow is clipped, not scrollable.

## 2. Inspection (the mandated checklist)

| Aspect | Pre-repair fact | Defect mechanism |
| --- | --- | --- |
| button width | intrinsic `TextButton` content width | the row's total intrinsic width exceeds ≈372 dp; Compose clips at the viewport edge |
| content padding | default `TextButton` (≈12 dp h) | no room left when the row overflows |
| minimum height | default (≈40 dp) | below the 48 dp touch target |
| text style | `HermesType` label | — |
| icon | none (text-only) | not a factor |
| maxLines | unbounded | the last visible button's label WRAPPED INTO the clip region → "incorrectly laid out" text |
| softWrap | default true | contributes to the visible wrap-clip |
| font scaling | Owner may use >1.0 | inflates label widths → the overflow appears even where 1.0 fits |
| parent constraints | fixed `Row` inside a vertical-scroll Column | no width negotiation, no wrap |
| sibling actions | 5 more buttons share the row | the aggregate width is the defect |
| localized Italian label | "Verifica" | Italian labels are the ones physically rendered |
| status spinner | none | not a factor |
| loading state | none on this button | not a factor |
| enabled/disabled | enabled state only tints | not the layout defect |

Root-cause class: **FIXED_ROW_ACTION_OVERFLOW** (ledger S11). CP-G4-027's
BadgeFlowRow repair touched BADGE rows only — the ACTION rows were left
fixed, which is exactly why the Owner saw no improvement on this button.

## 3. The repair (adaptive, not another generic FlowRow-only patch)

New component: `ui/components/AdaptiveActionsRow.kt` (+ pure
`AdaptiveActionPolicy` for the width threshold, unit-tested).

1. **Hierarchy**: PRIMARY actions render directly — Download, **Verify**
   (emphasized, filled), Activate, Details (Details stays direct: the E2E
   Italian Gate D walk expands the attribute sheet through it). All SECONDARY
   actions (Pause, Resume, Cancel, Install, Qualify, Deactivate, Rollback,
   Remove, Re-download, Repair, Update, Open diagnostics) live behind
   **"Altre azioni" (More actions)** — an overflow menu that can never clip.
2. **Width adaptation**: ≥480 dp available → primary actions share ONE row +
   the More-actions menu; below → the primary actions WRAP (FlowRow) with
   48 dp-minimum rows. The POCO's ≈372 dp is the wrapped path.
3. **Every action button**: minimum touch height **48 dp**; minimum readable
   horizontal padding **12 dp**; **no fixed narrow width** (intrinsic +
   wrap); **maxLines 2** with ellipsis overflow; **centered text**; no icon
   or progress indicator to overlap.
4. The expanded-sheet LINK row (Open source / Open license / Copy identity /
   Retry source check) uses the same adaptive component (secondary-only →
   fully behind More actions).
5. No label changed: every action keeps its existing localized string
   (`mm_verify` = "Verifica" etc.), so the physical instruction "the Verify
   action must use…" applies to the same named control the Owner tested.

## 4. Required-state coverage (§8)

- Actions > 3 on a phone → primary direct + **More actions** overflow. ✔
- Wide width → shared row; constrained → wrap + overflow. ✔ (AdaptiveActionPolicyTest)
- 48 dp touch height, centered, maxLines 2, no fixed width, no clipping. ✔ (component constants)
- Recommended direct set (Download or Install / Verify / Activate) present. ✔

## 5. Verification status

- Unit: AdaptiveActionPolicyTest (360/393/411 dp → WRAPPED_PRIMARY;
  480/600 dp → SINGLE_ROW) — green in the 310-test suite.
- Physical matrix (360/393/411 dp × font 1.00–1.50 × it/de): the wrapped
  layout is overflow-free BY CONSTRUCTION (wrap + overflow menu); the
  rendered screenshots are captured in the next physical cycle
  (PENDING_OWNER_CAPTURE in the ledger).
