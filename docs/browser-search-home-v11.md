# v0.11: custom Home root Back returns to the Dock Search+ child

## Corrected diagnosis

The v0.9 predicate confused `ai.page.multi.MultiSearchPage` (a native tab page)
with the Dock's Direct Search child of `NormalHome`. In addition, cold Home and
ordinary explicit Home continued to use the v0.8 owned-root Back guard, which
returned `false` from BaseTab's final page-stack Back method and therefore
requested the browser's native exit path. Merely avoiding `returnHome` in one
MultiSearch callback did not fix that behavior.

The v0.9 direct `loadUrl` special case is removed. All explicit URL Home entry
now uses the same generation/permission-aware native request tracking. This
change does not manufacture a web-history entry for Search+, use a Search URL,
remove the native Home anchor, clear tabs/history, or force an Activity exit.

## Actual reference APK evidence

Inspected `OPPO-Browser.apk`, 40.10.25.2 / 10250200, SHA-256
`e60a6712963c9601d4cb7b756efd3d66c16e5b5bc4576fac92a3ba557d8f3a2a`.
The following findings are derived from its actual DEX method bodies. They are
static evidence, not device validation.

* `home.g.c(ToolBar, View)` logs `onToolBarDirectSearchClicked`, tests
  `NormalHome.y1()`, and invokes `NormalHome.v2` -> `u2(false)`. This is not
  `home.g.f`, the separate search-router callback, or MultiSearchPage.
* `u2(boolean)` tests the registered Direct Search item, lazily creates field
  `x` through `S0/G0`, and selects it through `s2/t2`. The class is `lv1.c`,
  whose `getType()` is 7 and whose view is `DirectHomePageContainer`.
  `y1()` verifies selected child type 7. False avoids the optional extra focus
  action used by the boolean overload.
* `t2` -> `A0` runs `NormalHome$setCurrentPage$2.invoke` synchronously: it updates
  selected field `B`, deactivates/hides the old child, activates/shows the new
  child, and notifies Home observers. `A0` finally calls `o2`; for active Home,
  `lv1.c.e0(tab, HomeInfo)` persists HomeInfo child type 7.
* `BaseTab.h1(false)` calls `L` (client `D1` -> TabManager `v2/q2`, setting the
  native TO_HOME strategy) then `Tb(0,false)`. Home activation applies HomeInfo
  through `BaseBrowserHomeController.c` -> `home.c.S2`. Therefore selecting
  Search before returning Home is unsafe: activation can overwrite it.
* `NavigationController.f0` -> `ViewPager.Q/R/S/P` -> `o/onPageSelected` updates
  the selected native page synchronously with animation false. Selection is
  verified again after the call; no delayed child-selector is left queued.
* The native load path is `loadUrl` -> permission `s9` -> `Q` -> `h3` -> provider
  `TabManager.b/n1`. A non-web current page creates a native web page and inserts
  it through `ba/T9/NavigationController.G` at `n+1`. An existing ordinary web
  current page can be reused. `BaseLoadParams.V` retains a non-null supplied
  HistoryStrategy. None of these facts alone prove a MultiSearch history
  guarantee; the new fix does not depend on that claim or alter this strategy.
* Root interception stays at final `BaseTab.N(boolean,boolean)`, after native
  webpage internal history has declined Back. Identity-captured root page at
  index 1 only; deeper stacks, other pages, and inactive tabs remain native.
* `NormalHome.onBackPressed` -> `R1` first handles controller/keyboard/overlays
  and the selected child's `onBackPressed`. `lv1.c.onBackPressed` delegates to
  its embedded page before returning false. Only then does `R1` reach `U1`,
  which otherwise switches any non-major child to Major. For a Search+ child
  restored by this module, and only inside this actual Home Back call, the
  terminal `U1` fallback returns false. Native exit handling can then proceed
  without reopening the custom URL. Other Search+ visits are unchanged.

## Actual current-device APK evidence

Verified `Browser-Browser.apk`, 40.10.21.9.6 / 10210906, 272,305,359 bytes,
SHA-256 `caeda453881561b60e2342004688b86c830468dcb8e42a06231286581d88b6e1`.
The new whitelist is derived from this APK's method bodies, not name offsets.

* `home.g.c(ToolBar, View)` logs `onToolBarDirectSearchClicked`, checks
  `NormalHome.x1()`, and calls synthetic `u2` -> instance `t2(false)`.
  `t2` checks availability with `y1()` (not the selection predicate), lazily
  creates field `x` with `T0/G0`, then selects via `r2/s2/z0/A0`.
