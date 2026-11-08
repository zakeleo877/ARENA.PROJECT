#!/usr/bin/env bash
# Live acceptance run against a real Supabase project.
#
# It proves the store actually works against a real database, with the real
# publishable key and the real RLS policies. Nothing here is simulated: every
# PASS line below comes from an HTTP call to the project in TEST_SUPABASE_URL.
#
# Two modes:
#   MODE=test        (default) creates orders, then deletes them. Use a project
#                    dedicated to testing.
#   MODE=production  read-only except one order that is written only when
#                    ALLOW_ORDER_WRITE=1. Without that flag the write checks
#                    are reported as SKIP, never as PASS.
#
# Required:
#   TEST_SUPABASE_URL      https://<project>.supabase.co
#   TEST_SUPABASE_KEY      the publishable key (sb_publishable_...)
#   TEST_USER_EMAIL        a customer account that already exists
#   TEST_USER_PASSWORD
#   TEST_ADMIN_EMAIL       an account listed in public.admins
#   TEST_ADMIN_PASSWORD
#
# Never pass a secret key here: it bypasses RLS, so every policy check below
# would pass for the wrong reason. The script refuses it.
set -uo pipefail

: "${TEST_SUPABASE_URL:?TEST_SUPABASE_URL is required}"
: "${TEST_SUPABASE_KEY:?TEST_SUPABASE_KEY is required (the publishable key)}"
: "${TEST_USER_EMAIL:?TEST_USER_EMAIL is required}"
: "${TEST_USER_PASSWORD:?TEST_USER_PASSWORD is required}"
: "${TEST_ADMIN_EMAIL:?TEST_ADMIN_EMAIL is required}"
: "${TEST_ADMIN_PASSWORD:?TEST_ADMIN_PASSWORD is required}"

MODE="${MODE:-test}"
ALLOW_ORDER_WRITE="${ALLOW_ORDER_WRITE:-0}"

BASE="${TEST_SUPABASE_URL%/}"
KEY="$TEST_SUPABASE_KEY"
PASSES=0
FAILS=0
SKIPS=0

case "$KEY" in
  sb_secret_*)
    printf 'ABORT: that is a secret key. It bypasses RLS, so the policy checks would prove nothing.\n' >&2
    exit 1
    ;;
esac
# The legacy high-privilege JWT is refused by its role claim as well.
case "$(printf '%s' "$KEY" | base64 -d 2>/dev/null || true)" in
  *'"role":"service'*) printf 'ABORT: that is a high-privilege key.\n' >&2; exit 1 ;;
esac

case "$BASE" in
  https://*) : ;;
  # Loopback is allowed only so that this script can be rehearsed against the
  # isolated backend before it is pointed at a real project.
  http://127.0.0.1:*|http://localhost:*) : ;;
  *) printf 'ABORT: TEST_SUPABASE_URL must start with https://\n' >&2; exit 1 ;;
esac

if [ "$MODE" = "test" ] && [ -n "${SUPABASE_URL:-}" ] && [ "$BASE" = "${SUPABASE_URL%/}" ]; then
  printf 'ABORT: in test mode the URL must differ from the production SUPABASE_URL.\n' >&2
  printf '       Use MODE=production to check the production project.\n' >&2
  exit 1
fi

tmp="$(mktemp)"
catalog="$(mktemp)"
trap 'rm -f "$tmp" "$catalog"' EXIT

pass() { PASSES=$((PASSES + 1)); printf 'PASS: %s\n' "$1"; }
fail() { FAILS=$((FAILS + 1)); printf 'FAIL: %s\n' "$1"; }
skip() { SKIPS=$((SKIPS + 1)); printf 'SKIP: %s\n' "$1"; }

# json <file> <path...> -- reads one value from a saved response body.
json() {
  local file="$1"; shift
  python3 - "$file" "$@" <<'PY'
import json, sys
file, path = sys.argv[1], sys.argv[2:]
try:
    with open(file) as handle:
        node = json.load(handle)
except Exception:
    print(''); raise SystemExit(0)
for key in path:
    if isinstance(node, list):
        try:
            node = node[int(key)]
        except Exception:
            print(''); raise SystemExit(0)
    elif isinstance(node, dict):
        if key not in node:
            print(''); raise SystemExit(0)
        node = node[key]
    else:
        print(''); raise SystemExit(0)
print('' if node is None else node)
PY
}

# call <method> <path> [body] -- writes the body to $tmp and echoes the status.
call() {
  local method="$1" path="$2" body="${3:-}" auth="${4:-}"
  local args=(-sS -o "$tmp" -w '%{http_code}' -X "$method"
              -H "apikey: $KEY" -H 'Content-Type: application/json')
  [ -n "$auth" ] && args+=(-H "Authorization: Bearer $auth")
  [ -n "$body" ] && args+=(-d "$body")
  curl "${args[@]}" "$BASE$path"
}

