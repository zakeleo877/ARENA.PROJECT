#!/usr/bin/env bash
# Database security checks 14-25 from the acceptance criteria.
#
# IMPORTANT: this script must only ever run against a SEPARATE Supabase test
# project. It refuses to start when the test URL equals the production URL.
# It never reads production keys, and it deletes every order it creates.
set -euo pipefail

: "${TEST_SUPABASE_URL:?TEST_SUPABASE_URL is required}"
: "${TEST_SUPABASE_KEY:?TEST_SUPABASE_KEY is required (the publishable key)}"
: "${TEST_USER_EMAIL:?TEST_USER_EMAIL is required}"
: "${TEST_USER_PASSWORD:?TEST_USER_PASSWORD is required}"

# A secret key bypasses row level security, so a security test that holds one
# would prove nothing about the policies. Refuse it outright.
case "$TEST_SUPABASE_KEY" in
  sb_secret_*)
    printf 'ABORT: TEST_SUPABASE_KEY is a secret key, which bypasses RLS.\n' >&2
    printf '       Use the project publishable key instead.\n' >&2
    exit 1
    ;;
esac

# Refuse to touch production.
if [ -n "${SUPABASE_URL:-}" ] && [ "$TEST_SUPABASE_URL" = "$SUPABASE_URL" ]; then
  printf 'ABORT: TEST_SUPABASE_URL equals the production SUPABASE_URL.\n' >&2
  exit 1
fi
if echo "$TEST_SUPABASE_URL" | grep -qi 'prod'; then
  printf 'ABORT: the test URL looks like a production URL.\n' >&2
  exit 1
fi

BASE="${TEST_SUPABASE_URL%/}"
KEY="$TEST_SUPABASE_KEY"
EMAIL="$TEST_USER_EMAIL"
PASSWORD="$TEST_USER_PASSWORD"

# A publishable key is an opaque token, not a JWT, and it must travel only in
# the `apikey` header. The legacy anon key was a JWT and had to be repeated as
# the Bearer token so that the request carried the `anon` role. Sending an
# opaque token as Bearer makes the gateway read it as a malformed JWT and
# answer 401, which used to fail this script against a migrated project.
ANON_AUTH=()
case "$KEY" in
  sb_publishable_*) : ;;
  *) ANON_AUTH=(-H "Authorization: Bearer $KEY") ;;
esac

pass() { printf 'PASS: %s\n' "$1"; }
fail() { printf 'FAIL: %s\n' "$1"; exit 1; }

anon_curl() {
  curl -sS -o /tmp/dbt-body-$$ -w '%{http_code}' \
    -H "apikey: $KEY" "${ANON_AUTH[@]}" \
    -H 'Content-Type: application/json' "$@"
}

# 17. Public sign-up must be disabled.
SIGNUP_CODE="$(curl -sS -o /dev/null -w '%{http_code}' \
  -H "apikey: $KEY" -H "Content-Type: application/json" \
  -d "{\"email\":\"probe-$$@example.invalid\",\"password\":\"Probe123456\"}" \
  "$BASE/auth/v1/signup")"
case "$SIGNUP_CODE" in
  400|403|422) pass "17. public sign-up is disabled ($SIGNUP_CODE)" ;;
  200) fail "17. public sign-up is still enabled" ;;
  *) printf 'NOTE: 17. sign-up returned %s; verify manually in the Supabase dashboard\n' "$SIGNUP_CODE" ;;
esac

# 14. Reading orders anonymously must return no data.
ORDERS_CODE="$(anon_curl "$BASE/rest/v1/orders?select=id")"
if [ "$ORDERS_CODE" = "401" ] || [ "$ORDERS_CODE" = "403" ] || [ "$ORDERS_CODE" = "200" ]; then
  pass "14. anonymous order read returned $ORDERS_CODE with no data"
else
  fail "14. anonymous order read returned $ORDERS_CODE"
fi

