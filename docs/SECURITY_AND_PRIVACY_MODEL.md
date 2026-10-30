# SECURITY AND PRIVACY MODEL — HERMES VOICE CONSOLE ANDROID

| | |
|---|---|
| Owner | Paolo Lombisani |
| Version | v0.1.0-alpha |
| Date | 2026-10-30 |
| Status | For Owner review |

## 1. Threat model (STRIDE-lite, desk-device context)

| Threat | Vector | Control |
|---|---|---|
| Secret leakage | Token in source, config, logs, screenshots | SEC-01/02/06: no secrets in V0.1 at all; Keystore later; LogRedactor on every exported line |
| Eavesdropping on voice | Network interception to Tower | TLS mandatory (SEC-03); mock transport explicitly labeled `TLS: N/A — mock` |
| Unauthorized Tower access | Stolen pairing credential | Out-of-band pairing designed before any credential; device-bound key (Keystore); revocation by Tower |
| Covert listening | Hidden capture | SEC-04/08: explicit LISTENING visual, mic-state indicator, prominent privacy mute gating capture; no capture while muted |
| Data retention abuse | Accumulated audio/transcripts | SEC-05: audio retention OFF by default; transcript retention window configurable; delete controls in History |
| Malicious Tower commands | Compromised/impersonated Tower | Tower cannot execute consequential actions without Owner approval flow (Tower-authority model); console executes no consequential local actions in V0.1 |
| Privilege escalation | Root/ADB abuse | Device stock; no root; no remote shell; no ADB bridge; SEC-07 |
| Supply chain | Malicious dependency | Minimal dependency set; versions pinned in version catalog; licenses recorded in About & Safety |
| Screen content exposure | Guests reading transcript | Captions/history are Owner-facing; screen rest/black idle mode (V0.2) reduces ambient exposure |

## 2. Secrets policy

1. **V0.1 contains zero secrets by construction**: no endpoint, no token, no pairing material exists. The Connection screen holds a placeholder field that is not persisted and never leaves the device.
2. No production token in configuration files — there are no configuration files carrying credentials, and CI/build scripts contain none.
3. From V0.5, device-bound secrets (pairing token / device key) are generated at pairing time into **Android Keystore**: non-exportable, `ENCRYPT | SIGN` purposes, invalidated on key deletion; optional StrongBox when available. The console UI never re-displays a token after entry — it shows only existence and last-use metadata.
4. Logs and screenshots: every exported diagnostic line passes through `LogRedactor` (patterns: `token|key|secret|password|bearer` assignments, hex runs ≥ 24, base64-like runs ≥ 32) before leaving the process. Unit-tested.

## 3. Pairing design (to be implemented — V0.5, nothing real before approval)

1. Console displays a short, rotating, device-generated code; the Owner enters it in the Tower's admin surface (or scans a QR).
2. Tower returns a device identity + issues a scoped device token over TLS; the token is stored Keystore-bound to the device.
3. Revocation: Tower-side revocation plus console-side "forget pairing" (About & Safety → Reset local data).
4. Factory reset destroys Keystore material — pairing is void after reset; the checklist captures this.
5. The protocol payloads carry **no secrets and no personal data beyond the spoken transcript** (which is functionally necessary and minimized).

## 4. Microphone and listening honesty

- One audible-visible contract: **if audio is being captured, the Tower state shows LISTENING (or WAKE_DETECTED) and the mic indicator is active. If PRIVACY_MUTED is shown, no capture is possible.** The wake-word detector is the only always-on consumer and is itself disabled by the privacy mute.
- No hidden always-listening behavior; no audio telemetry; no "anonymous" usage upload.
- V0.1 does not capture audio at all; the simulation injects text. The indicator therefore shows "simulated" context truthfully.

## 5. Data inventory and retention

| Data | Where | Default retention | Control |
|---|---|---|---|
| Speech audio | RAM only | Never persisted (V0.1: never captured) | Audio retention switch = OFF (enabling is a later, deliberate act) |
| Transcript turns | In-memory ring (V0.1) | Session-scoped | History: delete per turn, clear all, retention window setting |
| Local event log | In-memory ring, 500 entries | Session-scoped | Export (redacted) from Diagnostics |
| UI settings | SharedPreferences (non-sensitive) | Until reset | Reset local data in About & Safety |
| Credentials | None exist | — | Keystore policy from V0.5 |

## 6. Prohibited by design (permanent for this mission's V-line)

- Remote shell, arbitrary command execution, unrestricted ADB bridge.
- Root, bootloader unlock, custom recovery/ROM.
- Hidden or non-indicated capture; automatic consequential actions; privilege escalation.
- A second authority: the console never confirms, receipts, or "completes" consequential Tower work.

## 7. Update and integrity

- Side-loaded debug APKs are verified by SHA-256 recorded in the build report; release signing strategy is a V1.0 topic (Owner-owned keystore, never committed).
- Component versions are visible in About & Safety; dependency upgrades re-run the qualification gates.

## 8. Incident handling

Suspected compromise (unexplained states, unknown pairing, leaked material): disconnect bridge, enable privacy mute, export redacted diagnostics, revoke pairing Tower-side, reset local data, and re-pair after the Tower side is verified. Factory reset is the last resort and is treated as a full unpair.
