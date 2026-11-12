# Reborn v0.10 community package

Version `1.3.5_reborn.10` / `23311`. Installed application ID: `io.github.zechariah0825.luckytoolreborn`. Name: LuckyTool Reborn. This is an independent enhanced fork of LuckyTool; upstream attribution and GPL-3.0 are retained.

## Change and compatibility audit

- `applicationId` is independent; source namespace, AIDL, real component classes and generated Xposed entry retain `com.luckyzyx.luckytool`.
- Launcher alias uses `${applicationId}.Hide` to match the existing icon show/hide code. Provider authorities, own broadcasts, RemoteViews and app-information routes use installed identity.
- Generated KSP Entry and `java_init.list` keep the correct implementation class. Yuki 1.5.0 bytecode was inspected: `LibXposedBaseEntry.onModuleLoaded()` passes framework `getModuleApplicationInfo()`; `YukiHookModuleCaller.callOnHook$default()` reads its `packageName` and `sourceDir`, not the namespace. Remote prefs use the bound framework service.
- Framework remote preferences are per module. Stable preference names stay the same, but a new package must be activated and the old export imported; it does not read the old application's private directory.
- Imports accept old base64 JSON or plain JSON. Invalid files show an error instead of crashing. Four allowed preference collections retain their existing remote/local destinations. Invalid values do not delete current settings. New exports retain Long/Float type metadata.
- Update checks use this fork's repository and select only exact community APK assets. Version and package checks reject old-package releases. See [migration and release conventions](COMMUNITY_MIGRATION.md).
- All v0.9 functionality, including removed diagnostics, unified browser/market settings, colored Google icon and Search+ Home return route, is retained.

## Validation

- Main `:app:compileDebugKotlin`: passed.
- Full `:app:testDebugUnitTest`: 344 tests, 0 failures, 0 errors, 0 skipped; includes 20 new backup/identity/provider/update-policy regression tests.
- Signed `:app:assembleDebug`: passed. APK identity, enabled launcher alias, all merged provider authorities and generated receiver permission use the independent ID.
- APK v3 signature matches the authorized v0.8/v0.9 certificate; ZIP CRC and 16 KiB ZIP/ELF checks passed. Modern Xposed entry and browser/market scopes are present.
- Full `:app:lintDebug`: 406 errors (all existing `MissingTranslation`), 679 warnings, 29 hints. A new Compose resource-read error found during development was fixed; no new error remains. Rules were not disabled.
- Final artifact checksums are distributed separately in `SHA256SUMS.txt`. No device installation/startup was claimed.

## Device and migration limits

No OPPO/ColorOS 17/Vector device is attached. Compilation, package checks and unit tests cannot establish all Hook behavior on the phone. Install as a separate app, disable the old module and enable the new module, restore the saved export, regrant required root/permissions, and restart affected apps. Keep the old app until migration has been checked. Full procedure: [COMMUNITY_MIGRATION.md](COMMUNITY_MIGRATION.md).

GitHub Actions remain deferred by user instruction; local build results are not CI results. Community submission/acceptance is a separate process.
