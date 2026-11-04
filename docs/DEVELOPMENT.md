<!-- Historical setup and development notes. Public introduction: ../README.md -->

# Glyph North · Nothing Things

## Version 1.6 · user-facing redesign

The new **Display** screen puts a live clock/compass preview, Start/Stop and the shake/flip guide first. **Settings** contains brightness, orientation, setup, calibration, privacy and connection details. The adaptive launcher icon supports Android themed icons.

- Test install: `dist/Glyph-North-1.6.apk` (debug-signed, updates the existing test app).
- Release preparation: `./scripts/release.sh` generates a minified APK and AAB. Configure the ignored `keystore.properties` using its example to sign with your own upload key. An unsigned AAB is not upload-ready.
- [Play release checklist](store/RELEASE-CHECKLIST.md), [store listing draft](store/listing.md), [privacy policy draft](store/privacy-policy.md).
- Generate original 512px store icon and 1024×500 feature artwork with `python3 scripts/store-artwork.py` (Pillow required). Actual app/phone screenshots still need to be captured.

Public release is pending physical reliability testing, publisher identity/contact/privacy hosting, upload-key configuration and SDK distribution/licensing confirmation.

A small Android app that points a Nothing Glyph Matrix toward **magnetic north**, designed primarily for **Nothing Phone (4a) Pro**. Includes a screen preview, live rear-display mode, and an Always-on Glyph Toy.

## Install on your phone

### Version 1.5: automatic clock and shake compass

Install `dist/Glyph-North-1.5.apk` over the existing app. Tap **Start automatic Time / Compass**, or activate the Quick Settings tile from Off (its first mode is Time). The service starts with the 24-hour clock. Hold the phone **screen down / Glyph up** for a moment and shake it twice to switch to Compass. Compass stays selected until the front screen faces upward for about 350 ms, then Time returns. Off stops motion listening too. The app does not automatically start on boot or installation.

The accelerometer remains active in Time mode to detect the shake/flip, so clock mode now uses more sensor power than v1.4. Two movement impulses within 900 ms, a face-down settling period and a cooldown reduce accidental switches. Motion sensitivity and screen-off lifetime require testing on the physical handset. For live mode, keep Glyph North deselected as the system Always-on Toy as described below. The fixed-arrow diagnostic switch overrides the rendered image; the automatic Start button disables it.

1. Transfer [`dist/Glyph-North-1.3.apk`](dist/Glyph-North-1.3.apk) to your phone and open it in Files.
2. If prompted, allow **Install unknown apps** for the app opening the APK, then install.
3. Open **Glyph North**. Enable Glyph Interface in Nothing Settings.
4. Tap **Start live Glyph compass**, allow notifications, and turn the phone face down. Hold it approximately horizontal, with the rear matrix visible. The arrowhead points north.
5. Stop from the app or its persistent notification when finished.

For the system-managed toy: **Settings → Glyph Interface → Flip to Glyph → Always-on Glyph Toy → Glyph North**. Enable Flip to Glyph. The app has a setup shortcut, with manual instructions if Nothing OS doesn't expose that shortcut.

### Quick Settings: Off → Time → Compass

Version 1.3 adds automatic reconnection after vendor-service disconnects/output errors, a five-second connection retry, and a 15-second live-session renewal for silent display takeovers. Selecting any active mode establishes a fresh SDK connection. These are recovery workarounds, not a guarantee that firmware will restore output after every notification; other system Glyph content retains priority. The app also has **Reconnect current Glyph mode** and recovery diagnostics. Validate notification recovery on the phone.

In v1.2, tap **Add Quick Settings tile** in the app and approve Android's prompt. Alternatively swipe down twice, edit tiles, and add **Glyph North**. Each tap cycles **Off → Time → Compass → Off** without opening the app. The tile subtitle shows the selected mode; long-press opens the app. Time is 24-hour, with **HH on the upper row and MM below**. Stop from the notification also turns the live tile off. Tile taps disable the fixed-arrow diagnostic test. See [implementation and Leveller analysis](docs/QUICK-SETTINGS.md).

