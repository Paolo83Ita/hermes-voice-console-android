# OWNER TOWER VISUAL PACKAGE — GRAPH 4.13 (§41)

Mission: OWNER VISUAL QUALITY, APP-WIDE ACCESSIBILITY, MODEL LIBRARY UX AND
MONUMENTAL TOWER REFOUNDATION. This package presents the refounded Central
Hermes Tower for the Owner's visual judgment. **No visual PASS is claimed or
self-granted here — every physical verdict remains Owner-only.**

Canonical state after this graph: `OWNER_VISUAL_RETEST_PENDING`
(implementation boundary: `GRAPH413_OWNER_VISUAL_ACCESSIBILITY_PACKAGE_READY`).

## 1. What was rejected and why (§29)

`CENTRAL_TOWER_GRAPH412_VISUAL_OWNER_REJECTED` — the Owner verdict: clean but
flat/schematic, thin disconnected columns, detached sign, small oracle core,
large empty upper area, inferior to the reference. The 4.12 set is RETIRED
from `res/` (evidence preserved under `docs/visual/tower412/`).

## 2. Owner reference use (§33)

`upload/Immagine_Hermes_Tower.png` was used as the authority for
monumentality, vertical hierarchy, sign, circular oracle core, bronze
richness, central gate and broad stairs. NOT reproduced: lateral model
towers, provider labels, arrows, messengers, audit panels, poster captions,
sky background. No reference pixels were extracted — the shipped artwork is
an ORIGINAL generated interpretation (§35).

## 3. The three candidates (§34)

| Candidate | Identity | Distinct traits |
| --- | --- | --- |
| **A — MONUMENTAL ORACLE CITADEL** (SELECTED) | closest to the Owner reference | heavy entablature with dentils + engraved frieze, SOLID massed central body, connected heavy pylons with capitals/bases and carved panels, integrated aedicula crown sign (bracket-connected, never floating), 42%-width oracle mechanism with deep nested bronze rings, engraved teeth and cyan sigil portal, arched lower gateway with interior glow, broad flaring stairs, three-tier bottom-integrated foundation, vertical cyan energy spine |
| B — MECHANICAL TOWER CORE | more mechanical | gear teeth, rivets, spoke struts, octagonal body, gear finial |
| C — MOBILE MONUMENTAL | strongest small-screen readability | boldest shapes, dominant core, minimal fine detail |

Independent visual comparison (§34): geometry metrics alone did NOT select —
the three composites were compared on Owner-reference fidelity first, then
mobile readability, visual coherence, memory, animation capability and
accessibility. **A wins fidelity first** (the Owner's standing criterion);
C remains the strongest at extreme small widths and is preserved as a
candidate for a future Owner decision. Renders:
`docs/visual/tower413/renders/comparison_sheet.png`.

## 4. Defect gates (§37) — ALL PASS (A, B, C)

`docs/visual/tower413/visual_validation_report.json` +
`scripts/verify_g413_tower.py`: detached sign ✗, floating beams ✗, missing
stairs ✗, incomplete ring ✗, off-center portal ✗, unsupported columns ✗,
accidental holes ✗, clipped architecture ✗, unreadable details ✗, excessive
empty upper area ✗, corruption-like black gaps ✗, resemblance to the rejected
Graph 4.10 extraction ✗, resemblance to the rejected 4.12 scaffold ✗.
OLED: zero pure-white pixels; cyan bounded (no generic neon flood);
composition mirror-symmetric.

## 5. Layer contract (§36)

13 layers, ONE normalized stage (1000×1375), mechanism layers cropped to the
oracle box (476/1000 stage units centered at 500,640):
`tower_architecture_rear, tower_plinth, tower_stairs, tower_lower_gate,
tower_heavy_pylons, tower_entablature, tower_crown_sign, tower_oracle_outer,
tower_oracle_middle, tower_oracle_inner, tower_portal, tower_energy,
tower_state_overlay`. Per-asset z-index, bounds, pivot, alpha policy,
animation policy, decoded memory, SHA-256, provenance and license
declaration: `docs/visual/tower413/INTEGRATION_MANIFEST.json`.

## 6. Performance profiles (§40)

| Profile | Asset tier | Decoded memory | Animation |
| --- | --- | --- | --- |
| LOW | dedicated `towerl_*` set (460 px stage) | ≈ 9.8 MB (≤ 12 MB) | static architecture; ONE slow ring retained (reduced, not zero); portal breathing + state alphas; no particles |
| BALANCED (default) | 680 px stage | ≈ 21.4 MB (≤ 24 MB) | full §39 state animation |
| HIGH | 1000 px stage (not shipped in APK default set) | optional | + particles (never default) |

The architecture and composition are IDENTICAL across tiers — only resolution
differs (§40: the visual architecture is never degraded to meet a budget).

## 7. Animation preservation (§39)

The §31/§39 state truth is preserved verbatim from the 4.12 contract: IDLE
slow outer motion; LISTENING input energy; TRANSCRIBING sequential marks;
PROCESSING_LOCAL inner mechanism active without a faked Tower link;
CONTACTING_TOWER outer conduit; SPEAKING portal responds to audio amplitude;
CONTINUOUS persistent low-energy listening; PRIVACY_MUTED closed input gate
(seal now anchored at the NEW gateway coordinates); CHAT_SPEECH_MUTED dimmed
output; DISCONNECTED conduit visibly off; ERROR controlled amber fracture,
never flashing. Programmatic geometry (§35) owns animation, particles,
energy and state overlays; the layered assets own the architecture.

## 8. Mobile composition (§38)

360–411 dp: sign legible (integrated attic, gold on bronze), oracle core
visually dominant (42% stage width), pylons/gate/stairs visible, no
horizontal clipping (screenshot gates). Keyboard open: the existing §33
coreScale focus contract collapses the Tower to a focused form (sign + core +
portal dominant; the full stairs recede with the scale) — unchanged
mechanism, verified at 0.24 height fraction in `renders/keyboard_open.png`.

## 9. Included renders (§41 checklist)

- Owner reference: `upload/Immagine_Hermes_Tower.png` (Owner-held).
- Rejected Graph 4.10 Tower: evidence preserved in the Graph 4.10 reports.
- Rejected Graph 4.12 Tower: `docs/visual/tower412/candidate_*/preview_composite.png`.
- Candidates A, B, C: `docs/visual/tower413/candidate_{a,b,c}/preview_composite.png`
  + `renders/comparison_sheet.png`.
- Selected candidate rationale: §3 above.
- POCO render: `renders/poco_idle.png` (393 dp @2.75x, 50% console height).
- Xiaomi render: `renders/xiaomi_idle.png` (360 dp).
- Keyboard-open render: `renders/keyboard_open.png`.
- LOW render: `renders/tier_low.png`; BALANCED render: `renders/tier_balanced.png`.
- Primary application states: `renders/state_{listening,transcribing,
  processing_local,speaking,privacy_muted,disconnected,error}.png` (+ IDLE =
  `stage_idle.png`).

**Owner approval remains required. State: OWNER_VISUAL_RETEST_PENDING.**
