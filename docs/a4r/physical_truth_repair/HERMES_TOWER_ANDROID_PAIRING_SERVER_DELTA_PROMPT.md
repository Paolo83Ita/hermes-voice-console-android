# EXECUTION PROMPT — HERMES TOWER ANDROID PAIRING SERVER DELTA (HR-A4R-PHYS-002)

**To be executed by:** the main Tower account (Hermes Tower server owner).
**Do NOT execute from the Android account.** The Android mission does not have Tower repository access (TOWER_REPOSITORY_ACCESS_FROM_ANDROID_ACCOUNT = UNAVAILABLE_OR_NOT_AUTHORIZED) and must not mutate the Tower.

**Expected Android commit SHA at handoff:** `ad80b9bf24dc042114221c5fb321dfa105cd1f5b`
**Package produced:** 2026-11-26, Android repair mission, branch `graph-a4r/physical-truth-repair`.

---

## WHAT YOU ARE ASKED TO DO

Implement the bounded, versioned server delta described in `PAIRING_TOWER_SERVER_DELTA_REQUEST.md` (in this ZIP). In summary:

1. **DELTA-A (QR emission):** emit the pairing QR with endpoint candidates (envelope v3 per `PAIRING_ENVELOPE_V3_SCHEMA.json`), ALWAYS including the qualified public endpoint candidate when the public plane is up. INTERIM ZERO-CODE MITIGATION AVAILABLE TODAY: emit the qualified public URL as the single v2 `endpoint` instead of `tower.local` — the repaired client then completes the transport from any network.
2. **DELTA-B (ack route):** serve `GET /api/tower/voice-console/v2/pairings/{id}/ack` with the in-band envelope, PENDING -> ACKED (releasing `console_token` ONLY at ACKED) / REVOKED / EXPIRED, idempotent, 2 s poll friendly, grace window expires_at + 300 s.
3. **DELTA-C (error envelope):** confirm the stable in-band error code list (`PAIRING_NOT_FOUND_OR_EXPIRED`, `CLAIM_CODE_INVALID`, `CLAIM_EXHAUSTED`, `PAIRING_ALREADY_CLAIMED`, `ATTEST_INVALID`, `PAIRING_EXPIRED`, `PAIRING_REVOKED`, `RATE_LIMITED`).
4. **DELTA-D (credential issuance/revocation + capability negotiation):** confirm the claim/attest request/response shapes (they match the client's already-live typed hypothesis) and a post-pairing capability route.
5. **DELTA-E (restart semantics):** confirm pairing-id validity across Tower restart within TTL and console-token survival across restart.

## VERIFICATION (what closes the Android P0)

After deploying the delta, the Android Owner retest (CP-012 package, delivered separately) runs the full physical chain: QR scan -> claim -> attest -> phrase match -> ack -> credential -> connection test -> capability negotiation -> PAIRED -> restart read-back -> revoke -> re-pair. The Android client is ALREADY contract-complete for all of it (v2+v3 envelopes, typed transport classification, per-candidate resolution, honest AMB-8 state).

## EVIDENCE IN THIS PACKAGE

- `PAIRING_TLS_NETWORK_EVIDENCE.json` — the live-plane measurements this request is grounded on (public endpoint healthy, CA valid to 2027-02-10; ack GET = 404 catch-all; claim/attest in-band envelopes on fake ids).
- `PAIRING_ENVELOPE_V3_SCHEMA.json` / `PAIRING_ENDPOINT_RESOLUTION_CONTRACT.json` / `PAIRING_TRUST_POLICY.json` / `PAIRING_TYPED_FAILURE_CATALOG.json` / `PAIRING_COMPATIBILITY_MATRIX.csv` — the full client-side contract the server delta must satisfy.
- `PAIRING_PRIMARY_UX_CONTRACT.json` — what the Owner will see on the phone.

## RULES

- No secret values anywhere (none exist in this package).
- Rollback: each delta is independently revertible (v3 falls back to v2; ack route off -> the client degrades to the typed REPAIR_AVAILABLE state, never a bypass).
- Tests expected server-side are listed in the request document (section 3).
