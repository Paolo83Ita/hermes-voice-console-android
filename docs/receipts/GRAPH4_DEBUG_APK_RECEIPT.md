# GRAPH 4 DEBUG APK RECEIPT — 0.2.0-debug (converged tip)

Measured from the GitHub Actions artifact, not from a local build. All numbers below were
computed this session from the downloaded artifact bytes.

## Artifact identity

| Field | Value |
| --- | --- |
| Commit | `b1df58a509510e317129a6ab8398d28d41a1f2f1` (Graph 4 converged tip: Xiaomi runbook addendum) |
| Workflow run | 41621955833 (`android-debug-build`, **SUCCESS**) |
| Branch | `graph-4/real-voice-pairing-archive-e2e` |
| Version | versionName `0.2.0-debug`, versionCode 2 (app/build.gradle.kts, minSdk 30, targetSdk 35) |
| File | `app-debug.apk` |
| Size | **165,029,486 bytes** |
| SHA-256 | `9275aa222d75411587eb9058bca370e6a0f4dfdcc32c2e21815d8e3f30ffdaa0` |
| Artifact id | 13224341213 (retention 90 days) |

## Why the size grew from Graph 3 (58,782,349 B → 165,029,486 B)

Measured content breakdown (uncompressed sizes from the APK central directory):

| Component | Uncompressed | Why |
| --- | --- | --- |
| `libonnxruntime.so` × 4 ABIs (20.3/20.3/17.6/12.3 MB) + `libonnxruntime4j_jni.so` × 4 | ≈ 73.7 MB | Real on-device wake-word + denoiser inference (CP-G4-009/G4-N13; ORT 1.20.0 pinned) |
| `libbarhopper_v3.so` × 4 ABIs (6.1/5.9/4.9/3.2 MB) + ML Kit tflite assets | ≈ 20.8 MB | Bundled on-device QR scanner (CP-G4-010 wave 2; no Play Services dependency) |
| 17 `classes*.dex` (35.2 + 21.5 + 6.3 + … MB) | ≈ 65 MB | Compose app + ORT Java bindings + five-language resources + Graph 3/4 screens |
| `assets/models/bundled/*` (4 ONNX artifacts + manifest) + `voice_pack_catalog.json` | ≈ 4.3 MB | Bundled verified delivery payloads (small by design; paola.tar.bz2 81 MB intentionally NOT bundled — receipted network installer remains its path) |

The universal debug APK ships all four ABIs. This is the honest cost of the real inference
stack and the real camera scanner; nothing is duplicated or gratuitous.

## Bundled-asset packaging integrity (re-verified INSIDE this CI-built APK)

Every bundled ONNX artifact extracted from the APK re-hashes exactly against the shipped
`assets/models/bundled/bundled_manifest.json` (same double gate the installer enforces at
delivery time):

| Asset | SHA-256 vs manifest | Size |
| --- | --- | --- |
| melspec.onnx | MATCH (ba2b0e0f…) | 1,087,958 B |
| embed.onnx | MATCH (70d16429…) | 1,326,578 B |
| hey_jarvis.onnx | MATCH (94a13cfe…) | 1,271,370 B |
| gtcrn_simple.onnx | MATCH (e77603ac…) | 535,638 B |

## Packaging strings (five languages)

`app_name` = "Hermes Console" in values, values-it, values-es, values-fr, values-de — the
brand name is intentionally invariant per the Owner §14 terminology anchors; the 75-key UI
catalog is fully localized behind it (LocalizationParityTest GATE-05). No packaging-level
string work is pending.

## Xiaomi sideload note (physical qualification)

The universal APK installs as-is on the Xiaomi 11 Lite 5G NE (arm64-v8a is included).
Optional follow-up, NOT executed: `splits { abi { enable true … } }` or an App Bundle
would cut the arm64-only artifact to roughly one third (~55 MB) at the cost of a changed
artifact layout (4 files instead of 1); deferred as an Owner-optional packaging refinement,
recorded here for the decision. The runbook `docs/INSTALL_XIAOMI.md` (b1df58a addendum)
consumes the single universal artifact and stays valid unchanged.

## Evidence chain

- Run 41621955833 SUCCESS @ b1df58a (build + 242/242 cloud JUnit + lint 0 errors/43 warnings).
- Adjacent-tip builds also SUCCESS: e9abee9 → 41621063944, 32e35e0 → 41621713767.
- Graph 3 authority APK (58,782,349 B, sha256 0639ae6e…) preserved in
  MISSION_GRAPH.yaml `graph3.apk`; this receipt supersedes it only for Graph 4 evidence.
