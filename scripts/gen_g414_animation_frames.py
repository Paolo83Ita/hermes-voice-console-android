#!/usr/bin/env python3
"""GRAPH 4.14 §10 — TOWER ANIMATION FINAL AUDIT (offline frame replication).

The sandbox has no emulator: each state's frames are rendered by replicating
the SHIPPED ReferenceTowerVisual.kt equations (animation curves, colors,
placement fractions) over the SHIPPED raster layers. This is renderer-logic
evidence, NOT device capture — the physical visibility verdict stays with the
Owner retest (typed PENDING in the audit MD).

Outputs: docs/visual/tower414/animation/<STATE>.png (3 frames: initial/mid/later)
         docs/visual/tower414/animation/ANIMATION_EVIDENCE.json
"""
import json
import math
import os
import numpy as np
from PIL import Image, ImageDraw

RES = "app/src/main/res/drawable-nodpi"
OUT = "docs/visual/tower414/animation"
os.makedirs(OUT, exist_ok=True)

STAGE_W = 640
STAGE_H = int(STAGE_W * 903 / 637)

PLACE = {
    "architecture": (0.0, 0.0, 1.0, 1.0),
    "crown_sign": (0.150, 0.030, 0.700, 0.195),
    "ring_outer": (0.180, 0.300, 0.640, 0.310),
    "ring_middle": (0.265, 0.340, 0.470, 0.235),
    "ring_inner": (0.370, 0.385, 0.260, 0.155),
    "portal": (0.300, 0.705, 0.400, 0.220),
    "energy_mask": (0.0, 0.0, 1.0, 1.0),
}
# shipped colors (MonumentalMarble/Obsidian cyanCore/amber/redBronze at the
# balanced tier — obsidian values, from MonumentalColor.kt)
CYAN = (0, 229, 255)
AMBER = (255, 176, 59)
REDBRONZE = (222, 122, 94)


def load_layers():
    out = {}
    for name in PLACE:
        img = Image.open(os.path.join(RES, f"tower414_{name}.webp")).convert("RGBA")
        out[name] = img.resize((STAGE_W, STAGE_H) if name in ("architecture", "energy_mask")
                               else img.size, Image.LANCZOS)
    return out


LAYERS = load_layers()


def fit_paste(canvas, img, xF, yF, wF, hF, rotation=0.0):
    W, H = canvas.size
    bw, bh = int(W * wF), int(H * hF)
    lw, lh = img.size
    s = min(bw / lw, bh / lh)
    nw, nh = max(1, int(lw * s)), max(1, int(lh * s))
    scaled = img.resize((nw, nh), Image.LANCZOS)
    if rotation:
        scaled = scaled.rotate(rotation, resample=Image.BICUBIC, expand=False)
    x0 = int(W * xF) + (bw - nw) // 2
    y0 = int(H * yF) + (bh - nh) // 2
    canvas.alpha_composite(scaled, (max(0, x0), max(0, y0)))


def base_frame(shimmer, breath):
    """Raster stack + ring rotation shimmer (§39 IDLE base)."""
    c = Image.new("RGBA", (STAGE_W, STAGE_H), (0, 0, 0, 0))
    # local dark halo behind the portal (§18: alpha 0.42)
    halo = Image.new("RGBA", (STAGE_W, STAGE_H), (0, 0, 0, 0))
    hd = ImageDraw.Draw(halo)
    cx, cy, r = int(W2 := STAGE_W * 0.5), int(STAGE_H * 0.78), int(STAGE_W * 0.62)
    hd.ellipse([cx - r, cy - r, cx + r, cy + r], fill=(0, 0, 0, int(255 * 0.42)))
    halo = halo.filter(__import__("PIL.ImageFilter", fromlist=["GaussianBlur"]).GaussianBlur(48))
    c.alpha_composite(halo)
    fit_paste(c, LAYERS["architecture"], 0, 0, 1, 1)
    fit_paste(c, LAYERS["ring_outer"], *PLACE["ring_outer"], rotation=shimmer * 8)
    fit_paste(c, LAYERS["ring_middle"], *PLACE["ring_middle"], rotation=-shimmer * 14)
    fit_paste(c, LAYERS["ring_inner"], *PLACE["ring_inner"], rotation=shimmer * 22)
    fit_paste(c, LAYERS["portal"], *PLACE["portal"])
    fit_paste(c, LAYERS["crown_sign"], *PLACE["crown_sign"])
    return c


