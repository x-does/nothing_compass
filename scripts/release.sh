#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
export JAVA_HOME="${JAVA_HOME:-$ROOT/.tools/jdk}"
export ANDROID_HOME="${ANDROID_HOME:-$ROOT/.tools/android-sdk}"
export PATH="$JAVA_HOME/bin:$PATH"
cd "$ROOT"
./gradlew testReleaseUnitTest lintRelease assembleRelease bundleRelease --console=plain
mkdir -p dist/release
cp app/build/outputs/bundle/release/app-release.aab dist/release/Glyph-North-1.6.aab
cp app/build/outputs/mapping/release/mapping.txt dist/release/mapping.txt
if [[ -f keystore.properties ]]; then
  cp app/build/outputs/apk/release/app-release.apk dist/release/Glyph-North-1.6.apk
  printf '\nSigned release outputs are in dist/release. Verify the upload certificate before uploading.\n'
else
  printf '\nUnsigned AAB generated for inspection only. Configure keystore.properties and rebuild before Play upload.\n'
fi
