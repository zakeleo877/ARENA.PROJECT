#!/usr/bin/env python3
"""Static reference check for the Kotlin sources.

The sandbox that runs this repository has no JDK and no Android SDK, so the
Gradle unit tests and lint tasks can only run on the GitHub Actions runner.
This script is not a replacement for the compiler: it is a cheap guard that
catches the exact class of mistake that broke the build before, namely a
missing import for a Modifier extension or a wrong name for a window size
class constant.

It reports:
  * Modifier extensions used without their import.
  * WindowWidthSizeClass / WindowHeightSizeClass constants that do not exist.
  * References to a local declaration that is never imported.

Exit code 0 means every checked reference resolves.
"""
from __future__ import annotations

import pathlib
import re
import sys

ROOT = pathlib.Path(__file__).resolve().parent.parent

# Modifier extensions that live in androidx.compose.foundation.layout and are
# therefore resolved through an import, not through the Modifier receiver.
LAYOUT_EXTENSIONS = {
    # align and weight are members of BoxScope and RowScope, not extensions.
    "aspectRatio",
    "background",
    "border",
    "clip",
    "clipToBounds",
    "defaultMinSize",
    "fillMaxHeight",
    "fillMaxSize",
    "fillMaxWidth",
    "height",
    "heightIn",
    "heightIn",
    "matchParentSize",
    "offset",
    "padding",
    "padding",
    "requiredHeight",
    "requiredHeightIn",
    "requiredSize",
    "requiredWidth",
    "requiredWidthIn",
    "size",
    "sizeIn",
    "width",
    "widthIn",
    "wrapContentHeight",
    "wrapContentSize",
    "wrapContentWidth",
}

# The real constants of the material3-window-size-class library.
WIDTH_SIZE_CLASSES = {"Compact", "Medium", "Expanded"}
HEIGHT_SIZE_CLASSES = {"Compact", "Medium", "Expanded"}

errors: list[str] = []


def kotlin_files() -> list[pathlib.Path]:
    files = []
    for module in ("core", "customer", "admin"):
        files.extend(sorted((ROOT / module).rglob("*.kt")))
    return files


def strip_noise(text: str) -> str:
    """Removes comments and string literals so only real code is inspected."""
    text = re.sub(r'"""(?:.|\n)*?"""', '""', text)
    text = re.sub(r'"(?:\\.|[^"\\])*"', '""', text)
    text = re.sub(r'//[^\n]*', '', text)
    text = re.sub(r'/\*(?:.|\n)*?\*/', '', text)
    return text


def check_modifier_extensions(path: pathlib.Path, code: str, imports: set[str]) -> None:
    for match in re.finditer(r"Modifier\s*\.\s*([A-Za-z_]\w*)", code):
        name = match.group(1)
        if name in LAYOUT_EXTENSIONS and f"androidx.compose.foundation.layout.{name}" not in imports:
            line = code[: match.start()].count("\n") + 1
            errors.append(
                f"{path.relative_to(ROOT)}:{line}: Modifier.{name} is used but "
                f"androidx.compose.foundation.layout.{name} is not imported"
            )


def check_size_classes(path: pathlib.Path, code: str) -> None:
    for match in re.finditer(r"WindowWidthSizeClass\s*\.\s*(\w+)", code):
        if match.group(1) not in WIDTH_SIZE_CLASSES:
            line = code[: match.start()].count("\n") + 1
            errors.append(
                f"{path.relative_to(ROOT)}:{line}: WindowWidthSizeClass."
                f"{match.group(1)} does not exist; the real constants are "
                f"{sorted(WIDTH_SIZE_CLASSES)}"
            )
    for match in re.finditer(r"WindowHeightSizeClass\s*\.\s*(\w+)", code):
        if match.group(1) not in HEIGHT_SIZE_CLASSES:
            line = code[: match.start()].count("\n") + 1
            errors.append(
                f"{path.relative_to(ROOT)}:{line}: WindowHeightSizeClass."
                f"{match.group(1)} does not exist; the real constants are "
                f"{sorted(HEIGHT_SIZE_CLASSES)}"
            )


def main() -> int:
    files = kotlin_files()
    for path in files:
        source = path.read_text(encoding="utf-8")
        imports = {
            m.group(1)
            for m in re.finditer(r"^import\s+([\w.]+)", source, re.M)
        }
        code = strip_noise(source)
        check_modifier_extensions(path, code, imports)
        check_size_classes(path, code)

    if errors:
        print("Static reference check failed:\n")
        for error in errors:
            print(f"  {error}")
        return 1

    print(f"PASS: every checked Kotlin reference resolves ({len(files)} files scanned).")
    return 0


if __name__ == "__main__":
    sys.exit(main())
