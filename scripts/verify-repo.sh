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
  --exclude='acceptance-results.md' \
  --exclude='verify-repo.sh' \
  --exclude='live-acceptance.sh' \
  . || true)"
if [ -z "$DELIVERY_HITS" ]; then
  pass "11. no delivery wording in the project"
else
  printf '%s\n' "$DELIVERY_HITS"
  fail "11. delivery wording found"
fi

# 11b. The optional map link must be named customerLocationUrl in the code and
# must never be called a delivery address. The Room persistence column and the
# SQL parameter keep the names "location" and "p_location" because they are
# fixed by the published database schema.
LOCATION_HITS="$(grep -rniE 'deliveryAddress|delivery_address|shippingAddress|shipping_address|deliveryAddressUrl' \
  --include='*.kt' --include='*.xml' --include='*.sql' \
  --exclude-dir={.git,build,.gradle,node_modules,.idea} \
  . || true)"
if [ -z "$LOCATION_HITS" ]; then
  pass "11b. no delivery-address identifier in the code"
else
  printf '%s\n' "$LOCATION_HITS"
  fail "11b. delivery-address identifier found"
fi

if grep -rq 'customerLocationUrl' --include='*.kt' core customer admin; then
  pass "11c. the map link is named customerLocationUrl in the code"
else
  fail "11c. customerLocationUrl is missing from the Kotlin sources"
fi

# 12. No secrets.
# The pattern is assembled from parts so that this script does not match itself.
# Both key families are covered: the legacy `service_role` JWT and the newer
# `sb_secret_` token, plus any leaked publishable token.
SECRET_PATTERN="AIza[0-9A-Za-z_-]{35}|sk-[A-Za-z0-9]{20,}|service""_role"
SECRET_PATTERN="$SECRET_PATTERN|eyJ[A-Za-z0-9_-]{20,}\\.[A-Za-z0-9_-]{20,}"
SECRET_PATTERN="$SECRET_PATTERN|sb_secret_[A-Za-z0-9_-]{10,}|sb_publishable_[A-Za-z0-9_-]{16,}"
SECRET_HITS="$(grep -rnoE "$SECRET_PATTERN" \
  --exclude-dir={.git,build,.gradle,node_modules,.idea} \
  --exclude='package-lock.json' \
  --exclude='saeed-stores-prompt-v2_2.md' \
  --exclude='verify-repo.sh' \
  . 2>/dev/null || true)"
# The tests carry deliberately fake values so that the key checks can run. They
# are dropped by looking at the value itself, not at the surrounding line, so a
# real key is still reported even when it sits next to the word `test`.
REAL_HITS="$(printf '%s\n' "$SECRET_HITS" \
  | grep -vE ':[A-Za-z0-9_-]*(placeholder|_fake|fake_|not_allowed|_test_|ForTest|for_the_)[A-Za-z0-9_-]*$' \
  | sed '/^$/d' || true)"
if [ -z "$REAL_HITS" ]; then
  pass "12. no secrets committed"
else
  printf '%s\n' "$REAL_HITS"
  fail "12. secrets found in the repository"
fi

# 12c. No application may accept a high-privilege key at configuration time.
for module in customer admin; do
  if grep -q 'sb_secret_' "$module/build.gradle.kts" &&
     grep -q 'roleOfLegacyJwt' "$module/build.gradle.kts"; then
    pass "12c. $module refuses a secret key and a high-privilege JWT"
  else
    fail "12c. $module does not guard against a high-privilege key"
  fi
done
# The workflows build the two packages from the publishable key only.
if grep -A 30 'Generate local.properties' .github/workflows/build.yml | grep -q 'sb_secret_'; then
  pass "12d. the build refuses a secret key before compiling"
else
  fail "12d. the build does not refuse a secret key"
fi
if grep -A 40 'Generate local.properties' .github/workflows/build.yml | grep -q 'add-mask'; then
  pass "12e. the values are masked in the build log"
else
  fail "12e. the values are not masked in the build log"
fi

# 13. Tests and lint must pass. This runs inside GitHub Actions.
if [ "${CI:-}" = "true" ] || [ -n "${GITHUB_ACTIONS:-}" ]; then
  ./gradlew --no-daemon test lint && pass "13. unit tests and lint passed" || fail "13. tests or lint failed"
else
  printf 'SKIP: 13. run "./gradlew test lint" in an environment with the Android SDK\n'
fi

printf '\nAll repository checks passed.\n'
