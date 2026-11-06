# OWNER UI LAYOUT DEFECT LEDGER — Owner Physical Finding 2 (POCO X6 PRO)

Date: 2026-11-06 · Branch: `graph-4/real-voice-pairing-archive-e2e` · Repair commit: `UI_POLISH_PASS` (CP-G4-027)
Intake receipt: `docs/receipts/OWNER_PHYSICAL_FEEDBACK_RECEIPT.md`

## 0. Context and evidence policy

The Owner reported, on the physical device: "Several secondary screens and settings
submenus show layout and spacing problems — incorrect text layout; imperfect spacing;
visual polish issues; some screens feel unfinished compared with the main experience."

**No screenshots were provided with the briefing** — every `screenshot reference`
field below is typed `PENDING_OWNER_CAPTURE` and is completed in the second Owner
validation cycle (the ledger stays open until then). The root-cause hypotheses below
are NOT guesses about pixels: each one is a **code-proven overflow/spacing mechanism**
found by auditing the layout code against the report's conditions (POCO X6 PRO width
class ≈ 360–412 dp, localized Italian/German label lengths, portrait). Screen size,
language and orientation are recorded per the Owner device and the audit scope.

Device: POCO X6 PRO — 6.67", 2712×1220 px, ~446 dpi → canonical width ≈ 372 dp
(long Italian/German strings overflow it; the mechanisms below are width-independent).

## 1. Systemic root-cause classes (code-proven)

| Class | Mechanism | Where |
| --- | --- | --- |
| `BADGE_ROW_OVERFLOW` | Status/badge/button rows are fixed horizontal `Row`s that CANNOT wrap; long localized labels (e.g. `li_load_badge` = "CARICA — BLOCCATO FINCHÉ MANCA UN RUNTIME VERIFICATO", ≈50 chars uppercase) overflow the screen edge and clip | 8 screens (see ledger rows) |
| `UNWEIGHTED_TEXT_COLUMN` | Text columns inside full-width rows have no `weight(1f)`, so long localized subtitles push the row to overflow and there is no end margin | Settings hub rows, SectionCard headers |
| `ROW_DENSITY_UNDERSIZED` | Settings rows have no minimum height / vertical padding — cramped against the 48 dp touch-target floor | Settings hub rows |

## 2. Per-screen ledger

### S1 — Settings hub

| Field | Value |
| --- | --- |
| Route | `settings` (SettingsHubScreen) |
| Screenshot reference | PENDING_OWNER_CAPTURE |
| Screen size | POCO X6 PRO ≈372 dp width class |
| Language | it (audit also covers en/de) |
| Orientation | portrait (audit) |
| Root-cause hypothesis | `UNWEIGHTED_TEXT_COLUMN` + `ROW_DENSITY_UNDERSIZED`: SettingsRow's text column had no weight, no end padding, no min height — long Italian subtitles ("Salute del dispositivo, test bridge, log offuscatti") crowd the right edge and rows sit cramped |
| Fix | text column `weight(1f)` + end padding 8 dp + `heightIn(min = 56.dp)` + vertical padding 8 dp |
| Verification status | REPAIRED_IN_CODE — owner re-check on `0.3.0-alpha-graph4` |

### S2 — Voice & Audio

