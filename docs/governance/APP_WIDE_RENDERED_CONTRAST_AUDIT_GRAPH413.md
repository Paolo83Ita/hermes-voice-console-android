# APP-WIDE RENDERED CONTRAST AUDIT — GRAPH 4.13 (CP-G4.13-002, post-repair)

Evidence: WCAG 2.x computed on the code's ACTUAL rendered surface stacks (source-parsed tokens, worst-case gradient stop, alpha blended).
PRE-REPAIR evidence: 82 rows / 11 FAIL (drove CP-G4.13-003).
LIVE semantic authority: 50 rows / 0 FAIL.
Emulator screenshot verification is layered by the CP-G4.13-004 screenshot gates.

## PART A — PRE-REPAIR evidence (historical, superseded)

### App shell

| component | state | theme | enabled | fg (worst bg) | ratio | class | verdict |
|---|---|---|---|---|---|---|---|
| screen host background | OBSIDIAN theme shell | OBSIDIAN | en | #FFE8E6E1 (#0A0E16) | 15.49:1 | normal ≥4.5 | **PASS** |
| screen host background | OBSIDIAN shell secondary | OBSIDIAN | en | #FFA7A39A (#0A0E16) | 7.68:1 | normal ≥4.5 | **PASS** |
| screen host background | OBSIDIAN shell accent label | OBSIDIAN | en | #FFC08A3E (#0A0E16) | 6.39:1 | normal ≥4.5 | **PASS** |
| screen host background | MARBLE theme shell | MARBLE | en | #FF201E19 (#0A0E16) | 1.16:1 | normal ≥4.5 | **FAIL** |
| screen host background | MARBLE shell secondary | MARBLE | en | #FF5A554A (#0A0E16) | 2.61:1 | normal ≥4.5 | **FAIL** |
| screen host background | MARBLE shell accent label | MARBLE | en | #FF7A5C2E (#0A0E16) | 3.12:1 | normal ≥4.5 | **FAIL** |

### Chrome

| component | state | theme | enabled | fg (worst bg) | ratio | class | verdict |
|---|---|---|---|---|---|---|---|
| SectionCard | title | OBSIDIAN | en | #FFD9B36A (#FF131824) | 8.96:1 | normal ≥4.5 | **PASS** |
| SectionCard | body content | OBSIDIAN | en | #FFEDE6D8 (#FF131824) | 14.29:1 | normal ≥4.5 | **PASS** |
| StatusPill | label | OBSIDIAN | en | #FFA89F8D (#FF0D1119) | 7.21:1 | normal ≥4.5 | **PASS** |
| StatusPill | state dot icon | OBSIDIAN | en | #FF35E0FF (#FF0D1119) | 11.93:1 | icon ≥3.0 | **PASS** |
| ModeChip | selected | OBSIDIAN | en | #FFD9B36A (#FF191611) | 9.11:1 | normal ≥4.5 | **PASS** |
| ModeChip | unselected | OBSIDIAN | en | #FFA89F8D (#FF0D1119) | 7.21:1 | normal ≥4.5 | **PASS** |
| ModeChip | disabled (pre-repair render) | OBSIDIAN | dis | #FFA89F8D (#FF0D1119) | 7.21:1 | disabled ≥3.0 | **PASS** |
| TruthBadge | gold badge text | OBSIDIAN | en | #FFD9B36A (#FF131824) | 8.96:1 | normal ≥4.5 | **PASS** |
| TruthBadge | warning badge text (redBronzeText) | OBSIDIAN | en | #FFDE7A5E (#FF131824) | 5.96:1 | critical ≥4.5 | **PASS** |
| ArchitecturalPanel | outline focus ring | OBSIDIAN | en | #FF8A6A38 (#FF131824) | 3.55:1 | focus ≥3.0 | **PASS** |
| FilledButton | disabled label (M3 default onSurface@38%) | OBSIDIAN | dis | #FFE8E6E1 (#2B822DE631AF) | 1.43:1 | disabled ≥3.0 | **FAIL** |
| FilterChip | disabled label (M3 default @38%) | OBSIDIAN | dis | #FFA7A39A (#FF12151A) | 2.09:1 | disabled ≥3.0 | **FAIL** |

