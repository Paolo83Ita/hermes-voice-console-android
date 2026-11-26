# PAIRING TOWER SERVER DELTA REQUEST — HR-A4R-PHYS-002 (v3 envelope + ack route)

**From:** Android repair mission (branch `graph-a4r/physical-truth-repair`, second GLM account dedicated to Android)
**To:** Main Tower account (Hermes Tower server owner)
**Produced:** 2026-11-26 — CP-ANDROID-REPAIR-004
**Supersedes:** HR-A4R-PHYS-001 (endpoint-class decision request) — that request's question is now closed by evidence and superseded by this concrete delta.
**Discipline:** The Android account does NOT mutate the Tower. This is a bounded, versioned server delta request. No secret values appear anywhere in this document.

---

## 0. Why this delta exists (one paragraph)

The Android client repair (CP-005) makes the phone capable of reaching the Tower through **every** network context: it parses multi-candidate envelopes, classifies transport failures, avoids LAN-only endpoints on cellular, and exposes manual fallback. But two server-side facts still block end-to-end pairing against the **live** Tower today:

1. **The QR emits `https://tower.local` as the ONLY endpoint** (Owner wizard screenshot 2026-11-26, PASSO 2/4). A phone on cellular can never resolve an mDNS `.local` name, and even on Wi-Fi the standard Android resolver does not resolve it in app processes. The qualified public endpoint `https://hermestower.aclclouds.online` is healthy, CA-valid, and serving the v2 claim/attest routes — but it is not in the QR.
2. **The ack route is absent** (GET `/api/tower/voice-console/v2/pairings/{id}/ack` → 404 catch-all, re-confirmed 2026-11-26; the POST 405 on the same path is an artifact of a GET-only catch-all route). Without it, the bilateral Owner confirmation (wizard PASSO 3/4) can never release the console token to the phone.

Until both land, the Android mission can only reach `PAIRING_CLIENT_IMPLEMENTATION_COMPLETE` + `TOWER_SERVER_DELTA_REQUIRED`, never `PAIRING_END_TO_END_COMPLETE`.

---

## 1. Requested changes (bounded, versioned)

### DELTA-A — QR envelope v3 with endpoint candidates (supersedes the single-endpoint field)

The QR payload stays `<base64url(payload)>.<base64url(signature)>` (unchanged framing). The payload gains version 3 with the fields in `PAIRING_ENVELOPE_V3_SCHEMA.json` (delivered in this package). Minimal required shape:

```json
{
  "v": 3,
  "protocol_version": "voice-console/2",
  "tower_public_id": "<stable public id>",
  "challenge_id": "<pairing id>",
  "nonce_hash_or_reference": "<opaque single-use nonce ref>",
  "single_use_binding": { "max_claims": 5, "replay_policy": "REJECT_ON_REPLAY", "binding_scope": "one-device-one-claim" },
  "issued_at": <epoch s>,
  "expires_at": <epoch s>,
  "verification_phrase_reference": { "kind": "REFERENCE_ONLY", "word_count": 3, "language": "it" },
  "endpoint_candidates": [
    {
      "transport_class": "QUALIFIED_PUBLIC_ENDPOINT",
      "url_or_host": "https://hermestower.aclclouds.online",
      "priority": 10,
      "network_requirement": "INTERNET_ANY",
      "tls_requirement": "CA_VALIDATED",
      "freshness": "AT_ISSUE",
      "expiry": <epoch s, <= envelope expires_at>,
      "source": "TOWER_QR",
      "qualification_state": "QUALIFIED"
    },
    {
      "transport_class": "LAN_IP",
      "url_or_host": "https://192.168.x.y:8443",
      "priority": 20,
      "network_requirement": "LAN_SAME_SUBNET",
      "tls_requirement": "LAN_SELF_SIGNED_EXPECTED",
      "freshness": "AT_ISSUE",
      "expiry": <epoch s>,
      "source": "TOWER_QR",
      "qualification_state": "QUALIFIED"
    }
  ],
  "transport_candidates": ["HTTPS_POST", "HTTPS_GET_POLL"],
  "certificate_trust_metadata": { "public_cert_sha256": "<sha256 of DER>", "public_cert_sans": ["hermestower.aclclouds.online"], "issuer_class": "LETSENCRYPT" },
  "scope_request": ["voice", "archive:read"],
  "capability_negotiation_version": "capneg/1"
}
```

Rules for the emitter:
- **Always include the QUALIFIED_PUBLIC_ENDPOINT candidate when the public plane is up** (the phone may pair from cellular — that is the physical reality this mission measured).
- LAN candidates (LAN_MDNS / LAN_IP) are optional additions, never the only class.
- Keep the TTL window 120-300 s; `expires_at` governs.
- **v2 compatibility:** if the Tower cannot ship v3 yet, an INTERIM one-line mitigation is available TODAY with zero code change: **emit the qualified public URL as the single v2 `endpoint`** instead of `tower.local`. The installed-v2 clients (and the CP-005 client) will then pass the claim transport from any network. (The ack-route delta below is still required for completion.)

### DELTA-B — ack route (GET `/api/tower/voice-console/v2/pairings/{id}/ack`)

