# OWNER POCO XIAOMI RETEST PACKAGE 3 — GRAPH 4.10 (CP-G4.10-014)

Status: **POCO_XIAOMI_RETEST_PACKAGE_3_READY** (pre-device terminal state).
No Android Studio required. Only the Owner can grant
`POCO_XIAOMI_CHAT_VOICE_LISTENING_LAYOUT_AND_VISUAL_RETEST_PASSED`.

## Devices

| | DEVICE A | DEVICE B |
| --- | --- | --- |
| Model | POCO X6 Pro | Xiaomi 11 Lite 5G NE |
| RAM / storage | 8 GB / 256 GB | 8 GB / 128 GB |
| ABI | arm64-v8a | arm64-v8a |

## Build identity (stamped inside the APK dex — verify in-app: About & Safety → Build information)

| Field | Value |
| --- | --- |
| versionName | 0.3.3-alpha-graph4.10-owner-repair |
| versionCode | 6 (MUST show 6 — a lower number is a stale install) |
| Build label | POCO XIAOMI OWNER REPAIR 3 |
| Full Git SHA | 653ae0f… (in-app Build information card) |
| Variant read-back | arm64 build reports ARM64_V8A; universal reports UNIVERSAL |
| Signature | debug-signed — UNINSTALL the previous build first if the installer reports a signature conflict (in-app data is safe to remove: models re-download from the Sanctum) |

## Artifacts (this build, hashed)

| Variant | Size | SHA-256 |
| --- | --- | --- |
| **app-arm64-v8a-debug.apk (PREFERRED, both devices)** | 116,561,845 B | `4ffefb80d4a51992…` (full hash in APK_RECEIPT row of WORK_LOG) |
| app-universal-debug.apk (fallback) | 246,208,674 B | `97e6f8124d59bcbf…` |
| app-x86_64-debug.apk (test only) | 121,277,812 B | `0312db163e39477e…` |

Unit tests 412/412 GREEN · lint 0 errors · dex stamps verified
(versionName + label inside classes11/classes13.dex) · ABI sets verified.

## What was repaired since the failed physical retest (Owner findings → fixes)

| Finding | Repair in this package |
| --- | --- |
| P0-01 English chat responses | ResponseLocaleResolver (5-level precedence) + ResponseCatalog (43 categories × it/en/es/fr/de); every local producer consumes the authority; the visible text IS the spoken text (§6 pin) |
| P0-03 selected voice not consumed | VoiceSelectionAuthority + VoiceRuntimeFactory: Activate now runs the six-condition runtime truth (files→load→synthesis→playback→read-back) before the voice is ACTIVE; Sanctum shows Qualifying/ACTIVE/Failed |
| P0-04 speaker button | One functional speaker control: speaking→Interrompi voce (StopCircle), idle→Silenzia/Attiva risposte vocali, MANUAL→Ascolta risposta; every assistant message has Ripeti |
| P0-05 chat speech mute | ChatSpeechOutputPolicy (AUTO_SPEAK/MUTED/MANUAL), restart-persistent; MUTED keeps text visible and stops the utterance in flight |
| P0-06 continuous listening | Ascolto continuo control + §15 loop + echo prevention (the console can never transcribe its own response) |
| P0-07 "parla invece" | → **Parla**; composer stop → **Interrompi**; send → **Invia** (5 locales) |
| P0-08 Xiaomi layout | Connection Studio rearchitected (§21): full-width PRIMARY, stacked actions at COMPACT width, §20 responsive headers on all routes |
| P0-09 "Invalid" raw state | §23 StateLabels: Non valida / Validata / Connessa / … (raw enums only in Diagnostics) |
| P0-10/11 Tower visual | Layered central Tower reconstructed from the Owner image (10 layers, ASSET_MANIFEST.json, 11 animated states, LOW/BALANCED/HIGH profiles) |

## Retest runbook (POCO X6 Pro — §40, ~20 minutes)

1. Install the arm64 APK; open About & Safety → confirm versionCode **6**, label **POCO XIAOMI OWNER REPAIR 3**, variant ARM64_V8A.
2. Settings → Language → Italiano (if not already). 
3. **Chat language**: type "ciao" → the reply must be ITALIAN text and ITALIAN speech (P0-01 dead). "chi sei" → the Owner-mandated Italian identity sentence.
4. **Voice A/B** (P0-03): Models and Voice → download/activate *Riccardo* → wait for "Voce ATTIVA", press Ascolta on a reply → note the voice; activate *Paola* → Ascolta again → the two voices must be audibly DIFFERENT.
5. **Chat mute** (P0-05): press Silenzia risposte vocali → send a message → text appears, NO speech; press Attiva risposte vocali → speech returns.
6. **Speaker control** (P0-04): while speaking, the control shows Interrompi voce and stops the CURRENT utterance only; the next response still speaks.
7. **Continuous listening** (P0-06): press the continuous control → speak 5 turns without touching the phone → the console must NOT hear its own spoken responses (echo guard). 
8. **Parla label** (P0-07): the composer shows Parla / Interrompi / Invia.
9. 10-turn continuous soak; note temperature, battery delta, RAM (Diagnostics), visual profile BALANCED (+ a short HIGH test).
10. **Tower visual** (P0-10/11): compare the central Tower with the provided reference image; rings rotate, portal breathes, mute seals the gate red-bronze.

## Retest runbook (Xiaomi 11 Lite 5G NE — §40, ~15 minutes)

1. Install the arm64 APK; confirm build identity (versionCode 6).
2. **Responsive header**: every screen title readable, ≤2 lines, nothing clipped at the compact width.
3. **Connection Studio**: "Salva e connetti" full-width; "Ripristina configurazione precedente" and "Ripristina modalità Mock" render as FULL WORDS (no vertical letters); press Reset to Mock — the state line shows the ITALIAN state word, never "Invalid" (P0-08/P0-09 dead).
4. **Model Sanctum / Voice and Audio**: all actions reachable by scroll, ≥48 dp, no overlap; listening-mode card shows the 4 options in Italian.
5. **Chat + mute + continuous**: 5 continuous turns; no self-echo; LOW profile default, then BALANCED.
6. Record temperature and RAM; the 4-hour soak can follow later.

## Evidence to return

Per device: build-identity screenshot, chat-language screenshot, voice A/B
verdict (audible difference yes/no), mute/continuous behavior, Connection
Studio screenshot, Tower visual verdict vs the reference image, crashes (none
expected), temperature/RAM/battery notes. The Owner's verdicts will be
recorded in `OWNER_POCO_RETEST_RECEIPT.md` (or a Graph 4.10 successor) and only
the Owner may set the passed state.

## Honest states

- `POCO_XIAOMI_RETEST_PACKAGE_3_READY` — set by this receipt (pre-device).
- `VOICE_SWITCHING_OWNER_VERIFICATION_PENDING` — unchanged until the Owner hears the A/B difference.
- `OWNER_VISUAL_RETEST_PENDING` — unchanged until the Owner judges the Tower.
- `RESPONSIVE_UI_QUALIFIED` — remains UNCLAIMED (forbidden token) until the Xiaomi physical pass.
- Real pairing: `REAL_TOWER_PENDING_GRAPH_1_7` — unchanged.
