# TOWER CONNECTION STUDIO — Graph 2 implementation

Status: IMPLEMENTED (cloud unit-tested + emulator E2E). Real Tower connectivity: never
claimed until a verified real endpoint is tested (no production endpoint exists yet).

## Entry points

- Onboarding flow wording: "Connect Hermes Tower" (the Studio screen itself).
- Main navigation drawer → **Hermes Tower Connection**.
- Connection screen → "Open Hermes Tower Connection Studio →".
- Disconnected-state action and Diagnostics → "Open Tower Connection Studio →".
- Self-Test Center relates via ST-04..ST-12.

## Basic fields (always visible)

Connection name · Tower address or base URL · Connection method (HTTPS / secure
WebSocket / HTTPS+secure WebSocket / Tailscale private address / Mock simulator) ·
Pairing code or access token (password field, vault-stored) · User or Owner reference ·
Device display name · Default operating mode · Preferred language · Connect
automatically · Use Tailscale address. Every option carries a one-line contextual
explanation; Mock mode requires no address and no secret.

## Advanced fields (collapsed by default)

REST base path · WebSocket path · protocol version · request timeout · streaming
timeout · bounded retry count · queued-request policy · local/Tower routing threshold ·
diagnostics verbosity · consequential-request confirmation switch · certificate behavior
(System trust / pinned SHA-256 fingerprint).

## Actions (all eleven)

Save draft · Validate fields · Test connection · Pair device · Save and connect ·
Connect (real handshake) · Disconnect · Clear sensitive values · Export secret-clean
profile · Import profile (conflict-checked) · Restore previous configuration ·
Reset to Mock mode.

## Error presentation

Every failure shows: a plain-language explanation, the typed code (e.g. AUTHENTICATION_FAILED,
PROTOCOL_MISMATCH, CONNECTION_TIMEOUT), the measured stage that failed, and one concrete
next action. Stack traces never reach the ordinary UI; redacted detail lives in
Diagnostics (LogRedactor-pinned, unit-tested).
