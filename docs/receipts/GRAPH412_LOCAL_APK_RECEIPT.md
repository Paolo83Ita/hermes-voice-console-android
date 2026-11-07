# GRAPH 4.12 — LOCAL VALIDATION APK RECEIPT (build label POCO XIAOMI PHYSICAL REPAIR 4)

**AUTHORITY NOTE**: the local build is the validation artifact. The pushed-commit
CI build (android-debug-build workflow) is the authoritative package — its
SHA-stamped artifact governs. Every physical verdict stays Owner-only.

## Build identity

| Field | Value |
| --- | --- |
| versionName | `0.3.5-alpha-graph4.12-physical-repair` |
| versionCode | `8` (succeeds 0.3.4-alpha-graph4.11 / versionCode 7) |
| Label | POCO XIAOMI PHYSICAL REPAIR 4 |
| Built from | branch `graph-4/real-voice-pairing-archive-e2e`, local commit at build time |
| JDK | Temurin 21.0.13 (jlink present) |
| Kotlin compile | in-process, 1 worker (sandbox memory constraints) |

## Validation APKs (local, sha256)

| Variant | Bytes | SHA-256 |
| --- | --- | --- |
| universal | 245,976,889 | `09f935dc49b16acbaa92ab25e82ba6e454963e2ebe33d33b454ad3f88c869b8f` |
| arm64-v8a (Owner devices) | 116,330,060 | `ec3a0217d687f80d0400641397e5b7f118c07e1511fd97e5b992a9a2a382089a` |
| x86_64 (test) | 121,046,027 | `4409c3ddc9b4e7421b8b95f624bc550d2bce18ddc7d3a9cb89a4096b8fd13878` |

`output-metadata.json` confirms versionCode 8 / versionName
`0.3.5-alpha-graph4.12-physical-repair` for all three variants.

## Gates passed at this build

| Gate | Result |
| --- | --- |
| Unit tests | **450/450 GREEN** (422 prior + 28 new: onboarding contract, unified interrupt, capability ladder, runtime receipts, crash-evidence/safe-mode pins, contrast pins) |
| Lint | **0 errors** (abortOnError; 84 warnings — pre-existing baseline) |
| Secret scan | 0 findings (working tree + full history) |
| Tower assets | 12 original layers shipped (candidate A, balanced 576×792); the 10 rejected layers quarantined in-repo |
| Contrast | inkDim 0xFF8A8375 ≥4.72:1; redBronzeText 0xFFDE7A5E ≥5.96:1; Marble Gold/Energy/Warn/Ok deepened ≥4.5:1 — all pinned by ContrastRatioTest |

## Known limitations (typed)

- `FIRST_RUN_SAFE_MODE` gates TTS auto-init; Vosk/Sherpa auto-load, wake word,
  model download and QR camera auto-start are equally gated by
  `FirstRunSafeMode.mustRefuse...` where the subsystems initialize
  automatically in this build (TTS at graph start). Subsystem-by-subsystem
  initialization is provided by the recovery panel.
- The unified interrupt is wired to the primary console control; the
  composer's per-control stops remain (they are aliases of the same physical
  stops).
- x86_64 APK exists for the emulator E2E lane only.
