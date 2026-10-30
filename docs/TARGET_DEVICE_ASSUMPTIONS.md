# TARGET DEVICE ASSUMPTIONS — Xiaomi 11 Lite 5G

| | |
|---|---|
| Owner | Paolo Lombisani |
| Version | v0.1.0-alpha |
| Date | 2026-10-30 |
| Status | For Owner review |

## 1. Purpose

Every fact about the physical device is either **confirmed** (seen on hardware) or **assumed** (from public specifications or Owner statements). This document is the single register of that split. Nothing in the codebase or architecture may rely on an assumption without the corresponding validation step in DEVICE_QUALIFICATION_CHECKLIST.md.

The device will be factory-reset before qualification; exact Android and MIUI versions are therefore **unknown today** and will be recorded after reset.

## 2. Confirmed by Owner (mission statement)

| # | Fact |
|---|---|
| C-01 | Model family: Xiaomi 11 Lite 5G |
| C-02 | 8 GB RAM, 128 GB storage |
| C-03 | Permanently connected to power |
| C-04 | Normally stationary in a custom 3D-printed desk enclosure |
| C-05 | Portrait orientation is primary; landscape must remain possible |
| C-06 | Device remains stock and non-rooted; bootloader unlock and root are out of scope for the first implementation |
| C-07 | No assumption may be made about root, custom recovery, custom ROM, unrestricted background execution, or privileged APIs |

## 3. Assumed (to verify on device — T-IDs map to DEVICE_QUALIFICATION_CHECKLIST.md)

| ID | Assumption | Basis | Impact if wrong | Verify by |
|---|---|---|---|---|
| A-01 | SoC is Snapdragon 780G (SM7350), 5 nm | Public specs for 11 Lite 5G and 5G NE; Owner asked for physical verification | On-device STT/LLM performance budgets change; qualification thresholds recalculated | Q-02 |
| A-02 | Exact model number in the M2101K9G / 2109119DG family (5G vs 5G NE unresolved) | Public specs; Owner stated "exact SoC variant to be physically verified" | None functionally; recorded for build/thermal reference | Q-01 |
| A-03 | Ships with Android 11 (MIUI 12) and may have been updated to Android 13 / MIUI 14 before reset; after reset the installed version is whatever Xiaomi's recovery installs | Public update history | minSdk choice, foreground-service type enforcement (API 34+), battery-tool APIs | Q-03, Q-04 |
| A-04 | 6.55″ AMOLED, 2400×1080, 90 Hz | Public specs | Burn-in profile and animation budget; 90 Hz may be power-configurable | Q-05 |
| A-05 | Battery ≈ 4250 mAh; device always charging in desk enclosure | Public specs + Owner statement | Thermal budget: charging + sustained voice pipeline is the worst case | Q-08, Q-09 |
| A-06 | 2 top microphones with beamforming; bottom mic present | Public specs | Wake-word capture quality in enclosure | Q-07 |
| A-07 | MIUI applies aggressive battery management to background apps even when plugged in | Known MIUI behavior family | Foreground service survival; requires MIUI-specific checklist steps (autostart, battery saver, lock-in-recents) | Q-10, Q-11, Q-12 |
| A-08 | Wi-Fi 6 + stable local network to the Tower host | Owner environment | DISCONNECTED_SAFE frequency | Q-13 |
| A-09 | Loudspeaker adequate for a desk-distance TTS voice at moderate volume | Public specs | Voice legibility; enclosure acoustics unknown | Q-14 |
| A-10 | Display burn-in is a real risk for a static AMOLED console over months | AMOLED physics | Burn-in mitigation is a first-class requirement, not an option | Q-05, R-02 |
| A-11 | Enclosure is passive (no fan), printed in PLA/PETG | Owner plan | Trapped heat raises sustained-load temperature; thermal monitoring mandatory | Q-09 |
| A-12 | Italian is the primary spoken language; UI language for V0.1 is English with Italian localization scheduled | Mission voice requirements | STT/TTS candidate selection weighting | Q-15 |

## 4. Rules for assumptions

1. No code path may assume a capability that A-01…A-12 have not yet confirmed (example: the code must not enable a 90 Hz assumption or a battery-optimization exemption silently).
2. Each verification produces a PASS / FAIL / PARTIAL record in DEVICE_QUALIFICATION_CHECKLIST.md with the exact command output captured into the qualification report.
3. If an assumption fails, ARCHITECTURE.md and the relevant option documents are re-opened before proceeding — a failed assumption never silently degrades into a workaround.
4. The device is never rooted, flashed, or otherwise modified to make an assumption true.

## 5. Device envelope used for V0.1 budgets (conservative)

| Budget | Value used | Rationale |
|---|---|---|
| Sustained free RAM for console | ≤ 1.6 GB | 8 GB total shared with MIUI + future on-device models |
| Concurrent audio stack (wake+VAD+STT) | ≤ 25 % of a big core | Mid-SoC conservative estimate pending A-01 |
| Sustained surface temperature in enclosure | ≤ 42 °C | Charging + AMOLED + passive enclosure |
| Voice round trip, local path (wake→first spoken audio) | ≤ 2.5 s target | Perceived responsiveness floor |
| Voice round trip, tower path | Tower-competitive; console adds ≤ 700 ms overhead target | Transport + streaming design |
