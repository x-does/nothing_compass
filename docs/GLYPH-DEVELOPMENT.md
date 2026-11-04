# Developing for Nothing's Glyph Matrix

Research checked 3 November 2026 against Nothing's official developer repositories. The Matrix SDK used here is version 2.0 at revision `999b1143a6bff0dd79db11d21b92c15c56a346b1`.

## What the Phone (4a) Pro supports

The official supported-device table lists **Phone (4a) Pro**, identifier `Glyph.DEVICE_25111p`, a **13×13** logical matrix, **no Glyph Touch**, and **AOD toys only**. This is different from the Phone (4a)'s six-channel strip and the Phone (3)'s 25×25 matrix with touch. A generic Phone (3) interactive toy is not automatically compatible with 4a Pro.

Use the Matrix SDK, not old strip-channel examples. `Common.is25111p()` identifies 4a Pro, and `Common.getDeviceMatrixLength()` provides its matrix dimension. Treat 13×13 as a logical frame grid: do not assume all cells have identical visible geometry or reuse 25×25 art unscaled.

## Required ingredients

1. Android project, JDK 17, Gradle, current Android platform/build tools, USB or wireless ADB.
2. Official `glyph-matrix-sdk-2.0.aar` dependency. It exposes the shared `com.nothing.ketchum` package.
3. Manifest permission `com.nothing.ketchum.permission.ENABLE` and compatibility `NothingKey` metadata. Nothing documents keyless operation from Android 16; older firmware has test/debug-key or developer-key requirements.
4. For a discoverable toy: exported Android Service with action `com.nothing.glyph.TOY`, name and preview-image metadata, and `com.nothing.glyph.toy.aod_support=1` on 4a Pro.
5. Real-device testing. An emulator cannot reproduce Nothing's vendor display service, LED orientation, notification priority, or AOD scheduling.

## Display ownership and lifecycle

- Obtain `GlyphMatrixManager.getInstance(context)` and call `init(callback)`.
- Wait for `onServiceConnected`, then call `register(Glyph.DEVICE_25111p)` and check its boolean result.
- **In-app/live output:** call `setAppMatrixFrame(...)`; use `closeAppMatrix()` when finished.
- **System-bound toy output:** call `setMatrixFrame(...)`; start on bind and release sensors/SDK on unbind/destruction.
- Return a `Messenger` binder from the toy Service. Handle `GlyphToy.MSG_GLYPH_TOY`, reading `GlyphToy.MSG_GLYPH_TOY_DATA` for `GlyphToy.EVENT_AOD`.
- Nothing documents AOD events once per minute. AOD availability does not promise a continuously running high-frame-rate animation loop.
- System toys outrank app display content. Apps must handle failed registration, disconnection, unavailable services, and cleanup.

The compass uses one SDK owner for both modes, routes output according to system toy binding, uses raw grayscale frames of the device's actual size, and avoids stale cached AOD headings by restarting sensor sampling on an AOD event. AOD mode does not acquire a wake lock; live mode does, with a visible notification and explicit Stop action.

## Creating animations

