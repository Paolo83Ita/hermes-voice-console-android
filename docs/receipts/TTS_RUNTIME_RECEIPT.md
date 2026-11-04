# TTS RUNTIME RECEIPT — Graph 4.6 (§5 REAL TTS DELIVERY)

Directive: GRAPH 4.6 §5 ("current fallback: Android System TTS — required:
downloadable voice candidate; source; license; install; activation; sample playback;
interruption; diagnostics"). Verification below executed physically this session
(2026-11-04); nothing narrated from memory.

## Required state

**`LOCAL_TTS_ACTIVE`** for the Android system TTS path (real synthesis is running in
the app today) **and** `LOCAL_TTS_READY_FOR_XIAOMI_QUALIFICATION` for the natural
downloaded-voice path (candidate verified, runtime integration pending its physical
qualification). The two-tier statement is the honest one.

## 1. ACTIVE engine — Android system TTS (tier 2 of the fallback chain)

| Contract item | State | Evidence |
| --- | --- | --- |
| Activation | `AndroidSystemTtsEngine.load()` → `TextToSpeech` init listener → READY; language set to `Locale.ITALY`; init failure → honest FAILED state with the status code | implemented, unit-covered |
| Sample playback | `speak(text, rate)` emits a real `Flow<Float>` amplitude while the utterance plays; completion ends the flow | implemented; Console "SPEAKING" state + captions driven by it |
| Interruption | `interrupt()` → `stop()`; the universal "any state → IDLE" machine rule plus Owner barge-in path both land here | implemented (UI_STATE_MACHINE universal rule) |
| Diagnostics | `activeEngineName` captured at init from `TextToSpeech.getDefaultEngine()` — the REAL engine package is displayed in the Diagnostics "Active runtime" card (×5 locales) via `AppGraph.activeRuntimeReadback` | implemented this graph (§13 read-back) |
| No-engine degradation | no TTS service on device → text-only console, honest state (no invented audio) | implemented |

## 2. Downloadable voice candidates (verified delivery chain)

Both candidates are REAL files with known SHA-256, revision-bound, delivered through
the Model Manager download → verify → install chain (same pipeline as STT/wake/denoise):

| Candidate | Identity | Size | SHA-256 | License | Source |
| --- | --- | --- | --- | --- | --- |
| Paola (medium) | `org.piper.voice.it-paola-medium` | 67,221,173 B download / 81,153,749 B installed | `7541f75778afa164e44e34baaef63befad7698595df26a95ca944b63ef1a16b4` | MIT (`github.com/rhasspy/piper-voices` LICENSE) | `https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/vits-piper-it_IT-paola-medium.tar.bz2` (tag-pinned release asset) |
| Riccardo (x_low) | `org.piper.voice.it-riccardo-x-low` | 26,496,614 B download | catalog-pinned | MIT | same release, `vits-piper-it_IT-riccardo-x_low.tar.bz2` |

Both are HTTPS, reviewed hosts (github.com/k2-fsa), immutable release-asset URLs —
`CatalogLinkIntegrityTest` enforces source+license openability, 64-hex SHA-256 and
revision binding for every VERIFIED_DOWNLOADABLE row.

## 3. Natural-voice runtime — the honest typed gap

| Item | State |
| --- | --- |
| Consuming runtime for the Piper voices | **sherpa-onnx Android runtime — CANDIDATE_PENDING_DISCOVERY.** No authoritative Maven artifact exists; pre-built AARs ship only via GitHub releases. The catalog's earlier `RUNTIME_BUNDLED / pinned in gradle` claim was FALSE and was corrected this graph (CP-G4-019/020 honesty correction, enforced by `VoicePackInstallerTest`) |
| Consequence | the downloaded voices stay INSTALLED-BUT-INACTIVE: the activation path refuses with the typed runtime gap instead of pretending; the ACTIVE voice remains Android system TTS |
| Path to ACTIVE | bind the release-pinned AAR (hash-verified), then Xiaomi qualification measures the §5 matrix (sample playback, interruption latency, RAM) |

## 4. On-device qualification items (Xiaomi package §5)

1. System TTS: Italian utterance → audible playback → interrupt mid-sentence → IDLE.
2. Engine identity read-back shows the actual package (e.g. `com.google.android.tts`).
3. Piper candidate: download (67 MB) → SHA-256 verify → install → activation attempt
   shows the TYPED runtime gap (honest) — no fake voice switch.
4. Interruption (barge-in) during SPEAKING returns to IDLE ≤ 300 ms.

## 5. Honesty boundaries (explicit)

| Not claimed | Why |
| --- | --- |
| Piper voice ACTIVE | runtime not integrated; typed gap enforced by tests |
| Any latency/RAM figure | measured only in the Xiaomi session |
| Any engine package name beyond the live read-back | identity comes from the device, not the catalog |
