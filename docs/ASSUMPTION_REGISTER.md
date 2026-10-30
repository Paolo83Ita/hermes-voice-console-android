# ASSUMPTION REGISTER — HERMES VOICE CONSOLE ANDROID

| | |
|---|---|
| Owner | Paolo Lombisani |
| Version | v0.1.0-alpha |
| Date | 2026-10-30 |
| Review cadence | Every checkpoint; each entry is OPEN / CONFIRMED / REFUTED with evidence |

| ID | Assumption | Domain | Impact if wrong | Validation | Status |
|---|---|---|---|---|---|
| A-01 | SoC is Snapdragon 780G (SM7350) or verified variant | Device | Performance budgets (STT/LLM/animation) | Q-02 on device | OPEN |
| A-02 | Model in M2101K9G / 2109119DG family (5G vs 5G NE) | Device | Reference only | Q-01 | OPEN |
| A-03 | Post-reset Android/MIUI version unknowable today (likely Android 11→13 lineage) | Device | minSdk, foreground-service typing, battery APIs | Q-03/Q-04 | OPEN |
| A-04 | 6.55″ AMOLED 2400×1080 90 Hz | Device | Burn-in profile, animation budget | Q-05 | OPEN |
| A-05 | Battery ≈ 4250 mAh, always charging in enclosure | Device | Thermal budget | Q-08/Q-09 | OPEN |
| A-06 | 2 top mics + bottom mic; enclosure affects capture | Device | Wake-word quality | Q-07/Q-14 | OPEN |
| A-07 | MIUI battery management restricts background even plugged | Device/Android | Service survival strategy | Q-10…Q-12 | OPEN |
| A-08 | Stable Wi-Fi 6 path to Tower host | Environment | DISCONNECTED_SAFE frequency | Q-13 | OPEN |
| A-09 | Loudspeaker adequate for desk-distance Italian TTS | Device | Voice legibility | Q-14 | OPEN |
| A-10 | AMOLED burn-in is a first-class risk for always-on console | Device | FR-40…FR-42 priority | Q-05 + time | OPEN |
| A-11 | Enclosure passive, PLA/PETG | Enclosure | Thermal ceiling 42 °C | Q-09 | OPEN |
| A-12 | Italian primary spoken language; V0.1 UI English (Italian localization scheduled V1.0) | Product | STT/TTS weighting, i18n plan | Owner confirmation | OPEN |
| A-20 | Proposed Tower contract (hermes.console.v0.1) is an unapproved proposal | Tower | V0.4 transport work blocked until Tower spec attached | Owner/Tower team review | OPEN |
| A-21 | Tower transport will be WSS primary, HTTPS secondary | Tower | Adapter design | Contract review | OPEN |
| A-22 | Auth scheme: device token issued at pairing; shape undefined | Tower/Security | V0.5 pairing implementation | Contract review | OPEN |
| A-23 | Language tags follow BCP-47 (it-IT) | Tower | Contract conformance | Contract review | OPEN |
| A-24 | Receipt references are opaque strings resolvable Tower-side | Tower | History receipt display | Contract review | OPEN |
| A-30 | Gradle/AGP/Kotlin version set (AGP 8.7.3, Kotlin 2.0.21, Gradle 8.9, JDK 17 toolchain) opens in current Android Studio | Toolchain | Build friction | Owner opens project (Stage C instructions) | OPEN — sandbox lacks Android SDK, untested by design |
| A-31 | Wrapper JAR regeneration is acceptable (binary not committed) | Toolchain | First import shows wrapper notice | Documented in BUILDING.md | OPEN |
| A-32 | Owner workstation can run Android Studio + emulator or sideload via USB | Environment | V0.2 program start | Owner confirmation | OPEN |
| A-33 | Deterministic canned responses are acceptable UX for V0.1 review | Product | Review value only | Owner review of prototype | OPEN |

## Handling rules

1. Every code path that depends on an OPEN assumption is written to fail soft (disable feature, show truthful status) — never to guess.
2. REFUTED assumptions trigger a register annotation and re-open of the affected option document before code proceeds.
3. CONFIRMED assumptions cite the checklist item and the captured command output.
