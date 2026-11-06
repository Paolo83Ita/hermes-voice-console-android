# INSTALL / UPGRADE GUIDE — POCO X6 PRO (HyperOS) — Hermes Voice Console Android

Package this guide describes: **`0.3.0-alpha-graph4`** (versionCode 3), universal +
arm64-v8a debug APKs, built from branch `graph-4/real-voice-pairing-archive-e2e`
(the Owner-feedback package). Guide date: 2026-11-06.

The previously tested package was `0.2.0-debug` (versionCode 2, commit `ca8980b`,
Graph 3 terminal). The new version name/number differ ON PURPOSE so the installed
build is identifiable at a glance in Android Settings → Apps.

---

## 1. Get the new APKs

Download from the cloud build of the owner-feedback commit (workflow
`android-debug-build` → run page → artifacts):

| Artifact | Content | Install on POCO X6 PRO? |
| --- | --- | --- |
| `debug-apk-universal` (`app-universal-debug.apk`) | every ABI | YES — the safe choice |
| `debug-apk-arm64-v8a` (`app-arm64-v8a-debug.apk`) | arm64 only, smaller | YES — the POCO X6 PRO (Dimensity 8300-Ultra) is arm64-v8a |
| `debug-apk-x86_64` | x86_64 (emulator) | no |

Both device APKs work; the arm64 one is ~30–40 MB smaller. If in doubt, install the
universal one.

Verify what you downloaded before installing (optional, the hash is printed in the
build log): the file must be named `app-universal-debug.apk` or
`app-arm64-v8a-debug.apk` and be ≈165 MB / ≈110 MB respectively.

## 2. First-time install (no adb) — Files app path

1. Copy the APK to the phone (USB file transfer, or download it in the browser).
2. Open **Files** (or the browser's Downloads) and tap the APK.
3. HyperOS will warn: *"For your safety, installation of apps from unknown sources
   is not allowed"* → tap **Settings** in the dialog → enable **Allow from this
   source** for the app you installed from → back → **Install**.
4. If prompted *"Blocked by Play Protect"* → tap **More details** → **Install
   anyway**. (This is the debug-signed internal build; it is not a Play package.)
5. Wait for **App installed** → Open.

## 3. Upgrade from `0.2.0-debug` (the package you already have)

The new APK is signed with the SAME debug keystore as the tested build, so it
installs directly OVER the old one, keeping the app's data:

- With the Files app: tap the new APK → **Install** → *"Update (keep data)"* when
  asked. No uninstall needed.
- If you ever see *"App not installed"* / signature error (only possible if a
  DIFFERENT build was installed at some point): uninstall the old app first, then
  install the new APK (data loss: settings and history only; voice models are
  re-delivered from the bundled assets automatically).

## 4. Upgrade with adb (optional, if USB debugging is enabled)

On HyperOS (POCO X6 PRO ships HyperOS):

1. **Settings → About phone → OS version** — tap it repeatedly until
   *"You are now a developer"*.
2. **Settings → Additional settings → Developer options** → enable **USB debugging**
   (and **USB debugging (Security settings)** if present).
3. Connect to a computer, accept the fingerprint dialog, then:

```bash
adb install -r app-universal-debug.apk      # -r keeps the app data
# or, device-specific and smaller:
adb install -r app-arm64-v8a-debug.apk
```

4. Verify the installed build:

```bash
adb shell dumpsys package com.hermestower.voiceconsole | grep versionName
# expected: versionName=0.3.0-alpha-graph4
```

No adb? **Settings → Apps → Manage apps → Hermes Voice Console** shows the version
at the bottom of the App info page.

## 5. One-time Italian voice check (the Finding-3 repair needs it)

The new build SHOWS the truth about the TTS voice instead of guessing:

1. Open the app → **Impostazioni (Settings) → Voce e audio** → the **Voce italiana**
   card shows a typed status, the active voice and its language.
2. If the status is **not** `ITALIAN_VOICE_ACTIVE`:
   - tap **Apri le impostazioni di sintesi vocale di sistema** (the button in the card),
   - in the system screen pick **Google Text-to-speech** → gear icon → **Install
     voice data** → **Italiano (Italia)** → download (a few MB),
   - back to the app → the status re-negotiates at the next answer; run
     **Self-Test → ST-51** to confirm.
3. Also read **Settings → Diagnostics → Runtime attivo**: rows `Voce attiva:` and
   `Lingua della voce:` show the on-device read-back.

## 6. The second Owner validation cycle (what to test and report)

With `0.3.0-alpha-graph4` installed:

| # | Check | Where | Typed evidence to report |
| --- | --- | --- | --- |
| 1 | Install + launch | — | `DEVICE_INSTALLATION_PASSED` + `APPLICATION_LAUNCH_PASSED` (new package) |
| 2 | Version visible | Settings → Apps | versionName `0.3.0-alpha-graph4` |
| 3 | Italian voice status | Settings → Voice & Audio → Italian voice card | the typed status + active voice + voice language |
| 4 | Runtime read-back | Settings → Diagnostics → Active runtime | the `Voce attiva:` / `Lingua della voce:` rows |
| 5 | ST-51 | Self-Test Center → Quick (or Voice foundation only) | ST-51 result line |
| 6 | Spoken answer language | Console: ask anything | Italian audio = `ITALIAN_VOICE_ACTIVE`; otherwise the card's typed state + any screenshot |
| 7 | Submenu layout | the six Settings categories + Voice/Audio, Local Intelligence, History, Archive, Connection, Diagnostics, About | OK / still-wrong per screen (screenshots welcome — they complete the defect ledger) |

Screenshots can be referenced as `IMG_*.jpg` filenames in the reply; the layout
ledger (`OWNER_UI_LAYOUT_DEFECT_LEDGER.md`) will be completed from them.

## 7. Known honest limitations in this build

- The natural downloaded voices (Piper "Paola"/"Riccardo") stay
  `INSTALLED-BUT-INACTIVE`: the sherpa-onnx runtime is not integrated yet — the
  ACTIVE voice is the Android system TTS (typed, by design; see
  `docs/receipts/TTS_RUNTIME_RECEIPT.md`).
- Tower connectivity is mock/simulator only.
- Optional local LLM is default-off and stays typed-blocked without a verified runtime.

## 8. Uninstall / rollback

The old `0.2.0-debug` APK cannot install over the newer one (versionCode). To roll
back: uninstall the app, then install the old APK. Prefer reporting the issue — the
new build is the one the project treats as current.
