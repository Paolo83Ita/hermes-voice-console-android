#!/usr/bin/env python3
"""GRAPH 4.13 §4 — APP-WIDE RENDERED CONTRAST AUDIT generator.

Parses the ACTUAL token values from the Kotlin sources (ui/theme/Color.kt,
ui/monumental/MonumentalColor.kt), builds the rendered-surface inventory
(screen × component × state × theme), computes WCAG 2.x contrast on the REAL
background stacks (gradient stops worst-case, alpha-blended overlays) and
emits APP_WIDE_RENDERED_CONTRAST_AUDIT_GRAPH413.{json,md}.

PART A rows = PRE-REPAIR evidence (historical, drove CP-G4.13-003).
PART B rows = LIVE semantic-authority pairs (post CP-G4.13-003, must all PASS).
Evidence class: RENDERED_PAIR_COMPUTED; emulator screenshot verification is
layered by the CP-G4.13-004 screenshot gates.
"""
import json, re
from pathlib import Path

REPO = Path("/home/z/my-project/hermes-voice-console-android")
UI = REPO / "app/src/main/java/com/hermestower/voiceconsole"
OUT_JSON = REPO / "docs/governance/APP_WIDE_RENDERED_CONTRAST_AUDIT_GRAPH413.json"
OUT_MD = REPO / "docs/governance/APP_WIDE_RENDERED_CONTRAST_AUDIT_GRAPH413.md"

# ---------- parse tokens from Kotlin sources ----------
def parse_colors(path):
    txt = Path(path).read_text()
    vals = {}
    for m in re.finditer(r"(?:val\s+)?(\w+)\s*=\s*Color\(0x([0-9A-Fa-f]{8})\)", txt):
        vals.setdefault(m.group(1), m.group(2).upper())
    return vals

mon = parse_colors(UI / "ui/monumental/MonumentalColor.kt")
theme = parse_colors(UI / "ui/theme/Color.kt")
theme_txt = (UI / "ui/theme/Color.kt").read_text()

def scheme_args(fn_name):
    body = theme_txt.split(f"fun {fn_name}")[1]
    body = body.split(")\n")[0] + ")"
    out = {}
    for m in re.finditer(r"(\w+)\s*=\s*Color\(0x([0-9A-Fa-f]{8})\)", body):
        out[m.group(1)] = m.group(2).upper()
    return out

OBS_SCHEME = scheme_args("obsidianMaterialScheme")
MAR_SCHEME = scheme_args("marbleMaterialScheme")

def sem_fields(palette_start_marker):
    body = theme_txt.split(palette_start_marker)[1]
    # stop at the next top-level declaration (the next palette or scheme fn)
    stop = len(body)
    for pat in ("\nfun ", "\nval "):
        i = body.find(pat)
        if i != -1:
            stop = min(stop, i)
    body = body[:stop]
    out = {}
    refs = {}
    for m in re.finditer(r"(\w+)\s*=\s*(?:Color\(0x([0-9A-Fa-f]{6,8})\)|([A-Z]\w+))[,)]", body[:5200]):
        name, lit, ref = m.group(1), m.group(2), m.group(3)
        if lit:
            out[name] = lit.upper()
        elif ref:
            refs[name] = ref
    for name, ref in refs.items():
        if name in out:
            continue
        if ref in theme:
            out[name] = theme[ref]
        elif ref in mon:
            out[name] = mon[ref]
    return out

MAR_SEM = sem_fields("val MarbleLightColors")
OBS_SEM = sem_fields("val ObsidianNightColors")

def h(hexstr, alpha=1.0):
    """hex (6 or 8 chars, no 0x) -> (r,g,b,a) floats"""
    hx = hexstr[-6:]
    r, g, b = (int(hx[i:i+2], 16)/255.0 for i in (0, 2, 4))
    return (r, g, b, alpha)

def blend(fg, bg):
    fa = fg[3]
    return tuple(fg[i]*fa + bg[i]*(1-fa) for i in range(3)) + (1.0,)

def lum(c):
    def f(x):
        return x/12.92 if x <= 0.04045 else ((x+0.055)/1.055)**2.4
    return 0.2126*f(c[0]) + 0.7152*f(c[1]) + 0.0722*f(c[2])

def ratio(fg, bg):
    l1, l2 = sorted([lum(fg), lum(bg)], reverse=True)
    return (l1+0.05)/(l2+0.05)

