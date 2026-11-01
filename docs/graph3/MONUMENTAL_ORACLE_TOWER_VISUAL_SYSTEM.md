# MONUMENTAL ORACLE TOWER VISUAL SYSTEM

Graph 3 · status OWNER_AUTHORIZED_FOR_IMPLEMENTATION · supersedes
`V0_1_MINIMAL_TECHNO_GREEK_VISUAL_DIRECTION` (semantic supersession, evidence preserved).

## 1. Design thesis

The Android display is **a live window into the central core of Hermes Tower**: a
monumental techno-Hellenic temple housing a mechanical oracle. Voice is expressed through
architecture — resonance, portal luminosity, moving rings, illuminated colonnades, energy
waves, messenger paths — never through a face or lips.

## 2. Material system (tokens)

| Material | Tokens | Usage |
|----------|--------|-------|
| OBSIDIAN | `obsidianDeep #04060A`, `obsidian #080B12`, `obsidianPanel #0D1119`, `obsidianRaised #131824`, `obsidianSheen #16202E` | backgrounds, panels; deep blue-black reflections via subtle gradients |
| AGED BRONZE | `bronzeDeep #3E2F18`, `bronze #6B5127`, `bronzeEdge #8A6A38`, `bronzeLight #B98C4A` | rings, edges, frames; engraved, never flat orange fill |
| GOLD | `gold #D9B36A`, `goldBright #F0D9A0` | inscriptions, active seals, sigil strokes |
| CARVED STONE | `stoneDeep #191611`, `stone #221D15`, `stoneMid #2C261B`, `stoneEdge #3A3225`, `stoneLit #4A4130` | columns, stairs, temple mass |
| MARBLE | `marble #EFE7D8` | selective accents only; never the dominant always-on background |
| ENERGY | `cyanCore #35E0FF`, `cyan #1FC2E8`, `cyanDeep #0E7E9C`, `cyanAbyss #0A3E52`, `towerBlue #4C8DFF`, `localGold #E8C87B`, `localCyan #7CE7D8`, `amber #E8A33D`, `redBronze #C4553B` | state-driven; violet/green/orange only for real differentiated roles |

## 3. Typography

- Display/identity: lapidary capitals (Cinzel-class serif; on-device fallback
  `FontFamily.Serif` with wide letterspacing until a bundled display face is licensed).
- Body: default sans (Inter-class), 15–17 sp.
- Inscriptions: letterspaced capitalis, engraved via dark top-edge shadow + gold fill.
- All text keeps minimum contrast per `verify_graph3_previews.py` (WCAG ≥4.5:1 body,
  ≥3:1 large/graphical).

## 4. Shape language

- Seals: rounded rectangles (6–8 dp radius) with bronze borders and inset glow.
- Panels: architectural panels — bronze/stone border, corner marks (◤◥), layered
  obsidian gradient fill.
- Medallions: circles with double ring, inset shadow, sigil center; 96/128 dp.
- Fractures: polyline breaks (ERROR), dashed conduits (DEGRADED) — no aggressive flashing.

## 5. Component inventory (Waves 1 & 4)

MonumentalColorSystem, MonumentalTypography, MonumentalShapes, MaterialTextureTokens,
ArchitecturalDepthTokens, EnergyTokens, MotionProfiles, ThermalVisualPolicy,
BurnInOffsetController, CommandFrieze, ArchitecturalPanel, StatusSeal, ControlMedallion,
TempleNavigation, MessengerPath, QueueSeal, PrivacyGate, ErrorFracture, OracleCore.

## 6. Messenger system (original language — figures NOT copied)

| Element | Form | Trigger (real events only) |
|---------|------|---------------------------|
| Winged sigil | diamond body + two chevron wing pairs | a request/response in flight |
| Light-scroll packet | amber suspended scroll seal | REQUEST_QUEUED (real queue count) |
| Departure trail | dashed cubic path toward upper edge, tower-blue | CONTACTING_TOWER / TOWER_PROCESSING |
| Arrival trail | dashed path inward, local-cyan | response delivery (SPEAKING) |
| Orbiting seal | small sigil orbiting portal | LOCAL processing (UNDERSTANDING_LOCAL) |
| Broken path | two severed segments + red-bronze arrowheads | DISCONNECTED |
| Degraded trail | sparse dashes | DEGRADED |
| Validation return | small sigil completing orbit | mock bridge ACK (deterministic) |

Role labels limited to: LOCAL CORE · TOWER · PRIMARY · VALIDATOR · JUDGE · SPECIALIST.
Real model names (from the real catalog) always show their real state:
AVAILABLE / QUALIFIED / SIMULATED / OFFLINE / UNKNOWN / UNQUALIFIED.

## 7. Screen naming (function first, architecture as subtitle)

Console → "Oracle Core" · History → "Archive chamber" · Connection/Tower Studio →
"Tower gate" · Voice Models → "Oracle library" · Self-Test → "Trial chamber" ·
Voice & Audio → "Resonance chamber" · Local Intelligence → "Local Core chamber" ·
Appearance → "Architectural forge" · Diagnostics → "Mechanism inspection" ·
About & Safety → "Foundation chamber". Plain functional titles remain primary for
navigation, search and TalkBack.

## 8. Precedence over Material 3

Material 3 remains the functional foundation (components, ripple, semantics) but never the
visual identity: no default top app bar on the console, no default card look, no default
button shapes on identity controls.
