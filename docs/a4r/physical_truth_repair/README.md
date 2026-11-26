# ANDROID REPAIR — FULL PHYSICAL-TRUTH REPAIR (mission artifacts)

Mission: HERMES VOICE CONSOLE ANDROID — Full physical-truth repair, Tower pairing
recovery, local Qwen runtime qualification, primary user-path redesign and Tower
visual convergence. Owner: Paolo Lombisani. Device: Xiaomi 11 Lite 5G NE (arm64-v8a).

## Status after this segment

| Checkpoint | State |
|---|---|
| CP-001 input identity + failed baseline | COMPLETE |
| CP-002 architecture + interaction inventory | COMPLETE |
| CP-003 pairing root cause | COMPLETE — endpoint class CLOSED (https://tower.local) |
| CP-006 Qwen runtime root cause | ANALYSIS COMPLETE (proven, re-confirmed 2026-11-26) |
| CP-004/005/007..012 | PENDING (implementation) |

## Key truths established

1. **Source identity PROVEN** — the failed APK (release asset `26dbad0c…` and the
   Owner-uploaded CI variant `1744e173…`, reconciled as same-source byte variants)
   builds from `graph-a4r/merge-with-b-base` @ `a58c630` (app content identical
   through tip `8863b61`): `app/` (A lineage) + `candidate-b-base/` (B pairing
   lane) compiled together.
2. **APP-P0-01 root cause PROVEN** — the Tower QR carries `https://tower.local`
   (LAN mDNS): unresolvable on the phone's 4G context, TLS-mismatched even if
   resolved, no client endpoint candidates; two contract divergences (AMB-8 ack
   route absent, AMB-9 in-band envelope unread) sit behind the transport fix.
3. **APP-P0-02 root cause PROVEN** — promotion dead-end: a real 522 MB download
   completes to tmp and is never verified/installed; re-click deletes it
   (Range-416) and restarts; the real llama.cpp runtime is wired but unreachable.
4. **Structural finding** — the console conversation runs a MOCK local executor
   with a null Tower executor: Tower-first routing must be built for real
   (ANDROID_DEAD_OR_UNBOUND_CONTROL_REGISTER.json).
5. **Executor** — GLM 5.4 unavailable in session; the charter's authorized
   fallback GLM 5.3 self-qualified 7/7 gates and proceeds.

## Gate discipline

No capability is claimed PASS without physical execution. NOT claimed:
OWNER_ACCEPTED, CANONICAL_APK, PAIRING_PHYSICALLY_VERIFIED,
QWEN_PHYSICALLY_VERIFIED, GRAPH_A4R_PASS. The frozen APK is evidence only.

Ledger: `download/A4R/phys_repair/A4R_OWNER_PHYSICAL_LEDGER.json` (schema 1.3).
Repair branch: `graph-a4r/physical-truth-repair` (from `8863b61`).
