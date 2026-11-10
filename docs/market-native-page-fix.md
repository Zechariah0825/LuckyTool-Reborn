# Market: preserve the actual App Moment page

## Verified target and reported symptom

This fix targets the supplied `com.heytap.market` **26.10.2_CN / 261002** APK:
93,531,819 bytes; SHA-256 `6a43dedbb4d74e45e53c4be5f438d87c101cf6eaf79714799e4ad886da5b2c56`.
The screenshots show 应用时刻 selected while the content is an app recommendation
feed, including 精品随心选 and 『附近地区』的人都在下. These are complete groups,
not merely stray headings or a tab that needs hiding.

## Root cause established from DEX

`HomeSubTabPresenter.ࡥ(SubTabCardGlobalDto,int)` reads `getFirstPagePos()`, removes
that entry from a working copy, and calls `hi7.getHomeSubPageItemList(...)` for the
other entries. It then inserts the existing **HomeChosenCardFragment** at the
removed position, with the removed entry's title. This ordering is essential:
changing the DTO list and forcing `firstPagePos` to 应用时刻 before this method
executes relabels the featured recommendation page as App Moment. Removing
individual recommendation card titles cannot repair that page identity mismatch.

The final private method `HomeSubTabPresenter.ޚ(List,List)` (U+079A) receives the
completed title DTO list and matching pager-item list. It updates
`CdoFragmentPagerAdapter.Ϳ(List)` and `ScrollIdleTabLayout.Ԩ(List)` together.
The pager item `CdoFragmentPagerAdapter$a` stores the fragment in `Ϳ` (U+037F),
title in `Ԩ` (U+0528), and original actionParam in `Ԫ` (U+052A).
`getItemPosition` uses fragment identity and item IDs derive from pager items,
so retaining the original objects preserves the native pages and their state.

`hi7.generatePageItem(...)` passes each original actionParam to the pager item
constructor and creates the corresponding fragment. `createHomeChildFragment`
recognizes `/appmoment/main`, defaults an empty request path to
`/card/store/v4/column/main`, and uses `HomeChildCardFragment` if the optional
fragment service does not return a fragment. This fallback is explicitly supported
only for an exact `oap://mk/appmoment/main` route (query arguments are allowed).
A real `AppMomentCardsFragment` is also recognized. Its separate activity route
is not used as proof that the optional home fragment service always returns it.

## Implementation

- Do not hook or rewrite `getSubTabDtoList`, its setter, or `getFirstPagePos`.
- At the final paired binding, require equal list lengths, matching titles, one
  精选 pair whose fragment is HomeChosen, and one 应用时刻 pair whose fragment/type
  and route identify the native moment page. Remove only the verified 精选 pair.
- Retain the actual App Moment page object, its route, its content and its native
  lifecycle. No content-title blacklist or global card filter remains.
- Start on App Moment. On later native rebuilds, retain the user's selected
  surviving tab by title. Keep the default/Back target on App Moment.
- The one-shot dataset observer reads `ࢯ` (U+08AF) during binding. Supply the
  restored current index temporarily, then restore the App Moment home index.
  Align `ࢳ` (U+08B3), the previous scrolling index, before dataset notification.
  After binding, select the intended current page and explicitly synchronize
  `onPageSelected(actualCurrentItem)`: same-index selection does not otherwise
  re-register the native Back callback after restoring the home target.
- Remove the earlier independent title-only layout watcher. An unknown or
  mismatched structure now leaves native pages and selection untouched instead
  of hiding one thing and selecting a different thing.

The existing `reborn_market_remove_featured_tab` switch still controls this fix.
Other Market pages and the other home tabs are not removed. The implementation
removes the wrongly displayed featured feed, rather than promising to remove
arbitrary future server content within the genuine App Moment route.

## Verification boundary

Regression tests model the captured factory ordering, reproduce the old relabeling
bug, assert page-object identity/order, exercise refresh and selection restoration,
separate the home/Back index from the current index, check exact native route and
fragment guards, and cover missing/ambiguous/mismatched lists and unknown routes.

Focused local result: all **9** Market JUnit 4.13.2 tests passed using the restored
Kotlin 2.4.10 CLI compiler and JDK 21. No Gradle task was run for this focused check.
`git diff --check` passed. Full app compilation and integration tests belong to the
separate final-build validation.

DEX inspection establishes signatures and control flow, not execution on a phone.
A device check remains necessary: cold start, cached start, refresh, switch among
home tabs, scroll, Back to App Moment, and process restart with the setting both
on and off. Unsupported target versions or unknown page mappings deliberately
retain the native UI; they are not silently treated as successfully cleaned.
