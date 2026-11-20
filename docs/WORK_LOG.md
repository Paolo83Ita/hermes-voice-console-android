# WORK LOG — HERMES VOICE CONSOLE ANDROID

| | |
|---|---|
| Owner | Paolo Lombisani |
| Version | v0.1.0-alpha |
| Date | 2026-10-30 |
| Format | One checkpoint report per boundary, per mission discipline: NODE / FINDING / ACTION / TEST / FILES / COMMIT OR CHECKPOINT / NEXT |

---

## CHECKPOINT A1 — Stage A documentation complete

**NODE:** Stage A / discovery and decisions
**FINDING:** Sandbox exposes Java 21, Playwright, Node, git, zip, mermaid-cli; it lacks Gradle, the Kotlin compiler and the Android SDK. Android compilation is therefore impossible in this environment — Stage C will follow the mission's contingency path (complete source package + exact instructions + honest build status). All device facts remain unverified until the post-reset qualification session.
**ACTION:** Produced the full Stage A documentation set: product vision, device assumptions, requirements, architecture, security/privacy model, voice pipeline options with decision codes, local model candidate plan (downloads deferred), proposed versioned Tower contract with mock behavior, UI state machine (authoritative transition table), risk register, assumption register, device qualification checklist, development roadmap, file tree proposal, Mermaid architecture + 4 sequence diagrams, decision log.
**TEST:** Cross-document consistency pass (IDs A-xx/R-xx/D-xx/FR-xx/Q-xx used consistently; state table identical to what the code will enforce; no decision code omitted). Mermaid syntax to be validated with mmdc at checkpoint A2.
**FILES:** docs/{PRODUCT_VISION, TARGET_DEVICE_ASSUMPTIONS, REQUIREMENTS, ARCHITECTURE, SECURITY_AND_PRIVACY_MODEL, VOICE_PIPELINE_OPTIONS, LOCAL_MODEL_CANDIDATE_PLAN, TOWER_BRIDGE_PROPOSED_CONTRACT, UI_STATE_MACHINE, RISK_REGISTER, ASSUMPTION_REGISTER, DEVICE_QUALIFICATION_CHECKLIST, DEVELOPMENT_ROADMAP, FILE_TREE_PROPOSAL, ARCHITECTURE_DIAGRAM, SEQUENCE_DIAGRAMS, DECISION_LOG, WORK_LOG}.md
**COMMIT OR CHECKPOINT:** `stage-a-docs` (git)
**NEXT:** Visual direction — contrast calculations, MARBLE LIGHT and OBSIDIAN NIGHT mockups, comparison; then Stage B.

---

## CHECKPOINT A2 — Visual direction complete

**NODE:** Stage A / visual proposals
**FINDING:** WCAG contrast computed programmatically for both palettes; all body-text pairs ≥ 4.5:1, all large/UI pairs ≥ 3:1. Obsidian Night wins the always-on comparison (power, heat, burn-in, ambience); Marble Light retained as daylight/inspection theme (decision D-009).
**ACTION:** Built three annotated HTML mockups (Marble Light console, Obsidian Night console, side-by-side comparison with state tiles and verdict) rendered to PNG via headless Chromium; wrote VISUAL_DIRECTION.md.
**TEST:** Playwright render succeeded (non-blank screenshots); contrast script output recorded in VISUAL_DIRECTION.md §3.
**FILES:** design/mockups/{mockup_marble_light.html, mockup_obsidian_night.html, mockup_comparison.html, *.png}, docs/VISUAL_DIRECTION.md, scripts/contrast_check.py, scripts/render_mockups.py
**COMMIT OR CHECKPOINT:** `stage-a-visuals` (git)
**NEXT:** Stage B — buildable Android prototype.

---

## CHECKPOINT B1 — Gradle scaffold and core domain complete

