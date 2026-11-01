# ANDROID EMULATOR E2E — Graph 2 qualification

Workflow: `.github/workflows/android-e2e.yml` — API 34, google_apis, x86_64, headless
(`-no-window -gpu swiftshader_indirect -noaudio -no-boot-anim`), animations disabled,
AVD userdata shrunk to 2 GB, boot timeout 2400 s, job budget 120 min, results uploaded
before failure evidence, bounded adb evidence capture, secret-clean evidence scan.

The suite runs `:app:connectedDebugAndroidTest` against the installed debug APK; the
in-app deterministic mock Tower server (127.0.0.1) supplies transport behaviors.

## Terminal state

`ANDROID_EMULATOR_E2E_PASSED` — never labelled PHYSICAL_DEVICE_QUALIFIED.

## Step → test mapping (E2E-01..45)

| Steps | Instrumented test |
| --- | --- |
| E2E-01..10 | `E2EFreshInstallTest` — fresh install, first launch, credential-free onboarding, Mock mode, Studio open/validate/save, profile read-back, mock connection test, CONNECTED only after accepted handshake |
| E2E-11..20 | `E2EVoiceFlowTest` — local request, Tower-routed request with streamed FINAL + receipt, cancel-to-idle, simulated disconnect → DISCONNECTED + effective SAFE mode, offline queue of a permitted non-consequential request, queued-never-shown-completed, reconnect with validation-before-delivery |
| E2E-21..30 | `E2EModelManagerTest` — Model Manager open, recommendation, manual mode, probe download, pause/resume (Range), hash verify, atomic install, activation read-back, rollback/removal |
| E2E-31..34 | `E2ESelfTestTest` — Quick run all-pass, Full run all-pass, typed fields, secret-clean clipboard export |
| E2E-35..45 | `E2EAppearancePersistenceTest` — Marble Light/Obsidian Night switch, reduced motion, privacy mute, listening blocked while muted, activity recreation, app restart persistence, clear sensitive values (metadata kept), final clean exit |
| secure storage | `KeystoreVaultInstrumentedTest` — GATE-04 (round-trip, no plaintext on disk, reopen, availability) |

## Failure handling

First-failure evidence: connected-test XML/HTML (uploaded even on failure), screenshot,
UI hierarchy, bounded logcat tail. Repair loop per GRAPH 2 §14: classify → smallest
root-cause repair → narrowest rerun → suite rerun; assertions are never weakened to
force a pass.

## Emulator environment constraints (recorded)

The Graph 2 runner provides no KVM (2 vCPU): the emulator boots with software
acceleration; disk was freed and the AVD userdata shrunk to 2 GB to fit. All E2E
evidence is EMULATOR_ONLY; Xiaomi read-backs are a separate physical graph.