**Live mode** uses a foreground service and partial wake lock to keep sensor processing available with the screen off. It consumes battery while active. **Toy mode** follows Nothing OS's binding/AOD lifecycle; the SDK documents AOD messages once per minute, so continuous toy refresh is not guaranteed. System Glyph Toys have priority over live app output.

The preview is front-facing; its left/right direction intentionally differs from the rear matrix. An **X** replaces the arrow when the sensor reports poor accuracy or the phone is tilted more than about 41°. Move the phone in a figure eight away from magnets or metal. If your firmware renders the matrix rotated, use **Adjust rear display alignment**.

### Compatibility and verification

- Minimum Android 14; compiled and targeted for Android 16 / API 36.
- Glyph output supports Phone (4a) Pro (13×13) and Phone (3) (25×25). Other devices get the screen compass only.
- No internet, location, account, or root required. Magnetic north differs from geographic true north; no location-based declination correction is applied.
- Supplied APK is **debug-signed**, suitable for personal sideloading. Preserve the local debug signing key if you want future locally built APKs to update this installation without uninstalling.
- Build, eight unit tests, lint, and APK signature verification were run locally. The user confirmed v1.1 live compass output on A069P / FroggerPro, Android 17 API 37, vendor package 13.01.01. **The new v1.2 tile and clock need device testing**; system toy/AOD behavior is still unverified.

## Development environment

Prepared locally in `.tools/`: Temurin JDK 17, Android command-line tools, Android API 36 platform, build tools, and ADB. Gradle 8.14.3 wrapper and Android Gradle Plugin 8.10.1 are pinned. The SDK download is pinned to Nothing's upstream revision and verified by SHA-256.

On a fresh **Linux x86-64** machine with `curl`, `unzip`, and `tar`:

```bash
./scripts/setup.sh     # Downloads tools and SDK; displays Google's licence prompts
./scripts/build.sh     # Tests, lint, APK build; outputs dist/Glyph-North-1.3.apk
./scripts/install.sh   # USB debugging enabled and phone authorized via ADB
```

In Android Studio, open the repository, select JDK 17, and configure your Android SDK normally. Run `scripts/setup.sh` or download the pinned Glyph AAR into `app/libs/` first. `.tools/` and vendor binaries are ignored by Git.

### If Glyph doesn't light up

Version 1.1 corrects raw-frame brightness to the SDK's 0–2047 range and adds a sensor-independent fixed-arrow test. Enable **Rear display test**, start live mode or select the system toy, and check the rear display. **Show / copy connection diagnostics** reports submitted frames, sensor samples, last system toy bind, last AOD event, and output errors. A successful SDK call does not confirm visible LEDs.

1. Check the app's Glyph status message; update Nothing OS and enable Glyph Interface.
2. Confirm your phone is **4a Pro**, not the 4a with a strip-style Glyph Bar.
3. For AOD mode, explicitly select Glyph North in the Always-on Glyph Toy setting.
4. Stop and restart live mode after changing settings. Other system Glyph activity can override app output.
5. Nothing's SDK documentation says API-key restrictions were removed starting with Android 16. The manifest retains `NothingKey=test` as recommended for compatibility. For older firmware that rejects registration, development mode can be enabled with:

```bash
adb shell settings put global nt_glyph_interface_debug_enable 1
```

Nothing says debug mode expires after 48 hours. Older firmware may need a real Nothing developer key for normal use. This is not expected to be necessary on current Android 16 firmware, but has not been verified on your handset.

## Extending the project

See [Glyph development research](docs/GLYPH-DEVELOPMENT.md) for hardware capabilities, APIs, animation/game architecture, constraints, and official sources.

- `CompassMath.java`: platform-independent geometry and 13×13 / 25×25 grayscale arrow frames.
- `HeadingSensor.java`: magnetic rotation-vector sensor, accelerometer/magnetometer fallback, circular smoothing, tilt and accuracy checks; output capped at 10 Hz.
- `CompassService.java`: SDK registration, live vs toy frame routing, notification, lifecycle and cleanup.
- `MainActivity.java`: setup, preview, brightness, alignment and calibration guidance.

The official Glyph SDK is proprietary; its terms are linked in `app/libs/README.md`. Review Nothing's commercial-use requirements before publishing a paid product.
