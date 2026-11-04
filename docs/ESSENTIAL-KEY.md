# Essential Key remapping feasibility

Research: 3 November 2026. Target handset reported by the user: Nothing A069P / FroggerPro, Android 17 API 37, Nothing OS build C5.0-261021-1625. This document is a design and source analysis; Essential Key support has not been added to the APK and no button events have been captured from this handset.

## Evidence

- [EssentialKeyTools](https://github.com/KoukeNeko/EssentialKeyTools): MIT-licensed source with single/double/triple/long detection, key learning, Accessibility key filtering, action execution and optional system-package disabling through Shizuku. Inspected `EssentialKeyDetectionService.kt`, `KeyGestureClassifier.kt`, `ActionExecutor.kt`, and Accessibility XML.
- [Essential Remap](https://github.com/AbdulKus/essential_remap): README reports testing on Phone (4a) Pro and describes Accessibility for screen-on plus an authenticated local shell input monitor for screen-off. These are upstream claims, not tests on this user's Android 17 firmware.
- [Screen-off verification guide](https://github.com/AbdulKus/essential_remap/blob/main/docs/screen-off-testing.md): deep-sleep, missing events, monitor restart, adbd lifetime, and reboot limitations.
- [Essential Key Remapper](https://github.com/wreck2053/essential-key): documented reversible ADB package changes and action catalog.
- [Community manual remap guide](https://github.com/z3phydev/How-to-remap-or-disable-the-Essential-Key).
- [Android AccessibilityService](https://developer.android.com/reference/android/accessibilityservice/AccessibilityService): key filtering, global actions, window node interaction and gesture dispatch.

## Hardware interception

This is the side Essential Key, not Glyph Touch. The Glyph Matrix SDK does not supply an Essential Key callback. An Activity's `onKeyDown` only handles events delivered to that Activity; a normal foreground service does not get global hardware key events.

EssentialKeyTools uses an Accessibility service with `flagRequestFilterKeyEvents` and `canRequestFilterKeyEvents=true`. It filters `event.scanCode` against a learned value. Its README reports `keyCode=0` / scan code 250 on verified devices, but does not hard-code that assumption for every phone. A capture screen should learn the A069P value and display down/up/repeat/device/timing information while suppressing mapped actions. Normal operation should inspect only the chosen key, not record unrelated input.

The source returns true for matching events, but explicitly notes Nothing's system-policy action can still fire. Its claims that double/triple/long coexist with Essential apps must be tested on the current firmware: no promise that native capture/record actions disappear merely because we classify a different gesture.

## Native action suppression and reversibility

Existing remappers free the key by disabling packages for the current Android user:

```text
com.nothing.ntessentialspace
com.nothing.ntessentialrecorder
```

Package availability and user ID must be checked on-device before any command. Disabling is an optional explicit setup operation; it disables those apps, not just the physical button, while retaining their data. Uninstalling our app will not restore package state. Store the previous enabled states and provide a Restore action. `pm disable-user` / `pm enable` are shell operations requiring authorized ADB or a suitable Shizuku shell service; an ordinary app cannot grant itself this ability.

Essential Remap additionally uses a vendor global setting `nt_block_essential_key=1` for its screen-off path. This is not a documented Nothing API and must not be applied blindly: capture the existing value, verify its behavior on this firmware, and restore the exact prior state when requested.

Root is not necessarily required. Accessibility plus optional authorized ADB/Shizuku is the practical starting point. Root/LSPosed can intercept system policy more directly, but brings a different installation and maintenance model and is unnecessary for the initial implementation.

## Gestures

Use a pure, testable state machine for single/double/triple/long:

- Initial tuning: 350–400 ms between taps; 550–650 ms for long hold, adjustable.
- Single cannot fire instantly if double/triple share the same context: wait for the multi-tap window.
- Ignore auto-repeat down events; match releases to their down time.
- Long emits once and consumes its release; it must not also fire single.
- Cancel timers on disable, changed profile, interrupted sequences and service teardown.
- Handle missing down/up and canceled events. Never infer a long press from delayed IPC delivery; use original input timestamps.
- Foreground-app rules take priority over global mappings. When Camera is open, single can be immediate shutter if no multi-tap shutter mappings are configured there.

## Action feasibility

| Action | Implementation | Important limit |
|---|---|---|
| Open installed app | Launcher intent / app picker | Activity starts and lock-screen behavior remain subject to OS restrictions |
| Open Camera | Camera launcher / still-image camera intent | Opens UI; does not prove a photo was captured |
| Shutter in existing Camera | Click a verified accessible shutter node, or shell-inject a camera/volume key | No universal public cross-app shutter API; actual Nothing Camera behavior must be inspected |
| Take photo in our own camera UI | CameraX/Camera2 with camera permission | Separate feature; cannot take ownership while another app already uses camera |
| Glyph Off/Time/Compass | Internal action to existing display controller | Inherits current Glyph lifecycle behavior |
| Flashlight | CameraManager torch callback and setTorchMode | Flash unavailable while camera owns hardware; inspect permission/device behavior |
| Screenshot, lock, Back/Home/Recents, QS | Accessibility global actions | Secure content and locked-device restrictions apply |
| Media play/pause/next | AudioManager media-key down/up pair | Depends on an active media session |
| Ringer / DND | AudioManager + notification-policy access where required | API/firmware-dependent changes; test actual mode |
| Wi-Fi/Bluetooth/mobile-data/airplane toggle | Settings panel or shell-level action where supported | Ordinary apps cannot directly toggle all protected settings |

For Camera, configure “volume key = shutter” in its settings and test shell-injected Volume Down as one privileged route. Accessibility key consumption alone does not provide arbitrary key injection. `Instrumentation.sendKeyDownUpSync` is not a permission-free system-wide solution. If using an accessible shutter, inspect the actual Camera package, resource IDs, descriptions and supported click action; avoid a blind hard-coded screen coordinate. UI inspection requires additional Accessibility window-content capability, beyond the minimal key-only service, and should be limited to enabled camera rules. Lock-screen Camera needs separate acceptance testing.

## Screen-off support

Accessibility may receive only release events after a press wakes the phone, or receive no usable events in deep sleep. EssentialKeyTools reconstructs an unmatched release from its event times; this is useful but not proof of reliable deep-sleep handling.

Essential Remap describes a shell-owned Java helper (`app_process`) reading Essential input through `getevent`, with source-side gesture recognition, authenticated loopback IPC, deduplication and short handoff wake locks. This is a substantial optional component, not a one-permission feature. Verify shell read access on A069P; non-root shell input access is firmware-dependent. Prefer an established licensed implementation over inventing an insecure exported broadcast receiver.

A shell helper stops on reboot and can die when adbd stops. The upstream guide recommends leaving USB debugging enabled to keep adbd alive without Wi-Fi; a USB cable need not remain connected. Shizuku also normally needs reactivation after reboot. Surface actual helper health; do not display a persisted enabled flag as proof of a live monitor. Avoid continuous wake locks merely for button listening.

## Proposed integration

1. Key capture and diagnostic screen with no remapping initially. User enables Accessibility in Android Settings; sideloaded Android may require Allow restricted settings.
2. An EssentialKeyService separate from CompassService: input should not depend on whether Glyph output is running or frozen.
3. Gesture engine and action registry with editable global and per-app profiles, explicit disabled/none entries and local-only storage.
4. First actions: launch app, open Camera, Glyph mode cycle, flashlight, media, screenshot and navigation.
5. Camera profile: verify accessible shutter and/or authorized shell key injection before offering “take picture” as supported.
6. Optional setup for suppressing native Essential handlers with state backup and Restore.
7. Optional shell monitor for screen-off use if Accessibility testing demonstrates lost events.

Example profile: global single = Glyph mode cycle, double = Camera, triple = chosen app, long = torch. Camera foreground single = shutter; other actions can be individually disabled or reassigned. The exact default map should be configurable.

## Acceptance checks before claiming support

- Capture Essential key down/up on the exact Android 17 build; do not assume scan code 250.
- Single/double/triple/hold execute exactly once; no unintended Essential capture/record opens.
- App launches, notification shade, lock screen, immediate screen-off and 10–30 minute deep sleep.
- Native Camera foreground: shutter captures a real photo once, including portrait/landscape and lock-screen UI where applicable.
- Disable service/remapping, reboot, update, missing releases, rapid presses and conflicts with another key-filtering Accessibility service.
- Restore all modified package states and vendor settings successfully.

Recommendation: begin with key capture and screen-on Accessibility remapping, with optional ADB/Shizuku unlocking. Treat full screen-off operation and cross-app Camera shutter as separately verified capabilities rather than promising them in the first build.
