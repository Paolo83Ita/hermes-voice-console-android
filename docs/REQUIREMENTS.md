# REQUIREMENTS — HERMES VOICE CONSOLE ANDROID

| | |
|---|---|
| Owner | Paolo Lombisani |
| Version | v0.1.0-alpha |
| Date | 2026-10-30 |
| Status | For Owner review |
| Convention | FR = functional · NFR = non-functional · SEC = security/privacy · DOC = documentation. Status per release: V0.1 (this prototype), V0.2+, V1.0. |

## 1. Functional requirements — voice pipeline

| ID | Requirement | Release |
|---|---|---|
| FR-01 | Wake-word detection runs on-device and is the only always-listening component; no other microphone consumer is active without a visible listening state | V0.3 |
| FR-02 | Voice activity detection gates capture: silence after speech ends ends the capture window | V0.3 |
| FR-03 | Audio capture uses the microphone with an explicit visible LISTENING state and a loud/soft privacy mute; capture never starts while PRIVACY_MUTED | V0.3 |
| FR-04 | Noise suppression applied where feasible and disclosed (provider + on/off) | V0.3+ |
| FR-05 | Speech-to-text produces a visible live transcript draft and a final transcript with a confidence value | V0.3 |
| FR-06 | Text-to-speech is interruptible at any moment (stop control, barge-in by new wake/speech), with sentence-level streaming where the provider allows | V0.3 (system TTS fallback) |
| FR-07 | Captions display the spoken response text in sync with playback | V0.3 |
| FR-08 | Volume control and speaking-rate control exist in Voice & Audio settings | V0.3 |
| FR-09 | No speech audio is written to persistent storage unless the Owner explicitly enables audio retention (default OFF) | V0.3 |
| FR-10 | The V0.1 prototype simulates wake → listen → transcribe → respond with text injection instead of the microphone; the simulation uses the real router/bridge/TTS-abstraction code paths | V0.1 ✅ |

## 2. Functional requirements — conversation and routing

| ID | Requirement | Release |
|---|---|---|
| FR-11 | A deterministic IntentRouter classifies each turn into HANDLE_LOCAL / ROUTE_TOWER / QUEUE_FOR_TOWER / DECLINE with a human-readable reason | V0.1 ✅ |
| FR-12 | LOCAL mode resolves only whitelisted local intents and declines the rest with an explanation | V0.1 ✅ |
| FR-13 | TOWER mode forwards every request; on disconnection it queues non-consequential requests and declines consequential ones | V0.1 ✅ |
| FR-14 | HYBRID mode (default) resolves qualified simple queries locally and routes complex, operational, uncertain, or consequential requests to the Tower | V0.1 ✅ |
| FR-15 | DISCONNECTED_SAFE mode discloses the condition, allows safe local functions, and optionally queues non-consequential requests | V0.1 ✅ |
| FR-16 | Conversation History distinguishes LOCAL vs TOWER turns, shows delivery state (DELIVERED / PENDING / QUEUED / FAILED), allows retry only when safe (non-consequential, transport failure), and provides delete + retention controls | V0.1 ✅ |
| FR-17 | A queued request visibly transitions to the Tower when connectivity returns; it is never silently marked complete | V0.1 ✅ |
| FR-18 | The optional local LLM performs only: conversational front-end, small talk, intent detection, command cleanup, STT correction, lightweight semantic routing, turn summarization before forwarding, help and device-status explanations, graceful offline responses | V0.6 |
| FR-19 | The local LLM never executes consequential actions and never overrides Tower authority | permanent |

## 3. Functional requirements — Tower bridge

| ID | Requirement | Release |
|---|---|---|
| FR-20 | All Tower communication passes through the TowerBridge interface; UI and pipeline code cannot address a transport directly | V0.1 ✅ (interface) |
| FR-21 | V0.1 ships a mock TowerBridge + deterministic simulator; no real endpoint is contacted; no real routes are invented | V0.1 ✅ |
| FR-22 | The proposed versioned contract (TOWER_BRIDGE_PROPOSED_CONTRACT.md) covers device identity, session/conversation/request IDs, transcript, language, confidence, requested/effective mode, local routing decision, acceptance, progress, streamed response, cancellation, final state, error classification, receipt reference, timestamp, protocol version | V0.1 ✅ |
| FR-23 | Connection screen exposes: endpoint placeholder, pairing state, transport mode, TLS status, last successful connection, test-connection action; no hard-coded credentials anywhere | V0.1 ✅ |
| FR-24 | A real transport adapter (HTTPS/WSS) is added behind the same interface without UI changes | V0.4 |

## 4. Functional requirements — UI surfaces

| ID | Requirement | Release |
|---|---|---|
| FR-30 | S1 Main Voice Console: central animated Tower, truthful state, transcript area, response captions, microphone control, privacy mute, connection indicator, current mode, compact status | V0.1 ✅ |
| FR-31 | S2 Conversation History per FR-16 | V0.1 ✅ |
| FR-32 | S3 Connection per FR-23 (endpoint, pairing, transport, TLS status, last success, test action) | V0.1 ✅ |
| FR-33 | S4 Voice & Audio: input device, wake-word configuration placeholder, STT provider, TTS provider, voice selection, speaking speed, interruption behavior, captions | V0.1 ✅ (configuration scaffold, engines marked inactive) |
| FR-34 | S5 Local Intelligence: local model state, candidate placeholder, LOCAL/HYBRID routing policy, resource limits, load/unload controls, truthful qualification status | V0.1 ✅ |
| FR-35 | S6 Appearance: Marble Light and Obsidian Night themes, brightness, animation intensity, reduced motion, burn-in protection, kiosk behavior note | V0.1 ✅ |
| FR-36 | S7 Diagnostics: device info, memory, storage, temperature where available, service state, connection tests, audio tests, redacted log export, no secret values | V0.1 ✅ |
| FR-37 | S8 About & Safety: component versions, licenses, privacy explanation, known limitations, reset local data | V0.1 ✅ |

