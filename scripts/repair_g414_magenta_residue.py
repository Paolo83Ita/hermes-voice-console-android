#!/usr/bin/env python3
"""GRAPH 4.14 §9 — CALIBRATED MAGENTA-DESPILL REPAIR (in-slice, deterministic).

Finding (independent re-audit of the SHIPPED slices): 110 opaque pixels on the
637x903 master carry the chroma-key signature (hue 295-345, sat>=0.62,
val>=0.55) in 50 clusters of 1-9 px at feather boundaries (crown-sign letters
and the right despill edge). The Owner reference contains ZERO pixels of that
signature (1.57M opaque px) — the tones are extraction residue, NOT artwork.

Repair: for every signature pixel (and 1px feather neighbors carrying the
relaxed signature) REPLACE THE RGB WITH THE MEDIAN OF NON-SIGNATURE
NEIGHBORS (5x5), alpha untouched. No re-extraction, no re-slicing, no
geometry change; only the contaminated pixels change.

Then: re-encode the affected slices (webp q90 method=6, alpha kept), update
ASSET_MANIFEST.json (sha256/bytes + repair provenance), and re-verify.
"""
import hashlib
import json
import os
from collections import deque
import numpy as np
from PIL import Image

RES = "app/src/main/res/drawable-nodpi"
OUT = "docs/visual/tower414"
LAYERS = [
    "tower414_architecture", "tower414_crown_sign", "tower414_ring_outer",
    "tower414_ring_middle", "tower414_ring_inner", "tower414_portal",
    "tower414_energy_mask",
]
PREFIX = {"balanced": "tower414_", "low": "tower414l_"}
MANIFEST = json.load(open(os.path.join(OUT, "ASSET_MANIFEST.json")))


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


def signature(arr, sat_min, val_min):
    h, sat, val = hue_sat_val(arr)
    alpha = arr[:, :, 3]
    return (alpha > 0) & (sat >= sat_min) & (val >= val_min) & (h > 295) & (h < 345)


def repair_slice(path):
    img = Image.open(path).convert("RGBA")
    arr = np.array(img)
    sig = signature(arr, 0.62, 0.55)
    # 1px dilation for feather edges (relaxed signature)
    H, W = sig.shape
    dil = sig.copy()
    for dy in (-1, 0, 1):
        for dx in (-1, 0, 1):
            dil |= np.roll(np.roll(sig, dy, 0), dx, 1)
    soft = dil & ~sig & (signature(arr, 0.50, 0.50) | (arr[:, :, 3] < 128))
    targets = sig | soft
    n_strong, n_soft = int(sig.sum()), int(soft.sum())
    if not targets.any():
        return 0, 0
    out = arr.copy()
    ys, xs = np.nonzero(targets)
    ok = ~signature(arr, 0.60, 0.50)
    for y, x in zip(ys, xs):
        y0, y1 = max(0, y - 2), min(H, y + 3)
        x0, x1 = max(0, x - 2), min(W, x + 3)
        nb = arr[y0:y1, x0:x1, :3].reshape(-1, 3)
        okn = ok[y0:y1, x0:x1].reshape(-1)
        good = nb[okn]
        if len(good) == 0:
            continue
        med = np.median(good, axis=0)
        out[y, x, :3] = med.astype(np.uint8)
    repaired = Image.fromarray(out)
    repaired.save(path, "WEBP", quality=90, method=6)
    return n_strong, n_soft


changed = []
for tier in ("balanced", "low"):
    for name in LAYERS:
        p = os.path.join(RES, f"{PREFIX[tier]}{name.split('_', 1)[1]}.webp")
        n_strong, n_soft = repair_slice(p)
        if n_strong or n_soft:
            changed.append((tier, name, n_strong, n_soft))
        print(f"{tier:8s} {name:28s} strong={n_strong:3d} soft={n_soft:3d}")

# update manifest pins for changed slices
for tier_key, tier in (("balanced", "balanced"), ("low", "low")):
    for entry in MANIFEST["tiers"][tier]["layers"]:
        p = os.path.join(RES, f"{PREFIX[tier]}{entry['layer'].split('_', 1)[1]}.webp")
        h = hashlib.sha256(open(p, "rb").read()).hexdigest()
        if h != entry["webpSha256"]:
            entry["webpSha256"] = h
            entry["webpBytes"] = os.path.getsize(p)

MANIFEST["repair"] = {
    "date": "graph-4.14-continuation",
    "finding": "110 opaque chroma-key-signature pixels (50 clusters, 1-9 px) at feather boundaries on the master; Owner reference has ZERO such pixels (calibrated cross-check)",
    "method": "in-slice median inpaint of signature + 1px feather pixels (no re-extraction, no geometry change)",
    "changedSlices": [f"{t}/{n}" for t, n, _, _ in changed],
}
with open(os.path.join(OUT, "ASSET_MANIFEST.json"), "w") as f:
    json.dump(MANIFEST, f, indent=1)

total = sum(c[2] for c in changed)
print("changed slices:", len(changed), "| strong pixels repaired:", total)
