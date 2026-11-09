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
    # align/matchParentSize and weight are BoxScope/RowScope members, not imports.
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
        imported = any(
            f"androidx.compose.{group}.{name}" in imports
            for group in ("foundation.layout", "ui.draw")
        )
        if name in LAYOUT_EXTENSIONS and not imported:
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


def check_shared_element(path: pathlib.Path, code: str) -> None:
    """The sharedElement modifier names its first parameter sharedContentState.

    An older signature used "state"; passing that name now fails to compile.
    """
    for match in re.finditer(r"\.sharedElement\s*\(", code):
        start = match.end()
        depth = 1
        index = start
        while index < len(code) and depth:
            if code[index] == "(":
                depth += 1
            elif code[index] == ")":
                depth -= 1
            index += 1
        arguments = code[start : index - 1]
        if "state" in arguments and "sharedContentState" not in arguments:
            line = code[: match.start()].count("\n") + 1
            errors.append(
                f"{path.relative_to(ROOT)}:{line}: sharedElement expects the "
                f"parameter sharedContentState, not state"
            )


def check_shared_content_state_import(path: pathlib.Path, imports: set[str]) -> None:
    """rememberSharedContentState is a member of SharedTransitionScope."""
    if "androidx.compose.animation.core.rememberSharedContentState" in imports:
        errors.append(
            f"{path.relative_to(ROOT)}: rememberSharedContentState is a member of "
            f"SharedTransitionScope and must not be imported from "
            f"androidx.compose.animation.core"
        )


def check_predictive_back_collect(path: pathlib.Path, code: str) -> None:
    """The lint rule NoCollectCallFound requires the progress flow to be collected.

    PredictiveBackHandler hands a Flow to a suspending lambda. Without a
    collect call the whole block runs when the gesture starts instead of when
    it completes, which also trips the lint rule that fails the build.
    """
    for match in re.finditer(r"PredictiveBackHandler\s*(\(|\{)", code):
        opener = match.group(1)
        closer = ")" if opener == "(" else "}"
        start = match.end()
        depth = 1
        index = start
        while index < len(code) and depth:
            if code[index] in "({":
                depth += 1
            elif code[index] in ")}":
                depth -= 1
            index += 1
        arguments = code[start : index - 1]
        if not re.search(r"\.collect\s*(\(|\{)", arguments):
            line = code[: match.start()].count("\n") + 1
            errors.append(
                f"{path.relative_to(ROOT)}:{line}: PredictiveBackHandler must "
                f"collect the progress flow, otherwise the block runs when the "
                f"gesture starts instead of when it completes"
            )

def check_required_imports(path: pathlib.Path, code: str, imports: set[str]) -> None:
    """Report a symbol that is used but whose import is missing.

    The Kotlin compiler only reports this after the whole module is compiled,
    which costs four minutes in the CI. The list below holds the symbols that
    were actually missing during this work.
    """
    required = {
        "mutableStateOf": "androidx.compose.runtime.mutableStateOf",
        "remember": "androidx.compose.runtime.remember",
        "setValue": "androidx.compose.runtime.setValue",
        "mutableIntStateOf": "androidx.compose.runtime.mutableIntStateOf",
        "mutableFloatStateOf": "androidx.compose.runtime.mutableFloatStateOf",
        "mutableStateListOf": "androidx.compose.runtime.mutableStateListOf",
        "collectAsState": "androidx.compose.runtime.collectAsState",
        "LaunchedEffect": "androidx.compose.runtime.LaunchedEffect",
        "rememberCoroutineScope": "androidx.compose.runtime.rememberCoroutineScope",
    }
    # The property delegate operators only need an import when they are used
    # with the `by` keyword. A map lookup is a different function entirely.
    delegates = {
        "getValue": "androidx.compose.runtime.getValue",
        "setValue": "androidx.compose.runtime.setValue",
    }
    for symbol, package in required.items():
        # The imports hold the full paths, so the symbol is matched against the
        # last part of every imported path.
        imported = any(path.rsplit(".", 1)[-1] == symbol for path in imports)
        if not imported:
            # The symbol has to be used before the import can be missing.
            pattern = r"\b" + re.escape(symbol) + r"\s*[({<]"
            if not re.search(pattern, code):
                continue
            if re.search(
                r"^\s*(private\s+|internal\s+)?(fun|val|var)\s+\w*\s*=\s*" + re.escape(symbol),
                code,
                re.M,
            ):
                continue
            line = next(
                (
                    code[: match.start()].count("\n") + 1
                    for match in re.finditer(pattern, code)
                ),
                1,
            )
            errors.append(
                f"{path.relative_to(ROOT)}:{line}: {symbol} is used but "
                f"{package} is not imported"
            )

    for symbol, package in delegates.items():
        imported = any(path.rsplit(".", 1)[-1] == symbol for path in imports)
        if imported:
            continue
        # The delegate operator is only needed when a property is declared
        # with `by` and the delegate is not a call that returns a state.
        pattern = r"\bby\s+(?:remember|rememberSaveable)\s*\{"
        if not re.search(pattern, code):
            continue
        # The delegate of `by remember { mutableStateOf(...) }` is the state
        # itself, so the operator is needed.
        if not re.search(r"\bby\s+(?:remember|rememberSaveable)\s*\{\s*mutable", code):
            continue
        match = re.search(pattern, code)
        line = code[: match.start()].count("\n") + 1
        errors.append(
            f"{path.relative_to(ROOT)}:{line}: {symbol} is needed for the "
            f"property delegate but {package} is not imported"
        )


