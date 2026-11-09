# GRAPH 4.14 — TOWER FINAL ASSET AUDIT (CP-G4.14-C06, layer leg)

§9: independent re-verification of every SHIPPED production Tower asset
(app/src/main/res/drawable-nodpi/tower414*.webp) — NOT the generation script's
PASS. Method: `scripts/audit_g414_tower_final.py` (recomputed on the shipped
pixels this session) + visual inspection of the contact sheet.

## NODE
CP-G4.14-C06 (layer leg).

## FINDING

### Identity + memory (both tiers, all 7 layers)
- 14 slices (7 layers × LOW/BALANCED) on ONE normalized stage (§37):
  BALANCED stage 680×964 px, LOW stage 460×652 px.
- Every slice's SHA-256 re-measured and re-pinned against
  `ASSET_MANIFEST.json` — 14/14 pins MATCH (`sha_manifest_pin: true`).
- Decoded totals: BALANCED **6.74 MB** (budget ≤24), LOW **3.09 MB**
  (budget ≤12) — both INSIDE budget; structure never sacrificed.
- Per-layer alpha verified (RGBA WebP): architecture 77.7% opaque with clean
  feather; sign/rings/portal/energy are properly masked crops.
- Source reference: upload/Immagine_Hermes_Tower.png (1536×1024,
  SHA-256 bc49a6d9…) via the Owner-authorized center-column extraction
  (§34); transformation method documented in the manifest
  (magenta-key subject extraction → calibrated arrow-hue removal → hole
  fill → island removal → despill → feather; bbox trim to 637×903 master;
  §37 slicing).
- Renderer placement (pivot/z-order): raster z-order architecture → rings
  (outer/middle/inner rotation shimmer) → portal → crown sign → energy mask;
  placement fractions pinned in ReferenceTowerVisual.kt LayerAt (single
  normalized coordinate system); stage bounds = layer boxes; keyboard-open
  collapse keeps sign + mechanism + portal visible (§41).
- Stage-bounds memory: composite rendered at both tiers; no out-of-stage
  layer boxes.

### Defect re-scan on the SHIPPED pixels (calibrated gates)
| Gate | BALANCED | LOW | Threshold / definition |
| --- | --- | --- | --- |
| no_magenta_remnant | **0 px** | **0 px** | chroma-key signature: hue 295–345°, sat≥0.62, val≥0.55 (cross-checked: the Owner reference contains 0 such pixels in 1.57 M) |
| no_residual_arrow_hues | 0 | 0 | green 70–120° / purple 255–295° / super-saturated orange 25–65° at sat>0.85–0.90 |
| single_connected_body | **1 body, 100.0% coverage** | **1 body, 100.0%** | largest component covers all opaque px — sign/rings/stairs/gate/lower architecture all connected; no lateral fragments, no disconnected debris |
| enclosed holes | **0** | **0** | no unplanned transparent holes inside the body (portal darkness is opaque artwork) |
| sign complete / not clipped | crown sign 0.150/0.030/0.700/0.195 — full "HERMES TOWER / MULTI-LLM ORACLE NETWORK / DEEP MAGIC CORE" | same | contact sheet row |
| rings complete | outer/middle/inner complete circles | same | contact sheet rows |
| pure-white pixels (OLED) | none introduced | — | composite inspected on black |

### Repair executed this continuation (typed)
The independent re-audit found what the generation gate had missed: **110
opaque chroma-key-signature pixels** (50 clusters, 1–9 px) on the master plus
their resampled/feather spread in the slices (crown-sign letters' edges and
the right despill boundary; e.g. rgb(164,55,113)). The Owner reference has
ZERO pixels of that signature → extraction residue, NOT artwork.
Repair (scripts/repair_g414_magenta_residue.py +
repair_g414_feather_despill.py): in-slice median inpaint of every signature
pixel (alpha>0) — NO re-extraction, NO re-slicing, NO geometry change; only
the contaminated pixels changed. Result: **0 residue on all 14 shipped
slices**; manifest re-pinned; the 147/19 remaining dark-wine pixels
(val≤0.60, desaturated) are documented artwork shadow tones (below the
chroma-key signature by construction).
The 637×903 master PNG (provenance artifact, not shipped) was repaired the
same way; residual sub-visible feather specks remain there and are typed as
superseded-by-slices.

### Contact sheet (visual proof, human-inspected)
`docs/visual/tower414/GRAPH414_TOWER_LAYER_CONTACT_SHEET.png` — every layer ×
{black, white, magenta, checkerboard} × both tiers + the stage composite row.
Inspected this session: clean alpha on magenta/checkerboard, sign intact,
rings complete, no arrows, no magenta fringe, no poster panels, no model
labels, no lateral fragments. Layer images re-composited by the audit script
independently of the renderer.

## ACTION
Independent re-audit → calibrated repair → re-pin → contact sheet → visual
inspection. The APKs were REBUILT after the repair (the shipped assets are
the repaired set).

## TEST
- Audit script gates: allPass=true (JSON: TOWER_FINAL_ASSET_AUDIT.json).
- 496/496 unit suite; rebuilt APKs hash-pinned in the build receipt.

## FILES
- app/src/main/res/drawable-nodpi/tower414*.webp (4 slices re-encoded after
  in-slice repair: balanced architecture/crown_sign, low architecture/crown_sign
  + feather passes across the slice set)
- docs/visual/tower414/{ASSET_MANIFEST.json (re-pinned + repair provenance),
  TOWER_FINAL_ASSET_AUDIT.json, GRAPH414_TOWER_LAYER_CONTACT_SHEET.png,
  GRAPH414_composite_balanced.png, GRAPH414_composite_low.png,
  tower414_master.png (provenance, repaired)}
- scripts/audit_g414_tower_final.py, scripts/repair_g414_magenta_residue.py,
  scripts/repair_g414_feather_despill.py

## EVIDENCE
- TOWER_FINAL_ASSET_AUDIT.json (per-layer identity + gates + scan counts).
- Contact sheet (visually inspected this session, both tiers).
- Manifest SHA pins 14/14.

## CHECKPOINT
CP-G4.14-C06 (layer leg) — DONE.

## NEXT
Animation leg (§10) then the Owner reference comparison (§11).
