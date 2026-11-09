# LuckyTool Reborn v0.3

Based on the supplied `LuckyTool-Reborn-v0.2-build-ready.zip`.
The upstream GPL-3.0 license and all 440 existing hook source files are preserved.
This is an independent fork, not an official LuckyTool release.

## Android 17 DEX optimization

- Android SDK 37 and newer use `/system/bin/cmd package compile` in the existing root service instead of the removed `IPackageManager.performDexOptMode` entry point.
- Older vendor releases fall back to the same command when the legacy method is absent or abstract.
- Commands use separate validated arguments, a 180-second deadline, a 32 KiB retained-output cap, and explicit successful exit/output checks.
- Recoverable Binder and linkage failures are reported as failed packages rather than terminating the app/root service. Profile clearing remains a required step.
- Cancelling a timed-out command client does not guarantee cancellation of work already accepted by Android's ART service.
- This mitigation does not establish that every vendor hook works on every Android 17/ColorOS build. A target-device test remains necessary.

## Monochrome appearance

- Material remains the default appearance on new installations.
- All 48 Material color roles are neutral in light and dark modes, including fixed accents, error roles, inverse surfaces and container colors.
- AMOLED backgrounds and containers are black. Principal text/background pairs meet the tested 4.5:1 contrast target.
- Dynamic color and colorful category badges default off, but explicit existing preferences are retained.
- App-list icons are grayscale in the default monochrome appearance. Launcher icons, splash colors and legacy controls are also neutral.
- The alternate Miuix appearance, custom accent colors, wallpaper colors, and all feature/settings screens remain available.

## Reproducible build setup

- Debug builds no longer require upstream private keystore properties.
- Release signing is optional and uses a private `keystore/keystore.properties` file when supplied; without it, release artifacts are unsigned.
- Removed dependency on private R8 obfuscation dictionaries.
- Corrected the Compose BOM to use `implementation(platform(...))`.
- Version code is deterministic (`23304`) and can be overridden with `-PrebornVersionCode`; version name can be overridden with `-PrebornVersionName`.
- The Gradle wrapper launcher was regenerated from the verified official distribution, and both have verified SHA-256 checksums. Builds use an installed JDK 21 without an Oracle-only daemon constraint.
- CI builds the checked-out project, runs tests/lint, and uploads artifacts. It does not import source from another repository or push changes.

See `VALIDATION.md` for the exact build and test results supplied with this revision.
