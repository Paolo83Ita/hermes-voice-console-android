# ASSET PIPELINE DECISION — Graph 3

Decision: **CONTROLLED HYBRID PIPELINE** (mission §13), owner-authorized direction.

## 1. Chosen architecture

| Element | Technology | Rationale |
|---------|-----------|-----------|
| Layout & interaction | Jetpack Compose (existing) | preserve state connections, semantics, 48 dp targets |
| Static architectural depth (L0 sky, L1 temple+crown, L2 colonnade, L7 stairs/frame) | layered WebP drawables (generated, original) | depth without per-frame redraw cost; replaceable files, not baked into code |
| Reusable sigils/seal glyphs | vector drawables (XML) | scalable, themeable, tiny |
| Dynamic rings, conduits, portal glow, messenger paths | Compose Canvas (one clock, like V0.1 TowerCore) | state-driven, cheap arcs/lines, testable |
| Rive / Lottie | NOT USED in V0.3 | no license-compatible runtime need proven yet; Canvas covers the motion |
| AGSL shaders | DEFERRED | only after physical-device qualification (mission §13/§14) |
| Single screenshot as interface | FORBIDDEN | maintainability + accessibility |

## 2. Generation pipeline (reproducible)

1. Geometry lives in ONE parametric generator (`scripts/monumental_lib.py` + renderer)
   which produced the approved preview pack — the previews are the visual contract.
2. The same geometry is emitted as: (a) SVG previews (design/monumental/preview/), and
   (b) production layered WebP/vector assets under `app/src/main/res/`.
3. Every emitted asset is registered in `ASSET_MANIFEST.json` (id, source, method,
   license, dimensions, size, transparency, layer, memory estimate, state usage,
   fallback, sha256).
4. No asset from the Owner reference image is shipped; the reference is direction-only.

## 3. Why not Compose-only Canvas (rejection of the simple path)

The mission explicitly rejects "one simple Compose Canvas drawing": the monumental look
needs irregular textures (obsidian sheen, stone grain, bronze engraving) that are better
authored as bitmap layers; Canvas remains for anything state-driven. This split keeps
idle frames cheap (bitmap blits) and animated layers bounded.

## 4. Replaceability contract

- Every layer drawable can be swapped without touching composables (resource names fixed
  per layer: `monumental_l0_sky`, `monumental_l1_temple`, ...).
- The Oracle Core composable takes layer IDs from a token object — a future theme pack
  can restyle the tower without code changes.
- Fallback: if a bitmap layer is missing at runtime, the Canvas draws a flat gradient
  placeholder for that layer (never crashes, never blank screen).
