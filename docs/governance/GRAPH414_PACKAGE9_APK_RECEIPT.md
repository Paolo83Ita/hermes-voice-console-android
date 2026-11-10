# GRAPH 4.14 — PACKAGE 9 APK RECEIPT (cloud-verified)

Mission: CONTINUE_FROM_LATEST_VERIFIED_REMOTE_AUTHORITY.
Every value below is parsed from the downloaded GitHub Actions artifact of the
authoritative tip — not from commit-message narrative and not from the green
status icon.

## Source of truth

| Item | Value |
| --- | --- |
| Remote tip (read-back verified) | `7fc1c0bc1731e77e65908df061aa08a402ffde66` |
| Cloud build workflow | `android-debug-build` run **#179** (id 42865207716), conclusion **SUCCESS** |
| Artifact | `hermes-voice-console-debug-apk-7fc1c0bc1731e77e65908df061aa08a402ffde66` (176,775,426 B zip) |
| Artifact ZIP integrity | OK (all entries extracted; each APK `unzip -t` OK) |
| Manifest parser | pyaxmlparser (binary AndroidManifest.xml, attribute-bound) |

## Variant identity (attribute-bound from each APK's manifest)

| Field | ARM64-V8A (Owner artifact) | UNIVERSAL | X86_64 TEST |
| --- | --- | --- | --- |
| File | `app-arm64-v8a-debug.apk` | `app-universal-debug.apk` | `app-x86_64-debug.apk` |
| Bytes | 122,639,026 | 252,285,855 | 121,767,608 |
| SHA-256 | `bc79ced64ce6321943514148618ed83e1f188a050f040d686910649ff80190e8` | `e3b4aabe9073adfdbc55879d2c6cbed97804775e1da34a714859f68ab6b0fe18` | `f88ab300d3ffb2d557119e0dc716124e812de4a3f34d9fe75ea482437fda4b6e` |
| Package | com.hermestower.voiceconsole | com.hermestower.voiceconsole | com.hermestower.voiceconsole |
| versionName | 0.3.9-alpha-graph4.14-round2-repairs | 0.3.9-alpha-graph4.14-round2-repairs | 0.3.9-alpha-graph4.14-round2-repairs |
| versionCode | 12 | 12 | 12 |
| App label | Hermes Console | Hermes Console | Hermes Console |
| Build label | OWNER RUNTIME THEME TOWER PACKAGE 9 | OWNER RUNTIME THEME TOWER PACKAGE 9 | OWNER RUNTIME THEME TOWER PACKAGE 9 |
| Build ref | tip 7fc1c0b / run #179 | tip 7fc1c0b / run #179 | tip 7fc1c0b / run #179 |
| ZIP integrity | OK | OK | OK |

Note: the C12 commit message carried the LOCAL build SHAs (arm64 17987c24…,
universal c2b9a482…, x86_64 cea0df92…). The cloud re-compile of the identical
source at the identical tip produces different bytes (ZIP recompression +
build-timestamp stamps — the variance class already decomposed in the C09
receipt: ≤128 B dex stamp deltas). Identity fields are identical everywhere;
the SHAs above are the ones to install and to verify against.

## Packaged runtime inventory — ARM64-V8A (Owner artifact)

All 11 required native libraries are PHYSICALLY present in the final cloud
artifact (17 total entries in `lib/arm64-v8a/`):

| Required | Present |
| --- | --- |
| libllama.so | YES |
| libllama_jni.so | YES |
| libggml.so | YES |
| libggml-base.so | YES |
| libggml-cpu.so | YES |
| libsherpa-onnx-jni.so | YES |
| libsherpa-onnx-c-api.so | YES |
| libsherpa-onnx-cxx-api.so | YES |
| libonnxruntime.so | YES |
| libonnxruntime4j_jni.so | YES |
| libvosk.so | YES |

Additional platform/SDK libs: libc++_shared.so, libbarhopper_v3.so (QR),
libandroidx.graphics.path.so, libimage_processing_util_jni.so,
libsurface_util_jni.so, libjnidispatch.so.

UNIVERSAL: arm64-v8a 17 + armeabi-v7a 11 + x86 11 + x86_64 11 entries.
X86_64 TEST: 11 entries (no llama native — the standing typed
RUNTIME_ABI_LIMITATION, unchanged from Package 7/8).

## Runtime assets inside the ARM64 APK

| Asset class | Entries |
| --- | --- |
| Bundled ONNX runtime models | `assets/models/bundled/embed.onnx`, `gtcrn_simple.onnx`, `hey_jarvis.onnx`, `melspec.onnx` |
| Model catalog / manifests | `assets/models/bundled/bundled_manifest.json`, `assets/models/voice_pack_catalog.json`, `assets/models/fixtures/fixture_manifest.json` |
| Tower 4.14 BALANCED tier (680w) | `tower414_architecture / crown_sign / energy_mask / portal / ring_inner / ring_middle / ring_outer` (.webp) |
| Tower 4.14 LOW tier | `tower414l_architecture / crown_sign / energy_mask / portal / ring_inner / ring_middle / ring_outer` (.webp) |
| Marble light layers | `monumental_l0_sky / l1_temple / l2_colonnade / l7_stairs` (.webp) |
| Locales (resources.arsc configs) | default + it / es / fr / de (5-locale parity pinned by LocalizationParityTest) |

## Qualification state

- Cloud unit tests: **501/501 GREEN** (68 suites, 0 f/e/s — JUnit XML parsed
  from `hermes-voice-console-test-results-7fc1c0b…`).
- Lint: **0 errors**; 73 warnings = the classified 72 + 1 environment-only
  `OldTargetApi` instance (rule classes identical to
  `docs/lint/LINT_WARNING_CLASSIFICATION.md`; 0 new unclassified rules).
- Secret scan (re-run on this continuation): working tree 0, history 0.

OWNER ARTIFACT PREFERENCE: **ARM64-V8A** (`app-arm64-v8a-debug.apk`, SHA-256
`bc79ced6…90e8`) for the POCO X6 Pro / Xiaomi physical retest (round 3).