### Console

| component | state | theme | enabled | fg (worst bg) | ratio | class | verdict |
|---|---|---|---|---|---|---|---|
| Frieze | HERMES title | OBSIDIAN | en | #FFD9B36A (#0A0E16) | 9.75:1 | large ≥3.0 | **PASS** |
| Frieze | TOWER title | OBSIDIAN | en | #FFB98C4A (#0A0E16) | 6.36:1 | large ≥3.0 | **PASS** |
| Frieze | subtitle | OBSIDIAN | en | #FFA89F8D (#0A0E16) | 7.37:1 | normal ≥4.5 | **PASS** |
| StateChip | state label | OBSIDIAN | en | #FFF0D9A0 (#FF131824) | 12.79:1 | large ≥3.0 | **PASS** |
| EngineRows | local-core label | OBSIDIAN | en | #FFE8C87B (#FF131824) | 10.96:1 | normal ≥4.5 | **PASS** |
| EngineRows | tower label | OBSIDIAN | en | #FF4C8DFF (#FF131824) | 5.54:1 | normal ≥4.5 | **PASS** |
| EngineRows | idle note | OBSIDIAN | en | #FF8A8375 (#FF131824) | 4.72:1 | normal ≥4.5 | **PASS** |
| Composer | typed text | OBSIDIAN | en | #FFEDE6D8 (#FF131824) | 14.29:1 | normal ≥4.5 | **PASS** |
| Composer | placeholder | OBSIDIAN | en | #FF8A8375 (#FF131824) | 4.72:1 | normal ≥4.5 | **PASS** |
| Composer | send icon | OBSIDIAN | en | #FF35E0FF (#FF131824) | 11.2:1 | icon ≥3.0 | **PASS** |
| Composer | mic icon (idle) | OBSIDIAN | en | #FFD9B36A (#FF131824) | 8.96:1 | icon ≥3.0 | **PASS** |
| Composer | privacy-muted mic icon | OBSIDIAN | en | #FFC4553B (#FF131824) | 3.98:1 | icon ≥3.0 | **PASS** |
| Composer | Interrompi enabled | OBSIDIAN | en | #FF35E0FF (#FF131824) | 11.2:1 | critical ≥4.5 | **PASS** |
| Composer | Interrompi disabled label | OBSIDIAN | dis | #FF8A8375 (#FF131824) | 4.72:1 | disabled ≥3.0 | **PASS** |
| Chat | user turn text | OBSIDIAN | en | #FFEDE6D8 (#FF131824) | 14.29:1 | normal ≥4.5 | **PASS** |
| Chat | turn who label | OBSIDIAN | en | #FFD9B36A (#FF131824) | 8.96:1 | normal ≥4.5 | **PASS** |
| Chat | error line | OBSIDIAN | en | #FFDE7A5E (#FF131824) | 5.96:1 | critical ≥4.5 | **PASS** |
| Chat | info line (cyan) | OBSIDIAN | en | #FF35E0FF (#FF131824) | 11.2:1 | normal ≥4.5 | **PASS** |
| UnderChrome | hint note | OBSIDIAN | en | #FF8A8375 (#FF131824) | 4.72:1 | normal ≥4.5 | **PASS** |

### Models and Voice