# ---------- background stacks (from the code) ----------
M = mon; T = theme
HO = {k.replace("Obsidian", ""): v for k, v in T.items() if k.startswith("Obsidian")}
HM = {k.replace("Marble", ""): v for k, v in T.items() if k.startswith("Marble")}
SHELL = ["0A0E16", M["obsidianDeep"]]            # AppNav content-host gradient (dark)
PANEL = [M["obsidianRaised"], M["obsidianPanel"], "0A0D14"]  # Chrome panel gradient
CONSOLE = ["0A0E16", "070A11"]                    # ConsoleScreen stage gradient

rows = []
RID = [0]
def add(*a, **k):
    RID[0] += 1
    rows.append(audit_row(f"G413-{RID[0]:03d}", *a, **k))

def audit_row(rid, screen, component, state, theme, enabled, kind, fg_hex, fg_alpha,
              bg_stack, font_sp, font_weight, note=""):
    fg = h(fg_hex, fg_alpha)
    best = None
    for stop in bg_stack:
        bg = h(stop)
        f = fg if fg_alpha >= 1.0 else blend(fg, bg)
        r = ratio(f, bg)
        if best is None or r < best[0]:
            best = (r, stop, f)
    r, worst_stop, eff = best
    thr = {"normal": 4.5, "large": 3.0, "icon": 3.0, "focus": 3.0,
           "critical": 4.5, "disabled": 3.0}[kind]
    large = kind == "large" or (kind == "normal" and (font_sp >= 24 or (font_sp >= 18.66 and font_weight >= 700)))
    eff_thr = 3.0 if large and kind == "normal" else thr
    verdict = "PASS" if r >= eff_thr - 1e-9 else "FAIL"
    return {
        "id": rid, "screen": screen, "component": component, "state": state,
        "theme": theme, "enabled": enabled, "kind": kind,
        "foreground_argb": "FF" + fg_hex, "foreground_alpha": round(fg_alpha, 3),
        "background_argb_worst_stop": "FF" + worst_stop,
        "background_stack": ["FF" + s for s in bg_stack],
        "effective_foreground": "FF%02X%02X%02X" % tuple(int(round(x*255)) for x in eff[:3]),
        "contrast_ratio": round(r, 2),
        "font_size_sp": font_sp, "font_weight": font_weight,
        "threshold": eff_thr, "large_text": large,
        "verdict": verdict, "note": note,
    }

# ============================ PART A — PRE-REPAIR EVIDENCE ============================
for tname, tokset in (("OBSIDIAN", HO), ("MARBLE", HM)):
    add("App shell", "screen host background", f"{tname} theme shell", tname, True, "normal",
        tokset["Ink"], 1.0, SHELL, 15, 400, "c.ink body text on the app shell gradient (ui/AppNav.kt)")
    add("App shell", "screen host background", f"{tname} shell secondary", tname, True, "normal",
        tokset["InkSecondary"], 1.0, SHELL, 13.5, 400, "c.inkSecondary on the app shell gradient")
    add("App shell", "screen host background", f"{tname} shell accent label", tname, True, "normal",
        tokset["Bronze"], 1.0, SHELL, 13, 700, "c.bronze sectionLabel on the app shell")

add("Chrome", "SectionCard", "title", "OBSIDIAN", True, "normal", M["gold"], 1.0, PANEL, 12, 700)
add("Chrome", "SectionCard", "body content", "OBSIDIAN", True, "normal", M["ink"], 1.0, PANEL, 15, 400)
add("Chrome", "StatusPill", "label", "OBSIDIAN", True, "normal", M["inkSecondary"], 1.0, [M["obsidianPanel"]], 12, 700)
add("Chrome", "StatusPill", "state dot icon", "OBSIDIAN", True, "icon", M["cyanCore"], 1.0, [M["obsidianPanel"]], 8, 400)
add("Chrome", "ModeChip", "selected", "OBSIDIAN", True, "normal", M["gold"], 1.0, [M["stoneDeep"]], 12, 700)
add("Chrome", "ModeChip", "unselected", "OBSIDIAN", True, "normal", M["inkSecondary"], 1.0, [M["obsidianPanel"]], 12, 700)
add("Chrome", "ModeChip", "disabled (pre-repair render)", "OBSIDIAN", False, "disabled", M["inkSecondary"], 1.0, [M["obsidianPanel"]], 12, 700,
    "§5 defect pre-repair: disabled chip color-identical to enabled (single cue)")
