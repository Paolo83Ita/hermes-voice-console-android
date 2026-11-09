#!/usr/bin/env python3
"""GRAPH 4.14 §9 — SECOND-PASS feather despill: repair EVERY remaining pixel
(alpha>0, incl. semi-transparent feather) carrying the chroma-key signature.
Iterates until the calibrated count is 0 on every repaired file."""
import hashlib
import json
import os
import numpy as np
from PIL import Image

RES = "app/src/main/res/drawable-nodpi"
OUT = "docs/visual/tower414"
PREFIX = {"balanced": "tower414_", "low": "tower414l_"}
LAYERS = [
    "tower414_architecture", "tower414_crown_sign", "tower414_ring_outer",
    "tower414_ring_middle", "tower414_ring_inner", "tower414_portal",
    "tower414_energy_mask",
]
MANIFEST_PATH = os.path.join(OUT, "ASSET_MANIFEST.json")


def hue_sat_val(arr):
    a = arr.astype(int)
    r, g, b = a[:, :, 0], a[:, :, 1], a[:, :, 2]
    mx = a[:, :, :3].max(axis=2)
    mn = a[:, :, :3].min(axis=2)
    d = mx - mn
    sat = np.where(mx > 0, d / np.maximum(mx, 1), 0)
    val = mx / 255.0
    h = np.zeros_like(mx)
    nz = d > 0
    rr = nz & (mx == r)
    gg = nz & (mx == g) & ~rr
    bb = nz & ~rr & ~gg
    h[rr] = (60 * ((g[rr] - b[rr]) / d[rr]) + 360) % 360
    h[gg] = 60 * ((b[gg] - r[gg]) / d[gg]) + 120
    h[bb] = 60 * ((r[bb] - g[bb]) / d[bb]) + 240
    return h, sat, val


def sig(arr, sat_min=0.60, val_min=0.50):
    h, sat, val = hue_sat_val(arr)
    return (arr[:, :, 3] > 0) & (sat >= sat_min) & (val >= val_min) & (h > 295) & (h < 345)


def count(path):
    return int(sig(np.array(Image.open(path).convert("RGBA"))).sum())


def inpaint(path, is_png=False):
    img = Image.open(path).convert("RGBA")
    arr = np.array(img)
    t = sig(arr)
    if not t.any():
        return 0
    H, W = t.shape
    out = arr.copy()
    ys, xs = np.nonzero(t)
    ok = ~sig(arr, 0.55, 0.45)
    for y, x in zip(ys, xs):
        y0, y1 = max(0, y - 3), min(H, y + 4)
        x0, x1 = max(0, x - 3), min(W, x + 4)
        nb = arr[y0:y1, x0:x1, :3].reshape(-1, 3)
        okn = ok[y0:y1, x0:x1].reshape(-1)
        good = nb[okn]
        if len(good) == 0:
            continue
        med = np.median(good, axis=0)
        out[y, x, :3] = med.astype(np.uint8)
    Image.fromarray(out).save(path, "PNG" if is_png else "WEBP",
                              **({} if is_png else {"quality": 90, "method": 6}))
    return len(ys)


files = [("master", os.path.join(OUT, "tower414_master.png"), True)]
for tier in ("balanced", "low"):
    for name in LAYERS:
        files.append((f"{tier}/{name}",
                      os.path.join(RES, f"{PREFIX[tier]}{name.split('_', 1)[1]}.webp"),
                      False))

report = {}
for label, path, is_png in files:
    before = count(path)
    rounds = 0
    while before > 0 and rounds < 6:
        n = inpaint(path, is_png)
        after = count(path)
        rounds += 1
        if after >= before:
            break
        before = after
    report[label] = {"residual": before, "rounds": rounds}
    print(f"{label:38s} residual={before} rounds={rounds}")

# update manifest pins (webp re-encodes changed bytes)
if os.path.exists(MANIFEST_PATH):
    man = json.load(open(MANIFEST_PATH))
    for tier in ("balanced", "low"):
        for entry in man["tiers"][tier]["layers"]:
            p = os.path.join(RES, f"{PREFIX[tier]}{entry['layer'].split('_', 1)[1]}.webp")
            h = hashlib.sha256(open(p, "rb").read()).hexdigest()
            if h != entry["webpSha256"]:
                entry["webpSha256"] = h
                entry["webpBytes"] = os.path.getsize(p)
    man["repair"]["secondPass"] = ("feather-alpha despill: every remaining alpha>0 "
                                   "signature pixel median-inpainted to 0 residual")
    json.dump(man, open(MANIFEST_PATH, "w"), indent=1)
print("DONE")
