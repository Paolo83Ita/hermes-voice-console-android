# OWNER PHYSICAL RETEST FAILURE RECEIPT

- Mission: HERMES VOICE CONSOLE ANDROID — OWNER PHYSICAL RETEST FAILURE
- Owner: Paolo Lombisani
- Physical test device: POCO X6 Pro (8 GB RAM / 256 GB storage)
- Date of record: 2026-11-06
- Mission mode: `CONTINUE_FROM_LATEST_VERIFIED_REMOTE_AUTHORITY`
- Remote tip read back at intake: `53841a5` (`graph-4/real-voice-pairing-archive-e2e`, full SHA `53841a5154c10aa7177c95fa9c9517b5476e436e`) — fresh `git fetch` + `git ls-remote` read-back performed before any change.
- State at intake: `OWNER_RETEST_FAILED_REPAIR_REQUIRED`
- Status: `OWNER_PHYSICAL_EVIDENCE` / `CHECKPOINT_REJECTED_PENDING_REPAIR`

## 1. What the Owner reported

The Owner installed and tested the supplied beta on the physical POCO X6 Pro.
The application installs and launches. The following physical defects remain:

| ID | Observation | Typed token |
| --- | --- | --- |
| PHYSICAL-FAIL-01 | The local spoken response remains English | `ITALIAN_SYSTEM_TTS_READBACK_MISMATCH` (candidate class, to be confirmed by the call-path trace) |
| PHYSICAL-FAIL-02 | `HIGH_PITCH_ENGLISH_TTS_OUTPUT` — high-pitched synthetic voice, uncomfortable to hear. No speaker identity/gender is inferred or described in product terminology. | `HIGH_PITCH_ENGLISH_TTS_OUTPUT` |
| PHYSICAL-FAIL-03 | In **Models and Voice**, the text inside the **Verify** button remains incorrectly laid out | `VERIFY_BUTTON_LAYOUT_DEFECT` |
| PHYSICAL-FAIL-04 | The microphone activation control uses a symbol that appears the same as the mute symbol; the two controls are not semantically distinguishable | `MICROPHONE_MUTE_ICON_COLLISION` |
| PHYSICAL-FAIL-05 | The Owner perceives no material improvement compared with the previously tested build | `OWNER_RETEST_FAILED_REPAIR_REQUIRED` |

Prohibited states (NOT used in this receipt or in any governance file this cycle):
`ITALIAN_TTS_OWNER_VERIFIED`, `RESPONSIVE_UI_QUALIFIED`, `POCO_RETEST_PASSED`.
The previous implementation checkpoint is **not Owner-accepted**.

## 2. Installed-build provenance — UNPROVEN, and that is itself the defect

The supplied package (`0.3.0-alpha-graph4`, versionCode 3, commit `bf34014`)
exposed only `versionName`/`versionCode` inside About & Safety. It had **no
in-app build identity** (Git SHA, build timestamp, workflow run ID, APK
variant, APK SHA-256), so the Owner cannot prove from inside the app which
binary physically ran. The retest verdict therefore carries an unresolvable
provenance question until the repair build ships its build-identity panel.

Honest consistency note (recorded, not guessed away): the previously binary-inspected
package `ca8980b` (`0.2.0-debug`) contains no real TTS engine at all
(`MockTextToSpeechEngine` produces no audio) — it could not have produced the
audible English answer. The `0.3.0-alpha-graph4` build CAN speak and its
fail-open speak path can produce exactly PHYSICAL-FAIL-01/02 (see §3). Both
fail-01/02 observations are therefore *consistent with* the new build having
run, but the app must now PROVE it — the repair build exposes the full build
identity in-app so the next retest is self-evidencing without ADB.

Repair (this cycle): `PHYSICAL_APK_PROVENANCE_REPAIR_REPORT.md` +
Settings → About and Safety → **Build information** + Diagnostics → **Runtime
truth** build identity rows + a compact build identity footer on the main
Diagnostics screen + **Copy build identity** + **Export secret-clean
diagnostic receipt**. Authoritative values (version, Git SHA, build
timestamp, workflow run ID, build label) are injected at BUILD time; the
installed-APK SHA-256 and the APK variant are MEASURED on device from the
real installed file (never invented).

## 3. Call-path trace verdict (pre-repair, code-proven on `53841a5`)

The full trace lives in `PHYSICAL_TTS_CALL_PATH_REPORT.md`. The decisive
pre-repair facts:

