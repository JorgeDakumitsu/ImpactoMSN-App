#!/usr/bin/env python3
"""Check Android string-resource coverage and common hardcoded Compose UI copy."""
from __future__ import annotations

import re
import sys
import xml.etree.ElementTree as ET
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
RES = ROOT / "app" / "src" / "main" / "res"
JAVA = ROOT / "app" / "src" / "main" / "java"

PLACEHOLDER = re.compile(r"%(?:\d+\$)?[sdif]")
UI_LITERAL_PATTERNS = {
    "Text()": re.compile(r'Text\(\s*"((?:\\.|[^"\\])*)"'),
    "text argument": re.compile(r'text\s*=\s*"((?:\\.|[^"\\])*)"'),
    "contentDescription": re.compile(r'contentDescription\s*=\s*"((?:\\.|[^"\\])*)"'),
    "placeholder": re.compile(r'placeholder\s*=\s*"((?:\\.|[^"\\])*)"'),
    "title": re.compile(r'title\s*=\s*"((?:\\.|[^"\\])*)"'),
}


def resource_map(path: Path) -> dict[str, tuple[str, str]]:
    root = ET.parse(path).getroot()
    result: dict[str, tuple[str, str]] = {}
    for element in root:
        if element.tag in {"string", "plurals", "string-array"}:
            name = element.attrib.get("name")
            if name:
                text = " ".join((child.text or "") for child in element) if element.tag != "string" else (element.text or "")
                result[name] = (element.tag, text)
    return result


def main() -> int:
    base_path = RES / "values" / "strings.xml"
    base = resource_map(base_path)
    errors: list[str] = []

    for path in sorted(RES.glob("values*/strings.xml")):
        locale = path.parent.name
        localized = resource_map(path)
        missing = sorted(set(base) - set(localized))
        if missing:
            errors.append(f"{locale}: missing keys: {', '.join(missing)}")
        for key, (kind, base_text) in base.items():
            if key not in localized or kind != "string":
                continue
            localized_text = localized[key][1]
            if sorted(PLACEHOLDER.findall(base_text)) != sorted(PLACEHOLDER.findall(localized_text)):
                errors.append(f"{locale}: placeholder mismatch for {key}")

    for path in sorted(JAVA.rglob("*.kt")):
        relative = path.relative_to(JAVA).as_posix()
        if "/data/" in f"/{relative}" or "/protocol/" in f"/{relative}":
            continue
        source = path.read_text(encoding="utf-8")
        for label, pattern in UI_LITERAL_PATTERNS.items():
            for match in pattern.finditer(source):
                value = match.group(1)
                technical = (
                    value.startswith(("http://", "https://", "application/", "#"))
                    or value in {"ENTER", "HIl", "MSN Messenger 8.5"}
                    or relative.endswith("LoginActivity.kt") and "serverConfig" in value
                    or re.fullmatch(r"\$\{[^}]+\}\s*/\s*\$\{[^}]+\}", value)
                    or re.fullmatch(r"\(\s*\$[A-Za-z_][A-Za-z0-9_]*\s*\)", value)
                )
                if re.search(r"[A-Za-z]{2}", value) and not technical:
                    line = source.count("\n", 0, match.start()) + 1
                    errors.append(f"{relative}:{line}: hardcoded {label}: {value}")

    if errors:
        print("Android localization check failed:")
        print("\n".join(f" - {error}" for error in errors))
        return 1

    print(f"Localization check passed: {len(base)} resource keys covered in all locale catalogs; no direct Compose UI literals found in audited patterns.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
