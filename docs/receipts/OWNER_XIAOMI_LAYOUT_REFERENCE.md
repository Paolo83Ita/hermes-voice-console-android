# OWNER XIAOMI LAYOUT REFERENCE — GRAPH 4.10 (§24, CP-G4.10-008/009)

Evidence class: OWNER PHYSICAL screenshots (Xiaomi 11 Lite 5G NE) supersede
emulator assumptions. This document is the regression reference the repaired
layout must satisfy on the next physical pass; each row is verified by the
measured-layout contract tests (`ResponsiveLayoutContractTest`) and by the
rebuilt screens. OWNER VISUAL/UI verdicts remain Owner-only.

## Measured width authority (§19)

| Item | Value |
| --- | --- |
| Xiaomi 11 Lite 5G NE measured available width | COMPACT or MEDIUM (never by physical pixels) |
| Class boundaries | COMPACT < 380 dp; MEDIUM 380–420 dp; LARGE > 420 dp |
| Authority | `ui/components/ConsoleWindowWidthClass.kt` (after insets + padding) |

## Required layout tests

| # | Screen / state | Requirement |
| --- | --- | --- |
| 1 | Connection Studio at compact width | PRIMARY "Salva e connetti" full-width; secondary/management actions STACK (one per row at COMPACT); destructive below a divider; ≥48 dp height; max 2 lines, centered; **no one-character-per-line text** ("Ripristina configurazione precedente" renders whole) |
| 2 | Console Italian chat | timeline text wraps normally; per-message "Ripeti" ≥48 dp; composer labels Parla / Interrompi / Invia |
| 3 | Voice and Audio diagnostics | listening-mode radio rows ≥48 dp; session-limit chips wrap; TTS diagnostic card readable at 360 dp |
| 4 | Model Sanctum action group | AdaptiveActionsRow (Graph 4.9 §authority) preserved: 48 dp, maxLines 2, "Altre azioni" overflow; responsive report MODEL_SANCTUM_RESPONSIVE_REPORT.md remains the Sanctum contract |
| 5 | Continuous listening | persistent "Ascolto continuo attivo" indicator; Hearing medallion distinct from mic controls |
| 6 | Chat speech muted | VolumeOff + "Attiva risposte vocali"; text stays visible; mic unchanged |

## Assertions (binding, from the Owner screenshots)

- no overlap between header and next panel (header has no fixed height);
- no character-per-line text anywhere (ResponsiveActionGroup stacking);
- no clipped title (max 2 lines + ellipsis + reduced font at COMPACT);
- no raw English enum ("Invalid" → "Non valida" via `StateLabels`);
- no excessive empty action container (rows built from measured content);
- controls above the navigation bar (no fixed-height bottom stacks);
- scrolling reaches every action (vertical scroll preserved);
- the PRIMARY action is obvious (single filled full-width button).

## Screens replaced by the responsive layer

| Screen | Change |
| --- | --- |
| Connection (Tower Studio) | §21 rearchitecture: PRIMARY + secondary + management + destructive via `ResponsiveActionGroup`; §23 labels; BoxWithConstraints width class |
| 11 route screens | §20 `ResponsiveScreenHeader` (compact font, ≤2 lines, statusBarsPadding, menu preserved, tracking removed at fontScale ≥ 1.30) |
| Console | §18 hierarchy: 1 primary + 4 secondary (privacy mute, speaker, continuous, repeat) + "Altre azioni" overflow |
| Voice & Audio | §17 listening-mode card with ≥48 dp rows and chip wrap |

## Status

- Layout repair implemented and unit-contracted; **`RESPONSIVE_UI_QUALIFIED`
  remains UNCLAIMED** (Graph 4.9 forbidden token) — only the Owner's physical
  pass on the Xiaomi can qualify the layout.
- Physical screenshot evidence will be appended to
  `OWNER_POCO_XIAOMI_RETEST_PACKAGE_3` when the Owner runs the retest.
