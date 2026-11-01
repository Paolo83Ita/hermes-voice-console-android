# VOICE MODEL MANAGER — Graph 2 implementation

Status: IMPLEMENTED (cloud unit-tested); real-model qualification: QUALIFICATION_PENDING_DEVICE.

## 1. What exists in V0.2

A real, reusable Voice Model Manager under `com.hermestower.voiceconsole.models`:

| Contract (GRAPH 2 §4) | Implementation |
| --- | --- |
| VoiceModelCatalog | `VoiceModelCatalog` / `DefaultVoiceModelCatalog` over the shipped manifest `app/src/main/assets/models/voice_pack_catalog.json` |
| VoicePackResolver | `VoicePackResolver` / `DefaultVoicePackResolver` (deterministic compatibility + recommendation) |
| ModelDownloadManager | `DefaultModelDownloadManager` (resumable, pausable, cancellable, hash-gated) |
| ModelManifestVerifier | `DefaultModelManifestVerifier` |
| ModelStorageManager | `DefaultModelStorageManager` (filesDir/models, preflight, atomic install) |
| ModelQualificationRunner | `DefaultModelQualificationRunner` (deterministic local checks) |
| ModelActivationController | `DefaultModelActivationController` (activation only after qualification, read-back) |
| ModelRollbackController | `DefaultModelRollbackController` (identity-based backup, verified restore, removal cleanup) |
| ModelInstallationRegistry | `FileInstallationRegistry` (JSON, atomic replacement, reopen-safe) |
| DeviceResourceReader | `AndroidDeviceResourceReader` (Build/ActivityManager/StatFs/Connectivity/Battery/TTS engines) |
| VoicePackPolicyResolver | `VoicePackPolicyResolver` (per-profile component policy + RAM/storage/thermal gates) |

Component classes managed independently: wake word, VAD, noise suppression, STT, local LLM,
TTS engine, voice. 18 lifecycle states (`ModelInstallState`) enforced through the registry.

## 2. User profiles

- **TOWER_FIRST** — wake word + VAD local; STT/TTS optional; the local LLM is always
  *visible* in the plan but routed **TOWER**.
- **HYBRID_BALANCED** — simple requests local; complex/uncertain/consequential to the Tower.
- **LOCAL_EXPERIMENTAL** — full local pipeline; disabled by default; explicit activation;
  gates: ≥3 GB available RAM, ≥6 GB free storage, thermal status < 4.
- **MANUAL_ADVANCED** — per-component independent selection; no silent activation.

## 3. Registry fields

Every catalog entry carries: component type, identity, display name, version, source URI,
provider, license (+approval flag), format, quantization, supported ABIs, Android minimum
version, download size, installed size, SHA-256, manifest version, storage location, RAM
estimate, latency class, languages, Italian status, known limitations, plus lifecycle
fields (state, active, previous version, rollback availability, last verification,
last qualification) maintained by the registry.

## 4. Download policy (implemented)

Wi-Fi preferred / metered warning surfaced; storage preflight (download + 256 MB reserve);
bounded retry with materially-changed conditions; resumable via HTTP Range; no duplicate
download when the same verified hash exists; no partial-file activation; temp cleanup on
failure; truthful byte-level progress; no model committed to Git.

## 5. What does NOT download

Real-world candidates (whisper-tiny, Silero VAD, RNNoise, Piper, Gemma-2B) are catalog
entries in non-downloadable states: zero-hash entries are structurally valid but blocked
("no verified binary published"), license-unapproved entries blocked (LICENSE_PENDING).
The only downloadable artifact is `org.hermestower.test.probe-artifact` — a deterministic
4 KB probe generated in-process (see `DeterministicTestArtifact`) and served by the local
mock server on 127.0.0.1. Real large-model qualification is a physical-device graph.

## 6. Tests

- JVM: `VoiceModelManagerCoreTest` (10), `ModelDownloadLifecycleTest` (9: happy path,
  hash mismatch, cancel, pause/resume with Range, no-duplicate, rollback+removal,
  tamper rejection, activation refusal, registry persistence).
- Self-Test Center ST-30..ST-40 exercise the same paths in-app.
- Emulator: `E2EModelManagerTest` (E2E-21..30).
