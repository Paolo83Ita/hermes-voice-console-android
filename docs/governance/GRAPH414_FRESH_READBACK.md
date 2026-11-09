# GRAPH 4.14 — FRESH READ-BACK (CP-G4.14-001)

Mission: CONTINUE_FROM_LATEST_VERIFIED_REMOTE_AUTHORITY. Fresh remote tip
prevails. Local was behind → fast-forward only; no force-push; main
untouched; no older local authority restored.

## 1. Branch / HEAD / tree

| Item | Value |
| --- | --- |
| Branch | graph-4/real-voice-pairing-archive-e2e |
| Local HEAD at session start | afefd3d (recycled sandbox snapshot) |
| Remote tip (fetch verified) | 17090bb (CP-G4.13-015b rehydration pointer) |
| Delta | 0 ahead / 53 behind → ff-only afefd3d → 17090bb |
| Working tree | clean (0 dirty files) |
| Worktrees | single main checkout; no stray worktree |
| Active processes | none (no gradle/adb/emulator); no duplicate execution started |
| main | e7ab101 — untouched |

## 2. Latest build / E2E / APKs / version / unit / lint

- Build: android-debug-build run 42571197175 @ 17090bb **SUCCESS** (and
  42571034398 @ afbe48d SUCCESS) — both docs-only governance tips.
- E2E: runs 42571034445 (afbe48d) + 42571197180 (17090bb) — **both lanes
  E2E_VERDICT_FAILED, standing typed R-42..R-49 harness family**; verdict
  artifact re-read at 17090bb = E2E_VERDICT_FAILED; gate-A signature = the
  A1 onboarding-anchor family; residual 7/2 — identical to the pre-4.13
  baseline 4b8bfb2. Zero Process-crashed anywhere. No 4.13-attributed
  regression (CP-G4.13-015 arbitration stands).
- APKs (current Owner artifact = Package 6): arm64 3b79b7c6…bcaa,
  universal da8a0ee1…1662, x86_64 a2e3cea8…d21c; CI tip rebuild carries
  tip-identity artifacts.
- Version: **0.3.6-alpha-graph4.13-visual-accessibility / versionCode 9**,
  label OWNER VISUAL ACCESSIBILITY PACKAGE 6. Graph 4.14 increments ABOVE
  this (0.3.7-alpha-graph4.14-runtime-theme-tower / 10 candidate).
- Unit tests: **472/472** (0 failures, 0 errors, 0 skipped; independently
  re-parsed from CI JUnit XML, 64 suites).
- Lint: **0 errors / 82 warnings, all classified** (below the 84 baseline;
  0 newly introduced). Secret scan 0 (tree+history).

## 3. Runtime catalog — component truth (CP-G4.14-002 input)

Source: app/src/main/assets/models/voice_pack_catalog.json (manifest 2.0.0,
12 entries) + code wiring verified this session.

| # | identity | type | catalog classification | runtime truth (code) | physical usable today? |
| --- | --- | --- | --- | --- | --- |
| 1 | org.hermestower.test.probe-artifact | WAKE_WORD | TEST_FIXTURE_ONLY | none (fixture) | n/a (test only) |
| 2 | org.openwakeword.model.melspectrogram | WAKE_WORD | VERIFIED_DOWNLOADABLE | onnxruntime-android 1.20.0 INTEGRATED (RealMelEmbedFrontend) | engine dependency only |
| 3 | org.openwakeword.model.embedding | WAKE_WORD | VERIFIED_DOWNLOADABLE | same ORT chain | engine dependency only |
| 4 | org.openwakeword.wakeword.hey-jarvis | WAKE_WORD | VERIFIED_DOWNLOADABLE | OpenWakeWordEngine real 3-graph ORT chain; **OFF by default** | pipeline qualification fixture (EN) |
| 5 | org.hermestower.wakeword.hermes-it | WAKE_WORD | CANDIDATE_PENDING_DISCOVERY | none; SOURCE_UNVERIFIED_DOWNLOAD_BLOCKED | NO — no verified source exists |
| 6 | org.silero.vad.v5 | VAD | VERIFIED_DOWNLOADABLE | **NO Silero engine implementation** — baseline is EnergyVadEngine (RMS); ORT runner available | NO — download exposed, unusable (FINDING-01) |
| 7 | org.gtcrn.speech-enhancement.simple | NOISE_SUPPRESSION | VERIFIED_DOWNLOADABLE | GtcrnDenoiserEngine real streaming ORT inference INTEGRATED | YES (optional, off by default) |
| 8 | org.vosk.model.small-it-0.22 | SPEECH_TO_TEXT | VERIFIED_DOWNLOADABLE | vosk-android 0.3.75 INTEGRATED; VoskFrameSttEngine loads Model(filesDir/models/stt/vosk-small-it); real Recognizer | YES (STT path real) |
| 9 | org.sherpa-onnx.tts.runtime | TTS_ENGINE | CANDIDATE_PENDING_DISCOVERY (stale label!) | sherpa-onnx v1.12.24 JitPack INTEGRATED (CP-G4.9-003); SherpaRealNativeBridge real OfflineTts | YES (bundled runtime) |
| 10 | org.piper.voice.it-riccardo-x-low | VOICE | VERIFIED_DOWNLOADABLE | engines wired w/ real native bridge (AppGraph L386-403); **activation gated by capability ladder + physical qualification** | download+install yes; ACTIVE only after ladder |
| 11 | org.piper.voice.it-paola-medium | VOICE | VERIFIED_DOWNLOADABLE | same | same |
| 12 | org.qwen.qwen2.5-0.5b-instruct.q5-k-m | LOCAL_LLM | VERIFIED_DOWNLOADABLE | **NoVerifiedLlmRuntime** — typed refusal LLM_RUNTIME_PENDING_VERIFIED_SOURCE; no llama.cpp/MLC/ORT-GenAI dependency linked | **NO — download exposed, unusable (FINDING-01/02)** |

