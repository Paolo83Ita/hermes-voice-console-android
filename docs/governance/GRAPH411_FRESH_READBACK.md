# GRAPH 4.11 FRESH READ-BACK — Hermes Voice Console Android

Executed: 2026-11-07, mode `CONTINUE_FROM_LATEST_VERIFIED_REMOTE_AUTHORITY`, branch `graph-4/real-voice-pairing-archive-e2e`.
Basis: the Owner GRAPH 4.11 "PHYSICAL EXPERIENCE COMPLETION" briefing (six nodes, P0 → P1 → P2) with the standing directive: **no new architecture, no new governance systems, no new abstractions, no Graph 5 — physical product quality and user-visible behavior only.**

---

## 1. Remote/local authority (fresh fetch, forward-only)

| Item | Verified value |
| --- | --- |
| Remote tip at session start | `5a567a3` (local was `afefd3d`, 32 commits behind) |
| Action taken | `git fetch` + `git merge --ff-only` → local == remote == `5a567a3`; working tree clean; `main` untouched; no force-push; no remote work discarded |
| What the 32 commits are | the COMPLETE Graph 4.10 execution (CP-G4.10-001…014) ending in `POCO_XIAOMI_RETEST_PACKAGE_3_READY` at `653ae0f`: versionName `0.3.3-alpha-graph4.10-owner-repair`, versionCode 6, label `POCO XIAOMI OWNER REPAIR 3`, arm64 116,561,845 B sha256 4ffefb80…, unit 412/412, lint 0 errors (run 42317581730) |
| Conclusion | the Owner's "ormai siamo oltre Graph 4.10" is verified at source level: the 4.10 repair build EXISTS and was declared READY; Graph 4.11 therefore runs as the qualification+completion graph on top of it |

## 2. Implementation inventory at the tip (the 4.10 authorities, read back)

| Authority | File(s) | State at `5a567a3` |
| --- | --- | --- |
| Response locale resolver | `locale/ResponseLocaleResolver.kt` | PRESENT (priority chain: explicit session → app language → input hint → system → English fallback) |
| Local response catalog (5 languages) | `locale/ResponseCatalog.kt` | PRESENT — 46 categories × it/en/es/fr/de, parity unit-tested |
| Text/TTS consistency | `simulator/ScriptedConsoleDriver.kt`, `bridge/MockTowerBridge.kt`, `local/MockLocalConversationEngine.kt` | PRESENT — producers consume the catalog (localization tests green) |
| Voice selection authority | `voice/VoiceSelectionAuthority.kt` | PRESENT — six-condition read-back, ACTIVE-only route truth |
| Voice runtime factory | `voice/VoiceRuntimeFactory.kt` | PRESENT — files→runtime→load→synthesis→playback→read-back |
| Natural voices | `voice/SherpaOnnxTtsEngine.kt` + AppGraph seams | PRESENT — riccardo-x-low / paola-medium bundles |
| Chat speech policy + speaker control | `voice/ChatSpeechOutputPolicy.kt`, `ui/screens/ConsoleControlSemantics.kt` | PRESENT — AUTO_SPEAK/MUTED/MANUAL, semantic IDs, "Parla" label |
| Listening mode + continuous loop + echo | `voice/ListeningModePolicy.kt`, `voice/ContinuousListeningController.kt`, `voice/EchoFingerprintGuard.kt` | PRESENT — PUSH_TO_TALK default, typed stop conditions, BARGE_IN_OFF |
| Responsive layout | `ui/components/ResponsiveActionGroup.kt`, `ResponsiveScreenHeader.kt`, `ConsoleWindowWidthClass.kt`, `AdaptiveActionsRow.kt` | PRESENT — COMPACT/MEDIUM/LARGE (§19), Xiaomi 11 Lite classification test (§22/§23 tests green) |
| Central Tower | 10 `tower_*.webp` + `ui/monumental/ReforgedTowerVisual.kt` | PRESENT — but the Graph 4.11 audit found 4 measured defects (see §4) |
| Self-Test Center | `selftest/…` ST-01..ST-53 | PRESENT — no dedicated voice A/B qualification (gap closed this graph) |

## 3. Graph 4.11 node mapping (what was executed)

