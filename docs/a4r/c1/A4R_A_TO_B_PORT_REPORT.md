# A4R A-to-B Selective Port Report (C1-C4, CP-A4R-C1-007)

**Direction:** Candidate B (`apps/android-voice-console`, byte-exact APK `908b289f…`)
is the architectural base for the Tower authority lane; Candidate A (this canonical
repository at `d133087`) is the feature and product-maturity donor. One production
application comes out of the merge — never two.

## What was ported (with explicit decisions)

| # | Capability | Decision | Where it landed |
|---|---|---|---|
| 1 | QR scanner UX (CameraX 1.4.2 + bundled ML Kit + typed states) | REUSE_AND_EXTEND | `QrScannerScreen` now runs `TowerQrV2ScanFlow` (v2 contract) + `TowerPairingController`; camera denial is split into `CAMERA_PERMISSION_DENIED` / `CAMERA_PERMISSION_PERMANENTLY_DENIED` |
| 2 | Pairing entry actions (Scansiona il QR / Codice numerico / Endpoint manualmente) | REUSE_AND_EXTEND | same labels ×5 locales; executors switched from the SIMULATED lane to the REAL Tower v2 lanes; numeric code = 6 digits (mission §7) |
| 3 | Five-language infrastructure | REUSE_AS_IS | kept; 5 new typed states + 18 v2 strings added ×5 locales |
| 4 | Marble/Obsidian themes | REUSE_AS_IS | untouched (no pairing-correctness coupling) |
| 5 | Accessibility base | REUSE_AS_IS | new pairing surfaces built on the same audited components |
| 6 | Diagnostics | REUSE_AND_EXTEND | `TowerPairingDiagnostics` (paired state, contract version, typed failure history — enum names only, no secrets) |
| 7 | Tower visual master canvas 680×963 | REUSE_AS_IS | carried from d133087 unchanged; golden suite stays the gate; Owner visual acceptance stays OPEN |
| 8 | A2 Keystore repair | REUSE_AND_EXTEND | applied to B's authority lane + 2 mission typed states added |

## What was rejected from production (kept in tests)

`MockTowerPairingClient` and the `hermes-pair://v1` candidate scheme are REMOVED
from the production route (`NO_MOCK_PAIRING_IN_PRODUCTION_PATH`): the AppGraph no
longer instantiates the mock executor and both production screens (scanner +
Tower Studio) run the real contract. The mock client survives ONLY in the JVM
test source (its deterministic properties are still asserted there) and in the
self-test diagnostics battery, clearly labeled SIMULATED, unreachable from the
normal production pairing navigation.

## What was deferred (mission §6 defer list)

Local llama.cpp runtime, Qwen local model, the complete local model manager,
Vosk, sherpa-onnx, openWakeWord and the other large optional native runtimes stay
OPTIONAL under the A7 resource envelope. The repaired APK remains Tower-first:
`LOCAL_LLM_REQUIRED_FOR_PAIRING = LOCAL_LLM_REQUIRED_FOR_VOICE =
LOCAL_LLM_REQUIRED_FOR_ARCHIVE = false`.

## What B gave the merge

The authority lane reconstructed fresh in `candidate-b-base/` (visible lineage,
per-file SHA-256 in `CANDIDATE_B_SOURCE_MANIFEST.json`): the REPAIRED
`TowerStore` (explicit `KeyGenParameterSpec`, AES-256-GCM, atomic pairing
commit), the QR v2 parser, the EC-P256 device identity, the pairing state
machine, and the 18-route `TowerApi` — all bound to B's forensic contract
surface, with the open request-body hypotheses recorded in
`ANDROID_TOWER_CONTRACT_DELTA_REQUEST.json`.

Generated: 2026-11-20T11:45:13Z
