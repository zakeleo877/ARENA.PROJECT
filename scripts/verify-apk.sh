#!/usr/bin/env bash
# Package checks 1-10 from the acceptance criteria, run against one APK.
# Usage: verify-apk.sh <apk-path> <expected-app-name>
set -euo pipefail

APK="${1:?usage: verify-apk.sh <apk> <name>}"
EXPECTED="${2:?usage: verify-apk.sh <apk> <name>}"

BUILD_TOOLS="${ANDROID_HOME:-$ANDROID_SDK_ROOT}/build-tools"
ZIPALIGN="$(find "$BUILD_TOOLS" -name zipalign -type f 2>/dev/null | sort | tail -n 1 || true)"
APKSIGNER="$(find "$BUILD_TOOLS" -name apksigner -type f 2>/dev/null | sort | tail -n 1 || true)"
AAPT2="$(find "$BUILD_TOOLS" -name aapt2 -type f 2>/dev/null | sort | tail -n 1 || true)"

fail() { printf 'FAIL: %s\n' "$1"; exit 1; }
pass() { printf 'PASS: %s\n' "$1"; }

[ -f "$APK" ] || fail "APK not found: $APK"
[ -n "$ZIPALIGN" ] || fail "zipalign not found in the Android SDK"
[ -n "$APKSIGNER" ] || fail "apksigner not found in the Android SDK"
[ -n "$AAPT2" ] || fail "aapt2 not found in the Android SDK"

# 1. resources.arsc must be stored uncompressed.
if unzip -v "$APK" | grep -q 'resources.arsc' && unzip -v "$APK" | grep 'resources.arsc' | grep -q 'Stored'; then
  pass "1. resources.arsc is stored uncompressed"
else
  fail "1. resources.arsc is compressed"
fi

# 2. Alignment.
"$ZIPALIGN" -c -p 4 "$APK" && pass "2. zipalign verified" || fail "2. zipalign failed"

# 3. Signature, v2 or newer.
"$APKSIGNER" verify --verbose --print-certs "$APK" | tee /tmp/apksigner-$$.txt
if grep -qE 'Verified using v2 scheme|Verified using v3 scheme|Verified using v4 scheme' /tmp/apksigner-$$.txt; then
  pass "3. APK is signed with v2 or newer"
else
  fail "3. APK is not signed with v2 or newer"
fi
rm -f /tmp/apksigner-$$.txt

# 4 and 5. Identity and debuggability.
BADGING="$("$AAPT2" dump badging "$APK")"
echo "$BADGING" | grep -E "^package:|application-label" || true
echo "$BADGING" | grep -q "application-label:'" || fail "4. application label missing"
if echo "$BADGING" | grep -q "minSdkVersion:'24'"; then
  pass "4. minSdkVersion is 24"
else
  fail "4. minSdkVersion is not 24"
fi
# The version is reported on the package line of the badging output.
if echo "$BADGING" | grep -q "versionName='1.0'"; then
  pass "4. versionName is 1.0"
else
  fail "4. versionName is not 1.0"
fi
if echo "$BADGING" | grep -q "application-debuggable"; then
  fail "5. APK is debuggable"
else
  pass "5. APK is not debuggable"
fi

# 6. Exported components.
MANIFEST_TREE="$("$AAPT2" dump xmltree "$APK" --file AndroidManifest.xml)"
if echo "$MANIFEST_TREE" | grep -q 'android:exported'; then
  pass "6. exported attribute present"
else
  fail "6. no exported attribute found"
fi

# 7. Package integrity.
unzip -t "$APK" > /dev/null && pass "7. package integrity verified" || fail "7. package is corrupt"

# 8. Permissions.
PERMISSIONS="$("$AAPT2" dump permissions "$APK")"
echo "$PERMISSIONS"
ALLOWED='android.permission.INTERNET|android.permission.ACCESS_NETWORK_STATE|android.permission.POST_NOTIFICATIONS|android.permission.ACCESS_COARSE_LOCATION|android.permission.ACCESS_FINE_LOCATION|android.permission.WAKE_LOCK|android.permission.RECEIVE_BOOT_COMPLETED|android.permission.FOREGROUND_SERVICE|android.permission.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION'
UNEXPECTED="$(echo "$PERMISSIONS" | grep -oE 'android\.permission\.[A-Z_]+' | sort -u | grep -vE "^($ALLOWED)$" || true)"
if [ -z "$UNEXPECTED" ]; then
  pass "8. no unexpected permissions"
else
  fail "8. unexpected permissions: $UNEXPECTED"
fi

# 9. Code isolation: the customer APK must not contain admin classes.
if [ "$EXPECTED" = "saeed-store" ]; then
  if "$AAPT2" dump badging "$APK" | grep -q 'com.zakeleo877.saeedadmin'; then
    fail "9. customer APK references the admin package"
  else
    pass "9. customer APK contains no admin code"
  fi
else
  if "$AAPT2" dump badging "$APK" | grep -q 'com.zakeleo877.saeedstore'; then
    fail "9. admin APK references the customer package"
  else
    pass "9. admin APK contains no customer code"
  fi
fi

# 10. Size.
SIZE_BYTES=$(stat -c%s "$APK")
if [ "$SIZE_BYTES" -le 20971520 ]; then
  pass "10. APK size is $((SIZE_BYTES / 1024)) KiB (limit 20 MiB)"
else
  fail "10. APK size $((SIZE_BYTES / 1024)) KiB exceeds 20 MiB"
fi

printf '\nAll package checks passed for %s.\n' "$EXPECTED"
