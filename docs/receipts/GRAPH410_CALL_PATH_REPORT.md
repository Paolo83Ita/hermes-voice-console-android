# GRAPH 4.10 — CALL PATH REPORT (CP-G4.10-001)

Three authoritative paths mapped BEFORE source modification. Rule: no parallel
authorities — every edge below is REUSED, wrapped or made locale/state-aware;
none is replaced by a second implementation.

---

## CALL PATH A — Italian input → response text → visible timeline → TTS

| # | Edge | Source file | Class.function | Authority today | Current value | Failure point | Reuse decision |
| --- | --- | --- | --- | --- | --- | --- | --- |
| A1 | Text/voice turn enters pipeline | `simulator/ScriptedConsoleDriver.kt` | `submitText()` / `submitUtterance()` → `runPipeline(text, conf)` | ConsoleStateStore (single state authority) | text trimmed; privacy-mute gate; IDLE transition | — | REUSE unchanged |
| A2 | Intent routing | `core/routing/RuleBasedIntentRouter.kt` | `route(RoutingRequest)` → `RoutingDecision` | RuleBasedIntentRouter (single router) | Returns action + intent + reason (English reason strings) | Reason text surfaces in English decline message | REUSE; localize DECISION-LEVEL messages at the RESPONSE layer, not inside the router |
| A3 | Local response selection | `local/MockLocalConversationEngine.kt` | `respond(text, statusProvider)` | LocalConversationEngine seam | **English-only canned strings; no locale parameter** | **P0-01 ROOT EDGE** | EXTEND: `respond(text, statusProvider, locale)` consuming `ResponseLocaleResolver` |
| A4 | Local response → visible timeline | `simulator/ScriptedConsoleDriver.kt` | `runPipeline` HANDLE_LOCAL → `store.appendTurn(Role.HERMES, reply.text, …)` | ConsoleStateStore.turns | Visible text == engine output (English) | P0-01 visible | REUSE (text becomes Italian once A3 is localized) |
| A5 | Decline / error / status notes | `simulator/ScriptedConsoleDriver.kt` | `"I cannot do that. "`, `"Tower accepted the request."`, `"The request failed at the Tower (…)"`, queue/error notes | — | Hard-coded English at 12+ sites | P0-01 visible/audible | REPLACE with `ResponseCatalog` entries keyed by `ResponseLocaleResolver` |
| A6 | Tower (mock) deterministic reply | `bridge/DeterministicTowerSimulator.kt` | `respond(transcript)` → `MockTowerBridge` DELTA/FINAL frames | DeterministicTowerSimulator | **English-only responses incl. SIMULATED disclaimer** | P0-01 visible/audible | EXTEND: `respond(transcript, locale)`; SIMULATED truth preserved per locale |
| A7 | Visible assistant text → TTS input | `simulator/ScriptedConsoleDriver.kt` | `speakAndFinish(text, captions)` → `tts.speak(text, rate)` | TextToSpeechEngine (TtsRouteSwitch) | TTS input == visible text (already equal) | — but equal to ENGLISH | KEEP invariant §6 `visibleAssistantText == ttsInputText`; both become Italian |
| A8 | TTS engine + voice negotiation | `voice/AndroidSystemTtsEngine.kt` + `voice/ItalianTtsVoiceNegotiator.kt` + `voice/TtsSpeakGate.kt` | `speak()` → negotiate Italian → fail-closed | ItalianTtsVoiceNegotiator (fail-closed) | Italian requested; English output physically blocked | — (P0-02 proves engine OK) | REUSE unchanged; gate now also verifies selected-voice authority (§12) |
| A9 | Route to natural/system engine | `tts/TtsRouteSwitch.kt` | `speak()` → `routeProvider()` → route | AppGraph.activeNaturalVoice() | Registry+disk truth | **P0-03: selection exists but Owner heard no change** | WRAP: routeProvider becomes `VoiceSelectionAuthority.activeRuntime()` (§7/§8) |

**Post-repair invariant:** for an Italian UI, edges A3→A4→A7 all carry the SAME
Italian string; TTS gate (A8) accepts only Italian-capable runtime or fails closed.

---

## CALL PATH B — Downloaded voice → selection → runtime → audible voice

