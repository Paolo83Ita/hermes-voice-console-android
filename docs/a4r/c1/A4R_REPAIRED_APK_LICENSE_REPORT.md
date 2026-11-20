# License report — GRAPH A4R merged B-base repair (C1-C4)

Generated 2026-11-20T12:15:54Z. The full component inventory with licenses is
`A4R_REPAIRED_APK_BOM.json` / `A4R_REPAIRED_APK_SBOM.json` (same directory).

- Production runtime: Apache-2.0 (AndroidX/Compose/CameraX/Vosk/sherpa-onnx),
  MIT (ONNX Runtime, llama-android), ML Kit bundled on-device terms.
- Test-only: EPL-1.0 (JUnit), Apache-2.0 (AndroidX test, coroutines-test),
  Public Domain (org.json:json — TEST CLASSPATH ONLY).
- First-party: this repository is Apache-2.0; the candidate-b-base authority
  lane inherits the repository license.
- The Owner release keystore is NOT in the repository and is never committed.
- No copyleft license was introduced by this block; no new production
  dependency was added (the org.json artifact is test-scope only).

Secret-clean guarantee: the debug CI signing key is CI-managed and
non-production; no private key, token or challenge material is committed
(ZERO_SECRET_LEAK gate in the build workflow).
