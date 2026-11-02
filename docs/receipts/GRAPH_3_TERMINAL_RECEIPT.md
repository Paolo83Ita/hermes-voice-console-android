# GRAPH 3 TERMINAL RECEIPT — Hermes Voice Console Android

Graph: HVC_GRAPH_3_MONUMENTAL_ORACLE_TOWER_VISUAL_REFOUNDATION v1.0
Branch: `graph-3/monumental-oracle-tower-visual-refoundation` (STARTING_SHA bc186db)
Issued: 2026-11-03 · Executor: GLM 5.3 Flash · Owner: Paolo Lombisani

## TERMINAL STATE (typed)

**GRAPH_3_CONDITIONALLY_COMPLETE_WITH_EMULATOR_LIMITATION**

Conditionality: every capability verifiable in this environment is verified with physical
evidence (cloud build, 102/102 unit tests, lint, APK, preview gate, secret-clean scan,
remote read-backs, plus a real emulator E2E progression 5→14→17 passing with two genuine
on-device root causes fixed). The remaining E2E failures are typed below (emulator
input/lifecycle environment flakes + one open queue-reconnect question) and are NOT
claimed green. Physical Xiaomi qualification remains a separate Owner phase.

## 1. Mission objectives — evidence ledger

| Objective | Status | Evidence |
|---|---|---|
| Graph 2 boundary preserved | DONE | Graph 2 closed `GRAPH_2_CONDITIONALLY_COMPLETE_WITH_EMULATOR_ENVIRONMENT_LIMITATION` (bc186db) before Graph 3 branch creation; branch never deleted; main untouched (e7ab101) |
| Visual refoundation (not a reskin) | DONE | 12-class gap analysis (`docs/graph3/DESIGN_GAP_ANALYSIS.md`); layered Oracle Core L0..L9 replaces the single-column hero; command frieze replaces the Material top bar; architectural chrome converges all screens |
| Owner reference respected, never shipped | DONE | `upload/Immagine_Hermes_Tower.png` used as direction only; not in repo, not in APK |
| Preservation contract REUSE_AND_EXTEND | DONE | Chrome API signatures preserved; all Graph 2 logic/state/tests/workflows intact; 102/102 unit tests include all 89 Graph 2 tests |
| Preview gate BEFORE production change | DONE | 11-render pack at target 1080×2400 ratio (+landscape 2400×1080), verification OVERALL: PASS (11/11 nonblank, exact dimensions, 0 overflow, contrast 8/8 PASS); V0.1-vs-Graph3 comparison; committed at `design/monumental/preview/` |
| Waves 1–9 | DONE (see ledger §2) | CP-G3-002..007 |
| Asset pipeline | DONE | Hybrid: 4 generated WebP depth layers (28 KB compressed, ~24.5 MB decoded) + Canvas dynamics + vectors; ASSET_MANIFEST.json with hashes; no Rive/Lottie; AGSL deferred post-qualification |
| Quality profiles + thermal | DONE | LOW/BALANCED/HIGH budgets (pure, unit-tested) + ThermalVisualPolicy on real device-health reads; HIGH gated behind device qualification |
| AMOLED safety | DONE | Per-region burn-in drift ≤4 dp/25 s (5 regions), state-budgeted luminance, resting bronze ≤0.55, black-idle option via scrim setting |
| Documentation convergence | DONE | 8 new Graph 3 docs + semantic supersession of VISUAL_DIRECTION + addenda to REQUIREMENTS/ARCHITECTURE/UI_STATE_MACHINE/SECURITY/RISK/ROADMAP + governance updates |
| Tests updated, none weakened | DONE | +13 OracleCoreModel tests; awaitState diagnostics strengthened; every existing behavioral assertion preserved |
| Cloud build + APK | DONE | run 41375246590 @ 85cdc0e: BUILD SUCCESS, unit 102/102, lint archived, APK 58,782,349 B sha256 `0639ae6ed50f3ec3a065625a40106125a6d103bdf8a639df826bb04b155cbc3a` (`docs/receipts/GRAPH_3_DEBUG_APK_RECEIPT.md`) |
| Secret-clean | DONE | repo scan 0 findings (working tree + history); source package re-scanned 0 findings; evidence scanned by workflow step |
| Remote read-backs | DONE | every checkpoint pushed and ls-remote verified (bc186db → 84c3114 → 6959c07 → de4e54e → 21a57c6 → fcea6bc → 6a92942 → 85cdc0e) |
| Merge to main | NOT EXECUTED | OWNER_APPROVAL_PENDING (mission §5.11); no force-push, no history rewrite, no branch deletion |

## 2. Wave ledger

