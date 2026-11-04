#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
mkdir -p "$ROOT/.tools" "$ROOT/app/libs"
TOOLS="$ROOT/.tools"
if [[ ! -x "$TOOLS/jdk/bin/java" ]]; then
  curl -fL --retry 3 'https://api.adoptium.net/v3/binary/version/jdk-17.0.21%2B9/linux/x64/jdk/hotspot/normal/eclipse' -o "$TOOLS/jdk.tar.gz"
  mkdir -p "$TOOLS/jdk"
  tar -xzf "$TOOLS/jdk.tar.gz" --strip-components=1 -C "$TOOLS/jdk"
  rm "$TOOLS/jdk.tar.gz"
fi
export JAVA_HOME="$TOOLS/jdk"
export PATH="$JAVA_HOME/bin:$PATH"
export ANDROID_HOME="$TOOLS/android-sdk"
if [[ ! -x "$TOOLS/android-cli/cmdline-tools/bin/sdkmanager" ]]; then
  curl -fL --retry 3 'https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip' -o "$TOOLS/android-cli.zip"
  unzip -q "$TOOLS/android-cli.zip" -d "$TOOLS/android-cli"
  rm "$TOOLS/android-cli.zip"
fi
SDKMANAGER="$TOOLS/android-cli/cmdline-tools/bin/sdkmanager"
# The SDK manager displays Google's terms and asks for acceptance interactively.
"$SDKMANAGER" --sdk_root="$ANDROID_HOME" 'platforms;android-36' 'build-tools;35.0.0' 'platform-tools'
SDK="$ROOT/app/libs/glyph-matrix-sdk-2.0.aar"
if [[ ! -f "$SDK" ]]; then
  curl -fL --retry 3 'https://raw.githubusercontent.com/Nothing-Developer-Programme/GlyphMatrix-Developer-Kit/999b1143a6bff0dd79db11d21b92c15c56a346b1/glyph-matrix-sdk-2.0.aar' -o "$SDK"
fi
printf '%s  %s\n' be00ee9cd7115f6b11984c6e31fe98e298fb726940d1555063610685ef3bbf29 "$SDK" | sha256sum -c -
printf '\nReady. Build with: ./scripts/build.sh\n'
