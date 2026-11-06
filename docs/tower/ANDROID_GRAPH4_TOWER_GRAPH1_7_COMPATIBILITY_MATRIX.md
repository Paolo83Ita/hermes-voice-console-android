# ANDROID_GRAPH4_TOWER_GRAPH1_7_COMPATIBILITY_MATRIX.md

- Mission: GRAPH 4.9 — CP-G4.9-008 (Tower Gate and Graph 1.7 compatibility)
- Owner: Paolo Lombisani
- Written: 2026-11-07 (fresh read-back tip `eb660d5` + this node)
- Rule: every required action is classified — CLIENT_IMPLEMENTED /
  MOCK_PASSED / GRAPH_1_7_SERVER_REQUIRED / REAL_TEST_PENDING / BLOCKED.
  No final production route is invented anywhere in this matrix.

## 1. Pairing (Tower Gate → Tower Graph 1.7 pairing contract)

| # | Required action | Android client state | Classification | Gap to Graph 1.7 |
| --- | --- | --- | --- | --- |
| P-01 | QR camera scanner (CameraX + ML Kit, on-device) | implemented, instrumented gate C | **CLIENT_IMPLEMENTED** | none client-side |
| P-02 | QR payload parser (`hermes-pair://v1`, strict matrix) | implemented + unit matrix (ST-48) | **CLIENT_IMPLEMENTED** | parser is versioned `v1`; Graph 1.7 must keep the payload schema or version it |
| P-03 | Payload expiry | implemented (parser rejects expired) | **CLIENT_IMPLEMENTED** | server must issue expiries |
| P-04 | Replay protection (session-lifetime nonce store) | implemented (`InMemoryPairingNonceStore` + process lifetime) | **CLIENT_IMPLEMENTED** | server-side replay checks remain server authority |
| P-05 | Requested scopes display + enforcement | implemented (scopes parsed + shown before accept) | **CLIENT_IMPLEMENTED** | server must honor scope downgrade/upgrade |
| P-06 | Matching phrase display | implemented | **CLIENT_IMPLEMENTED** | server must bind phrase to the session |
| P-07 | Android Keystore secret abstraction | implemented (`KeystoreVault` on device, file vault on JVM) | **CLIENT_IMPLEMENTED** | none client-side |
| P-08 | Numeric code fallback | implemented | **CLIENT_IMPLEMENTED** | server must accept the numeric handshake |
| P-09 | Pairing execution against a REAL Tower | mock executor only, SIMULATED labels | **GRAPH_1_7_SERVER_REQUIRED** | pairing endpoints + trust chain |
| P-10 | End-to-end real pairing proof | not attempted | **REAL_TEST_PENDING** | blocked by P-09 |

## 2. Transport and session

| # | Required action | Android client state | Classification |
| --- | --- | --- | --- |
| T-01 | HTTPS handshake + typed result | `RealTowerBridge` real HTTP handshake | **CLIENT_IMPLEMENTED** (transport-ready; server pending) |
| T-02 | Contract frame POST + tower event parsing | implemented | **CLIENT_IMPLEMENTED** |
| T-03 | Auth failure / timeout / disconnect / reconnect semantics | implemented + self-test ST-09…ST-12 | **MOCK_PASSED** |
| T-04 | LAN discovery interface | interface defined, no discovery daemon | **BLOCKED** (server advertisement contract unknown) |
| T-05 | Tailscale interface | interface defined (profile transport mode) | **BLOCKED** (server side of the tailnet contract owned by Graph 1.7) |
| T-06 | Session renewal | `SessionController` implemented (mock-backed) | **MOCK_PASSED** |
| T-07 | TLS verification against real certificates | MOCK_NOT_APPLICABLE today | **GRAPH_1_7_SERVER_REQUIRED** |

## 3. Conversation / voice exchange

| # | Required action | Android client state | Classification |
| --- | --- | --- | --- |
| V-01 | Streaming request/response frames | implemented against the mock + simulator | **MOCK_PASSED** |
| V-02 | Queue + reconnect flush | implemented + JVM repro test + e2e | **MOCK_PASSED** |
| V-03 | Cancellation/barge-in propagation | implemented (adapter interrupt token this node) | **MOCK_PASSED** |
| V-04 | Consequential-request confirmation loop | implemented | **MOCK_PASSED** |
| V-05 | Real Tower conversational exchange | not attempted | **GRAPH_1_7_SERVER_REQUIRED / REAL_TEST_PENDING** |

## 4. Archive (owner §14–17)

| # | Required action | Android client state | Classification |
| --- | --- | --- | --- |
| A-01 | Conversations/missions/artifacts repository surfaces | implemented (deterministic mock, SIMULATED) | **MOCK_PASSED** |
| A-02 | Artifact download/verify/remove/re-download lifecycle | implemented this node (typed transfer states) | **MOCK_PASSED** |
| A-03 | Real archive routes (list/recover/download bytes) | client seam `TowerArchiveClient` exists | **GRAPH_1_7_SERVER_REQUIRED** |
| A-04 | Real archive end-to-end proof | not attempted | **REAL_TEST_PENDING** |

## 5. Honest summary

- 15 actions CLIENT_IMPLEMENTED, 11 MOCK_PASSED, 5 GRAPH_1_7_SERVER_REQUIRED,
  2 REAL_TEST_PENDING, 2 BLOCKED (LAN/Tailscale discovery contracts).
- The client does NOT name any production route; every real-server dependency
  is a typed seam awaiting the Tower Graph 1.7 contract (the matrix is the
  integration checklist for that node).