| Node | Owner demand | Executed in Graph 4.11 |
| --- | --- | --- |
| 1 — VOICE SWITCHING QUALIFICATION | Voice A → play → Voice B → play → prove difference | NEW **ST-54 "Voice A/B comparison"** (SelfTestCenter/Registry/AppGraph `voiceAbQualification()`): the ACTIVE voice speaks the preview phrase, then the OTHER installed voice speaks the SAME phrase on the real engines; authority untouched (byte-compared before/after); JVM decision-logic tests (7 cases); audible verdict stays `VOICE_SWITCHING_OWNER_VERIFICATION_PENDING` (Owner-only) |
| 2 — CONTINUOUS LISTENING | 10 turni continui | NEW tests: `ten consecutive turns keep the session alive without any stop condition` (guard/resume timing per turn) and `ten consecutive turns reject every assistant echo and accept every user phrase` |
| 3 — ITALIAN CHAT | Nessuna risposta locale inglese | NEW catalog-level pin: `no category answers English under the Italian locale` (46 categories; the single locale-neutral `TOWER_PROGRESS` typed explicitly); the 4.10 producer tests carry |
| 4 — XIAOMI RESPONSIVE PASS | Basato sui tuoi screenshot reali | Verified existing §19/§22/§23 coverage (width classes, Xiaomi 11 Lite 5G NE COMPACT/MEDIUM classification, actions-per-row, Italian enum table, 5-locale parity). Real screenshots remain Owner-capture items (`OWNER_UI_LAYOUT_DEFECT_LEDGER.md`) |
| 5 — CENTRAL TOWER RECONSTRUCTION | Owner image as composition authority; 9 named layers; discard lateral elements | Measured audit + repair (report: `docs/receipts/CENTRAL_TOWER_VISUAL_REFOUNDATION_REPORT.md`): sign clip fixed, rear/ring arrow intrusions removed, ALL static bands re-staged stage-aligned; manifest v2; previews re-rendered; verdict **OWNER_VISUAL_RETEST_PENDING** |
| 6 — POCO OWNER PACKAGE 4 | `0.3.4-alpha-graph4.11` o superiore | versionCode **7**, versionName **0.3.4-alpha-graph4.11**, BUILD_LABEL default **POCO XIAOMI OWNER PACKAGE 4** |

## 4. Central Tower audit detail (Node 5)

| ID | Measured defect | Repair |
| --- | --- | --- |
| G4.10-D1 | `tower_sign` clipped the H of "HERMES" and the M of "MULTI" (sourceBox started inside the plaque) | plaque re-measured [578,0,1018,166], sky dropped, re-staged |
| G4.10-D2a | `tower_rear` carried the sign's text sliver | sign zone cleared (stage y < 271) |
| G4.10-D2b | rear + rings carried lateral arrow fragments (incl. a bronze-toned arrowhead inside the annulus) | hue/edge scrub + mirror-cloned clean arc repair |
| G4.10-D4 | the four static bands were tight canvases — in the app they rendered at the wrong size/position (all at stage top) | all bands re-staged on full 720×1653 canvases — Compose code unchanged, assets carry placement |

Core alignment proof: manifest core (789,413) → stage (360.0, 666.7) = (0.49997, 0.40332) — the code's exact ring center. Portal/energy byte-identical to 4.10.

## 5. Explicitly NOT true yet (unclaimed)

- Every physical verdict: `VOICE_SWITCHING_OWNER_VERIFICATION_PENDING`, `OWNER_VISUAL_RETEST_PENDING`, Xiaomi/POCO physical pass, 10-turn physical soak — Owner-only, runbook in the Package 4 document.
- Real Tower pairing: `REAL_TOWER_PENDING_GRAPH_1_7` (P2 — untouched by this graph).
- CI build results for the 0.3.4 build (recorded after push; the E2E fleet remains an emulator-environment gate, not an Owner-package gate).

## 6. Decision

Graph 4.11 continues the 4.10 authorities instead of replacing them (Owner directive). The graph's additions are: ST-54 A/B qualification, the 10-turn/anti-echo/anti-English test pins, the measured Tower repairs, the version bump and the Owner Package 4 runbook. No new abstraction, no new governance system, no Graph 5.
