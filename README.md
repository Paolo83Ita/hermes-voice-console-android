# HERMES VOICE CONSOLE ANDROID

Dedicated, visually distinctive, always-available Android voice terminal for **Hermes Tower**.
Owner: **Paolo Lombisani** · Version **v0.1.0-alpha** (mission V0.1: foundation + interactive visual prototype)

> The device must no longer look or behave like a reused smartphone. It must feel like the physical
> voice manifestation of Hermes Tower.

## Status (honest)

| Item | State |
|---|---|
| Stage A documentation (18 docs) | ✅ complete (`docs/`) |
| Visual direction + previews | ✅ complete (`design/mockups/*.png`) |
| Stage B buildable Android prototype | ✅ source complete — 50 Kotlin files, 6 JVM test classes |
| Compiled on this sandbox | ❌ **not built here** — no Gradle/Kotlin compiler/Android SDK in this environment |
| Compiled on your machine | ⏳ follow `BUILDING.md` (expected: opens in current Android Studio, versions pinned in `gradle/libs.versions.toml`) |

No microphone capture, no credentials, no model downloads, no root operations, no real Tower
connectivity — exactly as the mission prescribes for V0.1. Everything simulated is labeled simulated.

## Quick orientation

```
docs/               STAGE A: vision, requirements, architecture, security, state machine, registers…
design/mockups/     MARBLE LIGHT + OBSIDIAN NIGHT annotated previews (HTML + PNG)
app/                STAGE B: Kotlin + Jetpack Compose prototype (single module, package-modular)
BUILDING.md         exact reproducible build & test instructions
FINAL_REPORT.md     mission report: summary, inventory, decisions, blockers, next actions
```

## What the prototype does when it runs

- Central **animated Tower** rendering only the 15 truthful states (DORMANT → UPDATE_AVAILABLE),
  enforced by a state machine with a legal-transition table (`core/state/`).
- Deterministic **IntentRouter**: greetings/help/status/time resolve locally; consequential requests
  route to the Tower and *never* queue silently; offline queues non-consequential turns (DISCONNECTED_SAFE).
- **MockTowerBridge** emitting the proposed `hermes.console.v0.1` contract (ACK → PROGRESS → DELTA* →
  FINAL with receipt ref) with injectable faults: disconnect, tower error, slow tower.
- All eight UI surfaces: Console, History, Connection, Voice & Audio, Local Intelligence, Appearance,
  Diagnostics (real device facts only), About & Safety.
- **Marble Light / Obsidian Night** themes (Obsidian default, D-009), reduced-motion rendering,
  animation intensity, burn-in drift guard, captions, privacy mute that truly gates the pipeline.
- Text injection stands in for the microphone; wake/listen/STT are simulated and labeled as such.

## Hard rules honored by this codebase

No secrets in source · no root · no fake runtime data · no technology claimed qualified without a
physical device test · mock and real transports strictly separated · Tower stays the only authority.
