# LuckyTool Reborn — OPPO Browser / App Market local source implementation

Historical status dated 2026-11-09, retained below. For the current 2026-11-10 implementation and actual build/test results, see [VALIDATION_REBORN_4.md](VALIDATION_REBORN_4.md) and [TARGET_APKS.md](TARGET_APKS.md). The embedded-WebView and positional-navigation descriptions below describe the older implementation, not the current revision. No GitHub Actions changes were made in either iteration.

## Target apps and evidence

- User-provided `OPPO-Browser.apk`: ColorOS 17 OPPO Browser, screenshot version `40.10.25.2_e8dfd0f`.
- User-provided `OPPO-Market.apk`: ColorOS 17 OPPO Market, package `com.heytap.market`.
- DEX method/class signatures below were inspected from those APKs. This confirms hook **candidates**, not successful runtime invocation.

## Implemented in the local Kotlin source

1. Browser: opt-in homepage replacement at `NormalHomePageFragment.yh` and `SearchHomePageFragment.yh`, with blank, URL (embedded Android WebView), and shortcut grid modes. Shortcut list can be edited, deleted and reordered as lines in LuckyTool's settings. The original fragment is kept invisible in the view hierarchy for lifecycle callbacks. For URL mode the initial homepage is an embedded WebView, **not** an OPPO native tab. Shortcut clicks and HTTP(S) navigation open the browser via ACTION_VIEW; historical/tab continuity is **not verified**.
2. Browser: opt-in five-item bottom tab model filter, preserving positions 0,2,4 (首页 / 搜索+ / 我的) in `BottomTabDataDefault` / `BottomTabManager`. Unknown model formats are left untouched. Also includes a bounded visual fallback, with equal-weight reflow for recognized horizontal five-item LinearLayout.
3. Browser: opt-in full-card hiding for `我的小说` and `领现金` based on live view hierarchy geometry; the fallback may miss nonstandard/Compose/native-view cards and is not a verified data-level removal.
4. Browser: Bing and Google default search engines, multi-line user-defined engine templates, actual search URL routing via verified `search.impl.engine.c.r(String)`. Optional native list injection on `DefaultSearchEngines` returns and lookup methods; **experimental** and off by default. Selecting a module-level engine changes the URL builder regardless of native-list injection. Engine matching / search flow requires device validation.
5. Market: opt-in DTO list filtering removes the title `精选` only when `应用时刻` is present in the same tab list, preserving other entries and order. Hooks known `SubTabCardDto`, `SubTabCardGlobalDto`, `TabDto` getters/setters, with default selection preference plus a bounded view-level selection fallback on `ScrollIdleTabLayout`.
6. Browser and Market pages: all settings are wired into the existing module preferences and Xposed hookers; the old LuckyTool feature switches remain intact. New options default disabled, except selecting a Reborn search engine explicitly, to avoid unexpected app changes.
7. Existing browser and market DexKit hook registration is isolated so a hook-install exception doesn't prevent other hook installations. This does not guarantee all old hooks match the new app versions.

## Changed/new files

- `app/src/main/java/com/luckyzyx/luckytool/hook/scopes/browser/RebornBrowserConfig.kt` (new)
- `app/src/main/java/com/luckyzyx/luckytool/hook/scopes/browser/BrowserHomeReplacement.kt` (new)
- `app/src/main/java/com/luckyzyx/luckytool/hook/scopes/browser/BrowserBottomTabs.kt` (new)
- `app/src/main/java/com/luckyzyx/luckytool/hook/scopes/browser/BrowserUiCleanup.kt` (replaced experimental one-line label hide)
- `app/src/main/java/com/luckyzyx/luckytool/hook/scopes/browser/BrowserSearchEngines.kt` (new)
- `app/src/main/java/com/luckyzyx/luckytool/hook/scopes/market/RebornMarketTabRules.kt` (new)
- `app/src/main/java/com/luckyzyx/luckytool/hook/scopes/market/MarketHomeTabs.kt` (new)
- `app/src/main/java/com/luckyzyx/luckytool/hook/hookers/HookBrowser.kt`
- `app/src/main/java/com/luckyzyx/luckytool/hook/hookers/HookMarket.kt`
- `app/src/main/java/com/luckyzyx/luckytool/ui/compose/scopes/apps/OplusBrowserPage.kt`
- `app/src/main/java/com/luckyzyx/luckytool/ui/compose/scopes/apps/OplusMarketPage.kt`
- `app/src/test/java/com/luckyzyx/luckytool/hook/scopes/browser/RebornBrowserConfigTest.kt` (new)
- `app/src/test/java/com/luckyzyx/luckytool/hook/scopes/browser/RebornTabRulesTest.kt` (new)

## Validation performed

- The seven new hook/config Kotlin files passed Kotlin syntax and type checking with lightweight **mock Android/YukiHookAPI interface stubs**. Stub compilation is NOT Android compilation.
- All twelve local executable assertions passed (8 URL/shortcut/search config, 4 browser and Market tab filtering). Two JUnit test classes were added but the Gradle/JUnit suite was not run in this iteration.
- Observed target DEX classes include `NormalHomePageFragment`, `SearchHomePageFragment`, `BottomTabDataDefault`, `BottomTabManager`, `DefaultSearchEngines`, `search.impl.engine.c`, Market `SubTabCardDto`, `SubTabCardGlobalDto`, `TabDto`, and `ScrollIdleTabLayout`.
- Android SDK/Gradle toolchain not present in this container, so **no real `assembleDebug` build, APK signing, installation, device smoke test, or live hook compatibility verification** was performed. This bundle is NOT a validated installable release.

## Device verification needed

1. Compile the whole Gradle project on an Android SDK 37/JDK 21 machine with its dependency repositories.
2. Load on a matching OPPO device with Vector/Xposed, enable browser/market scopes, then enable one feature at a time in LuckyTool.
3. Restart the affected app after changing the module settings; confirm the active browser version matches the target.
4. Inspect module logs for any `Reborn:` lines; if a tab model does not match, it intentionally stays unchanged rather than risking corruption.
5. Check custom homepage open/new tab/back navigation, URL handling, home shortcut behavior, browser bottom bar geometry, search query routing, and Market startup tab after asynchronous loading.

The local source is progress toward the requested features, but without real Android compilation and device verification it must not be called a completed or working APK. No original OPPO APK was patched or redistributed here.