Build monochrome/grayscale images at the actual matrix resolution; use simple silhouettes, large pixels, and short loops. Implement a renderer such as `int[] render(long elapsedMillis, int matrixSize)`. **Raw SDK frames use row-major intensity values from 0 to 2047**, confirmed by the SDK collaborator in [issue 8](https://github.com/Nothing-Developer-Programme/GlyphMatrix-Developer-Kit/issues/8#issuecomment-3175500231). Image-object brightness uses a different 0–255 range. The compass's `CompassMath.frame()` uses internal 0–255 values for the preview; `hardwareFrame()` converts them to the raw SDK range before submission. Version 1.0 omitted that conversion.

For richer composition, the SDK exposes `GlyphMatrixObject.Builder` for bitmaps/text, position, orientation, brightness and scale; `GlyphMatrixFrame.Builder` composites low/mid/top layers. Read the current SDK signatures because upstream README examples contain inconsistencies (e.g. fixed 25×25 wording alongside dynamic 13×13 support).

Use a lifecycle-scoped timer, cap update rates, skip identical frames, stop when inactive, and measure battery and frame latency on the phone. Start with 5–10 Hz and test higher rates only where the use case needs them; this is an implementation recommendation, not an SDK hardware limit. AOD animations should assume OS-controlled scheduling.

## Creating games and other features

On 4a Pro, design input around **tilt, motion, or front-screen controls**; there is no rear touch/button API to rely on. An accelerometer-driven marble or balance game, shake-triggered dice, simple direction indicators, timers, pixel pets, and compact status graphics fit the display. For games requiring continuous input/output, prototype through an explicit live app session. Only offer an AOD toy version where minute-scale refresh and system suspension are acceptable.

Suggested extension structure:

- `Scene`: receives sensor/input events and elapsed time; returns a logical frame.
- `Renderer`: handles scaling, rotation, brightness and device size.
- `GlyphOutput`: owns registration, connection status, app/toy priority and shutdown.
- Activity: front-screen controls, help and preview.
- Toy Service: AOD lifecycle and system events.

Glyph North supplies the sensor, grayscale-rendering, installation, and app/toy lifecycle building blocks. It does not yet include an animation editor or game engine.

## Compass coordinate system

Android's rotation matrix maps device coordinates (+X toward the front's right edge, +Y toward its top edge, +Z outward from the front) to east/north/up. Magnetic north projected into the phone plane is `(R[3], R[4])`. The clockwise front-view angle is `atan2(R[3], R[4])`.

Looking at the rear reverses the visible X axis: the correct rear-view angle is `atan2(-R[3], R[4])`, **not** a constant 180° offset. This works for both face-up and face-down orientations, including the latter's changed world orientation. Circular smoothing avoids jumps at north. The UI bearing separately uses the phone top's horizontal projection `atan2(R[1], R[4])`.

No GPS is needed for magnetic north. Geographic true north would require location (or user-supplied coordinates) and Android `GeomagneticField` declination correction. Near-vertical operation is deliberately rejected because the display's horizontal directional interpretation becomes poorly conditioned.

## Physical acceptance checks

1. Install APK, confirm service registration succeeds and 13×13 output is used.
2. Calibrate, put phone face down on a non-metal surface and compare with a known compass away from speakers/cases/magnets.
3. Rotate through N/E/S/W; confirm the rear arrow stays physically toward north. Use alignment correction only if the firmware's matrix mounting orientation differs.
4. Turn the front screen off, verify live updates and the Stop notification; confirm Stop removes app output and releases the live session.
5. Select Glyph North as the Always-on Glyph Toy; check face-down activation, minute AOD events, update continuity and system unbinding.
6. Try another toy/notification, reboot, denied notification permission, disabled Glyph, and low sensor accuracy. Check that error states do not masquerade as a valid arrow.

## Official references

- [Glyph Matrix SDK and supported-device table](https://github.com/Nothing-Developer-Programme/GlyphMatrix-Developer-Kit)
- [Pinned documentation used for this build](https://github.com/Nothing-Developer-Programme/GlyphMatrix-Developer-Kit/blob/999b1143a6bff0dd79db11d21b92c15c56a346b1/README.md)
- [Official example project](https://github.com/Nothing-Developer-Programme/GlyphMatrix-Example-Project)
- [Shared SDK setup, API keys, debugging and strip devices](https://github.com/Nothing-Developer-Programme/Glyph-Developer-Kit)
- [Nothing SDK licence](https://github.com/Nothing-Developer-Programme/GlyphMatrix-Developer-Kit/blob/999b1143a6bff0dd79db11d21b92c15c56a346b1/LICENSE.md)
- [Android sensor coordinate system](https://developer.android.com/develop/sensors-and-location/sensors/sensors_overview#sensors-coords)
- Developer support: GDKsupport@nothing.tech
