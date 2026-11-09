#!/usr/bin/env python3
"""Check publishable/secret token candidates without ever printing the tokens.

The allowlist contains hashes of the fixed, deliberately fake values used in
unit tests and placeholder builds. A live-looking candidate is a failure even
if it contains words such as 'test' or 'fake'. Only files Git could publish are
scanned. Ignored local files are also checked for the known disclosed
fingerprint; generated dependencies, VCS data and build directories are skipped.
"""
from __future__ import annotations

import hashlib
import os
import pathlib
import re
import subprocess
import sys

ROOT = pathlib.Path(__file__).resolve().parent.parent
TOKEN_PATTERN = re.compile(rb"sb_(?:secret|publishable)_[A-Za-z0-9_-]{10,}")

# Hashes of fixed placeholders, not real credentials. Exact hashes are much
# safer than accepting every token containing a word like 'test'.
KNOWN_TEST_DIGESTS = frozenset({
    "a3de465d0a95653cc622d39529fcfd0434bd02ae96b6b656ce36661589ec5b5f",
    "509adfe09f0c7b7a44a7c4cae17679294644ac31a84f455309ef18c41f4b4d90",
    "a3842bbcc242a4c8b7cd0f1e89b4ebbf34af9a661d6c374af65c51aabfaede35",
    "da102eac634e93bd3e3ad175873d918cce5dd1016860a82a85bc20bb519fc998",
})


def has_disclosed_fingerprint(digest: str) -> bool:
    # Only the known prefix and suffix of the previously disclosed credential's
    # fingerprint are retained, never the credential itself.
    return digest.startswith("eab0b605") and digest.endswith("3feb1")


def candidate_is_forbidden(token: bytes) -> bool:
    digest = hashlib.sha256(token).hexdigest()
    return has_disclosed_fingerprint(digest) or digest not in KNOWN_TEST_DIGESTS


def disclosed_lines(content: bytes) -> list[int]:
    return [content.count(b"\n", 0, match.start()) + 1
            for match in TOKEN_PATTERN.finditer(content)
            if has_disclosed_fingerprint(hashlib.sha256(match.group()).hexdigest())]


def scan(content: bytes) -> list[int]:
    return [content.count(b"\n", 0, match.start()) + 1
            for match in TOKEN_PATTERN.finditer(content)
            if candidate_is_forbidden(match.group())]


def main() -> int:
    result = subprocess.run(
        ["git", "ls-files", "-co", "--exclude-standard", "-z"],
        cwd=ROOT, check=True, capture_output=True,
    )
    errors = []
    scanned = set()
    for name in dict.fromkeys(result.stdout.split(b"\0")):
        if not name:
            continue
        path = ROOT / pathlib.Path(name.decode("utf-8", errors="surrogateescape"))
        if not path.is_file() or path.is_symlink():
            continue
        scanned.add(path)
        for line in scan(path.read_bytes()):
            errors.append(f"{path.relative_to(ROOT)}:{line}")

    # Also search ignored local files for the known disclosed fingerprint. An
    # ignored configuration file must not hide this particular credential.
    # Exclude only VCS, dependencies, caches, and generated build output.
    excluded = {".git", ".gradle", ".idea", ".kotlin", "node_modules", "build", "__pycache__"}
    for directory, subdirs, files in os.walk(ROOT):
        subdirs[:] = [name for name in subdirs if name not in excluded]
        for filename in files:
            path = pathlib.Path(directory) / filename
            if path in scanned or path.is_symlink() or not path.is_file():
                continue
            for line in disclosed_lines(path.read_bytes()):
                errors.append(f"{path.relative_to(ROOT)}:{line}")
    if errors:
        print("FAIL: Supabase key-like value found (value redacted):")
        print("\n".join(errors))
        return 1
    print("PASS: 12f. no unapproved Supabase keys or disclosed fingerprint found")
    return 0


if __name__ == "__main__":
    sys.exit(main())
