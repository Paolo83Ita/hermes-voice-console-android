# GRAPH 4.14 — CONTINUATION READ-BACK (CP-G4.14-C12) — OWNER PHYSICAL RETEST ROUND 2

Mission mode: CONTINUE_THE_EXISTING_GRAPH_FROM_LATEST_VERIFIED_REMOTE_AUTHORITY.
Fresh remote tip prevails. No restart of Graph 4.14; no repeated discovery; no
recreated matrix/authority/theme; main untouched; no force-push.

## NODE
CP-G4.14-C12 — continuation after the Owner's physical retest of
**OWNER RUNTIME THEME TOWER PACKAGE 8** (0.3.8-alpha-graph4.14-owner-retest-repairs,
versionCode 11, run #178) on the **POCO X6 Pro**. The Owner's round-2 verdict
RE-OPENS/keeps open `OWNER_VISUAL_RETEST_PENDING` with 4 findings (G-A..G-D,
below); two confirmed repairs from round 1 are explicitly ACKNOWLEDGED by the
Owner's own wording ("la torre sullo sfondo è ok"; the download button now
EXISTS — the failure moved from "no button" to a transport error). The repairs
below are the response package (Package 9) and the physical re-grant stays
Owner-only.

## READ-BACK (verified this session)

| Item | Verified value |
| --- | --- |
| Active branch | graph-4/real-voice-pairing-archive-e2e |
| Local HEAD at session start | afefd3d (recycled sandbox snapshot — stale) |
| Remote tip (fetch verified) | 912ed91 (CP-G4.14-C11 round-1 repairs, Package 8) |
| Delta | 0 ahead / 1 behind → ff-only afefd3d → 912ed91 (forward-only) |
| Working tree | clean at read-back |
| main | untouched |
| Open gates at read-back | OWNER_VISUAL_RETEST_PENDING (round 2) + LLM_PHYSICAL_QUALIFICATION_PENDING |

## OWNER RETEST VERDICT (Package 8 on POCO X6 Pro) — registered verbatim-class

