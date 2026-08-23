#!/usr/bin/env bash
# Install Mneme on connected phone and grant runtime bits.
# Usage: ./scripts/install-mneme.sh [serial]
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
SERIAL="${1:-${ANDROID_SERIAL:-1431df87}}"
APK="$ROOT/android/mneme/app/build/outputs/apk/debug/app-debug.apk"
PKG="com.mneme.app.debug"
LLM_SRC="/sdcard/Mneme/models/phone1/llm/Qwen3.5-2B-M-TS-Q4_K_M.gguf"
LLM_DST="/sdcard/Android/data/$PKG/files/models/llm/Qwen3.5-2B-M-TS-Q4_K_M.gguf"

[[ -f "$APK" ]] || { echo "Missing APK — run ./scripts/build-mneme.sh first"; exit 1; }

adb -s "$SERIAL" wait-for-device
adb -s "$SERIAL" install -r -t "$APK"
adb -s "$SERIAL" shell "appops set $PKG PROJECT_MEDIA allow 2>/dev/null || true"
adb -s "$SERIAL" shell "appops set $PKG MANAGE_EXTERNAL_STORAGE allow 2>/dev/null || true"
adb -s "$SERIAL" shell "pm grant $PKG android.permission.POST_NOTIFICATIONS 2>/dev/null || true"
adb -s "$SERIAL" shell "pm grant $PKG android.permission.READ_MEDIA_IMAGES 2>/dev/null || true"
adb -s "$SERIAL" shell "pm grant $PKG android.permission.READ_MEDIA_AUDIO 2>/dev/null || true"

# App-private copy so MediaProvider cannot block llama.cpp file open
if adb -s "$SERIAL" shell "test -f '$LLM_SRC' && ! test -s '$LLM_DST'" >/dev/null 2>&1; then
  echo "Copying LLM into app files (one-time, ~1 GB)…"
  adb -s "$SERIAL" shell "mkdir -p /sdcard/Android/data/$PKG/files/models/llm"
  adb -s "$SERIAL" shell "cp '$LLM_SRC' '$LLM_DST'"
elif adb -s "$SERIAL" shell "test -s '$LLM_DST'" >/dev/null 2>&1; then
  echo "LLM already in app files"
else
  echo "WARN: shared LLM missing at $LLM_SRC — Ask/Pulse LLM will fail until models are pushed"
fi

adb -s "$SERIAL" shell "am start -n $PKG/com.mneme.app.MainActivity"
echo "Installed + launched $PKG on $SERIAL"
