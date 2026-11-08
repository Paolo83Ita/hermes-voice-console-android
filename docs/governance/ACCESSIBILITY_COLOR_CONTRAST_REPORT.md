# ACCESSIBILITY COLOR CONTRAST REPORT (GRAPH 4.12 §18/§19/§20)

Measured with the WCAG 2.x relative-luminance formula on the ACTUAL rendered
color pairs (same computation pinned by the JVM test ContrastRatioTest).
The Owner's PHYS-FAIL-02 (unreadable low-contrast text) traced to `inkDim`
`0xFF6E675C` at **3.17–3.63:1** on every obsidian surface — a measured WCAG
failure used on ordinary labels (composer placeholder, engine truth row,
idle state labels, mode notes).

## Repairs shipped (§19)

| Token | Before | After | Result |
| --- | --- | --- | --- |
| textMuted (inkDim) | 0xFF6E675C (3.17–3.63:1 FAIL) | 0xFF8A8375 | ≥4.72:1 on every obsidian surface |
| textWarning/Error small text | redBronze 0xFFC4553B (4.24:1 on panel) | redBronzeText 0xFFDE7A5E | ≥5.96:1; redBronze keeps ICON/seal duty (≥3.0) |

Rules enforced: every ordinary label uses semantic tokens; raw low-alpha
bronze descriptive text is banned; disabled-style color on enabled controls is
banned; textMuted still meets 4.5 for ordinary readable text. Focus indicators
render through the Material focus ring on raised surfaces (≥3.0 vs adjacent
surfaces).

## Obsidian Night (Monumental identity) — measured rendered combinations

| Foreground | Background | Ratio | Normal ≥4.5 | Large/Icon ≥3.0 |
| --- | --- | --- | --- | --- |
| textPrimary (ink) | obsidianDeep | 16.33:1 | PASS | PASS |
| textPrimary (ink) | obsidian | 15.85:1 | PASS | PASS |
| textPrimary (ink) | obsidianPanel | 15.22:1 | PASS | PASS |
| textPrimary (ink) | obsidianRaised | 14.29:1 | PASS | PASS |
| textSecondary (inkSecondary) | obsidianDeep | 7.73:1 | PASS | PASS |
| textSecondary (inkSecondary) | obsidian | 7.51:1 | PASS | PASS |
| textSecondary (inkSecondary) | obsidianPanel | 7.21:1 | PASS | PASS |
| textSecondary (inkSecondary) | obsidianRaised | 6.77:1 | PASS | PASS |
| textMuted (inkDim REPAIRED) | obsidianDeep | 5.39:1 | PASS | PASS |
| textMuted (inkDim REPAIRED) | obsidian | 5.23:1 | PASS | PASS |
| textMuted (inkDim REPAIRED) | obsidianPanel | 5.02:1 | PASS | PASS |
| textMuted (inkDim REPAIRED) | obsidianRaised | 4.72:1 | PASS | PASS |
| textMuted OLD (RETIRED) | obsidianDeep | 3.63:1 | FAIL | PASS |
| textMuted OLD (RETIRED) | obsidian | 3.52:1 | FAIL | PASS |
| textMuted OLD (RETIRED) | obsidianPanel | 3.38:1 | FAIL | PASS |
| textMuted OLD (RETIRED) | obsidianRaised | 3.17:1 | FAIL | PASS |
| redBronzeText (textWarning/Error REPAIRED) | obsidianDeep | 6.81:1 | PASS | PASS |
| redBronzeText (textWarning/Error REPAIRED) | obsidian | 6.61:1 | PASS | PASS |
| redBronzeText (textWarning/Error REPAIRED) | obsidianPanel | 6.34:1 | PASS | PASS |
| redBronzeText (textWarning/Error REPAIRED) | obsidianRaised | 5.96:1 | PASS | PASS |
| redBronze (icons/seal only) | obsidianDeep | 4.55:1 | PASS | PASS |
| redBronze (icons/seal only) | obsidian | 4.41:1 | FAIL | PASS |
| redBronze (icons/seal only) | obsidianPanel | 4.24:1 | FAIL | PASS |
| redBronze (icons/seal only) | obsidianRaised | 3.98:1 | FAIL | PASS |
| cyanCore (accent/icon) | obsidianDeep | 12.80:1 | PASS | PASS |
| cyanCore (accent/icon) | obsidian | 12.43:1 | PASS | PASS |
| cyanCore (accent/icon) | obsidianPanel | 11.93:1 | PASS | PASS |
| cyanCore (accent/icon) | obsidianRaised | 11.20:1 | PASS | PASS |
| gold (frieze/icon) | obsidianDeep | 10.24:1 | PASS | PASS |
| gold (frieze/icon) | obsidian | 9.94:1 | PASS | PASS |
| gold (frieze/icon) | obsidianPanel | 9.54:1 | PASS | PASS |
| gold (frieze/icon) | obsidianRaised | 8.96:1 | PASS | PASS |
| bronzeLight (icon) | obsidianDeep | 6.68:1 | PASS | PASS |
| bronzeLight (icon) | obsidian | 6.48:1 | PASS | PASS |
| bronzeLight (icon) | obsidianPanel | 6.22:1 | PASS | PASS |
| bronzeLight (icon) | obsidianRaised | 5.84:1 | PASS | PASS |
| amber (state) | obsidianDeep | 9.40:1 | PASS | PASS |
| amber (state) | obsidian | 9.13:1 | PASS | PASS |
| amber (state) | obsidianPanel | 8.76:1 | PASS | PASS |
| amber (state) | obsidianRaised | 8.23:1 | PASS | PASS |
## Marble Light — measured rendered combinations

