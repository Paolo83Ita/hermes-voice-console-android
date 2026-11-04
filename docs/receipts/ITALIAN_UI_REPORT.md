# ITALIAN UI REPORT — Graph 4.6 (§1 ITALIAN_UI_COMPLETE)

Directive: GRAPH 4.6 §1 — segmented audits A–F, every screen's visible user-facing
text under Italian, ordinary English banned (reviewed allowlist: Hermes Tower, model
names, provider names, URLs, hashes, protocol identifiers, diagnostic codes,
filenames, version strings), state `ITALIAN_UI_COMPLETE`.

## 1. What enforces the state (not a manual claim)

The gate is EXECUTED, not read: six instrumented classes run the REAL product path
under Italian (AppLanguage.apply + the system-driven per-app-locale relaunch), collect
the visible semantics tree (Text + contentDescription) at every audited surface, and
classify every token against (1) the app's own 652-key Italian resource pool built at
runtime, (2) the reviewed technical shapes (ALL-CAPS codes, camelCase, URLs, versions,
snake/kebab, units, separators) + invariant identities (HERMES/TOWER/MODEL
SANCTUM/SIMULATED/STT/TTS/QR…), (3) the reviewed ordinary-English banned list
(100+ words). ONE banned ordinary-English token fails the segment; ANY failed segment
fails the cloud job; the cloud verdict — not this document — releases the state.

## 2. The six segments (directive A–F mapping)

| Segment | Class | Audited surfaces |
| --- | --- | --- |
| A | ItalianGateA_OnboardingConsoleTest | six-step onboarding wizard (REAL walk: language → Tower → voice pack → LLM → voice test → finish), Console (top bar, oracle truth row, engine-truth line, composer), text-injection dialog (title/placeholder/buttons), onboarding-flag persistence |
| B | ItalianGateB_ChatHistoryArchiveTest | chat composer + timeline + honest attribution line, injection dialog, History, Tower Archive (all three tabs: Conversations/Missions/Artifacts + search + filters + pin/provenance rows) |
| C | ItalianGateC_TowerGateQrScannerTest | Tower Gate (Connection), Tower Studio, QR scanner (full-screen; entered and left via its OWN Back path — asserted) |
| D | ItalianGateD_ModelsAndVoiceTest | Model Sanctum (catalog cards, attribute sheet expanded: source/license/identity/hash rows, "Included with the application" honesty label) |
| E | ItalianGateE_SettingsAppearanceLanguageTest | Settings hub + all six categories (GENERAL/VOICE/TOWER CONNECTION/MODELS/DISPLAY/DIAGNOSTICS), Appearance, Voice, Language |
| F | ItalianGateF_DiagnosticsSelfTestAboutTest | Diagnostics (+ Active-runtime card), Self-Test Center, Local Intelligence (+ the new optional-LLM card), About/Safety |

## 3. Evidence chain to date (all runs parsed from artifacts)

| Run | Commit | Outcome | Ledger |
| --- | --- | --- | --- |
| 41833663387 | 01493a1 | real Italian UI walked end-to-end (dump-proven: CAMERA DELL'ARCHIVIO, Nessuna conversazione…, localized engine-truth row); QR/archive-region system ANR | R-4 |
| 41840691263 | 01493a1 | destination composes past the 20 s wait (dump-proven) | R-7 |
| 41868118783 | b3b78ed | FIRST segmented-model execution: A1 anchor case-transform (SCEGLI vs Scegli), A2/B relaunch-race on dialog state, C unbounded waitForIdle hang | R-8, R-9, R-10 |
| 41868118783 (regression leg) | b3b78ed | the dispositioned lifecycle residual crashed a SECOND class (E2EFreshInstallTest) → containment upgraded to per-class invocations | R-11 |
| 41884838553 | c97a02f | CP-G4-021 splits renamed the APK outputs; E2E install referenced the dead name → job died before instrumentation | (workflow defect, CP-G4-023 fix 2) |
| 41886819… | bba1ebc | **this session's final run: all repairs active** | verdict below |

## 4. Italian rendering evidence (physically dumped on the emulator)

- CONSOLE ORACOLO VOCALE / Archivio conversazioni → / Scrivi un messaggio… / parla
  invece / stop / invia / mic: push-to-talk (riconoscitore di sistema) · testo: di
  prima classe · voce uscita: TTS di sistema Android · STT offline + voce naturale:
  in attesa in Modelli e voce · tower: SIMULATA fino a Tower Graph 1.7
- CAMERA DELL'ARCHIVIO + Nessuna conversazione. Parla con il microfono o scrivi un
  messaggio — tutto ciò che è mostrato è l'output reale della console.
- SCEGLI LA TUA LINGUA wizard step + Segui il sistema / Italiano / English / Español /
  Français / Deutsch + Continua
- Drawer: NAVIGATION CHAMBER · PRINCIPALI · Console vocale ATTUALE · Archivio Tower ·
  Connetti a Hermes Tower · Modelli e voce · Impostazioni · v0.2.0-debug · mode HYBRID
  · lang italiano

## 5. Resources

652 string keys × 5 locales (en/it/es/fr/de) — parity enforced by
LocalizationParityTest (key sets, non-empty values, positional format args, coverage
areas, owner terminology anchors). The five-language completion state (§5 of the
directive) rides the same gates: EN/ES/FR/DE parity is a unit-gate invariant; the
Italian E2E proves the rendering path end-to-end in a real locale.

## 6. VERDICT

<!-- VERDICT_PLACEHOLDER -->

## 7. Honest boundaries

- The gate runs on the x86_64 software emulator; the PHYSICAL Italian rendering
  review (fonts, truncation, overflow on the 2400×1080 Xiaomi panel) is part of the
  Xiaomi package §3 (runbook §5).
- Technical identifiers (HERMES, TOWER, MODEL SANCTUM, SIMULATED, SHA-256, model
  names, diagnostic codes) remain untranslated BY DESIGN (directive allowlist).
