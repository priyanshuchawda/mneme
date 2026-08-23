#!/usr/bin/env bash
# Build Mneme product APK (arm64, JDK 21).
# Usage: ./scripts/build-mneme.sh [assembleDebug|assembleRelease]
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
APP_DIR="$ROOT/android/mneme"
JAVA_HOME="${JAVA_HOME:-$HOME/.local/jdk-21}"
export JAVA_HOME
export PATH="$JAVA_HOME/bin:$PATH"
TASK="${1:-assembleDebug}"
cd "$APP_DIR"
echo "JDK: $($JAVA_HOME/bin/java -version 2>&1 | head -1)"
./gradlew "$TASK" --no-daemon
APK="$APP_DIR/app/build/outputs/apk/debug/app-debug.apk"
if [[ "$TASK" == *Release* ]]; then
  APK="$APP_DIR/app/build/outputs/apk/release/app-release-unsigned.apk"
fi
if [[ -f "$APK" ]]; then
  echo "APK: $APK ($(du -h "$APK" | cut -f1))"
fi
