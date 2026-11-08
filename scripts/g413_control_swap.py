#!/usr/bin/env python3
"""GRAPH 4.13 §5 — swap Material control call sites to the §5-contract wrappers."""
import re
from pathlib import Path

SCR = Path("/home/z/my-project/hermes-voice-console-android/app/src/main/java/com/hermestower/voiceconsole/ui/screens")
IMPS = {
    "HermesFilledButton": "import com.hermestower.voiceconsole.ui.components.HermesFilledButton",
    "HermesOutlinedButton": "import com.hermestower.voiceconsole.ui.components.HermesOutlinedButton",
    "HermesFilterChip": "import com.hermestower.voiceconsole.ui.components.HermesFilterChip",
    "HermesSwitch": "import com.hermestower.voiceconsole.ui.components.HermesSwitch",
    "HermesOutlinedField": "import com.hermestower.voiceconsole.ui.components.HermesOutlinedField",
}
SWAPS = [
    (r"(?<![\w])FilterChip\(", "HermesFilterChip("),
    (r"(?<![\w])Switch\(", "HermesSwitch("),
    (r"(?<![\w])OutlinedTextField\(", "HermesOutlinedField("),
    (r"(?<![\w])Button\(", "HermesFilledButton("),
    (r"(?<![\w])OutlinedButton\(", "HermesOutlinedButton("),
]
for f in sorted(SCR.glob("*.kt")):
    if f.name in {"ConsoleScreen.kt", "ConsoleControlSemantics.kt"}:
        continue
    txt = f.read_text(); orig = txt
    used = set()
    for pat, rep in SWAPS:
        if re.search(pat, txt):
            used.add(rep.split("(")[0])
            txt = re.sub(pat, rep, txt)
    if txt != orig:
        lines = txt.split("\n")
        last_import = max(i for i, l in enumerate(lines) if l.startswith("import "))
        for name in sorted(used):
            if IMPS[name] not in txt:
                lines.insert(last_import + 1, IMPS[name])
                last_import += 1
        f.write_text("\n".join(lines))
        print("swapped", f.name, sorted(used))