| ID | Finding (Owner, it) | Repair surface |
| --- | --- | --- |
| G-A | "permangono errori nel menù delle impostazioni per le scritte nere su sfondo scuro poco visibili" | ambient content-color authority (THEME ROOT) |
| G-B | "in modelli qwen 2.5 0.5B non si riesce ad installare perché quando tenta di scaricarlo c'è un errore di connessione" | download transport (ModelDownloadManager) |
| G-C | "la torre sullo sfondo è ok ma il cerchio centrale è sdoppiato e traslato in basso rovinandola completamente" | crop rotation + contacting-arc geometry |
| G-D | "e nessun effetto è stato aggiunto" | §39 idle effects (same root as G-C #1) |

Round-1 repairs CONFIRMED by the Owner's wording: the tower ARCHITECTURE now
renders correctly ("la torre sullo sfondo è ok" — F-A image-rect repair held),
and the model cards now OFFER the download (F-C repair held — the Owner could
attempt it; the failure moved INTO the transport, see G-B).

## ROOT CAUSES (code-proven) + REPAIRS (all in this package)

### G-A — black text in the settings menu: the theme never provided LocalContentColor
The app root (`AppNav`) is a plain `Column` with a manual background — NOT a
Material `Surface`/`Scaffold`. Compose's `LocalContentColor` therefore kept
its framework default **Color.Black** for the whole tree, and EVERY
`Text(...)`/`Icon(...)` without an explicit color rendered black. A full census
(found by a balanced-paren call scanner, not grep): **152 uncolored Text sites
across 18 files** — SettingsHubScreen row label (line 202, the Owner's named
site), Archive (29), TowerStudio (22), VoiceAudio (14), Diagnostics (13),
History (7), AboutSafety (7), Onboarding (10), SelfTest (10), ModelManager
(10), Console (8), dialogs and components. The C11 grep-sweep could not see
this class: the defect is an ABSENT parameter, not a wrong palette.

REPAIR (two layers):
1. **Theme-root (class-level):** `HermesTheme` now provides
   `LocalContentColor = colors.textPrimary` (ivory in obsidian, marble ink in
   light). Material components that set their own contentColor (buttons, chips,
   surfaces) still override deeper in the tree — no existing pairing changes;
   only the UNANCHORED sites flip from invisible black to the theme ink.
2. **The Owner's named site:** the settings-hub row label now carries the
   explicit token (`hermesColors.textPrimary`) — deterministic regardless of
   the provider.

### G-B — Qwen 2.5 0.5B download "errore di connessione": transport-layer defects
The pinned source is **ALIVE** (verified this session:
`https://huggingface.co/Qwen/Qwen2.5-0.5B-Instruct-GGUF/resolve/9217f5db79a29953eb74d5343926648285ec7e67/qwen2.5-0.5b-instruct-q5_k_m.gguf`
→ 302 → `us.aws.cdn.hf.co` → 200, content-length 522186592 = the catalog's
522 MB q5_k_m). The failures were in `ModelDownloadManager`:

1. **NO User-Agent** — some CDNs reject empty/Java-default UAs with 403.
2. **`instanceFollowRedirects=true`** relied on the platform's redirect
   behavior for the HF→CDN cross-host 302 chain; headers (UA) and Range are
   not guaranteed to survive platform-managed hops.
3. **Tight timeouts** (connect 10s / read 20s) — a mobile network + TLS to a
   new CDN host trips both; every trip surfaced to the Owner as a connection
   error.
4. **THE CORRECTNESS BUG:** the bounded retry loop re-downloaded from the
   STALE offset captured before the loop and opened the partial in **APPEND**
   mode — a mid-stream failure at N bytes made the retry PREFIX the stream
   AGAIN from byte 0 onto the N-byte file → corrupt .part → guaranteed
   hash-mismatch quarantine ("Incomplete download" on better networks).

REPAIR (all typed, bounded, honest):
- Explicit redirect walk (301/302/303/307/308, ≤5 hops) with UA + Range
  preserved on every hop; **never downgrades https** (a pinned https source
  cannot be redirected to http).
- `User-Agent: HermesTowerConsole-ModelDownload/1.0 (Android)` on every request.
- connect 20s / read 45s.
- **True-offset resume:** each attempt resumes from the partial's ACTUAL size
  (positioned channel, never blind APPEND); a legacy oversized partial is
  reset; a server that ignores Range (200 with offset>0) restarts from zero;
  416 resets once.
- **Mirror fallback:** one bounded attempt via `hf-mirror.com` (same immutable
  path) for pinned huggingface.co sources, consuming one attempt of the SAME
  budget; the fallback is recorded in the progress error string.
- SHA-256 verification unchanged (every artifact still hash-gated before
  install).

PINS (ModelDownloadLifecycleTest, 3 new): redirect chain followed with the
pinned UA; stale-partial resume completes and verifies (the old append
behavior fails this hash assertion); a redirect loop ends FAILED honestly
within the bounded budget.

### G-C — the central circle "sdoppiato e traslato in basso": two geometry bugs
Round-1 NCC verification (C12 measurement, `docs/visual/tower414/
C12_CROP_ALIGNMENT_MEASUREMENT.json`) proves the SHIPPED crop placement is
pixel-true against the SHIPPED architecture (static drift ≤1.2dp on both
tiers) — the static composition is NOT the residual defect. Two code-proven
geometry bugs remained:

1. **The idle "ring shimmer" rotated the RASTER CROPS** (±8°/−14°/22° about
   the crop box). The crops carry the FULL mechanism artwork (gears, sigil
   triangle, figures — NOT rotationally symmetric): the rotating copy visibly
   separated from its baked original — masked-NCC similarity drops
   0.9955 → 0.878 at the 8° extreme; a rigid bitmap rotation displaces ring
   content by up to ~18dp at the crop's extremes. This is the Owner's
   doubled/shifted central circle, animating slowly enough to look "broken"
   rather than "animated". REPAIR: crops render at rotation 0 ALWAYS; the
   shimmer became a Canvas **ring light sweep** (a rotating highlight arc ON
   the ring band) which cannot move geometry.
2. **The CONTACTING arc mixed h- and w-fractions** (`topLeft = 0.30·h` with
   `size = 0.60·w`): its circle centered at 0.30h+0.30w = **+33.2dp BELOW the
   baked ring center** on the POCO stage (412×583.5dp image rect), with an
   8dp-smaller radius — a literal second cyan circle outline translated
   downward during every real contacting/queued state. REPAIR: the arc (and
   the transcribing ticks, previously 0.30w vs the ring's 0.32w) are
   CONCENTRIC with the measured ring band (center 0.5w/0.455h, radius
   0.32w = the ring_outer crop's outer edge).

### G-D — "nessun effetto è stato aggiunto" (same root as G-C #1)
The ONLY idle motion was the 90-second crop rotation — imperceptible as an
effect while harmful to geometry, and the §39 runbook expectations (idle
"visible but subtle", listening "clearly stronger", speaking amplitude
response) were not met. REPAIR (all Canvas-only, §40 truth preserved — mock
stays amber, never cyan):
- Portal breathing strengthened (glow base 0.55..1.0 × 0.45 vs the old
  0.72..1.0 × 0.4).
- Ring light sweep (rotating highlight arc on the band, alpha 0.22 idle /
  0.34 listening), 42s period BALANCED (was 90s), still slow/cool per the
  monumental identity.
- Listening lines, transcribing ticks (now ON the ring band), speaking
  amplitude response, privacy seal, energy tint — unchanged mechanisms,
  now geometrically correct.

## TEST

- Unit suite (local, post-repair): **501/501 GREEN** (68 suites; 0 f/e/s;
  498 C11 baseline + 3 new G-B pins).
- Lint: **0 errors / 72 warnings** (exactly the previously classified set;
  0 newly introduced).
- Secret scan: **0 findings** (working tree + history).
- C12 crop-alignment measurement: static drift ≤1.2dp / both tiers / all 5
  crops (masked NCC ≥0.96); idle-rotation deformation documented (the reason
  rotation was removed).
- Post-repair POCO render sims (scripts/out/c12_poco_*.png): contacting arc
  concentric on the ring band — single circle; sweep highlight on the band.

## FILES
- Modified: Theme.kt (LocalContentColor authority),
  SettingsHubScreen.kt (explicit hub label token),
  ModelDownloadManager.kt (transport repair),
  LocalTowerMockServer.kt (redirect route + UA capture for pins),
  ReferenceTowerVisual.kt (rotation removal, concentric arc, ring sweep,
  stronger breath), app/build.gradle.kts (0.3.9 / versionCode 12 / PACKAGE 9).
- Tests: ModelDownloadLifecycleTest.kt (3 new pins).
- Added evidence: docs/visual/tower414/C12_CROP_ALIGNMENT_MEASUREMENT.json;
  scripts/measure_g414_crop_alignment.py + scripts/find_uncolored_text.py
  (sandbox scripts, measurement provenance).

## CHECKPOINT
CP-G4.14-C12 — Owner round-2 intake + G-A..G-D repairs: DONE (this package).
State after this package: **GRAPH414_OWNER_RETEST_REPAIRS_PACKAGE_READY** with
`OWNER_VISUAL_RETEST_PENDING` (round 3) + `LLM_PHYSICAL_QUALIFICATION_PENDING`
unchanged — no physical verdict is self-granted.

## NEXT
1. Package 9 APKs (arm64-v8a / universal / x86_64) + SHA-256 receipt.
2. Push to graph-4/real-voice-pairing-archive-e2e (never main); CI arbitrates.
3. Owner physical retest round 3 on POCO X6 Pro: settings menu legibility
   (dark), Qwen download END-TO-END (G-B is transport — the real network is
   the arbiter), central-circle integrity during CONTACTING states, idle
   effects visibility (runbook §11–13 checklist).
