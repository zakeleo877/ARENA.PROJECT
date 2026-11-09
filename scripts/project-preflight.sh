#!/usr/bin/env bash
# Reports the state of a real Supabase project before any account is used.
#
# It answers three questions that have to be settled before the store can work
# on a project, and it does so without a customer account, because a project
# that has just been created has none:
#
#   1. Is the address reachable and is the publishable key accepted?
#   2. Is the schema applied, and does it have the pieces the applications use?
#   3. Is the image bucket public, so that a product image is really served?
#
# Nothing is written. Every line is the result of a call to the project.
#
# Required: TEST_SUPABASE_URL, TEST_SUPABASE_KEY
set -uo pipefail

: "${TEST_SUPABASE_URL:?TEST_SUPABASE_URL is required}"
: "${TEST_SUPABASE_KEY:?TEST_SUPABASE_KEY is required (the publishable key)}"

BASE="${TEST_SUPABASE_URL%/}"
KEY="$TEST_SUPABASE_KEY"
PASSES=0
FAILS=0
NOTES=0

pass() { PASSES=$((PASSES + 1)); printf 'PASS: %s\n' "$1"; }
fail() { FAILS=$((FAILS + 1)); printf 'FAIL: %s\n' "$1"; }
note() { NOTES=$((NOTES + 1)); printf 'NOTE: %s\n' "$1"; }

body="$(mktemp)"
trap 'rm -f "$body"' EXIT

# get <path> -- echoes the status, leaves the body in $body.
get() {
  curl -sS -o "$body" -w '%{http_code}' -H "apikey: $KEY" "$BASE$1" 2>/dev/null || echo 000
}

case "$KEY" in
  sb_secret_*)
    printf 'ABORT: that is a secret key. It bypasses RLS, so nothing here would prove anything.\n' >&2
    exit 1
    ;;
esac

printf '\n== The state of the project (no account is used) ==\n\n'

# ---------------------------------------------------------------- reachability
printf -- '-- the address and the key --\n'
CODE="$(get '/auth/v1/health')"
case "$CODE" in
  200) pass "the address answers and the publishable key is accepted" ;;
  000) fail "the address could not be reached; check for a typo in the project reference" ;;
  401|403) fail "the project refused the key ($CODE); is it the publishable key of this project?" ;;
  *) note "auth health returned $CODE" ;;
esac

# --------------------------------------------------------------------- schema
printf -- '\n-- the tables the applications read --\n'
for pair in \
  'products|id,name,price,description,image_url,category,available,sort_order,created_at,original_price,rating&limit=1' \
  'settings|key&is_public=eq.true&limit=1'; do
  table="${pair%%|*}"
  query="${pair#*|}"
  CODE="$(get "/rest/v1/$table?select=$query")"
  case "$CODE" in
    200) pass "the $table table exists and is readable" ;;
    404) fail "the $table table is missing; apply supabase/schema.sql" ;;
    401|403) fail "reading $table was refused ($CODE); check the policies" ;;
    *) fail "the $table table returned $CODE: $(head -c 120 "$body")" ;;
  esac
done

# The orders table must exist but must not answer an anonymous reader.
CODE="$(get '/rest/v1/orders?select=id&limit=1')"
case "$CODE" in
  200)
    if [ "$(head -c 2 "$body")" = "[]" ]; then
      pass "the orders table exists and hides every row from an anonymous reader"
    else
      fail "an anonymous reader received orders; the orders policy is too wide"
    fi
    ;;
  401|403) pass "the orders table exists and refuses an anonymous reader ($CODE)" ;;
  404) fail "the orders table is missing; apply supabase/schema.sql" ;;
  *) fail "the orders table returned $CODE" ;;
esac

# The functions the applications call.
CODE="$(curl -sS -o "$body" -w '%{http_code}' -X POST \
  -H "apikey: $KEY" -H 'Content-Type: application/json' -d '{}' \
  "$BASE/rest/v1/rpc/claim_first_admin" 2>/dev/null || echo 000)"
case "$CODE" in
  200) pass "claim_first_admin exists and answers without a session (false)" ;;
  401|403) pass "claim_first_admin exists and refuses a caller without a session ($CODE)" ;;
  404) fail "claim_first_admin is missing; apply supabase/schema.sql" ;;
  *) note "claim_first_admin returned $CODE" ;;
esac

# ---------------------------------------------------------------------- images
printf -- '\n-- the image bucket --\n'
# An anonymous listing of the bucket must not work: reading is for managers.
CODE="$(curl -sS -o "$body" -w '%{http_code}' \
  -H "apikey: $KEY" "$BASE/storage/v1/object/list/product-images" 2>/dev/null || echo 000)"
case "$CODE" in
  200) fail "an anonymous caller can list the image bucket; the storage policy is too wide" ;;
  400|401|403) pass "listing the image bucket is refused to an anonymous caller ($CODE)" ;;
  404) fail "the bucket product-images is missing; apply supabase/schema.sql" ;;
  *) note "listing the bucket returned $CODE" ;;
esac

# A public bucket serves /object/public/... without a key at all. That is what
# the customer application relies on, so the check omits the key on purpose.
IMAGE_PATH="$(get '/rest/v1/products?select=image_url&image_url=neq.&limit=1')"
if [ "$IMAGE_PATH" = "200" ]; then
  URL_IN_ROW="$(python3 -c '
import json, sys
try:
    rows = json.load(open(sys.argv[1]))
except Exception:
    print(""); raise SystemExit(0)
print(rows[0].get("image_url", "") if rows else "")
' "$body" 2>/dev/null)"
  case "$URL_IN_ROW" in
    "$BASE"/storage/v1/object/public/*)
      PUBLIC_CODE="$(curl -sS -o /dev/null -w '%{http_code}' "$URL_IN_ROW" 2>/dev/null)"
      case "$PUBLIC_CODE" in
        200) pass "a product image is served publicly (200); the bucket is public" ;;
        400|403) fail "the bucket is private ($PUBLIC_CODE); the customer screen would show a broken image" ;;
        *) fail "the product image returned $PUBLIC_CODE" ;;
      esac
      ;;
    '') note "no product carries an image yet, so the public read could not be checked" ;;
    *) note "the image is hosted outside the project, so the bucket was not exercised" ;;
  esac
else
  note "the image_url column could not be read ($IMAGE_PATH)"
fi

printf '\n%d passed, %d failed, %d noted.\n' "$PASSES" "$FAILS" "$NOTES"
[ "$FAILS" -eq 0 ] || exit 1