def glow(canvas, cx_f, cy_f, radius_f, color, alpha):
    if alpha <= 0:
        return
    overlay = Image.new("RGBA", canvas.size, (0, 0, 0, 0))
    d = ImageDraw.Draw(overlay)
    cx, cy = canvas.size[0] * cx_f, canvas.size[1] * cy_f
    r = canvas.size[0] * radius_f
    steps = 22
    for i in range(steps, 0, -1):
        a = int(alpha * 255 * (1 - i / steps) ** 1.5)
        ri = r * (0.35 + 0.65 * i / steps)
        d.ellipse([cx - ri, cy - ri, cx + ri, cy + ri], fill=(*color, a))
    from PIL import ImageFilter
    overlay = overlay.filter(ImageFilter.GaussianBlur(int(canvas.size[0] * 0.02)))
    canvas.alpha_composite(overlay)


def draw_lines(canvas, pulse, color):
    d = ImageDraw.Draw(canvas)
    w, h = canvas.size
    for i in range(5):
        ph = (pulse + i / 5) % 1
        y = h * (0.18 + 0.24 * ph)
        halfw = w * (0.10 + 0.05 * ph)
        a = int(0.55 * pulse * 255)
        d.line([(w * 0.5 - halfw, y), (w * 0.5 + halfw, y)], fill=(*color, a), width=2)


def draw_ticks(canvas, pulse, color):
    d = ImageDraw.Draw(canvas)
    w, h = canvas.size
    tick = int(pulse * 12) % 12
    for i in range(12):
        ang = i * math.pi * 2 / 12
        cx = w * 0.5 + w * 0.30 * math.cos(ang)
        cy = h * 0.455 + w * 0.30 * math.sin(ang)
        a = int((0.9 if i == tick else 0.25) * 255)
        r = w * 0.012
        d.ellipse([cx - r, cy - r, cx + r, cy + r], fill=(*color, a))


def draw_arc(canvas, shimmer, color):
    d = ImageDraw.Draw(canvas)
    w, h = canvas.size
    sweep = (shimmer * 300) % 360
    d.arc([w * 0.20, h * 0.30, w * 0.20 + w * 0.60, h * 0.30 + w * 0.60],
          start=-90, end=-90 + sweep, fill=(*color, int(0.5 * 255)), width=3)


def draw_seal(canvas, color):
    d = ImageDraw.Draw(canvas)
    w, h = canvas.size
    a = int(0.85 * 255)
    for x0, y0, x1, y1 in ((0.30, 0.42, 0.70, 0.49), (0.70, 0.42, 0.30, 0.49)):
        d.line([(w * x0, h * y0), (w * x1, h * y1)], fill=(*color, a), width=5)


def tint_energy(canvas, color, alpha):
    e = LAYERS["energy_mask"].copy()
    r, g, b = e.split()[0], e.split()[1], e.split()[2]
    solid = Image.new("RGBA", e.size, (*color, 255))
    tinted = Image.composite(solid, Image.new("RGBA", e.size, (0, 0, 0, 0)),
                             e.split()[3].point(lambda v: int(v * alpha)))
    canvas.alpha_composite(tinted)