## 4. Download manager / state machines / UX authorities

- Download: ModelDownloadManager + DownloadGatekeeper + ModelSha256 +
  ModelManifestVerifier — real, lifecycle-tested (ModelDownloadLifecycleTest).
- ModelInstallState: 18 values (ModelComponents.kt:22-41, terminal flags).
- Capability ladder: ModelCapabilityState 13 values; ModelCapabilityTransitions
  forbids INSTALLED→ACTIVE / DOWNLOADED→ACTIVE skips (no-skip refusal const).
- ModelRuntimeReceipt: typed proof record + recorder (MODEL_RUNTIME_TRUTH_REPORT).
- UX authority: ModelUxSemantics (CompatVerdictResolver → 13 typed verdict
  kinds; ModelActionResolver → ONE primary action per state) + ModelUxComponents
  renderers. RUNTIME_UNAVAILABLE verdict exists: IT "Non utilizzabile: il
  runtime Android richiesto non è disponibile." (values-it/strings.xml:823).
- runtimeRequirement flows catalog → ModelCatalogEntry.runtimeRequirement →
  CompatVerdictResolver (ModelManagerScreen L398-419).

## 5. Voice selection / runtime factory / LLM candidate

- Voice authority = **TtsRouteSwitch** (tts/TtsRouteSwitch.kt): default route
  SYSTEM; natural routes org.piper.voice.it-riccardo-x-low / -paola-medium
  map to the two SherpaOnnxTtsEngine instances and are returned ONLY when the
  selected voice is qualified/ACTIVE (routedNaturalVoiceId). Android system
  TTS stays the mandatory fallback (SystemTtsDomainAdapter).
- Voice runtime factory = AppGraph wiring (two per-voice SherpaOnnxTtsEngine
  instances with SherpaRealNativeBridge; AudioTrackPlayer playback).
- Local LLM candidate: Qwen2.5 0.5B Instruct GGUF q5_k_m, pinned commit
  9217f5db…, SHA-256 041474553f…, integrity chain real, inference seam =
  NoVerifiedLlmRuntime (the ONLY deliberately unimplemented runtime).

## 6. Bridge state authority / Mock authority / real-Tower authority

- **No typed MOCK vs REAL mode enum exists.** ConnectionStatus.BridgeStatus
  {UNCONFIGURED, CONNECTING, CONNECTED, DISCONNECTED} + TlsStatus
  {MOCK_NOT_APPLICABLE, UNKNOWN, ACTIVE, FAILED} (ConsoleStateStore.kt:28-36)
  are transport status, not truth mode.
- Mock authority: TowerEndpoint.MOCK + AppGraph boot sets
  store.connection=UNCONFIGURED(endpointLabel="mock://tower.simulator",
  tls=MOCK_NOT_APPLICABLE) + bridge.configure(MOCK). The console bridge is
  **MockTowerBridge only** (ScriptedConsoleDriver mirrors it into
  store.connection); RealTowerBridge is consumed ONLY by Tower Connection
  Studio + SelfTestCenter and its StateFlow is NEVER mirrored into
  store.connection.
