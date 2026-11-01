# INSTALL RUNBOOK — first debug build on the Xiaomi 11 Lite 5G NE (GRAPH 1)

Owner action document. The device is not available to the executor; everything here is
Owner-run, local, and reversible. No root, no bootloader actions, no ADB over any remote
channel (INV-13/INV-14).

## 1. What you are installing

| | |
|---|---|
| App | Hermes Voice Console (com.hermestower.voiceconsole), version 0.1.0-alpha (versionCode 1) |
| Built by | GitHub Actions run `41296592392` (workflow `android-debug-build`), private repo |
| Source commit | `505047e9e05351a74b2b400ccca6442c364118ff` (branch `foundation/v0.1-cloud-build`) |
| APK name | `app-debug.apk` (artifact `hermes-voice-console-debug-apk-505047e…`) |
| Size | 57,848,596 bytes (≈ 55.2 MB) |
| SHA-256 | `f7a767354e241961928f1b536ec71c23e005920f55b7e7a791b29cc126cd0be7` |
| Signature | ephemeral CI debug key (no production key exists — INV-04) |

V0.1 truth: this build has NO real microphone capture, NO real speech recognition, NO real
network calls, NO wake word. It renders the real 15-state console machine driven by the
internal deterministic simulator (text input), with mock local answers and mock Tower
exchanges, fault injection, privacy mute, reduced motion, burn-in mitigation and both
themes. That is exactly what it should do — nothing in it pretends otherwise.

## 2. Get the APK onto the phone

Path A (phone only): open `github.com/Paolo83Ita/hermes-voice-console-android/actions/runs/41296592392`
in the phone browser, sign in, download the artifact `hermes-voice-console-debug-apk-…`,
unzip it (the download is a ZIP containing `app-debug.apk`).

Path B (PC + USB): download the same artifact on a PC, verify the SHA-256
(`sha256sum app-debug.apk`), copy `app-debug.apk` to the phone over USB (MTP), or install
directly with `adb install app-debug.apk` from the Owner's PC (USB debugging is already
enabled). Never expose ADB over Tailscale or any network (INV-14).

## 3. Verify integrity

- On PC: `sha256sum app-debug.apk` must equal the SHA-256 above.
- On the phone (Termux): `sha256sum ~/storage/downloads/app-debug.apk` — same check.

## 4. Install

1. Open the APK from Files (or let `adb install` do it).
2. If prompted, allow "install unknown apps" for that file manager only.
3. If a previous alpha was installed from a different key, uninstall it first
   (debug keys are ephemeral; signature mismatch is expected between runners).
4. Launch "Hermes Voice Console".

## 5. First-launch qualification walk (≈ 10 minutes)

Record results in `docs/qualification/session-YYYY-MM-DD.md` (see
DEVICE_QUALIFICATION_CHECKLIST.md §7):

1. Obsidian Night is the default theme (per Stage A decision).
2. Central animated Tower shows DORMANT → IDLE transitions truthfully.
3. Navigation drawer opens; all 8 screens open: Console, History, Connection, Voice and
   Audio, Local Intelligence, Appearance, Diagnostics, About and Safety.
4. Submit a text request in HYBRID: state walk IDLE → CONTACTING_TOWER → TOWER_PROCESSING →
   SPEAKING → IDLE with captions; the receipt reference appears in History (mock bridge).
5. Switch mode to LOCAL and submit: local whitelist answers, no Tower contact.
6. Diagnostics → inject DISCONNECT fault, submit a non-consequential request: REQUEST_QUEUED
   appears; clear fault, submit again: queue flushes.
7. Inject an ERROR: ERROR state, non-consequential retry affordance, consequential decline.
8. Privacy mute: state → PRIVACY_MUTED; wake/requests ignored; unmute returns to IDLE.
9. Appearance: switch to Marble Light and back; reduce animation; enable reduced motion.
10. Diagnostics screen: Manufacturer / Model / Android / SDK / memory are REAL device values
    — copy them into the session file (this is the device read-back the Graph needs).
11. SPEAKING interrupt: submit another request while SPEAKING — the console must return to
    rest (barge-in to IDLE) without lying about state.

## 6. Report back (fills `QUALIFICATION_PENDING_DEVICE`)

- The Diagnostics read-back line (model, Android, SDK, RAM).
- PASS/FAIL/PARTIAL per the 11 steps above, with notes.
- Any visual issue at 2400×1080 / 90 Hz (text contrast, animation smoothness).
- Thermal/charging behaviour during 15 minutes screen-on if convenient.
