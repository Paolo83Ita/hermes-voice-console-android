# TOWER BRIDGE PROPOSED CONTRACT — hermes.console.v0.1

| | |
|---|---|
| Owner | Paolo Lombisani |
| Version | v0.1.0-alpha (proposal — the real Hermes Tower API contract is not yet attached) |
| Date | 2026-10-30 |
| Status | For Owner review |
| Hard rules | No real Tower routes are invented. No real connectivity is claimed. All assumptions live in ASSUMPTION_REGISTER.md. The transport adapter is replaceable. |

## 1. Transport shape (proposal)

- **Primary:** WebSocket (WSS) for session lifecycle + streaming (progress, token deltas).
- **Secondary:** HTTPS for single-shot operations (handshake test, health, receipt fetch) — same authentication, same envelope.
- One JSON envelope for all frames; `protocol` field enables version negotiation. The console sends `hermes.console.v0.1`; the Tower replies with the same or a higher minor. Unknown fields are ignored (`ignoreUnknownKeys`), never fatal.

## 2. Frame envelope

```json
{
  "protocol": "hermes.console.v0.1",
  "type": "REQUEST",
  "requestId": "req-000123",
  "timestamp": "2026-10-30T14:03:22.101Z"
}
```

`type` ∈ { HELLO, SESSION_OPEN, SESSION_CLOSE, REQUEST, ACK, PROGRESS, DELTA, FINAL, CANCEL, ERROR, RECEIPT_REF, PING }.

## 3. Message schemas (V0.1 code implements these as data classes; the mock bridge emits them)

### 3.1 HELLO (console → tower, on connect)

```json
{
  "protocol": "hermes.console.v0.1", "type": "HELLO", "timestamp": "…",
  "deviceId": "console-m2101k9g-01",
  "deviceModel": "Xiaomi 11 Lite 5G (verify on device)",
  "appVersion": "0.1.0-alpha",
  "capabilities": ["stt:it-IT", "tts", "captions", "privacy_mute", "queue"],
  "protocolVersion": "hermes.console.v0.1"
}
```

### 3.2 SESSION_OPEN (console → tower)

```json
{ "type": "SESSION_OPEN", "sessionId": "sess-…", "conversationId": "conv-…",
  "userIdRef": "owner-paolo", "language": "it-IT", "requestedMode": "HYBRID" }
```

### 3.3 REQUEST (console → tower) — the voice turn

```json
{
  "type": "REQUEST", "sessionId": "sess-…", "conversationId": "conv-…", "requestId": "req-000123",
  "transcript": "piano di missione per domani",
  "language": "it-IT", "confidence": 0.92,
  "requestedMode": "HYBRID", "effectiveMode": "HYBRID",
  "localRouting": { "decision": "ROUTE_TOWER", "reason": "consecutive pattern: mission",
                    "candidateIntent": "mission_plan", "consequential": true },
  "timestamp": "…"
}
```

### 3.4 ACK → PROGRESS → DELTA* → FINAL (tower → console)

```json
{ "type": "ACK",      "requestId": "req-000123", "stage": "ACCEPTED" }
{ "type": "PROGRESS", "requestId": "req-000123", "stage": "PLANNING" }
{ "type": "DELTA",    "requestId": "req-000123", "deltaText": "Ecco il " }
{ "type": "FINAL",    "requestId": "req-000123", "finalText": "…", "state": "COMPLETED", "receiptRef": "rcp-…" }
```

`state` ∈ { COMPLETED, DECLINED, NEEDS_APPROVAL, DEFERRED } — the console maps these to truthful UI (NEEDS_APPROVAL surfaces the Owner-approval requirement; it is never auto-approved).

### 3.5 CANCEL (either direction), ERROR, RECEIPT_REF

```json
{ "type": "CANCEL", "requestId": "req-000123", "reason": "OWNER_BARGE_IN" }
{ "type": "ERROR",  "requestId": "req-000123",
  "error": { "classification": "TRANSPORT", "retryable": true, "message": "…no secrets…" } }
{ "type": "RECEIPT_REF", "requestId": "req-000123", "receiptRef": "rcp-…" }
```

### 3.6 Error classification (closed set)

`AUTH` · `TRANSPORT` · `PROTOCOL` · `CAPACITY` · `TOWER_INTERNAL` · `CANCELLED` · `INVALID_REQUEST`

Console policy: TRANSPORT (retryable) → queue/visible retry; TOWER_INTERNAL non-retryable → turn FAILED with reason; AUTH → pairing surface, never retry-spam; CANCELLED → silent return to IDLE.

## 4. Field dictionary (required coverage per mission)

device identity · user identity reference (pseudonymous `userIdRef`, never raw PII) · session ID · conversation ID · request ID · transcript · language · confidence · requested mode · effective mode · local routing decision · Tower acceptance · progress events · streamed response text · cancellation · final state · error classification · receipt reference · timestamp · protocol version — **all present in the schemas above.**

## 5. Constraints and non-goals

- Payloads contain **no secrets and no personal data beyond the transcript**; the transcript itself is the minimum necessary content.
- No remote shell, no arbitrary command execution, no file push/pull in the contract. The Tower instructs; the console executes only whitelisted device functions (future, explicit).
- Limits (proposal): transcript ≤ 4,000 chars per request; ≥ 1 heartbeat/30 s; request backlog ≤ 20 items queued console-side.
- Versioning: minor bumps additive; major bump requires re-pairing and UI disclosure.

## 6. Mock behavior implemented in V0.1

`MockTowerBridge` + `DeterministicTowerSimulator`: deterministic keyword-driven responses, streaming DELTA cadence ~90 ms, injectable faults (disconnect-at-ack, tower-error-after-progress, slow), receipt refs `rcp-mock-####`, handshake latency 350 ms. Unit tests pin the sequences.

## 7. Assumptions carried (see ASSUMPTION_REGISTER.md)

A-20 contract shape unapproved · A-21 transport WSS unconfirmed · A-22 auth scheme undefined · A-23 language tagging it-IT · A-24 receipt semantics undefined. Each blocks only V0.4+ work, not the prototype.
