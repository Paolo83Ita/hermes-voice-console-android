# CURRENT_PROJECT_TRUTH — GRAPH 4 PRE-STATE (VERIFIED READ-BACK)

Generated at Graph 4 bootstrap, before any Graph 4 modification.
Every fact below was physically read back on 2026-11-02 (fresh `git fetch` + GitHub API + artifact byte-level verification).

## 1. Repository and branch truth

| Item | Verified value | Verification method |
| --- | --- | --- |
| Repository | `Paolo83Ita/hermes-voice-console-android` (PRIVATE) | GitHub API read-back |
| Authoritative branch | `graph-3/monumental-oracle-tower-visual-refoundation` | GitHub API branch read-back |
| Remote branch tip | `ca8980b8f157f25e9bede9158d46835f8b5079df` | GitHub API + `git fetch` (identical) |
| Local branch tip | `ca8980b8f157f25e9bede9158d46835f8b5079df` | local `git rev-parse` |
| `main` tip | `e7ab101ecde0dd093edd27b94bbb969822063d26` (Graph 1 terminal; untouched by Graphs 2–3) | `git fetch` read-back |
| Other remote branches | `foundation/v0.1-cloud-build`, `graph-2/model-manager-tower-connection-e2e` (both preserved) | `git branch -a` |
| Working tree state at read-back | CLEAN (0 dirty entries at `ca8980b`) | `git status --porcelain` |

### Dirty-state classification at session start

On session start the working tree reported 225 modified files with **0 insertions / 0 deletions** — a pure
`100644 → 100755` mode artifact of the sandbox filesystem, no content change. Classification: environment
artifact, NOT work. Action: `git restore .` (mode restored to index state). Nothing was discarded; no
complete/coherent uncommitted Graph 3 work existed to preserve. Verified: tree clean at `ca8980b` afterwards.

## 2. Graph 3 terminal state (verified)

- Terminal commit message head: `graph-3/terminal(CP-G3-008): typed terminality GRAPH_3_CONDITIONALLY_COMPLETE_WITH_EMULATOR_LIMITATION …`
- Terminality receipt: `docs/receipts/GRAPH_3_TERMINAL_RECEIPT.md`
- Merge to `main` remains Owner-blocked (not attempted in Graph 4 either).

## 3. Cloud authority evidence at `ca8980b`

| Item | Verified value | Evidence |
| --- | --- | --- |
| `android-debug-build` run | `41380930670`, completed, **success**, head `ca8980b` | GitHub API runs read-back |
| `android-e2e` run | `41380930578`, completed, **failure** (emulator-environment class, consistent with Graph 3 terminal receipt residual) | GitHub API runs read-back |
| Prior authority run | `41375246590` at `85cdc0e` (102/102, APK `0639ae6e…`) | commit message + API |

## 4. APK truth (byte-verified from Owner-uploaded artifact `ca8980b`)

| Property | Verified value |
| --- | --- |
| Uploaded zip | `hermes-voice-console-debug-apk-ca8980b8f157f25e9bede9158d46835f8b5079df.zip` |
| Extracted APK | `app-debug.apk`, 58,782,349 bytes |
| **SHA-256** | `79864d54262cc003616848ec34b2d28a282943cecfa019cf5259389a2c729113` |
| Match vs. reported baseline | **EXACT MATCH** |
| applicationId / versionName / versionCode | `com.hermestower.voiceconsole` / `0.2.0-debug` / `2` (reported; source-consistent) |
| minSdk / targetSdk / AGP | 30 / 35 / 8.7.3 (reported; source-consistent) |

## 5. Test truth (parsed from Owner-uploaded JUnit XML, run at `ca8980b`)

- Executed: **102**; Failures: **0**; Errors: **0**; Ignored: **0** — matches reported baseline.
- Suites include: ConsoleStateMachineTest, OracleCoreModelTest, VoiceModelManagerCoreTest,
  ModelDownloadLifecycleTest, TowerConnectionStudioTest, SelfTestCenterTest, RuleBasedIntentRouterTest,
  MockTowerBridgeTest, TowerContractJsonTest, SettingsMigrationTest, LogRedactorTest, ScriptedConsoleDriverTest.

## 6. Lint truth (parsed from Owner-uploaded `lint-results-debug.txt`, run at `ca8980b`)

**2 errors, 36 warnings.**

Errors (both `ProduceStateDoesNotAssignValue`, Compose runtime):

1. `app/src/main/java/com/hermestower/voiceconsole/ui/monumental/OracleCore.kt:47` — `val timeMs by produceState(0L, state, reducedMotion, quality) { … }` producer lambda does not assign the value.
2. `app/src/main/java/com/hermestower/voiceconsole/ui/components/TowerCore.kt:41` — same defect class.

Warning classification (36, individually listed for NODE 0):

| Count | Class | Graph 4 disposition |
| --- | --- | --- |
| 27 | GradleDependency | classify; document intentionally pinned versions (no bulk upgrade) |
| 3 | AndroidGradlePluginVersion | same |
| 2 | ObsoleteSdkInt | remove checks minSdk 30 already guarantees |
| 1 | UsableSpace | storage preflight repair (StorageManager path + safe reserve) |
| 1 | OldTargetApi | classify (targetSdk 35 intentional) |
| 1 | MonochromeLauncherIcon | add adaptive monochrome launcher icon |
| 1 | DataExtractionRules | add Android 12+ dataExtractionRules excluding secrets/pairing/models |

## 7. Capability baseline (from Graph 3 verified state)

Present and functional: ConsoleStateMachine; 15 visual states; Monumental Oracle Core; Voice Model Manager;
DefaultModelDownloadManager; model registry; hash verification; pause/resume; HTTP Range support; rollback and
removal; Tower Connection Studio; HTTPS + WSS profiles; Tailscale profile; TowerBridge abstraction;
deterministic mock server; Self-Test Center; migration framework; secret redaction; cloud Android build;
debug APK generation.

Missing (Graph 4 scope): real model URLs + production hashes (placeholders); real wake word / VAD / STT / TTS /
local LLM installed runtimes; QR pairing; local discovery; automatic Tailscale discovery; Tower Archive
(conversations / missions / artifacts); recovery of historical Tower conversations; physical Xiaomi
qualification. Pairing designed but not implemented.

## 8. Discrepancies found during read-back

| # | Item | Discrepancy | Resolution |
| --- | --- | --- | --- |
| 1 | Working tree | 225-file mode-only diff at session start | Environment artifact; restored; no work lost |
| 2 | Workflow run at tip | `android-debug-build` success is at `ca8980b` = run `41380930670` (newer than the run named in the Graph 3 receipt text `41375246590` at `85cdc0e`) | Both exist; `41380930670` is the tip authority. No reset performed (INV: latest verified remote tip prevails) |
| 3 | Everything else | None — APK hash, test counts, lint errors, remote tips all match the reported baseline | — |

## 9. Graph 4 starting position

- Start SHA for `graph-4/real-voice-pairing-archive-e2e`: `ca8980b8f157f25e9bede9158d46835f8b5079df`
- No production credential is stored in the repository; push/API access uses the protected out-of-repo secret
  (never printed, never committed — INV-G4-03).
- Physical Xiaomi evidence: NONE yet (all evidence cloud/emulator/JVM). Physical qualification remains PENDING.