| component | state | theme | enabled | fg (worst bg) | ratio | class | verdict |
|---|---|---|---|---|---|---|---|
| A. Recommended configuration | device/resources/TTS/network/profile rows | OBSIDIAN | en | #FFE8E6E1 (#FF131824) | 14.22:1 | normal ≥4.5 | **PASS** |
| A. Recommended configuration | warning line | OBSIDIAN | en | #FFF59E0B (#FF131824) | 8.26:1 | critical ≥4.5 | **PASS** |
| A. Recommended configuration | hint (select mode) | OBSIDIAN | en | #FFA7A39A (#FF131824) | 7.05:1 | normal ≥4.5 | **PASS** |
| A. Recommended configuration | profile chip label (FilterChip) | OBSIDIAN | en | #FFA7A39A (#FF12151A) | 7.27:1 | normal ≥4.5 | **PASS** |
| Catalog cards | model title | OBSIDIAN | en | #FFC08A3E (#FF131824) | 5.87:1 | normal ≥4.5 | **PASS** |
| Catalog cards | metadata line | OBSIDIAN | en | #FFA7A39A (#FF131824) | 7.05:1 | normal ≥4.5 | **PASS** |
| Catalog cards | state text (raw enum) | OBSIDIAN | en | #FF22D3EE (#FF131824) | 9.82:1 | normal ≥4.5 | **PASS** |
| Model detail | technical hash (mono) | OBSIDIAN | en | #FFA7A39A (#FF131824) | 7.05:1 | normal ≥4.5 | **PASS** |

### Voice and Audio

| component | state | theme | enabled | fg (worst bg) | ratio | class | verdict |
|---|---|---|---|---|---|---|---|
| Header | title | OBSIDIAN | en | #FFE8E6E1 (#0A0E16) | 15.49:1 | normal ≥4.5 | **PASS** |
| Engine truth rows | wake word badge (gold) | OBSIDIAN | en | #FFD4A24E (#FF131824) | 7.67:1 | normal ≥4.5 | **PASS** |
| Engine truth rows | TTS active badge (ok) | OBSIDIAN | en | #FF34D399 (#FF131824) | 9.23:1 | critical ≥4.5 | **PASS** |
| Listening modes | mode description | OBSIDIAN | en | #FFA7A39A (#FF131824) | 7.05:1 | normal ≥4.5 | **PASS** |
| Body | section note (energy) | OBSIDIAN | en | #FF22D3EE (#FF131824) | 9.82:1 | normal ≥4.5 | **PASS** |

### Tower Connection Studio

| component | state | theme | enabled | fg (worst bg) | ratio | class | verdict |
|---|---|---|---|---|---|---|---|
| Header | title | OBSIDIAN | en | #FFE8E6E1 (#0A0E16) | 15.49:1 | normal ≥4.5 | **PASS** |
| Method section | label (bronze) | OBSIDIAN | en | #FFC08A3E (#FF131824) | 5.87:1 | normal ≥4.5 | **PASS** |
| Outcome | detail lines | OBSIDIAN | en | #FFA7A39A (#FF131824) | 7.05:1 | normal ≥4.5 | **PASS** |
| Outcome | message (energy) | OBSIDIAN | en | #FF22D3EE (#FF131824) | 9.82:1 | normal ≥4.5 | **PASS** |

### QR Scanner

| component | state | theme | enabled | fg (worst bg) | ratio | class | verdict |
|---|---|---|---|---|---|---|---|
| Viewfinder | frame outline (focus-class) | OBSIDIAN | en | #FF8A6A38 (#FF04060A) | 4.06:1 | focus ≥3.0 | **PASS** |
| Helper | instruction text | OBSIDIAN | en | #FFA89F8D (#0A0E16) | 7.37:1 | normal ≥4.5 | **PASS** |
| Helper | error text | OBSIDIAN | en | #FFDE7A5E (#0A0E16) | 6.49:1 | critical ≥4.5 | **PASS** |

### Self-Test Center

| component | state | theme | enabled | fg (worst bg) | ratio | class | verdict |
|---|---|---|---|---|---|---|---|
| Run state | running line (energy) | OBSIDIAN | en | #FF22D3EE (#FF131824) | 9.82:1 | normal ≥4.5 | **PASS** |
| Results | test id + name | OBSIDIAN | en | #FFE8E6E1 (#FF131824) | 14.22:1 | normal ≥4.5 | **PASS** |
| Results | PASS verdict badge (ok) | OBSIDIAN | en | #FF34D399 (#FF131824) | 9.23:1 | critical ≥4.5 | **PASS** |

