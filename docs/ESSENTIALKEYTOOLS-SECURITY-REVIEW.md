# EssentialKeyTools security review

Reviewed 3 November 2026. Reviewer: coding assistant, static analysis only.

## Decision summary

No evidence of intentional malware, remote-control functionality, credential harvesting, key-event exfiltration, advertising or analytics was found in the reviewed first-party source and inspected release APK paths. The architecture is consistent with a hardware-button remapper. Reasonable to trial the official stable release with only Accessibility enabled, understanding the findings below. This is not a certification that the app or future updates are safe.

The 1K+ download count is neither proof of safety nor evidence of malware. Actual capabilities, implementation and release provenance are more useful evidence.

## Scope and identity

- Repository: https://github.com/KoukeNeko/EssentialKeyTools
- Clone: `/tmp/opencode/EssentialKeyTools` (temporary review checkout).
- Initially inspected main `3ad63ae590f6cbd2821803da14a7ed8f98956c80`, then checked out stable tag **v1.0.18** and reviewed that version specifically.
- Tag commit: **c3d17bd7eb643e42eed9f61853ecd8b90265d44a**. The tag itself is an annotated Git object with a different hash.
- Release: https://github.com/KoukeNeko/EssentialKeyTools/releases/tag/v1.0.18
- APK: `EssentialKeyTools-v1.0.18.apk`, 2,701,908 bytes, downloaded directly from the release.
- Application ID: `dev.koukeneko.essentialkeytools`; version 1.0.18, version code 10018; minimum API 35, target API 36.
- APK SHA-256: `76825306409229d66f92de19373036cb83b5680d5016a1d2ea899b44959c4d70`.
- Signing-certificate SHA-256: `8c031cbbf4390ca03ce827525fdf89d27c1edb9990106f75b8cb00e152580890`.

`apksigner verify` passed. The downloaded hash matches both GitHub asset metadata and release notes. Embedded APK version-control metadata names the exact tag commit above. These are useful consistency checks, but all repository-provided hashes/metadata ultimately share the maintainer's trust boundary. A valid signature proves integrity and signer continuity, not benevolent behavior or independently verified identity.

The Play-distributed split APKs and Play signing certificate were **not** obtained. Do not treat this as binary verification of whatever version Google Play serves. Current main has features absent from v1.0.18, notably the unlocked-only action setting.

## Findings

### F1 — Low: Android backup contradicts “never leaves your device” language

**Evidence:** `app/src/main/AndroidManifest.xml:40-42` enables backup; `res/xml/data_extraction_rules.xml:6-12` and `backup_rules.xml:8-13` have no exclusions. This was also confirmed in compiled APK manifest/XML. `PRIVACY.md:22-25,53-55` says settings stay only on-device. Crash report storage is `filesDir/last_crash_report.txt` (`diagnostics/CrashReportStore.kt:29-30`), and DataStore is in ordinary private storage.

**Impact:** mappings (including selected app package names), onboarding/key settings and the last crash report are eligible for Android-managed backup/transfer, depending on user backup settings and backup transport. This is not developer-operated exfiltration and not proof that a backup occurred. The absolute privacy claim is inaccurate.

**Fix:** exclude settings/reports from both backup and transfer, or document Android-managed backup accurately.

### F2 — Low privacy exposure: automatic cosmetic GitHub request

**Evidence:** `contributors/GitHubContributorsService.kt:19-49` performs an HTTPS GET to the project's public GitHub contributor endpoint, automatically displayed by the home screen. Requests have fixed User-Agent/Accept headers and no key events, settings, app inventory or user ID in the request construction.

**Impact:** GitHub observes IP address and normal connection metadata even if the user never checks for updates. This is disclosed in the privacy policy, and no analytics service was found. The connection is unnecessary for core remapping.

**Fix:** make contributor loading opt-in or bundle contributor names. Core key remapping is designed to work offline; traffic blocking was not dynamically tested.

### F3 — Operational/security boundary: stable v1.0.18 has no unlocked-only guard

**Evidence:** `service/EssentialKeyDetectionService.kt:179-190` executes assigned actions after gesture detection without querying KeyguardManager. Main's later commit adds an unlocked-only option, but it is not in the reviewed release.

**Impact:** a person with physical access can trigger assigned actions from the button where the OS delivers events, including while locked. This does not bypass Android's lock screen or prove access to protected app data. It is relevant if assigning sensitive launches, screenshots or media actions.

**Mitigation:** choose innocuous mappings for the stable version. Do not assume README features from main exist in the release. A build with a tested unlocked-only option is preferable when that boundary matters.

### F4 — Operational: native Essential apps stay disabled after removal

**Evidence:** `unlock/PackageToggleService.kt:16-32` issues reversible `pm disable-user`/`pm enable` operations. Candidate list is exactly `com.nothing.ntessentialspace`, `com.nothing.ntessentialrecorder`, `com.essentialintelligence`. `UnlockWizardScreen.kt:101-120,218-228` exposes per-package user-triggered toggles. Release notes and policy warn about persistence.

**Impact:** unlocking single press can disable Essential functionality entirely. Uninstalling the remapper does not restore package state. This is advertised functionality, not covert persistence; package data is not deleted by these commands.

**Mitigation:** leave Shizuku ungranted for an initial trial. If using unlock, restore the packages through the wizard before uninstalling. Restore uses `pm enable`, not a stored exact prior enabled-state restoration.

### F5 — Low supply-chain hardening: mutable CI actions and incomplete source-to-binary proof

