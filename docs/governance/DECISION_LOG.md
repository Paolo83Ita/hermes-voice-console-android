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

---

# Graph 2 decisions (G2-xxx)

### G2-001 — main established from verified Graph 1 authority (2026-11-01)
DECISION: per GRAPH 2 brief §21 (latest Owner decision, supersedes G-006), remote `main`
was created at `e7ab101ecde0dd093edd27b94bbb969822063d26` via the GitHub refs API
(new ref, no history rewrite, no force-push), local `main` fast-forwarded ffe6749→e7ab101,
fresh remote read-back performed, and `graph-2/model-manager-tower-connection-e2e`
created from that verified main.
EVIDENCE: API branch list shows main e7ab101; local merge was --ff-only.

### G2-002 — default-branch switch blocked by token scope (2026-11-01)
DECISION: attempt to set repo default_branch to main returned 403
"Resource not accessible by personal access token" (missing Administration scope).
NOT an auth expiry → execution continues with default branch unchanged
(`foundation/v0.1-cloud-build`); `main` is still the PR base and merge target.
If still blocked after merge, the default-branch switch is an Owner manual step
(recorded in Terminal Receipt). Typed as a scope constraint, not a blocker.

### G2-003 — zero new runtime dependencies for Graph 2 (2026-11-01)
DECISION: RealTowerBridge uses `HttpURLConnection`; the deterministic mock Tower server is a
hand-rolled `ServerSocket` fixture; secure storage is a direct AndroidKeyStore AES-256-GCM
vault. Only androidx.test / espresso / compose-ui-test are added, test-scope only.
RATIONALE: minimal supply-chain surface, deterministic tests, JVM-executable connection tests.

### G2-004 — model artifacts: generated test fixture only (2026-11-01)
DECISION: the download path is qualified with a small deterministic in-repo-generated test
artifact served by the in-app localhost mock server; the catalog lists real-world candidates
(whisper-tiny, piper, gemma-class) in non-downloadable states. No real large model is
downloaded in Graph 2; no model file is committed; real-model qualification is
QUALIFICATION_PENDING_DEVICE.

## Graph 3 decisions (2026-11-02)

### D-G3-001 · MONUMENTAL_ORACLE_TOWER_VISUAL_REFOUNDATION
- STATUS: OWNER_AUTHORIZED_FOR_IMPLEMENTATION
- SUPERSEDES: V0_1_MINIMAL_TECHNO_GREEK_VISUAL_DIRECTION (evidence preserved; superseded baseline)
- PRESERVES: all valid functional, security, state, test and build foundations (REUSE_AND_EXTEND)
- BASIS: Owner verdict in the Graph 3 mission; attached reference is direction-only and never shipped.
- CONSEQUENCES: every screen converges on the Monumental material system; Material 3 remains functional-only;
  the pinned E2E invariant "exactly one exact-text HERMES node" is preserved by design.

### D-G3-002 · HYBRID_ASSET_PIPELINE
- Layered WebP static depth (L0/L1/L2/L7) + Compose Canvas dynamics (L3..L9) + vector sigils.
- Rejected: Compose-only Canvas, Rive/Lottie (no license-compatible need), AGSL (post physical qualification),
  single screenshot (forbidden). Emitted layers total 28 KB compressed, ~24.5 MB decoded (within budget).

### D-G3-003 · PER_REGION_BURN_IN_SAFEGUARD
- Five region drivers (frieze/core/seals/transcript/medallions), ≤4 dp, 25 s cadence,
  disabled by reduced motion or Owner switch. Replaces the whole-console V0.1 drift.

## G4.11-D1 (2026-11-07) — Graph 4.11 runs ON TOP of the 4.10 authorities
The Owner directive ("Do not expand architecture / no new abstractions / no Graph 5") is implemented as: the 4.10 authorities (ResponseLocaleResolver, VoiceSelectionAuthority, VoiceRuntimeFactory, ChatSpeechOutputPolicy, ListeningModePolicy, ResponsiveActionGroup, layered Tower) are PRESERVED; Graph 4.11 adds ONLY qualification evidence (ST-54, 10-turn pins, anti-English pin), measured Tower repairs, and the package bump. No abstraction was introduced; ST-54 follows the ST-51/52/53 probe pattern exactly.

## G4.11-D2 (2026-11-07) — static Tower bands become stage-sized canvases
The 4.10 tight-band canvases (sign 629×203, stairs 484×154, gate 291×342, foreground 629×32) rendered at the WRONG size/position under ReforgedTowerVisual's TopCenter/fillMaxWidth/Fit (all bands stacked at the stage top). Decision: re-stage every static band on the full 720×1653 canvas at its measured position (uniform scale S=1.61426 anchored on the manifest core) instead of adding Compose offsets — the assets become the placement truth, the code stays unchanged, and the core lands exactly on the code's (0.500, 0.403). portal/energy untouched (byte-identical).

## G4.11-D3 (2026-11-07) — layer naming map (Node 5 list vs disk)
The Node-5 layer list names `tower_gate`; the shipped asset is `tower_lower_gate` (4.10 name). Decision: keep the disk name (no churn, no rebuild of working assets) and record the mapping tower_lower_gate == tower_gate in the manifest + report. tower_energy remains the Canvas-driven static energy path (the 4.11 list omits it because motion is code-owned).

## G4.14-D5 (2026-11-09) — calibrated magenta gate (not the generation gate)
The §38 generation gate ("no_magenta_remnant") had missed 110 opaque + feather-spread
chroma-key signature pixels. Decision: the SHIPPED-pixel gate is the CALIBRATED
signature (hue 295–345°, sat≥0.62, val≥0.55; Owner reference cross-check = 0 such
pixels); dark-wine artwork tones (val≤0.60) are documented-accepted. Repair =
in-slice median inpaint (deterministic, no re-extraction — honoring the charter's
"no new extraction without a material defect gate"; the residue WAS material at the
calibrated definition, and the repair avoided a rebuild).

## G4.14-D6 (2026-11-09) — DISCONNECTED/ERROR carry no dedicated tower overlay
The tower animates the console's OWN states; link truth (gray/red indicator) stays
with BridgeModeAuthority — the tower never fakes states the authority does not
declare (§40 coherence; typed in the animation audit).

## G4.14-D7 (2026-11-09) — self-test registration is structural truth
ST-55..57 (CP-G4.12) were appended after the returned list's closing paren — their
def() values were discarded, silently shrinking the battery to 54. Decision: the
battery = the RETURNED list (61); the test pins 61; a latent-count drift is a
BLOCKER-class honesty defect if ever reintroduced (INV-22).
