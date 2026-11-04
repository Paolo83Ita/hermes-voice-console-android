# MONUMENTAL TOWER VISUAL GAP — GRAPH 4 FINAL (directive §8 gate)

| | |
|---|---|
| **Gate** | GRAPH 4 directive §8 — visual-truth gate before full legacy-visual replacement |
| **Date** | 2026-11-04 |
| **Commit** | `145ee08` (branch `graph-4/real-voice-pairing-archive-e2e`) |
| **Render pack** | `/home/z/my-project/download/graph4-monumental-preview-pack/` (23 PNG + comparison sheet) |
| **Renderer** | `/home/z/my-project/scripts/render_graph4_monumental_previews.py` (deterministic PIL, no matplotlib, no network) |
| **App source touched by this audit** | **NONE** — no file under `app/` was created, modified or deleted (verified via `git status`: only the pre-existing `LocalizationParityTest.kt` working-tree edit remains, untouched by this task) |

## 1. Render method disclosure (honesty contract)

These previews are a **deterministic PIL re-render of the shipped Compose Canvas geometry** — a faithful parameter mirror, **not emulator screenshots**. The renderer re-implements, value-for-value:

- `OracleCore.kt` `drawOracleCore` — L0 atmosphere, L1 temple, L2 colonnade, L3 outer bronze ring (r=440, 72 ticks, 4 cardinal bosses), L4 mechanical rings (r=352 rotor with teeth+8 windows; r=270 with 12 sockets), L5 conduits, L6 portal (r=166) + Hermes sigil + shutters/waves/fracture, L7 stairs/gateway glow, L8 seals, L9 messenger modes;
- the shipped WebP depth layers `monumental_l0_sky / l1_temple / l2_colonnade / l7_stairs` composited exactly as `QualityBudgets` BALANCED/HIGH does (`bitmapLayers=true`); LOW uses the same geometry via the Canvas-fallback path;
- `TowerArchitecture.kt` `drawTowerFrame` — pediment at `band h*0.115`, entablature, 9 non-lexical gold glyph ticks, flanking columns at `w*0.085 / w*0.915`, 3-step plinth, gateway arch at `h*0.70 + (h-top)*0.16`, 5 frame stairs, messenger conduit `h*0.86 → h*0.985` with pulses **only** in CONTACTING_TOWER / TOWER_PROCESSING / REQUEST_QUEUED (static carved conduit otherwise);
- `OracleCoreModel.kt` state drivers verbatim; `MonumentalColor.kt` hex values verbatim; `VisualQuality.kt` budgets; `ConsoleScreen.kt` layout order (frieze → stage → chip → transcript → composer → caption → medallions → engine-truth row).

**Animation sampling (declared):** static frames freeze representative samples — ring phases at the state's `ringADegrees/ringBDegrees`, portal breath = 1.0 (t=0), LISTENING converge fractions 0 / ⅓ / ⅔, `TowerEnergyPhase = 0.35`, speaking amplitude = 0.62. No randomness anywhere.

**Comparison method disclosure:** in this sandbox the Read tool could not rasterize images for direct viewing (sub-agent limitation); the Owner reference (`upload/Immagine_Hermes_Tower.png`, 1536×1024, direction-only) and all rendered PNGs were therefore compared with **measured pixel metrics** (luminance bands, structural coverage, warm/cool chroma share, region contrast) computed with PIL/numpy, plus the exact deterministic Graph 3 preview sources. Every number below is reproducible from the renderer.

## 2. The 19 directive renders (+ 4 bonus frames)

