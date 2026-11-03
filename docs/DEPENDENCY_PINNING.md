# DEPENDENCY PINNING — GRAPH 4 NODE 0 (CP-G4-002)

The Android Lint `GradleDependency` / `AndroidGradlePluginVersion` warnings report that newer library
versions exist than the ones this project pins. This document records the intentional pinning decision
required by the Graph 4 mission (NODE 0: "document intentionally pinned dependency versions; do not
bulk-upgrade AGP, Kotlin or Compose in this node").

| Artifact | Pinned | Newest advertised by lint | Why pinned |
| --- | --- | --- | --- |
| Android Gradle Plugin | 8.7.3 | 9.5.0 | Build authority for Graphs 1–3 evidence (runs 41375246590/41380930670) used 8.7.3; upgrading mid-mission invalidates comparability and risks build breakage. Re-evaluate at a dedicated upgrade node. |
| Kotlin | 2.0.21 | — | Paired with AGP 8.7.3 and Compose compiler (K2). Same comparability argument. |
| Compose BOM | 2024.12.01 | 2026.09.00 | Runtime behavior of the Monumental Oracle Core (15 visual states, E2E frozen-clock harness) is verified on this BOM. |
| kotlinx-coroutines | 1.9.0 | — | `TestMonotonicFrameClock` behavior pinned for the frame-clock test harness. |
| androidx.activity / navigation / lifecycle | 1.9.3 / 2.8.5 / 2.8.7 | newer available | Verified pairings; no functional gap for Graph 4 scope. |
| androidx.test (ext-junit/runner/rules/espresso) | 1.2.1 / 1.6.2 / 1.6.1 / 3.6.1 | newer available | The E2E ledger (runs 41349811483…41380930578) was produced on these; changing them changes emulator-truth comparability. |
| onnxruntime-android (CP-G4-008) | 1.20.0 | 1.29.0 | REAL runtime binding for the physically verified voice assets (GTCRN denoiser, openWakeWord melspec/embed/head). 1.20.0 is a mature stable line covering opsets 11/13 used by the verified models; newer lines are re-evaluated at a dedicated node. minSdk 24+ ≤ project minSdk 30. |
| androidx.camera (CP-G4-010 wave 2) | 1.4.2 | newer available | QR pairing scanner capture pipeline (camera-camera2/lifecycle/view). Stable line, minSdk 21 ≤ project minSdk 30. Availability verified on Google Maven this session (HTTP 200 on the 1.4.2 POM). License Apache-2.0. bindToLifecycle gives lifecycle-bound open/close (camera honesty rules). |
| com.google.mlkit:barcode-scanning (CP-G4-010 wave 2) | 17.3.0 | newer available | BUNDLED on-device barcode scanning (QR-only configured) — no Play Services dependency, frames never leave the process. Availability verified on Google Maven this session (HTTP 200 on the 17.3.0 POM). License Apache-2.0. Upgrade re-evaluated at a dedicated node. |
| targetSdk | 35 | lint suggests newer preview | 35 matches the installed cloud SDK (`platforms;android-35`), keeping the build hermetic. |

Policy:
1. Upgrades happen in a dedicated, named node with their own checkpoint and green cloud run — never as a
   side effect of another node.
2. Security-relevant upgrades may be pulled forward only with an explicit decision entry in
   `docs/DECISION_LOG.md`.
3. Lint warnings of the `GradleDependency`/`AndroidGradlePluginVersion`/`OldTargetApi` classes are therefore
   ACCEPTED-INTENTIONAL (see `docs/lint/LINT_WARNING_CLASSIFICATION.md`).
