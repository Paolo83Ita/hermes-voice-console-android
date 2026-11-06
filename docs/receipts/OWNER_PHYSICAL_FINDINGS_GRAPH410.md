# OWNER PHYSICAL FINDINGS — GRAPH 4.10 (CP-G4.10-001)

Status entering this mission: **OWNER_RETEST_FAILED_REPAIR_REQUIRED**
Evidence class: **OWNER PHYSICAL — supersedes emulator assumptions.**
These findings are binding. Nothing on an emulator or JVM may refute them.

## Devices

| | DEVICE A | DEVICE B |
| --- | --- | --- |
| Model | POCO X6 Pro | Xiaomi 11 Lite 5G NE |
| RAM | 8 GB | 8 GB |
| Storage | 256 GB | 128 GB |
| ABI | arm64-v8a | arm64-v8a |

## Observed on the physical devices (Owner report, binding)

1. **P0-01 — Local chat responses are English while the UI is Italian.**
   The chat response TEXT is English and the console SPEAKS that English response.
2. **P0-02 — The Italian TTS diagnostic phrase works.**
   The TTS engine CAN produce Italian speech (isolates the defect to response
   language + voice selection, NOT to the engine's capability).
3. **P0-03 — Downloaded and selected voices do not materially change the voice**
   used by the chat (selection is not consumed by the runtime).
4. **P0-04 — The speaker button does not perform the expected action.**
5. **P0-05 — No dedicated mute for spoken chat responses.**
6. **P0-06 — No continuous-listening mode.**
7. **P0-07 — The Italian label "parla invece" is unacceptable → must be "Parla".**
8. **P0-08 — The Xiaomi compact layout is broken**, especially Tower Connection
   Studio: header and action layout overlap; "Reset to Mock" renders one
   character per line.
9. **P0-09 — Raw English status values such as "Invalid" remain visible.**
10. **P0-10 — The current Tower visual is rejected by the Owner.**
11. **P0-11 — The Owner wants the central Tower from the provided image
    (`Immagine_Hermes_Tower.png`) isolated, reconstructed, animated and used
    in the Console.**

## Source-level confirmation (fresh read-back c18e313)

| Finding | Root cause located |
| --- | --- |
| P0-01 | `MockLocalConversationEngine` (all-English canned replies), `DeterministicTowerSimulator` (all-English), `ScriptedConsoleDriver` (hard-coded English notes/errors); no locale input anywhere. |
| P0-02 | Confirms `AndroidSystemTtsEngine` + Italian negotiation works — the Graph 4.9 fail-closed gate did its job. |
| P0-03 | `TtsRouteSwitch` switches ENGINES per voice, but both natural routes use `SherpaOnnxTtsEngine` per-voice instances wired through registry identity; Owner heard no material difference — selection→runtime authority must become explicit (`VoiceSelectionAuthority` + `VoiceRuntimeFactory`), with runtime read-back and A/B-verifiable switching. |
| P0-04 | Only actions today: `repeatLastSpoken()` medallion + TTS stop; no per-message playback; no audibly distinct "speaker" semantics. |
| P0-05 | No `ChatSpeechOutputPolicy`; no chat-speech mute anywhere. |
| P0-06 | Push-to-talk only; no `ListeningModePolicy`, no continuous loop, no echo prevention. |
| P0-07 | `values-it/strings.xml:10` `composer_speak_instead` = "parla invece". |
| P0-08 | `TowerStudioScreen` five fixed `Row(spacedBy(6.dp))` multi-button groups; no width classes; Console header not height-adaptive. |
| P0-09 | `ConnectionConfigState`/`TowerConnectionTester.Outcome`/`TransportMode` English labels rendered raw (`displayLabel`, `.name`). |
| P0-10/11 | Visual is procedural Canvas (`ui/monumental/*`); layered reference-derived asset Tower does not exist. |

## Owner state machine

| State | Rule |
| --- | --- |
| `OWNER_RETEST_FAILED_REPAIR_REQUIRED` | current (set by the physical retest) |
| `POCO_XIAOMI_RETEST_PACKAGE_3_READY` | required pre-device state (this mission's terminal boundary) |
| `POCO_XIAOMI_CHAT_VOICE_LISTENING_LAYOUT_AND_VISUAL_RETEST_PASSED` | ONLY the Owner may grant this after a physical retest |

Forbidden without Owner evidence: `POCO_RETEST_PASSED`,
`ITALIAN_TTS_OWNER_VERIFIED`, `VOICE_SWITCHING_OWNER_VERIFIED`,
`RESPONSIVE_UI_QUALIFIED` (Graph 4.9 token), `REAL_TOWER_PAIRING_PASSED`.
