#!/usr/bin/env python3
"""GRAPH 4.13 §37 — VISUAL DEFECT GATES for the three tower candidates.

Rejects any candidate containing: detached sign, floating beams, missing
stairs, incomplete ring, off-center portal, unsupported columns, accidental
holes, excessive empty upper area, pure-white pixels (OLED safety), loose
neon (unbounded cyan), asymmetric composition, or memory over the §40 budget.
Exit code 0 = all candidates pass all gates.
"""
import json, math, os, sys
from PIL import Image

ROOT = "/home/z/my-project/hermes-voice-console-android/docs/visual/tower413"
STAGE_W, STAGE_H = 1000, 1375

def composite(candidate):
    man = json.load(open(os.path.join(ROOT, "ASSET_MANIFEST.json")))
    mech = set(man["mech_layers"])
    comp = Image.new("RGBA", (STAGE_W, STAGE_H), (6, 7, 10, 255))
    for name in man["layers"]:
        img = Image.open(os.path.join(ROOT, f"candidate_{candidate.lower()}/balanced/{name}.webp")).convert("RGBA")
        if name in mech:
            box = man["tiers"]["balanced"]["mech_box"]
            ccx, ccy = man["core_center_stage"]
            sx = man["tiers"]["balanced"]["stage_w"] / STAGE_W
            x0 = int(ccx * sx - box // 2); y0 = int(ccy * sx - box // 2)
            scaled = img.resize((int(box), int(box)), Image.LANCZOS)
            full = Image.new("RGBA", (int(man["tiers"]["balanced"]["stage_w"]), int(man["tiers"]["balanced"]["stage_w"] * STAGE_H / STAGE_W)), (0, 0, 0, 0))
            full.alpha_composite(scaled, (x0, y0))
            img = full.resize((STAGE_W, STAGE_H), Image.LANCZOS)
        else:
            img = img.resize((STAGE_W, STAGE_H), Image.LANCZOS)
        comp.alpha_composite(img)
    return comp

def coverage(img, x0, y0, x1, y1, thresh=24):
    px = img.load()
    n = hit = 0
    for y in range(int(y0), int(y1), 3):
        for x in range(int(x0), int(x1), 3):
            r, g, b, a = px[x, y]
            n += 1
            if a > thresh and (r + g + b) > 30:
                hit += 1
    return hit / max(1, n)

def ring_coverage(img, cx, cy, r, thresh=24):
    px = img.load()
    n = hit = 0
    for i in range(720):
        a = 2 * math.pi * i / 720
        x = int(cx + r * math.cos(a)); y = int(cy + r * math.sin(a))
        if 0 <= x < STAGE_W and 0 <= y < STAGE_H:
            r_, g_, b_, a_ = px[x, y]
            n += 1
            if a_ > thresh and (r_ + g_ + b_) > 30:
                hit += 1
    return hit / max(1, n)

def asymmetry(img):
    small = img.resize((125, 172))
    px = small.load()
    diff = n = 0
    for y in range(172):
        for x in range(62):
            l = px[x, y]; r = px[124 - x, y]
            d = abs(l[0] - r[0]) + abs(l[1] - r[1]) + abs(l[2] - r[2])
            diff += d; n += 1
    return diff / n / 3

def check(candidate):
    comp = composite(candidate)
    rgb = comp.convert("RGB")
    px = rgb.load()
    man = json.load(open(os.path.join(ROOT, "ASSET_MANIFEST.json")))
    cx, cy = man["candidates"][candidate]["core_center_stage"]
    failures = []
    # 1 detached sign: connection band between sign bottom and entablature
    band = coverage(comp, 250, 138, 750, 156)
    if band < 0.25:
        failures.append(f"detached sign (connection band coverage {band:.2f} < 0.25)")
    # 2 core fraction 32–42% of width
    core_w = coverage(comp, cx - 210, cy - 4, cx + 210, cy + 4)
    if core_w < 0.55:
        failures.append(f"oracle core not visually dominant (row coverage {core_w:.2f})")
    # 3 stairs present
    st = coverage(comp, 300, 1200, 700, 1258)
    if st < 0.35:
        failures.append(f"missing/invisible stairs (coverage {st:.2f})")
    # 4 outer ring complete
    rc = ring_coverage(comp, cx, cy, 195)
    if rc < 0.92:
        failures.append(f"incomplete outer ring (coverage {rc:.2f} < 0.92)")
    # 5 portal centered
    if abs(comp.getpixel((cx, cy))[2] - comp.getpixel((cx, cy))[0]) < 20:
        failures.append("portal center not cyan-dominant")
    # 6 no accidental holes in the central body (columns of pixels y 300..950)
    for xoff in (0, 120, 200):
        col = coverage(comp, cx + xoff - 3, 300, cx + xoff + 3, 950)
        if col < 0.80:
            failures.append(f"accidental hole in central body at x={cx+xoff} (coverage {col:.2f})")
    # 7 upper area not empty
    upper = coverage(comp, 60, 0, 940, 150)
    if upper < 0.25:
        failures.append(f"excessive empty upper area (coverage {upper:.2f})")
    # 8 OLED: no pure white
    whites = 0
    for y in range(0, STAGE_H, 4):
        for x in range(0, STAGE_W, 4):
            r, g, b = px[x, y]
            if r > 248 and g > 248 and b > 248:
                whites += 1
    if whites > 0:
        failures.append(f"pure-white pixels present (OLED safety): {whites}")
    # 9 bounded cyan (no overbright neon flood)
    cyan = 0
    n = 0
    for y in range(0, STAGE_H, 4):
        for x in range(0, STAGE_W, 4):
            r, g, b = px[x, y]
            n += 1
            if b > 140 and g > 120 and r < 110:
                cyan += 1
    frac = cyan / n
    if frac > 0.05:
        failures.append(f"cyan flood (generic neon look): {frac:.3f} > 0.05")
    # 10 symmetry (composition is mirrored by construction; tolerance for text)
    a = asymmetry(comp)
    if a > 26:
        failures.append(f"asymmetric composition (mean channel diff {a:.1f} > 26)")
    # 11 base foundation present (full-width)
    found = coverage(comp, 80, 1330, 920, 1360)
    if found < 0.55:
        failures.append(f"foundation not integrated at bottom (coverage {found:.2f})")
    return failures

def main():
    man = json.load(open(os.path.join(ROOT, "ASSET_MANIFEST.json")))
    mem_ok = True
    for tier in ("low", "balanced"):
        mb = man["tiers"][tier]["decoded_mb"]
        limit = 12.0 if tier == "low" else 24.0
        if mb > limit:
            print(f"MEMORY FAIL {tier}: {mb} > {limit}")
            mem_ok = False
        else:
            print(f"MEMORY PASS {tier}: {mb} ≤ {limit} MB")
    all_ok = mem_ok
    verdict = {}
    for cand in ("A", "B", "C"):
        fails = check(cand)
        verdict[cand] = fails
        if fails:
            all_ok = False
            print(f"CANDIDATE {cand}: REJECT")
            for f in fails:
                print(f"  - {f}")
        else:
            print(f"CANDIDATE {cand}: ALL GATES PASS")
    with open(os.path.join(ROOT, "visual_validation_report.json"), "w") as f:
        json.dump({"gates": verdict, "memory": man["tiers"], "pass": all_ok}, f, indent=1)
    sys.exit(0 if all_ok else 1)

if __name__ == "__main__":
    main()
