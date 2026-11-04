# XIAOMI_GRAPH4_BETA_PACKAGE — Graph 4.6 (§9)

Owner: Paolo Lombisani · Device: Xiaomi 11 Lite 5G NE (2109119DG) · arm64-v8a · Android 11+
**No Android Studio is required at any point of this package.**

This document is the package manifest and index. The step-by-step instructions live in
`docs/INSTALL_XIAOMI_GRAPH4_FINAL.md` (same repo) and remain authoritative for procedure.

## 1. Package contents (cloud artifacts, commit-bound)

| # | Item | Source | Verification |
| --- | --- | --- | --- |
| 1 | `app-arm64-v8a-debug.apk` — the Xiaomi install APK (smallest; only the arm64-v8a native set) | android-debug-build artifact `hermes-voice-console-debug-apk-<commit>` | SHA-256 printed in the build log + `APK_SIZE_AND_ABI_REPORT.md` |
| 2 | `app-universal-debug.apk` — all-ABI fallback (installs on the Xiaomi too; also serves the x86_64 emulator) | same artifact | same |
| 3 | `app-x86_64-debug.apk` — emulator/CI companion | same artifact | same |
| 4 | Test + lint report artifacts | same run | evidence trail |
| 5 | `docs/INSTALL_XIAOMI_GRAPH4_FINAL.md` | repo | procedure |
| 6 | `docs/DEVICE_QUALIFICATION_CHECKLIST.md` + this package | repo | recording sheets |

## 2. Install + upgrade path (runbook §1–§3)

1. Actions → latest green android-debug-build → Artifacts → download the ZIP.
2. Verify SHA-256 (phone file manager or PowerShell `Get-FileHash`) against the receipt
   — mismatch = STOP.
3. UPGRADE: open the APK → Update (settings, drafts, language and installed models
   survive — this IS part of the test). FRESH: uninstall first, then install.
4. First launch: six-step onboarding (runbook §4).

## 3. Test matrix (runbook §5–§14 — summary with the directive §9 items)

| Directive item | Where | Pass signal |
| --- | --- | --- |
| Language testing (Italian UI complete + 5 locales) | runbook §5 + Impostazioni → Generali → Lingua | zero ordinary English in Italian mode |
| Tower animation review | runbook §6 | monumentality + chat-focus shrink + no ring clipping |
| Microphone | runbook §8 | push-to-talk partial → final transcript; PRIVACY MUTED releases the mic physically |
| STT | runbook §9 | vosk download 49.7 MB → SHA-256 gate → install → Active-runtime row flips honestly → real Italian transcript recorded |
| TTS | runbook §10 | system TTS speaks + barge-in interrupt works; Piper voices verify + honest runtime row |
| Model downloads | runbook §9–§11 | progress, pause/resume, hash verify, receipts |
| Optional local LLM | runbook §11 + Local Intelligence | DEFAULT OFF; enable → delivery chain; typed LLM_RUNTIME_PENDING_VERIFIED_SOURCE stays honest |
| QR scanner | runbook §12 | camera permission, full-screen, Back returns to the studio; pairing SIMULATED |
| Text chat | runbook §7 | send + honest attribution + draft survival across rotation/backgrounding |
| Restart | runbook §14 | language + drafts + models survive reboot and process kill |
| RAM | runbook §13–§14 | no OOM under 3–4 heavy background apps; unload/reload of the voice pack stable |
| Temperature | runbook §13 | ≤ 42 °C surface after 30 min; recorded before/after the HIGH-profile hour |
| Four-hour soak | runbook §13 | battery %, temperature, ANR check, glitch photos every ~20 min voice turn |
| Lifecycle residual arbitration | the same session | if the INITIALIZED→DESTROYED residual reproduces on hardware: record + evidence (disposition PHYSICAL_XIAOMI_RECOVERY_REQUIRED is up for revision by THIS device) |

## 4. Evidence to return to the mission

1. Screenshots (every defect + the three anchor screens: Console in Italian, Archive
   chamber, Active-runtime card).
2. Diagnostics → REDACTED report text (it contains the runtime read-back rows).
3. The commit hash of the installed APK + both SHA-256 values.
4. Soak table (time, battery %, temperature, anomalies).
5. Verdict per runbook §16 vocabulary.