| # | File (1080×2280 unless noted) | State / screen | What it shows |
|---|---|---|---|
| 1 | `console_idle.png` | IDLE | Resting Tower: static carved conduit (no pulses), portal breathing sample, TOWER mode seal, BRIDGE OK + MIC pills, IT labels |
| 2 | `console_listening.png` | LISTENING | 3 converging dashed waves, local-gold orbit messenger, MIC ATTIVO pill, listening hint in transcript |
| 3 | `console_tower_processing.png` | TOWER_PROCESSING | Fastest ring sample (63°/−41°), double departure trails + second sigil, lit ring-B sockets, flowing conduit pulses |
| 4 | `console_speaking.png` | SPEAKING | Arrival messenger (local-cyan), amplitude-glow portal (0.62), caption live-region line |
| 5 | `console_disconnected.png` | DISCONNECTED | Broken path (two severed segments + red-bronze break marks), 3 half-shutters, OFFLINE pill |
| 6 | `console_transcribing.png` | TRANSCRIBING | 5 of 12 ring-B sockets lit (glyph sequence sample) |
| 7 | `console_understanding_local.png` | UNDERSTANDING_LOCAL | LOCAL CORE plate seal, local-gold orbit, fastest local ring (28°) |
| 8 | `console_contacting_tower.png` | CONTACTING_TOWER | Departure trail + sigil, flowing conduit pulses, tower link state |
| 9 | `console_queued.png` | REQUEST_QUEUED | Amber queue seal (×2) at ring top, IN CODA ×2 pill, static conduit + 2 pulses |
| 10 | `console_privacy_muted.png` | PRIVACY_MUTED | 5 closed input gates + dashed red-bronze ring, SILENZIOSO pill, red mic medallion |
| 11 | `console_error.png` | ERROR | Red-bronze fracture paths + dashed misalignment ring at r=208 |
| 12 | `console_chat_focused_keyboard.png` | IDLE + IME | Stage shrunk to 0.52 (738px = 32.4% of screen), compact portal + state chip, transcript + enlarged composer with draft + material keyboard (300dp) |
| 13 | `onboarding_italian.png` | Onboarding step 1 | Wizard card: "Passo 1 di 6", language FilterChips (Italiano selected), Continue button, real `values-it` strings |
| 14 | `models_and_voice_italian.png` | MODEL SANCTUM | Screen title + intro, "A. Configurazione consigliata" rows, profile chips, voice-pack card with SIMULATO badge |
| 15 | `tower_gate_italian.png` | Tower Gate (QR) | "Scansiona QR di abbinamento", camera preview with QR framing, "Esito scansione" card, SIMULATED pairing button |
| 16 | `tower_archive_italian.png` | Tower Archive | "CAMERA DELL'ARCHIVIO", DATI SIMULATI badge, tabs Conversazioni/Missioni/Artefatti, search field, LOCAL/TOWER filters, conversation cards |
| 17 | `settings_italian.png` | Settings hub | Six categories GENERAL / VOCE / CONNESSIONE ALLA TORRE / MODELLI / DISPLAY / DIAGNOSTICA with real sub-labels |
| 18 | `console_italian_main.png` | Console (IT) | Full console: HERMES TOWER frieze + CONSOLE ORACOLO VOCALE + meander + seals + timeline + composer + engine-truth row |
| 19 | `german_compact_drawer.png` | Compact drawer (DE) | 60%-width modal drawer, HAUPTBEREICH, 5 primary destinations in German (Sprachkonsole · Tower-Archiv · Mit Hermes Tower verbinden · Modelle und Stimme · Einstellungen) |
| b1 | `console_idle_quality_low.png` | IDLE @ LOW | Static frozen frame: no glow overlay, 12 (not 24) ring teeth — OLED-safe rung |
| b2 | `console_tower_processing_quality_high.png` | TOWER_PROCESSING @ HIGH | **Byte-identical to the BALANCED frame** (see defect D2) |
| b3 | `console_idle_full_column.png` | IDLE, full scroll column (1080×2797) | Layout anatomy: everything from frieze to engine-truth row incl. the six control medallions |
| b4 | `comparison_sheet.png` (3513×1550) | — | Owner reference vs Graph 3 `p01_console_idle.png` vs Graph 4 `console_idle.png`, side by side |

## 3. Comparison evaluation — Owner reference vs Graph 3 vs Graph 4 (§8 axes)

