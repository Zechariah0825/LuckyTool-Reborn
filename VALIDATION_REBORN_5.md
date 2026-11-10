# LuckyTool Reborn v0.5 feedback fixes

This iteration addresses the five device-reported failures after v0.4 (`c09b947`). It is a test build, not a device-verified release. The original app APKs were inspected; they were not modified or distributed.

## Scope

- Browser homepage: bind the replacement when the real shared HomeFrame attaches or changes, and when its owning tab becomes active. Creation alone returns an empty wrapper. Keep ownership and current-page checks before queued URL navigation, preserve the Back redirect guard, and lay out the replacement independently of the native news layout helper.
- Browser dock: remove the native item-count-based width restriction before child measurement. Preserve outer margins and the bottom offset; divide the available inner width equally among the remaining buttons. Rotation uses the current parent width, not a saved display width.
- Browser search: configure the result-page engine switching bar as original, hidden, or selected Bing, Google, saved custom engines and native 百度/360 搜索/抖音/影视. Native entries retain their actual objects/destinations and are included only when provided by the browser. The live browser default is always retained first in custom mode to prevent native index fallback from silently selecting another engine. Module default selection is now an **apply-once request**, not a permanent override: a changed request uses the native setter once; subsequent choices in OPPO's own settings win and persist across restart. One bounded digest records the applied request in browser-private preferences. Follow-browser clears that marker without modifying the native choice; a module request revision also handles switching to follow-browser and back before browser launch. Renaming engines or editing unrelated entries does not reapply a request. Fresh search models resolve the actual live default rather than assuming the module's last request is current. An already-bound page keeps its key order until native model refresh/rebuild, so a settings change alone cannot desynchronize pager and tab indices. Native refresh receives the necessary custom model objects before it validates saved keys. Hidden-only/follow-browser does not extend lists unless a saved module engine still needs preserving. Global per-engine URL overrides remain removed; every clicked engine keeps its native routing. Model construction uses accessible constructors, `singleSearch` and the `-1` icon sentinel.
- Browser profile: hook actual profile attachment and account button refreshes. Inspect both verified sign-button fields, hide only a button currently labeled as the reward control, and retain login, avatar, account/VIP information, tools and unrelated cards. A profile-local observer handles later updates and is removed on detach.
- Market: remove the complete home-feed card titled exactly `精品随心选` before it is expanded into rows. Keep `应用时刻`, other cards, item order and non-home feeds. This follows the existing remove-featured setting; removing a tab alone did not remove this content.

## Inspected native targets

Both supplied browser builds, 40.10.21.9.6 and 40.10.25.2, were inspected separately. APK identities/hashes are recorded in `TARGET_APKS.md`.

- `NormalHomePageFragment`: creation `xh`/`yh`, attach `ci`/`di`, active entry `rh`/`sh`, and `onHomeChanged`.
- `MajorContainer`: native layout/interception methods. Only the homepage content is replaced, not HomeFrame or other bottom pages.
- `ToolBar.onMeasure`: calls `d(width)` before `FrameLayout.onMeasure`; its inner dock contains `ToolBarNewsLayout`, a `LinearLayout`.
- `DefaultSearchEngines`: live default object `J()`/`F()`, key `G()`/`d()`, native apply setter `g(String)`/`j(String)`, result list `Q()`/`M()` and refresh `o0(List, boolean, boolean, boolean)`. Native picker writes `settings.b.E3(String)` / `BaseSettings.K3(String)` to the `search_engine` preference; `k0()`/`q0()` reads it without filtering custom keys. The one-time request hook runs before the key getter with a recursion guard; it does not replace getter results. `SearchEnginesFacade.b()` resolves the live manager. `MultiSearchPage`: shared engine list `Ui()`/`aj()` and setter `Lk(List)`/`dl(List)` including V1 override. `MultiSearchTabLayout`: binder `F0(List, boolean, Context)`/`G0(List, boolean, Context)`, constructor and exact-type visibility guard prevent later native reshow. `TabPageFragment.getContext()` is inherited and explicitly used by both builds. Pairs are 40.10.21.9.6 / 40.10.25.2.
- `MyProfileView.onAttachedToWindow`, `HeyTapAccountInfoView.setSignBtnText(String, boolean)` and `setShowSignButton(boolean)`. The SDK contains both `mImageBtnSignIn` and `mBtnSignIn`.