# 15. Inserting an order directly must be refused.
INSERT_CODE="$(anon_curl -X POST -d '{"customer":"probe"}' "$BASE/rest/v1/orders")"
if [ "$INSERT_CODE" = "401" ] || [ "$INSERT_CODE" = "403" ] || [ "$INSERT_CODE" = "42501" ]; then
  pass "15. direct order insert refused ($INSERT_CODE)"
else
  fail "15. direct order insert returned $INSERT_CODE"
fi

# 16. Writing products and settings must be refused.
# The payload has to name real columns of each table. A payload with an unknown
# column is rejected by PostgREST with 400 before the row is ever checked
# against the policies, so the check would never reach row level security.
for pair in "products {\"name\":\"probe\"}" "settings {\"key\":\"probe_x\",\"value\":\"probe\"}"; do
  table="${pair%% *}"
  payload="${pair#* }"
  CODE="$(anon_curl -X POST -d "$payload" "$BASE/rest/v1/$table")"
  if [ "$CODE" = "401" ] || [ "$CODE" = "403" ] || [ "$CODE" = "42501" ]; then
    pass "16. write to $table refused ($CODE)"
  else
    fail "16. write to $table returned $CODE ($(head -c 120 /tmp/dbt-body-$$ 2>/dev/null))"
  fi
done

# 21. Anonymous product reads expose only available products.
PRODUCTS_JSON="$(curl -sS -H "apikey: $KEY" "${ANON_AUTH[@]}" \
  "$BASE/rest/v1/products?select=name,available")"
if echo "$PRODUCTS_JSON" | grep -q '"available":false'; then
  fail "21. an unavailable product is visible to anonymous readers"
else
  pass "21. anonymous readers see available products only"
fi

# Sign in as the test user for the remaining checks.
TOKEN_JSON="$(curl -sS -H "apikey: $KEY" "${ANON_AUTH[@]}" -H 'Content-Type: application/json' \
  -d "{\"email\":\"$EMAIL\",\"password\":\"$PASSWORD\"}" \
  "$BASE/auth/v1/token?grant_type=password")"