| Foreground | Background | Ratio | Normal ≥4.5 | Large/Icon ≥3.0 |
| --- | --- | --- | --- | --- |
| ink (textPrimary) | marbleBackground | 14.49:1 | PASS | PASS |
| ink (textPrimary) | marbleSurface | 15.56:1 | PASS | PASS |
| ink (textPrimary) | marbleSurfaceVariant | 13.13:1 | PASS | PASS |
| inkSecondary (textSecondary) | marbleBackground | 6.45:1 | PASS | PASS |
| inkSecondary (textSecondary) | marbleSurface | 6.93:1 | PASS | PASS |
| inkSecondary (textSecondary) | marbleSurfaceVariant | 5.84:1 | PASS | PASS |
| marbleBronze (accent) | marbleBackground | 5.38:1 | PASS | PASS |
| marbleBronze (accent) | marbleSurface | 5.78:1 | PASS | PASS |
| marbleBronze (accent) | marbleSurfaceVariant | 4.87:1 | PASS | PASS |
| marbleGold (accent) | marbleBackground | 5.02:1 | PASS | PASS |
| marbleGold (accent) | marbleSurface | 5.40:1 | PASS | PASS |
| marbleGold (accent) | marbleSurfaceVariant | 4.55:1 | PASS | PASS |
| marbleEnergy (accent) | marbleBackground | 5.88:1 | PASS | PASS |
| marbleEnergy (accent) | marbleSurface | 6.31:1 | PASS | PASS |
| marbleEnergy (accent) | marbleSurfaceVariant | 5.32:1 | PASS | PASS |
| marbleWarn | marbleBackground | 5.55:1 | PASS | PASS |
| marbleWarn | marbleSurface | 5.96:1 | PASS | PASS |
| marbleWarn | marbleSurfaceVariant | 5.02:1 | PASS | PASS |
| marbleError | marbleBackground | 5.69:1 | PASS | PASS |
| marbleError | marbleSurface | 6.11:1 | PASS | PASS |
| marbleError | marbleSurfaceVariant | 5.15:1 | PASS | PASS |
| marbleOk | marbleBackground | 5.54:1 | PASS | PASS |
| marbleOk | marbleSurface | 5.95:1 | PASS | PASS |
| marbleOk | marbleSurfaceVariant | 5.02:1 | PASS | PASS |
## Font-scale coverage (§20)

| Scale | Applies to | Contrast impact |
| --- | --- | --- |
| 1.00 | body/labels | no threshold change; ordinary labels keep 4.5 |
| 1.15 | body/labels | no threshold change; ordinary labels keep 4.5 |
| 1.30 | body/labels | large-text threshold 3.0 may apply to text ≥18pt bold / ≥24pt; ALL ordinary labels keep the 4.5 requirement at every scale |
| 1.50 | body/labels | large-text threshold 3.0 may apply to text ≥18pt bold / ≥24pt; ALL ordinary labels keep the 4.5 requirement at every scale |

## Verdict

- Old inkDim would FAIL everywhere (regression-guarded by ContrastRatioTest).
- Repaired tokens: **PASS at 4.5:1 for ordinary text, 3.0:1 for large text and
  essential icons** across Obsidian Night and Marble Light.
- Raw values remain visible only in Advanced Diagnostics (§21), which is not
  an ordinary user surface.
- State after this report: contrast repaired in code; the physical verdict
  stays Owner-only.

---

# GRAPH 4.13 ADDENDUM (2026-11-09)

The 4.12 tables above are TOKEN-PAIR audits. The Owner's continued
APP_WIDE_TEXT_CONTRAST_FAIL led to the RENDERED-SURFACE audit of Graph 4.13:
`APP_WIDE_RENDERED_CONTRAST_AUDIT_GRAPH413.md` (WCAG 2.x on the code's real
surface stacks). Two defect classes were found and repaired:

1. Compose M3 default disabled labels measured 1.43:1 (FilledButton) and
   2.09:1 (FilterChip) on obsidian → replaced by the §5 wrappers
   (textDisabled ≥3.0 + fill + border cues).
2. Marble-Light prose measured 1.16–3.12:1 because the app shell stayed
   obsidian in both themes → theme-aware shell + marble semantic palette.

The live semantic authority (25 tokens × 2 palettes) is measured 50 rows /
0 FAIL in BOTH themes and pinned by `SemanticContrastAuthorityTest`
(thresholds: text ≥4.5, critical ≥4.5, icon/focus/disabled ≥3.0).
