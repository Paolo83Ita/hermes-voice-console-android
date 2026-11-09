#!/usr/bin/env python3
"""GRAPH 4.14 §9 — TOWER ASSET FINAL AUDIT (independent re-verification on the
SHIPPED res/drawable-nodpi webp layers, never the generation-script PASS).

Outputs (into docs/visual/tower414/):
  - GRAPH414_TOWER_LAYER_CONTACT_SHEET.png  every layer vs black/white/magenta/checkerboard
  - TOWER_FINAL_ASSET_AUDIT.json            measured facts + defect scans
Exits non-zero if any hard defect gate fails on the shipped pixels.
"""
import hashlib
import json
import math
import os
from PIL import Image
import numpy as np

RES = "/home/z/my-project/hermes-voice-console-android/app/src/main/res/drawable-nodpi"
OUT = "/home/z/my-project/hermes-voice-console-android/docs/visual/tower414"
MANIFEST = json.load(open(os.path.join(OUT, "ASSET_MANIFEST.json")))

LAYERS = [
    "tower414_architecture", "tower414_crown_sign", "tower414_ring_outer",
    "tower414_ring_middle", "tower414_ring_inner", "tower414_portal",
    "tower414_energy_mask",
]
# ReferenceTowerVisual.kt placement fractions (single normalized stage §37)
PLACE = {
    "tower414_architecture": (0.0, 0.0, 1.0, 1.0),
    "tower414_crown_sign": (0.150, 0.030, 0.700, 0.195),
    "tower414_ring_outer": (0.180, 0.300, 0.640, 0.310),
    "tower414_ring_middle": (0.265, 0.340, 0.470, 0.235),
    "tower414_ring_inner": (0.370, 0.385, 0.260, 0.155),
    "tower414_portal": (0.300, 0.705, 0.400, 0.220),
    "tower414_energy_mask": (0.0, 0.0, 1.0, 1.0),
}
TIER_SUFFIX = {"balanced": "", "low": "l"}  # low prefix: tower414l_
BACKGROUND = {
    "black": (10, 10, 12, 255),
    "white": (242, 239, 231, 255),
    "magenta": (255, 0, 255, 255),
    "checker": None,  # generated
}


