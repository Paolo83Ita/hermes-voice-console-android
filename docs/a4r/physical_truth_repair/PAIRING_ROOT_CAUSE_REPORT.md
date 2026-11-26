# PAIRING ROOT CAUSE REPORT — APP-P0-01 / A4R-PHYS-QR-001

**Mission:** HERMES VOICE CONSOLE ANDROID — FULL PHYSICAL-TRUTH REPAIR
**Produced:** 2026-11-26 (UTC) — CP-ANDROID-REPAIR-003
**Source basis:** bound source tree `graph-a4r/merge-with-b-base` @ `a58c630` (app content identical through `8863b61`), the APK that was physically installed on the Xiaomi 11 Lite 5G NE.
**Evidence basis:** 2 device failure screenshots (2026-11-20 16:51/16:53), 1 Tower-side wizard screenshot (2026-11-26 11:13, Owner-supplied), lane probes of the live Tower public plane (2026-11-20/21), static binding of the delivered source (git-identity-verified).

---

## 1. Owner verdict being explained

`APP-P0-01 — Tower pairing does not work.`

Physical truth: the camera scan works, the QR v2 payload parses, the six-digit code is accepted by the UI — and then every attempt ends on the same typed failure: **"Endpoint di pairing non raggiungibile — Tower non raggiungibile"**. The app stays open; no crash; no stack trace.

## 2. The failing transition (proven)

Of the eight reconstructed transitions (see `PAIRING_STATE_MACHINE.json`), the chain dies at **transition 4: the claim request transport**:

```
QrScannerScreen → TowerQrV2ScanFlow → QrPayload.Parser        [OK — observed twice on device]
  → resolveEndpoint                                          [OK — selects QR endpoint verbatim]
  → TowerStore.persistEndpoint (AndroidKeyStore)             [OK — the A4R-P1-001 repair holds]
  → TowerApi.claim  POST {endpoint}/api/tower/voice-console/v2/pairings/{id}/claim
                                                             *** FAILS ***
  → IOException → TowerApiException.NETWORK_UNREACHABLE
  → PairingFlow → Step.Failed(PAIRING_ENDPOINT_UNREACHABLE, "Tower non raggiungibile")
```

The UI text is the exact bound string of the `NETWORK_UNREACHABLE` mapping — the failure is transport-level, before any HTTP response.

## 3. Root cause (closed this session)

The Owner's 2026-11-26 screenshot of the Tower-side wizard ("WIZARD DI ABBINAMENTO CONSOLE VOCALE — PASSO 2 DI 4") answers the one question the previous forensic phase left open — **what endpoint class the QR carries**:

> **Endpoint temporaneo: `https://tower.local`**

`tower.local` is a **LAN-scoped mDNS hostname**. The failure is therefore a **stacked, deterministic impossibility** for the phone's network context:

| # | Layer | Fact | Result |
|---|-------|------|--------|
| 1 | DNS | mDNS is link-local multicast; the phone was on **4G+ cellular** (status bar in every capture). Android's standard resolver (`HttpURLConnection`/bionic `getaddrinfo`) does **not** resolve `.local` names in app processes even on Wi-Fi (MIUI included) — Android offers NSD for *service* discovery, not host resolution in the socket API. | `UnknownHostException` on cellular — *always*; unreliable at best on the home LAN |
| 2 | TLS | The Tower's certificate is Let's Encrypt `CN=hermestower.aclclouds.online` (valid 2026-11-12 → 2027-02-10, lane-verified). It does **not** cover `tower.local`; a self-signed `tower.local` cert would be untrusted on Android. | Even with DNS fixed, the handshake fails (name mismatch / untrusted) |
| 3 | Reachable plane unused | `https://hermestower.aclclouds.online` is lane-proven healthy and serves the v2 `claim`/`attest` routes — but the QR does not carry it, and the client has **no endpoint candidates, no LAN discovery, no MagicDNS surface, no visible manual lane** (the manual/numeric entry code from the A3 wave is dead code, superseded by the B lane and unreachable in navigation). | A working plane exists and is never tried |
| 4 | Diagnostics | `TowerApi` collapses every `IOException` into one `NETWORK_UNREACHABLE` ("Tower non raggiungibile") with no DNS/TCP/TLS classification. | The Owner cannot tell "wrong network for this name" from "Tower down" (APP-P1-06) |

