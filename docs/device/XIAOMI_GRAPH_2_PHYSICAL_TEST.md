# XIAOMI GRAPH 2 PHYSICAL TEST — package for the Owner (QUALIFICATION_PENDING_DEVICE)

Device: Xiaomi 11 Lite 5G NE · model 2109119DG · Android 14 · HyperOS 2.0.4.0 UKOEUXM ·
8 GB RAM · 128 GB storage. Everything in this file is pending until physically executed
on that device; emulator results never substitute for it.

## 0. Preconditions (no root, no bootloader unlock, no custom recovery)

1. Obtain the V0.2 debug APK (name, size and SHA-256 from
   `docs/receipts/GRAPH_2_DEBUG_APK_RECEIPT.md`).
2. `shasum -a 256 hermes-voice-console-debug-v0.2-<commit>.apk` must match the receipt.
3. If V0.1 is installed: install over it (upgrade path exercises the migration); also do
   one fresh-install pass on a clean profile to exercise E2E-01..03 semantics.
4. USB debugging stays under your control; no Tailscale-exposed ADB.

## 1. Installation and migration

- [ ] Install over V0.1 → first launch shows V0.2 with prior theme/motion/burn-in intact
- [ ] Settings schema migrated (Diagnostics → Self-Test Quick: ST-45 PASS)
- [ ] Fresh-install pass → EMPTY configuration state, mock profile available

## 2. Tower Connection Studio

- [ ] Open from drawer, from Connection, from Diagnostics
- [ ] Reset to Mock mode → Test connection → CONNECTION_PASSED
- [ ] Enter a real address (https://… or Tailscale 100.x/…ts.net) with pairing code →
      Save draft → Validate → Test connection → record typed outcome
      (expected now: HOST_UNREACHABLE/TIMEOUT until a real Tower exists — that is honest)
- [ ] Clear sensitive values → metadata remains; vault empty (ST-03 PASS)

## 3. Self-Test on device

- [ ] Quick: all PASS, under 30 s
- [ ] Full: all PASS or explicitly QUALIFICATION_PENDING_DEVICE rows only; record durations

## 4. Voice Model Manager

- [ ] Recommended setup shows real device facts (model, RAM, storage, TTS engines)
- [ ] Apply recommended configuration (no download starts silently)
- [ ] Download probe artifact → pause/resume → verify → install → activate → read-back →
      rollback → remove (ST-32..40 equivalent on device)
- [ ] Real-model candidates remain non-downloadable (typed reasons shown)

## 5. Device read-back template (fill during the session)

```
date/time:            ____ (UTC+8)
app version:          0.2.0-debug (verify in About & Safety)
RAM available:        ____ MB    storage free: ____ GB
battery temperature:  ____ °C    thermal status: ____
network type:         ____       metered: ____
TTS engines found:    ____
screen-on behavior:   keep-screen-on honored: y/n
soak (4 h):           alive y/n · restarts ____ · thermal events ____
```

## 6. Microphone and speaker (declared but not requested at runtime in V0.2)

- [ ] Privacy mute toggles; listening cannot start while muted (E2E-39 on device)
- [ ] No RECORD_AUDIO runtime prompt appears until a voice engine is qualified
- [ ] Android TTS discovery lists system engines (Model Manager card)

## 7. Storage, RAM, temperature, screen-on behavior

- [ ] Storage read-back before/after probe install (delta ≈ 4 KB)
- [ ] RAM read-back idle vs during FULL self-test
- [ ] Screen-on behavior with keep-screen-on on/off
- [ ] Four-hour soak: console alive, no thermal status ≥ 4 sustained, drift active

Report results back as a filled copy of this file plus screenshots; they become the
typed evidence (DEVICE_READBACK_VERIFIED / DEVICE_FUNCTION_TEST_PASSED) for the
physical qualification. No device identifiers (IP/MAC/IMEI/serial) go into the
repository — use the template fields above only.
