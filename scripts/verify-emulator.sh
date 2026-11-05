#!/usr/bin/env bash
# Acceptance check: the user interface on a real Android runtime.
#
# The script installs the release APK on an emulator, starts the launcher
# activity, waits for the first frame and captures a screenshot. It also
# checks that the process is alive and that no fatal exception was logged.
#
# Usage: verify-emulator.sh <apk> <package> <output-directory>
set -euo pipefail

APK="${1:?the apk path is required}"
PACKAGE="${2:?the package name is required}"
OUTPUT="${3:?the output directory is required}"
ACTIVITY="${4:-.MainActivity}"

mkdir -p "$OUTPUT"

# Every adb binary that the runner has is tried, because the emulator action
# can use a different SDK than the one this script finds. The first binary
# that reports the device is used for the rest of the run.
CANDIDATE_ADBS=""
for candidate in \
  "$(command -v adb || true)" \
  "${ANDROID_HOME:-}/platform-tools/adb" \
  "${ANDROID_SDK_ROOT:-}/platform-tools/adb" \
  /usr/local/lib/android/sdk/platform-tools/adb \
  "$HOME/Android/Sdk/platform-tools/adb"; do
  if [ -n "$candidate" ] && [ -x "$candidate" ]; then
    CANDIDATE_ADBS="$CANDIDATE_ADBS $candidate"
  fi
done
if [ -z "$CANDIDATE_ADBS" ]; then
  printf 'FAIL: adb is not on the path, so no device can be reached\n'
  exit 1
fi
printf 'The adb binaries that are tried:%s\n' "$CANDIDATE_ADBS"

# Wait for the device to show up, with a hard limit so the job cannot hang.
ADB=""
SERIAL=""
DEVICE_SEEN=0
for _ in $(seq 1 60); do
  for candidate in $CANDIDATE_ADBS; do
    "$candidate" start-server > /dev/null 2>&1 || true
    # The device shows up as offline for a moment after it starts, so both
    # states are accepted and the boot is waited for afterwards.
    FOUND="$("$candidate" devices 2>/dev/null | awk '$1 ~ /^emulator-[0-9]+$/ && ($2 == "device" || $2 == "offline") { print $1; exit }')"
    if [ -n "$FOUND" ]; then
      ADB="$candidate"
      SERIAL="$FOUND"
      break
    fi
  done
  if [ -n "$SERIAL" ]; then
    DEVICE_SEEN=1
    break
  fi
  sleep 5
done
if [ "$DEVICE_SEEN" != "1" ]; then
  printf 'FAIL: no emulator appeared in adb devices\n'
  for candidate in $CANDIDATE_ADBS; do
    printf -- '--- %s ---\n' "$candidate"
    "$candidate" devices 2>&1 || true
  done
  printf '\nThe emulator log follows.\n'
  tail -n 60 /tmp/emulator.log 2>/dev/null || true
  exit 1
fi
printf 'Using adb at %s for the device %s\n' "$ADB" "$SERIAL"
printf 'PASS: the emulator is visible to adb\n'

# Wait for the package manager to finish booting, again with a hard limit.
BOOTED=""
for _ in $(seq 1 90); do
  BOOTED="$("$ADB" -s "$SERIAL" shell getprop sys.boot_completed 2>/dev/null | tr -d '\r' || true)"
  [ "$BOOTED" = "1" ] && break
  sleep 4
done
if [ "$BOOTED" != "1" ]; then
  printf 'FAIL: the emulator did not finish booting\n'
  printf 'sys.boot_completed is "%s"\n' "$BOOTED"
  "$ADB" -s "$SERIAL" shell getprop 2>/dev/null | head -n 20 || true
  printf '\nThe emulator log follows.\n'
  tail -n 60 /tmp/emulator.log 2>/dev/null || true
  exit 1
fi
printf 'PASS: the emulator finished booting\n'

"$ADB" -s "$SERIAL" logcat -c
if ! timeout 300 "$ADB" -s "$SERIAL" install -r -t "$APK" > /tmp/emulator-install.txt 2>&1; then
  printf 'FAIL: the apk could not be installed\n'
  cat /tmp/emulator-install.txt
  exit 1
fi
printf 'PASS: the apk was installed on the emulator\n'

