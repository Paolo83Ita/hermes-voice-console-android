# A4R QR REAL-BACKEND DIFFERENTIAL — A4R-PHYS-QR-001 (P1)

produced: 2026-11-21T05:05:00Z (Europe/Rome 06:05)
branch: graph-a4r/merge-with-b-base @ a58c630 · APK HERMES_VOICE_CONSOLE_A4R_REPAIRED_DEBUG.apk (code 13, sha256 26dbad0c…80922e)

## 1. What the Owner observed (evidence, not interpretation)

Two pairing attempts on the Xiaomi 11 Lite 5G NE (2026-11-20 16:51 and 16:53, Europe/Rome), phone on **4G+ cellular**, Tailscale chip "1 dispositivo":

| Step | Observed |
| --- | --- |
| Camera detection | QR recognized both times |
| v2 parse | "Payload accettato dal parser v2" — `tower=hermes-t…`, `proto=voice-console/2`, `scopes=voice.capture,voice.tts,tower.status.read`, `expiresIn=230s` / `278s` |
| Numeric code | `521021`, then `563783`, "Conferma e associa" tapped both times |
| Result | Typed state **"Endpoint di pairing non raggiungibile"**, detail link **"Tower non raggiungibile"** |
| App state | Remained open, no crash, no stack trace shown |

## 2. What the typed state means in the client (static, a58c630)

`Tower non raggiungibile` is the exact detail string bound to `TowerApiException.Kind.NETWORK_UNREACHABLE` — raised by `HttpUrlTowerHttp.execute` on **any IOException before an HTTP response** (DNS, TCP connect, TLS handshake, or Android cleartext block). `PairingFlow.mapApiException` maps it to `PAIRING_ENDPOINT_UNREACHABLE`. Had ANY HTTP response arrived, the UI would have shown a different typed state ("richiesta rifiutata dalla Tower" for 4xx/5xx, "risposta claim incompleta" for malformed bodies). BAD_ENDPOINT would have shown "Nessun endpoint disponibile (usa l'inserimento manuale)". **None of those appeared ⇒ the request never got a response.**

## 3. Live Tower plane probes (lane, 2026-11-21 — read-only; POST only on FAKE pairing ids with empty bodies; no state created; no secrets)

| Probe | Result | Reading |
| --- | --- | --- |
| DNS | 141.11.237.119 | resolves |
| TLS | Let's Encrypt, CN=hermestower.aclclouds.online, valid 2026-11-12 → 2027-02-10 | healthy |
| GET / | 302 → /login | plane alive |
| POST `…/pairings/<fake>/claim` `{}` | **200** `{"ok":false,"error":"PAIRING_NOT_FOUND_OR_EXPIRED"}` | **claim route EXISTS** |
| POST `…/pairings/<fake>/attest` `{}` | **200** `{"ok":false,"error":"PAIRING_NOT_FOUND_OR_EXPIRED"}` | **attest route EXISTS** |
| GET `…/pairings/<fake>/ack` | **404** catch-all `{"detail":"No such API endpoint: …"}` | **ack route ABSENT** (AMB-8) |
| GET /openapi.json, /docs | 302 → /login | schema not public |
| GET /api/openapi.json | 401 unauthenticated | schema auth-gated |

## 4. Boundary-by-boundary verdicts

| # | Boundary | Verdict |
| --- | --- | --- |
| 1 | camera detection | OK on device |
| 2 | QR framing `payload.signature` | OK on device |
| 3 | base64url + JSON parse | OK on device |
| 4 | signature extraction | OK (client does not verify — B bound behavior preserved, no bypass) |
| 5 | protocol version (v=2, voice-console/2) | OK on device |
| 6 | expiry / clock skew | OK on device (230s/278s accepted) |
| 7 | forbidden-field scan | OK on device |
| 8 | endpoint resolution | RESOLVED — value not displayed (redaction-safe UI); exact scheme/host class **UNOBSERVED** |
| 9 | **TLS/cleartext/transport** | **FAIL HERE — transport-level, pre-HTTP** |
| 10 | claim route | route present on live Tower; never reached by the phone |
| 11 | claim request DTO `{claim_code, device_label}` | AMB-2 hypothesis still unconfirmed (needs real pairing) |
| 12 | claim response DTO | **divergence proven**: live Tower returns failures as HTTP 200 `{ok:false,…}`; client reads neither `ok` nor `error` (AMB-9) |
| 13 | HMAC claim-secret | static only (not exercised) |
| 14 | EC-P256 PoP | repaired in C1-C4, instrumented 14/14; not exercised on device |
| 15 | attest route | present on live Tower; never reached by the phone |
| 16 | PendingOwnerAck GET ack | **PROVEN ABSENT on live Tower** (AMB-8 — must fix before any Q6/Q7 gate) |
| 17 | encrypted persistence | repaired; instrumented 14/14; not exercised on device |

## 5. Root cause (evidence-ranked)

**Proven layer:** transport failure before any HTTP response to the QR-carried endpoint, while the public Tower plane is healthy and serves claim/attest. The QR endpoint was therefore **not reachable from the phone** (on 4G cellular at failure time).

**Surviving endpoint classes (HIGH → LOWER):** LAN-only endpoint (RFC1918 unreachable from cellular; cleartext also blocked by NSC even on Wi-Fi) · Tailscale-only endpoint (Tower PC not reachable in tailnet from cellular) · cleartext `http://` public host (blocked by NSC on any network) · private-TLS endpoint (lower — public plane uses valid LE).

**Decisive missing evidence (one item):** the endpoint scheme/host class the Tower wizard embeds in the QR. Three paths: (1) Owner screenshot of the full Tower wizard window showing endpoint text (QR excluded); (2) V2 secret-clean diagnostics export with transport classification; (3) Tower handoff answer.

## 6. Contract divergences (versioned delta requests, no bypass)

- **AMB-8** — GET `/pairings/{id}/ack` missing on live runtime (client implements it). Must fix before bilateral confirmation.
- **AMB-9** — failure envelope `{ok:false,error:…}` with HTTP 200; client would mislabel as CLAIM_RESPONSE_INCOMPLETE. Client adapter + fixture pin.
- **AMB-10** — QR `endpoint` emission policy unknown (LAN/tailnet/public). Handoff question.

## 7. Repair plan (V2)

R1 transport-failure typed sub-states (DNS/TCP/TLS/CLEARTEXT/HTTP_STATUS) in diagnostics · R2 ok/error envelope adapter · R3 Owner-triggered EXPORT SECRET-CLEAN DIAGNOSTICS extended to pairing boundaries · R4 contract delta HR-A4R-PHYS-001 · R5 environment guidance (phone must reach the QR-carried endpoint — same LAN or tailnet with Tower present). No schema change to make one sample pass; no signature, TTL, replay, forbidden-field, plaintext or hardcoding changes.
