# SOURCE AND LICENSE REGISTRY — GRAPH 4 NODE 1 (CP-G4-003)

Verification date: **2026-11-02** (sandbox, byte-level). Evidence method per row: the asset was
downloaded from the authoritative upstream and SHA-256-hashed locally, or the SHA-256 was read from
the authoritative LFS metadata API of a revision-pinned source (Qwen GGUF). **No URL, size or hash in
the shipped catalog is guessed.** Entries that could not be verified are NOT downloadable
(`CANDIDATE_PENDING_DISCOVERY`, `unresolved://` URI, download blocked in code).

Verified upstream releases consulted (authoritative, via GitHub/HuggingFace APIs):

- snakers4/silero-vad — tag `v5.1.2` (git tree object verified)
- dscripka/openWakeWord — release `v0.5.1` (v0.6.0 ships **no** release assets → v0.5.1 is the
  newest VERIFIED release; recorded as the revision binding)
- k2-fsa/sherpa-onnx — release tag `tts-models` (Italian VITS/piper bundles) and tag
  `speech-enhancement-models` (GTCRN denoiser)
- alphacephei.com/vosk/models — `vosk-model-small-it-0.22.zip` (frozen model archive URL)
- huggingface.co/Qwen/Qwen2.5-0.5B-Instruct-GGUF — commit `9217f5db79a29953eb74d5343926648285ec7e67`

## Verified entries (production)

| Component | Identity | Asset (revision binding) | Size (B) | SHA-256 | License | Runtime requirement |
| --- | --- | --- | --- | --- | --- | --- |
| VAD | `org.silero.vad.v5` | `silero_vad.onnx` @ tag v5.1.2 | 2,327,524 | `2623a2953f6ff3d2c1e61740c6cdb7168133479b267dfef114a4a3cc5bdd788f` | MIT | onnxruntime-android |
| NOISE_SUPPRESSION (optional) | `org.gtcrn.speech-enhancement.simple` | `gtcrn_simple.onnx` @ tag speech-enhancement-models | 535,638 | `e77603ac0c23dac3227dd2d7135b3a585cbee2679048aecfa886657d3ae1b534` | MIT (upstream Xiaobin-Rong/gtcrn, license read from GitHub API) | sherpa-onnx denoiser |
| SPEECH_TO_TEXT | `org.vosk.model.small-it-0.22` | `vosk-model-small-it-0.22.zip` | 49,665,141 (installed 91,299,150 measured) | `9ec65e75861d1c6c2e457cccd932705340dcdf233f5b239f00733b4de0bf3267` | Apache-2.0 | com.alphacephei:vosk-android |
| TTS engine (RUNTIME_BUNDLED) | `org.sherpa-onnx.tts.runtime` | Gradle dependency (code, no download) | — | — | Apache-2.0 | bundled |
| VOICE (recommended) | `org.piper.voice.it-riccardo-x-low` | `vits-piper-it_IT-riccardo-x_low.tar.bz2` @ tag tts-models | 26,496,614 (installed 38,575,555 measured) | `8ad0791558ae2b3e96054fc5756ae3ae2f6457fb1765bc01f43bbab85e0a7021` | MIT (piper voices) | sherpa-onnx TTS |
| VOICE (alternative) | `org.piper.voice.it-paola-medium` | `vits-piper-it_IT-paola-medium.tar.bz2` @ tag tts-models | 67,221,173 (installed 81,153,749 measured) | `7541f75778afa164e44e34baaef63befad7698595df26a95ca944b63ef1a16b4` | MIT (piper voices) | sherpa-onnx TTS |
| WAKE_WORD deps (pipeline fixtures) | `org.openwakeword.model.melspectrogram` / `…embedding` | `melspectrogram.onnx` / `embedding_model.onnx` @ release v0.5.1 | 1,087,958 / 1,326,578 | `ba2b0e0f8b7b875369a2c89cb13360ff53bac436f2895cced9f479fa65eb176f` / `70d164290c1d095d1d4ee149bc5e00543250a7316b59f31d056cff7bd3075c1f` | Apache-2.0 | onnxruntime-android |
| WAKE_WORD (English fixture) | `org.openwakeword.wakeword.hey-jarvis` | `hey_jarvis_v0.1.onnx` @ release v0.5.1 | 1,271,370 | `94a13cfe60075b132f6a472e7e462e8123ee70861bc3fb58434a73712ee0d2cb` | Apache-2.0 | onnxruntime-android |
| LOCAL_LLM (optional) | `org.qwen.qwen2.5-0.5b-instruct.q5-k-m` | `qwen2.5-0.5b-instruct-q5_k_m.gguf` @ HF commit 9217f5db… (revision-bound resolve URL) | 522,186,592 | `041474553fcabfc2a2d67903f9d2c2e50bd92528e670da4f33b5d0ce6e59fd55` (LFS oid) | Apache-2.0 | llama.cpp Android runtime — NOT integrated yet → activation blocked (RUNTIME_UNAVAILABLE) |

