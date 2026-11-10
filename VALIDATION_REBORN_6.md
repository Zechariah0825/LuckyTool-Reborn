# LuckyTool Reborn v0.6 integrated validation

Version: `1.3.5_reborn.6`, code `23307`, application ID `com.luckyzyx.luckytool`.
This integrates the browser fixes from `6d7b5f4`, the native Market page correction,
and runtime diagnostic evidence. It is not a device-verified compatibility claim.

## Browser

The homepage attachment/ownership correction, adaptive equal-width dock, exact
profile reward-control cleanup, configurable result-page engine bar, and native
browser default-engine persistence are retained. See `VALIDATION_REBORN_5.md`
for the inspected methods and behavioral boundaries. A module default choice is
an apply-once request; later changes made in OPPO browser settings take precedence.
Bing, Google, saved custom engines and native engines keep their actual routing.

## Market

The previous tab/first-index rewrite accidentally gave the featured content page
the 应用时刻 title. The final paired title/page binding now removes the verified
featured page and retains the real native App Moment page. Thus the recommended
app groups shown in the screenshots are removed at their page source rather than
by matching 精品随心选 or 『附近地区』的人都在下 text.

The original page objects, refresh selection and Back-to-App-Moment behavior are
preserved. Unsupported/mismatched structures retain the original UI. No claim is
made about arbitrary future server content inside the genuine native App Moment
route. Full DEX evidence and guards are in `docs/market-native-page-fix.md`.

## Diagnostics

105 setting keys across 107 Hook files have runtime callback evidence attached.
This is not full coverage: 104 of the 386 statically inventoried literal switches
are enrolled, plus one string mode; generated keys/custom controls need separate
review. Unenrolled or untriggered features remain explicitly unknown.

Module loading, hook registration, callback triggering and final visible effect
are distinct. A callback count is not proof that a setting worked. Only explicit
mutation/readback evidence can report a measured modification. Stale processes,
changed configuration/module/host APK identity, malformed or unmatched replies
and expired observations cannot be reused as current positive evidence.

See `docs/HOOK_DIAGNOSTICS.md` for scope, privacy, lifecycle and transport limits.

## Validation

- Final integrated `:app:assembleDebug`: passed.
- Full `:app:testDebugUnitTest`: 354 tests in 53 suites passed, with zero failures, errors or skipped tests. This includes 103 diagnostic tests and nine Market page-pairing tests.
- Full `:app:lintDebug`: failed with 406 errors, 641 warnings and 24 hints. All 406 errors are `MissingTranslation` entries in unchanged `app/src/main/res/values/strings.xml`; this is not a clean lint result.
- APK verification: package/version/minimum API30/target API37/arm64, ZIP CRC, v2 signature, Xposed entry/metadata, launcher alias, 16 KiB ZIP alignment and both native libraries' 0x4000 LOAD alignment passed. Source-revision metadata is rebuilt and the artifact checks repeated after publishing the source commit; the final file hash is provided with the APK.
- Static review verified the native Market factory/adapter ordering and corrected selection/Back synchronization before the full test run.
- The 105-key diagnostic static inventory and `git diff --check` passed. No tests were disabled and no lint suppression/baseline was added.

No OPPO phone or Vector/LSPosed runtime is attached. Required device checks remain:
cold and cached start, browser Back/Home/multiple tabs, default-engine changes in
both apps and restart, result-bar selection/visibility, dock width/rotation,
profile updates, Market refresh/Back/tab switching, and actual diagnostic callbacks
with both Material/Miuix layouts and multiple/shared target processes.

## Signing and installation

The former local v0.4/v0.5 signing key is unavailable in the restored environment.
This build uses a newly generated private debug key, not the official LuckyTool
key. It cannot overwrite the earlier differently signed APK despite its higher
version code. Back up settings before changing installations; do not uninstall a
working installation merely to test an incompatible build, because app-private
settings can be lost. The new public certificate SHA-256 is
`b0240d829cdb969d2a5fa90cc9d376984eefaa3cfb4d58656a41bbaf68e67be5`.
The app has Settings → Back up data / Restore data controls; verify the exported
backup file before deciding to replace the old installation. Re-enable the
module and its scopes after reinstalling and restart the affected apps/device.

GitHub Actions remain paused: no workflow edits or manual runs are part of this
work. Final source publication uses `[skip ci]`.
