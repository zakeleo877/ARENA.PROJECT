#!/usr/bin/env bash
# Acceptance check 10: the release signing key must be permanent.
#
# The workflow can fall back to a throwaway key when the production secrets
# are missing. A throwaway key is useless for a real release: the next run
# signs with a different certificate and the installed application cannot be
# updated. This script therefore reports which key was used and fails when
# the result would not be publishable.
#
# Usage: verify-signing.sh <mode> <certificate-sha256>
#   mode          production or throwaway
#   certificate   the SHA-256 fingerprint of the signing certificate
set -euo pipefail

MODE="${1:-unknown}"
FINGERPRINT="${2:-unknown}"

if [ "$MODE" = "production" ]; then
  if [ -z "$FINGERPRINT" ] || [ "$FINGERPRINT" = "unknown" ]; then
    printf 'FAIL: 10. the production certificate fingerprint could not be read\n'
    exit 1
  fi
  printf 'PASS: 10. the release APK is signed with the permanent production key\n'
  printf '      certificate SHA-256: %s\n' "$FINGERPRINT"
  exit 0
fi

if [ "$MODE" = "throwaway" ]; then
  printf 'FAIL: 10. the release APK is signed with a throwaway key, not a permanent one\n'
  printf '      The key was generated inside the runner and is gone after the job,\n'
  printf '      so the next build produces a different certificate and the\n'
  printf '      installed application cannot be updated. Configure the secrets\n'
  printf '      KEYSTORE_BASE64, KEYSTORE_PASSWORD, KEY_ALIAS and KEY_PASSWORD\n'
  printf '      before publishing.\n'
  exit 1
fi

printf 'FAIL: 10. the signing mode is unknown (%s)\n' "$MODE"
exit 1
