# candidate-b-base — Candidate B integration area (visible source lineage)

GRAPH A4R CONTINUATION BLOCK C1-C4 — branch `graph-a4r/merge-with-b-base`.

## What this area is

The isolated integration area required by mission section 3: Candidate B's
authority lane lives here with VISIBLE source lineage, compiled into the app
module through `app/build.gradle.kts` (`java.srcDir("../candidate-b-base/app/src/main/java")`).
The final project may later move into the canonical app module; the manifest
`CANDIDATE_B_SOURCE_MANIFEST.json` records every file's provenance.

## The honest source truth (READ THIS FIRST)

**The original B source tree is NOT reachable in this execution environment.**
`Paolo83Ita/Hermes-Tower` is ABSENT from GitHub (fresh-read 2026-11-20:
GET /repos/Paolo83Ita/Hermes-Tower = 404; the Owner account exposes exactly two
repositories), there is no local checkout and no git bundle. This was already
recorded on 2026-11-10 in `docs/governance/GRAPH4_TOWER_VPS_HANDOFF_READBACK.md`.

Therefore the gate `CANDIDATE_B_ORIGINAL_SOURCE_BOUND_PASS` is recorded as
**BLOCKED_WITH_TYPED_EVIDENCE**, and the merge proceeds on the sanctioned
fallback that the A5 merge plan itself defined (phase M1, "authority_core_port"):
**B's architecture as the base, implemented as fresh canonical source** bound to
B's observable contract surface (the byte-exact handoff APK `908b289f…`, its dex
inventory, the jadx behavioral spec of TowerStore/QrPayload/PairingFlow/
DeviceKeys, and contract delta CD-A4R-001).

**No decompiled code was copied.** Decompiled output was used ONLY for forensic
confirmation, behavioral-spec binding and provenance validation (mission
section 1). Every reconstructed file is marked in the manifest.

## Files

| File | Provenance | Content |
|---|---|---|
| `app/src/main/java/com/hermestower/voiceconsole/data/TowerStore.kt` | A2 repair package | THE repair: explicit KeyGenParameterSpec (AES-256-GCM, 256-bit, randomized encryption), atomic `commitPairing`, typed failures, no plaintext fallback |
| `app/src/main/java/com/hermestower/voiceconsole/data/PairingPersistence.kt` | A2 + C1-C4 | 7 mission typed states, BlobFraming, `PairingStateStore` seam (+ SIMULATED test fake) |
| `app/src/main/java/com/hermestower/voiceconsole/pairing/QrPayload.kt` | reconstructed | Tower QR v2 parser (`payload.signature`, v=2, 10 forbidden fields, expiry, endpoint resolution) |
| `app/src/main/java/com/hermestower/voiceconsole/pairing/DeviceKeys.kt` | reconstructed | EC-P256 AndroidKeyStore device identity (secp256r1, SHA256withECDSA) |
| `app/src/main/java/com/hermestower/voiceconsole/pairing/PairingFlow.kt` | reconstructed + A2 patch notes | Claimed/PendingOwnerAck/Registered/Failed machine, 120 s challenge TTL, repaired persistence mapping |
| `app/src/main/java/com/hermestower/voiceconsole/net/TowerApi.kt` | reconstructed | 18 live v2 routes behind a testable transport seam |
| `app/src/main/java/com/hermestower/voiceconsole/towerpair/TowerPairingController.kt` | merge layer | Production orchestrator, 11 mission typed states, secret-free diagnostics |
| `app/src/main/java/com/hermestower/voiceconsole/pairing/TowerQrV2ScanFlow.kt` | reuse-and-extend | Proven scan state machine bound to the v2 parser |

## Owner action that unblocks the true-source rebind

Upload the Hermes-Tower git bundle (or create `Paolo83Ita/Hermes-Tower` on
GitHub). A later block then: rebinds this area to the true original source,
diffs it against this reconstruction (semantic diff), and converts any
divergence into a versioned contract delta — never a silent overwrite.

Generated: 2026-11-20T11:43:18Z
