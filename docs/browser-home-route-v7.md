# Browser homepage routing follow-up

## Failure found in v0.6

The URL placeholder deliberately contained an `打开自定义主页` button. Its automatic
route was guarded once per `NormalHomePageFragment`; only the HomeFrame toolbar
listener `home.g.b(ToolBar, ToolBarRippleButton)` rearmed it. This is not the web
page Home route. The actual web/search Home callbacks call `BaseTab.h1(boolean)`.
Consequently a reused fragment could keep the consumed guard and show the manual
button after an explicit return Home. A native subpage switch likewise reuses the
fragment and runs in `NormalHome$setCurrentPage$2.invoke(): void`, not fragment
creation. Its action updates `NormalHome.B`, hides the old page, shows/activates
the new one, and then notifies listeners.

`BaseTab.h1` cannot itself be treated as evidence of a user Home click. The real
Back/special-URL routes in `TabManager.q1/t1` use it too. The new hook rearms only
while a verified UI Home callback is on the same thread, or when a real selected
subpage changes to MajorHomePage. The existing HomeFrame button remains covered.

## Navigation and Back

URL mode no longer creates a manual launch button. Routing waits until the next
animation turn, then checks the current home owner, selected major page, current
fragment, tab activity, and activation generation. A queued operation from an
old activation cannot consume the new activation. Active leave/tab reassignment
invalidates queued work without treating Back as a new Home request.

A void `loadUrl` return is not proof that a web page appeared: it enters the
browser's permission callback. `BaseTab.h0(last, current)` is the actual
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

## Evidence and verification boundary

Reference APK: `OPPO-Browser.apk`, 40.10.25.2, SHA-256
`e60a6712963c9601d4cb7b756efd3d66c16e5b5bc4576fac92a3ba557d8f3a2a`.
Relevant DEX classes include NormalHome and its setCurrentPage action, BaseTab,
NavigationController, webdetails/ui/d2, SearchBaiduToolBarWebListener, and
TabManager. The user screenshot confirms the actual device is 40.10.21.9.6;
reference inspection alone is not a device compatibility result.

Regression coverage exercises consumed Back versus explicit Home, stale queued
epochs, tab switching, nested activation, failure isolation, and reversible
native-content visibility without restoring native GONE or removed children.
Compilation/test results and current-APK cross-check belong in the integrated
validation record. No phone/LSPosed runtime pass is claimed here.
