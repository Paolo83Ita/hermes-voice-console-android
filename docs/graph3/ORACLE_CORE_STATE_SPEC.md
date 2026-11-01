# ORACLE CORE STATE SPEC — L0..L9 × 15 CONSOLE STATES

The Oracle Core replaces `TowerCore` as the voice identity. It renders layers L0–L9 and
derives ALL motion from the real `ConsoleState` + real inputs (speaking amplitude, queue
count, connection status). No decoration independent of state. Authoritative source for
tokens: `ui/monumental/OracleCore.kt` (Wave 2). Preview frames: `design/monumental/preview/`.

## 1. Layer contract

| Layer | Content | State dependence |
|-------|---------|------------------|
| L0 | deep atmospheric background (radial obsidian→sheen), soft abyss glow | luminance budget `dim` |
| L1 | rear temple: stepped crown, crenellation, two turrets with warm windows, pediment inscription ΗΕΡΜΗΣ ΟΡΚΕΛΟΣ | windows dim with state |
| L2 | colonnade: 6 fluted columns, capitals/bases, inner energy rim | rim alpha = conduit budget |
| L3 | outer bronze ring: engraved ticks ×72, gold every 18th, 4 cardinal bosses | bronze luminance budget `bd` |
| L4 | mechanical rings A (teeth + slot windows), B (12 sockets), C (thin gold) | rotation A CW / B CCW, speeds per state; sockets sequence TRANSCRIBING |
| L5 | 8 energy conduits + glyph ring textPath (ΗΕΡΜΗΣ ΟΔΟΣ ΛΟΓΟΣ) | conduit alpha per state; dashed when DEGRADED |
| L6 | portal disc, inner glow, Hermes geometric sigil (staff, twin S-waves, wings, diamond eye) | glow = `p` budget; amplitude scales glow in SPEAKING |
| L7 | foreground stairs ×7, gold nosing on 1/3/5, gateway arch glow, balustrade posts | gateway glow follows `p` |
| L8 | state seals: queue scroll, update seal, LOCAL CORE plate | only when real condition holds |
| L9 | messenger paths + winged sigils + trails + labels | only when real transport/queue/local events occur |

## 2. State mapping (15 states)

| State | Portal | Conduits | Rings (A°/B° sample frame) | Messengers | Extra |
|-------|--------|----------|-----------------------------|------------|-------|
| DORMANT | 0.10 sealed | 0.06 | static | none | structure at very low luminance |
| IDLE | 0.26 breathing | 0.24 | slow 16/−9 | none | OLED-idle budget, minimal particles |
| WAKE_DETECTED | 0.85 ignition | 0.55 | 6/−4 | local orbit | ONE expansion wave, sigil flash |
| LISTENING | 0.72 | 0.50 | 9/−6 | local orbit | waves CONVERGE inward; no tower path |
| TRANSCRIBING | 0.55 | 0.40 | 12/−8 | none | glyph sockets illuminate sequentially |
| UNDERSTANDING_LOCAL | 0.62 gold-cyan | 0.45 | 28/−14 | local orbit + LOCAL CORE plate | no fake tower link |
| CONTACTING_TOWER | 0.60 | 0.55 | 20/−12 | departure trail | connection state truthful |
| TOWER_PROCESSING | 0.80 | 0.70 | 63/−41 differentiated | departure + second trail | no completion indication |
| SPEAKING | 0.92 + amplitude | 0.55 | 12/−8 | arrival trail | captions visible; no lip animation |
| REQUEST_QUEUED | 0.42 | 0.30 | 8/−5 | none | amber scroll seal + REAL queue count |
| DISCONNECTED | 0.20 half-sealed | 0.16 | 6/−3 | broken path | local architecture stays operational |
| DEGRADED | 0.45 | 0.30 dashed | 14/−9 | sparse trail | clear DEGRADED label |
| ERROR | 0.38 | 0.25 | 10/−6 | none | controlled bronze-red fracture |
| PRIVACY_MUTED | 0.14 shutters closed | 0.05 | 0/0 | none | red-bronze privacy ring, mic state unmistakable |
| UPDATE_AVAILABLE | 0.32 | 0.24 | 8/−5 | none | small golden maintenance seal, no processing |

Every state provides: primary animation · static reduced-motion equivalent (frozen
representative frame, same information) · screen-reader text (`ConsoleState.accessibility`
preserved) · OLED-safe behavior (per-state `bd` bronze budget; resting states ≤0.55) ·
LOW/BALANCED/HIGH profile behavior.

## 3. Motion budgets (BALANCED default)

| Motion | Period | Source constant |
|--------|--------|-----------------|
| ring A rotation | 24 s/rev idle → 8 s/rev TOWER_PROCESSING | per-state |
| ring B counter-rotation | 40 s/rev idle → 14 s/rev | per-state |
| portal breathing | 4.8 s sine | IDLE_PULSE_MS class |
| listening converge | 1.8 s per wave, 3 waves | RING_MS class |
| messenger traversal | 3.2 s linear, eased fade at ends | STREAM_MS class |
| particle cadence | ≤6 (BALANCED), 0 (LOW), ≤12 (HIGH) | quality profile |

Reduced motion: all periods freeze to the sample frames above; the frame is chosen to
carry the same semantic markers (seals, shutters, fractures, trail presence).

## 4. Truthfulness invariants (core-specific)

- INV-CORE-01: no messenger trail may appear unless a real/simulated-designated transport
  or queue event is active (mock server events count as simulated and are labeled in
  diagnostics, not on the core).
- INV-CORE-02: queue seal shows the real `queueCount` state value, never a placeholder.
- INV-CORE-03: DISCONNECTED must visually break the tower path; DEGRADED must visibly
  dash it; ERROR must show the fracture. Never fake stability.
- INV-CORE-04: SPEAKING luminosity is a function of real TTS amplitude only.
- INV-CORE-05: PRIVACY_MUTED closes the input gates and suppresses ALL listening energy.