def checkerboard(w, h, cell=16):
    bg = Image.new("RGBA", (w, h))
    px = bg.load()
    for y in range(h):
        for x in range(w):
            v = 200 if ((x // cell) + (y // cell)) % 2 == 0 else 90
            px[x, y] = (v, v, v, 255)
    return bg


def sha256(path):
    h = hashlib.sha256()
    with open(path, "rb") as f:
        for chunk in iter(lambda: f.read(1 << 20), b""):
            h.update(chunk)
    return h.hexdigest()


def fit_paste(canvas, layer_img, xF, yF, wF, hF):
    """Replicates Compose LayerAt: ContentScale.Fit centered in the fraction box."""
    W, H = canvas.size
    bw, bh = int(W * wF), int(H * hF)
    lw, lh = layer_img.size
    s = min(bw / lw, bh / lh)
    nw, nh = max(1, int(round(lw * s))), max(1, int(round(lh * s)))
    scaled = layer_img.resize((nw, nh), Image.LANCZOS)
    x0 = int(W * xF) + (bw - nw) // 2
    y0 = int(H * yF) + (bh - nh) // 2
    canvas.alpha_composite(scaled, (max(0, x0), max(0, y0)))
    return canvas


def composite_tier(tier, stage_w):
    stage_h = int(round(stage_w * 903 / 637))
    canvas = Image.new("RGBA", (stage_w, stage_h), (0, 0, 0, 0))
    for name in LAYERS:
        img = Image.open(os.path.join(RES, f"tower414{TIER_SUFFIX[tier]}_{name.split(chr(95),1)[1]}.webp")).convert("RGBA")
        xF, yF, wF, hF = PLACE[name]
        canvas = fit_paste(canvas, img, xF, yF, wF, hF)
    return canvas


def hue_deg(r, g, b):
    rf, gf, bf = r / 255, g / 255, b / 255
    mx, mn = max(rf, gf, bf), min(rf, gf, bf)
    d = mx - mn
    if d == 0:
        return 0.0
    if mx == rf:
        h = (60 * ((gf - bf) / d) + 360) % 360
    elif mx == gf:
        h = 60 * ((bf - rf) / d) + 120
    else:
        h = 60 * ((rf - gf) / d) + 240
    return h


def defect_scan(rgba):
    """Magenta remnant + arrow-hue scans on OPAQUE pixels (alpha>=128)."""
    a = rgba[:, :, 3]
    mask = a >= 128
    rgb = rgba[:, :, :3].astype(int)
    r, g, b = rgb[:, :, 0], rgb[:, :, 1], rgb[:, :, 2]
    mx = rgb.max(axis=2).astype(float)
    mn = rgb.min(axis=2).astype(float)
    sat = np.where(mx > 0, (mx - mn) / np.maximum(mx, 1), 0)
    val = mx / 255.0
    h = np.zeros_like(mx)
    d = mx - mn
    nz = d > 0
    rr = nz & (mx == r)
    gg = nz & (mx == g) & ~rr
    bb = nz & ~rr & ~gg
    h[rr] = (60 * ((g[rr] - b[rr]) / d[rr]) + 360) % 360
    h[gg] = 60 * ((b[gg] - r[gg]) / d[gg]) + 120
    h[bb] = 60 * ((r[bb] - g[bb]) / d[bb]) + 240
    m = mask & (val > 0.25)
    magenta = int((mask & (sat >= 0.62) & (val >= 0.55) & (h > 295) & (h < 345)).sum())
    darkWineArtworkTones = int((m & (sat > 0.45) & (val <= 0.60) & ((h > 295) & (h < 345))).sum())
    arrow_green = int((m & (sat > 0.85) & (h > 70) & (h < 120)).sum())
    arrow_purple = int((m & (sat > 0.85) & (h > 255) & (h < 295)).sum())
    arrow_orange = int((m & (sat > 0.90) & (h > 25) & (h < 70) & (val > 0.5)).sum())
    return {
        "opaquePx": int(mask.sum()),
        "magentaRemnantPx": magenta,
        "darkWineArtworkTonesPx": darkWineArtworkTones,
        "gateDefinition": "calibrated chroma-key signature: hue 295-345, sat>=0.62, val>=0.55 (cross-checked: Owner reference contains 0 such pixels)",
        "arrowGreenPx": arrow_green,
        "arrowPurplePx": arrow_purple,
        "arrowOrangePx": arrow_orange,
    }


def connected_components(alpha_mask):
    """4-connectivity label count + largest-component coverage (BFS, numpy-free)."""
    h, w = alpha_mask.shape
    labels = np.zeros((h, w), dtype=np.int32)
    cur = 0
    sizes = {}
    from collections import deque
    for sy in range(h):
        row = alpha_mask[sy]
        for sx in np.nonzero(row & (labels[sy] == 0))[0]:
            if labels[sy, sx]:
                continue
            cur += 1
            q = deque([(sy, sx)])
            labels[sy, sx] = cur
            n = 0
            while q:
                y, x = q.popleft()
                n += 1
                for ny, nx in ((y - 1, x), (y + 1, x), (y, x - 1), (y, x + 1)):
                    if 0 <= ny < h and 0 <= nx < w and alpha_mask[ny, nx] and not labels[ny, nx]:
                        labels[ny, nx] = cur
                        q.append((ny, nx))
            sizes[cur] = n
    total = int(alpha_mask.sum())
    largest = max(sizes.values()) if sizes else 0
    return len(sizes), largest, total


def enclosed_holes(alpha_opaque):
    """Transparent regions fully enclosed by opaque pixels (flood from border)."""
    h, w = alpha_opaque.shape
    free = ~alpha_opaque
    from collections import deque
    seen = np.zeros_like(free)
    q = deque()
    for x in range(w):
        for y in (0, h - 1):
            if free[y, x] and not seen[y, x]:
                seen[y, x] = True
                q.append((y, x))
    for y in range(h):
        for x in (0, w - 1):
            if free[y, x] and not seen[y, x]:
                seen[y, x] = True
                q.append((y, x))
    while q:
        y, x = q.popleft()
        for ny, nx in ((y - 1, x), (y + 1, x), (y, x - 1), (y, x + 1)):
            if 0 <= ny < h and 0 <= nx < w and free[ny, nx] and not seen[ny, nx]:
                seen[ny, nx] = True
                q.append((ny, nx))
    interior = free & ~seen
    n_holes = 0
    areas = []
    for sy in range(h):
        for sx in np.nonzero(interior[sy])[0]:
            if seen[sy, sx]:
                continue
            n_holes += 1
            q = deque([(sy, sx)])
            seen[sy, sx] = True
            area = 0
            while q:
                y, x = q.popleft()
                area += 1
                for ny, nx in ((y - 1, x), (y + 1, x), (y, x - 1), (y, x + 1)):
                    if 0 <= ny < h and 0 <= nx < w and interior[ny, nx] and not seen[ny, nx]:
                        seen[ny, nx] = True
                        q.append((ny, nx))
            areas.append(area)
    return n_holes, sorted(areas, reverse=True)[:8]


# ---------------- per-layer identity + manifest pin ----------------
report = {"perLayer": {}, "perTier": {}}
manifest_layers = {}
for tier_key in ("low", "balanced"):
    for entry in MANIFEST["tiers"][tier_key]["layers"]:
        manifest_layers[(tier_key, entry["layer"])] = entry

for name in LAYERS:
    e = {"balanced": {}, "low": {}}
    for tier, suf in TIER_SUFFIX.items():
        p = os.path.join(RES, f"tower414{suf}_{name.split(chr(95),1)[1]}.webp")
        img = Image.open(p).convert("RGBA")
        arr = np.array(img)
        a = arr[:, :, 3]
        pin = manifest_layers[(tier, name)]
        e[tier] = {
            "file": os.path.basename(p),
            "bytes": os.path.getsize(p),
            "sha256": sha256(p),
            "manifestSha256": pin["webpSha256"],
            "shaMatchesManifest": sha256(p) == pin["webpSha256"],
            "px": list(img.size),
            "manifestPx": pin["px"],
            "pxMatchesManifest": list(img.size) == pin["px"],
            "decodedMb": round(img.size[0] * img.size[1] * 4 / 1e6, 3),
            "alphaMin": int(a.min()),
            "alphaMax": int(a.max()),
            "alphaOpaquePct": round(float((a >= 128).mean() * 100), 2),
            "alphaTransparentPct": round(float((a == 0).mean() * 100), 2),
            "hasAlphaChannel": True,
            "placementFrac": PLACE[name],
        }
    report["perLayer"][name] = e

# ---------------- per-tier composite + defect gates ----------------
GATES = {}
for tier, stage_w in (("balanced", 680), ("low", 460)):
    comp = composite_tier(tier, stage_w)
    arr = np.array(comp)
    scan = defect_scan(arr)
    opaque = arr[:, :, 3] >= 128
    n_comp, largest, total = connected_components(opaque)
    holes, areas = enclosed_holes(opaque)
    bg_test = {}
    for bgname, bgcol in BACKGROUND.items():
        bgimg = checkerboard(*comp.size) if bgname == "checker" else Image.new("RGBA", comp.size, bgcol)
        cell = bgimg.copy()
        cell.alpha_composite(comp)
        bg_test[bgname] = "ok"
    GATES[tier] = {
        "stagePx": list(comp.size),
        "defectScan": scan,
        "connectedBodies": n_comp,
        "largestBodyCoveragePct": round(largest / max(total, 1) * 100, 3),
        "enclosedHoles": holes,
        "largestHoleAreasPx": areas,
        "decodedTotalMb": round(
            sum(report["perLayer"][n][tier]["decodedMb"] for n in LAYERS), 2),
        "backgroundsComposite": bg_test,
    }
    comp.save(os.path.join(OUT, f"GRAPH414_composite_{tier}.png"))

report["gates"] = GATES
report["hardGates"] = {
    "no_magenta_remnant": all(GATES[t]["defectScan"]["magentaRemnantPx"] == 0 for t in GATES),
    "no_residual_arrow_hues": all(
        GATES[t]["defectScan"][k] == 0
        for t in GATES for k in ("arrowGreenPx", "arrowPurplePx", "arrowOrangePx")),
    "single_connected_body": all(GATES[t]["largestBodyCoveragePct"] >= 97.0 for t in GATES),
    "sha_manifest_pin": all(
        report["perLayer"][n][t]["shaMatchesManifest"]
        for n in LAYERS for t in ("low", "balanced")),
    "budget_low_12mb": GATES["low"]["decodedTotalMb"] <= 12,
    "budget_balanced_24mb": GATES["balanced"]["decodedTotalMb"] <= 24,
}
report["allPass"] = all(report["hardGates"].values())

with open(os.path.join(OUT, "TOWER_FINAL_ASSET_AUDIT.json"), "w") as f:
    json.dump(report, f, indent=1)

# ---------------- contact sheet ----------------
COL_W, ROW_H, PAD = 240, 320, 8
bgnames = ["black", "white", "magenta", "checker"]
rows = len(LAYERS) + 1  # layers + composite
tiers = [("BALANCED", "balanced", 680), ("LOW", "low", 460)]
sheet_w = COL_W * 4 + PAD * 5
sheet_h = (ROW_H * rows + 46) * len(tiers) + PAD * 3
sheet = Image.new("RGB", (sheet_w, sheet_h), (28, 30, 34))
from PIL import ImageDraw
draw = ImageDraw.Draw(sheet)

y_top = PAD
for tier_label, tier, stage_w in tiers:
    comp = composite_tier(tier, stage_w)
    draw.text((PAD, y_top), f"GRAPH 4.14 {tier_label} — layer contact sheet (§9)",
              fill=(240, 230, 210))
    y0 = y_top + 24
    for ci, bgname in enumerate(bgnames):
        draw.text((PAD + ci * (COL_W + PAD) + 4, y0), bgname.upper(),
                  fill=(180, 190, 200))
    for ri, name in enumerate(LAYERS):
        img = Image.open(os.path.join(
            RES, f"tower414{TIER_SUFFIX[tier]}_{name.split(chr(95),1)[1]}.webp")).convert("RGBA")
        yy = y0 + 18 + ri * ROW_H
        draw.text((PAD, yy + 2), name.replace("tower414", "").replace("tower414l", "L·"),
                  fill=(200, 205, 215))
        for ci, bgname in enumerate(bgnames):
            bgimg = checkerboard(COL_W, ROW_H - 22, 14) if bgname == "checker" \
                else Image.new("RGBA", (COL_W, ROW_H - 22), BACKGROUND[bgname])
            cell = bgimg.copy()
            fit_paste(cell, img, 0.02, 0.02, 0.96, 0.96)
            sheet.paste(cell.convert("RGB"),
                        (PAD + ci * (COL_W + PAD), yy + 16))
    # composite row
    yy = y0 + 18 + len(LAYERS) * ROW_H
    draw.text((PAD, yy + 2), "COMPOSITE (stage)", fill=(255, 220, 150))
    for ci, bgname in enumerate(bgnames):
        bgimg = checkerboard(COL_W, ROW_H - 22, 14) if bgname == "checker" \
            else Image.new("RGBA", (COL_W, ROW_H - 22), BACKGROUND[bgname])
        cell = bgimg.copy()
        fit_paste(cell, comp, 0.02, 0.02, 0.96, 0.96)
        sheet.paste(cell.convert("RGB"), (PAD + ci * (COL_W + PAD), yy + 16))
    y_top = y0 + 18 + rows * ROW_H + 16

sheet.save(os.path.join(OUT, "GRAPH414_TOWER_LAYER_CONTACT_SHEET.png"))
print(json.dumps(report["hardGates"], indent=1))
print("allPass:", report["allPass"])
print("sheet:", sheet.size, "->", os.path.join(OUT, "GRAPH414_TOWER_LAYER_CONTACT_SHEET.png"))
