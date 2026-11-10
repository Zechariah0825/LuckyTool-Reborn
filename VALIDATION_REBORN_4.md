# LuckyTool Reborn v0.4 development validation

Date: 2026-11-10. Work is based on upstream fork commit `a4619c30b5ec0f7e4fa80c95f790510aa4618e09`.

This is a test build, not a device-verified release. The earlier `VALIDATION.md` records the v0.3 base build only.

## Changes in this iteration

- Fixed malformed URI input escaping `IllegalArgumentException` handling: `URI` throws `URISyntaxException`. Invalid saved URLs no longer abort parsing of the rest of the shortcut/engine list.
- Accept normal URL anchors, reject invalid ports, and restrict search `%s` to path/query rather than host/port/fragment.
- Validate edited homepage, shortcut and engine settings before saving in both Material and Miuix interfaces; clear an explicitly deleted custom default selection. Custom engine IDs now use a 128-bit SHA-256 prefix rather than colliding Java string hashes, while previous saved IDs still resolve.
- Browser cleanup removes the exact MyBookshelfHistoryCard only inside MyProfileView, and the verified account-card reward button only when its current visible text is the reward label. Login, avatar, account/VIP card, game/document/video cards and unrelated pages are preserved. Detached-window observers are removed; asynchronous reappearance is handled by layout observation.
- Browser/Market only scan the APK with DexKit when an existing DexKit feature is enabled. Native-hook registration survives a DexKit initialization failure.
- Against the provided Market 26.10.2_CN APK, correct the filtered tab startup index, clear competing default flags, hook the inherited tab attachment method, support a one-tab result and clean up bounded asynchronous selection observers.
- Fix existing API31 dynamic-color calls on Android11, configuration-observable home resources and Scaffold padding.
- Replace the status-bar clock's permanently retained Timer with a main-thread ticker that pauses on detach and resumes on reattach.
- Version: `1.3.5_reborn.4`, code `23305`. Package and all existing settings/features remain preserved.

## Browser behavior and inspected targets

- The downloaded Drive browser is 40.10.21.9.6; the earlier uploaded browser is 40.10.25.2. Both APKs were inspected rather than assuming their obfuscated names match. Market is 26.10.2_CN. File hashes are recorded in `TARGET_APKS.md`.
- Bottom navigation filters actual integer IDs and verified model getters rather than assuming every list contains model objects or blindly dropping positions 1/3.
- Native search list/lookup and URL-builder signatures are selected separately for the two browser builds. Custom model construction was inspected against both. The native-list option remains experimental and opt-in; an explicit module-level default overrides the browser's default. “Follow browser settings” leaves selection to the browser, including added entries.
- Homepage replacement is limited to MajorContainer (首页), not the encompassing HomeFrame. The bottom navigation, 搜索+ and 我的 pages remain under native control. Blank and shortcut pages are native Android views.
- URL/shortcut navigation uses the actual owning browser tab's `loadUrl(String)`. It creates neither a secondary WebView nor a separate Activity.
- Initial URL-home activation and an explicit verified bottom-toolbar Home click open the configured URL. Back does not rearm automatic navigation, preventing a return-to-home redirect loop; the page offers an explicit open button. The separate webpage-toolbar Home path was not independently verified.
- Queued navigation rechecks both the current native fragment and the current Major page before loading, so changing to 我的 while a callback is queued does not redirect it.
- All new customization options are opt-in. Enable the desired switches/mode on LuckyTool's browser/store pages and restart the affected app to reload preferences.

## Verification

- Full `:app:assembleDebug`: passed after the final source changes.
- Full `:app:testDebugUnitTest`: passed, 226 tests in 42 suites, zero failures, errors or skipped tests. These include malformed URL/configuration, hash-collision compatibility, integer/model tab filtering, per-tab Home rearming, Market default positions, precise profile selectors, dynamic-color API guards and ticker attach/detach cases.
- An initial ticker test exposed a Robolectric virtual-time/wall-clock mismatch (42 callbacks rather than 2). The test now injects the synchronized virtual clock; production still defaults to real wall-clock seconds. Its exact count and lifecycle assertions remain intact.
- Some combined build/test attempts exhausted this constrained machine's process resources. The successful runs separate compilation, tests and lint, using one worker, in-process Kotlin and bounded test forks. No tests were deleted or ignored.
- APK package `com.luckyzyx.luckytool`; minimum API30, target API37; arm64-v8a only. APK ZIP CRC, v2 signing, 16KiB zip alignment and both native libraries' 0x4000 LOAD alignment passed.
- Enabled/exported MAIN/LAUNCHER alias, Xposed entry class, module properties and scope metadata are present.
- Signer certificate SHA-256: `03cf4fa61d485d7c83ed3f0575e1b5fbc2f647f53b5b9966a65d2bb30e30b6e2`. It differs from the v0.3 certificate.
- Full `:app:lintDebug`: failed with 406 errors, 608 warnings and 13 informational findings. All remaining errors are existing `MissingTranslation` entries. The prior API-level, main-thread, Scaffold/resource-observation and test-annotation errors were corrected. No baseline, blanket suppression or skipped tests were added; this is not a clean lint result.
- Final APK/source archive hashes and the published source commit are supplied with the delivery artifacts; an APK hash is not embedded in this source document to avoid circular build provenance.

No target OPPO phone, KernelSU LKM or Vector runtime was attached to this build environment. Compilation and unit tests cannot establish OEM hook execution or complete Android17 compatibility.

## Signing and rollback

The generated APK is signed with this environment's debug key. It is not the official LuckyTool key and may also differ from the earlier v0.3 environment. Same-package APKs signed by different keys cannot upgrade in place. Export and verify settings outside app-private storage before considering uninstall; uninstall can erase private data. Keep the previous APK and a recovery route available.

The module uses libxposed API102. The installed Vector/framework must support that API. No original OPPO APK is modified or included in the source/artifact bundle. GPL-3.0 and upstream notices are preserved.

GitHub Actions were not edited or manually triggered in this iteration.
