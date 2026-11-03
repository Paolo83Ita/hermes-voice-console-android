# LOCALIZATION LITERAL AUDIT — CP-G4-017 (directive §3)

Source: `scripts/literal_inventory.json` (extraction script `scripts/extract_literals.py`,
run against `app/src/main/java/com/hermestower/voiceconsole`, comments stripped).
**235 literal occurrences in 16 UI source files** (measured 2026-11-03).

Classification categories per directive §3: MUST_LOCALIZE, TECHNICAL_IDENTIFIER,
MODEL_IDENTITY, FILE_NAME, DIAGNOSTIC_CODE, TEST_ONLY, INTERNAL_NOT_VISIBLE.

## 1. Classification summary

| Category | Count | Rule applied |
| --- | --- | --- |
| MUST_LOCALIZE | 219 | User-visible prose: titles, buttons, captions, dialogs, a11y descriptions, statuses, empty states |
| TECHNICAL_IDENTIFIER | 8 | Brand/visual-identity tokens invariant by Owner §14 + E2E anchors (see §2) |
| MODEL_IDENTITY | 2 | `org.*` candidate identities rendered verbatim |
| DIAGNOSTIC_CODE | 6 | Uppercase single-token diagnostic/status codes |

(The file-level table below carries the authoritative per-literal verdicts; a literal may
carry interpolated values — only the fixed frame around them localizes.)

## 2. Intentionally invariant (allowed English per directive §20/§16)

- `HERMES`, `TOWER`, `HERMES TOWER` — product/brand identity (Owner §14 anchors).
- `MARBLE LIGHT`, `OBSIDIAN NIGHT` — theme product names AND E2E-pinned anchors
  (E2EAppearancePersistenceTest); renaming breaks the pinned evidence chain.