**NODE:** Stage B / buildable prototype, core layers
**FINDING:** The toolchain cannot compile here, so correctness had to come from discipline: versions pinned in a version catalog, domain kept pure Kotlin (JVM-testable), and every cross-file wiring point chosen from stable, well-known APIs. The state-machine table was fixed first in docs, then encoded verbatim.
**ACTION:** Created the full Gradle scaffold (settings, root/app build files, version catalog: AGP 8.7.3 / Kotlin 2.0.21 / Compose BOM 2024.12.01 / Gradle 8.9 / minSdk 30), manifest with justified-but-unrequested permissions, resources (strings, XML theme, adaptive vector launcher icon), and the core Kotlin: ConsoleState (15 states), ConsoleStateMachine (legal table + three universal rules), ConsoleStateStore (single source of truth), OperatingMode, IntentRouter + RuleBasedIntentRouter (deterministic table), the 14 domain engine contracts, TowerContract models (hermes.console.v0.1), MockTowerBridge with injectable faults, DeterministicTowerSimulator, MockLocalConversationEngine, MockTextToSpeechEngine, inactive voice stubs, LogRedactor, InMemoryEventStore, RedactedDiagnosticsExporter, SettingsRepository, SessionAndSecrets, AndroidDeviceHealthMonitor (real device data), VoiceConsoleService skeleton (registered, not started — D-011), AppGraph/HermesApp/AppGraphHolder.
**TEST:** Structural sanity across Kotlin files (brace/paren balance, no TODO/FIXME/XXX markers) — PASS; cross-wiring review of constructor signatures and call sites — PASS.
**FILES:** 34 main-source Kotlin files + gradle scaffold + manifest + resources (see FINAL_REPORT §2).
**COMMIT OR CHECKPOINT:** `stage-b-prototype` (git)
**NEXT:** Compose UI layer (theme, TowerCore, 8 screens, navigation) + unit tests.

---

## CHECKPOINT B2 — Compose UI, Tower core, navigation and tests complete

**NODE:** Stage B / visual system and verification suite
**FINDING:** The animated Tower required one continuous clock instead of several infinite transitions, so reduced motion can freeze every state cleanly; a universal "any state → IDLE" rule was added to the machine (and mirrored into UI_STATE_MACHINE.md) so Owner barge-in never needs an illegal edge. A CSS-class collision found in the HTML mockups (.tower vs .who.tower) was the visual-layer reminder to grep for shadowed names in Compose too.
**ACTION:** Implemented the theme layer (both palettes as code, typography tokens, shapes, motion tokens), TowerCore Canvas with per-state visual profiles and static reduced-motion variants, Meander/Pediment drawn motifs, chrome components (pills, chips, section cards, truth badges), ConsoleScreen (status strip, tower stage, transcript, captions, controls, burn-in drift, dim scrim, text-injection dialog) and the other seven screens, drawer navigation over all eight surfaces, MainActivity with keep-screen-on binding, and six JVM test classes (26 cases) including full-pipeline integration tests on virtual time. Bridge dispatcher made injectable so tests control virtual time.
**TEST:** Structural sanity over all 50 Kotlin files — PASS; contrast script re-run — ALL PASS; Mermaid re-validated — 7/7 OK.
**FILES:** 16 UI Kotlin files + 6 test files; UI_STATE_MACHINE.md amended (universal rule 3).
**COMMIT OR CHECKPOINT:** `stage-b-prototype` (git)
**NEXT:** Stage C — honest build assessment, top-level docs, packaging.

---

## CHECKPOINT C1 — Build assessment, packaging, mission report

**NODE:** Stage C / build and validation
**FINDING:** Environment exposes Java 21 but no Gradle, Kotlin compiler or Android SDK. Per the mission contingency ("do not fabricate success"), the delivery is a complete source package with exact reproducible instructions; missing dependencies are named precisely and the risk is registered (R-16). Nothing was claimed built.
**ACTION:** Wrote README.md (orientation + honest status), BUILDING.md (exact commands: wrapper regeneration, Studio path, CLI path, install + SHA-256 capture, expected first-run behavior), FINAL_REPORT.md (executive summary, inventory, decisions, build status, tests, blockers, device-data needs, next action). Packaged the full project (including git history) as hermes-voice-console-android-v0.1.zip in the delivery folder alongside the three design previews.
**TEST:** ZIP integrity verified (unzip -t); inventory diffed against the file tree; no placeholder markers in text deliverables; preview PNGs verified non-blank.
**FILES:** README.md · BUILDING.md · FINAL_REPORT.md · hermes-voice-console-android-v0.1.zip
**COMMIT OR CHECKPOINT:** `stage-c-package` (git)
**NEXT:** Owner opens the project in Android Studio (BUILDING.md §2) and reports the compile/test outcome; then the post-reset DEVICE_QUALIFICATION_CHECKLIST session on the Xiaomi.

