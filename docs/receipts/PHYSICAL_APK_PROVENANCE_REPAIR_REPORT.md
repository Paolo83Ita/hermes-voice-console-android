# PHYSICAL APK PROVENANCE REPAIR REPORT

- Mission: OWNER PHYSICAL RETEST FAILURE (CP-G4-029) — §1 PROVE THE INSTALLED
  BUILD / §2 PREVENT STALE OR WRONG APK INSTALLATION
- Owner: Paolo Lombisani · Device: POCO X6 Pro
- The defect: the supplied `0.3.0-alpha-graph4` package exposed no in-app
  build identity — the Owner could not prove from inside the app which binary
  physically ran (and the in-app `APP_VERSION` constant was stale:
  `0.2.0-debug` vs BuildConfig `0.3.0-alpha-graph4` — drift removed this
  cycle). The retest verdict was therefore unprovable; this repair closes it
  forward.

## 1. What the app now exposes (permanently accessible, no ADB)

Locations:

1. **Settings → About and Safety → Build information** (full panel);
2. **Diagnostics → Runtime truth** (the TTS/engine read-back rows, this cycle
   extended) + the **compact build identity footer** at the bottom of the
   main Diagnostics screen.

Displayed values — authority split (nothing invented):

| Value | Authority |
| --- | --- |
| application versionName | BUILD-INJECTED (Gradle `versionName`) |
| versionCode | BUILD-INJECTED (Gradle `versionCode`) |
| complete Git SHA | BUILD-INJECTED (`GITHUB_SHA` at build time) |
| short Git SHA | BUILD-INJECTED (first 7 chars) |
| build timestamp (ISO 8601) | BUILD-INJECTED (UTC, at configuration time) |
| build workflow run ID | BUILD-INJECTED (`GITHUB_RUN_ID`; "(local build)" when absent) |
| workflow ref | BUILD-INJECTED (`GITHUB_REF_NAME`) |
| build label | BUILD-INJECTED (`POCO OWNER REPAIR 1`) |
| package name | BUILD-INJECTED (`BuildConfig.APPLICATION_ID`) |
| APK variant (UNIVERSAL / ARM64_V8A / X86_64) | ON-DEVICE MEASURED — the `lib/<abi>` entries read from the REAL installed APK file |
| APK SHA-256 | ON-DEVICE MEASURED — streamed SHA-256 of the real installed APK file (byte-identical to the CI-published `sha256sum`) |
| APK size (bytes) | ON-DEVICE MEASURED |
| installation timestamp | ON-DEVICE MEASURED (`PackageInfo.firstInstallTime`) |
| last update timestamp | ON-DEVICE MEASURED (`PackageInfo.lastUpdateTime`) |
| current process start time | ON-DEVICE MEASURED (`Process.getStartElapsedRealtime`) |
| Android release / SDK / manufacturer / model | ON-DEVICE MEASURED |

The no-runtime-generation rule is honored: every value that IDENTIFIES the
build is baked in at build time; the on-device rows are measurements of the
real installed file (a build cannot hash its own APK — the measured hash is
the truth that matches the published receipt byte for byte).

## 2. Actions

1. **Copy build identity** — the full identity block to the clipboard.
2. **Export secret-clean diagnostic receipt** (in About & Safety AND in
   Diagnostics → export card) — build identity + the TTS call-path telemetry
   (last events) + the runtime read-back. No spoken content, no transcript
   content, no credential (SEC §4/SEC-06). This is the receipt the Owner
   copies out of the app for the retest flow STEP 7.

## 3. Version and label policy (§2)

| Field | Supplied package | Repair build (this cycle) |
| --- | --- | --- |
| versionName | 0.3.0-alpha-graph4 | **0.3.1-alpha-graph4-owner-repair** |
| versionCode | 3 | **4** (strictly greater; nothing was reduced) |
| build label | — | **POCO OWNER REPAIR 1** |

The build label is visible in the build-identity panel AND in the Diagnostics
footer, so the Owner distinguishes the repair package from the rejected one
at a glance.

## 4. Android signature mismatch and upgrade path

Covered in `docs/POCO_OWNER_REPAIR_INSTALL_GUIDE.md`: how to confirm the
installed version/Git SHA in-app; how to distinguish ARM64 from universal;
what to do on "App not installed" (signature mismatch — uninstall first);
that uninstalling removes local application data (chat drafts, settings,
downloaded model files) and must be a deliberate choice.

## 5. Verification status

- Unit-tested: `BuildIdentityTest` (variant derivation from real `lib/`
  entries, the full identity block, the footer, the honest local-build
  markers) — pure JVM, part of the green 310-test suite.
- The on-device measurement path (APK SHA-256 stream, PackageInfo, process
  start) executes on device; its values are verified in the next physical
  cycle against the published hashes (the retest flow STEP 1).
- The CI workflow rebuilds at every push with `GITHUB_SHA`/`GITHUB_RUN_ID`
  stamped — the CI-built artifacts carry the full identity without any local
  marker.
