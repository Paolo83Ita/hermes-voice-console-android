#!/usr/bin/env python3
"""
GRAPH 4.13 §30–§37 — MONUMENTAL TOWER REFOUNDATION asset pipeline.

The Graph 4.12 Tower is OWNER-REJECTED (CENTRAL_TOWER_GRAPH412_VISUAL_OWNER_REJECTED):
clean but flat, schematic, thin disconnected columns, detached sign, small
oracle core, large empty upper area. This generator DRAWS THREE GENUINELY
DISTINCT original candidates (§34) on ONE normalized stage (1000×1375), each
in the §36 layer contract (13 layers), at three resolution tiers:

  LOW      stage 460×632,  mech box 325×325 → decoded ≈ 10.8 MB  (≤12 MB)
  BALANCED stage 680×935,  mech box 480×480 → decoded ≈ 24 MB    (≤24 MB)
  HIGH     stage 1000×1375, mech box 707×707 (optional, never default)

Mechanism layers are cropped to the oracle box (their stage bounds are
recorded in the manifest); static layers are stage-sized. Architecture and
composition are IDENTICAL across tiers — only resolution differs (§40).

Identity per candidate:
  A  MONUMENTAL ORACLE CITADEL — closest to the Owner reference: heavy
     architecture, solid massed body, large oracle mechanism (42% width),
     broad flaring stairs, integrated crown sign.
  B  MECHANICAL TOWER CORE — deeper nested gear structures, octagonal body,
     exposed connected struts, stronger cyan; strongest animation capability.
  C  MOBILE MONUMENTAL — same monumental silhouette, bold simplified details,
     dominant core, reduced layer detail; strongest small-screen readability.

Forbidden everywhere (§27/§33): lateral model towers, provider labels,
arrows, messengers, audit panels, poster captions, sky background.
"""
from PIL import Image, ImageDraw, ImageFilter, ImageFont
import json, math, os

STAGE_W, STAGE_H = 1000, 1375
CX = STAGE_W // 2

# Layers per §36 contract, in z-order.
LAYERS = [
    "tower_architecture_rear",
    "tower_plinth",
    "tower_stairs",
    "tower_lower_gate",
    "tower_heavy_pylons",
    "tower_entablature",
    "tower_crown_sign",
    "tower_oracle_outer",
    "tower_oracle_middle",
    "tower_oracle_inner",
    "tower_portal",
    "tower_energy",
    "tower_state_overlay",
]
MECH_LAYERS = {"tower_oracle_outer", "tower_oracle_middle", "tower_oracle_inner", "tower_portal", "tower_energy"}

# ---- palettes (OLED-safe: no pure white; cyan controlled) ----
P_A = dict(
    bodyTop=(24, 27, 34, 255), bodyBot=(10, 12, 17, 255),
    panelTop=(30, 34, 42, 255), panelBot=(16, 19, 26, 255),
    bronzeDeep=(52, 39, 20, 255), bronze=(96, 72, 35, 255),
    bronzeEdge=(133, 100, 52, 255), bronzeLight=(178, 133, 69, 255),
    gold=(214, 176, 104, 255), goldBright=(238, 214, 156, 255),
    stoneDeep=(26, 22, 16, 255), stone=(38, 33, 24, 255),
    cyanCore=(53, 224, 255, 255), cyan=(31, 194, 232, 255), cyanDeep=(14, 126, 156, 255),
    shadow=(5, 6, 9, 255),
)
P_B = dict(P_A, bronzeEdge=(150, 108, 48, 255), bronzeLight=(196, 142, 66, 255),
           cyanCore=(70, 230, 255, 255), cyan=(40, 205, 240, 255))
P_C = dict(P_A, bronzeEdge=(146, 110, 58, 255), bronzeLight=(190, 145, 78, 255))

FONT_CANDIDATES = [
    "/usr/share/fonts/truetype/dejavu/DejaVuSerif-Bold.ttf",
    "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf",
]

def load_font(size):
    for f in FONT_CANDIDATES:
        if os.path.exists(f):
            return ImageFont.truetype(f, size)
    return ImageFont.load_default()

def new_layer():
    img = Image.new("RGBA", (STAGE_W, STAGE_H), (0, 0, 0, 0))
    return img, ImageDraw.Draw(img)

def mirror_poly(d, pts):
    d.polygon(pts, fill=None)

def sym_poly(d, pts, fill, outline=None, width=1):
    d.polygon(pts, fill=fill, outline=outline, width=width)
    d.polygon([(STAGE_W - x, y) for (x, y) in pts], fill=fill, outline=outline, width=width)

def sym_rect(d, x0, y0, x1, y1, fill, outline=None, width=1):
    d.rectangle([x0, y0, x1, y1], fill=fill, outline=outline, width=width)
    d.rectangle([STAGE_W - x1, y0, STAGE_W - x0, y1], fill=fill, outline=outline, width=width)