### Diagnostics

| component | state | theme | enabled | fg (worst bg) | ratio | class | verdict |
|---|---|---|---|---|---|---|---|
| Header | body rows | OBSIDIAN | en | #FFE8E6E1 (#FF131824) | 14.22:1 | normal ≥4.5 | **PASS** |
| Advanced | device identifier (2311DRK48G class) | OBSIDIAN | en | #FFA7A39A (#FF131824) | 7.05:1 | normal ≥4.5 | **PASS** |

### Appearance

| component | state | theme | enabled | fg (worst bg) | ratio | class | verdict |
|---|---|---|---|---|---|---|---|
| Theme modes | OBSIDIAN NIGHT chip label | OBSIDIAN | en | #FFA7A39A (#FF12151A) | 7.27:1 | normal ≥4.5 | **PASS** |
| Quality | profile description | OBSIDIAN | en | #FFA7A39A (#FF131824) | 7.05:1 | normal ≥4.5 | **PASS** |

### Onboarding

| component | state | theme | enabled | fg (worst bg) | ratio | class | verdict |
|---|---|---|---|---|---|---|---|
| Steps | title | OBSIDIAN | en | #FFE8E6E1 (#0A0E16) | 15.49:1 | normal ≥4.5 | **PASS** |
| Steps | section label (bronze) | OBSIDIAN | en | #FFC08A3E (#0A0E16) | 6.39:1 | normal ≥4.5 | **PASS** |
| Steps | body | OBSIDIAN | en | #FFE8E6E1 (#0A0E16) | 15.49:1 | normal ≥4.5 | **PASS** |
| Steps | progress (energy) | OBSIDIAN | en | #FF22D3EE (#0A0E16) | 10.69:1 | normal ≥4.5 | **PASS** |

### Archive

| component | state | theme | enabled | fg (worst bg) | ratio | class | verdict |
|---|---|---|---|---|---|---|---|
| List | artifact title | OBSIDIAN | en | #FFE8E6E1 (#FF131824) | 14.22:1 | normal ≥4.5 | **PASS** |
| Preview | SIMULATED marker | OBSIDIAN | en | #FFE8A33D (#FF131824) | 8.23:1 | critical ≥4.5 | **PASS** |

### About and Safety

| component | state | theme | enabled | fg (worst bg) | ratio | class | verdict |
|---|---|---|---|---|---|---|---|
| Sections | body | OBSIDIAN | en | #FFE8E6E1 (#FF131824) | 14.22:1 | normal ≥4.5 | **PASS** |
| Links | link label (energy) | OBSIDIAN | en | #FF22D3EE (#FF131824) | 9.82:1 | normal ≥4.5 | **PASS** |

### Settings hub

| component | state | theme | enabled | fg (worst bg) | ratio | class | verdict |
|---|---|---|---|---|---|---|---|
| Rows | row title | OBSIDIAN | en | #FFE8E6E1 (#0A0E16) | 15.49:1 | normal ≥4.5 | **PASS** |
| Rows | hub marker (inkDim) | OBSIDIAN | en | #FF8A8375 (#0A0E16) | 5.14:1 | normal ≥4.5 | **PASS** |

### History

| component | state | theme | enabled | fg (worst bg) | ratio | class | verdict |
|---|---|---|---|---|---|---|---|
| List | session meta | OBSIDIAN | en | #FFA7A39A (#FF131824) | 7.05:1 | normal ≥4.5 | **PASS** |

### Local Intelligence

| component | state | theme | enabled | fg (worst bg) | ratio | class | verdict |
|---|---|---|---|---|---|---|---|
| Runtime | unavailable badge | OBSIDIAN | en | #FFA7A39A (#FF131824) | 7.05:1 | critical ≥4.5 | **PASS** |