Market 26.10.2_CN:

- `a.a.a.cq2.ԫ(Fragment, dq2, CardApiRecycleViewAdapter)` binds a home adapter before loader callbacks.
- `HomeChildCardFragment` and `HomeChosenCardFragment` identify the allowed home scope.
- `addDataAndNotifyChanged(List)` and `setDatas(List)` receive complete card DTOs before row expansion. Card DTOs expose `getTitle()`; the actual server title is established by the user's screenshot.

Unknown signatures retain native behavior and log the missing mapping instead of broadly hiding text elsewhere.

## Validation

- Full `:app:assembleDebug`: passed after the final native-default and concurrency fixes.
- Full `:app:testDebugUnitTest`: passed, 253 tests in 47 suites, zero failures, errors or skipped tests. New coverage includes dock measurement, both profile reward controls, whole Market card filtering, selected/native engine identity, apply-once default requests across restarts, and stable open-page model order.
- Static review found and fixed two additional defects before the final run: changing defaults could reorder a live page without rebuilding its pager, and a new blocking sync monitor could invert the native manager lock order. Per-page model tracking and a nonblocking atomic sync claim address those paths.
- An earlier test attempt could not create Robolectric's download lock in the read-only home directory. The test runtime now uses a writable local home and the same cached SDK artifacts. No tests were disabled or skipped.
- Full `:app:lintDebug`: failed with 406 errors, 630 warnings and 25 hints. All errors are existing `MissingTranslation` entries in unchanged resource files. No baseline or blanket suppression was added; this is not a clean lint result.
- APK verification passed: package `com.luckyzyx.luckytool`, code `23306`, minimum API30, target API37, arm64-v8a; ZIP CRC, v2 signature and 16KiB zip alignment pass. Both native libraries retain 0x4000 LOAD alignment. Launcher alias, Xposed entry and module metadata are present. Verification is repeated after embedding the published source commit; the final APK hash is delivered separately.

No target OPPO phone or Vector/LSPosed runtime is attached. Source/DEX checks, JVM/Robolectric tests and APK verification do not establish that these hooks execute correctly on the user's exact ROM. Device validation must cover initial launch, repeated Home presses, Back, multiple tabs, switching to 我的/搜索+, changing engines, refresh, account login/logout, Market refresh/pagination and dock rotation.

## Installation and settings

Version is `1.3.5_reborn.5`, code `23306`; package remains `com.luckyzyx.luckytool`. Existing preferences are retained.

- In the browser module page choose the desired homepage mode. A saved URL is used only in `自定义网址` mode.
- Set `搜索结果顶部引擎切换栏` and engine switches separately from `应用默认搜索引擎（一次性）`. The latter shows the last module request, not the current browser choice. After applying it once, OPPO browser settings can change the default normally and retain it after restart. Custom bar mode always includes the live default even when its extra-visibility switch is off; hidden mode hides the entire bar. Removing a custom engine resets a matching module request to follow-browser; native fallback handles a removed saved engine.
- Existing browser bottom-tab/profile cleanup and Market featured-removal switches continue to control their fixes.
- Restart the browser and Market after changing preferences; existing running processes do not automatically reload all module settings.

The signer certificate SHA-256 is `03cf4fa61d485d7c83ed3f0575e1b5fbc2f647f53b5b9966a65d2bb30e30b6e2`, identical to the delivered v0.4 APK. The higher version code permits an in-place upgrade of that build. This is not the official LuckyTool signing key. Do not uninstall a working installation just to test an incompatible build; uninstalling can erase private settings.

GitHub Actions remain paused: no workflow edits or manual runs are part of this iteration, and source publication uses `[skip ci]`.
