# POCO_X6_PRO_OWNER_RETEST_PACKAGE_2

- Mission: OWNER PHYSICAL RETEST FAILURE (CP-G4-029) — §12 CURRENT BUILD
  RETEST PACKAGE
- Owner: Paolo Lombisani · Device: POCO X6 Pro (8 GB RAM / 256 GB storage)
- State delivered: **POCO_RETEST_PACKAGE_2_READY**
- Build label: **POCO OWNER REPAIR 1**

## 1. Build identity

| Field | Value |
| --- | --- |
| versionName | `0.3.1-alpha-graph4-owner-repair` |
| versionCode | 4 (strictly greater than the shipped 3; nothing reduced) |
| Git SHA (full) | `1e2701e5769e39d2cd15f8d541e97d33f6eaa25a` |
| Git SHA (short) | `1e2701e` |
| Branch | `graph-4/real-voice-pairing-archive-e2e` |
| Build timestamp | 2026-11-06 15:35 UTC (ISO 8601, build-injected) |
| Workflow run ID | local build — "(local build)" in the panel; the CI run triggered by the push carries its own run ID and is the preferred Owner artifact |
| Package | `com.hermestower.voiceconsole` |
| Unit tests | 310/310 GREEN (`testDebugUnitTest`) |
| Lint | 0 errors (`:app:lintDebug`, abortOnError gate green) |

## 2. Artifacts + SHA-256 (debug-signed, locally built at `1e2701e`)

| Artifact | Size (B) | SHA-256 |
| --- | --- | --- |
| **app-arm64-v8a-debug.apk** (PREFERRED Owner artifact) | 106,742,688 | `dbbdc429753904e13257031807eb5b76fb3d67cbf761f185edd26e669c7b60cc` |
| app-universal-debug.apk | 208,175,543 | `0d207db486788d324b8854e5d4370e2b145b4ebbc01524c8e4015871adbf9241` |
| app-x86_64-debug.apk (emulator lane) | 110,672,232 | `4eafffd3a79617135af3f91ab67da929e098a4a3004b73f970f4347f91ae739c` |

The stamped values are verified INSIDE the binaries (dex contains the full
SHA, the label and the version string; the arm64 APK carries exactly
`lib/arm64-v8a`, the universal carries the multi-ABI set — the in-app variant
derivation will read `ARM64_V8A` / `UNIVERSAL` accordingly).

## 3. Changelog vs the rejected `0.3.0-alpha-graph4` (versionCode 3)

1. **Italian TTS fail-closed** — a non-Italian active voice while Italian is
   requested REFUSES the utterance (`TEXT_ONLY_SAFE_DEGRADED` +
   `ITALIAN_SYSTEM_TTS_DATA_MISSING` / `ITALIAN_SYSTEM_TTS_READBACK_MISMATCH`);
   the response stays visible as text; direct actions open the Android TTS
   settings / the voice-data installer. The English-voice audible fallback
   class is impossible by construction.
2. **Speech profile** — rate 0.96 / pitch 0.95 defaults (comfortable starting
   values; no forced high pitch, no random voice selection), safe ranges
   enforced, persisted and read back after restart; Voice & Audio → Speech
   output with Slower/Recommended/Faster, Lower/Recommended/Higher, collapsed
   advanced sliders, preview phrase, reset.
3. **Verify button (Models and Voice)** — adaptive action layout: Download /
   Verify (filled) / Activate / Details direct; all other actions behind
   "Altre azioni"; ≥48 dp touch height, maxLines 2, centered, wrap at phone
   widths. The FIXED_ROW_ACTION_OVERFLOW class is dead.
4. **Distinct controls (Console)** — mic idle = Mic + "Parla"; listening =
   Stop + "Interrompi ascolto"; privacy mute = ALWAYS MicOff (red-bronze seal
   + persistent "Microfono disattivato" text when engaged); TTS stop =
   VolumeUp + "Interrompi voce". Unique icon/label/contentDescription in
   every state (unit-tested over the full state matrix).
5. **Build identity** — Settings → About and Safety → Build information +
   Diagnostics footer: build-injected version/SHA/timestamp/run ID/label +
   on-device measured APK SHA-256, variant, ABIs, install/update timestamps,
   process start. **Copy build identity** and **Export secret-clean
   diagnostic receipt** actions (About & Safety + Diagnostics).
6. **TTS telemetry + diagnostic card** — the TTS call path records itself
   (engine init → locale → availability → voices → selection → read-back →
   utterance lifecycle; spoken content NEVER logged) and is visible in
   Voice & Audio → Diagnostica TTS Android and in the exported receipt.
7. **ST-51** — real-engine result classes (PASS / BLOCKED_DEPENDENCY / FAIL /
   QUALIFICATION_PENDING_DEVICE on JVM).
8. **Provenance drift removed** — the in-app version is the BuildConfig
   authority (was hardcoded `0.2.0-debug`).

## 4. Known limitations

- Without Italian voice data on the device the app does not invent audio:
  fail-closed + typed reason + text answer + install-data actions (the
  audible-Italian terminality remains a device-side fact for the retest:
  `ITALIAN_VOICE_ACTIVE` requires the POCO to expose an Italian voice).
- The natural local Piper voice stays INSTALLED-BUT-INACTIVE (sherpa-onnx
  runtime gap); the system TTS route is the speaking route.
- Local builds stamp "(local build)" as the workflow run ID; CI artifacts
  carry the run ID (the push triggers the CI build automatically).
- The rate/pitch defaults are STARTING VALUES, not a physical qualification —
  the Owner's ear decides; the presets and sliders are there to converge.

## 5. Owner retest flow

`docs/POCO_OWNER_REPAIR_INSTALL_GUIDE.md` §7 — short and conclusive:
identity → Italian → Voice & Audio records → preview phrase → Verify button
→ distinct medallions → export the secret-clean receipt.

Required physical success state (Owner-confirmed ONLY):
`POCO_ITALIAN_TTS_UI_AND_CONTROLS_RETEST_PASSED`.
