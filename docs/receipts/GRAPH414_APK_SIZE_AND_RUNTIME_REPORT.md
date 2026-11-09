# GRAPH 4.14 — APK SIZE AND RUNTIME REPORT (§16)

Measured from the FINAL local validation build (this continuation; the CI tip
rebuild carries the authoritative tip-identity artifacts).

## NODE
§16 — APK size truth + classification vs the previous Owner package.

## APK set (versionCode 10, versionName 0.3.7-alpha-graph4.14-runtime-theme-tower, label OWNER RUNTIME THEME TOWER PACKAGE 7)

| Variant | Bytes | SHA-256 | native-code |
| --- | --- | --- | --- |
| arm64-v8a (Owner install THIS) | **124,010,662** | `1c3697a50402e13894f602c7b64bd2713407952693d6eba06e7037ac0c97c593` | arm64-v8a |
| universal | **253,657,491** | `216e95a6a4c4753d1a9f7948f10ec17e1f5955369108335072d45876ef6d8784` | arm64-v8a, armeabi, armeabi-v7a, mips, mips64, x86, x86_64 |
| x86_64 (emulator/E2E lane) | **123,139,244** | `ce62857329fce0c344db5d30b67d29e471c7f86cb9017d7204da013e20c2693e` | x86_64 |

## Composition (arm64, measured from the ZIP)

| Component | Bytes | Share |
| --- | --- | --- |
| DEX (18 dex files) | 66.97 MB | 54.0% |
| native libraries (17 .so) | 49.09 MB | 39.6% |
| — libonnxruntime.so | 17.57 MB | ORT engine (GTCRN/Silero/wake/mel) |
| — libvosk.so | 10.04 MB | STT engine |
| — libsherpa-onnx-{jni,c-api,cxx-api}.so | 9.84 MB | TTS engine |
| — libbarhopper_v3.so | 4.95 MB | QR scanning |
| — libllama.so + libggml-{cpu,base}.so + libggml.so + libllama_jni.so | 4.25 MB | llama.cpp runtime |
| — libc++_shared.so / libonnxruntime4j_jni.so / libjnidispatch.so | 2.39 MB | support |
| bundled models (assets/models/bundled) | 4.23 MB | embed 1.33 / hey_jarvis 1.27 / melspec 1.09 / gtcrn_simple 0.54 |
| ML Kit barcode tflite assets | 0.88 MB | scanner bundle |
| fixtures + catalog | 0.12 MB | STT/voice fixtures |
| Tower visuals (res) | 0.90 MB + arsc 1.34 MB | 14 webp slices (167 KB/97 KB … compressed) |
| other | 0.08 MB | manifest etc. |

## Comparison vs the previous Owner package (Package 6, 4.13)

| Variant | Package 6 (4.13) | Package 7 (4.14) | Δ | Classification |
| --- | --- | --- | --- | --- |
| arm64-v8a | 116,531,319 | 124,010,662 | **+7,479,343 (+6.42%)** | EXPECTED_RUNTIME_GROWTH (llama.cpp native 4.25 MB + llama JVM/KMP classes + sherpa/ORT metadata) |
| universal | 246,178,148 | 253,657,491 | +7,479,343 | EXPECTED_RUNTIME_GROWTH (same delta — universal carries every ABI) |
| x86_64 | 121,247,286 | 123,139,244 | **+1,891,958 (+1.56%)** | EXPECTED_RUNTIME_GROWTH — JVM classes only (the llama AAR ships NO x86_64 native libs; typed RUNTIME_ABI_LIMITATION on the emulator lane) |

- No DERIVED_DUPLICATION: pickFirst deduplicates the ORT copy across
  onnxruntime-android and the sherpa AAR (packaging resolution in
  app/build.gradle.kts).
- No REMOVABLE_UNUSED_ASSET: the rejected 4.13 Tower assets (30 webp) were
  already deleted in d66a523; lint UnusedResources is at 0 for drawables.
- No UNEXPECTED_REGRESSION: growth is accounted for by the integrated
  runtime to the byte-level component table above.
- Rule honored: required native functionality is NEVER removed to shrink the
  package.

## NODE / ACTION / TEST / FILES / EVIDENCE / CHECKPOINT / NEXT
- NODE: §16 size report.
- ACTION: measured the final APK set; classified growth vs Package 6.
- TEST: ZIP integrity (testzip) on all 3 variants; aapt2 badging identity.
- FILES: this report; the three APKs (local build outputs; CI rebuild carries
  the tip identity).
- EVIDENCE: byte counts + SHA-256 above (measured, not asserted).
- CHECKPOINT: §16 DONE.
- NEXT: §17 lint classification → §18 local build receipt → §19 push.
