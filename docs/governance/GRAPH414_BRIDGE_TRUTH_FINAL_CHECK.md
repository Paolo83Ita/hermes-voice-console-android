# GRAPH 4.14 — BRIDGE TRUTH FINAL CHECK (CP-G4.14-C04)

§5: physical-and-preview verification that Mock mode can NEVER appear as a
real Tower connection, and that the real-state projections remain available.

## NODE
CP-G4.14-C04 (bridge leg).

## FINDING

### The ONE authority
`BridgeModeAuthority.resolve(connection)` (domain/BridgeModeAuthority.kt) —
derives the truth mode from endpoint class + transport status + TLS evidence:

| Mode | Indicator role | Label (values-it) | Dot color | Verified by |
| --- | --- | --- | --- | --- |
| MOCK_BRIDGE_HEALTH | MOCK | **SIMULAZIONE** | amber (m.amber) | BridgeModeAuthorityTest `mock connected — SIMULAZIONE amber, never BRIDGE OK cyan` |
| LOCAL_BRIDGE_HEALTH | LOCAL_READY | BRIDGE LOCALE PRONTO | bronzeLight | unit test (all transport states) |
| REAL_TOWER_TRANSPORT | REACHABLE | TOWER RAGGIUNGIBILE | amber | unit test |
| REAL_TOWER_AUTHENTICATION/SESSION | CONNECTED | TOWER CONNESSA | cyanCore | unit test (requires tls==ACTIVE) |
| REAL_TOWER_CAPABILITY_NEGOTIATION | OPERATIVE | TOWER OPERATIVA | cyanCore + verified mark | typed (negotiation gate) |
| DISCONNECTED | DISCONNECTED | TOWER DISCONNESSA | inkSecondary | unit test |
| FAILED | ERROR | ERRORE BRIDGE | redBronze | mapping table |

### Mock truth — verified properties
- The console bridge is MockTowerBridge ONLY; RealTowerBridge's StateFlow is
  NEVER mirrored into store.connection (Connection Studio / Self-Test only).
- Mock endpoint (`mock://tower.simulator` / TowerEndpoint.MOCK) resolves to
  role=MOCK in EVERY transport state — the mock handshake outcome can never
  produce "BRIDGE OK", cyan, TOWER OPERATIVA or any real-connection state.
- The old cyan "BRIDGE OK" pill strings (`pill_connected` etc.) are REMOVED
  from all 5 locales (this continuation's §17 repair) — the misleading label
  class is deleted, not just unused.
- The Tower renderer consumes the SAME authority (`bridgeIsMock`): mock mode
  drives an AMBER pulse and forbids cyan remote-energy animation (§40) —
  verified in ReferenceTowerVisual + the animation evidence
  (`docs/visual/tower414/animation/MOCK_MODE.png`).
- Truth panel rows (ConsoleScreen L774+): mode / endpoint class / transport /
  auth / session / capability / last check / typed limitation; mock carries
  LIMITATION_MOCK and the §12 explanation (no real Tower connected).

### Screenshot/preview evidence (produced this session)
- `docs/visual/tower414/renders/BRIDGE_TRUTH_PILLS_OBSIDIAN.png` and
  `..._MARBLE.png` — all 7 roles with the EXACT shipped dot colors.
- `docs/visual/tower414/renders/CONSOLE_MOCK_MODE_OBSIDIAN.png` and
  `..._MARBLE.png` — the Mock console: amber SIMULAZIONE pill, amber tower
  pulse, explicit "AMBER PULSE — NO CYAN", composer + Interrompi chrome.
  These are OFFLINE PREVIEWS from the shipped token values (no emulator in
  the sandbox); the on-device screenshot leg belongs to the Owner retest
  (item 2/8 — typed PENDING, never self-granted).

### Real-state projections
REACHABLE / CONNECTED / OPERATIVE remain available for REAL_TOWER endpoints —
gated by transport + TLS evidence (authenticated = tls ACTIVE), never by the
mock path. No production pairing is claimed (Rule Theta).

## ACTION
Re-verified the authority, its consumers (Console pill, drawer, truth panel,
Tower renderer), the removed legacy strings, and produced the preview
evidence set.

## TEST
- BridgeModeAuthorityTest in the 496/496 suite; lint 0/72; secret scan 0.

## FILES
- domain/BridgeModeAuthority.kt + ui/components/Chrome.kt (role→label/color
  maps) + ConsoleScreen.kt (pill/truth panel/Tower input) — verified.
- values*/strings.xml — 13 orphaned keys removed (incl. the legacy cyan pills).
- docs/visual/tower414/renders/* (new evidence).

## EVIDENCE
- Test results + renders (above); BridgeTruthPanel row list in code.

## CHECKPOINT
CP-G4.14-C04 — Bridge and theme final truth: DONE.

## NEXT
CP-G4.14-C05 — Model Library final qualification.
