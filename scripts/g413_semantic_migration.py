#!/usr/bin/env python3
"""GRAPH 4.13 §6 — migrate screen prose tokens to the semantic authority.
Excludes the Console identity stage (monumental, always obsidian) and files
without a HermesColors accessor. Accent headings (gold/bronze) stay per §24."""
import re
from pathlib import Path

UI = Path("/home/z/my-project/hermes-voice-console-android/app/src/main/java/com/hermestower/voiceconsole")
EXCLUDE = {"ConsoleScreen.kt", "ConsoleControlSemantics.kt", "FirstRunRecoveryDialog.kt"}
RENAMES = [
    (r"\bc\.inkSecondary\b", "c.textSecondary"),
    (r"\bc\.ink\b", "c.textPrimary"),
    (r"\bc\.warn\b", "c.textWarning"),
    (r"\bc\.ok\b", "c.textSuccess"),
    (r"\bc\.energy\b", "c.textInfo"),
    (r"\bc\.danger\b", "c.textError"),
    (r"\bhermesColors\.inkSecondary\b", "hermesColors.textSecondary"),
    (r"\bhermesColors\.ink\b", "hermesColors.textPrimary"),
    (r"\bhermesColors\.warn\b", "hermesColors.textWarning"),
    (r"\bhermesColors\.ok\b", "hermesColors.textSuccess"),
    (r"\bhermesColors\.energy\b", "hermesColors.textInfo"),
    (r"\bhermesColors\.danger\b", "hermesColors.textError"),
]
# m.inkDim in screens → tertiary (files must define c)
DIM = (r"\bm\.inkDim\b", "c.textTertiaryReadable")

for f in sorted((UI / "ui" / "screens").glob("*.kt")):
    if f.name in EXCLUDE:
        continue
    txt = f.read_text()
    orig = txt
    defines_c = re.search(r"\bval c = hermesColors\b", txt) or "hermesColors." in txt
    if not defines_c:
        continue
    if not re.search(r"\bval c = hermesColors\b", txt) and "hermesColors." in txt:
        # normalize direct accessor usage to c for uniformity
        txt = re.sub(r"\bhermesColors\.", "c.", txt)
        if "val c = hermesColors" not in txt:
            # insert accessor after first `val ... =` line inside the screen fn — simplest: after imports' first composable fn signature is risky; append near first `rememberCoroutineScope` or `fun .*Screen(`
            m = re.search(r"(fun \w+\([^)]*\)\s*\{\n)", txt)
            if m:
                txt = txt[:m.end()] + "    val c = hermesColors\n" + txt[m.end():]
    if "val c = hermesColors" not in txt:
        m = re.search(r"(fun \w+\([^)]*\)\s*\{\n)", txt)
        if m:
            txt = txt[:m.end()] + "    val c = hermesColors\n" + txt[m.end():]
    for pat, rep in RENAMES:
        txt = re.sub(pat, rep, txt)
    txt = re.sub(DIM[0], DIM[1], txt)
    if txt != orig:
        f.write_text(txt)
        print("migrated", f.name)
