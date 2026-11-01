# APK RECEIPT — first authentic debug build (GRAPH 1 / V0.1)

Typed evidence: CLOUD_WORKFLOW_EXECUTED · CLOUD_BUILD_PASSED · UNIT_TEST_EXECUTED (CI) ·
ANDROID_LINT_EXECUTED · APK_CREATED · APK_HASHED · APK_BOUND_TO_COMMIT.

| Field | Value |
| --- | --- |
| Repository | `Paolo83Ita/hermes-voice-console-android` (private) |
| Workflow | `android-debug-build` (`.github/workflows/android-debug-build.yml`) |
| Run | 41296592392 — SUCCESS — https://github.com/Paolo83Ita/hermes-voice-console-android/actions/runs/41296592392 |
| Commit | `505047e9e05351a74b2b400ccca6442c364118ff` (branch `foundation/v0.1-cloud-build`) |
| Runner | ubuntu-latest, Temurin JDK 17, preinstalled Android SDK + `platforms;android-35` + `build-tools;35.0.0`, Gradle 8.9 wrapper |
| Gates | `:app:assembleDebug` + `:app:testDebugUnitTest` — green |
| CI tests | 33 tests, 0 failures/errors (test-results artifact 13079417861) |
| Lint | `lintDebug` executed, non-blocking (report artifact 13079387922) |
| Artifact | `hermes-voice-console-debug-apk-505047e9e05351a74b2b400ccca6442c364118ff` (id 13080210671) |
| APK | `app-debug.apk` — 57,848,596 bytes |
| SHA-256 | `f7a767354e241961928f1b536ec71c23e005920f55b7e7a791b29cc126cd0be7` |
| Signature | ephemeral CI debug key; no production key used (INV-04) |
| Local copy | deliverables folder as `hermes-voice-console-debug-v0.1-505047e.apk` |

Build iteration ledger (budget: 3 materially different iterations):

| # | Run | Result | Cause / repair |
| --- | --- | --- | --- |
| 1 | 41296487511 | CLOUD_BUILD_FAILED | `android-actions/setup-android@v3` internal license step ran `sdkmanager --licenses` without stdin → EOF → exit 1 on the runner's cmdline-tools 16.0 |
| 2 | 41296592392 | CLOUD_BUILD_PASSED | Material change: dropped the action; used the preinstalled runner SDK with `yes`-piped license acceptance + pinned package install |

Note: the local validation APK (`163d700a…357557`, built 2026-11-01 in the sandbox) is NOT
the deliverable; hashes differ because debug signing keys and build metadata are per-build.
The cloud APK above is the single authoritative Owner-installable artifact.