- **FINDING-03 confirmed at code level**: StatusPill (ConsoleScreen L224-237)
  renders pill_connected="BRIDGE OK" (values-it too) with a CYAN dot
  (m.cyanCore) whenever BridgeStatus==CONNECTED — reachable purely by the
  mock handshake; drawer indicator AppNav L278-300 same mapping. The
  indicator cannot distinguish mock from real today.

## 7. Theme authority / Console consumers

- Selection: settings key dark_theme (SettingsRepository K_DARK), default
  true; AppearanceScreen offers MARBLE LIGHT / OBSIDIAN NIGHT swatches →
  store.updateSettings(darkTheme). HermesTheme branches BOTH HermesColors
  (MarbleLightColors vs ObsidianNightColors incl. 4.13 semantic tokens) and
  the Material scheme. followSystemTheme persisted but never consumed.
- **FINDING-04/05 root cause**: Theme.kt:35 provides
  LocalMonumentalColors = MonumentalObsidian UNCONDITIONALLY — the entire
  monumental layer (tower stage, chrome, frieze) is always dark; Console
  root background = m.obsidianDeep (ConsoleScreen L158). Marble Light
  therefore cannot reach the Console.
- Hardcoded dark ARGB blockers (Marble repair list): ConsoleScreen L170
  frieze gradient (0xFF0A0E16/0xFF070A11), L798 MedallionDisc radial
  (0xFF070A10); MonumentalChrome L51/L168; OracleCore L248/338/368/387;
  TowerArchitecture L169; ReforgedTowerVisual overlay literals (L229,
  L339-388); TowerCore desaturation targets (L74-80). Dark-only WebP tower
  bitmaps (no Marble variant exists).

## 8. Current Tower assets + Owner reference image

- Integrated Tower = Graph 4.13 candidate A "Monumental Oracle Citadel":
  13-layer contract, candidate_a balanced (tower_*.webp) + low
  (towerl_*.webp) in res/drawable-nodpi; renderer ui/monumental/
  ReforgedTowerVisual.kt (8 static + 5 async mechanism layers,
  rememberInfiniteTransition ring rotation + portal breathing, state
  overlays, LOW-tier reduced animation); stage 1000×1375; docs/visual/
  tower413/{ASSET_MANIFEST,INTEGRATION_MANIFEST}.json with SHA-256 per asset.
- **FINDING-06/07 recorded**: the Owner physically rejects this Tower
  (CURRENT_TOWER_OWNER_REJECTED — flat, weak, insufficiently animated). It
  is superseded by the reference-central-Tower strategy (§32-§37 of the 4.14
  charter). The 4.13 assets stay in-tree until replacement is integrated.
- Owner reference image: /home/z/my-project/upload/Immagine_Hermes_Tower.png
  — PNG 1536×1024 px, 3,685,783 bytes. Authorized as the primary visual
  source for the central Tower (clean extraction + reconstruction +
  optimization + animation); the full poster (lateral model towers, labels,
  messengers, arrows, side panels, sky, audit panel) is NOT authorized.

## 9. Governance state

- Graph 4.13 terminal implementation state reached:
  GRAPH413_OWNER_VISUAL_ACCESSIBILITY_PACKAGE_READY (CP-G4.13-014/015).
- Physical retest: OWNER_VISUAL_RETEST_PENDING (4.13 Package 6) — superseded
  operationally by the 4.14 verdict below; the Owner-only grant rule stands.
- **NEW OWNER VERDICT (Graph 4.14 §0)**: OWNER_RUNTIME_THEME_VISUAL_REPAIR_REQUIRED.
  Preliminary physical PASS preserved (do not redesign): Italian local
  responses, unified Interrompi, continuous listening, QR reader opens,
  install+launch. Open findings F-01..F-08 registered (runtime missing;
  unusable download actions; Mock "BRIDGE OK" cyan misleading; Marble does
  not apply to Console; Marble low contrast; Tower rejected; Tower flat/
  weak/static; reference central Tower authorized as new visual authority).
- Blockers: B-0001/B-0003 unchanged; no new blocker.

## 10. Graph 4.14 scope freeze (from the charter)

Five programs ONLY: A runtime completion, B Model Library product
completion, C bridge physical truth, D theme convergence + complete Marble
Light, E reference-Tower integration + animation. Do not expand: Archive,
real Tower pairing, Graph 1.7 server routes, QR payload contract, user
management, unrelated settings, new LLM candidates without a runtime
decision.

NEXT (dependency-released): CP-G4.14-002 Android runtime requirement matrix
(§4), then per-component decisions (§5) in the §6 order.