Measured baselines: Owner ref mean-L **67.7**, warm-chroma share **33.5%**, structural coverage (L>40) **72.6%**. Graph 3 idle: mean-L **20.5**, warm **9.3%**, coverage **12.2%**. Graph 4 idle: mean-L **24.0**, warm **10.1%**, coverage **14.3%**, p95 **82** (vs 55 in G3).

| §8 axis | Verdict | Evidence (one line) |
|---|---|---|
| Architectural mass | **SATISFIED** | Built order spans 98.2% of the stage and 61.1% of the 2280px screen (stage alone 62.28%); temple depth reaches to stage y=150 → 55.7% of screen — inside the 55–65% directive window. |
| Layering | **SATISFIED** | All nine core layers (L0 sky → L9 messengers) plus the Graph 4 foreground frame composite in the shipped order, verified layer-by-layer in the mirror renderer. |
| Ring complexity | **SATISFIED** | 4 differentiated rings: bronze-gradient r440 with 72 engraved ticks (4 gold) + 4 cardinal bosses, rotor r352 with 24 diamond teeth + 8 windows, socket ring r270 with 12 lamps, gold hairline r208. |
| Portal quality | **SATISFIED** | Radial three-stop portal disc + breathing cyan glow + full Hermes sigil (staff, twin S-waves, wings, diamond eye); state shutters/half-shutters/waves all present. |
| Bronze detail | **PARTIAL** | Bronze materials are correct in hue but render dark-flat at BALANCED (warm-chroma 10.1% vs Owner 33.5%); ring gradient tops at bronzeLight L≈146 only at the ring crown. |
| Columns | **SATISFIED** | Six colonnade shafts (WebP) with flutes/bases + two Graph 4 flanking columns (stone gradient, 3 flutes, bronze capital+base) at w·0.085/0.915. |
| Crown | **SATISFIED** | Stepped crown silhouette with crenellation teeth and two turrets with three warm windows each, plus the Graph 4 pediment crown (apex at stage y=26) over it. |
| Lower gateway | **PARTIAL** | Arch + bronze rim + 5 frame stairs + 3-step plinth all present, but opening-vs-plinth contrast is only ≈1.9:1 (mean-L 9.7 vs 18.9) — the gateway reads as a rim outline, not a lit threshold (Owner lower-third mean-L ≈ 59.5). |
| Stairs | **SATISFIED** | Two stair systems render (7-step foreground via WebP/Canvas + 5-step frame stairs widening 0.55→1.35·archW), though they visually stack (see D7 note). |
| Energy paths | **PARTIAL** | Conduit + state-gated pulses are correctly implemented (pulses only in the 3 flow states), but the state chip occludes ≈71% of the conduit band (measured — D1), so the flowing path is mostly hidden exactly when it matters. |
| Lighting | **PARTIAL** | Atmosphere + portal + gateway glow exist, yet overall scene luminance (mean-L 24.0, p95 82) remains far below the Owner reference (67.7 / 143.8); the "controlled cyan atmosphere" is present but the stone never feels lit. |
| Empty space | **SATISFIED** | No flat void: non-black pixel share 58.4% at IDLE; background depth layers + sheen radial cover the stage. |
| Standard Material appearance | **SATISFIED** | Chrome is architectural (seals, medallions, carved panels, meander); residual Material3 is limited to functional text fields/tab row/buttons with bronze-edged styling. |
| Portrait readability | **PARTIAL** | Stage height 62.28% is inside the target window, but the scrollable column measures ≈117–124% of the 868.6dp viewport: at rest the composer starts ≈28dp above the fold and the medallion row + engine-truth row are below the fold (measured, D5). |
| OLED load | **SATISFIED** | Measured permanently-bright share: L>60 = 4.0–6.6% on resting states, ≤11.5% at peak states; L>180 ≤ 1.5%; mean-L ≤ 27.4 across all 11 states (table §5). |
| Chat usability | **PARTIAL** | IME shrink to 0.52 works (stage 32.4%, composer+timeline enlarge, keyboard 300dp drawn) but the outer bronze ring clips hard against the shrunken stage (D4) and the frieze must scroll away to fit. |

