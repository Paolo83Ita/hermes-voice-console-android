# THEME AUTHORITY CONSUMER MAP (Graph 4.14, CP-G4.14-006/007)

## The ONE authority

`HermesTheme(darkTheme)` (ui/theme/Theme.kt) provides, per theme:
- `LocalHermesColors` → ObsidianNightColors | MarbleLightColors (25 semantic tokens, §6/§7 authority from 4.13);
- `LocalMonumentalColors` → **MonumentalObsidian | MonumentalMarble** (GRAPH 4.14 — the hard pin to MonumentalObsidian was REMOVED; that pin was the FINDING-04/05 root cause);
- `MaterialTheme.colorScheme` → obsidianMaterialScheme() | marbleMaterialScheme();
- system bars follow `settings.darkTheme` reactively (MainActivity DisposableEffect — dark icons on Marble, light icons on Obsidian; applies immediately on switch, no process restart).

No route owns a second authority. No route may hardcode an obsidian assumption.

## Consumer map (every route)

| Route / surface | Theme source | Consumed colors | Local overrides (decoration-only) | System bars | Tower stage | Stale-theme risk | Physical validation |
| --- | --- | --- | --- | --- | --- | --- | --- |
| App root / MainActivity | HermesTheme | both locals | — | theme-reactive | n/a | none (single authority) | §21 switching contract tests |
| NavHost / drawer (AppNav) | same | m.* + hermesColors | — | same | drawer dot = BridgeModeAuthority | none | owner retest item K |
| Console | same | m.obsidian* stage + frieze gradients now tokens; hermesColors prose | Canvas state overlays (tower body only) | same | theme-aware stage; dark artwork + local halo allowed (§18) | cleared: literals 0xFF0A0E16/070A11/070A10 → m tokens | owner retest items B/C/J |
| Monumental header / chrome | same | MonumentalChrome gradients → colors.obsidian* tokens | — | same | n/a | cleared: 0xFF0A0D14/070A10 → tokens | owner retest K |
| Oracle core (Tower internals) | same | hardcoded dark internals = the tower MECHANISM artwork (kept dark per §18; no ordinary text on it) | — | same | inside tower | intentional (documented) | owner retest J |
| Timeline / composer | same | hermesColors semantics | — | same | n/a | none | owner retest B |
| Control buttons (frieze chips/pills) | same | m tokens via role mappings | — | same | n/a | none | owner retest L |
| Models and Voice | same | hermesColors semantics + compatVerdict colors | — | same | n/a | none | owner retest D |
| Voice and Audio | same | hermesColors | — | same | n/a | none | owner retest F |
| Connection Studio | same | hermesColors | — | same | n/a | none | owner retest A |
| QR scanner chrome | same | hermesColors; scanner overlay stays high-contrast | camera preview | same | n/a | overlay readable both themes (§17) | owner retest N |
| Archive | same | hermesColors | — | same | n/a | none | — |
| Diagnostics / Self-Test | same | hermesColors | — | same | n/a | none | — |
| Onboarding | same | hermesColors | — | same | n/a | none | owner retest K |
| Dialogs / snackbars | MaterialTheme scheme (per theme) | scheme tokens | — | same | n/a | M3 dialogs follow the scheme | owner retest B |
| Appearance | same | c.gold subtitle (theme-aware, ≥4.5 both); swatch labels LOCALIZED (app_theme_marble/obsidian ×5) | — | same | n/a | cleared (§20 fix) | owner retest C/K |

## §20 physical findings — repairs

1. "Aspetto nearly invisible in the top architectural header" → header ink/gold
   tokens theme-aware (MonumentalMarble gold 0xFF6E5420 ≥4.5 on ivory; HermesColors
   MarbleGold already deepened in 4.12); measured in the contrast report.
2. "Inactive theme labels too light" / "descriptions too pale" → marble ink family
   darkened (ink 0x241F14 / inkSecondary 0x554E3E / inkDim 0x665E4C — all ≥4.5 on
   every marble surface, pinned by ThemeAuthorityTest + report).
3. "Console remains dark after selecting Marble" → Theme.kt pin REMOVED; console
   stage/frieze/chrome consume theme tokens.
4. "Selected/unselected insufficiently distinct" → surfaceSelected (0xFFE8D9BC on
   0xFFE9E4D8? distinctness is shape+border+fill, plus textOnSelected 0xFF3F2F14);
   outlineFocus ≥3:1 (measured 5.8:1 on background).
5. "Weak light-card outlines" → outlineSecondary MarbleStoneShade ≥3:1 borders.

## §21 switching contract (implemented)

theme selected → store.settings.darkTheme (StateFlow) → HermesTheme recomposes
both locals + scheme → every consumer recomposes (no local theme caches) →
system bars updated (DisposableEffect) → tower stage recomposes via m tokens →
no restart. Process recreation/restart persists via SettingsRepository (K_DARK).
