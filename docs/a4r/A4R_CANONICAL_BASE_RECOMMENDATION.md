# A4R Canonical Base Recommendation (NODE A5)

Owner: Paolo Lombisani · Executor: GLM 5.3 Web (second Android account) · 2026-11-20
Status: **RECOMMENDATION — OWNER GATE REQUIRED** (no physical verdict is self-granted)

## 1. The decision problem

Two candidates exist; exactly one maintained app is the end state. The evidence
collected this session (NODE A0–A4) settles the *shape* of the decision:

- **A** (`com.hermestower.voiceconsole`, code 12) is the product-mature app:
  501-test CI line, 5 languages, themes, onboarding, model manager, honest typed
  states — but its Tower lanes are a *candidate contract* over `/tower/api` +
  `/tower/ws` (`hermes.console.v0.1`) that the live Tower does **not** serve, its
  pairing/archive executors are deterministic mocks, and its QR parser accepts the
  candidate `hermes-pair://v1` scheme, not the Tower's QR v2. **A cannot collect a
  single row of live-Tower physical evidence today.**
- **B** (`com.hermestower.voiceconsole.debug`, code 1) is the only candidate that
  speaks the real authority (18 live `/api/tower/voice-console/v2/*` endpoints, QR
  v2 JSON, EC-P256 device identity, real Archive, revocation, replay rejection) —
  but it **physically crashes on the primary QR-pairing path** (A4R-P1-001,
  Owner's Xiaomi, bound to build FP-B-G4 byte-exactly), and its persistence layer
  lacked typed failure handling (A4R-P1-002; repair package A2 delivered).

The mission's hard rule decides the as-is question: *do not select an app that
crashes on the primary path* — and *do not overweight declared feature count*.
Neither candidate is selectable AS-IS: A fails the authority gate, B fails the
stability gate (until the A2 repair lands and is physically retested).

## 2. Recommendation

**MERGE_WITH_B_AS_BASE** — B's Tower authority core becomes the base of the
ONE canonical app, absorbed **into the mission's canonical repository
`Paolo83Ita/hermes-voice-console-android`**, with A's product maturity as the
feature source. This is the only option that simultaneously:

1. keeps the cryptographic device identity and the live contract (the §2
   authority boundary stays natively satisfied);
2. preserves A's 501-test discipline, i18n, themes, offline lanes as the
   feature source (nothing of value is discarded);
3. produces one repository, one app, one package strategy, one signing
   strategy, one release line (the mission's merge definition);
4. matches, independently, the Tower-side Graph 4R long-term direction
   (P-C1..P-C5) — two campaigns, same conclusion, now on my own evidence.

## 3. Conditions (all typed, none waivable by the executor)

| # | Condition | Gate owner |
|---|---|---|
| C1 | A2 keystore repair integrated in the Tower repo, rebuilt APK, **Owner QR-pairing retest PASS on the Xiaomi** (ANDROID_KEYSTORE_PASS) | Tower account + Owner |
| C2 | QR v2 adapter beside the v1 parser in A (RD-04/A6) | executor |
| C3 | Comparable physical tests on both repaired builds (mission §11 sequence) recorded in `A4R_OWNER_PHYSICAL_LEDGER.json` | Owner |
| C4 | Owner approval of THIS merge direction | Owner |
| C5 | Package-ID unification, release signing authority, any supersede of installed apps: separate explicit Owner approvals | Owner |

## 4. Bridge (does not wait for the merge)

The **qualification vehicle for the Graph 4 physical gate is the REPAIRED B
build** — the capability set the gate tests (pairing, phrase, streaming,
transcript, response+TTS, Archive, hash verify, revocation, replay, re-pair,
restart) exists only there. The Tower-side G4R selection of B as the vehicle is
therefore *kept as a bridge*, superseded only as a FINAL-STATE decision.

## 5. What would change this recommendation

- If C1 fails physically after repair (keystore path still broken on the
  Xiaomi), B loses its base candidacy and the recommendation re-opens with A
  as base plus a full port of B's authority lane (higher risk, longer path) —
  decided on the ledger evidence, not on opinion.
- If the Owner prefers a different package/signing unification, the merge plan
  phases M2+ re-order accordingly; the authority-core-from-B principle stands
  unless the Owner directs otherwise.

## 6. Scorecard and plans

Machine truth: `A4R_CANONICAL_BASE_SCORECARD.json` (17 weighted criteria +
3 hard gates), `A4R_MERGE_PLAN.json` (phases M0–M5), `A4R_ROLLBACK_PLAN.json`.