def check_compose_test_api(path: pathlib.Path, code: str) -> None:
    """Catch the Compose test helpers that do not exist on the rule type.

    `onAllNodesWithText` is not a member of ComposeContentTestRule, it lives
    on SemanticsNodeInteractionsProvider. The compiler only reports this after
    the whole module is compiled, so the static check reports it in seconds.
    """
    for match in re.finditer(r"\b\w+\.onAllNodesWithText\(", code):
        line = code[: match.start()].count("\n") + 1
        errors.append(
            f"{path.relative_to(ROOT)}:{line}: onAllNodesWithText is not a member "
            f"of the compose rule, use onAllNodes(hasText(...)) instead"
        )

    # The body of a Ktor request is an OutgoingContent, and its toString is
    # not the payload. The payload is read through the TextContent.
    for match in re.finditer(r"request\.body\.toString\(\)", code):
        line = code[: match.start()].count("\n") + 1
        errors.append(
            f"{path.relative_to(ROOT)}:{line}: the body of a request is not "
            f"read with toString, cast it to TextContent and read the text"
        )



def check_hilt_view_models(paths: list[pathlib.Path]) -> None:
    """Catch a view model that the framework cannot create.

    `hiltViewModel()` asks the factory of the activity for the view model.
    The default factory of the framework looks for a constructor without
    arguments with the reflection, so a view model that has arguments can
    only be created when it carries the Hilt annotation. The device reported
    the missing constructor, and this check reports it in seconds.
    """
    requested: dict[str, pathlib.Path] = {}
    for path in paths:
        source = path.read_text(encoding="utf-8")
        for match in re.finditer(r"hiltViewModel\s*<\s*([A-Za-z_][\w.]*)\s*>", source):
            requested[match.group(1).rsplit(".", 1)[-1]] = path
        for match in re.finditer(r"hiltViewModel\s*\(\s*\)\s*(?:\)|as\s+[A-Za-z_][\w.]*)", source):
            pass

    for path in paths:
        source = path.read_text(encoding="utf-8")
        for match in re.finditer(
            r"hiltViewModel\s*<\s*([A-Za-z_][\w.]*)\s*>", source
        ):
            name = match.group(1).rsplit(".", 1)[-1]
            requested[name] = path

    # The composables that ask for the view model without a type, the type is
    # taken from the parameter of the composable.
    for path in paths:
        source = path.read_text(encoding="utf-8")
        for match in re.finditer(
            r"fun\s+\w+\s*\([^)]*viewModel\s*:\s*([A-Za-z_][\w.]*)\s*=\s*hiltViewModel\(\)",
            source,
        ):
            name = match.group(1).rsplit(".", 1)[-1]
            requested[name] = path

    if not requested:
        return

    declarations: dict[str, pathlib.Path] = {}
    for path in paths:
        source = path.read_text(encoding="utf-8")
        for match in re.finditer(
            r"(?m)^(?:@\w+(?:\([^\n]*\))?\s*)*class\s+([A-Za-z_]\w*)\b", source
        ):
            declarations[match.group(1)] = path

    for name, requester in sorted(requested.items()):
        declaration = declarations.get(name)
        if declaration is None:
            continue
        source = declaration.read_text(encoding="utf-8")
        if "@HiltViewModel" not in source:
            errors.append(
                f"{requester.relative_to(ROOT)}: {name} is requested with "
                f"hiltViewModel() but {declaration.relative_to(ROOT)} does not "
                f"carry the HiltViewModel annotation"
            )


