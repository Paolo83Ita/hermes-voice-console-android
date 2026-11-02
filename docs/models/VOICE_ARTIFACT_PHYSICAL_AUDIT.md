# VOICE ARTIFACT PHYSICAL AUDIT — GRAPH 4 CP-G4-008 (Owner §7)

Audit date: 2026-11-03 · Method: byte-level read of the physical workspace files, ONNX proto
parse (`onnx` 1.23.2), archive extraction, license read at the authoritative source.
Scope: the five artifacts physically present in the sandbox workspace. The remaining catalog
entries (Silero VAD v5.1.2, vosk small-it-0.22, riccardo-x-low, Qwen2.5-0.5B GGUF) were
byte-verified at download time in CP-G4-003 and are cross-referenced, not re-audited here.

**Verdict summary: all five artifacts are genuine, parseable, runtime-compatible assets. A
correct hash alone is NOT treated as sufficient (Owner §7): each row below records tensor
signatures, required preprocessing/postprocessing and the Android runtime binding.**

## 1. melspec.onnx — audio → log-mel front-end (openWakeWord pipeline)

| Field | Verified value |
| --- | --- |
| Canonical identity | `org.openwakeword.model.melspectrogram` (registry CP-G4-003) |
| Physical size | 1,087,958 B |
| SHA-256 | `ba2b0e0f8b7b875369a2c89cb13360ff53bac436f2895cced9f479fa65eb176f` (matches registry) |
| Magic bytes | ONNX protobuf wire start `08 07 12 07` (ir_version 7) — valid |
| ONNX parse | OK — 29 nodes, 6 initializers (1,085,580 B params), producer pytorch 1.11.0, opset 13 |
| Origin / revision | dscripka/openWakeWord release `v0.5.1` (newest VERIFIED release; v0.6.0 ships no assets) |
| License | Apache-2.0 |
| Inputs | `input` float32 [batch_size, samples] — RAW waveform |
| Outputs | `output` float32 [time, 1, Clip, 32] — 32-bin log-mel |
| Numerical type | float32 throughout |
| Preprocessing | PCM16 mono @16 kHz → float32 normalized to [-1, 1] |
| Postprocessing | slice into 76-frame windows → reshape [1, 76, 32, 1] for the embedding model |
| Required sample rate | 16,000 Hz (matches the CP-G4-006 AudioRecord source format exactly) |
| Android ABI compatibility | pure ONNX, CPU EP (Conv/MatMul) — all ABIs incl. arm64-v8a |
| Android runtime | onnxruntime-android (pinned via BOM-managed dependency) |
| Estimated memory | ~3–6 MB RSS (weights 1.1 MB + activations) — TARGET_PENDING_DEVICE_QUALIFICATION |
| Fallback | none needed (pure function); if ORT fails to load → wake word stays OFF (typed) |

## 2. embed.onnx — mel-window → 96-dim embedding (openWakeWord pipeline)

| Field | Verified value |
| --- | --- |
| Canonical identity | `org.openwakeword.model.embedding` (registry CP-G4-003) |
| Physical size | 1,326,578 B |
| SHA-256 | `70d164290c1d095d1d4ee149bc5e00543250a7316b59f31d056cff7bd3075c1f` (matches registry) |
| Magic bytes | ONNX `08 07 12 07` (ir 7) — valid |
| ONNX parse | OK — 65 nodes, 42 initializers (1,312,580 B), producer **tf2onnx 1.12.1**, opsets 13 + ai.onnx.ml 2 |
| Origin / revision | dscripka/openWakeWord release `v0.5.1` |
| License | Apache-2.0 |
| Inputs | `input_1` float32 [unk(batch), 76, 32, 1] — 76 mel frames × 32 bins |
| Outputs | `conv2d_19` float32 [unk, 1, 1, 96] — 96-dim embedding |
| Numerical type | float32 (Conv/LeakyRelu/Max/MaxPool only — CPU-friendly) |
| Preprocessing | melspec.onnx output windowed to 76×32 |
| Postprocessing | squeeze to 96-dim; sliding buffer of the LAST 16 embeddings feeds the wake head |
| Required sample rate | 16,000 Hz (inherited via melspec) |
| Android ABI / runtime | onnxruntime-android CPU — all ABIs |
| Estimated memory | ~4–8 MB RSS — TARGET_PENDING_DEVICE_QUALIFICATION |
| Fallback | wake pipeline OFF with typed error if either front-end model fails verification |

