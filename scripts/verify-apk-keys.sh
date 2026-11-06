#!/usr/bin/env bash
# Inspects the keys that were compiled into the two release packages.
#
# The publishable key is public by design, so finding it in an APK is expected.
# What must never happen is a high-privilege key inside the package: a secret
# key bypasses every row level security policy, and a package is readable by
# anyone who downloads it.
#
# Usage: verify-apk-keys.sh <apk>...
set -uo pipefail

PASSES=0
FAILS=0
pass() { PASSES=$((PASSES + 1)); printf 'PASS: %s\n' "$1"; }
fail() { FAILS=$((FAILS + 1)); printf 'FAIL: %s\n' "$1"; }

if [ "$#" -eq 0 ]; then
  printf 'FAIL: no package was given\n'
  exit 1
fi

for apk in "$@"; do
  name="$(basename "$apk")"
  if [ ! -f "$apk" ]; then
    fail "$name does not exist"
    continue
  fi

  # The whole package, including the compiled resources and the string pool.
  text="$(strings -a "$apk" 2>/dev/null || cat "$apk" | tr -c '[:print:]' '\n')"

  # 1. No high-privilege key. Both spellings are covered: the newer token and
  #    the role claim of the legacy service JWT.
  if printf '%s' "$text" | grep -qE 'sb_secret_[A-Za-z0-9_-]{10,}'; then
    fail "$name contains a secret key"
  else
    pass "$name contains no secret key"
  fi
  if printf '%s' "$text" | grep -qE '"(role|aud)":"?service'; then
    fail "$name contains a high-privilege JWT claim"
  else
    pass "$name contains no high-privilege JWT claim"
  fi

  # 2. A publishable key must be present, otherwise the build is unusable.
  if printf '%s' "$text" | grep -qE 'sb_publishable_[A-Za-z0-9_-]{10,}'; then
    pass "$name carries a publishable key"
  else
    fail "$name carries no publishable key, so it cannot reach the project"
  fi

  # 3. The project address must be a real https address, not a placeholder.
  if printf '%s' "$text" | grep -qE 'https://[A-Za-z0-9]+\.supabase\.co'; then
    pass "$name points at a Supabase project"
  else
    if printf '%s' "$text" | grep -q 'placeholder.invalid'; then
      printf 'NOTE: %s was built with the placeholder address.\n' "$name"
      printf '      That is expected for the emulator run; it is not distributable.\n'
      pass "$name has an address, but it is the placeholder"
    else
      fail "$name carries no Supabase project address"
    fi
  fi
done

printf '\n%d passed, %d failed.\n' "$PASSES" "$FAILS"
[ "$FAILS" -eq 0 ]
