# Play submission preparation — v1.6

## Produced locally

- Redesigned native Display / Settings UI, adaptive + themed launcher icon, large controls and diagnostic details under Settings.
- Explicit runtime notification flow, clock-first start, no network permission, backup/transfer exclusions.
- Release shrink/minify configuration, vendor keep rules, signing template and `scripts/release.sh`.
- Debug APK for user testing; release APK/AAB can be generated. Without `keystore.properties`, the release bundle is unsigned and **not uploadable**.
- Draft store copy, privacy policy and artwork. Artwork is generated from project designs; it is not a screenshot of the running app.

## Required before upload

1. **Reliability gate:** repeat the previously failing screen-off scenario on A069P Android 17: 30–60 minutes unplugged, notifications, lock/unlock, repeated tile cycles, shaking and face-up reset. Record battery use, failures and diagnostics. Passing geometry tests is not evidence this is fixed. Verify Phone (3) separately or narrow advertised support.
2. **UI verification on hardware:** font scale 1.0/1.5/2.0, narrow display, portrait/landscape, TalkBack names, start/stop, notification denial, background/foreground, clock legibility, bright/dim surroundings. Take actual phone screenshots for Play (at least two), without personal notifications. No emulator/browser screenshot is supplied as proof of hardware behavior.
3. **SDK distribution terms:** Nothing's vendored SDK is proprietary; its licence restricts redistribution except as authorized and prohibits commercial use without prior written permission. Resolve intended public distribution/commercial use with GDKsupport@nothing.tech. Include required vendor notices. Source analysis is not legal permission.
4. **Publisher identity:** provide support email, publisher name and a public hosted privacy-policy URL. Replace draft contact wording in policy before publishing. Confirm `dev.nothingthings.glyphnorth` is the intended permanent application ID before the first upload.
5. **Signing:** create and securely back up a publisher-controlled upload key, configure the ignored `keystore.properties`, enroll in Play App Signing, run `./scripts/release.sh`, verify APK/bundle signer, and retain R8 mapping per release. Never upload the debug APK. A Play/release signing key will differ from the existing debug installation; testers may need to uninstall the debug app, losing local preferences.
6. **Play forms:** complete Data safety (no app data collected/shared in this build, with sensor processing on-device), privacy URL, content rating, target audience, ads declaration, app access and foreground-service declaration. Publisher must verify answers against the final binary and current Console definitions.
7. **Special-use FGS:** explain that the user deliberately starts a visible rear clock/compass, motion input switches modes, front screen can be off, and Stop is always available. Provide the Console-requested demonstration video. `specialUse` approval is not guaranteed; do not claim a sensor FGS type exempts review.
8. **Device catalog:** exclude unsupported models where appropriate. A standard manifest feature cannot uniquely target the Nothing Glyph Matrix; app runtime checks and store wording both matter. Current min/target are API 34/36; verify the current target-level requirement in Console before submission.
9. **Testing tracks:** upload to internal testing, run pre-launch checks, then any closed-testing requirements applicable to the publisher account before production. No Console upload or account operation has been performed here.

## Foreground service declaration draft

Feature: user-started rear display clock / magnetic compass with shake-and-flip interaction.

User benefit: glance at the rear display and switch to compass without leaving the front display on.

Trigger: Start Glyph in the app or explicit Quick Settings tile tap. No boot auto-start.

Visible state/control: ongoing notification identifying mode with Stop; Stop in app and tile Off.

Why it cannot be deferred: current heading and user shake/orientation are interactive, real-time inputs; delayed scheduled work cannot provide the selected live mode. This explanation must be reviewed together with actual battery measurements and the final implementation.
