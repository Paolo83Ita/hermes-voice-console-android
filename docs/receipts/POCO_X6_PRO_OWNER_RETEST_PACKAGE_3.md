# POCO_X6_PRO_OWNER_RETEST_PACKAGE_3

- Mission: GRAPH 4.9 (CP-G4.9-010) — local-models delivery node
- Owner: Paolo Lombisani · Device: POCO X6 Pro (8 GB RAM / 256 GB storage)
- State: **POCO_RETEST_PACKAGE_3_READY** (implementation-complete, physical
  qualification pending — no physical claim)
- Preserves: `POCO_X6_PRO_OWNER_RETEST_PACKAGE_2` (0.3.1/vc4, dbbdc429…b60cc)
  — this package exists because the source CHANGED MATERIALLY after package 2.

## 1. Build identity

| Field | Value |
| --- | --- |
| versionName | `0.3.2-alpha-graph4.9-local-models` |
| versionCode | 5 (strictly greater than the in-flight 4; Android can update-install over it) |
| Git SHA (full) | `bae9b928af43c0209420daefc51743782e028d01` |
| Git SHA (short) | `bae9b92` |
| Branch | `graph-4/real-voice-pairing-archive-e2e` |
| Build timestamp | 2026-11-06 21:18 UTC (build-injected, ISO 8601) |
| Workflow run ID | "(local build)" — the CI run triggered by the same commit stamps its own run ID and is the preferred fully-stamped artifact |
| Build label | `GRAPH 4.9 LOCAL MODELS 1` |
| Package | `com.hermestower.voiceconsole` |
| Unit tests | 335/335 GREEN (`testDebugUnitTest`) |
| Lint | 0 errors (`:app:lintDebug`, abortOnError gate) |
| Stamps verified INSIDE the binaries | dex carries the version string + build identity; arm64 APK carries exactly the `lib/arm64-v8a/` set incl. `libsherpa-onnx-jni.so` |

## 2. Artifacts + SHA-256 (debug-signed, locally built at `bae9b92`)

| Artifact | Size (B) | SHA-256 |
| --- | --- | --- |
| **app-arm64-v8a-debug.apk** (PREFERRED Owner artifact for the POCO X6 Pro) | 115,795,166 | `d90352481d34d71e79874c6ccf094710f4686ba552bf0d286a2dade98abc6b0a` |
| app-universal-debug.apk | 245,441,995 | `db1fd448a166f0c372839f8a06cf47c56b7a187627882fbecf7410680c5fa3ca` |
| app-x86_64-debug.apk (emulator lane) | 120,511,133 | `f349f341a9a73a71f61adf06fc542036cdf295d2126d8e4f08625b9ab2d761b4` |

## 3. Changelog vs package 2 (0.3.1/vc4, the build the Owner is testing)

1. **Natural local Italian TTS route (CP-G4.9-003)** — sherpa-onnx v1.12.24
   (verified AAR) + piper `it-riccardo-x_low` / `it-paola-medium` (verified
   bundles) in the Model Sanctum: Download → Verify → Install → **Hear
   sample** → Activate. While a piper voice is ACTIVE the console speaks
   through it (real offline neural voice); the Android system engine remains
   the mandatory fallback. Speech profile rate/pitch applies to BOTH routes.
2. **Cancel regression repaired (CP-G4.9-001b)** — "Interrompi voce" during
   the engine's start window could be swallowed (the e2e_14 root cause); an
   interruption token now wins in every window.
3. **Local STT test path (CP-G4.9-004)** — Models and Voice → Speech-to-text
   → **Test**: real Italian speech (the mission phrase, same verified piper
   voice) is transcribed OFFLINE by vosk on the device; the transcript shown
   is what the engine produced.
4. **Optional local LLM (CP-G4.9-005)** — the Qwen2.5-0.5B artifact is
   verified at the pinned revision (hash-exact); the runtime stays honestly
   typed-blocked until a verified Android llama.cpp runtime is adopted.
5. **Tower Archive completion (CP-G4.9-007)** — artifacts carry MIME,
   provenance, SIMULATED previews; download/pause/resume/re-download/verify
   typed lifecycle; 6 actions on every artifact row.
6. **Self-Test expansion (CP-G4.9-009)** — ST-52 (natural TTS route) and
   ST-53 (offline STT route) with honest device-pending verdicts.
7. **Build identity** — versionCode/label updated; provenance panel
   unchanged in behavior (values are build-injected + on-device measured).

## 4. Physical validation notes for this package (PENDING — honest gaps)

- The natural-voice route merges ONE `libonnxruntime.so` (the project's
  1.20.0) shared by the wake-word chain and sherpa-onnx (stable ORT C API).
  **Must be exercised on the POCO**: Self-Test → ST-52 + the voice Hear
  sample — if the merged runtime misbehaves on device the fallback is typed
  (system engine), never a crash-by-design.
- TalkBack walk (control semantics) and the 1.50-font physical corner matrix
  remain device-lane items (see the two qualification reports).

## 5. Owner retest flow (unchanged pattern)

1. Install the arm64-v8a APK over the previous install (signature identical:
   debug-signed; versionCode 5 > 4 → direct update, data preserved).
   If Android refuses (signature mismatch with the OLD 0.2.0 Graph-3 build),
   see `docs/POCO_OWNER_REPAIR_INSTALL_GUIDE.md` §signature (uninstall first;
   local app data is cleared — expected and harmless for a beta).
2. Settings → About and Safety → **Build information**: confirm versionName
   `0.3.2-alpha-graph4.9-local-models`, Git SHA `bae9b92…`, label
   `GRAPH 4.9 LOCAL MODELS 1`, and the measured APK SHA-256
   `d9035248…c6b0a` (Copy build identity → paste into the retest report).
3. Console: ask something → the reply must be ITALIAN, comfortable pitch
   (0.95), rate 0.96; adjust in Voice & Audio if desired.
4. Voice & Audio → Diagnostica TTS: confirm the negotiation read-back rows.
5. Models and Voice → the piper voice → Hear sample / Activate; then
   Self-Test → ST-52 (natural route) and ST-53 (STT route).
6. Verify button in Models and Voice + the distinct mic/mute/TTS-stop
   controls (the CP-G4-029 items) re-confirmed on this build.
7. Export the secret-clean diagnostic receipt and attach it to the verdict.
