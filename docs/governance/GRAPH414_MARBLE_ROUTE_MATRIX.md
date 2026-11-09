# GRAPH 4.14 — MARBLE ROUTE MATRIX (CP-G4.14-C04)

§6: every visible route under Marble Light, with theme read-back, backgrounds,
measured contrasts, system-bar state and verdict. Contrast values come from
`MARBLE_LIGHT_RENDERED_CONTRAST_REPORT.{json,md}` (42 computed rows on the
SHIPPED token values, re-run this session — FAIL rows: 0) and are pinned by
`ThemeAuthorityTest`. Rendered-screen confirmation on the physical devices is
the Owner retest (typed PENDING there — token-level calculations are
additionally cross-anchored by the Compose screenshot matrix lane).

Authority: the ONE `HermesTheme(darkTheme)` (ui/theme/Theme.kt). No route owns
a second authority; the 4.13 hard pin to MonumentalObsidian is removed.

Console is NOT allowed to stay fully dark under Marble (§6 rule): the Console
stage/frieze/chrome consume Marble tokens; the ALLOWED darks are the local
halo behind the Tower, the dark bronze objects and the dark oracle portal
(§18) — artwork, not chrome.

| Route / surface | Theme read-back | Background (Marble) | Primary text ≥4.5 | Secondary text ≥4.5 | Control / icon ≥3.0 | System bars | Verdict |
| --- | --- | --- | --- | --- | --- | --- | --- |
| Console | HermesTheme → MarbleLightColors + MonumentalMarble | stageBase/stage ivory (0xFFF1EBDD/0xFFEAE3D2) | ink 13.79–12.81:1 | inkSecondary 6.94–6.45:1; inkDim 5.4–5.02:1 | amber 6.16–5.73:1; cyanCore 3.59–3.34:1; redBronze 5.73–5.33:1 | dark icons (reactive) | PASS — not fully dark; halo/portal darkness is artwork |
| Drawer | same authority | panel 0xFFF8F4EA | ink 14.92:1; gold nav label 6.48:1 | inkSecondary 7.51:1 | bridge dot = BridgeModeAuthority (amber/cyan/gray/red) | dark icons | PASS |
| Models and Voice | same | surface 0xFFFAF7F0 + surfaceVariant 0xFFE9E4D8 | textPrimary 15.56–13.13:1 | textSecondary 6.93–5.84:1; tertiary 5.4–4.55:1 | compat verdict colors ≥4.5 as text | dark icons | PASS |
| Voice and Audio | same | surface | 15.56:1 | 6.93:1 | controls ≥3.0 | dark icons | PASS |
| Connection Studio | same | surface/background | 15.56–14.49:1 | 6.93–6.45:1 | outlineFocus 5.38:1 | dark icons | PASS |
| QR scanner chrome | same; camera preview is content, overlay high-contrast both themes | background + camera | 14.49:1 (chrome) | 6.45:1 | overlay outline ≥3:1 | dark icons | PASS |
| Archive | same | surface | 15.56:1 | 6.93:1 | ≥3.0 | dark icons | PASS |
| Diagnostics | same | surface | 15.56:1 | 6.93:1 | ≥3.0 | dark icons | PASS |
| Self-Test | same | surface | 15.56:1 | 6.93:1 | state colors ≥4.5 text / ≥3.0 icons | dark icons | PASS |
| Appearance | same; swatch labels LOCALIZED (×5 locales) | panel + swatches | ink 14.92:1; selected-label textOnSelected 0xFF3F2F14 | inkDim 5.85:1 | outlineFocus 5.38:1; outlineSecondary ≥3:1 | dark icons | PASS |
| Onboarding | same | background/surface | 14.49–15.56:1 | 6.45–6.93:1 | buttons ≥3.0 | dark icons | PASS |
| Dialogs | MaterialTheme marbleMaterialScheme | scheme surface | scheme onSurface ≥4.5 | scheme onSurfaceVariant | scheme controls | dark icons | PASS |
| Snackbar | same scheme | inverse-surface pair per M3 | scheme pair | scheme pair | scheme action | dark icons | PASS |
| Status bar / navigation bar | DisposableEffect follows settings.darkTheme | chrome bg | n/a | n/a | icons: dark on Marble | dark icons; immediate on switch; no restart | PASS |
| Tower stage (Console interior) | theme-aware stage + §42 variants | Marble keeps the light stage; the ONLY darks: local halo (α0.42), bronze objects, oracle portal | portal/marble bridge labels use m tokens ≥4.5 | — | Canvas overlays: amber/cyan ≥3:1 as icons | n/a (inside stage) | PASS — artwork darkness allowed, chrome never dark |
| Obsidian (regression guard) | MonumentalObsidian unchanged | dark tokens | pinned unchanged by ThemeAuthorityTest | same | same | light icons | PASS |

Mixed dark/light chrome: FORBIDDEN and absent — every chrome surface consumes
the same authority (no stale Obsidian tokens: the 4.13/4.14 repairs replaced
the hardcoded 0xFF0A0E16/0xFF070A11/0xFF070A10/0xFF0A0D14/0xFF070A10 literals
with theme tokens; the remaining hardcoded darks are the documented tower
MECHANISM artwork (OracleCore internals, §18) which carries no ordinary text).

## NODE / ACTION / TEST / FILES / EVIDENCE / CHECKPOINT

- NODE: CP-G4.14-C04 (theme leg).
- ACTION: re-ran `scripts/gen_g414_marble_contrast.py` at the final tip
  (42 rows / 0 FAIL); verified every route's consumer in code; produced this
  matrix; bridge-truth pill + Mock console renders in
  `docs/visual/tower414/renders/`.
- TEST: ThemeAuthorityTest (marble/obsidian pins + contrast thresholds) in the
  496/496 suite; lint 0/72.
- FILES: docs/governance/THEME_AUTHORITY_CONSUMER_MAP.md (authority, unchanged),
  docs/governance/MARBLE_LIGHT_RENDERED_CONTRAST_REPORT.{json,md} (re-generated),
  docs/visual/tower414/renders/* (new), this matrix.
- EVIDENCE: contrast report JSON (42 rows), renders, test results.
- CHECKPOINT: CP-G4.14-C04 — Bridge and theme final truth: DONE
  (bridge leg in GRAPH414_BRIDGE_TRUTH_FINAL_CHECK.md).
- NEXT: CP-G4.14-C05 — Model Library final qualification.