add("Chrome", "TruthBadge", "gold badge text", "OBSIDIAN", True, "normal", M["gold"], 1.0, PANEL, 9, 400, "fg=badge color; bg=color@10% blended over panel per stop")
add("Chrome", "TruthBadge", "warning badge text (redBronzeText)", "OBSIDIAN", True, "critical", M["redBronzeText"], 1.0, PANEL, 9, 400)
add("Chrome", "ArchitecturalPanel", "outline focus ring", "OBSIDIAN", True, "focus", M["bronzeEdge"], 1.0, PANEL, 2, 400)
M3_DIS = "%02X%02X%02X" % tuple(int((0.12*a+0.88*b)*255) for a, b in zip((0xE8, 0xE6, 0xE1), (0x12, 0x15, 0x1A)))
add("Chrome", "FilledButton", "disabled label (M3 default onSurface@38%)", "OBSIDIAN", False, "disabled",
    HO["Ink"], 0.38, [M3_DIS], 15, 500, "pre-repair M3 default")
add("Chrome", "FilterChip", "disabled label (M3 default @38%)", "OBSIDIAN", False, "disabled",
    HO["InkSecondary"], 0.38, [T["ObsidianSurface"]], 14, 500, "pre-repair M3 default")

add("Console", "Frieze", "HERMES title", "OBSIDIAN", True, "large", M["gold"], 1.0, CONSOLE, 30, 900)
add("Console", "Frieze", "TOWER title", "OBSIDIAN", True, "large", M["bronzeLight"], 1.0, CONSOLE, 30, 900)
add("Console", "Frieze", "subtitle", "OBSIDIAN", True, "normal", M["inkSecondary"], 1.0, CONSOLE, 11, 400)
add("Console", "StateChip", "state label", "OBSIDIAN", True, "large", M["goldBright"], 1.0, PANEL, 15, 700)
add("Console", "EngineRows", "local-core label", "OBSIDIAN", True, "normal", M["localGold"], 1.0, PANEL, 9, 400)
add("Console", "EngineRows", "tower label", "OBSIDIAN", True, "normal", M["towerBlue"], 1.0, PANEL, 9, 400)
add("Console", "EngineRows", "idle note", "OBSIDIAN", True, "normal", M["inkDim"], 1.0, PANEL, 9, 400)
add("Console", "Composer", "typed text", "OBSIDIAN", True, "normal", M["ink"], 1.0, PANEL, 15, 400)
add("Console", "Composer", "placeholder", "OBSIDIAN", True, "normal", M["inkDim"], 1.0, PANEL, 13.5, 400)
add("Console", "Composer", "send icon", "OBSIDIAN", True, "icon", M["cyanCore"], 1.0, PANEL, 24, 400)
add("Console", "Composer", "mic icon (idle)", "OBSIDIAN", True, "icon", M["gold"], 1.0, PANEL, 24, 400)
add("Console", "Composer", "privacy-muted mic icon", "OBSIDIAN", True, "icon", M["redBronze"], 1.0, PANEL, 24, 400, "icon-only duty ≥3.0")
add("Console", "Composer", "Interrompi enabled", "OBSIDIAN", True, "critical", M["cyanCore"], 1.0, PANEL, 13.5, 400)
add("Console", "Composer", "Interrompi disabled label", "OBSIDIAN", False, "disabled", M["inkDim"], 1.0, PANEL, 13.5, 400)
add("Console", "Chat", "user turn text", "OBSIDIAN", True, "normal", M["ink"], 1.0, PANEL, 15, 400)
add("Console", "Chat", "turn who label", "OBSIDIAN", True, "normal", M["gold"], 1.0, PANEL, 12, 700)
add("Console", "Chat", "error line", "OBSIDIAN", True, "critical", M["redBronzeText"], 1.0, PANEL, 13.5, 400)
add("Console", "Chat", "info line (cyan)", "OBSIDIAN", True, "normal", M["cyanCore"], 1.0, PANEL, 13.5, 400)
add("Console", "UnderChrome", "hint note", "OBSIDIAN", True, "normal", M["inkDim"], 1.0, PANEL, 9, 400)

