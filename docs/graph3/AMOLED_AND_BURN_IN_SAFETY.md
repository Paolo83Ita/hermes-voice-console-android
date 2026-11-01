# AMOLED AND BURN-IN SAFETY — Graph 3

The monumental design must remain safe for prolonged AMOLED operation. The display of the
Xiaomi 11 Lite 5G NE is OLED-class; static high-luminance elements are the burn-in risk.

## 1. Baseline safeguards (kept from V0.1)

- Obsidian (near-black) default background — dominant dark pixels.
- Existing `burnInGuard` setting + reduced-motion respect (FR-40, D-015 semantics).
- Brightness scrim setting preserved (FR-41 prototype behavior).

## 2. New per-region safeguard

`BurnInOffsetController` applies a slow, bounded drift (±3 dp, 25 s cycle, region-phase
offsets) independently to:

| Region | Drift axis | Max offset |
|--------|-----------|-----------|
| Command frieze (title + seals) | X | ±3 dp |
| Oracle Core stage (whole stage group) | X+Y (Lissajous) | ±4 dp |
| Status seals (queue/update) | X | ±3 dp |
| Transcript panel | X | ±2 dp |
| Control medallions | X | ±3 dp |

Movement amplitudes are small enough not to harm usability (<0.5% of screen width per
element) and are disabled when reduced motion is on or burn-in guard is off (Owner
control, default ON).

## 3. Luminance discipline

- No permanent white header: the frieze is obsidian with gold engraving, budgeted ≤0.55
  bronze luminance in resting states (IDLE/DORMANT/DISCONNECTED/PRIVACY_MUTED).
- No permanent full-intensity cyan portal: portal budget per state (see
  ORACLE_CORE_STATE_SPEC §2); IDLE portal 0.26, breathing, never a static bright disc.
- Particle activity reduced during inactivity: IDLE ≤4 faint particles, DORMANT 0.
- Scheduled dimming: configurable display-rest schedule (Appearance) — after N minutes
  idle the core drops to near-static low luminance; option for pure black idle screen.
- Thermal-aware reduction reuses PERFORMANCE_AND_THERMAL_BUDGET.md §3 ladder.

## 4. Verification

- Preview pack AMOLED-risk review: resting-state renders (IDLE, DISCONNECTED) measured
  for bright-pixel ratio; luminance budgets asserted in the state table.
- Physical burn-in soak (4 h, static) remains part of the Xiaomi physical qualification
  package — QUALIFICATION_PENDING_DEVICE, never claimed from the emulator.
