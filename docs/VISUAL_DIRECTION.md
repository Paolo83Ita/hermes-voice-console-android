# VISUAL DIRECTION — techno-Greek identity, Marble Light vs Obsidian Night

| | |
|---|---|
| Owner | Paolo Lombisani |
| Version | v0.1.0-alpha |
| Date | 2026-10-30 |
| Status | For Owner review |
| Previews | `design/mockups/mockup_marble_light.png`, `mockup_obsidian_night.png`, `mockup_comparison.png` (annotated HTML sources alongside; rendered with headless Chromium — these are design mockups, not app screenshots) |

## 1. Identity: "Techno-Greek"

The console must feel like the physical voice of Hermes Tower: **monumental geometry, noble materials, honest energy**.

- **Architecture**: a central Tower — stylobate steps, fluted shaft, capital, and a rotating beacon "core" — carries the composition. A thin pediment line crowns the screen.
- **Materials**: white marble (light theme) or dark obsidian (dark theme); stone tones for structure; never pure gray plastic.
- **Details**: bronze/gold only for identity, mode selection and hierarchy; cyan only for *real* activity; amber only for QUEUED/ERROR; red only for failures and the mute emblem.
- **Greek motifs, sparingly**: exactly one meander band per screen zone (header divider), fluting only on the Tower shaft, a pediment line at the top. No pattern noise, no zodiac kitsch.
- **Motion**: energy is meaning — idle pulse, listening rings, transcribing sweep, tower stream, processing orbit, speaking illumination. Full spec: UI_STATE_MACHINE.md §2/§5.

## 2. Palettes

### Marble Light — daylight / inspection

| Token | Hex | Use |
|---|---|---|
| background | #F2EFE7 | warm marble field |
| surface / surfaceVariant | #FAF7F0 / #E9E4D8 | cards, strips |
| outline | #C9BFA9 | hairlines |
| ink / inkSecondary | #201E19 / #5A554A | text |
| bronze / gold | #7A5C2E / #B08D4C | identity, mode chips, controls |
| energy / energyBright | #0E7490 / #06B6D4 | text-level cyan / beacon & rings |
| stone / stoneShade | #D9D4C8 / #C2BBAB | Tower structure |
| success / warning / error | #2A7654 / #9A5B12 / #B3261E | truthful status |

### Obsidian Night — always-on default (D-009)

| Token | Hex | Use |
|---|---|---|
| background | #0B0D10 | obsidian field |
| surface / surfaceVariant | #12151A / #1B2027 | cards, strips |
| outline | #2E3640 | hairlines |
| ink / inkSecondary | #E8E6E1 / #A7A39A | text |
| bronze / gold | #C08A3E / #D4A24E | identity, mode chips, controls |
| energy / energyBright | #22D3EE / #35D6FF | text-level cyan / beacon & glow |
| stone / stoneLight | #222933 / #333D4A | Tower structure |
| success / warning / error | #34D399 / #F59E0B / #F87171 | truthful status |

## 3. Verified contrast (WCAG 2.1, computed — `scripts/contrast_check.py`)

| Pair | Ratio | Requirement |
|---|---|---|
| ML ink on background | 14.49:1 | 4.5 ✅ |
| ML secondary on surface | 6.93:1 | 4.5 ✅ |
| ML bronze on background | 5.38:1 | 4.5 ✅ |
| ML energy on surface | 5.01:1 | 4.5 ✅ |
| ML error / warning on background | 5.69 / 4.71 | 4.5 ✅ |
| ML success on background | 4.79:1 | 4.5 ✅ |
| ON ink on background | 15.60:1 | 4.5 ✅ |
| ON secondary on surface | 7.27:1 | 4.5 ✅ |
| ON energy on background | 10.77:1 | 4.5 ✅ |
| ON error / warning / success on background | 7.03 / 9.06 / 10.12 | 4.5 ✅ |
| ON bronze (large/UI) on background | 6.44:1 | 3.0 ✅ |

All body-text pairs ≥ 4.5:1 and all large/UI pairs ≥ 3:1 in both themes. The initial ML success green (#2E7D5B, 4.35:1) was **rejected** and replaced by #2A7654 (4.79:1) before any code was written.

## 4. Theme comparison and default-identity decision

| Criterion | Marble Light | Obsidian Night |
|---|---|---|
| AMOLED power (always-on) | High — bright static fields | **Low — dark pixels are off pixels** |
| Burn-in stress (A-10/R-02) | Higher | **Lower** |
| Heat in passive enclosure (R-14) | More display heat | **Less** |
| Night ambience | Bright beacon in dark rooms | **Calm, restrained glow** |
| Daylight legibility | **Excellent (14.49:1)** | Good (15.60:1; reflections possible) |
| Identity impact | Classical, airy, public-facing | **Monumental, premium; energy reads brighter** |
| Glance privacy | Transcript visible at distance | **Dark surfaces mask idle content** |

**Decision D-009 — Obsidian Night is the default identity.** It wins every criterion that matters for a permanently displayed, charging, passively enclosed AMOLED device. **Marble Light remains the daylight/inspection theme**, and the identity survives the switch because palette architecture, motifs, motion and the state language are identical. A time-based auto switch (Owner-configurable, never forced) is proposed for V0.2. Full annotated comparison: `mockup_comparison.png`.

## 5. Component inventory (what the mockups preview and the Compose code implements)

Pediment + brand · meander divider · mode chip row · connection/mic/queue pills · Tower core (stylobate, fluted shaft, capital, beacon) · state chip · transcript card with OWNER/LOCAL/TOWER attribution · streaming caption line · circular control row (mute, wake/action, stop) · state tiles (IDLE, PRIVACY_MUTED, DISCONNECTED) · palette & typography specimens.

## 6. Typography

V0.1 uses system families behind tokens (decision D-010): serif display for the Word (brand, captions), humanist sans for body, monospace uppercase for status chips — letter-spacing ≥ 0.14 em, sizes 8.5–24 sp. Bundled OFL-verified typefaces (e.g., a Cormorant-class display + Inter-class UI) are a V0.2 evaluation; the token layer makes that a one-file swap.

## 7. Mockup honesty statement

The PNG previews are **design mockups** rendered from annotated HTML — they demonstrate the identity, palettes, motifs, layout hierarchy and several states (IDLE, LISTENING, TOWER_PROCESSING, PRIVACY_MUTED, DISCONNECTED). They are not app screenshots; the sample conversation lines are illustrative and labeled as mockups on the artwork itself. The truthful-state rule applies to the application: the prototype renders only what the simulator actually did.
