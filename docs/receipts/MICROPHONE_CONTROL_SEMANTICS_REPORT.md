# MICROPHONE CONTROL SEMANTICS REPORT

- Mission: OWNER PHYSICAL RETEST FAILURE (CP-G4-029) — §9 MICROPHONE AND MUTE
  ICONS MUST BE DISTINCT / §10 CONTROL SEMANTICS AND ACCESSIBILITY
- Owner: Paolo Lombisani · Device: POCO X6 Pro
- Screen: Console (`console`, ConsoleScreen) — the control medallions row.
- Physical observation (PHYSICAL-FAIL-04): the microphone activation control
  uses a symbol that appears the same as the mute symbol; the two controls
  are not semantically distinguishable.

## 1. The pre-repair collision (code-proven at `53841a5`)

| Control | Pre-repair icon | Collision |
| --- | --- | --- |
| Privacy mute | `Icons.Filled.Mic` when NOT muted / `MicOff` when muted | the UNMUTED mute medallion rendered the same `Mic` symbol as the push-to-talk idle medallion, side by side in the same row |
| Push-to-talk | `Icons.Filled.Mic` when idle / `Stop` when listening | same `Mic` symbol as the unmuted mute control |
| Stop speaking | `Icons.Filled.Stop` | same `Stop` as the listening push-to-talk |
| Repeat / wake / inject | Replay / Send / History | unique |

Three controls shared two symbols — the Owner's report is exactly the
`ICON_SEMANTIC_COLLISION` class.

## 2. The required visual vocabulary (now implemented)

The single typed authority is `ui/screens/ConsoleControlSemantics.kt`
(`ConsoleControlSemanticsResolver`) — the composable renders ONLY what the
resolver returns. Required states:

| State | Icon | Label (it) | contentDescription (it) | Semantic identity |
| --- | --- | --- | --- | --- |
| MICROPHONE_IDLE | **Mic** (no slash) | Parla | "Avvia l'ascolto: un turno vocale push-to-talk" | `voice_start_listening` |
| MICROPHONE_LISTENING | **Stop** (cyan) | Interrompi ascolto | "Interrompi l'ascolto: termina l'acquisizione attiva" + state "In acquisizione" | `voice_stop_listening` |
| PRIVACY_MUTED | **MicOff** inside a thicker **red-bronze privacy seal** (border 3 dp, `redBronze`) | Microfono disattivato (persistent text row + stateDescription) | "Mute privacy attiva: microfono disconnesso. Tocca per riconnettere." | `privacy_mute_disable` |
| privacy mute (unmuted) | **MicOff** (dimmed, no seal) — the privacy control is ALWAYS MicOff, never the start-mic symbol | Mute privacy | "Attiva la mute privacy: disconnette fisicamente il microfono" | `privacy_mute_enable` |
| TTS_SPEAKING | **VolumeUp** (cyan) — never a microphone icon for TTS playback | Interrompi voce | "Interrompi la voce: ferma la risposta parlata" | `tts_stop_speaking` |
| TEXT_SEND | Send | send | "Invia il messaggio scritto alla console" | `text_send` |

The persistent muted text row ("Microfono disattivato", red-bronze, polite
live region) sits directly under the medallions while the privacy mute is
engaged; when not muted the row shows the CURRENT push-to-talk action label
(Parla / Interrompi ascolto).

## 3. The control matrix — distinct in every state

| Symbol | Used by | Never used by |
| --- | --- | --- |
| Mic | push-to-talk IDLE only | privacy mute, TTS stop, repeat, wake, inject |
| Stop | push-to-talk LISTENING only | privacy mute, TTS stop (now VolumeUp) |
| MicOff | privacy mute ALWAYS (its identity) | push-to-talk (any state) |
| VolumeUp | TTS stop only | microphone controls |

No two on-screen controls share a content description in ANY state
combination — asserted by `ConsoleControlSemanticsTest` over ALL
`MicUiState.entries × {muted, unmuted}` (6 × 2 states), plus the identity
tokens (`voice_start_listening`, `voice_stop_listening`,
`privacy_mute_enable`, `privacy_mute_disable`, `tts_stop_speaking`,
`text_send`), the stateDescriptions and the no-mic-for-TTS rule.

## 4. Accessibility contract (§10)

- Every control: unique icon + label + contentDescription (+ stateDescription
  where a state exists); TalkBack announces the resolved strings verbatim.
- The muted state adds a polite live-region text row — the state change is
  announced without focusing the control again.
- Distinct behaviors preserved (regression guards): privacy mute blocks
  capture physically (INV-G4-10, mute → capture refused); stopping the voice
  (`stopSpeaking`) does NOT mute the microphone; stopping the listening
  (`stopCapture`) does NOT disable TTS — the three actions touch disjoint
  coordinators exactly as before; this cycle changed ONLY the visual/semantic
  identity, not the wiring.

## 5. Verification status

- Unit: `ConsoleControlSemanticsTest` (uniqueness in every state, required
  identities, icon/type rules) — green in the 310-test suite.
- Physical: the rendered states (mic idle / listening / privacy muted /
  TTS speaking) are captured in the next physical cycle
  (PENDING_OWNER_CAPTURE in the ledger).
