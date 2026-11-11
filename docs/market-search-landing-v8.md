# Market search landing cleanup (v0.8)

## Scope and controls

- `reborn_market_clean_search_landing`, default `true`: search landing cards retain only 大家正在搜 and native search-history cards.
- `reborn_market_blank_search_hint`, default `true`: the dedicated search screen action bar has no suggested/rolling hint. Typed text and submitted queries are not rewritten.
- The controls are independent and are registered before the legacy DexKit early return. Turn off the relevant control and restart Market to restore its original behavior.
- Search-history storage, chips, and native clear action stay intact. Actual search results, installation actions, and the previously fixed 应用时刻 homepage are outside the landing hook.

## Verified target and screenshots

Static inspection used OPPO Market `26.10.2_CN` (`261002`), 93,531,819 bytes, SHA-256 `6a43dedbb4d74e45e53c4be5f438d87c101cf6eaf79714799e4ad886da5b2c56`, as recorded in `TARGET_APKS.md`.
The two supplied screenshots were viewed: the search landing screen has history chips, 人气热搜/精品推荐/热门APP合集 recommendation cards, 大家正在搜 keyword chips, and different gray suggested hints. No real history queries are included in tests or this document.

## Card identity and insertion boundary

Actual DEX registration `a.a.a.fn2.init` registers `com.nearme.cards.widget.card.impl.search.SearchAllLookingForCard` as **100203** (both registration code and renderer type). This is not a title substring guess.

`SearchAllLookingForCard.ރ(CardDto)` requires `TermListGlobalCardDto`, reads `getTerms()`, and renders the keyword chips. Its title comes from the DTO or `R$string.all_look_for` when empty. It owns its own title, so retaining arbitrary standalone title cards is unnecessary. Its `ຘ()` returns 100203.

`com.heytap.market.search.core.fragment.home.SearchRecordCardManager.ֈ(int)` explicitly recognizes **100204, 40117, 154** as history card types. Preserve these types unchanged; leave the manager and its history insertion/removal implementation alone.

The hook is on exactly:

- Class: `com.heytap.market.search.core.fragment.home.SearchHomeCardAdapterPresenter`
- Method: `ޒ` (U+0792), `(com.nearme.platform.loader.paging.d, java.util.List) -> void`

This method runs native first-page clearing and the history runnable, then delegates to `CardAdapterPresenter.ޒ`, which calls `CardApiRecycleViewAdapter.addDataAndNotifyChanged(list)`. The base presenter's success callback `ޗ(request,response)` obtains `ViewLayerWrapDto.getCards()` and dispatches virtually to `ޒ`. Filter a new list at this dedicated insertion boundary, not a shared DTO getter or renderer used by other screens. Both initial and later batches entering this path are covered; the original response and cache list are never mutated.

An all-recommendation batch becomes an empty list but the original method still runs, preserving first-page clear/history initialization. Unknown *card codes* are removed as recommendations by the requested allowlist. Unreadable *schemas* (null card, missing/failing/non-integer `getCode`) preserve the whole original batch and log a warning. No partial filtering is applied on a schema failure. This is explicit compatibility fallback, not proof of support for other Market versions.

## Hint lifecycle and user input separation

`com.heytap.market.search.core.actionbar.SearchActionBar` owns `ࢩ` (U+08A9), an EditText. Its constructor inflates `search_core_action_bar`, calls `ޒ()` (U+0792) to assign the EditText and install native listeners, then adjusts layout with `ޤ()`.

- `setSearchHint(String)` calls only `EditText.setHint` and native button-state update `ޥ(getSearchText(), hint)`. Force only its hint argument to `""`; run the native method.
- `setSearchHint(String,String,SearchActionBar$h)` and the five-argument variant create delayed global-layout listeners when needed, then render decorated hints. Short-circuit these before listener installation and call the simple hint setter instead.
- `a.a.a.jde.ԫ(SearchActionBar,a.a.a.nhe,String)` (U+052B) is the dedicated recommendation binding path. It can fetch `getNextRecommendInfo(0)` as a fallback and invoke either rich hint overload. Short-circuit it and use the simple blank setter.
- `a.a.a.jde.Ϳ(SearchActionBar)` (U+037F) reads the recommendation tag `search_core_action_bar_recommend_data` and already supports returning null. Return null so the native empty-text/button-state fallback cannot use a hidden recommended word.
- After native `ޒ()` initialization, call the simple blank setter to remove any inflated initial hint.

No `setSearchText` overload, `EditText.setText`, `TextWatcher`, query callback, or global TextView method is replaced. Native `ގ(int)` obtains `getSearchText()` and calls the existing submit callback; `ތ(String)` forwards its string normally. Typed queries and keywords selected from history/大家正在搜 keep their native path. Network recommendation fetching is not claimed to be disabled; the display and action-bar recommendation fallback are blocked.

## Checks and remaining device validation

Completed without Gradle:

- Nine JUnit tests in `MarketSearchRulesTest` passed using the standalone Kotlin compiler and JUnit 4.13.2.
- The three new production classes compiled with the actual YukiHook 1.5.0/KavaRef 1.1.1 APIs, Android SDK, and JVM target 17.
- `git diff --check` passed.

The tests cover exact retained card identities/order, all three history variants, title-independent filtering, unknown recommendations/headings, an absent keyword card, empty later batches, immutable/repeated input, schema fallback, and hint-only invocation preserving synthetic input/selection/submit data. The hint tests use a protocol fixture, not the installed APK or Xposed runtime.

The integrated v0.8 test run passed all 424 tests in 61 suites, including these nine tests; build, lint and artifact results are recorded in `VALIDATION_REBORN_8.md`. No device pass is claimed. On the device, verify cold search entry and reopening, refresh/pagination/cache reentry, history deletion, clicking a 大家正在搜 keyword, typing/submitting by button and keyboard, returning from results, waiting for hint rotation, and both switches disabled independently. Confirm the 应用时刻 homepage still behaves as previously verified.
