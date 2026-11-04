# Quick Settings: Leveller analysis and Glyph North implementation

## What is verifiable about Leveller

[Leveller For Glyph Matrix's Google Play listing](https://play.google.com/store/apps/details?id=com.lisra.matrixleveler) describes Phone (3) and Phone (4a) Pro support and a Quick Settings tile. Its May 10, 2026 changelog explicitly says “Add Nothing Phone (4a) Pro support via Quick Settings Tile”.

The developer's [public GitHub repositories](https://github.com/Lisra-git?tab=repositories) did not expose Leveller source during this research. The listing does not disclose its service implementation, wake-lock policy, exact SDK calls, or background strategy. We have not inspected its APK. Consequently we cannot claim a source-level reconstruction of Leveller. Its advertised UX can be reproduced with Android's documented APIs and the live Glyph SDK path already verified on this user's A069P.

## How our tile works

Android's standard [TileService API](https://developer.android.com/develop/ui/views/quicksettings-tiles) supplies `onClick`, `onStartListening`, state, subtitle and icon updates. The service is exported with the system-only `BIND_QUICK_SETTINGS_TILE` permission and `QS_TILE` intent filter. Active-tile metadata lets the display service request state updates. Since this tile has three modes it is not marked as a binary toggle.

`GlyphTileService` cycles **Off → Time → Compass → Off**. A user tile tap starts or switches the foreground display service directly, without opening the Activity. Off stops live output, clears owned frames, stops sensor sampling and releases the live wake lock. Holding the tile opens the app via `QS_TILE_PREFERENCES`. Android 13's `StatusBarManager.requestAddTileService` provides the in-app Add tile prompt; manual editing remains available.

Tile state is process-local rather than a persisted “on” flag: after a process death or reboot, an inactive service is not presented as still running. There is no automatic restart on boot. Tile labels indicate the selected live mode, not proof of visible LEDs; the app diagnostics provide SDK errors.

Clock mode draws **24-hour hours above minutes**, using four 3×5 digits in a 13×13 logical frame. It reads local time on each scheduled refresh, so date/time/timezone changes are picked up automatically. It applies brightness and rear-alignment settings. The phone's compass sensors are stopped in clock mode. The current implementation refreshes twice per second and retains the existing explicit-session wake lock; clock mode is not optimized into a minute-alarm scheduler.

The live foreground service owns the display even while the Quick Settings panel closes and its TileService is unbound. This avoids tying display lifetime to the transient tile UI. A persistent notification provides a Stop action. Standard Android user-interaction exemptions permit user-started foreground-service work; exact Nothing Android 17 behavior still needs on-device confirmation.

## Verification

### Notification takeover recovery (v1.3)

The user reported v1.2 output stopped after a notification and tile mode changes did not recover it. Code inspection found that `initialize()` returned whenever the manager object existed, even after `onServiceDisconnected` set `connected=false`; no rebind occurred. v1.3 makes active mode selection tear down and reinitialize the SDK, retries disconnected/timed-out connections at five-second intervals, and renews app output every 15 seconds with close/register/resubmit to address silent vendor takeovers. Connection generation checks reject callbacks from retired sessions. A watchdog is independent of sensor delivery and remains active through reconnects; Off removes it. The existing higher-priority notification/toy behavior is preserved. Silent-takeover recovery is a firmware workaround requiring physical testing; there is no SDK API here confirming visible LED state.

Local checks cover all three mode transitions, clock digit placement, every hour/minute combination on both matrix sizes, clock rotation, raw brightness scaling, and compass geometry. Build, lint, APK signature verification pass. Physical compass output in v1.1 was confirmed by the user on A069P / FroggerPro, Android 17 API 37, build C5.0-261021-1625, vendor package 13.01.01. The new tile and clock in v1.2 have not yet been physically verified.

Install v1.2 over v1.1, open the app, tap **Add Quick Settings tile**, then tap the added tile three times to verify Time, Compass and Off. Confirm Stop from notification also makes the tile inactive. Test switching with the screen locked and verify clock digits are legible on the physical matrix.
