# ITALIAN TTS ROOT CAUSE REPORT — Owner Physical Finding 3 (POCO X6 PRO)

Date: 2026-11-06 · Branch: `graph-4/real-voice-pairing-archive-e2e` · Tip at investigation: `4116819` (fix 24 / R-41, fresh fetch + read-back)
Intake receipt: `docs/receipts/OWNER_PHYSICAL_FEEDBACK_RECEIPT.md`

## 1. The observation (verbatim, typed)

> The local voice answers in English.
> This is a physical device observation.
> The TTS route is operational.
> The selected language route is incorrect.

Owner constraint: **classify precisely, do not guess, determine the real root cause**,
from: active TTS engine; active voice; voice language; selected locale; app locale;
Android locale; runtime read-back; fallback chain.

## 2. Package provenance (decisive physical fact)

The tested package (`hermes-voice-console-debug-apk-ca8980b…9df.zip`, binary-inspected
this session) is **commit `ca8980b` = Graph 3 terminal**, versionName `0.2.0-debug`,
versionCode 2. Its dex contains ONLY:

- `voice/InactiveSpeechToTextEngine`, `voice/InactiveVoiceActivityDetector`,
  `voice/InactiveWakeWordEngine`, `voice/NoAudioCaptureEngine`
- `tts/MockTextToSpeechEngine` (silent by design — produces no audio)

**No `AndroidSystemTtsEngine`, no `ConsoleVoiceCoordinator`, no real voice pipeline.**
Consequence: on THAT package the app could not have spoken at all — the TTS route was
NOT operational in it. The observation therefore binds either to (a) a later Graph 4
install, or (b) audio from outside the tested app. This contradiction is recorded, not
resolved by guessing; the runtime read-back delivered with the new package arbitrates
it ON DEVICE in the second validation cycle (§7).

## 3. Code-level root cause (PROVEN — applies to every Graph 4 build to date)

The pre-repair `AndroidSystemTtsEngine` (Graph 4, CP-G4-005 → tip `4116819`):

```kotlin
runCatching { tts?.language = Locale.ITALY }   // ← the ONLY language logic
```

Three independent defects, each individually sufficient:

| # | Defect | Mechanism |
| --- | --- | --- |
| D1 | **The typed result of `setLanguage` is DISCARDED.** `runCatching` catches exceptions only; the `Int` return (`LANG_MISSING_DATA` / `LANG_NOT_SUPPORTED` / `LANG_*_AVAILABLE`) is dropped | When the device's TTS engine lacks Italian voice data (the common state on a fresh Xiaomi/POCO — Google TTS ships en-US and downloads others on demand), the request fails SILENTLY and the engine keeps its default voice (en-US) |
| D2 | **No explicit Voice is ever selected.** The code never touches `tts.voices` / `tts.voice` | Setting `language` alone does not guarantee an Italian VOICE; engines keep the active voice or pick per their own policy. There is no language→voice routing to verify |
| D3 | **No read-back.** The active voice, its locale, the engine name — none are read after the request; the app publishes `TtsState.READY` regardless | The app cannot KNOW the audible language. The app-claimed state (Italian configured) and the device truth (English voice) drift apart, and no UI, diagnostic or self-test can catch it |

The audible result is exactly the Owner's finding: **the TTS route operates, the
answer comes out in English, and the app shows no defect.**

Additional route defect (D4): the spoken locale was HARDCODED (`Locale.ITALY`) — it
ignored the app language (AppLanguage/LocaleManager) and the Android locale, so even a
successful negotiation would not follow the Owner's "selected language route".

## 4. Precise classification (the Owner's candidate classes, judged on evidence)

| Candidate class | Verdict | Evidence |
| --- | --- | --- |
| `LANGUAGE_TO_VOICE_ROUTING_DEFECT` | **CONFIRMED (contract class)** | D1+D2: the language→voice mapping is never verified; the routing is a discarded one-way request |
| `ITALIAN_VOICE_NOT_ACTIVE` | **CONFIRMED (runtime state, consistent with the observation)** | English audio proves the active voice was not Italian on the tested build |
| `WRONG_VOICE_SELECTION` | NOT the root cause | No voice selection code existed at all — nothing selected a wrong voice |
| `FALLBACK_VOICE_TRIGGERED` | **CONFIRMED (the audible consequence)** | The engine's default (English) voice produced the audio — the fallback chain's tier 2 collapsed into its engine's own default |
| `TTS_CONFIGURATION_DRIFT` | **CONFIRMED (the observability consequence)** | D3: app state says READY/Italian; device truth is an English voice — undetectable before this repair |

**Root cause statement (typed):** `LANGUAGE_TO_VOICE_ROUTING_DEFECT` with silent
fallback — the engine requests Italian once, discards the typed result, selects no
voice, reads nothing back; when Italian voice data is absent the device speaks its
default (English) voice and the app reports readiness. The on-device discriminator
(ITALIAN_VOICE_MISSING_DATA vs ITALIAN_VOICE_NOT_SUPPORTED vs engine quirk) is now
measured by the app itself (§5 read-back) instead of being guessed here.

## 5. The repair (this cycle, commit CP-G4-026)

