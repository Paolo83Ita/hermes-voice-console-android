# UI STATE MACHINE — HERMES VOICE CONSOLE ANDROID

| | |
|---|---|
| Owner | Paolo Lombisani |
| Version | v0.1.0-alpha |
| Date | 2026-10-30 |
| Status | For Owner review |
| Implementation | `app/src/main/java/com/hermestower/voiceconsole/core/state/` (`ConsoleState.kt`, `ConsoleStateMachine.kt`) — table below is the authoritative copy |

## 1. Principle

The central animated Tower is an **instrument**: it renders only real states of the console. There are 15 states. Each has: a distinct visual profile, an accessibility announcement, and a legal-transition set enforced by `ConsoleStateMachine.ensure()`. Illegal transitions throw `IllegalConsoleTransitionException` — the UI can never drift into a lie.

## 2. State catalog

| State | Meaning | Visual profile (Tower core) | Accessibility announcement |
|---|---|---|---|
| DORMANT | Mode OFF; voice processing disabled | Stone gray, beacon off, meander faint; screen suggests rest | "Console dormant. Voice processing disabled." |
| IDLE | Awake, waiting | Slow beacon pulse (~4 s), faint energy updraft | "Idle. Listening for wake word." |
| WAKE_DETECTED | Wake word just recognized | Single bright flash, one ring, ~600 ms | "Wake word detected." |
| LISTENING | Capturing the utterance | Expanding rings from the base (~1.6 s cadence), beacon bright | "Listening." |
| TRANSCRIBING | Speech-to-text finishing | Controlled vertical light sweep along the shaft | "Transcribing." |
| UNDERSTANDING_LOCAL | Local processing / rule routing | Soft internal swirl, gold-tinted | "Thinking locally." |
| CONTACTING_TOWER | Request leaving for the Tower | Outward/upward data stream from beacon | "Contacting Hermes Tower." |
| TOWER_PROCESSING | Tower working | Energy bands circulating around the shaft | "Hermes Tower is processing." |
| SPEAKING | Response being spoken | Beacon + shaft illumination oscillating with voice amplitude (non-lip, non-mouth) | "Speaking." |
| REQUEST_QUEUED | Non-consequential request held for delivery | Slow amber pulse, queued-count chip | "Request queued until the Tower is reachable." |
| DISCONNECTED | Tower/network unavailable | Desaturated stone, beacon off, broken-link line, mode shows DISCONNECTED_SAFE | "Disconnected. Safe local functions only." |
| DEGRADED | Partial connectivity/quality | Energy at reduced intensity, intermittent link flicker | "Connection degraded." |
| ERROR | Recoverable failure | Amber-red pulse on the capital, 3 blinks then steady | "Error. Details in diagnostics." |
| PRIVACY_MUTED | Microphone disabled by Owner | Console dims, static mic-slash emblem at base ring, no motion | "Privacy muted. Microphone disabled." |
| UPDATE_AVAILABLE | New console build offered | Gentle gold halo around beacon over IDLE visuals | "Update available." |

## 3. Legal transition table (authoritative)

