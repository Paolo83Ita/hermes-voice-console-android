# OWNER CONTINUATION READBACK — Graph 4 directive intake (CP-G4-016)

Produced 2026-11-03 in response to the OWNER GRAPH 4 CONTINUATION DIRECTIVE
(simplified product experience, complete five-language localization, monumental animated
Tower, real clickable model/voice installation, E2E recovery, physical Xiaomi readiness).
Every value below was measured this session — nothing carried from memory.

## 1. Directive acknowledgment

| Owner decision | Intake |
| --- | --- |
| OWNER_DECISION_E2E_RECOVERY | **CI_LIFECYCLE_PROFILE AUTHORIZED** — animation-disabled CI test variant for lifecycle/navigation qualification only; NORMAL_VISUAL_PROFILE stays the physical-qualification target. Required variants + experiments TEST-A..TEST-E accepted as prescribed. |
| OWNER_DECISION_PRODUCT_SIMPLIFICATION | Accepted — simple default UX, technical depth under Advanced settings/Diagnostics. |
| OWNER_DECISION_VISUAL_REFINEMENT | Accepted — Tower refoundation toward the supplied reference (upload reference image on file), original implementation, no direct shipping of the reference. |
| OWNER_DECISION_REAL_MODEL_DELIVERY | Accepted — real clickable source links, real downloads, real installation, real activation for STT / TTS voices / optional local LLM. |
| OWNER_DECISION_LOCALIZATION | Accepted — full five-language usability with the Italian completeness gate (ITALIAN_UI_COMPLETE, not merely ITALIAN_RESOURCES_PRESENT). |

Execution order accepted: E2E residual → localization → navigation/onboarding simplification →
Tower refoundation → real downloads/links → regression → APK + Xiaomi package.
Mission mode CONTINUE_FROM_LATEST_VERIFIED_REMOTE_TIP honored: no Graph 4 restart, no main
modification, no force-push, no recreation of completed work.

## 2. Fresh local/remote read-back (measured this session)

| Item | Measured value |
| --- | --- |
| Active branch | `graph-4/real-voice-pairing-archive-e2e` |
| Local HEAD | `801385326a8b190f211cdcc2ef5043704946b0fd` |
| Remote HEAD (ls-remote) | `801385326a8b190f211cdcc2ef5043704946b0fd` — **exact match** |
| Working tree | clean (0 modified/untracked) |
| main | `e7ab101` untouched (verified via ls-remote) |
| Owner-reported governance commit | `8013853` — matches remote tip |

## 3. Latest cloud evidence (GitHub API read-back)

| Run | Commit | Workflow | Status | Conclusion |
| --- | --- | --- | --- | --- |
| 41638770540 | 8013853 | android-debug-build | completed | **success** |
| 41638770495 | 8013853 | android-e2e | in progress at read-back | push-triggered, NORMAL_VISUAL_PROFILE — documented residual expected |
| 41638599327 | 15b1f05 | android-debug-build | completed | success |
| 41638599014 | 15b1f05 | android-e2e | completed | failure (typed residual, NORMAL profile) |
| 41632784491 | 1fa7829 | android-e2e | completed | failure — **verdict-channel repair validated** (truthful INSTRUMENTATION_FAILED_OR_INCOMPLETE; 7th deterministic residual sample) |
| 41632784469 | 1fa7829 | android-debug-build | completed | success |

## 4. APK receipt status

`docs/receipts/GRAPH4_DEBUG_APK_RECEIPT.md` (committed 1fa7829): universal debug APK @
b1df58a, run 41621955833 SUCCESS, **165,029,486 B**, sha256 `9275aa222d75411587eb9058bca
370e6a0f4dfdcc32c2e21815d8e3f30ffdaa0`; growth vs Graph 3 decomposed (ONNX Runtime ×4 ABI
≈73.7 MB, ML Kit barhopper ×4 ABI ≈20.8 MB, dex ≈65 MB, bundled ONNX assets ≈4.3 MB); all 4
bundled artifacts re-hashed MATCH inside the CI-built APK. CP-G4-024 will produce the
arm64-v8a variant + APK_SIZE_AND_ABI_REPORT.md per directive §18.

## 5. Current string resources

| Locale | Keys (app/src/main/res) |
| --- | --- |
| values (en, fallback) | 76 |
| values-it | 76 |
| values-es | 76 |
| values-fr | 76 |
| values-de | 76 |

Resource parity holds (LocalizationParityTest GATE-05), but the directive correctly states
presence ≠ completeness: the hard-coded literal inventory below is the real gap, plus the
new UI work must be born-localized. `app_name` = "Hermes Console" in all five (brand
anchor §14, intentional).

## 6. Hard-coded UI literal inventory (measured scan)

