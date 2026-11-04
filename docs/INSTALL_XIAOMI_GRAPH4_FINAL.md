# HERMES VOICE CONSOLE — XIAOMI QUALIFICATION RUNBOOK (Graph 4 FINAL)

Owner: Paolo Lombisani · Device: Xiaomi 11 Lite 5G NE (2109119DG) · Android 11+ (minSdk 30)
You need: the phone, a PC browser, the private GitHub repo, a USB cable (or the phone
browser alone). NO Android Studio is required anywhere in this runbook.

## 0. What you will qualify

The DEBUG build of Graph 4 FINAL: complete Italian UI, five languages, simplified
five-destination navigation, six-step onboarding, the Monumental Tower, real offline
Italian STT delivery (vosk), the real TTS delivery chain, the optional local-LLM path,
the Tower Gate QR pairing flow (SIMULATED until Tower Graph 1.7) and the Tower Archive
(SIMULATED data). The lifecycle residual seen on the emulator is dispositioned
PHYSICAL_XIAOMI_RECOVERY_REQUIRED — THIS device is the arbitrator.

## 1. Download the APK (private GitHub artifact)

1. Open https://github.com/Paolo83Ita/hermes-voice-console-android in your browser and
   sign in.
2. Actions → the latest green **android-debug-build** run → Artifacts →
   `hermes-voice-console-debug-apk-<commit>` → download the ZIP and unzip it
   (`app-debug.apk`; arm64-v8a build if an arm64 artifact is present, otherwise the
   universal one installs on the Xiaomi too).
3. Note the commit hash in the artifact name (e.g. `3eac29a…`).

## 2. Verify the SHA-256 (Owner-friendly)

On the phone (no PC needed): install a free file-manager with checksum support (e.g.
"File Manager +" → file → Properties → CRC/checksum, choose SHA-256) OR on the PC
(Windows PowerShell):
`Get-FileHash .\app-debug.apk -Algorithm SHA256`
Compare with the hash printed in the run's `APK_SIZE_AND_ABI_REPORT.md` and with the
hash recorded in the APK receipt in the repo (`docs/receipts/`). The two hashes must be
IDENTICAL. If they differ: STOP, do not install, report the mismatch.

## 3. Install

