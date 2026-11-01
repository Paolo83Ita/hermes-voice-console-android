# REHYDRATION PACK — Hermes Voice Console Android

Purpose: any future executor (or the Owner) can resume exactly here. Contains NO secrets —
the GitHub token is Owner-supplied per session and is never stored in this repository or pack.

## Identity

- Project: Hermes Voice Console Android — dedicated voice/visual console for Hermes Tower.
- Private repo: `github.com/Paolo83Ita/hermes-voice-console-android` (visibility private).
- Default branch `main` (Owner merges only — INV-07). Working branch:
  `foundation/v0.1-cloud-build`.
- V0.1 scope: 8-screen Compose console, 15-state enforced machine, MockTowerBridge +
  DeterministicTowerSimulator + ScriptedConsoleDriver, privacy mute, reduced motion,
  burn-in mitigation, mock TTS, inactive voice engines (intentional).

## Where everything is

- Governance authority: `docs/governance/` (10 files; start at PROJECT_OPERATING_CONTRACT.md).
- Foundation docs: `docs/*.md` (19 files; REQUIREMENTS, ARCHITECTURE, UI_STATE_MACHINE,
  TOWER_BRIDGE_PROPOSED_CONTRACT, …) — preserved, do not duplicate.
- App source: `app/src/main/java/com/hermestower/voiceconsole/**` (44 files),
  tests `app/src/test/**` (6 classes, 33 tests).
- Design mockups: `design/mockups/` (Marble Light, Obsidian Night, comparison).
- Build: `settings.gradle.kts`, `build.gradle.kts`, `app/build.gradle.kts`,
  `gradle/libs.versions.toml`, committed Gradle 8.9 wrapper.

## How to build (verified 2026-11-01)

Requirements: JDK 17 (Temurin), Android SDK with `platforms;android-35` +
`build-tools;35.0.0` + platform-tools.

```bash
./gradlew :app:assembleDebug :app:testDebugUnitTest   # gates used by CI
./gradlew :app:lintDebug                              # non-blocking report
# APK: app/build/outputs/apk/debug/app-debug.apk
```

CI: GitHub Actions workflow `android-debug-build` (`.github/workflows/android-debug-build.yml`),
runs on every push to the working branch and via workflow_dispatch; uploads the APK, test
results and lint report as artifacts named with the commit SHA.

## State at last update (2026-11-01, post-N8)

- Local build green: 33/33 unit tests pass; local APK hashed (validation only).
- Cloud build green: run 41296592392, commit 505047e9 — CI 33/33; APK artifact + SHA-256 in
  `docs/governance/APK_RECEIPT_V0.1.md` (APK_CREATED, APK_HASHED, APK_BOUND_TO_COMMIT).
- Secret scan: 0 findings (tree + history).
- Open: physical Xiaomi qualification (Owner-run; docs/INSTALL_XIAOMI.md),
  real Tower integration (V0.4), real voice front-end (V0.2+), Owner merge to main.

## Next exact actions (if resuming)

1. Read `docs/governance/CURRENT_PROJECT_TRUTH.md` and `BLOCKER_REGISTER.md`.
2. If Owner returned device read-back: update truth + registers, type DEVICE_READBACK_VERIFIED.
3. If new code work: create the next graph/branch per Owner instruction; never touch main.

## Authentication protocol (no secrets stored here)

The Owner supplies a fine-grained GitHub token at session start, authorized ONLY for
`Paolo83Ita/hermes-voice-console-android`. Store it transiently outside the repo, use via a
credential helper or bearer header, never in remote URLs, never in files, history, logs or
this pack. Repo auth budget: 2 attempts; push budget: 2 attempts.
