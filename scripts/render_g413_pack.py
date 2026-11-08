#!/usr/bin/env python3
"""GRAPH 4.13 §41 — OWNER TOWER VISUAL PACK renderer.

Renders candidate A (selected) into the deliverable preview set:
  renders/comparison_sheet.png      A | B | C side by side
  renders/poco_idle.png             POCO X6 Pro 393 dp @2.75x, 50% console height
  renders/xiaomi_idle.png           Xiaomi 11 Lite 360 dp @2.75x
  renders/keyboard_open.png         focused form (§38: sign + core + portal)
  renders/tier_low.png / tier_balanced.png   LOW vs BALANCED decode truth
  renders/state_<name>.png          the §39 primary application states
All renders are PREVIEWS for the Owner package; physical verdicts stay
Owner-only. No forbidden reference element is reproduced.
"""
from PIL import Image, ImageDraw, ImageFilter
import os

ROOT = "/home/z/my-project/hermes-voice-console-android/docs/visual/tower413"
OUT = os.path.join(ROOT, "renders")
os.makedirs(OUT, exist_ok=True)
STAGE_W, STAGE_H = 1000, 1375
LAYERS_STATIC = [
    "tower_architecture_rear", "tower_plinth", "tower_stairs", "tower_lower_gate",
    "tower_heavy_pylons", "tower_entablature", "tower_crown_sign", "tower_state_overlay",
]
LAYERS_MECH = ["tower_oracle_outer", "tower_oracle_middle", "tower_oracle_inner", "tower_portal", "tower_energy"]
MECH_BOX = 476
CX, CY = 500, 640

