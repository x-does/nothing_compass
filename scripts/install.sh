#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
ADB="${ADB:-$ROOT/.tools/android-sdk/platform-tools/adb}"
if [[ ! -x "$ADB" ]]; then ADB=adb; fi
"$ADB" install -r "$ROOT/dist/Glyph-North-1.6.apk"
"$ADB" shell am start -n dev.nothingthings.glyphnorth/.MainActivity
