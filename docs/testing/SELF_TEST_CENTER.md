# SELF TEST CENTER — Graph 2 implementation

Surface: Diagnostics → **Self-Test Center**; also "Run Self-Test" from Diagnostics and
onboarding first-run guidance. Runs fully offline (127.0.0.1 mock server only), sends no
data anywhere, and needs no Tower.

## Modes

| Mode | Target | Contents |
| --- | --- | --- |
| QUICK | < 30 s | ST-01,02,03,04,05,12,13,14,18,21,22,23,24,27,28,30,31,41,42,43 (20 tests) |
| STANDARD | 1–3 min | everything except the long download ladder (ST-06..11,19,20,25,26,29,32,34,35,36 added) |
| FULL | ≤ 10 min | all ST-01..ST-45 (excludes soak tests) |
| CONNECTIVITY_ONLY | < 1 min | ST-04..ST-12, ST-27, ST-28 |
| VOICE_FOUNDATION_ONLY | < 2 min | ST-13..ST-24, ST-30, ST-31 |

## Test battery (45)

ST-01 identity/version · ST-02 configuration schema · ST-03 secure-storage availability ·
ST-04 profile validation · ST-05 mock handshake · ST-06 request acknowledgement ·
ST-07 streaming response · ST-08 cancellation · ST-09 disconnection · ST-10 reconnection ·
ST-11 timeout · ST-12 authentication failure · ST-13..17 OFF/LOCAL/TOWER/HYBRID/SAFE modes ·
ST-18 privacy mute · ST-19 queue truthfulness · ST-20 consequential confirmation ·
ST-21 state-machine transitions · ST-22 reduced motion · ST-23 burn-in drift ·
ST-24 theme switching · ST-25 navigation coverage · ST-26 process-recreation state ·
ST-27 config save/read-back · ST-28 secret-clean export · ST-29 import conflict handling ·
ST-30 catalog loading · ST-31 voice-pack recommendation · ST-32 probe download ·
ST-33 pause/resume · ST-34 cancellation · ST-35 hash verification · ST-36 mismatch
rejection · ST-37 atomic install · ST-38 activation read-back · ST-39 rollback ·
ST-40 removal cleanup · ST-41 diagnostics redaction · ST-42 accessibility semantics ·
ST-43 no fabricated metrics · ST-44 restart recovery · ST-45 migration compatibility.

## Result shape

`SelfTestResult{ testId, testName, startTime, finishTime, durationMs, state, evidence,
failureType, repairSuggestion, logsReference, environment, appVersion, commitReference }`
— states: NOT_RUN · RUNNING · PASS · FAIL · SKIPPED_NOT_APPLICABLE · BLOCKED_DEPENDENCY ·
QUALIFICATION_PENDING_DEVICE. Nothing is invented: a check either ran and produced
evidence, or reports why it could not.

## Controls

Run Quick / Run Standard / Run Full / Connectivity only / Voice foundation only · Stop ·
Retry Failed Tests · Export Secret-Clean Report (every field passes LogRedactor before
leaving the app) · Copy Summary · Open Related Setting (screens are one tap away) ·
Reset Test History.

## Determinism rules

Every check is bounded (per-run timeout 120 s); localhost mock server behaviors are
explicit (no sleeps); downloads use chunk-paced deterministic artifacts; results survive
only in memory (resettable), while persistence restart recovery is checked against the
real stores. Physical-device-only behaviors (microphone acoustics, real LLM inference,
thermal soak) are never claimed here — they remain QUALIFICATION_PENDING_DEVICE.
