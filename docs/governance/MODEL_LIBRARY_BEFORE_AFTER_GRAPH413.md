# MODEL LIBRARY BEFORE / AFTER — GRAPH 4.13 (§27, CP-G4.13-008)

Scope: the Models and Voice presentation rebuild (CP-G4.13-005/006/007).
The underlying model authority is UNCHANGED — same catalog, registry,
receipts, download lifecycle, capability state machine, profiles, hash
verification and rollback. Nothing in this pack is Owner-graded; physical
verdicts remain Owner-only.

## Evidence classes

- BEFORE metrics: the PRE-REPAIR section of
  `APP_WIDE_RENDERED_CONTRAST_AUDIT_GRAPH413.md` (RENDERED_PAIR_COMPUTED,
  WCAG 2.x, worst-case gradient stop, alpha blended) + the Owner screenshot
  findings (OWNER_PHYSICAL_FINDINGS_GRAPH413.md §4).
- AFTER metrics: the LIVE semantic-authority section of the same audit
  (50 rows / 0 FAIL, both themes) + the JVM pins
  (`ModelUxSemanticsTest`, `SemanticContrastAuthorityTest`).
- AFTER renders: the deterministic screenshot matrix
  (`ScreenshotMatrixTest`, androidTest) — CI channel:
  workflow `android-e2e` → `workflow_dispatch` with
  `E2E_CLASS_FILTER=com.hermestower.voiceconsole.visual.ScreenshotMatrixTest`;
  PNGs land in the app `filesDir/screenshots/g413/` with `INDEX.txt` and are
  uploaded with the run artifacts. Local: `./gradlew :app:connectedDebugAndroidTest`
  against any API 30+ device.

## Per-state before / after

### 1. Configuration card (§14, §25)

| Aspect | BEFORE (Package 4 build) | AFTER (this graph) |
| --- | --- | --- |
| Device info | `mm_detected_device` prose with the RAW identifier `2311DRK48G`, one dense paragraph, measured 1.16–3.12:1 in Marble, near-invisible rows | `RecommendedConfigurationCard`: two-column key-value rows (Device class / Android / Architecture / RAM / Storage / Engine / Network / Current profile / Recommended profile), stacked under 600 dp |
| Contrast | inkDim-class text 3.17–3.63:1 (pre-4.12), rows on mixed surfaces | every row on the semantic authority: textPrimary ≥13.65:1, textSecondary ≥6.77:1 (obsidian); ≥15.56:1 / ≥5.84:1 (marble) |
| Empty space | oversized card with a large blank lower region | 9 compact rows + one 2 dp engraved divider — no blank region |
| Device identity | raw product code in ordinary UI | `DeviceClassLabel` → "POCO X6 Pro" / "Xiaomi 11 Lite 5G NE"; the raw id only in Advanced Diagnostics (§25 pin: `ModelUxSemanticsTest`) |
| Touch targets | profile chips forced into one row (FilterChip 32 dp visual) | full-width ≥48 dp segmented cards (g413_18 gate) |

### 2. Profile selector (§15/§16)

| Aspect | BEFORE | AFTER |
| --- | --- | --- |
| Layout | 4 FilterChips in ONE horizontal row at 360 dp; "Local experimental" wrapped VERTICALLY (letter columns) | vertical segmented cards, one full-width row per profile at 360–411 dp; ≥600 dp up to 3 columns (g413_17 gate: title width ≥80 dp — letter-column wrap impossible) |
| Wrapping | vertical letter wrap (Owner defect) | normal word wrap, maxLines free, card height grows (g413_17 pins width ≥80 dp × density) |
| Local experimental unavailable | shown as an ordinary selectable chip (dishonest) | disabled card + surfaceDisabled fill + outlineDisabled border + readable unavailable explanation + "Why unavailable" section (§16: ≥2 cues, never dim-only) |
| Selected indicator | chip fill only | bronze `outlineFocus` 2 dp border + `textOnSelected` "Selected" seal |

### 3. Paola voice card (VOICE, org.piper.voice.it-paola-medium)

