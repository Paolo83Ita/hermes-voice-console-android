# GRAPH 2 EMULATOR BASELINE — EMULATOR_ONLY

All numbers below come from GitHub-hosted emulator runs (API 34, x86_64, 2 vCPU,
software acceleration, no KVM). **They are EMULATOR_ONLY baselines and are NOT Xiaomi
performance evidence.** Physical read-backs are a separate qualification graph.

Measured during Graph 2 cloud qualification (values to be refreshed from the final run's
reports before merge; timestamps UTC):

| Metric | How measured | Baseline note |
| --- | --- | --- |
| APK size | artifact size + CI sha256 step | V0.1 cloud APK: 57,848,596 B; V0.2 expected ≈ same order (no new runtime deps) |
| Application startup | first console text visible after launch (compose test timing) | sub-second on emulator idle start |
| Onboarding duration | drawer → Studio first save | seconds; dominated by first-input latency |
| Main navigation responsiveness | drawer open + destination switch in UI tests | no dropped frames asserted; no fabricated fps |
| Model-manager list rendering | catalog of 8 entries rendered in E2E-21 | bounded by compose idle |
| Mock request latency | handshake RTT vs 127.0.0.1 mock | loopback; not a Tower estimate |
| Mock streaming start | first DELTA after ACK (mock server) | loopback only |
| Self-Test duration | QUICK/STANDARD/FULL result durations (typed results carry durationMs) | QUICK target < 30 s; FULL ≤ 10 min envelope |
| Download test-artifact time | ST-32/33 result durations (4 KB / 256 KB chunk-paced) | deterministic pacing, not network truth |
| Configuration read-back latency | ST-27/ST-44 durations | file-backed, milliseconds on emulator |

## Rules of interpretation

- CPU, RAM, thermal and battery figures from the emulator are never quoted as device
  capacity; the device health monitor shows platform facts only, and the emulator
  exposes none of the Xiaomi's power/thermal profile.
- Latency classes in the catalog (NEGLIGIBLE/REALTIME/SLOW) are product metadata, not
  measurements.
- Xiaomi-specific numbers enter the record only via
  `docs/device/XIAOMI_GRAPH_2_PHYSICAL_TEST.md` after physical execution.