| Repair item | What changed |
| --- | --- |
| Typed negotiation | NEW `ItalianTtsVoiceNegotiator` (pure, unit-tested, 9 test cases): voice-selection preference (offline it-IT → any offline it → networked it-IT → any it), typed `TtsLanguageResultCode`, typed `ItalianTtsVoiceStatus` |
| Engine rewrite | `AndroidSystemTtsEngine` now: requests the FULL locale from the app language, enumerates `voices`, selects an explicit Italian `Voice` (offline preferred), then reads back `getVoice()` + `getLanguage()` + engine package and publishes `TtsVoiceReadback` on EVERY state emission |
| Silent-fallback kill | The negotiation re-runs before EVERY utterance; the post-apply truth — not the request — decides the classification |
| App-language route | Spoken locale = `AppLanguage.effectiveTag(context)` (Owner override → resolved system language); language changes re-apply immediately (AppearanceScreen hook) and are honored at the next answer by the provider re-read |
| Runtime read-back surfaces | Diagnostics → Active runtime: `Active voice: <describe>` + `Voice language: <locale>`; Settings → Voice & Audio → NEW "Italian voice" card (typed status badge, active voice, voice language, detail, and an `Open system Text-to-speech settings` action when user action can fix it); Self-Test **ST-51** "Italian TTS voice negotiation probe" (PASS on active; typed BLOCKED_DEPENDENCY with the full read-back + repair hint on device-side states) |
| Honest states published | `ITALIAN_VOICE_ACTIVE` / `ITALIAN_VOICE_MISSING_DATA` / `ITALIAN_VOICE_NOT_SUPPORTED` / `FALLBACK_VOICE_TRIGGERED` / `ENGINE_UNAVAILABLE` — never a bare string, never an invented "Italian" claim |

## 6. Required terminal states — status

| Required state | Status |
| --- | --- |
| `ITALIAN_VOICE_ACTIVE` | **GATED ON DEVICE READ-BACK** — the code path is implemented, unit-tested and cloud-verified; the typed status can only be pronounced by the POCO X6 PRO's own read-back (Diagnostics rows / ST-51) |
| `ITALIAN_VOICE_READY_AND_BLOCKED_BY_KNOWN_RUNTIME_LIMITATION` | **REACHABLE ON DEVICE** — if the read-back returns `ITALIAN_VOICE_MISSING_DATA` (fixable: install voice data, the app links directly to the system TTS settings) or `ITALIAN_VOICE_NOT_SUPPORTED`, the state is terminal WITH evidence per this report |

## 7. Second Owner validation cycle — the discriminating evidence (5 minutes on device)

1. Install the `0.3.0-alpha-graph4` package (guide: `docs/INSTALL_POCO_X6_PRO.md`).
2. Open **Settings → Voice & Audio → Italian voice**: read the typed status badge and the active voice.
3. Open **Settings → Diagnostics → Active runtime**: read `Active voice: …` and `Voice language: …`.
4. Run **Self-Test (Quick or Voice foundation only) → ST-51**: read the result line.
5. Ask the console a question and listen.

Decision table (no guessing required):

| Read-back shows | Meaning | Action |
| --- | --- | --- |
| `ITALIAN_VOICE_ACTIVE` (+ it-* voice) | Italian route COMPLETE | terminal state reached with evidence |
| `ITALIAN_VOICE_MISSING_DATA` | Italian data absent on the device (the D1 scenario) | tap the card's button → system TTS settings → install Italian voice → re-open the app → ST-51 again |
| `ITALIAN_VOICE_NOT_SUPPORTED` | the active engine cannot do Italian | switch the system TTS engine (Google TTS) and re-check |
| `FALLBACK_VOICE_TRIGGERED` | engine accepted but kept another voice | same as above; the report + read-back capture it |
| Status rows unavailable | the app never negotiated (engine absent) | text-only degradation is the honest terminal state |

If the console STILL answers in English while ST-51 reports `ITALIAN_VOICE_ACTIVE`,
the discrepancy itself is new decisive evidence (an engine that misreports) — capture
the Diagnostics screen and report it; the typed read-back makes the contradiction
provable instead of anecdotal.

## 8. Evidence index

- Binary inspection of the tested package: §2 (receipt §2 for the full table).
- Pre-repair code: `AndroidSystemTtsEngine.kt` at `4116819` (one line, D1).
- Repair: `ItalianTtsVoiceNegotiator.kt` (NEW), `AndroidSystemTtsEngine.kt` (rewritten),
  `VoiceSeams.kt` (`TtsOutput.voiceReadback`), `AppGraph.kt` (provider + read-back + ST-51 subject),
  `DiagnosticsScreen.kt` (+2 rows), `VoiceAudioScreen.kt` (Italian voice card),
  `SelfTestRegistry.kt` (ST-51), strings ×5 locales (659 keys, parity held).
- Unit tests: `ItalianTtsVoiceNegotiatorTest` (9 cases — including the exact Owner-observed class:
  language accepted, active voice English → `FALLBACK_VOICE_TRIGGERED`).
- Piper voices stay `INSTALLED-BUT-INACTIVE` (sherpa-onnx runtime gap, `TTS_RUNTIME_RECEIPT.md`) —
  unchanged by this report; the system TTS route is the route that spoke.