**Evidence:** `.github/workflows/release.yml` references Actions by tags rather than immutable commit hashes. It restores the signing keystore before executing the build. Release publication permits replacing assets (`--clobber`), and GitHub metadata says this release is not immutable. No independent reproducible-build comparison was performed. Dependencies are version-pinned, but no Gradle artifact verification metadata was located during review.

**Impact:** compromise of build dependencies/actions or the maintainer account remains a path to a malicious signed release. This is not evidence that compromise occurred. The APK is R8-obfuscated, which is normal for release optimization but limits manual comparison.

**Fix:** pin action SHAs, verify dependency checksums, minimize signing-secret exposure, publish build attestations and support independently reproducible builds.

## Positive controls and inspected behavior

### Accessibility is narrower than a generic automation app

The compiled service config enables key-event filtering only (flag `0x20`). It does **not** declare `canRetrieveWindowContent`, gesture injection, screenshot capture capability or notification/window event subscriptions. `onAccessibilityEvent` is empty in both source and decompiled APK. Screenshots use the assigned global system screenshot action, not a continuous capture/upload loop.

Normal operation filters to the learned scan code before classification (`EssentialKeyDetectionService.kt:133-160`). Learning mode can temporarily see other delivered hardware keys except volume keys (`KeyEventFilter.kt:27`), so the service is still a privileged input observer; it is not a text-input keyboard logger in the reviewed implementation. Raw event streams are in-process, bounded to 32 buffered entries, replay zero, and not persisted or sent over the network (`KeyEventStream.kt:31-54`). Detection mode is composition-scoped rather than explicitly Activity-resume-scoped, a hardening opportunity if the learning UI remains composed while backgrounded. No persistent history or transmission path was found.

### Shizuku path is allowlisted

The only first-party shell execution located was `ProcessBuilder` with a list of fixed arguments, not a shell-interpolated string. Packages must match the three constants. It does not accept arbitrary commands or download executable scripts. A fresh non-daemon Shizuku user service is released after calls. The exported Shizuku provider is protected by `INTERACT_ACROSS_USERS_FULL`, and the Accessibility service is non-exported and BIND_ACCESSIBILITY_SERVICE-protected. The launcher Activity is the expected public entrypoint; its intent handling did not expose an action-execution API in the inspected source.

This allowlist is an implementation restriction, not an OS sandbox limiting what a future update could do after receiving Shizuku permission. Granting Shizuku remains a significant trust decision. `ProcessBuilder.waitFor` has no execution timeout (only service binding has one), which is a potential availability issue, not an observed privilege-escalation path.

### Networking and updates

Only first-party HTTP clients found fetch public GitHub contributors/releases. Updates are checked on button interaction, use Play's API for Play installs or HTTPS GitHub release metadata for sideloads, and open the appropriate external listing/release page. No self-downloader, REQUEST_INSTALL_PACKAGES permission, arbitrary remote-code loader, custom TLS trust bypass, embedded WebView JavaScript bridge or first-party listening socket was found.

The user-triggered “Report on GitHub” URL includes device/build facts and an exception summary; the full report is copied for the user rather than automatically posted (`diagnostics/CrashIssue.kt:32-39`). Exception text can contain contextual information, so review a report before sharing. The app's private crash file holds only the latest report; no remote crash-reporting SDK was identified.

### APK permissions and libraries

Compiled permissions: INTERNET, VIBRATE, ACCESS_NOTIFICATION_POLICY, Nothing Glyph permission, optional Shizuku permission, and an app-specific signature permission generated by AndroidX. No microphone, camera-capture, location, contacts, SMS, external-storage, notification-listener, device-admin or APK-install permission was present. DND access is distinct from reading notifications.

Declared dependencies are AndroidX/Compose/DataStore, Kotlin, Play in-app update and Shizuku, plus Nothing's local proprietary AAR. No advertising/analytics dependencies were identified. Nothing's AAR SHA-256 matches the official SDK already fetched for Glyph North (`be00ee9cd7115f6b11984c6e31fe98e298fb726940d1555063610685ef3bbf29`). Native libraries in the APK are named AndroidX graphics-path and DataStore shared-counter libraries; native code was not independently audited. No comprehensive transitive dependency CVE scan was performed.

## Binary inspection and limitations

Used Android `aapt` to inspect compiled permissions, manifest, Accessibility and backup XML; `apksigner` for signature validation; JADX 1.5.3 for decompilation and targeted checks of Accessibility, shell commands and network destinations. Decompiled key filtering/empty accessibility event handler and package command allowlist align with stable-tag source. JADX reported six decompilation errors, so this is explicitly a **partial binary inspection**, not complete proof of correspondence. No independent rebuilt APK comparison was made.

The release's VirusTotal/Koodous labels say **submitted**, not “clean”. Current detection results were not obtained or independently verified; no clean-scan claim is made. There was no physical-device installation, runtime network capture, active IPC exploitation, full third-party/native audit or Android 17 behavior test. Future versions and Play artifacts are outside this specific APK review.

## Installation recommendation

For this reviewed version, a limited trial from the official publisher is reasonable. Start with Accessibility only, map a harmless action, and leave Shizuku/DND access off unless needed. Restore any disabled Essential packages before uninstalling. The reviewed release is not enough to promise cross-app camera shutter or reliable deep-sleep operation. If maximum control is desired, a locally built version without INTERNET, with backup disabled and an unlocked-only option would reduce the identified exposure, but was not built in this review.