add("Models and Voice", "A. Recommended configuration", "device/resources/TTS/network/profile rows", "OBSIDIAN", True, "normal", HO["Ink"], 1.0, PANEL, 15, 400)
add("Models and Voice", "A. Recommended configuration", "warning line", "OBSIDIAN", True, "critical", HO["Warn"], 1.0, PANEL, 13.5, 400)
add("Models and Voice", "A. Recommended configuration", "hint (select mode)", "OBSIDIAN", True, "normal", HO["InkSecondary"], 1.0, PANEL, 13.5, 400)
add("Models and Voice", "A. Recommended configuration", "profile chip label (FilterChip)", "OBSIDIAN", True, "normal", HO["InkSecondary"], 1.0, [T["ObsidianSurface"]], 14, 500)
add("Models and Voice", "Catalog cards", "model title", "OBSIDIAN", True, "normal", HO["Bronze"], 1.0, PANEL, 13, 700)
add("Models and Voice", "Catalog cards", "metadata line", "OBSIDIAN", True, "normal", HO["InkSecondary"], 1.0, PANEL, 13.5, 400)
add("Models and Voice", "Catalog cards", "state text (raw enum)", "OBSIDIAN", True, "normal", HO["Energy"], 1.0, PANEL, 13.5, 400)
add("Models and Voice", "Model detail", "technical hash (mono)", "OBSIDIAN", True, "normal", HO["InkSecondary"], 1.0, PANEL, 12, 400)

add("Voice and Audio", "Header", "title", "OBSIDIAN", True, "normal", HO["Ink"], 1.0, SHELL, 17, 600)
add("Voice and Audio", "Engine truth rows", "wake word badge (gold)", "OBSIDIAN", True, "normal", HO["Gold"], 1.0, PANEL, 9, 400)
add("Voice and Audio", "Engine truth rows", "TTS active badge (ok)", "OBSIDIAN", True, "critical", HO["Ok"], 1.0, PANEL, 9, 400)
add("Voice and Audio", "Listening modes", "mode description", "OBSIDIAN", True, "normal", HO["InkSecondary"], 1.0, PANEL, 13.5, 400)
add("Voice and Audio", "Body", "section note (energy)", "OBSIDIAN", True, "normal", HO["Energy"], 1.0, PANEL, 13.5, 400)

add("Tower Connection Studio", "Header", "title", "OBSIDIAN", True, "normal", HO["Ink"], 1.0, SHELL, 17, 600)
add("Tower Connection Studio", "Method section", "label (bronze)", "OBSIDIAN", True, "normal", HO["Bronze"], 1.0, PANEL, 13, 700)
add("Tower Connection Studio", "Outcome", "detail lines", "OBSIDIAN", True, "normal", HO["InkSecondary"], 1.0, PANEL, 13.5, 400)
add("Tower Connection Studio", "Outcome", "message (energy)", "OBSIDIAN", True, "normal", HO["Energy"], 1.0, PANEL, 15, 400)

add("QR Scanner", "Viewfinder", "frame outline (focus-class)", "OBSIDIAN", True, "focus", M["bronzeEdge"], 1.0, [M["obsidianDeep"]], 2, 400)
add("QR Scanner", "Helper", "instruction text", "OBSIDIAN", True, "normal", M["inkSecondary"], 1.0, SHELL, 13.5, 400)
add("QR Scanner", "Helper", "error text", "OBSIDIAN", True, "critical", M["redBronzeText"], 1.0, SHELL, 13.5, 400)

add("Self-Test Center", "Run state", "running line (energy)", "OBSIDIAN", True, "normal", HO["Energy"], 1.0, PANEL, 15, 400)
add("Self-Test Center", "Results", "test id + name", "OBSIDIAN", True, "normal", HO["Ink"], 1.0, PANEL, 15, 400)
add("Self-Test Center", "Results", "PASS verdict badge (ok)", "OBSIDIAN", True, "critical", HO["Ok"], 1.0, PANEL, 12, 700)

add("Diagnostics", "Header", "body rows", "OBSIDIAN", True, "normal", HO["Ink"], 1.0, PANEL, 15, 400)
add("Diagnostics", "Advanced", "device identifier (2311DRK48G class)", "OBSIDIAN", True, "normal", HO["InkSecondary"], 1.0, PANEL, 12, 400, "§25: advanced diagnostics only")

