# DECISION LOG — HERMES VOICE CONSOLE ANDROID

| | |
|---|---|
| Owner | Paolo Lombisani |
| Version | v0.1.0-alpha |
| Date | 2026-10-30 |
| Policy | One row per consequential decision; superseded rows are kept, never deleted. |

| ID | Decision | Rationale | Alternatives considered | Consequence | Date |
|---|---|---|---|---|---|
| D-001 | Kotlin + Jetpack Compose for the entire UI; Material 3 adopted selectively under the Hermes theme | Declarative Canvas-driven UI fits the animated Tower; theming flexibility | XML views; Flutter; web wrapper | Compose BOM pinned in catalog; M3 components used only where they serve the identity | 2026-10-30 |
| D-002 | Single Gradle module `:app` in V0.1 with package-level modularity | Nothing is device-qualified yet; module extraction without a second consumer is speculative | Multi-module from day one | FILE_TREE_PROPOSAL §B defines the extraction plan | 2026-10-30 |
| D-003 | Manual service locator (AppGraph) instead of DI framework in V0.1 | Keep the un-compiled toolchain surface minimal and honest; Hilt is PROTOTYPE at V0.4 | Hilt now; Koin | DI seam is one file (AppGraph) | 2026-10-30 |
| D-004 | SharedPreferences for UI settings in V0.1; DataStore/Room deferred until data model stabilizes | No sensitive data exists yet; fewer dependencies; history is session-scoped by design in V0.1 | DataStore now; Room now | SettingsRepository isolates the choice | 2026-10-30 |
| D-005 | V0.1 transport is mock-only (MockTowerBridge + DeterministicTowerSimulator); no OkHttp dependency yet | Mission rule: no real routes, no fake connectivity; unused network stacks would imply capability | Include OkHttp scaffold | Real adapter added in V0.4 behind TowerBridge | 2026-10-30 |
| D-006 | No microphone capture in V0.1; wake/listen/STT simulated by text injection | Mission stage B constraint; permission-safe capture is a V0.2 spike | Capture stub with real AudioRecord | AudioCaptureEngine interface defined but no adapter shipped | 2026-10-30 |
| D-007 | minSdk 30 (Android 11), targetSdk 35, JDK 17 toolchain | Covers target device lineage (A-03) and current Android Studio defaults | minSdk 33 | Deprecation edges guarded in code | 2026-10-30 |
| D-008 | AGP 8.7.3 + Kotlin 2.0.21 + Compose BOM 2024.12.01 + Gradle 8.9 | Stable, mutually compatible set available in current Android Studio | Newer 2025/2026 sets (unverifiable in sandbox) | A-30 records this as OPEN until Owner builds | 2026-10-30 |
| D-009 | Obsidian Night is the recommended default theme; Marble Light is the daylight/inspection theme | Always-on AMOLED: power, heat, burn-in, night ambience; see VISUAL_DIRECTION §4 | Marble Light default; time-based switch | Default theme constant = OBSIDIAN_NIGHT; switch proposal scheduled V0.2 | 2026-10-30 |
| D-010 | System fonts in V0.1; bundled display/UI typefaces evaluated in V0.2 | No license text can be verified in sandbox; identity survives on system serif/sans/mono pairing | Bundle OFL fonts now | Typography token layer makes the swap one-file | 2026-10-30 |
| D-011 | VoiceConsoleService registered but not started in V0.1 | Foreground service with microphone type without capture would be dishonest plumbing | Omit service entirely | Diagnostics shows "registered, not started" truthfully | 2026-10-30 |
| D-012 | English UI in V0.1; Italian localization scheduled V1.0 (A-12) | Owner's mission documents are English; voice is Italian-first from V0.3 | Italian UI now | All strings centralized in strings.xml for cheap i18n | 2026-10-30 |
| D-013 | Wrapper JAR not committed; regeneration documented | Binary artifact cannot be produced in sandbox without download; A-31 tracks acceptance | Commit a downloaded jar | BUILDING.md documents one-command regeneration | 2026-10-30 |
| D-014 | Deterministic canned simulation (no randomness) for the V0.1 demo loop | Testable, truthful, reproducible; randomness would fake liveliness | Random script injection | ScriptedConsoleDriver cycles a fixed utterance set | 2026-10-30 |
| D-015 | Burn-in guard = bounded slow drift (≤ 8 dp, 60 s cadence) + dark default + brightness ceiling plan | AMOLED static-content risk (A-10) with minimal motion perception | Full screensaver only | Guard toggleable; disabled by reduced-motion | 2026-10-30 |
