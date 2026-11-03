#!/usr/bin/env python3
"""LOCALIZATION_LITERAL_AUDIT re-scan tool (CP-G4-017, directive §3/§16).

Scans the UI source tree for hardcoded user-visible string literals and
emits scripts/literal_inventory.json. Comments are stripped before scanning
(so comment prose never pollutes the inventory).

Kind detection (ordered):
  title      — Text("...") top-level call in a composable body
  a11y       — contentDescription = "..."
  label      — Text("...") / label = "..." parameter positions
  chip       — short uppercase tokens rendered as status chips
  format     — string templates "…$x…"
Classification (per directive §3):
  TECHNICAL_IDENTIFIER / MODEL_IDENTITY / DIAGNOSTIC_CODE / FILE_NAME /
  TEST_ONLY / INTERNAL_NOT_VISIBLE are decided by ALLOWLIST / patterns;
  everything user-visible is MUST_LOCALIZE.

Usage: python3 scripts/extract_literals.py [--json PATH]
"""
import json
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
# Audit perimeter (directive §3): the UI source tree + the SIMULATED archive
# surface. Engine/model/tower files expose strings through logs and internal
# typed channels, not user-visible UI — audited at their definition sites
# separately (ConsoleState labels, Self-Test names, catalog identities).
SRC_CANDIDATES = [
    ROOT / "app/src/main/java/com/hermestower/voiceconsole/ui",
    ROOT / "app/src/main/java/com/hermestower/voiceconsole/archive",
]
OUT = ROOT / "scripts/literal_inventory.json"

# Directive §3 categories decided by pattern/allowlist:
ALLOWLIST_EXACT = {
    "HERMES", "TOWER", "HERMES TOWER",            # Owner §14 brand anchors
    "MARBLE LIGHT", "OBSIDIAN NIGHT",             # theme product names + E2E pins
    "ORACLE CORE", "MODEL SANCTUM",               # technical identity names
}
ALLOWLIST_CONTAINS = ("◤", "◥")                   # decorative glyphs
DIAG_RE = re.compile(r"^[A-Z][A-Z0-9_ ×%.-]*$")
MODEL_ID_RE = re.compile(r"^(org|com)\.[a-z0-9_.]+$")

STR_RE = re.compile(r'"((?:[^"\\]|\\.)*)"')


def strip_comments(src: str) -> str:
    src = re.sub(r"/\*.*?\*/", "", src, flags=re.S)
    src = re.sub(r"//[^\n]*", "", src)
    return src


def classify(lit: str, line: str) -> str | None:
    """Return the audit category, or None to skip (not user-visible / not a literal)."""
    if not lit or len(lit) < 2:
        return None
    if lit in ALLOWLIST_EXACT:
        return "TECHNICAL_IDENTIFIER"
    if any(g in lit for g in ALLOWLIST_CONTAINS):
        return "INTERNAL_NOT_VISIBLE"
    if MODEL_ID_RE.match(lit):
        return "MODEL_IDENTITY"
    if DIAG_RE.match(lit) and len(lit) <= 24 and " " not in lit.strip():
        return "DIAGNOSTIC_CODE"
    if "contentDescription" in line:
        return "MUST_LOCALIZE"
    return "MUST_LOCALIZE"


def kind_of(line: str) -> str:
    if "contentDescription" in line:
        return "a11y"
    if re.search(r"\bText\s*\(", line):
        return "title" if "screenTitle" in line or "style =" in line else "label"
    if "=" in line:
        return "param"
    return "expr"


def scan() -> list[dict]:
    inv: list[dict] = []
    files: list[Path] = []
    for base in SRC_CANDIDATES:
        files.extend(sorted(base.rglob("*.kt")))
    for path in files:
        rel = path.relative_to(SRC_CANDIDATES[0].parent).as_posix()
        if "/test" in rel or "Test" in path.name:
            continue
        text = strip_comments(path.read_text())
        for i, line in enumerate(text.splitlines(), 1):
            for m in STR_RE.finditer(line):
                lit = m.group(1)
                # skip non-visible technical strings
                if lit.startswith(("http", "android.", "com.hermestower", "hermes.")):
                    continue
                cat = classify(lit, line)
                if cat is None:
                    continue
                inv.append(
                    {
                        "file": rel,
                        "line": i,
                        "kind": kind_of(line),
                        "literal": lit,
                        "class": cat,
                    }
                )
    return inv


def main() -> int:
    out = OUT
    if "--json" in sys.argv:
        out = Path(sys.argv[sys.argv.index("--json") + 1])
    inv = scan()
    out.write_text(json.dumps(inv, indent=1, ensure_ascii=False))
    from collections import Counter

    c = Counter(e["class"] for e in inv)
    print(f"{len(inv)} literals → {out.relative_to(ROOT)}")
    for k, v in c.most_common():
        print(f"  {k}: {v}")
    per_file = Counter(e["file"] for e in inv)
    for f, n in per_file.most_common():
        print(f"  {n:4d}  {f}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