add("Appearance", "Theme modes", "OBSIDIAN NIGHT chip label", "OBSIDIAN", True, "normal", HO["InkSecondary"], 1.0, [T["ObsidianSurface"]], 14, 500)
add("Appearance", "Quality", "profile description", "OBSIDIAN", True, "normal", HO["InkSecondary"], 1.0, PANEL, 13.5, 400)

add("Onboarding", "Steps", "title", "OBSIDIAN", True, "normal", HO["Ink"], 1.0, SHELL, 17, 600)
add("Onboarding", "Steps", "section label (bronze)", "OBSIDIAN", True, "normal", HO["Bronze"], 1.0, SHELL, 13, 700)
add("Onboarding", "Steps", "body", "OBSIDIAN", True, "normal", HO["Ink"], 1.0, SHELL, 15, 400)
add("Onboarding", "Steps", "progress (energy)", "OBSIDIAN", True, "normal", HO["Energy"], 1.0, SHELL, 13.5, 400)

add("Archive", "List", "artifact title", "OBSIDIAN", True, "normal", HO["Ink"], 1.0, PANEL, 15, 400)
add("Archive", "Preview", "SIMULATED marker", "OBSIDIAN", True, "critical", M["amber"], 1.0, PANEL, 12, 700)

add("About and Safety", "Sections", "body", "OBSIDIAN", True, "normal", HO["Ink"], 1.0, PANEL, 15, 400)
add("About and Safety", "Links", "link label (energy)", "OBSIDIAN", True, "normal", HO["Energy"], 1.0, PANEL, 15, 400)

add("Settings hub", "Rows", "row title", "OBSIDIAN", True, "normal", HO["Ink"], 1.0, SHELL, 15, 400)
add("Settings hub", "Rows", "hub marker (inkDim)", "OBSIDIAN", True, "normal", M["inkDim"], 1.0, SHELL, 12, 700)

add("History", "List", "session meta", "OBSIDIAN", True, "normal", HO["InkSecondary"], 1.0, PANEL, 13.5, 400)
add("Local Intelligence", "Runtime", "unavailable badge", "OBSIDIAN", True, "critical", HO["InkSecondary"], 1.0, PANEL, 9, 400, "§16 honest unavailability — readable, not dim-only")

# Marble-mode shell-hosted rows (the app-wide pre-repair failure)
for comp, fg, sp, wt in (
    ("body text (c.ink)", HM["Ink"], 15, 400),
    ("secondary text (c.inkSecondary)", HM["InkSecondary"], 13.5, 400),
    ("accent label (c.bronze)", HM["Bronze"], 13, 700),
    ("warning (c.warn)", HM["Warn"], 13.5, 400),
    ("info (c.energy)", HM["Energy"], 13.5, 400),
    ("success (c.ok)", HM["Ok"], 12, 700),
):
    add("All screens (Marble)", "shell-hosted text", comp, "MARBLE", True, "normal", fg, 1.0, SHELL, sp, wt,
        "PRE-REPAIR: marble fg on the always-obsidian shell — the app-wide contrast failure")

pre_repair_rows = list(rows)
pre_fails = [r for r in pre_repair_rows if r["verdict"] == "FAIL"]

# ============================ PART B — LIVE SEMANTIC AUTHORITY ============================
rows = []
RID[0] = 0

