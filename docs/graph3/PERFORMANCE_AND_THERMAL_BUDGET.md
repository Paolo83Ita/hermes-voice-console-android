# PERFORMANCE AND THERMAL BUDGET — Graph 3

Target: Xiaomi 11 Lite 5G NE (2109119DG), Snapdragon 778G-class, 8 GB RAM, Android 14.
All measured values are **TARGET_PENDING_DEVICE_QUALIFICATION** until real ADB read-back
exists. No invented metrics are recorded anywhere in this document.

## 1. Quality profiles

| Profile | Core behavior | Effects | Cadence | Intent |
|---------|--------------|---------|---------|--------|
| LOW | static/nearly static core, fewer ring layers | no glow, particles 0 | 5–15 fps effect redraw | thermal pressure, reduced motion, battery saver |
| BALANCED (default) | full layered core | limited glow, particles ≤6 | 30 fps complex animation target, 60 fps direct control response where sustainable | daily driving |
| HIGH | richer particles, more ring layers | optional advanced effects | 30–60 fps | ONLY after physical qualification; auto-falls back under pressure |

Profile selection: Appearance setting (persisted, migration-compatible) + automatic
downgrade triggers (below). Factory default = BALANCED; LOW is never force-enabled without
a real trigger, and HIGH is gated behind device qualification.

## 2. Budgets (targets, not measurements)

| Budget | Target (BALANCED) | Enforcement point |
|--------|-------------------|-------------------|
| total visual asset package | ≤ 6 MB (all layers WebP) | build-time size check, asset manifest |
| decoded bitmap memory (all layers, 1080×2400) | ≤ 48 MB peak; target ≤ 32 MB | manifest estimate + runtime check (Debug.nativeHeapDelta in diagnostics, read-back only) |
| per-frame allocation | zero steady-state allocations in the core clock loop | code review + profiling later |
| average CPU (core visible, idle) | LOW single-digit % | DEVICE_QUALIFICATION_PENDING |
| peak CPU (TOWER_PROCESSING) | bounded by 30 fps cap | same |
| animation frame time | ≤ 16.6 ms budget @60 / ≤ 33.3 @30 | Choreographer stats pending device |
| battery temperature | real read via existing DeviceHealthMonitor; visual downgrade at REAL thresholds read from the device | ThermalVisualPolicy |
| sustained temperature | same source; HIGH→BALANCED→LOW ladder | same |
| idle luminance (OLED) | resting states bronze budget ≤0.55, portal ≤0.30 | state table (ORACLE_CORE_STATE_SPEC) |
| active luminance | SPEAKING cap 0.92 portal, no full-screen white | state table |
| memory pressure fallback | onTrimMemory → drop to LOW effects + release glyph caches | runtime hook |

## 3. Thermal-aware ladder

REAL device temperature (already wired via AndroidDeviceHealthMonitor) drives:
NOMINAL → BALANCED; ELEVATED → cap at 30 fps + glow reduced; CRITICAL → LOW profile +
particles 0. The app never fabricates a temperature; if the sensor is unavailable the
policy stays NOMINAL and diagnostics note SENSOR_UNAVAILABLE.

## 4. What is explicitly NOT claimed

- No fps/CPU/thermal numbers from the Xiaomi device exist yet.
- Emulator measurements are labeled EMULATOR_ONLY and are never Xiaomi evidence.
- HIGH profile availability is claimed nowhere until the physical qualification wave.
