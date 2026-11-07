# CENTRAL TOWER REPLACEMENT REPORT (GRAPH 4.12 §26–§30)

## 1. Retirement of the rejected set (§26)

The ten shipped layers (tower_rear/sign/outer_ring/middle_ring/inner_ring/
portal/lower_gate/stairs/foreground/energy — the APK-inspected, Owner-rejected
set) are REMOVED from the default product path and quarantined as historical
evidence under `docs/evidence/quarantined-tower-assets-graph412/` (bytes
preserved, never shipped). No automatic cutout, mirror-fill, clone-fill or
scrub pass was applied to them again. Declared:
`CENTRAL_TOWER_ASSET_RECONSTRUCTION_FAIL` (the old pipeline's terminal state).

## 2. New original candidates (§27/§28)

Three ORIGINAL vector-drawn compositions (no poster pixels, no cutouts) on a
normalized 1000×1375 stage, one truth rendered at LOW/BALANCED/HIGH:

| Candidate | Automated defect check | Decoded (BALANCED, all 12) | File bytes |
| --- | --- | --- | --- |
| Candidate A | PASS — zero defects | 20.88 MB | 95,034 B |
| Candidate B | PASS — zero defects | 20.88 MB | 95,304 B |
| Candidate C | PASS — zero defects | 20.88 MB | 94,016 B |

Selection: **Candidate A** — the Owner's standing decision favors fidelity to
the central reference Tower; candidate A (faithful monumental bronze
architecture) passed all automated and visual defect checks and offers the
best recognizability + mobile readability with clean animation separation.

Visual checks performed on the composite previews: complete sign, complete
rings, continuous architecture, clean stairs, no black holes, no residual
arrows, no fragments, no duplicated bands, no text corruption.

## 3. Layer contract (§29) — candidate A, BALANCED profile

| Layer | Size | Bytes | SHA-256 (16) | Nonblank | Components | Symmetry |
| --- | --- | --- | --- | --- | --- | --- |
| tower_structure_rear | 576×792 | 12,776 B | `4f222cbf7c7008a3…` | nonblank 0.3387 | comps 1 | sym 0.998 |
| tower_sign | 576×792 | 5,514 B | `26c11d5d8b5ebce6…` | nonblank 0.0503 | comps 1 | sym - |
| tower_columns | 576×792 | 7,170 B | `5f49886398ca7179…` | nonblank 0.1474 | comps 2 | sym 0.993 |
| tower_outer_mechanism | 576×792 | 17,614 B | `85df7fbab5fc2ed2…` | nonblank 0.0423 | comps 1 | sym 0.998 |
| tower_middle_mechanism | 576×792 | 14,482 B | `b62e48d9d5f6ac00…` | nonblank 0.0305 | comps 1 | sym 0.999 |
| tower_inner_mechanism | 576×792 | 11,954 B | `c0a66473b4131fc3…` | nonblank 0.021 | comps 1 | sym - |
| tower_portal | 576×792 | 10,292 B | `e1036312687d9498…` | nonblank 0.0465 | comps 1 | sym - |
| tower_lower_gateway | 576×792 | 5,856 B | `5465a7589dc6e2b5…` | nonblank 0.0444 | comps 1 | sym - |
| tower_stairs_plinth | 576×792 | 3,752 B | `6cc9e86342c6b4d8…` | nonblank 0.0601 | comps 1 | sym 1.0 |
| tower_foreground | 576×792 | 2,768 B | `2cbe0d7563dd4adc…` | nonblank 0.0096 | comps 1 | sym - |
| tower_energy_paths | 576×792 | 1,916 B | `a3d5eca1c555aacd…` | nonblank 0.0036 | comps 3 | sym - |
| tower_state_overlay | 576×792 | 940 B | `01cc2e945fe66f6b…` | nonblank 0.0 | comps - | sym - |

All layers share the EXACT stage size (576×792 at BALANCED; 448×616 LOW;
896×1232 HIGH) — no mixed tight crops. `tower_state_overlay` ships fully
transparent (runtime tint surface).

## 4. §32 performance budgets

- LOW: 12 × 448×616×4 B = **11.1 MB** decoded (< 12 MB budget) — no particles, minimal animation.
- BALANCED (default): 12 × 576×792×4 B = **20.9 MB** decoded (< 24 MB budget) — state-driven animation.
- HIGH (optional, not default): 12 × 896×1232×4 B ≈ 52.9 MB — requires thermal qualification; never loaded together with other variants.
- §24 staged decode: the first Console frame decodes only the 6 static layers; the 5 animated mechanism layers decode asynchronously right after (state_overlay never decodes).

## 5. Animation contract (§31)

IDLE slow outer motion + subtle portal; LISTENING incoming energy + open gate;
TRANSCRIBING sequential ring pulses; PROCESSING differentiated mechanisms
(22/-14/34 s); SPEAKING audio-amplitude portal; CONTINUOUS stable low-energy;
PRIVACY MUTED closed gate + red-bronze seal; CHAT SPEECH MUTED output dimmed
(mic unaffected); DISCONNECTED conduit off, local portal alive; ERROR
controlled amber, no flashing. Reduced motion freezes everything honestly.

## 6. Verdict

`OWNER_VISUAL_RETEST_PENDING` — only the Owner accepts the Tower (§34).