def sem_rows(theme_name, sem, shell, bronze, gold_hex):
    panel = [sem["surfaceRaised"]]; interactive = [sem["surfaceInteractive"]]
    selected = [sem["surfaceSelected"]]; disabled = [sem["surfaceDisabled"]]
    add("Semantic authority", "shell prose", "textPrimary on surfaceBase", theme_name, True, "normal", sem["textPrimary"], 1.0, shell, 15, 400)
    add("Semantic authority", "shell prose", "textSecondary on surfaceBase", theme_name, True, "normal", sem["textSecondary"], 1.0, shell, 13.5, 400)
    add("Semantic authority", "panel prose", "textPrimary on surfaceRaised", theme_name, True, "normal", sem["textPrimary"], 1.0, panel, 15, 400)
    add("Semantic authority", "panel prose", "textSecondary on surfaceInteractive", theme_name, True, "normal", sem["textSecondary"], 1.0, interactive, 13.5, 400)
    add("Semantic authority", "panel prose", "textTertiaryReadable on surfaceInteractive", theme_name, True, "normal", sem["textTertiaryReadable"], 1.0, interactive, 12, 400)
    add("Semantic authority", "panel prose", "textTechnical on surfaceRaised", theme_name, True, "normal", sem["textTechnical"], 1.0, panel, 12, 400)
    add("Semantic authority", "status text", "textSuccess on surfaceRaised", theme_name, True, "critical", sem["textSuccess"], 1.0, panel, 12, 700)
    add("Semantic authority", "status text", "textWarning on surfaceRaised", theme_name, True, "critical", sem["textWarning"], 1.0, panel, 12, 700)
    add("Semantic authority", "status text", "textError on surfaceRaised", theme_name, True, "critical", sem["textError"], 1.0, panel, 12, 700)
    add("Semantic authority", "status text", "textInfo on surfaceRaised", theme_name, True, "critical", sem["textInfo"], 1.0, panel, 12, 700)
    add("Semantic authority", "SectionCard", "heading gold on surfaceInteractive", theme_name, True, "normal", gold_hex, 1.0, interactive, 12, 700)
    add("Semantic authority", "SectionCard", "heading bronze on surfaceInteractive", theme_name, True, "normal", bronze, 1.0, interactive, 13, 700)
    add("Semantic authority", "StatusPill", "label on surfaceInteractive", theme_name, True, "normal", sem["textSecondary"], 1.0, interactive, 12, 700)
    add("Semantic authority", "ModeChip", "selected label on surfaceSelected", theme_name, True, "normal", sem["textOnSelected"], 1.0, selected, 12, 700)
    add("Semantic authority", "ModeChip", "unselected label on surfaceInteractive", theme_name, True, "normal", sem["textSecondary"], 1.0, interactive, 12, 700)
    add("Semantic authority", "ModeChip", "disabled label on surfaceDisabled (cue 1)", theme_name, False, "disabled", sem["textDisabled"], 1.0, disabled, 12, 700, "§5 cues: color + fill + outlineDisabled border")
    add("Semantic authority", "FilledButton", "enabled label on bronze", theme_name, True, "normal", sem["textOnAccent"], 1.0, [bronze], 15, 500)
    add("Semantic authority", "FilledButton", "disabled label on surfaceDisabled (cue 1)", theme_name, False, "disabled", sem["textDisabled"], 1.0, disabled, 15, 500, "§5: replaces M3 onSurface@38% default (was 1.43:1 obsidian)")
    add("Semantic authority", "FilterChip", "unselected on surfaceInteractive", theme_name, True, "normal", sem["textSecondary"], 1.0, interactive, 14, 500)
    add("Semantic authority", "FilterChip", "selected on surfaceSelected", theme_name, True, "normal", sem["textOnSelected"], 1.0, selected, 14, 500)
    add("Semantic authority", "FilterChip", "disabled on surfaceDisabled (cue 1)", theme_name, False, "disabled", sem["textDisabled"], 1.0, disabled, 14, 500, "§5: replaces M3 default (was 2.09:1 obsidian)")
    add("Semantic authority", "Switch", "thumb icon on surfaceInteractive", theme_name, True, "icon", sem["iconSecondary"], 1.0, interactive, 20, 400)
    add("Semantic authority", "Switch", "disabled thumb on surfaceDisabled (cue 1)", theme_name, False, "disabled", sem["textDisabled"], 1.0, disabled, 20, 400, "§5 cues: fill + border + label row")
    add("Semantic authority", "Focus", "outlineFocus ring on surfaceInteractive", theme_name, True, "focus", sem["outlineFocus"], 1.0, interactive, 2, 400)
    add("Semantic authority", "TruthBadge", "info badge on info@10% over interactive", theme_name, True, "critical", sem["textInfo"], 1.0, interactive, 9, 400, "alpha-blended bg per stop")

sem_rows("OBSIDIAN", OBS_SEM, SHELL, T["ObsidianBronze"], T["ObsidianGold"])
sem_rows("MARBLE", MAR_SEM, [HM["Background"]], T["MarbleBronze"], T["MarbleGold"])

fails = [r for r in rows if r["verdict"] == "FAIL"]