Kotlin UI sources scanned for string literals inside `Text("…")`, `contentDescription = "…"`,
`onClickLabel = "…"`, `title/label/placeholder = "…"`, `Toast.makeText(…, "…")` (comment
lines stripped): **235 literal occurrences across 16 UI source files** (measured this
session; per-file table to be produced as LOCALIZATION_LITERAL_AUDIT.md in CP-G4-017 with
the prescribed MUST_LOCALIZE / TECHNICAL_IDENTIFIER / MODEL_IDENTITY / FILE_NAME /
DIAGNOSTIC_CODE / TEST_ONLY / INTERNAL_NOT_VISIBLE classification). Densest files
(measured): ModelManagerScreen 50, TowerStudioScreen 43, DiagnosticsScreen 21,
SelfTestScreen 17, AppearanceScreen 15, ArchiveScreen 14, AboutSafetyScreen 13,
HistoryScreen 10, QrScannerScreen 9, VoiceAudioScreen 9, MonumentalChrome 8,
ConnectionScreen 8, ConsoleScreen 8, LocalIntelligenceScreen 7, AppNav 2.
This scan is the audit's starting evidence base.

## 7. Current downloadable catalog (production)

`app/src/main/assets/models/voice_pack_catalog.json` — 12 entries:
WAKE_WORD ×5 (probe-art, melspectrogram, embedding, hey-jarvis, hermes-custom v-pending),
VAD ×1 (silero v5.1.2), NOISE_SUPPRESSION ×1 (gtcrn simple),
SPEECH_TO_TEXT ×1 (vosk small-it-0.22), TTS_ENGINE ×1 (sherpa-onnx runtime),
VOICE ×2 (piper it-riccardo, piper it-paola), LOCAL_LLM ×1 (qwen2.5-0.5b-instruct q5_k_m).
Licenses: Apache-2.0 / MIT (hermes-custom entry = pending, marked).

## 8. Current bundled catalog

`assets/models/bundled/bundled_manifest.json` — 4 artifacts (melspec, embed, hey_jarvis,
gtcrn_simple), all hash-verified inside the shipped APK. Per directive §11 these must be
labeled "Included with the application" and do NOT count as post-install downloads; the
post-install download gate falls on STT (vosk small-it-0.22), TTS voice (piper), and the
optional LLM (qwen) — CP-G4-019/020/021.

## 9. Visual asset inventory

- `design/monumental/preview/` — Graph 3 11-preview pack + verification_report.json.
- `design/mockups/` — marble/obsidian mockups + comparison.
- `app/src/main/res/drawable-nodpi/` — monumental_l0_sky.webp, monumental_l1_temple.webp,
  monumental_l2_colonnade.webp, monumental_l7_stairs.webp (WebP layered pipeline, Graph 3).
- MeanderBand motif: present on Console + all 12 secondary screens (G4-C10).
- Reference image for the refoundation: Owner-supplied (on file in the workspace upload
  area) — design direction only, never shipped.

## 10. Current application navigation (AppRoutes + drawer)

Routes registered (AppRoutes.all): console, history, connection, voice, intelligence,
appearance, diagnostics, about, model_manager, tower_studio, self_test, archive,
qr_scanner.

Drawer today: PRIMARY = Console, Tower Archive, Tower Studio, Models and Voice (as
model_manager), Self-Test Center; SECONDARY = History, Connection, Voice and Audio, Local
Intelligence, Appearance, Diagnostics, About & Safety. Footer shows version/mode/lang.

Directive §5 target: PRIMARY = Console, Tower Archive, Connect to Hermes Tower, Models and
Voice, **Settings**; Appearance/connection-internals/intelligence-internals/engine-
diagnostics/registry-internals/fixtures/simulations move under the Settings categories
(GENERAL / VOICE / TOWER CONNECTION / MODELS / DISPLAY / DIAGNOSTICS) or Diagnostics.
Compact-panel constraints confirmed already implemented (DrawerWidthPolicy 60% preferred,
2/3 hard max, 248–360 dp bounds) and must be preserved.

## 11. E2E residual status (pre-decision baseline)

Typed residual EMULATOR_INPUT_INJECTION_LIMITATION, 7 deterministic samples (41551630218,
41615537760, 41621063936, 41621713747, 41621955951, plus 41632784491 validated-verdict
sample and the 41638599014/41638770495 normal-profile runs), signature
`IllegalStateException … INITIALIZED → DESTROYED … route=appearance`, position-independent.
Recovery decision (a) was exercised by the Owner: **CI_LIFECYCLE_PROFILE authorized**.
Next node implements the profile + experiments TEST-A..E + E2E_LIFECYCLE_ROOT_CAUSE_REPORT.md.

## 12. Consequence plan

CP-G4-016 is now in execution: this read-back + the E2E lifecycle decision work.
Follow-up checkpoints CP-G4-017..025 follow the directive order. No terminal verdict is
claimed; the permitted pre-physical state is GRAPH_4_IMPLEMENTATION_COMPLETE_XIAOMI_
QUALIFICATION_PENDING and it is NOT yet reached.