- **Method:** GET; **Auth:** none during pairing (the pairing id + claim state is the authorization); **Response:** the in-band envelope `{"ok": true|false, ...}`.
- **States:** `PENDING` (Owner has not confirmed yet) / `ACKED` (Owner confirmed the phrase on the wizard; response carries `device_id` + `console_token` + `phrase_hint`) / `REVOKED` / `EXPIRED`.
- **Lifecycle:** the route answers from pairing creation until `expires_at` + 300 s grace; after that, `EXPIRED`.
- **Polling contract:** clients poll every 2 s within a 300 s window (already implemented client-side); the route must be idempotent, cheap, and rate-limit-tolerant of 2 s polls from one device.
- **Token release:** the `console_token` is released ONLY in the ACKED response (never at claim/attest), exactly as the client already expects.

### DELTA-C — in-band error envelope (confirm, do not change)

The Tower already answers `HTTP 200 {"ok": false, "error": "<CODE>"}` on the pairing routes (measured: `PAIRING_NOT_FOUND_OR_EXPIRED` on fake ids). Please KEEP this and confirm the stable error code list so the client catalog can pin fixtures. Requested codes to confirm: `PAIRING_NOT_FOUND_OR_EXPIRED`, `CLAIM_CODE_INVALID`, `CLAIM_EXHAUSTED`, `PAIRING_ALREADY_CLAIMED`, `ATTEST_INVALID`, `PAIRING_EXPIRED`, `PAIRING_REVOKED`, `RATE_LIMITED`.

### DELTA-D — device credential issuance + revocation (confirm the existing shape)

- `claim` request: `{"claim_code": "<6-digit>", "device_label": "<string>"}` → response `{"ok":true, "claim_key": "...", "challenge": "..."}`.
- `attest` request: `{"claim_key", "challenge", "pop_signature", "device_public_key_pem", "device_key_algorithm": "EC", "attestation_metadata": {...}}` → response `{"ok":true, "device_id": "...", "phrase_hint": "..."}`.
- Revocation path (Tower console side): revoking a device must make subsequent console-token calls answer 403/410 so the client maps to `DEVICE_REVOKED` (already typed client-side).
- **Capability negotiation:** confirm a post-pairing route (e.g. `POST /api/tower/voice-console/v2/g4/capabilities` or the equivalent you prefer) returning the capability set with `capability_negotiation_version`; the client will adapt to whatever route name you choose (one contract fixture update).

### DELTA-E — restart/rehydration semantics (confirm)

After a Tower restart mid-pairing: in-flight pairing ids stay valid until `expires_at`; acked-but-unfetched tokens remain fetchable within the grace window. After a Tower restart POST-pairing: console tokens remain valid (or are re-issued via re-attest, in which case document the re-attest route).

---

## 2. What is NOT requested

- No change to the QR framing/signature scheme.
- No change to claim/attest request bodies (they match the client's typed hypothesis, already live).
- No weakening of TLS anywhere; no new secrets in the QR (the forbidden-field list is enforced on both sides).
- No Tower core/architecture change; this is routes + QR emission only.

## 3. Tests the Tower side should add

1. QR v3 emission: payload parses, contains >= 1 INTERNET_ANY candidate when the public plane is up, TTL sane, no forbidden field.
2. Ack route: PENDING → ACKED transition on Owner confirm; token only at ACKED; EXPIRED after grace; REVOKED on cancel.
3. Envelope: every error path answers in-band `{"ok":false,"error":CODE}` with codes from the confirmed list.
4. Replay: a consumed nonce/pairing cannot claim twice (`PAIRING_ALREADY_CLAIMED`).
5. Revocation: revoked device token → 403/410 on a v2 console route.
6. Restart: pairing survives Tower restart within TTL (mid-pairing), token survives restart (post-pairing).

## 4. Rollback

Each delta is independently revertible: v3 QR emission falls back to v2 emission (clients accept both); the ack route can be disabled (clients degrade to the typed `PAIRING_ROUTE_MISSING` honest stop — never a bypass); capability negotiation is optional (clients degrade to DEGRADED state). No database migration is implied beyond storing pairing session state you already store.

## 5. Compatibility with the current QR program

The client (CP-005) accepts BOTH v2 single-endpoint payloads and v3 multi-candidate envelopes; v2 payloads carrying a public endpoint work fully; v2 payloads carrying `tower.local` produce the typed `NO_ELIGIBLE_ENDPOINT` failure with manual fallback (never a dead-end). The expected Android commit SHA at handoff is recorded in the delivery manifest (`HERMES_TOWER_ANDROID_PAIRING_SERVER_DELTA.zip`).

## 6. Handoff artifacts in this package

- `PAIRING_TOWER_SERVER_DELTA_REQUEST.md` (this file)
- `PAIRING_ENVELOPE_V3_SCHEMA.json`
- `PAIRING_ENDPOINT_RESOLUTION_CONTRACT.json`
- `PAIRING_TRUST_POLICY.json`
- `PAIRING_TYPED_FAILURE_CATALOG.json`
- `PAIRING_COMPATIBILITY_MATRIX.csv`
- `PAIRING_TLS_NETWORK_EVIDENCE.json`
- `PAIRING_PRIMARY_UX_CONTRACT.json`
- `HERMES_TOWER_ANDROID_PAIRING_SERVER_DELTA_PROMPT.md` (execution prompt for the main Tower account)
- `DELTA_MANIFEST.json` (SHA-256 of every file; expected Android commit SHA)
