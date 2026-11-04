# APK SIZE AND ABI REPORT — Graph 4.6 (§8)

Directive: GRAPH 4.6 §8 — "Produce UNIVERSAL DEBUG APK and ARM64-V8A DEBUG APK for
Xiaomi; break down DEX / ONNX Runtime / ML Kit / resources / bundled models / assets;
calculate SHA-256 for both."

Source: GitHub Actions run **41884838651** (`android-debug-build`, **SUCCESS**),
commit `c97a02f` (Graph 4.6 tip: CP-G4-021 splits + CP-G4-022/023 product and gate
waves + fix 1), artifact
`hermes-voice-console-debug-apk-c97a02f5c25cc2ecaf97d3c8b1d3ce7c8285b405`
(artifact id 13351081998). Every number below was computed THIS SESSION from the
downloaded artifact bytes (SHA-256 + zip central directory), not from the build log.
Companion evidence at the same commit: **260/260 unit tests green** (artifact
13350398668) and **lint 0 errors / 60 warnings** (artifact 13351056892).

## 1. Artifact identity

| Field | Value |
| --- | --- |
| Commit | `c97a02f` (branch `graph-4/real-voice-pairing-archive-e2e`, Graph 4.6 tip at build time) |
| Version | versionName `0.2.0-debug`, versionCode 2, minSdk 30, targetSdk 35 |
| Split config | `splits.abi { isEnable = true; isUniversalApk = true; include("arm64-v8a", "x86_64") }` — debug-only qualification, no versionCode offsets |

## 2. The three APKs — size and SHA-256 (measured from the artifact)

| APK | Bytes | MiB | SHA-256 |
| --- | --- | --- | --- |
| **app-arm64-v8a-debug.apk** (THE XIAOMI APK) | **105,247,044** | 100.4 | `f2f38b612d8d7a3eba3b0968669d99a7bd636af4050b3c7f9eebf659faaafb62` |
| app-universal-debug.apk (all ABIs; CI + emulator + fallback) | 206,679,899 | 197.1 | `97998108ce300527096ef4f9fed15415d2e98faff2d6878a6f95b65644e93c72` |
| app-x86_64-debug.apk (emulator/CI companion) | 109,176,588 | 104.1 | `7c4de490f4295d66a9ba1a5530b898b7560b69b73d81b26d385336d84d66e115` |

The arm64-v8a split removes ~101 MB (~49%) versus the universal APK — this is the
artifact the Xiaomi qualification installs (XIAOMI_GRAPH4_BETA_PACKAGE §2).

## 3. Content breakdown (uncompressed, from the zip central directory)

### app-arm64-v8a-debug.apk (uncompressed total 106,313,040 B)

| Component | Uncompressed bytes | Why |
| --- | --- | --- |
| DEX (17 classes*.dex) | 65,881,548 | debug build, no minify (isMinifyEnabled=false by design for traceable debug stacks): Compose app + ORT Java bindings + five-locale resources + all screens |
| ONNX Runtime (libonnxruntime.so + JNI, arm64-v8a only) | 18,487,128 | real on-device wake-word + GTCRN denoiser inference (ORT 1.20.0 pinned) |
| vosk-android + JNA natives (libvosk.so 10,042,800 B + libjnidispatch.so) | 10,219,320 | REAL offline Italian STT runtime (CP-G4-019 §14; vosk-android 0.3.75) |
| ML Kit / barhopper (libbarhopper_v3.so + tflite assets of the scanner) | 4,946,720 | bundled on-device QR scanner |
| Other native (graphics.path, surface/image utils) | 220,456 | AndroidX/MLkit helpers |
| resources.arsc | 1,217,680 | 652 string keys × 5 locales + plurals |
| res/ (drawables etc.) | 309,013 | launcher/icon/theme resources |
| assets/models/bundled/ + voice_pack_catalog.json | 4,225,143 | 4 verified ONNX artifacts + manifest (paola.tar.bz2 intentionally NOT bundled — network installer path) |
| assets/ other | 902,975 | app assets |
| other (META-INF, manifest) | 79,577 | signing/metadata |

### app-universal-debug.apk (uncompressed total 207,473,200 B)

| Component | Uncompressed bytes | Delta vs arm64 (why the universal is ~2×) |
| --- | --- | --- |
| DEX | 65,881,548 | identical |
| ONNX Runtime | 73,776,280 | +55.3 MB — the same .so duplicated ×4 ABIs |
| vosk-android + JNA natives | 39,761,100 | +29.5 MB — libvosk.so ×4 ABIs (+ JNA's legacy mips/mips64 jnidispatch, ~0.3 MB dead weight from the JNA AAR itself) |
| ML Kit / barhopper | 20,222,808 | +15.3 MB — ×4 ABIs |
| Other native | 1,152,188 | ×4 ABIs |
| resources.arsc + res/ | 1,526,693 | identical |
| assets (bundled models + other) | 5,128,118 | identical |
| other | 79,577 | identical |

## 4. Why the universal APK grew from 165.0 MB (b1df58a) to 206.6 MB (7d1f8cf)

One cause, measured: the REAL offline STT runtime (CP-G4-019) added
`vosk-android 0.3.75` + `jna 5.18.1` — libvosk.so ×4 ABIs (8.99–10.40 MB each,
39.76 MB total uncompressed with JNA). Nothing else moved materially. This is the
honest cost of real on-device Italian speech recognition, and the arm64 split now
contains it to 10.2 MB for the Xiaomi.

## 5. Honesty boundaries

- These are DEBUG APKs (no minify/shrink) — sizes are qualification sizes, not
  release-shipping sizes; release sizing is out of scope for Graph 4.6.
- The five locale tables ride in resources.arsc/DEX and are NOT the size driver
  (≈1.5 MB total) — dropping locales would not meaningfully shrink the APK.
- SHA-256 values bind to artifact id 13351081998 @ commit c97a02f; any rebuild
  produces different bytes (debug builds are not reproducible byte-for-byte).
- The earlier CP-G4-021 verification build (7d1f8cf, run 41881238768, artifact
  13349138041: arm64 105,188,716 B / sha256 71ecdbdd…) preceded only a one-line
  test-source fix; both builds carry identical app behavior.
