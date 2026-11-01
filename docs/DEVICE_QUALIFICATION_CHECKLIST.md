# DEVICE QUALIFICATION CHECKLIST — Xiaomi 11 Lite 5G

| | |
|---|---|
| Owner | Paolo Lombisani |
| Version | v0.1.0-alpha (cloud build 505047e9, run 41296592392) |
| Date | 2026-10-30 — GRAPH 1 refresh 2026-11-01 |
| When | Immediately after factory reset, before any development build is installed |
| How | Record the exact command output for every item into `docs/qualification/` (one file per session). PASS/FAIL/PARTIAL + notes. |

> GRAPH 1 note: the Owner has since confirmed the physical device identity
> (Xiaomi 11 Lite 5G NE · 2109119DG · HyperOS 2.0.4.0 UKOEUXM · Android 14 ·
> build UKQ1.240624.001 · patch 2025-06-01 · kernel 5.4.289-gki · 8 GB · 128 GB).
> Items Q-01..Q-04 keep their read-back commands to VERIFY the Owner-provided values on
> device; developer options + USB debugging are already enabled (Q-15 pre-passed).

## 1. Identity and system

| Q-ID | Item | Command / action | Expected (assumption) | Result |
|---|---|---|---|---|
| Q-01 | Exact model | `adb shell getprop ro.product.model` `ro.product.marketname` | Owner-confirmed: 2109119DG / "Xiaomi 11 Lite 5G NE" — read-back pending | ☐ |
| Q-02 | SoC | `adb shell getprop ro.board.platform` `ro.soc.model` `ro.hardware` | Expect SM7325 (Snapdragon 778G, NE variant) — VERIFY on device | ☐ |
| Q-03 | Android version | `adb shell getprop ro.build.version.release` `ro.build.version.sdk` | Owner-confirmed: 14 / SDK 34 (build UKQ1.240624.001) — read-back pending | ☐ |
| Q-04 | MIUI version | `adb shell getprop ro.miui.ui.version.name` `ro.mi.os.version.name` | Owner-confirmed: HyperOS 2.0.4.0 UKOEUXM — read-back pending | ☐ |
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
| Q-15 | Developer options + USB debugging | Enable; `adb devices` sees device | ☐ (pre-passed: enabled by Owner) |
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

## 7. V0.1 build installation and first launch (GRAPH 1 addition)

Runbook: `docs/INSTALL_XIAOMI.md`. APK identity: commit `505047e9…`, SHA-256
`f7a767354e241961928f1b536ec71c23e005920f55b7e7a791b29cc126cd0be7` (APK_RECEIPT_V0.1.md).

| Q-ID | Item | Method | Result |
|---|---|---|---|
| Q-40 | APK integrity | SHA-256 of the transferred file matches the receipt | ☐ |
| Q-41 | Install (unknown sources for the file manager only; no root) | Files → install | ☐ |
| Q-42 | Obsidian Night default theme on first launch | visual | ☐ |
| Q-43 | All 8 screens open; navigation drawer; no crash | walk | ☐ |
| Q-44 | HYBRID text request: truthful IDLE→CONTACTING_TOWER→TOWER_PROCESSING→SPEAKING→IDLE with captions and receipt in History | walk | ☐ |
| Q-45 | LOCAL mode: whitelist answers only; DISCONNECTED fault: queue + flush | walk | ☐ |
| Q-46 | Privacy mute gates input; unmute returns to IDLE; reduced-motion + theme switch | walk | ☐ |
| Q-47 | Diagnostics read-back: manufacturer/model/Android/SDK/RAM are real — copied to session file | copy | ☐ |
| Q-48 | Barge-in: new submission during SPEAKING returns to rest without false state | walk | ☐ |

## Evidence rules

- One file per qualification session: `docs/qualification/session-YYYY-MM-DD.md`, containing raw outputs.
- PASS requires the recorded output, not memory.
- Any FAIL opens/updates the relevant register (assumption → REFUTED, risk → active) before development continues.
