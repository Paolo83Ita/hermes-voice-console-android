# ARCHITECTURE — HERMES VOICE CONSOLE ANDROID

| | |
|---|---|
| Owner | Paolo Lombisani |
| Version | v0.1.0-alpha |
| Date | 2026-10-30 |
| Status | For Owner review |
| Diagrams | ARCHITECTURE_DIAGRAM.md (Mermaid component diagram), SEQUENCE_DIAGRAMS.md (LOCAL / TOWER / HYBRID / DISCONNECTED_SAFE) |

## 1. Architectural stance

A **modular monolith with explicit seams**. V0.1 is a single Gradle module (`:app`) because nothing has been qualified on the physical device yet; however every engine family is defined as an interface owned by the domain layer, so extracting Gradle modules later is a mechanical move, not a rewrite. Hard coupling between UI, speech engines, the local language model, and Tower transport is prohibited.

Layering (strictly one-directional):

```
UI (Jetpack Compose)
  └─► Application services (stores, drivers, graph)
        └─► Domain (interfaces + pure logic: state machine, routing, contract models)
              └─► Adapters (mock engines now; system/qualified engines later; transport adapters later)
```

- UI depends on application services only; it never touches an adapter directly.
- Domain has **zero Android imports** (pure Kotlin + java.time + kotlinx.serialization) → unit-testable on the JVM.
- Adapters implement domain interfaces; replacement is configuration, not code surgery.

## 2. Engine seams (the 14 contracts)

| Interface | V0.1 adapter | Planned real adapter |
|---|---|---|
| WakeWordEngine | Inactive stub (simulation drives wake) | openWakeWord / Porcupine — QUALIFY_ON_DEVICE |
| VoiceActivityDetector | Inactive stub | WebRTC VAD / Silero VAD — QUALIFY_ON_DEVICE |
| AudioCaptureEngine | Not present (no mic capture in V0.1) | AudioRecord-based, permission-safe — V0.2 |
| SpeechToTextEngine | Simulated transcript provider | whisper.cpp / system SpeechRecognizer — QUALIFY_ON_DEVICE |
| LocalConversationEngine | MockLocalConversationEngine (canned, deterministic) | llama.cpp-compatible runtime — QUALIFY_ON_DEVICE |
| IntentRouter | RuleBasedIntentRouter (deterministic, tested) | LLM-assisted router behind same interface |
| TowerBridge | MockTowerBridge + DeterministicTowerSimulator | OkHttp/WSS adapter — V0.4 |
| TextToSpeechEngine | MockTextToSpeechEngine (timing + amplitude synthesis) | Android system TTS fallback (V0.3), Piper/Kokoro candidate (V0.6) |
| AnimationStateController | Store-backed (reduced motion, intensity) | same |
| DeviceHealthMonitor | AndroidDeviceHealthMonitor (real Build/ActivityManager/BatteryManager/StatFs data) | same |
| SecureConfigurationStore | InMemorySecureConfigurationStore (holds no secrets in V0.1) | Keystore-backed — V0.5 |
| SessionController | DefaultSessionController (UUID sessions, persistent conversation id) | same + Tower-issued identity |
| LocalEventStore | InMemoryEventStore (ring buffer, powers redacted export) | Room-backed if justified — V0.3 |
| DiagnosticsExporter | RedactedDiagnosticsExporter (uses LogRedactor) | same |

Rule enforced by code review and by the quality gates: **a candidate is never claimed to work until it has been built and tested on the physical target.**

## 3. State ownership

One source of truth: **ConsoleStateStore** (pure Kotlin, StateFlow-based).

- `ConsoleState` — the 15-state visual machine (see UI_STATE_MACHINE.md), changed only through `ConsoleStateMachine.ensure(from, to)` which enforces the legal-transition table.
- `OperatingMode` — requested vs effective mode (DISCONNECTED_SAFE may override TOWER/HYBRID while the bridge is down; both are displayed).
- `ConnectionStatus` — mirrored from `TowerBridge.connection`.
- Conversation turns, captions, transcript draft, speaking amplitude, queue count, UI settings (theme, reduced motion, animation intensity, burn-in guard, keep-screen-on).

`ScriptedConsoleDriver` is the only component that orchestrates state transitions at runtime in V0.1 (the simulation of the voice loop). In V0.3 the real pipeline (wake → VAD → capture → STT → router → engine → TTS) replaces the driver behind the same store.

## 4. Routing logic (deterministic core)

`RuleBasedIntentRouter` maps `(text, requestedMode, effectiveMode, connectivity, sttConfidence)` → `RoutingDecision {action, intent, confidence, consequential, reason}`. The rule table is short and auditable: local whitelist (greetings, help, time/date, device status, identity) → HANDLE_LOCAL where mode permits; consequential patterns (mission/execute/send/approve/control…) → ROUTE_TOWER; unknown or low-confidence → ROUTE_TOWER in HYBRID/TOWER, QUEUE_FOR_TOWER in DISCONNECTED_SAFE (non-consequential), DECLINE with explanation otherwise. Unit tests pin every branch.

## 5. Tower bridge design

