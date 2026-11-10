# Browser homepage routing follow-up

## Failure found in v0.6

The URL placeholder deliberately contained an `打开自定义主页` button. Its automatic
route was guarded once per `NormalHomePageFragment`; only the HomeFrame toolbar
listener `home.g.b(ToolBar, ToolBarRippleButton)` rearmed it. This is not the web
page Home route. The actual web/search Home callbacks call `BaseTab.g1(boolean)` in
40.10.21.9.6 and `BaseTab.h1(boolean)` in 40.10.25.2.
Consequently a reused fragment could keep the consumed guard and show the manual
button after an explicit return Home. A native subpage switch likewise reuses the
fragment and runs in `NormalHome$setCurrentPage$2.invoke(): void`, not fragment
creation. Its action updates `NormalHome.B`, hides the old page, shows/activates
the new one, and then notifies listeners.

Neither `BaseTab.g1` nor `BaseTab.h1` can itself be treated as evidence of a user
Home click. The real Back/special-URL routes use them too: `TabManager.p1/s1`
in 40.10.21.9.6 and `TabManager.q1/t1` in 40.10.25.2. The new hook rearms only
while a verified UI Home callback is on the same thread, or when a real selected
subpage changes to MajorHomePage. The existing HomeFrame button remains covered.

## Navigation and Back

URL mode no longer creates a manual launch button. Routing waits until the next
animation turn, then checks the current home owner, selected major page, current
fragment, tab activity, and activation generation. A queued operation from an
old activation cannot consume the new activation. Active leave/tab reassignment
invalidates queued work without treating Back as a new Home request.

A void `loadUrl` return is not proof that a web page appeared: it enters the
browser's permission callback. `BaseTab.g0(last, current)` / `h0(last, current)` is the actual
`onCurrentPageChanged` notification used to distinguish a completed departure
and later return. An accepted request that remains on the same active Home for
two seconds restores usable native controls without automatically resubmitting
the request. That recovery is generation-checked and cannot overwrite a new Home
request or an observed departure. History return restores the original interactive native Home
content instead of looping, retaining a manual gateway, or leaving a blank dead
page. A new explicit Home action hides that fallback and routes directly again.

The native Home is a permanent navigation anchor. The controller constructor
keeps it in `b` and at `f[0]`; removal/clear paths skip PAGE_HOME. No Home replacement
API was established. The patch does not delete that anchor, clear user history,
force-finish an Activity, or trigger an additional synthetic Back action.

## Actual 40.10.21.9.6 cross-check

The recovered original `Browser-Browser.apk` was read, not inferred from the
reference APK: 272305359 bytes, SHA-256
`caeda453881561b60e2342004688b86c830468dcb8e42a06231286581d88b6e1`.
Its binary manifest gives package `com.heytap.browser`, versionName
`40.10.21.9.6`, versionCode `10210906`. The settings screenshot's `_dba1160`
suffix is not part of the manifest versionName.

This inspection caught a real mapping difference before publication:
- Current: fragment create/attach/active-enter/active-leave are `xh/ci/rh/sh`;
  explicit web Home route is `BaseTab.g1(boolean)`; page-change notification is
  `BaseTab.g0(TabPageFragment, TabPageFragment)`.
- Reference: `yh/di/sh/th`; `BaseTab.h1(boolean)` and
  `BaseTab.h0(TabPageFragment, TabPageFragment)`. Its manifest versionName/code
  are `40.10.25.2` / `10250200`.
- Current has an unrelated `h0()` with no parameters. A first-name-found or
  same-shape search would be insufficient evidence of semantic compatibility.

Runtime selection now requires the exact manifest versionName, versionCode and
fragment creation name, then validates both selected declared methods' exact
instance/void signatures. Unknown versions and mismatched structures leave
native URL Home unchanged before installing any replacement/lifecycle hooks.
A mapping failure is logged rather than selecting another candidate by chance.

All six explicit web/search Home listener classes and their `h`/`e` methods
exist in both originals. Their current-version bodies invoke `g1`; the reference
bodies invoke `h1`. The actual current `g0` logs `onCurrentPageChanged` and forwards
last/current to `k0`. Both versions retain `NormalHome.B` and the
`NormalHome$setCurrentPage$2.invoke(): void` ordering: write selected page, hide
old, show/activate new, notify. `MajorContainer` measurement/layout/interception
signatures also match.

Permission handling remains native. Current `loadUrl(String)` constructs
LoadParams, calls `x9(LoadParams, boolean)`, then PermissionFacade and `tab.b.a`;
reference uses `s9` and `tab.b.d`. The module calls only the stable public
`loadUrl(String)` and does not bypass or replace permission decisions. Both
versions' navigation controllers retain the permanent Home anchor.

## Actual reward-control cross-check

Current XML follows `home_frame_my_profile_layout` → `CustomSignInVipCard` →
`account_center_heytap_custom` → `CustomSignInAccountInfoView`. That class extends
`HeyTapAccountInfoView`, which declares `mImageBtnSignIn: COUIIconButton` and
`mBtnSignIn: COUIButton`; both button classes inherit TextView. Exact-base-class
matching rejected this real subclass, and looking for declared fields on the
subclass could not find the inherited controls. The patch accepts the verified
subclass and reads the fields from their declaring superclass.

The current parent's `setSignBtnText(String, boolean)`, `setShowSignButton`,
`setThirdSignBtn`, and the subclass's `setSignBtnText`/`setAccountInfo` match the
reference instructions. Subclass setters call super and then update styling,
without restoring button visibility. MyProfileView.onAttachedToWindow exists.
Only identified cash-labeled button descendants are hidden; account, avatar,
VIP, login and other tools are retained.

Resource-name obfuscation differs: current still contains `button_login_sign`
and `nearx_btn_sign_in`; only the reference obfuscates those names. Therefore
resource obfuscation is not claimed as the current device's primary failure.
The superclass field lookup works independently of that difference.

## Verification boundary

Reference APK: `OPPO-Browser.apk`, 40.10.25.2, SHA-256
`e60a6712963c9601d4cb7b756efd3d66c16e5b5bc4576fac92a3ba557d8f3a2a`.
Both original APKs have now been statically inspected. This proves the mapped
class/method/field relationships, not successful Xposed execution on a phone.

Regression coverage exercises strict version/pair selection and unknown/mixed
signature rejection; consumed Back versus explicit Home; stale queued epochs;
tab switching; nested/repeated activation; direct failure and uncompleted native
permission dispatch; and reversible visibility without restoring native GONE or
removed children. Compilation/test results after the mapping correction belong
in the integrated validation record. No phone/LSPosed runtime pass is claimed.