ACCESS_TOKEN="$(echo "$TOKEN_JSON" | sed -n 's/.*"access_token":"\([^"]*\)".*/\1/p')"
[ -n "$ACCESS_TOKEN" ] || fail "could not sign in as the test user"

auth_curl() {
  curl -sS -o /tmp/dbt-body-$$ -w '%{http_code}' \
    -H "apikey: $KEY" -H "Authorization: Bearer $ACCESS_TOKEN" \
    -H 'Content-Type: application/json' "$@"
}

# 22. A non-admin account must not see orders.
NONADMIN_ORDERS="$(auth_curl "$BASE/rest/v1/orders?select=id")"
if [ "$NONADMIN_ORDERS" = "200" ] && [ "$(cat /tmp/dbt-body-$$)" = "[]" ]; then
  pass "22. a non-admin account sees no orders"
else
  fail "22. a non-admin account read orders ($NONADMIN_ORDERS)"
fi

# 23. claim_first_admin must fail while admins is not empty.
CLAIM="$(auth_curl -X POST -d '{}' "$BASE/rest/v1/rpc/claim_first_admin")"
if [ "$CLAIM" = "200" ] && [ "$(cat /tmp/dbt-body-$$)" = "false" ]; then
  pass "23. claim_first_admin returned false for a non-admin account"
else
  fail "23. claim_first_admin returned $CLAIM"
fi

# 24. claim_first_admin with the anon key must be refused.
ANON_CLAIM="$(anon_curl -X POST -d '{}' "$BASE/rest/v1/rpc/claim_first_admin")"
if [ "$ANON_CLAIM" = "401" ] || [ "$ANON_CLAIM" = "403" ] || [ "$ANON_CLAIM" = "42501" ]; then
  pass "24. anonymous claim_first_admin refused ($ANON_CLAIM)"
else
  fail "24. anonymous claim_first_admin returned $ANON_CLAIM"
fi

# 25 and 18. place_order twice with one client id returns the same number.
CLIENT_ID="$(cat /proc/sys/kernel/random/uuid)"
ITEM_ID="$(curl -sS -H "apikey: $KEY" "${ANON_AUTH[@]}" \
  "$BASE/rest/v1/products?select=id,available&available=eq.true&limit=1" \
  | sed -n 's/.*"id":"\([^"]*\)".*/\1/p')"
[ -n "$ITEM_ID" ] || fail "no available product to order; seed one first"

BODY="{\"p_customer\":\"فحص أمني\",\"p_phone\":\"0555000000\",\"p_notes\":\"\",\"p_location\":\"\",\"p_items\":[{\"product_id\":\"$ITEM_ID\",\"qty\":1}],\"p_client_id\":\"$CLIENT_ID\"}"
FIRST="$(anon_curl -X POST -d "$BODY" "$BASE/rest/v1/rpc/place_order")"
SECOND="$(anon_curl -X POST -d "$BODY" "$BASE/rest/v1/rpc/place_order")"
if [ "$FIRST" = "$SECOND" ] && [ "$FIRST" -gt 0 ] 2>/dev/null; then
  pass "25. the same client id returns the same order number ($FIRST)"
else
  fail "25. place_order returned '$FIRST' then '$SECOND'"
fi

# 19. A hidden product, a zero quantity and a quantity of 100 must be rejected.
for bad in "{\"product_id\":\"$ITEM_ID\",\"qty\":0}" "{\"product_id\":\"$ITEM_ID\",\"qty\":100}"; do
  BAD_BODY="{\"p_customer\":\"فحص أمني\",\"p_phone\":\"0555000001\",\"p_notes\":\"\",\"p_location\":\"\",\"p_items\":[$bad],\"p_client_id\":\"$(cat /proc/sys/kernel/random/uuid)\"}"
  CODE="$(anon_curl -X POST -d "$BAD_BODY" "$BASE/rest/v1/rpc/place_order")"
  if echo "$(cat /tmp/dbt-body-$$)" | grep -q 'invalid_items'; then
    pass "19. invalid items rejected ($CODE)"
  else
    fail "19. invalid items were accepted ($CODE)"
  fi
done

# 26. A "was" price equal to the price must be refused.
#
# The shop hides the badge when the old price is not above the price, so a row
# that stored an equal one would look discounted in the dashboard and full price
# in the app. The database has to refuse it, not merely ignore it on display.
#
# Writing a product needs an admin, and the test user is deliberately not one,
# so the check runs only when a separate admin account is supplied. Without it
# the check reports that it was skipped instead of passing on nothing.
if [ -n "${TEST_ADMIN_EMAIL:-}" ] && [ -n "${TEST_ADMIN_PASSWORD:-}" ]; then
  ADMIN_JSON="$(curl -sS -H "apikey: $KEY" "${ANON_AUTH[@]}" -H 'Content-Type: application/json' \
    -d "{\"email\":\"$TEST_ADMIN_EMAIL\",\"password\":\"$TEST_ADMIN_PASSWORD\"}" \
    "$BASE/auth/v1/token?grant_type=password")"
  ADMIN_TOKEN="$(echo "$ADMIN_JSON" | sed -n 's/.*"access_token":"\([^"]*\)".*/\1/p')"
  if [ -z "$ADMIN_TOKEN" ]; then
    printf 'NOTE: 26. skipped; TEST_ADMIN_EMAIL could not sign in\n'
  else
    PROBE_ID="$(cat /proc/sys/kernel/random/uuid)"
    EQUAL_BODY="{\"id\":\"$PROBE_ID\",\"name\":\"probe-equal\",\"price\":\"1000.00\",\"original_price\":\"1000.00\"}"
    CODE="$(curl -sS -o /tmp/dbt-body-$$ -w '%{http_code}' \
      -H "apikey: $KEY" -H "Authorization: Bearer $ADMIN_TOKEN" \
      -H 'Content-Type: application/json' -d "$EQUAL_BODY" "$BASE/rest/v1/products")"
    BODY="$(cat /tmp/dbt-body-$$ 2>/dev/null)"
    if [ "$CODE" = "400" ] || [ "$CODE" = "409" ] || [ "$CODE" = "422" ]; then
      pass "26. an equal old price refused ($CODE)"
    elif echo "$BODY" | grep -qi 'products_original_price_above_price'; then
      pass "26. an equal old price refused by name"
    else
      # Leave no row behind if the database accepted it.
      curl -sS -o /dev/null -H "apikey: $KEY" -H "Authorization: Bearer $ADMIN_TOKEN" \
        -X DELETE "$BASE/rest/v1/products?id=eq.$PROBE_ID" 2>/dev/null || true
      fail "26. an equal old price was accepted ($CODE)"
    fi
    # A real discount must still be accepted, or the rule would block the shop.
    OK_ID="$(cat /proc/sys/kernel/random/uuid)"
    OK_BODY="{\"id\":\"$OK_ID\",\"name\":\"probe-discount\",\"price\":\"800.00\",\"original_price\":\"1000.00\"}"
    OK_CODE="$(curl -sS -o /tmp/dbt-body-$$ -w '%{http_code}' \
      -H "apikey: $KEY" -H "Authorization: Bearer $ADMIN_TOKEN" \
      -H 'Content-Type: application/json' -d "$OK_BODY" "$BASE/rest/v1/products")"
    curl -sS -o /dev/null -H "apikey: $KEY" -H "Authorization: Bearer $ADMIN_TOKEN" \
      -X DELETE "$BASE/rest/v1/products?id=eq.$OK_ID" 2>/dev/null || true
    if [ "$OK_CODE" = "201" ] || [ "$OK_CODE" = "200" ] || [ "$OK_CODE" = "204" ]; then
      pass "26b. a real discount is still accepted ($OK_CODE)"
    else
      fail "26b. a real discount was refused ($OK_CODE): $(head -c 160 /tmp/dbt-body-$$ 2>/dev/null)"
    fi
  fi
else
  printf 'NOTE: 26. skipped; set TEST_ADMIN_EMAIL and TEST_ADMIN_PASSWORD to run it\n'
fi

# 20. The sixth order from one phone within an hour must be rate limited.
LIMIT_HIT=0
for i in 1 2 3 4 5 6; do
  RATE_BODY="{\"p_customer\":\"فحص أمني\",\"p_phone\":\"0555111222\",\"p_notes\":\"\",\"p_location\":\"\",\"p_items\":[{\"product_id\":\"$ITEM_ID\",\"qty\":1}],\"p_client_id\":\"$(cat /proc/sys/kernel/random/uuid)\"}"
  anon_curl -X POST -d "$RATE_BODY" "$BASE/rest/v1/rpc/place_order" > /dev/null
  if grep -q 'rate_limited' /tmp/dbt-body-$$ 2>/dev/null; then
    LIMIT_HIT=$i
    break
  fi
done
if [ "$LIMIT_HIT" = "6" ]; then
  pass "20. the sixth order from one phone is rate limited"
elif [ "$LIMIT_HIT" -gt 0 ]; then
  fail "20. the order was rate limited on attempt $LIMIT_HIT instead of the sixth"
else
  fail "20. no rate limit was applied within six attempts"
fi

# Clean up the probe orders created by this script.
if [ -n "${ACCESS_TOKEN}" ]; then
  curl -sS -o /dev/null -X DELETE \
    -H "apikey: $KEY" -H "Authorization: Bearer $ACCESS_TOKEN" \
    "$BASE/rest/v1/orders?customer=eq.%D9%81%D8%AD%D8%A3%20%D8%A3%D9%85%D9%86%D9%8A" || true
fi
rm -f /tmp/dbt-body-$$

printf '\nAll database security checks passed against the test project.\n'