**Root cause statement:** the Tower's QR emission policy puts a LAN mDNS endpoint into the QR as the *only* endpoint, while the phone attempts pairing from a network context that cannot resolve or reach it (and would fail TLS even if it could); the Android client offers no alternate endpoint mechanism and no transport classification, so pairing deterministically dead-ends at the claim request. Neither the QR schema nor the camera/parse/keystore stages are at fault — they were all observed working on the device.

**Gate reclassification stands:** `QR_V2_SCAN_PASS = NOT_PROVEN`, `PAIRING_CLAIM_START_PASS = FAIL_OR_BLOCKED`, `XIAOMI_KEYSTORE_PAIRING_REPAIR_PASS = NOT_DERIVABLE` (persistence was reached and held, but the gate is not certified end-to-end), `GRAPH_A4R_PASS = false`.

## 4. Two contract divergences waiting behind the transport fix

Proven by lane probes against the live Tower (read-only, fake ids, no state created):

- **AMB-8 — the ack route is absent.** The client's bilateral-confirmation poll `GET /api/tower/voice-console/v2/pairings/{id}/ack` answers **404 catch-all** ("No such API endpoint"), while `claim` and `attest` exist and answer in-band. Even with transport repaired, transition 7 cannot complete against the live Tower as-is.
- **AMB-9 — the in-band envelope is unread.** The Tower reports errors as `HTTP 200 {ok:false, error:...}`; the client parses only the success fields, so an expired/replayed pairing would surface as "risposta claim incompleta" (`MALFORMED_RESPONSE`) instead of the Tower's typed reason.

Handling per the mission's repair discipline: **no client-side bypass, no Tower core modification** — a versioned Tower-side contract decision request (`HR-A4R-PHYS-001`, already filed, updated with the closed endpoint class) plus a client-side envelope adapter with pinned contract fixtures.

## 5. Repair plan (client-side, smallest canonical path — CP-004/005)

1. **Transport classification (R1):** typed sub-states `DNS_FAILED / TCP_CONNECT_FAILED / TLS_HANDSHAKE_FAILED / CLEARTEXT_BLOCKED / HTTP_STATUS(n)` in `TowerHttp`, surfaced in the pairing failure detail and the secret-clean diagnostics export.
2. **Envelope adapter (R2):** read `ok`/`error` before DTO parsing; JVM contract fixtures pin both envelope shapes.
3. **Endpoint candidates (R3):** QR endpoint first, then Owner-configured alternates (public HTTPS, tailnet MagicDNS name, LAN address), each attempt classified; `ADVANCED_MANUAL` visible per charter §6. No endpoint is ever hardcoded.
4. **LAN discovery (R4):** `NsdManager`-based discovery so the phone can find the Tower on the home Wi-Fi without `.local` hostname resolution.
5. **Tailscale/MagicDNS (R5):** accept and guide tailnet names (`…ts.net`) as candidates.
6. **Contract delta update (R6):** ask the Tower side (versioned decision request) to carry a reachable endpoint or an endpoint candidate list in the QR (AMB-10).
7. **Primary user path (R7):** first-run `WELCOME → CONNECT TO HERMES TOWER → SCAN QR → VERIFY PHRASE → OWNER CONFIRMATION → CONNECTION TEST → PAIRED`; prominent "Connetti alla Tower" card on Home when unpaired; `Settings → Connessione alla Tower → Pairing e dispositivi`; `Diagnostics → Tower Connection → Test/Retry/Revoke/Pair again`; all pairing paths ≤ 2 taps from Home except advanced manual.

## 6. What is NOT claimed

`VOICE_CONSOLE_PAIRED_AND_VERIFIED`, `PAIRING_PHYSICALLY_VERIFIED`, `GRAPH_A4R_PASS`, `ANDROID_CANONICAL_BASE_SELECTED` — none of these. The root cause is proven and closed at the class level; the fix lands only with build + regression evidence, and physical verification belongs to the Owner's retest package (charter §17).

*Secret hygiene: the expired ephemeral pairing values visible in the Owner's wizard screenshot (pairing code, numeric code, three-word phrase) are redacted from every lane artifact; the raw screenshot is retained unmodified as Owner evidence only.*