- `ORACLE CORE` — the technical name of the central mechanism (identity, §14).
- `MODEL SANCTUM` — section identity of the Models screen (Graph 4 node name kept as
  the section's proper noun; its descriptive strings localize).
- `ARCHITECTURAL FORGE · VISUAL CALIBRATION CHAMBER`, `ARCHIVE CHAMBER` — classified
  MUST_LOCALIZE (visible captions) despite monumental styling: they are ordinary text.
- Mode labels (`MARBLE`, etc. from RequestedMode.label) come from enums — audited at
  their definition site (core/state), not here.

## 3. File-level inventory and verdicts

### ui/AppNav.kt (2)
| Line | Literal | Verdict |
| --- | --- | --- |
| 170 | `HERMES TOWER` | TECHNICAL_IDENTIFIER (brand header) |
| 349 | `CURRENT` | MUST_LOCALIZE (tower-status chip) |

### ui/monumental/MonumentalChrome.kt (8)
`MODE`, `QUEUED ×$count`, `SUSPENDED SCROLL`, `◤ ` / `◥ ` (glyphs — INTERNAL_NOT_VISIBLE
decorative geometry, keep), `Operating mode $label` (a11y), `$count requests queued for
the Tower` (a11y), `Privacy mute engaged. Microphone disabled.` (a11y) → MUST_LOCALIZE
except the two glyph strings.

### ui/screens/AboutSafetyScreen.kt (13) — all MUST_LOCALIZE
`About & Safety`, `Version`, version sentence, `Components and licenses`,
`Privacy explanation`, `Known limitations (truthful)`, `• $it`, `Danger zone`,
`Reset local data`, `Reset local data?`, reset-explanation, `Reset`, `Cancel`.

### ui/screens/AppearanceScreen.kt (15) — all MUST_LOCALIZE except theme names
`Appearance`, forge caption, `Visual quality (Monumental core)`, `Theme`,
`Brightness`, dimming sentence, `Motion`, intensity sentence, reduced-motion label,
`Display safety`, burn-in label, keep-screen-on label, `Kiosk behavior` → MUST_LOCALIZE;
`MARBLE LIGHT`, `OBSIDIAN NIGHT` → TECHNICAL_IDENTIFIER.

### ui/screens/ArchiveScreen.kt (14) — all MUST_LOCALIZE
`Tower Archive` (title keeps brand `Tower` as product name inside a localized sentence —
localized as a whole), `ARCHIVE CHAMBER`, `Search the archive…`, `Load more` ×3,
`Close`, `Open transcript`, `mission-linked`, `Blockers: …`, `Request continuation`,
`Download`, `Remove local`, `Verify hash`.

### ui/screens/ConnectionScreen.kt (8) — all MUST_LOCALIZE
`Connection`, `Open Hermes Tower Connection Studio →`, `Tower endpoint`,
`Endpoint (placeholder — unused in V0.1)`, `Transport`, `Pairing`, `Tests`,
`No secrets policy`.

### ui/screens/ConsoleScreen.kt (8)
`Open navigation drawer` (a11y) → MUST_LOCALIZE; `HERMES`, `TOWER` → brand; `you: "…"`
caption → MUST_LOCALIZE; `Inject text (STT stand-in)` → MUST_LOCALIZE (TEST_ONLY-labeled
control but visible on the debug console — localize with a TEST_ONLY marker retained);
`Type what you would say…`, `Send`, `Cancel` → MUST_LOCALIZE.

### ui/screens/DiagnosticsScreen.kt (21) — all MUST_LOCALIZE
Screen title, studio link, self-test link, `Device (real values)`, device fields,
`Service state`, `Tests`, `Run bridge test`, `Run audio test`, `Simulation (prototype
controls)` (visible prototype controls — localize; keep TEST_ONLY framing), 6 simulation
buttons, `Redacted log export`, `Generate redacted report`, `Copy full report`.

### ui/screens/HistoryScreen.kt (10) — all MUST_LOCALIZE
`Conversation History`, `Turns (…)`, `Clear history`, `Reset all local data`,
`Clear history?`, explanation, `Clear`, `Cancel`, turn title frame, `Retry (safe: …)`.

### ui/screens/LocalIntelligenceScreen.kt (7) — all MUST_LOCALIZE
`Local Intelligence`, `Local conversation engine`, `Candidates (not endorsements)`,
`$meta — $note`, `Routing policy (HYBRID)`, `Resource limits (targets)`,
`Load / unload controls`.

### ui/screens/ModelManagerScreen.kt (50) — all MUST_LOCALIZE except `MODEL SANCTUM`
Titles/sections (A. Recommended setup, B/C/D/E/F sections), device/profile/compatibility
sentences, `Apply Recommended Configuration`, recommendation prompts, install-pack flow
(+ confirmation dialog incl. `Optional components (including the local LLM) are NOT part
of this pack.`), action verbs (`Download`, `Pause`, `Resume`, `Cancel`, `Verify`,
`Install`, `Qualify`, `Activate`, `Deactivate`, `Roll back`, `Remove`, `Re-download`,
`Repair`, `Update`), `Open diagnostics`, storage-diagnostics lines, `Bundled verified
delivery`, `Status: $it`, licenses line, expected-duration line.
`MODEL SANCTUM` → TECHNICAL_IDENTIFIER.

### ui/screens/QrScannerScreen.kt (9) — all MUST_LOCALIZE
`Scan pairing QR`, `Camera permission`, `Grant camera permission`, `Camera unavailable`,
`Scan result`, `Scan again`, `Payload accepted by the strict parser:`,
`Pair with this Tower (SIMULATED)` (SIMULATED token is a truthfulness contract — keep the
token literal inside a localized sentence), `Back to Tower Studio`.

### ui/screens/SelfTestScreen.kt (17) — all MUST_LOCALIZE
`Self-Test Center`, `Run`, `Run Quick`, `Run Standard`, `Run Full`, `Connectivity only`,
`Voice foundation only`, `Stop`, `Retry Failed Tests`, `Reset Test History`,
`Export Secret-Clean Report`, `Copy Summary`, `Running: …`, `Results`,
`No tests run yet in this session.`, `$id — ${result.testName}` frame, `Repair: $it`.
(ST names are DIAGNOSTIC_CODEs from the engine — audited at the definition site.)

### ui/screens/TowerStudioScreen.kt (43) — all MUST_LOCALIZE
`Connect Hermes Tower` (localized sentence with brand token), Basic/Advanced/Actions/
`Last connection test` sections, every field label (connection name, address, method,
pairing code, owner ref, device name, mode, language, 8 advanced protocol fields,
certificate behavior, pinned fingerprint), every action (`Save draft`, `Validate fields`,
`Test connection`, `Pair device`, `Save and connect`, `Connect`, `Disconnect`,
`Clear sensitive values`, `Export secret-clean profile`, `Import profile`,
`Restore previous`, `Reset to Mock mode`), `Outcome: …`.

### ui/screens/VoiceAudioScreen.kt (9) — all MUST_LOCALIZE
`Voice & Audio`, `Wake word`, `Voice activity detection`, `Speech-to-text`,
`Text-to-speech`, speaking-speed sentence, `Interruption & captions`, barge-in label,
`Captions`.

### ui/MonumentalChrome footer (inside AppNav, counted above)
`v… · mode … · lang …` → MUST_LOCALIZE (visible footer).

## 4. Migration plan (waves, honest status)

- **Wave 1 (CP-G4-017 part 1)**: navigation-critical + most-visible strings — drawer
  destinations (already resources), screen titles/subtitles, chrome a11y, common action
  verbs, Console composer/PTT, footer, dialogs of About/History. NEW screens
  (Settings hub, onboarding) are born-localized.
- **Wave 2 (CP-G4-017 part 2)**: remaining screen bodies (ModelManager, TowerStudio,
  Diagnostics, SelfTest, Archive, QrScanner, VoiceAudio, Connection, LocalIntelligence).
- **Wave 3**: `archival/MockTowerArchive.kt` title (`Mission $m — …` — a SIMULATED-data
  surface; the fixed frame localizes, the data stays labeled SIMULATED).
- Each wave ends with the five-locale parity gate + the Italian completeness
  instrumentation test (directive §4) reporting zero ordinary-English findings against
  the approved technical allowlist above.

Wave-1+2+3 migration status is tracked in the checkpoint receipt; this file is the
audit source of truth and is re-scanned after every UI wave (directive §16).