**Reference honesty note:** Graph 4 is measurably closer to the Owner reference than Graph 3 (mean-L +17%, p95 +49%, warm share +9%, plus real built order), but it remains an OLED-dark interpretation — the reference's illuminated warm stone is ~2.8× brighter. That gap is a *direction* question for the Owner, not an implementation defect, because the dark OLED identity is itself authorized (§9 mission).

## 4. Defects found (measured, honest)

**No §9 target layer is missing and no architecture-occupancy GAP exists** — the five §9 target groups are all implemented: BACKGROUND temple depth (WebP L0/L1) ✓ · UPPER crown + columns + inscription band (non-lexical engraved glyph ticks, by original-work design) ✓ · ORACLE CORE 4 differentiated rings + portal + sigil ✓ · LOWER gateway + stairs + plinth ✓ · MESSENGERS (state-driven) ✓. Architecture occupancy measured **61.13% of portrait height** (built order) / **55.70%** (incl. temple depth) — above the 55% floor. The defects below are real but narrow:

- **D1 — State chip occludes the messenger path (medium, visual).** The `StateOverlayChip` (bottom-center of the stage, ≈131px tall + 6dp margin) covers stage rows ≈1273–1404 of the 1221–1399 conduit → **≈71% of the flowing messenger band** is behind a 92%-opaque panel, including both pulse centers at the declared 0.35 phase sample; pulses are only clearly visible in the remaining 29% band. Fix: raise the chip above the conduit or start the conduit higher.
- **D2 — HIGH quality profile renders byte-identically to BALANCED (medium, contract).** `md5(console_tower_processing.png) == md5(console_tower_processing_quality_high.png)`. The Canvas consumes only `bitmapLayers / glowEnabled / glyphRing / frozen-frame`; HIGH's `particleCount=12`, `ringLayers=4`, `effectFpsCap=60` drive nothing, and `pulses = if (particleCount >= 40) 4 else 2` is dead code (max particleCount is 12). The quality ladder's top rung is currently unobservable.
- **D3 — `privacyRing` is modeled but never drawn (low).** `CoreVisualParams.privacyRing` (PRIVACY_MUTED) has no draw-site; the red identity comes from the shutters-branch dashed ring + chrome. Dead parameter — either draw it or drop it from the model.
- **D4 — Chat-focus clips the outer ring (medium, visual).** With the 0.52 stage (h=738), `cy = h·620/1420 = 322`; ring r=440 spans −118…762 → **≈27% of the ring top and the bottom edge clip hard** against the stage bounds (no fade/scaling). The "compact portal" reads, but the ring crown is guillotined.
- **D5 — Console column exceeds the resting viewport (medium, usability).** Mirrored layout at 412dp×868.6dp: frieze ≈177dp + stage 541dp + transcript ≈120dp + composer ≈96dp + medallions ≈62dp + engine row ≈20dp ≈ **117–124% of viewport**. At scroll-top the composer's top edge sits ≈28dp above the fold; the medallion row and engine-truth row are below the fold (fully visible in `console_idle_full_column.png` and after any scroll / when the IME opens). Also: the in-code comment "top area ~13%" drifts from the implemented ≈20% frieze.
- **D6 — Lower gateway contrast (low, visual).** Arch opening `#05070C` on obsidian plinth: measured region contrast ≈1.9:1 (mean-L 9.7 vs 18.9) — geometry is present but the threshold reads only via its bronze rim (L≈110), versus a clearly lit gateway in the Owner reference.
- **D7 — Dual stair systems stack (note).** The 7-step foreground stairs (L7) and the 5-step frame stairs occupy overlapping stage bands (arch bottom → stage bottom); faithful to the shipped code, worth a look during the next visual pass.
- **D8 — Duplicated refusal live-region (code hygiene, outside visual geometry).** `ConsoleScreen.kt` renders the `lastRefusal` Text twice (two identical blocks ~lines 312–333); a typed refusal would appear twice. Not a Canvas issue; flagging for the next code-sweep commit.

