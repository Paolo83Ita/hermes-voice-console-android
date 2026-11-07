# CENTRAL TOWER VISUAL REFOUNDATION REPORT (§36, CP-G4.10-010/011)

## Verdict

**OWNER_VISUAL_RETEST_PENDING** — only the Owner accepts the final appearance.
The procedural Tower (OracleCore) is RETIRED from the Console per §29; the
Console now renders the Owner-reference-derived LAYERED central Tower.

## Pipeline executed (§30)

| Step | Evidence |
| --- | --- |
| 1. isolate the central Tower | central band crop (source x 566–1012 of `Immagine_Hermes_Tower.png` 1536×1024); measured core center (789, 413) |
| 2. remove the background | sky/band color separation (B > R+12 dark blue/purple rule) |
| 3. remove lateral elements | corridor profile (crown ±92/112, ring band ±195, gate ±140/150) + white-hot/gold/green arrow-mask (messenger arrows, fire-bowl flares) + model towers/panels/sky excluded by the band |
| 4. reconstruct occluded architecture | symmetry hole-fill (mirror-fill restricted to the messenger-occlusion rows y 280–560) |
| 5. restore symmetry | mirror fill as above; text bands EXCLUDED from filling (no duplicated carving) |
| 6. preserve bronze and stone detail | tower-tone retention (R ≥ B−6), brightness floor 120 |
| 7. preserve the circular oracle core | measured annuli 42–88 / 84–126 / 122–182 around (789, 413) |
| 8. reconstruct lower portal and stairs | gate band (700–880, 660–872) + stairs (640–940, 790–886); the lower AUDIT WORKFLOW panel is REMOVED (y ≥ ~888 excised) |
| 9. transparent layered assets | 10 transparent WebP layers (uniform scale 720/446 source px) |
| 10. validate edges at mobile scale | previews rendered at the display stage 720×1653; edge fragments (dim arrow glow) are sub-2% of stage width and invisible at Console scale; refinements documented in the manifest |

## Required layers (§31) — all produced

`tower_rear` (720×1653) · `tower_sign` (629×203) · `tower_outer_ring` /
`tower_middle_ring` / `tower_inner_ring` (645×645 each) · `tower_portal`
(645×645) · `tower_lower_gate` (291×342) · `tower_stairs` (484×154) ·
`tower_foreground` (629×32) · `tower_energy` (420×722).
Full geometry, SHA-256, sizes and per-layer provenance:
`docs/visual/ASSET_MANIFEST.json` (manifestVersion
graph-4.10-central-tower-refoundation-1). Canvas owns rotation, pulses, seals,
fracture, particles and messenger packets (§31).

## Removed elements (§29) — verified in previews

left/right model towers, all model and provider names, side brains, winged
messenger figures (arrows erased incl. white-hot cores), colored arrows, side
audit panels, background sky, side fire bowls, the full poster composition and
the lower audit workflow panel.

## Console integration (§33/§34/§35)

- `ReforgedTowerVisual` replaces `OracleCore` in the Console core slot; the
  chat-focus scale (keyboard open → 0.52) preserves the §33 height bands
  (~50–58 % closed / ~22–30 % open); sign, oracle core and cyan portal always
  preserved; the timeline and composer stay readable (no Tower pixel overlaps
  controls).
- §34 states implemented: IDLE (slow outer ring, low breathing), LISTENING
  (energy + open gate), TRANSCRIBING (sequential ring pulses), LOCAL (cyan-gold
  inner), CONTACTING/TOWER_PROCESSING (upward pulse + differentiated rotation),
  SPEAKING (amplitude-driven portal light), CONTINUOUS (persistent moderate
  seal), PRIVACY MUTED (closed gate + red-bronze seal), CHAT SPEECH MUTED
  (output glyph only), DISCONNECTED (conduit off, local core available), ERROR
  (controlled amber fracture — no flashing).
- §35 profiles: LOW = static rear + static portal; BALANCED (default) = smooth
  rings + state energy; HIGH = Canvas particles, never default, physical
  thermal qualification required. Reduced motion freezes everything honestly.

## Preview gate (§36)

Rendered states in `docs/visual/previews/`: IDLE, LISTENING, TRANSCRIBING,
LOCAL, TOWER_PROCESSING, SPEAKING, CONTINUOUS LISTENING, PRIVACY MUTED, CHAT
SPEECH MUTED, DISCONNECTED, ERROR + `composite_idle.png` +
`comparison_owner_vs_reforged.png` (Owner reference vs the new layered Tower).
The POCO/Xiaomi on-device screenshots will be appended to
`OWNER_POCO_XIAOMI_RETEST_PACKAGE_3` by the Owner's physical pass.

**Only the Owner accepts the final appearance — OWNER_VISUAL_RETEST_PENDING.**

---

# GRAPH 4.11 UPDATE (Node 5, CP-G4.11-002) — measured defect repair, 2026-11-07

The Owner re-issued the composition authority ("Stop further procedural tower
refinements. Use the Owner image as the composition authority..."). The
Graph 4.11 audit MEASURED four defects in the 4.10 assets and repaired them
(`scripts/tower_refoundation_g411.py`, persisted and reproducible):

| ID | Measured defect | Repair |
| --- | --- | --- |
| G4.10-D1 | `tower_sign` clipped the H of "HERMES" and the M of "MULTI" (sourceBox [596,6,986,132] started inside the plaque — measured on a gridded crop) | plaque re-measured **[578,0,1018,166]**, blue-grey sky dropped, re-staged at stage rect (19,0,710,268) |
| G4.10-D2a | `tower_rear` carried the sign's bottom text sliver ("M ORACLE NET") | sign zone cleared (stage y < 271; the DEEP AUDIT CORE banner preserved) |
| G4.10-D2b | `tower_rear` + rings carried lateral arrow fragments (orange/green hue rule) AND a bronze-toned arrowhead crossing the annulus right side | hue scrub + edge scrub + **mirror-cloned clean annulus arc** over the arrowhead (ring-local 535..645,188..264) |
| G4.10-D4 | the four static bands (sign/stairs/gate/foreground) were TIGHT canvases while `ReforgedTowerVisual` draws them TopCenter/fillMaxWidth/Fit — in the real app they rendered at the wrong size and position | **every static band re-staged on the full 720×1653 stage canvas** at its true position — no Compose change, the assets carry the placement |

Core-alignment proof: stage x = (orig_x − 566)·S, y = orig_y·S with S = 1653/1024
puts the manifest core (789,413) at (0.49997, 0.40332) of the stage — the code's
exact ring center. `tower_portal` and `tower_energy` are byte-identical to 4.10.

Manifest advanced to `graph-4.11-central-tower-refoundation-2` (all sha256
recomputed, 10/10 matching). Previews re-rendered with the REAL Compose layout:
`composite_idle.png`, `comparison_owner_vs_reforged.png`, 12 tower-scoped state
previews. Layer naming map: `tower_lower_gate` == the Node-5 `tower_gate`.

**Verdict unchanged: OWNER_VISUAL_RETEST_PENDING — only the Owner accepts the
final appearance (Package 4 runbook, "Verifica visiva della Torre").**
