#!/usr/bin/env bash
# Install Mneme on connected phone and grant runtime bits.
# Usage: ./scripts/install-mneme.sh [serial]
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
SERIAL="${1:-${ANDROID_SERIAL:-1431df87}}"
APK="$ROOT/android/mneme/app/build/outputs/apk/debug/app-debug.apk"
PKG="com.mneme.app.debug"

[[ -f "$APK" ]] || { echo "Missing APK — run ./scripts/build-mneme.sh first"; exit 1; }

adb -s "$SERIAL" wait-for-device
adb -s "$SERIAL" install -r -t "$APK"
adb -s "$SERIAL" shell "appops set $PKG PROJECT_MEDIA allow 2>/dev/null || true"
adb -s "$SERIAL" shell "pm grant $PKG android.permission.POST_NOTIFICATIONS 2>/dev/null || true"
adb -s "$SERIAL" shell "pm grant $PKG android.permission.READ_MEDIA_IMAGES 2>/dev/null || true"
adb -s "$SERIAL" shell "am start -n $PKG/com.mneme.app.MainActivity"
echo "Installed + launched $PKG on $SERIAL"
