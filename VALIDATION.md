# Validation: LuckyTool Reborn v0.3

Date: 2026-11-09. Validation ran on a Linux x86_64 build machine, not the target phone.

## Build

- Gradle 9.8.2, Android Gradle Plugin 9.5.0, Kotlin 2.4.21.
- Eclipse Temurin JDK 21.0.13+9, Android SDK platform 37.0 revision 2, Build Tools 36.0.0 for compilation.
- `:app:assembleDebug`: **passed**, 82 executed tasks, including Kotlin/Java compilation, resource/manifest processing, DEX packaging, vital lint and signing.
- The Gradle ZIP and JDK archive were checked against their official published SHA-256 values.
- The supplied wrapper JAR did not match the Gradle 9.8.2 checksum. It was regenerated using the verified Gradle distribution; the final wrapper JAR matches the official SHA-256 `3b8a25775a69158b5ad2b1d17a80a88dc7b40a352a73eb27d06c612a7ce68e98`.
- Final `:app:testDebugUnitTest :app:assembleDebug --offline`: **passed** after the final test/build configuration changes; 94 tasks (6 executed, 88 up-to-date). Its APK SHA-256 exactly matches the file already delivered.
- Fresh-checkout fixes include optional private release signing, applied Compose BOM, deterministic version code and removal of private R8 dictionary requirements.

## APK

- File: `LuckyTool-Reborn-v0.3-debug.apk`
- Size: 23,348,083 bytes.
- Package: `com.luckyzyx.luckytool`.
- Version: `1.3.5_reborn.3` / version code `23304`.
- Minimum API 30; target and compile API 37.
- ABI: arm64-v8a only.
- SHA-256: `ac8166d6517ef3c10b0115177d35fae412a2d02fe280b89565be586d2bb4d37c`.
- APK Signature Scheme v2 verification: **passed** (Android debug certificate, RSA 2048).
- Signer certificate SHA-256: `7ddf826e1dcdba21ba32ecf8c40f2a53a015d8c7123337ee2d6fa5e158291f4e`.
- `zipalign -c -P 16 -v 4`: **passed**.
- Both included arm64 libraries (`libdexkit.so`, `libandroidx.graphics.path.so`) have 16 KiB ELF LOAD-segment alignment.
- APK contains the Xposed entry class list, module properties and scope list. The module requires **libxposed API 102**; use a Vector/framework version that supports API 102.
- Enabled launcher alias and its MAIN/LAUNCHER intent filter are present in the packaged manifest.

## Tests and static checks

- `:app:testDebugUnitTest`: **passed**, 196 tests in 34 suites; zero failures, errors or skips.
- This includes 9 DEX compatibility tests, monochrome palette/contrast/AMOLED tests, saved-preference tests, and existing navigation/settings/hook-policy regression tests.
- Full `:app:lintDebug`: **failed**, 414 errors, 559 warnings and 8 hints. This is not a lint-clean release.
  - 406 errors are missing translations in existing locale resources; Android falls back to default strings for missing translations.
  - The remaining 8 errors are 4 API-level guard findings in the existing dynamic-color code, 2 configuration-aware resource findings on the home screen, 1 status-bar clock thread finding and 1 unused Scaffold padding finding in an existing test.
  - Every error location is in one of six files verified byte-for-byte unchanged from the supplied archive. These findings were not suppressed or hidden behind a baseline. This comparison is not a claim that the entire original project was independently linted with identical dependencies.
  - The unchanged status-bar clock thread finding and other existing behavior still require review before a production-quality release. The provided APK is a test build.
- All 440 hook source files from the supplied archive are unchanged. GPL-3.0 `LICENSE.txt` is unchanged.
- All ten launcher density/round PNGs are grayscale. Android XML color resources parse successfully and use neutral colors.

The first test run could not download Robolectric runtime artifacts because its separate JVM did not inherit the build network proxy. A local-only test configuration forwarded the environment's proxy and existing trusted CA store and used a writable cache. The final test run above completed successfully. One newly added test was corrected to compare color values rather than ColorScheme object identity; application code was not changed for that correction.

## Installation and runtime boundary

This APK was **not installed or tested on an OPPO Find X9s Pro**, KernelSU LKM or Vector. Successful compilation, signing, tests and alignment are not a promise that every hook works on a particular ROM.

The debug signing key differs from the official LuckyTool signing key. The same package name prevents installing this APK over an official release with a different key. Export settings from the old app to a location outside its app-specific directories (for example Downloads or a computer), and verify the backup exists before considering uninstalling it. Android may delete private app data and settings on uninstall. Restoring all older settings is not guaranteed. After installation, enable the module again in Vector, review its scopes and reboot as required by the framework.

Keep the working prior APK and a recovery route available. Validate module activation, app startup, light/dark appearance, one harmless setting, DEX optimization and reboot behavior before enabling broad hooks. No private signing key is included in the source archive.

CI keeps APK/unit-test validation required and runs full lint in a separate non-blocking job with its actual failure and reports preserved. A green required build must not be interpreted as clean lint.

The official Android SDK is provided under its [SDK Terms](https://developer.android.com/studio/terms).