## 5. OLED-load assessment per state (measured on the rendered frames)

Permanently bright elements at 100% duty (drawn every frame in every state): frieze title (gold/bronzeLight), meander (gold·0.75), selected TOWER seal (gold on stoneDeep), pill dots + borders, stage chip label, composer *invia* (cyanCore), medallion borders/icons, transcript attribution labels, ring strokes (bronze, budget×dim), engraved ticks (0.85·dim), gold hairline, portal bronze rim + sigil floor (0.12 + sig floor), plinth/arch bronze rims, inscription ticks (gold·0.55), carved conduit (cyanDeep·0.55 / cyanAbyss·0.8 — dim teal, low duty). State-additional luminous elements: lit ring-B sockets (TRANSCRIBING 5, TOWER_PROCESSING via secondTrail), 4 lit ring-A windows (states with conduitAlpha>0.3), portal glow disc (α = p·breath), amber queue seal (QUEUED), LOCAL plate (UNDERSTANDING_LOCAL), fracture paths (ERROR), red dashed gate ring (PRIVACY_MUTED).

| State | L>60 | L>120 | L>180 | mean-L |
|---|---|---|---|---|
| IDLE | 6.19% | 3.38% | 1.14% | 24.0 |
| LISTENING | 10.68% | 3.58% | 0.94% | 25.8 |
| TOWER_PROCESSING | 11.53% | 4.00% | 1.54% | 27.2 |
| SPEAKING | 11.55% | 3.91% | 1.39% | 27.4 |
| DISCONNECTED | 6.27% | 3.28% | 1.06% | 23.8 |
| TRANSCRIBING | 8.24% | 3.74% | 1.23% | 25.7 |
| UNDERSTANDING_LOCAL | 10.86% | 3.95% | 1.37% | 26.4 |
| REQUEST_QUEUED | 7.94% | 3.71% | 1.08% | 25.2 |
| PRIVACY_MUTED | 6.56% | 3.49% | 1.08% | 23.9 |
| ERROR | 6.89% | 3.22% | 1.06% | 24.6 |
| IDLE (chat-focused) | 4.00% | 1.65% | 0.73% | 26.4 |

Resting states stay ≈94% sub-threshold (L≤60) — genuine OLED discipline; the pipeline-active states rise to ≈11% lit share driven by the bronze rings + lit sockets + glow, all dim-scaled by the state budget. LOW profile removes the glow and freezes motion (bonus frame b1). Burn-in safety additionally rests on the existing per-region 25s drift (`BurnIn.kt`), which the still frames deliberately do not depict.

## 6. Verdict

**PROCEED — the current implementation may proceed to full replacement of the legacy visuals**, with four narrow repairs to schedule (none blocks the replacement; all are scoped, no re-planning):

1. **D1** — un-occlude the messenger path (move the state chip or the conduit; restores the §7 signature element in exactly the states that advertise it).
2. **D2** — make HIGH observable (consume `particleCount`/`ringLayers` in the Canvas or remove the dead `>=40` branch) so the quality ladder is truthful.
3. **D4** — guard the chat-focus shrink (scale or fade the ring crown instead of hard clipping at the 0.52 stage bounds).
4. **D6** — a small gateway-presence bump (raise opening/plinth separation or add a controlled warm interior glow) to close the largest remaining visual distance to the Owner reference.

D3/D5/D7/D8 are hygiene items for the next code sweep. Every §9 target layer exists, state truthfulness holds in all 11 rendered states, OLED load is verified low, and the stage occupies 62.28% of portrait height — inside the authorized 55–65% window.

*Audited renders: 23 PNG + 1 comparison sheet in `download/graph4-monumental-preview-pack/`; renderer: `scripts/render_graph4_monumental_previews.py`; no app source modified; nothing committed.*