1. `ConsoleScreen` → `ScriptedConsoleDriver.runPipeline` → `speakAndFinish` →
   `SystemTtsDomainAdapter.speak(text, rate)` — **the `rate` parameter is
   discarded by the adapter** — → `AndroidSystemTtsEngine.speak(text)`.
2. `AndroidSystemTtsEngine.speak()` re-negotiates language/voice and then
   calls `engine.speak(...)` **regardless of the negotiation verdict** — the
   pre-repair path is FAIL-OPEN: when the POCO's engine has no usable Italian
   voice, the engine's default (English) voice speaks anyway.
3. No `setSpeechRate`/`setPitch` call exists anywhere in the pre-repair
   pipeline — the heard voice is the engine's default voice character
   (`HIGH_PITCH_ENGLISH_TTS_OUTPUT`), not an app-chosen profile.
4. `negotiateAndApply` publishes the typed read-back (`ITALIAN_VOICE_*`,
   `FALLBACK_VOICE_TRIGGERED`), but no speak gate consumes it before audio.
5. Single `AndroidSystemTtsEngine` instance exists (`AppGraph.voiceTts`) shared
   by the text driver and the voice pipeline — no stale multi-instance drift;
   `AppGraph` holds no second instance after locale changes.
6. `MockTextToSpeechEngine` is NOT reachable in the app composition root
   (test seam only); `Inactive*` voice stubs are not on the TTS path.

Exact defect classes assigned (per the Owner's instruction to classify, not guess):

| Token | Verdict |
| --- | --- |
| `LANGUAGE_TO_VOICE_ROUTING_DEFECT` | CONFIRMED (fail-open speak gate absent — the routing exists but its verdict never blocks audio) |
| `ITALIAN_VOICE_NOT_ACTIVE` | CONFIRMED runtime state on the POCO when Italian voice data is absent/not enumerated |
| `FALLBACK_VOICE_TRIGGERED` | CONFIRMED audible consequence (the engine's default English voice spoke) |
| `TTS_CONFIGURATION_DRIFT` | CONFIRMED observability gap (rate/pitch never applied; no in-app identity/telemetry) |
| `WRONG_VOICE_SELECTION` | NOT CONFIRMED — selection prefers offline Italian correctly; the defect is that a failed selection still speaks |

## 4. Repair scope bound to this receipt (Owner priority override, unchanged)

1. Italian TTS fail-closed (§4 of the Owner brief): never speak a non-Italian
   voice while Italian is the effective language; typed
   `ITALIAN_SYSTEM_TTS_DATA_MISSING` / `ITALIAN_SYSTEM_TTS_READBACK_MISMATCH`;
   `TEXT_ONLY_SAFE_DEGRADED`; Italian message + open-TTS-settings +
   voice-data-install actions.
2. Speech-profile authority (§5): rate 0.96f, pitch 0.95f defaults, safe
   ranges (speed 0.75–1.30, pitch 0.75–1.25), Voice & Audio → Speech output
   controls, preview phrase, persistence + read-back.
3. Verify button targeted repair (§8): adaptive action layout — primary
   actions direct, secondary in **More actions**, ≥48 dp touch height,
   maxLines 2, centered, no fixed narrow width.
4. Microphone / privacy-mute / TTS-stop semantic separation (§9/§10): unique
   icon + label + contentDescription + state per control.
5. Build identity panel (§1/§2) + version `0.3.1-alpha-graph4-owner-repair`
   (versionCode 4) + build label `POCO OWNER REPAIR 1` + install guide +
   `POCO_X6_PRO_OWNER_RETEST_PACKAGE_2`.

## 5. Required states

- Pre-device state to reach this cycle: `POCO_RETEST_PACKAGE_2_READY`.
- Physical success state (Owner-confirmed only):
  `POCO_ITALIAN_TTS_UI_AND_CONTROLS_RETEST_PASSED`.
- The next Owner test is the §13 retest flow in the Owner brief (short and
  conclusive: build identity → Italian → Voice & Audio records → preview
  phrase → Verify button → distinct controls → export receipt).

## 6. Honesty boundaries

- No state in this receipt claims the repair works on the POCO; it claims the
  repair is implemented, unit-tested and packaged, and that the fail-open
  class is dead in code.
- The provenance question of the retested APK cannot be closed retroactively;
  it is closed forward by the build-identity panel.
- No speaker identity or gender is described anywhere; the technical token is
  `HIGH_PITCH_ENGLISH_TTS_OUTPUT`.
- Emulator/JVM results are labeled as such; ST-51 on JVM reports
  `QUALIFICATION_PENDING_DEVICE`.
