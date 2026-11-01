# DECISION LOG — Hermes Voice Console Android (graph era)

> Graph-era log. Foundation-era decisions (D-001…) are preserved verbatim in
> `docs/DECISION_LOG.md` and are NOT duplicated here.

## Graph-era decisions (G-xxx)

### G-001 — Local de-risk build before first remote push (2026-11-01)
DECISION: install a local Gradle 8.9 + Android SDK 35 + Temurin JDK 17 toolchain in the
sandbox and bring the build/tests green BEFORE creating the cloud workflow.
RATIONALE: the cloud-build repair budget is 3 materially different iterations; a 50-file
never-compiled Kotlin codebase would likely burn all three. Local iteration is cheap and
unlimited by the graph (bounded by honest attempt discipline).
EVIDENCE: 44 main-source compile errors + 9 test-source errors + 7 test failures found and
repaired locally; final local run BUILD SUCCESSFUL, 33/33 tests passed.

### G-002 — JDK 17 (Temurin) for local and CI parity (2026-11-01)
DECISION: standardize on Temurin JDK 17 (local standalone install + `actions/setup-java@v4`
with temurin 17 in CI). RATIONALE: AGP 8.7.3 + Gradle 8.9 sweet spot; sandbox system Java 21
is a JRE without `jlink` and cannot run AGP's `compileDebugJavaWithJavac`.

### G-003 — Minimal-diff repair discipline (2026-11-01)
DECISION: compile/test failures are repaired with the smallest correct change (imports,
signatures, missing platform calls); no restructuring, no dependency changes.
The state-machine doc (`docs/UI_STATE_MACHINE.md` §3) and the implementation were updated
TOGETHER when tests proved a documented edge set was incomplete (universal rule 4,
IDLE → CONTACTING_TOWER). RATIONALE: documentation must mirror implementation.

### G-004 — Redactor classification: hex runs are case-uniform (2026-11-01)
DECISION: `LogRedactor` classifies 24+ char case-uniform `[0-9a-f]` / `[0-9A-F]` runs as hex;
mixed-case alphanumerics are classified as base64. Assignment redaction preserves separator
whitespace and consumes an optional `Bearer` scheme token. Pinned by unit tests.

### G-005 — MockTextToSpeechEngine gains an injectable dispatcher (2026-11-01)
DECISION: constructor parameter `dispatcher: CoroutineDispatcher = Dispatchers.Default` with
`flowOn(dispatcher)`, so virtual-time tests drive SPEAKING deterministically. RATIONALE:
hard-coded real dispatcher made SPEAKING→IDLE undeterministic under `runTest`; mirrors the
existing MockTowerBridge dispatcher seam. Production default unchanged.

### G-006 — Push strategy: working branch only (2026-11-01)
DECISION: push `foundation/v0.1-cloud-build` only. `main` is NOT created/updated by this
graph; merge to main is an Owner decision after device qualification (INV-07).

### G-007 — Cloud build stack (2026-11-01)
DECISION: GitHub Actions `ubuntu-latest`, Temurin JDK 17, android-actions/setup-android@v3
+ pinned `platforms;android-35` / `build-tools;35.0.0`, committed Gradle 8.9 wrapper,
`assembleDebug` + `testDebugUnitTest` as gates, `lintDebug` executed non-blocking with
report artifact, APK + test results + lint report uploaded as artifacts named with the
commit SHA. RATIONALE: reproducible, hash-bound, no secrets needed inside the workflow.