## 3. hey_jarvis.onnx — wake-word head (English fixture; Italian Hermes pending discovery)

| Field | Verified value |
| --- | --- |
| Canonical identity | `org.openwakeword.wakeword.hey-jarvis` (registry CP-G4-003) |
| Physical size | 1,271,370 B |
| SHA-256 | `94a13cfe60075b132f6a472e7e462e8123ee70861bc3fb58434a73712ee0d2cb` (matches registry) |
| Magic bytes | ONNX `08 07 12 07` (ir 7) — valid |
| ONNX parse | OK — 33 nodes, 20 initializers (1,266,952 B), producer pytorch 1.12.1, opset 13 |
| Origin / revision | dscripka/openWakeWord release `v0.5.1` (`hey_jarvis_v0.1.onnx`) |
| License | Apache-2.0 |
| Inputs | `x.1` float32 [1, 16, 96] — 16 embeddings × 96 dims |
| Outputs | `53` float32 [1, 1] — sigmoid wake score (0..1), head ends Gemm→Sigmoid (verified) |
| Numerical type | float32 |
| Preprocessing | melspec → embed → 16-embedding sliding window |
| Postprocessing | threshold (openWakeWord default 0.5; final value fixed only after false-accept/false-reject harness on the Xiaomi) + duplicate suppression + cooldown |
| Required sample rate | 16,000 Hz |
| Android ABI / runtime | onnxruntime-android CPU — all ABIs |
| Estimated memory | ~2–4 MB RSS — TARGET_PENDING_DEVICE_QUALIFICATION |
| Fallback / status | wake word stays **OFF by default** until the FA/FR harness passes on device (Owner §8/§9); English fixture is not an Italian wake word — the custom IT model remains `CANDIDATE_PENDING_DISCOVERY` (honest gap, download refused in code) |

## 4. gtcrn_simple.onnx — streaming speech enhancement (noise suppression)

| Field | Verified value |
| --- | --- |
| Canonical identity | `org.gtcrn.speech-enhancement.simple` (registry CP-G4-003) |
| Physical size | 535,638 B |
| SHA-256 | `e77603ac0c23dac3227dd2d7135b3a585cbee2679048aecfa886657d3ae1b534` (matches registry) |
| Magic bytes | ONNX `08 06 12 07` (ir 6) — valid |
| ONNX parse | OK — 415 nodes, 214 initializers (467,512 B), producer pytorch 1.11.0, opset 11 |
| Origin / revision | k2-fsa/sherpa-onnx tag `speech-enhancement-models` (upstream Xiaobin-Rong/gtcrn, MIT) |
| License | MIT |
| Inputs | `mix` float32 [1, 257, 1, 2] (complex STFT, 257 bins = 512-point FFT) + 3 streaming cache inputs: `conv_cache` [2,1,16,16,33], `tra_cache` [2,3,1,1,16], `inter_cache` [2,1,33,16] |
| Outputs | `enh` float32 [1, 257, 1, 2] + `conv_cache_out`/`tra_cache_out`/`inter_cache_out` (stateful streaming) |
| Numerical type | float32; GRU ×14, ScatterND ×18 (cache updates) — all supported by onnxruntime CPU EP |
| Preprocessing | STFT: 512 window / 256 hop, Hann, 16 kHz mono; frame = [257 bins × real/imag] |
| Postprocessing | iSTFT + overlap-add → PCM16; caches fed back frame-to-frame |
| Required sample rate | 16,000 Hz |
| Android ABI / runtime | onnxruntime-android CPU — all ABIs |
| Latency | algorithmic 32 ms (512 FFT @ 16 kHz) + inference; measured on-device: TARGET_PENDING_DEVICE_QUALIFICATION |
| Estimated memory | ~3–6 MB RSS — TARGET_PENDING_DEVICE_QUALIFICATION |
| Fallback | feature-flagged; typed failure → **raw audio passthrough** (Owner §9); bypass always available |

## 5. Paola voice bundle — piper/VITS Italian TTS voice

