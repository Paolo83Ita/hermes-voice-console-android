# DEVICE QUALIFICATION CHECKLIST — Xiaomi 11 Lite 5G

| | |
|---|---|
| Owner | Paolo Lombisani |
| Version | v0.1.0-alpha |
| Date | 2026-10-30 |
| When | Immediately after factory reset, before any development build is installed |
| How | Record the exact command output for every item into `docs/qualification/` (one file per session). PASS/FAIL/PARTIAL + notes. |

## 1. Identity and system

| Q-ID | Item | Command / action | Expected (assumption) | Result |
|---|---|---|---|---|
| Q-01 | Exact model | `adb shell getprop ro.product.model` `ro.product.marketname` | M2101K9G-family / "Mi 11 Lite 5G" | ☐ |
| Q-02 | SoC | `adb shell getprop ro.board.platform` `ro.soc.model` `ro.hardware` | SM7350 / 780G family | ☐ |
| Q-03 | Android version | `adb shell getprop ro.build.version.release` `ro.build.version.sdk` | 11–13 lineage post-reset | ☐ |
| Q-04 | MIUI version | `adb shell getprop ro.miui.ui.version.name` | V12–V14 lineage | ☐ |
| Q-05 | Display | `adb shell dumpsys display` (modes, refresh rate); visual AMOLED check | 2400×1080, 90 Hz capable | ☐ |
| Q-06 | Codename | `adb shell getprop ro.product.device` | renoir-family (verify) | ☐ |

## 2. Audio

| Q-ID | Item | Command / action | Expected | Result |
|---|---|---|---|---|
| Q-07 | Microphones | `adb shell pm list features \| rg microphone`; `dumpsys audio` input devices | MIC present, array count from `dumpsys` | ☐ |
| Q-08 | Battery & power | `adb shell dumpsys battery` | Present, charging behavior noted | ☐ |
| Q-09 | Thermals | `adb shell dumpsys thermalservice`; 15 min charging + screen-on temperature curve | Within NFR-07 | ☐ |
| Q-14 | Loudspeaker Italian TTS audition | Play system TTS sample at desk distance (1 m) | Clear, adequate level | ☐ |

## 3. MIUI survival (background execution)

| Q-ID | Item | Action | Result |
|---|---|---|---|
| Q-10 | Autostart | Security app → Autostart: allow Hermes Console | ☐ |
| Q-11 | Battery saver | Settings → Battery: no restrictions for Hermes Console; "No restrictions" where offered | ☐ |
| Q-12 | Lock in recents | Lock console card in Recents; verify service survives 24 h plugged | ☐ |
| Q-13 | Network | Wi-Fi 6 link to Tower host LAN; ping/throughput sanity from device | ☐ |

## 4. Developer / build environment

| Q-ID | Item | Action | Result |
|---|---|---|---|
| Q-15 | Developer options + USB debugging | Enable; `adb devices` sees device | ☐ |
| Q-16 | Language/time | System language Italian, timezone Europe/Rome | ☐ |
| Q-17 | Display policy | Screen timeout set per plan; test `FLAG_KEEP_SCREEN_ON` override | ☐ |
| Q-18 | Storage | `df -h /data`; ≥ 60 GB free for models/build cache | ☐ |
| Q-19 | RAM baseline | `cat /proc/meminfo` + `dumpsys meminfo` idle snapshot (record for NFR-08 deltas) | ☐ |

## 5. Behavior probes (V0.2 program)

| Q-ID | Item | Method | Result |
|---|---|---|---|
| Q-20 | Foreground service survival | Install V0.2 service PoC; measure survival across 24 h plugged, screen off/on cycles | ☐ |
| Q-21 | Doze behavior | `dumpsys deviceidle` forced idle; verify service + network policy | ☐ |
| Q-22 | Boot recovery | Reboot device; verify console returns to IDLE/DISCONNECTED (FR-44) | ☐ |
| Q-23 | Permission dialogs | RECORD_AUDIO runtime grant/deny flow behaves; mute gates capture | ☐ |
| Q-24 | STT/TTS engines inventory | `adb shell pm list packages \| rg -i "speech|tts"`; note available engines/voices | ☐ |

## 6. Post-reset Owner checklist (non-technical)

| Q-ID | Item |
|---|---|
| Q-30 | Factory reset completed; OOBE finished (Wi-Fi configured; Google account per Owner choice) |
| Q-31 | MIUI updates applied **before** qualification baseline is recorded (record the final version) |
| Q-32 | Physical inspection: SoC/model label photo, enclosure fit check, vent plan |
| Q-33 | Device labeled `HERMES-CONSOLE-01`; hostname/SSID note added to Owner records |

## Evidence rules

- One file per qualification session: `docs/qualification/session-YYYY-MM-DD.md`, containing raw outputs.
- PASS requires the recorded output, not memory.
- Any FAIL opens/updates the relevant register (assumption → REFUTED, risk → active) before development continues.
