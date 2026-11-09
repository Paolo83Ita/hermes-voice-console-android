# GRAPH 4.14 — LOCAL BUILD RECEIPT (§18)

Local success is NOT sufficient for terminality — the authoritative build is
the cloud (CI) build at the pushed tip; this receipt is the local evidence
leg.

## NODE
§18 — Local build receipt (final local validation build of this continuation).

## Environment
| Item | Value |
| --- | --- |
| Host | Linux sandbox (recycled session; toolchain re-provisioned this session) |
| JDK | OpenJDK 17.0.21 (Temurin, installed at the pinned path /home/z/tools/jdk-17.0.21+9 — matches org.gradle.java.home and CI parity) |
| Gradle | Gradle 8.9 (wrapper distribution, checksum-fetched) |
| Android SDK | cmdline-tools 12.0; platform-tools; platforms;android-35; build-tools;35.0.0 (licenses accepted) |
| AGP | 8.7.3 (pinned) |

## Build commands (exact)
```
./gradlew --no-daemon --console=plain :app:testDebugUnitTest
./gradlew --no-daemon --console=plain :app:lintDebug
./gradlew --no-daemon --console=plain :app:assembleDebug
./gradlew --no-daemon --console=plain :app:compileDebugKotlin :app:compileDebugUnitTestKotlin   (incremental verification)
/home/z/android-sdk/build-tools/35.0.0/aapt2 dump badging <apk>      (identity)
python3 -c "zipfile.ZipFile(<apk>).testzip()"                        (ZIP integrity)
sha256sum app/build/outputs/apk/debug/*.apk                          (hashes)
```

## Gate results
| Gate | Result |
| --- | --- |
| Unit tests | **496/496 GREEN** (0 failures, 0 errors, 0 skipped; 68 suites; JUnit XML independently re-parsed — includes the 61-definition Self-Test battery registration, the llama seam tests, Silero/GTCRN/wake engine tests, ThemeAuthorityTest contrast pins, BridgeModeAuthorityTest, ModelUxSemanticsTest) |
| Lint | **0 errors / 72 warnings** (all classified — see docs/lint/LINT_WARNING_CLASSIFICATION.md Graph 4.14 addendum; 7 newly-introduced warnings repaired, not suppressed) |
| Secret scan | **0 findings** (working-tree diff + full-tree pattern sweep; no tokens, no local SDK paths in tracked build files) |
| APK variants | arm64-v8a 124,010,662 B · universal 253,657,491 B · x86_64 123,139,244 B (hashes in GRAPH414_APK_SIZE_AND_RUNTIME_REPORT.md) |
| Build identity | aapt2: versionCode 10, versionName 0.3.7-alpha-graph4.14-runtime-theme-tower, minSdk 30, targetSdk 35, compileSdk 35 |
| Build duration | assembleDebug ≈ 3 m 30 s (post-repair rebuild); full unit+lint wave ≈ 8 min |
| Warnings (build log) | Kotlin deprecation notices (createTempDir in legacy tests) + "multiple Kotlin daemon sessions" under --no-daemon — no errors |

## Runtime self-test state at this build (§25)
- Battery: **61 definitions** (ST-01..ST-61) — the latent CP-G4.12 discard
  bug repaired (ST-55/56/57 were silently unregistered); ST-58..61 added with
  REAL probes (Silero fixture inference / GTCRN streaming / wake-word typed
  state / LLM chain).
- On JVM: probe-less tests degrade to QUALIFICATION_PENDING_DEVICE /
  BLOCKED_DEPENDENCY honestly (never PASS from catalog state alone).

## Known limitations (typed)
- No emulator in the sandbox: E2E verification stays with the CI emulator
  lane + the standing R-42..R-49 harness verdict line; physical device
  verification stays with the Owner retest (Package 7).
- llama physical layers (native load / output / memory / thermal) —
  LLM_PHYSICAL_QUALIFICATION_PENDING (see GRAPH414_LLM_HONESTY_GATE.md).
- The universal APK carries legacy ABIs from dependency AARs
  (armeabi/mips families) — analysis/compatibility artifact only; the Owner
  installs the arm64-v8a split.

## FILES
- app/build/outputs/apk/debug/app-{arm64-v8a,universal,x86_64}-debug.apk
- app/build/test-results/testDebugUnitTest/ (JUnit XML)
- app/build/reports/lint-results-debug.{xml,html}
- docs/receipts/GRAPH414_APK_SIZE_AND_RUNTIME_REPORT.md

## CHECKPOINT
§18 DONE. NEXT: §19 commit/push discipline → §20 cloud build verification.
