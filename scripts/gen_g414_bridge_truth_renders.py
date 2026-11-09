#!/usr/bin/env python3
"""GRAPH 4.14 §5/§6 — BRIDGE TRUTH + MARBLE ROUTE preview renders.

Offline PIL renders from the SHIPPED token values (Chrome.kt role→color map,
MonumentalMarble palette). These are PREVIEWS for the Owner package — the
physical verdict stays Owner-only (typed in the audit MDs).

Outputs (docs/visual/tower414/renders/):
  BRIDGE_TRUTH_PILLS.png        the 7 indicator roles × obsidian/marble
  CONSOLE_MOCK_MODE_MARBLE.png  the Marble Console with the amber SIMULAZIONE pill
  CONSOLE_MOCK_MODE_OBSIDIAN.png the Obsidian Console with the amber SIMULAZIONE pill
"""
import os
from PIL import Image, ImageDraw, ImageFilter

OUT = "docs/visual/tower414/renders"
os.makedirs(OUT, exist_ok=True)

W, H = 760, 96

# shipped role→color tokens (Chrome.kt) and palettes (MonumentalColor.kt)
AMBER = (232, 163, 61)
CYAN = (0, 229, 255)
BRONZE_LIGHT = (185, 140, 74)
INK_SECONDARY = (138, 131, 117)   # obsidian inkSecondary
REDBRONZE = (222, 122, 94)
ROLES = [
    ("SIMULAZIONE", AMBER, "mock://tower.simulator — handshake CONNECTED"),
    ("BRIDGE LOCALE PRONTO", BRONZE_LIGHT, "LOCAL_BRIDGE, UNCONFIGURED"),
    ("TOWER RAGGIUNGIBILE", AMBER, "REAL_TOWER, CONNECTING/unauthenticated"),
    ("TOWER CONNESSA", CYAN, "REAL_TOWER, CONNECTED + TLS ACTIVE"),
    ("TOWER OPERATIVA", CYAN, "capability negotiation passed (+ mark)"),
    ("TOWER DISCONNESSA", INK_SECONDARY, "DISCONNECTED"),
    ("ERRORE BRIDGE", REDBRONZE, "FAILED"),
]


def pill_row(bg, ink, sub, path):
    img = Image.new("RGB", (W, H * len(ROLES) + 40), bg)
    d = ImageDraw.Draw(img)
    d.text((16, 10), f"BRIDGE TRUTH INDICATORS — {path}", fill=ink)
    for i, (label, color, note) in enumerate(ROLES):
        y = 34 + i * H
        d.rounded_rectangle([16, y, W - 16, y + H - 14], radius=18,
                            fill=tuple(min(255, c + 18) for c in bg) if bg[0] < 120 else bg,
                            outline=color, width=2)
        d.ellipse([40, y + 22, 60, y + 42], fill=color)
        d.text((74, y + 22), label, fill=ink if color != INK_SECONDARY or bg[0] < 120 else (255, 255, 255))
        d.text((74, y + 44), note, fill=sub)
    img.save(os.path.join(OUT, path))
    print(path, "ok")


# obsidian + marble chrome values
pill_row((7, 9, 13), (240, 234, 218), (168, 160, 142), "BRIDGE_TRUTH_PILLS_OBSIDIAN.png")
pill_row((234, 227, 210), (36, 31, 20), (102, 94, 76), "BRIDGE_TRUTH_PILLS_MARBLE.png")


def console_mock(path, dark):
    """Marble/Obsidian console with the amber SIMULAZIONE pill (§5 evidence)."""
    bg = (10, 12, 17) if dark else (242, 239, 231)
    ink = (240, 234, 218) if dark else (32, 30, 25)
    w, h = 420, 760
    img = Image.new("RGB", (w, h), bg)
    d = ImageDraw.Draw(img)
    # status bar / nav bar chrome
    d.rectangle([0, 0, w, 26], fill=tuple(max(0, c - 8) for c in bg))
    d.rectangle([0, h - 22, w, h], fill=tuple(max(0, c - 8) for c in bg))
    d.text((12, 6), "HERMES", fill=ink)
    # frieze band
    d.rounded_rectangle([14, 36, w - 14, 64], radius=10, outline=(90, 80, 60), width=1)
    d.text((24, 42), "CONSOLE — MOCK BRIDGE", fill=ink)
    # bridge pill: amber dot + SIMULAZIONE — NEVER cyan
    d.rounded_rectangle([90, 78, w - 90, 106], radius=14, outline=AMBER, width=2)
    d.ellipse([104, 84, 118, 98], fill=AMBER)
    d.text((126, 84), "SIMULAZIONE", fill=(232, 163, 61))
    # tower stage (dark halo is allowed locally under Marble §18)
    stage_top = 130
    stage_h = 380
    d.rounded_rectangle([14, stage_top, w - 14, stage_top + stage_h], radius=16,
                        fill=(8, 9, 12) if dark else (226, 219, 203))
    halo = Image.new("RGBA", (w, stage_h), (0, 0, 0, 0))
    hd = ImageDraw.Draw(halo)
    hd.ellipse([w // 2 - 130, stage_h // 2 - 130, w // 2 + 130, stage_h // 2 + 130],
               fill=(0, 0, 0, int(255 * 0.42)))
    halo = halo.filter(ImageFilter.GaussianBlur(30))
    stage = Image.new("RGBA", (w, stage_h), (0, 0, 0, 0))
    stage.alpha_composite(halo)
    # amber mock pulse rings (NO cyan — §40)
    dd = ImageDraw.Draw(stage)
    for r, a in ((58, 110), (84, 80), (110, 55)):
        dd.ellipse([w // 2 - r, stage_h // 2 - r, w // 2 + r, stage_h // 2 + r],
                   outline=(232, 163, 61, a), width=3)
    dd.ellipse([w // 2 - 26, stage_h // 2 - 26, w // 2 + 26, stage_h // 2 + 26],
               fill=(232, 163, 61, 90))
    img.paste(stage.convert("RGB"), (14, stage_top))
    d = ImageDraw.Draw(img)
    d.text((w // 2 - 84, stage_top + 8), "AMBER PULSE — NO CYAN", fill=(232, 163, 61))
    # composer
    d.rounded_rectangle([14, h - 118, w - 14, h - 70], radius=14,
                        outline=(120, 110, 90), width=1)
    d.text((28, h - 102), "Parla ora — mock bridge attivo", fill=(150, 140, 120))
    d.rounded_rectangle([14, h - 62, 120, h - 34], radius=10, outline=(120, 110, 90), width=1)
    d.text((30, h - 54), "Interrompi", fill=ink)
    img.save(os.path.join(OUT, path))
    print(path, "ok")


console_mock("CONSOLE_MOCK_MODE_OBSIDIAN.png", dark=True)
console_mock("CONSOLE_MOCK_MODE_MARBLE.png", dark=False)
print("done ->", OUT)