```
DORMANT            → IDLE
IDLE               → WAKE_DETECTED, TRANSCRIBING, LISTENING, CONTACTING_TOWER, PRIVACY_MUTED, DISCONNECTED, DEGRADED, ERROR, UPDATE_AVAILABLE
WAKE_DETECTED      → LISTENING, IDLE, PRIVACY_MUTED, ERROR, DISCONNECTED
LISTENING          → TRANSCRIBING, IDLE, PRIVACY_MUTED, ERROR, DISCONNECTED
TRANSCRIBING       → UNDERSTANDING_LOCAL, CONTACTING_TOWER, IDLE, PRIVACY_MUTED, ERROR, DISCONNECTED
UNDERSTANDING_LOCAL→ SPEAKING, CONTACTING_TOWER, IDLE, PRIVACY_MUTED, ERROR
CONTACTING_TOWER   → TOWER_PROCESSING, REQUEST_QUEUED, SPEAKING, DISCONNECTED, DEGRADED, ERROR, PRIVACY_MUTED
TOWER_PROCESSING   → SPEAKING, REQUEST_QUEUED, IDLE, DISCONNECTED, ERROR, PRIVACY_MUTED
SPEAKING           → IDLE, LISTENING, PRIVACY_MUTED, ERROR, DISCONNECTED
REQUEST_QUEUED     → CONTACTING_TOWER, IDLE, DISCONNECTED, ERROR, PRIVACY_MUTED
DISCONNECTED       → IDLE, DEGRADED, CONTACTING_TOWER, REQUEST_QUEUED, PRIVACY_MUTED
DEGRADED           → IDLE, DISCONNECTED, CONTACTING_TOWER, ERROR, PRIVACY_MUTED
ERROR              → IDLE, DISCONNECTED, PRIVACY_MUTED
PRIVACY_MUTED      → IDLE
UPDATE_AVAILABLE   → IDLE
```

Four universal rules complete the table (implemented in code):

1. **Any state → DORMANT** (power off is always legal).
2. **Any state except DORMANT → PRIVACY_MUTED** (the Owner can always silence the console).
3. **Any state except DORMANT → IDLE** (Owner override / barge-in / recovery to rest — e.g. a new submission while SPEAKING or CONTACTING_TOWER returns the console to rest before re-entering the pipeline).
4. **Any state → itself** (idempotent re-entry: power off while DORMANT, muting while already PRIVACY_MUTED, re-entering IDLE — all legal no-ops). IDLE → CONTACTING_TOWER exists for text-injected requests and queue flushes that start from rest, with no acoustic front-end phase.

Everything not listed is illegal and throws. Unit tests assert: every state appears, all driver paths used in the simulation are legal, illegal pairs are rejected.

## 4. State × operating mode interplay

| Mode | Typical states | Forced behavior |
|---|---|---|
| OFF | DORMANT | Pipeline cannot leave DORMANT |
| LOCAL | IDLE ↔ … ↔ UNDERSTANDING_LOCAL ↔ SPEAKING | No CONTACTING_TOWER reachable (router declines first) |
| TOWER | CONTACTING_TOWER / TOWER_PROCESSING / SPEAKING | Router always routes; offline → REQUEST_QUEUED or DECLINE |
| HYBRID | Any of the conversational set | Router decides per turn |
| DISCONNECTED_SAFE | DISCONNECTED / REQUEST_QUEUED + local answers | Entered automatically when bridge is down; requested mode restored on reconnect |

## 5. Motion specification

| Parameter | Value | Reduced-motion substitute |
|---|---|---|
| IDLE beacon pulse | 4000 ms ease-in-out loop | Static mid-intensity glow |
| LISTENING rings | 1600 ms, 3 phase-offset rings | Two static concentric rings, 40 % alpha |
| TRANSCRIBING sweep | 1200 ms vertical loop | Static bright band at 60 % shaft height |
| CONTACTING_TOWER stream | 1200 ms particle loop, 8 particles | Static 4-particle column, 50 % alpha |
| TOWER_PROCESSING orbit | 2000 ms arc rotation ×2 | Static opposing arcs |
| SPEAKING illumination | driven by amplitude flow, 120 ms smoothing | Static medium glow while SPEAKING |
| REQUEST_QUEUED pulse | 2400 ms amber | Static amber ring |
| ERROR blink | 3 × 350 ms | Steady amber capital |
| Burn-in guard drift | ≤ 8 dp translation, 60 s cadence, bounded box | No drift (guard disabled with reduced motion) |

## 6. Truth rules

- No fabricated telemetry is ever displayed in any state (no CPU meters, fake model activity, invented latencies).
- DISCONNECTED / DEGRADED always co-display the effective mode DISCONNECTED_SAFE and the requested mode if different.
- REQUEST_QUEUED shows the real queue count from the store.
- PRIVACY_MUTED is unmistakable: dimmed screen, mic-slash emblem, state chip, and TalkBack announcement all agree.
