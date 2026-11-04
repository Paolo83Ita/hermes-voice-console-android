# STT RUNTIME RECEIPT — Graph 4.6 (§4 REAL STT DELIVERY)

Directive: GRAPH 4.6 §4 ("vosk-android available — required: source verification;
license verification; download; install; activation; transcript generation; active
runtime read-back"). Every verification below was executed physically this session
(2026-11-04) in the delivery sandbox or against the live network — nothing is
narrated from memory.

## Required state

**`LOCAL_STT_READY_FOR_XIAOMI_QUALIFICATION`**

Justification (honest):
- the runtime, the model identity and the delivery chain are VERIFIED and INTEGRATED;
- real transcription requires a real microphone frame stream on hardware — the
  sandbox has no Android runtime (Stage A contingency) and the emulator cannot
  produce Italian speech — so transcript generation is executed in the Xiaomi
  qualification session (XIAOMI_GRAPH4_BETA_PACKAGE §4).

## 1. Source verification (executed 2026-11-04, live)

| Item | Value | Evidence |
| --- | --- | --- |
| Runtime artifact | `com.alphacephei:vosk-android:0.3.75` (AAR) | Maven Central directory listing fetched live |
| Download URL | `https://repo1.maven.org/maven2/com/alphacephei/vosk-android/0.3.75/vosk-android-0.3.75.aar` | immutable Maven path (group/artifact/version — no `latest`, no redirect) |
| AAR size | 13,472,638 bytes | measured on the downloaded file |
| AAR SHA-256 | `ab2f8b91ac8051561aa325546b35fed9a68b36b8121bac5c6fb927525c4adfad` | computed locally AND equal to Maven Central's published `.aar.sha256` |
| ABIs inside the AAR (measured, unzipped) | `arm64-v8a` (10,042,800 B libvosk.so) · `armeabi-v7a` (8,985,628 B) · `x86` (10,397,552 B) · `x86_64` (10,335,120 B) | `jni/<abi>/libvosk.so` — ABI-complete for the Xiaomi 11 Lite 5G NE (arm64-v8a) AND the x86_64 CI emulator |
| Dependency | `net.java.dev.jna:5.18.1` (AAR) | POM (Apache-2.0) |
| POM license declaration | "The Apache License, Version 2.0" — `http://www.apache.org/licenses/LICENSE-2.0.txt` | fetched POM |
| Upstream SCM | `git://github.com/alphacep/vosk-api.git` | POM `<scm>` |

## 2. License verification

- vosk-android 0.3.75: **Apache-2.0** (POM license block; upstream `vosk-api` LICENSE).
- Model `vosk-model-small-it-0.22`: **Apache-2.0** (Alpha Cephei models page; catalog
  entry `org.vosk.model.small-it-0.22` with `licenseApproved: true`).
- Catalog `licenseUrl` binds `https://github.com/alphacep/vosk-api/blob/master/LICENSE`
  (HTTPS, reviewed host, ModelLinks-validated).

## 3. Model candidate verification (executed 2026-11-04, live)

| Item | Value | Evidence |
| --- | --- | --- |
| Model | `vosk-model-small-it-0.22` (Italian, int8, Kaldi zip) | shipped catalog, VERIFIED_DOWNLOADABLE |
| URL | `https://alphacephei.com/vosk/models/vosk-model-small-it-0.22.zip` | frozen archive URL — **live HEAD-check this session: HTTP 200, `Content-Length: 49665141`, ETag `62cdb2a0-2f5d475`** |
| Size | 49,665,141 bytes | equals the catalog `downloadSizeBytes` exactly (byte-identical) |
| SHA-256 | `9ec65e75861d1c6c2e457cccd932705340dcdf233f5b239f00733b4de0bf3267` | catalog (verified against the downloaded archive 2026-11-02); enforced again at install time by the delivery chain |
| Installed size | 91,299,150 bytes | measured by extraction (catalog provenance) |
| Storage | `filesDir/models/stt/vosk-small-it/` | resolver tolerates the zip's single-level unzip |

## 4. Download / install / activation (implemented, wired)

| Step | Implementation | State |
| --- | --- | --- |
| Download | Model Manager delivery chain (`ModelDownloadManager` + `DownloadGatekeeper` thermal/network/storage gates) for catalog identity `org.vosk.model.small-it-0.22` | IMPLEMENTED |
| Verify | SHA-256 gate before install; mismatch quarantines the payload (no silent install) | IMPLEMENTED |
| Install | unpack under `filesDir/models/stt/vosk-small-it` | IMPLEMENTED |
| Activation | `VoskFrameSttEngine.load()` — resolves the model dir, constructs the vosk `Model` + 16 kHz `Recognizer`, state → READY; `unload()` closes both → UNLOADED | IMPLEMENTED (native path compiled in CI; executed on device in qualification) |
| Consumption | frame seam `AudioFrame (16 kHz PCM) → acceptWaveForm → typed partial/final transcripts`, wired into `VoicePipelineController` + Self-Test Center | IMPLEMENTED |
| Fallback | Android system `SpeechRecognizer` stays the ACTIVE engine until the vosk model is installed and loaded | ACTIVE by default (honest) |

## 5. Transcript generation

- Code path complete and unit-tested (frame → decoder → typed transcript states;
  `VoicePipelineControllerTest`, `VoskFrameSttEngine` honesty states).
- **On-device transcript proof: PENDING XIAOMI QUALIFICATION** — the Xiaomi package
  (§9 of this directive) includes the Italian voice + STT live test that produces a
  real transcript and its log evidence. No transcript is claimed before that run.

## 6. Active runtime read-back (§13)

- `DiagnosticsScreen` → "Active runtime" card (652-key localized set): the STT row
  reads `VoskFrameSttEngine.engineId` live — it names
  `vosk-android 0.3.75 + vosk-model-small-it-0.22` ONLY when the model is actually
  loaded, and `Android system recognizer (offline vosk model not installed)` otherwise.
  The app never claims local STT while the system recognizer runs.

## 7. Honesty boundaries (explicit)

| Not claimed | Why |
| --- | --- |
| LOCAL_STT_ACTIVE | no on-device transcript has been produced yet (no microphone in this sandbox; emulator silent) |
| Any transcript text | invention forbidden; produced only during the Xiaomi session |
| Any RAM/latency figure | measured only in the qualification matrix (LOCAL_MODEL_CANDIDATE_PLAN §5 discipline) |