def vgrad_band(img, d, x0, y0, x1, y1, c_top, c_bot, mirror=True):
    """Vertical-gradient filled band (the core mass material)."""
    h = max(1, y1 - y0)
    band = Image.new("RGBA", (max(1, x1 - x0), h), (0, 0, 0, 0))
    bd = ImageDraw.Draw(band)
    for y in range(h):
        t = y / max(1, h - 1)
        bd.line([(0, y), (band.width, y)], fill=tuple(int(c_top[i] + (c_bot[i] - c_top[i]) * t) for i in range(4)))
    img.alpha_composite(band, (min(x0, x1), y0))
    if mirror:
        img.alpha_composite(band, (STAGE_W - max(x0, x1), y0))

def ring(d, cx, cy, r_out, r_in, fill, outline=None, width=2):
    d.ellipse([cx - r_out, cy - r_out, cx + r_out, cy + r_out], fill=fill)
    d.ellipse([cx - r_in, cy - r_in, cx + r_in, cy + r_in], fill=(0, 0, 0, 0), outline=outline, width=width)

def teeth(d, cx, cy, r_base, n, size, fill, phase=0.0):
    for i in range(n):
        a = phase + (2 * math.pi * i) / n
        x1 = cx + (r_base) * math.cos(a); y1 = cy + (r_base) * math.sin(a)
        x2 = cx + (r_base + size) * math.cos(a); y2 = cy + (r_base + size) * math.sin(a)
        d.line([(x1, y1), (x2, y2)], fill=fill, width=max(2, size // 2))

def dentils(d, y0, y1, x0, x1, step, fill, mirror=True):
    x = x0
    while x < x1:
        d.rectangle([x, y0, x + step * 0.45, y1], fill=fill)
        if mirror:
            d.rectangle([STAGE_W - x - step * 0.45, y0, STAGE_W - x, y1], fill=fill)
        x += step

def core_center():
    return (CX, 640)

CORE_R = 210          # candidate A: 420px diameter = 42% of stage width
MECH_BOX = CORE_R * 2 + 56   # 476 → rounded 480 box with glow margin

# =====================================================================
# CANDIDATE A — MONUMENTAL ORACLE CITADEL
# =====================================================================

def draw_a():
    P = P_A
    layers = {name: new_layer() for name in LAYERS}

    # ---- 1. architecture_rear: central body mass + recessed panels ----
    img, d = layers["tower_architecture_rear"]
    # solid central body (the mass the Owner demanded — no scaffold).
    # Ends at the gate cornice (y≈1000): the gateway reads as its OWN structure.
    vgrad_band(img, d, CX - 240, 260, CX + 240, 1002, P["panelTop"], P["panelBot"])
    # recessed vertical panels (engraved depth)
    for px0 in (CX - 200, CX - 120, CX + 40, CX + 120):
        sym_rect(d, px0, 300, px0 + 60, 950, fill=(0, 0, 0, 0), outline=P["bronzeDeep"], width=3)
    # bronze pilasters framing the body
    sym_rect(d, CX - 244, 260, CX - 212, 1002, fill=P["bronzeDeep"])
    sym_rect(d, CX - 244, 260, CX - 240, 1002, fill=P["bronzeEdge"])  # lit edge
    # rear atmosphere: deep panel behind the whole stage (no sky, pure dark)
    sym_rect(d, CX - 500, 260, CX - 240, 1290, fill=(6, 7, 10, 255))

    # ---- 2. plinth: three-tier foundation, full and connected ----
    img, d = layers["tower_plinth"]
    sym_rect(d, CX - 440, 1326, CX - 60, 1362, fill=P["stoneDeep"], outline=P["bronzeDeep"], width=2)
    sym_rect(d, CX - 380, 1290, CX - 40, 1328, fill=P["stone"], outline=P["bronzeDeep"], width=2)
    sym_rect(d, CX - 330, 1262, CX - 30, 1292, fill=P["stoneDeep"], outline=P["bronzeEdge"], width=2)
    # bronze lit top edge of every tier
    sym_rect(d, CX - 330, 1262, CX - 30, 1266, fill=P["bronzeEdge"])
    sym_rect(d, CX - 380, 1290, CX - 40, 1294, fill=P["bronze"])

    # ---- 3. stairs: broad flaring flight (12 lit risers) ----
    img, d = layers["tower_stairs"]
    steps = 12
    top_w, bot_w = 150, 285
    top_y, bot_y = 1198, 1268
    for i in range(steps):
        t0 = i / steps; t1 = (i + 1) / steps
        w0 = top_w + (bot_w - top_w) * t0
        w1 = top_w + (bot_w - top_w) * t1
        y0 = top_y + (bot_y - top_y) * t0
        y1 = top_y + (bot_y - top_y) * t1
        fill = P["stone"] if i % 2 == 0 else P["stoneDeep"]
        sym_poly(d, [(CX - w0, y0), (CX + w0, y0), (CX + w1, y1), (CX - w1, y1)],
                 fill=fill, outline=P["bronzeDeep"], width=1)
        # lit riser edge
        sym_rect(d, CX - w0, y0, CX + w0, y0 + 2, fill=P["bronzeEdge"])

    # ---- 4. lower_gate: arched gateway with its own order ----
    img, d = layers["tower_lower_gate"]
    # horizontal cornice connecting pylons above the gate
    sym_rect(d, CX - 260, 960, CX - 60, 996, fill=P["bronze"], outline=P["bronzeEdge"], width=2)
    sym_rect(d, CX - 260, 960, CX - 60, 964, fill=P["bronzeLight"])
    # gate columns
    for gx in (CX - 240, CX - 130):
        sym_rect(d, gx, 996, gx + 34, 1240, fill=P["bronzeDeep"])
        sym_rect(d, gx, 996, gx + 4, 1240, fill=P["bronze"])
        sym_rect(d, gx - 6, 996, gx + 40, 1012, fill=P["bronze"])   # capital
        sym_rect(d, gx - 6, 1224, gx + 40, 1240, fill=P["bronze"])  # base
    # arch head + opening (the portal glow is layer 11; here the arch stone)
    d.polygon([(CX - 130, 1080), (CX + 130, 1080), (CX + 130, 1240), (CX - 130, 1240)], fill=P["shadow"])
    d.arc([CX - 130, 1000, CX + 130, 1160], start=180, end=360, fill=P["bronzeEdge"], width=10)
    d.arc([CX - 110, 1012, CX + 110, 1148], start=180, end=360, fill=P["bronze"], width=4)
    d.rectangle([CX - 130, 1080, CX + 130, 1240], outline=P["bronzeDeep"], width=6)
    # keystone
    sym_poly(d, [(CX - 26, 996), (CX + 26, 996), (CX + 18, 1040), (CX - 18, 1040)], fill=P["bronzeEdge"])
    # "VERITAS·LOGOS·RATIO"-style engraved band (original glyph ticks, non-lexical)
    for i in range(7):
        tx = CX - 108 + i * 36
        d.rectangle([tx, 1052, tx + 18, 1058], fill=P["bronzeLight"])

    # ---- 5. heavy_pylons: SOLID flanking masses with capitals/bases ----
    img, d = layers["tower_heavy_pylons"]
    sym_rect(d, 60, 260, 250, 1270, fill=(0, 0, 0, 0))  # keep transparent outside
    vgrad_band(img, d, 70, 320, 240, 1200, P["bodyTop"], P["bodyBot"])
    # pylon capital (connected to entablature above)
    sym_rect(d, 56, 262, 254, 330, fill=P["bronzeDeep"], outline=P["bronzeEdge"], width=3)
    sym_rect(d, 56, 262, 254, 274, fill=P["bronzeLight"])
    # pylon base (connected to foundation below)
    sym_rect(d, 56, 1200, 254, 1272, fill=P["stone"], outline=P["bronzeEdge"], width=3)
    sym_rect(d, 56, 1200, 254, 1214, fill=P["bronze"])
    # carved panel insets (depth, not flatness)
    for py in (380, 560, 740, 920):
        sym_rect(d, 100, py, 210, py + 130, fill=(0, 0, 0, 0), outline=P["bronze"], width=4)
        sym_rect(d, 106, py + 6, 204, py + 124, fill=(13, 17, 25, 255))
    # bronze corner edges (lit left edge only — dawn-side light)
    sym_rect(d, 70, 330, 78, 1200, fill=P["bronze"])
    # carved bronze groove on the pylon inner face (NO loose cyan neon —
    # cyan belongs to the portal, spine and gate only)
    sym_rect(d, 226, 340, 232, 1190, fill=P["bronzeDeep"])
    sym_rect(d, 228, 340, 230, 1190, fill=P["bronze"])

    # ---- 6. entablature: HEAVY stacked cornice filling the upper zone ----
    img, d = layers["tower_entablature"]
    sym_rect(d, CX - 430, 150, CX - 40, 196, fill=P["bronze"], outline=P["bronzeEdge"], width=2)
    dentils(d, 196, 212, CX - 420, CX - 60, 34, P["bronzeLight"])
    sym_rect(d, CX - 430, 212, CX - 40, 258, fill=P["bronzeDeep"], outline=P["bronzeEdge"], width=2)
    # engraved frieze with meander ticks (original, non-lexical)
    sym_rect(d, CX - 380, 222, CX - 60, 250, fill=(13, 17, 25, 255))
    for i in range(12):
        tx = CX - 370 + i * 62
        d.rectangle([tx, 226, tx + 30, 246], outline=P["bronzeLight"], width=2)
        d.rectangle([tx + 8, 231, tx + 22, 241], fill=P["bronze"])
    # lit top edge
    sym_rect(d, CX - 430, 150, CX - 40, 156, fill=P["bronzeLight"])

    # ---- 7. crown_sign: integrated aedicula (NOT floating) ----
    img, d = layers["tower_crown_sign"]
    # connecting brackets from the sign base INTO the entablature
    sym_poly(d, [(CX - 200, 138), (CX - 150, 138), (CX - 120, 156), (CX - 230, 156)], fill=P["bronze"])
    # attic block
    sym_rect(d, CX - 260, 28, CX - 40, 142, fill=P["bronzeDeep"], outline=P["bronzeEdge"], width=3)
    sym_rect(d, CX - 268, 20, CX - 32, 40, fill=P["bronze"], outline=P["bronzeLight"], width=2)
    sym_rect(d, CX - 268, 130, CX - 32, 142, fill=P["bronze"])
    # finial crown
    sym_poly(d, [(CX - 40, 20), (CX + 40, 20), (CX, -2)], fill=P["gold"])
    # engraved HERMES TOWER text (gold, tracked, inside the plaque)
    f = load_font(56)
    txt = "HERMES TOWER"
    tw = d.textlength(txt, font=f)
    tx, ty = CX - tw / 2, 56
    d.text((tx + 2, ty + 2), txt, font=f, fill=(0, 0, 0, 180))
    d.text((tx, ty), txt, font=f, fill=P["gold"])
    # side rosettes
    for sx in (CX - 300, CX + 300):
        d.ellipse([sx - 16, 70, sx + 16, 102], fill=P["bronze"], outline=P["bronzeLight"], width=2)
        d.ellipse([sx - 6, 80, sx + 6, 92], fill=P["gold"])

    # ---- 8-10. oracle mechanism (42% width; deep nested rings) ----
    cx, cy = core_center()
    img, d = layers["tower_oracle_outer"]
    ring(d, cx, cy, CORE_R, CORE_R - 34, P["bronzeDeep"], outline=P["bronzeEdge"], width=4)
    ring(d, cx, cy, CORE_R - 34, CORE_R - 40, P["bronze"], outline=None)
    teeth(d, cx, cy, CORE_R - 46, 48, 10, P["bronzeEdge"])           # engraved outer teeth
    ring(d, cx, cy, CORE_R - 52, CORE_R - 60, P["bronze"], outline=P["bronzeLight"], width=2)

    img, d = layers["tower_oracle_middle"]
    ring(d, cx, cy, 150, 118, P["bronze"], outline=P["bronzeEdge"], width=3)
    teeth(d, cx, cy, 122, 24, 16, P["bronzeLight"])
    ring(d, cx, cy, 118, 104, P["bronzeDeep"], outline=P["bronzeEdge"], width=2)

    img, d = layers["tower_oracle_inner"]
    ring(d, cx, cy, 104, 90, P["bronze"], outline=P["gold"], width=3)
    for i in range(12):
        a = math.pi * 2 * i / 12
        x1 = cx + 92 * math.cos(a); y1 = cy + 92 * math.sin(a)
        x2 = cx + 74 * math.cos(a); y2 = cy + 74 * math.sin(a)
        d.line([(x1, y1), (x2, y2)], fill=P["goldBright"], width=3)

    # ---- 11. portal: cyan glass + geometric sigil (static, breathing) ----
    img, d = layers["tower_portal"]
    ring(d, cx, cy, 88, 62, P["cyanDeep"])
    ring(d, cx, cy, 62, 40, P["cyan"])
    d.ellipse([cx - 40, cy - 40, cx + 40, cy + 40], fill=P["cyanCore"])
    # sigil: circle + inner triangle (original geometry, reference-inspired)
    d.ellipse([cx - 30, cy - 30, cx + 30, cy + 30], outline=(6, 40, 52, 255), width=4)
    d.polygon([(cx, cy - 22), (cx - 19, cy + 12), (cx + 19, cy + 12)], outline=(6, 40, 52, 255), width=3)

    # ---- 12. energy: vertical spine core→gate→stairs ----
    img, d = layers["tower_energy"]
    # soft cyan interior glow inside the gate arch (reference-faithful depth)
    glow = Image.new("RGBA", (STAGE_W, STAGE_H), (0, 0, 0, 0))
    gd = ImageDraw.Draw(glow)
    gd.ellipse([CX - 96, 1052, CX + 96, 1216], fill=(20, 140, 175, 70))
    glow = glow.filter(ImageFilter.GaussianBlur(10))
    img.alpha_composite(glow)
    d.rectangle([cx - 6, cy + 88, cx + 6, 1160], fill=P["cyan"])
    d.rectangle([cx - 2, cy + 88, cx + 2, 1160], fill=P["cyanCore"])
    glow = Image.new("RGBA", (STAGE_W, STAGE_H), (0, 0, 0, 0))
    gd = ImageDraw.Draw(glow)
    gd.rectangle([cx - 14, cy + 88, cx + 14, 1160], fill=(31, 194, 232, 60))
    glow = glow.filter(ImageFilter.GaussianBlur(6))
    img.alpha_composite(glow)

    # ---- 13. state_overlay: empty by default (Canvas dynamics own the states) ----
    img, d = layers["tower_state_overlay"]
    pass

    return layers

# =====================================================================
# CANDIDATE B — MECHANICAL TOWER CORE
# =====================================================================

def draw_b():
    P = P_B
    layers = {name: new_layer() for name in LAYERS}
    cx, cy = core_center()

    # rear: octagonal mechanical body
    img, d = layers["tower_architecture_rear"]
    oct_pts = [(CX - 250, 330), (CX - 180, 260), (CX + 180, 260), (CX + 250, 330),
               (CX + 250, 950), (CX + 180, 1002), (CX - 180, 1002), (CX - 250, 950)]
    d.polygon(oct_pts, fill=P["panelBot"])
    d.polygon([(STAGE_W - x, y) for (x, y) in oct_pts], fill=P["panelBot"])
    for py in (400, 620, 840):
        sym_rect(d, CX - 210, py, CX + 210, py + 150, fill=(11, 14, 20, 255), outline=P["bronzeDeep"], width=4)
    # body mass ends at the gate cornice

    sym_rect(d, CX - 250, 260, CX - 238, 1002, fill=P["bronzeEdge"])
    # exposed struts MERGED into the pylon mass (connected, not doubled)
    sym_rect(d, 120, 260, 180, 1270, fill=P["bronzeDeep"])
    sym_rect(d, 120, 260, 132, 1270, fill=P["bronze"])

    img, d = layers["tower_plinth"]
    sym_rect(d, CX - 450, 1326, CX - 50, 1364, fill=P["stoneDeep"], outline=P["bronze"], width=2)
    sym_rect(d, CX - 390, 1290, CX - 30, 1328, fill=P["stone"], outline=P["bronzeEdge"], width=2)
    sym_rect(d, CX - 330, 1258, CX - 20, 1292, fill=P["stoneDeep"], outline=P["bronze"], width=2)
    for i in range(9):  # riveted foundation
        dentils(d, 1332, 1358, CX - 440, CX - 70, 90, P["bronze"])

    img, d = layers["tower_stairs"]
    steps = 9
    for i in range(steps):
        t0 = i / steps; t1 = (i + 1) / steps
        w0 = 150 + 90 * t0; w1 = 150 + 90 * t1
        y0 = 1216 + 70 * t0; y1 = 1216 + 70 * t1
        sym_poly(d, [(CX - w0, y0), (CX + w0, y0), (CX + w1, y1), (CX - w1, y1)],
                 fill=P["stone"] if i % 2 else P["stoneDeep"], outline=P["bronze"], width=1)
        sym_rect(d, CX - w0, y0, CX + w0, y0 + 2, fill=P["bronzeLight"])

    img, d = layers["tower_lower_gate"]
    sym_rect(d, CX - 280, 950, CX + 0, 986, fill=P["bronze"], outline=P["bronzeEdge"], width=2)
    for gx in (CX - 250, CX - 140):
        sym_rect(d, gx, 986, gx + 40, 1210, fill=P["bronzeDeep"])
        sym_rect(d, gx, 986, gx + 6, 1210, fill=P["bronze"])
        d.rectangle([gx + 8, 1020, gx + 32, 1044], outline=P["bronzeLight"], width=2)
    d.polygon([(CX - 140, 1100), (CX + 140, 1100), (CX + 140, 1210), (CX - 140, 1210)], fill=P["shadow"])
    d.arc([CX - 140, 1010, CX + 140, 1190], 180, 360, fill=P["bronzeEdge"], width=12)
    d.rectangle([CX - 140, 1100, CX + 140, 1210], outline=P["bronzeDeep"], width=6)

    img, d = layers["tower_heavy_pylons"]
    vgrad_band(img, d, 190, 320, 330, 1190, P["bodyTop"], P["bodyBot"])
    sym_rect(d, 176, 262, 344, 336, fill=P["bronzeDeep"], outline=P["bronzeEdge"], width=3)
    sym_rect(d, 176, 1196, 344, 1270, fill=P["stone"], outline=P["bronzeEdge"], width=3)
    for py in (400, 640, 880, 1060):
        sym_rect(d, 216, py, 304, py + 100, fill=(13, 17, 25, 255), outline=P["bronze"], width=3)
        d.ellipse([256, py + 40, 266, py + 50], fill=P["bronzeLight"])  # rivet
    sym_rect(d, 176, 262, 188, 1270, fill=P["bronze"])

    img, d = layers["tower_entablature"]
    sym_rect(d, CX - 450, 146, CX - 30, 200, fill=P["bronze"], outline=P["bronzeEdge"], width=2)
    dentils(d, 200, 218, CX - 440, CX - 50, 40, P["bronzeLight"])
    sym_rect(d, CX - 450, 218, CX - 30, 262, fill=P["bronzeDeep"], outline=P["bronzeEdge"], width=2)
    for i in range(9):
        tx = CX - 400 + i * 100
        d.ellipse([tx, 228, tx + 20, 248], outline=P["bronzeLight"], width=2)

    img, d = layers["tower_crown_sign"]
    sym_poly(d, [(CX - 190, 132), (CX - 130, 132), (CX - 100, 150), (CX - 220, 150)], fill=P["bronze"])
    sym_rect(d, CX - 270, 26, CX - 30, 138, fill=P["bronzeDeep"], outline=P["bronzeEdge"], width=3)
    sym_rect(d, CX - 278, 18, CX - 22, 38, fill=P["bronze"], outline=P["bronzeLight"], width=2)
    # mechanical riveted plaque
    for sx in (CX - 252, CX - 232, CX + 232, CX + 252):
        d.ellipse([sx - 5, 78, sx + 5, 88], fill=P["bronzeLight"])
    f = load_font(52)
    txt = "HERMES TOWER"
    tw = d.textlength(txt, font=f)
    d.text((CX - tw / 2 + 2, 58), txt, font=f, fill=(0, 0, 0, 180))
    d.text((CX - tw / 2, 56), txt, font=f, fill=P["gold"])
    # gear finial
    ring(d, CX, 10, 22, 12, P["gold"])
    teeth(d, CX, 10, 22, 8, 8, P["goldBright"])

    img, d = layers["tower_oracle_outer"]
    ring(d, cx, cy, 218, 184, P["bronzeDeep"], outline=P["bronzeEdge"], width=4)
    teeth(d, cx, cy, 186, 36, 22, P["bronzeLight"])
    ring(d, cx, cy, 184, 168, P["bronze"], outline=P["bronzeLight"], width=2)
    for i in range(4):  # connected strut spokes (mechanical identity)
        a = math.pi / 4 + i * math.pi / 2
        d.line([(cx + 170 * math.cos(a), cy + 170 * math.sin(a)),
                (cx + 120 * math.cos(a), cy + 120 * math.sin(a))], fill=P["bronzeEdge"], width=10)

    img, d = layers["tower_oracle_middle"]
    ring(d, cx, cy, 152, 116, P["bronze"], outline=P["bronzeEdge"], width=3)
    teeth(d, cx, cy, 118, 32, 18, P["bronzeLight"])
    ring(d, cx, cy, 116, 100, P["bronzeDeep"], outline=P["bronzeEdge"], width=2)

    img, d = layers["tower_oracle_inner"]
    ring(d, cx, cy, 100, 86, P["bronze"], outline=P["gold"], width=3)
    teeth(d, cx, cy, 88, 18, 10, P["goldBright"])

    img, d = layers["tower_portal"]
    ring(d, cx, cy, 86, 58, P["cyanDeep"])
    ring(d, cx, cy, 58, 36, P["cyan"])
    d.ellipse([cx - 36, cy - 36, cx + 36, cy + 36], fill=P["cyanCore"])
    d.ellipse([cx - 24, cy - 24, cx + 24, cy + 24], outline=(6, 40, 52, 255), width=4)
    d.polygon([(cx, cy - 18), (cx - 16, cy + 10), (cx + 16, cy + 10)], outline=(6, 40, 52, 255), width=3)

    img, d = layers["tower_energy"]
    glow = Image.new("RGBA", (STAGE_W, STAGE_H), (0, 0, 0, 0))
    gd = ImageDraw.Draw(glow)
    gd.ellipse([CX - 96, 1052, CX + 96, 1216], fill=(20, 140, 175, 70))
    glow = glow.filter(ImageFilter.GaussianBlur(10))
    img.alpha_composite(glow)
    d.rectangle([cx - 7, cy + 86, cx + 7, 1150], fill=P["cyan"])
    d.rectangle([cx - 3, cy + 86, cx + 3, 1150], fill=P["cyanCore"])

    return layers

# =====================================================================
# CANDIDATE C — MOBILE MONUMENTAL (bold, simplified, dominant core)
# =====================================================================

def draw_c():
    P = P_C
    layers = {name: new_layer() for name in LAYERS}
    cx, cy = (CX, 620)
    CR = 232  # dominant core for small screens

    img, d = layers["tower_architecture_rear"]
    vgrad_band(img, d, CX - 230, 250, CX + 230, 1002, P["panelTop"], P["panelBot"])
    sym_rect(d, CX - 234, 250, CX - 202, 1002, fill=P["bronzeDeep"])
    sym_rect(d, CX - 234, 250, CX - 228, 1002, fill=P["bronzeEdge"])

    img, d = layers["tower_plinth"]
    sym_rect(d, CX - 430, 1322, CX - 40, 1364, fill=P["stoneDeep"], outline=P["bronze"], width=2)
    sym_rect(d, CX - 340, 1272, CX - 20, 1324, fill=P["stone"], outline=P["bronzeEdge"], width=2)

    img, d = layers["tower_stairs"]
    for i in range(7):
        t0 = i / 7; t1 = (i + 1) / 7
        w0 = 160 + 130 * t0; w1 = 160 + 130 * t1
        y0 = 1188 + 70 * t0; y1 = 1188 + 70 * t1
        sym_poly(d, [(CX - w0, y0), (CX + w0, y0), (CX + w1, y1), (CX - w1, y1)],
                 fill=P["stone"] if i % 2 else P["stoneDeep"], outline=P["bronze"], width=1)

    img, d = layers["tower_lower_gate"]
    sym_rect(d, CX - 250, 950, CX - 60, 990, fill=P["bronze"], outline=P["bronzeEdge"], width=2)
    for gx in (CX - 230, CX - 130):
        sym_rect(d, gx, 990, gx + 40, 1200, fill=P["bronzeDeep"])
        sym_rect(d, gx, 990, gx + 6, 1200, fill=P["bronze"])
    d.polygon([(CX - 130, 1090), (CX + 130, 1090), (CX + 130, 1200), (CX - 130, 1200)], fill=P["shadow"])
    d.arc([CX - 130, 1000, CX + 130, 1180], 180, 360, fill=P["bronzeEdge"], width=14)
    d.rectangle([CX - 130, 1090, CX + 130, 1200], outline=P["bronzeDeep"], width=6)

    img, d = layers["tower_heavy_pylons"]
    vgrad_band(img, d, 80, 310, 240, 1190, P["bodyTop"], P["bodyBot"])
    sym_rect(d, 66, 250, 254, 330, fill=P["bronzeDeep"], outline=P["bronzeEdge"], width=3)
    sym_rect(d, 66, 1186, 254, 1262, fill=P["stone"], outline=P["bronzeEdge"], width=3)
    for py in (420, 660, 900):
        sym_rect(d, 110, py, 210, py + 140, fill=(13, 17, 25, 255), outline=P["bronze"], width=5)
    sym_rect(d, 80, 330, 92, 1186, fill=P["bronze"])

    img, d = layers["tower_entablature"]
    sym_rect(d, CX - 420, 140, CX - 30, 200, fill=P["bronze"], outline=P["bronzeEdge"], width=2)
    sym_rect(d, CX - 420, 200, CX - 30, 254, fill=P["bronzeDeep"], outline=P["bronzeEdge"], width=2)
    sym_rect(d, CX - 420, 140, CX - 30, 150, fill=P["bronzeLight"])
    for i in range(8):
        tx = CX - 360 + i * 96
        d.rectangle([tx, 212, tx + 56, 242], outline=P["bronzeLight"], width=3)

    img, d = layers["tower_crown_sign"]
    sym_poly(d, [(CX - 180, 128), (CX - 120, 128), (CX - 90, 144), (CX - 210, 144)], fill=P["bronze"])
    sym_rect(d, CX - 270, 20, CX - 30, 134, fill=P["bronzeDeep"], outline=P["bronzeEdge"], width=3)
    sym_rect(d, CX - 278, 12, CX - 22, 32, fill=P["bronze"], outline=P["bronzeLight"], width=2)
    f = load_font(58)
    txt = "HERMES TOWER"
    tw = d.textlength(txt, font=f)
    d.text((CX - tw / 2 + 2, 52), txt, font=f, fill=(0, 0, 0, 180))
    d.text((CX - tw / 2, 50), txt, font=f, fill=P["gold"])

    img, d = layers["tower_oracle_outer"]
    ring(d, cx, cy, CR, CR - 40, P["bronzeDeep"], outline=P["bronzeEdge"], width=5)
    teeth(d, cx, cy, CR - 46, 36, 12, P["bronzeEdge"])
    ring(d, cx, cy, CR - 58, CR - 66, P["bronze"], outline=P["bronzeLight"], width=3)

    img, d = layers["tower_oracle_middle"]
    ring(d, cx, cy, 156, 118, P["bronze"], outline=P["bronzeEdge"], width=4)
    teeth(d, cx, cy, 122, 20, 18, P["bronzeLight"])

    img, d = layers["tower_oracle_inner"]
    ring(d, cx, cy, 104, 88, P["bronze"], outline=P["gold"], width=4)
    for i in range(8):
        a = math.pi * 2 * i / 8
        x1 = cx + 92 * math.cos(a); y1 = cy + 92 * math.sin(a)
        x2 = cx + 72 * math.cos(a); y2 = cy + 72 * math.sin(a)
        d.line([(x1, y1), (x2, y2)], fill=P["goldBright"], width=4)

    img, d = layers["tower_portal"]
    ring(d, cx, cy, 88, 58, P["cyanDeep"])
    ring(d, cx, cy, 58, 34, P["cyan"])
    d.ellipse([cx - 34, cy - 34, cx + 34, cy + 34], fill=P["cyanCore"])
    d.ellipse([cx - 22, cy - 22, cx + 22, cy + 22], outline=(6, 40, 52, 255), width=4)

    img, d = layers["tower_energy"]
    glow = Image.new("RGBA", (STAGE_W, STAGE_H), (0, 0, 0, 0))
    gd = ImageDraw.Draw(glow)
    gd.ellipse([CX - 96, 1042, CX + 96, 1206], fill=(20, 140, 175, 70))
    glow = glow.filter(ImageFilter.GaussianBlur(10))
    img.alpha_composite(glow)
    d.rectangle([cx - 7, cy + 86, cx + 7, 1140], fill=P["cyan"])
    d.rectangle([cx - 3, cy + 86, cx + 3, 1140], fill=P["cyanCore"])

    return layers

# =====================================================================
# rendering: tiers + crop + previews
# =====================================================================

TIERS = {
    "low": (460, 460 * MECH_BOX // STAGE_W),
    "balanced": (680, 680 * MECH_BOX // STAGE_W),
    "high": (1000, 1000 * MECH_BOX // STAGE_W),
}

def crop_mech(img, tier_stage_w, mech_box):
    """Crop a FULL-STAGE mech layer to the mech box at tier scale.
    The input is always the 1000×1375 truth; resize to tier scale FIRST,
    then crop around the scaled core center."""
    cx, cy = core_center()
    sx = tier_stage_w / STAGE_W
    tier = img.resize((int(STAGE_W * sx), int(STAGE_H * sx)), Image.LANCZOS)
    box = int(mech_box)
    ccx, ccy = int(cx * sx), int(cy * sx)
    x0 = ccx - box // 2; y0 = ccy - box // 2
    out = Image.new("RGBA", (box, box), (0, 0, 0, 0))
    out.alpha_composite(tier, (-x0, -y0))
    return out

def render(candidate, outdir):
    os.makedirs(outdir, exist_ok=True)
    if candidate == "A":
        layers = draw_a()
    elif candidate == "B":
        layers = draw_b()
    else:
        layers = draw_c()
    sizes = {}
    for tier, (stage_w, mech_box) in TIERS.items():
        sx = stage_w / STAGE_W
        stage_h = int(STAGE_H * sx)
        tdir = os.path.join(outdir, tier)
        os.makedirs(tdir, exist_ok=True)
        for name, (img, _) in layers.items():
            if name in MECH_LAYERS:
                out = crop_mech(img, stage_w, mech_box)
            else:
                out = img.resize((stage_w, stage_h), Image.LANCZOS)
            fp = os.path.join(tdir, f"{name}.webp")
            out.save(fp, "WEBP", quality=88, method=6)
            sizes[f"{tier}/{name}"] = os.path.getsize(fp)
    return layers, sizes

def composite_preview(layers, path):
    """Full-tower composite for visual comparison (balanced truth)."""
    comp = Image.new("RGBA", (STAGE_W, STAGE_H), (6, 7, 10, 255))
    for name in LAYERS:
        if name in MECH_LAYERS:
            continue
        comp.alpha_composite(layers[name][0])
    for name in ["tower_oracle_outer", "tower_oracle_middle", "tower_oracle_inner", "tower_portal", "tower_energy"]:
        comp.alpha_composite(layers[name][0])
    comp.convert("RGB").save(path, "PNG")
    comp.thumbnail((500, 687))
    comp.save(path.replace(".png", "_small.png"), "PNG")

def main():
    root = "/home/z/my-project/hermes-voice-console-android/docs/visual/tower413"
    manifest = {"stage": [STAGE_W, STAGE_H], "layers": LAYERS,
                "mech_layers": sorted(MECH_LAYERS),
                "mech_box_stage": MECH_BOX,
                "core_center_stage": list(core_center()),
                "tiers": {k: {"stage_w": v[0], "mech_box": v[1]} for k, v in TIERS.items()},
                "candidates": {}}
    for cand in ("A", "B", "C"):
        outdir = os.path.join(root, f"candidate_{cand.lower()}")
        layers, sizes = render(cand, outdir)
        composite_preview(layers, os.path.join(outdir, "preview_composite.png"))
        center = (500, 620) if cand == "C" else tuple(core_center())
        manifest["candidates"][cand] = {"dir": outdir, "bytes": sizes, "core_center_stage": list(center)}
        print(f"candidate {cand}: {sum(sizes.values())} bytes total")
    # decoded-memory audit per tier (ARGB_8888)
    for tier, (stage_w, mech_box) in TIERS.items():
        stage_h = int(STAGE_H * stage_w / STAGE_W)
        static_mem = 8 * stage_w * stage_h * 4
        mech_mem = 5 * mech_box * mech_box * 4
        total = (static_mem + mech_mem) / (1024 * 1024)
        manifest["tiers"][tier]["decoded_mb"] = round(total, 1)
        print(f"tier {tier}: decoded ≈ {total:.1f} MB")
    with open(os.path.join(root, "ASSET_MANIFEST.json"), "w") as f:
        json.dump(manifest, f, indent=1)
    print("manifest written")

if __name__ == "__main__":
    main()