---

## CHECKPOINT A4R-001 — GRAPH A4R nodes A0–A8 executed (two-candidate reconciliation)

**NODE:** GRAPH A4R / A0 resume → A1 differential → A2 keystore repair → A3 QR wiring → A4 visual manifest → A5 canonical recommendation → A6 contract delta → A7 resource truth → A8 ledger/runbook
**FINDING:** The Owner's physical crash evidence binds byte-exactly to Candidate B (FP-B-G4 908b289f…, com.hermestower.voiceconsole.debug 1.0.0-g4): `KeyGenerator.init(256)` without a KeyGenParameterSpec at TowerStore.kt:35 crashes every QR pairing on the Xiaomi BEFORE any network call (3 MIUI screenshots, decompile-confirmed). The Tower-side Graph 4R selection (09:35Z, REUSE_AND_EXTEND_B) is OLDER than the crash evidence (09:50Z) and is superseded by the A4R directive: no candidate is canonical by assumption. Candidate A (FP-A-UPLOAD, same size as CI Package 9 but a fresh debug key 07:59Z) is a same-lineage rebuild; its QR lane exists and is quality but reads only the candidate hermes-pair://v1 scheme, its pairing executors are mock-only, and its Tower visual lacked the mission's 680×963 master-canvas formalization.
**ACTION:** A0: physical probe of both APKs (identity, signing certs via APK Signing Block + keytool, dex inventories, B jadx decompile of TowerStore/PairingFlow/DeviceKeys/QrPayload) + receipts. A1: 50-axis capability matrix with source evidence. A2: drop-in repair package for the Tower account (corrected TowerStore.kt with explicit AES-256-GCM spec, 5 typed failure states, deterministic invalidation class, atomic commitPairing, 9 real-AndroidKeyStore instrumented tests + 7 JVM framing tests + PairingFlow/Screen delta notes) delivered as A4R_CANDIDATE_B_KEYSTORE_REPAIR.zip. A3: PairingEntryActions (numeric 8-digit lane, manual https endpoint lane, six mission typed states with §23-localized labels), TowerStudio dialogs, onboarding QR choice now DEMONSTRATES the scanner, QrScanner typed-state surface; 13 new JVM tests. A4: TowerLayerManifest (680×963, per-layer x/y/w/h/pivot/z/variant/anchor, shared contain transform scale=min(cw/680,ch/963)), ReferenceTowerVisual refactor (manifest-driven, no device offsets), 7 JVM golden tests + offline PIL golden harness PASS (6 renders, placement diff ≤1 px). A5: weighted scorecard (17 criteria + 3 hard gates) → MERGE_WITH_B_AS_BASE recommended INTO this canonical repository, conditional C1..C5, merge/rollback plans. A6: contract delta CD-A4R-001 (client-side QR v2 adoption; 18-endpoint surface registered; 18-row contract test matrix; no Tower core modification). A7: resource truth receipt (artifact facts measured; device protocol R1..R10 defined). A8: physical ledger initialized (1 CONFIRMED row, 28 PENDING) + Italian Xiaomi runbook.
**TEST:** structural sanity PASS on all touched/new Kotlin files (balanced, no markers); localization parity preserved (14 new keys ×5 locales + 6 StateLabels entries ×5); scorecard arithmetic computed and verified (A 6.16 / B 5.15, weight sum 104); golden harness A4R_TOWER_GOLDEN_OFFLINE_PASS; secret scan of produced artifacts clean. CI unit run + connectedAndroidTest pending (no Android SDK in this environment — same honest limitation as every prior sandbox checkpoint).
**FILES:** docs/a4r/* (13 files) + keystore_repair/ (7 files); app/src/main: PairingEntryActions.kt NEW, TowerLayerManifest.kt NEW, TowerStudioScreen/OnboardingScreen/QrScannerScreen/StateLabels/strings×5 MODIFIED, ReferenceTowerVisual.kt REFACTORED; app/src/test: PairingEntryActionsTest.kt NEW, TowerLayerManifestTest.kt NEW.
**COMMIT OR CHECKPOINT:** `graph-4/a4r-two-candidate-reconciliation` (git)
**NEXT:** Tower account integrates A2 + rebuilds B (same debug keystore) → Owner retests QR pairing on Xiaomi (A8-04) → CI runs the new suites → Owner accepts visual alignment → canonical decision gate C4.


## GRAPH A4R CONTINUATION BLOCK C1-C4 (2026-11-20) — merged B-base repair candidate

**FINDING:** The mission's declared B-source authority (Paolo83Ita/Hermes-Tower apps/android-voice-console) is UNREACHABLE (GitHub 404 fresh-read; no local checkout; no bundle) while the mission also requires one CI-built repaired APK. The honest resolution is the A5 merge-plan fallback (M1 authority_core_port): B's authority lane implemented as FRESH canonical source bound to B's forensic contract surface (byte-exact APK 908b289f…, dex inventory, jadx behavioral spec, CD-A4R-001), inside the visible-lineage area candidate-b-base/. CANDIDATE_B_ORIGINAL_SOURCE_BOUND_PASS is therefore typed BLOCKED_WITH_TYPED_EVIDENCE — never faked.
**ACTION:** branch graph-a4r/merge-with-b-base from d133087 → candidate-b-base/ (repaired TowerStore with the 7 mission typed states + PairingStateStore seam; QrPayload v2; DeviceKeys EC-P256; PairingFlow machine with 120 s challenge TTL and repaired persistence mapping; TowerApi 18 routes behind TowerHttp; TowerPairingController with the 11 mission states) → AppGraph rewired: production pairing = the REAL controller; MockTowerPairingClient REMOVED from production (NO_MOCK_PAIRING_IN_PRODUCTION_PATH), retained in tests + SIMULATED self-test lane → QrScannerScreen + TowerStudio dialogs rewired (camera denial split DENIED/PERMANENTLY_DENIED; numeric lane 6 digits per mission §7, prior 8-digit claim filed as contract ambiguity AMB-1) → StateLabels +5 mission states ×5 locales, +18 v2 strings ×5 locales → BlobFramingTest + TowerStoreInstrumentedTest + DeviceKeysInstrumentedTest landed in the repo → dedicated android-keystore-instrumented.yml emulator lane (SOFTWARE_ANDROID_KEYSTORE_TESTED) → android-debug-build.yml: branch trigger + secret scan + APK identity + signing fingerprint + build receipt → TowerContractSimulator + 15 network fixtures + 10 QR fixtures + pairing-machine suite → CD-A4R-C1-001 contract delta request (7 typed ambiguities) → debug package strategy (.a4rdebug suffix, versionCode 13) → local verification.
**TEST:** unit 566/566 GREEN (68+ suites; 33 new A4R tests: 15 contract fixtures, 10 QR fixtures, 12 pairing-machine/controller/scan-flow tests, BlobFraming 7 counted in the 566); lint 0 errors (classified warnings only); compileDebugAndroidTestKotlin green; assembleDebug 3 variants aapt2-verified (com.hermes tower.voiceconsole.a4rdebug / 0.4.0-alpha-grapha4r-merged-b-base / 13); HMAC vector pinned (RFC 4231 class); org.json real artifact added TEST-ONLY (android.jar stub shadows; production untouched).
**FILES:** candidate-b-base/ (10 files incl. manifest+lineage), app/src/main: AppGraph/QrScannerScreen/TowerStudioScreen/PairingEntryActions/StateLabels/strings×5/build.gradle.kts MODIFIED; app/src/test: towercontract/ (4 NEW), pairing/PairingEntryActionsTest MODIFIED, data/BlobFramingTest NEW; app/src/androidTest: data/TowerStoreInstrumentedTest NEW, pairing/DeviceKeysInstrumentedTest NEW; .github/workflows: android-keystore-instrumented.yml NEW, android-debug-build.yml MODIFIED; docs/a4r/c1/ (7) + ANDROID_TOWER_CONTRACT_DELTA_REQUEST.json NEW; governance truth/checkpoints/worklog/blocker/decision updated.
**COMMIT OR CHECKPOINT:** `graph-a4r/c1-c4-merged-b-base-repair` (git)
**NEXT:** push → CI gates → download-back → Owner mini-gate P1-P8 (Xiaomi) → visual Owner acceptance → canonical base decision remains Owner-gated.