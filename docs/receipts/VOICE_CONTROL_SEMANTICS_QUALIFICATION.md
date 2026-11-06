# VOICE_CONTROL_SEMANTICS_QUALIFICATION.md

- Mission: GRAPH 4.9 — CP-G4.9-006/§9 (microphone control qualification)
- Owner: Paolo Lombisani
- Written: 2026-11-07
- Preserve-order: the CP-G4-029 control vocabulary (ConsoleControlSemantics)
  is PRESERVED and re-qualified; nothing in this node changed it.

## 1. The distinct control vocabulary (implemented at CP-G4-029, qualified here)

| Console state | Icon | Label | contentDescription | semantic identity |
| --- | --- | --- | --- | --- |
| Microphone idle | Mic | Parla | "Parla" | `voice_start_listening` |
| Listening | Stop | Interrompi ascolto | "Interrompi ascolto" | `voice_stop_listening` |
| Privacy muted | MicOff + red-bronze seal (3 dp) | Microfono disattivato | "Microfono disattivato" + persistent live-region row | `privacy_mute_enable` / `privacy_mute_disable` |
| TTS speaking | VolumeUp | Interrompi voce | "Interrompi voce" | `tts_stop_speaking` |
| Text send | Send | Invia | "Invia" | `text_send` |

Rules enforced by `ConsoleControlSemanticsResolver` (pure, unit-tested):
- the mic icon is NEVER used for the TTS stop control (the PHYSICAL-FAIL-04
  collision is dead by construction);
- privacy mute ALWAYS shows MicOff + the red-bronze seal regardless of the
  console state, plus the persistent text row (TalkBack live region);
- every (MicUiState × muted) combination has a UNIQUE
  icon+label+contentDescription+stateDescription tuple;
- wiring unchanged: mute still physically blocks capture (INV-G4-10) and
  stopping the voice never mutes the microphone.

## 2. Combination matrix (briefing §9) → evidence

| Combination | Expected control truth | Evidence |
| --- | --- | --- |
| mic idle | Mic + Parla | `ConsoleControlSemanticsTest` (full state matrix) |
| listening | Stop + Interrompi ascolto | same unit matrix |
| privacy muted | MicOff + seal + persistent row | same unit matrix |
| TTS speaking | VolumeUp + Interrompi voce (never a mic icon) | same unit matrix |
| listening while TTS stops | stop-listening control stays the listening truth | unit matrix (independent controls) |
| TTS speaking while mic muted | mic shows MicOff+seal, TTS stop stays VolumeUp | unit matrix (states compose) |
| error | mic control per MicUiState, refusal text typed | ConsoleVoiceCoordinator refusals |
| disconnected | mic unavailable → typed refusal "recognizer unavailable" | ConsoleVoiceCoordinatorTest |

## 3. Accessibility proof classes

- unique content descriptions + state descriptions: unit matrix GREEN
  (334+ suite);
- unique semantic IDs: the identity table above is asserted verbatim in
  `ConsoleControlSemanticsTest`;
- TalkBack order: the mute row is a live region; the walk order
  (state row → transcript → controls) is the compose declaration order —
  the TALKBACK WALK ITSELF is a device-lane item (TalkBack is not installable
  on the headless emulator lane): **QUALIFICATION_PENDING_DEVICE**;
- 48 dp minimum touch targets: enforced by the control composables and the
  adaptive action row (unit-proven); physical pass pending.

## 4. Verdict

- JVM/evidence: QUALIFIED (unit matrix + wiring tests + lint 0 errors).
- Physical TalkBack walk + POCO screenshots: QUALIFICATION_PENDING_DEVICE
  (Owner physical evidence — no claim).
