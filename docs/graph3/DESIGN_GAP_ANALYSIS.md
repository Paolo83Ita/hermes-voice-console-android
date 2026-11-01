# DESIGN GAP ANALYSIS — V0.1 Visual Direction → Monumental Oracle Tower

Graph: **GRAPH 3 — MONUMENTAL ORACLE TOWER VISUAL REFOUNDATION (V1)**
Owner: Paolo Lombisani · Status of the decision: **OWNER_AUTHORIZED_FOR_IMPLEMENTATION**
Supersession record: `DECISION_LOG.md` → `MONUMENTAL_ORACLE_TOWER_VISUAL_REFOUNDATION`
Reference (direction only, not a shippable asset): `upload/Immagine_Hermes_Tower.png`

---

## 1. Owner verdict recorded verbatim (governance copy)

The existing V0.1 visual direction is **rejected for visual identity**. The current
implementation may be technically valid, accessible and coherent, but it is materially too:
minimal, sparse, flat, generic, card-oriented, visually lightweight, dependent on a simple
column, distant from the attached Hermes Tower reference. The V0.1 UI interpreted
techno-Greek as a restrained modern interface; the intended identity is **a live window
into the central core of Hermes Tower**.

Typed record: `V0_1_MINIMAL_TECHNO_GREEK_VISUAL_DIRECTION → OWNER_REJECTED_FOR_VISUAL_IDENTITY`
(superseded baseline; its evidence is preserved, never deleted — INV-visual-01).

## 2. Element-by-element gap matrix

| # | Area | V0.1 implementation (physical, file-verified) | Authorized direction | Gap type |
|---|------|-----------------------------------------------|----------------------|----------|
| 1 | Hero subject | `TowerCore.kt`: ONE Canvas, a single fluted column + plinth + diamond beacon (~45% of a 310 dp box inside a scrollable column) | Multilayer monumental Oracle Core, 55–65% of screen, 10 architectural layers L0–L9 | STRUCTURAL |
| 2 | Top identity | Material `TopAppBar` + "HERMES" text + `MeanderBand` | Monumental HERMES TOWER frieze: engraved gold title, subtitle, command frieze of seals; explicitly NOT a Material app bar | IDENTITY |
| 3 | Status display | `StatusPill` / `ModeChip` generic pills in a Row | Engraved seals / bronze status plates / sigils in an architectural command frieze | COMPONENT |
| 4 | Panels | `SectionCard` = Material card (border + rounded rect) | Carved-stone / bronze-edged obsidian / temple-glass architectural panels | MATERIAL |
| 5 | Buttons | Material `IconButton` + thin circle borders | Control medallions / portal controls / engraved command seals | COMPONENT |
| 6 | Navigation | `ModalNavigationDrawer` with plain rows + Material icons | Architectural navigation chamber with bronze/stone section hierarchy | IDENTITY |
| 7 | Color depth | `ObsidianNight` palette: flat fills, 1 cyan energy, minimal gradient use | Obsidian reflections, aged bronze with engraved depth, marble used selectively, 6-role energy system (cyan core / tower blue / local cyan-gold / amber / red-bronze / role colors) | MATERIAL |
| 8 | Typography | Default serif brand + default sans body | Lapidary display capitals (Cinzel-class) for identity text; engraved gold treatment; body remains readable sans | TYPOGRAPHIC |
| 9 | Messenger concept | None (energy streams/diamonds only) | Original messenger language: winged geometric sigils, light-scroll packets, energy glyphs, directional trails, orbiting seals — bound to real events | MISSING SYSTEM |
| 10 | Depth & atmosphere | Flat single-layer drawing | Layered depth: sky, rear temple, colonnade, rings, conduits, portal, stairs/gateway, state FX, messengers | STRUCTURAL |
| 11 | Motion profiles | `animationIntensity` float + reducedMotion flag | Three named profiles LOW / BALANCED / HIGH with thermal policy and automatic fallback | CAPABILITY |
| 12 | Burn-in guard | Whole-console 20 s drift (±4 dp) | Per-region safeguard (title, core, seals, transcript, medallions) + scheduled dimming + black idle option | CAPABILITY |

## 3. What is explicitly preserved (preservation contract REUSE_AND_EXTEND)

- 15-state truthful machine (`ConsoleState`, `ConsoleStateMachine`), operating modes
  OFF/LOCAL/TOWER/HYBRID/DISCONNECTED_SAFE, driver, queue, privacy mute.
- Voice Model Manager (catalog/resolver/download/install/activation/rollback), Tower
  Connection Studio (profiles/validator/vault/Real adapter/mock server), Self-Test Center
  (ST-01..ST-45), migration, diagnostics, redaction.
- Accessibility semantics (state.accessibility strings, live regions, 48 dp targets,
  TalkBack labels), reduced-motion static equivalents, contrast discipline.
- All tests (unit + E2E-01..45 + vault instrumented), workflows, governance authorities.
- E2E-pinned UI strings and content descriptions are preserved verbatim (see §5).

## 4. Risk register for the refoundation

| Risk | Mitigation |
|------|-----------|
| Regressing Graph 2 functionality while reskinning | Screens keep their function graph; only visual composables are replaced; Graph 2 E2E suite stays green (Wave 7/8 gates) |
| One unmaintainable mega-Composable | Layered token system + small components + asset manifest (mission §13) |
| OLED load from cinematic brightness | State-budgeted luminance (`bd` budget per state), idle LOW, burn-in offsets, scheduled dimming (mission §15) |
| Fake data leaking into UI | Previews and app show only real catalog entries / real state values; simulated values are labeled |
| Performance on SD 778G-class | LOW profile default fallback path, particle caps, no AGSL before device qualification (TARGET_PENDING_DEVICE_QUALIFICATION) |

## 5. Pinned strings the redesign must not lose (extracted from androidTest sources)

`HERMES` · `Open navigation drawer` (contentDescription) · `Open Hermes Tower Connection
Studio` · `Reset to Mock mode` · `Save and connect` · `Test connection` · `Hybrid balanced`
· `A. Recommended setup` · `B. Installed voice components` · `C. Available components`
· `Manual advanced` · `Apply Recommended Configuration` · `Run Quick` · `Run Full`
· `Export Secret-Clean Report` · `Clear sensitive values`.
Architectural names appear as subtitles; the plain functional titles stay accessible and
searchable (mission §12).

## 6. Composition budget adopted (1080 × 2400 target, Xiaomi 11 Lite 5G NE)

| Region | Height | Share | Content |
|--------|--------|-------|---------|
| Command frieze | 330 px | 13.75% | title, subtitle, mode + status seals |
| Oracle Core stage | 1420 px | 59.2% | L0–L9 monumental core |
| Lower console | 650 px | 27.1% | transcript, attribution, captions, medallions |

Matches mission §6 (top 10–15%, central 55–65%, lower 20–30%).

## 7. Verdict

The gap is **structural and identity-level, not a reskin**: 12 gap classes, 2 missing
systems (messenger language, quality/thermal policy). The preview pack
(`design/monumental/preview/`, 11 renders, verification report JSON) demonstrates the
authorized direction against the real Graph 2 feature set before any production UI change.