| # | Edge | Source file | Class.function | Authority today | Current value | Failure point | Reuse decision |
| --- | --- | --- | --- | --- | --- | --- | --- |
| B1 | Voice catalog | `models/VoiceModelCatalog.kt` | catalog entries | VoiceModelCatalog | riccardo-x-low / paola-medium with pinned SHA-256 | — | REUSE |
| B2 | Download + install | `models/ModelDownloadManager.kt`, `models/VoicePackInstaller.kt`, `models/BundledModelInstaller.kt` | install flow → files on disk | ModelInstallationRegistry | Files verified by SHA-256 at install | — | REUSE |
| B3 | Activation (UI) | `models/ModelActivationController.kt` | activate(componentId) | ModelInstallationRegistry (`active=true, state=ACTIVE`) | UI shows selected/active | **No runtime qualification after selection** | EXTEND: activation completes only through runtime qualification (§7) |
| B4 | Registry read-back → route id | `app/AppGraph.kt` | `activeNaturalVoice()` if-chain | ad-hoc map in AppGraph | identity→engine mapping inlined | **P0-03: no VoiceSelectionAuthority object; no per-activation runtime read-back persisted** | SUPERSEDE: `VoiceSelectionAuthority` becomes the canonical holder (state, SHA-256, qualification, last read-back); AppGraph delegates |
| B5 | Runtime creation | `app/AppGraph.kt` + `voice/SherpaOnnxTtsEngine.kt` | engine constructed eagerly for both voices | per-voice engine instances | Engines exist; load state implicit | No load/synthesis/ playback proof before ACTIVE | SUPERSEDE: `VoiceRuntimeFactory` creates/loads on activation; ONNX+JSON loaded exactly (§8) |
| B6 | Synthesis + playback | `voice/SherpaOnnxTtsEngine.kt` (+ `SystemTtsDomainAdapter` wrapper) | `speak()` | TextToSpeechEngine seam | Amplitude flow, interrupt | — | REUSE |
| B7 | Fallback | `tts/TtsRouteSwitch.kt` | unknown/removed id → `systemRoute` | TtsRouteSwitch | Falls back on next utterance | Silent swap masked as "selected" | REUSE + explicit `INSTALLED_RUNTIME_FAILED` / `TEXT_ONLY` states surfaced (§7/§8) |
| B8 | Fail-closed Italian gate | `voice/TtsSpeakGate.kt` | post-apply read-back gate | ItalianTtsVoiceNegotiator | English never audible when Italian requested | — | REUSE unchanged |

**Activation truth (§7):** a downloaded voice is INSTALLED, not ACTIVE, until
files verified + runtime available + voice loaded + test synthesis + playback +
read-back match. Only then `ACTIVE`; otherwise `INSTALLED_RUNTIME_FAILED` →
fallback (system Italian TTS, else TEXT_ONLY) — never "selected but old voice".

---

## CALL PATH C — Xiaomi available width → final rendered bounds

| # | Edge | Source file | Class.function | Authority today | Current value | Failure point | Reuse decision |
| --- | --- | --- | --- | --- | --- | --- | --- |
| C1 | Window insets | `ui/MainActivity.kt` (edge-to-edge enable) + Compose `WindowInsets` | — | system insets | status/nav bars + cutout applied per-screen | — | REUSE |
| C2 | Available width measure | (MISSING — no width class authority) | — | none | Screens use fixed paddings + fillMaxWidth | **P0-08 ROOT EDGE** | CREATE: `ConsoleWindowWidthClass` (COMPACT <380 dp / MEDIUM 380–420 / LARGE >420) from `BoxWithConstraints` after padding, per §19 |
| C3 | Screen header | `ui/screens/ConsoleScreen.kt` header + `ui/components/Chrome.kt` | title `Text` rows | MonumentalType fixed styles | Fixed font/spacing/height | Clipped/overlapping title at compact | EXTEND: responsive monumental header (§20): ≤2 lines, reduced font at COMPACT, growth allowed at fontScale ≥1.30 |
| C4 | Connection Studio action rows | `ui/screens/TowerStudioScreen.kt` | five fixed `Row(spacedBy(6.dp))` groups | none | 2–3 long-label TextButtons per row | **One-character-per-line ("Ripristina modalità Mock")** | REPLACE rows with `ResponsiveActionGroup` (§21/§22): PRIMARY full-width "Salva e connetti", stacked secondary/management/destructive at COMPACT |
| C5 | Console control row | `ui/screens/ConsoleScreen.kt` | six equal IconButtons in one Row | none | Six equal medallions | Ambiguous priority (§18) | REPLACE with state-driven hierarchy: 1 primary + ≤4 secondary + Altre azioni (§18) |
| C6 | Touch targets | `ui/screens/*` (MedallionDisc 50/62 dp; buttons default 48 dp) | MedallionDisc | Material minimums | OK on most; keep | — | ENFORCE ≥48 dp in ResponsiveActionGroup |
| C7 | Font scale | `ui/theme/Type.kt` (+ system fontScale) | — | sp scaling | No fontScale-aware constraints | Overlap at 1.30/1.50 | HANDLE in ResponsiveActionGroup + header (§20) |

**Width classes (measured, after insets — §19):** Xiaomi 11 Lite 5G NE must
qualify COMPACT or MEDIUM by measured dp; never by physical pixels.
