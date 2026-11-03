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

---

## GRAPH 4 ADDENDUM (2026-11-03) — qualification build from `graph-4/real-voice-pairing-archive-e2e`

Everything in §1–§6 above still applies (integrity check, install path, reversibility).
This addendum covers what is NEW since that receipt and what to verify ON THE DEVICE during
the physical Xiaomi qualification run. Always build from the LATEST SUCCESSFUL
`android-debug-build` run on the branch above (the Actions list shows the commit SHA next
to every run — read it back against `git ls-remote` before installing).

### A. What is new in this build (all measured, nothing simulated beyond the labels below)

- **Bundled verified models (Owner §6)**: the four physically verified ONNX artifacts
  (openWakeWord melspec/embed/hey_jarvis + GTCRN denoiser, SHA-256 published in
  `docs/models/VOICE_ARTIFACT_PHYSICAL_AUDIT.md`) ride inside the APK and install through
  the same hash-gated, receipted pipeline as downloads. Provenance shows `app-bundle`.
- **QR pairing scanner (Owner delta §11)**: Tower Studio → Actions → "Scan pairing QR".
  Camera permission is requested only in that screen; frames are decoded on-device and
  never stored; every rejected code shows a typed reason (e.g. EXPIRED, REPLAYED_NONCE).
- **Self-Test Center ST-46..ST-50**: bundled double-gate agreement, bundled delivery
  end-to-end (§6), QR strict-matrix probe, Archive truth (§15/§16/§17), route registry.
- **Status-bar fix (Owner field report)**: content clears the system clock/signal area
  (edge-to-edge with safe-drawing insets; the obsidian background stays full-bleed).
- **Tower Archive (Owner §14–§17)**: Archive Chamber with Conversations/Missions/Artifacts;
  every row is labeled SIMULATED — the real Tower stays the authority (Graph 1.7).

### B. On-device verification sequence (suggested order)

1. **Install + first launch** — app renders, no overlap with the system status bar.
2. **Model Manager → "Install bundled verified models"** — expect four `INSTALLED` lines
   with receipts (id `bundled-…`). Press it again: expect four `already installed, bytes
   re-verified` lines (idempotence is a contract, not an accident).
3. **Self-Test → Quick** — expect 22 pass, 0 fail (ST-46 included: it reads the bundled
   manifest from the APK). **Self-Test → Standard** — additionally ST-47 (bundled delivery
   end-to-end) and ST-49 (archive truth) must PASS on the device.
4. **Tower Studio → Scan pairing QR** — deny the permission once: expect the honest
   "manual pairing still works" state, no crash. Grant it, scan a `hermes-pair://v1` QR:
   the payload summary shows tower/env/scopes only (endpoint and challenge are never
   displayed). Pairing result is labeled **SIMULATED** — that is the truth until Tower
   Graph 1.7.
5. **Wake word** — stays OFF. The FA/FR harness on this device is the gate (Owner §8/§9):
   record false-accept and false-reject counts before enabling anything always-on.
6. **Denoiser** — remains bypassed until the device qualification numbers exist. Memory
   and latency stay `TARGET_PENDING_DEVICE_QUALIFICATION` until measured here.
7. **Language** — switch it/en/es/fr/de; nav + console + appearance strings follow.
8. **Archive** — open, paginate, pin a conversation offline, clear cache: the TOWER rows
   survive (local-cache removal never deletes remote authority — §15).

### C. What to bring back from the run

- Measured memory + latency for the denoiser and wake chain (replaces the
  `TARGET_PENDING_DEVICE_QUALIFICATION` markers).
- FA/FR harness results for `hey_jarvis` (the custom Italian wake word remains
  `CANDIDATE_PENDING_DISCOVERY` — no claim).
- Any typed failure verbatim (the app never silently falls back; every refusal is text).
