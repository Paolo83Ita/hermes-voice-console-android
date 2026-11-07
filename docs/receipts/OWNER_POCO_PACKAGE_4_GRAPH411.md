# OWNER POCO PACKAGE 4 — GRAPH 4.11 (PHYSICAL EXPERIENCE COMPLETION)

Owner: Paolo Lombisani · Branch: `graph-4/real-voice-pairing-archive-e2e` · 2026-11-07

## Devices

- **POCO X6 Pro** (8 GB / 256 GB) — preferred device, BALANCED + HIGH-short test
- **Xiaomi 11 Lite 5G NE** (8 GB / 128 GB) — LOW/BALANCED, compact-layout test

## Build identity (verify in-app: About & Safety → Build information)

| Field | Value |
| --- | --- |
| versionName | **0.3.4-alpha-graph4.11** |
| versionCode | **7** (must exceed the installed 6 / 0.3.3-alpha-graph4.10-owner-repair) |
| Build label | **POCO XIAOMI OWNER PACKAGE 4** |
| Commit | see the in-app row (the CI-built APK stamps the full SHA) |
| CI artifacts | arm64-v8a (preferred) + universal + x86_64, sha256 published by the workflow |

> NOTE: the local validation artifacts in `download/` (`…v0.3.4-graph4.11-5a567a3.apk`,
> arm64 sha256 `501792715d338a46d9e7f1f74e748c43f3387a38e8a445efc6952765c187e994`,
> universal `d5481f28…1bc43f6`) are the SAME code at `5a567a3` built outside CI —
> their in-app Git-SHA row reads `local`. The authoritative package is the CI build
> of this commit (the workflow stamps the real SHA + run id).

## What Graph 4.11 adds on top of Package 3

1. **ST-54 "Voice A/B comparison"** (Self-Test Center → STANDARD/FULL): the ACTIVE
   voice speaks the preview phrase, then the OTHER installed voice speaks the SAME
   phrase — the two-voice audition the Owner asked for ("Voice A → play → Voice B →
   play → prove difference"). The selection is never touched by the audition.
2. **10-turn continuous listening pinned by tests** (loop timing per turn + echo
   rejection for ten consecutive spoken responses).
3. **No-English pin**: every response category answers Italian under the Italian
   locale (46-category catalog-level test on top of the 4.10 producer tests).
4. **Central Tower measured repairs**: the sign is complete ("HERMES TOWER" /
   "MULTI-LLM ORACLE NETWORK" unclipped), the lateral arrow fragments and the sign
   sliver are gone from the rear/rings, and the four static bands now render at
   their true positions and sizes (they were mis-stacked at the stage top).
5. Version/label bump for traceability (7 / 0.3.4-alpha-graph4.11 / PACKAGE 4).

## Install (no Android Studio needed)

1. Copy the arm64-v8a APK to the phone (USB / cloud drive).
2. HyperOS: Settings → About phone → tap the build, or install with Files →
   allow "install unknown apps" for the source app.
3. If Package 3 is installed, the update installs OVER it (same signature,
   versionCode 7 > 6). Data and downloaded voices survive.
4. Open the app → About & Safety → Build information → verify the identity table
   (versionName 0.3.4-alpha-graph4.11, code 7, label POCO XIAOMI OWNER PACKAGE 4,
   the installed-APK sha256 row must match the published artifact hash).

## Retest runbook (POCO X6 Pro — ~25 minutes)

| # | Test | Expected (honest) |
| --- | --- | --- |
| 1 | Chat language: set app language Italian, ask "chi sei", "che sai fare", "qual è lo stato" | every local reply in Italian — no English response (the SIMULATED truth stays verbatim) |
| 2 | Voice switching: Models and Voice → install BOTH piper voices → activate Riccardo → wait for "ACTIVE (runtime read-back complete)" → speak a phrase | the voice REALLY changes (audible) |
| 3 | ST-54: Self-Test Center → STANDARD → find "ST-54 Voice A/B comparison" → run | voice A speaks, then voice B speaks the same phrase — YOU hear the difference; ST-54 prints both identities |
| 4 | Chat mute: tap the speaker control → "Silenzia risposte vocali" → send a message | text appears, NO audio; tap again "Attiva risposte vocali" → audio returns; TTS stop works while speaking |
| 5 | Continuous listening: Voice and Audio → Listening mode → "Ascolto continuo con Console aperta" → speak 10 turns in a row | 10 turns WITHOUT pressing any button; the console never transcribes its own replies; the session keeps running |
| 6 | Echo check: after a long reply, stay silent 2 s | no phantom user turn appears from the spoken answer |
| 7 | Thermal/battery: after the 10-turn soak note temperature and battery | warm, never hot; battery drop reasonable |
| 8 | Tower visual (NEW): look at the Console Tower | the sign reads "HERMES TOWER / MULTI-LLM ORACLE NETWORK" completely; NO arrows, NO side towers, NO model names, NO sky patchwork; the rings spin around the cyan portal |
| 9 | Quality profiles: Settings → visual quality → LOW / BALANCED / HIGH-short | BALANCED smooth; HIGH not the default; LOW calmer |

## Retest runbook (Xiaomi 11 Lite 5G NE — ~15 minutes)

| # | Test | Expected (honest) |
| --- | --- | --- |
| 1 | Connection Studio at your real width | primary action full-width; NO one-character-per-line rows; no button with a squeezed label |
| 2 | Console controls | ONE primary control + a few secondary; every label readable ("Parla", "Interrompi", "Silenzia/Attiva risposte vocali", "Ascolto continuo") |
| 3 | Headers on every screen | titles fit (two lines max), no clipped monumental header |
| 4 | Raw English states | NO raw enum ("Invalid", "Validated"…) in normal UI — Italian labels only |
| 5 | Chat mute + 5 continuous turns | mute works; 5 turns stable; no self-echo |
| 6 | Tower visual | same checks as POCO #8 |
| 7 | RAM/thermal after the session | app stays responsive; no system-kill |

## Evidence to return

- In-app Build information screenshot (both devices).
- ST-54 result line (both identities + PASS/PENDING state).
- 10-turn / 5-turn session outcome + temperature note.
- Console Tower screenshots (POCO + Xiaomi) — these decide the visual verdict.
- Any defect: what you did, what you saw, one screenshot.

## Honest states after this package

- `VOICE_SWITCHING_OWNER_VERIFICATION_PENDING` — until YOU hear the A/B difference.
- `OWNER_VISUAL_RETEST_PENDING` — until YOU accept the Tower appearance.
- Physical pass states — Owner-only, granted by your retest.
- `REAL_TOWER_PENDING_GRAPH_1_7` — unchanged (P2, next energy focus per your note:
  Tower visual → Archive → Graph 1.7 pairing).
