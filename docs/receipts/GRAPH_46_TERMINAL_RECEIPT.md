# GRAPH 4.6 TERMINAL RECEIPT

Directive: GRAPH 4.6 §10 TERMINALITY — required final state
**`GRAPH_4_IMPLEMENTATION_COMPLETE_XIAOMI_QUALIFICATION_PENDING`**.

This receipt is the closing record of the Graph 4.6 mission on branch
`graph-4/real-voice-pairing-archive-e2e` (never `main`; no force-push; no checkpoint
deleted). Every claim below is bound to a run, an artifact, or a committed file of
this repository.

## 1. Condition checklist (directive §10)

| # | Condition | State | Evidence |
| --- | --- | --- | --- |
| 1 | Italian UI complete | VERDICT BELOW (§2) | ITALIAN_UI_REPORT.md §6 — cloud gate at bba1ebc |
| 2 | Tower finalization complete | **COMPLETE** (implementation + repairs + measured previews; physical profile arbitration = Xiaomi) | MONUMENTAL_TOWER_FINAL_REPORT.md; GAP doc §6 (D1/D2/D4/D6 CLOSED) |
| 3 | STT path implemented | **YES — LOCAL_STT_READY_FOR_XIAOMI_QUALIFICATION** | STT_RUNTIME_RECEIPT.md (vosk-android 0.3.75 AAR sha256 ab2f8b91… verified LIVE; model URL 49,665,141 B verified LIVE; frame engine + delivery chain + honest active-engine read-back) |
| 4 | TTS path implemented | **YES — LOCAL_TTS_ACTIVE (system) + natural-voice candidates ready** | TTS_RUNTIME_RECEIPT.md (real playback/interruption/engine identity; Piper candidates hash-verified; runtime row honestly pending) |
| 5 | LLM path implemented | **YES — LOCAL_LLM_IMPLEMENTED (DEFAULT DISABLED)** | LocalLlmEngine + NoVerifiedLlmRuntime (typed LLM_RUNTIME_PENDING_VERIFIED_SOURCE) + 10 unit tests; LOCAL_LLM_QUALIFIED deliberately NOT claimed (Xiaomi tests first) |
| 6 | Source links implemented | **YES** | ModelLinks validator + physically clickable actions (CP-G4-019/020) + CatalogLinkIntegrityTest; reject rules (example.invalid / unknown origin / mutable latest) enforced |
| 7 | Archive complete | **YES (deterministic mock, every record SIMULATED)** | search/pagination/filters/open/preview/cache/offline-pin/provenance/hash/re-download/source-conversation/source-mission + MockTowerArchiveTest; UI pin + provenance + SHA-256 rows (CP-G4-022) |
| 8 | Unit tests green | **260/260** | artifact 13350398668 @ bba1ebc (run 41890040725) |
| 9 | Lint zero errors | **0 errors / 60 warnings** (classified) | artifact 13351056892 @ bba1ebc |
| 10 | APKs generated | **universal + arm64-v8a + x86_64** | APK_SIZE_AND_ABI_REPORT.md (measured from artifact bytes) |
| 11 | Hashes generated | **all three SHA-256** | arm64 `f2f38b61…` · universal `97998108…` · x86_64 `7c4de490…` @ c97a02f artifact 13351081998 |
| 12 | Governance updated | **YES** | GRAPH46_READBACK, ledger R-8..R-11, WORK_LOG G4-N24, CHECKPOINT_INDEX CP-G4-021..024, ACTIVE_RUNTIME_READBACK, this receipt |
| 13 | No dependency-released Android node left open | **YES** | every shipped node closed or typed: the LLM runtime gap and the Piper runtime are TYPED, test-enforced, honest states — not silent openings |

## 2. Condition 1 — Italian UI (the gate decides)

<!-- TERMINAL_VERDICT_PLACEHOLDER -->

## 3. Deliberately NOT claimed (directive §10 prohibitions)

- **REAL_PAIRING_PASSED** — Tower Graph 1.7 does not exist; pairing runs through the
  deterministic mock and is labeled SIMULATED everywhere.
- **REAL_TOWER_ARCHIVE** — the archive is the deterministic mock with SIMULATED
  badges; the real Tower stays the authority.
- **LOCAL_LLM_QUALIFIED** — no Xiaomi measurement yet (RAM/thermal/latency/Italian
  quality); the engine is implemented and typed-blocked.
- **Any transcript/voice latency/RAM figure** — produced only during the Xiaomi
  qualification session (XIAOMI_GRAPH4_BETA_PACKAGE).

## 4. The Xiaomi arbitration session

Package: docs/XIAOMI_GRAPH4_BETA_PACKAGE.md (§2 install/upgrade, §3 the 13-item test
matrix, §4 evidence to return). The lifecycle residual (R-11 lineage) is dispositioned
PHYSICAL_XIAOMI_RECOVERY_REQUIRED — the physical Xiaomi arbitrates it; the per-class
containment keeps every other class's evidence independent of it.

## 5. Commit lineage of this terminality (forward-only, no rewrites)

b3b78ed (verified tip at read-back) → 7d1f8cf (CP-G4-021) → d37d922 (CP-G4-022) →
6545969 + c97a02f + bba1ebc (CP-G4-023 + fixes) → (this commit) (CP-G4-024 terminality).
