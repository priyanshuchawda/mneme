#!/usr/bin/env bash
# Build Retenqo product APK (arm64, JDK 21).
# Usage: ./scripts/build-mneme.sh [assembleDebug|assembleRelease]
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
APP_DIR="$ROOT/android/mneme"
# Fedora system JDK may be 25; Gradle 8.14 needs ≤21. Prefer Temurin 21 always.
JAVA_HOME="${MNEME_JAVA_HOME:-$HOME/.local/jdk-21}"
export JAVA_HOME
export PATH="$JAVA_HOME/bin:$PATH"
JAVA_VER="$("$JAVA_HOME/bin/java" -version 2>&1 | head -1 || true)"
case "$JAVA_VER" in
  *\"17.*|*\"21.*) ;;
  *)
    echo "Need JDK 17/21 at $JAVA_HOME (or set MNEME_JAVA_HOME). Got: $JAVA_VER"
    exit 1
    ;;
esac
TASK="${1:-assembleDebug}"

cd "$APP_DIR"
echo "JDK: $JAVA_VER"
./gradlew "$TASK" --no-daemon
APK="$APP_DIR/app/build/outputs/apk/debug/app-debug.apk"
case "$TASK" in
  *Release*) APK="$APP_DIR/app/build/outputs/apk/release/app-release-unsigned.apk" ;;
esac
if [[ -f "$APK" ]]; then
  SIZE="$(du -h "$APK" | awk '{print $1}')"
  echo "APK: $APK ($SIZE)"
fi