- `TowerBridge` interface: connection StateFlow, `configure(endpoint)`, `handshake()`, `sendRequest(frame): Flow<TowerEventFrame>`, `cancel(requestId)`.
- Message models in `TowerContract.kt` (kotlinx-serialization), protocol id `hermes.console.v0.1`, error taxonomy (AUTH, TRANSPORT, PROTOCOL, CAPACITY, TOWER_INTERNAL, CANCELLED, INVALID_REQUEST).
- `MockTowerBridge` emits deterministic ACK → PROGRESS → DELTA* → FINAL(receiptRef) flows with injectable faults (disconnect-at-ack, tower-error-after-progress, slow) for honest UI testing of DISCONNECTED / ERROR / REQUEST_QUEUED.
- No real endpoint, no invented routes, no credentials. The transport adapter slot is prepared, not occupied.

## 6. Threading and concurrency

- UI: Compose main thread; state collected with `collectAsState`.
- Orchestration: driver runs in `CoroutineScope(SupervisorJob() + Dispatchers.Default)`; one active pipeline job, cancelled on new submission, mute, stop-speaking, or mode change.
- Bridge flows: `flowOn(Dispatchers.Default)`; cancellation is cooperative (flow collection cancel).
- Purity: domain classes never launch coroutines on Android dispatchers; they receive scopes.

## 7. Persistence and secrets

- V0.1: `SharedPreferences` (SettingsRepository) for UI settings; in-memory event ring + turn list. Justification: no sensitive data exists yet, and Room/DataStore adoption is deferred until the data model stabilizes on-device (decision D-004).
- V0.5+: Android Keystore for device-bound secrets; pairing before any credential; EncryptedSharedPreferences or Keystore-wrapped storage; nothing secret in backups.

## 8. Android integration points (V0.1 scope)

- Single Activity (`MainActivity`), Compose navigation, `FLAG_KEEP_SCREEN_ON` toggleable from Appearance.
- `VoiceConsoleService` skeleton registered but not started (foreground service with microphone type arrives in V0.2 with real capture); Diagnostics shows its truthful state.
- Permissions declared in manifest with justification comments (`RECORD_AUDIO`, `INTERNET`, `POST_NOTIFICATIONS`, `FOREGROUND_SERVICE*`) — **declared, not requested at runtime in V0.1**; no permission dialog exists.
- Burn-in guard: slow sub-pixel drift of console content via `graphicsLayer` translation, disabled by reduced motion.

## 9. Failure strategy

- Every asynchronous failure lands in a truthful state (ERROR / DISCONNECTED / DEGRADED / REQUEST_QUEUED) — never silently swallowed, never fabricated as success.
- Error classification from the contract drives recovery: TRANSPORT → queue/retry; TOWER_INTERNAL non-retryable → FAILED turn with reason; CANCELLED → back to IDLE.
- Restart-after-death (V0.2): store snapshots minimal state; on boot the console returns to IDLE/DISCONNECTED per bridge reality — never to a stale "SPEAKING".

## 10. Future module extraction (target layout)

```
:app                 → UI, navigation, DI graph
:core:state          → ConsoleStateStore, StateMachine, OperatingMode
:core:routing        → IntentRouter + rules
:core:contract       → Tower contract models + redaction
:engine:audio        → capture, VAD, wake seams + adapters
:engine:stt / :engine:tts / :engine:llm
:bridge:tower        → real transport adapter (OkHttp/WSS)
:bridge:tower-mock   → mock bridge + simulator (test harness)
```
Extraction happens when a second consumer exists (e.g., instrumented tests or a benchmark harness), not before — no speculative modules in V0.1.

## 11. Build and test strategy

- JVM unit tests for state machine, router, bridge, contract serialization, redaction, and the full scripted pipeline (`kotlinx-coroutines-test`).
- Instrumented tests and on-device benchmarks begin in V0.2 with the physical qualification program.
- Static analysis: Android lint at build time; ktlint proposed at V0.2 adoption (not added in V0.1 to keep the un-compiled toolchain surface minimal and honest).


---

# Graph 3 architecture addendum (2026-11-02)

New UI layer `ui/monumental/`:

| Unit | Role |
|---|---|
| MonumentalColor.kt | authorized material/energy/text tokens (CompositionLocal) |
| MonumentalType.kt | lapidary display typography tokens |
| VisualQuality.kt | LOW/BALANCED/HIGH budgets + thermal ladder (pure Kotlin) |
| BurnIn.kt | per-region AMOLED safeguard (pure logic + Modifier) |
| OracleCoreModel.kt | PURE 15-state → visual-parameter mapping (unit-tested) |
| OracleCore.kt | layered renderer L0..L9; WebP depth layers + Canvas dynamics |
| MonumentalChrome.kt | seals/panels/medallions (new components) |

ui/components/Chrome.kt keeps its public API; internals now render the Monumental
material language, converging every Graph 2 screen without functional changes.
The asset pipeline decision (hybrid WebP+Canvas+vector) and the asset manifest live in
`docs/graph3/ASSET_PIPELINE_DECISION.md` and `docs/graph3/ASSET_MANIFEST.json`.