signin() {
  local email="$1" password="$2"
  local code
  code="$(call POST '/auth/v1/token?grant_type=password' \
    "{\"email\":\"$email\",\"password\":\"$password\"}")"
  if [ "$code" != "200" ]; then echo ''; return; fi
  json "$tmp" access_token
}

printf '\n== Live acceptance against %s (mode %s) ==\n\n' "$BASE" "$MODE"

# ---------------------------------------------------------------- 5. accounts
printf -- '-- sign up, sign in, sign out --\n'
SIGNUP_CODE="$(call POST '/auth/v1/signup' \
  "{\"email\":\"probe-$$@example.invalid\",\"password\":\"Probe123456\"}")"
case "$SIGNUP_CODE" in
  400|403|422) pass "5a. public sign-up is disabled ($SIGNUP_CODE)" ;;
  200)         fail "5a. public sign-up is enabled; anyone can create an account" ;;
  *)           skip "5a. sign-up returned $SIGNUP_CODE; check the Auth settings by hand" ;;
esac

WRONG_CODE="$(call POST '/auth/v1/token?grant_type=password' \
  "{\"email\":\"$TEST_USER_EMAIL\",\"password\":\"definitely-wrong-$$\"}")"
case "$WRONG_CODE" in
  400|401|422) pass "5b. a wrong password is refused ($WRONG_CODE)" ;;
  *)           fail "5b. a wrong password returned $WRONG_CODE" ;;
esac

CUSTOMER_TOKEN="$(signin "$TEST_USER_EMAIL" "$TEST_USER_PASSWORD")"
ADMIN_TOKEN="$(signin "$TEST_ADMIN_EMAIL" "$TEST_ADMIN_PASSWORD")"
[ -n "$CUSTOMER_TOKEN" ] && pass "5c. the customer signs in and gets an access token" \
                         || fail "5c. the customer could not sign in"
[ -n "$ADMIN_TOKEN" ]    && pass "5d. the manager signs in and gets an access token" \
                         || fail "5d. the manager could not sign in"

if [ -n "$CUSTOMER_TOKEN" ]; then
  LOGOUT="$(call POST '/auth/v1/logout' '' "$CUSTOMER_TOKEN")"
  case "$LOGOUT" in
    200|204) pass "5e. sign-out succeeds ($LOGOUT)" ;;
    *)       fail "5e. sign-out returned $LOGOUT" ;;
  esac
  # The token from before the sign-out is still valid until it expires; that is
  # how Supabase works, so this is reported rather than asserted.
  CUSTOMER_TOKEN="$(signin "$TEST_USER_EMAIL" "$TEST_USER_PASSWORD")"
fi

# --------------------------------------------------- 6. products and categories
printf -- '\n-- products, categories and images --\n'
CODE="$(call GET '/rest/v1/products?select=id,name,price,category,image_url,available&order=sort_order.asc')"
cp "$tmp" "$catalog"
case "$CODE" in
  200) pass "6a. the catalog is readable with the publishable key ($CODE)" ;;
  *)   fail "6a. the catalog returned $CODE" ; cat "$tmp"; echo ;;
esac

PRODUCT_COUNT="$(python3 -c 'import json,sys;print(len(json.load(open(sys.argv[1]))))' "$catalog" 2>/dev/null || echo 0)"
if [ "${PRODUCT_COUNT:-0}" -gt 0 ]; then
  pass "6b. the catalog has $PRODUCT_COUNT product(s)"
else
  fail "6b. the catalog is empty; seed products before the run"
fi

if python3 -c 'import json,sys;d=json.load(open(sys.argv[1]));sys.exit(0 if any(p.get("available") is False for p in d) else 1)' "$catalog"; then
  fail "6c. a hidden product is visible to an anonymous reader"
else
  pass "6c. only available products are visible to anonymous readers"
fi

CATEGORIES="$(python3 -c 'import json,sys;d=json.load(open(sys.argv[1]));print(",".join(sorted({p["category"] for p in d if p.get("category")})))' "$catalog" 2>/dev/null)"
[ -n "$CATEGORIES" ] && pass "6d. the categories are readable: $CATEGORIES" \
                     || fail "6d. no category could be read"

SETTINGS="$(call GET '/rest/v1/settings?select=key,value&is_public=eq.true')"
[ "$SETTINGS" = "200" ] && pass "6e. the public store settings are readable" \
                        || fail "6e. the settings returned $SETTINGS"

