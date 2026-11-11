# Custom Home as the Back boundary (v0.8)

## Report and scope

Device feedback confirms v0.7 automatically opens the configured Bing page.
Screenshot 995 shows the native web toolbar; screenshot 996 shows the original
news Home after its bottom-left Back action. This was v0.7's intentional
`returnedHome -> nativeFallback` behavior, not a failed URL dispatch. The user
now explicitly rejects revealing that news page.

The fix keeps the browser's permanent Home anchor internally but treats the
specific native web page produced by the module's Home request as the logical
Back boundary. It does not create an extra WebView, delete the anchor, clear
history, automatically reload Bing on Back, finish an Activity directly, or
close other tabs.

## Original APK evidence

Both originals were restored and independently parsed again for this change:

- `Browser-Browser.apk`: 40.10.21.9.6 / 10210906, 272305359 bytes,
  SHA-256 `caeda453881561b60e2342004688b86c830468dcb8e42a06231286581d88b6e1`.
- `OPPO-Browser.apk`: 40.10.25.2 / 10250200, 223247642 bytes,
  SHA-256 `e60a6712963c9601d4cb7b756efd3d66c16e5b5bc4576fac92a3ba557d8f3a2a`.

The classes below are in classes2.dex (Controller/main tab client) and
classes3.dex (BaseTab/AbstractTab/NavigationController). Named UI listener
`com.heytap.browser.webdetails.ui.d2.t()` is the native toolbar Back dispatch.

### Current 40.10.21.9.6

- Toolbar `d2.t -> tab.X(false) -> BaseTab.ec(true,false)`.
- Hardware Back first runs `current.onBackPressed()`. Only an unconsumed action
  reaches `X(true) -> ec(true,true)`.
- `ec -> L(animated,hard)`. `L` first checks `current.Q()` and, when true, calls
  `current.xg(hard)` to consume native webpage history. Only otherwise does it
  call final `M(animated,hard)`.
- `M` reads `NavigationController.E()` (current index). For index > 1 it selects
  the previous native page. For index == 1 it invokes page-leave/return callbacks
  (`bi`, `Xd`) and then `H(animated)` to enter the Home/tab-back strategy. At
  index 0 it returns false without performing these callbacks.
- A false result propagates to `ec`, which invokes `G()` and returns true.
  `AbstractTab.G -> client.x1(tab) -> com.heytap.browser.g.x1 -> Controller.F0`.
- `F0` checks task-root status, entry flags and the native 3500ms double-Back
  interval. It either shows the original exit hint, backgrounds the task or
  finishes the non-root browser Activity. It does not require the current page
  to have PAGE_HOME kind. Its `T(context,boolean)` helper only records analytics.

### Reference 40.10.25.2

The same flow was read separately, not inferred from shifted obfuscated names:

- `d2.t -> Y(false) -> Xb -> M`; hardware Back first offers the page
  `onBackPressed` and then uses `Y(true)`.
- `M` first consumes native webpage history via `R()/yg(hard)`, then calls
  final `N(animated,hard)`.
- `N` is the equivalent index boundary. Index 0 returns false; `Xb` then calls
  `H() -> AbstractTab.H -> client.y1(tab) -> main g.y1 -> Controller.F0`.
- `NavigationController.E/B` and Controller.F0 have the same relevant semantics.

The hook changes only final `M` (current) / `N` (reference), before any native
page-leave callbacks. It returns the ordinary index-0 false result only when
all of these are true: the tab is active, the current native index is 1, and the
current page is exactly the web object captured from this tab's module Home
request. The original callers handle the result. Webpage-internal history,
modal Back handling and native history at index > 1 execute before or outside
this boundary unchanged. Repeated Back does not reload or mutate the page.

## Identifying the module's page

URL equality is not ownership evidence. Another tab or user navigation can have
the same URL, and redirects can change the URL of the intended page.

1. The module scopes its call to public `loadUrl(String)` with an owner/home/epoch
   request. `loadUrl` creates a native LoadParams and immediately passes it to
   `x9` (current) / `s9` (reference), which enters PermissionFacade.
2. The hook records that exact LoadParams object's identity using weak references.
   It does not use object equality or match a URL string.
3. After native permission handling, final `P(LoadParams,boolean)` (current) /
   `Q(...)` (reference) is the `doLoadWithParams` method. Only the matching request
   may establish a capture scope, and only while the original epoch, active tab,
   current Home fragment and selected MajorHomePage still match. An expired
   module request is ignored; unrelated native requests are untouched.
4. `P` creates/updates the native page and calls `p0`, then `C8(page,boolean)`.
   Reference `Q` calls `q0`, then **A8**, not a guessed D8 or E8.
5. Before `C8/A8`, capture the target only if the owning current page is still
   this Home and `NavigationController.B(target)` equals 1. This method is the
   same page-index lookup used by the native `g0(page,animated)` show operation.
6. Store weak Home/page identity pairs. A subsequent module Home request can
   replace its own pair, without changing another tab's pair. Switching away
   never turns an unrelated page into a logical Home.

Both complete mappings are selected by exact manifest versionName, versionCode
and NormalHome fragment creation method. All newly used declared methods are
checked for exact return and argument types; the permission-completion and
page-back methods must also be final. An unknown version or mismatch aborts
URL replacement before the overlay is installed. Existing g1/g0 versus h1/h0
explicit-Home and lifecycle mappings from v0.7 remain in use.

## Failure and race behavior

A successful load remains automatic. The URL overlay no longer restores the
native news children in any recovery case. If native loading fails or has not
left Home after two seconds, it shows a short failure message and Retry. Retry
is a recovery action, not a required launch step during normal success.

The timeout does not cancel an otherwise current permission request: approval
can still complete the exact request after the recovery controls appear. A new
explicit Home/Retry action, a different selected child page or a tab switch
invalidates the old epoch. Its later permission completion cannot navigate a
new foreground tab. Returning after a canceled pending request exposes recovery
controls rather than leaving a permanent loading screen. State changes in the
panel are idempotent so layout notifications do not create a relayout loop.

## Verification boundary

Regression tests cover strict current/reference mappings and mixed-version
rejection, exact identity despite equal objects, owner isolation, deeper history,
repeated Back without mutation, root replacement and tab isolation, late
permission versus newer epochs, and recovery after switching away. Existing
Home lifecycle/generation tests remain applicable.

Integrated build/test/lint results are recorded by the release task. Static DEX
analysis and unit tests are not an LSPosed/device runtime pass. Required device
checks include Bing -> result/link -> Back within the site -> root Back,
repeated root Back and native exit behavior, explicit Home again, multiple tabs,
and delayed/canceled permission followed by Retry.
