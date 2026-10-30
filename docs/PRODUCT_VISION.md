# PRODUCT VISION — HERMES VOICE CONSOLE ANDROID

| | |
|---|---|
| Owner | Paolo Lombisani |
| Version | v0.1.0-alpha (Stage A, mission V0.1) |
| Date | 2026-10-30 |
| Status | For Owner review |
| Related | REQUIREMENTS.md, ARCHITECTURE.md, UI_STATE_MACHINE.md, VISUAL_DIRECTION.md |

## 1. One-line definition

A dedicated, visually distinctive, always-available Android **voice terminal for Hermes Tower**: the device stops being a reused smartphone and becomes the physical voice manifestation of the Tower on the Owner's desk.

## 2. Why this product exists

Hermes Tower is the authoritative intelligence of its estate. A general-purpose phone is a poor front door for it: the screen fights for attention, Android treats the voice assistant as one app among many, and the identity of the Tower disappears behind a generic launcher. The Hermes Voice Console inverts this. One Xiaomi 11 Lite 5G, permanently powered, stationary in a custom 3D-printed desk enclosure, portrait-first, becomes a single-purpose instrument: it listens for its Owner, it speaks with a calm and authoritative voice, and it shows the truth of what it is doing at every instant.

The console is an **edge assistant**, not a second brain. It performs everything that must be fast, private, or available offline. It defers everything that must be authoritative — reasoning, mission planning, execution, evidence — to Hermes Tower.

## 3. Identity and feel

The visual language is **techno-Greek**: the discipline and geometry of classical architecture rendered in modern, premium materials.

- Surfaces: refined **white marble** (Marble Light) or **dark obsidian** (Obsidian Night).
- Details: restrained **bronze and gold** — never neon chrome.
- Energy: **electric blue / cyan**, used only to express real activity.
- Motifs: geometric Greek elements (meander band, column fluting, pediment line) used sparingly — at most one signature motif per zone of the screen.
- Centerpiece: an **animated Hermes Tower core** whose motion is a truthful instrument of state, not decoration.

Explicit rejections: no generic cyberpunk dashboard, no cluttered science-fiction HUD, no imitation of copyrighted fictional interfaces, no fake runtime data (no fabricated CPU graphs, fake mission telemetry, or invented connection stats).

## 4. Product role split

**The console (edge side) may perform locally:** wake-word detection, voice activity detection, audio capture, feasible noise suppression, speech-to-text, text-to-speech, short conversational exchanges, command classification, intent normalization, local status queries, cached responses, basic device control, connection management, secure session management, and buffering during temporary network failure.

**Hermes Tower remains authoritative for:** complex reasoning, mission planning, task execution, capability selection, tool and model orchestration, persistent mission state, evidence and receipts, consequential actions, Owner approval, and final operational truth.

**Hard rule:** the local model must never become a second Tower authority. No consequential action is ever taken or implied as completed locally; nothing is "silently completed" if the relevant authority did not accept and complete it.

## 5. Operating modes

| Mode | Meaning | Voice processing | Tower contact |
|---|---|---|---|
| OFF | Dormant console | Disabled | None |
| LOCAL | Self-sufficient device functions | Active, local only | None |
| TOWER | Tower is the responder | Active, forwarding | Required |
| HYBRID | Default target mode | Active; qualified simple queries resolved locally, complex/operational/uncertain/consequential requests routed | As needed |
| DISCONNECTED_SAFE | Network or Tower unavailable | Active for safe local functions; optionally queues non-consequential requests | Attempted, not available |

Mode transitions are always visible in the UI. The effective mode may differ from the requested mode (for example a requested TOWER mode degraded to DISCONNECTED_SAFE); both are then displayed.

## 6. Voice personality

The intended voice is natural, calm, authoritative, warm but not theatrical, clear in Italian, interruptible at low latency, and consistent with the Hermes identity. V0.1 ships a functional TTS abstraction with Android system TTS as the honest first fallback; higher-quality local voices (Piper, Kokoro, or other candidates) are treated as candidates pending physical qualification on the target device. Speech playback always supports: stop speaking, interruption by the Owner, sentence-level streaming where possible, volume control, captions, microphone-state indication, and no secret speech captured into persistent logs by default.

## 7. The animated Tower as a truthful instrument

The central visual state machine renders only truthful states (DORMANT, IDLE, WAKE_DETECTED, LISTENING, TRANSCRIBING, UNDERSTANDING_LOCAL, CONTACTING_TOWER, TOWER_PROCESSING, SPEAKING, REQUEST_QUEUED, DISCONNECTED, DEGRADED, ERROR, PRIVACY_MUTED, UPDATE_AVAILABLE). Each state is distinct, accessible, and restrained. The full specification is UI_STATE_MACHINE.md.

## 8. Trust principles

1. **Truthful UI** — the screen never shows a state, capability, or datum that does not exist.
2. **Privacy by default** — audio retention off, transcripts configurable, logs redacted, an unmistakable privacy mute.
3. **Honest degradation** — disconnection is disclosed, never masked; queued requests are visibly queued, never pretended delivered.
4. **Qualification before claims** — no technology is declared working until it has been built and tested on the physical Xiaomi.
5. **Replaceable parts** — every engine (wake word, VAD, STT, TTS, local model, transport) sits behind an interface so a provider can be exchanged without rewriting the application.

## 9. Success criteria for V0.1

- Stage A documentation is internally coherent and matches the delivered implementation.
- A buildable Kotlin + Jetpack Compose prototype exists with the techno-Greek identity, the animated Tower, all eight UI surfaces, both themes, reduced-motion mode, and a truthful visual-state simulator driven by mock engines.
- No real microphone capture, no credentials, no model downloads, no root operations, no fabricated build success.
- The Owner can open the project in a current Android Studio environment and run it on the target device to judge the identity and interaction before any hardware capability is wired.

## 10. Out of scope for V0.1

Root, bootloader unlock, custom ROM or recovery; real Tower connectivity or credentials; real microphone capture; local model downloads or qualification claims; kiosk lockdown as a controlled capability; production pairing; enclosure electronics beyond passive mounting. These are scheduled in DEVELOPMENT_ROADMAP.md.
