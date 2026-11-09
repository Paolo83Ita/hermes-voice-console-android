#!/usr/bin/env python3
"""Graph 4.14 §16/§19 — THEME_AUTHORITY_CONSUMER_MAP.md +
MARBLE_LIGHT_RENDERED_CONTRAST_REPORT.{json,md} (computed, not asserted)."""

OUT = "/home/z/my-project/hermes-voice-console-android/docs/governance"

def lum(argb):
    def ch(v):
        s = v / 255.0
        return s / 12.92 if s <= 0.03928 else ((s + 0.055) / 1.055) ** 2.4
    r = ch((argb >> 16) & 0xFF); g = ch((argb >> 8) & 0xFF); b = ch(argb & 0xFF)
    return 0.2126 * r + 0.7152 * g + 0.0722 * b

def ratio(fg, bg):
    l1, l2 = lum(fg), lum(bg)
    return round((max(l1, l2) + 0.05) / (min(l1, l2) + 0.05), 2)

# --- Marble monumental palette (ui/monumental/MonumentalColor.kt, GRAPH 4.14) ---
M = {
    "stageBase": 0xFFF1EBDD, "stage": 0xFFEAE3D2, "panel": 0xFFF8F4EA,
    "raised": 0xFFFFFBF1, "sheen": 0xFFE3DBC7,
    "ink": 0xFF241F14, "inkSecondary": 0xFF554E3E, "inkDim": 0xFF665E4C,
    "amber": 0xFF7E4A00, "redBronze": 0xFF9E3A22, "redBronzeText": 0xFF8F3420,
    "cyanCore": 0xFF0085A8, "bronzeLight": 0xFF8F6B33, "gold": 0xFF6E5420,
}
# --- Marble HermesColors semantics (ui/theme/Color.kt) ---
S = {
    "background": 0xFFF2EFE7, "surface": 0xFFFAF7F0, "surfaceVariant": 0xFFE9E4D8,
    "textPrimary": 0xFF201E19, "textSecondary": 0xFF5A554A,
    "textTertiaryReadable": 0xFF6B655A, "textDisabled": 0xFF857E70,
    "textWarning": 0xFF8A5210, "textError": 0xFFB3261E, "textInfo": 0xFF0C6478,
    "outlineFocus": 0xFF7A5C2E,
}

rows = []
def add(surface_name, surface, fg_name, fg, threshold, kind):
    r = ratio(fg, surface)
    rows.append({
        "surface": surface_name, "surfaceArgb": "0x%08X" % surface,
        "foreground": fg_name, "foregroundArgb": "0x%08X" % fg,
        "kind": kind, "threshold": threshold, "ratio": r,
        "verdict": "PASS" if r >= threshold else "FAIL",
    })

stages = [("stageBase", M["stageBase"]), ("stage", M["stage"]), ("panel", M["panel"]), ("raised", M["raised"])]
for sn, sv in stages:
    add(sn, sv, "ink", M["ink"], 4.5, "text")
    add(sn, sv, "inkSecondary", M["inkSecondary"], 4.5, "text")
    add(sn, sv, "inkDim", M["inkDim"], 4.5, "text")
    add(sn, sv, "amber (SIMULAZIONE dot)", M["amber"], 3.0, "icon")
    add(sn, sv, "cyanCore (connected dot)", M["cyanCore"], 3.0, "icon")
    add(sn, sv, "redBronze (seal/error icon)", M["redBronze"], 3.0, "icon")
add("panel", M["panel"], "amber as text (SIMULAZIONE label)", M["amber"], 4.5, "text")
add("panel", M["panel"], "redBronzeText (error text)", M["redBronzeText"], 4.5, "text")
add("panel", M["panel"], "gold (primary nav label)", M["gold"], 4.5, "text")
add("panel", M["panel"], "bronzeLight (interactive outline)", M["bronzeLight"], 3.0, "border")
surfs = [("background", S["background"]), ("surface", S["surface"]), ("surfaceVariant", S["surfaceVariant"])]
for sn, sv in surfs:
    add(sn, sv, "textPrimary", S["textPrimary"], 4.5, "text")
    add(sn, sv, "textSecondary", S["textSecondary"], 4.5, "text")
    add(sn, sv, "textTertiaryReadable", S["textTertiaryReadable"], 4.5, "text")
add("surfaceVariant", S["surfaceVariant"], "textDisabled (≥3.0 + border/fill cues)", S["textDisabled"], 3.0, "disabled")
add("surface", S["surface"], "textWarning", S["textWarning"], 4.5, "text")
add("surface", S["surface"], "textError", S["textError"], 4.5, "text")
add("surface", S["surface"], "textInfo", S["textInfo"], 4.5, "text")
add("background", S["background"], "outlineFocus (focus border)", S["outlineFocus"], 3.0, "border")

fails = [r for r in rows if r["verdict"] == "FAIL"]
import json
with open(f"{OUT}/MARBLE_LIGHT_RENDERED_CONTRAST_REPORT.json", "w") as f:
    json.dump({
        "report": "GRAPH 4.14 §19 — Marble Light rendered contrast",
        "method": "WCAG 2.x relative luminance on the SHIPPED token values (MonumentalMarble + MarbleLightColors); identical formula to the ThemeAuthorityTest pins",
        "thresholds": {"normalText": 4.5, "largeText": 3.0, "icons": 3.0, "interactiveBorders": 3.0, "disabled": 3.0},
        "rows": rows, "failCount": len(fails),
    }, f, indent=1)

md = ["# MARBLE LIGHT RENDERED CONTRAST REPORT (Graph 4.14 §19)", "",
      "Computed on the SHIPPED token values (MonumentalMarble + MarbleLightColors) — the",
      "same WCAG formula the ThemeAuthorityTest pins. Screens covered by the audited",
      "stacks: Console, Appearance, Models and Voice, configuration card, model cards,",
      "Voice and Audio, listening modes, Connection Studio, QR scanner, Archive,",
      "Diagnostics, Self-Test, onboarding.", "",
      "| Surface | Foreground | Kind | Ratio | Threshold | Verdict |", "| --- | --- | --- | --- | --- | --- |"]
for r in rows:
    md.append("| {} 0x{} | {} | {} | **{}:1** | {} | {} |".format(
        r["surface"], r["surfaceArgb"][2:], r["foreground"], r["kind"], r["ratio"], r["threshold"], r["verdict"]))
md += ["", "FAIL rows: {}".format(len(fails))] + ["- ⚠ {} on {}: {:.2f}".format(r["foreground"], r["surface"], r["ratio"]) for r in fails]
with open(f"{OUT}/MARBLE_LIGHT_RENDERED_CONTRAST_REPORT.md", "w") as f:
    f.write("\n".join(md))

print("rows:", len(rows), "fails:", len(fails))
for r in fails:
    print("FAIL:", r["surface"], r["foreground"], r["ratio"])
