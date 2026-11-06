# GRAPH49_FRESH_READBACK.md

- Mission: GRAPH 4.9 — Owner physical retest integration, real STT and TTS
  delivery, optional local LLM, Tower Archive completion, monumental UI
  polish, full regression and beta release candidate.
- Owner: Paolo Lombisani
- Mode: CONTINUE_FROM_LATEST_VERIFIED_REMOTE_AUTHORITY (fresh fetch + read-back
  performed; the freshly verified remote tip prevails).
- Written: 2026-11-07 (session time), evidence fetched live in this session.

## 1. Fresh local + remote read-back (CP-G4.9-001)

| Check | Verified value |
| --- | --- |
| Active branch | `graph-4/real-voice-pairing-archive-e2e` |
| Local HEAD (before read-back) | `afefd3d154db8680f96c388a2ed1d8020d371e35` |
| Remote HEAD (fresh fetch, origin) | `eb660d5198408f99266d7bf0c5bc47481319c949` |
| Divergence | local 0 ahead / 20 behind → **fast-forward only** (no reset, no force) |
| Local HEAD (after advance) | `eb660d5198408f99266d7bf0c5bc47481319c949` = remote tip |
| Working tree | clean (`git status --porcelain` = 0 entries) |
| Stash | empty |
| Remote branches | foundation/v0.1-cloud-build, graph-2/…, graph-3/…, graph-4/real-voice-pairing-archive-e2e, main (untouched) |

### Reported vs verified

| Briefing reported | Freshly verified | Verdict |
| --- | --- | --- |
| Remote tip `53841a5` | `eb660d5` (53841a5 is 20 commits behind) | **STALE — tip prevails** |
| Version `0.3.1-alpha-graph4-owner-repair` | same, `app/build.gradle.kts` line 23 | current |
| versionCode 4 | same, line 22 (> shipped 3) | current |
| Cloud build run `42226155144` | that run is `bf34014` (pre-repair). Latest build run at tip: **42247787324**; latest e2e: **42247787274** | **STALE — tip runs prevail** |

### The 20 advanced commits (summary)

`7c17745 … 4116819` = the R-27→R-41 emulator E2E recovery chain;
`1cd5738` = Owner physical feedback cycle; `bf34014` = CP-G4-026 verification
repair; `53841a5` = CP-G4-028 governance; `1e2701e` = **CP-G4-029 OWNER
PHYSICAL RETEST FAILURE repair** (TtsSpeakGate fail-closed, SpeechProfile,
TtsTelemetry, BuildIdentity, ConsoleControlSemantics, AdaptiveActionsRow,
ST-51 real-engine verdicts, version 0.3.1/versionCode 4);
`35fd9d1` = **POCO_X6_PRO_OWNER_RETEST_PACKAGE_2 READY** (310/310 unit, lint 0
errors, 3 APKs hashed); `eb660d5` = deployment record.

## 2. Cloud artifacts of the completed repair (verified from the API)

Run **42247787324** (android-debug-build @ `eb660d5`): **BUILD_SUCCESSFUL**.
Artifacts: debug APK (145,057,289 B), lint report (124,371 B), test results
(40,755 B). Parsed from the CI JUnit XML artifact itself:
**310 tests / 0 failures / 0 errors / 0 skipped** — matches the local claim.
Lint gate (abortOnError) passed ⇒ 0 lint errors at the tip.

Run **42247787274** (android-e2e @ `eb660d5`): **FAILED** — see §4.

## 3. Build identity validation (briefing §3)

- `app/build.gradle.kts`: versionCode 4, versionName
  `0.3.1-alpha-graph4-owner-repair`, BUILD_LABEL default
  `POCO OWNER REPAIR 1`, buildConfigField provenance block present.
- `app/.../app/BuildIdentity.kt` present (build-injected identity +
  on-device measured APK SHA-256/variant/ABIs/install-update timestamps).
- `BuildIdentityTest.kt` present in the unit suite (part of the 310).
- No stale `0.2.0-debug` constant remains (APP_VERSION is BuildConfig-backed).
- CI APK artifact at the tip carries the stamped identity (the build
  injects full SHA + run ID; run 42247787324 artifacts exist and are fresh).

State: **BUILD_IDENTITY_VERIFIED** at code+CI level; on-device POCO
confirmation remains with the Owner (open gate, §5).

## 4. E2E failure at the tip — REGRESSION FOUND AND CLASSIFIED

Lane evidence: artifacts `hermes-voice-console-e2e-results-sample-{1,2}-eb660d5…`.

| Test class | Pre-repair `bf34014` (runs 42226155140) | Post-repair tip `eb660d5` |
| --- | --- | --- |
| E2EVoiceFlowTest | OK (6 tests) **both lanes** | **2 failures** — `e2e_14_cancelActiveResponse_returnsToIdle` (expected IDLE, was SPEAKING) + `e2e_19_20_reconnect_validatesQueuedRequestBeforeDelivery` (queue never flushed: connection DISCONNECTED, 1 queued, owner turn PENDING) |
| E2EFreshInstallTest / E2EModelManagerTest / E2ESelfTestTest | crash (NavBackStackEntry INITIALIZED→DESTROYED, 2 refs each, both lanes) | identical crash (pre-existing, ledgered class: docs/testing/E2E_FAILURE_LEDGER.md §routes appearance/settings; R-chain R-13…R-41) |
| E2ELifecycleProfileTest / E2EOnboardingTest | OK | OK |

