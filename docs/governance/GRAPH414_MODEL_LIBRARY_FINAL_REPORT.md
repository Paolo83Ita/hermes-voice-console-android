# GRAPH 4.14 — MODEL LIBRARY FINAL REPORT (CP-G4.14-C05)

§8: the Model Library (Models and Voice) shows truthful product states.
Presentation layer only — the model authority (catalog/registry/receipts/
download chain) is UNTOUCHED from Graph 4.13.

## NODE
CP-G4.14-C05 — Model Library final qualification.

## FINDING — required truth properties, all verified in code at the final tip

| Requirement | Implementation | Verified |
| --- | --- | --- |
| No raw enum in ordinary UI | ModelUxSemantics maps every record state → Italian text; enums surface ONLY inside the expandable technical details | code + ModelUxSemanticsTest |
| No generic "Compatibility: false" | CompatVerdictResolver → 13 typed verdict kinds (incl. RUNTIME_UNAVAILABLE: "Non utilizzabile: il runtime Android richiesto non è disponibile." — values-it strings.xml) | ModelManagerScreen L398–419 + test |
| ONE filled primary action per card | ModelActionResolver → exactly one primary action per state (Download / Install / Qualify / Activate / CHECK_RUNTIME / Repair …) | ModelActionResolver + mapping test pin (RUNTIME_RESOLVED→CHECK_RUNTIME, CP-G4.14-009) |
| Runtime-missing reason | RUNTIME_UNAVAILABLE verdict + runtimeRequirement per card (catalog → ModelCatalogEntry.runtimeRequirement → resolver) | code |
| Installed vs active distinction | ModelInstallState 18 values; ModelCapabilityTransitions forbids INSTALLED→ACTIVE / DOWNLOADED→ACTIVE skips | ladder tests |
| Local LLM status reflects REAL integration | selectability keyed to `LocalLlmEngine.hasVerifiedRuntime` (llama.cpp in-tree since CP-G4.14-005); data-driven — disables honestly if the runtime disappears | §25 test + LocalLlmEngine |
| Paola/Riccardo reflect sherpa runtime | VoiceRuntimeFactory = two SherpaOnnxTtsEngine instances; routedNaturalVoiceId returns natural routes ONLY when qualified/ACTIVE | TtsRouteSwitch + ST-56/57 |
| Vosk reflects real runtime | vosk-android 0.3.75 INTEGRATED; VoskFrameSttEngine loads the real Model | runtime audit C02 |
| GTCRN reflects ONNX runtime | GtcrnDenoiserEngine real streaming ORT | C02 |
| Probe artifacts hidden | `org.hermestower.test.probe-artifact` filtered from ordinary UI (PROBE_IDENTITY) | ModelManagerScreen |
| Technical details expandable | details live behind the expandable section (13 typed verdict kinds listed there) | ModelUxComponents |
| Compact profile selector | 4.13 compact selector preserved untouched | CP-G4.13-008 |
| No vertical word wrapping | 4.13 §22 responsive layout (full-width stacking at compact width) preserved | 4.13 tests |
| No unreadable low-contrast metadata | Marble metadata tokens ≥4.5:1 (contrast report rows 39–52) | contrast report |

Per-state truth mapping (state → primary action → user-visible meaning):
NOT_DOWNLOADED→Download · DOWNLOADED→Install · HASH_VERIFIED→Qualify ·
QUALIFIED→Activate · ACTIVE→(in use) · RUNTIME_RESOLVED→CHECK_RUNTIME ·
INSTALLED→CHECK_RUNTIME · FAILED→Repair · INSTALL_FAILED→Repair ·
QUALIFY_FAILED→Repair · activation-failures→Repair-family ·
RUNTIME_UNAVAILABLE verdict → the typed Italian reason (no download button
masquerading as usability).

## ACTION
Re-verified every property at the final tip (post CP-G4.14-009 + this
continuation's string repairs). No Model Library code change was needed —
the 4.13/4.14 truth layer held; the orphaned `mm_*` strings removed this
session were verified zero-reference before deletion.

## TEST
- ModelUxSemanticsTest + capability-ladder tests + ModelDownloadLifecycleTest
  inside the 496/496 suite; lint 0/72.

## FILES
- models/ModelUxSemantics.kt, models/ModelCatalogEntry.kt,
  ui/screens/ModelManagerScreen.kt, ui/screens/ModelUxComponents.kt (verified),
- values*/strings.xml (13 orphaned keys removed).

## EVIDENCE
- Test suite results; code line references above; runtime audit C02.

## CHECKPOINT
CP-G4.14-C05 — Model Library final qualification: DONE.

## NEXT
CP-G4.14-C06 — Tower layer + animation audit.