# Extension functions and DSL receivers that a fully qualified name cannot
# resolve. Writing `io.ktor.serialization.kotlinx.json.json(...)` fails at
# compile time even though the path looks right, because the function is
# declared on the client config receiver and has to be imported.
EXTENSION_FUNCTIONS = {
    "io.ktor.serialization.kotlinx.json.json": "json",
    "io.ktor.client.request.header": "header",
    "io.ktor.client.request.setBody": "setBody",
}


def check_extension_functions(path: Path, source: str) -> list[str]:
    problems: list[str] = []
    imports = set(re.findall(r"^import\s+([\w.]+)", source, re.M))
    lines = source.splitlines()
    for dotted, name in EXTENSION_FUNCTIONS.items():
        for number, line in enumerate(lines, 1):
            stripped = line.strip()
            if stripped.startswith("import") or stripped.startswith("//"):
                continue
            # A fully qualified call of an extension function never resolves.
            if re.search(rf"\b{re.escape(dotted)}\s*\(", line):
                problems.append(
                    f"{path}:{number}: {dotted} is an extension function; import "
                    f"`{dotted}` and call `{name}(...)` instead"
                )
                continue
            # An argument that carries a package prefix instead of an import,
            # for example `json(io.ktor....DefaultJson)`, compiles, but it is a
            # sign that the import was forgotten elsewhere in the file.
            if re.search(rf"\b{re.escape(name)}\s*\(\s*io\.ktor\.", line):
                problems.append(
                    f"{path}:{number}: `{name}(...)` takes a named argument; add the "
                    f"import and pass the simple name"
                )
                continue
            if re.search(rf"(?<![\w.]){re.escape(name)}\s*\(", line) and dotted not in imports:
                # Only report when the call looks like the DSL use, which is
                # `json(config)` or `json {`, not a member of another type.
                if re.search(rf"(?<![\w.]){re.escape(name)}\s*(\(|\{{)", stripped):
                    if dotted not in imports:
                        problems.append(
                            f"{path}:{number}: `{name}` is used without "
                            f"`import {dotted}`"
                        )
    return problems


def check_gradle_scripts(root: Path) -> list[str]:
    """Catches the mistakes that only appear when Gradle compiles a script.

    A Kotlin build script is compiled before any task runs, so a script error
    stops the whole pipeline: the unit tests, the lint and every acceptance
    check are skipped. These patterns each cost a full CI cycle to discover.
    """
    problems: list[str] = []
    for path in sorted(root.rglob("build.gradle.kts")):
        text = path.read_text(encoding="utf-8", errors="replace")
        lines = text.splitlines()
        uses_base64 = "Base64.getUrlDecoder" in text
        imports_base64 = any(
            line.strip() in (
                "import java.util.Base64",
                "import java.util.*",
                "import java.util.Base64.*",
            )
            for line in lines
        )
        if uses_base64 and not imports_base64:
            # `java` in a build script resolves to the Gradle extension, so the
            # fully qualified name does not work either.
            problems.append(
                f"{path}: Base64 is used without `import java.util.Base64`; "
                "the fully qualified name does not compile in a build script"
            )
        for number, line in enumerate(lines, 1):
            stripped = line.strip()
            if stripped.startswith("//"):
                continue
            if stripped.startswith("compileSdkVersion("):
                problems.append(
                    f"{path}:{number}: compileSdkVersion is deprecated and Kotlin "
                    "treats the warning as an error; use the compileSdk block"
                )
            if "java.util." in stripped and "import" not in stripped:
                problems.append(
                    f"{path}:{number}: a fully qualified java.util name does not "
                    "compile in a build script; import the class instead"
                )
    return problems


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
        check_shared_element(path, code)
        check_shared_content_state_import(path, imports)
        check_predictive_back_collect(path, code)
        check_compose_test_api(path, code)
        check_required_imports(path, code, imports)

    check_hilt_view_models(files)
    errors.extend(check_gradle_scripts(ROOT))
    for path in files:
        errors.extend(check_extension_functions(path, path.read_text(encoding="utf-8")))

    if errors:
        print("Static reference check failed:\n")
        for error in errors:
            print(f"  {error}")
        return 1

    print(f"PASS: every checked Kotlin reference resolves ({len(files)} files scanned).")
    return 0


if __name__ == "__main__":
    sys.exit(main())
