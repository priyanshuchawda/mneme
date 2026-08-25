#!/usr/bin/env bash
# Run Retenqo JVM unit tests (no device required).
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
APP_DIR="$ROOT/android/mneme"
JAVA_HOME="${MNEME_JAVA_HOME:-$HOME/.local/jdk-21}"
export JAVA_HOME
export PATH="$JAVA_HOME/bin:$PATH"
cd "$APP_DIR"
./gradlew :app:testDebugUnitTest --no-daemon "$@"
echo "Unit tests OK"
