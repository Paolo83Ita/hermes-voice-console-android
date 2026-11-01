# MOBILE COMPOSITION SPEC — 1080 × 2400 PORTRAIT (Xiaomi 11 Lite 5G NE)

Target device: Xiaomi 11 Lite 5G NE, model 2109119DG, Android 14, HyperOS 2.0.4.0 UKOEUXM,
8 GB RAM, 128 GB storage, display 1080×2400 (~395 ppi class; density bucket on device to be
confirmed by ADB read-back — TARGET_PENDING_DEVICE_QUALIFICATION).

## 1. Portrait composition (primary)

```
┌──────────────────────────────┐ 0
│ COMMAND FRIEZE (330 px)      │ HERMES TOWER · VOICE ORACLE CONSOLE
│ title / subtitle / seals     │ mode seals · connection · mic · queue · update
├──────────────────────────────┤ 330
│                              │
│   ORACLE CORE (1420 px)      │ L0 sky · L1 temple · L2 colonnade · L3 bronze ring
│   L0..L9 live window         │ L4 mechanic rings · L5 conduits · L6 portal+sigil
│                              │ L7 stairs+gateway · L8 state seals · L9 messengers
│                              │ state label strip at stage foot
├──────────────────────────────┤ 1750
│ TRANSCRIPT PANEL             │ attribution seals (LOCAL CORE / TOWER)
│ captions (live region)       │
│ medallions row               │ mute · VOICE/WAKE (large) · inject · stop
│ chamber foot                 │ archive link · plain functional names
└──────────────────────────────┘ 2400
```

Region budget: frieze 13.75% · core 59.2% · lower 27.1% (mission §6 window respected).
The console page does NOT scroll on the primary axis; transcript shows the last turns and
history opens the archive chamber (matches V0.1 behavior of limited turns + link).

## 2. Touch and readability

- All interactive elements ≥48 dp; medallions are 72 dp/88 dp circles on-device.
- Focus order: mode seals → connection seal → mic seal → queue seal → transcript region →
  mute → voice/wake → inject → stop → archive link.
- State label strip and captions are live regions (Polite) — preserved from V0.1.
- Text: body 15–17 sp; frieze title is display-only (decorative text also duplicated in
  contentDescription for TalkBack).

## 3. Landscape compatibility (secondary)

- Core remains primary on the left (≈44% width); transcript + controls on the right.
- Frieze compresses (compact seals); no layout inversion; no duplicated controls.
- Verified by preview `p11_landscape.png` (2400×1080).

## 4. Asset layering on-device

- Static architectural depth (sky, temple, colonnade, stairs): layered WebP drawables
  generated from the same geometry as the preview SVG (asset pipeline: see
  ASSET_PIPELINE_DECISION.md).
- Dynamic rings/conduits/sigils/messengers: Compose Canvas over the static layers.
- Burn-in offsets applied per region (title, core, seals, transcript, medallions).
