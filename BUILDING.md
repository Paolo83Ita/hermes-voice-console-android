# BUILDING — exact reproducible instructions

Environment of record: **Android Studio (current, e.g. Ladybug or newer) on the Owner's workstation**,
or any CLI with JDK 17+, Gradle 8.9+ and Android SDK 35.

Pinned toolchain (single source: `gradle/libs.versions.toml`):

| Component | Version |
|---|---|
| Android Gradle Plugin | 8.7.3 |
| Kotlin | 2.0.21 |
| Compose BOM | 2024.12.01 |
| Gradle wrapper distribution | 8.9 |
| JDK | 17 (also runs on 21) |
| compileSdk / targetSdk | 35 |
| minSdk | 30 (covers the Xiaomi 11 Lite 5G Android 11→13 lineage) |

## 1. Wrapper note (read first)

The Gradle wrapper **JAR is a binary and is intentionally not committed** (decision D-013); the
wrapper *properties* are. On first use, regenerate it once:

```bash
cd hermes-voice-console-android
gradle wrapper --gradle-version 8.9     # needs any local Gradle ≥ 8.0, or:
# alternatively simply open in Android Studio and accept its "fix Gradle wrapper" prompt
```

This is recorded as OPEN assumption A-31. After regeneration, `./gradlew` works everywhere.

## 2. Android Studio path (recommended)

1. **File → Open** → select this folder (the one containing `settings.gradle.kts`).
2. Let Gradle sync (downloads AGP 8.7.3, Kotlin 2.0.21, Compose BOM artifacts).
3. Select the `app` configuration → **Run** on a device or emulator (API 30+).

## 3. CLI path

```bash
# build the debug APK
./gradlew :app:assembleDebug
# artifact: app/build/outputs/apk/debug/app-debug.apk

# run the JVM unit tests (state machine, router, bridge, contract JSON, redaction, pipeline)
./gradlew :app:testDebugUnitTest
# report: app/build/reports/tests/testDebugUnitTest/index.html

# static analysis
./gradlew :app:lintDebug
```

## 4. Install on the target Xiaomi 11 Lite 5G

```bash
adb devices                                  # device must appear (USB debugging on)
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Record the APK checksum for the build report:

```bash
sha256sum app/build/outputs/apk/debug/app-debug.apk
```

The V0.1 prototype requests **no** runtime permissions. The manifest declares future-stage
permissions (RECORD_AUDIO, INTERNET, …) with justification comments; none is exercised yet.

## 5. Expected first-run behavior

- App opens on the Console screen in **Obsidian Night**, state **IDLE** (boot performs a mock handshake).
- The bridge pill shows **MOCK/BRIDGE OK**; Diagnostics shows your real device facts.
- Tap the bronze circle button → simulated wake → canned utterance → LOCAL or TOWER path per the
  router; the Tower path streams text deltas and finishes with a receipt reference.
- Diagnostics → Simulation: drive DISCONNECTED / ERROR / SLOW / UPDATE states truthfully.
- Appearance: switch to Marble Light, enable reduced motion, adjust intensity and dimming scrim.

## 6. If something fails to build

The sandbox could not compile (no Gradle/Kotlin/SDK — see FINAL_REPORT.md §Build status), so the
code has passed structural and cross-wiring review but not a compiler. If a compile error appears:

1. Check the pinned versions resolved (AGP/Kotlin/BOM) — do not bump versions as a first reflex.
2. The likely friction points are Compose API opt-ins (`ExperimentalMaterial3Api`) — the code already
   opts in where used.
3. Report the exact error into `docs/qualification/session-<date>.md` per the checklist discipline —
   do not silently patch and move on.