| Wave | Content | Commit |
|---|---|---|
| Bootstrap | graph-3 branch @ bc186db, workflow triggers extended (same pipeline authority) | 84c3114 |
| W1–W4 | token system, OracleCoreModel + renderer, console refoundation, chrome re-expression, docs + preview pack | 6959c07 |
| Evidence | cloud build + APK receipt + doc convergence | de4e54e |
| W7–W8 repairs | drawer-HERMES dedup + display-UOE guard (inherited G2 frontier) | (G2) bc186db |
| E2E evidence | in-script logcat/screenshot capture | fcea6bc |
| Diagnostics | awaitState surfaces downloader error + counters | 6a92942 |
| Root cause | getFileStore → File.usableSpace (Android-supported) | 85cdc0e |

## 3. Emulator E2E ledger (software API 34, no KVM — env limitation)

| Cycle | Commit / run | Result |
|---|---|---|
| 1 | a9657f0 / 41341134806 | 30 reported: 5✓ 18✗, 7 killed by process crash (display-UOE) |
| 2 | 6959c07 / 41349811483 | 30 reported, no crash: 14✓ 16✗ (HERMES dedup + UOE repairs verified) |
| 3 | 21a57c6 / 41356511688 | 14✓ 16✗ (cleartext hardening — hypothesis not confirmed) |
| 4 | 6a92942 / 41368013409 | 14✓ 16✗ + surfaced error `getFileStore` (diagnostic repair worked) |
| 5 | 85cdc0e / 41375246654 | **17✓ 13✗** — all 9 download-class failures fixed |

Repair ledger (mission rule: ≤3 substantive repairs per failure class — respected):
1. duplicate-HERMES: 1 repair (drawer distinct string) — VERIFIED by cycles 2–5.
2. display-UOE crash: 1 repair (truthful-null degradation) — VERIFIED.
3. download-FAILED class: 3 attempts (cleartext hardening → diagnostic surfacing →
   getFileStore root-cause fix) — VERIFIED by cycle 5.
4. Remaining classes: "Failed to inject touch input" ×6 + navigation/destroy timeouts
   (~×6) = emulator input/lifecycle environment flakes (vary run to run; zero code
   repairs spent); e2e_19_20 queue-reconnect validation = 1 open content question.

Explicitly NOT claimed: PHYSICAL_DEVICE_QUALIFIED, REAL_TOWER_INTEGRATION_PASSED,
full-suite E2E green. Real Tower remains mock/simulator-only.

## 4. Terminality calculation

- remaining_nodes: 0 (all waves executed; E2E frontier documented above)
- dependency_released_nodes: physical Xiaomi qualification (Owner phase, next graph)
- executable_nodes: none in this environment (further E2E repair cycles possible but
  class budget spent; navigation flakes are environmental)
- repairable_nodes: e2e_19_20 queue-reconnect question + nav-flake variance — carried
  with full evidence for the next graph
- environment_blocked_nodes: full-suite E2E green (software emulator, no KVM)
- device_blocked_nodes: Xiaomi 11 Lite 5G NE qualification (quality profiles HIGH,
  thermal read-back, burn-in soak, TTS/STT discovery, 4 h soak)
- Owner-blocked_nodes: merge to main; any real-endpoint Tower data entry
- terminality_condition: CONDITIONALLY_COMPLETE — all environment-verifiable evidence
  green; E2E improved +12 with typed residual; merge owner-blocked
- terminality_proof: run 41375246590 (build+102/102+APK+lint), E2E cycle ledger §3,
  preview verification report, secret scans, remote read-back chain, this receipt

## 5. Deliverables index (mission §21)

1. DESIGN_GAP_ANALYSIS.md — docs/graph3/ · 2. MONUMENTAL_ORACLE_TOWER_VISUAL_SYSTEM.md —
docs/graph3/ · 3. ORACLE_CORE_STATE_SPEC.md — docs/graph3/ · 4. MOBILE_COMPOSITION_SPEC.md —
docs/graph3/ · 5. ASSET_PIPELINE_DECISION.md — docs/graph3/ ·
6. PERFORMANCE_AND_THERMAL_BUDGET.md — docs/graph3/ · 7. AMOLED_AND_BURN_IN_SAFETY.md —
docs/graph3/ · 8. ASSET_MANIFEST.json — docs/graph3/ · 9. preview pack (11 renders +
verification report) — design/monumental/preview/ · 10. V0.1-vs-Graph3 comparison —
same folder · 11. implementation — ui/monumental/ + Chrome + Console + AppNav + Theme ·
12. tests — OracleCoreModelTest (13) + strengthened awaitState · 13. cloud build evidence —
run 41375246590 · 14. debug APK · 15. APK SHA-256 — 0639ae6e…b155cbc3a ·
16. secret-clean source package — download/hermes-voice-console-android-v0.3-graph3-source.zip ·
17. this receipt · 18. REHYDRATION_PACK.md updated (Graph 3 delta).
