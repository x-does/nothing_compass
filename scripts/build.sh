#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
export JAVA_HOME="${JAVA_HOME:-$ROOT/.tools/jdk}"
export ANDROID_HOME="${ANDROID_HOME:-$ROOT/.tools/android-sdk}"
export PATH="$JAVA_HOME/bin:$PATH"
cd "$ROOT"
./gradlew testDebugUnitTest lintDebug assembleDebug --console=plain
mkdir -p dist
cp app/build/outputs/apk/debug/app-debug.apk dist/Glyph-North-1.6.apk
sha256sum dist/Glyph-North-1.6.apk > dist/SHA256SUMS
printf '\nInstallable APK: %s/dist/Glyph-North-1.6.apk\n' "$ROOT"