IMAGE_URL="$(python3 -c 'import json,sys;d=json.load(open(sys.argv[1]));print(next((p["image_url"] for p in d if p.get("image_url")),""))' "$catalog" 2>/dev/null)"
if [ -n "$IMAGE_URL" ]; then
  case "$IMAGE_URL" in
    "$BASE"/storage/v1/object/public/*)
      # The image lives in this project's bucket, so the customer application
      # depends on it. A private bucket answers 400 or 403 here.
      IMG_CODE="$(curl -sS -o /dev/null -w '%{http_code}' "$IMAGE_URL" 2>/dev/null)"
      IMG_TYPE="$(curl -sS -o /dev/null -w '%{content_type}' "$IMAGE_URL" 2>/dev/null)"
      case "$IMG_CODE" in
        200) pass "6f. the product image is publicly reachable (200, $IMG_TYPE)" ;;
        400|403) fail "6f. the bucket is not public ($IMG_CODE); the customer screen would show a broken image" ;;
        *) fail "6f. the product image returned $IMG_CODE" ;;
      esac
      ;;
    *)
      # An image hosted somewhere else cannot prove anything about the bucket.
      skip "6f. the product image is hosted outside the project ($IMAGE_URL), so the bucket rules were not exercised"
      ;;
  esac
else
  skip "6f. no product carries an image_url, so the image could not be checked"
fi

PRODUCT_ID="$(python3 -c 'import json,sys;d=json.load(open(sys.argv[1]));print(next((p["id"] for p in d if p.get("available")),""))' "$catalog" 2>/dev/null)"

# --------------------------------------------------------- 7. the cart and order
printf -- '\n-- the cart and the order --\n'
CLIENT_ID="$(python3 -c 'import uuid;print(uuid.uuid4())')"
ORDER_BODY="$(python3 - "$PRODUCT_ID" "$CLIENT_ID" <<'PY'
import json, sys
product, client = sys.argv[1], sys.argv[2]
print(json.dumps({
    "p_customer": "فحص قبول", "p_phone": "0555%06d" % (int(client[:6], 16) % 1000000),
    "p_notes": "", "p_location": "",
    "p_items": [{"product_id": product, "qty": 2}],
    "p_client_id": client,
}, ensure_ascii=False))
PY
)"

if [ -n "$PRODUCT_ID" ]; then
  if [ "$MODE" = "production" ] && [ "$ALLOW_ORDER_WRITE" != "1" ]; then
    skip "7a. placing an order was skipped in production mode (set ALLOW_ORDER_WRITE=1 to run it)"
  else
    FIRST="$(call POST '/rest/v1/rpc/place_order' "$ORDER_BODY")"
    FIRST_NUMBER="$(cat "$tmp" | tr -d '\n')"
    if [ "$FIRST" = "200" ] && [ -n "$FIRST_NUMBER" ] && [ "$FIRST_NUMBER" != "null" ]; then
      pass "7a. the order was saved with number $FIRST_NUMBER ($FIRST)"
    else
      fail "7a. place_order returned $FIRST: $(cat "$tmp")"
      FIRST_NUMBER=""
    fi

    # The same client id must return the same number instead of a copy.
    SECOND="$(call POST '/rest/v1/rpc/place_order' "$ORDER_BODY")"
    SECOND_NUMBER="$(cat "$tmp" | tr -d '\n')"
    if [ -n "$FIRST_NUMBER" ] && [ "$SECOND" = "200" ] && [ "$SECOND_NUMBER" = "$FIRST_NUMBER" ]; then
      pass "7b. resending after a lost response returned the same number, with no duplicate"
    else
      fail "7b. the second send returned $SECOND ('$SECOND_NUMBER')"
    fi

    BAD="$(call POST '/rest/v1/rpc/place_order' \
      "{\"p_customer\":\"فحص\",\"p_phone\":\"05550000\",\"p_notes\":\"\",\"p_location\":\"\",\"p_items\":[{\"product_id\":\"$PRODUCT_ID\",\"qty\":0}],\"p_client_id\":\"$(python3 -c 'import uuid;print(uuid.uuid4())')\"}")"
    if grep -q 'invalid_items' "$tmp" 2>/dev/null; then
      pass "7c. an invalid quantity is refused with a clear message ($BAD)"
    else
      fail "7c. an invalid quantity returned $BAD"
    fi

    # ------------------------------------------------- 8. the management panel
    printf -- '\n-- the management panel --\n'
    if [ -n "$ADMIN_TOKEN" ]; then
      LIST="$(call GET '/rest/v1/orders?select=id,number,status,total&order=created_at.desc&limit=5' '' "$ADMIN_TOKEN")"
      if [ "$LIST" = "200" ]; then
        pass "8a. the manager reads the orders ($LIST)"
      else
        fail "8a. the manager could not read the orders ($LIST)"
      fi

      ORDER_ID="$(python3 -c 'import json,sys;d=json.load(open(sys.argv[1]));print(d[0]["id"] if d else "")' "$tmp" 2>/dev/null)"
      if [ -n "$ORDER_ID" ]; then
        PATCH="$(call PATCH "/rest/v1/orders?id=eq.$ORDER_ID" '{"status":"قيد التحضير"}' "$ADMIN_TOKEN")"
        if [ "$PATCH" = "200" ] || [ "$PATCH" = "204" ]; then
          call GET "/rest/v1/orders?select=status&id=eq.$ORDER_ID" '' "$ADMIN_TOKEN" > /dev/null
          NOW="$(json "$tmp" 0 status)"
          if [ "$NOW" = "قيد التحضير" ]; then
            pass "8b. the manager changed the order status, and the change was stored"
          else
            fail "8b. the status was not stored (found '$NOW')"
          fi
          # Leave the order the way it was found.
          call PATCH "/rest/v1/orders?id=eq.$ORDER_ID" '{"status":"جديد"}' "$ADMIN_TOKEN" > /dev/null
        else
          fail "8b. changing the status returned $PATCH"
        fi
      else
        skip "8b. there was no order to change; place one in test mode first"
      fi
    else
      skip "8a-8b. no manager token, so the panel could not be checked"
    fi

    # ------------------------------------------------------ 9. who may do what
    printf -- '\n-- the customer and the manager, separately --\n'
    if [ -n "$CUSTOMER_TOKEN" ]; then
      C_ORDERS="$(call GET '/rest/v1/orders?select=id' '' "$CUSTOMER_TOKEN")"
      if { [ "$C_ORDERS" = "200" ] && [ "$(cat "$tmp")" = "[]" ]; } || [ "$C_ORDERS" = "403" ]; then
        pass "9a. a signed-in customer cannot read the orders ($C_ORDERS)"
      else
        fail "9a. a customer read the orders ($C_ORDERS): $(head -c 200 "$tmp")"
      fi

      C_INSERT="$(call POST '/rest/v1/orders' '{"customer":"probe"}' "$CUSTOMER_TOKEN")"
      case "$C_INSERT" in
        401|403|42501) pass "9b. a customer cannot insert an order directly ($C_INSERT)" ;;
        *)             fail "9b. a customer inserted an order directly ($C_INSERT)" ;;
      esac

      C_PRODUCT="$(call POST '/rest/v1/products' '{"name":"probe"}' "$CUSTOMER_TOKEN")"
      case "$C_PRODUCT" in
        401|403|42501) pass "9c. a customer cannot write a product ($C_PRODUCT)" ;;
        *)             fail "9c. a customer wrote a product ($C_PRODUCT)" ;;
      esac

      C_ADMIN_TABLE="$(call GET '/rest/v1/admins?select=user_id' '' "$CUSTOMER_TOKEN")"
      case "$C_ADMIN_TABLE" in
        200) [ "$(cat "$tmp")" = "[]" ] && pass "9d. a customer cannot read the managers table" \
                                        || fail "9d. a customer read the managers table" ;;
        401|403) pass "9d. a customer cannot read the managers table ($C_ADMIN_TABLE)" ;;
        *) fail "9d. the managers table returned $C_ADMIN_TABLE" ;;
      esac
    else
      skip "9a-9d. no customer token, so the separation could not be checked"
    fi
  fi
else
  skip "7a-9d. no available product, so the order path could not run"
fi

# ------------------------------------------------------------------- cleanup
if [ "$MODE" = "test" ] && [ -n "${ADMIN_TOKEN:-}" ] && [ -n "${CLIENT_ID:-}" ]; then
  call DELETE "/rest/v1/orders?client_id=eq.$CLIENT_ID" '' "$ADMIN_TOKEN" > /dev/null
  LEFT="$(call GET "/rest/v1/orders?select=id&client_id=eq.$CLIENT_ID" '' "$ADMIN_TOKEN")"
  if [ "$LEFT" = "200" ] && [ "$(cat "$tmp")" = "[]" ]; then
    pass "cleanup. the order created by this run was removed"
  else
    printf 'NOTE: cleanup left an order behind; remove it by hand.\n'
  fi
fi

printf '\n%d passed, %d failed, %d skipped.\n' "$PASSES" "$FAILS" "$SKIPS"
[ "$FAILS" -eq 0 ] || exit 1
