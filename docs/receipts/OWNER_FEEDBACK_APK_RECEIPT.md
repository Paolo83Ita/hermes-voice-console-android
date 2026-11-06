# OWNER FEEDBACK APK RECEIPT — 0.3.0-alpha-graph4 (the current Owner package)

Built: 2026-11-06 · Branch: `graph-4/real-voice-pairing-archive-e2e` · Commit: `bf340144671a5af20ccd7cf1baa8898223c0f27d`
Build authority: GitHub Actions run **42226155144** (workflow `android-debug-build`) — **BUILD SUCCESSFUL**
Unit tests: **274 completed, 0 failed** (JUnit XML, artifact `hermes-voice-console-test-results-bf34014…zip`, artifact ID 13506287245)
Lint: executed (`lintDebug`), non-blocking, per workflow policy.

## APKs (artifact `hermes-voice-console-debug-apk-bf34014…`)

| APK | Bytes | SHA-256 |
| --- | --- | --- |
| `app-universal-debug.apk` (install this on POCO X6 PRO if unsure) | 206,701,159 | `56ac87ab82faecb2078a3dd4022c42b6735ff0ef2c23ab4db87bad7ae565071d` |
| `app-arm64-v8a-debug.apk` (device-native, smaller) | 105,268,304 | `df4c0db0fd0108f125abead9df540a6d82c8461065522fb9009a76fd5f84f9f5` |
| `app-x86_64-debug.apk` (emulator) | 109,197,848 | `f58505918b9d4c0702e53229350ac168d8e0d0f4b902a97a841abee8001a3aaa` |

Size note (honest): the universal APK at the PREVIOUS tip `4116819` was already
206,679,899 B — this cycle added ~21 KB (code-only changes; no assets). The ~165 MB
figure in the Graph 4.6-era receipt predates later delivery-chain additions and is
NOT the comparison point.

## Version identity (glanceable)

- versionName **`0.3.0-alpha-graph4`**, versionCode **3** — vs the Owner-tested
  package `0.2.0-debug` (versionCode 2, commit `ca8980b`, Graph 3 terminal).
- Self-Test ST-01 asserts the `0.3.0` schema; Settings → Apps shows the version.

## Binding to the Owner cycle

- Install/upgrade + the second validation cycle: `docs/INSTALL_POCO_X6_PRO.md`.
- What the package carries: the Italian-TTS typed voice negotiation + runtime
  read-back (Diagnostics rows, Voice & Audio Italian-voice card, ST-51), the
  UI_POLISH_PASS layout repairs (BadgeFlowRow on 18 rows, SectionCard weight,
  Settings hub row repair), the honest 51-test self-test battery.
- E2E segmented-gate run 42226155140 @ `bf34014` was IN FLIGHT at receipt time —
  its verdict belongs to the remaining Graph 4 node loop and is reported separately;
  it does NOT gate the Owner package (build + unit tests + lint are the package gates).

## Repairs before this receipt (honest deployment history)

| Commit | Run | Outcome |
| --- | --- | --- |
| `1cd5738` | 42224431930 | BUILD_FAILED — 5 compile errors, 2 sites (ACTION_TTS_SETTINGS resolution; getLanguage() returns a Locale not a String) — fixed at `7cac4fe` |
| `7cac4fe` | 42225216683 | BUILD_FAILED — 274 tests, 3 failed: classify() ignored the engine language read-back when getVoice() is null (the negotiator's own test caught it) + SelfTestCenterTest honest realignment for ST-51 — fixed at `bf34014` |
| `bf34014` | 42226155144 | **BUILD_SUCCESSFUL — 274/274** — this receipt |
