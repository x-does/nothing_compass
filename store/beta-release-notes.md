# Glyph North 1.6 · Public beta

A clock and shake-to-compass experience for Nothing's rear Glyph Matrix.

## Download

Download **Glyph-North-1.6.apk** below. Android 14+ and a compatible Nothing phone are required for rear output. This is a debug-signed test APK; it is not yet a Google Play release.

SHA-256:
`d5809df4d1e55139199f52ff02f1938214cfe4094a30e433bfa4d0d3eb69c81d`

## Start here

1. Install the APK and enable Glyph Interface in phone settings.
2. Deselect Glyph North as the system Always-on Glyph Toy when using live mode.
3. Open the app and tap **Start Glyph**. Time appears first.
4. Hold screen down / Glyph up and shake twice for Compass. Flip screen up for Time again.
5. Add the Quick Settings shortcut for Off → Time → Compass.

## Included

- New Display and Settings UI with a clock/compass preview.
- Brightness and rear orientation controls.
- Clock-first startup and shake/flip switching.
- Quick Settings tile and notification Stop control.
- Local diagnostics and calibration help.
- No internet permission, account, ads, analytics or location permission.

## Compatibility and known issues

- Phone (4a) Pro: primary target, 13×13 matrix; live output confirmed by a tester.
- Phone (3): 25×25 integration included, physical verification pending.
- Other phones: preview only; regular Phone (4a) rear output is unsupported.
- **Extended screen-off use and notification interruptions have caused freezes in earlier builds. Fixes have been added, but reliability remains unverified.** This is the main beta testing priority.
- System Glyph content can take priority. Active display and motion sensing consume battery until stopped.
- Compass indicates magnetic north, not true north.

Builds, 11 unit tests per variant, lint and test APK signature checks passed. UI/motion sensitivity and long-running reliability require physical testing.

Before reporting a failure, copy **Settings → Connection details** and review device/build information before posting. Please use the repository's Issues tab.

This beta uses the project's debug signing key. A later production release may require uninstalling it, removing local preferences. Nothing's proprietary SDK remains under its own licence; this project is independent and not endorsed by Nothing.