### All screens (Marble)

| component | state | theme | enabled | fg (worst bg) | ratio | class | verdict |
|---|---|---|---|---|---|---|---|
| shell-hosted text | body text (c.ink) | MARBLE | en | #FF201E19 (#0A0E16) | 1.16:1 | normal ≥4.5 | **FAIL** |
| shell-hosted text | secondary text (c.inkSecondary) | MARBLE | en | #FF5A554A (#0A0E16) | 2.61:1 | normal ≥4.5 | **FAIL** |
| shell-hosted text | accent label (c.bronze) | MARBLE | en | #FF7A5C2E (#0A0E16) | 3.12:1 | normal ≥4.5 | **FAIL** |
| shell-hosted text | warning (c.warn) | MARBLE | en | #FF8A5210 (#0A0E16) | 3.03:1 | normal ≥4.5 | **FAIL** |
| shell-hosted text | info (c.energy) | MARBLE | en | #FF0C6478 (#0A0E16) | 2.86:1 | normal ≥4.5 | **FAIL** |
| shell-hosted text | success (c.ok) | MARBLE | en | #FF276B4D (#0A0E16) | 3.03:1 | normal ≥4.5 | **FAIL** |

### PRE-REPAIR FAIL summary (repair table)

- `G413-004` App shell / screen host background (MARBLE theme shell, MARBLE) — 1.16:1 < 4.5:1
- `G413-005` App shell / screen host background (MARBLE shell secondary, MARBLE) — 2.61:1 < 4.5:1
- `G413-006` App shell / screen host background (MARBLE shell accent label, MARBLE) — 3.12:1 < 4.5:1
- `G413-017` Chrome / FilledButton (disabled label (M3 default onSurface@38%), OBSIDIAN) — 1.43:1 < 3.0:1
- `G413-018` Chrome / FilterChip (disabled label (M3 default @38%), OBSIDIAN) — 2.09:1 < 3.0:1
- `G413-077` All screens (Marble) / shell-hosted text (body text (c.ink), MARBLE) — 1.16:1 < 4.5:1
- `G413-078` All screens (Marble) / shell-hosted text (secondary text (c.inkSecondary), MARBLE) — 2.61:1 < 4.5:1
- `G413-079` All screens (Marble) / shell-hosted text (accent label (c.bronze), MARBLE) — 3.12:1 < 4.5:1
- `G413-080` All screens (Marble) / shell-hosted text (warning (c.warn), MARBLE) — 3.03:1 < 4.5:1
- `G413-081` All screens (Marble) / shell-hosted text (info (c.energy), MARBLE) — 2.86:1 < 4.5:1
- `G413-082` All screens (Marble) / shell-hosted text (success (c.ok), MARBLE) — 3.03:1 < 4.5:1

## PART B — LIVE SEMANTIC AUTHORITY (post CP-G4.13-003)

### OBSIDIAN