def render(state, t01):
    """Replicate the shipped equations; t01 in [0,1) — phase within the loop."""
    shimmer = t01
    breath = 0.72 + (1 - 0.72) * (0.5 - 0.5 * math.cos(2 * math.pi * (t01 * 90 / 2.4 % 1)))
    pulse = 0.5 - 0.5 * math.cos(2 * math.pi * (t01 * 90 / 1.1 % 1))
    mock = 0.35 + (0.8 - 0.35) * (0.5 - 0.5 * math.cos(2 * math.pi * (t01 * 90 / 2.6 % 1)))
    c = base_frame(shimmer, breath)
    S = state
    portal_alpha = 0.4 * breath
    energy_alpha = 0.45
    color = CYAN
    if S == "IDLE":
        portal_alpha = breath * 0.4
    elif S == "LISTENING":
        portal_alpha = 0.55 + 0.3 * pulse
        draw_lines(c, pulse, CYAN)
    elif S == "TRANSCRIBING":
        portal_alpha = 0.5 + 0.25 * pulse
        draw_ticks(c, pulse, CYAN)
    elif S == "LOCAL_PROCESSING":
        portal_alpha = 0.4 * breath
        energy_alpha = 0.7
    elif S == "CONTACTING_TOWER":
        portal_alpha = 0.4 * breath
        draw_arc(c, shimmer, CYAN)
    elif S == "SPEAKING":
        amp = 0.5 + 0.5 * math.sin(2 * math.pi * t01 * 3)
        portal_alpha = 0.45 + 0.55 * amp
        color = CYAN
    elif S == "CONTINUOUS_LISTENING":
        portal_alpha = 0.55 + 0.3 * pulse
        draw_lines(c, pulse, CYAN)
    elif S == "PRIVACY_MUTED":
        portal_alpha = 0.25 * breath
        energy_alpha = 0.10
        draw_seal(c, REDBRONZE)
    elif S == "CHAT_SPEECH_MUTED":
        portal_alpha = 0.45 + 0.55 * 0.5
        energy_alpha = 0.12
    elif S == "MOCK_MODE":
        portal_alpha = mock
        color = AMBER
        energy_alpha = mock * 0.4
    elif S in ("DISCONNECTED", "ERROR"):
        portal_alpha = 0.4 * breath
        energy_alpha = 0.45
        color = (128, 128, 128) if S == "DISCONNECTED" else (200, 60, 40)
    glow(c, 0.5, 0.815, 0.16 + portal_alpha * 0.10, color, portal_alpha)
    glow(c, 0.5, 0.455, 0.33, color, energy_alpha * 0.5)
    tint_energy(c, color, energy_alpha)
    return c


STATES = [
    ("IDLE", "slow ring shimmer + portal breathing (visible but subtle)"),
    ("LISTENING", "5 incoming lines + stronger portal pulse — clearly distinct from IDLE"),
    ("TRANSCRIBING", "12 sequential illumination ticks on the mechanism ring"),
    ("LOCAL_PROCESSING", "inner mechanism energy at 0.7 alpha, no remote conduit"),
    ("CONTACTING_TOWER", "outer path arc sweep — cyan, real-transport only"),
    ("SPEAKING", "portal responds visibly to the TTS amplitude"),
    ("CONTINUOUS_LISTENING", "persistent listening pulse (same family as LISTENING, sustained)"),
    ("PRIVACY_MUTED", "red-bronze seal over the input path, energy dimmed"),
    ("CHAT_SPEECH_MUTED", "output path dims (energy tint 0.12)"),
    ("MOCK_MODE", "AMBER simulation pulse 0.35→0.80 — never cyan (§40)"),
    ("DISCONNECTED", "no dedicated tower overlay — truth carried by the gray bridge indicator (§40 coherence)"),
    ("ERROR", "no dedicated tower overlay — truth carried by the red bridge indicator (§40 coherence)"),
]

FRAMES = [0.02, 0.45, 0.85]  # initial / mid / later within the loop
evidence = {"method": "offline replication of ReferenceTowerVisual.kt shipped equations over shipped raster layers (no emulator in sandbox; physical visibility = Owner retest PENDING)",
            "stagePx": [STAGE_W, STAGE_H], "states": {}}
for name, expected in STATES:
    frames = [render(name, t) for t in FRAMES]
    strip = Image.new("RGBA", (STAGE_W * 3 + 24 * 4, STAGE_H + 56), (24, 26, 30, 255))
    d = ImageDraw.Draw(strip)
    d.text((14, 8), f"{name} — initial / mid / later", fill=(235, 226, 205))
    for i, f in enumerate(frames):
        cell_bg = Image.new("RGBA", (STAGE_W, STAGE_H), (10, 11, 14, 255))
        cell_bg.alpha_composite(f)
        strip.alpha_composite(cell_bg, (24 + i * (STAGE_W + 24), 40))
    strip.convert("RGB").save(os.path.join(OUT, f"{name}.png"))
    evidence["states"][name] = {
        "expected": expected,
        "frames": [f"{name}.png#f{i+1}" for i in range(3)],
        "frameTimesLoopFraction": FRAMES,
        "authority": ("ConsoleState + BridgeModeAuthority + privacy/continuous/chat flags (typed inputs of ReferenceTowerVisual)"),
    }
    print(name, "ok")

with open(os.path.join(OUT, "ANIMATION_EVIDENCE.json"), "w") as fh:
    json.dump(evidence, fh, indent=1)
print("strips:", len(STATES), "->", OUT)
