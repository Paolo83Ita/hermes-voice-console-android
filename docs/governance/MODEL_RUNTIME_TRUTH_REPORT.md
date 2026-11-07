# MODEL RUNTIME TRUTH REPORT (GRAPH 4.12 §8)

This report is GENERATED at runtime by `ModelRuntimeReceiptRecorder` after
each activation attempt (the device copy lands in app files as
`model_runtime_truth.properties`, secret-clean). This file pins the CONTRACT
and records the standing truths that do NOT change with activations.

## Receipt contract (per downloaded component)

component identity · version · expected SHA-256 · actual SHA-256 · storage
path class · runtime identity · runtime version · load start · load result ·
memory before/after · functional input class · functional output summary ·
latency · unload result · final state · fallback.

## Standing truths (unchanged by activations)

| Component | Truth | Reason |
| --- | --- | --- |
| Vosk Italian (org.vosk.model.small-it-0.22) | Capability ladder enforced; fixture leg runs the REAL engine on the REAL speech wav (ST-55); the microphone leg is a separate physical qualification | §9 |
| Riccardo (org.piper.voice.it-riccardo-x-low) | ACTIVE only after the six-condition chain (files → runtime → load → synthesis → playback → read-back); a failed runtime is INSTALLED_RUNTIME_NOT_AVAILABLE, never ACTIVE | §10 |
| Paola (org.piper.voice.it-paola-medium) | Same chain as Riccardo (ST-57) | §10 |
| Qwen2.5 0.5B GGUF | LOCAL_LLM_RUNTIME_UNAVAILABLE — download/verify/install/inspect/removal allowed; activation, response attribution and benchmark claims BLOCKED | §11 |

## Secret-clean statement

No user text, no audio bytes, no secret paths. Only path classes, hashes,
identities, timings and states are recorded.
