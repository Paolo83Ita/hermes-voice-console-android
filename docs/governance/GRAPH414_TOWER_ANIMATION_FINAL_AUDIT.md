# GRAPH 4.14 — TOWER ANIMATION FINAL AUDIT (CP-G4.14-C06, animation leg)

§10: visible-detectability audit of the twelve contracted states. Evidence
class: `docs/visual/tower414/animation/` — 12 state strips, 3 frames each
(initial / mid / later within the loop), rendered by
`scripts/gen_g414_animation_frames.py` which replicates the SHIPPED
ReferenceTowerVisual.kt equations (animation curves, alpha tables, placement
fractions, §40 color rules) over the SHIPPED raster layers.

Honest typing: these are OFFLINE RENDERER-LOGIC REPLICATIONS (the sandbox has
no emulator/device). They prove the equations and assets produce DISTINCT,
visible per-state frames; the on-device ordinary-speed visibility verdict is
the OWNER's (retest items 11–14 — typed PENDING, never self-granted).

## NODE
CP-G4.14-C06 (animation leg).

## FINDING — per-state table

| State | Authority (typed input) | Expected animation (contract) | Observed in frames | Distinct vs IDLE | Verdict |
| --- | --- | --- | --- | --- | --- |
| IDLE | ConsoleState idle | slow ring shimmer (90 s) + portal breathing (2.4 s, 0.72→1.0) | glow breathes; rings rotate imperceptibly per frame | baseline | PASS — visible but subtle |
| LISTENING | ConsoleState Listening/Capture | 5 incoming cyan lines + stronger portal | lines march toward the mechanism; portal 0.55→0.85 | YES (lines + stronger glow) | PASS |
| TRANSCRIBING | ConsoleState Transcribing | 12 sequential illumination ticks | active tick walks the ring | YES | PASS |
| LOCAL PROCESSING | Local/Thinking states | inner mechanism energy 0.7, NO remote conduit | inner glow stronger, no outer path | YES | PASS |
| CONTACTING TOWER | Contacting/Connecting/Queued AND !bridgeIsMock | outer path arc sweep (cyan) | arc sweeps 0→300° | YES | PASS — real-transport only |
| SPEAKING | ConsoleState Speaking | portal responds to TTS amplitude | glow 0.45→1.0 with the smoothed amplitude | YES | PASS |
| CONTINUOUS LISTENING | continuousActive | persistent listening pulse | same family as LISTENING, sustained | YES | PASS |
| PRIVACY MUTED | privacyMuted | red-bronze seal over the input path; energy 0.10 | X-seal over the mechanism; glow dimmed | YES | PASS |
| CHAT SPEECH MUTED | chatSpeechMuted && speaking | output path dims (energy 0.12) | energy drop visible while speaking | YES | PASS |
| MOCK MODE | BridgeModeAuthority.isMock | AMBER simulation pulse 0.35→0.80; NEVER cyan | amber portal + amber energy; zero cyan | YES (amber vs cyan) | PASS — §40 truth |
| DISCONNECTED | BridgeIndicatorRole.DISCONNECTED | no dedicated tower overlay — truth carried by the GRAY bridge indicator (§40 coherence: the tower never fakes states the authority does not declare) | base idle visuals; indicator gray | n/a (indicator carries it) | PASS (typed design decision) |
| ERROR | BridgeIndicatorRole.ERROR | same — truth carried by the RED indicator | base visuals; indicator red | n/a | PASS (typed design decision) |

Key frames inspected this session (IDLE / LISTENING / MOCK_MODE combined
proof): IDLE's motion is subtle (breathing only); LISTENING is clearly
stronger with visible incoming lines; MOCK is unmistakably AMBER — the mock
mode cannot borrow the cyan real-connection semantics.

LOW tier / reduced motion: `frozen = reducedMotion || low` stops ring
rotation and lines (alpha fades remain); the IDLE glow still breathes — the
animation is visible but reduced, per the quality ladder.

## ACTION
Generated + inspected the 12 state strips; verified the §40 mock/real color
coherence and the IDLE-subtlety / LISTENING-distinctness contract in the
frames.

## TEST
- Render script determinism (same inputs → same frames); shipped equations
  transcribed 1:1 from ReferenceTowerVisual.kt; 496/496 suite (renderer code
  compiled and pinned by the unit suite).

## FILES
- docs/visual/tower414/animation/{IDLE,LISTENING,TRANSCRIBING,
  LOCAL_PROCESSING,CONTACTING_TOWER,SPEAKING,CONTINUOUS_LISTENING,
  PRIVACY_MUTED,CHAT_SPEECH_MUTED,MOCK_MODE,DISCONNECTED,ERROR}.png
- docs/visual/tower414/animation/ANIMATION_EVIDENCE.json (method + frame map)
- scripts/gen_g414_animation_frames.py

## EVIDENCE
- The 12 strips + JSON; combined proof image inspected (IDLE/LISTENING/MOCK).

## CHECKPOINT
CP-G4.14-C06 (animation leg) — DONE. Physical ordinary-speed visibility:
OWNER_VISUAL_RETEST_PENDING.

## NEXT
§11 Owner reference comparison.