| component | state | enabled | fg (bg) | ratio | class | verdict |
|---|---|---|---|---|---|---|
| shell prose | textPrimary on surfaceBase | en | #FFE8E1D5 (#0A0E16) | 14.86:1 | normal ≥4.5 | **PASS** |
| shell prose | textSecondary on surfaceBase | en | #FFBEB5A6 (#0A0E16) | 9.52:1 | normal ≥4.5 | **PASS** |
| panel prose | textPrimary on surfaceRaised | en | #FFE8E1D5 (#FF131824) | 13.65:1 | normal ≥4.5 | **PASS** |
| panel prose | textSecondary on surfaceInteractive | en | #FFBEB5A6 (#FF1B2027) | 8.07:1 | normal ≥4.5 | **PASS** |
| panel prose | textTertiaryReadable on surfaceInteractive | en | #FFAAA69F (#FF1B2027) | 6.76:1 | normal ≥4.5 | **PASS** |
| panel prose | textTechnical on surfaceRaised | en | #FFAAA69F (#FF131824) | 7.32:1 | normal ≥4.5 | **PASS** |
| status text | textSuccess on surfaceRaised | en | #FF34D399 (#FF131824) | 9.23:1 | critical ≥4.5 | **PASS** |
| status text | textWarning on surfaceRaised | en | #FFF59E0B (#FF131824) | 8.26:1 | critical ≥4.5 | **PASS** |
| status text | textError on surfaceRaised | en | #FFF87171 (#FF131824) | 6.41:1 | critical ≥4.5 | **PASS** |
| status text | textInfo on surfaceRaised | en | #FF22D3EE (#FF131824) | 9.82:1 | critical ≥4.5 | **PASS** |
| SectionCard | heading gold on surfaceInteractive | en | #FFD4A24E (#FF1B2027) | 7.08:1 | normal ≥4.5 | **PASS** |
| SectionCard | heading bronze on surfaceInteractive | en | #FFC08A3E (#FF1B2027) | 5.42:1 | normal ≥4.5 | **PASS** |
| StatusPill | label on surfaceInteractive | en | #FFBEB5A6 (#FF1B2027) | 8.07:1 | normal ≥4.5 | **PASS** |
| ModeChip | selected label on surfaceSelected | en | #FFF0D9A0 (#FF2C261B) | 10.81:1 | normal ≥4.5 | **PASS** |
| ModeChip | unselected label on surfaceInteractive | en | #FFBEB5A6 (#FF1B2027) | 8.07:1 | normal ≥4.5 | **PASS** |
| ModeChip | disabled label on surfaceDisabled (cue 1) | dis | #FF9A9488 (#FF2D2E32) | 4.5:1 | disabled ≥3.0 | **PASS** |
| FilledButton | enabled label on bronze | en | #FF120C04 (#FFC08A3E) | 6.43:1 | normal ≥4.5 | **PASS** |
| FilledButton | disabled label on surfaceDisabled (cue 1) | dis | #FF9A9488 (#FF2D2E32) | 4.5:1 | disabled ≥3.0 | **PASS** |
| FilterChip | unselected on surfaceInteractive | en | #FFBEB5A6 (#FF1B2027) | 8.07:1 | normal ≥4.5 | **PASS** |
| FilterChip | selected on surfaceSelected | en | #FFF0D9A0 (#FF2C261B) | 10.81:1 | normal ≥4.5 | **PASS** |
| FilterChip | disabled on surfaceDisabled (cue 1) | dis | #FF9A9488 (#FF2D2E32) | 4.5:1 | disabled ≥3.0 | **PASS** |
| Switch | thumb icon on surfaceInteractive | en | #FFB98C4A (#FF1B2027) | 5.39:1 | icon ≥3.0 | **PASS** |
| Switch | disabled thumb on surfaceDisabled (cue 1) | dis | #FF9A9488 (#FF2D2E32) | 4.5:1 | disabled ≥3.0 | **PASS** |
| Focus | outlineFocus ring on surfaceInteractive | en | #FFD9B36A (#FF1B2027) | 8.27:1 | focus ≥3.0 | **PASS** |
| TruthBadge | info badge on info@10% over interactive | en | #FF22D3EE (#FF1B2027) | 9.06:1 | critical ≥4.5 | **PASS** |

### MARBLE

