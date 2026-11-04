# MONUMENTAL TOWER FINAL REPORT — Graph 4.6 (§2)

Directive: GRAPH 4.6 §2 — "The Oracle Tower must move substantially closer to the
provided reference … A LIVE WINDOW INTO HERMES TOWER, not a Material screen with a
tower icon." This report is the final-state record of the monumental implementation,
its measured evidence, and the honest delta against the Owner reference image.

## 1. What stands today (committed, cloud-verified)

| Layer | Implementation | Commit lineage |
| --- | --- | --- |
| ORACLE CORE (dominant center) | 5-level Canvas composition: L1 stone disc, L2 state aura, L3 outer bronze ring, L4 differentiated mechanical rings (A/B phases, 12–24 glyph teeth by budget), L5 glyph ring — state-driven phases, frozen under reduced-motion / CI_LIFECYCLE_PROFILE | Graph 3 waves → CP-G4-018 repairs |
| UPPER TOWER | pediment crown (two-gradient bronze fill + edge stroke), entablature band, 9-tick engraved inscription band (original non-lexical glyph ticks), flanking fluted columns with bronze capital + base | CP-G4-019 (41b4c35) |
| LOWER TOWER | gateway arch with bronze rim + clipped warm interior glow, five descending stairs, three-step widening plinth | CP-G4-019 + D6 repair (3eac29a) |
| BACKGROUND | obsidian/stone depth with controlled cyan atmosphere (no flat black void) | Graph 3 foundation |
| MESSENGER PATHS | cyan conduit with energy pulses driven by APP STATE ONLY (CONTACTING_TOWER / TOWER_PROCESSING / REQUEST_QUEUED); quiet carved conduit in IDLE; fully static when motion is frozen; pulse count keyed to the real quality budget (LOW 0 / BALANCED 2 / HIGH 4) | CP-G4-019 + D1/D2 repairs (3eac29a) |
| CHAT-FOCUSED KEYBOARD MODE | IME detection animates the stage to 52% — the Tower becomes a compact animated portal; the timeline and input gain the space; no state is deleted | CP-G4-019 |
| Visual quality profiles | LOW (static) / BALANCED (default) / HIGH (non-default, physically qualified on the Xiaomi only) via QualityBudget; burn-in guard with drift | Graph 3 → D2/D4 repairs |

Everything is deterministic geometry — zero bitmaps shipped, zero randomness, no
decorative timers when the state is quiet. **No reference bitmap is shipped** — the
frame is original Canvas work parameterized to the reference's architecture.

## 2. Directive feature checklist (§2, item by item)

| Required feature | Status | Where |
| --- | --- | --- |
| Monumental architecture | ✅ | full-height frame: upper order + lower structure around the core |
| Crown / pediment | ✅ | `drawUpperOrder` — pediment path, bronze gradient fill |
| Dark stone structure | ✅ | obsidian/stone palette (`MonumentalColor`), stone shafts |
| Bronze engravings | ✅ | 9-tick inscription band, bronze capitals/bases, rim engraving |
| ≥ 3 differentiated rings | ✅ | L3 outer bronze ring + L4 mechanical rings A/B (distinct phases/teeth) + L5 glyph ring — 4 ring systems, phases differ per state |
| Deeper portal | ✅ | gateway arch with bronze rim + interior glow; opening mean-L raised 9.6→14.4 with clipped glow (zero bleed, D6) |
| Columns | ✅ | fluted, with bronze capital/base |
| Gateway | ✅ | arch centered on the plinth's top step |
| Stairs | ✅ | five descending steps |
| Messenger paths | ✅ | state-driven cyan conduit + pulses |
| Energy conduits | ✅ | same system; pulse count from the real budget (D2: BAL=2, HIGH=4 observably different) |
| Atmospheric lighting | ✅ | controlled cyan atmosphere; warm interior gateway glow |
| Oracle remains dominant | ✅ | frame wraps the existing core stage; core scale `min(w/1080, h/1420)` keeps every ring inside the stage (D4) |

## 3. Required previews (all seven delivered)

Rendered as a **deterministic PIL re-render of the shipped Compose Canvas geometry**
(parameter-faithful mirror — explicitly NOT emulator screenshots; renderer:
`scripts/render_graph4_monumental_previews.py`, measurements:
`scripts/verify_graph4_postrepair.py`), post-repair pack at
`download/graph4-monumental-preview-pack/` (2026-11-04):

| Required state | File | Measured evidence (post-repair) |
| --- | --- | --- |
| IDLE | `console_idle.png` (+ `_full_column`, `_quality_low` variants) | quiet carved conduit; chip overlap 0.0 px |
| LISTENING | `console_listening.png` | aura + ring phase per state table |
| TRANSCRIBING | `console_transcribing.png` | transcript-side rings engaged |
| TOWER_PROCESSING | `console_tower_processing.png` (+ `_quality_high`) | messenger pulses 2 (BAL) vs 4 (HIGH), md5 diverged |
| SPEAKING | `console_speaking.png` | amplitude-driven aura |
| DISCONNECTED | `console_disconnected.png` | honest disconnected presentation |
| CHAT_FOCUS | `console_chat_focused_keyboard.png` | stage at 52%, ring span inside the stage (crown top row 77 > 0; D4 closed) |

Full sheet: `comparison_sheet.png` (11 states + quality variants + full column).
OLED discipline re-verified on the post-repair pack: worst mean-L 27.54, L>180 pixels
unchanged — the Tower stays a dark instrument, not a lamp.

## 4. Measured repairs (GAP doc §6, summarized)

| Defect | Pre | Post | Verdict |
| --- | --- | --- | --- |
| D1 chip occludes messenger band | 70.7% occluded | 0.0 px overlap, 100% visibility | CLOSED |
| D2 HIGH identical to BALANCED | md5 equal, 0 visible pulse cores | md5 differ, 2 vs 4 cores | CLOSED |
| D4 chat-focus ring clipping | ring span −117.8…762.2 vs 738 stage | 93.5…550.9 fully inside | CLOSED |
| D6 flat gateway opening | mean-L 9.62, warm 2.0% | mean-L 14.42, warm 9.2%, zero bleed | CLOSED |

## 5. Honest delta vs the Owner reference image

The reference (`download/Immagine_Hermes_Tower.png`) carries painterly detail the
deterministic Canvas intentionally does not copy: per-stone texture noise, volumetric
god-rays, pictorial weathering on the bronze, and a bespoke emblem crest. What the
implementation delivers instead is the reference's ARCHITECTURE (massing, order,
ring hierarchy, gateway depth, messenger energy) as live, state-driven, burn-in-safe
geometry at 60 fps within a 55–65% portrait band. A future pictorial pass (texture
shaders / AGSL) may close the residual painterly delta AFTER the physical Xiaomi
arbitration of the profiles — it is registered as visual debt, not as completed work.

## 6. Verdict

The console displays a monumental, animated, state-driven Tower architecture wrapped
around a dominant Oracle Core — a live window into Hermes Tower by construction:
every animation is the app's real state (never an idle screensaver), every quality
tier is budget-gated, and every one of the seven required previews is measured on the
re-rendered pack. Final physical arbitration (profile comfort, OLED behavior,
perceived monumentality) belongs to the Xiaomi qualification session.