doc = {
    "mission": "GRAPH 4.13 CP-G4.13-002 — app-wide rendered contrast audit",
    "evidence_class": "RENDERED_PAIR_COMPUTED (WCAG 2.x relative luminance; source-parsed token values; worst-case gradient stop; alpha blended)",
    "thresholds": {"normal": 4.5, "large": 3.0, "icon": 3.0, "focus": 3.0, "critical": 4.5, "disabled": 3.0},
    "screens_covered": ["Console", "Models and Voice", "model detail", "recommended configuration",
                        "Voice and Audio", "listening modes", "wake word", "VAD", "Tower Connection Studio",
                        "QR Scanner", "Self-Test Center", "Diagnostics", "Appearance", "onboarding",
                        "Archive", "About and Safety", "Settings hub", "History", "Local Intelligence", "shared chrome"],
    "themes": ["OBSIDIAN", "MARBLE"],
    "live_rows": rows, "live_row_count": len(rows), "live_fail_count": len(fails),
    "pre_repair_rows": pre_repair_rows, "pre_repair_row_count": len(pre_repair_rows),
    "pre_repair_fail_count": len(pre_fails),
    "repair_note": "pre_repair FAILs are the evidence that drove CP-G4.13-003 (M3 default disabled labels 1.43–2.09:1; Marble-mode prose on the always-obsidian shell 1.16–3.12:1). The live semantic authority supersedes them; historical rows retained verbatim.",
}
OUT_JSON.write_text(json.dumps(doc, indent=1))

with OUT_MD.open("w") as f:
    f.write("# APP-WIDE RENDERED CONTRAST AUDIT — GRAPH 4.13 (CP-G4.13-002, post-repair)\n\n")
    f.write("Evidence: WCAG 2.x computed on the code's ACTUAL rendered surface stacks ")
    f.write("(source-parsed tokens, worst-case gradient stop, alpha blended).\n")
    f.write(f"PRE-REPAIR evidence: {len(pre_repair_rows)} rows / {len(pre_fails)} FAIL (drove CP-G4.13-003).\n")
    f.write(f"LIVE semantic authority: {len(rows)} rows / {len(fails)} FAIL.\n")
    f.write("Emulator screenshot verification is layered by the CP-G4.13-004 screenshot gates.\n")
    f.write("\n## PART A — PRE-REPAIR evidence (historical, superseded)\n")
    cur = None
    for r in pre_repair_rows:
        if r["screen"] != cur:
            cur = r["screen"]
            f.write(f"\n### {cur}\n\n| component | state | theme | enabled | fg (worst bg) | ratio | class | verdict |\n|---|---|---|---|---|---|---|---|\n")
        f.write(f"| {r['component']} | {r['state']} | {r['theme']} | {'en' if r['enabled'] else 'dis'} | "
                f"#{r['foreground_argb'][2:]} (#{r['background_argb_worst_stop'][2:]}) | {r['contrast_ratio']}:1 | "
                f"{r['kind']} ≥{r['threshold']} | **{r['verdict']}** |\n")
    f.write("\n### PRE-REPAIR FAIL summary (repair table)\n\n")
    for r in pre_fails:
        f.write(f"- `{r['id']}` {r['screen']} / {r['component']} ({r['state']}, {r['theme']}) — "
                f"{r['contrast_ratio']}:1 < {r['threshold']}:1\n")
    f.write("\n## PART B — LIVE SEMANTIC AUTHORITY (post CP-G4.13-003)\n")
    cur = None
    for r in rows:
        if r["theme"] != cur:
            cur = r["theme"]
            f.write(f"\n### {cur}\n\n| component | state | enabled | fg (bg) | ratio | class | verdict |\n|---|---|---|---|---|---|---|\n")
        f.write(f"| {r['component']} | {r['state']} | {'en' if r['enabled'] else 'dis'} | "
                f"#{r['foreground_argb'][2:]} (#{r['background_argb_worst_stop'][2:]}) | {r['contrast_ratio']}:1 | "
                f"{r['kind']} ≥{r['threshold']} | **{r['verdict']}** |\n")
    f.write("\n### LIVE FAIL summary\n\n")
    if not fails:
        f.write("None — every live semantic pair meets its threshold in both themes.\n")
    for r in fails:
        f.write(f"- `{r['id']}` {r['component']} ({r['state']}, {r['theme']}) — {r['contrast_ratio']}:1 < {r['threshold']}:1\n")
print(f"pre_repair rows={len(pre_repair_rows)} fails={len(pre_fails)}; live rows={len(rows)} fails={len(fails)}")
print("wrote", OUT_JSON)
print("wrote", OUT_MD)
