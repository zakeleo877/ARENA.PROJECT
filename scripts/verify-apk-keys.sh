#!/usr/bin/env bash
# Inspects the keys that were compiled into the two release packages.
#
# The publishable key is public by design, so finding it in an APK is expected.
# What must never happen is a high-privilege key inside the package: a secret
# key bypasses every row level security policy, and a package is readable by
# anyone who downloads it.
#
# The scan reads every entry of the archive. A search over the raw file finds
# nothing, because the classes are deflated inside the zip; the first version
# of this script reported a missing key for that reason.
#
# Usage: verify-apk-keys.sh <apk>...
set -uo pipefail

if [ "$#" -eq 0 ]; then
  printf 'FAIL: no package was given\n'
  exit 1
fi

python3 - "$@" <<'PY'
import re
import sys
import zipfile

SECRET = re.compile(rb'sb_secret_[A-Za-z0-9_-]{10,}')
SECRET_CLAIM = re.compile(rb'"(?:role|aud)"\s*:\s*"?service')
PUBLISHABLE = re.compile(rb'sb_publishable_[A-Za-z0-9_-]{10,}')
PROJECT = re.compile(rb'https://[a-z0-9]+\.supabase\.co')
PLACEHOLDER = re.compile(rb'placeholder\.invalid')

passed = 0
failed = 0


def pass_(message):
    global passed
    passed += 1
    print(f'PASS: {message}')


def fail(message):
    global failed
    failed += 1
    print(f'FAIL: {message}')


for apk in sys.argv[1:]:
    name = apk.rsplit('/', 1)[-1]
    try:
        archive = zipfile.ZipFile(apk)
    except Exception as error:
        fail(f'{name} could not be opened ({error})')
        continue

    found_secret = found_claim = found_publishable = found_project = 0
    found_placeholder = 0
    with archive:
        for entry in archive.namelist():
            try:
                data = archive.read(entry)
            except Exception:
                continue
            found_secret += len(SECRET.findall(data))
            found_claim += len(SECRET_CLAIM.findall(data))
            found_publishable += len(PUBLISHABLE.findall(data))
            found_project += len(PROJECT.findall(data))
            found_placeholder += len(PLACEHOLDER.findall(data))

    if found_secret:
        fail(f'{name} contains a secret key ({found_secret} occurrences)')
    else:
        pass_(f'{name} contains no secret key')

    if found_claim:
        fail(f'{name} contains a high-privilege JWT claim ({found_claim} occurrences)')
    else:
        pass_(f'{name} contains no high-privilege JWT claim')

    if found_publishable:
        pass_(f'{name} carries a publishable key')
    else:
        fail(f'{name} carries no publishable key, so it cannot reach the project')

    if found_project:
        pass_(f'{name} points at a Supabase project')
    elif found_placeholder:
        print(f'NOTE: {name} was built with the placeholder address.')
        print('      That is expected for the emulator run; it is not distributable.')
        pass_(f'{name} has an address, but it is the placeholder')
    else:
        fail(f'{name} carries no Supabase project address')

print()
print(f'{passed} passed, {failed} failed.')
sys.exit(0 if failed == 0 else 1)
PY
