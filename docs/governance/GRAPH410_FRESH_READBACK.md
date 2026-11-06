# GRAPH 4.10 — FRESH READ-BACK (CP-G4.10-001)

Mission: GRAPH 4.10 — Owner physical product repair (Block 1 §1 FIRST EXACT ACTION).
Performed: 2026-11-07 (Europe/Rome).
Reported remote tip: `c18e313` (briefing value — NOT assumed current).

## 1. Git / branch authority

| Item | Verified value |
| --- | --- |
| Active branch (local) | `graph-4/real-voice-pairing-archive-e2e` |
| Tracked remote | `origin/graph-4/real-voice-pairing-archive-e2e` |
| Fresh `git fetch origin` | executed this session, success |
| Local HEAD | `c18e313d8eddd191b0182f1440b06cec625651d9` |
| Remote HEAD (post-fetch) | `c18e313d8eddd191b0182f1440b06cec625651d9` |
| Divergence | none — reported tip `c18e313` IS the verified tip |
| Working tree | clean (`nothing to commit`) |
| main modified? | NO (main untouched; force-push never used) |

## 2. Latest build / E2E / artifacts (inherited truth, re-verified locally)

| Item | Verified value |
| --- | --- |
| Latest deployment commit | `05be558` (code) → `c18e313` (deployment record) |
| Latest CI build run (from commit record) | android-debug-build `42295835464` @ `05be558` — BUILD_SUCCESSFUL (JUnit XML 335/335, lint + APK artifacts) |
| Latest CI E2E run (from commit record) | android-e2e `42295835446` @ `05be558` (was in flight at write time) |
| Local unit suite (measured now) | **335 tests / 0 failures / 0 skipped** across 43 JUnit XML files |
| APKs present | `app-universal-debug.apk`, `app-arm64-v8a-debug.apk`, `app-x86_64-debug.apk` |
| versionName | `0.3.2-alpha-graph4.9-local-models` |
| versionCode | `5` (build.gradle.kts pinned comment: MUST exceed versionCode 4) |
| Graph 4.9 package | `POCO_X6_PRO_OWNER_RETEST_PACKAGE_3` READY (arm64 sha256 `d9035248…c6b0a` preferred) |

## 3. Source read-back — response-language implementation (P0-01 ROOT CAUSE)

| Producer | File | Verified defect |
| --- | --- | --- |
| Local conversation engine | `local/MockLocalConversationEngine.kt` | ALL canned replies hard-coded English ("Good day. Hermes Console standing by.", "I am the Hermes Console…", "This console answers only whitelisted local intents…"). No locale parameter exists. |
| Scripted driver | `simulator/ScriptedConsoleDriver.kt` | Hard-coded English: `"I cannot do that. " + reason` (line 320), `"Tower accepted the request."`, `"Tower: …"`, `"Receipt …"`, `"The request failed at the Tower (…)"`, status notes in English. |
| Deterministic Tower | `bridge/DeterministicTowerSimulator.kt` | ALL deterministic Tower responses English ("Tower reports: mission queue is empty…", "Understood. I can draft a mission plan…", …). |
| MockTowerBridge | `bridge/MockTowerBridge.kt` | Error strings English ("Simulated transport failure after acceptance.", "Simulated Tower internal error."). |
| Locale authority today | `locale/AppLanguage.kt` | EXISTS and works for UI strings (`effectiveTag()` = override → system). **But no conversation producer consumes it** — `respond()` has no locale input. This is the P0-01 gap: the UI is Italian while `LocalConversationEngine` output is English. |
| Coordinator status notes | `app/ConsoleVoiceCoordinator.kt` | Hard-coded English status notes ("No speech captured.", "The recognizer needs network…"). |

**Conclusion:** a single canonical `ResponseLocaleResolver` does not exist. P0-01 confirmed at source level.

## 4. Source read-back — TTS engine routing (CALL PATH B)

| Item | Verified value |
| --- | --- |
| Route switch | `tts/TtsRouteSwitch.kt` — SYSTEM vs NATURAL routes, route from `routeProvider()`, interrupt-all on switch. |
| Route provider | `AppGraph.activeNaturalVoice()` — registry read-back (`componentType == "VOICE" && active && state == "ACTIVE"`) + disk `resolveVoiceDir()` check. |
| Natural voices | `org.piper.voice.it-riccardo-x-low` → `naturalTts`; `org.piper.voice.it-paola-medium` → `naturalTtsPaola` (both `SherpaOnnxTtsEngine` under `SystemTtsDomainAdapter`). |
| Known engines | `AndroidSystemTtsEngine` + `ItalianTtsVoiceNegotiator` (fail-closed) + `SpeechProfile` (rate 0.96 / pitch 0.95) + `TtsSpeakGate` (Italian post-apply read-back gate) + `TtsTelemetry`. |
| GAP vs GRAPH 4.10 §7/§8 | No single `VoiceSelectionAuthority` object (voice identity, SHA-256, qualification, last runtime read-back as one authority); selection→runtime mapping exists per-voice in `AppGraph` if-chains; no TextOnlyRuntime path; no `ITALIAN_VOICE_*` graded read-back state machine (§12). |

