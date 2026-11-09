# BRIDGE STATE AUTHORITY REPORT (Graph 4.14, CP-G4.14-004)

Owner finding repaired: F-03 — "the blue dot and the label BRIDGE OK are shown
while the application is in Mock mode… a misleading impression that a real
Hermes Tower is connected."

## 1. Current truth sources (read-back at CP-G4.14-001)

| Source | Truth it carries |
| --- | --- |
| `store.connection` (ConnectionStatus) | transport status (UNCONFIGURED/CONNECTING/CONNECTED/DISCONNECTED), endpoint label, TLS evidence, last handshake instant |
| Endpoint label scheme | `mock://` ⇒ the mock simulator; https/wss ⇒ real Tower class |
| `MockTowerBridge` | the ONLY bridge feeding `store.connection` in the current product (console path) |
| `RealTowerBridge` | Tower Connection Studio + Self-Test only; its StateFlow is NOT merged into the console (scope freeze: no pairing expansion) |

## 2. The authority (`domain/BridgeModeAuthority.kt`)

One resolver, one truth table — the UI never re-derives truth from
`status.name`:

| BridgeMode | IndicatorRole | Label (IT) | Color | Trigger |
| --- | --- | --- | --- | --- |
| MOCK_BRIDGE_HEALTH | MOCK | **SIMULAZIONE** | amber/bronze (never cyan) | endpoint = `mock://` (ANY transport state) |
| LOCAL_BRIDGE_HEALTH | LOCAL_READY | **BRIDGE LOCALE PRONTO** | neutral cyan-gray | real endpoint, UNCONFIGURED, no TLS |
| REAL_TOWER_TRANSPORT | REACHABLE | **TOWER RAGGIUNGIBILE** | amber | real endpoint CONNECTING, or CONNECTED with TLS ≠ ACTIVE |
| REAL_TOWER_AUTHENTICATION / REAL_TOWER_SESSION | CONNECTED | **TOWER CONNESSA** | cyan | real endpoint CONNECTED + TLS ACTIVE |
| REAL_TOWER_CAPABILITY_NEGOTIATION | OPERATIVE | **TOWER OPERATIVA** (+verified mark) | cyan | capability negotiation passed (typed `capabilityNegotiated=false` until a real negotiation exists) |
| DISCONNECTED | DISCONNECTED | **TOWER DISCONNESSA** | gray | real endpoint DISCONNECTED |
| FAILED | ERROR | **ERRORE BRIDGE** | error red | typed failure |

Hard rule enforced and tested: **a mock handshake can never resolve to the
cyan connected treatment** (BridgeModeAuthorityTest).

## 3. Consumers changed

- Console `StatusPill` (was: `pill_connected "BRIDGE OK"` + cyan dot on any
  CONNECTED — reachable purely by the mock handshake): now consumes the
  authority — SIMULAZIONE on amber in Mock, and a tap opens the truth panel.
- Drawer indicator (`AppNav`): the same authority drives dot color and the
  localized label (one truth, two surfaces).
- Truth panel (§12): compact dialog with application mode, endpoint class,
  transport state, authentication, session, capability negotiation, last
  successful check, typed limitation — plus the fixed Mock sentence
  "Nessuna Hermes Tower reale è collegata. Le risposte Tower sono simulate."
  No endpoint secrets are displayed (endpoint CLASS only, never URL/token).

## 4. Strings

24 new keys × 5 locales (it/en/es/fr/de): bridge_pill_* (7),
bridge_truth_* (13), desc_bridge_mock/desc_bridge_local,
bridge_truth_yes/no/na/pending/never. Parity maintained; French apostrophes
XML-escaped.

## 5. Evidence

- Code: domain/BridgeModeAuthority.kt; ConsoleScreen §10/§11 block; AppNav
  drawer block; Chrome.kt shared role mappings + StatusPill onClick seam.
- Tests: app/src/test/java/…/domain/BridgeModeAuthorityTest.kt (8 tests —
  mock-never-cyan ×4 statuses, real session cyan, reachable amber,
  LOCAL_READY, negotiation-pending, last-check passthrough).
- Unit suite at the checkpoint: **491/491 GREEN** (483 + 8).