- UPGRADE installation (a previous debug build is installed): open the APK → "Update".
- FRESH installation: uninstall any previous build first (Settings → Apps → Hermes
  Voice Console → Uninstall), then open the APK → "Install" (allow "install from this
  source" when asked).
- On first launch MIUI may ask once for the microphone permission and, later, for the
  camera permission (QR) — see steps 5 and 9.

## 4. Onboarding (six steps, directive §6)

1. LANGUAGE — choose Italiano (or Follow system).
2. TOWER — choose the simulation choice (the QR choice defers to the scanner later).
3. VOICE PACK — the recommended pack summary appears; install it (real bundled
   delivery) or continue later.
4. LOCAL LLM — leave "Not now" (the default).
5. VOICE TEST — the real Self-Test voice-foundation probes run (microphone, TTS,
   recognizer); the counts are honest.
6. FINISH — the Console opens.
Record any step that refuses, stalls or shows English when Italian is expected.

## 5. Italian UI check (§4 gate on the physical device)

Walk every drawer destination in Italian: Console, Archivio Tower, Connetti a Hermes
Tower, Modelli e voce, Impostazioni — then Impostazioni → each category (Generale,
Voce, Connessione alla Torre, Modelli, Display, Diagnostica). EVERY ordinary label must
be Italian. Technical identities (HERMES, TOWER, MODEL SANCTUM, SHA-256, model names)
stay as-is by design. Report any ordinary English text you see, with a screenshot.

## 6. Tower animation (§7-§10)

Watch the Tower in each state: IDLE (quiet carved conduit), LISTENING (breathing
rings), TRANSCRIBING, THINKING LOCALLY, CONTACTING TOWER / TOWER PROCESSING (messenger
pulses flowing), SPEAKING, QUEUED (amber seal), DISCONNECTED, PRIVACY MUTED, ERROR.
Check: the Tower occupies roughly half-to-two-thirds of the portrait screen; the state
chip never hides the energy path; the gateway reads as a lit threshold. Open the
keyboard and verify the chat-focused layout (Tower shrinks, composer grows, no ring
clipping). Display quality profiles live in Impostazioni → Display (LOW/BALANCED/HIGH;
HIGH is for the thermal qualification — see step 13).

## 7. Compact drawer + text chat

Open the drawer (hamburger), confirm ONLY five destinations. Type a message in the
composer and send it; watch the honest attribution line (local answers are never
attributed to the Tower). Rotate the phone or background/foreground the app mid-conversation —
the draft must survive (persistent drafts).

## 8. Microphone, push-to-talk, privacy mute

Hold the push-to-talk control and speak Italian: partial transcript appears live, the
final transcript lands in the timeline. Toggle PRIVACY MUTED: the Tower goes to
PRIVACY MUTED, the microphone is PHYSICALLY released (no listening chip), and no
capture happens until you unmute. Check Settings → Apps → Hermes Voice Console →
Permissions while muted: microphone should not be in active use.

## 9. STT download and use (§14)

Modelli e voce → find `vosk-model-small-it-0.22` (Italian STT, ~49.7 MB) → Open source
(github.com/alphacep — must open in the browser), Open license, Download (watch real
progress), Verify (SHA-256 gate), Install. Then Diagnostica → Active runtime: the
"Offline model state" row must show the installed model. Run a push-to-talk turn and
record what the "Speech recognition in use" row says — local vosk when loaded, system
recognizer otherwise; both are honest. Use "Test" on the entry; record the Italian
transcript you obtain.

## 10. TTS voice download and use (§15)

Modelli e voce → the Italian Piper voices (riccardo-x-low ~26.5 MB, paola-medium
~67.2 MB): Open source / Open license / Download / Verify. The consuming runtime for
these files is pending integration (honest state) — the ACTIVE voice output is the
Android system TTS (Diagnostica → Active runtime shows the engine). Use the system
voice to speak a line, interrupt it mid-speech (barge-in), and record the behavior.
The downloaded voice files must verify with SHA-256 OK even while the runtime row says
"pending integration".

## 11. Optional local LLM (§16)

Leave the LLM UNINSTALLED (default "No local model"). Optionally: Modelli e voce →
Qwen2.5-0.5B candidate (~522 MB): Open source / Open license / Download / Pause /
Resume / Verify / Install — record free-storage before and after. The runtime row for
local intelligence must stay honest ("llama.cpp runtime pending"); do NOT expect local
model responses in this build.

## 12. QR camera, pairing mock, archive mock (§17-§18)

Connetti a Hermes Tower → scan the Tower Dashboard QR (grant the CAMERA permission).
The scanner is full-screen: Back returns to the studio. Pairing runs through the
DETERMINISTIC MOCK — every result is labeled SIMULATED; no production endpoint is
called. Tower Archive: Conversations / Missions / Artifacts with search, filters,
pagination; every dataset row is labeled SIMULATED DATA.

## 13. Thermal and four-hour soak (§21)

- After 30 min of use, read Settings → About phone → temperature (or the Diagnostics
  battery-temperature row) and record it.
- FOUR-HOUR SOAK: leave the app open on the Console (screen ON, brightness ~50%),
  run one voice turn every ~20 minutes, keep the drawer closed. Record: battery %,
  temperature, any ANR dialog, any visual glitch photo. HIGH quality profile: enable
  for the LAST hour of the soak only, and record temperature before/after.

## 14. Restart, process recreation, RAM

- Restart the phone, reopen the app: the language, the drafts and the installed models
  must survive.
- Developer options → "Limit background processes" is NOT needed; instead simply open
  3-4 heavy apps to force a process kill, then return to the console: state restores.

## 15. Failure evidence collection (§21)

If anything fails: (1) screenshot the screen; (2) Diagnostica → generate the REDACTED
report → copy the full text; (3) note the time and what you did. The app collects its
test evidence internally wherever Android permits (the redacted report + receipts).
Send the screenshots + the report text + the commit hash back to the mission.

## 16. Verdict vocabulary

- Everything above passes → GRAPH_4_XIAOMI_VOICE_AND_MODEL_DELIVERY_QUALIFIED
  (physical state).
- Emulator-only residual reproduced on the phone → attach evidence; the disposition
  changes.
- Any ordinary-English UI text in Italian mode → report with screenshot (gate defect).