## 5. Functional requirements — always-on and display safety

| ID | Requirement | Release |
|---|---|---|
| FR-40 | Screen burn-in mitigation: subtle movement/drift of static elements, configurable guard | V0.1 ✅ (prototype) |
| FR-41 | Automatic dimming and reduced brightness during inactivity | V0.1 ✅ (dimming scrim prototype), V0.2 (real brightness control) |
| FR-42 | Scheduled screen rest and optional black idle mode | V0.2 |
| FR-43 | Thermal monitoring with charging-temperature warning | V0.1 ✅ (battery temperature + thermal status in Diagnostics), V0.2 (warnings) |
| FR-44 | Graceful restart after process death; boot recovery design | V0.2 |
| FR-45 | Foreground service for active voice operation | V0.2 (skeleton present in V0.1) |
| FR-46 | Kiosk mode as a later controlled capability | V0.7 |
| FR-47 | No assumption that Android/MIUI allows unrestricted background execution; all background behavior degrades honestly | permanent |

## 6. Non-functional requirements

| ID | Requirement | Target |
|---|---|---|
| NFR-01 | Local-path voice round trip (wake → first spoken audio) | ≤ 2.5 s on target device |
| NFR-02 | Console-added overhead on tower path (capture end → request sent; response end → first spoken audio) | ≤ 700 ms combined |
| NFR-03 | UI jank: no dropped frame in steady-state animations at device refresh rate | measured in V0.2 |
| NFR-04 | Contrast: all body text ≥ 4.5:1, large text/UI ≥ 3:1 (WCAG 2.1 AA); verified values in VISUAL_DIRECTION.md | permanent |
| NFR-05 | Touch targets ≥ 48 dp; TalkBack labels on every interactive element; state announcements via live region | permanent |
| NFR-06 | Reduced-motion mode renders a static, still-legible composition of every state | V0.1 ✅ |
| NFR-07 | Sustained surface temperature ≤ 42 °C in enclosure while charging under normal load | verified V0.2 |
| NFR-08 | Console steady-state memory footprint (without local LLM) | ≤ 350 MB |
| NFR-09 | With local LLM loaded (0.5–1.7B candidate class) | ≤ 1.6 GB RSS total, unload on idle timer |
| NFR-10 | Cold app start to interactive console | ≤ 2 s |
| NFR-11 | All engine seams replaceable without UI rewrite (WakeWordEngine, VoiceActivityDetector, AudioCaptureEngine, SpeechToTextEngine, LocalConversationEngine, IntentRouter, TowerBridge, TextToSpeechEngine, AnimationStateController, DeviceHealthMonitor, SecureConfigurationStore, SessionController, LocalEventStore, DiagnosticsExporter) | V0.1 ✅ interfaces |
| NFR-12 | Deterministic behavior of routing and bridge in unit tests (no network, no randomness without seed) | V0.1 ✅ |

## 7. Security and privacy requirements

| ID | Requirement |
|---|---|
| SEC-01 | No secrets in source, configuration files, logs, or screenshots |
| SEC-02 | Device-bound secrets live in Android Keystore; no token is displayed in UI after entry |
| SEC-03 | Tower communication uses TLS; pairing flow designed and approved before any real credential exists |
| SEC-04 | Explicit microphone indicator; prominent privacy mute that gates capture hardware-path honesty |
| SEC-05 | Audio retention OFF by default; transcript retention configurable with a retention window |
| SEC-06 | Logs redacted by default (LogRedactor: token/key/secret assignments, long hex/base64) |
| SEC-07 | No remote shell, arbitrary command execution, or unrestricted ADB bridge in V0.1 |
| SEC-08 | No hidden always-listening behavior; no automatic consequential action; no privilege escalation; no root requirement |
| SEC-09 | Mock and real transports are separated; real transport cannot be silently enabled |

## 8. Documentation requirements

| ID | Requirement |
|---|---|
| DOC-01 | Every technology candidate carries exactly one decision code: USE_FOR_FOUNDATION, PROTOTYPE, QUALIFY_ON_DEVICE, DEFER, REJECT |
| DOC-02 | Assumptions, risks, and decisions are maintained in registers with IDs and review dates |
| DOC-03 | Documentation matches implementation; anything simulated is labeled simulated |
| DOC-04 | Known limitations are explicit in About & Safety and in the final report |

## 9. Acceptance for V0.1 (quality gates from the mission)

1. Project opens in a current Android Studio environment (source complete, catalog versions coherent; wrapper regeneration documented because the wrapper JAR is a binary).
2. Architecture is modular; engine seams are interfaces; mock and real transports separated.
3. No real credential present anywhere in the repository.
4. UI states are truthful; no fabricated CPU/mission/model/connection data.
5. Visual identity is coherent; text readable at AA contrast.
6. Reduced-motion mode exists; static elements account for burn-in.
7. No unsupported hardware claim; no technology marked qualified without a physical device test.
8. Documentation matches implementation; known limitations explicit.
