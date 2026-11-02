# MODEL SANCTUM — Models and Voice (Graph 4 §6/§7)

Status: IMPLEMENTED (CP-G4-004) · Supersedes the Graph 2 "Voice Model Manager" screen layout; engine behavior preserved and extended.

## 1. What this is

MODEL SANCTUM is the refounded Model Delivery Center: a live window onto the real
model delivery engine, not a poster. The Graph 2 screen (sections A–F, explicit
actions, no silent downloads) remains the behavioral base; Graph 4 adds the
recommended voice pack, full per-component attribute sheets, the receipted §7
pipeline, and the separated storage roots.

Navigation: drawer entry "Voice Models" → screen titled **MODEL SANCTUM — Models and Voice**.
The architectural name is the title; the functional label is always visible.

## 2. Recommended voice pack (§6 bulk action)

- Planned deterministically by `DefaultVoicePackInstaller.plan(profile, resources, catalog)`
  for the recommended profile (HYBRID_BALANCED).
- Contains ONLY policy-required, `recommended`-flagged, `VERIFIED_DOWNLOADABLE`
  entries compatible with the device. For the Xiaomi baseline this is:
  Silero VAD v5.1.2, Vosk small-it-0.22, Piper IT Riccardo (x-low).
- **Never** contains: the local LLM (Qwen2.5-0.5B stays optional and manual), the
  alternative voice (Paola), the denoiser, the probe fixture, or any
  CANDIDATE_PENDING_DISCOVERY component.
- Honest gaps are displayed, not hidden: the custom Italian wake word appears as
  BLOCKED with its real classification until a verified production source exists.
- Bundled runtimes (sherpa-onnx TTS via Gradle) appear as BUNDLED, not downloads.
- Before start, the full preflight summary is shown: download total, installed
  total, conservative peak temporary storage (download + installed), the reserve
  (greater of 2 GB and 15% of available storage), minimum free required, a
  duration estimate explicitly labeled
  `TARGET_PENDING_DEVICE_QUALIFICATION` (transport math, not a measurement),
  Wi-Fi / charging / thermal advice, the license list, and the optional items
  that stay out. Install starts only after an explicit confirmation dialog.

## 3. Per-component attribute sheet (§6)

Each catalog entry exposes: display name, component type, version, provider,
license (+URL), installed status (registry read-back), qualification status,
download size, installed size, RAM estimate, expected usage mode (latency class),
Italian support, runtime format, quantization, storage destination, update
availability (registry version vs catalog version), active state, deterministic
fallback contract (`ComponentFallback`), upstream repository, revision binding,
runtime requirement, provenance note, and known limitations.

Actions, all wired to the real engine: Download (from the verified `sourceUri`),
Pause, Resume, Cancel, Verify, Install (receipted single-component pipeline),
Qualify, Activate, Deactivate, Roll back, Remove, Re-download, Repair (hash check
then re-fetch), Update (enabled when the registry version differs), Details
(expand), Open diagnostics (storage-root usage + receipt read-back).

## 4. Storage roots (§7)

`DefaultModelStorageManager` separates: `catalog/`, `tmp/` (partial downloads),
`staging/`, installed component directories, `active/`, `previous/` (version
evidence copies), `quarantine/` (rejected partials + facts sidecar), `logs/`,
`receipts/`. Models never leave the app-controlled root.

- Hash mismatch → the partial is MOVED to `quarantine/` with a
  `*.facts.txt` sidecar (expected/actual SHA-256, byte count). Evidence is
  preserved, never silently destroyed.
- Any installed file of an identity is COPIED to `previous/` before a
  replacing atomic move. The Graph 2 rollback contract (`.pkg.prev` inside the
  component dir, hash-verified restore) is untouched — no existing test weakened.
- `rootsUsage()` feeds the on-screen diagnostics.

## 5. Receipts (§7)

Every pack or single-component install writes secret-free receipts into
`receipts/`:

- `receipt-<identity>-<epoch>.json` — per component: identity, version, public
  SHA-256, sizes, source host, start/finish, outcome, resume offset, retry count,
  and the ordered pipeline steps (preflight → download → hash-verification →
  atomic-install → registry-read-back → qualification → activation).
- `pack-<epoch>.json` — pack receipt: profile, ordered component outcomes,
  INSTALLED / PARTIAL_OR_FAILED / INTERRUPTED.

Receipts contain artifact facts only — no tokens, no credentials, no private
endpoints (INV-G4-03; enforced by construction and by test).

## 6. Download gatekeeper (§7)

`DownloadGatekeeper.check(resources, neededBytes)` classifies before any
transfer and between pack items: offline → BLOCK; thermal ≥ severe → BLOCK;
thermal ≥ moderate → WARN; metered → WARN; power saving → WARN; insufficient
free storage → BLOCK (final authority remains the storage preflight with the
2 GB / 15% reserve and `StorageManager.getAllocatableBytes`).

## 7. Evidence

- Unit tests: `VoicePackInstallerTest` (10 cases: planning honesty, summary math,
  gatekeeper, receipted happy path, truthful stop + quarantine, receipt secret
  policy, previous-version preservation, quarantine sidecar, already-installed
  classification, fallback contract) — 126/126 total green.
- Lint: 0 errors, 31 warnings (unchanged from CP-G4-002 baseline).
- Local APK: app-debug.apk, 58,950,857 bytes, SHA-256 `bda3b0dfcc8f76ce3dfa1ef4577a3d915a1b16de51bfa7bb771d5f6241193222`.
- Physical-device qualification of every component remains
  `TARGET_PENDING_DEVICE_QUALIFICATION` (INV-G4-12).
