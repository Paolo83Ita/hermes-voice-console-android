# OWNER POCO XIAOMI RETEST PACKAGE 6 — GRAPH 4.13 (§42)

**Build label:** OWNER VISUAL ACCESSIBILITY PACKAGE 6
**versionName:** `0.3.6-alpha-graph4.13-visual-accessibility` · **versionCode:** 9
**Branch:** `graph-4/real-voice-pairing-archive-e2e` (forward-only; main untouched)
**Local validation build at:** the Graph 4.13 tip (commit `9d4f3d0`); CI will
rebuild at the converged tip — the CI-uploaded artifacts are the
authoritative installable set for the tip (the local SHA-256s below identify
the locally built validation copies).

## 0. Installable validation APKs (locally built + hashed)

| Variant | Bytes | SHA-256 |
| --- | --- | --- |
| arm64-v8a (POCO / Xiaomi install THIS) | 116,531,319 | `3b79b7c68e3d9f0e4ee127467f258dd01d35fd92f122597f542ee2a9b4abbcaa` |
| universal | 246,178,148 | `da8a0ee16a49d7f309e8715d24d89596d21abefe1ce5eafbb3f36401af151662` |
| x86_64 (emulator test lane) | 121,247,286 | `a2e3cea8ca389c107552cf4136d61dacd86a0cc8441b71fafdeabdabc75bd21c` |

Install: `adb install -r app-arm64-v8a-debug.apk` (both devices; the versionCode
9 upgrade replaces Package 5 in place, data preserved).

## 1. What changed since Package 5 (what to look at)

1. **App-wide readability (§4–§7)** — every text now consumes the semantic
   color authority (warm ivory primary, light taupe secondary, readable
   technical gray on obsidian; measured ≥4.5:1 for text, ≥3:1 icons/focus,
   disabled labels ≥3:1 with color+fill+border cues). Marble Light was
   repaired app-wide (it previously rendered dark prose on the obsidian shell).
2. **Models and Voice rebuilt (§12–§27)** — sectioned information
   architecture (A recommended configuration / Voices / Speech recognition /
   Wake word & noise / Optional local intelligence / Installed / Updates or
   repairs), compact two-column configuration card (device CLASS, not the raw
   identifier), vertical segmented profile cards (no more one-row forcing, no
   vertical letter wrapping; Local experimental shows "Why unavailable" when
   the local runtime is absent), model cards with localized status pills (no
   raw enums), typed plain-language compatibility (never "false"), ONE filled
   primary action per card following the real state.
3. **Central Tower refounded (§29–§41)** — MONUMENTAL ORACLE CITADEL: heavy
   entablature, solid connected pylons, integrated crown sign, large oracle
   mechanism (42% width) with nested rings and cyan sigil portal, arched gate
   with glow, broad stairs, integrated foundation, energy spine.
4. **Everything physically passing before is preserved** (unified Interrompi,
   continuous listening, QR scanner, Italian responses) and regression-gated.

## 2. POCO X6 Pro tests (§42 list)

| # | Test | Expected |
| --- | --- | --- |
| 1 | Open Console | new Tower renders (no blank area, no old scaffold) |
| 2 | Inspect new Tower | monumental mass; sign attached; big oracle core; stairs + foundation visible |
| 3 | Test IDLE | slow controlled ring motion, subtle portal breathing |
| 4 | Test LISTENING | input energy; cyan seal on portal rim |
| 5 | Test SPEAKING | portal responds to voice amplitude |
| 6 | Test CONTINUOUS LISTENING | persistent low-energy state |
| 7 | Confirm Interrompi still works | state-specific label, cancels capture/TTS |
| 8 | Open QR scanner | opens as before (camera permission path intact) |
| 9 | Open Models and Voice | sectioned, readable, no flat list |
| 10 | Check configuration card | two-column rows, device CLASS "POCO X6 Pro", no empty lower region |
| 11 | Check profile selector | vertical cards, selected ring, honest Local-experimental state |
| 12 | Check Paola card | purpose line, size/RAM/language, typed compatibility, one filled action |
| 13 | Check Qwen card | honest optional-local state, no download pressure |
| 14 | Increase font scale (1.3 / 1.5) | no clipping, no letter-column wrap |
| 15 | Confirm no unreadable text | every label ≥4.5:1 (or ≥3:1 disabled, with cues) |

## 3. Xiaomi 11 Lite 5G NE tests (§42 list)

| # | Test | Expected |
| --- | --- | --- |
| 1 | Open Models and Voice | sections render at 360 dp width |
| 2 | Confirm no vertical word wrapping | profile titles ≥80 dp wide (g413_17 gate) |
| 3 | Confirm no clipped buttons | g413_01..09 clipping gates; visually too |
| 4 | Confirm all text readable | semantic tokens in both themes |
| 5 | Confirm primary actions fit | one filled action + More actions; never clipped |
| 6 | Confirm Tower fits width | stage scaling at 360 dp; no horizontal clip |
| 7 | Test LOW profile | dedicated low-res tier; static rings + breathing; no architecture loss |
| 8 | Test BALANCED | full animation |
| 9 | Confirm QR scanner opens | unchanged |
| 10 | Confirm Interrompi remains operational | unchanged |

## 4. Known limitations (honest register)

- The E2E emulator fleet (CI) still runs the Graph 4.12-era typed
  anchor-failure line (R-42..R-49): gate/regression classes remain
  green-or-typed-honest; the crash family stays eliminated (4 consecutive
  samples). This is a HARNESS/emulator line, not an app defect evidenced at
  this tip.
- HIGH profile is optional and never default; its 1000 px tier is not
  shipped in the default APK asset set (LOW/BALANCED are).
- The visual renders in `docs/visual/tower413/renders/` are PREVIEWS; the
  physically authoritative judgment is the Owner's device test.
- Lint carries 82 accepted/classified warnings (0 errors; below the 84
  baseline; 0 newly introduced — see `docs/lint/LINT_WARNING_CLASSIFICATION.md`).

## 5. After the retest

- Grant/decline per finding → `OWNER_VISUAL_ACCESSIBILITY_AND_MODEL_LIBRARY_RETEST_PASSED`
  is OWNER-ONLY. Any FAIL feeds the next typed repair node.
- Report format: device, build label, per-test PASS/FAIL + screenshot.
