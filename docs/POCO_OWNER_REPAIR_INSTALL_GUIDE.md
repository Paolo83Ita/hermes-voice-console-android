# POCO OWNER REPAIR INSTALL GUIDE (POCO X6 Pro)

- Package: **POCO OWNER REPAIR 1** — versionName `0.3.1-alpha-graph4-owner-repair`, versionCode **4**
- Owner: Paolo Lombisani · Device: POCO X6 Pro (HyperOS)
- This guide replaces the retest procedure of `INSTALL_POCO_X6_PRO.md` for the
  repair build and answers exactly the six mandated questions (Owner brief §2).

## 1. How to confirm the installed version (no ADB)

1. Open **Hermes Voice Console** → drawer → **Impostazioni** → **Informazioni
   e sicurezza** (About and Safety).
2. The **Build information** card shows the full identity:
   - build label — must read **POCO OWNER REPAIR 1**;
   - version — `0.3.1-alpha-graph4-owner-repair (4)`;
   - Git SHA — the full commit hash of the source this APK was built from;
   - build timestamp, workflow run ID (CI build) or "(local build)";
   - APK variant (UNIVERSAL / ARM64_V8A) and the ABIs found in the installed APK;
   - **APK SHA-256 (measured on device)** — the hash of the APK actually
     installed on the phone, computed from the real file;
   - installed-at / last-updated timestamps.
3. Cross-check: the **Diagnostics** screen shows the compact one-line footer
   `v0.3.1-… (4) · <sha> · <variant> · POCO OWNER REPAIR 1` at the bottom.
4. Anything showing `0.3.0-alpha-graph4`, versionCode 3, or no build-identity
   card at all = the OLD rejected package. Do not retest on it.

## 2. How to confirm the Git SHA

The Git SHA in **Build information** is injected at build time from the
commit the APK was built from. Match it against:
- the commit pinned in `POCO_X6_PRO_OWNER_RETEST_PACKAGE_2` (the retest
  package receipt), or
- the GitHub Actions run page for the branch
  `graph-4/real-voice-pairing-archive-e2e`.
A mismatch = wrong APK installed.

## 3. How to distinguish ARM64 from universal

- In-app: **Build information → APK variant**. `ARM64_V8A` = the per-ABI
  package (preferred for the POCO); `UNIVERSAL` = the all-ABI package.
- By file name: `app-arm64-v8a-debug.apk` (ARM64) vs
  `app-universal-debug.apk` (universal). Both install on the POCO; the ARM64
  package is smaller and is the preferred Owner artifact.
- The SHA-256 in the retest package receipt identifies the exact file.

## 4. Android signature mismatch ("App not installed" / update blocked)

All debug packages of this project are signed with the same debug keystore,
so an upgrade install over `0.3.0-alpha-graph4` normally succeeds. If Android
refuses ("App not installed" or a signature error):

1. Check in **Build information** of the currently installed app WHICH
   package you have (if the app still opens).
2. A signature mismatch can only come from a package signed by a different
   keystore — in that case the previous app must be UNINSTALLED first
   (see §5), then install the repair APK.
3. HyperOS may also require **Impostazioni → Protezione → Installazione
   app da USB/origini sconosciute** enabled for the file manager/browser.

## 5. When uninstalling the previous app is necessary

Uninstall ONLY if:
- the signature-mismatch case of §4 occurred, or
- the installer reports a downgrade/corrupt-package error.

**Uninstalling removes local application data**: chat drafts, console
settings (including the speech profile), Tower connection profiles, downloaded
model files and the event history. There is no cloud backup — uninstalling is
a deliberate data-loss decision. A normal upgrade (install over the old
version) preserves all of it.

## 6. Install / upgrade steps (HyperOS, no ADB)

1. Copy the APK to the POCO (USB/MBIM/cloud link).
2. Open it from **File Manager** → confirm the unknown-sources prompt.
3. For an upgrade: the installer shows "Aggiornamento app" — proceed
   (data preserved). For a fresh install: open the app after install.
4. Launch → **Build information** → confirm the identity (§1).

## 7. The short, conclusive retest flow (Owner brief §13)

| STEP | Action | Expected |
| --- | --- | --- |
| 1 | About & Safety → Build information | version `0.3.1-alpha-graph4-owner-repair (4)`, label POCO OWNER REPAIR 1, Git SHA matches the package receipt |
| 2 | Impostazioni → Lingua → **Italiano** | UI in Italian |
| 3 | Voce e audio → Android TTS diagnostics | record engine, voice, voice locale, Italian availability, pitch, speed; run **Autotest vocale (ST-51)** in Centro di test |
| 4 | Play the preview: "Benvenuto nella console vocale di Hermes Tower." | Italian voice, comfortable pitch, understandable pronunciation — OR the honest fail-closed card (no Italian voice on the device) + the open-settings actions |
| 5 | Modelli e voce → **Verifica** | text fully readable, no clipping, button usable |
| 6 | Console → the medallions | mic activation (Mic/Parla), listening (Stop/Interrompi ascolto), privacy mute (MicOff + red seal + "Microfono disattivato"), stop voice (VolumeUp/Interrompi voce) — all visually and semantically DISTINCT |
| 7 | About & Safety → **Esporta ricevuta diagnostica** → paste it into the retest report | proves the exact build + the TTS call path |

## 8. Known limitations (honest)

- If the POCO has no Italian offline voice data, the app does NOT invent a
  voice: it refuses to speak (fail-closed), shows the typed reason
  (`ITALIAN_SYSTEM_TTS_DATA_MISSING`), keeps the answer as text and offers
  the direct path to install the Italian voice data. Installing the voice
  data is a system-settings action outside the app.
- The local Piper voice (natural IT voice) remains
  INSTALLED-BUT-INACTIVE (sherpa-onnx runtime gap) — the system TTS route is
  the speaking route.
- The build-identity run ID reads "(local build)" for locally-built APKs;
  CI-built artifacts carry the workflow run ID.