* Field `x` has class `uq1.c`; `getType()` returns 7 and `e0` writes HomeInfo 7.
  `x1()` reads `k1()` -> selected field `B.getType()` and compares with 7.
  The exact mapping is therefore `t2 / x1 / U1 / uq1.c`.
* `A0` invokes the action synchronously and calls `n2` to persist the active
  child's HomeInfo. `NormalHome$setCurrentPage$2` updates field `B`, visibility,
  active state and observers synchronously, as in the reference version.
* `BaseTab.g1(false)` -> `AbstractTab.K` -> client `g.C1` -> TabManager `s2/n2`
  configures native TO_HOME, then `bc(0,false)` -> NavigationController `f0`
  -> ViewPager `Q/R/S/P/o`. Both first-layout and normal paths synchronously
  notify `onPageSelected`. NavigationController `U` changes current page before
  activating it. `NormalHomePageFragment.rh` -> controller `setActive/c/N2`
  applies HomeInfo. The module selects Search only after this return finishes.
* `loadUrl` creates exact LoadParams and calls permission `x9`; callback resumes
  final `P` -> `AbstractTab.d3` -> provider `TabManager.b/m1`. Home produces a new
  Web page, inserted through `ja/Y9/NavigationController.G` at `n+1`, before
  `P` calls `C8`. Existing `x9/P/C8` request identity capture remains applicable.
* `BaseTab.L` checks current `Q()` and runs `xg()` for internal web history before
  calling final `M`. `M` handles index >1 normally and index 1 via Home strategy.
  The module only consumes the exact captured active index-1 page. All other
  pages retain native behavior; `ec` invokes native exit `G` only if Back is
  unhandled.
* `NormalHome.onBackPressed` -> `R1` still runs controller/overlay and selected
  child Back first. `uq1.c.onBackPressed` delegates to its embedded page. Its
  terminal `U1()` normally calls `w2()` to select Major when child type is not 1.
  The existing scoped U1 guard therefore also applies to the current APK,
  without bypassing keyboard or embedded-page Back handling.

The current lifecycle mappings `xh/ci/rh/sh`, native `g1/g0`, and boundary
`x9/P/C8/M` were rechecked against this APK. No additional runtime changes beyond
the new strict Search whitelist entry were required.

## Runtime ownership and failure behavior

On an owned root Back, invalidate the old URL generation before native Home
activation. Mark a synchronous Search return transaction so lifecycle/layout
callbacks cannot start another URL. Call verified `returnHome(false)`, check
same active owner/current Home, invoke the native Direct Search selector, then
verify its actual selected class and type. Successful return forgets the old
web boundary so a subsequently reused web object cannot impersonate a custom
Home root. A new explicit Home may capture a new root.

If the selector returns false, throws, or does not commit the expected child,
consume this Back without requesting exit or URL reload. The Major replacement
continues covering news and presents Search+ retry when Home is current. A
failed pager transition leaves the existing web page available for another
Back attempt. No selector is scheduled to run after a tab change. Explicit
Home cancels Search-return state and re-arms normal custom Home navigation.
Unknown versions or mismatched signatures fail before any URL overlay hooks.

During the synchronous return, temporary Major/type-7 child changes preserve
the transaction. After success, actually leaving type 7 relinquishes its special
Back behavior without authorizing a URL reload. Failed Search activation retains
Major retry state, but leaving Major relinquishes that state. Selection callbacks
that cancel or replace a request cannot complete the obsolete request.

## Validation scope

The three targeted suites now contain 26 tests: 14 Search return state tests,
7 boundary identity tests, and 5 strict Search route-mapping tests. They cover
native-return-before-child-select ordering, reentrancy, explicit cancellation,
inactive/wrong owner, unsuccessful selection, exceptions and retry, old
web-object reuse, canceled permission generations, non-explicit Major
transitions without redirects, and separate current/reference native mappings.
Existing route-gate tests provide additional delayed permission coverage.

Fresh integrated validation on 2026-11-14 passed compilation and all 367 unit
tests, including these 26 Search/Home cases. The signed APK passed package,
signature, ZIP integrity and 16 KiB ZIP/ELF alignment checks. Full lint remains
failed on existing missing translations; see [the release validation record](../VALIDATION_REBORN_11.md)
for exact counts. Static APK analysis and unit tests are not phone validation.
