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
# Both key families are covered: the legacy high-privilege JWT and the newer
# secret token, plus any leaked publishable token.
SECRET_PATTERN="AIza[0-9A-Za-z_-]{35}|sk-[A-Za-z0-9]{20,}|service""_role"
SECRET_PATTERN="$SECRET_PATTERN|eyJ[A-Za-z0-9_-]{20,}\\.[A-Za-z0-9_-]{20,}"
SECRET_PATTERN="$SECRET_PATTERN|sb_secret_[A-Za-z0-9_-]{10,}|sb_publishable_[A-Za-z0-9_-]{16,}"

# The scan judges what Git would publish. A file Git ignores and does not track
# is not published, so a hit there is dropped: `local.properties` holds the real
# values on a developer machine by design. A file that is tracked is scanned
# whether it is ignored or not, which is what matters for a real leak.
IGNORED_FILES="$(git ls-files --others --ignored --exclude-standard 2>/dev/null || true)"

SECRET_HITS="$(grep -rnoE "$SECRET_PATTERN" \
  --exclude-dir={.git,build,.gradle,node_modules,.idea} \
  --exclude='package-lock.json' \
  --exclude='saeed-stores-prompt-v2_2.md' \
  --exclude='verify-repo.sh' \
  . 2>/dev/null || true)"

PUBLISHED_HITS="$(
  printf '%s\n' "$SECRET_HITS" | while IFS= read -r line; do
    [ -z "$line" ] && continue
    file="${line%%:*}"
    file="${file#./}"
    case "$file" in
      local.properties|keystore.properties) continue ;;
    esac
    if [ -n "$IGNORED_FILES" ] && printf '%s\n' "$IGNORED_FILES" | grep -qxF "$file"; then
      continue
    fi
    printf '%s\n' "$line"
  done
)"

# The tests carry deliberately fake values so that the key checks can run. They
# are dropped by looking at the value itself, not at the surrounding line, so a
# real key is still reported even when it sits next to the word `test`.
REAL_HITS="$(printf '%s\n' "$PUBLISHED_HITS" \
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
# The build must refuse a secret key, and it must mask both values. The check
# reads the step block itself rather than a fixed number of following lines,
# because a count silently stops reaching the guard once the step grows.
# The block runs from the step name to the next step name.
STEP_BLOCK="$(awk '
  /^      - name: Generate local\.properties$/ { inside = 1; print; next }
  inside && /^      - name: / { exit }
  inside { print }
' .github/workflows/build.yml)"
if printf '%s\n' "$STEP_BLOCK" | grep -q 'sb_secret_'; then
  pass "12d. the build refuses a secret key before compiling"
else
  fail "12d. the build does not refuse a secret key"
fi
# The key is the value that must never appear, so it needs its own mask; the
# address mask alone used to satisfy this check.
if printf '%s\n' "$STEP_BLOCK" | grep -q 'add-mask::\$SUPABASE_KEY'; then
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