def full_composite(cand):
    man = f"{ROOT}/candidate_{cand.lower()}"
    comp = Image.new("RGBA", (STAGE_W, STAGE_H), (6, 7, 10, 255))
    for n in LAYERS_STATIC:
        img = Image.open(f"{man}/balanced/{n}.webp").convert("RGBA")
        comp.alpha_composite(img.resize((STAGE_W, STAGE_H), Image.LANCZOS))
    for n in LAYERS_MECH:
        img = Image.open(f"{man}/balanced/{n}.webp").convert("RGBA")
        scaled = img.resize((MECH_BOX, MECH_BOX), Image.LANCZOS)
        comp.alpha_composite(scaled, (CX - MECH_BOX // 2, CY - MECH_BOX // 2))
    return comp

def phone_render(comp, width_px, height_frac=0.50, screen_px=(1081, 2400)):
    """Composite the tower onto a device screen at [height_frac] of the height."""
    scr = Image.new("RGB", screen_px, (10, 13, 22))
    tw = int(screen_px[0] * 0.86)              # 14 dp side margins feel
    tower_w = tw
    tower_h = int(tower_w * STAGE_H / STAGE_W)
    max_h = int(screen_px[1] * height_frac)
    if tower_h > max_h:
        tower_h = max_h
        tower_w = int(tower_h * STAGE_W / STAGE_H)
    tower = comp.resize((tower_w, tower_h), Image.LANCZOS).convert("RGB")
    x = (screen_px[0] - tower_w) // 2
    scr.paste(tower, (x, 0))
    d = ImageDraw.Draw(scr)
    # console chrome hint: state chip + composer base (context, not the deliverable)
    d.rounded_rectangle([screen_px[0] // 2 - 90, tower_h + 14, screen_px[0] // 2 + 90, tower_h + 44],
                        radius=15, outline=(90, 80, 55), width=2)
    return scr

def state_overlay(comp, state):
    img = comp.copy()
    d = ImageDraw.Draw(img, "RGBA")
    core = (CX, CY)
    if state == "listening":
        d.ellipse([CX - 115, CY - 115, CX + 115, CY + 115], fill=(39, 196, 216, 85))
    elif state == "transcribing":
        for i, r in enumerate((150, 118, 88)):
            a = 60 + 40 * ((i + 1) % 2)
            d.ellipse([CX - r, CY - r, CX + r, CY + r], outline=(214, 176, 104, a), width=5)
    elif state == "processing":
        d.arc([CX - 190, CY - 190, CX + 190, CY + 190], 20, 120, fill=(53, 224, 255, 140), width=8)
        d.arc([CX - 140, CY - 140, CX + 140, CY + 140], 200, 300, fill=(53, 224, 255, 100), width=6)
    elif state == "speaking":
        d.ellipse([CX - 130, CY - 130, CX + 130, CY + 130], fill=(39, 196, 216, 70))
        d.ellipse([CX - 150, CY - 150, CX + 150, CY + 150], outline=(39, 196, 216, 110), width=4)
    elif state == "privacy_muted":
        d.ellipse([CX - 55, 1095, CX + 55, 1205], fill=(179, 69, 43, 200))
        d.ellipse([CX - 20, 1130, CX + 20, 1170], fill=(255, 217, 194, 230))
    elif state == "disconnected":
        d.rectangle([CX - 240, 330, CX + 240, 1000], fill=(6, 7, 10, 90))
    elif state == "error":
        d.arc([CX - 160, CY - 160, CX + 160, CY + 160], -30, -8, fill=(224, 166, 60, 179), width=12)
        d.arc([CX - 160, CY - 160, CX + 160, CY + 160], 150, 166, fill=(224, 166, 60, 143), width=10)
    return img

def main():
    A = full_composite("A")
    B = full_composite("B")
    C = full_composite("C")
    # comparison sheet
    cell_w, cell_h = 500, 687
    sheet = Image.new("RGB", (cell_w * 3 + 40, cell_h + 60), (12, 14, 20))
    d = ImageDraw.Draw(sheet)
    for i, (cand, comp) in enumerate((("A — MONUMENTAL ORACLE CITADEL", A), ("B — MECHANICAL TOWER CORE", B), ("C — MOBILE MONUMENTAL", C))):
        thumb = comp.resize((cell_w - 20, int((cell_w - 20) * STAGE_H / STAGE_W)), Image.LANCZOS).convert("RGB")
        sheet.paste(thumb, (10 + i * (cell_w + 10), 44))
        d.text((14 + i * (cell_w + 10), 12), cand, fill=(214, 176, 104))
    sheet.save(f"{OUT}/comparison_sheet.png")

    # device renders (candidate A)
    A.convert("RGB").save(f"{OUT}/stage_idle.png")
    phone_render(A, 393).save(f"{OUT}/poco_idle.png")
    phone_render(A, 360, screen_px=(990, 2400)).save(f"{OUT}/xiaomi_idle.png")
    phone_render(A, 393, height_frac=0.24).save(f"{OUT}/keyboard_open.png")
    phone_render(state_overlay(A, "listening"), 393).save(f"{OUT}/state_listening.png")
    phone_render(state_overlay(A, "transcribing"), 393).save(f"{OUT}/state_transcribing.png")
    phone_render(state_overlay(A, "processing"), 393).save(f"{OUT}/state_processing_local.png")
    phone_render(state_overlay(A, "speaking"), 393).save(f"{OUT}/state_speaking.png")
    phone_render(state_overlay(A, "privacy_muted"), 393).save(f"{OUT}/state_privacy_muted.png")
    phone_render(state_overlay(A, "disconnected"), 393).save(f"{OUT}/state_disconnected.png")
    phone_render(state_overlay(A, "error"), 393).save(f"{OUT}/state_error.png")

    # tier truth: LOW tier composite vs BALANCED tier composite
    def tier_composite(cand, tier):
        man = f"{ROOT}/candidate_{cand.lower()}/{tier}"
        import json
        man_j = json.load(open(f"{ROOT}/ASSET_MANIFEST.json"))
        sw = man_j["tiers"][tier]["stage_w"]
        sh = int(sw * STAGE_H / STAGE_W)
        comp = Image.new("RGBA", (sw, sh), (6, 7, 10, 255))
        for n in LAYERS_STATIC:
            img = Image.open(f"{man}/{n}.webp").convert("RGBA")
            comp.alpha_composite(img)
        box = man_j["tiers"][tier]["mech_box"]
        cx, cy = int(CX * sw / STAGE_W), int(CY * sw / STAGE_W)
        for n in LAYERS_MECH:
            img = Image.open(f"{man}/{n}.webp").convert("RGBA")
            comp.alpha_composite(img.resize((box, box), Image.LANCZOS), (cx - box // 2, cy - box // 2))
        return comp
    tier_composite("A", "low").convert("RGB").save(f"{OUT}/tier_low.png")
    tier_composite("A", "balanced").convert("RGB").save(f"{OUT}/tier_balanced.png")
    print("renders written to", OUT)

if __name__ == "__main__":
    main()