| Aspect | BEFORE | AFTER |
| --- | --- | --- |
| Header | `mm_spec_line` wall: identity, version, provider, license, sizes, latency class, languages — no hierarchy | display name (SemiBold 15 sp textPrimary) + localized state pill; purpose line ("Una voce scaricabile che pronuncia le risposte dell'assistente."); size · RAM · language in textTechnical |
| Compatibility | "Compatibilità: false" with no reason | typed verdict: "Compatibile con questo dispositivo." / "Non ancora utilizzabile: il componente non è stato scaricato." (§19 pins) |
| Actions | 4 direct buttons (Download, Verify emphasized, Activate, Details) + 7 behind More actions — Verify was the FILLED emphasis even when Download was due | §21 resolver: exactly ONE filled action per state — DOWNLOAD (not downloaded) → VERIFY (downloaded) → INSTALL (hash verified) → CHECK_RUNTIME (installed) → RUN_FUNCTIONAL_TEST → ACTIVATE → TEST (active, hear-sample real audio path); pause/resume/cancel state-gated; the rest behind More actions |

### 4. Qwen card (LOCAL_LLM, optional local intelligence §13-E)

| Aspect | BEFORE | AFTER |
| --- | --- | --- |
| Section | buried in the flat catalog | its own "Optional local intelligence" section |
| Honest state | classification raw enum text | "Non ancora utilizzabile…" + RUNTIME_UNAVAILABLE verdict when no verified runtime exists; the LLM card never advertises Download as available while the runtime seam is `LLM_RUNTIME_PENDING_VERIFIED_SOURCE` |
| Privacy | — | purpose line states optional/experimental; no download pressure |

### 5. Installed voice state / Active voice state (§13-F)

| Aspect | BEFORE | AFTER |
| --- | --- | --- |
| State text | `record.state` raw (`INSTALLED`, `ACTIVE`) inside mm_component_line | `ModelStatePill` → "Installato" / "Attiva" (StateLabels, 5 locales, §18 pin) |
| Verdict | no explanation | "Installato e pronto; attualmente non attivo." (INACTIVE_READY) / "Compatibile con questo dispositivo." (IN_USE) |

### 6. Compact profile selection at 360 dp (§22/§23)

- Available width 360 dp − 32 dp screen padding − 28 dp card padding = 300 dp content width.
- Each profile card: full 300 dp width, ≥48 dp touch height (title+body+requirements ≈ 108–132 dp), 2 dp selected border, 8 dp inter-card gap (§23 spacing scale).
- Primary selector scrolls vertically; no horizontal scrolling anywhere (g413_01 clipping gate).

## Required render list (CI screenshot channel)

| State | Capture | Gate |
| --- | --- | --- |
| Model Library initial | g413_01..04 (360/393/411/600 dp) | blank-render + clipping |
| Model Library font 1.3 / 1.5 | g413_05..06 | clipping at large font |
| Configuration card | g413_07..09 | clipping at the three compact widths |
| Profile selector letter-wrap | g413_17 | width ≥80 dp per title |
| Profile selector 48 dp | g413_18 | card height |
| Voice and Audio | g413_10..11 | blank-render + clipping |
| Console idle | g413_12..13 | blank-render + clipping |
| Appearance / Self-Test | g413_14..15 | blank-render + clipping |

The §9 gates that complete the matrix with the Block 3 integration:
Console LISTENING / ERROR, Tower LOW / BALANCED (join at the Tower
checkpoint), quarantine state, insufficient RAM, offline state (drive via
the bridge controls during the visual run).

## Verdict

- Raw enum rendering in ordinary UI: ELIMINATED (test-pinned).
- "Compatibilità: false": ELIMINATED (typed verdicts, test-pinned).
- One-row profile forcing at compact width: ELIMINATED (layout + gates).
- Vertical letter wrapping: gated (width ≥80 dp per profile title).
- One filled primary action per card: enforced by the resolver + wrappers.
- Every metric above is computed or pinned; every physical verdict remains
  Owner-only.
