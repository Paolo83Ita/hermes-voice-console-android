# PROJECT FILE TREE PROPOSAL

| | |
|---|---|
| Owner | Paolo Lombisani |
| Version | v0.1.0-alpha |
| Date | 2026-10-30 |

## A. Delivered V0.1 tree (actual, this package)

```
hermes-voice-console-android/
├── README.md                       — orientation, status, quick start
├── BUILDING.md                     — exact reproducible build instructions
├── FINAL_REPORT.md                 — mission report: summary, inventory, build status, blockers
├── .gitignore
├── docs/                           — STAGE A deliverables
│   ├── PRODUCT_VISION.md
│   ├── TARGET_DEVICE_ASSUMPTIONS.md
│   ├── REQUIREMENTS.md
│   ├── ARCHITECTURE.md
│   ├── SECURITY_AND_PRIVACY_MODEL.md
│   ├── VOICE_PIPELINE_OPTIONS.md
│   ├── LOCAL_MODEL_CANDIDATE_PLAN.md
│   ├── TOWER_BRIDGE_PROPOSED_CONTRACT.md
│   ├── UI_STATE_MACHINE.md
│   ├── RISK_REGISTER.md
│   ├── ASSUMPTION_REGISTER.md
│   ├── DEVICE_QUALIFICATION_CHECKLIST.md
│   ├── DEVELOPMENT_ROADMAP.md
│   ├── FILE_TREE_PROPOSAL.md       — this file
│   ├── ARCHITECTURE_DIAGRAM.md     — Mermaid component diagram
│   ├── SEQUENCE_DIAGRAMS.md        — LOCAL / TOWER / HYBRID / DISCONNECTED_SAFE
│   ├── VISUAL_DIRECTION.md         — Marble Light vs Obsidian Night + comparison
│   ├── DECISION_LOG.md
│   └── WORK_LOG.md                 — checkpoint reports (NODE/FINDING/ACTION/TEST/FILES/COMMIT/NEXT)
├── design/
│   └── mockups/
│       ├── mockup_marble_light.html/.png
│       ├── mockup_obsidian_night.html/.png
│       └── mockup_comparison.html/.png
├── gradle/
│   ├── libs.versions.toml          — version catalog (single source of versions)
│   └── wrapper/gradle-wrapper.properties
├── settings.gradle.kts
├── build.gradle.kts
├── gradle.properties
└── app/
    ├── build.gradle.kts
    ├── proguard-rules.pro
    └── src/
        ├── main/
        │   ├── AndroidManifest.xml
        │   ├── res/
        │   │   ├── values/{strings.xml, themes.xml, colors.xml}
        │   │   ├── drawable/ic_launcher_foreground.xml
        │   │   └── mipmap-anydpi-v26/ic_launcher.xml
        │   └── java/com/hermestower/voiceconsole/
        │       ├── app/            — HermesApp, AppGraph (service locator)
        │       ├── core/state/     — ConsoleState, ConsoleStateMachine, ConsoleStateStore
        │       ├── core/mode/      — OperatingMode
        │       ├── core/routing/   — IntentRouter (interface), RuleBasedIntentRouter
        │       ├── domain/         — the 14 engine contracts + shared result types
        │       ├── bridge/         — TowerContract, MockTowerBridge, DeterministicTowerSimulator
        │       ├── local/          — MockLocalConversationEngine
        │       ├── tts/            — MockTextToSpeechEngine
        │       ├── voice/          — wake/VAD/STT inactive stubs
        │       ├── simulator/      — ScriptedConsoleDriver (voice-loop simulation)
        │       ├── privacy/        — LogRedactor
        │       ├── health/         — AndroidDeviceHealthMonitor (real device data)
        │       ├── store/          — SettingsRepository (SharedPreferences)
        │       ├── service/        — VoiceConsoleService skeleton (registered, not started)
        │       └── ui/
        │           ├── MainActivity.kt, AppNav.kt
        │           ├── theme/      — Color, Type, Shape, Theme, Motion (both palettes)
        │           ├── components/ — TowerCore, MeanderBorder, StateChip, scaffolding
        │           └── screens/    — Console, History, Connection, VoiceAudio,
        │                             LocalIntelligence, Appearance, Diagnostics, AboutSafety
        └── test/java/com/hermestower/voiceconsole/
            ├── state/   — ConsoleStateMachineTest, ConsoleStateStoreTest
            ├── routing/ — RuleBasedIntentRouterTest
            ├── bridge/  — MockTowerBridgeTest, TowerContractJsonTest
            ├── privacy/ — LogRedactorTest
            └── simulator/ — ScriptedConsoleDriverTest (full pipeline, local + tower + queue paths)
```

## B. Target modular tree (V0.4+ extraction, per ARCHITECTURE §10)

```
:app                 UI, navigation, DI graph
:core:state          state machine + store
:core:routing        router + rules
:core:contract       Tower contract + redaction
:engine:audio        capture, VAD, wake seams + adapters
:engine:stt          STT adapters (whisper.cpp, system)
:engine:tts          TTS adapters (system, Piper, mock)
:engine:llm          local LLM runtime adapter
:bridge:tower        real OkHttp/WSS adapter
:bridge:tower-mock   mock bridge + deterministic simulator
```

## C. Deferred/optional locations

```
docs/qualification/        session-YYYY-MM-DD.md evidence files (starts V0.2)
design/enclosure/          3D-print references, vent plan (V0.7)
tools/                     benchmark harness scripts (V0.6)
```

Rationale for the V0.1 shape: a single `:app` module keeps the un-compiled toolchain surface honest and small, while package-level modularity (`core/domain/adapters/ui`) already enforces the dependency direction. Extraction into the target tree happens only when a second consumer appears (ARCHITECTURE.md §10).