## 5. Source read-back — chat speaker control / mute (P0-04, P0-05)

| Item | Verified value |
| --- | --- |
| Console medallion row | `ui/screens/ConsoleScreen.kt` — SIX equal `IconButton`s in one `Row`: privacy mute, push-to-talk (big), repeat, TTS stop, inject wake, inject dialog. §18 violation confirmed (six equal circular buttons, ambiguous priority). |
| Per-message playback | NONE — chat timeline turns have no per-message Ascolta/Ripeti control (§11 requirement unmet). |
| Chat speech policy | NONE — no `ChatSpeechOutputPolicy`, no MUTED mode, no `VolumeOff` chat-mute control. `repeatLastSpoken()` exists in the driver (the only "speaker" action). |
| "parla invece" | `values-it/strings.xml:10` `composer_speak_instead` = "parla invece" — P0-07 confirmed, used by the composer speak button (ConsoleScreen line 432). |

## 6. Source read-back — listening mode (P0-06)

| Item | Verified value |
| --- | --- |
| Modes today | Push-to-talk only (`onPushToTalkPress` / `stopCapture`); foreground-only; no continuous mode; no wake word in UI. |
| Authority | No `ListeningModePolicy`. `MicUiState` exists (IDLE/PERMISSION_REQUIRED/LISTENING/DENIED/PERMANENTLY_DENIED/UNAVAILABLE). |
| Barge-in/echo | No echo prevention: `submitUtterance` does `tts.interrupt()` but nothing pauses capture during TTS (TTS and STT are mutually exclusive today only because push-to-talk ends before TTS starts). |

## 7. Source read-back — Tower Connection Studio layout (P0-08, P0-09)

| Item | Verified value |
| --- | --- |
| Action layout | `TowerStudioScreen.kt` — five `Row(spacedBy(6.dp))` groups of 2–3 `TextButton`s with long Italian labels; no width class; no stacking → one-character-per-line at Xiaomi width. |
| Raw enums shown | `ConnectionConfigState` (`Invalid`, `Validated`, … English displayLabels in code) rendered via `graph.profileStore.state().displayLabel`; `r.outcome.name` rendered raw (lines 256/356); `TransportMode.displayLabel` English. §23 rule absent. |
| Width authority | None (no window-size classes used; `DrawerWidthPolicy` exists for drawer only). |

## 8. Source read-back — Monumental Tower (P0-10, P0-11)

| Item | Verified value |
| --- | --- |
| Current visual | Procedural Compose Canvas: `ui/monumental/{TowerArchitecture, OracleCore, OracleCoreModel, MonumentalChrome, MonumentalColor, MonumentalType, BurnIn, VisualQuality}.kt` + `ui/components/TowerCore.kt`. |
| Owner reference | `/home/z/my-project/upload/Immagine_Hermes_Tower.png` (3,685,783 B) — central Tower isolated subject; remove model towers/messengers/arrows/panels/sky/fire bowls/bottom workflow panel. |
| Asset layers | None exist yet (`tower_*.webp` deliverables of §31). |

## 9. Governance read-back

| Document | State |
| --- | --- |
| `docs/governance/CURRENT_PROJECT_TRUTH.md` | Graph 4.9 terminal state: 335/335 unit, lint 0 errors, package 3 READY, OWNER_POCO_RETEST_PENDING (stale vs the NEW Graph 4.10 physical findings — superseded by §2 of this receipt). |
| `docs/governance/GRAPH49_FRESH_READBACK.md` | Present (Graph 4.9 cycle). |
| `docs/governance/MISSION_GRAPH.yaml`, `CHECKPOINT_INDEX.md`, `WORK_LOG.md`, `REQUIREMENT_TO_EVIDENCE_REGISTER.md`, `INVARIANT_REGISTRY.md`, `BLOCKER_REGISTER.md`, `DECISION_LOG.md`, `REHYDRATION_PACK.md` | All present; to be advanced semantically (append, not rewrite) at each CP-G4.10 checkpoint. |
| Terminality per briefing §43 | Current: `OWNER_RETEST_FAILED_REPAIR_REQUIRED`. Pre-device target: `POCO_XIAOMI_RETEST_PACKAGE_3_READY`. Physical target (Owner-only): `POCO_XIAOMI_CHAT_VOICE_LISTENING_LAYOUT_AND_VISUAL_RETEST_PASSED`. |

## 10. Decision

- Reported tip `c18e313` = verified tip; local == remote; no fast-forward needed; no remote work discarded.
- Graph 4.9 closed nodes (local models, ST-52/53, Archive product, package 3) are NOT restarted; their artifacts carry forward.
- The Graph 4.10 repair sequence starts at §4 of the briefing (ResponseLocaleResolver) against the root causes recorded above.
