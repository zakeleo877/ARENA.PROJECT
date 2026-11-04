#!/usr/bin/env bash
# Repository checks 11-13 from the acceptance criteria.
set -euo pipefail

fail() { printf 'FAIL: %s\n' "$1"; exit 1; }
pass() { printf 'PASS: %s\n' "$1"; }

cd "$(dirname "$0")/.."

# 11. No delivery-related wording anywhere in the code.
# The check script itself is excluded, and so are the documents that legitimately
# discuss the wording rules.
DELIVERY_HITS="$(grep -rniwE 'shipping|delivery|توصيل|شحن' \
  --exclude-dir={.git,build,.gradle,node_modules,.idea} \
  --exclude='saeed-stores-prompt-v2_2.md' \
  --exclude='README.md' \
  --exclude='phase-3-plan.md' \
  --exclude='verify-repo.sh' \
  . || true)"
if [ -z "$DELIVERY_HITS" ]; then
  pass "11. no delivery wording in the project"
else
  printf '%s\n' "$DELIVERY_HITS"
  fail "11. delivery wording found"
fi

# 12. No secrets.
# The pattern is assembled from parts so that this script does not match itself.
SECRET_PATTERN="AIza[0-9A-Za-z_-]{35}|sk-[A-Za-z0-9]{20,}|service""_role|eyJ[A-Za-z0-9_-]{20,}\.[A-Za-z0-9_-]{20,}"
SECRET_HITS="$(grep -rnE "$SECRET_PATTERN" \
  --exclude-dir={.git,build,.gradle,node_modules,.idea} \
  --exclude='package-lock.json' \
  --exclude='saeed-stores-prompt-v2_2.md' \
  --exclude='verify-repo.sh' \
  . || true)"
if [ -z "$SECRET_HITS" ]; then
  pass "12. no secrets committed"
else
  printf '%s\n' "$SECRET_HITS"
  fail "12. secrets found in the repository"
fi

# 13. Tests and lint must pass. This runs inside GitHub Actions.
if [ "${CI:-}" = "true" ] || [ -n "${GITHUB_ACTIONS:-}" ]; then
  ./gradlew --no-daemon test lint && pass "13. unit tests and lint passed" || fail "13. tests or lint failed"
else
  printf 'SKIP: 13. run "./gradlew test lint" in an environment with the Android SDK\n'
fi

printf '\nAll repository checks passed.\n'