## Non-downloadable / pending entries

| Component | Identity | Classification | Reason |
| --- | --- | --- | --- |
| WAKE_WORD | `org.hermestower.wakeword.hermes-it` | CANDIDATE_PENDING_DISCOVERY | No verified Italian "Hermes" wake model exists upstream; a custom openWakeWord-compatible model must be trained and false-accept/false-reject tested on the Xiaomi. Download refused by `CatalogIntegrity` + resolver gate. |
| WAKE_WORD | `org.hermestower.test.probe-artifact` | TEST_FIXTURE_ONLY | Deterministic 4 KB probe; download pipeline qualification only; never a production component. |

## Superseded / rejected (old placeholder catalog at `ca8980b`)

| Old identity | Disposition | Reason |
| --- | --- | --- |
| `org.openwakeword.noah.it` | SUPERSEDED | Placeholder URL + zero hash; no such verified model exists upstream. Replaced by the openWakeWord engine-dep trio + pending custom Hermes-IT entry. |
| `org.silero.vad.tiny` | SUPERSEDED by `org.silero.vad.v5` | Placeholder URL; replaced by the byte-verified v5.1.2 asset. |
| `org.whisper.cpp.tiny.it` | SUPERSEDED | Placeholder; Vosk small-it verified first per the compact-Italian policy. A whisper.cpp alternative may be evaluated later only with a verified source. |
| `org.hermestower.llm.gemma-2b-it-q4` | REJECTED as default | Mission instruction: do not use Gemma 2B as default merely because it exists in the old placeholder catalog; Qwen2.5-0.5B verified instead (smaller, Apache-2.0, revision-pinned). |
| `org.piper.tts.it-IT-riccardo-medium` (engine) | SUPERSEDED | Split honestly: engine code (sherpa-onnx, RUNTIME_BUNDLED) vs voice bundles (downloadable, hash-pinned). |
| `org.rnnoise.rnn.classic` | SUPERSEDED by `org.gtcrn.speech-enhancement.simple` | Placeholder; GTCRN verified with upstream license evidence. |
| `org.piper.voice.riccardo-medium` | SUPERSEDED by `org.piper.voice.it-riccardo-x-low` (+ paola alternative) | Placeholder; real bundles verified from the tts-models release. |

## License notes

- All verified licenses permit commercial use and redistribution (MIT / Apache-2.0).
- Attribution: LICENSE texts for silero-vad, openWakeWord, vosk-api, sherpa-onnx, piper-voices,
  GTCRN are linked per-entry through `licenseUrl`; the About screen lists them (NODE 2 UI).
- RAM estimates are ENGINEERING ESTIMATES pending physical measurement (marked in
  `knownLimitations`); sizes and hashes are MEASURED/READ facts.

## Enforcement (code)

- `CatalogIntegrity.audit()` — production entries: https-only, no placeholders, no zero hash,
  revision-bound source, approved license, ABI/API match; ships-tested against the REAL asset
  manifest (`CatalogIntegrityTest.shipped production catalog audits clean`).
- `DefaultVoicePackResolver` — download gate additionally requires
  `entryClassification == VERIFIED_DOWNLOADABLE` and a non-blank hash.
