# LuckyTool Reborn target app references

Development targets: `com.heytap.browser` (OPPO Browser, screenshot version 40.10.25.2_e8dfd0f) and `com.heytap.market` (OPPO App Market).

The binary files `OPPO-Browser.apk` (~223 MB) and `OPPO-Market.apk` (~93 MB) were supplied by the device owner for compatibility inspection. They are NOT bundled in this public repository. Source code and test fixtures must not be confused with APK redistributables.

The 2026-11-09 local source ZIP is a **separate, newer working tree** than the existing v0.3 base snapshot. The full ZIP import has NOT been completed. The `CustomizationPolicy.java` and `BrowserConfig.kt` files are self-contained pieces recovered from the current development context; they are not proof of working browser/market hooks.

Before claiming completion, compare the latest local ZIP against this repository, import the remaining modified Hook files, compile `:app:assembleDebug`, then validate on a ColorOS 17 device.

Do not commit OEM APKs to this public source tree. GitHub has a 100 MB ordinary Git file limit; attaching a larger APK requires a separate authorized artifact or LFS upload path, which this connector does not expose.