**Classification: `e2e_14` is a repair-introduced regression (CP-G4-029,
commit `1e2701e`), root cause identified in code:**
`SystemTtsDomainAdapter.speak()` — the new honest ready-wait window
(`readyWaitMs = 2_000`) does not observe interruption. `interrupt()` during
the window flips the engine LOADING→READY, the wait loop exits on READY and
`engine.speak(text)` is called unconditionally — the Owner cancel is
swallowed and the utterance is spoken anyway; the console then shows
SPEAKING and `e2e_14`'s 10 s IDLE window expires. `e2e_19_20`'s queue-flush
failure is consistent with the cascade (process-wide AppGraph carries the
stuck SPEAKING state into the next test).

**Action:** fix the adapter (interruption token checked before
`engine.speak()`, inside both wait loops, and before the SPEAKING
emission), add unit tests, keep the fail-closed semantics untouched.

## 5. Owner retest asynchronous gate

`OWNER_POCO_RETEST_PENDING` — the Owner is testing the arm64 build
(POCO_X6_PRO_OWNER_RETEST_PACKAGE_2, preferred artifact
`app-arm64-v8a-debug.apk` sha256 `dbbdc429…b60cc`) on POCO X6 Pro.
No Owner evidence has arrived in this session; **no success may be inferred**
from build/emulator results. When evidence arrives it is integrated via
`OWNER_POCO_RETEST_RECEIPT.md` with outcomes POCO_RETEST_PASSED /
POCO_RETEST_FAILED_REPAIR_REQUIRED / POCO_RETEST_PARTIAL.
Forbidden tokens remain unclaimed: ITALIAN_TTS_OWNER_VERIFIED,
RESPONSIVE_UI_QUALIFIED, POCO_RETEST_PASSED.

## 6. Component state attested at the tip (read-back)

| Area | State at `eb660d5` |
| --- | --- |
| TTS fail-closed | `voice/TtsSpeakGate.kt`, `voice/ItalianTtsVoiceNegotiator.kt`, `voice/SpeechProfile.kt`, `voice/TtsTelemetry.kt`, engine-level gate in `AndroidSystemTtsEngine.speak()` — present, 310/310 unit green; **cancel-race regression found (§4) — repair queued** |
| ST-51 | real-engine verdict classes present (`SelfTestRegistry.kt`), never a JVM pass |
| Model Sanctum | `ui/screens/ModelManagerScreen.kt` with AdaptiveActionsRow (Verify repair preserved); catalog `assets/models/voice_pack_catalog.json` |
| STT runtime | `voice/VoskFrameSttEngine.kt` present (frame-seam; Vosk delivery = Graph 4.9 node) |
| TTS candidates | system engine + speech profile; natural local voice path = Graph 4.9 node |
| LLM candidates | `local/LocalLlmEngine.kt`, `local/LlmRuntime.kt`, `MockLocalConversationEngine.kt` (optional local LLM = Graph 4.9 node) |
| Tower Archive | `archive/MockTowerArchive.kt`, `archive/ArchiveRepositories.kt`, `ui/screens/ArchiveScreen.kt` (product completion = Graph 4.9 node) |
| Tower Gate | `pairing/*`, `tower/*` (QR, expiry, replay, scopes, Keystore vault) — Graph 1.7 matrix = Graph 4.9 node |
| Visual | monumental components + TowerArchitecture at tip; preview pack in repo |
| Governance | CURRENT_PROJECT_TRUTH / WORK_LOG / CHECKPOINT_INDEX / REQUIREMENT_TO_EVIDENCE_REGISTER / OWNER_UI_LAYOUT_DEFECT_LEDGER / ITALIAN_TTS_ROOT_CAUSE_REPORT all advanced to CP-G4-029 |

## 7. Decision and order of execution (briefing §18)

1. **Repair the regression first** (SystemTtsDomainAdapter interrupt token +
   unit tests) — regression must be green before new nodes stack on it.
2. Natural local TTS delivery (verified runtime candidate, no aspirational
   catalog fields).
3. Real local STT delivery (Vosk Italian, user-operable).
4. Optional local LLM (verified runtime compatibility, else typed blocked).
5. Model Sanctum responsive qualification + microphone-control qualification.
6. Tower Archive product completion.
7. Tower Gate / Graph 1.7 compatibility matrix.
8. Targeted visual polish + Self-Test expansion.
9. Full regression (unit green, lint 0 errors) + universal/arm64(/x86_64)
   APKs, hashes, receipts.
10. Commit, push (no force), fresh remote read-back, governance update.

Every checkpoint reports NODE / FINDING / ACTION / TEST / FILES / EVIDENCE /
COMMIT / REMOTE READ-BACK / CHECKPOINT / NEXT.