if ! timeout 180 "$ADB" -s "$SERIAL" shell am start -W -n "$PACKAGE/$ACTIVITY" > /tmp/emulator-start.txt 2>&1; then
  printf 'FAIL: the launcher activity did not start\n'
  cat /tmp/emulator-start.txt
  exit 1
fi
cat /tmp/emulator-start.txt

# Give the first composition time to finish and the network time to fail
# gracefully, which is also the offline behaviour that has to be checked.
sleep 15

# The activity is started again when it was closed, because the first launch
# can be interrupted by the setup of the system.
if [ -z "$(timeout 60 "$ADB" -s "$SERIAL" shell pidof "$PACKAGE" 2>/dev/null | tr -d '\r' || true)" ]; then
  printf 'The process was gone after the first launch, so the activity is started again.\n'
  timeout 180 "$ADB" -s "$SERIAL" shell am start -W -n "$PACKAGE/$ACTIVITY" > /tmp/emulator-restart.txt 2>&1 || true
  cat /tmp/emulator-restart.txt
  sleep 15
fi

# The process is looked for in the process list and in the activity manager.
# The second check catches a process that started and was then closed.
PROCESS_ID="$(timeout 60 "$ADB" -s "$SERIAL" shell pidof "$PACKAGE" 2>/dev/null | tr -d '\r' || true)"
if [ -z "$PROCESS_ID" ]; then
  printf 'FAIL: the application process is not running after it was started\n'
  printf -- '--- the foreground activity ---\n'
  timeout 60 "$ADB" -s "$SERIAL" shell dumpsys activity activities 2>/dev/null | grep -iE 'mResumedActivity|mFocusedApp|topResumedActivity' | head -n 5 || true
  printf -- '--- the log ---\n'
  "$ADB" -s "$SERIAL" logcat -d -t 300 2>/dev/null | grep -iE 'saeedstore|AndroidRuntime|FATAL|ActivityManager.*saeed' | tail -n 40 || true
  exit 1
fi
printf 'PASS: the application process is running with the identifier %s\n' "$PROCESS_ID"

FATAL="$("$ADB" -s "$SERIAL" logcat -d 2>/dev/null | grep -E 'FATAL EXCEPTION|AndroidRuntime' | head -n 40 || true)"
if [ -n "$FATAL" ]; then
  printf 'FAIL: a fatal exception was logged while the application was starting\n'
  printf '%s\n' "$FATAL"
  "$ADB" -s "$SERIAL" logcat -d -t 200 | tail -n 60
  exit 1
fi
printf 'PASS: no fatal exception was logged\n'

timeout 120 "$ADB" -s "$SERIAL" exec-out screencap -p > "$OUTPUT/$PACKAGE.png"
if [ ! -s "$OUTPUT/$PACKAGE.png" ]; then
  printf 'FAIL: the screenshot is empty\n'
  exit 1
fi
printf 'PASS: a screenshot was captured at %s\n' "$OUTPUT/$PACKAGE.png"

# The screenshot must not be a single flat colour, which would mean the
# screen never rendered anything.
python3 - "$OUTPUT/$PACKAGE.png" <<'PYEOF'
import sys
import zlib
import struct

path = sys.argv[1]
with open(path, 'rb') as handle:
    data = handle.read()
if data[:8] != b'\x89PNG\r\n\x1a\n':
    print('FAIL: the screenshot is not a PNG file')
    raise SystemExit(1)

position = 8
width = height = 0
idat = b''
while position < len(data):
    length = struct.unpack('>I', data[position:position + 4])[0]
    kind = data[position + 4:position + 8]
    chunk = data[position + 8:position + 8 + length]
    if kind == b'IHDR':
        width, height = struct.unpack('>II', chunk[:8])
    elif kind == b'IDAT':
        idat += chunk
    position += 12 + length

raw = zlib.decompress(idat)
# The PNG is not filtered here on purpose: counting the distinct byte values
# in the raw stream is enough to tell a rendered screen from a blank one.
distinct = len(set(raw))
if distinct < 12:
    print(f'FAIL: the screenshot looks blank ({distinct} distinct byte values)')
    raise SystemExit(1)
print(f'PASS: the screenshot shows a rendered screen ({width}x{height}, {distinct} distinct byte values)')
PYEOF

printf 'PASS: the user interface rendered on the emulator\n'