## 6. Post-repair re-render (2026-11-04, commit 3eac29a)

Appended section — history above is untouched. Commit `3eac29a` shipped the four narrow repairs D1/D2/D4/D6 (+ D8 hygiene, no render impact). The preview renderer was synced to the post-repair geometry and the whole pack re-rendered; every number below is measured with PIL/numpy on the re-rendered PNGs against the pre-repair pack (preserved at `/tmp/graph4-pack-prerepair/` for reproduction).

| | |
|---|---|
| **Renderer (synced)** | `scripts/render_graph4_monumental_previews.py` — mirrors `TowerArchitecture.kt` band `h*0.62→h*0.875` + pulse ladder + `clipPath` gateway glow, and `OracleCore.kt` `scale = min(w/1080, h/1420)` |
| **Verification script** | `scripts/verify_graph4_postrepair.py` (deterministic; prints every number below) |
| **Re-rendered** | full pack: 22 PNG + comparison sheet (all 11 console states re-rendered incl. the 5 minimum-required frames, quality variants, full column, sheet) |
| **App source touched** | **NONE** — renderer/verification live outside the repo; `git status` shows no `app/` modification; nothing committed by this task |

### Repairs verified (before → after, measured)

| Defect | Pre-repair (measured) | Post-repair (measured) | Verdict |
|---|---|---|---|
| **D1** chip occludes messenger path | band `h*0.86→h*0.985` = abs rows 1684–1862 (177.5px); chip rows 1736–1867 (pixel-detected top 1736) → **125.5px overlap = 70.7% occluded, pulse-band visibility 29.3%** | band `h*0.62→h*0.875` = abs rows 1343–1706 (362.1px); **overlap with chip 0.0px, gap 31.2px → visibility 100.0%**; pixel row-overlap band∩chip = 0 rows | **CLOSED** |
| **D2** HIGH byte-identical to BALANCED | `md5(console_tower_processing) == md5(…_quality_high)` = `442bda37…` (defect reproduced); full pulse cores visible in band: **0 / 0** (both centers at rows 1746/1835 behind the chip; only a 14px halo-sliver of the carve line at rows 1721–1734) | md5 **differ**: `492f8c36…` (BAL) vs `329975d6…` (HIGH); pulse cores in band (phase 0.35, halo-brightened carve line merged, erosion-isolated): **BALANCED = 2, HIGH = 4** | **CLOSED** |
| **D4** chat-focus clips outer ring | scale `w/1080 = 1.0`, r3=440 → ring span y **−117.8…762.2** vs stage 0…738 → clipped both ends; pixel ring crown top row **0** (touching the bound) | scale `min(1.0, 738/1420) = 0.5197`, r3=228.7 → ring span y **93.5…550.9** → fully inside stage 0…738; pixel crown top row **77** (> 0) | **CLOSED** |
| **D6** gateway opening reads as rim outline | opening mean-L **9.62** (reproduces the filed 9.7); warm-chroma (r>b) share in opening **2.0%**; plinth:opening 1.20:1 | opening mean-L **14.42** (+4.80, +50%); warm share **9.2%**; plinth:opening **0.80:1** (opening now the brighter, warm element); plinth face 11.59 and first stair band 40.27 unchanged, and the entire lower structure outside the opening polygon is **bit-stable pre→post (Δ +0.0000)** → the `clipPath` glow never bleeds | **CLOSED** |

Region notes (reproducibility): opening = the exact gateway-opening bézier polygon (stage rows ≈912–1283); plinth face = top plinth step rows 994–1136 excluding the opening bbox; the §4/D6 "plinth 18.9" figure used a broader lower-structure sampling region — the decisive pre→post facts are the opening delta (+50% mean-L, +7.2pp warm share) and the zero-delta outside the clip.

### OLED budget — re-rendered states (pre → post)

