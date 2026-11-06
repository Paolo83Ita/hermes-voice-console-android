# PHYSICAL TTS CALL PATH REPORT

- Mission: OWNER PHYSICAL RETEST FAILURE (CP-G4-029) — §3 TRACE THE ACTUAL
  SPOKEN-RESPONSE PATH
- Owner: Paolo Lombisani · Device: POCO X6 Pro
- Code base read back at intake: `53841a5` (`graph-4/real-voice-pairing-archive-e2e`)
- Scope: the pre-repair spoken-response call chain, the exact defect classes,
  and the repair now in place.

## 1. The complete runtime call chain (pre-repair, `53841a5`)

```
ConsoleScreen (typed text / recognized speech)
  → ConsoleVoiceCoordinator.submitUtterance / driver.submitText
    → ScriptedConsoleDriver.runPipeline
      → routing decision (RuleBasedIntentRouter)
      → localExecutor / towerExecutor → response text
      → speakAndFinish(text, captions)
        → SystemTtsDomainAdapter.speak(text, rate)   ← rate DISCARDED (pre-repair)
          → engine.load(); wait for READY
          → AndroidSystemTtsEngine.speak(text)
            → negotiateAndApply(engine)              ← typed verdict published
            → engine.speak(text, QUEUE_FLUSH)        ← VERDICT NEVER CHECKED (fail-open)
              → Android TextToSpeech instance
                → engine package (system default, e.g. Google TTS)
                → selected Voice (or the engine's default voice)
                → output audio
```

The voice-pipeline path (VoicePipelineController.listenOnce) calls the same
`AndroidSystemTtsEngine.speak` directly — ONE instance serves both paths.

## 2. Answers to the mandated proof questions (§3)

| Question | Answer (code-proven on `53841a5`) |
| --- | --- |
| Which class receives the final response | `ScriptedConsoleDriver.speakAndFinish` (text/voice turns) and `VoicePipelineController` (frame pipeline) |
| Which class calls speak() | `SystemTtsDomainAdapter` (domain seam) → `AndroidSystemTtsEngine.speak` (platform seam) |
| Which TTS implementation is instantiated | `AndroidSystemTtsEngine` (real Android `TextToSpeech`), wired in `AppGraph` (`voiceTts`) |
| Is MockTextToSpeechEngine reachable | NO — test seam only; the composition root never instantiates it |
| Is InactiveTextToSpeechEngine reachable | NO — not on the TTS path (the `Inactive*` classes are voice-frontend stubs) |
| Is AndroidSystemTtsEngine used | YES — the ONLY TTS implementation in the app |
| More than one TTS engine instance | NO — single `AppGraph.voiceTts` shared by driver + pipeline |
| Stale instance after locale changes | NO — the engine re-reads the app language before EVERY utterance (`negotiateAndApply` in `speak()`); no second instance is created on locale change |
| Does the requested Italian locale reach the active engine | YES — `setLanguage(it-IT)` + explicit `setVoice` when an Italian voice exists; the RESULT was typed but… |
| Does speak() run before voice negotiation completes | WORSE — speak() ran REGARDLESS of the negotiation result (fail-open): the verdict was published but never gated audio |
| Does fallback occur | YES — when the device has no usable Italian voice, `setLanguage`/`setVoice` keep the engine's default (English) voice and the pre-repair code spoke it anyway (`FALLBACK_VOICE_TRIGGERED` audible — PHYSICAL-FAIL-01) |

## 3. Root causes of the physically heard defects

1. **PHYSICAL-FAIL-01 (English spoken response)** — the fail-open gate:
   `AndroidSystemTtsEngine.speak()` negotiated and then spoke
   unconditionally. The typed negotiation verdict
   (`ITALIAN_VOICE_MISSING_DATA` / `FALLBACK_VOICE_TRIGGERED`) reached the
   Diagnostics UI but never the audio path.
2. **PHYSICAL-FAIL-02 (`HIGH_PITCH_ENGLISH_TTS_OUTPUT`)** — no speech profile:
   the pre-repair pipeline contained NO `setSpeechRate`/`setPitch` call;
   `SystemTtsDomainAdapter.speak(text, rate)` discarded `rate`. The audible
   voice = the engine's default voice character (high-pitched English
   synthetic voice), not an app-chosen profile.

## 4. The repair (this cycle)

1. **`TtsSpeakGate`** — the fail-closed decision (pure, 7 unit tests): when
   the effective language is Italian, audio requires the read-back to report
   an Italian voice. `ITALIAN_SYSTEM_TTS_READBACK_MISMATCH` /
   `ITALIAN_SYSTEM_TTS_DATA_MISSING` / `ITALIAN_SYSTEM_TTS_NOT_SUPPORTED` /
   engine-unavailable all REFUSE the utterance.
2. **`AndroidSystemTtsEngine.speak()`** — negotiates → applies the gate →
   only then applies the persisted `SpeechProfile` (rate/pitch) → speaks.
   A refusal publishes `TtsState.TEXT_ONLY_SAFE_DEGRADED` + typed
   `TtsSpeakBlock`; the response remains visible as text; the UI offers
   Open-Android-TTS-settings + Install-Italian-voice-data actions.
3. **`SpeechProfile`** — the rate/pitch authority (defaults 0.96/0.95, safe
   ranges 0.75–1.30 / 0.75–1.25), persisted via `SettingsRepository`
   (`speech_pitch` key) and read back after restart.
4. **`SystemTtsDomainAdapter`** — the post-speak race is fixed (the
   pre-repair single check ran before the utterance listener could ever fire,
   so the console never showed SPEAKING for real audio); the adapter now
   polls briefly for utterance start and treats the fail-closed refusal as
   an honest text-only completion.
5. **Structured telemetry** (`TtsTelemetry`, secret-clean ring buffer +
   verbatim surface in the diagnostic card and the exported receipt):
   engine init, locale requested, language availability, voices enumerated
   (total/Italian/offline/networked), selected voice, setVoice result,
   read-back voice + language, negotiation status, network-required state,
   rate/pitch applied, speak call (LENGTH ONLY — never spoken content),
   utterance start/done/error, speak-blocked token, fallback reason.
6. **ST-51** now reports the real-engine result classes: PASS (Italian
   read-back + utterance completed), BLOCKED_DEPENDENCY (Italian voice data
   missing / engine unavailable), FAIL (a non-Italian voice active while
   Italian was requested), QUALIFICATION_PENDING_DEVICE (JVM/emulator —
   never a JVM pass).

## 5. Honesty boundaries

- The on-device confirmation (which engine package, which voice, Italian
  availability) is the NEXT physical cycle's read-back: the Owner exports the
  secret-clean diagnostic receipt from About & Safety → Build information or
  Diagnostics; the telemetry rows answer the call-path questions verbatim.
- No claim is made that the POCO now speaks Italian — the claim is that a
  non-Italian voice CANNOT be audible while Italian is requested (fail-closed),
  and that when an Italian voice exists it is selected, applied and verified
  by read-back before any audio.