| Field | Value |
| --- | --- |
| Route | `voice` (VoiceAudioScreen) |
| Screenshot reference | PENDING_OWNER_CAPTURE |
| Screen size | POCO X6 PRO ≈372 dp width class |
| Language | it (Owner's session language) |
| Orientation | portrait (audit) |
| Root-cause hypothesis | `BADGE_ROW_OVERFLOW`: wake card ("PROVIDER: HERMES CANDIDATI" + "MOTORE: …") and TTS card badge pairs in non-wrapping Rows; Italian/German overflow at ≈372 dp |
| Fix | both badge rows → `BadgeFlowRow` (wraps); NEW Italian-voice card uses BadgeFlowRow from birth |
| Verification status | REPAIRED_IN_CODE — owner re-check on `0.3.0-alpha-graph4` |

### S3 — Local Intelligence internals

| Field | Value |
| --- | --- |
| Route | `intelligence` (LocalIntelligenceScreen) |
| Screenshot reference | PENDING_OWNER_CAPTURE |
| Screen size | POCO X6 PRO ≈372 dp width class |
| Language | it |
| Orientation | portrait (audit) |
| Root-cause hypothesis | `BADGE_ROW_OVERFLOW`, worst screen: `li_load_badge` "CARICA — BLOCCATO FINCHÉ MANCA UN RUNTIME VERIFICATO" + second badge in a non-wrapping Row; `li_runtime_none` "RUNTIME: NESSUNO INSTALLATO" pair; LLM card pair — all clip at ≈372 dp |
| Fix | 4 rows → `BadgeFlowRow` (state/runtime pair ×2, load/unload badges, LLM action buttons) |
| Verification status | REPAIRED_IN_CODE — owner re-check on `0.3.0-alpha-graph4` |

### S4 — Connection

| Field | Value |
| --- | --- |
| Route | `connection` (ConnectionScreen) |
| Screenshot reference | PENDING_OWNER_CAPTURE |
| Screen size | POCO X6 PRO ≈372 dp width class |
| Language | it |
| Orientation | portrait (audit) |
| Root-cause hypothesis | `BADGE_ROW_OVERFLOW`: mock/adapter pair + the TLS composite badge ("TLS N/A (MOCK)" built by string concatenation) in non-wrapping Rows |
| Fix | 3 rows → `BadgeFlowRow` |
| Verification status | REPAIRED_IN_CODE — owner re-check on `0.3.0-alpha-graph4` |

### S5 — History

| Field | Value |
| --- | --- |
| Route | `history` (HistoryScreen) |
| Screenshot reference | PENDING_OWNER_CAPTURE |
| Screen size | POCO X6 PRO ≈372 dp width class |
| Language | it |
| Orientation | portrait (audit) |
| Root-cause hypothesis | `BADGE_ROW_OVERFLOW`: per-turn badges (SOURCE + DELIVERY + CONSEQUENTIAL + receiptRef hex) in one non-wrapping Row — receipt refs are long hashes, guaranteed clip |
| Fix | turn badge row → `BadgeFlowRow`; filter chip row → `BadgeFlowRow` |
| Verification status | REPAIRED_IN_CODE — owner re-check on `0.3.0-alpha-graph4` |

### S6 — Archive

| Field | Value |
| --- | --- |
| Route | `archive` (ArchiveScreen) |
| Screenshot reference | PENDING_OWNER_CAPTURE |
| Screen size | POCO X6 PRO ≈372 dp width class |
| Language | it |
| Orientation | portrait (audit) |
| Root-cause hypothesis | `BADGE_ROW_OVERFLOW`: transcript actions ("Apri trascrizione" + "Rimuovi pin" TextButtons + PINNATO badge + mission link) in one non-wrapping Row; artifact action row (3 TextButtons) same |
| Fix | 3 rows → `BadgeFlowRow` (filter chips, transcript actions, artifact actions) |
| Verification status | REPAIRED_IN_CODE — owner re-check on `0.3.0-alpha-graph4` |

### S7 — Diagnostics

| Field | Value |
| --- | --- |
| Route | `diagnostics` (DiagnosticsScreen) |
| Screenshot reference | PENDING_OWNER_CAPTURE |
| Screen size | POCO X6 PRO ≈372 dp width class |
| Language | it |
| Orientation | portrait (audit) |
| Root-cause hypothesis | `BADGE_ROW_OVERFLOW` (minor): studio/self-test links, service badge, bridge-test buttons in non-wrapping Rows; NEW voice read-back rows added this cycle inherit correct wrapping from birth |
| Fix | 3 rows → `BadgeFlowRow` |
| Verification status | REPAIRED_IN_CODE — owner re-check on `0.3.0-alpha-graph4` |

### S8 — About & safety

| Field | Value |
| --- | --- |
| Route | `about` (AboutSafetyScreen) |
| Screenshot reference | PENDING_OWNER_CAPTURE |
| Screen size | POCO X6 PRO ≈372 dp width class |
| Language | it |
| Orientation | portrait (audit) |
| Root-cause hypothesis | Single top-level badges (no pair-overflow found); listed as LOW risk — no row repair required; SectionCard header weight applies |
| Fix | none required (audited; SectionCard header weight is systemic) |
| Verification status | AUDITED_NO_DEFECT_BEYOND_SYSTEMIC |

### S9 — Onboarding (step 1 language)

| Field | Value |
| --- | --- |
| Route | `onboarding` (OnboardingScreen) |
| Screenshot reference | PENDING_OWNER_CAPTURE |
| Screen size | POCO X6 PRO ≈372 dp width class |
| Language | any (chips are endonyms: Italiano/English/Español/Français/Deutsch) |
| Orientation | portrait (audit) |
| Root-cause hypothesis | `BADGE_ROW_OVERFLOW`: five FilterChips in one non-wrapping Row — overflows ≈372 dp in EVERY language |
| Fix | language chip row → `BadgeFlowRow` |
| Verification status | REPAIRED_IN_CODE — owner re-check on `0.3.0-alpha-graph4` |

### S10 — All SectionCards (systemic header check)

| Field | Value |
| --- | --- |
| Route | all screens using SectionCard (12 secondary surfaces) |
| Screenshot reference | PENDING_OWNER_CAPTURE |
| Screen size | POCO X6 PRO ≈372 dp width class |
| Language | it |
| Orientation | portrait (audit) |
| Root-cause hypothesis | `UNWEIGHTED_TEXT_COLUMN`: SectionCard's title column could collide with a trailing element on long localized titles |
| Fix | title column `weight(1f)` in Chrome.kt — systemic, one repair covers all surfaces |
| Verification status | REPAIRED_IN_CODE (systemic) |

## 3. Honesty boundaries

- The Owner's exact affected-screen list was not provided; the ledger covers EVERY
  secondary screen + settings submenu reached by the audit (S1–S10). If the second
  cycle's screenshots reveal a screen outside this list, a new row is appended —
  the ledger stays open until terminality.
- "Visual polish issues" beyond layout mechanics (colors, motion) are NOT claimed
  repaired — only the code-proven layout/spacing defects are.
- No emulator screenshot is offered as substitute evidence.