| State | mean-L pre → post (Δ) | L>60 % pre → post | L>180 % pre → post |
|---|---|---|---|
| IDLE | 24.00 → 24.16 (+0.16) | 6.19 → 6.27 | 1.14 → 1.14 |
| LISTENING | 25.77 → 25.92 (+0.16) | 10.68 → 10.77 | 0.94 → 0.94 |
| TOWER_PROCESSING | 27.19 → 27.39 (+0.20) | 11.53 → 11.65 | 1.54 → 1.54 |
| SPEAKING | 27.38 → 27.54 (+0.16) | 11.55 → 11.63 | 1.39 → 1.39 |
| DISCONNECTED | 23.78 → 23.95 (+0.16) | 6.27 → 6.35 | 1.06 → 1.06 |
| TRANSCRIBING | 25.66 → 25.82 (+0.16) | 8.24 → 8.32 | 1.23 → 1.23 |
| UNDERSTANDING_LOCAL | 26.45 → 26.61 (+0.16) | 10.86 → 10.94 | 1.37 → 1.37 |
| CONTACTING_TOWER | 26.22 → 26.42 (+0.21) | 10.81 → 10.94 | 1.29 → 1.29 |
| REQUEST_QUEUED | 25.18 → 25.39 (+0.21) | 7.94 → 8.07 | 1.08 → 1.08 |
| PRIVACY_MUTED | 23.93 → 24.09 (+0.16) | 6.56 → 6.65 | 1.08 → 1.08 |
| ERROR | 24.57 → 24.73 (+0.16) | 6.89 → 6.98 | 1.06 → 1.06 |
| IDLE (chat-focused) | 26.43 → 25.48 (−0.95) | 4.00 → 3.78 | 0.73 → 0.73 |
| TOWER_PROCESSING @HIGH | 27.19 → 27.45 (+0.26) | 11.53 → 11.69 | 1.54 → 1.54 |
| IDLE @LOW | 25.12 → 25.28 (+0.16) | 6.30 → 6.39 | 1.14 → 1.14 |

The longer carved band (D1) and the warm glow (D6) shift mean-L by **≤ +0.26 absolute (≈0.9% relative — inside the <1% impact envelope D6 declared)**; L>180 is unchanged (max 1.54%, the pre-existing worst). Honest flag: SPEAKING (27.54) and TOWER_PROCESSING @HIGH (27.45) now sit +0.14 / +0.05 above the previous §5 worst case of 27.4 — the OLED axis holds in substance (resting states still ≈94% sub-60) and the envelope movement is bounded and declared here. The chat-focused frame gets *darker* (−0.95) because the D4 min-scale ring spreads the same bronze over fewer pixels.

Residual watch-items (not reopenings): at the 0.52 chat-focus stage the screen-space chip top sits at `h*0.801`, so the *static* carved band tail (h*0.801–0.875) passes behind the chip there — no flowing state is rendered in chat focus and all three flow states render at the full-screen stage where the band is 100% clear; if IME + TOWER_PROCESSING can co-occur on-device, the same geometry applies and is worth one emulator look during the next visual pass. D3/D5/D7/D8 remain hygiene items for the next code sweep (D8 shipped in `3eac29a`, no render impact).

**Updated verdict: the four narrow repairs D1/D2/D4/D6 are measurably CLOSED on the re-rendered pack (visibility 29.3%→100%, HIGH md5 diverged with 2→4 pulse cores, chat ring span −118…762 → 94…551 inside the 738px stage, opening mean-L 9.6→14.4 with zero bleed outside the clip); OLED discipline retained (worst mean-L 27.54, +0.14 over the previous worst, L>180 unchanged). PROCEED status unchanged — full legacy-visual replacement may proceed.**

*Post-repair renders: 23 files in `download/graph4-monumental-preview-pack/` (re-rendered 2026-11-04); renderer synced at `scripts/render_graph4_monumental_previews.py`; measurements: `scripts/verify_graph4_postrepair.py`; no app source modified; nothing committed.*