| component | state | enabled | fg (bg) | ratio | class | verdict |
|---|---|---|---|---|---|---|
| shell prose | textPrimary on surfaceBase | en | #FF201E19 (#FFF2EFE7) | 14.49:1 | normal ≥4.5 | **PASS** |
| shell prose | textSecondary on surfaceBase | en | #FF5A554A (#FFF2EFE7) | 6.45:1 | normal ≥4.5 | **PASS** |
| panel prose | textPrimary on surfaceRaised | en | #FF201E19 (#FFFAF7F0) | 15.56:1 | normal ≥4.5 | **PASS** |
| panel prose | textSecondary on surfaceInteractive | en | #FF5A554A (#FFE9E4D8) | 5.84:1 | normal ≥4.5 | **PASS** |
| panel prose | textTertiaryReadable on surfaceInteractive | en | #FF6B655A (#FFE9E4D8) | 4.55:1 | normal ≥4.5 | **PASS** |
| panel prose | textTechnical on surfaceRaised | en | #FF6B655A (#FFFAF7F0) | 5.4:1 | normal ≥4.5 | **PASS** |
| status text | textSuccess on surfaceRaised | en | #FF276B4D (#FFFAF7F0) | 5.95:1 | critical ≥4.5 | **PASS** |
| status text | textWarning on surfaceRaised | en | #FF8A5210 (#FFFAF7F0) | 5.96:1 | critical ≥4.5 | **PASS** |
| status text | textError on surfaceRaised | en | #FFB3261E (#FFFAF7F0) | 6.11:1 | critical ≥4.5 | **PASS** |
| status text | textInfo on surfaceRaised | en | #FF0C6478 (#FFFAF7F0) | 6.31:1 | critical ≥4.5 | **PASS** |
| SectionCard | heading gold on surfaceInteractive | en | #FF7E612F (#FFE9E4D8) | 4.55:1 | normal ≥4.5 | **PASS** |
| SectionCard | heading bronze on surfaceInteractive | en | #FF7A5C2E (#FFE9E4D8) | 4.87:1 | normal ≥4.5 | **PASS** |
| StatusPill | label on surfaceInteractive | en | #FF5A554A (#FFE9E4D8) | 5.84:1 | normal ≥4.5 | **PASS** |
| ModeChip | selected label on surfaceSelected | en | #FF3F2F14 (#FFE8D9BC) | 9.26:1 | normal ≥4.5 | **PASS** |
| ModeChip | unselected label on surfaceInteractive | en | #FF5A554A (#FFE9E4D8) | 5.84:1 | normal ≥4.5 | **PASS** |
| ModeChip | disabled label on surfaceDisabled (cue 1) | dis | #FF857E70 (#FFE9E4D8) | 3.17:1 | disabled ≥3.0 | **PASS** |
| FilledButton | enabled label on bronze | en | #FFFFF8EC (#FF7A5C2E) | 5.85:1 | normal ≥4.5 | **PASS** |
| FilledButton | disabled label on surfaceDisabled (cue 1) | dis | #FF857E70 (#FFE9E4D8) | 3.17:1 | disabled ≥3.0 | **PASS** |
| FilterChip | unselected on surfaceInteractive | en | #FF5A554A (#FFE9E4D8) | 5.84:1 | normal ≥4.5 | **PASS** |
| FilterChip | selected on surfaceSelected | en | #FF3F2F14 (#FFE8D9BC) | 9.26:1 | normal ≥4.5 | **PASS** |
| FilterChip | disabled on surfaceDisabled (cue 1) | dis | #FF857E70 (#FFE9E4D8) | 3.17:1 | disabled ≥3.0 | **PASS** |
| Switch | thumb icon on surfaceInteractive | en | #FF7A5C2E (#FFE9E4D8) | 4.87:1 | icon ≥3.0 | **PASS** |
| Switch | disabled thumb on surfaceDisabled (cue 1) | dis | #FF857E70 (#FFE9E4D8) | 3.17:1 | disabled ≥3.0 | **PASS** |
| Focus | outlineFocus ring on surfaceInteractive | en | #FF7A5C2E (#FFE9E4D8) | 4.87:1 | focus ≥3.0 | **PASS** |
| TruthBadge | info badge on info@10% over interactive | en | #FF0C6478 (#FFE9E4D8) | 5.32:1 | critical ≥4.5 | **PASS** |

### LIVE FAIL summary

None — every live semantic pair meets its threshold in both themes.