| Field | Verified value |
| --- | --- |
| Canonical identity | `org.piper.voice.it-paola-medium` (registry CP-G4-003) |
| Archive | `paola.tar.bz2`, 67,221,173 B, SHA-256 `7541f75778afa164e44e34baaef63befad7698595df26a95ca944b63ef1a16b4` (matches registry) |
| Extracted installed size | **81,153,749 B measured** this session (matches registry's measured value exactly) |
| Payload | `vits-piper-it_IT-paola-medium/`: `it_IT-paola-medium.onnx` (63,153,775 B, SHA-256 `477ab68d8021543decc9e067d12c5fddbc777d81b7b221e233d3c14719a1a6b9`), `.onnx.json` config, `tokens.txt` (256 symbols), `espeak-ng-data/` (19 MB), MODEL_CARD |
| Magic bytes | ONNX `08 …` valid proto; archive is a real bzip2 tar (extracted cleanly) |
| ONNX parse | OK — 2,288 nodes, producer pytorch 1.13.1 (VITS) |
| Inputs | `input` int64 [batch, phonemes]; `input_lengths` int64 [batch]; `scales` float32 [3] (noise 0.8, length 1.0, noise_w 0.8 from config) |
| Outputs | `output` float32 [batch, 1, 1, time] — PCM waveform |
| Numerical type | float32 audio out (denormalized to int16 for playback) |
| Preprocessing | text → espeak-ng phonemization (`espeak.voice = "it"`, bundled espeak-ng-data) → phoneme IDs via tokens.txt |
| Postprocessing | output float → int16 PCM @ **22,050 Hz** (config `audio.sample_rate`) — playback must use a 22.05 kHz AudioTrack; 16 kHz capture chain unaffected |
| License | MODEL_CARD points to the dataset license — **CC0-1.0 verified this session at huggingface.co/datasets/paolapersico1/Voice-Dataset-Italian** (registry also carries the piper-voices MIT attribution note; both are recorded — CC0 dataset + piper packaging. Resolution: usable, attribution listed in About) |
| Origin / revision | k2-fsa/sherpa-onnx tag `tts-models` (immutable release asset) |
| Android ABI / runtime | sherpa-onnx TTS runtime (Apache-2.0, Gradle-bundled engine) + onnxruntime |
| Estimated memory | ~150–250 MB RSS during synthesis (medium voice) — TARGET_PENDING_DEVICE_QUALIFICATION; the x-low riccardo voice remains the RECOMMENDED default for the Xiaomi |
| Fallback chain | 1. qualified downloaded local voice → 2. Android offline system TTS → 3. text-only (Owner §9) — chain already implemented in CP-G4-005/006 (AndroidSystemTtsEngine = tier 2) |
| Activation rule | **ACTIVE only after actual audio is generated on device** (Owner §9); catalog status stays `CANDIDATE_PENDING_DEVICE_QUALIFICATION` until the Xiaomi run |

## 6. Cross-cutting findings

1. **No fake runtime metrics**: every memory/latency number above is an estimate explicitly
   marked `TARGET_PENDING_DEVICE_QUALIFICATION` (Owner §0/§8 rule respected).
2. **16 kHz coherence**: AudioRecord source (CP-G4-006), melspec, embed, hey_jarvis and GTCRN
   all require exactly 16,000 Hz mono — the capture pipeline is format-coherent end to end.
   Only the TTS output differs (22,050 Hz) and lives on the playback path.
3. **Pipeline wiring order** (wake): mic → VAD (energy; Silero when delivered) → [optional
   GTCRN] → melspec → embed → hey_jarvis head. Every stage has a typed failure path and the
   wake feature defaults to OFF.
4. **Registry integrity**: all five SHA-256 values match `SOURCE_AND_LICENSE_REGISTRY.md`
   byte-for-byte — no drift between workspace artifacts and shipped catalog.
5. **License hygiene**: MIT / Apache-2.0 / CC0-1.0 only; attribution list already surfaced in
   the About screen (NODE 2 UI contract).
6. **Android usability verdict**: all four ONNX graphs use CPU-EP-supported operator sets
   (largest risk class — ScatterND/GRU in GTCRN — is ORT-supported); the VITS voice requires
   the sherpa-onnx runtime, which is already the bundled engine dependency.
